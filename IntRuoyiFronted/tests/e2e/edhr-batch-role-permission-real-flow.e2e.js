const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_ROLE_E2E_BASE_URL || 'http://127.0.0.1:8095'
const BACKEND_URL = process.env.EDHR_ROLE_E2E_BACKEND_URL || 'http://127.0.0.1:48095'
const BROWSER_EXECUTABLE = process.env.EDHR_ROLE_E2E_BROWSER ||
  'C:/Users/BJB110/AppData/Local/ms-playwright/chromium-1223/chrome-win64/chrome.exe'
const TASK_DIR = path.resolve(process.cwd(), '..', 'ruoyi-vue-pro', 'doc', 'tasks', '20260706-edhr-batch-role-permission-flow')
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-batch-role-permission-real-flow')
const EVIDENCE_FILE = path.join(TASK_DIR, 'role-real-e2e-evidence.json')

const TEST_TENANT = '测试租户'
const FILLER = { username: 'aoteman', password: '111111', userId: 914520 }
const REVIEWER = { username: 'edhrmatrixapprover', password: '111111', userId: 914521 }
const PRODUCTION_OWNER = { username: 'edhrmatrixapprover', password: '111111', userId: 914521 }
const SAME_TENANT_UNRELATED = { username: 'zhaojie', password: '111111', userId: 913324 }
const UNRELATED = { username: 'admin', password: 'admin123', tenant: '芋道源码' }
const ROUTE_CODE = '900025'
const ROUTE_ID = 922046
const ROUTE_PROCESS_ID = 922339
const BATCH_ID = Number(process.env.EDHR_ROLE_E2E_BATCH_ID || 900000000462)
const BATCH_TASK_ID = Number(process.env.EDHR_ROLE_E2E_BATCH_TASK_ID || 2732)
const FILL_WORK_TASK_ID = Number(process.env.EDHR_ROLE_E2E_FILL_WORK_TASK_ID || 1098)
const INCOMING_NODE_TYPE = 'INCOMING_INSPECTION_REPORT'

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
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

function parseJson(output, label) {
  const line = output.split(/\r?\n/).find(Boolean)
  if (!line || line === 'NULL') {
    throw blocked(`${label} 未返回 JSON。`, [output])
  }
  return JSON.parse(line)
}

function loadTargetState() {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'batchId', b.id,
  'batchStatus', b.status,
  'batchTaskId', bt.id,
  'batchTaskStatus', bt.status,
  'executionId', bt.execution_id,
  'processName', bt.process_name,
  'fillWorkTaskId', wt.id,
  'fillWorkTaskStatus', wt.status,
  'fillAssigneeUserId', wt.assignee_user_id,
  'fillActionUrl', wt.action_url
)
FROM mes_pro_edhr_work_task wt
JOIN mes_pro_edhr_batch_execution b ON b.id=wt.batch_execution_id AND b.deleted=0
JOIN mes_pro_edhr_batch_execution_task bt ON bt.id=wt.batch_task_id AND bt.deleted=0
WHERE wt.tenant_id=122
  AND wt.deleted=0
  AND wt.id=${Number(FILL_WORK_TASK_ID)}
  AND wt.batch_execution_id=${Number(BATCH_ID)}
  AND wt.batch_task_id=${Number(BATCH_TASK_ID)}
LIMIT 1;
`)
  return parseJson(output, '目标批次任务')
}

function loadWorkTasks(executionId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT(
  'id', id,
  'taskType', task_type,
  'status', status,
  'batchExecutionId', batch_execution_id,
  'batchTaskId', batch_task_id,
  'executionId', execution_id,
  'assigneeUserId', assignee_user_id,
  'candidateSourceType', candidate_source_type,
  'candidateSourceId', candidate_source_id,
  'candidateUserSnapshot', candidate_user_snapshot,
  'signatureCellKey', signature_cell_key,
  'sourceUserId', source_user_id
)), JSON_ARRAY())
FROM mes_pro_edhr_work_task
WHERE tenant_id=122
  AND deleted=0
  AND execution_id=${Number(executionId)}
ORDER BY id;
`)
  return parseJson(output, '工作任务列表')
}

