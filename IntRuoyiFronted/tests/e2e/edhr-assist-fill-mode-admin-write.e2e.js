const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const taskDir = path.join(repoRoot, 'doc', 'tasks', '20260720-edhr-admin-assist-write-e2e')
const artifactDir = path.join(taskDir, 'e2e-artifacts')
const resultFile = path.join(artifactDir, 'assist-fill-mode-admin-write-e2e-result.json')
const screenshotFile = path.join(artifactDir, 'assist-fill-mode-admin-write-e2e.png')
const failureScreenshotFile = path.join(artifactDir, 'assist-fill-mode-admin-write-e2e-failure.png')

function createRunId() {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

const config = {
  baseUrl: process.env.EDHR_ADMIN_ASSIST_WRITE_BASE_URL || 'http://localhost:8081',
  tenant: process.env.EDHR_ADMIN_ASSIST_WRITE_TENANT || '芋道源码',
  adminUsername: 'admin',
  adminPassword: process.env.EDHR_ADMIN_ASSIST_WRITE_PASSWORD || '',
  writerUsername: process.env.EDHR_YUDAO_ASSIST_WRITER_USERNAME || '',
  writerPassword: process.env.EDHR_YUDAO_ASSIST_WRITER_PASSWORD || '',
  signaturePassword: process.env.EDHR_ADMIN_ASSIST_SIGNATURE_PASSWORD || '',
  allowTenantWrite: process.env.EDHR_YUDAO_ASSIST_WRITE_ALLOW === '1',
  allowExistingE2eBatch: process.env.EDHR_YUDAO_ASSIST_USE_EXISTING_E2E_BATCH === '1',
  workOrderCode: process.env.EDHR_YUDAO_ASSIST_WORK_ORDER_CODE || '881MO090889',
  batchCode: process.env.EDHR_YUDAO_ASSIST_BATCH_CODE || `E2E-ASSIST-${createRunId()}`,
  headed: process.env.EDHR_ADMIN_ASSIST_WRITE_HEADED === '1',
  maxWorkTaskPagesToScan: Number(process.env.EDHR_ADMIN_ASSIST_WRITE_MAX_WORK_TASK_PAGES || 5),
  maxBatchPagesToScan: Number(process.env.EDHR_ADMIN_ASSIST_WRITE_MAX_BATCH_PAGES || 10),
  maxCandidatesToTry: Number(process.env.EDHR_ADMIN_ASSIST_WRITE_MAX_CANDIDATES || 8)
}

const workTaskPath = '/mes/pro/feedback/edhr-work-task'
const batchExecutionPath = '/mes/pro/feedback/edhr-batch-execution'
const formPath = '/mes/pro/feedback/edhr-execution/form'

function failFast(message, details = {}) {
  const error = new Error(message)
  error.details = details
  return error
}

function ensureConfig() {
  const blockers = []
  if (config.baseUrl !== 'http://localhost:8081') {
    blockers.push(`baseUrl must be http://localhost:8081, got ${config.baseUrl}`)
  }
  if (config.tenant !== '芋道源码') {
    blockers.push(`write E2E must use 芋道源码 tenant, got ${config.tenant}`)
  }
  if (!config.adminPassword) {
    blockers.push('EDHR_ADMIN_ASSIST_WRITE_PASSWORD is required')
  }
  if (!config.writerPassword) {
    blockers.push('EDHR_YUDAO_ASSIST_WRITER_PASSWORD is required')
  }
  if (!config.signaturePassword) {
    blockers.push('EDHR_ADMIN_ASSIST_SIGNATURE_PASSWORD is required')
  }
  if (!config.allowTenantWrite) {
    blockers.push('EDHR_YUDAO_ASSIST_WRITE_ALLOW must be 1')
  }
  if (config.allowExistingE2eBatch && !config.allowTenantWrite) {
    blockers.push('existing E2E batch write requires EDHR_YUDAO_ASSIST_WRITE_ALLOW=1')
  }
  if (config.writerUsername === config.adminUsername) {
    blockers.push('admin must remain readonly; writer username cannot be admin')
  }
  if (!config.workOrderCode.trim()) {
    blockers.push('EDHR_YUDAO_ASSIST_WORK_ORDER_CODE cannot be empty')
  }
  if (!config.batchCode.trim()) {
    blockers.push('EDHR_YUDAO_ASSIST_BATCH_CODE cannot be empty')
  }
  if (blockers.length) {
    throw failFast('edhr_admin_assist_write_e2e_precondition_failed', { blockers })
  }
}

function redactUrl(rawUrl) {
  try {
    const url = new URL(rawUrl)
    for (const key of Array.from(url.searchParams.keys())) {
      if (/token|password|secret|key/i.test(key)) {
        url.searchParams.set(key, '[REDACTED]')
      }
    }
    return url.toString()
  } catch (error) {
    return rawUrl
  }
}

function normalizePageRows(payload) {
  const data = payload?.data || {}
  if (Array.isArray(data.list)) return data.list
  if (Array.isArray(data.records)) return data.records
  if (Array.isArray(data)) return data
  return []
}

async function safeJson(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

function summarizePayload(payload) {
  const data = payload?.data || payload || {}
  return {
    code: payload?.code,
    msg: payload?.msg,
    id: data.id,
    executionId: data.executionId || data.id,
    workTaskId: data.workTaskId,
    batchExecutionId: data.batchExecutionId,
    batchTaskId: data.batchTaskId,
    executionCode: data.executionCode,
    batchExecutionCode: data.batchExecutionCode,
    status: data.status,
    submittedAt: data.submittedAt,
    submittedBy: data.submittedBy,
    fieldAuditRevision: data.fieldAuditRevision,
    fieldAuditHeadHash: data.fieldAuditHeadHash,
    cellValuesHash: data.cellValuesHash,
    auditBatchId: data.auditBatchId,
    signatureId: data.signatureId,
    hashVerificationStatus: data.hashVerification?.status
  }
}

async function summarizePageResponse(response, rowMapper) {
  const payload = await safeJson(response)
  const data = payload?.data || {}
  const rows = normalizePageRows(payload)
  return {
    httpStatus: response.status(),
    code: payload?.code,
    total: data.total ?? data.count ?? rows.length,
    rowCount: rows.length,
    rows: rows.map(rowMapper),
    firstRows: rows.slice(0, 5).map(rowMapper)
  }
}

function summarizeWorkTaskRow(row) {
  return {
    id: row.id,
    taskCode: row.taskCode,
    taskType: row.taskType,
    status: row.status,
    executionId: row.executionId,
    sourceExecutionId: row.sourceExecutionId,
    batchExecutionId: row.batchExecutionId,
    batchTaskId: row.batchTaskId,
    workOrderCode: row.workOrderCode,
    batchCode: row.batchCode,
    processName: row.processName,
    actionUrl: row.actionUrl ? redactUrl(row.actionUrl) : undefined
  }
}

function summarizeBatchRow(row) {
  const tasks = Array.isArray(row.tasks) ? row.tasks : []
  const activeTasks = tasks.filter((task) => task.activeWorkTaskId)
  return {
    id: row.id,
    batchExecutionCode: row.batchExecutionCode,
    workOrderCode: row.workOrderCode,
    batchCode: row.batchCode,
    status: row.status,
    currentProcessName: row.currentProcessName,
    taskTotal: row.taskTotal,
    taskApprovedCount: row.taskApprovedCount,
    currentProcessProductionFillers: row.currentProcessProductionFillers,
    currentProcessEquipmentFillers: row.currentProcessEquipmentFillers,
    currentProcessQualityFillers: row.currentProcessQualityFillers,
    taskStatusSummary: tasks.reduce((acc, task) => {
      const key = String(task.status)
      acc[key] = (acc[key] || 0) + 1
      return acc
    }, {}),
    tasks: activeTasks.map((task) => ({
          id: task.id,
          status: task.status,
          activeWorkTaskId: task.activeWorkTaskId,
          activeWorkTaskType: task.activeWorkTaskType,
          allowedActions: task.allowedActions,
          processName: task.processName,
          canOpen: task.canOpen,
          fillableUsers: task.fillableUsers
        }))
  }
}

function isWorkTaskCandidate(row) {
  const taskType = String(row.taskType || '').toUpperCase()
  const status = String(row.status || '').toUpperCase()
  return (taskType === 'FILL' || taskType === 'REWORK') && status !== 'DONE' && status !== 'CANCELED'
}

function isBatchCandidate(row) {
  if (!Array.isArray(row.tasks)) return false
  return row.tasks.some((task) => {
    const status = String(task.status ?? '').toUpperCase()
    const type = String(task.activeWorkTaskType || '').toUpperCase()
    const actions = Array.isArray(task.allowedActions) ? task.allowedActions : []
    return (
      task.activeWorkTaskId &&
      actions.includes('OPEN_FORM') &&
      type !== 'BLOCKED' &&
      (status === '0' || status === '10' || status === 'WAITING' || status === 'DRAFT')
    )
  })
}

function runOfficialLoginPreflight(credentials, targetPath, targetText, label) {
  const scriptPath = path.join(repoRoot, 'scripts', 'preflight', 'login-preflight.mjs')
  const result = spawnSync(
    process.execPath,
    [
      scriptPath,
      '--base-url',
      config.baseUrl,
      '--tenant',
      config.tenant,
      '--username',
      credentials.username,
      '--password',
      credentials.password,
      '--target-path',
      targetPath,
      '--target-text',
      targetText
    ],
    {
      cwd: repoRoot,
      env: process.env,
      encoding: 'utf8',
      timeout: 120000
    }
  )
  if (result.status !== 0) {
    throw failFast(`official_${label}_login_preflight_failed`, {
      status: result.status,
      stdout: result.stdout,
      stderr: result.stderr
    })
  }
  return result.stdout.trim()
}

async function login(page, credentials) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 60000 })
    await option.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(credentials.username)
  await form.locator('input[type="password"]').first().fill(credentials.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await safeJson(loginResponse)
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_payload_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function waitForWorkTaskPageResponse(page, action, evidence) {
  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-work-task/my-page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    .catch(() => null)
  await action()
  const response = await responsePromise
  if (!response) return null
  assert.ok(response.ok(), `work_task_page_http_failed:${response.status()}`)
  const summary = await summarizePageResponse(response, summarizeWorkTaskRow)
  evidence.workTaskPageResponses.push(summary)
  return summary
}

async function waitForBatchPageResponse(page, action, evidence) {
  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-batch-execution/page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    .catch(() => null)
  await action()
  const response = await responsePromise
  if (!response) return null
  assert.ok(response.ok(), `batch_execution_page_http_failed:${response.status()}`)
  const summary = await summarizePageResponse(response, summarizeBatchRow)
  evidence.batchPageResponses.push(summary)
  return summary
}

async function openWorkTaskBoard(page, evidence) {
  const targetUrl = new URL(workTaskPath, config.baseUrl)
  return waitForWorkTaskPageResponse(
    page,
    async () => {
      await page.goto(targetUrl.toString(), { waitUntil: 'domcontentloaded' })
      await page.getByText('我的待办', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    },
    evidence
  )
}

async function openBatchExecutionList(page, evidence, query = {}) {
  const targetUrl = new URL(batchExecutionPath, config.baseUrl)
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      targetUrl.searchParams.set(key, String(value))
    }
  })
  return waitForBatchPageResponse(
    page,
    async () => {
      await page.goto(targetUrl.toString(), { waitUntil: 'domcontentloaded' })
      await page.getByText('批次执行编码', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    },
    evidence
  )
}

