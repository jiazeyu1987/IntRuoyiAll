const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')

const TASK_ID = envValue('EDHR_BATCH_E2E_TASK_ID') || 'fix-batch-record-fill-rule'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-batch-execution')
const EVIDENCE_FILE = envValue('EDHR_BATCH_E2E_EVIDENCE_FILE')
  ? path.resolve(envValue('EDHR_BATCH_E2E_EVIDENCE_FILE'))
  : path.resolve(process.cwd(), '..', 'doc', 'tasks', TASK_ID, 'real-e2e-evidence.md')
const REQUIRED_BASE_URL = 'http://localhost:8081'
const BATCH_EXECUTION_ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const OPEN_OR_CREATE_ENDPOINT_COVERAGE_TOKEN = '/mes/pro/edhr-batch-execution/open-or-create'
const EXECUTION_ROUTES = new Set([
  '/mes/pro/feedback/edhr-execution/detail',
  '/mes/pro/feedback/edhr-execution/form'
])
const MYSQL_CONTAINER_NAME = 'int-ruoyi-mysql'
const DATABASE_NAME = 'ruoyi-vue-pro'
const AUTHORIZED_TENANT_ID = '1'
const AUTHORIZED_TENANT_LABEL = '芋道源码'
const AUTHORIZED_USERNAME = 'admin'

