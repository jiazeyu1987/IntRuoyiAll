const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_DYNAMIC_FORMS_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_DYNAMIC_FORMS_TENANT || '测试租户',
  username: process.env.MES_ROUTE_DYNAMIC_FORMS_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_DYNAMIC_FORMS_PASSWORD || '111111',
  tenantId: process.env.MES_ROUTE_DYNAMIC_FORMS_TENANT_ID || '122',
  headed: process.env.MES_ROUTE_DYNAMIC_FORMS_HEADED === '1',
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  outDir: path.resolve('tests/output/mes-route-dynamic-form-slots-real')
}

if (config.baseUrl !== 'http://localhost:8081') {
  throw new Error(`real E2E must use local frontend http://localhost:8081, got ${config.baseUrl}`)
}
if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(`real E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`)
}

fs.mkdirSync(config.outDir, { recursive: true })

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
const escapeRegExp = (value) => String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
const uniqueBatchCode = () => `CODX-RF-${new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)}`

function unwrapCacheValue(value) {
  if (!value || typeof value !== 'object') return value
  for (const field of ['accessToken', 'v', 'value', 'data']) {
    if (Object.prototype.hasOwnProperty.call(value, field)) {
      return unwrapCacheValue(value[field])
    }
  }
  return value
}

function normalizeCacheString(value) {
  if (typeof value === 'string' && value.startsWith('"') && value.endsWith('"')) {
    return value.slice(1, -1)
  }
  return value
}

function unwrapResult(payload) {
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `API business failure: ${JSON.stringify(payload)}`)
  return payload.data
}

function sanitizeResponseUrl(rawUrl) {
  const url = new URL(rawUrl)
  return `${url.pathname}${url.search}`
}

function waitForResponseResult(page, predicate, timeout = 60000) {
  return page
    .waitForResponse(predicate, { timeout })
    .then((response) => ({ response }))
    .catch((error) => ({ error }))
}

function startMesResponseRecorder(page) {
  const observed = []
  const listener = (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/mes/pro/') && !url.includes('/admin-api/form-center/')) return
    observed.push({
      method: response.request().method(),
      status: response.status(),
      url: sanitizeResponseUrl(url)
    })
  }
  page.on('response', listener)
  return {
    observed,
    stop: () => page.off('response', listener)
  }
}

async function visibleTexts(page, selector) {
  return page
    .locator(selector)
    .evaluateAll((items) => items.map((item) => item.textContent?.trim()).filter(Boolean))
    .catch(() => [])
}

