// Called only by Root's credential-safe parent; stdout is captured in memory, never shown or persisted.
const { createRequire } = require('node:module')
const load = createRequire('C:/IntRuoyi/20261001-dcc-integration/IntRuoyiFronted/package.json')
const { chromium } = load('playwright')
;(async () => {
  let browser
  try {
    if (process.env.DCC_G43_LOGIN_SCOPE !== 'AUTHORIZED_LOCAL_FRONTEND_LOGIN_FOR_MAINTENANCE') throw new Error('scope')
    browser = await chromium.launch({ headless: true })
    const page = await browser.newPage({ timezoneId: 'Asia/Shanghai' })
    await page.goto('http://127.0.0.1:8067/login', { waitUntil: 'domcontentloaded' })
    const form = page.locator('.login-form:visible')
    await form.waitFor({ state: 'visible' })
    const tenant = form.locator('.el-select:visible')
    await tenant.click()
    await tenant.locator('input').fill('芋道源码')
    await tenant.locator('input').press('Enter')
    await form.getByPlaceholder(/用户名|账号/).fill(process.env.DCC_G43_USERNAME)
    await form.locator('input[type=password]').fill(process.env.DCC_G43_PASSWORD)
    const remember = form.getByRole('checkbox', { name: /记住/ })
    if (await remember.count()) await remember.uncheck()
    const received = page.waitForResponse(response => response.url().includes('/system/auth/login') && response.request().method() === 'POST')
    await form.getByRole('button', { name: '登录', exact: true }).click()
    const response = await received
    const result = await response.json()
    if (result.code !== 0 || typeof result.data?.accessToken !== 'string' || !result.data.accessToken) throw new Error('login')
    await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 45000 })
    // The authenticated response is only observed, never an API login or token bootstrap.
    process.stdout.write(JSON.stringify({ status: 'ACTUAL_FRONTEND_LOGIN_COMPLETE', accessToken: result.data.accessToken }))
  } catch (_) {
    process.stdout.write(JSON.stringify({ status: 'ACTUAL_FRONTEND_LOGIN_FAILED' }))
    process.exitCode = 1
  } finally {
    if (browser) await browser.close()
  }
})()
