import { expect, test, type Locator, type Page } from 'playwright/test'

const BASE_URL = process.env.SRM_E2E_BASE_URL || 'http://localhost:8093'
const TENANT_NAME = process.env.SRM_E2E_TENANT || '测试租户'
const USERNAME = process.env.SRM_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.SRM_E2E_PASSWORD || '111111'
const SUPPLIER_ID = Number(process.env.SRM_E2E_SUPPLIER_ID || '108')

test.describe.configure({ mode: 'serial' })
test.setTimeout(240000)

type AuthContext = {
  authorization: string
  tenantId: string
}

const requiredPermissions = [
  'srm:procurement-plan:query',
  'srm:procurement-plan:create',
  'srm:procurement-plan:submit',
  'srm:procurement-plan:audit',
  'srm:procurement-plan:generate',
  'srm:non-bidding-project:query',
  'srm:non-bidding-project:publish',
  'srm:non-bidding-project:quote',
  'srm:non-bidding-project:deal',
  'srm:non-bidding-project:contract'
]

function flattenMenus(list: any[], result: any[] = []) {
  for (const item of Array.isArray(list) ? list : []) {
    result.push(item)
    flattenMenus(item.children, result)
  }
  return result
}

function isSuccessPayload(payload: any) {
  return Boolean(payload) && (payload.code === 0 || payload.code === 200)
}

function formatDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
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

async function login(page: Page): Promise<AuthContext> {
  expect(TENANT_NAME).toBe('测试租户')
  expect(USERNAME).toBe('aoteman')

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
  const permissionPayload = await permissionResponse.json()
  const permissionData = permissionPayload.data || {}
  const permissions = permissionData.permissions || []
  const menus = flattenMenus(permissionData.menus || permissionData.menuList || [])

  for (const permission of requiredPermissions) {
    expect(permissions, `missing permission ${permission}`).toContain(permission)
  }
  expect(menus.some((menu) => menu?.component === 'srm/non-bidding-project/index')).toBe(true)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)

  const headers = permissionResponse.request().headers()
  const authorization = headers.authorization || headers.Authorization || ''
  const tenantId = headers['tenant-id'] || headers['Tenant-Id'] || ''
  expect(authorization).not.toBe('')
  expect(tenantId).toBe('122')
  return { authorization, tenantId }
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

async function waitForApiSuccess(page: Page, path: string, method: string, action: string) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(path) && item.request().method() === method,
    { timeout: 60000 }
  )
  expect(response.status(), `${action} should return HTTP 200`).toBe(200)
  const payload = await response.json().catch(() => null)
  expect(isSuccessPayload(payload), `${action} failed: ${JSON.stringify(payload)}`).toBe(true)
  return payload
}

async function apiGetJson(page: Page, relativePath: string, authContext: AuthContext) {
  const payload = await page.evaluate(
    async ({ relativePath, authorization, tenantId }) => {
      const response = await fetch(relativePath, {
        credentials: 'include',
        headers: {
          Accept: 'application/json, text/plain, */*',
          Authorization: authorization,
          'tenant-id': tenantId
        }
      })
      return { status: response.status, text: await response.text() }
    },
    { relativePath, authorization: authContext.authorization, tenantId: authContext.tenantId }
  )
  expect(payload.status, `${relativePath} should return HTTP 200`).toBe(200)
  const data = JSON.parse(payload.text)
  expect(isSuccessPayload(data), `${relativePath} failed: ${payload.text}`).toBe(true)
  return data
}

function formField(container: Locator, labelText: string) {
  return container.locator('.el-form-item').filter({ hasText: labelText }).first()
}

async function fillFormInput(container: Locator, labelText: string, value: string) {
  const input = formField(container, labelText).locator('input').last()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill('')
  await input.fill(value)
}

async function fillFormTextarea(container: Locator, labelText: string, value: string) {
  const textarea = formField(container, labelText).locator('textarea').last()
  await textarea.waitFor({ state: 'visible', timeout: 30000 })
  await textarea.fill('')
  await textarea.fill(value)
}

async function fillFirstLineInputs(dialog: Locator, values: string[]) {
  const row = dialog.locator('.el-table__body-wrapper tbody tr').first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const inputs = row.locator('input')
  for (let index = 0; index < values.length; index += 1) {
    const input = inputs.nth(index)
    await input.waitFor({ state: 'visible', timeout: 30000 })
    await input.fill('')
    await input.fill(values[index])
    if (/^\d{4}-\d{2}-\d{2}$/.test(values[index])) {
      await input.press('Enter')
    }
  }
}

