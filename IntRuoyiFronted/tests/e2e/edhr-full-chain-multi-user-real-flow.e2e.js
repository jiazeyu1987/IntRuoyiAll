const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { chromium } = require('playwright')

const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const GOAL_FILE = path.join(WORKSPACE_ROOT, '实现目标', '批记录目标', '1.txt')
const BASE_URL = (process.env.EDHR_FULL_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_FULL_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const ADMIN_SINGLE_ACTOR = process.env.EDHR_FULL_E2E_ADMIN_SINGLE_ACTOR === '1'
const TEST_TENANT = process.env.EDHR_FULL_E2E_TEST_TENANT || (ADMIN_SINGLE_ACTOR ? '芋道源码' : '测试租户')
const BATCH_EXECUTION_ID = Number(process.env.EDHR_FULL_E2E_BATCH_EXECUTION_ID || 0)
const MIN_DISTINCT_ACTORS = Number(process.env.EDHR_FULL_E2E_MIN_DISTINCT_ACTORS || (ADMIN_SINGLE_ACTOR ? 1 : 4))
const HEADLESS = process.env.EDHR_FULL_E2E_HEADED !== '1'
const CREATE_BATCH = process.env.EDHR_FULL_E2E_CREATE_BATCH === '1'
const RUN_ID = process.env.EDHR_FULL_E2E_RUN_ID || String(Date.now())
const EVIDENCE_DIR = path.resolve(
  process.env.EDHR_FULL_E2E_EVIDENCE_DIR ||
    path.join(WORKSPACE_ROOT, 'doc/tasks/20260624-edhr-must-fix-resolution/artifacts', `full-chain-${RUN_ID}`)
)
const CREATE_WORK_ORDER_CODE = process.env.EDHR_FULL_E2E_WORK_ORDER_CODE || '881MO090863'
const CREATE_WORK_ORDER_ID = Number(process.env.EDHR_FULL_E2E_WORK_ORDER_ID || 0)
const EXPLICIT_CREATE_ROUTE_ID = Number(process.env.EDHR_FULL_E2E_ROUTE_ID || 0)
const CREATE_BATCH_CODE = process.env.EDHR_FULL_E2E_BATCH_CODE || `E2E-FULL-${RUN_ID}`
const FILL_PREFIX = process.env.EDHR_FULL_E2E_FILL_PREFIX || `E2E-FULL-${RUN_ID}`
const REJECT_FIRST_ROUTE_TASK = process.env.EDHR_FULL_E2E_REJECT_FIRST_ROUTE_TASK !== '0'
const REJECT_REASON_PREFIX = process.env.EDHR_FULL_E2E_REJECT_REASON_PREFIX || `E2E-REJECT-${RUN_ID}`
const STERILIZATION_BATCH_NO = process.env.EDHR_FULL_E2E_STERILIZATION_BATCH_NO || `STER-${RUN_ID}`
const OQC_CODE = process.env.EDHR_FULL_E2E_OQC_CODE || `OQC-FULL-${RUN_ID}`
const OQC_TEMPLATE_CODE = process.env.EDHR_FULL_E2E_OQC_TEMPLATE_CODE || 'EDHR-REHEARSAL2-OQC-T'
const OQC_INDICATOR_CODE = process.env.EDHR_FULL_E2E_OQC_INDICATOR_CODE || 'EDHR-REHEARSAL2-OQC-I'
const OQC_INDICATOR_NAME = process.env.EDHR_FULL_E2E_OQC_INDICATOR_NAME || 'eDHR第二次演练OQC外观确认'
const OQC_PRODUCT_ITEM_CODE = process.env.EDHR_FULL_E2E_OQC_PRODUCT_ITEM_CODE || 'YXN.037.011.1002'
const OQC_CLIENT_CODE = process.env.EDHR_FULL_E2E_OQC_CLIENT_CODE || 'EDHR-REHEARSAL2-CUSTOMER'
const OQC_INSPECTOR_USERNAME = process.env.EDHR_FULL_E2E_OQC_INSPECTOR_USERNAME || (ADMIN_SINGLE_ACTOR ? 'admin' : 'aoteman')
const OQC_RESULT_LABEL = process.env.EDHR_FULL_E2E_OQC_RESULT_LABEL || '校验通过'
const EXPECTED_GOAL_COUNT = 58
const CORE_REQUIREMENT_IDS = Array.from({ length: 54 }, (_, index) => index + 1)
const TAIL_FOUR_COMPANION_REQUIREMENTS = [55, 56, 57, 58]
const TAIL_FOUR_COMPANION_SCRIPT = path.join(__dirname, 'edhr-tail-four-goals-real-flow.e2e.js')

const ROUTES = {
  scheduleOrder: '/mes/pro/schedule-order',
  workTask: '/mes/pro/feedback/edhr-work-task',
  oqc: '/mes/qc/oqc',
  qcTemplate: '/mes/qc/template',
  batchList: '/mes/pro/feedback/edhr-batch-execution',
  batchDetail: '/mes/pro/feedback/edhr-batch-execution/detail',
  batchReview: '/mes/pro/feedback/edhr-batch-execution/review',
  batchRecordFormList: '/mes/pro/batch-record-form-list',
  executionDetail: '/mes/pro/feedback/edhr-execution/form',
  approval: '/approval-center/todo'
}

const ENDPOINTS = {
  workOrderPage: '/mes/pro/work-order/page',
  batchOpenOrCreate: '/mes/pro/edhr-batch-execution/open-or-create',
  batchRouteOptions: '/mes/pro/edhr-batch-execution/work-order-route-options',
  batchGet: '/mes/pro/edhr-batch-execution/get',
  batchTaskOpen: '/mes/pro/edhr-batch-execution/task/open',
  cellRules: '/mes/pro/batch-record-report/cell-rules',
  batchSync: '/mes/pro/edhr-batch-execution/sync-status',
  batchClose: '/mes/pro/edhr-batch-execution/close',
  batchArchiveGenerate: '/mes/pro/edhr-batch-execution-archive/generate',
  executionDetail: '/mes/pro/batch-record-execution/get',
  workTaskMyPage: '/mes/pro/edhr-work-task/my-page',
  fieldAuditSave: '/mes/pro/batch-record-execution/field-audit/save-changes',
  formReviewSign: '/mes/pro/batch-record-execution/cosign',
  executionSubmit: '/mes/pro/batch-record-execution/submit',
  approvalPending: '/mes/pro/batch-record-execution/approval-pending-page',
  approvalApprove: '/mes/pro/batch-record-execution/approve',
  approvalReject: '/mes/pro/batch-record-execution/reject',
  approvalCenterTasks: '/approval-center/tasks/page',
  approvalCenterReview: '/approval-center/tasks/review',
  flowInterventionTransfer: '/mes/pro/edhr-flow-intervention/transfer',
  domainTraceVerify: '/mes/pro/batch-record-execution/domain-trace/verify',
  oqcCreate: '/mes/qc/oqc/create',
  oqcUpdate: '/mes/qc/oqc/update',
  oqcFinish: '/mes/qc/oqc/finish',
  templateItemCreate: '/mes/qc/template/item/create',
  templateIndicatorCreate: '/mes/qc/template/indicator/create',
  indicatorResultDetail: '/mes/qc/indicator-result/get-detail',
  indicatorResultCreate: '/mes/qc/indicator-result/create',
  specialNodeSkip: '/mes/pro/edhr-batch-execution/task/special-node/skip',
  specialNodeComplete: '/mes/pro/edhr-batch-execution/task/special-node/complete'
}

const REQUIRED_SPECIAL_NODES = new Map([
  ['INCOMING_INSPECTION_REPORT', '来料检报告'],
  ['STERILIZATION_REPORT', '灭菌报告'],
  ['FINISHED_PRODUCT_INSPECTION_REPORT', '成品检报告'],
  ['FINISHED_PRODUCT_INSPECTION_RECORD', '成品检记录']
])
const ROUTE_FORM_NODE_TYPE = 'ROUTE_FORM'
const READY_TO_CLOSE_BATCH_STATUS = 20
const ACCEPTED_BATCH_STATUSES = new Set([30, 40])
const APPROVED_OR_SKIPPED_TASK_STATUSES = new Set([40, 45])
const WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

function ensureEvidenceDir() {
  fs.mkdirSync(EVIDENCE_DIR, { recursive: true })
}

function writeEvidenceJson(fileName, payload) {
  ensureEvidenceDir()
  fs.writeFileSync(path.join(EVIDENCE_DIR, fileName), `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

async function captureEvidence(page, stepId, meta = {}) {
  ensureEvidenceDir()
  const safeStepId = stepId.replace(/[^a-zA-Z0-9_-]/g, '-')
  const screenshotPath = path.join(EVIDENCE_DIR, `${safeStepId}.png`)
  await page.screenshot({ path: screenshotPath, fullPage: true })
  writeEvidenceJson(`${safeStepId}.json`, {
    stepId,
    url: page.url(),
    capturedAt: new Date().toISOString(),
    screenshot: screenshotPath,
    ...meta
  })
}

const CORE_REQUIREMENT_COVERAGE = [
  { name: 'template-import-and-cell-rules', requirements: [1, 2] },
  { name: 'batch-route-form-binding-and-serial-parallel', requirements: [3, 4, 5, 7, 39, 40, 41] },
  { name: 'special-node-flow-and-skip-evidence', requirements: [6, 8, 9, 10, 11, 12, 13, 14, 50] },
  { name: 'schedule-start-and-batch-task-generation', requirements: [15, 16, 17, 18] },
  { name: 'multi-user-candidate-signature-pool', requirements: [19, 20, 21, 22, 30] },
  { name: 'advance-prerequisite-gates-and-fill-permission', requirements: [23, 24, 25, 26] },
  { name: 'field-audit-attachment-and-electronic-signature', requirements: [27, 28, 29] },
  { name: 'review-approval-and-rework-loop', requirements: [31, 32, 33, 34, 35, 36, 37, 38] },
  { name: 'close-readonly-review-and-integrity-gate', requirements: [42, 43, 44, 45, 46, 47, 48] },
  { name: 'controlled-final-archive-download-and-print', requirements: [49, 51, 52, 53, 54] }
]

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function env(prefix, key) {
  return (process.env[`EDHR_FULL_E2E_${prefix}_${key}`] || '').trim()
}

function actor(prefix, label, defaults = {}) {
  const userId = Number(env(prefix, 'USER_ID') || (ADMIN_SINGLE_ACTOR ? 1 : defaults.userId) || 0)
  return {
    prefix,
    label,
    tenant: env(prefix, 'TENANT') || TEST_TENANT,
    username: env(prefix, 'USERNAME') || (ADMIN_SINGLE_ACTOR ? 'admin' : defaults.username) || '',
    password: env(prefix, 'PASSWORD') || defaults.password || '',
    signaturePassword: env(prefix, 'SIGNATURE_PASSWORD') || defaults.signaturePassword || '',
    userId: Number.isFinite(userId) ? userId : 0,
    displayNames: [
      env(prefix, 'DISPLAY_NAME'),
      ADMIN_SINGLE_ACTOR ? '瑛泰管理员' : defaults.displayName,
      defaults.nickname,
      ADMIN_SINGLE_ACTOR ? 'admin' : defaults.username,
      userId ? String(userId) : ''
    ].filter(Boolean)
  }
}

function collectConfig() {
  return {
    actors: [
      actor('OWNER', '批次负责人/关闭人', { username: 'aoteman', userId: 113, displayName: '芋道1' }),
      actor('FILLER_A', '填写人 A', { username: 'aoteman', userId: 113, displayName: '芋道1' }),
      actor('FILLER_B', '填写人 B', { username: 'edhrmatrixexecutor', userId: 910244, displayName: 'eDHR矩阵-执行人' }),
      actor('REVIEWER', '审核人', { username: 'edhrmatrixapprover', userId: 910245, displayName: 'eDHR矩阵-审批人' }),
      actor('APPROVER', '批准人', { username: 'edhrmatrixapprover', userId: 910245, displayName: 'eDHR矩阵-审批人' }),
      actor('ARCHIVER', '归档人', { username: 'edhrmatrixarchiver', userId: 910246, displayName: 'eDHR矩阵-归档员' })
    ]
  }
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', '整链路 E2E 必须固定使用本机前端 http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, '整链路 E2E 必须固定使用本机后端 48081')
  const expectedTenant = ADMIN_SINGLE_ACTOR ? '芋道源码' : '测试租户'
  assert.equal(TEST_TENANT, expectedTenant, ADMIN_SINGLE_ACTOR
    ? 'admin 单账号授权模式必须固定使用芋道源码租户'
    : '真实写入/验证租户必须是测试租户')
}

function readGoals() {
  if (ADMIN_SINGLE_ACTOR && !fs.existsSync(GOAL_FILE)) {
    return []
  }
  const raw = fs.readFileSync(GOAL_FILE, 'utf8').replace(/^\uFEFF/, '')
  const goals = raw.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  assert.equal(
    goals.length,
    EXPECTED_GOAL_COUNT,
    `1.txt 应包含 ${EXPECTED_GOAL_COUNT} 条目标，当前为 ${goals.length}；如目标已新增，请先更新主链路和伴随脚本覆盖矩阵。`
  )
  return goals
}

function assertCoreCoverageMatrix(goals) {
  if (ADMIN_SINGLE_ACTOR && goals.length === 0) return
  const covered = new Set()
  for (const group of CORE_REQUIREMENT_COVERAGE) {
    for (const id of group.requirements) {
      assert.ok(id >= 1 && id <= CORE_REQUIREMENT_IDS.length, `核心覆盖矩阵 ${group.name} 引用了非主链路目标 ${id}`)
      covered.add(id)
    }
  }
  const missing = []
  for (const id of CORE_REQUIREMENT_IDS) {
    if (!covered.has(id)) missing.push({ id, goal: goals[id - 1] })
  }
  assert.deepEqual(missing, [], `整链路 E2E 核心覆盖矩阵遗漏目标：${JSON.stringify(missing)}`)
}

function assertTailFourCompanionCoverage(goals) {
  if (ADMIN_SINGLE_ACTOR && goals.length === 0) return
  const missing = TAIL_FOUR_COMPANION_REQUIREMENTS.filter((id) => id > goals.length)
  assert.deepEqual(missing, [], `第 55-58 条伴随目标缺失：${JSON.stringify(missing)}`)
  assert.ok(
    fs.existsSync(TAIL_FOUR_COMPANION_SCRIPT),
    `第 55-58 条目标必须由伴随真实路径脚本覆盖：${TAIL_FOUR_COMPANION_SCRIPT}`
  )
}

function validateConfig(config) {
  const blockers = []
  const expectedTenant = ADMIN_SINGLE_ACTOR ? '芋道源码' : '测试租户'
  if (!CREATE_BATCH && (!Number.isFinite(BATCH_EXECUTION_ID) || BATCH_EXECUTION_ID <= 0)) {
    blockers.push(ADMIN_SINGLE_ACTOR
      ? '缺少 EDHR_FULL_E2E_BATCH_EXECUTION_ID，必须指向芋道源码/admin 授权范围内真实已完成或已关闭的 eDHR 批次执行。'
      : '缺少 EDHR_FULL_E2E_BATCH_EXECUTION_ID，必须指向测试租户中真实已完成或已关闭的 eDHR 批次执行。')
  }
  if (CREATE_BATCH) {
    if (!CREATE_WORK_ORDER_CODE.trim()) {
      blockers.push(ADMIN_SINGLE_ACTOR
        ? '创建模式缺少 EDHR_FULL_E2E_WORK_ORDER_CODE，必须指向芋道源码/admin 授权范围内真实生产工单。'
        : '创建模式缺少 EDHR_FULL_E2E_WORK_ORDER_CODE，必须指向测试租户真实生产工单。')
    }
    if (process.env.EDHR_FULL_E2E_ROUTE_ID && (!Number.isFinite(EXPLICIT_CREATE_ROUTE_ID) || EXPLICIT_CREATE_ROUTE_ID <= 0)) {
      blockers.push(ADMIN_SINGLE_ACTOR
        ? '创建模式 EDHR_FULL_E2E_ROUTE_ID 无效；若填写该参数，必须指向芋道源码/admin 授权范围内真实工艺流程批记录配置。'
        : '创建模式 EDHR_FULL_E2E_ROUTE_ID 无效；若填写该参数，必须指向测试租户真实工艺流程批记录配置。')
    }
  }
  for (const actorConfig of config.actors) {
    if (actorConfig.tenant !== expectedTenant) {
      blockers.push(`${actorConfig.label} ${actorConfig.prefix}_TENANT 必须是${expectedTenant}，当前：${actorConfig.tenant || '<empty>'}`)
    }
    if (!actorConfig.username) {
      blockers.push(`缺少 EDHR_FULL_E2E_${actorConfig.prefix}_USERNAME（${actorConfig.label}）。`)
    }
    if (!ADMIN_SINGLE_ACTOR && !actorConfig.password) {
      blockers.push(`缺少 EDHR_FULL_E2E_${actorConfig.prefix}_PASSWORD（${actorConfig.label}）。`)
    }
    if (!ADMIN_SINGLE_ACTOR && !actorConfig.signaturePassword) {
      blockers.push(`缺少 EDHR_FULL_E2E_${actorConfig.prefix}_SIGNATURE_PASSWORD（${actorConfig.label}），不能证明多用户电子签名链路。`)
    }
  }
  const distinctUsers = new Set(config.actors.map((item) => item.username).filter(Boolean))
  if (distinctUsers.size < MIN_DISTINCT_ACTORS) {
    blockers.push(ADMIN_SINGLE_ACTOR
      ? `admin 单账号授权模式至少需要 ${MIN_DISTINCT_ACTORS} 个真实用户，当前：${[...distinctUsers].join(', ') || '<none>'}`
      : `整链路多用户用例至少需要 ${MIN_DISTINCT_ACTORS} 个不同真实用户，当前：${[...distinctUsers].join(', ') || '<none>'}`)
  }
  if (blockers.length > 0) {
    throw blocked(ADMIN_SINGLE_ACTOR ? 'eDHR admin 单账号真实 E2E 前置条件未满足。' : 'eDHR 整链路多用户真实 E2E 前置条件未满足。', blockers)
  }
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

function responseMatches(response, endpoint, method) {
  return response.url().includes(`/admin-api${endpoint}`) && response.request().method() === method
}

async function parseBusinessResponse(response, label) {
  if (response.status() !== 200) {
    const rawBody = await response.text().catch((error) => `<读取响应体失败：${error.message}>`)
    assert.equal(response.status(), 200, `${label} HTTP 必须为 200：${response.url()} -> ${rawBody}`)
  }
  let body
  try {
    body = await response.json()
  } catch (error) {
    throw new Error(`${label} 读取响应体失败：${response.url()} -> ${error.message}`)
  }
  assert.equal(body.code, 0, `${label} 业务响应失败：${body.msg || body.message || body.code}`)
  return body.data
}

async function waitForApiResponse(page, endpoint, label, method = 'GET', predicate = () => true) {
  const matcher = (item) => responseMatches(item, endpoint, method) && predicate(item)
  const response = await page.waitForResponse(matcher, { timeout: 90000 })
  try {
    return await parseBusinessResponse(response, label)
  } catch (error) {
    throw new Error(`${label}: ${error.message}`)
  }
}
async function waitForTenantRecognition(page, tenantName, loginForm) {
  const selectedTenant = loginForm.locator('.el-select__placeholder span').filter({ hasText: tenantName }).first()
  await selectedTenant.waitFor({ state: 'visible', timeout: 60000 })
}

async function selectLoginTenantByUi(page, loginForm, tenantName) {
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  let lastError = null
  for (let attempt = 0; attempt < 4; attempt += 1) {
    try {
      await tenantInput.waitFor({ state: 'visible', timeout: 90000 })
      await tenantInput.click({ force: true })
      await tenantInput.fill(tenantName)
      const tenantOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: tenantName }).first()
      await tenantOption.waitFor({ state: 'visible', timeout: 90000 })
      try {
        await tenantOption.click({ timeout: 10000 })
      } catch (error) {
        lastError = error
        await page.keyboard.press('Enter')
      }
      await waitForTenantRecognition(page, tenantName, loginForm)
      return
    } catch (error) {
      lastError = error
      await page.waitForTimeout(500)
    }
  }
  throw lastError || new Error(`Unable to select tenant: ${tenantName}`)
}

async function clickVisibleButton(root, name, label) {
  const locator = root.getByRole('button', { name })
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const button = locator.nth(index)
    if ((await button.isVisible().catch(() => false)) && !(await button.isDisabled().catch(() => true))) {
      await button.scrollIntoViewIfNeeded().catch(() => undefined)
      try {
        await button.click({ timeout: 10000 })
      } catch (error) {
        await button.click({ timeout: 10000, force: true })
      }
      return
    }
  }
  throw blocked(`缺少可点击控件：${label}`, [`当前可见按钮：${JSON.stringify(await visibleButtonLabels(root.page ? root.page() : root))}`])
}

async function waitForVisibleEnabledButton(root, name, label, timeout = 60000) {
  const page = root.page ? root.page() : root
  const startedAt = Date.now()
  const locator = root.getByRole('button', { name })
  while (Date.now() - startedAt < timeout) {
    const count = await locator.count()
    for (let index = 0; index < count; index += 1) {
      const button = locator.nth(index)
      if ((await button.isVisible().catch(() => false)) && !(await button.isDisabled().catch(() => true))) {
        return button
      }
    }
    await page.waitForTimeout(250)
  }
  throw blocked(`缺少可点击控件：${label}`, [`当前可见按钮：${JSON.stringify(await visibleButtonLabels(page))}`])
}

async function gotoPath(page, pathSuffix) {
  await page.goto(`${BASE_URL}${pathSuffix}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
}

async function visibleButtonLabels(page) {
  return page.locator('button').evaluateAll((nodes) =>
    nodes
      .filter((node) => Boolean(node.offsetWidth || node.offsetHeight || node.getClientRects().length))
      .map((node) => node.innerText.trim())
      .filter(Boolean)
  )
}

async function waitForAnyVisible(locator, label, timeout = 60000) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeout) {
    const count = await locator.count()
    for (let index = 0; index < count; index += 1) {
      if (await locator.nth(index).isVisible()) return
    }
    await locator.page().waitForTimeout(250)
  }
  throw new Error(`${label} 未在 ${timeout}ms 内显示。`)
}

