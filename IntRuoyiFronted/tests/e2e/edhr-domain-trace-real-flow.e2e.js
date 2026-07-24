const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = process.env.EDHR_E2E_TASK_ID || '20260528-edhr-domain-trace-implementation'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-domain-trace')
const DEFAULT_EVIDENCE_FILE = path.join(RESULT_DIR, 'evidence.md')
const REQUIRED_BASE_URL = 'http://localhost:8081'
const DEFAULT_DOMAIN_TRACE_LIST_ROUTE = '/mes/pro/feedback/edhr-domain-trace'
const DEFAULT_DOMAIN_TRACE_DETAIL_ROUTE = '/mes/pro/feedback/edhr-domain-trace/detail'
const DOMAIN_TRACE_PAGE_ENDPOINT = '/mes/pro/batch-record-execution/domain-trace/page'
const DOMAIN_TRACE_DETAIL_ENDPOINT = '/mes/pro/batch-record-execution/domain-trace/detail'
const DOMAIN_TRACE_VERIFY_ENDPOINT = '/mes/pro/batch-record-execution/domain-trace/verify'
const EXPECTED_DOMAIN_TRACE_STATUSES = new Set(['VERIFIED', 'BLOCKED'])
const DOMAIN_TRACE_STATUS_LABELS = {
  VERIFIED: '已校验',
  BLOCKED: '已阻塞',
  UNVERIFIED: '未校验'
}

const REQUIRED_ENV = [
  ['EDHR_E2E_BASE_URL', '真实前端入口，必须为 http://localhost:8081'],
  ['EDHR_E2E_TENANT', '测试租户名称，禁止使用 live 芋道源码租户'],
  ['EDHR_E2E_EXECUTOR_USERNAME', '执行人账号'],
  ['EDHR_E2E_EXECUTOR_PASSWORD', '执行人登录密码'],
  ['EDHR_E2E_DOMAIN_TRACE_EXECUTION_ID', '带有真实主数据追溯上下文的 eDHR 执行记录 ID'],
  ['EDHR_E2E_DOMAIN_TRACE_EXECUTION_CODE', '业务可见执行编号，用于页面证据断言']
]

const FORBIDDEN_LIVE_TENANTS = new Set(['芋道源码', 'yudao', 'Yudao', 'YUDAO'])
const DOMAIN_TRACE_ITEM_FIELDS = [
  'itemType',
  'itemKey',
  'itemName',
  'sourceId',
  'sourceCode',
  'sourceVersion',
  'snapshotJson',
  'snapshotHash',
  'status',
  'blockerReason'
]
const DOMAIN_TRACE_BLOCKER_FIELDS = ['itemType', 'itemKey', 'blockerCode', 'blockerMessage']

