const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')
const packageJsonPath = path.join(repoRoot, 'package.json')
const e2ePath = path.join(repoRoot, 'tests/e2e/smart-scheduling-smoke-real-flow.e2e.js')
const frontendApiPath = path.join(repoRoot, 'src/api/erp/sync/index.ts')
const erpSyncPagePath = path.join(repoRoot, 'src/views/erp/sync/index.vue')
const taskPagePath = path.join(repoRoot, 'src/views/mes/pro/task/index.vue')
const feedbackPagePath = path.join(repoRoot, 'src/views/mes/pro/feedback/index.vue')
const backendControllerPath = path.join(
  backendRoot,
  'yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sync/ErpKingdeeSyncController.java'
)
const backendCreateServicePath = path.join(
  backendRoot,
  'yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sync/admin/ErpKingdeeProductionOrderCreateServiceImpl.java'
)
const backendCreateReqPath = path.join(
  backendRoot,
  'yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sync/vo/ErpKingdeeProductionOrderCreateReqVO.java'
)
const backendKingdeeOrderPath = path.join(
  backendRoot,
  'yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/sync/ErpKingdeeProductionOrder.java'
)
const backendKingdeeClientPath = path.join(
  backendRoot,
  'yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/sync/ErpKingdeeProductionOrderClientImpl.java'
)
const backendMesSyncPath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/workorder/sync/MesKingdeeProductionOrderSyncServiceImpl.java'
)

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
assert.equal(
  packageJson.scripts?.['e2e:mes:smart-scheduling-smoke:check'],
  'node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js',
  'package.json must expose the smart scheduling smoke static check'
)
assert.equal(
  packageJson.scripts?.['e2e:mes:smart-scheduling-smoke'],
  'node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js',
  'package.json must expose the smart scheduling smoke real flow runner'
)

const source = readUtf8(e2ePath)
const declaredDependencies = {
  ...packageJson.dependencies,
  ...packageJson.devDependencies
}

if (source.includes("require('xlsx')")) {
  assert.ok(
    declaredDependencies.xlsx,
    "package.json must declare xlsx when smart scheduling smoke requires it at runtime"
  )
}

for (const envName of [
  'MES_SMOKE_BASE_URL',
  'MES_SMOKE_DEFAULT_PASSWORD',
  'MES_SMOKE_ERP_CREATOR_TENANT',
  'MES_SMOKE_ERP_CREATOR_USERNAME',
  'MES_SMOKE_PLANNER_TENANT',
  'MES_SMOKE_PLANNER_USERNAME',
  'MES_SMOKE_SUPERVISOR_TENANT',
  'MES_SMOKE_SUPERVISOR_USERNAME',
  'MES_SMOKE_NON_APPROVER_TENANT',
  'MES_SMOKE_NON_APPROVER_USERNAME',
  'MES_SMOKE_PRODUCT_CODE',
  'MES_SMOKE_ERP_UNIT_NUMBER',
  'MES_SMOKE_BATCH_NUMBER',
  'MES_SMOKE_CAPACITY_MODE'
]) {
  assert.ok(source.includes(envName), `E2E runner must fail fast on missing ${envName}`)
}

assert.ok(
  source.includes("optionalEnv('MES_SMOKE_EXCEL_FILE'") &&
    !source.includes("resolveExcelFile(requireEnv('MES_SMOKE_EXCEL_FILE'))"),
  'E2E runner may accept MES_SMOKE_EXCEL_FILE only as an optional template path, not as a required stale workbook'
)
assert.ok(
  source.includes("feedbackApproverName: optionalEnv(") &&
    source.includes("'MES_SMOKE_FEEDBACK_APPROVER_NAME'") &&
    source.includes('roles.supervisor.username'),
  'E2E runner default feedback approver must follow the configured supervisor username so one-shot imported smoke data can run without extra approver env overrides'
)
for (const fragment of [
  'function prepareFeedbackExcelWorkbook',
  "require('xlsx')",
  'feedback-workbook',
  '导管报工',
  'config.workOrderCode',
  'config.productCode',
  'config.smokeRunId',
  'config.sourceFileSha256 = sha256File(config.excelFile)'
]) {
  assert.ok(source.includes(fragment), `E2E runner must generate a run-specific feedback workbook fragment: ${fragment}`)
}

