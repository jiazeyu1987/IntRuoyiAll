const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const root = path.resolve(__dirname, '../..')
const taskDir = path.resolve(root, '../doc/tasks/20260830-system-user-lifecycle-deactivation')
const outputDir = path.resolve(root, '../output/playwright')
const runSuffix = Date.now().toString(36)
const screenshotPath = path.join(
  outputDir,
  `system-user-lifecycle-deactivation-real-${runSuffix}.png`
)
const summaryPath = path.join(taskDir, 'system-user-lifecycle-deactivation-real-summary.json')

const config = {
  baseUrl: (process.env.SYSTEM_USER_LIFECYCLE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.SYSTEM_USER_LIFECYCLE_E2E_TENANT || '测试租户',
  username: process.env.SYSTEM_USER_LIFECYCLE_E2E_USERNAME || 'aoteman',
  password: process.env.SYSTEM_USER_LIFECYCLE_E2E_PASSWORD || '111111'
}

function requireConfig(name, value) {
  if (!value || !String(value).trim()) {
    throw new Error(`${name} is required for system user lifecycle deactivation real E2E`)
  }
}

for (const [name, value] of Object.entries(config)) {
  requireConfig(name, value)
}

if (config.tenant !== '测试租户') {
  throw new Error('system user lifecycle deactivation real E2E must run against 测试租户')
}

function isSuccessPayload(payload) {
  return Boolean(payload) && (payload.code === 0 || payload.code === 200)
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

function hasPermission(permissions, permission) {
  return permissions.includes(permission) || permissions.includes('*:*:*')
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDateTimeForPicker(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function normalizeDateTime(value) {
  if (Array.isArray(value) && value.length >= 6) {
    const [year, month, day, hour, minute, second] = value
    return `${year}-${pad(month)}-${pad(day)}T${pad(hour)}:${pad(minute)}:${pad(second)}`
  }
  if (typeof value === 'number') {
    return formatDateTimeForPicker(new Date(value)).replace(' ', 'T')
  }
  if (typeof value === 'string') {
    return value.replace(' ', 'T').slice(0, 19)
  }
  return ''
}

function sanitizeErrorMessage(message) {
  return String(message || '').replace(/\u001b\[[0-9;]*m/g, '')
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
  await item.fill('')
  await item.fill(value)
  return item
}

function formField(container, labelText) {
  return container.locator('.el-form-item').filter({ hasText: labelText }).first()
}

async function fillFormInput(container, labelText, value) {
  const input = formField(container, labelText).locator('input').last()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill('')
  await input.fill(value)
  return input
}

async function fillDateInput(container, labelText, value) {
  const input = await fillFormInput(container, labelText, value)
  await input.press('Enter')
  await input.blur()
}

async function waitForApiSuccess(page, pathFragment, method, action) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(pathFragment) && item.request().method() === method,
    { timeout: 60000 }
  )
  assert.equal(response.status(), 200, `${action} should return HTTP 200`)
  const payload = await response.json()
  assert.ok(isSuccessPayload(payload), `${action} failed: ${JSON.stringify(payload)}`)
  return payload
}

async function apiJson(page, authContext, relativePath, method = 'GET', body) {
  const payload = await page.evaluate(
    async ({ relativePath, method, body, authContext }) => {
      const headers = {
        Accept: 'application/json, text/plain, */*',
        Authorization: authContext.authorization,
        'tenant-id': authContext.tenantId
      }
      if (body !== undefined) {
        headers['Content-Type'] = 'application/json;charset=UTF-8'
      }
      const response = await fetch(relativePath, {
        method,
        credentials: 'include',
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
      })
      return { status: response.status, text: await response.text() }
    },
    { relativePath, method, body, authContext }
  )
  assert.equal(payload.status, 200, `${method} ${relativePath} should return HTTP 200`)
  const data = JSON.parse(payload.text)
  assert.ok(isSuccessPayload(data), `${method} ${relativePath} failed: ${payload.text}`)
  return data
}

async function selectTenant(page) {
  const tenantInput = page
    .locator('.login-form input[placeholder="请输入租户名称"], .login-form .el-select__input')
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
}

async function login(page) {
  await page.context().clearCookies()
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page)
  await fillFirstVisible(
    page.locator(
      '.login-form input[placeholder="请输入用户名"], .login-form input.el-input__inner:not([type="password"]):not([role="combobox"])'
    ),
    config.username,
    'username input'
  )
  await fillFirstVisible(
    page.locator('.login-form input[type="password"], .login-form input[placeholder="请输入密码"]'),
    config.password,
    'password input'
  )

  const loginResponsePromise = waitForApiSuccess(
    page,
    '/admin-api/system/auth/login',
    'POST',
    '登录'
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: '登录' }).click()
  await loginResponsePromise
  const permissionResponse = await permissionResponsePromise
  const permissionPayload = await permissionResponse.json()
  assert.ok(isSuccessPayload(permissionPayload), 'permission info should return business success')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })

  const headers = permissionResponse.request().headers()
  const authorization = headers.authorization || headers.Authorization || ''
  const tenantId = headers['tenant-id'] || headers['Tenant-Id'] || ''
  assert.notEqual(authorization, '', 'permission request should include Authorization header')
  assert.notEqual(tenantId, '', 'permission request should include tenant-id header')
  return {
    authorization,
    tenantId,
    permissionInfo: unwrap(permissionPayload)
  }
}

