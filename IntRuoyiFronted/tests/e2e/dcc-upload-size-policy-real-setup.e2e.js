const assert = require('node:assert/strict')
const fs = require('node:fs')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_UPLOAD_POLICY_E2E_BASE_URL || process.env.DCC_BACKUP_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.DCC_UPLOAD_POLICY_E2E_TENANT || process.env.DCC_BACKUP_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_UPLOAD_POLICY_E2E_USERNAME || process.env.DCC_BACKUP_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_UPLOAD_POLICY_E2E_PASSWORD || process.env.DCC_BACKUP_E2E_PASSWORD || 'admin123'
const CATEGORY_NAME = process.env.DCC_UPLOAD_POLICY_E2E_CATEGORY || process.env.DCC_BACKUP_E2E_CATEGORY || 'Codex Local DCC Category'
const PURPOSE = process.env.DCC_UPLOAD_POLICY_E2E_PURPOSE || 'SOURCE'
const POLICY_CODE = process.env.DCC_UPLOAD_POLICY_E2E_POLICY_CODE || 'CODEX-TEST-DCC-SOURCE-UPLOAD-SIZE'
const POLICY_VERSION = process.env.DCC_UPLOAD_POLICY_E2E_POLICY_VERSION || 'test-e2e-v1'
const CHANGE_REASON =
  process.env.DCC_UPLOAD_POLICY_E2E_CHANGE_REASON ||
  '测试租户 DCC 真实增量备份恢复 E2E 上传大小策略前置准备'
const SOURCE_FILE =
  process.env.DCC_UPLOAD_POLICY_E2E_SOURCE_FILE ||
  process.env.DCC_BACKUP_E2E_SOURCE_FILE ||
  'D:\\ProjectPackage\\Int\\IntAuth\\fronted\\node_modules\\mammoth\\test\\test-data\\empty.docx'
const APPROVAL_TOKEN = 'ALLOW_TEST_DCC_UPLOAD_POLICY_WRITE'
const ALLOW_WRITE = process.env.DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE === 'true'
const APPROVAL = process.env.DCC_UPLOAD_POLICY_E2E_APPROVAL || ''

function assertSafeBoundary() {
  assert.ok(BASE_URL, 'DCC_UPLOAD_POLICY_E2E_BASE_URL or DCC_BACKUP_E2E_BASE_URL is required')
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'DCC upload policy setup must not target protected production server 172.30.30.57')
  assert.equal(TENANT, '测试租户', `DCC upload policy setup must use 测试租户, got ${TENANT}`)
  assert.equal(USERNAME, 'aoteman', `DCC upload policy setup must use 测试租户/aoteman, got ${USERNAME}`)
  assert.ok(fs.existsSync(SOURCE_FILE), `source file missing for upload size policy setup: ${SOURCE_FILE}`)
  if (ALLOW_WRITE) {
    assert.equal(
      APPROVAL,
      APPROVAL_TOKEN,
      `DCC_UPLOAD_POLICY_E2E_APPROVAL must equal ${APPROVAL_TOKEN} after explicit user approval`
    )
  }
}

function assertWriteAllowed(action) {
  if (!ALLOW_WRITE) {
    throw new Error(
      `${action}; set DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE=true and ` +
        `DCC_UPLOAD_POLICY_E2E_APPROVAL=${APPROVAL_TOKEN} only after explicit user approval`
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
  const form = page.locator('form.login-form:visible').first()
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(tenantName)
    const tenantOption = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: tenantName })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 10000 })
    await tenantOption.click()
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

async function waitForPageText(page, text, timeout = 30000) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    const locator = page.locator(`text=${text}`)
    const count = await locator.count().catch(() => 0)
    for (let index = 0; index < count; index += 1) {
      if (await locator.nth(index).isVisible().catch(() => false)) {
        return
      }
    }
    await page.waitForTimeout(250)
  }
  const bodyText = await page.locator('body').innerText().catch(() => '')
  throw new Error(`text ${text} is not visible; body=${JSON.stringify(bodyText.slice(0, 1500))}`)
}

async function openCategoryPage(page) {
  const categoryResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/file-categories') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await page.goto(`${BASE_URL}/dcc/controlled-file/categories`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const categoryResponse = await categoryResponsePromise
  const payload = await categoryResponse.json().catch(() => null)
  if (!categoryResponse.ok() || payload?.code !== 0) {
    throw new Error(`open DCC category page failed: status=${categoryResponse.status()} payload=${JSON.stringify(payload)}`)
  }
  await settle(page)
  await waitForPageText(page, '类别名称')
  await findTargetCategoryRow(page, CATEGORY_NAME)
}

async function findTargetCategoryRow(page, targetName) {
  const table = page.locator('.el-table').first()
  for (let attempt = 0; attempt < 20; attempt += 1) {
    const row = table.locator('.el-table__body-wrapper tr').filter({ hasText: targetName }).first()
    if (await row.isVisible().catch(() => false)) {
      return row
    }
    const nextButton = page.locator('.el-pagination .btn-next').last()
    if (!(await nextButton.count()) || !(await nextButton.isVisible().catch(() => false))) {
      break
    }
    const disabled = await nextButton.evaluate((element) =>
      element.hasAttribute('disabled') || element.classList.contains('is-disabled') || element.getAttribute('aria-disabled') === 'true'
    )
    if (disabled) {
      break
    }
    await nextButton.click()
    await settle(page)
  }
  const tableText = await table.innerText().catch(() => '')
  throw new Error(`missing target category row ${targetName}; table=${JSON.stringify(tableText.slice(0, 1500))}`)
}

