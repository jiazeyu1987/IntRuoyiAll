const assert = require('node:assert/strict')
const fs = require('node:fs')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_UPLOAD_POLICY_E2E_BASE_URL || process.env.DCC_BACKUP_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.DCC_UPLOAD_POLICY_E2E_TENANT || process.env.DCC_BACKUP_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_UPLOAD_POLICY_E2E_USERNAME || process.env.DCC_BACKUP_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_UPLOAD_POLICY_E2E_PASSWORD || process.env.DCC_BACKUP_E2E_PASSWORD || 'admin123'
const CATEGORY_NAME = process.env.DCC_UPLOAD_POLICY_E2E_CATEGORY || process.env.DCC_BACKUP_E2E_CATEGORY || 'Codex Local DCC Category'
const PURPOSE = process.env.DCC_UPLOAD_POLICY_E2E_PURPOSE || 'SOURCE'
const SOURCE_FILE =
  process.env.DCC_UPLOAD_POLICY_E2E_SOURCE_FILE ||
  process.env.DCC_BACKUP_E2E_SOURCE_FILE ||
  'D:\\ProjectPackage\\Int\\IntAuth\\fronted\\node_modules\\mammoth\\test\\test-data\\empty.docx'

function assertSafeBoundary() {
  assert.ok(BASE_URL, 'DCC_UPLOAD_POLICY_E2E_BASE_URL or DCC_BACKUP_E2E_BASE_URL is required')
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'DCC upload policy readiness must not target protected production server 172.30.30.57')
  assert.equal(TENANT, '测试租户', `DCC upload policy readiness must use 测试租户, got ${TENANT}`)
  assert.equal(USERNAME, 'aoteman', `DCC upload policy readiness must use 测试租户/aoteman, got ${USERNAME}`)
  assert.ok(fs.existsSync(SOURCE_FILE), `source file missing for upload size policy readiness: ${SOURCE_FILE}`)
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
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

function unwrapPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return payload
  }
  return payload.data ?? payload.result ?? payload
}

function findCategory(payload) {
  const categories = unwrapPayload(payload)
  assert.ok(Array.isArray(categories), `DCC category response is not a list: ${JSON.stringify(payload)}`)
  const category = categories.find((item) => item && item.name === CATEGORY_NAME && item.active !== false)
  assert.ok(
    category?.id,
    `DCC upload category precondition missing: ${CATEGORY_NAME}; categories=${JSON.stringify(categories.map((item) => item?.name).filter(Boolean))}`
  )
  return category
}

async function readCategoryFromUploadPage(page) {
  const categoryResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/file-categories') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await page.goto(`${BASE_URL}/dcc/controlled-file/upload`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const categoryResponse = await categoryResponsePromise
  const categoryPayload = await categoryResponse.json().catch(() => null)
  const requestHeaders = categoryResponse.request().headers()
  await settle(page)
  await page.locator('text=受控文件提交').first().waitFor({ state: 'visible', timeout: 30000 })
  return {
    category: findCategory(categoryPayload),
    authHeaders: {
      authorization: requestHeaders.authorization,
      'tenant-id': requestHeaders['tenant-id'],
      'visit-tenant-id': requestHeaders['visit-tenant-id']
    }
  }
}

async function getEffectivePolicy(page, categoryId, fileSize, authHeaders) {
  return await page.evaluate(
    async ({ categoryId, purpose, fileSize, authHeaders }) => {
      const headers = {
        Accept: 'application/json'
      }
      if (authHeaders.authorization) {
        headers.Authorization = authHeaders.authorization
      }
      if (authHeaders['tenant-id']) {
        headers['tenant-id'] = String(authHeaders['tenant-id'])
      }
      if (authHeaders['visit-tenant-id']) {
        headers['visit-tenant-id'] = String(authHeaders['visit-tenant-id'])
      }
      const query = new URLSearchParams({
        categoryId: String(categoryId),
        purpose,
        fileSize: String(fileSize)
      })
      const response = await fetch(`/admin-api/dcc/protection/upload-size-policies/effective?${query}`, {
        method: 'GET',
        headers
      })
      const payload = await response.json().catch(() => null)
      return {
        status: response.status,
        payload
      }
    },
    { categoryId, purpose: PURPOSE, fileSize, authHeaders }
  )
}

;(async () => {
  assertSafeBoundary()
  const fileSize = fs.statSync(SOURCE_FILE).size
  const browser = await chromium.launch({ headless: process.env.DCC_UPLOAD_POLICY_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    await login(page)
    const { category, authHeaders } = await readCategoryFromUploadPage(page)
    const effective = await getEffectivePolicy(page, category.id, fileSize, authHeaders)
    const data = unwrapPayload(effective.payload)
    if (effective.status !== 200 || effective.payload?.code !== 0 || !data?.policyId) {
      throw new Error(
        `DCC upload size policy readiness blocked: ${effective.payload?.message || effective.payload?.msg || 'missing approved upload size policy data'}; ` +
          `category=${CATEGORY_NAME}; categoryId=${category.id}; purpose=${PURPOSE}; fileSize=${fileSize}; ` +
          'missing approved upload size policy data'
      )
    }
    console.log(
      `DCC_UPLOAD_SIZE_POLICY_READINESS_RESULT ${JSON.stringify(
        {
          baseUrl: BASE_URL,
          tenant: TENANT,
          username: USERNAME,
          readOnly: true,
          categoryName: CATEGORY_NAME,
          categoryId: category.id,
          purpose: PURPOSE,
          fileSize,
          policy: data
        },
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
