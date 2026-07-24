const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_T9_E2E_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_T9_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.EDHR_T9_E2E_LOGIN_PASSWORD || '111111'
const SIGNATURE_PASSWORD = process.env.EDHR_T9_E2E_SIGNATURE_PASSWORD
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.EDHR_T9_E2E_ADMIN_PASSWORD || 'admin123'
const BATCH_DETAIL_ROUTE = '/mes/pro/feedback/edhr-batch-execution/detail'
const BATCH_LIST_ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const TASK_STATE_PATH = path.resolve(__dirname, '../../../doc/tasks/20260613-batch-record-gap-implementation/task-state.json')
const RUN_KEY = `T9-CLOSE-BLOCKERS-${Date.now()}`

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'T9 E2E must use local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'T9 E2E must use local backend 48081')
}

function requireRealGate(taskId) {
  const state = JSON.parse(fs.readFileSync(TASK_STATE_PATH, 'utf8'))
  const task = state.tasks.find((item) => item.task_id === taskId)
  if (!task) {
    throw blocked(`task-state.json 缺少 ${taskId} 状态，不能判断 T9 真实 E2E 依赖门禁。`)
  }
  if (task.status !== 'validated_real_e2e_pass') {
    throw blocked(`T9 真实 E2E 依赖 ${taskId} 真实写入门禁先通过。`, [
      `当前 ${taskId} 状态：${task.status}`,
      `当前 ${taskId} 结果：${task.last_outcome || '--'}`
    ])
  }
}

function requirePrerequisites() {
  const blockers = []
  if (!SIGNATURE_PASSWORD) {
    blockers.push('缺少 EDHR_T9_E2E_SIGNATURE_PASSWORD，不能执行 T9 批次关闭真实签名链路。')
  }
  for (const taskId of ['T6', 'T7', 'T8']) {
    try {
      requireRealGate(taskId)
    } catch (error) {
      if (!error.blocked) throw error
      blockers.push(error.message)
      blockers.push(...(error.details || []))
    }
  }
  if (blockers.length > 0) {
    throw blocked('T9 real E2E 前置条件未满足。', [
      ...blockers,
      '不得使用登录密码、mock、接口造数、跳过签名或外部服务器替代。'
    ])
  }
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

function findCloseBlockerCandidate() {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'batchExecutionId', b.id,
  'batchExecutionCode', b.batch_execution_code,
  'batchCode', b.batch_code,
  'status', b.status,
  'closedAt', b.closed_at,
  'unfinishedWorkTaskCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_work_task wt
    WHERE wt.tenant_id=b.tenant_id
      AND wt.deleted=0
      AND wt.batch_execution_id=b.id
      AND wt.status IN ('TODO', 'DOING', 'OVERDUE')
  ),
  'unfinishedRouteFormCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_batch_execution_task bt
    LEFT JOIN mes_pro_batch_record_execution e ON e.id=bt.execution_id AND e.tenant_id=bt.tenant_id AND e.deleted=0
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type='ROUTE_FORM'
      AND bt.required_flag=1
      AND (bt.execution_id IS NULL OR bt.status<>40 OR e.status<>3)
  ),
  'missingSignatureCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_batch_execution_task bt
    JOIN mes_pro_batch_record_execution e ON e.id=bt.execution_id AND e.tenant_id=bt.tenant_id AND e.deleted=0
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type='ROUTE_FORM'
      AND bt.required_flag=1
      AND (
        SELECT COUNT(DISTINCT s.action_type)
        FROM mes_pro_batch_record_execution_signature s
        WHERE s.tenant_id=bt.tenant_id
          AND s.deleted=0
          AND s.execution_id=bt.execution_id
          AND s.action_type = 'SUBMIT'
      ) < 1
  ),
  'auditOrTraceInvalidCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_batch_execution_task bt
    JOIN mes_pro_batch_record_execution e ON e.id=bt.execution_id AND e.tenant_id=bt.tenant_id AND e.deleted=0
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type='ROUTE_FORM'
      AND bt.required_flag=1
      AND (e.cell_values_hash IS NULL OR e.cell_values_hash='' OR e.field_audit_head_hash IS NULL OR e.field_audit_head_hash='' OR e.domain_trace_status<>'VERIFIED')
  ),
  'missingAttachmentEvidenceCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_batch_execution_task bt
    JOIN mes_pro_batch_record_execution e ON e.id=bt.execution_id AND e.tenant_id=bt.tenant_id AND e.deleted=0
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type='ROUTE_FORM'
      AND bt.required_flag=1
      AND JSON_CONTAINS_PATH(e.execution_snapshot_json, 'one', '$.attachmentRule.required') = 1
      AND JSON_EXTRACT(e.execution_snapshot_json, '$.attachmentRule.required') = true
      AND NOT EXISTS (
        SELECT 1
        FROM mes_pro_batch_record_execution_attachment a
        WHERE a.tenant_id=e.tenant_id
          AND a.deleted=0
          AND a.execution_id=e.id
      )
  ),
  'unfinishedSpecialNodeCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type<>'ROUTE_FORM'
      AND bt.required_flag=1
      AND bt.status NOT IN (40, 45)
  )
)
FROM mes_pro_edhr_batch_execution b
WHERE b.tenant_id=122
  AND b.deleted=0
  AND b.status NOT IN (30, 40, 50)
  AND (
    EXISTS (
      SELECT 1
      FROM mes_pro_edhr_work_task wt
      WHERE wt.tenant_id=b.tenant_id
        AND wt.deleted=0
        AND wt.batch_execution_id=b.id
        AND wt.status IN ('TODO', 'DOING', 'OVERDUE')
    )
    OR EXISTS (
      SELECT 1
      FROM mes_pro_edhr_batch_execution_task bt
      WHERE bt.tenant_id=b.tenant_id
        AND bt.deleted=0
        AND bt.batch_execution_id=b.id
        AND bt.required_flag=1
        AND bt.status NOT IN (40, 45)
    )
  )