const BDD_SCENARIOS = [
  'BDD: 主数据追溯列表可查询 -> Given 执行人登录测试租户, When 通过真实列表路由打开 `/mes/pro/feedback/edhr-domain-trace` 并等待 `/domain-trace/page` 响应, Then 页面和分页 rows 展示 executionCode、status、domainTraceHash、blockerCount 和 itemCount。',
  'BDD: 主数据追溯列表进入详情 -> Given 目标执行记录已经出现在主数据追溯列表, When 用户点击列表中的执行编号或详情入口, Then 前端进入 `/mes/pro/feedback/edhr-domain-trace/detail` 并继续展示该执行记录的 canonical 详情证据。',
  'BDD: 主数据追溯详情可见 -> Given 执行人登录测试租户, When 通过真实主数据追溯详情路由打开指定执行记录, Then 页面展示执行编号、status、domainTraceHash、blockers[] 和 items[] canonical 追溯明细。',
  'BDD: 主数据追溯校验由前端触发 -> Given 主数据追溯详情页已加载, When 用户点击页面上的校验动作, Then 前端发起 `/domain-trace/verify` 请求并展示后端返回的 canonical 校验状态。',
  'BDD: 主数据追溯 UI/API 证据一致 -> Given 校验动作完成, When 使用已登录页面上下文读取 `/domain-trace/detail`, Then API 中的 status/domainTraceHash/blockers/items 与页面关键证据一致。',
  'BDD: 主数据追溯详情进入执行详情 -> Given 主数据追溯详情页已展示目标 executionId, When 用户点击“执行详情”, Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>` 并展示同一执行编号。',
  'BDD: 主数据追溯 E2E 缺前置即阻塞 -> Given 缺少测试租户、真实账号、执行记录或前端入口, When 脚本启动, Then fail-fast 写入 evidence markdown 且不使用 mock、API 替代或 silent downgrade。'
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function collectConfig() {
  const missing = REQUIRED_ENV.filter(([key]) => !envValue(key)).map(([key, description]) => ({
    key,
    description
  }))
  const evidenceFile = process.env.EDHR_E2E_EVIDENCE_FILE
    ? path.resolve(process.env.EDHR_E2E_EVIDENCE_FILE)
    : DEFAULT_EVIDENCE_FILE

  if (missing.length > 0) {
    return {
      missing,
      evidenceFile,
      invalidConfig: false
    }
  }

  const config = {
    missing: [],
    baseUrl: envValue('EDHR_E2E_BASE_URL').replace(/\/+$/, ''),
    tenant: envValue('EDHR_E2E_TENANT'),
    executor: {
      username: envValue('EDHR_E2E_EXECUTOR_USERNAME'),
      password: envValue('EDHR_E2E_EXECUTOR_PASSWORD')
    },
    executionId: envValue('EDHR_E2E_DOMAIN_TRACE_EXECUTION_ID'),
    executionCode: envValue('EDHR_E2E_DOMAIN_TRACE_EXECUTION_CODE'),
    domainTraceListRoute: DEFAULT_DOMAIN_TRACE_LIST_ROUTE,
    domainTraceDetailRoute:
      envValue('EDHR_E2E_DOMAIN_TRACE_DETAIL_ROUTE') || DEFAULT_DOMAIN_TRACE_DETAIL_ROUTE,
    expectedStatus: envValue('EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS') || undefined,
    expectedBlockerCount: envValue('EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT')
      ? Number(envValue('EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT'))
      : undefined,
    expectedLoginName: envValue('EDHR_E2E_EXPECTED_LOGIN_NAME'),
    evidenceFile,
    headed: process.env.EDHR_E2E_HEADED === '1'
  }

  const invalid = collectInvalidConfig(config)
  if (invalid.length > 0) {
    return {
      missing: invalid,
      evidenceFile,
      invalidConfig: true
    }
  }

  return config
}

function collectInvalidConfig(config) {
  const invalid = []

  if (config.baseUrl !== REQUIRED_BASE_URL) {
    invalid.push({
      key: 'EDHR_E2E_BASE_URL',
      description: `当前值为 ${config.baseUrl}；本任务真实前端入口必须固定为 ${REQUIRED_BASE_URL}。`
    })
  }

  if (FORBIDDEN_LIVE_TENANTS.has(config.tenant)) {
    invalid.push({
      key: 'EDHR_E2E_TENANT',
      description: '当前值命中 live 保护名单；主数据追溯 E2E 只能使用真实测试租户。'
    })
  }

  if (!/^\d+$/.test(config.executionId)) {
    invalid.push({
      key: 'EDHR_E2E_DOMAIN_TRACE_EXECUTION_ID',
      description: '批记录执行 ID 必须是后端 Long ID，不能使用空值、编号、UUID 或占位文本。'
    })
  }

  if (!config.domainTraceDetailRoute.startsWith('/')) {
    invalid.push({
      key: 'EDHR_E2E_DOMAIN_TRACE_DETAIL_ROUTE',
      description: '可选详情路由必须是前端站内绝对路径，例如 /mes/pro/feedback/edhr-domain-trace/detail。'
    })
  }

  if (config.expectedStatus && !EXPECTED_DOMAIN_TRACE_STATUSES.has(config.expectedStatus)) {
    invalid.push({
      key: 'EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS',
      description: '可选期望状态只允许 VERIFIED 或 BLOCKED；配置后必须与最终 finalSummary.status 完全匹配。'
    })
  }

  const expectedBlockerCountRaw = envValue('EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT')
  if (expectedBlockerCountRaw && !/^\d+$/.test(expectedBlockerCountRaw)) {
    invalid.push({
      key: 'EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT',
      description: '可选期望 blocker 数量必须是非负整数；配置后必须与最终 finalSummary.blockerCount 完全匹配。'
    })
  }

  return invalid
}

function buildExpectedSummary(config) {
  return {
    status: config?.expectedStatus ?? null,
    blockerCount: config?.expectedBlockerCount ?? null
  }
}

function readExpectedSummaryFromEnv() {
  const expectedBlockerCountRaw = envValue('EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT')
  return {
    status: envValue('EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS') || null,
    blockerCount: expectedBlockerCountRaw
      ? /^\d+$/.test(expectedBlockerCountRaw)
        ? Number(expectedBlockerCountRaw)
        : expectedBlockerCountRaw
      : null
  }
}

function buildActualSummary(summary) {
  return {
    status: summary?.status ?? null,
    blockerCount: summary?.blockerCount ?? null
  }
}

function assertExpectedFinalSummary(config, finalSummary) {
  const expectedSummary = buildExpectedSummary(config)
  const actualSummary = buildActualSummary(finalSummary)

  if (config.expectedStatus !== undefined) {
    try {
      assert.equal(
        finalSummary.status,
        config.expectedStatus,
        `Expected final DomainTrace status ${config.expectedStatus}, actual ${finalSummary.status}`
      )
    } catch (error) {
      error.expectedSummary = expectedSummary
      error.actualSummary = actualSummary
      throw error
    }
  }

  if (config.expectedBlockerCount !== undefined) {
    try {
      assert.equal(
        finalSummary.blockerCount,
        config.expectedBlockerCount,
        `Expected final DomainTrace blockerCount ${config.expectedBlockerCount}, actual ${finalSummary.blockerCount}`
      )
    } catch (error) {
      error.expectedSummary = expectedSummary
      error.actualSummary = actualSummary
      throw error
    }
  }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error(
      'Missing Playwright runtime. Run `pnpm install` in this workspace so package.json devDependency `playwright` is installed, then re-run `pnpm e2e:edhr:domain-trace`.'
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
  const expectedSummary = result.expectedSummary || {}
  const actualSummary = result.actualSummary || buildActualSummary(result.finalSummary)
  const formatEvidenceValue = (value) => (value === undefined || value === null ? '未配置/未产生' : String(value))
  const lines = [
    '# eDHR 主数据追溯真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 生成时间：${new Date().toISOString()}`,
    `- 前端 worktree：${process.cwd()}`,
    `- 固定前端入口：\`${REQUIRED_BASE_URL}\``,
    '- 真实 E2E 复跑命令：`pnpm e2e:edhr:domain-trace`',
    '- 静态语法检查命令：`pnpm e2e:edhr:domain-trace:check`',
    '- 产物目录：`test-results/edhr-domain-trace/`（截图、trace、result.json、evidence.md 均不提交）',
    `- 当前状态：${result.status}`,
    `- Expected final status: \`${formatEvidenceValue(expectedSummary.status)}\``,
    `- Actual final status: \`${formatEvidenceValue(actualSummary.status)}\``,
    `- Expected final blocker count: \`${formatEvidenceValue(expectedSummary.blockerCount)}\``,
    `- Actual final blocker count: \`${formatEvidenceValue(actualSummary.blockerCount)}\``,
    ''
  ]

  lines.push('## BDD')
  lines.push('')
  for (const scenario of BDD_SCENARIOS) lines.push(`- ${scenario}`)
  lines.push('')

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED')
    lines.push('')
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:domain-trace\` -> FAIL, ${result.reason}`)
    if (result.missing?.length) {
      lines.push(result.invalidConfig ? '- 不满足的真实 E2E 前置条件：' : '- 缺失环境变量：')
      for (const item of result.missing) {
        lines.push(`  - \`${item.key}\`：${item.description}`)
      }
    }
    lines.push('- 影响：无法通过真实前端登录测试租户、打开主数据追溯列表、从列表进入详情、触发校验或生成 UI/API 一致性证据；未使用 mock、API 替代或 silent downgrade。')
    lines.push('')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN')
    lines.push('')
    lines.push('- GREEN: `pnpm e2e:edhr:domain-trace` -> PASS, 真实 UI 主数据追溯列表查看、从列表进入详情、校验和最终 API 交叉确认完成。')
    for (const step of result.steps || []) {
      lines.push(`- ${step.name} -> PASS${step.screenshot ? `, screenshot: \`${step.screenshot}\`` : ''}`)
    }
    lines.push(`- Trace: \`${result.trace}\``)
    if (result.listSummary) {
      lines.push(`- List status: \`${result.listSummary.status}\``)
      lines.push(`- List hash: \`${result.listSummary.hash}\``)
      lines.push(`- List blocker count: \`${result.listSummary.blockerCount}\``)
      lines.push(`- List item count: \`${result.listSummary.itemCount}\``)
    }
    if (result.finalSummary) {
      lines.push(`- Final status: \`${result.finalSummary.status}\``)
      lines.push(`- Final hash: \`${result.finalSummary.hash}\``)
      lines.push(`- Final blocker count: \`${result.finalSummary.blockerCount}\``)
      lines.push(`- Final item count: \`${result.finalSummary.itemCount}\``)
    }
    lines.push('')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED')
    lines.push('')
    lines.push(`- RED: \`pnpm e2e:edhr:domain-trace\` -> FAIL, ${result.error?.message || '未知错误'}`)
    lines.push('- 影响：真实 UI E2E 未放行；不得提交为通过。')
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

async function visibleCount(locator) {
  const count = await locator.count()
  let visible = 0
  for (let index = 0; index < count; index += 1) {
    if (await locator.nth(index).isVisible()) visible += 1
  }
  return visible
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
    config.executor.username,
    '执行人登录页缺少用户名输入框。'
  )
  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入密码"]'),
    config.executor.password,
    '执行人登录页缺少密码输入框。'
  )

  await clickVisibleButton(loginForm, /^登录$/, '执行人登录页缺少登录按钮。')
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })

  if (config.expectedLoginName) {
    await waitForText(
      page,
      config.expectedLoginName,
      `登录后页面未展示预期登录标识 ${config.expectedLoginName}`
    )
  }
}

