const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const SCHEMA_VERSION = 'AORD_V4_M0_A6_PREFLIGHT_V1'
const SIDE_EFFECTS_NONE = Object.freeze({
  browserBusinessWrites: 0,
  businessApiWrites: 0,
  sqlWrites: 0,
  manifestCreated: false
})

const ACTORS = Object.freeze([
  {
    role: 'PRODUCTION_WORKER',
    usernameKey: 'AORD_V4_M0_PRODUCTION_WORKER_USERNAME',
    passwordKey: 'AORD_V4_M0_PRODUCTION_WORKER_PASSWORD',
    signaturePasswordKey: 'AORD_V4_M0_PRODUCTION_WORKER_SIGNATURE_PASSWORD'
  },
  {
    role: 'PRODUCTION_LEADER',
    usernameKey: 'AORD_V4_M0_PRODUCTION_LEADER_USERNAME',
    passwordKey: 'AORD_V4_M0_PRODUCTION_LEADER_PASSWORD',
    signaturePasswordKey: 'AORD_V4_M0_PRODUCTION_LEADER_SIGNATURE_PASSWORD'
  },
  {
    role: 'PQC_INSPECTOR',
    usernameKey: 'AORD_V4_M0_PQC_INSPECTOR_USERNAME',
    passwordKey: 'AORD_V4_M0_PQC_INSPECTOR_PASSWORD',
    signaturePasswordKey: 'AORD_V4_M0_PQC_INSPECTOR_SIGNATURE_PASSWORD'
  },
  {
    role: 'PQC_LEADER',
    usernameKey: 'AORD_V4_M0_PQC_LEADER_USERNAME',
    passwordKey: 'AORD_V4_M0_PQC_LEADER_PASSWORD',
    signaturePasswordKey: 'AORD_V4_M0_PQC_LEADER_SIGNATURE_PASSWORD'
  },
  {
    role: 'RELEASE_OWNER',
    usernameKey: 'AORD_V4_M0_RELEASE_OWNER_USERNAME',
    passwordKey: 'AORD_V4_M0_RELEASE_OWNER_PASSWORD',
    signaturePasswordKey: 'AORD_V4_M0_RELEASE_OWNER_SIGNATURE_PASSWORD'
  }
])

const REQUIRED_ENV_KEYS = Object.freeze([
  'AORD_V4_M0_AUTHORIZATION_TOKEN',
  'AORD_V4_M0_TENANT_ID',
  'AORD_V4_M0_TENANT_NAME',
  'AORD_V4_M0_FRONTEND_URL',
  'AORD_V4_M0_BACKEND_URL',
  'AORD_V4_M0_BROWSER_PATH',
  'AORD_V4_M0_DB_CONTAINER',
  'AORD_V4_M0_DB_SCHEMA',
  'AORD_V4_M0_PRODUCT_ID',
  'AORD_V4_M0_ROUTE_ID',
  'AORD_V4_M0_ROUTE_VERSION_ID',
  'AORD_V4_M0_ROUTE_PROCESS_IDS',
  ...ACTORS.flatMap((actor) => [actor.usernameKey, actor.passwordKey, actor.signaturePasswordKey])
])

const SECRET_ENV_KEYS = Object.freeze([
  'AORD_V4_M0_AUTHORIZATION_TOKEN',
  ...ACTORS.flatMap((actor) => [actor.passwordKey, actor.signaturePasswordKey])
])

class PreflightBlockedError extends Error {
  constructor(checkKey, blockerType, message, details = {}) {
    super(message)
    this.name = 'PreflightBlockedError'
    this.checkKey = checkKey
    this.blockerType = blockerType
    this.details = details
  }
}

function parseArgs(argv) {
  const args = new Map()
  for (let index = 0; index < argv.length; index += 1) {
    const key = argv[index]
    if (!key.startsWith('--')) continue
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) {
      args.set(key.slice(2), 'true')
    } else {
      args.set(key.slice(2), value)
      index += 1
    }
  }
  return args
}

function collectSecretValues(env) {
  return SECRET_ENV_KEYS.map((key) => String(env[key] || ''))
    .filter((value) => value.length > 0)
    .sort((left, right) => right.length - left.length)
}

function sanitizeText(value, secretValues) {
  let text = String(value || '')
  for (const secret of secretValues) {
    if (secret) text = text.split(secret).join('[REDACTED]')
  }
  return text
}

function sanitizeValue(value, secretValues) {
  if (typeof value === 'string') return sanitizeText(value, secretValues)
  if (Array.isArray(value)) return value.map((item) => sanitizeValue(item, secretValues))
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, sanitizeValue(item, secretValues)])
    )
  }
  return value
}

