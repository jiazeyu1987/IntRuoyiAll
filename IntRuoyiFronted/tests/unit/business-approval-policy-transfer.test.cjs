const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const file = path.resolve(__dirname, '../../src/views/bpm/businessApprovalPolicy/policyTransfer.ts')
const ctx = { exports: {}, Error }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, ctx)
const parse = ctx.exports.parsePolicyTransfer
const policy = {
  dataDomain: 'MES', systemCode: 'MES', objectType: 'ROUTE_VERSION', actionCode: 'PUBLISH',
  objectState: 'DRAFT', policyMode: 'BPM_REQUIRED', processDefinitionKey: 'mes-route-version-approval-v1',
  effectExecutorCode: 'MES_ROUTE_VERSION_PUBLISH', status: 'PUBLISHED',
  formPolicyType: null, formSlotsJson: ' [{"value":"2116486795512508425"}] ', remark: ' 原备注\n中文 '
}
const process = { key: policy.processDefinitionKey, name: '工艺审批', category: { code: 'MES' },
  bpmnXml: '<definitions/>', metaInfo: { type: 10 }, roles: [], startUsernames: [] }
const pack = policies => ({ schemaVersion: 2, format: 'BUSINESS_APPROVAL_POLICIES', policies,
  processes: policies.some(p => p?.status === 'PUBLISHED' && p?.policyMode === 'BPM_REQUIRED') ? [process] : [] })

test('file parsing preserves all fields, whitespace, nested JSON text and every policy status', () => {
  const value = pack(['DRAFT', 'PUBLISHED', 'DISABLED'].map(status => ({ ...policy, status })))
  assert.equal(JSON.stringify(parse(JSON.stringify(value))), JSON.stringify(value))
})
test('malformed files, unsupported schema and incomplete business configuration are rejected', () => {
  for (const value of [null, {}, pack([]), { ...pack([policy]), schemaVersion: 3 },
    pack([null]), pack([{ ...policy, policyMode: 'FALLBACK' }]), pack([{ ...policy, objectType: '' }]),
    pack([{ ...policy, formSlotsJson: {} }]), pack([{ ...policy, status: 'UNKNOWN' }])]) {
    assert.throws(() => parse(JSON.stringify(value)))
  }
  assert.throws(() => parse('not json'))
})
test('source database identities, tenant selectors and unknown fields cannot be silently imported', () => {
  assert.throws(() => parse(JSON.stringify({ ...pack([policy]), tenantId: '101' })))
  for (const key of ['id', 'tenantId', 'updatedTime', 'unexpected']) {
    assert.throws(() => parse(JSON.stringify(pack([{ ...policy, [key]: '101' }]))))
  }
})

