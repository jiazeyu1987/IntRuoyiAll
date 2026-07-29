const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '..', '..')
const projectRoot = path.resolve(frontendRoot, '..')
const resultDir = path.join(frontendRoot, 'test-results', 'edhr-visual-fill-config-real-flow')
const resultFile = path.join(resultDir, 'result.json')
const cleanupResultFile = path.join(resultDir, 'cleanup-result.json')
const defaultConfigFile = path.join(resultDir, 'local-input.json')

const cliArgs = parseCliArgs(process.argv.slice(2))
const localInput = loadLocalInput(cliArgs.configPath || defaultConfigFile)
const config = {
  configPath: localInput.configPath,
  configLoadError: cliArgs.error || localInput.error || '',
  baseUrl: String(localInput.data.baseUrl || 'http://127.0.0.1:8083').replace(/\/+$/, ''),
  backendUrl: String(localInput.data.backendUrl || 'http://127.0.0.1:48083').replace(/\/+$/, ''),
  tenant: String(localInput.data.tenant || ''),
  adminUsername: String(localInput.data.accounts?.admin?.username || localInput.data.adminUsername || ''),
  adminPassword: String(localInput.data.accounts?.admin?.password || localInput.data.adminPassword || ''),
  employeeAUsername: String(localInput.data.accounts?.employeeA?.username || localInput.data.employeeAUsername || ''),
  employeeAPassword: String(localInput.data.accounts?.employeeA?.password || localInput.data.employeeAPassword || ''),
  employeeBUsername: String(localInput.data.accounts?.employeeB?.username || localInput.data.employeeBUsername || ''),
  employeeBPassword: String(localInput.data.accounts?.employeeB?.password || localInput.data.employeeBPassword || ''),
  batchRecordName: String(localInput.data.batchRecordName || localInput.data.reportName || ''),
  reportName: String(localInput.data.targetReportName || localInput.data.reportName || ''),
  allowWrite: localInput.data.allowWrite === true,
  configuredExistingWorkOrderCode: String(
    localInput.data.fixture?.existingWorkOrderCode || localInput.data.existingWorkOrderCode || ''
  ),
  routeProductName: String(
    localInput.data.fixture?.routeProductName || localInput.data.routeProductName || localInput.data.batchRecordName || ''
  ),
  sourceRouteCode: String(
    localInput.data.fixture?.sourceRouteCode || localInput.data.sourceRouteCode || ''
  ).trim(),
  preferredRouteCode: String(
    localInput.data.fixture?.preferredRouteCode || localInput.data.preferredRouteCode || ''
  ).trim(),
  targetProcessName: String(
    localInput.data.fixture?.targetProcessName || localInput.data.targetProcessName || ''
  ).trim(),
  fixtureProjectCode: String(localInput.data.fixture?.projectCode || 'CODXVFC20260726'),
  fixtureErpUnitNumber: String(localInput.data.fixture?.erpUnitNumber || 'Pcs'),
  fixtureWordFile: path.resolve(
    projectRoot,
    localInput.data.fixture?.sourceWordFile ||
      path.join('IntRuoyiBackend', 'yudao-module-mes', 'src', 'test', 'resources', 'fixtures', 'mes-batch-template-pilot.doc')
  ),
  erpSyncWaitMs: Number(localInput.data.fixture?.erpSyncWaitMs || 180000),
  erpSyncPollMs: Number(localInput.data.fixture?.erpSyncPollMs || 3000),
  headed: localInput.data.headed === true,
  chromeExecutable: String(localInput.data.chromeExecutable || '')
}

function parseCliArgs(argv) {
  const parsed = {}
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--cleanup-only') {
      parsed.cleanupOnly = true
    } else if (arg === '--config') {
      parsed.configPath = argv[index + 1]
      index += 1
      if (!parsed.configPath) {
        parsed.error = '--config requires a local JSON file path'
      }
    } else if (arg.startsWith('--config=')) {
      parsed.configPath = arg.slice('--config='.length)
    } else if (arg) {
      parsed.error = `unsupported_arg:${arg}`
    }
  }
  return parsed
}

function loadLocalInput(configPath) {
  const resolvedPath = path.resolve(configPath)
  if (!fs.existsSync(resolvedPath)) {
    return {
      configPath: resolvedPath,
      data: {},
      error: `missing local config file: ${resolvedPath}`
    }
  }
  try {
    const parsed = JSON.parse(fs.readFileSync(resolvedPath, 'utf8'))
    const data = parsed.data && typeof parsed.data === 'object' ? parsed.data : parsed
    return {
      configPath: resolvedPath,
      data,
      error: ''
    }
  } catch (error) {
    return {
      configPath: resolvedPath,
      data: {},
      error: `invalid local config JSON: ${error.message}`
    }
  }
}

function configuredSecrets() {
  return [
    config.adminPassword,
    config.employeeAPassword,
    config.employeeBPassword
  ].filter((secret) => typeof secret === 'string' && secret.length > 0)
}

function redactSecretText(value) {
  if (typeof value !== 'string') return value
  return configuredSecrets().reduce(
    (text, secret) => text.split(secret).join('<redacted>'),
    value
  )
}

function sanitizeEvidence(value, key = '') {
  if (value === null || value === undefined) return value
  if (typeof value === 'string') return redactSecretText(value)
  if (typeof value !== 'object') return value
  if (Array.isArray(value)) return value.map((item) => sanitizeEvidence(item))

  return Object.fromEntries(
    Object.entries(value).map(([entryKey, entryValue]) => {
      if (/password|token|secret/i.test(entryKey)) {
        return [entryKey, entryValue ? '<redacted>' : entryValue]
      }
      return [entryKey, sanitizeEvidence(entryValue, key ? `${key}.${entryKey}` : entryKey)]
    })
  )
}

function writeResult(result, outputFile = resultFile) {
  fs.mkdirSync(resultDir, { recursive: true })
  fs.writeFileSync(outputFile, `${JSON.stringify(sanitizeEvidence(result), null, 2)}\n`, 'utf8')
}

function block(message, details = {}) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function parsePort(url) {
  const parsed = new URL(url)
  return Number(parsed.port || (parsed.protocol === 'https:' ? 443 : 80))
}

function assertPairedWorktreeUrls() {
  const frontendPort = parsePort(config.baseUrl)
  const backendPort = parsePort(config.backendUrl)
  const frontendSlot = frontendPort - 8081
  const backendSlot = backendPort - 48081
  const isMainRuntime = frontendPort === 8081 && backendPort === 48081
  const isWorktreeRuntime = frontendSlot >= 1 && frontendSlot <= 19 && frontendSlot === backendSlot
  assert.ok(
    isMainRuntime || isWorktreeRuntime,
    `frontend/backend ports must be either int_main 8081/48081 or paired int_main worktree slot 1..19, got ${frontendPort}/${backendPort}`
  )
}

async function assertRuntimeReady() {
  const health = await fetch(`${config.backendUrl}/actuator/health`)
  assert.equal(health.status, 200, `backend health HTTP must be 200, got ${health.status}`)
  const healthBody = await health.json()
  assert.equal(healthBody.status, 'UP', `backend health must be UP, got ${JSON.stringify(healthBody)}`)
  const frontend = await fetch(`${config.baseUrl}/`)
  assert.ok(frontend.status >= 200 && frontend.status < 500, `frontend must respond, got ${frontend.status}`)
}

function collectMissingPreconditions() {
  const missing = []
  if (config.configLoadError) missing.push(config.configLoadError)
  if (!config.tenant) missing.push('local config tenant')
  if (!config.adminUsername) missing.push('local config accounts.admin.username')
  if (!config.adminPassword) missing.push('local config accounts.admin.password')
  if (!config.employeeAUsername) missing.push('local config accounts.employeeA.username')
  if (!config.employeeAPassword) missing.push('local config accounts.employeeA.password')
  if (!config.employeeBUsername) missing.push('local config accounts.employeeB.username')
  if (!config.employeeBPassword) missing.push('local config accounts.employeeB.password')
  if (!config.batchRecordName) missing.push('local config batchRecordName')
  if (!config.reportName) missing.push('local config targetReportName')
  if (!config.routeProductName) missing.push('local config fixture.routeProductName')
  if (!config.sourceRouteCode) missing.push('local config fixture.sourceRouteCode')
  if (!config.preferredRouteCode) missing.push('local config fixture.preferredRouteCode')
  if (!config.targetProcessName) missing.push('local config fixture.targetProcessName')
  if (config.preferredRouteCode && !config.preferredRouteCode.startsWith('CODX-VFC-')) {
    missing.push('local config fixture.preferredRouteCode must start with CODX-VFC-')
  }
  if (config.sourceRouteCode && config.sourceRouteCode === config.preferredRouteCode) {
    missing.push('local config sourceRouteCode and preferredRouteCode must be different')
  }
  if (!config.allowWrite) missing.push('local config allowWrite=true')
  if (!config.configuredExistingWorkOrderCode && !config.batchRecordName.startsWith('CODX-VFC-')) {
    missing.push('task-owned CODX-VFC-* batch record fixture or local config fixture.existingWorkOrderCode')
  }
  if (!fs.existsSync(config.fixtureWordFile)) missing.push(`fixture Word file: ${config.fixtureWordFile}`)
  return missing
}

function resolveWordImportRouteKey(fileName) {
  if (/[（(\s-]E\s*1[）)\s-]/i.test(` ${fileName} `)) {
    return 'E'
  }
  return 'B'
}

function buildTimestamp() {
  return new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
}

function toLocalApiDateTime(date) {
  return date.toISOString().slice(0, 19)
}

function buildPlannedDateTime(daysFromToday, hour) {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate() + daysFromToday, hour, 0, 0)
}

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function resolveTenantId() {
  const url = new URL('/admin-api/system/tenant/get-id-by-name', config.backendUrl)
  url.searchParams.set('name', config.tenant)
  const response = await fetch(url)
  assert.equal(response.status, 200, `tenant lookup HTTP failed:${response.status}`)
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `tenant lookup failed:${body.msg || body.code}`)
  const tenantId = Number(body.data)
  assert.ok(Number.isInteger(tenantId) && tenantId > 0, `tenant lookup returned invalid id:${body.data}`)
  return tenantId
}

function createAuthHeaders(accessToken, tenantId, extra = {}) {
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'visit-tenant-id': String(tenantId),
    ...extra
  }
}

async function fetchCommonJson(apiPath, auth, options = {}) {
  const url = new URL(`/admin-api${apiPath}`, config.backendUrl)
  const response = await fetch(url, {
    ...options,
    headers: {
      ...createAuthHeaders(auth.accessToken, auth.tenantId, options.headers || {})
    }
  })
  const body = await response.json().catch((error) => ({ parseError: error.message }))
  assert.equal(response.status, 200, `${apiPath} HTTP failed:${response.status}`)
  assert.ok([0, 200].includes(Number(body.code)), `${apiPath} failed:${body.msg || body.code}`)
  return body.data
}

async function findTaskOwnedReport(auth) {
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '50',
    latestVersionOnly: 'true',
    batchRecordName: config.batchRecordName
  })
  const data = await fetchCommonJson(`/mes/pro/batch-record-report/page?${params.toString()}`, auth)
  const rows = Array.isArray(data?.list) ? data.list : []
  const matched = rows.find((row) => row.batchRecordName === config.batchRecordName && row.reportName === config.reportName)
  if (matched) return matched
  if (rows.length) {
    throw block('task_owned_target_report_not_found', {
      batchRecordName: config.batchRecordName,
      targetReportName: config.reportName,
      availableReportNames: rows.map((row) => row.reportName).filter(Boolean)
    })
  }
  return undefined
}

