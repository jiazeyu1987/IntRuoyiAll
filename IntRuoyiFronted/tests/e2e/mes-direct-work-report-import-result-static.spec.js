const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const importFormPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/ThirdPartyFeedbackImportForm.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/feedback/index.ts')

assert(fs.existsSync(importFormPath), `直接报工导入弹窗必须存在：${importFormPath}`)
assert(fs.existsSync(apiPath), `报工 API 类型文件必须存在：${apiPath}`)

const importFormSource = fs.readFileSync(importFormPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert(
  apiSource.includes('importedCount: number')
    && apiSource.includes('submittedCount: number')
    && apiSource.includes('skippedRows?: number')
    && apiSource.includes('feedbackCodes: string[]')
    && apiSource.includes('importRecordIds: number[]')
    && apiSource.includes('directWorkReportDetails?: DirectWorkReportImportDetailVO[]')
    && apiSource.includes('directWorkReportSkipWarnings?: DirectWorkReportSkipWarningVO[]')
    && apiSource.includes('resultCode?: string')
    && apiSource.includes('resultMessage?: string')
    && apiSource.includes('reasonCode?: string'),
  '直接报工进度导入接口类型必须保留汇总统计、来源记录编号、已更新进度明细和未更新提示。'
)

assert(
  importFormSource.includes('directImportResultVisible')
    && importFormSource.includes('directImportResult.value = result')
    && importFormSource.includes('directImportResultVisible.value = true'),
  '直接报工导入完成后必须打开重构前的大弹框结果面板。'
)

assert(
  importFormSource.includes('title="直接报工导入结果"')
    && importFormSource.includes('groupedDirectWorkOrders')
    && importFormSource.includes('selectedDirectWorkOrder')
    && importFormSource.includes('selectedWorkOrderRows')
    && importFormSource.includes('directWorkReportSkipWarnings')
    && importFormSource.includes('warnings: []')
    && importFormSource.includes('group.warnings.length')
    && importFormSource.includes('未更新 {{ group.warnings.length }}')
    && importFormSource.includes(':title="formatWorkOrderWarningReasons(group.warnings)"')
    && importFormSource.includes('formatWorkOrderWarningReasons')
    && importFormSource.includes('direct-import-result__body')
    && importFormSource.includes('direct-import-result__work-orders')
    && importFormSource.includes('direct-import-result__detail-panel')
    && importFormSource.includes('本次导入未更新排产进度'),
  '直接报工进度导入结果弹框必须保留按生产工单分组的双栏明细，并在工单卡片保留未更新计数和原因定位。'
)

assert(
  !importFormSource.includes('direct-import-result__stats')
    && !importFormSource.includes('direct-import-result__stat')
    && !importFormSource.includes('direct-import-result__stat-label')
    && !importFormSource.includes('direct-import-result__warnings')
    && !importFormSource.includes('direct-import-result__warnings-title')
    && !importFormSource.includes('directImportSkipWarnings')
    && !importFormSource.includes('direct-import-result__stat-label">工作表数')
    && !importFormSource.includes('direct-import-result__stat-label">创建报工数')
    && !importFormSource.includes('direct-import-result__stat-label">提交审批数')
    && !importFormSource.includes('direct-import-result__stat-label">跳过杂务行')
    && !importFormSource.includes('<div class="direct-import-result__warnings-title">未创建提示</div>'),
  '直接报工导入结果弹框顶部不应显示统计汇总卡片，也不应显示顶部未创建提示表格。'
)

assert(
  importFormSource.includes('const NON_INTERSECTION_DIRECT_WORK_REPORT_WARNING_CODES = new Set')
    && importFormSource.includes("'WORK_ORDER_NOT_FOUND'")
    && importFormSource.includes("'WORK_ORDER_NOT_UNIQUE'")
    && importFormSource.includes("'SCHEDULE_ORDER_NOT_FOUND'")
    && importFormSource.includes("'SCHEDULE_ORDER_NOT_UNIQUE'")
    && importFormSource.includes("'PROCESS_NOT_FOUND'")
    && importFormSource.includes("'PROCESS_NOT_ENABLED'")
    && importFormSource.includes("'PROCESS_NOT_UNIQUE'")
    && importFormSource.includes('isDirectWorkReportDisplayableDetail')
    && importFormSource.includes('isDirectWorkReportDisplayableWarning')
    && importFormSource.includes('getVisibleDirectWorkReportDetails')
    && importFormSource.includes('getVisibleDirectWorkReportWarnings')
    && importFormSource.includes('const warnings = getVisibleDirectWorkReportWarnings()')
    && importFormSource.includes('for (const warning of warnings)')
    && importFormSource.includes('ensureDirectWorkOrderGroup(workOrderCode, warning.productCode, warning.productName)')
    && importFormSource.includes('group.warnings.push(warning)')
    && importFormSource.includes('group.totalFeedbackQuantity += sumNumeric(warning.feedbackQuantity)')
    && importFormSource.includes('group.processCount = group.details.length + group.warnings.length'),
  '直接报工结果左侧生产工单卡片必须只覆盖 Excel 与有效排产工单/工序交集；非交集 warning 不能作为受影响订单展示。'
)

assert(
  importFormSource.includes('const selectedWorkOrderRows = computed')
    && importFormSource.includes("resultType: 'UPDATED'")
    && importFormSource.includes("resultType: 'SKIPPED'")
    && importFormSource.includes("DIRECT_WORK_REPORT_OVER_REMAINING_CODE = 'OVER_REMAINING_QUANTITY'")
    && importFormSource.includes('isDirectWorkReportOverRemaining')
    && importFormSource.includes(':type="isDirectWorkReportOverRemaining(row) ? \'warning\' : \'success\'"')
    && importFormSource.includes('direct-import-result__warning-text')
    && importFormSource.includes(':data=\"selectedWorkOrderRows\"')
    && importFormSource.includes("row.resultType === 'SKIPPED'")
    && importFormSource.includes('本次报工 {{ formatQuantity(row.feedbackQuantity) }}')
    && importFormSource.includes('已报 {{ formatQuantity(row.reportedQuantity) }} / 剩余'),
  '右侧明细表必须同时展示已更新进度和未更新定位行的工序、数量；超剩余更新必须用黄色提醒，不能在 warning-only 工单下显示空表。'
)

const realFlowPath = path.resolve(process.cwd(), 'tests/e2e/mes-direct-work-report-import-real-flow.e2e.js')
assert(fs.existsSync(realFlowPath), `直接报工真实导入脚本必须存在：${realFlowPath}`)
const realFlowSource = fs.readFileSync(realFlowPath, 'utf8')

assert(
  realFlowSource.includes('locatedWorkOrderCodes')
    && realFlowSource.includes('directWorkReportDetails')
    && realFlowSource.includes('directWorkReportSkipWarnings')
    && realFlowSource.includes('isVisibleDirectWorkReportWarning')
    && realFlowSource.includes('visibleSkipWarnings')
    && realFlowSource.includes("resultDialog.locator('.direct-import-result__work-order-card').allTextContents()")
    && realFlowSource.includes('visibleWorkOrderCodes')
    && realFlowSource.includes('左侧生产工单卡片数量必须等于可展示交集生产工单集合')
    && realFlowSource.includes('左侧生产工单列表必须包含可展示交集生产工单'),
  '直接报工真实 E2E 必须断言左侧生产工单卡片覆盖创建明细和可展示未创建提示的交集工单，防止非交集 warning 被展示。'
)

assert(
  realFlowSource.includes('progressBefore')
    && realFlowSource.includes('progressAfterFirstImport')
    && realFlowSource.includes('progressAfterSecondImport')
    && realFlowSource.includes('submittedCount === 0')
    && realFlowSource.includes('feedbackCodes.length === 0')
    && realFlowSource.includes('第二次导入必须继续累计排产进度'),
  '直接报工真实 E2E 必须验证该接口只更新排产进度、不创建报工单，并且重复导入继续累计。'
)

assert(
  realFlowSource.includes("!resultText.includes('工作表数')")
    && realFlowSource.includes("!resultText.includes('创建报工数')")
    && realFlowSource.includes("!resultText.includes('提交审批数')")
    && realFlowSource.includes("!resultText.includes('跳过杂务行')")
    && realFlowSource.includes("!resultText.includes('未创建提示')"),
  '直接报工真实 E2E 必须跟随当前 UI 契约：不再要求顶部统计汇总和顶部未创建提示表。'
)

assert(
  !importFormSource.includes('direct-import-result__selected-warnings')
    && !importFormSource.includes('selectedWorkOrderWarnings'),
  '右侧选中工单详情区不应重复展示未更新提示块；未更新入口只保留在左侧工单卡片数量里。'
)

assert(
  importFormSource.includes('.direct-import-result__body {\n  display: grid;\n  grid-template-columns: 230px minmax(0, 1fr);\n  gap: 12px;\n  height: 460px;')
    && importFormSource.includes('.direct-import-result__work-orders {\n  height: 100%;\n  min-height: 0;'),
  '左侧生产工单列表必须占满结果 body 高度，不能在列表下方留下独立空白区域。'
)

assert(
  !importFormSource.includes('label="状态 / 原因"')
    && !importFormSource.includes('formatDirectImportDetailStatus')
    && !importFormSource.includes('row.remark'),
  '直接报工进度导入结果明细不应恢复旧待归属状态列或原始备注，未更新行用本次报工数量和简短原因在当前明细表中表达。'
)

assert(
  !importFormSource.includes('direct-import-result__stat-label">导入记录数')
    && !importFormSource.includes('direct-import-result__stat-label">待归属数')
    && !importFormSource.includes('formatDirectImportStatus')
    && !importFormSource.includes('报工单号：${feedbackCodesText}'),
  '直接报工导入结果弹框不应恢复为五卡片待归属面板或小 alert 文案。'
)

assert(
  !importFormSource.includes('进度增加 / 报工单号')
    && !importFormSource.includes('未创建')
    && !importFormSource.includes('已创建')
    && !importFormSource.includes("resultType === 'CREATED'"),
  '直接报工导入结果主语义必须是更新排产进度，不得继续以创建报工单或未创建报工单为主语义。'
)

console.log('PASS: MES direct work report import result static contract')
