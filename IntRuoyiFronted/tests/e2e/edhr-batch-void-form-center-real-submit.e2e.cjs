const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.EDHR_BATCH_VOID_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_BATCH_VOID_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const TENANT = process.env.EDHR_BATCH_VOID_E2E_TENANT || '测试租户'
const USERNAME = process.env.EDHR_BATCH_VOID_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_BATCH_VOID_E2E_PASSWORD || '111111'
const SIGNATURE_PASSWORD = process.env.EDHR_BATCH_VOID_E2E_SIGNATURE_PASSWORD || PASSWORD
const APPROVER_USERNAME = process.env.EDHR_BATCH_VOID_E2E_APPROVER_USERNAME || ''
const APPROVER_PASSWORD = process.env.EDHR_BATCH_VOID_E2E_APPROVER_PASSWORD || PASSWORD
const APPROVER_SIGNATURE_PASSWORD = process.env.EDHR_BATCH_VOID_E2E_APPROVER_SIGNATURE_PASSWORD || APPROVER_PASSWORD
const RUN_ID = process.env.EDHR_BATCH_VOID_E2E_RUN_ID || new Date().toISOString().replace(/\D/g, '').slice(0, 14)
const EXPECTED_TENANT_ID = process.env.EDHR_BATCH_VOID_E2E_TENANT_ID || '122'
const TEST_USER_ID = Number(process.env.EDHR_BATCH_VOID_E2E_USER_ID || '914520')
const APPROVAL_ROUTE = '/approval-center/todo'
const ARTIFACT_DIR = path.resolve(
  process.env.EDHR_BATCH_VOID_E2E_ARTIFACT_DIR ||
    path.join(
      WORKSPACE_ROOT,
      'doc/tasks/20260727-edhr-batch-void-work-task-closure/e2e-artifacts'
    )
)

function parseUrl(url, label) {
  const parsed = new URL(url)
  assert.match(parsed.protocol, /^http:$/, `${label} must use http`)
  assert.ok(['127.0.0.1', 'localhost'].includes(parsed.hostname), `${label} must stay local`)
  assert.ok(parsed.port, `${label} must include an explicit port`)
  return parsed
}

function normalizePath(value) {
  return path.resolve(value).toLowerCase()
}

function assertRuntimeUrls() {
  const frontend = parseUrl(BASE_URL, 'frontend URL')
  const backend = parseUrl(BACKEND_URL, 'backend URL')
  const frontendPort = Number(frontend.port)
  const backendPort = Number(backend.port)
  assert.equal(TENANT, '测试租户', 'write E2E must use the authorized test tenant')
  assert.equal(USERNAME, 'aoteman', 'write E2E must use the authorized test user')
  assert.ok(PASSWORD, 'login password must be supplied')
  assert.ok(SIGNATURE_PASSWORD, 'signature password must be supplied')
  assert.ok(APPROVER_PASSWORD, 'BPM approval requires an approver login password')
  assert.ok(APPROVER_SIGNATURE_PASSWORD, 'BPM approval requires an approver signature password')

  const workspace = normalizePath(WORKSPACE_ROOT)
  const worktreeRoot = normalizePath('D:/IntRuoyiWorktree')
  if (workspace.startsWith(`${worktreeRoot}${path.sep.toLowerCase()}`)) {
    const registryPath = 'D:/IntRuoyiWorktree/.ports/worktree-ports.json'
    assert.ok(fs.existsSync(registryPath), 'worktree URL validation requires the port registry')
    const registry = JSON.parse(fs.readFileSync(registryPath, 'utf8'))
    const entry = (registry.worktrees || []).find((item) => normalizePath(item.path) === workspace)
    assert.ok(entry, `missing port registry entry for ${WORKSPACE_ROOT}`)
    assert.equal(entry.active, true, 'worktree port registry entry must be active')
    assert.equal(entry.profile, 'int_main', 'this E2E expects an int_main profile worktree')
    assert.equal(frontendPort, Number(entry.frontendPort), 'frontend URL must match registered worktree port')
    assert.equal(backendPort, Number(entry.backendPort), 'backend URL must match registered worktree port')
    assert.equal(frontendPort - 8081, backendPort - 48081, 'frontend/backend ports must use the same int_main slot')
    return
  }

  assert.equal(frontendPort, 8081, 'main workspace frontend must use 8081')
  assert.equal(backendPort, 48081, 'main workspace backend must use 48081')
}

