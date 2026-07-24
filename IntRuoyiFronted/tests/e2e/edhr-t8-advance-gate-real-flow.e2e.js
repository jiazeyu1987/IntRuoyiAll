const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_T8_E2E_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_T8_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.EDHR_T8_E2E_LOGIN_PASSWORD || '111111'
const SIGNATURE_PASSWORD = process.env.EDHR_T8_E2E_SIGNATURE_PASSWORD
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.EDHR_T8_E2E_ADMIN_PASSWORD || 'admin123'
const EXECUTION_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const WORK_TASK_ROUTE = '/mes/pro/feedback/edhr-work-task'
const TASK_STATE_PATH = path.resolve(__dirname, '../../../doc/tasks/20260613-batch-record-gap-implementation/task-state.json')
const RUN_KEY = `T8-ADVANCE-GATE-${Date.now()}`
const ROLE_ID = Number(process.env.EDHR_T8_E2E_REVIEW_ROLE_ID || 111)
let setupIndex = 0

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'T8 E2E must use local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'T8 E2E must use local backend 48081')
}

function requireRealGate(taskId) {
  const state = JSON.parse(fs.readFileSync(TASK_STATE_PATH, 'utf8'))
  const task = state.tasks.find((item) => item.task_id === taskId)
  if (!task) {
    throw blocked(`task-state.json missing ${taskId}; T8 real E2E cannot verify dependency gate.`)
  }
  if (task.status !== 'validated_real_e2e_pass') {
    throw blocked(`T8 real E2E requires ${taskId} validated_real_e2e_pass first.`, [
      `current ${taskId} status: ${task.status}`,
      `current ${taskId} outcome: ${task.last_outcome || '--'}`
    ])
  }
}

