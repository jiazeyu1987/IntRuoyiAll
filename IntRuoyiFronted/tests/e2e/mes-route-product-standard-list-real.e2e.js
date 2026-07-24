const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const zh = {
  tenant: '\u6d4b\u8bd5\u79df\u6237',
  routePageText: '\u5de5\u827a\u6d41\u7a0b',
  productTab: '\u5173\u8054\u4ea7\u54c1',
  edit: '\u7f16\u8f91',
  addProduct: '\u5173\u8054\u4ea7\u54c1',
  bindProduct: '\u8865\u9f50\u4ea7\u54c1',
  save: '\u4fdd\u5b58',
  oldBindProduct: '\u4ece\u751f\u4ea7\u8ba2\u5355\u8865\u9f50\u4ea7\u54c1'
}

const config = {
  baseUrl: (process.env.MES_ROUTE_PRODUCT_STANDARD_LIST_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_PRODUCT_STANDARD_LIST_TENANT || zh.tenant,
  username: process.env.MES_ROUTE_PRODUCT_STANDARD_LIST_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_PRODUCT_STANDARD_LIST_PASSWORD || '111111',
  headed: process.env.MES_ROUTE_PRODUCT_STANDARD_LIST_HEADED === '1'
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalOnly(baseUrl) {
  const parsed = new URL(baseUrl)
  assert.ok(
    ['localhost', '127.0.0.1', '::1', '[::1]'].includes(parsed.hostname),
    `real E2E must stay local, got ${baseUrl}`
  )
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
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    if (await tenantOption.count()) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '\u767b\u5f55' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(payload.code),
    `login failed: HTTP ${loginResponse.status()} ${JSON.stringify(payload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function openFirstRouteProductTab(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText(zh.routePageText, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)

  const firstEditButton = page.getByRole('button', { name: zh.edit }).first()
  await firstEditButton.waitFor({ state: 'visible', timeout: 60000 })
  await firstEditButton.click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })
  await page.locator('.route-edit-page').first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)

  const productTab = page.getByRole('tab', { name: zh.productTab }).first()
  await productTab.waitFor({ state: 'visible', timeout: 60000 })
  await productTab.click()
  await settle(page)
}

async function assertToolbarButton(page, toolbar, label) {
  assert.ok(label.length <= 4, `${label} must be no more than 4 characters`)
  const button = toolbar.locator('button').filter({ hasText: label }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  const box = await button.boundingBox()
  assert.ok(box, `${label} button must have a visible bounding box`)
  return { label, box }
}

async function assertProductListLayout(page) {
  const template = page.locator('[data-table-key="mes.pro.route.product"]').first()
  await template.waitFor({ state: 'visible', timeout: 60000 })
  await template.locator('[data-user-table-key="mes.pro.route.product"]').first().waitFor({
    state: 'visible',
    timeout: 30000
  })

  const toolbar = template.locator('.unified-list-template__toolbar').first()
  await toolbar.waitFor({ state: 'visible', timeout: 30000 })
  const buttons = await Promise.all(
    [zh.addProduct, zh.bindProduct, zh.save].map((label) => assertToolbarButton(page, toolbar, label))
  )
  const centers = buttons.map((item) => item.box.y + item.box.height / 2)
  assert.ok(
    Math.max(...centers) - Math.min(...centers) <= 8,
    `toolbar buttons must stay on one row: ${JSON.stringify(buttons)}`
  )
  assert.equal(
    await toolbar.locator('button').filter({ hasText: zh.oldBindProduct }).count(),
    0,
    'old long bind button label must not be visible in the toolbar'
  )
  assert.equal(
    await page.locator('.route-edit-page__actions:visible').count(),
    0,
    'legacy bottom save action must be hidden on the product tab'
  )
}

async function main() {
  assertLocalOnly(config.baseUrl)
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  try {
    const page = await browser.newPage({ viewport: { width: 1680, height: 900 } })
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    await openFirstRouteProductTab(page)
    await assertProductListLayout(page)
    console.log('mes-route-product-standard-list-real PASS')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
