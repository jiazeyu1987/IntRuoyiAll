const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_ADMIN_PREVIEW_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_ADMIN_PREVIEW_TENANT || '芋道源码'
const USERNAME = process.env.EDHR_ADMIN_PREVIEW_USERNAME || 'admin'
const PASSWORD = process.env.EDHR_ADMIN_PREVIEW_PASSWORD
const EXPLICIT_BATCH_EXECUTION_ID = Number(process.env.EDHR_ADMIN_PREVIEW_BATCH_ID || 0)
const EXPLICIT_TASK_ID = Number(process.env.EDHR_ADMIN_PREVIEW_TASK_ID || 0)
const TASK_DOC_ID =
  process.env.EDHR_ADMIN_PREVIEW_TASK_DOC_ID || '20260725-full-e2e-admin-validation'
const OUTPUT_DIR = path.resolve(
  process.env.EDHR_ADMIN_PREVIEW_OUTPUT_DIR ||
    path.join(process.cwd(), '..', 'doc', 'tasks', TASK_DOC_ID, 'admin-preview-e2e-output')
)
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const MYSQL_CONTAINER_NAME = process.env.EDHR_ADMIN_PREVIEW_MYSQL_CONTAINER || 'int-ruoyi-mysql'
const DATABASE_NAME = process.env.EDHR_ADMIN_PREVIEW_DATABASE || 'ruoyi-vue-pro'
const AUTHORIZED_TENANT_ID = 1

