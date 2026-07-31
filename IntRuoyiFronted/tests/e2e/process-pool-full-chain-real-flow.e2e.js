const assert = require('node:assert/strict')
const fs = require('node:fs')
const http = require('node:http')
const https = require('node:https')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const BASE_URL = (
  process.env.PROCESS_POOL_FULL_CHAIN_E2E_BASE_URL || 'http://127.0.0.1:8082'
).replace(/\/+$/, '')
const BACKEND_URL = (
  process.env.PROCESS_POOL_FULL_CHAIN_E2E_BACKEND_URL || 'http://127.0.0.1:48082'
).replace(/\/+$/, '')
const TENANT_NAME = '测试租户'
const TENANT_ID = 122
const LOGIN_USERNAME = 'codexedhrcell01'
const LOGIN_USER_ID = 914523
const LOGIN_USER_NICKNAME = 'Codex单元格链接E2E'
const LOGIN_USER_DEPT_ID = 910656
const ITEM_ID = 922276
const ITEM_CODE = 'YXN.037.011.1007'
const UNIT_ID = 900312
const MYSQL_CONTAINER = 'int-ruoyi-mysql'
const DATABASE_NAME = 'ruoyi-vue-pro'
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const OUTPUT_ROOT = path.resolve(
  FRONTEND_ROOT,
  'output',
  'playwright',
  'process-pool-full-chain-real-flow'
)

const PRODUCTION_PATH = '/mes/pro/feedback/edhr-batch-production-fill'
const PQC_PATH = '/mes/pro/feedback/edhr-batch-pqc-fill'
const FIFO_PATH = '/mes/pro/process-pool/fifo-orchestration'
const REVIEW_PATH = '/mes/pro/process-pool/review-copy'
const WORKBENCH_PATH = '/mes/pro/process-pool/team-leader-workbench'
const FRONTLINE_SUBMIT_ENDPOINT = '/admin-api/mes/pro/feedback/frontline/submit'
const FIFO_ALLOCATE_ENDPOINT =
  '/admin-api/mes/pro/process-pool/fifo-orchestration/allocate-available-output'
const REVIEW_FROM_RULES_ENDPOINT =
  '/admin-api/mes/pro/process-pool/review-copy/generate-submit-from-rules'
const WORKBENCH_PAGE_ENDPOINT =
  '/admin-api/mes/pro/process-pool/team-leader-workbench/page'
const WORKBENCH_DETAIL_ENDPOINT =
  '/admin-api/mes/pro/process-pool/team-leader-workbench/detail'

const productionValues = {
  previousProcessInputQuantity: 60,
  outputQuantity: 50,
  lossQuantity: 10
}
const pqcValues = {
  inspectionQuantity: 50,
  lossQuantity: 0,
  result: 'SUCCESS'
}
const fifoValues = {
  earlyDemandQuantity: 20,
  lateDemandQuantity: 30
}
const reviewRule = {
  fieldCode: 'OUTPUT_QUANTITY',
  lowerLimit: 20,
  upperLimit: 40,
  rawValue: 50,
  correctedValue: 40,
  affectsAllocation: false
}

const RECORDBOOK_SCHEMA_KEYS = [
  'mode',
  'templateType',
  'PREVIOUS_PROCESS_INPUT_QUANTITY',
  'DEVICE',
  'DEVICE_PARAMETERS',
  'OUTPUT_QUANTITY',
  'SCRAP_QUANTITY',
  'PQC_RESULT',
  'fieldValues',
  'previousProcessInputQuantity',
  'outputQuantity',
  'scrapQuantity',
  'pqc',
  'equipmentParameters',
  'process',
  'employee',
  'rawPayload'
]

function parseLocalRuntimeUrl(value, label) {
  let url
  try {
    url = new URL(value)
  } catch {
    throw new Error(`${label} is not a valid URL: ${value}`)
  }
  assert.equal(url.protocol, 'http:', `${label} must use local HTTP`)
  assert.ok(
    ['127.0.0.1', 'localhost'].includes(url.hostname),
    `${label} must use localhost or 127.0.0.1`
  )
  assert.ok(!url.username && !url.password, `${label} must not contain credentials`)
  assert.ok(url.pathname === '/' || url.pathname === '', `${label} must not contain a path`)
  assert.ok(!url.search && !url.hash, `${label} must not contain query or hash data`)
  const port = Number(url.port)
  assert.ok(Number.isInteger(port) && port > 0, `${label} must include an explicit port`)
  return { url: `http://${url.hostname}:${port}`, port }
}

function assertAllowedRuntimePair() {
  const frontend = parseLocalRuntimeUrl(BASE_URL, 'frontend runtime')
  const backend = parseLocalRuntimeUrl(BACKEND_URL, 'backend runtime')
  assert.equal(frontend.port, 8082, `T6 frontend must use port 8082, actual ${frontend.port}`)
  assert.equal(backend.port, 48082, `T6 backend must use port 48082, actual ${backend.port}`)
  assert.equal(
    backend.port,
    frontend.port + 40000,
    `frontend/backend must use one runtime slot: ${frontend.port}/${backend.port}`
  )
  return { baseUrl: frontend.url, backendUrl: backend.url }
}

function readFrontendEnvValue(name) {
  const envPath = path.join(FRONTEND_ROOT, '.env')
  assert.ok(fs.existsSync(envPath), `missing frontend env file: ${envPath}`)
  const source = fs.readFileSync(envPath, 'utf8')
  let match
  if (name === 'VITE_APP_DEFAULT_LOGIN_PASSWORD') {
    match = source.match(/^\s*VITE_APP_DEFAULT_LOGIN_PASSWORD\s*=\s*(.*?)\s*$/m)
  } else {
    const escapedName = String(name).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    match = source.match(new RegExp(`^\\s*${escapedName}\\s*=\\s*(.*?)\\s*$`, 'm'))
  }
  assert.ok(match, `missing required frontend env value: ${name}`)
  let value = match[1].trim()
  if (
    value.length >= 2 &&
    ((value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'")))
  ) {
    value = value.slice(1, -1)
  }
  assert.ok(value, `frontend env value is empty: ${name}`)
  return value
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch {
    throw new Error('Playwright runtime is missing from IntRuoyiFronted/node_modules')
  }
}

function requestText(url, timeoutMs = 15000) {
  return new Promise((resolve, reject) => {
    const client = url.startsWith('https:') ? https : http
    const request = client.get(url, (response) => {
      const chunks = []
      response.on('data', (chunk) => chunks.push(chunk))
      response.on('end', () => {
        resolve({
          status: response.statusCode || 0,
          body: Buffer.concat(chunks).toString('utf8')
        })
      })
    })
    request.setTimeout(timeoutMs, () => {
      request.destroy(new Error(`timeout requesting ${url}`))
    })
    request.on('error', reject)
  })
}

async function assertRuntimeReady(runtime) {
  const frontend = await requestText(`${runtime.baseUrl}/`)
  assert.equal(frontend.status, 200, `frontend must return HTTP 200, actual ${frontend.status}`)
  const health = await requestText(`${runtime.backendUrl}/actuator/health`)
  assert.equal(health.status, 200, `backend health must return HTTP 200, actual ${health.status}`)
  const body = JSON.parse(health.body)
  assert.equal(body.status, 'UP', `backend health must be UP: ${health.body}`)
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome executable is missing: ${BROWSER_EXECUTABLE}`)
}

function sqlString(value) {
  return `'${String(value ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/'/g, "''")}'`
}

function sqlNullable(value) {
  return value === null || value === undefined ? 'NULL' : sqlString(value)
}

function sqlNumber(value, label) {
  const parsed = Number(value)
  assert.ok(Number.isFinite(parsed), `${label} must be numeric`)
  return String(parsed)
}

function mysql(sql, label) {
  const result = spawnSync(
    'docker',
    [
      'exec',
      '-i',
      MYSQL_CONTAINER,
      'sh',
      '-lc',
      `MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -D${DATABASE_NAME} -N -B --raw --default-character-set=utf8mb4`
    ],
    {
      input: sql,
      encoding: 'utf8',
      maxBuffer: 20 * 1024 * 1024
    }
  )
  if (result.error) {
    throw new Error(`${label} failed to start: ${result.error.message}`)
  }
  if (result.status !== 0) {
    throw new Error(
      `${label} failed with docker/mysql exit ${result.status}: ${(result.stderr || '').trim()}`
    )
  }
  return (result.stdout || '').trim()
}

function parseJsonOutput(output, label) {
  const line = output
    .split(/\r?\n/)
    .map((item) => item.trim())
    .find(Boolean)
  assert.ok(line, `${label} returned empty output`)
  try {
    return JSON.parse(line)
  } catch {
    throw new Error(`${label} returned invalid JSON: ${line}`)
  }
}

function readSignatureAuthorization() {
  const output = mysql(
    `
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'id', id,
  'userId', user_id,
  'electronicSignatureEnabled', electronic_signature_enabled,
  'authorizationState', authorization_state,
  'lockedUntil', DATE_FORMAT(locked_until, '%Y-%m-%d %H:%i:%s'),
  'lockReason', lock_reason,
  'lastFailureAt', DATE_FORMAT(last_failure_at, '%Y-%m-%d %H:%i:%s'),
  'failureCount', failure_count,
  'tenantId', tenant_id,
  'createTime', DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s'),
  'updateTime', DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s'),
  'creator', creator,
  'updater', updater,
  'deleted', deleted + 0
)
FROM dcc_electronic_signature_authorization
WHERE user_id = ${LOGIN_USER_ID}
LIMIT 1;
`,
    'read signature authorization'
  )
  return output ? parseJsonOutput(output, 'signature authorization') : null
}

function enableSignatureAuthorization(runMarker) {
  const original = readSignatureAuthorization()
  if (original) {
    const output = mysql(
      `
UPDATE dcc_electronic_signature_authorization
SET electronic_signature_enabled = 1,
    authorization_state = 'ENABLED',
    locked_until = NULL,
    lock_reason = NULL,
    last_failure_at = NULL,
    failure_count = 0,
    tenant_id = ${TENANT_ID},
    updater = ${sqlString(runMarker)},
    update_time = NOW(),
    deleted = 0
WHERE id = ${sqlNumber(original.id, 'signature authorization id')}
  AND user_id = ${LOGIN_USER_ID};
SELECT ROW_COUNT();
`,
      'enable existing signature authorization'
    )
    assert.equal(Number(output.split(/\r?\n/).at(-1)), 1, 'signature authorization update must affect 1 row')
    return { created: false, original }
  }

  const output = mysql(
    `
INSERT INTO dcc_electronic_signature_authorization (
  user_id,
  electronic_signature_enabled,
  authorization_state,
  locked_until,
  lock_reason,
  last_failure_at,
  failure_count,
  tenant_id,
  create_time,
  update_time,
  creator,
  updater,
  deleted
) VALUES (
  ${LOGIN_USER_ID},
  1,
  'ENABLED',
  NULL,
  NULL,
  NULL,
  0,
  ${TENANT_ID},
  NOW(),
  NOW(),
  ${sqlString(runMarker)},
  ${sqlString(runMarker)},
  0
);
SELECT LAST_INSERT_ID();
`,
    'create signature authorization'
  )
  const insertedId = Number(output.split(/\r?\n/).at(-1))
  assert.ok(insertedId > 0, 'created signature authorization must return an id')
  return { created: true, insertedId, original: null }
}

