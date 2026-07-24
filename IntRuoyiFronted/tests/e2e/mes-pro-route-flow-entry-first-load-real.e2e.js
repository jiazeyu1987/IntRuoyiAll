const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_PRO_ROUTE_FLOW_ENTRY_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_PRO_ROUTE_FLOW_ENTRY_TENANT || '测试租户',
  username: process.env.MES_PRO_ROUTE_FLOW_ENTRY_USERNAME || 'aoteman',
  password: process.env.MES_PRO_ROUTE_FLOW_ENTRY_PASSWORD || '111111',
  headed: process.env.MES_PRO_ROUTE_FLOW_ENTRY_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `工艺流程首次进入验证必须使用测试租户/aoteman，当前为 ${config.tenant}/${config.username}`
  )
}

function adminApiPath(url) {
  const marker = '/admin-api/'
  const index = url.indexOf(marker)
  if (index < 0) return ''
  return url.slice(index + marker.length).split('?')[0]
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
  throw new Error(`未找到可见输入框: ${label}`)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
}

async function login(page) {
  const targetPath = '/mes/pro/route'
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, form)
  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"]):visible'), config.username, 'username')
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok([0, 200].includes(payload.code), `登录失败: ${payload.msg || JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openRouteList(page) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const response = await pageResponsePromise
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `工艺路线列表加载失败: ${payload.msg || JSON.stringify(payload)}`)
  const firstRoute = payload.data?.list?.[0]
  assert.ok(firstRoute?.id && firstRoute?.code, '测试租户工艺路线列表必须至少有一条可进入记录。')
  await page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: firstRoute.code }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  return firstRoute
}

async function verifyFlowEntry(page, route) {
  const routeEditRequests = []
  const forbiddenWrites = []
  let captureRouteEdit = false

  page.on('request', (request) => {
    const path = adminApiPath(request.url())
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) && /^mes\/pro\/route/.test(path)) {
      forbiddenWrites.push(`${request.method()} ${path}`)
    }
    if (captureRouteEdit && path) {
      routeEditRequests.push(`${request.method()} ${path}`)
    }
  })

  const flowResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process-flow/get') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  const routeProcessResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process/list-by-route') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )

  captureRouteEdit = true
  const start = Date.now()
  const row = page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: route.code }).first()
  await row.getByRole('button', { name: '编辑' }).first().click()
  await page.locator('.route-flow-graph-designer').waitFor({ state: 'visible', timeout: 60000 })
  const [flowResponse, routeProcessResponse] = await Promise.all([
    flowResponsePromise,
    routeProcessResponsePromise
  ])
  const flowPayload = await flowResponse.json()
  const routeProcessPayload = await routeProcessResponse.json()
  assert.ok([0, 200].includes(flowPayload.code), `流转关系图加载失败: ${flowPayload.msg || JSON.stringify(flowPayload)}`)
  assert.ok(
    [0, 200].includes(routeProcessPayload.code),
    `工序列表加载失败: ${routeProcessPayload.msg || JSON.stringify(routeProcessPayload)}`
  )
  const durationMs = Date.now() - start
  captureRouteEdit = false

  const ownerCandidateRequests = routeEditRequests.filter(
    (request) =>
      request.includes('system/dept/list') ||
      request.includes('system/user/simple') ||
      request.includes('system/user/simple-list')
  )
  assert.deepEqual(ownerCandidateRequests, [], `首次进入 flow 页签不得加载负责人候选人: ${ownerCandidateRequests.join(', ')}`)
  assert.deepEqual(forbiddenWrites, [], `只读验证不得产生工艺路线写请求: ${forbiddenWrites.join(', ')}`)

  console.log(
    `PASS: 工艺流程首次进入 flow 页签完成，route=${route.code}, durationMs=${durationMs}, requests=${routeEditRequests.length}`
  )
}

async function main() {
  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    const firstRoute = await openRouteList(page)
    await verifyFlowEntry(page, firstRoute)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
