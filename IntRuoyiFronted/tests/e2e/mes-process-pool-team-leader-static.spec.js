const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const apiPath = path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts')
const pagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const routePath = path.join(root, 'src/router/modules/remaining.ts')

const read = (file) => fs.readFileSync(file, 'utf8')

assert(fs.existsSync(apiPath), '班组长工序池必须提供前端 API wrapper。')
assert(fs.existsSync(pagePath), '班组长工序池必须提供工作台页面。')

const api = read(apiPath)
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
assert(page.includes('data-team-leader-pqc-placeholder'), 'PQC 组长页签必须提供明确占位内容。')
assert(page.includes("if (leaderType === 'PRODUCTION')"), '切换回生产组长时才允许查询生产组长看板。')
assert(!page.includes('<el-radio-group v-model="queryParams.leaderType"'), '组长类型必须使用页签，不得继续使用单选按钮。')
assert(page.includes('originalPayloadJson'), '提交详情必须展示原始 payload，复核不能替代原始记录。')
assert(page.includes('reviewStatus') && page.includes('reviewRemark'), '提交复核必须包含复核状态和说明。')
assert(page.includes('lowerLimit') && page.includes('upperLimit'), '设备参数维护必须包含上下限字段。')
assert(!page.includes('ignoreErrorMessage: true'), '班组长页面不得静默隐藏后端错误。')

assert(routes.includes("path: 'pro/process-pool/team-leader'"), 'remaining 路由必须提供班组长工作台入口。')
assert(routes.includes("permission: ['mes:pro-process-pool-team-leader:query']"), '班组长工作台路由必须绑定查询权限。')

console.log('mes-process-pool-team-leader-static PASS')
