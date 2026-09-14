const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_DYNAMIC_CELL_LINK_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  backendUrl: (process.env.EDHR_DYNAMIC_CELL_LINK_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
  tenantId: Number(process.env.EDHR_DYNAMIC_CELL_LINK_TENANT_ID || 122),
  tenantLabel: process.env.EDHR_DYNAMIC_CELL_LINK_TENANT_LABEL || '测试租户',
  username: process.env.EDHR_DYNAMIC_CELL_LINK_USERNAME || 'codexedhrcell01',
  password: process.env.EDHR_DYNAMIC_CELL_LINK_PASSWORD || '',
  batchExecutionId: Number(process.env.EDHR_DYNAMIC_CELL_LINK_BATCH_ID || 900000000784),
  batchTaskId: Number(process.env.EDHR_DYNAMIC_CELL_LINK_TASK_ID || 5368),
  mysqlContainer: process.env.EDHR_DYNAMIC_CELL_LINK_MYSQL_CONTAINER || 'int-ruoyi-mysql',
  databaseName: process.env.EDHR_DYNAMIC_CELL_LINK_DATABASE || 'ruoyi-vue-pro',
  headed: process.env.EDHR_DYNAMIC_CELL_LINK_HEADED === '1',
  executablePath:
    process.env.EDHR_DYNAMIC_CELL_LINK_CHROME_EXECUTABLE ||
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
    '',
  evidenceFile: path.resolve(
    process.env.EDHR_DYNAMIC_CELL_LINK_EVIDENCE_FILE ||
      path.join(
        __dirname,
        '..',
        '..',
        '..',
        'doc',
        'tasks',
        '20260728-edhr-cell-link-main-e2e-repair',
        'dynamic-form-real-e2e-evidence.md'
      )
  )
}

const SOURCE_TYPE = 'PRODUCTION_WORK_ORDER'
const SOURCE_FIELD_CODE = 'batchCode'
const SOURCE_FIELD_NAME = '生产批号'
const SOURCE_REPORT_ID = 'PRODUCTION_WORK_ORDER'
const SOURCE_REPORT_NAME = '生产工单'
const OVERWRITE_POLICY = 'ONLY_WHEN_EMPTY'
const SPECIAL_ATTACHMENT_OWNERS = [
  ['INCOMING_INSPECTION_REPORT', '来料检报告'],
  ['STERILIZATION_REPORT', '灭菌报告'],
  ['FINISHED_PRODUCT_INSPECTION_REPORT', '成品检报告'],
  ['FINISHED_PRODUCT_INSPECTION_RECORD', '成品检记录']
]

