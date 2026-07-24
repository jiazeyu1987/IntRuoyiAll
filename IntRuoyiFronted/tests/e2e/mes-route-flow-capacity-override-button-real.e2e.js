const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_CAPACITY_OVERRIDE_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_FLOW_CAPACITY_OVERRIDE_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_CAPACITY_OVERRIDE_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_CAPACITY_OVERRIDE_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FLOW_CAPACITY_OVERRIDE_ROUTE_CODE || '',
  headed: process.env.MES_ROUTE_FLOW_CAPACITY_OVERRIDE_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_CAPACITY_OVERRIDE_ARTIFACT_DIR ||
      path.join(__dirname, '..', '..', '..', 'doc', 'tasks', '20260717-route-flow-capacity-override-button', 'e2e-artifacts')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertSafeTarget() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `capacity override E2E must stay local, got ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', `capacity override E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `capacity override E2E must use aoteman, got ${config.username}`)
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
  assert.ok(accessToken, 'ACCESS_TOKEN missing after real login')
  assert.equal(String(tenantId), '122', `capacity override E2E must use tenant-id=122, got ${tenantId}`)
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function apiRequest(page, method, apiPath, body) {
  const headers = await authHeaders(page)
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const response = await page.evaluate(
    async ({ url, requestMethod, requestHeaders, requestBody }) => {
      const result = await fetch(url, {
        method: requestMethod,
        headers: requestHeaders,
        body: requestBody === undefined ? undefined : JSON.stringify(requestBody)
      })
      const text = await result.text()
      let json
      try {
        json = JSON.parse(text)
      } catch {
        json = { raw: text }
      }
      return { ok: result.ok, status: result.status, json }
    },
    {
      url: `${config.baseUrl}/admin-api${apiPath}`,
      requestMethod: method,
      requestHeaders: headers,
      requestBody: body
    }
  )
  assert.ok(response.ok && response.json.code === 0, `${method} ${apiPath} failed: ${JSON.stringify(response)}`)
  return response.json.data
}

async function apiGet(page, apiPath) {
  return apiRequest(page, 'GET', apiPath)
}

function normalizeCapacityMode(mode) {
  return mode === 'FINITE_HOURLY' ? 'MANUAL_OVERRIDE' : mode || 'RESOURCE_CALCULATED'
}

function positiveNumber(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0
}

function nextHourlyCapacity(value) {
  const current = Number(value)
  const next = Number((current + 0.000001).toFixed(6))
  return next === current ? Number((current + 0.000002).toFixed(6)) : next
}

async function loadRoutePages(page) {
  if (config.routeCode) {
    const routePage = await apiGet(
      page,
      `/mes/pro/route/page?pageNo=1&pageSize=10&code=${encodeURIComponent(config.routeCode)}`
    )
    return routePage.list.filter((item) => item.code === config.routeCode)
  }
  const routes = []
  for (let pageNo = 1; pageNo <= 5; pageNo += 1) {
    const routePage = await apiGet(page, `/mes/pro/route/page?pageNo=${pageNo}&pageSize=50`)
    routes.push(...routePage.list)
    if (!routePage.list.length || routes.length >= Number(routePage.total || 0)) break
  }
  return routes
}

