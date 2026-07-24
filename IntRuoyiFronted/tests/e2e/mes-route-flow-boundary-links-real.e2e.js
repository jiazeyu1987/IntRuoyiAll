const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_E2E_BASE_URL || 'http://127.0.0.1:8094').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_FLOW_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_E2E_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FLOW_E2E_ROUTE_CODE || 'RT000017',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_E2E_ARTIFACT_DIR ||
      path.join(__dirname, '..', '..', '..', 'runtime', 'route-flow-boundary-links-e2e')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertWriteTarget() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', `real E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `real E2E must use aoteman, got ${config.username}`)
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
  await tenantInput.click()
  await tenantInput.fill(config.tenant)
  const tenantOption = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: config.tenant })
    .first()
  if (await tenantOption.count()) {
    await tenantOption.click()
  } else {
    await tenantInput.press('Enter')
  }

  const accountInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  await accountInput.fill('')
  await accountInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
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
  assert.equal(String(tenantId), '122', `write E2E must use tenant-id=122, got ${tenantId}`)
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
  assert.ok(
    response.ok && response.json.code === 0,
    `GET ${apiPath} failed: ${JSON.stringify(response)}`
  )
  return response.json.data
}

async function openRouteGraph(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page)

  const codeInput = page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
  await codeInput.fill(config.routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)

  const row = page.locator('tr.el-table__row').filter({ hasText: config.routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), {
    timeout: 60000
  })

  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.getByRole('tab', { name: '流转关系图' }).click()
  await editor.locator('[data-flow-node="route-process"]').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await editor.locator('[data-flow-node="route-boundary"][data-flow-boundary="START"]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page)
  return editor
}

function routeIdFromUrl(page) {
  const url = new URL(page.url())
  const queryId = Number(url.searchParams.get('id'))
  if (Number.isFinite(queryId) && queryId > 0) return queryId
  const pathId = Number(url.pathname.split('/').filter(Boolean).at(-1))
  assert.ok(Number.isFinite(pathId) && pathId > 0, `route id missing from ${url}`)
  return pathId
}

async function relationKeys(editor) {
  const ordinary = await editor
    .locator('[data-flow-action="select-edge-list"]')
    .evaluateAll((items) => items.map((item) => item.getAttribute('data-edge-key')).filter(Boolean))
  const boundary = await editor
    .locator('[data-flow-action="select-boundary-edge-list"]')
    .evaluateAll((items) => items.map((item) => item.getAttribute('data-edge-key')).filter(Boolean))
  return {
    ordinary: ordinary.sort(),
    boundary: boundary.sort()
  }
}

function splitOrdinaryKey(key) {
  const match = /^(\d+)->(\d+)$/.exec(key)
  assert.ok(match, `invalid ordinary relation key ${key}`)
  return [Number(match[1]), Number(match[2])]
}

function processIdFromBoundaryKey(key) {
  const startMatch = /^process-start->(\d+)$/.exec(key)
  if (startMatch) return { boundaryType: 'START', routeProcessId: Number(startMatch[1]) }
  const endMatch = /^(\d+)->process-end$/.exec(key)
  assert.ok(endMatch, `invalid boundary relation key ${key}`)
  return { boundaryType: 'END', routeProcessId: Number(endMatch[1]) }
}

function findThreeNodeChain(topology) {
  const startKeys = topology.boundary.filter((key) => key.startsWith('process-start->'))
  const endKeys = topology.boundary.filter((key) => key.endsWith('->process-end'))
  assert.equal(startKeys.length, 1, `route must initially have exactly one START relation: ${startKeys}`)
  assert.equal(endKeys.length, 1, `route must initially have exactly one END relation: ${endKeys}`)

  const firstId = processIdFromBoundaryKey(startKeys[0]).routeProcessId
  const firstEdge = topology.ordinary.find((key) => splitOrdinaryKey(key)[0] === firstId)
  assert.ok(firstEdge, `START target ${firstId} must have an ordinary successor`)
  const [, secondId] = splitOrdinaryKey(firstEdge)
  const secondEdge = topology.ordinary.find((key) => splitOrdinaryKey(key)[0] === secondId)
  assert.ok(secondEdge, `route ${config.routeCode} needs a three-process chain for merge verification`)
  const [, mergeId] = splitOrdinaryKey(secondEdge)
  assert.notEqual(firstId, mergeId)
  return {
    firstId,
    secondId,
    mergeId,
    terminalId: processIdFromBoundaryKey(endKeys[0]).routeProcessId
  }
}

