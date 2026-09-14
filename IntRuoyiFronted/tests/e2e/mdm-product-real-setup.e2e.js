const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.MDM_PRODUCT_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.MDM_PRODUCT_E2E_TENANT || '测试租户'
const USERNAME = process.env.MDM_PRODUCT_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.MDM_PRODUCT_E2E_PASSWORD || 'admin123'
const PRODUCT_CODE = process.env.MDM_PRODUCT_E2E_PRODUCT_CODE || 'CDX20260609001'
const DCC_PRODUCT_CODE = process.env.MDM_PRODUCT_E2E_DCC_PRODUCT_CODE || PRODUCT_CODE
const PRODUCT_NAME = process.env.MDM_PRODUCT_E2E_PRODUCT_NAME || 'Codex DCC E2E 产品'
const ALLOW_CREATE = process.env.MDM_PRODUCT_E2E_ALLOW_CREATE === 'true'
const APPROVAL_TOKEN = 'ALLOW_TEST_MDM_PRODUCT_WRITE'
const APPROVAL = process.env.MDM_PRODUCT_E2E_APPROVAL || ''

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'MDM product E2E must not target production server 172.30.30.57')
  assert.equal(TENANT, '测试租户', `MDM product E2E must use 测试租户, got ${TENANT}`)
  assert.notEqual(USERNAME, 'admin', 'MDM product E2E must not use 芋道源码/admin')
  assert.match(DCC_PRODUCT_CODE, /^[A-Za-z0-9]{14}$/, 'DCC product code must be 14 alphanumeric characters')
  if (ALLOW_CREATE && APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set MDM_PRODUCT_E2E_ALLOW_CREATE=true and MDM_PRODUCT_E2E_APPROVAL=${APPROVAL_TOKEN} ` +
        'only after explicit user approval.'
    )
  }
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function fillFirstVisible(page, selector, value, label) {
  const locator = page.locator(selector)
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible ${label}: ${selector}`)
}

async function selectTenant(page, tenantName) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible())) {
    await tenantSelect.click()
    await page.locator('.login-form .el-select__input').first().fill(tenantName)
    await page.keyboard.press('Enter')
    return true
  }
  return false
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  if (page.url().includes('/login')) {
    const selected = await selectTenant(page, TENANT)
    if (!selected) {
      await fillFirstVisible(page, 'input[placeholder="请输入租户名称"]', TENANT, 'tenant input')
    }
    await fillFirstVisible(page, 'input[placeholder="请输入用户名"]', USERNAME, 'username input')
    await fillFirstVisible(page, 'input[placeholder="请输入密码"]', PASSWORD, 'password input')
    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/system/auth/login') &&
          response.request().method() === 'POST',
        { timeout: 30000 }
      ),
      page.locator('.login-form .el-button--primary').first().click()
    ])
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  }
  await settle(page)
}

async function openProductPage(page) {
  await page.goto(`${BASE_URL}/mdm/product`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  try {
    await page.locator('text=展厅主数据').first().waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(
      `MDM product page is not visible; url=${page.url()} body=${JSON.stringify(bodyText.slice(0, 1500))}`
    )
  }
}

async function searchProduct(page, productCode) {
  const productCodeItem = page.locator('.el-form-item').filter({ hasText: '产品编码' }).first()
  await productCodeItem.locator('input').first().fill(productCode)
  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mdm/product/page') &&
        response.request().method() === 'GET',
      { timeout: 30000 }
    )
    .catch(() => null)
  await page.getByRole('button', { name: /搜索/ }).first().click()
  const response = await responsePromise
  if (response) {
    const payload = await response.json().catch(() => null)
    console.log(`MDM_PRODUCT_PAGE_RESPONSE ${JSON.stringify({ status: response.status(), payload }, null, 2)}`)
  }
  await settle(page)
  return page.locator('.el-table__body-wrapper').first().locator('tr').filter({ hasText: productCode }).first()
}