function sanitizeForOutput(error, secretValues) {
  const rawDetails = error && typeof error.details === 'object' && error.details ? error.details : {}
  const safeDetails = sanitizeValue(rawDetails, secretValues)
  return {
    checkKey: error.checkKey || 'PREFLIGHT_RUNTIME',
    blockerType: error.blockerType || 'PREFLIGHT_ERROR',
    message: sanitizeText(error.message || error, secretValues),
    ...safeDetails
  }
}

function assertNoSecretLeak(result, secretValues) {
  const serialized = JSON.stringify(result)
  for (const secret of secretValues) {
    if (secret.length >= 4) {
      assert.ok(!serialized.includes(secret), 'structured preflight result contains a secret value')
    }
  }
}

function requirePositiveInteger(rawValue, key) {
  if (!/^\d+$/.test(rawValue)) {
    throw new PreflightBlockedError('ENVIRONMENT', 'INVALID_ENV', `${key} must be a positive integer`, {
      invalidEnvKeys: [key]
    })
  }
  const value = Number(rawValue)
  if (!Number.isSafeInteger(value) || value <= 0) {
    throw new PreflightBlockedError('ENVIRONMENT', 'INVALID_ENV', `${key} must be a safe positive integer`, {
      invalidEnvKeys: [key]
    })
  }
  return value
}

function validateExplicitEnvironment(env) {
  const missingEnvKeys = REQUIRED_ENV_KEYS.filter((key) => !String(env[key] || '').trim()).sort()
  if (missingEnvKeys.length > 0) {
    throw new PreflightBlockedError(
      'ENVIRONMENT',
      'MISSING_EXPLICIT_ENV',
      'A6 preflight is missing explicit authorization, fixture, account, or signature environment variables',
      { missingEnvKeys }
    )
  }

  const authorizationToken = String(env.AORD_V4_M0_AUTHORIZATION_TOKEN).trim()
  if (!/^USER_APPROVED_[A-Z0-9][A-Z0-9_:-]{7,}$/.test(authorizationToken)) {
    throw new PreflightBlockedError(
      'AUTHORIZED_TENANT',
      'INVALID_AUTHORIZATION_TOKEN',
      'AORD_V4_M0_AUTHORIZATION_TOKEN must be an explicit USER_APPROVED_* authorization marker',
      { invalidEnvKeys: ['AORD_V4_M0_AUTHORIZATION_TOKEN'] }
    )
  }

  const tenantName = String(env.AORD_V4_M0_TENANT_NAME).trim()
  if (/(^|[^a-z])(prod|production)([^a-z]|$)|正式|生产环境/i.test(tenantName)) {
    throw new PreflightBlockedError(
      'AUTHORIZED_TENANT',
      'PRODUCTION_TENANT_FORBIDDEN',
      'A6 write E2E requires a confirmed non-production tenant',
      { tenantLabel: tenantName }
    )
  }

  let frontendUrl
  try {
    frontendUrl = new URL(String(env.AORD_V4_M0_FRONTEND_URL).trim())
  } catch {
    throw new PreflightBlockedError('ENVIRONMENT', 'INVALID_FRONTEND_URL', 'invalid explicit frontend URL')
  }
  if (!['127.0.0.1', 'localhost'].includes(frontendUrl.hostname) || frontendUrl.port !== '8081') {
    throw new PreflightBlockedError(
      'ENVIRONMENT',
      'NON_LOCAL_FRONTEND_FORBIDDEN',
      'A6 preflight only permits the int_main local frontend on port 8081',
      { frontendOrigin: frontendUrl.origin }
    )
  }

  let backendUrl
  try {
    backendUrl = new URL(String(env.AORD_V4_M0_BACKEND_URL).trim())
  } catch {
    throw new PreflightBlockedError('ENVIRONMENT', 'INVALID_BACKEND_URL', 'invalid explicit backend URL')
  }
  if (!['127.0.0.1', 'localhost'].includes(backendUrl.hostname) || backendUrl.port !== '48081') {
    throw new PreflightBlockedError(
      'ENVIRONMENT',
      'NON_LOCAL_BACKEND_FORBIDDEN',
      'A6 preflight only permits the int_main local backend on port 48081',
      { backendOrigin: backendUrl.origin }
    )
  }

  const browserPath = path.resolve(String(env.AORD_V4_M0_BROWSER_PATH).trim())
  if (!fs.existsSync(browserPath) || !fs.statSync(browserPath).isFile()) {
    throw new PreflightBlockedError('BROWSER', 'BROWSER_EXECUTABLE_MISSING', 'explicit browser executable is missing', {
      browserPath
    })
  }

  const dbContainer = String(env.AORD_V4_M0_DB_CONTAINER).trim()
  const dbSchema = String(env.AORD_V4_M0_DB_SCHEMA).trim()
  if (!/^[a-zA-Z0-9][a-zA-Z0-9_.-]*$/.test(dbContainer) || !/^[a-zA-Z0-9_-]+$/.test(dbSchema)) {
    throw new PreflightBlockedError(
      'DATABASE',
      'INVALID_DATABASE_TARGET',
      'database container or schema identifier is invalid'
    )
  }

  const routeProcessIds = String(env.AORD_V4_M0_ROUTE_PROCESS_IDS)
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean)
    .map((value) => requirePositiveInteger(value, 'AORD_V4_M0_ROUTE_PROCESS_IDS'))
  const stableRouteProcessIds = [...new Set(routeProcessIds)].sort((left, right) => left - right)
  if (stableRouteProcessIds.length === 0 || stableRouteProcessIds.length !== routeProcessIds.length) {
    throw new PreflightBlockedError(
      'ENVIRONMENT',
      'INVALID_ROUTE_PROCESS_IDS',
      'route process IDs must be a non-empty unique comma-separated list'
    )
  }

  const actors = ACTORS.map((definition) => {
    const username = String(env[definition.usernameKey]).trim()
    const password = String(env[definition.passwordKey])
    const signaturePassword = String(env[definition.signaturePasswordKey])
    if (!/^[A-Za-z0-9]+$/.test(username)) {
      throw new PreflightBlockedError(
        'ACCOUNT_CREDENTIALS',
        'INVALID_USERNAME',
        `${definition.usernameKey} must contain only letters and digits`,
        { invalidEnvKeys: [definition.usernameKey], role: definition.role }
      )
    }
    if (!password || !signaturePassword) {
      throw new PreflightBlockedError(
        'ACCOUNT_CREDENTIALS',
        'MISSING_EXPLICIT_SECRET',
        'explicit login and signature secrets must be non-empty',
        { role: definition.role, invalidEnvKeys: [definition.passwordKey, definition.signaturePasswordKey] }
      )
    }
    return { role: definition.role, username, password, signaturePassword }
  })
  if (new Set(actors.map((actor) => actor.username.toLowerCase())).size !== actors.length) {
    throw new PreflightBlockedError(
      'ACCOUNT_CREDENTIALS',
      'DUPLICATE_ROLE_ACCOUNT',
      'the five A6 role slots require five distinct login accounts'
    )
  }

  return {
    authorizationToken,
    tenantId: requirePositiveInteger(String(env.AORD_V4_M0_TENANT_ID).trim(), 'AORD_V4_M0_TENANT_ID'),
    tenantName,
    frontendUrl: frontendUrl.origin,
    backendUrl: backendUrl.origin,
    browserPath,
    dbContainer,
    dbSchema,
    productId: requirePositiveInteger(String(env.AORD_V4_M0_PRODUCT_ID).trim(), 'AORD_V4_M0_PRODUCT_ID'),
    routeId: requirePositiveInteger(String(env.AORD_V4_M0_ROUTE_ID).trim(), 'AORD_V4_M0_ROUTE_ID'),
    routeVersionId: requirePositiveInteger(
      String(env.AORD_V4_M0_ROUTE_VERSION_ID).trim(),
      'AORD_V4_M0_ROUTE_VERSION_ID'
    ),
    routeProcessIds: stableRouteProcessIds,
    actors
  }
}

