const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FIELD_BUTTON_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_FIELD_BUTTON_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FIELD_BUTTON_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FIELD_BUTTON_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FIELD_BUTTON_ROUTE_CODE || 'RT000017',
  headed: process.env.MES_ROUTE_FIELD_BUTTON_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FIELD_BUTTON_ARTIFACT_DIR ||
      path.join(__dirname, '..', 'output', 'route-field-button-detail-real')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalOnly(baseUrl) {
  const hostname = new URL(baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `field button real E2E must use local baseUrl, got ${baseUrl}`
  )
}

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeArtifact(name, payload) {
  fs.writeFileSync(path.join(config.artifactDir, name), `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/route')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 60000 })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(payload.code),
    `login failed: HTTP ${loginResponse.status()} ${JSON.stringify(payload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
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
  assert.ok(accessToken, 'ACCESS_TOKEN missing after login')
  assert.ok(tenantId, 'tenantId missing after login')
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
      const res = await fetch(url, { headers: requestHeaders })
      const json = await res.json()
      return { ok: res.ok, status: res.status, json }
    },
    { url: `${config.baseUrl}/admin-api${apiPath}`, requestHeaders: headers }
  )
  assert.equal(response.json.code, 0, `GET ${apiPath} failed: ${JSON.stringify(response)}`)
  return response.json.data
}

async function findRoute(page) {
  const data = await apiGet(
    page,
    `/mes/pro/route/page?pageNo=1&pageSize=10&code=${encodeURIComponent(config.routeCode)}`
  )
  const route = data.list.find((item) => item.code === config.routeCode)
  assert.ok(route, `route ${config.routeCode} missing in ${config.tenant}`)
  assert.ok(
    route.pendingRouteVersionId || route.activeRouteVersionId,
    `route ${config.routeCode} has no route version to open`
  )
  return route
}

function buildRouteEditUrl(route) {
  const params = new URLSearchParams({ tab: 'flow' })
  if (route.pendingRouteVersionId) {
    params.set('routeVersionId', String(route.pendingRouteVersionId))
    if (route.pendingRouteVersionNo) params.set('routeVersionNo', String(route.pendingRouteVersionNo))
    params.set('routeVersionStatus', String(route.pendingRouteVersionStatus || 'DRAFT'))
  } else {
    params.set('routeVersionId', String(route.activeRouteVersionId))
    if (route.activeRouteVersionNo) params.set('routeVersionNo', String(route.activeRouteVersionNo))
    params.set('routeVersionStatus', String(route.activeRouteVersionStatus || 'ACTIVE'))
  }
  return `${config.baseUrl}/mes/pro/route/edit/${route.id}?${params.toString()}`
}

async function openRouteFlow(page, route) {
  await page.goto(buildRouteEditUrl(route), { waitUntil: 'domcontentloaded', timeout: 60000 })
  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  const flowTab = editor.getByRole('tab', { name: '流转关系图' }).first()
  if (await flowTab.count()) {
    await flowTab.click()
  }
  await editor.locator('[data-flow-node="route-process"]').first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  return editor
}

async function selectedFieldName(panel) {
  const fieldName = panel.locator('.route-flow-graph-designer__selected-field-grid strong').nth(1)
  await fieldName.waitFor({ state: 'visible', timeout: 10000 })
  return (await fieldName.innerText()).trim()
}

async function selectedFieldValue(panel) {
  const value = panel.locator('.route-flow-graph-designer__selected-field-value strong').first()
  await value.waitFor({ state: 'visible', timeout: 10000 })
  return (await value.innerText()).trim()
}

async function main() {
  assertLocalOnly(config.baseUrl)
  assert.equal(config.tenant, '测试租户', `field button real E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `field button real E2E must use aoteman, got ${config.username}`)
  ensureArtifactDir()

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: fs.existsSync(executablePath) ? executablePath : undefined
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const pageErrors = []
  const mesWriteRequests = []
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))
  page.on('request', (request) => {
    const method = request.method()
    if (
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) &&
      request.url().includes('/admin-api/mes/pro/')
    ) {
      mesWriteRequests.push({ method, url: request.url() })
    }
  })

  try {
    await login(page)
    const route = await findRoute(page)
    const editor = await openRouteFlow(page, route)
    const nodes = editor.locator('[data-flow-node="route-process"]')
    await nodes.first().click()
    const leftPanel = editor.locator('[data-flow-panel="selected-process-detail"]').first()
    const fieldCard = leftPanel.locator('[data-flow-detail-field]').first()
    await fieldCard.waitFor({ state: 'visible', timeout: 10000 })
    const fieldKey = await fieldCard.getAttribute('data-flow-detail-field')
    assert.ok(fieldKey, 'selected detail field key must be present')
    const button = fieldCard.locator('[data-flow-detail-field-button]').first()
    await button.waitFor({ state: 'visible', timeout: 10000 })
    const fieldLabel = (await button.innerText()).trim()
    assert.notEqual(fieldLabel, '', 'field button label must not be empty')
    const leftValueCount = await fieldCard
      .locator('.route-flow-graph-designer__selected-detail-editor, .route-flow-graph-designer__selected-detail-links, strong')
      .count()
    assert.equal(leftValueCount, 0, 'left field card must not render concrete values or editors')
    const buttonBox = await button.boundingBox()
    const contentBox = await fieldCard.locator('.route-flow-graph-designer__selected-detail-content').first().boundingBox()
    assert.ok(buttonBox, 'field button bounding box missing')
    assert.ok(contentBox, 'field card content bounding box missing')
    const buttonCoverageRatio = buttonBox.width / contentBox.width
    assert.ok(
      buttonCoverageRatio >= 0.72,
      `field button click area must cover the yellow content area, got ${buttonCoverageRatio}`
    )
    const rightPanel = editor.locator('[data-flow-panel="selected-field-detail"]').first()
    await rightPanel.getByText('点击左侧字段查看明细').waitFor({ state: 'visible', timeout: 10000 })
    await button.click()
    await rightPanel.getByText('字段名称').waitFor({ state: 'visible', timeout: 10000 })
    assert.equal(await selectedFieldName(rightPanel), fieldLabel, 'right field name must match clicked left button')
    await rightPanel.getByText('字段来源').waitFor({ state: 'visible', timeout: 10000 })
    const firstValue = await selectedFieldValue(rightPanel)
    assert.notEqual(firstValue, '', 'selected field value must not be empty')
    assert.equal(await button.getAttribute('aria-pressed'), 'true', 'field button must expose pressed state')

    let secondValue = null
    if ((await nodes.count()) > 1) {
      await nodes.nth(1).click()
      assert.equal(await selectedFieldName(rightPanel), fieldLabel, 'right field name must remain selected after node switch')
      secondValue = await selectedFieldValue(rightPanel)
      assert.notEqual(secondValue, '', 'selected field value after node switch must not be empty')
      assert.equal(
        await leftPanel.locator(`[data-flow-detail-field="${fieldKey}"] [data-flow-detail-field-button]`).first().getAttribute('aria-pressed'),
        'true',
        'same field must remain selected after node switch'
      )
    }

    assert.deepEqual(pageErrors, [], `page errors detected: ${JSON.stringify(pageErrors)}`)
    assert.deepEqual(
      mesWriteRequests,
      [],
      `clicking field detail must not write MES data: ${JSON.stringify(mesWriteRequests)}`
    )

    const evidence = {
      tenant: config.tenant,
      username: config.username,
      routeCode: route.code,
      routeId: route.id,
      fieldKey,
      fieldLabel,
      firstValue,
      secondValue,
      leftValueCount,
      buttonCoverageRatio,
      pageErrors,
      mesWriteRequests
    }
    writeArtifact('route-field-button-detail-real-result.json', evidence)
    await page.screenshot({
      path: path.join(config.artifactDir, 'route-field-button-detail-real.png'),
      fullPage: true
    })
    console.log(`mes-route-field-button-detail-real PASS ${JSON.stringify(evidence)}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