function sqlString(value) {
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

function mysql(sql) {
  return execFileSync(
    'docker',
    [
      'exec',
      '-i',
      'int-ruoyi-mysql',
      'mysql',
      '-uroot',
      '-p123456',
      '--batch',
      '--raw',
      '--skip-column-names',
      '--default-character-set=utf8mb4',
      'ruoyi-vue-pro'
    ],
    { input: sql, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }
  ).trim()
}

function parseJsonRow(output, label) {
  const line = output.split(/\r?\n/).find(Boolean)
  assert.ok(line && line !== 'NULL', `${label} returned empty output`)
  try {
    return JSON.parse(line)
  } catch (error) {
    throw new Error(`${label} returned non JSON output: ${line}`)
  }
}

function assertBusinessSuccess(body, label) {
  assert.ok(body && typeof body === 'object', `${label} must return JSON object`)
  assert.ok([0, 200].includes(Number(body.code)), `${label} failed: ${body.msg || body.message || body.code}`)
  return body.data
}

function assertBusinessFailure(body, label) {
  assert.ok(body && typeof body === 'object', `${label} must return JSON object`)
  assert.notEqual(Number(body.code), 0, `${label} should fail fast but returned success`)
  assert.notEqual(Number(body.code), 200, `${label} should fail fast but returned success`)
  return body
}

function resolveApproverCredentials(evidence) {
  if (APPROVER_USERNAME) {
    return {
      username: APPROVER_USERNAME,
      password: APPROVER_PASSWORD,
      signaturePassword: APPROVER_SIGNATURE_PASSWORD,
      source: 'env'
    }
  }
  assert.ok(evidence.processInstanceId, `approver resolution requires BPM process instance: ${JSON.stringify(evidence)}`)
  const approver = parseJsonRow(
    mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'assigneeUserId', t.ASSIGNEE_,
  'username', u.username,
  'nickname', u.nickname
)
FROM act_ru_task t
LEFT JOIN system_users u ON CAST(u.id AS CHAR)=t.ASSIGNEE_ AND u.tenant_id=${Number(EXPECTED_TENANT_ID)}
WHERE t.PROC_INST_ID_=${sqlString(evidence.processInstanceId)}
ORDER BY t.CREATE_TIME_ DESC
LIMIT 1;
`),
    'batch void BPM approver'
  )
  assert.ok(approver.username, `BPM approver must map to a system user: ${JSON.stringify(approver)}`)
  return {
    username: approver.username,
    password: APPROVER_PASSWORD,
    signaturePassword: APPROVER_SIGNATURE_PASSWORD,
    assigneeUserId: approver.assigneeUserId,
    source: 'bpm-task-assignee'
  }
}

function prepareVoidableBatchWithActiveTasks() {
  const runKey = `M7-VOID-WORKTASK-${RUN_ID}`
  const output = mysql(`
SET NAMES utf8mb4;
SET @tenant_id := 122;
SET @assignee_user_id := ${Number(TEST_USER_ID)};
SET @run_key := ${sqlString(runKey)};
SET @source_execution_id := (
  SELECT e.id
  FROM mes_pro_batch_record_execution e
  JOIN mes_pro_edhr_work_task wt ON wt.execution_id=e.id
  JOIN mes_pro_edhr_batch_execution be ON be.id=wt.batch_execution_id
    AND be.tenant_id=@tenant_id
    AND be.deleted=0
  JOIN mes_pro_edhr_batch_execution_task bt ON bt.id=wt.batch_task_id
    AND bt.batch_execution_id=be.id
    AND bt.tenant_id=@tenant_id
    AND bt.deleted=0
  WHERE e.tenant_id=@tenant_id
    AND e.deleted=0
    AND wt.tenant_id=@tenant_id
    AND wt.deleted=0
    AND wt.task_type='FILL'
    AND e.work_order_id IS NOT NULL
    AND COALESCE(e.route_process_id, wt.route_process_id, bt.route_process_id) IS NOT NULL
    AND e.batch_record_report_id IS NOT NULL
  ORDER BY e.id DESC
  LIMIT 1
);
SET @source_work_task_id := (
  SELECT wt.id
  FROM mes_pro_edhr_work_task wt
  WHERE wt.tenant_id=@tenant_id
    AND wt.deleted=0
    AND wt.task_type='FILL'
    AND wt.execution_id=@source_execution_id
  ORDER BY wt.id DESC
  LIMIT 1
);
SET @source_batch_execution_id := (
  SELECT wt.batch_execution_id FROM mes_pro_edhr_work_task wt WHERE wt.id=@source_work_task_id
);
SET @source_batch_task_id := (
  SELECT wt.batch_task_id FROM mes_pro_edhr_work_task wt WHERE wt.id=@source_work_task_id
);
SELECT CASE
  WHEN @source_execution_id IS NULL OR @source_work_task_id IS NULL
    OR @source_batch_execution_id IS NULL OR @source_batch_task_id IS NULL
    THEN JSON_OBJECT('blocked', 'test tenant has no complete source execution/batch context')
  ELSE JSON_OBJECT(
    'sourceExecutionId', @source_execution_id,
    'sourceWorkTaskId', @source_work_task_id,
    'sourceBatchExecutionId', @source_batch_execution_id,
    'sourceBatchTaskId', @source_batch_task_id
  )
END;
INSERT INTO mes_pro_edhr_batch_execution (
  batch_execution_code, work_order_id, work_order_code, batch_code, active_context_key,
  attempt_no, product_id, product_code, product_name, route_id, route_code, route_name,
  status, task_total, task_approved_count, blocked_count, aggregate_hash, remark,
  creator, updater, deleted, tenant_id
)
SELECT CONCAT('BE-', @run_key), COALESCE(be.work_order_id, e.work_order_id), COALESCE(be.work_order_code, e.work_order_code),
  @run_key, CONCAT('CTX-', @run_key), 1, be.product_id, be.product_code, be.product_name,
  COALESCE(be.route_id, e.route_id, rp.route_id), be.route_code, be.route_name,
  10, 5, 0, 0, NULL, 'Task-owned eDHR batch void work-task closure E2E fixture',
  'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_edhr_work_task wt ON wt.id=@source_work_task_id
JOIN mes_pro_edhr_batch_execution be ON be.id=@source_batch_execution_id
JOIN mes_pro_route_process rp ON rp.id=COALESCE(e.route_process_id, wt.route_process_id)
WHERE e.id=@source_execution_id;
SET @batch_execution_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, route_process_sort, process_id, process_code, process_name,
  batch_record_report_id, batch_record_report_name, batch_record_definition_id, batch_record_version_id,
  batch_record_sort, instance_scope, shared_form_key, form_slot_type, form_binding_key, form_template_id,
  form_template_name_snapshot, form_template_version_id, form_template_version_no, record_category,
  validation_profile, recordbook_enabled, permission_scope_id, route_binding_id, route_binding_snapshot_hash,
  required_policy, owner_role_key, archive_visibility, slot_config_snapshot_hash, execution_mode, execution_id,
  status, required_flag, opened_by, opened_at, creator, updater, deleted, tenant_id
)
SELECT @batch_execution_id, COALESCE(bt.node_type, 'ROUTE_FORM'), COALESCE(e.route_process_id, wt.route_process_id, bt.route_process_id),
  COALESCE(bt.route_process_sort, 10), COALESCE(bt.process_id, rp.process_id), bt.process_code, COALESCE(bt.process_name, p.name),
  COALESCE(bt.batch_record_report_id, e.batch_record_report_id), COALESCE(bt.batch_record_report_name, e.template_name),
  COALESCE(bt.batch_record_definition_id, e.batch_record_definition_id), COALESCE(bt.batch_record_version_id, e.batch_record_version_id),
  COALESCE(bt.batch_record_sort, 1), COALESCE(bt.instance_scope, 'PROCESS'), bt.shared_form_key, bt.form_slot_type,
  bt.form_binding_key, bt.form_template_id, bt.form_template_name_snapshot, bt.form_template_version_id, bt.form_template_version_no,
  COALESCE(bt.record_category, 'BATCH_RECORD'), COALESCE(bt.validation_profile, 'CONTROLLED_BATCH'),
  COALESCE(bt.recordbook_enabled, b'1'), bt.permission_scope_id, bt.route_binding_id, bt.route_binding_snapshot_hash,
  COALESCE(bt.required_policy, 'REQUIRED'), COALESCE(bt.owner_role_key, 'PRODUCTION'), COALESCE(bt.archive_visibility, 'DOSSIER'),
  COALESCE(bt.slot_config_snapshot_hash, e.slot_config_snapshot_hash), COALESCE(bt.execution_mode, 'SEQUENTIAL'), NULL,
  0, b'1', NULL, NULL, 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_edhr_work_task wt ON wt.id=@source_work_task_id
JOIN mes_pro_edhr_batch_execution_task bt ON bt.id=@source_batch_task_id
JOIN mes_pro_route_process rp ON rp.id=COALESCE(e.route_process_id, wt.route_process_id, bt.route_process_id)
JOIN mes_pro_process p ON p.id=COALESCE(bt.process_id, rp.process_id)
WHERE e.id=@source_execution_id;
SET @batch_task_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, responsibility_source_key, responsibility_source_version,
  responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key, status, due_time, overdue_at,
  overdue_reason, started_at, completed_at, action_url, reason, remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('WT-', @run_key, '-TODO'), 'FILL', @batch_execution_id, @batch_task_id, 'BATCH_TASK', @batch_task_id,
  NULL, e.id, COALESCE(be.work_order_id, e.work_order_id), COALESCE(be.work_order_code, e.work_order_code), @run_key,
  COALESCE(be.route_id, e.route_id, rp.route_id), COALESCE(e.route_process_id, wt.route_process_id, bt.route_process_id),
  COALESCE(bt.process_id, rp.process_id), COALESCE(bt.process_name, p.name), @assignee_user_id,
  'USER', @assignee_user_id, CAST(@assignee_user_id AS CHAR), @assignee_user_id, 'E2E_FIXTURE',
  CONCAT('BATCH_VOID_FIXTURE:', @run_key), '1', SHA2(@run_key, 256), b'0', 'ASSIGNEE', '', 'TODO',
  DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, NULL,
  CONCAT('/mes/pro/feedback/edhr-batch-execution/task/open?batchExecutionId=', @batch_execution_id, '&taskId=', @batch_task_id),
  'E2E active TODO before void', 'E2E active TODO before void', 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_edhr_work_task wt ON wt.id=@source_work_task_id
JOIN mes_pro_edhr_batch_execution be ON be.id=@source_batch_execution_id
JOIN mes_pro_edhr_batch_execution_task bt ON bt.id=@source_batch_task_id
JOIN mes_pro_route_process rp ON rp.id=COALESCE(e.route_process_id, wt.route_process_id, bt.route_process_id)
JOIN mes_pro_process p ON p.id=COALESCE(bt.process_id, rp.process_id)
WHERE e.id=@source_execution_id;
SET @todo_work_task_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, responsibility_source_key, responsibility_source_version,
  responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key, status, due_time, overdue_at,
  overdue_reason, started_at, completed_at, action_url, reason, remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('WT-', @run_key, '-DOING'), 'FILL', @batch_execution_id, @batch_task_id, 'BATCH_TASK', @batch_task_id,
  NULL, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, CONCAT('BATCH_VOID_FIXTURE:', @run_key, ':DOING'),
  responsibility_source_version, responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key,
  'DOING', due_time, overdue_at, overdue_reason, NOW(), NULL, action_url,
  'E2E active DOING before void', 'E2E active DOING before void', creator, updater, deleted, tenant_id
FROM mes_pro_edhr_work_task WHERE id=@todo_work_task_id;
SET @doing_work_task_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, responsibility_source_key, responsibility_source_version,
  responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key, status, due_time, overdue_at,
  overdue_reason, started_at, completed_at, action_url, reason, remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('WT-', @run_key, '-OVERDUE'), 'FILL', batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, CONCAT('BATCH_VOID_FIXTURE:', @run_key, ':OVERDUE'),
  responsibility_source_version, responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key,
  'OVERDUE', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 'E2E overdue before void', started_at, NULL, action_url,
  'E2E active OVERDUE before void', 'E2E active OVERDUE before void', creator, updater, deleted, tenant_id
FROM mes_pro_edhr_work_task WHERE id=@todo_work_task_id;
SET @overdue_work_task_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, responsibility_source_key, responsibility_source_version,
  responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key, status, due_time, overdue_at,
  overdue_reason, started_at, completed_at, action_url, reason, remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('WT-', @run_key, '-DONE'), 'FILL', batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, CONCAT('BATCH_VOID_FIXTURE:', @run_key, ':DONE'),
  responsibility_source_version, responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key,
  'DONE', due_time, overdue_at, overdue_reason, started_at, NOW(), action_url,
  'E2E DONE history before void', 'E2E DONE history before void', creator, updater, deleted, tenant_id
FROM mes_pro_edhr_work_task WHERE id=@todo_work_task_id;
SET @done_work_task_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, responsibility_source_key, responsibility_source_version,
  responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key, status, due_time, overdue_at,
  overdue_reason, started_at, completed_at, action_url, reason, remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('WT-', @run_key, '-CANCELED'), 'FILL', batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, source_execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id,
  process_id, process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, responsibility_source_type, CONCAT('BATCH_VOID_FIXTURE:', @run_key, ':CANCELED'),
  responsibility_source_version, responsibility_source_digest, ownership_locked, fill_mode, signature_cell_key,
  'CANCELED', due_time, overdue_at, overdue_reason, started_at, NOW(), action_url,
  'E2E already canceled before void', 'E2E already canceled before void', creator, updater, deleted, tenant_id
FROM mes_pro_edhr_work_task WHERE id=@todo_work_task_id;
SET @canceled_work_task_id := LAST_INSERT_ID();
SELECT JSON_OBJECT(
  'batchExecutionId', @batch_execution_id,
  'batchTaskId', @batch_task_id,
  'batchExecutionCode', CONCAT('BE-', @run_key),
  'batchCode', @run_key,
  'runKey', @run_key,
  'todoWorkTaskId', @todo_work_task_id,
  'doingWorkTaskId', @doing_work_task_id,
  'overdueWorkTaskId', @overdue_work_task_id,
  'doneWorkTaskId', @done_work_task_id,
  'canceledWorkTaskId', @canceled_work_task_id
);
`)
  const rows = output.split(/\r?\n/).filter(Boolean)
  const source = parseJsonRow(rows[0] || '', 'source batch precondition')
  if (source.blocked) {
    throw new Error(source.blocked)
  }
  const setup = parseJsonRow(rows[rows.length - 1] || '', 'batch void work-task setup')
  assert.ok(setup.batchExecutionId, `setup did not create batch execution: ${JSON.stringify(setup)}`)
  return setup
}

function loadVoidEvidence(batchExecutionId, changeEventId) {
  return parseJsonRow(
    mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'batchExecutionId', b.id,
  'batchStatus', b.status,
  'activeContextKey', b.active_context_key,
  'changeEventId', ev.id,
  'changeStatus', ev.change_status,
  'processInstanceId', ev.bpm_process_instance_id,
  'reasonText', ev.reason_text,
  'requestSignatureId', ev.request_signature_id,
  'requestSignatureActionType', sig.action_type,
  'activeTaskCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_work_task wt
    WHERE wt.batch_execution_id=b.id
      AND wt.tenant_id=122
      AND wt.deleted=0
      AND wt.status IN ('TODO', 'DOING', 'OVERDUE')
  ),
  'canceledTaskCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_work_task wt
    WHERE wt.batch_execution_id=b.id
      AND wt.tenant_id=122
      AND wt.deleted=0
      AND wt.status='CANCELED'
  ),
  'doneTaskCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_work_task wt
    WHERE wt.batch_execution_id=b.id
      AND wt.tenant_id=122
      AND wt.deleted=0
      AND wt.status='DONE'
  ),
  'canceledReasonPrefixCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_work_task wt
    WHERE wt.batch_execution_id=b.id
      AND wt.tenant_id=122
      AND wt.deleted=0
      AND wt.status='CANCELED'
      AND wt.reason LIKE '批次已作废：%'
  ),
  'workTasks', (
    SELECT JSON_ARRAYAGG(JSON_OBJECT(
      'id', wt.id,
      'taskCode', wt.task_code,
      'status', wt.status,
      'reason', wt.reason,
      'completedAt', DATE_FORMAT(wt.completed_at, '%Y-%m-%d %H:%i:%s')
    ))
    FROM mes_pro_edhr_work_task wt
    WHERE wt.batch_execution_id=b.id
      AND wt.tenant_id=122
      AND wt.deleted=0
  )
)
FROM mes_pro_edhr_batch_execution b
LEFT JOIN mes_pro_edhr_record_change_event ev
  ON ev.id=${Number(changeEventId || 0)}
LEFT JOIN mes_pro_edhr_batch_execution_signature sig ON sig.id=ev.request_signature_id
WHERE b.id=${Number(batchExecutionId)}
  AND b.tenant_id=122
  AND b.deleted=0;
`),
    'eDHR batch void evidence'
  )
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible input: ${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  const visibleControls = []
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (!(await item.isVisible().catch(() => false))) {
      continue
    }
    const disabled = await item.isDisabled().catch(() => true)
    visibleControls.push({
      text: (await item.innerText().catch(() => '')).trim(),
      disabled
    })
    if (!disabled) {
      await item.click()
      return
    }
  }
  throw new Error(`missing enabled control: ${label}; visibleControls=${JSON.stringify(visibleControls)}`)
}

