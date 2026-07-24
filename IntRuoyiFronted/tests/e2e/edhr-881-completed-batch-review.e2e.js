const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-881-completed-batch-review')
const BASE_URL = process.env.EDHR_881_E2E_BASE_URL || 'http://localhost:8081'
const CONFIG = {
  tenant: process.env.EDHR_881_E2E_TENANT || '测试租户',
  username: process.env.EDHR_881_E2E_USERNAME || 'aoteman',
  password: process.env.EDHR_881_E2E_PASSWORD,
  batchExecutionId: Number(process.env.EDHR_881_E2E_BATCH_EXECUTION_ID || 9),
  batchCode: process.env.EDHR_881_E2E_BATCH_CODE || 'PC-E2E-20260610-0210',
  workOrderCode: process.env.EDHR_881_E2E_WORK_ORDER_CODE || '881MO090863',
  routeCode: process.env.EDHR_881_E2E_ROUTE_CODE || 'ROUTE-YXN.069.001.1001',
  headed: process.env.EDHR_881_E2E_HEADED === '1'
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function requireConfig() {
  const missing = []
  if (BASE_URL !== 'http://localhost:8081') missing.push('EDHR_881_E2E_BASE_URL must be http://localhost:8081')
  if (CONFIG.tenant !== '测试租户') missing.push('EDHR_881_E2E_TENANT must be 测试租户')
  if (!CONFIG.password) missing.push('EDHR_881_E2E_PASSWORD is required')
  if (!Number.isFinite(CONFIG.batchExecutionId) || CONFIG.batchExecutionId <= 0) {
    missing.push('EDHR_881_E2E_BATCH_EXECUTION_ID must be a positive number')
  }
  if (missing.length > 0) {
    throw new Error(`Missing prerequisites:\n${missing.map((item) => `- ${item}`).join('\n')}`)
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`Missing visible input: ${label}`)
}

async function clickVisibleButton(root, name) {
  const button = root.getByRole('button', { name }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  if (await button.isDisabled()) throw new Error(`Button is disabled: ${name}`)
  await button.click()
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/mes/pro/feedback/edhr-batch-execution`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('Captcha is enabled; unattended real E2E cannot continue.')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(CONFIG.tenant)
    await page.keyboard.press('Enter')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), CONFIG.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), CONFIG.password, 'password')
  await clickVisibleButton(loginForm, /^登录$/)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function unwrapResponse(body, label) {
  assert.equal(body.code, 0, `${label} must return business code 0: ${body.msg || body.code}`)
  return body.data
}

async function verifyDetail(page) {
  const detailResponse = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/mes/pro/edhr-batch-execution/get?id=${CONFIG.batchExecutionId}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${CONFIG.batchExecutionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const detail = unwrapResponse(await (await detailResponse).json(), 'batch detail')
  assert.equal(detail.batchCode, CONFIG.batchCode)
  assert.equal(detail.workOrderCode, CONFIG.workOrderCode)
  assert.equal(detail.routeCode, CONFIG.routeCode)
  assert.equal(detail.status, 40)
  assert.equal(detail.taskTotal, 21)
  assert.equal(detail.taskApprovedCount, 15)
  assert.equal(detail.blockedCount, 0)

  const tasks = detail.tasks || []
  assert.equal(tasks.length, 21)
  assert.equal(tasks.filter((task) => task.requiredFlag !== false && task.batchRecordReportId).length, 15)
  assert.equal(tasks.filter((task) => task.requiredFlag === false || !task.batchRecordReportId).length, 6)
  assert.equal(tasks.filter((task) => task.status === 40).length, 15)
  assert.equal(tasks.filter((task) => task.executionId).length, 15)

  await page.getByText(CONFIG.batchCode).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(detail.routeName || CONFIG.routeCode).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.screenshot({ path: path.join(RESULT_DIR, 'batch-detail.png'), fullPage: true })
  return detail
}

async function verifyReview(page) {
  const timelineResponse = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/mes/pro/edhr-batch-execution/review-timeline?id=${CONFIG.batchExecutionId}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/review?id=${CONFIG.batchExecutionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const timeline = unwrapResponse(await (await timelineResponse).json(), 'review timeline')
  const signatureRecords = timeline.signatureRecords || []
  const taskEvents = timeline.taskEvents || []
  const archiveVersions = timeline.archiveVersions || []
  assert(signatureRecords.some((item) => item.actionType === 'BATCH_CLOSE'), 'review page must contain batch close signature')
  assert.equal(taskEvents.filter((item) => item.status === 40 && item.approvedAt).length, 15)
  assert(
    archiveVersions.some((archive) => archive.archiveStatus === 'SEALED' && archive.fileName === `${CONFIG.batchCode}-edhr-final.pdf`),
    'review page must contain sealed final archive'
  )
  await page.getByText('eDHR 批次复盘').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(`${CONFIG.batchCode}-edhr-final.pdf`).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.screenshot({ path: path.join(RESULT_DIR, 'batch-review.png'), fullPage: true })
  return timeline
}

async function verifyDownloadAndPrint(page) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${CONFIG.batchExecutionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText(CONFIG.batchCode).first().waitFor({ state: 'visible', timeout: 60000 })

  const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
  await clickVisibleButton(page, '下载打印版 PDF')
  const download = await downloadPromise
  const downloadPath = path.join(RESULT_DIR, download.suggestedFilename())
  await download.saveAs(downloadPath)
  assert(fs.existsSync(downloadPath), 'downloaded final PDF must exist')
  assert(fs.statSync(downloadPath).size > 0, 'downloaded final PDF must be non-empty')

  const popupPromise = page.waitForEvent('popup', { timeout: 60000 })
  await clickVisibleButton(page, '打印')
  const popup = await popupPromise
  await popup.waitForLoadState('domcontentloaded', { timeout: 60000 }).catch(() => undefined)
  assert.equal(popup.isClosed(), false, 'print action must open a browser popup')
  const printPopupUrl = popup.url()
  await popup.close()

  return { downloadPath, suggestedFilename: download.suggestedFilename(), printPopupUrl }
}

async function main() {
  requireConfig()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !CONFIG.headed })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const page = await context.newPage()
  const result = { config: { ...CONFIG, password: '<redacted>' } }
  try {
    await login(page)
    result.detail = await verifyDetail(page)
    result.timeline = await verifyReview(page)
    result.archive = await verifyDownloadAndPrint(page)
    fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log('PASS: EDHR 881 completed batch review verified through real frontend')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
