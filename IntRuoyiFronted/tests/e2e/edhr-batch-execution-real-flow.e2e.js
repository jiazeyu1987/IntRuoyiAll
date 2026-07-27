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
const CELL_LINK_SOURCE_TYPE = 'PRODUCTION_WORK_ORDER'
const CELL_LINK_SOURCE_FIELD_CODE = 'batchCode'
const AUTO_PERSIST_ACCEPTED_STATUSES = new Set(['APPLIED', 'NO_CHANGE_ALREADY_APPLIED'])

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

function executeDatabaseMutation(sql, label) {
  const rows = queryLocalDatabase(sql, label)
  const affectedRows = Number(rows.at(-1)?.[0])
  if (!Number.isInteger(affectedRows)) {
    throw new Error(`${label} 未返回有效 ROW_COUNT`)
  }
  return affectedRows
}

function sqlString(value) {
  return `'${String(value ?? '').replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

function parseTargetCellKey(cellKey) {
  const match = /^(\d+):(\d+)$/.exec(String(cellKey || ''))
  assert.ok(match, `单元格链接目标坐标无效：${cellKey}`)
  return {
    rowIndex: Number(match[1]),
    columnIndex: Number(match[2])
  }
}

function normalizeComparableValue(value) {
  return String(value ?? '').trim()
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
            wt.task_type,
            wt.assignee_user_id,
            COALESCE(assignee.username, ''),
            r.id,
            r.rule_version,
            r.target_cell_key,
            r.target_row_index,
            r.target_column_index,
            COALESCE(r.target_label, ''),
            COALESCE(r.source_field_code, ''),
            COALESCE(wo.batch_code, be.batch_code, '')
       FROM mes_pro_edhr_batch_execution be
       JOIN mes_pro_work_order wo
         ON wo.id = be.work_order_id
        AND wo.deleted = b'0'
       JOIN mes_pro_edhr_batch_execution_task t
         ON t.batch_execution_id = be.id
        AND t.deleted = b'0'
       JOIN mes_pro_edhr_work_task wt
         ON wt.batch_task_id = t.id
        AND wt.deleted = b'0'
        AND wt.task_type IN ('FILL', 'REWORK')
        AND wt.status IN ('TODO', 'DOING', 'OVERDUE')
       JOIN system_users assignee
         ON assignee.id = wt.assignee_user_id
        AND assignee.deleted = b'0'
       JOIN mes_pro_batch_record_cell_link_rule r
         ON r.tenant_id = ${AUTHORIZED_TENANT_ID}
        AND r.deleted = b'0'
        AND r.enabled = b'1'
        AND r.source_type COLLATE utf8mb4_unicode_ci = _utf8mb4'${CELL_LINK_SOURCE_TYPE}' COLLATE utf8mb4_unicode_ci
        AND r.source_field_code COLLATE utf8mb4_unicode_ci = _utf8mb4'${CELL_LINK_SOURCE_FIELD_CODE}' COLLATE utf8mb4_unicode_ci
        AND r.target_report_id COLLATE utf8mb4_unicode_ci = t.batch_record_report_id COLLATE utf8mb4_unicode_ci
        AND r.scope_type COLLATE utf8mb4_unicode_ci = _utf8mb4'ROUTE_VERSION' COLLATE utf8mb4_unicode_ci
        AND r.scope_id = t.batch_record_version_id
      WHERE be.deleted = b'0'
        AND be.tenant_id = ${AUTHORIZED_TENANT_ID}
        AND be.status IN (0, 10, 20, 25)
        AND be.blocked_count = 0
        AND t.required_flag = b'1'
        AND t.node_type = 'ROUTE_FORM'
        AND t.status NOT IN (40, 45, 50)
        AND t.execution_id IS NOT NULL
        AND t.batch_record_report_id IS NOT NULL
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
               CASE WHEN wt.assignee_user_id = ${userId} THEN 0 ELSE 1 END,
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
    workTaskType,
    originalAssigneeUserId,
    originalAssigneeUsername,
    cellLinkRuleId,
    cellLinkRuleVersion,
    targetCellKey,
    targetRowIndex,
    targetColumnIndex,
    targetLabel,
    sourceFieldCode,
    sourceBatchCode
  ] = batch

  assert.equal(blockedCount, '0', '数据库夹具批次不得存在阻塞任务')
  assert.ok(Number.isFinite(Number(batchExecutionId)), '数据库夹具必须包含真实批次执行 ID')
  assert.ok(Number.isFinite(Number(taskId)), '数据库夹具必须包含真实任务 ID')
  assert.ok(Number.isFinite(Number(executionId)), '数据库夹具必须包含真实 eDHR 执行 ID')
  assert.equal(sourceFieldCode, CELL_LINK_SOURCE_FIELD_CODE, '数据库夹具必须命中生产批号单元格链接规则')
  assert.ok(sourceBatchCode, '数据库夹具生产工单批号不得为空')

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
    originalAssigneeUserId,
    originalAssigneeUsername,
    cellLinkRuleId,
    cellLinkRuleVersion,
    targetCellKey,
    targetRowIndex: Number(targetRowIndex),
    targetColumnIndex: Number(targetColumnIndex),
    targetLabel,
    sourceFieldCode,
    sourceBatchCode,
    fixtureSource: `${MYSQL_CONTAINER_NAME}/${DATABASE_NAME}`
  }
}

function prepareAdminFixtureAccess(config) {
  const adjustment = {
    required: String(config.originalAssigneeUserId) !== String(config.userId),
    applied: false,
    affectedRows: 0,
    rollbackRows: 0,
    originalAssigneeUserId: config.originalAssigneeUserId,
    originalAssigneeUsername: config.originalAssigneeUsername,
    temporaryAssigneeUserId: config.userId,
    temporaryAssigneeUsername: config.username,
    workTaskId: config.workTaskId
  }
  if (!adjustment.required) {
    return adjustment
  }
  adjustment.affectedRows = executeDatabaseMutation(
    `UPDATE mes_pro_edhr_work_task
        SET assignee_user_id = ${Number(config.userId)},
            updater = 'codex-e2e',
            update_time = NOW()
      WHERE id = ${Number(config.workTaskId)}
        AND assignee_user_id = ${Number(config.originalAssigneeUserId)};
     SELECT ROW_COUNT();`,
    '临时切换 eDHR 待办责任人'
  )
  assert.equal(adjustment.affectedRows, 1, '临时切换 eDHR 待办责任人必须只影响 1 行')
  adjustment.applied = true
  return adjustment
}

function rollbackAdminFixtureAccess(adjustment) {
  if (!adjustment?.applied) {
    return adjustment
  }
  adjustment.rollbackRows = executeDatabaseMutation(
    `UPDATE mes_pro_edhr_work_task
        SET assignee_user_id = ${Number(adjustment.originalAssigneeUserId)},
            updater = 'codex-e2e-rollback',
            update_time = NOW()
      WHERE id = ${Number(adjustment.workTaskId)}
        AND assignee_user_id = ${Number(adjustment.temporaryAssigneeUserId)};
     SELECT ROW_COUNT();`,
    '回滚 eDHR 待办责任人'
  )
  if (adjustment.rollbackRows === 0) {
    const [current] = firstDatabaseRow(
      `SELECT assignee_user_id
         FROM mes_pro_edhr_work_task
        WHERE id = ${Number(adjustment.workTaskId)}
        LIMIT 1;`,
      '核对 eDHR 待办责任人回滚结果'
    )
    assert.equal(
      String(current),
      String(adjustment.originalAssigneeUserId),
      '回滚 eDHR 待办责任人影响 0 行时，当前责任人必须已经是原责任人'
    )
    adjustment.alreadyRestored = true
    return adjustment
  }
  assert.equal(adjustment.rollbackRows, 1, '回滚 eDHR 待办责任人必须只影响 1 行或已经恢复为原值')
  return adjustment
}

function selectBatchCodeAutoPersistItem(autoPersist, expectedBatchCode) {
  assert.ok(autoPersist && typeof autoPersist === 'object', 'task/open 响应必须包含 cellLinkAutoPersist')
  assert.ok(Array.isArray(autoPersist.items), 'cellLinkAutoPersist.items 必须是数组')
  const expectedValue = normalizeComparableValue(expectedBatchCode)
  const item = autoPersist.items.find(
    (candidate) =>
      candidate?.sourceType === CELL_LINK_SOURCE_TYPE &&
      candidate?.sourceFieldCode === CELL_LINK_SOURCE_FIELD_CODE &&
      AUTO_PERSIST_ACCEPTED_STATUSES.has(candidate?.status) &&
      normalizeComparableValue(candidate?.value) === expectedValue
  )
  assert.ok(
    item,
    `cellLinkAutoPersist 必须包含生产批号自动落库项，expected=${expectedValue} items=${JSON.stringify(autoPersist.items)}`
  )
  return item
}

function extractCellValues(detailData) {
  if (Array.isArray(detailData?.cellValues)) {
    return detailData.cellValues
  }
  if (typeof detailData?.cellValuesJson === 'string' && detailData.cellValuesJson.trim()) {
    const parsed = JSON.parse(detailData.cellValuesJson)
    return Array.isArray(parsed) ? parsed : []
  }
  return []
}

function findPersistedCellValue(detailData, item) {
  const target = parseTargetCellKey(item.targetCellKey)
  return extractCellValues(detailData).find(
    (cellValue) =>
      Number(cellValue?.rowIndex) === target.rowIndex &&
      Number(cellValue?.columnIndex) === target.columnIndex
  )
}

async function assertPageDisplaysPersistedValue(page, expectedValue) {
  const normalized = normalizeComparableValue(expectedValue)
  await page.waitForFunction(
    (value) => {
      const controls = Array.from(document.querySelectorAll('input, textarea, [contenteditable="true"]'))
      return controls.some((control) => {
        const rawValue =
          control instanceof HTMLInputElement || control instanceof HTMLTextAreaElement
            ? control.value
            : control.textContent
        return String(rawValue || '').trim() === value
      })
    },
    normalized,
    { timeout: 60000 }
  )
  return normalized
}

async function fetchExecutionDetailFromPage(page, executionId, workTaskId) {
  return await page.evaluate(
    async ({ targetExecutionId, targetWorkTaskId }) => {
      const url = new URL('/admin-api/mes/pro/batch-record-execution/get', window.location.origin)
      url.searchParams.set('id', String(targetExecutionId))
      if (targetWorkTaskId) {
        url.searchParams.set('workTaskId', String(targetWorkTaskId))
      }
      const response = await fetch(url.toString(), { credentials: 'include' })
      let body = null
      try {
        body = await response.json()
      } catch (error) {
        body = { code: 'JSON_PARSE_FAILED', msg: error instanceof Error ? error.message : String(error) }
      }
      return {
        status: response.status,
        body
      }
    },
    {
      targetExecutionId: executionId,
      targetWorkTaskId: workTaskId
    }
  )
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
    fixture.cellLinkRuleId ? `- 单元格链接规则：ruleId \`${fixture.cellLinkRuleId}\`，source \`${CELL_LINK_SOURCE_TYPE}.${CELL_LINK_SOURCE_FIELD_CODE}\`，target \`${fixture.targetCellKey}\`` : '',
    result.accessAdjustment?.required
      ? `- 临时责任人切换：workTaskId \`${result.accessAdjustment.workTaskId}\`，原责任人 \`${result.accessAdjustment.originalAssigneeUsername || result.accessAdjustment.originalAssigneeUserId}\` -> \`${AUTHORIZED_USERNAME}\`；回滚影响行数 \`${result.accessAdjustment.rollbackRows}\`。`
      : '',
    '',
    '## BDD',
    '',
    '- BDD: 数据库夹具发现 -> Given 本机数据库存在授权租户 admin 与非作废 eDHR 批次任务 When 执行真实 E2E Then 脚本从数据库读取批次、任务和执行 ID，不要求人工注入工单或批次环境变量。',
    '- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。',
    '- BDD: 单元格链接自动落库 -> Given 批记录存在生产工单 batchCode 链接规则 When 用户打开执行记录 Then `task/open` 返回 `cellLinkAutoPersist`，详情接口包含已保存单元格值，页面输入框显示相同值。',
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
    lines.push(`- GREEN: task/open 返回 cellLinkAutoPersist，状态 \`${result.autoPersistStatus}\`，目标单元格 \`${result.targetCellKey}\`，值 \`${result.persistedValue}\`。`)
    lines.push(`- GREEN: 执行详情 cellValues 包含目标单元格保存值；页面输入控件显示值 \`${result.pageDisplayedValue}\`。`)
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
    const autoPersist = taskOpenBody.data?.cellLinkAutoPersist
    const autoPersistItem = selectBatchCodeAutoPersistItem(autoPersist, config.sourceBatchCode)
    assert.equal(String(autoPersistItem.ruleId), String(config.cellLinkRuleId), '自动落库项必须来自数据库夹具命中的单元格链接规则')
    await page.waitForURL((url) => EXECUTION_ROUTES.has(url.pathname), { timeout: 60000 })
    const executionDetailResult = await fetchExecutionDetailFromPage(
      page,
      taskOpenBody.data?.executionId,
      taskOpenBody.data?.workTaskId || config.workTaskId
    )
    assert.equal(executionDetailResult.status, 200, '执行详情接口必须返回 HTTP 200')
    const executionDetailBody = executionDetailResult.body
    assert.equal(executionDetailBody.code, 0, `执行详情业务响应必须成功：${executionDetailBody.msg || executionDetailBody.code}`)
    assert.equal(
      String(executionDetailBody.data?.id),
      String(taskOpenBody.data?.executionId),
      '执行详情必须加载 task/open 返回的 executionId'
    )
    const persistedCellValue = findPersistedCellValue(executionDetailBody.data, autoPersistItem)
    assert.ok(persistedCellValue, `执行详情 cellValues 必须包含目标单元格：${autoPersistItem.targetCellKey}`)
    assert.equal(
      normalizeComparableValue(persistedCellValue.value),
      normalizeComparableValue(config.sourceBatchCode),
      '执行详情保存值必须等于生产工单批号'
    )
    await page.locator('body').waitFor({ state: 'visible', timeout: 60000 })
    assert.ok(EXECUTION_ROUTES.has(new URL(page.url()).pathname), `打开工序任务后必须进入 eDHR 执行页：${page.url()}`)
    const pageDisplayedValue = await assertPageDisplaysPersistedValue(page, config.sourceBatchCode)
    return {
      autoPersistStatus: autoPersistItem.status,
      targetCellKey: autoPersistItem.targetCellKey,
      persistedValue: normalizeComparableValue(persistedCellValue.value),
      pageDisplayedValue
    }
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

  let accessAdjustment
  try {
    accessAdjustment = prepareAdminFixtureAccess(config)
    let flowResult
    try {
      flowResult = await runRealFlow(config)
    } finally {
      rollbackAdminFixtureAccess(accessAdjustment)
    }
    writeEvidence({ status: 'PASS', fixture: config, accessAdjustment, ...flowResult })
    console.log('PASS: eDHR batch execution real path')
  } catch (error) {
    writeEvidence({
      status: 'FAIL',
      error: error instanceof Error ? error.message : String(error),
      fixture: config,
      accessAdjustment
    })
    throw error
  }
}

main()
