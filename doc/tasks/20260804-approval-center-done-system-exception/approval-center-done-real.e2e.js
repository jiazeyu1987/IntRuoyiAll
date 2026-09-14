const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

function readEnvFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf8')
  const entries = new Map()
  for (const line of content.split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/)
    if (!match) continue
    let value = match[2]
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    entries.set(match[1], value)
  }
  return entries
}

const defaultLogin = readEnvFile(path.join(frontendRoot, '.env'))

const config = {
  baseUrl: (process.env.APPROVAL_CENTER_DONE_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  backendUrl: (process.env.APPROVAL_CENTER_DONE_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_CENTER_DONE_TENANT || defaultLogin.get('VITE_APP_DEFAULT_LOGIN_TENANT'),
  username: process.env.APPROVAL_CENTER_DONE_USERNAME || defaultLogin.get('VITE_APP_DEFAULT_LOGIN_USERNAME'),
  password: process.env.APPROVAL_CENTER_DONE_PASSWORD || defaultLogin.get('VITE_APP_DEFAULT_LOGIN_PASSWORD'),
  targetPath: '/approval-center/done',
  taskDir: path.join(__dirname, 'e2e-artifacts')
}

assert.ok(config.tenant, 'default login tenant is missing')
assert.ok(config.username, 'default login username is missing')
assert.ok(config.password, 'default login password is missing')

const artifacts = {
  result: path.join(config.taskDir, 'approval-center-done-real-result.json'),
  screenshot: path.join(config.taskDir, 'approval-center-done-real.png')
}

let currentStage = 'init'

function markStage(stage) {
  currentStage = stage
  process.stdout.write(`STAGE: ${stage}\n`)
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function extractData(payload) {
  assert.ok(isSuccessPayload(payload), `page api failed: ${JSON.stringify(payload)}`)
  assert.ok(payload.data && typeof payload.data.total === 'number', `page api missing total: ${JSON.stringify(payload)}`)
  assert.ok(Array.isArray(payload.data.list), `page api missing list: ${JSON.stringify(payload)}`)
  return payload.data
}

function tryUrl(value) {
  try {
    return new URL(value)
  } catch {
    return null
  }
}

function isDonePageResponse(response) {
  if (response.request().method() !== 'GET') return false
  const url = tryUrl(response.url())
  return (
    url &&
    url.origin === new URL(config.baseUrl).origin &&
    url.pathname === '/admin-api/approval-center/tasks/page' &&
    url.searchParams.get('viewType') === 'DONE'
  )
}

function isApprovalCenterTargetUrl(value) {
  const url = tryUrl(value)
  if (!url || url.origin !== new URL(config.baseUrl).origin) return false
  return url.pathname === '/approval-center/done' || url.pathname.startsWith('/admin-api/approval-center/')
}

function isDoneTargetUrl(value) {
  const url = tryUrl(value)
  if (!url || url.origin !== new URL(config.baseUrl).origin) return false
  if (url.pathname === '/approval-center/done') return true
  return (
    url.pathname === '/admin-api/approval-center/tasks/page' &&
    url.searchParams.get('viewType') === 'DONE'
  )
}

function targetPathWithQuery(value) {
  const url = tryUrl(value)
  return url ? `${url.pathname}${url.search}` : value
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
}

async function firstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) return item
  }
  throw new Error(`visible element missing: ${label}`)
}

async function selectTenant(page, form) {
  const tenantInputs = form.locator('.el-select input[role="combobox"], input.el-select__input')
  if ((await tenantInputs.count()) === 0) return
  const tenantInput = await firstVisible(tenantInputs, 'tenant input')
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
  await tenantOption.click()
}

