const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260615-edhr-tail-four-goals-design'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-tail-four-goals')
const EVIDENCE_FILE = path.resolve(process.cwd(), '..', 'doc', 'tasks', TASK_ID, 'real-e2e-evidence.md')

const REQUIRED_BASE_URL = 'http://localhost:8081'
const REQUIRED_TENANT = '测试租户'
const REQUIRED_USERNAME = 'aoteman'
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])
const ABILITY_LABELS = {
  AUDIT_VIEW: '审计查看'
}

const ROUTES = {
  batchExecution: '/mes/pro/feedback/edhr-batch-execution',
  operationAudit: '/mes/pro/feedback/edhr-operation-audit',
  permissionMatrix: '/mes/pro/feedback/edhr-permission-matrix'
}

const REQUIRED_ENV = [
  ['EDHR_TAIL_FOUR_E2E_BASE_URL', `本机真实前端入口，必须为 ${REQUIRED_BASE_URL}`],
  ['EDHR_TAIL_FOUR_E2E_TENANT', `写入型 E2E 租户，必须为 ${REQUIRED_TENANT}`],
  ['EDHR_TAIL_FOUR_E2E_USERNAME', `写入型 E2E 账号，必须为 ${REQUIRED_USERNAME}`],
  ['EDHR_TAIL_FOUR_E2E_PASSWORD', '测试租户账号真实登录密码'],
  ['EDHR_TAIL_FOUR_E2E_SIGNATURE_PASSWORD', '当前登录账号真实电子签名密码'],
  ['EDHR_TAIL_FOUR_E2E_ROUTE_CONFIG_PATH', '真实批处理路线配置页路径，例如 /mes/pro/edhr-batch-route'],
  ['EDHR_TAIL_FOUR_E2E_ROUTE_QUERY', '可唯一定位目标工艺路线的编码或名称'],
  ['EDHR_TAIL_FOUR_E2E_INTERNAL_REPORT_ID', '可绑定为内部记录表的真实批记录报表 ID'],
  ['EDHR_TAIL_FOUR_E2E_PERMISSION_SCOPE_ID', '真实对象权限范围 ID'],
  ['EDHR_TAIL_FOUR_E2E_WORK_ORDER_ID', '真实生产工单 ID'],
  ['EDHR_TAIL_FOUR_E2E_WORK_ORDER_QUERY', '真实生产工单查询关键字，必须能在页面远程下拉中唯一定位目标工单'],
  ['EDHR_TAIL_FOUR_E2E_BATCH_CODE', '本次 E2E 使用的真实批次号'],
  ['EDHR_TAIL_FOUR_E2E_FIELD_LABEL', '内部记录表中一个可编辑数值字段的页面标签'],
  ['EDHR_TAIL_FOUR_E2E_OUT_OF_RANGE_VALUE', '用于验证内部追溯校验不因上下限阻断的数值'],
  ['EDHR_TAIL_FOUR_E2E_WRITER_USER_ID', '写入账号在系统用户表中的真实用户 ID'],
  ['EDHR_TAIL_FOUR_E2E_DENIED_USER_ID', '无目标对象权限账号在系统用户表中的真实用户 ID'],
  ['EDHR_TAIL_FOUR_E2E_DENIED_USERNAME', '无目标对象权限的真实账号'],
  ['EDHR_TAIL_FOUR_E2E_DENIED_PASSWORD', '无目标对象权限账号的真实登录密码']
]

const BDD_SCENARIOS = [
  'BDD: 内部记录表真实配置 -> Given 测试租户存在真实工艺路线、工序、报表模板和权限范围 When 用户在批处理路线配置页把目标报表设置为内部记录表 Then 保存接口返回真实成功且页面保留 INTERNAL_RECORD / INTERNAL_TRACE 元数据。',
  'BDD: 内部记录表真实填写签名 -> Given 批次任务打开到内部记录表执行页 When 用户填写超出数值上下限的目标字段、输入变更原因、选择签名显示时间并签名 Then 字段审计保存成功，签名返回服务器签名时间、选择签名时间和显示签名时间证据。',
  'BDD: eDHR 全操作审计真实可查 -> Given 已发生配置、打开、保存和签名操作 When 用户进入 eDHR 操作审计页按执行对象查询 Then 页面和 API 均返回成功事件、权限决策和审计事件 ID。',
  'BDD: 对象级权限真实拒绝 -> Given 另一个测试租户账号没有目标对象能力 When 该账号登录并打开同一 eDHR 执行对象 Then 页面显式展示无权限/403/404/拒绝结果，并且不通过前端隐藏或接口绕过实现。',
  'BDD: 缺少真实前置即阻塞 -> Given 真实账号、签名密码、目标路线、报表、工单、批次、字段或权限范围任一缺失 When 执行脚本 Then 脚本写入 BLOCKED 证据并退出非零，不使用 mock、默认成功或 API-only 替代。'
]