for (const fragment of [
  'smokeRunId',
  'assertDistinctRoleAccounts',
  'assertLocalOnly',
  'assertNoAdminRole',
  'writeJsonArtifact',
  'captureStep',
  'sourceFileSha256',
  '新增ERP工单',
  'batchNumber',
  '批次号',
  'kingdeeProductionOrderSyncJob',
  'waitForMesWorkOrderSync',
  'batchCode',
  '/admin-api/erp/kingdee-sync/production-order/create',
  '/admin-api/infra/job/trigger',
  '/erp/production-material-list/page',
  '/mes/pro/work-order/page',
  'READY_TO_ADMIT',
  'sourceSnapshotJson',
  'quantityScheduled',
  'calendarContextToken',
  'shortageCount',
  'approveUserId',
  'currentUserId',
  'preserveManualLockedTasks',
  'GAP-01',
  'GAP-02',
  'GAP-03',
  'GAP-04',
  'GAP-05',
  'GAP-06',
  'GAP-07',
  'GAP-08',
  'GAP-09',
  'GAP-10',
  'REG-01',
  'REG-08',
  'kingdeeProductionMaterialListSyncJob',
  'productionMaterialListSyncHandlerName',
  'triggerProductionMaterialListSync',
  'waitForProductionMaterialListSync',
  'production-material-list-sync-trigger.json',
  'synced-production-material-list.json',
  'production-material-list-sync-timeout.json'
]) {
  assert.ok(source.includes(fragment), `E2E runner must include gate fragment: ${fragment}`)
}

for (const forbiddenPattern of [
  /fetch\([^)]*\/erp\/kingdee-sync\/production-order\/create/,
  /fetch\([^)]*\/infra\/job\/trigger/,
  /fetch\([^)]*\/mes\/pro\/work-order\/create/,
  /fetch\([^)]*\/mes\/pro\/schedule-order\/create-from-work-order/,
  /fetch\([^)]*\/mes\/pro\/task\/auto-schedule\/apply/,
  /fetch\([^)]*\/mes\/pro\/feedback\/import-third-party-xlsx/,
  /fetch\([^)]*\/mes\/pro\/feedback\/import-record\/attribute/,
  /fetch\([^)]*\/mes\/pro\/feedback\/approve/
]) {
  assert.ok(
    !forbiddenPattern.test(source),
    `E2E runner must not bypass frontend write paths: ${forbiddenPattern}`
  )
}