async function searchByPlaceholder(page: Page, placeholder: string, value: string, apiPath: string, action: string) {
  await page.locator(`input[placeholder="${placeholder}"]`).first().fill('')
  const responsePromise = waitForApiSuccess(page, apiPath, 'GET', action)
  await page.locator(`input[placeholder="${placeholder}"]`).first().fill(value)
  await page.getByRole('button', { name: '搜索' }).click()
  await responsePromise
}

async function tableRow(page: Page, text: string) {
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: text }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function createApprovedNonBiddingProject(page: Page, title: string) {
  await openMenuPath(page, ['SRM', '采购计划'], '/srm/procurement-plan')
  await page.getByRole('button', { name: '新增计划' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增采购计划' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '计划标题', title)
  await fillFormInput(dialog, '预计金额', '16888.5')
  await fillFormTextarea(dialog, '备注', `T3真实E2E采购计划 ${title}`)
  await fillFirstLineInputs(dialog, [`NBT-${Date.now()}`, `T3非招标物料-${title}`, '3001', '10', '件', '2026-07-01'])
  const createPromise = waitForApiSuccess(page, '/srm/procurement-plan/create', 'POST', '新增采购计划')
  await dialog.getByRole('button', { name: '保存' }).click()
  await createPromise
  await searchByPlaceholder(page, '请输入计划标题', title, '/srm/procurement-plan/page', '查询采购计划')

  let row = await tableRow(page, title)
  const submitPromise = waitForApiSuccess(page, '/srm/procurement-plan/submit', 'PUT', '提交采购计划')
  await row.getByRole('button', { name: '提交' }).click()
  await submitPromise
  await searchByPlaceholder(page, '请输入计划标题', title, '/srm/procurement-plan/page', '查询采购计划')

  row = await tableRow(page, title)
  const approvePromise = waitForApiSuccess(page, '/srm/procurement-plan/approve', 'PUT', '通过采购计划')
  await row.getByRole('button', { name: '通过' }).click()
  const auditDialog = page.locator('.el-dialog:visible').filter({ hasText: '通过采购计划' }).last()
  await auditDialog.locator('textarea').fill(`T3真实E2E审核通过 ${title}`)
  await auditDialog.getByRole('button', { name: '提交' }).click()
  await approvePromise
  await searchByPlaceholder(page, '请输入计划标题', title, '/srm/procurement-plan/page', '查询采购计划')

  row = await tableRow(page, title)
  const generatePromise = waitForApiSuccess(page, '/srm/procurement-plan/generate-sourcing', 'POST', '生成非招标项目')
  await row.getByRole('button', { name: '生成项目' }).click()
  await page.locator('.el-dropdown-menu:visible').getByText('非招标项目').click()
  const payload = await generatePromise
  expect(payload.data.projectType).toBe('NON_BIDDING')
  return payload.data
}

async function publishProject(page: Page, title: string) {
  await openMenuPath(page, ['SRM', '非招标项目'], '/srm/non-bidding-project')
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/non-bidding-project/page', '查询非招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '发布' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '发布非招标项目' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const now = Date.now()
  await fillFormInput(dialog, '报价开始', formatDateTime(new Date(now - 60 * 60 * 1000)))
  await fillFormInput(dialog, '报价截止', formatDateTime(new Date(now + 24 * 60 * 60 * 1000)))
  await fillFormInput(dialog, '发布附件', 'http://127.0.0.1:9000/yudao/srm/non-bidding/t3-publish.pdf')
  await fillFormInput(dialog, '供应商范围', String(SUPPLIER_ID))
  const publishPromise = waitForApiSuccess(page, '/srm/non-bidding-project/publish', 'POST', '发布非招标项目')
  await dialog.getByRole('button', { name: '发布' }).click()
  const payload = await publishPromise
  await expect(page.getByText('非招标项目已发布')).toBeVisible()
  expect(payload.data.projectStatus).toBe('PUBLISHED')
  expect(payload.data.supplierScopes.length).toBeGreaterThan(0)
  return payload.data
}

async function quoteProject(page: Page, title: string) {
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/non-bidding-project/page', '查询非招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '报价' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '提交供应商报价' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '供应商ID', String(SUPPLIER_ID))
  await fillFormInput(dialog, '报价金额', '1180')
  await fillFormInput(dialog, '报价附件', 'http://127.0.0.1:9000/yudao/srm/non-bidding/t3-quote.pdf')
  const rowLine = dialog.locator('.el-table__body-wrapper tbody tr').first()
  await rowLine.waitFor({ state: 'visible', timeout: 30000 })
  const unitPriceInput = rowLine.locator('input').nth(0)
  await unitPriceInput.fill('')
  await unitPriceInput.fill('118')
  const quotePromise = waitForApiSuccess(page, '/srm/non-bidding-project/quote', 'POST', '提交报价')
  await dialog.getByRole('button', { name: '提交报价' }).click()
  const payload = await quotePromise
  await expect(page.getByText('供应商报价已提交')).toBeVisible()
  expect(payload.data.quotes.length).toBeGreaterThan(0)
  return payload.data
}

async function dealProject(page: Page, title: string) {
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/non-bidding-project/page', '查询非招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '成交' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '确认成交' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormTextarea(dialog, '成交说明', `T3 E2E 确认成交 ${title}`)
  const dealPromise = waitForApiSuccess(page, '/srm/non-bidding-project/deal', 'POST', '确认成交')
  await dialog.getByRole('button', { name: '确认成交' }).click()
  const payload = await dealPromise
  await expect(page.getByText('非招标项目已确认成交')).toBeVisible()
  expect(payload.data.projectStatus).toBe('DEAL_CONFIRMED')
  expect(payload.data.dealSupplierId).toBe(SUPPLIER_ID)
  expect(payload.data.contractId ?? null).toBe(null)
  return payload.data
}

async function verifyProjectInsights(page: Page, title: string, projectNo: string) {
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/non-bidding-project/page', '查询非招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '详情' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: title }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await expect(dialog.getByText('比价汇总')).toBeVisible()
  await expect(dialog.getByText('价格趋势')).toBeVisible()
  await expect(dialog.getByText('报价供应商数')).toBeVisible()
  await expect(dialog.getByText('最低报价供应商')).toBeVisible()
  await expect(dialog.locator('.el-table').filter({ hasText: '排名' }).getByRole('cell', { name: '1' }).first()).toBeVisible()
  await expect(dialog.locator('.el-table').filter({ hasText: '供应商ID' }).getByText(String(SUPPLIER_ID))).toBeVisible()
  await expect(dialog.getByText(`T3非招标物料-${title}`).first()).toBeVisible()
  expect(await dialog.getByText(projectNo).count()).toBeGreaterThan(1)
  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden({ timeout: 30000 })
}

