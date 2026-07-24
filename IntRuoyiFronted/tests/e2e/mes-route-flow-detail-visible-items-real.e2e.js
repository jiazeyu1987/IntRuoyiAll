const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const detailFieldsTableKey = 'mes.pro.route.flow.detailFields'
const batchRecordFieldKey = 'batchRecordFormNames'
const processCodeFieldKey = 'code'
const machineryFieldKey = 'machineryQuantityTotal'
const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_FLOW_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_E2E_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FLOW_E2E_ROUTE_CODE || 'RT000017',
  headed: process.env.MES_ROUTE_FLOW_E2E_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_E2E_ARTIFACT_DIR ||
      path.join(__dirname, '..', 'output', 'route-flow-detail-visible-items-real')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalOnly(baseUrl) {
  const parsed = new URL(baseUrl)
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(parsed.hostname),
    `MES_ROUTE_FLOW_E2E_BASE_URL must be local, got ${baseUrl}`
  )
}

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeArtifact(name, payload) {
  fs.writeFileSync(path.join(config.artifactDir, name), `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
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
  const accountInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  await accountInput.fill('')
  await accountInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
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
  assert.ok(accessToken, 'ACCESS_TOKEN missing after real login')
  assert.ok(tenantId, 'tenantId missing after real login')
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
  return page.evaluate(
    async ({ url, method: requestMethod, headers: requestHeaders, body: requestBody }) => {
      const response = await fetch(url, {
        method: requestMethod,
        headers: requestHeaders,
        body: requestBody === undefined ? undefined : JSON.stringify(requestBody)
      })
      const text = await response.text()
      let json
      try {
        json = JSON.parse(text)
      } catch {
        json = { raw: text }
      }
      return { ok: response.ok, status: response.status, json }
    },
    {
      url: `${config.baseUrl}/admin-api${apiPath}`,
      method,
      headers,
      body
    }
  )
}

async function apiGet(page, apiPath) {
  const response = await apiRequest(page, 'GET', apiPath)
  assert.equal(response.json.code, 0, `GET ${apiPath} failed: ${JSON.stringify(response)}`)
  return response.json.data
}

async function findRoute(page) {
  const data = await apiGet(
    page,
    `/mes/pro/route/page?pageNo=1&pageSize=10&code=${encodeURIComponent(config.routeCode)}`
  )
  const route = data.list.find((item) => item.code === config.routeCode)
  assert.ok(route, `route ${config.routeCode} missing`)
  return route
}

async function readUserConfig(page) {
  return apiGet(
    page,
    `/system/user-table-column-config/get?tableKey=${encodeURIComponent(detailFieldsTableKey)}`
  )
}

async function openRouteGraph(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  await page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
    .fill(config.routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)
  const row = page.locator('tr.el-table__row').filter({ hasText: config.routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })
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

async function clickRouteNode(editor, index) {
  const node = editor.locator('[data-flow-node="route-process"]').nth(index)
  await node.waitFor({ state: 'visible', timeout: 60000 })
  await node.click()
  await detailPanel(editor).getByText('关键工序', { exact: false }).waitFor({ state: 'visible', timeout: 10000 })
  return node
}

async function selectedNodeTitle(node) {
  return (await node.innerText()).replace(/\s+/g, ' ').trim()
}

function createDeferred() {
  let resolve
  const promise = new Promise((resolvePromise) => {
    resolve = resolvePromise
  })
  return { promise, resolve }
}

async function delayNextProcessDetailRequests(page) {
  const detailGate = createDeferred()
  const machineryGate = createDeferred()
  const detailBlocked = createDeferred()
  const machineryBlocked = createDeferred()
  let shouldDelayDetail = true
  let shouldDelayMachinery = true
  const detailPattern = '**/admin-api/mes/pro/process/get?**'
  const machineryPattern = '**/admin-api/mes/pro/process/machinery-list?**'
  const detailHandler = async (route) => {
    if (!shouldDelayDetail) {
      await route.continue()
      return
    }
    shouldDelayDetail = false
    detailBlocked.resolve(route.request().url())
    await detailGate.promise
    await route.continue()
  }
  const machineryHandler = async (route) => {
    if (!shouldDelayMachinery) {
      await route.continue()
      return
    }
    shouldDelayMachinery = false
    machineryBlocked.resolve(route.request().url())
    await machineryGate.promise
    await route.continue()
  }
  await page.route(detailPattern, detailHandler)
  await page.route(machineryPattern, machineryHandler)
  return {
    waitUntilBlocked: () => Promise.all([detailBlocked.promise, machineryBlocked.promise]),
    releaseDetail: () => detailGate.resolve(),
    releaseMachinery: () => machineryGate.resolve(),
    dispose: async () => {
      detailGate.resolve()
      machineryGate.resolve()
      await page.unroute(detailPattern, detailHandler)
      await page.unroute(machineryPattern, machineryHandler)
    }
  }
}

async function ensureDetailFieldVisible(page, editor, fieldKey, fieldLabel) {
  const panel = detailPanel(editor)
  const detailField = panel.locator(`[data-flow-detail-field="${fieldKey}"]`)
  if (await detailField.count()) {
    return false
  }
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/user-table-column-config/save') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await panel.locator('.route-flow-graph-designer__process-detail-field-picker .el-select').click()
  const dropdownOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', {
    hasText: fieldLabel
  }).last()
  await dropdownOption.waitFor({ state: 'visible', timeout: 10000 })
  await dropdownOption.click()
  await panel.locator('[data-flow-action="add-process-config-item"]').click()
  const saveResponse = await saveResponsePromise
  const savePayload = await saveResponse.json()
  assert.ok(
    saveResponse.ok() && [0, 200].includes(savePayload.code),
    `detail fields save failed: HTTP ${saveResponse.status()} ${JSON.stringify(savePayload)}`
  )
  await detailField.waitFor({ state: 'visible', timeout: 10000 })
  return true
}

async function ensureBatchRecordFieldVisible(page, editor) {
  return ensureDetailFieldVisible(page, editor, batchRecordFieldKey, '批记录表单')
}

async function assertPartialRefreshBehavior(page, editor, graphNodes) {
  const orderedNodes = graphNodes
    .slice()
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))
  assert.ok(orderedNodes.length >= 3, 'partial refresh E2E requires at least 3 process nodes')
  await ensureDetailFieldVisible(page, editor, processCodeFieldKey, '工序编码')
  await ensureDetailFieldVisible(page, editor, machineryFieldKey, '关联设备')

  const panel = detailPanel(editor)
  const detailFields = panel.locator('[data-flow-detail-field]')
  const codeField = panel.locator(`[data-flow-detail-field="${processCodeFieldKey}"]`)
  const machineryField = panel.locator(`[data-flow-detail-field="${machineryFieldKey}"]`)
  const loadingSelector = '.route-flow-graph-designer__process-detail-loading'
  const fieldCount = await detailFields.count()
  assert.ok(fieldCount >= 2, 'partial refresh E2E requires visible detail fields')
  await clickRouteNode(editor, 0)
  await codeField
    .locator('strong')
    .filter({ hasText: orderedNodes[0].processCode })
    .waitFor({ state: 'visible', timeout: 60000 })

  const independentDelay = await delayNextProcessDetailRequests(page)
  try {
    const secondNode = editor.locator('[data-flow-node="route-process"]').nth(1)
    await secondNode.click()
    await independentDelay.waitUntilBlocked()
    assert.equal(await panel.locator('.el-loading-mask').count(), 0, 'sidebar must not show loading mask')
    assert.equal(await detailFields.count(), fieldCount, 'detail field cards must remain mounted')
    await panel.getByText('关键工序', { exact: false }).waitFor({ state: 'visible', timeout: 10000 })
    await panel
      .locator('.route-flow-graph-designer__process-detail-field-picker')
      .waitFor({ state: 'visible', timeout: 10000 })
    await codeField.locator(loadingSelector).waitFor({ state: 'visible', timeout: 10000 })
    await machineryField.locator(loadingSelector).waitFor({ state: 'visible', timeout: 10000 })

    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/process/get?') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    independentDelay.releaseDetail()
    const detailResponse = await detailResponsePromise
    assert.ok(detailResponse.ok(), `process detail request failed: HTTP ${detailResponse.status()}`)
    await codeField.locator(loadingSelector).waitFor({ state: 'detached', timeout: 10000 })
    assert.equal(
      (await codeField.locator('strong').innerText()).trim(),
      orderedNodes[1].processCode,
      'ordinary process detail must update before machinery'
    )
    await machineryField.locator(loadingSelector).waitFor({ state: 'visible', timeout: 10000 })

    const machineryResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/process/machinery-list?') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    independentDelay.releaseMachinery()
    const machineryResponse = await machineryResponsePromise
    assert.ok(machineryResponse.ok(), `machinery request failed: HTTP ${machineryResponse.status()}`)
    await machineryField.locator(loadingSelector).waitFor({ state: 'detached', timeout: 10000 })
  } finally {
    await independentDelay.dispose()
  }

  const staleDelay = await delayNextProcessDetailRequests(page)
  try {
    const staleNode = editor.locator('[data-flow-node="route-process"]').nth(0)
    const latestNode = editor.locator('[data-flow-node="route-process"]').nth(2)
    await staleNode.click()
    await staleDelay.waitUntilBlocked()
    await latestNode.click()
    await codeField
      .locator('strong')
      .filter({ hasText: orderedNodes[2].processCode })
      .waitFor({ state: 'visible', timeout: 60000 })

    const staleDetailResponsePromise = page.waitForResponse(
      (response) => {
        if (!response.url().includes('/mes/pro/process/get?')) return false
        return new URL(response.url()).searchParams.get('id') === String(orderedNodes[0].processId)
      },
      { timeout: 60000 }
    )
    const staleMachineryResponsePromise = page.waitForResponse(
      (response) => {
        if (!response.url().includes('/mes/pro/process/machinery-list?')) return false
        return (
          new URL(response.url()).searchParams.get('processId') === String(orderedNodes[0].processId)
        )
      },
      { timeout: 60000 }
    )
    staleDelay.releaseDetail()
    staleDelay.releaseMachinery()
    await Promise.all([staleDetailResponsePromise, staleMachineryResponsePromise])
    await page.waitForTimeout(300)
    assert.equal(
      (await codeField.locator('strong').innerText()).trim(),
      orderedNodes[2].processCode,
      'stale response must not overwrite latest selected process'
    )
  } finally {
    await staleDelay.dispose()
  }

  return {
    fieldCount,
    processCodeFieldKey,
    machineryFieldKey,
    latestProcessCode: orderedNodes[2].processCode
  }
}

async function assertBatchRecordFieldVisibleForSelectedNode(editor, expectedNodeText) {
  const panel = detailPanel(editor)
  await panel.getByText('批记录表单', { exact: true }).waitFor({ state: 'visible', timeout: 10000 })
  const panelText = (await panel.innerText()).replace(/\s+/g, ' ')
  assert.match(panelText, /批记录表单/, 'selected detail panel must include 批记录表单')
  assert.notEqual(panelText.includes('点击加号添加关注字段'), true, 'selected detail list must not be empty')
  return {
    expectedNodeText,
    panelText
  }
}

async function main() {
  assertLocalOnly(config.baseUrl)
  assert.equal(config.tenant, '测试租户', `write E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `write E2E must use aoteman, got ${config.username}`)
  ensureArtifactDir()

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const pageErrors = []
  const consoleErrors = []
  const requestFailures = []

  try {
    const context = await browser.newContext({ viewport: { width: 1366, height: 768 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') {
        const location = message.location()
        const source = location.url ? ` @ ${location.url}` : ''
        consoleErrors.push(`${message.text()}${source}`)
      }
    })
    page.on('requestfailed', (request) => {
      const failure = request.failure()
      requestFailures.push({
        url: request.url(),
        method: request.method(),
        resourceType: request.resourceType(),
        errorText: failure ? failure.errorText : ''
      })
    })

    await login(page)
    const route = await findRoute(page)
    const graph = await apiGet(page, `/mes/pro/route-process-flow/get?routeId=${route.id}`)
    assert.ok(graph.nodes.length >= 2, 'route needs at least 2 process nodes')

    let editor = await openRouteGraph(page)
    const firstNode = await clickRouteNode(editor, 0)
    const firstNodeText = await selectedNodeTitle(firstNode)
    const changedByTest = await ensureBatchRecordFieldVisible(page, editor)
    const addedProcessCodeField = await ensureDetailFieldVisible(
      page,
      editor,
      processCodeFieldKey,
      '工序编码'
    )
    const addedMachineryField = await ensureDetailFieldVisible(
      page,
      editor,
      machineryFieldKey,
      '关联设备'
    )
    const savedConfig = await readUserConfig(page)
    const savedBatchColumn = savedConfig.columns.find((column) => column.key === batchRecordFieldKey)
    assert.ok(savedBatchColumn?.visible, `saved config must include visible ${batchRecordFieldKey}`)

    const secondNode = await clickRouteNode(editor, 1)
    const secondNodeText = await selectedNodeTitle(secondNode)
    assert.notEqual(firstNodeText, secondNodeText, 'E2E route must expose different selected process nodes')
    const secondEvidence = await assertBatchRecordFieldVisibleForSelectedNode(editor, secondNodeText)
    const partialRefreshEvidence = await assertPartialRefreshBehavior(page, editor, graph.nodes)

    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    editor = page.locator('.route-edit-page').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })
    await editor.getByRole('tab', { name: '流转关系图' }).click()
    await editor.locator('[data-flow-node="route-process"]').first().waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)
    const afterReloadNode = await clickRouteNode(editor, 0)
    const afterReloadNodeText = await selectedNodeTitle(afterReloadNode)
    const afterReloadEvidence = await assertBatchRecordFieldVisibleForSelectedNode(editor, afterReloadNodeText)

    await context.close()

    const freshContext = await browser.newContext({ viewport: { width: 1366, height: 768 } })
    const freshPage = await freshContext.newPage()
    freshPage.setDefaultTimeout(60000)
    freshPage.setDefaultNavigationTimeout(60000)
    await login(freshPage)
    const freshEditor = await openRouteGraph(freshPage)
    const freshNode = await clickRouteNode(freshEditor, 0)
    const freshNodeText = await selectedNodeTitle(freshNode)
    const freshEvidence = await assertBatchRecordFieldVisibleForSelectedNode(freshEditor, freshNodeText)
    const freshConfig = await readUserConfig(freshPage)
    await freshPage.screenshot({
      path: path.join(config.artifactDir, 'route-flow-detail-visible-items-real.png'),
      fullPage: true
    })
    await freshContext.close()

    const unexpectedRequestFailures = requestFailures.filter((failure) => {
      if (failure.url.includes('/system/user-table-column-config')) return true
      if (
        failure.url.includes('api.iconify.design') ||
        failure.url.includes('hm.baidu.com') ||
        failure.url.includes('/infra/file/') ||
        failure.url.includes('/profile/') ||
        failure.url.includes('/favicon')
      ) {
        return false
      }
      if (failure.errorText === 'net::ERR_ABORTED') return false
      return true
    })
    const evidence = {
      ok: true,
      routeId: route.id,
      routeCode: route.code,
      tenant: config.tenant,
      username: config.username,
      changedByTest,
      addedProcessCodeField,
      addedMachineryField,
      tableKey: detailFieldsTableKey,
      savedBatchColumn,
      savedVisibleKeys: savedConfig.columns.filter((column) => column.visible !== false).map((column) => column.key),
      freshVisibleKeys: freshConfig.columns.filter((column) => column.visible !== false).map((column) => column.key),
      firstNodeText,
      secondNodeText,
      secondEvidence,
      partialRefreshEvidence,
      afterReloadEvidence,
      freshEvidence,
      pageErrors,
      consoleErrors,
      requestFailures: unexpectedRequestFailures
    }
    writeArtifact('route-flow-detail-visible-items-real-result.json', evidence)
    assert.deepEqual(pageErrors, [], `page errors detected: ${pageErrors.join('\n')}`)
    assert.deepEqual(consoleErrors, [], `console errors detected: ${consoleErrors.join('\n')}`)
    assert.deepEqual(
      unexpectedRequestFailures,
      [],
      `request failures detected: ${JSON.stringify(unexpectedRequestFailures, null, 2)}`
    )
    console.log(
      `PASS: route flow detail visible items real E2E route=${route.code} field=${batchRecordFieldKey}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