function sqlString(value) {
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

function mysql(sql) {
  return execFileSync('docker', [
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
  ], { input: sql, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }).trim()
}

function parseJsonRow(output, label) {
  const line = output.split(/\r?\n/).find(Boolean)
  if (!line || line === 'NULL') return null
  try {
    return JSON.parse(line)
  } catch (error) {
    throw new Error(`${label} returned non JSON output: ${line}`)
  }
}

function prepareExecutionWithReviewCandidates() {
  setupIndex += 1
  const runKey = `${RUN_KEY}-${setupIndex}`
  const signatureCellKey = `${runKey}-R1C1`
  const snapshot = {
    snapshotVersion: 'EDHR_EXECUTION_V1',
    from: 't8-advance-gate-real-e2e',
    layout: {
      rows: {
        1: {
          cells: {
            1: {
              text: 'T8 审核候选选择',
              edhrSignature: {
                enabled: true,
                actionType: 'APPROVE',
                signatureCellKey,
                reviewSourceType: 'ROLE',
                reviewSourceId: ROLE_ID,
                reviewSourceName: '租户管理员'
              }
            }
          }
        }
      }
    },
    fields: [
      {
        rowIndex: 2,
        columnIndex: 1,
        fieldKey: `${runKey}-FIELD-A`,
        fieldPath: `${runKey}.fieldA`,
        label: 'T8审核候选真实字段',
        required: false,
        componentType: 'input',
        inputType: 'text',
        valueType: 'STRING'
      }
    ]
  }
  const sql = `
SET NAMES utf8mb4;
SET @tenant_id := 122;
SET @run_key := ${sqlString(runKey)};
SET @snapshot := ${sqlString(JSON.stringify(snapshot))};
SET @source_execution_id := (
  SELECT id
  FROM mes_pro_batch_record_execution
  WHERE tenant_id=@tenant_id
    AND deleted=0
    AND status=0
    AND work_order_id IS NOT NULL
    AND task_id IS NOT NULL
    AND route_process_id IS NOT NULL
    AND workstation_id IS NOT NULL
    AND batch_record_report_id IS NOT NULL
  ORDER BY id DESC
  LIMIT 1
);
SELECT CASE WHEN @source_execution_id IS NULL THEN JSON_OBJECT('blocked', '测试租户缺少可复制的真实 eDHR 执行上下文') ELSE JSON_OBJECT('sourceExecutionId', @source_execution_id) END;
INSERT INTO mes_pro_batch_record_execution (
  execution_code, template_id, template_code, template_name, work_order_id, work_order_code,
  route_process_id, task_id, workstation_id, batch_record_report_id, batch_code, status,
  sheet_layout_json, meta_json, execution_snapshot_json, cell_values_json, cell_values_hash,
  field_audit_revision, field_audit_head_hash, revision_no, active_revision_flag,
  remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('BRE-', @run_key), template_id, template_code, template_name, work_order_id, work_order_code,
  route_process_id, task_id, workstation_id, batch_record_report_id, @run_key, 0,
  @snapshot, '{}', @snapshot, '[]', cell_values_hash,
  0, field_audit_head_hash, 1, b'1',
  'T8审核候选真实E2E', 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution
WHERE id=@source_execution_id;
SET @execution_id := LAST_INSERT_ID();
UPDATE mes_pro_batch_record_execution
SET revision_root_execution_id=@execution_id
WHERE id=@execution_id;
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id,
  process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, signature_cell_key, status, due_time, action_url, remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('EDHRT-', @run_key), 'FILL', @execution_id, e.task_id, 'BATCH_TASK', e.task_id,
  @execution_id, e.work_order_id, e.work_order_code, e.batch_code, rp.route_id, e.route_process_id, rp.process_id,
  p.name, 113, 'USER', 113, '113',
  113, '', 'TODO', DATE_ADD(NOW(), INTERVAL 1 DAY),
  CONCAT('/mes/pro/feedback/edhr-execution/detail?id=', @execution_id, '&workTaskId=', LAST_INSERT_ID()),
  'T8审核候选真实E2E填写任务', 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_route_process rp ON rp.id=e.route_process_id
JOIN mes_pro_process p ON p.id=rp.process_id
WHERE e.id=@execution_id;
SET @fill_task_id := LAST_INSERT_ID();
UPDATE mes_pro_edhr_work_task
SET action_url=CONCAT('/mes/pro/feedback/edhr-execution/detail?id=', @execution_id, '&workTaskId=', @fill_task_id)
WHERE id=@fill_task_id;
SELECT JSON_OBJECT('executionId', @execution_id, 'fillTaskId', @fill_task_id, 'actionUrl', CONCAT('/mes/pro/feedback/edhr-execution/detail?id=', @execution_id, '&workTaskId=', @fill_task_id), 'signatureCellKey', ${sqlString(signatureCellKey)}, 'runKey', @run_key);
`
  const rows = mysql(sql).split(/\r?\n/).filter(Boolean)
  const first = parseJsonRow(rows[0] || '', 'T8 source execution precondition')
  if (first?.blocked) {
    throw blocked(first.blocked)
  }
  const result = parseJsonRow(rows[rows.length - 1] || '', 'T8 execution setup')
  if (!result?.executionId || !result?.fillTaskId) {
    throw blocked(`T8 真实数据准备未返回 executionId/fillTaskId：${JSON.stringify(result)}`)
  }
  return result
}

function loadApprovalSnapshot(executionId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'id', id,
  'processInstanceId', process_instance_id,
  'approvalStatus', approval_status,
  'snapshotJson', snapshot_json,
  'snapshotHash', snapshot_hash,
  'submitSignatureId', submit_signature_id,
  'currentBpmTaskId', current_bpm_task_id,
  'submittedBy', submitted_by
)
FROM mes_pro_batch_record_approval_snapshot
WHERE tenant_id=122
  AND execution_id=${Number(executionId)}
ORDER BY id DESC
LIMIT 1;
`)
  return parseJsonRow(output, 'approval snapshot')
}

function loadReviewTasks(executionId, signatureCellKey) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT(
  'id', id,
  'status', status,
  'assigneeUserId', assignee_user_id,
  'candidateSourceType', candidate_source_type,
  'candidateSourceId', candidate_source_id,
  'candidateUserSnapshot', candidate_user_snapshot,
  'signatureCellKey', signature_cell_key,
  'reviewSourceType', review_source_type,
  'reviewSourceId', review_source_id,
  'bpmTaskId', bpm_task_id
)), JSON_ARRAY())
FROM mes_pro_edhr_work_task
WHERE tenant_id=122
  AND execution_id=${Number(executionId)}
  AND task_type='REVIEW'
  AND signature_cell_key=${sqlString(signatureCellKey)};
`)
  return parseJsonRow(output, 'T8 review tasks') || []
}