async function ensureDccProjectCode(auth, evidence) {
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '10',
    projectName: config.routeProductName
  })
  const existingPage = await fetchCommonJson(`/dcc/project-codes/page?${params.toString()}`, auth)
  const existingRows = Array.isArray(existingPage?.list) ? existingPage.list : []
  const existing = existingRows.find(
    (row) => row.projectName === config.routeProductName && row.projectCode === config.fixtureProjectCode
  )
  if (existing) {
    evidence.fixtureSetup.projectCode = {
      action: 'reuse',
      id: existing.id,
      projectName: existing.projectName,
      projectCode: existing.projectCode
    }
    return existing.id
  }
  if (!config.routeProductName.startsWith('CODX-VFC-')) {
    throw block('route_product_dcc_project_code_missing', {
      routeProductName: config.routeProductName,
      projectCode: config.fixtureProjectCode,
      availableProjectCodes: existingRows.map((row) => ({
        id: row.id,
        projectName: row.projectName,
        projectCode: row.projectCode,
        status: row.status
      }))
    })
  }

  const createdId = await fetchCommonJson('/dcc/project-codes/create', auth, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      projectName: config.routeProductName,
      projectCode: config.fixtureProjectCode,
      category: 'CODX-E2E',
      priority: 'E2E',
      status: 'ENABLE'
    })
  })
  evidence.fixtureSetup.projectCode = {
    action: 'create',
    id: createdId,
    projectName: config.routeProductName,
    projectCode: config.fixtureProjectCode
  }
  return createdId
}

async function validateRouteProductFixturePreconditions(auth, evidence) {
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '10',
    code: config.fixtureProjectCode
  })
  const itemPage = await fetchCommonJson(`/mes/md/item/page?${params.toString()}`, auth)
  const itemRows = Array.isArray(itemPage?.list) ? itemPage.list : []
  const exactItem = itemRows.find((row) => row.code === config.fixtureProjectCode)
  if (!exactItem) {
    evidence.fixtureSetup.routeProduct = {
      status: 'pending_import_create',
      routeProductName: config.routeProductName,
      projectCode: config.fixtureProjectCode
    }
    return
  }
  if (exactItem.name !== config.routeProductName) {
    throw block('route_product_mes_item_name_mismatch', {
      routeProductName: config.routeProductName,
      projectCode: config.fixtureProjectCode,
      mesItemName: exactItem.name,
      mesItemId: exactItem.id
    })
  }
  if (exactItem.batchFlag !== true) {
    throw block('route_product_mes_item_not_batch_enabled', {
      routeProductName: config.routeProductName,
      projectCode: config.fixtureProjectCode,
      mesItemId: exactItem.id,
      batchFlag: exactItem.batchFlag
    })
  }
  evidence.fixtureSetup.routeProduct = {
    status: 'validated',
    routeProductName: config.routeProductName,
    projectCode: config.fixtureProjectCode,
    mesItemId: exactItem.id,
    batchFlag: exactItem.batchFlag
  }
}

async function importTaskOwnedReport(auth, evidence) {
  const form = new FormData()
  const fileName = path.basename(config.fixtureWordFile)
  const fileBuffer = fs.readFileSync(config.fixtureWordFile)
  const routeKey = resolveWordImportRouteKey(fileName)
  const preflightParams = new URLSearchParams({
    routeKey,
    batchRecordName: config.batchRecordName
  })
  preflightParams.append('productNames', config.routeProductName)
  const preflight = await fetchCommonJson(
    `/mes/pro/batch-record-report/recognize-uploaded/preflight?${preflightParams.toString()}`,
    auth
  )
  const importAction = String(preflight?.recommendedAction || 'REBUILD_V1')
  const routeUpgradeConfirmed = Boolean(preflight?.routeUpgradeRequired && preflight?.currentRouteId)
  form.append('file', new Blob([fileBuffer], { type: 'application/msword' }), fileName)
  form.append('routeKey', routeKey)
  form.append('batchRecordName', config.batchRecordName)
  form.append('upgrade', String(importAction === 'UPGRADE'))
  form.append('importAction', importAction)
  if (importAction === 'UPGRADE' && preflight?.currentBatchRecordVersionId !== undefined) {
    form.append('expectedSourceVersionId', String(preflight.currentBatchRecordVersionId))
  }
  if (importAction === 'UPGRADE' && preflight?.nextVersionNo) {
    form.append('expectedTargetVersionNo', String(preflight.nextVersionNo))
  }
  form.append('routeUpgradeConfirmed', String(routeUpgradeConfirmed))
  if (routeUpgradeConfirmed && preflight?.currentRouteId !== undefined) {
    form.append('expectedRouteId', String(preflight.currentRouteId))
  }
  if (routeUpgradeConfirmed && preflight?.currentRouteVersionId !== undefined) {
    form.append('expectedRouteVersionId', String(preflight.currentRouteVersionId))
  }
  form.append('rebuildBatchRecord', 'true')
  form.append('productNames', config.routeProductName)
  form.append('selectedProductNames', config.routeProductName)

  const response = await fetch(`${config.backendUrl}/admin-api/mes/pro/batch-record-report/recognize-uploaded`, {
    method: 'POST',
    headers: createAuthHeaders(auth.accessToken, auth.tenantId),
    body: form,
    signal: AbortSignal.timeout(10 * 60 * 1000)
  })
  const body = await response.json().catch((error) => ({ parseError: error.message }))
  assert.equal(response.status, 200, `recognize-uploaded HTTP failed:${response.status}`)
  assert.ok([0, 200].includes(Number(body.code)), `recognize-uploaded failed:${body.msg || body.code}`)
  const data = body.data || {}
  const reports = Array.isArray(data.reports) ? data.reports : []
  if (!reports.length) {
    throw block('task_owned_report_import_returned_no_reports', {
      batchRecordName: config.batchRecordName,
      sourceWordFile: config.fixtureWordFile
    })
  }
  evidence.fixtureSetup.importPreflight = {
    recommendedAction: preflight?.recommendedAction,
    importAction,
    routeUpgradeRequired: preflight?.routeUpgradeRequired,
    routeUpgradeConfirmed,
    currentRouteId: preflight?.currentRouteId,
    currentRouteVersionId: preflight?.currentRouteVersionId,
    currentBatchRecordVersionId: preflight?.currentBatchRecordVersionId,
    nextVersionNo: preflight?.nextVersionNo
  }
  const primaryReport = reports.find((report) => report.reportName === config.reportName)
  if (!primaryReport) {
    throw block('task_owned_target_report_not_imported', {
      batchRecordName: config.batchRecordName,
      targetReportName: config.reportName,
      importedReportNames: reports.map((report) => report.reportName).filter(Boolean)
    })
  }
  evidence.fixtureSetup.importResult = {
    importedCount: data.importedCount,
    createdCount: data.createdCount,
    updatedCount: data.updatedCount,
    batchRecordDefinitionId: data.batchRecordDefinitionId,
    batchRecordVersionId: data.batchRecordVersionId,
    reportIds: reports.map((report) => report.reportId).filter(Boolean),
    sourceWordFile: config.fixtureWordFile
  }
  return {
    ...primaryReport,
    reportName: primaryReport.reportName || config.reportName,
    batchRecordName: primaryReport.batchRecordName || config.batchRecordName
  }
}

async function ensureTaskOwnedReportFixtureCanBindPermissionScope(auth, report, evidence) {
  const permission = await fetchCommonJson(
    `/mes/pro/edhr-process-form-permission-rule/get-by-report?batchRecordReportId=${encodeURIComponent(report.reportId)}`,
    auth
  )
  const permissionScopeId = permission?.permissionScopeId
  if (!permissionScopeId) {
    evidence.fixtureSetup.permissionScope = {
      action: 'not_bound',
      reportId: report.reportId
    }
    return true
  }
  const url = new URL('/admin-api/mes/pro/edhr-permission-scopes/get', config.backendUrl)
  url.searchParams.set('scopeId', String(permissionScopeId))
  const response = await fetch(url, {
    headers: createAuthHeaders(auth.accessToken, auth.tenantId)
  })
  const body = await response.json().catch((error) => ({ parseError: error.message }))
  const usable = response.status === 200 && [0, 200].includes(Number(body.code))
  evidence.fixtureSetup.permissionScope = {
    action: usable ? 'reuse' : 'invalid_rebuild_required',
    reportId: report.reportId,
    permissionScopeId,
    status: response.status,
    code: body.code,
    message: body.msg || body.message || body.parseError || ''
  }
  return usable
}

async function deleteTaskOwnedReportFixture(auth, evidence) {
  const params = new URLSearchParams({
    batchRecordName: config.batchRecordName,
    forceUnbind: 'true'
  })
  const data = await fetchCommonJson(
    `/mes/pro/batch-record-report/delete-by-batch-record-name?${params.toString()}`,
    auth,
    { method: 'DELETE' }
  )
  evidence.fixtureSetup.rebuild = {
    action: 'delete_invalid_task_owned_fixture',
    batchRecordName: config.batchRecordName,
    deleteResult: data
  }
}

async function ensureTaskOwnedReportFixture(auth, evidence) {
  evidence.fixtureSetup = {
    batchRecordName: config.batchRecordName,
    routeProductName: config.routeProductName,
    projectCode: config.fixtureProjectCode,
    existingWorkOrderCode: config.configuredExistingWorkOrderCode || null,
    targetReportName: config.reportName,
    source: config.configuredExistingWorkOrderCode
      ? 'official_existing_work_order_report_fixture'
      : 'official_admin_api_setup_for_task_owned_e2e_fixture'
  }
  let existingReport = await findTaskOwnedReport(auth)
  if (existingReport) {
    if (await ensureTaskOwnedReportFixtureCanBindPermissionScope(auth, existingReport, evidence)) {
      evidence.fixtureSetup.report = {
        action: 'reuse',
        reportId: existingReport.reportId,
        reportName: existingReport.reportName,
        batchRecordName: existingReport.batchRecordName
      }
      return existingReport
    }
    if (config.configuredExistingWorkOrderCode) {
      throw block('existing_work_order_report_permission_scope_invalid', {
        batchRecordName: config.batchRecordName,
        targetReportName: config.reportName,
        reportId: existingReport.reportId,
        permissionScope: evidence.fixtureSetup.permissionScope
      })
    }
    await deleteTaskOwnedReportFixture(auth, evidence)
    existingReport = undefined
  }
  if (config.configuredExistingWorkOrderCode) {
    throw block('existing_work_order_report_fixture_missing', {
      batchRecordName: config.batchRecordName,
      targetReportName: config.reportName,
      existingWorkOrderCode: config.configuredExistingWorkOrderCode
    })
  }
  await ensureDccProjectCode(auth, evidence)
  await validateRouteProductFixturePreconditions(auth, evidence)
  const importedReport = await importTaskOwnedReport(auth, evidence)
  evidence.fixtureSetup.report = {
    action: 'import',
    reportId: importedReport.reportId,
    reportName: importedReport.reportName,
    batchRecordName: importedReport.batchRecordName || config.batchRecordName
  }
  return importedReport
}

async function captureVisualFillConfigBackup(auth, report, evidence) {
  const [cellRules, permission] = await Promise.all([
    fetchCommonJson(`/mes/pro/batch-record-report/cell-rules?reportId=${encodeURIComponent(report.reportId)}`, auth),
    fetchCommonJson(
      `/mes/pro/edhr-process-form-permission-rule/get-by-report?batchRecordReportId=${encodeURIComponent(report.reportId)}`,
      auth
    )
  ])
  const backup = { reportId: report.reportId, cellRules, permission }
  evidence.fixtureSetup.configBackup = {
    action: 'capture_before_visual_fill_config',
    reportId: report.reportId,
    ruleCount: Array.isArray(cellRules?.rules) ? cellRules.rules.length : 0,
    assistRowCount: Array.isArray(cellRules?.assistRows) ? cellRules.assistRows.length : 0,
    fillRuleStatus: permission?.fillRuleStatus,
    fillAssignmentCount: Array.isArray(permission?.fillAssignments) ? permission.fillAssignments.length : 0
  }
  return backup
}

