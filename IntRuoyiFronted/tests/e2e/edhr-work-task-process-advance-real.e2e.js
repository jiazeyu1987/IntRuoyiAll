const assert = require('node:assert/strict')
const fs = require('node:fs')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = (process.env.EDHR_PROCESS_ADVANCE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_PROCESS_ADVANCE_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const TENANT = process.env.EDHR_PROCESS_ADVANCE_E2E_TENANT || '测试租户'
const AOTEMAN_USERNAME = process.env.EDHR_PROCESS_ADVANCE_E2E_USERNAME || 'aoteman'
const AOTEMAN_PASSWORD = process.env.EDHR_PROCESS_ADVANCE_E2E_PASSWORD || '111111'
const ADMIN_USERNAME = process.env.EDHR_PROCESS_ADVANCE_E2E_ADMIN_USERNAME || 'admin'
const ADMIN_PASSWORD = process.env.EDHR_PROCESS_ADVANCE_E2E_ADMIN_PASSWORD || '111111'
const EXECUTABLE_PATH = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || ''
const RUN_ID = (process.env.EDHR_PROCESS_ADVANCE_E2E_RUN_ID || new Date().toISOString())
  .replace(/[^A-Za-z0-9]/g, '')
  .slice(-12)
const CLEANUP = process.env.EDHR_PROCESS_ADVANCE_E2E_CLEANUP !== '0'

const TEST_TENANT_ID = 122
const AOTEMAN_USER_ID = 914520
const ADMIN_USER_ID = 912398
const ROUTE_ID = 922186
const ROUTE_VERSION_ID = 239
const ROUTE_VERSION_NO = 'V2'
const CURRENT_ROUTE_PROCESS_ID = 926146
const CURRENT_PROCESS_ID = 922789
const NEXT_ROUTE_PROCESS_ID = 926147
const NEXT_PROCESS_ID = 922795
const FORM_TEMPLATE_ID = 23
const FORM_TEMPLATE_VERSION_ID = 24
const FORM_TEMPLATE_NAME = 'Codex损耗单模板20260721041916'
const FORM_TEMPLATE_VERSION_NO = 'V2.0'

function assertLocalOnly() {
  assert.ok(
    BASE_URL === 'http://localhost:8081' || BASE_URL === 'http://127.0.0.1:8081',
    `真实 E2E 只允许本机前端 8081，当前 ${BASE_URL}`
  )
  assert.ok(
    BACKEND_URL === 'http://127.0.0.1:48081' || BACKEND_URL === 'http://localhost:48081',
    `真实 E2E 只允许本机后端 48081，当前 ${BACKEND_URL}`
  )
  assert.equal(TENANT, '测试租户', '写入型 E2E 必须使用测试租户')
  assert.equal(AOTEMAN_USERNAME, 'aoteman', '候选填写人路径必须使用测试账号 aoteman')
  assert.equal(ADMIN_USERNAME, 'admin', '过程检验填写人路径必须使用测试租户 admin')
  if (EXECUTABLE_PATH) {
    assert.ok(fs.existsSync(EXECUTABLE_PATH), `Chromium executable not found: ${EXECUTABLE_PATH}`)
  }
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
      'sh',
      '-lc',
      'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --batch --raw --skip-column-names --default-character-set=utf8mb4 ruoyi-vue-pro'
    ],
    { input: sql, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }
  ).trim()
}

function parseJsonOutput(output, label) {
  const line = output.split(/\r?\n/).find((item) => item && item.trim())
  assert.ok(line, `${label} returned empty output`)
  try {
    return JSON.parse(line)
  } catch (error) {
    throw new Error(`${label} returned non JSON output: ${line}`)
  }
}

function unwrapResponse(body, label) {
  assert.ok(body && typeof body === 'object', `${label} must return JSON`)
  assert.ok(body.code === 0 || body.code === 200, `${label} failed: ${body.msg || body.code}`)
  return body.data
}

async function assertRuntimeUp() {
  const frontend = await fetch(`${BASE_URL}/`)
  assert.ok(frontend.ok, `frontend ${BASE_URL} must be reachable: ${frontend.status}`)
  const health = await fetch(`${BACKEND_URL}/actuator/health`)
  assert.ok(health.ok, `backend health must be reachable: ${health.status}`)
  const body = await health.json()
  assert.equal(body.status, 'UP', `backend health must be UP: ${JSON.stringify(body)}`)
}

function cleanupRun(runKey) {
  mysql(`
SET NAMES utf8mb4;
SET @run_key := ${sqlString(runKey)};
UPDATE bpm_form_effect_execution
   SET deleted=b'1', updater='codex-e2e-cleanup'
 WHERE tenant_id=${TEST_TENANT_ID}
   AND instance_id IN (
     SELECT id FROM bpm_form_action_instance
      WHERE tenant_id=${TEST_TENANT_ID}
        AND instance_code LIKE (CONCAT('FAI-', @run_key, '%') COLLATE utf8mb4_unicode_ci)
   );
UPDATE bpm_form_action_snapshot
   SET deleted=b'1', updater='codex-e2e-cleanup'
 WHERE tenant_id=${TEST_TENANT_ID}
   AND instance_id IN (
     SELECT id FROM bpm_form_action_instance
      WHERE tenant_id=${TEST_TENANT_ID}
        AND instance_code LIKE (CONCAT('FAI-', @run_key, '%') COLLATE utf8mb4_unicode_ci)
   );
UPDATE bpm_form_action_instance
   SET deleted=b'1', updater='codex-e2e-cleanup'
 WHERE tenant_id=${TEST_TENANT_ID}
   AND instance_code LIKE (CONCAT('FAI-', @run_key, '%') COLLATE utf8mb4_unicode_ci);
UPDATE bpm_business_approval_policy
   SET deleted=b'1', updater='codex-e2e-cleanup'
 WHERE tenant_id=${TEST_TENANT_ID}
   AND remark LIKE (CONCAT('%', @run_key, '%') COLLATE utf8mb4_unicode_ci);
UPDATE mes_pro_edhr_work_task
   SET deleted=b'1', updater='codex-e2e-cleanup'
 WHERE tenant_id=${TEST_TENANT_ID}
   AND batch_code LIKE (CONCAT(@run_key, '%') COLLATE utf8mb4_0900_ai_ci);
UPDATE mes_pro_edhr_batch_execution_task
   SET deleted=b'1', updater='codex-e2e-cleanup'
 WHERE tenant_id=${TEST_TENANT_ID}
   AND batch_execution_id IN (
     SELECT id FROM mes_pro_edhr_batch_execution
      WHERE tenant_id=${TEST_TENANT_ID}
        AND batch_code LIKE (CONCAT(@run_key, '%') COLLATE utf8mb4_0900_ai_ci)
   );
UPDATE mes_pro_edhr_batch_execution
    SET deleted=b'1', updater='codex-e2e-cleanup'
  WHERE tenant_id=${TEST_TENANT_ID}
    AND batch_code LIKE (CONCAT(@run_key, '%') COLLATE utf8mb4_0900_ai_ci);
UPDATE mes_pro_work_order
   SET deleted=b'1', updater='codex-e2e-cleanup'
 WHERE tenant_id=${TEST_TENANT_ID}
   AND batch_code LIKE (CONCAT(@run_key, '%') COLLATE utf8mb4_unicode_ci);
UPDATE mes_pro_edhr_process_form_permission_rule
    SET deleted=b'1', updater='codex-e2e-cleanup'
  WHERE tenant_id=${TEST_TENANT_ID}
    AND remark LIKE (CONCAT('%', @run_key, '%') COLLATE utf8mb4_unicode_ci);
`)
}