function envValue(name) {
  return (process.env[name] || '').trim()
}

function normalizeSelectedSignedAtForApi(value) {
  return String(value || '').trim().replace(
    /^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2})$/,
    '$1T$2'
  )
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function isForbiddenTenant(tenant) {
  const value = String(tenant || '').trim()
  const lower = value.toLowerCase()
  return FORBIDDEN_TENANTS.has(lower) || value.includes('芋道源码')
}

function requireNumber(value, name, missing) {
  if (!String(value || '').trim()) return
  if (!/^\d+$/.test(String(value || '').trim())) {
    missing.push({ name, description: '必须是真实数字 ID，不能是空值、编号或占位文本。' })
  }
}

function collectConfig() {
  const missing = REQUIRED_ENV.filter(([name]) => !envValue(name)).map(([name, description]) => ({
    name,
    description
  }))

  const config = {
    baseUrl: envValue('EDHR_TAIL_FOUR_E2E_BASE_URL'),
    tenant: envValue('EDHR_TAIL_FOUR_E2E_TENANT'),
    username: envValue('EDHR_TAIL_FOUR_E2E_USERNAME'),
    password: envValue('EDHR_TAIL_FOUR_E2E_PASSWORD'),
    signaturePassword: envValue('EDHR_TAIL_FOUR_E2E_SIGNATURE_PASSWORD'),
    routeConfigPath: envValue('EDHR_TAIL_FOUR_E2E_ROUTE_CONFIG_PATH'),
    routeQuery: envValue('EDHR_TAIL_FOUR_E2E_ROUTE_QUERY'),
    internalReportId: envValue('EDHR_TAIL_FOUR_E2E_INTERNAL_REPORT_ID'),
    permissionScopeId: envValue('EDHR_TAIL_FOUR_E2E_PERMISSION_SCOPE_ID'),
    workOrderId: envValue('EDHR_TAIL_FOUR_E2E_WORK_ORDER_ID'),
    workOrderQuery: envValue('EDHR_TAIL_FOUR_E2E_WORK_ORDER_QUERY'),
    batchCode: envValue('EDHR_TAIL_FOUR_E2E_BATCH_CODE'),
    routeId: envValue('EDHR_TAIL_FOUR_E2E_ROUTE_ID'),
    fieldLabel: envValue('EDHR_TAIL_FOUR_E2E_FIELD_LABEL'),
    outOfRangeValue: envValue('EDHR_TAIL_FOUR_E2E_OUT_OF_RANGE_VALUE'),
    writerUserId: envValue('EDHR_TAIL_FOUR_E2E_WRITER_USER_ID'),
    deniedUserId: envValue('EDHR_TAIL_FOUR_E2E_DENIED_USER_ID'),
    deniedUsername: envValue('EDHR_TAIL_FOUR_E2E_DENIED_USERNAME'),
    deniedPassword: envValue('EDHR_TAIL_FOUR_E2E_DENIED_PASSWORD'),
    selectedSignedAt: envValue('EDHR_TAIL_FOUR_E2E_SELECTED_SIGNED_AT') || '2026-06-15 09:30:00',
    selectedTimeZone: envValue('EDHR_TAIL_FOUR_E2E_SELECTED_TIME_ZONE') || 'Asia/Shanghai',
    selectedTimeReason:
      envValue('EDHR_TAIL_FOUR_E2E_SELECTED_TIME_REASON') ||
      'P8 real E2E selected signature time evidence',
    headed: envValue('EDHR_TAIL_FOUR_E2E_HEADED') === '1'
  }

  if (config.baseUrl && config.baseUrl.replace(/\/+$/, '') !== REQUIRED_BASE_URL) {
    missing.push({
      name: 'EDHR_TAIL_FOUR_E2E_BASE_URL',
      description: `当前值为 ${config.baseUrl}；本任务只允许本机 ${REQUIRED_BASE_URL}。`
    })
  }
  if (config.tenant && config.tenant !== REQUIRED_TENANT) {
    missing.push({
      name: 'EDHR_TAIL_FOUR_E2E_TENANT',
      description: `当前值为 ${config.tenant}；写入型 E2E 必须使用 ${REQUIRED_TENANT}。`
    })
  }
  if (config.tenant && isForbiddenTenant(config.tenant)) {
    missing.push({
      name: 'EDHR_TAIL_FOUR_E2E_TENANT',
      description: '当前租户命中 live/prod 保护名单，禁止执行写入型 E2E。'
    })
  }
  if (config.username && config.username !== REQUIRED_USERNAME) {
    missing.push({
      name: 'EDHR_TAIL_FOUR_E2E_USERNAME',
      description: `当前值为 ${config.username}；写入型 E2E 必须使用 ${REQUIRED_USERNAME}。`
    })
  }
  if (config.deniedUsername && config.deniedUsername === config.username) {
    missing.push({
      name: 'EDHR_TAIL_FOUR_E2E_DENIED_USERNAME',
      description: '无权限账号必须与写入账号不同，不能用同一账号证明对象级拒绝。'
    })
  }
  if (config.routeConfigPath && !config.routeConfigPath.startsWith('/')) {
    missing.push({
      name: 'EDHR_TAIL_FOUR_E2E_ROUTE_CONFIG_PATH',
      description: '页面路径必须以 / 开头。'
    })
  }
  if (config.internalReportId && !/^[0-9a-zA-Z_-]+$/.test(config.internalReportId)) {
    missing.push({
      name: 'EDHR_TAIL_FOUR_E2E_INTERNAL_REPORT_ID',
      description: '必须是真实批记录报表 report_id，不能包含空白或占位文本。'
    })
  }
  requireNumber(config.permissionScopeId, 'EDHR_TAIL_FOUR_E2E_PERMISSION_SCOPE_ID', missing)
  requireNumber(config.workOrderId, 'EDHR_TAIL_FOUR_E2E_WORK_ORDER_ID', missing)
  requireNumber(config.writerUserId, 'EDHR_TAIL_FOUR_E2E_WRITER_USER_ID', missing)
  requireNumber(config.deniedUserId, 'EDHR_TAIL_FOUR_E2E_DENIED_USER_ID', missing)
  if (config.routeId) requireNumber(config.routeId, 'EDHR_TAIL_FOUR_E2E_ROUTE_ID', missing)
  if (config.outOfRangeValue && !/^[-+]?\d+(\.\d+)?$/.test(config.outOfRangeValue)) {
    missing.push({
      name: 'EDHR_TAIL_FOUR_E2E_OUT_OF_RANGE_VALUE',
      description: '必须是真实数值，用于证明内部记录表不因上下限阻断。'
    })
  }

  return { ...config, missing }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error('缺少 Playwright runtime，请先在 yudao-ui-admin-vue3 执行 pnpm install。')
    blocked.blocked = true
    throw blocked
  }
}

