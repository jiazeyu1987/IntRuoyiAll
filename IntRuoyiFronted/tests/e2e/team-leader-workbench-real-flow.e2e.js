const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260731-team-leader-workbench-prd-plan'
const DATA_PREFIX = 'TLW-20260731-'
const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const FRONTEND_ROOT = path.resolve(WORKSPACE_ROOT, 'IntRuoyiFronted')
const RESULT_DIR = path.resolve(FRONTEND_ROOT, 'test-results', 'team-leader-workbench-real-flow')
const EVIDENCE_FILE = path.resolve(WORKSPACE_ROOT, 'doc', 'tasks', TASK_ID, 'p6-real-e2e-evidence.md')

const TEAM_LEADER_ROUTE = '/mes/pro/process-pool/team-leader'
const PRODUCTION_FILL_ROUTE = '/mes/pro/feedback/edhr-batch-production-fill'
const FORBIDDEN_TENANTS = ['芋道源码', 'yudao', 'prod', 'production']

const REQUIRED_ENV = [
  ['TLW_FRONTEND_URL', '真实前端入口，例如 http://127.0.0.1:8084 或 http://127.0.0.1:8081。'],
  ['TLW_BACKEND_URL', '真实后端入口，例如 http://127.0.0.1:48084 或 http://127.0.0.1:48081。'],
  ['TLW_TENANT', '可写测试租户，禁止使用生产或 admin 基线租户。'],
  ['TLW_USERNAME', '拥有生产组长页签和员工填报路径权限的测试账号。'],
  ['TLW_PASSWORD', '测试账号密码，只能通过进程环境注入。'],
  ['TLW_WORK_ORDER_ID', '任务自有生产订单 ID。'],
  ['TLW_WORK_ORDER_CODE', '任务自有生产订单编码。'],
  ['TLW_TASK_ID', '任务自有生产任务 ID。'],
  ['TLW_ROUTE_ID', '正式工艺路线 ID。'],
  ['TLW_ROUTE_PROCESS_ID', '正式路线工序 ID。'],
  ['TLW_PROCESS_ID', '正式工序 ID。'],
  ['TLW_ITEM_ID', '生产订单对应产品物料 ID。'],
  ['TLW_EMPLOYEE_PROFILE_ID', '组长配置的员工档案 ID，可为临时工档案。'],
  ['TLW_DEVICE_ID', '组长配置的设备 ID。'],
  ['TLW_RECORDBOOK_ID', '正式记录本 ID。'],
  ['TLW_SIGNATURE_ID', '真实电子签名 ID。'],
  ['TLW_SIGNATURE_EMPLOYEE_ID', '签名员工 ID，必须等于实际填报员工。'],
  ['TLW_APPROVE_USER_ID', '生产组长审批人 ID。'],
  ['TLW_FEEDBACK_CODE', '本次一线报工单号，建议带 TLW-20260731- 前缀。'],
  ['TLW_FEEDBACK_TYPE', '正式报工类型。']
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function numberEnv(key) {
  const value = Number(envValue(key))
  return Number.isFinite(value) && value > 0 ? value : undefined
}

function sanitizeUrl(value) {
  return value ? value.replace(/\/+$/, '') : value
}

function collectConfig() {
  const config = {
    frontendUrl: sanitizeUrl(envValue('TLW_FRONTEND_URL')),
    backendUrl: sanitizeUrl(envValue('TLW_BACKEND_URL')),
    tenant: envValue('TLW_TENANT'),
    username: envValue('TLW_USERNAME'),
    password: envValue('TLW_PASSWORD'),
    dataPrefix: envValue('TLW_DATA_PREFIX') || DATA_PREFIX,
    workOrderId: numberEnv('TLW_WORK_ORDER_ID'),
    workOrderCode: envValue('TLW_WORK_ORDER_CODE'),
    taskId: numberEnv('TLW_TASK_ID'),
    routeId: numberEnv('TLW_ROUTE_ID'),
    routeProcessId: numberEnv('TLW_ROUTE_PROCESS_ID'),
    processId: numberEnv('TLW_PROCESS_ID'),
    itemId: numberEnv('TLW_ITEM_ID'),
    employeeProfileId: numberEnv('TLW_EMPLOYEE_PROFILE_ID'),
    deviceId: numberEnv('TLW_DEVICE_ID'),
    recordbookId: numberEnv('TLW_RECORDBOOK_ID'),
    signatureId: numberEnv('TLW_SIGNATURE_ID'),
    signatureEmployeeId: numberEnv('TLW_SIGNATURE_EMPLOYEE_ID'),
    approveUserId: numberEnv('TLW_APPROVE_USER_ID'),
    feedbackCode: envValue('TLW_FEEDBACK_CODE'),
    feedbackType: numberEnv('TLW_FEEDBACK_TYPE'),
    submitDate: envValue('TLW_SUBMIT_DATE'),
    outputQuantity: numberEnv('TLW_OUTPUT_QUANTITY') || 80,
    previousProcessInputQuantity: numberEnv('TLW_PREVIOUS_PROCESS_INPUT_QUANTITY') || 120,
    parameterLowerLimit: numberEnv('TLW_PARAMETER_LOWER_LIMIT') || 10,
    parameterUpperLimit: numberEnv('TLW_PARAMETER_UPPER_LIMIT') || 20,
    parameterDefaultValue: numberEnv('TLW_PARAMETER_DEFAULT_VALUE') || 15,
    verifyAllocationPath: envValue('TLW_VERIFY_ALLOCATION_PATH'),
    verifyOrderProcessPath: envValue('TLW_VERIFY_ORDER_PROCESS_PATH'),
    verifyBatchRecordPath: envValue('TLW_VERIFY_BATCH_RECORD_PATH'),
    headed: envValue('TLW_HEADED') === '1'
  }

  return {
    ...config,
    missing: collectMissingConfig(config)
  }
}

function collectMissingConfig(config) {
  const missing = []
  for (const [key, description] of REQUIRED_ENV) {
    const value = envValue(key)
    if (!value) {
      missing.push({ key, description })
    }
  }
  for (const key of [
    'TLW_WORK_ORDER_ID',
    'TLW_TASK_ID',
    'TLW_ROUTE_ID',
    'TLW_ROUTE_PROCESS_ID',
    'TLW_PROCESS_ID',
    'TLW_ITEM_ID',
    'TLW_EMPLOYEE_PROFILE_ID',
    'TLW_DEVICE_ID',
    'TLW_RECORDBOOK_ID',
    'TLW_SIGNATURE_ID',
    'TLW_SIGNATURE_EMPLOYEE_ID',
    'TLW_APPROVE_USER_ID',
    'TLW_FEEDBACK_TYPE'
  ]) {
    if (!numberEnv(key)) {
      missing.push({ key, description: '必须是大于 0 的真实数字 ID，不能使用占位值。' })
    }
  }
  if (!config.dataPrefix.startsWith(DATA_PREFIX)) {
    missing.push({
      key: 'TLW_DATA_PREFIX',
      description: `写入型数据必须使用 ${DATA_PREFIX} 前缀，便于清理和审计。`
    })
  }
  if (config.tenant && FORBIDDEN_TENANTS.some((tenant) => config.tenant.toLowerCase().includes(tenant))) {
    missing.push({
      key: 'TLW_TENANT',
      description: '命中禁止的生产或 admin 基线租户口径，不能执行写入型 E2E。'
    })
  }
  if (!isAllowedRuntimePair(config.frontendUrl, config.backendUrl)) {
    missing.push({
      key: 'TLW_FRONTEND_URL/TLW_BACKEND_URL',
      description: '前后端 URL 必须成对使用：8084/48084 用于当前 worktree，或 8081/48081 用于 int_main 融合后验证。'
    })
  }
  return missing
}

function isAllowedRuntimePair(frontendUrl, backendUrl) {
  const pairs = new Map([
    ['http://127.0.0.1:8084', 'http://127.0.0.1:48084'],
    ['http://localhost:8084', 'http://127.0.0.1:48084'],
    ['http://127.0.0.1:8081', 'http://127.0.0.1:48081'],
    ['http://localhost:8081', 'http://127.0.0.1:48081']
  ])
  return pairs.get(frontendUrl) === backendUrl
}

function formatLocalDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function resolveSubmissionQueryDate(config) {
  if (!config.submitDate) {
    return formatLocalDate(new Date())
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(config.submitDate)) {
    throw new Error('TLW_SUBMIT_DATE 必须使用 YYYY-MM-DD 格式。')
  }
  return config.submitDate
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error('缺少 Playwright runtime；请先在 IntRuoyiFronted 执行 pnpm install。')
    blocked.blocked = true
    throw blocked
  }
}