function buildBusinessContextJson(taskId, actionCode, formBindingKey) {
  return `JSON_OBJECT(
    'dataDomain', 'MES',
    'systemCode', 'MES',
    'objectType', 'EDHR_ROUTE_FORM',
    'objectId', CAST(${taskId} AS CHAR),
    'objectVersion', CAST(${ROUTE_VERSION_ID} AS CHAR),
    'actionCode', ${actionCode},
    'objectState', 'ACTIVE',
    'orgCode', '',
    'deptCode', '',
    'roleCodes', JSON_ARRAY(),
    'productCode', 'E2E-PRODUCT',
    'categoryCode', '',
    'reason', 'eDHR route form fill',
    'formBindingKey', ${formBindingKey}
  )`
}

function prepareNoInspectionScenario(runKey) {
  const currentBinding = `${RUN_ID}A`
  const nextBinding = `${RUN_ID}AN`
  const actionCode = `EDHR_RF_${ROUTE_VERSION_ID}_${currentBinding}`
  const batchCode = `${runKey}-NOIPQC`
  const output = mysql(`
SET NAMES utf8mb4;
SET @run_key := ${sqlString(runKey)};
SET @batch_code := ${sqlString(batchCode)};
SET @current_binding := ${sqlString(currentBinding)};
SET @next_binding := ${sqlString(nextBinding)};
SET @action_code := ${sqlString(actionCode)};
SET @work_order_code := CONCAT('WO-', @batch_code);
INSERT INTO mes_pro_work_order (
  code, name, type, order_source_type, order_source_code, product_id,
  quantity, quantity_produced, quantity_changed, quantity_scheduled,
  batch_code, request_date, parent_id, status, temporary_frozen,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  @work_order_code, CONCAT('eDHR推进E2E工单-', @batch_code), 1, 2, CONCAT('SRC-', @batch_code), 9002001,
  1, 0, 0, 0,
  @batch_code, NOW(), 0, 1, b'0',
  CONCAT('eDHR process advance E2E ', @run_key, ' work order no inspection'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @work_order_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_batch_execution (
  batch_execution_code, work_order_id, work_order_code, batch_code, active_context_key,
  attempt_no, product_id, product_code, product_name, route_id, route_version_id, route_version_no,
  route_code, route_name, status, task_total, task_approved_count, blocked_count,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT('BE-', @batch_code), @work_order_id, @work_order_code, @batch_code, CONCAT('CTX-', @batch_code),
  1, 9002001, 'E2E-PRODUCT', 'eDHR推进E2E产品', ${ROUTE_ID}, ${ROUTE_VERSION_ID}, ${sqlString(ROUTE_VERSION_NO)},
  'E2E-ROUTE', 'eDHR推进E2E路线', 10, 2, 0, 0,
  CONCAT('eDHR process advance E2E ', @run_key, ' no inspection'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @batch_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, root_process_flag, route_process_sort,
  process_id, process_code, process_name, batch_record_report_id, batch_record_report_name,
  form_slot_type, form_binding_key, form_template_id, form_template_name_snapshot,
  form_template_version_id, form_template_version_no, batch_record_sort, execution_mode,
  record_category, validation_profile, recordbook_enabled, required_policy, owner_role_key,
  archive_visibility, batch_record_version_id, status, required_flag, creator, updater, deleted, tenant_id
) VALUES (
  @batch_id, 'ROUTE_FORM', ${CURRENT_ROUTE_PROCESS_ID}, b'1', 1,
  ${CURRENT_PROCESS_ID}, 'Z2630', '吹球囊成型-无过程检验推进E2E', NULL, '无过程检验动态表单',
  'MAIN', @current_binding, ${FORM_TEMPLATE_ID}, ${sqlString(FORM_TEMPLATE_NAME)},
  ${FORM_TEMPLATE_VERSION_ID}, ${sqlString(FORM_TEMPLATE_VERSION_NO)}, 0, 'SEQUENTIAL',
  'BATCH_RECORD', 'CONTROLLED_BATCH', b'1', 'REQUIRED', 'PRODUCTION',
  'DOSSIER', ${ROUTE_VERSION_ID}, 0, b'1', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @current_task_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, root_process_flag, route_process_sort,
  process_id, process_code, process_name, batch_record_report_id, batch_record_report_name,
  form_slot_type, form_binding_key, form_template_id, form_template_name_snapshot,
  form_template_version_id, form_template_version_no, batch_record_sort, execution_mode,
  record_category, validation_profile, recordbook_enabled, required_policy, owner_role_key,
  archive_visibility, batch_record_version_id, status, required_flag, creator, updater, deleted, tenant_id
) VALUES (
  @batch_id, 'ROUTE_FORM', ${NEXT_ROUTE_PROCESS_ID}, b'0', 2,
  ${NEXT_PROCESS_ID}, 'Z3710', '球囊裁剪-无过程检验推进E2E', NULL, '下一工序动态表单',
  'MAIN', @next_binding, ${FORM_TEMPLATE_ID}, ${sqlString(FORM_TEMPLATE_NAME)},
  ${FORM_TEMPLATE_VERSION_ID}, ${sqlString(FORM_TEMPLATE_VERSION_NO)}, 0, 'SEQUENTIAL',
  'BATCH_RECORD', 'CONTROLLED_BATCH', b'1', 'REQUIRED', 'PRODUCTION',
  'DOSSIER', ${ROUTE_VERSION_ID}, 0, b'1', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @next_task_id := LAST_INSERT_ID();
INSERT INTO bpm_business_approval_policy (
  tenant_id, data_domain, system_code, object_type, action_code, object_state,
  policy_mode, process_definition_key, effect_executor_code, form_policy_type,
  form_slots_json, status, remark, creator, updater, deleted
) VALUES (
  ${TEST_TENANT_ID}, 'MES', 'MES', 'EDHR_ROUTE_FORM', @action_code, 'ACTIVE',
  'DIRECT', NULL, 'MES_EDHR_ROUTE_FORM_FILL', NULL,
  NULL, 'PUBLISHED', CONCAT('eDHR process advance E2E ', @run_key, ' current no inspection'), 'codex-e2e', 'codex-e2e', b'0'
);
SET @policy_id := LAST_INSERT_ID();
INSERT INTO bpm_form_action_instance (
  instance_code, tenant_id, policy_id, applicant_user_id, status, data_domain, system_code,
  object_type, action_code, object_state, object_id, object_version, idempotency_key,
  business_context_json, form_data_json, creator, updater, deleted
) VALUES (
  CONCAT('FAI-', @run_key, '-A'), ${TEST_TENANT_ID}, @policy_id, ${AOTEMAN_USER_ID}, 'DRAFT', 'MES', 'MES',
  'EDHR_ROUTE_FORM', @action_code, 'ACTIVE', CAST(@current_task_id AS CHAR), CAST(${ROUTE_VERSION_ID} AS CHAR),
  CONCAT('EDHR_ROUTE_FORM:', @batch_id, ':', @current_task_id, ':', @current_binding),
  ${buildBusinessContextJson('@current_task_id', '@action_code', '@current_binding')},
  JSON_OBJECT('batchCode', @batch_code, 'case', 'NO_INSPECTION'), 'codex-e2e', 'codex-e2e', b'0'
);
SET @instance_id := LAST_INSERT_ID();
UPDATE mes_pro_edhr_batch_execution_task
   SET form_center_instance_id=@instance_id
 WHERE id=@current_task_id;
INSERT INTO mes_pro_edhr_process_form_permission_rule (
  route_process_id, batch_record_report_id, batch_record_version_id, rule_type,
  signature_cell_key, candidate_source_type, candidate_source_ids, completion_policy,
  due_minutes, enabled, remark, creator, updater, deleted, tenant_id
) VALUES (
  ${NEXT_ROUTE_PROCESS_ID}, @next_binding, ${ROUTE_VERSION_ID}, 'FILL',
  '', 'USERS', CAST(${AOTEMAN_USER_ID} AS CHAR), 'ANY_ONE',
  180, b'1', CONCAT('eDHR process advance E2E ', @run_key, ' next no inspection'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id, process_name,
  assignee_user_id, candidate_source_type, candidate_user_snapshot, status, due_time, action_url,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT('WT-', @batch_code), 'FILL', @batch_id, @current_task_id, 'BATCH_TASK', @current_task_id,
  @work_order_id, @work_order_code, @batch_code, ${ROUTE_ID}, ${CURRENT_ROUTE_PROCESS_ID}, ${CURRENT_PROCESS_ID}, '吹球囊成型-无过程检验推进E2E',
  ${ADMIN_USER_ID}, 'USERS', CONCAT(${ADMIN_USER_ID}, ',', ${AOTEMAN_USER_ID}), 'TODO', DATE_ADD(NOW(), INTERVAL 1 DAY),
  CONCAT('/mes/pro/feedback/edhr-batch-execution/detail?id=', @batch_id, '&batchExecutionId=', @batch_id, '&batchTaskId=', @current_task_id),
  'candidate non-assignee can fill and advance without process inspection', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @work_task_id := LAST_INSERT_ID();
UPDATE mes_pro_edhr_work_task
   SET action_url=CONCAT(action_url, '&workTaskId=', @work_task_id)
 WHERE id=@work_task_id;
SELECT JSON_OBJECT(
  'caseName', 'noInspection',
  'batchId', @batch_id,
  'batchCode', @batch_code,
  'currentTaskId', @current_task_id,
  'nextTaskId', @next_task_id,
  'workTaskId', @work_task_id,
  'workOrderId', @work_order_id,
  'instanceId', @instance_id,
  'formBindingKey', @current_binding,
  'expectedNextFill', true,
  'actorUserId', ${AOTEMAN_USER_ID}
);
`)
  return parseJsonOutput(output, 'prepareNoInspectionScenario')
}