async function restoreVisualFillConfigFixture(auth, backup, evidence) {
  if (!backup?.reportId) return
  const cellRules = backup.cellRules || {}
  const rulesForRestore = Array.isArray(cellRules.rules) ? cellRules.rules : []
  const originalAssistRows = Array.isArray(cellRules.assistRows) ? cellRules.assistRows : []
  const assistRowsForRestore = originalAssistRows.length > 0 ? originalAssistRows : null
  await fetchCommonJson('/mes/pro/batch-record-report/cell-rules', auth, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      reportId: backup.reportId,
      rules: rulesForRestore,
      assistRows: assistRowsForRestore
    })
  })
  const permission = backup.permission || {}
  const fillAssignments = Array.isArray(permission.fillAssignments) ? permission.fillAssignments : []
  if (fillAssignments.length) {
    await fetchCommonJson('/mes/pro/edhr-process-form-permission-rule/save-by-report', auth, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        batchRecordReportId: backup.reportId,
        fillAssignments
      })
    })
  } else if (permission.fillRule) {
    await fetchCommonJson('/mes/pro/edhr-process-form-permission-rule/save-by-report', auth, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        batchRecordReportId: backup.reportId,
        fillRule: permission.fillRule
      })
    })
  } else {
    throw block('visual_fill_config_restore_permission_requires_existing_rule', {
      reportId: backup.reportId,
      originalFillRuleStatus: permission.fillRuleStatus
    })
  }
  evidence.fixtureSetup.configRestore = {
    action: 'restoreVisualFillConfigFixture',
    reportId: backup.reportId,
    restoredRuleCount: rulesForRestore.length,
    restoredAssistRowCount: originalAssistRows.length,
    restoredFillRuleStatus: permission.fillRuleStatus
  }
}

async function resolveUserIds(auth) {
  const users = await fetchCommonJson('/system/user/simple-list', auth)
  const userRows = Array.isArray(users) ? users : []
  const findUser = (username) =>
    userRows.find((user) => user.username === username || user.nickname === username)
  const employeeA = findUser(config.employeeAUsername)
  const employeeB = findUser(config.employeeBUsername)
  const missing = []
  if (!employeeA?.id) missing.push(config.employeeAUsername)
  if (!employeeB?.id) missing.push(config.employeeBUsername)
  if (missing.length) {
    throw block('configured_employee_user_missing', {
      missingUsernames: missing,
      availableUsernames: userRows.map((user) => user.username).filter(Boolean)
    })
  }
  return {
    employeeA: Number(employeeA.id),
    employeeB: Number(employeeB.id)
  }
}

function uniqueRulesForSave(cellRules) {
  const nextRules = new Map()
  for (const sourceRule of [...(cellRules.rules || []), ...(cellRules.suggestions || [])]) {
    const rowIndex = Number(sourceRule.rowIndex)
    const columnIndex = Number(sourceRule.columnIndex)
    if (!Number.isInteger(rowIndex) || !Number.isInteger(columnIndex)) continue
    nextRules.set(`${rowIndex}:${columnIndex}`, {
      ...sourceRule,
      rowIndex,
      columnIndex,
      valueType: sourceRule.valueType || 'STRING',
      componentFlag: sourceRule.componentFlag || 'input',
      required: sourceRule.required === true,
      label: String(sourceRule.label || sourceRule.placeholder || `E2E字段${nextRules.size + 1}`),
      constraints: sourceRule.constraints || {},
      source: 'MANUAL',
      confidence: 1,
      reviewed: true
    })
  }
  return Array.from(nextRules.values()).sort(
    (left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex
  )
}

async function ensureConfiguredVisualFillFixture(auth, report, evidence) {
  const cellRules = await fetchCommonJson(
    `/mes/pro/batch-record-report/cell-rules?reportId=${encodeURIComponent(report.reportId)}`,
    auth
  )
  const rules = uniqueRulesForSave(cellRules)
  if (!rules.length) {
    throw block('task_owned_report_has_no_cell_rules', {
      reportId: report.reportId,
      reportName: report.reportName,
      batchRecordName: report.batchRecordName || config.batchRecordName
    })
  }
  const userIds = await resolveUserIds(auth)
  const assistRows = rules.map((rule, index) => ({
    rowKey: `ASSIST_GRID_USERS${index % 2 === 0 ? userIds.employeeA : userIds.employeeB}_R${Math.floor(index / 10)}_C${index % 10}`,
    description: `E2E辅助行${index + 1}-${rule.label}`,
    sort: index + 1,
    fields: [{ rowIndex: rule.rowIndex, columnIndex: rule.columnIndex }]
  }))
  await fetchCommonJson('/mes/pro/batch-record-report/cell-rules', auth, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      reportId: report.reportId,
      rules,
      assistRows
    })
  })
  const fillAssignments = assistRows.map((row, index) => ({
    scopeKey: row.rowKey,
    candidateSourceType: 'USERS',
    candidateSourceIds: [index % 2 === 0 ? userIds.employeeA : userIds.employeeB],
    completionPolicy: 'ANY_ONE',
    enabled: true,
    remark: row.description
  }))
  await fetchCommonJson('/mes/pro/edhr-process-form-permission-rule/save-by-report', auth, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      batchRecordReportId: report.reportId,
      fillAssignments
    })
  })
  evidence.fixtureSetup.visualFillConfig = {
    action: 'seed_task_owned_config',
    reportId: report.reportId,
    ruleCount: rules.length,
    assistRowCount: assistRows.length,
    employeeUsernames: [config.employeeAUsername, config.employeeBUsername]
  }
  return {
    ruleCount: rules.length,
    assistRowCount: assistRows.length
  }
}

async function findWorkOrderByCode(auth, workOrderCode) {
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '20',
    code: workOrderCode
  })
  const page = await fetchCommonJson(`/mes/pro/work-order/page?${params.toString()}`, auth)
  const rows = Array.isArray(page?.list) ? page.list : []
  return rows.find((row) => String(row.code) === String(workOrderCode))
}

async function waitForTaskOwnedWorkOrder(auth, workOrderCode, evidence) {
  const attempts = []
  const deadline = Date.now() + config.erpSyncWaitMs
  while (Date.now() <= deadline) {
    const workOrder = await findWorkOrderByCode(auth, workOrderCode)
    attempts.push({
      at: new Date().toISOString(),
      found: Boolean(workOrder),
      status: workOrder?.status,
      productId: workOrder?.productId,
      productCode: workOrder?.productCode,
      batchCode: workOrder?.batchCode
    })
    if (workOrder) {
      evidence.fixtureSetup.workOrderSyncAttempts = attempts
      return workOrder
    }
    await sleep(config.erpSyncPollMs)
  }
  throw block('task_owned_work_order_sync_timeout', {
    workOrderCode,
    waitMs: config.erpSyncWaitMs,
    attempts
  })
}

async function createTaskOwnedErpProductionOrder(auth, evidence) {
  const stamp = buildTimestamp()
  const workOrderCode = `CODX-VFC-MO-${stamp}`
  const batchCode = `CODX-VFC-BATCH-${stamp}`
  const payload = {
    billNo: workOrderCode,
    materialNumber: config.fixtureProjectCode,
    unitNumber: config.fixtureErpUnitNumber,
    quantity: 1,
    plannedStartDate: toLocalApiDateTime(buildPlannedDateTime(1, 9)),
    plannedFinishDate: toLocalApiDateTime(buildPlannedDateTime(2, 17)),
    sourceBillNo: `${workOrderCode}-SO`,
    batchNumber: batchCode
  }
  const erpOrder = await fetchCommonJson('/erp/kingdee-sync/production-order/create', auth, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  evidence.fixtureSetup.erpProductionOrder = {
    action: 'create_task_owned_erp_order',
    workOrderCode,
    batchCode,
    materialNumber: config.fixtureProjectCode,
    unitNumber: config.fixtureErpUnitNumber,
    erpBillNo: erpOrder?.erpBillNo,
    saved: erpOrder?.saved,
    submitted: erpOrder?.submitted
  }
  return { workOrderCode, batchCode, erpOrder }
}

async function assertTargetReportRouteBinding(auth, routeOption, report, evidence) {
  const params = new URLSearchParams({
    routeId: String(routeOption.routeId),
    useType: 'BATCH'
  })
  const processConfigs = await fetchCommonJson(`/mes/pro/route/flow-config?${params.toString()}`, auth)
  const bindings = (Array.isArray(processConfigs) ? processConfigs : []).flatMap((processConfig) =>
    (Array.isArray(processConfig?.batchRecordReports) ? processConfig.batchRecordReports : []).map((binding) => ({
      routeProcessId: processConfig.routeProcessId,
      processName: processConfig.processName,
      batchRecordReportId: binding.batchRecordReportId,
      batchRecordReportName: binding.batchRecordReportName,
      batchRecordVersionId: binding.batchRecordVersionId,
      reportSort: binding.reportSort
    }))
  )
  const targetBinding = bindings.find(
    (binding) => String(binding.batchRecordReportId || '') === String(report.reportId)
  )
  if (!targetBinding) {
    throw block('task_owned_batch_route_missing_target_report_binding', {
      routeId: Number(routeOption.routeId),
      routeCode: routeOption.routeCode,
      routeName: routeOption.routeName,
      targetReportId: report.reportId,
      targetReportName: report.reportName,
      configuredBatchRecordReports: bindings
    })
  }
  evidence.fixtureSetup.targetReportRouteBinding = {
    action: 'validate_current_active_route_binding',
    routeId: Number(routeOption.routeId),
    routeCode: routeOption.routeCode,
    routeName: routeOption.routeName,
    ...targetBinding
  }
  return targetBinding
}

function assertTargetReportBatchTask(batchExecution, report) {
  const tasks = Array.isArray(batchExecution?.tasks) ? batchExecution.tasks : []
  const targetTask = tasks.find(
    (task) => String(task.batchRecordReportId || '') === String(report.reportId)
  )
  if (!targetTask) {
    throw block('task_owned_batch_execution_missing_target_report_task', {
      batchExecutionId: batchExecution?.id,
      batchExecutionCode: batchExecution?.batchExecutionCode,
      routeId: batchExecution?.routeId,
      routeVersionId: batchExecution?.routeVersionId,
      targetReportId: report.reportId,
      targetReportName: report.reportName,
      tasks: tasks.map((task) => ({
        id: task.id,
        nodeType: task.nodeType,
        routeProcessId: task.routeProcessId,
        processName: task.processName,
        batchRecordReportId: task.batchRecordReportId,
        batchRecordReportName: task.batchRecordReportName,
        formBindingKey: task.formBindingKey
      }))
    })
  }
  return targetTask
}

async function ensureTaskOwnedWorkOrderForVisualFill(auth, report, evidence) {
  let workOrder
  let created
  if (config.configuredExistingWorkOrderCode) {
    workOrder = await findWorkOrderByCode(auth, config.configuredExistingWorkOrderCode)
    if (!workOrder) {
      throw block('configured_existing_work_order_not_found', {
        existingWorkOrderCode: config.configuredExistingWorkOrderCode
      })
    }
    created = {
      workOrderCode: String(workOrder.code),
      batchCode: `CODX-VFC-BATCH-${buildTimestamp()}`
    }
    evidence.fixtureSetup.existingWorkOrder = {
      action: 'use_configured_existing_work_order',
      workOrderCode: workOrder.code,
      workOrderId: workOrder.id,
      productId: workOrder.productId,
      productCode: workOrder.productCode,
      productName: workOrder.productName,
      temporaryFrozen: workOrder.temporaryFrozen,
      status: workOrder.status
    }
  } else {
    created = await createTaskOwnedErpProductionOrder(auth, evidence)
    const syncResult = await fetchCommonJson('/mes/pro/work-order/sync-kingdee', auth, {
      method: 'POST'
    })
    evidence.fixtureSetup.workOrderSync = {
      action: 'sync_kingdee_work_order',
      result: syncResult
    }
    workOrder = await waitForTaskOwnedWorkOrder(auth, created.workOrderCode, evidence)
  }
  if (!config.configuredExistingWorkOrderCode && String(workOrder.productCode) !== String(config.fixtureProjectCode)) {
    throw block('task_owned_work_order_product_mismatch', {
      workOrderCode: created.workOrderCode,
      expectedProductCode: config.fixtureProjectCode,
      actualProductCode: workOrder.productCode,
      productId: workOrder.productId
    })
  }
  const routeOptions = await fetchCommonJson(
    `/mes/pro/edhr-batch-execution/work-order-route-options?workOrderId=${Number(workOrder.id)}`,
    auth
  )
  const enabledRouteOptions = (Array.isArray(routeOptions) ? routeOptions : []).filter(
    (option) => option?.routeId && option.batchRouteEnabled !== false
  )
  const routeOption = enabledRouteOptions.find(
    (option) => String(option.routeCode || '') === config.preferredRouteCode
  )
  if (!routeOption) {
    throw block('task_owned_work_order_missing_preferred_batch_route', {
      workOrderCode: created.workOrderCode,
      workOrderId: workOrder.id,
      productId: workOrder.productId,
      productCode: workOrder.productCode,
      sourceRouteCode: config.sourceRouteCode,
      preferredRouteCode: config.preferredRouteCode,
      routeOptions
    })
  }
  await assertTargetReportRouteBinding(auth, routeOption, report, evidence)
  const batchCode = String(created.batchCode || workOrder.batchCode)
  const batchExecution = await fetchCommonJson('/mes/pro/edhr-batch-execution/open-or-create', auth, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      workOrderId: Number(workOrder.id),
      batchCode,
      routeId: Number(routeOption.routeId),
      remark: `CODX visual fill E2E ${created.workOrderCode}`
    })
  })
  const targetBatchTask = assertTargetReportBatchTask(batchExecution, report)
  return {
    workOrderId: Number(workOrder.id),
    workOrderCode: String(workOrder.code),
    productId: Number(workOrder.productId),
    productCode: String(workOrder.productCode),
    batchCode,
    routeId: Number(routeOption.routeId),
    routeCode: routeOption.routeCode,
    routeName: routeOption.routeName,
    batchExecutionId: batchExecution?.id,
    batchExecutionCode: batchExecution?.batchExecutionCode,
    taskTotal: batchExecution?.taskTotal,
    status: batchExecution?.status,
    targetBatchTaskId: targetBatchTask.id,
    targetBatchTaskProcessName: targetBatchTask.processName
  }
}

