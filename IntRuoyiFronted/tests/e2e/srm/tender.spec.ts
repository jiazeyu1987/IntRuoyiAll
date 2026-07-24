import { expect, test, type Locator, type Page } from 'playwright/test'

const BASE_URL = process.env.SRM_E2E_BASE_URL || 'http://localhost:8093'
const TENANT_NAME = process.env.SRM_E2E_TENANT || '测试租户'
const USERNAME = process.env.SRM_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.SRM_E2E_PASSWORD || '111111'
const SUPPLIER_ID = Number(process.env.SRM_E2E_SUPPLIER_ID || '103')

test.describe.configure({ mode: 'serial' })
test.setTimeout(300000)

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
  'srm:tender-project:query',
  'srm:tender-project:publish',
  'srm:tender-project:submit-bid',
  'srm:tender-project:expert',
  'srm:tender-project:committee',
  'srm:tender-project:candidate',
  'srm:tender-project:winning'
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
  expect(menus.some((menu) => menu?.component === 'srm/tender-project/index')).toBe(true)
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
  let leaf = page.getByText(leafLabel, { exact: true }).locator('xpath=ancestor::*[contains(@class,"el-menu-item")][1]').first()
  for (const label of labels.slice(0, -1)) {
    if (await leaf.isVisible().catch(() => false)) {
      break
    }
    const parent = page.locator('.el-sub-menu__title:visible').filter({ hasText: label }).first()
    await parent.waitFor({ state: 'visible', timeout: 30000 })
    await parent.click()
    await page.waitForTimeout(500)
    leaf = page.getByText(leafLabel, { exact: true }).locator('xpath=ancestor::*[contains(@class,"el-menu-item")][1]').first()
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

async function createTenderProjectFromProcurementPlan(page: Page, title: string) {
  await openMenuPath(page, ['SRM', '采购计划'], '/srm/procurement-plan')
  await page.getByRole('button', { name: '新增计划' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增采购计划' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '计划标题', title)
  await fillFormInput(dialog, '预计金额', '23888.5')
  await fillFormTextarea(dialog, '备注', `T4真实E2E招标采购计划 ${title}`)
  await fillFirstLineInputs(dialog, [`TDR-${Date.now()}`, `T4招标物料-${title}`, '4001', '8', '件', '2026-07-01'])
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
  await auditDialog.locator('textarea').fill(`T4真实E2E审核通过 ${title}`)
  await auditDialog.getByRole('button', { name: '提交' }).click()
  await approvePromise
  await searchByPlaceholder(page, '请输入计划标题', title, '/srm/procurement-plan/page', '查询采购计划')

  row = await tableRow(page, title)
  const generatePromise = waitForApiSuccess(page, '/srm/procurement-plan/generate-sourcing', 'POST', '生成招标项目')
  await row.getByRole('button', { name: '生成项目' }).click()
  await page.locator('.el-dropdown-menu:visible').getByRole('menuitem', { name: '招标项目', exact: true }).click()
  const payload = await generatePromise
  expect(payload.data.projectType).toBe('TENDER')
  return payload.data
}

async function publishTender(page: Page, title: string) {
  await openMenuPath(page, ['SRM', '招标项目'], '/srm/tender-project')
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '发布' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '发布招标项目' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const now = Date.now()
  await fillFormInput(dialog, '公告标题', `${title}公告`)
  await fillFormInput(dialog, '公告附件', 'http://127.0.0.1:9000/yudao/srm/tender/t4-notice.pdf')
  await fillFormInput(dialog, '标书名称', `${title}标书`)
  await fillFormInput(dialog, '标书附件', 'http://127.0.0.1:9000/yudao/srm/tender/t4-document.pdf')
  await fillFormInput(dialog, '投标开始', formatDateTime(new Date(now - 60 * 60 * 1000)))
  await fillFormInput(dialog, '投标截止', formatDateTime(new Date(now + 24 * 60 * 60 * 1000)))
  const publishPromise = waitForApiSuccess(page, '/srm/tender-project/publish', 'POST', '发布招标项目')
  await dialog.getByRole('button', { name: '发布' }).click()
  const payload = await publishPromise
  await expect(page.getByText('招标项目已发布')).toBeVisible()
  expect(payload.data.projectStatus).toBe('PUBLISHED')
  return payload.data
}

async function submitTenderBid(page: Page, title: string) {
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '投标' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '提交供应商投标' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '供应商ID', String(SUPPLIER_ID))
  await fillFormInput(dialog, '投标金额', '2180')
  await fillFormInput(dialog, '投标附件', 'http://127.0.0.1:9000/yudao/srm/tender/t4-bid.pdf')
  const bidPromise = waitForApiSuccess(page, '/srm/tender-project/submit-bid', 'POST', '提交供应商投标')
  await dialog.getByRole('button', { name: '提交投标' }).click()
  const payload = await bidPromise
  await expect(page.getByText('供应商投标已提交')).toBeVisible()
  expect(payload.data.submissions.length).toBeGreaterThan(0)
  return payload.data
}

async function createApprovedExperts(page: Page, title: string) {
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '专家' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '创建并通过招标专家' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '专家姓名', `${title}专家一,${title}专家二`)
  await fillFormInput(dialog, '专业类型', '医疗耗材')
  await fillFormTextarea(dialog, '审核意见', `T4 E2E 专家审核通过 ${title}`)
  const createResponses: Promise<any>[] = []
  page.on('response', async (response) => {
    if (response.url().includes('/srm/tender-project/expert/create') && response.request().method() === 'POST') {
      createResponses.push(response.json().catch(() => null))
    }
  })
  const approveWaits = [
    waitForApiSuccess(page, '/srm/tender-project/expert/approve', 'PUT', '通过招标专家1'),
    waitForApiSuccess(page, '/srm/tender-project/expert/approve', 'PUT', '通过招标专家2')
  ]
  await dialog.getByRole('button', { name: '创建并通过' }).click()
  await Promise.all(approveWaits)
  await expect(page.getByText('招标专家已创建并审核通过')).toBeVisible()
  const expertIds: number[] = []
  for (const responsePromise of createResponses) {
    const payload = await responsePromise
    if (isSuccessPayload(payload)) {
      expertIds.push(payload.data)
    }
  }
  if (expertIds.length < 2) {
    const cells = await dialog.locator('.el-table__body-wrapper tbody tr td:first-child').allTextContents()
    for (const cell of cells) {
      const id = Number(cell.trim())
      if (Number.isFinite(id) && id > 0) {
        expertIds.push(id)
      }
    }
  }
  expect(expertIds.length, 'created expert ids should be visible and real').toBeGreaterThanOrEqual(2)
  await dialog.getByRole('button', { name: '取消' }).click()
  return expertIds.slice(0, 2)
}

async function formTenderCommittee(page: Page, title: string, expertIds: number[]) {
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '评委会' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '组建评标委员会' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '要求专业类型', '医疗耗材')
  await fillFormInput(dialog, '要求专家人数', String(expertIds.length))
  await fillFormInput(dialog, '专家ID列表', expertIds.join(','))
  const committeePromise = waitForApiSuccess(page, '/srm/tender-project/committee', 'POST', '组建评标委员会')
  await dialog.getByRole('button', { name: '组建评委会' }).click()
  const payload = await committeePromise
  await expect(page.getByText('评标委员会已组建')).toBeVisible()
  expect(payload.data.projectStatus).toBe('COMMITTEE_CONFIRMED')
  expect(payload.data.committeeMembers.length).toBe(expertIds.length)
  return payload.data
}