function assertReadOnlySql(sql) {
  const normalized = String(sql).trim()
  if (!/^(SELECT|SHOW)\b/i.test(normalized)) {
    throw new Error('preflight SQL must begin with SELECT or SHOW')
  }
  const forbidden = /\b(?:INSERT|UPDATE|DELETE|REPLACE|MERGE|CALL|ALTER|DROP|CREATE|TRUNCATE|GRANT|REVOKE|SET)\b/i
  if (forbidden.test(normalized)) {
    throw new Error('preflight SQL contains a forbidden write or administrative statement')
  }
}

function runReadOnlyMysql(environment, sql) {
  assertReadOnlySql(sql)
  const mysqlCommand = 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -B "$1"'
  const run = spawnSync(
    'docker',
    ['exec', '-i', environment.dbContainer, 'sh', '-lc', mysqlCommand, 'a6-preflight', environment.dbSchema],
    { input: `${sql.trim()}\n`, encoding: 'utf8', windowsHide: true }
  )
  if (run.error || run.status !== 0) {
    throw new PreflightBlockedError(
      'DATABASE',
      'READ_ONLY_DATABASE_UNAVAILABLE',
      run.error ? run.error.message : run.stderr || `docker/mysql exited ${run.status}`
    )
  }
  return String(run.stdout || '')
    .split(/\r?\n/)
    .filter((line) => line.length > 0)
    .map((line) => line.split('\t'))
}

function sqlString(value) {
  if (!/^[A-Za-z0-9][A-Za-z0-9._:-]*$/.test(value)) {
    throw new PreflightBlockedError(
      'FORMAL_IDENTITY',
      'INVALID_FORMAL_IDENTITY',
      'a formal account or report identity contains unsupported SQL literal characters'
    )
  }
  return `'${value}'`
}

