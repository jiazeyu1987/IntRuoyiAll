const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_MAXIMIZE_BASE_URL || 'http://localhost:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_ROUTE_FLOW_MAXIMIZE_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_MAXIMIZE_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_MAXIMIZE_PASSWORD || '111111',
  headed: process.env.MES_ROUTE_FLOW_MAXIMIZE_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `流转关系图最大化真实验证必须使用测试租户/aoteman，当前为 ${config.tenant}/${config.username}`
  )
}

function adminApiPath(url) {
  const marker = '/admin-api/'
  const index = url.indexOf(marker)
  if (index < 0) return ''
  return url.slice(index + marker.length).split('?')[0]
}

function extractRouteList(payload) {
  const data = payload?.data ?? payload
  return data?.list || payload?.list || []
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
  const tenantInput = form
    .locator('.el-select input[role="combobox"]:visible, input.el-select__input:visible')
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
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
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):visible'),
    config.username,
    'username'
  )
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok([0, 200].includes(payload.code), `登录失败: ${payload.msg || JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function selectReadableRoute(page) {
  const routePageResponsePromise = page.waitForResponse(
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
  const routePageResponse = await routePageResponsePromise
  const routePayload = await routePageResponse.json()
  assert.ok(
    [0, 200].includes(routePayload.code),
    `工艺路线列表加载失败: ${routePayload.msg || JSON.stringify(routePayload)}`
  )
  const routes = extractRouteList(routePayload)
  const route =
    routes.find((item) => item?.id && item?.activeRouteVersionId && item?.flowGraphConfigured) ||
    routes.find((item) => item?.id && item?.activeRouteVersionId) ||
    routes.find((item) => item?.id)
  if (!route) {
    throw new Error('BLOCKER: 测试租户工艺路线列表没有真实路线，无法验证流转关系图最大化。')
  }
  return route
}

async function waitForGraph(page, route) {
  const forbiddenWrites = []
  page.on('request', (request) => {
    const path = adminApiPath(request.url())
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) && /^mes\/pro\/route/.test(path)) {
      forbiddenWrites.push(`${request.method()} ${path}`)
    }
  })

  const flowResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process-flow/get') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/route/edit/${route.id}?tab=flow`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const graph = page.locator('.route-flow-graph-designer').first()
  await graph.waitFor({ state: 'visible', timeout: 60000 })
  const flowResponse = await flowResponsePromise
  const flowPayload = await flowResponse.json()
  assert.ok([0, 200].includes(flowPayload.code), `流转关系图加载失败: ${JSON.stringify(flowPayload)}`)
  return { graph, forbiddenWrites }
}

async function verifyMaximizeAndRestore(page, graph) {
  const button = graph.locator('[data-flow-action="toggle-route-flow-maximize"]').first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await assertButtonText(button, '最大化')

  const beforeRect = await graph.boundingBox()
  const viewport = page.viewportSize()
  assert.ok(beforeRect, '最大化前必须能读取关系图区域尺寸')
  assert.ok(viewport, '必须能读取浏览器视口尺寸')
  assert.ok(
    beforeRect.height < viewport.height || beforeRect.width < viewport.width,
    `最大化前关系图不应已经覆盖全视口: before=${JSON.stringify(beforeRect)}, viewport=${JSON.stringify(viewport)}`
  )

  await button.click()
  await page.waitForFunction(() => {
    const graphElement = document.querySelector('.route-flow-graph-designer')
    return graphElement?.classList.contains('is-maximized')
  })
  await assertButtonText(button, '恢复')
  const maximizedRect = await graph.boundingBox()
  assert.ok(maximizedRect, '最大化后必须能读取关系图区域尺寸')
  assert.ok(maximizedRect.x <= 1, `最大化后左边界必须贴近视口: ${JSON.stringify(maximizedRect)}`)
  assert.ok(maximizedRect.y <= 1, `最大化后上边界必须贴近视口: ${JSON.stringify(maximizedRect)}`)
  assert.ok(
    maximizedRect.width >= viewport.width - 2,
    `最大化后宽度必须覆盖视口: rect=${JSON.stringify(maximizedRect)}, viewport=${JSON.stringify(viewport)}`
  )
  assert.ok(
    maximizedRect.height >= viewport.height - 2,
    `最大化后高度必须覆盖视口: rect=${JSON.stringify(maximizedRect)}, viewport=${JSON.stringify(viewport)}`
  )

  await button.click()
  await page.waitForFunction(() => {
    const graphElement = document.querySelector('.route-flow-graph-designer')
    return graphElement && !graphElement.classList.contains('is-maximized')
  })
  await assertButtonText(button, '最大化')
  const restoredRect = await graph.boundingBox()
  assert.ok(restoredRect, '恢复后必须能读取关系图区域尺寸')
  assert.ok(
    Math.abs(restoredRect.height - beforeRect.height) <= 4,
    `恢复后高度必须回到原布局: before=${JSON.stringify(beforeRect)}, restored=${JSON.stringify(restoredRect)}`
  )

  await button.click()
  await page.waitForFunction(() =>
    document.querySelector('.route-flow-graph-designer')?.classList.contains('is-maximized')
  )
  await page.keyboard.press('Escape')
  await page.waitForFunction(() => {
    const graphElement = document.querySelector('.route-flow-graph-designer')
    return graphElement && !graphElement.classList.contains('is-maximized')
  })
  await assertButtonText(button, '最大化')
}

async function assertButtonText(button, expectedText) {
  await button.waitFor({ state: 'visible', timeout: 30000 })
  const text = ((await button.textContent()) || '').replace(/\s+/g, '')
  assert.ok(text.includes(expectedText), `按钮文案应包含 ${expectedText}，实际为 ${text}`)
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
    const route = await selectReadableRoute(page)
    const { graph, forbiddenWrites } = await waitForGraph(page, route)
    await verifyMaximizeAndRestore(page, graph)
    assert.deepEqual(forbiddenWrites, [], `只读最大化验证不得产生工艺路线写请求: ${forbiddenWrites.join(', ')}`)
    console.log(`mes-route-flow-maximize-real PASS route=${route.code || route.id}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