async function waitForCurrentUrl(page, predicate, label, timeout = 60000) {
  const deadline = Date.now() + timeout
  let lastUrl = page.url()
  while (Date.now() < deadline) {
    const currentUrl = new URL(page.url())
    lastUrl = currentUrl.toString()
    if (predicate(currentUrl)) {
      return currentUrl
    }
    await page.waitForTimeout(250)
  }
  throw new Error(`${label}: 等待 URL 匹配超时，当前 URL=${lastUrl}`)
}

async function login(page, actorConfig, redirectPath) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 90000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw blocked('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  const tenantRecognitionPromise = (async () => {
    if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
      await waitForTenantRecognition(page, actorConfig.tenant, loginForm)
    }
  })()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await selectLoginTenantByUi(page, loginForm, actorConfig.tenant)
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), actorConfig.tenant, '租户')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), actorConfig.username, '用户名')
  const passwordInput = loginForm.locator('input[placeholder="请输入密码"]').first()
  if (actorConfig.password) {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), actorConfig.password, '密码')
  } else if (ADMIN_SINGLE_ACTOR) {
    await passwordInput.waitFor({ state: 'visible', timeout: 30000 })
    actorConfig.password = await passwordInput.inputValue()
    if (!actorConfig.password) {
      throw blocked('admin 单账号授权模式登录页默认密码为空；脚本不会写入或记录明文密码。')
    }
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), actorConfig.password, '密码')
  }
  actorConfig.signaturePassword = actorConfig.signaturePassword || actorConfig.password
  await tenantRecognitionPromise
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await clickFirstEnabled(loginForm.locator('button.el-button--primary'), `${actorConfig.label} 登录`)
  let loginResponse
  try {
    loginResponse = await loginResponsePromise
  } catch (error) {
    await captureEvidence(page, `login-failed-${actorConfig.prefix}`, {
      actor: actorConfig.username,
      redirectPath
    })
    throw error
  }
  const loginBody = await loginResponse.json()
  assert.equal(loginResponse.status(), 200, `${actorConfig.label} 登录 HTTP 必须为 200`)
  assert.ok([0, 200].includes(Number(loginBody.code)), `${actorConfig.label} 登录业务响应失败：${loginBody.msg || loginBody.message || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 90000 })
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
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
          current = current.accessToken
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'v')) {
          current = current.v
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'value')) {
          current = current.value
          continue
        }
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

async function apiGet(page, auth, endpoint, params = {}) {
  const url = new URL(`${BACKEND_URL}/admin-api${endpoint}`)
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  const response = await page.request.get(url.toString(), {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      'tenant-id': String(auth.tenantId),
      ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
    }
  })
  assert.equal(response.status(), 200, `${endpoint} HTTP 必须为 200`)
  const body = await response.json()
  assert.equal(body.code, 0, `${endpoint} 业务响应失败：${body.msg || body.code}`)
  return body.data
}

async function apiPost(page, auth, endpoint, data = {}) {
  const response = await page.request.post(`${BACKEND_URL}/admin-api${endpoint}`, {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      'tenant-id': String(auth.tenantId),
      ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
    },
    data
  })
  assert.equal(response.status(), 200, `${endpoint} HTTP 必须为 200`)
  const body = await response.json()
  assert.equal(body.code, 0, `${endpoint} 业务响应失败：${body.msg || body.code}`)
  return body.data
}

async function assertCurrentActor(page, actorConfig, stepId) {
  const auth = await browserAuth(page)
  const permissionInfo = await apiGet(page, auth, '/system/auth/get-permission-info')
  const currentUserId = Number(permissionInfo?.user?.id || permissionInfo?.userId || 0)
  if (currentUserId !== Number(actorConfig.userId)) {
    await captureEvidence(page, `${stepId}-wrong-actor`, {
      expectedUserId: actorConfig.userId,
      expectedUsername: actorConfig.username,
      currentUserId,
      permissionInfoUser: permissionInfo?.user
    })
  }
  assert.equal(
    currentUserId,
    Number(actorConfig.userId),
    `${stepId} 必须由 ${actorConfig.username}(${actorConfig.userId}) 执行，当前登录用户为 ${currentUserId || '<unknown>'}`
  )
}

async function ensureRouteCloseRule(page, actorConfig, routeId) {
  const resolvedRouteId = Number(routeId)
  assert.ok(Number.isFinite(resolvedRouteId) && resolvedRouteId > 0, '路线关闭规则必须使用真实工单路线选项返回的 routeId。')
  const auth = await browserAuth(page)
  const rule = await apiPost(page, auth, '/mes/pro/edhr-work-task/route-close-rule', {
    routeId: resolvedRouteId,
    assigneeUserId: actorConfig.userId,
    dueMinutes: 240,
    enabled: true,
    remark: `E2E full-chain close owner ${RUN_ID}`
  })
  assert.equal(Number(rule.assigneeUserId), Number(actorConfig.userId), '路线关闭规则必须指向本轮关闭责任人。')
  assert.equal(rule.taskType, 'CLOSE', '路线关闭规则必须保存为 CLOSE 任务类型。')
  writeEvidenceJson('route-close-rule.json', {
    runId: RUN_ID,
    routeId: resolvedRouteId,
    closeOwner: {
      username: actorConfig.username,
      userId: actorConfig.userId
    },
    rule
  })
  return rule
}

async function openCreateBatchDialog(page) {
  await gotoPath(page, ROUTES.batchList)
  await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })
  await clickVisibleButton(page, '打开/创建', '打开/创建批次')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  return dialog
}

async function selectWorkOrderByKeyword(page, dialog, keyword) {
  const workOrderSelect = dialog.locator('.el-select input[role="combobox"]').first()
  await workOrderSelect.click()
  await workOrderSelect.fill(keyword)
  const options = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: keyword })
  const option = CREATE_WORK_ORDER_ID > 0
    ? options.filter({ hasText: `ID ${CREATE_WORK_ORDER_ID}` }).first()
    : options.first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
}

function chooseCreateRouteOption(routeOptions) {
  const options = (Array.isArray(routeOptions) ? routeOptions : [])
    .map((item) => ({ ...item, resolvedRouteId: Number(item.routeId || item.id || 0) }))
    .filter((item) => Number.isFinite(item.resolvedRouteId) && item.resolvedRouteId > 0)
  if (options.length === 0) {
    throw blocked('当前工单没有返回可用工艺路线选项。', [
      'workOrderCode=' + CREATE_WORK_ORDER_CODE,
      'routeOptions=' + JSON.stringify(routeOptions)
    ])
  }
  if (EXPLICIT_CREATE_ROUTE_ID > 0) {
    const explicit = options.find((item) => item.resolvedRouteId === EXPLICIT_CREATE_ROUTE_ID)
    if (!explicit) {
      throw blocked('EDHR_FULL_E2E_ROUTE_ID 未出现在当前工单真实路线选项中。', [
        'expectedRouteId=' + EXPLICIT_CREATE_ROUTE_ID,
        'availableRouteIds=' + options.map((item) => item.resolvedRouteId).join(',')
      ])
    }
    return explicit
  }
  const enabled = options.find((item) => item.batchRouteEnabled !== false)
  return enabled || options[0]
}

async function selectRouteById(page, dialog, routeId) {
  const routeItem = dialog.locator('.el-form-item').filter({ hasText: '工艺路线' }).first()
  const routeSelect = routeItem.locator('.el-select input[role="combobox"], .el-select__wrapper').first()
  const optionText = `ID ${routeId}`
  for (let attempt = 0; attempt < 6; attempt += 1) {
    const selectedText = (await routeItem.innerText().catch(() => '')).trim()
    if (selectedText.includes(optionText)) {
      return
    }
    await routeSelect.click({ force: true }).catch(() => undefined)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
    if ((await option.isVisible().catch(() => false))) {
      await option.click()
      return
    }
    await page.waitForTimeout(1000)
  }
  const visibleOptions = await page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').trim()).filter(Boolean))
    .catch(() => [])
  throw blocked(`打开/创建弹窗没有找到工艺路线 ${routeId}`, [
    `当前工艺路线区域：${(await routeItem.innerText().catch(() => '')).trim()}`,
    `当前可见路线选项：${JSON.stringify(visibleOptions)}`
  ])
}

async function createBatchByUi(page, closeOwner) {
  const dialog = await openCreateBatchDialog(page)
  const routeOptionsPromise = waitForApiResponse(page, ENDPOINTS.batchRouteOptions, '加载工单工艺路线选项', 'GET')
  await selectWorkOrderByKeyword(page, dialog, CREATE_WORK_ORDER_CODE)
  const routeOptions = await routeOptionsPromise
  const selectedRouteOption = chooseCreateRouteOption(routeOptions)
  const selectedRouteId = selectedRouteOption.resolvedRouteId
  await fillFirstVisible(dialog.locator('.el-form-item').filter({ hasText: '批次号' }).locator('input'), CREATE_BATCH_CODE, '批次号')
  await selectRouteById(page, dialog, selectedRouteId)
  await ensureRouteCloseRule(page, closeOwner, selectedRouteId)
  const responsePromise = waitForApiResponse(page, ENDPOINTS.batchOpenOrCreate, '打开或创建 eDHR 批次执行', 'POST')
  await clickVisibleButton(dialog, /^确\s*认$/, '确认打开或创建')
  const batch = await responsePromise
  assert.ok(batch?.id, '打开或创建批次后未返回有效批次 ID。')
  const batchExecutionId = Number(batch.id)
  assert.ok(Number.isFinite(batchExecutionId) && batchExecutionId > 0, `打开或创建批次返回了无效批次 ID：${batch.id}`)
  const detail = await loadBatchDetailByUi(page, batchExecutionId, '创建后批次详情')
  assert.equal(Number(detail.id), batchExecutionId, `创建后批次详情必须匹配返回 ID ${batchExecutionId}。`)
  assert.ok(detail.batchCode || detail.batchExecutionCode, '创建后批次详情缺少批次号或批次执行编码。')
  return {
    batchExecutionId,
    batchCode: detail.batchCode || batch.batchCode || CREATE_BATCH_CODE,
    routeId: selectedRouteId,
    routeOption: selectedRouteOption,
    batch: detail
  }
}

function isSameBatchDetailPage(page, batchId) {
  const current = new URL(page.url())
  return current.pathname === ROUTES.batchDetail && current.searchParams.get('id') === String(batchId)
}

async function loadBatchDetailByUi(page, batchId, label = '批次详情') {
  const detailSignalPromise = page.waitForResponse(
    (response) => responseMatches(response, ENDPOINTS.batchGet, 'GET') && response.url().includes(`id=${batchId}`),
    { timeout: 90000 }
  )
  if (isSameBatchDetailPage(page, batchId)) {
    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
  } else {
    await gotoPath(page, `${ROUTES.batchDetail}?id=${batchId}`)
  }
  const detailResponse = await detailSignalPromise
  assert.ok(detailResponse.ok(), `${label}: 详情接口 HTTP ${detailResponse.status()}`)
  await page.getByText('eDHR批次详情').first().waitFor({ state: 'visible', timeout: 60000 })
  const auth = await browserAuth(page)
  const detail = await apiGet(page, auth, ENDPOINTS.batchGet, { id: batchId })
  assert.equal(Number(detail.id), Number(batchId), `${label}: 只读详情接口必须返回当前批次 ${batchId}`)
  return detail
}

async function loadBatchDetailTaskByUi(page, batchId, taskId, label = '批次详情任务') {
  const detailSignalPromise = page.waitForResponse(
    (response) => responseMatches(response, ENDPOINTS.batchGet, 'GET') && response.url().includes(`id=${batchId}`),
    { timeout: 90000 }
  )
  await gotoPath(page, `${ROUTES.batchDetail}?id=${batchId}&batchTaskId=${taskId}`)
  const detailResponse = await detailSignalPromise
  assert.ok(detailResponse.ok(), `${label}: 详情接口 HTTP ${detailResponse.status()}`)
  await page.getByText('eDHR批次详情').first().waitFor({ state: 'visible', timeout: 60000 })
  const auth = await browserAuth(page)
  const detail = await apiGet(page, auth, ENDPOINTS.batchGet, { id: batchId })
  assert.equal(Number(detail.id), Number(batchId), `${label}: 只读详情接口必须返回当前批次 ${batchId}`)
  return detail
}

async function syncBatchByUi(page, batchId) {
  const syncButton = await waitForVisibleEnabledButton(page, '同步状态', '同步状态')
  const syncPromise = waitForApiResponse(
    page,
    ENDPOINTS.batchSync,
    '同步批次状态',
    'POST',
    (response) => response.url().includes(`id=${batchId}`)
  )
  await syncButton.click()
  return await syncPromise
}

function isRouteFormTask(task) {
  return task?.nodeType === ROUTE_FORM_NODE_TYPE
}

function isIncompleteRouteFormTask(task) {
  return isRouteFormTask(task) && Number(task.status) !== 40
}

function isActiveRouteFormTask(task) {
  return isRouteFormTask(task) && Number(task.activeWorkTaskId || 0) > 0 && Number(task.status) !== 40
}

function taskAllowsAction(task, action) {
  return Array.isArray(task?.allowedActions) && task.allowedActions.includes(action)
}

function shouldTakeOverRouteTask(task) {
  return isActiveRouteFormTask(task) && !taskAllowsAction(task, 'OPEN_FORM')
}

function isFormCenterRouteTask(task) {
  if (!isRouteFormTask(task)) return false
  if (Number(task.formCenterInstanceId || 0) > 0) return true
  if (Number(task.formTemplateId || 0) > 0) return true
  const slotType = String(task.formSlotType || '').trim()
  return Boolean(slotType && slotType !== 'MAIN')
}

function formCenterTaskSearchTokens(task) {
  const slotLabels = {
    LOSS_REPORT: '损耗',
    PROCESS_INSPECTION: '过程检验',
    PARAMETER_RECORD: '参数',
    REWORK_RECORD: '返工'
  }
  return [
    task.formTemplateName,
    task.formBindingName,
    task.formBindingKey,
    task.sharedFormKey,
    slotLabels[String(task.formSlotType || '').trim()],
    task.formSlotType,
    task.processName,
    task.processCode,
    task.id
  ]
    .filter((item) => item !== undefined && item !== null && String(item).trim())
    .map((item) => String(item).trim())
}

function taskSearchTokens(task) {
  return [
    task.batchRecordReportName,
    task.batchRecordReportCode,
    task.batchRecordReportId,
    task.processName,
    task.processCode,
    task.id
  ]
    .filter((item) => item !== undefined && item !== null && String(item).trim())
    .map((item) => String(item).trim())
}

async function visibleTableRowTexts(page) {
  return page.locator('.el-table__body-wrapper tbody tr, .el-table__row').evaluateAll((rows) =>
    rows
      .filter((row) => Boolean(row.offsetWidth || row.offsetHeight || row.getClientRects().length))
      .map((row) => (row.innerText || '').replace(/\s+/g, ' ').trim())
      .filter(Boolean)
  )
}


async function ensureBatchTaskCellRulesConfirmedByUi(page, batch) {
  const auth = await browserAuth(page)
  const reportTasks = []
  const seenReportIds = new Set()
  for (const task of batch?.tasks || []) {
    const reportId = String(task.batchRecordReportId || '').trim()
    if (!reportId || seenReportIds.has(reportId) || task.nodeType !== ROUTE_FORM_NODE_TYPE) continue
    seenReportIds.add(reportId)
    reportTasks.push(task)
  }
  if (reportTasks.length === 0) {
    throw blocked('创建后的批次没有可确认填写规则的普通工序报表任务。')
  }

  const confirmations = []
  for (const task of reportTasks) {
    const reportId = String(task.batchRecordReportId).trim()
    const before = await apiGet(page, auth, ENDPOINTS.cellRules, { reportId })
    const beforeUnreviewed = Number(before.unreviewedFillableCellCount || 0)
    if (beforeUnreviewed <= 0) {
      confirmations.push({
        reportId,
        reportName: task.batchRecordReportName,
        processName: task.processName,
        beforeUnreviewed,
        savedByUi: false,
        afterUnreviewed: 0,
        ruleCount: Array.isArray(before.rules) ? before.rules.length : 0
      })
      continue
    }

    const readPromise = waitForApiResponse(
      page,
      ENDPOINTS.cellRules,
      `打开填写规则弹窗 ${task.batchRecordReportName || reportId}`,
      'GET',
      (response) => response.url().includes(`reportId=${encodeURIComponent(reportId)}`)
    )
    const versionNoQuery = task.batchRecordVersionNo ? `&versionNo=${encodeURIComponent(task.batchRecordVersionNo)}` : ''
    await gotoPath(page, `${ROUTES.batchRecordFormList}?reportId=${encodeURIComponent(reportId)}&action=cellRules${versionNoQuery}`)
    const loaded = await readPromise
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '单元格规则' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 90000 })
    await dialog.locator('.batch-record-cell-rules-editor').first().waitFor({ state: 'visible', timeout: 90000 })
    const loadedUnreviewed = Number(loaded.unreviewedFillableCellCount || 0)
    assert.ok(loadedUnreviewed > 0, `填写规则弹窗必须暴露待确认数量，reportId=${reportId}`)

    const savePromise = waitForApiResponse(
      page,
      ENDPOINTS.cellRules,
      `保存填写规则 ${task.batchRecordReportName || reportId}`,
      'PUT'
    )
    await clickVisibleButton(dialog, '保存规则', `保存填写规则 ${task.batchRecordReportName || reportId}`)
    const saved = await savePromise
    const afterUnreviewed = Number(saved.unreviewedFillableCellCount || 0)
    assert.equal(afterUnreviewed, 0, `保存填写规则后仍存在未确认单元格，reportId=${reportId}`)
    await dialog.waitFor({ state: 'hidden', timeout: 90000 })
    confirmations.push({
      reportId,
      reportName: task.batchRecordReportName,
      processName: task.processName,
      beforeUnreviewed,
      loadedUnreviewed,
      savedByUi: true,
      afterUnreviewed,
      ruleCount: Array.isArray(saved.rules) ? saved.rules.length : 0,
      suggestionCount: Array.isArray(saved.suggestions) ? saved.suggestions.length : 0
    })
  }

  writeEvidenceJson('cell-rule-confirmation.json', {
    runId: RUN_ID,
    batchExecutionId: batch.id,
    batchCode: batch.batchCode || batch.batchExecutionCode,
    reportCount: confirmations.length,
    savedCount: confirmations.filter((item) => item.savedByUi).length,
    confirmations
  })
  return confirmations
}

async function openTaskByUi(page, task) {
  const tokens = taskSearchTokens(task)
  let row
  let matchedToken = ''
  const rows = page.locator('.el-table__body-wrapper tbody tr, .el-table__row')
  for (const token of tokens) {
    const candidate = rows.filter({ hasText: token }).first()
    if ((await candidate.count()) > 0 && (await candidate.isVisible().catch(() => false))) {
      row = candidate
      matchedToken = token
      break
    }
  }
  if (!row) {
    throw blocked(`批次详情页未找到待打开的工序任务行：${tokens.join(' / ')}`, [
      `当前可见任务行：${JSON.stringify(await visibleTableRowTexts(page))}`
    ])
  }
  const rowText = (await row.innerText()).replace(/\s+/g, ' ').trim()
  const openResponsePromise = waitForApiResponse(page, ENDPOINTS.batchTaskOpen, `打开工序任务 ${matchedToken}`, 'POST')
  await clickVisibleButton(row, /打开填写|打开返工/, `打开工序任务 ${matchedToken}：${rowText}`)
  const opened = await openResponsePromise
  assert.ok(opened?.executionId, `工序任务 ${matchedToken} 打开后未返回 executionId。`)
  await page.waitForURL((url) => url.pathname === ROUTES.executionDetail, { timeout: 60000 })
  return opened
}

async function clickWorkTaskBoardActionButton(page, row, name, label) {
  const target = await row.evaluate((element) => {
    const isVisible = (node) => Boolean(node.offsetWidth || node.offsetHeight || node.getClientRects().length)
    const body = element.closest('.el-table__body-wrapper') || element.closest('tbody')
    const rows = Array.from((body || document).querySelectorAll('tbody tr')).filter(isVisible)
    return {
      index: rows.indexOf(element),
      text: (element.innerText || '').replace(/\s+/g, ' ').trim()
    }
  })
  if (target.index < 0) {
    throw blocked(`无法解析工作任务目标行序号：${label}`, [`目标行：${target.text || '<empty>'}`])
  }

  const rowButton = row.getByRole('button', { name }).first()
  if ((await rowButton.count()) > 0 && (await rowButton.isVisible().catch(() => false)) && !(await rowButton.isDisabled().catch(() => true))) {
    await rowButton.click()
    return
  }

  const buttons = page.getByRole('button', { name })
  const count = await buttons.count()
  for (let index = 0; index < count; index += 1) {
    const button = buttons.nth(index)
    if (!(await button.isVisible().catch(() => false)) || (await button.isDisabled().catch(() => true))) continue
    const buttonRow = await button.evaluate((element) => {
      const isVisible = (node) => Boolean(node.offsetWidth || node.offsetHeight || node.getClientRects().length)
      const rowElement = element.closest('tr')
      const body = rowElement?.closest('.el-table__body-wrapper') || rowElement?.parentElement
      const rows = Array.from((body || document).querySelectorAll('tr')).filter(isVisible)
      return {
        index: rowElement ? rows.indexOf(rowElement) : -1,
        text: (rowElement?.innerText || '').replace(/\s+/g, ' ').trim()
      }
    }).catch(() => ({ index: -1, text: '' }))
    if (buttonRow.index === target.index) {
      await button.click()
      return
    }
  }
  throw blocked(`缺少可点击控件：${label}`, [
    `目标待办行：${target.text || '<empty>'}`,
    `当前可见按钮：${JSON.stringify(await visibleButtonLabels(page))}`
  ])
}

async function openFillTaskFromBoard(page, batchId, batchCode, task, options = {}) {
  const taskTypeOption = options.taskTypeOption || '填写'
  const rowTypeTexts = options.rowTypeTexts || [taskTypeOption]
  const actionLabel = options.actionLabel || taskTypeOption
  await gotoPath(page, ROUTES.workTask)
  await page.getByText('任务类型').first().waitFor({ state: 'visible', timeout: 60000 })
  const toolbar = page.locator('.edhr-work-task-page__toolbar').first()
  await toolbar.locator('.el-select').first().click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: taskTypeOption }).first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
  await fillFirstVisible(toolbar.locator('.el-form-item').filter({ hasText: '批次' }).locator('input'), batchCode, `${actionLabel}任务批次筛选`)
  const processInput = toolbar.locator('.el-form-item').filter({ hasText: '工序' }).locator('input').first()
  const processToken = task.processName || task.processCode
  if (processToken && (await processInput.count()) > 0) {
    await processInput.fill(String(processToken))
  }
  const queryPromise = waitForApiResponse(
    page,
    ENDPOINTS.workTaskMyPage,
    `查询${actionLabel}待办 ${batchCode}`,
    'GET',
    (response) => response.url().includes(`batchCode=${encodeURIComponent(batchCode)}`)
  )
  await clickVisibleButton(toolbar, /^查询$/, `查询${actionLabel}待办`)
  const queryResult = await queryPromise

  const rowTokens = [batchCode, task.processName, task.processCode].filter(Boolean).map((item) => String(item))
  const processTokens = rowTokens.slice(1)
  const rows = page.locator('.el-table__body-wrapper tbody tr')
  let row
  for (const typeText of rowTypeTexts) {
    const typedRows = rows.filter({ hasText: batchCode }).filter({ hasText: typeText })
    for (const token of processTokens) {
      const candidate = typedRows.filter({ hasText: token }).first()
      if ((await candidate.count()) > 0 && (await candidate.isVisible().catch(() => false))) {
        row = candidate
        break
      }
    }
    if (row) break
    if (processTokens.length === 0) {
      const candidate = typedRows.first()
      if ((await candidate.count()) > 0 && (await candidate.isVisible().catch(() => false))) {
        row = candidate
        break
      }
    }
  }
  if (!row) {
    await captureEvidence(page, `work-task-not-found-${actionLabel}-${batchCode}`, {
      batchId,
      batchCode,
      actionLabel,
      rowTokens,
      rowTypeTexts,
      workTaskQuery: queryResult
    })
    throw blocked(`工作任务看板未找到批次 ${batchCode} 的${actionLabel}待办：${rowTokens.join(' / ')}；任务类型：${rowTypeTexts.join(' / ')}`, [
      `当前可见任务行：${JSON.stringify(await visibleTableRowTexts(page))}`
    ])
  }

  const openResponsePromise = waitForApiResponse(page, ENDPOINTS.batchTaskOpen, `处理${actionLabel}待办 ${batchCode}`, 'POST')
  await clickWorkTaskBoardActionButton(page, row, '处理', `处理${actionLabel}待办 ${batchCode}`)
  const opened = await openResponsePromise
  const openedExecutionId = options.expectedExecutionId || opened?.executionId
  if (!openedExecutionId) {
    await captureEvidence(page, `work-task-open-without-execution-${actionLabel}-${batchCode}`, {
      batchId,
      batchCode,
      actionLabel,
      rowTokens,
      rowText: (await row.innerText()).replace(/\s+/g, ' ').trim(),
      opened
    })
    throw blocked(`${actionLabel}待办打开后必须返回 executionId。`, [
      `openTask 返回：${JSON.stringify(opened)}`
    ])
  }
  const url = await waitForCurrentUrl(
    page,
    (currentUrl) => {
      const hasWorkTaskId = Boolean(currentUrl.searchParams.get('workTaskId'))
      return currentUrl.pathname === ROUTES.executionDetail && hasWorkTaskId && Boolean(currentUrl.searchParams.get('id'))
    },
    `处理${actionLabel}待办后进入填写页`,
    60000
  )
  if (options.expectedWorkTaskId) {
    assert.equal(url.searchParams.get('workTaskId'), String(options.expectedWorkTaskId), `${actionLabel}待办 workTaskId 必须匹配返工任务 ${options.expectedWorkTaskId}`)
  }
  assert.equal(url.searchParams.get('id'), String(openedExecutionId), `${actionLabel}待办必须进入执行 ${openedExecutionId}`)
  await page.locator('.edhr-fill-workspace').first().waitFor({ state: 'visible', timeout: 60000 })
  url.openedTask = opened
  return url
}

async function selectBatchDetailRouteTask(page, task) {
  const processToken = task.processName || task.processCode || task.batchRecordReportName
  assert.ok(processToken, `批次详情接管任务 ${task.id} 缺少工序定位文本。`)
  const taskToken = task.batchRecordReportName || task.formTemplateName || task.processName || task.processCode
  const taskCard = page.locator('.edhr-batch-detail__rail-process-form-item').filter({ hasText: String(taskToken) }).first()
  if (await taskCard.waitFor({ state: 'visible', timeout: 3000 }).then(() => true, () => false)) {
    return taskCard
  }
  const processHead = page
    .locator('.edhr-batch-detail__process-task-group:not(.edhr-batch-detail__special-process-task-group) .edhr-batch-detail__process-task-group-head')
    .filter({ hasText: String(processToken) })
    .first()
  await processHead.waitFor({ state: 'visible', timeout: 60000 })
  await processHead.click({ timeout: 10000, force: true })
  await taskCard.waitFor({ state: 'visible', timeout: 60000 })
  return taskCard
}

async function openFillTaskFromBatchDetailTakeover(page, batchId, batchCode, task) {
  await loadBatchDetailTaskByUi(page, batchId, task.id, `管理员接管前批次详情 ${task.id}`)
  const taskCard = await selectBatchDetailRouteTask(page, task)
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const batchRecordToggle = page.getByRole('button', { name: '批记录' }).first()
    if ((await batchRecordToggle.count()) === 0 || !(await batchRecordToggle.isVisible().catch(() => false))) break
    if (await batchRecordToggle.isDisabled().catch(() => true)) break
    if ((await batchRecordToggle.getAttribute('aria-pressed').catch(() => null)) === 'true') break
    try {
      await batchRecordToggle.click({ timeout: 5000, force: true })
      break
    } catch (error) {
      await page.waitForTimeout(500)
    }
  }
  await selectBatchDetailRouteTask(page, task)
  const taskToken = task.batchRecordReportName || task.formTemplateName || task.processName || task.processCode
  const takeoverButtonLocator = () =>
    page
      .locator('.edhr-batch-detail__rail-process-form-item')
      .filter({ hasText: String(taskToken) })
      .getByRole('button', { name: '管理员接管并填写' })
      .first()
  let takeoverClicked = false
  let takeoverClickError
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const takeoverButton = takeoverButtonLocator()
    const visible = await takeoverButton.waitFor({ state: 'visible', timeout: 5000 }).then(() => true, (error) => {
      takeoverClickError = error
      return false
    })
    if (!visible) {
      await page.waitForTimeout(500)
      continue
    }
    try {
      await takeoverButton.click({ timeout: 5000, force: true })
      takeoverClicked = true
      break
    } catch (error) {
      takeoverClickError = error
      await page.waitForTimeout(500)
    }
  }
  if (!takeoverClicked) {
    await captureEvidence(page, `takeover-button-missing-${batchCode}-${task.id}`, {
      batchId,
      batchCode,
      taskId: task.id,
      taskName: task.batchRecordReportName || task.processName,
      allowedActions: task.allowedActions,
      currentUserRole: task.currentUserRole,
      disabledReason: task.disabledReason,
      visibleButtons: await visibleButtonLabels(page),
      clickError: takeoverClickError?.message
    })
    throw blocked(`批次详情未能点击管理员接管并填写按钮：${batchCode} / ${task.processName || task.batchRecordReportName}`, [
      `任务 ${task.id} currentUserRole=${task.currentUserRole || '<empty>'}, allowedActions=${JSON.stringify(task.allowedActions || [])}, disabledReason=${task.disabledReason || '<empty>'}`,
      `点击错误：${takeoverClickError?.message || '<empty>'}`
    ])
  }
  const confirm = page.locator('.el-message-box:visible').first()
  await confirm.waitFor({ state: 'visible', timeout: 60000 })
  const transferPromise = waitForApiResponse(page, ENDPOINTS.flowInterventionTransfer, `管理员接管填写任务 ${batchCode}`, 'POST').then(
    (data) => ({ data }),
    (error) => ({ error })
  )
  const openResponsePromise = waitForApiResponse(page, ENDPOINTS.batchTaskOpen, `接管后打开填写任务 ${batchCode}`, 'POST').then(
    (data) => ({ data }),
    (error) => ({ error })
  )
  await clickVisibleButton(confirm, /确\s*认|确\s*定|确认/, '确认管理员接管并填写')
  const transferResult = await transferPromise
  if (transferResult.error) throw transferResult.error
  const openResult = await openResponsePromise
  if (openResult.error) throw openResult.error
  const opened = openResult.data
  assert.ok(opened?.executionId, `管理员接管后打开任务必须返回 executionId，任务 ${task.id}`)
  const url = await waitForCurrentUrl(
    page,
    (currentUrl) => {
      const hasWorkTaskId = Boolean(currentUrl.searchParams.get('workTaskId'))
      return currentUrl.pathname === ROUTES.executionDetail && hasWorkTaskId && Boolean(currentUrl.searchParams.get('id'))
    },
    `管理员接管后进入填写页 ${batchCode}`,
    60000
  )
  assert.equal(url.searchParams.get('id'), String(opened.executionId), `管理员接管后必须进入执行 ${opened.executionId}`)
  await page.locator('.edhr-fill-workspace').first().waitFor({ state: 'visible', timeout: 60000 })
  url.openedTask = opened
  url.takeoverApplied = true
  return url
}

async function fillEditableControls(page, valuePrefix, taskIndex) {
  const form = page.locator('.edhr-page-shell__form').first()
  if ((await form.count()) === 0 || !(await form.isVisible())) {
    return { filled: 0, selected: 0 }
  }

  let filled = 0
  let selected = 0
  const formItems = form.locator('.el-form-item')
  const count = await formItems.count()
  for (let index = 0; index < count; index += 1) {
    const item = formItems.nth(index)
    if (!(await item.isVisible().catch(() => false))) continue
    const itemDisabled = await item
      .evaluate((element) => element.closest('.is-disabled') != null || element.querySelector('.is-disabled') != null)
      .catch(() => true)
    if (itemDisabled) continue

    const select = item.locator('.el-select input[role="combobox"]').first()
    if ((await select.count()) > 0 && (await select.isVisible().catch(() => false)) && (await select.isEnabled().catch(() => false))) {
      await select.click()
      await page.keyboard.press('ArrowDown')
      await page.keyboard.press('Enter')
      selected += 1
      continue
    }

    const checkbox = item.locator('.el-checkbox:not(.is-disabled)').first()
    if ((await checkbox.count()) > 0 && (await checkbox.isVisible().catch(() => false))) {
      await checkbox.click()
      selected += 1
      continue
    }

    const numberInput = item.locator('.el-input-number input').first()
    if ((await numberInput.count()) > 0 && (await numberInput.isVisible().catch(() => false)) && (await numberInput.isEnabled().catch(() => false))) {
      await numberInput.fill(String(10 + taskIndex))
      await numberInput.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const dateInput = item.locator('.el-date-editor input').first()
    if ((await dateInput.count()) > 0 && (await dateInput.isVisible().catch(() => false)) && (await dateInput.isEnabled().catch(() => false))) {
      const isDateTime = await dateInput
        .evaluate((element) => element.closest('.el-date-editor')?.className.includes('datetime') === true)
        .catch(() => false)
      await dateInput.fill(isDateTime ? '2026-06-14 10:20:30' : '2026-06-14')
      await dateInput.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const textarea = item.locator('textarea').first()
    if ((await textarea.count()) > 0 && (await textarea.isVisible().catch(() => false)) && (await textarea.isEnabled().catch(() => false))) {
      await textarea.fill(`${valuePrefix}-${filled + 1}`)
      await textarea.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const input = item.locator('input:not([type="hidden"]):not([type="password"]):not([type="checkbox"])').first()
    if ((await input.count()) === 0 || !(await input.isVisible().catch(() => false)) || !(await input.isEnabled().catch(() => false))) continue
    const readonly = await input.evaluate((element) => element.hasAttribute('readonly')).catch(() => true)
    if (readonly) continue
    await input.fill(`${valuePrefix}-${filled + 1}`)
    await input.press('Tab').catch(() => undefined)
    filled += 1
  }
  return { filled, selected }
}

async function saveFieldAuditIfNeeded(page, signaturePassword, taskIndex) {
  const pendingRows = page.locator('.edhr-page-shell__field-audit-table .el-table__row')
  const pendingCount = await pendingRows.count()
  if (pendingCount === 0) {
    return { saved: false, pendingCount }
  }

  const reasonArea = page.locator('.edhr-page-shell__field-audit-reason').first()
  const reasonSelects = reasonArea.locator('.el-select input[role="combobox"], .el-select__wrapper')
  for (let index = 0; index < await reasonSelects.count(); index += 1) {
    const reasonSelect = reasonSelects.nth(index)
    if (!(await reasonSelect.isVisible().catch(() => false))) continue
    await reasonSelect.click({ force: true })
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: /操作录入|纠正录入/ }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    break
  }
  const reasonInput = reasonArea.locator('.el-form-item').last().locator('input:not([type="hidden"])').first()
  if ((await reasonInput.count()) > 0 && (await reasonInput.isVisible())) {
    await reasonInput.fill(`${FILL_PREFIX}-FIELD_CHANGE_REASON-${taskIndex}`)
  }
  const saveButton = page.locator('.edhr-page-shell__field-audit').getByRole('button', { name: /保存变更/ }).first()
  await saveButton.waitFor({ state: 'visible', timeout: 60000 })
  const saveResponsePromise = waitForApiResponse(page, ENDPOINTS.fieldAuditSave, '字段审计保存', 'PUT')
  await saveButton.click()
  const result = await saveResponsePromise
  assert.equal(result.hashVerification?.status, 'VALID', '字段审计链校验必须为 VALID。')
  await waitForVisibleEnabledButton(page, '复核签名', `复核签名 T${taskIndex}`)
  return { saved: true, pendingCount, result }
}

async function formReviewSign(page, signaturePassword, taskIndex) {
  const reviewButton = await waitForVisibleEnabledButton(page, '复核签名', `复核签名 T${taskIndex}`)
  await reviewButton.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '表单复核签名' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), signaturePassword, '复核签名密码')
  const commentInput = dialog.locator('textarea').first()
  if ((await commentInput.count()) > 0 && (await commentInput.isVisible())) {
    await commentInput.fill(`${FILL_PREFIX}-FORM_REVIEW-${taskIndex}`)
  }
  const signResponsePromise = waitForApiResponse(page, ENDPOINTS.formReviewSign, `表单复核签名 T${taskIndex}`, 'PUT')
  await clickVisibleButton(dialog, /确\s*认\s*签\s*名/, '确认复核签名')
  return await signResponsePromise
}

async function openSubmitDialog(page) {
  await clickVisibleButton(page, '提交执行', '提交执行')
  const dialog = page.locator('.edhr-fill-workspace__submit-sign-dialog.el-dialog:visible').first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.locator('input[type="password"]').first().waitFor({ state: 'visible', timeout: 60000 })
  return dialog
}

async function chooseReviewAssignees(page, dialog, actorConfig) {
  const selects = dialog.locator('.edhr-page-shell__submit-select')
  const selectionMap = {}
  const count = await selects.count()
  if (count === 0 || !actorConfig) {
    return selectionMap
  }
  for (let index = 0; index < count; index += 1) {
    const select = selects.nth(index)
    await select.click()
    const options = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
    await options.first().waitFor({ state: 'visible', timeout: 60000 })
    const optionCount = await options.count()
    let chosenIndex = -1
    for (let optionIndex = 0; optionIndex < optionCount; optionIndex += 1) {
      const optionText = (await options.nth(optionIndex).innerText().catch(() => '')).trim()
      if (
        optionText.includes(actorConfig.username) ||
        actorConfig.displayNames.some((item) => item && optionText.includes(item)) ||
        (actorConfig.userId && optionText.includes(String(actorConfig.userId)))
      ) {
        chosenIndex = optionIndex
        break
      }
    }
    if (chosenIndex < 0) {
      throw blocked(`提交弹窗没有找到 ${actorConfig.label} 对应的历史审批候选人`, [
        `可见候选：${JSON.stringify(await options.evaluateAll((nodes) => nodes.map((node) => (node.innerText || '').trim()).filter(Boolean)))}`
      ])
    }
    const selectedText = (await options.nth(chosenIndex).innerText()).trim()
    await options.nth(chosenIndex).click()
    selectionMap[index] = selectedText
  }
  return selectionMap
}

async function submitExecution(page, signaturePassword, actorConfig, taskIndex) {
  const dialog = await openSubmitDialog(page)
  await fillFirstVisible(dialog.locator('input[type="password"]'), signaturePassword, '提交密码')
  const commentInput = dialog.locator('textarea').first()
  if ((await commentInput.count()) > 0 && (await commentInput.isVisible())) {
    await commentInput.fill(`${FILL_PREFIX}-SUBMIT-${taskIndex}`)
  }
  const selections = await chooseReviewAssignees(page, dialog, actorConfig)
  const responsePromise = waitForApiResponse(page, ENDPOINTS.executionSubmit, `提交执行 T${taskIndex}`, 'PUT')
  await clickVisibleButton(dialog, /确\s*认(?:\s*提\s*交)?/, '确认提交执行')
  let result
  try {
    result = await responsePromise
  } catch (error) {
    const visibleErrors = await page
      .locator('.el-message, .el-form-item__error, .el-alert')
      .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').trim()).filter(Boolean))
      .catch(() => [])
    await captureEvidence(page, `submit-execution-failed-T${taskIndex}`, {
      taskIndex,
      selections,
      visibleErrors
    })
    throw error
  }
  return { result, selections }
}

async function approveExecution(page, actorConfig, executionCode, taskIndex) {
  await gotoPath(page, ROUTES.approval)
  await page.getByText('待我审批').first().waitFor({ state: 'visible', timeout: 60000 })
  const toolbar = page.locator('.edhr-workbench__toolbar').first()
  await fillFirstVisible(toolbar.locator('input').first(), executionCode, '审批执行编号')
  const pendingResponsePromise = waitForApiResponse(page, ENDPOINTS.approvalPending, `待审批查询 ${executionCode}`, 'GET')
  await clickVisibleButton(toolbar, '查询', '审批查询')
  const pageData = await pendingResponsePromise
  const rows = pageData.list || []
  assert.ok(rows.some((item) => item.executionCode === executionCode), `待我审批未查询到 ${executionCode}。`)
  const row = page.locator('.el-table__row').filter({ hasText: executionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickVisibleButton(row, '通过', `审批通过 ${executionCode}`)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '通过 eDHR 审批' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), actorConfig.signaturePassword, '审批密码')
  await fillLabeledTextarea(dialog, '审批意见', `${FILL_PREFIX}-APPROVE-${taskIndex}`)
  const approveResponsePromise = waitForApiResponse(page, ENDPOINTS.approvalApprove, `审批通过 ${executionCode}`, 'PUT')
  await clickVisibleButton(dialog, /^确\s*认$/, '确认审批通过')
  const result = await approveResponsePromise
  assert.ok([3, 4, 40].includes(Number(result.status)), `审批通过后状态必须是关闭或待关闭态，实际 ${result.status}`)
  return result
}

async function rejectExecution(page, actorConfig, executionCode, taskIndex, originalExecutionId) {
  await gotoPath(page, ROUTES.approval)
  await page.getByText('待我审批').first().waitFor({ state: 'visible', timeout: 60000 })
  const toolbar = page.locator('.edhr-workbench__toolbar').first()
  await fillFirstVisible(toolbar.locator('input').first(), executionCode, '审批执行编号')
  const pendingResponsePromise = waitForApiResponse(page, ENDPOINTS.approvalPending, `待驳回查询 ${executionCode}`, 'GET')
  await clickVisibleButton(toolbar, '查询', '审批查询')
  const pageData = await pendingResponsePromise
  const rows = pageData.list || []
  assert.ok(rows.some((item) => item.executionCode === executionCode), `待我审批未查询到待驳回执行 ${executionCode}。`)
  const row = page.locator('.el-table__row').filter({ hasText: executionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickVisibleButton(row, '驳回', `审批驳回 ${executionCode}`)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '驳回 eDHR 审批' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  const reason = `${REJECT_REASON_PREFIX}-T${taskIndex}`
  await fillFirstVisible(dialog.locator('.el-form-item').filter({ hasText: '当前密码' }).locator('input[type="password"]'), actorConfig.signaturePassword, '驳回密码')
  await fillLabeledTextarea(dialog, '驳回原因', reason)
  await fillLabeledTextarea(dialog, '审批意见', `${FILL_PREFIX}-REJECT-COMMENT-${taskIndex}`)
  const rejectResponsePromise = waitForApiResponse(page, ENDPOINTS.approvalReject, `审批驳回 ${executionCode}`, 'PUT')
  await clickVisibleButton(dialog, /^确\s*认$/, '确认审批驳回')
  const result = await rejectResponsePromise
  assert.equal(Number(result.executionId), Number(originalExecutionId), `驳回响应 executionId 必须是原执行 ${originalExecutionId}`)
  assert.equal(Number(result.status), 2, `审批驳回后状态必须是 REJECTED(2)，实际 ${result.status}`)
  assert.ok(result.rejectedAt, '审批驳回后必须返回 rejectedAt。')
  assert.ok(Number(result.revisionExecutionId || 0) > 0, '审批驳回后必须返回 revisionExecutionId。')
  assert.ok(Number(result.reworkTaskId || 0) > 0, '审批驳回后必须返回 reworkTaskId。')
  assert.ok(result.signatureId, '审批驳回后必须记录驳回签名。')
  return { ...result, reason }
}

async function assertRejectedExecutionReadonly(page, executionId) {
  const detail = await loadExecutionDetail(page, executionId, { preserveWorkTask: false })
  assert.equal(Number(detail.status), 2, `被驳回原执行 ${executionId} 必须是 REJECTED(2)，实际 ${detail.status}`)
  await page.getByText('已驳回（不可归档）').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('被驳回原版本已锁定', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const enabledSubmitButtons = await page
    .getByRole('button', { name: '提交执行' })
    .evaluateAll((nodes) =>
      nodes.filter(
        (node) =>
          Boolean(node.offsetWidth || node.offsetHeight || node.getClientRects().length) &&
          !node.disabled &&
          !node.classList.contains('is-disabled') &&
          node.getAttribute('aria-disabled') !== 'true'
      ).length
    )
  assert.equal(enabledSubmitButtons, 0, `被驳回原执行 ${executionId} 不允许继续提交。`)
  return detail
}

async function specialNodeAction(page, label, actionName, handler) {
  const node = page
    .locator('.edhr-batch-detail__special-process-task-group .edhr-batch-detail__process-task-group-head')
    .filter({ hasText: label })
    .first()
  await node.waitFor({ state: 'visible', timeout: 60000 })
  await page
    .locator('.edhr-batch-detail__process-task-group.is-active, .edhr-batch-detail__release-process-item.is-active')
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
  const actionLabel = actionName === '完成' ? '完成节点' : actionName === '跳过' ? '跳过节点' : actionName
  const actionGrid = page.locator('.edhr-batch-detail__special-node-action-grid:visible').first()
  const targetActiveGroup = page.locator('.edhr-batch-detail__process-task-group.is-active').filter({ hasText: label }).first()
  let actionButton
  for (let attempt = 0; attempt < 6; attempt += 1) {
    await node.scrollIntoViewIfNeeded().catch(() => undefined)
    await node.click({ timeout: 10000 })
    const targetActive = await targetActiveGroup.waitFor({ state: 'visible', timeout: 2000 }).then(() => true, () => false)
    if (!targetActive) {
      await page.waitForTimeout(500)
      continue
    }
    const actionGridVisible = await actionGrid.waitFor({ state: 'visible', timeout: 2000 }).then(() => true, () => false)
    if (!actionGridVisible) {
      await page.waitForTimeout(500)
      continue
    }
    const candidate = actionGrid.locator('.edhr-batch-detail__rail-task-action:visible').filter({ hasText: actionLabel }).first()
    const candidateVisible = await candidate.waitFor({ state: 'visible', timeout: 2000 }).then(() => true, () => false)
    if (!candidateVisible) {
      await page.waitForTimeout(500)
      continue
    }
    await page.waitForTimeout(300)
    if (await targetActiveGroup.isVisible().catch(() => false)) {
      actionButton = candidate
      break
    }
  }
  if (!actionButton) {
    const visibleSpecialNodes = await page
      .locator('.edhr-batch-detail__special-process-task-group')
      .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').replace(/\s+/g, ' ').trim()).filter(Boolean))
      .catch(() => [])
    const activeGroups = await page
      .locator('.edhr-batch-detail__process-task-group.is-active')
      .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').replace(/\s+/g, ' ').trim()).filter(Boolean))
      .catch(() => [])
    const actionTexts = await page
      .locator('.edhr-batch-detail__special-node-action-grid:visible .edhr-batch-detail__rail-task-action:visible')
      .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').replace(/\s+/g, ' ').trim()).filter(Boolean))
      .catch(() => [])
    throw blocked('特殊节点「' + label + '」未能打开「' + actionLabel + '」操作。', [
      '当前可见特殊节点：' + JSON.stringify(visibleSpecialNodes),
      '当前激活节点：' + JSON.stringify(activeGroups),
      '当前可见特殊节点按钮：' + JSON.stringify(actionTexts)
    ])
  }
  const rowText = (
    (await page
      .locator('.edhr-batch-detail__rail-task-detail')
      .first()
      .textContent({ timeout: 1000 })
      .catch(() => '')) || ''
  )
    .replace(/\s+/g, ' ')
    .trim()
  if (rowText.includes('已跳过') || rowText.includes('已批准')) {
    return { alreadyDone: true, rowText }
  }
  let buttonReady = false
  let disabled = true
  let ariaDisabled = null
  let className = ''
  for (let attempt = 0; attempt < 20; attempt += 1) {
    disabled = await actionButton.isDisabled().catch(() => true)
    ariaDisabled = await actionButton.getAttribute('aria-disabled').catch(() => null)
    className = await actionButton.getAttribute('class').catch(() => '')
    if (!disabled && ariaDisabled !== 'true' && !String(className || '').includes('is-disabled')) {
      buttonReady = true
      break
    }
    await page.waitForTimeout(500)
  }
  if (!buttonReady) {
    throw blocked('特殊节点「' + label + '」的「' + actionLabel + '」按钮不可用。', [
      '当前明细：' + rowText,
      '通常表示该路线缺少对应责任规则、当前登录人不是责任人，或上游必需报告未完成。'
    ])
  }
  const endpoint = actionName === '跳过' ? ENDPOINTS.specialNodeSkip : ENDPOINTS.specialNodeComplete
  const responsePromise = page
    .waitForResponse((response) => responseMatches(response, endpoint, 'POST'), { timeout: 60000 })
    .then(
      (response) => ({ response }),
      (error) => ({ error })
    )
  await actionButton.click({ timeout: 10000 })
  await handler()
  const responseResult = await responsePromise
  if (responseResult.error) {
    throw responseResult.error
  }
  const response = responseResult.response
  const result = await parseBusinessResponse(response, label + ' ' + actionName)
  return { alreadyDone: false, result }
}

async function skipSpecialNode(page, label, signaturePassword) {
  return await specialNodeAction(page, label, '跳过', async () => {
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '跳过特殊节点' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    await fillLabeledTextarea(dialog, '跳过原因', `${FILL_PREFIX}-SKIP-${label}`)
    await fillFirstVisible(dialog.locator('input[type="password"]'), signaturePassword, `${label} 跳过签名密码`)
    await clickVisibleButton(dialog, /签\s*名\s*并\s*跳\s*过/, `${label} 签名并跳过`)
  })
}

async function completeSpecialNode(page, label) {
  return await specialNodeAction(page, label, '完成', async () => {
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '完成特殊节点' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    if (label === '灭菌报告') {
      await fillFirstVisible(dialog.locator('input[placeholder="请输入灭菌批次"]'), STERILIZATION_BATCH_NO, '灭菌批次')
    }
    await clickVisibleButton(dialog, /^确\s*认$/, `${label} 完成确认`)
  })
}

async function fillLabeledInput(root, label, value) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  await fillFirstVisible(item.locator('input:not([type="hidden"])'), value, label)
}

async function fillLabeledTextarea(root, label, value) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  await fillFirstVisible(item.locator('textarea'), value, label)
}

async function fillLabeledNumber(root, label, value) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  await fillFirstVisible(item.locator('.el-input-number input, input:not([type="hidden"])'), String(value), label)
}

async function selectLabeledOption(page, root, label, optionText) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  const select = item.locator('.el-select input[role="combobox"], .el-select__wrapper').first()
  await select.click({ force: true })
  const options = page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
  await options.first().waitFor({ state: 'visible', timeout: 60000 })
  const option = options.filter({ hasText: optionText }).first()
  if ((await option.count()) === 0) {
    const visibleOptions = (await options.allTextContents()).map((text) => text.trim()).filter(Boolean)
    throw blocked(`${label} 缺少选项：${optionText}`, [`当前可见选项：${JSON.stringify(visibleOptions)}`])
  }
  await option.click()
}

async function selectEntityByDialog(page, root, fieldLabel, dialogTitle, searchLabel, searchValue, rowText) {
  const item = root.locator('.el-form-item').filter({ hasText: fieldLabel }).first()
  await item.locator('input:not([type="hidden"])').first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: dialogTitle }).last()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillLabeledInput(dialog, searchLabel, searchValue)
  await clickVisibleButton(dialog, /^搜索$/, `${dialogTitle} 搜索`)
  const row = dialog.locator('.el-table__body-wrapper tbody tr, .el-table__row').filter({ hasText: rowText }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.click()
  await clickVisibleButton(dialog, /^确\s*定$/, `${dialogTitle} 确定`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
}

async function fillIndicatorResultValues(page, dialog) {
  const valueItems = dialog.locator('.el-form-item').filter({ hasText: '检测值' })
  const count = await valueItems.count()
  for (let index = 0; index < count; index += 1) {
    const item = valueItems.nth(index)
    if (!(await item.isVisible().catch(() => false))) continue
    const select = item.locator('.el-select input[role="combobox"], .el-select__wrapper').first()
    if ((await select.count()) > 0 && (await select.isVisible().catch(() => false))) {
      await select.click({ force: true })
      const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)').first()
      await option.waitFor({ state: 'visible', timeout: 60000 })
      await option.click()
      continue
    }
    const numberInput = item.locator('.el-input-number input').first()
    if ((await numberInput.count()) > 0 && (await numberInput.isVisible().catch(() => false))) {
      await numberInput.fill('1')
      continue
    }
    const textarea = item.locator('textarea').first()
    if ((await textarea.count()) > 0 && (await textarea.isVisible().catch(() => false))) {
      await textarea.fill(`${FILL_PREFIX}-OQC-VALUE-${index + 1}`)
      continue
    }
    const input = item.locator('input:not([type="hidden"])').first()
    if ((await input.count()) > 0 && (await input.isVisible().catch(() => false))) {
      await input.fill(`${FILL_PREFIX}-OQC-VALUE-${index + 1}`)
    }
  }
}

async function ensureOqcTemplateIndicatorByUi(page) {
  if (!OQC_INDICATOR_CODE.trim() || !OQC_INDICATOR_NAME.trim()) {
    throw blocked('创建 OQC 前缺少 EDHR_FULL_E2E_OQC_INDICATOR_CODE 或 EDHR_FULL_E2E_OQC_INDICATOR_NAME，无法补齐模板检测指标。')
  }
  await gotoPath(page, ROUTES.qcTemplate)
  await page.getByText('质检方案').first().waitFor({ state: 'visible', timeout: 60000 })
  const toolbar = page.locator('form').filter({ hasText: '方案编号' }).first()
  await fillLabeledInput(toolbar, '方案编号', OQC_TEMPLATE_CODE)
  const templateSearchPromise = waitForApiResponse(page, '/mes/qc/template/page', '搜索 OQC 质检方案', 'GET', (response) =>
    response.url().includes(`code=${encodeURIComponent(OQC_TEMPLATE_CODE)}`)
  )
  await clickVisibleButton(toolbar, /^搜索$/, '搜索 OQC 质检方案')
  await templateSearchPromise
  const row = page.locator('.el-table__body-wrapper tbody tr, .el-table__row').filter({ hasText: OQC_TEMPLATE_CODE }).first()
  if (!(await row.waitFor({ state: 'visible', timeout: 60000 }).then(() => true, () => false))) {
    const visibleRows = await page
      .locator('.el-table__body-wrapper tbody tr, .el-table__row')
      .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').replace(/\s+/g, ' ').trim()).filter(Boolean))
      .catch(() => [])
    throw blocked('OQC 质检方案列表未显示目标方案：' + OQC_TEMPLATE_CODE, [
      '当前可见方案行：' + JSON.stringify(visibleRows)
    ])
  }
  await clickVisibleButton(row, '编辑', '编辑 OQC 质检方案')
  const templateDialog = page.locator('.el-dialog:visible').filter({ hasText: '方案编号' }).first()
  await templateDialog.waitFor({ state: 'visible', timeout: 60000 })
  await templateDialog.getByText('检测指标项', { exact: true }).click()
  const indicatorPane = templateDialog.locator('.el-tab-pane:visible').filter({ hasText: '新增指标项' }).first()
  await indicatorPane.waitFor({ state: 'visible', timeout: 60000 })

  const existingByCode = indicatorPane.locator('.el-table__body-wrapper tbody tr, .el-table__row').filter({ hasText: OQC_INDICATOR_CODE }).first()
  const existingByName = indicatorPane.locator('.el-table__body-wrapper tbody tr, .el-table__row').filter({ hasText: OQC_INDICATOR_NAME }).first()
  if (
    ((await existingByCode.count()) > 0 && (await existingByCode.isVisible().catch(() => false))) ||
    ((await existingByName.count()) > 0 && (await existingByName.isVisible().catch(() => false)))
  ) {
    await clickVisibleButton(templateDialog, /^取\s*消$/, '关闭 OQC 质检方案')
    return { created: false, templateCode: OQC_TEMPLATE_CODE, indicatorCode: OQC_INDICATOR_CODE }
  }

  await clickVisibleButton(indicatorPane, '新增指标项', '新增 OQC 模板指标项')
  const indicatorDialog = page.locator('.el-dialog:visible').filter({ hasText: '质检指标' }).last()
  await indicatorDialog.waitFor({ state: 'visible', timeout: 60000 })
  await selectEntityByDialog(page, indicatorDialog, '质检指标', '质检指标选择', '检测项名称', OQC_INDICATOR_NAME, OQC_INDICATOR_NAME)
  await fillLabeledNumber(indicatorDialog, '标准值', 1)
  await fillLabeledTextarea(indicatorDialog, '检测方法', `${FILL_PREFIX}-OQC-CHECK`)
  await fillLabeledTextarea(indicatorDialog, '备注', `${FILL_PREFIX}-OQC-INDICATOR`)
  const createPromise = waitForApiResponse(page, ENDPOINTS.templateIndicatorCreate, '新增 OQC 质检方案检测指标项', 'POST')
  await clickVisibleButton(indicatorDialog, /^确\s*定$/, '确认新增 OQC 模板指标项')
  const templateIndicatorId = Number(await createPromise)
  assert.ok(Number.isFinite(templateIndicatorId) && templateIndicatorId > 0, '新增 OQC 质检方案检测指标项未返回有效 ID。')
  await indicatorDialog.waitFor({ state: 'hidden', timeout: 60000 })
  await indicatorPane.locator('.el-table__body-wrapper tbody tr, .el-table__row').filter({ hasText: OQC_INDICATOR_NAME }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await clickVisibleButton(templateDialog, /^取\s*消$/, '关闭 OQC 质检方案')
  return { created: true, templateCode: OQC_TEMPLATE_CODE, indicatorCode: OQC_INDICATOR_CODE, templateIndicatorId }
}

async function ensureOqcTemplateBindingByUi(page) {
  await gotoPath(page, ROUTES.qcTemplate)
  await page.getByText('质检方案').first().waitFor({ state: 'visible', timeout: 60000 })
  const toolbar = page.locator('form').filter({ hasText: '方案编号' }).first()
  await fillLabeledInput(toolbar, '方案编号', OQC_TEMPLATE_CODE)
  const templateSearchPromise = waitForApiResponse(page, '/mes/qc/template/page', '搜索 OQC 质检方案', 'GET', (response) =>
    response.url().includes(`code=${encodeURIComponent(OQC_TEMPLATE_CODE)}`)
  )
  await clickVisibleButton(toolbar, /^搜索$/, '搜索 OQC 质检方案')
  await templateSearchPromise
  const row = page.locator('.el-table__body-wrapper tbody tr, .el-table__row').filter({ hasText: OQC_TEMPLATE_CODE }).first()
  if (!(await row.waitFor({ state: 'visible', timeout: 60000 }).then(() => true, () => false))) {
    const visibleRows = await page
      .locator('.el-table__body-wrapper tbody tr, .el-table__row')
      .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').replace(/\s+/g, ' ').trim()).filter(Boolean))
      .catch(() => [])
    throw blocked('OQC 质检方案列表未显示目标方案：' + OQC_TEMPLATE_CODE, [
      '当前可见方案行：' + JSON.stringify(visibleRows)
    ])
  }
  await clickVisibleButton(row, '编辑', '编辑 OQC 质检方案')
  const templateDialog = page.locator('.el-dialog:visible').filter({ hasText: '方案编号' }).first()
  await templateDialog.waitFor({ state: 'visible', timeout: 60000 })
  await templateDialog.getByText('产品关联', { exact: true }).click()
  const productPane = templateDialog.locator('.el-tab-pane:visible').filter({ hasText: '新增产品关联' }).first()
  await productPane.waitFor({ state: 'visible', timeout: 60000 })

  const existing = productPane.locator('.el-table__body-wrapper tbody tr, .el-table__row').filter({ hasText: OQC_PRODUCT_ITEM_CODE }).first()
  if ((await existing.count()) > 0 && (await existing.isVisible().catch(() => false))) {
    await clickVisibleButton(templateDialog, /^取\s*消$/, '关闭 OQC 质检方案')
    return { created: false, templateCode: OQC_TEMPLATE_CODE, productItemCode: OQC_PRODUCT_ITEM_CODE }
  }

  await clickVisibleButton(productPane, '新增产品关联', '新增 OQC 产品关联')
  const itemDialog = page.locator('.el-dialog:visible').filter({ hasText: '产品物料' }).last()
  await itemDialog.waitFor({ state: 'visible', timeout: 60000 })
  await selectEntityByDialog(page, itemDialog, '产品物料', '物料产品选择', '物料编码', OQC_PRODUCT_ITEM_CODE, OQC_PRODUCT_ITEM_CODE)
  await fillLabeledNumber(itemDialog, '最低检测数', 1)
  await fillLabeledNumber(itemDialog, '最大不合格数', 0)
  const createPromise = waitForApiResponse(page, ENDPOINTS.templateItemCreate, '新增 OQC 质检方案产品关联', 'POST')
  await clickVisibleButton(itemDialog, /^确\s*定$/, '确认新增 OQC 产品关联')
  let templateItemId
  try {
    templateItemId = Number(await createPromise)
  } catch (error) {
    if (String(error?.message || error).includes('该产品已关联此质检方案')) {
      await clickVisibleButton(itemDialog, /^取\s*消$/, '关闭重复 OQC 产品关联')
      await clickVisibleButton(templateDialog, /^取\s*消$/, '关闭 OQC 质检方案')
      return { created: false, templateCode: OQC_TEMPLATE_CODE, productItemCode: OQC_PRODUCT_ITEM_CODE }
    }
    throw error
  }
  assert.ok(Number.isFinite(templateItemId) && templateItemId > 0, '新增 OQC 质检方案产品关联未返回有效 ID。')
  await itemDialog.waitFor({ state: 'hidden', timeout: 60000 })
  await productPane.locator('.el-table__body-wrapper tbody tr, .el-table__row').filter({ hasText: OQC_PRODUCT_ITEM_CODE }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await clickVisibleButton(templateDialog, /^取\s*消$/, '关闭 OQC 质检方案')
  return { created: true, templateCode: OQC_TEMPLATE_CODE, productItemCode: OQC_PRODUCT_ITEM_CODE, templateItemId }
}

async function createAndFinishOqcByUi(page, batchDetail, batchCode) {
  if (!OQC_PRODUCT_ITEM_CODE.trim()) {
    throw blocked('创建 OQC 前缺少 EDHR_FULL_E2E_OQC_PRODUCT_ITEM_CODE，无法选择产品物料。')
  }
  await gotoPath(page, ROUTES.oqc)
  await page.getByText('出货检验').first().waitFor({ state: 'visible', timeout: 60000 })
  await clickVisibleButton(page, '新增', '新增出货检验单')
  const createDialog = page.locator('.el-dialog:visible').filter({ hasText: '添加出货检验单' }).first()
  await createDialog.waitFor({ state: 'visible', timeout: 60000 })

  await fillLabeledInput(createDialog, '检验单编号', OQC_CODE)
  await fillLabeledInput(createDialog, '检验单名称', `${FILL_PREFIX}-OQC`)
  await selectEntityByDialog(page, createDialog, '产品物料', '物料产品选择', '物料编码', OQC_PRODUCT_ITEM_CODE, OQC_PRODUCT_ITEM_CODE)
  await selectEntityByDialog(page, createDialog, '客户', '客户选择', '客户编码', OQC_CLIENT_CODE, OQC_CLIENT_CODE)
  await fillLabeledInput(createDialog, '批次号', batchCode)
  await fillLabeledNumber(createDialog, '发货数量', 1)
  await fillLabeledNumber(createDialog, '检测数量', 1)
  await fillLabeledNumber(createDialog, '合格品数量', 1)
  await fillLabeledNumber(createDialog, '不合格品数量', 0)
  await selectEntityByDialog(page, createDialog, '检测人员', '人员选择', '用户名称', OQC_INSPECTOR_USERNAME, OQC_INSPECTOR_USERNAME)
  await fillLabeledInput(createDialog, '出货日期', '2026-06-14')
  await fillLabeledInput(createDialog, '检测日期', '2026-06-14')
  await selectLabeledOption(page, createDialog, '检测结果', OQC_RESULT_LABEL)
  await fillLabeledTextarea(createDialog, '备注', `${FILL_PREFIX}-OQC`)
  const createPromise = waitForApiResponse(page, ENDPOINTS.oqcCreate, '新增出货检验单', 'POST')
  await clickVisibleButton(createDialog, /^保\s*存$/, '保存出货检验单')
  const oqcId = Number(await createPromise)
  assert.ok(Number.isFinite(oqcId) && oqcId > 0, '新增 OQC 未返回有效 ID。')
  await createDialog.waitFor({ state: 'hidden', timeout: 60000 })

  const toolbar = page.locator('form').filter({ hasText: '检验单编号' }).first()
  await fillLabeledInput(toolbar, '检验单编号', OQC_CODE)
  await clickVisibleButton(toolbar, /^搜索$/, '搜索新建 OQC')
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: OQC_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickVisibleButton(row, '编辑', '编辑新建 OQC')
  const updateDialog = page.locator('.el-dialog:visible').filter({ hasText: '修改出货检验单' }).first()
  await updateDialog.waitFor({ state: 'visible', timeout: 60000 })

  await updateDialog.getByRole('tab', { name: '检测结果' }).click()
  const detailPromise = waitForApiResponse(page, ENDPOINTS.indicatorResultDetail, '加载 OQC 检测结果明细', 'GET')
  await clickVisibleButton(updateDialog, '新增', '新增 OQC 检测结果')
  await detailPromise
  const resultDialog = page.locator('.el-dialog:visible').filter({ hasText: '检测值' }).last()
  await resultDialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillLabeledInput(resultDialog, '样品编号', `${OQC_CODE}-SAMPLE`)
  await fillLabeledInput(resultDialog, '物资SN', `${OQC_CODE}-SN`)
  await fillLabeledTextarea(resultDialog, '备注', `${FILL_PREFIX}-OQC-RESULT`)
  await fillIndicatorResultValues(page, resultDialog)
  const resultPromise = waitForApiResponse(page, ENDPOINTS.indicatorResultCreate, '新增 OQC 检测结果', 'POST')
  await clickVisibleButton(resultDialog, /^确\s*定$/, '确认新增 OQC 检测结果')
  await resultPromise
  await resultDialog.waitFor({ state: 'hidden', timeout: 60000 })

  const finishPromise = waitForApiResponse(
    page,
    ENDPOINTS.oqcFinish,
    '完成 OQC 出货检验单',
    'PUT',
    (response) => response.url().includes(`id=${oqcId}`)
  )
  await clickVisibleButton(updateDialog, /^完\s*成$/, '完成 OQC 出货检验单')
  const confirm = page.locator('.el-message-box:visible').first()
  await confirm.waitFor({ state: 'visible', timeout: 60000 })
  await clickVisibleButton(confirm, /确\s*认|确\s*定|确认/, '确认完成 OQC')
  await finishPromise
  await updateDialog.waitFor({ state: 'hidden', timeout: 60000 })
  return { oqcId, oqcCode: OQC_CODE }
}

async function loadExecutionDetail(page, executionId, options = {}) {
  const currentUrl = new URL(page.url())
  const workTaskId = options.preserveWorkTask === false ? '' : currentUrl.searchParams.get('workTaskId')
  const target = `${ROUTES.executionDetail}?id=${executionId}${workTaskId ? `&workTaskId=${workTaskId}` : ''}`
  await gotoPath(page, target)
  await page.locator('.edhr-fill-workspace, .edhr-page-shell__tracking-detail').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  const auth = await browserAuth(page)
  const detail = await apiGet(page, auth, ENDPOINTS.executionDetail, {
    id: executionId,
    ...(workTaskId ? { workTaskId } : {})
  })
  return detail
}

async function fillSignAndSubmitExecution(page, fillActor, taskIndex, valuePrefix) {
  const fill = await fillEditableControls(page, valuePrefix, taskIndex)
  const fieldAudit = await saveFieldAuditIfNeeded(page, fillActor.signaturePassword, taskIndex)
  const submitResult = await submitExecution(page, fillActor.signaturePassword, undefined, taskIndex)
  return { fill, fieldAudit, submitResult }
}

async function fillFormCenterControls(page, drawer, valuePrefix, taskIndex) {
  const panel = drawer.locator('.form-action-panel').first()
  let filled = 0
  let selected = 0
  const formItems = panel.locator('.el-form-item')
  const count = await formItems.count()

  for (let index = 0; index < count; index += 1) {
    const item = formItems.nth(index)
    if (!(await item.isVisible().catch(() => false))) continue
    const itemDisabled = await item
      .evaluate((element) => element.closest('.is-disabled') != null || element.querySelector('.is-disabled') != null)
      .catch(() => true)
    if (itemDisabled) continue

    const select = item.locator('.el-select input[role="combobox"], .el-select__wrapper').first()
    if ((await select.count()) > 0 && (await select.isVisible().catch(() => false)) && (await select.isEnabled().catch(() => false))) {
      await select.click({ force: true })
      const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)').first()
      if (await option.waitFor({ state: 'visible', timeout: 5000 }).then(() => true).catch(() => false)) {
        await option.click()
        selected += 1
        continue
      }
    }

    const radio = item.locator('.el-radio:not(.is-disabled)').first()
    if ((await radio.count()) > 0 && (await radio.isVisible().catch(() => false))) {
      await radio.click()
      selected += 1
      continue
    }

    const checkbox = item.locator('.el-checkbox:not(.is-disabled)').first()
    if ((await checkbox.count()) > 0 && (await checkbox.isVisible().catch(() => false))) {
      await checkbox.click()
      selected += 1
      continue
    }

    const numberInput = item.locator('.el-input-number input').first()
    if ((await numberInput.count()) > 0 && (await numberInput.isVisible().catch(() => false)) && (await numberInput.isEnabled().catch(() => false))) {
      await numberInput.fill(String(100 + taskIndex + filled))
      await numberInput.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const dateInput = item.locator('.el-date-editor input').first()
    if ((await dateInput.count()) > 0 && (await dateInput.isVisible().catch(() => false)) && (await dateInput.isEnabled().catch(() => false))) {
      const isDateTime = await dateInput
        .evaluate((element) => element.closest('.el-date-editor')?.className.includes('datetime') === true)
        .catch(() => false)
      await dateInput.fill(isDateTime ? '2026-07-25 10:20:30' : '2026-07-25')
      await dateInput.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const textarea = item.locator('textarea').first()
    if ((await textarea.count()) > 0 && (await textarea.isVisible().catch(() => false)) && (await textarea.isEnabled().catch(() => false))) {
      await textarea.fill(valuePrefix + '-' + (filled + 1))
      await textarea.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const input = item.locator('input:not([type="hidden"]):not([type="password"]):not([type="checkbox"]):not([role="combobox"])').first()
    if ((await input.count()) === 0 || !(await input.isVisible().catch(() => false)) || !(await input.isEnabled().catch(() => false))) continue
    const readonly = await input.evaluate((element) => element.hasAttribute('readonly')).catch(() => true)
    if (readonly) continue
    await input.fill(valuePrefix + '-' + (filled + 1))
    await input.press('Tab').catch(() => undefined)
    filled += 1
  }

  return { filled, selected }
}

async function findFormCenterTaskCard(page, task) {
  const cards = page.locator('.edhr-batch-detail__rail-process-form-item')
  await cards.first().waitFor({ state: 'visible', timeout: 60000 })
  const tokens = formCenterTaskSearchTokens(task)
  for (const token of tokens) {
    const candidate = cards.filter({ hasText: token }).first()
    if ((await candidate.count()) > 0 && (await candidate.isVisible().catch(() => false))) {
      return candidate
    }
  }

  const visibleCards = await cards.evaluateAll((nodes) =>
    nodes
      .filter((node) => Boolean(node.offsetWidth || node.offsetHeight || node.getClientRects().length))
      .map((node) => (node.innerText || '').replace(/\s+/g, ' ').trim())
      .filter(Boolean)
  )
  throw blocked('批次详情未找到 FormCenter 表单任务卡片：' + tokens.join(' / '), [
    '任务：' + JSON.stringify({
      id: task.id,
      formCenterInstanceId: task.formCenterInstanceId,
      formTemplateId: task.formTemplateId,
      formTemplateName: task.formTemplateName,
      formSlotType: task.formSlotType,
      processName: task.processName
    }),
    '当前可见表单卡片：' + JSON.stringify(visibleCards)
  ])
}

async function processRouteFormCenterTask(page, batchId, batchCode, task, index) {
  const batchDetail = await loadBatchDetailByUi(page, batchId, '打开 FormCenter 任务前批次详情 T' + index)
  const pendingTask = (batchDetail.tasks || []).find((item) => Number(item.id) === Number(task.id)) || task
  assert.ok(isFormCenterRouteTask(pendingTask), '任务 ' + pendingTask.id + ' 必须是 FormCenter 路线表单任务。')

  const workTaskQuery = pendingTask.activeWorkTaskId ? '&workTaskId=' + pendingTask.activeWorkTaskId : ''
  await gotoPath(page, ROUTES.batchDetail + '?id=' + batchId + '&batchTaskId=' + pendingTask.id + workTaskQuery)
  await page.getByText('eDHR批次详情').first().waitFor({ state: 'visible', timeout: 60000 })
  const taskCard = await findFormCenterTaskCard(page, pendingTask)
  const openResponsePromise = waitForApiResponse(page, ENDPOINTS.batchTaskOpen, '打开 FormCenter 路线表单 T' + index, 'POST')
  await taskCard.getByRole('button', { name: /打开填写|打开返工|处理/ }).first().click()
  const opened = await openResponsePromise
  assert.ok(opened?.formCenterInstanceId, 'FormCenter 任务 ' + pendingTask.id + ' 打开后必须返回 formCenterInstanceId。')
  assert.ok(opened?.formTemplateId, 'FormCenter 任务 ' + pendingTask.id + ' 打开后必须返回 formTemplateId。')
  assert.equal(Number(opened.formCenterInstanceId), Number(pendingTask.formCenterInstanceId), 'FormCenter 任务 ' + pendingTask.id + ' 实例 ID 必须匹配批次详情。')
  assert.equal(Number(opened.formTemplateId), Number(pendingTask.formTemplateId), 'FormCenter 任务 ' + pendingTask.id + ' 模板 ID 必须匹配批次详情。')

  if (Number(opened.status) === 40 && (opened.instanceScope === 'BATCH_SHARED' || pendingTask.instanceScope === 'BATCH_SHARED')) {
    await loadBatchDetailByUi(page, batchId, 'FormCenter 共享实例已生效后刷新批次详情 T' + index)
    return {
      taskId: pendingTask.id,
      routeProcessSort: pendingTask.routeProcessSort,
      processCode: pendingTask.processCode,
      processName: pendingTask.processName,
      formCenterInstanceId: Number(opened.formCenterInstanceId),
      formTemplateId: Number(opened.formTemplateId),
      formTemplateName: pendingTask.formTemplateName,
      formSlotType: pendingTask.formSlotType,
      filledFields: 0,
      selectedFields: 0,
      draftStatus: 'SKIPPED_ALREADY_EFFECTIVE',
      submittedStatus: 'EFFECTIVE',
      autoCompletedByEffectiveSharedInstance: true,
      batchCode
    }
  }

  const drawer = page.locator('.el-drawer:visible').filter({ hasText: /填写表单|表单/ }).last()
  await drawer.waitFor({ state: 'visible', timeout: 60000 })
  await drawer.locator('.form-action-panel').waitFor({ state: 'visible', timeout: 60000 })
  const fill = await fillFormCenterControls(page, drawer, FILL_PREFIX + '-FORMCENTER-T' + index, index)

  const draftResponsePromise = waitForApiResponse(
    page,
    '/form-center/instances/' + opened.formCenterInstanceId + '/draft',
    '保存 FormCenter 草稿 T' + index,
    'PUT'
  )
  await drawer.getByRole('button', { name: '保存草稿' }).click()
  const draft = await draftResponsePromise

  const submitResponsePromise = waitForApiResponse(
    page,
    '/form-center/instances/' + opened.formCenterInstanceId + '/submit',
    '提交 FormCenter 实例 T' + index,
    'POST'
  )
  await drawer.getByRole('button', { name: /^提交$/ }).click()
  const submitted = await submitResponsePromise
  assert.ok(
    ['EFFECTIVE', 'PENDING_EFFECT', 'IN_APPROVAL'].includes(submitted.status),
    'FormCenter 实例 ' + opened.formCenterInstanceId + ' 提交后状态异常：' + submitted.status
  )

  return {
    taskId: pendingTask.id,
    routeProcessSort: pendingTask.routeProcessSort,
    processCode: pendingTask.processCode,
    processName: pendingTask.processName,
    formCenterInstanceId: Number(opened.formCenterInstanceId),
    formTemplateId: Number(opened.formTemplateId),
    formTemplateName: pendingTask.formTemplateName,
    formSlotType: pendingTask.formSlotType,
    filledFields: fill.filled,
    selectedFields: fill.selected,
    draftStatus: draft?.status,
    submittedStatus: submitted.status,
    batchCode
  }
}

async function processRouteTask(fillPage, approvalPage, batchId, batchCode, task, index, fillActor, reviewerActor, options = {}) {
  const pendingTask = task
  assert.ok(Number(pendingTask?.id || 0) > 0, `普通工序任务 T${index} 必须来自已加载的批次详情。`)
  const fillTaskUrl = shouldTakeOverRouteTask(pendingTask)
    ? await openFillTaskFromBatchDetailTakeover(fillPage, batchId, batchCode, pendingTask)
    : await openFillTaskFromBoard(fillPage, batchId, batchCode, pendingTask)
  const opened = fillTaskUrl.openedTask
  assert.ok(opened?.executionId, `工作台处理普通工序任务后必须返回 executionId，任务 ${pendingTask.id}`)
  const executionId = Number(opened.executionId)
  const detailData = await loadExecutionDetail(fillPage, executionId)
  const executionCode = detailData?.executionCode || opened.executionCode || String(executionId)
  assert.ok(String(executionCode).trim(), `执行任务 ${executionId} 必须返回执行编号。`)
  await fillPage.locator('.edhr-fill-workspace').first().waitFor({ state: 'visible', timeout: 60000 })

  const firstPass = await fillSignAndSubmitExecution(fillPage, fillActor, index, `${FILL_PREFIX}-T${index}`)
  let rejection = null
  let rejectedDetail = null
  let rework = null

  if (options.rejectOnce) {
    rejection = await rejectExecution(approvalPage, reviewerActor, executionCode, index, executionId)
    rejectedDetail = await assertRejectedExecutionReadonly(fillPage, executionId)
    const reworkBatchDetail = await loadBatchDetailByUi(fillPage, batchId, `驳回返工批次详情 T${index}`)
    const reworkTask = (reworkBatchDetail.tasks || []).find((item) => Number(item.id) === Number(pendingTask.id)) || pendingTask
    const reworkUrl = await openFillTaskFromBoard(fillPage, batchId, batchCode, reworkTask, {
      taskTypeOption: '驳回修改',
      rowTypeTexts: ['REWORK 修改'],
      actionLabel: '返工',
      expectedWorkTaskId: rejection.reworkTaskId,
      expectedExecutionId: rejection.revisionExecutionId,
      allowExecutionDetail: true
    })
    const reworkOpened = reworkUrl.openedTask
    assert.ok(reworkOpened?.executionId, `工作台处理返工任务后必须返回 executionId，任务 ${reworkTask.id}`)
    const revisionExecutionId = Number(reworkOpened.executionId)
    assert.equal(revisionExecutionId, Number(rejection.revisionExecutionId), `返工任务必须打开修订草稿 ${rejection.revisionExecutionId}`)
    const reworkDetailData = await loadExecutionDetail(fillPage, revisionExecutionId)
    assert.equal(Number(reworkDetailData.sourceRejectedExecutionId), executionId, `修订草稿必须关联被驳回原执行 ${executionId}`)
    const reworkExecutionCode = reworkDetailData?.executionCode || reworkOpened.executionCode || String(revisionExecutionId)
    assert.ok(String(reworkExecutionCode).trim(), `返工执行 ${revisionExecutionId} 必须返回执行编号。`)
    await fillPage.locator('.edhr-fill-workspace').first().waitFor({ state: 'visible', timeout: 60000 })
    await fillPage.getByText('当前记录是驳回后创建的受控修订草稿。').first().waitFor({ state: 'visible', timeout: 60000 })
    const reworkPass = await fillSignAndSubmitExecution(fillPage, fillActor, index, `${FILL_PREFIX}-REWORK-T${index}`)
    rework = {
      revisionExecutionId,
      reworkExecutionCode,
      reworkTaskId: Number(rejection.reworkTaskId),
      filledFields: reworkPass.fill.filled,
      selectedFields: reworkPass.fill.selected,
      fieldAuditSaved: reworkPass.fieldAudit.saved,
      fieldAuditPendingCount: reworkPass.fieldAudit.pendingCount,
      submitSelectionCount: Object.keys(reworkPass.submitResult.selections || {}).length
    }
  }

  return {
    taskId: task.id,
    routeProcessSort: task.routeProcessSort,
    processCode: task.processCode,
    processName: task.processName,
    batchRecordReportId: task.batchRecordReportId,
    batchRecordReportName: task.batchRecordReportName,
    executionId,
    executionCode,
    filledFields: firstPass.fill.filled,
    selectedFields: firstPass.fill.selected,
    fieldAuditSaved: firstPass.fieldAudit.saved,
    fieldAuditPendingCount: firstPass.fieldAudit.pendingCount,
    submitSelectionCount: Object.keys(firstPass.submitResult.selections || {}).length,
    rejectedStatus: rejectedDetail?.status,
    rejectionSignatureId: rejection?.signatureId,
    rejectedAt: rejection?.rejectedAt,
    rejectionReason: rejection?.reason,
    revisionExecutionId: rework?.revisionExecutionId,
    reworkExecutionCode: rework?.reworkExecutionCode,
    reworkTaskId: rework?.reworkTaskId,
    reworkFilledFields: rework?.filledFields,
    reworkSelectedFields: rework?.selectedFields,
    reworkFieldAuditSaved: rework?.fieldAuditSaved,
    reworkFieldAuditPendingCount: rework?.fieldAuditPendingCount,
    reworkSubmitSelectionCount: rework?.submitSelectionCount,
  }
}

async function closeBatch(page, batchId, signaturePassword) {
  await loadBatchDetailByUi(page, batchId, '关闭前批次详情')
  const detail = await syncBatchByUi(page, batchId)
  assert.ok(
    detail.canClose === true ||
      Number(detail.status) === READY_TO_CLOSE_BATCH_STATUS ||
      ACCEPTED_BATCH_STATUSES.has(Number(detail.status)),
    `关闭前批次必须满足关闭条件，当前 status=${detail.status}。`
  )
  await clickVisibleButton(page, '关闭批次', '关闭批次')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '关闭 eDHR 批次' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(dialog.locator('textarea'), `${FILL_PREFIX}-BATCH_CLOSE`, '关闭说明')
  await fillFirstVisible(dialog.locator('input[type="password"]'), signaturePassword, '关闭密码')
  const closeResponsePromise = waitForApiResponse(page, ENDPOINTS.batchClose, '关闭批次', 'POST')
  await clickVisibleButton(dialog, /^确\s*认$/, '确认关闭批次')
  const closed = await closeResponsePromise
  assert.ok(closed.closedAt, '关闭批次后未返回 closedAt。')
  return closed
}

async function openArchiveTaskFromBoard(page, batchCode) {
  await gotoPath(page, ROUTES.workTask)
  await page.getByText('任务类型').first().waitFor({ state: 'visible', timeout: 60000 })
  const toolbar = page.locator('.edhr-work-task-page__toolbar').first()
  await toolbar.locator('.el-select').first().click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: '最终归档' }).first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
  await fillFirstVisible(toolbar.locator('.el-form-item').filter({ hasText: '批次' }).locator('input'), batchCode, '归档任务批次筛选')
  await clickVisibleButton(toolbar, /^查询$/, '查询最终归档待办')
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: batchCode }).filter({ hasText: '最终归档' }).first()
  try {
    await row.waitFor({ state: 'visible', timeout: 60000 })
  } catch (error) {
    const auth = await browserAuth(page)
    const apiPage = await apiGet(page, auth, '/mes/pro/edhr-work-task/my-page', {
      pageNo: 1,
      pageSize: 20,
      taskType: 'ARCHIVE',
      batchCode,
      status: 'TODO'
    }).catch((apiError) => ({ apiError: apiError.message }))
    throw blocked(`归档工作台未找到批次 ${batchCode} 的最终归档待办。`, [
      `当前可见任务行：${JSON.stringify(await visibleTableRowTexts(page))}`,
      `当前账号归档待办接口：${JSON.stringify(apiPage)}`
    ])
  }
  await clickVisibleButton(row, '处理', '处理最终归档待办')
  await page.waitForURL(
    (url) => url.pathname === ROUTES.batchDetail && Boolean(url.searchParams.get('workTaskId')) && Boolean(url.searchParams.get('id')),
    { timeout: 60000 }
  )
  return new URL(page.url())
}

async function generateArchiveAndPrint(page) {
  const generateButton = await waitForVisibleEnabledButton(page, '生成最终归档', '生成最终归档')
  const generateResponsePromise = waitForApiResponse(page, ENDPOINTS.batchArchiveGenerate, '生成批次最终归档', 'POST')
  await generateButton.click()
  const archive = await generateResponsePromise
  assert.ok(archive?.id, '生成最终归档未返回 archive id。')
  assert.equal(archive.archiveStatus, 'SEALED', '批次最终归档必须为 SEALED。')
  const pdfBuffer = await downloadAndPrintArchiveViaUi(page)
  return { archive, pdfBuffer }
}

function extractPdfText(buffer) {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'edhr-pdf-text-'))
  const pdfPath = path.join(tempDir, 'archive.pdf')
  const python = process.env.EDHR_PDF_TEXT_PYTHON || 'python'
  const script = [
    'import sys',
    'from pypdf import PdfReader',
    'reader = PdfReader(sys.argv[1])',
    'texts = []',
    'for page in reader.pages:',
    '    texts.append(page.extract_text() or "")',
    'print("\\n".join(texts))'
  ].join('\n')
  try {
    fs.writeFileSync(pdfPath, buffer)
    return execFileSync(python, ['-X', 'utf8', '-c', script, pdfPath], {
      encoding: 'utf8',
      maxBuffer: 10 * 1024 * 1024,
      windowsHide: true
    })
  } catch (error) {
    const details = [error.message, error.stdout, error.stderr].filter(Boolean).join('\n')
    throw new Error(`最终 PDF 文本解析失败；请确认 Python 可用且已安装 pypdf。${details ? `\n${details}` : ''}`)
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true })
  }
}

async function verifyActorPageAccess(browser, actorConfig, pathSuffix, expectedText) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const writes = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && WRITE_METHODS.has(request.method())) {
      writes.push(`${request.method()} ${request.url()}`)
    }
  })
  try {
    await login(page, actorConfig, pathSuffix)
    await page.goto(`${BASE_URL}${pathSuffix}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    try {
      await waitForAnyVisible(page.getByText(expectedText), expectedText)
    } catch (error) {
      await captureEvidence(page, `actor-access-failed-${actorConfig.prefix}`, {
        actor: actorConfig.username,
        pathSuffix,
        expectedText,
        visibleText: await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
      })
      throw error
    }
    assert.deepEqual(writes, [], `${actorConfig.label} 页面验收阶段不得写入 MES 数据：${JSON.stringify(writes)}`)
  } finally {
    await context.close()
  }
}

