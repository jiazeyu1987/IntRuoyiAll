const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260529-edhr-tracking-signature-real-e2e-gate'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-tracking-signature')
const DEFAULT_EVIDENCE_FILE = path.resolve(
  process.cwd(),
  'doc',
  'tasks',
  TASK_ID,
  'real-e2e-evidence.md'
)

const REQUIRED_BASE_URL = 'http://localhost:8081'
const DEFAULT_TENANT = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const DEFAULT_EXECUTION_ID = '40'
const DEFAULT_EXECUTION_CODE = 'BRE202605280518101280040'
const DEFAULT_BATCH_CODE = 'EDHR-BATCH-122-E2E-APPROVE-GATE05280525'

const TRACKING_ROUTE = '/mes/pro/feedback/edhr-tracking'
const SIGNATURE_ROUTE = '/mes/pro/feedback/edhr-signatures'
const EXECUTION_DETAIL_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const TRACKING_PAGE_ENDPOINT = '/mes/pro/batch-record-execution/tracking-page'
const TRACKING_TIMELINE_ENDPOINT = '/mes/pro/batch-record-execution/tracking-timeline'
const SIGNATURE_PAGE_ENDPOINT = '/mes/pro/batch-record-execution/signature-page'
const SIGNATURE_PAGE_SOURCE = path.resolve(
  process.cwd(),
  'src',
  'views',
  'mes',
  'pro',
  'edhr',
  'SignaturePage.vue'
)
const FORBIDDEN_LIVE_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])
const TRACKING_EVENT_TYPES = ['SUBMIT', 'APPROVE', 'ARCHIVE_SEAL']
const SIGNATURE_ACTION_PRIORITY = ['ARCHIVE_SEAL', 'APPROVE', 'SUBMIT', 'REJECT', 'FIELD_CHANGE']
const SIGNATURE_ACTION_LABELS = {
  FIELD_CHANGE: '字段变更',
  FORM_REVIEW: '表单复核',
  SUBMIT: '提交审批',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  ARCHIVE_SEAL: '归档封存'
}

const EXECUTION_STATUS_LABELS = {
  0: '草稿',
  1: '待审批',
  2: '已驳回',
  3: '已关闭'
}