function ensurePrerequisites() {
  assert.match(BASE_URL, /^http:\/\/(127\.0\.0\.1|localhost):8081$/, 'E2E 只能验证本机 8081 前端')
  assert.equal(TENANT, '芋道源码', '管理员只读预览必须使用芋道源码租户')
  assert.equal(USERNAME, 'admin', '管理员只读预览必须使用 admin')
  assert.ok(PASSWORD, '缺少 EDHR_ADMIN_PREVIEW_PASSWORD')
  assert.equal(
    EXPLICIT_BATCH_EXECUTION_ID > 0,
    EXPLICIT_TASK_ID > 0,
    '显式指定预览目标时必须同时提供 EDHR_ADMIN_PREVIEW_BATCH_ID 和 EDHR_ADMIN_PREVIEW_TASK_ID'
  )
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `系统 Chrome 不存在: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

function parseMysqlRows(stdout) {
  return stdout
    .trim()
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split('\t').map((value) => (value === 'NULL' ? '' : value)))
}

function queryLocalDatabase(sql, label) {
  const result = spawnSync(
    'docker',
    [
      'exec',
      '-i',
      MYSQL_CONTAINER_NAME,
      'sh',
      '-lc',
      `MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -D${DATABASE_NAME} -N -B`
    ],
    {
      input: sql,
      encoding: 'utf8',
      maxBuffer: 10 * 1024 * 1024
    }
  )
  if (result.error) {
    throw new Error(`${label} 数据库读取失败：${result.error.message}`)
  }
  if (result.status !== 0) {
    const stderr = (result.stderr || '').trim()
    throw new Error(`${label} 数据库读取失败：${stderr || `docker/mysql 退出码 ${result.status}`}`)
  }
  return parseMysqlRows(result.stdout || '')
}

function resolvePreviewTarget() {
  if (EXPLICIT_BATCH_EXECUTION_ID > 0 && EXPLICIT_TASK_ID > 0) {
    return {
      batchExecutionId: EXPLICIT_BATCH_EXECUTION_ID,
      taskId: EXPLICIT_TASK_ID,
      source: 'env'
    }
  }

  const rows = queryLocalDatabase(
    `SELECT be.id,
            be.batch_execution_code,
            be.status,
            t.id,
            COALESCE(t.process_name, ''),
            COALESCE(t.batch_record_report_name, ''),
            t.batch_record_report_id,
            t.status
       FROM mes_pro_edhr_batch_execution be
       JOIN mes_pro_edhr_batch_execution_task t
         ON t.batch_execution_id = be.id
        AND t.deleted = b'0'
      WHERE be.deleted = b'0'
        AND be.tenant_id = ${AUTHORIZED_TENANT_ID}
        AND be.status IN (0, 10, 20, 25)
        AND t.node_type = 'ROUTE_FORM'
        AND t.execution_id IS NULL
        AND t.batch_record_report_id IS NOT NULL
      ORDER BY be.update_time DESC, t.route_process_sort, t.batch_record_sort
      LIMIT 1;`,
    '管理员只读预览目标'
  )
  assert.ok(rows.length > 0, '本机数据库未找到授权租户下可只读预览的未开始批记录任务')
  const [
    batchExecutionId,
    batchExecutionCode,
    batchStatus,
    taskId,
    processName,
    taskDisplayName,
    batchRecordReportId,
    taskStatus
  ] = rows[0]
  return {
    batchExecutionId: Number(batchExecutionId),
    batchExecutionCode,
    batchStatus: Number(batchStatus),
    taskId: Number(taskId),
    taskStatus: Number(taskStatus),
    processName,
    taskDisplayName,
    batchRecordReportId,
    source: `${MYSQL_CONTAINER_NAME}/${DATABASE_NAME}`
  }
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

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /登录/ }).click()
  const loginBody = await (await loginResponsePromise).json()
  assert(
    loginBody.code === 0 || loginBody.code === 200,
    `登录失败：${loginBody.msg || loginBody.code}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function run() {
  ensurePrerequisites()
  const previewTarget = resolvePreviewTarget()
  const targetPath =
    `/mes/pro/feedback/edhr-batch-execution/detail?id=${previewTarget.batchExecutionId}` +
    `&batchTaskId=${previewTarget.taskId}`

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
        response.url().includes(`batchExecutionId=${previewTarget.batchExecutionId}`) &&
        response.url().includes(`taskId=${previewTarget.taskId}`),
      { timeout: 90000 }
    )

    await page.goto(`${BASE_URL}${targetPath}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })

    const previewResponse = await previewResponsePromise
    const previewBody = await previewResponse.json()
    assert.equal(previewResponse.status(), 200, '只读预览接口 HTTP 状态必须为 200')
    assert.ok(
      previewBody.code === 0 || previewBody.code === 200,
      `只读预览接口必须成功: ${JSON.stringify(previewBody)}`
    )
    assert.equal(Number(previewBody.data?.taskId), previewTarget.taskId, '只读预览必须返回目标任务')
    if (previewTarget.source !== 'env') {
      assert.equal(previewBody.data?.executionCreated, false, '数据库发现的预览目标必须保持未创建执行记录')
    }
    assert.ok(previewBody.data?.formViewModel, '只读预览必须返回表单视图模型')

    const readonlyForm = page.locator('.edhr-readonly-form')
    const templateSheet = page.locator('.edhr-template-sheet')
    await readonlyForm.waitFor({ state: 'visible', timeout: 90000 })
    await templateSheet.waitFor({ state: 'visible', timeout: 90000 })

    const expectedProcessText = previewTarget.processName || previewTarget.taskDisplayName
    if (expectedProcessText) {
      await page.getByText(expectedProcessText, { exact: false }).first().waitFor({
        state: 'visible',
        timeout: 30000
      })
    }
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
    const activeFormActionCount = await activeFormAction.count()
    const activeFormActionEnabled =
      activeFormActionCount > 0 ? await activeFormAction.first().isEnabled() : false

    const screenshotPath = path.join(OUTPUT_DIR, 'admin-unstarted-form-preview.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })

    const result = {
      tenant: TENANT,
      username: USERNAME,
      targetSource: previewTarget.source,
      batchExecutionId: previewTarget.batchExecutionId,
      batchExecutionCode: previewTarget.batchExecutionCode,
      batchStatus: previewTarget.batchStatus,
      taskId: previewTarget.taskId,
      taskStatus: previewTarget.taskStatus,
      processName: previewTarget.processName,
      taskDisplayName: previewTarget.taskDisplayName,
      previewHttpStatus: previewResponse.status(),
      previewCode: previewBody.code,
      executionCreated: previewBody.data?.executionCreated,
      executionId: previewBody.data?.executionId,
      readonlyFormVisible: await readonlyForm.isVisible(),
      templateSheetVisible: await templateSheet.isVisible(),
      activeFormActionCount,
      activeFormActionEnabled,
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
      `PASS: admin readonly preview batch=${previewTarget.batchExecutionId} task=${previewTarget.taskId} executionCreated=${String(result.executionCreated)}`
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
