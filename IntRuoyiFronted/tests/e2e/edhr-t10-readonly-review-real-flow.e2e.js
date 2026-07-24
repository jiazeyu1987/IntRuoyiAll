const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_T10_E2E_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_T10_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.EDHR_T10_E2E_LOGIN_PASSWORD || '111111'
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.EDHR_T10_E2E_ADMIN_PASSWORD || 'admin123'
const REVIEW_ROUTE = '/mes/pro/feedback/edhr-batch-execution/review'
const DETAIL_ROUTE = '/mes/pro/feedback/edhr-batch-execution/detail'
const TASK_STATE_PATH = path.resolve(__dirname, '../../../doc/tasks/20260613-batch-record-gap-implementation/task-state.json')
const RUN_KEY = `T10-READONLY-REVIEW-${Date.now()}`
const SPECIAL_NODE_TYPES = [
  'INCOMING_INSPECTION_REPORT',
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD'
]

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'T10 E2E must use local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'T10 E2E must use local backend 48081')
}

function requireRealGate(taskId) {
  const state = JSON.parse(fs.readFileSync(TASK_STATE_PATH, 'utf8'))
  const task = state.tasks.find((item) => item.task_id === taskId)
  if (!task) {
    throw blocked(`task-state.json 缺少 ${taskId} 状态，不能判断 T10 真实 E2E 依赖门禁。`)
  }
  if (task.status !== 'validated_real_e2e_pass') {
    throw blocked(`T10 真实 E2E 依赖 ${taskId} 真实门禁先通过。`, [
      `当前 ${taskId} 状态：${task.status}`,
      `当前 ${taskId} 结果：${task.last_outcome || '--'}`
    ])
  }
}

function requirePrerequisites() {
  const blockers = []
  for (const taskId of ['T7', 'T8', 'T9']) {
    try {
      requireRealGate(taskId)
    } catch (error) {
      if (!error.blocked) throw error
      blockers.push(error.message)
      blockers.push(...(error.details || []))
    }
  }
  if (blockers.length > 0) {
    throw blocked('T10 real E2E 前置条件未满足。', [
      ...blockers,
      'T10 本身不执行签名写入，但必须依赖 T9 真实关闭批次；不得用 mock、SQL 造关闭状态、接口造成功或外部服务器替代。'
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

function findClosedBatchCandidate() {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'batchExecutionId', b.id,
  'batchExecutionCode', b.batch_execution_code,
  'batchCode', b.batch_code,
  'status', b.status,
  'closedAt', b.closed_at,
  'routeTaskCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id AND bt.deleted=0 AND bt.batch_execution_id=b.id AND bt.node_type='ROUTE_FORM'
  ),
  'specialNodeCount', (
    SELECT COUNT(DISTINCT bt.node_type)
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id AND bt.deleted=0 AND bt.batch_execution_id=b.id AND bt.node_type<>'ROUTE_FORM'
  ),
  'signatureCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_execution_signature s
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=s.execution_id AND bt.tenant_id=s.tenant_id AND bt.deleted=0
    WHERE s.tenant_id=b.tenant_id AND s.deleted=0 AND bt.batch_execution_id=b.id
  ),
  'approvalCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_approval_snapshot a
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=a.execution_id AND bt.tenant_id=a.tenant_id AND bt.deleted=0
    WHERE a.tenant_id=b.tenant_id AND a.deleted=0 AND bt.batch_execution_id=b.id
  )
)
FROM mes_pro_edhr_batch_execution b
WHERE b.tenant_id=122
  AND b.deleted=0
  AND b.status IN (30,40)
  AND b.closed_at IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type='ROUTE_FORM'
  )
  AND (
    SELECT COUNT(DISTINCT bt.node_type)
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type IN ('INCOMING_INSPECTION_REPORT','STERILIZATION_REPORT','FINISHED_PRODUCT_INSPECTION_REPORT','FINISHED_PRODUCT_INSPECTION_RECORD')
  ) = 4
