const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_UPLOAD_POLICY_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_UPLOAD_POLICY_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_UPLOAD_POLICY_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_UPLOAD_POLICY_E2E_PASSWORD || 'admin123'
const ALLOW_TEST_WRITE = process.env.DCC_UPLOAD_POLICY_E2E_ALLOW_TEST_WRITE === '1'
const CATEGORY_CODE = process.env.DCC_UPLOAD_POLICY_E2E_CATEGORY_CODE || 'CODEX_E2E_SYSTEM'
const PURPOSE = process.env.DCC_UPLOAD_POLICY_E2E_PURPOSE || 'SOURCE'
const PURPOSE_LABEL = process.env.DCC_UPLOAD_POLICY_E2E_PURPOSE_LABEL || '源文件'
const POLICY_CODE =
  process.env.DCC_UPLOAD_POLICY_E2E_POLICY_CODE || 'CODEX_E2E_SYSTEM_SOURCE_LOCAL_20260530'
const POLICY_VERSION =
  process.env.DCC_UPLOAD_POLICY_E2E_POLICY_VERSION || 'local-e2e-20260530'
const MAX_BYTES = Number(process.env.DCC_UPLOAD_POLICY_E2E_MAX_BYTES || '20000')
const CHANGE_REASON =
  process.env.DCC_UPLOAD_POLICY_E2E_CHANGE_REASON ||
  '解除 prepare_test_tenant_dcc_upload_size_policy 阻塞，仅用于本机测试租户真实前端验证'

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.notEqual(
    url.hostname,
    '172.30.30.57',
    'DCC upload policy E2E must not target protected production server 172.30.30.57'
  )
  assert.equal(TENANT, '测试租户', `DCC upload policy E2E must use 测试租户, got ${TENANT}`)
  assert.equal(USERNAME, 'aoteman', `DCC upload policy E2E must use aoteman, got ${USERNAME}`)
  assert.equal(
    ALLOW_TEST_WRITE,
    true,
    'Set DCC_UPLOAD_POLICY_E2E_ALLOW_TEST_WRITE=1 after explicit user approval to write test tenant policy'
  )
  assert(Number.isInteger(MAX_BYTES) && MAX_BYTES > 0, `MAX_BYTES must be positive, got ${MAX_BYTES}`)
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
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    return false
  }
  await tenantSelect.click()
  const input = page.locator('.login-form .el-select__input').first()
  await input.fill(tenantName)
  await page.keyboard.press('Enter')
  return true
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/dcc/controlled-file/categories`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }
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
  await settle(page)
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

async function fetchJson(page, headers, url) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, { headers: requestHeaders })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    { requestUrl: url, requestHeaders: headers }
  )
}

async function openUploadSizePolicyDialog(page) {
  await page.goto(`${BASE_URL}/dcc/controlled-file/categories`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await expectCategoryRow(page)
  const button = page.getByRole('button', { name: /上传大小策略/ }).first()
  await expectVisible(button, 'upload size policy button')
  await button.click()
  const dialog = page.locator('.el-dialog').filter({ hasText: '上传大小策略' }).last()
  await expectVisible(dialog, 'upload size policy dialog')
  await settle(page)
  return dialog
}

async function expectVisible(locator, label) {
  await locator.waitFor({ state: 'visible', timeout: 30000 }).catch((error) => {
    throw new Error(`missing visible ${label}: ${error.message}`)
  })
}

async function expectCategoryRow(page) {
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: CATEGORY_CODE }).first()
  try {
    await row.waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    const rows = await page.locator('.el-table__body-wrapper tbody tr').allTextContents()
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(
      `missing category row ${CATEGORY_CODE}; visibleRows=${JSON.stringify(rows.slice(0, 12))}; bodySnippet=${bodyText.slice(0, 1000)}; original=${error.message}`
    )
  }
}

async function selectOptionFromFormItem(page, dialog, label, optionText) {
  const item = await findFormItemByLabel(dialog, label)
  await expectVisible(item, `${label} form item`)
  const select = item.locator('.el-select').first()
  await select.click()
  const input = select.locator('input').first()
  if ((await input.count()) > 0) {
    const readonly = await input.getAttribute('readonly')
    if (readonly === null) {
      await input.fill(optionText)
    } else {
      await page.keyboard.type(optionText)
    }
  }
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: optionText })
    .first()
  await expectVisible(option, `${label} option ${optionText}`)
  await option.click()
}

async function fillInputFromFormItem(dialog, label, value) {
  const item = await findFormItemByLabel(dialog, label)
  await expectVisible(item, `${label} form item`)
  await item.locator('input').first().fill(String(value))
}

async function fillTextareaFromFormItem(dialog, label, value) {
  const item = await findFormItemByLabel(dialog, label)
  await expectVisible(item, `${label} form item`)
  await item.locator('textarea').first().fill(value)
}

async function ensureSwitchOn(dialog, label) {
  const item = await findFormItemByLabel(dialog, label)
  await expectVisible(item, `${label} form item`)
  const control = item.locator('.el-switch').first()
  const className = (await control.getAttribute('class')) || ''
  if (!className.includes('is-checked')) {
    await control.click()
  }
}

async function findFormItemByLabel(dialog, label) {
  const items = dialog.locator('.el-form-item')
  const count = await items.count()
  for (let index = 0; index < count; index += 1) {
    const item = items.nth(index)
    const labelText = await item
      .locator('.el-form-item__label')
      .first()
      .innerText()
      .catch(() => '')
    if (labelText.trim() === label) {
      return item
    }
  }
  throw new Error(`missing form item label ${label}`)
}

async function chooseCreateOrEdit(dialog) {
  const rowByPolicyCode = dialog.locator('.el-table__body-wrapper tbody tr').filter({
    hasText: POLICY_CODE
  })
  if ((await rowByPolicyCode.count()) > 0) {
    await rowByPolicyCode.first().getByRole('button', { name: /编辑/ }).click()
    return 'edit-policy-code'
  }
  const rowByScopeVersion = dialog
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: CATEGORY_CODE })
    .filter({ hasText: PURPOSE_LABEL })
    .filter({ hasText: POLICY_VERSION })
  if ((await rowByScopeVersion.count()) > 0) {
    await rowByScopeVersion.first().getByRole('button', { name: /编辑/ }).click()
    return 'edit-scope-version'
  }
  await dialog.getByRole('button', { name: /新增策略/ }).click()
  return 'create'
}

async function savePolicyThroughDialog(page, dialog) {
  const mode = await chooseCreateOrEdit(dialog)
  await fillInputFromFormItem(dialog, '策略编码', POLICY_CODE)
  await selectOptionFromFormItem(page, dialog, '范围', '类别与用途')
  await selectOptionFromFormItem(page, dialog, '文件类别', CATEGORY_CODE)
  await selectOptionFromFormItem(page, dialog, '用途', PURPOSE_LABEL)
  await fillInputFromFormItem(dialog, '最大大小', MAX_BYTES)
  await fillInputFromFormItem(dialog, '策略版本', POLICY_VERSION)
  await ensureSwitchOn(dialog, '启用状态')
  await fillTextareaFromFormItem(dialog, '变更原因', CHANGE_REASON)

  const saveButton = dialog.getByRole('button', { name: /创建策略|保存策略/ }).last()
  await expectVisible(saveButton, 'save policy button')
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/protection/upload-size-policies') &&
        ['POST', 'PUT'].includes(response.request().method()),
      { timeout: 30000 }
    ),
    saveButton.click()
  ])
  await expectVisible(dialog.locator('.el-table__body-wrapper tbody tr').filter({ hasText: POLICY_CODE }).first(), 'saved policy row')
  return mode
}

async function verifyEffectivePolicy(page) {
  const headers = await buildAuthHeaders(page)
  const categoryResult = await fetchJson(
    page,
    headers,
    `${BASE_URL}/admin-api/dcc/file-categories`
  )
  assert.equal(categoryResult.status, 200, `category list HTTP failed: ${JSON.stringify(categoryResult)}`)
  assert.equal(categoryResult.payload.code, 0, `category list API failed: ${JSON.stringify(categoryResult.payload)}`)
  const category = categoryResult.payload.data.find((item) => String(item.code) === String(CATEGORY_CODE))
  assert.ok(category?.id, `category ${CATEGORY_CODE} is missing`)

  const effectiveResult = await fetchJson(
    page,
    headers,
    `${BASE_URL}/admin-api/dcc/protection/upload-size-policies/effective?categoryId=${category.id}&purpose=${encodeURIComponent(PURPOSE)}&fileSize=${MAX_BYTES}`
  )
  assert.equal(effectiveResult.status, 200, `effective policy HTTP failed: ${JSON.stringify(effectiveResult)}`)
  assert.equal(effectiveResult.payload.code, 0, `effective policy API failed: ${JSON.stringify(effectiveResult.payload)}`)
  assert.equal(effectiveResult.payload.data.policyCode, POLICY_CODE)
  assert.equal(effectiveResult.payload.data.scopeType, 'CATEGORY_PURPOSE')
  assert.equal(String(effectiveResult.payload.data.categoryId), String(category.id))
  assert.equal(effectiveResult.payload.data.purpose, PURPOSE)
  assert.equal(Number(effectiveResult.payload.data.maxBytes), MAX_BYTES)
  assert.equal(effectiveResult.payload.data.policyVersion, POLICY_VERSION)
  return {
    categoryId: category.id,
    policyId: effectiveResult.payload.data.policyId,
    policyCode: effectiveResult.payload.data.policyCode,
    maxBytes: effectiveResult.payload.data.maxBytes,
    policyVersion: effectiveResult.payload.data.policyVersion
  }
}

async function main() {
  assertSafeBoundary()
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(page)
    const dialog = await openUploadSizePolicyDialog(page)
    const mode = await savePolicyThroughDialog(page, dialog)
    const verification = await verifyEffectivePolicy(page)
    console.log(
      `PASS: DCC upload size policy frontend write mode=${mode} tenant=${TENANT} username=${USERNAME} category=${CATEGORY_CODE} policyId=${verification.policyId} maxBytes=${verification.maxBytes}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(`FAIL: ${error.stack || error.message}`)
  process.exit(1)
})