function prepareMainBlockedScenario(runKey) {
  const mainBinding = `${RUN_ID}B`
  const inspectionBinding = `${RUN_ID}BI`
  const nextBinding = `${RUN_ID}BN`
  const actionCode = `EDHR_RF_${ROUTE_VERSION_ID}_${mainBinding}`
  const batchCode = `${runKey}-MAINBLOCK`
  const output = mysql(`
SET NAMES utf8mb4;
SET @run_key := ${sqlString(runKey)};
SET @batch_code := ${sqlString(batchCode)};
SET @main_binding := ${sqlString(mainBinding)};
SET @inspection_binding := ${sqlString(inspectionBinding)};
SET @next_binding := ${sqlString(nextBinding)};
SET @action_code := ${sqlString(actionCode)};
SET @work_order_code := CONCAT('WO-', @batch_code);
INSERT INTO mes_pro_work_order (
  code, name, type, order_source_type, order_source_code, product_id,
  quantity, quantity_produced, quantity_changed, quantity_scheduled,
  batch_code, request_date, parent_id, status, temporary_frozen,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  @work_order_code, CONCAT('eDHR推进E2E工单-', @batch_code), 1, 2, CONCAT('SRC-', @batch_code), 9002001,
  1, 0, 0, 0,
  @batch_code, NOW(), 0, 1, b'0',
  CONCAT('eDHR process advance E2E ', @run_key, ' work order main blocked'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @work_order_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_batch_execution (
  batch_execution_code, work_order_id, work_order_code, batch_code, active_context_key,
  attempt_no, product_id, product_code, product_name, route_id, route_version_id, route_version_no,
  route_code, route_name, status, task_total, task_approved_count, blocked_count,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT('BE-', @batch_code), @work_order_id, @work_order_code, @batch_code, CONCAT('CTX-', @batch_code),
  1, 9002001, 'E2E-PRODUCT', 'eDHR推进E2E产品', ${ROUTE_ID}, ${ROUTE_VERSION_ID}, ${sqlString(ROUTE_VERSION_NO)},
  'E2E-ROUTE', 'eDHR推进E2E路线', 10, 3, 1, 0,
  CONCAT('eDHR process advance E2E ', @run_key, ' main blocked by inspection filler'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @batch_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, root_process_flag, route_process_sort,
  process_id, process_code, process_name, batch_record_report_id, batch_record_report_name,
  form_slot_type, form_binding_key, form_template_id, form_template_name_snapshot,
  form_template_version_id, form_template_version_no, batch_record_sort, execution_mode,
  record_category, validation_profile, recordbook_enabled, required_policy, owner_role_key,
  archive_visibility, batch_record_version_id, status, required_flag, creator, updater, deleted, tenant_id
) VALUES
(
  @batch_id, 'ROUTE_FORM', ${CURRENT_ROUTE_PROCESS_ID}, b'1', 1,
  ${CURRENT_PROCESS_ID}, 'Z2630', '吹球囊成型-主表非推进E2E', NULL, '主表动态表单',
  'MAIN', @main_binding, ${FORM_TEMPLATE_ID}, ${sqlString(FORM_TEMPLATE_NAME)},
  ${FORM_TEMPLATE_VERSION_ID}, ${sqlString(FORM_TEMPLATE_VERSION_NO)}, 0, 'SEQUENTIAL',
  'BATCH_RECORD', 'CONTROLLED_BATCH', b'1', 'REQUIRED', 'PRODUCTION',
  'DOSSIER', ${ROUTE_VERSION_ID}, 0, b'1', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
),
(
  @batch_id, 'ROUTE_FORM', ${CURRENT_ROUTE_PROCESS_ID}, b'0', 1,
  ${CURRENT_PROCESS_ID}, 'Z2630', '吹球囊成型-过程检验已完成E2E', NULL, '过程检验动态表单',
  'PROCESS_INSPECTION', @inspection_binding, ${FORM_TEMPLATE_ID}, ${sqlString(FORM_TEMPLATE_NAME)},
  ${FORM_TEMPLATE_VERSION_ID}, ${sqlString(FORM_TEMPLATE_VERSION_NO)}, 1, 'SEQUENTIAL',
  'BATCH_RECORD', 'CONTROLLED_BATCH', b'1', 'REQUIRED', 'QUALITY',
  'DOSSIER', ${ROUTE_VERSION_ID}, 40, b'1', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @first_current_task_id := LAST_INSERT_ID();
SET @main_task_id := @first_current_task_id;
SET @inspection_task_id := @first_current_task_id + 1;
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, root_process_flag, route_process_sort,
  process_id, process_code, process_name, batch_record_report_id, batch_record_report_name,
  form_slot_type, form_binding_key, form_template_id, form_template_name_snapshot,
  form_template_version_id, form_template_version_no, batch_record_sort, execution_mode,
  record_category, validation_profile, recordbook_enabled, required_policy, owner_role_key,
  archive_visibility, batch_record_version_id, status, required_flag, creator, updater, deleted, tenant_id
) VALUES (
  @batch_id, 'ROUTE_FORM', ${NEXT_ROUTE_PROCESS_ID}, b'0', 2,
  ${NEXT_PROCESS_ID}, 'Z3710', '球囊裁剪-主表非推进E2E', NULL, '下一工序动态表单',
  'MAIN', @next_binding, ${FORM_TEMPLATE_ID}, ${sqlString(FORM_TEMPLATE_NAME)},
  ${FORM_TEMPLATE_VERSION_ID}, ${sqlString(FORM_TEMPLATE_VERSION_NO)}, 0, 'SEQUENTIAL',
  'BATCH_RECORD', 'CONTROLLED_BATCH', b'1', 'REQUIRED', 'PRODUCTION',
  'DOSSIER', ${ROUTE_VERSION_ID}, 0, b'1', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @next_task_id := LAST_INSERT_ID();
INSERT INTO bpm_business_approval_policy (
  tenant_id, data_domain, system_code, object_type, action_code, object_state,
  policy_mode, process_definition_key, effect_executor_code, form_policy_type,
  form_slots_json, status, remark, creator, updater, deleted
) VALUES (
  ${TEST_TENANT_ID}, 'MES', 'MES', 'EDHR_ROUTE_FORM', @action_code, 'ACTIVE',
  'DIRECT', NULL, 'MES_EDHR_ROUTE_FORM_FILL', NULL,
  NULL, 'PUBLISHED', CONCAT('eDHR process advance E2E ', @run_key, ' main blocked'), 'codex-e2e', 'codex-e2e', b'0'
);
SET @policy_id := LAST_INSERT_ID();
INSERT INTO bpm_form_action_instance (
  instance_code, tenant_id, policy_id, applicant_user_id, status, data_domain, system_code,
  object_type, action_code, object_state, object_id, object_version, idempotency_key,
  business_context_json, form_data_json, creator, updater, deleted
) VALUES (
  CONCAT('FAI-', @run_key, '-B'), ${TEST_TENANT_ID}, @policy_id, ${AOTEMAN_USER_ID}, 'DRAFT', 'MES', 'MES',
  'EDHR_ROUTE_FORM', @action_code, 'ACTIVE', CAST(@main_task_id AS CHAR), CAST(${ROUTE_VERSION_ID} AS CHAR),
  CONCAT('EDHR_ROUTE_FORM:', @batch_id, ':', @main_task_id, ':', @main_binding),
  ${buildBusinessContextJson('@main_task_id', '@action_code', '@main_binding')},
  JSON_OBJECT('batchCode', @batch_code, 'case', 'MAIN_BLOCKED'), 'codex-e2e', 'codex-e2e', b'0'
);
SET @instance_id := LAST_INSERT_ID();
UPDATE mes_pro_edhr_batch_execution_task
   SET form_center_instance_id=@instance_id
 WHERE id=@main_task_id;
INSERT INTO mes_pro_edhr_process_form_permission_rule (
  route_process_id, batch_record_report_id, batch_record_version_id, rule_type,
  signature_cell_key, candidate_source_type, candidate_source_ids, completion_policy,
  due_minutes, enabled, remark, creator, updater, deleted, tenant_id
) VALUES (
  ${NEXT_ROUTE_PROCESS_ID}, @next_binding, ${ROUTE_VERSION_ID}, 'FILL',
  '', 'USERS', CAST(${AOTEMAN_USER_ID} AS CHAR), 'ANY_ONE',
  180, b'1', CONCAT('eDHR process advance E2E ', @run_key, ' next main blocked'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id, process_name,
  assignee_user_id, candidate_source_type, candidate_user_snapshot, status, due_time, action_url,
  remark, creator, updater, deleted, tenant_id
) VALUES
(
  CONCAT('WT-', @batch_code, '-MAIN'), 'FILL', @batch_id, @main_task_id, 'BATCH_TASK', @main_task_id,
  @work_order_id, @work_order_code, @batch_code, ${ROUTE_ID}, ${CURRENT_ROUTE_PROCESS_ID}, ${CURRENT_PROCESS_ID}, '吹球囊成型-主表非推进E2E',
  ${ADMIN_USER_ID}, 'USERS', CONCAT(${ADMIN_USER_ID}, ',', ${AOTEMAN_USER_ID}), 'TODO', DATE_ADD(NOW(), INTERVAL 1 DAY),
  CONCAT('/mes/pro/feedback/edhr-batch-execution/detail?id=', @batch_id, '&batchExecutionId=', @batch_id, '&batchTaskId=', @main_task_id),
  'candidate non-assignee can fill but must not advance while inspection filler exists', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
),
(
  CONCAT('WT-', @batch_code, '-IPQC'), 'FILL', @batch_id, @inspection_task_id, 'BATCH_TASK', @inspection_task_id,
  @work_order_id, @work_order_code, @batch_code, ${ROUTE_ID}, ${CURRENT_ROUTE_PROCESS_ID}, ${CURRENT_PROCESS_ID}, '吹球囊成型-过程检验已完成E2E',
  ${ADMIN_USER_ID}, 'USERS', CAST(${ADMIN_USER_ID} AS CHAR), 'DONE', DATE_ADD(NOW(), INTERVAL 1 DAY),
  CONCAT('/mes/pro/feedback/edhr-batch-execution/detail?id=', @batch_id, '&batchExecutionId=', @batch_id, '&batchTaskId=', @inspection_task_id),
  'completed inspection filler defines advance actor set', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @first_work_task_id := LAST_INSERT_ID();
SET @main_work_task_id := @first_work_task_id;
SET @inspection_work_task_id := @first_work_task_id + 1;
UPDATE mes_pro_edhr_work_task
   SET action_url=CONCAT(action_url, '&workTaskId=', id)
 WHERE id IN (@main_work_task_id, @inspection_work_task_id);
SELECT JSON_OBJECT(
  'caseName', 'mainBlockedByInspection',
  'batchId', @batch_id,
  'batchCode', @batch_code,
  'currentTaskId', @main_task_id,
  'inspectionTaskId', @inspection_task_id,
  'nextTaskId', @next_task_id,
  'workTaskId', @main_work_task_id,
  'inspectionWorkTaskId', @inspection_work_task_id,
  'workOrderId', @work_order_id,
  'instanceId', @instance_id,
  'formBindingKey', @main_binding,
  'expectedNextFill', false,
  'actorUserId', ${AOTEMAN_USER_ID}
);
`)
  return parseJsonOutput(output, 'prepareMainBlockedScenario')
}

