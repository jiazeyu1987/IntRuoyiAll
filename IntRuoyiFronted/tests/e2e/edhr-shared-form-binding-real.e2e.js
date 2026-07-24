const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_SHARED_FORM_E2E_BASE_URL || 'http://127.0.0.1:8095').replace(/\/+$/, ''),
  tenant: process.env.EDHR_SHARED_FORM_E2E_TENANT || '测试租户',
  username: process.env.EDHR_SHARED_FORM_E2E_USERNAME || 'aoteman',
  password: process.env.EDHR_SHARED_FORM_E2E_PASSWORD || '111111',
  routeId: Number(process.env.EDHR_SHARED_FORM_E2E_ROUTE_ID || '922074'),
  routeVersionId: Number(process.env.EDHR_SHARED_FORM_E2E_ROUTE_VERSION_ID || '121'),
  routeVersionNo: process.env.EDHR_SHARED_FORM_E2E_ROUTE_VERSION_NO || 'V26',
  routeVersionStatus: process.env.EDHR_SHARED_FORM_E2E_ROUTE_VERSION_STATUS || 'DRAFT',
  firstRouteProcessId: Number(process.env.EDHR_SHARED_FORM_E2E_FIRST_ROUTE_PROCESS_ID || '922869'),
  secondRouteProcessId: Number(process.env.EDHR_SHARED_FORM_E2E_SECOND_ROUTE_PROCESS_ID || '922870'),
  reportId: process.env.EDHR_SHARED_FORM_E2E_REPORT_ID || 'ad9db334c68145318de2b67fc0d53d2a',
  formSlotType: process.env.EDHR_SHARED_FORM_E2E_FORM_SLOT_TYPE || 'PROCESS_INSPECTION',
  sharedFormKey: process.env.EDHR_SHARED_FORM_E2E_SHARED_KEY || 'e2e-process-inspection-shared',
  requiredPolicy: process.env.EDHR_SHARED_FORM_E2E_REQUIRED_POLICY || 'OPTIONAL',
  artifactDir:
    process.env.EDHR_SHARED_FORM_E2E_ARTIFACT_DIR ||
    path.resolve(process.cwd(), 'tests/output/edhr-shared-form-binding-real')
}

const formSlotSelectFields = {
  MAIN: 'batchRecordFormNames',
  LOSS_REPORT: 'lossReportFormNames',
  PROCESS_INSPECTION: 'processInspectionFormNames',
  PARAMETER_RECORD: 'parameterRecordFormNames'
}

const requiredPolicyLabels = {
  REQUIRED: '必填',
  OPTIONAL: '可选'
}

