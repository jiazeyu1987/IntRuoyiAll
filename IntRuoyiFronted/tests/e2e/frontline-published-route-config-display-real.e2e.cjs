const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const TASK_ID = '20260909-route-published-config-frontline-e2e'
const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const OUTPUT_DIR = path.resolve(WORKSPACE_ROOT, 'doc', 'tasks', TASK_ID, 'e2e-artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'frontline-published-route-config-display-result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'frontline-published-route-config-display.png')
const PRODUCTION_ROUTE = '/mes/pro/feedback/edhr-batch-production-fill'
const WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {}
  const result = {}
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const separator = trimmed.indexOf('=')
    if (separator <= 0) continue
    const key = trimmed.slice(0, separator).trim()
    let value = trimmed.slice(separator + 1).trim()
    if (
      (value.startsWith("'") && value.endsWith("'")) ||
      (value.startsWith('"') && value.endsWith('"'))
    ) {
      value = value.slice(1, -1)
    }
    result[key] = value
  }
  return result
}

const env = {
  ...readEnvFile(path.join(FRONTEND_ROOT, '.env')),
  ...readEnvFile(path.join(FRONTEND_ROOT, '.env.local')),
  ...process.env
}

const BASE_URL = String(
  env.FRONTLINE_PUBLISHED_CONFIG_E2E_BASE_URL || 'http://127.0.0.1:8081'
).replace(/\/+$/, '')
const BACKEND_URL = String(
  env.FRONTLINE_PUBLISHED_CONFIG_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
).replace(/\/+$/, '')
const CREDENTIALS = {
  tenant: env.FRONTLINE_PUBLISHED_CONFIG_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: env.FRONTLINE_PUBLISHED_CONFIG_E2E_USERNAME,
  password: env.FRONTLINE_PUBLISHED_CONFIG_E2E_PASSWORD
}
const TARGET = {
  workOrderCode: env.FRONTLINE_PUBLISHED_CONFIG_E2E_WORK_ORDER_CODE,
  processName: env.FRONTLINE_PUBLISHED_CONFIG_E2E_PROCESS_NAME,
  deviceCode: env.FRONTLINE_PUBLISHED_CONFIG_E2E_DEVICE_CODE
}
const BROWSER_EXECUTABLE =
  env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

class E2EBlockedError extends Error {
  constructor(message, details = {}) {
    super(message)
    this.name = 'E2EBlockedError'
    this.details = details
  }
}

function sanitizeUrl(url) {
  return String(url || '').replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')
}

function writeResult(result) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function requireConfig() {
  const missing = []
  if (!CREDENTIALS.tenant) missing.push('FRONTLINE_PUBLISHED_CONFIG_E2E_TENANT')
  if (!CREDENTIALS.username) missing.push('FRONTLINE_PUBLISHED_CONFIG_E2E_USERNAME')
  if (!CREDENTIALS.password) missing.push('FRONTLINE_PUBLISHED_CONFIG_E2E_PASSWORD')
  if (!fs.existsSync(BROWSER_EXECUTABLE)) {
    missing.push('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
  }
  if (missing.length) {
    throw new E2EBlockedError('缺少真实页面 E2E 前置配置', { missing })
  }
}

async function assertRuntimeReady() {
  const frontend = await fetch(BASE_URL)
  assert.equal(frontend.status, 200, `前端不可访问：${BASE_URL}`)
  const backend = await fetch(`${BACKEND_URL}/actuator/health`)
  assert.equal(backend.status, 200, `后端健康检查不可访问：${BACKEND_URL}`)
  const body = await backend.json()
  assert.equal(body.status, 'UP', `后端健康状态不是 UP：${JSON.stringify(body)}`)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && (await item.isEnabled().catch(() => false))) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`缺少可用的${label}输入框`)
}

async function selectLoginTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) === 0 || !(await tenantInput.isVisible().catch(() => false))) {
    return
  }
  await tenantInput.click()
  await tenantInput.fill(CREDENTIALS.tenant)
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: CREDENTIALS.tenant })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(PRODUCTION_ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })
  assert.equal(
    await form
      .locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder*="验证码"]:visible')
      .count(),
    0,
    '登录页启用了验证码，无法执行无人值守真实页面 E2E'
  )

  await selectLoginTenant(page, form)
  await fillFirstVisible(
    form.locator(
      'input[placeholder*="账号"], input[placeholder*="用户名"], input.el-input__inner:not([role="combobox"]):not([type="password"])'
    ),
    CREDENTIALS.username,
    '用户名'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), CREDENTIALS.password, '密码')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  assert.ok(loginResponse.ok(), `登录 HTTP 失败：${loginResponse.status()}`)
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `登录业务失败：${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function parseBusinessResponse(response, label) {
  assert.ok(response.ok(), `${label} HTTP 失败：${response.status()}`)
  const body = await response.json()
  assert.ok([0, 200].includes(body.code), `${label} 业务失败：${body.msg || body.code}`)
  return body.data
}

async function openProductionPage(page) {
  const activeOrdersPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/feedback/frontline/device-account/active-orders') &&
      response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await page.goto(`${BASE_URL}${PRODUCTION_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  const activeOrders = await parseBusinessResponse(await activeOrdersPromise, '一线生产活跃订单')
  if (!Array.isArray(activeOrders) || activeOrders.length === 0) {
    throw new E2EBlockedError('当前账号没有可进入一线生产的活跃订单')
  }
  const screen = page.locator('[data-frontline-production-operator]').first()
  await screen.waitFor({ state: 'visible', timeout: 90000 })
  return { screen, activeOrders }
}

