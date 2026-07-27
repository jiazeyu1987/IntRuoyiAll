const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '..', '..')
const projectRoot = path.resolve(frontendRoot, '..')
const resultDir = path.join(frontendRoot, 'test-results', 'edhr-visual-fill-config-real-flow')
const resultFile = path.join(resultDir, 'result.json')
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
    if (arg === '--config') {
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

function writeResult(result) {
  fs.mkdirSync(resultDir, { recursive: true })
  fs.writeFileSync(resultFile, `${JSON.stringify(sanitizeEvidence(result), null, 2)}\n`, 'utf8')
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
  assert.ok(frontendSlot >= 1 && frontendSlot <= 19, `frontend port must be int_main worktree slot 1..19, got ${frontendPort}`)
  assert.equal(frontendSlot, backendSlot, `frontend/backend ports must use same slot, got ${frontendPort}/${backendPort}`)
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
    rowKey: `CODX_VFC_ASSIST_${index + 1}`,
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

async function ensureTaskOwnedWorkOrderForVisualFill(auth, evidence) {
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
  const routeOption =
    enabledRouteOptions.find((option) => String(option.routeName || '') === config.batchRecordName) ||
    enabledRouteOptions.find((option) => String(option.routeName || '') === config.routeProductName) ||
    enabledRouteOptions.find((option) => String(option.routeCode || '').trim())
  if (!routeOption) {
    throw block('task_owned_work_order_has_no_enabled_batch_route', {
      workOrderCode: created.workOrderCode,
      workOrderId: workOrder.id,
      productId: workOrder.productId,
      productCode: workOrder.productCode,
      routeOptions
    })
  }
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
    status: batchExecution?.status
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
      timeout: 120000
    }
  )
  if (result.status !== 0) {
    throw block('official_login_preflight_failed', {
      status: result.status,
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
  await targetRow.click()
  const actionBar = page.locator('.batch-record-form-preview__actions').first()
  await actionBar.waitFor({ state: 'visible', timeout: 30000 })
  await actionBar.getByRole('button', { name: '填写配置' }).click()
  const dialog = page.locator('.el-overlay:visible .batch-record-cell-rules-editor').last()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })
  await dialog.locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 90000 })
  const firstRuleCell = dialog
    .locator('.batch-record-cell-rules-editor__cell.is-rule .batch-record-cell-rules-editor__cell-button')
    .first()
  await firstRuleCell.waitFor({ state: 'visible', timeout: 90000 })
  await firstRuleCell.scrollIntoViewIfNeeded()
  await firstRuleCell.click()
  await dialog.getByText('辅助行配置', { exact: false }).waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText('辅助行填写人', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText('字段类型', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

async function saveVisualFillConfigDialog(page) {
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
  await page.getByRole('button', { name: '保存填写配置' }).last().click()
  const [cellRulesResponse, assignmentResponse] = await Promise.all([
    cellRulesResponsePromise,
    assignmentResponsePromise
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
    await filteredResponse
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
        visibleTableText: visibleTableText.slice(0, 1000)
      })
    }
    await processButton.click()
    await page.waitForURL((url) => url.pathname === '/mes/pro/feedback/edhr-execution/form', { timeout: 90000 })
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
      batchCode: taskOwnedBatchExecution.batchCode
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
    targetReportName: config.reportName || '<missing>',
    resultFile
  }
  let adminAuth
  let visualFillConfigBackup
  let restoreAttempted = false
  const restoreIfNeeded = async () => {
    if (!adminAuth || !visualFillConfigBackup || restoreAttempted) return
    restoreAttempted = true
    await restoreVisualFillConfigFixture(adminAuth, visualFillConfigBackup, evidence)
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
      evidence.taskOwnedBatchExecution = await ensureTaskOwnedWorkOrderForVisualFill(
        adminAuth,
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
    await restoreIfNeeded()
    evidence.status = 'PASS'
    writeResult(evidence)
    console.log(JSON.stringify(evidence, null, 2))
  } catch (error) {
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

run()
