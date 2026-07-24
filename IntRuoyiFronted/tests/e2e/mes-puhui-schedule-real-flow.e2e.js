const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES Puhui schedule E2E. Run in a workspace where 'playwright' is installed.")
  }
}

const config = {
  baseUrl: (process.env.MES_PUHUI_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_PUHUI_E2E_TENANT || '测试租户',
  username: process.env.MES_PUHUI_E2E_USERNAME || 'aoteman',
  password: process.env.MES_PUHUI_E2E_PASSWORD || 'admin123',
  headed: process.env.MES_PUHUI_E2E_HEADED === '1'
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
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/puhui-schedule`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (page.url().includes('/login')) {
    const loginForm = page.locator('.login-form:visible').first()
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
    await Promise.all([
      page.waitForResponse(
        (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
        { timeout: 60000 }
      ),
      loginForm.locator('.el-button--primary').first().click()
    ])
  }

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openPuhuiSchedule(page) {
  await page.goto(`${config.baseUrl}/mes/pro/puhui-schedule`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.waitForSelector('text=璞慧排产', { timeout: 30000 })
  await page.evaluate(() => {
    window.localStorage.removeItem('liteScheduler.scenario.v1')
    window.localStorage.removeItem('liteScheduler.scenario.snapshots.v1')
  })
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await page.waitForSelector('text=璞慧排产', { timeout: 30000 })
}

async function addLine(page) {
  await page.getByRole('tab', { name: '产线管理' }).click()
  await page.locator('input[placeholder="产线名称"]').fill('E2E导管产线')
  await fillFirstVisible(page.locator('.puhui-tab-toolbar .el-input-number input'), '120', 'line base capacity')
  await page.getByRole('button', { name: /新增产线/ }).click()
  await page.waitForFunction(() => document.body.innerText.includes('E2E导管产线'), { timeout: 10000 })
}

async function addQuantityOrder(page) {
  await page.getByRole('tab', { name: '订单录入' }).click()
  await page.getByRole('button', { name: /新增订单/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增订单' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  const optionalInputs = dialog.locator('input[placeholder="可选"]')
  await optionalInputs.nth(0).fill('E2E-QTY-001')
  await optionalInputs.nth(1).fill('数量模式产品')
  await optionalInputs.nth(2).fill('S-QTY')
  await optionalInputs.nth(3).fill('B-QTY')
  await dialog.locator('.puhui-allocation-row .el-input-number input').first().fill('60')
  await dialog.getByRole('button', { name: '创建订单' }).click()
  await page.waitForSelector('text=E2E-QTY-001', { timeout: 10000 })
}

async function exportSchedule(page) {
  await page.getByRole('tab', { name: '每日排产' }).click()
  await page.waitForSelector('text=导出已排订单', { timeout: 10000 })
  const downloadPromise = page.waitForEvent('download', { timeout: 30000 })
  await page.getByRole('button', { name: /导出已排订单/ }).click()
  const download = await downloadPromise
  assert.match(download.suggestedFilename(), /^lite排产订单_\d{4}-\d{2}-\d{2}\.xls$/)
}

async function saveSnapshot(page) {
  await page.getByRole('button', { name: /保存场景/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '保存场景' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await dialog.locator('input[placeholder="场景名称"]').fill('E2E璞慧排产场景')
  await dialog.getByRole('button', { name: '保存到本地' }).click()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await page.keyboard.press('Escape')
  await dialog.waitFor({ state: 'hidden', timeout: 10000 })
}

async function addDurationOrderAndFinish(page) {
  await page.locator('.puhui-toolbar .el-radio-button').filter({ hasText: '按天数排产' }).click()
  await page.getByRole('tab', { name: '订单录入' }).click()
  await page.getByRole('button', { name: /新增订单/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增订单' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  const optionalInputs = dialog.locator('input[placeholder="可选"]')
  await optionalInputs.nth(0).fill('E2E-DAY-001')
  await optionalInputs.nth(1).fill('天数模式产品')
  const firstAllocationInputs = dialog.locator('.puhui-allocation-row').first().locator('.el-input-number input')
  await firstAllocationInputs.nth(0).fill('2')
  await firstAllocationInputs.nth(1).fill('10')
  await dialog.getByRole('button', { name: '创建订单' }).click()
  await page.waitForSelector('text=E2E-DAY-001', { timeout: 10000 })

  await page.getByRole('tab', { name: '每日排产' }).click()
  await page.locator('button.puhui-order-chip', { hasText: 'E2E-DAY-001' }).first().click()
  const finishDialog = page.locator('.el-dialog:visible').filter({ hasText: '手动报结束' }).last()
  await finishDialog.waitFor({ state: 'visible', timeout: 10000 })
  await finishDialog.getByRole('button', { name: '保存报结束' }).click()
  await page.waitForFunction(() => document.body.innerText.includes('已结束'), { timeout: 10000 })
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    await openPuhuiSchedule(page)
    await addLine(page)
    await addQuantityOrder(page)
    await exportSchedule(page)
    await saveSnapshot(page)
    await addDurationOrderAndFinish(page)
    await page.getByRole('button', { name: /推进1天/ }).click()
    await page.waitForTimeout(500)
    const storageState = await page.evaluate(() => ({
      scenario: window.localStorage.getItem('liteScheduler.scenario.v1'),
      snapshots: window.localStorage.getItem('liteScheduler.scenario.snapshots.v1')
    }))
    assert.ok(storageState.scenario, 'scenario must be written to localStorage')
    assert.ok(storageState.snapshots, 'snapshot rows must be written to localStorage')
    assert.deepEqual(pageErrors, [])
    console.log('PASS: MES Puhui schedule real UI E2E')
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