function loadExecutionStatus(executionId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'id', id,
  'status', status,
  'submittedBy', submitted_by,
  'submittedAt', submitted_at,
  'approvedBy', approved_by,
  'approvedAt', approved_at,
  'closedAt', closed_at
)
FROM mes_pro_batch_record_execution
WHERE tenant_id=122 AND deleted=0 AND id=${Number(executionId)}
LIMIT 1;
`)
  return parseJson(output, '执行状态')
}

function loadBatchTaskState(batchTaskId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'id', id,
  'batchExecutionId', batch_execution_id,
  'nodeType', node_type,
  'processName', process_name,
  'status', status,
  'executionId', execution_id,
  'batchRecordReportId', batch_record_report_id
)
FROM mes_pro_edhr_batch_execution_task
WHERE tenant_id=122 AND deleted=0 AND id=${Number(batchTaskId)}
LIMIT 1;
`)
  return parseJson(output, '批次工序状态')
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

async function login(page, tenant, username, password, redirect) {
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

async function snapshot(page, name) {
  ensureDir(RESULT_DIR)
  await page.screenshot({ path: path.join(RESULT_DIR, `${name}.png`), fullPage: true })
}

async function getJsonFromPage(page, pathWithQuery) {
  return page.evaluate(async (path) => {
    function parseCacheValue(key) {
      const raw = window.localStorage.getItem(key)
      if (!raw) return undefined
      function unwrap(value) {
        if (typeof value !== 'string') return value
        try {
          return JSON.parse(value)
        } catch (_) {
          return value
        }
      }
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object') {
          if (parsed.v !== undefined) return unwrap(parsed.v)
          if (parsed.value !== undefined) return unwrap(parsed.value)
          if (parsed.data !== undefined) return unwrap(parsed.data)
        }
      } catch (_) {
        return raw
      }
      return raw
    }
    const accessToken = parseCacheValue('ACCESS_TOKEN')
    const tenantId = parseCacheValue('tenantId')
    const visitTenantId = parseCacheValue('visitTenantId')
    if (!accessToken) throw new Error('ACCESS_TOKEN is missing after login')
    if (!tenantId) throw new Error('tenantId is missing after login')
    const headers = {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
      'tenant-id': String(tenantId)
    }
    if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
    const response = await fetch(`/admin-api${path}`, { credentials: 'include', headers })
    return { status: response.status, body: await response.json() }
  }, pathWithQuery)
}

async function requestJsonFromPage(page, method, pathWithQuery, body) {
  return page.evaluate(async ({ method, path, body }) => {
    function parseCacheValue(key) {
      const raw = window.localStorage.getItem(key)
      if (!raw) return undefined
      function unwrap(value) {
        if (typeof value !== 'string') return value
        try {
          return JSON.parse(value)
        } catch (_) {
          return value
        }
      }
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object') {
          if (parsed.v !== undefined) return unwrap(parsed.v)
          if (parsed.value !== undefined) return unwrap(parsed.value)
          if (parsed.data !== undefined) return unwrap(parsed.data)
        }
      } catch (_) {
        return raw
      }
      return raw
    }
    const accessToken = parseCacheValue('ACCESS_TOKEN')
    const tenantId = parseCacheValue('tenantId')
    const visitTenantId = parseCacheValue('visitTenantId')
    if (!accessToken) throw new Error('ACCESS_TOKEN is missing after login')
    if (!tenantId) throw new Error('tenantId is missing after login')
    const headers = {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      'tenant-id': String(tenantId)
    }
    if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
    const response = await fetch(`/admin-api${path}`, {
      method,
      credentials: 'include',
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    })
    let responseBody
    try {
      responseBody = await response.json()
    } catch (_) {
      responseBody = null
    }
    return { status: response.status, body: responseBody }
  }, { method, path: pathWithQuery, body })
}

function assertRejected(response, label) {
  assert.ok(response.status >= 200, `${label} HTTP response must exist: ${JSON.stringify(response)}`)
  assert.notEqual(response.body?.code, 0, `${label} must be rejected by backend: ${JSON.stringify(response.body)}`)
  return {
    status: response.status,
    bodyCode: response.body?.code,
    bodyMsg: response.body?.msg || response.body?.message || null
  }
}

async function assertRoleDetail(page, batchId, expectedRole, expectedActions, label) {
  return assertTaskDetail(page, batchId, BATCH_TASK_ID, expectedRole, expectedActions, label)
}

