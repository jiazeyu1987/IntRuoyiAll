const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_ROUTE_OPERATIONS_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_ROUTE_OPERATIONS_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_ROUTE_OPERATIONS_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_ROUTE_OPERATIONS_E2E_PASSWORD || '111111'
const ALLOW_TEST_WRITE = process.env.DCC_ROUTE_OPERATIONS_E2E_ALLOW_TEST_WRITE === '1'
const MARKER = process.env.DCC_ROUTE_OPERATIONS_E2E_MARKER || `CODEX_ROUTE_OPS_${Date.now().toString(36).toUpperCase()}`
const CATEGORY_CODE = MARKER
const CATEGORY_NAME = `流程路线E2E-${MARKER}`
const ROUTE_REMARK = `route-create-${MARKER}`
const ROUTE_UPDATED_REMARK = `route-edit-${MARKER}`
const ROUTES_PATH = '/dcc/controlled-file/routes'
const CATEGORIES_PATH = '/dcc/controlled-file/categories'
const RESULT_DIR = path.resolve(process.cwd(), 'output/playwright/dcc-route-operations-real-e2e')
const CHROME_EXECUTABLE = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
const CATEGORY_TAXONOMY_PATH = (process.env.DCC_ROUTE_OPERATIONS_E2E_TAXONOMY_PATH || '\u6280\u672f\u6587\u6863/\u8bbe\u8ba1\u548c\u5f00\u53d1\u8f93\u5165\u9636\u6bb5/Codex\u8f93\u5165E2E\u53f6\u5b50').split('/')

function ensureResultDir() {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
}

function writeResult(result) {
  ensureResultDir()
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/)
  assert.equal(TENANT, '测试租户')
  assert.equal(USERNAME, 'aoteman')
  assert.equal(ALLOW_TEST_WRITE, true, 'Set DCC_ROUTE_OPERATIONS_E2E_ALLOW_TEST_WRITE=1 for test-tenant write verification')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if (await input.isVisible()) {
      await input.fill('')
      await input.fill(value)
      return
    }
  }
  throw new Error(`missing visible input: ${label}`)
}