function resolveChromiumLaunchOptions(config) {
  const chromiumExecutablePath = envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
  const launchOptions = { headless: !config.headed }
  if (!chromiumExecutablePath) {
    return launchOptions
  }
  if (!fs.existsSync(chromiumExecutablePath)) {
    throw new Error(`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH 指向的浏览器不存在：${chromiumExecutablePath}`)
  }
  return { ...launchOptions, executablePath: chromiumExecutablePath }
}

function writeEvidence(result) {
  ensureDir(RESULT_DIR)
  ensureDir(path.dirname(EVIDENCE_FILE))
  fs.writeFileSync(
    path.join(RESULT_DIR, 'result.json'),
    `${JSON.stringify(redactResult(result), null, 2)}\n`,
    'utf8'
  )

  const lines = [
    '# P6 生产组长工作台真实 E2E 证据',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- Generated At: \`${new Date().toISOString()}\``,
    `- Status: \`${result.status}\``,
    `- Frontend: \`${result.frontendUrl || '--'}\``,
    `- Backend: \`${result.backendUrl || '--'}\``,
    `- Tenant: \`${result.tenant || '--'}\``,
    `- User: \`${result.username || '--'}\``,
    `- Data Prefix: \`${result.dataPrefix || DATA_PREFIX}\``,
    '',
    '## BDD',
    '',
    '- BDD: 生产组长配置驱动员工填报并完成订单工序 -> Given 测试租户有组长、员工、订单、工序、设备和正式批记录绑定 When 组长配置、员工填报、组长确认分配 Then 订单工序完成且批记录回填。',
    '- BDD: FIFO 自动分配且可手动调整 -> Given 员工提交完成数量且活跃订单有剩余 When 组长点击 FIFO 自动分配并必要时手动调整 Then 分配只能保存到活跃订单且总数等于提交数量。',
    ''
  ]

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED', '')
    lines.push(`- E2E: \`pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real\` -> BLOCKED, ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- Missing: \`${item.key}\` - ${item.description}`)
    }
    lines.push('- Impact: 未执行写入型真实 E2E；没有使用 mock、静态合同或 API-only 冒充成功。')
  } else if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real` -> PASS')
    for (const step of result.steps || []) {
      lines.push(`- Step: ${step}`)
    }
    lines.push(`- Screenshot: \`${result.screenshot || '--'}\``)
    lines.push('- Cleanup: PASS/BLOCKED 见 result.json；只清理 TLW-20260731- 任务自有数据。')
  } else {
    lines.push('## RED', '')
    lines.push(`- RED: \`pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real\` -> FAIL, ${result.error?.message || result.reason}`)
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function redactResult(result) {
  const copy = { ...result }
  delete copy.password
  return copy
}

async function assertHttpOk(url, label) {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`${label} 不可用：${url} -> HTTP ${response.status}`)
  }
  return response
}