async function runReadOnlyRuntimeChecks(environment) {
  let frontendResponse
  let backendResponse
  try {
    frontendResponse = await fetch(environment.frontendUrl, { method: 'GET', redirect: 'manual' })
    backendResponse = await fetch(new URL('/actuator/health', environment.backendUrl), {
      method: 'GET',
      redirect: 'manual'
    })
  } catch (error) {
    throw new PreflightBlockedError(
      'INT_MAIN_RUNTIME',
      'LOCAL_RUNTIME_UNAVAILABLE',
      'the explicit int_main frontend or backend is unreachable',
      { reason: error instanceof Error ? error.message : String(error) }
    )
  }
  if (!frontendResponse.ok || !backendResponse.ok) {
    throw new PreflightBlockedError(
      'INT_MAIN_RUNTIME',
      'LOCAL_RUNTIME_UNHEALTHY',
      'the explicit int_main frontend or backend returned a non-success status',
      { frontendStatus: frontendResponse.status, backendStatus: backendResponse.status }
    )
  }
  let backendHealth
  try {
    backendHealth = await backendResponse.json()
  } catch {
    throw new PreflightBlockedError(
      'INT_MAIN_RUNTIME',
      'BACKEND_HEALTH_RESPONSE_INVALID',
      'the explicit int_main backend health response is not JSON'
    )
  }
  if (backendHealth?.status !== 'UP') {
    throw new PreflightBlockedError(
      'INT_MAIN_RUNTIME',
      'BACKEND_NOT_UP',
      'the explicit int_main backend health status is not UP',
      { backendStatus: backendHealth?.status || 'MISSING' }
    )
  }
  return {
    frontend: { origin: environment.frontendUrl, status: frontendResponse.status },
    backend: { origin: environment.backendUrl, status: backendResponse.status, health: backendHealth.status }
  }
}

function requireExactly(rows, expected, checkKey, blockerType, message, details = {}) {
  if (rows.length !== expected) {
    throw new PreflightBlockedError(checkKey, blockerType, message, {
      expectedCount: expected,
      actualCount: rows.length,
      ...details
    })
  }
}

