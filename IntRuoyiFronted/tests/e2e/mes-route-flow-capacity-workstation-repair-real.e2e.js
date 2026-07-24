const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const TASK_ID = '20260720-capacity-override-workstation-repair-real-e2e'

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_PASSWORD || '111111',
  signaturePassword:
    process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_SIGNATURE_PASSWORD ||
    process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_PASSWORD ||
    '111111',
  sourceRouteCode:
    process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_SOURCE_ROUTE_CODE ||
    process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_ROUTE_CODE ||
    '',
  copyRouteCode: process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_COPY_ROUTE_CODE || '',
  headed: process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_WORKSTATION_REPAIR_ARTIFACT_DIR ||
      path.join(__dirname, '..', '..', '..', 'doc', 'tasks', TASK_ID, 'e2e-artifacts')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertSafeTarget() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `workstation repair E2E must stay local, got ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', `workstation repair E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `workstation repair E2E must use aoteman, got ${config.username}`)
}

function writeArtifact(name, payload) {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const filePath = path.join(config.artifactDir, name)
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return filePath
}

async function screenshot(page, name) {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const filePath = path.join(config.artifactDir, name)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(700)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), {
    waitUntil: 'commit',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
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
  assert.equal(String(tenantId), '122', `workstation repair E2E must use tenant-id=122, got ${tenantId}`)
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
  assert.ok(response.ok && [0, 200].includes(response.json.code), `${method} ${apiPath} failed: ${JSON.stringify(response)}`)
  return response.json.data
}

async function apiGet(page, apiPath) {
  return apiRequest(page, 'GET', apiPath)
}

function numericValue(value) {
  if (value === undefined || value === null || value === '') return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

function positiveNumber(value) {
  const parsed = numericValue(value)
  return parsed !== undefined && parsed > 0
}

function normalizeCapacityMode(mode) {
  return mode === 'FINITE_HOURLY' ? 'MANUAL_OVERRIDE' : mode || 'RESOURCE_CALCULATED'
}

function adminApiPath(url) {
  const marker = '/admin-api/'
  const index = url.indexOf(marker)
  if (index < 0) return ''
  return url.slice(index + marker.length).split('?')[0]
}

async function loadRoutePages(page) {
  if (config.sourceRouteCode) {
    const routePage = await apiGet(
      page,
      `/mes/pro/route/page?pageNo=1&pageSize=10&code=${encodeURIComponent(config.sourceRouteCode)}`
    )
    return routePage.list.filter((item) => item.code === config.sourceRouteCode)
  }
  const routes = []
  for (let pageNo = 1; pageNo <= 8; pageNo += 1) {
    const routePage = await apiGet(page, `/mes/pro/route/page?pageNo=${pageNo}&pageSize=50`)
    routes.push(...routePage.list)
    if (!routePage.list.length || routes.length >= Number(routePage.total || 0)) break
  }
  return routes
}

function rowLabel(row) {
  return [row.processCode, row.processName].filter(Boolean).join(' / ') || `routeProcessId=${row.id}`
}

function buildE2ERouteCode() {
  const timestamp = new Date().toISOString().replace(/\D/g, '').slice(0, 14)
  return `E2E-CAP-WS-${timestamp}`
}

function parseRouteVersionContextFromUrl(urlText) {
  const url = new URL(urlText)
  const routeVersionId = Number(url.searchParams.get('routeVersionId'))
  const versionNo = url.searchParams.get('routeVersionNo') || ''
  const lifecycleStatus = url.searchParams.get('routeVersionStatus') || ''
  assert.ok(
    Number.isFinite(routeVersionId) && routeVersionId > 0 && versionNo && lifecycleStatus === 'DRAFT',
    `draft route version context missing from URL: ${urlText}`
  )
  return { id: routeVersionId, versionNo, lifecycleStatus }
}

async function filterRouteList(page, routeCode) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)

  const codeInput = page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
  await codeInput.fill(routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)
  const row = page.locator('tr.el-table__row').filter({ hasText: routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  return row
}


async function openAutoCodeRulePage(page) {
  await page.goto(config.baseUrl + '/mes/md/auto-code', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('\u89c4\u5219\u7f16\u7801', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
}

async function queryAutoCodeRuleRow(page, ruleCode) {
  await openAutoCodeRulePage(page)
  const codeInput = page.locator('input[placeholder="\u8bf7\u8f93\u5165\u89c4\u5219\u7f16\u7801"]').first()
  await codeInput.fill(ruleCode)
  await page.getByRole('button', { name: /\u641c\u7d22|\u67e5\u8be2/ }).first().click()
  await settle(page)
  const row = page.locator('tr.el-table__row').filter({ hasText: ruleCode }).first()
  return (await row.isVisible().catch(() => false)) ? row : null
}

async function fillAutoCodeInputNumber(dialog, placeholder, value) {
  const input = dialog.locator('input[placeholder="' + placeholder + '"]').first()
  await input.fill(String(value))
  await input.dispatchEvent('change')
}

async function createWorkstationAutoCodeRuleThroughUi(page) {
  await openAutoCodeRulePage(page)
  await page.getByRole('button', { name: /\u65b0\u589e/ }).first().click()
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[placeholder="\u8bf7\u8f93\u5165\u89c4\u5219\u7f16\u7801"]').fill('MD_WORKSTATION_CODE')
  await dialog.locator('input[placeholder="\u8bf7\u8f93\u5165\u89c4\u5219\u540d\u79f0"]').fill('Workstation Code')
  await dialog.locator('input[placeholder="\u8bf7\u8f93\u5165\u89c4\u5219\u63cf\u8ff0"]').fill('Capacity workstation repair E2E prerequisite')
  await fillAutoCodeInputNumber(dialog, '\u8bf7\u8f93\u5165\u6700\u5927\u957f\u5ea6', 30)
  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/md/auto-code-rule/create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: /\u786e\s*\u5b9a/ }).click()
  const createResponse = await createResponsePromise
  const createPayload = await createResponse.json()
  assert.ok(
    createResponse.ok() && [0, 200].includes(createPayload.code) && Number(createPayload.data) > 0,
    'auto-code rule create failed: ' + JSON.stringify(createPayload)
  )
  await dialog.waitFor({ state: 'detached', timeout: 30000 })
  await settle(page)
  return Number(createPayload.data)
}

async function openWorkstationAutoCodeRuleDialog(page, ruleCode) {
  const row = await queryAutoCodeRuleRow(page, ruleCode)
  assert.ok(row, 'workstation auto-code rule should be visible before editing parts')
  const partListResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/md/auto-code-part/list-by-rule-id') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await row.getByRole('button', { name: /\u7f16\u8f91/ }).click()
  const ruleDialog = page.locator('.el-dialog:visible').last()
  await ruleDialog.waitFor({ state: 'visible', timeout: 30000 })
  await ruleDialog.getByText('\u89c4\u5219\u7ec4\u6210', { exact: false }).waitFor({ state: 'visible', timeout: 30000 })
  const partListResponse = await partListResponsePromise
  const partListPayload = await partListResponse.json()
  assert.ok(
    partListResponse.ok() && [0, 200].includes(partListPayload.code) && Array.isArray(partListPayload.data),
    'auto-code part list failed before editing parts: ' + JSON.stringify(partListPayload)
  )
  const loadingMask = ruleDialog.locator('.el-loading-mask:visible').first()
  if (await loadingMask.count()) {
    await loadingMask.waitFor({ state: 'detached', timeout: 30000 })
  }
  await settle(page)
  return ruleDialog
}

async function closeDialogIfVisible(dialog) {
  if (!(await dialog.isVisible().catch(() => false))) return
  await dialog.getByRole('button', { name: /\u53d6\s*\u6d88/ }).click().catch(() => null)
  await dialog.waitFor({ state: 'detached', timeout: 5000 }).catch(() => null)
}

async function addAutoCodePartThroughUi(page, ruleDialog, part) {
  await ruleDialog.getByRole('button', { name: /\u65b0\u589e\u5206\u6bb5/ }).click()
  const partDialog = page.locator('.el-dialog:visible').last()
  await partDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillAutoCodeInputNumber(partDialog, '\u8bf7\u8f93\u5165\u5206\u6bb5\u957f\u5ea6', part.length)
  await partDialog.locator('.el-form-item').filter({ hasText: '\u5206\u6bb5\u7c7b\u578b' }).locator('.el-select').first().click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: part.typeLabel }).first().click()
  if (part.fixCharacter) {
    await partDialog.locator('input[placeholder="\u8bf7\u8f93\u5165\u56fa\u5b9a\u5b57\u7b26"]').fill(part.fixCharacter)
  }
  if (part.serialStartNo) {
    await fillAutoCodeInputNumber(partDialog, '\u8bf7\u8f93\u5165\u6d41\u6c34\u53f7\u8d77\u59cb\u503c', part.serialStartNo)
    await fillAutoCodeInputNumber(partDialog, '\u8bf7\u8f93\u5165\u6d41\u6c34\u53f7\u6b65\u957f', part.serialStep)
  }
  const createPartResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/md/auto-code-part/create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await partDialog.getByRole('button', { name: /\u786e\s*\u5b9a/ }).click()
  const createPartResponse = await createPartResponsePromise
  const createPartPayload = await createPartResponse.json()
  assert.ok(
    createPartResponse.ok() && [0, 200].includes(createPartPayload.code),
    'auto-code part create failed: ' + JSON.stringify(createPartPayload)
  )
  await partDialog.waitFor({ state: 'detached', timeout: 5000 }).catch(async () => {
    await partDialog.getByRole('button', { name: /\u53d6\s*\u6d88/ }).click().catch(() => null)
    await partDialog.waitFor({ state: 'detached', timeout: 5000 }).catch(() => null)
  })
  await settle(page)
  return createPartPayload.data
}

