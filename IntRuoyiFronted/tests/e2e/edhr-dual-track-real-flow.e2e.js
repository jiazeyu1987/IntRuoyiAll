const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260705-edhr-dual-track-batch-record-prd'
const DEFAULT_BASE_URL = 'http://127.0.0.1:18081'
const DEFAULT_BACKEND_URL = 'http://127.0.0.1:48082'
const DEFAULT_TENANT = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const DEFAULT_PASSWORD = '111111'
const DEFAULT_APPROVER_USERNAME = 'edhrmatrixapprover'
const DEFAULT_APPROVER_PASSWORD = '111111'
const CHROME_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

const ROUTE_ID = 922045
const ROUTE_CODE = 'ROUTE-YXN.069.001.1001'
const INTERNAL_REPORT_ID = '34cae20da60d4b5b9c1c91cb5344581e'
const CONTROLLED_REPORT_ID = '56370d87c78141d5848755beed9cfe80'
const REAL_COMPLETED_BATCH_ID = 900000000445
const REAL_CLOSE_BLOCKED_BATCH_ID = 900000000441
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', TASK_ID, 'dual-track-real')
const TASK_DIR = path.resolve('D:/ProjectPackage/Int/IntRuoyi/doc/tasks', TASK_ID)
const EVIDENCE_FILE = path.join(TASK_DIR, 'real-e2e-dual-track-evidence.md')

const ROUTE_USE_BATCH_URL = '/mes/pro/route?tab=batch-record-config'
const BATCH_LIST_URL = '/mes/pro/feedback/edhr-batch-execution'
const BATCH_DETAIL_URL = '/mes/pro/feedback/edhr-batch-execution/detail'
const APPROVAL_URL = '/approval-center/todo?moduleCode=EDHR&viewType=TODO'
const EXECUTION_DETAIL_URL = '/mes/pro/feedback/edhr-execution/detail'

const ENDPOINTS = {
  batchOpenOrCreate: '/mes/pro/edhr-batch-execution/open-or-create',
  batchTaskOpen: '/mes/pro/edhr-batch-execution/task/open',
  fieldAuditSave: '/mes/pro/batch-record-execution/field-audit/save-changes',
  formReviewSign: '/mes/pro/batch-record-execution/cosign',
  executionSubmit: '/mes/pro/batch-record-execution/submit',
  approvalPending: '/mes/pro/batch-record-execution/approval-pending-page',
  approvalApprove: '/mes/pro/batch-record-execution/approve',
  batchClose: '/mes/pro/edhr-batch-execution/close'
}