async function login(page) {
  await page.context().clearCookies()
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, form)

  const usernameInput = await firstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    'username input'
  )
  await usernameInput.fill('')
  await usernameInput.fill(config.username)

  const passwordInput = await firstVisible(form.locator('input[type="password"]'), 'password input')
  await passwordInput.fill('')
  await passwordInput.fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginPayload = await (await loginResponsePromise).json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `login failed for ${config.tenant}/${config.username}: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function collectDonePageState(page) {
  return page.evaluate(() => {
    const isVisible = (element) => {
      if (!(element instanceof HTMLElement)) return false
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    }
    const rows = Array.from(document.querySelectorAll('.approval-center__table .el-table__body-wrapper tbody tr'))
      .filter((row) => isVisible(row) && (row.textContent || '').trim())
    const emptyText = Array.from(document.querySelectorAll('.approval-center__table .el-table__empty-text'))
      .find((node) => isVisible(node))
    const activeTexts = Array.from(document.querySelectorAll('.is-active, .router-link-active'))
      .filter(isVisible)
      .map((node) => (node.textContent || '').replace(/\s+/g, ' ').trim())
      .filter(Boolean)
    return {
      url: window.location.href,
      bodyText: document.body.innerText || '',
      visibleRowCount: rows.length,
      emptyText: emptyText ? (emptyText.textContent || '').replace(/\s+/g, ' ').trim() : '',
      activeTexts
    }
  })
}

async function main() {
  fs.mkdirSync(config.taskDir, { recursive: true })
  const launchOptions = { headless: true }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  markStage('launch browser')
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const consoleErrors = []
  const targetNetworkFailures = []
  const nonDoneApprovalCenterNetworkFailures = []
  const externalNetworkFailures = []
  const targetBadResponses = []
  const targetWriteRequests = []
  let trackTarget = false

  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('request', (request) => {
    if (!trackTarget) return
    if (!isApprovalCenterTargetUrl(request.url())) return
    if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      targetWriteRequests.push({ method: request.method(), path: targetPathWithQuery(request.url()) })
    }
  })
  page.on('requestfailed', (request) => {
    const entry = {
      method: request.method(),
      url: request.url(),
      failure: request.failure()?.errorText || 'unknown'
    }
    if (isDoneTargetUrl(request.url())) {
      targetNetworkFailures.push({ ...entry, url: targetPathWithQuery(request.url()) })
    } else if (isApprovalCenterTargetUrl(request.url())) {
      nonDoneApprovalCenterNetworkFailures.push({ ...entry, url: targetPathWithQuery(request.url()) })
    } else {
      externalNetworkFailures.push(entry)
    }
  })
  page.on('response', (response) => {
    if (!trackTarget || !isDoneTargetUrl(response.url())) return
    if (response.status() >= 400) {
      targetBadResponses.push({
        status: response.status(),
        method: response.request().method(),
        path: targetPathWithQuery(response.url())
      })
    }
  })

  try {
    markStage('login')
    await login(page)
    trackTarget = true
    markStage('open done page')
    const doneResponsePromise = page.waitForResponse(isDonePageResponse, { timeout: 60000 })
    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    markStage('wait done response')
    const doneResponse = await doneResponsePromise
    const donePayload = await doneResponse.json()
    const pageData = extractData(donePayload)
    markStage('wait done ui')
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page, 30000)
    markStage('collect done state')
    const state = await collectDonePageState(page)
    await page.screenshot({ path: artifacts.screenshot, fullPage: true })

    assert.equal(new URL(state.url).pathname, config.targetPath, `DONE page route mismatch: ${state.url}`)
    assert.match(state.bodyText, /已办/, 'DONE page should expose 已办 navigation text')
    assert.doesNotMatch(state.bodyText, /系统异常|APPROVAL_RESULT_UNSUPPORTED/, 'DONE page must not show system exception')
    if (pageData.list.length === 0) {
      assert.match(state.emptyText, /暂无审批任务/, `empty DONE page should show empty state: ${JSON.stringify(state)}`)
    } else {
      assert.ok(state.visibleRowCount > 0, `DONE page should render rows: ${JSON.stringify(state)}`)
    }

    const targetConsoleErrors = consoleErrors.filter((message) =>
      /approval-center|tasks\/page|系统异常|APPROVAL_RESULT_UNSUPPORTED/i.test(message)
    )
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)
    assert.deepEqual(targetConsoleErrors, [], `target console errors: ${targetConsoleErrors.join(' || ')}`)
    assert.deepEqual(targetNetworkFailures, [], `target network failures: ${JSON.stringify(targetNetworkFailures)}`)
    assert.deepEqual(targetBadResponses, [], `target bad responses: ${JSON.stringify(targetBadResponses)}`)
    assert.deepEqual(targetWriteRequests, [], `DONE readonly path must not write: ${JSON.stringify(targetWriteRequests)}`)

    const result = {
      baseUrl: config.baseUrl,
      backendUrl: config.backendUrl,
      identity: `${config.tenant}/${config.username}`,
      targetPath: config.targetPath,
      doneRequest: {
        status: doneResponse.status(),
        code: donePayload.code,
        total: pageData.total,
        listSize: pageData.list.length,
        requestPath: targetPathWithQuery(doneResponse.url())
      },
      ui: {
        route: new URL(state.url).pathname,
        visibleRowCount: state.visibleRowCount,
        emptyText: state.emptyText,
        activeTexts: state.activeTexts
      },
      targetWriteRequestCount: targetWriteRequests.length,
      pageErrors,
      consoleErrors,
      targetNetworkFailures,
      nonDoneApprovalCenterNetworkFailures,
      externalNetworkFailures,
      screenshot: artifacts.screenshot
    }
    fs.writeFileSync(artifacts.result, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`PASS: approval center DONE real E2E\n${JSON.stringify(result, null, 2)}\n`)
  } finally {
    markStage('close browser')
    trackTarget = false
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

Promise.race([
  main(),
  new Promise((_, reject) => {
    setTimeout(() => reject(new Error(`approval_center_done_e2e_timeout:${currentStage}`)), 180000)
  })
]).then(() => {
  process.exit(0)
}).catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
