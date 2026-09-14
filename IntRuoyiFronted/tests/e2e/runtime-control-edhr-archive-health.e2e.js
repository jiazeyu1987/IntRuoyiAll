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

function assertOkResponse(response) {
  assert(response.status >= 200 && response.status < 300, `business-health HTTP status ${response.status}`)
  if (response.body && typeof response.body.code === 'number') {
    assert(response.body.code === 0, `business-health business code ${response.body.code}: ${response.body.msg || ''}`)
  }
  if (response.body && response.body.parseError) {
    throw new Error(`business-health response JSON parse failed: ${response.body.parseError}`)
  }
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

async function requestBusinessHealth(page) {
  return page.evaluate(async (apiPath) => {
    const readStoredValue = (raw) => {
      if (!raw) return undefined
      try {
        const parsed = JSON.parse(raw)
        if (parsed && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
          try {
            return JSON.parse(parsed.v)
          } catch (error) {
            return parsed.v
          }
        }
        return parsed
      } catch (error) {
        return raw
      }
    }
    const accessToken = readStoredValue(localStorage.getItem('ACCESS_TOKEN'))
    const tenantId = readStoredValue(localStorage.getItem('tenantId'))
    const headers = {}
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`
    if (tenantId) headers['tenant-id'] = tenantId
    const response = await fetch(apiPath, { headers })
    let body
    try {
      body = await response.json()
    } catch (error) {
      body = { parseError: error.message }
    }
    return {
      status: response.status,
      body,
      data: body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body,
      authState: {
        hasAccessToken: Boolean(accessToken),
        hasTenantId: Boolean(tenantId)
      },
      at: Date.now()
    }
  }, BUSINESS_HEALTH_API)
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
  const loginForm = page.locator('.login-form:visible').first()
  const tenantSelect = loginForm.locator('.el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    return false
  }
  await tenantSelect.click()
  const input = loginForm.locator('.el-select input[role="combobox"], .el-select input').first()
  await input.click()
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
  await page.keyboard.press('Backspace')
  await page.keyboard.type(tenantName)
  await page.waitForTimeout(200)
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: tenantName })
    .first()
  if ((await option.count()) > 0 && (await option.isVisible())) {
    await option.click()
  } else {
    await page.keyboard.press('Enter')
  }
  return true
}

async function loginRuntimeControl(page) {
  await page.goto(`${BASE_URL}/login?redirect=/infra/monitors/runtime-control`, {
    waitUntil: 'commit',
    timeout: 120000
  })

  if (page.url().includes('/login')) {
    await page
      .locator('.login-form:visible input[placeholder="请输入用户名"], .login-form:visible input[placeholder="请输入账号"]')
      .first()
      .waitFor({ state: 'visible', timeout: 120000 })
    const tenantSelected = await selectTenant(page, TEST_TENANT)
    if (!tenantSelected) {
      await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), TEST_TENANT)
    }
    await requireFillFirstVisible(
      page.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'),
      TEST_USERNAME,
      'username'
    )
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

  const response = latestBusinessHealthResponse(responses) || (await requestBusinessHealth(page))
  assert(response.authState?.hasAccessToken !== false, 'business-health request is missing the logged-in access token')
  assert(response.authState?.hasTenantId !== false, 'business-health request is missing the selected tenant id')
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

  const itemSignal = [item.evidence, item.reason].filter(Boolean).join(' ')

  assert(
    writeRequests.length === 0,
    `runtime-control business health page must stay read-only; non-GET requests: ${writeRequests.join(', ')}`
  )

  console.log(
    `PASS: ${HEALTH_ITEM_CODE} ${HEALTH_ITEM_NAME} verified by real runtime-control login with status=${item.status}, signal=${itemSignal || '-'}, writes=${writeRequests.length}`
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
