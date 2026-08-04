const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const TASK_ID = '20260804-production-fill-fullscreen-toggle'
const ROUTE = '/mes/pro/feedback/edhr-batch-production-fill'
const OUTPUT_DIR = path.resolve(process.cwd(), 'test-results', TASK_ID)
const DEFAULT_CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const DEFAULT_EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

function envValue(key) {
  return String(process.env[key] || '').trim()
}

function sanitizeUrl(value) {
  return String(value || '').replace(/\/+$/, '')
}

const BASE_URL = sanitizeUrl(
  envValue('PFFT_E2E_BASE_URL') || envValue('TLW_FRONTEND_URL') || 'http://127.0.0.1:8081'
)
const BACKEND_URL = sanitizeUrl(
  envValue('PFFT_E2E_BACKEND_URL') || envValue('TLW_BACKEND_URL') || 'http://127.0.0.1:48081'
)

const CREDENTIALS = {
  tenant: envValue('PFFT_E2E_TENANT') || envValue('TLW_TENANT'),
  username: envValue('PFFT_E2E_USERNAME') || envValue('TLW_USERNAME'),
  password: envValue('PFFT_E2E_PASSWORD') || envValue('TLW_PASSWORD')
}

function ensureOutputDir() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

function writeResult(result) {
  ensureOutputDir()
  const safeResult = { ...result }
  delete safeResult.password
  if (safeResult.credentials) {
    safeResult.credentials = {
      tenant: safeResult.credentials.tenant,
      username: safeResult.credentials.username
    }
  }
  fs.writeFileSync(
    path.join(OUTPUT_DIR, 'result.json'),
    `${JSON.stringify(safeResult, null, 2)}\n`,
    'utf8'
  )
}

function block(reason, details = {}) {
  writeResult({
    status: 'BLOCKED',
    generatedAt: new Date().toISOString(),
    taskId: TASK_ID,
    route: ROUTE,
    baseUrl: BASE_URL,
    backendUrl: BACKEND_URL,
    reason,
    ...details
  })
  const error = new Error(reason)
  error.blocked = true
  throw error
}

function assertRuntimePair() {
  const allowedPairs = new Map([
    ['http://127.0.0.1:8081', 'http://127.0.0.1:48081'],
    ['http://localhost:8081', 'http://127.0.0.1:48081']
  ])
  if (allowedPairs.get(BASE_URL) !== BACKEND_URL) {
    block('前后端 URL 必须使用当前 int_main 主运行态 8081/48081，不能静默切换端口。', {
      allowedPairs: Array.from(allowedPairs.entries()).map(([frontend, backend]) => ({
        frontend,
        backend
      }))
    })
  }
}

function collectMissingCredentials() {
  const missing = []
  if (!CREDENTIALS.tenant) {
    missing.push({
      key: 'PFFT_E2E_TENANT or TLW_TENANT',
      description: '生产组长账号所在租户。'
    })
  }
  if (!CREDENTIALS.username) {
    missing.push({
      key: 'PFFT_E2E_USERNAME or TLW_USERNAME',
      description: '已授权的生产组长测试账号。'
    })
  }
  if (!CREDENTIALS.password) {
    missing.push({
      key: 'PFFT_E2E_PASSWORD or TLW_PASSWORD',
      description: '生产组长测试账号密码，只能通过环境变量注入。'
    })
  }
  return missing
}

function resolveBrowserExecutable() {
  const configuredPath =
    envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH') || envValue('PLAYWRIGHT_CHROME_EXECUTABLE')
  if (configuredPath) {
    if (!fs.existsSync(configuredPath)) {
      block('指定的 Playwright 浏览器可执行文件不存在。', {
        browserExecutable: configuredPath
      })
    }
    return configuredPath
  }
  if (fs.existsSync(DEFAULT_CHROME)) return DEFAULT_CHROME
  if (fs.existsSync(DEFAULT_EDGE)) return DEFAULT_EDGE
  block('本机未发现可用 Chrome 或 Edge，无法执行真实 Playwright E2E。')
}

async function assertServiceReady() {
  const frontend = await fetch(BASE_URL)
  assert.equal(frontend.status, 200, `frontend must return HTTP 200 at ${BASE_URL}`)
  const backend = await fetch(`${BACKEND_URL}/actuator/health`)
  assert.equal(backend.status, 200, `backend health must return HTTP 200 at ${BACKEND_URL}`)
  const body = await backend.json()
  assert.equal(body.status, 'UP', `backend health must be UP: ${JSON.stringify(body)}`)
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
  throw new Error(`missing visible input: ${label}`)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(CREDENTIALS.tenant)
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: CREDENTIALS.tenant })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), CREDENTIALS.tenant, 'tenant')
}