function buildDomainTraceDetailUrl(config) {
  const url = new URL(config.domainTraceDetailRoute, config.baseUrl)
  url.searchParams.set('executionId', config.executionId)
  url.searchParams.set('executionCode', config.executionCode)
  return url.toString()
}

function buildDomainTraceListUrl(config) {
  const url = new URL(config.domainTraceListRoute, config.baseUrl)
  url.searchParams.set('executionCode', config.executionCode)
  url.searchParams.set('executionId', config.executionId)
  return url.toString()
}

function isDomainTracePageResponse(response, config) {
  if (response.request().method() !== 'GET') return false
  if (!response.url().includes(DOMAIN_TRACE_PAGE_ENDPOINT)) return false
  try {
    const url = new URL(response.url())
    return (
      url.searchParams.get('executionId') === String(config.executionId) &&
      url.searchParams.get('executionCode') === String(config.executionCode)
    )
  } catch (error) {
    return false
  }
}

function isDomainTraceDetailResponse(response, executionId) {
  if (!response.url().includes(DOMAIN_TRACE_DETAIL_ENDPOINT)) return false
  try {
    const url = new URL(response.url())
    return url.searchParams.get('executionId') === String(executionId)
  } catch (error) {
    return false
  }
}

async function openDomainTraceList(page, config) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(DOMAIN_TRACE_PAGE_ENDPOINT) &&
      isDomainTracePageResponse(response, config),
    { timeout: 60000 }
  )
  await page.goto(buildDomainTraceListUrl(config), {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await waitForText(page, /执行编号/, '未进入 eDHR 主数据追溯列表页')
  const response = await pageResponsePromise
  const pageData = await parseJsonResponse(response, '主数据追溯列表分页')
  const rows = extractDomainTracePageRows(pageData, '主数据追溯列表分页')
  const row = findDomainTraceListRow(rows, config)
  const summary = summarizeDomainTraceListRow(row, '主数据追溯列表目标行')
  await assertPageShowsDomainTraceListEvidence(page, config, summary)
  return {
    response,
    rows,
    row,
    summary
  }
}

async function enterDomainTraceDetailFromList(page, config) {
  const detailResponsePromise = page.waitForResponse(
    (response) => isDomainTraceDetailResponse(response, config.executionId),
    { timeout: 60000 }
  )
  const targetRow = page
    .locator('.edhr-domain-trace__table .el-table__row')
    .filter({ hasText: config.executionCode })
    .first()
  await targetRow.waitFor({ state: 'visible', timeout: 30000 })
  const executionCodeButton = targetRow.getByRole('button', {
    name: config.executionCode,
    exact: true
  })
  if ((await visibleCount(executionCodeButton)) > 0) {
    await firstVisible(
      executionCodeButton,
      `主数据追溯列表目标行缺少执行编号按钮 ${config.executionCode}。`
    ).then((button) => button.click())
  } else {
    await clickVisibleButton(targetRow, /^详情$/, '主数据追溯列表目标行缺少详情入口。')
  }
  await page.waitForURL(
    (url) =>
      url.pathname === DEFAULT_DOMAIN_TRACE_DETAIL_ROUTE &&
      url.searchParams.get('executionId') === String(config.executionId),
    { timeout: 60000 }
  )
  await waitForText(page, /主数据追溯/, '未进入 eDHR 主数据追溯详情页')
  const response = await detailResponsePromise
  return response
}

async function parseJsonResponse(response, label) {
  assert.equal(response.status(), 200, `${label} HTTP 状态应为 200，实际 ${response.status()}，URL: ${response.url()}`)
  let body
  try {
    body = await response.json()
  } catch (error) {
    throw new Error(`${label} 响应不是 JSON：${error.message}`)
  }
  return unwrapBusinessData(body, label)
}

function unwrapBusinessData(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 响应体必须是对象。`)
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

function findField(source, keys, label) {
  for (const key of keys) {
    if (Object.prototype.hasOwnProperty.call(source, key)) {
      const value = source[key]
      if (value !== undefined && value !== null && value !== '') return { key, value }
    }
  }
  throw new Error(`${label} 缺少字段：${keys.join(' / ')}`)
}

function extractDomainTracePageRows(data, label) {
  assert.ok(data && typeof data === 'object', `${label} data 必须是对象。`)
  assert.ok(Array.isArray(data.list), `${label} PageResult 缺少 list rows。`)
  return data.list
}

function findDomainTraceListRow(rows, config) {
  assert.ok(Array.isArray(rows), '主数据追溯列表 rows 必须是数组。')
  const executionId = String(config.executionId)
  const executionCode = String(config.executionCode)
  const row = rows.find((item) => {
    if (!item || typeof item !== 'object') return false
    return String(item.executionId) === executionId || String(item.executionCode) === executionCode
  })
  if (!row) {
    throw new Error(
      `主数据追溯列表分页 rows 未包含目标执行记录：executionId=${executionId}, executionCode=${executionCode}。`
    )
  }
  return row
}

function readDomainTraceCount(row, countField, arrayField, label, options = {}) {
  const hasCountField =
    Object.prototype.hasOwnProperty.call(row, countField) &&
    row[countField] !== undefined &&
    row[countField] !== null &&
    row[countField] !== ''
  const hasArrayField =
    Object.prototype.hasOwnProperty.call(row, arrayField) &&
    row[arrayField] !== undefined &&
    row[arrayField] !== null

  let parsed
  if (hasCountField) {
    parsed = Number(row[countField])
    assert.ok(Number.isFinite(parsed), `${label} ${countField} 必须可数字化，实际值：${row[countField]}。`)
  } else if (hasArrayField) {
    assert.ok(Array.isArray(row[arrayField]), `${label} ${arrayField} 必须是数组，才能作为 ${countField} 来源。`)
    parsed = row[arrayField].length
  } else {
    throw new Error(`${label} 缺少 ${countField} 来源：必须提供 ${countField} 或 ${arrayField}.length。`)
  }

  assert.ok(parsed >= 0, `${label} ${countField} 必须是非负数，实际值：${parsed}。`)
  if (options.requirePositive) {
    assert.ok(parsed > 0, `${label} ${countField} 必须大于 0，实际值：${parsed}。`)
  }
  return parsed
}

function summarizeDomainTraceListRow(row, label) {
  assert.ok(row && typeof row === 'object', `${label} 必须是对象。`)
  const statusField = findField(row, ['status'], `${label} status`)
  const hashField = findField(row, ['domainTraceHash'], `${label} domainTraceHash`)
  const blockerCount = readDomainTraceCount(row, 'blockerCount', 'blockers', label)
  const itemCount = readDomainTraceCount(row, 'itemCount', 'items', label, {
    requirePositive: true
  })

  return {
    status: String(statusField.value),
    hash: String(hashField.value),
    blockerCount,
    itemCount
  }
}

function toArray(value) {
  if (Array.isArray(value)) return value
  if (value && typeof value === 'object') {
    if (Array.isArray(value.list)) return value.list
    if (Array.isArray(value.records)) return value.records
    if (Array.isArray(value.items)) return value.items
  }
  return []
}

function firstEvidenceText(items, keys) {
  for (const item of items) {
    if (!item || typeof item !== 'object') continue
    for (const key of keys) {
      const value = item[key]
      if (value !== undefined && value !== null && String(value).trim()) {
        return String(value).trim()
      }
    }
  }
  return ''
}

function requireCanonicalFields(record, fields, label) {
  assert.ok(record && typeof record === 'object', `${label} 必须是对象。`)
  for (const field of fields) {
    assert.ok(
      Object.prototype.hasOwnProperty.call(record, field),
      `${label} 缺少后端 canonical 字段 \`${field}\`。`
    )
  }
}