function unwrapBusinessPayload(payload, label) {
  if (payload?.parseError) {
    throw failFast(`${label}_response_not_json`, { payload })
  }
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    assert.ok([0, 200].includes(payload.code), `${label}_business_failed:${JSON.stringify(payload)}`)
  }
  return payload?.data ?? payload
}

function summarizeCreatedBatch(batch) {
  return {
    id: batch?.id,
    batchExecutionCode: batch?.batchExecutionCode,
    workOrderId: batch?.workOrderId,
    workOrderCode: batch?.workOrderCode,
    batchCode: batch?.batchCode,
    routeId: batch?.routeId,
    routeCode: batch?.routeCode,
    routeName: batch?.routeName,
    status: batch?.status,
    currentProcessName: batch?.currentProcessName,
    currentProcessProductionFillers: batch?.currentProcessProductionFillers,
    currentProcessEquipmentFillers: batch?.currentProcessEquipmentFillers,
    currentProcessQualityFillers: batch?.currentProcessQualityFillers,
    tasks: Array.isArray(batch?.tasks)
      ? batch.tasks
        .filter((task) => task.activeWorkTaskId)
        .map((task) => ({
          id: task.id,
          processName: task.processName,
          status: task.status,
          activeWorkTaskId: task.activeWorkTaskId,
          activeWorkTaskType: task.activeWorkTaskType,
          allowedActions: task.allowedActions,
          fillableUsers: task.fillableUsers
        }))
      : []
  }
}

