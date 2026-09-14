const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.TENANT_PACKAGE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const SYSTEM_TENANT = process.env.TENANT_PACKAGE_E2E_SYSTEM_TENANT || '芋道源码'
const SYSTEM_USERNAME = process.env.TENANT_PACKAGE_E2E_SYSTEM_USERNAME || 'admin'
const SYSTEM_PASSWORD = process.env.TENANT_PACKAGE_E2E_SYSTEM_PASSWORD || 'admin123'
const TARGET_TENANT_NAME = process.env.TENANT_PACKAGE_E2E_TARGET_TENANT || '测试租户'
const TEST_PACKAGE_NAME = process.env.TENANT_PACKAGE_E2E_PACKAGE_NAME || 'Codex DCC E2E 独立测试套餐'
const APPROVAL_TOKEN = 'ALLOW_TEST_TENANT_PACKAGE_WRITE'
const ALLOW_WRITE = process.env.TENANT_PACKAGE_E2E_ALLOW_WRITE === 'true'
const APPROVAL = process.env.TENANT_PACKAGE_E2E_APPROVAL || ''
const SHARED_PACKAGE_ID = 111
const REQUIRED_PACKAGE_MENU_LABELS = [
  '文件提交',
  '文件查阅',
  '文控权限',
  '运行控制台',
  '角色管理',
  '展厅主数据'
]
const REQUIRED_PACKAGE_MENU_CONTEXT_LABELS = [
  '文控中心',
  '基础设施',
  '系统管理',
  '基础数据',
  ...REQUIRED_PACKAGE_MENU_LABELS
]
const MAX_TEST_PACKAGE_SELECTED_TREE_NODES = 80

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'test tenant package setup must not target production server 172.30.30.57')
  assert.ok(
    ['localhost', '127.0.0.1', '172.30.30.58'].includes(url.hostname),
    `test tenant package setup must target local frontend or test server, got ${url.hostname}`
  )
  assert.equal(TARGET_TENANT_NAME, '测试租户', `target tenant must be 测试租户, got ${TARGET_TENANT_NAME}`)
  if (ALLOW_WRITE) {
    assert.equal(
      APPROVAL,
      APPROVAL_TOKEN,
      `TENANT_PACKAGE_E2E_APPROVAL must equal ${APPROVAL_TOKEN} after explicit user approval`
    )
  }
}

function assertWriteAllowed(action) {
  if (!ALLOW_WRITE) {
    throw new Error(
      `${action}; set TENANT_PACKAGE_E2E_ALLOW_WRITE=true and TENANT_PACKAGE_E2E_APPROVAL=${APPROVAL_TOKEN} only after explicit user approval`
    )
  }
}

function assertPackageIsNotShared(packageId, action) {
  assert.notEqual(
    Number(packageId),
    SHARED_PACKAGE_ID,
    `${action}: current package is shared package 111 and must not be updated`
  )
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
    const selected = await selectTenant(page, SYSTEM_TENANT)
    if (!selected) {
      await fillFirstVisible(page, 'input[placeholder="请输入租户名称"]', SYSTEM_TENANT, 'tenant input')
    }
    await fillFirstVisible(page, 'input[placeholder="请输入用户名"]', SYSTEM_USERNAME, 'username input')
    await fillFirstVisible(page, 'input[placeholder="请输入密码"]', SYSTEM_PASSWORD, 'password input')
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

async function waitForAnyVisibleText(page, text, timeout = 30000) {
  const deadline = Date.now() + timeout
  let lastMatchCount = 0
  while (Date.now() < deadline) {
    const locator = page.locator(`text=${text}`)
    const count = await locator.count().catch(() => 0)
    lastMatchCount = count
    for (let index = 0; index < count; index += 1) {
      if (await locator.nth(index).isVisible().catch(() => false)) {
        return
      }
    }
    await page.waitForTimeout(250)
  }
  throw new Error(`text ${text} is not visible after ${timeout}ms; matched=${lastMatchCount}`)
}

async function openPage(page, route, requiredText) {
  await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  try {
    await waitForAnyVisibleText(page, requiredText)
  } catch (error) {
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(`${route} is not visible; url=${page.url()} body=${JSON.stringify(bodyText.slice(0, 1500))}`)
  }
}

async function findVisibleFormInput(page, label, placeholders = [], timeout = 30000) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    const locators = [page.locator('.el-form-item').filter({ hasText: label }).locator('input')]
    for (const placeholder of placeholders) {
      locators.push(page.locator(`input[placeholder="${placeholder}"]`))
    }
    for (const locator of locators) {
      const count = await locator.count().catch(() => 0)
      for (let index = 0; index < count; index += 1) {
        const input = locator.nth(index)
        if ((await input.isVisible().catch(() => false)) && (await input.isEnabled().catch(() => false))) {
          return input
        }
      }
    }
    await page.waitForTimeout(250)
  }
  const bodyText = await page.locator('body').innerText().catch(() => '')
  throw new Error(
    `missing visible form input for ${label}; placeholders=${JSON.stringify(placeholders)} body=${JSON.stringify(
      bodyText.slice(0, 1500)
    )}`
  )
}

