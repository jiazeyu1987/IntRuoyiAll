const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_E2E_BASE_URL || 'http://127.0.0.1:18093').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_ROUTE_FLOW_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_E2E_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FLOW_E2E_ROUTE_CODE || 'RT000017',
  labelOnly: process.env.MES_ROUTE_FLOW_E2E_LABEL_ONLY === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_E2E_ARTIFACT_DIR ||
      path.join(__dirname, '..', '..', 'runtime', 'route-flow-connection-discoverability')
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
  assert.equal(config.tenant, '测试租户')
  assert.equal(config.username, 'aoteman')
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
  const tenantSelect = form.locator('.el-select').first()
  const tenantInput = tenantSelect.locator('input[role="combobox"], input.el-select__input').first()
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
  const selectedTenantTexts = await tenantSelect
    .locator('.el-select__selected-item')
    .allInnerTexts()
  assert.equal(
    selectedTenantTexts.some((value) => value.includes(config.tenant)),
    true,
    `tenant selection failed: ${JSON.stringify(selectedTenantTexts)}`
  )
  await form
    .locator('input.el-input__inner:not([role="combobox"]):visible')
    .first()
    .fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const loginButton = form.getByRole('button', { name: /^登录$/ })
  await loginButton.click()
  let response
  try {
    response = await responsePromise
  } catch (error) {
    await page.screenshot({
      path: path.join(config.artifactDir, 'login-submit-failed.png'),
      fullPage: true
    })
    const inputState = await form.locator('input').evaluateAll((inputs) =>
      inputs.map((input) => ({
        type: input.getAttribute('type'),
        placeholder: input.getAttribute('placeholder'),
        role: input.getAttribute('role'),
        value: input.value
      }))
    )
    const validationErrors = await form.locator('.el-form-item__error').allInnerTexts()
    throw new Error(
      `login request was not sent: buttonDisabled=${await loginButton.isDisabled()} ` +
        `inputs=${JSON.stringify(inputState)} errors=${JSON.stringify(validationErrors)}; ${error.message}`
    )
  }
  const payload = await response.json()
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `login failed: HTTP ${response.status()} ${JSON.stringify(payload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
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
  await page
    .getByRole('button', { name: /查询|搜索/ })
    .first()
    .click()
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
  const ids = await editor
    .locator('[data-flow-node="route-process"]')
    .evaluateAll((nodes) => nodes.map((node) => Number(node.getAttribute('data-route-process-id'))))
  assert.ok(ids.length >= 4, `route requires at least four process nodes, got ${ids.length}`)
  return ids
}

async function visibleEdgeKeys(editor) {
  return editor.locator('.vue-flow__edge').evaluateAll((edges) =>
    edges
      .map((edge) => edge.getAttribute('data-id'))
      .filter((key) => key && /^\d+->\d+$/.test(key))
      .sort()
  )
}

async function waitForEdgeState(editor, required, forbidden) {
  await assert.doesNotReject(async () => {
    await editor.page().waitForFunction(
      ({ requiredKeys, forbiddenKeys }) => {
        const keys = Array.from(document.querySelectorAll('.vue-flow__edge'))
          .map((edge) => edge.getAttribute('data-id'))
          .filter(Boolean)
        return (
          requiredKeys.every((key) => keys.includes(key)) &&
          forbiddenKeys.every((key) => !keys.includes(key))
        )
      },
      { requiredKeys: required, forbiddenKeys: forbidden },
      { timeout: 15000 }
    )
  })
}

async function waitForLayoutRevision(editor, previousRevision) {
  await editor.page().waitForFunction(
    ({ selector, previous }) => {
      const value = Number(
        document.querySelector(selector)?.getAttribute('data-flow-layout-revision')
      )
      return value > previous
    },
    {
      selector: '.route-flow-graph-designer',
      previous: previousRevision
    },
    { timeout: 10000 }
  )
}

async function nodeBox(editor, routeProcessId) {
  const box = await editor
    .locator(`[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"]`)
    .boundingBox()
  assert.ok(box, `process node box missing: ${routeProcessId}`)
  return box
}