async function login(page, target = '/index', credentials = {}) {
  const username = credentials.username || USERNAME
  const password = credentials.password || PASSWORD
  await page.context().clearCookies().catch(() => undefined)
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  }).catch(() => undefined)
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"]:visible, input.el-select__input:visible').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    await page.keyboard.press('Enter')
    await page.waitForTimeout(300)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    if (await option.isVisible().catch(() => false)) {
      await option.click()
    }
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'), username, 'username')
  await fillFirstVisible(form.locator('input[placeholder="请输入密码"]'), password, 'password')
  const loginResponse = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(form.getByRole('button', { name: /^登录$/ }), 'login')
  const payload = await (await loginResponse).json()
  assert.ok([0, 200].includes(Number(payload.code)), `login failed: ${payload.msg || payload.message || payload.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${BASE_URL}${target}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
}

function redactSensitiveRequestPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return payload
  }
  return {
    ...payload,
    ...(Object.prototype.hasOwnProperty.call(payload, 'signaturePassword')
      ? { signaturePassword: '[REDACTED]' }
      : {})
  }
}

async function unwrapBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    throw new Error(`${label} wait failed: ${result.__error.message}`)
  }
  return result
}

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      result[key] = result[key] || sessionStorage.getItem(key)
    }
    return result
  })
  const unwrap = (raw) => {
    if (!raw) return ''
    let current = raw
    for (let index = 0; index < 6; index += 1) {
      try {
        current = JSON.parse(current)
      } catch {
        break
      }
      if (current && typeof current === 'object') {
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) current = current.accessToken
        else if (Object.prototype.hasOwnProperty.call(current, 'v')) current = current.v
        else if (Object.prototype.hasOwnProperty.call(current, 'value')) current = current.value
      }
      if (typeof current !== 'string') break
    }
    return String(current || '').replace(/^"|"$/g, '')
  }
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

async function authenticatedRequest(page, method, endpoint, options, label, expectSuccess = true) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, `${label} requires browser login token`)
  assert.equal(String(tenantId), EXPECTED_TENANT_ID, `${label} tenant-id mismatch: ${tenantId}`)
  const response = await page.request[method](`${BACKEND_URL}${endpoint}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    ...options
  })
  assert.equal(response.status(), 200, `${label} HTTP status must be 200`)
  const body = await response.json()
  return expectSuccess ? assertBusinessSuccess(body, label) : assertBusinessFailure(body, label)
}

