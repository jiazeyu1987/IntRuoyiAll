const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260807-team-leader-loss-maintenance-dialog'
const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const RESULT_DIR = path.resolve(
  process.env.TEAM_LEADER_LOSS_DIALOG_ARTIFACT_DIR
    || path.join(WORKSPACE_ROOT, 'doc', 'tasks', TASK_ID, 'evidence', 'real-browser')
)
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')
const TEAM_LEADER_ROUTE = '/mes/pro/process-pool/production-leader'
const DEFAULT_DATA_PREFIX = 'LMD20260807'
const LOSS_REASON_ENDPOINT = '/mes/pro/process-pool/team-leader/loss-reasons'
const PROCESS_CONFIG_ENDPOINT = '/mes/pro/process-pool/team-leader/process-config/list'

const REQUIRED_ENV = [
  ['TEAM_LEADER_LOSS_DIALOG_BASE_URL', '真实前端入口，例如 http://127.0.0.1:8081。'],
  ['TEAM_LEADER_LOSS_DIALOG_BACKEND_URL', '真实后端入口，例如 http://127.0.0.1:48081。'],
  ['TEAM_LEADER_LOSS_DIALOG_TENANT', '可写测试租户，禁止使用生产基线租户。'],
  ['TEAM_LEADER_LOSS_DIALOG_USERNAME', '拥有生产组长工序配置入口权限的测试账号。'],
  ['TEAM_LEADER_LOSS_DIALOG_PASSWORD', '测试账号密码，只能通过进程环境注入。'],
  ['TEAM_LEADER_LOSS_DIALOG_ROUTE_PROCESS_ID', '生产组长正式授权的路线工序 ID。'],
  ['TEAM_LEADER_LOSS_DIALOG_PROCESS_TEXT', '目标工序行唯一可见的路线/工序业务标识。']
]

function envValue(key) {
  return String(process.env[key] || '').trim()
}

function numberEnv(key) {
  const value = Number(envValue(key))
  return Number.isFinite(value) && value > 0 ? value : undefined
}

function sanitizeUrl(value) {
  return value.replace(/\/+$/, '')
}

function collectConfig() {
  const timestamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
  const dataPrefix = envValue('TEAM_LEADER_LOSS_DIALOG_DATA_PREFIX') || DEFAULT_DATA_PREFIX
  const initialName = `${dataPrefix}-${timestamp}-初始损耗`
  const config = {
    baseUrl: sanitizeUrl(envValue('TEAM_LEADER_LOSS_DIALOG_BASE_URL')),
    backendUrl: sanitizeUrl(envValue('TEAM_LEADER_LOSS_DIALOG_BACKEND_URL')),
    tenant: envValue('TEAM_LEADER_LOSS_DIALOG_TENANT'),
    username: envValue('TEAM_LEADER_LOSS_DIALOG_USERNAME'),
    password: envValue('TEAM_LEADER_LOSS_DIALOG_PASSWORD'),
    routeProcessId: numberEnv('TEAM_LEADER_LOSS_DIALOG_ROUTE_PROCESS_ID'),
    processText: envValue('TEAM_LEADER_LOSS_DIALOG_PROCESS_TEXT'),
    dataPrefix,
    initialName,
    cancelledName: `${initialName}-取消修改`,
    updatedName: `${initialName}-已修改`,
    updatedRemark: `${dataPrefix}-${timestamp}-真实页面维护说明`,
    reenabledRemark: `${dataPrefix}-${timestamp}-重新启用后清理`,
    headed: envValue('TEAM_LEADER_LOSS_DIALOG_HEADED') === '1'
  }
  config.missing = collectMissingConfig(config)
  return config
}

