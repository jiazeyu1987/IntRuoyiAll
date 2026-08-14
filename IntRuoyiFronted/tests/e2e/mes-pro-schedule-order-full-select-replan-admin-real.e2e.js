const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const TASK_ROOT = path.resolve(
  FRONTEND_ROOT,
  '../doc/tasks/20260804-mes-partial-replan-blockers'
)
const ARTIFACT_DIR = path.join(TASK_ROOT, 'artifacts')
const BASE_URL = process.env.MES_FULL_SELECT_REPLAN_E2E_BASE_URL || 'http://127.0.0.1:8081'
const START_DATE = process.env.MES_FULL_SELECT_REPLAN_E2E_START_DATE || '2026-08-06'
const TENANT = process.env.MES_FULL_SELECT_REPLAN_E2E_TENANT || readEnvDefault('VITE_APP_DEFAULT_LOGIN_TENANT') || '芋道源码'
const USERNAME = process.env.MES_FULL_SELECT_REPLAN_E2E_USERNAME || readEnvDefault('VITE_APP_DEFAULT_LOGIN_USERNAME') || 'admin'
const PASSWORD = process.env.MES_FULL_SELECT_REPLAN_E2E_PASSWORD || readEnvDefault('VITE_APP_DEFAULT_LOGIN_PASSWORD')
const CHROME_PATH =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function readEnvDefault(key) {
  for (const envFileName of ['.env.local', '.env']) {
    const envPath = path.join(FRONTEND_ROOT, envFileName)
    if (!fs.existsSync(envPath)) {
      continue
    }
    const text = fs.readFileSync(envPath, 'utf8')
    const pattern = new RegExp(`^\\s*${key}\\s*=\\s*(.*)\\s*$`)
    for (const line of text.split(/\r?\n/)) {
      const match = line.match(pattern)
      if (match) {
        return match[1].trim().replace(/^['"]|['"]$/g, '')
      }
    }
  }
  return ''
}

async function settle(page, timeout = 30000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function clickFirstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.click()
      return item
    }
  }
  throw new Error(`missing visible ${label}`)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      return item
    }
  }
  throw new Error(`missing visible ${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible().catch(() => false))) {
    await tenantSelect.click()
    await page.locator('.login-form .el-select__input').first().fill(TENANT)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: TENANT })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), USERNAME, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), PASSWORD, 'password')
  await clickFirstVisible(page.locator('.login-form .el-button--primary'), 'login submit button')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function selectAllVisibleScheduleOrderRows(page) {
  const rowLocators = page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr')
  const selectedRows = []
  const seenTexts = new Set()
  const count = await rowLocators.count()
  for (let index = 0; index < count; index += 1) {
    const row = rowLocators.nth(index)
    if (!(await row.isVisible().catch(() => false))) {
      continue
    }
    const text = (await row.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
    if (!text || text.includes('暂无数据') || seenTexts.has(text)) {
      continue
    }
    const checkbox = row.locator('.el-checkbox').first()
    if (!(await checkbox.isVisible().catch(() => false))) {
      continue
    }
    const checkboxClass = (await checkbox.getAttribute('class').catch(() => '')) || ''
    const checkboxInputDisabled = await checkbox
      .locator('input')
      .first()
      .isDisabled()
      .catch(() => false)
    if (checkboxClass.includes('is-disabled') || checkboxInputDisabled) {
      continue
    }
    await checkbox.click()
    selectedRows.push(text)
    seenTexts.add(text)
    await page.waitForTimeout(80)
  }
  assert.ok(selectedRows.length > 0, 'must select at least one visible schedule order row')
  return selectedRows
}

async function collectProgressSnapshot(page) {
  const progressText = await page
    .locator('.schedule-order-pool__replan-progress')
    .filter({ hasText: '重排进度' })
    .last()
    .innerText({ timeout: 1000 })
    .catch(() => '')
  const confirmDialogVisible = await page
    .locator('.el-message-box:visible')
    .filter({ hasText: '存在未参与排产的工单' })
    .isVisible()
    .catch(() => false)
  const dateDialogVisible = await page
    .locator('.el-dialog:visible')
    .filter({ hasText: '开始重排日期' })
    .isVisible()
    .catch(() => false)
  const skippedNoticeVisible = await page
    .locator('.el-notification:visible')
    .filter({ hasText: '存在未参与排产的工单' })
    .isVisible()
    .catch(() => false)
  return {
    progressText,
    confirmDialogVisible,
    dateDialogVisible,
    skippedNoticeVisible
  }
}

function sanitizePostData(postData) {
  if (!postData) {
    return null
  }
  try {
    const parsed = JSON.parse(postData)
    if (parsed.calendarContextToken) {
      parsed.calendarContextToken = '<redacted>'
    }
    if (parsed.idempotencyKey) {
      parsed.idempotencyKey = '<redacted>'
    }
    return parsed
  } catch {
    return '<unparsed>'
  }
}

function summarizeTargetPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return { rawType: typeof payload }
  }
  const data = payload.data || {}
  return {
    code: payload.code,
    msg: payload.msg,
    result: data.result,
    summary: data.summary,
    issueCount: Array.isArray(data.issues) ? data.issues.length : undefined,
    calendarContextTokenPresent: Boolean(data.calendarContextToken)
  }
}

async function run() {
  assert.ok(PASSWORD, 'default local login password must be available from env or .env')
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
  const resultPath = path.join(
    ARTIFACT_DIR,
    `full-select-replan-admin-e2e-${timestamp}.json`
  )
  const screenshotPath = path.join(
    ARTIFACT_DIR,
    `full-select-replan-admin-e2e-${timestamp}.png`
  )
  const browser = await chromium.launch({
    headless: process.env.MES_FULL_SELECT_REPLAN_E2E_HEADED !== '1',
    executablePath: fs.existsSync(CHROME_PATH) ? CHROME_PATH : undefined
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  await context.clearCookies()
  const page = await context.newPage()
  const pageErrors = []
  const consoleErrors = []
  const targetRequests = []
  const targetResponses = []
  const result = {
    status: 'UNKNOWN',
    baseUrl: BASE_URL,
    tenantUser: `${TENANT}/${USERNAME}`,
    startDate: START_DATE,
    selectedRows: [],
    targetRequests,
    targetResponses,
    pageErrors,
    consoleErrors,
    progressSnapshots: []
  }

  page.on('pageerror', (error) => pageErrors.push(String(error)))
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('request', (request) => {
    const url = request.url()
    if (
      url.includes('/admin-api/mes/pro/schedule-order/preflight') ||
      url.includes('/admin-api/mes/pro/auto-schedule/replan/preview') ||
      url.includes('/admin-api/mes/pro/auto-schedule/replan/apply')
    ) {
      targetRequests.push({
        method: request.method(),
        url,
        postData: sanitizePostData(request.postData())
      })
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (
      !url.includes('/admin-api/mes/pro/schedule-order/preflight') &&
      !url.includes('/admin-api/mes/pro/auto-schedule/replan/preview') &&
      !url.includes('/admin-api/mes/pro/auto-schedule/replan/apply')
    ) {
      return
    }
    let payload = null
    try {
      payload = await response.json()
    } catch {
      payload = await response.text().catch(() => '')
    }
    targetResponses.push({
      url,
      status: response.status(),
      payload: summarizeTargetPayload(payload)
    })
  })

  try {
    await login(page)
    await page.goto(`${BASE_URL}/mes/pro/schedule-order`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)

    result.selectedRows = await selectAllVisibleScheduleOrderRows(page)
    await clickFirstVisible(page.getByRole('button', { name: /手动重排/ }), 'manual replan button')
    const drawer = page.locator('.el-drawer:visible').filter({ hasText: '排产前检查 / 手动重排' }).first()
    await drawer.waitFor({ state: 'visible', timeout: 60000 })

    const startButton = drawer.getByRole('button', { name: /开始重排/ }).first()
    await startButton.waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(await startButton.isDisabled(), false, 'start replan button must be enabled after selecting visible rows')
    await startButton.click()

    const dateDialog = page.locator('.el-dialog:visible').filter({ hasText: '开始重排日期' }).last()
    await dateDialog.waitFor({ state: 'visible', timeout: 60000 })
    const dateInput = dateDialog.locator('input').first()
    await dateInput.fill(START_DATE)
    await page.keyboard.press('Escape')
    await page.locator('.el-picker-panel:visible').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined)

    const applyPromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/auto-schedule/replan/apply'),
      { timeout: 240000 }
    )
    await clickFirstVisible(dateDialog.getByRole('button', { name: /确认应用重排/ }), 'confirm apply replan button')

    for (let index = 0; index < 5; index += 1) {
      await page.waitForTimeout(1000)
      result.progressSnapshots.push(await collectProgressSnapshot(page))
    }
    assert.equal(
      result.progressSnapshots.some((snapshot) => snapshot.confirmDialogVisible),
      false,
      'skipped selected rows must not open a second blocking confirmation dialog'
    )

    const applyResponse = await applyPromise
    const applyPayload = await applyResponse.json()
    result.applyStatus = applyResponse.status()
    assert.equal(applyResponse.status(), 200, 'apply HTTP status must be 200')
    assert.equal(applyPayload.code, 0, `apply business response must succeed: ${applyPayload.msg || ''}`)
    result.applyPayload = summarizeTargetPayload(applyPayload)

    await page.waitForFunction(
      () => !document.body.innerText.includes('重排进度 90%'),
      { timeout: 60000 }
    )
    await dateDialog.waitFor({ state: 'hidden', timeout: 60000 })
    await drawer.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
    result.progressSnapshots.push(await collectProgressSnapshot(page))
    const finalSnapshot = result.progressSnapshots[result.progressSnapshots.length - 1]
    assert.equal(finalSnapshot.dateDialogVisible, false, 'date confirmation dialog must close after apply succeeds')
    assert.equal(finalSnapshot.confirmDialogVisible, false, 'skipped-row confirmation dialog must remain absent after apply succeeds')
    assert.notEqual(finalSnapshot.progressText, '重排进度 90%', 'replan progress must not remain at 90% after apply succeeds')
    const successMessageVisible = await page
      .locator('.el-message:visible')
      .filter({ hasText: '应用重排成功' })
      .isVisible()
      .catch(() => false)
    result.successMessageVisible = successMessageVisible
    result.status = 'PASS'
    await page.screenshot({ path: screenshotPath, fullPage: true })
  } catch (error) {
    result.status = 'FAIL'
    result.error = error.stack || error.message
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => undefined)
    throw error
  } finally {
    result.screenshotPath = screenshotPath
    fs.writeFileSync(resultPath, JSON.stringify(result, null, 2), 'utf8')
    console.log(JSON.stringify({
      status: result.status,
      tenantUser: result.tenantUser,
      selectedRowCount: result.selectedRows.length,
      applyObserved: targetRequests.some((item) => item.url.includes('/replan/apply')),
      applyCode: result.applyPayload?.code,
      progressSnapshots: result.progressSnapshots,
      resultPath,
      screenshotPath
    }, null, 2))
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error.stack || error.message)
  process.exit(1)
})