function resolveAssignedWriter(batch) {
  const tasks = Array.isArray(batch?.tasks) ? batch.tasks : []
  const activeFillTask = tasks.find((task) => {
    const type = String(task.activeWorkTaskType || '').toUpperCase()
    return task.activeWorkTaskId && (type === 'FILL' || type === 'REWORK')
  })
  const taskWriter = activeFillTask?.fillableUsers?.find((user) => Number(user?.userId) > 0)
  if (taskWriter) {
    return {
      userId: Number(taskWriter.userId),
      displayName: taskWriter.displayName,
      activeWorkTaskId: Number(activeFillTask.activeWorkTaskId),
      batchTaskId: Number(activeFillTask.id)
    }
  }

  const currentFillers = [
    ...(batch?.currentProcessProductionFillers || []),
    ...(batch?.currentProcessEquipmentFillers || []),
    ...(batch?.currentProcessQualityFillers || [])
  ]
  const currentWriter = currentFillers.find((user) => Number(user?.userId) > 0)
  if (!currentWriter) return null
  return {
    userId: Number(currentWriter.userId),
    displayName: currentWriter.displayName,
    activeWorkTaskId: activeFillTask?.activeWorkTaskId
      ? Number(activeFillTask.activeWorkTaskId)
      : undefined,
    batchTaskId: activeFillTask?.id ? Number(activeFillTask.id) : undefined
  }
}