async function queryMyTodo(page, batchCode, status = 'TODO') {
  return authenticatedRequest(
    page,
    'get',
    '/admin-api/mes/pro/edhr-work-task/my-page',
    {
      params: {
        pageNo: 1,
        pageSize: 20,
        status,
        batchCode
      }
    },
    `work-task my-page ${status}`
  )
}

async function queryStats(page) {
  return authenticatedRequest(
    page,
    'get',
    '/admin-api/mes/pro/edhr-work-task/stats',
    {},
    'work-task stats'
  )
}

async function queryApprovalTodoByProcess(page, evidence) {
  assert.ok(evidence.processInstanceId, `approval todo query requires process instance: ${JSON.stringify(evidence)}`)
  const pageData = await authenticatedRequest(
    page,
    'get',
    '/admin-api/approval-center/tasks/page',
    {
      params: {
        pageNo: 1,
        pageSize: 200,
        viewType: 'TODO',
        moduleCode: 'BPM'
      }
    },
    'batch void approval todo query'
  )
  const list = Array.isArray(pageData?.list) ? pageData.list : []
  const matchedIndex = list.findIndex((item) =>
    item?.moduleCode === 'BPM' &&
    item?.sourceTaskType === 'BPM_TASK_TODO' &&
    String(item?.processInstanceId) === String(evidence.processInstanceId) &&
    item?.businessStatus === 'TODO' &&
    Array.isArray(item?.availableActions) &&
    item.availableActions.includes('APPROVE') &&
    item.availableActions.includes('REJECT')
  )
  const matched = matchedIndex >= 0 ? list[matchedIndex] : undefined
  assert.ok(matched, `approval todo must include void process=${evidence.processInstanceId}, list=${JSON.stringify(list.slice(0, 10))}`)
  return { ...matched, matchedIndex }
}

