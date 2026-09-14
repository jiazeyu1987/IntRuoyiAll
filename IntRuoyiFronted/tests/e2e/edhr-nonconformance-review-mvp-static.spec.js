const assert = require('assert/strict')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/mes/pro/edhr/nonconformanceReview.ts')
assert.match(api, /SOURCE_TYPE_PQC_SUBMISSION\s*=\s*'PQC_SUBMISSION'/)
assert.match(api, /SOURCE_TYPE_PQC_RELEASE\s*=\s*'PQC_RELEASE'/)
assert.match(api, /DISPOSITION_CONCESSION_RELEASE\s*=\s*'concession_release'/)
assert.match(api, /DISPOSITION_REWORK\s*=\s*'rework'/)
assert.match(api, /DISPOSITION_VOID\s*=\s*'void'/)
assert.match(api, /createNonconformanceReview/)
assert.match(api, /disposeNonconformanceReview/)
assert.match(api, /getPendingNonconformanceReviewPage/)

const page = read('src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')
assert.match(page, /UploadFile/)
assert.match(page, /让步放行/)
assert.match(page, /返工/)
assert.match(page, /作废/)
assert.match(page, /评审材料/)
assert.match(page, /评审意见/)
assert.match(page, /QA签名/)
assert.match(page, /提交不合格评审/)
assert.match(page, /冻结后禁止报工、PQC提交、PQC放行/)

const router = read('src/router/modules/remaining.ts')
assert.match(router, /MesProFeedbackEdhrNonconformanceReview/)
assert.match(router, /edhr-nonconformance-review/)

const batchApi = read('src/api/mes/pro/edhr/batchExecution.ts')
assert.match(batchApi, /EDHR_BATCH_STATUS_FROZEN\s*=\s*15/)

