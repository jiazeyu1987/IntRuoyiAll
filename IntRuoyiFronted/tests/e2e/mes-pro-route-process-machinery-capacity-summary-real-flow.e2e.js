const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES route process machinery capacity summary E2E. Run in a workspace where 'playwright' is installed.")
  }
}

const config = {
  baseUrl: (process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_TENANT || '芋道源码',
  username: process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_USERNAME || 'admin',
  password: process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_PASSWORD || 'admin123',
  routeId: process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_ROUTE_ID || '900026',
  processCode: process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_PROCESS_CODE || 'B010',
  processName: process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_PROCESS_NAME || '吹球囊成型',
  machineryCode: process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_MACHINERY_CODE || 'A03190',
  expectedSingleHourlyCapacity: '9.52381',
  expectedSingleShiftCapacity: '100',
  expectedTotalHourlyCapacity: '47.61905',
  expectedTotalShiftCapacity: '500',
  headed: process.env.MES_ROUTE_PROCESS_MACHINERY_SUMMARY_E2E_HEADED === '1'
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
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')

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
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openRouteDetail(page) {
  const routeProcessResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process/list-by-route') &&
      response.url().includes(`routeId=${config.routeId}`) &&
      response.status() === 200,
    { timeout: 60000 }
  )

  await page.goto(`${config.baseUrl}/mes/pro/route?openId=${config.routeId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const routeProcessResponse = await routeProcessResponsePromise
  const routeProcessBody = await routeProcessResponse.json()
  if (routeProcessBody.code !== 0) {
    throw new Error(`工艺路线工序接口返回业务错误: ${routeProcessBody.msg || routeProcessBody.code}`)
  }

  const targetProcess = routeProcessBody.data?.find?.(
    (row) => row.processCode === config.processCode && row.processName === config.processName
  )
  assert.ok(targetProcess, `工艺路线 ${config.routeId} 缺少 ${config.processCode} ${config.processName} 真实工序。`)

  const detailDialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺路线详情' }).last()
  await detailDialog.waitFor({ state: 'visible', timeout: 30000 })
  return detailDialog
}

async function verifyMachineryCapacitySummary(page, detailDialog) {
  const processRow = detailDialog
    .locator('tr.el-table__row')
    .filter({ hasText: config.processCode })
    .filter({ hasText: config.processName })
    .first()
  await processRow.waitFor({ state: 'visible', timeout: 30000 })

  const deviceEntry = processRow.locator('.el-link').filter({ hasText: /\d+\s*台/ }).first()
  await deviceEntry.waitFor({ state: 'visible', timeout: 10000 })
  await deviceEntry.click()

  const machineryDialog = page.locator('.el-dialog:visible').filter({ hasText: '设备列表' }).last()
  await machineryDialog.waitFor({ state: 'visible', timeout: 30000 })

  const headerText = await machineryDialog.locator('.el-table__header-wrapper').innerText()
  assert.ok(headerText.includes('单台产能/h'), '设备列表表头缺少单台产能/h。')
  assert.ok(headerText.includes('单台产能/班次'), '设备列表表头缺少单台产能/班次。')
  assert.ok(!headerText.includes('总产能/h'), '总产能/h 不应继续显示在行级表头。')

  const bodyText = await machineryDialog.locator('.el-table__body-wrapper').innerText()
  assert.ok(bodyText.includes(config.machineryCode), `设备列表缺少真实设备 ${config.machineryCode}。`)
  assert.ok(
    bodyText.includes(config.expectedSingleHourlyCapacity),
    `设备列表缺少单台小时产能 ${config.expectedSingleHourlyCapacity}。`
  )
  assert.ok(
    bodyText.includes(config.expectedSingleShiftCapacity),
    `设备列表缺少单台班次产能 ${config.expectedSingleShiftCapacity}。`
  )

  const footerText = await machineryDialog.locator('.el-dialog__footer').innerText()
  assert.ok(footerText.includes('1班次=10.5小时'), '设备列表底部缺少班次小时说明。')
  assert.ok(footerText.includes('总产能/h'), '设备列表底部缺少总产能/h。')
  assert.ok(footerText.includes('总产能/班次'), '设备列表底部缺少总产能/班次。')
  assert.ok(
    footerText.includes(config.expectedTotalHourlyCapacity),
    `设备列表底部缺少总小时产能 ${config.expectedTotalHourlyCapacity}。`
  )
  assert.ok(
    footerText.includes(config.expectedTotalShiftCapacity),
    `设备列表底部缺少总班次产能 ${config.expectedTotalShiftCapacity}。`
  )

  console.log('E2E_MACHINERY_CAPACITY_SUMMARY_HEADER:', headerText.replace(/\s+/g, ' ').trim())
  console.log('E2E_MACHINERY_CAPACITY_SUMMARY_FOOTER:', footerText.replace(/\s+/g, ' ').trim())
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
    const detailDialog = await openRouteDetail(page)
    await verifyMachineryCapacitySummary(page, detailDialog)
    assert.deepEqual(pageErrors, [])
    console.log('PASS: MES route process machinery capacity summary real UI E2E')
  } catch (error) {
    console.error('E2E_FAILURE_URL:', page.url())
    console.error('E2E_PAGE_ERRORS:', JSON.stringify(pageErrors))
    console.error(
      'E2E_FAILURE_BODY:',
      (await page.locator('body').innerText().catch((innerError) => String(innerError))).slice(0, 4000)
    )
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