function hasTemplatePlaceholders(value) {
  return /\$\{[^}]+}/.test(String(value || ''))
}

async function findApprovalRowByTask(page, approvalTask, setup) {
  const rows = page.locator('.approval-center__table .el-table__body-wrapper .el-table__row')
  await rows.first().waitFor({ state: 'visible', timeout: 60000 })
  const rowTexts = await rows.evaluateAll((items) => items.map((item) => item.innerText || ''))
  const title = String(approvalTask?.businessTitle || '').trim()
  const businessCode = String(approvalTask?.businessCode || '').trim()
  const businessKey = String(approvalTask?.businessKey || '').trim()
  const sourceTaskId = String(approvalTask?.sourceTaskId || '').trim()
  const candidateRules = []
  if (title && !hasTemplatePlaceholders(title)) {
    candidateRules.push({ label: `businessTitle=${title}`, match: (text) => text.includes(title) })
  }
  if (businessCode) {
    candidateRules.push({ label: `businessCode=${businessCode}`, match: (text) => text.includes(businessCode) })
  }
  if (businessKey) {
    candidateRules.push({ label: `businessKey=${businessKey}`, match: (text) => text.includes(businessKey) })
  }
  if (sourceTaskId) {
    candidateRules.push({ label: `sourceTaskId=${sourceTaskId}`, match: (text) => text.includes(sourceTaskId) })
  }
  candidateRules.push({ label: `batchExecutionCode=${setup.batchExecutionCode}`, match: (text) => text.includes(setup.batchExecutionCode) })
  candidateRules.push({ label: `runKey=${setup.runKey}`, match: (text) => text.includes(setup.runKey) })
  for (const rule of candidateRules) {
    const matchedIndexes = rowTexts
      .map((text, index) => ({ text, index }))
      .filter(({ text }) => rule.match(text))
      .map(({ index }) => index)
    if (matchedIndexes.length === 1) {
      return rows.nth(matchedIndexes[0])
    }
  }
  if (
    Number.isInteger(approvalTask?.matchedIndex) &&
    approvalTask.matchedIndex >= 0 &&
    approvalTask.matchedIndex < rowTexts.length
  ) {
    return rows.nth(approvalTask.matchedIndex)
  }
  throw new Error(
    `unable to locate approval row: processInstanceId=${approvalTask?.processInstanceId}, title=${title}, rowTexts=${JSON.stringify(rowTexts)}`
  )
}