ORDER BY b.closed_at DESC, b.id DESC
LIMIT 1;
`)
  return parseJsonRow(output, 'T10 closed batch candidate')
}

function loadReadonlyDbEvidence(batchExecutionId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'status', b.status,
  'closedAt', b.closed_at,
  'taskCount', (
    SELECT COUNT(*) FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id AND bt.deleted=0 AND bt.batch_execution_id=b.id
  ),
  'routeTaskCount', (
    SELECT COUNT(*) FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id AND bt.deleted=0 AND bt.batch_execution_id=b.id AND bt.node_type='ROUTE_FORM'
  ),
  'specialNodeTypes', (
    SELECT JSON_ARRAYAGG(x.node_type)
    FROM (
      SELECT DISTINCT bt.node_type
      FROM mes_pro_edhr_batch_execution_task bt
      WHERE bt.tenant_id=b.tenant_id AND bt.deleted=0 AND bt.batch_execution_id=b.id AND bt.node_type<>'ROUTE_FORM'
      ORDER BY bt.node_type
    ) x
  ),
  'signatureCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_execution_signature s
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=s.execution_id AND bt.tenant_id=s.tenant_id AND bt.deleted=0
    WHERE s.tenant_id=b.tenant_id AND s.deleted=0 AND bt.batch_execution_id=b.id
  ),
  'approvalCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_approval_snapshot a
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=a.execution_id AND bt.tenant_id=a.tenant_id AND bt.deleted=0
    WHERE a.tenant_id=b.tenant_id AND a.deleted=0 AND bt.batch_execution_id=b.id
  ),
  'fieldAuditBatchCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_execution_field_audit_batch ab
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=ab.execution_id AND bt.tenant_id=ab.tenant_id AND bt.deleted=0
    WHERE ab.tenant_id=b.tenant_id AND ab.deleted=0 AND bt.batch_execution_id=b.id
  )
)
FROM mes_pro_edhr_batch_execution b
WHERE b.tenant_id=122 AND b.deleted=0 AND b.id=${Number(batchExecutionId)}
LIMIT 1;
`)
  return parseJsonRow(output, 'T10 readonly DB evidence')
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