function prepareInspectionAdvanceScenario(runKey) {
  const mainBinding = `${RUN_ID}C`
  const inspectionBinding = `${RUN_ID}CI`
  const nextBinding = `${RUN_ID}CN`
  const actionCode = `EDHR_RF_${ROUTE_VERSION_ID}_${inspectionBinding}`
  const batchCode = `${runKey}-IPQCADV`
  const output = mysql(`
SET NAMES utf8mb4;
SET @run_key := ${sqlString(runKey)};
SET @batch_code := ${sqlString(batchCode)};
SET @main_binding := ${sqlString(mainBinding)};
SET @inspection_binding := ${sqlString(inspectionBinding)};
SET @next_binding := ${sqlString(nextBinding)};
SET @action_code := ${sqlString(actionCode)};
SET @work_order_code := CONCAT('WO-', @batch_code);
INSERT INTO mes_pro_work_order (
  code, name, type, order_source_type, order_source_code, product_id,
  quantity, quantity_produced, quantity_changed, quantity_scheduled,
  batch_code, request_date, parent_id, status, temporary_frozen,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  @work_order_code, CONCAT('eDHR推进E2E工单-', @batch_code), 1, 2, CONCAT('SRC-', @batch_code), 9002001,
  1, 0, 0, 0,
  @batch_code, NOW(), 0, 1, b'0',
  CONCAT('eDHR process advance E2E ', @run_key, ' work order inspection advances'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @work_order_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_batch_execution (
  batch_execution_code, work_order_id, work_order_code, batch_code, active_context_key,
  attempt_no, product_id, product_code, product_name, route_id, route_version_id, route_version_no,
  route_code, route_name, status, task_total, task_approved_count, blocked_count,
  remark, creator, updater, deleted, tenant_id
) VALUES (
  CONCAT('BE-', @batch_code), @work_order_id, @work_order_code, @batch_code, CONCAT('CTX-', @batch_code),
  1, 9002001, 'E2E-PRODUCT', 'eDHR推进E2E产品', ${ROUTE_ID}, ${ROUTE_VERSION_ID}, ${sqlString(ROUTE_VERSION_NO)},
  'E2E-ROUTE', 'eDHR推进E2E路线', 10, 3, 1, 0,
  CONCAT('eDHR process advance E2E ', @run_key, ' inspection advances'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @batch_id := LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, root_process_flag, route_process_sort,
  process_id, process_code, process_name, batch_record_report_id, batch_record_report_name,
  form_slot_type, form_binding_key, form_template_id, form_template_name_snapshot,
  form_template_version_id, form_template_version_no, batch_record_sort, execution_mode,
  record_category, validation_profile, recordbook_enabled, required_policy, owner_role_key,
  archive_visibility, batch_record_version_id, status, required_flag, creator, updater, deleted, tenant_id
) VALUES
(
  @batch_id, 'ROUTE_FORM', ${CURRENT_ROUTE_PROCESS_ID}, b'1', 1,
  ${CURRENT_PROCESS_ID}, 'Z2630', '吹球囊成型-主表已完成E2E', NULL, '主表动态表单',
  'MAIN', @main_binding, ${FORM_TEMPLATE_ID}, ${sqlString(FORM_TEMPLATE_NAME)},
  ${FORM_TEMPLATE_VERSION_ID}, ${sqlString(FORM_TEMPLATE_VERSION_NO)}, 0, 'SEQUENTIAL',
  'BATCH_RECORD', 'CONTROLLED_BATCH', b'1', 'REQUIRED', 'PRODUCTION',
  'DOSSIER', ${ROUTE_VERSION_ID}, 40, b'1', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
),
(
  @batch_id, 'ROUTE_FORM', ${CURRENT_ROUTE_PROCESS_ID}, b'0', 1,
  ${CURRENT_PROCESS_ID}, 'Z2630', '吹球囊成型-过程检验推进E2E', NULL, '过程检验动态表单',
  'PROCESS_INSPECTION', @inspection_binding, ${FORM_TEMPLATE_ID}, ${sqlString(FORM_TEMPLATE_NAME)},
  ${FORM_TEMPLATE_VERSION_ID}, ${sqlString(FORM_TEMPLATE_VERSION_NO)}, 1, 'SEQUENTIAL',
  'BATCH_RECORD', 'CONTROLLED_BATCH', b'1', 'REQUIRED', 'QUALITY',
  'DOSSIER', ${ROUTE_VERSION_ID}, 0, b'1', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @first_current_task_id := LAST_INSERT_ID();
SET @main_task_id := @first_current_task_id;
SET @inspection_task_id := @first_current_task_id + 1;
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, root_process_flag, route_process_sort,
  process_id, process_code, process_name, batch_record_report_id, batch_record_report_name,
  form_slot_type, form_binding_key, form_template_id, form_template_name_snapshot,
  form_template_version_id, form_template_version_no, batch_record_sort, execution_mode,
  record_category, validation_profile, recordbook_enabled, required_policy, owner_role_key,
  archive_visibility, batch_record_version_id, status, required_flag, creator, updater, deleted, tenant_id
) VALUES (
  @batch_id, 'ROUTE_FORM', ${NEXT_ROUTE_PROCESS_ID}, b'0', 2,
  ${NEXT_PROCESS_ID}, 'Z3710', '球囊裁剪-过程检验推进E2E', NULL, '下一工序动态表单',
  'MAIN', @next_binding, ${FORM_TEMPLATE_ID}, ${sqlString(FORM_TEMPLATE_NAME)},
  ${FORM_TEMPLATE_VERSION_ID}, ${sqlString(FORM_TEMPLATE_VERSION_NO)}, 0, 'SEQUENTIAL',
  'BATCH_RECORD', 'CONTROLLED_BATCH', b'1', 'REQUIRED', 'PRODUCTION',
  'DOSSIER', ${ROUTE_VERSION_ID}, 0, b'1', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @next_task_id := LAST_INSERT_ID();
INSERT INTO bpm_business_approval_policy (
  tenant_id, data_domain, system_code, object_type, action_code, object_state,
  policy_mode, process_definition_key, effect_executor_code, form_policy_type,
  form_slots_json, status, remark, creator, updater, deleted
) VALUES (
  ${TEST_TENANT_ID}, 'MES', 'MES', 'EDHR_ROUTE_FORM', @action_code, 'ACTIVE',
  'DIRECT', NULL, 'MES_EDHR_ROUTE_FORM_FILL', NULL,
  NULL, 'PUBLISHED', CONCAT('eDHR process advance E2E ', @run_key, ' inspection advances'), 'codex-e2e', 'codex-e2e', b'0'
);
SET @policy_id := LAST_INSERT_ID();
INSERT INTO bpm_form_action_instance (
  instance_code, tenant_id, policy_id, applicant_user_id, status, data_domain, system_code,
  object_type, action_code, object_state, object_id, object_version, idempotency_key,
  business_context_json, form_data_json, creator, updater, deleted
) VALUES (
  CONCAT('FAI-', @run_key, '-C'), ${TEST_TENANT_ID}, @policy_id, ${ADMIN_USER_ID}, 'DRAFT', 'MES', 'MES',
  'EDHR_ROUTE_FORM', @action_code, 'ACTIVE', CAST(@inspection_task_id AS CHAR), CAST(${ROUTE_VERSION_ID} AS CHAR),
  CONCAT('EDHR_ROUTE_FORM:', @batch_id, ':', @inspection_task_id, ':', @inspection_binding),
  ${buildBusinessContextJson('@inspection_task_id', '@action_code', '@inspection_binding')},
  JSON_OBJECT('batchCode', @batch_code, 'case', 'INSPECTION_ADVANCES'), 'codex-e2e', 'codex-e2e', b'0'
);
SET @instance_id := LAST_INSERT_ID();
UPDATE mes_pro_edhr_batch_execution_task
   SET form_center_instance_id=@instance_id
 WHERE id=@inspection_task_id;
INSERT INTO mes_pro_edhr_process_form_permission_rule (
  route_process_id, batch_record_report_id, batch_record_version_id, rule_type,
  signature_cell_key, candidate_source_type, candidate_source_ids, completion_policy,
  due_minutes, enabled, remark, creator, updater, deleted, tenant_id
) VALUES (
  ${NEXT_ROUTE_PROCESS_ID}, @next_binding, ${ROUTE_VERSION_ID}, 'FILL',
  '', 'USERS', CAST(${AOTEMAN_USER_ID} AS CHAR), 'ANY_ONE',
  180, b'1', CONCAT('eDHR process advance E2E ', @run_key, ' next inspection advances'), 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id, process_name,
  assignee_user_id, candidate_source_type, candidate_user_snapshot, status, due_time, action_url,
  remark, creator, updater, deleted, tenant_id
) VALUES
(
  CONCAT('WT-', @batch_code, '-MAIN'), 'FILL', @batch_id, @main_task_id, 'BATCH_TASK', @main_task_id,
  @work_order_id, @work_order_code, @batch_code, ${ROUTE_ID}, ${CURRENT_ROUTE_PROCESS_ID}, ${CURRENT_PROCESS_ID}, '吹球囊成型-主表已完成E2E',
  ${AOTEMAN_USER_ID}, 'USERS', CAST(${AOTEMAN_USER_ID} AS CHAR), 'DONE', DATE_ADD(NOW(), INTERVAL 1 DAY),
  CONCAT('/mes/pro/feedback/edhr-batch-execution/detail?id=', @batch_id, '&batchExecutionId=', @batch_id, '&batchTaskId=', @main_task_id),
  'completed main filler does not define advance actor while inspection exists', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
),
(
  CONCAT('WT-', @batch_code, '-IPQC'), 'FILL', @batch_id, @inspection_task_id, 'BATCH_TASK', @inspection_task_id,
  @work_order_id, @work_order_code, @batch_code, ${ROUTE_ID}, ${CURRENT_ROUTE_PROCESS_ID}, ${CURRENT_PROCESS_ID}, '吹球囊成型-过程检验推进E2E',
  ${ADMIN_USER_ID}, 'USERS', CAST(${ADMIN_USER_ID} AS CHAR), 'TODO', DATE_ADD(NOW(), INTERVAL 1 DAY),
  CONCAT('/mes/pro/feedback/edhr-batch-execution/detail?id=', @batch_id, '&batchExecutionId=', @batch_id, '&batchTaskId=', @inspection_task_id),
  'inspection filler can advance to next process', 'codex-e2e', 'codex-e2e', b'0', ${TEST_TENANT_ID}
);
SET @first_work_task_id := LAST_INSERT_ID();
SET @main_work_task_id := @first_work_task_id;
SET @inspection_work_task_id := @first_work_task_id + 1;
UPDATE mes_pro_edhr_work_task
   SET action_url=CONCAT(action_url, '&workTaskId=', id)
 WHERE id IN (@main_work_task_id, @inspection_work_task_id);
SELECT JSON_OBJECT(
  'caseName', 'inspectionAdvances',
  'batchId', @batch_id,
  'batchCode', @batch_code,
  'mainTaskId', @main_task_id,
  'currentTaskId', @inspection_task_id,
  'nextTaskId', @next_task_id,
  'mainWorkTaskId', @main_work_task_id,
  'workTaskId', @inspection_work_task_id,
  'workOrderId', @work_order_id,
  'instanceId', @instance_id,
  'formBindingKey', @inspection_binding,
  'expectedNextFill', true,
  'actorUserId', ${ADMIN_USER_ID}
);
`)
  return parseJsonOutput(output, 'prepareInspectionAdvanceScenario')
}

