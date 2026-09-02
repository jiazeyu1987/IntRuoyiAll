const assert = require('assert')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const TASK_ID = '20260902-active-order-latest-version-upgrade-restart-code'
const FRONTEND_URL = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_FRONTEND_URL || 'http://127.0.0.1:8093'
const TENANT_NAME = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TENANT || '芋道源码'
const USERNAME = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_USERNAME || 'admin'
const PASSWORD = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_PASSWORD || ''
const WORK_ORDER_CODE =
  process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_WORK_ORDER_CODE || 'CODX-AOUP-20260902205106'
const ACTIVE_ORDER_ID = Number(process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_ACTIVE_ORDER_ID || '1009200000')
const OUTPUT_DIR = path.resolve(__dirname, '../../../doc/tasks', TASK_ID, 'e2e-artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'active-order-version-upgrade-submit-real-result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'active-order-version-upgrade-submit-real.png')

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
      await locator.waitFor({ state: 'visible', timeout: 5000 })
      await locator.fill(value)
      return selector
    } catch (_) {
      // Try next stable selector.
    }
  }
  throw new Error(`No fillable locator found for selectors: ${selectors.join(', ')}`)
}

async function clickFirst(pageOrLocator, selectors, label) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(selector).first()
    if ((await locator.count()) === 0) continue
    try {
      await locator.waitFor({ state: 'visible', timeout: 8000 })
      await locator.click()
      return selector
    } catch (_) {
      // Try next stable selector.
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
      '.login-form input[placeholder="请输入用户名"]',
      '.login-form input[placeholder*="账号"]',
      '.login-form input[name="username"]'
    ],
    USERNAME
  )
  await fillFirst(
    page,
    [
      '.login-form input[type="password"]',
      '.login-form input[placeholder="请输入密码"]',
      '.login-form input[name="password"]'
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
  await clickFirst(page, ['.login-form button[type="submit"]', 'button:has-text("登录")'], '登录')
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  const body = await loginResponse.json()
  assert.equal(Number(body.code), 0, `登录接口业务失败：${body.msg || body.message || 'unknown'}`)
  await permissionResponsePromise
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

async function openActiveOrderPool(page) {
  await page.goto(`${FRONTEND_URL}/mes/pro/process-pool/production-leader`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('[data-team-leader-report-workbench]').first().waitFor({ state: 'visible', timeout: 30000 })
  const listResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/list') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  const activeOrderTab = page
    .locator('.el-tabs__item:visible, [role="tab"]:visible', { hasText: '活跃订单池' })
    .first()
  try {
    await activeOrderTab.waitFor({ state: 'visible', timeout: 15000 })
  } catch (error) {
    const visibleText = await page.locator('body').innerText({ timeout: 5000 })
    throw new Error(`活跃订单池页签不可见；当前页面文本片段：${visibleText.slice(0, 1000)}`)
  }
  await activeOrderTab.click()
  const listResponse = await listResponsePromise
  assert.equal(listResponse.ok(), true, `活跃订单列表 HTTP 失败：${listResponse.status()}`)
  const body = await listResponse.json()
  assert.equal(Number(body.code), 0, `活跃订单列表业务失败：${body.msg || body.message || 'unknown'}`)
  await page.locator('[data-team-leader-active-order-list]').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const evidence = {
    taskId: TASK_ID,
    frontendUrl: FRONTEND_URL,
    tenant: TENANT_NAME,
    username: USERNAME,
    workOrderCode: WORK_ORDER_CODE,
    activeOrderId: ACTIVE_ORDER_ID,
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
    if (!url.includes('/active-order/version-upgrade/')) return
    let body = null
    try {
      body = await response.json()
    } catch (_) {
      body = null
    }
    evidence.requests.push({
      url: sanitizeUrl(url),
      method: response.request().method(),
      status: response.status(),
      businessCode: body && body.code,
      businessMessage: body && (body.msg || body.message),
      data: body && body.data
    })
  })

  try {
    await login(page)
    evidence.login = 'PASS'
    await openActiveOrderPool(page)
    const row = page.locator('[data-team-leader-active-order-list] .el-table__body-wrapper tbody tr', {
      hasText: WORK_ORDER_CODE
    }).first()
    await row.waitFor({ state: 'visible', timeout: 30000 })
    await row.locator('[data-team-leader-active-order-version-upgrade]').first().click()

    const dialog = page.locator('.el-dialog:visible', { hasText: '版本升级重启' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText('本流程将按全部最新正式版本提交审批', { exact: false }).waitFor({
      state: 'visible',
      timeout: 15000
    })
    const previewRequest = evidence.requests.find((request) =>
      request.url.includes('/active-order/version-upgrade/preview')
    )
    assert.ok(previewRequest?.data, '升级预览接口必须返回机器可读预览数据')
    assert.equal(
      previewRequest.data.perVersionSelectionAllowed,
      false,
      '升级预览必须禁止逐项版本选择'
    )
    if (!previewRequest.data.submittable) {
      throw new Error(`目标活跃订单不可提交升级：${(previewRequest.data.blockers || []).join('；') || '无变更项'}`)
    }
    const changedVersionLines = (previewRequest.data.targetVersions || []).filter((line) => line.changed)
    assert.ok(changedVersionLines.length > 0, '可提交升级必须至少存在一个受控对象版本差异')
    for (const line of changedVersionLines) {
      await dialog.getByText(String(line.currentVersionNo), { exact: false }).first().waitFor({
        state: 'visible',
        timeout: 15000
      })
      await dialog.getByText(String(line.targetVersionNo), { exact: false }).first().waitFor({
        state: 'visible',
        timeout: 15000
      })
    }
    await fillFirst(dialog, ['textarea[placeholder*="为什么需要"]', 'textarea'], `真实E2E验证升级重启 ${Date.now()}`)
    await dialog.locator('.el-checkbox').first().click()

    const submitResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/version-upgrade/submit') &&
        response.request().method() === 'POST',
      { timeout: 30000 }
    )
    await dialog.locator('button', { hasText: '提交升级审批' }).first().click()
    const submitResponse = await submitResponsePromise
    assert.equal(submitResponse.ok(), true, `提交升级审批 HTTP 失败：${submitResponse.status()}`)
    const submitBody = await submitResponse.json()
    assert.equal(Number(submitBody.code), 0, `提交升级审批业务失败：${submitBody.msg || submitBody.message || 'unknown'}`)
    assert.equal(submitBody.data.activeOrderId, ACTIVE_ORDER_ID, '提交结果必须返回当前任务自有活跃订单')
    assert.equal(submitBody.data.approvalStatus, 'PENDING', '提交后审批状态必须为 PENDING')
    assert.equal(submitBody.data.freezeStatus, 'OLD_ORDER_FROZEN', '提交后旧订单必须被冻结')
    evidence.submit = {
      requestCode: submitBody.data.requestCode,
      approvalStatus: submitBody.data.approvalStatus,
      freezeStatus: submitBody.data.freezeStatus
    }
    await page.locator('.el-message', { hasText: '版本升级审批已提交' }).first().waitFor({
      state: 'visible',
      timeout: 15000
    })
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
      // Ignore screenshot failures; preserve original failure.
    }
    throw error
  } finally {
    fs.writeFileSync(RESULT_PATH, JSON.stringify(evidence, null, 2), 'utf8')
    await browser.close()
  }
  console.log(`PASS: active-order version upgrade submit real E2E -> ${RESULT_PATH}`)
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error))
  process.exit(1)
})