async function verifyContractableDialog(page: Page, title: string) {
  const responsePromise = waitForApiSuccess(page, '/srm/non-bidding-project/contractable-page', 'GET', '查询可建合同来源')
  await page.getByRole('button', { name: '可建合同来源' }).click()
  await responsePromise
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '可建合同非招标来源' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await expect(dialog.getByText(title).first()).toBeVisible()
}

test('non-bidding publish quote deal and contractable real flow', async ({ page }) => {
  const unique = `SRM-T3-E2E-${Date.now()}`
  const projectTitle = `${unique}-非招标项目`
  const authContext = await login(page)

  const eligibility = await apiGetJson(page, `/admin-api/srm/supplier-access/check?supplierId=${SUPPLIER_ID}`, authContext)
  expect(eligibility.data.eligible, 'supplier must be approved and risk-free before T3 E2E writes').toBe(true)

  const generatedProject = await createApprovedNonBiddingProject(page, projectTitle)
  expect(generatedProject.projectTitle).toBe(projectTitle)
  await publishProject(page, projectTitle)
  await quoteProject(page, projectTitle)
  const dealtProject = await dealProject(page, projectTitle)
  await verifyProjectInsights(page, projectTitle, dealtProject.projectNo)
  await verifyContractableDialog(page, projectTitle)

  const finalProject = await apiGetJson(
    page,
    `/admin-api/srm/non-bidding-project/get?id=${dealtProject.id}`,
    authContext
  )
  expect(finalProject.data.projectStatus).toBe('DEAL_CONFIRMED')
  expect(finalProject.data.dealSupplierId).toBe(SUPPLIER_ID)
  expect(finalProject.data.quotes.length).toBe(1)
  expect(finalProject.data.supplierScopes.length).toBe(1)
  expect(finalProject.data.contractId ?? null).toBe(null)
  expect(finalProject.data.comparisonSummary?.supplierQuoteCount).toBe(1)
  expect((finalProject.data.priceTrends || []).length).toBeGreaterThan(0)
})
