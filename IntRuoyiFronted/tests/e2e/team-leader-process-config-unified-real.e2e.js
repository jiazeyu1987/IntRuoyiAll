const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260805-production-leader-process-config-unification'
const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const RESULT_DIR = path.resolve(
  process.env.TEAM_LEADER_PROCESS_CONFIG_ARTIFACT_DIR
    || path.join(WORKSPACE_ROOT, 'doc', 'tasks', TASK_ID, 'evidence', 'real-browser')
)
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')
const TEAM_LEADER_ROUTE = '/mes/pro/process-pool/team-leader'
const DEFAULT_DATA_PREFIX = 'PCU20260805'

const REQUIRED_ENV = [
  ['TEAM_LEADER_PROCESS_CONFIG_BASE_URL', '真实前端入口，例如 http://127.0.0.1:8085。'],
  ['TEAM_LEADER_PROCESS_CONFIG_BACKEND_URL', '真实后端入口，例如 http://127.0.0.1:48085。'],
  ['TEAM_LEADER_PROCESS_CONFIG_TENANT', '可写测试租户，禁止使用 admin/生产基线租户。'],
  ['TEAM_LEADER_PROCESS_CONFIG_USERNAME', '拥有生产组长工序配置入口权限的测试账号。'],
  ['TEAM_LEADER_PROCESS_CONFIG_PASSWORD', '测试账号密码，只能通过进程环境注入。'],
  ['TEAM_LEADER_PROCESS_CONFIG_ROUTE_PROCESS_ID', '生产组长正式授权的路线工序 ID。'],
  ['TEAM_LEADER_PROCESS_CONFIG_DEVICE_ID', '当前组长可选设备 ID。'],
  ['TEAM_LEADER_PROCESS_CONFIG_DEVICE_TEXT', '设备下拉中可见的设备编码或名称。'],
  ['TEAM_LEADER_PROCESS_CONFIG_FRONTLINE_URL', '真实一线生产填写页完整 URL，用于产生正式 PRODUCTION_SUBMIT。'],
  ['TEAM_LEADER_PROCESS_CONFIG_FRONTLINE_PARAMETER_SELECTOR', '一线填写页设备参数输入框 CSS 选择器。'],
  ['TEAM_LEADER_PROCESS_CONFIG_EXPECTED_AVERAGE', '完成正式提交后统一表应展示的参数平均值。'],
  ['TEAM_LEADER_PROCESS_CONFIG_EXPECTED_SAMPLE_COUNT', '完成正式提交后统一表应展示的样本数。']
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
  const dataPrefix = envValue('TEAM_LEADER_PROCESS_CONFIG_DATA_PREFIX') || DEFAULT_DATA_PREFIX
  const parameterCode = envValue('TEAM_LEADER_PROCESS_CONFIG_PARAMETER_CODE') || `${dataPrefix}-PRESSURE-${timestamp}`
  const config = {
    baseUrl: sanitizeUrl(envValue('TEAM_LEADER_PROCESS_CONFIG_BASE_URL')),
    backendUrl: sanitizeUrl(envValue('TEAM_LEADER_PROCESS_CONFIG_BACKEND_URL')),
    tenant: envValue('TEAM_LEADER_PROCESS_CONFIG_TENANT'),
    username: envValue('TEAM_LEADER_PROCESS_CONFIG_USERNAME'),
    password: envValue('TEAM_LEADER_PROCESS_CONFIG_PASSWORD'),
    dataPrefix,
    routeProcessId: numberEnv('TEAM_LEADER_PROCESS_CONFIG_ROUTE_PROCESS_ID'),
    deviceId: numberEnv('TEAM_LEADER_PROCESS_CONFIG_DEVICE_ID'),
    deviceText: envValue('TEAM_LEADER_PROCESS_CONFIG_DEVICE_TEXT'),
    processText: envValue('TEAM_LEADER_PROCESS_CONFIG_PROCESS_TEXT'),
    parameterCode,
    parameterName: envValue('TEAM_LEADER_PROCESS_CONFIG_PARAMETER_NAME') || `${dataPrefix}压力`,
    parameterUnit: envValue('TEAM_LEADER_PROCESS_CONFIG_PARAMETER_UNIT') || 'MPa',
    lowerLimit: Number(envValue('TEAM_LEADER_PROCESS_CONFIG_LOWER_LIMIT') || 10),
    targetValue: Number(envValue('TEAM_LEADER_PROCESS_CONFIG_TARGET_VALUE') || 15),
    upperLimit: Number(envValue('TEAM_LEADER_PROCESS_CONFIG_UPPER_LIMIT') || 20),
    updatedTargetValue: Number(envValue('TEAM_LEADER_PROCESS_CONFIG_UPDATED_TARGET_VALUE') || 16),
    invalidLowerLimit: Number(envValue('TEAM_LEADER_PROCESS_CONFIG_INVALID_LOWER_LIMIT') || 30),
    lossReasonCode: envValue('TEAM_LEADER_PROCESS_CONFIG_LOSS_REASON_CODE') || `${dataPrefix}-LOSS-${timestamp}`,
    lossReasonName: envValue('TEAM_LEADER_PROCESS_CONFIG_LOSS_REASON_NAME') || `${dataPrefix}真实损耗`,
    frontlineUrl: envValue('TEAM_LEADER_PROCESS_CONFIG_FRONTLINE_URL'),
    frontlineParameterSelector: envValue('TEAM_LEADER_PROCESS_CONFIG_FRONTLINE_PARAMETER_SELECTOR'),
    frontlineSubmitSelector: envValue('TEAM_LEADER_PROCESS_CONFIG_FRONTLINE_SUBMIT_SELECTOR') || 'button:has-text("提交")',
    frontlineSubmitEndpoint: envValue('TEAM_LEADER_PROCESS_CONFIG_FRONTLINE_SUBMIT_ENDPOINT') || '/mes/pro/feedback/frontline/submit',
    frontlineParameterValue: Number(envValue('TEAM_LEADER_PROCESS_CONFIG_FRONTLINE_PARAMETER_VALUE') || 18),
    expectedAverage: Number(envValue('TEAM_LEADER_PROCESS_CONFIG_EXPECTED_AVERAGE')),
    expectedSampleCount: Number(envValue('TEAM_LEADER_PROCESS_CONFIG_EXPECTED_SAMPLE_COUNT')),
    noSampleParameterCode: envValue('TEAM_LEADER_PROCESS_CONFIG_NO_SAMPLE_PARAMETER_CODE'),
    headed: envValue('TEAM_LEADER_PROCESS_CONFIG_HEADED') === '1'
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
  for (const key of [
    'TEAM_LEADER_PROCESS_CONFIG_ROUTE_PROCESS_ID',
    'TEAM_LEADER_PROCESS_CONFIG_DEVICE_ID',
    'TEAM_LEADER_PROCESS_CONFIG_EXPECTED_SAMPLE_COUNT'
  ]) {
    if (!numberEnv(key)) {
      missing.push({ key, description: '必须是大于 0 的真实数字值，不能使用占位值。' })
    }
  }
  if (!Number.isFinite(config.lowerLimit) || !Number.isFinite(config.targetValue) || !Number.isFinite(config.upperLimit)) {
    missing.push({ key: 'TEAM_LEADER_PROCESS_CONFIG_LIMITS', description: '参数下限、目标值和上限必须是数字。' })
  }
  if (Number.isFinite(config.lowerLimit) && Number.isFinite(config.targetValue) && Number.isFinite(config.upperLimit)
    && !(config.lowerLimit <= config.targetValue && config.targetValue <= config.upperLimit)) {
    missing.push({ key: 'TEAM_LEADER_PROCESS_CONFIG_LIMITS', description: '合法参数标准必须满足 lower <= target <= upper。' })
  }
  if (config.tenant && /芋道源码|prod|production|yudao/i.test(config.tenant)) {
    missing.push({ key: 'TEAM_LEADER_PROCESS_CONFIG_TENANT', description: '写入型 E2E 禁止使用 admin/生产基线租户。' })
  }
  if (config.username && config.username.toLowerCase() === 'admin') {
    missing.push({ key: 'TEAM_LEADER_PROCESS_CONFIG_USERNAME', description: '写入型 E2E 禁止使用 admin 基线账号。' })
  }
  if (config.dataPrefix && !config.dataPrefix.startsWith(DEFAULT_DATA_PREFIX)) {
    missing.push({ key: 'TEAM_LEADER_PROCESS_CONFIG_DATA_PREFIX', description: `任务自有数据前缀必须以 ${DEFAULT_DATA_PREFIX} 开头。` })
  }
  if (config.baseUrl && config.backendUrl && !isAllowedRuntimePair(config.baseUrl, config.backendUrl)) {
    missing.push({ key: 'TEAM_LEADER_PROCESS_CONFIG_BASE_URL/BACKEND_URL', description: '前后端必须是同一 int_main slot 端口对，例如 8085/48085。' })
  }
  if (envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH') && !fs.existsSync(envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH'))) {
    missing.push({ key: 'PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH', description: '指定的本机 Chrome/Edge 可执行文件不存在。' })
  }
  return missing
}

