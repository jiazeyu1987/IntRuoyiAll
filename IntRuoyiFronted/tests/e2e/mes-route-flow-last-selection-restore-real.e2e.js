const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_LAST_SELECTION_BASE_URL || 'http://localhost:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_ROUTE_FLOW_LAST_SELECTION_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_LAST_SELECTION_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_LAST_SELECTION_PASSWORD || '111111',
  headed: process.env.MES_ROUTE_FLOW_LAST_SELECTION_HEADED === '1',
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '',
  outDir: path.resolve('tests/output/20260722-route-flow-last-selection-restore')
}

function assertLocalReadOnlyTarget() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', `real E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `real E2E must use aoteman, got ${config.username}`)
}

async function settle(page) {
  await page.waitForFunction(
    () =>
      document.querySelectorAll('.el-loading-mask').length === 0 &&
      document.querySelectorAll('.el-message').length === 0,
    null,
    { timeout: 30000 }
  )
  await page.waitForTimeout(300)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/route')}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantSelect = form.locator('.el-select').first()
  const selectedTenantText = await tenantSelect.innerText()
  if (!selectedTenantText.includes(config.tenant)) {
    const tenantInput = form
      .locator('.el-select input[role="combobox"]:visible, input.el-select__input:visible')
      .first()
    await tenantInput.waitFor({ state: 'visible', timeout: 10000 })
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await page.keyboard.press('Enter')
    await page.waitForTimeout(300)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    if (await tenantOption.isVisible().catch(() => false)) {
      await tenantOption.click()
    }
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login http failed: ${loginResponse.status()}`)
  assert.ok(
    [0, 200].includes(loginPayload.code),
    `login business failed: ${JSON.stringify(loginPayload)}`
  )
  await page.waitForFunction(() => !window.location.pathname.includes('/login'), null, {
    timeout: 60000
  })
}

function extractRouteList(payload) {
  const data = payload?.data ?? payload
  return data?.list || payload?.list || []
}

function routeRow(page, route) {
  const rowText = route.code || route.name
  if (!rowText) {
    throw new Error(`BLOCKER: 路线 ${route.id} 缺少编码和名称，无法通过真实列表定位。`)
  }
  return page.locator('tr.el-table__row').filter({ hasText: rowText }).first()
}

async function openRouteList(page) {
  const routePageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'commit', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  const routePageResponse = await routePageResponsePromise
  const routePayload = await routePageResponse.json()
  assert.ok(
    routePageResponse.ok() && [0, 200].includes(routePayload.code),
    `route page failed: HTTP ${routePageResponse.status()} ${JSON.stringify(routePayload)}`
  )
  await settle(page)
  return extractRouteList(routePayload)
}

async function chooseReadableActiveRoute(page, routes) {
  const candidates = routes.filter(
    (route) =>
      route?.id &&
      route?.activeRouteVersionId &&
      route?.activeRouteVersionNo &&
      route?.flowGraphConfigured
  )
  for (const route of candidates) {
    const row = routeRow(page, route)
    if ((await row.count()) === 0 || !(await row.isVisible())) continue
    if ((await row.locator('.route-list__version-link').count()) > 0) return route
  }
  throw new Error(
    'BLOCKER: 测试租户当前列表没有同时具备生效版本、已配置关系图和可见生效版本链接的路线。'
  )
}

async function openActiveRouteVersion(page, route) {
  const row = routeRow(page, route)
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const activeVersionLink = row.locator('.route-list__version-link').first()
  await activeVersionLink.waitFor({ state: 'visible', timeout: 60000 })
  assert.equal(
    (await activeVersionLink.textContent())?.trim(),
    String(route.activeRouteVersionNo).trim(),
    'must enter through the selected route active-version link'
  )
  await activeVersionLink.click()
  await page.waitForURL(
    (url) =>
      url.pathname === `/mes/pro/route/edit/${route.id}` &&
      url.searchParams.get('tab') === 'flow' &&
      !url.searchParams.has('routeProcessId'),
    { timeout: 60000 }
  )
  const designer = page.locator('.route-flow-graph-designer').first()
  await designer.waitFor({ state: 'visible', timeout: 60000 })
  await designer.locator('[data-flow-node="route-process"]').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page)
  return designer
}

async function clickReadableProcessNode(page, designer) {
  const routeProcessNodes = designer.locator('[data-flow-node="route-process"]')
  const clickTarget = await routeProcessNodes.evaluateAll((nodes) => {
    const fixedHeaderBottom = Array.from(
      document.querySelectorAll('#v-tool-header, #v-tags-view')
    ).reduce((bottom, element) => Math.max(bottom, element.getBoundingClientRect().bottom), 0)
    const candidates = nodes.map((node) => {
      const rect = node.getBoundingClientRect()
      const x = rect.left + rect.width / 2
      const y = rect.top + rect.height / 2
      const hit = document.elementFromPoint(x, y)
      const hitNode = hit?.closest?.('[data-flow-node="route-process"]')
      return {
        x,
        y,
        routeProcessId: hitNode?.getAttribute('data-route-process-id') || '',
        label: hitNode?.textContent?.trim() || ''
      }
    })
    return candidates.find(
      (candidate) =>
        candidate.routeProcessId &&
        candidate.y > fixedHeaderBottom + 8 &&
        candidate.y < window.innerHeight - 8
    )
  })
  assert.ok(clickTarget?.routeProcessId, 'route process node is not hittable in the current viewport')
  await page.mouse.click(clickTarget.x, clickTarget.y)
  await page.waitForFunction(
    (routeProcessId) =>
      document
        .querySelector(
          `[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"]`
        )
        ?.classList.contains('is-selected'),
    clickTarget.routeProcessId,
    { timeout: 10000 }
  )
  assert.equal(
    new URL(page.url()).searchParams.get('routeProcessId'),
    null,
    'plain process selection must not create an explicit routeProcessId query'
  )
  return clickTarget
}

