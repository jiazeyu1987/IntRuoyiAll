const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.EDHR_EXEC_SUBMIT_REVIEW_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_EXEC_SUBMIT_REVIEW_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const TENANT = process.env.EDHR_EXEC_SUBMIT_REVIEW_TENANT || '测试租户'
const USERNAME = process.env.EDHR_EXEC_SUBMIT_REVIEW_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_EXEC_SUBMIT_REVIEW_PASSWORD || '111111'
const SIGNATURE_PASSWORD = process.env.EDHR_EXEC_SUBMIT_REVIEW_SIGNATURE_PASSWORD || PASSWORD
const APPROVAL_MODE = process.env.EDHR_EXEC_SUBMIT_REVIEW_APPROVAL_MODE || 'DIRECT'
const RUN_ID = process.env.EDHR_EXEC_SUBMIT_REVIEW_RUN_ID || new Date().toISOString().replace(/\D/g, '').slice(0, 14)
const APPROVER_USER_ID = Number(process.env.EDHR_EXEC_SUBMIT_REVIEW_APPROVER_USER_ID || '912398')
const APPROVER_USERNAME = process.env.EDHR_EXEC_SUBMIT_REVIEW_APPROVER_USERNAME || USERNAME
const APPROVER_PASSWORD = process.env.EDHR_EXEC_SUBMIT_REVIEW_APPROVER_PASSWORD || PASSWORD
const APPROVER_SIGNATURE_PASSWORD =
  process.env.EDHR_EXEC_SUBMIT_REVIEW_APPROVER_SIGNATURE_PASSWORD ||
  process.env.EDHR_EXEC_SUBMIT_REVIEW_SIGNATURE_PASSWORD ||
  ''
const SHOULD_COMPLETE_APPROVAL = process.env.EDHR_EXEC_SUBMIT_REVIEW_COMPLETE_APPROVAL === '1'
const EXPECTED_TENANT_ID = '122'
const APPROVAL_ROUTE = '/approval-center/todo'
const ARTIFACT_DIR = path.resolve(
  process.env.EDHR_EXEC_SUBMIT_REVIEW_ARTIFACT_DIR ||
    path.join(
      WORKSPACE_ROOT,
      'doc/tasks/20260721-batch-record-bpm-toggle-implementation/e2e-artifacts/batch-execution-submit-review'
    )
)

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'batch execution submit review E2E must use local frontend')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'batch execution submit review E2E must use local backend')
  assert.equal(TENANT, '测试租户', 'write E2E must use test tenant')
  assert.equal(USERNAME, 'aoteman', 'write E2E must use test tenant aoteman')
  assert.ok(['DIRECT', 'BPM_REQUIRED'].includes(APPROVAL_MODE), 'APPROVAL_MODE must be DIRECT or BPM_REQUIRED')
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

async function unwrapBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    throw new Error(`${label} wait failed: ${result.__error.message}`)
  }
  return result
}