async function createDedicatedBatch(page, evidence) {
  await openBatchExecutionList(page, evidence)
  const createButton = page.getByRole('button', { name: '打开/创建' }).first()
  await createButton.waitFor({ state: 'visible', timeout: 30000 })
  await createButton.click()

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  const workOrderSelect = dialog
    .locator('.el-form-item')
    .filter({ hasText: '生产工单' })
    .locator('.el-select')
    .first()
  await workOrderSelect.waitFor({ state: 'visible', timeout: 30000 })
  await workOrderSelect.click()
  const workOrderResponsePromise = page.waitForResponse(
    (response) => {
      if (!response.url().includes('/mes/pro/work-order/page') || response.request().method() !== 'GET') {
        return false
      }
      try {
        return new URL(response.url()).searchParams.get('code') === config.workOrderCode
      } catch (error) {
        return false
      }
    },
    { timeout: 60000 }
  )
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
  await page.keyboard.type(config.workOrderCode, { delay: 20 })
  const workOrderResponse = await workOrderResponsePromise
  assert.ok(workOrderResponse.ok(), `work_order_page_http_failed:${workOrderResponse.status()}`)
  const workOrderPayload = await safeJson(workOrderResponse)
  const workOrders = normalizePageRows(workOrderPayload)
  const workOrder = workOrders.find((item) => item.code === config.workOrderCode)
  if (!workOrder?.id) {
    throw failFast('dedicated_batch_work_order_not_found', {
      workOrderCode: config.workOrderCode,
      visibleOptions: workOrders.slice(0, 10).map((item) => ({ id: item.id, code: item.code, name: item.name }))
    })
  }

  const routeResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/edhr-batch-execution/work-order-route-options') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const workOrderOption = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: config.workOrderCode })
    .first()
  await workOrderOption.waitFor({ state: 'visible', timeout: 30000 })
  await workOrderOption.click()
  const routeResponse = await routeResponsePromise
  assert.ok(routeResponse.ok(), `work_order_route_options_http_failed:${routeResponse.status()}`)
  const routePayload = await safeJson(routeResponse)
  const routeOptions = unwrapBusinessPayload(routePayload, 'work_order_route_options')
  const enabledRoute = Array.isArray(routeOptions)
    ? routeOptions.find((route) => route.batchRouteEnabled === true)
    : undefined
  if (!enabledRoute?.routeId) {
    throw failFast('dedicated_batch_has_no_enabled_batch_route', {
      workOrderId: workOrder.id,
      workOrderCode: workOrder.code,
      routeOptions
    })
  }

  const routeSelect = dialog
    .locator('.el-form-item')
    .filter({ hasText: '工艺路线' })
    .locator('.el-select')
    .first()
  const routeInput = routeSelect.locator('input[role="combobox"], input').first()
  await routeInput.waitFor({ state: 'visible', timeout: 30000 })
  const selectedRouteLabel = await routeInput.inputValue().catch(() => '')
  if (!selectedRouteLabel.includes(enabledRoute.routeCode || enabledRoute.routeName || String(enabledRoute.routeId))) {
    await routeSelect.click()
    const routeOptionText = enabledRoute.routeCode || enabledRoute.routeName || String(enabledRoute.routeId)
    const routeOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: routeOptionText })
      .first()
    await routeOption.waitFor({ state: 'visible', timeout: 30000 })
    await routeOption.click()
  }

  await dialog.getByPlaceholder('请输入真实批次号').fill(config.batchCode)
  const remark = dialog.locator('textarea').first()
  if (await remark.count()) {
    await remark.fill(`辅助模式保存提交 E2E ${config.batchCode}`)
  }

  const openResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/edhr-batch-execution/open-or-create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/edhr-batch-execution/get') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await dialog.locator('button').filter({ hasText: '确 认' }).first().click()

  const openResponse = await openResponsePromise
  assert.ok(openResponse.ok(), `dedicated_batch_open_http_failed:${openResponse.status()}`)
  const openPayload = await safeJson(openResponse)
  const openedBatch = unwrapBusinessPayload(openPayload, 'dedicated_batch_open')
  assert.ok(Number(openedBatch?.id) > 0, `dedicated_batch_open_missing_id:${JSON.stringify(openPayload)}`)
  assert.equal(openedBatch.batchCode, config.batchCode, 'dedicated_batch_code_mismatch')

  await page.waitForURL(
    (url) => url.pathname === '/mes/pro/feedback/edhr-batch-execution/detail',
    { timeout: 60000 }
  )
  const detailResponse = await detailResponsePromise
  assert.ok(detailResponse.ok(), `dedicated_batch_detail_http_failed:${detailResponse.status()}`)
  const detailPayload = await safeJson(detailResponse)
  const batch = unwrapBusinessPayload(detailPayload, 'dedicated_batch_detail')
  assert.equal(Number(batch.id), Number(openedBatch.id), 'dedicated_batch_detail_id_mismatch')

  const assignedWriter = resolveAssignedWriter(batch)
  if (!assignedWriter?.userId || !assignedWriter.activeWorkTaskId || !assignedWriter.batchTaskId) {
    throw failFast('dedicated_batch_missing_active_fill_assignment', {
      createdBatch: summarizeCreatedBatch(batch)
    })
  }

  evidence.createdBatch = summarizeCreatedBatch(batch)
  evidence.selectedWorkOrder = {
    id: workOrder.id,
    code: workOrder.code,
    name: workOrder.name
  }
  evidence.selectedRoute = {
    routeId: enabledRoute.routeId,
    routeCode: enabledRoute.routeCode,
    routeName: enabledRoute.routeName,
    batchRouteEnabled: enabledRoute.batchRouteEnabled
  }
  evidence.assignedWriter = assignedWriter
  return { batch, assignedWriter }
}

async function selectExistingE2eWritableBatch(page, evidence) {
  let summary = await openBatchExecutionList(page, evidence)
  const inferredMaxPages = Math.max(1, Math.ceil((summary?.total || 0) / Math.max(1, summary?.rowCount || 10)))
  const maxPages = Math.min(config.maxBatchPagesToScan, inferredMaxPages)

  for (let pageIndex = 1; pageIndex <= maxPages; pageIndex += 1) {
    const rows = summary?.rows || []
    const candidate = rows.find((row) => {
      if (!/^E2E-/.test(String(row.batchCode || ''))) return false
      return (row.tasks || []).some((task) => {
        const taskType = String(task.activeWorkTaskType || '').toUpperCase()
        return (
          task.activeWorkTaskId &&
          (taskType === 'FILL' || taskType === 'REWORK') &&
          Array.isArray(task.fillableUsers) &&
          task.fillableUsers.some((user) => Number(user?.userId) > 0)
        )
      })
    })
    if (candidate) {
      const activeTask = candidate.tasks.find((task) => {
        const taskType = String(task.activeWorkTaskType || '').toUpperCase()
        return task.activeWorkTaskId && (taskType === 'FILL' || taskType === 'REWORK')
      })
      const writer = activeTask.fillableUsers.find((user) => Number(user?.userId) > 0)
      const assignedWriter = {
        userId: Number(writer.userId),
        displayName: writer.displayName,
        activeWorkTaskId: Number(activeTask.activeWorkTaskId),
        batchTaskId: Number(activeTask.id)
      }
      evidence.existingE2eBatch = candidate
      evidence.assignedWriter = assignedWriter
      return {
        batch: candidate,
        assignedWriter,
        usedExistingE2eBatch: true
      }
    }

    if (pageIndex >= maxPages) break
    summary = await goToNextPage(page, evidence, 'batch-execution')
    if (!summary) break
  }

  throw failFast('no_existing_e2e_writer_batch_found', {
    batchPageResponses: evidence.batchPageResponses.map((item) => ({
      total: item.total,
      rowCount: item.rowCount,
      firstRows: item.firstRows
    }))
  })
}

