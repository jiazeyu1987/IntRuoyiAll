const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const TASK_ID = '20260904-active-order-submission-overview'
const FRONTEND_URL = process.env.ACTIVE_ORDER_SUBMISSION_OVERVIEW_E2E_FRONTEND_URL || 'http://127.0.0.1:8095'
const TENANT_NAME = process.env.ACTIVE_ORDER_SUBMISSION_OVERVIEW_E2E_TENANT || '芋道源码'
const USERNAME = process.env.ACTIVE_ORDER_SUBMISSION_OVERVIEW_E2E_USERNAME || 'admin'
const PASSWORD = process.env.ACTIVE_ORDER_SUBMISSION_OVERVIEW_E2E_PASSWORD || ''
const ACTIVE_ORDER_ID = process.env.ACTIVE_ORDER_SUBMISSION_OVERVIEW_E2E_ACTIVE_ORDER_ID || '396'
const WORK_ORDER_CODE =
  process.env.ACTIVE_ORDER_SUBMISSION_OVERVIEW_E2E_WORK_ORDER_CODE ||
  'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'

const REQUIRED_TEXTS = [
  '生产提交',
  'PQC提交',
  '领料单',
  '提交数量',
  '提交人',
  '审核人',
  '提交时间',
  'ZJJH250906',
  'BH251002',
  '25308A0',
  'CODXLOT-BAF13994A736',
  'SIM-SOUT-C58EA189A6E4-04'
]
const FORBIDDEN_TEXTS = [
  '2087829649074102000',
  '2087829649074102069',
  '2087829649074102070',
  '2087829649074102071',
  '2087829649074102072'
]

const OUTPUT_DIR = path.resolve(__dirname, '../../../doc/tasks', TASK_ID, 'e2e-artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'active-order-submission-overview-real-result.json')
const FINAL_SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'active-order-submission-overview-real-final.png')
const MATERIAL_SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'active-order-submission-overview-real-materials.png')

if (!PASSWORD) {
  throw new Error(
    'ACTIVE_ORDER_SUBMISSION_OVERVIEW_E2E_PASSWORD is required; do not store passwords in source or logs.'
  )
}

function sanitizeUrl(url) {
  return String(url || '').replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')
}

async function fillVisible(locator, value, label) {
  const candidate = locator.first()
  await candidate.waitFor({ state: 'visible', timeout: 15000 })
  await candidate.fill(value)
  return label
}

async function selectLoginTenant(page) {
  const tenantInput = page.locator('.login-form .el-select input:visible').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 15000 })
  await tenantInput.click()
  await tenantInput.fill(TENANT_NAME)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: TENANT_NAME }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await selectLoginTenant(page)
  await fillVisible(page.locator('.login-form input[placeholder="请输入用户名"]:visible'), USERNAME, 'username')
  await fillVisible(
    page.locator('.login-form input[type="password"][placeholder="请输入密码"]:visible'),
    PASSWORD,
    'password'
  )
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await page.locator('.login-form button[type="submit"]:visible, .login-form button:has-text("登录"):visible').first().click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

