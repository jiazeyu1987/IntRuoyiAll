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
const resultPath = path.join(outputDir, 'mes-md-item-route-selection-write-real-result.json')
const screenshotPath = path.join(outputDir, 'mes-md-item-route-selection-write-real-bound.png')
const afterCleanupScreenshotPath = path.join(
  outputDir,
  'mes-md-item-route-selection-write-real-cleaned.png'
)

const config = {
  baseUrl: (process.env.MES_ITEM_ROUTE_WRITE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_ITEM_ROUTE_WRITE_E2E_TENANT || '测试租户',
  username: process.env.MES_ITEM_ROUTE_WRITE_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ITEM_ROUTE_WRITE_E2E_PASSWORD || '',
  timeout: Number(process.env.MES_ITEM_ROUTE_WRITE_E2E_TIMEOUT || 90000),
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  headed: process.env.MES_ITEM_ROUTE_WRITE_E2E_HEADED === '1'
}

const releaseTag = `ITEM_ROUTE_E2E_${Date.now().toString(36).toUpperCase()}`
const routeCode = `E2E-ITEM-ROUTE-${releaseTag.slice(-8)}`
const routeName = `E2E Item Route ${releaseTag.slice(-8)}`

function assertConfig() {
  assert.match(config.baseUrl, /^https?:\/\//, 'base URL must be HTTP(S)')
  assert.ok(['127.0.0.1', 'localhost', '::1', '[::1]'].includes(new URL(config.baseUrl).hostname))
  assert.equal(config.tenant, '测试租户', `write E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `write E2E must use aoteman, got ${config.username}`)
  assert.ok(config.password, 'MES_ITEM_ROUTE_WRITE_E2E_PASSWORD is required')
  if (config.executablePath) {
    assert.ok(fs.existsSync(config.executablePath), `Chrome not found: ${config.executablePath}`)
  }
}

function unwrap(payload) {
  return payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')
    ? payload.data
    : payload
}

function assertBusinessPayload(payload, label) {
  assert.ok(payload, `${label} missing JSON body`)
  assert.ok([0, 200].includes(payload.code), `${label} business code failed: ${payload.code} ${payload.msg || ''}`)
  return unwrap(payload)
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
  await form.waitFor({ state: 'visible', timeout: config.timeout })
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
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  assertBusinessPayload(await loginResponse.json(), 'login')
  await page.waitForURL((url) => !url.pathname.includes('/login'), {
    timeout: config.timeout,
    waitUntil: 'commit'
  })
}

async function browserApi(page, method, pathName, payload) {
  return await page.evaluate(
    async ({ method: requestMethod, pathName: requestPath, payload: requestPayload }) => {
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
      const init = {
        method: requestMethod,
        headers: {
          Authorization: `Bearer ${accessToken}`,
          ...(tenantId ? { 'tenant-id': String(tenantId) } : {}),
          'Cache-Control': 'no-cache',
          Pragma: 'no-cache'
        }
      }
      if (requestMethod === 'GET') {
        for (const [key, value] of Object.entries(requestPayload || {})) {
          if (value !== undefined && value !== null && value !== '') {
            url.searchParams.set(key, String(value))
          }
        }
      } else if (requestPayload !== undefined) {
        init.headers['Content-Type'] = 'application/json'
        init.body = JSON.stringify(requestPayload)
      }
      const response = await fetch(url.toString(), init)
      const body = await response.json().catch(() => undefined)
      return { ok: response.ok, status: response.status, body }
    },
    { method, pathName, payload }
  )
}

async function apiGet(page, pathName, params = {}) {
  const response = await browserApi(page, 'GET', pathName, params)
  assert.ok(response.ok, `${pathName} HTTP failed: ${response.status}`)
  return assertBusinessPayload(response.body, pathName)
}

async function apiDelete(page, pathName) {
  const response = await browserApi(page, 'DELETE', pathName)
  assert.ok(response.ok, `${pathName} HTTP failed: ${response.status}`)
  return assertBusinessPayload(response.body, pathName)
}

async function apiPost(page, pathName, payload) {
  const response = await browserApi(page, 'POST', pathName, payload)
  assert.ok(response.ok, `${pathName} HTTP failed: ${response.status}`)
  return assertBusinessPayload(response.body, pathName)
}

async function discoverProductCandidate(page) {
  const routes = await apiGet(page, '/mes/pro/route/item-binding-list')
  assert.ok(Array.isArray(routes), 'route item-binding-list must be an array')
  assert.ok(
    routes.every((route) => route.status === 0),
    'precondition expected current test tenant routes to be enabled before creating the task route'
  )

  for (let pageNo = 1; pageNo <= 5; pageNo += 1) {
    const pageData = await apiGet(page, '/mes/md/item/page', {
      pageNo,
      pageSize: 100,
      status: 0
    })
    const products = (pageData.list || []).filter((item) => item.itemOrProduct === 'PRODUCT')
    for (const item of products) {
      const binding = await apiGet(page, '/mes/pro/route-product/get-by-item', { itemId: item.id })
      if (!binding?.routeId) {
        return { item, originalBinding: binding || null, discoveryPageNo: pageNo }
      }
    }
  }
  throw new Error('BLOCKED: 测试租户前 5 页未找到未绑定工艺路线的启用产品')
}

async function createRouteThroughPage(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.getByRole('button', { name: /^新增$/ }).first().waitFor({
    state: 'visible',
    timeout: config.timeout
  })

  await page.getByRole('button', { name: /^新增$/ }).first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增工艺路线' }).first()
  await dialog.waitFor({ state: 'visible', timeout: config.timeout })
  const routeCodeInput = dialog.locator('input.el-input__inner:visible').nth(0)
  await routeCodeInput.waitFor({ state: 'visible', timeout: config.timeout })
  await routeCodeInput.fill(routeCode)
  const routeNameInput = dialog.locator('input.el-input__inner:visible').nth(1)
  await routeNameInput.waitFor({ state: 'visible', timeout: config.timeout })
  await routeNameInput.fill(routeName)

  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/create') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await dialog.getByRole('button', { name: /^保\s*存$/ }).click()
  const createResponse = await createResponsePromise
  assert.ok(createResponse.ok(), `route create HTTP failed: ${createResponse.status()}`)
  const routeId = assertBusinessPayload(await createResponse.json(), 'route create')
  await dialog.getByRole('button', { name: /^关\s*闭$/ }).click()
  await dialog.waitFor({ state: 'hidden', timeout: config.timeout }).catch(() => undefined)

  const route = await apiGet(page, '/mes/pro/route/get', { id: routeId })
  assert.equal(route.code, routeCode, 'created route code mismatch')
  assert.equal(route.status, 1, 'created route must remain disabled for product-side selection')
  return route
}

async function openProductRouteTab(page, product) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/md/item/page') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  await page.goto(`${config.baseUrl}/mes/md/item`, { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await pageResponsePromise

  const codeInput = page.locator('input[placeholder="请输入物料编码"]').filter({ visible: true }).first()
  await codeInput.fill(product.code)
  const searchResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/md/item/page') &&
      response.url().includes(encodeURIComponent(product.code)) &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  await page.getByRole('button', { name: /搜索/ }).first().click()
  await searchResponsePromise

  const row = page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: product.code }).first()
  await row.waitFor({ state: 'visible', timeout: config.timeout })
  await row.getByRole('button', { name: '编辑' }).first().click()

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '修改物料/产品' }).first()
  await dialog.waitFor({ state: 'visible', timeout: config.timeout })
  const routeListPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/item-binding-list') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  const routeBindingPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-product/get-by-item') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  await dialog.getByRole('tab', { name: '工艺路线' }).click()
  await routeListPromise
  await routeBindingPromise
  return dialog
}

async function selectAndSaveRoute(page, dialog, route) {
  const formItem = dialog.locator('.el-form-item').filter({ hasText: '工艺路线' }).first()
  await formItem.locator('.el-select').first().click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: route.code }).first()
  await option.waitFor({ state: 'visible', timeout: config.timeout })
  const optionClass = (await option.getAttribute('class')) || ''
  assert.ok(!optionClass.includes('is-disabled'), 'new disabled-status route option must be selectable')
  await option.click()

  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-product/save-by-item') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await dialog.getByRole('button', { name: '保存工艺路线' }).click()
  const saveResponse = await saveResponsePromise
  assert.ok(saveResponse.ok(), `save-by-item HTTP failed: ${saveResponse.status()}`)
  assertBusinessPayload(await saveResponse.json(), 'save-by-item')
}

async function run() {
  assertConfig()
  fs.mkdirSync(outputDir, { recursive: true })

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.executablePath || undefined
  })
  const context = await browser.newContext({ viewport: { width: 1600, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()

  const evidence = {
    status: 'RUNNING',
    identityLabel: `${config.tenant}/${config.username}`,
    baseUrl: config.baseUrl,
    targetPage: '/mes/md/item',
    releaseTag,
    routeCode,
    routeName,
    writeRequests: [],
    targetNetworkFailures: [],
    targetHttpErrors: [],
    consoleErrors: [],
    pageErrors: [],
    cleanup: {}
  }

  const isTargetUrl = (url) =>
    url.includes('/admin-api/mes/pro/route/create') ||
    url.includes('/admin-api/mes/pro/route/delete') ||
    url.includes('/admin-api/mes/pro/route-product/save-by-item') ||
    url.includes('/admin-api/mes/pro/route-product/get-by-item') ||
    url.includes('/admin-api/mes/pro/route/item-binding-list') ||
    url.includes('/admin-api/mes/md/item/page')

  let captureTargetTraffic = false

  page.on('request', (request) => {
    const method = request.method()
    const url = request.url()
    if (isTargetUrl(url) && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      evidence.writeRequests.push({ method, url: url.replace(/\?.*$/, '') })
    }
  })
  page.on('requestfailed', (request) => {
    if (isTargetUrl(request.url())) {
      evidence.targetNetworkFailures.push({
        method: request.method(),
        url: request.url(),
        failure: request.failure()?.errorText || ''
      })
    }
  })
  page.on('response', (response) => {
    if (isTargetUrl(response.url()) && response.status() >= 400) {
      evidence.targetHttpErrors.push({
        status: response.status(),
        url: response.url().replace(/\?.*$/, '')
      })
    }
  })
  page.on('console', (message) => {
    if (!captureTargetTraffic) return
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => {
    if (!captureTargetTraffic) return
    evidence.pageErrors.push(error.message)
  })

  let createdRoute
  let selectedProduct
  try {
    await login(page)
    captureTargetTraffic = true
    const candidate = await discoverProductCandidate(page)
    selectedProduct = candidate.item
    evidence.selectedProduct = {
      id: selectedProduct.id,
      code: selectedProduct.code,
      name: selectedProduct.name,
      discoveryPageNo: candidate.discoveryPageNo
    }

    createdRoute = await createRouteThroughPage(page)
    evidence.createdRoute = {
      id: createdRoute.id,
      code: createdRoute.code,
      name: createdRoute.name,
      status: createdRoute.status
    }

    const dialog = await openProductRouteTab(page, selectedProduct)
    await selectAndSaveRoute(page, dialog, createdRoute)

    const bindingAfterSave = await apiGet(page, '/mes/pro/route-product/get-by-item', {
      itemId: selectedProduct.id
    })
    assert.equal(bindingAfterSave.routeId, createdRoute.id, 'saved binding routeId mismatch')
    evidence.bindingAfterSave = {
      id: bindingAfterSave.id,
      itemId: bindingAfterSave.itemId,
      routeId: bindingAfterSave.routeId,
      routeCode: bindingAfterSave.routeCode
    }
    await page.screenshot({ path: screenshotPath, fullPage: true })

    await apiPost(page, '/mes/pro/route-product/save-by-item', {
      itemId: selectedProduct.id,
      routeId: null
    })
    const bindingAfterUnbind = await apiGet(page, '/mes/pro/route-product/get-by-item', {
      itemId: selectedProduct.id
    })
    assert.ok(!bindingAfterUnbind?.routeId, 'cleanup unbind must remove route binding')
    evidence.cleanup.unbound = true

    await apiDelete(page, `/mes/pro/route/delete?id=${createdRoute.id}`)
    evidence.cleanup.routeDeleted = true

    const cleanupDialog = await openProductRouteTab(page, selectedProduct)
    const cleanupRouteFormItem = cleanupDialog
      .locator('.el-form-item')
      .filter({ hasText: '工艺路线' })
      .first()
    const cleanupRouteText = (await cleanupRouteFormItem.innerText()).replace(/\s+/g, ' ').trim()
    assert.ok(
      !cleanupRouteText.includes(createdRoute.code),
      'cleanup page reload must not display the deleted task route'
    )
    evidence.cleanup.pageReloadShowsNoTaskRoute = true
    await page.screenshot({ path: afterCleanupScreenshotPath, fullPage: true })

    assert.deepEqual(evidence.targetNetworkFailures, [], 'target network failures must be empty')
    assert.deepEqual(evidence.targetHttpErrors, [], 'target HTTP errors must be empty')
    assert.deepEqual(evidence.pageErrors, [], 'page errors must be empty')
    assert.equal(evidence.consoleErrors.length, 0, 'console error count must be zero')

    evidence.status = 'PASS'
    evidence.screenshot = screenshotPath
    evidence.afterCleanupScreenshot = afterCleanupScreenshotPath
    evidence.resultPath = resultPath
    evidence.checkedAt = new Date().toISOString()
    fs.writeFileSync(resultPath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.log(JSON.stringify(evidence, null, 2))
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error.message
    evidence.checkedAt = new Date().toISOString()
    if (createdRoute && selectedProduct) {
      try {
        await apiPost(page, '/mes/pro/route-product/save-by-item', {
          itemId: selectedProduct.id,
          routeId: null
        })
        evidence.cleanup.unboundAfterFailure = true
      } catch (cleanupError) {
        evidence.cleanup.unbindAfterFailureError = cleanupError.message
      }
    }
    if (createdRoute) {
      try {
        await apiDelete(page, `/mes/pro/route/delete?id=${createdRoute.id}`)
        evidence.cleanup.routeDeletedAfterFailure = true
      } catch (cleanupError) {
        evidence.cleanup.routeDeleteAfterFailureError = cleanupError.message
      }
    }
    fs.writeFileSync(resultPath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
