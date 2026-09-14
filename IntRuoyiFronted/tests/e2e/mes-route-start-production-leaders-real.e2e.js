const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const outputDir = path.resolve(
  workspaceRoot,
  'output',
  'playwright',
  '20260803-pressure-pump-leader-switch-scope'
)

function readEnvFiles() {
  const result = {}
  for (const fileName of ['.env', '.env.local']) {
    const filePath = path.join(repoRoot, fileName)
    if (!fs.existsSync(filePath)) continue
    const content = fs.readFileSync(filePath, 'utf8')
    for (const line of content.split(/\r?\n/)) {
      const trimmed = line.trim()
      if (!trimmed || trimmed.startsWith('#')) continue
      const separator = trimmed.indexOf('=')
      if (separator <= 0) continue
      const key = trimmed.slice(0, separator).trim()
      const value = trimmed
        .slice(separator + 1)
        .trim()
        .replace(/^['"]|['"]$/g, '')
      result[key] = value
    }
  }
  return result
}

const localEnv = readEnvFiles()
const requiredLoginValue = (envKey, fileKey) => {
  const value = process.env[envKey] || localEnv[fileKey]
  if (!value) {
    throw new Error(`Missing local E2E login precondition: ${envKey} or ${fileKey}`)
  }
  return value
}

const config = {
  baseUrl: (process.env.MES_ROUTE_START_PRODUCTION_LEADERS_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: requiredLoginValue(
    'MES_ROUTE_START_PRODUCTION_LEADERS_TENANT',
    'VITE_APP_DEFAULT_LOGIN_TENANT'
  ),
  username: requiredLoginValue(
    'MES_ROUTE_START_PRODUCTION_LEADERS_USERNAME',
    'VITE_APP_DEFAULT_LOGIN_USERNAME'
  ),
  password: requiredLoginValue(
    'MES_ROUTE_START_PRODUCTION_LEADERS_PASSWORD',
    'VITE_APP_DEFAULT_LOGIN_PASSWORD'
  ),
  routeCode: process.env.MES_ROUTE_START_PRODUCTION_LEADERS_ROUTE_CODE || '',
  headed: process.env.MES_ROUTE_START_PRODUCTION_LEADERS_HEADED === '1',
  timeout: Number(process.env.MES_ROUTE_START_PRODUCTION_LEADERS_TIMEOUT || 60000),
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
}

const ROUTE_LIST_PATH = '/mes/pro/route'
const ROUTE_LIST_API = '/admin-api/mes/pro/route/page'
const PRODUCTION_LINES_API =
  '/admin-api/mes/pro/route/flow-config/route-start-production-leader-production-lines'
const PRODUCTION_LEADERS_API =
  '/admin-api/mes/pro/route/flow-config/route-start-production-leaders'

function assertLocalOnly(baseUrl) {
  const hostname = new URL(baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${baseUrl}`
  )
}

function isTargetApi(url, apiPath) {
  return new URL(url).pathname === apiPath
}

function isMesWriteRequest(request) {
  return (
    request.url().includes('/admin-api/mes/') &&
    !['GET', 'HEAD', 'OPTIONS'].includes(request.method().toUpperCase())
  )
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => null)
  await page.waitForTimeout(1000)
}

async function waitForBusinessResponse(page, predicate, label, action) {
  const responsePromise = page.waitForResponse(
    (response) => predicate(response.url()) && response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  try {
    await action()
  } catch (error) {
    responsePromise.catch(() => undefined)
    throw error
  }
  const response = await responsePromise
  assert.ok(response.ok(), `${label} HTTP failed: ${response.status()}`)
  const payload = await response.json().catch(() => undefined)
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    assert.ok([0, 200].includes(payload.code), `${label} payload failed: code=${payload.code}`)
  }
  return { status: response.status(), payloadCode: payload ? payload.code : undefined }
}

function unwrapCacheValue(raw) {
  if (!raw) return null
  let current = raw
  for (let index = 0; index < 4; index += 1) {
    if (typeof current !== 'string') return current
    try {
      current = JSON.parse(current)
    } catch {
      return current
    }
    if (current && typeof current === 'object' && Object.prototype.hasOwnProperty.call(current, 'v')) {
      current = current.v
    }
  }
  return current
}

async function authHeaders(page) {
  const cache = await page.evaluate(() =>
    Object.fromEntries(
      Array.from({ length: localStorage.length }, (_, index) => {
        const key = localStorage.key(index)
        return [key, localStorage.getItem(key)]
      })
    )
  )
  const accessToken = unwrapCacheValue(cache.ACCESS_TOKEN)
  const tenantId = unwrapCacheValue(cache.tenantId)
  const visitTenantId = unwrapCacheValue(cache.visitTenantId)
  assert.ok(accessToken, 'ACCESS_TOKEN missing after real login')
  assert.ok(tenantId, 'tenantId missing after real login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function apiGet(page, apiPath) {
  const headers = await authHeaders(page)
  const response = await page.evaluate(
    async ({ url, requestHeaders }) => {
      const result = await fetch(url, { headers: requestHeaders })
      return {
        ok: result.ok,
        status: result.status,
        json: await result.json()
      }
    },
    {
      url: `${config.baseUrl}/admin-api${apiPath}`,
      requestHeaders: headers
    }
  )
  assert.ok(response.ok, `GET ${apiPath} HTTP failed: ${response.status}`)
  assert.ok(
    response.json && [0, 200].includes(response.json.code),
    `GET ${apiPath} payload failed: code=${response.json && response.json.code}`
  )
  return response.json.data
}

function routeRowsFromPageData(data) {
  if (Array.isArray(data)) return data
  if (data && Array.isArray(data.list)) return data.list
  if (data && Array.isArray(data.records)) return data.records
  if (data && Array.isArray(data.rows)) return data.rows
  return []
}

async function resolveRouteTarget(page) {
  if (config.routeCode) return { code: config.routeCode, configured: true }
  const pageSize = 100
  let probedRouteCount = 0
  let totalRouteCount = null
  for (let pageNo = 1; pageNo <= 10; pageNo += 1) {
    const pageData = await apiGet(page, `/mes/pro/route/page?pageNo=${pageNo}&pageSize=${pageSize}`)
    const routes = routeRowsFromPageData(pageData)
    if (typeof pageData?.total === 'number') totalRouteCount = pageData.total
    if (routes.length === 0) break
    for (const route of routes) {
      probedRouteCount += 1
      const routeId = Number(route && route.id)
      const code = route && route.code ? String(route.code) : ''
      if (!routeId || !code) continue
      const routeVersionId = Number(route.pendingRouteVersionId || route.activeRouteVersionId || 0)
      const params = new URLSearchParams({ routeId: String(routeId) })
      if (routeVersionId > 0) params.set('routeVersionId', String(routeVersionId))
      const productionLines = await apiGet(
        page,
        `/mes/pro/route/flow-config/route-start-production-leader-production-lines?${params}`
      )
      if (Array.isArray(productionLines) && productionLines.length > 0) {
        return {
          code,
          routeId,
          routeVersionId: routeVersionId > 0 ? routeVersionId : null,
          productionLineCount: productionLines.length,
          configured: false,
          probedRouteCount,
          totalRouteCount
        }
      }
    }
    if (totalRouteCount !== null && probedRouteCount >= totalRouteCount) break
  }
  throw new Error(
    `当前登录租户下未找到可配置生产组长负责范围的工艺路线，无法验证生产组长配置行；已扫描路线数=${probedRouteCount}` +
      (totalRouteCount === null ? '' : `，接口总数=${totalRouteCount}`)
  )
}

async function selectTenant(page, form) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: config.timeout })
    await tenantOption.click()
    return
  }
  await form.locator('input[placeholder="请输入租户名称"]').first().fill(config.tenant)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', ROUTE_LIST_PATH)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form)
  const usernameInput = form
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
  await usernameInput.fill('')
  await usernameInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  const permissionResponsePromise = page
    .waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/get-permission-info'),
      { timeout: config.timeout }
    )
    .catch((error) => error)
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  assert.ok(
    [0, 200].includes(loginPayload.code),
    `login payload failed: code=${loginPayload.code}, message=${loginPayload.msg || loginPayload.message || ''}`
  )
  const permissionResponse = await permissionResponsePromise
  if (permissionResponse instanceof Error) throw permissionResponse
  assert.ok(permissionResponse.ok(), `permission info HTTP failed: ${permissionResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
}

async function openRouteFlowGraph(page, routeTarget) {
  await waitForBusinessResponse(
    page,
    (url) => isTargetApi(url, ROUTE_LIST_API),
    'route list',
    async () => {
      await page.goto(new URL(ROUTE_LIST_PATH, config.baseUrl).toString(), {
        waitUntil: 'domcontentloaded',
        timeout: config.timeout
      })
    }
  )
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  await settle(page)

  if (routeTarget.code) {
    const routeCodeInput = page
      .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
      .first()
    await routeCodeInput.fill(routeTarget.code)
    await waitForBusinessResponse(
      page,
      (url) => isTargetApi(url, ROUTE_LIST_API),
      'route list query',
      async () => {
        await page.getByRole('button', { name: /查询|搜索/ }).first().click()
      }
    )
    await settle(page)
  }

  const rows = page.locator('.el-table__body-wrapper .el-table__row')
  const row = routeTarget.code ? rows.filter({ hasText: routeTarget.code }).first() : rows.first()
  await row.waitFor({ state: 'visible', timeout: config.timeout })
  const selectedRouteText = (await row.innerText()).replace(/\s+/g, ' ').trim()
  await row.getByRole('button', { name: '编辑' }).first().click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), {
    timeout: config.timeout
  })

  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: config.timeout })
  await editor.getByRole('tab', { name: '流转关系图' }).click()
  await editor.locator('.route-flow-graph-designer').waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  await editor.locator('[data-flow-node="route-boundary"][data-flow-boundary="START"]').waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  const maximizeButton = editor.locator('[data-flow-action="toggle-route-flow-maximize"]').first()
  await maximizeButton.waitFor({ state: 'visible', timeout: config.timeout })
  await maximizeButton.click()
  await editor.locator('.route-flow-graph-designer.is-maximized').waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  await settle(page)
  return { editor, selectedRouteText, usedMaximizedGraph: true }
}

async function openProductionLeaderPanel(page, editor, routeTarget) {
  const startNode = editor.locator('[data-flow-node="route-boundary"][data-flow-boundary="START"]').first()
  await startNode.click()
  const field = editor.locator('[data-flow-boundary-field="productionLeader"]').first()
  await field.waitFor({ state: 'visible', timeout: config.timeout })
  await field.click()

  const panel = editor.locator('[data-flow-panel="route-start-production-leader-detail"]').first()
  await panel.waitFor({ state: 'visible', timeout: config.timeout })

  const routeId = Number(routeTarget.routeId || new URL(page.url()).searchParams.get('id') || 0)
  assert.ok(routeId > 0, `routeId missing for production leader endpoint verification: ${page.url()}`)
  const params = new URLSearchParams({ routeId: String(routeId) })
  if (routeTarget.routeVersionId) params.set('routeVersionId', String(routeTarget.routeVersionId))
  const productionLines = await apiGet(
    page,
    `/mes/pro/route/flow-config/route-start-production-leader-production-lines?${params}`
  )
  const productionLeaders = await apiGet(
    page,
    `/mes/pro/route/flow-config/route-start-production-leaders?${params}`
  )
  assert.ok(
    Array.isArray(productionLines) && productionLines.length > 0,
    '生产组长 E2E 目标路线必须至少有一个当前工艺路线负责范围。'
  )

  if ((await panel.locator('[data-route-start-production-leader-production-line]').count()) === 0) {
    const addButton = panel.locator('[data-flow-action="add-route-start-production-leader"]').first()
    await addButton.waitFor({ state: 'visible', timeout: config.timeout })
    assert.ok(
      await addButton.isEnabled(),
      '生产组长面板未渲染配置行，且新增按钮不可用；当前路线没有可配置的负责范围。'
    )
    await addButton.click()
  }

  await panel
    .locator('[data-route-start-production-leader-production-line]')
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })
  await panel
    .locator('[data-route-start-production-leader-source-type]')
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })
  await panel
    .locator('[data-route-start-production-leader-candidate]')
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })

  const sourceType = panel.locator('[data-route-start-production-leader-source-type]').first()
  await sourceType.locator('input[role="combobox"], input.el-select__input').first().click()
  const sourceOptions = await page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .evaluateAll((items) => items.map((item) => item.textContent.replace(/\s+/g, ' ').trim()))
  assert.ok(sourceOptions.some((text) => text.includes('账号')), `source options missing 账号: ${sourceOptions}`)
  assert.ok(
    sourceOptions.some((text) => text.includes('权限角色') || text.includes('角色')),
    `source options missing 权限角色: ${sourceOptions}`
  )
  await page.keyboard.press('Escape')

  return {
    productionLinesCode: 0,
    productionLineCount: productionLines.length,
    productionLeadersCode: 0,
    productionLeaderCount: Array.isArray(productionLeaders) ? productionLeaders.length : null,
    sourceOptions
  }
}