function ordinaryRelation(editor, key) {
  return editor.locator(`[data-flow-action="select-edge-list"][data-edge-key="${key}"]`).first()
}

function boundaryRelation(editor, key) {
  return editor
    .locator(`[data-flow-action="select-boundary-edge-list"][data-edge-key="${key}"]`)
    .first()
}

async function waitRelation(editor, key, present) {
  const locator = key.includes('process-') ? boundaryRelation(editor, key) : ordinaryRelation(editor, key)
  await locator.waitFor({
    state: present ? 'visible' : 'detached',
    timeout: 15000
  })
}

function processHandle(editor, routeProcessId, type) {
  return editor
    .locator(
      `[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"] [data-flow-handle="${type}"]`
    )
    .first()
}

function boundaryHandle(editor, boundaryType, type) {
  return editor
    .locator(
      `[data-flow-node="route-boundary"][data-flow-boundary="${boundaryType}"] [data-flow-handle="${type}"]`
    )
    .first()
}

async function dragConnection(page, source, target) {
  const sourceBox = await source.boundingBox()
  const targetBox = await target.boundingBox()
  assert.ok(sourceBox, 'source handle must be visible')
  assert.ok(targetBox, 'target handle must be visible')
  const sourcePoint = {
    x: sourceBox.x + sourceBox.width / 2,
    y: sourceBox.y + sourceBox.height / 2
  }
  const targetPoint = {
    x: targetBox.x + targetBox.width / 2,
    y: targetBox.y + targetBox.height / 2
  }
  await page.mouse.move(sourcePoint.x, sourcePoint.y)
  await page.mouse.down()
  await page.mouse.move(sourcePoint.x + 80, sourcePoint.y, { steps: 8 })
  await page.mouse.move((sourcePoint.x + targetPoint.x) / 2, (sourcePoint.y + targetPoint.y) / 2, {
    steps: 12
  })
  await page.mouse.move(targetPoint.x - 60, targetPoint.y, { steps: 8 })
  await page.mouse.move(targetPoint.x, targetPoint.y, { steps: 8 })
  await page.mouse.up()
  await page.waitForTimeout(700)
}

async function addOrdinaryRelation(page, editor, sourceId, targetId) {
  const key = `${sourceId}->${targetId}`
  const connectButton = editor.locator('[data-flow-action="connect-route-process"]')
  if ((await page.locator('[data-flow-panel="connection-selector"]:visible').count()) === 0) {
    await connectButton.click()
  }
  const panel = page.locator('[data-flow-panel="connection-selector"]:visible').first()
  await panel.waitFor({ state: 'visible', timeout: 10000 })
  await selectConnectionOption(panel, 'connection-source', sourceId)
  await selectConnectionOption(panel, 'connection-target', targetId)
  await panel.locator('[data-flow-action="confirm-route-process-connection"]').click()
  await waitRelation(editor, key, true)
}

async function addBoundaryRelation(page, editor, boundaryType, routeProcessId) {
  const key =
    boundaryType === 'START'
      ? `process-start->${routeProcessId}`
      : `${routeProcessId}->process-end`
  const source =
    boundaryType === 'START'
      ? boundaryHandle(editor, 'START', 'source')
      : processHandle(editor, routeProcessId, 'source')
  const target =
    boundaryType === 'START'
      ? processHandle(editor, routeProcessId, 'target')
      : boundaryHandle(editor, 'END', 'target')
  await dragConnection(page, source, target)
  await waitRelation(editor, key, true)
}

