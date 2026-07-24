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
      path.join(__dirname, '..', '..', 'tests', 'output', 'route-flow-delete-add-real')
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
  await tenantInput.click()
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  const accountInput = form.locator('input.el-input__inner:not([role="combobox"])').first()
  await accountInput.fill('')
  await accountInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
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

function graphSnapshot(graph) {
  return {
    nodeIds: graph.nodes.map((node) => Number(node.routeProcessId)).sort((a, b) => a - b),
    processIds: graph.nodes.map((node) => Number(node.processId)).sort((a, b) => a - b),
    edgeKeys: graph.edges
      .map((edge) => `${edge.sourceRouteProcessId}->${edge.targetRouteProcessId}`)
      .sort()
  }
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

async function visibleRouteProcessIds(editor) {
  return editor.locator('[data-flow-node="route-process"]').evaluateAll((nodes) =>
    nodes.map((node) => Number(node.getAttribute('data-route-process-id')))
  )
}

async function clickConfirm(page) {
  const confirmButton = page.locator('.el-message-box:visible').getByRole('button', { name: /确认|确定/ }).last()
  await confirmButton.waitFor({ state: 'visible', timeout: 10000 })
  await confirmButton.click()
}

async function deleteNodeWithKeyboard(editor, page, routeProcessId) {
  const nodeButton = editor.locator(`[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"]`).first()
  await nodeButton.waitFor({ state: 'visible', timeout: 10000 })
  await nodeButton.evaluate((element) => {
    if (!(element instanceof HTMLElement)) {
      throw new Error('route process node is not focusable')
    }
    element.focus()
    element.dispatchEvent(new MouseEvent('click', { bubbles: true, composed: true }))
  })
  await page.waitForFunction(
    (id) => document.activeElement?.getAttribute('data-route-process-id') === String(id),
    routeProcessId,
    { timeout: 10000 }
  )
  await page.keyboard.press('Delete')
  await page
    .locator('.el-message-box:visible')
    .filter({ hasText: '确认从当前工艺路线删除工序' })
    .last()
    .waitFor({ state: 'visible', timeout: 10000 })
  await clickConfirm(page)
  await page.locator('.el-message-box:visible').waitFor({ state: 'hidden', timeout: 10000 })
  await editor
    .locator(`[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"]`)
    .waitFor({ state: 'detached', timeout: 10000 })
}

async function selectProcessOption(page, addDialog, process) {
  const label = [process.code, process.name].filter(Boolean).join(' / ') || String(process.id)
  const select = addDialog.locator('[data-flow-action="select-route-process"]').first()
  await select.waitFor({ state: 'visible', timeout: 10000 })
  await select.click()
  const input = select.locator('input').first()
  await input.fill(process.code || process.name || String(process.id))
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await dropdown.waitFor({ state: 'visible', timeout: 10000 })
  const option = dropdown.locator('.el-select-dropdown__item:not(.is-disabled)').filter({ hasText: label }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
}

async function main() {
  assertLocalOnly(config.baseUrl)
  const isTestTenantWriter = config.tenant === '测试租户' && config.username === 'aoteman'
  const isAdminReadonly =
    process.env.MES_ROUTE_FLOW_E2E_ALLOW_ADMIN_READONLY === '1' &&
    config.tenant === '芋道源码' &&
    config.username === 'admin'
  assert.ok(
    isTestTenantWriter || isAdminReadonly,
    `real E2E must use 测试租户/aoteman or explicit readonly 芋道源码/admin, got ${config.tenant}/${config.username}`
  )
  ensureArtifactDir()

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  try {
    const context = await browser.newContext({ viewport: { width: 1366, height: 768 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    const writeRequests = []
    for (const pattern of [
      '**/admin-api/mes/pro/route-process/create**',
      '**/admin-api/mes/pro/route-process/delete**',
      '**/admin-api/mes/pro/route-process-flow/save**'
    ]) {
      await page.route(pattern, async (route) => {
        const request = route.request()
        writeRequests.push({ method: request.method(), url: request.url(), blocked: true })
        await route.abort('blockedbyclient')
      })
    }
    page.on('request', (request) => {
      const url = request.url()
      const method = request.method()
      if (
        url.includes('/mes/pro/route-process/create') ||
        url.includes('/mes/pro/route-process/delete') ||
        (url.includes('/mes/pro/route-process-flow/save') && method === 'POST')
      ) {
        writeRequests.push({ method, url, observed: true })
      }
    })

    await login(page)
    const route = await findRoute(page)
    const initialGraph = await apiGet(page, `/mes/pro/route-process-flow/get?routeId=${route.id}`)
    assert.ok(initialGraph.nodes.length >= 3, `route needs at least 3 nodes: ${initialGraph.nodes.length}`)
    const initialSnapshot = graphSnapshot(initialGraph)
    const usedProcessIds = new Set(initialGraph.nodes.map((node) => Number(node.processId)))
    const processOptions = await apiGet(page, '/mes/pro/process/simple-list')
    const addCandidate = processOptions.find((process) => process.id && !usedProcessIds.has(Number(process.id)))
    assert.ok(addCandidate, 'no unused real process is available for route process add E2E')

    const editor = await openRouteGraph(page)
    const beforeIds = await visibleRouteProcessIds(editor)
    assert.deepEqual(
      [...beforeIds].sort((a, b) => a - b),
      initialSnapshot.nodeIds,
      `initial visible nodes mismatch: ${JSON.stringify(beforeIds)}`
    )
    const deleteIds = beforeIds.slice(1, 4)
    assert.equal(deleteIds.length, 3, `route needs three deletable nodes: ${JSON.stringify(beforeIds)}`)
    const deletedNodeTexts = []
    for (const deleteId of deleteIds) {
      const deleteNode = editor.locator(`[data-flow-node="route-process"][data-route-process-id="${deleteId}"]`).first()
      deletedNodeTexts.push(await deleteNode.innerText())
      await deleteNodeWithKeyboard(editor, page, deleteId)
    }
    const afterDeleteIds = await visibleRouteProcessIds(editor)
    for (const deleteId of deleteIds) {
      assert.equal(afterDeleteIds.includes(deleteId), false, 'deleted route process is still visible after draft delete')
    }

    await editor.locator('[data-flow-action="add-route-process"]').click()
    const afterOpenAddDialogIds = await visibleRouteProcessIds(editor)
    for (const deleteId of deleteIds) {
      assert.equal(
        afterOpenAddDialogIds.includes(deleteId),
        false,
        'deleted route process was restored when opening add process dialog'
      )
    }
    const addDialog = page.locator('.el-dialog:visible').filter({ hasText: '添加工序' }).last()
    await addDialog.waitFor({ state: 'visible', timeout: 10000 })
    await selectProcessOption(page, addDialog, addCandidate)
    await addDialog.locator('[data-flow-action="submit-add-route-process"]').click()
    await addDialog.waitFor({ state: 'hidden', timeout: 10000 })

    const afterAddIds = await visibleRouteProcessIds(editor)
    for (const deleteId of deleteIds) {
      assert.equal(afterAddIds.includes(deleteId), false, 'deleted route process was restored after adding another process')
    }
    assert.ok(afterAddIds.some((id) => id < 0), `draft added route process missing: ${JSON.stringify(afterAddIds)}`)
    assert.equal(
      afterAddIds.length,
      beforeIds.length - deleteIds.length + 1,
      `draft delete three and add one should reduce visible count by two: before=${beforeIds.length}, after=${afterAddIds.length}`
    )

    const liveAfterDraftChanges = await apiGet(page, `/mes/pro/route-process-flow/get?routeId=${route.id}`)
    assert.deepEqual(
      graphSnapshot(liveAfterDraftChanges),
      initialSnapshot,
      'backend graph changed before clicking route save'
    )
    assert.deepEqual(writeRequests, [], `draft delete/add must not send persistence requests: ${JSON.stringify(writeRequests)}`)

    const evidence = {
      ok: true,
      routeId: route.id,
      routeCode: route.code,
      tenant: config.tenant,
      username: config.username,
      deleteIds,
      deletedNodeTexts,
      addCandidate: {
        id: addCandidate.id,
        code: addCandidate.code,
        name: addCandidate.name
      },
      initialSnapshot,
      beforeIds,
      afterDeleteIds,
      afterOpenAddDialogIds,
      afterAddIds,
      writeRequests
    }
    writeArtifact('route-flow-delete-add-real-e2e-result.json', evidence)
    await page.screenshot({
      path: path.join(config.artifactDir, 'route-flow-delete-add-real-e2e-after-add.png'),
      fullPage: true
    })
    console.log(
      `PASS: route flow delete-add real E2E route=${route.code} routeId=${route.id} deleted=${deleteIds.join(',')} addedProcess=${addCandidate.id}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
