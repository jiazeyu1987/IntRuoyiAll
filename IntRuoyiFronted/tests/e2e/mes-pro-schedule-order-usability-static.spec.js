const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')
const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(apiPath), 'Schedule order API module must exist.')
assert(fs.existsSync(pagePath), 'Schedule order pool page must exist.')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  '/mes/pro/schedule-order/admission-diff',
  '/mes/pro/schedule-order/preflight',
  'getAdmissionDiff',
  'preflightScheduleOrders',
  'MesProScheduleOrderAdmissionDiffRowVO',
  'MesProScheduleOrderPreflightRespVO',
  'productName?: string',
  'productCode?: string'
]) {
  assert(apiSource.includes(token), `Schedule order API must expose ${token}.`)
}

for (const token of [
  '同步工单',
  '重新检查',
  '入池状态',
  '不可排原因',
  '建议处理',
  '缺失权限',
  'canApplyReplan',
  'preflightResult',
  'BLOCKED_MISSING_ROUTE',
  'WARN_ERP_SYNC_RECORD_MISSING',
  'openIssueAction'
]) {
  assert(pageSource.includes(token), `Schedule order page must render or handle ${token}.`)
}

for (const token of [
  "v-hasPermi=\"['mes:pro-auto-schedule:replan']\"",
  'const hasReplanPermission = computed(() => checkPermi([\'mes:pro-auto-schedule:replan\']))',
  "(!hasReplanPermission.value && '当前账号没有手动重排权限')",
  "message.warning(replanProjectionState.value.blockerMessage || '当前账号没有手动重排权限')"
]) {
  assert(pageSource.includes(token), `Schedule order page must guard manual replan with ${token}.`)
}

assert(
  !/@click="openPreflightDrawer"/.test(pageSource),
  'Schedule order toolbar must not keep the removed standalone preflight entry.'
)
assert(
  !/<el-button[\s\S]*?type="success"[\s\S]*?@click="openPreflightDrawer"[\s\S]*?排产前检查/.test(
    pageSource
  ),
  'Schedule order toolbar must not render the removed green preflight button.'
)
assert(
  /schedule-order-pool__preflight-title">排产前检查</.test(pageSource),
  'Manual replan drawer must still expose the embedded preflight section title.'
)

assert(
  pageSource.includes(':selectable="isAdmissionRowSelectable"'),
  'Admission diff table must disable selection for already-admitted and blocked rows.'
)
assert(
  /:filter-definitions="workOrderAdmissionQuickFilterDefinitions"[\s\S]*@quick-filter-query="workOrderAdmissionQuickFilter\.applyQuickFilter"/.test(
    pageSource
  ),
  'Admission status filter must use the unified quick-filter query flow.'
)
assert(
  /const workOrderAdmissionQueryParams = reactive\(\{[\s\S]*admissionStatus: DEFAULT_WORK_ORDER_ADMISSION_STATUS/.test(
    pageSource
  ),
  'Admission query params must keep admissionStatus as an explicit query parameter.'
)
assert(
  /key:\s*'admissionStatus'[\s\S]*label:\s*'入池状态'[\s\S]*type:\s*'select'/.test(
    pageSource
  ),
  'Admission quick filter must keep admissionStatus as a selectable filter.'
)
assert(
  /confirmApplyReplanStartChoice[\s\S]*runPreflightForRequest\(applyRequest\)[\s\S]*排产前检查存在阻断问题，不能应用重排/.test(
    pageSource
  ),
  'Start replan must rerun preflight and stop before writing when blockers exist.'
)
assert(
  /v-if="hasReplanPermission"[\s\S]*@click="applyReplan"[\s\S]*开始重排/.test(pageSource),
  'Start replan action must stay inside the replan permission guard.'
)
assert(
  /requiredPermission[\s\S]*?checkPermi/.test(pageSource),
  'Issue actions must show missing permission instead of hiding the reason.'
)
assert(
  /label="产品\/编号"[\s\S]*?getPreflightIssueProductName\(row\)[\s\S]*?getPreflightIssueProductCode\(row\)/.test(
    pageSource
  ),
  'Preflight issue table must expose product name and product code for missing-route blockers.'
)
assert(
  !/catch\s*\{\s*\}/.test(pageSource),
  'Schedule order usability flow must not silently swallow frontend errors.'
)

console.log('PASS: MES schedule order usability static contract')