async function selectConnectionOption(panel, field, optionId) {
  const select = panel.locator(`[data-flow-field="${field}"]`)
  const input = select.locator('input[role="combobox"], input.el-select__input').first()
  await input.click()
  const option = select
    .locator(`.el-select-dropdown__item[data-route-process-option-id="${optionId}"]:visible`)
    .first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
}

async function addStartBoundaryRelationViaSelector(page, editor, routeProcessId) {
  const key = `process-start->${routeProcessId}`
  const connectButton = editor.locator('[data-flow-action="connect-route-process"]')
  if ((await page.locator('[data-flow-panel="connection-selector"]:visible').count()) === 0) {
    await connectButton.click()
  }
  const panel = page.locator('[data-flow-panel="connection-selector"]:visible').first()
  await panel.waitFor({ state: 'visible', timeout: 10000 })
  await selectConnectionOption(panel, 'connection-source', 'process-start')
  await selectConnectionOption(panel, 'connection-target', routeProcessId)
  await panel.locator('[data-flow-action="confirm-route-process-connection"]').click()
  await waitRelation(editor, key, true)
}

async function deleteOrdinaryRelation(editor, key) {
  await editor
    .locator(`[data-flow-action="delete-edge-list"][data-edge-key="${key}"]`)
    .first()
    .click()
  await waitRelation(editor, key, false)
}

async function deleteBoundaryRelation(editor, key) {
  await editor
    .locator(`[data-flow-action="delete-boundary-edge-list"][data-edge-key="${key}"]`)
    .first()
    .click()
  await waitRelation(editor, key, false)
}

async function expectRejectedConnection(page, editor, source, target, messagePattern) {
  const before = await relationKeys(editor)
  await dragConnection(page, source, target)
  const toast = page.locator('.el-message:visible').filter({ hasText: messagePattern }).last()
  await toast.waitFor({ state: 'visible', timeout: 10000 })
  const after = await relationKeys(editor)
  assert.deepEqual(after, before, `rejected connection must not alter relations: ${messagePattern}`)
}

async function selectBoundaryNode(editor, boundaryType) {
  const node = editor.locator(
    `[data-flow-node="route-boundary"][data-flow-boundary="${boundaryType}"]`
  )
  await node.click()
  await assert.doesNotReject(() => node.waitFor({ state: 'visible' }))
  await editor.locator('[data-flow-panel="selected-boundary-detail"]').waitFor({
    state: 'visible',
    timeout: 10000
  })
  await assert.doesNotReject(async () => {
    assert.equal(await node.evaluate((element) => element.classList.contains('is-selected')), true)
    assert.equal(await editor.locator('[data-flow-action="toggle-key-process"]').count(), 0)
    assert.equal(await editor.locator('[data-flow-action="delete-route-process"]').count(), 0)
  })
}

async function selectCanvasBoundaryEdge(page, editor, key) {
  const edge = editor.locator(`.vue-flow__edge[data-id="${key}"]`).first()
  await edge.waitFor({ state: 'attached', timeout: 10000 })
  const box = await edge.boundingBox()
  assert.ok(box, `boundary edge must be rendered: ${key}`)
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
  await editor
    .locator(`[data-flow-action="select-boundary-edge-list"][data-edge-key="${key}"].is-selected`)
    .waitFor({ state: 'visible', timeout: 10000 })
  await editor.locator('[data-flow-action="delete-selected-edge"]').waitFor({
    state: 'visible',
    timeout: 10000
  })
}

async function nodeBounds(editor, selector) {
  const node = editor.locator(selector).first()
  await node.waitFor({ state: 'visible', timeout: 10000 })
  const box = await node.boundingBox()
  assert.ok(box, `node bounds missing for selector: ${selector}`)
  return box
}