function parseBusinessData(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 响应必须是对象。`)
  if (Object.prototype.hasOwnProperty.call(body, 'code')) {
    assert.ok(
      body.code === 0 || body.code === 200,
      `${label} 业务状态码应为 0 或 200，实际 ${body.code}: ${body.msg || body.message || ''}`
    )
    return body.data
  }
  return body
}

async function parseJsonResponse(response, label) {
  assert.equal(response.status(), 200, `${label} HTTP 状态应为 200，实际 ${response.status()}，URL=${response.url()}`)
  return parseBusinessData(await response.json(), label)
}

async function firstVisible(locator, failureMessage) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(failureMessage)
}

async function fillFirstVisible(locator, value, failureMessage) {
  const item = await firstVisible(locator, failureMessage)
  await item.fill('')
  await item.fill(value)
}

async function fillDateTimePickerInput(locator, value, failureMessage) {
  const item = await firstVisible(locator, failureMessage)
  await item.click()
  await item.fill('')
  await item.fill(value)
  await item.press('Enter')
  await item.blur()
}

async function clickVisibleButton(scope, namePattern, failureMessage) {
  const button = await firstVisible(scope.getByRole('button', { name: namePattern }), failureMessage)
  if (await button.isDisabled()) throw new Error(`${failureMessage} 按钮处于禁用状态。`)
  await button.click()
}

function formItem(page, surfaceSelector, label) {
  return page.locator(`${surfaceSelector} .el-form-item`).filter({ hasText: label }).first()
}

async function setSelectByVisibleText(page, selectRoot, optionText) {
  await selectRoot.click()
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await dropdown.waitFor({ state: 'visible', timeout: 30000 })
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click({ force: true })
}

async function selectVisibleOption(page, root, placeholder, optionText, failureMessage) {
  const input = await firstVisible(root.locator(`input[placeholder="${placeholder}"]`), `${failureMessage}：缺少 ${placeholder}。`)
  await input.click()
  await input.fill('')
  await input.fill(optionText)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function selectElementOption(page, selectLocator, optionText, failureMessage) {
  const select = await firstVisible(selectLocator, failureMessage)
  await select.click()
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await dropdown.waitFor({ state: 'visible', timeout: 30000 })
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click({ force: true })
}

async function selectElementOptionIfNeeded(page, selectLocator, optionText, failureMessage) {
  const select = await firstVisible(selectLocator, failureMessage)
  const selectedText = `${(await select.textContent().catch(() => '')) || ''} ${
    (await select.locator('input').first().inputValue().catch(() => '')) || ''
  }`.replace(/\s+/g, '')
  if (selectedText.includes(optionText.replace(/\s+/g, ''))) return
  await selectElementOption(page, selectLocator, optionText, failureMessage)
}

async function selectRemoteWorkOrder(page, dialog, config) {
  const workOrderItem = dialog.locator('.el-form-item').filter({ hasText: '生产工单' }).first()
  await workOrderItem.waitFor({ state: 'visible', timeout: 30000 })
  const input = await firstVisible(
    workOrderItem.locator('input'),
    '打开/创建弹窗缺少生产工单远程下拉输入框。'
  )
  await input.click()
  const searchResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/work-order/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await input.fill('')
  await input.fill(config.workOrderQuery)
  await parseJsonResponse(await searchResponsePromise, '生产工单远程查询')
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await dropdown.waitFor({ state: 'visible', timeout: 30000 })
  const option = dropdown
    .locator('.el-select-dropdown__item')
    .filter({ hasText: `ID ${config.workOrderId}` })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click({ force: true })
}

async function login(page, runtime, account) {
  await page.goto(`${runtime.baseUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page
    .locator('form.login-form')
    .filter({ has: page.getByPlaceholder('请输入用户名') })
    .filter({ hasText: '记住我' })
    .first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })

  const captchaInput = loginForm.locator('input[placeholder*="验证码"]').first()
  if ((await captchaInput.count()) > 0 && (await captchaInput.isVisible())) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = loginForm.locator('input.el-select__input:visible').first()
  if ((await tenantInput.count()) === 0) {
    throw new Error('登录页缺少可见租户选择输入框，无法确认租户上下文。')
  }
  await tenantInput.click()
  await page.keyboard.press('Control+A')
  await page.keyboard.type(runtime.tenant)
  await page.keyboard.press('Enter')
  await page.waitForTimeout(400)

  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), account.username, '登录页缺少用户名输入框。')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), account.password, '登录页缺少密码输入框。')
  await clickVisibleButton(loginForm, /^登录$/, '登录页缺少登录按钮。')
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })
}