function requireCanonicalRows(rows, fields, label) {
  for (let index = 0; index < rows.length; index += 1) {
    requireCanonicalFields(rows[index], fields, `${label}[${index}]`)
  }
}

function summarizeDomainTraceData(data, label) {
  assert.ok(data && typeof data === 'object', `${label} data 必须是对象。`)
  const statusField = findField(data, ['status'], `${label} status`)
  const hashField = findField(data, ['domainTraceHash'], `${label} domainTraceHash`)
  const blockerField = findField(data, ['blockers'], `${label} blockers`)
  const itemField = findField(data, ['items'], `${label} items`)
  const items = toArray(itemField.value)
  assert.ok(items.length > 0, `${label} 主数据追溯 items 必须至少包含一条真实追溯项。`)
  const blockers = toArray(blockerField.value)
  requireCanonicalRows(items, DOMAIN_TRACE_ITEM_FIELDS, `${label} items`)
  requireCanonicalRows(blockers, DOMAIN_TRACE_BLOCKER_FIELDS, `${label} blockers`)

  return {
    status: String(statusField.value),
    hash: String(hashField.value),
    blockerCount: blockers.length,
    blockers,
    items,
    itemCount: items.length,
    blockerEvidence: firstEvidenceText(blockers, [
      'blockerMessage',
      'blockerCode',
      'itemType',
      'itemKey'
    ]),
    itemEvidence: firstEvidenceText(items, [
      'itemType',
      'itemKey',
      'itemName',
      'sourceId',
      'sourceCode',
      'sourceVersion',
      'snapshotHash'
    ]),
    itemStatusEvidence: firstEvidenceText(items, ['status'])
  }
}

