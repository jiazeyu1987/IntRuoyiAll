const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
function moduleFrom(file, resolve = name => { throw new Error(name) }) {
  const sandbox = { exports: {}, Error, BigInt, URL, Set, window: { location: { origin: 'https://dcc.local' } }, require: resolve }
  vm.runInNewContext(ts.transpileModule(read(file), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, sandbox)
  return sandbox.exports
}
function workflowApi(metadata, postResult = true) {
  const calls = []
  const content = read('src/api/dcc/controlledFile/workflow.ts').replace(/import\.meta\.env/g, '({})')
  const sandbox = { exports: {}, Error, BigInt, URL, Set, Date, window: {}, require: name => {
    if (name === '@/config/axios') return { default: {
      get: async request => { calls.push(request); return metadata },
      post: async request => { calls.push(request); return postResult }
    } }
    if (name === '@/config/axios/config') return { config: { base_url: '/admin-api' } }
    if (name === '@/utils/auth') return { getTenantId: () => 1, getVisitTenantId: () => null, getAccessToken: () => 'isolated-test-token' }
    if (name === '@/views/dcc/controlled-file/project-attributes/state') return moduleFrom('src/views/dcc/controlled-file/project-attributes/state.ts')
    if (name === 'axios' || name === 'element-plus' || name === '@/utils/filt') return {}
    throw new Error(name)
  } }
  vm.runInNewContext(ts.transpileModule(content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, sandbox)
  return { api: sandbox.exports, calls }
}
const metadata = () => ({ controlledFileId: params.controlledFileId, distributionId: params.distributionId,
  recipientId: params.recipientId, versionNo: params.version, approvalProcessInstanceId: params.approvalProcessInstanceId,
  previewKind: 'PDF', fileName: '正文.pdf', contentType: 'application/pdf', viewerToken: 'signed-viewer-token',
  viewerTokenId: 'token-id', viewerTokenNonce: 'nonce', accessEventCode: 'event', watermarkTraceCode: 'watermark',
  watermark: { label: '受控', text: 'watermark', actorName: '接收人', actorAccount: 'account', timestamp: 'real-time', purpose: 'preview', overlay: {} } })
const identity = moduleFrom('src/utils/dccDistributionNavigation.ts')
const params = { controlledFileId: '9223372036854775000', distributionId: '9223372036854775001', recipientId: '9223372036854775002', version: 'B/1', approvalProcessInstanceId: 'real-round' }
const navigation = () => moduleFrom('src/utils/notifyMessageNavigation.ts', name => {
  if (name.endsWith('dccDistributionNavigation')) return identity
  if (name.includes('dccOfflineTrainingRecord')) return {}
  if (name.includes('edhrWorkTaskNavigation')) return { EDHR_WORK_TASK_NOTIFY_PATHS: new Set() }
  throw new Error(name)
})
test('distribution notification navigates with exact version and all task identities', async () => {
  const api = navigation(), calls = []
  const target = api.getNotifyMessageTarget({ templateCode: 'dcc_distribution', templateParams: params })
  assert.equal(target?.type, 'dccDistribution')
  await api.navigateToNotifyMessageTarget({ push: async location => calls.push(location) }, target)
  assert.equal(calls[0].params.id, params.controlledFileId)
  assert.equal(calls[0].query.distributionId, params.distributionId)
  assert.equal(calls[0].query.recipientId, params.recipientId)
  assert.equal(calls[0].query.mode, 'distribution-read')
})
test('invalid or historical distribution message cannot fall through to an ordinary file route', () => {
  for (const bad of [{ ...params, recipientId: undefined }, { ...params, distributionId: Number(params.distributionId) },
    { ...params, controlledFileId: '9223372036854775808' }, { ...params, approvalProcessInstanceId: '' }]) {
    const target = navigation().getNotifyMessageTarget({ templateCode: 'dcc_distribution', templateParams: {
      ...bad, followupUrl: '/dcc/controlled-file/detail/1?viewer=1&from=notification' } })
    assert.equal(target, null)
  }
})
test('route identity rejects half contexts, arrays and incompatible entry modes', () => {
  const query = identity.dccDistributionReadLocation(identity.readDccDistributionNotification(params)).query
  assert.equal(identity.readDccDistributionRoute(params.controlledFileId, query).versionNo, 'B/1')
  for (const patch of [{ recipientId: undefined }, { recipientId: [params.recipientId] }, { mode: 'manage' }, { viewer: '0' }])
    assert.throws(() => identity.readDccDistributionRoute(params.controlledFileId, { ...query, ...patch }))
})
test('actual preview wrapper passes task identity and rejects a foreign formal response', async () => {
  const task = identity.readDccDistributionNotification(params)
  const state = workflowApi(metadata())
  const result = await state.api.getControlledFilePreviewMetadata(task.controlledFileId, task)
  assert.equal(state.calls[0].params.recipientId, task.recipientId)
  assert.equal(result.controlledFileId, task.controlledFileId)
  await assert.rejects(workflowApi({ ...metadata(), recipientId: '1' }).api.getControlledFilePreviewMetadata(task.controlledFileId, task), /接收义务/)
})
test('an explicitly unavailable preview exposes its formal reason and never requires fabricated token fields', async () => {
  const response = { ...metadata(), previewKind: 'OFFICE', previewUnavailableReason: '本次通知 Office 任务凭证未配置' }
  delete response.viewerToken
  await assert.rejects(workflowApi(response).api.getControlledFilePreviewMetadata(params.controlledFileId,
    identity.readDccDistributionNotification(params)), /Office 任务凭证未配置/)
})
test('authenticated Office opened command binds the precise task and complete existing preview evidence', async () => {
  const task=identity.readDccDistributionNotification(params), proof={...metadata(),previewKind:'OFFICE'}
  const state=workflowApi(null)
  assert.equal(await state.api.confirmDistributionOfficeOpened(task,proof),true)
  assert.ok(state.calls[0].url.endsWith(`/recipients/${task.recipientId}/office-body-opened`))
  assert.equal(state.calls[0].data.viewerToken,proof.viewerToken)
  assert.equal(state.calls[0].data.accessEventCode,proof.accessEventCode)
  await assert.rejects(workflowApi(null,false).api.confirmDistributionOfficeOpened(task,proof),/保存结果/)
})
test('metadata-only or incomplete Office proof cannot write a human opened confirmation',async()=>{
  const task=identity.readDccDistributionNotification(params)
  for(const patch of [{viewerToken:undefined},{accessEventCode:''},{viewerTokenNonce:null},{previewKind:'PDF'}]) {
    const state=workflowApi(null)
    await assert.rejects(state.api.confirmDistributionOfficeOpened(task,{...metadata(),previewKind:'OFFICE',...patch}))
    assert.equal(state.calls.length,0)
  }
})
test('task-only detail consumes preview and signature sources without loading ordinary history', () => {
  const source = read('src/views/dcc/controlled-file/detail/index.vue')
  assert.match(source, /<DistributionReadPanel[\s\S]*?v-if="distributionReadMode"/)
  const reload = source.slice(source.indexOf('const reloadAll = async () => {'), source.indexOf('const reloadAll = async () => {') + 450)
  assert.match(reload, /if \(distributionReadMode\.value\)/)
  const panel = read('src/views/dcc/controlled-file/detail/DistributionReadPanel.vue')
  assert.match(panel, /readDccDistributionRoute/)
  assert.match(panel, /DistributionReceiptConfirm/)
  assert.doesNotMatch(panel, /getControlledFile\(|getControlledFileVersionHistory|downloadControlledFile|Date\.now/)
})