async function createTenderCandidates(page: Page, title: string) {
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '候选' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '生成中标候选' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const candidatePromise = waitForApiSuccess(page, '/srm/tender-project/candidate', 'POST', '生成中标候选')
  await dialog.getByRole('button', { name: '生成候选' }).click()
  const payload = await candidatePromise
  await expect(page.getByText('中标候选已生成')).toBeVisible()
  expect(payload.data.projectStatus).toBe('CANDIDATE_CONFIRMED')
  expect(payload.data.candidates.length).toBeGreaterThan(0)
  return payload.data
}

async function confirmTenderWinning(page: Page, title: string) {
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  const row = await tableRow(page, title)
  await row.getByRole('button', { name: '中标' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '确认中标结果' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormTextarea(dialog, '中标说明', `T4 E2E 确认中标 ${title}`)
  const winningPromise = waitForApiSuccess(page, '/srm/tender-project/winning', 'POST', '确认中标结果')
  await dialog.getByRole('button', { name: '确认中标' }).click()
  const payload = await winningPromise
  await expect(page.getByText('中标结果已确认')).toBeVisible()
  expect(payload.data.projectStatus).toBe('WINNING_CONFIRMED')
  expect(payload.data.dealSupplierId).toBe(SUPPLIER_ID)
  expect(payload.data.contractId ?? null).toBe(null)
  return payload.data
}

test('tender publish bid expert committee candidate and winning real flow', async ({ page }) => {
  const unique = `SRM-T4-E2E-${Date.now()}`
  const projectTitle = `${unique}-招标项目`
  const authContext = await login(page)

  const eligibility = await apiGetJson(page, `/admin-api/srm/supplier-access/check?supplierId=${SUPPLIER_ID}`, authContext)
  expect(eligibility.data.eligible, 'supplier must be approved and risk-free before T4 E2E writes').toBe(true)

  const tenderProject = await createTenderProjectFromProcurementPlan(page, projectTitle)
  expect(tenderProject.projectTitle).toBe(projectTitle)
  await publishTender(page, projectTitle)
  await submitTenderBid(page, projectTitle)
  const expertIds = await createApprovedExperts(page, projectTitle)
  await formTenderCommittee(page, projectTitle, expertIds)
  await createTenderCandidates(page, projectTitle)
  const winningProject = await confirmTenderWinning(page, projectTitle)

  const finalProject = await apiGetJson(
    page,
    `/admin-api/srm/tender-project/get?id=${winningProject.id}`,
    authContext
  )
  expect(finalProject.data.projectStatus).toBe('WINNING_CONFIRMED')
  expect(finalProject.data.dealSupplierId).toBe(SUPPLIER_ID)
  expect(finalProject.data.submissions.length).toBe(1)
  expect(finalProject.data.committeeMembers.length).toBe(expertIds.length)
  expect(finalProject.data.candidates.length).toBe(1)
  expect(finalProject.data.winningResult.supplierId).toBe(SUPPLIER_ID)
  expect(finalProject.data.contractId ?? null).toBe(null)
})
