import { expect, test, type Locator, type Page } from 'playwright/test'

const BASE_URL = process.env.SRM_E2E_BASE_URL || 'http://localhost:8093'
const TENANT_NAME = process.env.SRM_E2E_TENANT || '测试租户'
const USERNAME = process.env.SRM_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.SRM_E2E_PASSWORD || '111111'
const SUPPLIER_ID = Number(process.env.SRM_E2E_SUPPLIER_ID || '103')

test.describe.configure({ mode: 'serial' })
test.setTimeout(600000)

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
  'srm:non-bidding-project:contract',
  'srm:tender-project:query',
  'srm:tender-project:publish',
  'srm:tender-project:submit-bid',
  'srm:tender-project:expert',
  'srm:tender-project:committee',
  'srm:tender-project:candidate',
  'srm:tender-project:winning',
  'srm:procurement-contract:query',
  'srm:procurement-contract:create',
  'srm:procurement-contract:cancel',
  'srm:procurement-contract:delete'
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
  expect(menus.some((menu) => menu?.component === 'srm/procurement-contract/index')).toBe(true)
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

async function fillFormSelect(page: Page, container: Locator, labelText: string, optionText: string) {
  const selectWrapper = formField(container, labelText).locator('.el-select__wrapper').last()
  await selectWrapper.waitFor({ state: 'visible', timeout: 30000 })
  await selectWrapper.click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').getByText(optionText, { exact: true }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
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
  await fillFormTextarea(dialog, '备注', `T5真实E2E非招标采购计划 ${title}`)
  await fillFirstLineInputs(dialog, [`CTNB-${Date.now()}`, `T5非招标物料-${title}`, '5001', '10', '件', '2026-07-01'])
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
  await auditDialog.locator('textarea').fill(`T5真实E2E审核通过 ${title}`)
  await auditDialog.getByRole('button', { name: '提交' }).click()
  await approvePromise
  await searchByPlaceholder(page, '请输入计划标题', title, '/srm/procurement-plan/page', '查询采购计划')

  row = await tableRow(page, title)
  const generatePromise = waitForApiSuccess(page, '/srm/procurement-plan/generate-sourcing', 'POST', '生成非招标项目')
  await row.getByRole('button', { name: '生成项目' }).click()
  await page.locator('.el-dropdown-menu:visible').getByRole('menuitem', { name: '非招标项目', exact: true }).click()
  const payload = await generatePromise
  expect(payload.data.projectType).toBe('NON_BIDDING')
  return payload.data
}

