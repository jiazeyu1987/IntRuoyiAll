const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.SRM_NAS_LOCATOR_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.SRM_NAS_LOCATOR_E2E_TENANT || '测试租户',
  username: process.env.SRM_NAS_LOCATOR_E2E_USERNAME || 'aoteman',
  password: process.env.SRM_NAS_LOCATOR_E2E_PASSWORD || '111111',
  keyword: process.env.SRM_NAS_LOCATOR_E2E_KEYWORD || '*MO13*.pdf',
  headless: process.env.SRM_NAS_LOCATOR_E2E_HEADED !== '1'
}

function isSuccessPayload(payload) {
  return Boolean(payload) && (payload.code === 0 || payload.code === 200)
}

async function settle(page, timeout = 30000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const target = locator.nth(index)
    if (await target.isVisible()) {
      await target.fill('')
      await target.fill(value)
      return target
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/srm/nas-locator`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]')
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(
    loginForm.locator('input[type="password"], input[placeholder="请输入密码"]'),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function main() {
  const browser = await chromium.launch({ headless: config.headless })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const consoleErrors = []
  const pageErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => {
    pageErrors.push(error.message)
  })
  page.on('close', () => {
    console.error('PAGE_CLOSED')
  })
  page.on('crash', () => {
    console.error('PAGE_CRASHED')
  })

  try {
    await login(page)
    await page.goto(`${config.baseUrl}/srm/nas-locator`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)
    await page.getByText('关键词', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })

    const searchResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/srm/nas-locator/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    const input = page
      .locator('input[placeholder*="请输入关键词"], .el-form input.el-input__inner')
      .first()
    await input.waitFor({ state: 'visible', timeout: 30000 })
    await input.fill('')
    await input.fill(config.keyword)
    const response = await Promise.all([
      searchResponsePromise,
      page.getByRole('button', { name: '搜索' }).click()
    ]).then((result) => result[0])
    const payload = await response.json().catch(() => null)
    const bodyText = await page.locator('body').innerText()

    console.log(
      JSON.stringify(
        {
          keyword: config.keyword,
          url: response.url(),
          status: response.status(),
          ok: response.ok(),
          payload,
          pageErrors,
          consoleErrors,
          pageToast: bodyText.includes('系统异常'),
          pageBodyExcerpt: bodyText.slice(0, 500)
        },
        null,
        2
      )
    )

    if (!isSuccessPayload(payload)) {
      throw new Error(`search failed: HTTP ${response.status()} ${JSON.stringify(payload)}`)
    }
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