async function ensureWorkstationAutoCodeRuleThroughUi(page) {
  const ruleCode = 'MD_WORKSTATION_CODE'
  let setupStatus = 'existing'
  let row = await queryAutoCodeRuleRow(page, ruleCode)
  let ruleId
  if (!row) {
    ruleId = await createWorkstationAutoCodeRuleThroughUi(page)
    setupStatus = 'created'
    row = await queryAutoCodeRuleRow(page, ruleCode)
    assert.ok(row, 'created workstation auto-code rule should be visible in rule list')
  }

  let ruleDialog = await openWorkstationAutoCodeRuleDialog(page, ruleCode)
  let hasFixedPart = await ruleDialog.locator('tr.el-table__row').filter({ hasText: 'WS' }).first().isVisible().catch(() => false)
  let hasSerialPart = await ruleDialog.locator('tr.el-table__row').filter({ hasText: '\u6d41\u6c34\u53f7' }).first().isVisible().catch(() => false)
  await closeDialogIfVisible(ruleDialog)
  if (!hasFixedPart) {
    ruleDialog = await openWorkstationAutoCodeRuleDialog(page, ruleCode)
    await addAutoCodePartThroughUi(page, ruleDialog, {
      typeLabel: '\u56fa\u5b9a\u5b57\u7b26',
      length: 2,
      fixCharacter: 'WS'
    })
    await closeDialogIfVisible(ruleDialog)
    setupStatus = setupStatus === 'existing' ? 'completed-existing-rule' : setupStatus
  }
  ruleDialog = await openWorkstationAutoCodeRuleDialog(page, ruleCode)
  hasSerialPart = await ruleDialog.locator('tr.el-table__row').filter({ hasText: '\u6d41\u6c34\u53f7' }).first().isVisible().catch(() => false)
  if (!hasSerialPart) {
    await addAutoCodePartThroughUi(page, ruleDialog, {
      typeLabel: '\u6d41\u6c34\u53f7',
      length: 8,
      serialStartNo: 1,
      serialStep: 1
    })
    setupStatus = setupStatus === 'existing' ? 'completed-existing-rule' : setupStatus
  }
  await closeDialogIfVisible(ruleDialog)
  return { status: setupStatus, ruleCode, ruleId }
}