async function createContractableNonBiddingSource(page: Page, title: string) {
  const generatedProject = await createApprovedNonBiddingProject(page, title)
  await openMenuPath(page, ['SRM', '非招标项目'], '/srm/non-bidding-project')
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/non-bidding-project/page', '查询非招标项目')

  let row = await tableRow(page, title)
  await row.getByRole('button', { name: '发布' }).click()
  const publishDialog = page.locator('.el-dialog:visible').filter({ hasText: '发布非招标项目' }).last()
  await publishDialog.waitFor({ state: 'visible', timeout: 30000 })
  const now = Date.now()
  await fillFormInput(publishDialog, '报价开始', formatDateTime(new Date(now - 60 * 60 * 1000)))
  await fillFormInput(publishDialog, '报价截止', formatDateTime(new Date(now + 24 * 60 * 60 * 1000)))
  await fillFormInput(publishDialog, '发布附件', 'http://127.0.0.1:9000/yudao/srm/contract/t5-non-bidding-publish.pdf')
  await fillFormInput(publishDialog, '供应商范围', String(SUPPLIER_ID))
  const publishPromise = waitForApiSuccess(page, '/srm/non-bidding-project/publish', 'POST', '发布非招标项目')
  await publishDialog.getByRole('button', { name: '发布' }).click()
  const publishPayload = await publishPromise
  expect(publishPayload.data.projectStatus).toBe('PUBLISHED')

  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/non-bidding-project/page', '查询非招标项目')
  row = await tableRow(page, title)
  await row.getByRole('button', { name: '报价' }).click()
  const quoteDialog = page.locator('.el-dialog:visible').filter({ hasText: '提交供应商报价' }).last()
  await quoteDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(quoteDialog, '供应商ID', String(SUPPLIER_ID))
  await fillFormInput(quoteDialog, '报价金额', '1180')
  await fillFormInput(quoteDialog, '报价附件', 'http://127.0.0.1:9000/yudao/srm/contract/t5-non-bidding-quote.pdf')
  const quoteLine = quoteDialog.locator('.el-table__body-wrapper tbody tr').first()
  await quoteLine.waitFor({ state: 'visible', timeout: 30000 })
  await quoteLine.locator('input').nth(0).fill('118')
  const quotePromise = waitForApiSuccess(page, '/srm/non-bidding-project/quote', 'POST', '提交报价')
  await quoteDialog.getByRole('button', { name: '提交报价' }).click()
  await quotePromise

  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/non-bidding-project/page', '查询非招标项目')
  row = await tableRow(page, title)
  await row.getByRole('button', { name: '成交' }).click()
  const dealDialog = page.locator('.el-dialog:visible').filter({ hasText: '确认成交' }).last()
  await dealDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormTextarea(dealDialog, '成交说明', `T5 E2E 确认成交 ${title}`)
  const dealPromise = waitForApiSuccess(page, '/srm/non-bidding-project/deal', 'POST', '确认成交')
  await dealDialog.getByRole('button', { name: '确认成交' }).click()
  const dealPayload = await dealPromise
  expect(dealPayload.data.id).toBe(generatedProject.id)
  expect(dealPayload.data.projectStatus).toBe('DEAL_CONFIRMED')
  expect(dealPayload.data.contractId ?? null).toBe(null)
  return dealPayload.data
}