async function assertPageShowsDomainTraceListEvidence(page, config, summary) {
  await waitForText(page, config.executionCode, `主数据追溯列表未展示执行编号 ${config.executionCode}`)
  const targetRow = page
    .locator('.edhr-domain-trace__table .el-table__row')
    .filter({ hasText: config.executionCode })
    .first()
  await targetRow.waitFor({ state: 'visible', timeout: 30000 })
  const expandIcon = targetRow.locator('.el-table__expand-icon').first()
  if ((await expandIcon.count()) > 0 && (await expandIcon.isVisible())) {
    const expanded = await targetRow.getAttribute('class')
    if (!String(expanded || '').includes('expanded')) {
      await expandIcon.click()
    }
  }

  const bodyText = (await page.locator('body').textContent({ timeout: 30000 })) || ''
  const statusLabel = DOMAIN_TRACE_STATUS_LABELS[summary.status]

  assert.match(bodyText, /执行编号/, '主数据追溯列表页面未展示执行编号列或查询项。')
  assert.ok(bodyText.includes(config.executionCode), `主数据追溯列表页面未展示执行编号 ${config.executionCode}。`)
  assert.ok(
    bodyText.includes(summary.status) || (statusLabel && bodyText.includes(statusLabel)),
    `主数据追溯列表页面未展示目标状态或状态标签：${summary.status} / ${statusLabel || '未知标签'}。`
  )
  assert.match(bodyText, /追溯哈希|哈希/i, '主数据追溯列表展开证据未展示追溯哈希标签。')
  assert.ok(
    bodyText.includes(summary.hash.slice(0, 12)),
    `主数据追溯列表展开证据未展示分页返回 hash 的前 12 位：${summary.hash.slice(0, 12)}。`
  )
  assert.ok(
    bodyText.includes(`${summary.blockerCount} 项阻塞`) || bodyText.includes(`${summary.blockerCount} 项`),
    `主数据追溯列表页面未展示阻塞数量 ${summary.blockerCount} 项。`
  )
  assert.ok(
    bodyText.includes(`${summary.itemCount} 项`),
    `主数据追溯列表页面未展示追溯项数量 ${summary.itemCount} 项。`
  )
}