const BDD_SCENARIOS = [
  'BDD: 追踪页按真实执行编号筛选 -> Given 测试租户存在真实 eDHR 执行记录、追踪事件和动态菜单 `eDHR追踪` / When 用户登录并打开 `/mes/pro/feedback/edhr-tracking?executionCode=<real-code>` / Then 前端请求真实 `/mes/pro/batch-record-execution/tracking-page`，页面展示执行编号、工单号、批次号、当前状态、最后事件、意见/原因、最后处理时间和归档状态。',
  'BDD: 追踪页进入真实执行详情 -> Given 追踪页列表展示目标执行记录 / When 用户点击该行执行编号 / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>`，详情页请求真实 `/tracking-timeline`，展示同一执行编号与提交、审批或归档时间线证据。',
  'BDD: 签名页按真实执行筛选 -> Given 测试租户存在真实 eDHR 电子签名记录和动态菜单 `eDHR签名记录` / When 用户打开 `/mes/pro/feedback/edhr-signatures?executionId=<real-id>` / Then 前端请求真实 `/mes/pro/batch-record-execution/signature-page`，页面展示签名编号、执行编号、动作、签名含义、签名人、签名方式、密码校验、流程任务、签名时间和意见/原因。',
  'BDD: 签名页动作筛选真实有效 -> Given 目标执行记录存在 SUBMIT、APPROVE 或 ARCHIVE_SEAL 等真实签名动作 / When 用户在动作筛选中选择真实动作并查询 / Then API 查询参数包含对应 `actionType`，且目标响应 rows 全部匹配该动作。',
  'BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实执行记录、追踪事件、签名记录、菜单权限或前端入口 / When 执行 E2E / Then 脚本写入 `BLOCKED/FAIL` 证据并退出非零，不使用模拟数据、API-only 或降级路径。'
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function assertSourceIncludes(source, expected, message) {
  assert.ok(source.includes(expected), `${message}。缺少源码片段：${expected}`)
}

function assertSourceNotIncludes(source, unexpected, message) {
  assert.ok(!source.includes(unexpected), `${message}。不应继续包含源码片段：${unexpected}`)
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function runStaticDisplayFormatCheck() {
  const source = fs.readFileSync(SIGNATURE_PAGE_SOURCE, 'utf8')
  assertSourceIncludes(
    source,
    'SIGNATURE_ACTION_LABELS',
    '签名记录页必须维护动作中文映射'
  )
  for (const [action, label] of Object.entries(SIGNATURE_ACTION_LABELS)) {
    assertSourceIncludes(source, action, `签名记录页动作映射缺少 ${action}`)
    assertSourceIncludes(source, label, `签名记录页动作映射缺少 ${label}`)
  }
  assertSourceIncludes(
    source,
    'formatSignatureAction',
    '签名记录页动作列必须通过格式化函数展示'
  )
  assertSourceIncludes(
    source,
    'formatSignatureSignedAt',
    '签名记录页签名时间列必须通过格式化函数展示'
  )
  assertSourceIncludes(
    source,
    'YYYY年M月D日',
    '签名记录页签名时间必须使用年月日格式'
  )
  assertSourceNotIncludes(
    source,
    '<el-table-column label="动作" prop="actionType"',
    '签名记录页动作列不能直出 actionType 编码'
  )
  assertSourceNotIncludes(
    source,
    '<el-table-column label="签名时间" prop="signedAt"',
    '签名记录页签名时间列不能直出 signedAt 原始值'
  )
  console.log('PASS: eDHR signature display format static check')
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function resolveEvidenceFile() {
  return process.env.EDHR_TRACKING_SIGNATURE_EVIDENCE_FILE
    ? path.resolve(process.env.EDHR_TRACKING_SIGNATURE_EVIDENCE_FILE)
    : DEFAULT_EVIDENCE_FILE
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_TRACKING_SIGNATURE_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_TRACKING_SIGNATURE_TENANT') || DEFAULT_TENANT,
    username: envValue('EDHR_TRACKING_SIGNATURE_USERNAME') || DEFAULT_USERNAME,
    password: envValue('EDHR_TRACKING_SIGNATURE_PASSWORD'),
    executionId: envValue('EDHR_TRACKING_SIGNATURE_EXECUTION_ID') || DEFAULT_EXECUTION_ID,
    executionCode: envValue('EDHR_TRACKING_SIGNATURE_EXECUTION_CODE') || DEFAULT_EXECUTION_CODE,
    batchCode: envValue('EDHR_TRACKING_SIGNATURE_BATCH_CODE') || DEFAULT_BATCH_CODE,
    executablePath:
      envValue('EDHR_TRACKING_SIGNATURE_CHROME_EXECUTABLE') || envValue('PLAYWRIGHT_CHROME_EXECUTABLE'),
    headed: envValue('EDHR_TRACKING_SIGNATURE_HEADED') === '1',
    evidenceFile: resolveEvidenceFile()
  }

  const missing = collectInvalidConfig(config)
  if (missing.length > 0) {
    return {
      missing,
      evidenceFile: config.evidenceFile,
      invalidConfig: true,
      executionId: config.executionId,
      executionCode: config.executionCode,
      batchCode: config.batchCode
    }
  }

  return {
    ...config,
    missing: []
  }
}

function collectInvalidConfig(config) {
  const invalid = []

  if (config.baseUrl !== REQUIRED_BASE_URL) {
    invalid.push({
      key: 'EDHR_TRACKING_SIGNATURE_BASE_URL',
      description: `真实前端入口必须固定为 ${REQUIRED_BASE_URL}。`
    })
  }

  const normalizedTenant = String(config.tenant || '').toLowerCase()
  if (config.tenant !== DEFAULT_TENANT) {
    invalid.push({
      key: 'EDHR_TRACKING_SIGNATURE_TENANT',
      description: `真实 E2E 只能使用测试租户 ${DEFAULT_TENANT}。`
    })
  }
  if (FORBIDDEN_LIVE_TENANTS.has(normalizedTenant) || String(config.tenant || '').includes('芋道源码')) {
    invalid.push({
      key: 'EDHR_TRACKING_SIGNATURE_TENANT',
      description: '当前值命中 live 租户保护名单；真实 E2E 只能使用测试租户。'
    })
  }

  if (!config.username) {
    invalid.push({
      key: 'EDHR_TRACKING_SIGNATURE_USERNAME',
      description: '必须提供测试租户真实账号名。'
    })
  }

  if (!config.password) {
    invalid.push({
      key: 'EDHR_TRACKING_SIGNATURE_PASSWORD',
      description: '测试租户密码必须由当前进程环境或登录基线注入；不得写入脚本默认值或证据文件。'
    })
  }

  if (!/^\d+$/.test(String(config.executionId))) {
    invalid.push({
      key: 'EDHR_TRACKING_SIGNATURE_EXECUTION_ID',
      description: '必须是真实数字型执行记录 ID。'
    })
  }

  for (const [key, value] of [
    ['EDHR_TRACKING_SIGNATURE_EXECUTION_CODE', config.executionCode],
    ['EDHR_TRACKING_SIGNATURE_BATCH_CODE', config.batchCode]
  ]) {
    if (!String(value || '').trim()) {
      invalid.push({
        key,
        description: '必须提供真实业务可见值，不能为空。'
      })
    }
  }

  return invalid
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error(
      'Missing Playwright runtime. Run `pnpm install` in this workspace so package.json devDependency `playwright` is installed, then re-run `pnpm e2e:edhr:tracking-signature`.'
    )
    blocked.blocked = true
    throw blocked
  }
}

function serializeError(error) {
  if (!error) return undefined
  return {
    name: error.name || 'Error',
    message: error.message || String(error),
    stack: error.stack
  }
}

function writeJsonResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(
    path.join(RESULT_DIR, 'result.json'),
    `${JSON.stringify(result, null, 2)}\n`,
    'utf8'
  )
}

