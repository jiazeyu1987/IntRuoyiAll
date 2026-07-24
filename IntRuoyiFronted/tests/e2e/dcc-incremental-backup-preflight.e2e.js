const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_BACKUP_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.DCC_BACKUP_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_BACKUP_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_BACKUP_E2E_PASSWORD || 'admin123'
const SKIP_RUNTIME_CONTROL = process.env.DCC_BACKUP_E2E_SKIP_RUNTIME_CONTROL === '1'
const PRINT_UPLOAD_CATEGORIES = process.env.DCC_BACKUP_E2E_PRINT_UPLOAD_CATEGORIES === '1'
const INSPECT_UPLOAD_CATEGORY = process.env.DCC_BACKUP_E2E_INSPECT_UPLOAD_CATEGORY || ''

function assertSafeDccBackupBoundary() {
  assert.ok(BASE_URL, 'DCC_BACKUP_E2E_BASE_URL is required for DCC preflight E2E')
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'DCC preflight E2E must not target protected production server 172.30.30.57')
  if (TENANT !== '测试租户') {
    throw new Error(`DCC preflight E2E must use 测试租户, got ${TENANT}`)
  }
  if (USERNAME !== 'aoteman') {
    throw new Error(`DCC preflight E2E must use 测试租户/aoteman, got ${USERNAME}`)
  }
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

async function expectVisible(page, selector, label) {
  const locator = page.locator(selector).first()
  await locator.waitFor({ state: 'visible', timeout: 30000 })
  return locator
}

async function gotoAndAssert(page, path, selector, label) {
  await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await expectVisible(page, selector, label)
}

async function expectNoProdBackupOption(page) {
  await page.goto(`${BASE_URL}/infra/monitors/runtime-control`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await page.locator('button:has-text("立即备份")').first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '立即备份' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  const testOption = dialog.getByRole('radio', { name: '测试服' })
  await testOption.waitFor({ state: 'visible', timeout: 10000 })
  assert.equal(await dialog.getByRole('radio', { name: '正式服' }).count(), 0, 'backup-now must not expose prod')
}

async function maybePrintUploadCategories(page) {
  if (!PRINT_UPLOAD_CATEGORIES) {
    return
  }
  await page.locator('.el-form-item').filter({ hasText: '文件类别' }).locator('.el-select').first().click()
  await page.waitForTimeout(800)
  const labels = await page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .evaluateAll((nodes) =>
      nodes
        .map((node) => (node.textContent || '').trim())
        .filter(Boolean)
    )
  console.log(`UPLOAD_CATEGORY_OPTIONS ${JSON.stringify(labels)}`)
  await page.keyboard.press('Escape').catch(() => {})
}

async function maybeInspectUploadCategory(page) {
  if (!INSPECT_UPLOAD_CATEGORY) {
    return
  }
  const categorySelect = page
    .locator('.el-form-item')
    .filter({ hasText: '文件类别' })
    .locator('.el-select')
    .first()
  await categorySelect.click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({
    hasText: INSPECT_UPLOAD_CATEGORY
  }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
  await page.waitForTimeout(1500)
  const directoryText = await page.locator('body').innerText()
  const match = directoryText.match(/绑定目录[\s\S]*?提交目录[\s\S]*?(最终提交路径：.*|请选择到最后一层叶子目录后再提交。|当前绑定目录已经是最后一层目录，将直接提交到该目录。)/)
  console.log(`UPLOAD_CATEGORY_INSPECT ${JSON.stringify({ category: INSPECT_UPLOAD_CATEGORY, snippet: match ? match[0] : directoryText.slice(0, 1200) })}`)
}

;(async () => {
  assertSafeDccBackupBoundary()
  const browser = await chromium.launch({ headless: process.env.DCC_BACKUP_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()

  try {
    await login(page)
    await gotoAndAssert(page, '/dcc/controlled-file/upload', 'text=受控文件提交', 'upload page title')
    await expectVisible(page, 'button:has-text("选择文件")', 'upload source button')
    await expectVisible(page, 'button:has-text("提交审批")', 'upload submit button')
    await maybePrintUploadCategories(page)
    await maybeInspectUploadCategory(page)

    await gotoAndAssert(page, '/dcc/controlled-file/browser', 'text=处理状态', 'browser filter')
    await expectVisible(page, 'button:has-text("刷新")', 'browser refresh button')

    await gotoAndAssert(page, '/dcc/controlled-file/browser', 'button:has-text("刷新列表")', 'browser refresh list button')
    await expectVisible(page, 'button:has-text("刷新列表")', 'browser refresh list button')

    if (!SKIP_RUNTIME_CONTROL) {
      await expectNoProdBackupOption(page)
    }

    console.log(
      `PASS: dcc incremental backup preflight baseUrl=${BASE_URL} tenant=${TENANT} skipRuntimeControl=${SKIP_RUNTIME_CONTROL}`
    )
  } finally {
    await browser.close()
  }
})().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