function runReadOnlyDatabaseChecks(environment) {
  const processIdsSql = environment.routeProcessIds.join(',')
  const identityRows = runReadOnlyMysql(
    environment,
    `SELECT r.id, rv.id, rv.lifecycle_status, CAST(rv.active AS UNSIGNED), rp.item_id, COUNT(DISTINCT p.id)
       FROM mes_pro_route r
       JOIN mes_pro_route_version rv ON rv.route_id = r.id AND rv.tenant_id = r.tenant_id AND rv.deleted = 0
       JOIN mes_pro_route_product rp ON rp.route_id = r.id AND rp.tenant_id = r.tenant_id AND rp.deleted = 0
       JOIN mes_pro_route_process p ON p.route_id = r.id AND p.tenant_id = r.tenant_id AND p.deleted = 0
      WHERE r.tenant_id = ${environment.tenantId}
        AND r.id = ${environment.routeId}
        AND r.deleted = 0
        AND rv.id = ${environment.routeVersionId}
        AND rp.item_id = ${environment.productId}
        AND p.id IN (${processIdsSql})
      GROUP BY r.id, rv.id, rv.lifecycle_status, rv.active, rp.item_id`
  )
  requireExactly(
    identityRows,
    1,
    'FORMAL_ROUTE',
    'FORMAL_ROUTE_IDENTITY_REQUIRED',
    'explicit product, route, published version, and route processes do not resolve as one formal fixture'
  )
  const identity = identityRows[0]
  if (identity[2] !== 'ACTIVE' || identity[3] !== '1' || Number(identity[5]) !== environment.routeProcessIds.length) {
    throw new PreflightBlockedError(
      'FORMAL_ROUTE',
      'PUBLISHED_ROUTE_VERSION_REQUIRED',
      'explicit route version is not the active published version or does not contain every target process',
      { lifecycleStatus: identity[2], active: identity[3], resolvedProcessCount: Number(identity[5]) }
    )
  }

  const usernames = environment.actors.map((actor) => sqlString(actor.username)).join(',')
  const accountRows = runReadOnlyMysql(
    environment,
    `SELECT u.username, u.id, u.status,
            CAST(COALESCE(a.electronic_signature_enabled, 0) AS UNSIGNED),
            COALESCE(a.authorization_state, 'MISSING'),
            SUM(CASE WHEN i.active = 1 AND i.image_status = 'ACTIVE' AND i.deleted = 0 THEN 1 ELSE 0 END)
       FROM system_users u
       LEFT JOIN dcc_electronic_signature_authorization a
         ON a.user_id = u.id AND a.tenant_id = u.tenant_id AND a.deleted = 0
       LEFT JOIN dcc_electronic_signature_image i
         ON i.user_id = u.id AND i.tenant_id = u.tenant_id
      WHERE u.tenant_id = ${environment.tenantId}
        AND u.deleted = 0
        AND u.username IN (${usernames})
      GROUP BY u.username, u.id, u.status, a.electronic_signature_enabled, a.authorization_state
      ORDER BY u.username`
  )
  requireExactly(
    accountRows,
    environment.actors.length,
    'ACCOUNT_SIGNATURES',
    'FIVE_ROLE_ACCOUNTS_REQUIRED',
    'not every explicit role account resolves in the authorized tenant'
  )
  const accountByUsername = new Map()
  for (const row of accountRows) {
    if (row[2] !== '0' || Number(row[3]) !== 1 || row[4] !== 'ENABLED' || Number(row[5]) < 1) {
      throw new PreflightBlockedError(
        'ACCOUNT_SIGNATURES',
        'ACTIVE_SIGNATURE_REQUIRED',
        'an explicit role account is disabled or lacks an enabled active signature image',
        { accountLabel: row[0], userId: Number(row[1]) }
      )
    }
    accountByUsername.set(row[0].toLowerCase(), Number(row[1]))
  }

  const bindingRows = runReadOnlyMysql(
    environment,
    `SELECT b.route_process_id, b.form_slot_type, b.record_category,
            b.batch_record_report_id, b.batch_record_definition_id, b.batch_record_version_id,
            r.id, r.form_slot_type, d.current_version_id, v.status
       FROM mes_pro_route_flow_process_batch_record b
       JOIN mes_pro_batch_record_report r
         ON r.report_id = b.batch_record_report_id
        AND r.tenant_id = b.tenant_id
        AND r.deleted = 0
       JOIN mes_pro_batch_record_definition d
         ON d.id = b.batch_record_definition_id
        AND d.tenant_id = b.tenant_id
        AND d.deleted = 0
       JOIN mes_pro_batch_record_version v
         ON v.id = b.batch_record_version_id
        AND v.definition_id = d.id
        AND v.tenant_id = b.tenant_id
        AND v.deleted = 0
      WHERE b.tenant_id = ${environment.tenantId}
        AND b.route_id = ${environment.routeId}
        AND b.route_process_id IN (${processIdsSql})
        AND b.use_type = 'BATCH'
        AND b.form_slot_type IN ('MAIN', 'PROCESS_INSPECTION', 'LOSS_REPORT')
        AND b.batch_record_report_id IS NOT NULL
        AND b.batch_record_report_id <> ''
        AND b.batch_record_definition_id IS NOT NULL
        AND b.batch_record_version_id IS NOT NULL
        AND b.deleted = 0
      ORDER BY b.route_process_id, b.form_slot_type, b.id`
  )
  const expectedBindingCount = environment.routeProcessIds.length * 3
  requireExactly(
    bindingRows,
    expectedBindingCount,
    'TRADITIONAL_REPORT_BINDINGS',
    'THREE_TRADITIONAL_REPORTS_REQUIRED',
    'every route process requires unique non-empty MAIN, PROCESS_INSPECTION, and LOSS_REPORT report identities',
    { routeProcessIds: environment.routeProcessIds }
  )
  const bindings = new Map()
  for (const row of bindingRows) {
    const routeProcessId = Number(row[0])
    const formSlotType = row[1]
    const expectedCategory = formSlotType === 'MAIN' ? 'BATCH_RECORD' : 'INTERNAL_RECORD'
    const key = `${routeProcessId}:${formSlotType}`
    if (
      bindings.has(key) ||
      row[2] !== expectedCategory ||
      row[7] !== formSlotType ||
      Number(row[8]) !== Number(row[5]) ||
      row[9] !== 'APPROVED'
    ) {
      throw new PreflightBlockedError(
        'TRADITIONAL_REPORT_BINDINGS',
        'TRADITIONAL_REPORT_METADATA_INVALID',
        'traditional report identity, category, definition, current version, or APPROVED status is invalid',
        { routeProcessId, formSlotType }
      )
    }
    bindings.set(key, {
      routeProcessId,
      formSlotType,
      reportId: row[3],
      definitionId: Number(row[4]),
      versionId: Number(row[5])
    })
  }

  const qaRows = runReadOnlyMysql(
    environment,
    `SELECT q.route_process_id, q.process_id, q.current_version_id,
            q.lifecycle_status, v.lifecycle_status,
            (SELECT COUNT(*)
               FROM mes_qa_inspection_regulation_item i
              WHERE i.tenant_id = q.tenant_id
                AND i.regulation_version_id = v.id
                AND i.deleted = 0) AS item_count,
            (SELECT COUNT(*)
               FROM mes_qa_inspection_regulation_item i
              WHERE i.tenant_id = q.tenant_id
                AND i.regulation_version_id = v.id
                AND i.equipment_required = 1
                AND i.deleted = 0
                AND NOT EXISTS (
                  SELECT 1
                    FROM mes_qa_inspection_regulation_item_equipment e
                   WHERE e.tenant_id = i.tenant_id
                     AND e.regulation_version_id = i.regulation_version_id
                     AND e.inspection_type = i.inspection_type
                     AND e.item_code = i.item_code
                     AND e.deleted = 0
                )) AS missing_equipment_count
       FROM mes_qa_inspection_regulation q
       JOIN mes_qa_inspection_regulation_version v
         ON v.id = q.current_version_id
        AND v.tenant_id = q.tenant_id
        AND v.deleted = 0
      WHERE q.tenant_id = ${environment.tenantId}
        AND q.product_id = ${environment.productId}
        AND q.route_id = ${environment.routeId}
        AND q.route_version_id = ${environment.routeVersionId}
        AND q.route_process_id IN (${processIdsSql})
        AND q.deleted = 0
      ORDER BY q.route_process_id, q.id`
  )
  requireExactly(
    qaRows,
    environment.routeProcessIds.length,
    'PUBLISHED_QA',
    'PUBLISHED_QA_REQUIRED',
    'each target route process requires one current published QA regulation'
  )
  const qaEvidence = []
  for (const row of qaRows) {
    if (row[3] !== 'PUBLISHED' || row[4] !== 'PUBLISHED' || Number(row[5]) < 1 || Number(row[6]) !== 0) {
      throw new PreflightBlockedError(
        'PUBLISHED_QA',
        'QA_ITEMS_OR_EQUIPMENT_REQUIRED',
        'published QA items or required equipment mappings are incomplete',
        { routeProcessId: Number(row[0]), regulationVersionId: Number(row[2]) }
      )
    }
    qaEvidence.push({
      routeProcessId: Number(row[0]),
      processId: Number(row[1]),
      regulationVersionId: Number(row[2]),
      itemCount: Number(row[5])
    })
  }

  const reportIds = [...bindings.values()].map((binding) => sqlString(binding.reportId)).join(',')
  const mappingRows = runReadOnlyMysql(
    environment,
    `SELECT target_report_id, batch_record_definition_id, batch_record_version_id, source_type, COUNT(*)
       FROM mes_pro_batch_record_cell_link_rule
      WHERE tenant_id = ${environment.tenantId}
        AND target_report_id IN (${reportIds})
        AND source_type IN ('PROCESS_POOL_REPORT', 'PQC_AGGREGATE_DETAIL', 'PRODUCTION_LOSS')
        AND enabled = 1
        AND deleted = 0
      GROUP BY target_report_id, batch_record_definition_id, batch_record_version_id, source_type
      ORDER BY target_report_id, source_type`
  )
  const mappingKeys = new Set(
    mappingRows
      .filter((row) => Number(row[4]) > 0)
      .map((row) => `${row[0]}:${Number(row[1])}:${Number(row[2])}:${row[3]}`)
  )
  const sourceTypeBySlot = {
    MAIN: 'PROCESS_POOL_REPORT',
    PROCESS_INSPECTION: 'PQC_AGGREGATE_DETAIL',
    LOSS_REPORT: 'PRODUCTION_LOSS'
  }
  for (const binding of bindings.values()) {
    const sourceType = sourceTypeBySlot[binding.formSlotType]
    const mappingKey = `${binding.reportId}:${binding.definitionId}:${binding.versionId}:${sourceType}`
    if (!mappingKeys.has(mappingKey)) {
      throw new PreflightBlockedError(
        'FORMAL_CELL_MAPPINGS',
        'FORMAL_MAPPING_REQUIRED',
        'a traditional report lacks its required enabled source mapping',
        { routeProcessId: binding.routeProcessId, formSlotType: binding.formSlotType, sourceType }
      )
    }
  }

  const releaseRuleRows = runReadOnlyMysql(
    environment,
    `SELECT id, COALESCE(candidate_source_type, 'USER'), candidate_source_id, assignee_user_id
       FROM mes_pro_edhr_work_task_assignment_rule
      WHERE tenant_id = ${environment.tenantId}
        AND scope_type = 'ROUTE'
        AND scope_id = ${environment.routeId}
        AND task_type = 'RELEASE_APPROVE'
        AND enabled = 1
        AND deleted = 0
      ORDER BY id`
  )
  requireExactly(
    releaseRuleRows,
    1,
    'RELEASE_APPROVE',
    'UNIQUE_RELEASE_APPROVE_RULE_REQUIRED',
    'the route requires exactly one enabled route-level RELEASE_APPROVE rule'
  )
  const releaseRule = releaseRuleRows[0]
  const candidateSourceType = releaseRule[1]
  const candidateSourceId = Number(releaseRule[2] || releaseRule[3])
  if (!['USER', 'ROLE_GROUP', 'DEPT_GROUP'].includes(candidateSourceType) || !candidateSourceId) {
    throw new PreflightBlockedError(
      'RELEASE_APPROVE',
      'RELEASE_APPROVE_CANDIDATE_SOURCE_INVALID',
      'release candidate source must be USER, ROLE_GROUP, or DEPT_GROUP with a positive source ID'
    )
  }
  let candidateSql
  if (candidateSourceType === 'USER') {
    candidateSql = `SELECT id FROM system_users WHERE tenant_id = ${environment.tenantId} AND id = ${candidateSourceId} AND status = 0 AND deleted = 0 ORDER BY id`
  } else if (candidateSourceType === 'ROLE_GROUP') {
    candidateSql = `SELECT DISTINCT u.id
                      FROM system_user_role ur
                      JOIN system_role r ON r.id = ur.role_id AND r.tenant_id = ur.tenant_id AND r.status = 0 AND r.deleted = 0
                      JOIN system_users u ON u.id = ur.user_id AND u.tenant_id = ur.tenant_id AND u.status = 0 AND u.deleted = 0
                     WHERE ur.tenant_id = ${environment.tenantId} AND ur.role_id = ${candidateSourceId} AND ur.deleted = 0
                     ORDER BY u.id`
  } else {
    candidateSql = `SELECT id FROM system_users WHERE tenant_id = ${environment.tenantId} AND dept_id = ${candidateSourceId} AND status = 0 AND deleted = 0 ORDER BY id`
  }
  const candidateUserIds = runReadOnlyMysql(environment, candidateSql).map((row) => Number(row[0]))
  if (candidateUserIds.length === 0) {
    throw new PreflightBlockedError(
      'RELEASE_APPROVE',
      'RELEASE_APPROVE_CANDIDATE_EMPTY',
      'the formal RELEASE_APPROVE rule resolves no enabled users'
    )
  }
  const releaseOwner = environment.actors.find((actor) => actor.role === 'RELEASE_OWNER')
  const releaseOwnerUserId = accountByUsername.get(releaseOwner.username.toLowerCase())
  if (!candidateUserIds.includes(releaseOwnerUserId)) {
    throw new PreflightBlockedError(
      'RELEASE_APPROVE',
      'EXPLICIT_RELEASE_OWNER_NOT_CANDIDATE',
      'the explicit release owner account is not resolved by the formal RELEASE_APPROVE rule',
      { releaseOwnerUserId }
    )
  }

  return {
    accounts: environment.actors.map((actor) => ({
      role: actor.role,
      userId: accountByUsername.get(actor.username.toLowerCase()),
      signatureConfigured: true,
      signatureSecretPresent: true
    })),
    route: {
      productId: environment.productId,
      routeId: environment.routeId,
      routeVersionId: environment.routeVersionId,
      routeProcessIds: environment.routeProcessIds
    },
    traditionalBindings: [...bindings.values()].sort((left, right) =>
      left.routeProcessId - right.routeProcessId || left.formSlotType.localeCompare(right.formSlotType)
    ),
    qa: qaEvidence.sort((left, right) => left.routeProcessId - right.routeProcessId),
    requiredMappingSourceTypes: Object.values(sourceTypeBySlot).sort(),
    releaseApprove: {
      ruleId: Number(releaseRule[0]),
      candidateSourceType,
      candidateSourceId,
      candidateUserIds: candidateUserIds.sort((left, right) => left - right)
    }
  }
}