async function unwrapExpectedResponse(page, result, label, observed) {
  if (result.error) {
    const messages = await visibleTexts(page, '.el-message:visible, .el-message-box:visible')
    throw new Error(
      `${label} response not observed: ${result.error.message}; messages=${JSON.stringify(messages)}; observed=${JSON.stringify(observed)}`
    )
  }
  return unwrapResult(await result.response.json())
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(300)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if (await input.isVisible().catch(() => false)) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('BLOCKER: login captcha is enabled; unattended Playwright E2E cannot continue.')
  }

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])'), config.username, 'username')
  await fillFirstVisible(form.locator('input[placeholder="请输入密码"], input[type="password"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const body = await loginResponse.json()
  assert.equal(loginResponse.status(), 200, `login HTTP status must be 200: ${loginResponse.status()}`)
  unwrapResult(body)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function readBrowserCache(page, key) {
  return page.evaluate((cacheKey) => {
    const unwrap = (value) => {
      if (!value || typeof value !== 'object') return value
      for (const field of ['accessToken', 'v', 'value', 'data']) {
        if (Object.prototype.hasOwnProperty.call(value, field)) {
          return unwrap(value[field])
        }
      }
      return value
    }
    for (const storage of [localStorage, sessionStorage]) {
      let raw = storage.getItem(cacheKey)
      if (!raw) {
        const matched = Object.keys(storage).find((item) => item === cacheKey || item.endsWith(cacheKey))
        if (matched) raw = storage.getItem(matched)
      }
      if (!raw) continue
      try {
        return unwrap(JSON.parse(raw))
      } catch (_error) {
        return raw
      }
    }
    return undefined
  }, key)
}

async function apiFetch(page, apiPath, options = {}) {
  const accessToken = normalizeCacheString(unwrapCacheValue(await readBrowserCache(page, 'ACCESS_TOKEN')))
  if (!accessToken) throw new Error('missing ACCESS_TOKEN after UI login')
  const tenantId = normalizeCacheString(unwrapCacheValue(await readBrowserCache(page, 'tenantId'))) || config.tenantId
  const response = await page.evaluate(
    async ({ requestPath, method, body, headers }) => {
      const res = await fetch(`/admin-api${requestPath}`, {
        method,
        credentials: 'omit',
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
      })
      return { status: res.status, body: await res.json().catch(() => null) }
    },
    {
      requestPath: apiPath,
      method: options.method || 'GET',
      body: options.body,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId),
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
    }
  )
  assert.equal(response.status, 200, `API ${apiPath} HTTP status must be 200: ${response.status}`)
  return unwrapResult(response.body)
}

function visibleDialog(page, title) {
  return page.locator('.el-dialog:visible').filter({ hasText: title }).last()
}

function visibleMessageBox(page, title) {
  return page.locator('.el-message-box:visible').filter({ hasText: title }).last()
}

async function clickConfirmButton(page, title) {
  const box = visibleMessageBox(page, title)
  await box.waitFor({ state: 'visible', timeout: 60000 })
  const button = box.getByRole('button', { name: /确定|确认|OK/ }).last()
  await button.click()
}

function activeOptions(page) {
  return page
    .locator('.el-select-dropdown:visible')
    .last()
    .locator('.el-select-dropdown__item:not(.is-disabled)')
}

function exactTextPattern(value) {
  return new RegExp(`^\\s*${escapeRegExp(value)}\\s*$`)
}

async function ensureWorkOrderOptionsOpen(page, workOrderSelect) {
  const options = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ has: page.locator('.edhr-batch-page__work-order-code') })
    .filter({ hasNotText: '批记录流程' })
  if (!(await options.first().isVisible().catch(() => false))) {
    await workOrderSelect.locator('input').first().click({ force: true })
  }
  await options.first().waitFor({ state: 'visible', timeout: 60000 })
  return options
}

function formItem(scope, label) {
  return scope.locator('.el-form-item:visible').filter({ hasText: label }).first()
}

async function getJsonResponseData(response) {
  const body = await response.json()
  return unwrapResult(body)
}

async function gotoBatchList(page, query = {}) {
  const url = new URL('/mes/pro/feedback/edhr-batch-execution', config.baseUrl)
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && String(value) !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('打开/创建').first().waitFor({ state: 'visible', timeout: 60000 })
}

async function openBatchCreateDialog(page, query = {}) {
  await gotoBatchList(page, query)
  await page.getByRole('button', { name: '打开/创建' }).click()
  const dialog = visibleDialog(page, '打开或创建 eDHR 批次执行')
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  return dialog
}

async function fetchSelectableWorkOrderCandidates(page) {
  const candidates = []
  const pageSize = 50
  for (let pageNo = 1; pageNo <= 20; pageNo += 1) {
    const pageData = await apiFetch(
      page,
      `/mes/pro/work-order/page?pageNo=${pageNo}&pageSize=${pageSize}&temporaryFrozen=false`
    )
    const list = pageData?.list || []
    for (const workOrder of list) {
      if (!workOrder?.id || !workOrder?.code || Number(workOrder.status) === 40) continue
      candidates.push({
        workOrderId: Number(workOrder.id),
        workOrderCode: workOrder.code,
        optionText: [workOrder.code, workOrder.name, workOrder.productName, `ID ${workOrder.id}`]
          .filter(Boolean)
          .join(' / ')
      })
    }
    if (list.length < pageSize || candidates.length >= Number(pageData?.total || 0)) break
  }
  return candidates
}