function runOfficialLoginPreflight(username, password, targetPath, targetText) {
  const scriptPath = path.join(projectRoot, 'scripts', 'preflight', 'login-preflight.mjs')
  if (!fs.existsSync(scriptPath)) {
    throw block('official_login_preflight_missing', { scriptPath })
  }
  const result = spawnSync(
    process.execPath,
    [
      scriptPath,
      '--base-url',
      config.baseUrl,
      '--tenant',
      config.tenant,
      '--username',
      username,
      '--password',
      password,
      '--target-path',
      targetPath,
      '--target-text',
      targetText
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
      timeout: 180000
    }
  )
  if (result.status !== 0) {
    throw block('official_login_preflight_failed', {
      status: result.status,
      signal: result.signal,
      error: result.error ? redactSecretText(result.error.message) : '',
      stdout: redactSecretText(result.stdout),
      stderr: redactSecretText(result.stderr)
    })
  }
  return result.stdout.trim()
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible().catch(() => false)) && !(await input.isDisabled().catch(() => true))) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`missing_login_control:${label}`)
}

async function login(page, username, password, targetPath) {
  const url = new URL('/login', config.baseUrl)
  url.searchParams.set('redirect', targetPath)
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder*="验证码"]').count()) > 0) {
    throw block('captcha_enabled')
  }

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }

  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'), username, 'username')
  await fillFirstVisible(form.locator('input[type="password"]'), password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const body = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(Number(body.code)), `login failed:${body.msg || body.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 90000 })
  return body.data || {}
}

async function readBrowserBusinessData(response, label) {
  const body = await response.json().catch((error) => {
    throw new Error(`${label} response is not JSON: ${error.message}`)
  })
  assert.equal(response.status(), 200, `${label} HTTP failed:${response.status()}`)
  assert.ok([0, 200].includes(Number(body.code)), `${label} failed:${body.msg || body.code}`)
  return body.data
}

async function waitForValue(label, probe, timeoutMs = 90000, pollMs = 1000) {
  const deadline = Date.now() + timeoutMs
  let lastError
  while (Date.now() <= deadline) {
    try {
      const value = await probe()
      if (value) return value
    } catch (error) {
      lastError = error
    }
    await sleep(pollMs)
  }
  throw new Error(
    `timeout_waiting_for_${label}${lastError ? `:${lastError.message || String(lastError)}` : ''}`
  )
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function findRouteByCode(auth, routeCode) {
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '20',
    code: routeCode
  })
  const page = await fetchCommonJson(`/mes/pro/route/page?${params.toString()}`, auth)
  const routes = Array.isArray(page?.list) ? page.list : []
  return routes.find((route) => String(route.code || '') === routeCode)
}

async function waitForRouteByCode(auth, routeCode) {
  return waitForValue(`route_${routeCode}`, () => findRouteByCode(auth, routeCode))
}

async function waitForRouteVersion(auth, routeId, versionId, acceptedStatuses) {
  const expectedStatuses = new Set(acceptedStatuses)
  return waitForValue(`route_version_${versionId}_${acceptedStatuses.join('_')}`, async () => {
    const versions = await fetchCommonJson(
      `/mes/pro/route-version/list-by-route?routeId=${encodeURIComponent(routeId)}`,
      auth
    )
    return (Array.isArray(versions) ? versions : []).find(
      (version) =>
        Number(version.id) === Number(versionId) &&
        expectedStatuses.has(String(version.lifecycleStatus || ''))
    )
  })
}

async function gotoRouteList(page, routeCode) {
  const url = new URL('/mes/pro/route', config.baseUrl)
  url.searchParams.set('code', routeCode)
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 90000
  })
  const row = page
    .locator('.el-table__body-wrapper:visible tbody tr')
    .filter({ hasText: routeCode })
    .first()
  await row.waitFor({ state: 'visible', timeout: 90000 })
  return row
}

function visibleDialog(page, title) {
  return page.locator('.el-dialog:visible').filter({ hasText: title }).last()
}

async function closeVisibleDialog(page, dialog) {
  if (!(await dialog.isVisible().catch(() => false))) return
  await dialog.locator('.el-dialog__headerbtn').first().click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function copyTaskOwnedRouteThroughUi(page, auth, evidence) {
  const sourceRoute = await findRouteByCode(auth, config.sourceRouteCode)
  if (!sourceRoute?.id) {
    throw block('source_route_not_found', { sourceRouteCode: config.sourceRouteCode })
  }
  const existingTaskRoute = await findRouteByCode(auth, config.preferredRouteCode)
  if (existingTaskRoute) {
    throw block('task_owned_route_already_exists_requires_cleanup', {
      routeId: existingTaskRoute.id,
      routeCode: existingTaskRoute.code,
      status: existingTaskRoute.status
    })
  }

  const sourceRow = await gotoRouteList(page, config.sourceRouteCode)
  await sourceRow.getByRole('button', { name: '复制', exact: true }).first().click()
  const dialog = visibleDialog(page, '复制工艺路线')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const targetName = `${config.routeProductName}-${config.preferredRouteCode}`
  await dialog.locator('input[placeholder="请输入副本路线编码"]').fill(config.preferredRouteCode)
  await dialog.locator('input[placeholder="请输入副本路线名称"]').fill(targetName)
  const copyResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/copy') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await dialog.getByRole('button', { name: '确认复制', exact: true }).click()
  const copiedRouteId = Number(
    await readBrowserBusinessData(await copyResponsePromise, 'copy task-owned route')
  )
  assert.ok(Number.isFinite(copiedRouteId) && copiedRouteId > 0, 'route copy must return id')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  const copiedRoute = await waitForRouteByCode(auth, config.preferredRouteCode)
  assert.equal(Number(copiedRoute.id), copiedRouteId, 'copied route id must match page response')
  assert.equal(Number(copiedRoute.status), 1, 'copied task-owned route must start disabled')
  evidence.taskOwnedRoute = {
    routeId: copiedRouteId,
    routeCode: copiedRoute.code,
    routeName: copiedRoute.name,
    sourceRouteId: sourceRoute.id,
    sourceRouteCode: sourceRoute.code,
    initialStatus: copiedRoute.status
  }
  return copiedRoute
}

function routeVersionRow(workspace, versionNo) {
  return workspace
    .locator('.el-table__body-wrapper:visible tbody tr')
    .filter({ hasText: versionNo })
    .first()
}

async function openRouteVersionWorkspace(page, routeCode) {
  const routeRow = await gotoRouteList(page, routeCode)
  await routeRow.getByRole('button', { name: '版本', exact: true }).first().click()
  const workspace = visibleDialog(page, '工艺路线版本')
  await workspace.waitFor({ state: 'visible', timeout: 60000 })
  await workspace.getByText('当前 ACTIVE：', { exact: false }).waitFor({
    state: 'visible',
    timeout: 60000
  })
  return workspace
}

async function createTaskOwnedRouteCandidateThroughUi(page, copiedRoute) {
  const workspace = await openRouteVersionWorkspace(page, copiedRoute.code)
  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-version/create-candidate') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await workspace.getByRole('button', { name: '创建候选版本', exact: true }).click()
  const candidate = await readBrowserBusinessData(
    await createResponsePromise,
    'create task-owned route candidate'
  )
  assert.ok(candidate?.id && candidate?.versionNo, 'route candidate response must include id and versionNo')
  assert.equal(candidate.lifecycleStatus, 'DRAFT', 'new route candidate must be DRAFT')
  const row = routeVersionRow(workspace, candidate.versionNo)
  await row.getByRole('button', { name: '编辑', exact: true }).first().click()
  await page.waitForURL(
    (url) =>
      url.pathname.includes(`/mes/pro/route/edit/${copiedRoute.id}`) &&
      url.searchParams.get('routeVersionId') === String(candidate.id),
    { timeout: 90000 }
  )
  return candidate
}

async function findTargetRouteProcess(auth, copiedRoute, candidate) {
  const params = new URLSearchParams({
    routeId: String(copiedRoute.id),
    useType: 'BATCH',
    routeVersionId: String(candidate.id)
  })
  const configs = await fetchCommonJson(`/mes/pro/route/flow-config?${params.toString()}`, auth)
  const matched = (Array.isArray(configs) ? configs : []).filter(
    (item) => String(item.processName || '').trim() === config.targetProcessName
  )
  if (matched.length !== 1) {
    throw block('target_route_process_not_unique', {
      routeId: copiedRoute.id,
      routeVersionId: candidate.id,
      targetProcessName: config.targetProcessName,
      matched: matched.map((item) => ({
        routeProcessId: item.routeProcessId,
        processName: item.processName,
        processCode: item.processCode
      }))
    })
  }
  return matched[0]
}

function resolvePersistedRouteProcessId(routeProcessId, routeProcessIdMap) {
  const originalRouteProcessId = Number(routeProcessId)
  assert.ok(
    Number.isFinite(originalRouteProcessId) && originalRouteProcessId > 0,
    `target route process id is invalid: ${routeProcessId}`
  )
  const mappedValue = routeProcessIdMap?.[String(originalRouteProcessId)]
  if (mappedValue === undefined || mappedValue === null || mappedValue === '') {
    return originalRouteProcessId
  }
  const persistedRouteProcessId = Number(mappedValue)
  assert.ok(
    Number.isFinite(persistedRouteProcessId) && persistedRouteProcessId > 0,
    `routeProcessIdMap returned invalid target id: ${JSON.stringify({
      routeProcessId: originalRouteProcessId,
      mappedValue
    })}`
  )
  return persistedRouteProcessId
}

async function readBatchRecordReportSelectionDiagnostics(reportSelect) {
  return reportSelect.evaluate((element) => {
    const readTexts = (selector) =>
      Array.from(element.querySelectorAll(selector))
        .map((item) => item.textContent?.trim())
        .filter(Boolean)
    const vueProps = element.__vueParentComponent
      ? {
          typeName: element.__vueParentComponent.type?.name,
          modelValue: element.__vueParentComponent.props?.modelValue,
          multiple: element.__vueParentComponent.props?.multiple,
          disabled: element.__vueParentComponent.props?.disabled
        }
      : null
    return {
      innerText: element.innerText,
      selectedTexts: readTexts(
        '.el-tag, .el-select__tags-text, .el-select__selected-item, .el-select__collapse-tags'
      ),
      optionTexts: readTexts('.el-select-dropdown__item'),
      optionHtml: Array.from(element.querySelectorAll('.el-select-dropdown__item'))
        .map((item) => item.outerHTML)
        .slice(0, 5),
      optionStyles: Array.from(
        element.querySelectorAll('.route-flow-graph-designer__batch-record-report-option')
      )
        .map((item) => {
          const style = window.getComputedStyle(item)
          return {
            pointerEvents: style.pointerEvents,
            display: style.display,
            width: style.width,
            height: style.height
          }
        })
        .slice(0, 5),
      inputValues: Array.from(element.querySelectorAll('input')).map((input) => input.value),
      className: element.className,
      vueProps
    }
  })
}

async function waitForSelectedBatchRecordReport(reportSelect, report, clickDiagnostics) {
  const expectedReportId = String(report.reportId)
  const deadline = Date.now() + 30000
  let diagnostics = null
  while (Date.now() < deadline) {
    diagnostics = await readBatchRecordReportSelectionDiagnostics(reportSelect)
    const selectedIds = Array.isArray(diagnostics.vueProps?.modelValue)
      ? diagnostics.vueProps.modelValue.map((item) => String(item))
      : []
    if (selectedIds.includes(expectedReportId)) {
      return diagnostics
    }
    await sleep(250)
  }
  throw new Error(
    `target batch record report selection did not update modelValue: ${JSON.stringify({
      reportId: report.reportId,
      reportCode: report.reportCode,
      reportName: report.reportName,
      clickDiagnostics,
      diagnostics
    })}`
  )
}

async function captureBatchRecordReportOptionClickDiagnostics(page, reportOption) {
  await page.evaluate(() => {
    window.__codexBatchRecordReportOptionClickDiagnostics = []
  })
  await reportOption.evaluate((element) => {
    const describeOptionComponent = () => {
      const component = element.__vueParentComponent
      return component
        ? {
            typeName: component.type?.name,
            value: component.props?.value,
            label: component.props?.label,
            disabled: component.props?.disabled,
            itemSelected: component.proxy?.itemSelected
          }
        : null
    }
    const record = (name, event) => {
      window.__codexBatchRecordReportOptionClickDiagnostics.push({
        name,
        eventPhase: event.eventPhase,
        defaultPrevented: event.defaultPrevented,
        targetClassName: event.target?.className || '',
        currentTargetClassName: event.currentTarget?.className || '',
        optionComponent: describeOptionComponent()
      })
    }
    element.addEventListener('pointerdown', (event) => record('li_pointerdown_capture', event), true)
    element.addEventListener('click', (event) => record('li_click_capture', event), true)
    element.addEventListener('click', (event) => record('li_click_bubble', event))
  })
}

async function readBatchRecordReportOptionClickDiagnostics(page) {
  return page.evaluate(() => window.__codexBatchRecordReportOptionClickDiagnostics || [])
}

async function readBatchRecordReportOptionRuntimeDiagnostics(reportOption) {
  return reportOption.evaluate((element) => {
    const component = element.__vueParentComponent
    return {
      className: element.className,
      ariaSelected: element.getAttribute('aria-selected'),
      optionComponent: component
        ? {
            typeName: component.type?.name,
            value: component.props?.value,
            label: component.props?.label,
            disabled: component.props?.disabled,
            itemSelected: component.proxy?.itemSelected
          }
        : null
    }
  })
}

async function waitForRouteProcessAttributeEditorReady(editor) {
  await editor
    .locator('.route-flow-graph-designer__process-detail-loading')
    .waitFor({ state: 'hidden', timeout: 90000 })
    .catch(() => undefined)
}

async function waitForEmployeeExecutionFormPage(page, context) {
  try {
    await waitForValue(
      'employee_execution_form_url',
      () => {
        const url = new URL(page.url())
        return url.pathname === '/mes/pro/feedback/edhr-execution/form' ? url.href : null
      },
      90000,
      500
    )
  } catch (error) {
    throw block('employee_work_task_did_not_open_execution_form', {
      ...context,
      currentUrl: page.url(),
      pageTitle: await page.title().catch(() => ''),
      visibleBodyText: (await page.locator('body').innerText().catch(() => '')).slice(0, 1500),
      cause: error.message || String(error)
    })
  }
}

const sanitizeWorkTaskRow = (row) => ({
  id: row.id,
  taskCode: row.taskCode,
  taskType: row.taskType,
  status: row.status,
  actionUrl: row.actionUrl,
  batchExecutionId: row.batchExecutionId,
  batchTaskId: row.batchTaskId,
  executionId: row.executionId,
  workOrderCode: row.workOrderCode,
  batchCode: row.batchCode,
  processName: row.processName,
  candidateSourceType: row.candidateSourceType,
  candidateSourceIds: row.candidateSourceIds,
  responsibilitySourceType: row.responsibilitySourceType,
  inactionReason: row.inactionReason
})

async function ensureBatchRecordDetailFieldVisible(page, editor) {
  let fieldButton = editor
    .locator('[data-flow-action="select-process-detail-field"]')
    .filter({ hasText: '批记录表单' })
    .first()
  if (await fieldButton.isVisible().catch(() => false)) return fieldButton

  const fieldSelect = editor.locator('[data-flow-field="process-config-item-select"]').first()
  await fieldSelect.click()
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: '批记录表单' })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await editor.locator('[data-flow-action="add-process-config-item"]').click()
  fieldButton = editor
    .locator('[data-flow-action="select-process-detail-field"]')
    .filter({ hasText: '批记录表单' })
    .first()
  await fieldButton.waitFor({ state: 'visible', timeout: 30000 })
  return fieldButton
}

async function configureTargetBatchRecordReportThroughUi(page, auth, copiedRoute, candidate, report) {
  const targetProcess = await findTargetRouteProcess(auth, copiedRoute, candidate)
  const editorUrl = new URL(`/mes/pro/route/edit/${copiedRoute.id}`, config.baseUrl)
  editorUrl.searchParams.set('tab', 'flow')
  editorUrl.searchParams.set('routeProcessId', String(targetProcess.routeProcessId))
  editorUrl.searchParams.set('routeVersionId', String(candidate.id))
  editorUrl.searchParams.set('routeVersionNo', candidate.versionNo)
  editorUrl.searchParams.set('routeVersionStatus', candidate.lifecycleStatus)
  await page.goto(editorUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
  const editor = page.locator('.route-flow-graph-designer').first()
  await editor.waitFor({ state: 'visible', timeout: 90000 })
  const targetNode = editor
    .locator(
      `[data-flow-node="route-process"][data-route-process-id="${targetProcess.routeProcessId}"]`
    )
    .first()
  await targetNode.waitFor({ state: 'visible', timeout: 90000 })
  await targetNode.scrollIntoViewIfNeeded()
  await targetNode.click()
  await waitForRouteProcessAttributeEditorReady(editor)

  const fieldButton = await ensureBatchRecordDetailFieldVisible(page, editor)
  await fieldButton.click()
  await waitForRouteProcessAttributeEditorReady(editor)
  const reportSelect = editor
    .locator('[data-route-process-setting-field="batch-record-report"]')
    .first()
  await reportSelect.waitFor({ state: 'visible', timeout: 30000 })
  await reportSelect.click()
  const reportInput = reportSelect.locator('input[role="combobox"]').first()
  assert.ok(report.reportCode, 'target batch record report code is required for exact selection')
  await reportInput.fill(report.reportCode)
  const reportDropdown = reportSelect.locator('.el-select-dropdown:visible')
  const reportOption = reportDropdown
    .locator('.el-select-dropdown__item')
    .filter({ hasText: report.reportCode })
    .first()
  await reportOption.waitFor({ state: 'visible', timeout: 60000 })
  await sleep(500)
  const beforeClickSelectionDiagnostics = await readBatchRecordReportSelectionDiagnostics(reportSelect)
  const selectedIdsBeforeClick = Array.isArray(beforeClickSelectionDiagnostics.vueProps?.modelValue)
    ? beforeClickSelectionDiagnostics.vueProps.modelValue.map((item) => String(item))
    : []
  const beforeClickOptionDiagnostics = await readBatchRecordReportOptionRuntimeDiagnostics(reportOption)
  const isAlreadySelected =
    selectedIdsBeforeClick.includes(String(report.reportId)) ||
    beforeClickOptionDiagnostics.optionComponent?.itemSelected === true ||
    String(beforeClickOptionDiagnostics.ariaSelected) === 'true' ||
    String(beforeClickOptionDiagnostics.className || '').includes('is-selected')
  let clickDiagnostics = [
    {
      name: 'option_selection_state_before_click',
      selectionDiagnostics: beforeClickSelectionDiagnostics,
      optionDiagnostics: beforeClickOptionDiagnostics
    }
  ]
  if (!isAlreadySelected) {
    await captureBatchRecordReportOptionClickDiagnostics(page, reportOption)
    await reportOption.click()
    clickDiagnostics = await readBatchRecordReportOptionClickDiagnostics(page)
  }
  const shouldWaitForBatchSave = !isAlreadySelected
  await page.keyboard.press('Escape')
  await waitForSelectedBatchRecordReport(reportSelect, report, clickDiagnostics)

  const validateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process-flow/validate') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  const graphSaveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process-flow/save') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  const batchSaveResponsePromise = shouldWaitForBatchSave
    ? page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/route/flow-config/batch-record/save') &&
          response.request().method() === 'POST',
        { timeout: 90000 }
      )
    : null
  await editor.locator('[data-flow-action="save-route-flow"]').click()
  const validateResult = await readBrowserBusinessData(
    await validateResponsePromise,
    'validate task-owned route flow'
  )
  assert.equal(validateResult?.valid, true, 'task-owned route flow validation must pass')
  const graphSaveResult = await readBrowserBusinessData(
    await graphSaveResponsePromise,
    'save task-owned route flow'
  )
  assert.equal(graphSaveResult?.valid, true, 'task-owned route graph save must pass')
  const persistedRouteProcessId = resolvePersistedRouteProcessId(
    targetProcess.routeProcessId,
    graphSaveResult?.routeProcessIdMap
  )
  let batchSaveRequestPayload = {
    skipped: 'target batch record report was already selected before save'
  }
  if (batchSaveResponsePromise) {
    const batchSaveResponse = await batchSaveResponsePromise
    batchSaveRequestPayload = JSON.parse(batchSaveResponse.request().postData() || '{}')
    await readBrowserBusinessData(batchSaveResponse, 'save target batch record binding')
    const savedRequestProcess = (batchSaveRequestPayload.processConfigs || []).find(
      (item) => Number(item.routeProcessId) === Number(persistedRouteProcessId)
    )
    const savedRequestBinding = (savedRequestProcess?.batchRecordReports || []).find(
      (item) => String(item.batchRecordReportId || '') === String(report.reportId)
    )
    assert.ok(
      savedRequestBinding,
      `target batch record report must be present in batch save request: ${JSON.stringify({
        targetRouteProcessId: Number(targetProcess.routeProcessId),
        persistedRouteProcessId,
        routeProcessIdMap: graphSaveResult?.routeProcessIdMap,
        batchSaveRequestPayload
      })}`
    )
  }

  const savedParams = new URLSearchParams({
    routeId: String(copiedRoute.id),
    useType: 'BATCH',
    routeVersionId: String(candidate.id)
  })
  const savedConfigs = await fetchCommonJson(
    `/mes/pro/route/flow-config?${savedParams.toString()}`,
    auth
  )
  const savedProcess = (Array.isArray(savedConfigs) ? savedConfigs : []).find(
    (item) => Number(item.routeProcessId) === Number(persistedRouteProcessId)
  )
  const savedBinding = (savedProcess?.batchRecordReports || []).find(
    (item) => String(item.batchRecordReportId || '') === String(report.reportId)
  )
  assert.ok(
    savedBinding,
    `target batch record report must be saved on the exact route process: ${JSON.stringify({
      targetRouteProcessId: Number(targetProcess.routeProcessId),
      persistedRouteProcessId,
      routeProcessIdMap: graphSaveResult?.routeProcessIdMap,
      batchSaveRequestPayload,
      savedProcess: savedProcess
        ? {
            routeProcessId: savedProcess.routeProcessId,
            processName: savedProcess.processName,
            batchRecordReports: savedProcess.batchRecordReports
          }
        : null
    })}`
  )
  return {
    routeProcessId: persistedRouteProcessId,
    processName: targetProcess.processName,
    batchRecordReportId: savedBinding.batchRecordReportId,
    batchRecordReportName: savedBinding.batchRecordReportName
  }
}

async function switchBrowserUser(page, account, targetPath) {
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  })
  await page.context().clearCookies()
  return login(page, account.username, account.password, targetPath)
}

function authorizedAccounts() {
  return [
    { label: 'admin', username: config.adminUsername, password: config.adminPassword },
    { label: 'employeeA', username: config.employeeAUsername, password: config.employeeAPassword },
    { label: 'employeeB', username: config.employeeBUsername, password: config.employeeBPassword }
  ]
}

async function completeApprovalThroughUi(page, processInstanceId, approvalLabel) {
  const attempts = []
  for (const account of authorizedAccounts()) {
    try {
      await switchBrowserUser(page, account, '/approval-center/todo?moduleCode=BPM')
      const tasksResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/approval-center/tasks/page') &&
          response.url().includes('moduleCode=BPM') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/approval-center/todo?moduleCode=BPM`, {
        waitUntil: 'domcontentloaded',
        timeout: 90000
      })
      await page.getByRole('heading', { name: '审批中心' }).waitFor({
        state: 'visible',
        timeout: 60000
      })
      const pageData = await readBrowserBusinessData(
        await tasksResponsePromise,
        `${approvalLabel} approval tasks`
      )
      const tasks = Array.isArray(pageData?.list) ? pageData.list : []
      const rowIndex = tasks.findIndex(
        (task) =>
          String(task.processInstanceId || '') === String(processInstanceId) ||
          String(task.businessKey || '') === String(processInstanceId)
      )
      attempts.push({ username: account.username, taskFound: rowIndex >= 0 })
      if (rowIndex < 0) continue

      const row = page.locator('.el-table__body-wrapper:visible tbody tr').nth(rowIndex)
      await row.waitFor({ state: 'visible', timeout: 30000 })
      await row.getByRole('button', { name: '审核', exact: true }).first().click()
      const dialog = visibleDialog(page, '审核确认')
      await dialog.waitFor({ state: 'visible', timeout: 30000 })
      const approveOption = dialog.getByText('审核通过', { exact: true }).first()
      if (await approveOption.isVisible().catch(() => false)) {
        await approveOption.click()
      }
      await dialog.locator('input[type="password"]').first().fill(account.password)
      const reviewResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/approval-center/tasks/review') &&
          response.request().method() === 'POST',
        { timeout: 90000 }
      )
      await dialog.getByRole('button', { name: '确认审核', exact: true }).click()
      await readBrowserBusinessData(await reviewResponsePromise, `${approvalLabel} approval review`)
      await dialog.waitFor({ state: 'hidden', timeout: 30000 })
      return { username: account.username, processInstanceId: String(processInstanceId) }
    } catch (error) {
      attempts.push({
        username: account.username,
        error: redactSecretText(error.message || String(error))
      })
    }
  }
  throw block(`${approvalLabel}_authorized_approver_not_found`, {
    processInstanceId: String(processInstanceId),
    attempts
  })
}

