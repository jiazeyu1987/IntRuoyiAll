const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_TEMPLATE_RULE_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_TEMPLATE_RULE_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = process.env.EDHR_TEMPLATE_RULE_TEST_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_TEMPLATE_RULE_TEST_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_TEMPLATE_RULE_TEST_PASSWORD || 'admin123'
const ADMIN_TENANT = process.env.EDHR_TEMPLATE_RULE_ADMIN_TENANT || '芋道源码'
const ADMIN_USERNAME = process.env.EDHR_TEMPLATE_RULE_ADMIN_USERNAME || 'admin'
const ADMIN_PASSWORD = process.env.EDHR_TEMPLATE_RULE_ADMIN_PASSWORD || 'admin123'
const ROUTE = '/mes/pro/batch-record-form-list'
const GROUP_KEY = `e2e-attachment-rule-${Date.now()}`

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'E2E must use the local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'E2E must use the local backend')
  assert.equal(TEST_TENANT, '测试租户', 'write E2E must use the local test tenant')
  assert.equal(TEST_USERNAME, 'aoteman', 'write E2E must use the dedicated test user')
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
  throw new Error(`Missing visible input: ${label}`)
}

async function clickFirstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.click()
      return
    }
  }
  throw new Error(`Missing visible target: ${label}`)
}

async function login(page, tenant, username, password) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('Login captcha is enabled; unattended real E2E cannot continue.')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), password, 'password')
  await clickFirstVisible(loginForm.getByRole('button', { name: /^登录$/ }), 'login button')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openTemplatePage(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('电子批记录列表').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/batch-record-report/page') && response.status() === 200,
    { timeout: 60000 }
  ).catch(() => undefined)
  const rows = page.locator('.batch-record-table .el-table__body-wrapper tbody tr')
  const rowCount = await rows.count()
  assert.ok(rowCount > 0, 'template page must contain real batch record templates')
  return rows.first()
}

async function openFirstCellRuleDialog(page) {
  const row = await openTemplatePage(page)
  const reportName = (await row.locator('td').nth(1).innerText()).trim()
  assert.ok(reportName, 'selected template row must expose a report name')
  await row.getByRole('button', { name: '单元格规则' }).click()
  await page.getByRole('dialog', { name: '单元格规则' }).waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('附件规则').first().waitFor({ state: 'visible', timeout: 60000 })
  return reportName
}

async function configureAttachmentRule(page) {
  await openFirstCellRuleDialog(page)
  const dialog = page.getByRole('dialog', { name: '单元格规则' })
  const attachmentRuleItem = dialog.locator('.el-form-item').filter({ hasText: '附件规则' }).first()
  const enableCheckbox = attachmentRuleItem.locator('.el-checkbox').first()
  if (!(await enableCheckbox.locator('input').isChecked())) {
    await enableCheckbox.click()
  }
  const requiredItem = dialog.locator('.el-form-item').filter({ hasText: '附件必填' }).first()
  const requiredCheckbox = requiredItem.locator('.el-checkbox').first()
  if (!(await requiredCheckbox.locator('input').isChecked())) {
    await requiredCheckbox.click()
  }
  const countInputs = dialog.locator('.el-form-item').filter({ hasText: '附件数量' }).locator('input')
  await countInputs.nth(0).fill('1')
  await countInputs.nth(1).fill('2')
  await dialog.locator('.el-form-item').filter({ hasText: '附件类型' }).locator('input').fill('FILE')
  await dialog.locator('.el-form-item').filter({ hasText: '附件组' }).locator('input').fill(GROUP_KEY)

  const saveResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-report/cell-rules') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '保存' }).last().click()
  const response = await saveResponse
  assert.equal(response.status(), 200, 'cell-rules save HTTP status must be 200')
  const body = await response.json()
  assert.equal(body.code, 0, `cell-rules save business response must succeed: ${body.msg || body.code}`)
  await page.getByText('单元格规则已保存').waitFor({ state: 'visible', timeout: 60000 })
}

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      result[key] = result[key] || sessionStorage.getItem(key)
    }
    return result
  })
  const unwrap = (raw) => {
    if (!raw) return ''
    let current = raw
    for (let index = 0; index < 6; index += 1) {
      try {
        current = JSON.parse(current)
      } catch {
        break
      }
      if (current && typeof current === 'object') {
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
      }
      if (typeof current !== 'string') break
    }
    return String(current || '').replace(/^"|"$/g, '')
  }
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

async function finalApiVerify(page) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, 'final API verification requires browser access token')
  assert.ok(tenantId, 'final API verification requires browser tenant-id')
  const response = await page.request.get(`${BACKEND_URL}/admin-api/mes/pro/batch-record-report/page`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    params: { pageNo: 1, pageSize: 1 }
  })
  assert.equal(response.status(), 200, 'final page API HTTP status must be 200')
  const body = await response.json()
  assert.equal(body.code, 0, `final page API business response must succeed: ${body.msg || body.code}`)
}

async function verifyAdminReadonly(page) {
  await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD)
  await openFirstCellRuleDialog(page)
  const dialog = page.getByRole('dialog', { name: '单元格规则' })
  await dialog.getByText('附件规则').first().waitFor({ state: 'visible', timeout: 60000 })
  const attachmentRuleItem = dialog.locator('.el-form-item').filter({ hasText: '附件规则' }).first()
  const enableCheckbox = attachmentRuleItem.locator('.el-checkbox').first()
  if (!(await enableCheckbox.locator('input').isChecked())) {
    await enableCheckbox.click()
  }
  await dialog.getByText('附件必填').first().waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText('附件数量').first().waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText('附件类型').first().waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText('附件组').first().waitFor({ state: 'visible', timeout: 60000 })
}

async function main() {
  assertLocalOnly()
  const browser = await chromium.launch({ headless: process.env.EDHR_TEMPLATE_RULE_HEADED !== '1' })
  const testContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const testPage = await testContext.newPage()
  try {
    await login(testPage, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD)
    await configureAttachmentRule(testPage)
    await finalApiVerify(testPage)

    const adminContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const adminPage = await adminContext.newPage()
    await verifyAdminReadonly(adminPage)
    await adminContext.close()
  } finally {
    await testContext.close()
    await browser.close()
  }
  console.log(`PASS: template attachment rule real E2E groupKey=${GROUP_KEY}`)
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
