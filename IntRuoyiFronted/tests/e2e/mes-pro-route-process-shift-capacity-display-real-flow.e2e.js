const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES route process shift capacity display E2E. Run in a workspace where 'playwright' is installed.")
  }
}

const config = {
  baseUrl: (process.env.MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_TENANT || '芋道源码',
  username: process.env.MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_USERNAME || 'admin',
  password: process.env.MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_PASSWORD || 'admin123',
  routeId: process.env.MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_ROUTE_ID || '900026',
  machineProcessCode: process.env.MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_MACHINE_PROCESS_CODE || 'B010',
  workerProcessCode: process.env.MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_WORKER_PROCESS_CODE || '',
  headed: process.env.MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_HEADED === '1'
}

function formatShiftCapacity(value) {
  if (value === undefined || value === null || value === '') {
    return '未配置'
  }
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return String(value)
  }
  return Number(numberValue.toFixed(4)).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
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
  const machineProcess = routeProcessBody.data?.find?.((row) => row.processCode === config.machineProcessCode)
  const workerProcess = config.workerProcessCode
    ? routeProcessBody.data?.find?.((row) => row.processCode === config.workerProcessCode)
    : routeProcessBody.data?.find?.(
      (row) => row.capacitySource === 'WORKER' && Number(row.machineryQuantityTotal || 0) === 0
    )
  assert.ok(machineProcess, `缺少真实设备工序 ${config.machineProcessCode}`)
  assert.ok(workerProcess, config.workerProcessCode
    ? `缺少真实人工工序 ${config.workerProcessCode}`
    : '当前路线缺少真实人工工序')
  assert.equal(machineProcess.capacitySource, 'MACHINE', `${config.machineProcessCode} 应为设备产能来源`)
  assert.equal(workerProcess.capacitySource, 'WORKER', `${config.workerProcessCode} 应为人工产能来源`)
  assert.equal(workerProcess.workerQuantityTotal, 5, `${workerProcess.processCode} 应显示默认 5 人`)
  console.log('E2E_SELECTED_WORKER_PROCESS:', workerProcess.processCode)

  const detailDialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺路线详情' }).last()
  await detailDialog.waitFor({ state: 'visible', timeout: 30000 })
  return { detailDialog, machineProcess, workerProcess }
}

async function verifyRouteProcessTable(detailDialog, machineProcess, workerProcess) {
  const processTable = detailDialog.locator('.el-table').first()
  const headerText = await processTable.locator('thead').innerText()
  assert.ok(headerText.includes('班次产能'), '组成工序表格缺少班次产能表头。')
  assert.ok(!headerText.includes('准备时间'), '组成工序表格不应显示准备时间表头。')

  const machineRow = processTable.locator('tr.el-table__row').filter({ hasText: machineProcess.processCode }).first()
  await machineRow.waitFor({ state: 'visible', timeout: 30000 })
  assert.ok(
    (await machineRow.innerText()).includes(formatShiftCapacity(machineProcess.processShiftCapacityTotal)),
    `${machineProcess.processCode} 行缺少设备班次产能。`
  )

  const workerRow = processTable.locator('tr.el-table__row').filter({ hasText: workerProcess.processCode }).first()
  await workerRow.waitFor({ state: 'visible', timeout: 30000 })
  assert.ok((await workerRow.innerText()).includes(`${workerProcess.workerQuantityTotal}人`),
    `${workerProcess.processCode} 行缺少人工人数入口。`)
  assert.ok(
    (await workerRow.innerText()).includes(formatShiftCapacity(workerProcess.processShiftCapacityTotal)),
    `${workerProcess.processCode} 行缺少人工班次产能。`
  )
}

async function verifyMachineDialog(page, detailDialog, machineProcess) {
  const processRow = detailDialog.locator('tr.el-table__row').filter({ hasText: machineProcess.processCode }).first()
  await processRow.locator('.el-link').filter({ hasText: /\d+\s*台/ }).first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '设备列表' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const headerText = await dialog.locator('.el-table__header-wrapper').innerText()
  assert.ok(headerText.includes('单台产能/班次'), '设备列表缺少单台产能/班次。')
  const footerText = await dialog.locator('.el-dialog__footer').innerText()
  assert.ok(footerText.includes('总产能/班次'), '设备列表底部缺少总产能/班次。')
  const closeButtons = page.locator('.el-dialog:visible .el-dialog__headerbtn')
  if ((await closeButtons.count()) > 0) {
    await closeButtons.last().click({ force: true })
  } else {
    await page.keyboard.press('Escape')
  }
  await page.waitForTimeout(500)
}

async function verifyWorkerDialog(page, detailDialog, workerProcess) {
  const processRow = detailDialog.locator('tr.el-table__row').filter({ hasText: workerProcess.processCode }).first()
  await processRow.locator('.el-link').filter({ hasText: /\d+\s*人/ }).first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '人工产能' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const dialogText = await dialog.innerText()
  assert.ok(dialogText.includes('人数'), '人工产能编辑区缺少人数。')
  assert.ok(dialogText.includes('单人产能/h'), '人工产能编辑区缺少单人产能。')
  assert.ok(dialogText.includes('班次小时'), '人工产能编辑区缺少班次小时。')
  assert.ok(dialogText.includes('班次总产能'), '人工产能编辑区缺少班次总产能。')
  assert.ok(dialogText.includes(formatShiftCapacity(workerProcess.processShiftCapacityTotal)), '人工产能弹窗缺少总班次产能值。')
  assert.ok(dialogText.includes('1班次=10.5小时'), '人工产能弹窗缺少班次时长说明。')
  const workerQuantityInput = dialog.locator('.el-form-item').filter({ hasText: '人数' }).locator('input').first()
  assert.equal(Number(await workerQuantityInput.inputValue()), Number(workerProcess.workerQuantityTotal), '人工产能编辑区人数回显不正确。')
  assert.equal(await dialog.locator('.el-table:visible').count(), 0, '人工产能弹窗不应显示设备明细表。')
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
    const { detailDialog, machineProcess, workerProcess } = await openRouteDetail(page)
    await verifyRouteProcessTable(detailDialog, machineProcess, workerProcess)
    await verifyMachineDialog(page, detailDialog, machineProcess)
    await verifyWorkerDialog(page, detailDialog, workerProcess)
    assert.deepEqual(pageErrors, [])
    console.log('PASS: MES route process shift capacity display real UI E2E')
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
