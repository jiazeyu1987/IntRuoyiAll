const { chromium } = require('playwright')

function requireTrimmedEnv(name) {
  const value = process.env[name]
  if (!value || !value.trim()) {
    throw new Error(`${name} is required for yudao admin runtime-control readonly E2E`)
  }
  return value.trim().replace(/\/+$/, '')
}

function optionalEnv(name, defaultValue) {
  const value = process.env[name]
  return value && value.trim() ? value.trim() : defaultValue
}

const BASE_URL = requireTrimmedEnv('RUNTIME_CONTROL_E2E_BASE_URL')
const VERIFY_TENANT = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_TENANT', '芋道源码')
const VERIFY_USERNAME = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_USERNAME', 'admin')
const VERIFY_PASSWORD = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_PASSWORD', 'admin123')

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

function unwrapResponse(payload) {
  if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')) {
    return payload.data
  }
  return payload
}

function pageList(payload) {
  const data = unwrapResponse(payload)
  if (Array.isArray(data)) return data
  if (data && Array.isArray(data.list)) return data.list
  if (data && Array.isArray(data.records)) return data.records
  return []
}

function runtimeControlPath(url) {
  const basePath = '/admin-api/infra/runtime-control'
  try {
    const pathname = new URL(url).pathname
    if (!pathname.startsWith(basePath)) return ''
    return pathname.slice(basePath.length) || '/'
  } catch (error) {
    const index = url.indexOf(basePath)
    return index >= 0 ? url.slice(index + basePath.length).split('?')[0] : ''
  }
}

function collectRuntimeActivity(page) {
  const responses = []
  const writeRequests = []
  page.on('request', (request) => {
    const runtimePath = runtimeControlPath(request.url())
    if (runtimePath && request.method() !== 'GET') {
      writeRequests.push(`${request.method()} ${runtimePath}`)
    }
  })
  page.on('response', async (response) => {
    const runtimePath = runtimeControlPath(response.url())
    if (!runtimePath) return
    let body
    try {
      body = await response.json()
    } catch (error) {
      body = { parseError: error.message }
    }
    responses.push({
      status: response.status(),
      runtimePath,
      body,
      data: unwrapResponse(body),
      at: Date.now()
    })
  })
  return { responses, writeRequests }
}

function findResponse(responses, matcher, minAt = 0) {
  for (let index = responses.length - 1; index >= 0; index -= 1) {
    const response = responses[index]
    if (response.at >= minAt && matcher(response)) {
      return response
    }
  }
  return undefined
}

async function waitForRuntimeResponse(responses, matcher, label, timeoutMs = 20000, minAt = 0) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    const match = findResponse(responses, matcher, minAt)
    if (match) return match
    await new Promise((resolve) => setTimeout(resolve, 150))
  }
  throw new Error(`Timed out waiting for runtime-control response ${label}`)
}

async function waitForResponseData(responses, path, timeoutMs = 20000, minAt = 0) {
  return waitForRuntimeResponse(
    responses,
    (response) => response.runtimePath === path,
    path,
    timeoutMs,
    minAt
  )
}

function assertOkResponse(response, label) {
  assert(response.status >= 200 && response.status < 300, `${label} HTTP status ${response.status}`)
  if (response.body && typeof response.body.code === 'number') {
    assert(response.body.code === 0, `${label} business code ${response.body.code}: ${response.body.msg || ''}`)
  }
}