async function discoverWorkOrderAndRouteByUi(page) {
  await gotoBatchList(page)
  const workOrderCandidates = await fetchSelectableWorkOrderCandidates(page)
  const maxAttempts = Math.min(workOrderCandidates.length, 1000)
  const skippedRoutes = []
  for (let index = 0; index < maxAttempts; index += 1) {
    const candidate = workOrderCandidates[index]
    const workOrderCode = candidate.workOrderCode
    const optionText = candidate.optionText
    const workOrderId = candidate.workOrderId
    const routeOptions = await apiFetch(
      page,
      `/mes/pro/edhr-batch-execution/work-order-route-options?workOrderId=${Number(workOrderId)}`
    )
    const enabledRouteOptions = (routeOptions || []).filter(
      (item) => item && item.routeId && item.routeCode && item.batchRouteEnabled !== false
    )
    for (const routeOption of enabledRouteOptions) {
      const graph = await apiFetch(page, `/mes/pro/route-process-flow/get?routeId=${Number(routeOption.routeId)}`)
      if (!graph?.valid) {
        skippedRoutes.push({
          workOrderCode,
          routeCode: routeOption.routeCode,
          validationStatus: graph?.validationStatus,
          messages: (graph?.validationMessages || []).map((item) => item.code)
        })
        continue
      }

      return {
        workOrderId,
        workOrderCode,
        workOrderOptionText: optionText,
        routeOption,
        activeGraphVersion: graph.graphVersion
      }
    }
  }
  throw new Error(
    `BLOCKER: 测试租户可选生产工单没有已启用且流转图有效的批记录路线。scannedWorkOrders=${maxAttempts}; skipped=${JSON.stringify(skippedRoutes.slice(0, 50))}`
  )
}

async function gotoRouteList(page, routeCode) {
  const url = new URL('/mes/pro/route', config.baseUrl)
  url.searchParams.set('code', routeCode)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/route/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.el-table').first().waitFor({ state: 'visible', timeout: 60000 })
  const routePageData = await getJsonResponseData(await responsePromise)
  const routes = routePageData?.list || []
  const route = routes.find((item) => Number(item.id) > 0 && item.code === routeCode)
  if (!route) {
    throw new Error(`BLOCKER: 路线列表按编码 ${routeCode} 未返回目标路线。`)
  }
  if (route.pendingRouteVersionStatus && route.pendingRouteVersionStatus !== 'DRAFT') {
    throw new Error(
      `BLOCKER: 路线 ${routeCode} 当前候选状态为 ${route.pendingRouteVersionStatus}，需先撤回/取消后才能配置动态表单。`
    )
  }
  return route
}

async function openRouteCandidateFromList(page, routeCode) {
  const route = await gotoRouteList(page, routeCode)
  const row = page.locator('tr.el-table__row').filter({ hasText: routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })
  await page.waitForURL(
    (url) =>
      url.searchParams.get('routeVersionStatus') === 'DRAFT' &&
      Number(url.searchParams.get('routeVersionId')) > 0,
    { timeout: 60000 }
  )
  return route
}

async function waitForElementPlusMessagesToClose(page) {
  await page.waitForFunction(() => document.querySelectorAll('.el-message').length === 0, null, {
    timeout: 15000
  }).catch(() => {})
}

async function waitForElementPlusLoadingMasksToClose(page) {
  await page.waitForFunction(() => document.querySelectorAll('.el-loading-mask').length === 0, null, {
    timeout: 30000
  }).catch(() => {})
}

async function clickRouteProcessNode(page) {
  const routeProcessNodes = page.locator('[data-flow-node="route-process"]')
  await routeProcessNodes.first().waitFor({ state: 'visible', timeout: 60000 })
  await waitForElementPlusMessagesToClose(page)
  await waitForElementPlusLoadingMasksToClose(page)
  const clickTarget = await routeProcessNodes.evaluateAll((nodes) => {
    const fixedHeaderBottom = Array.from(document.querySelectorAll('#v-tool-header, #v-tags-view'))
      .map((element) => element.getBoundingClientRect().bottom)
      .reduce((max, bottom) => Math.max(max, bottom), 0)
    const candidates = nodes.map((node, index) => {
      const rect = node.getBoundingClientRect()
      const x = rect.left + rect.width / 2
      const y = rect.top + rect.height / 2
      const hit = document.elementFromPoint(x, y)
      const routeProcessNode = hit?.closest?.('[data-flow-node="route-process"]')
      return {
        index,
        x,
        y,
        routeProcessId: node.getAttribute('data-route-process-id') || '',
        hitRouteProcessId: routeProcessNode?.getAttribute('data-route-process-id') || '',
        text: node.textContent?.trim() || ''
      }
    })
    return candidates.find((item) => item.hitRouteProcessId && item.y > fixedHeaderBottom + 8)
  })
  assert.ok(clickTarget, 'no hittable route process node found')
  await page.mouse.click(clickTarget.x, clickTarget.y)
  return clickTarget
}

