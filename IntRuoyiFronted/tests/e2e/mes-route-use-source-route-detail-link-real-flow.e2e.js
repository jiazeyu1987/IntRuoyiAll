const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_USE_DETAIL_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_USE_DETAIL_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_USE_DETAIL_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_USE_DETAIL_E2E_PASSWORD || 'admin123',
  headed: process.env.MES_ROUTE_USE_DETAIL_E2E_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `MES route flow detail E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`
  )
}

function routePath(url) {
  const marker = '/admin-api/'
  const index = url.indexOf(marker)
  if (index < 0) return ''
  return url.slice(index + marker.length).split('?')[0]
}

function collectForbiddenMasterWrites(page) {
  const writes = []
  page.on('request', (request) => {
    const method = request.method()
    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) return
    const path = routePath(request.url())
    if (
      /^mes\/pro\/route\/(create|update|delete)/.test(path) ||
      /^mes\/pro\/route-process\/(create|update|delete)/.test(path)
    ) {
      writes.push(`${method} ${path}`)
    }
  })
  return writes
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(300)
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

async function selectLoginTenant(page, loginForm) {
  const tenantSelectInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantSelectInput.count()) > 0 && (await tenantSelectInput.isVisible())) {
    await tenantSelectInput.click()
    await tenantSelectInput.fill(config.tenant)
    await tenantSelectInput.press('Enter')
    return
  }
  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入租户名称"]'),
    config.tenant,
    'tenant'
  )
}

async function login(page, redirectPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  await selectLoginTenant(page, loginForm)
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
    { timeout: 30000 }
  )
  await loginForm.locator('.el-button--primary').first().click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.equal(payload.code, 0, `login failed: ${payload.msg || JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function assertNoLoadError(page, context) {
  const alert = page.locator('.route-flow-config-panel-alert.el-alert--error:visible').first()
  if ((await alert.count()) > 0 && (await alert.isVisible())) {
    throw new Error(`${context} failed: ${await alert.innerText()}`)
  }
}

async function openFirstRouteDetail(page, route) {
  const routePageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await page.goto(`${config.baseUrl}${route.path}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const routePageResponse = await routePageResponsePromise
  const routePagePayload = await routePageResponse.json()
  assert.equal(routePagePayload.code, 0, `${route.title} route page failed: ${routePagePayload.msg || JSON.stringify(routePagePayload)}`)
  const firstRoute = routePagePayload.data?.list?.[0]
  assert.ok(firstRoute?.id, `${route.title} has no source route rows`)
  assert.ok(firstRoute?.name, `${route.title} first source route name is empty`)

  await page.locator('.route-flow-config-panel-page').waitFor({ state: 'visible', timeout: 30000 })
  await assertNoLoadError(page, route.title)
  const firstRow = page.locator('.route-flow-config-panel-table .el-table__body-wrapper .el-table__row').first()
  await firstRow.waitFor({ state: 'visible', timeout: 30000 })
  const routeNameButton = firstRow.locator('td').nth(1).locator('button').first()
  await routeNameButton.waitFor({ state: 'visible', timeout: 10000 })

  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/mes/pro/route/get?id=${firstRoute.id}`) &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await routeNameButton.click()
  const detailResponse = await detailResponsePromise
  const detailPayload = await detailResponse.json()
  assert.equal(detailPayload.code, 0, `${route.title} route detail failed: ${detailPayload.msg || JSON.stringify(detailPayload)}`)

  const detailDialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺路线详情' }).last()
  await detailDialog.waitFor({ state: 'visible', timeout: 30000 })
  const dialogText = await detailDialog.innerText()
  assert.ok(dialogText.includes('组成工序'), `${route.title} detail dialog must show route process tab`)
  assert.ok(dialogText.includes(firstRoute.name), `${route.title} detail dialog must show source route name`)

  await detailDialog.getByRole('button', { name: '关 闭' }).click()
  await detailDialog.waitFor({ state: 'hidden', timeout: 30000 })
  return firstRoute
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const forbiddenMasterWrites = collectForbiddenMasterWrites(page)

  try {
    await login(page, '/mes/pro/route?tab=schedule-config')
    const scheduleRoute = await openFirstRouteDetail(page, {
      title: '工艺流程排产配置',
      path: '/mes/pro/route?tab=schedule-config'
    })
    const batchRoute = await openFirstRouteDetail(page, {
      title: '工艺流程批记录配置',
      path: '/mes/pro/route?tab=batch-record-config'
    })
    assert.deepEqual(forbiddenMasterWrites, [], `original route/process CRUD was called: ${forbiddenMasterWrites.join(', ')}`)
    console.log(
      `PASS: MES route flow source route detail link real flow. scheduleRoute=${scheduleRoute.code}, batchRoute=${batchRoute.code}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