function assertUserManagementAccess(permissionInfo) {
  const permissions = Array.isArray(permissionInfo.permissions) ? permissionInfo.permissions : []
  const menus = Array.isArray(permissionInfo.menus) ? permissionInfo.menus : []
  for (const permission of [
    'system:user:query',
    'system:user:create',
    'system:user:update',
    'system:user:delete'
  ]) {
    assert.ok(hasPermission(permissions, permission), `missing permission ${permission}`)
  }
  assert.ok(
    menuContains(
      menus,
      (menu) =>
        menu?.name === '用户管理' ||
        menu?.path === 'user' ||
        menu?.component === 'system/user/index'
    ),
    'dynamic menu response must include 用户管理 menu'
  )
}

async function openSystemUserPage(page) {
  const pageResponsePromise = waitForApiSuccess(
    page,
    '/admin-api/system/user/page',
    'GET',
    '打开用户列表'
  )
  await page.goto(`${config.baseUrl}/system/user`, { waitUntil: 'domcontentloaded' })
  await pageResponsePromise
  await page.locator('.system-user-resizable-table').waitFor({ state: 'visible', timeout: 60000 })
}

async function openCreateUserDialog(page) {
  await page.getByRole('button', { name: /高级/ }).first().click()
  await page.locator('.el-dropdown-menu:visible').getByText('新增', { exact: true }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '用户昵称' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  return dialog
}

async function createUserThroughUi(page, fixture) {
  const dialog = await openCreateUserDialog(page)
  await fillFormInput(dialog, '用户昵称', fixture.nickname)
  await fillFormInput(dialog, '用户名称', fixture.username)
  await fillFormInput(dialog, '用户密码', fixture.password)
  const createPromise = waitForApiSuccess(page, '/admin-api/system/user/create', 'POST', '新增用户')
  await dialog.getByRole('button', { name: '确 定' }).click()
  const createPayload = await createPromise
  const userId = unwrap(createPayload)
  assert.ok(Number.isFinite(Number(userId)), `create user response should return user id: ${userId}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  return Number(userId)
}

async function ensureUsernameFilterVisible(page) {
  const input = page.locator('.table-multi-filter input[placeholder="请输入用户名称"]').first()
  if ((await input.count()) === 0) {
    await page.locator('.table-multi-filter button[aria-label="新增筛选条件"]').first().click()
  }
  await input.waitFor({ state: 'visible', timeout: 30000 })
  const fieldSelect = page.locator('.table-multi-filter__field-select').first()
  const fieldText = (await fieldSelect.textContent()) || ''
  if (!fieldText.includes('用户名称')) {
    await fieldSelect.click()
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '用户名称' }).click()
  }
  return input
}

async function searchUserByUsername(page, username) {
  const input = await ensureUsernameFilterVisible(page)
  await input.fill('')
  await input.fill(username)
  const pagePromise = waitForApiSuccess(page, '/admin-api/system/user/page', 'GET', '查询用户')
  await page.locator('.table-multi-filter').getByRole('button', { name: /查询/ }).click()
  await pagePromise
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: username }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function registerLifecycleDeactivationThroughUi(page, fixture) {
  await searchUserByUsername(page, fixture.username)
  const lifecycleButton = page.getByRole('button', { name: '离职/转岗' }).first()
  assert.equal(await lifecycleButton.isEnabled(), true, 'lifecycle action should be enabled before registration')
  await lifecycleButton.click()

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '登记离职/转岗停用' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByDisplayValue(new RegExp(fixture.username)).waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '离职/转岗单号', fixture.documentNo)
  await fillDateInput(dialog, '单据时间', fixture.documentTimeDisplay)
  await fillDateInput(dialog, '生效时间', fixture.effectiveTimeDisplay)

  const lifecyclePromise = waitForApiSuccess(
    page,
    '/admin-api/system/user/lifecycle-deactivation',
    'PUT',
    '登记离职/转岗停用'
  )
  await dialog.getByRole('button', { name: '确 定' }).click()
  await lifecyclePromise
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function verifyLifecycleResult(page, authContext, fixture) {
  const payload = await apiJson(
    page,
    authContext,
    `/admin-api/system/user/page?pageNo=1&pageSize=10&username=${encodeURIComponent(fixture.username)}`
  )
  const data = unwrap(payload)
  const list = Array.isArray(data?.list) ? data.list : []
  const user = list.find((item) => item.username === fixture.username)
  assert.ok(user, `created user ${fixture.username} should be present in user page API`)
  assert.equal(user.status, 1, 'user should be disabled after due lifecycle deactivation')
  assert.equal(user.lifecycleDocumentType, 'RESIGNATION')
  assert.equal(user.lifecycleDocumentNo, fixture.documentNo)
  assert.equal(normalizeDateTime(user.lifecycleDocumentTime), fixture.documentTimeIso)
  assert.equal(normalizeDateTime(user.lifecycleEffectiveTime), fixture.effectiveTimeIso)
  assert.equal(normalizeDateTime(user.lifecycleDeactivatedTime), fixture.effectiveTimeIso)

  const row = await searchUserByUsername(page, fixture.username)
  const statusSwitch = row.locator('.el-switch').first()
  await statusSwitch.waitFor({ state: 'visible', timeout: 30000 })
  const switchDisabled = await statusSwitch.evaluate((node) => {
    const input = node.querySelector('input')
    return node.classList.contains('is-disabled') || node.getAttribute('aria-disabled') === 'true' || Boolean(input?.disabled)
  })
  assert.equal(switchDisabled, true, 'status switch should be disabled after lifecycle deactivation')
  assert.equal(
    await page.getByRole('button', { name: '离职/转岗' }).first().isDisabled(),
    true,
    'lifecycle action should be disabled after registration'
  )
  return user
}

async function deleteStaleLifecycleE2eUsers(page, authContext) {
  const payload = await apiJson(
    page,
    authContext,
    '/admin-api/system/user/page?pageNo=1&pageSize=100&username=e2eulc'
  )
  const data = unwrap(payload)
  const list = Array.isArray(data?.list) ? data.list : []
  const staleUsers = list.filter(
    (item) => typeof item.username === 'string' && item.username.startsWith('e2eulc')
  )
  for (const user of staleUsers) {
    await apiJson(page, authContext, `/admin-api/system/user/delete?id=${user.id}`, 'DELETE')
  }
  return staleUsers.length
}

async function deleteCreatedUser(page, authContext, userId, username) {
  await apiJson(page, authContext, `/admin-api/system/user/delete?id=${userId}`, 'DELETE')
  const payload = await apiJson(
    page,
    authContext,
    `/admin-api/system/user/page?pageNo=1&pageSize=10&username=${encodeURIComponent(username)}`
  )
  const data = unwrap(payload)
  const list = Array.isArray(data?.list) ? data.list : []
  assert.equal(list.some((item) => item.username === username), false, 'created test user should be deleted')
}

async function main() {
  fs.mkdirSync(taskDir, { recursive: true })
  fs.mkdirSync(outputDir, { recursive: true })

  const now = new Date()
  const documentTime = new Date(now.getTime() - 120000)
  const effectiveTime = new Date(now.getTime() - 60000)
  const fixture = {
    username: `e2eulc${runSuffix}`.slice(0, 30),
    nickname: `联动停用E2E${runSuffix.slice(-6)}`,
    password: `E2eUser${runSuffix}!9`,
    documentNo: `LZ${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${runSuffix}`,
    documentTimeDisplay: formatDateTimeForPicker(documentTime),
    effectiveTimeDisplay: formatDateTimeForPicker(effectiveTime),
    documentTimeIso: formatDateTimeForPicker(documentTime).replace(' ', 'T'),
    effectiveTimeIso: formatDateTimeForPicker(effectiveTime).replace(' ', 'T')
  }
  const summary = {
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    loginUsername: config.username,
    fixtureUsername: fixture.username,
    documentNo: fixture.documentNo,
    createdUserId: null,
    preflightDeletedStaleUsers: 0,
    lifecycleRegistered: false,
    apiVerified: false,
    uiVerified: false,
    cleanupDeleted: false,
    screenshot: screenshotPath,
    checkedAt: new Date().toISOString()
  }

  const browser = await chromium.launch({ headless: true })
  let context
  let page
  let authContext
  try {
    context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    page = await context.newPage()
    authContext = await login(page)
    assertUserManagementAccess(authContext.permissionInfo)
    await openSystemUserPage(page)
    summary.preflightDeletedStaleUsers = await deleteStaleLifecycleE2eUsers(page, authContext)
    summary.createdUserId = await createUserThroughUi(page, fixture)
    await searchUserByUsername(page, fixture.username)
    await registerLifecycleDeactivationThroughUi(page, fixture)
    const verifiedUser = await verifyLifecycleResult(page, authContext, fixture)
    summary.lifecycleRegistered = true
    summary.apiVerified = true
    summary.uiVerified = true
    summary.verifiedLifecycleDeactivatedTime = normalizeDateTime(verifiedUser.lifecycleDeactivatedTime)
    await page.screenshot({ path: screenshotPath, fullPage: true })
  } catch (error) {
    summary.error = sanitizeErrorMessage(error.message)
    throw error
  } finally {
    try {
      if (page && authContext && summary.createdUserId) {
        await deleteCreatedUser(page, authContext, summary.createdUserId, fixture.username)
        summary.cleanupDeleted = true
      }
    } finally {
      fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
      if (context) await context.close()
      await browser.close()
    }
  }

  console.log('PASS: system user lifecycle deactivation real E2E')
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