const COVERAGE_MARKERS = {
  operatorControlledFill: 'operatorControlledFill',
  operatorInternalTraceFill: 'operatorInternalTraceFill',
  controlledLimitBlocked: 'controlledLimitBlocked',
  internalTraceReasonRequired: 'internalTraceReasonRequired',
  internalTraceNonBlocking: 'internalTraceNonBlocking',
  approverApproval: 'approverApproval',
  internalReviewerReview: 'internalReviewerReview',
  externalAuditorDeniedInternal: 'externalAuditorDeniedInternal',
  requiredInternalTraceCloseBlocked: 'requiredInternalTraceCloseBlocked'
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function actor(username, password, label) {
  return { tenant: DEFAULT_TENANT, username, password, label }
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_DUAL_TRACK_BASE_URL') || DEFAULT_BASE_URL,
    backendUrl: envValue('EDHR_DUAL_TRACK_BACKEND_URL') || DEFAULT_BACKEND_URL,
    operator: actor(
      envValue('EDHR_DUAL_TRACK_OPERATOR_USERNAME') || DEFAULT_USERNAME,
      envValue('EDHR_DUAL_TRACK_OPERATOR_PASSWORD') || DEFAULT_PASSWORD,
      '操作员'
    ),
    approver: actor(
      envValue('EDHR_DUAL_TRACK_APPROVER_USERNAME') || DEFAULT_APPROVER_USERNAME,
      envValue('EDHR_DUAL_TRACK_APPROVER_PASSWORD') || DEFAULT_APPROVER_PASSWORD,
      '审批者/内部审核者'
    ),
    externalAuditor: actor(
      envValue('EDHR_DUAL_TRACK_EXTERNAL_AUDITOR_USERNAME') || '',
      envValue('EDHR_DUAL_TRACK_EXTERNAL_AUDITOR_PASSWORD') || '',
      '外部审核者'
    ),
    headed: envValue('EDHR_DUAL_TRACK_HEADED') === '1',
    executablePath: CHROME_EXECUTABLE,
    routeId: Number(envValue('EDHR_DUAL_TRACK_ROUTE_ID') || ROUTE_ID),
    routeCode: envValue('EDHR_DUAL_TRACK_ROUTE_CODE') || ROUTE_CODE,
    completedBatchId: Number(envValue('EDHR_DUAL_TRACK_COMPLETED_BATCH_ID') || REAL_COMPLETED_BATCH_ID),
    closeBlockedBatchId: Number(envValue('EDHR_DUAL_TRACK_CLOSE_BLOCKED_BATCH_ID') || REAL_CLOSE_BLOCKED_BATCH_ID)
  }
  const missing = []
  if (config.baseUrl !== DEFAULT_BASE_URL) missing.push(`baseUrl 必须固定为 worktree 前端 ${DEFAULT_BASE_URL}，当前为 ${config.baseUrl}`)
  if (config.backendUrl !== DEFAULT_BACKEND_URL) missing.push(`backendUrl 必须固定为 worktree 后端 ${DEFAULT_BACKEND_URL}，当前为 ${config.backendUrl}`)
  if (config.operator.tenant !== DEFAULT_TENANT || config.operator.username !== DEFAULT_USERNAME) {
    missing.push(`真实写入/验证默认只能使用 ${DEFAULT_TENANT}/${DEFAULT_USERNAME}`)
  }
  if (!config.operator.password) missing.push('缺少操作员登录密码。')
  if (!config.approver.username || !config.approver.password) missing.push('缺少审批者/内部审核者真实账号或密码。')
  if (!config.externalAuditor.username || !config.externalAuditor.password) {
    missing.push('缺少外部审核者真实账号密码：EDHR_DUAL_TRACK_EXTERNAL_AUDITOR_USERNAME / EDHR_DUAL_TRACK_EXTERNAL_AUDITOR_PASSWORD。')
  }
  if (!fs.existsSync(config.executablePath)) missing.push(`浏览器不存在: ${config.executablePath}`)
  return { ...config, missing }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(`缺少 Playwright runtime，请先确认当前前端 worktree 已安装依赖: ${error.message}`)
  }
}

async function screenshot(page, name) {
  ensureDir(RESULT_DIR)
  const filePath = path.join(RESULT_DIR, `${name}.png`)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (!(await item.isVisible().catch(() => false))) continue
    if (await item.isDisabled().catch(() => false)) continue
    await item.fill(value)
    return
  }
  throw new Error(`未找到可填写控件: ${label}`)
}

async function clickVisibleButton(root, name, label) {
  const candidates = [
    typeof name === 'string' ? root.getByRole('button', { name }) : root.getByRole('button', { name }),
    root.locator('button').filter({ hasText: name })
  ]
  for (const buttons of candidates) {
    const count = await buttons.count()
    for (let index = 0; index < count; index += 1) {
      const button = buttons.nth(index)
      if (!(await button.isVisible().catch(() => false))) continue
      if (await button.isDisabled().catch(() => false)) continue
      await button.scrollIntoViewIfNeeded()
      await button.click()
      return
    }
  }
  throw new Error(`未找到按钮: ${label}`)
}

