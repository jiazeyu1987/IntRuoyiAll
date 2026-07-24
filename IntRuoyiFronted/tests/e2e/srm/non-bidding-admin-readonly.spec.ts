import { expect, test, type Locator, type Page } from 'playwright/test'

const BASE_URL = process.env.SRM_E2E_BASE_URL || 'http://127.0.0.1:8120'
const TENANT_NAME = process.env.SRM_E2E_TENANT || '芋道源码'
const USERNAME = process.env.SRM_E2E_USERNAME || 'admin'
const PASSWORD = process.env.SRM_E2E_PASSWORD || 'admin123'
const PROJECT_TITLE = process.env.SRM_E2E_PROJECT_TITLE || 'SRM-T3-E2E-1782000650476-非招标项目'

test.describe.configure({ mode: 'serial' })
test.setTimeout(240000)

function isSuccessPayload(payload: any) {
  return Boolean(payload) && (payload.code === 0 || payload.code === 200)
}

async function settle(page: Page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(700)
}

async function fillFirstVisible(locator: Locator, value: string, label: string) {
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

async function selectTenant(page: Page, loginForm: Locator) {
  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]')
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(TENANT_NAME)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.fill('')
  await tenantInput.fill(TENANT_NAME)
  await tenantInput.press('Enter')
  await tenantResponsePromise
}

async function login(page: Page) {
  expect(TENANT_NAME).toBe('芋道源码')
  expect(USERNAME).toBe('admin')

  await page.context().clearCookies()
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })
  await settle(page)

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, loginForm)

  const textboxes = loginForm.getByRole('textbox')
  const textboxCount = await textboxes.count()
  if (textboxCount >= 2) {
    await textboxes.nth(textboxCount >= 3 ? 1 : 0).fill('')
    await textboxes.nth(textboxCount >= 3 ? 1 : 0).fill(USERNAME)
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), USERNAME, 'username')
  }
  await fillFirstVisible(loginForm.locator('input[type="password"], input[placeholder="请输入密码"]'), PASSWORD, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: '登录' }).click()

  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  expect(isSuccessPayload(loginPayload), `login failed: ${JSON.stringify(loginPayload)}`).toBe(true)
  const permissionResponse = await permissionPromise
  const headers = permissionResponse.request().headers()
  const tenantId = headers['tenant-id'] || headers['Tenant-Id'] || ''
  expect(tenantId).toBe('1')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function openMenuPath(page: Page, labels: string[], expectedPath: string) {
  const leafLabel = labels[labels.length - 1]
  let leaf = page.locator('.el-menu-item:visible').filter({ hasText: leafLabel }).first()
  for (const label of labels.slice(0, -1)) {
    if (await leaf.isVisible().catch(() => false)) {
      break
    }
    const parent = page.locator('.el-sub-menu__title:visible').filter({ hasText: label }).first()
    await parent.waitFor({ state: 'visible', timeout: 30000 })
    await parent.click()
    await page.waitForTimeout(500)
    leaf = page.locator('.el-menu-item:visible').filter({ hasText: leafLabel }).first()
  }
  await leaf.waitFor({ state: 'visible', timeout: 30000 })
  await Promise.all([
    page.waitForURL((url) => url.href.includes(expectedPath), { timeout: 60000 }),
    leaf.click()
  ])
  await settle(page, 30000)
  expect(page.url()).toContain(expectedPath)
}

test('admin tenant cannot see test-tenant non-bidding project', async ({ page }) => {
  const mutatingRequests: string[] = []
  page.on('request', (request) => {
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(request.method()) && request.url().includes('/admin-api/')) {
      if (!request.url().includes('/system/auth/login')) {
        mutatingRequests.push(`${request.method()} ${request.url()}`)
      }
    }
  })

  await login(page)
  await openMenuPath(page, ['SRM', '非招标项目'], '/srm/non-bidding-project')

  const searchResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/srm/non-bidding-project/page') &&
      response.request().method() === 'GET' &&
      response.ok(),
    { timeout: 60000 }
  )
  const titleInput = page.locator('input[placeholder="请输入项目标题"]').first()
  await titleInput.fill('')
  await titleInput.fill(PROJECT_TITLE)
  await page.getByRole('button', { name: '搜索' }).click()

  const searchResponse = await searchResponsePromise
  const searchPayload = await searchResponse.json()
  expect(isSuccessPayload(searchPayload), `page query failed: ${JSON.stringify(searchPayload)}`).toBe(true)
  const rows = searchPayload.data?.list || []
  expect(rows, 'admin tenant should not see test-tenant project').toHaveLength(0)

  const visibleRow = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: PROJECT_TITLE }).first()
  await expect(visibleRow).toHaveCount(0)
  expect(mutatingRequests, `readonly verification should not trigger writes: ${mutatingRequests.join(', ')}`).toEqual([])
})
