import { expect, test, type Locator, type Page } from 'playwright/test'

const BASE_URL = process.env.SRM_E2E_BASE_URL || 'http://localhost:8093'
const TENANT_NAME = process.env.SRM_E2E_TENANT || '测试租户'
const USERNAME = process.env.SRM_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.SRM_E2E_PASSWORD || '111111'
const SUPPLIER_ID = Number(process.env.SRM_E2E_SUPPLIER_ID || '103')
const SUPPLIER_NAME = process.env.SRM_E2E_SUPPLIER_NAME || '山东瑛泰医疗器械有限公司'
const SUPPLIER_KEYWORD = process.env.SRM_E2E_SUPPLIER_KEYWORD || '瑛泰'

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
  'srm:framework-plan:query',
  'srm:framework-plan:create',
  'srm:framework-plan:submit',
  'srm:framework-plan:audit',
  'srm:framework-plan:agreement'
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
  expect(menus.some((menu) => menu?.component === 'srm/procurement-plan/index')).toBe(true)
  expect(menus.some((menu) => menu?.component === 'srm/framework-plan/index')).toBe(true)
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

async function fillFormNumber(container: Locator, labelText: string, value: number) {
  await fillFormInput(container, labelText, String(value))
}

async function fillDateField(container: Locator, labelText: string, value: string) {
  const input = formField(container, labelText).locator('input').last()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill('')
  await input.fill(value)
  await input.press('Enter')
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

async function searchProcurementPlan(page: Page, title: string) {
  await page.locator('input[placeholder="请输入计划标题"]').first().fill('')
  const responsePromise = waitForApiSuccess(page, '/srm/procurement-plan/page', 'GET', '查询采购计划')
  await page.locator('input[placeholder="请输入计划标题"]').first().fill(title)
  await page.getByRole('button', { name: '搜索' }).click()
  await responsePromise
}

async function searchFrameworkPlan(page: Page, supplierName: string) {
  await page.locator('input[placeholder="请输入供应商名称"]').first().fill('')
  const responsePromise = waitForApiSuccess(page, '/srm/framework-plan/page', 'GET', '查询框架计划')
  await page.locator('input[placeholder="请输入供应商名称"]').first().fill(supplierName)
  await page.getByRole('button', { name: '搜索' }).click()
  await responsePromise
}

async function procurementRow(page: Page, title: string) {
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: title }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function frameworkRow(page: Page, title: string) {
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: title }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function createProcurementPlan(page: Page, title: string) {
  await page.getByRole('button', { name: '新增计划' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增采购计划' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '计划标题', title)
  await fillFormNumber(dialog, '预计金额', 12888.5)
  await fillFormTextarea(dialog, '备注', `T2真实E2E采购计划 ${title}`)
  await fillFirstLineInputs(dialog, [`MAT-${Date.now()}`, `T2采购物料-${title}`, '1001', '12', '件', '2026-07-01'])
  const createPromise = waitForApiSuccess(page, '/srm/procurement-plan/create', 'POST', '新增采购计划')
  await dialog.getByRole('button', { name: '保存' }).click()
  await createPromise
  await expect(page.getByText('采购计划已保存')).toBeVisible()
  await searchProcurementPlan(page, title)
}

async function approveProcurementPlanAndGenerate(page: Page, title: string) {
  let row = await procurementRow(page, title)
  const submitPromise = waitForApiSuccess(page, '/srm/procurement-plan/submit', 'PUT', '提交采购计划')
  await row.getByRole('button', { name: '提交' }).click()
  await submitPromise
  await expect(page.getByText('采购计划已提交')).toBeVisible()
  await searchProcurementPlan(page, title)

  row = await procurementRow(page, title)
  const approvePromise = waitForApiSuccess(page, '/srm/procurement-plan/approve', 'PUT', '通过采购计划')
  await row.getByRole('button', { name: '通过' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '通过采购计划' }).last()
  await dialog.locator('textarea').fill(`T2真实E2E审核通过 ${title}`)
  await dialog.getByRole('button', { name: '提交' }).click()
  await approvePromise
  await expect(page.getByText('采购计划已通过')).toBeVisible()
  await searchProcurementPlan(page, title)

  row = await procurementRow(page, title)
  const generatePromise = waitForApiSuccess(page, '/srm/procurement-plan/generate-sourcing', 'POST', '生成寻源项目')
  await row.getByRole('button', { name: '生成项目' }).click()
  await page.locator('.el-dropdown-menu:visible').getByText('非招标项目').click()
  const generatePayload = await generatePromise
  await expect(page.getByText(/已生成非招标/)).toBeVisible()
  await searchProcurementPlan(page, title)
  return generatePayload.data
}

async function chooseSupplier(page: Page, dialog: Locator) {
  const input = formField(dialog, '合格供应商').locator('.el-select input[role="combobox"], input.el-select__input').first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.click()
  await input.fill('')
  await input.fill(SUPPLIER_KEYWORD)
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({
    hasText: `${SUPPLIER_NAME} (#${SUPPLIER_ID})`
  }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function createFrameworkPlan(page: Page, title: string) {
  await page.getByRole('button', { name: '新增框架计划' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增框架计划' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '计划标题', title)
  await chooseSupplier(page, dialog)
  await fillFormNumber(dialog, '预算金额', 46666)
  await fillDateField(dialog, '开始日期', '2026-07-01')
  await fillDateField(dialog, '结束日期', '2026-12-31')
  await fillFirstLineInputs(dialog, [`FW-${Date.now()}`, `T2框架物料-${title}`, '2001', '24', '件', '46666'])
  const createPromise = waitForApiSuccess(page, '/srm/framework-plan/create', 'POST', '新增框架计划')
  await dialog.getByRole('button', { name: '保存' }).click()
  await createPromise
  await expect(page.getByText('框架计划已保存')).toBeVisible()
  await searchFrameworkPlan(page, SUPPLIER_NAME)
}

async function approveFrameworkPlanAndAgreement(page: Page, title: string) {
  let row = await frameworkRow(page, title)
  const submitPromise = waitForApiSuccess(page, '/srm/framework-plan/submit', 'PUT', '提交框架计划')
  await row.getByRole('button', { name: '提交' }).click()
  await submitPromise
  await expect(page.getByText('框架计划已提交')).toBeVisible()
  await searchFrameworkPlan(page, SUPPLIER_NAME)

  row = await frameworkRow(page, title)
  const approvePromise = waitForApiSuccess(page, '/srm/framework-plan/approve', 'PUT', '通过框架计划')
  await row.getByRole('button', { name: '通过' }).click()
  const auditDialog = page.locator('.el-dialog:visible').filter({ hasText: '通过框架计划' }).last()
  await auditDialog.locator('textarea').fill(`T2真实E2E框架审核通过 ${title}`)
  await auditDialog.getByRole('button', { name: '提交' }).click()
  await approvePromise
  await expect(page.getByText('框架计划已通过')).toBeVisible()
  await searchFrameworkPlan(page, SUPPLIER_NAME)

  row = await frameworkRow(page, title)
  const agreementPromise = waitForApiSuccess(page, '/srm/framework-plan/create-agreement', 'POST', '生成框架协议')
  await row.getByRole('button', { name: '生成协议' }).click()
  const agreementPayload = await agreementPromise
  await expect(page.getByText(/已生成框架协议/)).toBeVisible()
  await searchFrameworkPlan(page, SUPPLIER_NAME)
  return agreementPayload.data
}

test('procurement plan and framework agreement real flow', async ({ page }) => {
  const unique = `SRM-T2-E2E-${Date.now()}`
  const procurementTitle = `${unique}-采购计划`
  const frameworkTitle = `${unique}-框架计划`

  const authContext = await login(page)

  const eligibility = await apiGetJson(page, `/admin-api/srm/supplier-access/check?supplierId=${SUPPLIER_ID}`, authContext)
  expect(eligibility.data.eligible, 'framework supplier must be approved and risk-free before T2 E2E writes').toBe(true)

  await openMenuPath(page, ['SRM', '采购计划'], '/srm/procurement-plan')
  await expect(page.getByRole('button', { name: '新增计划' })).toBeVisible()
  await createProcurementPlan(page, procurementTitle)
  const generatedProject = await approveProcurementPlanAndGenerate(page, procurementTitle)
  expect(generatedProject.projectType).toBe('NON_BIDDING')
  expect(generatedProject.sourcePlanNo).not.toBe('')
  expect(generatedProject.lines.length).toBeGreaterThan(0)

  await openMenuPath(page, ['SRM', '框架计划'], '/srm/framework-plan')
  await expect(page.getByRole('button', { name: '新增框架计划' })).toBeVisible()
  await createFrameworkPlan(page, frameworkTitle)
  const agreement = await approveFrameworkPlanAndAgreement(page, frameworkTitle)
  expect(agreement.frameworkPlanNo).not.toBe('')
  expect(agreement.agreementNo).not.toBe('')
  expect(agreement.lines.length).toBeGreaterThan(0)

  const procurementFinal = await apiGetJson(
    page,
    `/admin-api/srm/procurement-plan/page?pageNo=1&pageSize=10&planTitle=${encodeURIComponent(procurementTitle)}`,
    authContext
  )
  const procurementRecord = procurementFinal.data.list.find((item: any) => item.planTitle === procurementTitle)
  expect(procurementRecord.planStatus).toBe('GENERATED')
  expect(procurementRecord.generatedProjectNo).toBe(generatedProject.projectNo)

  const frameworkFinal = await apiGetJson(
    page,
    `/admin-api/srm/framework-plan/page?pageNo=1&pageSize=10&supplierName=${encodeURIComponent(SUPPLIER_NAME)}`,
    authContext
  )
  const frameworkRecord = frameworkFinal.data.list.find((item: any) => item.planTitle === frameworkTitle)
  expect(frameworkRecord.planStatus).toBe('AGREEMENT_CREATED')
  expect(frameworkRecord.agreementNo).toBe(agreement.agreementNo)

  const agreementFinal = await apiGetJson(
    page,
    `/admin-api/srm/framework-plan/agreement-page?pageNo=1&pageSize=20&frameworkPlanNo=${encodeURIComponent(agreement.frameworkPlanNo)}`,
    authContext
  )
  const agreementRecord = agreementFinal.data.list.find((item: any) => item.agreementNo === agreement.agreementNo)
  expect(agreementRecord.agreementStatus).toBe('EFFECTIVE')
  expect(agreementRecord.supplierId).toBe(SUPPLIER_ID)
})