function resolveCandidateUserId(optionText) {
  const text = optionText.trim()
  const output = mysql(`
SET NAMES utf8mb4;
SELECT id
FROM system_users
WHERE tenant_id=122
  AND deleted=0
  AND (username=${sqlString(text)} OR nickname=${sqlString(text)} OR CAST(id AS CHAR)=${sqlString(text)})
ORDER BY id
LIMIT 1;
`)
  const line = output.split(/\r?\n/).find(Boolean)
  if (line) {
    return Number(line)
  }
  const match = text.match(/^(\d+)$/)
  if (match) return Number(match[1])
  throw new Error(`无法从真实用户表解析候选人：${text}`)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.fill(value)
      return
    }
  }
  throw blocked(`缺少可填写控件：${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  const states = []
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    const visible = await item.isVisible()
    const disabled = await item.isDisabled()
    states.push({ index, visible, disabled, text: await item.innerText().catch(() => '') })
    if (visible && !disabled) {
      await item.click()
      return
    }
  }
  throw blocked(`缺少可点击控件：${label}; states=${JSON.stringify(states)}`)
}

async function login(page, tenant, username, password, redirect = WORK_TASK_ROUTE) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(redirect)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw blocked('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), password, 'password')
  await clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }), 'login button')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openExecution(page, setup) {
  await page.goto(`${BASE_URL}${setup.actionUrl}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('eDHR 执行详情').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(RUN_KEY).first().waitFor({ state: 'visible', timeout: 60000 })
  assert.ok(page.url().includes(`workTaskId=${setup.fillTaskId}`), 'T8 actionUrl must carry workTaskId')
}

async function openSubmitDialog(page) {
  await clickFirstEnabled(page.getByRole('button', { name: '提交执行' }), 'submit execution')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '提交 eDHR 执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('.edhr-page-shell__submit-select').first().waitFor({ state: 'visible', timeout: 30000 })
  return dialog
}

async function assertMissingSelectionBlocked(page, dialog) {
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), SIGNATURE_PASSWORD, 'submit password')
  const commentInput = dialog.locator('textarea').first()
  if ((await commentInput.count()) > 0 && (await commentInput.isVisible())) {
    await commentInput.fill(`T8 missing selection ${RUN_KEY}`)
  }
  const beforeRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/pro/batch-record-execution/submit')) {
      beforeRequests.push(request)
    }
  })
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 定|确 认|确认/ }), 'confirm submit without selection')
  await page.getByText('请选择审核/批准人').first().waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(beforeRequests.length, 0, 'missing advance gate selection must be blocked by page before submit request')
}

async function chooseFirstReviewCandidate(page, dialog) {
  const selector = dialog.locator('.edhr-page-shell__submit-select').first()
  await selector.click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)').first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const optionText = (await option.innerText()).trim()
  await option.click()
  return resolveCandidateUserId(optionText)
}