function isAllowedRuntimePair(baseUrl, backendUrl) {
  const frontendPort = Number(new URL(baseUrl).port)
  const backendPort = Number(new URL(backendUrl).port)
  if (!Number.isInteger(frontendPort) || !Number.isInteger(backendPort)) {
    return false
  }
  const slot = frontendPort - 8081
  return slot >= 0 && slot <= 19 && backendPort === 48081 + slot
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
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
  const loginResponse = waitForBusinessResponse(page, '/system/auth/login', 'POST')
  await form.getByRole('button', { name: '登录' }).click()
  await loginResponse
  await page.waitForResponse((response) =>
    response.url().includes('/system/auth/get-permission-info') && response.status() === 200,
  { timeout: 20000 })
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 20000 })
  await page.waitForLoadState('networkidle')
}

async function selectTenant(page, form, tenantName) {
  const tenantInput = form.locator('input[placeholder="请输入租户名称"], .el-select__input[role="combobox"]').first()
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

async function fillFormItem(scope, label, value) {
  const item = scope.locator('.el-form-item', { hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 15000 })
  const input = item.locator('input:visible, textarea:visible').first()
  await input.fill(String(value))
}

async function setInputNumber(page, selector, value) {
  const input = page.locator(`${selector} input:visible`).first()
  await input.waitFor({ state: 'visible', timeout: 15000 })
  await input.fill(String(value))
}

async function waitForBusinessResponse(page, fragment, method) {
  const response = await page.waitForResponse((candidate) =>
    candidate.url().includes(fragment) && candidate.request().method() === method,
  { timeout: 30000 })
  assert.equal(response.ok(), true, `${fragment} HTTP ${response.status()}`)
  const body = await response.json().catch(() => ({}))
  assert.equal(body.code, 0, `${fragment} 业务失败：${body.msg || body.message || 'unknown'}`)
  return body
}

async function openProcessConfig(page, config) {
  const listResponse = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/process-pool/team-leader/process-config/list')
      && response.request().method() === 'GET',
  { timeout: 30000 })
  await page.goto(`${config.baseUrl}${TEAM_LEADER_ROUTE}`, { waitUntil: 'networkidle' })
  const processConfigTab = page.locator('[data-production-leader-module-tab-process-config]').first()
  if (await processConfigTab.isVisible().catch(() => false)) {
    await processConfigTab.click()
  }
  await page.locator('[data-team-leader-process-config-table]').waitFor({ state: 'visible', timeout: 30000 })
  const body = await listResponse
  return Array.isArray(body.data) ? body.data : []
}