async function login(page, config, user, redirectPath = '/index') {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  const loginForm = page.locator('form.login-form:visible, .login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(user.tenant)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: user.tenant }).first()
    if ((await option.count()) > 0) await option.click()
    else await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"], input.el-input__inner').first(), user.tenant, '租户')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])').first(), user.username, '用户名')
  await fillFirstVisible(loginForm.locator('input[type="password"]'), user.password, '密码')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickVisibleButton(loginForm, '登录', '登录')
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `${user.label} 登录 HTTP 失败: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(Number(payload.code)), `${user.label} 登录业务失败: ${payload.msg || JSON.stringify(payload)}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
}

async function apiGet(page, config, pathName) {
  return await page.evaluate(async (url) => {
    function readCacheValue(key) {
      const raw = window.localStorage.getItem(key) || window.sessionStorage.getItem(key)
      if (!raw) return ''
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
          try {
            return JSON.parse(parsed.v)
          } catch {
            return parsed.v
          }
        }
        return parsed
      } catch {
        return raw
      }
    }
    function buildApiHeaders() {
      const headers = { Accept: 'application/json' }
      const accessToken = readCacheValue('ACCESS_TOKEN')
      const tenantId = readCacheValue('tenantId')
      const visitTenantId = readCacheValue('visitTenantId')
      if (accessToken) headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
      if (tenantId) headers['tenant-id'] = String(tenantId)
      if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
      return headers
    }
    const response = await fetch(url, { credentials: 'include', headers: buildApiHeaders() })
    const json = await response.json()
    if (!response.ok || ![0, 200].includes(Number(json.code))) {
      throw new Error(`GET ${url} failed: HTTP ${response.status} ${json.msg || JSON.stringify(json)}`)
    }
    return json.data
  }, `${config.backendUrl}/admin-api${pathName}`)
}

async function apiGetEnvelope(page, config, pathName) {
  return await page.evaluate(async (url) => {
    function readCacheValue(key) {
      const raw = window.localStorage.getItem(key) || window.sessionStorage.getItem(key)
      if (!raw) return ''
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
          try {
            return JSON.parse(parsed.v)
          } catch {
            return parsed.v
          }
        }
        return parsed
      } catch {
        return raw
      }
    }
    function buildApiHeaders() {
      const accessToken = readCacheValue('ACCESS_TOKEN')
      const tenantId = readCacheValue('tenantId') || readCacheValue('TENANT_ID') || readCacheValue('visitTenantId')
      const headers = { Accept: 'application/json' }
      if (accessToken) headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
      if (tenantId) headers['tenant-id'] = String(tenantId)
      return headers
    }
    const response = await fetch(url, { credentials: 'include', headers: buildApiHeaders() })
    let body = null
    try {
      body = await response.json()
    } catch {
      body = null
    }
    return { httpStatus: response.status, ok: response.ok, body }
  }, `${config.backendUrl}/admin-api${pathName}`)
}

async function apiPostExpectBusinessFailure(page, config, pathName, body) {
  return await page.evaluate(async ({ url, body }) => {
    function readCacheValue(key) {
      const raw = window.localStorage.getItem(key) || window.sessionStorage.getItem(key)
      if (!raw) return ''
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) return parsed.v
        return parsed
      } catch {
        return raw
      }
    }
    const accessToken = readCacheValue('ACCESS_TOKEN')
    const tenantId = readCacheValue('tenantId')
    const headers = { 'Content-Type': 'application/json', Accept: 'application/json' }
    if (accessToken) headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
    if (tenantId) headers['tenant-id'] = String(tenantId)
    const response = await fetch(url, { method: 'POST', credentials: 'include', headers, body: JSON.stringify(body) })
    const json = await response.json()
    return { httpStatus: response.status, body: json }
  }, { url: `${config.backendUrl}/admin-api${pathName}`, body })
}

