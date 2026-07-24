const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_RESP_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_RESP_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_RESP_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_RESP_E2E_PASSWORD || '111111',
  writerUsername: process.env.MES_ROUTE_RESP_WRITE_USERNAME || 'aoteman',
  writerPassword: process.env.MES_ROUTE_RESP_WRITE_PASSWORD || '111111',
  scheduleReadRouteCode: process.env.MES_ROUTE_RESP_SCHEDULE_READ_ROUTE_CODE || 'ROUTE-XLSX-00001',
  batchReadRouteCode:
    process.env.MES_ROUTE_RESP_BATCH_READ_ROUTE_CODE || 'ROUTE-YXN.044.02.1020',
  scheduleWriteRouteCode:
    process.env.MES_ROUTE_RESP_SCHEDULE_WRITE_ROUTE_CODE || 'ROUTE-YXN.069.001.1001',
  batchWriteRouteCode:
    process.env.MES_ROUTE_RESP_BATCH_WRITE_ROUTE_CODE || 'ROUTE-YXN.069.001.1001',
  headed: process.env.MES_ROUTE_RESP_E2E_HEADED === '1'
}

if (config.tenant !== '测试租户') {
  throw new Error(`MES route responsibility E2E must use 测试租户, got ${config.tenant}`)
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

async function selectLoginTenant(loginForm, tenant) {
  const tenantSelectInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantSelectInput.count()) > 0 && (await tenantSelectInput.isVisible())) {
    await tenantSelectInput.click()
    await tenantSelectInput.fill(tenant)
    await tenantSelectInput.press('Enter')
    return
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
}