async function loadSourceRouteForCopy(page) {
  const routes = await loadRoutePages(page)
  assert.ok(
    routes.length,
    config.sourceRouteCode
      ? `BLOCKER: route ${config.sourceRouteCode} missing in test tenant`
      : 'BLOCKER: no route rows available in test tenant'
  )

  const scanned = []
  for (const route of routes) {
    if (!config.sourceRouteCode && String(route.code || '').startsWith('E2E-CAP-WS-')) {
      scanned.push({ routeCode: route.code, reason: 'skip prior E2E copied route as source' })
      continue
    }
    const [routeInfo, routeVersions] = await Promise.all([
      apiGet(page, `/mes/pro/route/get?id=${route.id}`),
      apiGet(page, `/mes/pro/route-version/list-by-route?routeId=${route.id}`)
    ])
    const activeRouteVersionId = routeInfo.activeRouteVersionId || route.activeRouteVersionId
    const activeVersion =
      routeVersions.find((version) => Number(version.id) === Number(activeRouteVersionId)) ||
      routeVersions.find((version) => Boolean(version.active))
    if (!activeVersion) {
      scanned.push({ routeCode: route.code, reason: 'no active route version to copy from' })
      continue
    }

    const processRows = await apiGet(page, `/mes/pro/route-process/list-by-route?routeId=${route.id}`)
    const scheduleConfigs = await apiGet(
      page,
      `/mes/pro/route-schedule-config/list-by-route-version?routeVersionId=${activeVersion.id}`
    )
    const configuredRouteProcessIds = new Set(scheduleConfigs.map((item) => Number(item.routeProcessId)))
    const missingScheduleConfigRows = processRows.filter((row) => !configuredRouteProcessIds.has(Number(row.id)))
    if (missingScheduleConfigRows.length) {
      scanned.push({
        routeCode: route.code,
        reason: 'route copy would fail because active version has route processes without schedule config',
        activeRouteVersionId: activeVersion.id,
        missingRouteProcessIds: missingScheduleConfigRows.map((row) => row.id).slice(0, 10)
      })
      continue
    }
    const boundSources = processRows
      .filter((row) => row.id && row.workstationId && positiveNumber(row.shiftHours))
      .sort((left, right) => (left.sort || 0) - (right.sort || 0) || Number(left.id) - Number(right.id))
    if (boundSources.length < 2) {
      scanned.push({ routeCode: route.code, reason: 'needs at least two route processes with bound workstation and positive shiftHours' })
      continue
    }
    const processList = await apiGet(page, '/mes/pro/process/simple-list')
    const usedProcessIds = new Set(processRows.map((row) => Number(row.processId)))
    let replacementProcess
    for (const process of processList) {
      const processId = Number(process.id)
      if (!Number.isFinite(processId) || usedProcessIds.has(processId)) continue
      const workstationPage = await apiGet(
        page,
        `/mes/md-workstation/page?pageNo=1&pageSize=1&processId=${processId}`
      )
      if (!workstationPage.list?.length) {
        replacementProcess = process
        break
      }
    }
    if (!replacementProcess?.id) {
      scanned.push({ routeCode: route.code, reason: 'no unused process without existing workstation available for unbound target setup' })
      continue
    }
    return {
      route: { ...routeInfo, ...route, activeRouteVersionId: activeRouteVersionId || activeVersion?.id },
      activeVersion,
      templateSource: boundSources[0],
      templateTarget: boundSources[1],
      replacementProcess
    }
  }

  throw new Error(
    `BLOCKER: no route found that can be copied for a real workstation repair E2E. Scanned=${JSON.stringify(scanned.slice(0, 20))}`
  )
}

async function copyRouteThroughUi(page, sourceRoute) {
  const targetCode = config.copyRouteCode || buildE2ERouteCode()
  const targetName = `${targetCode} Capacity Repair E2E`
  const row = await filterRouteList(page, sourceRoute.route.code)
  await row.getByRole('button', { name: '复制' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '复制工艺路线' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[placeholder="请输入副本路线编码"]').fill(targetCode)
  await dialog.locator('input[placeholder="请输入副本路线名称"]').fill(targetName)
  const copyResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route/copy') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '确认复制' }).click()
  const copyResponse = await copyResponsePromise
  const copyPayload = await copyResponse.json()
  assert.ok(copyResponse.ok() && [0, 200].includes(copyPayload.code), `route copy failed: ${JSON.stringify(copyPayload)}`)
  const copiedRouteId = Number(copyPayload.data)
  assert.ok(Number.isFinite(copiedRouteId) && copiedRouteId > 0, `route copy did not return a valid id: ${JSON.stringify(copyPayload)}`)
  await dialog.waitFor({ state: 'detached', timeout: 30000 })
  await settle(page)
  const copiedRoute = await apiGet(page, `/mes/pro/route/get?id=${copiedRouteId}`)
  assert.equal(copiedRoute.code, targetCode, `copied route code mismatch: expected ${targetCode}, got ${copiedRoute.code}`)
  return copiedRoute
}

async function openDraftCandidateFromRouteList(page, copiedRoute) {
  const row = await filterRouteList(page, copiedRoute.code)
  const createCandidateResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/route-version/create-candidate') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    .catch(() => null)
  await row.getByRole('button', { name: '编辑' }).click()
  const createCandidateResponse = await createCandidateResponsePromise
  if (createCandidateResponse) {
    const createPayload = await createCandidateResponse.json()
    assert.ok(
      createCandidateResponse.ok() && [0, 200].includes(createPayload.code),
      `route candidate creation failed: ${JSON.stringify(createPayload)}`
    )
  }
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })
  await page.locator('.route-edit-page').first().waitFor({ state: 'visible', timeout: 60000 })
  return parseRouteVersionContextFromUrl(page.url())
}

