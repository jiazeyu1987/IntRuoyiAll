const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const taskRoot = path.resolve(repoRoot, '../doc/tasks/20260807-team-leader-process-config-responsible-routes')
const evidenceDir = path.join(taskRoot, 'evidence', 'real-browser')
const targetRouteNames = ['球囊扩张压力泵', '按压式球囊扩充压力泵']

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {}
  return fs.readFileSync(filePath, 'utf8').split(/\r?\n/).reduce((result, line) => {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/)
    if (!match || match[1].startsWith('#')) return result
    let value = match[2]
    if ((value.startsWith('"') && value.endsWith('"'))
      || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1)
    }
    result[match[1]] = value
    return result
  }, {})
}

function loadLoginConfig() {
  const baseEnv = readEnvFile(path.join(repoRoot, '.env'))
  const localEnv = readEnvFile(path.join(repoRoot, '.env.local'))
  return {
    baseUrl: process.env.TEAM_LEADER_RESPONSIBLE_ROUTES_BASE_URL || 'http://127.0.0.1:8081',
    tenant: process.env.TEAM_LEADER_RESPONSIBLE_ROUTES_TENANT
      || localEnv.VITE_APP_DEFAULT_LOGIN_TENANT
      || baseEnv.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: process.env.TEAM_LEADER_RESPONSIBLE_ROUTES_USERNAME
      || localEnv.VITE_APP_DEFAULT_LOGIN_USERNAME
      || baseEnv.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: process.env.TEAM_LEADER_RESPONSIBLE_ROUTES_PASSWORD
      || localEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD
      || baseEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
      || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  }
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible')
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if (await tenantInput.isVisible().catch(() => false)) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', {
      hasText: config.tenant
    })
    if (await tenantOption.count()) {
      await tenantOption.first().click()
    } else {
      await tenantInput.press('Enter')
    }
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
    .fill(config.username)
  await form.locator('input[type="password"]:visible').first().fill(config.password)
  await form.getByRole('button', { name: /登录/ }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function responseBody(response) {
  assert.equal(response.status(), 200, `unexpected HTTP status for ${response.url()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `unexpected business response for ${response.url()}: ${body.msg}`)
  return body
}

async function run() {
  const config = loadLoginConfig()
  assert.match(config.baseUrl, /^http:\/\/(127\.0\.0\.1|localhost):8081$/)
  assert.equal(config.tenant, '芋道源码', 'verification must use the screenshot tenant identity')
  assert.equal(config.username, 'admin', 'verification must use the screenshot admin identity')
  assert.ok(config.password, 'default local login password is missing')
  assert.ok(fs.existsSync(config.executablePath), `browser executable missing: ${config.executablePath}`)
  fs.mkdirSync(evidenceDir, { recursive: true })

  const browser = await chromium.launch({ headless: true, executablePath: config.executablePath })
  const context = await browser.newContext({ viewport: { width: 1680, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const consoleErrors = []
  const writeRequests = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/')
      && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      writeRequests.push({ method: request.method(), url: request.url() })
    }
  })

  try {
    await login(page, config)
    const responsibilityResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/process-pool/team-leader/responsible-routes'),
      { timeout: 60000 }
    )
    const processConfigResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/process-pool/team-leader/process-config/list'),
      { timeout: 60000 }
    )
    await page.goto(`${config.baseUrl}/mes/pro/process-pool/team-leader`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })

    const responsibilityBody = await responseBody(await responsibilityResponsePromise)
    const responsibleRoutes = responsibilityBody.data || []
    const apiRouteNames = responsibleRoutes.map((route) => String(route.routeName || '').trim())
    assert.deepEqual(apiRouteNames, targetRouteNames)

    const header = page.locator('[data-production-leader-responsible-routes]:visible').first()
    try {
      await header.waitFor({ state: 'visible', timeout: 60000 })
    } catch (error) {
      await page.screenshot({ path: path.join(evidenceDir, 'admin-responsible-routes-failure.png'), fullPage: true })
      const visibleText = (await page.locator('body').innerText()).slice(0, 1500)
      throw new Error(`${error.message}; url=${page.url()}; visibleText=${visibleText}`)
    }
    const visibleRouteNames = (await header.locator('.el-tag').allTextContents()).map((name) => name.trim())
    assert.deepEqual(visibleRouteNames, targetRouteNames)
    const headerScreenshot = path.join(evidenceDir, 'admin-responsible-routes-header.png')
    await header.screenshot({ path: headerScreenshot })

    const processConfigBody = await responseBody(await processConfigResponsePromise)
    const processConfigRows = processConfigBody.data || []
    const processConfigRouteNames = [...new Set(processConfigRows
      .map((row) => String(row.routeName || '').trim())
      .filter(Boolean))]
    assert.deepEqual(processConfigRouteNames, targetRouteNames,
      'process-config rows must come only from formal responsible routes')

    const processConfigTab = page.locator(
      '[data-production-leader-module-tab-process-config]:visible'
    ).first()
    if (await processConfigTab.isVisible().catch(() => false)) {
      await processConfigTab.click()
    }
    const processConfigTable = page.locator('[data-team-leader-process-config-table]:visible').first()
    await processConfigTable.waitFor({ state: 'visible', timeout: 30000 })
    await processConfigTable.scrollIntoViewIfNeeded()
    await page.screenshot({ path: path.join(evidenceDir, 'admin-responsible-routes.png'), fullPage: true })

    assert.deepEqual(writeRequests, [], 'read-only verification must not write MES data')
    assert.deepEqual(pageErrors, [], 'page must not raise runtime errors')
    assert.deepEqual(consoleErrors, [], 'page must not log console errors')

    const result = {
      identity: '芋道源码/admin',
      responsibleRoutes,
      visibleRouteNames,
      processConfigRouteNames,
      processConfigRowCount: processConfigRows.length,
      hasNonResponsibilityProcessConfigRoute: false,
      writeRequests,
      pageErrors,
      consoleErrors,
      headerScreenshot,
      screenshot: path.join(evidenceDir, 'admin-responsible-routes.png')
    }
    fs.writeFileSync(path.join(evidenceDir, 'result.json'), JSON.stringify(result, null, 2), 'utf8')
    console.log(JSON.stringify({
      identity: result.identity,
      visibleRouteNames: result.visibleRouteNames,
      processConfigRouteNames: result.processConfigRouteNames,
      processConfigRowCount: result.processConfigRowCount,
      writeRequestCount: result.writeRequests.length,
      pageErrorCount: result.pageErrors.length,
      consoleErrorCount: result.consoleErrors.length
    }))
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error.stack || error.message)
  process.exitCode = 1
})