async function openTargetActiveOrderDetail(page) {
  await page.goto(`${FRONTEND_URL}/mes/pro/process-pool/team-leader`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const root = page.locator('[data-team-leader-active-order-config]').first()
  await root.waitFor({ state: 'visible', timeout: 60000 })
  const codeCell = root.locator('[data-team-leader-active-order-work-order-code]', { hasText: WORK_ORDER_CODE }).first()
  await codeCell.waitFor({ state: 'visible', timeout: 60000 })
  const row = codeCell.locator('xpath=ancestor::tr[1]')
  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/detail') &&
      response.url().includes(`activeOrderId=${ACTIVE_ORDER_ID}`) &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  const detailPagePromise = page.waitForURL((url) => url.pathname.includes('/submission-detail'), {
    timeout: 30000
  })
  await row.locator('[data-team-leader-active-order-detail]').first().click()
  await detailPagePromise
  const detailResponse = await detailResponsePromise
  assert.equal(detailResponse.ok(), true, `活跃订单详情 HTTP 失败：${detailResponse.status()}`)
  const detailPage = page.locator('[data-team-leader-active-order-detail-page]', { hasText: WORK_ORDER_CODE }).first()
  await detailPage.waitFor({ state: 'visible', timeout: 30000 })
  return detailPage
}

async function collectDialogTextByScrolling(page, dialog) {
  const productionProcessTabs = dialog.locator('[data-team-leader-active-order-detail-production-process-tab]')
  const productionProcessTabCount = await productionProcessTabs.count()
  assert.equal(productionProcessTabCount, 15, `生产提交必须展示 15 个生产工序 Tab，实际 ${productionProcessTabCount}`)
  await dialog.getByRole('tab', { name: /领料单/ }).waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByRole('tab', { name: /生产提交/ }).waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByRole('tab', { name: /PQC提交/ }).waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByRole('tab', { name: /生产提交/ }).click()
  await dialog.getByRole('tab', { name: /1\.\s*粗洗工序/ }).click()

  let collected = `\n---FIRST-PROCESS---\n${await dialog.innerText({ timeout: 10000 })}`
  await dialog.getByRole('tab', { name: /PQC提交/ }).click()
  await dialog.locator('[data-team-leader-active-order-detail-pqc-process-tab]').first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  collected += `\n---FIRST-PROCESS-PQC---\n${await dialog.innerText({ timeout: 10000 })}`
  await dialog.getByRole('tab', { name: /领料单/ }).click()
  await dialog.getByText('SIM-SOUT-C58EA189A6E4-04', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  collected += `\n---PICK-LIST---\n${await dialog.innerText({ timeout: 10000 })}`
  await page.screenshot({ path: MATERIAL_SCREENSHOT_PATH, fullPage: true })
  return collected
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const evidence = {
    taskId: TASK_ID,
    verificationMode: 'FRONTEND_ONLY_DOM_ASSERTIONS',
    frontendUrl: FRONTEND_URL,
    tenant: TENANT_NAME,
    username: USERNAME,
    activeOrderId: ACTIVE_ORDER_ID,
    workOrderCode: WORK_ORDER_CODE,
    requests: [],
    consoleErrors: [],
    pageErrors: []
  }
  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 950 } })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('response', (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/mes/pro/process-pool/team-leader/active-order')) return
    evidence.requests.push({
      url: sanitizeUrl(url),
      method: response.request().method(),
      status: response.status()
    })
  })

  try {
    await login(page)
    const dialog = await openTargetActiveOrderDetail(page)
    const collectedText = await collectDialogTextByScrolling(page, dialog)
    const missing = REQUIRED_TEXTS.filter((text) => !collectedText.includes(text))
    const forbidden = FORBIDDEN_TEXTS.filter((text) => collectedText.includes(text))
    assert.deepEqual(missing, [], `详情页面缺少预期文本：${missing.join(', ')}`)
    assert.deepEqual(forbidden, [], `详情页面不应展示长整型领料单 ID：${forbidden.join(', ')}`)
    assert.equal(evidence.pageErrors.length, 0, `页面错误：${evidence.pageErrors.join('\n')}`)
    await page.screenshot({ path: FINAL_SCREENSHOT_PATH, fullPage: true })
    evidence.hasNoPqcText = collectedText.includes('暂无一线PQC提交')
    evidence.productionProcessTabCount = await dialog.locator('[data-team-leader-active-order-detail-production-process-tab]').count()
    evidence.pqcProcessTabCount = await dialog.locator('[data-team-leader-active-order-detail-pqc-process-tab]').count()
    evidence.totalTopLevelTabs = 3
    evidence.requiredTexts = REQUIRED_TEXTS
    evidence.forbiddenTexts = FORBIDDEN_TEXTS
    evidence.screenshots = [FINAL_SCREENSHOT_PATH, MATERIAL_SCREENSHOT_PATH]
    evidence.status = 'PASS'
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    try {
      await page.screenshot({ path: FINAL_SCREENSHOT_PATH, fullPage: true })
      evidence.screenshots = [FINAL_SCREENSHOT_PATH]
    } catch (_) {
      // Preserve original failure.
    }
    throw error
  } finally {
    await browser.close().catch(() => {})
    fs.writeFileSync(RESULT_PATH, JSON.stringify(evidence, null, 2))
  }
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error))
  process.exit(1)
})
