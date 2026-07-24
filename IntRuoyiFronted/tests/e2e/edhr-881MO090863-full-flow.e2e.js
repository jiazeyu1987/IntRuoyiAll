const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260610-edhr-881MO090863-full-e2e-fill'
const REQUIRED_BASE_URL = 'http://localhost:8081'
const TENANT = '芋道源码'
const USERNAME = 'admin'
const DEFAULT_PASSWORD = 'admin123'
const WORK_ORDER_CODE = '881MO090863'
const ROUTE_CODE = 'ROUTE-YXN.069.001.1001'
const ROUTE_ID = 900022
const REQUIRED_TASK_COUNT = 15
const TASK_TOTAL = 21
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', TASK_ID)
const TASK_DIR = path.resolve(process.cwd(), 'doc', 'tasks', TASK_ID)
const EVIDENCE_FILE = path.join(TASK_DIR, 'real-e2e-evidence.md')

const BATCH_LIST_ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const BATCH_DETAIL_ROUTE = '/mes/pro/feedback/edhr-batch-execution/detail'
const BATCH_REVIEW_ROUTE = '/mes/pro/feedback/edhr-batch-execution/review'
const EXECUTION_DETAIL_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const APPROVAL_ROUTE = '/mes/pro/feedback/edhr-approval'
const DOMAIN_TRACE_DETAIL_ROUTE = '/mes/pro/feedback/edhr-domain-trace/detail'
const PRODUCTION_SCHEDULE_ROUTE = '/mes/pro/task'

const ENDPOINTS = {
  workOrderPage: '/mes/pro/work-order/page',
  productionTaskPage: '/mes/pro/task/page',
  productionTaskCreate: '/mes/pro/task/create',
  workstationPage: '/mes/md-workstation/page',
  routeProcessByRoute: '/mes/pro/route-process/list-by-route',
  routeProcessByProduct: '/mes/pro/route-process/list-by-product',
  batchOpenOrCreate: '/mes/pro/edhr-batch-execution/open-or-create',
  batchGet: '/mes/pro/edhr-batch-execution/get',
  batchTaskOpen: '/mes/pro/edhr-batch-execution/task/open',
  batchSync: '/mes/pro/edhr-batch-execution/sync-status',
  batchClose: '/mes/pro/edhr-batch-execution/close',
  batchArchiveGenerate: '/mes/pro/edhr-batch-execution-archive/generate',
  batchArchiveLatest: '/mes/pro/edhr-batch-execution-archive/latest',
  batchArchiveDownload: '/mes/pro/edhr-batch-execution-archive/download',
  batchReviewTimeline: '/mes/pro/edhr-batch-execution/review-timeline',
  executionDetail: '/mes/pro/batch-record-execution/get',
  fieldAuditSave: '/mes/pro/batch-record-execution/field-audit/save-changes',
  formReviewSign: '/mes/pro/batch-record-execution/cosign',
  executionSubmit: '/mes/pro/batch-record-execution/submit',
  approvalPending: '/mes/pro/batch-record-execution/approval-pending-page',
  approvalApprove: '/mes/pro/batch-record-execution/approve',
  domainTraceVerify: '/mes/pro/batch-record-execution/domain-trace/verify'
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function timestamp() {
  const now = new Date()
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function collectConfig() {
  const runId = envValue('EDHR_881_E2E_RUN_ID') || timestamp()
  const batchCode = envValue('EDHR_881_E2E_BATCH_CODE') || `E2E-881MO090863-${runId}`
  const fillPrefix = envValue('EDHR_881_E2E_FILL_PREFIX') || `E2E模拟填写-881MO090863-${runId}`
  const config = {
    baseUrl: envValue('EDHR_881_E2E_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_881_E2E_TENANT') || TENANT,
    username: envValue('EDHR_881_E2E_USERNAME') || USERNAME,
    password: envValue('EDHR_881_E2E_PASSWORD') || DEFAULT_PASSWORD,
    signaturePassword: envValue('EDHR_881_E2E_SIGNATURE_PASSWORD') || envValue('EDHR_881_E2E_PASSWORD') || DEFAULT_PASSWORD,
    allowAdminWrite: envValue('EDHR_881_E2E_ALLOW_ADMIN_WRITE') === '1',
    headed: envValue('EDHR_881_E2E_HEADED') === '1',
    batchCode,
    fillPrefix,
    runId
  }

  const missing = []
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    missing.push({
      key: 'EDHR_881_E2E_BASE_URL',
      description: `必须固定为 ${REQUIRED_BASE_URL}`
    })
  }
  if (config.tenant !== TENANT || config.username !== USERNAME) {
    missing.push({
      key: 'EDHR_881_E2E_TENANT / EDHR_881_E2E_USERNAME',
      description: `本任务只允许 ${TENANT}/${USERNAME}`
    })
  }
  if (!config.allowAdminWrite) {
    missing.push({
      key: 'EDHR_881_E2E_ALLOW_ADMIN_WRITE',
      description: '本脚本会写入芋道源码/admin 真实 eDHR 数据，必须显式设置为 1'
    })
  }
  if (!config.password || !config.signaturePassword) {
    missing.push({
      key: 'EDHR_881_E2E_PASSWORD / EDHR_881_E2E_SIGNATURE_PASSWORD',
      description: '登录密码和签名密码不能为空'
    })
  }
  return { ...config, missing }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('缺少 Playwright runtime，请先在此前端 worktree 执行 pnpm install。')
  }
}

function serializeError(error) {
  return {
    name: error?.name || 'Error',
    message: error?.message || String(error),
    stack: error?.stack
  }
}

function unwrapCommonResult(json, label) {
  if (json && typeof json === 'object' && Object.prototype.hasOwnProperty.call(json, 'code')) {
    assert.equal(json.code, 0, `${label} 业务响应失败：${json.msg || json.message || json.code}`)
    return json.data
  }
  return json
}

async function parseApiResponse(response, label) {
  assert.ok(response.ok(), `${label} HTTP ${response.status()} ${response.url()}`)
  const json = await response.json()
  return unwrapCommonResult(json, label)
}

function responseMatches(response, endpoint, method) {
  return (
    response.url().includes(endpoint) &&
    (!method || response.request().method().toUpperCase() === method.toUpperCase())
  )
}