async function searchByFormInput(page, label, value, responseUrlPart, placeholders = []) {
  const input = await findVisibleFormInput(page, label, placeholders)
  await input.fill(value)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes(responseUrlPart) && response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: /搜索/ }).first().click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  if (!response.ok() || payload?.code !== 0) {
    throw new Error(`search ${responseUrlPart} failed: status=${response.status()} payload=${JSON.stringify(payload)}`)
  }
  await settle(page)
  return payload.data?.list || []
}

async function searchTenant(page) {
  await openPage(page, '/system/tenant/list', '租户列表')
  const tenants = await searchByFormInput(page, '租户名', TARGET_TENANT_NAME, '/admin-api/system/tenant/page', [
    '请输入租户名',
    '请输入租户名称'
  ])
  const tenant = tenants.find((item) => item.name === TARGET_TENANT_NAME)
  if (!tenant) {
    throw new Error(`target tenant ${TARGET_TENANT_NAME} does not exist; real DCC E2E cannot prepare test data`)
  }
  const row = page.locator('.el-table__body-wrapper').first().locator('tr').filter({ hasText: TARGET_TENANT_NAME }).first()
  await row.waitFor({ state: 'visible', timeout: 10000 })
  return { row, tenant }
}

async function searchPackage(page) {
  await openPage(page, '/system/tenant/package', '租户套餐')
  const packages = await searchByFormInput(
    page,
    '套餐名',
    TEST_PACKAGE_NAME,
    '/admin-api/system/tenant-package/page',
    ['请输入套餐名']
  )
  const tenantPackage = packages.find((item) => item.name === TEST_PACKAGE_NAME)
  if (!tenantPackage) {
    return null
  }
  assertPackageIsNotShared(tenantPackage.id, 'refuse to reuse dedicated test package name on shared package')
  const row = page.locator('.el-table__body-wrapper').first().locator('tr').filter({ hasText: TEST_PACKAGE_NAME }).first()
  await row.waitFor({ state: 'visible', timeout: 10000 })
  return { row, tenantPackage }
}

async function clickVisibleButton(scope, name) {
  const button = scope.getByRole('button', { name }).first()
  await button.waitFor({ state: 'visible', timeout: 10000 })
  await button.click()
}

async function clickVisibleTableAction(page, actionNames, targetName) {
  const names = Array.isArray(actionNames) ? actionNames : [actionNames]
  for (const actionName of names) {
    const buttons = page.locator('.el-table').getByRole('button', { name: actionName })
    const count = await buttons.count()
    for (let index = 0; index < count; index += 1) {
      const button = buttons.nth(index)
      if (await button.isVisible().catch(() => false)) {
        await button.click()
        return
      }
    }
  }
  const tableText = await page.locator('.el-table').innerText().catch(() => '')
  throw new Error(
    `missing visible table action ${names.join('/')} for ${targetName}; table=${JSON.stringify(tableText.slice(0, 1500))}`
  )
}

async function expandPackageMenuTree(dialog, page) {
  const expandSwitch = dialog.locator('.el-switch').nth(1)
  if (await expandSwitch.isVisible().catch(() => false)) {
    const expanded = await expandSwitch.evaluate((element) => element.classList.contains('is-checked'))
    if (!expanded) {
      await expandSwitch.click()
      await page.waitForTimeout(500)
    }
  }
}

async function assertRequiredPackageMenuLabels(dialog) {
  for (const label of REQUIRED_PACKAGE_MENU_CONTEXT_LABELS) {
    try {
      await dialog.locator('.el-tree-node__content').filter({ hasText: label }).first().waitFor({
        state: 'visible',
        timeout: 15000
      })
    } catch (error) {
      const treeText = await dialog.locator('.el-tree').innerText().catch(() => '')
      throw new Error(`tenant package menu tree does not contain ${label}; tree=${JSON.stringify(treeText.slice(0, 3000))}`)
    }
  }
}

async function selectRequiredPackageMenuNodes(dialog, page) {
  let changed = false
  for (const label of REQUIRED_PACKAGE_MENU_LABELS) {
    const node = dialog.locator('.el-tree-node__content').filter({ hasText: label }).first()
    const checkbox = node.locator('.el-checkbox').first()
    const checked = await checkbox.evaluate((element) => element.classList.contains('is-checked'))
    if (!checked) {
      await checkbox.click()
      await page.waitForTimeout(300)
      changed = true
    }
  }

  const selectedMenuNodeCount = await dialog
    .locator('.el-tree-node__content .el-checkbox.is-checked, .el-tree-node__content .el-checkbox.is-indeterminate')
    .count()
  if (selectedMenuNodeCount === 0 || selectedMenuNodeCount > MAX_TEST_PACKAGE_SELECTED_TREE_NODES) {
    throw new Error(
      `too many menu nodes selected for dedicated E2E tenant package: selectedMenuNodeCount=${selectedMenuNodeCount}, ` +
        `limit=${MAX_TEST_PACKAGE_SELECTED_TREE_NODES}`
    )
  }
  return changed
}

