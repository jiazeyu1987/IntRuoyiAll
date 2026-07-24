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
const WORK_TASK_ROUTE = '/mes/pro/feedback/edhr-work-task'
const RUN_KEY = `E2E-CANDIDATE-${Date.now()}`

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'E2E must use local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'E2E must use local backend 48081')
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

function prepareCandidateTasks() {
  const sql = `
SET NAMES utf8mb4;
SET @tenant_id := 122;
SET @batch_execution_id := (SELECT id FROM mes_pro_edhr_batch_execution WHERE tenant_id=@tenant_id AND deleted=0 ORDER BY id DESC LIMIT 1);
SET @batch_task_id := (SELECT id FROM mes_pro_edhr_batch_execution_task WHERE tenant_id=@tenant_id AND deleted=0 AND batch_execution_id=@batch_execution_id AND route_process_id IS NOT NULL ORDER BY id LIMIT 1);
SET @execution_id := 990000000 + FLOOR(RAND() * 1000000);
SET @candidate_user_id := 113;
SET @peer_user_id := 910245;
SET @run_key := '${RUN_KEY}';
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id,
  process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, signature_cell_key, signature_row_index, signature_column_index, review_source_type,
  review_source_id, review_source_name, bpm_task_id, status, due_time, action_url, remark, creator,
  updater, deleted, tenant_id
)
SELECT CONCAT(@run_key, '-A'), 'REVIEW', b.id, t.id, 'BATCH_TASK', t.id,
  @execution_id, b.work_order_id, b.work_order_code, b.batch_code, b.route_id, t.route_process_id, t.process_id,
  CONCAT('候选审核-', @run_key), @candidate_user_id, 'ROLE_GROUP', 9001, CONCAT(@candidate_user_id, ',', @peer_user_id),
  @candidate_user_id, CONCAT(@run_key, '-R1C1'), 0, 0, 'ROLE', 9001, '候选审核角色组',
  CONCAT(@run_key, '-BPM-A'), 'TODO', DATE_ADD(NOW(), INTERVAL 1 DAY), '/mes/pro/feedback/edhr-execution/detail?id=990000000',
  'T5候选池真实E2E', 'codex', 'codex', 0, @tenant_id
FROM mes_pro_edhr_batch_execution b
JOIN mes_pro_edhr_batch_execution_task t ON t.id=@batch_task_id
WHERE b.id=@batch_execution_id;
INSERT INTO mes_pro_edhr_work_task (
  task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
  execution_id, work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id,
  process_name, assignee_user_id, candidate_source_type, candidate_source_id, candidate_user_snapshot,
  source_user_id, signature_cell_key, signature_row_index, signature_column_index, review_source_type,
  review_source_id, review_source_name, bpm_task_id, status, due_time, action_url, remark, creator,
  updater, deleted, tenant_id
)
SELECT CONCAT(@run_key, '-B'), 'REVIEW', b.id, t.id, 'BATCH_TASK', t.id,
  @execution_id, b.work_order_id, b.work_order_code, b.batch_code, b.route_id, t.route_process_id, t.process_id,
  CONCAT('候选审核-', @run_key), @peer_user_id, 'ROLE_GROUP', 9001, CONCAT(@candidate_user_id, ',', @peer_user_id),
  @candidate_user_id, CONCAT(@run_key, '-R1C1'), 0, 0, 'ROLE', 9001, '候选审核角色组',
  CONCAT(@run_key, '-BPM-B'), 'TODO', DATE_ADD(NOW(), INTERVAL 1 DAY), '/mes/pro/feedback/edhr-execution/detail?id=990000000',
  'T5候选池真实E2E-peer', 'codex', 'codex', 0, @tenant_id
FROM mes_pro_edhr_batch_execution b
JOIN mes_pro_edhr_batch_execution_task t ON t.id=@batch_task_id
WHERE b.id=@batch_execution_id;
SELECT JSON_OBJECT(
  'candidateTaskId', MIN(CASE WHEN task_code=CONCAT(@run_key, '-A') THEN id END),
  'peerTaskId', MIN(CASE WHEN task_code=CONCAT(@run_key, '-B') THEN id END),
  'executionId', @execution_id,
  'runKey', @run_key
) FROM mes_pro_edhr_work_task WHERE tenant_id=@tenant_id AND task_code IN (CONCAT(@run_key, '-A'), CONCAT(@run_key, '-B'));
`
  const result = parseJsonRow(mysql(sql), 'candidate task setup')
  assert.ok(result.candidateTaskId, 'candidate task id must be created')
  assert.ok(result.peerTaskId, 'peer task id must be created')
  assert.ok(result.executionId, 'execution id must be assigned')
  return result
}

function loadTaskState(taskIds) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_ARRAYAGG(JSON_OBJECT('id', id, 'status', status, 'reason', reason, 'taskCode', task_code))
FROM mes_pro_edhr_work_task
WHERE id IN (${taskIds.map(Number).join(',')});
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
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click()
      return
    }
  }
  throw new Error(`Missing enabled target: ${label}`)
}

async function login(page, tenant, username, password) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(WORK_TASK_ROUTE)}`, {
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
}

async function confirmMessageBox(page) {
  const messageBox = page.locator('.el-message-box:visible').first()
  await messageBox.waitFor({ state: 'visible', timeout: 30000 })
  await clickFirstEnabled(messageBox.getByRole('button', { name: /确认|确定/ }), 'message box confirm')
}

async function run() {
  assertLocalOnly()
  const setup = prepareCandidateTasks()
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()
  try {
    await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD)
    await page.goto(`${BASE_URL}${WORK_TASK_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('候选审核').first().waitFor({ state: 'visible', timeout: 60000 })
    await page.getByText('候选审核').first().click()
    await page.getByText(RUN_KEY).first().waitFor({ state: 'visible', timeout: 60000 })
    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: `${RUN_KEY}-A` }).first()
    await row.waitFor({ state: 'visible', timeout: 60000 })
    await row.getByText('角色组 9001').waitFor({ state: 'visible', timeout: 60000 })
    await row.getByText('113,910245').waitFor({ state: 'visible', timeout: 60000 })

    const completeResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/edhr-work-task/candidate-signature/complete') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await clickFirstEnabled(row.getByRole('button', { name: '完成签名' }), 'complete candidate signature')
    await confirmMessageBox(page)
    const response = await completeResponse
    assert.equal(response.status(), 200, 'candidate complete HTTP status must be 200')
    const body = await response.json()
    assert.equal(body.code, 0, `candidate complete business response must succeed: ${JSON.stringify(body)}`)

    const states = loadTaskState([setup.candidateTaskId, setup.peerTaskId])
    const completed = states.find((item) => Number(item.id) === Number(setup.candidateTaskId))
    const peer = states.find((item) => Number(item.id) === Number(setup.peerTaskId))
    assert.equal(completed.status, 'DONE', 'candidate task must become DONE')
    assert.equal(peer.status, 'CANCELED', 'peer candidate task in the same signature cell must be canceled')
    assert.equal(peer.reason, '同一签名位已有候选人完成')

    await page.context().clearCookies()
    await page.evaluate(() => {
      localStorage.clear()
      sessionStorage.clear()
    })
    await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD)
    await page.goto(`${BASE_URL}${WORK_TASK_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('候选审核').first().waitFor({ state: 'visible', timeout: 60000 })
    console.log(`PASS: eDHR candidate work task real E2E runKey=${RUN_KEY} task=${setup.candidateTaskId} peer=${setup.peerTaskId}`)
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exit(1)
})
