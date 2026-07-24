const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')

const TASK_ID = '20260612-edhr-attachment-prepare-upload-api'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-attachment-prereq')
const EVIDENCE_FILE = path.resolve(process.cwd(), 'doc', 'tasks', TASK_ID, 'real-upload-prereq-evidence.md')
const REQUIRED_BASE_URL = 'http://localhost:8081'
const TEMPLATE_ROUTE = '/mes/pro/batch-record-form-list'
const BATCH_ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])

function envValue(name) {
  return (process.env[name] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('缺少 Playwright runtime，请先在此前端 worktree 执行 pnpm install。')
  }
}

function runMysqlJson(sql) {
  const mysqlContainer = envValue('EDHR_ATTACHMENT_SETUP_MYSQL_CONTAINER') || 'int-ruoyi-mysql'
  const mysqlUser = envValue('EDHR_ATTACHMENT_SETUP_MYSQL_USER') || 'root'
  const mysqlPassword = envValue('EDHR_ATTACHMENT_SETUP_MYSQL_PASSWORD') || '123456'
  const mysqlDatabase = envValue('EDHR_ATTACHMENT_SETUP_MYSQL_DATABASE') || 'ruoyi-vue-pro'
  const result = spawnSync(
    'docker',
    [
      'exec',
      mysqlContainer,
      'mysql',
      `-u${mysqlUser}`,
      `-p${mysqlPassword}`,
      '--default-character-set=utf8mb4',
      '-N',
      '-B',
      mysqlDatabase,
      '-e',
      sql
    ],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) {
    throw new Error(`只读前置发现失败：${(result.stderr || result.stdout || '').trim() || `exit ${result.status}`}`)
  }
  const output = (result.stdout || '').trim()
  if (!output) return undefined
  try {
    return JSON.parse(output)
  } catch (error) {
    throw new Error(`只读前置发现返回不可解析 JSON：${output}`)
  }
}

function discoverDefaults() {
  const targetReport = runMysqlJson(`
SELECT JSON_OBJECT(
  'reportId', report_id,
  'reportCode', report_code,
  'reportName', report_name
) AS payload
FROM mes_pro_batch_record_report
WHERE tenant_id=122
  AND deleted=0
  AND report_code='EBR_TN122_A_T01'
LIMIT 1;`)
  const workOrder = runMysqlJson(`
SELECT JSON_OBJECT(
  'id', candidate.id,
  'code', candidate.code,
  'name', candidate.name,
  'routeId', candidate.route_id
) AS payload
FROM (
  SELECT w.id, w.code, w.name, t.route_id, MAX(be.id) AS latest_batch_id
  FROM mes_pro_work_order w
  JOIN mes_pro_task t
    ON t.work_order_id=w.id
   AND t.tenant_id=w.tenant_id
   AND t.deleted=0
   AND t.route_id IS NOT NULL
  LEFT JOIN mes_pro_edhr_batch_execution be
    ON be.work_order_id=w.id
   AND be.tenant_id=w.tenant_id
   AND be.deleted=0
   AND be.route_id=t.route_id
  JOIN (
    SELECT c.tenant_id, c.route_id, COUNT(DISTINCT c.route_process_id) AS enabled_count,
           COUNT(DISTINCT br.route_process_id) AS record_count,
           COUNT(DISTINCT r.report_id) AS valid_report_count
    FROM mes_pro_route_flow_process_config c
    JOIN mes_pro_route_flow_process_batch_record br
      ON br.tenant_id=c.tenant_id
     AND br.route_process_id=c.route_process_id
     AND br.use_type=c.use_type
     AND br.deleted=b'0'
    JOIN mes_pro_batch_record_report r
      ON r.tenant_id=br.tenant_id
     AND r.report_id=br.batch_record_report_id
     AND r.deleted=b'0'
    WHERE c.tenant_id=122
      AND c.deleted=b'0'
      AND c.use_type='BATCH'
      AND c.enabled=b'1'
    GROUP BY c.tenant_id, c.route_id
  ) route_ready
    ON route_ready.tenant_id=w.tenant_id
   AND route_ready.route_id=t.route_id
   AND route_ready.enabled_count=route_ready.record_count
   AND route_ready.enabled_count=route_ready.valid_report_count
   AND route_ready.enabled_count > 0
  WHERE w.tenant_id=122
    AND w.deleted=0
    AND w.temporary_frozen=b'0'
    AND w.status <> 4
  GROUP BY w.id, w.code, w.name, t.route_id
) candidate
ORDER BY (candidate.latest_batch_id IS NOT NULL) DESC, candidate.latest_batch_id DESC, candidate.id DESC
LIMIT 1;`)
  return { targetReport, workOrder }
}

