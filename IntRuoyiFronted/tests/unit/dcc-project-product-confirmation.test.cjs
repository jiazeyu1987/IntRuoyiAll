const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')

const source = fs.readFileSync('src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue', 'utf8')
const handler = source.slice(source.indexOf('const submitProjectProductRequest ='), source.indexOf('const handleProjectProductAction ='))
const reviewer = { configured: true, enabled: true, reviewerUserId: '9223372036854775707', reviewerUsername: 'quality', reviewerNickname: '质量审核' }
const attrs = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
function host({ confirm = async () => {}, configuration = async () => ({ ...reviewer }), rejectedId } = {}) {
  const writes = [], confirmations = [], successes = []
  const context = {
    projectProductLoading: { value: false }, projectProductError: { value: '' },
    projectProductFormRef: { value: { validate: async () => true } },
    projectProductDialogVisible: { value: true }, projectProductMode: { value: 'create' },
    projectProductResubmitRequestId: { value: rejectedId }, reviewerConfiguration: { value: undefined },
    projectProductForm: { projectName: '项目A', projectCode: 'PA', projectLeaderUserId: 7, projectLeader: '项目负责人',
      folderTemplateId: 9, defaultAttributes: structuredClone(attrs), productCode: 'P-01', productName: '产品A',
      classification: '二类', creationReason: '新项目申请', resubmissionReason: rejectedId ? '修正后重提' : undefined, remark: '备注' },
    projectLeaderUsers: { value: [{ id: 7, nickname: '项目负责人', username: 'leader' }] },
    folderTemplates: { value: [{ id: 9, name: '模板A', active: true, structureJson: '{"nodes":[]}' }] },
    validateAttributes: value => value,
    requireConfiguredReviewer: value => { if (!value.configured || !value.enabled) throw new Error('审核配置无效'); return value },
    getDccProjectReviewerConfiguration: configuration,
    confirmProjectProductApplication: async (...values) => { confirmations.push(values); await confirm(context) },
    createDccProjectProductRequest: async value => writes.push({ value }),
    resubmitDccProjectProductRequest: async (id, value) => writes.push({ id, value }),
    message: { success: value => successes.push(value) }, resetProjectProductForm() {}, loadProjectProductRequests: async () => {},
    Error, JSON
  }
  vm.runInNewContext(ts.transpileModule(handler + '\n globalThis.submit = submitProjectProductRequest', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return { context, writes, confirmations, successes, submit: context.submit }
}

test('cancel at the project/product review sends no create and retains input', async () => {
  const h = host({ confirm: async () => { throw 'cancel' } })
  await h.submit()
  assert.equal(h.writes.length, 0)
  assert.equal(h.confirmations.length, 1)
  assert.equal(h.context.projectProductForm.productName, '产品A')
  assert.equal(h.context.projectProductError.value, '')
  assert.equal(h.context.projectProductLoading.value, false)
})
test('confirmed rejected request cannot drift to another request while review is open', async () => {
  const h = host({ rejectedId: '9007199254740993', confirm: async c => { c.projectProductResubmitRequestId.value = '9007199254740995' } })
  await h.submit()
  assert.equal(h.writes.length, 0)
  assert.match(h.context.projectProductError.value, /变化|确认/)
})
test('reviewer changed after confirmation is displayed and must be reconfirmed before any write', async () => {
  let reads = 0
  const h = host({ configuration: async () => reads++ ? { ...reviewer, reviewerUserId: '8', reviewerUsername: 'new-review', reviewerNickname: '新审核人' } : { ...reviewer } })
  await h.submit()
  assert.equal(h.confirmations.length, 1)
  assert.equal(h.writes.length, 0)
  assert.match(h.context.projectProductError.value, /审核.*变化|审核.*确认/)
  assert.equal(h.context.reviewerConfiguration.value.reviewerUserId, '8')
})
test('unchanged confirmed request submits exactly its captured original payload once', async () => {
  const h = host({ rejectedId: '9007199254740993' })
  const original = JSON.stringify(h.context.projectProductForm)
  await h.submit()
  assert.equal(h.confirmations.length, 1)
  assert.equal(h.writes.length, 1)
  assert.equal(h.writes[0].id, '9007199254740993')
  assert.equal(JSON.stringify(h.writes[0].value), original)
  assert.equal(h.successes.length, 1)
})

test('changing actual project attributes or closing the form while confirming sends no request', async () => {
  for (const change of [c => { c.projectProductForm.defaultAttributes.targetMarkets = ['FDA'] },
    c => { c.folderTemplates.value[0].name = '修改后的模板' },
    c => { c.projectLeaderUsers.value[0].nickname = '变更后的负责人信息' },
    c => { c.projectProductDialogVisible.value = false }]) {
    const h = host({ confirm: async c => change(c) })
    await h.submit()
    assert.equal(h.writes.length, 0)
    assert.match(h.context.projectProductError.value, /变化|确认/)
  }
})
test('a pending confirmation blocks repeated submission and acknowledges only the real write', async () => {
  let release
  const h = host({ confirm: () => new Promise(resolve => { release = resolve }) })
  const pending = h.submit()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(h.context.projectProductLoading.value, true)
  await h.submit()
  assert.equal(h.confirmations.length, 1)
  assert.equal(h.writes.length, 0)
  release()
  await pending
  assert.equal(h.writes.length, 1)
})

function typedModule(file, resolve) {
  const sandbox = { exports: {}, require: resolve, Error, JSON }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, sandbox)
  return sandbox.exports
}
function confirmationHelper(confirm) {
  const configuration = typedModule('src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts', () => { throw Error('unexpected dependency') })
  const attributes = typedModule('src/views/dcc/controlled-file/project-attributes/state.ts', () => { throw Error('unexpected dependency') })
  return typedModule('src/views/dcc/controlled-file/basic-data/components/project-product-confirmation.ts', name => {
    if (name === 'vue') return require('vue')
    if (name === 'element-plus') return { ElMessageBox: { confirm } }
    if (name === './project-reviewer') return configuration
    if (name === '../../project-attributes/state') return attributes
    throw Error(name)
  }).confirmProjectProductApplication
}
test('actual review summary uses safe Vue text and includes exact configured account and three attributes', async () => {
  let body, options
  const confirm = confirmationHelper(async (message, title, config) => { body = message; options = config })
  const h = host()
  h.context.projectProductForm.productName = '<img src=x onerror=alert(1)>'
  h.context.projectProductForm.defaultAttributes = { ...attrs, targetMarkets: ['OTHER'], otherMarket: '目标市场说明', documentTransfer: 'Y', transferTo: '目标项目' }
  await confirm(h.context.projectProductForm, h.context.projectLeaderUsers.value, h.context.folderTemplates.value, reviewer, false)
  const texts = []
  function visit(node) {
    if (typeof node === 'string') { texts.push(node); return }
    if (!node || typeof node !== 'object') return
    assert.equal(node.props?.innerHTML, undefined)
    if (Array.isArray(node.children)) node.children.forEach(visit)
    else if (typeof node.children === 'string') texts.push(node.children)
  }
  visit(body)
  const rendered = texts.join(' ')
  for (const value of ['项目A', 'PA', 'P-01', '<img src=x onerror=alert(1)>', '模板A', '是否为注册人', '是否为生产方', '目标市场说明', '目标项目', reviewer.reviewerUserId, reviewer.reviewerUsername, '备注', '新项目申请'])
    assert.ok(rendered.includes(value), value)
  assert.equal(options.confirmButtonText, '提交申请')
  assert.equal(options.cancelButtonText, '返回修改')
})
test('missing leader or disabled folder template fails before opening the review', async () => {
  let opened = 0
  const confirm = confirmationHelper(async () => { opened++ })
  const h = host()
  await assert.rejects(confirm(h.context.projectProductForm, [], h.context.folderTemplates.value, reviewer, false), /负责人|模板/)
  await assert.rejects(confirm(h.context.projectProductForm, h.context.projectLeaderUsers.value, [{ id: 9, name: '模板A', active: false }], reviewer, false), /负责人|模板/)
  await assert.rejects(confirm(h.context.projectProductForm, h.context.projectLeaderUsers.value, h.context.folderTemplates.value, { ...reviewer, enabled: false }, false), /启用/)
  assert.equal(opened, 0)
})
