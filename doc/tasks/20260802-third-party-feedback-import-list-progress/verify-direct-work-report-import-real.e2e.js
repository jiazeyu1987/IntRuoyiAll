const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(`Playwright is required: ${error.message}`)
  }
}

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {}
  const result = {}
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#') || !trimmed.includes('=')) continue
    const index = trimmed.indexOf('=')
    const key = trimmed.slice(0, index).trim()
    const value = trimmed.slice(index + 1).trim().replace(/^['"]|['"]$/g, '')
    result[key] = value
  }
  return result
}

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const frontendEnv = {
  ...readEnvFile(path.join(frontendRoot, '.env')),
  ...readEnvFile(path.join(frontendRoot, '.env.local'))
}

const config = {
  baseUrl: (process.env.MES_DIRECT_WORK_REPORT_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_DIRECT_WORK_REPORT_E2E_TENANT || '芋道源码',
  username: process.env.MES_DIRECT_WORK_REPORT_E2E_USERNAME || 'admin',
  password: process.env.MES_DIRECT_WORK_REPORT_E2E_PASSWORD || frontendEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
  uploadFile:
    process.env.MES_DIRECT_WORK_REPORT_E2E_UPLOAD_FILE || 'C:\\Users\\BJB110\\Desktop\\文档\\李萍.xlsx',
  headed: process.env.MES_DIRECT_WORK_REPORT_E2E_HEADED === '1',
  browserPath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
}
const FEEDBACK_STATUS_APPROVING = 2

if (!config.password) {
  throw new Error('Missing local default login password. Set MES_DIRECT_WORK_REPORT_E2E_PASSWORD for this run.')
}

function normalizeCacheValue(value) {
  if (value === undefined || value === null) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  if (typeof value === 'object') {
    if (Object.prototype.hasOwnProperty.call(value, 'value')) return normalizeCacheValue(value.value)
    if (Object.prototype.hasOwnProperty.call(value, 'data')) return normalizeCacheValue(value.data)
  }
  return ''
}

function readWsCacheValue(snapshot, key) {
  for (const candidate of [key, `vueuse_${key}`, `pro__${key}`, `yudao__${key}`]) {
    const raw = snapshot[candidate]
    if (!raw) continue
    try {
      const parsed = JSON.parse(raw)
      const normalized = normalizeCacheValue(parsed)
      if (normalized) return normalized
    } catch {
      const normalized = normalizeCacheValue(raw)
      if (normalized) return normalized
    }
  }
  return ''
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('Login captcha is enabled, blocking unattended real E2E.')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    const tenantResponsePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes('/system/tenant/get-id-by-name') &&
          response.url().includes(encodeURIComponent(config.tenant)) &&
          response.ok(),
        { timeout: 30000 }
      )
      .catch(() => null)
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
    await tenantResponsePromise
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[type="password"]'), config.password, 'password')

  const permissionPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  )
  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginBody.code), `login business failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  const permissionResponse = await permissionPromise
  const permissionBody = await permissionResponse.json()
  assert.ok([0, 200].includes(permissionBody.code), `permission business failed: ${permissionBody.msg || permissionBody.code}`)
  const headers = permissionResponse.request().headers()
  const authorization = headers.authorization || headers.Authorization || ''
  const tenantId = headers['tenant-id'] || headers['Tenant-Id'] || ''
  const visitTenantId = headers['visit-tenant-id'] || headers['Visit-Tenant-Id'] || ''
  assert.ok(authorization, 'authorization header is missing after login')
  assert.ok(tenantId, 'tenant-id header is missing after login')
  return { authorization, tenantId, visitTenantId }
}

async function buildAuthHeaders(page, authContext) {
  if (authContext?.authorization && authContext?.tenantId) {
    const headers = {
      Authorization: authContext.authorization,
      'tenant-id': String(authContext.tenantId)
    }
    if (authContext.visitTenantId) headers['visit-tenant-id'] = String(authContext.visitTenantId)
    return headers
  }
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const accessToken = readWsCacheValue(snapshot, 'ACCESS_TOKEN')
  const tenantId = readWsCacheValue(snapshot, 'tenantId')
  const visitTenantId = readWsCacheValue(snapshot, 'visitTenantId')
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function apiGet(page, url, params = {}, authContext = null) {
  const headers = await buildAuthHeaders(page, authContext)
  return page.evaluate(
    async ({ url, params, headers }) => {
      const query = new URLSearchParams()
      for (const [key, value] of Object.entries(params)) {
        if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
      }
      const response = await fetch(`/admin-api${url}${query.toString() ? `?${query}` : ''}`, {
        credentials: 'include',
        headers
      })
      const body = await response.json()
      return { status: response.status, body }
    },
    { url, params, headers }
  )
}

async function openFeedbackPage(page) {
  await page.goto(`${config.baseUrl}/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByRole('button', { name: /第三方导入/ }).waitFor({ state: 'visible', timeout: 30000 })
}

async function importDirectWorkReport(page) {
  await page.getByRole('button', { name: /第三方导入/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入报工' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[type="file"]').setInputFiles(path.resolve(config.uploadFile))
  const [importResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/feedback/import-direct-work-report-xlsx') &&
        response.request().method() === 'POST',
      { timeout: 120000 }
    ),
    dialog.getByRole('button', { name: /确 定/ }).click()
  ])
  assert.ok(importResponse.ok(), `import HTTP failed: ${importResponse.status()}`)
  const importBody = await importResponse.json()
  assert.ok([0, 200].includes(importBody.code), `import business failed: ${importBody.msg || importBody.code}`)
  const resultDialog = page.locator('.el-dialog:visible').filter({ hasText: '直接报工导入结果' }).last()
  await resultDialog.waitFor({ state: 'visible', timeout: 30000 })
  await resultDialog.getByRole('button', { name: /确 定|确定/ }).click().catch(() => {})
  await settle(page)
  return importBody.data || {}
}