async function login(page, config) {
  await page.goto(`${config.frontendUrl}/login?redirect=/index`, { waitUntil: 'networkidle' })
  await selectLoginTenant(page, config.tenant)
  await fillFirst(page, [
    'input[placeholder*="账号"]',
    'input[placeholder*="用户名"]',
    'input[name="username"]'
  ], config.username)
  await fillFirst(page, [
    'input[placeholder*="密码"]',
    'input[type="password"]',
    'input[name="password"]'
  ], config.password)
  await clickFirst(page, [
    'button:has-text("登录")',
    '.login-form button[type="submit"]'
  ])
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 20000 })
  await page.waitForLoadState('networkidle')
}

async function selectLoginTenant(page, tenantName) {
  const tenantSelectInput = page.locator('.el-select input:visible').first()
  await tenantSelectInput.waitFor({ state: 'visible', timeout: 15000 })
  await tenantSelectInput.click()
  await tenantSelectInput.fill(tenantName)
  const tenantOption = page.locator('.el-select-dropdown__item:visible', { hasText: tenantName }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: 15000 })
  await tenantOption.click()
  await page.getByText(tenantName, { exact: true }).first().waitFor({ state: 'visible', timeout: 15000 })
}

async function fillFirst(pageOrLocator, selectors, value) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(`${selector}:visible`).first()
    if (await locator.count()) {
      await locator.fill(String(value))
      return
    }
  }
  throw new Error(`找不到可填写控件：${selectors.join(', ')}`)
}

