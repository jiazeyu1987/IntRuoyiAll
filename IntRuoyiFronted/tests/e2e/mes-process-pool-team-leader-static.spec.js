const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const timelineApiPath = path.join(root, 'src/api/mes/pro/processpool/index.ts')
const apiPath = path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts')
const eventRevisionApiPath = path.join(root, 'src/api/mes/pro/processpool/eventRevision.ts')
const pagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const productionLeaderPagePath = path.join(root, 'src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue')
const pqcLeaderPagePath = path.join(root, 'src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue')
const routePath = path.join(root, 'src/router/modules/remaining.ts')

const read = (file) => fs.readFileSync(file, 'utf8')

assert(fs.existsSync(apiPath), '班组长工序池必须提供前端 API wrapper。')
assert(fs.existsSync(eventRevisionApiPath), '工序池原始记录修订必须提供前端 API wrapper。')
assert(fs.existsSync(timelineApiPath), '工序池时间轴必须提供前端响应类型。')
assert(fs.existsSync(pagePath), '班组长工序池必须提供工作台页面。')
assert(fs.existsSync(productionLeaderPagePath), '生产组长必须提供独立主导航页面包装。')
assert(fs.existsSync(pqcLeaderPagePath), 'PQC组长必须提供独立主导航页面包装。')

const timelineApi = read(timelineApiPath)
const api = read(apiPath)
const eventRevisionApi = read(eventRevisionApiPath)
const page = read(pagePath)
const productionLeaderPage = read(productionLeaderPagePath)
const pqcLeaderPage = read(pqcLeaderPagePath)
const routes = read(routePath)

for (const endpoint of [
  '/mes/pro/process-pool/team-leader/submission/page',
  '/mes/pro/process-pool/team-leader/submission/detail',
  '/mes/pro/process-pool/team-leader/submission/review',
  '/mes/pro/process-pool/team-leader/work-order/abnormal/report',
  '/mes/pro/process-pool/team-leader/active-order/list',
  '/mes/pro/process-pool/team-leader/active-order/add',
  '/mes/pro/process-pool/team-leader/active-order/remove',
  '/mes/pro/process-pool/team-leader/employee-profile/create',
  '/mes/pro/process-pool/team-leader/process-employee-binding/save',
  '/mes/pro/process-pool/team-leader/team-device/create',
  '/mes/pro/process-pool/team-leader/team-device/status/update',
  '/mes/pro/process-pool/team-leader/process-device-binding/save',
  '/mes/pro/process-pool/team-leader/runtime-device-parameter-rule/save',
  '/mes/pro/process-pool/team-leader/process-defect-reason/save',
  '/mes/pro/process-pool/team-leader/submission/allocation/preview-fifo',
  '/mes/pro/process-pool/team-leader/submission/allocation/confirm'
]) {
  assert(api.includes(endpoint), `缺少班组长专用接口: ${endpoint}`)
}

assert(!/leaderUserId\??:/.test(api), '前端请求类型不得接受 leaderUserId，必须由后端从登录态注入。')
assert(api.includes('leaderType'), '班组长看板必须显式传递 PRODUCTION/PQC 类型。')

for (const apiName of [
  'getTeamLeaderSubmissionPage',
  'getTeamLeaderSubmissionDetail',
  'reviewTeamLeaderSubmission',
  'markAndReportWorkOrderAbnormal',
  'getTeamLeaderActiveOrderList',
  'addTeamLeaderActiveOrder',
  'removeTeamLeaderActiveOrder',
  'createTeamEmployeeProfile',
  'saveTeamProcessEmployeeBinding',
  'createTeamDevice',
  'updateTeamDeviceStatus',
  'saveTeamProcessDeviceBinding',
  'saveTeamRuntimeDeviceParameterRule',
  'saveTeamProcessDefectReason',
  'previewTeamLeaderReportFifoAllocation',
  'confirmTeamLeaderReportAllocation'
]) {
  assert(page.includes(apiName), `工作台页面必须调用 ${apiName}`)
}

