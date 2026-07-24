const path = require('path')
const assert = require('assert')
const { chromium } = require('playwright')

const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8088'
const backendUrl = process.env.BACKEND_URL || 'http://127.0.0.1:48081'
const executablePath = process.env.PLAYWRIGHT_CHROME_EXECUTABLE || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const tenant = '\u6d4b\u8bd5\u79df\u6237'
const username = 'aoteman'
const password = '111111'
const targetPath = '/mes/pro/feedback/edhr-batch-execution'

const waitBusinessResponse = (page, pathPart, method = 'GET') => page.waitForResponse(
  (response) => response.url().includes(pathPart) && response.request().method() === method,
  { timeout: 90000 }
)

async function login(page) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, { waitUntil: 'domcontentloaded' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(tenant)
    await tenantInput.press('Enter')
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"])').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const loginResponsePromise = waitBusinessResponse(page, '/system/auth/login', 'POST')
  await page.getByRole('button', { name: '\u767b\u5f55' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok() && [0, 200].includes(loginPayload.code), `login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function main() {
  const browser = await chromium.launch({
    headless: true,
    executablePath,
    args: ['--disable-dev-shm-usage', '--no-sandbox']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(90000)
  page.setDefaultNavigationTimeout(90000)

  const businessResponses = []
  page.on('response', async (response) => {
    const url = response.url()
    if (url.includes('/admin-api/mes/pro/edhr-batch-execution')) {
      businessResponses.push({ url, status: response.status(), method: response.request().method() })
    }
  })

  try {
    const health = await page.request.get(`${backendUrl}/actuator/health`)
    assert.strictEqual(health.status(), 200, 'backend health must be 200')

    await login(page)
    const listResponsePromise = waitBusinessResponse(page, '/mes/pro/edhr-batch-execution/page', 'GET')
    await page.goto(`${baseUrl}${targetPath}`, { waitUntil: 'domcontentloaded' })
    const listResponse = await listResponsePromise
    assert.strictEqual(listResponse.status(), 200, 'batch list response must be 200')
    await page.getByText('批次执行编码', { exact: false }).first().waitFor({ state: 'visible' })

    const firstRow = page.locator('.el-table__body tr').first()
    await firstRow.waitFor({ state: 'visible' })
    const firstRowText = await firstRow.innerText()
    assert.ok(!firstRowText.includes('复盘'), 'list row must not expose review direct action')
    assert.ok(!firstRowText.includes('模板'), 'list row must not expose template direct action')

    const detailResponsePromise = waitBusinessResponse(page, '/mes/pro/edhr-batch-execution/get', 'GET')
    const workbenchResponsePromise = waitBusinessResponse(page, '/mes/pro/edhr-batch-execution/workbench', 'GET')
    await firstRow.getByRole('button', { name: '详情' }).first().click()
    await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-batch-execution/detail'), { timeout: 90000 })
    const detailResponse = await detailResponsePromise
    const workbenchResponse = await workbenchResponsePromise
    assert.strictEqual(detailResponse.status(), 200, 'detail get response must be 200')
    assert.strictEqual(workbenchResponse.status(), 200, 'workbench response must be 200')

    for (const text of ['eDHR批次详情', '填写载体', '收尾/放行归档', '放行审批']) {
      await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible' })
    }

    const detailText = await page.locator('body').innerText()
    assert.ok(detailText.includes('批记录'), 'detail page must expose batch record fill carrier')
    assert.ok(detailText.includes('记录本'), 'detail page must expose notebook fill carrier')
    assert.ok(!detailText.includes('eDHR记录本'), 'detail page must not expose standalone eDHR recordbook entry')

    const routeProbe = await page.evaluate(() => {
      const paths = [
        '/mes/pro/edhr-work-task',
        '/mes/pro/feedback/edhr-signature',
        '/mes/pro/feedback/edhr-print-task',
        '/mes/pro/feedback/edhr-form-template',
        '/mes/pro/feedback/edhr-form-instance'
      ]
      return paths.map((path) => ({ path, resolved: window.__VUE_ROUTER__ ? 'unknown' : document.body.innerText.includes(path) }))
    }).catch(() => [])

    console.log(JSON.stringify({
      marker: 'EDHR_DEEP_DEDUP_LIST_DETAIL_REAL_E2E_PASS',
      baseUrl,
      backendUrl,
      tenant,
      username,
      currentUrl: page.url(),
      firstRowText: firstRowText.slice(0, 300),
      businessResponses,
      routeProbe
    }, null, 2))
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