async function clickFirst(pageOrLocator, selectors) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(`${selector}:visible`).first()
    if (await locator.count()) {
      await locator.click()
      return
    }
  }
  throw new Error(`找不到可点击控件：${selectors.join(', ')}`)
}

async function fillFormItem(section, label, value) {
  const item = section.locator('.el-form-item', { hasText: label }).first()
  await fillFirst(item, ['input'], value)
}

function formForAction(section, actionText) {
  return section.getByRole('button', { name: actionText }).first()
    .locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " el-form ")][1]')
}

async function fillFormItemForAction(section, actionText, label, value) {
  await fillFormItem(formForAction(section, actionText), label, value)
}

async function selectFormItem(section, label, optionText) {
  const item = section.locator('.el-form-item', { hasText: label }).first()
  await clickFirst(item, ['.el-select__wrapper', '.el-select'])
  await section.page().locator('.el-select-dropdown__item:visible', { hasText: optionText }).last().click()
}

async function selectFormItemForAction(section, actionText, label, optionText) {
  await selectFormItem(formForAction(section, actionText), label, optionText)
}

async function clickButton(section, text) {
  await section.getByRole('button', { name: text }).click()
  await section.page().waitForLoadState('networkidle')
}

async function clickButtonAndWaitForSuccess(section, text, endpointFragment) {
  const page = section.page()
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes(endpointFragment)
      && ['POST', 'PUT', 'DELETE'].includes(response.request().method())
  , { timeout: 20000 })
  await section.getByRole('button', { name: text }).click()
  const response = await responsePromise
  assert.strictEqual(response.ok(), true, `${text} 写入接口 HTTP 失败：${response.status()}`)
  const body = await response.json()
  assert.strictEqual(body.code, 0, `${text} 写入接口业务失败：${body.msg || body.message || 'unknown'}`)
  await page.waitForLoadState('networkidle')
  return body.data
}