for (const marker of [
  'data-team-leader-report-workbench',
  'data-team-leader-config-center',
  'data-team-leader-active-order-config',
  'data-team-leader-employee-config',
  'data-team-leader-device-config',
  'data-team-leader-parameter-config',
  'data-team-leader-process-relation-config',
  'data-team-leader-active-order-select',
  'data-team-leader-defect-reason-select',
  'data-team-leader-structured-detail',
  'data-team-leader-fifo-allocation',
  'data-team-leader-allocation-table'
]) {
  assert(page.includes(marker), `班组长工作台缺少当前 PRD 稳定选择器: ${marker}`)
}
assert(page.includes('报工确认工作台'), '生产组长必须有报工确认工作台。')
assert(page.includes('班组配置中心'), '生产组长必须有班组配置中心。')
assert(page.includes('活跃订单池'), '班组配置中心必须维护活跃订单。')
assert(page.includes('生产人员档案'), '人员管理 tab 必须维护生产人员档案。')
assert(page.includes('生产人员工序绑定'), '班组配置中心必须维护工序员工关系。')
assert(page.includes('设备档案与状态'), '班组配置中心必须维护设备档案和设备状态。')
assert(page.includes('工序设备与异常关系'), '班组配置中心必须维护工序设备和工序异常关系。')
assert(page.includes('设备参数维护'), '班组配置中心必须维护设备参数、上下限和默认值。')
assert(page.includes('报修') && page.includes('REPAIRING'), '设备状态必须支持报修，不得误写为保修。')
assert(page.includes('allocationMode') && page.includes('FIFO') && page.includes('MANUAL'), '报工确认必须支持 FIFO 自动分配和手动分配。')
assert(page.includes('buildAllocationSubmitLines'), '确认报工必须提交活跃订单分配明细。')
assert(page.includes('getTeamLeaderActiveOrderList'), '异常上报和手动分配必须从活跃订单读取。')
assert(!page.includes('label="生产工单ID"'), '异常上报不能要求手工填写生产工单 ID，必须来自活跃订单。')
assert(!page.includes('label="来源提交ID"'), '异常上报不能要求手工填写来源提交 ID。')
assert(!page.includes('<template #header>员工工序绑定</template>'), '旧员工绑定卡片必须替换为员工档案与工序员工关系配置。')
assert(!page.includes('<template #header>设备参数上下限</template>'), '旧设备参数卡片必须替换为设备档案、状态和运行参数配置。')
assert(page.includes('PQC') && page.includes('PRODUCTION'), '页面必须继续支持生产班组长和 PQC 班组长两类查询。')
assert(page.includes('leaderType?: TeamLeaderType'), '工作台组件必须允许包装页显式锁定 PRODUCTION/PQC 类型。')
assert(page.includes('showLeaderTypeTabs?: boolean'), '工作台组件必须允许包装页关闭内部组长类型切换。')
assert(page.includes('showLeaderTypeTabs: false'), '工作台组件默认不得显示内部生产/PQC 切换页签。')
assert(
  /v-if="showLeaderTypeTabs"[\s\S]*data-team-leader-type-tabs/.test(page),
  '内部生产/PQC 切换页签只能在显式 showLeaderTypeTabs=true 时显示。'
)
assert(!page.includes('data-team-leader-pqc-placeholder'), 'PQC 组长页签不能停留在占位内容。')
assert(!page.includes('PQC 组长功能正在建设中'), 'PQC 组长必须能看到检验员提交内容，不能显示建设中占位。')
assert(page.includes("if (leaderType === 'PRODUCTION')"), '生产专属活跃订单/配置加载必须与 PQC 提交看板查询区分。')
assert(page.includes('handleQuery()'), '切换到 PQC 组长后仍必须查询提交看板。')
assert(page.includes('PQC检验员'), 'PQC 组长提交看板必须按 PQC 检验员展示提交人。')
assert(!page.includes('<el-radio-group v-model="queryParams.leaderType"'), '组长类型必须使用页签，不得继续使用单选按钮。')
assert(page.includes('originalPayloadJson'), '提交详情必须展示原始 payload，复核不能替代原始记录。')
assert(
  /export interface ProcessPoolTimelineEventVO[\s\S]*originalPayloadJson\?: string/.test(timelineApi),
  '组长列表分页响应必须暴露 originalPayloadJson，不能只在详情接口暴露原始 payload。'
)
assert(
  page.includes('resolvePqcSubmissionContentItems'),
  'PQC 组长列表提交内容必须通过正式解析函数展示检验员逐项填写内容。'
)
assert(
  page.includes('data-pqc-leader-submission-content'),
  'PQC 组长列表必须提供稳定选择器承载逐项提交内容。'
)
for (const payloadField of ['pqcItemDetails', 'itemResults']) {
  assert(
    page.includes(payloadField),
    `PQC 组长列表必须优先读取正式项目级明细字段: ${payloadField}`
  )
}
assert(
  page.includes('resolvePqcItemSnapshotDetails'),
  'PQC 组长列表必须通过项目级快照解析函数展示检验员逐项填写内容。'
)
assert(
  !/PQC_SUBMISSION_CONTENT_DEFINITIONS[\s\S]*length[\s\S]*appearance[\s\S]*seal[\s\S]*pressure/.test(page),
  'PQC 组长列表不得继续使用固定 length/appearance/seal/pressure 四项作为正式明细。'
)
assert(
  !page.includes('{{ row.submittedSummary || row.pqcSummary || \'--\' }}'),
  'PQC 组长列表提交内容不得继续只展示 submittedSummary/pqcSummary 汇总。'
)
assert(
  page.includes('PQC提交内容缺少正式项目明细'),
  'PQC 原始项目级明细缺失时必须显式提示，不能用汇总字段冒充。'
)
assert(page.includes('reviewStatus') && page.includes('reviewRemark'), '提交复核必须包含复核状态和说明。')
for (const field of [
  'submissionReviewStatus',
  'submissionReviewRemark',
  'submissionReviewLeaderUserId',
  'submissionReviewedAt'
]) {
  assert(timelineApi.includes(`${field}?:`), `组长列表响应必须回显复核日志字段: ${field}`)
  assert(page.includes(field), `组长检查列表必须展示复核日志字段: ${field}`)
}
assert(page.includes('正确') && page.includes('不正确'), '组长复核必须按正确/不正确表达，不得只用通过/退回。')
assert(page.includes('data-team-leader-review-log'), '组长检查列表必须提供稳定选择器展示复核判定日志。')
assert(
  eventRevisionApi.includes('/mes/pro/process-pool/event-revision/update-original') &&
    page.includes('updateProcessPoolOriginalRecord'),
  '组长修改不正确内容必须走正式原始记录修订接口并写修订日志。'
)
assert(
  page.includes('openCorrection') && page.includes('submitCorrection'),
  '组长检查列表必须提供修改不正确内容入口并提交正式修订。'
)
const reviewSubmissionCall =
  page.match(/reviewTeamLeaderSubmission\(\{[\s\S]*?\}\)/)?.[0] || ''
