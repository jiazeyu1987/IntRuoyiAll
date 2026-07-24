const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_BACKUP_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.DCC_BACKUP_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_BACKUP_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_BACKUP_E2E_PASSWORD || 'admin123'
const CONTROLLED_FILE_ID = String(process.env.DCC_BACKUP_E2E_CONTROLLED_FILE_ID || '').trim()
const ALLOW_WRITE = process.env.DCC_BACKUP_E2E_ALLOW_WRITE === '1'
const APPROVAL_TOKEN = 'ALLOW_TEST_DCC_FILE_WRITE'
const APPROVAL = process.env.DCC_BACKUP_E2E_APPROVAL || ''

function assertSafeDccBackupBoundary() {
  assert.ok(BASE_URL, 'DCC_BACKUP_E2E_BASE_URL is required for DCC delete E2E')
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'DCC delete E2E must not target protected production server 172.30.30.57')
  if (TENANT !== '测试租户') {
    throw new Error(`DCC delete E2E must use 测试租户, got ${TENANT}`)
  }
  if (USERNAME !== 'aoteman') {
    throw new Error(`DCC delete E2E must use 测试租户/aoteman, got ${USERNAME}`)
  }
  if (!ALLOW_WRITE || APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set DCC_BACKUP_E2E_ALLOW_WRITE=1 and DCC_BACKUP_E2E_APPROVAL=${APPROVAL_TOKEN} ` +
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

async function confirmMessageBox(page) {
  const dialog = page.locator('.el-message-box:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await dialog.locator('.el-message-box__btns .el-button--primary').click()
}

async function clickIfVisible(page, buttonText) {
  const button = page.getByRole('button', { name: buttonText }).first()
  if ((await button.count()) === 0) {
    return false
  }
  if (!(await button.isVisible().catch(() => false))) {
    return false
  }
  await button.click()
  return true
}

;(async () => {
  assertSafeDccBackupBoundary()
  assert.ok(CONTROLLED_FILE_ID, 'DCC_BACKUP_E2E_CONTROLLED_FILE_ID is required')
  const browser = await chromium.launch({ headless: process.env.DCC_BACKUP_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()

  try {
    await login(page)
    await page.goto(`${BASE_URL}/dcc/controlled-file/detail/${CONTROLLED_FILE_ID}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)

    const withdrawn = await clickIfVisible(page, '撤回申请')
    if (withdrawn) {
      await confirmMessageBox(page)
      await page.waitForTimeout(1500)
      await settle(page)
    }

    const deleted = await clickIfVisible(page, '删除流程')
    if (deleted) {
      await confirmMessageBox(page)
      await page.waitForTimeout(1500)
      await settle(page)
    }

    console.log(
      `DCC_WITHDRAW_DELETE_RESULT ${JSON.stringify(
        {
          baseUrl: BASE_URL,
          controlledFileId: CONTROLLED_FILE_ID,
          withdrew: withdrawn,
          deleted
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