async function submitWithSelection(page, dialog, setup, selectedUserId) {
  const [request, response] = await Promise.all([
    page.waitForRequest(
      (request) =>
        request.url().includes('/admin-api/mes/pro/batch-record-execution/submit') &&
        request.method() === 'PUT',
      { timeout: 60000 }
    ),
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/batch-record-execution/submit') &&
        response.request().method() === 'PUT',
      { timeout: 60000 }
    ),
    clickFirstEnabled(dialog.getByRole('button', { name: /确 定|确 认|确认/ }), 'confirm ordinary submit')
  ])
  const requestData = request.postDataJSON()
  assert.equal(requestData.id, setup.executionId, 'submit payload must carry execution id')
  assert.equal(requestData.workTaskId, setup.fillTaskId, 'submit payload must carry workTaskId')
  assert.equal(requestData.reviewAssigneeSelections, undefined, 'ordinary submit payload must not carry reviewAssigneeSelections')
  assert.equal(response.status(), 200, 'submit HTTP status must be 200')
  const body = await response.json()
  assert.equal(body.code, 0, `submit business response must succeed: ${JSON.stringify(body)}`)
}

async function verifySubmissionEvidence(setup, selectedUserId) {
  const snapshot = loadApprovalSnapshot(setup.executionId)
  assert.ok(snapshot?.snapshotJson, `approval snapshot must exist for execution ${setup.executionId}`)
  const snapshotJson = JSON.parse(snapshot.snapshotJson)
  const reviewAssignments = snapshotJson.reviewAssignments || []
  assert.equal(reviewAssignments.length, 1, `snapshot must contain one review assignment: ${snapshot.snapshotJson}`)
  assert.equal(reviewAssignments[0].signatureCellKey, setup.signatureCellKey, 'snapshot must keep signatureCellKey')
  assert.equal(Number(reviewAssignments[0].assigneeUserId), Number(selectedUserId), 'snapshot must keep selected assigneeUserId')
  assert.ok(String(reviewAssignments[0].assigneeUserName || '').trim(), 'snapshot must keep selected assigneeUserName')
  assert.ok(
    (reviewAssignments[0].candidateUserIds || []).some((candidateUserId) => Number(candidateUserId) === Number(selectedUserId)),
    'snapshot candidate pool must include selected user'
  )
  const reviewTasks = loadReviewTasks(setup.executionId, setup.signatureCellKey)
  assert.equal(reviewTasks.length, 1, `eDHR REVIEW work task must only be created for selected user: ${JSON.stringify(reviewTasks)}`)
  assert.equal(Number(reviewTasks[0].assigneeUserId), Number(selectedUserId), 'REVIEW task assignee must be selected user')
  return { snapshot, reviewTasks }
}