async function completeRouteApprovalThroughUi(page, submittedVersion) {
  const processInstanceId = submittedVersion?.approvalProcessInstanceId
  if (!processInstanceId) {
    throw block('route_version_pending_approval_missing_process_instance', {
      routeVersionId: submittedVersion?.id,
      lifecycleStatus: submittedVersion?.lifecycleStatus
    })
  }
  return completeApprovalThroughUi(page, processInstanceId, 'route_version')
}

async function publishTaskOwnedRouteCandidateThroughUi(page, auth, copiedRoute, candidate) {
  await switchBrowserUser(
    page,
    { username: config.adminUsername, password: config.adminPassword },
    '/mes/pro/route'
  )
  const workspace = await openRouteVersionWorkspace(page, copiedRoute.code)
  const row = routeVersionRow(workspace, candidate.versionNo)
  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-version/submit-publish') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await row.getByRole('button', { name: '提交发布', exact: true }).first().click()
  let submittedVersion = await readBrowserBusinessData(
    await submitResponsePromise,
    'submit task-owned route candidate'
  )
  if (submittedVersion?.lifecycleStatus === 'PENDING_APPROVAL') {
    await closeVisibleDialog(page, workspace)
    const approval = await completeRouteApprovalThroughUi(page, submittedVersion)
    submittedVersion = await waitForRouteVersion(auth, copiedRoute.id, candidate.id, ['ACTIVE'])
    return { submittedVersion, approval }
  }
  assert.equal(
    submittedVersion?.lifecycleStatus,
    'ACTIVE',
    `route candidate must become ACTIVE or PENDING_APPROVAL, got ${submittedVersion?.lifecycleStatus}`
  )
  return { submittedVersion, approval: null }
}

