const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_SCHEDULER_WORKBENCH_TAB_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_SCHEDULER_WORKBENCH_TAB_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULER_WORKBENCH_TAB_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULER_WORKBENCH_TAB_E2E_PASSWORD || 'admin123',
  headed: process.env.MES_SCHEDULER_WORKBENCH_TAB_E2E_HEADED === '1'
}

const expectedVisibleChildren = [
  {
    id: 5590,
    name: '排产员工作台',
    path: '/mes/pro/scheduler-workbench',
    permission: 'mes:pro-scheduler-workbench:query'
  },
  { id: 5580, name: '排产工单', path: '/mes/pro/schedule-order', permission: 'mes:pro-schedule-order:query' },
  { id: 5550, name: '报工', path: '/mes/pro/feedback', permission: 'mes:pro-feedback:query' },
  { id: 5262, name: '排程日历', path: '/mes/pro/schedule-calendar', permission: 'mes:pro-task:query' },
  { id: 5540, name: '生产排产', path: '/mes/pro/task', permission: 'mes:pro-task:query' },
  { id: 900104, name: '璞慧排产', path: '/mes/pro/puhui-schedule', permission: 'mes:pro-puhui-schedule:query' }
]

function assertPrerequisites() {
  assert.equal(config.baseUrl, 'http://127.0.0.1:8081', 'E2E must use the local frontend entry.')
  assert.equal(config.tenant, '测试租户', 'E2E must use 测试租户 for local verification.')
  assert.equal(config.username, 'aoteman', 'E2E must use the test tenant account aoteman.')
  assert.ok(config.password, 'MES_SCHEDULER_WORKBENCH_TAB_E2E_PASSWORD is required.')
}

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

async function selectTenant(loginForm, tenantName) {
  const tenantSelect = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible())) {
    await tenantSelect.click()
    await tenantSelect.fill(tenantName)
    await tenantSelect.press('Enter')
    return
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), tenantName, 'tenant')
}

async function loginAndCapturePermissionInfo(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  const permissionInfoPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )

  if (page.url().includes('/login')) {
    const loginForm = page.locator('.login-form:visible').first()
    await loginForm.waitFor({ state: 'visible', timeout: 60000 })
    if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
      throw new Error('Captcha is enabled; unattended real E2E cannot continue.')
    }

    await selectTenant(loginForm, config.tenant)
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')
    await Promise.all([
      page.waitForResponse(
        (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
        { timeout: 60000 }
      ),
      loginForm.locator('.el-button--primary').first().click()
    ])
  }

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  const permissionInfoResponse = await permissionInfoPromise
  assert.equal(permissionInfoResponse.status(), 200, 'permission info HTTP status must be 200')
  const body = await permissionInfoResponse.json()
  assert.equal(body.code, 0, `permission info business code must be 0: ${body.msg || body.code}`)
  return body.data
}

function findMenuById(menus, id) {
  for (const menu of menus || []) {
    if (Number(menu.id) === id) {
      return menu
    }
    const child = findMenuById(menu.children || [], id)
    if (child) {
      return child
    }
  }
  return null
}

async function main() {
  assertPrerequisites()

  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage()

  try {
    const permissionInfo = await loginAndCapturePermissionInfo(page)
    const parent = findMenuById(permissionInfo.menus, 900120)
    assert.ok(parent, '智能排产 menu 900120 must be visible in permission menus.')
    assert.equal(parent.name, '智能排产')

    const children = parent.children || []
    const visibleChildren = children.filter((child) => child.visible !== false && child.visible !== 0)

    assert.ok(
      !visibleChildren.some((child) => Number(child.id) === 5985),
      '排产看板 must not be displayed as a visible smart scheduling child tab.'
    )
    const visibleChildIds = visibleChildren.map((child) => Number(child.id))
    let lastMatchedIndex = -1
    for (const expected of expectedVisibleChildren) {
      const matchedIndex = visibleChildIds.findIndex(
        (id, index) => index > lastMatchedIndex && id === expected.id
      )
      assert.notEqual(
        matchedIndex,
        -1,
        `visible child menu ${expected.id} must be present after hiding 排产看板.`
      )
      lastMatchedIndex = matchedIndex
    }

    for (const expected of expectedVisibleChildren) {
      const child = visibleChildren.find((item) => Number(item.id) === expected.id)
      assert.ok(child, `missing child menu ${expected.id}`)
      assert.equal(child.name, expected.name)
      assert.equal(child.path, expected.path)
    }

    const permissions = new Set(permissionInfo.permissions || [])
    assert.ok(permissions.has('mes:pro-smart-scheduling:query'), 'parent permission must be included.')
    for (const expected of expectedVisibleChildren) {
      assert.ok(permissions.has(expected.permission), `permission missing: ${expected.permission}`)
    }

    console.log(
      `PASS: 智能排产 permission menu visible with children ${visibleChildren.map((child) => child.name).join(' > ')}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