async function createTenderProjectFromProcurementPlan(page: Page, title: string) {
  await openMenuPath(page, ['SRM', '采购计划'], '/srm/procurement-plan')
  await page.getByRole('button', { name: '新增计划' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增采购计划' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(dialog, '计划标题', title)
  await fillFormInput(dialog, '预计金额', '23888.5')
  await fillFormTextarea(dialog, '备注', `T5真实E2E招标采购计划 ${title}`)
  await fillFirstLineInputs(dialog, [`CTTD-${Date.now()}`, `T5招标物料-${title}`, '6001', '8', '件', '2026-07-01'])
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
  await auditDialog.locator('textarea').fill(`T5真实E2E审核通过 ${title}`)
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

async function createContractableTenderSource(page: Page, title: string) {
  const tenderProject = await createTenderProjectFromProcurementPlan(page, title)
  await openMenuPath(page, ['SRM', '招标项目'], '/srm/tender-project')
  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')

  let row = await tableRow(page, title)
  await row.getByRole('button', { name: '发布' }).click()
  const publishDialog = page.locator('.el-dialog:visible').filter({ hasText: '发布招标项目' }).last()
  await publishDialog.waitFor({ state: 'visible', timeout: 30000 })
  const now = Date.now()
  await fillFormInput(publishDialog, '公告标题', `${title}公告`)
  await fillFormInput(publishDialog, '公告附件', 'http://127.0.0.1:9000/yudao/srm/contract/t5-tender-notice.pdf')
  await fillFormInput(publishDialog, '标书名称', `${title}标书`)
  await fillFormInput(publishDialog, '标书附件', 'http://127.0.0.1:9000/yudao/srm/contract/t5-tender-document.pdf')
  await fillFormInput(publishDialog, '投标开始', formatDateTime(new Date(now - 60 * 60 * 1000)))
  await fillFormInput(publishDialog, '投标截止', formatDateTime(new Date(now + 24 * 60 * 60 * 1000)))
  const publishPromise = waitForApiSuccess(page, '/srm/tender-project/publish', 'POST', '发布招标项目')
  await publishDialog.getByRole('button', { name: '发布' }).click()
  const publishPayload = await publishPromise
  expect(publishPayload.data.projectStatus).toBe('PUBLISHED')

  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  row = await tableRow(page, title)
  await row.getByRole('button', { name: '投标' }).click()
  const bidDialog = page.locator('.el-dialog:visible').filter({ hasText: '提交供应商投标' }).last()
  await bidDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(bidDialog, '供应商ID', String(SUPPLIER_ID))
  await fillFormInput(bidDialog, '投标金额', '2180')
  await fillFormInput(bidDialog, '投标附件', 'http://127.0.0.1:9000/yudao/srm/contract/t5-tender-bid.pdf')
  const bidPromise = waitForApiSuccess(page, '/srm/tender-project/submit-bid', 'POST', '提交供应商投标')
  await bidDialog.getByRole('button', { name: '提交投标' }).click()
  await bidPromise

  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  row = await tableRow(page, title)
  await row.getByRole('button', { name: '专家' }).click()
  const expertDialog = page.locator('.el-dialog:visible').filter({ hasText: '创建并通过招标专家' }).last()
  await expertDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(expertDialog, '专家姓名', `${title}专家一,${title}专家二`)
  await fillFormInput(expertDialog, '专业类型', '医疗耗材')
  await fillFormTextarea(expertDialog, '审核意见', `T5 E2E 专家审核通过 ${title}`)
  const createdExpertIds: number[] = []
  page.on('response', async (response) => {
    if (response.url().includes('/srm/tender-project/expert/create') && response.request().method() === 'POST') {
      const payload = await response.json().catch(() => null)
      if (isSuccessPayload(payload)) {
        createdExpertIds.push(payload.data)
      }
    }
  })
  const approveWaits = [
    waitForApiSuccess(page, '/srm/tender-project/expert/approve', 'PUT', '通过招标专家1'),
    waitForApiSuccess(page, '/srm/tender-project/expert/approve', 'PUT', '通过招标专家2')
  ]
  await expertDialog.getByRole('button', { name: '创建并通过' }).click()
  await Promise.all(approveWaits)
  await expect(page.getByText('招标专家已创建并审核通过')).toBeVisible()
  if (createdExpertIds.length < 2) {
    const cells = await expertDialog.locator('.el-table__body-wrapper tbody tr td:first-child').allTextContents()
    for (const cell of cells) {
      const id = Number(cell.trim())
      if (Number.isFinite(id) && id > 0) {
        createdExpertIds.push(id)
      }
    }
  }
  expect(createdExpertIds.length).toBeGreaterThanOrEqual(2)
  await expertDialog.getByRole('button', { name: '取消' }).click()

  row = await tableRow(page, title)
  await row.getByRole('button', { name: '评委会' }).click()
  const committeeDialog = page.locator('.el-dialog:visible').filter({ hasText: '组建评标委员会' }).last()
  await committeeDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormInput(committeeDialog, '要求专业类型', '医疗耗材')
  await fillFormInput(committeeDialog, '要求专家人数', '2')
  await fillFormInput(committeeDialog, '专家ID列表', createdExpertIds.slice(0, 2).join(','))
  const committeePromise = waitForApiSuccess(page, '/srm/tender-project/committee', 'POST', '组建评标委员会')
  await committeeDialog.getByRole('button', { name: '组建评委会' }).click()
  await committeePromise

  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  row = await tableRow(page, title)
  await row.getByRole('button', { name: '候选' }).click()
  const candidateDialog = page.locator('.el-dialog:visible').filter({ hasText: '生成中标候选' }).last()
  await candidateDialog.waitFor({ state: 'visible', timeout: 30000 })
  const candidatePromise = waitForApiSuccess(page, '/srm/tender-project/candidate', 'POST', '生成中标候选')
  await candidateDialog.getByRole('button', { name: '生成候选' }).click()
  await candidatePromise

  await searchByPlaceholder(page, '请输入项目标题', title, '/srm/tender-project/page', '查询招标项目')
  row = await tableRow(page, title)
  await row.getByRole('button', { name: '中标' }).click()
  const winningDialog = page.locator('.el-dialog:visible').filter({ hasText: '确认中标结果' }).last()
  await winningDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormTextarea(winningDialog, '中标说明', `T5 E2E 确认中标 ${title}`)
  const winningPromise = waitForApiSuccess(page, '/srm/tender-project/winning', 'POST', '确认中标结果')
  await winningDialog.getByRole('button', { name: '确认中标' }).click()
  const winningPayload = await winningPromise
  expect(winningPayload.data.id).toBe(tenderProject.id)
  expect(winningPayload.data.projectStatus).toBe('WINNING_CONFIRMED')
  expect(winningPayload.data.contractId ?? null).toBe(null)
  return winningPayload.data
}

async function createContractFromSource(page: Page, sourceType: 'NON_BIDDING' | 'TENDER', sourceId: number, title: string, amount: string) {
  await openMenuPath(page, ['SRM', '采购合同'], '/srm/procurement-contract')
  await page.getByRole('button', { name: '新建合同' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '创建采购合同' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormSelect(page, dialog, '来源类型', sourceType === 'NON_BIDDING' ? '非招标项目' : '招标项目')
  await fillFormInput(dialog, '来源ID', String(sourceId))
  await fillFormInput(dialog, '合同金额', amount)
  await fillFormInput(dialog, '合同标题', title)
  await fillFormInput(dialog, '生效日期', '2026-07-01')
  await formField(dialog, '生效日期').locator('input').last().press('Enter')
  await fillFormInput(dialog, '到期日期', '2026-12-31')
  await formField(dialog, '到期日期').locator('input').last().press('Enter')
  await fillFirstLineInputs(dialog, ['首付款', '30', sourceType === 'NON_BIDDING' ? '354' : '654', '2026-07-10', `T5付款 ${title}`])
  const signingRow = dialog.locator('.el-table__body-wrapper tbody tr').nth(1)
  await signingRow.waitFor({ state: 'visible', timeout: 30000 })
  const signingInputs = signingRow.locator('input')
  await signingInputs.nth(0).fill('采购方')
  await signingInputs.nth(1).fill('采购负责人')
  await signingInputs.nth(2).fill('2026-07-01')
  await signingInputs.nth(2).press('Enter')
  await signingInputs.nth(3).fill(`T5签署 ${title}`)
  const attachmentRow = dialog.locator('.el-table__body-wrapper tbody tr').nth(2)
  await attachmentRow.waitFor({ state: 'visible', timeout: 30000 })
  const attachmentInputs = attachmentRow.locator('input')
  await attachmentInputs.nth(0).fill(`合同正文-${title}`)
  await attachmentInputs.nth(1).fill(`http://127.0.0.1:9000/yudao/srm/contract/${encodeURIComponent(title)}.pdf`)
  await attachmentInputs.nth(2).fill('CONTRACT_FILE')
  const createPromise = waitForApiSuccess(page, '/srm/procurement-contract/create', 'POST', '创建采购合同')
  await dialog.getByRole('button', { name: '保存合同' }).click()
  const payload = await createPromise
  await expect(page.getByText('采购合同已创建并回写来源项目')).toBeVisible()
  expect(payload.data.sourceType).toBe(sourceType)
  expect(payload.data.sourceId).toBe(sourceId)
  expect(payload.data.contractStatus).toBe('EFFECTIVE')
  await searchByPlaceholder(page, '请输入合同标题', title, '/srm/procurement-contract/page', '查询采购合同')
  await expect((await tableRow(page, title)).getByText('生效中')).toBeVisible()
  return payload.data
}

async function cancelContract(page: Page, contractTitle: string) {
  await openMenuPath(page, ['SRM', '采购合同'], '/srm/procurement-contract')
  await searchByPlaceholder(page, '请输入合同标题', contractTitle, '/srm/procurement-contract/page', '查询采购合同')
  const row = await tableRow(page, contractTitle)
  await row.getByRole('button', { name: '作废' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '作废采购合同' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormTextarea(dialog, '作废原因', `T5 E2E 作废合同 ${contractTitle}`)
  const cancelPromise = waitForApiSuccess(page, '/srm/procurement-contract/cancel', 'PUT', '作废采购合同')
  await dialog.getByRole('button', { name: '确认作废' }).click()
  await cancelPromise
  await expect(page.getByText('采购合同已作废，来源项目已恢复可建合同状态')).toBeVisible()
}

async function assertNonBiddingContractable(page: Page, title: string, shouldExist: boolean) {
  await openMenuPath(page, ['SRM', '非招标项目'], '/srm/non-bidding-project')
  const responsePromise = waitForApiSuccess(page, '/srm/non-bidding-project/contractable-page', 'GET', '查询可建合同来源')
  await page.getByRole('button', { name: '可建合同来源' }).click()
  await responsePromise
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '可建合同非招标来源' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  if (shouldExist) {
    await expect(dialog.getByText(title).first()).toBeVisible()
  } else {
    await expect(dialog.getByText(title).first()).toHaveCount(0)
  }
  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
}

test('procurement contract source writeback real flow', async ({ page }) => {
  const unique = `SRM-T5-E2E-${Date.now()}`
  const nonBiddingTitle = `${unique}-非招标项目`
  const tenderTitle = `${unique}-招标项目`
  const nonBiddingContractTitle = `${unique}-非招标合同`
  const tenderContractTitle = `${unique}-招标合同`
  const authContext = await login(page)

  const eligibility = await apiGetJson(page, `/admin-api/srm/supplier-access/check?supplierId=${SUPPLIER_ID}`, authContext)
  expect(eligibility.data.eligible, 'supplier must be approved and risk-free before T5 E2E writes').toBe(true)

  const nonBiddingSource = await createContractableNonBiddingSource(page, nonBiddingTitle)
  await assertNonBiddingContractable(page, nonBiddingTitle, true)
  const nonBiddingContract = await createContractFromSource(page, 'NON_BIDDING', nonBiddingSource.id, nonBiddingContractTitle, '1180')
  const nonBiddingAfterCreate = await apiGetJson(
    page,
    `/admin-api/srm/non-bidding-project/get?id=${nonBiddingSource.id}`,
    authContext
  )
  expect(nonBiddingAfterCreate.data.projectStatus).toBe('CONTRACT_CREATED')
  expect(nonBiddingAfterCreate.data.contractId).toBe(nonBiddingContract.id)
  await assertNonBiddingContractable(page, nonBiddingTitle, false)
  await cancelContract(page, nonBiddingContractTitle)
  const nonBiddingAfterCancel = await apiGetJson(
    page,
    `/admin-api/srm/non-bidding-project/get?id=${nonBiddingSource.id}`,
    authContext
  )
  expect(nonBiddingAfterCancel.data.projectStatus).toBe('DEAL_CONFIRMED')
  expect(nonBiddingAfterCancel.data.contractId ?? null).toBe(null)
  await assertNonBiddingContractable(page, nonBiddingTitle, true)

  const tenderSource = await createContractableTenderSource(page, tenderTitle)
  const tenderContract = await createContractFromSource(page, 'TENDER', tenderSource.id, tenderContractTitle, '2180')
  const tenderAfterCreate = await apiGetJson(
    page,
    `/admin-api/srm/tender-project/get?id=${tenderSource.id}`,
    authContext
  )
  expect(tenderAfterCreate.data.projectStatus).toBe('CONTRACT_CREATED')
  expect(tenderAfterCreate.data.contractId).toBe(tenderContract.id)

  const contractDetail = await apiGetJson(
    page,
    `/admin-api/srm/procurement-contract/get?id=${tenderContract.id}`,
    authContext
  )
  expect(contractDetail.data.payments.length).toBeGreaterThan(0)
  expect(contractDetail.data.signings.length).toBeGreaterThan(0)
  expect(contractDetail.data.attachments.length).toBeGreaterThan(0)
})
