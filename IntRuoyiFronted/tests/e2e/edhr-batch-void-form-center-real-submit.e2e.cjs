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
const APPROVAL_MODE = process.env.EDHR_BATCH_VOID_E2E_APPROVAL_MODE || 'DIRECT'
const RUN_ID = process.env.EDHR_BATCH_VOID_E2E_RUN_ID || new Date().toISOString().replace(/\D/g, '').slice(0, 14)
const APPROVER_USER_ID = Number(process.env.EDHR_BATCH_VOID_E2E_APPROVER_USER_ID || '912398')
const APPROVER_USERNAME = process.env.EDHR_BATCH_VOID_E2E_APPROVER_USERNAME || 'admin'
const APPROVER_PASSWORD = process.env.EDHR_BATCH_VOID_E2E_APPROVER_PASSWORD || '111111'
const APPROVER_SIGNATURE_PASSWORD =
  process.env.EDHR_BATCH_VOID_E2E_APPROVER_SIGNATURE_PASSWORD ||
  process.env.EDHR_BATCH_VOID_E2E_SIGNATURE_PASSWORD ||
  '111111'
const SHOULD_COMPLETE_APPROVAL = process.env.EDHR_BATCH_VOID_E2E_COMPLETE_APPROVAL === '1'
const RESUME_BATCH_EXECUTION_ID = Number(
  process.env.EDHR_BATCH_VOID_E2E_RESUME_BATCH_EXECUTION_ID || '0'
)
const RESUME_FORM_INSTANCE_ID = Number(
  process.env.EDHR_BATCH_VOID_E2E_RESUME_FORM_INSTANCE_ID || '0'
)
const EXPECTED_TENANT_ID = '122'
const APPROVAL_ROUTE = '/approval-center/todo'
const ARTIFACT_DIR = path.resolve(
  process.env.EDHR_BATCH_VOID_E2E_ARTIFACT_DIR ||
    path.join(
      WORKSPACE_ROOT,
      'doc/tasks/20260721-batch-record-bpm-toggle-implementation/e2e-artifacts/edhr-batch-void'
    )
)

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'eDHR batch void E2E must use local frontend')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'eDHR batch void E2E must use local backend')
  assert.equal(TENANT, '测试租户', 'write E2E must use test tenant')
  assert.equal(USERNAME, 'aoteman', 'write E2E must use test tenant aoteman')
  assert.ok(['DIRECT', 'BPM_REQUIRED'].includes(APPROVAL_MODE), 'EDHR_BATCH_VOID_E2E_APPROVAL_MODE must be DIRECT or BPM_REQUIRED')
  const hasResumeBatch = Number.isInteger(RESUME_BATCH_EXECUTION_ID) && RESUME_BATCH_EXECUTION_ID > 0
  const hasResumeInstance = Number.isInteger(RESUME_FORM_INSTANCE_ID) && RESUME_FORM_INSTANCE_ID > 0
  assert.equal(
    hasResumeBatch,
    hasResumeInstance,
    'resume mode requires both batch execution id and form instance id'
  )
  if (hasResumeBatch) {
    assert.equal(APPROVAL_MODE, 'BPM_REQUIRED', 'resume mode only supports BPM_REQUIRED')
    assert.equal(SHOULD_COMPLETE_APPROVAL, true, 'resume mode must complete the pending approval')
  }
  if (SHOULD_COMPLETE_APPROVAL) {
    assert.equal(APPROVAL_MODE, 'BPM_REQUIRED', 'terminal approval completion only applies to BPM_REQUIRED')
    assert.ok(APPROVER_USERNAME, 'terminal approval requires approver username')
    assert.ok(APPROVER_PASSWORD, 'terminal approval requires approver login password')
    assert.ok(APPROVER_SIGNATURE_PASSWORD, 'terminal approval requires approver signature password')
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

function sleep(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms)
}