async function login(page, redirectPath, username, password, tenant = config.tenant) {
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

  await selectLoginTenant(loginForm, tenant)
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
    { timeout: 30000 }
  )
  await loginForm.locator('.el-button--primary').first().click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.equal(payload.code, 0, `login failed for ${username}: ${payload.msg || JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function assertNoLoadError(page, context) {
  const alert = page.locator('.route-flow-config-panel-alert.el-alert--error:visible').first()
  if ((await alert.count()) > 0 && (await alert.isVisible())) {
    throw new Error(`${context} failed: ${await alert.innerText()}`)
  }
}

async function searchRouteByCode(page, routeCode, titleText) {
  const codeInput = page.locator('input[placeholder="请输入工艺路线编码"]').first()
  await codeInput.waitFor({ state: 'visible', timeout: 30000 })
  await codeInput.fill(routeCode)

  const searchResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: '搜索' }).click()
  const searchResponse = await searchResponsePromise
  const searchPayload = await searchResponse.json()
  assert.equal(
    searchPayload.code,
    0,
    `${titleText} route query failed: ${searchPayload.msg || JSON.stringify(searchPayload)}`
  )
  await settle(page)
}

async function openRouteConfig(page, routePathValue, titleText, routeCode) {
  await page.goto(`${config.baseUrl}${routePathValue}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.locator('.route-flow-config-panel-page').waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText(titleText, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await assertNoLoadError(page, titleText)

  await searchRouteByCode(page, routeCode, titleText)

  const routeRow = page
    .locator('.route-flow-config-panel-table .el-table__body-wrapper .el-table__row')
    .filter({ hasText: routeCode })
    .first()
  await routeRow.waitFor({ state: 'visible', timeout: 30000 })
  const routeCount = await page
    .locator('.route-flow-config-panel-table .el-table__body-wrapper .el-table__row')
    .count()
  assert.ok(routeCount > 0, `${titleText} must show at least one route row after search`)
  const currentRouteCode = (await routeRow.locator('td').nth(0).innerText()).trim()
  assert.equal(currentRouteCode, routeCode, `${titleText} first matched row must be ${routeCode}`)

  const openResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/process-config-list') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await routeRow.getByRole('button', { name: routeCode, exact: true }).click()
  const openResponse = await openResponsePromise
  const openPayload = await openResponse.json()
  assert.equal(openPayload.code, 0, `${titleText} config list failed: ${openPayload.msg || JSON.stringify(openPayload)}`)

  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const processRows = dialog.locator('.route/flow-config-table .el-table__body-wrapper .el-table__row')
  await processRows.first().waitFor({ state: 'visible', timeout: 30000 })
  const processCount = await processRows.count()
  assert.ok(processCount > 0, `${titleText} must show process rows`)
  await assertNoLoadError(page, `${titleText} config`)

  return { dialog, routeCode, routeCount, processCount }
}

async function countEnabledRows(dialog) {
  return await dialog.locator('.route/flow-config-table .el-switch.is-checked').count()
}

async function saveScheduleWithWriter(page, routePathValue, routeCode) {
  await login(page, routePathValue, config.writerUsername, config.writerPassword)
  const { dialog } = await openRouteConfig(page, routePathValue, '工艺流程排产配置', routeCode)
  const enabledCountBeforeSave = await countEnabledRows(dialog)
  assert.equal(enabledCountBeforeSave, 0, '工艺流程排产配置零配置路线默认必须全部未启用')

  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/save') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '保存用途配置' }).click()
  const saveResponse = await saveResponsePromise
  const payload = await saveResponse.json()
  assert.equal(saveResponse.status(), 200, '工艺流程排产配置保存 HTTP 状态必须为 200')
  assert.equal(payload.code, 0, `工艺流程排产配置保存失败: ${payload.msg || JSON.stringify(payload)}`)
  await page.getByText('用途配置保存成功').first().waitFor({ state: 'visible', timeout: 10000 })
  await assertNoLoadError(page, '工艺流程排产配置保存回刷')

  await dialog.getByRole('button', { name: '关闭' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  return { routeCode, enabledCountBeforeSave }
}

async function saveBatchWithWriter(page, routePathValue, routeCode) {
  await login(page, routePathValue, config.writerUsername, config.writerPassword)
  const { dialog } = await openRouteConfig(page, routePathValue, '工艺流程批记录配置', routeCode)
  const enabledCountBeforeSave = await countEnabledRows(dialog)
  assert.ok(enabledCountBeforeSave > 0, '工艺流程批记录配置写入验证路线必须已存在可保存配置')

  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/save') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '保存用途配置' }).click()
  const saveResponse = await saveResponsePromise
  const payload = await saveResponse.json()
  assert.equal(saveResponse.status(), 200, '工艺流程批记录配置保存 HTTP 状态必须为 200')
  assert.equal(payload.code, 0, `工艺流程批记录配置保存失败: ${payload.msg || JSON.stringify(payload)}`)
  await page.getByText('用途配置保存成功').first().waitFor({ state: 'visible', timeout: 10000 })
  await assertNoLoadError(page, '工艺流程批记录配置保存回刷')

  await dialog.getByRole('button', { name: '关闭' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  return { routeCode, enabledCountBeforeSave }
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const forbiddenMasterWrites = collectForbiddenMasterWrites(page)

  try {
    await login(page, '/mes/pro/route?tab=schedule-config', config.username, config.password)
    const scheduleRead = await openRouteConfig(
      page,
      '/mes/pro/route?tab=schedule-config',
      '工艺流程排产配置',
      config.scheduleReadRouteCode
    )
    scheduleRead.enabledCount = await countEnabledRows(scheduleRead.dialog)
    assert.equal(scheduleRead.enabledCount, 0, '工艺流程排产配置空态读取默认必须全部未启用')
    await scheduleRead.dialog.getByRole('button', { name: '关闭' }).click()
    await scheduleRead.dialog.waitFor({ state: 'hidden', timeout: 30000 })

    await login(page, '/mes/pro/route?tab=batch-record-config', config.username, config.password)
    const batchRead = await openRouteConfig(
      page,
      '/mes/pro/route?tab=batch-record-config',
      '工艺流程批记录配置',
      config.batchReadRouteCode
    )
    batchRead.enabledCount = await countEnabledRows(batchRead.dialog)
    assert.equal(batchRead.enabledCount, 0, '工艺流程批记录配置空态读取默认必须全部未启用')
    await batchRead.dialog.getByRole('button', { name: '关闭' }).click()
    await batchRead.dialog.waitFor({ state: 'hidden', timeout: 30000 })

    const scheduleWrite = await saveScheduleWithWriter(
      page,
      '/mes/pro/route?tab=schedule-config',
      config.scheduleWriteRouteCode
    )
    const batchWrite = await saveBatchWithWriter(
      page,
      '/mes/pro/route?tab=batch-record-config',
      config.batchWriteRouteCode
    )

    assert.deepEqual(
      forbiddenMasterWrites,
      [],
      `original route/process CRUD was called: ${forbiddenMasterWrites.join(', ')}`
    )

    console.log(
      JSON.stringify(
        {
          ok: true,
          readOnlyUser: config.username,
          writerUser: config.writerUsername,
          scheduleRead,
          batchRead,
          scheduleWrite,
          batchWrite
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