async function completeVoidApprovalFromTodo(page, setup, submittedEvidence) {
  const approverCredentials = resolveApproverCredentials(submittedEvidence)
  await login(page, APPROVAL_ROUTE, {
    username: approverCredentials.username,
    password: approverCredentials.password
  })
  const approvalTask = await queryApprovalTodoByProcess(page, submittedEvidence)
  await page.goto(`${BASE_URL}${APPROVAL_ROUTE}?moduleCode=BPM`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  ).catch(() => undefined)
  await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
  const approvalRow = await findApprovalRowByTask(page, approvalTask, setup)
  const reviewResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/review') &&
      response.request().method() === 'POST',
    { timeout: 120000 }
  ).then(async (response) => {
    let requestPayload
    try {
      requestPayload = response.request().postDataJSON()
    } catch {
      requestPayload = undefined
    }
    assert.equal(response.status(), 200, 'approval center review HTTP status must be 200')
    return {
      data: assertBusinessSuccess(await response.json(), 'batch void approval review'),
      requestPayload: redactSensitiveRequestPayload(requestPayload)
    }
  }).catch((error) => ({ __error: error }))
  await clickFirstEnabled(approvalRow.getByRole('button', { name: /^审核$/ }), 'approval center review')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '审核确认' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText('审核通过', { exact: true }).click()
  await fillFirstVisible(dialog.locator('input[type="password"]'), approverCredentials.signaturePassword, 'approval signature password')
  await clickFirstEnabled(dialog.getByRole('button', { name: /确认审核/ }), 'confirm approval')
  const reviewEnvelope = await unwrapBusinessWait(reviewResponse, 'batch void approval review')
  assert.equal(
    String(reviewEnvelope.requestPayload?.processInstanceId),
    String(submittedEvidence.processInstanceId),
    `approval request must lock this process: ${JSON.stringify(reviewEnvelope.requestPayload)}`
  )
  assert.equal(reviewEnvelope.data, true, `approval center review must return true: ${JSON.stringify(reviewEnvelope.data)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
  return {
    approverUsername: approverCredentials.username,
    approverSource: approverCredentials.source,
    approverUserId: approverCredentials.assigneeUserId,
    approvalTask: redactSensitiveRequestPayload(approvalTask),
    reviewResult: reviewEnvelope.data,
    requestPayload: reviewEnvelope.requestPayload
  }
}

async function openOldTask(page, setup) {
  return authenticatedRequest(
    page,
    'post',
    '/admin-api/mes/pro/edhr-batch-execution/task/open',
    {
      data: {
        batchExecutionId: setup.batchExecutionId,
        taskId: setup.batchTaskId,
        workTaskId: setup.todoWorkTaskId
      }
    },
    'old work task open after void',
    false
  )
}

async function submitVoidThroughUi(page, setup) {
  const listResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution?batchExecutionCode=${encodeURIComponent(setup.batchExecutionCode)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const listData = assertBusinessSuccess(await (await listResponsePromise).json(), 'eDHR batch execution list')
  assert.ok(
    (listData.list || []).some((row) => String(row.id) === String(setup.batchExecutionId)),
    `target batch must be visible in list response: ${JSON.stringify(listData.list || [])}`
  )
  const row = page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: setup.batchExecutionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const resolutionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-change/void-batch-execution/approval-resolution') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(row.getByRole('button', { name: '作废' }), 'void row action')

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '作废批次执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText(setup.batchExecutionCode, { exact: false }).waitFor({ state: 'visible', timeout: 30000 })
  const reasonItem = dialog.locator('.el-form-item').filter({ hasText: '原因分类' }).first()
  await reasonItem.locator('.el-select, .el-input').first().click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '其他' }).first().click()
  const reasonText = `批次作废取消工作台任务验证 ${RUN_ID}`
  await dialog
    .locator('.el-form-item')
    .filter({ hasText: '原因说明' })
    .first()
    .locator('textarea')
    .first()
    .fill(reasonText)
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="电子签名密码"]'), SIGNATURE_PASSWORD, 'void signature password')
  const comment = dialog.locator('.el-form-item').filter({ hasText: '备注' }).first().locator('textarea').first()
  if (await comment.isVisible().catch(() => false)) {
    await comment.fill(`batch void work-task closure ${RUN_ID}`)
  }

  const requestResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-change/void-batch-execution/request') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /提交作废流程/ }), 'submit void form')
  const [resolutionResponse, requestResponse] = await Promise.all([resolutionResponsePromise, requestResponsePromise])
  assert.equal(resolutionResponse.status(), 200, 'resolve batch void approval HTTP status must be 200')
  assert.equal(requestResponse.status(), 200, 'request batch void HTTP status must be 200')
  const resolution = assertBusinessSuccess(await resolutionResponse.json(), 'resolve batch void approval')
  const changeEvent = assertBusinessSuccess(await requestResponse.json(), 'request batch void')
  assert.ok(changeEvent.id, `void request must return change event id: ${JSON.stringify(changeEvent)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
  return { resolution, changeEvent, reasonText }
}

async function verifyWorkbenchPageExcludesTask(page, setup) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-work-task`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-work-task/my-page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  ).catch(() => undefined)
  const batchInput = page.locator('.edhr-work-task-page__toolbar .el-form-item').filter({ hasText: '批次' }).locator('input').first()
  await batchInput.fill(setup.batchCode)
  const filteredResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-work-task/my-page') &&
      response.url().includes(encodeURIComponent(setup.batchCode)) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickFirstEnabled(page.locator('.edhr-work-task-page__toolbar').getByRole('button', { name: '查询' }), 'query work task board')
  const filteredData = assertBusinessSuccess(await (await filteredResponse).json(), 'filtered work-task page')
  assert.equal(Number(filteredData.total || 0), 0, `filtered workbench API must not include voided batch task: ${JSON.stringify(filteredData)}`)
  await page.getByText('暂无工作任务', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await assertNoVisibleTaskCode(page, `WT-${setup.runKey}-TODO`)
}

async function assertNoVisibleTaskCode(page, taskCode) {
  const rows = page.locator('.edhr-work-task-page .el-table__body-wrapper .el-table__row')
  const texts = await rows.evaluateAll((items) => items.map((item) => item.innerText || ''))
  assert.ok(!texts.some((text) => text.includes(taskCode)), `workbench page still shows ${taskCode}: ${JSON.stringify(texts)}`)
}

function assertTerminalEvidence(evidence, reasonText) {
  assert.equal(Number(evidence.batchStatus), 60, `void must make batch VOIDED: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.activeContextKey, null, `void must clear active context key: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.changeStatus, 'EFFECTIVE', `void change must be effective: ${JSON.stringify(evidence)}`)
  assert.ok(evidence.requestSignatureId, `void must write request signature: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.requestSignatureActionType, 'BATCH_VOID_REQUEST', `signature action mismatch: ${JSON.stringify(evidence)}`)
  assert.equal(Number(evidence.activeTaskCount), 0, `active work tasks must be canceled: ${JSON.stringify(evidence)}`)
  assert.equal(Number(evidence.canceledTaskCount), 4, `three active plus one existing canceled task must be CANCELED: ${JSON.stringify(evidence)}`)
  assert.equal(Number(evidence.doneTaskCount), 1, `DONE history must remain DONE: ${JSON.stringify(evidence)}`)
  assert.equal(Number(evidence.canceledReasonPrefixCount), 3, `newly canceled active tasks need void reason prefix: ${JSON.stringify(evidence)}`)
  assert.ok(
    JSON.stringify(evidence.workTasks || []).includes(`批次已作废：${reasonText}`),
    `canceled tasks must include deterministic reason: ${JSON.stringify(evidence)}`
  )
}