function prepareLegacyReviewExecution() {
  const runKey = `M7-EDHR-EXEC-${APPROVAL_MODE}-${RUN_ID}`
  const signatureCellKey = `${runKey}-REVIEW`
  const snapshot = {
    snapshotVersion: 'EDHR_EXECUTION_V1',
    from: 'm7-bpm-policy-real-e2e',
    layout: {
      rows: {
        1: {
          cells: {
            1: {
              text: 'M7 批记录执行提交审核',
              edhrSignature: {
                enabled: true,
                actionType: 'APPROVE',
                signatureCellKey,
                reviewSourceType: 'USER',
                reviewSourceId: APPROVER_USER_ID,
                reviewSourceName: 'M7 审核人'
              }
            }
          }
        },
        2: {
          cells: {
            1: {
              text: 'M7提交审核字段'
            }
          }
        }
      }
    },
    fields: [
      {
        rowIndex: 2,
        columnIndex: 1,
        fieldKey: `${runKey}-FIELD`,
        fieldPath: `${runKey}.field`,
        label: 'M7提交审核字段',
        required: false,
        componentType: 'input',
        inputType: 'text',
        valueType: 'STRING'
      }
    ]
  }
  const sheetLayout = snapshot.layout
  const output = mysql(`
SET NAMES utf8mb4;
SET @tenant_id := 122;
SET @run_key := ${sqlString(runKey)};
SET @snapshot := ${sqlString(JSON.stringify(snapshot))};
SET @sheet_layout := ${sqlString(JSON.stringify(sheetLayout))};
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
  10, 1, 0, 0, NULL, 'M7 submit review real E2E batch execution',
  'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_edhr_work_task wt ON wt.id=@source_work_task_id
JOIN mes_pro_edhr_batch_execution be ON be.id=@source_batch_execution_id
JOIN mes_pro_route_process rp ON rp.id=COALESCE(e.route_process_id, wt.route_process_id)
WHERE e.id=@source_execution_id;
SET @batch_execution_id := LAST_INSERT_ID();
INSERT INTO mes_pro_batch_record_execution (
  execution_code, template_id, template_code, template_name, work_order_id, work_order_code,
  route_id, route_process_id, task_id, workstation_id, batch_record_report_id, batch_record_definition_id,
  batch_record_version_id, batch_execution_id, batch_code, status, sheet_layout_json, meta_json, execution_snapshot_json,
  cell_values_json, cell_values_hash, field_audit_revision, field_audit_head_hash, revision_no,
  active_revision_flag, record_category, validation_profile, slot_config_snapshot_hash, remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('BRE-', @run_key), e.template_id, e.template_code, e.template_name, e.work_order_id, e.work_order_code,
  COALESCE(e.route_id, rp.route_id), COALESCE(e.route_process_id, wt.route_process_id), NULL, e.workstation_id, e.batch_record_report_id, e.batch_record_definition_id,
  e.batch_record_version_id, @batch_execution_id, @run_key, 0, @sheet_layout, '{}', @snapshot,
  '[]', '84b9a938bd9a94b26da55f087f6a2fab21c438a2333cfb6d45fc85e34388690b', 0, 'c89790f1db795880e667042c652ac63aaba03b9a91c1a14ae34c7d0fbf855a42', 1,
  b'1', 'INTERNAL_RECORD', 'INTERNAL_TRACE', e.slot_config_snapshot_hash, 'M7 submit review real E2E', 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_edhr_work_task wt ON wt.id=@source_work_task_id
JOIN mes_pro_route_process rp ON rp.id=COALESCE(e.route_process_id, wt.route_process_id)
WHERE e.id=@source_execution_id;
SET @execution_id := LAST_INSERT_ID();
UPDATE mes_pro_batch_record_execution
SET revision_root_execution_id=@execution_id
WHERE id=@execution_id;
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
  COALESCE(bt.execution_mode, 'SEQUENTIAL'), @execution_id, 10, b'1', 914520, NOW(), 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_edhr_work_task wt ON wt.id=@source_work_task_id
JOIN mes_pro_edhr_batch_execution_task bt ON bt.id=@source_batch_task_id
JOIN mes_pro_route_process rp ON rp.id=COALESCE(e.route_process_id, wt.route_process_id, bt.route_process_id)
JOIN mes_pro_process p ON p.id=COALESCE(bt.process_id, rp.process_id)
WHERE e.id=@source_execution_id;
SET @batch_task_id := LAST_INSERT_ID();
UPDATE mes_pro_batch_record_execution
SET task_id=@batch_task_id
WHERE id=@execution_id;
INSERT INTO mes_pro_edhr_work_task_assignment_rule (
  route_process_id, scope_type, scope_id, task_type, assignee_user_id, review_user_id,
  candidate_source_type, candidate_source_id, due_minutes, enabled, remark, creator, updater, deleted, tenant_id
)
SELECT e.route_process_id, 'ROUTE_PROCESS', e.route_process_id, 'REVIEW', ${Number(APPROVER_USER_ID)}, ${Number(APPROVER_USER_ID)},
  'USER', ${Number(APPROVER_USER_ID)}, 1440, b'1', 'M7 submit review real E2E review rule',
  'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
WHERE e.id=@execution_id
ON DUPLICATE KEY UPDATE
  assignee_user_id=VALUES(assignee_user_id),
  review_user_id=VALUES(review_user_id),
  candidate_source_type=VALUES(candidate_source_type),
  candidate_source_id=VALUES(candidate_source_id),
  due_minutes=VALUES(due_minutes),
  enabled=b'1',
  remark=VALUES(remark),
  updater='codex';
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id,
  process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, signature_cell_key, status, due_time, action_url, remark, creator, updater, deleted, tenant_id
)
SELECT CONCAT('EDHRT-', @run_key), 'FILL', @batch_execution_id, @batch_task_id, 'BATCH_TASK', @batch_task_id,
  @execution_id, e.work_order_id, e.work_order_code, e.batch_code, rp.route_id, e.route_process_id, rp.process_id,
  p.name, 914520, 'USER', 914520, '914520',
  914520, '', 'TODO', DATE_ADD(NOW(), INTERVAL 1 DAY),
  CONCAT('/mes/pro/feedback/edhr-execution/form?id=', @execution_id, '&workTaskId=', LAST_INSERT_ID()),
  'M7 submit review real E2E fill task', 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_route_process rp ON rp.id=e.route_process_id
JOIN mes_pro_process p ON p.id=rp.process_id
WHERE e.id=@execution_id;
SET @fill_task_id := LAST_INSERT_ID();
UPDATE mes_pro_edhr_work_task
SET action_url=CONCAT('/mes/pro/feedback/edhr-execution/form?id=', @execution_id, '&workTaskId=', @fill_task_id)
WHERE id=@fill_task_id;
SELECT JSON_OBJECT(
  'executionId', @execution_id,
  'fillTaskId', @fill_task_id,
  'actionUrl', CONCAT('/mes/pro/feedback/edhr-execution/form?id=', @execution_id, '&workTaskId=', @fill_task_id),
  'signatureCellKey', ${sqlString(signatureCellKey)},
  'runKey', @run_key
);
`)
  const rows = output.split(/\r?\n/).filter(Boolean)
  const source = parseJsonRow(rows[0] || '', 'source execution precondition')
  if (source.blocked) {
    throw new Error(source.blocked)
  }
  return parseJsonRow(rows[rows.length - 1] || '', 'legacy review execution setup')
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

async function closeResultDialogIfVisible(page, expectedText) {
  const dialog = page
    .locator('.edhr-fill-workspace__result-dialog .el-dialog:visible, .el-dialog:visible')
    .filter({ hasText: expectedText })
    .first()
  if (!(await dialog.isVisible().catch(() => false))) {
    return false
  }
  await clickFirstEnabled(dialog.getByRole('button', { name: /^确认$/ }), `close ${expectedText} result`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => undefined)
  return true
}

async function fillAndSaveExecutionValue(page, setup) {
  const sampleValue = `${setup.runKey}-已提交内容`
  const originalModeButton = page.locator('button:visible').filter({ hasText: '原表模式' }).first()
  if (await originalModeButton.isVisible().catch(() => false)) {
    await originalModeButton.click({ force: true })
  }
  await page.locator('.edhr-fill-workspace__form, .edhr-page-shell__legacy-form').first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  const fieldInputs = page.locator(
    [
      '.edhr-fill-workspace__form .edhr-fill-workspace__field input.el-input__inner:not([type="password"])',
      '.edhr-fill-workspace__form .edhr-fill-workspace__field textarea',
      '.edhr-fill-workspace input.el-input__inner:not([type="password"]):not([readonly])',
      '.edhr-fill-workspace textarea:not([readonly])',
      '.edhr-template-editable-form__editable-cell input.el-input__inner:not([type="password"])',
      '.edhr-template-editable-form__editable-cell textarea',
      '.edhr-page-shell__legacy-form input.el-input__inner:not([type="password"])',
      '.edhr-page-shell__legacy-form textarea'
    ].join(', ')
  )
  await fillFirstVisible(fieldInputs, sampleValue, 'submitted content sample field')
  await page.getByText('待保存变更', { exact: true }).first().waitFor({ state: 'visible', timeout: 30000 })
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/field-audit/save-changes') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickFirstEnabled(page.getByRole('button', { name: '保存变更' }), 'save field changes')
  const saveResponse = await saveResponsePromise
  assert.equal(saveResponse.status(), 200, 'field audit save HTTP status must be 200')
  const saveBody = await saveResponse.json()
  assert.ok([0, 200].includes(Number(saveBody.code)), `field audit save failed: ${JSON.stringify(saveBody)}`)
  assert.equal(
    saveBody.data?.hashVerification?.status,
    'VALID',
    `field audit hash verification must be valid: ${JSON.stringify(saveBody)}`
  )
  await closeResultDialogIfVisible(page, '已保存')
  await page.getByDisplayValue(sampleValue).first().waitFor({ state: 'visible', timeout: 60000 })
  return {
    sampleValue,
    auditBatchId: saveBody.data?.auditBatchId,
    fieldAuditRevision: saveBody.data?.fieldAuditRevision,
    cellValuesHash: saveBody.data?.cellValuesHash
  }
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

async function submitExecutionThroughUi(page, setup) {
  const loadResponses = []
  const browserErrors = []
  page.on('pageerror', (error) => {
    browserErrors.push(`pageerror: ${error.message}`)
  })
  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      browserErrors.push(`${message.type()}: ${message.text()}`)
    }
  })
  page.on('response', async (response) => {
    if (response.url().includes('/admin-api/mes/pro/batch-record-execution/get')) {
      loadResponses.push({
        status: response.status(),
        body: (await response.text().catch(() => '')).slice(0, 4000)
      })
    }
  })
  await page.goto(`${BASE_URL}${setup.actionUrl}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page
    .waitForResponse((response) => response.url().includes('/admin-api/mes/pro/batch-record-execution/get'), {
      timeout: 60000
    })
    .catch(() => undefined)
  await page.locator('.edhr-fill-workspace').first().waitFor({ state: 'visible', timeout: 60000 })
  const submitButtonCount = await page.getByRole('button', { name: '提交执行' }).count()
  if (submitButtonCount === 0) {
    const bodyText = (await page.locator('body').innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
    throw new Error(
      `submit execution button not rendered; loadResponses=${JSON.stringify(loadResponses)}; browserErrors=${JSON.stringify(browserErrors)}; pageText=${bodyText.slice(0, 1000)}`
    )
  }
  await clickFirstEnabled(page.getByRole('button', { name: '提交执行' }), 'submit execution')
  const dialog = page
    .locator('.edhr-fill-workspace__submit-sign-dialog .el-dialog:visible, .el-dialog:visible')
    .filter({ hasText: /电子签名|提交 eDHR 执行/ })
    .first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const selects = dialog.locator('.edhr-page-shell__submit-select')
  const selectCount = await selects.count()
  for (let index = 0; index < selectCount; index += 1) {
    const select = selects.nth(index)
    const selectedText = (await select.innerText().catch(() => '')).trim()
    if (!selectedText.includes(String(APPROVER_USER_ID))) {
      await select.click()
      const option = page
        .locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
        .filter({ hasText: String(APPROVER_USER_ID) })
        .first()
      await option.waitFor({ state: 'visible', timeout: 30000 })
      await option.click()
    }
  }
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), SIGNATURE_PASSWORD, 'submit password')
  const comment = dialog.locator('textarea').first()
  if ((await comment.count()) > 0 && (await comment.isVisible().catch(() => false))) {
    await comment.fill(`M7 ${APPROVAL_MODE} submit review ${RUN_ID}`)
  }
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/submit') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确\s*认(?:\s*提\s*交)?/ }), 'confirm submit')
  const response = await responsePromise
  assert.equal(response.status(), 200, 'submit HTTP status must be 200')
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `submit business response failed: ${JSON.stringify(body)}`)
  return body.data
}

async function queryApprovalTodoByProcess(page, evidence) {
  assert.ok(evidence.processInstanceId, `approval todo query requires process instance: ${JSON.stringify(evidence)}`)
  const pageData = await authenticatedGet(
    page,
    '/admin-api/approval-center/tasks/page',
    {
      pageNo: 1,
      pageSize: 200,
      viewType: 'TODO',
      moduleCode: 'BPM'
    },
    'batch execution submit review approval todo query'
  )
  const list = Array.isArray(pageData?.list) ? pageData.list : []
  const matchedIndex = list.findIndex((item) =>
    item?.moduleCode === 'BPM' &&
    item?.sourceTaskType === 'BPM_TASK_TODO' &&
    String(item?.processInstanceId) === String(evidence.processInstanceId) &&
    item?.businessStatus === 'TODO' &&
    Array.isArray(item?.availableActions) &&
    item.availableActions.includes('APPROVE')
  )
  const matched = matchedIndex >= 0 ? list[matchedIndex] : undefined
  assert.ok(matched, `approval todo must include execution process=${evidence.processInstanceId}, list=${JSON.stringify(list.slice(0, 10))}`)
  return { ...matched, matchedIndex }
}

function hasTemplatePlaceholders(value) {
  return /\$\{[^}]+}/.test(String(value || ''))
}

async function findApprovalRowByTask(page, approvalTask, setup) {
  const rows = page.locator('.el-table__body-wrapper .el-table__row')
  await rows.first().waitFor({ state: 'visible', timeout: 60000 })
  const rowTexts = await rows.evaluateAll((items) => items.map((item) => item.innerText || ''))
  const title = String(approvalTask?.businessTitle || '').trim()
  const businessCode = String(approvalTask?.businessCode || '').trim()
  const businessKey = String(approvalTask?.businessKey || '').trim()
  const sourceTaskId = String(approvalTask?.sourceTaskId || '').trim()
  const runKey = String(setup?.runKey || '').trim()
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
  if (runKey) {
    candidateRules.push({ label: `runKey=${runKey}`, match: (text) => text.includes(runKey) })
  }
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

async function completeExecutionApprovalFromTodo(page, setup, evidence) {
  let activeSignaturePassword = SIGNATURE_PASSWORD
  let approvalTask
  if (APPROVER_USERNAME !== USERNAME) {
    await login(page, APPROVER_USERNAME, APPROVER_PASSWORD, APPROVAL_ROUTE)
    activeSignaturePassword = APPROVER_SIGNATURE_PASSWORD
  }
  approvalTask = await queryApprovalTodoByProcess(page, evidence)
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
      data: assertBusinessSuccess(await response.json(), 'batch execution submit review approval'),
      requestPayload: redactSensitiveRequestPayload(requestPayload)
    }
  }).catch((error) => ({ __error: error }))
  await clickFirstEnabled(approvalRow.getByRole('button', { name: /^审核$/ }), 'approval center review')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '审核确认' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText('审核通过', { exact: true }).click()
  await fillFirstVisible(dialog.locator('input[type="password"]'), activeSignaturePassword, 'approval signature password')
  await clickFirstEnabled(dialog.getByRole('button', { name: /确认审核/ }), 'confirm approval')
  const reviewEnvelope = await unwrapBusinessWait(reviewResponse, 'batch execution submit review approval')
  assert.equal(
    String(reviewEnvelope.requestPayload?.processInstanceId),
    String(evidence.processInstanceId),
    `approval request must lock this process: ${JSON.stringify(reviewEnvelope.requestPayload)}`
  )
  assert.equal(reviewEnvelope.data, true, `approval center review must return true: ${JSON.stringify(reviewEnvelope.data)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
  return {
    reviewResult: reviewEnvelope.data,
    requestPayload: reviewEnvelope.requestPayload
  }
}

function loadExecutionEvidence(executionId) {
  return parseJsonRow(
    mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'executionId', e.id,
  'status', e.status,
  'processDefinitionKey', e.process_definition_key,
  'processInstanceId', e.process_instance_id,
  'submittedBy', e.submitted_by,
  'approvedBy', e.approved_by,
  'closedAt', DATE_FORMAT(e.closed_at, '%Y-%m-%d %H:%i:%s'),
  'snapshotStatus', s.approval_status,
  'snapshotProcessInstanceId', s.process_instance_id,
  'snapshotCurrentBpmTaskId', s.current_bpm_task_id,
  'submitSignatureId', s.submit_signature_id,
  'reviewTaskCount', (
    SELECT COUNT(*) FROM mes_pro_edhr_work_task wt
    WHERE wt.tenant_id=122 AND wt.deleted=0 AND wt.execution_id=e.id AND wt.task_type='REVIEW'
  ),
  'reviewTaskDoneCount', (
    SELECT COUNT(*) FROM mes_pro_edhr_work_task wt
    WHERE wt.tenant_id=122 AND wt.deleted=0 AND wt.execution_id=e.id AND wt.task_type='REVIEW' AND wt.status='DONE'
  ),
  'reviewTaskTodoCount', (
    SELECT COUNT(*) FROM mes_pro_edhr_work_task wt
    WHERE wt.tenant_id=122 AND wt.deleted=0 AND wt.execution_id=e.id AND wt.task_type='REVIEW' AND wt.status IN ('TODO','DOING')
  ),
  'runningBpmTaskCount', (
    SELECT COUNT(*) FROM ACT_RU_TASK t WHERE BINARY t.PROC_INST_ID_=BINARY e.process_instance_id
  ),
  'historicProcessEndTime', (
    SELECT DATE_FORMAT(h.END_TIME_, '%Y-%m-%d %H:%i:%s') FROM ACT_HI_PROCINST h WHERE BINARY h.PROC_INST_ID_=BINARY e.process_instance_id LIMIT 1
  ),
  'approvalSignatureRecordCount', (
    SELECT COUNT(*) FROM bpm_approval_signature_record r
    WHERE r.tenant_id=122 AND r.deleted=0 AND BINARY r.process_instance_id=BINARY e.process_instance_id AND r.review_result='APPROVE'
  ),
  'approvalSignatureRecordId', (
    SELECT MAX(r.id) FROM bpm_approval_signature_record r
    WHERE r.tenant_id=122 AND r.deleted=0 AND BINARY r.process_instance_id=BINARY e.process_instance_id AND r.review_result='APPROVE'
  ),
  'approvalSignatureSignerUserId', (
    SELECT MAX(r.signer_user_id) FROM bpm_approval_signature_record r
    WHERE r.tenant_id=122 AND r.deleted=0 AND BINARY r.process_instance_id=BINARY e.process_instance_id AND r.review_result='APPROVE'
  )
)
FROM mes_pro_batch_record_execution e
LEFT JOIN mes_pro_batch_record_approval_snapshot s ON s.execution_id=e.id
WHERE e.id=${Number(executionId)} AND e.tenant_id=122;
`),
    'execution evidence'
  )
}

function assertDirectEvidence(evidence) {
  assert.equal(Number(evidence.status), 3, `DIRECT execution must be approved: ${JSON.stringify(evidence)}`)
  assert.equal(
    evidence.processDefinitionKey,
    'mes-edhr-approval-v1',
    `DIRECT execution must keep the configured process key for audit: ${JSON.stringify(evidence)}`
  )
  assert.equal(evidence.processInstanceId, null, `DIRECT execution must not have BPM process: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.snapshotProcessInstanceId, null, `DIRECT snapshot must not have BPM process: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.snapshotStatus, 'APPROVED', `DIRECT snapshot must be approved: ${JSON.stringify(evidence)}`)
  assert.equal(Number(evidence.reviewTaskCount), 0, `DIRECT must not create review work tasks: ${JSON.stringify(evidence)}`)
}

function assertBpmRequiredEvidence(evidence) {
  assert.equal(Number(evidence.status), 1, `BPM_REQUIRED execution must be submitted: ${JSON.stringify(evidence)}`)
  assert.equal(
    evidence.processDefinitionKey,
    'mes-edhr-approval-v1',
    `BPM_REQUIRED execution must use the configured process key: ${JSON.stringify(evidence)}`
  )
  assert.ok(evidence.processInstanceId, `BPM_REQUIRED execution must have a BPM process: ${JSON.stringify(evidence)}`)
  assert.equal(
    evidence.snapshotProcessInstanceId,
    evidence.processInstanceId,
    `BPM_REQUIRED snapshot must bind the BPM process: ${JSON.stringify(evidence)}`
  )
  assert.equal(evidence.snapshotStatus, 'SUBMITTED', `BPM_REQUIRED snapshot must be submitted: ${JSON.stringify(evidence)}`)
  assert.ok(Number(evidence.reviewTaskCount) > 0, `BPM_REQUIRED must create review work tasks: ${JSON.stringify(evidence)}`)
  assert.ok(Number(evidence.runningBpmTaskCount) > 0, `BPM_REQUIRED must leave a running BPM task: ${JSON.stringify(evidence)}`)
}

function assertBpmRequiredTerminalEvidence(evidence) {
  assert.equal(Number(evidence.status), 3, `BPM_REQUIRED approved execution must be approved: ${JSON.stringify(evidence)}`)
  assert.ok(evidence.processInstanceId, `BPM_REQUIRED terminal evidence must keep BPM process id: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.snapshotStatus, 'APPROVED', `BPM_REQUIRED snapshot must be approved: ${JSON.stringify(evidence)}`)
  assert.equal(evidence.snapshotCurrentBpmTaskId, null, `BPM_REQUIRED snapshot BPM task must be cleared: ${JSON.stringify(evidence)}`)
  assert.equal(Number(evidence.runningBpmTaskCount), 0, `BPM_REQUIRED running BPM task count must be 0: ${JSON.stringify(evidence)}`)
  assert.ok(evidence.historicProcessEndTime, `BPM_REQUIRED historic process must be ended: ${JSON.stringify(evidence)}`)
  assert.ok(Number(evidence.reviewTaskCount) > 0, `BPM_REQUIRED terminal evidence must keep review tasks: ${JSON.stringify(evidence)}`)
  assert.equal(
    Number(evidence.reviewTaskDoneCount),
    Number(evidence.reviewTaskCount),
    `BPM_REQUIRED review work tasks must be DONE: ${JSON.stringify(evidence)}`
  )
  assert.equal(Number(evidence.reviewTaskTodoCount), 0, `BPM_REQUIRED review work tasks must have no TODO/DOING: ${JSON.stringify(evidence)}`)
  assert.ok(Number(evidence.approvalSignatureRecordCount) > 0, `BPM_REQUIRED approval must write unified signature record: ${JSON.stringify(evidence)}`)
  assert.equal(
    Number(evidence.approvalSignatureSignerUserId),
    Number(APPROVER_USER_ID),
    `BPM_REQUIRED signature signer must be the approver: ${JSON.stringify(evidence)}`
  )
}

