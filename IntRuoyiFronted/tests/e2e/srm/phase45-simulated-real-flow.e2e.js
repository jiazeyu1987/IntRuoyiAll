const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = process.cwd()
const outputDir = path.resolve(frontendRoot, 'output/playwright/srm-phase45')
fs.mkdirSync(outputDir, { recursive: true })

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  return Object.fromEntries(
    fs
      .readFileSync(filePath, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#') && line.includes('='))
      .map((line) => {
        const delimiterIndex = line.indexOf('=')
        return [line.slice(0, delimiterIndex), line.slice(delimiterIndex + 1)]
      })
  )
}

function firstNonEmpty(...values) {
  for (const value of values) {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      return String(value).trim()
    }
  }
  return ''
}

function requiredNumber(label, ...values) {
  const rawValue = firstNonEmpty(...values)
  const value = Number(rawValue)
  assert.ok(Number.isFinite(value) && value > 0, `${label} must be a positive number, got ${rawValue || '<empty>'}`)
  return value
}

function requiredText(label, ...values) {
  const value = firstNonEmpty(...values)
  assert.ok(value, `${label} is required`)
  return value
}

const runtimeEnv = readEnvFile(path.resolve(frontendRoot, '.runtime', 'runtime.env'))
const config = {
  baseUrl: requiredText('baseUrl', process.env.SRM_PHASE45_BASE_URL, runtimeEnv.BASE_URL),
  backendUrl: requiredText('backendUrl', process.env.SRM_PHASE45_BACKEND_URL, runtimeEnv.BACKEND_URL),
  actuatorUrl: requiredText(
    'actuatorUrl',
    process.env.SRM_PHASE45_ACTUATOR_URL,
    runtimeEnv.ACTUATOR_URL,
    firstNonEmpty(process.env.SRM_PHASE45_BACKEND_URL, runtimeEnv.BACKEND_URL)
      ? `${firstNonEmpty(process.env.SRM_PHASE45_BACKEND_URL, runtimeEnv.BACKEND_URL)}/actuator/health`
      : ''
  ),
  tenantName: requiredText('tenantName', process.env.SRM_PHASE45_TENANT, runtimeEnv.TENANT_WRITE, '测试租户'),
  username: requiredText('username', process.env.SRM_PHASE45_USERNAME, runtimeEnv.USERNAME_WRITE, 'aoteman'),
  password: requiredText('password', process.env.SRM_PHASE45_PASSWORD, '111111'),
  readonlyTenantName: requiredText(
    'readonlyTenantName',
    process.env.SRM_PHASE45_READONLY_TENANT,
    runtimeEnv.TENANT_READONLY,
    '芋道源码'
  ),
  readonlyUsername: requiredText(
    'readonlyUsername',
    process.env.SRM_PHASE45_READONLY_USERNAME,
    runtimeEnv.USERNAME_READONLY,
    'admin'
  ),
  readonlyPassword: requiredText('readonlyPassword', process.env.SRM_PHASE45_READONLY_PASSWORD, 'admin123'),
  projectId: requiredNumber('projectId', process.env.SRM_PHASE45_PROJECT_ID, runtimeEnv.SAMPLE_PROJECT_ID),
  projectNo: requiredText('projectNo', process.env.SRM_PHASE45_PROJECT_NO, runtimeEnv.SAMPLE_PROJECT_NO),
  purchaseOrderId: requiredNumber(
    'purchaseOrderId',
    process.env.SRM_PHASE45_PURCHASE_ORDER_ID,
    runtimeEnv.SAMPLE_PURCHASE_ORDER_ID
  ),
  purchaseOrderNo: requiredText(
    'purchaseOrderNo',
    process.env.SRM_PHASE45_PURCHASE_ORDER_NO,
    runtimeEnv.SAMPLE_PURCHASE_ORDER_NO
  )
}

const TEST_TENANT = '测试租户'
const READONLY_TENANT = '芋道源码'
const requiredPermissions = [
  'srm:procurement-contract:query',
  'srm:procurement-contract:create',
  'srm:outsource-execution:query',
  'srm:outsource-execution:create',
  'srm:outsource-execution:update',
  'srm:payment-execution:query',
  'srm:payment-execution:create',
  'srm:payment-execution:approve'
]
const requiredComponents = [
  'srm/procurement-contract/index',
  'srm/outsource-execution/index',
  'srm/outsource-execution/my',
  'srm/payment-execution/index'
]