async function waitForUserFieldConfigSave(page, action) {
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/user-table-column-config/save') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await action()
  const payload = await (await saveResponsePromise).json()
  unwrapResult(payload)
}

async function ensureFormSlotsEditor(page) {
  const picker = page.locator('.route-flow-graph-designer__process-detail-field-picker').first()
  await picker.waitFor({ state: 'visible', timeout: 60000 })
  let formSlotField = page.locator('[data-flow-detail-field="formSlots"]').first()
  if ((await formSlotField.count()) === 0) {
    const addSelect = picker.locator('[data-flow-field="process-config-item-select"]').first()
    await addSelect.click()
    const formSlotOption = activeOptions(page).filter({ hasText: '表单槽位' }).first()
    await formSlotOption.waitFor({ state: 'visible', timeout: 30000 })
    await formSlotOption.click()
    await waitForUserFieldConfigSave(page, async () => {
      await picker.locator('[data-flow-action="add-process-config-item"]').first().click()
    })
    formSlotField = page.locator('[data-flow-detail-field="formSlots"]').first()
  }
  await formSlotField.waitFor({ state: 'visible', timeout: 30000 })
  await formSlotField.locator('[data-flow-detail-field-button]').click()
  await page.locator('[data-flow-action="add-form-binding"]').waitFor({ state: 'visible', timeout: 30000 })
}

async function clearExistingFormBindingRows(page) {
  const fixedLabels = ['批记录表单', '损耗单', '过程检验单', '参数记录表']
  const existingLabels = await page
    .locator('.route-flow-graph-designer__record-binding-label')
    .evaluateAll((items) => items.map((item) => item.textContent?.trim()).filter(Boolean))
  for (const label of existingLabels) {
    assert.ok(!fixedLabels.includes(label), `fixed form slot label must not be rendered: ${label}`)
  }
  for (let guard = 0; guard < 20; guard += 1) {
    const rows = page.locator('.route-flow-graph-designer__record-binding-item')
    const count = await rows.count()
    if (count === 0) return
    await rows.nth(count - 1).locator('[data-flow-action="remove-form-binding"]').click()
    await page.waitForFunction(
      (previousCount) => document.querySelectorAll('.route-flow-graph-designer__record-binding-item').length < previousCount,
      count,
      { timeout: 10000 }
    )
  }
  throw new Error('failed to clear existing dynamic form binding rows within guard limit')
}

async function waitForTemplatePoolResponse(page, action) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/form-center/template-pool') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await action()
  return getJsonResponseData(await responsePromise)
}

function dedupeTemplates(items) {
  const byTemplateId = new Map()
  for (const item of items || []) {
    if (!byTemplateId.has(Number(item.templateId))) {
      byTemplateId.set(Number(item.templateId), item)
    }
  }
  return [...byTemplateId.values()]
}

async function addFormBindingRow(page) {
  await page.locator('[data-flow-action="add-form-binding"]').click()
  await page.locator('.route-flow-graph-designer__record-binding-item').last().waitFor({ state: 'visible', timeout: 10000 })
}

async function openTemplateSelect(page, rowIndex) {
  const row = page.locator('.route-flow-graph-designer__record-binding-item').nth(rowIndex)
  await row.waitFor({ state: 'visible', timeout: 10000 })
  await row.locator('[data-route-process-setting-field="form-template"]').click()
}