async function assertStartBoundaryTreeLayout(editor, chain) {
  const startBox = await nodeBounds(
    editor,
    '[data-flow-node="route-boundary"][data-flow-boundary="START"]'
  )
  const firstBox = await nodeBounds(
    editor,
    `[data-flow-node="route-process"][data-route-process-id="${chain.firstId}"]`
  )
  const secondBox = await nodeBounds(
    editor,
    `[data-flow-node="route-process"][data-route-process-id="${chain.secondId}"]`
  )
  const mergeBox = await nodeBounds(
    editor,
    `[data-flow-node="route-process"][data-route-process-id="${chain.mergeId}"]`
  )
  assert.ok(
    startBox.x < Math.min(firstBox.x, secondBox.x),
    `START boundary must stay before both roots: start=${startBox.x}, first=${firstBox.x}, second=${secondBox.x}`
  )
  assert.ok(
    Math.abs(firstBox.y - secondBox.y) > 20,
    `START targets must fan out vertically: firstY=${firstBox.y}, secondY=${secondBox.y}`
  )
  assert.ok(
    mergeBox.x > Math.max(firstBox.x, secondBox.x),
    `merge target must be laid out after START roots: merge=${mergeBox.x}, first=${firstBox.x}, second=${secondBox.x}`
  )
}

async function saveGraph(page, editor) {
  const routeUpdateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route/update') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-process-flow/save') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await editor.locator('[data-flow-action="save-route-flow"]').click()
  const routeUpdateResponse = await routeUpdateResponsePromise
  const routeUpdatePayload = await routeUpdateResponse.json()
  assert.ok(
    routeUpdateResponse.ok() && [0, 200].includes(routeUpdatePayload.code),
    `route update failed: HTTP ${routeUpdateResponse.status()} ${JSON.stringify(routeUpdatePayload)}`
  )
  const saveResponse = await saveResponsePromise
  const payload = await saveResponse.json()
  assert.ok(
    saveResponse.ok() && [0, 200].includes(payload.code),
    `graph save failed: HTTP ${saveResponse.status()} ${JSON.stringify(payload)}`
  )
  await page.getByText('保存成功', { exact: false }).last().waitFor({
    state: 'visible',
    timeout: 30000
  })
  await page.waitForTimeout(500)
}

async function refreshGraph(page, editor) {
  const graphResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-process-flow/get') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await editor.getByRole('button', { name: '刷新' }).click()
  const response = await graphResponsePromise
  const payload = await response.json()
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `graph refresh failed: HTTP ${response.status()} ${JSON.stringify(payload)}`
  )
  await settle(page)
}

async function restoreTopology(page, editor, initialTopology) {
  const current = await relationKeys(editor)
  for (const key of current.boundary.filter((item) => !initialTopology.boundary.includes(item))) {
    await deleteBoundaryRelation(editor, key)
  }
  for (const key of current.ordinary.filter((item) => !initialTopology.ordinary.includes(item))) {
    await deleteOrdinaryRelation(editor, key)
  }

  const afterDelete = await relationKeys(editor)
  for (const key of initialTopology.ordinary.filter((item) => !afterDelete.ordinary.includes(item))) {
    const [sourceId, targetId] = splitOrdinaryKey(key)
    await addOrdinaryRelation(page, editor, sourceId, targetId)
  }

  const afterOrdinary = await relationKeys(editor)
  for (const key of initialTopology.boundary.filter((item) => !afterOrdinary.boundary.includes(item))) {
    const boundary = processIdFromBoundaryKey(key)
    await addBoundaryRelation(page, editor, boundary.boundaryType, boundary.routeProcessId)
  }
  assert.deepEqual(await relationKeys(editor), initialTopology)
}

function graphTopology(graph) {
  return {
    ordinary: graph.edges
      .map((edge) => `${Number(edge.sourceRouteProcessId)}->${Number(edge.targetRouteProcessId)}`)
      .sort(),
    boundary: graph.boundaryEdges
      .map((edge) =>
        edge.boundaryType === 'START'
          ? `process-start->${Number(edge.routeProcessId)}`
          : `${Number(edge.routeProcessId)}->process-end`
      )
      .sort()
  }
}