async function clickVisibleTableAction(page, actionName, targetName) {
  const row = await findTargetCategoryRow(page, targetName)
  const buttons = row.getByRole('button', { name: actionName })
  const count = await buttons.count()
  for (let index = 0; index < count; index += 1) {
    const button = buttons.nth(index)
    if (await button.isVisible().catch(() => false)) {
      await button.scrollIntoViewIfNeeded()
      const box = await button.boundingBox()
      if (!box) {
        throw new Error(`missing clickable box for table action ${actionName} on ${targetName}`)
      }
      await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
      return
    }
  }
  const rowText = await row.innerText().catch(() => '')
  throw new Error(`missing visible table action ${actionName} for ${targetName}; row=${JSON.stringify(rowText.slice(0, 1500))}`)
}

async function openUploadPolicyDialog(page) {
  await clickVisibleTableAction(page, '上传策略', CATEGORY_NAME)
  const dialog = page.locator('.el-dialog').filter({ hasText: '上传大小策略' }).last()
  try {
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    const dialogCount = await page.locator('.el-dialog').count().catch(() => 0)
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(
      `upload size policy dialog did not open for ${CATEGORY_NAME}; dialogCount=${dialogCount}; ` +
        `body=${JSON.stringify(bodyText.slice(-2000))}`
    )
  }
  await settle(page)
  return dialog
}

async function fillDialogInput(dialog, label, value) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 10000 })
  const input = item.locator('input, textarea').first()
  await input.fill(String(value))
}

async function ensureSwitchChecked(dialog, label) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 10000 })
  const switcher = item.locator('.el-switch').first()
  const checked = await switcher.evaluate((element) => element.classList.contains('is-checked'))
  if (!checked) {
    await switcher.click()
  }
}

async function selectPurpose(page, dialog) {
  const label = PURPOSE === 'DRAWING_PDF' ? '图纸 PDF' : '源文件'
  const item = dialog.locator('.el-form-item').filter({ hasText: '用途' }).first()
  await item.locator('.el-select').click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: label }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
}

function resolveMaxBytes() {
  const fileSize = fs.statSync(SOURCE_FILE).size
  const configured = Number(process.env.DCC_UPLOAD_POLICY_E2E_MAX_BYTES || 0)
  if (Number.isFinite(configured) && configured > 0) {
    return Math.max(configured, fileSize)
  }
  return Math.max(10 * 1024 * 1024, fileSize)
}

async function policyCodeAlreadyVisible(dialog) {
  return (await dialog.locator('.el-table').filter({ hasText: POLICY_CODE }).count()) > 0
}

async function createPolicyThroughDialog(page, dialog, maxBytes) {
  assertWriteAllowed(`DCC upload size policy ${POLICY_CODE} is missing`)
  await dialog.getByRole('button', { name: '新增策略' }).click()
  await fillDialogInput(dialog, '策略编码', POLICY_CODE)
  await selectPurpose(page, dialog)
  await fillDialogInput(dialog, '最大大小', maxBytes)
  await ensureSwitchChecked(dialog, '启用状态')
  await fillDialogInput(dialog, '策略版本', POLICY_VERSION)
  await fillDialogInput(dialog, '变更原因', CHANGE_REASON)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/protection/upload-size-policies') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '保存策略' }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  if (!response.ok() || payload?.code !== 0) {
    throw new Error(`create DCC upload size policy failed: status=${response.status()} payload=${JSON.stringify(payload)}`)
  }
  await settle(page)
}

;(async () => {
  assertSafeBoundary()
  const browser = await chromium.launch({ headless: process.env.DCC_UPLOAD_POLICY_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    await login(page)
    await openCategoryPage(page)
    const dialog = await openUploadPolicyDialog(page)
    const maxBytes = resolveMaxBytes()
    let changed = false
    if (!(await policyCodeAlreadyVisible(dialog))) {
      await createPolicyThroughDialog(page, dialog, maxBytes)
      changed = true
    } else if (!ALLOW_WRITE) {
      console.log(
        `DCC_UPLOAD_SIZE_POLICY_SETUP_RESULT ${JSON.stringify(
          {
            baseUrl: BASE_URL,
            tenant: TENANT,
            username: USERNAME,
            readOnly: true,
            categoryName: CATEGORY_NAME,
            policyCode: POLICY_CODE,
            purpose: PURPOSE,
            changed: false
          },
          null,
          2
        )}`
      )
      return
    }
    console.log(
      `DCC_UPLOAD_SIZE_POLICY_SETUP_RESULT ${JSON.stringify(
        {
          baseUrl: BASE_URL,
          tenant: TENANT,
          username: USERNAME,
          readOnly: !ALLOW_WRITE,
          categoryName: CATEGORY_NAME,
          policyCode: POLICY_CODE,
          purpose: PURPOSE,
          maxBytes,
          changed
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
