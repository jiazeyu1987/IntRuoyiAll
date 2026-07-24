const assert = require('node:assert/strict')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      "Playwright is required for MES route product copy real E2E. Run in yudao-ui-admin-vue3 where 'playwright' is installed."
    )
  }
}

const zh = {
  tenant: '\u6d4b\u8bd5\u79df\u6237',
  routeCode: '\u8def\u7ebf\u7f16\u7801',
  productTab: '\u5173\u8054\u4ea7\u54c1',
  edit: '\u7f16\u8f91',
  copy: '\u590d\u5236',
  confirm: '\u786e \u5b9a',
  success: '\u590d\u5236\u6210\u529f',
  materialDialog: '\u7269\u6599\u4ea7\u54c1\u9009\u62e9',
  materialCode: '\u7269\u6599\u7f16\u7801'
}

const config = {
  baseUrl: (process.env.MES_ROUTE_PRODUCT_COPY_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  apiBaseUrl: (
    process.env.MES_ROUTE_PRODUCT_COPY_E2E_API_BASE_URL || 'http://127.0.0.1:48081/admin-api'
  ).replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_PRODUCT_COPY_E2E_TENANT || zh.tenant,
  username: process.env.MES_ROUTE_PRODUCT_COPY_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_PRODUCT_COPY_E2E_PASSWORD || '111111',
  headed: process.env.MES_ROUTE_PRODUCT_COPY_E2E_HEADED === '1',
  routeKeyword: process.env.MES_ROUTE_PRODUCT_COPY_E2E_ROUTE_KEYWORD || '',
  targetItemCode: process.env.MES_ROUTE_PRODUCT_COPY_E2E_TARGET_ITEM_CODE || ''
}

function apiUrl(pathname, params = {}) {
  const url = new URL(`${config.apiBaseUrl}${pathname}`)
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  return url
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

function readCacheValue(page, key) {
  return page.evaluate((cacheKey) => {
    const raw = localStorage.getItem(cacheKey)
    if (!raw) {
      return undefined
    }
    const parseNested = (value) => {
      try {
        return JSON.parse(value)
      } catch (error) {
        return value
      }
    }
    try {
      const parsed = JSON.parse(raw)
      if (parsed && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
        return parseNested(parsed.v)
      }
      if (parsed && Object.prototype.hasOwnProperty.call(parsed, 'value')) {
        return parseNested(parsed.value)
      }
      return parsed
    } catch (error) {
      return raw
    }
  }, key)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if (
    (await loginForm
      .locator('.verify-img-panel, .verify-bar-area, input[placeholder="\u8bf7\u8f93\u5165\u9a8c\u8bc1\u7801"]')
      .count()) > 0
  ) {
    throw new Error('Login captcha is enabled; real E2E cannot continue without manual input.')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input.el-input__inner').first(), config.tenant, 'tenant')
  }
  await fillFirstVisible(
    loginForm.locator('input.el-input__inner:not([role="combobox"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(loginForm.locator('input[type="password"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '\u767b\u5f55' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  if (!loginResponse.ok() || ![0, 200].includes(loginPayload.code)) {
    throw new Error(`Login failed: HTTP ${loginResponse.status()} ${loginPayload.msg || loginPayload.code}`)
  }
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function authContext(page) {
  const accessToken = await readCacheValue(page, 'ACCESS_TOKEN')
  const tenantId = await readCacheValue(page, 'tenantId')
  if (!accessToken || !tenantId) {
    throw new Error('Logged-in context is missing ACCESS_TOKEN or tenantId.')
  }
  return { accessToken, tenantId }
}

async function apiGet(page, pathname, params = {}) {
  const { accessToken, tenantId } = await authContext(page)
  const url = apiUrl(pathname, params)
  const response = await page.request.get(url.toString(), {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'tenant-id': String(tenantId)
    }
  })
  const body = await response.json().catch(async () => ({
    code: -1,
    msg: await response.text()
  }))
  if (!response.ok() || ![0, 200].includes(body.code)) {
    throw new Error(`GET ${pathname} failed: HTTP ${response.status()} ${body.msg || body.code}`)
  }
  return body.data
}

async function getAllRouteProducts(page, routes) {
  const routeProducts = new Map()
  for (const route of routes) {
    const products = await apiGet(page, '/mes/pro/route-product/list-by-route', { routeId: route.id })
    routeProducts.set(route.id, products)
  }
  return routeProducts
}

async function findCandidate(page) {
  const routesPage = await apiGet(page, '/mes/pro/route/page', {
    pageNo: 1,
    pageSize: 100,
    code: config.routeKeyword
  })
  const routes = (routesPage.list || []).filter((route) => route.id)
  console.log(
    JSON.stringify({
      phase: 'route-scan',
      routes: routes.map((route) => ({
        id: route.id,
        code: route.code,
        status: route.status,
        productCodes: route.productCodes || ''
      }))
    })
  )
  if (routes.length === 0) {
    throw new Error('No route found in real test tenant data.')
  }

  const routeProductsByRouteId = await getAllRouteProducts(page, routes)
  const routeProductItemIds = new Set()
  let sourceRoute
  let sourceProduct
  let sourceBomList = []

  for (const route of routes) {
    const products = routeProductsByRouteId.get(route.id) || []
    for (const product of products) {
      if (product.itemId) {
        routeProductItemIds.add(product.itemId)
      }
      if (product.id && product.itemId) {
        const bomList = await apiGet(page, '/mes/pro/route-product-bom/list', {
          routeId: route.id,
          productId: product.itemId
        })
        if (!sourceRoute || bomList.length > sourceBomList.length) {
          sourceRoute = route
          sourceProduct = product
          sourceBomList = bomList || []
        }
      }
    }
  }

  if (!sourceRoute || !sourceProduct) {
    throw new Error('No route product row found for copy source in real test tenant data.')
  }
  if (sourceBomList.length === 0) {
    throw new Error('No route product with BOM rows found in real test tenant data.')
  }

  let targetItem
  if (config.targetItemCode) {
    const itemPage = await apiGet(page, '/mes/md/item/page', {
      pageNo: 1,
      pageSize: 10,
      code: config.targetItemCode,
      status: 0
    })
    targetItem = (itemPage.list || []).find((item) => item.code === config.targetItemCode)
    if (!targetItem) {
      throw new Error(`Configured target item not found or disabled: ${config.targetItemCode}`)
    }
    if (routeProductItemIds.has(targetItem.id)) {
      throw new Error(`Configured target item is already associated to a route: ${config.targetItemCode}`)
    }
  } else {
    for (let pageNo = 1; pageNo <= 20 && !targetItem; pageNo += 1) {
      const itemPage = await apiGet(page, '/mes/md/item/page', {
        pageNo,
        pageSize: 100,
        status: 0
      })
      const items = itemPage.list || []
      targetItem = items.find((item) => item.id && !routeProductItemIds.has(item.id))
      if (items.length < 100) {
        break
      }
    }
  }

  if (!targetItem) {
    throw new Error('No enabled unassociated target item found in real test tenant data.')
  }

  return { route: sourceRoute, sourceProduct, sourceBomList, targetItem }
}

async function openRouteEditProductTab(page, route) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await page.getByText(zh.routeCode, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('input[placeholder="\u8bf7\u8f93\u5165\u5de5\u827a\u8def\u7ebf\u7f16\u7801"]').fill(route.code)
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: /\u641c\u7d22/ }).first().click()
  ])
  await settle(page)

  const routeRow = page.locator('.el-table__body-wrapper tr.el-table__row').filter({ hasText: route.code }).first()
  await routeRow.waitFor({ state: 'visible', timeout: 30000 })
  await routeRow.getByRole('button', { name: zh.edit }).click()
  await page.getByRole('dialog').filter({ hasText: '\u7f16\u8f91\u5de5\u827a\u8def\u7ebf' }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await page.getByRole('tab', { name: zh.productTab }).click()
  await page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/route-product/list-by-route') && response.status() === 200,
    { timeout: 60000 }
  ).catch(() => {})
  await settle(page)
}

async function searchRouteRow(page, route) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await page.getByText(zh.routeCode, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('input[placeholder="\u8bf7\u8f93\u5165\u5de5\u827a\u8def\u7ebf\u7f16\u7801"]').fill(route.code)
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: /\u641c\u7d22/ }).first().click()
  ])
  await settle(page)
  const routeRow = page.locator('.el-table__body-wrapper tr.el-table__row').filter({ hasText: route.code }).first()
  await routeRow.waitFor({ state: 'visible', timeout: 30000 })
  return routeRow
}