async function login(page) {
  await page.context().clearCookies()
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })
  if ((await form.locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder*="验证码"]:visible').count()) > 0) {
    block('登录页启用了验证码，无法执行无人值守真实 E2E。')
  }

  await selectTenant(page, form)
  await fillFirstVisible(
    form.locator(
      'input[placeholder*="账号"], input[placeholder*="用户名"], input.el-input__inner:not([role="combobox"]):not([type="password"])'
    ),
    CREDENTIALS.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), CREDENTIALS.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `login business failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

function isTargetMesUrl(url) {
  return (
    url.includes('/admin-api/mes/pro/feedback/frontline') ||
    url.includes('/admin-api/mes/pro/batch-record')
  )
}

function createNetworkTracker(page) {
  const targetWriteRequests = []
  const targetFailures = []
  page.on('request', (request) => {
    const url = request.url()
    if (isTargetMesUrl(url) && WRITE_METHODS.has(request.method())) {
      targetWriteRequests.push({
        method: request.method(),
        path: new URL(url).pathname
      })
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (!isTargetMesUrl(url)) return
    if (response.status() >= 400) {
      targetFailures.push({
        method: response.request().method(),
        path: new URL(url).pathname,
        status: response.status()
      })
      return
    }
    const contentType = String(response.headers()['content-type'] || '')
    if (!contentType.includes('application/json')) return
    try {
      const body = await response.json()
      if (body && ![0, 200, undefined].includes(body.code)) {
        targetFailures.push({
          method: response.request().method(),
          path: new URL(url).pathname,
          code: body.code,
          message: body.msg || body.message
        })
      }
    } catch {
      targetFailures.push({
        method: response.request().method(),
        path: new URL(url).pathname,
        status: response.status(),
        message: '目标接口 JSON 响应无法解析。'
      })
    }
  })
  return { targetWriteRequests, targetFailures }
}

async function readButtonText(button) {
  return String(await button.textContent()).replace(/\s+/g, '')
}

async function assertButtonText(button, expected) {
  const actual = await readButtonText(button)
  assert.equal(actual, expected, `fullscreen button text should be ${expected}, got ${actual}`)
}

async function verifyProductionFullscreen(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  const screen = page.locator('[data-frontline-production-operator]').first()
  await screen.waitFor({ state: 'visible', timeout: 90000 })

  const button = screen.locator('.frontline-production-fullscreen-button').first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await assertButtonText(button, '最大化')

  await button.click()
  await page.waitForFunction(() => {
    const target = document.querySelector('[data-frontline-production-operator]')
    return document.fullscreenElement === target
  }, null, { timeout: 30000 })
  await assertButtonText(button, '主页')
  await page.screenshot({
    path: path.join(OUTPUT_DIR, 'production-fill-fullscreen.png'),
    fullPage: true
  })

  await button.click()
  await page.waitForFunction(() => document.fullscreenElement === null, null, { timeout: 30000 })
  await assertButtonText(button, '最大化')
  await page.screenshot({
    path: path.join(OUTPUT_DIR, 'production-fill-restored.png'),
    fullPage: true
  })
}

async function run() {
  ensureOutputDir()
  assertRuntimePair()
  const missingCredentials = collectMissingCredentials()
  if (missingCredentials.length) {
    block('缺少生产组长账号环境变量，不能用 admin 或默认本机账号替代真实生产组长路径。', {
      missing: missingCredentials
    })
  }
  const browserExecutable = resolveBrowserExecutable()
  await assertServiceReady()

  const browser = await chromium.launch({
    headless: envValue('PFFT_E2E_HEADED') !== '1',
    executablePath: browserExecutable
  })
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const consoleErrors = []
  const pageErrors = []
  const { targetWriteRequests, targetFailures } = createNetworkTracker(page)

  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => {
    pageErrors.push(error.message)
  })

  try {
    await login(page)
    await verifyProductionFullscreen(page)
    assert.deepEqual(targetWriteRequests, [], 'production fullscreen E2E must not send MES write requests')
    assert.deepEqual(targetFailures, [], `target MES request failures: ${JSON.stringify(targetFailures, null, 2)}`)
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join('\n')}`)

    const result = {
      status: 'PASS',
      generatedAt: new Date().toISOString(),
      taskId: TASK_ID,
      route: ROUTE,
      baseUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      credentials: CREDENTIALS,
      writeRequestCount: targetWriteRequests.length,
      targetFailures,
      consoleErrors,
      pageErrors,
      screenshots: [
        path.join(OUTPUT_DIR, 'production-fill-fullscreen.png'),
        path.join(OUTPUT_DIR, 'production-fill-restored.png')
      ]
    }
    writeResult(result)
    console.log(
      `PASS: production fill fullscreen real E2E route=${ROUTE} tenant=${CREDENTIALS.tenant} username=${CREDENTIALS.username}`
    )
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
  }
}

run().catch((error) => {
  if (error.blocked) {
    console.error(`BLOCKED: ${error.message}`)
    process.exitCode = 2
    return
  }
  console.error(error)
  process.exitCode = 1
})
