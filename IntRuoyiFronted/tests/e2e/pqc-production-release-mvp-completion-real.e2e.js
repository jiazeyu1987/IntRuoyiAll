const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.PQC_RELEASE_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.PQC_RELEASE_E2E_TENANT || ''
const USERNAME = process.env.PQC_RELEASE_E2E_USERNAME || ''
const PASSWORD = process.env.PQC_RELEASE_E2E_PASSWORD || ''
const CHROME_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const RESULT_DIR = path.resolve(
  process.cwd(),
  'output',
  'playwright',
  'pqc-production-release-mvp-completion'
)
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')

const writeResult = (result) => {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_FILE, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

const firstVisible = async (locator, label) => {
  for (let index = 0; index < (await locator.count()); index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`未找到可见控件：${label}`)
}

const clickMenu = async (page, text) => {
  const targetText = await firstVisible(
    page.locator('.el-menu').getByText(text, { exact: true }),
    text
  )
  const target = targetText.locator(
    'xpath=ancestor-or-self::*[contains(@class, "el-menu-item") or contains(@class, "el-sub-menu__title")][1]'
  )
  await target.click()
  await page.waitForTimeout(250)
  return target
}

const login = async (page) => {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.login-form:visible').first().waitFor({ state: 'visible', timeout: 60000 })
  const form = await firstVisible(page.locator('.login-form'), '登录表单')
  const tenantInput = await firstVisible(form.locator('input.el-select__input'), '租户')
  await tenantInput.click()
  await tenantInput.fill(TENANT)
  await page.waitForTimeout(300)
  await firstVisible(
    page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: TENANT }),
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

async function main() {
  assert.equal(BASE_URL, 'http://127.0.0.1:8311')
  assert.ok(TENANT && USERNAME && PASSWORD, '真实登录输入缺失')
  assert.ok(fs.existsSync(CHROME_EXECUTABLE), `Chrome不存在：${CHROME_EXECUTABLE}`)
  const browser = await chromium.launch({ headless: true, executablePath: CHROME_EXECUTABLE })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const consoleErrors = []
  const pageErrors = []
  const failedRequests = []
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    failedRequests.push({
      method: request.method(),
      url: request.url(),
      error: request.failure()?.errorText || 'unknown'
    })
  })
  let result
  try {
    await login(page)
    await clickMenu(page, 'MES 系统')
    await clickMenu(page, 'eDHR批记录')
    const releaseMenu = page.locator('.el-menu').getByText('PQC生产放行', { exact: true })
    if (
      !(await releaseMenu
        .first()
        .isVisible()
        .catch(() => false))
    ) {
      const visibleMenuTexts = await page
        .locator('.el-menu:visible .el-menu-item:visible')
        .allInnerTexts()
      await page.screenshot({
        path: path.join(RESULT_DIR, 'release-menu-blocked.png'),
        fullPage: true
      })
      result = {
        status: 'BLOCKED',
        reason: '当前登录身份的真实菜单中没有PQC生产放行，无法进入真实页面入口。',
        tenant: TENANT,
        username: USERNAME,
        visibleMenuTexts,
        writeRequestCount: 0,
        consoleErrors,
        pageErrors,
        failedRequests
      }
      writeResult(result)
      process.exitCode = 2
      return
    }
    const pageResponsePromise = page.waitForResponse(
      (response) =>
        new URL(response.url()).pathname.endsWith('/mes/pro/production-release/pqc/page'),
      { timeout: 60000 }
    )
    const clickedReleaseMenu = await clickMenu(page, 'PQC生产放行')
    await page.waitForTimeout(1500)
    if (new URL(page.url()).pathname !== '/mes/production-release/pqc') {
      pageResponsePromise.catch(() => {})
      const menuIndex = await clickedReleaseMenu.getAttribute('index')
      const menuClass = await clickedReleaseMenu.getAttribute('class')
      await page.screenshot({
        path: path.join(RESULT_DIR, 'release-menu-click-blocked.png'),
        fullPage: true
      })
      result = {
        status: 'BLOCKED',
        reason: 'PQC生产放行菜单可见但点击后未进入页面。',
        tenant: TENANT,
        username: USERNAME,
        url: page.url(),
        menuIndex,
        menuClass,
        writeRequestCount: 0,
        consoleErrors,
        pageErrors,
        failedRequests
      }
      writeResult(result)
      process.exitCode = 2
      return
    }
    await page.waitForURL((url) => url.pathname === '/mes/production-release/pqc', {
      timeout: 60000
    })
    const pageResponse = await pageResponsePromise
    assert.equal(pageResponse.status(), 200)
    const pageBody = await pageResponse.json()
    assert.equal(pageBody.code, 0, pageBody.msg || 'PQC生产放行列表接口失败')

    const expectedTabs = ['待放行', '已放行', '已作废', '已返工', '已让步放行']
    for (const tab of expectedTabs) {
      await page.getByRole('tab', { name: tab }).waitFor({ state: 'visible', timeout: 30000 })
    }
    const pendingRows = page.locator('.el-table__body-wrapper tbody tr')
    const pendingRowCount = await pendingRows.count()
    if (pendingRowCount > 0) {
      const firstRow = pendingRows.first()
      await firstRow
        .getByRole('button', { name: '放行', exact: true })
        .waitFor({ state: 'visible' })
      await firstRow
        .getByRole('button', { name: '不合格审查', exact: true })
        .waitFor({ state: 'visible' })
      assert.equal(await firstRow.getByRole('button', { name: /拒绝|驳回/ }).count(), 0)
    }
    await page.screenshot({
      path: path.join(RESULT_DIR, 'pqc-production-release.png'),
      fullPage: true
    })
    result = {
      status: 'PASS',
      tenant: TENANT,
      username: USERNAME,
      url: page.url(),
      tabs: expectedTabs,
      pendingRowCount,
      writeRequestCount: 0,
      consoleErrors,
      pageErrors,
      failedRequests
    }
    writeResult(result)
    assert.equal(pageErrors.length, 0, `页面异常：${pageErrors.join('; ')}`)
  } catch (error) {
    await page
      .screenshot({ path: path.join(RESULT_DIR, 'failure.png'), fullPage: true })
      .catch(() => {})
    result = {
      status: 'FAIL',
      tenant: TENANT,
      username: USERNAME,
      url: page.url(),
      error: error instanceof Error ? error.message : String(error),
      writeRequestCount: 0,
      consoleErrors,
      pageErrors,
      failedRequests
    }
    writeResult(result)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
})