async function fillDialogField(dialog, label, value) {
  await dialog.locator('.el-form-item').filter({ hasText: label }).first().locator('input').first().fill(value)
}

async function createProduct(page) {
  const createButton = page.getByRole('button', { name: /新增/ }).first()
  if (!(await createButton.isVisible().catch(() => false))) {
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(`MDM product create button is not visible for ${TENANT}/${USERNAME}; body=${bodyText.slice(0, 1000)}`)
  }
  await createButton.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增产品主数据' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await fillDialogField(dialog, '产品编码', PRODUCT_CODE)
  await fillDialogField(dialog, 'DCC产品编号', DCC_PRODUCT_CODE)
  await fillDialogField(dialog, '中文名称', PRODUCT_NAME)
  await fillDialogField(dialog, '英文名称', 'Codex DCC E2E Product')
  await fillDialogField(dialog, '型号规格', 'E2E')
  await fillDialogField(dialog, '分类', 'DCC E2E')

  const createResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mdm/product/create') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '保存' }).click()
  const response = await createResponse
  const payload = await response.json().catch(() => null)
  console.log(`MDM_PRODUCT_CREATE_RESPONSE ${JSON.stringify({ status: response.status(), payload }, null, 2)}`)
  if (!response.ok() || payload?.code !== 0) {
    throw new Error(`MDM product create failed: status=${response.status()} payload=${JSON.stringify(payload)}`)
  }
  await dialog.waitFor({ state: 'hidden', timeout: 10000 })
  await settle(page)
}

async function ensureEnabled(page, productCode) {
  const row = await searchProduct(page, productCode)
  if (!(await row.isVisible().catch(() => false))) {
    if (!ALLOW_CREATE) {
      throw new Error(
        `required MDM product does not exist in ${TENANT}; set MDM_PRODUCT_E2E_ALLOW_CREATE=true only after explicit user approval`
      )
    }
    await createProduct(page)
    return
  }
  const rowText = await row.innerText()
  if (rowText.includes('停用')) {
    return
  }
  const enableButton = row.getByRole('button', { name: '启用' }).first()
  if (await enableButton.isVisible().catch(() => false)) {
    if (!ALLOW_CREATE) {
      throw new Error(
        `required MDM product is disabled in ${TENANT}; set MDM_PRODUCT_E2E_ALLOW_CREATE=true only after explicit user approval`
      )
    }
    const responsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mdm/product/update-status') &&
        response.request().method() === 'PUT',
      { timeout: 30000 }
    )
    await enableButton.click()
    const response = await responsePromise
    const payload = await response.json().catch(() => null)
    console.log(`MDM_PRODUCT_ENABLE_RESPONSE ${JSON.stringify({ status: response.status(), payload }, null, 2)}`)
    if (!response.ok() || payload?.code !== 0) {
      throw new Error(`MDM product enable failed: status=${response.status()} payload=${JSON.stringify(payload)}`)
    }
  }
}

;(async () => {
  assertSafeBoundary()
  const browser = await chromium.launch({ headless: process.env.MDM_PRODUCT_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    await login(page)
    await openProductPage(page)
    await ensureEnabled(page, PRODUCT_CODE)
    const row = await searchProduct(page, PRODUCT_CODE)
    await row.waitFor({ state: 'visible', timeout: 10000 })
    const rowText = await row.innerText()
    if (!rowText.includes(DCC_PRODUCT_CODE) || !rowText.includes(PRODUCT_NAME) || !rowText.includes('启用')) {
      throw new Error(`MDM product row does not match expected enabled DCC product: ${rowText}`)
    }
    console.log(
      `MDM_PRODUCT_SETUP_RESULT ${JSON.stringify(
        { baseUrl: BASE_URL, tenant: TENANT, username: USERNAME, productCode: PRODUCT_CODE, dccProductCode: DCC_PRODUCT_CODE },
        null,
        2
      )}`
    )
  } finally {
    await browser.close()
  }
})().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