async function openPicker(screen, cardSelector) {
  const card = screen.locator(cardSelector).first()
  await card.waitFor({ state: 'visible', timeout: 30000 })
  await card.click()
  const picker = screen.locator('.frontline-picker__card:visible').first()
  await picker.waitFor({ state: 'visible', timeout: 30000 })
  return picker
}

async function selectTargetOrder(page, screen, activeOrders) {
  if (!TARGET.workOrderCode) return activeOrders[0]
  const targetOrder = activeOrders.find((order) => order.workOrderCode === TARGET.workOrderCode)
  if (!targetOrder) {
    throw new E2EBlockedError(`当前活跃订单列表找不到目标生产工单：${TARGET.workOrderCode}`)
  }
  const picker = await openPicker(screen, '[data-frontline-production-active-order-card]')
  const input = picker.locator('input[aria-label="输入订单号筛选活跃订单"]').first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill(TARGET.workOrderCode)
  const processResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/feedback/frontline/device-account/active-order/processes') &&
      response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await picker.locator('button').filter({ hasText: TARGET.workOrderCode }).first().click()
  await parseBusinessResponse(await processResponsePromise, '目标活跃订单工序')
  await picker.waitFor({ state: 'hidden', timeout: 30000 })
  return targetOrder
}

async function selectTargetProcess(page, screen) {
  if (!TARGET.processName) return
  const picker = await openPicker(screen, '[data-frontline-production-process-nav-card]')
  const runtimeConfigPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/feedback/frontline/device-account/runtime-config') &&
      response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await picker.locator('.frontline-picker__options button').filter({ hasText: TARGET.processName }).first().click()
  await picker.waitFor({ state: 'hidden', timeout: 30000 })
  return parseBusinessResponse(await runtimeConfigPromise, '目标工序运行态配置')
}

function firstDeviceWithParameters(runtimeConfig) {
  const devices = Array.isArray(runtimeConfig?.devices) ? runtimeConfig.devices : []
  const candidates = devices.filter(
    (device) =>
      Number(device.deviceId || 0) > 0 &&
      Array.isArray(device.parameters) &&
      device.parameters.length > 0
  )
  if (TARGET.deviceCode) {
    const target = candidates.find((device) => device.deviceCode === TARGET.deviceCode)
    if (!target) {
      throw new E2EBlockedError(`运行态配置找不到目标设备或设备参数：${TARGET.deviceCode}`)
    }
    return target
  }
  if (!candidates.length) {
    throw new E2EBlockedError('当前工序运行态配置没有设备参数，无法验证发布配置在一线生产显示')
  }
  return candidates[0]
}

function formatTargetRange(parameter) {
  if (parameter.valueType === 'TEXT_STANDARD') return ''
  const lower = parameter.lowerLimit ?? ''
  const upper = parameter.upperLimit ?? ''
  const unit = parameter.unit || ''
  if (lower !== '' && upper !== '') return `目标范围：${lower} - ${upper}${unit}`.trim()
  if (lower !== '') return `目标范围：≥ ${lower}${unit}`.trim()
  if (upper !== '') return `目标范围：≤ ${upper}${unit}`.trim()
  return ''
}

function normalizeDisplayText(value) {
  return String(value || '')
    .replace(/\s+/g, '')
    .replace(/（/g, '(')
    .replace(/）/g, ')')
}

async function assertDeviceCardSelection(screen, runtimeConfig, targetDevice) {
  const cards = screen.locator('.frontline-production-device-card')
  const cardCount = await cards.count()
  assert.equal(cardCount, runtimeConfig.devices.length, '设备卡数量必须等于发布后运行态设备数量')
  const firstCard = cards.first()
  await firstCard.waitFor({ state: 'visible', timeout: 30000 })
  assert.match(await firstCard.getAttribute('class'), /\bactive\b/, '进入工序后必须默认选中第一个设备')
  await firstCard.getByText(runtimeConfig.devices[0].deviceCode, { exact: true }).waitFor({
    state: 'visible',
    timeout: 30000
  })
  if (targetDevice.deviceCode !== runtimeConfig.devices[0].deviceCode) {
    await cards.filter({ hasText: targetDevice.deviceCode }).first().locator('.device-tab').click()
  }
  await cards.filter({ hasText: targetDevice.deviceCode }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
}

async function assertSelectOptions(page, row, parameter) {
  const select = row.locator('[data-frontline-select-parameter]').first()
  await select.waitFor({ state: 'visible', timeout: 30000 })
  await select.click()
  for (const option of parameter.optionValues || []) {
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: option }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
  }
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: '其他' }).first()
    .waitFor({ state: 'visible', timeout: 30000 })
  await page.keyboard.press('Escape')
}

