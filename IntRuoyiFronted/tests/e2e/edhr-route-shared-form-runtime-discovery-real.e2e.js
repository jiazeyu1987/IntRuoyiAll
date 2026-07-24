const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_ROUTE_SHARED_FORM_DISCOVERY_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.EDHR_ROUTE_SHARED_FORM_DISCOVERY_TENANT || '测试租户',
  username: process.env.EDHR_ROUTE_SHARED_FORM_DISCOVERY_USERNAME || 'aoteman',
  password: process.env.EDHR_ROUTE_SHARED_FORM_DISCOVERY_PASSWORD || '111111',
  routeId: Number(process.env.EDHR_ROUTE_SHARED_FORM_DISCOVERY_ROUTE_ID || '922046'),
  artifactDir:
    process.env.EDHR_ROUTE_SHARED_FORM_DISCOVERY_ARTIFACT_DIR ||
    path.resolve(process.cwd(), 'tests/output/edhr-optional-shared-form-tasks-real')
}

if (config.baseUrl !== 'http://localhost:8081') {
  throw new Error(`BLOCKER: discovery must use local frontend http://localhost:8081, got ${config.baseUrl}`)
}
if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(`BLOCKER: discovery must use 测试租户/aoteman, got ${config.tenant}/${config.username}`)
}
assert.ok(Number.isFinite(config.routeId) && config.routeId > 0, 'routeId must be positive')

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeArtifact(name, payload) {
  ensureArtifactDir()
  fs.writeFileSync(path.join(config.artifactDir, name), `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 20000 }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/feedback/edhr-batch-execution')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/feedback/edhr-batch-execution')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json().catch(() => null)
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `login failed: ${JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function apiRequest(page, method, apiPath, body) {
  const result = await page.evaluate(
    async ({ method: requestMethod, apiPath: requestPath, body: requestBody }) => {
      const unwrap = (value) => {
        if (!value || typeof value !== 'object') return value
        for (const field of ['accessToken', 'value', 'v', 'data']) {
          if (Object.prototype.hasOwnProperty.call(value, field)) return unwrap(value[field])
        }
        return value
      }
      const readCache = (key) => {
        for (const storage of [localStorage, sessionStorage]) {
          const matchedKey = Object.keys(storage).find((item) => item === key || item.endsWith(key))
          if (!matchedKey) continue
          const raw = storage.getItem(matchedKey)
          if (!raw) continue
          try {
            const value = unwrap(JSON.parse(raw))
            if (typeof value === 'string' && value.startsWith('"') && value.endsWith('"')) {
              return value.slice(1, -1)
            }
            return value
          } catch {
            return raw.replace(/^"|"$/g, '')
          }
        }
        return undefined
      }
      const headers = { 'Cache-Control': 'no-cache', Pragma: 'no-cache' }
      const accessToken = readCache('ACCESS_TOKEN')
      const tenantId = readCache('tenantId')
      if (accessToken) headers.Authorization = `Bearer ${accessToken}`
      if (tenantId) headers['tenant-id'] = String(tenantId)
      if (requestBody !== undefined) headers['Content-Type'] = 'application/json'
      const response = await fetch(`/admin-api${requestPath}`, {
        method: requestMethod,
        credentials: 'omit',
        headers,
        body: requestBody === undefined ? undefined : JSON.stringify(requestBody)
      })
      return { status: response.status, body: await response.json().catch(() => null) }
    },
    { method, apiPath, body }
  )
  assert.equal(result.status, 200, `HTTP error ${method} ${apiPath}: ${JSON.stringify(result.body)}`)
  assert.ok(
    result.body && (result.body.code === 0 || result.body.code === 200),
    `API error ${method} ${apiPath}: ${JSON.stringify(result.body)}`
  )
  return result.body.data
}

function buildQuery(params) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  }
  return search.toString()
}

async function apiGet(page, apiPath, params = {}) {
  const query = buildQuery(params)
  return apiRequest(page, 'GET', `${apiPath}${query ? `?${query}` : ''}`)
}

async function discoverWorkOrders(page) {
  const candidates = []
  for (let pageNo = 1; pageNo <= 4; pageNo += 1) {
    const pageData = await apiGet(page, '/mes/pro/work-order/page', {
      pageNo,
      pageSize: 20,
      temporaryFrozen: false
    })
    const list = Array.isArray(pageData?.list) ? pageData.list : []
    for (const workOrder of list) {
      if (!workOrder?.id) continue
      const routeOptions = await apiGet(page, '/mes/pro/edhr-batch-execution/work-order-route-options', {
        workOrderId: workOrder.id
      }).catch((error) => {
        candidates.push({
          workOrderId: workOrder.id,
          workOrderCode: workOrder.code,
          batchCode: workOrder.batchCode,
          error: error.message
        })
        return []
      })
      const routeOptionIds = (routeOptions || []).map((item) => Number(item.routeId))
      candidates.push({
        workOrderId: workOrder.id,
        workOrderCode: workOrder.code,
        batchCode: workOrder.batchCode,
        matchedTargetRoute: routeOptionIds.includes(config.routeId),
        routeOptions: (routeOptions || []).map((item) => ({
          routeId: item.routeId,
          routeCode: item.routeCode,
          routeName: item.routeName,
          routeVersionId: item.routeVersionId,
          routeVersionNo: item.routeVersionNo
        }))
      })
    }
    if (list.length < 20) break
  }
  return candidates
}

function summarizeConfigs(configRows) {
  return (configRows || []).map((row) => ({
    routeProcessId: row.routeProcessId,
    processCode: row.processCode,
    processName: row.processName,
    enabled: row.enabled,
    batchRecordReports: (row.batchRecordReports || []).map((report) => ({
      batchRecordReportId: report.batchRecordReportId,
      batchRecordReportCode: report.batchRecordReportCode,
      batchRecordReportName: report.batchRecordReportName,
      formSlotType: report.formSlotType,
      instanceScope: report.instanceScope,
      sharedFormKey: report.sharedFormKey,
      requiredPolicy: report.requiredPolicy,
      fillableScopeJson: report.fillableScopeJson
    }))
  }))
}

async function main() {
  ensureArtifactDir()
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1600, height: 980 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const mesWriteRequests = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      mesWriteRequests.push({ method: request.method(), url: request.url(), postData: request.postData() })
    }
  })

  try {
    await login(page)
    const route = await apiGet(page, '/mes/pro/route/get', { id: config.routeId })
    const versions = await apiGet(page, '/mes/pro/route-version/list-by-route', { routeId: config.routeId })
    const versionConfigs = []
    for (const version of versions || []) {
      const configs = await apiGet(page, '/mes/pro/route/flow-config', {
        routeId: config.routeId,
        useType: 'BATCH',
        routeVersionId: version.id
      }).catch((error) => ({ error: error.message }))
      versionConfigs.push({
        routeVersionId: version.id,
        versionNo: version.versionNo,
        lifecycleStatus: version.lifecycleStatus,
        active: version.active,
        configRows: Array.isArray(configs) ? summarizeConfigs(configs) : configs
      })
    }
    const workOrderCandidates = await discoverWorkOrders(page)
    const matchedWorkOrders = workOrderCandidates.filter((item) => item.matchedTargetRoute)
    assert.deepEqual(mesWriteRequests, [], `read-only discovery made MES write requests: ${JSON.stringify(mesWriteRequests)}`)
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routeId: config.routeId,
      route: {
        id: route?.id,
        code: route?.code,
        name: route?.name,
        activeRouteVersionId: route?.activeRouteVersionId,
        activeRouteVersionNo: route?.activeRouteVersionNo,
        pendingRouteVersionId: route?.pendingRouteVersionId,
        pendingRouteVersionNo: route?.pendingRouteVersionNo,
        pendingRouteVersionStatus: route?.pendingRouteVersionStatus,
        batchRouteEnabled: route?.batchRouteEnabled
      },
      versions,
      versionConfigs,
      matchedWorkOrders,
      workOrderCandidateCount: workOrderCandidates.length,
      pageErrors,
      mesWriteRequests
    }
    writeArtifact(`route-shared-form-runtime-discovery-${config.routeId}.json`, result)
    process.stdout.write(`PASS: route shared form runtime discovery\n${JSON.stringify(result, null, 2)}\n`)
  } catch (error) {
    const result = {
      status: 'FAIL',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routeId: config.routeId,
      error: error.message,
      pageErrors,
      mesWriteRequests
    }
    writeArtifact(`route-shared-form-runtime-discovery-${config.routeId}.json`, result)
    throw error
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
