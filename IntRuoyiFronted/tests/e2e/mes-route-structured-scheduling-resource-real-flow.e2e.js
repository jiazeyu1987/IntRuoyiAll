const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for MES route structured scheduling resource E2E.')
  }
}

const config = {
  baseUrl: (process.env.MES_ROUTE_RESOURCE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_RESOURCE_E2E_TENANT || '芋道源码',
  username: process.env.MES_ROUTE_RESOURCE_E2E_USERNAME || 'admin',
  password: process.env.MES_ROUTE_RESOURCE_E2E_PASSWORD || 'admin123',
  routeId: process.env.MES_ROUTE_RESOURCE_E2E_ROUTE_ID || '900026',
  machineProcessCode: process.env.MES_ROUTE_RESOURCE_E2E_MACHINE_PROCESS_CODE || 'B010',
  headed: process.env.MES_ROUTE_RESOURCE_E2E_HEADED === '1'
}

function formatCapacity(value) {
  if (value === undefined || value === null || value === '') {
    return '未配置'
  }
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return String(value)
  }
  return Number(numberValue.toFixed(4)).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

function formatResourceQuantity(row, value) {
  const numberValue = Number(value || 0)
  return row.capacitySource === 'WORKER'
    ? `${numberValue.toLocaleString('zh-CN')}人`
    : `${numberValue.toLocaleString('zh-CN')}台`
}

function finiteNumber(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : undefined
}

function formatShortageRatio(today, standard) {
  return `${formatCapacity(today)}/${formatCapacity(standard)}`
}

function expectedResourceText(row) {
  const standard = row.capacitySource === 'WORKER'
    ? finiteNumber(row.workerQuantityTotal)
    : finiteNumber(row.machineryQuantityTotal)
  const today = finiteNumber(row.todayAvailableResourceQuantityTotal)
  if (standard !== undefined && today !== undefined && today < standard) {
    return formatShortageRatio(today, standard)
  }
  return formatResourceQuantity(
    row,
    row.capacitySource === 'WORKER' ? row.workerQuantityTotal : row.machineryQuantityTotal
  )
}

function expectedShiftCapacityText(row) {
  const standard = finiteNumber(row.processShiftCapacityTotal)
  const today = finiteNumber(row.todayShiftCapacityTotal)
  if (standard !== undefined && today !== undefined && today < standard) {
    return formatShortageRatio(today, standard)
  }
  return formatCapacity(row.processShiftCapacityTotal)
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
  const processes = routeProcessBody.data || []
  const machineProcess = processes.find((row) => row.processCode === config.machineProcessCode)
  const workerProcess = processes.find((row) => row.capacitySource === 'WORKER')
  assert.ok(machineProcess, `缺少真实设备工序 ${config.machineProcessCode}`)
  assert.ok(workerProcess, '当前路线缺少真实人工工序')
  assert.equal(machineProcess.capacitySource, 'MACHINE', `${config.machineProcessCode} 应为设备产能来源`)
  assert.equal(workerProcess.capacitySource, 'WORKER', `${workerProcess.processCode} 应为人工产能来源`)
  for (const row of [machineProcess, workerProcess]) {
    assert.notEqual(row.processShiftCapacityTotal, undefined, `${row.processCode} 缺少标准班次产能`)
    assert.notEqual(row.todayShiftCapacityTotal, undefined, `${row.processCode} 缺少今日班次产能`)
    assert.notEqual(row.resourceStatus, undefined, `${row.processCode} 缺少资源状态`)
  }

  const detailDialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺路线详情' }).last()
  await detailDialog.waitFor({ state: 'visible', timeout: 30000 })
  return { detailDialog, machineProcess, workerProcess }
}

async function verifyRouteProcessTable(detailDialog, machineProcess, workerProcess) {
  const processTable = detailDialog.locator('.el-table').first()
  const headerText = await processTable.locator('thead').innerText()
  for (const label of ['资源类型', '标准资源', '标准班次产能', '资源状态']) {
    assert.ok(headerText.includes(label), `组成工序表格缺少 ${label} 表头。`)
  }
  for (const removedLabel of ['今日可用', '今日班次产能', '等待时间', '甘特图颜色']) {
    assert.ok(!headerText.includes(removedLabel), `组成工序表格不应显示 ${removedLabel} 表头。`)
  }
  assert.ok(!headerText.includes('准备时间'), '组成工序表格不应显示准备时间表头。')

  const machineRow = processTable.locator('tr.el-table__row').filter({ hasText: machineProcess.processCode }).first()
  await machineRow.waitFor({ state: 'visible', timeout: 30000 })
  const machineRowText = await machineRow.innerText()
  assert.ok(machineRowText.includes('设备'), `${machineProcess.processCode} 行缺少设备资源类型。`)
  assert.ok(machineRowText.includes(expectedResourceText(machineProcess)), `${machineProcess.processCode} 行缺少资源短缺展示。`)
  assert.ok(machineRowText.includes(expectedShiftCapacityText(machineProcess)), `${machineProcess.processCode} 行缺少班次产能短缺展示。`)

  const workerRow = processTable.locator('tr.el-table__row').filter({ hasText: workerProcess.processCode }).first()
  await workerRow.waitFor({ state: 'visible', timeout: 30000 })
  const workerRowText = await workerRow.innerText()
  assert.ok(workerRowText.includes('人工'), `${workerProcess.processCode} 行缺少人工资源类型。`)
  assert.ok(workerRowText.includes(expectedResourceText(workerProcess)), `${workerProcess.processCode} 行缺少资源短缺展示。`)
  assert.ok(workerRowText.includes(expectedShiftCapacityText(workerProcess)), `${workerProcess.processCode} 行缺少班次产能短缺展示。`)
}

async function closeTopDialog(page) {
  const closeButtons = page.locator('.el-dialog:visible .el-dialog__headerbtn')
  if ((await closeButtons.count()) > 0) {
    await closeButtons.last().click({ force: true })
  } else {
    await page.keyboard.press('Escape')
  }
  await page.waitForTimeout(500)
}

async function verifyMachineDialog(page, detailDialog, machineProcess) {
  const processRow = detailDialog.locator('tr.el-table__row').filter({ hasText: machineProcess.processCode }).first()
  await processRow.locator('.el-link').first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '设备列表' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const headerText = await dialog.locator('.el-table__header-wrapper').innerText()
  for (const label of ['今日可用', '单台产能/h', '单台产能/班次', '班次产能', '状态']) {
    assert.ok(headerText.includes(label), `设备列表缺少 ${label}。`)
  }
  const footerText = await dialog.locator('.el-dialog__footer').innerText()
  assert.ok(footerText.includes('标准总产能/h'), '设备列表底部缺少标准总小时产能。')
  assert.ok(footerText.includes('今日总产能/班次'), '设备列表底部缺少今日总班次产能。')
  assert.ok(footerText.includes(formatCapacity(machineProcess.todayShiftCapacityTotal)), '设备列表底部缺少今日班次产能值。')
  await closeTopDialog(page)
}

async function verifyWorkerDialog(page, detailDialog, workerProcess) {
  const processRow = detailDialog.locator('tr.el-table__row').filter({ hasText: workerProcess.processCode }).first()
  await processRow.locator('.el-link').first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '人工产能' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const dialogText = await dialog.innerText()
  for (const label of ['人数', '单人产能/h', '班次小时', '班次总产能', '1班次=10.5小时']) {
    assert.ok(dialogText.includes(label), `人工产能编辑区缺少 ${label}。`)
  }
  assert.ok(dialogText.includes(formatCapacity(workerProcess.processShiftCapacityTotal)), '人工产能编辑区缺少班次总产能值。')
  const workerQuantityInput = dialog.locator('.el-form-item').filter({ hasText: '人数' }).locator('input').first()
  assert.equal(Number(await workerQuantityInput.inputValue()), Number(workerProcess.workerQuantityTotal), '人工产能编辑区人数回显不正确。')
  assert.equal(await dialog.locator('.el-table:visible').count(), 0, '人工产能编辑区不应显示设备明细表。')
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
    console.log(`PASS: MES structured scheduling resource real UI E2E, worker=${workerProcess.processCode}`)
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