function assertSuccess(payload, action) {
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `${action} failed: ${JSON.stringify(payload)}`)
}

function flattenMenus(list, result = []) {
  for (const item of Array.isArray(list) ? list : []) {
    result.push(item)
    flattenMenus(item.children, result)
  }
  return result
}

function normalizeText(value) {
  return String(value || '')
    .replace(/\s+/g, ' ')
    .trim()
}

function isoDate(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
}

async function fillInput(input, value) {
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.click({ timeout: 30000 })
  await input.fill('')
  await input.fill(String(value))
  await input.press('Tab').catch(() => null)
}

async function fillTextarea(container, labelText, value) {
  const textarea = container.locator('.el-form-item').filter({ hasText: labelText }).first().locator('textarea').last()
  await textarea.waitFor({ state: 'visible', timeout: 30000 })
  await textarea.fill('')
  await textarea.fill(value)
}

async function fillLabeledInput(container, labelText, value) {
  const input = container.locator('.el-form-item').filter({ hasText: labelText }).first().locator('input').last()
  await fillInput(input, value)
}

async function chooseSelectOption(page, container, labelText, optionText) {
  const input = container
    .locator('.el-form-item')
    .filter({ hasText: labelText })
    .first()
    .locator('.el-select input[role="combobox"], .el-select input')
    .first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.click()
  if (await input.isEditable().catch(() => false)) {
    await input.fill('')
    await input.fill(optionText)
  }
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function fillTableRowInputs(table, values) {
  const row = table.locator('.el-table__body-wrapper tbody tr').first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const inputs = row.locator('input')
  for (let index = 0; index < values.length; index += 1) {
    await fillInput(inputs.nth(index), values[index])
  }
}

async function waitForApiSuccessDetailed(page, pathFragment, method, action) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(pathFragment) && item.request().method() === method,
    { timeout: 60000 }
  )
  assert.equal(response.status(), 200, `${action} should return HTTP 200`)
  const payload = await response.json().catch(() => null)
  assertSuccess(payload, action)
  return { response, payload }
}

async function login(page, credentials) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const inputs = form.locator('input')
  const tenantInput = form
    .locator('input[role="combobox"], input.el-select__input, .el-select input[role="combobox"]')
    .first()
  const userInput = inputs.nth(1)
  const passwordInput = form.locator('input[type="password"]').first()
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(credentials.tenantName)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.fill('')
  await tenantInput.fill(credentials.tenantName)
  await tenantInput.press('Enter')
  await tenantResponsePromise
  await page.keyboard.press('Escape').catch(() => null)
  await page.waitForTimeout(300)

  await userInput.fill('')
  await userInput.fill(credentials.username)
  await passwordInput.fill('')
  await passwordInput.fill(credentials.password)

  const loginResponsePromise = waitForApiSuccessDetailed(page, '/system/auth/login', 'POST', `login(${credentials.username})`)
  const permissionPromise = waitForApiSuccessDetailed(page, '/system/auth/get-permission-info', 'GET', `permission(${credentials.username})`)
  await form.locator('button.el-button--primary').first().click()
  const { payload: permissionPayload, response: permissionResponse } = await permissionPromise
  await loginResponsePromise
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)

  const permissions = permissionPayload.data?.permissions || []
  const menus = flattenMenus(permissionPayload.data?.menus || permissionPayload.data?.menuList || [])
  for (const permission of requiredPermissions) {
    assert.ok(permissions.includes(permission), `missing permission ${permission}`)
  }
  for (const component of requiredComponents) {
    assert.ok(menus.some((item) => item?.component === component), `missing route component ${component}`)
  }

  const headers = permissionResponse.request().headers()
  return {
    authorization: headers.authorization || headers.Authorization || '',
    tenantId: headers['tenant-id'] || headers['Tenant-Id'] || '',
    permissionPayload
  }
}

