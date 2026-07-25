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
assert.ok(openFillTaskBlock.includes("waitForVisibleEnabledButton(page, '处理'"), '工作任务台处理按钮应从页面可见操作列定位，不能限定在主表格行。')
assert.ok(!openFillTaskBlock.includes("await clickVisibleButton(row, '处理'"), '工作任务台处理按钮不得从不含固定操作列的主表格行内点击。')

const loadBatchDetailStart = source.indexOf('async function loadBatchDetailByUi')
const loadBatchDetailEnd = source.indexOf('async function syncBatchByUi', loadBatchDetailStart)
const loadBatchDetailBlock = source.slice(loadBatchDetailStart, loadBatchDetailEnd)

assert.ok(loadBatchDetailBlock.includes('page.waitForResponse'), '批次详情加载必须观察真实页面详情接口请求。')
assert.ok(loadBatchDetailBlock.includes('apiGet(page, auth, ENDPOINTS.batchGet'), '批次详情结构化状态必须通过同一登录会话只读 API 获取，避免抢读导航响应体。')
assert.ok(!loadBatchDetailBlock.includes('const detail = await detailPromise'), '批次详情加载不得把导航响应体解析结果作为结构化状态。')

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