async function confirmMessageBox(page) {
  const messageBox = page.locator('.el-message-box').last()
  await messageBox.waitFor({ state: 'visible', timeout: 30000 })
  await messageBox.getByRole('button', { name: /\u786e\u5b9a|\u786e\u8ba4/ }).click()
  await messageBox.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => {})
}

async function setRouteEnabledByUi(page, route, enabled) {
  const routeRow = await searchRouteRow(page, route)
  const routeSwitch = routeRow.locator('.el-switch').first()
  const switchClass = (await routeSwitch.getAttribute('class')) || ''
  const currentlyEnabled = switchClass.includes('is-checked')
  if (currentlyEnabled === enabled) {
    return false
  }
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route/update-status') && response.status() === 200,
      { timeout: 60000 }
    ),
    (async () => {
      await routeSwitch.click()
      await confirmMessageBox(page)
    })()
  ])
  await settle(page)
  const routePage = await apiGet(page, '/mes/pro/route/page', {
    pageNo: 1,
    pageSize: 10,
    code: route.code
  })
  const refreshedRoute = (routePage.list || []).find((item) => item.id === route.id)
  const expectedStatus = enabled ? 0 : 1
  assert.equal(refreshedRoute?.status, expectedStatus, `route status should be ${expectedStatus} after UI toggle`)
  route.status = expectedStatus
  return true
}

