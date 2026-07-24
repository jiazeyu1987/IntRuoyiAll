import { expect, test, type Locator, type Page } from 'playwright/test'

const BASE_URL = process.env.SRM_E2E_BASE_URL || 'http://localhost:8093'
const TENANT_NAME = process.env.SRM_E2E_TENANT || '测试租户'
const CREATOR_USERNAME = process.env.SRM_E2E_USERNAME || 'aoteman'
const CREATOR_PASSWORD = process.env.SRM_E2E_PASSWORD || '111111'
const APPROVER_USERNAME = process.env.SRM_E2E_APPROVER_USERNAME || 'showroomsupervisor'
const APPROVER_PASSWORD = process.env.SRM_E2E_APPROVER_PASSWORD || '111111'
const SUPPLIER_ID = Number(process.env.SRM_E2E_SUPPLIER_ID || '103')
const SUPPLIER_NAME =
  process.env.SRM_E2E_SUPPLIER_NAME || '山东瑛泰医疗器械有限公司'

test.describe.configure({ mode: 'serial' })
test.setTimeout(240000)

const waitForApi = (page: Page, fragment: string) =>
  page.waitForResponse((response) => response.url().includes(fragment) && response.ok())

const supplierAccessRoute = '/srm/supplier/access'
const supplierRiskRoute = '/srm/supplier/risk'

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

async function selectReferenceSupplier(page: Page, container: Locator) {
  const input = container
    .locator('.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入供应商名称检索"]')
    .first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.click()
  await input.fill('')
  await input.fill(SUPPLIER_NAME)
  await page.getByRole('option', { name: new RegExp(`${SUPPLIER_NAME}.*#${SUPPLIER_ID}`) }).click()
}

async function login(page: Page, username: string, password: string) {
  expect(TENANT_NAME).toBe('测试租户')
  expect(CREATOR_USERNAME).toBe('aoteman')

  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(supplierAccessRoute)}`, {
    waitUntil: 'domcontentloaded'
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(supplierAccessRoute)}`, {
    waitUntil: 'domcontentloaded'
  })
  await settle(page)
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, loginForm)
  const textboxes = loginForm.getByRole('textbox')
  const textboxCount = await textboxes.count()
  if (textboxCount >= 2) {
    await textboxes.nth(textboxCount >= 3 ? 1 : 0).fill('')
    await textboxes.nth(textboxCount >= 3 ? 1 : 0).fill(username)
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, 'username')
  }
  await fillFirstVisible(
    loginForm.locator('input[type="password"], input[placeholder="请输入密码"]'),
    password,
    'password'
  )
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    loginForm.getByRole('button', { name: '登录' }).click()
  ])
  const permissionResponse = await page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.waitForURL((url) => !url.pathname.startsWith('/login'))
  const headers = permissionResponse.request().headers()
  return {
    authorization: headers.authorization || headers.Authorization || '',
    tenantId: headers['tenant-id'] || headers['Tenant-Id'] || ''
  }
}

async function gotoRoute(page: Page, route: string, apiFragment: string, readyText: string) {
  await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' })
  await waitForApi(page, apiFragment)
  await expect(page.getByText(readyText)).toBeVisible()
}

async function searchAccess(page: Page) {
  const responsePromise = waitForApi(page, '/srm/supplier-access/page')
  await page.locator('input[placeholder="请输入供应商名称"]').first().fill(SUPPLIER_NAME)
  await page.getByRole('button', { name: '搜索' }).click()
  await responsePromise
}

async function searchRisk(page: Page) {
  const responsePromise = waitForApi(page, '/srm/supplier-risk/page')
  await page.locator('input[placeholder="请输入供应商名称"]').first().fill(SUPPLIER_NAME)
  await page.getByRole('button', { name: '搜索' }).click()
  await responsePromise
}

async function accessRow(page: Page) {
  return page.locator('.el-table__row').filter({ hasText: SUPPLIER_NAME }).first()
}

async function riskRow(page: Page, extraText?: string) {
  const locator = page.locator('.el-table__row').filter({ hasText: SUPPLIER_NAME })
  return extraText ? locator.filter({ hasText: extraText }).first() : locator.first()
}

