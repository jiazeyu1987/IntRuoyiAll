function requireTrimmedEnv(name) {
  const value = process.env[name]
  if (!value || !value.trim()) {
    throw new Error(`${name} is required for eDHR archive runtime-control E2E`)
  }
  return value.trim().replace(/\/+$/, '')
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      "Playwright is required for eDHR archive runtime-control E2E. Run with a Node environment that can resolve the 'playwright' package."
    )
  }
}

const BASE_URL = requireTrimmedEnv('RUNTIME_CONTROL_E2E_BASE_URL')
const TEST_TENANT = requireTrimmedEnv('RUNTIME_CONTROL_E2E_TEST_TENANT')
const TEST_USERNAME = requireTrimmedEnv('RUNTIME_CONTROL_E2E_TEST_USERNAME')
const TEST_PASSWORD = requireTrimmedEnv('RUNTIME_CONTROL_E2E_TEST_PASSWORD')

const RUNTIME_CONTROL_ROUTE = '/infra/monitors/runtime-control'
const BUSINESS_HEALTH_API = '/admin-api/infra/runtime-control/business-health'
const HEALTH_ITEM_CODE = 'edhr-archive-integrity'
const HEALTH_ITEM_NAME = 'eDHR 归档完整性'

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
    if (!response.url().includes(BUSINESS_HEALTH_API)) return
    let body
    try {
      body = await response.json()
    } catch (error) {
      body = { parseError: error.message }
    }
    responses.push({
      status: response.status(),
      body,
      data: unwrapResponse(body),
      at: Date.now()
    })
  })

  return { responses, writeRequests }
}

function latestBusinessHealthResponse(responses, minAt = 0) {
  for (let index = responses.length - 1; index >= 0; index -= 1) {
    const response = responses[index]
    if (response.at >= minAt) {
      return response
    }
  }
  return undefined
}

async function waitForBusinessHealthResponse(responses, timeoutMs = 45000, minAt = 0) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    const response = latestBusinessHealthResponse(responses, minAt)
    if (response) return response
    await new Promise((resolve) => setTimeout(resolve, 150))
  }
  throw new Error(`Timed out waiting for ${BUSINESS_HEALTH_API}`)
}

function assertOkResponse(response) {
  assert(response.status >= 200 && response.status < 300, `business-health HTTP status ${response.status}`)
  if (response.body && typeof response.body.code === 'number') {
    assert(response.body.code === 0, `business-health business code ${response.body.code}: ${response.body.msg || ''}`)
  }
  if (response.body && response.body.parseError) {
    throw new Error(`business-health response JSON parse failed: ${response.body.parseError}`)
  }
}

function extractBusinessHealthItems(data) {
  if (Array.isArray(data)) return data
  if (data && Array.isArray(data.items)) return data.items
  if (data && data.businessHealth && Array.isArray(data.businessHealth.items)) return data.businessHealth.items
  return []
}

async function fillFirstVisible(locator, value) {
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

async function requireFillFirstVisible(locator, value, label) {
  const filled = await fillFirstVisible(locator, value)
  if (!filled) {
    throw new Error(`No visible input found for ${label}`)
  }
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
    const tenantSelected = await selectTenant(page, TEST_TENANT)
    if (!tenantSelected) {
      await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), TEST_TENANT)
    }
    await requireFillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), TEST_USERNAME, 'username')
    await requireFillFirstVisible(page.locator('input[placeholder="请输入密码"]'), TEST_PASSWORD, 'password')
    await page.locator('button:has-text("登录")').first().click()
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  }

  if (!page.url().includes(RUNTIME_CONTROL_ROUTE)) {
    await page.goto(`${BASE_URL}${RUNTIME_CONTROL_ROUTE}`, { waitUntil: 'domcontentloaded' })
  }
  await page.waitForSelector('text=运行控制台', { timeout: 30000 })
}

async function assertVisible(locator, label, timeout = 15000) {
  try {
    await locator.waitFor({ state: 'visible', timeout })
  } catch (error) {
    throw new Error(`${label} should be visible. ${error.message}`)
  }
}

async function verifyEdhrArchiveBusinessHealth(page, responses, writeRequests) {
  await loginRuntimeControl(page)

  const response = await waitForBusinessHealthResponse(responses)
  assertOkResponse(response)

  const items = extractBusinessHealthItems(response.data)
  const item = items.find((candidate) => candidate && candidate.code === HEALTH_ITEM_CODE)
  assert(
    item,
    `BLOCKED: current-code backend has not loaded ${HEALTH_ITEM_CODE}; observed business health item codes: ${
      items.map((candidate) => candidate && candidate.code).filter(Boolean).join(', ') || '(none)'
    }`
  )
  assert(item.name === HEALTH_ITEM_NAME, `unexpected ${HEALTH_ITEM_CODE} name: ${item.name}`)
  assert(['PASS', 'WARN', 'BLOCKED'].includes(item.status), `unexpected ${HEALTH_ITEM_CODE} status: ${item.status}`)

  await assertVisible(page.getByText(HEALTH_ITEM_NAME, { exact: true }).first(), `${HEALTH_ITEM_NAME} UI row`)

  const itemSignal = [item.evidence, item.reason].filter(Boolean).join(' ')
  const signalToken = itemSignal.includes('sealed=') ? 'sealed=' : itemSignal.includes('storageRetention') ? 'storageRetention' : ''
  if (signalToken) {
    await assertVisible(page.getByText(signalToken, { exact: false }).first(), `${HEALTH_ITEM_NAME} ${signalToken} signal`, 10000)
  }

  assert(
    writeRequests.length === 0,
    `runtime-control business health page must stay read-only; non-GET requests: ${writeRequests.join(', ')}`
  )

  console.log(
    `PASS: ${HEALTH_ITEM_CODE} ${HEALTH_ITEM_NAME} visible with status=${item.status}, writes=${writeRequests.length}`
  )
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const page = await context.newPage()
    const { responses, writeRequests } = collectRuntimeActivity(page)
    await verifyEdhrArchiveBusinessHealth(page, responses, writeRequests)
    await context.close()
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
