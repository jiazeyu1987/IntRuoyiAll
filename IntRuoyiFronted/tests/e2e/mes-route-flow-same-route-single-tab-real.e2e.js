const assert = require('node:assert/strict')
const path = require('node:path')
const fs = require('node:fs')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_FLOW_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_E2E_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FLOW_E2E_ROUTE_CODE || 'RT000017',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_E2E_ARTIFACT_DIR ||
      path.join(__dirname, '..', 'output', '20260710-route-flow-same-route-single-tab')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalReadOnlyTarget() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', `real E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `real E2E must use aoteman, got ${config.username}`)
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(600)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/route')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  await tenantInput.fill(config.tenant)
  const tenantOption = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: config.tenant })
    .first()
  if (await tenantOption.count()) {
    await tenantOption.click()
  } else {
    await tenantInput.press('Enter')
  }

  const accountInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  await accountInput.fill('')
  await accountInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(loginPayload.code),
    `login failed: HTTP ${loginResponse.status()} ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function openRouteGraph(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page)

  const routeCodeInput = page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
  await routeCodeInput.fill(config.routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)

  const row = page.locator('tr.el-table__row').filter({ hasText: config.routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit/'), {
    timeout: 60000
  })

  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.getByRole('tab', { name: '流转关系图' }).click()
  const nodes = editor.locator('[data-flow-node="route-process"]')
  await nodes.first().waitFor({ state: 'visible', timeout: 60000 })
  assert.ok((await nodes.count()) >= 3, `route ${config.routeCode} needs at least three process nodes`)
  await settle(page)
  return { editor, nodes }
}

function routeEditTags(page) {
  return page.locator('#v-tags-view .v-tags-view__item').filter({ hasText: '编辑工艺路线' })
}

async function clickNodeAndReadRouteProcessId(page, nodes, index) {
  const node = nodes.nth(index)
  const routeProcessId = await node.getAttribute('data-route-process-id')
  assert.ok(routeProcessId, `route process node ${index + 1} must expose routeProcessId`)
  await node.click()
  await page.waitForFunction(
    (id) =>
      document
        .querySelector(`[data-flow-node="route-process"][data-route-process-id="${id}"]`)
        ?.classList.contains('is-selected'),
    routeProcessId,
    { timeout: 10000 }
  )
  await page.waitForTimeout(300)
  assert.equal(
    new URL(page.url()).searchParams.get('routeProcessId'),
    null,
    'plain process node click must not persist routeProcessId query'
  )
  return routeProcessId
}

async function main() {
  assertLocalReadOnlyTarget()
  fs.mkdirSync(config.artifactDir, { recursive: true })

  const browser = await chromium.launch({
    headless: true,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })

  try {
    const context = await browser.newContext({ viewport: { width: 1600, height: 900 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    const clickPhaseRequests = {
      routeGraphReloads: 0,
      routeDetailLoads: 0,
      flowConfigLoads: 0,
      routeScheduleConfigLoads: 0,
      mesWriteRequests: []
    }
    let captureClickPhaseRequests = false
    page.on('request', (request) => {
      if (!captureClickPhaseRequests) return
      const url = request.url()
      const method = request.method()
      if (url.includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        clickPhaseRequests.mesWriteRequests.push({ method, url })
      }
      if (url.includes('/admin-api/mes/pro/route-process-flow/get')) {
        clickPhaseRequests.routeGraphReloads += 1
      }
      if (url.includes('/admin-api/mes/pro/route/get')) {
        clickPhaseRequests.routeDetailLoads += 1
      }
      if (url.includes('/admin-api/mes/pro/route/flow-config')) {
        clickPhaseRequests.flowConfigLoads += 1
      }
      if (url.includes('/admin-api/mes/pro/route-schedule-config/list-by-route-version')) {
        clickPhaseRequests.routeScheduleConfigLoads += 1
      }
    })

    await login(page)
    const { nodes } = await openRouteGraph(page)
    assert.ok(
      (await routeEditTags(page).count()) <= 1,
      'opening one route must not create duplicate edit tabs'
    )
    assert.equal(
      new URL(page.url()).searchParams.get('routeProcessId'),
      null,
      'route graph should open without selected routeProcessId query before plain node clicks'
    )

    const routeProcessIds = []
    captureClickPhaseRequests = true
    for (const index of [0, 1, 2]) {
      routeProcessIds.push(await clickNodeAndReadRouteProcessId(page, nodes, index))
      assert.ok(
        (await routeEditTags(page).count()) <= 1,
        `clicking process node ${index + 1} must not create duplicate route edit tabs`
      )
    }
    captureClickPhaseRequests = false

    assert.equal(new Set(routeProcessIds).size, 3, 'E2E must click three different route process nodes')
    assert.equal(
      clickPhaseRequests.routeGraphReloads,
      0,
      'plain process node clicks must not reload the route flow graph'
    )
    assert.ok(
      clickPhaseRequests.routeDetailLoads <= 1,
      `route-level detail config should be cached, route/get loads=${clickPhaseRequests.routeDetailLoads}`
    )
    assert.ok(
      clickPhaseRequests.flowConfigLoads <= 2,
      `route-level flow configs should be loaded once for SCHEDULE and BATCH, loads=${clickPhaseRequests.flowConfigLoads}`
    )
    assert.ok(
      clickPhaseRequests.routeScheduleConfigLoads <= 1,
      `route schedule configs should be cached, loads=${clickPhaseRequests.routeScheduleConfigLoads}`
    )
    assert.deepEqual(
      clickPhaseRequests.mesWriteRequests,
      [],
      'read-only node click E2E must not issue MES write requests'
    )
    const routeEditTagCount = await routeEditTags(page).count()
    const tagTexts = await routeEditTags(page).allInnerTexts()
    assert.ok(
      tagTexts.length <= 1,
      `route edit tags must not duplicate: ${JSON.stringify(tagTexts)}`
    )

    await page.screenshot({
      path: path.join(config.artifactDir, 'single-route-edit-tab.png'),
      fullPage: true
    })

    process.stdout.write(
      `${JSON.stringify(
        {
          ok: true,
          baseUrl: config.baseUrl,
          tenant: config.tenant,
          username: config.username,
          routeCode: config.routeCode,
          routeProcessIds,
          clickPhaseRequests,
          routeEditTagCount,
          tagTexts
        },
        null,
        2
      )}\n`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