async function createAccessIfMissing(page: Page) {
  await gotoRoute(page, supplierAccessRoute, '/srm/supplier-access/page', '新增准入')
  await searchAccess(page)
  const rows = page.locator('.el-table__row').filter({ hasText: SUPPLIER_NAME })
  if ((await rows.count()) > 0) {
    return
  }

  await page.getByRole('button', { name: '新增准入' }).click()
  const dialog = page.locator('.el-dialog').filter({ hasText: '新增供应商准入' }).last()
  await expect(dialog).toBeVisible()
  await selectReferenceSupplier(page, dialog)
  await dialog
    .locator('textarea')
    .fill(`T1 E2E 准入申请 ${new Date().toISOString()}`)
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/srm/supplier-access/create')),
    dialog.getByRole('button', { name: '保存' }).click()
  ])
  await expect(page.getByText('供应商准入档案已新增')).toBeVisible()
  await searchAccess(page)
}

async function ensureApprovedAndEnabled(page: Page) {
  await gotoRoute(page, supplierAccessRoute, '/srm/supplier-access/page', '新增准入')
  await searchAccess(page)
  const row = await accessRow(page)
  await expect(row).toBeVisible()

  if ((await row.textContent())?.includes('已通过') !== true) {
    await row.getByRole('button', { name: '通过' }).click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '通过准入审核' }).last()
    await dialog.locator('textarea').fill('T1 E2E 审核通过')
    await Promise.all([
      page.waitForResponse((response) => response.url().includes('/srm/supplier-access/approve')),
      dialog.getByRole('button', { name: '提交' }).click()
    ])
    await expect(page.getByText('供应商准入已通过')).toBeVisible()
    await searchAccess(page)
  }

  const refreshedRow = await accessRow(page)
  const switchLocator = refreshedRow.locator('[role="switch"]').first()
  if ((await switchLocator.getAttribute('aria-checked')) !== 'true') {
    await switchLocator.click()
    await waitForApi(page, '/srm/supplier-access/enable')
    await expect(page.getByText('供应商准入已启用')).toBeVisible()
    await searchAccess(page)
  }
}

async function openEligibilityDialogFromRow(page: Page) {
  await gotoRoute(page, supplierAccessRoute, '/srm/supplier-access/page', '新增准入')
  await searchAccess(page)
  const row = await accessRow(page)
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: '校验' }).click()
  const dialog = page.locator('.el-dialog').filter({ hasText: '供应商资格校验' }).last()
  await expect(dialog).toBeVisible()
  return dialog
}

async function runEligibilityCheck(dialog: Locator, page: Page) {
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/srm/supplier-access/check')),
    dialog.getByRole('button', { name: '执行校验' }).click()
  ])
}

async function closeLatestDialog(page: Page) {
  await page.locator('.el-dialog').last().getByRole('button', { name: '关闭' }).click()
}

async function resolveExistingOpenRisks(page: Page) {
  await gotoRoute(page, supplierRiskRoute, '/srm/supplier-risk/page', '新增风险')
  while (true) {
    await searchRisk(page)
    const rows = page.locator('.el-table__row').filter({ hasText: SUPPLIER_NAME })
    const count = await rows.count()
    let targetRow: Locator | undefined
    for (let index = 0; index < count; index += 1) {
      const currentRow = rows.nth(index)
      const handleButton = currentRow.getByRole('button', { name: '处理' })
      if ((await handleButton.count()) > 0 && (await handleButton.isEnabled())) {
        targetRow = currentRow
        break
      }
    }
    if (!targetRow) {
      return
    }
    await targetRow.getByRole('button', { name: '处理' }).click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '处理供应商风险' }).last()
    await dialog.locator('textarea').last().fill('T1 E2E 预清理历史未处理风险')
    await Promise.all([
      page.waitForResponse((response) => response.url().includes('/srm/supplier-risk/resolve')),
      dialog.getByRole('button', { name: '提交' }).click()
    ])
    await expect(page.getByText('供应商风险已处理')).toBeVisible()
  }
}

async function createOpenHighRisk(page: Page, sourceCode: string) {
  await gotoRoute(page, supplierRiskRoute, '/srm/supplier-risk/page', '新增风险')
  await page.getByRole('button', { name: '新增风险' }).click()
  const dialog = page.locator('.el-dialog').filter({ hasText: '新增供应商风险' }).last()
  await selectReferenceSupplier(page, dialog)
  await dialog.locator('.el-form-item').filter({ hasText: '来源编码' }).locator('input').fill(sourceCode)
  await dialog
    .locator('.el-form-item')
    .filter({ hasText: '来源名称' })
    .locator('input')
    .fill(`准入申请-${sourceCode}`)
  const textareas = dialog.locator('textarea')
  await textareas.nth(0).fill(`T1 E2E 未处理高风险阻断 ${sourceCode}`)
  await textareas.nth(1).fill('等待补齐资质文件并复核')
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/srm/supplier-risk/create')),
    dialog.getByRole('button', { name: '保存' }).click()
  ])
  await expect(page.getByText('供应商风险记录已新增')).toBeVisible()
}