async function assertVisibleBranchLayout(editor, sourceId, firstTargetId, secondTargetId) {
  const [source, firstTarget, secondTarget] = await Promise.all([
    nodeBox(editor, sourceId),
    nodeBox(editor, firstTargetId),
    nodeBox(editor, secondTargetId)
  ])
  const centerY = (box) => box.y + box.height / 2
  const firstCenterY = centerY(firstTarget)
  const secondCenterY = centerY(secondTarget)
  const sourceCenterY = centerY(source)

  assert.ok(
    source.width >= 90,
    `changed branch must stay readable after layout: ${JSON.stringify({ source })}`
  )
  assert.ok(
    firstTarget.x > source.x + source.width * 0.6,
    `first branch must be placed after the source: ${JSON.stringify({ source, firstTarget })}`
  )
  assert.ok(
    secondTarget.x > source.x + source.width * 0.6,
    `second branch must be placed after the source: ${JSON.stringify({ source, secondTarget })}`
  )
  assert.ok(
    Math.abs(firstTarget.x - secondTarget.x) <= 4,
    `direct branches must share the next relation column: ${JSON.stringify({
      firstTarget,
      secondTarget
    })}`
  )
  assert.ok(
    Math.abs(firstCenterY - secondCenterY) >= Math.min(firstTarget.height, secondTarget.height),
    `direct branches must not overlap vertically: ${JSON.stringify({
      firstTarget,
      secondTarget
    })}`
  )
  assert.ok(
    sourceCenterY >= Math.min(firstCenterY, secondCenterY) &&
      sourceCenterY <= Math.max(firstCenterY, secondCenterY),
    `source must be vertically centered between direct branches: ${JSON.stringify({
      source,
      firstTarget,
      secondTarget
    })}`
  )
}

async function selectRouteProcess(page, panel, field, routeProcessId) {
  const select = panel.locator(`[data-flow-field="${field}"]`)
  const input = select.locator('input[role="combobox"], input.el-select__input').first()
  await input.click()
  const option = select
    .locator(`.el-select-dropdown__item[data-route-process-option-id="${routeProcessId}"]:visible`)
    .first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  const expectedName = (
    await page
      .locator(
        `[data-flow-node="route-process"][data-route-process-id="${routeProcessId}"] .route-flow-graph-designer__node-name`
      )
      .innerText()
  ).trim()
  const optionText = (await option.innerText()).trim()
  assert.equal(optionText, expectedName, 'connection dropdown option should show only process name')
  await option.click()
  await panel.waitFor({ state: 'visible', timeout: 10000 })
  return optionText
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
  return panel
}

async function connectRouteProcessToEnd(editor, routeProcessId) {
  await editor.getByRole('button', { name: '自动布局', exact: true }).click()
  await editor.page().waitForTimeout(700)
  const source = editor
    .locator(`[data-flow-handle="source"][data-route-process-id="${routeProcessId}"].is-visible`)
    .first()
  const target = editor
    .locator(
      '[data-flow-node="route-boundary"][data-flow-boundary="END"] [data-flow-handle="target"]'
    )
    .first()
  const sourceBox = await source.boundingBox()
  let targetBox = await target.boundingBox()
  assert.ok(sourceBox, `source handle missing for end boundary: ${routeProcessId}`)
  assert.ok(targetBox, 'end boundary target handle missing')

  const page = editor.page()
  await page.mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2)
  await page.mouse.down()
  await page.mouse.move((sourceBox.x + targetBox.x) / 2, (sourceBox.y + targetBox.y) / 2, {
    steps: 18
  })
  let targetReached = false
  for (let index = 0; index < 8; index += 1) {
    targetBox = await target.boundingBox()
    assert.ok(targetBox, 'end boundary target handle disappeared while connecting')
    const targetX = targetBox.x + targetBox.width / 2
    const targetY = targetBox.y + targetBox.height / 2
    await page.mouse.move(targetX, targetY, { steps: 6 })
    await page.waitForTimeout(100)
    targetReached = await page.evaluate(
      ({ x, y }) =>
        document
          .elementsFromPoint(x, y)
          .some(
            (element) =>
              element.getAttribute('data-nodeid') === 'process-end' &&
              element.classList.contains('vue-flow__handle')
          ),
      { x: targetX, y: targetY }
    )
    if (targetReached) break
  }
  assert.equal(targetReached, true, 'pointer must reach the moving end boundary handle')
  await page.mouse.up()
}