function writeEvidenceMarkdown(result, evidenceFile) {
  ensureDir(path.dirname(evidenceFile))
  const lines = [
    '# eDHR 追踪与签名真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 生成时间：${new Date().toISOString()}`,
    `- 前端 worktree：${process.cwd()}`,
    `- 固定前端入口：\`${REQUIRED_BASE_URL}\``,
    '- 固定测试租户：`测试租户`',
    '- 默认账号名：`aoteman`；密码由 `EDHR_TRACKING_SIGNATURE_PASSWORD` 注入，不写入仓库证据。',
    '- 真实 E2E 复跑命令：`pnpm e2e:edhr:tracking-signature`',
    '- 静态语法检查命令：`pnpm e2e:edhr:tracking-signature:check`',
    '- 证据文件：默认写入本任务目录 `doc/tasks/20260529-edhr-tracking-signature-real-e2e-gate/real-e2e-evidence.md`。',
    '- 临时产物目录：`test-results/edhr-tracking-signature/`（截图、trace、result.json 不提交）',
    `- 当前状态：${result.status}`,
    `- executionId：\`${result.executionId || DEFAULT_EXECUTION_ID}\``,
    `- executionCode：\`${result.executionCode || DEFAULT_EXECUTION_CODE}\``,
    `- batchCode：\`${result.batchCode || DEFAULT_BATCH_CODE}\``,
    ''
  ]

  lines.push('## BDD')
  lines.push('')
  for (const scenario of BDD_SCENARIOS) lines.push(`- ${scenario}`)
  lines.push('')

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED')
    lines.push('')
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:tracking-signature\` -> FAIL, ${result.reason}`)
    if (result.missing?.length) {
      lines.push(result.invalidConfig ? '- 不满足的真实 E2E 前置条件：' : '- 缺失前置：')
      for (const item of result.missing) {
        lines.push(`  - \`${item.key}\`：${item.description}`)
      }
    }
    lines.push('- 影响：无法通过真实页面登录测试租户、打开追踪页、进入详情时间线、打开签名页或验证动作筛选；未使用模拟数据、API-only 或降级路径。')
    lines.push('')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN')
    lines.push('')
    lines.push('- GREEN: `pnpm e2e:edhr:tracking-signature` -> PASS, 真实追踪筛选、详情时间线、签名查询和动作筛选已完成。')
    for (const step of result.steps || []) {
      lines.push(`- ${step.name} -> PASS${step.screenshot ? `, screenshot: \`${step.screenshot}\`` : ''}`)
      if (step.trackingRow) {
        lines.push(`  - tracking: executionCode=${step.trackingRow.executionCode}, workOrderId=${step.trackingRow.workOrderId}, workOrderCode=${step.trackingRow.workOrderCode}, batchId=${step.trackingRow.batchId || '--'}, batchCode=${step.trackingRow.batchCode}, currentNodeName=${step.trackingRow.currentNodeName || '--'}, currentAssigneeNames=${(step.trackingRow.currentAssigneeNames || []).join('、') || '--'}, status=${step.trackingRow.status}, lastEventType=${step.trackingRow.lastEventType}, lastEventAt=${step.trackingRow.lastEventAt}, archiveStatus=${step.trackingRow.archiveStatus || '--'}`)
      }
      if (step.timelineEvent) {
        lines.push(`  - timeline: eventType=${step.timelineEvent.eventType}, actionType=${step.timelineEvent.actionType || '--'}, actorName=${step.timelineEvent.actorName || '--'}, occurredAt=${step.timelineEvent.occurredAt}`)
      }
      if (step.signatureSummary) {
        lines.push(`  - signature: actionTypes=${step.signatureSummary.actionTypes.join(',')}, actors=${step.signatureSummary.actors.join(',')}, selectedAction=${step.signatureSummary.selectedAction || '--'}, rowCount=${step.signatureSummary.rowCount}`)
      }
    }
    lines.push(`- Trace: \`${result.trace}\``)
    lines.push('')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED')
    lines.push('')
    lines.push(`- RED: \`pnpm e2e:edhr:tracking-signature\` -> FAIL, ${result.error?.message || '未知错误'}`)
    lines.push('- 影响：真实 UI E2E 未放行；不得记录为通过。')
    lines.push('')
  }

  fs.writeFileSync(evidenceFile, `${lines.join('\n')}\n`, 'utf8')
}