async function assertTaskDetail(page, batchId, taskId, expectedRole, expectedActions, label) {
  const response = await getJsonFromPage(page, `/mes/pro/edhr-batch-execution/get?id=${batchId}`)
  assert.equal(response.status, 200, `${label} detail HTTP must be 200`)
  assert.equal(response.body.code, 0, `${label} detail business response must succeed: ${JSON.stringify(response.body)}`)
  const task = (response.body.data.tasks || []).find((item) => Number(item.id) === Number(taskId))
  assert.ok(task, `${label} detail must contain target batch task ${taskId}`)
  assert.equal(task.currentUserRole, expectedRole, `${label} role must be ${expectedRole}`)
  for (const action of expectedActions) {
    assert.ok((task.allowedActions || []).includes(action), `${label} allowedActions must include ${action}: ${JSON.stringify(task)}`)
  }
  return task
}

async function loadBatchDetail(page, batchId) {
  const response = await getJsonFromPage(page, `/mes/pro/edhr-batch-execution/get?id=${batchId}`)
  assert.equal(response.status, 200, 'batch detail HTTP must be 200')
  assert.equal(response.body.code, 0, `batch detail business response must succeed: ${JSON.stringify(response.body)}`)
  return response.body.data
}

async function skipIncomingInspectionAsProductionOwner(page, batchId) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${batchId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('待处理工序').first().waitFor({ state: 'visible', timeout: 60000 })
  const incomingItem = page.locator('.edhr-batch-detail__pending-task-item').filter({ hasText: '来料检报告' }).first()
  await incomingItem.waitFor({ state: 'visible', timeout: 60000 })
  const incomingText = await incomingItem.innerText().catch(() => '')
  if (incomingText.includes('已跳过')) {
    return { alreadySkipped: true }
  }
  await clickFirstEnabled(incomingItem.getByRole('button', { name: /跳过节点|完成节点/ }), 'incoming special node action')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '跳过特殊节点' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('textarea'), '真实E2E：来料检报告前置节点无模板，生产负责人签名跳过以进入路线900025填写链路', 'incoming skip reason')
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), PRODUCTION_OWNER.password, 'incoming skip password')
  const skipResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/special-node/skip') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /签名并跳过|确 定|确认/ }), 'confirm incoming skip')
  const response = await skipResponse
  const body = await response.json()
  assert.equal(body.code, 0, `incoming special node skip must succeed: ${JSON.stringify(body)}`)
  await page.getByText('特殊节点已跳过').first().waitFor({ state: 'visible', timeout: 60000 }).catch(() => {})
  return body.data
}

async function openExecutionFromBatchDetail(page, batchId, targetState) {
  if (targetState?.executionId) {
    await page.goto(
      `${BASE_URL}/mes/pro/feedback/edhr-execution/form?id=${targetState.executionId}&batchExecutionId=${batchId}&batchTaskId=${BATCH_TASK_ID}&executionId=${targetState.executionId}&workTaskId=${FILL_WORK_TASK_ID}`,
      {
        waitUntil: 'domcontentloaded',
        timeout: 60000
      }
    )
    await page.getByText('eDHR 执行详情').first().waitFor({ state: 'visible', timeout: 60000 })
    return Number(targetState.executionId)
  }
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${batchId}&batchTaskId=${BATCH_TASK_ID}&workTaskId=${FILL_WORK_TASK_ID}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('待处理工序').first().waitFor({ state: 'visible', timeout: 60000 })
  const fillTaskItem = page
    .locator('.edhr-batch-detail__pending-task-item')
    .filter({ hasText: '吹球囊成型' })
    .filter({ hasText: '填写人' })
    .first()
  await fillTaskItem.waitFor({ state: 'visible', timeout: 60000 })
  const taskOpenPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(fillTaskItem.locator('.edhr-batch-detail__pending-task-action'), 'open fill task')
  const response = await taskOpenPromise
  const body = await response.json()
  assert.equal(body.code, 0, `open fill task must succeed: ${JSON.stringify(body)}`)
  assert.equal(Number(body.data.workTaskId), FILL_WORK_TASK_ID, 'open response must keep fill workTaskId')
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-execution/form'), { timeout: 60000 })
  return Number(body.data.executionId)
}