async function resetSession(context, page) {
  await context.clearCookies()
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  })
}

async function configureInternalRecord(page, config, steps) {
  await page.goto(`${config.baseUrl}${config.routeConfigPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await fillFirstVisible(
    page.locator('input[placeholder="请输入工艺路线编码"], input[placeholder="请输入工艺路线名称"]'),
    config.routeQuery,
    '批处理路线配置页缺少路线查询输入框。'
  )
  await clickVisibleButton(page, /^搜索$/, '批处理路线配置页缺少搜索按钮。')
  await page.waitForTimeout(1000)
  await clickVisibleButton(page, /配置|批处理用途配置/, '批处理路线配置页缺少配置入口。')
  await page.getByText(/批处理用途|批记录用途|工艺流程批记录配置|记录类型/).first().waitFor({ state: 'visible', timeout: 30000 })

  const reportRows = page.locator('.route-flow-config-panel-report-list__row, .route/flow-config__report-row')
  await reportRows.first().waitFor({ state: 'visible', timeout: 30000 }).catch(async () => {
    await clickVisibleButton(page, /添加批记录表格/, '批处理用途配置弹窗缺少添加批记录表格按钮。')
    await reportRows.first().waitFor({ state: 'visible', timeout: 30000 })
  })
  const reportRow = await firstVisible(
    reportRows,
    '批处理用途配置弹窗未出现记录表绑定行。'
  )

  const directReportIdInput = reportRow.locator('input[placeholder="请输入批记录报表ID"]').first()
  if ((await directReportIdInput.count()) > 0 && (await directReportIdInput.isVisible())) {
    await directReportIdInput.fill('')
    await directReportIdInput.fill(config.internalReportId)
  }

  await selectElementOptionIfNeeded(page, reportRow.locator('.route-flow-config-panel-report-meta'), '内部记录表', '无法选择内部记录表类型')
  await page.waitForTimeout(300)
  await selectElementOptionIfNeeded(page, reportRow.locator('.route-flow-config-panel-report-profile'), '内部追溯', '无法选择内部追溯校验策略')
  await fillFirstVisible(reportRow.locator('.route-flow-config-panel-report-scope input'), config.permissionScopeId, '缺少权限范围ID输入框。')

  const saveResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/route/flow-config/save') && response.request().method() === 'POST',
      { timeout: 60000 }
  )
  await clickVisibleButton(page, /保存用途配置|^保存$/, '批处理用途配置弹窗缺少保存按钮。')
  const saveWinner = await Promise.race([
    saveResponsePromise.then((response) => ({ type: 'response', response })),
    page
      .locator('.route-flow-config-panel-alert:visible')
      .last()
      .waitFor({ state: 'visible', timeout: 10000 })
      .then(async () => ({
        type: 'error',
        message: ((await page.locator('.route-flow-config-panel-alert:visible').last().textContent()) || '').trim()
      }))
  ])
  if (saveWinner.type === 'error') {
    saveResponsePromise.catch(() => {})
    throw new Error(`路线用途配置保存未发起或未成功：${saveWinner.message}`)
  }
  await parseJsonResponse(saveWinner.response, '路线用途配置保存')
  steps.push({ name: 'configure internal record', outcome: 'PASS' })
}

async function openBatchExecution(page, config, steps) {
  await page.goto(`${config.baseUrl}${ROUTES.batchExecution}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText(/批次执行|批次执行编码|打开\/创建/).first().waitFor({ state: 'visible', timeout: 60000 })

  await clickVisibleButton(page, /打开\/创建|打开创建/, '批次执行页缺少打开/创建按钮。')
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await selectRemoteWorkOrder(page, dialog, config)
  await fillFirstVisible(
    dialog.locator('.el-form-item').filter({ hasText: '批次号' }).locator('input'),
    config.batchCode,
    '打开/创建弹窗缺少批次号输入框。'
  )
  if (config.routeId) {
    await fillFirstVisible(
      dialog.locator('.el-form-item').filter({ hasText: '路线ID' }).locator('input'),
      config.routeId,
      '打开/创建弹窗缺少路线ID输入框。'
    )
  }
  const openResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-batch-execution/open-or-create') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
  await clickVisibleButton(dialog, /确\s*认|确定/, '打开/创建弹窗缺少确认按钮。')
  const openWinner = await Promise.race([
    openResponsePromise.then((response) => ({ type: 'response', response })),
    dialog
      .locator('.edhr-batch-page__dialog-alert:visible')
      .last()
      .waitFor({ state: 'visible', timeout: 10000 })
      .then(async () => ({
        type: 'error',
        message: ((await dialog.locator('.edhr-batch-page__dialog-alert:visible').last().textContent()) || '').trim()
      }))
  ])
  if (openWinner.type === 'error') {
    openResponsePromise.catch(() => {})
    throw new Error(`打开/创建批次执行未发起或未成功：${openWinner.message}`)
  }
  const openData = await parseJsonResponse(openWinner.response, '打开/创建批次执行')
  assert.ok(openData && Number(openData.id || openData.batchExecutionId), '打开/创建批次执行必须返回真实批次执行 ID。')
  await page.waitForURL((url) => url.pathname === `${ROUTES.batchExecution}/detail`, { timeout: 60000 })
  await page.getByText(config.batchCode).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('内部记录表').first().waitFor({ state: 'visible', timeout: 60000 })
  steps.push({
    name: 'open batch execution',
    outcome: 'PASS',
    batchExecutionId: String(openData.id || openData.batchExecutionId)
  })
  return openData
}

async function openInternalTask(page, steps) {
  const [taskOpenResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-batch-execution/task/open') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    clickVisibleButton(page, /^打开填写$/, '批次详情页缺少打开填写按钮。')
  ])
  const taskData = await parseJsonResponse(taskOpenResponse, '打开内部记录表任务')
  await page.waitForURL((url) => url.pathname === '/mes/pro/feedback/edhr-execution/form', { timeout: 60000 })
  await page.getByText('记录类型').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('内部记录表').first().waitFor({ state: 'visible', timeout: 60000 })
  assert.ok(Number(taskData?.executionId || taskData?.id), '打开任务必须返回真实 eDHR 执行 ID。')
  steps.push({
    name: 'open internal task',
    outcome: 'PASS',
    executionId: String(taskData.executionId || taskData.id)
  })
  return taskData
}

