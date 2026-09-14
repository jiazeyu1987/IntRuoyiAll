const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const OUTPUT_DIR = path.resolve(
  WORKSPACE_ROOT,
  'output',
  'playwright',
  '20260812-frontline-production-active-order'
)
const PRODUCTION_ROUTE = '/mes/pro/feedback/edhr-batch-production-fill'
const TEAM_LEADER_ROUTE = '/mes/pro/process-pool/production-leader'
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
const BASE_URL = String(env.FRONTLINE_ACTIVE_ORDER_E2E_BASE_URL || 'http://127.0.0.1:8100')
  .replace(/\/+$/, '')
const BACKEND_URL = String(env.FRONTLINE_ACTIVE_ORDER_E2E_BACKEND_URL || 'http://127.0.0.1:48100')
  .replace(/\/+$/, '')
const CREDENTIALS = {
  tenant: env.FRONTLINE_ACTIVE_ORDER_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: env.FRONTLINE_ACTIVE_ORDER_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: env.FRONTLINE_ACTIVE_ORDER_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD
}

function resolveBrowserExecutable() {
  const configured = env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || env.PLAYWRIGHT_CHROME_EXECUTABLE
  const candidates = [
    configured,
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
  ].filter(Boolean)
  const executable = candidates.find((candidate) => fs.existsSync(candidate))
  assert.ok(executable, '本机缺少可用的 Chrome 或 Edge，无法执行真实页面验收。')
  return executable
}

async function assertRuntimeReady() {
  const allowedPairs = new Map([
    ['http://127.0.0.1:8100', 'http://127.0.0.1:48100'],
    ['http://localhost:8100', 'http://127.0.0.1:48100'],
    ['http://127.0.0.1:8081', 'http://127.0.0.1:48081'],
    ['http://localhost:8081', 'http://127.0.0.1:48081']
  ])
  assert.equal(
    allowedPairs.get(BASE_URL),
    BACKEND_URL,
    '前后端地址必须使用当前工作树 8100/48100 或融合后的 int_main 8081/48081。'
  )
  const frontend = await fetch(BASE_URL)
  assert.equal(frontend.status, 200, `前端不可访问：${BASE_URL}`)
  const backend = await fetch(`${BACKEND_URL}/actuator/health`)
  assert.equal(backend.status, 200, `后端健康检查不可访问：${BACKEND_URL}`)
  assert.equal((await backend.json()).status, 'UP', '后端健康状态不是 UP。')
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
  throw new Error(`缺少可用的${label}输入框。`)
}

async function login(page) {
  assert.ok(CREDENTIALS.tenant, '缺少本机登录租户。')
  assert.ok(CREDENTIALS.username, '缺少本机登录用户名。')
  assert.ok(CREDENTIALS.password, '缺少本机登录密码。')

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
    '登录页启用了验证码，无法执行无人值守真实页面验收。'
  )

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
  }

  await fillFirstVisible(
    form.locator(
      'input[placeholder*="账号"], input[placeholder*="用户名"], input.el-input__inner:not([role="combobox"]):not([type="password"])'
    ),
    CREDENTIALS.username,
    '用户名'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), CREDENTIALS.password, '密码')

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await responsePromise
  assert.ok(response.ok(), `登录 HTTP 失败：${response.status()}`)
  const body = await response.json()
  assert.ok([0, 200].includes(body.code), `登录业务失败：${body.msg || body.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function verifyProductionPage(page) {
  await page.goto(`${BASE_URL}${PRODUCTION_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const screen = page.locator('[data-frontline-production-operator]').first()
  await screen.waitFor({ state: 'visible', timeout: 90000 })

  const cards = screen.locator('[data-frontline-production-selection-card]')
  assert.equal(await cards.count(), 3, '一线生产顶部必须显示活跃订单、工序、员工三个选择区域。')
  assert.equal(
    await cards.nth(0).getAttribute('data-frontline-production-active-order-card'),
    '',
    '第一项必须是活跃订单选择。'
  )
  const labels = await cards.locator('.top-label').allTextContents()
  assert.deepEqual(labels.map((label) => label.trim()), ['活跃订单', '工序', '员工'])

  const boxes = await Promise.all([cards.nth(0), cards.nth(1), cards.nth(2)].map((card) => card.boundingBox()))
  assert.ok(boxes.every(Boolean), '三个选择区域必须全部可见。')
  assert.ok(boxes[0].x < boxes[1].x && boxes[1].x < boxes[2].x, '选择区域顺序必须为订单、工序、员工。')

  await cards.nth(0).click()
  const picker = screen.locator('.frontline-picker__card').first()
  await picker.waitFor({ state: 'visible', timeout: 30000 })
  await picker
    .locator('input[aria-label="输入订单号筛选活跃订单"]')
    .waitFor({ state: 'visible', timeout: 30000 })
  await picker.getByText('选择活跃订单', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })

  const screenshot = path.join(OUTPUT_DIR, 'production-active-order-picker.png')
  await page.screenshot({ path: screenshot, fullPage: true })
  await picker.locator('.frontline-picker__close').click()
  await picker.waitFor({ state: 'hidden', timeout: 30000 })
  return screenshot
}

async function verifyTeamLeaderPage(page) {
  await page.goto(`${BASE_URL}${TEAM_LEADER_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const workbench = page.locator('[data-team-leader-report-workbench]').first()
  await workbench.waitFor({ state: 'visible', timeout: 90000 })
  await workbench.getByText('生产工单', { exact: true }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  const screenshot = path.join(OUTPUT_DIR, 'team-leader-report-workbench.png')
  await page.screenshot({ path: screenshot, fullPage: true })
  return screenshot
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  await assertRuntimeReady()
  const browser = await chromium.launch({
    headless: env.FRONTLINE_ACTIVE_ORDER_E2E_HEADED !== '1',
    executablePath: resolveBrowserExecutable()
  })
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const mesWriteRequests = []
  const pageErrors = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && WRITE_METHODS.has(request.method())) {
      mesWriteRequests.push({ method: request.method(), path: new URL(request.url()).pathname })
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    const productionScreenshot = await verifyProductionPage(page)
    const teamLeaderScreenshot = await verifyTeamLeaderPage(page)
    assert.deepEqual(mesWriteRequests, [], '只读验收不得发出 MES 写请求。')
    assert.deepEqual(pageErrors, [], `页面错误：${pageErrors.join('\n')}`)

    const result = {
      status: 'PASS',
      generatedAt: new Date().toISOString(),
      baseUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      identity: `${CREDENTIALS.tenant}/${CREDENTIALS.username}`,
      productionRoute: PRODUCTION_ROUTE,
      teamLeaderRoute: TEAM_LEADER_ROUTE,
      mesWriteRequests,
      pageErrors,
      screenshots: [productionScreenshot, teamLeaderScreenshot]
    }
    fs.writeFileSync(path.join(OUTPUT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log('PASS: frontline production active-order read-only real E2E')
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