async function assertPageShowsDomainTraceEvidence(page, config, summary, label) {
  const bodyText = (await page.locator('body').textContent({ timeout: 30000 })) || ''
  assert.match(bodyText, /主数据追溯/, `${label} 页面未展示“主数据追溯”。`)
  assert.ok(
    bodyText.includes(config.executionCode) || bodyText.includes(config.executionId),
    `${label} 页面未展示执行编号或执行 ID。执行编号：${config.executionCode}；执行 ID：${config.executionId}`
  )
  assert.match(bodyText, /状态|status/i, `${label} 页面未展示 status 证据。`)
  assert.match(bodyText, /hash|哈希/i, `${label} 页面未展示 hash 证据。`)
  assert.match(bodyText, /阻塞|blocker/i, `${label} 页面未展示 blockers 证据。`)
  assert.match(
    bodyText,
    /追溯项|追溯明细|明细|items|WORK_ORDER|MATERIAL|EQUIPMENT|PERSONNEL|QC/i,
    `${label} 页面未展示 items 证据。`
  )
  assert.ok(
    bodyText.includes(summary.hash.slice(0, 12)),
    `${label} 页面未展示 API 返回 hash 的前 12 位：${summary.hash.slice(0, 12)}`
  )
  if (summary.blockerEvidence) {
    assert.ok(
      bodyText.includes(summary.blockerEvidence),
      `${label} 页面未展示阻塞项关键证据：${summary.blockerEvidence}`
    )
  }
  if (summary.itemEvidence) {
    assert.ok(
      bodyText.includes(summary.itemEvidence),
      `${label} 页面未展示追溯项关键证据：${summary.itemEvidence}`
    )
  }
  if (summary.itemStatusEvidence) {
    assert.ok(
      bodyText.includes(summary.itemStatusEvidence),
      `${label} 页面未展示追溯项 status 补充证据：${summary.itemStatusEvidence}`
    )
  }
}