async function skipIncomingSpecialNode(page, steps) {
  const specialRow = page.locator('.el-table__row').filter({ hasText: '来料检报告' }).first()
  await specialRow.waitFor({ state: 'visible', timeout: 60000 })
  const rowText = ((await specialRow.textContent()) || '').trim()
  if (rowText.includes('已跳过') || rowText.includes('已完成')) {
    steps.push({ name: 'skip incoming special node', outcome: 'PASS', alreadyResolved: true })
    return
  }
  const skipResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/edhr-batch-execution/task/special-node/skip') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickVisibleButton(specialRow, /^跳过$/, '来料检报告特殊节点缺少跳过按钮。')
  const confirmBox = page.locator('.el-message-box:visible').last()
  await confirmBox.waitFor({ state: 'visible', timeout: 30000 })
  await clickVisibleButton(confirmBox, /确\s*定|确认/, '跳过来料检报告确认框缺少确认按钮。')
  await parseJsonResponse(await skipResponsePromise, '跳过来料检报告特殊节点')
  steps.push({ name: 'skip incoming special node', outcome: 'PASS' })
}

async function chooseFirstReasonCategory(page) {
  const reasonForm = page.locator('.edhr-page-shell__field-audit-reason').first()
  const select = await firstVisible(reasonForm.locator('.el-select'), '字段审计原因分类选择器不存在。')
  await select.click()
  const option = page.locator('.el-select-dropdown__item:visible').first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function saveFieldChangeWithSelectedTime(page, config, steps) {
  const fieldItem = page.locator('.edhr-page-shell__form .el-form-item').filter({ hasText: config.fieldLabel }).first()
  await fieldItem.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(
    fieldItem.locator('input:not([disabled]), textarea:not([disabled])'),
    config.outOfRangeValue,
    `字段 ${config.fieldLabel} 缺少可编辑输入框。`
  )
  await chooseFirstReasonCategory(page)
  await fillFirstVisible(
    page.locator('input[placeholder="请输入字段变更原因"]'),
    'P8 内部记录表真实 E2E 字段变更',
    '字段审计原因说明输入框不存在。'
  )

  await clickVisibleButton(page, /^保存变更$/, '执行页缺少保存变更按钮。')
  const signDialog = page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).first()
  await signDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(signDialog.locator('input[type="password"]'), config.signaturePassword, '字段变更签名弹窗缺少签名密码输入框。')
  await fillDateTimePickerInput(
    signDialog.locator('input[placeholder="可选择人工签名时间"]'),
    config.selectedSignedAt,
    '字段变更签名弹窗缺少签名时间输入框。'
  )
  await fillFirstVisible(signDialog.locator('input[placeholder="例如 Asia/Shanghai"]'), config.selectedTimeZone, '字段变更签名弹窗缺少签名时区输入框。')
  await fillFirstVisible(signDialog.locator('textarea[placeholder="选择人工签名时间时必须说明原因"]'), config.selectedTimeReason, '字段变更签名弹窗缺少时间原因输入框。')

  const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/batch-record-execution/field-audit/save-changes') &&
        response.request().method() === 'PUT',
      { timeout: 60000 }
    )
  await clickVisibleButton(signDialog, /确\s*认\s*保\s*存/, '字段变更签名弹窗缺少确认保存按钮。')
  const saveWinner = await Promise.race([
    saveResponsePromise.then((response) => ({ type: 'response', response })),
    signDialog
      .locator('.edhr-page-shell__archive-alert:visible')
      .last()
      .waitFor({ state: 'visible', timeout: 15000 })
      .then(async () => ({
        type: 'error',
        message: ((await signDialog.locator('.edhr-page-shell__archive-alert:visible').last().textContent()) || '').trim()
      }))
  ])
  if (saveWinner.type === 'error') {
    saveResponsePromise.catch(() => {})
    throw new Error(`字段变更签名保存未发起或未成功：${saveWinner.message}`)
  }
  const saveData = await parseJsonResponse(saveWinner.response, '字段变更签名保存')
  const expectedSelectedSignedAt = normalizeSelectedSignedAtForApi(config.selectedSignedAt)
  const saveRequestBody = saveWinner.response.request().postData() || ''
  assert.ok(
    saveRequestBody.includes(expectedSelectedSignedAt),
    `字段变更保存原始请求体必须携带用户实际选择的签名时间：${expectedSelectedSignedAt}`
  )
  const savePayload = saveWinner.response.request().postDataJSON()
  assert.equal(
    savePayload?.signature?.signatureTime?.selectedSignedAt,
    expectedSelectedSignedAt,
    '字段变更保存请求必须携带用户实际选择的签名时间。'
  )
  assert.ok(Number(saveData?.signatureId), '字段变更保存必须返回真实 signatureId。')
  assert.ok(saveData?.auditBatchId, '字段变更保存必须返回 auditBatchId。')
  await page.getByText(/字段变更已写入|字段审计批次/).first().waitFor({ state: 'visible', timeout: 60000 })
  steps.push({
    name: 'save field change with selected signature time',
    outcome: 'PASS',
    signatureId: String(saveData.signatureId),
    auditBatchId: String(saveData.auditBatchId),
    selectedSignedAt: expectedSelectedSignedAt
  })
  return saveData
}