function installReadOnlyObservers(page, actorRole) {
  const evidence = {
    role: actorRole,
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    errorResponses: [],
    blockedBusinessWrites: []
  }
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    evidence.requestFailures.push({ url: request.url(), method: request.method(), error: request.failure()?.errorText })
  })
  page.on('response', (response) => {
    if (response.status() >= 400 && response.url().includes('/admin-api/')) {
      evidence.errorResponses.push({ url: response.url(), status: response.status() })
    }
  })
  return evidence
}

async function installBusinessWriteGuard(context, evidence) {
  await context.route('**/admin-api/**', async (route) => {
    const request = route.request()
    const pathname = new URL(request.url()).pathname
    const isAuthenticationLogin = pathname.endsWith('/system/auth/login') && request.method() === 'POST'
    const isReadOnly = ['GET', 'HEAD', 'OPTIONS'].includes(request.method())
    if (!isAuthenticationLogin && !isReadOnly) {
      evidence.blockedBusinessWrites.push({ method: request.method(), pathname })
      await route.abort('blockedbyclient')
      return
    }
    await route.continue()
  })
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`missing visible login control: ${label}`)
}

async function selectTenant(page, form, tenantName) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenantName)
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: tenantName })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), tenantName, 'tenant')
}

async function loginReadOnly(page, environment, actor, expectedUserId) {
  const loginUrl = new URL('/login', environment.frontendUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('login captcha is enabled')
  }
  await selectTenant(page, form, environment.tenantName)
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    actor.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), actor.password, 'password')
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  if (!loginResponse.ok() || ![0, 200].includes(loginBody.code)) {
    throw new Error(`login rejected for ${actor.role}: HTTP ${loginResponse.status()} code ${loginBody.code}`)
  }
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
  const permission = await page.evaluate(async () => {
    const response = await fetch('/admin-api/system/auth/get-permission-info', { method: 'GET' })
    return { status: response.status, body: await response.json() }
  })
  if (permission.status >= 400 || ![0, 200].includes(permission.body.code)) {
    throw new Error(`permission info rejected for ${actor.role}: HTTP ${permission.status}`)
  }
  const actualUserId = Number(permission.body?.data?.user?.id)
  if (actualUserId !== expectedUserId) {
    throw new Error(`login identity mismatch for ${actor.role}`)
  }
  return actualUserId
}

