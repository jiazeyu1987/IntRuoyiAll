const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const text = {
  tenant: '\u6d4b\u8bd5\u79df\u6237',
  adminTenant: '\u828b\u9053\u6e90\u7801',
  login: '\u767b\u5f55',
  exportExcel: '\u5bfc\u51fa Excel',
  precheck: '\u9884\u68c0',
  precheckPassed: '\u9884\u68c0\u901a\u8fc7',
  precheckFailed: '\u9884\u68c0\u672a\u901a\u8fc7',
  tenantPlaceholder: '\u8bf7\u8f93\u5165\u79df\u6237\u540d\u79f0',
  usernamePlaceholder: '\u8bf7\u8f93\u5165\u7528\u6237\u540d',
  passwordPlaceholder: '\u8bf7\u8f93\u5165\u5bc6\u7801'
}

const config = {
  baseUrl: (process.env.SYSTEM_CONFIG_PACKAGE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.SYSTEM_CONFIG_PACKAGE_E2E_TENANT || text.adminTenant,
  username: process.env.SYSTEM_CONFIG_PACKAGE_E2E_USERNAME || 'admin',
  password: process.env.SYSTEM_CONFIG_PACKAGE_E2E_PASSWORD || 'admin123',
  targetPath: '/system/config-package',
  headed: process.env.SYSTEM_CONFIG_PACKAGE_E2E_HEADED === '1',
  requireValidPrecheck: process.env.SYSTEM_CONFIG_PACKAGE_E2E_REQUIRE_VALID === '1'
}

const repoRoot = path.resolve(__dirname, '../../..')
const taskDir = path.join(repoRoot, 'doc/tasks/20260617-system-config-package-import-export/e2e-artifacts')
const workbookPath = path.join(taskDir, 'system-config-package-real-e2e.xlsx')
const screenshotPath = path.join(taskDir, 'system-config-package-page.png')
const loginFailedScreenshotPath = path.join(taskDir, 'system-config-package-login-failed.png')

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(700)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function selectTenant(page, loginForm) {
  const tenantInput = loginForm
    .locator(`.el-select input[role="combobox"], input.el-select__input, input[placeholder="${text.tenantPlaceholder}"]`)
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(config.tenant)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await tenantResponsePromise
}

async function getLoginProbe(page) {
  return page.evaluate(() => ({
    url: window.location.href,
    inputs: Array.from(document.querySelectorAll('input')).map((input) => ({
      type: input.getAttribute('type'),
      placeholder: input.getAttribute('placeholder'),
      value: input.value,
      visible: Boolean(input.offsetParent),
      disabled: input.disabled
    })),
    buttons: Array.from(document.querySelectorAll('button')).map((button) => ({
      text: (button.textContent || '').replace(/\s+/g, ' ').trim(),
      disabled: button.disabled,
      visible: Boolean(button.offsetParent)
    })),
    body: (document.body.innerText || '').slice(0, 1000)
  }))
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return null
  }

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, loginForm)
  const textboxes = loginForm.getByRole('textbox')
  const textboxCount = await textboxes.count()
  if (textboxCount >= 2) {
    await textboxes.nth(textboxCount >= 3 ? 1 : 0).fill('')
    await textboxes.nth(textboxCount >= 3 ? 1 : 0).fill(config.username)
  } else {
    await fillFirstVisible(
      loginForm.locator(`input[placeholder="${text.usernamePlaceholder}"]`),
      config.username,
      'username'
    )
  }
  await fillFirstVisible(
    loginForm.locator(`input[type="password"], input[placeholder="${text.passwordPlaceholder}"]`),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  ).catch(() => null)
  const observedRequests = []
  const requestListener = (request) => {
    if (request.url().includes('/admin-api/system/')) {
      observedRequests.push(`${request.method()} ${request.url()}`)
    }
  }
  page.on('request', requestListener)
  const loginButton = loginForm.getByRole('button', { name: text.login }).first()
  await loginButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await loginButton.isEnabled(), true, 'login button must be enabled')
  await loginButton.click()
  let loginResponse
  try {
    loginResponse = await loginResponsePromise
  } catch (error) {
    await page.screenshot({ path: loginFailedScreenshotPath, fullPage: true }).catch(() => null)
    const probe = await getLoginProbe(page).catch((probeError) => ({ probeError: probeError.message }))
    throw new Error(
      `login_response_timeout:${error.message}; observedRequests=${observedRequests.join(' || ')}; probe=${JSON.stringify(probe)}; screenshot=${loginFailedScreenshotPath}`
    )
  } finally {
    page.off('request', requestListener)
  }
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(loginPayload && (loginPayload.code === 0 || loginPayload.code === 200), `login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  const permissionResponse = await permissionPromise
  return permissionResponse ? await permissionResponse.json().catch(() => null) : null
}

function flattenMenus(list, result = []) {
  for (const item of Array.isArray(list) ? list : []) {
    result.push(item)
    flattenMenus(item.children, result)
  }
  return result
}

async function main() {
  fs.mkdirSync(taskDir, { recursive: true })
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    acceptDownloads: true,
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const pageErrors = []
  const consoleWarningsOrErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      consoleWarningsOrErrors.push(`${message.type()}: ${message.text()}`)
    }
  })

  try {
    const permissionPayload = await login(page)
    await settle(page)
    const permissionData = permissionPayload?.data || {}
    const permissions = permissionData.permissions || []
    const menus = flattenMenus(permissionData.menus || permissionData.menuList || [])
    const permission = {
      hasQueryPerm: permissions.includes('system:config-package:query'),
      hasExportPerm: permissions.includes('system:config-package:export'),
      hasImportPerm: permissions.includes('system:config-package:import'),
      hasMenuRoute: menus.some(
        (menu) => menu?.path === 'config-package' || menu?.component === 'system/config-package/index'
      )
    }

    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page, 30000)
    await page.screenshot({ path: screenshotPath, fullPage: true })
    const bodyText = await page.locator('body').innerText({ timeout: 10000 }).catch(() => '')
    if (!permission.hasQueryPerm || !permission.hasExportPerm || !permission.hasImportPerm || !permission.hasMenuRoute) {
      throw new Error(
        `config_package_permission_or_menu_missing:${JSON.stringify({
          ...permission,
          url: page.url(),
          body: bodyText.slice(0, 500)
        })}`
      )
    }
    if (/404|Not Found/i.test(bodyText)) {
      throw new Error(`config_package_page_not_found:url=${page.url()} body=${bodyText.slice(0, 800)}`)
    }

    const exportButton = page.getByRole('button', { name: text.exportExcel }).first()
    await exportButton.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(await exportButton.isEnabled(), true, 'export button must be enabled')
    const [exportResponse, download] = await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/system/config-package/export-excel') && response.status() === 200,
        { timeout: 60000 }
      ),
      page.waitForEvent('download', { timeout: 60000 }),
      exportButton.click()
    ])
    assert.equal(exportResponse.status(), 200, `export http status must be 200, got ${exportResponse.status()}`)
    await download.saveAs(workbookPath)
    const workbookStat = fs.statSync(workbookPath)
    assert.ok(workbookStat.size > 1024, `downloaded workbook is too small: ${workbookStat.size}`)

    const uploadInput = page.locator('.config-package-upload input[type="file"]').first()
    await uploadInput.setInputFiles(workbookPath)
    const precheckButton = page.getByRole('button', { name: text.precheck }).first()
    await precheckButton.waitFor({ state: 'visible', timeout: 30000 })
    await page.waitForFunction((label) => {
      const buttons = Array.from(document.querySelectorAll('button'))
      const button = buttons.find((item) => (item.textContent || '').trim() === label)
      return button && !button.disabled
    }, text.precheck, { timeout: 30000 })
    const precheckResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/config-package/precheck') && response.status() === 200,
      { timeout: 60000 }
    )
    await precheckButton.click()
    const precheckResponse = await precheckResponsePromise
    const precheckPayload = await precheckResponse.json()
    assert.ok(precheckPayload && (precheckPayload.code === 0 || precheckPayload.code === 200), `precheck failed: ${JSON.stringify(precheckPayload)}`)
    assert.ok(precheckPayload.data, `precheck response must include data: ${JSON.stringify(precheckPayload)}`)
    assert.ok(
      Array.isArray(precheckPayload.data.blockingErrors),
      `precheck response must include blockingErrors array: ${JSON.stringify(precheckPayload.data)}`
    )
    if (config.requireValidPrecheck) {
      assert.equal(
        precheckPayload.data.valid,
        true,
        `precheck must be valid when SYSTEM_CONFIG_PACKAGE_E2E_REQUIRE_VALID=1: ${JSON.stringify(precheckPayload.data)}`
      )
    }
    const precheckStatus = precheckPayload.data.valid ? text.precheckPassed : text.precheckFailed
    await page
      .locator('.config-package-summary')
      .filter({ hasText: precheckStatus })
      .first()
      .waitFor({ state: 'visible', timeout: 30000 })
    await page.screenshot({ path: screenshotPath, fullPage: true })
    assert.deepEqual(pageErrors, [], `page errors were emitted: ${pageErrors.join(' | ')}`)

    console.log(JSON.stringify({
      status: 'PASS',
      url: page.url(),
      workbookPath,
      workbookBytes: workbookStat.size,
      screenshotPath,
      permission,
      precheck: {
        valid: precheckPayload.data.valid,
        sheetDiffs: precheckPayload.data.sheetDiffs?.length || 0,
        blockingErrors: precheckPayload.data.blockingErrors?.length || 0,
        warnings: precheckPayload.data.warnings?.length || 0,
        firstBlockingErrors: precheckPayload.data.blockingErrors?.slice(0, 5) || []
      },
      consoleWarningsOrErrors: consoleWarningsOrErrors.slice(0, 20)
    }, null, 2))
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error.stack || error.message || error)
  process.exit(1)
})
