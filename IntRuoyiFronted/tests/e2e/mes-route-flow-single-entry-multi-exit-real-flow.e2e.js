const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_E2E_BASE_URL || 'http://127.0.0.1:18091').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_ROUTE_FLOW_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_E2E_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FLOW_E2E_ROUTE_CODE || 'RT000017',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_E2E_ARTIFACT_DIR ||
      path.join(__dirname, '..', '..', '..', 'runtime', 'route-flow-single-entry-e2e')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalWriter() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', `write E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `write E2E must use aoteman, got ${config.username}`)
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
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
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

  const accountInput = form
    .locator('input.el-input__inner:not([role="combobox"]):visible')
    .first()
  await accountInput.fill('')
  await accountInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const response = await responsePromise
  const payload = await response.json()
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `login failed: HTTP ${response.status()} ${JSON.stringify(payload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })

  const identity = await page.evaluate(() => ({
    tenantId: localStorage.getItem('tenantId'),
    username: localStorage.getItem('username')
  }))
  assert.match(identity.tenantId || '', /122/, `unexpected tenant identity: ${JSON.stringify(identity)}`)
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

  await page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
    .fill(config.routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)

  const row = page.locator('tr.el-table__row').filter({ hasText: config.routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit/'), {
    timeout: 60000
  })

  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.getByRole('tab', { name: '流转关系图' }).click()
  await editor.locator('[data-flow-node="route-process"]').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page)
  return editor
}

async function orderedNodeIds(editor) {
  const ids = await editor.locator('[data-flow-node="route-process"]').evaluateAll((nodes) =>
    nodes.map((node) => Number(node.getAttribute('data-route-process-id')))
  )
  assert.ok(ids.length >= 4, `route requires at least four process nodes, got ${ids.length}`)
  return ids
}

function edgeKey(sourceId, targetId) {
  return `${sourceId}->${targetId}`
}

async function visibleEdgeKeys(editor) {
  return editor.locator('.vue-flow__edge').evaluateAll((edges) =>
    edges
      .map((edge) => edge.getAttribute('data-id'))
      .filter((key) => key && /^\d+->\d+$/.test(key))
      .sort()
  )
}

async function assertEdges(editor, required, forbidden = []) {
  const keys = await visibleEdgeKeys(editor)
  for (const key of required) {
    assert.ok(keys.includes(key), `required edge missing: ${key}; actual=${JSON.stringify(keys)}`)
  }
  for (const key of forbidden) {
    assert.equal(keys.includes(key), false, `forbidden edge present: ${key}; actual=${JSON.stringify(keys)}`)
  }
  return keys
}

async function connect(editor, sourceId, targetId) {
  const source = editor
    .locator(`[data-flow-handle="source"][data-route-process-id="${sourceId}"]`)
    .first()
  const target = editor
    .locator(`[data-flow-handle="target"][data-route-process-id="${targetId}"]`)
    .first()
  const sourceBox = await source.boundingBox()
  const targetBox = await target.boundingBox()
  assert.ok(sourceBox, `source handle missing: ${sourceId}`)
  assert.ok(targetBox, `target handle missing: ${targetId}`)

  const page = editor.page()
  await page.mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2)
  await page.mouse.down()
  await page.mouse.move(
    (sourceBox.x + targetBox.x) / 2,
    (sourceBox.y + targetBox.y) / 2,
    { steps: 18 }
  )
  await page.mouse.move(targetBox.x + targetBox.width / 2, targetBox.y + targetBox.height / 2, {
    steps: 12
  })
  await page.mouse.up()
  await page.waitForTimeout(800)
}

async function deleteOrdinaryEdge(editor, key) {
  const deleteButton = editor
    .locator(`[data-flow-action="delete-edge-list"][data-edge-key="${key}"]`)
    .first()
  await deleteButton.waitFor({ state: 'visible', timeout: 60000 })
  await deleteButton.click()
  await editor.page().waitForTimeout(800)
}

async function selectRouteProcess(page, panel, field, routeProcessId) {
  const select = panel.locator(`[data-flow-field="${field}"]`)
  const input = select.locator('input[role="combobox"], input.el-select__input').first()
  await input.click()
  const option = select
    .locator(`.el-select-dropdown__item[data-route-process-option-id="${routeProcessId}"]:visible`)
    .first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
  await panel.waitFor({ state: 'visible', timeout: 10000 })
}

async function connectViaSelector(page, editor, sourceRouteProcessId, targetRouteProcessId) {
  const connectButton = editor.locator('[data-flow-action="connect-route-process"]')
  const visiblePanel = page.locator('[data-flow-panel="connection-selector"]:visible')
  if ((await visiblePanel.count()) === 0) {
    await connectButton.click()
  }
  const panel = page.locator('[data-flow-panel="connection-selector"]:visible')
  await panel.waitFor({ state: 'visible', timeout: 10000 })
  await selectRouteProcess(page, panel, 'connection-source', sourceRouteProcessId)
  await selectRouteProcess(page, panel, 'connection-target', targetRouteProcessId)
  await panel.locator('[data-flow-action="confirm-route-process-connection"]').click()
  await page.waitForTimeout(800)
}

async function saveGraph(page, editor) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-process-flow/save') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await editor.locator('[data-flow-action="save-route-flow"]').click()
  const response = await responsePromise
  const payload = await response.json()
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `graph save failed: HTTP ${response.status()} ${JSON.stringify(payload)}`
  )
  await page.getByText('保存成功', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  return payload.data
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

async function finalGraphFromApi(page, routeId) {
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
  assert.ok(accessToken, 'ACCESS_TOKEN missing for final verification')
  assert.equal(String(tenantId), '122', `final verification tenant must be 122, got ${tenantId}`)

  const response = await page.request.get(
    `${config.baseUrl}/admin-api/mes/pro/route-process-flow/get?routeId=${routeId}`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId)
      }
    }
  )
  const payload = await response.json()
  assert.equal(payload.code, 0, `final graph query failed: ${JSON.stringify(payload)}`)
  return payload.data
}

async function main() {
  assertLocalWriter()
  fs.mkdirSync(config.artifactDir, { recursive: true })

  const browser = await chromium.launch({
    headless: true,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  let mutationSaved = false
  let ids
  let page
  try {
    const context = await browser.newContext({ viewport: { width: 1600, height: 900 } })
    page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)
    let editor = await openRouteGraph(page)
    ids = await orderedNodeIds(editor)
    const [processA, processB, processC, processD] = ids
    const initialRequired = [
      edgeKey(processA, processB),
      edgeKey(processB, processC),
      edgeKey(processC, processD)
    ]
    await assertEdges(editor, initialRequired)

    await connectViaSelector(page, editor, processA, processC)
    const branchedRequired = [
      edgeKey(processA, processB),
      edgeKey(processA, processC),
      edgeKey(processB, processC),
      edgeKey(processC, processD)
    ]
    const mergeEdge = edgeKey(processB, processC)
    const addedMergeEdge = edgeKey(processA, processC)
    const draftKeys = await assertEdges(editor, branchedRequired)
    await saveGraph(page, editor)
    mutationSaved = true

    editor = await openRouteGraph(page)
    const persistedBranchKeys = await assertEdges(editor, branchedRequired)
    await page.screenshot({
      path: path.join(config.artifactDir, 'single-entry-multi-exit-persisted.png'),
      fullPage: true
    })

    await deleteOrdinaryEdge(editor, addedMergeEdge)
    const restoredDraftKeys = await assertEdges(editor, initialRequired, [
      addedMergeEdge
    ])
    await saveGraph(page, editor)
    mutationSaved = false

    editor = await openRouteGraph(page)
    const restoredPersistedKeys = await assertEdges(editor, initialRequired, [
      addedMergeEdge
    ])
    const routeId = Number(new URL(page.url()).pathname.split('/').filter(Boolean).at(-1))
    assert.ok(Number.isFinite(routeId), `route id missing from URL: ${page.url()}`)
    const finalGraph = await finalGraphFromApi(page, routeId)
    const finalKeys = finalGraph.edges
      .map((edge) => edgeKey(edge.sourceRouteProcessId, edge.targetRouteProcessId))
      .sort()
    assert.deepEqual(
      finalKeys,
      restoredPersistedKeys,
      `final API graph differs from restored UI graph: ${JSON.stringify(finalGraph)}`
    )

    const evidence = {
      ok: true,
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      tenantId: 122,
      username: config.username,
      routeCode: config.routeCode,
      routeId,
      processA,
      processB,
      processC,
      processD,
      mergeEdge,
      draftKeys,
      persistedBranchKeys,
      restoredDraftKeys,
      restoredPersistedKeys,
      finalGraphVersion: finalGraph.graphVersion,
      finalValidationStatus: finalGraph.validationStatus
    }
    fs.writeFileSync(
      path.join(config.artifactDir, 'result.json'),
      `${JSON.stringify(evidence, null, 2)}\n`,
      'utf8'
    )
    console.log(
      `PASS: single-entry multi-exit route=${config.routeCode} ` +
        `branch=${processA}->${processB},${processA}->${processC},${processB}->${processC},${processC}->${processD}`
    )
  } catch (error) {
    if (mutationSaved && page && ids?.length >= 3) {
      try {
        const editor = await openRouteGraph(page)
        await deleteOrdinaryEdge(editor, edgeKey(ids[0], ids[2]))
        await saveGraph(page, editor)
        mutationSaved = false
      } catch (restoreError) {
        console.error('RESTORE_FAILED', restoreError)
      }
    }
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
