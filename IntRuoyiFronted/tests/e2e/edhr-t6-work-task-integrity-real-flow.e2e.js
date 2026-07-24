const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_T6_E2E_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_T6_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.EDHR_T6_E2E_LOGIN_PASSWORD || '111111'
const SIGNATURE_PASSWORD = process.env.EDHR_T6_E2E_SIGNATURE_PASSWORD
const CHROME_EXECUTABLE =
  process.env.EDHR_T6_E2E_CHROME_EXECUTABLE ||
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.EDHR_T6_E2E_ADMIN_PASSWORD || 'admin123'
const WORK_TASK_ROUTE = '/mes/pro/feedback/edhr-work-task'
const EXECUTION_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const RUN_KEY = `T6-REAL-E2E-${Date.now()}`

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

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
  if (!line || line === 'NULL') return null
  try {
    return JSON.parse(line)
  } catch (error) {
    throw new Error(`${label} returned non JSON output: ${line}`)
  }
}

function loadTestUser() {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'userId', id,
  'username', username,
  'tenantId', tenant_id,
  'status', status
)
FROM system_users
WHERE tenant_id=122
  AND deleted=0
  AND username='${TEST_USERNAME}'
LIMIT 1;
`)
  return parseJsonRow(output, 'test tenant user lookup')
}

function findWritableFillTask(assigneeUserId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'workTaskId', t.id,
  'batchExecutionId', t.batch_execution_id,
  'batchTaskId', t.batch_task_id,
  'executionId', t.execution_id,
  'executionCode', e.execution_code,
  'actionUrl', t.action_url,
  'batchCode', t.batch_code,
  'taskCode', t.task_code,
  'fieldCount', COALESCE(JSON_LENGTH(JSON_EXTRACT(e.execution_snapshot_json, '$.fields')), 0)
)
FROM mes_pro_edhr_work_task t
LEFT JOIN mes_pro_batch_record_execution e ON e.id=t.execution_id AND e.tenant_id=t.tenant_id AND e.deleted=0
WHERE t.tenant_id=122
  AND t.deleted=0
  AND t.task_type='FILL'
  AND t.status IN ('TODO', 'OVERDUE')
  AND t.assignee_user_id=${Number(assigneeUserId)}
  AND (
    (
      t.execution_id IS NULL
      AND t.batch_execution_id IS NOT NULL
      AND t.batch_task_id IS NOT NULL
      AND t.action_url LIKE '/mes/pro/feedback/edhr-batch-execution/detail?id=%batchTaskId=%workTaskId=%'
    )
    OR (
      t.execution_id IS NOT NULL
      AND t.action_url LIKE '/mes/pro/feedback/edhr-execution/%id=%workTaskId=%'
      AND e.status=0
      AND JSON_LENGTH(JSON_EXTRACT(e.execution_snapshot_json, '$.fields')) > 0
    )
  )
ORDER BY t.id DESC
LIMIT 1;
`)
  return parseJsonRow(output, 'writable FILL task lookup')
}

function reloadWorkTask(workTaskId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'workTaskId', t.id,
  'batchExecutionId', t.batch_execution_id,
  'batchTaskId', t.batch_task_id,
  'executionId', t.execution_id,
  'executionCode', e.execution_code,
  'actionUrl', t.action_url,
  'batchCode', t.batch_code,
  'taskCode', t.task_code,
  'fieldCount', COALESCE(JSON_LENGTH(JSON_EXTRACT(e.execution_snapshot_json, '$.fields')), 0)
)
FROM mes_pro_edhr_work_task t
LEFT JOIN mes_pro_batch_record_execution e ON e.id=t.execution_id AND e.tenant_id=t.tenant_id AND e.deleted=0
WHERE t.tenant_id=122
  AND t.deleted=0
  AND t.id=${Number(workTaskId)}