async function tryRestoreRouteEnabledByUi(page, route) {
  try {
    await setRouteEnabledByUi(page, route, true)
    const routePage = await apiGet(page, '/mes/pro/route/page', {
      pageNo: 1,
      pageSize: 10,
      code: route.code
    })
    const refreshedRoute = (routePage.list || []).find((item) => item.id === route.id)
    return {
      restored: refreshedRoute?.status === 0,
      status: refreshedRoute?.status,
      error: undefined
    }
  } catch (error) {
    const routePage = await apiGet(page, '/mes/pro/route/page', {
      pageNo: 1,
      pageSize: 10,
      code: route.code
    }).catch(() => ({ list: [] }))
    const refreshedRoute = (routePage.list || []).find((item) => item.id === route.id)
    return {
      restored: false,
      status: refreshedRoute?.status,
      error: error instanceof Error ? error.message : String(error)
    }
  }
}

async function selectTargetItem(page, targetItem) {
  const copyDialog = page.getByRole('dialog').filter({ hasText: '\u590d\u5236\u4ea7\u54c1' }).last()
  const targetFormItem = copyDialog.locator('.el-form-item').filter({ hasText: '\u76ee\u6807\u4ea7\u54c1' }).first()
  await targetFormItem.locator('.el-input').first().click()

  const itemDialog = page.getByRole('dialog').filter({ hasText: zh.materialDialog }).last()
  await itemDialog.waitFor({ state: 'visible', timeout: 30000 })
  await itemDialog.locator('input[placeholder="\u8bf7\u8f93\u5165\u7269\u6599\u7f16\u7801"]').fill(targetItem.code)
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/md/item/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    itemDialog.getByRole('button', { name: /\u641c\u7d22/ }).click()
  ])
  await settle(page)
  const itemRow = itemDialog.locator('.el-table__body-wrapper tr.el-table__row').filter({ hasText: targetItem.code }).first()
  await itemRow.waitFor({ state: 'visible', timeout: 30000 })
  await itemRow.click()
  await itemDialog.getByRole('button', { name: zh.confirm }).click()
  await itemDialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function copyProductByUi(page, sourceProduct, targetItem) {
  const sourceRow = page
    .locator('.el-table__body-wrapper tr.el-table__row')
    .filter({ hasText: sourceProduct.itemCode })
    .first()
  await sourceRow.waitFor({ state: 'visible', timeout: 30000 })
  const editDialog = page.getByRole('dialog').filter({ hasText: '\u7f16\u8f91\u5de5\u827a\u8def\u7ebf' }).first()
  const copyButtons = editDialog.getByRole('button', { name: zh.copy })
  const copyButtonCount = await copyButtons.count()
  if (copyButtonCount === 0) {
    throw new Error('No visible copy button found in route product tab.')
  }
  await copyButtons.nth(copyButtonCount - 1).click()

  const copyDialog = page.getByRole('dialog').filter({ hasText: '\u590d\u5236\u4ea7\u54c1' }).last()
  await copyDialog.waitFor({ state: 'visible', timeout: 30000 })
  await selectTargetItem(page, targetItem)

  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route-product/copy') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    copyDialog.getByRole('button', { name: zh.confirm }).click()
  ])
  await page.getByText(zh.success, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await copyDialog.waitFor({ state: 'hidden', timeout: 30000 })
  await settle(page)
}

