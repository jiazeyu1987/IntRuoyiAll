const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_T7_E2E_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_T7_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.EDHR_T7_E2E_LOGIN_PASSWORD || '111111'
const SIGNATURE_PASSWORD = process.env.EDHR_T7_E2E_SIGNATURE_PASSWORD
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.EDHR_T7_E2E_ADMIN_PASSWORD || 'admin123'
const EXECUTION_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const WORK_TASK_ROUTE = '/mes/pro/feedback/edhr-work-task'
const TASK_STATE_PATH = path.resolve(__dirname, '../../../doc/tasks/20260613-batch-record-gap-implementation/task-state.json')
const RUN_KEY = `T7-REVIEW-ASSIGNEE-${Date.now()}`
const ROLE_ID = Number(process.env.EDHR_T7_E2E_REVIEW_ROLE_ID || 111)

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'T7 E2E must use local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'T7 E2E must use local backend 48081')
}

function requireT6RealGate() {
  const state = JSON.parse(fs.readFileSync(TASK_STATE_PATH, 'utf8'))
  const t6 = state.tasks.find((task) => task.task_id === 'T6')
  if (!t6) {
    throw blocked('task-state.json 缺少 T6 状态，不能判断 T7 真实 E2E 依赖门禁。')
  }
  if (t6.status !== 'validated_real_e2e_pass') {
    throw blocked('T7 真实 E2E 依赖 T6 真实写入门禁先通过。', [
      `当前 T6 状态：${t6.status}`,
      `当前 T6 结果：${t6.last_outcome || '--'}`,
      '请先提供 EDHR_T6_E2E_SIGNATURE_PASSWORD 并通过 T6 real E2E。'
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
  const signatureCellKey = `${RUN_KEY}-R1C1`
  const snapshot = {
    snapshotVersion: 'EDHR_EXECUTION_V1',
    from: 't7-review-assignee-real-e2e',
    layout: {
      rows: {
        1: {
          cells: {
            1: {
              text: 'T7 审核候选选择',
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
        fieldKey: `${RUN_KEY}-FIELD-A`,
        fieldPath: `${RUN_KEY}.fieldA`,
        label: 'T7审核候选真实字段',
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
SET @run_key := ${sqlString(RUN_KEY)};
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
  'T7审核候选真实E2E', 'codex', 'codex', b'0', @tenant_id
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
  'T7审核候选真实E2E填写任务', 'codex', 'codex', b'0', @tenant_id
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
  const first = parseJsonRow(rows[0] || '', 'T7 source execution precondition')
  if (first?.blocked) {
    throw blocked(first.blocked)
  }
  const result = parseJsonRow(rows[rows.length - 1] || '', 'T7 execution setup')
  if (!result?.executionId || !result?.fillTaskId) {
    throw blocked(`T7 真实数据准备未返回 executionId/fillTaskId：${JSON.stringify(result)}`)
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
  'reviewSourceId', review_source_id
)), JSON_ARRAY())
FROM mes_pro_edhr_work_task
WHERE tenant_id=122
  AND execution_id=${Number(executionId)}
  AND task_type='REVIEW'
  AND signature_cell_key=${sqlString(signatureCellKey)};
`)
  return parseJsonRow(output, 'T7 review tasks') || []
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
  assert.ok(page.url().includes(`workTaskId=${setup.fillTaskId}`), 'T7 actionUrl must carry workTaskId')
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
    await commentInput.fill(`T7 missing selection ${RUN_KEY}`)
  }
  const beforeRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/pro/batch-record-execution/submit')) {
      beforeRequests.push(request)
    }
  })
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 定|确 认|确认/ }), 'confirm submit without selection')
  await page.getByText('请选择审核/批准人').first().waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(beforeRequests.length, 0, 'missing review assignee selection must be blocked by page before submit request')
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
    blockers.push('缺少 EDHR_T7_E2E_SIGNATURE_PASSWORD，不能执行提交签名真实 E2E。')
  }
  try {
    requireT6RealGate()
  } catch (error) {
    if (error.blocked) {
      blockers.push(error.message)
      blockers.push(...(error.details || []))
    } else {
      throw error
    }
  }
  if (blockers.length > 0) {
    throw blocked('T7 real E2E 前置条件未满足。', blockers)
  }

  const setup = prepareExecutionWithReviewCandidates()
  const browser = await chromium.launch({ headless: process.env.EDHR_T7_E2E_HEADED !== '1' })
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
      await commentInput.fill(`T7 review assignee real E2E ${RUN_KEY}`)
    }
    const selectedUserId = await chooseFirstReviewCandidate(page, dialog)
    await submitWithSelection(page, dialog, setup, selectedUserId)
    const evidence = await verifySubmissionEvidence(setup, selectedUserId)
    await verifyAdminReadonly(browser)
    console.log(`PASS: T7 review assignee real E2E runKey=${RUN_KEY} execution=${setup.executionId} selectedUser=${selectedUserId}`)
    console.log(`PASS: approvalSnapshot=${evidence.snapshot.id} reviewTask=${evidence.reviewTasks[0].id}`)
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
