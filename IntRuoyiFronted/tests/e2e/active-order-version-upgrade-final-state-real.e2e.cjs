const assert = require('assert')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const TASK_ID = '20260902-active-order-latest-version-upgrade-restart-code'
const FRONTEND_URL = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_FRONTEND_URL || 'http://127.0.0.1:8093'
const TENANT_NAME = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TENANT || '芋道源码'
const USERNAME = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_USERNAME || 'admin'
const PASSWORD = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_PASSWORD || ''
const SOURCE_ACTIVE_ORDER_ID = Number(process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_SOURCE_ACTIVE_ORDER_ID || '45')
const TARGET_ACTIVE_ORDER_ID = Number(process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TARGET_ACTIVE_ORDER_ID || '1009200001')
const WORK_ORDER_CODE = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_WORK_ORDER_CODE || 'CODX-PQC-20260807-SP-WO-05'
const EXPECTED_TARGET_ROUTE_VERSION_ID = Number(process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TARGET_ROUTE_VERSION_ID || '742')
const EXPECTED_TARGET_ROUTE_VERSION_NO = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TARGET_ROUTE_VERSION_NO || 'V12'
const EXPECTED_PROCESS_INSTANCE_ID =
  process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_PROCESS_INSTANCE_ID || '7f9ca694-a6da-11f1-a6b9-00155d07b6dd'

const OUTPUT_DIR = path.resolve(__dirname, '../../../doc/tasks', TASK_ID, 'e2e-artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'active-order-version-upgrade-final-state-real-result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'active-order-version-upgrade-final-state-real.png')

if (!PASSWORD) {
  throw new Error('ACTIVE_ORDER_VERSION_UPGRADE_E2E_PASSWORD is required; do not store passwords in source or logs.')
}

function sanitizeUrl(url) {
  return String(url || '').replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')
}

async function fillFirst(pageOrLocator, selectors, value) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(selector).first()
    if ((await locator.count()) === 0) continue
    try {
      await locator.waitFor({ state: 'visible', timeout: 8000 })
      await locator.fill(value)
      return selector
    } catch (_) {
      // Try the next stable selector.
    }
  }
  throw new Error(`No fillable locator found for selectors: ${selectors.join(', ')}`)
}

async function clickFirst(pageOrLocator, selectors, label) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(selector).first()
    if ((await locator.count()) === 0) continue
    try {
      await locator.waitFor({ state: 'visible', timeout: 10000 })
      await locator.click()
      return selector
    } catch (_) {
      // Try the next stable selector.
    }
  }
  throw new Error(`No clickable locator found for ${label}: ${selectors.join(', ')}`)
}