function findRouteProcessIndex(rows, config) {
  const index = rows.findIndex((row) => Number(row.routeProcessId) === config.routeProcessId)
  assert.notEqual(index, -1, `统一表正式数据缺少 routeProcessId=${config.routeProcessId}`)
  return index
}

async function clickRowAction(page, rowIndex, selector) {
  const action = page.locator(selector).nth(rowIndex)
  await action.waitFor({ state: 'visible', timeout: 15000 })
  await action.click()
}

async function saveLossReason(page, config, rowIndex, steps) {
  await clickRowAction(page, rowIndex, '[data-team-leader-process-config-add-loss]')
  const dialog = page.locator('[data-loss-reason-edit-dialog]').first()
  await dialog.waitFor({ state: 'visible', timeout: 15000 })
  await fillFormItem(dialog, '原因编码', config.lossReasonCode)
  await fillFormItem(dialog, '原因名称', config.lossReasonName)
  const responsePromise = waitForBusinessResponse(page, '/mes/pro/process-pool/team-leader/loss-reasons', 'POST')
  await dialog.getByRole('button', { name: '保存损耗原因' }).click()
  await responsePromise
  await page.locator('[data-loss-reason-edit-dialog]').waitFor({ state: 'hidden', timeout: 15000 })
  steps.push('从统一表当前行新增损耗原因并保存后刷新正式行')
}