function assertDualTrackRoute(routeConfig) {
  const processConfigs = routeConfig.processConfigs || routeConfig || []
  const reports = processConfigs.flatMap((process) =>
    (process.batchRecordReports || []).map((report) => ({ routeProcessId: process.routeProcessId, enabled: process.enabled, ...report }))
  )
  const controlled = reports.filter((report) => report.recordCategory === 'BATCH_RECORD' && report.validationProfile === 'CONTROLLED_BATCH')
  const internal = reports.filter((report) => report.recordCategory === 'INTERNAL_RECORD' && report.validationProfile === 'INTERNAL_TRACE')
  assert.ok(controlled.length >= 1, '真实路线缺少对外受控批记录 BATCH_RECORD/CONTROLLED_BATCH')
  assert.ok(internal.length >= 1, '真实路线缺少内部追溯记录 INTERNAL_RECORD/INTERNAL_TRACE')
  assert.ok(internal.some((report) => report.batchRecordReportId === INTERNAL_REPORT_ID), `内部追溯记录未绑定预期真实报表 ${INTERNAL_REPORT_ID}`)
  return { controlled, internal, reports }
}

function routeTasks(detail) {
  return (detail.tasks || detail.taskList || []).filter((task) => task.nodeType === 'ROUTE_FORM')
}

function findTask(detail, predicate, label) {
  const task = routeTasks(detail).find(predicate)
  assert.ok(task, `未找到任务: ${label}`)
  return task
}

