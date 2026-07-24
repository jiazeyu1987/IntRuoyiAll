const { chromium } = require('playwright')

const REQUIRED_BASE_URL_MESSAGE = 'RUNTIME_CONTROL_E2E_BASE_URL is required'
const REQUIRED_ACTION_ORIGIN_MESSAGE = 'RUNTIME_CONTROL_E2E_ACTION_ORIGIN is required'

function requireTrimmedEnv(name, message) {
  const value = process.env[name]
  if (!value || !value.trim()) {
    throw new Error(`${message}; ${name} must point to the current runtime-control target.`)
  }
  return value.trim().replace(/\/+$/, '')
}

function optionalEnv(name, defaultValue) {
  const value = process.env[name]
  return value && value.trim() ? value.trim() : defaultValue
}

const BASE_URL = requireTrimmedEnv('RUNTIME_CONTROL_E2E_BASE_URL', REQUIRED_BASE_URL_MESSAGE)
const ACTION_ORIGIN = requireTrimmedEnv(
  'RUNTIME_CONTROL_E2E_ACTION_ORIGIN',
  REQUIRED_ACTION_ORIGIN_MESSAGE
)

const TEST_TENANT = optionalEnv('RUNTIME_CONTROL_E2E_TENANT', '测试租户')
const TEST_USERNAME = optionalEnv('RUNTIME_CONTROL_E2E_USERNAME', 'aoteman')
const TEST_PASSWORD = optionalEnv('RUNTIME_CONTROL_E2E_PASSWORD', 'admin123')
const VERIFY_TENANT = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_TENANT', '芋道源码')
const VERIFY_USERNAME = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_USERNAME', 'admin')
const VERIFY_PASSWORD = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_PASSWORD', 'admin123')
const ACTION_API_BASE = `${ACTION_ORIGIN}/admin-api`

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

function collectRuntimeResponses(page) {
  const responses = []
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/infra/runtime-control/')) return
    const runtimePath = runtimeControlPath(url)
    let body
    try {
      body = await response.json()
    } catch (error) {
      body = { parseError: error.message }
    }
    responses.push({
      status: response.status(),
      url,
      runtimePath,
      body,
      data: unwrapResponse(body),
      at: Date.now()
    })
  })
  return responses
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

function findResponse(responses, matcher, minAt = 0) {
  for (let index = responses.length - 1; index >= 0; index -= 1) {
    const response = responses[index]
    if (response.at >= minAt && matcher(response)) {
      return response
    }
  }
  return undefined
}

async function waitForRuntimeResponse(responses, matcher, label, timeoutMs = 15000, minAt = 0) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    const match = findResponse(responses, matcher, minAt)
    if (match) return match
    await new Promise((resolve) => setTimeout(resolve, 150))
  }
  throw new Error(`Timed out waiting for runtime-control response ${label}`)
}

async function waitForResponseData(responses, path, timeoutMs = 15000, minAt = 0) {
  return waitForRuntimeResponse(
    responses,
    (response) => response.runtimePath === path,
    path,
    timeoutMs,
    minAt
  )
}

async function waitForResponsePathSuffix(responses, suffix, timeoutMs = 15000, minAt = 0) {
  return waitForRuntimeResponse(
    responses,
    (response) => response.runtimePath.endsWith(suffix),
    `*${suffix}`,
    timeoutMs,
    minAt
  )
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

async function loginRuntimeControl(page, credentials) {
  await page.goto(`${BASE_URL}/login?redirect=/infra/monitors/runtime-control`, {
    waitUntil: 'domcontentloaded'
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)

  if (page.url().includes('/login')) {
    const tenantSelected = await selectTenant(page, credentials.tenant)
    if (!tenantSelected) {
      await fillFirstVisibleIfPresent(page.locator('input[placeholder="请输入租户名称"]'), credentials.tenant)
    }
    await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), credentials.username)
    await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), credentials.password)
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

function assertOkResponse(response, label) {
  assert(response.status >= 200 && response.status < 300, `${label} HTTP status ${response.status}`)
  if (response.body && typeof response.body.code === 'number') {
    assert(response.body.code === 0, `${label} business code ${response.body.code}: ${response.body.msg || ''}`)
  }
}