async function fetchJson(page, relativeUrl, authContext, { method = 'GET', body } = {}) {
  const payload = await page.evaluate(
    async ({ url, method, body, authHeader, tenantHeader }) => {
      const headers = { Accept: 'application/json, text/plain, */*' }
      if (authHeader) headers.Authorization = authHeader
      if (tenantHeader) headers['tenant-id'] = tenantHeader
      if (body) headers['Content-Type'] = 'application/json'
      const response = await fetch(url, {
        method,
        credentials: 'include',
        headers,
        body: body ? JSON.stringify(body) : undefined
      })
      return { status: response.status, text: await response.text() }
    },
    {
      url: `${config.baseUrl}${relativeUrl}`,
      method,
      body,
      authHeader: authContext.authorization || '',
      tenantHeader: authContext.tenantId || ''
    }
  )
  assert.equal(payload.status, 200, `${method} ${relativeUrl} should return HTTP 200`)
  const data = JSON.parse(payload.text)
  assertSuccess(data, `${method} ${relativeUrl}`)
  return data
}

async function openRoute(page, relativePath, listApiPath, markerLocator) {
  const responsePromise = waitForApiSuccessDetailed(page, listApiPath, 'GET', `open ${relativePath}`)
  await page.goto(`${config.baseUrl}${relativePath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const { response } = await responsePromise
  await settle(page, 30000)
  if (markerLocator) {
    await markerLocator(page).waitFor({ state: 'visible', timeout: 30000 })
  }
  return response.url()
}

async function searchByPlaceholder(page, placeholder, value, apiPath, action) {
  const input = page.locator(`input[placeholder="${placeholder}"]`).first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill('')
  await input.fill(value)
  const responsePromise = waitForApiSuccessDetailed(page, apiPath, 'GET', action)
  await page.getByRole('button', { name: '搜索' }).click()
  await responsePromise
  await settle(page)
}

async function tableRow(page, text) {
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: text }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function readCellText(row, index) {
  return normalizeText(await row.locator('td').nth(index).innerText())
}

async function createContract(page) {
  const contractTitle = `P45-E2E-${config.projectNo}-${Date.now()}`
  const effectiveDate = isoDate(0)
  const expireDate = isoDate(180)
  const createPageRequestUrl = await openRoute(
    page,
    '/srm/procurement-contract',
    '/srm/procurement-contract/page',
    (currentPage) => currentPage.getByRole('button', { name: '新建合同' })
  )

  await page.getByRole('button', { name: '新建合同' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '创建采购合同' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillLabeledInput(dialog, '来源ID', String(config.projectId))
  await fillLabeledInput(dialog, '合同金额', '1180')
  await fillLabeledInput(dialog, '合同标题', contractTitle)
  await fillLabeledInput(dialog, '生效日期', effectiveDate)
  await fillLabeledInput(dialog, '到期日期', expireDate)

  const tables = dialog.locator('.el-table')
  await fillTableRowInputs(tables.nth(0), ['预付款', '30', '354', effectiveDate, 'Phase45 E2E 付款约定'])
  await fillTableRowInputs(tables.nth(1), ['采购方', 'Phase45签署人', effectiveDate, 'Phase45 E2E 签署信息'])
  await fillTableRowInputs(tables.nth(2), [
    '合同正文',
    'http://127.0.0.1:9000/yudao/srm/contract/phase45-e2e.pdf',
    'CONTRACT_FILE'
  ])

  const createPromise = waitForApiSuccessDetailed(page, '/srm/procurement-contract/create', 'POST', 'create contract')
  await dialog.getByRole('button', { name: '保存合同' }).click()
  const { payload } = await createPromise
  const contract = payload.data
  assert.equal(contract.sourceId, config.projectId)
  assert.equal(contract.sourceNo, config.projectNo)
  assert.equal(contract.contractStatus, 'EFFECTIVE')
  assert.equal(contract.contractTitle, contractTitle)

  await searchByPlaceholder(page, '请输入合同标题', contractTitle, '/srm/procurement-contract/page', 'query contract by title')
  const row = await tableRow(page, contractTitle)
  assert.equal(await readCellText(row, 0), contract.contractNo)
  assert.equal(await readCellText(row, 4), config.projectNo)
  await page.screenshot({ path: path.join(outputDir, `phase45-contract-${contract.contractNo}.png`), fullPage: true })
  return {
    createPageRequestUrl,
    contractId: Number(contract.id),
    contractNo: contract.contractNo,
    contractTitle
  }
}

async function createAndRunOutsource(page, contractResult) {
  await openRoute(
    page,
    '/srm/outsource-execution',
    '/srm/outsource-execution/page',
    (currentPage) => currentPage.getByRole('button', { name: '新建委外执行' })
  )
  await page.getByRole('button', { name: '新建委外执行' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '创建委外执行单' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await chooseSelectOption(page, dialog, '采购订单协同单', config.purchaseOrderNo)
  await fillTextarea(dialog, '模拟说明', `Phase45 E2E ${config.purchaseOrderNo} -> ${contractResult.contractNo}`)
  const createPromise = waitForApiSuccessDetailed(
    page,
    '/srm/outsource-execution/create-from-purchase-order',
    'POST',
    'create outsource execution'
  )
  await dialog.getByRole('button', { name: '创建' }).click()
  const createPayload = (await createPromise).payload
  const executionId = Number(createPayload.data)
  assert.ok(executionId > 0, 'execution id should be returned')

  await searchByPlaceholder(
    page,
    '请输入采购订单号',
    config.purchaseOrderNo,
    '/srm/outsource-execution/page',
    'query outsource execution by purchase order'
  )
  let row = await tableRow(page, config.purchaseOrderNo)
  const executionNo = await readCellText(row, 0)
  await row.getByRole('button', { name: '发料' }).click()
  let issueDialog = page.locator('.el-dialog:visible').filter({ hasText: '登记模拟发料' }).last()
  await fillLabeledInput(issueDialog, '发料数量', '10')
  await fillTextarea(issueDialog, '发料说明', 'Phase45 E2E 模拟 PDA 发料')
  const issuePromise = waitForApiSuccessDetailed(page, '/srm/outsource-execution/issue', 'PUT', 'confirm issue')
  await issueDialog.getByRole('button', { name: '确认发料' }).click()
  await issuePromise
  await searchByPlaceholder(
    page,
    '请输入采购订单号',
    config.purchaseOrderNo,
    '/srm/outsource-execution/page',
    'refresh outsource execution after issue'
  )
  row = await tableRow(page, config.purchaseOrderNo)
  await page.screenshot({ path: path.join(outputDir, `phase45-outsource-issued-${executionNo}.png`), fullPage: true })

  await openRoute(
    page,
    '/srm/outsource-execution/my',
    '/srm/outsource-execution/my/page',
    (currentPage) => currentPage.getByText('委外执行协同台')
  )
  await searchByPlaceholder(
    page,
    '请输入采购订单号',
    config.purchaseOrderNo,
    '/srm/outsource-execution/my/page',
    'query supplier outsource execution by purchase order'
  )
  row = await tableRow(page, config.purchaseOrderNo)
  await row.getByRole('button', { name: '回传进度' }).click()
  let progressDialog = page.locator('.el-dialog:visible').filter({ hasText: '回传加工进度' }).last()
  await fillLabeledInput(progressDialog, '进度百分比', '60')
  await fillLabeledInput(progressDialog, '进度阶段', '加工中')
  await fillTextarea(progressDialog, '补充说明', 'Phase45 E2E 供应商模拟进度回传')
  const progressPromise = waitForApiSuccessDetailed(
    page,
    '/srm/outsource-execution/my/progress',
    'PUT',
    'submit outsource progress'
  )
  await progressDialog.getByRole('button', { name: '提交进度' }).click()
  await progressPromise
  await searchByPlaceholder(
    page,
    '请输入采购订单号',
    config.purchaseOrderNo,
    '/srm/outsource-execution/my/page',
    'refresh supplier outsource execution after progress'
  )
  row = await tableRow(page, config.purchaseOrderNo)
  await row.getByRole('button', { name: '回传收货' }).click()
  let receiveDialog = page.locator('.el-dialog:visible').filter({ hasText: '回传送收货结果' }).last()
  await fillLabeledInput(receiveDialog, '收货数量', '10')
  await fillTextarea(receiveDialog, '补充说明', 'Phase45 E2E 供应商模拟送收货回传')
  const receivePromise = waitForApiSuccessDetailed(
    page,
    '/srm/outsource-execution/my/receive',
    'PUT',
    'submit outsource receive'
  )
  await receiveDialog.getByRole('button', { name: '提交收货' }).click()
  await receivePromise
  await searchByPlaceholder(
    page,
    '请输入采购订单号',
    config.purchaseOrderNo,
    '/srm/outsource-execution/my/page',
    'refresh supplier outsource execution after receive'
  )
  row = await tableRow(page, config.purchaseOrderNo)
  await page.screenshot({ path: path.join(outputDir, `phase45-outsource-supplier-${executionNo}.png`), fullPage: true })

  await openRoute(
    page,
    '/srm/outsource-execution',
    '/srm/outsource-execution/page',
    (currentPage) => currentPage.getByRole('button', { name: '新建委外执行' })
  )
  await searchByPlaceholder(
    page,
    '请输入采购订单号',
    config.purchaseOrderNo,
    '/srm/outsource-execution/page',
    'query outsource execution before inspect'
  )
  row = await tableRow(page, config.purchaseOrderNo)
  await row.getByRole('button', { name: '检验' }).click()
  let inspectDialog = page.locator('.el-dialog:visible').filter({ hasText: '登记检验结果' }).last()
  await fillLabeledInput(inspectDialog, '合格数量', '10')
  await fillTextarea(inspectDialog, '检验说明', 'Phase45 E2E 模拟检验合格')
  await Promise.all([
    waitForApiSuccessDetailed(page, '/srm/outsource-execution/inspect', 'PUT', 'inspect outsource execution'),
    inspectDialog.getByRole('button', { name: '确认检验' }).click()
  ])
  await searchByPlaceholder(
    page,
    '请输入采购订单号',
    config.purchaseOrderNo,
    '/srm/outsource-execution/page',
    'refresh outsource execution before reconcile'
  )
  row = await tableRow(page, config.purchaseOrderNo)
  await row.getByRole('button', { name: '对账' }).click()
  let reconcileDialog = page.locator('.el-dialog:visible').filter({ hasText: '确认对账结果' }).last()
  await fillTextarea(reconcileDialog, '对账说明', 'Phase45 E2E 模拟对账确认')
  await Promise.all([
    waitForApiSuccessDetailed(page, '/srm/outsource-execution/reconcile', 'PUT', 'reconcile outsource execution'),
    reconcileDialog.getByRole('button', { name: '确认对账' }).click()
  ])
  await searchByPlaceholder(
    page,
    '请输入采购订单号',
    config.purchaseOrderNo,
    '/srm/outsource-execution/page',
    'refresh outsource execution after reconcile'
  )
  row = await tableRow(page, config.purchaseOrderNo)
  const reconciliationAmount = await readCellText(row, 7)
  await page.screenshot({ path: path.join(outputDir, `phase45-outsource-reconciled-${executionNo}.png`), fullPage: true })

  return {
    executionId,
    executionNo,
    reconciliationAmount
  }
}

async function createAndCompletePayment(page, executionResult, contractResult) {
  await openRoute(
    page,
    '/srm/payment-execution',
    '/srm/payment-execution/page',
    (currentPage) => currentPage.getByRole('button', { name: '新建付款申请' })
  )
  await page.getByRole('button', { name: '新建付款申请' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '创建付款申请' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await chooseSelectOption(page, dialog, '对账结果', executionResult.executionNo)
  await page.waitForTimeout(1000)
  await chooseSelectOption(page, dialog, '采购合同', contractResult.contractNo)
  await fillTextarea(dialog, '付款说明', 'Phase45 E2E 测试租户受控模拟付款申请')
  const createPromise = waitForApiSuccessDetailed(
    page,
    '/srm/payment-execution/create-from-reconciliation',
    'POST',
    'create payment execution'
  )
  await dialog.getByRole('button', { name: '创建' }).click()
  const createPayload = (await createPromise).payload
  const paymentId = Number(createPayload.data)
  assert.ok(paymentId > 0, 'payment execution id should be returned')

  let row = await tableRow(page, contractResult.contractNo)
  const paymentNo = await readCellText(row, 0)
  const reconciliationNo = await readCellText(row, 1)
  await row.getByRole('button', { name: '提交' }).click()
  let submitDialog = page.locator('.el-dialog:visible').filter({ hasText: '提交付款申请' }).last()
  await fillTextarea(submitDialog, '提交说明', 'Phase45 E2E 提交付款申请')
  await Promise.all([
    waitForApiSuccessDetailed(page, '/srm/payment-execution/submit', 'PUT', 'submit payment execution'),
    submitDialog.getByRole('button', { name: '确认提交' }).click()
  ])

  await searchByPlaceholder(
    page,
    '请输入对账单号',
    reconciliationNo,
    '/srm/payment-execution/page',
    'refresh payment execution before approve'
  )
  row = await tableRow(page, reconciliationNo)
  await row.getByRole('button', { name: '审批通过' }).click()
  let approveDialog = page.locator('.el-dialog:visible').filter({ hasText: '审批通过付款申请' }).last()
  await fillTextarea(approveDialog, '审批说明', 'Phase45 E2E 审批通过')
  await Promise.all([
    waitForApiSuccessDetailed(page, '/srm/payment-execution/approve', 'PUT', 'approve payment execution'),
    approveDialog.getByRole('button', { name: '确认审批' }).click()
  ])

  await searchByPlaceholder(
    page,
    '请输入对账单号',
    reconciliationNo,
    '/srm/payment-execution/page',
    'refresh payment execution before finance push'
  )
  row = await tableRow(page, reconciliationNo)
  await row.getByRole('button', { name: '财务回执' }).click()
  let pushDialog = page.locator('.el-dialog:visible').filter({ hasText: '记录财务回执' }).last()
  await fillTextarea(pushDialog, '回执说明', 'Phase45 E2E 模拟财务推送成功')
  await Promise.all([
    waitForApiSuccessDetailed(page, '/srm/payment-execution/finance-push', 'PUT', 'finance push payment execution'),
    pushDialog.getByRole('button', { name: '确认记录' }).click()
  ])

  await searchByPlaceholder(
    page,
    '请输入对账单号',
    reconciliationNo,
    '/srm/payment-execution/page',
    'refresh payment execution after finance push'
  )
  row = await tableRow(page, reconciliationNo)
  await page.screenshot({ path: path.join(outputDir, `phase45-payment-${paymentNo}.png`), fullPage: true })
  return {
    paymentId,
    paymentNo,
    reconciliationNo
  }
}

async function verifyFinalState(page, authContext, contractResult, executionResult, paymentResult) {
  const contractPayload = await fetchJson(
    page,
    `/admin-api/srm/procurement-contract/get?id=${contractResult.contractId}`,
    authContext
  )
  const contract = contractPayload.data
  assert.equal(contract.contractNo, contractResult.contractNo)
  assert.equal(contract.sourceId, config.projectId)
  assert.equal(contract.sourceNo, config.projectNo)
  assert.equal(contract.contractStatus, 'EFFECTIVE')
  assert.ok((contract.payments || []).length > 0, 'contract payments should not be empty')

  const executionPayload = await fetchJson(
    page,
    `/admin-api/srm/outsource-execution/get?id=${executionResult.executionId}`,
    authContext
  )
  const execution = executionPayload.data
  assert.equal(execution.executionNo, executionResult.executionNo)
  assert.equal(execution.sourcePurchaseOrderId, config.purchaseOrderId)
  assert.equal(execution.sourcePurchaseOrderNo, config.purchaseOrderNo)
  assert.equal(execution.executionStatus, 'RECONCILED')
  assert.equal(execution.simulationLabel, '测试租户受控模拟链路')
  assert.equal(Number(execution.receivedQuantity), 10)
  assert.equal(Number(execution.qualifiedQuantity), 10)
  assert.ok(execution.reconciliation?.reconciliationNo, 'reconciliation number should be generated')
  assert.ok((execution.events || []).length >= 5, 'execution should record create/issue/progress/receive/inspect/reconcile events')

  const paymentPayload = await fetchJson(
    page,
    `/admin-api/srm/payment-execution/get?id=${paymentResult.paymentId}`,
    authContext
  )
  const payment = paymentPayload.data
  assert.equal(payment.paymentNo, paymentResult.paymentNo)
  assert.equal(payment.reconciliationNo, paymentResult.reconciliationNo)
  assert.equal(payment.contractNo, contractResult.contractNo)
  assert.equal(payment.paymentStatus, 'PUSH_SUCCESS')
  assert.match(payment.paymentTermSummary || '', new RegExp(contractResult.contractNo))
  assert.equal(payment.simulationLabel, '测试租户受控模拟链路')
  assert.ok((payment.events || []).length >= 4, 'payment should record create/submit/approve/push events')

  return {
    contract,
    execution,
    payment
  }
}

async function verifyReadonlyIsolation(browser, identifiers) {
  const readonlyContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const readonlyPage = await readonlyContext.newPage()
  try {
    const readonlyAuth = await login(readonlyPage, {
      tenantName: config.readonlyTenantName,
      username: config.readonlyUsername,
      password: config.readonlyPassword
    })
    assert.equal(readonlyAuth.tenantId, '1', 'readonly verification must use tenant-id=1')

    const contractPage = await fetchJson(
      readonlyPage,
      `/admin-api/srm/procurement-contract/page?pageNo=1&pageSize=10&contractNo=${encodeURIComponent(identifiers.contractNo)}`,
      readonlyAuth
    )
    const executionPage = await fetchJson(
      readonlyPage,
      `/admin-api/srm/outsource-execution/page?pageNo=1&pageSize=10&executionNo=${encodeURIComponent(identifiers.executionNo)}`,
      readonlyAuth
    )
    const paymentPage = await fetchJson(
      readonlyPage,
      `/admin-api/srm/payment-execution/page?pageNo=1&pageSize=10&paymentNo=${encodeURIComponent(identifiers.paymentNo)}`,
      readonlyAuth
    )

    assert.equal((contractPage.data?.list || []).length, 0, 'readonly tenant should not see test-tenant contract')
    assert.equal((executionPage.data?.list || []).length, 0, 'readonly tenant should not see test-tenant outsource execution')
    assert.equal((paymentPage.data?.list || []).length, 0, 'readonly tenant should not see test-tenant payment execution')
    return readonlyAuth.tenantId
  } finally {
    await readonlyContext.close()
  }
}

async function main() {
  assert.equal(config.tenantName, TEST_TENANT, `write E2E must use 测试租户, got ${config.tenantName}`)
  assert.equal(config.username, 'aoteman', `write E2E must use aoteman, got ${config.username}`)
  assert.equal(config.readonlyTenantName, READONLY_TENANT, `readonly tenant must be 芋道源码, got ${config.readonlyTenantName}`)

  const healthResponse = await fetch(config.actuatorUrl)
  assert.equal(healthResponse.status, 200, `${config.actuatorUrl} should return 200`)
  const healthPayload = await healthResponse.json()
  assert.equal(healthPayload.status, 'UP', 'backend actuator must be UP before phase45 E2E')

  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  try {
    const writerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const writerPage = await writerContext.newPage()
    const writerAuth = await login(writerPage, {
      tenantName: config.tenantName,
      username: config.username,
      password: config.password
    })
    assert.equal(writerAuth.tenantId, '122', 'writer tenant-id must be 122')

    const contractResult = await createContract(writerPage)
    const executionResult = await createAndRunOutsource(writerPage, contractResult)
    const paymentResult = await createAndCompletePayment(writerPage, executionResult, contractResult)
    const finalState = await verifyFinalState(writerPage, writerAuth, contractResult, executionResult, paymentResult)
    const readonlyTenantId = await verifyReadonlyIsolation(browser, {
      contractNo: contractResult.contractNo,
      executionNo: executionResult.executionNo,
      paymentNo: paymentResult.paymentNo
    })

    console.log(
      JSON.stringify(
        {
          ok: true,
          runtime: {
            baseUrl: config.baseUrl,
            backendUrl: config.backendUrl,
            actuatorUrl: config.actuatorUrl,
            firstBusinessRequestUrl: contractResult.createPageRequestUrl
          },
          writerTenantId: writerAuth.tenantId,
          readonlyTenantId,
          samples: {
            projectId: config.projectId,
            projectNo: config.projectNo,
            purchaseOrderId: config.purchaseOrderId,
            purchaseOrderNo: config.purchaseOrderNo
          },
          contract: {
            id: contractResult.contractId,
            no: contractResult.contractNo,
            title: contractResult.contractTitle,
            status: finalState.contract.contractStatus
          },
          execution: {
            id: executionResult.executionId,
            no: executionResult.executionNo,
            status: finalState.execution.executionStatus,
            reconciliationNo: finalState.execution.reconciliation?.reconciliationNo,
            reconciliationAmount: finalState.execution.reconciliation?.reconciliationAmount
          },
          payment: {
            id: paymentResult.paymentId,
            no: paymentResult.paymentNo,
            status: finalState.payment.paymentStatus,
            applyAmount: finalState.payment.applyAmount
          },
          screenshots: {
            contract: path.join(outputDir, `phase45-contract-${contractResult.contractNo}.png`),
            outsourceIssued: path.join(outputDir, `phase45-outsource-issued-${executionResult.executionNo}.png`),
            outsourceSupplier: path.join(outputDir, `phase45-outsource-supplier-${executionResult.executionNo}.png`),
            outsourceReconciled: path.join(outputDir, `phase45-outsource-reconciled-${executionResult.executionNo}.png`),
            payment: path.join(outputDir, `phase45-payment-${paymentResult.paymentNo}.png`)
          }
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
