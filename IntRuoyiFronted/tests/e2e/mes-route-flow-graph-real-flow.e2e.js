const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_FLOW_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_E2E_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FLOW_E2E_ROUTE_CODE || 'RT000017',
  headed: process.env.MES_ROUTE_FLOW_E2E_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_E2E_ARTIFACT_DIR ||
      path.join(__dirname, '..', '..', '..', 'runtime', 'p6-e2e')
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
  const filePath = path.join(config.artifactDir, name)
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return filePath
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
  if (!page.url().includes('/login')) {
    return
  }
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  await tenantInput.click()
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await form.locator('input.el-input__inner:not([role="combobox"])').first().fill('')
  await form.locator('input.el-input__inner:not([role="combobox"])').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginButton = form.getByRole('button', { name: '登录' }).first()
  await loginButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await loginButton.isEnabled(), true, 'login button must be enabled')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginButton.click()
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
  if (visitTenantId) {
    headers['visit-tenant-id'] = String(visitTenantId)
  }
  return headers
}

async function apiRequest(page, method, apiPath, body) {
  const headers = await authHeaders(page)
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
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

async function apiPost(page, apiPath, body) {
  const response = await apiRequest(page, 'POST', apiPath, body)
  assert.equal(response.json.code, 0, `POST ${apiPath} failed: ${JSON.stringify(response)}`)
  return response.json.data
}

async function apiPostRaw(page, apiPath, body) {
  return apiRequest(page, 'POST', apiPath, body)
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

async function openRouteGraph(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)

  await page.locator('input[placeholder="请输入工艺路线编码"]').first().fill(config.routeCode)
  await page.getByRole('button', { name: /搜索/ }).first().click()
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

async function clickBottomSaveAndWait(page, dialog, expectedGraphSave = true) {
  const saveResponsePromise = expectedGraphSave
    ? page.waitForResponse(
        (response) =>
          response.url().includes('/mes/pro/route-process-flow/save') &&
          response.request().method() === 'POST',
        { timeout: 60000 }
      )
    : null
  await dialog.getByRole('button', { name: '保 存' }).last().click()
  if (!saveResponsePromise) return null
  const saveResponse = await saveResponsePromise
  const savePayload = await saveResponse.json()
  assert.ok(
    saveResponse.ok() && [0, 200].includes(savePayload.code),
    `save failed: HTTP ${saveResponse.status()} ${JSON.stringify(savePayload)}`
  )
  await page.getByText('保存成功', { exact: false }).first().waitFor({ state: 'visible' })
  return savePayload
}

async function oneScreenMetrics(page, dialog) {
  return dialog.evaluate(() => {
    const html = document.documentElement
    const body = document.body
    const root = document.querySelector('.route-flow-graph-designer')
    const main = document.querySelector('.route-flow-graph-designer__main')
    const flow = document.querySelector('.route-flow-graph-designer__flow')
    const panel = document.querySelector('.route-flow-graph-designer__panel')
    const readBox = (el) => {
      if (!el) return null
      const rect = el.getBoundingClientRect()
      const style = getComputedStyle(el)
      return {
        width: rect.width,
        height: rect.height,
        overflowX: style.overflowX,
        overflowY: style.overflowY,
        scrollWidth: el.scrollWidth,
        clientWidth: el.clientWidth,
        scrollHeight: el.scrollHeight,
        clientHeight: el.clientHeight
      }
    }
    return {
      html: {
        scrollWidth: html.scrollWidth,
        clientWidth: html.clientWidth,
        scrollHeight: html.scrollHeight,
        clientHeight: html.clientHeight
      },
      body: {
        scrollWidth: body.scrollWidth,
        clientWidth: body.clientWidth,
        scrollHeight: body.scrollHeight,
        clientHeight: body.clientHeight
      },
      root: readBox(root),
      main: readBox(main),
      flow: readBox(flow),
      panel: readBox(panel)
    }
  })
}

function assertNoPageScrollbar(metrics) {
  assert.ok(
    metrics.html.scrollHeight <= metrics.html.clientHeight + 1,
    `html should not need vertical page scrollbar: ${JSON.stringify(metrics.html)}`
  )
  assert.ok(
    metrics.body.scrollHeight <= metrics.body.clientHeight + 1,
    `body should not need vertical page scrollbar: ${JSON.stringify(metrics.body)}`
  )
  for (const key of ['root', 'main', 'flow']) {
    assert.equal(metrics[key].overflowY, 'hidden', `${key} must hide vertical overflow`)
  }
}

async function nodeIds(dialog) {
  const ids = await dialog.locator('[data-flow-node="route-process"]').evaluateAll((nodes) =>
    nodes.map((node) => Number(node.getAttribute('data-route-process-id')))
  )
  assert.ok(ids.length >= 5, `expected at least 5 nodes, got ${ids.length}`)
  return ids
}

async function connect(dialog, sourceId, targetId) {
  const source = dialog.locator(`[data-flow-handle="source"][data-route-process-id="${sourceId}"]`).first()
  const target = dialog.locator(`[data-flow-handle="target"][data-route-process-id="${targetId}"]`).first()
  const sourceBox = await source.boundingBox()
  const targetBox = await target.boundingBox()
  assert.ok(sourceBox, `source handle missing: ${sourceId}`)
  assert.ok(targetBox, `target handle missing: ${targetId}`)
  const sourceStyle = await source.evaluate((element) => getComputedStyle(element))
  const targetStyle = await target.evaluate((element) => getComputedStyle(element))
  assert.equal(sourceStyle.width, '24px', `source handle design width changed: ${sourceStyle.width}`)
  assert.equal(sourceStyle.height, '24px', `source handle design height changed: ${sourceStyle.height}`)
  assert.equal(targetStyle.width, '24px', `target handle design width changed: ${targetStyle.width}`)
  assert.equal(targetStyle.height, '24px', `target handle design height changed: ${targetStyle.height}`)
  assert.ok(sourceBox.width >= 6 && sourceBox.height >= 6, `source handle rendered hit area too small: ${JSON.stringify(sourceBox)}`)
  assert.ok(targetBox.width >= 6 && targetBox.height >= 6, `target handle rendered hit area too small: ${JSON.stringify(targetBox)}`)
  await dialog.page().mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2)
  await dialog.page().mouse.down()
  await dialog.page().mouse.move(sourceBox.x + 120, sourceBox.y + sourceBox.height / 2, { steps: 10 })
  await dialog.page().mouse.move((sourceBox.x + targetBox.x) / 2, (sourceBox.y + targetBox.y) / 2, { steps: 14 })
  await dialog.page().mouse.move(targetBox.x - 120, targetBox.y + targetBox.height / 2, { steps: 10 })
  await dialog.page().mouse.move(targetBox.x + targetBox.width / 2, targetBox.y + targetBox.height / 2, { steps: 8 })
  await dialog.page().mouse.up()
  await dialog.page().waitForTimeout(800)
  try {
    await dialog.locator(`.vue-flow__edge[data-id="${sourceId}->${targetId}"]`).waitFor({
      state: 'attached',
      timeout: 10000
    })
  } catch (error) {
    const debug = await dialog.evaluate(
      (root, edgeId) => ({
        expectedEdgeId: edgeId,
        edges: [...root.querySelectorAll('.vue-flow__edge')].map((edge) => edge.getAttribute('data-id')),
        handles: [...root.querySelectorAll('[data-flow-handle]')].map((handle) => {
          const box = handle.getBoundingClientRect()
          return {
            id: handle.getAttribute('data-route-process-id'),
            type: handle.getAttribute('data-flow-handle'),
            x: box.x,
            y: box.y,
            width: box.width,
            height: box.height
          }
        })
      }),
      `${sourceId}->${targetId}`
    )
    writeArtifact(`connect-failed-${sourceId}-${targetId}.json`, debug)
    await dialog.page().screenshot({
      path: path.join(config.artifactDir, `connect-failed-${sourceId}-${targetId}.png`),
      fullPage: true
    })
    throw error
  }
}

async function selectEdge(dialog, sourceId, targetId) {
  const edge = dialog.locator(`.vue-flow__edge[data-id="${sourceId}->${targetId}"]`).first()
  await edge.waitFor({ state: 'attached', timeout: 10000 })
  const box = await edge.boundingBox()
  assert.ok(box, `edge has no box: ${sourceId}->${targetId}`)
  await dialog.page().mouse.click(box.x + box.width / 2, box.y + box.height / 2)
  await dialog.page().waitForTimeout(300)
}

async function nodeName(dialog, routeProcessId) {
  const name = await dialog
    .locator(`[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"] .route-flow-graph-designer__node-name`)
    .first()
    .innerText()
  return name.trim()
}

async function selectDropdownOptionByText(page, text) {
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await dropdown.waitFor({ state: 'visible', timeout: 10000 })
  const options = await dropdown.locator('.el-select-dropdown__item:not(.is-disabled)').elementHandles()
  for (const option of options) {
    const optionText = await option.evaluate((element) => element.textContent?.trim() || '')
    if (optionText === text) {
      await option.evaluate((element) => {
        element.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))
        element.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
        element.dispatchEvent(new MouseEvent('click', { bubbles: true }))
      })
      await page.waitForTimeout(200)
      return
    }
  }
  throw new Error(`select option not found: ${text}`)
}

