const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.REG_CERT_BUSINESS_TIME_BASE_URL || 'http://127.0.0.1:8315'
const BACKEND_URL = process.env.REG_CERT_BUSINESS_TIME_BACKEND_URL || 'http://127.0.0.1:48315'
const TENANT_NAME = process.env.REG_CERT_BUSINESS_TIME_TENANT || '芋道源码'
const USERNAME = process.env.REG_CERT_BUSINESS_TIME_USERNAME || 'admin'
const BUSINESS_DATE = process.env.REG_CERT_BUSINESS_TIME_DATE || '2000-01-01'
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const OUTPUT_DIR = path.resolve(
  __dirname,
  '../../../doc/tasks/20260901-registration-business-time-simulation-integration/e2e-output'
)

function readRequiredPassword() {
  const password = process.env.REG_CERT_BUSINESS_TIME_PASSWORD
  assert.ok(
    password,
    'REG_CERT_BUSINESS_TIME_PASSWORD is required for real E2E login; the current frontend no longer stores a default login password in .env'
  )
  return password
}

async function login(page, password) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent('/mdm/registration-certificate')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible')
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.fill(TENANT_NAME)
    const tenantOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', {
      hasText: TENANT_NAME
    })
    if ((await tenantOption.count()) > 0) {
      await tenantOption.first().click()
    } else {
      await tenantInput.press('Enter')
    }
  }
  await form
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
    .fill(USERNAME)
  await form.locator('input[type="password"]:visible').first().fill(password)
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.status(), 200)
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `login failed: ${loginBody.msg || loginBody.message}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function waitUntilEnabled(page, locator, timeoutMs = 30000) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    if (await locator.isEnabled()) {
      return
    }
    await page.waitForTimeout(250)
  }
  throw new Error('simulate button did not become enabled')
}

async function run() {
  assert.match(BASE_URL, /^http:\/\/(127\.0\.0\.1|localhost):(8081|8315)$/)
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):(48081|48315)$/)
  assert.ok(
    /^\d{4}-\d{2}-\d{2}$/.test(BUSINESS_DATE) && BUSINESS_DATE < '2000-01-02',
    'E2E must use a no-due-candidate safety date before 2000-01-02'
  )
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome not found: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })

  const password = readRequiredPassword()
  const browser = await chromium.launch({ headless: true, executablePath: BROWSER_EXECUTABLE })
  const context = await browser.newContext({ viewport: { width: 1680, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const consoleErrors = []
  const failedResponses = []
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('response', (response) => {
    if (response.status() >= 400) {
      failedResponses.push(`${response.status()} ${response.request().method()} ${response.url()}`)
    }
  })

  try {
    await login(page, password)
    await page.goto(`${BASE_URL}/mdm/registration-certificate`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.getByRole('tab', { name: '注册测试' }).waitFor({ state: 'visible', timeout: 90000 })
    await page.getByRole('tab', { name: '注册测试' }).click()
    const testTab = page.locator('[data-testid="registration-certificate-test-tab"]')
    await testTab.waitFor({ state: 'visible', timeout: 30000 })
    const dateInput = testTab
      .locator('[data-testid="registration-certificate-business-date"] input, input[placeholder="选择模拟日期"]')
      .first()
    await dateInput.fill(BUSINESS_DATE)
    await dateInput.press('Enter')
    const button = testTab.locator('[data-testid="registration-certificate-simulate-daily-run"]')
    await button.waitFor({ state: 'visible', timeout: 30000 })
    await waitUntilEnabled(page, button)
    const simulateResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/registration-certificates/business-time/simulate-daily-run') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await button.click()
    const simulateResponse = await simulateResponsePromise
    assert.equal(simulateResponse.status(), 200)
    const simulateBody = await simulateResponse.json()
    assert.ok([0, 200].includes(simulateBody.code), simulateBody.msg || simulateBody.message)
    assert.equal(String(simulateBody.data.businessDate).slice(0, 10), BUSINESS_DATE)
    assert.equal(String(simulateBody.data.simulatedAt), `${BUSINESS_DATE}T09:00`)
    assert.ok(String(simulateBody.data.jobResult).includes(`businessDate=${BUSINESS_DATE}`))
    await page.getByText(new RegExp(`注册证业务时间模拟完成|已按 ${BUSINESS_DATE} 09:00 触发注册证每日任务`)).first().waitFor({
      state: 'visible',
      timeout: 30000
    })
    const screenshotPath = path.join(OUTPUT_DIR, 'registration-certificate-business-time-simulation.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })
    const result = {
      baseUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      tenant: TENANT_NAME,
      username: USERNAME,
      businessDate: BUSINESS_DATE,
      response: {
        status: simulateResponse.status(),
        code: simulateBody.code,
        simulatedAt: simulateBody.data.simulatedAt,
        jobResult: simulateBody.data.jobResult
      },
      consoleErrorCount: consoleErrors.length,
      screenshotPath
    }
    fs.writeFileSync(
      path.join(OUTPUT_DIR, 'registration-certificate-business-time-simulation.json'),
      `${JSON.stringify(result, null, 2)}\n`,
      'utf8'
    )
    assert.equal(
      consoleErrors.length,
      0,
      `console errors: ${consoleErrors.join('\n')}\nfailed responses: ${failedResponses.join('\n')}`
    )
    console.log(
      `PASS: registration certificate business time simulation tenant=${TENANT_NAME} date=${BUSINESS_DATE} simulatedAt=${simulateBody.data.simulatedAt}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
