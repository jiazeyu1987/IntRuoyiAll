const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')
const envPath = path.join(frontendRoot, '.env')

const STATUS_LABELS = {
  DRAFT: '草稿',
  PENDING_APPROVAL: '审核中',
  READY_TO_PUBLISH: '待生效',
  ACTIVE: '已生效',
  SUPERSEDED: '已替代',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}
const EFFECTIVE_HISTORY_STATUSES = new Set(['ACTIVE', 'SUPERSEDED'])
const READ_ONLY_MES_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

function readLoginDefaults() {
  const defaults = new Map()
  const envText = fs.readFileSync(envPath, 'utf8')
  for (const line of envText.split(/\r?\n/)) {
    const match = line.match(/^\s*(VITE_APP_DEFAULT_LOGIN_[A-Z]+)\s*=\s*(.+?)\s*$/)
    if (match) defaults.set(match[1], match[2].trim())
  }
  return {
    tenant: defaults.get('VITE_APP_DEFAULT_LOGIN_TENANT'),
    username: defaults.get('VITE_APP_DEFAULT_LOGIN_USERNAME'),
    password: defaults.get('VITE_APP_DEFAULT_LOGIN_PASSWORD')
  }
}

function normalizeBaseUrl(value) {
  return String(value || '').replace(/\/+$/, '')
}

function assertLocalUrl(url, label) {
  const parsed = new URL(url)
  assert.ok(['127.0.0.1', 'localhost', '::1', '[::1]'].includes(parsed.hostname), `${label} must be local: ${url}`)
  return parsed
}

function assertPairedRuntimeUrls(baseUrl, backendUrl) {
  const frontend = assertLocalUrl(baseUrl, 'frontend URL')
  const backend = assertLocalUrl(backendUrl, 'backend URL')
  const frontendPort = Number(frontend.port || 80)
  const backendPort = Number(backend.port || 80)
  const frontendSlot = frontendPort - 8081
  const backendSlot = backendPort - 48081
  assert.equal(frontendSlot, backendSlot, `frontend/backend ports must be from the same int_main slot: ${baseUrl} / ${backendUrl}`)
  assert.ok(frontendSlot >= 0 && frontendSlot <= 19, `unexpected int_main slot from ports: ${frontendSlot}`)
}

async function assertBackendHealth(backendUrl, timeout) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeout)
  try {
    const response = await fetch(`${backendUrl}/actuator/health`, { signal: controller.signal })
    assert.equal(response.ok, true, `backend health HTTP ${response.status}`)
    const payload = await response.json()
    assert.equal(payload.status, 'UP', `backend health is not UP: ${JSON.stringify(payload)}`)
  } finally {
    clearTimeout(timer)
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`Missing visible field: ${label}`)
}

async function selectTenant(page, form, tenant) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').filter({ visible: true }).first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form, config.tenant)
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
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  const loginBody = await loginResponse.json()
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: config.timeout })
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) return ''
  const normalizeString = (value) => {
    let current = String(value || '').trim()
    for (let index = 0; index < 3; index += 1) {
      if (!current.startsWith('"')) return current
      try {
        const parsed = JSON.parse(current)
        if (typeof parsed !== 'string' || parsed === current) return current.replace(/^"(.*)"$/, '$1')
        current = parsed
      } catch {
        return current.replace(/^"(.*)"$/, '$1')
      }
    }
    return current
  }
  const unwrap = (value) => {
    let current = value
    for (let index = 0; index < 6; index += 1) {
      if (!current || typeof current !== 'object') return typeof current === 'string' ? normalizeString(current) : current || ''
      if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
        current = current.accessToken
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'v')) {
        current = current.v
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'value')) {
        current = current.value
        continue
      }
      return current
    }
    return current || ''
  }
  try {
    return unwrap(JSON.parse(raw))
  } catch {
    return normalizeString(raw)
  }
}

async function buildAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const accessToken = readWsCacheValue(snapshot, 'ACCESS_TOKEN')
  const tenantId = readWsCacheValue(snapshot, 'tenantId')
  const visitTenantId = readWsCacheValue(snapshot, 'visitTenantId')
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function requestJson(page, config, headers, relativePath) {
  const response = await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const httpResponse = await fetch(requestUrl, { method: 'GET', headers: requestHeaders })
      const text = await httpResponse.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { rawText: text }
      }
      return { status: httpResponse.status, ok: httpResponse.ok, payload }
    },
    { requestUrl: `${config.baseUrl}${relativePath}`, requestHeaders: headers }
  )
  assert.equal(response.ok, true, `${relativePath} HTTP ${response.status}`)
  assert.ok(response.payload && (response.payload.code === 0 || response.payload.code === 200), `${relativePath} failed: ${JSON.stringify(response.payload)}`)
  return response.payload.data
}