assert(
  !reviewSubmissionCall.includes('afterPayload'),
  '复核判定接口不得携带修正 payload，修改必须走 event-revision 正式日志链路。'
)
assert(
  page.includes('data-pqc-submission-log') &&
    page.includes('PQC提交日志') &&
    page.includes('originalPayloadJson'),
  'PQC 检验员提交必须在组长详情中展示提交日志、事件编号、提交时间和原始 payload。'
)
assert(page.includes('lowerLimit') && page.includes('upperLimit'), '设备参数维护必须包含上下限字段。')
assert(!page.includes('ignoreErrorMessage: true'), '班组长页面不得静默隐藏后端错误。')

assert(routes.includes("path: 'pro/process-pool/team-leader'"), 'remaining 路由必须提供班组长工作台入口。')
assert(routes.includes("path: 'pro/process-pool/production-leader'"), 'remaining 路由必须提供独立生产组长主导航入口。')
assert(routes.includes('ProductionLeaderWorkbenchPage.vue'), '生产组长入口必须使用 process-pool 专门包装页。')
assert(routes.includes("path: 'pro/process-pool/pqc-leader'"), 'remaining 路由必须提供独立 PQC组长主导航入口。')
assert(routes.includes('PqcLeaderWorkbenchPage.vue'), 'PQC组长入口必须使用 process-pool 专门包装页。')
assert(!/pro\/feedback\/edhr-batch-(?:team-leader|production-leader|pqc-leader)/.test(routes), '旧 eDHR 组长路由必须移除。')
assert(routes.includes("permission: ['mes:pro-process-pool-team-leader:query']"), '班组长工作台路由必须绑定查询权限。')
assert(
  /data-production-leader-workbench-page[\s\S]*leader-type="PRODUCTION"[\s\S]*:show-leader-type-tabs="false"/.test(productionLeaderPage),
  '生产组长独立页面必须锁定生产工作台内容并关闭内部切换。'
)
assert(
  /data-pqc-leader-workbench-page[\s\S]*leader-type="PQC"[\s\S]*:show-leader-type-tabs="false"/.test(pqcLeaderPage),
  'PQC组长独立页面必须锁定 PQC 工作台内容并关闭内部切换。'
)
assert(
  !/EdhrBatchRecordTabs|active-tab=/.test(`${productionLeaderPage}\n${pqcLeaderPage}`),
  '两个独立组长页面不得通过 eDHR 内部 tabs 承载。'
)

console.log('mes-process-pool-team-leader-static PASS')