async function verifyBatchDetailUi(page, batch, batchExecutionId) {
  await page.goto(`${BASE_URL}${ROUTES.batchDetail}?id=${batchExecutionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('eDHR批次详情').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(String(batch.batchCode || batch.batchExecutionCode)).first().waitFor({ state: 'visible', timeout: 60000 })
  for (const label of REQUIRED_SPECIAL_NODES.values()) {
    await page.getByText(label).first().waitFor({ state: 'visible', timeout: 60000 })
  }
}

async function runCreateBatchFlow(browser, ownerPage, config) {
  const owner = config.actors[0]
  const closeOwner = config.actors[2]
  const reviewActor = config.actors[4]
  const archiver = config.actors[5]
  const created = await createBatchByUi(ownerPage, closeOwner)
  const cellRuleConfirmations = await ensureBatchTaskCellRulesConfirmedByUi(ownerPage, created.batch)
  await loadBatchDetailByUi(ownerPage, created.batchExecutionId, '填写规则确认后批次详情')
  const batchId = created.batchExecutionId
  const processedTasks = []

  const approvalContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const approvalPage = await approvalContext.newPage()
  const archiveContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const archivePage = await archiveContext.newPage()
  const closeContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const closePage = await closeContext.newPage()
  try {
    await login(approvalPage, reviewActor, ROUTES.approval)
    await skipSpecialNode(ownerPage, '来料检报告', owner.signaturePassword)

    for (let guard = 0; guard < 60; guard += 1) {
      const detail = await loadBatchDetailByUi(ownerPage, batchId, `创建模式批次详情 ${guard + 1}`)
      const activeRouteTasks = (detail.tasks || [])
        .filter(isActiveRouteFormTask)
        .sort((left, right) => (left.routeProcessSort || 0) - (right.routeProcessSort || 0) || (left.batchRecordSort || 0) - (right.batchRecordSort || 0))
      if (activeRouteTasks.length === 0) {
        const incompleteRouteTasks = (detail.tasks || []).filter(isIncompleteRouteFormTask)
        if (incompleteRouteTasks.length === 0) break
        await syncBatchByUi(ownerPage, batchId).catch(() => undefined)
        await ownerPage.waitForTimeout(1000)
        continue
      }
      const taskIndex = processedTasks.length + 1
      const nextRouteTask = activeRouteTasks[0]
      if (isFormCenterRouteTask(nextRouteTask)) {
        processedTasks.push(await processRouteFormCenterTask(ownerPage, batchId, created.batchCode, nextRouteTask, taskIndex))
      } else {
        processedTasks.push(
          await processRouteTask(ownerPage, approvalPage, batchId, created.batchCode, nextRouteTask, taskIndex, owner, reviewActor, {
            rejectOnce: REJECT_FIRST_ROUTE_TASK && processedTasks.length === 0
          })
        )
      }
    }

    assert.ok(processedTasks.length > 0, '创建模式必须至少处理一张普通工序批记录表单。')
    if (REJECT_FIRST_ROUTE_TASK) {
      assert.ok(
        processedTasks.some((task) => task.reworkTaskId && task.revisionExecutionId && task.reworkApprovalSignatureId),
        '创建模式必须覆盖一次审批驳回、返工修订草稿和返工后审批通过。'
      )
    }
    const afterRouteDetail = await loadBatchDetailByUi(ownerPage, batchId, '普通表单全部审批后批次详情')
    const remainingRouteTasks = (afterRouteDetail.tasks || []).filter(isIncompleteRouteFormTask)
    assert.deepEqual(
      remainingRouteTasks.map((task) => ({ id: task.id, name: task.batchRecordReportName, status: task.status })),
      [],
      '普通工序批记录和 FormCenter 路线表单必须全部完成。'
    )

    await completeSpecialNode(ownerPage, '灭菌报告')
    await skipSpecialNode(ownerPage, '成品检报告', owner.signaturePassword)
    await skipSpecialNode(ownerPage, '成品检记录', owner.signaturePassword)
    const specialDoneDetail = await loadBatchDetailByUi(ownerPage, batchId, '特殊节点完成后批次详情')
    const oqcResult = await createAndFinishOqcByUi(ownerPage, specialDoneDetail, created.batchCode)
    await login(closePage, closeOwner, `${ROUTES.batchDetail}?id=${batchId}`)
    await loadBatchDetailByUi(closePage, batchId, '关闭责任人批次详情')
    await assertCurrentActor(closePage, closeOwner, 'close-batch')
    await closeBatch(closePage, batchId, closeOwner.signaturePassword)

    await login(archivePage, archiver, ROUTES.workTask)
    const archiveUrl = await openArchiveTaskFromBoard(archivePage, created.batchCode)
    assert.equal(archiveUrl.searchParams.get('id'), String(batchId), '归档待办必须进入同一批次详情。')
    const archiveResult = await generateArchiveAndPrint(archivePage)

    return {
      batchExecutionId: batchId,
      batchCode: created.batchCode,
      oqc: oqcResult,
      processedTasks,
      cellRuleConfirmations,
      rejectReworkEvidence: processedTasks.find((task) => task.reworkTaskId && task.revisionExecutionId),
      archive: archiveResult.archive,
      pdfBuffer: archiveResult.pdfBuffer
    }
  } finally {
    await approvalContext.close()
    await archiveContext.close()
    await closeContext.close()
  }
}

async function downloadAndPrintArchiveViaUi(page) {
  if ((await page.getByRole('button', { name: '下载打印版 PDF' }).count()) === 0) {
    throw blocked('缺少可点击控件：下载打印版 PDF', [`当前页面可见按钮：${JSON.stringify(await visibleButtonLabels(page))}`])
  }
  const [downloadResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/edhr-batch-execution-archive/download') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    ),
    clickFirstEnabled(page.getByRole('button', { name: '下载打印版 PDF' }), '下载打印版 PDF')
  ])
  assert.equal(downloadResponse.status(), 200, '受控 PDF 下载 HTTP 必须为 200')
  const pdfBuffer = Buffer.from(await downloadResponse.body())

  const printPopupPromise = page.waitForEvent('popup', { timeout: 15000 }).catch(() => null)
  await clickFirstEnabled(page.getByRole('button', { name: '打印' }), '打印')
  const printPopup = await printPopupPromise
  const printedToastCount = await page.getByText('打印版 PDF 窗口已打开').count()
  assert.ok(printPopup || printedToastCount > 0, '打印入口必须打开浏览器打印窗口或显示打印版 PDF 窗口已打开提示')
  if (printPopup) await printPopup.close()
  return pdfBuffer
}

async function loadArchiveDoneTaskByApi(browser, archiver, batchCode) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page, archiver, ROUTES.workTask)
    const auth = await browserAuth(page)
    const pageResult = await apiGet(page, auth, '/mes/pro/edhr-work-task/done-page', {
      pageNo: 1,
      pageSize: 20,
      taskType: 'ARCHIVE',
      batchCode
    })
    const tasks = pageResult?.list || []
    const task = tasks.find((item) => item.taskType === 'ARCHIVE' && item.batchCode === batchCode)
    assert.ok(task, `归档人已办任务中缺少批次 ${batchCode} 的最终归档任务。`)
    assert.equal(Number(task.assigneeUserId), Number(archiver.userId), `最终归档已办任务必须归属 ${archiver.username}。`)
    assert.equal(task.status, 'DONE', `最终归档已办任务必须为 DONE，当前：${task.status}`)
    return task
  } finally {
    await context.close()
  }
}

function collectActorsFromTimeline(timeline, extraActors = []) {
  const actors = new Set()
  const addActor = (value) => {
    if (value !== undefined && value !== null && String(value).trim()) actors.add(String(value))
  }
  for (const actor of extraActors) {
    addActor(actor)
  }
  for (const signature of timeline.signatureRecords || []) {
    addActor(signature.actorId || signature.actorName)
  }
  for (const approval of timeline.approvalRecords || []) {
    addActor(approval.actorId || approval.assigneeUserId)
  }
  for (const task of timeline.taskEvents || []) {
    addActor(task.assigneeUserId || task.operatorId)
  }
  for (const review of timeline.executionReviews || []) {
    for (const signature of review.signatureRecords || []) {
      addActor(signature.actorId || signature.actorName)
    }
  }
  for (const archive of timeline.archiveVersions || []) {
    addActor(archive.generatedBy || archive.sealedBy)
  }
  return actors
}

function assertBatchEvidence(detail, timeline, archive, pdfBuffer, extraActors = []) {
  assert.ok(ACCEPTED_BATCH_STATUSES.has(Number(detail.status)), `批次必须已关闭或已归档，当前 status=${detail.status}`)
  assert.ok(detail.closedAt, '批次必须已经关闭并记录 closedAt')
  const tasks = detail.tasks || []
  assert.ok(tasks.length > 0, '批次详情必须包含任务列表')
  assert.ok(tasks.some((task) => task.nodeType === ROUTE_FORM_NODE_TYPE), '批次必须包含普通工序批记录表单任务')
  for (const [nodeType, label] of REQUIRED_SPECIAL_NODES) {
    const task = tasks.find((item) => item.nodeType === nodeType)
    assert.ok(task, `批次必须包含特殊节点：${label}`)
    assert.ok(APPROVED_OR_SKIPPED_TASK_STATUSES.has(Number(task.status)), `${label} 必须已完成或已跳过，当前 status=${task.status}`)
    if (Number(task.status) === 45) {
      assert.ok(task.skippedBy, `${label} 跳过时必须记录操作人`)
      assert.ok(task.skippedAt, `${label} 跳过时必须记录操作时间`)
    }
  }
  const skippedRouteForms = tasks.filter((task) => task.nodeType === ROUTE_FORM_NODE_TYPE && Number(task.status) === 45)
  assert.deepEqual(skippedRouteForms, [], '普通模板表单不允许跳过')
  assert.ok((timeline.signatureRecords || []).length > 0, '复盘时间线必须包含签名记录')
  assert.ok(Array.isArray(timeline.approvalRecords || []), '复盘时间线必须包含放行阶段审核/批准记录')
  assert.ok((timeline.executionReviews || []).length > 0, '复盘时间线必须包含表单复盘记录')
  assert.ok((timeline.archiveVersions || []).length > 0, '复盘时间线必须包含归档版本')
  const actors = collectActorsFromTimeline(timeline, extraActors)
  assert.ok(actors.size >= MIN_DISTINCT_ACTORS, `复盘证据至少需要 ${MIN_DISTINCT_ACTORS} 个不同实际操作人，当前：${[...actors].join(', ')}`)
  assert.equal(archive.archiveStatus, 'SEALED', `最终归档状态必须是 SEALED，当前：${archive.archiveStatus}`)
  assert.ok(Number(archive.archiveVersion || 0) > 0, '最终归档必须记录版本号')
  assert.ok(archive.contentHash && String(archive.contentHash).length >= 32, '最终归档必须记录文件 hash')
  assert.ok(archive.generatedAt, '最终归档必须记录生成时间')
  assert.ok(Number(archive.fileSize || 0) > 1000, `最终归档 PDF 文件大小异常：${archive.fileSize}`)

  const text = extractPdfText(pdfBuffer)
  assert.ok(text.trim().length > 0, '最终 PDF 必须可抽取文本内容')
  for (const term of ['eDHR', '批次', '路线', '来料检报告', '灭菌报告', '成品检报告', '成品检记录', '附件', '跳过', '操作人', '操作时间', '签名', '审核', '批准', '返工', '审计', '追踪', 'manifest']) {
    assert.ok(text.includes(term), `最终 PDF 缺少关键内容：${term}`)
  }
}

async function run() {
  assertLocalOnly()
  ensureEvidenceDir()
  const goals = readGoals()
  assertCoreCoverageMatrix(goals)
  assertTailFourCompanionCoverage(goals)
  const config = collectConfig()
  validateConfig(config)
  writeEvidenceJson('run-config.json', {
    runId: RUN_ID,
    baseUrl: BASE_URL,
    backendUrl: BACKEND_URL,
    tenant: TEST_TENANT,
    createBatch: CREATE_BATCH,
    adminSingleActor: ADMIN_SINGLE_ACTOR,
    minDistinctActors: MIN_DISTINCT_ACTORS,
    actors: config.actors.map((item) => ({
      prefix: item.prefix,
      label: item.label,
      username: item.username,
      userId: item.userId
    }))
  })

  const browser = await chromium.launch({ headless: HEADLESS })
  const ownerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const ownerPage = await ownerContext.newPage()
  try {
    const owner = config.actors[0]
    const closeOwner = config.actors[2]
    await login(ownerPage, owner, CREATE_BATCH ? ROUTES.batchList : ROUTES.batchDetail)
    await verifyActorPageAccess(browser, config.actors[1], ROUTES.workTask, '任务类型')
    await verifyActorPageAccess(browser, config.actors[2], ROUTES.workTask, '任务类型')
    await verifyActorPageAccess(browser, config.actors[3], ROUTES.approval, '审批中心')
    await verifyActorPageAccess(browser, config.actors[4], ROUTES.approval, '审批中心')
    await verifyActorPageAccess(browser, config.actors[5], ROUTES.batchList, '批次执行编码')
    await captureEvidence(ownerPage, '01-owner-batch-entry', {
      actor: owner.username,
      assertion: '批次负责人已通过真实登录进入批次执行入口'
    })

    const templateIndicatorBinding = CREATE_BATCH ? await ensureOqcTemplateIndicatorByUi(ownerPage) : null
    const templateBinding = CREATE_BATCH ? await ensureOqcTemplateBindingByUi(ownerPage) : null
    const createdResult = CREATE_BATCH ? await runCreateBatchFlow(browser, ownerPage, config) : null
    const targetBatchExecutionId = createdResult?.batchExecutionId || BATCH_EXECUTION_ID
    await captureEvidence(ownerPage, '02-created-or-opened-batch', {
      batchExecutionId: targetBatchExecutionId,
      batchCode: createdResult?.batchCode
    })

    const auth = await browserAuth(ownerPage)
    assert.ok(auth.token, '最终 API 验证必须取得浏览器 access token')
    assert.ok(auth.tenantId, '最终 API 验证必须取得 tenant-id')
    const detail = await apiGet(ownerPage, auth, '/mes/pro/edhr-batch-execution/get', { id: targetBatchExecutionId })
    const timeline = await apiGet(ownerPage, auth, '/mes/pro/edhr-batch-execution/review-timeline', { id: targetBatchExecutionId })
    const archive = await apiGet(ownerPage, auth, '/mes/pro/edhr-batch-execution-archive/latest', {
      batchExecutionId: targetBatchExecutionId
    })
    await ownerPage.goto(`${BASE_URL}${ROUTES.scheduleOrder}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await ownerPage.getByText('排产工单').first().waitFor({ state: 'visible', timeout: 60000 })
    await verifyBatchDetailUi(ownerPage, detail, targetBatchExecutionId)
    await ownerPage.goto(`${BASE_URL}${ROUTES.batchReview}?id=${targetBatchExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await ownerPage.getByText('已填写批记录').first().waitFor({ state: 'visible', timeout: 60000 })
    await captureEvidence(ownerPage, '03-batch-review-page', {
      batchExecutionId: targetBatchExecutionId,
      assertion: '批次复盘页可见已填写批记录'
    })
    await verifyBatchDetailUi(ownerPage, detail, targetBatchExecutionId)
    const pdfBuffer = await downloadAndPrintArchiveViaUi(ownerPage)
    fs.writeFileSync(path.join(EVIDENCE_DIR, `archive-${targetBatchExecutionId}.pdf`), pdfBuffer)
    const archiveDoneTask = await loadArchiveDoneTaskByApi(browser, config.actors[5], detail.batchCode)
    if (createdResult && REJECT_FIRST_ROUTE_TASK) {
      assert.ok(createdResult.rejectReworkEvidence?.reworkTaskId, '最终验证必须包含审批驳回返工任务证据。')
    }
    assertBatchEvidence(detail, timeline, archive, pdfBuffer, [archiveDoneTask.assigneeUserId])
    writeEvidenceJson('final-summary.json', {
      runId: RUN_ID,
      batchExecutionId: targetBatchExecutionId,
      batchCode: detail.batchCode,
      processedRouteTasks: createdResult?.processedTasks?.length || 0,
      cellRuleConfirmations: createdResult?.cellRuleConfirmations?.length || 0,
      cellRuleSavedCount: createdResult?.cellRuleConfirmations?.filter((item) => item.savedByUi).length || 0,
      rejectReworkEvidence: createdResult?.rejectReworkEvidence,
      oqc: createdResult?.oqc,
      archiveId: archive.id,
      archiveStatus: archive.archiveStatus,
      archivePdf: path.join(EVIDENCE_DIR, `archive-${targetBatchExecutionId}.pdf`),
      distinctActorCount: collectActorsFromTimeline(timeline, [archiveDoneTask.assigneeUserId]).size,
      actorUsernames: config.actors.map((item) => item.username),
      evidenceDir: EVIDENCE_DIR,
      coveredCoreRequirements: CORE_REQUIREMENT_IDS.length,
      companionRequirements: TAIL_FOUR_COMPANION_REQUIREMENTS
    })
    console.log(`PASS: eDHR full-chain multi-user real E2E batchExecutionId=${targetBatchExecutionId}`)
    if (createdResult) {
      console.log(`PASS: createdBatch=${createdResult.batchCode} processedRouteTasks=${createdResult.processedTasks.length}`)
      if (createdResult.rejectReworkEvidence) {
        const evidence = createdResult.rejectReworkEvidence
        console.log(
          `PASS: rejectRework originalExecution=${evidence.executionId} revisionExecution=${evidence.revisionExecutionId} reworkTask=${evidence.reworkTaskId}`
        )
      }
      console.log(`PASS: oqc=${createdResult.oqc.oqcCode} templateBindingCreated=${templateBinding?.created === true}`)
      console.log(`PASS: templateIndicatorCreated=${templateIndicatorBinding?.created === true}`)
    }
    console.log(
      `PASS: coveredCoreRequirements=${CORE_REQUIREMENT_IDS.length} companionRequirements=${TAIL_FOUR_COMPANION_REQUIREMENTS.join(',')} archive=${archive.id} actors=${collectActorsFromTimeline(timeline, [archiveDoneTask.assigneeUserId]).size}`
    )
  } finally {
    await ownerContext.close()
    await browser.close()
  }
}

run().catch((error) => {
  if (error.blocked) {
    console.error(`BLOCKED: ${error.message}`)
    for (const detail of error.details || []) {
      console.error(`- ${detail}`)
    }
    process.exitCode = 2
  } else {
    console.error(error)
    process.exitCode = 1
  }
})
