const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const TASK_ID = '20260808-active-order-product-search'
const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_ROOT = path.join(WORKSPACE_ROOT, 'doc', 'tasks', TASK_ID)
const EVIDENCE_DIR = path.join(TASK_ROOT, 'evidence', 'active-order-product-search-real')
const RESULT_FILE = path.join(EVIDENCE_DIR, 'result.json')
const SCREENSHOT_FILE = path.join(EVIDENCE_DIR, 'product-search-candidates.png')
const TARGET_ROUTE = '/mes/pro/process-pool/production-leader'
const DEFAULT_PRODUCT_KEYWORD = '球囊扩张压力泵'
const CANDIDATES_PATH = '/admin-api/mes/pro/process-pool/team-leader/active-order/candidates'
const ADD_PATH = '/admin-api/mes/pro/process-pool/team-leader/active-order/add'

function parseEnvValue(value) {
  const trimmed = String(value || '').trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  return fs.readFileSync(filePath, 'utf8').split(/\r?\n/).reduce((result, line) => {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/)
    if (!match || match[1].startsWith('#')) {
      return result
    }
    result[match[1]] = parseEnvValue(match[2])
    return result
  }, {})
}

function sanitizeUrl(value) {
  return String(value || '').trim().replace(/\/+$/, '')
}

function collectConfig() {
  const env = {
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env')),
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env.local')),
    ...process.env
  }
  return {
    baseUrl: sanitizeUrl(env.ACTIVE_ORDER_PRODUCT_E2E_BASE_URL || 'http://127.0.0.1:8081'),
    backendUrl: sanitizeUrl(
      env.ACTIVE_ORDER_PRODUCT_E2E_BACKEND_URL ||
      env.VITE_BASE_URL ||
      'http://127.0.0.1:48081'
    ),
    tenant: env.ACTIVE_ORDER_PRODUCT_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.ACTIVE_ORDER_PRODUCT_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.ACTIVE_ORDER_PRODUCT_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD,
    productKeyword: env.ACTIVE_ORDER_PRODUCT_E2E_KEYWORD || DEFAULT_PRODUCT_KEYWORD,
    headed: env.ACTIVE_ORDER_PRODUCT_E2E_HEADED === '1'
  }
}

function createBlockedError(message, details = {}) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function redact(value) {
  return String(value || '')
    .replace(/([?&](?:token|accessToken|refreshToken|password|secret)=)[^&\s]+/gi, '$1<redacted>')
    .replace(/(Authorization:\s*Bearer\s+)[^\s]+/gi, '$1<redacted>')
    .slice(0, 500)
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function writeResult(result) {
  ensureDir(EVIDENCE_DIR)
  fs.writeFileSync(RESULT_FILE, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function resolveBrowserExecutable() {
  const candidates = [
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH,
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe'
  ].filter(Boolean)
  return candidates.find((candidate) => fs.existsSync(candidate))
}

function assertLocalRuntime(config) {
  assert.match(
    config.baseUrl,
    /^http:\/\/(127\.0\.0\.1|localhost):8081$/,
    'E2E must use the local int_main frontend runtime on port 8081'
  )
  assert.match(
    config.backendUrl,
    /^http:\/\/(127\.0\.0\.1|localhost):48081$/,
    'E2E must use the local int_main backend runtime on port 48081'
  )
  if (!config.tenant || !config.username || !config.password) {
    throw createBlockedError('缺少本机默认登录租户、用户名或密码。', {
      tenantPresent: Boolean(config.tenant),
      usernamePresent: Boolean(config.username),
      passwordPresent: Boolean(config.password)
    })
  }
  if (!config.productKeyword.trim()) {
    throw createBlockedError('缺少产品搜索关键词。')
  }
}

async function assertHttpOk(url, label) {
  const response = await fetch(url)
  assert.equal(response.ok, true, `${label} HTTP ${response.status}`)
}

async function responseJsonOk(response, label) {
  assert.equal(response.ok(), true, `${label} HTTP ${response.status()}`)
  const body = await response.json()
  assert.ok([0, 200].includes(body.code), `${label} business code ${body.code}: ${body.msg || body.message || ''}`)
  return body
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if (await input.isVisible().catch(() => false)) {
      await input.fill(String(value))
      return
    }
  }
  throw new Error(`找不到可填写的 ${label} 输入框。`)
}

async function selectTenant(page, form, tenant) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.isVisible().catch(() => false)) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    const option = page.locator('.el-select-dropdown__item:visible', { hasText: tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form, config.tenant)
  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input[placeholder*="用户名"]'),
    config.username,
    'username'
  )
  await fillFirstVisible(
    form.locator('input[type="password"], input[placeholder="请输入密码"], input[placeholder*="密码"]'),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /登录/ }).click()
  await responseJsonOk(await loginResponsePromise, 'login')
  await responseJsonOk(await permissionResponsePromise, 'permission')
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

