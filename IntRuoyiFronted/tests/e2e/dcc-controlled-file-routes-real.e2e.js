const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_ROUTES_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_ROUTES_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_ROUTES_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_ROUTES_E2E_PASSWORD
const TARGET_PATH = '/dcc/controlled-file/routes'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'dcc-controlled-file-routes')
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'Yudao', 'YUDAO'])
const READ_ONLY_DCC_POST_PATHS = ['/admin-api/dcc/approval-routes/preview']

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(path.join(RESULT_DIR, 'real-e2e-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function requirePrerequisites() {
  assert.equal(TENANT, '测试租户', 'DCC routes real E2E must use 测试租户')
  assert.equal(USERNAME, 'aoteman', 'DCC routes real E2E must use aoteman')
  assert.ok(PASSWORD, 'DCC_ROUTES_E2E_PASSWORD is required')
  assert.equal(FORBIDDEN_TENANTS.has(TENANT), false, 'Real E2E must not target a protected tenant')
}

async function login(page) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', TARGET_PATH)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

function assertRouteRows(payload, label) {
  assert.ok([0, 200].includes(payload.code), `${label} business code ${payload.code}`)
  const rows = payload.data?.list || []
  assert.ok((payload.data?.total ?? 0) > 0, `${label} must return real rows`)
  assert.ok(rows.length > 0, `${label} must return visible page rows`)
  for (const row of rows) {
    assert.ok(row.categoryId, `${label} row must include categoryId`)
    assert.ok(row.categoryName, `${label} row must include categoryName`)
    assert.ok(row.statusLabel, `${label} row must include statusLabel`)
    assert.ok(row.nodeCount > 0, `${label} row must include nodeCount`)
    assert.ok(row.nodeSummary && row.nodeSummary.includes('：'), `${label} row must include readable nodeSummary`)
  }
  return rows
}

async function waitForRoutePageResponse(page, predicate = () => true) {
  const response = await page.waitForResponse(
    (candidate) =>
      candidate.url().includes('/dcc/approval-routes/page') &&
      candidate.request().method() === 'GET' &&
      predicate(candidate),
    { timeout: 60000 }
  )
  assert.equal(response.ok(), true, `route page HTTP status ${response.status()}`)
  return response.json()
}

async function assertNodeColumnsVisible(page, table) {
  for (const stageNo of [1, 2, 3, 4]) {
    await page.getByText(`节点${stageNo}`, { exact: true }).first().waitFor({ state: 'visible' })
  }
  assert.equal(await page.getByText('路线摘要', { exact: true }).count(), 0, 'route summary column must be hidden')
  assert.equal(await page.getByText('备注', { exact: true }).count(), 0, 'remark column must be hidden')
  const nodeCellTexts = await table.locator('.route-node-assignees').evaluateAll((nodes) =>
    nodes.map((node) => node.textContent?.trim() || '').filter(Boolean)
  )
  assert.ok(nodeCellTexts.length >= 4, 'node assignee cells must be rendered')
  assert.ok(nodeCellTexts.some((text) => text !== '-'), 'node assignee cells must show real assignees')
  return nodeCellTexts
}

async function chooseCategoryFilter(page, categoryName) {
  const quickFilter = page
    .locator('.table-quick-filter[data-table-key="dcc.controlledFile.routes.main"]')
    .first()
  await quickFilter.waitFor({ state: 'visible' })
  await quickFilter.locator('.table-quick-filter__value').click()
  const option = page
    .locator('.el-select-dropdown__item:visible:not(.is-disabled)')
    .filter({ hasText: categoryName })
    .first()
  const selectedLabel = (await option.innerText()).trim()
  await option.click()
  assert.ok(selectedLabel.includes(categoryName), 'category filter option label must match the selected route row')
  return selectedLabel
}

async function main() {
  requirePrerequisites()
  const launchOptions = { headless: true, args: ['--disable-dev-shm-usage'] }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const writeRequests = []
  let lastRoutePayload = null
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)

    page.on('request', (request) => {
      const method = request.method()
      const requestUrl = request.url()
      const isReadOnlyDccPost = method === 'POST' && READ_ONLY_DCC_POST_PATHS.some((path) => requestUrl.includes(path))
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && !isReadOnlyDccPost && requestUrl.includes('/admin-api/dcc/')) {
        writeRequests.push({ method, url: request.url() })
      }
    })

    const initialResponsePromise = waitForRoutePageResponse(page)
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    const initialPayload = await initialResponsePromise
    lastRoutePayload = initialPayload
    const initialRows = assertRouteRows(initialPayload, 'initial route list')

    const table = page.locator('[data-user-table-key="dcc.controlledFile.routes.main"]').first()
    await table.waitFor({ state: 'visible' })
    await page.getByText('文件类别', { exact: false }).first().waitFor({ state: 'visible' })
    await page.getByText(initialRows[0].categoryName, { exact: false }).first().waitFor({ state: 'visible' })
    const initialNodeCellTexts = await assertNodeColumnsVisible(page, table)

    const filteredResponsePromise = waitForRoutePageResponse(page, (response) =>
      response.url().includes(`categoryId=${initialRows[0].categoryId}`)
    )
    const selectedCategoryLabel = await chooseCategoryFilter(page, initialRows[0].categoryName)
    const filteredPayload = await filteredResponsePromise
    lastRoutePayload = filteredPayload
    const filteredRows = assertRouteRows(filteredPayload, 'filtered route list')
    const filteredCategoryIds = new Set(filteredRows.map((row) => row.categoryId))
    assert.equal(filteredCategoryIds.size, 1, 'category filter must return one category only')
    const filteredNodeCellTexts = await assertNodeColumnsVisible(page, table)
    assert.deepEqual(writeRequests, [], 'routes real E2E must not send DCC write requests')

    const result = {
      status: 'PASS',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetPath: TARGET_PATH,
      initialTotal: initialPayload.data.total,
      initialRows: initialRows.length,
      firstRoute: {
        categoryId: initialRows[0].categoryId,
        categoryName: initialRows[0].categoryName,
        nodeCount: initialRows[0].nodeCount,
        nodeSummary: initialRows[0].nodeSummary
      },
      initialNodeCellTexts,
      selectedCategoryLabel,
      filteredTotal: filteredPayload.data.total,
      filteredRows: filteredRows.length,
      filteredCategoryId: filteredRows[0].categoryId,
      filteredNodeCellTexts,
      writeRequests
    }
    writeResult(result)
    console.log(
      `PASS: dcc routes real E2E, initialTotal=${result.initialTotal}, filteredTotal=${result.filteredTotal}`
    )
  } catch (error) {
    writeResult({
      status: 'FAIL',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetPath: TARGET_PATH,
      error: error.message,
      lastRoutePayload,
      writeRequests
    })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