async function deleteCopiedProductByUi(page, route, targetItem) {
  await openRouteEditProductTab(page, route)
  const editDialog = page.getByRole('dialog').filter({ hasText: '\u7f16\u8f91\u5de5\u827a\u8def\u7ebf' }).first()
  const targetRow = editDialog.locator('.el-table__body-wrapper tr.el-table__row').filter({ hasText: targetItem.code }).first()
  await targetRow.waitFor({ state: 'visible', timeout: 30000 })
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route-product/delete') && response.request().method() === 'DELETE',
      { timeout: 60000 }
    ),
    (async () => {
      await targetRow.getByRole('button', { name: '\u5220\u9664' }).click()
      await confirmMessageBox(page)
    })()
  ])
  await settle(page)
  const productsAfterDelete = await apiGet(page, '/mes/pro/route-product/list-by-route', { routeId: route.id })
  assert.ok(
    !productsAfterDelete.some((product) => product.itemId === targetItem.id),
    `copied product row should be deleted during cleanup: ${targetItem.code}`
  )
  await editDialog.getByRole('button', { name: '\u5173 \u95ed' }).click()
  await editDialog.waitFor({ state: 'hidden', timeout: 30000 })
}

function normalizeNumber(value) {
  if (value === undefined || value === null || value === '') {
    return value
  }
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : value
}

function bomComparableRows(rows) {
  return rows
    .map((row) => ({
      processId: row.processId,
      itemId: row.itemId,
      quantity: normalizeNumber(row.quantity),
      remark: row.remark || ''
    }))
    .sort((left, right) => {
      const processCompare = Number(left.processId) - Number(right.processId)
      if (processCompare !== 0) {
        return processCompare
      }
      return Number(left.itemId) - Number(right.itemId)
    })
}

async function verifyCopyResult(page, candidate) {
  const products = await apiGet(page, '/mes/pro/route-product/list-by-route', { routeId: candidate.route.id })
  const copiedProduct = products.find((product) => product.itemId === candidate.targetItem.id)
  assert.ok(copiedProduct, `Copied product row not found for target item ${candidate.targetItem.code}`)
  assert.equal(copiedProduct.quantity, candidate.sourceProduct.quantity, 'quantity should inherit source row')
  assert.equal(
    normalizeNumber(copiedProduct.productionTime),
    normalizeNumber(candidate.sourceProduct.productionTime),
    'productionTime should inherit source row'
  )
  assert.equal(copiedProduct.timeUnitType, candidate.sourceProduct.timeUnitType, 'timeUnitType should inherit source row')
  assert.equal(copiedProduct.remark || '', candidate.sourceProduct.remark || '', 'remark should inherit source row')

  const targetBomList = await apiGet(page, '/mes/pro/route-product-bom/list', {
    routeId: candidate.route.id,
    productId: candidate.targetItem.id
  })
  assert.deepEqual(
    bomComparableRows(targetBomList),
    bomComparableRows(candidate.sourceBomList),
    'target product BOM rows should match source product BOM rows'
  )
  return { copiedProduct, targetBomList }
}