LIMIT 1;
`)
  return parseJsonRow(output, 'work task reload')
}

function loadEvidenceCounts(executionId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'executionStatus', (SELECT status FROM mes_pro_batch_record_execution WHERE tenant_id=122 AND id=${Number(executionId)} AND deleted=0),
  'submittedAtCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution WHERE tenant_id=122 AND id=${Number(executionId)} AND deleted=0 AND submitted_at IS NOT NULL),
  'approvedByCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution WHERE tenant_id=122 AND id=${Number(executionId)} AND deleted=0 AND approved_by IS NOT NULL),
  'approvedAtCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution WHERE tenant_id=122 AND id=${Number(executionId)} AND deleted=0 AND approved_at IS NOT NULL),
  'closedAtCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution WHERE tenant_id=122 AND id=${Number(executionId)} AND deleted=0 AND closed_at IS NOT NULL),
  'fieldAuditBatchCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution_field_audit_batch WHERE tenant_id=122 AND execution_id=${Number(executionId)}),
  'fieldAuditItemCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution_field_audit_item WHERE tenant_id=122 AND execution_id=${Number(executionId)}),
  'formReviewSignatureCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution_signature WHERE tenant_id=122 AND execution_id=${Number(executionId)} AND action_type='FORM_REVIEW'),
  'submitSignatureCount', (SELECT COUNT(*) FROM mes_pro_batch_record_execution_signature WHERE tenant_id=122 AND execution_id=${Number(executionId)} AND action_type='SUBMIT'),
  'reviewTaskCount', (SELECT COUNT(*) FROM mes_pro_edhr_work_task WHERE tenant_id=122 AND execution_id=${Number(executionId)} AND deleted=0 AND task_type='REVIEW'),
  'approveTaskCount', (SELECT COUNT(*) FROM mes_pro_edhr_work_task WHERE tenant_id=122 AND execution_id=${Number(executionId)} AND deleted=0 AND task_type='APPROVE')
);
`)
  return parseJsonRow(output, 'evidence counts')
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
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click()
      return
    }
  }
  throw blocked(`缺少可点击控件：${label}`)
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

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (const store of [localStorage, sessionStorage]) {
      for (let index = 0; index < store.length; index += 1) {
        const key = store.key(index)
        result[key] = store.getItem(key)
      }
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
        current = current.accessToken ?? current.v ?? current.value ?? current.token ?? current
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

async function authedRequest(page, method, path, data) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, 'network verification requires browser access token')
  assert.ok(tenantId, 'network verification requires browser tenant-id')
  const options = {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    data
  }
  const response = await page.request[method](`${BACKEND_URL}/admin-api${path}`, options)
  const body = await response.json().catch(async () => ({ raw: await response.text() }))
  return { response, body }
}

