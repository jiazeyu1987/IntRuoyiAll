const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_BACKUP_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.DCC_BACKUP_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_BACKUP_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_BACKUP_E2E_PASSWORD || 'admin123'
const CATEGORY_NAME = process.env.DCC_BACKUP_E2E_CATEGORY || 'Codex Local DCC Category'
const PRODUCT_KEYWORD =
  process.env.DCC_BACKUP_E2E_PRODUCT_KEYWORD ||
  process.env.DCC_BACKUP_E2E_PRODUCT_CODE ||
  'ABC12345678901'
const SOURCE_FILE =
  process.env.DCC_BACKUP_E2E_SOURCE_FILE ||
  'D:\\ProjectPackage\\Int\\IntAuth\\fronted\\node_modules\\mammoth\\test\\test-data\\empty.docx'
const ALLOW_WRITE = process.env.DCC_BACKUP_E2E_ALLOW_WRITE === '1'
const APPROVAL_TOKEN = 'ALLOW_TEST_DCC_FILE_WRITE'
const APPROVAL = process.env.DCC_BACKUP_E2E_APPROVAL || ''

function assertSafeDccBackupBoundary() {
  assert.ok(BASE_URL, 'DCC_BACKUP_E2E_BASE_URL is required for DCC backup E2E')
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'DCC backup E2E must not target protected production server 172.30.30.57')
  if (TENANT !== '测试租户') {
    throw new Error(`DCC backup E2E must use 测试租户, got ${TENANT}`)
  }
  if (USERNAME !== 'aoteman') {
    throw new Error(`DCC backup E2E must use 测试租户/aoteman, got ${USERNAME}`)
  }
  if (!ALLOW_WRITE || APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set DCC_BACKUP_E2E_ALLOW_WRITE=1 and DCC_BACKUP_E2E_APPROVAL=${APPROVAL_TOKEN} ` +
        'only after explicit user approval.'
    )
  }
}

function todayString() {
  const now = new Date()
  const yyyy = now.getFullYear()
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function buildUniquePayload() {
  const now = new Date()
  const stamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}${String(now.getSeconds()).padStart(2, '0')}`
  return {
    fileName: process.env.DCC_BACKUP_E2E_FILE_NAME || `codex-incremental-backup-A-${stamp}.docx`,
    fileNumber: process.env.DCC_BACKUP_E2E_FILE_NUMBER || `CB-${stamp.slice(-6)}`,
    productKeyword: PRODUCT_KEYWORD,
    versionNo: process.env.DCC_BACKUP_E2E_VERSION_NO || 'V1.0',
    effectiveDate: process.env.DCC_BACKUP_E2E_EFFECTIVE_DATE || todayString(),
    remark:
      process.env.DCC_BACKUP_E2E_REMARK || `codex incremental backup baseline ${stamp}`
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

async function selectCategory(page, categoryName) {
  const categoryFormItem = page.locator('.el-form-item').filter({ hasText: '文件类别' }).first()
  await categoryFormItem.locator('.el-select').first().click()
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: categoryName })
    .first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
}

async function waitForDirectoryReady(page) {
  const formItem = page.locator('.el-form-item').filter({ hasText: '提交目录' }).first()
  await formItem.waitFor({ state: 'visible', timeout: 20000 })
  await page.waitForFunction(() => {
    const item = Array.from(document.querySelectorAll('.el-form-item')).find((node) =>
      (node.textContent || '').includes('提交目录')
    )
    return !!item && /(当前绑定目录已经是最后一层目录|最终提交路径：)/.test(item.textContent || '')
  })
}