async function fillFirstVisible(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${locator}`)
}

async function fillFirstVisibleIfPresent(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return true
    }
  }
  return false
}

async function selectTenant(page, tenantName) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    return false
  }
  await tenantSelect.click()
  const input = page.locator('.login-form .el-select__input').first()
  await input.fill(tenantName)
  await page.keyboard.press('Enter')
  return true
}

async function loginRuntimeControl(page) {
  await page.goto(`${BASE_URL}/login?redirect=/infra/monitors/runtime-control`, {
    waitUntil: 'domcontentloaded'
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)

  if (page.url().includes('/login')) {
    const tenantSelected = await selectTenant(page, VERIFY_TENANT)
    if (!tenantSelected) {
      await fillFirstVisibleIfPresent(page.locator('input[placeholder="请输入租户名称"]'), VERIFY_TENANT)
    }
    await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), VERIFY_USERNAME)
    await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), VERIFY_PASSWORD)
    await page.locator('button:has-text("登录")').first().click()
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  }

  if (!page.url().includes('/infra/monitors/runtime-control')) {
    await page.goto(`${BASE_URL}/infra/monitors/runtime-control`, { waitUntil: 'domcontentloaded' })
  }

  await page.waitForSelector('text=运行控制台', { timeout: 30000 })
  await page.locator('text=探针状态').waitFor({ state: 'visible', timeout: 30000 })
}

function card(page, title) {
  return page.locator('.ops-card').filter({ hasText: title }).first()
}

async function assertVisible(locator, label, timeout = 10000) {
  try {
    await locator.waitFor({ state: 'visible', timeout })
  } catch (error) {
    throw new Error(`${label} should be visible. ${error.message}`)
  }
}

async function assertNotVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    if (await locator.nth(index).isVisible()) {
      throw new Error(`${label} should not be visible`)
    }
  }
}

async function openOperationDialog(page, buttonName, dialogTitle) {
  const startedAt = Date.now()
  const button = page.getByRole('button', { name: buttonName }).first()
  await assertVisible(button, `${buttonName} operation button`)
  await button.click()
  const dialog = page.locator('.el-dialog').filter({ hasText: dialogTitle }).first()
  await assertVisible(dialog, `${dialogTitle} dialog`)
  return { dialog, startedAt }
}

async function closeDialog(dialog) {
  await dialog.getByRole('button', { name: '取消' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 10000 })
}

async function verifyYudaoAdminReadonly(page, responses, writeRequests) {
  await loginRuntimeControl(page)

  for (const title of ['探针状态', '日志与磁盘风险']) {
    await assertVisible(card(page, title).locator(`text=${title}`).first(), `YUDAO admin readonly ${title}`)
  }
  await assertVisible(page.getByRole('button', { name: '事故闭环' }), 'YUDAO admin readonly incident entry')

  const overviewResponse = await waitForResponseData(responses, '/overview', 90000)
  assertOkResponse(overviewResponse, 'YUDAO admin readonly overview')
  const operationsResponse = await waitForResponseData(responses, '/operations', 30000)
  assertOkResponse(operationsResponse, 'YUDAO admin readonly operations')

  const ownerResponse = await waitForResponseData(responses, '/owner-matrix', 30000)
  assertOkResponse(ownerResponse, 'AC-02 YUDAO admin owner matrix')
  assert(Array.isArray(ownerResponse.data), 'AC-02 owner matrix response must be an array')
  console.log(`AC-02 ADMIN_READONLY_PASS ownerRows=${ownerResponse.data.length}`)

  const rollbackResponse = await waitForResponseData(responses, '/rollback-candidates', 30000)
  assertOkResponse(rollbackResponse, 'AC-04 YUDAO admin rollback candidates')
  const rollbackCandidates = Array.isArray(rollbackResponse.data) ? rollbackResponse.data : []
  assert(rollbackCandidates.length > 0, 'AC-04 rollback candidates must contain real rows')
  const rollbackDialog = await openOperationDialog(page, '回滚版本', '回滚版本')
  await assertVisible(rollbackDialog.dialog.locator('.candidate-picker').first(), 'AC-04 rollback candidate picker')
  await assertNotVisible(
    rollbackDialog.dialog.locator('input[placeholder="例如 20260524_035800"]'),
    'AC-04 manual image tag input'
  )
  await closeDialog(rollbackDialog.dialog)
  console.log(`AC-04 ADMIN_READONLY_PASS rollbackCandidates=${rollbackCandidates.length}`)

  const restoreResponse = await waitForResponseData(responses, '/restore-candidates', 30000)
  assertOkResponse(restoreResponse, 'AC-05 YUDAO admin restore candidates')
  const restoreCandidates = Array.isArray(restoreResponse.data) ? restoreResponse.data : []
  assert(restoreCandidates.length > 0, 'AC-05 restore candidates must contain real rows')
  const restoreDialog = await openOperationDialog(page, '恢复数据', '恢复数据')
  await assertVisible(restoreDialog.dialog.locator('.candidate-picker').first(), 'AC-05 restore candidate picker')
  await assertNotVisible(
    restoreDialog.dialog.locator('input[placeholder="例如 20260524_174058"]'),
    'AC-05 manual backup input'
  )
  await closeDialog(restoreDialog.dialog)
  console.log(`AC-05 ADMIN_READONLY_PASS restoreCandidates=${restoreCandidates.length}`)

  const probesResponse = await waitForResponseData(responses, '/probes/latest', 30000)
  assertOkResponse(probesResponse, 'AC-08 YUDAO admin latest probes')
  const probes = probesResponse.data?.probes || []
  assert(probes.length > 0, 'AC-08 latest probes must expose readonly rows')
  await assertVisible(card(page, '探针状态').getByRole('button', { name: '执行探针' }), 'AC-08 probe run entry')
  console.log(`AC-08 ADMIN_READONLY_PASS probes=${probes.length}`)

  const capacityResponse = await waitForResponseData(responses, '/capacity/status', 30000)
  assertOkResponse(capacityResponse, 'AC-09 YUDAO admin capacity status')
  assert(capacityResponse.data?.disk || capacityResponse.data?.logDirectory, 'AC-09 must include disk or logDirectory metric')
  await assertVisible(card(page, '日志与磁盘风险').locator('text=磁盘').first(), 'AC-09 disk metric')
  console.log(`AC-09 ADMIN_READONLY_PASS status=${capacityResponse.data?.status}`)

  const incidentsResponse = await waitForResponseData(responses, '/incidents/page', 30000)
  assertOkResponse(incidentsResponse, 'AC-11 YUDAO admin incidents page')
  await page.getByRole('button', { name: '事故闭环' }).click()
  const drawer = page.locator('.ops-incident-drawer')
  await assertVisible(drawer, 'AC-11 incident drawer')
  await assertVisible(drawer.getByRole('button', { name: '新建事故' }), 'AC-11 incident create entry')
  console.log(`AC-11 ADMIN_READONLY_PASS incidents=${pageList(incidentsResponse.body).length}`)

  assert(
    writeRequests.length === 0,
    `YUDAO admin readonly must not call write runtime-control endpoints: ${writeRequests.join(', ')}`
  )
  console.log('YUDAO_ADMIN_READONLY_PASS')
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const page = await context.newPage()
    const { responses, writeRequests } = collectRuntimeActivity(page)
    await verifyYudaoAdminReadonly(page, responses, writeRequests)
    await context.close()
    console.log('PASS: yudao/admin readonly runtime-control E2E covers AC-01 through AC-11')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