function assertRows(rows, label) {
  assert(rows.length > 0, `${label} must contain real data rows`)
}

async function requestRuntimeJson(path, options = {}) {
  const response = await fetch(`${ACTION_API_BASE}${path}`, options)
  let body
  try {
    body = await response.json()
  } catch (error) {
    throw new Error(`Invalid JSON from ${path}: HTTP ${response.status}, ${error.message}`)
  }
  assert(response.status >= 200 && response.status < 300, `${path} HTTP ${response.status}`)
  if (typeof body.code === 'number') {
    assert(body.code === 0, `${path} business code ${body.code}: ${body.msg || ''}`)
  }
  return body.data
}

async function loginForSetup(credentials) {
  const tenantId = await requestRuntimeJson(
    `/system/tenant/get-id-by-name?name=${encodeURIComponent(credentials.tenant)}`
  )
  assert(tenantId, `Cannot resolve tenant id for ${credentials.tenant}`)
  const token = await requestRuntimeJson('/system/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'tenant-id': String(tenantId)
    },
    body: JSON.stringify({
      username: credentials.username,
      password: credentials.password,
      captchaVerification: ''
    })
  })
  assert(token?.accessToken, `Cannot login setup user ${credentials.username}`)
  return {
    tenantId,
    userId: token.userId,
    headers: {
      Authorization: `Bearer ${token.accessToken}`,
      'tenant-id': String(tenantId),
      'Content-Type': 'application/json'
    }
  }
}

async function ensureTestTenantOwnerMatrixData() {
  const setup = await loginForSetup({
    tenant: TEST_TENANT,
    username: TEST_USERNAME,
    password: TEST_PASSWORD
  })
  const rows = await requestRuntimeJson('/infra/runtime-control/owner-matrix', {
    headers: setup.headers
  })
  const existingRows = Array.isArray(rows) ? rows : []
  const requiredRows = [
    {
      environment: 'local',
      action: 'storage-capacity-warning',
      role: 'capacity-owner',
      required: true,
      ownerUserId: setup.userId,
      ownerName: TEST_USERNAME,
      escalationPath: 'E2E测试租户容量告警责任人'
    },
    {
      environment: 'prod',
      action: 'rollback-app',
      role: 'release-owner',
      required: true,
      ownerUserId: setup.userId,
      ownerName: TEST_USERNAME,
      escalationPath: 'E2E测试租户回滚责任人'
    },
    {
      environment: 'prod',
      action: 'restore-data',
      role: 'data-owner',
      required: true,
      ownerUserId: setup.userId,
      ownerName: TEST_USERNAME,
      escalationPath: 'E2E测试租户恢复责任人'
    }
  ]
  let created = 0
  for (const row of requiredRows) {
    const exists = existingRows.some(
      (item) =>
        item.environment === row.environment &&
        item.action === row.action &&
        item.role === row.role
    )
    if (exists) continue
    await requestRuntimeJson('/infra/runtime-control/owner-matrix', {
      method: 'POST',
      headers: setup.headers,
      body: JSON.stringify(row)
    })
    created += 1
  }
  console.log(`SETUP ownerMatrix created=${created}`)
  return setup
}

async function ac02OwnerMatrix(page, responses) {
  const response = await waitForResponseData(responses, '/owner-matrix')
  assertOkResponse(response, 'AC-02 owner matrix')
  const rows = Array.isArray(response.data) ? response.data : []
  assertRows(rows, 'AC-02 owner matrix')
  assert(rows.some((row) => row.required === true), 'AC-02 owner matrix must include required owner rows')
  console.log(`AC-02 PASS ownerRows=${rows.length}`)
}

