const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const api = read('src/api/mes/pro/processpool/teamLeader.ts')
const page = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const service = read('../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage6/MesStage6IdiSimulationServiceImpl.java')

assert.match(api, /interface Stage6IdiSimulationReqVO[\s\S]*simulationRunId: string/)
assert.doesNotMatch(api.slice(api.indexOf('interface Stage6IdiSimulationReqVO'), api.indexOf('interface Stage6IdiSimulationRespVO')), /signaturePassword/)
assert.match(api, /simulateStage6IdiData[\s\S]*\/simulation\/stage6-idpr/)

assert.match(page, /data-team-leader-simulate-stage6-idpr/)
assert.match(page, /simulateStage6IdiData/)
const stage6Handler = page.slice(
  page.indexOf('const handleSimulateStage6Idi'),
  page.indexOf('const handleRecommendedActiveOrderConflictResolution')
)
assert.doesNotMatch(stage6Handler, /signaturePassword|ElMessageBox\.prompt/)

assert.match(service, /stage5Service\.getReleaseSnapshot/)
assert.match(service, /batchTraceabilityService\.getTraceability/)
assert.match(service, /domainTraceService\.getTracePage/)
assert.match(service, /domainTraceService\.getTraceDetail/)
assert.match(service, /domainTraceService\.verify/)
assert.doesNotMatch(service, /activeOrderService|completionService|pqcReleaseService|approvalCenterService/)
assert.doesNotMatch(service, /createWorkOrder|addActiveOrder|uploadReleaseReports|reviewTask/)
assert.match(service, /BatchExecutionTraceDrawer/)
assert.match(service, /domainTraceDetailRoute/)
assert.match(service, /domain-trace\/detail/)

const releaseTraceTab = read('src/views/mes/pro/edhr/form-trace/FormTraceReleaseTab.vue')
assert.match(releaseTraceTab, /BatchExecutionTraceDrawer/)
assert.match(releaseTraceTab, /autoOpenBatchExecutionId/)
assert.match(releaseTraceTab, /openBatchTrace\(matchedRow\)/)
assert.match(releaseTraceTab, /getEdhrBatchReviewTimeline|traceDrawerVisible/)

const traceDrawer = read('src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue')
assert.match(traceDrawer, /批记录表单/)
assert.match(traceDrawer, /单元格填写责任/)
assert.match(traceDrawer, /操作审计/)
assert.match(traceDrawer, /电子签名记录/)
assert.match(traceDrawer, /放行事件/)

const domainDetail = read('src/views/mes/pro/edhr/DomainTraceDetailPage.vue')
assert.match(domainDetail, /getEdhrDomainTraceDetail/)
assert.match(domainDetail, /verifyEdhrDomainTrace/)

console.log('team-leader-stage6-idi-static: PASS')