async function selectLoginTenant(page, tenantName) {
  const tenantInput = page.locator('.login-form .el-select input:visible').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 15000 })
  await tenantInput.click()
  await tenantInput.fill(tenantName)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: tenantName }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await selectLoginTenant(page, TENANT_NAME)
  await fillFirst(
    page,
    [
      '.login-form input[placeholder="请输入用户名"]:visible',
      '.login-form input[placeholder*="账号"]:visible',
      '.login-form input[name="username"]:visible'
    ],
    USERNAME
  )
  await fillFirst(
    page,
    [
      '.login-form input[placeholder="请输入密码"]:visible',
      '.login-form input[type="password"]:visible',
      '.login-form input[name="password"]:visible'
    ],
    PASSWORD
  )
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') && response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await clickFirst(page, ['.login-form button[type="submit"]:visible', 'button:has-text("登录"):visible'], '登录')
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  const permissionResponse = await permissionResponsePromise
  assert.equal(permissionResponse.ok(), true, `权限接口 HTTP 失败：${permissionResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

async function openActiveOrderPool(page) {
  const listResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/list') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  ).catch((error) => error)
  await page.goto(`${FRONTEND_URL}/mes/pro/process-pool/production-leader`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('[data-team-leader-report-workbench]').first().waitFor({ state: 'visible', timeout: 30000 })
  const activeOrderTab = page
    .locator('.el-tabs__item:visible, [role="tab"]:visible', { hasText: '活跃订单池' })
    .first()
  await activeOrderTab.waitFor({ state: 'visible', timeout: 15000 })
  await activeOrderTab.click()
  await page.locator('[data-team-leader-active-order-list]').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-team-leader-active-order-list] .el-table__body-wrapper tbody tr').first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  const listResponse = await listResponsePromise
  if (listResponse instanceof Error) {
    const visibleText = await page.locator('body').innerText({ timeout: 5000 })
    throw new Error(`活跃订单列表接口未在 30 秒内返回；当前页面文本片段：${visibleText.slice(0, 1000)}`)
  }
  assert.equal(listResponse.ok(), true, `活跃订单列表 HTTP 失败：${listResponse.status()}`)
}

async function findActiveOrderRowByVisibleId(page, activeOrderId) {
  const idCell = page.locator(`[data-team-leader-active-order-id="${activeOrderId}"]`).first()
  await idCell.waitFor({ state: 'visible', timeout: 30000 })
  return idCell.locator('xpath=ancestor::tr[1]')
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const evidence = {
    taskId: TASK_ID,
    verificationMode: 'FRONTEND_DOM_ONLY',
    frontendUrl: FRONTEND_URL,
    tenant: TENANT_NAME,
    username: USERNAME,
    sourceActiveOrderId: SOURCE_ACTIVE_ORDER_ID,
    targetActiveOrderId: TARGET_ACTIVE_ORDER_ID,
    workOrderCode: WORK_ORDER_CODE,
    expectedTargetRouteVersionId: EXPECTED_TARGET_ROUTE_VERSION_ID,
    expectedTargetRouteVersionNo: EXPECTED_TARGET_ROUTE_VERSION_NO,
    expectedProcessInstanceId: EXPECTED_PROCESS_INSTANCE_ID,
    requests: [],
    consoleErrors: [],
    pageErrors: []
  }
  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/active-order/list') && !url.includes('/active-order/detail')) return
    evidence.requests.push({
      url: sanitizeUrl(url),
      method: response.request().method(),
      status: response.status()
    })
  })

  try {
    await login(page)
    evidence.login = 'PASS'
    await openActiveOrderPool(page)
    const sourceIdCellCount = await page.locator(`[data-team-leader-active-order-id="${SOURCE_ACTIVE_ORDER_ID}"]`).count()
    assert.equal(sourceIdCellCount, 0, `旧活跃订单 ${SOURCE_ACTIVE_ORDER_ID} 不得继续出现在当前活跃订单池可见表格中`)

    const targetRow = await findActiveOrderRowByVisibleId(page, TARGET_ACTIVE_ORDER_ID)
    const targetRowText = await targetRow.innerText({ timeout: 10000 })
    assert.ok(targetRowText.includes(WORK_ORDER_CODE), `新活跃订单可见行必须包含生产订单号：${WORK_ORDER_CODE}`)
    assert.ok(
      targetRowText.includes(EXPECTED_TARGET_ROUTE_VERSION_NO),
      `新活跃订单可见行必须包含目标路线版本号：${EXPECTED_TARGET_ROUTE_VERSION_NO}`
    )
    assert.ok(targetRowText.includes('正式订单'), '新活跃订单可见行必须是正式订单')

    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/detail') &&
        response.request().method() === 'GET',
      { timeout: 30000 }
    )
    await targetRow.locator('[data-team-leader-active-order-detail]').first().click()
    const detailResponse = await detailResponsePromise
    assert.equal(detailResponse.ok(), true, `活跃订单详情 HTTP 失败：${detailResponse.status()}`)
    const detailDialog = page.locator('[data-team-leader-active-order-detail-dialog]:visible', {
      hasText: WORK_ORDER_CODE
    }).first()
    await detailDialog.waitFor({ state: 'visible', timeout: 30000 })
    const detailDialogText = await detailDialog.innerText({ timeout: 10000 })
    assert.ok(detailDialogText.includes('工序提交详情'), '详情弹窗必须为工序提交详情')
    assert.ok(detailDialogText.includes(WORK_ORDER_CODE), `详情弹窗必须显示生产订单号：${WORK_ORDER_CODE}`)

    evidence.replacementActiveOrder = {
      id: TARGET_ACTIVE_ORDER_ID,
      workOrderCode: WORK_ORDER_CODE,
      routeVersionId: EXPECTED_TARGET_ROUTE_VERSION_ID,
      routeVersionNo: EXPECTED_TARGET_ROUTE_VERSION_NO,
      activePoolVisible: true,
      activeStatus: 'verified by active-order pool visibility',
      businessStatus: 'verified by active-order pool visibility',
      rowText: targetRowText,
      detailDialogText
    }
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    evidence.screenshot = SCREENSHOT_PATH
    evidence.status = 'PASS'
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
    fs.writeFileSync(RESULT_PATH, JSON.stringify(evidence, null, 2), 'utf8')
    await browser.close()
  }
  console.log(`PASS: active-order version upgrade final-state real E2E -> ${RESULT_PATH}`)
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error))
  process.exit(1)
})