async function screenshot(page, name, steps) {
  ensureDir(RESULT_DIR)
  const fileName = `${String(steps.length + 1).padStart(2, '0')}-${name}.png`
  const filePath = path.join(RESULT_DIR, fileName)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function firstVisible(locator, failureMessage) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(failureMessage)
}

async function clickVisibleButton(scope, namePattern, failureMessage) {
  const button = await firstVisible(scope.getByRole('button', { name: namePattern }), failureMessage)
  await button.click()
}

async function fillFirstVisible(locator, value, failureMessage) {
  const item = await firstVisible(locator, failureMessage)
  await item.fill(value)
}

async function waitForText(page, textOrPattern, failureMessage) {
  const locator = page.getByText(textOrPattern).first()
  try {
    await locator.waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    throw new Error(`${failureMessage}: ${error.message}`)
  }
}

async function assertTextContentContains(locator, expectedText, failureMessage) {
  const text = (await locator.textContent({ timeout: 30000 })) || ''
  assert.ok(
    text.includes(expectedText),
    `${failureMessage}。期望包含：${expectedText}；实际文本片段：${text.slice(0, 1000)}`
  )
}

async function assertTextContentNotContains(locator, unexpectedText, failureMessage) {
  const text = (await locator.textContent({ timeout: 30000 })) || ''
  assert.ok(
    !text.includes(unexpectedText),
    `${failureMessage}。不应包含：${unexpectedText}；实际文本片段：${text.slice(0, 1000)}`
  )
}

async function assertLocatorTextMatches(locator, expectedPattern, failureMessage) {
  const text = (await locator.textContent({ timeout: 30000 })) || ''
  assert.match(text, expectedPattern, `${failureMessage}。实际文本片段：${text.slice(0, 1000)}`)
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
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
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E；请在测试租户关闭验证码后重跑。')
  }

  const tenantInput = loginForm.locator('input.el-select__input:visible').first()
  if ((await tenantInput.count()) === 0) {
    throw new Error('登录页缺少可见租户选择输入框，无法确认正在登录测试租户。')
  }
  await tenantInput.click()
  await page.keyboard.press('Control+A')
  await page.keyboard.type(config.tenant)
  await page.keyboard.press('Enter')
  await page.waitForTimeout(400)

  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入用户名"]'),
    config.username,
    '登录页缺少用户名输入框。'
  )
  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入密码"]'),
    config.password,
    '登录页缺少密码输入框。'
  )

  await clickVisibleButton(loginForm, /^登录$/, '登录页缺少登录按钮。')
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })
}

function buildTrackingUrl(config) {
  const url = new URL(config.baseUrl)
  url.pathname = TRACKING_ROUTE
  url.searchParams.set('executionCode', config.executionCode)
  return url.toString()
}

function buildSignatureUrl(config) {
  const url = new URL(config.baseUrl)
  url.pathname = SIGNATURE_ROUTE
  url.searchParams.set('executionId', config.executionId)
  return url.toString()
}

