const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_BASIC_INFO_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_ROUTE_BASIC_INFO_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_BASIC_INFO_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_BASIC_INFO_E2E_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_BASIC_INFO_E2E_ROUTE_CODE || 'RT000017'
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalOnly(baseUrl) {
  const parsed = new URL(baseUrl)
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(parsed.hostname),
    `MES_ROUTE_BASIC_INFO_E2E_BASE_URL must be local, got ${baseUrl}`
  )
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(800)
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
  if (await tenantInput.count()) {
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
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
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
  const payload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(payload.code),
    `login failed: HTTP ${loginResponse.status()} ${JSON.stringify(payload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function main() {
  assertLocalOnly(config.baseUrl)
  assert.equal(config.tenant, '测试租户', `real E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `real E2E must use aoteman, got ${config.username}`)

  const browser = await chromium.launch({ headless: true, executablePath })
  const context = await browser.newContext()
  const page = await context.newPage()
  const pageErrors = []
  const consoleErrors = []
  const requestFailures = []
  const mesWriteRequests = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('requestfailed', (request) => {
    requestFailures.push({
      url: request.url(),
      errorText: request.failure()?.errorText || 'unknown request failure'
    })
  })
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
    ) {
      mesWriteRequests.push(`${request.method()} ${request.url()}`)
    }
  })

  try {
    await login(page)
    await page.goto(`${config.baseUrl}/mes/pro/route`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.getByText('工艺流程', { exact: false }).first().waitFor({
      state: 'visible',
      timeout: 60000
    })
    await settle(page)

    await page
      .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
      .first()
      .fill(config.routeCode)
    await page.getByRole('button', { name: /查询|搜索/ }).first().click()
    await settle(page)

    const row = page.locator('tr.el-table__row').filter({ hasText: config.routeCode }).first()
    await row.waitFor({ state: 'visible', timeout: 60000 })
    await row.getByRole('button', { name: '编辑' }).click()
    await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), {
      timeout: 60000
    })

    const editor = page.locator('.route-edit-page').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })
    const topTabs = editor
      .locator('.route-form-content__tabs > .el-tabs__header')
      .first()
      .getByRole('tab')
    await topTabs.filter({ hasText: '流转关系图' }).waitFor({
      state: 'visible',
      timeout: 60000
    })
    const tabNames = (await topTabs.allTextContents()).map((text) => text.trim()).filter(Boolean)
    const expectedTabs = ['组成工序', '基础信息', '流转关系图', '关联产品']
    const expectedIndexes = expectedTabs.map((name) => tabNames.indexOf(name))
    assert.ok(expectedIndexes.every((index) => index >= 0), `route tabs missing: ${tabNames.join(', ')}`)
    assert.ok(
      expectedIndexes.every((index, position) => position === 0 || expectedIndexes[position - 1] < index),
      `route tabs out of order: ${tabNames.join(', ')}`
    )

    await topTabs.filter({ hasText: '基础信息' }).click()
    const basicPanel = editor.locator('.route-form-content__tabs .el-tab-pane:visible').first()
    for (const label of ['编码', '名称', '负责人', '说明', '备注']) {
      await basicPanel.getByText(label, { exact: true }).waitFor({ state: 'visible', timeout: 10000 })
    }
    await editor.getByRole('button', { name: /保\s*存/ }).waitFor({
      state: 'visible',
      timeout: 10000
    })

    const relevantRequestFailures = requestFailures.filter(
      ({ url, errorText }) =>
        errorText !== 'net::ERR_ABORTED' &&
        (url.startsWith(config.baseUrl) || url.includes('/admin-api/mes/'))
    )
    const unexpectedConsoleErrors = consoleErrors.filter(
      (message) => !/^Failed to load resource: net::ERR_(CONNECTION_TIMED_OUT|ABORTED)$/.test(message)
    )
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' | ')}`)
    assert.deepEqual(
      relevantRequestFailures,
      [],
      `local request failures: ${JSON.stringify(relevantRequestFailures)}`
    )
    assert.deepEqual(
      unexpectedConsoleErrors,
      [],
      `console errors: ${unexpectedConsoleErrors.join(' | ')}`
    )
    assert.deepEqual(mesWriteRequests, [], `read-only E2E emitted MES writes: ${mesWriteRequests.join(' | ')}`)
    console.log(
      `PASS: route basic info real E2E, route=${config.routeCode}, tabs=${tabNames.join(' > ')}, externalRequestFailures=${requestFailures.length - relevantRequestFailures.length}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