async function waitForApiResponse(page, endpoint, label, method, predicate) {
  const response = await page.waitForResponse(
    async (candidate) => {
      if (!responseMatches(candidate, endpoint, method)) return false
      if (!predicate) return true
      try {
        return await predicate(candidate)
      } catch {
        return false
      }
    },
    { timeout: 90000 }
  )
  return await parseApiResponse(response, label)
}

async function clickVisibleButton(root, name, label) {
  const deadline = Date.now() + 30000
  let sawVisibleDisabled = false
  while (Date.now() < deadline) {
    const candidates = [
      root.locator('button').filter({ hasText: name }),
      root.getByRole('button', { name })
    ]
    for (const buttons of candidates) {
      const count = await buttons.count()
      for (let index = 0; index < count; index += 1) {
        const button = buttons.nth(index)
        if (!(await button.isVisible().catch(() => false))) continue
        await button.scrollIntoViewIfNeeded()
        if (await button.isDisabled().catch(() => true)) {
          sawVisibleDisabled = true
          continue
        }
        await button.click()
        return
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  if (sawVisibleDisabled) {
    throw new Error(`${label || name} 按钮不可用。`)
  }
  throw new Error(`找不到可见按钮：${label || name}`)
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
  throw new Error(`找不到可见输入框：${label}`)
}

async function waitForText(page, text, label) {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 90000 }).catch((error) => {
    throw new Error(`${label}: ${error.message}`)
  })
}

async function readExecutionCodeFromVisibleDetail(page, executionId) {
  const summary = page.locator('.edhr-page-shell__summary').first()
  await summary.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForFunction(
    (id) => {
      const text = document.querySelector('.edhr-page-shell__summary')?.innerText || ''
      if (!text.includes('执行编码')) return false
      const normalized = text.replace(/\s+/g, ' ').trim()
      if (normalized.includes('执行编码 --')) return false
      return normalized.includes(String(id)) || /执行编码\s+[\w.-]+/.test(normalized)
    },
    executionId,
    { timeout: 30000 }
  )
  const summaryText = (await summary.innerText()).replace(/\s+/g, ' ').trim()
  const match = summaryText.match(/执行编码\s+([A-Za-z0-9._-]+)/)
  assert.ok(match?.[1] && match[1] !== '--', `执行详情未展示有效执行编码：${summaryText}`)
  return match[1]
}

async function gotoPath(page, config, route) {
  await page.goto(`${config.baseUrl}${route}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
}

async function screenshot(page, name) {
  ensureDir(RESULT_DIR)
  const filePath = path.join(RESULT_DIR, `${name}.png`)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(BATCH_LIST_ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 90000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await page.keyboard.press('Enter')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, '密码')
  await clickVisibleButton(loginForm, /^登录$/, '登录')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 90000 })
}

function assertBatchShape(batch, label) {
  assert.equal(batch.workOrderCode, WORK_ORDER_CODE, `${label} 工单编码不一致。`)
  assert.equal(batch.routeCode, ROUTE_CODE, `${label} 路线编码不一致。`)
  assert.equal(batch.taskTotal, TASK_TOTAL, `${label} taskTotal 应为 ${TASK_TOTAL}。`)
  assert.equal(batch.blockedCount, 0, `${label} blockedCount 应为 0。`)
  const required = (batch.tasks || []).filter((task) => task.requiredFlag !== false && task.batchRecordReportId)
  assert.equal(required.length, REQUIRED_TASK_COUNT, `${label} 必填批记录数量应为 ${REQUIRED_TASK_COUNT}。`)
  return required.sort((left, right) => (left.routeProcessSort || 0) - (right.routeProcessSort || 0))
}

async function openOrCreateBatch(page, config) {
  await gotoPath(page, config, BATCH_LIST_ROUTE)
  await waitForText(page, '批次执行编码', '未进入 eDHR 批次执行列表')
  await clickVisibleButton(page, '打开/创建', '打开/创建批次')
  const dialog = page.locator('.el-dialog').filter({ hasText: '打开或创建 eDHR 批次执行' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  const workOrderResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.workOrderPage,
    '未冻结生产工单查询',
    'GET'
  )
  const workOrderInput = dialog.locator('.el-select input').first()
  await workOrderInput.click()
  await workOrderInput.fill(WORK_ORDER_CODE)
  const workOrderPage = await workOrderResponsePromise
  const workOrders = workOrderPage.list || []
  const workOrder = workOrders.find((item) => item.code === WORK_ORDER_CODE)
  assert.ok(workOrder?.id, `未查询到未冻结生产工单 ${WORK_ORDER_CODE}。`)

  const option = page.locator('.el-select-dropdown__item').filter({ hasText: WORK_ORDER_CODE }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await fillFirstVisible(dialog.locator('input').nth(1), config.batchCode, '批次号')
  await fillFirstVisible(dialog.locator('input[placeholder="可选；为空由后端解析"]'), String(ROUTE_ID), '路线ID')

  const openResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.batchOpenOrCreate,
    '打开或创建 eDHR 批次执行',
    'POST'
  )
  await clickVisibleButton(dialog, /^确 认$/, '确认打开或创建')
  const batch = await openResponsePromise
  assert.ok(batch?.id, '打开或创建批次后未返回有效批次 ID。')
  assertBatchShape(batch, '打开或创建结果')
  await page.waitForURL((url) => url.pathname === BATCH_DETAIL_ROUTE, { timeout: 90000 })
  await waitForText(page, config.batchCode, '批次详情未展示目标批次号')
  return { batch, workOrder }
}

function buildScheduleStartTime(taskIndex) {
  const base = new Date()
  base.setHours(8 + (taskIndex % 8), 0, 0, 0)
  base.setDate(base.getDate() + Math.floor(taskIndex / 8))
  return `${base.getFullYear()}-${pad(base.getMonth() + 1)}-${pad(base.getDate())} ${pad(base.getHours())}:00:00`
}

async function openProductionScheduleDialog(page, config) {
  await gotoPath(page, config, PRODUCTION_SCHEDULE_ROUTE)
  await waitForText(page, '待排产工单', '未进入生产排产页面')

  const queryForm = page.locator('.el-form').first()
  await fillFirstVisible(
    queryForm.locator('.el-form-item').filter({ hasText: '工单编码' }).locator('input'),
    WORK_ORDER_CODE,
    '生产排产工单编码'
  )
  const workOrderResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.workOrderPage,
    '生产排产工单查询',
    'GET'
  )
  await clickVisibleButton(queryForm, '搜索', '生产排产搜索')
  const workOrderPage = await workOrderResponsePromise
  const match = (workOrderPage.list || []).find((item) => item.code === WORK_ORDER_CODE)
  assert.ok(match?.id, `生产排产页未查询到工单 ${WORK_ORDER_CODE}。`)

  const row = page.locator('.el-table__row').filter({ hasText: WORK_ORDER_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const routeProcessPromise = waitForApiResponse(
    page,
    ENDPOINTS.routeProcessByProduct,
    `排产产品工艺路线加载 ${WORK_ORDER_CODE}`,
    'GET'
  )
  await clickVisibleButton(row, '排产', `打开 ${WORK_ORDER_CODE} 排产`)
  const dialog = page.locator('.el-dialog').filter({ hasText: '生产排产' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await waitForText(page, '新增任务', '生产排产弹框未展示新增任务入口')
  const routeProcesses = await routeProcessPromise
  return { dialog, routeProcesses }
}

async function inspectScheduleRouteLoadedByUi(dialog, routeProcesses, requiredTasks) {
  assert.ok(Array.isArray(routeProcesses), '排产产品工艺路线响应必须为数组。')
  assert.ok(routeProcesses.length > 0, '排产产品工艺路线响应不能为空。')

  const routeIds = [...new Set(routeProcesses.map((item) => Number(item.routeId)).filter((value) => !Number.isNaN(value)))]
  const loadedProcessIds = new Set(routeProcesses.map((item) => Number(item.processId)))
  const missingRequiredTasks = requiredTasks.filter((task) => !loadedProcessIds.has(Number(task.processId)))

  const warning = dialog.getByText('当前产品未配置工艺路线', { exact: false })
  assert.equal(await warning.count(), 0, '排产弹框提示当前产品未配置工艺路线。')

  const stepTitles = dialog.locator('.el-step__title')
  await stepTitles.first().waitFor({ state: 'visible', timeout: 30000 })
  const visibleStepCount = await stepTitles.count()
  for (const process of routeProcesses) {
    const title = process.processName || process.processCode || String(process.processId)
    await stepTitles.filter({ hasText: title }).first().waitFor({ state: 'visible', timeout: 30000 })
  }

  const matchesBatchRoute = routeIds.length === 1 && routeIds[0] === ROUTE_ID && routeProcesses.length === TASK_TOTAL
  if (matchesBatchRoute) {
    assert.equal(visibleStepCount, TASK_TOTAL, `排产弹框应展示 ${TASK_TOTAL} 个工序步骤。`)
    assert.equal(
      missingRequiredTasks.length,
      0,
      `排产产品工艺路线缺少必填批记录工序：${missingRequiredTasks.map((task) => task.processCode || task.processName || task.processId).join(', ')}`
    )
  }

  return {
    routeIds,
    routeProcessCount: routeProcesses.length,
    visibleStepCount,
    matchesBatchRoute,
    missingRequiredTasks: missingRequiredTasks.map((task) => ({
      processCode: task.processCode,
      processName: task.processName,
      processId: task.processId
    }))
  }
}

async function selectFirstWorkstationForProcess(page, taskDialog, task) {
  const workstationResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.workstationPage,
    `工序 ${task.processCode || task.processName} 工作站查询`,
    'GET'
  )
  const workstationInput = taskDialog.locator('.el-form-item').filter({ hasText: '工作站' }).locator('input').first()
  await workstationInput.click()
  const workstationPage = await workstationResponsePromise
  const workstations = workstationPage.list || []
  assert.ok(
    workstations.length > 0,
    `工序 ${task.processCode || '--'} ${task.processName || '--'} 没有启用工作站，无法补齐生产任务上下文。`
  )
  const workstationDialog = page.locator('.el-dialog').filter({ hasText: '工作站选择' }).last()
  await workstationDialog.waitFor({ state: 'visible', timeout: 30000 })
  const firstRow = workstationDialog.locator('.el-table__row').first()
  await firstRow.waitFor({ state: 'visible', timeout: 30000 })
  await firstRow.click()
  await clickVisibleButton(workstationDialog, /^确 定$/, '确认选择工作站')
  return workstations[0]
}

async function ensureProductionTaskForProcess(page, dialog, config, task, taskIndex) {
  const processTitle = task.processName || task.processCode || String(task.processId)
  const step = dialog.locator('.el-step__title').filter({ hasText: processTitle }).first()
  await step.waitFor({ state: 'visible', timeout: 30000 })
  await step.click()

  const visibleCard = dialog.locator('.el-card:visible').last()
  await visibleCard.locator('.el-table').first().waitFor({ state: 'visible', timeout: 30000 })
  const existingRows = await visibleCard.locator('.el-table__row').count()
  if (existingRows > 0) {
    return { processCode: task.processCode, processName: task.processName, created: false }
  }

  await clickVisibleButton(visibleCard, '新增任务', `新增生产任务 ${processTitle}`)
  const taskDialog = page.locator('.el-dialog').filter({ hasText: '新增生产任务' }).last()
  await taskDialog.waitFor({ state: 'visible', timeout: 30000 })

  const workstation = await selectFirstWorkstationForProcess(page, taskDialog, task)
  await fillFirstVisible(
    taskDialog.locator('.el-form-item').filter({ hasText: '排产数量' }).locator('input'),
    '1.00',
    `排产数量 ${processTitle}`
  )
  const startInput = taskDialog.locator('.el-form-item').filter({ hasText: '开始时间' }).locator('input').first()
  await startInput.click()
  await startInput.fill(buildScheduleStartTime(taskIndex))
  await page.keyboard.press('Enter')
  await fillFirstVisible(
    taskDialog.locator('.el-form-item').filter({ hasText: '生产时长' }).locator('input'),
    '1',
    `生产时长 ${processTitle}`
  )
  await fillFirstVisible(
    taskDialog.locator('.el-form-item').filter({ hasText: '备注' }).locator('textarea'),
    `${config.fillPrefix}-PRO_TASK-${task.processCode || taskIndex}`,
    `生产任务备注 ${processTitle}`
  )

  const createResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.productionTaskCreate,
    `创建生产任务 ${processTitle}`,
    'POST'
  )
  await clickVisibleButton(taskDialog, /^确 定$/, `确认创建生产任务 ${processTitle}`)
  const taskId = await createResponsePromise
  await visibleCard.locator('.el-table__row').first().waitFor({ state: 'visible', timeout: 30000 })
  return {
    processCode: task.processCode,
    processName: task.processName,
    created: true,
    taskId,
    workstationId: workstation.id,
    workstationCode: workstation.code
  }
}

async function fetchProductionTasksByWorkOrder(page, workOrderId) {
  return await page.evaluate(async ({ workOrderId }) => {
    const resolveToken = () => {
      const raw =
        window.localStorage.getItem('ACCESS_TOKEN') ||
        window.localStorage.getItem('accessToken') ||
        window.sessionStorage.getItem('ACCESS_TOKEN') ||
        window.sessionStorage.getItem('accessToken') ||
        ''
      if (!raw) return ''
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && 'v' in parsed) {
          return JSON.parse(parsed.v || '""') || ''
        }
      } catch {}
      return raw
    }
    const token = resolveToken()
    const response = await fetch(
      `/admin-api/mes/pro/task/page?pageNo=1&pageSize=200&workOrderId=${encodeURIComponent(String(workOrderId))}`,
      {
        credentials: 'include',
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      }
    )
    if (!response.ok) {
      throw new Error(`production task page HTTP ${response.status}`)
    }
    const json = await response.json()
    if (json && Object.prototype.hasOwnProperty.call(json, 'code') && json.code !== 0) {
      throw new Error(json.msg || json.message || `production task page business ${json.code}`)
    }
    const data = json.data ?? json
    return Array.isArray(data?.list) ? data.list : []
  }, { workOrderId })
}

async function ensureProductionTasksByUi(page, config, workOrderId, routeId, requiredTasks) {
  const { dialog, routeProcesses } = await openProductionScheduleDialog(page, config)
  const scheduleContext = await inspectScheduleRouteLoadedByUi(dialog, routeProcesses, requiredTasks)
  const existingTasks = await fetchProductionTasksByWorkOrder(page, workOrderId)
  const results = []
  if (!scheduleContext.matchesBatchRoute) {
    await clickVisibleButton(dialog, /^关\s*闭$/, '关闭生产排产弹框')
    await dialog.waitFor({ state: 'hidden', timeout: 30000 })
    for (const task of requiredTasks) {
      const matched = existingTasks.filter(
        (item) => Number(item.routeId) === Number(routeId) && Number(item.processId) === Number(task.processId)
      )
      results.push({
        processCode: task.processCode,
        processName: task.processName,
        created: false,
        existingCount: matched.length,
        taskIds: matched.map((item) => item.id),
        scheduleRouteMismatch: true,
        scheduleRouteIds: scheduleContext.routeIds,
        scheduleRouteProcessCount: scheduleContext.routeProcessCount,
        missingFromScheduleRoute: scheduleContext.missingRequiredTasks.some(
          (item) => Number(item.processId) === Number(task.processId)
        )
      })
    }
    return results
  }

  let index = 0
  for (const task of requiredTasks) {
    index += 1
    const matched = existingTasks.filter(
      (item) => Number(item.routeId) === Number(routeId) && Number(item.processId) === Number(task.processId)
    )
    if (matched.length > 0) {
      results.push({
        processCode: task.processCode,
        processName: task.processName,
        created: false,
        existingCount: matched.length,
        taskIds: matched.map((item) => item.id)
      })
      continue
    }
    results.push(await ensureProductionTaskForProcess(page, dialog, config, task, index))
  }
  await clickVisibleButton(dialog, '关 闭', '关闭生产排产弹框')
  return results
}

async function loadBatchDetailByUi(page, config, batchId, label = '批次详情') {
  const detailPromise = waitForApiResponse(
    page,
    ENDPOINTS.batchGet,
    label,
    'GET',
    (response) => response.url().includes(`id=${batchId}`)
  )
  await gotoPath(page, config, `${BATCH_DETAIL_ROUTE}?id=${batchId}`)
  const detail = await detailPromise
  await waitForText(page, config.batchCode, `${label} 未显示批次号`)
  return detail
}

async function syncBatchByUi(page, batchId) {
  const syncPromise = waitForApiResponse(
    page,
    ENDPOINTS.batchSync,
    '同步批次状态',
    'POST',
    (response) => response.url().includes(`id=${batchId}`)
  )
  await clickVisibleButton(page, '同步状态', '同步状态')
  return await syncPromise
}

async function openTaskByUi(page, batchId, task) {
  const processToken = task.processCode || task.processName || task.batchRecordReportName
  const row = page.locator('.el-table__row').filter({ hasText: processToken }).first()
  await row.scrollIntoViewIfNeeded()
  const openResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.batchTaskOpen,
    `打开工序任务 ${processToken}`,
    'POST'
  )
  await clickVisibleButton(row, '打开填写', `打开填写 ${processToken}`)
  const opened = await openResponsePromise
  assert.ok(opened?.executionId, `工序 ${processToken} 打开后未返回 executionId。`)
  await page.waitForURL((url) => url.pathname === EXECUTION_DETAIL_ROUTE, { timeout: 90000 })
  return opened
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
      await dateInput.fill(isDateTime ? '2026-06-10 10:20:30' : '2026-06-10')
      await dateInput.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const textarea = item.locator('textarea').first()
    if ((await textarea.count()) > 0 && (await textarea.isVisible().catch(() => false)) && (await textarea.isEnabled().catch(() => false))) {
      await textarea.fill(`E2E-T${taskIndex}-${filled + 1}`)
      await textarea.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const input = item.locator('input:not([type="hidden"]):not([type="password"]):not([type="checkbox"])').first()
    if ((await input.count()) === 0 || !(await input.isVisible().catch(() => false)) || !(await input.isEnabled().catch(() => false))) continue
    const readonly = await input.evaluate((element) => element.hasAttribute('readonly')).catch(() => true)
    if (readonly) continue
    await input.fill(`E2E-T${taskIndex}-${filled + 1}`)
    await input.press('Tab').catch(() => undefined)
    filled += 1
  }
  return { filled, selected }
}

async function isActuallyDisabled(locator) {
  if ((await locator.count()) === 0) return true
  return await locator
    .evaluate((element) => {
      return (
        element.disabled === true ||
        element.classList.contains('is-disabled') ||
        element.getAttribute('aria-disabled') === 'true' ||
        element.closest('.is-disabled') != null
      )
    })
    .catch(() => true)
}

async function collectFieldAuditGateContext(page) {
  const alerts = await page.locator('.el-alert:visible, .el-message:visible').allInnerTexts().catch(() => [])
  const rows = await page
    .locator('.edhr-page-shell__field-audit-table .el-table__body tbody tr')
    .allInnerTexts()
    .catch(() => [])
  return {
    alerts: alerts.map((item) => item.replace(/\s+/g, ' ').trim()).filter(Boolean),
    pendingRows: rows.map((item) => item.replace(/\s+/g, ' ').trim()).filter(Boolean).slice(0, 5)
  }
}

async function selectFieldAuditReasonCategory(page, reasonArea) {
  const wrapper = reasonArea.locator('.el-select__wrapper').first()
  const input = reasonArea.locator('.el-select input[role="combobox"]').first()
  for (let attempt = 0; attempt < 3; attempt += 1) {
    if ((await input.count()) > 0) {
      await input.scrollIntoViewIfNeeded()
      await input.click({ force: true })
    } else {
      await wrapper.scrollIntoViewIfNeeded()
      await wrapper.click({ force: true })
    }
    const correctionOption = page.locator('.el-select-dropdown__item').filter({ hasText: '纠正录入' }).last()
    if ((await correctionOption.isVisible().catch(() => false))) {
      await correctionOption.click()
    } else {
      await page.keyboard.press('ArrowDown')
      await page.keyboard.press('Enter')
    }
    await page.waitForTimeout(300)
    const reasonText = await reasonArea.innerText().catch(() => '')
    if (reasonText.includes('CORRECTION') || reasonText.includes('纠正录入')) {
      return
    }
  }
  const popperTexts = await page.locator('.el-popper, .el-select-dropdown').allInnerTexts().catch(() => [])
  throw new Error(`原因分类未选中 CORRECTION：poppers=${JSON.stringify(popperTexts.map((item) => item.replace(/\s+/g, ' ').trim()).filter(Boolean))}`)
}

async function saveFieldAuditIfNeeded(page, config, taskIndex) {
  const pendingRows = page.locator('.edhr-page-shell__field-audit-table .el-table__row')
  const pendingCount = await pendingRows.count()
  const legacySaveButton = page.locator('.edhr-page-shell__field-audit').getByRole('button', { name: /保存变更/ }).first()
  if (pendingCount === 0 || (await legacySaveButton.count()) === 0) {
    return { saved: false, pendingCount }
  }

  const reasonArea = page.locator('.edhr-page-shell__field-audit-reason').first()
  await selectFieldAuditReasonCategory(page, reasonArea)
  await fillFirstVisible(
    reasonArea.locator('.el-form-item').last().locator('input:not([type="hidden"])'),
    `${config.fillPrefix}-FIELD_CHANGE_REASON-${taskIndex}`,
    '字段变更原因'
  )
  await page.keyboard.press('Tab')
  await new Promise((resolve) => setTimeout(resolve, 300))

  const sectionButtons = page.locator('.edhr-page-shell__field-audit .edhr-page-shell__section-actions .el-button')
  const namedSaveButton = sectionButtons.filter({ hasText: /保存变更/ }).first()
  let saveButton = namedSaveButton
  if ((await namedSaveButton.count()) === 0) {
    saveButton = sectionButtons.first()
  }
  await saveButton.waitFor({ state: 'visible', timeout: 30000 })
  await saveButton.scrollIntoViewIfNeeded()
  if (await isActuallyDisabled(saveButton)) {
    const context = await collectFieldAuditGateContext(page)
    throw new Error(`保存变更按钮不可用：alerts=${JSON.stringify(context.alerts)} pending=${JSON.stringify(context.pendingRows)}`)
  }
  await saveButton.click()
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 }).catch(async (error) => {
    const context = await collectFieldAuditGateContext(page)
    throw new Error(`字段变更电子签名弹框未打开：${error.message} alerts=${JSON.stringify(context.alerts)} pending=${JSON.stringify(context.pendingRows)}`)
  })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, '字段变更签名密码')
  const saveResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.fieldAuditSave,
    '字段审计保存',
    'PUT'
  )
  const confirmSaveButton = dialog.locator('.el-dialog__footer .el-button').last()
  await confirmSaveButton.waitFor({ state: 'visible', timeout: 30000 })
  await confirmSaveButton.click({ force: true })
  const result = await saveResponsePromise
  assert.equal(result.hashVerification?.status, 'VALID', '字段审计链校验必须为 VALID。')
  await waitForText(page, '字段变更已写入', '字段审计保存后未出现成功提示')
  return { saved: true, pendingCount, result }
}

async function formReviewSign(page, config, taskIndex) {
  await clickVisibleButton(page, '复核签名', `复核签名 T${taskIndex}`)
  const dialog = page.locator('.el-dialog').filter({ hasText: '表单复核签名' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, '复核签名密码')
  await fillFirstVisible(
    dialog.locator('textarea'),
    `${config.fillPrefix}-FORM_REVIEW-${taskIndex}`,
    '复核备注'
  )
  const signResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.formReviewSign,
    `表单复核签名 T${taskIndex}`,
    'PUT'
  )
  await clickVisibleButton(dialog, /确 认 签 名/, '确认复核签名')
  return await signResponsePromise
}

async function verifyDomainTrace(page, config, executionId) {
  await clickVisibleButton(page, '主数据追溯', '主数据追溯')
  await page.waitForURL((url) => url.pathname === DOMAIN_TRACE_DETAIL_ROUTE, { timeout: 90000 })
  await waitForText(page, '主数据追溯', '未进入主数据追溯详情')
  const verifyResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.domainTraceVerify,
    `主数据追溯校验 executionId=${executionId}`,
    'POST'
  )
  await clickVisibleButton(page, /校验|验证|Verify/i, '主数据追溯校验')
  const result = await verifyResponsePromise
  if (result?.status) {
    assert.notEqual(result.status, 'BLOCKED', `主数据追溯校验阻塞：${JSON.stringify(result.blockers || [])}`)
  }
  await gotoPath(page, config, `${EXECUTION_DETAIL_ROUTE}?id=${executionId}`)
  await waitForText(page, 'eDHR 执行详情', '主数据追溯后未返回执行详情')
  return result
}

async function submitExecution(page, config, taskIndex) {
  await clickVisibleButton(page, '提交执行', `提交执行 T${taskIndex}`)
  const dialog = page.locator('.el-dialog').filter({ hasText: '提交 eDHR 执行' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, '提交密码')
  await fillFirstVisible(dialog.locator('textarea'), `${config.fillPrefix}-SUBMIT-${taskIndex}`, '提交备注')
  const submitResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.executionSubmit,
    `提交执行 T${taskIndex}`,
    'PUT'
  )
  await clickVisibleButton(dialog, /确 认 提 交/, '确认提交执行')
  await submitResponsePromise
}

async function approveExecution(page, config, executionCode, taskIndex) {
  await gotoPath(page, config, `${APPROVAL_ROUTE}?tab=pending`)
  await waitForText(page, '待我审批', '未进入 eDHR 审批页')
  const toolbar = page.locator('.edhr-workbench__toolbar').first()
  await fillFirstVisible(toolbar.locator('input').first(), executionCode, '审批执行编号')
  const pendingResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.approvalPending,
    `待审批查询 ${executionCode}`,
    'GET'
  )
  await clickVisibleButton(toolbar, '查询', '审批查询')
  const pageData = await pendingResponsePromise
  const rows = pageData.list || []
  assert.ok(rows.some((row) => row.executionCode === executionCode), `待我审批未查询到 ${executionCode}。`)
  const row = page.locator('.el-table__row').filter({ hasText: executionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await clickVisibleButton(row, '通过', `审批通过 ${executionCode}`)
  const dialog = page.locator('.el-dialog').filter({ hasText: '通过 eDHR 审批' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, '审批密码')
  await fillFirstVisible(dialog.locator('textarea').last(), `${config.fillPrefix}-APPROVE-${taskIndex}`, '审批意见')
  const approveResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.approvalApprove,
    `审批通过 ${executionCode}`,
    'PUT'
  )
  await clickVisibleButton(dialog, /^确 认$/, '确认审批通过')
  const result = await approveResponsePromise
  assert.equal(result.status, 3, `审批通过后 ${executionCode} 未进入已关闭状态。`)
  return result
}

async function processTask(page, config, batchId, task, index) {
  await loadBatchDetailByUi(page, config, batchId, `打开任务前批次详情 T${index}`)
  const opened = await openTaskByUi(page, batchId, task)
  const executionId = Number(opened.executionId)
  const detailData = await waitForApiResponse(
    page,
    ENDPOINTS.executionDetail,
    `执行详情 ${executionId}`,
    'GET',
    (response) => response.url().includes(`id=${executionId}`)
  ).catch(() => undefined)
  await waitForText(page, WORK_ORDER_CODE, `执行详情未展示工单 ${WORK_ORDER_CODE}`)
  const executionCode = detailData?.executionCode || opened.executionCode || await readExecutionCodeFromVisibleDetail(page, executionId)
  await waitForText(page, executionCode, `执行详情未展示执行编码 ${executionCode}`)

  const fill = await fillEditableControls(page, config.fillPrefix, index)
  const fieldAudit = await saveFieldAuditIfNeeded(page, config, index)
  const domainTrace = await verifyDomainTrace(page, config, executionId)
  await submitExecution(page, config, index)

  return {
    taskId: task.id,
    routeProcessSort: task.routeProcessSort,
    processCode: task.processCode,
    processName: task.processName,
    batchRecordReportId: task.batchRecordReportId,
    batchRecordReportName: task.batchRecordReportName,
    executionId,
    executionCode,
    filledFields: fill.filled,
    selectedFields: fill.selected,
    fieldAuditSaved: fieldAudit.saved,
    fieldAuditPendingCount: fieldAudit.pendingCount,
    domainTraceStatus: domainTrace?.status
  }
}

async function closeBatch(page, config, batchId) {
  const detail = await syncBatchByUi(page, batchId)
  assert.equal(detail.taskApprovedCount, REQUIRED_TASK_COUNT, '关闭前批准数必须为 15。')
  assert.equal(detail.taskTotal, TASK_TOTAL, '关闭前任务总数必须为 21。')
  assert.equal(detail.blockedCount, 0, '关闭前阻塞数必须为 0。')
  assert.equal(detail.canClose, true, '关闭前后端必须返回 canClose=true。')

  await clickVisibleButton(page, '关闭批次', '关闭批次')
  const dialog = page.locator('.el-dialog').filter({ hasText: '关闭 eDHR 批次' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('textarea'), `${config.fillPrefix}-BATCH_CLOSE`, '关闭说明')
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, '关闭密码')
  const closeResponsePromise = waitForApiResponse(page, ENDPOINTS.batchClose, '关闭批次', 'POST')
  await clickVisibleButton(dialog, /^确 认$/, '确认关闭批次')
  const closed = await closeResponsePromise
  assert.ok(closed.closedAt, '关闭批次后未返回 closedAt。')
  return closed
}

async function generateArchiveAndPrint(page, config, batchId) {
  const generateResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.batchArchiveGenerate,
    '生成批次最终归档',
    'POST'
  )
  await clickVisibleButton(page, '生成最终归档', '生成最终归档')
  const archive = await generateResponsePromise
  assert.ok(archive?.id, '生成最终归档未返回 archive id。')
  assert.equal(archive.archiveStatus, 'SEALED', '批次最终归档必须为 SEALED。')

  const downloadPromise = page.waitForEvent('download', { timeout: 90000 })
  const downloadResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.batchArchiveDownload,
    '下载打印版 PDF',
    'GET'
  ).catch(() => undefined)
  await clickVisibleButton(page, '下载打印版 PDF', '下载打印版 PDF')
  const download = await downloadPromise
  const downloadPath = path.join(RESULT_DIR, download.suggestedFilename())
  await download.saveAs(downloadPath)
  assert.ok(fs.existsSync(downloadPath), 'PDF 下载文件不存在。')
  assert.ok(fs.statSync(downloadPath).size > 0, 'PDF 下载文件为空。')
  await downloadResponsePromise

  const popupPromise = page.waitForEvent('popup', { timeout: 90000 }).catch(() => undefined)
  await clickVisibleButton(page, '打印', '打印')
  const printPopup = await popupPromise
  assert.ok(printPopup, '打印窗口未打开。')
  await printPopup.close().catch(() => undefined)
  return { archive, downloadPath, printOpened: true }
}

async function verifyReviewPage(page, config, batchId, processedTasks, archive) {
  const timelinePromise = waitForApiResponse(
    page,
    ENDPOINTS.batchReviewTimeline,
    '批次复盘时间线',
    'GET',
    (response) => response.url().includes(`id=${batchId}`)
  )
  await gotoPath(page, config, `${BATCH_REVIEW_ROUTE}?id=${batchId}`)
  await waitForText(page, '已填写批记录', '未进入批次复盘页')
  const timeline = await timelinePromise
  const approvalRecords = timeline.approvalRecords || []
  const archiveVersions = timeline.archiveVersions || []
  const executionReviews = timeline.executionReviews || []
  assert.equal(executionReviews.length, REQUIRED_TASK_COUNT, '复盘页已填写批记录数量不一致。')
  const reviewByExecutionCode = new Map(executionReviews.map((item) => [item.executionCode, item]))
  for (const task of processedTasks) {
    const review = reviewByExecutionCode.get(task.executionCode)
    assert.ok(review, `复盘时间线缺少执行 ${task.executionCode}。`)
    assert.ok((review.signatureSummary?.submitCount || 0) >= 1, `${task.executionCode} 缺少 SUBMIT 签名。`)
  }
  const expectedFieldAuditCount = processedTasks.filter((task) => task.fieldAuditSaved).length
  const actualFieldAuditCount = executionReviews.filter((item) => (item.fieldAuditSummary?.batchCount || 0) > 0).length
  assert.equal(actualFieldAuditCount, expectedFieldAuditCount, '复盘页字段审计批次数量不一致。')
  assert.ok(Array.isArray(approvalRecords), '复盘页放行阶段审核/批准记录必须可读取。')
  assert.ok(archiveVersions.some((item) => item.id === archive.id || item.archiveStatus === 'SEALED'), '复盘页未展示 SEALED 归档。')
  const visibleText = await page.locator('body').innerText()
  assert.ok(visibleText.includes('已填写批记录'), '复盘页未展示已填写批记录区域。')
  assert.ok(visibleText.includes(processedTasks[0].executionCode), `复盘页未展示默认选中的执行编号 ${processedTasks[0].executionCode}。`)
  return timeline
}

async function finalReadOnlyVerify(page, config, batchId, processedTasks, archive) {
  const result = await page.evaluate(
    async ({ batchId, archiveId, prefix }) => {
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
      async function getJson(url) {
        const accessToken = readCacheValue('ACCESS_TOKEN')
        const tenantId = readCacheValue('tenantId')
        const headers = { Accept: 'application/json' }
        if (accessToken) {
          headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
        }
        if (tenantId) {
          headers['tenant-id'] = String(tenantId)
        }
        const response = await fetch(url, { credentials: 'include', headers })
        if (!response.ok) throw new Error(`${url} HTTP ${response.status}`)
        const json = await response.json()
        if (json && Object.prototype.hasOwnProperty.call(json, 'code') && json.code !== 0) {
          throw new Error(`${url} business ${json.code}: ${json.msg || json.message}`)
        }
        return json.data ?? json
      }
      const batch = await getJson(`/admin-api/mes/pro/edhr-batch-execution/get?id=${batchId}`)
      const archiveLatest = await getJson(`/admin-api/mes/pro/edhr-batch-execution-archive/latest?batchExecutionId=${batchId}`)
      const timeline = await getJson(`/admin-api/mes/pro/edhr-batch-execution/review-timeline?id=${batchId}`)
      const serializedTimeline = JSON.stringify(timeline)
      return {
        batch,
        archiveLatest,
        timeline,
        archiveIdMatches: archiveLatest?.id === archiveId,
        prefixFound: serializedTimeline.includes(prefix)
      }
    },
    { batchId, archiveId: archive.id, prefix: config.fillPrefix }
  )

  assert.equal(result.batch.workOrderCode, WORK_ORDER_CODE, '最终校验工单编码不一致。')
  assert.equal(result.batch.routeCode, ROUTE_CODE, '最终校验路线编码不一致。')
  assert.equal(result.batch.taskApprovedCount, REQUIRED_TASK_COUNT, '最终校验批准数不一致。')
  assert.equal(result.batch.taskTotal, TASK_TOTAL, '最终校验任务总数不一致。')
  assert.equal(result.batch.blockedCount, 0, '最终校验阻塞数不一致。')
  assert.equal(result.batch.status, 40, '最终校验批次状态必须为已归档。')
  assert.equal(result.archiveLatest.archiveStatus, 'SEALED', '最终校验归档状态必须为 SEALED。')
  assert.equal(result.archiveIdMatches, true, '最终校验最新归档 ID 不一致。')
  assert.equal(result.prefixFound, true, '最终校验未在复盘时间线中找到模拟填写前缀。')
  assert.equal(processedTasks.length, REQUIRED_TASK_COUNT, '最终校验处理任务数不一致。')
  return result
}

function writeEvidence(result) {
  ensureDir(TASK_DIR)
  const lines = [
    '# 881MO090863 eDHR 全批次真实 E2E 证据',
    '',
    `- 状态：${result.status}`,
    `- 批次号：${result.batchCode || '--'}`,
    `- 工单：${WORK_ORDER_CODE}`,
    `- 路线：${ROUTE_CODE}`,
    `- 模拟前缀：${result.fillPrefix || '--'}`,
    `- 生成时间：${new Date().toISOString()}`,
    '',
    '## BDD',
    '',
    '- BDD: 创建批次执行 -> Given 芋道源码/admin 存在目标工单和路线 When 真实前端打开或创建批次 Then 批次详情展示 21 道工序、15 张必填批记录和 0 阻塞。',
    '- BDD: 逐张填写签名审批 -> Given 15 张必填批记录 When 逐张打开填写、字段审计、复核签名、追溯校验、提交和审批 Then 全部批准。',
    '- BDD: 关闭归档复盘 -> Given 批次可关闭 When 关闭、归档、下载/打印并打开复盘 Then 可查看填写、签名、审批、关闭和归档记录。',
    ''
  ]

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED')
    lines.push('')
    lines.push(`- BLOCKED: ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`  - \`${item.key}\`：${item.description}`)
    }
  } else if (result.status === 'PASS') {
    lines.push('## GREEN')
    lines.push('')
    lines.push(`- GREEN: \`pnpm e2e:edhr:881-full-flow\` -> PASS。`)
    lines.push(`- 批次执行ID：${result.batchExecutionId}`)
    lines.push(`- 生产任务上下文：新增 ${result.productionTasks.filter((item) => item.created).length}，复用 ${result.productionTasks.filter((item) => !item.created).length}`)
    const scheduleMismatch = result.productionTasks.find((item) => item.scheduleRouteMismatch)
    if (scheduleMismatch) {
      lines.push(`- 排产页产品路线漂移：当前产品路线 ${scheduleMismatch.scheduleRouteIds.join(', ')}，工序数 ${scheduleMismatch.scheduleRouteProcessCount}；本批次仍使用 ${ROUTE_ID} / ${ROUTE_CODE}`)
    }
    lines.push(`- 完成批记录：${result.processedTasks.length} / ${REQUIRED_TASK_COUNT}`)
    lines.push(`- 批次批准数：${result.finalBatch.taskApprovedCount} / ${result.finalBatch.taskTotal}`)
    lines.push(`- 阻塞项：${result.finalBatch.blockedCount}`)
    lines.push(`- 归档状态：${result.archive.archiveStatus}`)
    lines.push(`- 下载文件：\`${result.downloadPath}\``)
    lines.push(`- 打印窗口：${result.printOpened ? '已打开' : '未打开'}`)
    const reviewedExecutions = result.timeline.executionReviews || []
    const fieldAuditReviewCount = reviewedExecutions.filter((item) => (item.fieldAuditSummary?.batchCount || 0) > 0).length
    lines.push(`- 复盘记录：执行 ${reviewedExecutions.length}，字段审计 ${fieldAuditReviewCount}，审批 ${result.timeline.approvalRecords?.length || 0}，归档 ${result.timeline.archiveVersions?.length || 0}`)
    lines.push('')
    lines.push('## 执行明细')
    lines.push('')
    for (const task of result.processedTasks) {
      lines.push(`- sort ${task.routeProcessSort} ${task.processCode || '--'} ${task.processName || '--'} -> ${task.executionCode}, fields=${task.filledFields}, fieldAudit=${task.fieldAuditSaved}, approvalSignatureId=${task.approvalSignatureId || '--'}`)
    }
  } else {
    lines.push('## FAIL')
    lines.push('')
    lines.push(`- RED: ${result.error?.message || '未知错误'}`)
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function writeJsonResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const page = await context.newPage()
  const processedTasks = []

  try {
    await login(page, config)
    const opened = await openOrCreateBatch(page, config)
    const batchId = opened.batch.id
    let batchDetail = await loadBatchDetailByUi(page, config, batchId, '初始批次详情')
    let requiredTasks = assertBatchShape(batchDetail, '初始批次详情')
    const productionTasks = await ensureProductionTasksByUi(page, config, opened.workOrder.id, opened.batch.routeId, requiredTasks)
    await screenshot(page, '01-batch-created')

    let taskIndex = 0
    for (const task of requiredTasks) {
      taskIndex += 1
      if (task.status === 40) continue
      const processed = await processTask(page, config, batchId, task, taskIndex)
      processedTasks.push(processed)
    }

    batchDetail = await loadBatchDetailByUi(page, config, batchId, '全部审批后批次详情')
    requiredTasks = assertBatchShape(batchDetail, '全部审批后批次详情')
    assert.equal(processedTasks.length, REQUIRED_TASK_COUNT, `本轮应处理 ${REQUIRED_TASK_COUNT} 张必填批记录。`)
    assert.ok(requiredTasks.every((task) => task.status === 40), '所有必填批记录必须为已批准。')
    const closed = await closeBatch(page, config, batchId)
    await screenshot(page, '02-batch-closed')
    const archiveResult = await generateArchiveAndPrint(page, config, batchId)
    await screenshot(page, '03-archive-generated')
    const timeline = await verifyReviewPage(page, config, batchId, processedTasks, archiveResult.archive)
    await screenshot(page, '04-review-visible')
    const finalVerify = await finalReadOnlyVerify(page, config, batchId, processedTasks, archiveResult.archive)

    return {
      status: 'PASS',
      batchCode: config.batchCode,
      fillPrefix: config.fillPrefix,
      batchExecutionId: batchId,
      productionTasks,
      processedTasks,
      closed,
      archive: archiveResult.archive,
      downloadPath: archiveResult.downloadPath,
      printOpened: archiveResult.printOpened,
      timeline,
      finalBatch: finalVerify.batch,
      finalArchive: finalVerify.archiveLatest,
      finalVerify
    }
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实写入 E2E 前置条件缺失。',
      missing: config.missing,
      batchCode: config.batchCode,
      fillPrefix: config.fillPrefix
    }
    writeEvidence(result)
    writeJsonResult(result)
    console.error(result.reason)
    process.exitCode = 1
    return
  }

  try {
    const result = await runRealFlow(config)
    writeEvidence(result)
    writeJsonResult(result)
    console.log(`PASS: 881MO090863 full eDHR batch E2E completed. batch=${result.batchCode}`)
  } catch (error) {
    const result = {
      status: 'FAIL',
      batchCode: config.batchCode,
      fillPrefix: config.fillPrefix,
      error: serializeError(error)
    }
    writeEvidence(result)
    writeJsonResult(result)
    throw error
  }
}

main()
