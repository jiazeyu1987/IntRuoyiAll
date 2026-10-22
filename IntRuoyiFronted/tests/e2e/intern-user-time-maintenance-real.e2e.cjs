const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const TASK_ID = '20260922-intern-user-lifecycle-time-audit-module'
const FRONTEND_URL = process.env.INTERN_USER_E2E_FRONTEND_URL || 'http://127.0.0.1:8061'
const TENANT_NAME = process.env.INTERN_USER_E2E_TENANT || '芋道源码'
const USERNAME = process.env.INTERN_USER_E2E_USERNAME || 'admin'
const PASSWORD = process.env.INTERN_USER_E2E_PASSWORD || ''
const BROWSER_CHANNEL = process.env.INTERN_USER_E2E_BROWSER_CHANNEL || 'chrome'
const DCC_DIRECTORY_NAME = process.env.INTERN_USER_E2E_DCC_DIRECTORY || '2.DHF'
const OUTPUT_DIR = path.resolve(__dirname, '../../../doc/tasks', TASK_ID, 'e2e-artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'intern-user-time-maintenance-result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'intern-user-time-maintenance-final.png')
const FAILURE_SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'intern-user-time-maintenance-failure.png')

if (!PASSWORD) {
  throw new Error('INTERN_USER_E2E_PASSWORD is required; do not store passwords in source.')
}

function nowText() {
  return new Date().toISOString().replace('T', ' ').slice(0, 19)
}