async function fillPermissionRuleRow(page, rowIndex, userId, ability, decisionText) {
  const rows = page.locator('.edhr-permission-matrix__rule-table .el-table__body-wrapper .el-table__row')
  const row = rows.nth(rowIndex)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(row.locator('.el-input-number input').first(), String(userId), `第 ${rowIndex + 1} 条规则缺少主体ID输入框。`)
  await setSelectByVisibleText(page, row.locator('.el-select').nth(1), ABILITY_LABELS[ability])
  await setSelectByVisibleText(page, row.locator('.el-select').nth(2), decisionText)
}

async function ensureExecutionAuditPermission(page, config, executionId, steps) {
  await page.goto(`${config.baseUrl}${ROUTES.permissionMatrix}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('保存规则').first().waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(
    formItem(page, '.edhr-permission-matrix__toolbar', '范围名称').locator('input'),
    `P8-执行审计-${executionId}-${Date.now()}`,
    '权限矩阵页缺少范围名称输入框。'
  )
  await fillFirstVisible(
    formItem(page, '.edhr-permission-matrix__toolbar', '对象类型').locator('input'),
    'BATCH_RECORD_EXECUTION',
    '权限矩阵页缺少对象类型输入框。'
  )
  await fillFirstVisible(
    formItem(page, '.edhr-permission-matrix__toolbar', '对象ID').locator('input'),
    String(executionId),
    '权限矩阵页缺少对象ID输入框。'
  )
  await clickVisibleButton(page, /添加规则/, '权限矩阵页缺少添加规则按钮。')
  await fillPermissionRuleRow(page, 0, config.writerUserId, 'AUDIT_VIEW', '允许')
  await fillPermissionRuleRow(page, 1, config.deniedUserId, 'AUDIT_VIEW', '拒绝')

  const saveResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/edhr-permission-scopes/save') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /^保存规则$/, '权限矩阵页缺少保存规则按钮。')
  const saveData = await parseJsonResponse(await saveResponse, '执行对象审计权限保存')
  assert.ok(Number(saveData?.scopeId), '执行对象审计权限保存必须返回真实 scopeId。')
  assert.equal(saveData.objectType, 'BATCH_RECORD_EXECUTION', '执行对象审计权限保存必须回显对象类型。')
  assert.equal(String(saveData.objectId), String(executionId), '执行对象审计权限保存必须回显对象ID。')
  assert.ok((saveData.rules || []).length >= 2, '执行对象审计权限保存必须返回允许和拒绝规则。')
  steps.push({
    name: 'ensure execution audit permission',
    outcome: 'PASS',
    scopeId: String(saveData.scopeId)
  })
  return saveData
}

async function verifyOperationAudit(page, config, executionId, steps) {
  const url = new URL(`${config.baseUrl}${ROUTES.operationAudit}`)
  url.searchParams.set('objectType', 'BATCH_RECORD_EXECUTION')
  url.searchParams.set('objectId', String(executionId))
  url.searchParams.set('executionId', String(executionId))
  url.searchParams.set('recordCategory', 'INTERNAL_RECORD')
  const [auditResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-operation-audit/page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    ),
    page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  ])
  const auditData = await parseJsonResponse(auditResponse, 'eDHR 操作审计查询')
  const rows = auditData?.list || []
  assert.ok(rows.length > 0, 'eDHR 操作审计查询必须返回真实事件。')
  assert.ok(
    rows.some((row) => row.resultStatus === 'SUCCESS' || row.operationType === 'FIELD_CHANGE'),
    '操作审计必须包含成功事件或字段变更事件。'
  )
  await page.getByText(/操作类型|权限决策|结果/).first().waitFor({ state: 'visible', timeout: 30000 })
  steps.push({ name: 'verify operation audit', outcome: 'PASS', auditRows: rows.length })
}

async function verifyPermissionMatrix(page, config, executionId, steps) {
  const url = new URL(`${config.baseUrl}${ROUTES.permissionMatrix}`)
  url.searchParams.set('objectType', 'BATCH_RECORD_EXECUTION')
  url.searchParams.set('objectId', String(executionId))
  url.searchParams.set('executionId', String(executionId))
  url.searchParams.set('recordCategory', 'INTERNAL_RECORD')
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('评估').first().waitFor({ state: 'visible', timeout: 60000 })
  const permissionResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/edhr-permission-scopes/evaluate') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /^评估$/, '权限矩阵页缺少评估按钮。')
  const permissionData = await parseJsonResponse(await permissionResponse, 'eDHR 对象权限评估')
  assert.ok(permissionData?.decisions && typeof permissionData.decisions === 'object', '对象权限评估必须返回 decisions。')
  assert.ok(permissionData.operationAuditEventId, '对象权限评估必须返回 operationAuditEventId。')
  await page.getByText(/后端决策|审计事件ID/).first().waitFor({ state: 'visible', timeout: 30000 })
  steps.push({
    name: 'verify permission matrix',
    outcome: 'PASS',
    operationAuditEventId: String(permissionData.operationAuditEventId)
  })
}

async function verifyDeniedUser(context, page, config, executionId, steps) {
  await resetSession(context, page)
  await login(
    page,
    { baseUrl: config.baseUrl, tenant: config.tenant },
    { role: 'denied', username: config.deniedUsername, password: config.deniedPassword }
  )
  await page.goto(`${config.baseUrl}/mes/pro/feedback/edhr-execution/detail?id=${executionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.waitForTimeout(1500)
  const bodyText = ((await page.locator('body').textContent({ timeout: 30000 })) || '').trim()
  assert.match(
    bodyText,
    /403|无权限|未授权|forbidden|no permission|404|页面不存在|对象权限|拒绝/i,
    `无权限账号未暴露对象级拒绝证据，页面文本片段：${bodyText.slice(0, 500)}`
  )
  steps.push({ name: 'verify denied user object permission', outcome: 'PASS' })
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai'
  })
  const page = await context.newPage()
  const steps = []
  try {
    await login(
      page,
      { baseUrl: config.baseUrl, tenant: config.tenant },
      { role: 'writer', username: config.username, password: config.password }
    )
    await configureInternalRecord(page, config, steps)
    await openBatchExecution(page, config, steps)
    await skipIncomingSpecialNode(page, steps)
    const taskData = await openInternalTask(page, steps)
    const executionId = Number(taskData.executionId || taskData.id)
    await saveFieldChangeWithSelectedTime(page, config, steps)
    await ensureExecutionAuditPermission(page, config, executionId, steps)
    await verifyOperationAudit(page, config, executionId, steps)
    await verifyPermissionMatrix(page, config, executionId, steps)
    await verifyDeniedUser(context, page, config, executionId, steps)
    await browser.close()
    return {
      status: 'PASS',
      executionId,
      generatedAt: new Date().toISOString(),
      steps
    }
  } catch (error) {
    await browser.close()
    throw Object.assign(error, { steps })
  }
}

