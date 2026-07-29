const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { createRequire } = require('node:module')

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..')
const FRONTEND_ROOT = path.join(REPO_ROOT, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(FRONTEND_ROOT, 'package.json'))
const { chromium } = frontendRequire('playwright')

const BASE_URL = process.env.EDHR_ADMIN_SUBMITTED_BASE_URL || 'http://127.0.0.1:8081'
const BACKEND_URL = process.env.EDHR_ADMIN_SUBMITTED_BACKEND_URL || 'http://127.0.0.1:48081'
const MYSQL_CONTAINER_NAME = process.env.EDHR_ADMIN_SUBMITTED_MYSQL_CONTAINER || 'int-ruoyi-mysql'
const DATABASE_NAME = process.env.EDHR_ADMIN_SUBMITTED_DATABASE || 'ruoyi-vue-pro'
const VERIFY_MODE = process.env.EDHR_ADMIN_SUBMITTED_VERIFY_MODE || 'submitted-content'
const TARGET_WORK_ORDER_CODE = process.env.EDHR_ADMIN_SUBMITTED_WORK_ORDER_CODE || '881MO090935'
const OUTPUT_DIR = path.join(__dirname, 'admin-submitted-content-e2e-output')
const DEFAULT_CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const SUBMITTED_STATUSES = new Set([2, 3, 4])

function parseEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {}
  return fs
    .readFileSync(filePath, 'utf8')
    .split(/\r?\n/)
    .reduce((result, line) => {
      const trimmed = line.trim()
      if (!trimmed || trimmed.startsWith('#')) return result
      const match = trimmed.match(/^([^=\s]+)\s*=\s*(.*)$/)
      if (!match) return result
      result[match[1]] = match[2].trim().replace(/^['"]|['"]$/g, '')
      return result
    }, {})
}

function resolveLoginConfig() {
  const env = parseEnvFile(path.join(FRONTEND_ROOT, '.env'))
  return {
    tenant: process.env.EDHR_ADMIN_SUBMITTED_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: process.env.EDHR_ADMIN_SUBMITTED_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: process.env.EDHR_ADMIN_SUBMITTED_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
}

function ensurePrerequisites() {
  assert.match(BASE_URL, /^http:\/\/(127\.0\.0\.1|localhost):8081$/, 'E2E 只能验证本机 8081 前端')
  assert.match(BACKEND_URL, /^http:\/\/127\.0\.0\.1:48081$/, 'E2E 只能验证本机 48081 后端')
  assert.ok(
    ['submitted-content', 'current-unsubmitted'].includes(VERIFY_MODE),
    `未知验证模式: ${VERIFY_MODE}`
  )
  const login = resolveLoginConfig()
  assert.ok(login.tenant, '缺少登录租户')
  assert.ok(login.username, '缺少登录账号')
  assert.ok(login.password, '缺少管理员登录密码来源')
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    assert.ok(
      fs.existsSync(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH),
      `指定 Chrome 不存在: ${process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH}`
    )
  } else {
    assert.ok(fs.existsSync(DEFAULT_CHROME), `系统 Chrome 不存在: ${DEFAULT_CHROME}`)
  }
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  return login
}

function sqlString(value) {
  return `'${String(value ?? '').replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
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
      maxBuffer: 32 * 1024 * 1024
    }
  )
  if (result.error) throw new Error(`${label} 数据库读取失败：${result.error.message}`)
  if (result.status !== 0) {
    throw new Error(`${label} 数据库读取失败：${(result.stderr || '').trim() || result.status}`)
  }
  return parseMysqlRows(result.stdout || '')
}

function decodeHexUtf8(value) {
  if (!value) return ''
  return Buffer.from(value, 'hex').toString('utf8')
}

function resolveTenantUser(login) {
  const rows = queryLocalDatabase(
    `SELECT t.id, t.name, t.status, u.id, u.username, COALESCE(u.nickname, ''), u.status
       FROM system_tenant t
       JOIN system_users u
         ON u.tenant_id = t.id
        AND u.deleted = b'0'
        AND u.username = ${sqlString(login.username)}
      WHERE t.deleted = b'0'
        AND t.name = ${sqlString(login.tenant)}
      LIMIT 1;`,
    '登录租户账号'
  )
  assert.ok(rows.length > 0, `本地库未找到租户/账号：${login.tenant}/${login.username}`)
  const row = rows[0]
  assert.equal(row[2], '0', '登录租户必须启用')
  assert.equal(row[6], '0', '登录账号必须启用')
  return {
    tenantId: Number(row[0]),
    tenantName: row[1],
    userId: Number(row[3]),
    username: row[4],
    nickname: row[5]
  }
}

function firstSubmittedDisplayText(cellValuesJson) {
  const parsed = JSON.parse(cellValuesJson)
  assert.ok(Array.isArray(parsed), 'cell_values_json 必须是数组')
  const textCell = parsed.find((item) => {
    const display = typeof item?.valueDisplay === 'string' ? item.valueDisplay.trim() : ''
    if (!display || display === 'true' || display === 'false') return false
    return display.length >= 8
  })
  if (textCell) return textCell.valueDisplay.trim()
  const fallback = parsed.find((item) => {
    const value = typeof item?.value === 'string' ? item.value.trim() : ''
    return value && value.length >= 8
  })
  return fallback?.value?.trim() || ''
}

function resolveSubmittedTarget(tenantUser) {
  const rows = queryLocalDatabase(
    `SELECT be.id,
            be.batch_execution_code,
            be.batch_code,
            be.work_order_code,
            be.status,
            t.id,
            t.route_process_sort,
            t.batch_record_sort,
            COALESCE(t.process_name, ''),
            COALESCE(t.batch_record_report_name, ''),
            e.id,
            e.status,
            e.submitted_by,
            COALESCE(e.submitted_at, ''),
            HEX(e.cell_values_json)
       FROM mes_pro_edhr_batch_execution be
       JOIN mes_pro_edhr_batch_execution_task t
         ON t.batch_execution_id = be.id
        AND t.deleted = b'0'
       JOIN mes_pro_batch_record_execution e
         ON e.id = t.execution_id
        AND e.deleted = b'0'
      WHERE be.deleted = b'0'
        AND be.tenant_id = ${tenantUser.tenantId}
        AND t.node_type = 'ROUTE_FORM'
        AND t.batch_record_report_id IS NOT NULL
        AND e.status IN (2, 3, 4)
        AND CHAR_LENGTH(COALESCE(e.cell_values_json, '')) > 20
      ORDER BY be.update_time DESC, t.route_process_sort, t.batch_record_sort
      LIMIT 50;`,
    '已提交批记录执行样本'
  )
  for (const row of rows) {
    const cellValuesJson = decodeHexUtf8(row[14])
    let expectedText = ''
    try {
      expectedText = firstSubmittedDisplayText(cellValuesJson)
    } catch {
      expectedText = ''
    }
    if (!expectedText) continue
    return {
      batchExecutionId: Number(row[0]),
      batchExecutionCode: row[1],
      batchCode: row[2],
      workOrderCode: row[3],
      batchStatus: Number(row[4]),
      taskId: Number(row[5]),
      routeProcessSort: Number(row[6]),
      batchRecordSort: Number(row[7]),
      processName: row[8],
      reportName: row[9],
      executionId: Number(row[10]),
      executionStatus: Number(row[11]),
      submittedBy: row[12],
      submittedAt: row[13],
      expectedText,
      cellValuesJson
    }
  }
  throw new Error('本机数据库未找到可用于页面断言的已提交非空批记录执行样本')
}

function resolveCurrentUnsubmittedTarget(tenantUser) {
  const rows = queryLocalDatabase(
    `SELECT be.id,
            be.batch_execution_code,
            be.batch_code,
            be.work_order_code,
            be.status,
            t.id,
            t.route_process_sort,
            t.batch_record_sort,
            COALESCE(t.process_name, ''),
            COALESCE(t.batch_record_report_name, ''),
            COALESCE(t.execution_id, ''),
            COALESCE(e.status, ''),
            CHAR_LENGTH(COALESCE(e.cell_values_json, ''))
       FROM mes_pro_edhr_batch_execution be
       JOIN mes_pro_edhr_batch_execution_task t
         ON t.batch_execution_id = be.id
        AND t.deleted = b'0'
       LEFT JOIN mes_pro_batch_record_execution e
         ON e.id = t.execution_id
        AND e.deleted = b'0'
      WHERE be.deleted = b'0'
        AND be.tenant_id = ${tenantUser.tenantId}
        AND be.work_order_code = ${sqlString(TARGET_WORK_ORDER_CODE)}
        AND t.node_type = 'ROUTE_FORM'
        AND t.batch_record_report_id IS NOT NULL
      ORDER BY be.update_time DESC, t.route_process_sort, t.batch_record_sort
      LIMIT 1;`,
    '当前未提交目标'
  )
  assert.ok(rows.length > 0, `本机数据库未找到工单 ${TARGET_WORK_ORDER_CODE} 的批记录任务`)
  const row = rows[0]
  return {
    batchExecutionId: Number(row[0]),
    batchExecutionCode: row[1],
    batchCode: row[2],
    workOrderCode: row[3],
    batchStatus: Number(row[4]),
    taskId: Number(row[5]),
    routeProcessSort: Number(row[6]),
    batchRecordSort: Number(row[7]),
    processName: row[8],
    reportName: row[9],
    executionId: row[10] ? Number(row[10]) : null,
    executionStatus: row[11] === '' ? null : Number(row[11]),
    cellValuesLength: Number(row[12])
  }
}

function resolveBatchSnapshot(tenantUser, workOrderCode) {
  const rows = queryLocalDatabase(
    `SELECT be.id,
            be.batch_execution_code,
            be.batch_code,
            be.work_order_code,
            be.status,
            t.id,
            COALESCE(t.process_name, ''),
            COALESCE(t.batch_record_report_name, ''),
            t.status,
            COALESCE(t.execution_id, ''),
            COALESCE(e.status, ''),
            CHAR_LENGTH(COALESCE(e.cell_values_json, ''))
       FROM mes_pro_edhr_batch_execution be
       JOIN mes_pro_edhr_batch_execution_task t
         ON t.batch_execution_id = be.id
        AND t.deleted = b'0'
       LEFT JOIN mes_pro_batch_record_execution e
         ON e.id = t.execution_id
        AND e.deleted = b'0'
      WHERE be.deleted = b'0'
        AND be.tenant_id = ${tenantUser.tenantId}
        AND be.work_order_code = ${sqlString(workOrderCode)}
      ORDER BY be.update_time DESC, t.route_process_sort, t.batch_record_sort
      LIMIT 30;`,
    '目标工单当前批次状态'
  )
  return rows.map((row) => ({
    batchExecutionId: Number(row[0]),
    batchExecutionCode: row[1],
    batchCode: row[2],
    workOrderCode: row[3],
    batchStatus: Number(row[4]),
    taskId: Number(row[5]),
    processName: row[6],
    reportName: row[7],
    taskStatus: Number(row[8]),
    executionId: row[9] ? Number(row[9]) : null,
    executionStatus: row[10] === '' ? null : Number(row[10]),
    cellValuesLength: Number(row[11])
  }))
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写登录控件：${label}`)
}

