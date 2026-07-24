const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_CATEGORY_STAGE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_CATEGORY_STAGE_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_CATEGORY_STAGE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_CATEGORY_STAGE_E2E_PASSWORD || '111111'
const ALLOW_TEST_WRITE = process.env.DCC_CATEGORY_STAGE_E2E_ALLOW_TEST_WRITE === '1'
const CATEGORY_CODE =
  process.env.DCC_CATEGORY_STAGE_E2E_CODE || `CODEX_STAGE_E2E_${Date.now().toString(36).toUpperCase()}`
const CATEGORY_NAME = process.env.DCC_CATEGORY_STAGE_E2E_NAME || `阶段E2E临时类别${Date.now()}`
const CHROME_EXECUTABLE = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
const TAXONOMY_ROOT_NAME = '\u6280\u672f\u6587\u6863'
const INPUT_STAGE_NAME = '\u8bbe\u8ba1\u548c\u5f00\u53d1\u8f93\u5165\u9636\u6bb5'
const OUTPUT_STAGE_NAME = '\u8bbe\u8ba1\u548c\u5f00\u53d1\u8f93\u51fa\u9636\u6bb5'
const INPUT_LEAF_NAME = 'Codex\u8f93\u5165E2E\u53f6\u5b50'
const OUTPUT_LEAF_NAME = 'Codex\u8f93\u51faE2E\u53f6\u5b50'
const CATEGORY_TABLE_KEY = 'dcc.controlledFile.permission.categories'
const TAXONOMY_TABLE_KEY = 'dcc.fileTypeTaxonomy.main'
const TAXONOMY_CODE_SUFFIX = Date.now().toString(36).toUpperCase()

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/)
  assert.equal(TENANT, '测试租户')
  assert.equal(USERNAME, 'aoteman')
  assert.equal(ALLOW_TEST_WRITE, true, 'Set DCC_CATEGORY_STAGE_E2E_ALLOW_TEST_WRITE=1 for test-tenant write verification')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(600)
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

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'commit',
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
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }
  const usernameInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  if (await usernameInput.count()) {
    await usernameInput.fill('')
    await usernameInput.fill(USERNAME)
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入用户名"]'), USERNAME, 'username')
  }
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
  await page.goto(`${BASE_URL}/dcc/controlled-file/categories`, { waitUntil: 'commit', timeout: 60000 })
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) {
    return ''
  }
  const normalizeString = (value) => {
    let current = value || ''
    for (let index = 0; index < 3; index += 1) {
      const trimmed = String(current).trim()
      if (!trimmed.startsWith('"')) {
        return trimmed
      }
      try {
        const parsed = JSON.parse(trimmed)
        if (typeof parsed !== 'string' || parsed === current) {
          return trimmed.replace(/^"(.*)"$/, '$1')
        }
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
  if (visitTenantId) {
    headers['visit-tenant-id'] = String(visitTenantId)
  }
  return headers
}

async function requestJson(page, headers, method, path, data) {
  return await page.evaluate(
    async ({ requestUrl, requestMethod, requestHeaders, requestData }) => {
      const response = await fetch(requestUrl, {
        method: requestMethod,
        headers: {
          ...requestHeaders,
          ...(requestData === undefined ? {} : { 'Content-Type': 'application/json' })
        },
        body: requestData === undefined ? undefined : JSON.stringify(requestData)
      })
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
      requestUrl: `${BASE_URL}${path}`,
      requestMethod: method,
      requestHeaders: headers,
      requestData: data
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

async function listFileTypeTaxonomies(page, headers) {
  const result = await requestJson(page, headers, 'GET', '/admin-api/dcc/file-type-taxonomies')
  assertApiOk(result, 'list file type taxonomies')
  return result.payload.data || []
}

function resolveTaxonomyPathForStage(rows, stageName) {
  const activeRows = rows.filter((item) => item && item.active !== false)
  const byId = new Map(activeRows.map((item) => [Number(item.id), item]))
  const childrenByParent = new Map()
  for (const item of activeRows) {
    const parentId = item.parentId == null ? 0 : Number(item.parentId)
    if (!childrenByParent.has(parentId)) {
      childrenByParent.set(parentId, [])
    }
    childrenByParent.get(parentId).push(item)
  }
  for (const children of childrenByParent.values()) {
    children.sort((a, b) => Number(a.sort || 0) - Number(b.sort || 0) || String(a.name).localeCompare(String(b.name), 'zh-CN'))
  }
  const pathTo = (item) => {
    const names = []
    let current = item
    const seen = new Set()
    while (current?.id && !seen.has(Number(current.id))) {
      seen.add(Number(current.id))
      names.unshift(String(current.name))
      current = current.parentId == null ? undefined : byId.get(Number(current.parentId))
    }
    return names
  }
  const deepestLeafBelow = (stage) => {
    let best = stage
    const stack = [...(childrenByParent.get(Number(stage.id)) || [])]
    while (stack.length) {
      const current = stack.shift()
      best = current
      stack.unshift(...(childrenByParent.get(Number(current.id)) || []))
    }
    return best
  }
  const stage = activeRows
    .filter((item) => String(item.name).trim() === stageName)
    .find((item) => pathTo(item)[0] === TAXONOMY_ROOT_NAME)
  assert.ok(stage, `missing active taxonomy stage: ${stageName}`)
  const leaf = deepestLeafBelow(stage)
  const path = pathTo(leaf)
  assert.ok(path.includes(stageName), `resolved taxonomy path must include ${stageName}: ${path.join(' / ')}`)
  return path
}

function resolveTaxonomyPaths(rows) {
  return {
    input: resolveTaxonomyPathForStage(rows, INPUT_STAGE_NAME),
    output: resolveTaxonomyPathForStage(rows, OUTPUT_STAGE_NAME)
  }
}

function findTaxonomyByNameAndParent(rows, name, parentId) {
  const normalizedParentId = parentId == null ? 0 : Number(parentId)
  return rows.find(
    (item) =>
      item &&
      item.active !== false &&
      String(item.name).trim() === name &&
      (item.parentId == null ? 0 : Number(item.parentId)) === normalizedParentId
  )
}

async function createTaxonomyNodeThroughUi(page, parentRow, name, code) {
  await page.goto(`${BASE_URL}/mdm/file-type-taxonomy`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('DCC\u6587\u4ef6\u5206\u7c7b', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  if (parentRow) {
    await applyTaxonomyKeywordFilter(page, String(parentRow.name))
    const parentTableRow = taxonomyRows(page).filter({ hasText: String(parentRow.name) }).first()
    await parentTableRow.waitFor({ state: 'visible', timeout: 30000 })
    await parentTableRow.getByRole('button', { name: /新增下级/ }).click()
  } else {
    await page.getByRole('button', { name: /新增一级/ }).click()
  }
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增文件分类' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  if (parentRow) {
    await fillInputFromFormItem(dialog, '\u5206\u7c7b\u7f16\u7801', code)
  }
  await fillInputFromFormItem(dialog, '\u5206\u7c7b\u540d\u79f0', name)
  const [response] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/file-type-taxonomies') && response.request().method() === 'POST',
      { timeout: 30000 }
    ),
    dialog.getByRole('button', { name: '保存' }).click()
  ])
  const payload = await response.json().catch(() => null)
  assert.ok(
    response.ok() && payload && [0, 200].includes(payload.code),
    `create taxonomy failed: status=${response.status()} request=${response.request().postData()} response=${JSON.stringify(payload)}`
  )
  await settle(page)
}

async function ensureTaxonomySeedThroughUi(page, headers) {
  let rows = await listFileTypeTaxonomies(page, headers)
  let root = findTaxonomyByNameAndParent(rows, TAXONOMY_ROOT_NAME, 0)
  if (!root) {
    await createTaxonomyNodeThroughUi(page, null, TAXONOMY_ROOT_NAME, '')
    rows = await listFileTypeTaxonomies(page, headers)
    root = findTaxonomyByNameAndParent(rows, TAXONOMY_ROOT_NAME, 0)
  }
  assert.ok(root?.id, 'taxonomy root must exist after UI seed')

  const seeds = [
    {
      stageName: INPUT_STAGE_NAME,
      leafName: INPUT_LEAF_NAME,
      stageCode: `CODEX_E2E_IN_STAGE_${TAXONOMY_CODE_SUFFIX}`,
      leafCode: `CODEX_E2E_IN_LEAF_${TAXONOMY_CODE_SUFFIX}`
    },
    {
      stageName: OUTPUT_STAGE_NAME,
      leafName: OUTPUT_LEAF_NAME,
      stageCode: `CODEX_E2E_OUT_STAGE_${TAXONOMY_CODE_SUFFIX}`,
      leafCode: `CODEX_E2E_OUT_LEAF_${TAXONOMY_CODE_SUFFIX}`
    }
  ]
  for (const seed of seeds) {
    let stage = findTaxonomyByNameAndParent(rows, seed.stageName, root.id)
    if (!stage) {
      await createTaxonomyNodeThroughUi(page, root, seed.stageName, seed.stageCode)
      rows = await listFileTypeTaxonomies(page, headers)
      stage = findTaxonomyByNameAndParent(rows, seed.stageName, root.id)
    }
    assert.ok(stage?.id, `taxonomy stage must exist after UI seed: ${seed.stageName}`)
    let leaf = findTaxonomyByNameAndParent(rows, seed.leafName, stage.id)
    if (!leaf) {
      await createTaxonomyNodeThroughUi(page, stage, seed.leafName, seed.leafCode)
      rows = await listFileTypeTaxonomies(page, headers)
      leaf = findTaxonomyByNameAndParent(rows, seed.leafName, stage.id)
    }
    assert.ok(leaf?.id, `taxonomy leaf must exist after UI seed: ${seed.leafName}`)
  }
  return resolveTaxonomyPaths(rows)
}

async function cleanupCategory(page, headers, code) {
  const existing = (await listCategories(page, headers)).find((item) => item.code === code)
  if (existing?.id) {
    const result = await requestJson(page, headers, 'DELETE', `/admin-api/dcc/file-categories/${existing.id}`)
    assertApiOk(result, 'cleanup category')
  }
}

async function selectOptionFromFormItem(page, root, label, optionText) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  await item.locator('.el-select').first().click()
  await page.locator('.el-popper:visible').last().getByText(optionText, { exact: true }).click()
}

function categoryQuickFilter(page) {
  return page.locator(`.table-quick-filter[data-table-key="${CATEGORY_TABLE_KEY}"]`).first()
}

function categoryRows(page) {
  return page.locator(`[data-user-table-key="${CATEGORY_TABLE_KEY}"] .el-table__body-wrapper tbody tr`)
}

function taxonomyQuickFilter(page) {
  return page.locator(`.table-quick-filter[data-table-key="${TAXONOMY_TABLE_KEY}"]`).first()
}

function taxonomyRows(page) {
  return page.locator(`[data-user-table-key="${TAXONOMY_TABLE_KEY}"] .el-table__body-wrapper tbody tr`)
}

async function selectQuickFilterField(page, fieldLabel) {
  const quickFilter = categoryQuickFilter(page)
  await quickFilter.waitFor({ state: 'visible', timeout: 30000 })
  await quickFilter.locator('.table-quick-filter__field').click()
  await page.locator('.el-popper:visible').last().getByText(fieldLabel, { exact: true }).click()
  await settle(page)
}

async function applyTextQuickFilter(page, fieldLabel, value) {
  const quickFilter = categoryQuickFilter(page)
  await selectQuickFilterField(page, fieldLabel)
  await fillFirstVisible(quickFilter.locator('.table-quick-filter__value input.el-input__inner'), value, fieldLabel)
  await Promise.all([
    page.waitForTimeout(300),
    quickFilter.getByRole('button', { name: /查询/ }).click()
  ])
  await settle(page)
}

async function applySelectQuickFilter(page, fieldLabel, valueText) {
  const quickFilter = categoryQuickFilter(page)
  await selectQuickFilterField(page, fieldLabel)
  await quickFilter.locator('.table-quick-filter__value').click()
  await page.locator('.el-popper:visible').last().getByText(valueText, { exact: true }).click()
  await Promise.all([
    page.waitForTimeout(300),
    quickFilter.getByRole('button', { name: /查询/ }).click()
  ])
  await settle(page)
}

async function applyTaxonomyKeywordFilter(page, keyword) {
  const quickFilter = taxonomyQuickFilter(page)
  await quickFilter.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(quickFilter.locator('.table-quick-filter__value input.el-input__inner'), keyword, 'taxonomy keyword')
  await Promise.all([
    page.waitForTimeout(300),
    quickFilter.getByRole('button', { name: /查询/ }).click()
  ])
  await settle(page)
}

async function fillInputFromFormItem(root, label, value) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  await fillFirstVisible(item.locator('input'), value, label)
}

async function selectFirstDirectory(page, dialog) {
  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      const currentDialog = page.locator('.el-dialog:visible').filter({ hasText: '新增文件类别' }).last()
      const item = (await currentDialog.count())
        ? currentDialog.locator('.el-form-item').filter({ hasText: '绑定目录' }).first()
        : dialog.locator('.el-form-item').filter({ hasText: '绑定目录' }).first()
      await item.waitFor({ state: 'attached', timeout: 30000 })
      await item.scrollIntoViewIfNeeded({ timeout: 10000 })
      await page.waitForTimeout(500)
      await item.locator('.el-select__wrapper').first().click({ force: true, timeout: 10000 })
      const option = page
        .locator(
          '.el-popper:visible.el-tree-select__popper .el-select-dropdown__item, .el-popper:visible .el-tree-select__popper .el-select-dropdown__item'
        )
        .first()
      await option.waitFor({ state: 'visible', timeout: 10000 })
      await option.click()
      return
    } catch (error) {
      await page.keyboard.press('Escape').catch(() => undefined)
      if (attempt === 2) {
        throw error
      }
    }
  }
}