async function waitForBoundaryEdge(editor, boundaryEdgeKey, present) {
  await editor.page().waitForFunction(
    ({ key, shouldExist }) => {
      const edge = document.querySelector(`.vue-flow__edge[data-id="${key}"]`)
      return shouldExist ? Boolean(edge) : !edge
    },
    { key: boundaryEdgeKey, shouldExist: present },
    { timeout: 10000 }
  )
}

async function deleteBoundaryEdgeIfPresent(editor, boundaryEdgeKey) {
  const deleteButton = editor.locator(
    `[data-flow-action="delete-boundary-edge-list"][data-edge-key="${boundaryEdgeKey}"]`
  )
  if ((await deleteButton.count()) === 0) return
  await deleteButton.click()
  await waitForBoundaryEdge(editor, boundaryEdgeKey, false)
}

async function saveGraph(page, editor) {
  let resolveSaveResponse
  const routeRequestTrace = []
  const saveResponsePromise = new Promise((resolve) => {
    resolveSaveResponse = resolve
  })
  const handleRouteRequest = (request) => {
    if (!request.url().includes('/mes/pro/route')) return
    routeRequestTrace.push({
      event: 'request',
      method: request.method(),
      url: request.url()
    })
  }
  const handleSaveResponse = async (response) => {
    if (response.url().includes('/mes/pro/route')) {
      let body
      try {
        body = await response.json()
      } catch {
        body = undefined
      }
      routeRequestTrace.push({
        body,
        event: 'response',
        method: response.request().method(),
        status: response.status(),
        url: response.url()
      })
    }
    if (
      response.url().includes('/mes/pro/route-process-flow/save') &&
      response.request().method() === 'POST'
    ) {
      resolveSaveResponse(response)
    }
  }
  const handleRouteRequestFailed = (request) => {
    if (!request.url().includes('/mes/pro/route')) return
    routeRequestTrace.push({
      event: 'requestfailed',
      failure: request.failure()?.errorText || 'unknown',
      method: request.method(),
      url: request.url()
    })
  }
  page.on('request', handleRouteRequest)
  page.on('response', handleSaveResponse)
  page.on('requestfailed', handleRouteRequestFailed)
  const validationPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route-process-flow/validate') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  try {
    await editor.locator('[data-flow-action="save-route-flow"]').click()
    const validationResponse = await validationPromise
    const validationPayload = await validationResponse.json()
    assert.ok(
      validationResponse.ok() &&
        [0, 200].includes(validationPayload.code) &&
        validationPayload.data?.valid,
      `graph validation failed: HTTP ${validationResponse.status()} ${JSON.stringify(validationPayload)}`
    )
    const response = await Promise.race([
      saveResponsePromise,
      page.waitForTimeout(60000).then(async () => {
        const visibleMessages = await page
          .locator('.el-message:visible, .el-notification:visible')
          .allInnerTexts()
        await page.screenshot({
          path: path.join(config.artifactDir, 'graph-save-timeout.png'),
          fullPage: true
        })
        throw new Error(
          `graph save response timed out: ${JSON.stringify({ routeRequestTrace, visibleMessages })}`
        )
      })
    ])
    const payload = await response.json()
    assert.ok(
      response.ok() && [0, 200].includes(payload.code),
      `graph save failed: HTTP ${response.status()} ${JSON.stringify(payload)}`
    )
    await page.getByText('保存成功', { exact: false }).first().waitFor({
      state: 'visible',
      timeout: 30000
    })
  } finally {
    page.off('request', handleRouteRequest)
    page.off('response', handleSaveResponse)
    page.off('requestfailed', handleRouteRequestFailed)
  }
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
  let processIds
  let page
  try {
    const context = await browser.newContext({ viewport: { width: 1600, height: 900 } })
    page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)
    const graphSaveRequests = []
    page.on('request', (request) => {
      if (
        request.url().includes('/mes/pro/route-process-flow/save') &&
        request.method() === 'POST'
      ) {
        graphSaveRequests.push(request.url())
      }
    })

    let editor = await openRouteGraph(page)
    const connectButton = editor.locator('[data-flow-action="connect-route-process"]')
    await connectButton.waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(
      await page.locator('[data-flow-panel="connection-selector"]:visible').count(),
      0,
      'connection selector should stay closed before the toolbar action'
    )

    processIds = await orderedNodeIds(editor)
    assert.ok(
      processIds.length > 10,
      `branch layout E2E requires a dense route with more than ten nodes, got ${processIds.length}`
    )
    const [processA, processB, processC, processD] = processIds
    const branchTerminalEdge = `${processB}->process-end`
    const processCName = (
      await editor
        .locator(
          `[data-flow-node="route-process"][data-route-process-id="${processC}"] .route-flow-graph-designer__node-name`
        )
        .innerText()
    ).trim()
    const initialRequired = [
      `${processA}->${processB}`,
      `${processB}->${processC}`,
      `${processC}->${processD}`
    ]
    if (config.labelOnly) {
      const currentEdges = await visibleEdgeKeys(editor)
      await connectButton.click()
      const labelPanel = page.locator('[data-flow-panel="connection-selector"]:visible')
      await labelPanel.waitFor({ state: 'visible', timeout: 10000 })
      const sourceLabel = await selectRouteProcess(page, labelPanel, 'connection-source', processA)
      const targetLabel = await selectRouteProcess(page, labelPanel, 'connection-target', processC)
      const evidence = {
        ok: true,
        mode: 'label-only',
        baseUrl: config.baseUrl,
        tenant: config.tenant,
        username: config.username,
        routeCode: config.routeCode,
        sourceLabel,
        targetLabel,
        currentEdges
      }
      fs.writeFileSync(
        path.join(config.artifactDir, 'label-only-result.json'),
        `${JSON.stringify(evidence, null, 2)}\n`,
        'utf8'
      )
      await page.screenshot({
        path: path.join(config.artifactDir, 'connection-options-name-only.png'),
        fullPage: true
      })
      console.log(
        `PASS: route flow selector options display process names only for ${config.routeCode}`
      )
      return
    }
    await waitForEdgeState(editor, initialRequired, [`${processA}->${processC}`])
    const initialLayoutRevision = Number(
      await editor.locator('.route-flow-graph-designer').getAttribute('data-flow-layout-revision')
    )
    await page.screenshot({
      path: path.join(config.artifactDir, 'connection-entry-visible.png'),
      fullPage: true
    })

    await connectButton.click()
    const panel = page.locator('[data-flow-panel="connection-selector"]:visible')
    await panel.waitFor({
      state: 'visible',
      timeout: 10000
    })

    await selectRouteProcess(page, panel, 'connection-source', processA)
    await selectRouteProcess(page, panel, 'connection-target', processC)
    await panel.locator('[data-flow-hint="connection-replacement"]').waitFor({
      state: 'visible',
      timeout: 10000
    })
    await panel.getByText(/目标工序当前入口.*确认后将替换/).waitFor({
      state: 'visible',
      timeout: 10000
    })
    await panel.locator('[data-flow-action="confirm-route-process-connection"]').click()
    const branchedRequired = [
      `${processA}->${processB}`,
      `${processA}->${processC}`,
      `${processC}->${processD}`
    ]
    await waitForEdgeState(editor, branchedRequired, [`${processB}->${processC}`])
    const unsavedStatus = editor.locator('[data-flow-status="unsaved"]')
    await unsavedStatus.waitFor({
      state: 'visible',
      timeout: 10000
    })
    await page.waitForFunction(
      () =>
        (document.querySelector('[data-flow-status="unsaved"]')?.getBoundingClientRect().width ||
          0) >= 60,
      undefined,
      { timeout: 10000 }
    )
    const unsavedStatusBox = await unsavedStatus.boundingBox()
    const unsavedStatusStyles = await unsavedStatus.evaluate((element) => {
      const style = window.getComputedStyle(element)
      return {
        display: style.display,
        flex: style.flex,
        flexShrink: style.flexShrink,
        minWidth: style.minWidth,
        width: style.width,
        whiteSpace: style.whiteSpace
      }
    })
    assert.ok(
      unsavedStatusBox && unsavedStatusBox.width >= 60,
      `unsaved status must remain readable: box=${JSON.stringify(unsavedStatusBox)} styles=${JSON.stringify(unsavedStatusStyles)}`
    )
    assert.equal((await unsavedStatus.innerText()).trim(), '未保存')
    const sourceSelectionTexts = await panel
      .locator('[data-flow-field="connection-source"] .el-select__selected-item')
      .allInnerTexts()
    assert.equal(
      sourceSelectionTexts.some((value) => value.trim() && !value.includes('/')),
      true,
      'source process should stay selected with a name-only label'
    )
    const targetSelectionTexts = await panel
      .locator('[data-flow-field="connection-target"] .el-select__selected-item')
      .allInnerTexts()
    assert.equal(
      targetSelectionTexts.some((value) => value.trim() === processCName),
      false,
      'target process should reset after connection'
    )
    const branchedLayoutRevision = Number(
      await editor.locator('.route-flow-graph-designer').getAttribute('data-flow-layout-revision')
    )
    assert.ok(
      branchedLayoutRevision > initialLayoutRevision,
      'confirming a connection should complete a whole-graph auto layout'
    )
    await assertVisibleBranchLayout(editor, processA, processB, processC)
    await page.screenshot({
      path: path.join(config.artifactDir, 'connection-created-draft.png'),
      fullPage: true
    })

    const revisionBeforeDelete = Number(
      await editor.locator('.route-flow-graph-designer').getAttribute('data-flow-layout-revision')
    )
    await editor
      .locator(`[data-flow-action="delete-edge-list"][data-edge-key="${processA}->${processB}"]`)
      .click()
    await waitForEdgeState(
      editor,
      [`${processA}->${processC}`],
      [`${processA}->${processB}`, `${processB}->${processC}`]
    )
    await waitForLayoutRevision(editor, revisionBeforeDelete)
    const revisionAfterDelete = Number(
      await editor.locator('.route-flow-graph-designer').getAttribute('data-flow-layout-revision')
    )
    assert.ok(
      revisionAfterDelete > revisionBeforeDelete,
      'deleting a process connection should complete a whole-graph auto layout'
    )
    await connectViaSelector(page, editor, processA, processB)
    await waitForEdgeState(editor, branchedRequired, [`${processB}->${processC}`])
    await assertVisibleBranchLayout(editor, processA, processB, processC)

    await editor.getByRole('tab', { name: '基础信息' }).click()
    await editor.getByRole('tab', { name: '流转关系图' }).click()
    await editor.locator('[data-flow-node="route-process"]').first().waitFor({
      state: 'visible',
      timeout: 10000
    })
    await waitForEdgeState(editor, branchedRequired, [`${processB}->${processC}`])
    await editor.locator('[data-flow-status="unsaved"]').waitFor({
      state: 'visible',
      timeout: 10000
    })
    assert.deepEqual(graphSaveRequests, [], 'discoverability E2E must not save the graph')

    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    editor = page.locator('.route-edit-page').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })
    await editor.getByRole('tab', { name: '流转关系图' }).click()
    await editor.locator('[data-flow-node="route-process"]').first().waitFor({
      state: 'visible',
      timeout: 60000
    })
    await settle(page)
    await waitForEdgeState(editor, initialRequired, [`${processA}->${processC}`])
    assert.equal(
      await editor.locator('[data-flow-status="unsaved"]').count(),
      0,
      'page reload must clear the unsaved marker'
    )
    assert.deepEqual(graphSaveRequests, [], 'page reload must discard the unsaved draft')

    const explicitSavePanel = await connectViaSelector(page, editor, processA, processC)
    await waitForEdgeState(editor, branchedRequired, [`${processB}->${processC}`])
    await editor.locator('[data-flow-action="connect-route-process"]').click()
    await explicitSavePanel.waitFor({ state: 'hidden', timeout: 10000 })
    await connectRouteProcessToEnd(editor, processB)
    await waitForBoundaryEdge(editor, branchTerminalEdge, true)
    assert.deepEqual(
      graphSaveRequests,
      [],
      'confirming the selector connection must remain local before explicit save'
    )
    await saveGraph(page, editor)
    mutationSaved = true
    assert.equal(graphSaveRequests.length, 1, 'top save must send exactly one graph save request')

    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    editor = page.locator('.route-edit-page').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })
    await editor.getByRole('tab', { name: '流转关系图' }).click()
    await editor.locator('[data-flow-node="route-process"]').first().waitFor({
      state: 'visible',
      timeout: 60000
    })
    await settle(page)
    await waitForEdgeState(editor, branchedRequired, [`${processB}->${processC}`])
    await waitForBoundaryEdge(editor, branchTerminalEdge, true)
    assert.equal(
      await editor.locator('[data-flow-status="unsaved"]').count(),
      0,
      'saved graph reload must not show an unsaved marker'
    )

    await connectViaSelector(page, editor, processB, processC)
    await waitForEdgeState(editor, initialRequired, [`${processA}->${processC}`])
    await deleteBoundaryEdgeIfPresent(editor, branchTerminalEdge)
    await saveGraph(page, editor)
    mutationSaved = false
    assert.equal(
      graphSaveRequests.length,
      2,
      'restoring the graph must send the second explicit save'
    )

    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    editor = page.locator('.route-edit-page').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })
    await editor.getByRole('tab', { name: '流转关系图' }).click()
    await editor.locator('[data-flow-node="route-process"]').first().waitFor({
      state: 'visible',
      timeout: 60000
    })
    await settle(page)
    await waitForEdgeState(editor, initialRequired, [`${processA}->${processC}`])

    const evidence = {
      ok: true,
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routeCode: config.routeCode,
      processA,
      processB,
      processC,
      processD,
      branchTerminalEdge,
      restoredEdges: await visibleEdgeKeys(editor),
      graphSaveRequestCount: graphSaveRequests.length
    }
    fs.writeFileSync(
      path.join(config.artifactDir, 'result.json'),
      `${JSON.stringify(evidence, null, 2)}\n`,
      'utf8'
    )
    console.log(
      `PASS: route flow selector keeps drafts local and saves explicitly for ${config.routeCode}`
    )
  } catch (error) {
    if (mutationSaved && page && processIds?.length >= 3) {
      try {
        const editor = await openRouteGraph(page)
        await connectViaSelector(page, editor, processIds[1], processIds[2])
        await deleteBoundaryEdgeIfPresent(editor, `${processIds[1]}->process-end`)
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