async function login(page, targetPath) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }
  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"])'), USERNAME, 'username')
  await fillFirstVisible(form.locator('input[type="password"]'), PASSWORD, 'password')
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) => item.url().includes('/system/auth/login') && item.request().method() === 'POST',
      { timeout: 60000 }
    ),
    form.getByRole('button', { name: '登录' }).click()
  ])
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && payload && [0, 200].includes(payload.code), `login failed: ${JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) return ''
  const normalizeString = (value) => {
    let current = value || ''
    for (let index = 0; index < 3; index += 1) {
      const trimmed = String(current).trim()
      if (!trimmed.startsWith('"')) return trimmed
      try {
        const parsed = JSON.parse(trimmed)
        if (typeof parsed !== 'string' || parsed === current) return trimmed.replace(/^"(.*)"$/, '$1')
        current = parsed
      } catch {
        return trimmed.replace(/^"(.*)"$/, '$1')
      }
    }
    return String(current).trim()
  }
  const unwrap = (value) => {
    let current = value
    for (let index = 0; index < 6; index += 1) {
      if (!current || typeof current !== 'object') {
        return typeof current === 'string' ? normalizeString(current) : current || ''
      }
      if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
        current = current.accessToken
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'v')) {
        current = current.v
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'value')) {
        current = current.value
        continue
      }
      return current
    }
    return current || ''
  }
  try {
    return unwrap(JSON.parse(raw))
  } catch {
    return normalizeString(raw)
  }
}

async function buildAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const accessToken = readWsCacheValue(snapshot, 'ACCESS_TOKEN')
  const tenantId = readWsCacheValue(snapshot, 'tenantId')
  const visitTenantId = readWsCacheValue(snapshot, 'visitTenantId')
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function requestJson(page, headers, method, requestPath) {
  return await page.evaluate(
    async ({ requestUrl, requestMethod, requestHeaders }) => {
      const response = await fetch(requestUrl, { method: requestMethod, headers: requestHeaders })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    {
      requestUrl: `${BASE_URL}${requestPath}`,
      requestMethod: method,
      requestHeaders: headers
    }
  )
}

function assertApiOk(result, label) {
  assert.equal(result.status, 200, `${label} HTTP failed: ${JSON.stringify(result)}`)
  assert.ok([0, 200].includes(result.payload?.code), `${label} API failed: ${JSON.stringify(result.payload)}`)
}

async function listCategories(page, headers) {
  const result = await requestJson(page, headers, 'GET', '/admin-api/dcc/file-categories')
  assertApiOk(result, 'list categories')
  return result.payload.data || []
}

async function listRoutes(page, headers, categoryId) {
  const result = await requestJson(
    page,
    headers,
    'GET',
    `/admin-api/dcc/approval-routes/page?pageNo=1&pageSize=50&categoryId=${categoryId}`
  )
  assertApiOk(result, 'list routes')
  return result.payload.data?.list || []
}

async function selectElementOption(page, triggerLocator, optionText) {
  await triggerLocator.click()
  const option = page.locator('.el-select-dropdown__item:visible:not(.is-disabled)').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function selectFirstElementOption(page, triggerLocator, label) {
  await triggerLocator.click()
  const option = page.locator('.el-popper:visible').last().locator('.el-select-dropdown__item:not(.is-disabled)').first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const selected = (await option.innerText()).trim()
  assert.ok(selected, `${label} option must not be empty`)
  await option.click({ force: true })
  return selected
}

async function selectFirstNonZeroElementOption(page, triggerLocator, label, preferredTexts = []) {
  await triggerLocator.click()
  const options = page.locator('.el-popper:visible').last().locator('.el-select-dropdown__item:not(.is-disabled)')
  await options.first().waitFor({ state: 'visible', timeout: 30000 })
  const count = await options.count()
  for (const preferredText of preferredTexts) {
    for (let index = 0; index < count; index += 1) {
      const option = options.nth(index)
      const text = (await option.innerText()).trim()
      if (text && text.includes(preferredText)) {
        await option.click({ force: true })
        return text
      }
    }
  }
  for (let index = 0; index < count; index += 1) {
    const option = options.nth(index)
    const text = (await option.innerText()).trim()
    if (text && text !== '0' && text !== '-' && !/^0\s*$/.test(text)) {
      await option.click({ force: true })
      return text
    }
  }
  throw new Error(`missing non-zero option: ${label}`)
}

async function fillInputFromFormItem(root, label, value) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  const textControls = item.locator('input, textarea')
  await fillFirstVisible(textControls, value, label)
}

async function selectFromFormItem(page, root, label, optionText) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await selectElementOption(page, item.locator('.el-select, .el-select__wrapper').first(), optionText)
}

async function selectFirstDirectory(page, dialog) {
  const item = dialog.locator('.el-form-item').filter({ hasText: '绑定目录' }).first()
  await item.locator('.el-select__wrapper').first().click()
  const treeOption = page.locator('.el-popper:visible .el-tree-node__content').first()
  if (await treeOption.count()) {
    await treeOption.waitFor({ state: 'visible', timeout: 30000 })
    const text = (await treeOption.innerText()).trim()
    assert.ok(text, 'directory option must not be empty')
    await treeOption.click({ force: true })
    return text
  }
  const option = page.locator('.el-select-dropdown__item:visible:not(.is-disabled)').first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const selected = (await option.innerText()).trim()
  await option.click({ force: true })
  return selected
}


async function selectTaxonomyPath(page, dialog, pathSegments) {
  const cascader = dialog.locator('.el-cascader').first()
  await cascader.waitFor({ state: 'visible', timeout: 30000 })
  await cascader.click()
  for (const segment of pathSegments) {
    const node = page.locator('.el-cascader-node:visible').filter({ hasText: segment }).last()
    await node.waitFor({ state: 'visible', timeout: 30000 })
    await node.click()
  }
}

async function gotoCategories(page) {
  await page.goto(`${BASE_URL}${CATEGORIES_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('类别列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await settle(page)
}

async function createCategoryThroughUi(page) {
  await gotoCategories(page)
  await page.getByRole('button', { name: /新增类别/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增文件类别' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillInputFromFormItem(dialog, '类别编码', CATEGORY_CODE)
  await fillInputFromFormItem(dialog, '类别名称', CATEGORY_NAME)
  await selectTaxonomyPath(page, dialog, CATEGORY_TAXONOMY_PATH)
  const directoryLabel = await selectFirstDirectory(page, dialog)
  await fillInputFromFormItem(dialog, '类别说明', `DCC route operations E2E ${MARKER}`)
  await fillInputFromFormItem(dialog, '来源标识', 'CodexE2E')
  await fillInputFromFormItem(dialog, '备注', `created by ${MARKER}`)
  const [createResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/file-categories') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    dialog.getByRole('button', { name: '确定' }).click()
  ])
  const payload = await createResponse.json().catch(() => null)
  assert.ok(
    createResponse.ok() && payload && [0, 200].includes(payload.code),
    `create category failed: ${JSON.stringify(payload)}`
  )
  await settle(page)
  return { categoryId: payload.data, directoryLabel }
}

async function applyCategoryQuickFilter(page) {
  const filter = page.locator('.table-quick-filter[data-table-key="dcc.controlledFile.permission.categories"]').first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  await filter.locator('.table-quick-filter__field').click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '类别编码' }).first().click()
  await filter.locator('.table-quick-filter__value input').fill(CATEGORY_CODE)
  await filter.getByRole('button', { name: /查询/ }).click()
  await settle(page)
}