function collectMissingConfig(config) {
  const missing = []
  for (const [key, description] of REQUIRED_ENV) {
    if (!envValue(key)) {
      missing.push({ key, description })
    }
  }
  if (envValue('TEAM_LEADER_LOSS_DIALOG_ROUTE_PROCESS_ID')
    && !numberEnv('TEAM_LEADER_LOSS_DIALOG_ROUTE_PROCESS_ID')) {
    missing.push({
      key: 'TEAM_LEADER_LOSS_DIALOG_ROUTE_PROCESS_ID',
      description: '必须是大于 0 的正式路线工序 ID，不能使用占位值。'
    })
  }
  if (config.tenant && /芋道源码|prod|production|yudao/i.test(config.tenant)) {
    missing.push({
      key: 'TEAM_LEADER_LOSS_DIALOG_TENANT',
      description: '写入型 E2E 禁止使用生产基线租户。'
    })
  }
  if (config.username && config.username.toLowerCase() === 'admin') {
    missing.push({
      key: 'TEAM_LEADER_LOSS_DIALOG_USERNAME',
      description: '写入型 E2E 禁止使用 admin 基线账号。'
    })
  }
  if (config.dataPrefix && !config.dataPrefix.startsWith(DEFAULT_DATA_PREFIX)) {
    missing.push({
      key: 'TEAM_LEADER_LOSS_DIALOG_DATA_PREFIX',
      description: `任务自有数据前缀必须以 ${DEFAULT_DATA_PREFIX} 开头。`
    })
  }
  if (config.baseUrl && config.backendUrl && !isAllowedRuntimePair(config.baseUrl, config.backendUrl)) {
    missing.push({
      key: 'TEAM_LEADER_LOSS_DIALOG_BASE_URL/BACKEND_URL',
      description: '前后端必须是同一 int_main slot 端口对，例如 8081/48081。'
    })
  }
  const executablePath = envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
  if (executablePath && !fs.existsSync(executablePath)) {
    missing.push({
      key: 'PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH',
      description: '指定的本机 Chrome/Edge 可执行文件不存在。'
    })
  }
  return missing
}

function isAllowedRuntimePair(baseUrl, backendUrl) {
  try {
    const frontendPort = Number(new URL(baseUrl).port)
    const backendPort = Number(new URL(backendUrl).port)
    const slot = frontendPort - 8081
    return Number.isInteger(frontendPort)
      && Number.isInteger(backendPort)
      && slot >= 0
      && slot <= 19
      && backendPort === 48081 + slot
  } catch (_error) {
    return false
  }
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (_error) {
    const blocked = new Error('缺少 Playwright runtime；请先在 IntRuoyiFronted 执行 pnpm install。')
    blocked.blocked = true
    throw blocked
  }
}

function resolveLaunchOptions(config) {
  const executablePath = envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
  const launchOptions = { headless: !config.headed }
  return executablePath ? { ...launchOptions, executablePath } : launchOptions
}

function redactedConfig(config) {
  const copy = { ...config }
  delete copy.password
  delete copy.missing
  return copy
}

function writeResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(RESULT_FILE, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function captureOutcome(promise) {
  return promise.then(
    (value) => ({ status: 'fulfilled', value }),
    (reason) => ({ status: 'rejected', reason })
  )
}

async function requireOutcome(outcomePromise) {
  const outcome = await outcomePromise
  if (outcome.status === 'rejected') {
    throw outcome.reason
  }
  return outcome.value
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'networkidle' })
  const form = page.locator('.login-form').first()
  await form.waitFor({ state: 'visible', timeout: 20000 })
  await selectTenant(page, form, config.tenant)
  await fillFirstVisible(form, [
    'input[placeholder="请输入用户名"]',
    'input[placeholder*="用户名"]',
    'input[placeholder*="账号"]',
    'input.el-input__inner:not([type="password"]):not([role="combobox"])'
  ], config.username)
  await fillFirstVisible(form, [
    'input[type="password"]',
    'input[placeholder="请输入密码"]',
    'input[placeholder*="密码"]'
  ], config.password)
  const loginResponse = captureOutcome(
    waitForBusinessResponse(page, '/system/auth/login', 'POST')
  )
  const permissionResponse = captureOutcome(
    page.waitForResponse((response) =>
      response.url().includes('/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 20000 })
  )
  await form.getByRole('button', { name: '登录' }).click()
  await requireOutcome(loginResponse)
  await requireOutcome(permissionResponse)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 20000 })
  await page.waitForLoadState('networkidle')
}

async function selectTenant(page, form, tenantName) {
  const tenantInput = form.locator(
    'input[placeholder="请输入租户名称"], .el-select__input[role="combobox"]'
  ).first()
  if (!(await tenantInput.isVisible().catch(() => false))) {
    return
  }
  await tenantInput.click()
  await tenantInput.fill(tenantName)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: tenantName }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function fillFirstVisible(scope, selectors, value) {
  for (const selector of selectors) {
    const locator = scope.locator(selector).first()
    if (await locator.isVisible().catch(() => false)) {
      await locator.fill(String(value))
      return
    }
  }
  throw new Error(`找不到可填写控件：${selectors.join(', ')}`)
}