function prepareFixtures(runKey) {
  cleanupRun(runKey)
  return {
    runKey,
    noInspection: prepareNoInspectionScenario(runKey),
    mainBlocked: prepareMainBlockedScenario(runKey),
    inspectionAdvances: prepareInspectionAdvanceScenario(runKey)
  }
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
  throw new Error(`缺少可填写登录控件：${label}`)
}

async function login(page, username, password, targetPath = '/mes/pro/feedback/edhr-work-task') {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', targetPath)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible, form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if (
    (await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0
  ) {
    throw new Error('登录页启用了验证码，无法执行无人值守真实 E2E')
  }

  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TENANT, '租户')
  }
  await fillFirstVisible(
    loginForm.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    username,
    '账号'
  )
  await fillFirstVisible(loginForm.locator('input[type="password"]'), password, '密码')
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: /^登录$/ }).click()
  const loginBody = await (await loginResponsePromise).json()
  unwrapResponse(loginBody, `登录 ${TENANT}/${username}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function queryWorkTask(page, fixture, actorUserId) {
  const firstLoadPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-work-task/my-page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-work-task`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await firstLoadPromise

  const toolbar = page.locator('.edhr-work-task-page__toolbar').first()
  await toolbar.waitFor({ state: 'visible', timeout: 60000 })
  const batchInput = toolbar.locator('.el-form-item').filter({ hasText: '批次' }).locator('input').first()
  await batchInput.fill(fixture.batchCode)
  const queryResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-work-task/my-page') &&
      response.url().includes(encodeURIComponent(fixture.batchCode)) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await toolbar.getByRole('button', { name: '查询' }).click()
  const pageData = unwrapResponse(await (await queryResponsePromise).json(), `工作台查询 ${fixture.caseName}`)
  const rows = Array.isArray(pageData?.list) ? pageData.list : []
  assert.ok(rows.length >= 1, `工作台必须返回 ${fixture.batchCode}`)
  const matched = rows.find((row) => Number(row.id) === Number(fixture.workTaskId))
  assert.ok(matched, `工作台响应缺少目标工作任务 ${fixture.workTaskId}: ${JSON.stringify(rows)}`)
  assert.equal(Number(matched.assigneeUserId), fixture.caseName === 'inspectionAdvances' ? ADMIN_USER_ID : ADMIN_USER_ID)
  assert.ok(
    String(matched.candidateUserSnapshot || '').split(',').map((item) => Number(item.trim())).includes(actorUserId),
    `候选快照必须包含当前用户 ${actorUserId}: ${matched.candidateUserSnapshot}`
  )
  await page.getByText(fixture.batchCode, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return matched
}

async function openAndSubmitFromWorkbench(page, fixture, actorUserId) {
  const row = await queryWorkTask(page, fixture, actorUserId)
  if (actorUserId === AOTEMAN_USER_ID) {
    assert.notEqual(
      Number(row.assigneeUserId),
      actorUserId,
      `${fixture.caseName} must prove candidate non-assignee visibility`
    )
  }
  const openResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '处理' }).first().click()
  const opened = unwrapResponse(await (await openResponsePromise).json(), `打开工作任务 ${fixture.caseName}`)
  assert.equal(Number(opened.workTaskId), Number(fixture.workTaskId), '打开响应必须绑定当前工作任务')
  assert.equal(Number(opened.taskId), Number(fixture.currentTaskId), '打开响应必须绑定当前批次任务')
  assert.ok(Number(opened.formCenterInstanceId) > 0, 'FormCenter 动态表单必须返回实例 ID')
  assert.ok(Number(opened.formTemplateId) > 0, 'FormCenter 动态表单必须返回模板 ID')

  await page.waitForURL(
    (url) =>
      url.pathname.includes('/mes/pro/feedback/edhr-batch-execution/detail') &&
      url.searchParams.get('openRouteForm') === '1',
    { timeout: 60000 }
  )
  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '填写表单' }).last()
  await drawer.waitFor({ state: 'visible', timeout: 60000 })
  await drawer.locator('.form-action-panel').waitFor({ state: 'visible', timeout: 60000 })

  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/form-center/instances/${opened.formCenterInstanceId}/submit`) &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await drawer.getByRole('button', { name: /^提交$/ }).click()
  const submitted = unwrapResponse(await (await submitResponsePromise).json(), `提交动态表单 ${fixture.caseName}`)
  assert.ok(
    ['EFFECTIVE', 'EFFECT_FAILED_PENDING', 'PENDING_EFFECT'].includes(submitted.status),
    `动态表单提交状态不符合预期：${submitted.status}`
  )
  return { opened, submitted }
}

function verifyScenarioDb(fixture) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'caseName', ${sqlString(fixture.caseName)},
  'batchCode', ${sqlString(fixture.batchCode)},
  'workTaskStatus', (SELECT status FROM mes_pro_edhr_work_task WHERE id=${Number(fixture.workTaskId)}),
  'batchTaskStatus', (SELECT status FROM mes_pro_edhr_batch_execution_task WHERE id=${Number(fixture.currentTaskId)}),
  'instanceStatus', (SELECT status FROM bpm_form_action_instance WHERE id=${Number(fixture.instanceId)}),
  'effectStatus', (SELECT status FROM bpm_form_effect_execution WHERE tenant_id=${TEST_TENANT_ID} AND instance_id=${Number(fixture.instanceId)} AND deleted=b'0' ORDER BY id DESC LIMIT 1),
  'nextFillCount', (SELECT COUNT(*) FROM mes_pro_edhr_work_task WHERE tenant_id=${TEST_TENANT_ID} AND deleted=b'0' AND batch_task_id=${Number(fixture.nextTaskId)} AND task_type='FILL'),
  'nextCandidateSnapshot', (SELECT candidate_user_snapshot FROM mes_pro_edhr_work_task WHERE tenant_id=${TEST_TENANT_ID} AND deleted=b'0' AND batch_task_id=${Number(fixture.nextTaskId)} AND task_type='FILL' ORDER BY id DESC LIMIT 1)
);
`)
  const evidence = parseJsonOutput(output, `verifyScenarioDb ${fixture.caseName}`)
  assert.equal(evidence.workTaskStatus, 'DONE', `${fixture.caseName} current work task must be DONE`)
  assert.equal(Number(evidence.batchTaskStatus), 40, `${fixture.caseName} current batch task must be APPROVED`)
  assert.equal(evidence.instanceStatus, 'EFFECTIVE', `${fixture.caseName} FormCenter instance must be EFFECTIVE`)
  assert.equal(evidence.effectStatus, 'APPLIED', `${fixture.caseName} business effect must be APPLIED`)
  if (fixture.expectedNextFill) {
    assert.ok(Number(evidence.nextFillCount) >= 1, `${fixture.caseName} must create next process fill task`)
  } else {
    assert.equal(Number(evidence.nextFillCount), 0, `${fixture.caseName} must not create next process fill task`)
  }
  return evidence
}