async function discoverUsernameByUserId(page, evidence, assignedWriter) {
  const targetUrl = new URL('/system/user', config.baseUrl)
  targetUrl.searchParams.set('userId', String(assignedWriter.userId))
  await page.goto(targetUrl.toString(), { waitUntil: 'domcontentloaded' })
  await page.getByText('用户名称', { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })

  const row = page.locator('tr.system-user-table__row--target').first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const username = (await row.locator('.system-user-username').innerText()).trim()
  const rowText = (await row.innerText()).replace(/\s+/g, ' ').trim()
  if (!username || username === config.adminUsername) {
    throw failFast('assigned_writer_username_invalid', {
      assignedWriter,
      username,
      rowText
    })
  }
  if (config.writerUsername && config.writerUsername !== username) {
    throw failFast('configured_writer_username_mismatch', {
      configured: config.writerUsername,
      discovered: username,
      assignedWriter
    })
  }
  evidence.writerIdentity = {
    userId: assignedWriter.userId,
    displayName: assignedWriter.displayName,
    username,
    rowText
  }
  return username
}

async function goToNextPage(page, evidence, mode) {
  const nextButton = page.locator('.el-pagination .btn-next').first()
  if (!(await nextButton.count()) || (await nextButton.isDisabled().catch(() => true))) {
    return null
  }
  if (mode === 'work-task') {
    return waitForWorkTaskPageResponse(page, async () => nextButton.click(), evidence)
  }
  return waitForBatchPageResponse(page, async () => nextButton.click(), evidence)
}

async function clickAndWaitForForm(page, clickAction, evidence, source) {
  const openResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-batch-execution/task/open') &&
        response.request().method() === 'POST',
      { timeout: 30000 }
    )
    .catch(() => null)

  await clickAction()
  await page.waitForURL(
    (url) => url.pathname === formPath && url.searchParams.has('workTaskId'),
    { timeout: 60000 }
  )

  const openResponse = await Promise.race([
    openResponsePromise,
    page.waitForTimeout(1000).then(() => null)
  ])
  if (openResponse) {
    assert.ok(openResponse.ok(), `task_open_http_failed:${openResponse.status()}`)
    const payload = await safeJson(openResponse)
    evidence.taskOpenResponses.push({
      source,
      httpStatus: openResponse.status(),
      ...summarizePayload(payload)
    })
  }

  const url = new URL(page.url())
  return {
    source,
    executionId: url.searchParams.get('id') || url.searchParams.get('executionId'),
    workTaskId: url.searchParams.get('workTaskId'),
    batchExecutionId: url.searchParams.get('batchExecutionId'),
    batchTaskId: url.searchParams.get('batchTaskId')
  }
}

async function tryOpenFromWorkTaskBoard(page, evidence, targetBatchCode) {
  let summary = await openWorkTaskBoard(page, evidence)
  const inferredMaxPages = Math.max(1, Math.ceil((summary?.total || 0) / Math.max(1, summary?.rowCount || 10)))
  const maxPages = Math.min(config.maxWorkTaskPagesToScan, inferredMaxPages)
  let tried = 0

  for (let pageIndex = 1; pageIndex <= maxPages; pageIndex += 1) {
    const rows = summary?.rows || []
    const visibleProcessButtons = await page.getByRole('button', { name: '处理' }).count()
    const candidates = rows
      .map((row, index) => ({ row, index }))
      .filter(
        ({ row, index }) =>
          index < visibleProcessButtons &&
          isWorkTaskCandidate(row) &&
          (!targetBatchCode || row.batchCode === targetBatchCode)
      )

    evidence.entryAttempts.push({
      source: 'work-task',
      pageIndex,
      rowCount: rows.length,
      visibleProcessButtons,
      candidateCount: candidates.length,
      targetBatchCode,
      taskTypes: rows.reduce((acc, row) => {
        const key = String(row.taskType || 'UNKNOWN')
        acc[key] = (acc[key] || 0) + 1
        return acc
      }, {})
    })

    for (const candidate of candidates) {
      if (tried >= config.maxCandidatesToTry) break
      tried += 1
      const button = page.getByRole('button', { name: '处理' }).nth(candidate.index)
      try {
        await button.scrollIntoViewIfNeeded()
        const routeContext = await clickAndWaitForForm(page, async () => button.click(), evidence, 'work-task')
        return { routeContext, selected: candidate.row }
      } catch (error) {
        evidence.entryErrors.push({
          source: 'work-task',
          rowIndex: candidate.index,
          selected: candidate.row,
          message: error.message
        })
        await openWorkTaskBoard(page, evidence)
      }
    }

    if (pageIndex >= maxPages) break
    summary = await goToNextPage(page, evidence, 'work-task')
    if (!summary) break
  }
  return null
}

async function tryOpenFromBatchExecutionList(page, evidence, targetBatchCode) {
  let summary = await openBatchExecutionList(page, evidence)
  const inferredMaxPages = Math.max(1, Math.ceil((summary?.total || 0) / Math.max(1, summary?.rowCount || 10)))
  const maxPages = Math.min(config.maxBatchPagesToScan, inferredMaxPages)
  let tried = 0

  for (let pageIndex = 1; pageIndex <= maxPages; pageIndex += 1) {
    const rows = summary?.rows || []
    const visibleFillButtons = await page.getByRole('button', { name: '去填写' }).count()
    const candidates = rows
      .map((row, index) => ({ row, index }))
      .filter(({ row }) => isBatchCandidate(row) && (!targetBatchCode || row.batchCode === targetBatchCode))

    evidence.entryAttempts.push({
      source: 'batch-execution',
      pageIndex,
      rowCount: rows.length,
      visibleFillButtons,
      candidateCount: candidates.length,
      targetBatchCode,
      statuses: rows.reduce((acc, row) => {
        const key = String(row.status)
        acc[key] = (acc[key] || 0) + 1
        return acc
      }, {})
    })

    for (let index = 0; index < visibleFillButtons; index += 1) {
      if (tried >= config.maxCandidatesToTry) break
      tried += 1
      const button = page.getByRole('button', { name: '去填写' }).nth(index)
      try {
        await button.scrollIntoViewIfNeeded()
        const routeContext = await clickAndWaitForForm(page, async () => button.click(), evidence, 'batch-execution')
        return { routeContext, selected: candidates[index]?.row || { fillButtonIndex: index } }
      } catch (error) {
        evidence.entryErrors.push({
          source: 'batch-execution',
          fillButtonIndex: index,
          selected: candidates[index]?.row,
          message: error.message
        })
        await openBatchExecutionList(page, evidence)
      }
    }

    if (pageIndex >= maxPages) break
    summary = await goToNextPage(page, evidence, 'batch-execution')
    if (!summary) break
  }
  return null
}