function restoreSignatureAuthorization(snapshot) {
  if (!snapshot) {
    return { status: 'NOT_PREPARED' }
  }
  if (snapshot.created) {
    const output = mysql(
      `
DELETE FROM dcc_electronic_signature_authorization
WHERE id = ${sqlNumber(snapshot.insertedId, 'inserted signature authorization id')}
  AND user_id = ${LOGIN_USER_ID};
SELECT ROW_COUNT();
`,
      'remove task-created signature authorization'
    )
    assert.equal(Number(output.split(/\r?\n/).at(-1)), 1, 'signature authorization cleanup must affect 1 row')
    assert.equal(readSignatureAuthorization(), null, 'task-created signature authorization must be removed')
    return { status: 'REMOVED_TASK_ROW', id: snapshot.insertedId }
  }

  const original = snapshot.original
  assert.ok(original?.id, 'original signature authorization snapshot is required')
  mysql(
    `
UPDATE dcc_electronic_signature_authorization
SET user_id = ${sqlNumber(original.userId, 'signature user id')},
    electronic_signature_enabled = ${sqlNumber(
      original.electronicSignatureEnabled,
      'signature enabled'
    )},
    authorization_state = ${sqlString(original.authorizationState)},
    locked_until = ${sqlNullable(original.lockedUntil)},
    lock_reason = ${sqlNullable(original.lockReason)},
    last_failure_at = ${sqlNullable(original.lastFailureAt)},
    failure_count = ${sqlNumber(original.failureCount, 'signature failure count')},
    tenant_id = ${sqlNumber(original.tenantId, 'signature tenant id')},
    create_time = ${sqlNullable(original.createTime)},
    update_time = ${sqlNullable(original.updateTime)},
    creator = ${sqlNullable(original.creator)},
    updater = ${sqlNullable(original.updater)},
    deleted = ${sqlNumber(original.deleted, 'signature deleted flag')}
WHERE id = ${sqlNumber(original.id, 'signature authorization id')};
SELECT ROW_COUNT();
`,
    'restore signature authorization'
  )
  const restored = readSignatureAuthorization()
  assert.deepEqual(restored, original, 'signature authorization must be restored exactly')
  return { status: 'RESTORED_ORIGINAL_ROW', id: original.id }
}

function buildRecordbookSchemaJson() {
  return JSON.stringify(
    RECORDBOOK_SCHEMA_KEYS.map((key) => ({
      key,
      label: key,
      type: 'text',
      required: false
    }))
  )
}

function assertNoStaleFixtures(runMarker) {
  const output = mysql(
    `
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'routes', (SELECT COUNT(*) FROM mes_pro_route
    WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND code LIKE ${sqlString(`${runMarker}%`)}),
  'workOrders', (SELECT COUNT(*) FROM mes_pro_work_order
    WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND code LIKE ${sqlString(`${runMarker}%`)}),
  'recordbooks', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook
    WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
      AND recordbook_code LIKE ${sqlString(`${runMarker}%`)}),
  'posts', (SELECT COUNT(*) FROM system_post
    WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
      AND code = ${sqlString(`${runMarker}-POST`)})
);
`,
    'check stale fixture marker'
  )
  const stale = parseJsonOutput(output, 'stale fixture marker')
  assert.deepEqual(
    {
      routes: Number(stale.routes),
      workOrders: Number(stale.workOrders),
      recordbooks: Number(stale.recordbooks),
      posts: Number(stale.posts)
    },
    { routes: 0, workOrders: 0, recordbooks: 0, posts: 0 },
    `stale fixture rows exist for ${runMarker}`
  )
}

function assertFixtureCounts(actual) {
  const expected = {
    routeCount: 2,
    processCount: 2,
    routeProcessCount: 2,
    workstationCount: 2,
    machineryCount: 2,
    workstationWorkerCount: 2,
    workstationMachineCount: 2,
    machineryProcessCount: 2,
    deviceRouteBindingCount: 2,
    templateBindingCount: 2,
    postCount: 1,
    userPostCount: 1,
    recordbookTemplateCount: 1,
    recordbookCount: 1,
    tagCount: 2,
    workOrderCount: 4,
    taskCount: 2,
    reviewRuleCount: 1,
    reviewSignatureCount: 1
  }
  const normalized = Object.fromEntries(
    Object.keys(expected).map((key) => [key, Number(actual?.[key])])
  )
  assert.deepEqual(normalized, expected, 'fixture rows must match the exact T6 count contract')
}