async function ac04RollbackCandidate(page, responses) {
  const response = await waitForResponseData(responses, '/rollback-candidates')
  assertOkResponse(response, 'AC-04 rollback candidates')
  const rows = Array.isArray(response.data) ? response.data : []
  assertRows(rows, 'AC-04 rollback candidates')
  await page.locator('button:has-text("回滚版本")').first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '回滚版本' }).last()
  await assertVisible(dialog.locator('.candidate-picker').first(), 'AC-04 rollback candidate picker')
  await assertNotVisible(dialog.locator('input[placeholder="例如 20260524_035800"]'), 'AC-04 manual image tag input')
  await dialog.locator('button:has-text("取消")').click()
  console.log(`AC-04 PASS rollbackCandidates=${rows.length}`)
}

async function ac05RestoreCandidate(page, responses) {
  const response = await waitForResponseData(responses, '/restore-candidates')
  assertOkResponse(response, 'AC-05 restore candidates')
  const rows = Array.isArray(response.data) ? response.data : []
  assertRows(rows, 'AC-05 restore candidates')
  assert(
    rows.some((row) => row.status === 'BLOCKED' || row.status === 'AVAILABLE'),
    'AC-05 restore candidate must expose server status'
  )
  await page.locator('button:has-text("恢复数据")').first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '恢复数据' }).last()
  await assertVisible(dialog.locator('.candidate-picker').first(), 'AC-05 restore candidate picker')
  await assertNotVisible(dialog.locator('input[placeholder="例如 20260524_174058"]'), 'AC-05 manual backup input')
  await dialog.locator('button:has-text("取消")').click()
  console.log(`AC-05 PASS restoreCandidates=${rows.length}`)
}

async function ac08Probes(page, responses) {
  const probeStartedAt = Date.now()
  await card(page, '探针状态').getByRole('button', { name: '执行探针' }).click()
  const response = await waitForResponseData(responses, '/probes/run', 15000, probeStartedAt)
  assertOkResponse(response, 'AC-08 probe run')
  const probes = response.data?.probes || []
  assertRows(probes, 'AC-08 probes')
  assert(probes.some((probe) => probe.probeType === 'backend'), 'AC-08 backend probe missing')
  assert(probes.some((probe) => probe.probeType === 'frontend'), 'AC-08 frontend probe missing')
  await assertVisible(card(page, '探针状态').locator('text=耗时').first(), 'AC-08 probe duration column')
  console.log(`AC-08 PASS probes=${probes.length}`)
}

async function ac09Capacity(page, responses) {
  const response = await waitForResponseData(responses, '/capacity/status')
  assertOkResponse(response, 'AC-09 capacity status')
  assert(response.data?.disk || response.data?.logDirectory, 'AC-09 must include disk or logDirectory metric')
  await assertVisible(card(page, '日志与磁盘风险').locator('text=磁盘').first(), 'AC-09 disk metric')
  console.log(`AC-09 PASS status=${response.data?.status}`)
}

async function ac11IncidentFlow(page, responses) {
  const title = `E2E事故-${Date.now()}`
  await page.getByRole('button', { name: '事故闭环' }).click()
  const drawer = page.locator('.ops-incident-drawer')
  await assertVisible(drawer.getByRole('button', { name: '新建事故' }), 'AC-11 incident drawer action')
  const createForm = drawer.locator('.incident-form').filter({ hasText: '新建事故' }).last()
  await createForm.locator('.el-form-item').filter({ hasText: '标题' }).locator('input').fill(title)
  await createForm.locator('.el-form-item').filter({ hasText: '描述' }).locator('textarea').fill('测试租户真实数据 E2E 创建事故')
  const createStartedAt = Date.now()
  const createResponsePromise = waitForResponseData(responses, '/incidents', 15000, createStartedAt)
  await createForm.getByRole('button', { name: '新建事故' }).click()
  const createResponse = await createResponsePromise
  assertOkResponse(createResponse, 'AC-11 create incident')
  await assertVisible(drawer.locator(`text=${title}`).first(), 'AC-11 created incident row', 15000)
  await drawer.locator(`text=${title}`).first().click()

  const actionForm = drawer.locator('.incident-detail .incident-form').first()
  await actionForm.locator('.el-form-item').filter({ hasText: '处置动作' }).locator('input').fill('E2E记录处置')
  await actionForm.locator('.el-form-item').filter({ hasText: '证据' }).locator('textarea').fill('E2E真实页面记录')
  const recordStartedAt = Date.now()
  const recordResponsePromise = waitForResponsePathSuffix(responses, '/actions', 15000, recordStartedAt)
  await actionForm.getByRole('button', { name: '记录处置' }).click()
  const recordResponse = await recordResponsePromise
  assertOkResponse(recordResponse, 'AC-11 record incident action')

  await assertVisible(drawer.locator(`text=${title}`).first(), 'AC-11 incident row after action record', 15000)
  await drawer.locator(`text=${title}`).first().click()
  const closeForm = drawer.locator('.incident-form--gate').first()
  await assertVisible(closeForm.getByRole('button', { name: '关闭事故' }), 'AC-11 close incident action')
  const closeStartedAt = Date.now()
  const closeResponsePromise = waitForResponsePathSuffix(responses, '/close', 15000, closeStartedAt)
  await closeForm.getByRole('button', { name: '关闭事故' }).click()
  const closeResponse = await closeResponsePromise
  assert(
    closeResponse.status >= 400 || closeResponse.body?.code !== 0,
    'AC-11 close without remaining risk and reason must be rejected'
  )
  console.log(`AC-11 PASS incident=${title}`)
}