async function selectProductMaster(page, keyword) {
  const formItem = page.locator('.el-form-item').filter({ hasText: '产品编号' }).first()
  await formItem.waitFor({ state: 'visible', timeout: 20000 })
  await formItem.locator('.el-select').first().click()
  if (keyword === '__FIRST_VISIBLE__') {
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: '·' })
      .first()
    try {
      await option.waitFor({ state: 'visible', timeout: 20000 })
    } catch (error) {
      const dropdownText = await page
        .locator('.el-select-dropdown:visible')
        .allInnerTexts()
        .catch(() => [])
      throw new Error(
        `DCC product options precondition missing: no enabled product with DCC code is visible for ${TENANT}/${USERNAME}; dropdown=${JSON.stringify(dropdownText)}`
      )
    }
    const label = await option.innerText()
    console.log(`DCC_PRODUCT_OPTIONS_SELECTED ${JSON.stringify({ label })}`)
    await option.click()
    return
  }
  const input = formItem.locator('.el-select__input').first()
  const productResponse = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/product-options') &&
        response.request().method() === 'GET',
      { timeout: 20000 }
    )
    .catch(() => null)
  await input.fill(keyword)
  const response = await productResponse
  if (response) {
    const payload = await response.json().catch(() => null)
    console.log(`DCC_PRODUCT_OPTIONS_RESPONSE ${JSON.stringify({ status: response.status(), payload }, null, 2)}`)
  }
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: keyword })
    .first()
  await option.waitFor({ state: 'visible', timeout: 20000 })
  await option.click()
}

async function uploadSourceFile(page, sourceFile) {
  const inputs = page.locator('input[type="file"]')
  const response = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/upload-preview') &&
        response.request().method() === 'POST',
      { timeout: 30000 }
    ),
    inputs.first().setInputFiles(sourceFile)
  ]).then((items) => items[0])
  const payload = await response.json().catch(() => null)
  console.log(`UPLOAD_PREVIEW_RESPONSE ${JSON.stringify({ status: response.status(), payload }, null, 2)}`)
  try {
    await page.waitForFunction(() => {
      const bodyText = document.body.innerText || ''
      return bodyText.includes('预览文件：')
    }, { timeout: 30000 })
  } catch (error) {
    const visibleMessages = await page
      .locator('.el-message:visible,.el-alert:visible,.el-form-item__error')
      .allInnerTexts()
      .catch(() => [])
    throw new Error(`preview upload did not render; visibleMessages=${JSON.stringify(visibleMessages)}`)
  }
}

async function fillForm(page, payload) {
  await page.locator('input[placeholder="可选择历史文件名称，或直接输入新名称"]').fill(payload.fileName)
  await page.locator('input[placeholder="例如 SOP-001"]').fill(payload.fileNumber)
  await selectProductMaster(page, payload.productKeyword)
  await page.locator('input[placeholder="例如 V1.0"]').fill(payload.versionNo)
  await page.locator('input[placeholder="请选择生效日期"]').fill(payload.effectiveDate)
  await page.locator('textarea[placeholder="请输入本次受控文件提交说明"]').fill(payload.remark)
}

async function submitAndCapture(page) {
  let submitPayload = null
  let submitResult = null
  page.on('request', (request) => {
    if (
      request.method() === 'POST' &&
      request.url().includes('/admin-api/dcc/controlled-files/submit')
    ) {
      submitPayload = request.postDataJSON()
    }
  })
  page.on('response', async (response) => {
    if (
      response.request().method() === 'POST' &&
      response.url().includes('/admin-api/dcc/controlled-files/submit')
    ) {
      submitResult = await response.json().catch(() => null)
    }
  })

  await page.getByRole('button', { name: '提交审批' }).click()
  await page.waitForURL((url) => url.href.includes('/dcc/controlled-file/browser'), { timeout: 30000 })
  await settle(page)
  return { submitPayload, submitResult }
}

;(async () => {
  assertSafeDccBackupBoundary()
  assert.ok(fs.existsSync(SOURCE_FILE), `source file missing: ${SOURCE_FILE}`)
  const payload = buildUniquePayload()
  const browser = await chromium.launch({ headless: process.env.DCC_BACKUP_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()

  try {
    await login(page)
    await page.goto(`${BASE_URL}/dcc/controlled-file/upload`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)
    await page.locator('text=受控文件提交').first().waitFor({ state: 'visible', timeout: 30000 })

    await selectCategory(page, CATEGORY_NAME)
    await waitForDirectoryReady(page)
    await fillForm(page, payload)
    await uploadSourceFile(page, SOURCE_FILE)
    const { submitPayload, submitResult } = await submitAndCapture(page)

    const browserTable = page.locator('.el-table').first()
    await browserTable.waitFor({ state: 'visible', timeout: 30000 })

    console.log(
      `DCC_UPLOAD_RESULT ${JSON.stringify(
        {
          baseUrl: BASE_URL,
          tenant: TENANT,
          category: CATEGORY_NAME,
          sourceFile: SOURCE_FILE,
          payload,
          submitPayload,
          submitResult
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