async function selectTaxonomyPath(page, dialog, pathSegments) {
  const cascader = dialog.locator('.el-cascader').first()
  await cascader.waitFor({ state: 'visible', timeout: 30000 })
  const input = cascader.locator('input').first()
  const leafName = pathSegments[pathSegments.length - 1]
  await input.click({ force: true })
  await input.press('Control+A').catch(() => undefined)
  await input.press('Backspace').catch(() => undefined)
  await page.waitForTimeout(300)
  await cascader.click({ force: true })
  for (const [index, segment] of pathSegments.entries()) {
    const node = page.locator('.el-cascader-panel:visible .el-cascader-node').filter({ hasText: segment }).first()
    await node.waitFor({ state: 'visible', timeout: 30000 })
    if (index === pathSegments.length - 1) {
      const selector = node.locator('.el-radio__input, .el-checkbox__input').first()
      if (await selector.count()) {
        await selector.click({ force: true })
      } else {
        await node.click()
      }
    } else {
      await node.click()
    }
    await page.waitForTimeout(250)
  }
  await page.waitForFunction(
    (leaf) =>
      Array.from(document.querySelectorAll('.el-dialog input')).some((item) =>
        ((item).value || '').includes(leaf)
      ),
    leafName,
    { timeout: 30000 }
  )
  await dialog.locator('.el-dialog__header').click({ force: true }).catch(() => undefined)
}