async function selectTenant(page, form, tenant) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  await tenantInput.click()
  await tenantInput.fill(tenant)
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: tenant })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function login(page, loginConfig) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form, loginConfig.tenant)
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    loginConfig.username,
    '账号'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), loginConfig.password, '密码')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `登录 HTTP 失败：${loginResponse.status()}`)
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

function assertReviewTimelineContainsSubmittedTarget(reviewBody, target) {
  assert.ok(reviewBody.code === 0 || reviewBody.code === 200, `review-timeline 返回失败：${reviewBody.msg || reviewBody.code}`)
  const executions = reviewBody.data?.executionReviews
  assert.ok(Array.isArray(executions), 'review-timeline 必须返回 executionReviews')
  const matched = executions.find((item) => Number(item.executionId) === target.executionId)
  assert.ok(matched, `review-timeline 缺少目标 executionId=${target.executionId}`)
  assert.ok(SUBMITTED_STATUSES.has(Number(matched.status)), `目标 execution 状态不是已提交态：${matched.status}`)
  assert.ok(matched.formViewModel, '目标 execution 必须携带 formViewModel')
  assert.ok(
    String(matched.formViewModel.cellValuesJson || '').includes(target.expectedText),
    'review-timeline 的已提交 formViewModel 必须包含目标单元格值'
  )
}