async function fillAndSaveDraft(page, executionId) {
  await page.getByText('eDHR 执行详情').first().waitFor({ state: 'visible', timeout: 60000 })
  const firstEditableField = page
    .locator(
      '.edhr-page-shell__form textarea:not([disabled]), ' +
        '.edhr-page-shell__form input:not([type="hidden"]):not([type="file"]):not([type="checkbox"]):not([disabled])'
    )
    .filter({ hasNotText: '执行备注' })
    .first()
  await firstEditableField.waitFor({ state: 'visible', timeout: 60000 })
  await firstEditableField.fill(`ROLE-E2E-${Date.now()}`)
  const reasonSelect = page.locator('.edhr-page-shell__field-audit-reason .el-select').first()
  await reasonSelect.waitFor({ state: 'visible', timeout: 30000 })
  await reasonSelect.click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)').first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await fillFirstVisible(page.locator('.edhr-page-shell__field-audit-reason input').last(), 'role permission real e2e draft save', 'field audit reason')
  await clickFirstEnabled(
    page.locator('.edhr-page-shell__field-audit').getByRole('button', { name: /^保存变更$/ }),
    'save field changes'
  )
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '确认保存 eDHR 字段变更' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), FILLER.password, 'field audit password')
  const saveResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-execution/field-audit/save-changes') &&
      ['POST', 'PUT'].includes(response.request().method()),
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 认 保 存|确认保存|确 定/ }), 'confirm save field changes')
  const response = await saveResponse
  const body = await response.json()
  assert.equal(body.code, 0, `save draft/field audit must succeed: ${JSON.stringify(body)}`)
  await page.getByText(/字段审计批次|暂无待保存变更/).first().waitFor({ state: 'visible', timeout: 60000 })
  const status = loadExecutionStatus(executionId)
  assert.equal(Number(status.status), 0, 'saving form changes must keep execution in DRAFT')
}

async function submitExecution(page) {
  await clickFirstEnabled(page.getByRole('button', { name: '提交执行' }), 'submit execution')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '提交 eDHR 执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const reviewSelect = dialog.locator('.edhr-page-shell__submit-select').first()
  if ((await reviewSelect.count()) > 0 && (await reviewSelect.isVisible())) {
    await reviewSelect.click()
    const reviewerOption = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
      .filter({ hasText: String(REVIEWER.userId) })
      .first()
    await reviewerOption.waitFor({ state: 'visible', timeout: 30000 })
    await reviewerOption.click()
  }
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), FILLER.password, 'submit password')
  const commentInput = dialog.locator('textarea').first()
  if ((await commentInput.count()) > 0 && (await commentInput.isVisible())) {
    await commentInput.fill('role permission real e2e submit')
  }
  const submitResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/submit') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 认 提 交|确认提交|确 定/ }), 'confirm submit')
  const response = await submitResponse
  const body = await response.json()
  assert.equal(body.code, 0, `submit execution must succeed: ${JSON.stringify(body)}`)
  return body.data
}

async function approveCurrentTask(page, executionId, workTaskId, label) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-approval/detail?id=${executionId}&workTaskId=${workTaskId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('eDHR 审批详情').first().waitFor({ state: 'visible', timeout: 60000 })
  const approveButton = page.locator('button').filter({ hasText: /通过/ }).first()
  await approveButton.waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(approveButton, `${label} approve`)
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), REVIEWER.password, `${label} password`)
  const textareas = dialog.locator('textarea')
  if ((await textareas.count()) > 0 && (await textareas.first().isVisible())) {
    await textareas.first().fill(`role permission real e2e ${label}`)
  }
  const approveResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/approve') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 定|确 认|确认/ }), `${label} confirm`)
  const response = await approveResponse
  const body = await response.json()
  assert.equal(body.code, 0, `${label} approve must succeed: ${JSON.stringify(body)}`)
  return body.data
}