async function openRealWritableTask(page, evidence, targetBatchCode) {
  const fromWorkTask = await tryOpenFromWorkTaskBoard(page, evidence, targetBatchCode)
  if (fromWorkTask) return fromWorkTask

  const fromBatchList = await tryOpenFromBatchExecutionList(page, evidence, targetBatchCode)
  if (fromBatchList) return fromBatchList

  throw failFast('no_writer_writable_edhr_fill_task_found', {
    targetBatchCode,
    entryAttempts: evidence.entryAttempts,
    entryErrors: evidence.entryErrors
  })
}

async function collectVisibleErrors(page) {
  const alerts = await page.locator('.el-alert:visible').evaluateAll((nodes) =>
    nodes.slice(0, 10).map((node) => node.textContent.replace(/\s+/g, ' ').trim()).filter(Boolean)
  ).catch(() => [])
  const messages = await page.locator('.el-message:visible').evaluateAll((nodes) =>
    nodes.slice(0, 10).map((node) => node.textContent.replace(/\s+/g, ' ').trim()).filter(Boolean)
  ).catch(() => [])
  return { alerts, messages }
}

async function collectFormSummary(page) {
  const url = new URL(page.url())
  const labels = await page.locator('.edhr-fill-workspace__assist-label').evaluateAll((nodes) =>
    nodes.slice(0, 20).map((node) => node.textContent.trim()).filter(Boolean)
  )
  const helpTexts = await page.locator('.edhr-fill-workspace__assist-help').evaluateAll((nodes) =>
    nodes.slice(0, 20).map((node) => node.textContent.trim()).filter(Boolean)
  )
  return {
    url: redactUrl(page.url()),
    query: {
      id: url.searchParams.get('id'),
      executionId: url.searchParams.get('executionId'),
      workTaskId: url.searchParams.get('workTaskId'),
      batchExecutionId: url.searchParams.get('batchExecutionId'),
      batchTaskId: url.searchParams.get('batchTaskId')
    },
    assistCardCount: await page.locator('.edhr-fill-workspace__assist-card').count(),
    labels,
    helpTexts,
    saveButtonVisible: await page.getByRole('button', { name: '保存' }).first().isVisible().catch(() => false),
    submitButtonVisible: await page.getByRole('button', { name: '提交执行' }).first().isVisible().catch(() => false)
  }
}

async function isElementDisabled(locator) {
  if (!(await locator.count())) return true
  if (await locator.isDisabled().catch(() => false)) return true
  const className = await locator.evaluate((node) => node.className || '').catch(() => '')
  return String(className).includes('is-disabled')
}

async function selectFirstVisibleOption(page, selectRoot) {
  await selectRoot.click()
  const option = page.locator('.el-select-dropdown__item:visible:not(.is-disabled)').first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  const label = (await option.innerText()).trim()
  await option.click()
  return label
}

async function fillOneAssistCard(page, card, index, marker) {
  const label = (await card.locator('.edhr-fill-workspace__assist-label').first().innerText().catch(() => '')).trim()
  const helpText = (await card.locator('.edhr-fill-workspace__assist-help').first().innerText().catch(() => '')).trim()
  const required = await card.getByText('必填', { exact: true }).first().isVisible().catch(() => false)
  const result = { index, label, helpText, required, changed: false, skipped: false, kind: 'unknown' }

  const selectRoot = card.locator('.el-select:not(.is-disabled)').first()
  if ((await selectRoot.count()) && (await selectRoot.isVisible().catch(() => false))) {
    result.kind = 'select'
    result.selected = await selectFirstVisibleOption(page, selectRoot)
    result.changed = true
    return result
  }

  const checkboxRoot = card.locator('.el-checkbox:not(.is-disabled)').first()
  const checkbox = checkboxRoot.locator('input[type="checkbox"]').first()
  if ((await checkboxRoot.count()) && (await checkboxRoot.isVisible().catch(() => false)) && !(await checkbox.isDisabled().catch(() => true))) {
    result.kind = 'checkbox'
    const before = await checkbox.isChecked().catch(() => false)
    await checkboxRoot.click({ force: true })
    result.changed = true
    result.value = !before
    return result
  }

  const numberInput = card.locator('.el-input-number input:visible, input[role="spinbutton"]:visible').first()
  if ((await numberInput.count()) && !(await isElementDisabled(numberInput))) {
    result.kind = 'number'
    result.previousValue = await numberInput.inputValue().catch(() => '')
    result.value = String(100 + index)
    await numberInput.fill(result.value)
    result.changed = true
    return result
  }

  const dateInput = card.locator('.el-date-editor input:visible').first()
  if ((await dateInput.count()) && !(await isElementDisabled(dateInput))) {
    result.kind = 'date'
    result.previousValue = await dateInput.inputValue().catch(() => '')
    result.value = '2026-07-20'
    await dateInput.fill(result.value)
    await dateInput.press('Enter').catch(() => undefined)
    result.changed = true
    return result
  }

  const textarea = card.locator('textarea:visible:not([disabled]):not([readonly])').first()
  if ((await textarea.count()) && !(await isElementDisabled(textarea))) {
    result.kind = 'textarea'
    result.previousValue = await textarea.inputValue().catch(() => '')
    result.value = `${marker}-T${index}`
    await textarea.fill(result.value)
    result.changed = true
    return result
  }

  const input = card.locator('input.el-input__inner:visible:not([disabled]):not([readonly])').first()
  if ((await input.count()) && !(await isElementDisabled(input))) {
    result.kind = 'input'
    result.previousValue = await input.inputValue().catch(() => '')
    result.value = `${marker}-F${index}`
    await input.fill(result.value)
    result.changed = true
    return result
  }

  result.skipped = true
  result.blocker = required ? 'required_field_has_no_supported_frontend_control' : 'no_supported_frontend_control'
  return result
}

