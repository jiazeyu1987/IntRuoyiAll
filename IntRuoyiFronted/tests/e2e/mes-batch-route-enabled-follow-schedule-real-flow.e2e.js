const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_BATCH_ROUTE_FOLLOW_E2E_BASE_URL || 'http://localhost:8082').replace(/\/+$/, ''),
  tenant: process.env.MES_BATCH_ROUTE_FOLLOW_E2E_TENANT || '测试租户',
  username: process.env.MES_BATCH_ROUTE_FOLLOW_E2E_USERNAME || 'aoteman',
  password: process.env.MES_BATCH_ROUTE_FOLLOW_E2E_PASSWORD || '111111',
  routeCode: process.env.MES_BATCH_ROUTE_FOLLOW_E2E_ROUTE_CODE || 'ROUTE-YXN.069.001.1001',
  headed: process.env.MES_BATCH_ROUTE_FOLLOW_E2E_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `MES batch route follow E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`
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

async function selectLoginTenant(loginForm) {
  const tenantSelectInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantSelectInput.count()) > 0 && (await tenantSelectInput.isVisible())) {
    await tenantSelectInput.click()
    await tenantSelectInput.fill(config.tenant)
    await tenantSelectInput.press('Enter')
    return
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
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
  if (
    (await loginForm
      .locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]')
      .count()) > 0
  ) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  await selectLoginTenant(loginForm)
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

