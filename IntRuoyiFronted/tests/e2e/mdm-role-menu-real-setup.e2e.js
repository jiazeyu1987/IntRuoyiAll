const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.MDM_ROLE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.MDM_ROLE_E2E_TENANT || '测试租户'
const USERNAME = process.env.MDM_ROLE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.MDM_ROLE_E2E_PASSWORD || 'admin123'
const ROLE_CODE = process.env.MDM_ROLE_E2E_ROLE_CODE || 'showroom_publicity'
const ALLOW_ASSIGN = process.env.MDM_ROLE_E2E_ALLOW_ASSIGN === 'true'
const APPROVAL_TOKEN = 'ALLOW_TEST_MDM_ROLE_MENU_WRITE'
const APPROVAL = process.env.MDM_ROLE_E2E_APPROVAL || ''

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'MDM role E2E must not target production server 172.30.30.57')
  assert.equal(TENANT, '测试租户', `MDM role E2E must use 测试租户, got ${TENANT}`)
  assert.notEqual(USERNAME, 'admin', 'MDM role E2E must not use 芋道源码/admin')
  assert.equal(ROLE_CODE, 'showroom_publicity', 'MDM role E2E only grants menus to the test showroom_publicity role')
  if (ALLOW_ASSIGN && APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set MDM_ROLE_E2E_ALLOW_ASSIGN=true and MDM_ROLE_E2E_APPROVAL=${APPROVAL_TOKEN} ` +
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

async function openRolePage(page) {
  await page.goto(`${BASE_URL}/system/role`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  try {
    await page.locator('text=角色管理').first().waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(`System role page is not visible; url=${page.url()} body=${JSON.stringify(bodyText.slice(0, 1500))}`)
  }
}

async function searchRole(page) {
  const roleCodeItem = page.locator('.el-form-item').filter({ hasText: '角色标识' }).first()
  await roleCodeItem.locator('input').first().fill(ROLE_CODE)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/role/page') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: /搜索/ }).first().click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  console.log(`SYSTEM_ROLE_PAGE_RESPONSE ${JSON.stringify({ status: response.status(), payload }, null, 2)}`)
  await settle(page)
  const row = page.locator('.el-table__body-wrapper').first().locator('tr').filter({ hasText: ROLE_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 10000 })
  return row
}

async function checkTreeNode(dialog, label) {
  const node = dialog.locator('.el-tree-node__content').filter({ hasText: label }).first()
  try {
    await node.waitFor({ state: 'visible', timeout: 15000 })
  } catch (error) {
    const treeText = await dialog.locator('.el-tree').innerText().catch(() => '')
    throw new Error(`role menu tree does not contain ${label}; tree=${JSON.stringify(treeText.slice(0, 3000))}`)
  }
  const checkbox = node.locator('.el-checkbox').first()
  const checked = await checkbox.evaluate((element) => element.classList.contains('is-checked'))
  if (!checked) {
    if (!ALLOW_ASSIGN) {
      throw new Error(
        `role ${ROLE_CODE} is missing ${label}; set MDM_ROLE_E2E_ALLOW_ASSIGN=true only after explicit user approval`
      )
    }
    await checkbox.click()
  }
}

async function assignMdmProductMenus(page) {
  let menuSimplePayload = null
  page.on('response', async (response) => {
    if (response.url().includes('/admin-api/system/menu/simple-list')) {
      menuSimplePayload = await response.json().catch(() => null)
    }
  })
  const row = await searchRole(page)
  await row.getByRole('button', { name: '菜单权限' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '菜单权限' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })

  const expandSwitches = dialog.locator('.el-switch')
  if ((await expandSwitches.count()) >= 2) {
    const expandSwitch = expandSwitches.nth(1)
    const isChecked = await expandSwitch.evaluate((element) => element.classList.contains('is-checked'))
    if (!isChecked) {
      await expandSwitch.click()
      await page.waitForTimeout(1000)
    }
  }

  console.log(`SYSTEM_MENU_SIMPLE_RESPONSE ${JSON.stringify(menuSimplePayload, null, 2)}`)
  await checkTreeNode(dialog, '文控中心')
  await checkTreeNode(dialog, '文控权限')
  await checkTreeNode(dialog, '基础数据')
  await checkTreeNode(dialog, '展厅主数据')

  if (!ALLOW_ASSIGN) {
    return {
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      roleCode: ROLE_CODE,
      readOnly: true,
      requiredMenusPresent: true
    }
  }

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/permission/assign-role-menu') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '确 定' }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  console.log(`SYSTEM_ROLE_MENU_ASSIGN_RESPONSE ${JSON.stringify({ status: response.status(), payload }, null, 2)}`)
  if (!response.ok() || payload?.code !== 0) {
    throw new Error(`assign role menu failed: status=${response.status()} payload=${JSON.stringify(payload)}`)
  }
  await dialog.waitFor({ state: 'hidden', timeout: 10000 })
  return {
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME,
    roleCode: ROLE_CODE,
    readOnly: false,
    granted: 'DCC file category and MDM product menus'
  }
}

;(async () => {
  assertSafeBoundary()
  const browser = await chromium.launch({ headless: process.env.MDM_ROLE_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    await login(page)
    await openRolePage(page)
    const result = await assignMdmProductMenus(page)
    console.log(`MDM_ROLE_MENU_SETUP_RESULT ${JSON.stringify(result, null, 2)}`)
  } finally {
    await browser.close()
  }
})().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