function timestampFor(secondsOffset) {
  const date = new Date(Date.now() + secondsOffset * 1000)
  const pad = (value) => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + ' ' + [
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join(':')
}

function sanitizeUrl(url) {
  return String(url || '').replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')
}

async function readJsonResponse(response) {
  const text = await response.text()
  try {
    return JSON.parse(text)
  } catch (error) {
    throw new Error(`Response is not JSON: HTTP ${response.status()} ${sanitizeUrl(response.url())} ${text.slice(0, 300)}`)
  }
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function waitForBusinessResponse(page, urlPart, method) {
  const response = await page.waitForResponse(
    (candidate) => candidate.url().includes(urlPart) && candidate.request().method() === method,
    { timeout: 60000 }
  )
  const payload = await readJsonResponse(response)
  assert.equal(response.ok(), true, `${urlPart} HTTP ${response.status()}`)
  assert.equal(isBusinessOk(payload), true, `${urlPart} business code ${payload && payload.code}: ${payload && payload.msg}`)
  return payload
}

async function selectLoginTenant(page) {
  const form = page.locator('form.login-form:visible').first()
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (!(await tenantInput.isVisible().catch(() => false))) {
    return
  }
  await tenantInput.click()
  await tenantInput.press('Control+A')
  await tenantInput.press('Backspace')
  await tenantInput.fill(TENANT_NAME)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT_NAME }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
  await selectLoginTenant(page)
  await page.locator('.login-form input[placeholder="请输入用户名"]:visible').first().fill(USERNAME)
  await page.locator('.login-form input[type="password"][placeholder="请输入密码"]:visible').first().fill(PASSWORD)
  const loginResponsePromise = waitForBusinessResponse(page, '/system/auth/login', 'POST')
  await page.locator('.login-form button[type="submit"]:visible, .login-form button:has-text("登录"):visible').first().click()
  await loginResponsePromise
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function gotoRoute(page, route, pageApiPart, options = {}) {
  const { waitForRows = true } = options
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes(pageApiPart) && response.request().method() === 'GET',
    { timeout: 90000 }
  ).catch(() => null)
  await page.goto(`${FRONTEND_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const response = await responsePromise
  if (response) {
    const payload = await readJsonResponse(response)
    assert.equal(response.ok(), true, `${pageApiPart} HTTP ${response.status()}`)
    assert.equal(isBusinessOk(payload), true, `${pageApiPart} business code ${payload && payload.code}: ${payload && payload.msg}`)
  }
  if (waitForRows) {
    await page.locator('.el-table__body-wrapper tbody tr:visible').first().waitFor({ state: 'visible', timeout: 90000 })
  }
}

async function clickButton(page, text) {
  const button = page.locator(`button:visible:has-text("${text}")`).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await button.scrollIntoViewIfNeeded()
  await button.click()
}

async function clickFirstTableRowButton(page, text) {
  const firstRow = page.locator('.el-table__body-wrapper tbody tr:visible').first()
  await firstRow.waitFor({ state: 'visible', timeout: 30000 })
  const button = firstRow.locator(`button:visible:has-text("${text}")`).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await button.scrollIntoViewIfNeeded()
  await button.click()
}

async function visibleDialog(page, title) {
  const dialog = page.locator('.el-overlay:visible .el-dialog:visible, .el-dialog:visible').filter({ hasText: title }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  return dialog
}

async function fillVisibleDateTime(page, dialogTitle, value) {
  const dialog = await visibleDialog(page, dialogTitle)
  const input = dialog.locator('.el-date-editor input:visible, input[placeholder*="时间"]:visible').first()
  await input.click()
  await input.press('Control+A')
  await input.press('Backspace')
  await input.fill(value)
  await input.evaluate((element) => {
    element.dispatchEvent(new Event('input', { bubbles: true }))
    element.dispatchEvent(new Event('change', { bubbles: true }))
    element.blur()
  })
}

async function submitDialog(page, dialogTitle, responsePart) {
  const dialog = await visibleDialog(page, dialogTitle)
  const saveButton = dialog.locator('button:visible:has-text("保存")').first()
  await saveButton.waitFor({ state: 'visible', timeout: 30000 })
  const responsePromise = waitForBusinessResponse(page, responsePart, 'PUT')
  await saveButton.click()
  await responsePromise
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function assertAuditDialog(page, clickText, title, responsePart, fieldName) {
  const responsePromise = waitForBusinessResponse(page, responsePart, 'GET')
  await clickFirstTableRowButton(page, clickText)
  const payload = await responsePromise
  const rows = Array.isArray(payload.data) ? payload.data : []
  assert.ok(rows.some((row) => row.fieldName === fieldName), `${title} must include field ${fieldName}`)
  const dialog = await visibleDialog(page, title)
  await dialog.getByText(fieldName, { exact: true }).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.keyboard.press('Escape')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => {})
}

async function runUploadTimeFlow(page, evidence) {
  await gotoRoute(page, '/infra/file/file', '/infra/file/page')
  await clickFirstTableRowButton(page, '修改时间')
  const uploadTime = timestampFor(60)
  await fillVisibleDateTime(page, '修改上传时间', uploadTime)
  await submitDialog(page, '修改上传时间', '/intern-user/time-maintenance/file/upload-time')
  await assertAuditDialog(
    page,
    '修改审计',
    '上传时间修改审计',
    '/intern-user/time-maintenance/file/upload-time/audits',
    'createTime'
  )
  evidence.steps.push({ name: '修改上传时间并查看上传时间修改审计', targetTime: uploadTime, completedAt: nowText() })
}

async function runDccTimeFlow(page, evidence) {
  await gotoRoute(page, '/dcc/controlled-file/browser', '/dcc/controlled-files/browser-page', { waitForRows: false })
  const directorySearch = page.locator('input[placeholder="搜索目录"]:visible').first()
  await directorySearch.waitFor({ state: 'visible', timeout: 30000 })
  await directorySearch.click()
  await directorySearch.press('Control+A')
  await directorySearch.press('Backspace')
  await directorySearch.fill(DCC_DIRECTORY_NAME)
  const directoryNode = page
    .locator('button.browser-directory-search__item:visible, .el-tree-node__content:visible')
    .filter({ hasText: DCC_DIRECTORY_NAME })
    .first()
  await directoryNode.waitFor({ state: 'visible', timeout: 30000 })
  const pageResponsePromise = waitForBusinessResponse(page, '/dcc/controlled-files/browser-page', 'GET')
  await directoryNode.click()
  await pageResponsePromise
  await page.locator('.el-table__body-wrapper tbody tr:visible').first().waitFor({ state: 'visible', timeout: 90000 })
  await clickFirstTableRowButton(page, '修改升版时间')
  const publishedTime = timestampFor(120)
  await fillVisibleDateTime(page, '修改升版时间', publishedTime)
  await submitDialog(page, '修改升版时间', '/intern-user/time-maintenance/dcc/published-time')
  await assertAuditDialog(
    page,
    '升版审计',
    '升版时间修改审计',
    '/intern-user/time-maintenance/dcc/published-time/audits',
    'publishedTime'
  )
  evidence.steps.push({ name: '修改升版时间并查看升版时间修改审计', targetTime: publishedTime, completedAt: nowText() })

  await clickFirstTableRowButton(page, '修改作废时间')
  const obsoletedTime = timestampFor(180)
  await fillVisibleDateTime(page, '修改作废时间', obsoletedTime)
  await submitDialog(page, '修改作废时间', '/intern-user/time-maintenance/dcc/obsoleted-time')
  await assertAuditDialog(
    page,
    '作废审计',
    '作废时间修改审计',
    '/intern-user/time-maintenance/dcc/obsoleted-time/audits',
    'obsoletedTime'
  )
  evidence.steps.push({ name: '修改作废时间并查看作废时间修改审计', targetTime: obsoletedTime, completedAt: nowText() })
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const evidence = {
    taskId: TASK_ID,
    frontendUrl: FRONTEND_URL,
    tenant: TENANT_NAME,
    username: USERNAME,
    startedAt: nowText(),
    steps: [],
    responses: [],
    consoleErrors: [],
    pageErrors: []
  }
  assert.equal(TENANT_NAME, '芋道源码', 'E2E must use 芋道源码 tenant')
  assert.equal(USERNAME, 'admin', 'E2E must use admin account')

  const browser = await chromium.launch({
    channel: BROWSER_CHANNEL,
    headless: process.env.HEADLESS !== 'false'
  })
  const context = await browser.newContext({ viewport: { width: 1680, height: 940 } })
  const page = await context.newPage()

  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('response', (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/')) return
    if (!/intern-user\/time-maintenance|infra\/file\/page|dcc\/controlled-files\/browser-page|system\/auth\/login/.test(url)) return
    evidence.responses.push({
      url: sanitizeUrl(url),
      method: response.request().method(),
      status: response.status()
    })
  })

  try {
    await login(page)
    await runUploadTimeFlow(page, evidence)
    await runDccTimeFlow(page, evidence)
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    evidence.status = 'PASS'
    evidence.screenshot = SCREENSHOT_PATH
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    try {
      await page.screenshot({ path: FAILURE_SCREENSHOT_PATH, fullPage: true })
      evidence.screenshot = FAILURE_SCREENSHOT_PATH
    } catch (_) {
      // Preserve the original failure.
    }
    throw error
  } finally {
    evidence.finishedAt = nowText()
    fs.writeFileSync(RESULT_PATH, JSON.stringify(evidence, null, 2), 'utf8')
    await browser.close().catch(() => {})
    console.log(JSON.stringify(evidence, null, 2))
  }
}

run().catch((error) => {
  console.error(error)
  process.exit(1)
})