assert.ok(
  !source.includes('/admin-api/mes/pro/work-order/create') &&
    !source.includes('createAndConfirmProductionWorkOrder'),
  'E2E runner must not create production work orders from the old MES manual-create path'
)
assert.ok(
  source.includes('createErpProductionOrderAndWaitForMesSync'),
  'E2E runner must create a production order from ERP and wait for MES sync'
)
assert.ok(
  /productionMaterialList:\s*optionalEnv\(\s*'MES_SMOKE_PRODUCTION_MATERIAL_LIST_PATH'/.test(source) &&
    source.includes('/erp/production/material-list'),
  'E2E runner must expose the ERP production material list route used for post-sync verification'
)
assert.ok(
  /const productionMaterialListSyncTrigger = await triggerProductionMaterialListSync\(erpPage\)/.test(
    source
  ) &&
    /const productionMaterialListSync = await waitForProductionMaterialListSync\(erpPage,\s*synced\)/.test(
      source
    ),
  'E2E runner must trigger and wait for ERP production material list sync before schedule admission'
)
assert.ok(
  source.includes('erpSyncEligibleDateTime') &&
    source.includes("MES_SMOKE_ERP_PLANNED_START_TIME', erpSyncEligibleDateTime()"),
  'E2E runner default ERP planned start time must stay inside the Kingdee sync date window'
)
assert.ok(
  source.includes("MES_SMOKE_PROMISE_DATE', isoDateAfter(90)"),
  'E2E runner default promise date must leave enough real-queue horizon for the happy-path smoke publish'
)
assert.ok(
  source.includes('function nextWeekdayDateTimeAfter(') &&
    source.includes('date.getDay() === 0 || date.getDay() === 6') &&
    /MES_SMOKE_ERP_PLANNED_FINISH_TIME'\s*,\s*nextWeekdayDateTimeAfter\(36\)/.test(source),
  'E2E runner default ERP planned finish time must avoid weekend dates rejected by the Kingdee workshop calendar'
)
assert.ok(
  source.includes("scheduleOrder: optionalEnv('MES_SMOKE_SCHEDULE_ORDER_PATH', '/mes/pro/schedule-order')"),
  'E2E runner default schedule order route must match the dynamic menu path'
)
assert.ok(
  source.includes("workOrder: optionalEnv('MES_SMOKE_WORK_ORDER_PATH', '/mes/pro/work-order')"),
  'E2E runner default work order route must match the current dynamic menu path'
)
assert.ok(
  source.includes('.el-select__wrapper') && !source.includes('await input.fill(optionText)'),
  'E2E runner must select Element Plus readonly selects through the select wrapper'
)
assert.ok(
  source.includes('async function confirmMessageBox') &&
    source.includes('await confirmButton.focus()') &&
    source.includes("await page.keyboard.press('Enter')"),
  'E2E runner must confirm Element Plus message boxes through focused keyboard activation'
)
assert.ok(
  source.includes('选中工单加入排产工单池'),
  'E2E runner must admit work orders through the frontend admission button'
)
assert.ok(
  source.includes('第三方导入') && source.includes('选择归属') && source.includes('审批'),
  'E2E runner must cover import attribution and approval UI flows'
)
assert.ok(
  source.includes('processImportedFeedbackSequentially') &&
    /runStep\(\s*supervisorPage,\s*'sequential-feedback-attribution-and-approval'/.test(source) &&
    /processImportedFeedbackSequentially\(\s*supervisorPage,\s*nonApproverPage,\s*plannerPage,\s*importResult,\s*admitted\s*\)/.test(source) &&
    source.includes('post-attribution-process-snapshot-${item.feedbackId}.json') &&
    source.includes('await approveFeedback(supervisorPage, item)') &&
    !source.includes('attributeAndApproveImportedFeedback'),
  'E2E runner must attribute and approve serial feedback records one by one instead of attributing a later process before the previous process is approved'
)
assert.ok(
  source.includes('function selectSerialProgressBoundaryProcesses(') &&
    source.includes('function isSerialBoundaryMarker(') &&
    source.includes('lastBoundarySort'),
  'E2E runner must select feedback rows with the same serial boundary process rule used by schedule order progress'
)
assert.ok(
  source.includes('function selectPositiveSmokeBoundaryProcesses(') &&
    source.includes('boundaryProcesses.slice(0, 1)'),
  'E2E runner positive smoke must narrow the workbook to one real serial-boundary process'
)
assert.ok(
  source.includes('function prepareFeedbackExcelWorkbook(admitted)') &&
    source.includes('const boundaryProcesses = selectSerialProgressBoundaryProcesses(admitted.processes)') &&
    source.includes('const smokeBoundaryProcesses = selectPositiveSmokeBoundaryProcesses(boundaryProcesses)') &&
    source.includes('process.processCode') &&
    source.includes('process.processName'),
  'E2E runner must generate feedback workbook rows from admitted schedule order process snapshots'
)
assert.ok(
  /runStep\(\s*supervisorPage,\s*'prepare-boundary-feedback-workbook'/.test(source) &&
    source.includes('prepareFeedbackExcelWorkbook(admitted)') &&
    source.lastIndexOf('prepareFeedbackExcelWorkbook(admitted)') >
      source.indexOf('admit-confirmed-work-order-to-schedule-pool'),
  'E2E runner must build the feedback workbook after schedule order process snapshots are known'
)
assert.ok(
  /records\.sort\(\s*\(left, right\)\s*=>/.test(source) &&
    /Number\(left\.rowNo \|\| 0\)/.test(source) &&
    /Number\(right\.rowNo \|\| 0\)/.test(source),
  'E2E runner must attribute imported feedback rows in workbook row order'
)
assert.ok(
  source.includes('async function switchFeedbackTab(') &&
    source.includes('async function searchPendingImportRecord(') &&
    source.includes('input[placeholder="请输入记录编号"]') &&
    source.includes('const recordId = record.id') &&
    source.includes('searchPendingImportRecord(page, record)'),
  'E2E runner must switch to the pending attribution tab and search by import record id instead of assuming the row is visible on the current page'
)
assert.ok(
  source.includes("runStep(plannerPage, 'calendar-shift-shortage-lock-check'") &&
    !source.includes("runStep(supervisorPage, 'calendar-shift-shortage-lock-check'"),
  'E2E runner must verify the post-publish calendar as the A2 planner before switching to the A4 execution role'
)
assert.ok(
  source.includes('async function loadScheduleOrderProcesses(') &&
    source.includes('assertSequentialProcessContinuity(record, exact, latestProcesses)'),
  'E2E runner must use the latest process snapshot for sequential continuity checks'
)
assert.ok(
  source.includes('async function verifyBoundaryFeedbackCoverage(') &&
    source.includes("runStep(plannerPage, 'boundary-feedback-progress-after-approval'") &&
    source.includes('Number(scheduleOrder.progressPercent || 0) > 0'),
  'E2E runner must assert schedule order list progress advances after the selected positive-smoke boundary feedback is approved'
)
assert.ok(
  source.includes("getByRole('tab', { name: /正式报工/ })") &&
    source.includes('input[placeholder="请输入报工单号"]') &&
    !source.includes('请输入报工单编号'),
  'E2E runner must switch to the formal feedback tab and use the real feedback code search placeholder'
)

const frontendApi = readUtf8(frontendApiPath)
assert.ok(
  frontendApi.includes('createProductionOrder') &&
    frontendApi.includes('/erp/kingdee-sync/production-order/create'),
  'ERP sync frontend API must expose createProductionOrder'
)

const erpSyncPage = readUtf8(erpSyncPagePath)
for (const fragment of [
  '新增ERP工单',
  '创建并提交ERP工单',
  'erp-production-order-closure',
  'ERP已保存并提交',
  'MES工单生成状态',
  '最近成功水位仅表示同步窗口水位',
  'productionOrderDialogVisible',
  'productionOrderForm',
  'batchNumber',
  '批次号',
  'ErpKingdeeSyncApi.createProductionOrder',
  'value-format="x"',
  'kingdeeProductionOrderSyncJob'
]) {
  assert.ok(erpSyncPage.includes(fragment), `ERP sync page must include production-order UI fragment: ${fragment}`)
}

const taskPage = readUtf8(taskPagePath)
for (const fragment of [
  'autoSchedulePublishResult',
  '正式任务确认',
  '首末工序时间',
  '日历落点',
  '阻塞问题为 0，正式排程已写入'
]) {
  assert.ok(taskPage.includes(fragment), `Task page must keep publish-result evidence visible after auto scheduling: ${fragment}`)
}
assert.ok(
  taskPage.includes("v-hasPermi=\"['mes:pro-auto-schedule:preview']\"") &&
    !taskPage.includes("v-hasPermi=\"['mes:pro-task:create']\""),
  'Task page auto schedule entry must be gated by mes:pro-auto-schedule:preview instead of mes:pro-task:create'
)

const feedbackPage = readUtf8(feedbackPagePath)
for (const fragment of [
  'openAttribution',
  '选择归属',
  '确认报工',
  '归属结果'
]) {
  assert.ok(feedbackPage.includes(fragment), `Feedback page must keep the import attribution workflow visible: ${fragment}`)
}

const backendController = readUtf8(backendControllerPath)
assert.ok(
  backendController.includes('@PostMapping("/production-order/create")') &&
    backendController.includes('createProductionOrder') &&
    backendController.includes('ErpKingdeeProductionOrderCreateReqVO'),
  'ERP backend controller must expose the production-order create endpoint'
)

const backendCreateService = readUtf8(backendCreateServicePath)
assert.ok(
  backendCreateService.includes('getEffectiveProperties') &&
    backendCreateService.includes('createAndSubmitProductionOrder') &&
    backendCreateService.includes('getTemplateBillNo') &&
    backendCreateService.includes('batchNumber'),
  'ERP backend create service must use effective Kingdee config and the configured template bill number'
)

const backendCreateReq = readUtf8(backendCreateReqPath)
for (const fragment of [
  'billNo',
  'materialNumber',
  'unitNumber',
  'quantity',
  'plannedStartDate',
  'plannedFinishDate',
  'batchNumber'
]) {
  assert.ok(backendCreateReq.includes(fragment), `ERP create request VO must include ${fragment}`)
}

const backendKingdeeOrder = readUtf8(backendKingdeeOrderPath)
assert.ok(
  backendKingdeeOrder.includes('batchNumber'),
  'Kingdee production order model must expose batchNumber parsed from ERP'
)

const backendKingdeeClient = readUtf8(backendKingdeeClientPath)
for (const fragment of ['FLot.FNumber', 'INDEX_BATCH_NUMBER', 'setBatchNumber', 'getBatchNumber']) {
  assert.ok(
    backendKingdeeClient.includes(fragment),
    `Kingdee production order client must include batch field fragment: ${fragment}`
  )
}

const backendMesSync = readUtf8(backendMesSyncPath)
for (const fragment of ['setBatchCode', 'getBatchNumber', 'batchCode']) {
  assert.ok(
    backendMesSync.includes(fragment),
    `MES Kingdee production order sync must map batch to work order fragment: ${fragment}`
  )
}

console.log('PASS: smart scheduling smoke real flow static contract')