async function configureTeamLeaderPage(page, config, steps) {
  await page.goto(`${config.frontendUrl}${TEAM_LEADER_ROUTE}`, { waitUntil: 'networkidle' })
  await page.locator('[data-team-leader-report-workbench]').waitFor({ state: 'visible' })
  await page.locator('[data-team-leader-config-center]').waitFor({ state: 'visible' })
  steps.push('组长工作台和配置中心可见')

  const activeOrder = page.locator('[data-team-leader-active-order-config]').first()
  await fillFormItemForAction(activeOrder, '加入活跃订单', '生产订单ID', config.workOrderId)
  await clickButtonAndWaitForSuccess(activeOrder, '加入活跃订单', '/mes/pro/process-pool/team-leader/active-order/add')
  steps.push('生产组长通过 UI 加入活跃订单')

  const employee = page.locator('[data-team-leader-employee-config]').first()
  await fillFormItemForAction(employee, '绑定工序员工', '工序ID', config.processId)
  await fillFormItemForAction(employee, '绑定工序员工', '员工档案ID', config.employeeProfileId)
  await clickButtonAndWaitForSuccess(employee, '绑定工序员工',
    '/mes/pro/process-pool/team-leader/process-employee-binding/save')
  steps.push('生产组长通过 UI 绑定工序员工')

  const device = page.locator('[data-team-leader-device-config]').first()
  await fillFormItemForAction(device, '更新状态', '设备ID', config.deviceId)
  await selectFormItemForAction(device, '更新状态', '状态', '启用')
  await clickButtonAndWaitForSuccess(device, '更新状态',
    '/mes/pro/process-pool/team-leader/team-device/status/update')
  steps.push('生产组长通过 UI 恢复设备为启用')

  const relation = page.locator('[data-team-leader-process-relation-config]').first()
  await fillFormItemForAction(relation, '绑定工序设备', '工序ID', config.processId)
  await fillFormItemForAction(relation, '绑定工序设备', '设备ID', config.deviceId)
  await clickButtonAndWaitForSuccess(relation, '绑定工序设备',
    '/mes/pro/process-pool/team-leader/process-device-binding/save')
  await fillFormItemForAction(relation, '保存工序异常原因', '工序ID', config.processId)
  await selectFormItemForAction(relation, '保存工序异常原因', '原因类型', '损耗')
  await fillFormItemForAction(relation, '保存工序异常原因', '原因编码', `${config.dataPrefix}LOSS`)
  await fillFormItemForAction(relation, '保存工序异常原因', '原因名称', `${config.dataPrefix}正常损耗`)
  await clickButtonAndWaitForSuccess(relation, '保存工序异常原因',
    '/mes/pro/process-pool/team-leader/process-defect-reason/save')
  steps.push('生产组长通过 UI 维护工序设备和异常关系')

  const parameter = page.locator('[data-team-leader-parameter-config]').first()
  await fillFormItemForAction(parameter, '保存参数', '工序ID', config.processId)
  await fillFormItemForAction(parameter, '保存参数', '设备ID', config.deviceId)
  await fillFormItemForAction(parameter, '保存参数', '参数编码', `${config.dataPrefix}PRESSURE`)
  await fillFormItemForAction(parameter, '保存参数', '参数名称', '压力')
  await fillFormItemForAction(parameter, '保存参数', '单位', 'MPa')
  await fillFormItemForAction(parameter, '保存参数', '下限', config.parameterLowerLimit)
  await fillFormItemForAction(parameter, '保存参数', '上限', config.parameterUpperLimit)
  await fillFormItemForAction(parameter, '保存参数', '默认值', config.parameterDefaultValue)
  await clickButtonAndWaitForSuccess(parameter, '保存参数',
    '/mes/pro/process-pool/team-leader/runtime-device-parameter-rule/save')
  steps.push('生产组长通过 UI 维护设备参数上下限和默认值')
}