async function waitForBusinessResponse(page, fragment, method) {
  const response = await page.waitForResponse((candidate) =>
    candidate.url().includes(fragment) && candidate.request().method() === method,
  { timeout: 30000 })
  assert.equal(response.ok(), true, `${fragment} HTTP ${response.status()}`)
  const body = await response.json().catch(() => ({}))
  assert.equal(body.code, 0, `${fragment} 业务失败：${body.msg || body.message || 'unknown'}`)
  return { response, body }
}

async function openLossMaintenanceDialog(page, config) {
  const listResponse = captureOutcome(
    waitForBusinessResponse(page, PROCESS_CONFIG_ENDPOINT, 'GET')
  )
  await page.goto(`${config.baseUrl}${TEAM_LEADER_ROUTE}`, { waitUntil: 'networkidle' })
  const processConfigTab = page.locator('[data-production-leader-module-tab-process-config]').first()
  if (await processConfigTab.isVisible().catch(() => false)) {
    await processConfigTab.click()
  }
  await requireOutcome(listResponse)
  await page.locator('[data-team-leader-process-config-table]').waitFor({
    state: 'visible',
    timeout: 30000
  })
  const row = page
    .locator('[data-team-leader-process-config-table] .el-table__body-wrapper tbody tr')
    .filter({ hasText: config.processText })
    .filter({
      has: page.locator(
        `[data-team-leader-process-config-manage-loss][data-route-process-id="${config.routeProcessId}"]`
      )
    })
  assert.equal(
    await row.count(),
    1,
    `可见业务标识「${config.processText}」与 routeProcessId=${config.routeProcessId} 必须唯一定位工序行`
  )
  const action = row.locator('[data-team-leader-process-config-manage-loss]')
  assert.equal(await action.count(), 1, '目标工序行应只有一个损耗入口')
  await action.click()
  const dialog = page.locator('[data-loss-reason-maintenance-dialog]').first()
  await dialog.waitFor({ state: 'visible', timeout: 15000 })
  return dialog
}

function lossReasonRow(dialog, reasonName) {
  return dialog
    .locator('[data-loss-reason-maintenance-table] .el-table__body-wrapper tbody tr')
    .filter({ hasText: reasonName })
    .first()
}

async function assertDialogOpen(dialog, reason) {
  assert.equal(await dialog.isVisible(), true, reason)
}

async function createLossReason(page, dialog, config, dataState, steps) {
  await dialog.locator('[data-loss-reason-inline-add]').click()
  const nameInput = dialog.locator('[data-loss-reason-inline-create-name]')
  await nameInput.waitFor({ state: 'visible', timeout: 10000 })
  await nameInput.fill(config.initialName)
  const writePromise = captureOutcome(
    waitForBusinessResponse(page, LOSS_REASON_ENDPOINT, 'POST')
  )
  const reloadPromise = captureOutcome(
    waitForBusinessResponse(page, PROCESS_CONFIG_ENDPOINT, 'GET')
  )
  await dialog.locator('[data-loss-reason-inline-save-create]').click()
  const { response } = await requireOutcome(writePromise)
  dataState.created = true
  dataState.currentName = config.initialName
  const payload = response.request().postDataJSON()
  assert.deepEqual(
    Object.keys(payload).sort(),
    ['reasonName', 'routeProcessId'],
    '新增损耗只能提交 routeProcessId 和 reasonName'
  )
  assert.equal(payload.routeProcessId, config.routeProcessId)
  assert.equal(payload.reasonName, config.initialName)
  await requireOutcome(reloadPromise)
  await assertDialogOpen(dialog, '新增成功后维护弹框必须保持打开')
  await lossReasonRow(dialog, config.initialName).waitFor({ state: 'visible', timeout: 15000 })
  steps.push('通过当前工序损耗弹框新增任务自有损耗，正式 POST code=0，并刷新列表且保持弹框打开')
}

