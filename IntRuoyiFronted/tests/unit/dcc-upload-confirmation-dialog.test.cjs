const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { h, ref } = require('vue')
const page = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
const source = page.slice(page.indexOf('const confirmUploadApplication ='), page.indexOf('const saveUploadDraft ='))
const rows = [
  { label: '完整文件名称（含后缀）', value: '长文件名称.docx' },
  { label: '文件编号', value: 'SOP-008' }, { label: '初始版本号', value: 'A/1' },
  { label: '批准人', value: '批准甲' }, { label: '批准人账号', value: '账号ID 901' },
  { label: '原批准组 99', value: '必须全部批准' },
  { label: '项目', value: '测试项目' }, { label: '存储文件夹', value: 'G44文档' },
  { label: '文件类型', value: '质量管理 / 操作规程' }, { label: '生效方式', value: '文控审核后立即生效' },
  { label: '生效时间', value: '文控审核签名完成时生效' }, { label: '培训', value: '不需要培训' },
  { label: '会签部门', value: '质量部' }, { label: '下发部门', value: '生产部' },
  { label: '下发范围', value: '受控且已生效后向全部人员下发' },
  { label: '关联文件', value: '甲.pdf；乙.docx', items: ['甲.pdf', '乙.docx'] },
  { label: '附件', value: '未上传', items: [] }, { label: '编制人', value: '编制乙' }
]
const setup = () => {
  let box
  const context = { h, ref, exports: {}, formData: { processType: 'CONTROLLED_FILE' },
    isExternalReview: ref(false), submitFieldErrors: {},
    buildSubmitFailureFeedback: error => ({ message: error.message }),
    applySubmitFailureFeedback() {}, isApprovalMatrixStaleError: () => false,
    ElMessageBox: { confirm: (content, title, options) => { box = { content, title, options }; return box } }
  }
  vm.runInNewContext(ts.transpileModule(source + '\nexports.open = confirmUploadApplication',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { open: context.exports.open, getBox: () => box }
}
const text = node => typeof node === 'string' ? node : Array.isArray(node?.children) ? node.children.map(text).join(' ') : String(node?.children || '')

test('confirmation has a file summary, two columns and collapsed rules; selected materials are directly visible', () => {
  const { open } = setup()
  const box = open([...rows].reverse(), async () => {})
  const body = box.content.type.render()
  const columns = body.children.find(node => node?.props?.class === 'upload-confirm-columns')
  const sections = columns.children
  assert.deepEqual(Array.from(sections, node => text(node.children[0])), ['存储与生效', '审批与下发'])
  assert.match(text(body), /长文件名称.docx/)
  assert.match(text(sections[0]), /测试项目 \/ G44文档/)
  assert.match(text(sections[0]), /培训要求 不需要培训/)
  const supplement = body.children.find(node => node?.props?.class === 'upload-confirm-supplement')
  assert.match(text(supplement), /关联文件（2）.*甲.pdf.*乙.docx/)
  assert.equal(supplement.children.some(node => node.type === 'details'), false)
  assert.doesNotMatch(text(supplement), /未上传/)
  const approvalDetails = sections[1].children.find(node => node.type === 'details')
  assert.deepEqual(Array.from(sections[1].children.filter(node => node.props?.class === 'upload-confirm-field'),
    node => node.children[0].children), ['会签部门', '批准人', '下发部门'])
  assert.match(text(approvalDetails), /账号ID 901.*原批准组 99/)
  assert.equal(approvalDetails.props?.open, undefined)
  const rules = sections[0].children.find(node => node.type === 'details')
  assert.equal(rules.children[0].children, '生效及下发规则')
  assert.match(text(rules), /文控审核签名完成时生效.*受控且已生效后向全部人员下发/)
  assert.equal(rules.props?.open, undefined)
  assert.equal(body.children.at(-1).children[0].children, '其他提交信息')
  assert.equal(box.options.cancelButtonText, '返回修改')
  assert.equal(box.options.showClose, false)
})

test('all file formats share the same icon and long names retain their ending and full accessible name', () => {
  const { open } = setup()
  let iconPath
  for (const extension of ['.docx', '.pdf', '.xlsx']) {
    const fileName = '这是一份很长的需要从中间省略的受控文件名称'.repeat(4) + extension
    const body = open(rows.map(row => row.label === '完整文件名称（含后缀）' ? { ...row, value: fileName } : row), async () => {}).content.type.render()
    const summary = body.children.find(node => node?.props?.class === 'upload-confirm-file-summary')
    const icon = summary.children[0]
    assert.equal(icon.type, 'svg')
    if (iconPath) assert.equal(icon.children[0].props.d, iconPath)
    iconPath = icon.children[0].props.d
    const name = summary.children[1].children[0]
    assert.equal(name.props.title, fileName)
    assert.equal(name.props['aria-label'], fileName)
    assert.equal(name.children.map(node => node.children).join(''), fileName)
    assert.ok(name.children[1].children.endsWith(extension))
  }
})

test('no supplementary data is combined into one message; notes and attachments are shown directly when present', () => {
  const { open } = setup()
  const emptyRows = rows.filter(row => !['关联文件', '附件'].includes(row.label))
  const body = open([...emptyRows, { label: '提交备注', value: '未填写', empty: true }], async () => {}).content.type.render()
  assert.match(text(body), /无补充材料/)
  const populated = open([...emptyRows, { label: '附件', value: '补充.pdf', items: ['补充.pdf'] },
    { label: '提交备注', value: '请重点核对生产部门' }], async () => {}).content.type.render()
  const supplement = populated.children.find(node => node?.props?.class === 'upload-confirm-supplement')
  assert.match(text(supplement), /附件（1）.*补充.pdf.*请重点核对生产部门/)
  assert.doesNotMatch(text(populated), /无补充材料/)
  const literalRemark = open([...emptyRows, { label: '提交备注', value: '未填写', empty: false }], async () => {}).content.type.render()
  assert.doesNotMatch(text(literalRemark), /无补充材料/)
  assert.match(text(literalRemark), /提交备注 未填写/)
})

test('return closes without submitting; in-flight confirm and close cannot repeat or interrupt submission', async () => {
  const { open } = setup(); let writes = 0; let finish; let closes = 0
  const box = open(rows, () => { writes++; return new Promise(resolve => { finish = resolve }) })
  // Element Plus supplies mutable MessageBoxState; showClose is an option/prop, not state.
  const instance = Object.seal({ action: 'cancel', confirmButtonText: '', confirmButtonLoading: false, showCancelButton: true })
  const done = () => closes++
  await box.options.beforeClose('cancel', instance, done)
  assert.equal(writes, 0); assert.equal(closes, 1)
  const pending = box.options.beforeClose('confirm', instance, done)
  assert.equal(instance.confirmButtonText, '提交中'); assert.equal(instance.confirmButtonLoading, true)
  await box.options.beforeClose('confirm', instance, done)
  // The real handleAction updates action before beforeClose, including during an outstanding submit.
  instance.action = 'close'
  await box.options.beforeClose('close', instance, done)
  assert.equal(writes, 1); assert.equal(closes, 1)
  finish(); await pending
  assert.equal(closes, 2)
  assert.equal(instance.action, 'confirm')
})

test('failed submit keeps the dialog and snapshot, exposes reason and allows a successful retry', async () => {
  const { open } = setup(); let writes = 0; let closes = 0
  const box = open(rows, async () => { if (++writes === 1) throw new Error('文件已被占用，请释放后重试') })
  const instance = {}; const original = JSON.stringify(rows)
  await box.options.beforeClose('confirm', instance, () => closes++)
  assert.equal(closes, 0); assert.equal(instance.confirmButtonLoading, false)
  assert.equal(instance.confirmButtonText, '确认提交'); assert.equal(instance.showCancelButton, true)
  assert.match(text(box.content.type.render()), /文件已被占用，请释放后重试/)
  assert.equal(JSON.stringify(rows), original)
  await box.options.beforeClose('confirm', instance, () => closes++)
  assert.equal(writes, 2); assert.equal(closes, 1)
})