async function submitEmployeeReport(page, config, steps) {
  const query = new URLSearchParams({
    workOrderId: String(config.workOrderId),
    productionOrderCode: config.workOrderCode,
    routeId: String(config.routeId),
    routeProcessId: String(config.routeProcessId),
    processId: String(config.processId),
    taskId: String(config.taskId),
    itemId: String(config.itemId),
    feedbackCode: config.feedbackCode,
    feedbackType: String(config.feedbackType),
    approveUserId: String(config.approveUserId),
    recordbookId: String(config.recordbookId),
    signatureId: String(config.signatureId),
    signatureEmployeeId: String(config.signatureEmployeeId),
    previousProcessInputQuantity: String(config.previousProcessInputQuantity)
  })
  await page.goto(`${config.frontendUrl}${PRODUCTION_FILL_ROUTE}?${query.toString()}`, {
    waitUntil: 'networkidle'
  })
  await page.locator('[data-frontline-production-operator]').waitFor({ state: 'visible' })
  await fillFirst(page, ['#frontlineProductionOutputQuantity'], config.outputQuantity)
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/feedback/frontline/submit')
      && response.request().method() === 'POST'
  , { timeout: 20000 })
  await page.getByRole('button', { name: '提交' }).click()
  const response = await responsePromise
  assert.strictEqual(response.ok(), true, `员工正式报工提交接口 HTTP 失败：${response.status()}`)
  const body = await response.json()
  assert.strictEqual(body.code, 0, `员工正式报工提交接口业务失败：${body.msg || body.message || 'unknown'}`)
  await page.waitForLoadState('networkidle')
  steps.push('员工端通过正式 frontlineSubmit 提交报工和设备参数')
}

async function confirmTeamLeaderReport(page, config, steps) {
  await page.goto(`${config.frontendUrl}${TEAM_LEADER_ROUTE}`, { waitUntil: 'networkidle' })
  await fillFirst(page, ['input[placeholder="工单编码"]'], config.workOrderCode)
  await page.getByRole('button', { name: '搜索' }).click()
  await page.waitForLoadState('networkidle')
  await page.getByRole('button', { name: '复核' }).first().click()
  await page.locator('[data-team-leader-fifo-allocation]').waitFor({ state: 'visible' })
  await page.locator('[data-team-leader-fifo-allocation]').click()
  await page.locator('[data-team-leader-allocation-table] tbody tr').first().waitFor({
    state: 'visible',
    timeout: 15000
  })
  steps.push('生产组长通过 UI 生成 FIFO 自动分配行')
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/process-pool/team-leader/submission/allocation/confirm')
      && response.request().method() === 'POST'
  , { timeout: 20000 })
  await page.getByRole('button', { name: '提交复核' }).click()
  const response = await responsePromise
  assert.strictEqual(response.ok(), true, `确认报工分配接口 HTTP 失败：${response.status()}`)
  const body = await response.json()
  assert.strictEqual(body.code, 0, `确认报工分配接口业务失败：${body.msg || body.message || 'unknown'}`)
  await page.waitForLoadState('networkidle')
  steps.push('生产组长通过 UI 确认报工并分配到活跃订单')
}

async function readOnlyVerify(page, config, steps) {
  const token = await getAccessToken(page)

  const allocationBody = await fetchTrace(
    config,
    token,
    '分配记录',
    resolveVerifyPath(config.verifyAllocationPath, 'allocation', config)
  )
  assertTraceNumber(allocationBody.data, 'totalAllocatedQuantity', config.outputQuantity, '分配记录总数')
  assert.ok(Array.isArray(allocationBody.data.lines) && allocationBody.data.lines.length > 0,
    '分配记录只读核验必须返回至少一条订单分配行。')
  steps.push('分配记录只读 API 核验通过，且分配总数等于员工报工数量')

  const orderProcessBody = await fetchTrace(
    config,
    token,
    '订单工序完成',
    resolveVerifyPath(config.verifyOrderProcessPath, 'orderProcess', config)
  )
  assertTraceNumber(orderProcessBody.data, 'confirmedQuantity', config.outputQuantity, '订单工序累计确认数量')
  assertTraceValue(orderProcessBody.data, 'completionStatus', 'COMPLETED', '订单工序完成状态')
  assertTraceValue(orderProcessBody.data, 'backfillStatus', 'SUCCESS', '批记录回填状态')
  assertTracePresent(orderProcessBody.data, 'backfillExecutionId', '批记录回填执行实例')
  steps.push('订单工序完成只读 API 核验通过，且状态为 COMPLETED / SUCCESS')

  const batchRecordBody = await fetchTrace(
    config,
    token,
    '正式批记录回填',
    resolveVerifyPath(config.verifyBatchRecordPath, 'batchRecord', config)
  )
  assertBatchRecordBackfillTrace(batchRecordBody.data, config)
  steps.push('正式批记录回填只读 API 核验通过，且包含字段审计或单元格投影证据')
}

