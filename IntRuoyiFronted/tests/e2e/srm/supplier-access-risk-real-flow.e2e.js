const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = process.cwd()
const runtimeEnvPath = path.resolve(frontendRoot, '.runtime/runtime.env')
const outputDir = path.resolve(frontendRoot, 'output/playwright/srm-d7-d10-t1')

function readRuntimeEnv(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  return fs
    .readFileSync(filePath, 'utf8')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#') && line.includes('='))
    .reduce((result, line) => {
      const index = line.indexOf('=')
      result[line.slice(0, index)] = line.slice(index + 1)
      return result
    }, {})
}

const runtimeEnv = readRuntimeEnv(runtimeEnvPath)
const config = {
  baseUrl: (process.env.SRM_SUPPLIER_E2E_BASE_URL || runtimeEnv.FRONTEND_URL || 'http://localhost:8093').replace(/\/+$/, ''),
  tenant: process.env.SRM_SUPPLIER_E2E_TENANT || runtimeEnv.WRITE_TENANT_NAME || '测试租户',
  creatorUsername: process.env.SRM_SUPPLIER_E2E_USERNAME || runtimeEnv.WRITE_TENANT_ACCOUNT || 'aoteman',
  creatorPassword: process.env.SRM_SUPPLIER_E2E_PASSWORD || '111111',
  auditorUsername: process.env.SRM_SUPPLIER_E2E_AUDITOR_USERNAME || 'edhrmatrixapprover',
  auditorPassword: process.env.SRM_SUPPLIER_E2E_AUDITOR_PASSWORD || '111111',
  supplierId: Number(process.env.SRM_SUPPLIER_E2E_SUPPLIER_ID || '103'),
  supplierName: process.env.SRM_SUPPLIER_E2E_SUPPLIER_NAME || '山东瑛泰医疗器械有限公司',
  supplierKeyword: process.env.SRM_SUPPLIER_E2E_SUPPLIER_KEYWORD || '瑛泰',
  accessPath: '/srm/supplier/access',
  riskPath: '/srm/supplier/risk',
  headed: process.env.SRM_SUPPLIER_E2E_HEADED === '1'
}

const screenshots = {
  access: path.join(outputDir, 'supplier-access-page.png'),
  risk: path.join(outputDir, 'supplier-risk-page.png'),
  failure: path.join(outputDir, 'supplier-access-risk-failure.png')
}

const requiredPermissions = [
  'srm:supplier-access:query',
  'srm:supplier-access:create',
  'srm:supplier-access:audit',
  'srm:supplier-access:enable',
  'srm:supplier-access:check',
  'srm:supplier-risk:query',
  'srm:supplier-risk:create',
  'srm:supplier-risk:resolve'
]

function flattenMenus(list, result = []) {
  for (const item of Array.isArray(list) ? list : []) {
    result.push(item)
    flattenMenus(item.children, result)
  }
  return result
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(700)
}