async function runReadOnlyBrowserLogins(environment, databaseEvidence) {
  let browser
  const evidence = []
  try {
    const { chromium } = require('playwright')
    browser = await chromium.launch({ headless: true, executablePath: environment.browserPath })
    for (const actor of environment.actors) {
      const account = databaseEvidence.accounts.find((item) => item.role === actor.role)
      const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
      const page = await context.newPage()
      const actorEvidence = installReadOnlyObservers(page, actor.role)
      await installBusinessWriteGuard(context, actorEvidence)
      try {
        const userId = await loginReadOnly(page, environment, actor, account.userId)
        if (
          actorEvidence.blockedBusinessWrites.length > 0 ||
          actorEvidence.consoleErrors.length > 0 ||
          actorEvidence.pageErrors.length > 0 ||
          actorEvidence.requestFailures.length > 0 ||
          actorEvidence.errorResponses.length > 0
        ) {
          throw new Error(`read-only login observers captured an error for ${actor.role}`)
        }
        evidence.push({ role: actor.role, userId, readOnlyLogin: 'PASS' })
      } finally {
        await context.close()
      }
    }
    await browser.close()
    browser = undefined
    return evidence
  } catch (error) {
    if (browser) {
      try {
        await browser.close()
      } catch {
        // The original browser/login prerequisite failure remains authoritative.
      }
    }
    if (error instanceof PreflightBlockedError) throw error
    throw new PreflightBlockedError(
      'FIVE_ROLE_LOGIN',
      'ROLE_LOGIN_PREREQUISITE_FAILED',
      'five explicit role accounts could not complete the read-only UI login preflight',
      { reason: error instanceof Error ? error.message : String(error) }
    )
  }
}