async function assertGraphEditActions(dialog) {
  await dialog.locator('[data-flow-action="add-route-process"]').waitFor({ state: 'visible', timeout: 10000 })
  await dialog.locator('[data-flow-action="add-edge-dialog"]').waitFor({ state: 'visible', timeout: 10000 })
  await dialog.locator('[data-flow-node="route-process"]').first().click()
  await dialog.locator('[data-flow-action="delete-route-process"]').waitFor({ state: 'visible', timeout: 10000 })
}

async function addEdgeWithDialog(dialog, sourceId, targetId) {
  const sourceName = await nodeName(dialog, sourceId)
  const targetName = await nodeName(dialog, targetId)
  await dialog.locator('[data-flow-action="add-edge-dialog"]').click()
  const page = dialog.page()
  const edgeDialog = page.locator('.el-dialog:visible').filter({ hasText: '添加连接线' }).last()
  await edgeDialog.waitFor({ state: 'visible', timeout: 10000 })
  const sourceSelect = edgeDialog.locator('[data-flow-action="select-edge-source"]')
  await sourceSelect.waitFor({ state: 'visible', timeout: 10000 })
  await sourceSelect.click()
  await selectDropdownOptionByText(page, sourceName)
  const targetSelect = edgeDialog.locator('[data-flow-action="select-edge-target"]')
  await targetSelect.waitFor({ state: 'visible', timeout: 10000 })
  await targetSelect.click()
  await selectDropdownOptionByText(page, targetName)
  await edgeDialog.locator('[data-flow-action="submit-add-edge"]').click()
  await edgeDialog.waitFor({ state: 'hidden', timeout: 10000 })
  await dialog.locator(`.vue-flow__edge[data-id="${sourceId}->${targetId}"]`).waitFor({
    state: 'attached',
    timeout: 10000
  })
}

