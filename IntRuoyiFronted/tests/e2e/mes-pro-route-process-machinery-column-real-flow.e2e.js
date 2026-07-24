const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES route process machinery E2E. Run in a workspace where 'playwright' is installed.")
  }
}

const config = {
  baseUrl: (process.env.MES_ROUTE_PROCESS_MACHINERY_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_PROCESS_MACHINERY_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_PROCESS_MACHINERY_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_PROCESS_MACHINERY_E2E_PASSWORD || 'admin123',
  headed: process.env.MES_ROUTE_PROCESS_MACHINERY_E2E_HEADED === '1'
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

async function expectInputValue(locator, expectedValue, label) {
  await locator.waitFor({ state: 'visible', timeout: 10000 })
  const deadline = Date.now() + 10000
  while (Date.now() < deadline) {
    if ((await locator.inputValue()) === expectedValue) {
      return
    }
    await new Promise((resolve) => setTimeout(resolve, 200))
  }
  const actualValue = await locator.inputValue()
  assert.equal(actualValue, expectedValue, `${label} 应显示 ${expectedValue}`)
}

async function login(page) {
  await page.goto(config.baseUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), config.password, 'password')
  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    loginForm.locator('.el-button--primary').first().click()
  ])
  const loginBody = await loginResponse.json()
  if (loginBody.code !== 0) {
    throw new Error(`登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  }
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  if (page.url().includes('/login')) {
    await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  }
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openResourceWorkbench(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.waitForSelector('.route-view-switch', { timeout: 30000 })
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route-resource/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.locator('.route-view-switch').getByText('资源大表').click()
  ])
  await page.waitForSelector('.route-resource-workbench', { timeout: 30000 })
}

async function filterMachineResources(page) {
  const workbench = page.locator('.route-resource-workbench')
  await workbench.locator('.resource-toolbar__select').click()
  await page.getByRole('option', { name: '设备' }).click()
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route-resource/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    workbench.getByRole('button', { name: /搜索/ }).click()
  ])
  await settle(page)
}

async function readFirstMachineResource(page) {
  const rows = page.locator('.resource-table tr.el-table__row').filter({ hasText: '设备' })
  const count = await rows.count()
  assert.ok(count > 0, '资源大表中没有真实设备资源行，无法验证工艺路线详情设备列。')

  for (let index = 0; index < count; index += 1) {
    const row = rows.nth(index)
    const mainCells = row.locator('.cell-main')
    const subCells = row.locator('.cell-sub')
    if ((await mainCells.count()) < 4 || (await subCells.count()) < 4) {
      continue
    }
    const routeCode = (await mainCells.nth(0).innerText()).trim()
    const processName = (await subCells.nth(1).innerText()).trim()
    const machineryCode = (await mainCells.nth(3).innerText()).trim()
    if (routeCode && processName && machineryCode && machineryCode !== '-') {
      console.log(`E2E_SELECTED_RESOURCE: route=${routeCode}, process=${processName}, machinery=${machineryCode}`)
      return { routeCode, processName, machineryCode }
    }
  }
  throw new Error('资源大表中没有设备编码不为空的真实设备资源行，无法验证工艺路线详情设备列。')
}

async function openRouteDetail(page, routeCode) {
  await page.locator('.route-view-switch').getByText('工艺路线').click()
  await settle(page)
  await page.locator('input[placeholder="请输入工艺路线编码"]').fill(routeCode)
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: /搜索/ }).click()
  ])
  await settle(page)
  const [routeProcessResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route-process/list-by-route') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: routeCode }).first().click()
  ])
  const routeProcessBody = await routeProcessResponse.json()
  console.log('E2E_ROUTE_PROCESS_URL:', routeProcessResponse.url())
  const firstMachineryProcess = routeProcessBody.data?.find?.((row) => row.machineryQuantityTotal > 0)
  console.log('E2E_ROUTE_PROCESS_SAMPLE:', JSON.stringify(firstMachineryProcess || routeProcessBody.data?.[0] || null))
  const detailDialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺路线详情' }).last()
  await detailDialog.waitFor({ state: 'visible', timeout: 30000 })
  return detailDialog
}

async function verifyMachineryColumn(page, detailDialog, processName, machineryCode) {
  const processTable = detailDialog.locator('.el-table').first()
  const headerText = await processTable.locator('thead').innerText()
  const headers = headerText.split(/\s+/).filter(Boolean)
  assert.ok(headers.includes('设备'), '组成工序表格缺少“设备”表头。')
  assert.ok(!headers.includes('下一道工序'), '组成工序表格不应展示“下一道工序”表头。')

  const processRow = processTable.locator('tr.el-table__row').filter({ hasText: processName }).first()
  await processRow.waitFor({ state: 'visible', timeout: 30000 })
  const deviceEntry = processRow.locator('.el-link').filter({ hasText: /\d+\s*台/ }).first()
  await deviceEntry.waitFor({ state: 'visible', timeout: 10000 })
  const quantity = Number((await deviceEntry.innerText()).replace(/[^\d]/g, ''))
  assert.ok(quantity > 0, `工序 ${processName} 的设备数量应大于 0。`)

  await deviceEntry.click()
  const machineryDialog = page.locator('.el-dialog:visible').filter({ hasText: '设备列表' }).last()
  await machineryDialog.waitFor({ state: 'visible', timeout: 30000 })
  await machineryDialog.getByText(machineryCode, { exact: true }).waitFor({ state: 'visible', timeout: 10000 })
  const [machineryDetailResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/dv/machinery/get') && response.status() === 200,
      { timeout: 60000 }
    ),
    machineryDialog.getByText(machineryCode, { exact: true }).click()
  ])
  const machineryDetailBody = await machineryDetailResponse.json()
  if (machineryDetailBody.code !== 0) {
    throw new Error(
      `设备详情接口返回业务错误: code=${machineryDetailBody.code}, msg=${machineryDetailBody.msg || ''}`
    )
  }

  const detailMachineryDialog = page.locator('.el-dialog:visible').filter({ hasText: '查看设备' }).last()
  await detailMachineryDialog.waitFor({ state: 'visible', timeout: 30000 })
  await expectInputValue(detailMachineryDialog.locator('input').first(), machineryCode, '设备编码')
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    await openResourceWorkbench(page)
    await filterMachineResources(page)
    const resource = await readFirstMachineResource(page)
    const detailDialog = await openRouteDetail(page, resource.routeCode)
    await verifyMachineryColumn(page, detailDialog, resource.processName, resource.machineryCode)
    assert.deepEqual(pageErrors, [])

    const screenshotDir = path.resolve(__dirname, '../../output/playwright')
    fs.mkdirSync(screenshotDir, { recursive: true })
    await page.screenshot({
      path: path.join(screenshotDir, 'mes-pro-route-process-machinery-column-real-flow.png'),
      fullPage: true
    })
    console.log('PASS: MES route process machinery column real UI E2E')
  } catch (error) {
    console.error('E2E_FAILURE_URL:', page.url())
    console.error('E2E_PAGE_ERRORS:', JSON.stringify(pageErrors))
    console.error('E2E_FAILURE_BODY:', (await page.locator('body').innerText().catch((innerError) => String(innerError))).slice(0, 4000))
    const screenshotDir = path.resolve(__dirname, '../../output/playwright')
    fs.mkdirSync(screenshotDir, { recursive: true })
    await page.screenshot({
      path: path.join(screenshotDir, 'mes-pro-route-process-machinery-column-failure.png'),
      fullPage: true
    }).catch(() => {})
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
