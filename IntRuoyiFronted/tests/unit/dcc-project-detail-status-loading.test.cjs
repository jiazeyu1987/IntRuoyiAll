const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'), 'utf8').replace(/\r\n/g, '\n')
const names = ['formatDccProjectGovernanceStatus', 'loadDccProjectGovernanceStatus',
  'loadQaRegulationStatuses', 'formatQaRegulationStatus', 'getList',
  'openProjectCodeDetail', 'closeProjectCodeDetail']
const declarations = names.map(name => {
  const match = source.match(new RegExp(`const ${name} = [\\s\\S]*?\\n\\}`))
  assert.ok(match, `missing production function ${name}`)
  return match[0]
}).join('\n')
const ref = value => ({ value })
const row = { id: '2121453181393522757', projectName: '测试项目' }
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function host(overrides = {}) {
  const h = {
    governanceStatusLoadState: ref('idle'), governanceStatusError: ref(''),
    dccProjectGovernanceByProjectName: ref({}), qaRegulationStatusLoadState: ref('idle'),
    qaRegulationStatusError: ref(''), qaRegulationStatusPermissionDenied: ref(false),
    qaRegulationStatusByDccProjectCodeId: ref({}),
    getDccProjectGovernanceStatus: async () => [{ projectName: row.projectName, routeStatus: 'MISSING' }],
    QcTemplateApi: { getQaRegulationProjectStatuses: async () => [] }, checkPermi: () => true,
    resolveAiCategoryErrorMessage: e => e.message, queryParams: { pageNo: 4, pageSize: 10, projectCode: 'T2TEST20261115' },
    loading: ref(false), list: ref([]), total: ref(0),
    getProjectCodePage: async () => ({ list: [row], total: 1 }),
    route: { path: '/mes/md/dcc-project-code', query: { projectCodeId: row.id, productMasterId: '55' } },
    isProjectCodeRoute: () => true, resolvePositiveRouteQueryText: x => String(x),
    selectedProjectCode: ref(null), detailDrawerVisible: ref(false), detailLoading: ref(false),
    resetAssociatedFilesState() {}, resetProjectFileTemplateState() {},
    detailCalls: 0, replacements: [], ...overrides
  }
  h.syncDetailFromRoute = async () => { h.detailCalls++ }
  h.router = { replace: async value => { h.replacements.push(value) } }
  vm.createContext(h)
  vm.runInContext(ts.transpile(`let governanceStatusLoadSerial=0,qaRegulationStatusLoadSerial=0,projectListRequestGeneration=0;
    ${declarations}\nObject.assign(globalThis,{${names.join(',')}})`,
  { target: ts.ScriptTarget.ES2022 }), h)
  return h
}

test('only explicit MISSING displays unconfigured; missing data and loading remain distinct', async () => {
  const pending = deferred(), h = host({ getDccProjectGovernanceStatus: () => pending.promise })
  const load = h.loadDccProjectGovernanceStatus([row])
  assert.equal(h.formatDccProjectGovernanceStatus(), '加载中')
  pending.resolve([{ projectName: row.projectName, routeStatus: 'MISSING' }]); await load
  assert.equal(h.formatDccProjectGovernanceStatus('MISSING'), '未配置')
  assert.equal(h.formatDccProjectGovernanceStatus(), '状态未知')
  assert.equal(h.formatDccProjectGovernanceStatus('OK'), '已配置')
  assert.equal(h.formatDccProjectGovernanceStatus('DUPLICATE'), '重复')
})
test('governance failure clears old values, exposes the real error and can be retried', async () => {
  const h = host({ getDccProjectGovernanceStatus: async () => { throw Error('HTTP 400 参数错误') } })
  h.dccProjectGovernanceByProjectName.value = { old: { routeStatus: 'OK' } }
  await h.loadDccProjectGovernanceStatus([row])
  assert.equal(h.formatDccProjectGovernanceStatus('OK'), '加载失败')
  assert.equal(h.governanceStatusError.value, 'HTTP 400 参数错误')
  assert.equal(Object.keys(h.dccProjectGovernanceByProjectName.value).length, 0)
  h.getDccProjectGovernanceStatus = async () => [{ projectName: row.projectName, routeStatus: 'OK' }]
  await h.loadDccProjectGovernanceStatus([row])
  assert.equal(h.governanceStatusError.value, '')
  assert.equal(h.dccProjectGovernanceByProjectName.value[row.projectName].routeStatus, 'OK')
})
test('an old failed request cannot overwrite a newer successful page', async () => {
  const old = deferred(), h = host({ getDccProjectGovernanceStatus: () => old.promise })
  const first = h.loadDccProjectGovernanceStatus([row])
  h.getDccProjectGovernanceStatus = async () => [{ projectName: row.projectName, routeStatus: 'OK' }]
  await h.loadDccProjectGovernanceStatus([row]); old.reject(Error('old page failed')); await first
  assert.equal(h.governanceStatusLoadState.value, 'loaded')
  assert.equal(h.governanceStatusError.value, '')
})
test('QA permission denial, request failure and successful empty data are different states', async () => {
  const h = host({ checkPermi: () => false })
  await h.loadQaRegulationStatuses([row]); assert.equal(h.formatQaRegulationStatus(), '无查询权限')
  h.checkPermi = () => true
  h.QcTemplateApi.getQaRegulationProjectStatuses = async () => { throw Error('QA request failed') }
  await h.loadQaRegulationStatuses([row]); assert.equal(h.formatQaRegulationStatus(), '加载失败')
  assert.equal(h.qaRegulationStatusError.value, 'QA request failed')
  h.QcTemplateApi.getQaRegulationProjectStatuses = async () => []
  await h.loadQaRegulationStatuses([row]); assert.equal(h.formatQaRegulationStatus(), '状态未知')
  assert.equal(h.formatQaRegulationStatus({ configured: false, productionReady: false }), '未配置')
})
test('project list completes while governance is still pending so detail initialization can proceed', async () => {
  const pending = deferred(), h = host({ getDccProjectGovernanceStatus: () => pending.promise })
  assert.equal(await h.getList(), true)
  assert.equal(h.loading.value, false); assert.equal(h.list.value[0].id, row.id)
  assert.equal(h.governanceStatusLoadState.value, 'loading')
  pending.resolve([])
})
test('opening and closing detail preserves the list route, filter and pagination', async () => {
  const h = host(); delete h.route.query.projectCodeId
  await h.openProjectCodeDetail(row)
  assert.equal(h.replacements[0].path, '/mes/md/dcc-project-code')
  assert.equal(h.replacements[0].query.projectCodeId, row.id)
  assert.equal(h.queryParams.pageNo, 4); assert.equal(h.queryParams.projectCode, 'T2TEST20261115')
  h.route.query = { ...h.replacements[0].query, associatedFocus: 'files', associatedFileId: '42' }
  await h.closeProjectCodeDetail()
  assert.deepEqual(Object.keys(h.replacements[1].query), ['productMasterId'])
  assert.equal(h.queryParams.pageNo, 4)
  await h.openProjectCodeDetail(row); assert.equal(h.detailCalls, 1)
})