function parseBusinessData(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 响应必须是对象。`)
  if (Object.prototype.hasOwnProperty.call(body, 'code')) {
    assert.ok(
      body.code === 0 || body.code === 200,
      `${label} 业务状态码应为 0 或 200，实际 ${body.code}：${body.msg || body.message || ''}`
    )
    assert.notEqual(body.data, undefined, `${label} CommonResult 缺少 data。`)
    return body.data
  }
  return body
}

async function parseJsonResponse(response, label) {
  assert.equal(response.status(), 200, `${label} HTTP 状态应为 200，实际 ${response.status()}，URL: ${response.url()}`)
  try {
    return parseBusinessData(await response.json(), label)
  } catch (error) {
    throw new Error(`${label} 响应不是 JSON：${error.message}`)
  }
}

function extractRows(data, label) {
  assert.ok(data && typeof data === 'object', `${label} data 必须是对象。`)
  assert.ok(Array.isArray(data.list), `${label} PageResult 缺少 list rows。`)
  return data.list
}

function responseMatches(response, endpoint, expectedParams) {
  if (response.request().method() !== 'GET') return false
  if (!response.url().includes(endpoint)) return false
  try {
    const url = new URL(response.url())
    return Object.entries(expectedParams).every(([key, value]) => {
      const actual = url.searchParams.get(key)
      return actual === String(value) || actual === encodeURIComponent(String(value))
    })
  } catch (error) {
    return false
  }
}

async function waitForApiResponse(page, endpoint, expectedParams, label) {
  try {
    const response = await page.waitForResponse(
      (candidate) => responseMatches(candidate, endpoint, expectedParams),
      { timeout: 60000 }
    )
    assert.equal(
      response.status(),
      200,
      `${label} API HTTP 状态应为 200，实际 ${response.status()}，URL: ${response.url()}`
    )
    return response
  } catch (error) {
    throw new Error(
      `${label} API 未在 60 秒内返回 HTTP 200：${endpoint} ${JSON.stringify(expectedParams)}；${
        error instanceof Error ? error.message : String(error)
      }`
    )
  }
}

function findTrackingRow(rows, config) {
  const row = rows.find((item) => {
    if (!item || typeof item !== 'object') return false
    return (
      String(item.executionId) === String(config.executionId) ||
      String(item.id) === String(config.executionId) ||
      String(item.executionCode) === String(config.executionCode)
    )
  })
  if (!row) {
    throw new Error(
      `追踪分页 rows 未包含目标执行记录：executionId=${config.executionId}, executionCode=${config.executionCode}, batchCode=${config.batchCode}。`
    )
  }
  return row
}

function assertTrackingRow(row, config) {
  assert.equal(String(row.executionId), String(config.executionId), '追踪目标行 executionId 必须等于目标 executionId。')
  assert.equal(String(row.executionCode), String(config.executionCode), '追踪目标行 executionCode 不一致。')
  assert.equal(String(row.batchCode), String(config.batchCode), '追踪目标行 batchCode 不一致。')
  assert.ok(row.workOrderId, '追踪目标行必须包含 workOrderId，工单号才能打开详情。')
  assert.ok(row.workOrderCode, '追踪目标行必须包含 workOrderCode。')
  assert.ok(Object.prototype.hasOwnProperty.call(row, 'batchId'), '追踪目标行必须返回 batchId 字段，缺少批次主数据时允许为空。')
  assert.ok(row.currentNodeName, '追踪目标行必须包含最后操作工序。')
  assert.ok(Array.isArray(row.currentAssigneeNames) && row.currentAssigneeNames.length > 0, '追踪目标行必须包含最后操作人。')
  assert.notEqual(row.status, undefined, '追踪目标行必须包含当前状态。')
  assert.ok(row.lastEventType, '追踪目标行必须包含最后事件。')
  assert.ok(Object.prototype.hasOwnProperty.call(row, 'lastEventReason'), '追踪目标行必须包含意见/原因字段。')
  assert.ok(row.lastEventAt, '追踪目标行必须包含最后处理时间。')
  assert.ok(Object.prototype.hasOwnProperty.call(row, 'archiveStatus'), '追踪目标行必须包含归档状态字段。')
}

function formatSignatureDateText(value) {
  assert.ok(value, '签名时间不能为空。')
  const date = /^\d+$/.test(String(value)) ? new Date(Number(value)) : new Date(value)
  assert.ok(!Number.isNaN(date.getTime()), `签名时间必须可解析：${value}`)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

function formatTrackingDateText(value) {
  assert.ok(value, '追踪最后处理时间不能为空。')
  const date = /^\d+$/.test(String(value)) ? new Date(Number(value)) : new Date(value)
  assert.ok(!Number.isNaN(date.getTime()), `追踪最后处理时间必须可解析：${value}`)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

async function assertTrackingPageEvidence(page, config, row) {
  await waitForText(page, config.executionCode, `追踪页未展示执行编号 ${config.executionCode}`)
  const table = page.locator('.edhr-query__table').first()
  await table.waitFor({ state: 'visible', timeout: 30000 })
  const trackingDateText = formatTrackingDateText(row.lastEventAt)
  const trackingEventText = SIGNATURE_ACTION_LABELS[row.lastEventType] || String(row.lastEventType)
  for (const token of [
    '执行编号',
    '生产上下文',
    '当前阶段',
    '最后处理',
    '归档状态',
    config.executionCode,
    config.batchCode,
    String(row.workOrderCode),
    String(row.currentNodeName),
    ...row.currentAssigneeNames.map(String),
    trackingEventText,
    trackingDateText
  ]) {
    await assertTextContentContains(table, token, `追踪页表格未展示 ${token}`)
  }
  await assertTextContentNotContains(table, String(row.lastEventAt), '追踪页表格不能直出最后处理时间原始值')
  await assertTextContentNotContains(table, String(row.lastEventType), '追踪页表格不能直出最后事件英文编码')
  const statusLabel = EXECUTION_STATUS_LABELS[row.status]
  if (statusLabel) await assertTextContentContains(table, statusLabel, `追踪页表格未展示状态 ${statusLabel}`)
  const reasonEvidence = row.lastEventReason ? String(row.lastEventReason) : '--'
  await assertTextContentContains(table, reasonEvidence, `追踪页表格未展示意见/原因 ${reasonEvidence}`)
  const archiveStatusLabel = row.archiveStatus
    ? {
        GENERATING: '生成中',
        SEALED: '已封存',
        FAILED: '生成失败'
      }[row.archiveStatus]
    : '未归档'
  await assertTextContentContains(table, archiveStatusLabel, `追踪页表格未展示归档状态 ${archiveStatusLabel}`)
  const targetRow = table
    .locator('.el-table__body .el-table__row')
    .filter({ hasText: config.executionCode })
    .first()
  await targetRow.locator('.el-table__expand-icon').first().click()
  for (const token of ['追踪证据', '执行记录', '工单编号', '批次编号', '流程实例']) {
    await assertTextContentContains(table, token, `追踪页展开证据未展示 ${token}`)
  }
}

async function openTrackingPage(page, config) {
  const responsePromise = waitForApiResponse(
    page,
    TRACKING_PAGE_ENDPOINT,
    { executionCode: config.executionCode },
    '追踪分页'
  )
  await page.goto(buildTrackingUrl(config), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await waitForText(page, /执行编号|eDHR/, '未进入 eDHR 追踪页')
  const response = await responsePromise
  const data = await parseJsonResponse(response, '追踪分页')
  const rows = extractRows(data, '追踪分页')
  const row = findTrackingRow(rows, config)
  assertTrackingRow(row, config)
  await assertTrackingPageEvidence(page, config, row)
  return row
}

function extractTimelineEvents(data, label) {
  if (Array.isArray(data)) return data
  if (data && Array.isArray(data.list)) return data.list
  throw new Error(`${label} 必须返回时间线数组。`)
}

function assertTimelineEvents(events, config) {
  const targetEvents = events.filter((item) => String(item.executionId) === String(config.executionId))
  assert.ok(targetEvents.length > 0, `tracking-timeline 未返回目标 executionId=${config.executionId} 的事件。`)
  const evidenceEvent = targetEvents.find((item) => TRACKING_EVENT_TYPES.includes(String(item.eventType || item.actionType)))
  assert.ok(
    evidenceEvent,
    `tracking-timeline 未返回 ${TRACKING_EVENT_TYPES.join('/')} 之一，实际事件：${targetEvents
      .map((item) => item.eventType || item.actionType || 'UNKNOWN')
      .join(',')}`
  )
  assert.ok(evidenceEvent.occurredAt, '时间线证据事件必须包含 occurredAt。')
  return evidenceEvent
}

async function openDetailFromTrackingRow(page, config) {
  const timelineResponsePromise = waitForApiResponse(
    page,
    TRACKING_TIMELINE_ENDPOINT,
    { executionId: config.executionId },
    '执行详情追踪时间线'
  )
  const targetRow = page
    .locator('.edhr-query__table .el-table__body .el-table__row')
    .filter({ hasText: config.executionCode })
    .first()
  await targetRow.waitFor({ state: 'visible', timeout: 30000 })
  const executionDetailLink = targetRow
    .locator('.edhr-tracking__execution-link')
    .filter({ hasText: config.executionCode })
    .first()
  await executionDetailLink.waitFor({ state: 'visible', timeout: 30000 })
  await executionDetailLink.click()
  await page.waitForURL(
    (url) =>
      url.pathname === EXECUTION_DETAIL_ROUTE &&
      url.searchParams.get('id') === String(config.executionId),
    { timeout: 60000 }
  )
  await waitForText(page, 'eDHR 执行详情', '未进入 eDHR 执行详情页')
  await waitForText(page, config.executionCode, `执行详情页未展示执行编号 ${config.executionCode}`)

  const timelineResponse = await timelineResponsePromise
  const timelineData = await parseJsonResponse(timelineResponse, '执行详情追踪时间线')
  const events = extractTimelineEvents(timelineData, '执行详情追踪时间线')
  const evidenceEvent = assertTimelineEvents(events, config)

  const trackingAudit = page.locator('.edhr-page-shell__tracking-audit').first()
  await trackingAudit.waitFor({ state: 'visible', timeout: 30000 })
  await assertLocatorTextMatches(trackingAudit, /事件[\s\S]*流程任务[\s\S]*处理人[\s\S]*处理时间/, '详情只读追踪区未展示时间线表格列')
  await assertTextContentContains(
    trackingAudit,
    evidenceEvent.eventType || evidenceEvent.actionType,
    '详情只读追踪区未展示目标时间线事件'
  )
  await assertTextContentContains(
    trackingAudit,
    String(evidenceEvent.occurredAt).slice(0, 10),
    '详情只读追踪区未展示目标时间线日期'
  )

  return evidenceEvent
}

function findSignatureRows(rows, config) {
  const targetRows = rows.filter((item) => {
    if (!item || typeof item !== 'object') return false
    return (
      String(item.executionId) === String(config.executionId) ||
      String(item.executionCode) === String(config.executionCode)
    )
  })
  if (targetRows.length === 0) {
    throw new Error(
      `签名分页 rows 未包含目标执行记录：executionId=${config.executionId}, executionCode=${config.executionCode}。`
    )
  }
  return targetRows
}

function assertSignatureRows(rows, config) {
  for (const row of rows) {
    assert.equal(String(row.executionId), String(config.executionId), '签名目标行 executionId 必须等于目标 executionId。')
    assert.equal(String(row.executionCode), String(config.executionCode), '签名目标行 executionCode 不一致。')
    assert.ok(row.actionType, '签名目标行必须包含 actionType。')
    assert.ok(row.meaningText, '签名目标行必须包含 meaningText。')
    assert.ok(row.actorName, '签名目标行必须包含 actorName。')
    assert.equal(row.signatureMode, 'PASSWORD', '签名目标行签名方式必须为 PASSWORD。')
    assert.equal(row.passwordVerified, true, '签名目标行 passwordVerified 必须为 true。')
    assert.ok(row.signedAt, '签名目标行必须包含 signedAt。')
  }
}

async function assertSignaturePageEvidence(page, config, rows) {
  await waitForText(page, config.executionCode, `签名页未展示执行编号 ${config.executionCode}`)
  const table = page.locator('.edhr-query__table').first()
  await table.waitFor({ state: 'visible', timeout: 30000 })
  for (const token of [
    '执行编号',
    '签名动作',
    '签名含义',
    '签名人',
    '签名确认',
    '签名时间',
    '意见/原因',
    config.executionCode,
    '密码签名',
    '通过'
  ]) {
    await assertTextContentContains(table, token, `签名页表格未展示 ${token}`)
  }

  await table.locator('.el-table__expand-icon').first().click()
  for (const token of ['签名时间证据', '签名方式', '密码校验', '流程任务', '流程实例', '时间审计哈希']) {
    await assertTextContentContains(table, token, `签名页展开证据未展示 ${token}`)
  }

  for (const row of rows) {
    const actionLabel = SIGNATURE_ACTION_LABELS[row.actionType] || row.actionType
    await assertTextContentContains(table, actionLabel, `签名页表格未展示动作 ${actionLabel}`)
    await assertTextContentContains(table, row.meaningText, `签名页表格未展示签名含义 ${row.meaningText}`)
    await assertTextContentContains(table, row.actorName, `签名页表格未展示签名人 ${row.actorName}`)
    const signedDateText = formatSignatureDateText(row.signedAt)
    await assertTextContentContains(table, signedDateText, `签名页表格未展示签名时间 ${signedDateText}`)
  }
}

function selectPreferredAction(rows) {
  return SIGNATURE_ACTION_PRIORITY.find((action) => rows.some((row) => row.actionType === action))
}

async function selectSignatureAction(page, selectedAction) {
  const toolbar = page.locator('.edhr-query__toolbar').first()
  const actionFormItem = toolbar.locator('.el-form-item').filter({ hasText: '动作' }).first()
  await actionFormItem.waitFor({ state: 'visible', timeout: 30000 })
  await actionFormItem.locator('.el-select').first().click()
  const selectedActionLabel = SIGNATURE_ACTION_LABELS[selectedAction]
  assert.ok(selectedActionLabel, `签名动作 ${selectedAction} 缺少中文标签。`)
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: new RegExp(`^\\s*${escapeRegExp(selectedActionLabel)}\\s*$`) })
    .last()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function openSignaturePage(page, config) {
  const responsePromise = waitForApiResponse(
    page,
    SIGNATURE_PAGE_ENDPOINT,
    { executionId: config.executionId },
    '签名分页'
  )
  await page.goto(buildSignatureUrl(config), { waitUntil: 'domcontentloaded', timeout: 60000 })
  const signatureTable = page.locator('.edhr-query__table').first()
  await signatureTable.waitFor({ state: 'visible', timeout: 30000 })
  await assertLocatorTextMatches(
    signatureTable,
    /执行编号[\s\S]*签名动作[\s\S]*签名含义[\s\S]*签名人[\s\S]*签名确认[\s\S]*签名时间/,
    '未进入 eDHR 签名记录页'
  )
  const response = await responsePromise
  const data = await parseJsonResponse(response, '签名分页')
  const rows = findSignatureRows(extractRows(data, '签名分页'), config)
  assertSignatureRows(rows, config)
  await assertSignaturePageEvidence(page, config, rows)
  return rows
}

async function querySignatureByAction(page, config, rows) {
  const selectedAction = selectPreferredAction(rows)
  assert.ok(
    selectedAction,
    `目标签名记录缺少 ${SIGNATURE_ACTION_PRIORITY.join('/')} 之一，无法验证动作筛选。`
  )

  const responsePromise = waitForApiResponse(
    page,
    SIGNATURE_PAGE_ENDPOINT,
    { executionId: config.executionId, actionType: selectedAction },
    '签名动作筛选'
  )
  await selectSignatureAction(page, selectedAction)
  await clickVisibleButton(page.locator('.edhr-query__toolbar').first(), /^查询$/, '签名页缺少查询按钮。')
  const response = await responsePromise
  const data = await parseJsonResponse(response, '签名动作筛选')
  const filteredRows = extractRows(data, '签名动作筛选')
  assert.ok(filteredRows.length > 0, `签名动作筛选 ${selectedAction} 未返回任何真实记录。`)
  assert.ok(
    filteredRows.every((row) => row.actionType === selectedAction),
    `签名动作筛选 rows 必须全部为 ${selectedAction}，实际：${filteredRows
      .map((row) => row.actionType || 'UNKNOWN')
      .join(',')}`
  )
  assert.ok(
    filteredRows.some((row) => String(row.executionId) === String(config.executionId)),
    `签名动作筛选 ${selectedAction} 未包含目标 executionId=${config.executionId}。`
  )
  await assertTextContentContains(
    page.locator('.edhr-query__table').first(),
    SIGNATURE_ACTION_LABELS[selectedAction] || selectedAction,
    `签名动作筛选后页面未展示动作 ${SIGNATURE_ACTION_LABELS[selectedAction] || selectedAction}`
  )
  return {
    selectedAction,
    filteredRows
  }
}

function summarizeSignatureRows(rows, selectedAction) {
  return {
    rowCount: rows.length,
    actionTypes: Array.from(new Set(rows.map((row) => row.actionType))).sort(),
    actors: Array.from(new Set(rows.map((row) => row.actorName))).sort(),
    selectedAction
  }
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({
    headless: !config.headed,
    ...(config.executablePath ? { executablePath: config.executablePath } : {})
  })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai'
  })
  const tracePath = path.join(RESULT_DIR, 'trace.zip')
  const page = await context.newPage()
  const steps = []

  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  try {
    await login(page, config)

    const trackingRow = await openTrackingPage(page, config)
    steps.push({
      name: '追踪页目标执行记录可见',
      screenshot: await screenshot(page, 'tracking-page', steps),
      trackingRow
    })

    const timelineEvent = await openDetailFromTrackingRow(page, config)
    steps.push({
      name: '追踪页查看进入详情时间线',
      screenshot: await screenshot(page, 'tracking-detail-timeline', steps),
      timelineEvent
    })

    const signatureRows = await openSignaturePage(page, config)
    steps.push({
      name: '签名页目标签名记录可见',
      screenshot: await screenshot(page, 'signature-page', steps),
      signatureSummary: summarizeSignatureRows(signatureRows)
    })

    const actionResult = await querySignatureByAction(page, config, signatureRows)
    steps.push({
      name: '签名页 actionType 筛选真实有效',
      screenshot: await screenshot(page, 'signature-action-filter', steps),
      signatureSummary: summarizeSignatureRows(actionResult.filteredRows, actionResult.selectedAction)
    })

    await context.tracing.stop({ path: tracePath })
    await browser.close()
    return {
      status: 'PASS',
      steps,
      trace: tracePath,
      resultFile: path.join(RESULT_DIR, 'result.json'),
      executionId: config.executionId,
      executionCode: config.executionCode,
      batchCode: config.batchCode
    }
  } catch (error) {
    try {
      await context.tracing.stop({ path: tracePath })
    } catch (traceError) {
      error.message = `${error.message}; trace 写入失败: ${
        traceError instanceof Error ? traceError.message : String(traceError)
      }`
    }
    await browser.close()
    throw Object.assign(error, {
      tracePath,
      steps,
      executionId: config.executionId,
      executionCode: config.executionCode,
      batchCode: config.batchCode
    })
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实 E2E 前置条件不满足，不能使用 live 租户、错误前端入口、无效真实数据或缺失测试租户密码。',
      missing: config.missing,
      invalidConfig: config.invalidConfig === true,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: config.evidenceFile,
      executionId: String(config.executionId || DEFAULT_EXECUTION_ID),
      executionCode: String(config.executionCode || DEFAULT_EXECUTION_CODE),
      batchCode: String(config.batchCode || DEFAULT_BATCH_CODE)
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    console.error(`BLOCKED: ${result.reason}`)
    for (const item of config.missing) {
      console.error(`- ${item.key}: ${item.description}`)
    }
    process.exitCode = 1
    return
  }

  try {
    const result = await runRealFlow(config)
    result.generatedAt = new Date().toISOString()
    result.resultDir = RESULT_DIR
    result.evidenceFile = config.evidenceFile
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    console.log(`PASS: eDHR tracking/signature real E2E. Trace: ${result.trace}`)
  } catch (error) {
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.blocked ? error.message : undefined,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: config.evidenceFile,
      executionId: String(config.executionId),
      executionCode: String(config.executionCode),
      batchCode: String(config.batchCode),
      steps: error.steps || [],
      trace: error.tracePath,
      error: serializeError(error),
      missing: error.blocked
        ? [
            {
              key: 'playwright',
              description: error.message
            }
          ]
        : undefined,
      invalidConfig: false
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    console.error(`${result.status}: ${error.message}`)
    process.exitCode = 1
  }
}

if (process.argv.includes('--static-display-format')) {
  runStaticDisplayFormatCheck()
} else {
  main()
}
