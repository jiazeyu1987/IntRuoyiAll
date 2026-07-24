const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_VERSION_READONLY_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_VERSION_READONLY_TENANT || '芋道源码',
  username: process.env.MES_ROUTE_VERSION_READONLY_USERNAME || 'admin',
  password: process.env.MES_ROUTE_VERSION_READONLY_PASSWORD || 'admin123',
  expectedTenantId: process.env.MES_ROUTE_VERSION_READONLY_TENANT_ID || '1',
  headed: process.env.MES_ROUTE_VERSION_READONLY_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_VERSION_READONLY_ARTIFACT_DIR ||
      path.join(__dirname, '..', '..', '..', 'doc', 'tasks', '20260718-route-version-single-candidate-flow-docs', 'e2e-artifacts')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalOnly() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `route version readonly E2E must stay local, got ${config.baseUrl}`
  )
}

function writeArtifact(name, payload) {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const filePath = path.join(config.artifactDir, name)
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return filePath
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(700)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    if (await tenantOption.count()) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(loginPayload.code),
    `login failed: HTTP ${loginResponse.status()} ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
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
  assert.equal(String(tenantId), config.expectedTenantId, `expected tenant-id=${config.expectedTenantId}, got ${tenantId}`)
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
      const result = await fetch(url, { method: 'GET', headers: requestHeaders })
      const text = await result.text()
      let json
      try {
        json = JSON.parse(text)
      } catch {
        json = { raw: text }
      }
      return { ok: result.ok, status: result.status, json }
    },
    { url: `${config.baseUrl}/admin-api${apiPath}`, requestHeaders: headers }
  )
  assert.ok(response.ok && response.json.code === 0, `GET ${apiPath} failed: ${JSON.stringify(response)}`)
  return response.json.data
}

function openCandidateCount(versions) {
  return versions.filter((version) =>
    ['DRAFT', 'PENDING_APPROVAL', 'READY_TO_PUBLISH'].includes(version.lifecycleStatus)
  ).length
}

async function main() {
  assertLocalOnly()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const evidence = {
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    expectedTenantId: config.expectedTenantId,
    writeRequests: [],
    checkedRoutes: []
  }

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    page.on('request', (request) => {
      const method = request.method()
      if (
        request.url().includes('/admin-api/mes/') &&
        !['GET', 'HEAD', 'OPTIONS'].includes(method)
      ) {
        evidence.writeRequests.push({ method, url: request.url() })
      }
    })

    await login(page)
    const routePagePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route/page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('工艺', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    const routePageResponse = await routePagePromise
    const routePagePayload = await routePageResponse.json()
    assert.equal(routePagePayload.code, 0, `route page API failed: ${JSON.stringify(routePagePayload)}`)
    const routes = routePagePayload.data?.list || []
    assert.ok(routes.length > 0, 'route page must contain real rows')

    const routeForDialog = routes.find((route) => route.activeRouteVersionId || route.pendingRouteVersionId) || routes[0]
    const versionResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route-version/list-by-route') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    const row = page.locator('.el-table__body-wrapper tr').filter({ hasText: routeForDialog.code || routeForDialog.name }).first()
    if (await row.count()) {
      await row.getByRole('button', { name: /^版本$/ }).first().click()
    } else {
      await page.getByRole('button', { name: /^版本$/ }).first().click()
    }
    await page.getByText('工艺路线版本', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    const versionPayload = await (await versionResponsePromise).json()
    assert.equal(versionPayload.code, 0, `route version API failed: ${JSON.stringify(versionPayload)}`)
    const dialogVersions = versionPayload.data || []
    evidence.dialogRoute = {
      routeId: routeForDialog.id,
      routeCode: routeForDialog.code,
      routeName: routeForDialog.name,
      versionCount: dialogVersions.length,
      openCandidateCount: openCandidateCount(dialogVersions)
    }
    assert.ok(
      evidence.dialogRoute.openCandidateCount <= 1,
      `dialog route has multiple open candidates: ${JSON.stringify(evidence.dialogRoute)}`
    )

    for (const route of routes.slice(0, 5)) {
      const versions = await apiGet(page, `/mes/pro/route-version/list-by-route?routeId=${route.id}`)
      const count = openCandidateCount(versions)
      evidence.checkedRoutes.push({
        routeId: route.id,
        routeCode: route.code,
        versionCount: versions.length,
        openCandidateCount: count
      })
      assert.ok(count <= 1, `route ${route.id} has multiple open candidates`)
    }

    assert.deepEqual(evidence.writeRequests, [], `readonly E2E sent MES write requests: ${JSON.stringify(evidence.writeRequests)}`)
    const artifactPath = writeArtifact('route-version-single-candidate-readonly-e2e.json', evidence)
    console.log(
      `GREEN: route-version-single-candidate-readonly-e2e -> PASS, tenantId=${config.expectedTenantId}, checkedRoutes=${evidence.checkedRoutes.length}, artifact=${artifactPath}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(`BLOCKER: route-version-single-candidate-readonly-e2e -> ${error.stack || error.message}`)
  process.exit(1)
})