async function clickReadableDetailField(page, designer) {
  const fieldButtons = designer.locator(
    '[data-flow-action="select-process-detail-field"][data-flow-detail-field-button]'
  )
  await fieldButtons.first().waitFor({ state: 'visible', timeout: 60000 })
  const fieldButton = fieldButtons.last()
  const fieldItem = fieldButton.locator('xpath=ancestor::*[@data-flow-detail-field][1]')
  const fieldKey = await fieldItem.getAttribute('data-flow-detail-field')
  const fieldLabel = (await fieldButton.textContent())?.trim()
  assert.ok(fieldKey, 'existing left detail field must expose data-flow-detail-field')
  assert.ok(fieldLabel, `detail field ${fieldKey} must expose a visible label`)
  await fieldButton.click()
  await page.waitForFunction(
    ({ fieldKey }) =>
      document
        .querySelector(`[data-flow-detail-field="${fieldKey}"]`)
        ?.classList.contains('is-selected'),
    { fieldKey },
    { timeout: 10000 }
  )
  const selectedFieldDetail = designer.locator('[data-flow-panel="selected-field-detail"]').first()
  await selectedFieldDetail.waitFor({ state: 'visible', timeout: 10000 })
  await assertEventuallyIncludes(page, selectedFieldDetail, fieldLabel, 'selected field detail')
  return { fieldKey, fieldLabel }
}

async function assertEventuallyIncludes(page, locator, expectedText, label) {
  await page.waitForFunction(
    (expected) =>
      document
        .querySelector('[data-flow-panel="selected-field-detail"]')
        ?.textContent?.includes(expected) === true,
    expectedText,
    { timeout: 10000 }
  )
  assert.ok((await locator.textContent())?.includes(expectedText), `${label} must show ${expectedText}`)
}

