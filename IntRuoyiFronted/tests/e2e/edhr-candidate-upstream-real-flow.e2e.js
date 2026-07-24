const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_CANDIDATE_E2E_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_CANDIDATE_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.EDHR_CANDIDATE_E2E_PASSWORD || 'admin123'
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.EDHR_CANDIDATE_E2E_ADMIN_PASSWORD || 'admin123'
const EXECUTION_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const WORK_TASK_ROUTE = '/mes/pro/feedback/edhr-work-task'
const RUN_KEY = `E2E-CANDIDATE-UPSTREAM-${Date.now()}`
const ROLE_ID = 111

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'E2E must use local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'E2E must use local backend 48081')
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
  assert.ok(line, `${label} must return one JSON row`)
  return JSON.parse(line)
}

function prepareDraftExecution() {
  const snapshot = {
    snapshotVersion: 'EDHR_EXECUTION_V1',
    from: 'candidate-upstream-e2e',
    layout: {
      rows: {
        1: {
          cells: {
            1: {
              text: '候选审核签字格',
              edhrSignature: {
                enabled: true,
                actionType: 'APPROVE',
                signatureCellKey: `${RUN_KEY}-R1C1`,
                reviewSourceType: 'ROLE',
                reviewSourceId: ROLE_ID,
                reviewSourceName: '租户管理员'
              }
            }
          }
        }
      }
    },
    fields: []
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
  'T5候选池上游真实E2E', 'codex', 'codex', b'0', @tenant_id
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
  'T5候选池上游真实E2E填写任务', 'codex', 'codex', b'0', @tenant_id
FROM mes_pro_batch_record_execution e
JOIN mes_pro_route_process rp ON rp.id=e.route_process_id
JOIN mes_pro_process p ON p.id=rp.process_id
WHERE e.id=@execution_id;
SET @fill_task_id := LAST_INSERT_ID();
UPDATE mes_pro_edhr_work_task
SET action_url=CONCAT('/mes/pro/feedback/edhr-execution/detail?id=', @execution_id, '&workTaskId=', @fill_task_id)
WHERE id=@fill_task_id;
SELECT JSON_OBJECT('executionId', @execution_id, 'fillTaskId', @fill_task_id, 'runKey', @run_key);
`
  const result = parseJsonRow(mysql(sql), 'draft execution setup')
  assert.ok(result.executionId, 'execution id must be created')
  assert.ok(result.fillTaskId, 'fill task id must be created')
  return result
}

function loadReviewTasks(executionId, signatureCellKey) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_ARRAYAGG(JSON_OBJECT(
  'id', id,
  'status', status,
  'assigneeUserId', assignee_user_id,
  'candidateSourceType', candidate_source_type,
  'candidateSourceId', candidate_source_id,
  'candidateUserSnapshot', candidate_user_snapshot,
  'signatureCellKey', signature_cell_key,
  'reviewSourceType', review_source_type,
  'reviewSourceId', review_source_id
))
FROM mes_pro_edhr_work_task
WHERE tenant_id=122
  AND execution_id=${Number(executionId)}
  AND task_type='REVIEW'
  AND signature_cell_key=${sqlString(signatureCellKey)};
`)
  return parseJsonRow(output, 'review task state') || []
}

function loadTaskState(taskIds) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_ARRAYAGG(JSON_OBJECT('id', id, 'status', status, 'reason', reason, 'assigneeUserId', assignee_user_id))
FROM mes_pro_edhr_work_task
WHERE tenant_id=122
  AND id IN (${taskIds.map(Number).join(',')});
`)
  return parseJsonRow(output, 'task state')
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`Missing visible input: ${label}`)
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
  throw new Error(`Missing enabled target: ${label}; states=${JSON.stringify(states)}`)
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
    throw new Error('Login captcha is enabled; unattended real E2E cannot continue.')
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
  assert.ok(!page.url().includes('/login'), `login must leave login page, current=${page.url()}`)
}

async function confirmMessageBox(page) {
  const messageBox = page.locator('.el-message-box:visible').first()
  await messageBox.waitFor({ state: 'visible', timeout: 30000 })
  await clickFirstEnabled(messageBox.getByRole('button', { name: /确认|确定/ }), 'message box confirm')
}

async function submitExecutionFromPage(page, setup) {
  page.on('response', async (response) => {
    if (response.url().includes('/admin-api/mes/pro/batch-record-execution/get')) {
      const text = await response.text().catch(() => '')
      console.log(`DEBUG edhr-execution/get ${response.status()} ${text.slice(0, 300)}`)
    }
  })
  await page.goto(`${BASE_URL}${EXECUTION_ROUTE}?id=${setup.executionId}&workTaskId=${setup.fillTaskId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  try {
    await page.getByText('eDHR 执行详情').first().waitFor({ state: 'visible', timeout: 60000 })
    await page.getByText(RUN_KEY).first().waitFor({ state: 'visible', timeout: 60000 })
  } catch (error) {
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(`execution detail page did not open. url=${page.url()} body=${bodyText.slice(0, 500)}`)
  }
  const bodyBeforeSubmit = await page.locator('body').innerText().catch(() => '')
  if (!bodyBeforeSubmit.includes('提交执行')) {
    throw new Error(`submit execution button is not rendered. url=${page.url()} body=${bodyBeforeSubmit.slice(0, 1000)}`)
  }
  await clickFirstEnabled(page.getByRole('button', { name: '提交执行' }), 'submit execution button')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '提交 eDHR 执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), TEST_PASSWORD, 'submit password')
  await fillFirstVisible(dialog.locator('textarea'), `T5候选池上游真实E2E ${RUN_KEY}`, 'submit comment')
  const submitResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/submit') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 定|确 认|确认/ }), 'confirm submit')
  const response = await submitResponse
  assert.equal(response.status(), 200, 'submit HTTP status must be 200')
  const body = await response.json()
  assert.equal(body.code, 0, `submit business response must succeed: ${JSON.stringify(body)}`)
}