function collectConfig() {
  const discovered = discoverDefaults()
  const tenant = envValue('EDHR_ATTACHMENT_SETUP_TENANT') || '测试租户'
  const config = {
    baseUrl: envValue('EDHR_ATTACHMENT_SETUP_BASE_URL') || REQUIRED_BASE_URL,
    tenant,
    username: envValue('EDHR_ATTACHMENT_SETUP_USERNAME') || 'aoteman',
    password: envValue('EDHR_ATTACHMENT_SETUP_PASSWORD') || envValue('EDHR_ATTACHMENT_E2E_PASSWORD'),
    reportCode: envValue('EDHR_ATTACHMENT_SETUP_REPORT_CODE') || discovered.targetReport?.reportCode,
    reportId: envValue('EDHR_ATTACHMENT_SETUP_REPORT_ID') || discovered.targetReport?.reportId,
    workOrderCode: envValue('EDHR_ATTACHMENT_SETUP_WORK_ORDER_CODE') || discovered.workOrder?.code,
    batchCode: envValue('EDHR_ATTACHMENT_SETUP_BATCH_CODE') || `E2E-122-ATTACH-${Date.now()}`,
    routeId: envValue('EDHR_ATTACHMENT_SETUP_ROUTE_ID') || discovered.workOrder?.routeId,
    headed: envValue('EDHR_ATTACHMENT_SETUP_HEADED') === '1',
    discovered
  }
  const missing = []
  if (!config.password) missing.push(['EDHR_ATTACHMENT_SETUP_PASSWORD', '测试租户登录密码'])
  if (!config.reportCode || !config.reportId) missing.push(['EDHR_ATTACHMENT_SETUP_REPORT_CODE', '测试租户批记录报表编码'])
  if (!config.workOrderCode) missing.push(['EDHR_ATTACHMENT_SETUP_WORK_ORDER_CODE', '真实生产工单编码'])
  if (config.baseUrl !== REQUIRED_BASE_URL) missing.push(['EDHR_ATTACHMENT_SETUP_BASE_URL', `必须固定为 ${REQUIRED_BASE_URL}`])
  if (FORBIDDEN_TENANTS.has(config.tenant.toLowerCase()) || config.tenant.includes('芋道源码')) {
    missing.push(['EDHR_ATTACHMENT_SETUP_TENANT', '禁止使用正式或芋道源码租户'])
  }
  return { ...config, missing }
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 附件上传真实前置创建 Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 状态：${result.status}`,
    `- 前端入口：\`${REQUIRED_BASE_URL}\``,
    '- 创建方式：Playwright 操作测试租户真实前端页面；密码由环境变量注入，不写入仓库。',
    '',
    '## BDD',
    '',
    '- BDD: 先创建附件上传前置 -> Given 测试租户存在真实批记录模板和生产工单 / When 通过前端配置模板上传字段并打开批次工序 / Then 生成包含 upload-file 字段的 DRAFT execution 和 TODO/DOING 工作任务。',
    '',
    '## Result',
    ''
  ]
  if (result.status === 'BLOCKED') {
    lines.push(`- BLOCKED: 前置创建未执行，${result.reason}`)
    for (const [key, description] of result.missing || []) {
      lines.push(`- 缺失前置：\`${key}\`，${description}`)
    }
  } else if (result.status === 'PASS') {
    lines.push('- GREEN: 已通过真实前端路径创建附件上传 E2E 前置。')
    lines.push(`- reportCode：\`${result.reportCode}\``)
    lines.push(`- workOrderCode：\`${result.workOrderCode}\``)
    lines.push(`- routeId：\`${result.routeId}\``)
    lines.push(`- batchCode：\`${result.batchCode}\``)
    lines.push(`- executionId：\`${result.executionId}\``)
    lines.push(`- workTaskId：\`${result.workTaskId}\``)
  } else {
    lines.push(`- RED: 前置创建失败，${result.error || '未知错误'}`)
    if (result.context) {
      lines.push(`- context：\`${JSON.stringify(result.context)}\``)
    }
  }
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
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
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(TEMPLATE_ROUTE)}`, {
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
    await tenantInput.fill(config.tenant)
    await page.keyboard.press('Enter')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, '密码')
  await clickButton(loginForm, /^登录$/)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function configureUploadCellRule(page, config) {
  await page.goto(`${config.baseUrl}${TEMPLATE_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('按报表名称或编码搜索').first().waitFor({ state: 'visible', timeout: 60000 }).catch(() => null)
  const searchInput = page.locator('input[placeholder="按报表名称或编码搜索"]').first()
  await searchInput.fill(config.reportCode)
  const pageResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/batch-record-report/page') && response.status() === 200,
    { timeout: 30000 }
  )
  await page.keyboard.press('Enter')
  await pageResponse
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: config.reportCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.getByRole('button', { name: '单元格规则' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '单元格规则' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('.batch-record-rule-sheet__cell:not(.is-empty)').first().click()
  await dialog.locator('.batch-record-rule-config__editor').filter({ hasText: '控件' }).waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(
    dialog.locator('.el-form-item').filter({ hasText: '控件' }).locator('input'),
    'upload-file',
    '控件'
  )
  await fillFirstVisible(
    dialog.locator('.el-form-item').filter({ hasText: '标签' }).locator('input'),
    '附件上传证据',
    '标签'
  )
  const reviewed = dialog.locator('.el-form-item').filter({ hasText: '确认' }).locator('.el-checkbox').first()
  if ((await reviewed.count()) > 0 && !(await reviewed.evaluate((el) => el.classList.contains('is-checked')))) {
    await reviewed.click()
  }
  const saveResponse = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/mes/pro/batch-record-report/cell-rules'),
    { timeout: 30000 }
  )
  await clickButton(dialog, /^保存$/)
  const response = await saveResponse
  const body = await response.json()
  assert.equal(body.code, 0, `保存单元格规则失败：${body.msg || body.code}`)
  await page.getByText('单元格规则已保存').first().waitFor({ state: 'visible', timeout: 10000 }).catch(() => null)
}