function assertReviewTimelineHasNoSubmittedContentForTask(reviewBody, target) {
  assert.ok(reviewBody.code === 0 || reviewBody.code === 200, `review-timeline 返回失败：${reviewBody.msg || reviewBody.code}`)
  const executions = reviewBody.data?.executionReviews || []
  assert.ok(Array.isArray(executions), 'review-timeline 必须返回 executionReviews')
  const matched = executions.find(
    (item) =>
      Number(item.taskId) === target.taskId ||
      (target.executionId != null && Number(item.executionId) === target.executionId)
  )
  if (matched) {
    assert.ok(
      !SUBMITTED_STATUSES.has(Number(matched.status)),
      `当前截图任务已有已提交 execution，不能作为未提交验证样本：${matched.status}`
    )
  }
}

async function run() {
  const loginConfig = ensurePrerequisites()
  const tenantUser = resolveTenantUser(loginConfig)
  const target = VERIFY_MODE === 'current-unsubmitted'
    ? resolveCurrentUnsubmittedTarget(tenantUser)
    : resolveSubmittedTarget(tenantUser)
  const batchSnapshot = resolveBatchSnapshot(tenantUser, target.workOrderCode || TARGET_WORK_ORDER_CODE)
  const executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || DEFAULT_CHROME
  const browser = await chromium.launch({
    headless: process.env.EDHR_ADMIN_SUBMITTED_HEADED !== '1',
    executablePath
  })
  const context = await browser.newContext({ viewport: { width: 1680, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const mesWriteRequests = []
  const taskPreviewRequests = []
  const consoleErrors = []
  const pageErrors = []

  page.on('request', (request) => {
    const method = request.method()
    const url = request.url()
    if (url.includes('/admin-api/mes/pro/edhr-batch-execution/task/preview')) {
      taskPreviewRequests.push({ method, url })
    }
    if (url.includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      mesWriteRequests.push({ method, url })
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))

  try {
    await login(page, loginConfig)
    const detailPath =
      `/mes/pro/feedback/edhr-batch-execution/detail?id=${target.batchExecutionId}` +
      `&batchTaskId=${target.taskId}`
    const reviewTimelineResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes('/admin-api/mes/pro/edhr-batch-execution/review-timeline') &&
        response.url().includes(`id=${target.batchExecutionId}`),
      { timeout: 90000 }
    )
    await page.goto(`${BASE_URL}${detailPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    const reviewTimelineResponse = await reviewTimelineResponsePromise
    assert.equal(reviewTimelineResponse.status(), 200, 'review-timeline HTTP 状态必须为 200')
    const reviewTimelineBody = await reviewTimelineResponse.json()
    if (VERIFY_MODE === 'current-unsubmitted') {
      assertReviewTimelineHasNoSubmittedContentForTask(reviewTimelineBody, target)
    } else {
      assertReviewTimelineContainsSubmittedTarget(reviewTimelineBody, target)
    }

    await page.locator('.edhr-batch-detail__review-list').waitFor({ state: 'visible', timeout: 90000 })
    const processButton = page
      .locator('.edhr-batch-detail__process-task-group-head')
      .filter({ hasText: target.processName })
      .first()
    await processButton.waitFor({ state: 'visible', timeout: 60000 })
    await processButton.click()

    const readonlyForm = page.locator('.edhr-readonly-form').first()
    const templateSheet = page.locator('.edhr-readonly-form .edhr-template-sheet').first()
    let sheetText = ''
    if (VERIFY_MODE === 'current-unsubmitted') {
      await page.getByText('暂无已提交批记录内容', { exact: true }).first().waitFor({
        state: 'visible',
        timeout: 60000
      })
      assert.equal(await readonlyForm.count(), 0, '未提交任务不得渲染只读原表内容')
    } else {
      await readonlyForm.waitFor({ state: 'visible', timeout: 90000 })
      await templateSheet.waitFor({ state: 'visible', timeout: 90000 })
      await page.getByText(target.expectedText, { exact: false }).first().waitFor({
        state: 'visible',
        timeout: 60000
      })
      sheetText = await templateSheet.innerText()
      assert.ok(sheetText.includes(target.expectedText), '主区域原表必须显示已提交单元格内容')
      assert.equal(
        await page.getByText('暂无已提交批记录内容', { exact: true }).count(),
        0,
        '有已提交 execution 时不得显示暂无已提交内容'
      )
    }
    assert.equal(taskPreviewRequests.length, 0, '管理员主区域不得请求 task/preview 读取未提交预览或草稿')
    assert.equal(mesWriteRequests.length, 0, `管理员只读查看不得触发 MES 写请求: ${JSON.stringify(mesWriteRequests)}`)

    const screenshotPath = path.join(
      OUTPUT_DIR,
      VERIFY_MODE === 'current-unsubmitted'
        ? 'admin-current-unsubmitted-main-area.png'
        : 'admin-submitted-content-main-area.png'
    )
    await page.screenshot({ path: screenshotPath, fullPage: true })
    const result = {
      verifyMode: VERIFY_MODE,
      baseUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      tenant: loginConfig.tenant,
      username: loginConfig.username,
      tenantUser,
      target,
      batchSnapshot,
      reviewTimelineHttpStatus: reviewTimelineResponse.status(),
      readonlyFormVisible: await readonlyForm.isVisible(),
      templateSheetVisible: await templateSheet.isVisible(),
      expectedTextVisible: VERIFY_MODE === 'submitted-content' ? sheetText.includes(target.expectedText) : false,
      emptySubmittedContentVisible:
        VERIFY_MODE === 'current-unsubmitted'
          ? (await page.getByText('暂无已提交批记录内容', { exact: true }).count()) > 0
          : false,
      taskPreviewRequests,
      mesWriteRequests,
      consoleErrors,
      pageErrors,
      screenshotPath
    }
    const resultPath = path.join(
      OUTPUT_DIR,
      VERIFY_MODE === 'current-unsubmitted'
        ? 'admin-current-unsubmitted-main-area.json'
        : 'admin-submitted-content-main-area.json'
    )
    fs.writeFileSync(resultPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    if (VERIFY_MODE === 'current-unsubmitted') {
      console.log(`PASS: admin current unsubmitted empty state batch=${target.batchExecutionId} task=${target.taskId} execution=${target.executionId}`)
    } else {
      console.log(`PASS: admin submitted content visible batch=${target.batchExecutionId} task=${target.taskId} execution=${target.executionId}`)
      console.log(`PASS: expectedText=${target.expectedText}`)
    }
    console.log(`PASS: evidence=${resultPath}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