const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
assert.doesNotMatch(batchDetail, /SOURCE_TYPE_PQC_SUBMISSION/)
assert.match(batchDetail, /SOURCE_TYPE_PQC_RELEASE/)
assert.match(batchDetail, /openNonconformanceReviewEntry/)
assert.match(batchDetail, /name:\s*'MesProFeedbackEdhrNonconformanceReview'/)
assert.match(batchDetail, /不合格审查/)
assert.match(
  batchDetail,
  /nonconformanceFrozenActionLocked\.value\s*\|\|\s*\(\s*!hasGoldenFingerActionBypass\.value/
)

const frontlinePanel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
assert.doesNotMatch(frontlinePanel, /frontline-pqc-nonconformance-button/)
assert.doesNotMatch(frontlinePanel, /openNonconformanceReviewEntry/)
assert.doesNotMatch(frontlinePanel, /name:\s*'MesProFeedbackEdhrNonconformanceReview'/)

const teamLeaderPage = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
assert.match(teamLeaderPage, /SOURCE_TYPE_PQC_SUBMISSION/)
assert.match(teamLeaderPage, /canOpenPqcSubmissionNonconformanceReview/)
assert.match(teamLeaderPage, /openPqcSubmissionNonconformanceReview/)
assert.match(teamLeaderPage, /data-pqc-submission-nonconformance-review-event-id/)
assert.match(teamLeaderPage, /name:\s*'MesProFeedbackEdhrNonconformanceReview'/)
assert.match(teamLeaderPage, /sourceType:\s*SOURCE_TYPE_PQC_SUBMISSION/)
assert.match(teamLeaderPage, /sourceId:\s*String\(row\.id\)/)
assert.doesNotMatch(teamLeaderPage, /query\.batchExecutionId\s*=|batchExecutionId:\s*String\(row\.batchExecutionId\)/)

const processPoolApi = read('src/api/mes/pro/processpool/index.ts')
assert.match(processPoolApi, /batchExecutionId\?:\s*number/)

const realE2e = read('tests/e2e/edhr-nonconformance-review-mvp-real.e2e.js')
const runFunctionStart = realE2e.indexOf('async function run()')
assert.notEqual(runFunctionStart, -1)
const pqcEntryOnlyStart = realE2e.indexOf('if (PQC_ENTRY_ONLY)', runFunctionStart)
const pqcEntryOnlyEnd = realE2e.indexOf('await openQaListFromMenu(page)', pqcEntryOnlyStart)
assert.notEqual(pqcEntryOnlyStart, -1)
assert.notEqual(pqcEntryOnlyEnd, -1)
const pqcEntryOnlySegment = realE2e.slice(pqcEntryOnlyStart, pqcEntryOnlyEnd)
assert.match(realE2e, /worktree-ports\.json/)
assert.match(realE2e, /resolveRegisteredRuntime/)
assert.match(realE2e, /runtime\.frontendPort/)
assert.match(realE2e, /runtime\.backendPort/)
assert.match(realE2e, /table-multi-filter/)
assert.match(realE2e, /新增筛选条件/)
assert.match(
  realE2e,
  /page\.locator\('\.el-menu:visible'\)\.getByText/,
  '动态菜单定位必须限定当前可见菜单树。'
)
assert.match(
  realE2e,
  /page\.waitForResponse\([\s\S]*\)\.catch\(\(error\)\s*=>\s*\(\{\s*waitError:\s*error\s*\}\)\)/,
  '菜单点击失败时，预注册响应等待器必须被显式收敛。'
)
assert.match(realE2e, /\/mes\/pro\/edhr-batch-execution\/open-or-create-manual/)
assert.doesNotMatch(realE2e, /\/mes\/pro\/edhr-batch-execution\/open-or-create['"]/)
assert.match(
  realE2e,
  /applyTableMultiFilter\([\s\S]*'mes\.pro\.route\.main\.admin-layout-v1'[\s\S]*'code'[\s\S]*'路线编码'/,
  '真实 E2E 的工艺路线前置筛选必须使用当前多条件筛选器。'
)
assert.match(
  realE2e,
  /filter\(\s*\(route\)\s*=>\s*route\.code\s*===\s*SOURCE_ROUTE_CODE\s*\)/,
  '路线编码使用包含筛选时，真实 E2E 必须从响应中精确选择目标路线。'
)
assert.doesNotMatch(
  realE2e,
  /table-quick-filter\[data-table-key="mes\.pro\.route\.main\.admin-layout-v1"\]/,
  '真实 E2E 不得继续等待旧工艺路线 quick filter。'
)
assert.match(realE2e, /PQC_LEADER_PATH/)
assert.match(
  realE2e,
  /const releaseReviewButton = page\.getByRole\('button', \{ name: '不合格审查' \}\)[\s\S]*releaseReviewButton\.first\(\)\.waitFor\(\{ state: 'visible', timeout: 90000 \}\)/,
  '批次详情放行不合格审查按钮必须等待辅助工作台渲染完成。'
)
assert.match(
  realE2e,
  /async function openPqcLeaderManagementFromMenu[\s\S]*clickMenuText\(page, 'MES 系统'\)[\s\S]*clickMenuText\(page, 'eDHR批记录'\)[\s\S]*clickMenuText\(page, 'PQC组长'\)/,
  'PQC组长必须从当前真实 MES 系统 -> eDHR批记录 菜单链进入。'
)
assert.match(realE2e, /data-pqc-submission-nonconformance-review-event-id/)
assert.match(
  realE2e,
  /getByPlaceholder\('工单编码'\)\.fill\(batch\.workOrderCode\)/,
  'PQC管理工单筛选必须使用当前页面“工单编码”输入框。'
)
assert.match(
  realE2e,
  /\.find\(\s*\(row\)\s*=>\s*String\(row\.batchExecutionId\)\s*===\s*String\(batch\.id\)\s*\)/,
  'PQC管理存在多条同批次提交时，真实 E2E 必须选择一条精确关联记录。'
)
assert.match(realE2e, /NCR_E2E_PQC_ENTRY_ONLY/)
assert.match(realE2e, /NCR_E2E_VERIFY_FROZEN_ACTIONS/)
assert.match(realE2e, /NCR_E2E_FROZEN_ACTIONS_ONLY/)
assert.match(realE2e, /NCR_E2E_SKIP_FROZEN_PRODUCTION/)
assert.match(realE2e, /mode:\s*'FROZEN_ACTIONS_ONLY'/)
assert.match(realE2e, /verifyFrozenProductionReport/)
assert.match(realE2e, /waitForProductionSubmitReady/)
assert.match(realE2e, /\/mes\/pro\/feedback\/frontline\/submit/)
assert.match(realE2e, /verifyFrozenPqcSubmit/)
assert.match(realE2e, /waitForPqcEmployeeSwitch/)
assert.match(realE2e, /\/mes\/pro\/feedback\/frontline\/device-account\/pqc\/submit/)
assert.match(realE2e, /verifyFrozenPqcRelease/)
assert.match(realE2e, /releaseButtonVisible/)
assert.match(realE2e, /1040750474/)
assert.match(realE2e, /action === '生产报工' \? '\(\?:生产\)\?报工' : action/)
assert.match(realE2e, /frozenActionChecks/)
assert.match(realE2e, /mode:\s*'PQC_ENTRY_ONLY'/)
assert.match(
  pqcEntryOnlySegment,
  /const releaseEntry = await openReleaseReviewEntry\(page, batch\)[\s\S]*const submissionEntry = await openPqcReviewEntry\(page, batch\)/,
  'PQC entry-only E2E 必须同时验证 PQC生产放行与PQC管理两个不合格评审入口。'
)
assert.match(realE2e, /entry-pqc-release\.png/)
assert.match(realE2e, /entry-pqc-management\.png/)
assert.match(
  pqcEntryOnlySegment,
  /context\.tracing\.stop\(\{ path: path\.join\(RESULT_DIR, 'trace\.zip'\) \}\)/,
  'PQC entry-only E2E 必须保存 trace.zip 供逐步截图导出。'
)
assert.match(realE2e, /pqcSubmissionEventId:\s*submissionEntry\.eventId/)
assert.match(realE2e, /releaseReviewUrl:\s*releaseEntry\.reviewUrl/)
assert.match(realE2e, /pqcManagementReviewUrl:\s*submissionEntry\.reviewUrl/)
assert.match(realE2e, /sourceType:\s*'PQC_RELEASE'/)
assert.match(realE2e, /sourceType:\s*'PQC_SUBMISSION'/)
assert.match(realE2e, /sourceRouteNeedsRestore\s*=\s*await setSourceRouteEnabled\(page,\s*true\)/)
assert.match(
  realE2e,
  /EXISTING_EXECUTION_ID\s*\?\s*\{[\s\S]*executionId:[\s\S]*\}\s*:\s*await openTraceExecution\(page,\s*batch\)/,
  '已有任务夹具未预置执行时，必须通过真实页面打开任务生成追溯执行。'
)
assert.doesNotMatch(realE2e, /sourceRouteNeedsRestore\s*=\s*true\s*\n\s*await setSourceRouteEnabled\(page,\s*true\)/)
assert.match(realE2e, /nonTargetConsoleErrors/)
assert.match(realE2e, /审批待办数量加载失败/)

const releasePage = read('src/views/mes/pro/edhr-release/ReleasePage.vue')
assert.match(releasePage, /SOURCE_TYPE_PQC_RELEASE/)
assert.match(releasePage, /openNonconformanceReviewEntry/)
assert.match(releasePage, /name:\s*'MesProFeedbackEdhrNonconformanceReview'/)
assert.match(releasePage, /不合格审查/)

const traceApi = read('src/api/mes/pro/edhr/domainTrace.ts')
assert.match(traceApi, /nonconformanceReviews/)

const tracePage = read('src/views/mes/pro/edhr/DomainTraceDetailPage.vue')
assert.match(tracePage, /不合格评审/)
assert.match(tracePage, /reviewMaterialUrl/)
assert.match(tracePage, /concession_release/)
assert.match(tracePage, /rework/)
assert.match(tracePage, /void/)

console.log('edhr-nonconformance-review-mvp-static: PASS')