async function clickConfirm(page) {
  const confirmButton = page.locator('.el-message-box:visible').getByRole('button', { name: /确认|确定/ }).last()
  await confirmButton.waitFor({ state: 'visible', timeout: 10000 })
  await confirmButton.click()
}

async function ensureGraphState(page, routeId, nodes) {
  const graph = await apiGet(page, `/mes/pro/route-process-flow/get?routeId=${routeId}`)
  const expectedEdges = nodes.slice(0, -1).map((node, index) => ({
    sourceRouteProcessId: node,
    targetRouteProcessId: nodes[index + 1]
  }))
  const actualKeys = graph.edges
    .map((edge) => `${edge.sourceRouteProcessId}->${edge.targetRouteProcessId}`)
    .sort()
  const expectedKeys = expectedEdges
    .map((edge) => `${edge.sourceRouteProcessId}->${edge.targetRouteProcessId}`)
    .sort()
  assert.deepEqual(actualKeys, expectedKeys, `persisted graph edges mismatch: ${JSON.stringify(graph.edges)}`)
  assert.equal(graph.validationStatus, 'VALID', `persisted graph should be VALID: ${JSON.stringify(graph)}`)
  assert.equal(graph.valid, true, `persisted graph should be valid: ${JSON.stringify(graph)}`)
  assert.ok(
    graph.nodes.every((node) => node.processCode && node.processName),
    `persisted node labels missing: ${JSON.stringify(graph.nodes.slice(0, 5))}`
  )
  return graph
}