function prepareFixtures(state) {
  const runMarker = state.runMarker
  const markerSql = sqlString(runMarker)
  const markerPrefixSql = sqlString(`${runMarker}%`)
  assertNoStaleFixtures(runMarker)

  state.signatureAuthorization = enableSignatureAuthorization(runMarker)

  const entrySchemaJson = buildRecordbookSchemaJson()
  const tagPolicyJson = JSON.stringify({
    required: false,
    allowedTagCodes: ['FRONTLINE_PRODUCTION', 'FRONTLINE_PQC']
  })
  const reviewMetadataJson = JSON.stringify({
    runMarker,
    fieldCode: reviewRule.fieldCode,
    source: 'T6_FULL_CHAIN_E2E'
  })

  const output = mysql(
    `
SET NAMES utf8mb4;
START TRANSACTION;
SET @run_key := ${sqlString(runMarker)};
SET @creator := 'codex-e2e';

INSERT INTO system_post (
  code, name, sort, status, remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-POST'), CONCAT(@run_key, ' Operator Post'),
  1, 0, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @operator_post_id := LAST_INSERT_ID();

INSERT INTO mes_pro_process (
  product_name, code, name, status, remark, creator, updater, deleted, tenant_id
) VALUES (
  ${sqlString(ITEM_CODE)}, CONCAT(@run_key, '-PROC-PROD'),
  CONCAT(@run_key, ' Production Process'), 0, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @production_process_id := LAST_INSERT_ID();

INSERT INTO mes_pro_process (
  product_name, code, name, status, remark, creator, updater, deleted, tenant_id
) VALUES (
  ${sqlString(ITEM_CODE)}, CONCAT(@run_key, '-PROC-PQC'),
  CONCAT(@run_key, ' PQC Process'), 0, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @pqc_process_id := LAST_INSERT_ID();

INSERT INTO mes_pro_route (
  code, name, description, status, remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-ROUTE-PROD'), CONCAT(@run_key, ' Production Route'),
  'T6 production route', 0, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @production_route_id := LAST_INSERT_ID();

INSERT INTO mes_pro_route (
  code, name, description, status, remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-ROUTE-PQC'), CONCAT(@run_key, ' PQC Route'),
  'T6 PQC route', 0, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @pqc_route_id := LAST_INSERT_ID();

INSERT INTO mes_md_workstation (
  code, name, process_id, status, remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-WS-PROD'), CONCAT(@run_key, ' Production Workstation'),
  @production_process_id, 0, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @production_workstation_id := LAST_INSERT_ID();

INSERT INTO mes_md_workstation (
  code, name, process_id, status, remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-WS-PQC'), CONCAT(@run_key, ' PQC Workstation'),
  @pqc_process_id, 0, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @pqc_workstation_id := LAST_INSERT_ID();

INSERT INTO mes_dv_machinery (
  code, name, process_name, standard_hourly_capacity, status, remark,
  creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-DEV-PROD'), CONCAT(@run_key, ' Production Device'),
  CONCAT(@run_key, ' Production Process'), 100, 0, @run_key,
  @creator, @creator, b'0', ${TENANT_ID}
);
SET @production_device_id := LAST_INSERT_ID();

INSERT INTO mes_dv_machinery (
  code, name, process_name, standard_hourly_capacity, status, remark,
  creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-DEV-PQC'), CONCAT(@run_key, ' PQC Device'),
  CONCAT(@run_key, ' PQC Process'), 100, 0, @run_key,
  @creator, @creator, b'0', ${TENANT_ID}
);
SET @pqc_device_id := LAST_INSERT_ID();

INSERT INTO mes_pro_route_process (
  route_id, process_id, workstation_id, sort, next_process_id, link_type,
  prepare_time, wait_time, color_code, key_flag, check_flag,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  @production_route_id, @production_process_id, @production_workstation_id, 1, NULL, 1,
  0, 0, '#2563eb', b'0', b'1', @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @production_route_process_id := LAST_INSERT_ID();

INSERT INTO mes_pro_route_process (
  route_id, process_id, workstation_id, sort, next_process_id, link_type,
  prepare_time, wait_time, color_code, key_flag, check_flag,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  @pqc_route_id, @pqc_process_id, @pqc_workstation_id, 1, NULL, 1,
  0, 0, '#16a34a', b'0', b'1', @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @pqc_route_process_id := LAST_INSERT_ID();

INSERT INTO mes_md_workstation_worker (
  workstation_id, post_id, quantity, remark, creator, updater, deleted, tenant_id
) VALUES
  (@production_workstation_id, @operator_post_id, 1, @run_key, @creator, @creator, b'0', ${TENANT_ID}),
  (@pqc_workstation_id, @operator_post_id, 1, @run_key, @creator, @creator, b'0', ${TENANT_ID});

INSERT INTO system_user_post (
  user_id, post_id, creator, updater, deleted, tenant_id
) VALUES (
  ${LOGIN_USER_ID}, @operator_post_id, @creator, @creator, b'0', ${TENANT_ID}
);
SET @system_user_post_id := LAST_INSERT_ID();

INSERT INTO mes_md_workstation_machine (
  workstation_id, machinery_id, quantity, remark, creator, updater, deleted, tenant_id
) VALUES
  (@production_workstation_id, @production_device_id, 1, @run_key, @creator, @creator, b'0', ${TENANT_ID}),
  (@pqc_workstation_id, @pqc_device_id, 1, @run_key, @creator, @creator, b'0', ${TENANT_ID});

INSERT INTO mes_dv_machinery_process (
  machinery_id, process_id, process_code, machinery_code, line_name, process_name,
  device_name, device_quantity, ten_half_hour_daily_capacity, standard_hourly_capacity,
  remark, creator, updater, deleted, tenant_id
) VALUES
  (
    @production_device_id, @production_process_id, CONCAT(@run_key, '-PROC-PROD'),
    CONCAT(@run_key, '-DEV-PROD'), 'T6', CONCAT(@run_key, ' Production Process'),
    CONCAT(@run_key, ' Production Device'), 1, 1000, 100,
    @run_key, @creator, @creator, b'0', ${TENANT_ID}
  ),
  (
    @pqc_device_id, @pqc_process_id, CONCAT(@run_key, '-PROC-PQC'),
    CONCAT(@run_key, '-DEV-PQC'), 'T6', CONCAT(@run_key, ' PQC Process'),
    CONCAT(@run_key, ' PQC Device'), 1, 1000, 100,
    @run_key, @creator, @creator, b'0', ${TENANT_ID}
  );

INSERT INTO mes_pro_edhr_recordbook_template (
  template_code, template_name, template_version, recordbook_type,
  entry_schema_json, tag_policy_json, status, active_by, active_at,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-RB-TPL'), CONCAT(@run_key, ' Frontline Recordbook Template'),
  '1.0', 'FRONTLINE', ${sqlString(entrySchemaJson)}, ${sqlString(tagPolicyJson)},
  'ACTIVE', ${LOGIN_USER_ID}, NOW(), @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @recordbook_template_id := LAST_INSERT_ID();

INSERT INTO mes_pro_edhr_controlled_tag (
  tag_code, tag_name, tag_type, tag_status, active_by, active_at,
  remark, creator, updater, deleted, tenant_id
) VALUES
  (
    'FRONTLINE_PRODUCTION', 'Frontline Production', 'FRONTLINE', 'ACTIVE',
    ${LOGIN_USER_ID}, NOW(), @run_key, @creator, @creator, b'0', ${TENANT_ID}
  ),
  (
    'FRONTLINE_PQC', 'Frontline PQC', 'FRONTLINE', 'ACTIVE',
    ${LOGIN_USER_ID}, NOW(), @run_key, @creator, @creator, b'0', ${TENANT_ID}
  );

INSERT INTO mes_pro_edhr_recordbook (
  recordbook_code, recordbook_name, template_id, template_code, template_name,
  template_version, recordbook_type, status, owner_user_id, owner_dept_id,
  business_scope, business_object_type, business_object_id, business_object_code,
  opened_at, entry_count, remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-RB'), CONCAT(@run_key, ' Frontline Recordbook'),
  @recordbook_template_id, CONCAT(@run_key, '-RB-TPL'),
  CONCAT(@run_key, ' Frontline Recordbook Template'), '1.0', 'FRONTLINE', 'OPEN',
  ${LOGIN_USER_ID}, ${LOGIN_USER_DEPT_ID}, 'T6_FULL_CHAIN', 'PRODUCTION_WORK_ORDER',
  NULL, @run_key, NOW(), 0, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @recordbook_id := LAST_INSERT_ID();

INSERT INTO mes_frontline_device_account_route_binding (
  device_account_user_id, route_id, device_id, workstation_id,
  default_approve_user_id, recordbook_id, feedback_type, status,
  remark, creator, updater, deleted, tenant_id
) VALUES
  (
    ${LOGIN_USER_ID}, @production_route_id, @production_device_id, @production_workstation_id,
    ${LOGIN_USER_ID}, @recordbook_id, 1, 0,
    @run_key, @creator, @creator, b'0', ${TENANT_ID}
  ),
  (
    ${LOGIN_USER_ID}, @pqc_route_id, @pqc_device_id, @pqc_workstation_id,
    ${LOGIN_USER_ID}, @recordbook_id, 1, 0,
    @run_key, @creator, @creator, b'0', ${TENANT_ID}
  );

INSERT INTO mes_frontline_employee_template_binding (
  actual_employee_id, route_process_id, process_id, template_no, template_type,
  status, remark, creator, updater, deleted, tenant_id
) VALUES
  (
    ${LOGIN_USER_ID}, @production_route_process_id, @production_process_id,
    'PRODUCTION_SIMPLIFIED', 'PRODUCTION_SIMPLIFIED', 0,
    @run_key, @creator, @creator, b'0', ${TENANT_ID}
  ),
  (
    ${LOGIN_USER_ID}, @pqc_route_process_id, @pqc_process_id,
    'PQC_SIMPLIFIED', 'PQC_SIMPLIFIED', 0,
    @run_key, @creator, @creator, b'0', ${TENANT_ID}
  );

INSERT INTO mes_pro_work_order (
  code, name, type, order_source_type, order_source_code, product_id,
  quantity, quantity_produced, quantity_changed, quantity_scheduled,
  batch_code, business_status, schedule_status, planned_start_time, planned_end_time,
  request_date, parent_id, status, temporary_frozen, remark,
  creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-WO-PROD'), CONCAT(@run_key, ' Production Work Order'),
  1, 1, @run_key, ${ITEM_ID},
  60, 0, 0, 60, CONCAT(@run_key, '-BATCH-PROD'), 'CONFIRMED', 'CONFIRMED',
  '2026-07-30 10:00:00', '2026-07-30 18:00:00', '2026-07-30 10:00:00',
  0, 1, b'0', @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @production_work_order_id := LAST_INSERT_ID();

INSERT INTO mes_pro_work_order (
  code, name, type, order_source_type, order_source_code, product_id,
  quantity, quantity_produced, quantity_changed, quantity_scheduled,
  batch_code, business_status, schedule_status, planned_start_time, planned_end_time,
  request_date, parent_id, status, temporary_frozen, remark,
  creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-WO-PQC'), CONCAT(@run_key, ' PQC Work Order'),
  1, 1, @run_key, ${ITEM_ID},
  50, 0, 0, 50, CONCAT(@run_key, '-BATCH-PQC'), 'CONFIRMED', 'CONFIRMED',
  '2026-07-30 11:00:00', '2026-07-30 19:00:00', '2026-07-30 11:00:00',
  0, 1, b'0', @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @pqc_work_order_id := LAST_INSERT_ID();

INSERT INTO mes_pro_work_order (
  code, name, type, order_source_type, order_source_code, product_id,
  quantity, quantity_produced, quantity_changed, quantity_scheduled,
  batch_code, business_status, schedule_status, planned_start_time, planned_end_time,
  request_date, parent_id, status, temporary_frozen, remark,
  creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-WO-EARLY'), CONCAT(@run_key, ' FIFO Early Work Order'),
  1, 1, @run_key, ${ITEM_ID},
  ${fifoValues.earlyDemandQuantity}, 0, 0, ${fifoValues.earlyDemandQuantity},
  CONCAT(@run_key, '-BATCH-EARLY'), 'CONFIRMED', 'CONFIRMED',
  '2026-07-30 08:00:00', '2026-07-30 12:00:00', '2026-07-30 08:00:00',
  0, 1, b'0', @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @early_work_order_id := LAST_INSERT_ID();

INSERT INTO mes_pro_work_order (
  code, name, type, order_source_type, order_source_code, product_id,
  quantity, quantity_produced, quantity_changed, quantity_scheduled,
  batch_code, business_status, schedule_status, planned_start_time, planned_end_time,
  request_date, parent_id, status, temporary_frozen, remark,
  creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-WO-LATE'), CONCAT(@run_key, ' FIFO Late Work Order'),
  1, 1, @run_key, ${ITEM_ID},
  ${fifoValues.lateDemandQuantity}, 0, 0, ${fifoValues.lateDemandQuantity},
  CONCAT(@run_key, '-BATCH-LATE'), 'CONFIRMED', 'CONFIRMED',
  '2026-07-30 09:00:00', '2026-07-30 13:00:00', '2026-07-30 09:00:00',
  0, 1, b'0', @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @late_work_order_id := LAST_INSERT_ID();

INSERT INTO mes_pro_task (
  code, name, work_order_id, workstation_id, route_id, process_id, item_id,
  quantity, produced_quantity, qualify_quantity, unqualify_quantity, changed_quantity,
  start_time, duration, end_time, status, remark,
  creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-TASK-PROD'), CONCAT(@run_key, ' Production Task'),
  @production_work_order_id, @production_workstation_id, @production_route_id,
  @production_process_id, ${ITEM_ID},
  60, 0, 0, 0, 0, '2026-07-30 10:00:00', 480, '2026-07-30 18:00:00',
  1, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @production_task_id := LAST_INSERT_ID();

INSERT INTO mes_pro_task (
  code, name, work_order_id, workstation_id, route_id, process_id, item_id,
  quantity, produced_quantity, qualify_quantity, unqualify_quantity, changed_quantity,
  start_time, duration, end_time, status, remark,
  creator, updater, deleted, tenant_id
) VALUES (
  CONCAT(@run_key, '-TASK-PQC'), CONCAT(@run_key, ' PQC Task'),
  @pqc_work_order_id, @pqc_workstation_id, @pqc_route_id,
  @pqc_process_id, ${ITEM_ID},
  50, 0, 0, 0, 0, '2026-07-30 11:00:00', 480, '2026-07-30 19:00:00',
  1, @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @pqc_task_id := LAST_INSERT_ID();

INSERT INTO mes_pro_process_pool_review_copy_rule (
  process_id, device_id, template_type, field_code, field_name,
  lower_limit, upper_limit, value_type, affects_allocation,
  allocation_field, source_quantity_type, template_field_metadata_json,
  enabled, remark, creator, updater, deleted, tenant_id
) VALUES (
  @production_process_id, @production_device_id, 'PRODUCTION_SIMPLIFIED',
  ${sqlString(reviewRule.fieldCode)}, 'Output quantity',
  ${reviewRule.lowerLimit}, ${reviewRule.upperLimit}, 'NUMBER', b'0',
  NULL, NULL, ${sqlString(reviewMetadataJson)},
  b'1', @run_key, @creator, @creator, b'0', ${TENANT_ID}
);
SET @review_rule_id := LAST_INSERT_ID();

INSERT INTO mes_pro_batch_record_execution_signature (
  execution_id, actor_id, action_type, signature_mode, password_verified,
  comment, signed_at, signature_display_at, signature_time_mode,
  selected_time_zone, selected_time_policy_version, actor_username_snapshot,
  actor_nickname_snapshot, actor_dept_id_snapshot, signature_purpose,
  authorization_basis, authentication_method, snapshot_status, actor_name,
  creator, updater, deleted, tenant_id
) VALUES (
  0, ${LOGIN_USER_ID}, 'REVIEW', 'PASSWORD', b'1',
  CONCAT(@run_key, ' review signature'), NOW(), NOW(), 'SERVER_TIME',
  'Asia/Shanghai', 'T6-2026-07-30', ${sqlString(LOGIN_USERNAME)},
  ${sqlString(LOGIN_USER_NICKNAME)}, ${LOGIN_USER_DEPT_ID}, 'Process pool review copy',
  @run_key, 'PASSWORD', 'CAPTURED', ${sqlString(LOGIN_USER_NICKNAME)},
  @creator, @creator, b'0', ${TENANT_ID}
);
SET @review_signature_id := LAST_INSERT_ID();

COMMIT;

SELECT JSON_OBJECT(
  'productionProcessId', @production_process_id,
  'pqcProcessId', @pqc_process_id,
  'productionRouteId', @production_route_id,
  'pqcRouteId', @pqc_route_id,
  'productionRouteProcessId', @production_route_process_id,
  'pqcRouteProcessId', @pqc_route_process_id,
  'productionWorkstationId', @production_workstation_id,
  'pqcWorkstationId', @pqc_workstation_id,
  'productionDeviceId', @production_device_id,
  'pqcDeviceId', @pqc_device_id,
  'recordbookTemplateId', @recordbook_template_id,
  'recordbookId', @recordbook_id,
  'operatorPostId', @operator_post_id,
  'systemUserPostId', @system_user_post_id,
  'productionWorkOrderId', @production_work_order_id,
  'pqcWorkOrderId', @pqc_work_order_id,
  'earlyWorkOrderId', @early_work_order_id,
  'lateWorkOrderId', @late_work_order_id,
  'productionTaskId', @production_task_id,
  'pqcTaskId', @pqc_task_id,
  'reviewRuleId', @review_rule_id,
  'reviewSignatureId', @review_signature_id,
  'productionProcessName', CONCAT(@run_key, ' Production Process'),
  'pqcProcessName', CONCAT(@run_key, ' PQC Process'),
  'productionWorkOrderCode', CONCAT(@run_key, '-WO-PROD'),
  'pqcWorkOrderCode', CONCAT(@run_key, '-WO-PQC'),
  'earlyWorkOrderCode', CONCAT(@run_key, '-WO-EARLY'),
  'lateWorkOrderCode', CONCAT(@run_key, '-WO-LATE'),
  'productionTaskCode', CONCAT(@run_key, '-TASK-PROD'),
  'pqcTaskCode', CONCAT(@run_key, '-TASK-PQC'),
  'fixtureCounts', JSON_OBJECT(
    'routeCount', (SELECT COUNT(*) FROM mes_pro_route
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND code LIKE ${markerPrefixSql}),
    'processCount', (SELECT COUNT(*) FROM mes_pro_process
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND code LIKE ${markerPrefixSql}),
    'routeProcessCount', (SELECT COUNT(*) FROM mes_pro_route_process
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
        AND route_id IN (@production_route_id, @pqc_route_id)),
    'workstationCount', (SELECT COUNT(*) FROM mes_md_workstation
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND code LIKE ${markerPrefixSql}),
    'machineryCount', (SELECT COUNT(*) FROM mes_dv_machinery
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND code LIKE ${markerPrefixSql}),
    'workstationWorkerCount', (SELECT COUNT(*) FROM mes_md_workstation_worker
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
        AND workstation_id IN (@production_workstation_id, @pqc_workstation_id)),
    'workstationMachineCount', (SELECT COUNT(*) FROM mes_md_workstation_machine
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
        AND workstation_id IN (@production_workstation_id, @pqc_workstation_id)),
    'machineryProcessCount', (SELECT COUNT(*) FROM mes_dv_machinery_process
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
        AND machinery_id IN (@production_device_id, @pqc_device_id)),
    'deviceRouteBindingCount', (SELECT COUNT(*) FROM mes_frontline_device_account_route_binding
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
        AND route_id IN (@production_route_id, @pqc_route_id)),
    'templateBindingCount', (SELECT COUNT(*) FROM mes_frontline_employee_template_binding
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
        AND route_process_id IN (@production_route_process_id, @pqc_route_process_id)),
    'postCount', (SELECT COUNT(*) FROM system_post
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND id = @operator_post_id),
    'userPostCount', (SELECT COUNT(*) FROM system_user_post
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND id = @system_user_post_id),
    'recordbookTemplateCount', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_template
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND id = @recordbook_template_id),
    'recordbookCount', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND id = @recordbook_id),
    'tagCount', (SELECT COUNT(*) FROM mes_pro_edhr_controlled_tag
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND remark = ${markerSql}),
    'workOrderCount', (SELECT COUNT(*) FROM mes_pro_work_order
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND code LIKE ${markerPrefixSql}),
    'taskCount', (SELECT COUNT(*) FROM mes_pro_task
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0'
        AND work_order_id IN (@production_work_order_id, @pqc_work_order_id)),
    'reviewRuleCount', (SELECT COUNT(*) FROM mes_pro_process_pool_review_copy_rule
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND id = @review_rule_id),
    'reviewSignatureCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution_signature
      WHERE tenant_id = ${TENANT_ID} AND deleted = b'0' AND id = @review_signature_id)
  )
);
`,
    'prepare full-chain fixtures'
  )

  const fixture = parseJsonOutput(output, 'prepared fixtures')
  assertFixtureCounts(fixture.fixtureCounts)
  Object.assign(state, fixture)
  state.productionFeedbackCode = `${runMarker}-FB-PROD`
  state.pqcFeedbackCode = `${runMarker}-FB-PQC`
  state.allocationBatchNo = `FIFO-${runMarker}`
  state.fixtureStatus = 'PREPARED'
  return state
}