async function enableTaskOwnedRouteThroughUi(page, auth, copiedRoute) {
  await switchBrowserUser(
    page,
    { username: config.adminUsername, password: config.adminPassword },
    '/mes/pro/route'
  )
  const currentRoute = await findRouteByCode(auth, copiedRoute.code)
  assert.equal(Number(currentRoute?.status), 1, 'task-owned route must be disabled before enable')
  const routeRow = await gotoRouteList(page, copiedRoute.code)
  const statusSwitch = routeRow.locator('.el-switch').first()
  await statusSwitch.waitFor({ state: 'visible', timeout: 30000 })
  const statusResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/update-status') &&
      response.request().method() === 'PUT',
    { timeout: 90000 }
  )
  await statusSwitch.click()
  const confirm = page.locator('.el-message-box:visible').last()
  await confirm.waitFor({ state: 'visible', timeout: 30000 })
  await confirm.getByRole('button', { name: '确定', exact: true }).click()
  await readBrowserBusinessData(await statusResponsePromise, 'enable task-owned route')
  const enabledRoute = await waitForValue(`enabled_route_${copiedRoute.code}`, async () => {
    const route = await findRouteByCode(auth, copiedRoute.code)
    return Number(route?.status) === 0 ? route : undefined
  })
  return {
    routeId: enabledRoute.id,
    routeCode: enabledRoute.code,
    status: enabledRoute.status
  }
}

async function prepareTaskOwnedRouteThroughUi(page, auth, report, evidence) {
  const copiedRoute = await copyTaskOwnedRouteThroughUi(page, auth, evidence)
  const candidate = await createTaskOwnedRouteCandidateThroughUi(page, copiedRoute)
  evidence.taskOwnedRoute.candidateRouteVersionId = candidate.id
  evidence.taskOwnedRoute.candidateRouteVersionNo = candidate.versionNo
  evidence.taskOwnedRoute.targetBinding = await configureTargetBatchRecordReportThroughUi(
    page,
    auth,
    copiedRoute,
    candidate,
    report
  )
  evidence.taskOwnedRoute.publish = await publishTaskOwnedRouteCandidateThroughUi(
    page,
    auth,
    copiedRoute,
    candidate
  )
  evidence.taskOwnedRoute.enable = await enableTaskOwnedRouteThroughUi(page, auth, copiedRoute)
  return evidence.taskOwnedRoute
}

async function fillOptionalApprovalAssignees(page, dialog, auth) {
  const users = await fetchCommonJson('/system/user/simple-list', auth)
  const adminUser = (Array.isArray(users) ? users : []).find(
    (user) => String(user.username || '') === config.adminUsername
  )
  const optionTexts = [adminUser?.nickname, adminUser?.username, config.adminUsername]
    .filter(Boolean)
    .map((value) => String(value))
  const approvalItems = dialog.locator('.el-form-item').filter({ hasText: /审批人/ })
  const count = await approvalItems.count()
  for (let index = 0; index < count; index += 1) {
    const item = approvalItems.nth(index)
    if (!(await item.isVisible().catch(() => false))) continue
    await item.locator('.el-select, .el-input').first().click()
    const optionPattern = new RegExp(optionTexts.map(escapeRegExp).join('|'))
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: optionPattern })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  }
}

async function cleanupTaskOwnedBatchThroughUi(page, auth, batchExecution) {
  if (!batchExecution?.batchExecutionId) {
    return { status: 'not-created' }
  }
  await switchBrowserUser(
    page,
    { username: config.adminUsername, password: config.adminPassword },
    '/mes/pro/feedback/edhr-batch-execution'
  )
  const listUrl = new URL('/mes/pro/feedback/edhr-batch-execution', config.baseUrl)
  listUrl.searchParams.set('batchExecutionCode', batchExecution.batchExecutionCode)
  const listResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/page') &&
      response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await page.goto(listUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
  const pageData = await readBrowserBusinessData(
    await listResponsePromise,
    'load task-owned batch for cleanup'
  )
  const targetRows = (Array.isArray(pageData?.list) ? pageData.list : []).filter(
    (row) => Number(row.id) === Number(batchExecution.batchExecutionId)
  )
  if (targetRows.length !== 1) {
    const currentBatch = await fetchCommonJson(
      `/mes/pro/edhr-batch-execution/get?id=${encodeURIComponent(
        batchExecution.batchExecutionId
      )}`,
      auth
    )
    if (Number(currentBatch?.status) === 60) {
      return {
        status: 'already-voided',
        batchExecutionId: currentBatch.id,
        batchExecutionCode: currentBatch.batchExecutionCode
      }
    }
    throw block('cleanup_task_owned_batch_not_visible_on_batch_page', {
      expectedBatchExecutionId: batchExecution.batchExecutionId,
      expectedBatchExecutionCode: batchExecution.batchExecutionCode,
      pageTotal: pageData?.total,
      visibleRows: (Array.isArray(pageData?.list) ? pageData.list : []).map((item) => ({
        id: item.id,
        batchExecutionCode: item.batchExecutionCode,
        status: item.status
      }))
    })
  }
  const targetBatch = targetRows[0]
  const row = page
    .locator('.el-table__body-wrapper:visible tbody tr')
    .filter({ hasText: batchExecution.batchExecutionCode })
    .first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '作废', exact: true }).first().click()

  const dialog = visibleDialog(page, '作废批次执行')
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText(batchExecution.batchExecutionCode, { exact: false }).waitFor({
    state: 'visible',
    timeout: 30000
  })
  const reasonItem = dialog.locator('.el-form-item').filter({ hasText: '原因分类' }).first()
  await reasonItem.locator('.el-select, .el-input').first().click()
  const reasonOption = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: '其他' })
    .first()
  await reasonOption.waitFor({ state: 'visible', timeout: 30000 })
  await reasonOption.click()
  await dialog
    .locator('.el-form-item')
    .filter({ hasText: '原因说明' })
    .locator('textarea')
    .first()
    .fill(`CODX-VFC E2E cleanup ${config.preferredRouteCode}`)
  await dialog.locator('input[type="password"]').first().fill(config.adminPassword)
  const comment = dialog.locator('.el-form-item').filter({ hasText: '备注' }).locator('textarea').first()
  if (await comment.isVisible().catch(() => false)) {
    await comment.fill('任务自有批次验证完成后清理')
  }
  await fillOptionalApprovalAssignees(page, dialog, auth)

  const voidResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(
        '/admin-api/mes/pro/edhr-change/void-batch-execution/request'
      ) && response.request().method() === 'POST',
    { timeout: 120000 }
  )
  await dialog.getByRole('button', { name: '提交作废流程', exact: true }).click()
  const change = await readBrowserBusinessData(
    await voidResponsePromise,
    'submit task-owned batch void'
  )
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  let approval = null
  if (change?.bpmProcessInstanceId && change?.changeStatus !== 'EFFECTIVE') {
    approval = await completeApprovalThroughUi(
      page,
      change.bpmProcessInstanceId,
      'task_owned_batch_void'
    )
  }
  const voided = await waitForValue(
    `voided_batch_${batchExecution.batchExecutionId}`,
    async () => {
      const current = await fetchCommonJson(
        `/mes/pro/edhr-batch-execution/get?id=${encodeURIComponent(
          batchExecution.batchExecutionId
        )}`,
        auth
      )
      return Number(current?.status) === 60 ? current : undefined
    },
    120000
  )
  return {
    status: 'voided',
    batchExecutionId: voided.id,
    batchExecutionCode: voided.batchExecutionCode,
    approval
  }
}