function uniqueNumbers(values) {
  return [...new Set(values.map((value) => Number(value || 0)).filter((value) => Number.isFinite(value) && value > 0))]
}

function uniqueText(values) {
  return [...new Set(values.map((value) => String(value || '').trim()).filter(Boolean))]
}

async function main() {
  const { chromium } = loadPlaywright()
  const launchOptions = {
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  }
  if (config.browserPath) launchOptions.executablePath = config.browserPath
  const browser = await chromium.launch(launchOptions)
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    const authContext = await login(page)
    await openFeedbackPage(page)
    const importResult = await importDirectWorkReport(page)
    const details = Array.isArray(importResult.directWorkReportDetails) ? importResult.directWorkReportDetails : []
    const feedbackCodes = Array.isArray(importResult.feedbackCodes) ? importResult.feedbackCodes.filter(Boolean) : []
    const scheduleOrderCodes = uniqueText(details.map((detail) => detail.scheduleOrderCode))
    const importRecordIds = uniqueNumbers(importResult.importRecordIds || [])

    assert.ok(Number(importResult.submittedCount || 0) > 0, `direct import must submit formal feedback: ${JSON.stringify(importResult)}`)
    assert.ok(feedbackCodes.length > 0, `direct import must return formal feedback codes: ${JSON.stringify(importResult)}`)
    assert.ok(details.length > 0, `direct import must return formal direct work report details: ${JSON.stringify(importResult)}`)

    const feedbackRows = []
    for (const feedbackCode of feedbackCodes) {
      const feedbackPage = await apiGet(page, '/mes/pro/feedback/page', {
        pageNo: 1,
        pageSize: 10,
        code: feedbackCode,
        status: FEEDBACK_STATUS_APPROVING
      }, authContext)
      assert.equal(feedbackPage.status, 200, `feedback ${feedbackCode} list HTTP must be 200`)
      assert.ok([0, 200].includes(feedbackPage.body.code), `feedback ${feedbackCode} list business failed: ${JSON.stringify(feedbackPage.body)}`)
      const rows = feedbackPage.body.data?.list || []
      assert.ok(rows.some((row) => row.code === feedbackCode), `feedback list missing imported formal feedback code ${feedbackCode}`)
      const matchedRows = rows.filter((row) => row.code === feedbackCode)
      assert.ok(
        matchedRows.every((row) => Number(row.status) === FEEDBACK_STATUS_APPROVING),
        `feedback ${feedbackCode} must remain visible in approving list`
      )
      feedbackRows.push(...matchedRows)
    }

    const scheduleSnapshots = []
    for (const scheduleOrderCode of scheduleOrderCodes) {
      const orderPage = await apiGet(page, '/mes/pro/schedule-order/page', {
        pageNo: 1,
        pageSize: 10,
        code: scheduleOrderCode
      }, authContext)
      assert.equal(orderPage.status, 200, `schedule order ${scheduleOrderCode} page HTTP must be 200`)
      assert.ok([0, 200].includes(orderPage.body.code), `schedule order ${scheduleOrderCode} page business failed: ${JSON.stringify(orderPage.body)}`)
      const order = (orderPage.body.data?.list || []).find((row) => row.code === scheduleOrderCode)
      assert.ok(order, `schedule order page missing imported schedule order code ${scheduleOrderCode}`)
      const processList = await apiGet(page, '/mes/pro/schedule-order/process-list', {
        scheduleOrderId: order.id
      }, authContext)
      assert.equal(processList.status, 200, `schedule order ${scheduleOrderCode} process HTTP must be 200`)
      assert.ok([0, 200].includes(processList.body.code), `schedule order ${scheduleOrderCode} process business failed: ${JSON.stringify(processList.body)}`)
      const relatedDetails = details.filter((detail) => detail.scheduleOrderCode === scheduleOrderCode)
      for (const detail of relatedDetails) {
        const process = (processList.body.data || []).find((row) => row.processCode === detail.processCode)
        assert.ok(process, `schedule order ${scheduleOrderCode} missing process ${detail.processCode}`)
        assert.ok(
          Number(process.reportedQuantity || 0) >= Number(detail.afterReportedQuantity || 0),
          `schedule process ${scheduleOrderCode}/${detail.processCode} did not reflect imported progress`
        )
      }
      scheduleSnapshots.push({ order, processList: processList.body.data || [] })
    }

    const evidence = {
      status: 'PASS',
      tenant: config.tenant,
      username: config.username,
      uploadFile: config.uploadFile,
      submittedCount: importResult.submittedCount,
      importedCount: importResult.importedCount,
      feedbackCodes,
      importRecordIds,
      scheduleOrderCodes,
      feedbackListRows: feedbackRows.length,
      scheduleSnapshots: scheduleSnapshots.map(({ order, processList }) => ({
        id: order?.id,
        code: order?.code,
        completedQuantity: order?.completedQuantity,
        uncompletedQuantity: order?.uncompletedQuantity,
        progressPercent: order?.progressPercent,
        status: order?.status,
        processCount: processList.length
      }))
    }
    const artifactPath = path.join(__dirname, 'direct-work-report-real-e2e-result.json')
    fs.writeFileSync(artifactPath, JSON.stringify(evidence, null, 2), 'utf8')
    console.log(JSON.stringify(evidence, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
