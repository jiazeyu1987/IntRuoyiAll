const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workstationApi = read('src/api/mes/md/workstation/index.ts')
const workstationPage = read('src/views/mes/md/workstation/index.vue')
const workstationSelect = read('src/views/mes/md/workstation/components/MdWorkstationSelect.vue')
const workstationSelectDialog = read(
  'src/views/mes/md/workstation/components/MdWorkstationSelectDialog.vue'
)
const routeProcessList = read('src/views/mes/pro/route/RouteProcessList.vue')
const routeFlowGraph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const edhrTrackingApi = read('src/api/mes/pro/edhr/tracking.ts')
const edhrTrackingPage = read('src/views/mes/pro/edhr/TrackingPage.vue')
const formTraceAuditTab = read('src/views/mes/pro/edhr/form-trace/FormTraceAuditTab.vue')

function assertIncludes(source, expected, label) {
  assert.ok(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, unexpected, label) {
  assert.ok(!source.includes(unexpected), `${label}: unexpected ${JSON.stringify(unexpected)}`)
}

function assertPattern(source, pattern, label) {
  assert.match(source, pattern, label)
}

for (const [label, source] of [
  ['workstation page', workstationPage],
  ['workstation select', workstationSelect],
  ['workstation select dialog', workstationSelectDialog],
  ['route process list', routeProcessList],
  ['route flow graph', routeFlowGraph],
  ['eDHR tracking page', edhrTrackingPage],
  ['form trace audit tab', formTraceAuditTab]
]) {
  assertIncludes(source, 'normalizePositiveProcessId', `${label} must use a positive process id guard`)
}

assertPattern(
  workstationApi,
  /const normalizePositiveIdParam = \(value: unknown\) =>[\s\S]*Number\.isFinite\(parsed\) && parsed > 0 \? parsed : undefined/,
  'workstation API must normalize non-positive processId before request'
)
assertIncludes(
  workstationApi,
  'params: buildWorkstationPageParams(params)',
  'workstation page API must send normalized params'
)
assertPattern(
  workstationApi,
  /processId:\s*normalizePositiveIdParam\(params\?\.processId\)/,
  'workstation API processId must be positive-only'
)
assertNotIncludes(
  workstationApi,
  'request.get({ url: `/mes/md-workstation/page`, params })',
  'workstation API must not send raw page params'
)

assertPattern(
  workstationPage,
  /processId:\s*normalizePositiveProcessId\(queryParams\.processId\)/,
  'workstation main list must normalize processId in page params'
)
assertPattern(
  workstationPage,
  /queryParams\.processId = normalizePositiveProcessId\(processIdText\)/,
  'workstation route query processId must ignore zero sentinel values'
)

assertIncludes(
  workstationSelect,
  ':process-id="normalizedProcessId"',
  'workstation select must pass normalized processId to dialog'
)
assertPattern(
  workstationSelect,
  /const normalizedProcessId = computed\(\(\) => normalizePositiveProcessId\(props\.processId\)\)/,
  'workstation select normalizedProcessId must derive from props'
)

assertIncludes(
  workstationSelectDialog,
  'MdWorkstationApi.getWorkstationPage(buildWorkstationDialogPageParams())',
  'workstation select dialog must send normalized page params'
)
assertPattern(
  workstationSelectDialog,
  /queryParams\.processId = normalizePositiveProcessId\(props\.processId\)/,
  'workstation select dialog must normalize external processId'
)
assertNotIncludes(
  workstationSelectDialog,
  'MdWorkstationApi.getWorkstationPage(queryParams)',
  'workstation select dialog must not send raw queryParams'
)

for (const [label, source] of [
  ['route process list', routeProcessList],
  ['route flow graph', routeFlowGraph]
]) {
  assertIncludes(
    source,
    '...buildWorkstationProcessQuery(row.processId)',
    `${label} must omit processId=0 when navigating to workstation list`
  )
  assertNotIncludes(
    source,
    'processId: String(row.processId)',
    `${label} must not serialize raw row.processId`
  )
}

assertPattern(
  edhrTrackingApi,
  /processId:\s*normalizePositiveIdParam\(params\.processId\)/,
  'eDHR tracking API must normalize processId before request'
)
assertIncludes(
  edhrTrackingApi,
  'params: buildEdhrTrackingPageParams(params)',
  'eDHR tracking API must send normalized params'
)
assertPattern(
  edhrTrackingPage,
  /processId:\s*normalizePositiveProcessId\(queryParams\.processId\)/,
  'eDHR tracking page must ignore processId=0'
)
assertPattern(
  formTraceAuditTab,
  /processId:\s*normalizePositiveProcessId\(queryParams\.processId\)/,
  'form trace audit tab must ignore processId=0'
)
assertNotIncludes(
  edhrTrackingPage,
  'processId: Number.isFinite(queryParams.processId) ? queryParams.processId : undefined',
  'eDHR tracking page must not treat zero as a valid process filter'
)
assertNotIncludes(
  formTraceAuditTab,
  'processId: Number.isFinite(queryParams.processId) ? queryParams.processId : undefined',
  'form trace audit tab must not treat zero as a valid process filter'
)

console.log('PASS: processId=0 frontend guards static contract')