async function saveDeviceBinding(page, config, rowIndex, steps) {
  await clickRowAction(page, rowIndex, '[data-team-leader-process-config-bind-device]')
  const dialog = page.locator('[data-team-leader-process-config-device-dialog]').first()
  await dialog.waitFor({ state: 'visible', timeout: 15000 })
  await dialog.locator('[data-team-leader-process-config-device-select]').click()
  await page.locator('.el-select-dropdown__item:visible', { hasText: config.deviceText }).last().click()
  const responsePromise = waitForBusinessResponse(page, '/mes/pro/process-pool/team-leader/process-config/device-binding/save', 'POST')
  await dialog.getByRole('button', { name: '保存设备映射' }).click()
  await responsePromise
  await page.locator('[data-team-leader-process-config-device-dialog]').waitFor({ state: 'hidden', timeout: 15000 })
  steps.push('从统一表当前行通过设备候选列表保存设备映射')
}

async function saveParameter(page, config, rowIndex, targetValue, steps, actionLabel) {
  await clickRowAction(page, rowIndex, '[data-team-leader-process-config-edit-parameter]')
  const dialog = page.locator('[data-team-leader-process-config-parameter-dialog]').first()
  await dialog.waitFor({ state: 'visible', timeout: 15000 })
  await fillFormItem(dialog, '参数编码', config.parameterCode)
  await fillFormItem(dialog, '参数名称', config.parameterName)
  await fillFormItem(dialog, '单位', config.parameterUnit)
  await setInputNumber(page, '[data-team-leader-process-config-lower-limit]', config.lowerLimit)
  await setInputNumber(page, '[data-team-leader-process-config-target-value]', targetValue)
  await setInputNumber(page, '[data-team-leader-process-config-upper-limit]', config.upperLimit)
  await page.locator('[data-team-leader-process-config-average-readonly]').waitFor({ state: 'visible', timeout: 15000 })
  const responsePromise = waitForBusinessResponse(page, '/mes/pro/process-pool/team-leader/process-config/device-parameter-rule/save', 'POST')
  await dialog.getByRole('button', { name: '保存参数标准' }).click()
  await responsePromise
  await page.locator('[data-team-leader-process-config-parameter-dialog]').waitFor({ state: 'hidden', timeout: 15000 })
  steps.push(actionLabel)
}

async function assertInvalidRangeBlocked(page, config, rowIndex, steps) {
  await clickRowAction(page, rowIndex, '[data-team-leader-process-config-edit-parameter]')
  const dialog = page.locator('[data-team-leader-process-config-parameter-dialog]').first()
  await dialog.waitFor({ state: 'visible', timeout: 15000 })
  await fillFormItem(dialog, '参数编码', config.parameterCode)
  await setInputNumber(page, '[data-team-leader-process-config-lower-limit]', config.invalidLowerLimit)
  await setInputNumber(page, '[data-team-leader-process-config-target-value]', config.targetValue)
  await setInputNumber(page, '[data-team-leader-process-config-upper-limit]', config.upperLimit)
  const writes = []
  const onRequest = (request) => {
    if (request.url().includes('/mes/pro/process-pool/team-leader/process-config/device-parameter-rule/save')) {
      writes.push(request.url())
    }
  }
  page.on('request', onRequest)
  await dialog.getByRole('button', { name: '保存参数标准' }).click()
  await page.locator('.el-message:visible', { hasText: '参数区间必须满足下限 <= 目标值 <= 上限' }).waitFor({ timeout: 10000 })
  page.off('request', onRequest)
  assert.equal(writes.length, 0, '非法区间不得发起参数保存请求')
  await dialog.getByRole('button', { name: '取消' }).click()
  steps.push('非法参数区间在前端被可见错误拦截且未发起写请求')
}

async function submitFrontlineSample(page, config, steps) {
  await page.goto(config.frontlineUrl, { waitUntil: 'networkidle' })
  const parameterInput = page.locator(config.frontlineParameterSelector).first()
  await parameterInput.waitFor({ state: 'visible', timeout: 30000 })
  await parameterInput.fill(String(config.frontlineParameterValue))
  const responsePromise = waitForBusinessResponse(page, config.frontlineSubmitEndpoint, 'POST')
  await page.locator(config.frontlineSubmitSelector).first().click()
  await responsePromise
  steps.push('通过真实一线生产填写页提交 equipmentParameters 数值样本')
}