function prepareClosedBatchVoidSample() {
  const runKey = `M7-EDHR-VOID-${APPROVAL_MODE}-${RUN_ID}`
  const output = mysql(`
SET NAMES utf8mb4;
SET @tenant_id := 122;
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
    AND COALESCE(e.route_process_id, wt.route_process_id) IS NOT NULL
    AND e.batch_record_report_id IS NOT NULL
    AND e.slot_config_snapshot_hash IS NOT NULL
    AND wt.batch_execution_id IS NOT NULL
    AND wt.batch_task_id IS NOT NULL
  ORDER BY e.id DESC
  LIMIT 1
);
SET @source_work_task_id := (
  SELECT wt.id
  FROM mes_pro_edhr_work_task wt
  JOIN mes_pro_batch_record_execution e2 ON e2.id=wt.execution_id
  JOIN mes_pro_edhr_batch_execution be ON be.id=wt.batch_execution_id
    AND be.tenant_id=@tenant_id
    AND be.deleted=0
  JOIN mes_pro_edhr_batch_execution_task bt ON bt.id=wt.batch_task_id
    AND bt.batch_execution_id=be.id
    AND bt.tenant_id=@tenant_id
    AND bt.deleted=0
  WHERE wt.tenant_id=@tenant_id
    AND wt.deleted=0
    AND wt.task_type='FILL'
    AND wt.execution_id=@source_execution_id
    AND COALESCE(wt.route_process_id, e2.route_process_id) IS NOT NULL
  ORDER BY wt.id DESC
  LIMIT 1
);
SET @source_batch_execution_id := (
  SELECT wt.batch_execution_id
  FROM mes_pro_edhr_work_task wt
  WHERE wt.id=@source_work_task_id
    AND wt.tenant_id=@tenant_id
    AND wt.deleted=0
);
SET @source_batch_task_id := (
  SELECT wt.batch_task_id
  FROM mes_pro_edhr_work_task wt
  WHERE wt.id=@source_work_task_id
    AND wt.tenant_id=@tenant_id
    AND wt.deleted=0
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
  30, 1, 1, 0, NULL, 'M7 eDHR batch void real E2E closed batch',
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
  batch_record_sort, instance_scope, shared_form_key, form_slot_type, record_category, validation_profile,
  permission_scope_id, route_binding_id, route_binding_snapshot_hash, archive_visibility, slot_config_snapshot_hash,
  execution_mode, execution_id, status, required_flag, opened_by, opened_at, creator, updater, deleted, tenant_id
)
SELECT @batch_execution_id, COALESCE(bt.node_type, 'ROUTE_FORM'), COALESCE(e.route_process_id, wt.route_process_id, bt.route_process_id),
  COALESCE(bt.route_process_sort, 10), COALESCE(bt.process_id, rp.process_id), bt.process_code, COALESCE(bt.process_name, p.name),
  COALESCE(bt.batch_record_report_id, e.batch_record_report_id), COALESCE(bt.batch_record_report_name, e.template_name),
  COALESCE(bt.batch_record_definition_id, e.batch_record_definition_id), COALESCE(bt.batch_record_version_id, e.batch_record_version_id),
  COALESCE(bt.batch_record_sort, 1), bt.instance_scope, bt.shared_form_key, bt.form_slot_type, bt.record_category, bt.validation_profile,
  bt.permission_scope_id, bt.route_binding_id, bt.route_binding_snapshot_hash, bt.archive_visibility, COALESCE(bt.slot_config_snapshot_hash, e.slot_config_snapshot_hash),
  COALESCE(bt.execution_mode, 'SEQUENTIAL'), NULL, 40, b'1', 914520, NOW(), 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_edhr_work_task wt ON wt.id=@source_work_task_id
JOIN mes_pro_edhr_batch_execution_task bt ON bt.id=@source_batch_task_id
JOIN mes_pro_route_process rp ON rp.id=COALESCE(e.route_process_id, wt.route_process_id, bt.route_process_id)
JOIN mes_pro_process p ON p.id=COALESCE(bt.process_id, rp.process_id)
WHERE e.id=@source_execution_id;
SELECT JSON_OBJECT(
  'batchExecutionId', @batch_execution_id,
  'batchExecutionCode', CONCAT('BE-', @run_key),
  'batchCode', @run_key,
  'runKey', @run_key,
  'actionPath', CONCAT('/mes/pro/feedback/edhr-batch-execution?batchExecutionCode=', CONCAT('BE-', @run_key))
);
`)
  const rows = output.split(/\r?\n/).filter(Boolean)
  const source = parseJsonRow(rows[0] || '', 'source batch precondition')
  if (source.blocked) {
    throw new Error(source.blocked)
  }
  const setup = parseJsonRow(rows[rows.length - 1] || '', 'closed batch void setup')
  assert.ok(setup.batchExecutionId, `setup did not create batch execution: ${JSON.stringify(setup)}`)
  return setup
}