async function createDedicatedPackage(page) {
  assertWriteAllowed(`dedicated tenant package ${TEST_PACKAGE_NAME} is missing`)
  await openPage(page, '/system/tenant/package', '租户套餐')
  await clickVisibleButton(page, /新增/)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await dialog.locator('.el-form-item').filter({ hasText: '套餐名' }).first().locator('input').fill(TEST_PACKAGE_NAME)
  await dialog
    .locator('.el-form-item')
    .filter({ hasText: '备注' })
    .first()
    .locator('input')
    .fill('DCC real-data E2E only; does not modify shared tenant package 111')

  await expandPackageMenuTree(dialog, page)
  await assertRequiredPackageMenuLabels(dialog)
  await selectRequiredPackageMenuNodes(dialog, page)

  const createResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/tenant-package/create') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '确 定' }).click()
  const response = await createResponse
  const payload = await response.json().catch(() => null)
  if (!response.ok() || payload?.code !== 0) {
    throw new Error(`create tenant package failed: status=${response.status()} payload=${JSON.stringify(payload)}`)
  }
  await dialog.waitFor({ state: 'hidden', timeout: 10000 })
  await settle(page)
  const result = await searchPackage(page)
  if (!result) {
    throw new Error(`created tenant package ${TEST_PACKAGE_NAME} is not visible after refresh`)
  }
  return result.tenantPackage
}

async function ensureRequiredPackageMenus(page, existing) {
  await clickVisibleTableAction(page, ['修改', '编辑'], TEST_PACKAGE_NAME)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '编辑' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await expandPackageMenuTree(dialog, page)
  await assertRequiredPackageMenuLabels(dialog)
  const changed = await selectRequiredPackageMenuNodes(dialog, page)
  if (!changed) {
    await dialog.getByRole('button', { name: '取 消' }).click()
    await dialog.waitFor({ state: 'hidden', timeout: 10000 })
    return existing.tenantPackage
  }
  assertWriteAllowed(`dedicated tenant package ${TEST_PACKAGE_NAME} is missing required E2E menus`)
  const updateResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/tenant-package/update') &&
      response.request().method() === 'PUT',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '确 定' }).click()
  const response = await updateResponse
  const payload = await response.json().catch(() => null)
  if (!response.ok() || payload?.code !== 0) {
    throw new Error(`update tenant package failed: status=${response.status()} payload=${JSON.stringify(payload)}`)
  }
  await dialog.waitFor({ state: 'hidden', timeout: 10000 })
  await settle(page)
  const result = await searchPackage(page)
  if (!result) {
    throw new Error(`updated tenant package ${TEST_PACKAGE_NAME} is not visible after refresh`)
  }
  return result.tenantPackage
}

async function ensureDedicatedPackage(page) {
  const existing = await searchPackage(page)
  if (existing) {
    return ensureRequiredPackageMenus(page, existing)
  }
  return createDedicatedPackage(page)
}

async function switchTestTenantToPackage(page, tenantPackage) {
  const { row, tenant } = await searchTenant(page)
  if (Number(tenant.packageId) === Number(tenantPackage.id)) {
    return { tenant, changed: false }
  }
  if (Number(tenant.packageId) === SHARED_PACKAGE_ID) {
    console.log(`TEST_TENANT_PACKAGE_SHARED ${JSON.stringify({ tenant: TARGET_TENANT_NAME, currentPackageId: tenant.packageId })}`)
  } else {
    assertPackageIsNotShared(tenant.packageId, 'refuse to update current tenant package in place')
  }
  assertWriteAllowed(`target tenant ${TARGET_TENANT_NAME} is not bound to dedicated package ${tenantPackage.id}`)

  await clickVisibleTableAction(page, ['编辑', '修改'], TARGET_TENANT_NAME)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '编辑' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  const packageItem = dialog.locator('.el-form-item').filter({ hasText: '租户套餐' }).first()
  await packageItem.locator('.el-select').click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TEST_PACKAGE_NAME }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()

  const updateResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/tenant/update') &&
      response.request().method() === 'PUT',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '确 定' }).click()
  const response = await updateResponse
  const payload = await response.json().catch(() => null)
  if (!response.ok() || payload?.code !== 0) {
    throw new Error(`update tenant package failed: status=${response.status()} payload=${JSON.stringify(payload)}`)
  }
  await dialog.waitFor({ state: 'hidden', timeout: 10000 })
  return { tenant, changed: true }
}

;(async () => {
  assertSafeBoundary()
  const browser = await chromium.launch({ headless: process.env.TENANT_PACKAGE_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    await login(page)
    const tenantPackage = await ensureDedicatedPackage(page)
    const result = await switchTestTenantToPackage(page, tenantPackage)
    console.log(
      `TENANT_PACKAGE_SETUP_RESULT ${JSON.stringify(
        {
          baseUrl: BASE_URL,
          systemTenant: SYSTEM_TENANT,
          systemUsername: SYSTEM_USERNAME,
          targetTenant: TARGET_TENANT_NAME,
          packageId: tenantPackage.id,
          packageName: tenantPackage.name,
          changed: result.changed
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
