const assert = require('node:assert/strict')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for showroom base workbook real E2E.')
  }
}

const config = {
  baseUrl: (process.env.SHOWROOM_BASE_WORKBOOK_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.SHOWROOM_BASE_WORKBOOK_TENANT || '测试租户',
  username: process.env.SHOWROOM_BASE_WORKBOOK_USERNAME || 'aoteman',
  password: process.env.SHOWROOM_BASE_WORKBOOK_PASSWORD || '111111',
  workbookPath:
    process.env.SHOWROOM_BASE_WORKBOOK_PATH ||
    'C:\\Users\\BJB110\\Desktop\\展厅讲解软件产品资料更新底表.xlsx',
  headed: process.env.SHOWROOM_BASE_WORKBOOK_HEADED === '1'
}

const expectedProductCode = 'product_001'
const expectedAwardCode = 'AWARD-001'

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

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/showroom/product`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (page.url().includes('/login')) {
    const loginForm = page.locator('.login-form:visible').first()
    const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
    if ((await tenantInput.count()) > 0) {
      await tenantInput.click()
      await tenantInput.fill(config.tenant)
      await tenantInput.press('Enter')
    } else {
      await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
    }
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ).catch(() => null)
    await loginForm.getByRole('button', { name: /登录/ }).click()
    const response = await responsePromise
    if (response) {
      const payload = await response.json().catch(() => null)
      assert.ok(payload && (payload.code === 0 || payload.code === 200), `login failed: ${JSON.stringify(payload)}`)
    }
  }

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${config.baseUrl}/showroom/product`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.getByText('产品管理').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function fetchJson(page, relativeUrl) {
  return await page.evaluate(async (url) => {
    const token = window.localStorage.getItem('ACCESS_TOKEN') || ''
    const tenantId = window.localStorage.getItem('tenantId') || ''
    const visitTenantId = window.localStorage.getItem('visitTenantId') || ''
    const headers = {
      'Cache-Control': 'no-cache',
      Pragma: 'no-cache'
    }
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
    if (tenantId) {
      headers['tenant-id'] = tenantId
    }
    if (token && visitTenantId) {
      headers['visit-tenant-id'] = visitTenantId
    }
    const response = await fetch(url, {
      method: 'GET',
      headers
    })
    return {
      ok: response.ok,
      status: response.status,
      json: await response.json()
    }
  }, relativeUrl)
}

async function fetchProductCover(page, productCode) {
  const payload = await fetchJson(
    page,
    `/admin-api/showroom/product/page?pageNo=1&pageSize=20&keyword=${encodeURIComponent(productCode)}`
  )
  assert.equal(payload.status, 200, `product page status must be 200, got ${payload.status}`)
  assert.ok(payload.json && (payload.json.code === 0 || payload.json.code === 200), JSON.stringify(payload.json))
  const list = payload.json.data?.list || payload.json.data?.records || []
  const matched = list.find((item) => String(item.productCode || '') === productCode)
  assert.ok(matched, `product ${productCode} must exist in showroom product page`)
  const revision = matched.revision || {}
  const displayRevision = matched.displayRevision || {}
  const coverImage =
    String(revision?.fields?.cover_image || revision?.fields?.coverImage || '').trim() ||
    String(displayRevision?.fields?.cover_image || displayRevision?.fields?.coverImage || '').trim()
  assert.ok(coverImage, `product ${productCode} must have a cover image before/after import`)
  return coverImage
}

async function fetchAwardCover(page, awardCode) {
  const payload = await fetchJson(
    page,
    `/admin-api/showroom/award/page?pageNo=1&pageSize=50&keyword=${encodeURIComponent(awardCode)}`
  )
  assert.equal(payload.status, 200, `award page status must be 200, got ${payload.status}`)
  assert.ok(payload.json && (payload.json.code === 0 || payload.json.code === 200), JSON.stringify(payload.json))
  const list = payload.json.data?.list || payload.json.data?.records || []
  const matched = list.find((item) => String(item.awardCode || '') === awardCode)
  assert.ok(matched, `award ${awardCode} must exist in showroom award page`)
  const coverImage = String(matched.coverImageUrl || matched.coverImage || '').trim()
  assert.ok(coverImage, `award ${awardCode} must have a cover image before/after import`)
  return coverImage
}

async function importBaseWorkbook(page, workbookPath) {
  const importButton = page
    .locator('.showroom-product-list__actions')
    .getByRole('button', { name: '导入无产品图底表' })
    .first()
  await importButton.waitFor({ state: 'visible', timeout: 30000 })
  await importButton.click()

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '产品更新底表导入' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[type="file"]').setInputFiles(workbookPath)

  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/showroom/product/import-base-workbook'),
    { timeout: 180000 }
  )
  await dialog.getByRole('button', { name: /^确 定$/ }).click()
  const response = await responsePromise
  assert.equal(response.status(), 200, `base workbook import status must be 200, got ${response.status()}`)
  const payload = await response.json()
  assert.ok(payload && (payload.code === 0 || payload.code === 200), JSON.stringify(payload))
  const data = payload.data || {}
  assert.ok(Number(data.successCount || 0) + Number(data.skippedCount || 0) > 0, JSON.stringify(data))
  assert.ok(Number(data.awardTotalRows || 0) > 0, JSON.stringify(data))
  await page.getByRole('button', { name: /^确定$/ }).click().catch(async () => {
    await page.getByRole('button', { name: /^OK$/ }).click({ timeout: 5000 })
  })
  await settle(page)
  return data
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    acceptDownloads: true,
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  try {
    await login(page)
    const beforeProductCover = await fetchProductCover(page, expectedProductCode)
    const beforeAwardCover = await fetchAwardCover(page, expectedAwardCode)
    const result = await importBaseWorkbook(page, config.workbookPath)
    const afterProductCover = await fetchProductCover(page, expectedProductCode)
    const afterAwardCover = await fetchAwardCover(page, expectedAwardCode)

    assert.equal(afterProductCover, beforeProductCover, 'existing product cover must not be cleared or replaced')
    assert.equal(afterAwardCover, beforeAwardCover, 'existing award cover must not be cleared or replaced')

    console.log(
      JSON.stringify({
        productCode: expectedProductCode,
        awardCode: expectedAwardCode,
        successCount: Number(result.successCount || 0),
        skippedCount: Number(result.skippedCount || 0),
        failureCount: Number(result.failureCount || 0),
        awardSuccessCount: Number(result.awardSuccessCount || 0),
        awardFailureCount: Number(result.awardFailureCount || 0),
        beforeProductCover,
        afterProductCover,
        beforeAwardCover,
        afterAwardCover
      })
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