async function findCategoryRow(page) {
  await applyCategoryQuickFilter(page)
  const table = page.locator('[data-user-table-key="dcc.controlledFile.permission.categories"]').first()
  const row = table.locator('.el-table__body-wrapper tbody tr').filter({ hasText: CATEGORY_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function deleteCategoryThroughUi(page) {
  await gotoCategories(page)
  const row = await findCategoryRow(page)
  const deleteButton = row.getByRole('button', { name: /删除/ }).first()
  await deleteButton.click()
  const [deleteResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/file-categories/') && response.request().method() === 'DELETE',
      { timeout: 60000 }
    ),
    page.locator('.el-message-box:visible .el-button--primary').click()
  ])
  const payload = await deleteResponse.json().catch(() => null)
  assert.ok(deleteResponse.ok() && payload && [0, 200].includes(payload.code), `delete category failed: ${JSON.stringify(payload)}`)
  await settle(page)
}

async function gotoRoutes(page) {
  await page.goto(`${BASE_URL}${ROUTES_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByRole('button', { name: /新增路线/ }).waitFor({ state: 'visible', timeout: 30000 })
  await settle(page)
}

async function selectRouteCategoryQuickFilter(page, categoryId) {
  const filter = page.locator('.table-quick-filter[data-table-key="dcc.controlledFile.routes.main"]').first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/approval-routes/page') &&
      response.url().includes(`categoryId=${categoryId}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await filter.locator('.table-quick-filter__value').click()
  await filter.locator('.table-quick-filter__value input').fill(CATEGORY_NAME)
  await page.locator('.el-select-dropdown__item:visible:not(.is-disabled)').filter({ hasText: CATEGORY_NAME }).first().click()
  const response = await responsePromise
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `route filter failed: ${JSON.stringify(payload)}`)
  return payload.data?.list || []
}

async function waitForRoutePageRefresh(page, categoryId) {
  const response = await page.waitForResponse(
    (candidate) =>
      candidate.url().includes('/admin-api/dcc/approval-routes/page') &&
      candidate.url().includes(`categoryId=${categoryId}`) &&
      candidate.request().method() === 'GET',
    { timeout: 60000 }
  )
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `route page refresh failed: ${JSON.stringify(payload)}`)
  return payload.data?.list || []
}

async function reloadRouteRows(page, categoryId) {
  const responsePromise = waitForRoutePageRefresh(page, categoryId)
  await page.getByRole('button', { name: /查询路线/ }).click()
  return await responsePromise
}