async function readDraftPayload(dialog, routeId) {
  const draft = await dialog.evaluate((rootElement, id) => {
    const scope = rootElement instanceof Element ? rootElement : document
    const nodeRecords = Array.from(scope.querySelectorAll('[data-flow-node="route-process"]')).map((nodeElement) => {
      const box = nodeElement.getBoundingClientRect()
      return {
        routeProcessId: Number(nodeElement.getAttribute('data-route-process-id')),
        x: Math.round(box.x),
        y: Math.round(box.y),
        width: Math.round(box.width),
        height: Math.round(box.height)
      }
    })
    const edges = Array.from(scope.querySelectorAll('.vue-flow__edge'))
      .map((edge) => edge.getAttribute('data-id') || '')
      .filter(Boolean)
      .map((key) => {
        const [sourceRouteProcessId, targetRouteProcessId] = key.split('->').map(Number)
        return { sourceRouteProcessId, targetRouteProcessId, relationType: 'NORMAL' }
      })
      .filter(
        (edge) =>
          Number.isFinite(edge.sourceRouteProcessId) && Number.isFinite(edge.targetRouteProcessId)
      )
    return {
      routeId: Number(id),
      edges,
      layouts: nodeRecords
    }
  }, routeId)
  return JSON.parse(JSON.stringify(draft))
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
    const initialGraph = await apiGet(page, `/mes/pro/route-process-flow/get?routeId=${route.id}`)
    assert.ok(initialGraph.nodes.length >= 5, 'route needs at least 5 process nodes')
    assert.ok(
      initialGraph.nodes.every((node) => node.processCode && node.processName),
      `initial node labels missing: ${JSON.stringify(initialGraph.nodes.slice(0, 5))}`
    )

    let dialog = await openRouteGraph(page)
    const ids = await nodeIds(dialog)
    const firstText = await dialog.locator(`[data-flow-node="route-process"][data-route-process-id="${ids[0]}"]`).innerText()
    assert.match(firstText, /工序生产记录/, `node should display process name, got: ${firstText}`)
    await assertGraphEditActions(dialog)
    const metricsBefore = await oneScreenMetrics(page, dialog)
    assertNoPageScrollbar(metricsBefore)

    const confirmPromise = clickConfirm(page)
    await dialog.getByRole('button', { name: '根据序号生成线性关系' }).click()
    await confirmPromise
    await dialog.locator(`.vue-flow__edge[data-id="${ids[0]}->${ids[1]}"]`).waitFor({
      state: 'attached',
      timeout: 10000
    })
    await dialog.getByRole('button', { name: '自动布局' }).click()
    await dialog.locator('.el-message', { hasText: /已按/ }).last().waitFor({ state: 'visible', timeout: 10000 }).catch(() => null)
    await page.waitForTimeout(800)

    await connect(dialog, ids[0], ids[2])
    await dialog.locator(`[data-flow-action="delete-edge-list"][data-edge-key="${ids[0]}->${ids[2]}"]`).click()
    await dialog.locator(`.vue-flow__edge[data-id="${ids[0]}->${ids[2]}"]`).waitFor({
      state: 'detached',
      timeout: 10000
    })
    await addEdgeWithDialog(dialog, ids[2], ids[0])
    await dialog.locator(`[data-flow-action="select-edge-list"][data-edge-key="${ids[2]}->${ids[0]}"]`).click()
    await dialog.locator('[data-flow-action="delete-selected-edge"]').click()
    await dialog.locator(`.vue-flow__edge[data-id="${ids[2]}->${ids[0]}"]`).waitFor({
      state: 'detached',
      timeout: 10000
    })

    const draftBeforeSave = await readDraftPayload(dialog, route.id)
    const liveBeforeSave = await apiGet(page, `/mes/pro/route-process-flow/get?routeId=${route.id}`)
    const validationPayload = {
      ...draftBeforeSave,
      graphVersion: liveBeforeSave.graphVersion
    }
    const validationRawBeforeSave = await apiPostRaw(page, '/mes/pro/route-process-flow/validate', validationPayload)
    writeArtifact('route-flow-real-e2e-before-save-debug.json', {
      routeId: route.id,
      ids,
      draftBeforeSave,
      liveGraphVersion: liveBeforeSave.graphVersion,
      validationPayload,
      validationRawBeforeSave
    })
    assert.equal(
      validationRawBeforeSave.json.code,
      0,
      `validate before save failed: ${JSON.stringify(validationRawBeforeSave)}`
    )
    const validationBeforeSave = validationRawBeforeSave.json.data
    assert.equal(
      validationBeforeSave.valid,
      true,
      `draft graph should be valid before save: ${JSON.stringify(validationBeforeSave)}`
    )

    await clickBottomSaveAndWait(page, dialog)

    const persisted = await ensureGraphState(page, route.id, ids)
    await page.screenshot({ path: path.join(config.artifactDir, 'route-flow-real-e2e-saved-1366.png'), fullPage: true })

    dialog = await openRouteGraph(page)
    await dialog.locator(`.vue-flow__edge[data-id="${ids[0]}->${ids[1]}"]`).waitFor({
      state: 'attached',
      timeout: 10000
    })
    await dialog.locator(`.vue-flow__edge[data-id="${ids[2]}->${ids[3]}"]`).waitFor({
      state: 'attached',
      timeout: 10000
    })
    const metricsAfter = await oneScreenMetrics(page, dialog)
    assertNoPageScrollbar(metricsAfter)

    await addEdgeWithDialog(dialog, ids[3], ids[0])
    const invalidValidateResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/route-process-flow/validate') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    const unexpectedSavePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes('/mes/pro/route-process-flow/save') &&
          response.request().method() === 'POST',
        { timeout: 2500 }
      )
      .then(() => true)
      .catch(() => false)
    await clickBottomSaveAndWait(page, dialog, false).catch(() => null)
    const invalidValidateResponse = await invalidValidateResponsePromise
    const invalidValidatePayload = await invalidValidateResponse.json()
    assert.equal(
      invalidValidatePayload.code,
      0,
      `cycle validate request should complete with validation payload: ${JSON.stringify(invalidValidatePayload)}`
    )
    assert.equal(
      invalidValidatePayload.data?.valid,
      false,
      `cycle validation should fail before save: ${JSON.stringify(invalidValidatePayload)}`
    )
    assert.equal(await unexpectedSavePromise, false, 'cycle graph must not call save after pre-validation failure')
    assert.match(
      JSON.stringify(invalidValidatePayload),
      /流转关系图无效|无效|循环|cycle|CYCLE/i,
      `cycle validation should expose invalid reason: ${JSON.stringify(invalidValidatePayload)}`
    )
    const persistedAfterFailedSave = await ensureGraphState(page, route.id, ids)

    const expectedValidationPageErrorPattern =
      /流转关系图无效|同一工序只能存在一个前置工序|只能存在一个开始工序|工序流转关系存在循环/
    const evidence = {
      ok: true,
      routeId: route.id,
      routeCode: route.code,
      tenant: config.tenant,
      username: config.username,
      nodeCount: ids.length,
      graphVersionBefore: initialGraph.graphVersion,
      graphVersionAfter: persisted.graphVersion,
      validationStatusAfter: persisted.validationStatus,
      edgeCountAfter: persisted.edges.length,
      persistedVersionAfterFailedCycle: persistedAfterFailedSave.graphVersion,
      oneScreenBefore: metricsBefore,
      oneScreenAfter: metricsAfter,
      expectedPageErrors: pageErrors.filter((error) => expectedValidationPageErrorPattern.test(error)),
      unexpectedPageErrors: pageErrors.filter((error) => !expectedValidationPageErrorPattern.test(error)),
      consoleErrors,
      requestFailures
    }
    writeArtifact('route-flow-real-e2e-result.json', evidence)
    assert.deepEqual(evidence.unexpectedPageErrors, [], `page errors detected: ${evidence.unexpectedPageErrors.join('\n')}`)
    assert.deepEqual(consoleErrors, [], `console errors detected: ${consoleErrors.join('\n')}`)
    console.log(`PASS: MES route flow graph real E2E route=${route.code} routeId=${route.id} nodes=${ids.length}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
