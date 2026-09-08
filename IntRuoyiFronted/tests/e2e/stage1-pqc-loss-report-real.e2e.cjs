const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.STAGE1_PQC_LOSS_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.STAGE1_PQC_LOSS_E2E_TENANT || ''
const USERNAME = process.env.STAGE1_PQC_LOSS_E2E_USERNAME || ''
const PASSWORD = process.env.STAGE1_PQC_LOSS_E2E_PASSWORD || ''
const TARGET_WORK_ORDER_CODE = process.env.STAGE1_PQC_LOSS_E2E_WORK_ORDER_CODE || ''
const CHROME_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const RUN_ID = `STAGE1-PQC-LOSS-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`
const RESULT_DIR = path.resolve(process.cwd(), 'output', 'playwright', 'stage1-pqc-loss-report', RUN_ID)
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')
const SCREENSHOT_FILE = path.join(RESULT_DIR, 'loss-report.png')

const evidence = {
  runId: RUN_ID,
  status: 'RUNNING',
  baseUrl: BASE_URL,
  tenant: TENANT,
  username: USERNAME,
  targetWorkOrderCode: TARGET_WORK_ORDER_CODE,
  detailUrl: null,
  lossReportText: null,
  screenshot: SCREENSHOT_FILE,
  consoleErrors: [],
  pageErrors: [],
  failedRequests: []
}

const persist = () => {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_FILE, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

const firstVisible = async (locator, label) => {
  for (let index = 0; index < (await locator.count()); index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`未找到可见控件：${label}`)
}

const clickMenu = async (page, text) => {
  const textNode = await firstVisible(page.locator('.el-menu').getByText(text, { exact: true }), text)
  const target = textNode.locator(
    'xpath=ancestor-or-self::*[contains(@class, "el-menu-item") or contains(@class, "el-sub-menu__title")][1]'
  )
  const targetClass = (await target.getAttribute('class')) || ''
  if (targetClass.includes('el-sub-menu__title')) {
    const submenu = target.locator('xpath=ancestor::li[contains(@class, "el-sub-menu")][1]')
    const submenuClass = (await submenu.getAttribute('class')) || ''
    if (!submenuClass.includes('is-opened')) await target.click()
    return
  }
  await target.click()
}

const login = async (page) => {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('.login-form:visible').first().waitFor({ state: 'visible', timeout: 60000 })
  const form = await firstVisible(page.locator('.login-form'), '登录表单')
  const tenantInput = await firstVisible(form.locator('input.el-select__input'), '租户')
  await tenantInput.click()
  await tenantInput.fill(TENANT)
  await page.waitForTimeout(300)
  await firstVisible(
    page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }),
    '租户选项'
  ).then((item) => item.click())
  await firstVisible(
    form.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'),
    '用户名'
  ).then((item) => item.fill(USERNAME))
  await firstVisible(form.locator('input[placeholder="请输入密码"]'), '密码').then((item) =>
    item.fill(PASSWORD)
  )
  await firstVisible(form.getByRole('button', { name: /^登录$/ }), '登录按钮').then((item) =>
    item.click()
  )
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 90000 })
}

const openActiveOrderPool = async (page) => {
  await clickMenu(page, 'MES 系统')
  await clickMenu(page, 'eDHR批记录')
  await clickMenu(page, '生产组长')
  await page.waitForURL((url) => url.pathname === '/mes/pro/process-pool/production-leader', {
    timeout: 60000
  })
  await page.getByRole('tab', { name: '活跃订单池' }).click()
  await page.locator('[data-team-leader-active-order-list]').waitFor({ state: 'visible', timeout: 60000 })
}

const targetRow = (page) =>
  page
    .locator('[data-team-leader-active-order-list] .el-table__body-wrapper tbody tr')
    .filter({ hasText: TARGET_WORK_ORDER_CODE })
    .first()

const clickStage1Simulation = async (page) => {
  const row = targetRow(page)
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const button = row.locator('[data-team-leader-simulate-active-order-stage1]').first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await button.isEnabled(), true, '目标订单 Stage1 模拟按钮不可用')
  await button.click()
  await firstVisible(page.getByRole('button', { name: '开始模拟' }), '开始模拟').then((item) =>
    item.click()
  )
  await page.waitForURL((url) => url.pathname.includes('/submission-detail'), { timeout: 180000 })
  evidence.detailUrl = page.url()
  persist()
}

const verifyLossReport = async (page) => {
  await page.locator('[data-team-leader-active-order-detail-page]').waitFor({
    state: 'visible',
    timeout: 90000
  })
  await page.getByRole('tab', { name: '生产过程损耗报告单' }).click()
  const table = page.locator('[data-active-order-pqc-loss-report-table]').first()
  await table.waitFor({ state: 'visible', timeout: 30000 })
  const tableText = await table.innerText()
  evidence.lossReportText = tableText
  persist()
  assert.ok(tableText.includes('生产过程损耗报告单'), '未显示生产过程损耗报告单')
  assert.ok(!tableText.includes('暂无生产过程损耗记录'), '损耗报告单仍为空')
  assert.match(tableText, /不合格数量[\s\S]*\b1\b/, '损耗报告单未显示 PQC 模拟损耗数量 1')
  await page.screenshot({ path: SCREENSHOT_FILE, fullPage: true })
}

async function main() {
  assert.ok(BASE_URL, '缺少 BASE_URL')
  assert.ok(TENANT && USERNAME && PASSWORD, '缺少真实登录信息')
  assert.ok(TARGET_WORK_ORDER_CODE, '缺少目标生产订单号')
  assert.ok(fs.existsSync(CHROME_EXECUTABLE), 'Chrome 不存在')
  persist()
  const browser = await chromium.launch({ headless: true, executablePath: CHROME_EXECUTABLE })
  const page = await browser.newPage({ viewport: { width: 1680, height: 950 } })
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    evidence.failedRequests.push({
      method: request.method(),
      url: request.url(),
      error: request.failure()?.errorText || 'unknown'
    })
  })
  try {
    await login(page)
    await openActiveOrderPool(page)
    await clickStage1Simulation(page)
    await verifyLossReport(page)
    evidence.status = 'PASS'
    persist()
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error.stack || error.message
    persist()
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
