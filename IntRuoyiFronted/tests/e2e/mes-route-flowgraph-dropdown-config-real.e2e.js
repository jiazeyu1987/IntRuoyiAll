const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.MES_ROUTE_FLOW_DROPDOWN_BASE_URL || 'http://127.0.0.1:8081',
  tenant: process.env.MES_ROUTE_FLOW_DROPDOWN_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_DROPDOWN_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_DROPDOWN_PASSWORD || '111111',
  headed: process.env.MES_ROUTE_FLOW_DROPDOWN_HEADED === '1',
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  outDir: path.resolve('tests/output/route-flowgraph-dropdown-config-real')
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `route flow dropdown real E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`
  )
}

fs.mkdirSync(config.outDir, { recursive: true })

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantSelect = form.locator('.el-select').first()
  const selectedTenantText = await tenantSelect.innerText().catch(() => '')
  if (!selectedTenantText.includes(config.tenant)) {
    const tenantInput = form
      .locator('.el-select input[role="combobox"]:visible, input.el-select__input:visible')
      .first()
    if (await tenantInput.count()) {
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
    } else {
      const textboxes = form.locator('input.el-input__inner')
      await textboxes.nth(0).fill(config.tenant)
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
  assert.ok([0, 200].includes(loginPayload.code), `login business failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForFunction(() => !window.location.pathname.includes('/login'), null, {
    timeout: 60000
  })
}

function extractRouteList(payload) {
  const data = payload?.data ?? payload
  return data?.list || payload?.list || []
}

async function selectRouteFromList(page) {
  const routePageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/route/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'commit', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const routePageResponse = await routePageResponsePromise
  const routePayload = await routePageResponse.json()
  const routes = extractRouteList(routePayload)
  const route =
    routes.find(
      (item) => item?.id && item?.pendingRouteVersionId && item?.pendingRouteVersionStatus === 'DRAFT'
    ) ||
    routes.find(
      (item) =>
        item?.id &&
        item?.activeRouteVersionId &&
        !item?.pendingRouteVersionId &&
        item?.flowGraphConfigured
    ) ||
    routes.find((item) => item?.id && item?.activeRouteVersionId && !item?.pendingRouteVersionId)
  if (!route) {
    throw new Error(
      'BLOCKER: 测试租户工艺路线列表没有可编辑或可创建候选的真实路线，无法验证流转关系图下拉。'
    )
  }
  return route
}

function routeHasDraftCandidate(route) {
  return Boolean(route?.pendingRouteVersionId && route?.pendingRouteVersionStatus === 'DRAFT')
}

function buildDraftRouteEditUrl(route) {
  const params = new URLSearchParams({ tab: 'flow' })
  if (routeHasDraftCandidate(route)) {
    params.set('routeVersionId', String(route.pendingRouteVersionId))
    params.set('routeVersionNo', String(route.pendingRouteVersionNo))
    params.set('routeVersionStatus', String(route.pendingRouteVersionStatus))
  }
  return `${config.baseUrl}/mes/pro/route/edit/${route.id}?${params.toString()}`
}

async function openEditableRouteFromList(page, route) {
  if (routeHasDraftCandidate(route)) {
    await page.goto(buildDraftRouteEditUrl(route), { waitUntil: 'commit', timeout: 60000 })
    return { createdCandidate: false }
  }

  const rowByCode = route.code
    ? page.locator('tr.el-table__row').filter({ hasText: route.code }).first()
    : page.locator('tr.el-table__row').first()
  const row = (await rowByCode.count()) > 0 ? rowByCode : page.locator('tr.el-table__row').first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })
  await page.waitForURL(
    (url) =>
      url.searchParams.get('routeVersionStatus') === 'DRAFT' &&
      url.searchParams.get('routeDraftOrigin') === 'list-edit',
    { timeout: 60000 }
  )
  return { createdCandidate: true }
}

async function cleanupCreatedCandidate(page) {
  await page.locator('[data-flow-action="back-route-list"]').first().click()
  await page.getByRole('button', { name: '不保存草稿' }).click()
  await page.waitForURL((url) => url.pathname === '/mes/pro/route', { timeout: 60000 })
}

async function waitForElementPlusMessagesToClose(page) {
  await page.waitForFunction(() => document.querySelectorAll('.el-message').length === 0, null, {
    timeout: 10000
  })
}

async function waitForElementPlusLoadingMasksToClose(page) {
  await page.waitForFunction(() => document.querySelectorAll('.el-loading-mask').length === 0, null, {
    timeout: 30000
  })
}

async function clickRouteProcessNode(page) {
  const routeProcessNodes = page.locator('[data-flow-node="route-process"]')
  await routeProcessNodes.first().waitFor({ state: 'visible', timeout: 60000 })
  await waitForElementPlusMessagesToClose(page)
  await waitForElementPlusLoadingMasksToClose(page)
  const clickTarget = await routeProcessNodes.evaluateAll((nodes) => {
    const fixedHeaderElements = Array.from(
      document.querySelectorAll('#v-tool-header, #v-tags-view')
    )
    const fixedHeaderBounds = fixedHeaderElements.map((element) => {
      const rect = element.getBoundingClientRect()
      return { top: rect.top, bottom: rect.bottom, height: rect.height }
    })
    const fixedHeaderBottom = fixedHeaderBounds.reduce(
      (bottom, rect) => Math.max(bottom, rect.bottom),
      0
    )
    const canvasRect = document
      .querySelector('.route-flow-graph-designer__canvas')
      ?.getBoundingClientRect()
    const graphRect = document
      .querySelector('.route-flow-graph-designer')
      ?.getBoundingClientRect()
    const candidates = nodes.flatMap((node, index) => {
      const rect = node.getBoundingClientRect()
      const centerX = rect.left + rect.width / 2
      const centerY = rect.top + rect.height / 2
      const points = [
        { kind: 'raw', x: centerX, y: centerY },
        { kind: 'canvas-top-adjusted', x: centerX, y: centerY + (canvasRect?.top || 0) },
        { kind: 'graph-top-adjusted', x: centerX, y: centerY + (graphRect?.top || 0) }
      ]
      return points.map((point) => {
        const hit = document.elementFromPoint(point.x, point.y)
        const routeProcessNode = hit?.closest?.('[data-flow-node="route-process"]')
        return {
          ...point,
          index,
          text: node.textContent?.trim() || '',
          hitRouteProcessId: routeProcessNode?.getAttribute('data-route-process-id') || '',
          hitText:
            routeProcessNode?.textContent?.trim() || hit?.textContent?.trim().slice(0, 120) || ''
        }
      })
    })
    const selected = candidates.find(
      (item) =>
        item.hitRouteProcessId && item.y > fixedHeaderBottom + 8 && item.y < window.innerHeight - 8
    )
    return {
      selected,
      fixedHeaderBounds,
      fixedHeaderBottom,
      viewportHeight: window.innerHeight,
      candidates: candidates.slice(0, 30),
      canvas: canvasRect ? { top: canvasRect.top, bottom: canvasRect.bottom } : null,
      graph: graphRect ? { top: graphRect.top, bottom: graphRect.bottom } : null
    }
  })
  assert.ok(
    clickTarget.selected,
    `route process node is not hittable in viewport: ${JSON.stringify(clickTarget)}`
  )
  await page.mouse.click(clickTarget.selected.x, clickTarget.selected.y)
}

async function waitForUserFieldConfigSave(page, action) {
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/user-table-column-config/save') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await action()
  const saveResponse = await saveResponsePromise
  const payload = await saveResponse.json()
  assert.equal(payload.code, 0, `save user detail field config failed: ${JSON.stringify(payload)}`)
}

async function assertFormSlotAggregateCard(page) {
  const formSlotField = page.locator('[data-flow-detail-field="formSlots"]').first()
  await formSlotField.waitFor({ state: 'visible', timeout: 10000 })
  const formSlotText = (await formSlotField.textContent())?.trim() || ''
  assert.ok(formSlotText.includes('表单槽位'), `表单槽位字段卡片缺少标题: ${formSlotText}`)
  const slotLabels = await formSlotField
    .locator('.route-flow-graph-designer__record-binding-label')
    .evaluateAll((items) => items.map((item) => item.textContent?.trim()).filter(Boolean))
  if (slotLabels.length > 0) {
    for (const expected of ['批记录表单', '损耗单', '过程检验单', '参数记录表']) {
      assert.ok(
        slotLabels.includes(expected),
        `表单槽位聚合卡片缺少 ${expected}: ${JSON.stringify(slotLabels)}`
      )
    }
  }
  return slotLabels
}

async function assertRelationListCard(page) {
  const relationListField = page.locator('[data-flow-detail-field="relationList"]').first()
  await relationListField.waitFor({ state: 'visible', timeout: 10000 })
  const relationListText = (await relationListField.textContent())?.trim() || ''
  assert.ok(
    relationListText.includes('关系清单'),
    `关系清单字段卡片缺少标题: ${relationListText}`
  )
  const selectedFieldDetail = page.locator('[data-flow-panel="selected-field-detail"]').first()
  await selectedFieldDetail.waitFor({ state: 'visible', timeout: 10000 })
  assert.equal(
    await selectedFieldDetail.locator('[data-flow-panel="relation-list-detail"]').count(),
    0,
    '未点击左侧关系清单字段前，右侧字段明细不应常态显示关系清单'
  )
  await relationListField.locator('[data-flow-detail-field-button]').click()
  const relationListDetail = selectedFieldDetail
    .locator('[data-flow-panel="relation-list-detail"]')
    .first()
  await relationListDetail.waitFor({ state: 'visible', timeout: 10000 })
  const relationListDetailText = (await relationListDetail.textContent())?.trim() || ''
  const relationItems = relationListDetail.locator('.route-flow-graph-designer__relation-item')
  const relationItemTexts = await relationItems.evaluateAll((items) =>
    items.map((item) => item.textContent?.trim() || '').filter(Boolean)
  )
  if (relationItemTexts.length === 0) {
    assert.ok(
      relationListDetailText.includes('暂无关系'),
      `关系清单明细无关系时必须显示空状态: ${relationListDetailText}`
    )
  } else {
    assert.ok(
      (await relationListDetail.locator('[data-flow-action="select-boundary-edge-list"]').count()) > 0 ||
        (await relationListDetail.locator('[data-flow-action="select-edge-list"]').count()) > 0,
      `关系清单明细有关系时必须复用现有选择动作: ${JSON.stringify(relationItemTexts)}`
    )
  }
  return {
    relationListText,
    relationListDetailText,
    relationItemTexts
  }
}

async function removeFormSlotsIfAdded(page) {
  const formSlotField = page.locator('[data-flow-detail-field="formSlots"]').first()
  if ((await formSlotField.count()) === 0) return false
  await waitForUserFieldConfigSave(page, async () => {
    await formSlotField.locator('[data-flow-action="remove-process-detail-field"]').click()
  })
  await formSlotField.waitFor({ state: 'detached', timeout: 10000 })
  return true
}

async function removeRelationListIfAdded(page) {
  const relationListField = page.locator('[data-flow-detail-field="relationList"]').first()
  if ((await relationListField.count()) === 0) return false
  await waitForUserFieldConfigSave(page, async () => {
    await relationListField.locator('[data-flow-action="remove-process-detail-field"]').click()
  })
  await relationListField.waitFor({ state: 'detached', timeout: 10000 })
  return true
}

async function addConfigItemIfNeeded(page, picker, label) {
  const addSelect = picker.locator('[data-flow-field="process-config-item-select"]').first()
  await addSelect.click()
  const optionLocator = page.locator('.el-select-dropdown__item:visible')
  await optionLocator.first().waitFor({ state: 'visible', timeout: 60000 })
  const targetOption = optionLocator.filter({ hasText: label }).first()
  await targetOption.waitFor({ state: 'visible', timeout: 10000 })
  const targetOptionClass = (await targetOption.getAttribute('class')) || ''
  if (!targetOptionClass.includes('is-disabled')) {
    await targetOption.click()
    await waitForUserFieldConfigSave(page, async () => {
      await picker.locator('[data-flow-action="add-process-config-item"]').first().click()
    })
    return true
  }
  await page.keyboard.press('Escape')
  return false
}

async function assertRouteEditFlowGraph(page, route) {
  await page.locator('.route-edit-page, .route-form-content').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await page.getByRole('tab', { name: '流转关系图' }).waitFor({ state: 'visible', timeout: 60000 })

  const tabs = await page
    .locator('.el-tabs__item')
    .evaluateAll((items) => items.map((item) => item.textContent?.trim()).filter(Boolean))
  assert.ok(tabs.includes('流转关系图'), `missing flow tab: ${JSON.stringify(tabs)}`)
  assert.ok(!tabs.includes('组成工序'), `组成工序 tab must be hidden after migration: ${JSON.stringify(tabs)}`)

  await clickRouteProcessNode(page)

  const picker = page.locator('.route-flow-graph-designer__process-detail-field-picker').first()
  await picker.waitFor({ state: 'visible', timeout: 60000 })
  const addSelect = picker.locator('[data-flow-field="process-config-item-select"]').first()
  await addSelect.waitFor({ state: 'visible', timeout: 60000 })
  await picker.locator('[data-flow-action="add-process-config-item"]').first().waitFor({
    state: 'visible',
    timeout: 60000
  })

  await addSelect.click()
  const optionLocator = page.locator('.el-select-dropdown__item:visible')
  await optionLocator.first().waitFor({ state: 'visible', timeout: 60000 })
  const optionGroupTitles = await page
    .locator('.el-popper[aria-hidden="false"] .el-select-group__title')
    .evaluateAll((items) => items.map((item) => item.textContent?.trim()).filter(Boolean))
  assert.ok(
    !optionGroupTitles.includes('工序设置列（表单槽位）') &&
      !optionGroupTitles.includes('工序设置列（基础字段）'),
    `添加配置项下拉不应继续显示分组标题: ${JSON.stringify(optionGroupTitles)}`
  )
  const optionTexts = await optionLocator.evaluateAll((items) =>
    items.map((item) => item.textContent?.trim()).filter(Boolean)
  )
  assert.ok(
    optionTexts.some((text) => text.includes('表单槽位')),
    `添加配置项下拉必须可见通用表单槽位列: ${JSON.stringify(optionTexts)}`
  )
  assert.ok(
    optionTexts.some((text) => text.includes('关系清单')),
    `添加配置项下拉必须可见关系清单列: ${JSON.stringify(optionTexts)}`
  )
  for (const expected of ['批记录表单', '损耗单', '过程检验单', '参数记录表']) {
    assert.ok(
      optionTexts.some((text) => text.includes(expected)),
      `迁移配置项必须始终在下拉列表中可见: ${expected}, options=${JSON.stringify(optionTexts)}`
    )
  }
  await page.keyboard.press('Escape')

  const formSlotsAddedByTest = await addConfigItemIfNeeded(page, picker, '表单槽位')
  const formSlotLabels = await assertFormSlotAggregateCard(page)
  const relationListAddedByTest = await addConfigItemIfNeeded(page, picker, '关系清单')
  const relationListText = await assertRelationListCard(page)
  const selectedCardTexts = await page.locator('[data-flow-detail-field]').evaluateAll((items) =>
    items.map((item) => item.textContent?.trim() || '').filter(Boolean)
  )
  if (relationListAddedByTest) {
    await removeRelationListIfAdded(page)
  }
  if (formSlotsAddedByTest) {
    await removeFormSlotsIfAdded(page)
  }

  return {
    routeId: route.id,
    routeCode: route.code,
    routeName: route.name,
    routeOpenedWithExistingDraft: routeHasDraftCandidate(route),
    tabs,
    optionTexts,
    optionGroupTitles,
    selectedCardTexts,
    formSlotsAddedByTest,
    relationListAddedByTest,
    formSlotLabels,
    relationListText
  }
}

function isAllowedCandidateSetupWrite(request) {
  return (
    request.method === 'POST' &&
    (request.url.includes('/mes/pro/route-version/create-candidate') ||
      request.url.includes('/mes/pro/route-version/cancel'))
  )
}

;(async () => {
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: fs.existsSync(config.executablePath) ? config.executablePath : undefined,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const mesWriteRequests = []
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))
  page.on('request', (request) => {
    if (
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) &&
      request.url().includes('/mes/')
    ) {
      mesWriteRequests.push({ method: request.method(), url: request.url() })
    }
  })

  let createdCandidateForCleanup = false
  try {
    await login(page)
    const route = await selectRouteFromList(page)
    const editOpenResult = await openEditableRouteFromList(page, route)
    createdCandidateForCleanup = editOpenResult.createdCandidate
    const result = await assertRouteEditFlowGraph(page, route)
    if (editOpenResult.createdCandidate) {
      await cleanupCreatedCandidate(page)
      createdCandidateForCleanup = false
    }
    const unexpectedMesWriteRequests = mesWriteRequests.filter(
      (request) => !isAllowedCandidateSetupWrite(request)
    )
    assert.deepEqual(
      unexpectedMesWriteRequests,
      [],
      `除候选创建/丢弃外不得产生 MES 写请求: ${JSON.stringify(mesWriteRequests)}`
    )
    if (editOpenResult.createdCandidate) {
      assert.ok(
        mesWriteRequests.some((request) => request.url.includes('/mes/pro/route-version/create-candidate')),
        `临时候选创建未被记录: ${JSON.stringify(mesWriteRequests)}`
      )
      assert.ok(
        mesWriteRequests.some((request) => request.url.includes('/mes/pro/route-version/cancel')),
        `临时候选丢弃未被记录: ${JSON.stringify(mesWriteRequests)}`
      )
    }
    assert.deepEqual(pageErrors, [], `页面存在未捕获异常: ${JSON.stringify(pageErrors)}`)
    const artifact = {
      ...result,
      tenant: config.tenant,
      username: config.username,
      createdCandidateForEditablePath: editOpenResult.createdCandidate,
      mesWriteRequests,
      pageErrors
    }
    fs.writeFileSync(path.join(config.outDir, 'result.json'), JSON.stringify(artifact, null, 2), 'utf8')
    await page.screenshot({ path: path.join(config.outDir, 'route-flowgraph-dropdown.png'), fullPage: true })
    console.log(`mes-route-flowgraph-dropdown-config-real PASS ${JSON.stringify(artifact)}`)
  } finally {
    if (createdCandidateForCleanup) {
      try {
        await cleanupCreatedCandidate(page)
      } catch (error) {
        console.error(`WARN: failed to cleanup created route candidate: ${error.message}`)
      }
    }
    await browser.close()
  }
})().catch((error) => {
  console.error(error)
  process.exit(1)
})