async function cleanupTaskOwnedRouteThroughUi(page, auth) {
  let route = await findRouteByCode(auth, config.preferredRouteCode)
  if (!route) {
    return { status: 'not-created', routeCode: config.preferredRouteCode }
  }
  await switchBrowserUser(
    page,
    { username: config.adminUsername, password: config.adminPassword },
    '/mes/pro/route'
  )
  if (Number(route.status) === 0) {
    const row = await gotoRouteList(page, route.code)
    const statusSwitch = row.locator('.el-switch').first()
    const disableResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route/update-status') &&
        response.request().method() === 'PUT',
      { timeout: 90000 }
    )
    await statusSwitch.click()
    const confirm = page.locator('.el-message-box:visible').last()
    await confirm.waitFor({ state: 'visible', timeout: 30000 })
    await confirm.getByRole('button', { name: '确定', exact: true }).click()
    await readBrowserBusinessData(await disableResponsePromise, 'disable task-owned route')
    route = await waitForValue(`disabled_route_${route.code}`, async () => {
      const current = await findRouteByCode(auth, route.code)
      return Number(current?.status) === 1 ? current : undefined
    })
  }

  const row = await gotoRouteList(page, route.code)
  const deleteResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/delete') &&
      response.request().method() === 'DELETE',
    { timeout: 90000 }
  )
  await row.getByRole('button', { name: '删除', exact: true }).first().click()
  const confirm = page.locator('.el-message-box:visible').last()
  await confirm.waitFor({ state: 'visible', timeout: 30000 })
  await confirm.getByRole('button', { name: '确定', exact: true }).click()
  await readBrowserBusinessData(await deleteResponsePromise, 'delete task-owned route')
  await waitForValue(`deleted_route_${route.code}`, async () => {
    const current = await findRouteByCode(auth, route.code)
    return current ? undefined : true
  })
  return { status: 'deleted', routeId: route.id, routeCode: route.code }
}

async function cleanupTaskRuntimeFixtures(auth, evidence) {
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.chromeExecutable || undefined
  })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  try {
    await login(
      page,
      config.adminUsername,
      config.adminPassword,
      '/mes/pro/feedback/edhr-batch-execution'
    )
    const batch = await cleanupTaskOwnedBatchThroughUi(
      page,
      auth,
      evidence.taskOwnedBatchExecution
    )
    const route = await cleanupTaskOwnedRouteThroughUi(page, auth)
    return { batch, route }
  } finally {
    await context.close()
    await browser.close()
  }
}

async function runCleanupOnly() {
  const evidence = {
    status: 'RUNNING',
    mode: 'cleanup-only',
    configPath: config.configPath,
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    tenant: config.tenant || '<missing>',
    account: config.adminUsername || '<missing>',
    preferredRouteCode: config.preferredRouteCode || '<missing>',
    sourceResultFile: resultFile,
    resultFile: cleanupResultFile
  }
  let browser
  let context
  try {
    assertPairedWorktreeUrls()
    await assertRuntimeReady()
    const missing = []
    if (config.configLoadError) missing.push(config.configLoadError)
    if (!config.tenant) missing.push('local config tenant')
    if (!config.adminUsername) missing.push('local config accounts.admin.username')
    if (!config.adminPassword) missing.push('local config accounts.admin.password')
    if (!config.preferredRouteCode) missing.push('local config fixture.preferredRouteCode')
    if (!config.allowWrite) missing.push('local config allowWrite=true')
    if (!fs.existsSync(resultFile)) missing.push(`previous result file:${resultFile}`)
    if (missing.length) {
      throw block('edhr_visual_fill_cleanup_precondition_missing', { missing })
    }

    const previousEvidence = JSON.parse(fs.readFileSync(resultFile, 'utf8'))
    if (!previousEvidence?.taskOwnedBatchExecution?.batchExecutionId) {
      throw block('cleanup_previous_result_missing_task_owned_batch', {
        sourceResultFile: resultFile
      })
    }
    evidence.taskOwnedBatchExecution = previousEvidence.taskOwnedBatchExecution

    browser = await chromium.launch({
      headless: !config.headed,
      executablePath: config.chromeExecutable || undefined
    })
    context = await browser.newContext({
      viewport: { width: 1440, height: 960 },
      locale: 'zh-CN'
    })
    const page = await context.newPage()
    const loginData = await login(
      page,
      config.adminUsername,
      config.adminPassword,
      '/mes/pro/feedback/edhr-batch-execution'
    )
    const accessToken = loginData.accessToken || loginData.access_token
    if (!accessToken) {
      throw block('admin_login_response_missing_access_token')
    }
    const auth = { accessToken, tenantId: await resolveTenantId() }
    evidence.cleanup = {
      batch: await cleanupTaskOwnedBatchThroughUi(
        page,
        auth,
        evidence.taskOwnedBatchExecution
      ),
      route: await cleanupTaskOwnedRouteThroughUi(page, auth)
    }
    evidence.status = 'PASS'
    writeResult(evidence, cleanupResultFile)
    console.log(JSON.stringify(sanitizeEvidence(evidence), null, 2))
  } catch (error) {
    evidence.status = error.blocked ? 'BLOCKED' : 'FAIL'
    evidence.error = {
      message: redactSecretText(error.message),
      details: sanitizeEvidence(error.details),
      stack: redactSecretText(error.stack)
    }
    writeResult(evidence, cleanupResultFile)
    console.error(JSON.stringify(sanitizeEvidence(evidence), null, 2))
    process.exitCode = 1
  } finally {
    await context?.close().catch(() => undefined)
    await browser?.close().catch(() => undefined)
  }
}

async function captureVisibleOverlayDiagnostics(page) {
  const visibleOverlays = page.locator('.el-overlay:visible')
  const count = await visibleOverlays.count()
  const overlays = []
  for (let index = 0; index < count; index += 1) {
    const overlay = visibleOverlays.nth(index)
    const title = await overlay
      .locator('.el-dialog__title, .el-message-box__title, .el-drawer__title')
      .first()
      .innerText()
      .catch(() => '')
    const text = await overlay.innerText().catch(() => '')
    overlays.push({
      index,
      className: (await overlay.getAttribute('class')) || '',
      title: title.trim().slice(0, 200),
      text: text.trim().replace(/\s+/g, ' ').slice(0, 200),
      hasVisualFillEditor: (await overlay.locator('.batch-record-cell-rules-editor').count()) > 0
    })
  }
  return {
    url: page.url(),
    count,
    overlays
  }
}

async function openVisualFillConfigDialog(page, report) {
  const targetReportId = String(report?.reportId || '').trim()
  const targetReportName = String(report?.reportName || config.reportName || '').trim()
  const targetProductName = String(report?.batchRecordName || config.batchRecordName || '').trim()
  assert.ok(targetReportId, 'target report id is required to open visual fill config')
  assert.ok(targetReportName, 'target report name is required to open visual fill config')
  const targetPageResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-report/page') &&
      response.url().includes(`reportId=${encodeURIComponent(targetReportId)}`),
    { timeout: 90000 }
  )
  await page.goto(
    `${config.baseUrl}/mes/pro/batch-record-form-list?reportId=${encodeURIComponent(targetReportId)}`,
    {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    }
  )
  await targetPageResponse
  await page.getByText('批记录表单', { exact: false }).first().waitFor({ state: 'visible', timeout: 90000 })
  const visibleBodyRows = page
    .locator('.el-table__body-wrapper:visible tbody tr')
    .filter({ hasText: targetReportName })
    .filter({ hasText: targetProductName })
  await visibleBodyRows.first().waitFor({ state: 'visible', timeout: 30000 }).catch(() => undefined)
  const targetRowCount = await visibleBodyRows.count()
  if (targetRowCount !== 1) {
    const visibleTableText = await page.locator('.el-table__body-wrapper').first().innerText().catch(() => '')
    throw block(
      targetRowCount < 1 ? 'task_owned_report_fixture_not_visible' : 'task_owned_report_fixture_ambiguous',
      {
        reportId: targetReportId,
        batchRecordName: config.batchRecordName,
        targetReportName,
        targetProductName,
        targetRowCount,
        visibleTableText: visibleTableText.slice(0, 1000)
      }
    )
  }
  const targetRow = visibleBodyRows.first()
  await targetRow.locator('td.el-table__cell').filter({ hasText: targetReportName }).first().click()
  const actionBar = page.locator('.batch-record-form-preview__actions').first()
  await actionBar.waitFor({ state: 'visible', timeout: 30000 })
  const overlayDiagnosticsBeforeClick = await captureVisibleOverlayDiagnostics(page)
  if (overlayDiagnosticsBeforeClick.count > 0) {
    throw block('unexpected_overlay_before_visual_fill_config_click', overlayDiagnosticsBeforeClick)
  }
  await actionBar.getByRole('button', { name: '填写配置' }).click({ noWaitAfter: true })
  const visibleVisualFillDialogs = page.locator('.el-overlay:visible .batch-record-cell-rules-editor')
  await visibleVisualFillDialogs.first().waitFor({ state: 'visible', timeout: 90000 })
  await page.waitForTimeout(500)
  assert.equal(await visibleVisualFillDialogs.count(), 1, 'visual fill config must open exactly one visible editor')
  const dialog = visibleVisualFillDialogs.last()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })
  await dialog.locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 90000 })
  const firstRuleCell = dialog
    .locator('.batch-record-cell-rules-editor__cell.is-rule .batch-record-cell-rules-editor__cell-button')
    .first()
  await firstRuleCell.waitFor({ state: 'visible', timeout: 90000 })
  await firstRuleCell.scrollIntoViewIfNeeded()
  const firstRuleCellSelected = await firstRuleCell.getAttribute('aria-pressed')
  if (firstRuleCellSelected !== 'true') {
    await firstRuleCell.click()
  }
  await dialog
    .locator('.batch-record-cell-rules-editor__mode-switch')
    .getByText('辅助表单映射', { exact: true })
    .click()
  await dialog
    .locator('[data-fill-config-panel="assist-preview"]')
    .waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText('责任主体', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText('字段类型', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

async function saveVisualFillConfigDialog(page) {
  const overlayDiagnosticsBeforeSave = await captureVisibleOverlayDiagnostics(page)
  const visualFillOverlayCount = overlayDiagnosticsBeforeSave.overlays.filter(
    (overlay) => overlay.hasVisualFillEditor
  ).length
  if (overlayDiagnosticsBeforeSave.count !== 1 || visualFillOverlayCount !== 1) {
    throw block('unexpected_overlay_before_visual_fill_config_save', overlayDiagnosticsBeforeSave)
  }
  const visualFillOverlay = page
    .locator('.el-overlay:visible')
    .filter({ has: page.locator('.batch-record-cell-rules-editor') })
    .last()
  const saveButton = visualFillOverlay.getByRole('button', { name: '保存填写配置' })
  const cellRulesResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-report/cell-rules') &&
      response.request().method() === 'PUT',
    { timeout: 90000 }
  )
  const assignmentResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-process-form-permission-rule/save-by-report') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  const [cellRulesResponse, assignmentResponse] = await Promise.all([
    cellRulesResponsePromise,
    assignmentResponsePromise,
    saveButton.click()
  ])
  assert.equal(cellRulesResponse.status(), 200, `cell-rules save HTTP failed:${cellRulesResponse.status()}`)
  assert.equal(assignmentResponse.status(), 200, `save-by-report HTTP failed:${assignmentResponse.status()}`)
  const cellRulesBody = await cellRulesResponse.json()
  const assignmentBody = await assignmentResponse.json()
  assert.ok([0, 200].includes(Number(cellRulesBody.code)), `cell-rules save failed:${cellRulesBody.msg || cellRulesBody.code}`)
  assert.ok([0, 200].includes(Number(assignmentBody.code)), `save-by-report failed:${assignmentBody.msg || assignmentBody.code}`)
  assert.ok(Array.isArray(cellRulesBody.data?.assistRows), 'cell-rules response must include assistRows')
  assert.ok(Array.isArray(assignmentBody.data?.fillAssignments), 'save-by-report response must include fillAssignments')
  return {
    assistRowCount: cellRulesBody.data.assistRows.length,
    assignmentCount: assignmentBody.data.fillAssignments.length
  }
}