async function main() {
  const { chromium } = loadPlaywright()
  const launchOptions = {
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const pageErrors = []
  const consoleErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (['error'].includes(message.type())) {
      consoleErrors.push(message.text())
    }
  })

  try {
    await login(page)
    await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText(zh.routeCode, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    const { tenantId } = await authContext(page)
    const candidate = await findCandidate(page)
    let copiedRouteProductId
    let routeStatusChanged = false

    console.log(
      JSON.stringify({
        phase: 'candidate',
        tenantId,
        routeId: candidate.route.id,
        routeCode: candidate.route.code,
        sourceRouteProductId: candidate.sourceProduct.id,
        sourceItemId: candidate.sourceProduct.itemId,
        sourceItemCode: candidate.sourceProduct.itemCode,
        sourceBomCount: candidate.sourceBomList.length,
        targetItemId: candidate.targetItem.id,
        targetItemCode: candidate.targetItem.code
      })
    )

    try {
      routeStatusChanged = await setRouteEnabledByUi(page, candidate.route, false)
      await openRouteEditProductTab(page, candidate.route)
      await copyProductByUi(page, candidate.sourceProduct, candidate.targetItem)
      const result = await verifyCopyResult(page, candidate)
      copiedRouteProductId = result.copiedProduct.id
      await page.screenshot({
        path: path.resolve(__dirname, '../output/mes-pro-route-product-copy-real.png'),
        fullPage: true
      })

      assert.deepEqual(pageErrors, [])
      console.log(
        JSON.stringify({
          phase: 'verified',
          routeId: candidate.route.id,
          routeCode: candidate.route.code,
          copiedRouteProductId: result.copiedProduct.id,
          copiedItemId: result.copiedProduct.itemId,
          copiedItemCode: result.copiedProduct.itemCode,
          copiedBomCount: result.targetBomList.length,
          consoleErrorCount: consoleErrors.length
        })
      )
    } finally {
      if (copiedRouteProductId) {
        await deleteCopiedProductByUi(page, candidate.route, candidate.targetItem)
        console.log(
          JSON.stringify({
            phase: 'cleanup',
            deletedRouteProductId: copiedRouteProductId,
            targetItemCode: candidate.targetItem.code
          })
        )
      }
      if (routeStatusChanged) {
        const restoreResult = await tryRestoreRouteEnabledByUi(page, candidate.route)
        console.log(
          JSON.stringify({
            phase: 'cleanup',
            routeStatusRestored: restoreResult.restored,
            routeStatus: restoreResult.status,
            routeStatusRestoreError: restoreResult.error,
            routeId: candidate.route.id,
            routeCode: candidate.route.code
          })
        )
      }
    }
    await page.screenshot({
      path: path.resolve(__dirname, '../output/mes-pro-route-product-copy-real-cleanup.png'),
      fullPage: true
    })

    console.log('PASS: MES route product copy real UI E2E')
  } catch (error) {
    console.error('E2E_FAILURE_URL:', page.url())
    console.error('E2E_PAGE_ERRORS:', JSON.stringify(pageErrors))
    console.error('E2E_CONSOLE_ERRORS:', JSON.stringify(consoleErrors.slice(-20)))
    console.error(
      'E2E_FAILURE_BODY:',
      (await page.locator('body').innerText().catch((innerError) => String(innerError))).slice(0, 4000)
    )
    await page
      .screenshot({
        path: path.resolve(__dirname, '../output/mes-pro-route-product-copy-real-failure.png'),
        fullPage: true
      })
      .catch(() => {})
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