async function cancelEditWithoutWrite(page, dialog, config, steps) {
  const writes = []
  const onRequest = (request) => {
    if (request.method() === 'PUT' && request.url().includes(LOSS_REASON_ENDPOINT)) {
      writes.push(request.url())
    }
  }
  page.on('request', onRequest)
  try {
    const row = lossReasonRow(dialog, config.initialName)
    await row.locator('[data-loss-reason-inline-edit]').click()
    const input = row.locator('[data-loss-reason-inline-name]')
    await input.fill(config.cancelledName)
    await row.locator('[data-loss-reason-inline-cancel-edit]').click()
    await page.waitForTimeout(300)
    assert.deepEqual(writes, [], '取消修改不得发起 PUT 请求')
    await lossReasonRow(dialog, config.initialName).waitFor({ state: 'visible', timeout: 10000 })
    assert.equal(await dialog.getByText(config.cancelledName, { exact: true }).count(), 0)
    steps.push('取消行内修改后未发起 PUT，正式列表内容保持不变')
  } finally {
    page.off('request', onRequest)
  }
}

async function updateLossReason(page, dialog, config, dataState, steps) {
  const row = lossReasonRow(dialog, config.initialName)
  await row.locator('[data-loss-reason-inline-edit]').click()
  await row.locator('[data-loss-reason-inline-name]').fill(config.updatedName)
  await row.locator('[data-loss-reason-inline-remark]').fill(config.updatedRemark)
  const enabledSwitch = row.locator('[data-loss-reason-inline-enabled]')
  assert.equal(await enabledSwitch.getAttribute('aria-checked'), 'true')
  await enabledSwitch.click()
  assert.equal(await enabledSwitch.getAttribute('aria-checked'), 'false')
  const writePromise = captureOutcome(
    waitForBusinessResponse(page, LOSS_REASON_ENDPOINT, 'PUT')
  )
  const reloadPromise = captureOutcome(
    waitForBusinessResponse(page, PROCESS_CONFIG_ENDPOINT, 'GET')
  )
  await row.locator('[data-loss-reason-inline-save-edit]').click()
  const { response } = await requireOutcome(writePromise)
  dataState.currentName = config.updatedName
  dataState.disabled = true
  const payload = response.request().postDataJSON()
  assert.deepEqual(Object.keys(payload).sort(), ['enabled', 'reasonName', 'remark'])
  assert.equal(payload.reasonName, config.updatedName)
  assert.equal(payload.enabled, false)
  assert.equal(payload.remark, config.updatedRemark)
  await requireOutcome(reloadPromise)
  await assertDialogOpen(dialog, '修改成功后维护弹框必须保持打开')
  const updatedRow = lossReasonRow(dialog, config.updatedName)
  await updatedRow.waitFor({ state: 'visible', timeout: 15000 })
  await updatedRow.getByText('停用', { exact: true }).waitFor({ state: 'visible', timeout: 10000 })
  steps.push('行内修改描述、启用状态和维护说明，正式 PUT code=0，并刷新列表且保持弹框打开')
}

async function reenableLossReason(page, dialog, config, dataState, steps) {
  const row = lossReasonRow(dialog, config.updatedName)
  await row.locator('[data-loss-reason-inline-edit]').click()
  const enabledSwitch = row.locator('[data-loss-reason-inline-enabled]')
  assert.equal(await enabledSwitch.getAttribute('aria-checked'), 'false')
  await enabledSwitch.click()
  assert.equal(await enabledSwitch.getAttribute('aria-checked'), 'true')
  await row.locator('[data-loss-reason-inline-remark]').fill(config.reenabledRemark)
  const writePromise = captureOutcome(
    waitForBusinessResponse(page, LOSS_REASON_ENDPOINT, 'PUT')
  )
  const reloadPromise = captureOutcome(
    waitForBusinessResponse(page, PROCESS_CONFIG_ENDPOINT, 'GET')
  )
  await row.locator('[data-loss-reason-inline-save-edit]').click()
  const { response } = await requireOutcome(writePromise)
  dataState.disabled = false
  const payload = response.request().postDataJSON()
  assert.deepEqual(Object.keys(payload).sort(), ['enabled', 'reasonName', 'remark'])
  assert.equal(payload.reasonName, config.updatedName)
  assert.equal(payload.enabled, true)
  assert.equal(payload.remark, config.reenabledRemark)
  await requireOutcome(reloadPromise)
  const enabledRow = lossReasonRow(dialog, config.updatedName)
  await enabledRow.getByText('启用', { exact: true }).waitFor({ state: 'visible', timeout: 10000 })
  steps.push('通过同一行内编辑器重新启用任务损耗，保证后续删除验证从启用状态开始')
}