async function loadCandidate(page) {
  const routes = await loadRoutePages(page)
  assert.ok(
    routes.length,
    config.routeCode
      ? `BLOCKER: route ${config.routeCode} missing in test tenant`
      : 'BLOCKER: no route rows available in test tenant'
  )

  for (const route of routes) {
    if (!route.activeRouteVersionId) continue
    const [processRows, routeVersions] = await Promise.all([
      apiGet(page, `/mes/pro/route-process/list-by-route?routeId=${route.id}`),
      apiGet(page, `/mes/pro/route-version/list-by-route?routeId=${route.id}`)
    ])
    const draftVersions = routeVersions.filter(
      (version) => !version.active && version.lifecycleStatus === 'DRAFT'
    )
    for (const routeVersion of draftVersions) {
      const scheduleConfigs = await apiGet(
        page,
        `/mes/pro/route-schedule-config/list-by-route-version?routeVersionId=${routeVersion.id}`
      )
      const scheduleByRouteProcessId = new Map(
        scheduleConfigs.map((item) => [Number(item.routeProcessId), item])
      )
      const routeProcess = processRows.find((row) => {
        const scheduleConfig = scheduleByRouteProcessId.get(Number(row.id))
        return (
          row.workstationId &&
          positiveNumber(row.shiftHours) &&
          positiveNumber(row.processShiftCapacityTotal) &&
          scheduleConfig &&
          normalizeCapacityMode(scheduleConfig.capacityMode) === 'MANUAL_OVERRIDE' &&
          positiveNumber(scheduleConfig.hourlyCapacity)
        )
      })
      if (routeProcess) {
        return {
          route,
          routeVersion,
          routeProcess,
          scheduleConfig: scheduleByRouteProcessId.get(Number(routeProcess.id))
        }
      }
    }
  }

  throw new Error(
    config.routeCode
      ? `BLOCKER: route ${config.routeCode} has no DRAFT candidate with a safely restorable workstation route process already in MANUAL_OVERRIDE with shift hours`
      : 'BLOCKER: no DRAFT candidate with a safely restorable workstation route process already in MANUAL_OVERRIDE with shift hours was found in scanned routes'
  )
}

async function openRouteGraph(page, route, routeVersion) {
  const routeCode = route.code
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  await page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
    .fill(routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)

  const row = page.locator('tr.el-table__row').filter({ hasText: routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })
  const candidateUrl = new URL(`/mes/pro/route/edit/${route.id}`, config.baseUrl)
  candidateUrl.searchParams.set('tab', 'flow')
  candidateUrl.searchParams.set('routeVersionId', String(routeVersion.id))
  candidateUrl.searchParams.set('routeVersionNo', routeVersion.versionNo)
  candidateUrl.searchParams.set('routeVersionStatus', routeVersion.lifecycleStatus)
  await page.goto(candidateUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.getByRole('tab', { name: '流转关系图' }).click()
  await editor.locator('[data-flow-node="route-process"]').first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  return editor
}

function detailPanel(editor) {
  return editor.locator('[data-flow-panel="selected-process-detail"]').first()
}

async function ensureWorkstationDetailField(editor) {
  const page = editor.page()
  const panel = detailPanel(editor)
  const field = panel.locator('[data-flow-detail-field="workstation"]').first()
  if (await field.count()) {
    await field.waitFor({ state: 'visible', timeout: 10000 })
    return field
  }

  const picker = panel.locator('.route-flow-graph-designer__process-detail-field-picker').first()
  await picker.locator('.el-select').click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: '工作站' }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/user-table-column-config/save') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await picker.locator('[data-flow-action="add-process-config-item"]').click()
  const saveResponse = await saveResponsePromise
  const savePayload = await saveResponse.json()
  assert.equal(savePayload.code, 0, `save workstation detail field failed: ${JSON.stringify(savePayload)}`)
  await field.waitFor({ state: 'visible', timeout: 10000 })
  return field
}

async function selectRouteProcess(editor, routeProcessId) {
  const node = editor.locator(`[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"]`).first()
  await node.waitFor({ state: 'visible', timeout: 60000 })
  await node.scrollIntoViewIfNeeded()
  await node.click()
  const panel = detailPanel(editor)
  await expectNodeSelected(node)
  await panel.locator('.route-flow-graph-designer__process-detail-field-picker').first().waitFor({
    state: 'visible',
    timeout: 10000
  })
  return node
}

async function expectNodeSelected(node) {
  await node
    .locator('xpath=.')
    .evaluate((element) => element.classList.contains('is-selected'))
    .then((selected) => {
      assert.equal(selected, true, 'route process node should be selected after click')
    })
}

