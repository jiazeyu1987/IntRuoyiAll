const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const root = path.resolve(__dirname, '../..')
const taskDir = path.resolve(root, '../doc/tasks/20260725-codex-test-management-admin-e2e')
const summaryPath = path.join(taskDir, 'system-codex-test-management-real-summary.json')
const screenshotPath = path.join(taskDir, 'system-codex-test-management-real.png')

function parseEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {}
  const result = {}
  for (const rawLine of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const match = line.match(/^([^=]+?)\s*=\s*(.*)$/)
    if (!match) continue
    result[match[1].trim()] = match[2].trim().replace(/^['"]|['"]$/g, '')
  }
  return result
}

const envFile = parseEnvFile(path.join(root, '.env'))
const config = {
  baseUrl: (process.env.CODEX_TEST_MANAGEMENT_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.CODEX_TEST_MANAGEMENT_TENANT || envFile.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: process.env.CODEX_TEST_MANAGEMENT_USERNAME || envFile.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: process.env.CODEX_TEST_MANAGEMENT_PASSWORD || envFile.VITE_APP_DEFAULT_LOGIN_PASSWORD
}

function requireConfig(name, value) {
  if (!value || !String(value).trim()) {
    throw new Error(`${name} is required for system codex test management real E2E`)
  }
}

for (const [name, value] of Object.entries(config)) {
  requireConfig(name, value)
}

function unwrap(payload) {
  return payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')
    ? payload.data
    : payload
}

function menuContains(menus, predicate) {
  const queue = Array.isArray(menus) ? [...menus] : []
  while (queue.length > 0) {
    const menu = queue.shift()
    if (predicate(menu)) return true
    if (Array.isArray(menu.children)) queue.push(...menu.children)
  }
  return false
}

async function firstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`No visible element found for ${label}`)
}

async function fillFirstVisible(locator, value, label) {
  const item = await firstVisible(locator, label)
  await item.fill(value)
}

async function selectTenant(page) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    return
  }
  await tenantSelect.click()
  const input = page.locator('.login-form .el-select__input').first()
  await input.fill(config.tenant)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
  if ((await option.count()) > 0) {
    await option.click()
  } else {
    await input.press('Enter')
  }
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)

  await selectTenant(page)
  await fillFirstVisible(
    page.locator('.login-form input[placeholder="请输入用户名"], .login-form input.el-input__inner:not([type="password"]):not([role="combobox"])'),
    config.username,
    'username input'
  )
  await fillFirstVisible(
    page.locator('.login-form input[type="password"], .login-form input[placeholder="请输入密码"]'),
    config.password,
    'password input'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 30000 }
  )
  await page.locator('button:has-text("登录")').first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginPayload.code, 0, `login should succeed for ${config.tenant}/${config.username}`)
  const permissionResponse = await permissionResponsePromise
  const permissionPayload = await permissionResponse.json()
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  return unwrap(permissionPayload)
}

async function openSystemManagementMenu(page) {
  const systemMenu = page.getByText('系统管理', { exact: true }).first()
  await systemMenu.waitFor({ state: 'visible', timeout: 30000 })
  await systemMenu.click()
}

async function main() {
  fs.mkdirSync(taskDir, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const summary = {
    tenant: config.tenant,
    username: config.username,
    baseUrl: config.baseUrl,
    hasCodexPermission: false,
    hasCodexMenuContract: false,
    openedPage: false,
    screenshot: screenshotPath,
    checkedAt: new Date().toISOString()
  }

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const page = await context.newPage()
    const permissionInfo = await login(page)
    const permissions = Array.isArray(permissionInfo.permissions) ? permissionInfo.permissions : []
    const menus = Array.isArray(permissionInfo.menus) ? permissionInfo.menus : []
    summary.hasCodexPermission = permissions.includes('system:codex-test:query') || permissions.includes('*:*:*')
    summary.hasCodexMenuContract = menuContains(
      menus,
      (menu) =>
        menu?.name === '测试管理' ||
        menu?.path === 'codex-test-management' ||
        menu?.component === 'system/codex-test-management/index'
    )

    assert.ok(
      summary.hasCodexPermission,
      'permission response must include system:codex-test:query for 芋道源码/admin'
    )
    assert.ok(summary.hasCodexMenuContract, 'dynamic menu response must include 测试管理 menu')

    await openSystemManagementMenu(page)
    const testManagementMenu = page.getByText('测试管理', { exact: true }).first()
    await testManagementMenu.waitFor({ state: 'visible', timeout: 30000 })

    const pageResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/codex-test-case/page') && response.status() === 200,
      { timeout: 30000 }
    )
    await testManagementMenu.click()
    const pageResponse = await pageResponsePromise
    const pagePayload = await pageResponse.json()
    assert.equal(pagePayload.code, 0, 'codex test case page API should return business code 0')

    await page.locator('text=测试方法项').waitFor({ state: 'visible', timeout: 30000 })
    await page.locator('text=检查点').first().waitFor({ state: 'visible', timeout: 30000 })
    await page.locator('button:has-text("新增")').first().waitFor({ state: 'visible', timeout: 30000 })
    await page.screenshot({ path: screenshotPath, fullPage: true })

    summary.openedPage = true
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    await context.close()
    console.log('PASS: system codex test management real E2E')
  } catch (error) {
    summary.error = error.message
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