async function openProcessTab(page, route, routeVersion) {
  const processUrl = new URL(`/mes/pro/route/edit/${route.id}`, config.baseUrl)
  processUrl.searchParams.set('tab', 'process')
  processUrl.searchParams.set('routeVersionId', String(routeVersion.id))
  processUrl.searchParams.set('routeVersionNo', routeVersion.versionNo)
  processUrl.searchParams.set('routeVersionStatus', routeVersion.lifecycleStatus)
  await page.goto(processUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.getByRole('tab', { name: '组成工序' }).click()
  await page.getByText('添加工序', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
}

async function changeTargetToUnboundProcess(page, target, replacementProcess) {
  const row = page.locator('tr.el-table__row').filter({ hasText: target.processName }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '编辑工序' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  const processField = dialog.locator('.el-form-item').filter({ hasText: '工序' }).first()
  await processField.locator('.el-select').click()
  const processOption = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: replacementProcess.name })
    .first()
  await processOption.waitFor({ state: 'visible', timeout: 30000 })
  await processOption.click()

  const updateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-process/update') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '确定' }).click()
  const updateResponse = await updateResponsePromise
  const updatePayload = await updateResponse.json()
  assert.ok(
    updateResponse.ok() && [0, 200].includes(updatePayload.code),
    `route process unbound setup failed: ${JSON.stringify(updatePayload)}`
  )
  const requestBody = JSON.parse(updateResponse.request().postData() || '{}')
  assert.equal(Number(requestBody.id), Number(target.id), 'unbound setup must update the selected target route process')
  assert.equal(Number(requestBody.processId), Number(replacementProcess.id), 'unbound setup must switch target to the unused process')
  assert.ok(!requestBody.workstationId, 'unbound setup must clear workstationId through the real form')
  await dialog.waitFor({ state: 'detached', timeout: 30000 })
  await page.locator('tr.el-table__row').filter({ hasText: replacementProcess.name }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  return { updatePayload, requestBody }
}

async function openDraftFlowPage(page, route, routeVersion, targetRouteProcessId) {
  const candidateUrl = new URL(`/mes/pro/route/edit/${route.id}`, config.baseUrl)
  candidateUrl.searchParams.set('tab', 'flow')
  if (targetRouteProcessId) {
    candidateUrl.searchParams.set('routeProcessId', String(targetRouteProcessId))
  }
  candidateUrl.searchParams.set('routeVersionId', String(routeVersion.id))
  candidateUrl.searchParams.set('routeVersionNo', routeVersion.versionNo)
  candidateUrl.searchParams.set('routeVersionStatus', routeVersion.lifecycleStatus)
  await page.goto(candidateUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  const editor = page.locator('.route-flow-graph-designer').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.locator('[data-flow-node="route-process"]').first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  return editor
}

async function confirmVisibleMessageBox(page) {
  const messageBox = page.locator('.el-message-box:visible').last()
  await messageBox.waitFor({ state: 'visible', timeout: 30000 })
  await messageBox.getByRole('button', { name: /确定|确认/ }).click()
  await messageBox.waitFor({ state: 'detached', timeout: 30000 }).catch(() => null)
}

async function addUnboundProcessThroughGraph(page, route, routeVersion, replacementProcess) {
  const beforeGraph = await apiGet(
    page,
    `/mes/pro/route-process-flow/get?routeId=${route.id}&routeVersionId=${routeVersion.id}`
  )
  const editor = await openDraftFlowPage(page, route, routeVersion)
  await editor.locator('[data-flow-action="add-route-process"]').click()

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '添加工序' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-flow-action="select-route-process"]').click()
  const optionLabel = replacementProcess.name || replacementProcess.code || String(replacementProcess.id)
  const option = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: optionLabel })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await dialog.locator('[data-flow-action="submit-add-route-process"]').click()
  await dialog.waitFor({ state: 'detached', timeout: 30000 })
  await editor.getByText(optionLabel, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })

  await editor.getByRole('button', { name: '自动生成' }).click()
  await confirmVisibleMessageBox(page)
  await editor.locator('[data-flow-status="unsaved"]').waitFor({ state: 'visible', timeout: 30000 })

  const validateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-process-flow/validate') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-process-flow/save') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await editor.locator('[data-flow-action="save-route-flow"]').click()
  const validateResponse = await validateResponsePromise
  const validatePayload = await validateResponse.json()
  assert.ok(
    validateResponse.ok() && [0, 200].includes(validatePayload.code) && validatePayload.data?.valid === true,
    `route flow validation failed after adding process: ${JSON.stringify(validatePayload)}`
  )
  const saveResponse = await saveResponsePromise
  const savePayload = await saveResponse.json()
  assert.ok(
    saveResponse.ok() && [0, 200].includes(savePayload.code) && savePayload.data?.valid === true,
    `route flow save failed after adding process: ${JSON.stringify(savePayload)}`
  )
  await editor.locator('[data-flow-status="unsaved"]').waitFor({ state: 'detached', timeout: 30000 }).catch(() => null)
  await settle(page)

  const afterGraph = await apiGet(
    page,
    `/mes/pro/route-process-flow/get?routeId=${route.id}&routeVersionId=${routeVersion.id}`
  )
  const addedNodes = (afterGraph.nodes || []).filter(
    (node) => Number(node.processId) === Number(replacementProcess.id)
  )
  assert.equal(
    addedNodes.length,
    1,
    `expected one added candidate graph node for ${optionLabel}, got ${JSON.stringify(addedNodes)}`
  )
  const targetNode = addedNodes[0]
  return {
    targetNode,
    savePayload,
    validatePayload,
    beforeGraphNodeCount: (beforeGraph.nodes || []).length,
    afterGraphNodeCount: (afterGraph.nodes || []).length
  }
}