function writeArtifact(payload) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  const artifactPath = path.join(ARTIFACT_DIR, `edhr-batch-void-work-task-${RUN_ID}.json`)
  fs.writeFileSync(artifactPath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return artifactPath
}

async function main() {
  assertRuntimeUrls()
  const setup = prepareVoidableBatchWithActiveTasks()
  const browser = await chromium.launch({
    headless: process.env.EDHR_BATCH_VOID_E2E_HEADED !== '1',
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const pageErrors = []
  const observedResponses = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('response', (response) => {
    const url = response.url()
    if (
      url.includes('/mes/pro/edhr-change/') ||
      url.includes('/mes/pro/edhr-work-task/') ||
      url.includes('/mes/pro/edhr-batch-execution/') ||
      url.includes('/approval-center/')
    ) {
      observedResponses.push({ method: response.request().method(), status: response.status(), url })
    }
  })

  try {
    await login(page, `/mes/pro/feedback/edhr-batch-execution?batchExecutionCode=${encodeURIComponent(setup.batchExecutionCode)}`)
    const beforeTodo = await queryMyTodo(page, setup.batchCode, 'TODO')
    const beforeOverdue = await queryMyTodo(page, setup.batchCode, 'OVERDUE')
    const beforeStats = await queryStats(page)
    assert.ok(
      (beforeTodo.list || []).some((row) => String(row.id) === String(setup.todoWorkTaskId)),
      `precondition TODO task must be visible before void: ${JSON.stringify(beforeTodo)}`
    )
    assert.ok(
      (beforeOverdue.list || []).some((row) => String(row.id) === String(setup.overdueWorkTaskId)),
      `precondition OVERDUE task must be visible before void: ${JSON.stringify(beforeOverdue)}`
    )

    const submitSummary = await submitVoidThroughUi(page, setup)
    let approvalSummary
    if (submitSummary.resolution?.policyMode === 'BPM_REQUIRED') {
      const submittedEvidence = loadVoidEvidence(setup.batchExecutionId, submitSummary.changeEvent.id)
      assert.equal(submittedEvidence.changeStatus, 'SUBMITTED', `BPM_REQUIRED void must be submitted before approval: ${JSON.stringify(submittedEvidence)}`)
      approvalSummary = await completeVoidApprovalFromTodo(page, setup, submittedEvidence)
      if (approvalSummary.approverUsername !== USERNAME) {
        await login(page, '/mes/pro/feedback/edhr-work-task')
      }
    }
    const evidence = loadVoidEvidence(setup.batchExecutionId, submitSummary.changeEvent.id)
    assertTerminalEvidence(evidence, submitSummary.reasonText)

    const afterTodo = await queryMyTodo(page, setup.batchCode, 'TODO')
    const afterOverdue = await queryMyTodo(page, setup.batchCode, 'OVERDUE')
    const afterStats = await queryStats(page)
    assert.equal(Number(afterTodo.total || 0), 0, `voided batch TODO task must leave my-page: ${JSON.stringify(afterTodo)}`)
    assert.equal(Number(afterOverdue.total || 0), 0, `voided batch OVERDUE task must leave my-page: ${JSON.stringify(afterOverdue)}`)
    assert.ok(Number(afterStats.todoCount) <= Number(beforeStats.todoCount) - 1, `todo stats must drop after void: before=${JSON.stringify(beforeStats)} after=${JSON.stringify(afterStats)}`)
    assert.ok(Number(afterStats.overdueCount) <= Number(beforeStats.overdueCount) - 1, `overdue stats must drop after void: before=${JSON.stringify(beforeStats)} after=${JSON.stringify(afterStats)}`)

    const oldOpenFailure = await openOldTask(page, setup)
    assert.match(oldOpenFailure.msg || oldOpenFailure.message || '', /状态|作废|终态|不允许/, `old link error should be terminal-state related: ${JSON.stringify(oldOpenFailure)}`)
    await verifyWorkbenchPageExcludesTask(page, setup)
    assert.deepEqual(pageErrors, [], 'eDHR batch void E2E must not produce page errors')

    const artifactPath = writeArtifact({
      status: 'PASS',
      frontendUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      tenant: TENANT,
      username: USERNAME,
      setup,
      submitSummary,
      beforeTodo,
      beforeOverdue,
      beforeStats,
      approvalSummary,
      afterTodo,
      afterOverdue,
      afterStats,
      oldOpenFailure,
      evidence,
      observedResponses,
      pageErrors
    })
    console.log(
      `PASS: eDHR batch void cancels workbench tasks batch=${setup.batchExecutionId} change=${submitSummary.changeEvent.id} artifact=${artifactPath}`
    )
  } catch (error) {
    const artifactPath = writeArtifact({
      status: 'FAIL',
      frontendUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      tenant: TENANT,
      username: USERNAME,
      setup,
      error: error.stack || error.message,
      observedResponses,
      pageErrors
    })
    console.error((error.stack || error.message) + `\nartifact=${artifactPath}`)
    process.exitCode = 1
  } finally {
    await context.close()
    await browser.close()
  }
}

main()