function cleanupFixtures(state) {
  if (!state?.runMarker) {
    return { status: 'NOT_PREPARED' }
  }
  const runMarker = state.runMarker
  const markerSql = sqlString(runMarker)
  const markerPrefixSql = sqlString(`${runMarker}%`)
  const markerContainsSql = sqlString(`%${runMarker}%`)
  const productionWorkOrderCodeSql = sqlString(`${runMarker}-WO-PROD`)
  const pqcWorkOrderCodeSql = sqlString(`${runMarker}-WO-PQC`)
  const productionFeedbackCodeSql = sqlString(`${runMarker}-FB-PROD`)
  const pqcFeedbackCodeSql = sqlString(`${runMarker}-FB-PQC`)
  const recordbookCodeSql = sqlString(`${runMarker}-RB`)
  const recordbookTemplateCodeSql = sqlString(`${runMarker}-RB-TPL`)
  const allocationBatchNoSql = sqlString(`FIFO-${runMarker}`)
  const operatorPostCodeSql = sqlString(`${runMarker}-POST`)
  const output = mysql(
    `
SET NAMES utf8mb4;
START TRANSACTION;
SET @production_work_order_id := (
  SELECT id FROM mes_pro_work_order
  WHERE tenant_id = ${TENANT_ID} AND code = ${productionWorkOrderCodeSql}
  ORDER BY id DESC LIMIT 1
);
SET @pqc_work_order_id := (
  SELECT id FROM mes_pro_work_order
  WHERE tenant_id = ${TENANT_ID} AND code = ${pqcWorkOrderCodeSql}
  ORDER BY id DESC LIMIT 1
);
SET @operator_post_id := (
  SELECT id FROM system_post
  WHERE tenant_id = ${TENANT_ID}
    AND code = ${operatorPostCodeSql}
    AND remark = ${markerSql}
  ORDER BY id DESC LIMIT 1
);

DELETE FROM mes_pro_process_pool_review_copy_field
WHERE tenant_id = ${TENANT_ID}
  AND review_copy_id IN (
    SELECT id FROM mes_pro_process_pool_review_copy
    WHERE tenant_id = ${TENANT_ID}
      AND event_id IN (
        SELECT id FROM mes_pro_process_pool_event
        WHERE tenant_id = ${TENANT_ID}
          AND work_order_id IN (@production_work_order_id, @pqc_work_order_id)
      )
  );

DELETE FROM mes_pro_process_pool_review_copy
WHERE tenant_id = ${TENANT_ID}
  AND event_id IN (
    SELECT id FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID}
      AND work_order_id IN (@production_work_order_id, @pqc_work_order_id)
  );

DELETE FROM mes_pro_process_pool_fifo_allocation_line
WHERE tenant_id = ${TENANT_ID}
  AND allocation_batch_no = ${allocationBatchNoSql};

DELETE FROM mes_pro_process_pool_pqc_record
WHERE tenant_id = ${TENANT_ID}
  AND event_id IN (
    SELECT id FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID}
      AND work_order_id IN (@production_work_order_id, @pqc_work_order_id)
  );

DELETE FROM mes_pro_process_pool_quantity_fragment
WHERE tenant_id = ${TENANT_ID}
  AND event_id IN (
    SELECT id FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID}
      AND work_order_id IN (@production_work_order_id, @pqc_work_order_id)
  );

DELETE FROM mes_pro_process_pool_event
WHERE tenant_id = ${TENANT_ID}
  AND work_order_id IN (@production_work_order_id, @pqc_work_order_id);

DELETE FROM mes_pro_process_pool
WHERE tenant_id = ${TENANT_ID}
  AND work_order_id IN (@production_work_order_id, @pqc_work_order_id);

DELETE FROM mes_pro_feedback
WHERE tenant_id = ${TENANT_ID}
  AND work_order_id IN (@production_work_order_id, @pqc_work_order_id);

DELETE FROM mes_pro_edhr_recordbook_tag_binding
WHERE tenant_id = ${TENANT_ID}
  AND recordbook_id IN (
    SELECT id FROM mes_pro_edhr_recordbook
    WHERE tenant_id = ${TENANT_ID}
      AND recordbook_code = ${recordbookCodeSql}
  );

DELETE FROM mes_pro_edhr_recordbook_event
WHERE tenant_id = ${TENANT_ID}
  AND (
    recordbook_id IN (
      SELECT id FROM mes_pro_edhr_recordbook
      WHERE tenant_id = ${TENANT_ID}
        AND recordbook_code = ${recordbookCodeSql}
    )
    OR entry_id IN (
      SELECT id FROM mes_pro_edhr_recordbook_entry
      WHERE tenant_id = ${TENANT_ID}
        AND recordbook_id IN (
          SELECT id FROM mes_pro_edhr_recordbook
          WHERE tenant_id = ${TENANT_ID}
            AND recordbook_code = ${recordbookCodeSql}
        )
    )
  );

DELETE FROM mes_pro_edhr_recordbook_entry
WHERE tenant_id = ${TENANT_ID}
  AND recordbook_id IN (
    SELECT id FROM mes_pro_edhr_recordbook
    WHERE tenant_id = ${TENANT_ID}
      AND recordbook_code = ${recordbookCodeSql}
  );

DELETE FROM mes_frontline_device_account_route_binding
WHERE tenant_id = ${TENANT_ID}
  AND remark = ${markerSql};

DELETE FROM mes_frontline_employee_template_binding
WHERE tenant_id = ${TENANT_ID}
  AND remark = ${markerSql};

DELETE FROM mes_pro_process_pool_review_copy_rule
WHERE tenant_id = ${TENANT_ID}
  AND remark = ${markerSql};

DELETE FROM mes_pro_task
WHERE tenant_id = ${TENANT_ID}
  AND work_order_id IN (
    SELECT id FROM mes_pro_work_order
    WHERE tenant_id = ${TENANT_ID}
      AND code LIKE ${markerPrefixSql}
  );

DELETE FROM mes_pro_work_order
WHERE tenant_id = ${TENANT_ID}
  AND code LIKE ${markerPrefixSql};

DELETE FROM mes_pro_route_process
WHERE tenant_id = ${TENANT_ID}
  AND route_id IN (
    SELECT id FROM mes_pro_route
    WHERE tenant_id = ${TENANT_ID}
      AND code LIKE ${markerPrefixSql}
  );

DELETE FROM mes_pro_route
WHERE tenant_id = ${TENANT_ID}
  AND code LIKE ${markerPrefixSql};

DELETE FROM mes_md_workstation_machine
WHERE tenant_id = ${TENANT_ID}
  AND workstation_id IN (
    SELECT id FROM mes_md_workstation
    WHERE tenant_id = ${TENANT_ID}
      AND code LIKE ${markerPrefixSql}
  );

DELETE FROM mes_md_workstation_worker
WHERE tenant_id = ${TENANT_ID}
  AND workstation_id IN (
    SELECT id FROM mes_md_workstation
    WHERE tenant_id = ${TENANT_ID}
      AND code LIKE ${markerPrefixSql}
  );

DELETE FROM mes_dv_machinery_process
WHERE tenant_id = ${TENANT_ID}
  AND machinery_id IN (
    SELECT id FROM mes_dv_machinery
    WHERE tenant_id = ${TENANT_ID}
      AND code LIKE ${markerPrefixSql}
  );

DELETE FROM mes_md_workstation
WHERE tenant_id = ${TENANT_ID}
  AND code LIKE ${markerPrefixSql};

DELETE FROM mes_dv_machinery
WHERE tenant_id = ${TENANT_ID}
  AND code LIKE ${markerPrefixSql};

DELETE FROM mes_pro_process
WHERE tenant_id = ${TENANT_ID}
  AND code LIKE ${markerPrefixSql};

DELETE FROM mes_pro_edhr_recordbook
WHERE tenant_id = ${TENANT_ID}
  AND recordbook_code = ${recordbookCodeSql};

DELETE FROM mes_pro_edhr_recordbook_template
WHERE tenant_id = ${TENANT_ID}
  AND template_code = ${recordbookTemplateCodeSql};

DELETE FROM mes_pro_edhr_controlled_tag
WHERE tenant_id = ${TENANT_ID}
  AND tag_code IN ('FRONTLINE_PRODUCTION', 'FRONTLINE_PQC')
  AND remark = ${markerSql};

DELETE FROM system_user_post
WHERE tenant_id = ${TENANT_ID}
  AND user_id = ${LOGIN_USER_ID}
  AND post_id = @operator_post_id;

DELETE FROM system_post
WHERE tenant_id = ${TENANT_ID}
  AND id = @operator_post_id
  AND code = ${operatorPostCodeSql}
  AND remark = ${markerSql};

DELETE FROM mes_pro_batch_record_execution_signature
WHERE tenant_id = ${TENANT_ID}
  AND actor_id = ${LOGIN_USER_ID}
  AND execution_id = 0
  AND comment LIKE ${markerContainsSql};

COMMIT;

SELECT JSON_OBJECT(
  'remainingRoutes', (SELECT COUNT(*) FROM mes_pro_route
    WHERE tenant_id = ${TENANT_ID} AND code LIKE ${markerPrefixSql}),
  'remainingProcesses', (SELECT COUNT(*) FROM mes_pro_process
    WHERE tenant_id = ${TENANT_ID} AND code LIKE ${markerPrefixSql}),
  'remainingWorkstations', (SELECT COUNT(*) FROM mes_md_workstation
    WHERE tenant_id = ${TENANT_ID} AND code LIKE ${markerPrefixSql}),
  'remainingMachinery', (SELECT COUNT(*) FROM mes_dv_machinery
    WHERE tenant_id = ${TENANT_ID} AND code LIKE ${markerPrefixSql}),
  'remainingWorkOrders', (SELECT COUNT(*) FROM mes_pro_work_order
    WHERE tenant_id = ${TENANT_ID} AND code LIKE ${markerPrefixSql}),
  'remainingTasks', (SELECT COUNT(*) FROM mes_pro_task
    WHERE tenant_id = ${TENANT_ID} AND remark = ${markerSql}),
  'remainingFeedback', (SELECT COUNT(*) FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID}
      AND code IN (${productionFeedbackCodeSql}, ${pqcFeedbackCodeSql})),
  'remainingRecordbooks', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook
    WHERE tenant_id = ${TENANT_ID} AND recordbook_code = ${recordbookCodeSql}),
  'remainingRecordbookEntries', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_entry
    WHERE tenant_id = ${TENANT_ID} AND entry_content_json LIKE ${markerContainsSql}),
  'remainingProcessPoolEvents', (SELECT COUNT(*) FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID} AND raw_payload LIKE ${markerContainsSql}),
  'remainingFifoLines', (SELECT COUNT(*) FROM mes_pro_process_pool_fifo_allocation_line
    WHERE tenant_id = ${TENANT_ID} AND allocation_batch_no = ${allocationBatchNoSql}),
  'remainingReviewRules', (SELECT COUNT(*) FROM mes_pro_process_pool_review_copy_rule
    WHERE tenant_id = ${TENANT_ID} AND remark = ${markerSql}),
  'remainingSignatures', (SELECT COUNT(*) FROM mes_pro_batch_record_execution_signature
    WHERE tenant_id = ${TENANT_ID} AND comment LIKE ${markerContainsSql}),
  'remainingUserPost', (SELECT COUNT(*) FROM system_user_post
    WHERE tenant_id = ${TENANT_ID}
      AND user_id = ${LOGIN_USER_ID}
      AND post_id = @operator_post_id),
  'remainingPost', (SELECT COUNT(*) FROM system_post
    WHERE tenant_id = ${TENANT_ID} AND code = ${operatorPostCodeSql})
);
`,
    'cleanup full-chain fixtures'
  )
  const cleanup = parseJsonOutput(output, 'fixture cleanup')
  for (const [key, value] of Object.entries(cleanup)) {
    assert.equal(Number(value), 0, `${key} must be zero after cleanup`)
  }
  return { status: 'CLEAN', ...cleanup }
}