function writeResult(resultPath, result, secretValues) {
  assertNoSecretLeak(result, secretValues)
  const serialized = `${JSON.stringify(result, null, 2)}\n`
  process.stdout.write(serialized)
  if (!resultPath) return
  const absolutePath = path.resolve(resultPath)
  if (!fs.existsSync(path.dirname(absolutePath))) {
    throw new Error(`result directory does not exist: ${path.dirname(absolutePath)}`)
  }
  fs.writeFileSync(absolutePath, serialized, 'utf8')
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  const resultPath = args.get('result-path') || ''
  const secretValues = collectSecretValues(process.env)
  try {
    const environment = validateExplicitEnvironment(process.env)
    const runtimeEvidence = await runReadOnlyRuntimeChecks(environment)
    const databaseEvidence = runReadOnlyDatabaseChecks(environment)
    const browserEvidence = await runReadOnlyBrowserLogins(environment, databaseEvidence)
    const result = {
      schemaVersion: SCHEMA_VERSION,
      status: 'PASS',
      canRunRealE2E: true,
      generatedAt: new Date().toISOString(),
      authorizedTenant: { id: environment.tenantId, name: environment.tenantName },
      runtimeEvidence,
      databaseEvidence,
      browserEvidence,
      blockers: [],
      sideEffects: { ...SIDE_EFFECTS_NONE }
    }
    writeResult(resultPath, result, secretValues)
  } catch (error) {
    const blocker = sanitizeForOutput(error, secretValues)
    const blocked = error instanceof PreflightBlockedError
    const result = {
      schemaVersion: SCHEMA_VERSION,
      status: blocked ? 'BLOCKED' : 'ERROR',
      canRunRealE2E: false,
      generatedAt: new Date().toISOString(),
      blockers: [blocker],
      sideEffects: { ...SIDE_EFFECTS_NONE }
    }
    writeResult(resultPath, result, secretValues)
    process.exitCode = blocked ? 2 : 1
  }
}

main().catch((error) => {
  const secretValues = collectSecretValues(process.env)
  const result = {
    schemaVersion: SCHEMA_VERSION,
    status: 'ERROR',
    canRunRealE2E: false,
    generatedAt: new Date().toISOString(),
    blockers: [sanitizeForOutput(error, secretValues)],
    sideEffects: { ...SIDE_EFFECTS_NONE }
  }
  writeResult('', result, secretValues)
  process.exitCode = 1
})
