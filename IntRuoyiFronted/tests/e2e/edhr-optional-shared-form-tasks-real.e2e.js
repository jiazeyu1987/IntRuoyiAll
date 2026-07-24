const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_TENANT || '测试租户',
  username: process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_USERNAME || 'aoteman',
  password: process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_PASSWORD || '111111',
  routeId: Number(process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_ROUTE_ID || '922074'),
  routeVersionId: Number(process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_ROUTE_VERSION_ID || '0'),
  setupCopyFromRouteId: Number(process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_SETUP_COPY_FROM_ROUTE_ID || '0'),
  setupRouteCodePrefix: process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_SETUP_ROUTE_CODE_PREFIX || 'E2E-OSF',
  requiredSlotType: process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_REQUIRED_SLOT || 'MAIN',
  requiredSharedFormKey:
    process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_REQUIRED_SHARED_KEY || 'e2e-main-shared',
  optionalSlotType: process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_OPTIONAL_SLOT || 'PROCESS_INSPECTION',
  optionalSharedFormKey:
    process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_SHARED_KEY || 'e2e-process-inspection-shared',
  signaturePassword:
    process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_SIGNATURE_PASSWORD ||
    process.env.EDHR_TRACKING_SIGNATURE_PASSWORD ||
    '',
  artifactDir:
    process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_ARTIFACT_DIR ||
    path.resolve(process.cwd(), 'tests/output/edhr-optional-shared-form-tasks-real')
}

const batchExecutionPath = '/mes/pro/feedback/edhr-batch-execution'
const taskStatusSkipped = 45
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
const setupScopes = {
  first: '{"ranges":[{"sourceTableIndex":0,"startRow":0,"endRow":1}]}',
  second: '{"ranges":[{"sourceTableIndex":0,"startRow":2,"endRow":3}]}'
}

if (config.baseUrl !== 'http://localhost:8081') {
  throw new Error(`BLOCKER: real E2E must use local frontend http://localhost:8081, got ${config.baseUrl}`)
}
if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(`BLOCKER: write E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`)
}
assert.ok(Number.isFinite(config.routeId) && config.routeId > 0, 'routeId must be positive')
if (!config.setupCopyFromRouteId) {
  assert.ok(Number.isFinite(config.routeVersionId), 'routeVersionId must be numeric when provided')
}

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeArtifact(name, payload) {
  ensureArtifactDir()
  fs.writeFileSync(path.join(config.artifactDir, name), `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function block(message, details = {}) {
  const error = new Error(`BLOCKER: ${message}`)
  error.blocked = true
  error.details = details
  return error
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 20000 }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(batchExecutionPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(batchExecutionPath)}`, {
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
    await tenantInput.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
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
      const headers = { 'Cache-Control': 'no-cache', Pragma: 'no-cache' }
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
      return { status: response.status, body: await response.json().catch(() => null) }
    },
    { method, apiPath, body }
  )
  assert.equal(result.status, 200, `HTTP error ${method} ${apiPath}: ${JSON.stringify(result.body)}`)
  assert.ok(
    result.body && (result.body.code === 0 || result.body.code === 200),
    `API error ${method} ${apiPath}: ${JSON.stringify(result.body)}`
  )
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

async function waitForApi(page, description, probe, timeout = 60000) {
  const startedAt = Date.now()
  let lastError
  while (Date.now() - startedAt < timeout) {
    try {
      const result = await probe()
      if (result) return result
    } catch (error) {
      if (error?.blocked) throw error
      lastError = error
    }
    await page.waitForTimeout(1000)
  }
  throw new Error(`timeout waiting for ${description}${lastError ? `: ${lastError.message}` : ''}`)
}

function visibleDialog(page, title) {
  return page.locator('.el-dialog:visible').filter({ hasText: title }).last()
}

async function gotoRouteList(page, query = {}) {
  const url = new URL('/mes/pro/route', config.baseUrl)
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && String(value) !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  let lastError
  for (let attempt = 1; attempt <= 2; attempt += 1) {
    await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
    try {
      await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
      await page.locator('.el-table').first().waitFor({ state: 'visible', timeout: 60000 })
      await settle(page)
      return
    } catch (error) {
      lastError = error
      await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => null)
      await settle(page)
    }
  }
  throw lastError
}

async function waitForRouteByCode(page, code) {
  return waitForApi(page, `route ${code}`, async () => {
    const pageData = await apiGet(page, '/mes/pro/route/page', {
      pageNo: 1,
      pageSize: 10,
      code
    })
    return (pageData?.list || []).find((route) => route.code === code)
  })
}

async function clickRouteListAction(page, routeCode, actionName) {
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: actionName, exact: true }).first().click()
}