async function findRouteWithCancelledAndEffectiveHistory(page, config, headers) {
  const pageSize = Number(process.env.MES_ROUTE_VERSION_LIST_E2E_ROUTE_PAGE_SIZE || 100)
  for (let pageNo = 1; pageNo <= 20; pageNo += 1) {
    const routePage = await requestJson(
      page,
      config,
      headers,
      `/admin-api/mes/pro/route/page?pageNo=${pageNo}&pageSize=${pageSize}`
    )
    const routes = Array.isArray(routePage?.list) ? routePage.list : []
    for (const route of routes) {
      if (!route?.id) continue
      const versions = await requestJson(
        page,
        config,
        headers,
        `/admin-api/mes/pro/route-version/list-by-route?routeId=${route.id}`
      )
      const versionList = Array.isArray(versions) ? versions : []
      const effectiveHistory = versionList.filter(
        (item) => item.active || EFFECTIVE_HISTORY_STATUSES.has(item.lifecycleStatus)
      )
      const hiddenNonEffective = versionList.filter(
        (item) => !item.active && !EFFECTIVE_HISTORY_STATUSES.has(item.lifecycleStatus)
      )
      const cancelled = hiddenNonEffective.filter((item) => item.lifecycleStatus === 'CANCELLED')
      if (cancelled.length > 0 && effectiveHistory.length > 0) {
        return { route, versions: versionList, cancelled, hiddenNonEffective, effectiveHistory }
      }
    }
    if (routes.length < pageSize || Number(routePage?.total || 0) <= pageNo * pageSize) break
  }
  throw new Error('BLOCKED: read-only API found no MES route with both CANCELLED and ACTIVE/SUPERSEDED versions.')
}