function isCommonSuccess(body) {
  return body && (Number(body.code) === 0 || Number(body.code) === 200)
}

function unwrapCommonResult(body, label) {
  assert.ok(body && typeof body === 'object', `${label} must return JSON`)
  assert.ok(isCommonSuccess(body), `${label} failed: ${body.msg || body.code}`)
  return body.data
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const target = locator.nth(index)
    if ((await target.isVisible().catch(() => false)) && !(await target.isDisabled().catch(() => true))) {
      await target.fill(String(value))
      return target
    }
  }
  throw new Error(`missing visible input: ${label}`)
}

async function login(page, password) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', PRODUCTION_PATH)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
  const loginForm = page.locator('form.login-form:visible, .login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 90000 })
  if (
    (await loginForm
      .locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]')
      .count()) > 0
  ) {
    throw new Error('login captcha is enabled; unattended real E2E cannot continue')
  }

  const tenantInput = loginForm
    .locator(
      '.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]'
    )
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  await tenantInput.fill(TENANT_NAME)
  const tenantOption = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: TENANT_NAME })
    .first()
  if (await tenantOption.isVisible({ timeout: 3000 }).catch(() => false)) {
    await tenantOption.click()
  } else {
    await tenantInput.press('Enter')
  }

  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入用户名"]'),
    LOGIN_USERNAME,
    'username'
  )
  await fillFirstVisible(
    loginForm.locator('input[type="password"], input[placeholder="请输入密码"]'),
    password,
    'password'
  )
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await loginForm.getByRole('button', { name: /^登录$/ }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.ok(response.ok(), `login HTTP ${response.status()}`)
  unwrapCommonResult(body, `login ${TENANT_NAME}/${LOGIN_USERNAME}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

function formItem(page, label) {
  return page.locator('.el-form-item').filter({ hasText: label }).first()
}

async function fillFormInput(page, label, value) {
  const input = formItem(page, label).locator('input').first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill(String(value))
  await input.press('Tab')
  return input
}

async function fillFormTextarea(page, label, value) {
  const textarea = formItem(page, label).locator('textarea').first()
  await textarea.waitFor({ state: 'visible', timeout: 30000 })
  await textarea.fill(String(value))
  return textarea
}

async function fillFrontlineNumber(page, label, value) {
  const field = page.locator('label').filter({ hasText: label }).first()
  await field.waitFor({ state: 'visible', timeout: 30000 })
  const input = field.locator('input').first()
  await input.fill(String(value))
  await input.press('Tab')
  const actual = Number(await input.inputValue())
  assert.equal(actual, Number(value), `${label} must contain ${value}`)
}

async function waitForFrontlineReady(page) {
  const status = page.locator('.frontline-submit-bar span').first()
  await status.waitFor({ state: 'visible', timeout: 90000 })
  const deadline = Date.now() + 90000
  while (Date.now() < deadline) {
    const text = (await status.innerText()).trim()
    if (text === '准备提交') {
      return
    }
    if (
      text.includes('缺少') ||
      text.includes('失败') ||
      text.includes('无权限') ||
      text.includes('不存在')
    ) {
      throw new Error(`frontline context did not become ready: ${text}`)
    }
    await page.waitForTimeout(500)
  }
  throw new Error(`frontline page did not become ready: ${(await status.innerText()).trim()}`)
}

async function ensureFrontlineEmployee(page) {
  const operator = page.locator('.frontline-operator-screen:visible').first()
  await operator.waitFor({ state: 'visible', timeout: 90000 })
  const employeeCard = operator.locator('button.frontline-top-card').filter({ hasText: '员工' }).first()
  await employeeCard.waitFor({ state: 'visible', timeout: 90000 })
  await employeeCard.locator('strong').filter({ hasText: LOGIN_USER_NICKNAME }).waitFor({
    state: 'visible',
    timeout: 90000
  })
}

async function assertFrontlineProcess(page, expectedProcessName) {
  const operator = page.locator('.frontline-operator-screen:visible').first()
  await operator.waitFor({ state: 'visible', timeout: 90000 })
  const processCard = operator.locator('button.frontline-top-card').filter({ hasText: '工序' }).first()
  await processCard.waitFor({ state: 'visible', timeout: 90000 })
  const processLabel = processCard.locator('strong')
  await processLabel.filter({ hasText: expectedProcessName }).waitFor({
    state: 'visible',
    timeout: 90000
  })
  assert.ok(
    (await processLabel.innerText()).includes(expectedProcessName),
    `frontline page must select process ${expectedProcessName}`
  )
}

function recordWriteResponse(state, label, response, body) {
  const url = new URL(response.url())
  state.writeEvidence.push({
    label,
    method: response.request().method(),
    path: url.pathname,
    httpStatus: response.status(),
    responseCode: Number(body.code),
    responseData:
      body.data && typeof body.data === 'object'
        ? Object.fromEntries(
            Object.entries(body.data).filter(
              ([key]) =>
                key.endsWith('Id') ||
                key === 'totalAllocatedQuantity' ||
                key === 'lines'
            )
          )
        : body.data
  })
}

async function captureScreenshot(page, state, name) {
  fs.mkdirSync(state.artifactDir, { recursive: true })
  const target = path.join(state.artifactDir, `${name}.png`)
  await page.screenshot({ path: target, fullPage: true })
  state.screenshots.push(target)
  return target
}

function buildFrontlineUrl(pathname, fixture) {
  const url = new URL(pathname, BASE_URL)
  const query = {
    routeId: fixture.routeId,
    routeProcessId: fixture.routeProcessId,
    processId: fixture.processId,
    taskId: fixture.taskId,
    taskCode: fixture.taskCode,
    actualEmployeeId: LOGIN_USER_ID,
    deviceAccountUserId: LOGIN_USER_ID,
    feedbackCode: fixture.feedbackCode
  }
  for (const [key, value] of Object.entries(query)) {
    url.searchParams.set(key, String(value))
  }
  return url.toString()
}

async function submitFrontlineProduction(page, password, state) {
  const fixture = {
    routeId: state.productionRouteId,
    routeProcessId: state.productionRouteProcessId,
    processId: state.productionProcessId,
    processName: state.productionProcessName,
    taskId: state.productionTaskId,
    taskCode: state.productionTaskCode,
    feedbackCode: state.productionFeedbackCode
  }
  await page.goto(buildFrontlineUrl(PRODUCTION_PATH, fixture), {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.locator('[data-frontline-production-operator]').waitFor({
    state: 'visible',
    timeout: 90000
  })
  await assertFrontlineProcess(page, fixture.processName)
  await ensureFrontlineEmployee(page)
  await waitForFrontlineReady(page)

  await fillFrontlineNumber(
    page,
    '上工序输入数量',
    productionValues.previousProcessInputQuantity
  )
  await fillFrontlineNumber(page, '输出数量', productionValues.outputQuantity)
  await fillFrontlineNumber(page, '损耗数量', productionValues.lossQuantity)
  const parameterInput = page.locator('.frontline-device-card input').first()
  await parameterInput.waitFor({ state: 'visible', timeout: 30000 })
  await parameterInput.fill(`${state.runMarker}-DEVICE-PARAMETER`)
  assert.ok(
    (await parameterInput.inputValue()).includes(state.runMarker),
    'production device parameter must contain the run marker'
  )

  await page.getByRole('button', { name: '提交', exact: true }).click()
  const dialog = page
    .locator('[data-frontline-submit-signature-dialog]:visible, .el-dialog:visible')
    .filter({ hasText: '电子签名确认' })
    .first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), password, 'production signature password')
  await fillFirstVisible(
    dialog.locator('input[placeholder*="可选"]'),
    `${state.runMarker} production submit`,
    'production signature comment'
  )
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(FRONTLINE_SUBMIT_ENDPOINT) &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await dialog.getByRole('button', { name: '确认提交', exact: true }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.ok(response.ok(), `production frontline submit HTTP ${response.status()}`)
  const data = unwrapCommonResult(body, 'production frontline submit')
  recordWriteResponse(state, 'production-frontline-submit', response, body)
  assert.ok(Number(data.feedbackId) > 0, 'production submit must return feedbackId')
  assert.ok(Number(data.recordbookEntryId) > 0, 'production submit must return recordbookEntryId')
  assert.ok(Number(data.recordbookEventId) > 0, 'production submit must return recordbookEventId')
  assert.ok(Number(data.processPoolEventId) > 0, 'production submit must return processPoolEventId')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  await captureScreenshot(page, state, '01-production-submit')
  return data
}

async function submitFrontlinePqc(page, password, state) {
  const fixture = {
    routeId: state.pqcRouteId,
    routeProcessId: state.pqcRouteProcessId,
    processId: state.pqcProcessId,
    processName: state.pqcProcessName,
    taskId: state.pqcTaskId,
    taskCode: state.pqcTaskCode,
    feedbackCode: state.pqcFeedbackCode
  }
  await page.goto(buildFrontlineUrl(PQC_PATH, fixture), {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.locator('[data-frontline-pqc-operator]').waitFor({
    state: 'visible',
    timeout: 90000
  })
  await assertFrontlineProcess(page, fixture.processName)
  await ensureFrontlineEmployee(page)
  await waitForFrontlineReady(page)

  for (const label of ['外观', '密封']) {
    const row = page.locator('.frontline-inspection-row').filter({ hasText: label }).first()
    await row.waitFor({ state: 'visible', timeout: 30000 })
    await row.getByText('合格', { exact: true }).click()
  }
  await page.getByText('首检', { exact: true }).click()
  await fillFrontlineNumber(page, '检验数量', pqcValues.inspectionQuantity)
  await fillFrontlineNumber(page, '损耗数量', pqcValues.lossQuantity)

  await page.getByRole('button', { name: '提交', exact: true }).click()
  const dialog = page
    .locator('[data-frontline-submit-signature-dialog]:visible, .el-dialog:visible')
    .filter({ hasText: '电子签名确认' })
    .first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), password, 'PQC signature password')
  await fillFirstVisible(
    dialog.locator('input[placeholder*="可选"]'),
    `${state.runMarker} PQC submit`,
    'PQC signature comment'
  )
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(FRONTLINE_SUBMIT_ENDPOINT) &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await dialog.getByRole('button', { name: '确认提交', exact: true }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.ok(response.ok(), `PQC frontline submit HTTP ${response.status()}`)
  const data = unwrapCommonResult(body, 'PQC frontline submit')
  recordWriteResponse(state, 'pqc-frontline-submit', response, body)
  assert.ok(Number(data.feedbackId) > 0, 'PQC submit must return feedbackId')
  assert.ok(Number(data.recordbookEntryId) > 0, 'PQC submit must return recordbookEntryId')
  assert.ok(Number(data.recordbookEventId) > 0, 'PQC submit must return recordbookEventId')
  assert.ok(Number(data.processPoolEventId) > 0, 'PQC submit must return processPoolEventId')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  await captureScreenshot(page, state, '02-pqc-submit')
  return data
}

async function runFifoAllocation(page, state) {
  await page.goto(`${BASE_URL}${FIFO_PATH}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.locator('.process-pool-fifo__title').filter({ hasText: 'FIFO 编排' }).waitFor({
    state: 'visible',
    timeout: 90000
  })
  await fillFormInput(page, '分配批次号', state.allocationBatchNo)
  await fillFormInput(page, '来源工序ID', state.productionProcessId)
  await fillFormInput(page, '目标路线工序ID', state.pqcRouteProcessId)
  await fillFormInput(page, '目标工序ID', state.pqcProcessId)

  const lateWorkOrderId = state.lateWorkOrderId
  const earlyWorkOrderId = state.earlyWorkOrderId
  const targetWorkOrderIdsText = `${lateWorkOrderId},${earlyWorkOrderId}`
  await fillFormTextarea(page, '目标生产工单ID列表', targetWorkOrderIdsText)

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(FIFO_ALLOCATE_ENDPOINT) &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await page.getByRole('button', { name: '执行 FIFO 分配', exact: true }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.ok(response.ok(), `FIFO allocation HTTP ${response.status()}`)
  const data = unwrapCommonResult(body, 'FIFO allocation')
  recordWriteResponse(state, 'fifo-allocation', response, body)

  assert.equal(Number(data.totalAllocatedQuantity), 50, 'FIFO must allocate all 50 output units')
  assert.equal(data.lines?.length, 2, 'FIFO must create exactly 2 allocation lines')
  assert.equal(
    Number(data.lines[0].targetWorkOrderId),
    Number(earlyWorkOrderId),
    'FIFO must allocate the earlier work order first even though the UI submitted it second'
  )
  assert.equal(
    Number(data.lines[0].allocatedQuantity),
    fifoValues.earlyDemandQuantity,
    'early work order must receive 20'
  )
  assert.equal(
    Number(data.lines[1].targetWorkOrderId),
    Number(lateWorkOrderId),
    'FIFO must allocate the later work order second'
  )
  assert.equal(
    Number(data.lines[1].allocatedQuantity),
    fifoValues.lateDemandQuantity,
    'late work order must receive 30'
  )

  const rows = page.locator('.process-pool-fifo__table .el-table__body-wrapper tbody tr')
  await rows.first().waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await rows.count(), 2, 'FIFO UI table must show exactly 2 lines')
  const firstRowText = await rows.nth(0).innerText()
  const secondRowText = await rows.nth(1).innerText()
  assert.ok(firstRowText.includes(state.earlyWorkOrderCode), 'FIFO UI first row must be the early work order')
  assert.ok(firstRowText.includes('20'), 'FIFO UI first row must show 20')
  assert.ok(secondRowText.includes(state.lateWorkOrderCode), 'FIFO UI second row must be the late work order')
  assert.ok(secondRowText.includes('30'), 'FIFO UI second row must show 30')
  await captureScreenshot(page, state, '03-fifo-allocation')
  return data
}

async function runAutomaticReviewCopy(page, state) {
  await page.goto(`${BASE_URL}${REVIEW_PATH}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.locator('.process-pool-write__title').filter({ hasText: '审核副本处理' }).waitFor({
    state: 'visible',
    timeout: 90000
  })
  const signatureSnapshot = JSON.stringify(
    {
      signType: 'REVIEW',
      signedAt: '2026-07-30T00:00:00+08:00',
      reviewerUserId: LOGIN_USER_ID,
      runMarker: state.runMarker
    },
    null,
    2
  )
  await fillFormInput(page, '工序池提交事件ID', state.productionSubmit.processPoolEventId)
  await fillFormInput(page, '审核人用户ID', LOGIN_USER_ID)
  await fillFormInput(page, '审核签名ID', state.reviewSignatureId)
  await fillFormInput(page, '签名员工用户ID', LOGIN_USER_ID)
  await fillFormTextarea(page, '审核签名快照JSON', signatureSnapshot)

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(REVIEW_FROM_RULES_ENDPOINT) &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await page
    .getByRole('button', { name: '按正式规则自动生成并提交', exact: true })
    .click()
  const response = await responsePromise
  const body = await response.json()
  assert.ok(response.ok(), `review copy HTTP ${response.status()}`)
  const reviewCopyId = unwrapCommonResult(body, 'automatic review copy')
  recordWriteResponse(state, 'automatic-review-copy', response, body)
  assert.ok(Number(reviewCopyId) > 0, 'automatic review copy must return a positive id')
  await page.getByText(`审核副本编号：${reviewCopyId}`, { exact: false }).waitFor({
    state: 'visible',
    timeout: 30000
  })
  await captureScreenshot(page, state, '04-review-copy')
  return Number(reviewCopyId)
}

function verifyDatabaseState(state) {
  const production = state.productionSubmit
  const pqc = state.pqcSubmit
  const markerContainsSql = sqlString(`%${state.runMarker}%`)
  const output = mysql(
    `
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'productionFeedbackCount', (SELECT COUNT(*) FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(production.feedbackId, 'production feedback id')}),
  'productionFeedbackQuantity', (SELECT feedback_quantity + 0 FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(production.feedbackId, 'production feedback id')}),
  'productionLossQuantity', (SELECT unqualified_quantity + 0 FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(production.feedbackId, 'production feedback id')}),
  'productionFeedbackStatus', (SELECT status FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(production.feedbackId, 'production feedback id')}),
  'productionFeedbackUserId', (SELECT feedback_user_id FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(production.feedbackId, 'production feedback id')}),
  'productionFeedbackTimePresent', (SELECT feedback_time IS NOT NULL FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(production.feedbackId, 'production feedback id')}),
  'pqcFeedbackCount', (SELECT COUNT(*) FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(pqc.feedbackId, 'PQC feedback id')}),
  'pqcInspectionQuantity', (SELECT feedback_quantity + 0 FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(pqc.feedbackId, 'PQC feedback id')}),
  'pqcLossQuantity', (SELECT unqualified_quantity + 0 FROM mes_pro_feedback
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(pqc.feedbackId, 'PQC feedback id')}),
  'productionRecordbookEntryCount', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_entry
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      production.recordbookEntryId,
      'production recordbook entry id'
    )}),
  'productionRecordbookPreviousInput', (SELECT JSON_UNQUOTE(JSON_EXTRACT(
      entry_content_json, '$.previousProcessInputQuantity'))
    FROM mes_pro_edhr_recordbook_entry
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      production.recordbookEntryId,
      'production recordbook entry id'
    )}),
  'productionRecordbookContainsMarker', (SELECT entry_content_json LIKE ${markerContainsSql}
    FROM mes_pro_edhr_recordbook_entry
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      production.recordbookEntryId,
      'production recordbook entry id'
    )}),
  'productionRecordbookTagCount', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_tag_binding
    WHERE tenant_id = ${TENANT_ID}
      AND entry_id = ${sqlNumber(production.recordbookEntryId, 'production recordbook entry id')}
      AND tag_code = 'FRONTLINE_PRODUCTION'),
  'pqcRecordbookEntryCount', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_entry
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      pqc.recordbookEntryId,
      'PQC recordbook entry id'
    )}),
  'pqcRecordbookTagCount', (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_tag_binding
    WHERE tenant_id = ${TENANT_ID}
      AND entry_id = ${sqlNumber(pqc.recordbookEntryId, 'PQC recordbook entry id')}
      AND tag_code = 'FRONTLINE_PQC'),
  'productionEventCount', (SELECT COUNT(*) FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      production.processPoolEventId,
      'production process pool event id'
    )}),
  'productionEventOutputRaw', (SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_payload, '$.OUTPUT_QUANTITY'))
    FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      production.processPoolEventId,
      'production process pool event id'
    )}),
  'productionActualEmployeeId', (SELECT actual_employee_id FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      production.processPoolEventId,
      'production process pool event id'
    )}),
  'productionQuantityFragmentCount', (SELECT COUNT(*) FROM mes_pro_process_pool_quantity_fragment
    WHERE tenant_id = ${TENANT_ID}
      AND event_id = ${sqlNumber(production.processPoolEventId, 'production process pool event id')}),
  'productionOutputTotal', (SELECT total_quantity + 0 FROM mes_pro_process_pool_quantity_fragment
    WHERE tenant_id = ${TENANT_ID}
      AND event_id = ${sqlNumber(production.processPoolEventId, 'production process pool event id')}
      AND source_quantity_type = 'OUTPUT'),
  'productionOutputAllocated', (SELECT allocated_quantity + 0 FROM mes_pro_process_pool_quantity_fragment
    WHERE tenant_id = ${TENANT_ID}
      AND event_id = ${sqlNumber(production.processPoolEventId, 'production process pool event id')}
      AND source_quantity_type = 'OUTPUT'),
  'productionOutputAvailable', (SELECT available_quantity + 0 FROM mes_pro_process_pool_quantity_fragment
    WHERE tenant_id = ${TENANT_ID}
      AND event_id = ${sqlNumber(production.processPoolEventId, 'production process pool event id')}
      AND source_quantity_type = 'OUTPUT'),
  'productionOutputAllocationStatus', (SELECT allocation_status FROM mes_pro_process_pool_quantity_fragment
    WHERE tenant_id = ${TENANT_ID}
      AND event_id = ${sqlNumber(production.processPoolEventId, 'production process pool event id')}
      AND source_quantity_type = 'OUTPUT'),
  'productionLossTotal', (SELECT total_quantity + 0 FROM mes_pro_process_pool_quantity_fragment
    WHERE tenant_id = ${TENANT_ID}
      AND event_id = ${sqlNumber(production.processPoolEventId, 'production process pool event id')}
      AND source_quantity_type = 'LOSS'),
  'pqcEventCount', (SELECT COUNT(*) FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      pqc.processPoolEventId,
      'PQC process pool event id'
    )}),
  'pqcResult', (SELECT inspection_result FROM mes_pro_process_pool_pqc_record
    WHERE tenant_id = ${TENANT_ID}
      AND event_id = ${sqlNumber(pqc.processPoolEventId, 'PQC process pool event id')}),
  'pqcQuantityFragmentCount', (SELECT COUNT(*) FROM mes_pro_process_pool_quantity_fragment
    WHERE tenant_id = ${TENANT_ID}
      AND event_id = ${sqlNumber(pqc.processPoolEventId, 'PQC process pool event id')}),
  'fifoLineCount', (SELECT COUNT(*) FROM mes_pro_process_pool_fifo_allocation_line
    WHERE tenant_id = ${TENANT_ID}
      AND allocation_batch_no = ${sqlString(state.allocationBatchNo)}),
  'fifoFirstTargetWorkOrderId', (SELECT target_work_order_id FROM mes_pro_process_pool_fifo_allocation_line
    WHERE tenant_id = ${TENANT_ID}
      AND allocation_batch_no = ${sqlString(state.allocationBatchNo)}
    ORDER BY id ASC LIMIT 1),
  'fifoFirstQuantity', (SELECT allocated_quantity + 0 FROM mes_pro_process_pool_fifo_allocation_line
    WHERE tenant_id = ${TENANT_ID}
      AND allocation_batch_no = ${sqlString(state.allocationBatchNo)}
    ORDER BY id ASC LIMIT 1),
  'fifoSecondTargetWorkOrderId', (SELECT target_work_order_id FROM mes_pro_process_pool_fifo_allocation_line
    WHERE tenant_id = ${TENANT_ID}
      AND allocation_batch_no = ${sqlString(state.allocationBatchNo)}
    ORDER BY id ASC LIMIT 1 OFFSET 1),
  'fifoSecondQuantity', (SELECT allocated_quantity + 0 FROM mes_pro_process_pool_fifo_allocation_line
    WHERE tenant_id = ${TENANT_ID}
      AND allocation_batch_no = ${sqlString(state.allocationBatchNo)}
    ORDER BY id ASC LIMIT 1 OFFSET 1),
  'reviewCopyCount', (SELECT COUNT(*) FROM mes_pro_process_pool_review_copy
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(state.reviewCopyId, 'review copy id')}),
  'reviewCopyStatus', (SELECT review_status FROM mes_pro_process_pool_review_copy
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(state.reviewCopyId, 'review copy id')}),
  'reviewFieldCount', (SELECT COUNT(*) FROM mes_pro_process_pool_review_copy_field
    WHERE tenant_id = ${TENANT_ID} AND review_copy_id = ${sqlNumber(
      state.reviewCopyId,
      'review copy id'
    )}),
  'reviewFieldCode', (SELECT field_code FROM mes_pro_process_pool_review_copy_field
    WHERE tenant_id = ${TENANT_ID} AND review_copy_id = ${sqlNumber(
      state.reviewCopyId,
      'review copy id'
    )}),
  'reviewFieldRaw', (SELECT raw_value FROM mes_pro_process_pool_review_copy_field
    WHERE tenant_id = ${TENANT_ID} AND review_copy_id = ${sqlNumber(
      state.reviewCopyId,
      'review copy id'
    )}),
  'reviewFieldCorrected', (SELECT corrected_value FROM mes_pro_process_pool_review_copy_field
    WHERE tenant_id = ${TENANT_ID} AND review_copy_id = ${sqlNumber(
      state.reviewCopyId,
      'review copy id'
    )}),
  'reviewFieldLowerLimit', (SELECT lower_limit + 0 FROM mes_pro_process_pool_review_copy_field
    WHERE tenant_id = ${TENANT_ID} AND review_copy_id = ${sqlNumber(
      state.reviewCopyId,
      'review copy id'
    )}),
  'reviewFieldUpperLimit', (SELECT upper_limit + 0 FROM mes_pro_process_pool_review_copy_field
    WHERE tenant_id = ${TENANT_ID} AND review_copy_id = ${sqlNumber(
      state.reviewCopyId,
      'review copy id'
    )}),
  'reviewFieldRuleType', (SELECT rule_type FROM mes_pro_process_pool_review_copy_field
    WHERE tenant_id = ${TENANT_ID} AND review_copy_id = ${sqlNumber(
      state.reviewCopyId,
      'review copy id'
    )}),
  'reviewFieldAffectsAllocation', (SELECT affects_allocation + 0
    FROM mes_pro_process_pool_review_copy_field
    WHERE tenant_id = ${TENANT_ID} AND review_copy_id = ${sqlNumber(
      state.reviewCopyId,
      'review copy id'
    )}),
  'submitDate', (SELECT DATE_FORMAT(server_submit_time, '%Y-%m-%d')
    FROM mes_pro_process_pool_event
    WHERE tenant_id = ${TENANT_ID} AND id = ${sqlNumber(
      production.processPoolEventId,
      'production process pool event id'
    )})
);
`,
    'verify full-chain database state'
  )
  const evidence = parseJsonOutput(output, 'full-chain database evidence')

  assert.equal(Number(evidence.productionFeedbackCount), 1)
  assert.equal(Number(evidence.productionFeedbackQuantity), productionValues.outputQuantity)
  assert.equal(Number(evidence.productionLossQuantity), productionValues.lossQuantity)
  assert.equal(Number(evidence.productionFeedbackStatus), 2)
  assert.equal(Number(evidence.productionFeedbackUserId), LOGIN_USER_ID)
  assert.equal(Number(evidence.productionFeedbackTimePresent), 1)
  assert.equal(Number(evidence.pqcFeedbackCount), 1)
  assert.equal(Number(evidence.pqcInspectionQuantity), pqcValues.inspectionQuantity)
  assert.equal(Number(evidence.pqcLossQuantity), pqcValues.lossQuantity)
  assert.equal(Number(evidence.productionRecordbookEntryCount), 1)
  assert.equal(
    Number(evidence.productionRecordbookPreviousInput),
    productionValues.previousProcessInputQuantity
  )
  assert.equal(Number(evidence.productionRecordbookContainsMarker), 1)
  assert.equal(Number(evidence.productionRecordbookTagCount), 1)
  assert.equal(Number(evidence.pqcRecordbookEntryCount), 1)
  assert.equal(Number(evidence.pqcRecordbookTagCount), 1)
  assert.equal(Number(evidence.productionEventCount), 1)
  assert.equal(Number(evidence.productionEventOutputRaw), reviewRule.rawValue)
  assert.equal(Number(evidence.productionActualEmployeeId), LOGIN_USER_ID)
  assert.equal(Number(evidence.productionQuantityFragmentCount), 2)
  assert.equal(Number(evidence.productionOutputTotal), productionValues.outputQuantity)
  assert.equal(Number(evidence.productionOutputAllocated), productionValues.outputQuantity)
  assert.equal(Number(evidence.productionOutputAvailable), 0)
  assert.equal(evidence.productionOutputAllocationStatus, 'ALLOCATED')
  assert.equal(Number(evidence.productionLossTotal), productionValues.lossQuantity)
  assert.equal(Number(evidence.pqcEventCount), 1)
  assert.equal(evidence.pqcResult, pqcValues.result)
  assert.equal(Number(evidence.pqcQuantityFragmentCount), 0)
  assert.equal(Number(evidence.fifoLineCount), 2)
  assert.equal(Number(evidence.fifoFirstTargetWorkOrderId), Number(state.earlyWorkOrderId))
  assert.equal(Number(evidence.fifoFirstQuantity), fifoValues.earlyDemandQuantity)
  assert.equal(Number(evidence.fifoSecondTargetWorkOrderId), Number(state.lateWorkOrderId))
  assert.equal(Number(evidence.fifoSecondQuantity), fifoValues.lateDemandQuantity)
  assert.equal(Number(evidence.reviewCopyCount), 1)
  assert.equal(evidence.reviewCopyStatus, 'SUBMITTED')
  assert.equal(Number(evidence.reviewFieldCount), 1)
  assert.equal(evidence.reviewFieldCode, reviewRule.fieldCode)
  assert.equal(Number(evidence.reviewFieldRaw), reviewRule.rawValue)
  assert.equal(Number(evidence.reviewFieldCorrected), reviewRule.correctedValue)
  assert.equal(Number(evidence.reviewFieldLowerLimit), reviewRule.lowerLimit)
  assert.equal(Number(evidence.reviewFieldUpperLimit), reviewRule.upperLimit)
  assert.equal(evidence.reviewFieldRuleType, 'CLAMP_TO_MAX')
  assert.equal(Number(evidence.reviewFieldAffectsAllocation), Number(reviewRule.affectsAllocation))
  assert.match(evidence.submitDate, /^\d{4}-\d{2}-\d{2}$/)
  return evidence
}

async function runTeamLeaderWorkbench(page, state) {
  await page.goto(`${BASE_URL}${WORKBENCH_PATH}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page
    .locator('.process-pool-team-workbench__title')
    .filter({ hasText: '班组长工作台' })
    .waitFor({ state: 'visible', timeout: 90000 })

  const dateInput = formItem(page, '提交日期').locator('input').first()
  await dateInput.fill(state.databaseEvidence.submitDate)
  await dateInput.press('Enter')
  await fillFormInput(page, '员工', LOGIN_USER_ID)

  const responsePromise = page.waitForResponse(
    (response) => {
      if (
        !response.url().includes(WORKBENCH_PAGE_ENDPOINT) ||
        response.request().method() !== 'GET'
      ) {
        return false
      }
      const responseUrl = new URL(response.url())
      return (
        responseUrl.searchParams.get('submitDate') === state.databaseEvidence.submitDate &&
        responseUrl.searchParams.get('employeeUserId') === String(LOGIN_USER_ID)
      )
    },
    { timeout: 90000 }
  )
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.ok(response.ok(), `team leader workbench HTTP ${response.status()}`)
  const data = unwrapCommonResult(body, 'team leader workbench')
  state.readEvidence.push({
    label: 'team-leader-workbench-page',
    method: response.request().method(),
    path: new URL(response.url()).pathname,
    httpStatus: response.status(),
    responseCode: Number(body.code),
    total: Number(data.total || 0)
  })

  const events = Array.isArray(data.events) ? data.events : []
  const productionEvent = events.find(
    (event) => Number(event.id) === Number(state.productionSubmit.processPoolEventId)
  )
  const pqcEvent = events.find(
    (event) => Number(event.id) === Number(state.pqcSubmit.processPoolEventId)
  )
  assert.ok(productionEvent, 'team leader workbench must return the production event')
  assert.ok(pqcEvent, 'team leader workbench must return the PQC event')

  const rows = page.locator(
    '.process-pool-team-workbench__table .el-table__body-wrapper tbody tr'
  )
  await rows.first().waitFor({ state: 'visible', timeout: 30000 })
  const productionRow = rows.filter({ hasText: state.productionWorkOrderCode }).first()
  const pqcRow = rows.filter({ hasText: state.pqcWorkOrderCode }).first()
  await productionRow.waitFor({ state: 'visible', timeout: 30000 })
  await pqcRow.waitFor({ state: 'visible', timeout: 30000 })
  const productionRowText = await productionRow.innerText()
  const pqcRowText = await pqcRow.innerText()
  await captureScreenshot(page, state, '05-team-leader-workbench')

  const defects = []
  if (productionEvent.actualEmployeeUserName !== LOGIN_USER_NICKNAME) {
    defects.push(
      `actualEmployeeUserName expected ${LOGIN_USER_NICKNAME}, actual ${String(
        productionEvent.actualEmployeeUserName
      )}`
    )
  }
  if (productionEvent.fifoAllocationStatus !== 'ALLOCATED') {
    defects.push(
      `fifoAllocationStatus expected ALLOCATED, actual ${String(
        productionEvent.fifoAllocationStatus
      )}`
    )
  }
  if (productionEvent.auditCopyStatus !== 'SUBMITTED') {
    defects.push(
      `auditCopyStatus expected SUBMITTED, actual ${String(productionEvent.auditCopyStatus)}`
    )
  }
  if (pqcEvent.pqcResult !== pqcValues.result) {
    defects.push(`pqcResult expected ${pqcValues.result}, actual ${String(pqcEvent.pqcResult)}`)
  }
  if (!productionRowText.includes(LOGIN_USER_NICKNAME)) {
    defects.push('production UI row does not show the actual employee name')
  }
  if (!productionRowText.includes('ALLOCATED')) {
    defects.push('production UI row does not show FIFO ALLOCATED')
  }
  if (!productionRowText.includes('SUBMITTED')) {
    defects.push('production UI row does not show review copy SUBMITTED')
  }
  if (!pqcRowText.includes(pqcValues.result)) {
    defects.push(`PQC UI row does not show ${pqcValues.result}`)
  }

  assert.deepEqual(
    defects,
    [],
    `team leader workbench full-chain assertions failed:\n${defects.join('\n')}`
  )

  const detailResponsePromise = page.waitForResponse(
    (response) => {
      if (
        !response.url().includes(WORKBENCH_DETAIL_ENDPOINT) ||
        response.request().method() !== 'GET'
      ) {
        return false
      }
      const responseUrl = new URL(response.url())
      return (
        responseUrl.searchParams.get('id') ===
        String(state.productionSubmit.processPoolEventId)
      )
    },
    { timeout: 90000 }
  )
  await productionRow.getByRole('button', { name: '查看', exact: true }).click()
  const detailResponse = await detailResponsePromise
  const detailBody = await detailResponse.json()
  assert.ok(detailResponse.ok(), `team leader detail HTTP ${detailResponse.status()}`)
  const detailData = unwrapCommonResult(detailBody, 'team leader detail')
  state.readEvidence.push({
    label: 'team-leader-workbench-detail',
    method: detailResponse.request().method(),
    path: new URL(detailResponse.url()).pathname,
    httpStatus: detailResponse.status(),
    responseCode: Number(detailBody.code),
    eventId: Number(detailData.id)
  })
  assert.equal(detailData.actualEmployeeUserName, LOGIN_USER_NICKNAME)
  assert.equal(detailData.fifoAllocationStatus, 'ALLOCATED')
  assert.equal(detailData.auditCopyStatus, 'SUBMITTED')
  assert.match(
    String(detailData.fifoAllocationSummary),
    new RegExp(
      `^已分配\\s+${productionValues.outputQuantity}(?:\\.0+)?，待分配\\s+0(?:\\.0+)?$`
    )
  )
  assert.match(String(detailData.auditCopySummary), /^审核副本已提交，字段\s+1\s+个$/)

  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '班组长只读详情' }).first()
  await drawer.waitFor({ state: 'visible', timeout: 30000 })
  await drawer
    .getByText(LOGIN_USER_NICKNAME, { exact: true })
    .waitFor({ state: 'visible', timeout: 30000 })
  await drawer
    .getByText(String(detailData.fifoAllocationSummary), { exact: true })
    .waitFor({ state: 'visible', timeout: 30000 })
  await drawer
    .getByText(String(detailData.auditCopySummary), { exact: true })
    .waitFor({ state: 'visible', timeout: 30000 })
  await captureScreenshot(page, state, '06-team-leader-detail')
  return data
}