async function selectTemplateByName(page, rowIndex, templateName) {
  await openTemplateSelect(page, rowIndex)
  const option = activeOptions(page).filter({ hasText: exactTextPattern(templateName) }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function configureCandidateFiller(page, rowIndex) {
  const row = page.locator('.route-flow-graph-designer__record-binding-item').nth(rowIndex)
  await row.locator('[data-route-process-setting-field="candidate-source-type"]').click()
  await activeOptions(page).filter({ hasText: /^个人$/ }).first().click()
  await row.locator('[data-route-process-setting-field="candidate-source-id"]').click()
  const userOption = activeOptions(page).filter({ hasText: new RegExp(escapeRegExp(config.username)) }).first()
  await userOption.waitFor({ state: 'visible', timeout: 60000 })
  await userOption.click()
}

async function configureDynamicFormBindings(page) {
  await page.locator('.route-edit-page, .route-form-content').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('tab', { name: '流转关系图' }).waitFor({ state: 'visible', timeout: 60000 })
  const selectedNode = await clickRouteProcessNode(page)
  await ensureFormSlotsEditor(page)
  await clearExistingFormBindingRows(page)
  await page.getByText('暂无表单，点击新增表单后从表单中心模板选择').waitFor({ state: 'visible', timeout: 10000 })

  const templatePool = await waitForTemplatePoolResponse(page, () => addFormBindingRow(page))
  const rawTemplates = templatePool?.list || []
  const duplicateTemplateIds = rawTemplates
    .map((item) => Number(item.templateId))
    .filter((templateId, index, values) => values.indexOf(templateId) !== index)
  assert.ok(rawTemplates.length >= 2, `published template pool must contain at least 2 rows: ${JSON.stringify(rawTemplates)}`)
  assert.ok(duplicateTemplateIds.length > 0, 'published template pool must include multiple versions of at least one templateId for this BDD.')
  const templates = dedupeTemplates(rawTemplates)
  assert.ok(templates.length >= 2, `published template pool must contain at least 2 distinct templateIds: ${JSON.stringify(rawTemplates)}`)
  const [templateA, templateB] = templates

  await openTemplateSelect(page, 0)
  await activeOptions(page).filter({ hasText: exactTextPattern(templateA.templateName) }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  const visibleTemplateLabels = await activeOptions(page).evaluateAll((items) =>
    items.map((item) => item.textContent?.trim()).filter(Boolean)
  )
  assert.ok(visibleTemplateLabels.includes(templateA.templateName), `template dropdown must show template name ${templateA.templateName}`)
  for (const raw of rawTemplates) {
    if (raw.versionNo) {
      assert.ok(
        !visibleTemplateLabels.some((label) => label.includes(raw.versionNo)),
        `template dropdown label must not show versionNo ${raw.versionNo}: ${JSON.stringify(visibleTemplateLabels)}`
      )
    }
  }
  await activeOptions(page).filter({ hasText: exactTextPattern(templateA.templateName) }).first().click()

  await addFormBindingRow(page)
  await selectTemplateByName(page, 1, templateA.templateName)
  await page.getByText('同一工序表单重复', { exact: false }).waitFor({ state: 'visible', timeout: 10000 })
  await selectTemplateByName(page, 1, templateB.templateName)

  await addFormBindingRow(page)
  await page.locator('.route-flow-graph-designer__record-binding-item').nth(2).locator('[data-flow-action="remove-form-binding"]').click()
  await page.waitForFunction(() => document.querySelectorAll('.route-flow-graph-designer__record-binding-item').length === 2, null, {
    timeout: 10000
  })
  await page.locator('.route-flow-graph-designer__record-binding-item').nth(0).locator('[data-flow-action="move-form-binding-down"]').click()
  await page.locator('.route-flow-graph-designer__record-binding-item').nth(1).locator('[data-flow-action="move-form-binding-up"]').click()
  await configureCandidateFiller(page, 0)
  await configureCandidateFiller(page, 1)

  return {
    routeProcessId: Number(selectedNode.routeProcessId || selectedNode.hitRouteProcessId),
    templates: [
      { templateId: Number(templateA.templateId), templateName: templateA.templateName },
      { templateId: Number(templateB.templateId), templateName: templateB.templateName }
    ],
    visibleTemplateLabels
  }
}

async function saveAndPublishRouteCandidate(page) {
  const recorder = startMesResponseRecorder(page)
  const validationResponsePromise = waitForResponseResult(
    page,
    (response) => response.url().includes('/admin-api/mes/pro/route-process-flow/validate') && response.request().method() === 'POST'
  )
  const flowSaveResponsePromise = waitForResponseResult(
    page,
    (response) => response.url().includes('/admin-api/mes/pro/route-process-flow/save') && response.request().method() === 'POST',
  )
  const batchConfigSaveResponsePromise = waitForResponseResult(
    page,
    (response) => response.url().includes('/admin-api/mes/pro/route/flow-config/batch-record/save') && response.request().method() === 'POST',
  )
  try {
    await page.locator('[data-flow-action="save-route-flow"]').click()
    const validation = await unwrapExpectedResponse(page, await validationResponsePromise, 'route flow validation', recorder.observed)
    assert.ok(validation.valid, `route flow validation must pass before save: ${JSON.stringify(validation)}`)
    await unwrapExpectedResponse(page, await flowSaveResponsePromise, 'route flow save', recorder.observed)
    await unwrapExpectedResponse(page, await batchConfigSaveResponsePromise, 'dynamic form binding save', recorder.observed)
  } finally {
    recorder.stop()
  }

  await clickConfirmButton(page, '提交发布')
  const publishResponse = await page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/route-version/submit-publish') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const publishedVersion = unwrapResult(await publishResponse.json())
  if (publishedVersion.lifecycleStatus !== 'ACTIVE') {
    throw new Error(
      `BLOCKER: 路线候选版本提交后状态为 ${publishedVersion.lifecycleStatus}，缺少审批账号或直接发布策略，无法继续启动 eDHR 运行态验证。`
    )
  }
  await page.waitForURL((url) => !url.searchParams.get('routeVersionStatus'), { timeout: 60000 })
  return publishedVersion
}

async function createBatchByUi(page, selected, batchCode) {
  const dialog = await openBatchCreateDialog(page, { prefillWorkOrderCode: selected.workOrderCode })
  await page.waitForFunction(
    (routeCode) => document.body.innerText.includes(routeCode),
    selected.routeOption.routeCode,
    { timeout: 60000 }
  )
  const routeSelect = formItem(dialog, '工艺路线').locator('.el-select').first()
  if (!((await routeSelect.innerText()).includes(selected.routeOption.routeCode))) {
    await routeSelect.click()
    await activeOptions(page).filter({ hasText: new RegExp(escapeRegExp(selected.routeOption.routeCode)) }).first().click()
  }
  const batchCodeInput = formItem(dialog, '批次号').locator('input').first()
  await batchCodeInput.fill(batchCode)

  const openResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/open-or-create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: /确\s*认|确认/ }).click()
  const batch = unwrapResult(await (await openResponsePromise).json())
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-batch-execution/detail'), {
    timeout: 60000
  })
  await page.getByText(batch.batchExecutionCode || batchCode, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return batch
}

