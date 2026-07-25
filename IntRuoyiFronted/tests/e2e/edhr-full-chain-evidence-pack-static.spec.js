const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const scriptPath = path.join(repoRoot, 'tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js')
const source = fs.readFileSync(scriptPath, 'utf8')
const detailPagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const detailPageSource = fs.readFileSync(detailPagePath, 'utf8')
const backendServicePath = path.resolve(repoRoot, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java')
const backendServiceSource = fs.readFileSync(backendServicePath, 'utf8')

assert.match(
  source,
  /const MIN_DISTINCT_ACTORS = Number\(process\.env\.EDHR_FULL_E2E_MIN_DISTINCT_ACTORS \|\| \(ADMIN_SINGLE_ACTOR \? 1 : 4\)\)/,
  '完整演练默认多用户模式至少需要 4 个不同真实账号，admin 授权模式显式降为单账号。'
)


const createBatchStart = source.indexOf('async function createBatchByUi')
const createBatchEnd = source.indexOf('function isSameBatchDetailPage', createBatchStart)
const createBatchBlock = source.slice(createBatchStart, createBatchEnd)

assert.ok(createBatchBlock.includes("const detail = await loadBatchDetailByUi(page, batchExecutionId, '创建后批次详情')"), '创建批次后必须使用返回的批次 ID 重新加载详情接口。')
assert.ok(createBatchBlock.includes('assert.equal(Number(detail.id), batchExecutionId'), '创建后详情必须校验接口返回 ID 与创建响应 ID 一致。')
assert.ok(!createBatchBlock.includes('getByText(CREATE_BATCH_CODE)'), '创建批次后不得依赖列表或当前页立即显示批次号文本。')

assert.ok(!source.includes('|| 922045'), '完整演练不得继续使用历史固定 routeId 默认值。')
assert.ok(!source.includes('CODX70915957-T'), '完整演练不得默认使用非当前授权租户的旧 OQC 质检方案。')
assert.ok(!source.includes('CODX71027874-C'), '完整演练不得默认使用非当前授权租户的旧 OQC 客户。')
assert.ok(source.includes("EDHR-REHEARSAL2-OQC-T"), '完整演练默认 OQC 方案必须使用当前授权租户页面可见数据。')
assert.ok(source.includes('const existingByName = indicatorPane'), 'OQC 指标子表必须支持按页面显示名称识别已存在指标。')
assert.ok(source.includes('OQC_INDICATOR_NAME, OQC_INDICATOR_NAME'), 'OQC 指标选择弹窗必须按检测项名称定位当前页面行。')
assert.ok(source.includes("code=${encodeURIComponent(OQC_TEMPLATE_CODE)}"), 'OQC 模板搜索必须等待带 code 参数的真实分页响应。')
assert.ok(source.includes(".el-table__body-wrapper tbody tr, .el-table__row"), 'OQC 模板行定位必须兼容当前 Element Plus 表格行结构。')
assert.ok(source.includes("EDHR-REHEARSAL2-CUSTOMER"), '完整演练默认 OQC 客户必须使用当前授权租户页面可见数据。')
assert.ok(source.includes('const EXPLICIT_CREATE_ROUTE_ID = Number(process.env.EDHR_FULL_E2E_ROUTE_ID || 0)'), '完整演练只能把 EDHR_FULL_E2E_ROUTE_ID 作为显式覆盖参数。')
assert.ok(source.includes('function chooseCreateRouteOption(routeOptions)'), '创建批次必须从当前工单真实路线选项解析 routeId。')
assert.ok(createBatchBlock.includes('const routeOptions = await routeOptionsPromise'), '创建批次必须读取当前工单 route-options 响应。')
assert.ok(createBatchBlock.includes('const selectedRouteOption = chooseCreateRouteOption(routeOptions)'), '创建批次必须通过真实 route-options 选择路线。')
assert.ok(createBatchBlock.includes('await ensureRouteCloseRule(page, closeOwner, selectedRouteId)'), '关闭责任规则必须使用真实选中的 routeId 保存。')


const openFillTaskStart = source.indexOf('async function openFillTaskFromBoard')
const openFillTaskEnd = source.indexOf('async function fillEditableControls', openFillTaskStart)
const openFillTaskBlock = source.slice(openFillTaskStart, openFillTaskEnd)

assert.ok(openFillTaskBlock.includes('waitForCurrentUrl('), '处理待办后必须轮询当前 SPA URL，不能依赖 load 导航事件。')
assert.ok(!openFillTaskBlock.includes('await page.waitForURL('), '处理待办后的 URL 等待不得使用 waitForURL 默认 load 等待。')

assert.ok(openFillTaskBlock.includes('ENDPOINTS.batchTaskOpen'), '工作任务台处理填写/返工待办必须等待 task/open 响应生成 execution。')
assert.ok(!openFillTaskBlock.includes("await clickVisibleButton(row, '处理'"), '工作任务台处理按钮不得从不含固定操作列的主表格行内点击。')
assert.ok(source.includes('async function clickWorkTaskBoardActionButton'), '工作任务台必须封装按目标行定位操作列按钮的逻辑。')
assert.ok(openFillTaskBlock.includes("clickWorkTaskBoardActionButton(page, row, '处理'"), '工作任务台处理必须点击与目标待办行同序号的操作列按钮。')
assert.ok(!openFillTaskBlock.includes("waitForVisibleEnabledButton(page, '处理'"), '工作任务台处理不得点击页面第一个可见处理按钮，避免命中过期或其他待办。')

const loadBatchDetailStart = source.indexOf('async function loadBatchDetailByUi')
const loadBatchDetailEnd = source.indexOf('async function syncBatchByUi', loadBatchDetailStart)
const loadBatchDetailBlock = source.slice(loadBatchDetailStart, loadBatchDetailEnd)

assert.ok(loadBatchDetailBlock.includes('page.waitForResponse'), '批次详情加载必须观察真实页面详情接口请求。')
assert.ok(loadBatchDetailBlock.includes('apiGet(page, auth, ENDPOINTS.batchGet'), '批次详情结构化状态必须通过同一登录会话只读 API 获取，避免抢读导航响应体。')
assert.ok(!loadBatchDetailBlock.includes('const detail = await detailPromise'), '批次详情加载不得把导航响应体解析结果作为结构化状态。')

const confirmCellRulesStart = source.indexOf('async function ensureBatchTaskCellRulesConfirmedByUi')
const confirmCellRulesEnd = source.indexOf('async function openTaskByUi', confirmCellRulesStart)
const confirmCellRulesBlock = source.slice(confirmCellRulesStart, confirmCellRulesEnd)

assert.ok(confirmCellRulesStart >= 0, '创建批次后必须通过真实规则弹窗确认当前批次绑定报表的填写规则。')
assert.ok(confirmCellRulesBlock.includes('ROUTES.batchRecordFormList'), '填写规则确认必须打开批记录表单列表真实页面。')
assert.ok(confirmCellRulesBlock.includes('action=cellRules'), '填写规则确认必须使用页面 cellRules 动作打开真实弹窗。')
assert.ok(confirmCellRulesBlock.includes('ENDPOINTS.cellRules'), '填写规则确认必须等待真实 cell-rules 读写接口。')
assert.ok(confirmCellRulesBlock.includes('unreviewedFillableCellCount'), '填写规则确认必须以后端待确认数量作为保存门禁。')
assert.ok(confirmCellRulesBlock.includes('保存规则'), '填写规则确认必须点击真实页面保存规则按钮。')
assert.ok(confirmCellRulesBlock.includes('cell-rule-confirmation.json'), '填写规则确认必须写入本轮证据包。')

const createFlowStart = source.indexOf('async function runCreateBatchFlow')
const createFlowEnd = source.indexOf('async function verifyBatchDetailUi', createFlowStart)
const createFlowBlock = source.slice(createFlowStart, createFlowEnd)

assert.ok(createFlowBlock.includes('ensureBatchTaskCellRulesConfirmedByUi(ownerPage, created.batch)'), '创建批次后、打开填写任务前必须先确认批次绑定报表规则。')

assert.ok(source.includes('function isActiveRouteFormTask'), '完整演练必须只从 activeWorkTaskId 明确存在的路线任务进入填写。')
assert.ok(source.includes('Number(task.activeWorkTaskId || 0) > 0 && Number(task.status) !== 40'), '路线任务处理不得选择已完成或尚未生成工作待办的任务。')
assert.ok(createFlowBlock.includes('filter(isActiveRouteFormTask)'), '创建模式循环必须优先处理已有活动工作待办。')
assert.ok(createFlowBlock.includes('filter(isIncompleteRouteFormTask)'), '创建模式收尾必须用未完成任务而不是活动待办做完成性检查。')

const formCenterRouteTaskStart = source.indexOf('async function processRouteFormCenterTask')
const formCenterRouteTaskEnd = source.indexOf('async function processRouteTask', formCenterRouteTaskStart)
const formCenterRouteTaskBlock = source.slice(formCenterRouteTaskStart, formCenterRouteTaskEnd)

assert.ok(source.includes('function isFormCenterRouteTask'), '完整演练必须识别 FormCenter 动态表单/共享表单任务。')
assert.ok(source.includes('Number(task.formCenterInstanceId || 0) > 0'), 'FormCenter 任务识别必须接受仅返回 formCenterInstanceId 的活动待办。')
assert.ok(source.includes("slotType && slotType !== 'MAIN'"), 'FormCenter 任务识别必须覆盖 LOSS_REPORT 等非 MAIN 共享表单槽位。')
assert.ok(formCenterRouteTaskStart >= 0, '完整演练必须通过真实批次详情抽屉处理 FormCenter 动态表单任务。')
assert.ok(formCenterRouteTaskBlock.includes('.form-action-panel'), 'FormCenter 动态表单必须等待真实表单面板。')
assert.ok(formCenterRouteTaskBlock.includes('/form-center/instances/'), 'FormCenter 动态表单必须等待真实草稿和提交接口。')
assert.ok(formCenterRouteTaskBlock.includes('保存草稿'), 'FormCenter 动态表单必须通过真实保存草稿按钮。')
assert.ok(formCenterRouteTaskBlock.includes('name: /^提交$/'), 'FormCenter 动态表单必须通过真实提交按钮。')
assert.ok(formCenterRouteTaskBlock.includes('Number(opened.status) === 40'), 'FormCenter 共享实例已生效时必须识别后端已完成状态。')
assert.ok(formCenterRouteTaskBlock.includes('autoCompletedByEffectiveSharedInstance'), 'FormCenter 共享实例已生效时必须记录自动完成证据。')
assert.ok(detailPageSource.includes("opened.instanceScope === 'BATCH_SHARED'"), '批次详情页必须只对 BATCH_SHARED 已生效表单走自动完成刷新。')
assert.ok(detailPageSource.includes('opened.status === EDHR_BATCH_TASK_STATUS_APPROVED'), '批次详情页必须用后端完成状态阻止打开已生效表单抽屉。')
assert.ok(detailPageSource.includes('共享表单已生效，当前任务已自动完成'), '批次详情页必须向用户明确共享表单已自动完成。')
assert.ok(backendServiceSource.includes('completeAlreadyEffectiveBatchSharedRouteFormTask'), '后端 openTask 必须包含共享 FormCenter 已生效任务推进逻辑。')
assert.ok(backendServiceSource.includes('FormInstanceStatus.EFFECTIVE.name()'), '后端必须按 FormCenter 实例 EFFECTIVE 状态判断自动完成。')
assert.ok(backendServiceSource.includes('workTaskService.completeRouteFormFillAndCreateNextFill'), '后端必须复用路线表单正式完成链路推进后续任务。')

const specialNodeActionStart = source.indexOf('async function specialNodeAction')
const specialNodeActionEnd = source.indexOf('async function skipSpecialNode', specialNodeActionStart)
const specialNodeActionBlock = source.slice(specialNodeActionStart, specialNodeActionEnd)

assert.ok(specialNodeActionStart >= 0, '完整演练必须封装特殊节点操作。')
assert.ok(specialNodeActionBlock.includes("const actionLabel = actionName === '完成' ? '完成节点'"), '特殊节点完成动作必须匹配当前页面“完成节点”按钮。')
assert.ok(specialNodeActionBlock.includes(".edhr-batch-detail__special-process-task-group .edhr-batch-detail__process-task-group-head"), '特殊节点必须点击特殊节点任务按钮，不得误点普通工序或放行项。')
assert.ok(specialNodeActionBlock.includes(".edhr-batch-detail__process-task-group.is-active, .edhr-batch-detail__release-process-item.is-active"), '特殊节点操作前必须等待批次复盘初始选中态稳定，避免异步默认选中覆盖目标特殊节点。')
assert.ok(specialNodeActionBlock.includes("targetActiveGroup"), '特殊节点操作必须确认目标节点自身已激活，不能只等待任意特殊节点操作区。')
assert.ok(specialNodeActionBlock.includes(".edhr-batch-detail__special-node-action-grid:visible"), '特殊节点动作必须限定在可见特殊节点操作区内。')
assert.ok(specialNodeActionBlock.includes("actionGrid.locator('.edhr-batch-detail__rail-task-action:visible')"), '特殊节点动作按钮必须在目标 active 后的可见特殊节点操作区内定位。')
assert.ok(specialNodeActionBlock.includes("textContent({ timeout: 1000 })"), '特殊节点明细文本探测不得长时间阻塞当前按钮定位。')

const processRouteTaskStart = source.indexOf('async function processRouteTask')
const processRouteTaskEnd = source.indexOf('async function closeBatch', processRouteTaskStart)
const processRouteTaskBlock = source.slice(processRouteTaskStart, processRouteTaskEnd)

assert.ok(source.includes('function shouldTakeOverRouteTask'), 'admin-only 完整演练必须识别当前账号无 OPEN_FORM 的活动任务。')
assert.ok(source.includes('async function openFillTaskFromBatchDetailTakeover'), 'admin-only 完整演练必须通过批次详情正式管理员接管路径处理非本人任务。')
assert.ok(source.includes("管理员接管并填写"), '管理员接管必须点击当前页面正式“管理员接管并填写”按钮。')
assert.ok(source.includes('ENDPOINTS.flowInterventionTransfer'), '管理员接管必须等待正式流程干预 transfer 响应。')
assert.ok(source.includes("getByRole('button', { name: '批记录' })"), '管理员接管前必须切换到批记录填写方式，避免默认打开记录本。')
assert.ok(processRouteTaskBlock.includes('openFillTaskFromBatchDetailTakeover'), '普通工序处理必须在无 OPEN_FORM 时走批次详情接管路径。')
assert.ok(processRouteTaskBlock.includes('openFillTaskFromBoard'), '普通工序处理仍需在当前账号可填写时复用工作台 task/open 结果。')
assert.ok(processRouteTaskBlock.includes('const opened = fillTaskUrl.openedTask'), '工作台进入填写页后不得再次调用批次详情打开任务。')
assert.ok(!processRouteTaskBlock.includes('const opened = await openTaskByUi(fillPage, pendingTask)'), '工作台处理已经进入填写页后禁止重复 openTaskByUi。')
assert.ok(processRouteTaskBlock.includes('const reworkOpened = reworkUrl.openedTask'), '返工处理也必须复用工作台 task/open 结果。')
assert.ok(!processRouteTaskBlock.includes('reworkUrl.pathname === ROUTES.executionDetail ? {} : await openTaskByUi'), '返工进入填写页后禁止以批次详情打开作为降级路径。')

const submitDialogStart = source.indexOf('async function openSubmitDialog')
const submitDialogEnd = source.indexOf('async function chooseReviewAssignees', submitDialogStart)
const submitDialogBlock = source.slice(submitDialogStart, submitDialogEnd)
const submitExecutionStart = source.indexOf('async function submitExecution')
const submitExecutionEnd = source.indexOf('async function approveExecution', submitExecutionStart)
const submitExecutionBlock = source.slice(submitExecutionStart, submitExecutionEnd)

assert.ok(submitDialogStart >= 0, '完整演练必须封装提交执行电子签名弹窗打开逻辑。')
assert.ok(submitDialogBlock.includes('.edhr-fill-workspace__submit-sign-dialog'), '提交执行必须等待当前真实电子签名弹窗类名，不能依赖历史标题。')
assert.ok(submitDialogBlock.includes('input[type="password"]'), '提交执行弹窗打开后必须确认密码输入框可见。')
assert.ok(!submitDialogBlock.includes("hasText: '提交 eDHR 执行'"), '提交执行弹窗不得依赖已废弃标题“提交 eDHR 执行”。')
assert.ok(/\/确\\s\*认\(\?:\\s\*提\\s\*交\)\?\//.test(submitExecutionBlock), '提交执行确认按钮必须兼容当前“确认”和历史“确认提交”按钮文案。')

for (const token of [
  'EDHR_FULL_E2E_ADMIN_SINGLE_ACTOR',
  "ADMIN_SINGLE_ACTOR ? '芋道源码' : '测试租户'",
  "ADMIN_SINGLE_ACTOR ? 'admin' : defaults.username",
  "ADMIN_SINGLE_ACTOR ? '瑛泰管理员' : defaults.displayName",
  'actorConfig.signaturePassword = actorConfig.signaturePassword || actorConfig.password',
  'ADMIN_SINGLE_ACTOR ? 1 : 4',
  'ADMIN_SINGLE_ACTOR && !fs.existsSync(GOAL_FILE)',
  'if (ADMIN_SINGLE_ACTOR && goals.length === 0) return'
]) {
  assert.ok(source.includes(token), `admin 单账号授权模式必须显式受控且不落盘密码：${token}`)
}

for (const token of [
  "require('node:child_process')",
  "require('node:os')",
  'EDHR_FULL_E2E_EVIDENCE_DIR',
  'ensureEvidenceDir',
  'writeEvidenceJson',
  'captureEvidence',
  'run-config.json',
  'final-summary.json',
  'archive-${targetBatchExecutionId}.pdf',
  '01-owner-batch-entry',
  '02-created-or-opened-batch',
  '03-batch-review-page'
]) {
  assert.ok(source.includes(token), `完整演练必须保留证据包能力：${token}`)
}

for (const token of ['EDHR_PDF_TEXT_PYTHON', 'from pypdf import PdfReader', '最终 PDF 文本解析失败']) {
  assert.ok(source.includes(token), `最终 PDF 证据校验必须使用标准 PDF 文本解析并 fail-fast：${token}`)
}

assert.match(
  source,
  /page\.screenshot\(\{ path: screenshotPath, fullPage: true \}\)/,
  '证据包必须保存关键页面截图，不能只依赖控制台 PASS。'
)

console.log('PASS: eDHR full-chain evidence pack static contract')