async function openRouteGraph(page, candidate) {
  const { route, routeVersion, target } = candidate
  const candidateUrl = new URL(`/mes/pro/route/edit/${route.id}`, config.baseUrl)
  candidateUrl.searchParams.set('tab', 'flow')
  candidateUrl.searchParams.set('routeProcessId', String(target.id))
  candidateUrl.searchParams.set('capacitySourceFocus', 'schedule')
  if (routeVersion?.id) {
    candidateUrl.searchParams.set('routeVersionId', String(routeVersion.id))
    candidateUrl.searchParams.set('routeVersionNo', routeVersion.versionNo)
    candidateUrl.searchParams.set('routeVersionStatus', routeVersion.lifecycleStatus)
  }
  await page.goto(candidateUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  const editor = page.locator('.route-flow-graph-designer').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.locator('[data-flow-node="route-process"]').first().waitFor({ state: 'visible', timeout: 60000 })
  await editor
    .locator(`[data-flow-node="route-process"][data-route-process-id="${target.id}"]`)
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  return editor
}

async function submitDraftRouteVersionThroughUi(page, routeVersion) {
  const submitResponseMatcher = (response) =>
    response.url().includes('/mes/pro/route-version/submit-publish') &&
    response.request().method() === 'POST'
  let messageBox = page.locator('.el-message-box:visible').last()
  if (!(await messageBox.isVisible().catch(() => false))) {
    await page.locator('[data-route-version-action="submit-route-candidate"]').first().click()
    messageBox = page.locator('.el-message-box:visible').last()
    await messageBox.waitFor({ state: 'visible', timeout: 30000 })
  }
  const submitResponsePromise = page.waitForResponse(submitResponseMatcher, { timeout: 60000 })
  await messageBox.getByRole('button', { name: /确定|确认|保存草稿/ }).click()
  await messageBox.waitFor({ state: 'detached', timeout: 30000 }).catch(() => null)
  const signatureDialog = page.locator('.el-message-box:visible').filter({ hasText: '电子签名发布' }).last()
  await signatureDialog.waitFor({ state: 'visible', timeout: 30000 })
  await signatureDialog.locator('input[type="password"]').fill(config.signaturePassword)
  await signatureDialog.getByRole('button', { name: '确认签名并发布', exact: true }).click()
  const submitResponse = await submitResponsePromise
  const submitPayload = await submitResponse.json()
  assert.ok(
    submitResponse.ok() &&
      [0, 200].includes(submitPayload.code) &&
      submitPayload.data?.lifecycleStatus === 'ACTIVE',
    `route version submit-publish failed: ${JSON.stringify(submitPayload)}`
  )
  await settle(page)
  return submitPayload.data
}

async function publishReadyRouteVersionThroughUi(page, route, readyVersion) {
  const publishedRoute = await apiGet(page, `/mes/pro/route/get?id=${route.id}`)
  assert.equal(
    Number(publishedRoute.activeRouteVersionId),
    Number(readyVersion.id),
    `route version submit-publish must activate signed candidate: ${JSON.stringify(publishedRoute)}`
  )
  await settle(page)
  return readyVersion
}

async function confirmCandidateCreationIfPrompted(page) {
  const messageBox = page.locator('.el-message-box:visible').first()
  await messageBox.waitFor({ state: 'visible', timeout: 5000 }).catch(() => null)
  if (!(await messageBox.count()) || !(await messageBox.isVisible().catch(() => false))) {
    return null
  }
  const title = (await messageBox.locator('.el-message-box__title').innerText().catch(() => '')).trim()
  const content = (await messageBox.locator('.el-message-box__message').innerText().catch(() => '')).trim()
  const createCandidateResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/route-version/create-candidate') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    .catch(() => null)
  await messageBox.getByRole('button', { name: /确定|确认/ }).click()
  const createCandidateResponse = await createCandidateResponsePromise
  if (!createCandidateResponse) {
    return { title, content, candidate: null }
  }
  const payload = await createCandidateResponse.json()
  assert.ok(
    createCandidateResponse.ok() && [0, 200].includes(payload.code),
    `create route candidate failed: ${JSON.stringify(payload)}`
  )
  return { title, content, candidate: payload.data }
}

async function openCapacityOverride(editor, targetRouteProcessId) {
  const page = editor.page()
  const node = editor.locator(`[data-flow-node="route-process"][data-route-process-id="${targetRouteProcessId}"]`).first()
  await node.scrollIntoViewIfNeeded()
  await node.click()
  await editor
    .locator('[data-testid="route-flow-workstation-capacity-override-card"]')
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
  const button = editor.locator('[data-flow-action="open-capacity-override-dialog"]').first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await button.isDisabled(), false, 'capacity override button must be enabled before repair')
  await button.click()
  const candidateCreation = await confirmCandidateCreationIfPrompted(page)
  const dialog = page.locator('[data-testid="route-flow-capacity-override-dialog"]').first()
  if (candidateCreation) {
    await page.waitForURL((url) => url.searchParams.get('capacityOverride') === '1', {
      timeout: 60000,
      waitUntil: 'commit'
    })
    await settle(page)
  }
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  return { dialog, candidateCreation }
}

async function fillHourlyCapacity(dialog, hourlyCapacity) {
  const input = dialog.locator('[data-flow-field="capacity-override-hourly-capacity"] input').first()
  await input.fill(String(hourlyCapacity))
  await input.dispatchEvent('change')
  const actualValue = await input.inputValue()
  assert.equal(Number(actualValue), hourlyCapacity, `hourly capacity input should preserve ${hourlyCapacity}`)
}