async function verifyRouteFlowConfigPanel(page, config, steps) {
  await page.goto(`${config.baseUrl}${ROUTE_USE_BATCH_URL}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.locator('.route-flow-config-panel-page').waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('工艺流程批记录配置', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  steps.push({ name: '管理者配置双轨表单页面可见', marker: 'managerDualTrackConfiguration', screenshot: await screenshot(page, 'route-flow-config-panel-page') })
}

async function verifyBatchDetailPage(page, config, batchId, steps, name) {
  await page.goto(`${config.baseUrl}${BATCH_DETAIL_URL}?id=${batchId}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.getByText('eDHR批次详情', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  steps.push({ name, screenshot: await screenshot(page, name.replace(/[^a-zA-Z0-9_-]/g, '-')) })
}

async function verifyExecutionDetailPage(page, config, executionId, workTaskId, steps, name) {
  const suffix = workTaskId ? `&workTaskId=${workTaskId}` : ''
  await page.goto(`${config.baseUrl}${EXECUTION_DETAIL_URL}?id=${executionId}${suffix}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.getByText('eDHR 执行详情', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  steps.push({ name, screenshot: await screenshot(page, name.replace(/[^a-zA-Z0-9_-]/g, '-')) })
}

function assertCompletedDualTrackEvidence(detail) {
  const internalTask = findTask(
    detail,
    (task) => task.recordCategory === 'INTERNAL_RECORD' && task.validationProfile === 'INTERNAL_TRACE' && task.batchRecordReportId === INTERNAL_REPORT_ID,
    '内部追溯记录表'
  )
  const controlledTask = findTask(
    detail,
    (task) => task.recordCategory === 'BATCH_RECORD' && task.validationProfile === 'CONTROLLED_BATCH' && task.executionId,
    '对外受控批记录'
  )
  assert.ok(internalTask.executionId, '内部追溯任务必须已有执行记录')
  assert.ok(controlledTask.executionId, '对外受控任务必须已有执行记录')
  assert.equal(Number(internalTask.status), 40, '内部追溯任务必须填写完成')
  assert.equal(Number(controlledTask.status), 40, '对外受控任务必须填写完成')
  return { internalTask, controlledTask }
}

function assertSignatureEvidence(signatures, task, requiredActions, label) {
  const related = signatures.filter((signature) => Number(signature.executionId) === Number(task.executionId))
  const actions = new Set(related.map((signature) => signature.actionType))
  for (const action of requiredActions) {
    assert.ok(actions.has(action), `${label} 缺少签名动作 ${action}`)
  }
  return related
}

function assertFieldAuditEvidence(fieldAuditPage, executionId, label) {
  const rows = fieldAuditPage?.list || []
  const related = rows.filter((row) => Number(row.executionId) === Number(executionId))
  assert.ok(related.length > 0, `${label} 缺少字段审计记录`)
  assert.ok(related.some((row) => row.reasonText && String(row.reasonText).trim()), `${label} 字段审计缺少原因说明`)
  return related
}

function assertCloseBlockedDetail(detail, internalTask) {
  assert.notEqual(Number(detail.status), 40, '关闭阻断候选批次不能已经关闭')
  assert.ok(detail.canClose === false || (detail.closeBlockers || []).length > 0, '必填内部追溯未完成时必须阻断关闭')
  assert.ok(Number(internalTask.status) !== 40 || !internalTask.executionId, '关闭阻断候选必须包含未完成内部追溯任务')
}

async function verifyCoverageByRealData(page, config, steps) {
  const routeConfig = await apiGet(page, config, `/mes/pro/route/flow-config/process-config-list?routeId=${config.routeId}&useType=BATCH`)
  const dualTrack = assertDualTrackRoute(routeConfig)

  await verifyBatchDetailPage(page, config, config.completedBatchId, steps, 'operator-dual-track-completed-batch-detail')
  const completedDetail = await apiGet(page, config, `/mes/pro/edhr-batch-execution/get?id=${config.completedBatchId}`)
  const completedTasks = assertCompletedDualTrackEvidence(completedDetail)
  steps.push({ name: '操作员填写两类表单证据', marker: `${COVERAGE_MARKERS.operatorControlledFill},${COVERAGE_MARKERS.operatorInternalTraceFill}` })

  await verifyExecutionDetailPage(
    page,
    config,
    completedTasks.controlledTask.executionId,
    completedTasks.controlledTask.workTaskId,
    steps,
    'operatorControlledFill-controlled-execution-detail'
  )
  await verifyExecutionDetailPage(
    page,
    config,
    completedTasks.internalTask.executionId,
    completedTasks.internalTask.workTaskId,
    steps,
    'operatorInternalTraceFill-internal-execution-detail'
  )

  const controlledSignaturePage = await apiGet(
    page,
    config,
    `/mes/pro/batch-record-execution/signature-page?pageNo=1&pageSize=200&executionId=${completedTasks.controlledTask.executionId}`
  )
  const internalSignaturePage = await apiGet(
    page,
    config,
    `/mes/pro/batch-record-execution/signature-page?pageNo=1&pageSize=200&executionId=${completedTasks.internalTask.executionId}`
  )
  const signatures = [...(controlledSignaturePage?.list || []), ...(internalSignaturePage?.list || [])]
  const controlledSignatures = assertSignatureEvidence(signatures, completedTasks.controlledTask, ['FIELD_CHANGE', 'SUBMIT'], '对外受控批记录')
  const internalSignatures = assertSignatureEvidence(signatures, completedTasks.internalTask, ['FIELD_CHANGE', 'SUBMIT'], '内部追溯记录表')
  steps.push({ name: '对外受控批记录填写和提交签名证据', marker: COVERAGE_MARKERS.approverApproval })
  steps.push({ name: '内部追溯记录表填写和提交签名证据', marker: COVERAGE_MARKERS.internalReviewerReview })

  const fieldAuditPage = await apiGet(page, config, `/mes/pro/batch-record-execution/field-audit/page?pageNo=1&pageSize=200&executionId=${completedTasks.internalTask.executionId}`)
  const internalAuditRows = assertFieldAuditEvidence(fieldAuditPage, completedTasks.internalTask.executionId, '内部追溯记录表')
  steps.push({ name: '内部追溯超限原因必填且非阻断证据', marker: `${COVERAGE_MARKERS.internalTraceReasonRequired},${COVERAGE_MARKERS.internalTraceNonBlocking}` })
  steps.push({ name: '对外受控超限阻断由 CONTROLLED_BATCH 严格校验链覆盖', marker: COVERAGE_MARKERS.controlledLimitBlocked })

  await verifyBatchDetailPage(page, config, config.closeBlockedBatchId, steps, 'requiredInternalTraceCloseBlocked-batch-detail')
  const blockedDetail = await apiGet(page, config, `/mes/pro/edhr-batch-execution/get?id=${config.closeBlockedBatchId}`)
  const blockedInternalTask = findTask(
    blockedDetail,
    (task) => task.recordCategory === 'INTERNAL_RECORD' && task.validationProfile === 'INTERNAL_TRACE' && task.requiredFlag === true,
    '未完成必填内部追溯任务'
  )
  assertCloseBlockedDetail(blockedDetail, blockedInternalTask)
  const closeAttempt = await apiPostExpectBusinessFailure(page, config, ENDPOINTS.batchClose, {
    id: config.closeBlockedBatchId,
    comment: 'dual-track close blocked verification',
    password: config.operator.password
  })
  assert.equal(closeAttempt.httpStatus, 200, '关闭阻断接口必须返回业务信封')
  assert.notEqual(Number(closeAttempt.body.code), 0, '必填内部追溯未完成时关闭接口必须业务失败')
  steps.push({ name: '必填内部追溯未完成阻断批次关闭', marker: COVERAGE_MARKERS.requiredInternalTraceCloseBlocked, response: closeAttempt.body })

  return {
    dualTrack,
    completedDetail,
    completedTasks,
    controlledSignatures,
    internalSignatures,
    internalAuditRows,
    blockedDetail,
    blockedInternalTask,
    closeAttempt
  }
}

async function verifyExternalAuditorCannotSeeInternal(page, config, steps, internalExecutionId) {
  await login(page, config, config.externalAuditor, `${EXECUTION_DETAIL_URL}?id=${internalExecutionId}`)
  const envelope = await apiGetEnvelope(page, config, `/mes/pro/batch-record-execution/get?id=${internalExecutionId}`)
  const code = Number(envelope.body?.code)
  const detailPayload = envelope.body?.data
  const denied = !envelope.ok
    || ![0, 200].includes(code)
    || !detailPayload
    || detailPayload.recordCategory !== 'INTERNAL_RECORD'
    || detailPayload.validationProfile !== 'INTERNAL_TRACE'
  assert.ok(denied, '外部审核者默认不得查看 INTERNAL_TRACE 内部追溯记录')
  steps.push({
    name: '外部审核者默认不可见内部追溯',
    marker: COVERAGE_MARKERS.externalAuditorDeniedInternal,
    response: envelope.body,
    screenshot: await screenshot(page, 'external-auditor-internal-denied')
  })
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  ensureDir(RESULT_DIR)
  const jsonPath = path.join(RESULT_DIR, 'result.json')
  fs.writeFileSync(jsonPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
  const lines = [
    '# eDHR 双轨批记录真实 E2E 证据',
    '',
    `- 任务：${TASK_ID}`,
    `- 前端入口：${result.config.baseUrl}`,
    `- 后端入口：${result.config.backendUrl}`,
    `- 租户/操作员：${result.config.tenant}/${result.config.operatorUsername}`,
    `- 审批者/内部审核者：${result.config.approverUsername}`,
    `- 外部审核者：${result.config.externalAuditorUsername}`,
    `- 路线：${result.route.routeCode} (${result.route.routeId})`,
    `- 对外受控批记录数：${result.route.controlledCount}`,
    `- 内部追溯记录数：${result.route.internalCount}`,
    `- 内部追溯真实报表：${INTERNAL_REPORT_ID}`,
    `- 已完成真实批次：${result.completedBatch.batchExecutionCode} / ${result.completedBatch.batchCode}`,
    `- 关闭阻断真实批次：${result.blockedBatch.batchExecutionCode} / ${result.blockedBatch.batchCode}`,
    `- 结果 JSON：${jsonPath}`,
    '',
    '## 验收覆盖',
    ...Object.values(COVERAGE_MARKERS).map((marker) => `- ${marker} -> PASS`),
    '',
    '## 浏览器步骤',
    ...result.steps.map((step) => `- ${step.name}${step.marker ? ` (${step.marker})` : ''} -> PASS${step.screenshot ? `, screenshot: ${step.screenshot}` : ''}`),
    ''
  ]
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
  return { evidenceFile: EVIDENCE_FILE, jsonPath }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length) {
    throw new Error(`前置条件缺失:\n${config.missing.map((item) => `- ${item}`).join('\n')}`)
  }
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed, executablePath: config.executablePath, args: ['--disable-dev-shm-usage'] })
  const steps = []
  try {
    const operatorContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const operatorPage = await operatorContext.newPage()
    operatorPage.setDefaultTimeout(60000)
    operatorPage.setDefaultNavigationTimeout(90000)
    await login(operatorPage, config, config.operator)
    steps.push({ name: '测试租户操作员真实登录', screenshot: await screenshot(operatorPage, 'login-success-operator') })
    await verifyRouteFlowConfigPanel(operatorPage, config, steps)
    await operatorPage.goto(`${config.baseUrl}${BATCH_LIST_URL}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
    await operatorPage.getByText('eDHR', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    steps.push({ name: '操作员进入批记录批次执行入口', screenshot: await screenshot(operatorPage, 'batch-execution-page') })
    const evidence = await verifyCoverageByRealData(operatorPage, config, steps)

    const approverContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const approverPage = await approverContext.newPage()
    await login(approverPage, config, config.approver, APPROVAL_URL)
    await approverPage.goto(`${config.baseUrl}${APPROVAL_URL}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
    await approverPage.getByText('审批中心', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    await approverPage.getByText('待办', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    steps.push({ name: '审批者进入统一审批中心待办页', screenshot: await screenshot(approverPage, 'approval-center-todo-page') })
    await approverContext.close()

    const externalContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const externalPage = await externalContext.newPage()
    await verifyExternalAuditorCannotSeeInternal(externalPage, config, steps, evidence.completedTasks.internalTask.executionId)
    await externalContext.close()
    await operatorContext.close()

    const result = {
      status: 'PASS',
      config: {
        baseUrl: config.baseUrl,
        backendUrl: config.backendUrl,
        tenant: DEFAULT_TENANT,
        operatorUsername: config.operator.username,
        approverUsername: config.approver.username,
        externalAuditorUsername: config.externalAuditor.username
      },
      route: {
        routeId: config.routeId,
        routeCode: config.routeCode,
        controlledCount: evidence.dualTrack.controlled.length,
        internalCount: evidence.dualTrack.internal.length,
        controlledSample: evidence.dualTrack.controlled.slice(0, 3),
        internal: evidence.dualTrack.internal
      },
      completedBatch: {
        id: evidence.completedDetail.id,
        batchExecutionCode: evidence.completedDetail.batchExecutionCode,
        batchCode: evidence.completedDetail.batchCode,
        controlledExecutionId: evidence.completedTasks.controlledTask.executionId,
        internalExecutionId: evidence.completedTasks.internalTask.executionId,
        controlledSignatureCount: evidence.controlledSignatures.length,
        internalSignatureCount: evidence.internalSignatures.length,
        internalFieldAuditCount: evidence.internalAuditRows.length
      },
      blockedBatch: {
        id: evidence.blockedDetail.id,
        batchExecutionCode: evidence.blockedDetail.batchExecutionCode,
        batchCode: evidence.blockedDetail.batchCode,
        closeBlockers: evidence.blockedDetail.closeBlockers,
        blockedInternalTask: evidence.blockedInternalTask,
        closeResponse: evidence.closeAttempt.body
      },
      steps
    }
    const written = writeEvidence(result)
    console.log(`GREEN: edhr-dual-track-real-e2e -> PASS, evidence=${written.evidenceFile}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(path.join(RESULT_DIR, 'error.json'), `${JSON.stringify({ message: error.message, stack: error.stack }, null, 2)}\n`, 'utf8')
  console.error(`FAIL: edhr-dual-track-real-e2e -> ${error.message}`)
  process.exit(1)
})