async function verifyEmployeeAssistMode(username, password, label, taskOwnedBatchExecution) {
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.chromeExecutable || undefined
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const employeeBrowserDiagnostics = { console: [], pageErrors: [] }
  page.on('console', (message) => {
    if (employeeBrowserDiagnostics.console.length < 100) {
      employeeBrowserDiagnostics.console.push({
        type: message.type(),
        text: redactSecretText(message.text())
      })
    }
  })
  page.on('pageerror', (error) => {
    if (employeeBrowserDiagnostics.pageErrors.length < 20) {
      employeeBrowserDiagnostics.pageErrors.push(redactSecretText(error.message || String(error)))
    }
  })
  try {
    await login(page, username, password, '/mes/pro/feedback/edhr-work-task')
    await page.goto(`${config.baseUrl}/mes/pro/feedback/edhr-work-task`, {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    })
    await page.getByText('我的待办', { exact: false }).first().waitFor({ state: 'visible', timeout: 90000 })
    const toolbar = page.locator('.edhr-work-task-page__toolbar').first()
    await toolbar.locator('.el-form-item').filter({ hasText: '工单' }).locator('input').first().fill(taskOwnedBatchExecution.workOrderCode)
    await toolbar.locator('.el-form-item').filter({ hasText: '批次' }).locator('input').first().fill(taskOwnedBatchExecution.batchCode)
    const filteredResponse = page
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/edhr-work-task/my-page') &&
          response.url().includes(`workOrderCode=${encodeURIComponent(taskOwnedBatchExecution.workOrderCode)}`),
        { timeout: 30000 }
      )
      .catch(() => null)
    await toolbar.getByRole('button', { name: '查询' }).click()
    const filteredTaskResponse = await filteredResponse
    let filteredTaskRows = []
    if (filteredTaskResponse) {
      const filteredTaskData = await readBrowserBusinessData(
        filteredTaskResponse,
        `${label} filtered work task list`
      )
      filteredTaskRows = Array.isArray(filteredTaskData?.list) ? filteredTaskData.list : []
    }
    const matchingApiRows = filteredTaskRows
      .filter(
        (row) =>
          String(row.workOrderCode || '') === String(taskOwnedBatchExecution.workOrderCode) &&
          String(row.batchCode || '') === String(taskOwnedBatchExecution.batchCode)
      )
      .map(sanitizeWorkTaskRow)
    const targetRow = page
      .locator('.el-table__body-wrapper:visible tbody tr')
      .filter({ hasText: taskOwnedBatchExecution.workOrderCode })
      .filter({ hasText: taskOwnedBatchExecution.batchCode })
      .first()
    await targetRow.waitFor({ state: 'visible', timeout: 90000 }).catch(() => undefined)
    const processButton = targetRow.getByRole('button', { name: '处理' }).first()
    if (!(await processButton.isVisible().catch(() => false))) {
      const visibleTableText = await page.locator('.el-table__body-wrapper').first().innerText().catch(() => '')
      throw block('no_openable_task_owned_work_task_for_employee', {
        label,
        username,
        workOrderCode: taskOwnedBatchExecution.workOrderCode,
        batchCode: taskOwnedBatchExecution.batchCode,
        matchingApiRows,
        visibleTableText: visibleTableText.slice(0, 1000)
      })
    }
    const taskOpenResponsePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') &&
          response.request().method() === 'POST',
        { timeout: 30000 }
      )
      .catch(() => null)
    await processButton.click()
    const taskOpenResponse = await taskOpenResponsePromise
    if (!taskOpenResponse) {
      throw block('employee_work_task_open_api_not_called', {
        label,
        username,
        workOrderCode: taskOwnedBatchExecution.workOrderCode,
        batchCode: taskOwnedBatchExecution.batchCode,
        targetBatchTaskId: taskOwnedBatchExecution.targetBatchTaskId,
        matchingApiRows,
        visibleMessages: await page
          .locator('.el-message:visible, .el-notification:visible')
          .allInnerTexts()
          .catch(() => []),
        currentUrl: page.url(),
        employeeBrowserDiagnostics
      })
    }
    const taskOpenData = await readBrowserBusinessData(taskOpenResponse, `${label} open work task`)
    await waitForEmployeeExecutionFormPage(page, {
      label,
      username,
      workOrderCode: taskOwnedBatchExecution.workOrderCode,
      batchCode: taskOwnedBatchExecution.batchCode,
      targetBatchTaskId: taskOwnedBatchExecution.targetBatchTaskId,
      taskOpenData,
      matchingApiRows,
      employeeBrowserDiagnostics
    })
    const assistPanel = page.locator('.edhr-fill-workspace__assist-panel').first()
    await assistPanel.waitFor({ state: 'visible', timeout: 90000 })
    const emptyText = await page.getByText('未配置辅助模式', { exact: false }).first().isVisible().catch(() => false)
    assert.equal(emptyText, false, `${label} should open configured assist mode`)
    const rowCount = await page.locator('.edhr-fill-workspace__assist-row').count()
    assert.ok(rowCount > 0, `${label} assist mode should show at least one row`)
    return {
      label,
      rowCount,
      url: page.url(),
      workOrderCode: taskOwnedBatchExecution.workOrderCode,
      batchCode: taskOwnedBatchExecution.batchCode,
      taskOpenData
    }
  } finally {
    await context.close()
    await browser.close()
  }
}

async function run() {
  const evidence = {
    status: 'RUNNING',
    configPath: config.configPath,
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    tenant: config.tenant || '<missing>',
    accounts: {
      admin: config.adminUsername || '<missing>',
      employeeA: config.employeeAUsername || '<missing>',
      employeeB: config.employeeBUsername || '<missing>'
    },
    batchRecordName: config.batchRecordName || '<missing>',
    routeProductName: config.routeProductName || '<missing>',
    sourceRouteCode: config.sourceRouteCode || '<missing>',
    preferredRouteCode: config.preferredRouteCode || '<missing>',
    targetProcessName: config.targetProcessName || '<missing>',
    targetReportName: config.reportName || '<missing>',
    resultFile
  }
  let adminAuth
  let visualFillConfigBackup
  let restoreAttempted = false
  let cleanupAttempted = false
  const restoreIfNeeded = async () => {
    if (!adminAuth || !visualFillConfigBackup || restoreAttempted) return
    restoreAttempted = true
    await restoreVisualFillConfigFixture(adminAuth, visualFillConfigBackup, evidence)
  }
  const cleanupIfNeeded = async () => {
    if (!adminAuth || cleanupAttempted) return
    cleanupAttempted = true
    evidence.cleanup = await cleanupTaskRuntimeFixtures(adminAuth, evidence)
  }
  try {
    assertPairedWorktreeUrls()
    await assertRuntimeReady()
    const missing = collectMissingPreconditions()
    if (missing.length) {
      throw block('edhr_visual_fill_real_e2e_precondition_missing', { missing })
    }
    runOfficialLoginPreflight(config.adminUsername, config.adminPassword, '/mes/pro/batch-record-form-list', '批记录表单')

    const browser = await chromium.launch({
      headless: !config.headed,
      executablePath: config.chromeExecutable || undefined
    })
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const page = await context.newPage()
    evidence.browserDiagnostics = {
      console: [],
      pageErrors: []
    }
    page.on('console', (message) => {
      if (!['error', 'warning'].includes(message.type())) return
      evidence.browserDiagnostics.console.push({
        type: message.type(),
        text: redactSecretText(message.text()).slice(0, 1000)
      })
    })
    page.on('pageerror', (error) => {
      evidence.browserDiagnostics.pageErrors.push(redactSecretText(error.message).slice(0, 1000))
    })
    try {
      const loginData = await login(page, config.adminUsername, config.adminPassword, '/mes/pro/batch-record-form-list')
      const tenantId = await resolveTenantId()
      const accessToken = loginData.accessToken || loginData.access_token
      if (!accessToken) {
        throw block('admin_login_response_missing_access_token')
      }
      adminAuth = { accessToken, tenantId }
      const fixtureReport = await ensureTaskOwnedReportFixture(adminAuth, evidence)
      visualFillConfigBackup = await captureVisualFillConfigBackup(adminAuth, fixtureReport, evidence)
      evidence.fixtureSetup.visualFillSeed = await ensureConfiguredVisualFillFixture(
        adminAuth,
        fixtureReport,
        evidence
      )
      await openVisualFillConfigDialog(page, fixtureReport)
      evidence.adminSave = await saveVisualFillConfigDialog(page)
      evidence.adminConfigDialog = {
        visible: true,
        reportName: fixtureReport.reportName,
        batchRecordName: fixtureReport.batchRecordName || config.batchRecordName
      }
      await prepareTaskOwnedRouteThroughUi(page, adminAuth, fixtureReport, evidence)
      evidence.taskOwnedBatchExecution = await ensureTaskOwnedWorkOrderForVisualFill(
        adminAuth,
        fixtureReport,
        evidence
      )
    } finally {
      await context.close()
      await browser.close()
    }

    evidence.employeeA = await verifyEmployeeAssistMode(
      config.employeeAUsername,
      config.employeeAPassword,
      'employeeA',
      evidence.taskOwnedBatchExecution
    )
    evidence.employeeB = await verifyEmployeeAssistMode(
      config.employeeBUsername,
      config.employeeBPassword,
      'employeeB',
      evidence.taskOwnedBatchExecution
    )
    await cleanupIfNeeded()
    await restoreIfNeeded()
    evidence.status = 'PASS'
    writeResult(evidence)
    console.log(JSON.stringify(evidence, null, 2))
  } catch (error) {
    try {
      await cleanupIfNeeded()
    } catch (cleanupError) {
      evidence.cleanupError = {
        message: redactSecretText(cleanupError.message),
        details: sanitizeEvidence(cleanupError.details),
        stack: redactSecretText(cleanupError.stack)
      }
    }
    try {
      await restoreIfNeeded()
    } catch (restoreError) {
      evidence.restoreError = {
        message: redactSecretText(restoreError.message),
        details: sanitizeEvidence(restoreError.details),
        stack: redactSecretText(restoreError.stack)
      }
    }
    evidence.status = error.blocked ? 'BLOCKED' : 'FAIL'
    evidence.error = {
      message: redactSecretText(error.message),
      details: sanitizeEvidence(error.details),
      stack: redactSecretText(error.stack)
    }
    writeResult(evidence)
    console.error(JSON.stringify(evidence, null, 2))
    process.exitCode = 1
  }
}

if (cliArgs.cleanupOnly) {
  runCleanupOnly()
} else {
  run()
}