async function selectRouteCategoryInDialog(page, dialog) {
  const item = dialog.locator('.el-form-item').filter({ hasText: '文件类别' }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await item.locator('.el-select').first().click()
  await item.locator('input').first().fill(CATEGORY_NAME)
  await page.locator('.el-select-dropdown__item:visible:not(.is-disabled)').filter({ hasText: CATEGORY_NAME }).first().click()
}

async function fillRouteDate(dialog) {
  const item = dialog.locator('.el-form-item').filter({ hasText: '生效时间' }).first()
  const input = item.locator('input').first()
  await input.fill('2026-07-14 18:00:00')
  await input.press('Enter').catch(() => undefined)
}

async function fillRouteRemark(dialog, value) {
  const item = dialog.locator('.el-form-item').filter({ hasText: '备注' }).first()
  await item.locator('textarea').first().fill(value)
}

async function fillRouteNodes(page, dialog) {
  while ((await dialog.locator('.el-table__body-wrapper tbody tr').count()) < 4) {
    await dialog.getByRole('button', { name: /新增节点/ }).click()
  }
  const stageNames = ['文控审核', '会签审核', '会签批准', '文控批准']
  const preferredCandidates = ['文控', '编制人直接主管', '财务', '仓储物流', 'QA', '部门负责人']
  const rows = dialog.locator('.el-table__body-wrapper tbody tr')
  for (let index = 0; index < 4; index += 1) {
    const row = rows.nth(index)
    await row.locator('.el-input-number input').first().fill(String(index + 1))
    await row.locator('input[placeholder*="文控审核"], input[placeholder*="例如"]').first().fill(stageNames[index])
    const candidateSelect = row.locator('.el-select').nth(1)
    const candidateLabel = await selectFirstNonZeroElementOption(
      page,
      candidateSelect,
      `candidate row ${index + 1}`,
      preferredCandidates
    )
    assert.notEqual(candidateLabel, '0', `candidate row ${index + 1} must not select placeholder 0`)
    const sortInput = row.locator('.el-input-number input').last()
    await sortInput.fill(String(index + 1))
  }
}

async function saveRouteDialog(page, dialog, categoryId, label) {
  const [saveResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/dcc/approval-routes/${categoryId}`) &&
        response.request().method() === 'PUT',
      { timeout: 60000 }
    ),
    dialog.getByRole('button', { name: /保存路线/ }).click()
  ])
  const payload = await saveResponse.json().catch(() => null)
  assert.ok(saveResponse.ok() && payload && [0, 200].includes(payload.code), `${label} failed: ${JSON.stringify(payload)}`)
  const routeId = payload.data
  assert.ok(routeId, `${label} must return route id`)
  await page.locator('.el-dialog:visible').filter({ hasText: '审批节点' }).waitFor({ state: 'hidden', timeout: 30000 })
  await settle(page)
  return routeId
}

async function createRouteThroughUi(page, categoryId) {
  await gotoRoutes(page)
  await page.getByRole('button', { name: /新增路线/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增路线' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await selectRouteCategoryInDialog(page, dialog)
  await fillRouteDate(dialog)
  await fillRouteRemark(dialog, ROUTE_REMARK)
  await fillRouteNodes(page, dialog)
  return await saveRouteDialog(page, dialog, categoryId, 'create route')
}

async function routeIndexById(routeRows, routeId) {
  const index = routeRows.findIndex((row) => Number(row.id) === Number(routeId))
  assert.notEqual(index, -1, `route id ${routeId} must exist in filtered route rows`)
  return index
}

async function clickRouteAction(page, actionName, rowIndex) {
  const table = page.locator('[data-user-table-key="dcc.controlledFile.routes.main"]').first()
  const action = table.getByRole('button', { name: new RegExp(actionName) }).nth(rowIndex)
  await action.waitFor({ state: 'visible', timeout: 30000 })
  await action.click()
}

async function editRouteThroughUi(page, categoryId, routeId) {
  const routeRows = await selectRouteCategoryQuickFilter(page, categoryId)
  const index = await routeIndexById(routeRows, routeId)
  await clickRouteAction(page, '修改', index)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '审批节点' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillRouteRemark(dialog, ROUTE_UPDATED_REMARK)
  const newRouteId = await saveRouteDialog(page, dialog, categoryId, 'edit route')
  assert.notEqual(Number(newRouteId), Number(routeId), 'edit route is expected to create a new route version')
  return newRouteId
}

async function deleteRouteThroughUi(page, categoryId, routeId) {
  const routeRows = await reloadRouteRows(page, categoryId)
  const index = await routeIndexById(routeRows, routeId)
  await clickRouteAction(page, '删除', index)
  const [deleteResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/dcc/approval-routes/${routeId}`) &&
        response.request().method() === 'DELETE',
      { timeout: 60000 }
    ),
    page.locator('.el-message-box:visible .el-button--primary').click()
  ])
  const payload = await deleteResponse.json().catch(() => null)
  assert.ok(deleteResponse.ok() && payload && [0, 200].includes(payload.code), `delete route failed: ${JSON.stringify(payload)}`)
  await settle(page)
}