const scopes = {
  first: '{"ranges":[{"sourceTableIndex":0,"startRow":0,"endRow":1}]}',
  second: '{"ranges":[{"sourceTableIndex":0,"startRow":2,"endRow":3}]}'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(`shared form write E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`)
}
for (const [name, value] of Object.entries({
  routeId: config.routeId,
  routeVersionId: config.routeVersionId,
  firstRouteProcessId: config.firstRouteProcessId,
  secondRouteProcessId: config.secondRouteProcessId
})) {
  assert.ok(Number.isFinite(value) && value > 0, `${name} must be a positive number`)
}
assert.ok(formSlotSelectFields[config.formSlotType], `unsupported formSlotType: ${config.formSlotType}`)
assert.ok(requiredPolicyLabels[config.requiredPolicy], `unsupported requiredPolicy: ${config.requiredPolicy}`)

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeArtifact(name, payload) {
  ensureArtifactDir()
  fs.writeFileSync(path.join(config.artifactDir, name), `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 20000 }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page, redirectPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `login failed: ${JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function apiRequest(page, method, apiPath, body) {
  const result = await page.evaluate(
    async ({ method: requestMethod, apiPath: requestPath, body: requestBody }) => {
      const unwrap = (value) => {
        if (!value || typeof value !== 'object') return value
        for (const field of ['accessToken', 'value', 'v', 'data']) {
          if (Object.prototype.hasOwnProperty.call(value, field)) return unwrap(value[field])
        }
        return value
      }
      const readCache = (key) => {
        for (const storage of [localStorage, sessionStorage]) {
          const matchedKey = Object.keys(storage).find((item) => item === key || item.endsWith(key))
          if (!matchedKey) continue
          const raw = storage.getItem(matchedKey)
          if (!raw) continue
          try {
            const value = unwrap(JSON.parse(raw))
            if (typeof value === 'string' && value.startsWith('"') && value.endsWith('"')) {
              return value.slice(1, -1)
            }
            return value
          } catch {
            return raw.replace(/^"|"$/g, '')
          }
        }
        return undefined
      }

      const headers = {
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
      const accessToken = readCache('ACCESS_TOKEN')
      const tenantId = readCache('tenantId')
      if (accessToken) headers.Authorization = `Bearer ${accessToken}`
      if (tenantId) headers['tenant-id'] = String(tenantId)
      if (requestBody !== undefined) headers['Content-Type'] = 'application/json'

      const response = await fetch(`/admin-api${requestPath}`, {
        method: requestMethod,
        credentials: 'omit',
        headers,
        body: requestBody === undefined ? undefined : JSON.stringify(requestBody)
      })
      return {
        status: response.status,
        body: await response.json().catch(() => null)
      }
    },
    { method, apiPath, body }
  )
  assert.equal(result.status, 200, `HTTP error ${method} ${apiPath}: ${JSON.stringify(result.body)}`)
  assert.ok(result.body && (result.body.code === 0 || result.body.code === 200), `API error ${method} ${apiPath}: ${JSON.stringify(result.body)}`)
  return result.body.data
}

function buildQuery(params) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  }
  return search.toString()
}

async function apiGet(page, apiPath, params = {}) {
  const query = buildQuery(params)
  return apiRequest(page, 'GET', `${apiPath}${query ? `?${query}` : ''}`)
}

async function loadTargetReport(page) {
  const data = await apiGet(page, '/mes/pro/batch-record-report/page', {
    pageNo: 1,
    pageSize: 50,
    formSlotType: config.formSlotType
  })
  const reports = Array.isArray(data?.list) ? data.list : []
  const autoSelectReport = !config.reportId || config.reportId === 'AUTO'
  const report = autoSelectReport
    ? reports[0]
    : reports.find((item) => item.reportId === config.reportId)
  assert.ok(
    report,
    `target report ${config.reportId || 'AUTO'} not found in first 50 ${config.formSlotType} reports; real E2E cannot select it via the visible route binding dropdown`
  )
  config.reportId = report.reportId
  return report
}

async function assertPreconditions(page) {
  const batchConfigs = await apiGet(page, '/mes/pro/route/flow-config', {
    routeId: config.routeId,
    useType: 'BATCH',
    routeVersionId: config.routeVersionId
  })
  const routeProcessIds = new Set((batchConfigs || []).map((row) => Number(row.routeProcessId)))
  assert.ok(routeProcessIds.has(config.firstRouteProcessId), `missing first route process ${config.firstRouteProcessId}`)
  assert.ok(routeProcessIds.has(config.secondRouteProcessId), `missing second route process ${config.secondRouteProcessId}`)
  return batchConfigs
}

async function selectReport(binding, page, report) {
  const select = binding.locator(`[data-route-process-setting-field="${formSlotSelectFields[config.formSlotType]}"]`).first()
  await select.waitFor({ state: 'visible', timeout: 30000 })
  await select.click()
  const input = select.locator('input').first()
  await input.fill(report.reportName || report.reportCode || report.reportId)
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  const option = dropdown
    .locator('.el-select-dropdown__item')
    .filter({ hasText: report.reportCode || report.reportName || report.reportId })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await binding
    .locator('[data-route-process-setting-field="shared-form-instance-scope"]')
    .waitFor({ state: 'visible', timeout: 30000 })
}

async function selectBatchSharedScope(binding, page) {
  const select = binding.locator('[data-route-process-setting-field="shared-form-instance-scope"]').first()
  await select.waitFor({ state: 'visible', timeout: 30000 })
  await select.click()
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: '批次共享表单' }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await findFieldControl(binding, 'shared-form-key', 'input').waitFor({ state: 'visible', timeout: 30000 })
}

async function selectRequiredPolicy(binding, page) {
  const select = binding.locator('[data-route-process-setting-field="required-policy"]').first()
  await select.waitFor({ state: 'visible', timeout: 30000 })
  await select.click()
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  const option = dropdown
    .locator('.el-select-dropdown__item')
    .filter({ hasText: requiredPolicyLabels[config.requiredPolicy] })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

function findFieldControl(binding, field, controlTag) {
  const selector = `[data-route-process-setting-field="${field}"]`
  return binding
    .locator(`${selector} ${controlTag}, ${controlTag}${selector}`)
    .first()
}

async function fillSharedBinding(page, routeProcessId, scopeJson, report, diagnostics) {
  const binding = page.locator(
    `[data-route-process-id="${routeProcessId}"][data-form-slot-type="${config.formSlotType}"]`
  )
  await binding.waitFor({ state: 'visible', timeout: 60000 })
  await selectReport(binding, page, report)
  await selectBatchSharedScope(binding, page)
  await selectRequiredPolicy(binding, page)
  await findFieldControl(binding, 'shared-form-key', 'input').fill(config.sharedFormKey)
  await findFieldControl(binding, 'fillable-scope-json', 'textarea').fill(scopeJson)

  const saveButton = page.locator(
    `[data-route-process-id="${routeProcessId}"][data-route-process-action="save-process-settings"]`
  )
  await saveButton.waitFor({ state: 'visible', timeout: 30000 })
  diagnostics.buttonChecks.push({
    routeProcessId,
    count: await saveButton.count(),
    state: await saveButton
      .first()
      .evaluate((element) => ({
        tagName: element.tagName,
        text: element.textContent,
        disabled: element.hasAttribute('disabled'),
        ariaDisabled: element.getAttribute('aria-disabled'),
        className: element.getAttribute('class'),
        pointerEvents: getComputedStyle(element).pointerEvents
      }))
      .catch((error) => ({ error: error.message }))
  })
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/batch-record/save') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await saveButton.click()
  const response = await saveResponsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `save failed: ${JSON.stringify(payload)}`)
  await page.locator('.el-message:visible').filter({ hasText: '工序设置保存成功' }).first().waitFor({ timeout: 30000 }).catch(() => null)
  await settle(page)
}

async function verifySavedConfig(page) {
  const batchConfigs = await apiGet(page, '/mes/pro/route/flow-config', {
    routeId: config.routeId,
    useType: 'BATCH',
    routeVersionId: config.routeVersionId
  })
  const byProcess = new Map((batchConfigs || []).map((row) => [Number(row.routeProcessId), row]))
  const checks = [
    [config.firstRouteProcessId, scopes.first],
    [config.secondRouteProcessId, scopes.second]
  ]
  for (const [routeProcessId, expectedScope] of checks) {
    const row = byProcess.get(routeProcessId)
    assert.ok(row, `saved config missing routeProcessId=${routeProcessId}`)
    const report = (row.batchRecordReports || []).find((item) => item.formSlotType === config.formSlotType)
    assert.ok(report, `saved config missing ${config.formSlotType} binding for routeProcessId=${routeProcessId}`)
    assert.equal(report.batchRecordReportId, config.reportId)
    assert.equal(report.instanceScope, 'BATCH_SHARED')
    assert.equal(report.sharedFormKey, config.sharedFormKey)
    assert.equal(report.requiredPolicy, config.requiredPolicy)
    assert.deepEqual(JSON.parse(report.fillableScopeJson), JSON.parse(expectedScope))
  }
  return batchConfigs
}

async function main() {
  ensureArtifactDir()
  const redirectPath =
    `/mes/pro/route/edit/${config.routeId}?tab=process` +
    `&routeVersionId=${config.routeVersionId}` +
    `&routeVersionNo=${encodeURIComponent(config.routeVersionNo)}` +
    `&routeVersionStatus=${config.routeVersionStatus}`
  const browser = await chromium.launch({ headless: process.env.EDHR_SHARED_FORM_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1800, height: 980 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const mesWriteRequests = []
  const mesWriteResponses = []
  const diagnostics = { buttonChecks: [], mesWriteRequests, mesWriteResponses, pageErrors }
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      mesWriteRequests.push({
        method: request.method(),
        url: request.url(),
        postData: request.postData()
      })
    }
  })
  page.on('response', async (response) => {
    const request = response.request()
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      mesWriteResponses.push({
        method: request.method(),
        url: request.url(),
        status: response.status(),
        body: await response.text().catch((error) => `response body read failed: ${error.message}`)
      })
    }
  })

  try {
    await login(page, redirectPath)
    const targetReport = await loadTargetReport(page)
    const beforeConfig = await assertPreconditions(page)
    await page.goto(`${config.baseUrl}${redirectPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.locator('.route-process-list__toolbar').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)

    await fillSharedBinding(page, config.firstRouteProcessId, scopes.first, targetReport, diagnostics)
    await fillSharedBinding(page, config.secondRouteProcessId, scopes.second, targetReport, diagnostics)
    const afterConfig = await verifySavedConfig(page)
    await page.screenshot({ path: path.join(config.artifactDir, 'route-shared-binding.png'), fullPage: true })

    const result = {
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routeId: config.routeId,
      routeVersionId: config.routeVersionId,
      routeVersionNo: config.routeVersionNo,
      formSlotType: config.formSlotType,
      reportId: config.reportId,
      sharedFormKey: config.sharedFormKey,
      requiredPolicy: config.requiredPolicy,
      routeProcessIds: [config.firstRouteProcessId, config.secondRouteProcessId],
      scopes,
      beforeConfigCount: beforeConfig.length,
      afterConfigCount: afterConfig.length,
      mesWriteRequests,
      pageErrors
    }
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)
    writeArtifact('route-shared-binding-result.json', result)
    process.stdout.write(`edhr shared form binding real e2e passed\n${JSON.stringify(result, null, 2)}\n`)
  } catch (error) {
    await page.screenshot({ path: path.join(config.artifactDir, 'route-shared-binding-failed.png'), fullPage: true }).catch(() => null)
    writeArtifact('route-shared-binding-failure-diagnostics.json', {
      message: error.message,
      stack: error.stack,
      diagnostics
    })
    throw error
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