async function getAccessToken(page) {
  const rawToken = await page.evaluate(() => {
    const keys = Object.keys(localStorage)
    const tokenKey = keys.find((key) => key.toLowerCase().includes('token'))
    return tokenKey ? localStorage.getItem(tokenKey) : ''
  })
  const token = parseStoredAccessToken(rawToken)
  assert.ok(token, '登录后必须能读取本机前端保存的访问 token 用于只读核验。')
  return token
}

function parseStoredAccessToken(raw) {
  let token = String(raw || '').trim()
  for (let i = 0; i < 2; i += 1) {
    try {
      const parsed = JSON.parse(token)
      if (typeof parsed === 'string') {
        token = parsed
      } else if (parsed && typeof parsed === 'object') {
        token = parsed.v || parsed.value || parsed.accessToken || parsed.token || token
      }
    } catch {
      break
    }
  }
  return String(token || '').replace(/^Bearer\s+/i, '').trim()
}

async function discoverSubmittedEventId(page, config, steps) {
  const token = await getAccessToken(page)
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '20',
    leaderType: 'PRODUCTION',
    submitDate: resolveSubmissionQueryDate(config),
    workOrderCode: config.workOrderCode
  })
  const body = await fetchTrace(
    config,
    token,
    '员工提交事件发现',
    `/admin-api/mes/pro/process-pool/team-leader/submission/page?${params.toString()}`
  )
  const rows = Array.isArray(body.data?.list) ? body.data.list : []
  const matched = rows.find((row) =>
    Number(row.workOrderId) === Number(config.workOrderId) &&
    Number(row.routeProcessId) === Number(config.routeProcessId) &&
    Number(row.processId) === Number(config.processId)
  )
  assert.ok(matched?.id, '真实 E2E 必须能从组长提交分页只读发现刚提交的 eventId。')
  config.eventId = Number(matched.id)
  steps.push(`只读发现员工提交事件 eventId=${config.eventId}`)
  return config.eventId
}

function resolveVerifyPath(template, traceType, config) {
  const defaults = {
    allocation: '/admin-api/mes/pro/process-pool/team-leader/submission/allocation/trace?eventId=__EVENT_ID__&workOrderId=__WORK_ORDER_ID__&routeProcessId=__ROUTE_PROCESS_ID__&processId=__PROCESS_ID__',
    orderProcess: '/admin-api/mes/pro/process-pool/team-leader/order-process/trace?workOrderId=__WORK_ORDER_ID__&routeProcessId=__ROUTE_PROCESS_ID__&processId=__PROCESS_ID__',
    batchRecord: '/admin-api/mes/pro/process-pool/team-leader/batch-record/trace?workOrderId=__WORK_ORDER_ID__&routeProcessId=__ROUTE_PROCESS_ID__&processId=__PROCESS_ID__'
  }
  const source = template || defaults[traceType]
  assert.ok(source, `未知核验路径类型：${traceType}`)
  assert.ok(config.eventId || traceType !== 'allocation', '分配记录核验必须先动态发现 eventId。')
  return source
    .replaceAll('__EVENT_ID__', encodeURIComponent(String(config.eventId)))
    .replaceAll('{{eventId}}', encodeURIComponent(String(config.eventId)))
    .replaceAll('__WORK_ORDER_ID__', encodeURIComponent(String(config.workOrderId)))
    .replaceAll('{{workOrderId}}', encodeURIComponent(String(config.workOrderId)))
    .replaceAll('__ROUTE_PROCESS_ID__', encodeURIComponent(String(config.routeProcessId)))
    .replaceAll('{{routeProcessId}}', encodeURIComponent(String(config.routeProcessId)))
    .replaceAll('__PROCESS_ID__', encodeURIComponent(String(config.processId)))
    .replaceAll('{{processId}}', encodeURIComponent(String(config.processId)))
}