function writeEvidence(state) {
  fs.mkdirSync(state.artifactDir, { recursive: true })
  const evidencePath = path.join(state.artifactDir, 'result.json')
  const payload = {
    scenario: 'process-pool-full-chain-real-flow',
    status: state.status,
    runMarker: state.runMarker,
    runtime: state.runtime,
    tenant: TENANT_NAME,
    tenantId: TENANT_ID,
    username: LOGIN_USERNAME,
    userId: LOGIN_USER_ID,
    executionMode: 'playwright-real-ui',
    mockUsed: false,
    directApiBusinessWrites: 0,
    fixtureSource: `${MYSQL_CONTAINER}/${DATABASE_NAME}`,
    fixtureStatus: state.fixtureStatus,
    fixtureCounts: state.fixtureCounts,
    ids: {
      productionWorkOrderId: state.productionWorkOrderId,
      pqcWorkOrderId: state.pqcWorkOrderId,
      earlyWorkOrderId: state.earlyWorkOrderId,
      lateWorkOrderId: state.lateWorkOrderId,
      productionTaskId: state.productionTaskId,
      pqcTaskId: state.pqcTaskId,
      productionProcessPoolEventId: state.productionSubmit?.processPoolEventId,
      pqcProcessPoolEventId: state.pqcSubmit?.processPoolEventId,
      reviewCopyId: state.reviewCopyId
    },
    writeEvidence: state.writeEvidence,
    readEvidence: state.readEvidence,
    databaseEvidence: state.databaseEvidence,
    workbenchEvidence: state.workbenchEvidence,
    screenshots: state.screenshots,
    pageErrors: state.pageErrors,
    signatureAuthorizationRestoration: state.signatureAuthorizationRestoration,
    cleanup: state.cleanup,
    failures: state.failures
  }
  fs.writeFileSync(evidencePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  state.evidencePath = evidencePath
  return evidencePath
}

async function main() {
  const runtime = assertAllowedRuntimePair()
  await assertRuntimeReady(runtime)
  const password = readFrontendEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD')
  const runMarker = `PPFC-${Date.now()}-${process.pid}`
  const state = {
    status: 'FAIL',
    runMarker,
    runtime,
    artifactDir: path.join(OUTPUT_ROOT, runMarker),
    fixtureStatus: 'NOT_PREPARED',
    writeEvidence: [],
    readEvidence: [],
    screenshots: [],
    pageErrors: [],
    failures: []
  }

  let browser
  let context
  let page
  try {
    prepareFixtures(state)
    const { chromium } = loadPlaywright()
    browser = await chromium.launch({
      executablePath: BROWSER_EXECUTABLE,
      headless: process.env.PROCESS_POOL_FULL_CHAIN_E2E_HEADED !== '1',
      args: ['--disable-dev-shm-usage']
    })
    context = await browser.newContext({
      viewport: { width: 1600, height: 1000 },
      locale: 'zh-CN'
    })
    page = await context.newPage()
    page.setDefaultTimeout(90000)
    page.setDefaultNavigationTimeout(90000)
    page.on('pageerror', (error) => {
      state.pageErrors.push(error.stack || error.message)
    })

    await login(page, password)
    state.productionSubmit = await submitFrontlineProduction(page, password, state)
    state.pqcSubmit = await submitFrontlinePqc(page, password, state)
    state.fifoResult = await runFifoAllocation(page, state)
    state.reviewCopyId = await runAutomaticReviewCopy(page, state)
    state.databaseEvidence = verifyDatabaseState(state)
    state.workbenchEvidence = await runTeamLeaderWorkbench(page, state)
    assert.deepEqual(state.pageErrors, [], `page errors detected:\n${state.pageErrors.join('\n')}`)
    state.status = 'PASS'
  } catch (error) {
    state.failures.push(error instanceof Error ? error.stack || error.message : String(error))
    if (page) {
      await captureScreenshot(page, state, '99-failure').catch((screenshotError) => {
        state.failures.push(
          `failure screenshot error: ${
            screenshotError instanceof Error ? screenshotError.message : String(screenshotError)
          }`
        )
      })
    }
  } finally {
    try {
      state.signatureAuthorizationRestoration = restoreSignatureAuthorization(
        state.signatureAuthorization
      )
    } catch (error) {
      state.failures.push(
        `signature authorization restoration failed: ${
          error instanceof Error ? error.stack || error.message : String(error)
        }`
      )
    }
    try {
      state.cleanup = cleanupFixtures(state)
    } catch (error) {
      state.failures.push(
        `fixture cleanup failed: ${
          error instanceof Error ? error.stack || error.message : String(error)
        }`
      )
    }
  }

  await context?.close().catch((error) => {
    state.failures.push(`browser context close failed: ${error.message || String(error)}`)
  })
  await browser?.close().catch((error) => {
    state.failures.push(`browser close failed: ${error.message || String(error)}`)
  })

  if (state.failures.length > 0) {
    state.status = 'FAIL'
  }
  const evidencePath = writeEvidence(state)
  console.log(
    JSON.stringify(
      {
        status: state.status,
        runMarker: state.runMarker,
        evidencePath,
        writeEvidence: state.writeEvidence,
        signatureAuthorizationRestoration: state.signatureAuthorizationRestoration,
        cleanup: state.cleanup,
        failures: state.failures
      },
      null,
      2
    )
  )
  if (state.failures.length > 0) {
    throw new Error(state.failures.join('\n\n'))
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.stack || error.message : String(error))
  process.exitCode = 1
})