async function runForUser(username, password, cases) {
  const browser = await chromium.launch({
    headless: process.env.EDHR_PROCESS_ADVANCE_E2E_HEADED !== '1',
    executablePath: EXECUTABLE_PATH || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))
  try {
    await login(page, username, password)
    const results = []
    for (const item of cases) {
      results.push(await openAndSubmitFromWorkbench(page, item.fixture, item.actorUserId))
    }
    assert.deepEqual(pageErrors, [], `页面运行时错误：${pageErrors.join('\n')}`)
    return results
  } finally {
    await context.close()
    await browser.close()
  }
}

async function main() {
  assertLocalOnly()
  await assertRuntimeUp()
  const runKey = `EDHR-ADV-${RUN_ID}`
  let fixtures
  try {
    fixtures = prepareFixtures(runKey)
    await runForUser(AOTEMAN_USERNAME, AOTEMAN_PASSWORD, [
      { fixture: fixtures.noInspection, actorUserId: AOTEMAN_USER_ID },
      { fixture: fixtures.mainBlocked, actorUserId: AOTEMAN_USER_ID }
    ])
    await runForUser(ADMIN_USERNAME, ADMIN_PASSWORD, [
      { fixture: fixtures.inspectionAdvances, actorUserId: ADMIN_USER_ID }
    ])
    const dbEvidence = [
      verifyScenarioDb(fixtures.noInspection),
      verifyScenarioDb(fixtures.mainBlocked),
      verifyScenarioDb(fixtures.inspectionAdvances)
    ]
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          runKey,
          tenant: TENANT,
          users: [AOTEMAN_USERNAME, ADMIN_USERNAME],
          baseUrl: BASE_URL,
          backendUrl: BACKEND_URL,
          fixtures,
          dbEvidence,
          cleanup: CLEANUP
        },
        null,
        2
      )
    )
  } finally {
    if (CLEANUP) {
      cleanupRun(runKey)
    }
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