function loadExistingVoidSetup(batchExecutionId) {
  return parseJsonRow(
    mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'batchExecutionId', b.id,
  'batchExecutionCode', b.batch_execution_code,
  'batchCode', b.batch_code,
  'runKey', b.batch_code,
  'actionPath', CONCAT('/mes/pro/feedback/edhr-batch-execution?batchExecutionCode=', b.batch_execution_code)
)
FROM mes_pro_edhr_batch_execution b
WHERE b.id=${Number(batchExecutionId)}
  AND b.tenant_id=122
  AND b.deleted=0;
`),
    'existing eDHR batch void setup'
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

async function login(page, username = USERNAME, password = PASSWORD, target = '/index') {
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
  assert.ok([0, 200].includes(Number(payload.code)), `login failed: ${JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${BASE_URL}${target}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
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

async function authenticatedGet(page, endpoint, params, label) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, `${label} requires browser login token`)
  assert.equal(String(tenantId), EXPECTED_TENANT_ID, `${label} tenant-id mismatch: ${tenantId}`)
  const response = await page.request.get(`${BACKEND_URL}${endpoint}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    params
  })
  assert.equal(response.status(), 200, `${label} HTTP status must be 200`)
  return assertBusinessSuccess(await response.json(), label)
}

async function unwrapBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    throw new Error(`${label} wait failed: ${result.__error.message}`)
  }
  return result
}

async function fillOptionalStartUserSelectedAssignees(page, dialog) {
  const assigneeItems = dialog.locator('.el-form-item').filter({ hasText: /审批人/ })
  const itemCount = await assigneeItems.count()
  for (let index = 0; index < itemCount; index += 1) {
    const item = assigneeItems.nth(index)
    if (!(await item.isVisible().catch(() => false))) {
      continue
    }
    const currentText = (await item.innerText().catch(() => '')).trim()
    if (currentText.includes(String(APPROVER_USER_ID)) || currentText.includes(APPROVER_USERNAME)) {
      continue
    }
    await item.locator('.el-select, .el-input').first().click()
    const option = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: new RegExp(`${APPROVER_USER_ID}|${APPROVER_USERNAME}`) })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  }
}

async function selectReasonCategory(page, dialog) {
  const reasonItem = dialog.locator('.el-form-item').filter({ hasText: '原因分类' }).first()
  await reasonItem.locator('.el-select, .el-input').first().click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: '其他' }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
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
  const listPayload = await (await listResponsePromise).json()
  const listData = assertBusinessSuccess(listPayload, 'eDHR batch execution list')
  assert.ok(
    (listData.list || []).some((row) => String(row.id) === String(setup.batchExecutionId)),
    `target batch must be visible in list response: ${JSON.stringify(listData.list || [])}`
  )
  const row = page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: setup.batchExecutionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(row.getByRole('button', { name: '作废' }), 'void row action')

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '作废批次执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText(setup.batchExecutionCode, { exact: false }).waitFor({ state: 'visible', timeout: 30000 })
  await selectReasonCategory(page, dialog)
  await dialog
    .locator('.el-form-item')
    .filter({ hasText: '原因说明' })
    .first()
    .locator('textarea')
    .first()
    .fill(`M7 ${APPROVAL_MODE} eDHR批记录作废验证 ${RUN_ID}`)
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="电子签名密码"]'), SIGNATURE_PASSWORD, 'void signature password')
  const comment = dialog.locator('.el-form-item').filter({ hasText: '备注' }).first().locator('textarea').first()
  if (await comment.isVisible().catch(() => false)) {
    await comment.fill(`M7 ${APPROVAL_MODE} void ${RUN_ID}`)
  }
  await fillOptionalStartUserSelectedAssignees(page, dialog)

  const resolveResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/form-center/actions/resolve') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/form-center/instances') &&
      !response.url().includes('/submit') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/form-center/instances/') &&
      response.url().includes('/submit') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /提交作废流程/ }), 'submit void form')

  const resolution = assertBusinessSuccess(await (await resolveResponsePromise).json(), 'resolve eDHR batch void business action')
  assert.equal(resolution.approvalMode, APPROVAL_MODE, `resolved approvalMode mismatch: ${JSON.stringify(resolution)}`)
  assert.equal(
    resolution.requiresBpm,
    APPROVAL_MODE === 'BPM_REQUIRED',
    `resolved requiresBpm mismatch: ${JSON.stringify(resolution)}`
  )
  const created = assertBusinessSuccess(await (await createResponsePromise).json(), 'create eDHR batch void form instance')
  assert.ok(created.id, `created form instance id is missing: ${JSON.stringify(created)}`)
  const submitted = assertBusinessSuccess(await (await submitResponsePromise).json(), 'submit eDHR batch void form instance')
  assert.equal(String(submitted.id), String(created.id), `submitted form instance id mismatch: ${JSON.stringify(submitted)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
  return { resolution, created, submitted }
}

function loadVoidEvidence(batchExecutionId, instanceId) {
  return parseJsonRow(
    mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'batchExecutionId', b.id,
  'batchStatus', b.status,
  'activeContextKey', b.active_context_key,
  'changeEventId', ev.id,
  'changeStatus', ev.change_status,
  'changeCode', ev.change_code,
  'targetScope', ev.target_scope,
  'requestSignatureId', ev.request_signature_id,
  'requestSignatureActionType', sig.action_type,
  'requestedBy', ev.requested_by,
  'approvedBy', ev.approved_by,
  'bpmProcessInstanceId', ev.bpm_process_instance_id,
  'bpmTaskId', ev.bpm_task_id,
  'formInstanceId', fi.id,
  'formInstanceStatus', fi.status,
  'formInstanceBpmProcessInstanceId', fi.bpm_process_instance_id,
  'effectExecutionStatus', fe.status,
  'effectExecutionResultRef', fe.result_ref,
  'effectExecutionFailureReason', fe.failure_reason,
  'runningBpmTaskCount', (
    SELECT COUNT(*) FROM ACT_RU_TASK t
    WHERE ev.bpm_process_instance_id IS NOT NULL
      AND BINARY t.PROC_INST_ID_=BINARY ev.bpm_process_instance_id
  ),
  'historicProcessEndTime', (
    SELECT DATE_FORMAT(h.END_TIME_, '%Y-%m-%d %H:%i:%s') FROM ACT_HI_PROCINST h
    WHERE ev.bpm_process_instance_id IS NOT NULL
      AND BINARY h.PROC_INST_ID_=BINARY ev.bpm_process_instance_id
    LIMIT 1
  ),
  'approvalSignatureRecordCount', (
    SELECT COUNT(*) FROM bpm_approval_signature_record r
    WHERE ev.bpm_process_instance_id IS NOT NULL
      AND r.tenant_id=122 AND r.deleted=0
      AND BINARY r.process_instance_id=BINARY ev.bpm_process_instance_id
      AND r.review_result='APPROVE'
  ),
  'approvalSignatureRecordId', (
    SELECT MAX(r.id) FROM bpm_approval_signature_record r
    WHERE ev.bpm_process_instance_id IS NOT NULL
      AND r.tenant_id=122 AND r.deleted=0
      AND BINARY r.process_instance_id=BINARY ev.bpm_process_instance_id
      AND r.review_result='APPROVE'
  ),
  'approvalSignatureSignerUserId', (
    SELECT MAX(r.signer_user_id) FROM bpm_approval_signature_record r
    WHERE ev.bpm_process_instance_id IS NOT NULL
      AND r.tenant_id=122 AND r.deleted=0
      AND BINARY r.process_instance_id=BINARY ev.bpm_process_instance_id
      AND r.review_result='APPROVE'
  )
)
FROM mes_pro_edhr_batch_execution b
LEFT JOIN mes_pro_edhr_record_change_event ev
  ON ev.id = (
    SELECT MAX(ev2.id)
    FROM mes_pro_edhr_record_change_event ev2
    WHERE ev2.tenant_id=122
      AND ev2.deleted=0
      AND ev2.batch_execution_id=b.id
      AND ev2.target_scope='BATCH'
      AND ev2.change_type='VOID'
  )
LEFT JOIN mes_pro_edhr_batch_execution_signature sig ON sig.id=ev.request_signature_id
LEFT JOIN bpm_form_action_instance fi ON fi.id=${Number(instanceId || 0)} AND fi.tenant_id=122 AND fi.deleted=0
LEFT JOIN bpm_form_effect_execution fe
  ON fe.id = (
    SELECT MAX(fe2.id)
    FROM bpm_form_effect_execution fe2
    WHERE fe2.tenant_id=122
      AND fe2.deleted=0
      AND fe2.instance_id=fi.id
  )
WHERE b.id=${Number(batchExecutionId)}
  AND b.tenant_id=122
  AND b.deleted=0;
`),
    'eDHR batch void evidence'
  )
}

function waitForEvidence(label, batchExecutionId, instanceId, predicate) {
  let latest
  for (let attempt = 0; attempt < 30; attempt += 1) {
    latest = loadVoidEvidence(batchExecutionId, instanceId)
    if (predicate(latest)) {
      return latest
    }
    sleep(1000)
  }
  throw new Error(`${label} did not reach expected state: ${JSON.stringify(latest)}`)
}

function assertDirectVoidEvidence(evidence) {
  assert.equal(Number(evidence.batchStatus), 60, `DIRECT void must make batch terminal VOIDED: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.activeContextKey, null, `DIRECT void must clear active context key: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.changeStatus, 'EFFECTIVE', `DIRECT void change must be EFFECTIVE: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.targetScope, 'BATCH', `DIRECT void change must target BATCH: ${JSON.stringify(evidence)}`)
  assert.ok(evidence.requestSignatureId, `DIRECT void must write batch request signature: ${JSON.stringify(evidence)}`)
  assert.equal(
    evidence.requestSignatureActionType,
    'BATCH_VOID_REQUEST',
    `DIRECT void request signature action mismatch: ${JSON.stringify(evidence)}`
  )
  assert.equal(evidence.bpmProcessInstanceId, null, `DIRECT void must not create BPM process: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.formInstanceStatus, 'EFFECTIVE', `DIRECT form instance must be EFFECTIVE: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.formInstanceBpmProcessInstanceId, null, `DIRECT form instance must not bind BPM: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.effectExecutionStatus, 'APPLIED', `DIRECT effect must be applied: ${JSON.stringify(evidence)}`)
}

function assertBpmRequiredVoidEvidence(evidence) {
  assert.equal(Number(evidence.batchStatus), 30, `BPM_REQUIRED void must leave batch CLOSED before approval: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.changeStatus, 'SUBMITTED', `BPM_REQUIRED void change must be submitted: ${JSON.stringify(evidence)}`)
  assert.ok(evidence.requestSignatureId, `BPM_REQUIRED void must write request signature: ${JSON.stringify(evidence)}`)
  assert.ok(evidence.bpmProcessInstanceId, `BPM_REQUIRED void must create BPM process: ${JSON.stringify(evidence)}`)
  assert.equal(
    evidence.formInstanceBpmProcessInstanceId,
    evidence.bpmProcessInstanceId,
    `BPM_REQUIRED form instance must bind BPM process: ${JSON.stringify(evidence)}`
  )
  assert.equal(evidence.formInstanceStatus, 'IN_APPROVAL', `BPM_REQUIRED form instance must be IN_APPROVAL: ${JSON.stringify(evidence)}`)
  assert.ok(Number(evidence.runningBpmTaskCount) > 0, `BPM_REQUIRED void must have running BPM task: ${JSON.stringify(evidence)}`)
}

function assertBpmRequiredTerminalVoidEvidence(evidence) {
  assert.equal(Number(evidence.batchStatus), 60, `approved BPM_REQUIRED void must make batch VOIDED: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.activeContextKey, null, `approved BPM_REQUIRED void must clear active context key: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.changeStatus, 'EFFECTIVE', `approved BPM_REQUIRED void change must be EFFECTIVE: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.formInstanceStatus, 'EFFECTIVE', `approved BPM_REQUIRED form instance must be EFFECTIVE: ${JSON.stringify(evidence)}`)
  assert.equal(Number(evidence.runningBpmTaskCount), 0, `approved BPM_REQUIRED must have no running BPM tasks: ${JSON.stringify(evidence)}`)
  assert.ok(evidence.historicProcessEndTime, `approved BPM_REQUIRED historic process must be ended: ${JSON.stringify(evidence)}`)
  assert.ok(
    Number(evidence.approvalSignatureRecordCount) > 0,
    `approved BPM_REQUIRED must write unified approval signature: ${JSON.stringify(evidence)}`
  )
  assert.equal(
    Number(evidence.approvalSignatureSignerUserId),
    Number(APPROVER_USER_ID),
    `approved BPM_REQUIRED signer user mismatch: ${JSON.stringify(evidence)}`
  )
}

async function queryApprovalTodoByProcess(page, processInstanceId) {
  const pageData = await authenticatedGet(
    page,
    '/admin-api/approval-center/tasks/page',
    {
      pageNo: 1,
      pageSize: 200,
      viewType: 'TODO',
      moduleCode: 'BPM'
    },
    'eDHR batch void approval todo query'
  )
  const list = Array.isArray(pageData?.list) ? pageData.list : []
  const matchedIndex = list.findIndex((item) =>
    item?.moduleCode === 'BPM' &&
    item?.sourceTaskType === 'BPM_TASK_TODO' &&
    String(item?.processInstanceId) === String(processInstanceId) &&
    item?.businessStatus === 'TODO' &&
    Array.isArray(item?.availableActions) &&
    item.availableActions.includes('APPROVE')
  )
  const matched = matchedIndex >= 0 ? list[matchedIndex] : undefined
  assert.ok(matched, `approval todo must include void process=${processInstanceId}, list=${JSON.stringify(list.slice(0, 10))}`)
  return { ...matched, matchedIndex }
}

async function findApprovalRowByTask(page, approvalTask, setup) {
  const rows = page.locator('.el-table__body-wrapper .el-table__row')
  await rows.first().waitFor({ state: 'visible', timeout: 60000 })
  const rowTexts = await rows.evaluateAll((items) => items.map((item) => item.innerText || ''))
  const candidateRules = [
    { label: `processInstanceId=${approvalTask?.processInstanceId}`, match: (text) => text.includes(String(approvalTask?.processInstanceId || '')) },
    { label: `batchExecutionCode=${setup.batchExecutionCode}`, match: (text) => text.includes(setup.batchExecutionCode) },
    { label: `runKey=${setup.runKey}`, match: (text) => text.includes(setup.runKey) },
    { label: `businessCode=${approvalTask?.businessCode}`, match: (text) => text.includes(String(approvalTask?.businessCode || '')) },
    { label: `businessKey=${approvalTask?.businessKey}`, match: (text) => text.includes(String(approvalTask?.businessKey || '')) }
  ].filter((rule) => !rule.label.endsWith('='))
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
    `unable to locate eDHR batch void approval row: processInstanceId=${approvalTask?.processInstanceId}, rowTexts=${JSON.stringify(rowTexts)}`
  )
}

async function completeVoidApprovalFromTodo(page, setup, processInstanceId) {
  await login(page, APPROVER_USERNAME, APPROVER_PASSWORD, APPROVAL_ROUTE)
  const approvalTask = await queryApprovalTodoByProcess(page, processInstanceId)
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
      data: assertBusinessSuccess(await response.json(), 'eDHR batch void approval'),
      requestPayload: redactSensitiveRequestPayload(requestPayload)
    }
  }).catch((error) => ({ __error: error }))
  await clickFirstEnabled(approvalRow.getByRole('button', { name: /^审核$/ }), 'approval center review')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '审核确认' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText('审核通过', { exact: true }).click()
  await fillFirstVisible(dialog.locator('input[type="password"]'), APPROVER_SIGNATURE_PASSWORD, 'approval signature password')
  await clickFirstEnabled(dialog.getByRole('button', { name: /确认审核/ }), 'confirm approval')
  const reviewEnvelope = await unwrapBusinessWait(reviewResponse, 'eDHR batch void approval')
  assert.equal(
    String(reviewEnvelope.requestPayload?.processInstanceId),
    String(processInstanceId),
    `approval request must lock this process: ${JSON.stringify(reviewEnvelope.requestPayload)}`
  )
  assert.equal(reviewEnvelope.data, true, `approval center review must return true: ${JSON.stringify(reviewEnvelope.data)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
  return {
    reviewResult: reviewEnvelope.data,
    requestPayload: reviewEnvelope.requestPayload
  }
}

function writeArtifact(payload) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  const artifactPath = path.join(ARTIFACT_DIR, `edhr-batch-void-${RUN_ID}-${APPROVAL_MODE}.json`)
  fs.writeFileSync(artifactPath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return artifactPath
}

async function main() {
  assertLocalOnly()
  const resumeMode = RESUME_BATCH_EXECUTION_ID > 0
  const setup = resumeMode
    ? loadExistingVoidSetup(RESUME_BATCH_EXECUTION_ID)
    : prepareClosedBatchVoidSample()
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
  const writeRequests = []
  const observedResponses = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('request', (request) => {
    if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/')) {
      writeRequests.push({ method: request.method(), url: request.url() })
    }
  })
  page.on('response', (response) => {
    const url = response.url()
    if (
      url.includes('/form-center/') ||
      url.includes('/approval-center/') ||
      url.includes('/mes/pro/edhr-batch-execution')
    ) {
      observedResponses.push({ method: response.request().method(), status: response.status(), url })
    }
  })

  try {
    let submitSummary
    let evidence
    if (resumeMode) {
      evidence = waitForEvidence(
        'resumed BPM_REQUIRED eDHR batch void',
        setup.batchExecutionId,
        RESUME_FORM_INSTANCE_ID,
        (current) => current.formInstanceId != null && current.changeEventId != null
      )
    } else {
      await login(page, USERNAME, PASSWORD, setup.actionPath)
      submitSummary = await submitVoidThroughUi(page, setup)
      evidence = waitForEvidence(
        `eDHR batch void ${APPROVAL_MODE}`,
        setup.batchExecutionId,
        submitSummary.created.id,
        (current) => current.formInstanceId != null && current.changeEventId != null
      )
    }
    const formInstanceId = resumeMode ? RESUME_FORM_INSTANCE_ID : submitSummary.created.id
    let approvalSummary
    let terminalEvidence
    if (APPROVAL_MODE === 'DIRECT') {
      evidence = waitForEvidence(
        'DIRECT eDHR batch void terminal evidence',
        setup.batchExecutionId,
        formInstanceId,
        (current) => Number(current.batchStatus) === 60 && current.changeStatus === 'EFFECTIVE'
      )
      assertDirectVoidEvidence(evidence)
    } else {
      assertBpmRequiredVoidEvidence(evidence)
      if (SHOULD_COMPLETE_APPROVAL) {
        approvalSummary = await completeVoidApprovalFromTodo(page, setup, evidence.bpmProcessInstanceId)
        terminalEvidence = waitForEvidence(
          'BPM_REQUIRED eDHR batch void terminal evidence',
          setup.batchExecutionId,
          formInstanceId,
          (current) => Number(current.batchStatus) === 60 && current.changeStatus === 'EFFECTIVE' && Number(current.runningBpmTaskCount) === 0
        )
        assertBpmRequiredTerminalVoidEvidence(terminalEvidence)
      }
    }
    assert.deepEqual(pageErrors, [], 'eDHR batch void E2E must not produce page errors')
    const artifactPath = writeArtifact({
      status: 'PASS',
      approvalMode: APPROVAL_MODE,
      tenant: TENANT,
      username: USERNAME,
      approverUsername: SHOULD_COMPLETE_APPROVAL ? APPROVER_USERNAME : undefined,
      resumeMode,
      setup,
      submitSummary,
      evidence,
      approvalSummary,
      terminalEvidence,
      writeRequests,
      observedResponses,
      pageErrors
    })
    console.log(
      `PASS: eDHR batch void ${APPROVAL_MODE} E2E batch=${setup.batchExecutionId} status=${(terminalEvidence || evidence).batchStatus} process=${evidence.bpmProcessInstanceId} artifact=${artifactPath}`
    )
  } catch (error) {
    const artifactPath = writeArtifact({
      status: 'FAIL',
      approvalMode: APPROVAL_MODE,
      tenant: TENANT,
      username: USERNAME,
      resumeMode,
      setup,
      error: error.stack || error.message,
      writeRequests,
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
