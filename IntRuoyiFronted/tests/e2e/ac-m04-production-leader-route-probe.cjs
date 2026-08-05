const { chromium } = require('playwright')

async function fillFirstVisible(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible()) {
      await candidate.fill(value)
      return
    }
  }
  throw new Error('No visible login input was found.')
}

async function readState(page) {
  const moduleTabs = page.locator('[data-production-leader-module-tabs]:visible')
  const tabItems = moduleTabs.locator('.el-tabs__item')
  return {
    url: page.url(),
    moduleTabContainerCount: await moduleTabs.count(),
    moduleTabTexts: await tabItems.allInnerTexts(),
    activeModuleTabTexts: await tabItems.filter({ has: page.locator('.is-active') }).allInnerTexts().catch(() => []),
    reportCount: await page.locator('[data-team-leader-report-workbench]').count(),
    reportVisible: await page.locator('[data-team-leader-report-workbench]').first().isVisible().catch(() => false),
    configCount: await page.locator('[data-team-leader-config-center]').count(),
    configVisible: await page.locator('[data-team-leader-config-center]').first().isVisible().catch(() => false),
    personnelCount: await page.locator('[data-team-leader-production-personnel-tab]').count(),
    personnelVisible: await page.locator('[data-team-leader-production-personnel-tab]').first().isVisible().catch(() => false),
    alerts: await page.locator('.el-alert').allInnerTexts().catch(() => [])
  }
}

async function main() {
  const frontendUrl = process.env.RRM_FRONTEND_URL
  const tenant = process.env.RRM_TENANT
  const username = process.env.RRM_PRODUCTION_LEADER_USERNAME
  const password = process.env.RRM_PRODUCTION_LEADER_PASSWORD
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    const loginUrl = new URL('/login', frontendUrl)
    loginUrl.searchParams.set('redirect', '/index')
    await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 90000 })
    const form = page.locator('form.login-form:visible').first()
    await form.waitFor({ state: 'visible', timeout: 90000 })
    const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').filter({ visible: true }).first()
    if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
      await tenantInput.click()
      await tenantInput.fill(tenant)
      const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: tenant }).first()
      await option.waitFor({ state: 'visible', timeout: 30000 })
      await option.click()
    }
    await fillFirstVisible(
      form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
      username
    )
    await fillFirstVisible(form.locator('input[type="password"]'), password)
    const loginResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login')
        && response.request().method() === 'POST',
      { timeout: 90000 }
    )
    await form.getByRole('button', { name: /^登录$/ }).click()
    const loginResponse = await loginResponsePromise
    const loginBody = await loginResponse.json()
    if (!loginResponse.ok() || ![0, 200].includes(loginBody.code)) {
      throw new Error(`Login failed: ${loginBody.msg || loginBody.code}`)
    }
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
    await page.goto(new URL('/mes/pro/process-pool/production-leader', frontendUrl).toString(), {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    })
    await page.waitForTimeout(1500)
    const before = await readState(page)
    const reportTab = page
      .locator('[data-production-leader-module-tabs]:visible .el-tabs__item')
      .filter({ hasText: '报工管理' })
      .first()
    await reportTab.click()
    await page.waitForTimeout(1500)
    const after = await readState(page)
    console.log(JSON.stringify({ before, after }, null, 2))
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