function writeArtifact(payload) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  const artifactPath = path.join(
    ARTIFACT_DIR,
    `edhr-batch-execution-submit-review-${RUN_ID}-${APPROVAL_MODE}.json`
  )
  fs.writeFileSync(artifactPath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return artifactPath
}

async function main() {
  assertLocalOnly()
  const setup = prepareLegacyReviewExecution()
  const browser = await chromium.launch({ headless: process.env.EDHR_EXEC_SUBMIT_REVIEW_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    await login(page, USERNAME, PASSWORD, setup.actionUrl)
    const fieldSave = await fillAndSaveExecutionValue(page, setup)
    await submitExecutionThroughUi(page, setup)
    const evidence = loadExecutionEvidence(setup.executionId)
    let approvalSummary
    let terminalEvidence
    if (APPROVAL_MODE === 'DIRECT') {
      assertDirectEvidence(evidence)
    } else {
      assertBpmRequiredEvidence(evidence)
      if (SHOULD_COMPLETE_APPROVAL) {
        approvalSummary = await completeExecutionApprovalFromTodo(page, setup, evidence)
        terminalEvidence = loadExecutionEvidence(setup.executionId)
        assertBpmRequiredTerminalEvidence(terminalEvidence)
      }
    }
    const artifactPath = writeArtifact({
      approvalMode: APPROVAL_MODE,
      tenant: TENANT,
      username: USERNAME,
      approverUsername: SHOULD_COMPLETE_APPROVAL ? APPROVER_USERNAME : undefined,
      setup,
      fieldSave,
      evidence,
      approvalSummary,
      terminalEvidence
    })
    console.log(
      `PASS: eDHR batch execution submit review ${APPROVAL_MODE} E2E execution=${setup.executionId} status=${evidence.status} process=${evidence.processInstanceId} artifact=${artifactPath}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