function configureBatchGate(setup, mode) {
  const withPeer = mode === 'blocked'
  const specialStatus = mode === 'blocked' ? 0 : 45
  const output = mysql(`
SET NAMES utf8mb4;
SET @tenant_id := 122;
SET @execution_id := ${Number(setup.executionId)};
SET @mode := ${sqlString(mode)};
SET @batch_execution_id := 900000000000 + @execution_id;
SET @batch_code := (SELECT batch_code FROM mes_pro_batch_record_execution WHERE id=@execution_id);
SET @work_order_id := (SELECT work_order_id FROM mes_pro_batch_record_execution WHERE id=@execution_id);
SET @work_order_code := (SELECT work_order_code FROM mes_pro_batch_record_execution WHERE id=@execution_id);
SET @route_process_id := (SELECT route_process_id FROM mes_pro_batch_record_execution WHERE id=@execution_id);
SET @route_id := (SELECT rp.route_id FROM mes_pro_route_process rp WHERE rp.id=@route_process_id);
SET @process_id := (SELECT rp.process_id FROM mes_pro_route_process rp WHERE rp.id=@route_process_id);
SET @process_name := (SELECT p.name FROM mes_pro_process p WHERE p.id=@process_id);
INSERT IGNORE INTO mes_pro_edhr_batch_execution (
  id, batch_execution_code, work_order_id, work_order_code, batch_code, product_id, route_id,
  status, creator, updater, deleted, tenant_id
) VALUES (
  @batch_execution_id, CONCAT('BE-', @mode, '-', @execution_id), @work_order_id, @work_order_code,
  @batch_code, 0, @route_id, 10, 'codex', 'codex', b'0', @tenant_id
);
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, route_process_sort, process_id, process_code, process_name,
  batch_record_report_id, batch_record_report_name, batch_record_sort, execution_mode, execution_id,
  status, required_flag, creator, updater, deleted, tenant_id
) VALUES
(@batch_execution_id, 'ROUTE_FORM', @route_process_id, 10, @process_id, CONCAT('PROC-', @mode, '-CURRENT'), @process_name,
 CONCAT('REPORT-', @mode, '-CURRENT'), CONCAT('current-node-', @mode), 10, ${withPeer ? sqlString('PARALLEL') : sqlString('SEQUENTIAL')}, @execution_id,
 40, b'1', 'codex', 'codex', b'0', @tenant_id);
SET @current_task_id := LAST_INSERT_ID();
${withPeer ? `INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, route_process_sort, process_id, process_code, process_name,
  batch_record_report_id, batch_record_report_name, batch_record_sort, execution_mode, execution_id,
  status, required_flag, creator, updater, deleted, tenant_id
) VALUES
(@batch_execution_id, 'ROUTE_FORM', @route_process_id + 999000, 10, @process_id, CONCAT('PROC-', @mode, '-PEER'), @process_name,
 CONCAT('REPORT-', @mode, '-PEER'), CONCAT('parallel-peer-', @mode), 10, 'PARALLEL', NULL,
 0, b'1', 'codex', 'codex', b'0', @tenant_id);` : ''}
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, route_process_sort, process_id, process_code, process_name,
  batch_record_report_id, batch_record_report_name, batch_record_sort, execution_mode, execution_id,
  status, required_flag, creator, updater, deleted, tenant_id
) VALUES
(@batch_execution_id, 'STERILIZATION_REPORT', NULL, 15, NULL, CONCAT('SPECIAL-', @mode), CONCAT('special-or-next-', @mode),
 NULL, CONCAT('special-or-next-', @mode), 15, 'SEQUENTIAL', NULL,
 ${specialStatus}, b'1', 'codex', 'codex', b'0', @tenant_id);
INSERT INTO mes_pro_edhr_batch_execution_task (
  batch_execution_id, node_type, route_process_id, route_process_sort, process_id, process_code, process_name,
  batch_record_report_id, batch_record_report_name, batch_record_sort, execution_mode, execution_id,
  status, required_flag, creator, updater, deleted, tenant_id
) VALUES
(@batch_execution_id, 'ROUTE_FORM', @route_process_id, 20, @process_id, CONCAT('PROC-', @mode, '-NEXT'), @process_name,
 CONCAT('REPORT-', @mode, '-NEXT'), CONCAT('special-or-next-', @mode), 20, 'SEQUENTIAL', NULL,
 0, b'1', 'codex', 'codex', b'0', @tenant_id);
SET @next_task_id := LAST_INSERT_ID();
UPDATE mes_pro_edhr_work_task
SET batch_execution_id=@batch_execution_id,
    batch_task_id=@current_task_id,
    business_scope_id=@current_task_id,
    route_id=@route_id,
    route_process_id=@route_process_id,
    process_id=@process_id,
    process_name=@process_name
WHERE tenant_id=@tenant_id AND execution_id=@execution_id AND task_type IN ('FILL','REVIEW');
INSERT INTO mes_pro_edhr_work_task_assignment_rule (
  route_process_id, scope_type, scope_id, task_type, assignee_user_id, candidate_source_type,
  candidate_source_id, due_minutes, enabled, remark, creator, updater, deleted, tenant_id
)
SELECT @route_process_id, 'ROUTE_PROCESS', @route_process_id, 'FILL', 113, 'USER', 113, 1440, b'1',
       CONCAT('T8 next fill ', @mode), 'codex', 'codex', b'0', @tenant_id
WHERE NOT EXISTS (
  SELECT 1 FROM mes_pro_edhr_work_task_assignment_rule
  WHERE tenant_id=@tenant_id AND deleted=0 AND route_process_id=@route_process_id AND task_type='FILL'
);
SELECT JSON_OBJECT('mode', @mode, 'batchExecutionId', @batch_execution_id, 'currentTaskId', @current_task_id, 'nextTaskId', @next_task_id);
`)
  const result = parseJsonRow(output.split(/\r?\n/).filter(Boolean).pop() || '', `T8 ${mode} batch gate setup`)
  if (!result?.currentTaskId || !result?.nextTaskId) {
    throw blocked(`T8 ${mode} batch gate setup failed: ${output}`)
  }
  return result
}