async function cancelDeleteWithoutWrite(page, dialog, config, steps) {
  const writes = []
  const onRequest = (request) => {
    if (request.method() === 'DELETE' && request.url().includes(LOSS_REASON_ENDPOINT)) {
      writes.push(request.url())
    }
  }
  page.on('request', onRequest)
  try {
    const row = lossReasonRow(dialog, config.updatedName)
    await row.locator('[data-loss-reason-inline-delete]').click()
    const messageBox = page.locator('.el-message-box:visible').first()
    await messageBox.waitFor({ state: 'visible', timeout: 10000 })
    await messageBox.getByRole('button', { name: '取消', exact: true }).click()
    await messageBox.waitFor({ state: 'hidden', timeout: 10000 })
    await page.waitForTimeout(300)
    assert.deepEqual(writes, [], '取消删除不得发起 DELETE 请求')
    await assertDialogOpen(dialog, '取消删除后维护弹框必须保持打开')
    steps.push('取消删除确认后未发起 DELETE，维护弹框保持打开')
  } finally {
    page.off('request', onRequest)
  }
}

async function deleteLossReason(page, dialog, config, dataState, steps) {
  const row = lossReasonRow(dialog, config.updatedName)
  await row.locator('[data-loss-reason-inline-delete]').click()
  const messageBox = page.locator('.el-message-box:visible').first()
  await messageBox.waitFor({ state: 'visible', timeout: 10000 })
  const writePromise = captureOutcome(
    waitForBusinessResponse(page, LOSS_REASON_ENDPOINT, 'DELETE')
  )
  const reloadPromise = captureOutcome(
    waitForBusinessResponse(page, PROCESS_CONFIG_ENDPOINT, 'GET')
  )
  await messageBox.getByRole('button', { name: '确定', exact: true }).click()
  await requireOutcome(writePromise)
  dataState.disabled = true
  await requireOutcome(reloadPromise)
  await assertDialogOpen(dialog, '删除成功后维护弹框必须保持打开')
  const disabledRow = lossReasonRow(dialog, config.updatedName)
  await disabledRow.waitFor({ state: 'visible', timeout: 15000 })
  await disabledRow.getByText('停用', { exact: true }).waitFor({ state: 'visible', timeout: 10000 })
  steps.push('确认删除调用正式 DELETE code=0，刷新后以停用状态保留历史损耗且弹框保持打开')
}