ORDER BY b.id DESC
LIMIT 1;
`)
  return parseJsonRow(output, 'T9 close blocker candidate')
}

function loadBatchState(batchExecutionId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT('id', id, 'status', status, 'closedAt', closed_at, 'closeSignatureId', close_signature_id)
FROM mes_pro_edhr_batch_execution
WHERE tenant_id=122 AND deleted=0 AND id=${Number(batchExecutionId)}
LIMIT 1;
`)
  return parseJsonRow(output, 'T9 batch state')
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

async function login(page, tenant, username, password, redirect = BATCH_LIST_ROUTE) {
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
  await clickFirstEnabled(loginForm.locator('button.el-button--primary'), 'login button')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openBatchDetail(page, candidate) {
  await page.goto(`${BASE_URL}${BATCH_DETAIL_ROUTE}?id=${candidate.batchExecutionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('eDHR 批次执行详情').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(String(candidate.batchCode || candidate.batchExecutionCode)).first().waitFor({ state: 'visible', timeout: 60000 })
}

async function submitCloseExpectBlocked(page, candidate) {
  const blockerItems = page.locator('.edhr-batch-detail__blocker-item')
  const blockerCount = await blockerItems.count()
  assert.ok(blockerCount > 0, 'T9 detail page must expose close blocker list before close attempt')
  const pageBlockers = []
  for (let index = 0; index < blockerCount; index += 1) {
    pageBlockers.push((await blockerItems.nth(index).innerText()).trim())
  }

  const closeButton = page.getByRole('button', { name: '关闭批次' }).first()
  if ((await closeButton.isDisabled())) {
    throw blocked('当前详情页因 canClose=false 禁用了关闭按钮，无法验证“点击关闭后的失败响应”。', [
      'T9 GREEN 实现阶段需要提供可触发关闭校验失败的真实页面入口，或让关闭按钮在 blockers 存在时打开只读失败明细。',
      `pageBlockers=${JSON.stringify(pageBlockers)}`
    ])
  }
  await closeButton.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '关闭 eDHR 批次' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('textarea'), `T9 close blockers ${RUN_KEY}`, 'close comment')
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="密码"]'), SIGNATURE_PASSWORD, 'close password')
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/close') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确 定|确 认|确认/ }), 'confirm close')
  const response = await responsePromise
  const body = await response.json()
  assert.equal(response.status(), 200, 'close API must return business envelope')
  assert.notEqual(body.code, 0, `T9 close with blockers must fail: ${JSON.stringify(body)}`)
  const message = String(body.msg || body.message || '')
  assert.ok(message.trim().length > 0, `blocked close response must expose blocker message: ${JSON.stringify(body)}`)
  return { response: body, pageBlockers }
}

async function verifyAdminReadonly(browser, batchExecutionId) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const writeRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      writeRequests.push(`${request.method()} ${request.url()}`)
    }
  })
  try {
    await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD, `${BATCH_DETAIL_ROUTE}?id=${batchExecutionId}`)
    await page.goto(`${BASE_URL}${BATCH_DETAIL_ROUTE}?id=${batchExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.getByText('eDHR 批次执行详情').first().waitFor({ state: 'visible', timeout: 60000 })
    assert.deepEqual(writeRequests, [], `admin readonly verification must not issue MES writes: ${JSON.stringify(writeRequests)}`)
  } finally {
    await context.close()
  }
}

async function run() {
  assertLocalOnly()
  requirePrerequisites()

  const candidate = findCloseBlockerCandidate()
  if (!candidate) {
    throw blocked('测试租户/aoteman 当前不存在可用于 T9 关闭 blocker 验证的真实未关闭批次。', [
      '需要已有真实批次包含未完成任务、缺签名、审计/追溯/附件/特殊节点/审批 blocker。',
      '不得用 SQL 插入测试专用成功数据替代真实页面路径。'
    ])
  }
  const evidenceCounts = [
    candidate.unfinishedWorkTaskCount,
    candidate.unfinishedRouteFormCount,
    candidate.missingSignatureCount,
    candidate.auditOrTraceInvalidCount,
    candidate.missingAttachmentEvidenceCount,
    candidate.unfinishedSpecialNodeCount
  ].map((value) => Number(value || 0))
  if (!evidenceCounts.some((value) => value > 0)) {
    throw blocked('当前候选批次没有 T9 可观察 blocker，无法验证关闭前完整性校验。', [
      `candidate=${JSON.stringify(candidate)}`
    ])
  }

  const beforeState = loadBatchState(candidate.batchExecutionId)
  const browser = await chromium.launch({ headless: process.env.EDHR_T9_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD, `${BATCH_DETAIL_ROUTE}?id=${candidate.batchExecutionId}`)
    await openBatchDetail(page, candidate)
    const closeResult = await submitCloseExpectBlocked(page, candidate)
    const afterState = loadBatchState(candidate.batchExecutionId)
    assert.deepEqual(afterState, beforeState, `blocked close must keep DB state unchanged: before=${JSON.stringify(beforeState)} after=${JSON.stringify(afterState)}`)
    await verifyAdminReadonly(browser, candidate.batchExecutionId)
    console.log(`PASS: T9 close blockers real E2E runKey=${RUN_KEY} batch=${candidate.batchExecutionId}`)
    console.log(`PASS: pageBlockers=${JSON.stringify(closeResult.pageBlockers)} response=${JSON.stringify(closeResult.response)}`)
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