async function fetchTrace(config, token, name, verifyPath) {
  const response = await fetch(`${config.backendUrl}${verifyPath}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  assert.equal(response.ok, true, `${name}只读核验失败：HTTP ${response.status}`)
  const body = await response.json()
  assert.equal(body.code, 0, `${name}只读核验业务失败：${body.msg || body.message || 'unknown'}`)
  assert.ok(body.data, `${name}只读核验必须返回 data。`)
  return body
}

function assertTraceNumber(data, key, expected, label) {
  assertTracePresent(data, key, label)
  assert.equal(Number(data[key]), Number(expected), `${label}必须等于 ${expected}。`)
}

function assertTraceValue(data, key, expected, label) {
  assertTracePresent(data, key, label)
  assert.equal(data[key], expected, `${label}必须等于 ${expected}。`)
}

function assertTracePresent(data, key, label = key) {
  assert.ok(data && data[key] !== undefined && data[key] !== null && data[key] !== '',
    `${label}不能为空。`)
}

function assertBatchRecordBackfillTrace(data, config) {
  assertTracePresent(data, 'executionId', '批记录执行实例')
  assertTracePresent(data, 'batchRecordReportId', '正式批记录报表 ID')
  assertTracePresent(data, 'fieldAuditLastBatchId', '字段审计批次 ID')
  assert.ok(Array.isArray(data.cells) && data.cells.length > 0,
    '正式批记录回填只读核验必须返回至少一个字段审计单元格。')
  const valueDisplays = data.cells.map((cell) => String(cell.valueDisplay ?? cell.valueJson ?? ''))
  assert.ok(valueDisplays.includes(String(config.parameterDefaultValue)),
    `正式批记录回填单元格必须包含设备参数默认值 ${config.parameterDefaultValue}。`)
}

async function main() {
  const config = collectConfig()
  if (config.missing.length) {
    const result = {
      status: 'BLOCKED',
      reason: '缺少真实写入型 E2E 前置条件。',
      missing: config.missing,
      ...config
    }
    writeEvidence(result)
    process.exitCode = 2
    return
  }

  const steps = []
  let browser
  try {
    await assertHttpOk(`${config.frontendUrl}/`, '前端入口')
    await assertHttpOk(`${config.backendUrl}/actuator/health`, '后端健康检查')
    const { chromium } = loadPlaywright()
    browser = await chromium.launch(resolveChromiumLaunchOptions(config))
    const context = await browser.newContext({ ignoreHTTPSErrors: true })
    const page = await context.newPage()
    await login(page, config)
    steps.push('真实 UI 登录测试租户成功')
    await configureTeamLeaderPage(page, config, steps)
    await submitEmployeeReport(page, config, steps)
    await discoverSubmittedEventId(page, config, steps)
    await confirmTeamLeaderReport(page, config, steps)
    await readOnlyVerify(page, config, steps)
    ensureDir(RESULT_DIR)
    const screenshot = path.join(RESULT_DIR, 'team-leader-workbench-pass.png')
    await page.screenshot({ path: screenshot, fullPage: true })
    writeEvidence({
      status: 'PASS',
      reason: '真实 UI 闭环已通过。',
      steps,
      screenshot,
      ...config
    })
  } catch (error) {
    writeEvidence({
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.message || String(error),
      error: {
        name: error.name || 'Error',
        message: error.message || String(error),
        stack: error.stack
      },
      steps,
      ...config
    })
    process.exitCode = error.blocked ? 2 : 1
  } finally {
    if (browser) {
      await browser.close()
    }
  }
}

void main()