async function saveCapacityOverride(editor, hourlyCapacity) {
  const page = editor.page()
  const field = await ensureWorkstationDetailField(editor)
  const button = field.locator('[data-flow-action="open-capacity-override-dialog"]').first()
  await button.waitFor({ state: 'visible', timeout: 10000 })
  assert.equal(await button.isDisabled(), false, 'capacity override button should be enabled')
  await button.click()
  const dialog = page.locator('[data-testid="route-flow-capacity-override-dialog"]').first()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  const input = dialog.locator('input').first()
  await input.fill(String(hourlyCapacity))
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-schedule-config/save') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const reloadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-schedule-config/list-by-route-version') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await dialog.locator('[data-flow-action="submit-capacity-override"]').click()
  const saveResponse = await saveResponsePromise
  const savePayload = await saveResponse.json()
  assert.equal(savePayload.code, 0, `capacity override save failed: ${JSON.stringify(savePayload)}`)
  await reloadResponsePromise
  await dialog.waitFor({ state: 'detached', timeout: 10000 })
  await field.locator('[data-flow-capacity="override-shift-capacity"]').waitFor({ state: 'visible', timeout: 10000 })
  const text = await field.innerText()
  assert.match(text, /原班次产能：/)
  assert.match(text, /覆盖产能：/)
  assert.match(text, /覆盖班次产能：/)
  return text.replace(/\s+/g, ' ').trim()
}

async function readScheduleConfig(page, routeVersionId, routeProcessId) {
  const configs = await apiGet(
    page,
    `/mes/pro/route-schedule-config/list-by-route-version?routeVersionId=${routeVersionId}`
  )
  const configRow = configs.find((item) => Number(item.routeProcessId) === Number(routeProcessId))
  assert.ok(configRow, `route process schedule config missing after save: ${routeProcessId}`)
  return configRow
}

async function main() {
  assertSafeTarget()
  const browser = await chromium.launch({ headless: !config.headed, executablePath })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const evidence = {
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    requestedRouteCode: config.routeCode || null,
    steps: []
  }
  let candidate
  try {
    await login(page)
    candidate = await loadCandidate(page)
    evidence.routeCode = candidate.route.code
    const originalHourlyCapacity = Number(candidate.scheduleConfig.hourlyCapacity)
    const overrideHourlyCapacity = nextHourlyCapacity(originalHourlyCapacity)
    evidence.routeId = candidate.route.id
    evidence.routeVersionId = candidate.routeVersion.id
    evidence.routeVersionNo = candidate.routeVersion.versionNo
    evidence.routeProcessId = candidate.routeProcess.id
    evidence.originalHourlyCapacity = originalHourlyCapacity
    evidence.overrideHourlyCapacity = overrideHourlyCapacity

    let editor = await openRouteGraph(page, candidate.route, candidate.routeVersion)
    await selectRouteProcess(editor, candidate.routeProcess.id)
    evidence.steps.push({
      action: 'saveOverrideViaFlowGraph',
      cardText: await saveCapacityOverride(editor, overrideHourlyCapacity)
    })
    const savedConfig = await readScheduleConfig(page, candidate.routeVersion.id, candidate.routeProcess.id)
    assert.equal(normalizeCapacityMode(savedConfig.capacityMode), 'MANUAL_OVERRIDE')
    assert.equal(Number(savedConfig.hourlyCapacity), overrideHourlyCapacity)

    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    editor = page.locator('.route-edit-page').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })
    await editor.getByRole('tab', { name: '流转关系图' }).click()
    await selectRouteProcess(editor, candidate.routeProcess.id)
    const restoredField = await ensureWorkstationDetailField(editor)
    const reloadedText = (await restoredField.innerText()).replace(/\s+/g, ' ').trim()
    assert.match(reloadedText, /原班次产能：/)
    assert.match(reloadedText, /覆盖产能：/)
    assert.match(reloadedText, /覆盖班次产能：/)
    evidence.steps.push({ action: 'reloadVerifyOverrideCard', cardText: reloadedText })

    evidence.steps.push({
      action: 'restoreOriginalOverrideViaFlowGraph',
      cardText: await saveCapacityOverride(editor, originalHourlyCapacity)
    })
    const restoredConfig = await readScheduleConfig(page, candidate.routeVersion.id, candidate.routeProcess.id)
    assert.equal(normalizeCapacityMode(restoredConfig.capacityMode), 'MANUAL_OVERRIDE')
    assert.equal(Number(restoredConfig.hourlyCapacity), originalHourlyCapacity)
    evidence.restored = true
    evidence.artifact = writeArtifact('capacity-override-real-evidence.json', evidence)
    console.log(`mes-route-flow-capacity-override-button-real PASS ${evidence.artifact}`)
  } catch (error) {
    evidence.error = error && error.stack ? error.stack : String(error)
    evidence.artifact = writeArtifact('capacity-override-real-evidence.json', evidence)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
