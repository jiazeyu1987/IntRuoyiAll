const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const timelineApiPath = path.join(root, 'src/api/mes/pro/processpool/index.ts')
const apiPath = path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts')
const eventRevisionApiPath = path.join(root, 'src/api/mes/pro/processpool/eventRevision.ts')
const pagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const routePath = path.join(root, 'src/router/modules/remaining.ts')

const read = (file) => fs.readFileSync(file, 'utf8')

assert(fs.existsSync(apiPath), '班组长工序池必须提供前端 API wrapper。')
assert(fs.existsSync(eventRevisionApiPath), '工序池原始记录修订必须提供前端 API wrapper。')
assert(fs.existsSync(timelineApiPath), '工序池时间轴必须提供前端响应类型。')
assert(fs.existsSync(pagePath), '班组长工序池必须提供工作台页面。')

const timelineApi = read(timelineApiPath)
const api = read(apiPath)
const eventRevisionApi = read(eventRevisionApiPath)
const page = read(pagePath)
const routes = read(routePath)

for (const endpoint of [
  '/mes/pro/process-pool/team-leader/submission/page',
  '/mes/pro/process-pool/team-leader/submission/detail',
  '/mes/pro/process-pool/team-leader/submission/review',
  '/mes/pro/process-pool/team-leader/work-order/abnormal/report',
  '/mes/pro/process-pool/team-leader/employee-binding/add',
  '/mes/pro/process-pool/team-leader/employee-binding/disable',
  '/mes/pro/process-pool/team-leader/defect-reason/create',
  '/mes/pro/process-pool/team-leader/device-parameter-rule/save'
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
  'addTeamEmployeeBinding',
  'disableTeamEmployeeBinding',
  'createTeamDefectReason',
  'saveTeamDeviceParameterRule'
]) {
  assert(page.includes(apiName), `工作台页面必须调用 ${apiName}`)
}

assert(page.includes("activeTab = ref<'submission' | 'abnormal' | 'maintenance'>"), '页面必须拆分提交看板、异常上报、班组维护三类入口。')
assert(page.includes('PQC') && page.includes('PRODUCTION'), '页面必须支持生产班组长和 PQC 班组长切换。')
assert(page.includes('data-team-leader-type-tabs'), '页面必须提供生产组长和 PQC 组长一级页签。')
assert(page.includes('label="生产组长" name="PRODUCTION"'), '页面必须提供生产组长页签。')
assert(page.includes('label="PQC 组长" name="PQC"'), '页面必须提供 PQC 组长页签。')
assert(!page.includes('data-team-leader-pqc-placeholder'), 'PQC 组长页签不能停留在占位内容。')
assert(!page.includes('PQC 组长功能正在建设中'), 'PQC 组长必须能看到检验员提交内容，不能显示建设中占位。')
assert(!page.includes("if (leaderType === 'PRODUCTION')"), '切换到 PQC 组长也必须查询提交看板。')
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
for (const payloadField of ['pqcPieceValues', 'inspectionType', 'inspectionQuantity', 'scrapQuantity']) {
  assert(
    page.includes(payloadField),
    `PQC 组长列表必须读取检验员填写 payload 字段: ${payloadField}`
  )
}
for (const [key, label] of [
  ['length', '长度'],
  ['appearance', '外观'],
  ['seal', '密封'],
  ['pressure', '压力']
]) {
  assert(
    page.includes(`key: '${key}'`) && page.includes(`label: '${label}'`),
    `PQC 组长列表必须展示检验员填写页同一检验项: ${label}`
  )
}
assert(
  !page.includes('{{ row.submittedSummary || row.pqcSummary || \'--\' }}'),
  'PQC 组长列表提交内容不得继续只展示 submittedSummary/pqcSummary 汇总。'
)
assert(
  page.includes('PQC提交内容缺少正式明细'),
  'PQC 原始逐项明细缺失时必须显式提示，不能用汇总字段冒充。'
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
assert(
  !/reviewTeamLeaderSubmission\(\{[\s\S]*afterPayload/.test(page),
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
assert(routes.includes("permission: ['mes:pro-process-pool-team-leader:query']"), '班组长工作台路由必须绑定查询权限。')

console.log('mes-process-pool-team-leader-static PASS')