async function returnToRouteList(page) {
  await page.locator('[data-flow-action="back-route-list"]').first().click()
  await page.waitForURL((url) => url.pathname === '/mes/pro/route', { timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page)
}

async function assertRestoredSelection(page, designer, selectedProcess, selectedField) {
  const selectedNode = designer.locator(
    `[data-flow-node="route-process"][data-route-process-id="${selectedProcess.routeProcessId}"]`
  )
  await page.waitForFunction(
    (routeProcessId) =>
      document
        .querySelector(
          `[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"]`
        )
        ?.classList.contains('is-selected'),
    selectedProcess.routeProcessId,
    { timeout: 60000 }
  )
  assert.equal(
    await selectedNode.evaluate((element) => element.classList.contains('is-selected')),
    true,
    'reopened graph must restore the remembered process node'
  )

  const selectedFieldItem = designer.locator(
    `[data-flow-detail-field="${selectedField.fieldKey}"]`
  )
  await selectedFieldItem.waitFor({ state: 'visible', timeout: 60000 })
  await page.waitForFunction(
    (fieldKey) =>
      document
        .querySelector(`[data-flow-detail-field="${fieldKey}"]`)
        ?.classList.contains('is-selected'),
    selectedField.fieldKey,
    { timeout: 60000 }
  )
  assert.equal(
    await selectedFieldItem.evaluate((element) => element.classList.contains('is-selected')),
    true,
    'reopened graph must restore the remembered left detail field'
  )
  assert.equal(
    await selectedFieldItem
      .locator('[data-flow-action="select-process-detail-field"]')
      .getAttribute('aria-pressed'),
    'true',
    'restored detail field button must remain pressed'
  )
  await assertEventuallyIncludes(
    page,
    designer.locator('[data-flow-panel="selected-field-detail"]').first(),
    selectedField.fieldLabel,
    'restored field detail'
  )
}

;(async () => {
  assertLocalReadOnlyTarget()
  if (config.executablePath) {
    assert.ok(
      fs.existsSync(config.executablePath),
      `BLOCKER: Playwright Chromium executable not found: ${config.executablePath}`
    )
  }
  fs.mkdirSync(config.outDir, { recursive: true })
  const launchOptions = {
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  }
  if (config.executablePath) {
    launchOptions.executablePath = config.executablePath
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1600, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const writeRequests = []
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))
  page.on('request', (request) => {
    const method = request.method()
    const url = request.url()
    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) return
    if (
      url.includes('/admin-api/mes/') ||
      url.includes('/mes/') ||
      url.includes('/system/user-table-column-config/save')
    ) {
      writeRequests.push({ method, url })
    }
  })

  try {
    await login(page)
    const routes = await openRouteList(page)
    const route = await chooseReadableActiveRoute(page, routes)
    const designer = await openActiveRouteVersion(page, route)
    const selectedProcess = await clickReadableProcessNode(page, designer)
    const selectedField = await clickReadableDetailField(page, designer)

    await returnToRouteList(page)
    const reopenedDesigner = await openActiveRouteVersion(page, route)
    await assertRestoredSelection(page, reopenedDesigner, selectedProcess, selectedField)

    assert.deepEqual(
      writeRequests,
      [],
      `read-only restore E2E must not issue MES or user-column-config writes: ${JSON.stringify(writeRequests)}`
    )
    assert.deepEqual(pageErrors, [], `page must not report uncaught errors: ${JSON.stringify(pageErrors)}`)

    const artifact = {
      routeId: route.id,
      routeCode: route.code,
      routeName: route.name,
      routeVersionId: route.activeRouteVersionId,
      routeVersionNo: route.activeRouteVersionNo,
      routeProcessId: Number(selectedProcess.routeProcessId),
      routeProcessLabel: selectedProcess.label,
      detailFieldKey: selectedField.fieldKey,
      detailFieldLabel: selectedField.fieldLabel,
      tenant: config.tenant,
      username: config.username,
      writeRequests,
      pageErrors
    }
    fs.writeFileSync(path.join(config.outDir, 'result.json'), JSON.stringify(artifact, null, 2), 'utf8')
    await page.screenshot({
      path: path.join(config.outDir, 'restored-selection.png'),
      fullPage: true
    })
    console.log(`mes-route-flow-last-selection-restore-real PASS ${JSON.stringify(artifact)}`)
  } finally {
    await browser.close()
  }
})().catch((error) => {
  console.error(error)
  process.exit(1)
})