async function assertAverage(page, config, steps) {
  const rows = await openProcessConfig(page, config)
  const row = rows.find((item) => Number(item.routeProcessId) === config.routeProcessId)
  assert.ok(row, `平均值复验缺少 routeProcessId=${config.routeProcessId}`)
  const parameters = (row.devices || []).flatMap((device) => device.parameters || [])
  const parameter = parameters.find((item) => item.parameterCode === config.parameterCode)
  assert.ok(parameter, `平均值复验缺少参数 ${config.parameterCode}`)
  assert.equal(Number(parameter.sampleCount), config.expectedSampleCount, '样本数不符合预期')
  assert.equal(Number(parameter.actualAverage), config.expectedAverage, '实际平均值不符合预期')
  steps.push('统一表重新读取正式行后显示预期平均值和样本数')
  if (config.noSampleParameterCode) {
    const noSample = parameters.find((item) => item.parameterCode === config.noSampleParameterCode)
    assert.ok(noSample, `无样本复验缺少参数 ${config.noSampleParameterCode}`)
    assert.equal(noSample.actualAverage ?? null, null, '无样本参数 actualAverage 必须为空')
    assert.equal(Number(noSample.sampleCount), 0, '无样本参数 sampleCount 必须为 0')
    steps.push('另一个无样本参数保持 actualAverage=null/sampleCount=0 语义')
  }
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
  let browser
  let context
  try {
    const { chromium } = loadPlaywright()
    browser = await chromium.launch(resolveLaunchOptions(config))
    context = await browser.newContext({ viewport: { width: 1440, height: 980 } })
    await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
    const page = await context.newPage()
    page.on('pageerror', (error) => pageErrors.push(error.message || String(error)))
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })
    page.on('requestfailed', (request) => {
      if (request.url().startsWith(config.backendUrl) || request.url().startsWith(config.baseUrl)) {
        targetNetworkFailures.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`.trim())
      }
    })

    await login(page, config)
    steps.push('测试生产组长通过真实登录页进入系统')
    let rows = await openProcessConfig(page, config)
    let rowIndex = findRouteProcessIndex(rows, config)
    await saveLossReason(page, config, rowIndex, steps)
    rows = await openProcessConfig(page, config)
    rowIndex = findRouteProcessIndex(rows, config)
    await saveDeviceBinding(page, config, rowIndex, steps)
    rows = await openProcessConfig(page, config)
    rowIndex = findRouteProcessIndex(rows, config)
    await saveParameter(page, config, rowIndex, config.targetValue, steps, '新增合法参数标准并保存后刷新正式行')
    rows = await openProcessConfig(page, config)
    rowIndex = findRouteProcessIndex(rows, config)
    await saveParameter(page, config, rowIndex, config.updatedTargetValue, steps, '相同参数编码再次保存走更新路径')
    rows = await openProcessConfig(page, config)
    rowIndex = findRouteProcessIndex(rows, config)
    await assertInvalidRangeBlocked(page, config, rowIndex, steps)
    await submitFrontlineSample(page, config, steps)
    await assertAverage(page, config, steps)

    assert.deepEqual(pageErrors, [], '真实页面不得出现 pageerror')
    assert.deepEqual(targetNetworkFailures, [], '目标链路不得出现 requestfailed')
    ensureDir(RESULT_DIR)
    const screenshot = path.join(RESULT_DIR, 'T18-unified-config.png')
    const trace = path.join(RESULT_DIR, 'T18-unified-config-trace.zip')
    await page.screenshot({ path: screenshot, fullPage: true })
    await context.tracing.stop({ path: trace })
    writeResult({
      status: 'PASS',
      reason: '生产组长工序配置统一表真实 E2E 通过。',
      config: redactedConfig(config),
      steps,
      pageErrors,
      consoleErrors,
      targetNetworkFailures,
      screenshot,
      trace
    })
  } catch (error) {
    let trace
    if (context) {
      ensureDir(RESULT_DIR)
      trace = path.join(RESULT_DIR, 'team-leader-process-config-failure-trace.zip')
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