async function copyRouteThroughUi(page, sourceRouteId) {
  const sourceRoute = await apiGet(page, '/mes/pro/route/get', { id: sourceRouteId })
  if (!sourceRoute?.code) {
    throw block('setup source route does not exist or has no code', { sourceRouteId, sourceRoute })
  }
  const timestamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
  const targetCode = `${config.setupRouteCodePrefix}-${timestamp}`
  const targetName = `可选共享表单E2E-${timestamp}`

  await gotoRouteList(page, { code: sourceRoute.code })
  await clickRouteListAction(page, sourceRoute.code, '复制')
  const dialog = visibleDialog(page, '复制工艺路线')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('.el-form-item').filter({ hasText: '副本编码' }).locator('input').fill(targetCode)
  await dialog.locator('.el-form-item').filter({ hasText: '副本名称' }).locator('input').fill(targetName)
  const copyResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/route/copy') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '确认复制', exact: true }).click()
  const copyResponse = await copyResponsePromise
  const copyPayload = await copyResponse.json().catch(() => null)
  assert.ok(copyPayload && (copyPayload.code === 0 || copyPayload.code === 200), `copy route failed: ${JSON.stringify(copyPayload)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => null)
  const copiedRoute = await waitForRouteByCode(page, targetCode)
  return {
    sourceRouteId,
    sourceRouteCode: sourceRoute.code,
    routeId: copiedRoute.id,
    routeCode: targetCode,
    routeName: targetName
  }
}

async function openRouteVersionWorkspace(page, routeCode) {
  await gotoRouteList(page, { code: routeCode })
  await clickRouteListAction(page, routeCode, '版本')
  const workspace = visibleDialog(page, '工艺路线版本')
  await workspace.waitFor({ state: 'visible', timeout: 60000 })
  await workspace.getByText('当前 ACTIVE：', { exact: false }).waitFor({ state: 'visible', timeout: 60000 })
  return workspace
}

function versionRow(workspace, versionNo) {
  return workspace.locator('.el-table__body-wrapper tbody tr').filter({ hasText: versionNo }).first()
}

async function clickVersionRowAction(workspace, versionNo, actionName) {
  const row = versionRow(workspace, versionNo)
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: actionName, exact: true }).first().click()
}

async function createRouteCandidateThroughUi(page, copiedRoute) {
  const workspace = await openRouteVersionWorkspace(page, copiedRoute.routeCode)
  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-version/create-candidate') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await workspace.getByRole('button', { name: '创建候选版本', exact: true }).click()
  const createResponse = await createResponsePromise
  const payload = await createResponse.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `create candidate failed: ${JSON.stringify(payload)}`)
  const candidate = payload.data
  await workspace.getByText(candidate.versionNo, { exact: true }).waitFor({ state: 'visible', timeout: 60000 })
  return candidate
}

function reportIdentity(report) {
  return report?.reportName || report?.reportCode || report?.reportId || '<unknown-report>'
}

async function loadReportReadiness(page, report) {
  const rules = await apiGet(page, '/mes/pro/batch-record-report/cell-rules', {
    reportId: report.reportId
  })
  return {
    reportId: report.reportId,
    reportCode: report.reportCode,
    reportName: report.reportName,
    batchRecordDefinitionId: report.batchRecordDefinitionId,
    batchRecordVersionId: report.batchRecordVersionId,
    versionStatus: report.versionStatus,
    sourceTableIndex: report.sourceTableIndex,
    formSlotType: report.formSlotType,
    unreviewedFillableCellCount: Number(rules?.unreviewedFillableCellCount || 0),
    reviewedRuleCount: Array.isArray(rules?.rules) ? rules.rules.length : 0,
    suggestionCount: Array.isArray(rules?.suggestions) ? rules.suggestions.length : 0
  }
}

async function loadTargetReport(page, formSlotType) {
  const inspected = []
  for (let pageNo = 1; pageNo <= 8; pageNo += 1) {
    const data = await apiGet(page, '/mes/pro/batch-record-report/page', {
      pageNo,
      pageSize: 20,
      formSlotType
    })
    const reports = Array.isArray(data?.list) ? data.list : []
    for (const report of reports) {
      if (!report?.reportId) continue
      try {
        const readiness = await loadReportReadiness(page, report)
        inspected.push({ report, readiness })
      } catch (error) {
        inspected.push({
          report,
          readiness: {
            reportId: report.reportId,
            reportCode: report.reportCode,
            reportName: report.reportName,
            batchRecordDefinitionId: report.batchRecordDefinitionId,
            batchRecordVersionId: report.batchRecordVersionId,
            versionStatus: report.versionStatus,
            sourceTableIndex: report.sourceTableIndex,
            formSlotType: report.formSlotType,
          },
          readinessError: error.message
        })
      }
    }
    if (reports.length < 20) break
  }
  const latestApprovedVersionByDefinition = new Map()
  for (const item of inspected) {
    const definitionId = item.readiness?.batchRecordDefinitionId
    const versionId = Number(item.readiness?.batchRecordVersionId || 0)
    if (!definitionId || item.readiness?.versionStatus !== 'APPROVED' || versionId <= 0) continue
    const previous = latestApprovedVersionByDefinition.get(definitionId) || 0
    if (versionId > previous) latestApprovedVersionByDefinition.set(definitionId, versionId)
  }
  for (const item of inspected) {
    const definitionId = item.readiness?.batchRecordDefinitionId
    const versionId = Number(item.readiness?.batchRecordVersionId || 0)
    const latestApprovedVersionId = definitionId
      ? latestApprovedVersionByDefinition.get(definitionId)
      : undefined
    if (
      item.report?.reportId &&
      definitionId &&
      versionId > 0 &&
      versionId === latestApprovedVersionId &&
      item.readiness?.versionStatus === 'APPROVED' &&
      item.readiness?.unreviewedFillableCellCount === 0
    ) {
      return { ...item.report, readiness: item.readiness }
    }
  }
  throw block(`no cell-rule-ready report found for formSlotType=${formSlotType}`, {
    formSlotType,
    inspected: inspected.map((item) => ({
      ...item.readiness,
      readinessError: item.readinessError,
      runtimeLatestApprovedVersionId: item.readiness?.batchRecordDefinitionId
        ? latestApprovedVersionByDefinition.get(item.readiness.batchRecordDefinitionId)
        : undefined
    }))
  })
}

function findFieldControl(binding, field, controlTag) {
  const selector = `[data-route-process-setting-field="${field}"]`
  return binding.locator(`${selector} ${controlTag}, ${controlTag}${selector}`).first()
}

async function selectReportForSlot(binding, page, formSlotType, report) {
  const select = binding.locator(`[data-route-process-setting-field="${formSlotSelectFields[formSlotType]}"]`).first()
  await select.waitFor({ state: 'visible', timeout: 30000 })
  await select.click()
  const input = select.locator('input').first()
  await input.fill(reportIdentity(report))
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  const option = dropdown
    .locator('.el-select-dropdown__item')
    .filter({ hasText: report.reportCode || report.reportName || report.reportId })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function selectBatchSharedScope(binding, page) {
  const select = binding.locator('[data-route-process-setting-field="shared-form-instance-scope"]').first()
  await select.waitFor({ state: 'visible', timeout: 30000 })
  await select.click()
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: '批次共享表单' }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function selectRequiredPolicy(binding, page, requiredPolicy) {
  const select = binding.locator('[data-route-process-setting-field="required-policy"]').first()
  await select.waitFor({ state: 'visible', timeout: 30000 })
  await select.click()
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: requiredPolicyLabels[requiredPolicy] }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function fillSharedBinding(page, routeProcessId, formSlotType, report, sharedFormKey, requiredPolicy, scopeJson) {
  const binding = page.locator(`[data-route-process-id="${routeProcessId}"][data-form-slot-type="${formSlotType}"]`)
  await binding.waitFor({ state: 'visible', timeout: 60000 })
  await selectReportForSlot(binding, page, formSlotType, report)
  await selectBatchSharedScope(binding, page)
  await selectRequiredPolicy(binding, page, requiredPolicy)
  await findFieldControl(binding, 'shared-form-key', 'input').fill(sharedFormKey)
  await findFieldControl(binding, 'fillable-scope-json', 'textarea').fill(scopeJson)

  const saveButton = page.locator(
    `[data-route-process-id="${routeProcessId}"][data-route-process-action="save-process-settings"]`
  )
  await saveButton.waitFor({ state: 'visible', timeout: 30000 })
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/batch-record/save') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await saveButton.click()
  const response = await saveResponsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `route binding save failed: ${JSON.stringify(payload)}`)
  await settle(page)
}

async function configureRouteBindingsThroughUi(page, copiedRoute, candidate) {
  const configs = await apiGet(page, '/mes/pro/route/flow-config', {
    routeId: copiedRoute.routeId,
    useType: 'BATCH',
    routeVersionId: candidate.id
  })
  const routeProcessIds = (configs || [])
    .map((row) => Number(row.routeProcessId))
    .filter((id) => Number.isFinite(id) && id > 0)
  if (routeProcessIds.length < 2) {
    throw block('copied route candidate does not expose at least two route processes for shared form range split', {
      routeId: copiedRoute.routeId,
      candidate,
      configs
    })
  }
  const [firstRouteProcessId, secondRouteProcessId] = routeProcessIds
  const mainReport = await loadTargetReport(page, config.requiredSlotType)
  const optionalReport = await loadTargetReport(page, config.optionalSlotType)
  const fillRules = await configureReportFillRulesThroughUi(page, [mainReport, optionalReport])
  const redirectPath =
    `/mes/pro/route/edit/${copiedRoute.routeId}?tab=process` +
    `&routeVersionId=${candidate.id}` +
    `&routeVersionNo=${encodeURIComponent(candidate.versionNo)}` +
    `&routeVersionStatus=${candidate.lifecycleStatus}`
  await page.goto(`${config.baseUrl}${redirectPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('.route-process-list__toolbar').waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  await fillSharedBinding(
    page,
    firstRouteProcessId,
    config.requiredSlotType,
    mainReport,
    config.requiredSharedFormKey,
    'REQUIRED',
    setupScopes.first
  )
  await fillSharedBinding(
    page,
    secondRouteProcessId,
    config.requiredSlotType,
    mainReport,
    config.requiredSharedFormKey,
    'REQUIRED',
    setupScopes.second
  )
  await fillSharedBinding(
    page,
    firstRouteProcessId,
    config.optionalSlotType,
    optionalReport,
    config.optionalSharedFormKey,
    'OPTIONAL',
    setupScopes.first
  )
  await fillSharedBinding(
    page,
    secondRouteProcessId,
    config.optionalSlotType,
    optionalReport,
    config.optionalSharedFormKey,
    'OPTIONAL',
    setupScopes.second
  )
  return {
    routeProcessIds: [firstRouteProcessId, secondRouteProcessId],
    mainReportId: mainReport.reportId,
    mainReportReadiness: mainReport.readiness,
    optionalReportId: optionalReport.reportId,
    optionalReportReadiness: optionalReport.readiness,
    fillRules
  }
}

async function ensureCopiedRouteHasKeyProcessThroughUi(page, copiedRoute, candidate, preferredRouteProcessId) {
  const before = await apiGet(page, '/mes/pro/route-process/list-by-route', { routeId: copiedRoute.routeId })
  const existingKeyProcess = (before || []).find((process) => process.keyFlag === true)
  if (existingKeyProcess) {
    return {
      routeProcessId: existingKeyProcess.id,
      processName: existingKeyProcess.processName,
      alreadyKey: true
    }
  }

  const target =
    (before || []).find((process) => Number(process.id) === Number(preferredRouteProcessId)) ||
    (before || []).slice().sort((left, right) => (left.sort || 0) - (right.sort || 0))[
      Math.max((before || []).length - 1, 0)
    ]
  if (!target?.id) {
    throw block('copied route has no route process available to mark as key process', {
      routeId: copiedRoute.routeId,
      routeProcesses: before
    })
  }

  const redirectPath =
    `/mes/pro/route/edit/${copiedRoute.routeId}?tab=process` +
    `&routeVersionId=${candidate.id}` +
    `&routeVersionNo=${encodeURIComponent(candidate.versionNo)}` +
    `&routeVersionStatus=${candidate.lifecycleStatus}`
  await page.goto(`${config.baseUrl}${redirectPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('.route-process-list__toolbar').waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)

  const saveSettingsButton = page.locator(
    `[data-route-process-action="save-process-settings"][data-route-process-id="${target.id}"]`
  )
  await saveSettingsButton.waitFor({ state: 'visible', timeout: 60000 })
  const row = saveSettingsButton.locator('xpath=ancestor::tr[1]')
  await row.getByRole('button', { name: '编辑', exact: true }).first().click()

  const dialog = visibleDialog(page, '编辑工序')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const keySwitch = dialog.locator('.el-form-item').filter({ hasText: '是否关键工序' }).locator('.el-switch').first()
  await keySwitch.waitFor({ state: 'visible', timeout: 30000 })
  const checked = await keySwitch.evaluate((node) => {
    const input = node.querySelector('input')
    return node.classList.contains('is-checked') || node.getAttribute('aria-checked') === 'true' || input?.checked === true
  })
  if (!checked) {
    await keySwitch.click()
  }

  const updateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process/update') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: /^确\s*定$/ }).click()
  const response = await updateResponsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `set key process failed: ${JSON.stringify(payload)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => null)
  await settle(page)

  const keyProcess = await waitForApi(page, `copied route ${copiedRoute.routeId} key process`, async () => {
    const routeProcesses = await apiGet(page, '/mes/pro/route-process/list-by-route', { routeId: copiedRoute.routeId })
    return (routeProcesses || []).find(
      (process) => Number(process.id) === Number(target.id) && process.keyFlag === true
    )
  }, 60000)
  return {
    routeProcessId: keyProcess.id,
    processName: keyProcess.processName,
    alreadyKey: false
  }
}

async function submitAndPublishRouteCandidateThroughUi(page, copiedRoute, candidate) {
  const workspace = await openRouteVersionWorkspace(page, copiedRoute.routeCode)
  if (!config.signaturePassword) {
    throw block('route candidate publish requires electronic signature password', {
      requiredEnv: ['EDHR_OPTIONAL_SHARED_FORM_E2E_SIGNATURE_PASSWORD', 'EDHR_TRACKING_SIGNATURE_PASSWORD'],
      routeId: copiedRoute.routeId,
      candidateId: candidate.id
    })
  }
  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-version/submit-publish') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickVersionRowAction(workspace, candidate.versionNo, '提交发布')
  const signatureDialog = page.locator('.el-message-box:visible').filter({ hasText: '电子签名发布' }).last()
  await signatureDialog.waitFor({ state: 'visible', timeout: 30000 })
  await signatureDialog.locator('input[type="password"]').fill(config.signaturePassword)
  await signatureDialog.getByRole('button', { name: '确认签名并发布', exact: true }).click()
  const submitResponse = await submitResponsePromise
  const submitPayload = await submitResponse.json().catch(() => null)
  assert.ok(
    submitPayload && (submitPayload.code === 0 || submitPayload.code === 200),
    `route submit-publish failed: ${JSON.stringify(submitPayload)}`
  )
  const active = await waitForApi(page, `route candidate ${candidate.id} ACTIVE`, async () => {
    const route = await apiGet(page, '/mes/pro/route/get', { id: copiedRoute.routeId })
    const versions = await apiGet(page, '/mes/pro/route-version/list-by-route', { routeId: copiedRoute.routeId })
    const currentCandidate = (versions || []).find((version) => Number(version.id) === Number(candidate.id))
    if (currentCandidate?.lifecycleStatus === 'PENDING_APPROVAL') {
      throw block('route candidate publish is pending approval, cannot use it as ACTIVE runtime route yet', {
        routeId: copiedRoute.routeId,
        candidate: currentCandidate,
        publishPayload
      })
    }
    const activeVersion = (versions || []).find(
      (version) => Number(version.id) === Number(candidate.id) && version.active === true && version.lifecycleStatus === 'ACTIVE'
    )
    return Number(route?.activeRouteVersionId) === Number(candidate.id) && activeVersion
      ? { route, activeVersion, publishPayload: submitPayload }
      : undefined
  }, 90000)
  await workspace.getByRole('button', { name: '关闭' }).click().catch(() => null)
  return { readyCandidate: submitPayload.data, active, signaturePasswordProvided: true }
}

async function enableRouteThroughUi(page, copiedRoute) {
  const before = await apiGet(page, '/mes/pro/route/get', { id: copiedRoute.routeId })
  if (before?.status === 0) {
    return { beforeStatus: before.status, afterStatus: before.status, alreadyEnabled: true }
  }
  await gotoRouteList(page, { code: copiedRoute.routeCode })
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: copiedRoute.routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const switchControl = row.locator('.el-switch').first()
  await switchControl.waitFor({ state: 'visible', timeout: 30000 })
  await switchControl.click()
  const confirm = page.locator('.el-message-box:visible').last()
  await confirm.waitFor({ state: 'visible', timeout: 30000 })
  const statusResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/mes/pro/route/update-status?id=${copiedRoute.routeId}`) &&
      response.url().includes('status=0') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await confirm.getByRole('button', { name: /确\s*定|确认/ }).click()
  const response = await statusResponsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `enable copied route failed: ${JSON.stringify(payload)}`)
  const enabled = await waitForApi(page, `copied route ${copiedRoute.routeId} enabled`, async () => {
    const route = await apiGet(page, '/mes/pro/route/get', { id: copiedRoute.routeId })
    return route?.status === 0 ? route : undefined
  })
  return { beforeStatus: before.status, afterStatus: enabled.status, alreadyEnabled: false }
}

async function ensureConfiguredRouteEnabledThroughUi(page) {
  const route = await apiGet(page, '/mes/pro/route/get', { id: config.routeId })
  if (!route?.code) {
    throw block('configured route does not exist or has no code', { routeId: config.routeId, route })
  }
  const enabled = await enableRouteThroughUi(page, {
    routeId: route.id,
    routeCode: route.code,
    routeName: route.name
  })
  return {
    routeId: route.id,
    routeCode: route.code,
    beforeStatus: enabled.beforeStatus,
    afterStatus: enabled.afterStatus,
    alreadyEnabled: enabled.alreadyEnabled
  }
}

async function setupCopiedRouteIfRequested(page) {
  if (!config.setupCopyFromRouteId) return null
  const copiedRoute = await copyRouteThroughUi(page, config.setupCopyFromRouteId)
  const candidate = await createRouteCandidateThroughUi(page, copiedRoute)
  const binding = await configureRouteBindingsThroughUi(page, copiedRoute, candidate)
  const keyProcess = await ensureCopiedRouteHasKeyProcessThroughUi(
    page,
    copiedRoute,
    candidate,
    binding.routeProcessIds[binding.routeProcessIds.length - 1]
  )
  const published = await submitAndPublishRouteCandidateThroughUi(page, copiedRoute, candidate)
  const enabled = await enableRouteThroughUi(page, copiedRoute)
  config.routeId = copiedRoute.routeId
  config.routeVersionId = candidate.id
  return { copiedRoute, candidate, binding, keyProcess, published, enabled }
}

async function assertRouteBindingReady(page) {
  const params = {
    routeId: config.routeId,
    useType: 'BATCH'
  }
  if (!config.setupCopyFromRouteId && config.routeVersionId) {
    params.routeVersionId = config.routeVersionId
  }
  const rows = await apiGet(page, '/mes/pro/route/flow-config', params)
  const configured = []
  for (const row of rows || []) {
    for (const report of row.batchRecordReports || []) {
      configured.push({ routeProcessId: row.routeProcessId, ...report })
    }
  }
  const required = configured.find(
    (item) => item.formSlotType === config.requiredSlotType && item.requiredPolicy === 'REQUIRED'
  )
  const optional = configured.find(
    (item) =>
      item.formSlotType === config.optionalSlotType &&
      item.requiredPolicy === 'OPTIONAL' &&
      item.instanceScope === 'BATCH_SHARED' &&
      item.sharedFormKey === config.optionalSharedFormKey
  )
  if (!required || !optional) {
    throw block('route binding precondition missing required anchor or optional shared form', {
      routeId: config.routeId,
      requiredSlotType: config.requiredSlotType,
      optionalSlotType: config.optionalSlotType,
      configured
    })
  }
  return { required, optional, configuredCount: configured.length }
}

async function findWorkOrderForRoute(page) {
  const explicitWorkOrderId = Number(process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_WORK_ORDER_ID || 0)
  const candidates = []
  for (let pageNo = 1; pageNo <= 8; pageNo += 1) {
    const pageData = await apiGet(page, '/mes/pro/work-order/page', {
      pageNo,
      pageSize: 20,
      temporaryFrozen: false
    })
    const list = Array.isArray(pageData?.list) ? pageData.list : []
    for (const workOrder of list) {
      if (!workOrder?.id) continue
      if (explicitWorkOrderId > 0 && Number(workOrder.id) !== explicitWorkOrderId) continue
      const routeOptions = await apiGet(page, '/mes/pro/edhr-batch-execution/work-order-route-options', {
        workOrderId: workOrder.id
      }).catch((error) => {
        candidates.push({ workOrderId: workOrder.id, routeOptionError: error.message })
        return []
      })
      candidates.push({
        workOrderId: workOrder.id,
        workOrderCode: workOrder.code,
        batchCode: workOrder.batchCode,
        routeOptions: (routeOptions || []).map((item) => item.routeId)
      })
      if ((routeOptions || []).some((item) => Number(item.routeId) === config.routeId)) {
        return { workOrder, routeOptions }
      }
    }
    if (list.length < 20) break
  }
  throw block('no selectable work order exposes the target route through the real route-options API', {
    routeId: config.routeId,
    candidates
  })
}

async function clickButton(root, name) {
  const button = root.getByRole('button', { name }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  if (await button.isDisabled()) {
    throw new Error(`button disabled: ${name}`)
  }
  await button.click()
}

async function selectDropdownOption(page, optionText) {
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function gotoBatchRecordFormList(page) {
  await page.goto(`${config.baseUrl}/mes/pro/batch-record-form-list`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.batch-record-form-page').waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.batch-record-form-page .el-table').first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
}

async function filterBatchRecordFormByReport(page, report) {
  const queryValue = report.reportCode || report.reportName || report.reportId
  if (!queryValue) {
    throw block('target report has no searchable code, name, or id for fill-rule UI setup', { report })
  }
  await gotoBatchRecordFormList(page)
  const quickFilter = page.locator('.batch-record-form-page .table-quick-filter').first()
  await quickFilter.waitFor({ state: 'visible', timeout: 30000 })
  await quickFilter.locator('.table-quick-filter__field .el-select__wrapper').click()
  await selectDropdownOption(page, '表单名称')
  await page.waitForTimeout(100)
  const valueInput = quickFilter.locator('.table-quick-filter__value input').first()
  await valueInput.waitFor({ state: 'visible', timeout: 30000 })
  await valueInput.fill(queryValue)

  const responsePromise = page.waitForResponse(
    (response) => {
      if (
        !response.url().includes('/admin-api/mes/pro/batch-record-report/page') ||
        response.request().method() !== 'GET'
      ) {
        return false
      }
      const url = new URL(response.url())
      return url.searchParams.get('name') === queryValue
    },
    { timeout: 60000 }
  )
  await quickFilter.getByRole('button', { name: '查询' }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `record form filter failed: ${JSON.stringify(payload)}`)
  const rows = Array.isArray(payload.data?.list) ? payload.data.list : []
  if (!rows.some((row) => row.reportId === report.reportId)) {
    throw block('target report is not visible after real record-form UI filtering', {
      reportId: report.reportId,
      queryValue,
      visibleReports: rows.map((row) => ({
        reportId: row.reportId,
        reportCode: row.reportCode,
        reportName: row.reportName,
        formSlotType: row.formSlotType,
        versionNo: row.versionNo
      }))
    })
  }
  await settle(page)
  const tableRows = page.locator('.batch-record-form-page .el-table__body-wrapper tbody tr')
  const row = tableRows.filter({ hasText: report.reportName || queryValue }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  return row
}

async function resolveCurrentUserCandidate(page) {
  const users = await apiGet(page, '/system/user/simple-list')
  const candidates = Array.isArray(users) ? users : []
  const user = candidates.find((item) => item.username === config.username) ||
    candidates.find((item) => item.nickname === config.username)
  if (!user?.id) {
    throw block('logged-in E2E user is missing from the real user selector options', {
      username: config.username,
      visibleUsers: candidates.map((item) => ({
        id: item.id,
        username: item.username,
        nickname: item.nickname
      }))
    })
  }
  return {
    id: Number(user.id),
    username: user.username,
    nickname: user.nickname,
    optionLabel: user.nickname || user.username || `用户 ${user.id}`
  }
}

async function clearMultiSelectTags(page, select) {
  for (let index = 0; index < 20; index += 1) {
    const close = select.locator('.el-tag .el-tag__close, .el-select__selection .el-tag__close').first()
    if ((await close.count()) === 0) return
    await close.click()
    await page.waitForTimeout(100)
  }
  throw new Error('failed to clear existing multi-select tags')
}

async function assertReportFillRuleReady(page, report, userCandidate) {
  const rule = await apiGet(page, '/mes/pro/edhr-process-form-permission-rule/get-by-report', {
    batchRecordReportId: report.reportId
  })
  const sourceIds = (rule?.fillRule?.candidateSourceIds || []).map((id) => Number(id))
  const candidateUsers = rule?.fillRule?.candidateUsers || []
  const userMatched =
    sourceIds.includes(Number(userCandidate.id)) ||
    candidateUsers.some((user) => Number(user.userId) === Number(userCandidate.id))
  if (rule?.fillRuleStatus !== 'CONFIGURED' || !userMatched) {
    throw block('record-form fill rule was not configured for the logged-in E2E user', {
      reportId: report.reportId,
      reportCode: report.reportCode,
      reportName: report.reportName,
      expectedUserId: userCandidate.id,
      rule
    })
  }
  return {
    batchRecordReportId: report.reportId,
    reportCode: report.reportCode,
    reportName: report.reportName,
    fillRuleStatus: rule.fillRuleStatus,
    candidateSourceType: rule.fillRule?.candidateSourceType,
    candidateSourceIds: sourceIds,
    candidateUsers: candidateUsers.map((user) => ({
      userId: user.userId,
      displayName: user.displayName
    }))
  }
}

async function configureReportFillRuleThroughUi(page, report, userCandidate) {
  const row = await filterBatchRecordFormByReport(page, report)
  await row.locator('.batch-record-form-filler-cell').first().click()
  const dialog = visibleDialog(page, '批记录表单填写人设置')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  const permissionFields = dialog.locator('.batch-record-form-permission-field')
  const sourceSelect = permissionFields.nth(0).locator('.el-select').first()
  await sourceSelect.locator('.el-select__wrapper').click()
  await selectDropdownOption(page, '个人')

  const candidateSelect = permissionFields.nth(1).locator('.el-select').first()
  await clearMultiSelectTags(page, candidateSelect)
  await candidateSelect.locator('.el-select__wrapper').click()
  const userOption = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: userCandidate.optionLabel })
    .first()
  await userOption.waitFor({ state: 'visible', timeout: 30000 })
  await userOption.click()

  const completionSelect = permissionFields.nth(2).locator('.el-select').first()
  await completionSelect.locator('.el-select__wrapper').click()
  await selectDropdownOption(page, '任一人完成')

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-process-form-permission-rule/save-by-report') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '保存填写设置' }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `save fill rule failed: ${JSON.stringify(payload)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => null)
  await settle(page)
  return assertReportFillRuleReady(page, report, userCandidate)
}

async function configureReportFillRulesThroughUi(page, reports) {
  const userCandidate = await resolveCurrentUserCandidate(page)
  const uniqueReports = Array.from(
    new Map(reports.filter((report) => report?.reportId).map((report) => [report.reportId, report])).values()
  )
  const configured = []
  for (const report of uniqueReports) {
    configured.push(await configureReportFillRuleThroughUi(page, report, userCandidate))
  }
  return {
    userCandidate,
    reports: configured
  }
}

async function openOrCreateBatchViaUi(page, workOrder) {
  const batchCode =
    process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_BATCH_CODE ||
    `OPT-SHARED-${config.routeId}-${Date.now()}`
  await page.goto(`${config.baseUrl}${batchExecutionPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批次执行编码', { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
  await clickButton(page, '打开/创建')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  const workOrderInput = dialog.locator('.el-select').nth(0).locator('input').first()
  await workOrderInput.waitFor({ state: 'visible', timeout: 30000 })
  await workOrderInput.click()
  await workOrderInput.fill(workOrder.code || String(workOrder.id))
  const routeOptionsResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/work-order-route-options') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await selectDropdownOption(page, workOrder.code || `ID ${workOrder.id}`)
  await routeOptionsResponsePromise

  const routeSelect = dialog.locator('.el-select').nth(1)
  const routeInput = routeSelect.locator('input').first()
  await routeInput.waitFor({ state: 'visible', timeout: 30000 })
  if (!(await routeInput.isDisabled().catch(() => false))) {
    await routeSelect.locator('.el-select__wrapper').click()
    await selectDropdownOption(page, `ID ${config.routeId}`)
  }

  await dialog.locator('input[placeholder="请输入真实批次号"]').first().fill(batchCode)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/open-or-create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickButton(dialog, '确 认')
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `open/create failed: ${JSON.stringify(payload)}`)
  await page.waitForURL((url) => url.pathname === `${batchExecutionPath}/detail`, { timeout: 60000 })
  await settle(page)
  return { batchCode, batch: payload.data }
}

function findOptionalTask(detail) {
  return (detail.tasks || []).find(
    (task) =>
      task.nodeType === 'ROUTE_FORM' &&
      task.formSlotType === config.optionalSlotType &&
      task.requiredPolicy === 'OPTIONAL' &&
      task.requiredFlag === false &&
      task.instanceScope === 'BATCH_SHARED' &&
      task.sharedFormKey === config.optionalSharedFormKey
  )
}

async function verifyOptionalTaskUi(page, optionalTask) {
  const processName = optionalTask.processName || optionalTask.processCode || String(optionalTask.routeProcessId)
  const group = page.locator('.edhr-batch-detail__process-task-group').filter({ hasText: processName }).first()
  await group.waitFor({ state: 'visible', timeout: 60000 })
  await group.locator('.edhr-batch-detail__process-task-group-head').click()
  const item = page.locator('.edhr-batch-detail__rail-process-form-item').filter({
    hasText: optionalTask.batchRecordReportName || optionalTask.batchRecordReportId || config.optionalSlotType
  }).first()
  await item.waitFor({ state: 'visible', timeout: 60000 })
  const itemText = await item.innerText()
  assert.match(itemText, /可选填写/, 'optional task must show 可选填写')
  const skipButton = item.getByRole('button', { name: '跳过表单' }).first()
  await skipButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await skipButton.isDisabled(), false, 'skip button must be enabled for active optional task')
  return { itemText, skipButton }
}

async function skipOptionalTask(page, skipButton, optionalTask, batchExecutionId) {
  await skipButton.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '跳过可选表单' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('textarea[placeholder*="跳过原因"]').first().fill(`E2E optional skip ${Date.now()}`)
  await dialog.locator('input[type="password"]').first().fill(config.signaturePassword)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/special-node/skip') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickButton(dialog, '签名并跳过')
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `skip failed: ${JSON.stringify(payload)}`)
  const latest = await apiGet(page, '/mes/pro/edhr-batch-execution/get', {
    id: batchExecutionId
  })
  const skippedTask = (latest.tasks || []).find((task) => Number(task.id) === Number(optionalTask.id))
  assert.ok(skippedTask, 'skipped task must still exist after skip')
  assert.equal(skippedTask.status, taskStatusSkipped, 'optional task must be SKIPPED after signing skip')
  return { latest, skippedTask }
}

async function main() {
  ensureArtifactDir()
  const browser = await chromium.launch({ headless: process.env.EDHR_OPTIONAL_SHARED_FORM_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1600, height: 980 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const mesWriteRequests = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      mesWriteRequests.push({ method: request.method(), url: request.url(), postData: request.postData() })
    }
  })

  const result = {
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    routeId: config.routeId,
    routeVersionId: config.routeVersionId,
    optionalSlotType: config.optionalSlotType,
    optionalSharedFormKey: config.optionalSharedFormKey,
    signaturePasswordProvided: Boolean(config.signaturePassword),
    pageErrors,
    mesWriteRequests
  }

  try {
    await login(page)
    result.setup = await setupCopiedRouteIfRequested(page)
    result.routeId = config.routeId
    result.routeVersionId = config.routeVersionId
    result.routeBinding = await assertRouteBindingReady(page)
    result.routeEnable = await ensureConfiguredRouteEnabledThroughUi(page)
    const workOrderMatch = await findWorkOrderForRoute(page)
    result.workOrder = {
      id: workOrderMatch.workOrder.id,
      code: workOrderMatch.workOrder.code,
      batchCode: workOrderMatch.workOrder.batchCode
    }
    const opened = await openOrCreateBatchViaUi(page, workOrderMatch.workOrder)
    result.createdBatch = {
      id: opened.batch?.id,
      batchExecutionCode: opened.batch?.batchExecutionCode,
      batchCode: opened.batchCode
    }
    const detail = await apiGet(page, '/mes/pro/edhr-batch-execution/get', { id: opened.batch.id })
    const optionalTask = findOptionalTask(detail)
    if (!optionalTask) {
      throw block('created/opened batch does not contain the configured optional shared route form task', {
        batchExecutionId: opened.batch.id,
        tasks: (detail.tasks || []).map((task) => ({
          id: task.id,
          routeProcessId: task.routeProcessId,
          formSlotType: task.formSlotType,
          requiredPolicy: task.requiredPolicy,
          requiredFlag: task.requiredFlag,
          instanceScope: task.instanceScope,
          sharedFormKey: task.sharedFormKey,
          status: task.status
        }))
      })
    }
    if (!optionalTask.activeWorkTaskId || !(optionalTask.allowedActions || []).includes('SKIP')) {
      throw block('optional shared form task exists but is not currently skippable by the logged-in user', {
        optionalTask
      })
    }
    result.optionalTask = {
      id: optionalTask.id,
      activeWorkTaskId: optionalTask.activeWorkTaskId,
      batchExecutionId: opened.batch.id,
      routeProcessId: optionalTask.routeProcessId,
      reportName: optionalTask.batchRecordReportName,
      allowedActions: optionalTask.allowedActions
    }
    const ui = await verifyOptionalTaskUi(page, optionalTask)
    result.optionalTask.uiText = ui.itemText
    await page.screenshot({ path: path.join(config.artifactDir, 'optional-skip-entry.png'), fullPage: true })
    if (!config.signaturePassword) {
      throw block('signature password env is missing, cannot complete the signed skip action', {
        requiredEnv: ['EDHR_OPTIONAL_SHARED_FORM_E2E_SIGNATURE_PASSWORD', 'EDHR_TRACKING_SIGNATURE_PASSWORD'],
        verifiedBeforeBlock: result.optionalTask
      })
    }
    const skipped = await skipOptionalTask(page, ui.skipButton, optionalTask, opened.batch.id)
    result.skippedTask = {
      id: skipped.skippedTask.id,
      status: skipped.skippedTask.status,
      requiredFlag: skipped.skippedTask.requiredFlag,
      requiredPolicy: skipped.skippedTask.requiredPolicy
    }
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)
    result.status = 'PASS'
    writeArtifact('optional-shared-form-task-result.json', result)
    process.stdout.write(`PASS: optional shared form task real E2E\n${JSON.stringify(result, null, 2)}\n`)
  } catch (error) {
    await page.screenshot({ path: path.join(config.artifactDir, 'optional-shared-form-task-failed.png'), fullPage: true }).catch(() => null)
    result.status = error.blocked ? 'BLOCKED' : 'FAIL'
    result.error = error.message
    result.details = error.details
    writeArtifact('optional-shared-form-task-result.json', result)
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