async function openCandidateTabAndFilterRun(page) {
  await page.goto(`${BASE_URL}${WORK_TASK_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('候选审核').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('候选审核').first().click()
  await fillFirstVisible(page.locator('label:has-text("批次")').locator('..').locator('input'), RUN_KEY, 'candidate batch filter')
  await clickFirstEnabled(page.getByRole('button', { name: '查询' }), 'candidate query button')
  await page.getByText(RUN_KEY).first().waitFor({ state: 'visible', timeout: 60000 })
}

async function run() {
  assertLocalOnly()
  const setup = prepareDraftExecution()
  const signatureCellKey = `${RUN_KEY}-R1C1`
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()
  try {
    await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD, `${EXECUTION_ROUTE}?id=${setup.executionId}&workTaskId=${setup.fillTaskId}`)
    await submitExecutionFromPage(page, setup)
    const reviewTasks = loadReviewTasks(setup.executionId, signatureCellKey)
    assert.ok(reviewTasks.length >= 2, `submit must create peer review tasks for the signature cell: ${JSON.stringify(reviewTasks)}`)
    assert.ok(reviewTasks.every((task) => task.candidateSourceType === 'ROLE_GROUP'), 'all review tasks must keep ROLE_GROUP source')
    assert.ok(reviewTasks.every((task) => Number(task.candidateSourceId) === ROLE_ID), 'all review tasks must keep the role source id')
    assert.ok(reviewTasks.every((task) => task.signatureCellKey === signatureCellKey), 'all review tasks must keep the same signature cell')
    assert.ok(reviewTasks.every((task) => /(^|,)113(,|$)/.test(task.candidateUserSnapshot)), 'candidate snapshot must include aoteman')
    assert.ok(reviewTasks.every((task) => /(^|,)910245(,|$)/.test(task.candidateUserSnapshot)), 'candidate snapshot must include role peer')
    const reviewTaskIds = reviewTasks.map((task) => Number(task.id))

    await openCandidateTabAndFilterRun(page)
    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: RUN_KEY }).first()
    await row.getByText(`角色组 ${ROLE_ID}`).waitFor({ state: 'visible', timeout: 60000 })
    const completeResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/edhr-work-task/candidate-signature/complete') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await clickFirstEnabled(row.getByRole('button', { name: '完成签名' }), 'complete upstream candidate signature')
    await confirmMessageBox(page)
    const complete = await completeResponse
    assert.equal(complete.status(), 200, 'candidate complete HTTP status must be 200')
    const completeBody = await complete.json()
    assert.equal(completeBody.code, 0, `candidate complete business response must succeed: ${JSON.stringify(completeBody)}`)

    const finalStates = loadTaskState(reviewTaskIds)
    const doneTasks = finalStates.filter((task) => task.status === 'DONE')
    const canceledTasks = finalStates.filter((task) => task.status === 'CANCELED')
    assert.equal(doneTasks.length, 1, `one candidate task must be DONE: ${JSON.stringify(finalStates)}`)
    assert.equal(canceledTasks.length, reviewTasks.length - 1, `peer candidate tasks must be CANCELED: ${JSON.stringify(finalStates)}`)
    assert.ok(canceledTasks.every((task) => task.reason === '同一签名位已有候选人完成'), 'peer cancel reason must be explicit')

    await page.context().clearCookies()
    await page.evaluate(() => {
      localStorage.clear()
      sessionStorage.clear()
    })
    await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD, WORK_TASK_ROUTE)
    await page.goto(`${BASE_URL}${WORK_TASK_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('候选审核').first().waitFor({ state: 'visible', timeout: 60000 })
    console.log(`PASS: eDHR candidate upstream real E2E runKey=${RUN_KEY} execution=${setup.executionId} reviewTasks=${reviewTaskIds.join(',')}`)
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exit(1)
})