async function verifyDynamicRouteFormTasks(page, batchId, routeProcessId, templates) {
  const detail = await apiFetch(page, `/mes/pro/edhr-batch-execution/get?id=${batchId}`)
  const selectedTemplateIds = new Set(templates.map((item) => Number(item.templateId)))
  const dynamicTasks = (detail.tasks || []).filter(
    (task) =>
      task.nodeType === 'ROUTE_FORM' &&
      Number(task.routeProcessId) === Number(routeProcessId) &&
      selectedTemplateIds.has(Number(task.formTemplateId))
  )
  assert.equal(dynamicTasks.length, 2, `batch must generate exactly two dynamic route form tasks for selected process: ${JSON.stringify(detail.tasks)}`)
  for (const task of dynamicTasks) {
    assert.ok(task.formBindingKey, `task ${task.id} must snapshot formBindingKey`)
    assert.ok(Number(task.formTemplateId) > 0, `task ${task.id} must snapshot formTemplateId`)
    assert.ok(Number(task.formTemplateVersionId) > 0, `task ${task.id} must snapshot formTemplateVersionId`)
    assert.ok(task.formTemplateVersionNo, `task ${task.id} must snapshot formTemplateVersionNo`)
    assert.ok(Number(task.formCenterInstanceId) > 0, `task ${task.id} must snapshot formCenterInstanceId`)
    assert.ok(Number(task.activeWorkTaskId) > 0, `task ${task.id} must have active work task for UI opening`)
  }
  return { detail, dynamicTasks }
}

async function openAndSubmitFirstDynamicTaskByUi(page, task) {
  const taskCard = page
    .locator('.edhr-batch-detail__rail-process-form-item')
    .filter({ hasText: task.formTemplateName || String(task.formTemplateId) })
    .first()
  await taskCard.waitFor({ state: 'visible', timeout: 60000 })
  const openResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await taskCard.getByRole('button', { name: /打开填写|打开返工/ }).click()
  const opened = unwrapResult(await (await openResponsePromise).json())
  assert.ok(opened.formCenterInstanceId, 'opened task must return formCenterInstanceId')
  assert.ok(opened.formTemplateId, 'opened task must return formTemplateId')
  assert.ok(opened.formTemplateVersionId, 'opened task must return formTemplateVersionId')

  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '填写表单' }).last()
  await drawer.waitFor({ state: 'visible', timeout: 60000 })
  await drawer.locator('.form-action-panel').waitFor({ state: 'visible', timeout: 60000 })
  const draftResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/form-center/instances/${opened.formCenterInstanceId}/draft`) &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await drawer.getByRole('button', { name: '保存草稿' }).click()
  unwrapResult(await (await draftResponsePromise).json())

  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/form-center/instances/${opened.formCenterInstanceId}/submit`) &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await drawer.getByRole('button', { name: /^提交$/ }).click()
  const submitted = unwrapResult(await (await submitResponsePromise).json())
  assert.ok(['EFFECTIVE', 'PENDING_EFFECT', 'IN_APPROVAL'].includes(submitted.status), `unexpected submitted status: ${submitted.status}`)
  return { opened, submitted }
}