async function fillFirstVisible(locator, value, label) {
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

async function selectTenant(page, loginForm) {
  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]')
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

function isSuccessPayload(payload) {
  return Boolean(payload) && (payload.code === 0 || payload.code === 200)
}

function ensureSrmPermissions(permissionPayload) {
  const permissionData = permissionPayload?.data || {}
  const permissions = permissionData.permissions || []
  const menus = flattenMenus(permissionData.menus || permissionData.menuList || [])

  for (const permission of requiredPermissions) {
    assert.ok(permissions.includes(permission), `missing permission ${permission}`)
  }
  assert.ok(
    menus.some((menu) => menu?.component === 'srm/supplier-access/index'),
    'missing supplier access route menu'
  )
  assert.ok(
    menus.some((menu) => menu?.component === 'srm/supplier-risk/index'),
    'missing supplier risk route menu'
  )
}

function pickLatestRecord(list, predicate) {
  return [...(Array.isArray(list) ? list : [])]
    .filter(predicate)
    .sort((left, right) => Number(right?.id || 0) - Number(left?.id || 0))[0] || null
}

async function closeDialog(dialog) {
  for (const buttonName of ['关闭', '取消']) {
    const button = dialog.getByRole('button', { name: buttonName }).first()
    if (await button.isVisible().catch(() => false)) {
      await button.click()
      return
    }
  }
  await dialog.locator('.el-dialog__headerbtn').click().catch(() => null)
}

async function login(page, actor) {
  await page.context().clearCookies()
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
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
    await textboxes.nth(textboxCount >= 3 ? 1 : 0).fill(actor.username)
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), actor.username, 'username')
  }
  await fillFirstVisible(
    loginForm.locator('input[type="password"], input[placeholder="请输入密码"]'),
    actor.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionPromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
      { timeout: 60000 }
    )
    .catch(() => null)

  const loginButton = loginForm.getByRole('button', { name: '登录' }).first()
  await loginButton.waitFor({ state: 'visible', timeout: 30000 })
  await loginButton.click()

  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(
    isSuccessPayload(loginPayload),
    `login failed for ${actor.username}: ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)

  const permissionResponse = await permissionPromise
  return {
    permissionPayload: permissionResponse ? await permissionResponse.json().catch(() => null) : null,
    authorization:
      permissionResponse?.request().headers()['authorization'] ||
      permissionResponse?.request().headers()['Authorization'] ||
      '',
    tenantId:
      permissionResponse?.request().headers()['tenant-id'] ||
      permissionResponse?.request().headers()['Tenant-Id'] ||
      ''
  }
}

async function apiGetJson(page, relativePath, authContext = {}) {
  const payload = await page.evaluate(async ({ url, authHeader, tenantHeader }) => {
    const headers = { Accept: 'application/json, text/plain, */*' }
    if (authHeader) {
      headers.Authorization = authHeader
    }
    if (tenantHeader) {
      headers['tenant-id'] = tenantHeader
    }
    const response = await fetch(url, {
      credentials: 'include',
      headers
    })
    const text = await response.text()
    return { status: response.status, text, hasAccessToken: Boolean(authHeader), tenantId: tenantHeader }
  }, {
    url: relativePath,
    authHeader: authContext.authorization || '',
    tenantHeader: authContext.tenantId || ''
  })
  let data = null
  try {
    data = JSON.parse(payload.text)
  } catch (error) {
    throw new Error(`Failed to parse JSON from ${relativePath}: ${payload.text}`)
  }
  assert.equal(
    payload.status,
    200,
    `${relativePath} should return HTTP 200; token=${payload.hasAccessToken}; tenantId=${payload.tenantId}`
  )
  assert.ok(isSuccessPayload(data), `${relativePath} should return code 0/200: ${payload.text}`)
  return data
}

async function openMenuPath(page, labels, expectedPath) {
  for (const label of labels.slice(0, -1)) {
    const parent = page.locator('.el-sub-menu__title:visible').filter({ hasText: label }).first()
    await parent.waitFor({ state: 'visible', timeout: 30000 })
    await parent.click()
    await page.waitForTimeout(500)
  }
  const leafLabel = labels[labels.length - 1]
  const leaf = page.locator('.el-menu-item:visible').filter({ hasText: leafLabel }).first()
  await leaf.waitFor({ state: 'visible', timeout: 30000 })
  await Promise.all([
    page.waitForURL((url) => url.href.includes(expectedPath), { timeout: 60000 }),
    leaf.click()
  ])
  await settle(page, 30000)
  assert.ok(page.url().includes(expectedPath), `expected current page to be ${expectedPath}, got ${page.url()}`)
}

async function openMenuPathVariants(page, variants, expectedPath) {
  let lastError = null
  for (const labels of variants) {
    try {
      await openMenuPath(page, labels, expectedPath)
      return
    } catch (error) {
      lastError = error
    }
  }
  await page.goto(`${config.baseUrl}${expectedPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page, 30000)
  if (page.url().includes(expectedPath)) {
    return
  }
  throw lastError || new Error(`failed to open ${expectedPath}`)
}