async function openRouteConfig(page, routePathValue, titleText) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await page.goto(`${config.baseUrl}${routePathValue}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const pageResponse = await pageResponsePromise
  const pagePayload = await pageResponse.json()
  assert.equal(pagePayload.code, 0, `${titleText} route page failed: ${pagePayload.msg || JSON.stringify(pagePayload)}`)

  await page.locator('.route-flow-config-panel-page').waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText(titleText, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await assertNoLoadError(page, titleText)

  await page.locator('input[placeholder="请输入工艺路线编码"]').first().fill(config.routeCode)
  const searchResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: '搜索' }).first().click()
  const searchResponse = await searchResponsePromise
  const searchPayload = await searchResponse.json()
  assert.equal(
    searchPayload.code,
    0,
    `${titleText} route search failed: ${searchPayload.msg || JSON.stringify(searchPayload)}`
  )
  assert.ok(searchPayload.data?.list?.length > 0, `${titleText} route ${config.routeCode} must exist`)
  assert.equal(searchPayload.data.list[0].code, config.routeCode, `${titleText} first route must match configured code`)

  const routeRow = page
    .locator('.route-flow-config-panel-table .el-table__body-wrapper .el-table__row')
    .filter({ hasText: config.routeCode })
    .first()
  await routeRow.waitFor({ state: 'visible', timeout: 30000 })
  const openResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/process-config-list') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await routeRow.getByRole('button', { name: config.routeCode, exact: true }).click()
  const openResponse = await openResponsePromise
  const openPayload = await openResponse.json()
  assert.equal(openPayload.code, 0, `${titleText} config list failed: ${openPayload.msg || JSON.stringify(openPayload)}`)
  assert.ok(openPayload.data?.length > 0, `${titleText} route ${config.routeCode} must have process rows`)

  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('.route/flow-config-table .el-table__body-wrapper .el-table__row').first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  await assertNoLoadError(page, `${titleText} config`)
  return { dialog, rows: openPayload.data }
}

async function readSwitchStates(dialog) {
  const rows = dialog.locator('.route/flow-config-table .el-table__body-wrapper .el-table__row')
  const count = await rows.count()
  const states = []
  for (let index = 0; index < count; index += 1) {
    const row = rows.nth(index)
    const processCode = (await row.locator('td').nth(1).innerText()).trim()
    const switchLocator = row.locator('.el-switch').first()
    states.push({
      processCode,
      enabled: await switchLocator.evaluate((element) => element.classList.contains('is-checked'))
    })
  }
  return states
}

function invertStates(states) {
  return states.map((state) => ({
    processCode: state.processCode,
    enabled: !state.enabled
  }))
}

function assertSameStates(actual, expected, context) {
  const actualByCode = new Map(actual.map((state) => [state.processCode, state.enabled]))
  for (const state of expected) {
    assert.equal(
      actualByCode.get(state.processCode),
      state.enabled,
      `${context} process ${state.processCode} enabled state must follow schedule`
    )
  }
}

async function applySwitchStates(dialog, targetStates) {
  const targetByCode = new Map(targetStates.map((state) => [state.processCode, state.enabled]))
  const rows = dialog.locator('.route/flow-config-table .el-table__body-wrapper .el-table__row')
  const count = await rows.count()
  for (let index = 0; index < count; index += 1) {
    const row = rows.nth(index)
    const processCode = (await row.locator('td').nth(1).innerText()).trim()
    if (!targetByCode.has(processCode)) continue
    const switchLocator = row.locator('.el-switch').first()
    const current = await switchLocator.evaluate((element) => element.classList.contains('is-checked'))
    if (current !== targetByCode.get(processCode)) {
      await switchLocator.click()
    }
  }
}

async function saveSchedule(page, dialog) {
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/save') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '保存用途配置' }).click()
  const saveResponse = await saveResponsePromise
  const payload = await saveResponse.json()
  assert.equal(saveResponse.status(), 200, 'schedule save HTTP status must be 200')
  assert.equal(payload.code, 0, `schedule save failed: ${payload.msg || JSON.stringify(payload)}`)
  await page.getByText('用途配置保存成功').first().waitFor({ state: 'visible', timeout: 10000 })
  await settle(page)
}

async function closeDialog(dialog) {
  await dialog.getByRole('button', { name: '关闭' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function main() {
  const executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  const browser = await chromium.launch({ headless: !config.headed, executablePath })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const forbiddenMasterWrites = collectForbiddenMasterWrites(page)

  try {
    await login(page, '/mes/pro/route?tab=schedule-config')

    const scheduleBefore = await openRouteConfig(page, '/mes/pro/route?tab=schedule-config', '工艺流程排产配置')
    const originalStates = await readSwitchStates(scheduleBefore.dialog)
    assert.ok(originalStates.length > 0, 'schedule route must expose enabled switches')
    const targetStates = invertStates(originalStates)
    await applySwitchStates(scheduleBefore.dialog, targetStates)
    await saveSchedule(page, scheduleBefore.dialog)
    await closeDialog(scheduleBefore.dialog)

    const batchAfter = await openRouteConfig(page, '/mes/pro/route?tab=batch-record-config', '工艺流程批记录配置')
    const batchStatesAfter = await readSwitchStates(batchAfter.dialog)
    assertSameStates(batchStatesAfter, targetStates, 'batch route after schedule save')
    await closeDialog(batchAfter.dialog)

    const scheduleRestore = await openRouteConfig(page, '/mes/pro/route?tab=schedule-config', '工艺流程排产配置')
    await applySwitchStates(scheduleRestore.dialog, originalStates)
    await saveSchedule(page, scheduleRestore.dialog)
    await closeDialog(scheduleRestore.dialog)

    const batchRestored = await openRouteConfig(page, '/mes/pro/route?tab=batch-record-config', '工艺流程批记录配置')
    const restoredBatchStates = await readSwitchStates(batchRestored.dialog)
    assertSameStates(restoredBatchStates, originalStates, 'batch route after schedule restore')
    await closeDialog(batchRestored.dialog)

    assert.deepEqual(
      forbiddenMasterWrites,
      [],
      `original route/process CRUD was called: ${forbiddenMasterWrites.join(', ')}`
    )

    console.log(
      `PASS: MES batch route enabled follows schedule route. routeCode=${config.routeCode}, processes=${originalStates.length}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