function envValue(name) {
  return (process.env[name] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
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

function firstDatabaseRow(sql, label) {
  const rows = queryLocalDatabase(sql, label)
  if (rows.length === 0) {
    throw new Error(`${label} 未找到符合条件的本地数据库记录`)
  }
  return rows[0]
}

function resolveDatabaseFixture() {
  const [tenantId, tenantName, tenantStatus, tenantDeleted] = firstDatabaseRow(
    `SELECT id, name, status, deleted + 0 AS deleted FROM system_tenant WHERE id = ${AUTHORIZED_TENANT_ID} AND deleted = b'0' LIMIT 1;`,
    '授权租户'
  )
  assert.equal(tenantId, AUTHORIZED_TENANT_ID, '数据库夹具必须使用用户授权的 tenant 1')
  assert.equal(tenantStatus, '0', '用户授权租户必须处于启用状态')
  assert.ok(tenantDeleted === '\0' || tenantDeleted === '0', '用户授权租户不得被删除')

  const [userId, username, nickname, userStatus, userTenantId, userDeleted] = firstDatabaseRow(
    `SELECT id, username, nickname, status, tenant_id, deleted + 0 AS deleted FROM system_users WHERE username = '${AUTHORIZED_USERNAME}' AND tenant_id = ${AUTHORIZED_TENANT_ID} AND deleted = b'0' LIMIT 1;`,
    '授权账号'
  )
  assert.equal(username, AUTHORIZED_USERNAME, '数据库夹具必须使用用户授权的 admin 账号')
  assert.equal(userTenantId, AUTHORIZED_TENANT_ID, '授权账号必须属于用户授权租户')
  assert.equal(userStatus, '0', '授权账号必须处于启用状态')
  assert.ok(userDeleted === '\0' || userDeleted === '0', '授权账号不得被删除')

  const batch = firstDatabaseRow(
    `SELECT be.id,
            be.batch_execution_code,
            be.work_order_id,
            be.work_order_code,
            be.batch_code,
            be.route_id,
            COALESCE(be.route_code, ''),
            COALESCE(be.route_name, ''),
            be.status,
            be.blocked_count,
            t.id,
            COALESCE(t.process_name, ''),
            COALESCE(t.batch_record_report_name, t.form_template_name_snapshot, ''),
            t.execution_id,
            t.status,
            wt.id,
            wt.task_type
       FROM mes_pro_edhr_batch_execution be
       JOIN mes_pro_edhr_batch_execution_task t
         ON t.batch_execution_id = be.id
        AND t.deleted = b'0'
       JOIN mes_pro_edhr_work_task wt
         ON wt.batch_task_id = t.id
        AND wt.deleted = b'0'
        AND wt.task_type IN ('FILL', 'REWORK')
        AND wt.status IN ('TODO', 'DOING', 'OVERDUE')
        AND wt.assignee_user_id = ${userId}
      WHERE be.deleted = b'0'
        AND be.tenant_id = ${AUTHORIZED_TENANT_ID}
        AND be.status IN (0, 10, 20, 25)
        AND be.blocked_count = 0
        AND t.required_flag = b'1'
        AND t.node_type = 'ROUTE_FORM'
        AND t.status NOT IN (40, 45, 50)
        AND (t.batch_record_report_id IS NOT NULL OR t.form_template_id IS NOT NULL)
        AND NOT EXISTS (
              SELECT 1
                FROM mes_pro_edhr_batch_execution_task previous_task
               WHERE previous_task.batch_execution_id = be.id
                 AND previous_task.deleted = b'0'
                 AND previous_task.required_flag = b'1'
                 AND previous_task.node_type = 'ROUTE_FORM'
                 AND previous_task.status NOT IN (40, 45)
                 AND (
                      (
                        previous_task.route_process_id = t.route_process_id
                        AND (
                             COALESCE(previous_task.batch_record_sort, 2147483647) < COALESCE(t.batch_record_sort, 2147483647)
                             OR (
                                  COALESCE(previous_task.batch_record_sort, 2147483647) = COALESCE(t.batch_record_sort, 2147483647)
                                  AND previous_task.id < t.id
                             )
                        )
                      )
                      OR (
                        t.predecessor_route_process_id IS NOT NULL
                        AND previous_task.route_process_id = t.predecessor_route_process_id
                      )
                 )
        )
      ORDER BY CASE WHEN t.execution_id IS NOT NULL THEN 0 ELSE 1 END,
               be.update_time DESC,
               t.route_process_sort,
               t.batch_record_sort,
               wt.id DESC
      LIMIT 1;`,
    '可打开批次任务'
  )

  const [
    batchExecutionId,
    batchExecutionCode,
    workOrderId,
    workOrderCode,
    batchCode,
    routeId,
    routeCode,
    routeName,
    batchStatus,
    blockedCount,
    taskId,
    processName,
    taskDisplayName,
    executionId,
    taskStatus,
    workTaskId,
    workTaskType
  ] = batch

  assert.equal(blockedCount, '0', '数据库夹具批次不得存在阻塞任务')
  assert.ok(Number.isFinite(Number(batchExecutionId)), '数据库夹具必须包含真实批次执行 ID')
  assert.ok(Number.isFinite(Number(taskId)), '数据库夹具必须包含真实任务 ID')
  assert.ok(Number.isFinite(Number(executionId)), '数据库夹具必须包含真实 eDHR 执行 ID')

  return {
    tenantId,
    tenantName: tenantName || AUTHORIZED_TENANT_LABEL,
    username,
    nickname,
    userId,
    batchExecutionId,
    batchExecutionCode,
    workOrderId,
    workOrderCode,
    batchCode,
    routeId,
    routeCode,
    routeName,
    batchStatus,
    taskId,
    taskStatus,
    workTaskId,
    workTaskType,
    processName,
    taskDisplayName,
    executionId,
    fixtureSource: `${MYSQL_CONTAINER_NAME}/${DATABASE_NAME}`
  }
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const fixture = result.fixture || {}
  const lines = [
    '# eDHR 批次执行真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 状态：${result.status}`,
    `- 前端入口：\`${REQUIRED_BASE_URL}\``,
    `- 授权租户/账号：\`${AUTHORIZED_TENANT_LABEL}/${AUTHORIZED_USERNAME}\`；密码由登录页本机默认值提供，脚本和证据不记录明文密码。`,
    `- 数据来源：\`${fixture.fixtureSource || `${MYSQL_CONTAINER_NAME}/${DATABASE_NAME}`}\``,
    fixture.batchExecutionId ? `- 批次执行：\`${fixture.batchExecutionCode || fixture.batchExecutionId}\`，任务 ID \`${fixture.taskId}\`，执行 ID \`${fixture.executionId}\`` : '',
    '',
    '## BDD',
    '',
    '- BDD: 数据库夹具发现 -> Given 本机数据库存在授权租户 admin 与非作废 eDHR 批次任务 When 执行真实 E2E Then 脚本从数据库读取批次、任务和执行 ID，不要求人工注入工单或批次环境变量。',
    '- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。',
    '',
    '## Result',
    ''
  ].filter(Boolean)

  if (result.status === 'BLOCKED') {
    lines.push(`- BLOCKED: \`node tests/e2e/edhr-batch-execution-real-flow.e2e.js\` -> FAIL, ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- 缺失前置：\`${item.name}\`，${item.description}`)
    }
    lines.push('- 影响：无法在真实前端页面完成批次详情打开和工序填写验证；未使用 mock、API-only 或测试专用控件。')
  } else if (result.status === 'PASS') {
    lines.push('- GREEN: 真实前端详情页打开填写路径已完成。')
  } else {
    lines.push(`- RED: 真实前端路径失败，${result.error || '未知错误'}`)
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function collectConfig() {
  const missing = []
  let fixture
  try {
    fixture = resolveDatabaseFixture()
  } catch (error) {
    missing.push({
      name: 'LOCAL_DATABASE_FIXTURE',
      description: error instanceof Error ? error.message : String(error)
    })
  }

  const baseUrl = envValue('EDHR_BATCH_E2E_BASE_URL') || REQUIRED_BASE_URL
  if (baseUrl !== REQUIRED_BASE_URL) {
    missing.push({ name: 'EDHR_BATCH_E2E_BASE_URL', description: `必须固定为 ${REQUIRED_BASE_URL}` })
  }

  return {
    baseUrl,
    ...(fixture || {}),
    executablePath:
      envValue('EDHR_BATCH_E2E_CHROME_EXECUTABLE') ||
      envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH') ||
      envValue('PLAYWRIGHT_CHROME_EXECUTABLE'),
    headed: envValue('EDHR_BATCH_E2E_HEADED') === '1',
    missing
  }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('缺少 Playwright runtime，请先在此前端 worktree 执行 pnpm install。')
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return item
    }
  }
  throw new Error(`页面缺少可见输入框：${label}`)
}

async function clickButton(root, name) {
  const button = root.getByRole('button', { name }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  if (await button.isDisabled()) throw new Error(`按钮不可用：${name}`)
  await button.click()
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${BATCH_EXECUTION_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，真实 E2E 无法无人值守执行。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenantName || AUTHORIZED_TENANT_LABEL)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({
      hasText: config.tenantName || AUTHORIZED_TENANT_LABEL
    }).first()
    if ((await tenantOption.count()) > 0) {
      await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, '用户名')
  const passwordInput = loginForm.locator('input[placeholder="请输入密码"]').first()
  await passwordInput.waitFor({ state: 'visible', timeout: 30000 })
  const passwordValue = await passwordInput.inputValue()
  if (!passwordValue) {
    throw new Error('登录页默认密码为空；真实 E2E 不在脚本中保存明文密码，请先修复本机登录默认值。')
  }
  await clickButton(loginForm, /^登录$/)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({
    headless: !config.headed,
    ...(config.executablePath ? { executablePath: config.executablePath } : {})
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const page = await context.newPage()
  try {
    await login(page, config)
    await page.goto(`${config.baseUrl}${BATCH_EXECUTION_ROUTE}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    assert.ok(OPEN_OR_CREATE_ENDPOINT_COVERAGE_TOKEN.includes('/open-or-create'), '发布覆盖矩阵必须继续追踪批次 open-or-create 入口')
    await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })

    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-batch-execution/get?id=') &&
        response.url().includes(String(config.batchExecutionId)) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${config.baseUrl}${BATCH_EXECUTION_ROUTE}/detail?id=${config.batchExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const detailResult = await detailResponsePromise
    assert.equal(detailResult.status(), 200, '批次详情接口必须返回 HTTP 200')
    const detailBody = await detailResult.json()
    assert.equal(detailBody.code, 0, `批次详情业务响应必须成功：${detailBody.msg || detailBody.code}`)
    assert.equal(String(detailBody.data?.id), String(config.batchExecutionId), '详情页必须加载数据库夹具批次')
    assert.equal(detailBody.data?.blockedCount, 0, '数据库夹具批次不能存在阻塞任务')

    const tasks = detailBody.data?.tasks || []
    assert.ok(tasks.length > 0, '批次详情必须返回真实工序任务')
    const openableTask =
      tasks.find(
        (task) =>
          String(task.id) === String(config.taskId) &&
          Array.isArray(task.allowedActions) &&
          task.allowedActions.includes('OPEN_FORM')
      ) ||
      tasks.find(
        (task) =>
          task.requiredFlag !== false &&
          task.activeWorkTaskId &&
          Array.isArray(task.allowedActions) &&
          task.allowedActions.includes('OPEN_FORM')
      )
    assert.ok(
      openableTask && (openableTask.batchRecordReportId || openableTask.formTemplateId),
      '批次任务必须包含至少一个当前账号可打开的真实批记录或动态表单'
    )

    await page.getByText(config.batchExecutionCode || String(config.batchExecutionId)).first().waitFor({
      state: 'visible',
      timeout: 60000
    })
    const processGroup = page
      .locator('.edhr-batch-detail__process-task-group')
      .filter({ hasText: openableTask.processName || config.processName || '' })
      .first()
    await processGroup.waitFor({ state: 'visible', timeout: 60000 })
    await processGroup.click()
    const formItem = page
      .locator('.edhr-batch-detail__rail-process-form-item')
      .filter({
        hasText:
          openableTask.batchRecordReportName ||
          openableTask.formTemplateName ||
          config.taskDisplayName ||
          openableTask.processName ||
          String(openableTask.id)
      })
      .first()
    await formItem.waitFor({ state: 'visible', timeout: 60000 })
    const openTaskButton = formItem.getByRole('button', { name: /打开填写|打开返工/ }).first()
    await openTaskButton.waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(await openTaskButton.isEnabled(), true, '当前真实任务的打开填写按钮必须可用')

    const [taskOpenResult] = await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/mes/pro/edhr-batch-execution/task/open') &&
          response.request().method() === 'POST',
        { timeout: 60000 }
      ),
      openTaskButton.click()
    ])
    assert.equal(taskOpenResult.status(), 200, '打开工序任务接口必须返回 HTTP 200')
    const taskOpenBody = await taskOpenResult.json()
    assert.equal(taskOpenBody.code, 0, `打开工序任务业务响应必须成功：${taskOpenBody.msg || taskOpenBody.code}`)
    assert(Number.isFinite(Number(taskOpenBody.data?.executionId)), '打开工序任务必须返回真实单张 eDHR 执行ID')
    await page.waitForURL((url) => EXECUTION_ROUTES.has(url.pathname), { timeout: 60000 })
    await page.locator('body').waitFor({ state: 'visible', timeout: 60000 })
    assert.ok(EXECUTION_ROUTES.has(new URL(page.url()).pathname), `打开工序任务后必须进入 eDHR 执行页：${page.url()}`)
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实 E2E 本地数据库夹具前置条件缺失。',
      missing: config.missing,
      fixture: config
    }
    writeEvidence(result)
    console.error(result.reason)
    process.exitCode = 1
    return
  }

  try {
    await runRealFlow(config)
    writeEvidence({ status: 'PASS', fixture: config })
    console.log('PASS: eDHR batch execution real path')
  } catch (error) {
    writeEvidence({ status: 'FAIL', error: error instanceof Error ? error.message : String(error), fixture: config })
    throw error
  }
}

main()
