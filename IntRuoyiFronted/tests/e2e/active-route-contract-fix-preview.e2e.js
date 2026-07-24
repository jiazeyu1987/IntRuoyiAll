const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_ROUTE_FIX_BASE_URL
const PASSWORD = process.env.EDHR_ROUTE_FIX_ADMIN_PASSWORD
const BATCH_EXECUTION_ID = Number(process.env.EDHR_ROUTE_FIX_BATCH_ID || 900000000480)
const TASK_ID = Number(process.env.EDHR_ROUTE_FIX_TASK_ID || 3041)
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const OUTPUT_DIR = path.resolve(
  'D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260710-active-route-contract-fix/e2e-output'
)

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible')
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  await tenantInput.click()
  await tenantInput.fill('芋道源码')
  const tenantOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', {
    hasText: '芋道源码'
  })
  if ((await tenantOption.count()) > 0) {
    await tenantOption.first().click()
  } else {
    await tenantInput.press('Enter')
  }
  await form
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
    .fill('admin')
  await form.locator('input[type="password"]:visible').first().fill(PASSWORD)
  await form.getByRole('button', { name: /登录/ }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function run() {
  assert.match(BASE_URL || '', /^http:\/\/(127\.0\.0\.1|localhost):\d+$/)
  assert.ok(PASSWORD, 'EDHR_ROUTE_FIX_ADMIN_PASSWORD is required')
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome not found: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })

  const browser = await chromium.launch({ headless: true, executablePath: BROWSER_EXECUTABLE })
  const context = await browser.newContext({ viewport: { width: 1680, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const mesWriteRequests = []
  const previewResponses = []
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
    ) {
      mesWriteRequests.push({ method: request.method(), url: request.url() })
    }
  })
  page.on('response', async (response) => {
    if (response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/preview')) {
      previewResponses.push({
        status: response.status(),
        url: response.url(),
        body: await response.json().catch(() => null)
      })
    }
  })

  try {
    await login(page)
    const targetPath =
      `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_EXECUTION_ID}` +
      `&batchTaskId=${TASK_ID}`
    await page.goto(`${BASE_URL}${targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.locator('.edhr-readonly-form').waitFor({ state: 'visible', timeout: 90000 })
    await page.locator('.edhr-template-sheet').waitFor({ state: 'visible', timeout: 90000 })
    await page.waitForTimeout(1000)

    assert.equal(previewResponses.length, 1, JSON.stringify(previewResponses))
    assert.equal(previewResponses[0].status, 200)
    assert.ok([0, 200].includes(previewResponses[0].body?.code), JSON.stringify(previewResponses[0].body))
    assert.equal(Number(previewResponses[0].body?.data?.taskId), TASK_ID)
    assert.ok(previewResponses[0].body?.data?.formViewModel)
    assert.equal(await page.getByText(/请求地址不存在/).count(), 0)
    assert.deepEqual(mesWriteRequests, [])

    const screenshotPath = path.join(OUTPUT_DIR, 'unstarted-process-preview.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })
    const result = {
      baseUrl: BASE_URL,
      batchExecutionId: BATCH_EXECUTION_ID,
      taskId: TASK_ID,
      preview: previewResponses[0],
      readonlyFormVisible: await page.locator('.edhr-readonly-form').isVisible(),
      templateSheetVisible: await page.locator('.edhr-template-sheet').isVisible(),
      mesWriteRequests,
      screenshotPath
    }
    fs.writeFileSync(
      path.join(OUTPUT_DIR, 'unstarted-process-preview.json'),
      `${JSON.stringify(result, null, 2)}\n`,
      'utf8'
    )
    console.log(
      `PASS: preview batch=${BATCH_EXECUTION_ID} task=${TASK_ID} status=${previewResponses[0].status}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