async function assertParameterRows(page, screen, device) {
  for (const parameter of device.parameters) {
    if (!parameter.parameterCode) continue
    const label = parameter.parameterName || parameter.parameterCode
    const row = screen.locator('.frontline-production-device-param').filter({ hasText: label }).first()
    await row.waitFor({ state: 'visible', timeout: 30000 })
    const targetRange = formatTargetRange(parameter)
    if (targetRange) {
      const rowText = await row.innerText()
      assert.ok(
        normalizeDisplayText(rowText).includes(normalizeDisplayText(targetRange)),
        `${label} 目标范围未按发布配置显示：expected=${targetRange}; actual=${rowText}`
      )
    }
    if (parameter.valueType === 'SELECT') {
      await assertSelectOptions(page, row, parameter)
      const valueText = await row.innerText()
      const expected = String(parameter.defaultText || '').trim()
      if (expected) assert.ok(valueText.includes(expected), `${label} 下拉默认值未显示：${expected}`)
      continue
    }
    if (parameter.valueType === 'BOOLEAN') {
      await row.locator('[data-frontline-boolean-parameter]').waitFor({ state: 'visible', timeout: 30000 })
      continue
    }
    const input = row.locator('input.device-value').first()
    await input.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(await input.isEnabled(), true, `${label} 参数输入框必须可编辑`)
    const expectedValue = String(parameter.defaultValue ?? parameter.defaultText ?? '').trim()
    if (expectedValue) {
      assert.equal((await input.inputValue()).trim(), expectedValue, `${label} 默认值应来自发布后配置`)
    }
  }
}

async function runScenario() {
  requireConfig()
  await assertRuntimeReady()
  const browser = await chromium.launch({
    headless: env.FRONTLINE_PUBLISHED_CONFIG_E2E_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({ viewport: { width: 1720, height: 980 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const evidence = {
    status: 'FAIL',
    generatedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    backendUrl: BACKEND_URL,
    identity: `${CREDENTIALS.tenant}/${CREDENTIALS.username}`,
    target: TARGET,
    naturalRequests: [],
    mesWriteRequests: [],
    consoleErrors: [],
    pageErrors: []
  }
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && WRITE_METHODS.has(request.method())) {
      evidence.mesWriteRequests.push({ method: request.method(), url: sanitizeUrl(request.url()) })
    }
  })
  page.on('response', (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/mes/pro/feedback/frontline/device-account/')) return
    evidence.naturalRequests.push({
      method: response.request().method(),
      status: response.status(),
      url: sanitizeUrl(url)
    })
  })
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))

  try {
    await login(page)
    const runtimeConfigPromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/feedback/frontline/device-account/runtime-config') &&
        response.request().method() === 'GET',
      { timeout: 90000 }
    )
    const { screen, activeOrders } = await openProductionPage(page)
    const activeOrder = await selectTargetOrder(page, screen, activeOrders)
    const explicitRuntimeConfig = await selectTargetProcess(page, screen)
    const runtimeConfig = explicitRuntimeConfig ||
      await parseBusinessResponse(await runtimeConfigPromise, '默认工序运行态配置')
    const targetDevice = firstDeviceWithParameters(runtimeConfig)
    await assertDeviceCardSelection(screen, runtimeConfig, targetDevice)
    await assertParameterRows(page, screen, targetDevice)
    assert.deepEqual(evidence.mesWriteRequests, [], '只读显示验收不得发出 MES 写请求')
    assert.deepEqual(evidence.pageErrors, [], `页面错误：${evidence.pageErrors.join('\n')}`)
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    evidence.status = 'PASS'
    evidence.activeOrder = {
      workOrderId: activeOrder.workOrderId,
      workOrderCode: activeOrder.workOrderCode,
      routeId: activeOrder.routeId
    }
    evidence.runtimeConfig = {
      routeId: runtimeConfig.routeId,
      routeProcessId: runtimeConfig.routeProcessId,
      processId: runtimeConfig.processId,
      deviceCount: runtimeConfig.devices.length,
      materialCount: runtimeConfig.materials?.length || 0,
      parameterSnapshotState: runtimeConfig.productionSubmitContext?.parameterSnapshotState,
      targetDeviceCode: targetDevice.deviceCode,
      targetParameterCount: targetDevice.parameters.length
    }
    evidence.screenshot = SCREENSHOT_PATH
    writeResult(evidence)
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
  }
}

runScenario().catch((error) => {
  const status = error instanceof E2EBlockedError ? 'BLOCKED' : 'FAIL'
  writeResult({
    status,
    generatedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    backendUrl: BACKEND_URL,
    identity: `${CREDENTIALS.tenant || '<missing>'}/${CREDENTIALS.username || '<missing>'}`,
    target: TARGET,
    reason: error.message,
    details: error.details,
    error: error.stack || String(error)
  })
  console.error(error.stack || String(error))
  process.exitCode = status === 'BLOCKED' ? 2 : 1
})