async function login(page, tenant, username, password, redirect = REVIEW_ROUTE) {
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
  await page.waitForFunction(
    () => !window.location.href.includes('/login') || Boolean(window.localStorage.getItem('ACCESS_TOKEN')),
    { timeout: 60000 }
  )
  if (page.url().includes('/login')) {
    await page.goto(`${BASE_URL}${redirect}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  }
}

function assertReviewTimelineCompleteness(timeline, dbEvidence) {
  assert.equal(Number(timeline.batchExecutionId), Number(dbEvidence.batchExecutionId), 'review timeline must belong to target batch')
  assert.ok(Array.isArray(timeline.taskEvents), 'review timeline must expose taskEvents')
  assert.ok(Array.isArray(timeline.executionReviews), 'review timeline must expose executionReviews')
  assert.ok(Array.isArray(timeline.signatureRecords), 'review timeline must expose signatureRecords')
  assert.ok(Array.isArray(timeline.approvalRecords), 'review timeline must expose approvalRecords')
  assert.ok(Array.isArray(timeline.archiveVersions), 'review timeline must expose archiveVersions or controlled manifest list')

  const taskEvents = timeline.taskEvents || []
  const executionReviews = timeline.executionReviews || []
  const dossierItems = timeline.dossierItems || []
  const timelineText = JSON.stringify([...taskEvents, ...dossierItems])
  for (const nodeType of SPECIAL_NODE_TYPES) {
    assert.ok(timelineText.includes(nodeType), `readonly review must expose special node ${nodeType}`)
  }
  assert.ok(executionReviews.length > 0, 'readonly review must expose normal route form reviews')
  assert.ok(
    executionReviews.every((item) => item.formViewModel || item.batchRecordReportName || item.executionCode),
    'each normal form review must expose a readonly form model or identifying form metadata'
  )
  assert.ok(
    [
      ...(timeline.signatureRecords || []),
      ...executionReviews.flatMap((item) => item.signatureRecords || [])
    ].length >= Number(dbEvidence.signatureCount || 0),
    'readonly review signature records must cover DB signature evidence'
  )
  assert.ok(
    (timeline.approvalRecords || []).length >= Number(dbEvidence.approvalCount || 0),
    'readonly review approval records must cover DB approval evidence'
  )
  assert.ok(
    executionReviews.some((item) => item.fieldAuditSummary || item.domainTraceSummary || item.attachmentSummaries),
    'readonly review must expose field audit, tracking or attachment summaries'
  )
}

async function openReadonlyReview(page, candidate, writeRequests) {
  const reviewResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/review-timeline') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}${REVIEW_ROUTE}?id=${candidate.batchExecutionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('已填写批记录').first().waitFor({ state: 'visible', timeout: 60000 })
  const response = await reviewResponsePromise
  const body = await response.json()
  const timeline = body.data || body.result || body
  assert.deepEqual(writeRequests, [], `readonly review page must not issue MES writes: ${JSON.stringify(writeRequests)}`)
  return timeline
}

async function assertDetailClosedReadonly(page, candidate, writeRequests) {
  await page.goto(`${BASE_URL}${DETAIL_ROUTE}?id=${candidate.batchExecutionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('eDHR批次详情').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(String(candidate.batchCode || candidate.batchExecutionCode)).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('已关闭').or(page.getByText('已归档')).first().waitFor({ state: 'visible', timeout: 60000 })
  const forbiddenEnabledButtons = ['关闭批次', '完成', '跳过', '提交', '保存', '质量拒收']
  for (const label of forbiddenEnabledButtons) {
    const buttons = page.getByRole('button', { name: label })
    const count = await buttons.count()
    for (let index = 0; index < count; index += 1) {
      assert.equal(await buttons.nth(index).isDisabled(), true, `closed batch detail must not expose enabled write button: ${label}`)
    }
  }
  assert.deepEqual(writeRequests, [], `closed detail readonly verification must not issue MES writes: ${JSON.stringify(writeRequests)}`)
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
    await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD, `${REVIEW_ROUTE}?id=${batchExecutionId}`)
    await page.goto(`${BASE_URL}${REVIEW_ROUTE}?id=${batchExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.getByText('已填写批记录').first().waitFor({ state: 'visible', timeout: 60000 })
    assert.deepEqual(writeRequests, [], `admin readonly review must not issue MES writes: ${JSON.stringify(writeRequests)}`)
  } finally {
    await context.close()
  }
}

async function run() {
  assertLocalOnly()
  requirePrerequisites()

  const candidate = findClosedBatchCandidate()
  if (!candidate) {
    throw blocked('测试租户/aoteman 当前不存在可用于 T10 只读复盘验证的真实已关闭批次。', [
      '需要 T9 通过真实关闭校验后产生 closedAt 非空、状态 CLOSED/ARCHIVED 的批次。',
      '不得用 SQL 修改状态、接口造成功、mock timeline 或外部服务器替代。'
    ])
  }
  const dbEvidence = {
    ...loadReadonlyDbEvidence(candidate.batchExecutionId),
    batchExecutionId: candidate.batchExecutionId
  }
  if (Number(candidate.routeTaskCount || 0) === 0 || Number(candidate.specialNodeCount || 0) < 4) {
    throw blocked('候选已关闭批次不具备 T10 完整复盘所需路线/四类特殊节点证据。', [
      `candidate=${JSON.stringify(candidate)}`,
      `dbEvidence=${JSON.stringify(dbEvidence)}`
    ])
  }

  const browser = await chromium.launch({ headless: process.env.EDHR_T10_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const writeRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      writeRequests.push(`${request.method()} ${request.url()}`)
    }
  })
  try {
    await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD, `${REVIEW_ROUTE}?id=${candidate.batchExecutionId}`)
    const timeline = await openReadonlyReview(page, candidate, writeRequests)
    assertReviewTimelineCompleteness(timeline, dbEvidence)
    await assertDetailClosedReadonly(page, candidate, writeRequests)
    await verifyAdminReadonly(browser, candidate.batchExecutionId)
    console.log(`PASS: T10 readonly review real E2E runKey=${RUN_KEY} batch=${candidate.batchExecutionId}`)
    console.log(`PASS: dbEvidence=${JSON.stringify(dbEvidence)}`)
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