async function verifyYudaoAdmin(page, responses) {
  const writeRequests = []
  page.on('request', (request) => {
    const url = new URL(request.url())
    if (url.pathname.includes('/admin-api/infra/runtime-control/') && request.method() !== 'GET') {
      writeRequests.push(`${request.method()} ${url.pathname}`)
    }
  })

  await loginRuntimeControl(page, {
    tenant: VERIFY_TENANT,
    username: VERIFY_USERNAME,
    password: VERIFY_PASSWORD
  })

  for (const title of ['探针状态', '日志与磁盘风险']) {
    await assertVisible(card(page, title).locator(`text=${title}`).first(), `YUDAO verify ${title}`)
  }
  await assertVisible(page.getByRole('button', { name: '事故闭环' }), 'YUDAO verify incident entry')

  for (const path of [
    '/owner-matrix',
    '/rollback-candidates',
    '/restore-candidates',
    '/probes/latest',
    '/capacity/status',
    '/incidents/page'
  ]) {
    const response = await waitForResponseData(responses, path)
    assertOkResponse(response, `YUDAO verify ${path}`)
  }
  assert(writeRequests.length === 0, `YUDAO verify must not call write runtime-control endpoints: ${writeRequests.join(', ')}`)
  console.log('YUDAO_ADMIN_VERIFY_PASS')
}

async function runForTestTenant(page, responses) {
  await loginRuntimeControl(page, {
    tenant: TEST_TENANT,
    username: TEST_USERNAME,
    password: TEST_PASSWORD
  })

  assert(
    ACTION_ORIGIN.startsWith('http://127.0.0.1:48098') || ACTION_ORIGIN.startsWith('http'),
    `RUNTIME_CONTROL_E2E_ACTION_ORIGIN must be explicit, got ${ACTION_ORIGIN}`
  )

  await ac02OwnerMatrix(page, responses)
  await ac04RollbackCandidate(page, responses)
  await ac05RestoreCandidate(page, responses)
  await ac08Probes(page, responses)
  await ac09Capacity(page, responses)
  await ac11IncidentFlow(page, responses)
  console.log('TEST_TENANT_PASS')
}

async function main() {
  await ensureTestTenantOwnerMatrixData()
  const browser = await chromium.launch({ headless: true })
  try {
    const testContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const testPage = await testContext.newPage()
    const testResponses = collectRuntimeResponses(testPage)
    await runForTestTenant(testPage, testResponses)
    await testContext.close()

    const verifyContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const verifyPage = await verifyContext.newPage()
    const verifyResponses = collectRuntimeResponses(verifyPage)
    await verifyYudaoAdmin(verifyPage, verifyResponses)
    await verifyContext.close()

    console.log('PASS: runtime control real-data E2E covers retained runtime-control entries')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