async function openActionUrl(page, setup) {
  await page.goto(`${BASE_URL}${setup.actionUrl}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  assert.ok(page.url().includes(`workTaskId=${setup.workTaskId}`), 'actionUrl must carry the writable workTaskId')
  if (setup.executionId) {
    await page.getByText(/eDHR 执行详情|提交执行|执行编号/).first().waitFor({ state: 'visible', timeout: 60000 })
    return setup
  }
  await page.getByRole('button', { name: /打开填写|打开返工/ }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  const [taskOpenResult] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    clickFirstEnabled(page.getByRole('button', { name: /打开填写|打开返工/ }), 'open batch task')
  ])
  const taskOpenBody = await taskOpenResult.json()
  assert.equal(taskOpenResult.status(), 200, 'batch task open HTTP status must be 200')
  assert.equal(taskOpenBody.code, 0, `batch task open business response must succeed: ${JSON.stringify(taskOpenBody)}`)
  await page.waitForURL(
    (url) =>
      url.pathname === '/mes/pro/feedback/edhr-execution/form' ||
      url.pathname === '/mes/pro/feedback/edhr-execution/detail',
    { timeout: 60000 }
  )
  assert.ok(page.url().includes(`workTaskId=${setup.workTaskId}`), 'opened execution URL must carry the writable workTaskId')
  const opened = reloadWorkTask(setup.workTaskId)
  assert.ok(opened?.executionId, 'opening batch task must bind executionId to the writable work task')
  await page.getByText(/提交执行|执行编号|字段审计/).first().waitFor({ state: 'visible', timeout: 60000 })
  return opened
}

async function changeFirstEditableField(page) {
  const fieldInputs = page.locator('.edhr-page-shell__form input:not([type="password"]), .edhr-page-shell__form textarea')
  const count = await fieldInputs.count()
  for (let index = 0; index < count; index += 1) {
    const input = fieldInputs.nth(index)
    if (!(await input.isVisible()) || (await input.isDisabled())) continue
    const before = await input.inputValue().catch(() => '')
    const value = `${RUN_KEY}-${index}`
    await input.fill(value)
    const after = await input.inputValue().catch(() => '')
    if (after !== before) {
      await page.getByText('待保存变更').first().waitFor({ state: 'visible', timeout: 30000 })
      return { before, after, index }
    }
  }
  throw blocked('测试租户当前 FILL 工作任务没有可编辑字段，无法验证字段审计保存写权限。')
}

async function saveFieldAuditFromPage(page) {
  await clickFirstEnabled(page.locator('.edhr-page-shell__field-audit-reason .el-select').first(), 'field audit reason category')
  await page.getByText('操作录入').last().click()
  await fillFirstVisible(
    page.locator('.edhr-page-shell__field-audit-reason input[placeholder="请输入字段变更原因"]'),
    `T6 workTaskId real E2E ${RUN_KEY}`,
    'field audit reason text'
  )
  await clickFirstEnabled(page.getByRole('button', { name: '保存变更' }), 'save field changes')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), SIGNATURE_PASSWORD, 'field audit signature password')
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/field-audit/save-changes') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 定|确 认|确认/ }), 'confirm field audit signature')
  const response = await saveResponsePromise
  const body = await response.json()
  assert.equal(response.status(), 200, 'field audit HTTP status must be 200')
  assert.equal(body.code, 0, `field audit business response must succeed: ${JSON.stringify(body)}`)
  assert.equal(body.data?.hashVerification?.status, 'VALID', 'field audit hash verification must be VALID')
  await page.getByText(/字段审计批次|字段变更已写入不可篡改审计链/).first().waitFor({ state: 'visible', timeout: 60000 })
  return body.data
}

async function submitGateFromPage(page) {
  await clickFirstEnabled(page.getByRole('button', { name: '提交执行' }), 'submit execution')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '提交 eDHR 执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), SIGNATURE_PASSWORD, 'submit password')
  const textInput = dialog.locator('textarea').first()
  if ((await textInput.count()) > 0 && (await textInput.isVisible())) {
    await textInput.fill(`T6 submit gate ${RUN_KEY}`)
  }
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/submit') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 定|确 认|确认/ }), 'confirm submit')
  const response = await responsePromise
  const body = await response.json()
  assert.equal(response.status(), 200, 'submit HTTP status must be 200')
  assert.equal(body.code, 0, `ordinary process submit must succeed: ${JSON.stringify(body)}`)
  return body
}

async function verifyInvalidWorkTaskNetworkFailures(page, setup, beforeCounts) {
  const wrongWorkTaskId = Number(setup.workTaskId) + 999999999
  const missingSubmit = await authedRequest(page, 'put', '/mes/pro/batch-record-execution/submit', {
    id: setup.executionId,
    password: SIGNATURE_PASSWORD,
    comment: `missing workTaskId ${RUN_KEY}`
  })
  assert.notEqual(missingSubmit.body.code, 0, `missing workTaskId submit must fail: ${JSON.stringify(missingSubmit.body)}`)

  const wrongSubmit = await authedRequest(page, 'put', '/mes/pro/batch-record-execution/submit', {
    id: setup.executionId,
    workTaskId: wrongWorkTaskId,
    password: SIGNATURE_PASSWORD,
    comment: `wrong workTaskId ${RUN_KEY}`
  })
  assert.notEqual(wrongSubmit.body.code, 0, `wrong workTaskId submit must fail: ${JSON.stringify(wrongSubmit.body)}`)

  const wrongDetail = await authedRequest(page, 'get', `/mes/pro/batch-record-execution/get?id=${setup.executionId}&workTaskId=${wrongWorkTaskId}`)
  assert.notEqual(wrongDetail.body.code, 0, `wrong workTaskId detail must fail: ${JSON.stringify(wrongDetail.body)}`)

  const afterCounts = loadEvidenceCounts(setup.executionId)
  assert.equal(afterCounts.fieldAuditBatchCount, beforeCounts.fieldAuditBatchCount, 'invalid workTaskId checks must not create field audit batches')
  assert.equal(afterCounts.fieldAuditItemCount, beforeCounts.fieldAuditItemCount, 'invalid workTaskId checks must not create field audit items')
  assert.equal(afterCounts.formReviewSignatureCount, beforeCounts.formReviewSignatureCount, 'invalid workTaskId checks must not create FORM_REVIEW signatures')
  assert.equal(afterCounts.submitSignatureCount, beforeCounts.submitSignatureCount, 'invalid workTaskId checks must not create SUBMIT signatures')
  return { missingSubmit: missingSubmit.body, wrongSubmit: wrongSubmit.body, wrongDetail: wrongDetail.body }
}

async function verifyAdminReadonly(browser) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const writeRequests = []
  page.on('request', (request) => {
    const method = request.method()
    const url = request.url()
    if (url.includes('/admin-api/mes/pro/') && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      writeRequests.push(`${method} ${url}`)
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
    assert.deepEqual(writeRequests, [], `admin readonly verification must not send MES write requests: ${writeRequests.join('; ')}`)
  } finally {
    await context.close()
  }
}

async function run() {
  assertLocalOnly()
  if (!SIGNATURE_PASSWORD) {
    throw blocked('缺少 EDHR_T6_E2E_SIGNATURE_PASSWORD，不能执行字段审计或提交签名真实 E2E。', [
      '设置本机测试租户 aoteman 的真实电子签名密码后重跑。',
      '不得使用 mock 密码、接口造数或切换到 admin 租户写入。'
    ])
  }
  const testUser = loadTestUser()
  if (!testUser?.userId) {
    throw blocked(`测试租户不存在可登录用户 ${TEST_USERNAME}，无法确认真实工作任务责任人。`)
  }
  const setup = findWritableFillTask(testUser.userId)
  if (!setup) {
    throw blocked(`测试租户/${TEST_USERNAME} 当前不存在可写 FILL 工作任务，无法从真实 actionUrl 进入并验证 T6 写权限。`, [
      `当前真实登录用户 ID：${testUser.userId}。`,
      '现有可写 FILL 任务若仍挂在旧用户 ID 上，必须先通过本地测试租户配置/任务分配修复，不能在 E2E 中硬编码旧责任人或绕过责任人校验。'
    ])
  }

  const browser = await chromium.launch({
    headless: process.env.EDHR_T6_E2E_HEADED !== '1',
    ...(CHROME_EXECUTABLE ? { executablePath: CHROME_EXECUTABLE } : {})
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD, setup.actionUrl)
    const openedSetup = await openActionUrl(page, setup)
    const beforeInvalidCounts = loadEvidenceCounts(openedSetup.executionId)
    const invalidResults = await verifyInvalidWorkTaskNetworkFailures(page, openedSetup, beforeInvalidCounts)

    await changeFirstEditableField(page)
    const fieldAuditResult = await saveFieldAuditFromPage(page)
    const afterFieldAuditCounts = loadEvidenceCounts(openedSetup.executionId)
    assert.equal(afterFieldAuditCounts.fieldAuditBatchCount, beforeInvalidCounts.fieldAuditBatchCount + 1, 'correct workTaskId must create one field audit batch')
    assert.ok(afterFieldAuditCounts.fieldAuditItemCount > beforeInvalidCounts.fieldAuditItemCount, 'correct workTaskId must create field audit item evidence')

    const submitResult = await submitGateFromPage(page)
    const afterSubmitCounts = loadEvidenceCounts(openedSetup.executionId)
    assert.equal(afterSubmitCounts.executionStatus, 4, 'ordinary process submit must close execution as FILL_COMPLETED=4')
    assert.equal(afterSubmitCounts.submittedAtCount, 1, 'ordinary process submit must keep submittedAt evidence')
    assert.equal(afterSubmitCounts.closedAtCount, 1, 'ordinary process submit must keep closedAt evidence')
    assert.equal(afterSubmitCounts.approvedByCount, 0, 'ordinary process submit must not write approvedBy')
    assert.equal(afterSubmitCounts.approvedAtCount, 0, 'ordinary process submit must not write approvedAt')
    assert.equal(afterSubmitCounts.submitSignatureCount, beforeInvalidCounts.submitSignatureCount + 1, 'ordinary process submit must create one SUBMIT signature')
    assert.equal(afterSubmitCounts.formReviewSignatureCount, beforeInvalidCounts.formReviewSignatureCount, 'ordinary process submit must not create FORM_REVIEW signature')
    assert.equal(afterSubmitCounts.reviewTaskCount, beforeInvalidCounts.reviewTaskCount, 'ordinary process submit must not create REVIEW work task')
    assert.equal(afterSubmitCounts.approveTaskCount, beforeInvalidCounts.approveTaskCount, 'ordinary process submit must not create APPROVE work task')
    await verifyAdminReadonly(browser)

    console.log(`PASS: T6 workTaskId real E2E runKey=${RUN_KEY} execution=${openedSetup.executionId} workTask=${openedSetup.workTaskId}`)
    console.log(`PASS: invalid missing/wrong workTaskId failed without audit/signature: ${JSON.stringify(invalidResults)}`)
    console.log(`PASS: fieldAuditBatch=${fieldAuditResult.auditBatchId} submitCode=${submitResult.code} ordinary flow reached FILL_COMPLETED=4 without approvedBy/approvedAt or FORM_REVIEW/REVIEW/APPROVE`)
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