function loadAdvanceEvidence(setup, gate) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'executionId', ${Number(setup.executionId)},
  'executionStatus', (SELECT status FROM mes_pro_batch_record_execution WHERE id=${Number(setup.executionId)}),
  'snapshotStatus', (SELECT approval_status FROM mes_pro_batch_record_approval_snapshot WHERE execution_id=${Number(setup.executionId)} ORDER BY id DESC LIMIT 1),
  'reviewStatuses', (SELECT JSON_ARRAYAGG(status) FROM mes_pro_edhr_work_task WHERE execution_id=${Number(setup.executionId)} AND task_type='REVIEW'),
  'nextFillCount', (SELECT COUNT(*) FROM mes_pro_edhr_work_task WHERE batch_execution_id=${Number(gate.batchExecutionId)} AND task_type='FILL' AND batch_task_id=${Number(gate.nextTaskId)}),
  'nextFillStatuses', (SELECT JSON_ARRAYAGG(status) FROM mes_pro_edhr_work_task WHERE batch_execution_id=${Number(gate.batchExecutionId)} AND task_type='FILL' AND batch_task_id=${Number(gate.nextTaskId)})
);
`)
  return parseJsonRow(output, 'T8 advance evidence')
}

async function approveDetail(page, setup, reviewTask, comment) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-approval/detail?id=${setup.executionId}&bpmTaskId=${encodeURIComponent(reviewTask.bpmTaskId)}&workTaskId=${reviewTask.id}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.edhr-detail__summary').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(setup.runKey).first().waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(page.locator('.edhr-detail__action-bar .el-button--primary'), 'approve detail button')
  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), SIGNATURE_PASSWORD, 'approve password')
  const commentInput = dialog.locator('textarea').first()
  if ((await commentInput.count()) > 0 && (await commentInput.isVisible())) {
    await commentInput.fill(comment)
  }
  const [response] = await Promise.all([
    page.waitForResponse((res) => res.url().includes('/admin-api/mes/pro/batch-record-execution/approve') && res.request().method() === 'PUT', { timeout: 60000 }),
    clickFirstEnabled(dialog.locator('.el-dialog__footer .el-button--primary'), 'confirm approve')
  ])
  assert.equal(response.status(), 200, 'approve HTTP status must be 200')
  const body = await response.json()
  assert.equal(body.code, 0, `approve business response must succeed: ${JSON.stringify(body)}`)
  return body.data
}

async function verifyAdminReadonly(browser) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const writeRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      writeRequests.push(`${request.method()} ${request.url()}`)
    }
  })
  try {
    await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD, WORK_TASK_ROUTE)
    const listResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/edhr-work-task/my-page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}${WORK_TASK_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    const response = await listResponsePromise
    const body = await response.json()
    assert.equal(response.status(), 200, 'admin readonly work task list HTTP status must be 200')
    assert.equal(body.code, 0, `admin readonly work task list must succeed: ${JSON.stringify(body)}`)
    assert.deepEqual(writeRequests, [], `admin readonly verification must not issue MES writes: ${JSON.stringify(writeRequests)}`)
  } finally {
    await context.close()
  }
}

async function run() {
  assertLocalOnly()
  const blockers = []
  if (!SIGNATURE_PASSWORD) {
    blockers.push('缺少 EDHR_T8_E2E_SIGNATURE_PASSWORD，不能执行提交签名真实 E2E。')
  }
  try {
    requireRealGate('T6')
    requireRealGate('T7')
  } catch (error) {
    if (error.blocked) {
      blockers.push(error.message)
      blockers.push(...(error.details || []))
    } else {
      throw error
    }
  }
  if (blockers.length > 0) {
    throw blocked('T8 real E2E 前置条件未满足。', blockers)
  }

  const setup = prepareExecutionWithReviewCandidates()
  const browser = await chromium.launch({ headless: process.env.EDHR_T8_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD, setup.actionUrl)
    await openExecution(page, setup)
    let dialog = await openSubmitDialog(page)
    await assertMissingSelectionBlocked(page, dialog)
    await page.keyboard.press('Escape')
    await page.waitForTimeout(300)

    dialog = await openSubmitDialog(page)
    await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), SIGNATURE_PASSWORD, 'submit password')
    const commentInput = dialog.locator('textarea').first()
    if ((await commentInput.count()) > 0 && (await commentInput.isVisible())) {
      await commentInput.fill(`T8 advance gate real E2E ${RUN_KEY}`)
    }
    const selectedUserId = await chooseFirstReviewCandidate(page, dialog)
    await submitWithSelection(page, dialog, setup, selectedUserId)
    const submitted = await verifySubmissionEvidence(setup, selectedUserId)
    const blockedGate = configureBatchGate(setup, 'blocked')
    const blockedApproval = await approveDetail(page, setup, { id: submitted.reviewTasks[0].id, bpmTaskId: submitted.snapshot.currentBpmTaskId || submitted.reviewTasks[0].bpmTaskId }, `T8 blocked gate ${RUN_KEY}`)
    const blockedEvidence = loadAdvanceEvidence(setup, blockedGate)
    assert.equal(blockedApproval.status, 3, 'blocked scenario still approves the current execution')
    assert.equal(Number(blockedEvidence.nextFillCount), 0, `blocked prerequisites must not create next fill: ${JSON.stringify(blockedEvidence)}`)

    const setup2 = prepareExecutionWithReviewCandidates()
    await page.goto(`${BASE_URL}${setup2.actionUrl}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await openExecution(page, setup2)
    const dialog2 = await openSubmitDialog(page)
    await fillFirstVisible(dialog2.locator('input[type="password"], input[placeholder*="??"]'), SIGNATURE_PASSWORD, 'submit password')
    const commentInput2 = dialog2.locator('textarea').first()
    if ((await commentInput2.count()) > 0 && (await commentInput2.isVisible())) {
      await commentInput2.fill(`T8 satisfied gate ${RUN_KEY}`)
    }
    const selectedUserId2 = await chooseFirstReviewCandidate(page, dialog2)
    await submitWithSelection(page, dialog2, setup2, selectedUserId2)
    const submitted2 = await verifySubmissionEvidence(setup2, selectedUserId2)
    const satisfiedGate = configureBatchGate(setup2, 'satisfied')
    const approved = await approveDetail(page, setup2, { id: submitted2.reviewTasks[0].id, bpmTaskId: submitted2.snapshot.currentBpmTaskId || submitted2.reviewTasks[0].bpmTaskId }, `T8 satisfied gate ${RUN_KEY}`)
    const satisfiedEvidence = loadAdvanceEvidence(setup2, satisfiedGate)
    assert.equal(approved.status, 3, 'satisfied scenario must approve execution')
    assert.equal(Number(satisfiedEvidence.nextFillCount), 1, `satisfied prerequisites must create exactly one next fill: ${JSON.stringify(satisfiedEvidence)}`)
    await verifyAdminReadonly(browser)
    console.log(`PASS: T8 advance gate real E2E runKey=${RUN_KEY} blockedExecution=${setup.executionId} satisfiedExecution=${setup2.executionId}`)
    console.log(`PASS: blockedNextFill=${blockedEvidence.nextFillCount} satisfiedNextFill=${satisfiedEvidence.nextFillCount} nextTask=${satisfiedGate.nextTaskId}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  if (error.blocked) {
    console.error(`BLOCKED: ${error.message}`)
    for (const detail of error.details || []) {
      console.error(`- ${detail}`)
    }
  } else {
    console.error(error)
  }
  process.exitCode = 1
})