async function createBatchAndOpenTask(page, config) {
  await page.goto(`${config.baseUrl}${BATCH_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })
  await clickButton(page, '打开/创建')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const workOrderInput = dialog.locator('.el-form-item').filter({ hasText: '生产工单' }).locator('input').first()
  await workOrderInput.click()
  await workOrderInput.fill(config.workOrderCode)
  await page.waitForResponse(
    (response) => response.url().includes('/mes/pro/work-order/page') && response.status() === 200,
    { timeout: 30000 }
  )
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: config.workOrderCode }).first().click()
  await fillFirstVisible(dialog.locator('.el-form-item').filter({ hasText: '批次号' }).locator('input'), config.batchCode, '批次号')
  if (config.routeId) {
    await fillFirstVisible(dialog.locator('.el-form-item').filter({ hasText: '路线ID' }).locator('input'), String(config.routeId), '路线ID')
  }
  const openBatchResponse = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/mes/pro/edhr-batch-execution/open-or-create'),
    { timeout: 60000 }
  )
  await clickButton(dialog, /^确\s*认$/)
  const openBatch = await openBatchResponse
  const openBatchBody = await openBatch.json()
  assert.equal(
    openBatchBody.code,
    0,
    `打开/创建批次失败：${openBatchBody.msg || openBatchBody.message || openBatchBody.code}，请求：${JSON.stringify({
      workOrderCode: config.workOrderCode,
      routeId: config.routeId,
      batchCode: config.batchCode
    })}，响应：${JSON.stringify(openBatchBody)}`
  )
  const batchId = openBatchBody.data?.id
  assert.ok(batchId, '打开/创建批次未返回 id')
  await page.waitForURL((url) => url.pathname === `${BATCH_ROUTE}/detail`, { timeout: 60000 })
  await page.getByText(config.batchCode).first().waitFor({ state: 'visible', timeout: 60000 })
  const targetRow = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: config.reportCode.replace('EBR_TN122_A_', '表') }).first()
  const row = (await targetRow.count()) > 0
    ? targetRow
    : page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: '产品信息' }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const openTaskResponse = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/mes/pro/edhr-batch-execution/task/open'),
    { timeout: 60000 }
  )
  await row.getByRole('button', { name: /打开填写|继续填写/ }).first().click()
  const openTask = await openTaskResponse
  const openTaskBody = await openTask.json()
  assert.equal(openTaskBody.code, 0, `打开工序任务失败：${openTaskBody.msg || openTaskBody.code}`)
  const executionId = openTaskBody.data?.executionId
  assert.ok(executionId, '打开工序任务未返回 executionId')
  await page.waitForURL((url) => url.pathname === '/mes/pro/feedback/edhr-execution/form', { timeout: 60000 })
  const workTaskId = discoverWorkTaskId(executionId)
  return { batchId, executionId, workTaskId }
}

function discoverWorkTaskId(executionId) {
  const result = runMysqlJson(`
SELECT JSON_OBJECT('workTaskId', id) AS payload
FROM mes_pro_edhr_work_task
WHERE tenant_id=122
  AND deleted=0
  AND execution_id=${Number(executionId)}
  AND assignee_user_id=113
  AND status IN ('TODO','DOING')
ORDER BY id DESC
LIMIT 1;`)
  assert.ok(result?.workTaskId, `未发现 executionId=${executionId} 对应的 aoteman 活动工作任务`)
  return result.workTaskId
}

async function runRealPrereq(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page, config)
    await configureUploadCellRule(page, config)
    const created = await createBatchAndOpenTask(page, config)
    return {
      reportCode: config.reportCode,
      workOrderCode: config.workOrderCode,
      routeId: config.routeId,
      batchCode: config.batchCode,
      ...created
    }
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    writeEvidence({
      status: 'BLOCKED',
      reason: '真实前置创建缺少必要输入或命中受保护租户。',
      missing: config.missing
    })
    console.error('真实前置创建缺少必要输入或命中受保护租户。')
    process.exitCode = 1
    return
  }
  try {
    const result = await runRealPrereq(config)
    writeEvidence({ status: 'PASS', ...result })
    console.log(`PASS: eDHR attachment prereq created executionId=${result.executionId} workTaskId=${result.workTaskId}`)
  } catch (error) {
    writeEvidence({
      status: 'FAIL',
      error: error instanceof Error ? error.message : String(error),
      context: {
        reportCode: config.reportCode,
        workOrderCode: config.workOrderCode,
        routeId: config.routeId,
        batchCode: config.batchCode,
        discovered: config.discovered
      }
    })
    throw error
  }
}

main()