async function main() {
  assertWriteTarget()
  fs.mkdirSync(config.artifactDir, { recursive: true })

  const browser = await chromium.launch({
    headless: true,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })

  let page
  let editor
  let initialTopology
  let savedChangedTopology = false
  let restored = false
  try {
    const context = await browser.newContext({ viewport: { width: 1680, height: 960 } })
    page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)
    editor = await openRouteGraph(page)
    const routeId = routeIdFromUrl(page)
    const tenantId = (await authHeaders(page))['tenant-id']
    initialTopology = await relationKeys(editor)
    const chain = findThreeNodeChain(initialTopology)

    process.stdout.write(
      `E2E target: tenant-id=${tenantId}, account=${config.username}, routeId=${routeId}, routeCode=${config.routeCode}\n`
    )

    await selectBoundaryNode(editor, 'START')
    await selectBoundaryNode(editor, 'END')
    await selectCanvasBoundaryEdge(page, editor, `process-start->${chain.firstId}`)

    await deleteOrdinaryRelation(editor, `${chain.firstId}->${chain.secondId}`)
    await addStartBoundaryRelationViaSelector(page, editor, chain.secondId)
    await addOrdinaryRelation(page, editor, chain.firstId, chain.mergeId)
    await assertStartBoundaryTreeLayout(editor, chain)

    const changedTopology = await relationKeys(editor)
    assert.equal(
      changedTopology.boundary.filter((key) => key.startsWith('process-start->')).length,
      2
    )
    assert.ok(changedTopology.boundary.includes(`process-start->${chain.firstId}`))
    assert.ok(changedTopology.boundary.includes(`process-start->${chain.secondId}`))
    assert.ok(changedTopology.ordinary.includes(`${chain.firstId}->${chain.mergeId}`))
    assert.ok(changedTopology.ordinary.includes(`${chain.secondId}->${chain.mergeId}`))
    assert.ok(!changedTopology.ordinary.includes(`${chain.firstId}->${chain.secondId}`))

    await saveGraph(page, editor)
    savedChangedTopology = true
    await refreshGraph(page, editor)
    assert.deepEqual(await relationKeys(editor), changedTopology)

    const persistedChangedGraph = await apiGet(
      page,
      `/mes/pro/route-process-flow/get?routeId=${routeId}`
    )
    assert.equal(persistedChangedGraph.validationStatus, 'VALID')
    assert.deepEqual(graphTopology(persistedChangedGraph), changedTopology)

    await page.screenshot({
      path: path.join(config.artifactDir, 'boundary-multi-start-persisted.png'),
      fullPage: true
    })

    editor = await openRouteGraph(page)
    assert.equal(routeIdFromUrl(page), routeId, 'reopened route must keep the same route id')
    await restoreTopology(page, editor, initialTopology)
    await saveGraph(page, editor)
    await refreshGraph(page, editor)
    const restoredGraph = await apiGet(page, `/mes/pro/route-process-flow/get?routeId=${routeId}`)
    assert.equal(restoredGraph.validationStatus, 'VALID')
    assert.deepEqual(graphTopology(restoredGraph), initialTopology)
    restored = true

    const result = {
      ok: true,
      baseUrl: config.baseUrl,
      tenantId,
      account: config.username,
      routeId,
      routeCode: config.routeCode,
      chain,
      initialTopology,
      changedTopology,
      restoredTopology: graphTopology(restoredGraph)
    }
    writeArtifact('result.json', result)
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`)
  } catch (error) {
    writeArtifact('failure.json', {
      message: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined,
      savedChangedTopology,
      restored
    })
    if (page) {
      await page
        .screenshot({
          path: path.join(config.artifactDir, 'failure.png'),
          fullPage: true
        })
        .catch(() => null)
    }
    throw error
  } finally {
    if (savedChangedTopology && !restored && page && editor && initialTopology) {
      editor = await openRouteGraph(page)
      await restoreTopology(page, editor, initialTopology)
      await saveGraph(page, editor)
    }
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