function actions(api) {
  const component = fs.readFileSync(path.resolve(__dirname,
    '../../src/views/bpm/businessApprovalPolicy/PolicyTransferActions.vue'), 'utf8')
  const script = component.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const revision = { value: 0 }
  const emitted = []
  const downloads = []
  const messages = []
  const dependencies = {
    vue: { ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }), watch() {} },
    '@/api/bpm/businessApprovalPolicy': { BusinessApprovalPolicyApi: api },
    '@/utils/permission': { checkPermi: () => true },
    '@/utils/auth': { authContextRevision: revision },
    '@/utils/download': { default: { json: (blob, name) => downloads.push({ blob, name }) } },
    './policyTransfer': { parsePolicyTransfer: parse }
  }
  const context = {
    exports: {}, Blob, Error,
    require: name => { assert.ok(name in dependencies, name); return dependencies[name] },
    defineEmits: () => event => emitted.push(event),
    useMessage: () => ({ success: message => messages.push(message), error: message => messages.push(message) })
  }
  vm.runInNewContext(ts.transpileModule(script +
    '\nexports.actions = {readFile, exportPolicies, importing};', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return { ...context.exports.actions, revision, emitted, downloads, messages }
}
const fileEvent = value => ({ target: { value: 'file', files: [{ name: '审批策略.json', text: async () => JSON.stringify(value) }] } })

test('selecting a file imports it unchanged without confirmation and refreshes after server success', async () => {
  const value = pack([policy])
  let posted
  const p = actions({ importPolicies: async (data, guard) => {
    assert.equal(guard(), true)
    posted = JSON.stringify(data)
    return 1
  } })
  await p.readFile(fileEvent(value))
  assert.equal(posted, JSON.stringify(value))
  assert.deepEqual(p.emitted, ['imported'])
  assert.equal(p.importing.value, false)
  assert.match(p.messages[0], /已按原配置导入1条审批策略/)
})
test('changing tenant while reading the file prevents import and produces an explicit error', async () => {
  let called = false
  const p = actions({ importPolicies: async () => { called = true } })
  const event = fileEvent(pack([policy]))
  event.target.files[0].text = async () => {
    p.revision.value++
    return JSON.stringify(pack([policy]))
  }
  await p.readFile(event)
  assert.equal(called, false)
  assert.match(p.messages[0], /租户或登录状态已变更/)
  assert.deepEqual(p.emitted, [])
})
test('failed import reports the server error and allows selecting the same file again', async () => {
  const p = actions({ importPolicies: async () => { throw new Error('目标租户缺少审批流程') } })
  const event = fileEvent(pack([policy]))
  await p.readFile(event)
  assert.match(p.messages[0], /目标租户缺少审批流程/)
  assert.equal(p.importing.value, false)
  assert.equal(event.target.value, '')
  assert.deepEqual(p.emitted, [])
  assert.equal(p.messages.length, 1)
})
test('invalid files and cancelling selection never submit an import', async () => {
  let called = false
  const p = actions({ importPolicies: async () => { called = true } })
  await p.readFile({ target: { value: '', files: [] } })
  assert.equal(p.messages.length, 0)
  await p.readFile(fileEvent({}))
  assert.equal(called, false)
  assert.equal(p.messages.length, 1)
  assert.deepEqual(p.emitted, [])
  assert.equal(p.importing.value, false)
})
test('pending imports block repeated submissions and refresh only after success', async () => {
  let finish
  let calls = 0
  const p = actions({ importPolicies: () => {
    calls++
    return new Promise(resolve => { finish = resolve })
  } })
  const pending = p.readFile(fileEvent(pack([policy])))
  await Promise.resolve()
  assert.equal(p.importing.value, true)
  assert.deepEqual(p.emitted, [])
  await p.readFile(fileEvent(pack([policy])))
  assert.equal(calls, 1)
  finish(1)
  await pending
  assert.equal(p.importing.value, false)
  assert.deepEqual(p.emitted, ['imported'])
})
test('changing tenant during the request invalidates the guard and never reports success', async () => {
  const p = actions({ importPolicies: async (data, guard) => {
    p.revision.value++
    assert.equal(guard(), false)
    return 1
  } })
  await p.readFile(fileEvent(pack([policy])))
  assert.match(p.messages[0], /导入请求的租户状态已变更/)
  assert.deepEqual(p.emitted, [])
})
test('export downloads the complete server configuration as a reusable JSON package', async () => {
  const value = pack([policy])
  const p = actions({ exportPolicies: async guard => { assert.equal(guard(), true); return value } })
  await p.exportPolicies()
  assert.equal(p.downloads.length, 1)
  assert.deepEqual(JSON.parse(await p.downloads[0].blob.text()), value)
})


test('version1 packages cannot initialize a tenant and require a fresh complete export', () => {
  assert.throws(() => parse(JSON.stringify({schemaVersion: 1, format: 'BUSINESS_APPROVAL_POLICIES', policies: [policy]})), /重新导出/)
})
test('missing, duplicate, unrelated and incomplete dependency processes are rejected', () => {
  for (const processes of [[], [process, process], [{...process, key: 'unrelated'}], [{...process, bpmnXml: ''}], [{...process, roles: [{code: 'r'}]}]]) {
    assert.throws(() => parse(JSON.stringify({...pack([policy]), processes})))
  }
})