function serializeError(error) {
  return {
    name: error?.name || 'Error',
    message: error?.message || String(error),
    stack: error?.stack
  }
}

function writeJsonResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 第 55-58 条真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- Generated at: ${new Date().toISOString()}`,
    `- Frontend cwd: \`${process.cwd()}\``,
    `- Required base URL: \`${REQUIRED_BASE_URL}\``,
    '- Required tenant: `测试租户`；写入账号必须为 `aoteman`。',
    '- Signature password: 仅从环境变量读取，不写入证据。',
    '- Command: `node tests/e2e/edhr-tail-four-goals-real-flow.e2e.js`',
    '- Result JSON: `test-results/edhr-tail-four-goals/result.json`',
    `- Status: ${result.status}`,
    '',
    '## BDD',
    '',
    ...BDD_SCENARIOS.map((scenario) => `- ${scenario}`),
    ''
  ]

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED', '')
    lines.push(`- BLOCKED: \`node tests/e2e/edhr-tail-four-goals-real-flow.e2e.js\` -> FAIL, ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- \`${item.name}\`: ${item.description}`)
    }
    lines.push('- Impact: 无法进入真实写入型 eDHR P8 验收；未使用 mock、默认成功、接口直写或切换租户替代。')
    lines.push('')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `node tests/e2e/edhr-tail-four-goals-real-flow.e2e.js` -> PASS, 第 55-58 条真实 UI 路径通过。')
    for (const step of result.steps || []) {
      lines.push(`- ${step.name} -> ${step.outcome}${step.executionId ? `, executionId=${step.executionId}` : ''}${step.signatureId ? `, signatureId=${step.signatureId}` : ''}${step.auditRows ? `, auditRows=${step.auditRows}` : ''}`)
    }
    lines.push('')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED', '')
    lines.push(`- RED: \`node tests/e2e/edhr-tail-four-goals-real-flow.e2e.js\` -> FAIL, ${result.error?.message || 'unknown error'}`)
    for (const step of result.steps || []) lines.push(`- Completed before failure: ${step.name} -> ${step.outcome}`)
    lines.push('- Impact: P8 真实验收未放行。')
    lines.push('')
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实 E2E 前置条件缺失或命中受保护范围。',
      missing: config.missing,
      generatedAt: new Date().toISOString()
    }
    writeJsonResult(result)
    writeEvidence(result)
    console.error(result.reason)
    process.exitCode = 1
    return
  }

  try {
    const result = await runRealFlow(config)
    writeJsonResult(result)
    writeEvidence(result)
    console.log('PASS: eDHR tail four goals real UI flow')
  } catch (error) {
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.blocked ? error.message : undefined,
      error: serializeError(error),
      steps: error.steps || [],
      generatedAt: new Date().toISOString()
    }
    writeJsonResult(result)
    writeEvidence(result)
    if (error.blocked) {
      console.error(error.message)
      process.exitCode = 1
      return
    }
    throw error
  }
}

main()