async function resolveRiskBySourceCode(page: Page, sourceCode: string) {
  await gotoRoute(page, supplierRiskRoute, '/srm/supplier-risk/page', '新增风险')
  await searchRisk(page)
  const row = await riskRow(page, sourceCode)
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: '处理' }).click()
  const dialog = page.locator('.el-dialog').filter({ hasText: '处理供应商风险' }).last()
  await dialog
    .locator('textarea')
    .last()
    .fill(`T1 E2E 风险关闭 ${sourceCode}`)
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/srm/supplier-risk/resolve')),
    dialog.getByRole('button', { name: '提交' }).click()
  ])
  await expect(page.getByText('供应商风险已处理')).toBeVisible()
}

async function verifyByApi(page: Page, sourceCode: string, authContext: { authorization: string; tenantId: string }) {
  const result = await page.evaluate(
    async ({ supplierId, supplierName, sourceCode, authorization, tenantId }) => {
      if (!authorization || !tenantId) {
        throw new Error('缺少权限请求头中的 authorization 或 tenant-id，无法做最终 API 校验。')
      }
      const headers = {
        Authorization: authorization,
        'tenant-id': tenantId,
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
      const [accessResp, riskResp, checkResp] = await Promise.all([
        fetch(`/admin-api/srm/supplier-access/page?pageNo=1&pageSize=20&supplierName=${encodeURIComponent(supplierName)}`, {
          headers
        }),
        fetch(`/admin-api/srm/supplier-risk/page?pageNo=1&pageSize=20&supplierName=${encodeURIComponent(supplierName)}`, {
          headers
        }),
        fetch(`/admin-api/srm/supplier-access/check?supplierId=${supplierId}`, { headers })
      ])
      const accessData = await accessResp.json()
      const riskData = await riskResp.json()
      const checkData = await checkResp.json()
      return { accessData, riskData, checkData, sourceCode }
    },
    {
      supplierId: SUPPLIER_ID,
      supplierName: SUPPLIER_NAME,
      sourceCode,
      authorization: authContext.authorization,
      tenantId: authContext.tenantId
    }
  )

  expect(result.accessData.code).toBe(0)
  expect(result.riskData.code).toBe(0)
  expect(result.checkData.code).toBe(0)
  const accessRow = result.accessData.data.list.find(
    (item: any) => Number(item.supplierId) === SUPPLIER_ID
  )
  expect(accessRow.accessStatus).toBe('APPROVED')
  expect(accessRow.enabled).toBe(true)
  const riskRow = result.riskData.data.list.find((item: any) => item.sourceCode === sourceCode)
  expect(riskRow.riskStatus).toBe('RESOLVED')
  expect(result.checkData.data.eligible).toBe(true)
  expect(result.checkData.data.openHighRiskCount).toBe(0)
}

test('supplier access and risk gate real flow', async ({ page }) => {
  const sourceCode = `SRM-T1-E2E-${Date.now()}`

  await login(page, CREATOR_USERNAME, CREATOR_PASSWORD)
  await createAccessIfMissing(page)
  const approverAuth = await login(page, APPROVER_USERNAME, APPROVER_PASSWORD)
  await ensureApprovedAndEnabled(page)
  await resolveExistingOpenRisks(page)

  const readyDialog = await openEligibilityDialogFromRow(page)
  await runEligibilityCheck(readyDialog, page)
  await expect(readyDialog.getByText('校验通过')).toBeVisible()
  await closeLatestDialog(page)

  await createOpenHighRisk(page, sourceCode)

  const blockedDialog = await openEligibilityDialogFromRow(page)
  await runEligibilityCheck(blockedDialog, page)
  await expect(blockedDialog.getByText('已阻断')).toBeVisible()
  await expect(blockedDialog.getByText(sourceCode).first()).toBeVisible()
  await closeLatestDialog(page)

  await resolveRiskBySourceCode(page, sourceCode)
  await resolveExistingOpenRisks(page)

  const restoredDialog = await openEligibilityDialogFromRow(page)
  await runEligibilityCheck(restoredDialog, page)
  await expect(restoredDialog.getByText('校验通过')).toBeVisible()
  await closeLatestDialog(page)

  await verifyByApi(page, sourceCode, approverAuth)
})