function ensureLocalRuntime() {
  assert.ok(['http://127.0.0.1:8081', 'http://localhost:8081'].includes(config.baseUrl), `front-end URL must be int_main local runtime: ${config.baseUrl}`)
  assert.equal(config.backendUrl, 'http://127.0.0.1:48081', `backend URL must be int_main local runtime: ${config.backendUrl}`)
  assert.equal(config.tenantId, 122, 'write E2E must stay in the authorized test tenant')
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

function mysql(sql, label) {
  const result = spawnSync(
    'docker',
    [
      'exec',
      '-i',
      config.mysqlContainer,
      'sh',
      '-lc',
      `MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -D${config.databaseName} -N -B`
    ],
    {
      input: sql,
      encoding: 'utf8',
      maxBuffer: 20 * 1024 * 1024
    }
  )
  if (result.error) throw new Error(`${label}: ${result.error.message}`)
  if (result.status !== 0) {
    throw new Error(`${label}: ${(result.stderr || '').trim() || `mysql exit ${result.status}`}`)
  }
  return parseMysqlRows(result.stdout || '')
}

function decodeBase64(value) {
  return value ? Buffer.from(value, 'base64').toString('utf8') : ''
}

function unwrapResult(body, label) {
  assert.ok(body && (body.code === 0 || body.code === 200), `${label} failed: ${JSON.stringify(body)}`)
  return body.data
}

function findBatchField(recognizedSchemaJson) {
  const fields = JSON.parse(recognizedSchemaJson)
  const index = fields.findIndex((field) => /批号/.test(String(field?.label || '')))
  assert.ok(index >= 0, `dynamic form template must contain a batch field: ${recognizedSchemaJson.slice(0, 300)}`)
  const fieldCode = String(fields[index]?.fieldCode || '').trim()
  assert.ok(fieldCode, `dynamic form batch field must have a fieldCode: ${JSON.stringify(fields[index])}`)
  return {
    rowIndex: Math.floor(index / 2) + 3,
    columnIndex: index % 2 === 0 ? 1 : 3,
    fieldCode,
    label: String(fields[index].label || '批号')
  }
}

function readFixture() {
  const rows = mysql(
    `
SELECT be.id,
       be.batch_execution_code,
       REPLACE(TO_BASE64(be.batch_code), '\n', ''),
       be.work_order_id,
       be.work_order_code,
       COALESCE(REPLACE(TO_BASE64(wo.batch_code), '\n', ''), ''),
       be.route_id,
       REPLACE(TO_BASE64(COALESCE(be.route_snapshot_json, JSON_OBJECT())), '\n', ''),
       t.id,
       t.route_process_id,
       COALESCE(t.process_id, 0),
       REPLACE(TO_BASE64(COALESCE(t.process_name, '')), '\n', ''),
       REPLACE(TO_BASE64(COALESCE(t.form_template_name_snapshot, '')), '\n', ''),
       t.form_template_id,
       t.form_template_version_id,
       t.form_center_instance_id,
       t.status,
       COALESCE(t.opened_by, 0),
       COALESCE(DATE_FORMAT(t.opened_at, '%Y-%m-%d %H:%i:%s'), ''),
       REPLACE(TO_BASE64(COALESCE(ai.form_data_json, '{}')), '\n', ''),
       REPLACE(TO_BASE64(COALESCE(tv.recognized_schema_json, '')), '\n', ''),
       u.id,
       u.username
  FROM mes_pro_edhr_batch_execution be
  JOIN mes_pro_work_order wo ON wo.id = be.work_order_id AND wo.deleted = b'0'
  JOIN mes_pro_edhr_batch_execution_task t ON t.batch_execution_id = be.id AND t.deleted = b'0'
  JOIN bpm_form_action_instance ai ON ai.id = t.form_center_instance_id AND ai.deleted = b'0'
  JOIN bpm_form_template_version tv ON tv.id = t.form_template_version_id AND tv.deleted = b'0'
  JOIN system_users u ON u.tenant_id = be.tenant_id AND u.username = ${sqlString(config.username)} AND u.deleted = b'0'
 WHERE be.id = ${Number(config.batchExecutionId)}
   AND t.id = ${Number(config.batchTaskId)}
   AND be.tenant_id = ${Number(config.tenantId)}
   AND be.deleted = b'0'
   AND t.form_template_version_id IS NOT NULL
   AND t.form_center_instance_id IS NOT NULL
 LIMIT 1;
`,
    'read dynamic form fixture'
  )
  assert.equal(rows.length, 1, 'authorized test tenant dynamic form fixture must exist')
  const row = rows[0]
  const batchCode = decodeBase64(row[2])
  assert.ok(batchCode, 'eDHR batch execution batchCode must not be empty')
  assert.equal(decodeBase64(row[5]), '', 'work order table batch_code must be empty to reproduce the original dynamic-form miss')
  const target = findBatchField(decodeBase64(row[20]))
  return {
    batchExecutionId: Number(row[0]),
    batchExecutionCode: row[1],
    batchCode,
    workOrderId: Number(row[3]),
    workOrderCode: row[4],
    routeId: Number(row[6]),
    originalRouteSnapshotJson: decodeBase64(row[7]),
    batchTaskId: Number(row[8]),
    routeProcessId: Number(row[9]),
    processId: Number(row[10]) || null,
    processName: decodeBase64(row[11]),
    templateName: decodeBase64(row[12]),
    formTemplateId: Number(row[13]),
    formTemplateVersionId: Number(row[14]),
    formCenterInstanceId: Number(row[15]),
    taskStatus: Number(row[16]),
    openedBy: Number(row[17]) || null,
    openedAt: row[18],
    originalFormDataJson: decodeBase64(row[19]),
    userId: Number(row[21]),
    username: row[22],
    target
  }
}

function ensureNoExistingRule(fixture) {
  const rows = mysql(
    `
SELECT id
  FROM mes_pro_batch_record_cell_link_rule
 WHERE tenant_id = ${Number(config.tenantId)}
   AND deleted = b'0'
   AND scope_type = 'FORM_TEMPLATE_VERSION'
   AND scope_id = ${Number(fixture.formTemplateVersionId)}
 LIMIT 1;
`,
    'check existing dynamic form cell-link rules'
  )
  assert.equal(rows.length, 0, `test fixture scope already has link rules; refusing to overwrite scope ${fixture.formTemplateVersionId}`)
}

function setupFixture(fixture) {
  ensureNoExistingRule(fixture)
  const targetCellKey = `${fixture.target.rowIndex}:${fixture.target.columnIndex}`
  const targetFormDataKey = fixture.target.fieldCode
  const now = Date.now()
  const taskCodePrefix = `EDHRT-CODX-DYN-${now}-`
  const hash = crypto.createHash('sha256').update(`codex-dynamic-cell-link|${now}|${targetCellKey}`).digest('hex')
  const attachmentOwners = SPECIAL_ATTACHMENT_OWNERS.map(([attachmentCode, attachmentName]) => ({
    attachmentCode,
    attachmentName,
    candidateSourceType: 'USERS',
    candidateSourceIds: [fixture.userId],
    candidateSourceNames: [fixture.username]
  }))
  const setupRows = mysql(
    `
UPDATE bpm_form_action_instance
   SET form_data_json = JSON_REMOVE(COALESCE(form_data_json, JSON_OBJECT()), '$."${targetCellKey}"', '$."${targetFormDataKey}"'),
       updater = 'codex-e2e',
       update_time = NOW()
 WHERE id = ${Number(fixture.formCenterInstanceId)}
   AND tenant_id = ${Number(config.tenantId)};
SELECT ROW_COUNT();
UPDATE mes_pro_edhr_batch_execution
   SET route_snapshot_json = JSON_SET(
           CASE
             WHEN JSON_EXTRACT(route_snapshot_json, '$.configSnapshots') IS NULL
             THEN JSON_SET(COALESCE(route_snapshot_json, JSON_OBJECT()), '$.configSnapshots', JSON_OBJECT())
             ELSE route_snapshot_json
           END,
           '$.configSnapshots.batchRecordAttachmentOwners',
           JSON_EXTRACT(${sqlString(JSON.stringify(attachmentOwners))}, '$')
       ),
       updater = 'codex-e2e',
       update_time = NOW()
 WHERE id = ${Number(fixture.batchExecutionId)}
   AND tenant_id = ${Number(config.tenantId)};
SELECT ROW_COUNT();
INSERT INTO mes_pro_batch_record_cell_link_rule (
    scope_type, scope_id, route_id, batch_record_definition_id, batch_record_version_id,
    source_type, source_report_id, source_report_name, source_row_index, source_column_index,
    source_cell_key, source_field_code, source_field_name, source_label, source_value_type,
    target_report_id, target_report_name, target_row_index, target_column_index, target_cell_key,
    target_label, target_value_type, overwrite_policy, template_snapshot_hash, rule_version,
    enabled, remark, creator, updater, tenant_id
) VALUES (
    'FORM_TEMPLATE_VERSION', ${Number(fixture.formTemplateVersionId)}, NULL, NULL, NULL,
    ${sqlString(SOURCE_TYPE)}, ${sqlString(SOURCE_REPORT_ID)}, ${sqlString(SOURCE_REPORT_NAME)}, -1, -1,
    ${sqlString(SOURCE_FIELD_CODE)}, ${sqlString(SOURCE_FIELD_CODE)}, ${sqlString(SOURCE_FIELD_NAME)}, ${sqlString(SOURCE_FIELD_NAME)}, 'STRING',
    ${sqlString(`FORMTPL:${fixture.formTemplateVersionId}`)}, ${sqlString(`${fixture.templateName} ${fixture.formTemplateVersionId}`)}, ${Number(fixture.target.rowIndex)}, ${Number(fixture.target.columnIndex)}, ${sqlString(targetCellKey)},
    ${sqlString(fixture.target.label)}, 'STRING', ${sqlString(OVERWRITE_POLICY)}, ${sqlString(hash)}, ${now},
    b'1', 'codex dynamic form cell-link E2E fixture', 'codex-e2e', 'codex-e2e', ${Number(config.tenantId)}
);
SELECT LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_work_task (
    task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
    work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id, process_name,
    assignee_user_id, candidate_source_type, candidate_user_snapshot, responsibility_source_type,
    responsibility_source_key, responsibility_source_version, fill_mode, signature_cell_key,
    status, due_time, action_url, remark, creator, updater, tenant_id
) VALUES (
    ${sqlString(`${taskCodePrefix}T`)}, 'FILL', ${Number(fixture.batchExecutionId)}, ${Number(fixture.batchTaskId)}, 'BATCH_TASK', ${Number(fixture.batchTaskId)},
    ${Number(fixture.workOrderId)}, ${sqlString(fixture.workOrderCode)}, ${sqlString(fixture.batchCode)}, ${Number(fixture.routeId)}, ${Number(fixture.routeProcessId)}, ${fixture.processId == null ? 'NULL' : Number(fixture.processId)}, ${sqlString(fixture.processName)},
    ${Number(fixture.userId)}, 'USERS', ${sqlString(String(fixture.userId))}, 'CODEX_E2E_DYNAMIC_CELL_LINK',
    ${sqlString(`BATCH_TASK|${fixture.batchTaskId}|FORMTPL:${fixture.formTemplateVersionId}`)}, ${sqlString(String(now))}, 'ASSIGNEE', '',
    'TODO', DATE_ADD(NOW(), INTERVAL 7 DAY), ${sqlString(`/mes/pro/feedback/edhr-batch-execution/detail?id=${fixture.batchExecutionId}&batchTaskId=${fixture.batchTaskId}`)}, 'codex dynamic form cell-link E2E fixture', 'codex-e2e', 'codex-e2e', ${Number(config.tenantId)}
);
SELECT LAST_INSERT_ID();
INSERT INTO mes_pro_edhr_work_task (
    task_code, task_type, batch_execution_id, batch_task_id, business_scope_type, business_scope_id,
    work_order_id, work_order_code, batch_code, route_id, route_process_id, process_id, process_name,
    assignee_user_id, candidate_source_type, candidate_user_snapshot, responsibility_source_type,
    responsibility_source_key, responsibility_source_version, fill_mode, signature_cell_key,
    status, due_time, action_url, remark, creator, updater, tenant_id
)
SELECT CONCAT(${sqlString(taskCodePrefix)}, 'C-', sibling.id), 'FILL', ${Number(fixture.batchExecutionId)}, sibling.id, 'BATCH_TASK', sibling.id,
       ${Number(fixture.workOrderId)}, ${sqlString(fixture.workOrderCode)}, ${sqlString(fixture.batchCode)}, ${Number(fixture.routeId)}, sibling.route_process_id, sibling.process_id, sibling.process_name,
       ${Number(fixture.userId)}, 'USERS', ${sqlString(String(fixture.userId))}, 'CODEX_E2E_DYNAMIC_CELL_LINK',
       CONCAT('BATCH_TASK|', sibling.id, '|FORMTPL:', COALESCE(sibling.form_template_version_id, '')), ${sqlString(String(now))}, 'ASSIGNEE', '',
       'TODO', DATE_ADD(NOW(), INTERVAL 7 DAY), ${sqlString(`/mes/pro/feedback/edhr-batch-execution/detail?id=${fixture.batchExecutionId}`)}, 'codex dynamic form cell-link E2E fixture', 'codex-e2e', 'codex-e2e', ${Number(config.tenantId)}
  FROM mes_pro_edhr_batch_execution_task sibling
 WHERE sibling.batch_execution_id = ${Number(fixture.batchExecutionId)}
   AND sibling.tenant_id = ${Number(config.tenantId)}
   AND sibling.deleted = b'0'
   AND sibling.id <> ${Number(fixture.batchTaskId)}
   AND sibling.route_process_id = ${Number(fixture.routeProcessId)}
   AND sibling.node_type = 'ROUTE_FORM'
   AND sibling.status = 0
   AND sibling.form_template_version_id IS NOT NULL
   AND sibling.form_center_instance_id IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
         FROM mes_pro_edhr_work_task existing
        WHERE existing.batch_task_id = sibling.id
          AND existing.tenant_id = ${Number(config.tenantId)}
          AND existing.deleted = b'0'
          AND existing.task_type IN ('FILL', 'REWORK')
          AND existing.status IN ('TODO', 'DOING', 'OVERDUE')
   );
SELECT ROW_COUNT();
SELECT COALESCE(GROUP_CONCAT(id ORDER BY id), '')
  FROM mes_pro_edhr_work_task
 WHERE task_code LIKE ${sqlString(`${taskCodePrefix}%`)}
   AND tenant_id = ${Number(config.tenantId)}
   AND deleted = b'0';
`,
    'setup dynamic form fixture'
  )
  assert.equal(Number(setupRows[0]?.[0]), 1, 'fixture formData cleanup must affect exactly one instance')
  assert.equal(Number(setupRows[1]?.[0]), 1, 'fixture route snapshot repair must affect exactly one batch')
  return {
    targetCellKey,
    targetFormDataKey,
    ruleId: Number(setupRows[2]?.[0]),
    workTaskId: Number(setupRows[3]?.[0]),
    companionTaskCount: Number(setupRows[4]?.[0] || 0),
    workTaskIds: String(setupRows[5]?.[0] || '').split(',').filter(Boolean).map(Number),
    taskCodePrefix
  }
}

function cleanupFixture(fixture, setup) {
  if (!fixture) return { skipped: true }
  const restoreJson = fixture.originalFormDataJson || '{}'
  const cleanupSql = `
${setup?.taskCodePrefix ? `DELETE FROM mes_pro_edhr_work_task WHERE task_code LIKE ${sqlString(`${setup.taskCodePrefix}%`)} AND tenant_id = ${Number(config.tenantId)};\nSELECT ROW_COUNT();` : 'SELECT 0;'}
${setup?.ruleId ? `DELETE FROM mes_pro_batch_record_cell_link_rule WHERE id = ${Number(setup.ruleId)} AND tenant_id = ${Number(config.tenantId)};\nSELECT ROW_COUNT();` : 'SELECT 0;'}
UPDATE bpm_form_action_instance
   SET form_data_json = ${sqlString(restoreJson)},
       updater = 'codex-e2e-rollback',
       update_time = NOW()
 WHERE id = ${Number(fixture.formCenterInstanceId)}
   AND tenant_id = ${Number(config.tenantId)};
SELECT ROW_COUNT();
UPDATE mes_pro_edhr_batch_execution
   SET route_snapshot_json = ${sqlString(fixture.originalRouteSnapshotJson || '{}')},
       updater = 'codex-e2e-rollback',
       update_time = NOW()
 WHERE id = ${Number(fixture.batchExecutionId)}
   AND tenant_id = ${Number(config.tenantId)};
SELECT ROW_COUNT();
UPDATE mes_pro_edhr_batch_execution_task
   SET opened_by = ${fixture.openedBy == null ? 'NULL' : Number(fixture.openedBy)},
       opened_at = ${fixture.openedAt ? sqlString(fixture.openedAt) : 'NULL'},
       updater = 'codex-e2e-rollback',
       update_time = NOW()
 WHERE id = ${Number(fixture.batchTaskId)}
   AND tenant_id = ${Number(config.tenantId)};
SELECT ROW_COUNT();
`
  const rows = mysql(cleanupSql, 'cleanup dynamic form fixture')
  return {
    workTaskDeleted: Number(rows[0]?.[0] || 0),
    ruleDeleted: Number(rows[1]?.[0] || 0),
    formDataRestored: Number(rows[2]?.[0] || 0),
    routeSnapshotRestored: Number(rows[3]?.[0] || 0),
    taskRestored: Number(rows[4]?.[0] || 0)
  }
}

function readPersistedValue(fixture, targetFormDataKey) {
  const rows = mysql(
    `
SELECT JSON_UNQUOTE(JSON_EXTRACT(form_data_json, '$."${targetFormDataKey}"'))
  FROM bpm_form_action_instance
 WHERE id = ${Number(fixture.formCenterInstanceId)}
   AND tenant_id = ${Number(config.tenantId)}
 LIMIT 1;
`,
    'read persisted dynamic form value'
  )
  return rows[0]?.[0] || ''
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible input: ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/feedback/edhr-batch-execution')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('login captcha is enabled')
  }
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(config.tenantLabel)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenantLabel }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenantLabel, 'tenant')
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"]):not([type="password"])'), config.username, 'username')
  const passwordInput = form.locator('input[placeholder="请输入密码"], input[type="password"]').first()
  await passwordInput.waitFor({ state: 'visible', timeout: 30000 })
  if (config.password) await passwordInput.fill(config.password)
  const currentPassword = await passwordInput.inputValue()
  assert.ok(currentPassword, 'login password must be supplied by local env or default login form')
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  unwrapResult(await (await responsePromise).json(), 'login')
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function verifyThroughUi(fixture, setup) {
  const browser = await chromium.launch({
    headless: !config.headed,
    ...(config.executablePath && fs.existsSync(config.executablePath)
      ? { executablePath: config.executablePath }
      : {})
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  try {
    await login(page)
    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/mes/pro/edhr-batch-execution/get?id=${fixture.batchExecutionId}`) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${config.baseUrl}/mes/pro/feedback/edhr-batch-execution/detail?id=${fixture.batchExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const detail = unwrapResult(await (await detailResponsePromise).json(), 'batch detail')
    const task = (detail.tasks || []).find((item) => Number(item.id) === Number(fixture.batchTaskId))
    assert.ok(
      task,
      `detail response must include target dynamic form task; returned=${JSON.stringify(
        (detail.tasks || []).map((item) => ({
          id: item.id,
          processName: item.processName,
          batchRecordReportName: item.batchRecordReportName,
          formTemplateName: item.formTemplateName,
          formCenterInstanceId: item.formCenterInstanceId,
          activeWorkTaskId: item.activeWorkTaskId,
          allowedActions: item.allowedActions
        }))
      )}; status=${detail.status}; blockedCount=${detail.blockedCount}; closeBlockers=${JSON.stringify(
        detail.closeBlockers || []
      )}; stageBlockers=${JSON.stringify(detail.stageBlockers || [])}`
    )
    assert.ok((task.allowedActions || []).includes('OPEN_FORM'), `target dynamic form task must be openable: ${JSON.stringify(task)}`)
    assert.equal(Number(task.activeWorkTaskId), Number(setup.workTaskId), 'detail must expose the task-owned active work task')

    const processGroup = page
      .locator('.edhr-batch-detail__process-task-group')
      .filter({ hasText: fixture.processName })
      .first()
    await processGroup.waitFor({ state: 'visible', timeout: 60000 })
    await processGroup.locator('.edhr-batch-detail__process-task-group-head').click()

    const formItem = page
      .locator('.edhr-batch-detail__rail-process-form-item')
      .filter({ hasText: fixture.templateName })
      .first()
    await formItem.waitFor({ state: 'visible', timeout: 60000 })
    const openButton = formItem.getByRole('button', { name: /打开填写|打开返工/ }).first()
    await openButton.waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(await openButton.isEnabled(), true, 'dynamic form open button must be enabled')

    const openResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await openButton.click()
    const opened = unwrapResult(await (await openResponsePromise).json(), 'task open')
    assert.equal(Number(opened.taskId), Number(fixture.batchTaskId), 'task/open must open the target batch task')
    assert.equal(Number(opened.formCenterInstanceId), Number(fixture.formCenterInstanceId), 'task/open must return the target FormCenter instance')
    assert.equal(Number(opened.formTemplateVersionId), Number(fixture.formTemplateVersionId), 'task/open must return the target template version')
    const persistedAfterOpen = readPersistedValue(fixture, setup.targetFormDataKey)
    assert.equal(persistedAfterOpen, fixture.batchCode, 'task/open must persist the dynamic form prefill before UI render')

    await page.locator('.el-drawer:visible, .form-action-panel').first().waitFor({ state: 'visible', timeout: 60000 })
    await page.waitForFunction(
      (expected) =>
        Array.from(document.querySelectorAll('input, textarea, [contenteditable="true"]')).some((control) => {
          const rawValue =
            control instanceof HTMLInputElement || control instanceof HTMLTextAreaElement
              ? control.value
              : control.textContent
          return String(rawValue || '').trim() === expected
        }),
      fixture.batchCode,
      { timeout: 60000 }
    )
    return opened
  } finally {
    await browser.close()
  }
}

function writeEvidence(result) {
  fs.mkdirSync(path.dirname(config.evidenceFile), { recursive: true })
  const lines = [
    '# eDHR 动态表单单元格链接真实 E2E Evidence',
    '',
    `- 状态：${result.status}`,
    `- 前端入口：\`${config.baseUrl}\``,
    `- 后端入口：\`${config.backendUrl}\``,
    `- 授权租户/账号：\`${config.tenantLabel}/${config.username}\`；不记录密码。`,
    result.fixture
      ? `- 批次/任务：\`${result.fixture.batchExecutionCode}\` / task \`${result.fixture.batchTaskId}\` / instance \`${result.fixture.formCenterInstanceId}\``
      : '',
    result.setup
      ? `- 临时规则/待办：rule \`${result.setup.ruleId}\`，workTask \`${result.setup.workTaskId}\`，target \`${result.setup.targetCellKey}\` -> \`${result.setup.targetFormDataKey}\``
      : '',
    '',
    '## BDD',
    '',
    '- BDD: Dynamic route form opens with production work order prefill -> Given 测试租户动态 FormCenter 表单存在 PRODUCTION_WORK_ORDER.batchCode 链接规则且工单表 batch_code 为空、eDHR 执行 batch_code 有值 When 用户从批次详情点击打开填写 Then FormCenter 实例草稿和页面控件必须显示 eDHR 执行上下文批号。',
    '',
    '## Result',
    ''
  ].filter(Boolean)

  if (result.status === 'PASS') {
    lines.push(`- GREEN: task/open 返回 FormCenter 实例 \`${result.opened.formCenterInstanceId}\`。`)
    lines.push(`- GREEN: bpm_form_action_instance.form_data_json[\`${result.setup.targetFormDataKey}\`] = \`${result.persistedValue}\`。`)
    lines.push(`- GREEN: 页面动态表单输入控件显示 \`${result.persistedValue}\`。`)
    lines.push(`- CLEANUP: ${JSON.stringify(result.cleanup)}`)
  } else {
    lines.push(`- FAIL: ${result.error}`)
    if (result.persistedValue) {
      lines.push(`- OBSERVED: bpm_form_action_instance.form_data_json[\`${result.setup.targetFormDataKey}\`] = \`${result.persistedValue}\`。`)
    }
    if (result.cleanup) lines.push(`- CLEANUP: ${JSON.stringify(result.cleanup)}`)
  }
  fs.writeFileSync(config.evidenceFile, `${lines.join('\n')}\n`, 'utf8')
}

async function main() {
  ensureLocalRuntime()
  const health = await fetch(`${config.backendUrl}/actuator/health`).then((response) => response.json())
  assert.equal(health.status, 'UP', 'backend health must be UP')
  const frontendStatus = await fetch(`${config.baseUrl}/`).then((response) => response.status)
  assert.equal(frontendStatus, 200, 'frontend must return HTTP 200')

  let fixture
  let setup
  let persistedValue
  try {
    fixture = readFixture()
    setup = setupFixture(fixture)
    const opened = await verifyThroughUi(fixture, setup)
    persistedValue = readPersistedValue(fixture, setup.targetFormDataKey)
    assert.equal(persistedValue, fixture.batchCode, 'dynamic form persisted value must equal eDHR execution batchCode')
    const cleanup = cleanupFixture(fixture, setup)
    writeEvidence({ status: 'PASS', fixture, setup, opened, persistedValue, cleanup })
    console.log(`PASS: dynamic form cell-link prefilled ${setup.targetCellKey}=${persistedValue}`)
  } catch (error) {
    let cleanup
    if (fixture && setup) {
      try {
        persistedValue = readPersistedValue(fixture, setup.targetFormDataKey)
      } catch {
        persistedValue = ''
      }
    }
    try {
      cleanup = cleanupFixture(fixture, setup)
    } catch (cleanupError) {
      cleanup = { error: cleanupError instanceof Error ? cleanupError.message : String(cleanupError) }
    }
    writeEvidence({
      status: 'FAIL',
      fixture,
      setup,
      error: error instanceof Error ? error.message : String(error),
      persistedValue,
      cleanup
    })
    throw error
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
