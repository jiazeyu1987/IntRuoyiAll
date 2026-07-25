const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const scriptPath = path.join(repoRoot, 'tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js')
const source = fs.readFileSync(scriptPath, 'utf8')

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

const formCenterRouteTaskStart = source.indexOf('async function processRouteFormCenterTask')
const formCenterRouteTaskEnd = source.indexOf('async function processRouteTask', formCenterRouteTaskStart)
const formCenterRouteTaskBlock = source.slice(formCenterRouteTaskStart, formCenterRouteTaskEnd)

assert.ok(source.includes('function isFormCenterRouteTask'), '完整演练必须识别 FormCenter 动态表单/共享表单任务。')
assert.ok(formCenterRouteTaskStart >= 0, '完整演练必须通过真实批次详情抽屉处理 FormCenter 动态表单任务。')
assert.ok(formCenterRouteTaskBlock.includes('.form-action-panel'), 'FormCenter 动态表单必须等待真实表单面板。')
assert.ok(formCenterRouteTaskBlock.includes('/form-center/instances/'), 'FormCenter 动态表单必须等待真实草稿和提交接口。')
assert.ok(formCenterRouteTaskBlock.includes('保存草稿'), 'FormCenter 动态表单必须通过真实保存草稿按钮。')
assert.ok(formCenterRouteTaskBlock.includes('name: /^提交$/'), 'FormCenter 动态表单必须通过真实提交按钮。')

const processRouteTaskStart = source.indexOf('async function processRouteTask')
const processRouteTaskEnd = source.indexOf('async function closeBatch', processRouteTaskStart)
const processRouteTaskBlock = source.slice(processRouteTaskStart, processRouteTaskEnd)

assert.ok(processRouteTaskBlock.includes('const fillTaskUrl = await openFillTaskFromBoard'), '普通工序处理必须复用工作台 task/open 结果。')
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
