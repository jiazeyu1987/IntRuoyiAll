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
const backendArchivePdfRendererPath = path.resolve(repoRoot, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchArchivePrintablePdfRenderer.java')
const backendArchivePdfRendererSource = fs.readFileSync(backendArchivePdfRendererPath, 'utf8')

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
assert.ok(source.includes("const CREATE_WORK_ORDER_CODE = process.env.EDHR_FULL_E2E_WORK_ORDER_CODE || ''"), '创建模式必须显式传入当前授权范围内真实生产工单，不得默认使用过期工单。')
assert.ok(!source.includes("|| '881MO090863'"), '完整演练不得默认使用已失效的历史生产工单。')
assert.ok(source.includes('process.env.EDHR_FULL_E2E_REJECT_FIRST_ROUTE_TASK') && source.includes(': !ADMIN_SINGLE_ACTOR'), 'admin 单账号金手指模式默认不得强制走普通工序审批驳回返工分支，多用户模式仍默认覆盖返工。')
assert.ok(!source.includes('CODX70915957-T'), '完整演练不得默认使用非当前授权租户的旧 OQC 质检方案。')
assert.ok(!source.includes('CODX71027874-C'), '完整演练不得默认使用非当前授权租户的旧 OQC 客户。')
assert.ok(source.includes("EDHR-REHEARSAL2-OQC-T"), '完整演练默认 OQC 方案必须使用当前授权租户页面可见数据。')
assert.ok(source.includes("const EXPLICIT_OQC_PRODUCT_ITEM_CODE = process.env.EDHR_FULL_E2E_OQC_PRODUCT_ITEM_CODE || ''"), 'OQC 产品物料只能作为显式覆盖参数，不得硬编码默认物料。')
assert.ok(source.includes('async function resolveOqcProductItemCode(page, batchDetail)'), 'OQC 产品物料必须从当前批次详情解析。')
assert.ok(source.includes('ENDPOINTS.itemGet'), '当批次 productCode 是历史数字 ID 时，OQC 必须通过真实物料详情接口解析 code。')
assert.ok(source.includes('/mes/md/item/get'), 'OQC 物料解析必须使用正式物料详情接口。')
assert.ok(source.includes('batchDetail?.productId || batchProductCode'), 'OQC 物料解析必须优先绑定当前 eDHR 批次 productId，确保同批次同物料回写成品检卷宗。')
assert.ok(!source.includes("|| 'YXN.037.011.1002'"), '完整演练不得默认使用与当前生产工单不一致的历史 OQC 产品物料。')
assert.ok(source.includes('const existingByName = indicatorPane'), 'OQC 指标子表必须支持按页面显示名称识别已存在指标。')
assert.ok(source.includes('OQC_INDICATOR_NAME, OQC_INDICATOR_NAME'), 'OQC 指标选择弹窗必须按检测项名称定位当前页面行。')
assert.ok(source.includes("code=${encodeURIComponent(OQC_TEMPLATE_CODE)}"), 'OQC 模板搜索必须等待带 code 参数的真实分页响应。')
assert.ok(source.includes(".el-table__body-wrapper tbody tr, .el-table__row"), 'OQC 模板行定位必须兼容当前 Element Plus 表格行结构。')
assert.ok(source.includes('scrollIntoViewIfNeeded()'), '弹窗内可见按钮点击前必须先滚动到可点击区域。')
assert.ok(source.includes('force: true'), '弹窗内可见启用按钮被布局判定视口外时必须允许 force 点击同一按钮。')
assert.ok(source.includes('element.click()'), '弹窗内可见启用按钮被布局判定视口外且 force 失败时必须 DOM 点击同一按钮。')
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
assert.ok(loadBatchDetailBlock.includes('Array.isArray(detail.tasks)'), '批次详情结构化状态必须包含任务列表，缺失时不得误判路线任务已完成。')

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
const resumeFlowStart = source.indexOf('async function runResumeBatchFlow')
const resumeFlowEnd = source.indexOf('function collectActorsFromTimeline', resumeFlowStart)
const resumeFlowBlock = source.slice(resumeFlowStart, resumeFlowEnd)
const verifyBatchDetailStart = source.indexOf('async function verifyBatchDetailUi')
const verifyBatchDetailEnd = source.indexOf('async function runCreateBatchFlow', verifyBatchDetailStart)
const verifyBatchDetailBlock = source.slice(verifyBatchDetailStart, verifyBatchDetailEnd)

assert.ok(createFlowBlock.includes('ensureBatchTaskCellRulesConfirmedByUi(ownerPage, created.batch)'), '创建批次后、打开填写任务前必须先确认批次绑定报表规则。')
assert.ok(createFlowBlock.includes('ensureOqcTemplateBindingByUi(ownerPage, oqcProductItemCode)'), '创建模式必须在取得批次产品后再绑定 OQC 质检方案产品关联。')
assert.ok(resumeFlowBlock.includes('ensureOqcTemplateBindingByUi(ownerPage, oqcProductItemCode)'), '续跑模式必须在取得批次产品后再绑定 OQC 质检方案产品关联。')
assert.ok(verifyBatchDetailBlock.includes('const identityTexts = [batch.batchCode, batch.batchExecutionCode, batch.workOrderCode]'), '最终详情页验收必须接受批次号、批次执行编码或工单号任一身份标识。')
assert.ok(verifyBatchDetailBlock.includes('批次详情未显示批次身份信息'), '最终详情页验收缺失身份标识时必须 fail-fast 并输出页面片段。')
assert.ok(source.includes('async function runResumeBatchFlow'), '完整演练必须支持用当前任务已创建的批次继续执行剩余放行/归档路径。')
assert.ok(source.includes('BATCH_EXECUTION_ID') && source.includes('runResumeBatchFlow'), '续跑入口必须显式绑定 EDHR_FULL_E2E_BATCH_EXECUTION_ID，不能隐式选择历史批次。')
assert.ok(source.includes('function isBatchClosedOrArchived'), '续跑模式必须显式识别已关闭/已归档批次状态。')
assert.ok(source.includes('async function assertClosedBatchReadyForArchive'), '续跑已关闭批次必须先校验放行事务和 closedAt，再进入归档。')
assert.ok(resumeFlowBlock.includes('if (isBatchClosedOrArchived(detail))'), '续跑已关闭批次不得重复执行特殊节点、OQC 和放行。')
assert.ok(resumeFlowBlock.includes("assertClosedBatchReadyForArchive(closePage, batchId, '续跑已关闭批次放行状态校验')"), '续跑已关闭批次必须通过真实放行工作区校验 RELEASED 状态。')
assert.ok(resumeFlowBlock.includes('if (!isBatchClosedOrArchived(detail))'), '续跑未关闭批次仍必须完成正式电子签名放行后再归档。')
assert.ok(resumeFlowBlock.includes('resumedFromClosedBatch: oqcResult === null'), '续跑最终证据必须标记是否从已关闭批次继续归档。')
assert.ok(source.includes('const ARCHIVED_BATCH_STATUS = 40'), '完整演练必须显式识别已归档状态。')
assert.ok(source.includes('async function openArchivePrintDrawerByUi'), '已归档续跑必须能从批次详情真实打开归档打印抽屉。')
assert.ok(source.includes('.edhr-batch-detail__release-process-item'), '已归档续跑必须点击真实放行流程节点，而不是按混合按钮文本猜测。')
assert.ok(source.includes('while (Date.now() - startedAt < 60000)'), '已归档续跑必须循环确认放行节点已激活，避免二级数据加载覆盖初始选择。')
assert.ok(source.includes("locator('.edhr-batch-detail__release-process-item.is-active')"), '已归档续跑点击放行节点后必须等待 active 放行节点可见。')
assert.ok(source.includes("startsWith('99放行')"), '已归档续跑必须在页面内按真实按钮文本触发放行节点点击。')
assert.ok(source.includes("waitForReleaseRailActionButton(page, /^归\\s*档\\s*打\\s*印$/, '归档打印', 1000)"), '已归档续跑必须在循环中短等待归档打印动作真实出现。')
assert.ok(resumeFlowBlock.includes('if (Number(detail.status) === ARCHIVED_BATCH_STATUS)'), '已归档续跑不得继续查找 TODO 最终归档待办。')
assert.ok(resumeFlowBlock.includes('openArchivePrintDrawerByUi(archivePage, batchId)'), '已归档续跑必须从批次详情打开归档抽屉下载打印。')
assert.ok(resumeFlowBlock.includes('/mes/pro/edhr-batch-execution-archive/latest'), '已归档续跑必须读取最新归档记录作为最终归档对象。')

const createSterilizationIndex = createFlowBlock.indexOf("await completeSpecialNode(ownerPage, batchId, '灭菌报告')")
const createOqcIndex = createFlowBlock.indexOf('const oqcResult = await createAndFinishOqcByUi')
const createFinishedReportIndex = createFlowBlock.indexOf("await skipSpecialNode(ownerPage, batchId, '成品检报告'")
assert.ok(
  createSterilizationIndex >= 0 && createOqcIndex > createSterilizationIndex && createFinishedReportIndex > createOqcIndex,
  '创建模式必须先完成灭菌，再通过 OQC 回写成品检卷宗证据，最后处理成品检报告/记录特殊节点。'
)

const resumeSterilizationIndex = resumeFlowBlock.indexOf("await completeSpecialNode(ownerPage, batchId, '灭菌报告')")
const resumeOqcIndex = resumeFlowBlock.indexOf('oqcResult = await createAndFinishOqcByUi')
const resumeFinishedReportIndex = resumeFlowBlock.indexOf("await skipSpecialNode(ownerPage, batchId, '成品检报告'")
assert.ok(
  resumeFlowStart >= 0 && resumeSterilizationIndex >= 0 && resumeOqcIndex > resumeSterilizationIndex && resumeFinishedReportIndex > resumeOqcIndex,
  '续跑模式必须按灭菌 -> OQC 回写 -> 成品检特殊节点的顺序补齐剩余链路。'
)

assert.ok(source.includes('function isActiveRouteFormTask'), '完整演练必须只从 activeWorkTaskId 明确存在的路线任务进入填写。')
assert.ok(source.includes('Number(task.activeWorkTaskId || 0) > 0 && Number(task.status) !== 40'), '路线任务处理不得选择已完成或尚未生成工作待办的任务。')
assert.ok(createFlowBlock.includes('filter(isActiveRouteFormTask)'), '创建模式循环必须优先处理已有活动工作待办。')
assert.ok(createFlowBlock.includes('filter(isIncompleteRouteFormTask)'), '创建模式收尾必须用未完成任务而不是活动待办做完成性检查。')
assert.ok(createFlowBlock.includes('routeLoopCompleted'), '创建模式路线循环达到 guard 上限时必须 fail fast，不得继续处理特殊节点。')

const formCenterRouteTaskStart = source.indexOf('async function processRouteFormCenterTask')
const formCenterRouteTaskEnd = source.indexOf('async function processRouteTask', formCenterRouteTaskStart)
const formCenterRouteTaskBlock = source.slice(formCenterRouteTaskStart, formCenterRouteTaskEnd)

assert.ok(source.includes('async function waitForRouteTaskCompletedByUi'), '完整演练必须在 FormCenter 提交后轮询批次任务完成状态。')
assert.ok(source.includes('function isFormCenterRouteTask'), '完整演练必须识别 FormCenter 动态表单/共享表单任务。')
assert.ok(source.includes('Number(task.formCenterInstanceId || 0) > 0'), 'FormCenter 任务识别必须接受仅返回 formCenterInstanceId 的活动待办。')
assert.ok(source.includes("slotType && slotType !== 'MAIN'"), 'FormCenter 任务识别必须覆盖 LOSS_REPORT 等非 MAIN 共享表单槽位。')
assert.ok(formCenterRouteTaskStart >= 0, '完整演练必须通过真实批次详情抽屉处理 FormCenter 动态表单任务。')
assert.ok(formCenterRouteTaskBlock.includes('.form-action-panel'), 'FormCenter 动态表单必须等待真实表单面板。')
assert.ok(formCenterRouteTaskBlock.includes('/form-center/instances/'), 'FormCenter 动态表单必须等待真实草稿和提交接口。')
assert.ok(formCenterRouteTaskBlock.includes('保存草稿'), 'FormCenter 动态表单必须通过真实保存草稿按钮。')
assert.ok(formCenterRouteTaskBlock.includes('name: /^提交$/'), 'FormCenter 动态表单必须通过真实提交按钮。')
assert.ok(formCenterRouteTaskBlock.includes('waitForRouteTaskCompletedByUi'), 'FormCenter 动态表单提交后必须等待 eDHR 批次任务完成和下一任务推进。')
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
assert.ok(source.includes('async function resolveSpecialNodeTaskByLabel'), '特殊节点操作必须先通过批次详情接口解析目标 batchTaskId。')
assert.ok(specialNodeActionBlock.includes('if (isTaskResolved(targetTask))'), '特殊节点已完成判断必须使用目标 batchTaskId 的后端状态，不能误读页面第一个节点明细。')
assert.ok(specialNodeActionBlock.includes('loadBatchDetailTaskByUi'), '特殊节点操作必须通过 batchTaskId 直达目标节点，避免默认选中态干扰。')
assert.ok(specialNodeActionBlock.includes(".edhr-batch-detail__special-process-task-group .edhr-batch-detail__process-task-group-head"), '特殊节点必须点击特殊节点任务按钮，不得误点普通工序或放行项。')
assert.ok(specialNodeActionBlock.includes(".edhr-batch-detail__process-task-group.is-active, .edhr-batch-detail__release-process-item.is-active"), '特殊节点操作前必须等待批次复盘初始选中态稳定，避免异步默认选中覆盖目标特殊节点。')
assert.ok(specialNodeActionBlock.includes("targetActiveGroup"), '特殊节点操作必须确认目标节点自身已激活，不能只等待任意特殊节点操作区。')
assert.ok(specialNodeActionBlock.includes(".edhr-batch-detail__special-node-action-grid:visible"), '特殊节点动作必须限定在可见特殊节点操作区内。')
assert.ok(specialNodeActionBlock.includes("actionGrid.locator('.edhr-batch-detail__rail-task-action:visible')"), '特殊节点动作按钮必须在目标 active 后的可见特殊节点操作区内定位。')
assert.ok(!specialNodeActionBlock.includes("locator('.edhr-batch-detail__rail-task-detail')"), '特殊节点完成状态不得读取非唯一 rail-task-detail，避免把其他节点状态误判为目标节点已完成。')
assert.ok(specialNodeActionBlock.includes("replace(/\\s+/g, ' ')"), '特殊节点证据文本必须按空白正则归一化，不得误写成 /s+/。')
assert.ok(specialNodeActionBlock.includes('let buttonReady = false'), '特殊节点动作按钮必须等待可用状态稳定，避免刚渲染时误判 disabled。')
assert.ok(specialNodeActionBlock.includes('detailRefreshPromise'), '特殊节点提交后必须等待批次详情刷新完成，避免连续处理成品检节点时读到旧选中态。')
assert.ok(specialNodeActionBlock.includes('ENDPOINTS.batchGet'), '特殊节点提交后的刷新等待必须绑定真实批次详情 GET 接口。')

const processRouteTaskStart = source.indexOf('async function processRouteTask')
const processRouteTaskEnd = source.indexOf('async function closeBatch', processRouteTaskStart)
const processRouteTaskBlock = source.slice(processRouteTaskStart, processRouteTaskEnd)
const closeBatchStart = source.indexOf('async function closeBatch')
const closeBatchEnd = source.indexOf('async function openArchiveTaskFromBoard', closeBatchStart)
const closeBatchBlock = source.slice(closeBatchStart, closeBatchEnd)
const archiveTaskStart = source.indexOf('async function openArchiveTaskFromBoard')
const archiveTaskEnd = source.indexOf('async function generateArchiveAndPrint', archiveTaskStart)
const archiveTaskBlock = source.slice(archiveTaskStart, archiveTaskEnd)
const generateArchiveStart = source.indexOf('async function generateArchiveAndPrint')
const generateArchiveEnd = source.indexOf('function extractPdfText', generateArchiveStart)
const generateArchiveBlock = source.slice(generateArchiveStart, generateArchiveEnd)
const downloadArchiveStart = source.indexOf('async function downloadAndPrintArchiveViaUi')
const downloadArchiveEnd = source.indexOf('async function loadArchiveDoneTaskByApi', downloadArchiveStart)
const downloadArchiveBlock = source.slice(downloadArchiveStart, downloadArchiveEnd)

assert.ok(source.includes('function shouldTakeOverRouteTask'), 'admin-only 完整演练必须识别当前账号无 OPEN_FORM 的活动任务。')
assert.ok(source.includes('async function openFillTaskFromBatchDetailTakeover'), 'admin-only 完整演练必须通过批次详情正式管理员接管路径处理非本人任务。')
assert.ok(source.includes('async function loadBatchDetailTaskByUi'), '管理员接管前必须支持通过 batchTaskId 直达目标批次任务。')
assert.ok(source.includes('batchTaskId=${taskId}'), '管理员接管前批次详情 URL 必须携带 batchTaskId，避免默认选中覆盖目标任务。')
assert.ok(source.includes("管理员接管并填写"), '管理员接管必须点击当前页面正式“管理员接管并填写”按钮。')
assert.ok(source.includes('ENDPOINTS.flowInterventionTransfer'), '管理员接管必须等待正式流程干预 transfer 响应。')
assert.ok(source.includes("getByRole('button', { name: '批记录' })"), '管理员接管前必须切换到批记录填写方式，避免默认打开记录本。')
assert.ok(processRouteTaskBlock.includes('openFillTaskFromBatchDetailTakeover'), '普通工序处理必须在无 OPEN_FORM 时走批次详情接管路径。')
assert.ok(processRouteTaskBlock.includes('openFillTaskFromBoard'), '普通工序处理仍需在当前账号可填写时复用工作台 task/open 结果。')
assert.ok(processRouteTaskBlock.includes('const opened = fillTaskUrl.openedTask'), '工作台进入填写页后不得再次调用批次详情打开任务。')
assert.ok(!processRouteTaskBlock.includes('const opened = await openTaskByUi(fillPage, pendingTask)'), '工作台处理已经进入填写页后禁止重复 openTaskByUi。')
assert.ok(processRouteTaskBlock.includes('const reworkOpened = reworkUrl.openedTask'), '返工处理也必须复用工作台 task/open 结果。')
assert.ok(!processRouteTaskBlock.includes('reworkUrl.pathname === ROUTES.executionDetail ? {} : await openTaskByUi'), '返工进入填写页后禁止以批次详情打开作为降级路径。')
assert.ok(source.includes('async function loadReleaseApprovalWorkspaceByUi'), '放行步骤必须封装 focus=approval 工作区加载并等待详情接口刷新。')
assert.ok(source.includes("`${ROUTES.batchDetail}?id=${batchId}&focus=${focus}`"), '放行步骤必须通过详情页 focus 参数进入正式放行工作区。')
assert.ok(source.includes('batchWorkbench'), '放行工作区刷新必须读取 /mes/pro/edhr-batch-execution/workbench 的 releaseSummary。')
assert.ok(source.includes('workbenchSignalPromise'), '放行工作区加载必须等待页面真实 workbench 请求完成。')
assert.ok(source.includes('apiGet(page, auth, ENDPOINTS.batchWorkbench'), '放行工作区必须通过同一登录会话只读读取 workbench 摘要。')
assert.ok(source.includes('async function waitForReleaseRailActionButton'), '放行按钮必须封装右侧放行工序参数动作区定位，避免命中左侧流程节点。')
assert.ok(source.includes('function buttonTextMatches'), '按钮等待必须枚举 innerText 后手动匹配，避免 Playwright hasText 精确正则漏判。')
assert.ok(!source.includes("root.locator('button').filter({ hasText: name })"), '按钮等待不得继续依赖 hasText:name 精确过滤。')
assert.ok(source.includes('.edhr-batch-detail__release-rail-actions[aria-label="放行工序参数"]'), '放行动作定位必须限定在右侧放行工序参数区域。')
assert.ok(source.includes('async function waitForReleasePrecheckActionButton'), '放行预检必须封装主预检工作区定位。')
assert.ok(source.includes('.edhr-batch-detail__release-precheck-workspace[aria-label="放行预检工作区"]'), '放行预检动作必须限定在主预检工作区，不能误找右侧放行动作区。')
assert.ok(closeBatchBlock.includes("loadReleaseApprovalWorkspaceByUi(page, batchId, '关闭前放行预检工作区', 'precheck')"), '放行前必须先进入正式预检工作区并读取当前详情。')
assert.ok(detailPageSource.includes("type EdhrBatchExecutionDetailFocus = 'process' | 'precheck' | 'approval'"), '批次详情页必须支持 focus=precheck，避免预检阶段被 focus=approval 标记为只读。')
assert.ok(detailPageSource.includes("viewedReleaseStageKey.value = focus === 'precheck' ? 'precheck' : 'release-approval'"), '批次详情页必须按 focus=precheck 选择当前预检阶段，而不是只读审批阶段。')
assert.ok(closeBatchBlock.includes('ENDPOINTS.releasePrecheck'), '放行步骤必须点击真实“预检”并等待 edhr-release/precheck 响应。')
assert.ok(closeBatchBlock.includes('ENDPOINTS.releaseSubmit'), '放行步骤必须点击真实“确认放行”并等待 edhr-release/submit 响应。')
assert.ok(closeBatchBlock.includes('const [precheck] = await Promise.all'), '放行预检必须把页面点击和接口监听绑定为同一原子动作，避免点击失败被异步监听错误掩盖。')
assert.ok(closeBatchBlock.includes("const currentReleaseStatus = detail.workbench?.releaseSummary?.releaseStatus"), '放行步骤必须读取当前 releaseSummary，识别已通过预检的续跑状态。')
assert.ok(closeBatchBlock.includes("if (currentReleaseStatus !== 'PRECHECK_PASSED')"), '已通过预检的批次不得重复点击只读预检按钮，必须直接进入放行。')
assert.ok(closeBatchBlock.includes('waitForReleasePrecheckActionButton(page)'), '放行预检必须点击主预检工作区内的“预检”动作，避免误找右侧放行动作区。')
assert.ok(closeBatchBlock.includes("loadReleaseApprovalWorkspaceByUi(page, batchId, '放行预检通过后批次详情')"), '放行预检通过后必须重新进入 focus=approval，等待页面摘要刷新后再启用放行按钮。')
assert.ok(closeBatchBlock.includes('refreshedDetail.workbench?.releaseSummary?.releaseStatus'), '放行预检后必须断言详情 releaseSummary 已刷新为 PRECHECK_PASSED。')
assert.ok(closeBatchBlock.includes('waitForReleaseRailActionButton(page, /^放\\s*行$/'), '电子签名前必须点击右侧放行工序参数区内的“放行”动作。')
assert.ok(closeBatchBlock.includes('const [released] = await Promise.all'), '电子签名放行必须把确认点击和 releaseSubmit 监听绑定为同一原子动作。')
assert.ok(!closeBatchBlock.includes("clickVisibleButton(page, '关闭批次'"), '放行步骤不得继续寻找已废弃的“关闭批次”按钮。')
assert.ok(archiveTaskBlock.includes('&focus=approval'), '归档待办进入批次详情后必须保持 focus=approval，确保右侧切到归档阶段动作区。')
assert.ok(archiveTaskBlock.includes("waitForReleaseRailActionButton(page, /^归\\s*档\\s*打\\s*印$/"), '归档待办入口必须等待右侧“归档打印”动作可用。')
assert.ok(generateArchiveBlock.includes("waitForReleaseRailActionButton(page, /^归\\s*档\\s*打\\s*印$/"), '生成归档前必须通过真实右侧“归档打印”按钮打开抽屉。')
assert.ok(generateArchiveBlock.includes(".el-drawer:visible')"), '生成归档必须限定在当前可见归档打印抽屉内。')
assert.ok(generateArchiveBlock.includes("waitForVisibleEnabledButton(drawer, /^生成归档$/"), '生成归档必须点击当前抽屉内真实“生成归档”按钮，不得寻找旧文案。')
assert.ok(generateArchiveBlock.includes('downloadAndPrintArchiveViaUi(page, drawer)'), '生成归档后的下载和打印必须继续限定在同一个归档抽屉内。')
assert.ok(!generateArchiveBlock.includes("'生成最终归档'"), '归档脚本不得继续依赖废弃按钮文案“生成最终归档”。')
assert.ok(downloadArchiveStart >= 0, '完整演练必须封装归档下载打印。')
assert.ok(downloadArchiveBlock.includes('async function downloadAndPrintArchiveViaUi(page, drawer)'), '归档下载打印必须接收当前可见归档抽屉作为作用域。')
assert.ok(downloadArchiveBlock.includes("drawer.getByRole('button', { name: /^下载打印版 PDF$/ })"), '下载打印版 PDF 必须限定在归档抽屉内，避免命中背景按钮。')
assert.ok(downloadArchiveBlock.includes("drawer.getByRole('button', { name: /^打印$/ })"), '打印按钮必须限定在归档抽屉内，避免命中背景“归档打印”按钮。')
assert.ok(detailPageSource.includes('@click="handlePrintArchive"'), '归档打印抽屉必须提供正式“打印”按钮。')
assert.ok(detailPageSource.includes('下载打印版 PDF') && detailPageSource.includes('打印'), '归档打印抽屉必须同时提供下载和打印动作。')
assert.ok(backendServiceSource.includes('manifest.put("releaseTransactionSnapshot", toArchiveReleaseTransactionManifest(releaseTransaction))'), '批次归档 manifest 必须持久化真实放行事务快照，不能只依赖前端时间线。')
assert.ok(backendServiceSource.includes('manifest.put("releaseEvents", buildArchiveReleaseEventManifests(releaseTransaction))'), '批次归档 manifest 必须持久化放行预检/提交/审批事件。')
assert.ok(backendServiceSource.includes('private Map<String, Object> toArchiveReleaseTransactionManifest'), '后端必须有放行事务归档快照构造函数。')
assert.ok(backendServiceSource.includes('private List<Map<String, Object>> buildArchiveReleaseEventManifests'), '后端必须有放行事件归档快照构造函数。')
assert.ok(backendArchivePdfRendererSource.includes('writeReleaseApprovalAppendix(canvas, manifest)'), '打印版 PDF 必须渲染放行审核/批准证据。')
for (const token of ['放行审核与批准', '放行状态', '审核人', '批准人', '审批意见', '放行事件']) {
  assert.ok(backendArchivePdfRendererSource.includes(token), `打印版 PDF 必须包含放行审批关键文本：${token}`)
}

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