async function triggerRepairDialog(page, capacityDialog) {
  await capacityDialog.locator('[data-flow-action="submit-capacity-override"]').click()
  const repairDialog = page.locator('[data-testid="route-flow-capacity-workstation-repair-dialog"]').first()
  await repairDialog.waitFor({ state: 'visible', timeout: 30000 })
  await repairDialog.getByText('绑定方式', { exact: true }).waitFor({
    state: 'visible',
    timeout: 30000
  })
  return repairDialog
}

async function chooseReuseSource(repairDialog, sourceRouteProcess) {
  const page = repairDialog.page()
  await repairDialog.getByText('绑定已有工作站', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  const sourceSelect = repairDialog.locator('[data-flow-field="capacity-workstation-repair-source-route-process"]').first()
  await sourceSelect.click()
  const sourceLabel = sourceRouteProcess.processName || sourceRouteProcess.processCode || String(sourceRouteProcess.id)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: sourceLabel }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const optionText = (await option.innerText()).replace(/\s+/g, ' ').trim()
  await option.click()
  return optionText
}

async function assertReuseSourceOptionAvailable(repairDialog, sourceRouteProcess) {
  const page = repairDialog.page()
  await repairDialog.getByText('绑定已有工作站', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  const sourceSelect = repairDialog.locator('[data-flow-field="capacity-workstation-repair-source-route-process"]').first()
  await sourceSelect.click()
  const sourceLabel = sourceRouteProcess.processName || sourceRouteProcess.processCode || String(sourceRouteProcess.id)
  const visibleOptions = page.locator('.el-select-dropdown__item:visible')
  await visibleOptions.first().waitFor({ state: 'visible', timeout: 30000 })
  const option = visibleOptions.filter({ hasText: sourceLabel }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const optionText = (await option.innerText()).replace(/\s+/g, ' ').trim()
  assert.ok(!/No data/i.test(optionText), `reuse source dropdown rendered No data instead of ${sourceLabel}`)
  const optionCount = await visibleOptions.count()
  await page.keyboard.press('Escape')
  return {
    optionText,
    optionCount
  }
}

async function submitReuseRepair(page, repairDialog) {
  const updateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-process/update') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await repairDialog.locator('[data-flow-action="submit-capacity-workstation-repair"]').click()
  const updateResponse = await updateResponsePromise
  const updatePayload = await updateResponse.json()
  assert.ok(
    updateResponse.ok() && [0, 200].includes(updatePayload.code),
    `route process workstation bind failed: ${JSON.stringify(updatePayload)}`
  )
  await repairDialog.waitFor({ state: 'hidden', timeout: 30000 })
  const capacityDialog = page.locator('[data-testid="route-flow-capacity-override-dialog"]').first()
  await capacityDialog.waitFor({ state: 'visible', timeout: 60000 })
  return {
    updatePayload,
    boundWorkstationId: JSON.parse(updateResponse.request().postData() || '{}').workstationId,
    capacityDialog
  }
}

async function chooseCreateWorkstationRepair(repairDialog, shiftHours) {
  const page = repairDialog.page()
  await repairDialog.getByText('新建工作站并绑定', { exact: true }).click()
  const workshopSelect = repairDialog.locator('[data-flow-field="capacity-workstation-repair-workshop"]').first()
  await workshopSelect.waitFor({ state: 'visible', timeout: 30000 })
  await workshopSelect.click()
  const option = page.locator('.el-select-dropdown__item:visible').first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const workshopText = (await option.innerText()).replace(/\s+/g, ' ').trim()
  await option.click()
  const shiftInput = repairDialog
    .locator('[data-flow-field="capacity-workstation-repair-shift-hours"] input')
    .first()
  await shiftInput.fill(String(shiftHours))
  await shiftInput.dispatchEvent('change')
  assert.equal(Number(await shiftInput.inputValue()), shiftHours, `repair shift hours should be ${shiftHours}`)
  return workshopText
}

async function submitCreateWorkstationRepair(page, repairDialog) {
  const autoCodeResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/md/auto-code-record/generate') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const createResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/md-workstation/create') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    .catch((error) => error)
  const updateResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/route-process/update') &&
        response.request().method() === 'PUT',
      { timeout: 60000 }
    )
    .catch((error) => error)
  await repairDialog.locator('[data-flow-action="submit-capacity-workstation-repair"]').click()
  const autoCodeResponse = await autoCodeResponsePromise
  const autoCodePayload = await autoCodeResponse.json()
  assert.ok(
    autoCodeResponse.ok() && [0, 200].includes(autoCodePayload.code) && autoCodePayload.data,
    `workstation auto-code failed before create: ${JSON.stringify(autoCodePayload)}`
  )
  const [createResponseResult, updateResponseResult] = await Promise.all([createResponsePromise, updateResponsePromise])
  assert.ok(
    !(createResponseResult instanceof Error),
    'workstation create request missing after auto-code ' + JSON.stringify(autoCodePayload) + ': ' + createResponseResult.message
  )
  assert.ok(
    !(updateResponseResult instanceof Error),
    'route process update request missing after auto-code ' + JSON.stringify(autoCodePayload) + ': ' + updateResponseResult.message
  )
  const createResponse = createResponseResult
  const updateResponse = updateResponseResult
  const createPayload = await createResponse.json()
  assert.ok(
    createResponse.ok() && [0, 200].includes(createPayload.code) && Number(createPayload.data) > 0,
    `workstation create failed: ${JSON.stringify(createPayload)}`
  )
  const updatePayload = await updateResponse.json()
  const updateBody = JSON.parse(updateResponse.request().postData() || '{}')
  assert.ok(
    updateResponse.ok() && [0, 200].includes(updatePayload.code),
    `route process workstation bind failed: ${JSON.stringify(updatePayload)}`
  )
  assert.equal(
    Number(updateBody.workstationId),
    Number(createPayload.data),
    'repair must bind the newly created workstation to the route process'
  )
  await repairDialog.waitFor({ state: 'hidden', timeout: 30000 })
  const capacityDialog = page.locator('[data-testid="route-flow-capacity-override-dialog"]').first()
  await capacityDialog.waitFor({ state: 'visible', timeout: 60000 })
  return {
    createPayload,
    updatePayload,
    createdWorkstationId: Number(createPayload.data),
    boundWorkstationId: Number(updateBody.workstationId),
    capacityDialog
  }
}