async function waitForRoutePage(page, config, routeCode) {
  const routePageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: config.timeout }
  )
  await page.goto(`${config.baseUrl}/mes/pro/route?code=${encodeURIComponent(routeCode)}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const routeResponse = await routePageResponsePromise
  const routePayload = await routeResponse.json().catch(() => null)
  assert.ok(routePayload && (routePayload.code === 0 || routePayload.code === 200), `route page failed: ${JSON.stringify(routePayload)}`)
}

async function openVersionWorkspace(page, config, target) {
  await waitForRoutePage(page, config, target.route.code)
  await page.getByText('MES 工艺路线', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout }).catch(() => null)
  const rows = page.locator('.el-table__body-wrapper tbody tr:visible')
  await rows.first().waitFor({ state: 'visible', timeout: config.timeout })
  const targetRow = rows.filter({ hasText: target.route.code }).first()
  await targetRow.waitFor({ state: 'visible', timeout: config.timeout })
  const rowText = await targetRow.innerText()
  assert.ok(rowText.includes(target.route.code), `target route row should include route code ${target.route.code}`)

  const versionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-version/list-by-route') &&
      response.url().includes(`routeId=${target.route.id}`) &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: config.timeout }
  )
  const versionButtons = page.locator('[data-testid="route-version-workspace"]:visible')
  await versionButtons.first().waitFor({ state: 'visible', timeout: config.timeout })
  await versionButtons.first().click()
  const versionResponse = await versionResponsePromise
  const versionPayload = await versionResponse.json().catch(() => null)
  assert.ok(versionPayload && (versionPayload.code === 0 || versionPayload.code === 200), `route version list failed: ${JSON.stringify(versionPayload)}`)

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺路线版本' }).last()
  await dialog.waitFor({ state: 'visible', timeout: config.timeout })
  await dialog.locator('.route-version-workspace__candidate-list').waitFor({ state: 'visible', timeout: config.timeout })
  return dialog
}

async function assertVersionWorkspace(dialog, target) {
  const tableBody = dialog.locator('.route-version-workspace__candidate-list .el-table__body-wrapper').first()
  await tableBody.waitFor({ state: 'visible', timeout: 30000 })
  const tableText = await tableBody.innerText()
  assert.ok(tableText.trim().length > 0, 'version workspace table should render visible rows')
  assert.equal(tableText.includes(STATUS_LABELS.CANCELLED), false, 'version workspace must not display 已取消 rows')
  assert.equal(tableText.includes('CANCELLED'), false, 'version workspace must not display CANCELLED text')
  for (const status of ['DRAFT', 'PENDING_APPROVAL', 'READY_TO_PUBLISH', 'REJECTED']) {
    assert.equal(tableText.includes(STATUS_LABELS[status]), false, `version workspace must not display ${STATUS_LABELS[status]} rows`)
    assert.equal(tableText.includes(status), false, `version workspace must not display ${status} text`)
  }
  for (const version of target.hiddenNonEffective) {
    assert.equal(tableText.includes(version.versionNo), false, `non-effective version should be hidden: ${version.versionNo}`)
  }
  for (const version of target.effectiveHistory) {
    assert.ok(tableText.includes(version.versionNo), `effective historical version should remain visible: ${version.versionNo}`)
    const expectedStatusLabel = version.active ? STATUS_LABELS.ACTIVE : STATUS_LABELS[version.lifecycleStatus]
    if (expectedStatusLabel) {
      assert.ok(tableText.includes(expectedStatusLabel), `status label should remain visible for ${version.versionNo}: ${expectedStatusLabel}`)
    }
  }
}

async function main() {
  const loginDefaults = readLoginDefaults()
  const config = {
    baseUrl: normalizeBaseUrl(process.env.MES_ROUTE_VERSION_LIST_E2E_BASE_URL || 'http://127.0.0.1:8089'),
    backendUrl: normalizeBaseUrl(process.env.MES_ROUTE_VERSION_LIST_E2E_BACKEND_URL || 'http://127.0.0.1:48089'),
    timeout: Number(process.env.MES_ROUTE_VERSION_LIST_E2E_TIMEOUT || 90000),
    artifactDir: path.resolve(
      process.env.MES_ROUTE_VERSION_LIST_E2E_ARTIFACT_DIR ||
        path.join(repoRoot, 'output', 'e2e', 'route-version-list-active-history-only')
    ),
    ...loginDefaults
  }
  assert.ok(config.tenant && config.username && config.password, 'Missing login defaults in frontend .env')
  assertPairedRuntimeUrls(config.baseUrl, config.backendUrl)
  await assertBackendHealth(config.backendUrl, 10000)
  fs.mkdirSync(config.artifactDir, { recursive: true })

  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1600, height: 1000 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const mesWriteRequests = []
  page.on('request', (request) => {
    const method = request.method()
    if (request.url().includes('/admin-api/mes/') && !READ_ONLY_MES_METHODS.has(method)) {
      mesWriteRequests.push({ method, url: request.url() })
    }
  })

  try {
    await login(page, config)
    const headers = await buildAuthHeaders(page)
    const target = await findRouteWithCancelledAndEffectiveHistory(page, config, headers)
    const dialog = await openVersionWorkspace(page, config, target)
    await assertVersionWorkspace(dialog, target)
    assert.deepEqual(mesWriteRequests, [], `E2E must not send MES write requests: ${JSON.stringify(mesWriteRequests)}`)

    const runId = new Date().toISOString().replace(/\D/g, '').slice(0, 14)
    const screenshotPath = path.join(config.artifactDir, `mes-route-version-list-${runId}.png`)
    const resultPath = path.join(config.artifactDir, `mes-route-version-list-${runId}.json`)
    await page.screenshot({ path: screenshotPath, fullPage: true })
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      backendUrl: config.backendUrl,
      tenant: config.tenant,
      username: config.username,
      route: {
        id: target.route.id,
        code: target.route.code,
        name: target.route.name
      },
      visibleEffectiveVersionNos: target.effectiveHistory.map((item) => ({
        id: item.id,
        versionNo: item.versionNo,
        lifecycleStatus: item.lifecycleStatus,
        active: item.active === true
      })),
      hiddenNonEffectiveVersionNos: target.hiddenNonEffective.map((item) => ({
        id: item.id,
        versionNo: item.versionNo,
        lifecycleStatus: item.lifecycleStatus
      })),
      hiddenCancelledVersionNos: target.cancelled.map((item) => ({
        id: item.id,
        versionNo: item.versionNo,
        lifecycleStatus: item.lifecycleStatus
      })),
      mesWriteRequests,
      screenshotPath
    }
    fs.writeFileSync(resultPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(`PASS: route version workspace shows effective historical versions only; result=${resultPath}`)
  } catch (error) {
    const failurePath = path.join(config.artifactDir, `mes-route-version-list-failure-${Date.now()}.png`)
    await page.screenshot({ path: failurePath, fullPage: true }).catch(() => null)
    throw error
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