function visibleTab(page, name) {
  return page.locator('.el-tabs__item:visible').filter({
    hasText: new RegExp(`^\\s*${name}\\s*$`)
  }).first()
}

async function runProductSearch(page, config) {
  await page.goto(`${config.baseUrl}${TARGET_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('[data-production-leader-workbench-page]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  await visibleTab(page, '活跃订单池').click()

  const activeOrderSection = page.locator('[data-team-leader-active-order-config]:visible').first()
  await activeOrderSection.waitFor({ state: 'visible', timeout: 30000 })
  await activeOrderSection.getByRole('button', { name: '新增活跃订单' }).click()

  const dialog = page.locator('[data-team-leader-active-order-add-dialog]').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const formItem = dialog.locator('[data-team-leader-active-order-work-order-code]').first()
  const labelText = (await formItem.locator('.el-form-item__label').innerText()).trim()
  assert.equal(labelText, '订单号/产品')

  const comboInput = formItem.locator('input[role="combobox"]:visible').first()
  await comboInput.waitFor({ state: 'visible', timeout: 30000 })
  const placeholderNode = formItem.getByText('请输入订单号、产品编码或产品名称', { exact: true }).first()
  await placeholderNode.waitFor({ state: 'visible', timeout: 30000 })
  const placeholder = (await placeholderNode.innerText()).trim()
  assert.equal(placeholder, '请输入订单号、产品编码或产品名称')

  const keyword = config.productKeyword.trim()
  const candidateResponsePromise = page.waitForResponse((response) => {
    if (response.request().method() !== 'GET') {
      return false
    }
    const url = new URL(response.url())
    return url.pathname === CANDIDATES_PATH && url.searchParams.get('keyword') === keyword
  }, { timeout: 60000 })

  await comboInput.click()
  await comboInput.fill(keyword)

  const candidateResponse = await candidateResponsePromise
  const candidateBody = await responseJsonOk(candidateResponse, 'active order candidates')
  const candidates = Array.isArray(candidateBody.data) ? candidateBody.data : []
  if (candidates.length === 0) {
    throw createBlockedError('产品关键词没有返回任何活跃订单候选，缺少可验证的本机测试数据。', {
      productKeyword: keyword
    })
  }
  const firstCandidate = candidates.find((candidate) =>
    Number(candidate.workOrderId) > 0 && String(candidate.workOrderCode || '').trim()
  )
  if (!firstCandidate) {
    throw createBlockedError('候选接口返回了数据，但缺少有效 workOrderId/workOrderCode。', {
      productKeyword: keyword,
      candidateCount: candidates.length
    })
  }

  const option = page.locator('.el-select-dropdown__item:visible', {
    hasText: String(firstCandidate.workOrderCode)
  }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })

  await page.screenshot({ path: SCREENSHOT_FILE, fullPage: true })
  return {
    labelText,
    placeholder,
    requestKeyword: new URL(candidateResponse.url()).searchParams.get('keyword'),
    candidateCount: candidates.length,
    firstCandidate: {
      workOrderId: firstCandidate.workOrderId,
      workOrderCode: firstCandidate.workOrderCode,
      eligible: firstCandidate.eligible,
      ineligibleReason: firstCandidate.ineligibleReason || ''
    },
    screenshot: SCREENSHOT_FILE
  }
}

async function main() {
  const config = collectConfig()
  const executablePath = resolveBrowserExecutable()
  const result = {
    status: 'RUNNING',
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    targetRoute: TARGET_ROUTE,
    tenant: config.tenant,
    username: config.username,
    productKeyword: config.productKeyword,
    browserExecutable: executablePath || '',
    targetNetworkFailures: [],
    pageErrors: [],
    consoleErrors: [],
    activeOrderAddRequestCount: 0
  }

  let browser
  try {
    assertLocalRuntime(config)
    if (!executablePath) {
      throw createBlockedError('未找到可用 Chrome/Edge，且 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH 未指向有效文件。')
    }
    await assertHttpOk(`${config.baseUrl}/`, 'frontend')
    await assertHttpOk(`${config.backendUrl}/actuator/health`, 'backend health')

    browser = await chromium.launch({
      headless: !config.headed,
      executablePath
    })
    const context = await browser.newContext({
      viewport: { width: 1680, height: 900 },
      locale: 'zh-CN',
      ignoreHTTPSErrors: true
    })
    const page = await context.newPage()
    page.on('pageerror', (error) => result.pageErrors.push(redact(error.message)))
    page.on('console', (message) => {
      if (message.type() === 'error') {
        result.consoleErrors.push(redact(message.text()))
      }
    })
    page.on('request', (request) => {
      const url = new URL(request.url())
      if (url.pathname === ADD_PATH && request.method() === 'POST') {
        result.activeOrderAddRequestCount += 1
      }
    })
    page.on('response', (response) => {
      const url = new URL(response.url())
      if (
        url.pathname.includes('/admin-api/mes/pro/process-pool/team-leader/active-order') &&
        !response.ok()
      ) {
        result.targetNetworkFailures.push({
          method: response.request().method(),
          url: redact(response.url()),
          status: response.status()
        })
      }
    })

    await login(page, config)
    result.search = await runProductSearch(page, config)

    assert.equal(result.activeOrderAddRequestCount, 0, '只读 E2E 不允许发起新增活跃订单写请求。')
    assert.deepEqual(result.targetNetworkFailures, [], '目标活跃订单链路存在 HTTP 失败。')
    assert.deepEqual(result.pageErrors, [], '页面存在未处理异常。')

    result.status = 'PASS'
    result.reason = '产品关键词远程搜索候选下拉真实页面验证通过，且没有新增写请求。'
    writeResult(result)
    console.log(
      `PASS: productKeyword=${result.productKeyword} candidates=${result.search.candidateCount} addRequests=${result.activeOrderAddRequestCount}`
    )
  } catch (error) {
    result.status = error.blocked ? 'BLOCKED' : 'FAIL'
    result.reason = error.message || String(error)
    result.details = error.details || undefined
    try {
      ensureDir(EVIDENCE_DIR)
      if (browser) {
        const pages = browser.contexts().flatMap((context) => context.pages())
        const page = pages[0]
        if (page && !page.isClosed()) {
          await page.screenshot({ path: SCREENSHOT_FILE, fullPage: true })
          result.screenshot = SCREENSHOT_FILE
        }
      }
    } catch (screenshotError) {
      result.screenshotError = redact(screenshotError.message)
    }
    writeResult(result)
    console.error(`${result.status}: ${result.reason}`)
    process.exitCode = error.blocked ? 2 : 1
  } finally {
    if (browser) {
      await browser.close()
    }
  }
}

void main()