async function clickConfirmIfVisible(page) {
  const dialog = page.locator('.el-message-box:visible, .el-dialog:visible').last()
  if ((await dialog.count()) === 0 || !(await dialog.isVisible())) return
  const confirmButtons = dialog.getByRole('button', { name: /确认|确定|确\s*定/ })
  if ((await visibleCount(confirmButtons)) > 0) {
    await firstVisible(confirmButtons, '确认弹框缺少确认按钮。').then((button) => button.click())
  }
}

async function triggerDomainTraceVerification(page, config) {
  const verifyResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes(DOMAIN_TRACE_VERIFY_ENDPOINT),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /校验|验证|Verify/i, '主数据追溯详情页缺少“校验/验证”按钮。')
  await clickConfirmIfVisible(page)
  const response = await verifyResponsePromise
  const requestBody = response.request().postData() || ''
  assert.ok(
    requestBody.includes(config.executionId),
    `主数据追溯校验请求未携带目标 executionId=${config.executionId}。实际请求体：${requestBody}`
  )
  return response
}

function apiHeadersFromRequest(headers) {
  const picked = {}
  for (const key of ['authorization', 'tenant-id', 'visit-tenant-id', 'accept']) {
    if (headers[key]) picked[key] = headers[key]
  }
  assert.ok(picked.authorization, '最终 API 交叉确认缺少已登录 Authorization 请求头。')
  assert.ok(picked['tenant-id'], '最终 API 交叉确认缺少 tenant-id 请求头，无法证明测试租户上下文。')
  return picked
}

async function crossCheckDetailWithLoggedContext(page, detailResponse, verifySummary) {
  const requestHeaders = await detailResponse.request().allHeaders()
  const response = await page.request.get(detailResponse.url(), {
    headers: apiHeadersFromRequest(requestHeaders),
    timeout: 60000
  })
  const data = await parseJsonResponse(response, '最终主数据追溯详情 API 交叉确认')
  const finalSummary = summarizeDomainTraceData(data, '最终主数据追溯详情 API 交叉确认')
  assert.equal(finalSummary.hash, verifySummary.hash, '最终详情 API hash 与校验后 UI/API hash 不一致。')
  assert.equal(finalSummary.status, verifySummary.status, '最终详情 API status 与校验后 UI/API status 不一致。')
  assert.equal(
    finalSummary.itemCount,
    verifySummary.itemCount,
    '最终详情 API items 数量与校验后 UI/API items 数量不一致。'
  )
  return finalSummary
}