async function main() {
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: fs.existsSync(config.executablePath) ? config.executablePath : undefined,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))

  try {
    await login(page)
    const selected = await discoverWorkOrderAndRouteByUi(page)
    await openRouteCandidateFromList(page, selected.routeOption.routeCode)
    const formBindingEvidence = await configureDynamicFormBindings(page)
    const publishedVersion = await saveAndPublishRouteCandidate(page)
    const batchCode = uniqueBatchCode()
    const batch = await createBatchByUi(page, selected, batchCode)
    const taskEvidence = await verifyDynamicRouteFormTasks(
      page,
      batch.id,
      formBindingEvidence.routeProcessId,
      formBindingEvidence.templates
    )
    const submitEvidence = await openAndSubmitFirstDynamicTaskByUi(page, taskEvidence.dynamicTasks[0])
    const finalBatch = await apiFetch(page, `/mes/pro/edhr-batch-execution/get?id=${batch.id}`)
    assert.deepEqual(pageErrors, [], `page errors must be empty: ${JSON.stringify(pageErrors)}`)

    const artifact = {
      tenant: config.tenant,
      username: config.username,
      selected,
      routeProcessId: formBindingEvidence.routeProcessId,
      templates: formBindingEvidence.templates,
      publishedVersion: {
        id: publishedVersion.id,
        versionNo: publishedVersion.versionNo,
        lifecycleStatus: publishedVersion.lifecycleStatus
      },
      batch: {
        id: batch.id,
        batchExecutionCode: batch.batchExecutionCode,
        batchCode: batch.batchCode,
        routeId: batch.routeId,
        routeVersionId: batch.routeVersionId
      },
      dynamicTasks: taskEvidence.dynamicTasks.map((task) => ({
        id: task.id,
        routeProcessId: task.routeProcessId,
        formBindingKey: task.formBindingKey,
        formTemplateId: task.formTemplateId,
        formTemplateName: task.formTemplateName,
        formTemplateVersionId: task.formTemplateVersionId,
        formTemplateVersionNo: task.formTemplateVersionNo,
        formCenterInstanceId: task.formCenterInstanceId,
        activeWorkTaskId: task.activeWorkTaskId,
        status: task.status
      })),
      opened: {
        taskId: submitEvidence.opened.taskId,
        formBindingKey: submitEvidence.opened.formBindingKey,
        formTemplateId: submitEvidence.opened.formTemplateId,
        formTemplateVersionId: submitEvidence.opened.formTemplateVersionId,
        formTemplateVersionNo: submitEvidence.opened.formTemplateVersionNo,
        formCenterInstanceId: submitEvidence.opened.formCenterInstanceId
      },
      submitted: {
        instanceId: submitEvidence.submitted.id,
        status: submitEvidence.submitted.status,
        bpmProcessInstanceId: submitEvidence.submitted.bpmProcessInstanceId || ''
      },
      finalTaskStatuses: (finalBatch.tasks || [])
        .filter((task) => taskEvidence.dynamicTasks.some((dynamicTask) => dynamicTask.id === task.id))
        .map((task) => ({ id: task.id, status: task.status, submittedAt: task.submittedAt })),
      pageErrors
    }
    fs.writeFileSync(path.join(config.outDir, 'result.json'), `${JSON.stringify(artifact, null, 2)}\n`, 'utf8')
    await page.screenshot({ path: path.join(config.outDir, 'final.png'), fullPage: true })
    console.log(`mes-route-dynamic-form-slots-real PASS ${JSON.stringify(artifact)}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