async function fillAssistFields(page, evidence) {
  const cards = page.locator('.edhr-fill-workspace__assist-card')
  const count = await cards.count()
  if (count <= 0) {
    throw failFast('admin_writable_form_has_no_assist_fields', await collectFormSummary(page))
  }

  const marker = `ADMIN-ASSIST-E2E-${Date.now()}`
  const results = []
  for (let index = 0; index < count; index += 1) {
    const card = cards.nth(index)
    await card.scrollIntoViewIfNeeded()
    const result = await fillOneAssistCard(page, card, index, marker)
    results.push(result)
    await page.waitForTimeout(80)
  }

  evidence.fieldFillResults = results
  const changedCount = results.filter((item) => item.changed).length
  if (changedCount <= 0) {
    throw failFast('admin_assist_mode_no_supported_editable_field_changed', { results })
  }
  const requiredUnsupported = results.filter(
    (item) => item.required && item.skipped && item.blocker === 'required_field_has_no_supported_frontend_control'
  )
  if (requiredUnsupported.length > 0) {
    throw failFast('admin_assist_mode_required_fields_need_unsupported_control', { requiredUnsupported })
  }
  return { marker, changedCount, results }
}

async function chooseReasonCategory(page, dialog) {
  const reasonSelect = dialog.locator('.el-form-item').filter({ hasText: '原因分类' }).locator('.el-select').first()
  await reasonSelect.click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: '操作录入' }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
}

async function saveFieldChanges(page, evidence) {
  const saveButton = page.getByRole('button', { name: '保存' }).first()
  await saveButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await saveButton.isDisabled().catch(() => false), false, 'assist_save_button_should_be_enabled_after_edit')

  await saveButton.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await chooseReasonCategory(page, dialog)
  await dialog.getByPlaceholder('请输入字段变更原因').fill('辅助模式写入型 E2E 验证保存字段变更')
  await dialog.getByPlaceholder('请输入当前账号密码').fill(config.signaturePassword)

  const reloadPromise = waitForNextExecutionGet(page, evidence, 'after-save-reload')
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-execution/field-audit/save-changes') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await dialog.locator('button').filter({ hasText: '确 认 保 存' }).first().click()
  const response = await responsePromise
  assert.ok(response.ok(), `field_audit_save_http_failed:${response.status()}`)
  const payload = await safeJson(response)
  assert.ok([0, 200, undefined].includes(payload.code), `field_audit_save_payload_failed:${JSON.stringify(payload)}`)
  evidence.saveResponse = {
    httpStatus: response.status(),
    ...summarizePayload(payload)
  }
  assert.equal(
    evidence.saveResponse.hashVerificationStatus,
    'VALID',
    `field_audit_hash_verification_failed:${JSON.stringify(evidence.saveResponse)}`
  )
  await page.getByText('字段变更已写入不可篡改审计链', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 30000
  }).catch(() => undefined)
  await page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).waitFor({
    state: 'hidden',
    timeout: 30000
  }).catch(() => undefined)
  evidence.afterSaveExecution = await reloadPromise
}

async function submitExecution(page, evidence) {
  const submitButton = page.getByRole('button', { name: '提交执行' }).first()
  await submitButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await submitButton.isDisabled().catch(() => false), false, 'assist_submit_button_should_be_enabled_after_save')

  await submitButton.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '提交 eDHR 执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByPlaceholder('请输入当前账号密码').fill(config.signaturePassword)
  await dialog.getByPlaceholder('请输入提交备注（可选）').fill('辅助模式写入型 E2E 验证提交执行')

  const reloadPromise = waitForNextExecutionGet(page, evidence, 'after-submit-reload')
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-execution/submit') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await dialog.locator('button').filter({ hasText: '确 认 提 交' }).first().click()
  const response = await responsePromise
  assert.ok(response.ok(), `submit_execution_http_failed:${response.status()}`)
  const payload = await safeJson(response)
  assert.ok([0, 200, undefined].includes(payload.code), `submit_execution_payload_failed:${JSON.stringify(payload)}`)
  evidence.submitResponse = {
    httpStatus: response.status(),
    ...summarizePayload(payload)
  }
  await page.getByText(/eDHR 执行已提交|eDHR 执行已重新提交/).first().waitFor({
    state: 'visible',
    timeout: 30000
  }).catch(() => undefined)
  evidence.afterSubmitExecution = await reloadPromise
}