async function openExecutionFromDomainTraceDetail(page, config) {
  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes(`/mes/pro/batch-record-execution/get?id=${config.executionId}`),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /^执行详情$/, '主数据追溯详情页缺少“执行详情”按钮。')
  await page.waitForURL(
    (url) =>
      url.pathname === '/mes/pro/feedback/edhr-execution/detail' &&
      url.searchParams.get('id') === String(config.executionId),
    { timeout: 60000 }
  )
  const detailResponse = await detailResponsePromise
  const detailData = await parseJsonResponse(detailResponse, '主数据追溯进入执行详情')
  assert.equal(String(detailData.id), String(config.executionId), '执行详情接口返回的 id 与主数据追溯 executionId 不一致。')
  await waitForText(page, 'eDHR 执行详情', '主数据追溯进入后未展示 eDHR 执行详情页')
  await waitForText(page, config.executionCode, `执行详情页未展示执行编号 ${config.executionCode}`)
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
  const tracePath = path.join(RESULT_DIR, 'trace.zip')
  const page = await context.newPage()
  const steps = []

  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  try {
    await login(page, config)
    const listResult = await openDomainTraceList(page, config)
    steps.push({
      name: '主数据追溯列表目标行可见',
      screenshot: await screenshot(page, 'domain-trace-list', steps)
    })

    const detailResponse = await enterDomainTraceDetailFromList(page, config)
    const detailData = await parseJsonResponse(detailResponse, '主数据追溯详情')
    const detailSummary = summarizeDomainTraceData(detailData, '主数据追溯详情')
    await assertPageShowsDomainTraceEvidence(page, config, detailSummary, '主数据追溯详情')
    steps.push({
      name: '主数据追溯详情可见',
      screenshot: await screenshot(page, 'domain-trace-detail', steps)
    })

    const verifyResponse = await triggerDomainTraceVerification(page, config)
    const verifyData = await parseJsonResponse(verifyResponse, '主数据追溯校验')
    const verifySummary = summarizeDomainTraceData(verifyData, '主数据追溯校验')
    await assertPageShowsDomainTraceEvidence(page, config, verifySummary, '主数据追溯校验后')
    steps.push({
      name: '主数据追溯校验状态可见',
      screenshot: await screenshot(page, 'domain-trace-verified', steps)
    })

    const finalSummary = await crossCheckDetailWithLoggedContext(page, detailResponse, verifySummary)
    assertExpectedFinalSummary(config, finalSummary)
    steps.push({ name: '已登录上下文 API 最终交叉确认' })

    await openExecutionFromDomainTraceDetail(page, config)
    steps.push({
      name: '主数据追溯详情进入执行详情',
      screenshot: await screenshot(page, 'domain-trace-open-execution', steps)
    })

    await context.tracing.stop({ path: tracePath })
    await browser.close()
    return {
      status: 'PASS',
      steps,
      listSummary: listResult.summary,
      expectedSummary: buildExpectedSummary(config),
      actualSummary: buildActualSummary(finalSummary),
      finalSummary,
      trace: tracePath,
      resultFile: path.join(RESULT_DIR, 'result.json')
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
    throw Object.assign(error, { tracePath, steps })
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: config.invalidConfig
        ? '真实 E2E 前置条件不满足，不能使用 live 租户、错误前端入口或无效执行记录。'
        : '缺少真实前端入口、测试租户、执行人账号密码、执行记录 ID 或业务执行编号，不能执行真实 UI E2E。',
      missing: config.missing,
      invalidConfig: config.invalidConfig === true,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: config.evidenceFile,
      expectedSummary: readExpectedSummaryFromEnv(),
      actualSummary: buildActualSummary()
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
    console.log(`PASS: eDHR domain trace real E2E. Trace: ${result.trace}`)
  } catch (error) {
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.blocked ? error.message : undefined,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: config.evidenceFile,
      expectedSummary: error.expectedSummary || buildExpectedSummary(config),
      actualSummary: error.actualSummary || buildActualSummary(error.finalSummary),
      trace: error.tracePath,
      steps: error.steps || [],
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

main()
