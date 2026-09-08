const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const FRONTEND_URL = process.env.ACTIVE_ORDER_SUMMARY_TAB_E2E_FRONTEND_URL || 'http://127.0.0.1:8081'
const TENANT_NAME = process.env.ACTIVE_ORDER_SUMMARY_TAB_E2E_TENANT || '芋道源码'
const USERNAME = process.env.ACTIVE_ORDER_SUMMARY_TAB_E2E_USERNAME || 'admin'
const PASSWORD = process.env.ACTIVE_ORDER_SUMMARY_TAB_E2E_PASSWORD || ''
const WORK_ORDER_CODE =
  process.env.ACTIVE_ORDER_SUMMARY_TAB_E2E_WORK_ORDER_CODE ||
  'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'

if (!PASSWORD) {
  throw new Error('ACTIVE_ORDER_SUMMARY_TAB_E2E_PASSWORD is required.')
}

async function fillFirstVisible(locator, value, label) {
  const input = locator.first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill(value)
  return label
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input:visible').first()
  await tenantInput.click()
  await tenantInput.fill(TENANT_NAME)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: TENANT_NAME }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()

  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"]:visible, input[placeholder*="账号"]:visible, input[name="username"]:visible'),
    USERNAME,
    'username'
  )
  await fillFirstVisible(
    form.locator('input[type="password"]:visible, input[placeholder="请输入密码"]:visible, input[name="password"]:visible'),
    PASSWORD,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await form.locator('button[type="submit"]:visible, button:has-text("登录"):visible').first().click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function run() {
  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const consoleErrors = []
  const pageErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    await page.goto(`${FRONTEND_URL}/mes/pro/process-pool/production-leader`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const activeOrderTab = page.locator('.el-tabs__item:visible', { hasText: '活跃订单池' }).first()
    await activeOrderTab.waitFor({ state: 'visible', timeout: 30000 })
    await activeOrderTab.click()
    const rowCode = page.locator('[data-team-leader-active-order-work-order-code], .el-table__body .cell', {
      hasText: WORK_ORDER_CODE
    }).first()
    await rowCode.waitFor({ state: 'visible', timeout: 60000 })
    const row = rowCode.locator('xpath=ancestor::tr[1]')
    await row.locator('[data-team-leader-active-order-detail]').first().click()
    await page.waitForURL((url) => url.pathname.includes('/submission-detail'), { timeout: 30000 })
    const detailPage = page.locator('[data-team-leader-active-order-detail-page]', {
      hasText: WORK_ORDER_CODE
    }).first()
    await detailPage.waitFor({ state: 'visible', timeout: 30000 })

    const summaryTab = detailPage.getByRole('tab', { name: /^总表$/ }).first()
    await summaryTab.waitFor({ state: 'visible', timeout: 30000 })
    await summaryTab.click()
    await detailPage.locator('[data-active-order-summary-product-table]').waitFor({ state: 'visible', timeout: 30000 })
    await detailPage.locator('[data-active-order-summary-material-batches-table]').waitFor({ state: 'visible', timeout: 30000 })
    await detailPage.locator('[data-active-order-summary-process-personnel-table]').waitFor({ state: 'visible', timeout: 30000 })

    assert.equal(pageErrors.length, 0, `页面运行错误：${pageErrors.join('\n')}`)
    console.log('PASS: active order summary tab visible real E2E')
  } catch (error) {
    const bodyText = await page.locator('body').innerText().catch(() => '')
    console.error(`DIAG currentUrl=${page.url()}`)
    console.error(`DIAG body=${bodyText.replace(/\s+/g, ' ').slice(0, 2000)}`)
    if (pageErrors.length) console.error(`DIAG pageErrors=${pageErrors.join('\n')}`)
    if (consoleErrors.length) console.error(`DIAG consoleErrors=${consoleErrors.slice(0, 10).join('\n')}`)
    throw error
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error))
  process.exit(1)
})