async function submitCapacityOverrideAfterRepair(page, capacityDialog) {
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-schedule-config/save') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await capacityDialog.locator('[data-flow-action="submit-capacity-override"]').click()
  const saveResponse = await saveResponsePromise
  const savePayload = await saveResponse.json()
  assert.ok(
    saveResponse.ok() && [0, 200].includes(savePayload.code),
    `capacity override save after repair failed: ${JSON.stringify(savePayload)}`
  )
  await capacityDialog.waitFor({ state: 'hidden', timeout: 30000 })
  return savePayload
}

async function readSavedScheduleConfig(page, routeVersionId, routeProcessId) {
  const configs = await apiGet(
    page,
    `/mes/pro/route-schedule-config/list-by-route-version?routeVersionId=${routeVersionId}`
  )
  const row = configs.find((item) => Number(item.routeProcessId) === Number(routeProcessId))
  assert.ok(row, `route schedule config missing after repair save: routeProcessId=${routeProcessId}`)
  return row
}

async function main() {
  assertSafeTarget()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  const evidence = {
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    requestedSourceRouteCode: config.sourceRouteCode || null,
    requestedCopyRouteCode: config.copyRouteCode || null,
    writeRequests: [],
    steps: []
  }

  try {
    await login(page)
    page.on('request', (request) => {
      const requestPath = adminApiPath(request.url())
      if (!requestPath) return
      if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())) {
        evidence.writeRequests.push(`${request.method()} ${requestPath}`)
      }
    })

    const autoCodeRuleSetup = await ensureWorkstationAutoCodeRuleThroughUi(page)
    evidence.autoCodeRuleSetup = autoCodeRuleSetup
    evidence.steps.push({ action: 'ensureWorkstationAutoCodeRule', status: autoCodeRuleSetup.status })

    const sourceRoute = await loadSourceRouteForCopy(page)
    const copiedRoute = await copyRouteThroughUi(page, sourceRoute)
    const draftRouteVersion = await openDraftCandidateFromRouteList(page, copiedRoute)
    const targetSetup = await addUnboundProcessThroughGraph(
      page,
      copiedRoute,
      draftRouteVersion,
      sourceRoute.replacementProcess
    )
    const readyVersion = await submitDraftRouteVersionThroughUi(page, draftRouteVersion)
    const activeVersion = await publishReadyRouteVersionThroughUi(page, copiedRoute, readyVersion)
    const publishedRoute = await apiGet(page, `/mes/pro/route/get?id=${copiedRoute.id}`)
    const publishedProcessRows = await apiGet(page, `/mes/pro/route-process/list-by-route?routeId=${copiedRoute.id}`)
    const target = publishedProcessRows.find(
      (row) =>
        Number(row.processId) === Number(sourceRoute.replacementProcess.id) &&
        Number(row.sort) === Number(targetSetup.targetNode.sort)
    )
    assert.ok(
      target,
      `published active route process missing for added processId=${sourceRoute.replacementProcess.id}`
    )
    assert.ok(!target.workstationId, `target must have no workstation before repair, got ${target.workstationId}`)
    assert.ok(!positiveNumber(target.shiftHours), `target must have no positive shiftHours before repair, got ${target.shiftHours}`)
    const reuseSource =
      publishedProcessRows.find(
        (row) =>
          Number(row.processId) === Number(sourceRoute.templateSource.processId) &&
          Number(row.sort) === Number(sourceRoute.templateSource.sort) &&
          row.workstationId
      ) ||
      publishedProcessRows.find(
        (row) => Number(row.id) !== Number(target.id) && row.id && row.workstationId
      )
    assert.ok(
      reuseSource?.workstationId,
      `published route must keep at least one workstation-bound process for reuse repair; rows=${JSON.stringify(
        publishedProcessRows.map((row) => ({
          id: row.id,
          processId: row.processId,
          sort: row.sort,
          workstationId: row.workstationId
        }))
      )}`
    )

    const candidate = {
      route: publishedRoute,
      routeVersion: null,
      target,
      source: reuseSource,
      sourceShiftHours: numericValue(reuseSource.shiftHours),
      targetOriginalWorkstationId: null,
      targetOriginalShiftHours: numericValue(target.shiftHours),
      targetSetup,
      readyVersion,
      activeVersion
    }
    const hourlyCapacity = 0.123457
    const repairShiftHours = 7.5
    evidence.sourceRouteCode = sourceRoute.route.code
    evidence.sourceRouteId = sourceRoute.route.id
    evidence.sourceActiveRouteVersionId = sourceRoute.activeVersion.id
    evidence.copiedRouteCode = candidate.route.code
    evidence.copiedRouteId = candidate.route.id
    evidence.initialRouteVersionId = draftRouteVersion.id
    evidence.initialRouteVersionNo = draftRouteVersion.versionNo
    evidence.initialRouteVersionStatus = draftRouteVersion.lifecycleStatus
    evidence.readyRouteVersionId = readyVersion.id
    evidence.activeRouteVersionId = activeVersion.id
    evidence.targetRouteProcessId = candidate.target.id
    evidence.targetProcess = rowLabel(candidate.target)
    evidence.targetOriginalWorkstationId = candidate.target.workstationId || null
    evidence.targetCopiedWorkstationIdBeforeUnbind = null
    evidence.targetOriginalShiftHours = candidate.targetOriginalShiftHours ?? null
    evidence.reuseSourceRouteProcessId = candidate.source.id
    evidence.reuseSourceProcess = rowLabel(candidate.source)
    evidence.reuseSourceWorkstationId = candidate.source.workstationId
    evidence.reuseSourceShiftHours = candidate.sourceShiftHours ?? null
    evidence.targetReplacementProcess = {
      id: sourceRoute.replacementProcess.id,
      code: sourceRoute.replacementProcess.code,
      name: sourceRoute.replacementProcess.name
    }
    evidence.targetSetup = {
      method: 'route-flow-graph-add-process',
      beforeGraphNodeCount: candidate.targetSetup.beforeGraphNodeCount,
      afterGraphNodeCount: candidate.targetSetup.afterGraphNodeCount,
      saveValid: candidate.targetSetup.savePayload.data?.valid === true
    }
    evidence.hourlyCapacity = hourlyCapacity
    evidence.repairShiftHours = repairShiftHours
    evidence.steps.push({ action: 'submitDraftRouteVersion', routeVersionId: readyVersion.id })
    evidence.steps.push({ action: 'publishRouteVersion', routeVersionId: activeVersion.id })

    const editor = await openRouteGraph(page, candidate)
    evidence.steps.push({ action: 'openRouteGraph', url: page.url() })

    const capacityOverrideEntry = await openCapacityOverride(editor, candidate.target.id)
    const { dialog: capacityDialog, candidateCreation } = capacityOverrideEntry
    const effectiveRouteVersion = candidateCreation?.candidate || candidate.routeVersion
    assert.ok(effectiveRouteVersion?.id, 'capacity override must create or use a draft route version')
    evidence.routeVersionId = effectiveRouteVersion.id
    evidence.routeVersionNo = effectiveRouteVersion.versionNo
    evidence.routeVersionStatus = effectiveRouteVersion.lifecycleStatus
    if (candidateCreation) {
      evidence.candidateCreation = candidateCreation
      evidence.steps.push({ action: 'confirmRouteCandidateCreation' })
    }
    await fillHourlyCapacity(capacityDialog, hourlyCapacity)
    evidence.steps.push({ action: 'openCapacityOverrideAndEnterHourlyCapacity' })

    const repairDialog = await triggerRepairDialog(page, capacityDialog)
    evidence.screenshotRepairDialog = await screenshot(page, 'capacity-workstation-repair-dialog.png')
    evidence.steps.push({ action: 'triggerRepairDialog' })

    evidence.reuseSourceDropdown = await assertReuseSourceOptionAvailable(repairDialog, candidate.source)
    evidence.steps.push({ action: 'assertReuseSourceDropdownHasWorkstationBoundProcess' })

    evidence.selectedCreateWorkshopText = await chooseCreateWorkstationRepair(repairDialog, repairShiftHours)
    const repairResult = await submitCreateWorkstationRepair(page, repairDialog)
    evidence.steps.push({
      action: 'createWorkstationAndBind',
      createdWorkstationId: Number(repairResult.createdWorkstationId),
      boundWorkstationId: Number(repairResult.boundWorkstationId)
    })

    const preservedInput = repairResult.capacityDialog
      .locator('[data-flow-field="capacity-override-hourly-capacity"] input')
      .first()
    await preservedInput.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(Number(await preservedInput.inputValue()), hourlyCapacity, 'repair flow must preserve entered hourly capacity')
    evidence.screenshotCapacityDialogAfterRepair = await screenshot(page, 'capacity-dialog-after-repair.png')
    evidence.steps.push({ action: 'capacityDialogReopenedWithHourlyCapacityPreserved' })

    const savePayload = await submitCapacityOverrideAfterRepair(page, repairResult.capacityDialog)
    const savedConfig = await readSavedScheduleConfig(page, effectiveRouteVersion.id, candidate.target.id)
    assert.equal(normalizeCapacityMode(savedConfig.capacityMode), 'MANUAL_OVERRIDE')
    assert.equal(Number(savedConfig.hourlyCapacity), hourlyCapacity)
    evidence.steps.push({ action: 'saveCapacityOverrideAfterRepair', savePayloadCode: savePayload.code })
    evidence.savedConfig = {
      id: savedConfig.id,
      routeVersionId: savedConfig.routeVersionId,
      routeProcessId: savedConfig.routeProcessId,
      capacityMode: normalizeCapacityMode(savedConfig.capacityMode),
      hourlyCapacity: Number(savedConfig.hourlyCapacity),
      shiftHours: Number(savedConfig.shiftHours)
    }
    assert.equal(Number(savedConfig.shiftHours), repairShiftHours, 'saved config should use the created workstation shift hours')

    const allowedWrites = new Set([
      'POST mes/pro/route/copy',
      'POST mes/pro/route-version/create-candidate',
      'POST mes/pro/route-version/submit-publish',
      'POST mes/pro/route-process-flow/validate',
      'POST mes/pro/route-process-flow/save',
      'POST mes/md/auto-code-rule/create',
      'POST mes/md/auto-code-part/create',
      'POST mes/md/auto-code-record/generate',
      'POST mes/md-workstation/create',
      'PUT mes/pro/route-process/update',
      'POST mes/pro/route-schedule-config/save'
    ])
    const unexpectedWrites = evidence.writeRequests.filter((item) => !allowedWrites.has(item))
    assert.deepEqual(unexpectedWrites, [], `unexpected write requests: ${unexpectedWrites.join(', ')}`)

    evidence.status = 'PASS'
    evidence.artifact = writeArtifact('capacity-workstation-repair-real-evidence.json', evidence)
    console.log(`mes-route-flow-capacity-workstation-repair-real PASS ${evidence.artifact}`)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    evidence.screenshotFailure = await screenshot(page, 'capacity-workstation-repair-failure.png').catch(() => null)
    evidence.artifact = writeArtifact('capacity-workstation-repair-real-evidence.json', evidence)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
