const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

function loadDeclarations(file, names, context) {
  const source = parse(fs.readFileSync(file, 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(file + '.ts', source, ts.ScriptTarget.Latest, true)
  const code = ast.statements.filter(s => s.declarationList?.declarations.some(d => names.includes(d.name?.text)))
    .map(s => s.getText(ast)).join('\n')
  for (const name of names) assert.ok(code.includes(name), `missing ${name}`)
  vm.runInNewContext(ts.transpileModule(code + '\nObject.assign(this, {' + names.join(',') + '})', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context
}
const approval = loadDeclarations('src/views/approval-center/index.vue', [
  'EMPTY_APPROVAL_DISPLAY', 'APPROVAL_STATUS_LABELS', 'ENGLISH_LETTER_PATTERN',
  'CHINESE_CHARACTER_PATTERN', 'normalizeApprovalDisplayText', 'containsEnglishLetters',
  'containsChineseCharacters', 'resolveMappedApprovalText', 'resolveReviewerLabel', 'resolveNodeSubLabel'
], {})
const history = loadDeclarations('src/views/dcc/controlled-file/detail/index.vue', ['getVersionApprovalResultText'], {
  getDetailStatusLabel: status => ({ CONTROLLED_PENDING_EFFECTIVE: '受控（待生效）',
    PENDING_DELAYED_EFFECTIVE_CONFIRM: '待延迟生效确认', ACTIVE: '现行' })[status] || status,
  formatControlledFileDateTime: value => String(value)
}).getVersionApprovalResultText

test('delayed confirmation uses the formal file status without changing reviewer or unknown handling', () => {
  assert.equal(approval.resolveNodeSubLabel({businessStatus: 'PENDING_DELAYED_EFFECTIVE_CONFIRM'}), '待延迟生效确认')
  assert.equal(approval.resolveNodeSubLabel({businessStatus: 'PENDING_DELAYED_EFFECTIVE_CONFIRM', assigneeUserName: '申请人甲'}), '审核人：申请人甲')
  assert.equal(approval.resolveNodeSubLabel({businessStatus: 'UNKNOWN_DCC_STAGE'}), '未配置中文状态')
})
test('controlled pending history does not turn publication or approval into effectiveness', () => {
  const row = Object.freeze({status: 'CONTROLLED_PENDING_EFFECTIVE', publishedTime: '2026-10-20 10:45:26', approvedTime: '2026-10-20 10:44:00'})
  assert.equal(history(row), '受控（待生效）')
})
test('confirmation pending history preserves its formal status even with publication metadata', () => {
  assert.equal(history({status: 'PENDING_DELAYED_EFFECTIVE_CONFIRM', publishedTime: 'published', approvedTime: 'approved'}), '待延迟生效确认')
})
test('effective history displays the actual effective time rather than the earlier publication time', () => {
  const row = Object.freeze({status: 'ACTIVE', effectiveTime: '2026-10-20 11:00:00', publishedTime: '2026-10-20 10:45:26'})
  assert.equal(history(row), '已生效：2026-10-20 11:00:00')
})
test('publication and approval alone retain their separate meanings', () => {
  assert.equal(history({status: 'ACTIVE', publishedTime: 'published'}), '已发布：published')
  assert.equal(history({status: 'ACTIVE', approvedTime: 'approved'}), '已批准：approved')
  assert.equal(history({status: 'ACTIVE'}), '现行')
})
test('failure and rejection remain higher priority than lifecycle facts', () => {
  assert.equal(history({status: 'CONTROLLED_PENDING_EFFECTIVE', finalizationError: '签章失败', publishedTime: 'published'}), '生效失败：签章失败')
  assert.equal(history({status: 'CONTROLLED_PENDING_EFFECTIVE', rejectReason: '内容错误', approvedTime: 'approved'}), '审批驳回：内容错误')
})