async function verifyClosedExecutionReadOnly(browser, executionId, evidence) {
  const finalExecution = loadExecutionStatus(executionId)
  const finalBatchTask = loadBatchTaskState(BATCH_TASK_ID)
  const finalWorkTasks = loadWorkTasks(executionId)
  const reviewDoneTask = finalWorkTasks.find((task) => task.taskType === 'REVIEW')
  const approveDoneTask = finalWorkTasks.find((task) => task.taskType === 'APPROVE')
  assert.equal(Number(finalExecution.status), 3, `closed-mode execution must be approved/closed: ${JSON.stringify(finalExecution)}`)
  assert.equal(Number(finalExecution.approvedBy), REVIEWER.userId, 'closed-mode approvedBy must remain approver user')
  assert.equal(Number(finalBatchTask.status), 40, `closed-mode route form task must stay approved status=40: ${JSON.stringify(finalBatchTask)}`)
  assert.ok(finalWorkTasks.every((task) => task.status === 'DONE'), `closed-mode all work tasks must be DONE: ${JSON.stringify(finalWorkTasks)}`)
  assert.ok(reviewDoneTask, `closed-mode must have review work task: ${JSON.stringify(finalWorkTasks)}`)
  assert.ok(approveDoneTask, `closed-mode must have approve work task: ${JSON.stringify(finalWorkTasks)}`)

  const fillerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const fillerPage = await fillerContext.newPage()
  await login(fillerPage, TEST_TENANT, FILLER.username, FILLER.password, `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)
  const fillerDetail = await getJsonFromPage(fillerPage, `/mes/pro/edhr-batch-execution/get?id=${BATCH_ID}`)
  assert.equal(fillerDetail.status, 200, 'closed filler detail HTTP must be 200')
  assert.equal(fillerDetail.body.code, 0, `closed filler detail must succeed: ${JSON.stringify(fillerDetail.body)}`)
  const fillerTask = (fillerDetail.body.data.tasks || []).find((task) => Number(task.id) === Number(BATCH_TASK_ID))
  assert.ok(fillerTask, 'closed filler detail must contain target route task')
  assert.deepEqual(fillerTask.allowedActions || [], [], `closed filler must not have allowed actions: ${JSON.stringify(fillerTask)}`)
  const reopenResponse = await requestJsonFromPage(fillerPage, 'POST', '/mes/pro/edhr-batch-execution/task/open', {
    batchExecutionId: BATCH_ID,
    taskId: BATCH_TASK_ID
  })
  const resubmitResponse = await requestJsonFromPage(fillerPage, 'PUT', '/mes/pro/batch-record-execution/submit', {
    id: executionId,
    workTaskId: FILL_WORK_TASK_ID,
    password: FILLER.password,
    comment: 'closed-mode resubmit must be rejected'
  })
  await fillerContext.close()

  const approverContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const approverPage = await approverContext.newPage()
  await login(approverPage, TEST_TENANT, REVIEWER.username, REVIEWER.password, `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)
  const reviewDetailResponse = await getJsonFromPage(approverPage, `/mes/pro/batch-record-execution/approval-detail?id=${executionId}&workTaskId=${reviewDoneTask.id}`)
  assert.equal(reviewDetailResponse.status, 200, 'closed reviewer approval-detail HTTP must be 200')
  assert.equal(reviewDetailResponse.body.code, 0, `closed reviewer approval-detail must succeed: ${JSON.stringify(reviewDetailResponse.body)}`)
  const approvalDetailResponse = await getJsonFromPage(approverPage, `/mes/pro/batch-record-execution/approval-detail?id=${executionId}&workTaskId=${approveDoneTask.id}`)
  assert.equal(approvalDetailResponse.status, 200, 'closed approver approval-detail HTTP must be 200')
  assert.equal(approvalDetailResponse.body.code, 0, `closed approver approval-detail must succeed: ${JSON.stringify(approvalDetailResponse.body)}`)
  const approvalData = approvalDetailResponse.body.data || {}
  const reapproveResponse = await requestJsonFromPage(approverPage, 'PUT', '/mes/pro/batch-record-execution/approve', {
    executionId,
    workTaskId: approveDoneTask.id,
    processInstanceId: approvalData.processInstanceId || 'closed-mode-process-instance',
    approvalSnapshotId: approvalData.approvalSnapshotId || 1,
    approvalSnapshotHash: approvalData.approvalSnapshotHash || 'closed-mode-snapshot-hash',
    bpmTaskId: approvalData.bpmTaskId || approveDoneTask.bpmTaskId || 'closed-mode-bpm-task',
    password: REVIEWER.password,
    comment: 'closed-mode repeat approve must be rejected'
  })
  await approverContext.close()

  const sameTenantContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const sameTenantPage = await sameTenantContext.newPage()
  await login(sameTenantPage, TEST_TENANT, SAME_TENANT_UNRELATED.username, SAME_TENANT_UNRELATED.password, `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)
  const sameTenantDetail = await getJsonFromPage(sameTenantPage, `/mes/pro/edhr-batch-execution/get?id=${BATCH_ID}`)
  assert.equal(sameTenantDetail.status, 200, 'same-tenant unrelated detail HTTP must be 200')
  let sameTenantVisibility = 'denied'
  let sameTenantTask = null
  let sameTenantSpecialSkipResponse = null
  if (sameTenantDetail.body.code === 0) {
    sameTenantVisibility = 'readonly'
    sameTenantTask = (sameTenantDetail.body.data.tasks || []).find((task) => Number(task.id) === Number(BATCH_TASK_ID))
    assert.ok(sameTenantTask, 'same-tenant unrelated detail must contain target route task')
    assert.deepEqual(sameTenantTask.allowedActions || [], [], `same-tenant unrelated must not have actions: ${JSON.stringify(sameTenantTask)}`)
    const skippedIncoming = (sameTenantDetail.body.data.tasks || []).find((task) => task.nodeType === INCOMING_NODE_TYPE)
    if (skippedIncoming) {
      sameTenantSpecialSkipResponse = await requestJsonFromPage(sameTenantPage, 'POST', '/mes/pro/edhr-batch-execution/task/special-node/skip', {
        taskId: skippedIncoming.id,
        reason: 'same-tenant unrelated special skip must be rejected',
        password: SAME_TENANT_UNRELATED.password,
        attachments: []
      })
    }
  } else {
    assert.equal(sameTenantDetail.body.code, 403, `same-tenant unrelated detail must be readonly or denied: ${JSON.stringify(sameTenantDetail.body)}`)
  }
  const sameTenantOpenResponse = await requestJsonFromPage(sameTenantPage, 'POST', '/mes/pro/edhr-batch-execution/task/open', {
    batchExecutionId: BATCH_ID,
    taskId: BATCH_TASK_ID
  })
  await sameTenantContext.close()

  evidence.steps.push({
    role: 'closed-mode-readonly-and-rejection',
    execution: finalExecution,
    batchTask: finalBatchTask,
    workTasks: finalWorkTasks,
    fillerTask,
    reviewerEvidence: {
      reviewTask: reviewDoneTask,
      approveTaskCreatedAfterReview: approveDoneTask,
      expectedActiveStageActions: ['REVIEW_APPROVE', 'REVIEW_REJECT'],
      readonlyBasis: 'approval-detail page uses submitted execution data and review work task; reviewer is not a fill assignee and cannot save or submit fields',
      approvalDetail: {
        httpStatus: reviewDetailResponse.status,
        code: reviewDetailResponse.body.code,
        workTaskId: reviewDoneTask.id,
        taskType: reviewDoneTask.taskType,
        status: reviewDoneTask.status
      }
    },
    fillerRejections: {
      reopen: assertRejected(reopenResponse, 'closed filler reopen'),
      resubmit: assertRejected(resubmitResponse, 'closed filler resubmit')
    },
    approverRejections: {
      repeatApprove: assertRejected(reapproveResponse, 'closed approver repeat approve')
    },
    sameTenantUnrelated: {
      userId: SAME_TENANT_UNRELATED.userId,
      visibility: sameTenantVisibility,
      detail: {
        httpStatus: sameTenantDetail.status,
        code: sameTenantDetail.body.code,
        message: sameTenantDetail.body.msg
      },
      task: sameTenantTask,
      open: assertRejected(sameTenantOpenResponse, 'same-tenant unrelated open'),
      specialSkip: sameTenantSpecialSkipResponse
        ? assertRejected(sameTenantSpecialSkipResponse, 'same-tenant unrelated special skip')
        : null
    }
  })
}

async function run() {
  assert.match(BASE_URL, /^http:\/\/(127\.0\.0\.1|localhost):8095$/, 'role E2E must use worktree frontend 8095')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48095$/, 'role E2E must use worktree backend 48095')
  ensureDir(RESULT_DIR)
  ensureDir(TASK_DIR)

  const initial = loadTargetState()
  const resumedExecutionStatus = initial.executionId ? loadExecutionStatus(initial.executionId) : undefined
  const canStartFromFill = initial.fillWorkTaskStatus === 'TODO'
  const canResumeAfterSubmit =
    initial.fillWorkTaskStatus === 'DONE' &&
    initial.executionId &&
    Number(resumedExecutionStatus?.status) === 1
  const canVerifyClosed =
    initial.fillWorkTaskStatus === 'DONE' &&
    initial.executionId &&
    Number(resumedExecutionStatus?.status) === 3
  if (!canStartFromFill && !canResumeAfterSubmit && !canVerifyClosed) {
    throw blocked('目标真实任务已被消费，不能重复执行会写入的角色 E2E。', [JSON.stringify(initial)])
  }

  const browser = await chromium.launch({
    headless: process.env.EDHR_ROLE_E2E_HEADED !== '1',
    executablePath: fs.existsSync(BROWSER_EXECUTABLE) ? BROWSER_EXECUTABLE : undefined
  })
  const evidence = {
    routeCode: ROUTE_CODE,
    routeId: ROUTE_ID,
    routeProcessId: ROUTE_PROCESS_ID,
    batchExecutionId: BATCH_ID,
    batchTaskId: BATCH_TASK_ID,
    fillWorkTaskId: FILL_WORK_TASK_ID,
    tenant: TEST_TENANT,
    accounts: {
      filler: { username: FILLER.username, userId: FILLER.userId },
      reviewer: { username: REVIEWER.username, userId: REVIEWER.userId },
      productionOwner: { username: PRODUCTION_OWNER.username, userId: PRODUCTION_OWNER.userId },
      sameTenantUnrelated: { username: SAME_TENANT_UNRELATED.username, userId: SAME_TENANT_UNRELATED.userId },
      crossTenantUnrelated: { username: UNRELATED.username, tenant: UNRELATED.tenant }
    },
    initial,
    steps: []
  }
  try {
    let executionId = Number(initial.executionId || 0)
    if (canStartFromFill) {
      const ownerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
      const ownerPage = await ownerContext.newPage()
      await login(ownerPage, TEST_TENANT, PRODUCTION_OWNER.username, PRODUCTION_OWNER.password, `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)
      const ownerDetail = await loadBatchDetail(ownerPage, BATCH_ID)
      const incomingTask = (ownerDetail.tasks || []).find((task) => task.nodeType === INCOMING_NODE_TYPE)
      assert.ok(incomingTask, `batch detail must contain incoming inspection task: ${JSON.stringify(ownerDetail.tasks || [])}`)
      if (incomingTask.status === 45) {
        evidence.steps.push({ role: 'production-owner-before-special-node-skip', task: incomingTask, alreadySkipped: true })
      } else {
        const ownerIncomingTask = await assertTaskDetail(ownerPage, BATCH_ID, incomingTask.id, 'PRODUCTION_OWNER', ['CLOSE'], 'production-owner-incoming')
        evidence.steps.push({ role: 'production-owner-before-special-node-skip', task: ownerIncomingTask })
        await skipIncomingInspectionAsProductionOwner(ownerPage, BATCH_ID)
      }
      await snapshot(ownerPage, '00-production-owner-skipped-incoming')
      await ownerContext.close()

      const fillerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
      const fillerPage = await fillerContext.newPage()
      await login(fillerPage, TEST_TENANT, FILLER.username, FILLER.password, `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)
      const fillerTask = await assertRoleDetail(fillerPage, BATCH_ID, 'FILLER', ['OPEN_FORM', 'SAVE_FORM', 'SUBMIT'], 'filler')
      assert.equal(fillerTask.processName, '吹球囊成型', 'filler must show process name')
      evidence.steps.push({ role: 'filler-before-open', task: fillerTask })
      executionId = await openExecutionFromBatchDetail(fillerPage, BATCH_ID, initial)
      evidence.executionId = executionId
      await fillAndSaveDraft(fillerPage, executionId)
      await snapshot(fillerPage, '01-filler-saved-draft')
      await submitExecution(fillerPage)
      await snapshot(fillerPage, '02-filler-submitted')
      await fillerContext.close()
    } else {
      evidence.executionId = executionId
      evidence.steps.push({
        role: canVerifyClosed ? 'resume-after-close' : 'resume-after-submit',
        execution: resumedExecutionStatus,
        target: initial
      })
      if (canVerifyClosed) {
        await verifyClosedExecutionReadOnly(browser, executionId, evidence)

        const unrelatedContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
        const unrelatedPage = await unrelatedContext.newPage()
        await login(unrelatedPage, UNRELATED.tenant, UNRELATED.username, UNRELATED.password, `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)
        const unrelatedResponse = await getJsonFromPage(unrelatedPage, `/mes/pro/edhr-batch-execution/get?id=${BATCH_ID}`)
        evidence.steps.push({ role: 'cross-tenant-unrelated-readonly', status: unrelatedResponse.status, bodyCode: unrelatedResponse.body.code })
        await unrelatedContext.close()

        fs.writeFileSync(EVIDENCE_FILE, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
        console.log(`PASS: edhr batch role permission closed verification batch=${BATCH_ID} execution=${executionId}`)
        console.log(`PASS: evidence=${EVIDENCE_FILE}`)
        return
      }
    }

    const reviewTasksAfterSubmit = loadWorkTasks(executionId)
    const reviewTask = reviewTasksAfterSubmit.find((task) => task.taskType === 'REVIEW' && task.status === 'TODO')
    assert.ok(reviewTask, `submit must create REVIEW task: ${JSON.stringify(reviewTasksAfterSubmit)}`)
    assert.equal(Number(reviewTask.assigneeUserId), REVIEWER.userId, 'REVIEW task must be assigned to reviewer')
    evidence.steps.push({ role: 'after-submit', workTasks: reviewTasksAfterSubmit })

    const reviewerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const reviewerPage = await reviewerContext.newPage()
    await login(reviewerPage, TEST_TENANT, REVIEWER.username, REVIEWER.password, `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)
    const reviewerTask = await assertRoleDetail(reviewerPage, BATCH_ID, 'REVIEWER', ['REVIEW_APPROVE', 'REVIEW_REJECT'], 'reviewer')
    evidence.steps.push({ role: 'reviewer-before-approve', task: reviewerTask })
    await approveCurrentTask(reviewerPage, executionId, reviewTask.id, 'review')
    await snapshot(reviewerPage, '03-reviewer-approved')

    const tasksAfterReview = loadWorkTasks(executionId)
    const approveTask = tasksAfterReview.find((task) => task.taskType === 'APPROVE' && task.status === 'TODO')
    assert.ok(approveTask, `review must create APPROVE task: ${JSON.stringify(tasksAfterReview)}`)
    assert.equal(Number(approveTask.assigneeUserId), REVIEWER.userId, 'APPROVE task must prefer explicit assignee inside candidate pool')
    evidence.steps.push({ role: 'after-review', workTasks: tasksAfterReview })

    const approverTask = await assertRoleDetail(reviewerPage, BATCH_ID, 'APPROVER', ['APPROVE', 'REJECT'], 'approver')
    evidence.steps.push({ role: 'approver-before-approve', task: approverTask })
    await approveCurrentTask(reviewerPage, executionId, approveTask.id, 'approve')
    await snapshot(reviewerPage, '04-approver-approved')
    await reviewerContext.close()

    const tasksAfterApprove = loadWorkTasks(executionId)
    const finalExecution = loadExecutionStatus(executionId)
    assert.equal(Number(finalExecution.status), 3, `execution must be APPROVED after approve task: ${JSON.stringify(finalExecution)}`)
    assert.equal(Number(finalExecution.approvedBy), REVIEWER.userId, 'approvedBy must be reviewer/approver user')
    evidence.steps.push({ role: 'after-approve', workTasks: tasksAfterApprove, execution: finalExecution })

    const unrelatedContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const unrelatedPage = await unrelatedContext.newPage()
    await login(unrelatedPage, UNRELATED.tenant, UNRELATED.username, UNRELATED.password, `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)
    const unrelatedResponse = await getJsonFromPage(unrelatedPage, `/mes/pro/edhr-batch-execution/get?id=${BATCH_ID}`)
    evidence.steps.push({ role: 'cross-tenant-unrelated-readonly', status: unrelatedResponse.status, bodyCode: unrelatedResponse.body.code })
    await unrelatedContext.close()

    fs.writeFileSync(EVIDENCE_FILE, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.log(`PASS: edhr batch role permission real flow batch=${BATCH_ID} execution=${executionId}`)
    console.log(`PASS: evidence=${EVIDENCE_FILE}`)
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  if (error.blocked) {
    console.error(`BLOCKED: ${error.message}`)
    for (const detail of error.details || []) console.error(`- ${detail}`)
  } else {
    console.error(error)
  }
  process.exitCode = 1
})