async function screenshot(page, name) {
  ensureResultDir()
  const filePath = path.join(RESULT_DIR, `${name}.png`)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function closeVisibleDialogs(page) {
  const dialogs = page.locator('.el-dialog:visible')
  for (let index = await dialogs.count() - 1; index >= 0; index -= 1) {
    const dialog = dialogs.nth(index)
    const closeButton = dialog.locator('.el-dialog__headerbtn').first()
    if (await closeButton.count()) {
      await closeButton.click({ force: true }).catch(() => undefined)
    }
  }
  await page.locator('.el-overlay:visible').waitFor({ state: 'hidden', timeout: 10000 }).catch(() => undefined)
}

async function cleanupCategoryAndRoutesThroughUi(page, headers, categoryId) {
  const existingRoutes = await listRoutes(page, headers, categoryId).catch(() => [])
  if (existingRoutes.length > 0) {
    await gotoRoutes(page)
    await selectRouteCategoryQuickFilter(page, categoryId)
    const routeIds = existingRoutes.map((row) => row.id).filter(Boolean).sort((left, right) => Number(right) - Number(left))
    for (const routeId of routeIds) {
      await deleteRouteThroughUi(page, categoryId, routeId)
    }
  }
  await deleteCategoryThroughUi(page)
}

async function cleanupRoutesThroughUi(page, headers, categoryId) {
  const existingRoutes = await listRoutes(page, headers, categoryId).catch(() => [])
  if (existingRoutes.length === 0) {
    return
  }
  await gotoRoutes(page)
  await selectRouteCategoryQuickFilter(page, categoryId)
  const routeIds = existingRoutes.map((row) => row.id).filter(Boolean).sort((left, right) => Number(right) - Number(left))
  for (const routeId of routeIds) {
    await deleteRouteThroughUi(page, categoryId, routeId)
  }
}

async function main() {
  assertSafeBoundary()
  const browser = await chromium.launch({
    headless: process.env.DCC_ROUTE_OPERATIONS_E2E_HEADED !== '1',
    args: ['--disable-dev-shm-usage'],
    ...(CHROME_EXECUTABLE ? { executablePath: CHROME_EXECUTABLE } : {})
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const writeRequests = []
  const result = {
    status: 'RUNNING',
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME,
    marker: MARKER,
    categoryCode: CATEGORY_CODE,
    categoryName: CATEGORY_NAME,
    writeRequests
  }
  try {
    page.on('request', (request) => {
      const method = request.method()
      const requestUrl = request.url()
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && requestUrl.includes('/admin-api/dcc/')) {
        writeRequests.push({ method, url: requestUrl.replace(/\?.*$/, '') })
      }
    })

    await login(page, CATEGORIES_PATH)
    const headers = await buildAuthHeaders(page)
    result.headersTenantId = headers['tenant-id']

    const beforeCategories = await listCategories(page, headers)
    const existingCategory = beforeCategories.find((item) => item.code === CATEGORY_CODE)
    let categoryId
    let categoryCreatedDuringRun = false
    if (existingCategory?.id) {
      categoryId = existingCategory.id
      result.reusedCategoryId = categoryId
      await cleanupRoutesThroughUi(page, headers, categoryId)
    } else {
      const categoryCreate = await createCategoryThroughUi(page)
      categoryId = categoryCreate.categoryId
      result.directoryLabel = categoryCreate.directoryLabel
      categoryCreatedDuringRun = true
    }
    result.categoryId = categoryId
    result.categoryCreatedDuringRun = categoryCreatedDuringRun
    const categories = await listCategories(page, headers)
    const targetCategory = categories.find((item) => item.id === categoryId)
    assert.equal(targetCategory?.code, CATEGORY_CODE)
    assert.equal(targetCategory?.name, CATEGORY_NAME)
    assert.equal(targetCategory?.active, true)

    const createdRouteId = await createRouteThroughUi(page, categoryId)
    result.createdRouteId = createdRouteId
    await screenshot(page, 'route-created')
    let routeRows = await listRoutes(page, headers, categoryId)
    assert.ok(routeRows.some((row) => Number(row.id) === Number(createdRouteId)), 'created route must exist after save')
    assert.equal(routeRows.length, 1, 'test category should have exactly one route after create')

    const editedRouteId = await editRouteThroughUi(page, categoryId, createdRouteId)
    result.editedRouteId = editedRouteId
    await screenshot(page, 'route-edited')
    routeRows = await listRoutes(page, headers, categoryId)
    assert.ok(routeRows.some((row) => Number(row.id) === Number(createdRouteId)), 'original route version must still exist after edit')
    assert.ok(routeRows.some((row) => Number(row.id) === Number(editedRouteId)), 'edited route version must exist after edit')
    assert.equal(routeRows.length, 2, 'test category should have two route versions after edit')

    await deleteRouteThroughUi(page, categoryId, editedRouteId)
    await screenshot(page, 'route-edited-deleted')
    routeRows = await listRoutes(page, headers, categoryId)
    assert.equal(routeRows.some((row) => Number(row.id) === Number(editedRouteId)), false, 'edited route version must be deleted')
    assert.ok(routeRows.some((row) => Number(row.id) === Number(createdRouteId)), 'single-row delete must not delete original route version')

    await deleteRouteThroughUi(page, categoryId, createdRouteId)
    routeRows = await listRoutes(page, headers, categoryId)
    assert.equal(routeRows.length, 0, 'all test route versions must be removed before category cleanup')

    if (categoryCreatedDuringRun) {
      try {
        await deleteCategoryThroughUi(page)
        await screenshot(page, 'category-cleaned')
        result.categoryCleaned = true
      } catch (cleanupError) {
        result.categoryCleaned = false
        result.categoryCleanupBlocked = cleanupError.message
      }
    } else {
      result.categoryCleaned = 'SKIPPED_REUSED_TEST_CATEGORY'
    }

    const expectedWritePattern = [
      ['PUT', `/admin-api/dcc/approval-routes/${categoryId}`],
      ['PUT', `/admin-api/dcc/approval-routes/${categoryId}`],
      ['DELETE', `/admin-api/dcc/approval-routes/${editedRouteId}`],
      ['DELETE', `/admin-api/dcc/approval-routes/${createdRouteId}`]
    ]
    if (categoryCreatedDuringRun) {
      expectedWritePattern.unshift(
        ['POST', '/admin-api/dcc/file-categories'],
        ['PUT', `/admin-api/dcc/file-categories/${categoryId}/directory-binding`]
      )
    }
    for (const [method, urlPart] of expectedWritePattern) {
      assert.ok(
        writeRequests.some((request) => request.method === method && request.url.includes(urlPart)),
        `expected write request missing: ${method} ${urlPart}`
      )
    }

    result.status = 'PASS'
    result.finalRouteCount = routeRows.length
    writeResult(result)
    console.log(
      `PASS: DCC route operations real E2E marker=${MARKER} categoryId=${categoryId} createdRouteId=${createdRouteId} editedRouteId=${editedRouteId}`
    )
  } catch (error) {
    result.status = 'FAIL'
    result.error = error.message
    await screenshot(page, 'failure').catch(() => undefined)
    if (result.categoryId) {
      await closeVisibleDialogs(page)
      const headers = await buildAuthHeaders(page).catch(() => undefined)
      if (headers) {
        try {
          await cleanupRoutesThroughUi(page, headers, result.categoryId)
          result.failureCleanup = 'PASS'
        } catch (cleanupError) {
          result.failureCleanup = 'FAIL'
          result.failureCleanupError = cleanupError.message
        }
      }
    }
    writeResult(result)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