async function verifyInputStageFilter(page) {
  await applySelectQuickFilter(page, '\u9636\u6bb5', INPUT_STAGE_NAME)
  const row = categoryRows(page).filter({ hasText: CATEGORY_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const text = await row.innerText()
  assert.match(text, new RegExp(INPUT_STAGE_NAME), `filtered row must stay in input stage: ${text}`)
}

async function createCategoryThroughUi(page, taxonomyPath) {
  await page.getByRole('button', { name: /新增类别/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增文件类别' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillInputFromFormItem(dialog, '类别编码', CATEGORY_CODE)
  await fillInputFromFormItem(dialog, '类别名称', CATEGORY_NAME)
  await selectFirstDirectory(page, dialog)
  await selectTaxonomyPath(page, dialog, taxonomyPath)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/dcc/file-categories') && response.request().method() === 'POST',
    { timeout: 30000 }
  ).catch(async (error) => {
    const dialogText = await dialog.innerText().catch(() => '')
    throw new Error(`create category request timeout: ${error.message}; dialog=${dialogText}`)
  })
  await dialog.getByRole('button', { name: '确定' }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(
    response.ok() && payload && [0, 200].includes(payload.code),
    `create category failed: status=${response.status()} request=${response.request().postData()} response=${JSON.stringify(payload)}`
  )
  assert.match(response.request().postData() || '', /"lifecycleStage":""/)
  assert.match(response.request().postData() || '', /"fileTypeTaxonomyId":/)
  await settle(page)
}

async function editCategoryThroughUi(page, taxonomyPath) {
  await applyTextQuickFilter(page, '\u7c7b\u522b\u7f16\u7801', CATEGORY_CODE)
  const row = categoryRows(page).filter({ hasText: CATEGORY_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.getByRole('button', { name: /编辑/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '编辑文件类别' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await selectTaxonomyPath(page, dialog, taxonomyPath)
  const [response] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/file-categories/') && response.request().method() === 'PUT',
      { timeout: 30000 }
    ),
    dialog.getByRole('button', { name: '确定' }).click()
  ])
  const payload = await response.json().catch(() => null)
  assert.ok(
    response.ok() && payload && [0, 200].includes(payload.code),
    `update category failed: status=${response.status()} request=${response.request().postData()} response=${JSON.stringify(payload)}`
  )
  assert.match(response.request().postData() || '', /"lifecycleStage":""/)
  assert.match(response.request().postData() || '', /"fileTypeTaxonomyId":/)
  await settle(page)
}

async function deleteCategoryThroughUi(page) {
  await applyTextQuickFilter(page, '\u7c7b\u522b\u7f16\u7801', CATEGORY_CODE)
  const row = categoryRows(page).filter({ hasText: CATEGORY_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.getByRole('button', { name: /删除/ }).click()
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/file-categories/') && response.request().method() === 'DELETE',
      { timeout: 30000 }
    ),
    page.locator('.el-message-box:visible .el-button--primary').click()
  ])
  await settle(page)
}

async function main() {
  assertSafeBoundary()
  const browser = await chromium.launch({
    headless: process.env.DCC_CATEGORY_STAGE_E2E_HEADED !== '1',
    args: ['--disable-dev-shm-usage'],
    ...(CHROME_EXECUTABLE ? { executablePath: CHROME_EXECUTABLE } : {})
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(page)
    await page.goto(`${BASE_URL}/dcc/controlled-file/categories`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('类别列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    const headers = await buildAuthHeaders(page)
    const taxonomyPaths = await ensureTaxonomySeedThroughUi(page, headers)
    await page.goto(`${BASE_URL}/dcc/controlled-file/categories`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('类别列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    await cleanupCategory(page, headers, CATEGORY_CODE)
    await createCategoryThroughUi(page, taxonomyPaths.input)
    let category = (await listCategories(page, headers)).find((item) => item.code === CATEGORY_CODE)
    assert.equal(category?.lifecycleStage, 'INPUT')
    await verifyInputStageFilter(page)
    await editCategoryThroughUi(page, taxonomyPaths.output)
    category = (await listCategories(page, headers)).find((item) => item.code === CATEGORY_CODE)
    assert.equal(category?.lifecycleStage, 'OUTPUT')
    console.log(
      `PASS: DCC category lifecycle stage real E2E code=${CATEGORY_CODE} categoryId=${category?.id} tenant=${TENANT} username=${USERNAME}`
    )
  } finally {
    await cleanupCategory(page, await buildAuthHeaders(page).catch(() => ({})), CATEGORY_CODE).catch(() => undefined)
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