async function cleanupTaskLossReason(page, dialog, config, dataState, steps) {
  if (!dataState.created || dataState.disabled) {
    return { attempted: false, completed: dataState.disabled }
  }
  let activeDialog = dialog
  if (!activeDialog || !(await activeDialog.isVisible().catch(() => false))) {
    activeDialog = await openLossMaintenanceDialog(page, config)
  }
  const openMessageBox = page.locator('.el-message-box:visible').first()
  if (await openMessageBox.isVisible().catch(() => false)) {
    await openMessageBox.getByRole('button', { name: '取消', exact: true }).click()
    await openMessageBox.waitFor({ state: 'hidden', timeout: 10000 })
  }
  const cancelEdit = activeDialog.locator('[data-loss-reason-inline-cancel-edit]:visible').first()
  if (await cancelEdit.isVisible().catch(() => false)) {
    await cancelEdit.click()
  }
  const candidateNames = [dataState.currentName, config.updatedName, config.initialName]
    .filter((value, index, values) => value && values.indexOf(value) === index)
  let row
  for (const name of candidateNames) {
    const candidate = lossReasonRow(activeDialog, name)
    if (await candidate.isVisible().catch(() => false)) {
      row = candidate
      break
    }
  }
  assert.ok(row, `清理失败：找不到任务损耗 ${candidateNames.join(' / ')}`)
  await row.locator('[data-loss-reason-inline-delete]').click()
  const messageBox = page.locator('.el-message-box:visible').first()
  await messageBox.waitFor({ state: 'visible', timeout: 10000 })
  const writePromise = captureOutcome(
    waitForBusinessResponse(page, LOSS_REASON_ENDPOINT, 'DELETE')
  )
  const reloadPromise = captureOutcome(
    waitForBusinessResponse(page, PROCESS_CONFIG_ENDPOINT, 'GET')
  )
  await messageBox.getByRole('button', { name: '确定', exact: true }).click()
  await requireOutcome(writePromise)
  dataState.disabled = true
  await requireOutcome(reloadPromise)
  steps.push('异常路径通过同一真实维护弹框停用已创建的任务损耗')
  return { attempted: true, completed: true }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    writeResult({
      status: 'BLOCKED',
      reason: '缺少真实写入型 E2E 前置条件。',
      config: redactedConfig(config),
      missing: config.missing,
      steps: []
    })
    process.exitCode = 2
    return
  }

  const steps = []
  const pageErrors = []
  const consoleErrors = []
  const targetNetworkFailures = []
  const dataState = { created: false, disabled: false, currentName: undefined }
  let browser
  let context
  let page
  let dialog
  let tracingStopped = false
  try {
    const { chromium } = loadPlaywright()
    browser = await chromium.launch(resolveLaunchOptions(config))
    context = await browser.newContext({ viewport: { width: 1440, height: 980 } })
    await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
    page = await context.newPage()
    page.on('pageerror', (error) => pageErrors.push(error.message || String(error)))
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })
    page.on('requestfailed', (request) => {
      if (request.url().startsWith(config.backendUrl) || request.url().startsWith(config.baseUrl)) {
        targetNetworkFailures.push(
          `${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`.trim()
        )
      }
    })

    await login(page, config)
    steps.push('测试生产组长通过真实登录页进入系统')
    dialog = await openLossMaintenanceDialog(page, config)
    steps.push('按正式 routeProcessId 定位唯一损耗入口并打开当前工序损耗列表')
    await createLossReason(page, dialog, config, dataState, steps)
    await cancelEditWithoutWrite(page, dialog, config, steps)
    await updateLossReason(page, dialog, config, dataState, steps)
    await reenableLossReason(page, dialog, config, dataState, steps)
    await cancelDeleteWithoutWrite(page, dialog, config, steps)
    await deleteLossReason(page, dialog, config, dataState, steps)

    assert.deepEqual(pageErrors, [], '真实页面不得出现 pageerror')
    assert.deepEqual(consoleErrors, [], '真实页面控制台不得出现 error')
    assert.deepEqual(targetNetworkFailures, [], '目标前后端链路不得出现 requestfailed')
    ensureDir(RESULT_DIR)
    const screenshot = path.join(RESULT_DIR, 'loss-maintenance-dialog-final.png')
    const trace = path.join(RESULT_DIR, 'loss-maintenance-dialog-trace.zip')
    await page.screenshot({ path: screenshot, fullPage: true })
    await context.tracing.stop({ path: trace })
    tracingStopped = true
    writeResult({
      status: 'PASS',
      reason: '生产组长损耗统一维护弹框真实 E2E 通过。',
      config: redactedConfig(config),
      steps,
      pageErrors,
      consoleErrors,
      targetNetworkFailures,
      dataState,
      screenshot,
      trace
    })
  } catch (error) {
    let cleanup
    let cleanupError
    if (page && dataState.created && !dataState.disabled) {
      try {
        cleanup = await cleanupTaskLossReason(page, dialog, config, dataState, steps)
      } catch (cleanupFailure) {
        cleanupError = cleanupFailure.message || String(cleanupFailure)
      }
    }
    let trace
    if (context && !tracingStopped) {
      ensureDir(RESULT_DIR)
      trace = path.join(RESULT_DIR, 'loss-maintenance-dialog-failure-trace.zip')
      await context.tracing.stop({ path: trace }).catch(() => undefined)
    }
    writeResult({
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.message || String(error),
      config: redactedConfig(config),
      steps,
      pageErrors,
      consoleErrors,
      targetNetworkFailures,
      dataState,
      cleanup,
      cleanupError,
      residualEnabledData: dataState.created && !dataState.disabled,
      trace,
      error: {
        name: error.name || 'Error',
        message: error.message || String(error),
        stack: error.stack
      }
    })
    process.exitCode = error.blocked ? 2 : 1
  } finally {
    if (browser) {
      await browser.close()
    }
  }
}

void main()
