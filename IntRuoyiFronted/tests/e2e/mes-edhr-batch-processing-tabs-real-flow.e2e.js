const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_EDHR_BATCH_TABS_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_EDHR_BATCH_TABS_E2E_TENANT || '测试租户',
  username: process.env.MES_EDHR_BATCH_TABS_E2E_USERNAME || 'aoteman',
  password: process.env.MES_EDHR_BATCH_TABS_E2E_PASSWORD || '111111',
  headed: process.env.MES_EDHR_BATCH_TABS_E2E_HEADED === '1'
}

const expectedVisibleChildren = [
  {
    id: 900365,
    name: '批记录表单',
    path: '/mes/pro/batch-record-form-list',
    permission: 'mes:pro-batch-record-template:query'
  },
  {
    id: 900033,
    name: '批次执行',
    path: '/mes/pro/feedback/edhr-batch-execution',
    permission: 'mes:pro-edhr-batch-execution:query'
  },
  {
    id: 900025,
    name: '表单追溯',
    path: '/mes/pro/feedback/edhr-form-trace',
    permission: 'mes:pro-batch-record-execution:track'
  },
  {
    id: 900432,
    name: '表单日志',
    path: '/mes/pro/feedback/edhr-form-fill-log',
    permission: 'mes:pro-edhr-form-fill-log:query'
  }
]

const expectedRemovedTemplateConfig = {
  id: 900002,
  name: '模板与配置',
  path: '',
  permission: 'mes:pro-batch-record-template:query'
}

function assertPrerequisites() {
  assert.equal(config.baseUrl, 'http://127.0.0.1:8081', 'E2E must use the local frontend entry.')
  assert.equal(config.tenant, '测试租户', 'E2E must use 测试租户 for local verification.')
  assert.equal(config.username, 'aoteman', 'E2E must use the test tenant account aoteman.')
  assert.ok(config.password, 'MES_EDHR_BATCH_TABS_E2E_PASSWORD is required.')
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
    const parent = findMenuById(permissionInfo.menus, 900220)
    assert.ok(parent, 'eDHR批记录 menu 900220 must be visible in permission menus.')
    assert.equal(parent.name, 'eDHR批记录')
    assert.equal(parent.path, 'edhr-batch-processing')

    const children = parent.children || []
    const visibleChildren = children.filter((child) => child.visible !== false)
    assert.deepEqual(
      visibleChildren.map((child) => Number(child.id)),
      expectedVisibleChildren.map((child) => child.id),
      'eDHR批记录 visible child menu ids must exclude 模板与配置 and keep the approved order.'
    )

    for (const expected of expectedVisibleChildren) {
      const child = visibleChildren.find((item) => Number(item.id) === expected.id)
      assert.ok(child, `missing child menu ${expected.id}`)
      assert.equal(child.name, expected.name)
      assert.equal(child.path, expected.path)
    }

    const retiredExecutionList = children.find((item) => Number(item.id) === 900023)
    assert.equal(retiredExecutionList, undefined, 'retired eDHR execution list menu 900023 must not remain routable in permission menus.')

    const removedTemplateConfig = children.find(
      (item) => Number(item.id) === expectedRemovedTemplateConfig.id
    )
    if (removedTemplateConfig) {
      assert.equal(removedTemplateConfig.visible, false, '模板与配置 menu 900002 must be hidden.')
      assert.equal(removedTemplateConfig.name, expectedRemovedTemplateConfig.name)
      assert.equal(removedTemplateConfig.path, expectedRemovedTemplateConfig.path)
    }

    const permissions = new Set(permissionInfo.permissions || [])
    assert.ok(
      permissions.has('mes:pro-edhr-batch-processing:query'),
      'parent permission must be included in permission info.'
    )
    for (const expected of expectedVisibleChildren) {
      assert.ok(permissions.has(expected.permission), `permission missing: ${expected.permission}`)
    }
    assert.ok(
      permissions.has(expectedRemovedTemplateConfig.permission),
      'template query permission must remain for retained batch-record form features.'
    )
    assert.ok(
      permissions.has('mes:pro-edhr-release:query'),
      'release query permission must remain through 表单追溯 release tab.'
    )
    assert.equal(
      visibleChildren.find((item) => Number(item.id) === 900260),
      undefined,
      '放行与归档 menu 900260 must not remain visible as an independent child.'
    )

    await page.goto(`${config.baseUrl}/mes/pro/batch-record-form-list`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)
    await page.getByText('批记录表单').first().waitFor({ state: 'visible', timeout: 30000 })

    await page.goto(`${config.baseUrl}/mes/pro/batch-record-template`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)
    const notFoundText = page.getByText(/404|Page Not Found|页面不存在|找不到页面/)
    assert.ok(
      (await notFoundText.count()) > 0,
      'direct legacy /mes/pro/batch-record-template access must render 404.'
    )

    console.log(
      `PASS: eDHR批记录 visible children ${visibleChildren.map((child) => child.name).join(' > ')}; legacy template config route removed`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