async function chooseRemoteSupplier(page, container, keyword, optionText) {
  const input = container.locator('.el-select input[role="combobox"], .el-select input.el-select__input').first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.click()
  await input.fill('')
  await input.fill(keyword)
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function fillNumberInput(container, labelText, value) {
  const field = container.locator('.el-form-item').filter({ hasText: labelText }).first()
  const input = field.locator('input').last()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill('')
  await input.type(String(value))
}

async function waitForApiPayload(page, matcher, action) {
  return page.waitForResponse((response) => {
    if (!response.url().includes(matcher.path)) {
      return false
    }
    if (matcher.method && response.request().method() !== matcher.method) {
      return false
    }
    return true
  }, { timeout: 60000 }).then(async (response) => {
    const payload = await response.json().catch(() => null)
    return { payload, status: response.status(), url: response.url(), action }
  })
}

async function waitForApiSuccess(page, matcher, action) {
  const result = await waitForApiPayload(page, matcher, action)
  assert.equal(result.status, 200, `${action} should return HTTP 200`)
  assert.ok(isSuccessPayload(result.payload), `${action} failed: ${JSON.stringify(result.payload)}`)
  return result.payload
}

async function waitForApiFailure(page, matcher, action, expectedMessage) {
  const result = await waitForApiPayload(page, matcher, action)
  assert.equal(result.status, 200, `${action} should return HTTP 200`)
  assert.ok(!isSuccessPayload(result.payload), `${action} should fail but returned success: ${JSON.stringify(result.payload)}`)
  if (expectedMessage) {
    const message = result.payload?.message || result.payload?.msg || JSON.stringify(result.payload)
    assert.match(message, expectedMessage, `${action} should expose expected failure`)
  }
  return result.payload
}

async function checkEligibility(page, row, expectedBlockedText) {
  const checkDialogPromise = page.locator('.el-dialog:visible .eligibility-panel').waitFor({ state: 'visible', timeout: 30000 }).catch(() => null)
  await row.getByRole('button', { name: '校验' }).click()
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const requestPromise = waitForApiSuccess(page, { path: '/srm/supplier-access/check', method: 'GET' }, '资格校验')
  await dialog.getByRole('button', { name: '执行校验' }).click()
  await requestPromise
  await checkDialogPromise
  await settle(page)
  const bodyText = await dialog.innerText()
  if (expectedBlockedText) {
    assert.match(bodyText, expectedBlockedText, `expected blocked text ${expectedBlockedText}, got ${bodyText}`)
  } else {
    assert.match(bodyText, /校验通过/, `expected eligibility success, got ${bodyText}`)
  }
  await dialog.getByRole('button', { name: '关闭' }).click()
}

async function resolveRiskRow(page, row, resolutionRemark) {
  await row.getByRole('button', { name: '处理' }).click()
  const resolveDialog = page.locator('.el-dialog:visible').last()
  await resolveDialog.waitFor({ state: 'visible', timeout: 30000 })
  await resolveDialog.locator('textarea').last().fill(resolutionRemark)
  const resolveRiskPromise = waitForApiSuccess(
    page,
    { path: '/srm/supplier-risk/resolve', method: 'PUT' },
    '处理供应商风险'
  )
  await resolveDialog.getByRole('button', { name: '提交' }).click()
  await resolveRiskPromise
  await settle(page)
}

async function resolveOpenSupplierRisks(page, authContext, resolutionPrefix) {
  const riskPage = await apiGetJson(
    page,
    `/admin-api/srm/supplier-risk/page?pageNo=1&pageSize=20&supplierName=${encodeURIComponent(config.supplierName)}`,
    authContext
  )
  const openRisks = (riskPage.data.list || []).filter(
    (item) => item.supplierId === config.supplierId && item.riskStatus === 'OPEN'
  )
  for (const risk of openRisks) {
    const riskRow = page.locator('.el-table__body-wrapper tbody tr').filter({
      hasText: risk.riskDescription || risk.sourceCode || config.supplierName
    }).first()
    await riskRow.waitFor({ state: 'visible', timeout: 30000 })
    await resolveRiskRow(page, riskRow, `${resolutionPrefix}-${risk.id}`)
    await page.goto(`${config.baseUrl}${config.riskPath}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page, 30000)
  }
  return openRisks
}

async function main() {
  assert.equal(config.tenant, '测试租户', `write E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.creatorUsername, 'aoteman', `write E2E must use aoteman as creator, got ${config.creatorUsername}`)
  assert.notEqual(config.auditorUsername, config.creatorUsername, 'auditor must differ from creator for self-audit gate')
  fs.mkdirSync(outputDir, { recursive: true })

  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const pageErrors = []
  const consoleErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })

  try {
    const creatorActor = {
      username: config.creatorUsername,
      password: config.creatorPassword
    }
    const auditorActor = {
      username: config.auditorUsername,
      password: config.auditorPassword
    }

    let loginResult = await login(page, creatorActor)
    ensureSrmPermissions(loginResult?.permissionPayload)

    let authContext = {
      authorization: loginResult?.authorization || '',
      tenantId: loginResult?.tenantId || ''
    }

    await openMenuPath(page, ['SRM', '供应商管理', '准入管理'], config.accessPath)
    await page.screenshot({ path: screenshots.access, fullPage: true })

    const accessListBefore = await apiGetJson(
      page,
      `/admin-api/srm/supplier-access/page?pageNo=1&pageSize=10&supplierName=${encodeURIComponent(config.supplierName)}`,
      authContext
    )
    let accessRecord = pickLatestRecord(accessListBefore.data.list, (item) => item.supplierId === config.supplierId)

    if (!accessRecord) {
      const createAccessPromise = waitForApiSuccess(
        page,
        { path: '/srm/supplier-access/create', method: 'POST' },
        '新增供应商准入'
      )
      await page.getByRole('button', { name: '新增准入' }).click()
      const accessDialog = page.locator('.el-dialog:visible').last()
      await accessDialog.waitFor({ state: 'visible', timeout: 30000 })
      await chooseRemoteSupplier(page, accessDialog, config.supplierKeyword, `${config.supplierName} (#${config.supplierId})`)
      const accessRemark = `T1准入申请-${Date.now()}`
      await accessDialog.locator('textarea').fill(accessRemark)
      await accessDialog.getByRole('button', { name: '保存' }).click()
      await createAccessPromise
      await settle(page)

      const accessPageAfterCreate = await apiGetJson(
        page,
        `/admin-api/srm/supplier-access/page?pageNo=1&pageSize=10&supplierName=${encodeURIComponent(config.supplierName)}`,
        authContext
      )
      accessRecord = pickLatestRecord(accessPageAfterCreate.data.list, (item) => item.supplierId === config.supplierId)
      assert.ok(accessRecord, 'created supplier access record must be queryable')
    }

    assert.ok(['PENDING', 'APPROVED'].includes(accessRecord.accessStatus), `unexpected access status ${accessRecord.accessStatus}`)
    assert.equal(accessRecord.enabled, true, 'supplier access should start enabled for T1 flow')

    let accessRow = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: config.supplierName }).first()
    await accessRow.waitFor({ state: 'visible', timeout: 30000 })

    if (accessRecord.accessStatus === 'PENDING') {
      await checkEligibility(page, accessRow, /待审核/)

      const selfAuditFailurePromise = waitForApiFailure(
        page,
        { path: '/srm/supplier-access/approve', method: 'PUT' },
        '提交人自审阻断',
        /提交人不能自审/
      )
      await accessRow.getByRole('button', { name: '通过' }).click()
      const selfAuditDialog = page.locator('.el-dialog:visible').last()
      await selfAuditDialog.waitFor({ state: 'visible', timeout: 30000 })
      await selfAuditDialog.locator('textarea').fill('T1自审阻断验证，预期失败')
      await selfAuditDialog.getByRole('button', { name: '提交' }).click()
      await selfAuditFailurePromise
      await settle(page)
      await closeDialog(selfAuditDialog)
      await settle(page)

      loginResult = await login(page, auditorActor)
      ensureSrmPermissions(loginResult?.permissionPayload)
      authContext = {
        authorization: loginResult?.authorization || '',
        tenantId: loginResult?.tenantId || ''
      }

      await openMenuPath(page, ['SRM', '供应商管理', '准入管理'], config.accessPath)
      accessRow = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: config.supplierName }).first()
      await accessRow.waitFor({ state: 'visible', timeout: 30000 })

      const approvePromise = waitForApiSuccess(
        page,
        { path: '/srm/supplier-access/approve', method: 'PUT' },
        '通过供应商准入'
      )
      await accessRow.getByRole('button', { name: '通过' }).click()
      const auditDialog = page.locator('.el-dialog:visible').last()
      await auditDialog.waitFor({ state: 'visible', timeout: 30000 })
      await auditDialog.locator('textarea').fill('T1真实审核通过，允许进入后续门禁')
      await auditDialog.getByRole('button', { name: '提交' }).click()
      await approvePromise
      await settle(page)

      const accessPageAfterApprove = await apiGetJson(
        page,
        `/admin-api/srm/supplier-access/page?pageNo=1&pageSize=10&supplierName=${encodeURIComponent(config.supplierName)}`,
        authContext
      )
      accessRecord = pickLatestRecord(accessPageAfterApprove.data.list, (item) => item.supplierId === config.supplierId)
      assert.ok(accessRecord, 'approved supplier access record must remain queryable')
      assert.equal(accessRecord.accessStatus, 'APPROVED')
    } else {
      loginResult = await login(page, auditorActor)
      ensureSrmPermissions(loginResult?.permissionPayload)
      authContext = {
        authorization: loginResult?.authorization || '',
        tenantId: loginResult?.tenantId || ''
      }
      await openMenuPath(page, ['SRM', '供应商管理', '准入管理'], config.accessPath)
      accessRow = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: config.supplierName }).first()
      await accessRow.waitFor({ state: 'visible', timeout: 30000 })
    }

    await checkEligibility(page, accessRow, null)

    const disablePromise = waitForApiSuccess(
      page,
      { path: '/srm/supplier-access/enable', method: 'PUT' },
      '停用供应商准入'
    )
    await accessRow.locator('.el-switch').click()
    await disablePromise
    await settle(page)
    await checkEligibility(page, accessRow, /停用/)

    const reEnablePromise = waitForApiSuccess(
      page,
      { path: '/srm/supplier-access/enable', method: 'PUT' },
      '恢复供应商准入'
    )
    await accessRow.locator('.el-switch').click()
    await reEnablePromise
    await settle(page)
    await checkEligibility(page, accessRow, null)

    await openMenuPathVariants(
      page,
      [
        ['SRM', '供应商管理', '风险管理'],
        ['SRM', '风险管理']
      ],
      config.riskPath
    )
    await page.screenshot({ path: screenshots.risk, fullPage: true })
    await resolveOpenSupplierRisks(page, authContext, 'T1历史残留风险清理')

    const createRiskPromise = waitForApiSuccess(
      page,
      { path: '/srm/supplier-risk/create', method: 'POST' },
      '新增供应商风险'
    )
    await page.getByRole('button', { name: '新增风险' }).click()
    const riskDialog = page.locator('.el-dialog:visible').last()
    await riskDialog.waitFor({ state: 'visible', timeout: 30000 })
    await chooseRemoteSupplier(page, riskDialog, config.supplierKeyword, `${config.supplierName} (#${config.supplierId})`)
    await fillNumberInput(riskDialog, '准入档案编号', accessRecord.id)
    await fillNumberInput(riskDialog, '来源编号', accessRecord.id)
    await riskDialog.locator('.el-form-item').filter({ hasText: '来源编码' }).locator('input').fill(`ACCESS-${accessRecord.id}`)
    await riskDialog.locator('.el-form-item').filter({ hasText: '来源名称' }).locator('input').fill(`准入申请-${config.supplierName}`)
    const riskDescription = `T1高风险阻断-${Date.now()}`
    const textareas = riskDialog.locator('textarea')
    await textareas.nth(0).fill(riskDescription)
    await textareas.nth(1).fill('真实 E2E 风险上报，用于验证未处理高风险阻断')
    await riskDialog.getByRole('button', { name: '保存' }).click()
    await createRiskPromise
    await settle(page)

    await openMenuPathVariants(
      page,
      [
        ['SRM', '供应商管理', '准入管理'],
        ['SRM', '准入管理']
      ],
      config.accessPath
    )
    const accessRowAfterRisk = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: config.supplierName }).first()
    await accessRowAfterRisk.waitFor({ state: 'visible', timeout: 30000 })
    await checkEligibility(page, accessRowAfterRisk, new RegExp(`ACCESS-${accessRecord.id}`))

    await openMenuPathVariants(
      page,
      [
        ['SRM', '供应商管理', '风险管理'],
        ['SRM', '风险管理']
      ],
      config.riskPath
    )
    const riskRow = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: riskDescription }).first()
    await riskRow.waitFor({ state: 'visible', timeout: 30000 })
    await resolveRiskRow(page, riskRow, 'T1真实 E2E 已补齐材料并复核通过')

    await openMenuPathVariants(
      page,
      [
        ['SRM', '供应商管理', '准入管理'],
        ['SRM', '准入管理']
      ],
      config.accessPath
    )
    const accessRowAfterResolve = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: config.supplierName }).first()
    await accessRowAfterResolve.waitFor({ state: 'visible', timeout: 30000 })
    await checkEligibility(page, accessRowAfterResolve, null)

    const accessFinal = await apiGetJson(
      page,
      `/admin-api/srm/supplier-access/page?pageNo=1&pageSize=10&supplierName=${encodeURIComponent(config.supplierName)}`,
      authContext
    )
    const riskFinal = await apiGetJson(
      page,
      `/admin-api/srm/supplier-risk/page?pageNo=1&pageSize=20&supplierName=${encodeURIComponent(config.supplierName)}`,
      authContext
    )
    const eligibilityFinal = await apiGetJson(
      page,
      `/admin-api/srm/supplier-access/check?supplierId=${config.supplierId}`,
      authContext
    )

    const finalAccessRecord = accessFinal.data.list.find((item) => item.supplierId === config.supplierId)
    const finalRiskRecord = riskFinal.data.list.find((item) => item.riskDescription === riskDescription)
    assert.ok(finalAccessRecord, 'final supplier access record must exist')
    assert.equal(finalAccessRecord.accessStatus, 'APPROVED')
    assert.equal(finalAccessRecord.enabled, true)
    assert.equal(Number(finalAccessRecord.openHighRiskCount || 0), 0)
    assert.ok(finalRiskRecord, 'final supplier risk record must exist')
    assert.equal(finalRiskRecord.riskStatus, 'RESOLVED')
    assert.equal(eligibilityFinal.data.eligible, true)
    assert.equal(Number(eligibilityFinal.data.openHighRiskCount || 0), 0)

    assert.deepEqual(pageErrors, [], `page errors detected: ${JSON.stringify(pageErrors)}`)
    assert.deepEqual(consoleErrors, [], `console errors detected: ${JSON.stringify(consoleErrors)}`)

    console.log(
      JSON.stringify(
        {
          ok: true,
          baseUrl: config.baseUrl,
          supplierId: config.supplierId,
          accessId: finalAccessRecord.id,
          riskId: finalRiskRecord.id,
          screenshots
        },
        null,
        2
      )
    )
  } catch (error) {
    await page.screenshot({ path: screenshots.failure, fullPage: true }).catch(() => null)
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
