const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.SHOWROOM_FREEZE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.SHOWROOM_FREEZE_E2E_TENANT || '测试租户'
const USERNAME = process.env.SHOWROOM_FREEZE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.SHOWROOM_FREEZE_E2E_PASSWORD || '111111'
const CHROME_EXECUTABLE = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/)
  assert.equal(TENANT, '测试租户')
  assert.equal(USERNAME, 'aoteman')
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
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent('/showroom/product')}`, {
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
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }
  const usernameInput = form.locator('input.el-input__inner:not([role="combobox"])').first()
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
  return result.payload.data
}

async function getHallPage(page, headers) {
  return assertApiOk(
    await requestJson(page, headers, 'GET', '/admin-api/showroom/hall/page?pageNo=1&pageSize=20', undefined),
    'hall page'
  )
}

async function getProductPage(page, headers) {
  return assertApiOk(
    await requestJson(page, headers, 'GET', '/admin-api/showroom/product/page?pageNo=1&pageSize=50', undefined),
    'product page'
  )
}

function rowsOf(pageResult) {
  if (Array.isArray(pageResult)) {
    return pageResult
  }
  if (Array.isArray(pageResult?.list)) {
    return pageResult.list
  }
  if (Array.isArray(pageResult?.records)) {
    return pageResult.records
  }
  return []
}

function hallProducts(hall) {
  const raw = hall.products || hall.productMappings || hall.productList || []
  return Array.isArray(raw) ? raw : []
}

async function findPlacedUnfrozenProduct(page, headers) {
  const products = rowsOf(await getProductPage(page, headers))
  const halls = rowsOf(await getHallPage(page, headers))
  assert.ok(products.length > 0, 'BLOCKER: 测试租户产品列表为空，无法执行冻结真实 E2E。')
  assert.ok(halls.length > 0, 'BLOCKER: 测试租户展柜列表为空，无法执行冻结真实 E2E。')
  const productById = new Map(products.map((product) => [String(product.productId), product]))
  for (const hall of halls) {
    for (const mapping of hallProducts(hall)) {
      const productId = String(mapping.productId || mapping.itemId || '')
      const product = productById.get(productId)
      if (product && !product.frozen) {
        return { product, hall, originalHalls: collectProductHallState(halls, productId) }
      }
    }
  }
  throw new Error('BLOCKER: 测试租户未找到已挂展柜且未冻结的产品，无法验证冻结后移出与解冻恢复。')
}

function collectProductHallState(halls, productId) {
  return halls
    .map((hall) => {
      const mapping = hallProducts(hall).find((item) => String(item.productId || item.itemId || '') === String(productId))
      if (!mapping) {
        return null
      }
      return {
        hallId: Number(hall.hallId || hall.id),
        displayOrder: Number(mapping.displayOrder),
        layoutX: mapping.layoutX ?? null,
        layoutY: mapping.layoutY ?? null,
        layoutWidth: mapping.layoutWidth ?? null,
        layoutHeight: mapping.layoutHeight ?? null
      }
    })
    .filter(Boolean)
    .sort((a, b) => a.hallId - b.hallId)
}

async function locateProductRow(page, product) {
  const searchTokens = [
    product.displayRevision?.nameCn,
    product.revision?.nameCn,
    product.nameCn,
    product.displayRevision?.nameEn,
    product.revision?.nameEn,
    product.nameEn,
    product.productCode,
    product.legacyProductCode,
    String(product.productId || '')
  ]
    .map((value) => String(value || '').trim())
    .filter(Boolean)

  await page.goto(`${BASE_URL}/showroom/product`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  for (const token of searchTokens) {
    const searchInput = page.locator('.showroom-product-list__search input').first()
    await searchInput.fill('')
    await searchInput.fill(token)
    await Promise.all([
      page.waitForResponse((item) => item.url().includes('/admin-api/showroom/product/page'), { timeout: 60000 }),
      page.getByRole('button', { name: '查询' }).click()
    ])
    await settle(page)
    const row = page.locator('.el-table__body-wrapper tr', { hasText: token }).first()
    if ((await row.count()) > 0 && (await row.isVisible().catch(() => false))) {
      return row
    }
  }
  throw new Error(
    `无法在产品列表中定位目标产品行: productId=${product.productId}, productCode=${product.productCode}, nameCn=${product.nameCn}`
  )
}

async function clickRowAction(page, product, actionName) {
  const row = await locateProductRow(page, product)
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) =>
        item.url().includes(`/admin-api/showroom/product/${actionName === '冻结' ? 'freeze' : 'unfreeze'}`) &&
        item.request().method() === 'PUT',
      { timeout: 60000 }
    ),
    row.getByRole('button', { name: actionName }).click()
  ])
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && payload && [0, 200].includes(payload.code), `${actionName}接口失败: ${JSON.stringify(payload)}`)
  await settle(page)
  return payload.data
}

async function main() {
  assertSafeBoundary()
  const browser = await chromium.launch({
    headless: true,
    executablePath: CHROME_EXECUTABLE || undefined,
    args: ['--disable-dev-shm-usage']
  })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    await page.goto(`${BASE_URL}/showroom/product`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page)
    await page.getByText('产品管理', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })

    const headers = await buildAuthHeaders(page)
    const sample = await findPlacedUnfrozenProduct(page, headers)
    const productId = Number(sample.product.productId)
    const productCode = String(sample.product.productCode || sample.product.legacyProductCode || productId)
    assert.ok(sample.originalHalls.length > 0, '选中的产品必须存在冻结前展柜关系。')

    const freezeResult = await clickRowAction(page, sample.product, '冻结')
    assert.equal(freezeResult.productId, productId)
    assert.equal(freezeResult.frozen, true)
    const frozenProducts = rowsOf(await getProductPage(page, headers))
    const frozenRow = frozenProducts.find((item) => Number(item.productId) === productId)
    assert.equal(frozenRow?.frozen, true, '冻结后产品分页必须返回 frozen=true。')
    const hallsAfterFreeze = rowsOf(await getHallPage(page, headers))
    assert.deepEqual(collectProductHallState(hallsAfterFreeze, productId), [], '冻结后产品必须从全部展柜移出。')

    const unfreezeResult = await clickRowAction(page, sample.product, '解冻')
    assert.equal(unfreezeResult.productId, productId)
    assert.equal(unfreezeResult.frozen, false)
    const unfrozenProducts = rowsOf(await getProductPage(page, headers))
    const unfrozenRow = unfrozenProducts.find((item) => Number(item.productId) === productId)
    assert.equal(unfrozenRow?.frozen, false, '解冻后产品分页必须返回 frozen=false。')
    const restoredHalls = collectProductHallState(rowsOf(await getHallPage(page, headers)), productId)
    assert.deepEqual(restoredHalls, sample.originalHalls, '解冻后必须恢复冻结前全部展柜、排序和布局。')

    console.log(
      `PASS: showroom product freeze/unfreeze real E2E restored productId=${productId}, productCode=${productCode}, hallCount=${restoredHalls.length}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
