const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const taskDir = __dirname
const repoRoot = path.resolve(taskDir, '..', '..', '..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

const outputDir = path.join(repoRoot, 'output', 'playwright', '20260804-mes-item-route-selection')
const resultPath = path.join(outputDir, 'mes-md-item-route-selection-readonly-real-result.json')
const screenshotPath = path.join(outputDir, 'mes-md-item-route-selection-readonly-real.png')

function parseEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {}
  const result = {}
  for (const rawLine of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const match = line.match(/^([^=]+?)\s*=\s*(.*)$/)
    if (!match) continue
    result[match[1].trim()] = match[2].trim().replace(/^['"]|['"]$/g, '')
  }
  return result
}

const envFile = parseEnvFile(path.join(frontendRoot, '.env'))
const config = {
  baseUrl: (process.env.MES_ITEM_ROUTE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  backendUrl: (process.env.MES_ITEM_ROUTE_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
  tenant: process.env.MES_ITEM_ROUTE_E2E_TENANT || envFile.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: process.env.MES_ITEM_ROUTE_E2E_USERNAME || envFile.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: process.env.MES_ITEM_ROUTE_E2E_PASSWORD || envFile.VITE_APP_DEFAULT_LOGIN_PASSWORD,
  timeout: Number(process.env.MES_ITEM_ROUTE_E2E_TIMEOUT || 90000),
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  headed: process.env.MES_ITEM_ROUTE_E2E_HEADED === '1'
}

function assertLocalOnly(url, label) {
  const hostname = new URL(url).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `${label} must stay local, got ${url}`
  )
}

function requireText(name, value) {
  assert.ok(String(value || '').trim(), `${name} is required`)
}

function unwrap(payload) {
  return payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')
    ? payload.data
    : payload
}

function writeResult(payload) {
  fs.mkdirSync(outputDir, { recursive: true })
  fs.writeFileSync(resultPath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

async function firstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`No visible element found for ${label}`)
}

async function fillFirstVisible(locator, value, label) {
  const input = await firstVisible(locator, label)
  await input.fill(value)
}

async function selectTenant(page, form) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await option.waitFor({ state: 'visible', timeout: config.timeout })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  const loginEntry = await Promise.race([
    form.waitFor({ state: 'visible', timeout: config.timeout }).then(() => 'form'),
    page
      .waitForURL((url) => !url.pathname.includes('/login'), {
        timeout: config.timeout,
        waitUntil: 'commit'
      })
      .then(() => 'redirect')
  ])
  if (loginEntry === 'redirect') return
  await selectTenant(page, form)
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code failed: ${loginPayload.code}`)
  const permissionResponse = await permissionResponsePromise
  const permissionPayload = await permissionResponse.json()
  assert.ok([0, 200].includes(permissionPayload.code), 'permission info must return business code 0')
  await page.waitForURL((url) => !url.pathname.includes('/login'), {
    timeout: config.timeout,
    waitUntil: 'commit'
  })
}

async function authenticatedGet(page, pathName, params = {}) {
  return await page.evaluate(
    async ({ pathName: requestPath, params: requestParams }) => {
      const parseCacheValue = (raw) => {
        if (!raw) return ''
        try {
          const parsed = JSON.parse(raw)
          if (parsed && typeof parsed === 'object') {
            if (Object.prototype.hasOwnProperty.call(parsed, 'v')) {
              try {
                return JSON.parse(parsed.v)
              } catch {
                return parsed.v
              }
            }
            if (Object.prototype.hasOwnProperty.call(parsed, 'data')) return parsed.data
            if (Object.prototype.hasOwnProperty.call(parsed, 'value')) return parsed.value
          }
          return parsed
        } catch {
          return raw
        }
      }
      const accessToken = parseCacheValue(localStorage.getItem('ACCESS_TOKEN'))
      const tenantId = parseCacheValue(localStorage.getItem('tenantId'))
      if (!accessToken) throw new Error('missing_access_token_after_login')
      const url = new URL(`/admin-api${requestPath}`, window.location.origin)
      for (const [key, value] of Object.entries(requestParams || {})) {
        if (value !== undefined && value !== null && value !== '') {
          url.searchParams.set(key, String(value))
        }
      }
      const response = await fetch(url.toString(), {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          ...(tenantId ? { 'tenant-id': String(tenantId) } : {}),
          'Cache-Control': 'no-cache',
          Pragma: 'no-cache'
        }
      })
      const body = await response.json().catch(() => undefined)
      return { ok: response.ok, status: response.status, body }
    },
    { pathName, params }
  )
}

function assertCommonResult(response, label) {
  assert.ok(response.ok, `${label} HTTP failed: ${response.status}`)
  assert.ok(response.body, `${label} missing JSON body`)
  assert.ok([0, 200].includes(response.body.code), `${label} business code failed: ${response.body.code}`)
  return unwrap(response.body)
}

async function discoverProductCandidate(page) {
  const routeResponse = await authenticatedGet(page, '/mes/pro/route/item-binding-list')
  const routes = assertCommonResult(routeResponse, 'route item binding list')
  assert.ok(Array.isArray(routes) && routes.length > 0, 'BLOCKED: no route options available')

  let selected = null
  let scanned = 0
  for (let pageNo = 1; pageNo <= 5 && !selected; pageNo += 1) {
    const itemResponse = await authenticatedGet(page, '/mes/md/item/page', {
      pageNo,
      pageSize: 100,
      status: 0
    })
    const pageData = assertCommonResult(itemResponse, `item page ${pageNo}`)
    const items = Array.isArray(pageData.list) ? pageData.list : []
    scanned += items.length
    for (const item of items.filter((row) => row.itemOrProduct === 'PRODUCT')) {
      const bindingResponse = await authenticatedGet(page, '/mes/pro/route-product/get-by-item', {
        itemId: item.id
      })
      const binding = assertCommonResult(bindingResponse, `route binding for item ${item.id}`)
      const currentRoute = binding?.routeId ? routes.find((route) => route.id === binding.routeId) : undefined
      if (!currentRoute || currentRoute.status !== 0) {
        selected = { item, binding, currentRoute, locked: false, discoveryPageNo: pageNo }
        break
      }
      if (!selected) {
        selected = { item, binding, currentRoute, locked: true, discoveryPageNo: pageNo }
      }
    }
  }
  assert.ok(selected, `BLOCKED: scanned ${scanned} enabled items but found no PRODUCT row`)
  return { ...selected, routes }
}

function isItemPageResponse(response) {
  return (
    response.url().includes('/admin-api/mes/md/item/page') &&
    response.request().method() === 'GET'
  )
}

function isTargetRouteRequest(url) {
  return (
    url.includes('/admin-api/mes/pro/route/item-binding-list') ||
    url.includes('/admin-api/mes/pro/route-product/get-by-item')
  )
}

async function openItemPageAndSearch(page, item) {
  await page.goto(`${config.baseUrl}/mes/md/item`, { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.getByText('物料产品管理', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  const itemPageResponsePromise = page.waitForResponse(isItemPageResponse, { timeout: config.timeout })
  const codeInput = page.locator('input[placeholder="请输入物料编码"]').filter({ visible: true }).first()
  await codeInput.fill(item.code)
  await page.getByRole('button', { name: /搜索|查询/ }).first().click()
  const itemPageResponse = await itemPageResponsePromise
  assert.ok(itemPageResponse.ok(), `item page search HTTP failed: ${itemPageResponse.status()}`)
  const itemPagePayload = await itemPageResponse.json()
  assert.ok([0, 200].includes(itemPagePayload.code), `item page search business code ${itemPagePayload.code}`)
  const visibleRow = page
    .locator('.el-table__body-wrapper tbody tr:visible')
    .filter({ hasText: item.code })
    .first()
  await visibleRow.waitFor({ state: 'visible', timeout: config.timeout })
  await visibleRow.getByRole('button', { name: '编辑' }).click()
}

async function waitForInputValue(page, locator, expected, label) {
  let lastValue = ''
  const deadline = Date.now() + config.timeout
  while (Date.now() < deadline) {
    try {
      lastValue = await locator.inputValue()
      if (lastValue === expected) return
    } catch {
      lastValue = ''
    }
    await page.waitForTimeout(250)
  }
  assert.equal(lastValue, expected, label)
}

async function verifyRouteTab(page, candidate, counters) {
  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.getByText('修改物料/产品', { exact: false }).waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  const codeInput = dialog.locator('input[placeholder="请输入物料编码"]').first()
  await codeInput.waitFor({ state: 'visible', timeout: config.timeout })
  await waitForInputValue(page, codeInput, candidate.item.code, 'dialog must open the selected product code')

  const routeListPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/item-binding-list') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  const bindingPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-product/get-by-item') &&
      response.url().includes(`itemId=${candidate.item.id}`) &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  await dialog.getByRole('tab', { name: '工艺路线' }).click()
  const [routeListResponse, bindingResponse] = await Promise.all([routeListPromise, bindingPromise])
  assert.ok(routeListResponse.ok(), `route option HTTP failed: ${routeListResponse.status()}`)
  assert.ok(bindingResponse.ok(), `binding HTTP failed: ${bindingResponse.status()}`)
  const routeListPayload = await routeListResponse.json()
  const bindingPayload = await bindingResponse.json()
  assert.ok([0, 200].includes(routeListPayload.code), `route option business code ${routeListPayload.code}`)
  assert.ok([0, 200].includes(bindingPayload.code), `binding business code ${bindingPayload.code}`)

  const routeForm = dialog.locator('.md-item-route-form').first()
  await routeForm.waitFor({ state: 'visible', timeout: config.timeout })
  await routeForm.getByText('生产数量、生产用时仍在工艺路线关联产品中维护').waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  await routeForm.getByText('工艺路线', { exact: true }).waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  const saveButton = routeForm.getByRole('button', { name: '保存工艺路线' }).first()
  await saveButton.waitFor({ state: 'visible', timeout: config.timeout })

  const currentRouteId = bindingPayload.data?.routeId
  const currentRoute = currentRouteId
    ? routeListPayload.data.find((route) => route.id === currentRouteId)
    : undefined
  const locked = currentRoute?.status === 0
  assert.equal(await saveButton.isDisabled(), locked, 'save button lock state must match current route status')
  if (locked) {
    await routeForm.getByText('当前工艺路线已启用，不能在产品侧变更或解除').waitFor({
      state: 'visible',
      timeout: config.timeout
    })
  } else {
    const select = routeForm.locator('.el-select').first()
    await select.click()
    const options = page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
    await options.first().waitFor({ state: 'visible', timeout: config.timeout })
    const optionTexts = await options.evaluateAll((nodes) =>
      nodes.map((node) => ({
        text: node.textContent?.replace(/\s+/g, ' ').trim() || '',
        disabled: node.classList.contains('is-disabled') || node.getAttribute('aria-disabled') === 'true'
      }))
    )
    assert.ok(optionTexts.length > 0, 'route selector must render visible options')
    const enabledRoutes = routeListPayload.data.filter((route) => route.status === 0)
    if (enabledRoutes.length > 0) {
      assert.ok(
        optionTexts.some((option) => option.disabled && option.text.includes('已启用，仅回显')),
        'enabled routes must be visible as disabled display-only options'
      )
    }
    await page.keyboard.press('Escape')
    counters.optionTexts = optionTexts
  }

  return {
    itemId: candidate.item.id,
    itemCode: candidate.item.code,
    itemName: candidate.item.name,
    routeOptionCount: routeListPayload.data.length,
    enabledRouteOptionCount: routeListPayload.data.filter((route) => route.status === 0).length,
    currentRouteId,
    locked,
    selectorOpened: !locked,
    routeListBusinessCode: routeListPayload.code,
    bindingBusinessCode: bindingPayload.code
  }
}

async function main() {
  assertLocalOnly(config.baseUrl, 'frontend base URL')
  assertLocalOnly(config.backendUrl, 'backend URL')
  for (const [name, value] of Object.entries({
    tenant: config.tenant,
    username: config.username,
    password: config.password
  })) {
    requireText(name, value)
  }
  assert.ok(Number.isFinite(config.timeout) && config.timeout > 0, 'timeout must be positive')
  assert.ok(fs.existsSync(config.executablePath), `Chrome executable not found: ${config.executablePath}`)

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1600, height: 1000 }, locale: 'zh-CN' })
  await context.clearCookies()
  const page = await context.newPage()
  page.setDefaultTimeout(config.timeout)
  page.setDefaultNavigationTimeout(config.timeout)

  const mesWriteRequests = []
  const simpleListRequests = []
  const targetNetworkFailures = []
  const targetHttpErrors = []
  const nonTargetHttpErrors = []
  const consoleErrors = []
  const pageErrors = []
  const counters = {}

  page.on('request', (request) => {
    const method = request.method().toUpperCase()
    const url = request.url()
    if (url.includes('/admin-api/mes/pro/route/simple-list')) {
      simpleListRequests.push({ method, url: new URL(url).pathname })
    }
    if (url.includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      mesWriteRequests.push({ method, path: new URL(url).pathname })
    }
  })
  page.on('requestfailed', (request) => {
    if (isTargetRouteRequest(request.url()) || request.url().includes('/admin-api/mes/md/item/page')) {
      targetNetworkFailures.push({
        method: request.method(),
        url: request.url(),
        errorText: request.failure()?.errorText || ''
      })
    }
  })
  page.on('response', (response) => {
    if (response.status() < 400) return
    const url = response.url()
    const entry = {
      status: response.status(),
      method: response.request().method(),
      url: new URL(url).pathname
    }
    if (isTargetRouteRequest(url) || url.includes('/admin-api/mes/md/item/page')) {
      targetHttpErrors.push(entry)
    } else {
      nonTargetHttpErrors.push(entry)
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  const result = {
    status: 'FAIL',
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    identityLabel: `${config.tenant}/${config.username}`,
    scope: 'readonly_real_frontend_path',
    targetPage: '/mes/md/item',
    writePathStatus: 'not_executed_readonly_scope',
    screenshot: screenshotPath,
    resultPath,
    checkedAt: new Date().toISOString()
  }

  try {
    await login(page)
    const candidate = await discoverProductCandidate(page)
    result.discovery = {
      selectedItemId: candidate.item.id,
      selectedItemCode: candidate.item.code,
      selectedItemName: candidate.item.name,
      discoveryPageNo: candidate.discoveryPageNo,
      precomputedLocked: candidate.locked,
      routeOptionCount: candidate.routes.length
    }

    await openItemPageAndSearch(page, candidate.item)
    const routeTabEvidence = await verifyRouteTab(page, candidate, counters)
    await page.screenshot({ path: screenshotPath, fullPage: true })

    assert.deepEqual(simpleListRequests, [], 'product-side route selector must not call route simple-list')
    assert.deepEqual(mesWriteRequests, [], 'read-only E2E must not emit MES write requests')
    assert.deepEqual(targetNetworkFailures, [], 'target network failures must be empty')
    assert.deepEqual(targetHttpErrors, [], 'target HTTP errors must be empty')
    assert.deepEqual(pageErrors, [], 'page runtime errors must be empty')

    Object.assign(result, {
      status: 'PASS',
      routeTabEvidence,
      disabledOptionSample: counters.optionTexts?.find((option) => option.disabled) || null,
      mesWriteRequests,
      simpleListRequests,
      targetNetworkFailures,
      targetHttpErrors,
      nonTargetHttpErrors,
      consoleErrorCount: consoleErrors.length,
      consoleErrorsSample: consoleErrors.slice(0, 5),
      pageErrors
    })
    writeResult(result)
    console.log(JSON.stringify(result, null, 2))
  } catch (error) {
    result.error = error.stack || error.message
    result.mesWriteRequests = mesWriteRequests
    result.simpleListRequests = simpleListRequests
    result.targetNetworkFailures = targetNetworkFailures
    result.targetHttpErrors = targetHttpErrors
    result.nonTargetHttpErrors = nonTargetHttpErrors
    result.consoleErrors = consoleErrors
    result.pageErrors = pageErrors
    writeResult(result)
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
