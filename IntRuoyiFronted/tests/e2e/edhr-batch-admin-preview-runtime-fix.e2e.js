const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_ADMIN_PREVIEW_BASE_URL || 'http://localhost:8081'
const TENANT = '芋道源码'
const USERNAME = 'admin'
const PASSWORD = 'admin123'
const BATCH_EXECUTION_ID = 900000000480
const TASK_ID = 3041
const TARGET_PATH =
  `/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_EXECUTION_ID}` +
  `&batchTaskId=${TASK_ID}`
const OUTPUT_DIR = path.resolve(
  process.cwd(),
  'doc/tasks/20260710-edhr-batch-admin-preview-runtime-fix/e2e-output'
)
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function ensurePrerequisites() {
  assert.match(BASE_URL, /^http:\/\/(127\.0\.0\.1|localhost):8081$/, 'E2E 只能验证本机 8081 前端')
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `系统 Chrome 不存在: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

async function login(page) {
  await page.goto(
    `${BASE_URL}/login?redirect=${encodeURIComponent('/mes/pro/feedback/edhr-batch-execution')}`,
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )

  const form = page.locator('form.login-form:visible')
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  await tenantInput.click()
  await tenantInput.fill(TENANT)
  const tenantOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', {
    hasText: TENANT
  })
  if ((await tenantOption.count()) > 0) {
    await tenantOption.first().click()
  } else {
    await tenantInput.press('Enter')
  }

  const usernameInput = form
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
  const passwordInput = form.locator('input[type="password"]:visible').first()
  await usernameInput.fill(USERNAME)
  await passwordInput.fill(PASSWORD)

  await form.getByRole('button', { name: /登录/ }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function run() {
  ensurePrerequisites()

  const browser = await chromium.launch({
    headless: process.env.EDHR_ADMIN_PREVIEW_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({
    viewport: { width: 1680, height: 900 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const mesWriteRequests = []
  const consoleErrors = []
  const pageErrors = []

  page.on('request', (request) => {
    const method = request.method()
    if (
      request.url().includes('/admin-api/mes/') &&
      !['GET', 'HEAD', 'OPTIONS'].includes(method)
    ) {
      mesWriteRequests.push({ method, url: request.url() })
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))

  try {
    await login(page)

    const previewResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/preview') &&
        response.url().includes(`batchExecutionId=${BATCH_EXECUTION_ID}`) &&
        response.url().includes(`taskId=${TASK_ID}`),
      { timeout: 90000 }
    )

    await page.goto(`${BASE_URL}${TARGET_PATH}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })

    const previewResponse = await previewResponsePromise
    const previewBody = await previewResponse.json()
    assert.equal(previewResponse.status(), 200, '只读预览接口 HTTP 状态必须为 200')
    assert.equal(previewBody.code, 0, `只读预览接口必须成功: ${JSON.stringify(previewBody)}`)
    assert.equal(Number(previewBody.data?.taskId), TASK_ID, '只读预览必须返回目标任务')
    assert.ok(previewBody.data?.formViewModel, '只读预览必须返回表单视图模型')

    const readonlyForm = page.locator('.edhr-readonly-form')
    const templateSheet = page.locator('.edhr-template-sheet')
    await readonlyForm.waitFor({ state: 'visible', timeout: 90000 })
    await templateSheet.waitFor({ state: 'visible', timeout: 90000 })

    await assert.doesNotReject(async () => {
      await page.getByText('精洗工序生产记录', { exact: false }).first().waitFor({
        state: 'visible',
        timeout: 30000
      })
    }, '页面必须显示精洗工序表单名称')
    assert.equal(
      await page.getByText(
        '当前表单尚未形成已填写内容，请在右侧工序表单中打开填写',
        { exact: true }
      ).count(),
      0,
      '不得继续显示旧的误导空状态'
    )
    assert.equal(mesWriteRequests.length, 0, `管理员只读验证不得产生 MES 写请求: ${JSON.stringify(mesWriteRequests)}`)

    const activeFormAction = page.locator(
      '.edhr-batch-detail__rail-process-form-item.is-active .edhr-batch-detail__rail-process-form-action'
    )
    if ((await activeFormAction.count()) > 0) {
      assert.equal(
        await activeFormAction.first().isEnabled(),
        false,
        '管理员未被分配时表单写操作必须禁用'
      )
    }

    const screenshotPath = path.join(OUTPUT_DIR, 'admin-unstarted-form-preview.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })

    const result = {
      batchExecutionId: BATCH_EXECUTION_ID,
      taskId: TASK_ID,
      previewHttpStatus: previewResponse.status(),
      previewCode: previewBody.code,
      executionCreated: previewBody.data?.executionCreated,
      executionId: previewBody.data?.executionId,
      readonlyFormVisible: await readonlyForm.isVisible(),
      templateSheetVisible: await templateSheet.isVisible(),
      mesWriteRequests,
      consoleErrors,
      pageErrors,
      screenshotPath
    }
    fs.writeFileSync(
      path.join(OUTPUT_DIR, 'admin-unstarted-form-preview.json'),
      `${JSON.stringify(result, null, 2)}\n`,
      'utf8'
    )

    console.log(
      `PASS: admin readonly preview batch=${BATCH_EXECUTION_ID} task=${TASK_ID} executionCreated=${String(result.executionCreated)}`
    )
    console.log(`PASS: evidence=${OUTPUT_DIR}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