async function main() {
  assertLocalOnly(config.baseUrl)
  if (config.executablePath) {
    assert.ok(fs.existsSync(config.executablePath), `Chrome not found: ${config.executablePath}`)
  }
  fs.mkdirSync(outputDir, { recursive: true })

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.executablePath || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1600, height: 980 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(config.timeout)
  page.setDefaultNavigationTimeout(config.timeout)

  const mesWriteRequests = []
  const targetNetworkFailures = []
  const consoleErrors = []
  const pageErrors = []

  page.on('request', (request) => {
    if (isMesWriteRequest(request)) {
      mesWriteRequests.push(`${request.method()} ${new URL(request.url()).pathname}`)
    }
  })
  page.on('requestfailed', (request) => {
    if (
      isTargetApi(request.url(), PRODUCTION_LINES_API) ||
      isTargetApi(request.url(), PRODUCTION_LEADERS_API)
    ) {
      targetNetworkFailures.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`)
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    const routeTarget = await resolveRouteTarget(page)
    const { editor, selectedRouteText, usedMaximizedGraph } = await openRouteFlowGraph(
      page,
      routeTarget
    )
    const panelEvidence = await openProductionLeaderPanel(page, editor, routeTarget)

    assert.deepEqual(mesWriteRequests, [], `read-only E2E emitted MES writes: ${mesWriteRequests.join(' | ')}`)
    assert.deepEqual(targetNetworkFailures, [], `target network failures: ${targetNetworkFailures.join(' | ')}`)
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' | ')}`)
    assert.deepEqual(consoleErrors, [], `console errors: ${consoleErrors.join(' | ')}`)

    const screenshot = path.join(outputDir, 'mes-route-start-production-leaders-real-pass.png')
    await page.screenshot({ path: screenshot, fullPage: true })
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routeTarget,
      selectedRouteText,
      usedMaximizedGraph,
      panelEvidence,
      mesWriteRequests,
      targetNetworkFailures,
      consoleErrors,
      pageErrors,
      screenshot
    }
    const resultPath = path.join(outputDir, 'mes-route-start-production-leaders-real-result.json')
    fs.writeFileSync(resultPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(JSON.stringify(result, null, 2))
  } catch (error) {
    const screenshot = path.join(outputDir, 'mes-route-start-production-leaders-real-failure.png')
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => undefined)
    const failure = {
      status: 'FAIL',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routeCodeFilter: config.routeCode || null,
      message: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined,
      mesWriteRequests,
      targetNetworkFailures,
      consoleErrors,
      pageErrors,
      screenshot
    }
    const failurePath = path.join(outputDir, 'mes-route-start-production-leaders-real-failure.json')
    fs.writeFileSync(failurePath, `${JSON.stringify(failure, null, 2)}\n`, 'utf8')
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