async function waitForNextExecutionGet(page, evidence, label) {
  return page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/batch-record-execution/get') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    .then(async (response) => {
      assert.ok(response.ok(), `${label}_execution_get_http_failed:${response.status()}`)
      const payload = await safeJson(response)
      const summary = {
        label,
        httpStatus: response.status(),
        ...summarizePayload(payload)
      }
      evidence.executionGetResponses.push(summary)
      return summary
    })
    .catch((error) => {
      evidence.executionGetWaitErrors.push({ label, message: error.message })
      return null
    })
}

async function verifyWritableFormLoaded(page, evidence) {
  await page.locator('.edhr-fill-workspace__assist-panel').first().waitFor({ state: 'visible', timeout: 60000 })
  await page
    .locator('.edhr-fill-workspace__view-actions button.is-active')
    .filter({ hasText: '填写辅助模式' })
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
  await page.getByRole('button', { name: '原表模式' }).first().waitFor({ state: 'visible', timeout: 30000 })
  const summary = await collectFormSummary(page)
  evidence.initialFormSummary = summary
  if (!summary.query.workTaskId) {
    throw failFast('writer_writable_form_missing_work_task_id', summary)
  }
  if (!summary.saveButtonVisible || !summary.submitButtonVisible) {
    throw failFast('writer_writable_form_missing_save_or_submit_button', summary)
  }
}

async function main() {
  fs.mkdirSync(artifactDir, { recursive: true })
  ensureConfig()
  const adminCredentials = {
    username: config.adminUsername,
    password: config.adminPassword
  }
  const adminOfficialPreflight = runOfficialLoginPreflight(
    adminCredentials,
    batchExecutionPath,
    '打开/创建',
    'admin'
  )
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const evidence = {
    status: 'RUNNING',
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    adminUsername: config.adminUsername,
    batchCode: config.batchCode,
    workOrderCode: config.workOrderCode,
    authorizedWriteScope: 'assist-mode-save-and-submit-only',
    adminOfficialPreflight,
    entryAttempts: [],
    entryErrors: [],
    workTaskPageResponses: [],
    batchPageResponses: [],
    taskOpenResponses: [],
    executionGetResponses: [],
    executionGetWaitErrors: [],
    allowExistingE2eBatch: config.allowExistingE2eBatch
  }
  let page
  let adminContext
  let writerContext

  try {
    adminContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    page = await adminContext.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page, adminCredentials)
    let batchContext
    try {
      batchContext = await createDedicatedBatch(page, evidence)
    } catch (error) {
      if (!config.allowExistingE2eBatch || !String(error.message).startsWith('dedicated_batch_')) {
        throw error
      }
      evidence.dedicatedBatchCreationBlocked = {
        message: error.message,
        details: error.details
      }
      batchContext = await selectExistingE2eWritableBatch(page, evidence)
    }
    const { assignedWriter } = batchContext
    const targetBatchCode = batchContext.batch?.batchCode || config.batchCode
    evidence.targetBatchCode = targetBatchCode
    const writerUsername = await discoverUsernameByUserId(page, evidence, assignedWriter)
    await adminContext.close()
    adminContext = undefined
    page = undefined

    const writerCredentials = {
      username: writerUsername,
      password: config.writerPassword
    }
    evidence.writerOfficialPreflight = runOfficialLoginPreflight(
      writerCredentials,
      workTaskPath,
      '任务类型',
      'writer'
    )

    writerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    page = await writerContext.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page, writerCredentials)
    const initialGetPromise = waitForNextExecutionGet(page, evidence, 'initial-form-load')
    const opened = await openRealWritableTask(page, evidence, targetBatchCode)
    evidence.opened = opened
    assert.equal(
      Number(opened.routeContext.workTaskId),
      Number(assignedWriter.activeWorkTaskId),
      'writer_opened_unexpected_work_task'
    )
    assert.equal(
      Number(opened.routeContext.batchTaskId),
      Number(assignedWriter.batchTaskId),
      'writer_opened_unexpected_batch_task'
    )

    const initialGet = await initialGetPromise
    evidence.initialExecution = initialGet
    await verifyWritableFormLoaded(page, evidence)
    await fillAssistFields(page, evidence)
    await saveFieldChanges(page, evidence)
    await submitExecution(page, evidence)

    const finalSummary = await collectFormSummary(page)
    evidence.finalFormSummary = finalSummary
    evidence.visibleErrors = await collectVisibleErrors(page)
    await page.screenshot({ path: screenshotFile, fullPage: true })

    const afterSubmit = evidence.executionGetResponses.find((item) => item.label === 'after-submit-reload')
    if (!afterSubmit) {
      throw failFast('writer_assist_submit_missing_final_execution_readback', {
        executionGetWaitErrors: evidence.executionGetWaitErrors
      })
    }
    if (afterSubmit.status === 0) {
      throw failFast('writer_assist_submit_did_not_leave_draft_status', { afterSubmit })
    }
    evidence.status = 'PASS'
    evidence.screenshot = screenshotFile
    fs.writeFileSync(resultFile, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.log(JSON.stringify(evidence, null, 2))
  } catch (error) {
    if (page) {
      await page.screenshot({ path: failureScreenshotFile, fullPage: true }).catch(() => undefined)
      evidence.visibleErrors = await collectVisibleErrors(page).catch(() => undefined)
      evidence.failureUrl = redactUrl(page.url())
    }
    evidence.status =
      /^(no_writer_|no_existing_|dedicated_batch_|assigned_writer_|configured_writer_|official_writer_)/.test(error.message)
        ? 'BLOCKED'
        : 'FAIL'
    evidence.error = {
      message: error.message,
      details: error.details,
      stack: error.stack
    }
    evidence.failureScreenshot = fs.existsSync(failureScreenshotFile) ? failureScreenshotFile : undefined
    fs.writeFileSync(resultFile, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.error(JSON.stringify(evidence, null, 2))
    process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
