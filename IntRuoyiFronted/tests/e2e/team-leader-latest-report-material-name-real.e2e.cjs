const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const TASK_ID = '20260905-latest-report-material-name-regression'
const FRONTEND_URL = process.env.TEAM_LEADER_LATEST_REPORT_FRONTEND_URL || 'http://127.0.0.1:8081'
const TENANT_NAME = process.env.TEAM_LEADER_LATEST_REPORT_TENANT || '芋道源码'
const USERNAME = process.env.TEAM_LEADER_LATEST_REPORT_USERNAME || 'admin'
const PASSWORD = process.env.TEAM_LEADER_LATEST_REPORT_PASSWORD || ''
const TARGET_ROUTE = '/mes/pro/process-pool/production-leader'
const OUTPUT_DIR = path.resolve(__dirname, '../../../doc/tasks', TASK_ID, 'e2e-artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'latest-report-material-name-result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'latest-report-material-name-expanded.png')

if (!PASSWORD) {
  throw new Error('TEAM_LEADER_LATEST_REPORT_PASSWORD is required; do not store passwords in source.')
}

function sanitizeUrl(url) {
  return String(url || '').replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')
}

async function selectLoginTenant(page) {
  const tenantInput = page.locator('.login-form .el-select input:visible').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 15000 })
  await tenantInput.click()
  await tenantInput.press('Control+A')
  await tenantInput.press('Backspace')
  await tenantInput.fill(TENANT_NAME)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: TENANT_NAME }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.reload({ waitUntil: 'domcontentloaded' })
  await selectLoginTenant(page)
  await page.locator('.login-form input[placeholder="请输入用户名"]:visible').first().fill(USERNAME)
  await page.locator('.login-form input[type="password"][placeholder="请输入密码"]:visible').first().fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await page
    .locator('.login-form button[type="submit"]:visible, .login-form button:has-text("登录"):visible')
    .first()
    .click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

async function openProductionReportTab(page) {
  await page.goto(`${FRONTEND_URL}${TARGET_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const reportWorkbench = page.locator('[data-team-leader-report-workbench]').first()
  const reportTab = page.getByRole('tab', { name: '报工管理', exact: true }).first()
  try {
    await reportWorkbench.waitFor({ state: 'visible', timeout: 10000 })
    return reportWorkbench
  } catch (_) {
    await reportTab.waitFor({ state: 'visible', timeout: 60000 })
    await reportTab.click()
  }
  await reportWorkbench.waitFor({ state: 'visible', timeout: 60000 })
  return reportWorkbench
}

async function expandLatestReportRow(page, reportWorkbench) {
  await page.locator('.el-table__body-wrapper tbody tr').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  const firstDataRow = reportWorkbench.locator('.el-table__body-wrapper tbody tr').first()
  const expandControl = firstDataRow.locator('.el-table__expand-icon, td.el-table__cell').first()
  await expandControl.click()
  const detail = reportWorkbench.locator('[data-team-leader-submission-expand-detail]').first()
  await detail.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForTimeout(500)
  return detail
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const evidence = {
    taskId: TASK_ID,
    frontendUrl: FRONTEND_URL,
    tenant: TENANT_NAME,
    username: USERNAME,
    targetRoute: TARGET_ROUTE,
    requests: [],
    consoleErrors: [],
    pageErrors: []
  }
  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1680, height: 920 } })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('response', (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/mes/pro/process-pool/team-leader/submission/page')) return
    evidence.requests.push({
      url: sanitizeUrl(url),
      method: response.request().method(),
      status: response.status()
    })
  })

  try {
    await login(page)
    const reportWorkbench = await openProductionReportTab(page)
    const detail = await expandLatestReportRow(page, reportWorkbench)
    const text = await detail.innerText({ timeout: 10000 })
    const materialHeadTexts = await detail
      .locator('[data-team-leader-submission-material-card] .team-leader-workbench__submission-material-head strong')
      .evaluateAll((nodes) => nodes.map((node) => node.textContent?.trim()).filter(Boolean))
    assert.ok(materialHeadTexts.length > 0, '展开行必须显示至少一个物料明细标题。')
    assert.equal(text.includes('物料名称未记录'), false, '展开行不能再显示“物料名称未记录”。')
    assert.equal(
      materialHeadTexts.some((item) => /^物料\s*\d+$/.test(item)),
      false,
      `展开行不能用“物料 N”冒充真实物料名：${materialHeadTexts.join('、')}`
    )
    assert.equal(evidence.pageErrors.length, 0, `页面错误：${evidence.pageErrors.join('\n')}`)
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    evidence.status = 'PASS'
    evidence.materialHeadTexts = materialHeadTexts
    evidence.screenshot = SCREENSHOT_PATH
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    try {
      await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
      evidence.screenshot = SCREENSHOT_PATH
    } catch (_) {
      // Preserve the original failure.
    }
    throw error
  } finally {
    await browser.close().catch(() => {})
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
  }
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error))
  process.exit(1)
})
