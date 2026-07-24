const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')

const TASK_ID = '20260528-edhr-archive-approval-evidence'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-approval-tracking')
const DEFAULT_EVIDENCE_FILE = path.resolve(
  process.cwd(),
  'doc',
  'tasks',
  TASK_ID,
  'real-e2e-evidence.md'
)

const REQUIRED_ENV = [
  ['EDHR_E2E_BASE_URL', '真实前端入口，例如 http://localhost:8081'],
  ['EDHR_E2E_TENANT', '测试租户名称，禁止使用 live 芋道源码租户改数据'],
  ['EDHR_E2E_EXECUTOR_USERNAME', '执行人账号'],
  ['EDHR_E2E_EXECUTOR_PASSWORD', '执行人登录密码'],
  ['EDHR_E2E_APPROVER_USERNAME', '审批人账号'],
  ['EDHR_E2E_APPROVER_PASSWORD', '审批人登录密码'],
  ['EDHR_E2E_EXECUTOR_SIGNATURE_PASSWORD', '执行提交电子签名密码'],
  ['EDHR_E2E_APPROVER_SIGNATURE_PASSWORD', '审批电子签名密码'],
  ['EDHR_E2E_ARCHIVE_SIGNATURE_PASSWORD', '归档封存电子签名密码'],
  ['EDHR_E2E_FIELD_CHANGE_SIGNATURE_PASSWORD', 'FIELD_CHANGE 字段变更电子签名密码'],
  ['EDHR_E2E_DRAFT_WORK_ORDER_CODE', 'DRAFT 禁止归档负向证明使用的真实生产工单编码，必须是未打开过 eDHR 的测试租户数据'],
  ['EDHR_E2E_DRAFT_TASK_CODE', 'DRAFT 禁止归档负向证明使用的真实生产任务编号，必须与 DRAFT 工单匹配'],
  ['EDHR_E2E_APPROVE_FLOW_WORK_ORDER_CODE', '审批通过流程 fresh DRAFT 使用的真实生产工单编码，必须是未打开过 eDHR 的测试租户数据'],
  ['EDHR_E2E_APPROVE_FLOW_TASK_CODE', '审批通过流程 fresh DRAFT 使用的真实生产任务编号，必须与审批通过工单匹配'],
  ['EDHR_E2E_REJECT_FLOW_WORK_ORDER_CODE', '审批驳回流程 fresh DRAFT 使用的真实生产工单编码，必须是未打开过 eDHR 的测试租户数据'],
  ['EDHR_E2E_REJECT_FLOW_TASK_CODE', '审批驳回流程 fresh DRAFT 使用的真实生产任务编号，必须与审批驳回工单匹配'],
  ['EDHR_E2E_EXPECTED_APPROVER_NAME', '追踪/签名页应展示的审批人员姓名'],
  ['EDHR_E2E_EXPECTED_REJECT_REASON', '追踪/签名页应展示的驳回原因']
]

const FORBIDDEN_LIVE_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])

const FRESH_CONTEXT_DEFINITIONS = [
  {
    key: 'draft',
    label: 'DRAFT 禁止归档负向证明',
    workOrderEnv: 'EDHR_E2E_DRAFT_WORK_ORDER_CODE',
    taskEnv: 'EDHR_E2E_DRAFT_TASK_CODE'
  },
  {
    key: 'approveFlow',
    label: '审批通过流程',
    workOrderEnv: 'EDHR_E2E_APPROVE_FLOW_WORK_ORDER_CODE',
    taskEnv: 'EDHR_E2E_APPROVE_FLOW_TASK_CODE'
  },
  {
    key: 'rejectFlow',
    label: '审批驳回流程',
    workOrderEnv: 'EDHR_E2E_REJECT_FLOW_WORK_ORDER_CODE',
    taskEnv: 'EDHR_E2E_REJECT_FLOW_TASK_CODE'
  }
]

const SUBMITTED_FRESH_CONTEXT_DEFINITION = {
  key: 'submittedFlow',
  label: 'SUBMITTED 禁止归档负向证明',
  workOrderEnv: 'EDHR_E2E_SUBMITTED_FLOW_WORK_ORDER_CODE',
  taskEnv: 'EDHR_E2E_SUBMITTED_FLOW_TASK_CODE'
}

const BDD_SCENARIOS = [
  'BDD: 批次入口与工序草稿创建 -> Given 真实测试租户工单/任务上下文, When 执行人从生产报工入口打开 eDHR 批次并打开工序任务, Then 批次 open-or-create 必须返回 batchExecutionId/batchExecutionCode，工序 task/open 必须返回 executionId 并进入执行详情。',
  'BDD: DRAFT 禁止归档 -> Given 草稿 eDHR 执行记录, When 执行人通过真实 UI 查看详情, Then 页面不暴露可执行归档动作且不发起归档接口。',
  'BDD: SUBMITTED 禁止归档 -> Given 已提交待审批记录或可提交草稿二选一输入, When 用户通过真实 UI 查看待审批详情, Then 页面提示审批关闭后才可归档且不发起归档接口。',
  'BDD: FIELD_CHANGE 字段审计 -> Given 审批通过流程 fresh 草稿存在可编辑字段, When 执行人修改字段、填写原因并输入 FIELD_CHANGE 签名密码, Then 保存、verify-chain 和详情均返回/展示 VALID FIELD_CHANGE 证据。',
  'BDD: 提交后进入审批 -> Given 可提交草稿和执行人签名密码, When 执行人通过真实 UI 提交, Then 记录进入待审批状态并在审批列表可查询。',
  'BDD: 审批详情真实 API 展示 -> Given 脚本提交出的真实 BPM 待办和业务可见执行编号, When 审批人从审批列表点击执行编号, Then 前端进入 `/mes/pro/feedback/edhr-approval/detail` 并等待真实 `/mes/pro/batch-record-execution/approval-detail` 响应展示同一执行编号。',
  'BDD: 审批通过关闭 -> Given 脚本提交出的真实 BPM 待办和业务可见执行编号, When 审批人从审批列表真实 UI 点击通过并输入签名密码, Then 记录进入已关闭状态。',
  'BDD: 审批驳回留痕 -> Given 脚本提交出的真实 BPM 待办和业务可见执行编号, When 审批人从审批列表真实 UI 点击驳回并输入签名密码和驳回原因, Then 记录进入已驳回状态并保留原因。',
  'BDD: 我已审批列表可追溯 -> Given 本轮审批人刚完成通过和驳回动作, When 审批人打开 `/mes/pro/feedback/edhr-approval?tab=done` 并查询执行编号, Then 前端等待真实 `/mes/pro/batch-record-execution/approval-done-page` 响应并展示已关闭或已驳回记录。',
  'BDD: 关闭后可归档 -> Given 刚通过审批关闭的真实执行记录, When 授权用户通过真实 UI 输入封存密码生成归档, Then 前端发起归档接口并展示 sha256、signatureHash、approvalSnapshotId 和 approvalSnapshotHash 证据。',
  'BDD: 归档版本可查看 -> Given 刚生成的 SEALED 归档, When 用户点击执行详情中的“查看版本”, Then 前端请求真实 `/mes/pro/batch-record-execution-archive/page` 并在“归档版本”弹窗展示同一归档版本和 sha256。',
  'BDD: 受控归档下载 -> Given 刚生成的 SEALED 归档, When 用户通过真实 UI 点击下载归档, Then 浏览器必须从 `/mes/pro/batch-record-execution-archive/download` 获取非空下载文件，保存产物并重算 SHA-256，且 downloadedSha256 必须等于归档响应 sha256。',
  'BDD: 追踪与签名查询 -> Given 本轮通过与驳回流程已产生 BPM、人员、时间和原因事件, When 用户通过真实追踪/签名页查询, Then 页面展示 BPM 任务、人员、时间、原因和签名含义。'
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function resolveEvidenceFile() {
  return process.env.EDHR_E2E_EVIDENCE_FILE
    ? path.resolve(process.env.EDHR_E2E_EVIDENCE_FILE)
    : DEFAULT_EVIDENCE_FILE
}

function buildFreshContext(definition) {
  return {
    key: definition.key,
    label: definition.label,
    workOrderCode: envValue(definition.workOrderEnv),
    taskCode: envValue(definition.taskEnv)
  }
}

function collectConfig() {
  const missing = REQUIRED_ENV.filter(([key]) => !envValue(key)).map(([key, description]) => ({
    key,
    description
  }))
  const submittedExistingId = envValue('EDHR_E2E_SUBMITTED_EXECUTION_ID')
  const submittedFreshContext = buildFreshContext(SUBMITTED_FRESH_CONTEXT_DEFINITION)
  const submittedModeIssues = collectSubmittedModeIssues(submittedExistingId, submittedFreshContext)
  const hasSubmittedFreshContext = Boolean(
    submittedFreshContext.workOrderCode || submittedFreshContext.taskCode
  )
  const submittedMode = submittedExistingId
    ? 'existing-submitted'
    : hasSubmittedFreshContext
      ? 'draft-submit'
      : ''

  missing.push(...submittedModeIssues)
  if (missing.length > 0) {
    return {
      missing,
      evidenceFile: resolveEvidenceFile(),
      invalidConfig: submittedModeIssues.some((item) => item.invalidConfig),
      submittedMode: submittedMode || 'unresolved'
    }
  }
  const config = {
    missing: [],
    baseUrl: envValue('EDHR_E2E_BASE_URL').replace(/\/$/, ''),
    tenant: envValue('EDHR_E2E_TENANT'),
    executor: {
      username: envValue('EDHR_E2E_EXECUTOR_USERNAME'),
      password: envValue('EDHR_E2E_EXECUTOR_PASSWORD'),
      signaturePassword: envValue('EDHR_E2E_EXECUTOR_SIGNATURE_PASSWORD')
    },
    approver: {
      username: envValue('EDHR_E2E_APPROVER_USERNAME'),
      password: envValue('EDHR_E2E_APPROVER_PASSWORD'),
      signaturePassword: envValue('EDHR_E2E_APPROVER_SIGNATURE_PASSWORD')
    },
    archiveSignaturePassword: envValue('EDHR_E2E_ARCHIVE_SIGNATURE_PASSWORD'),
    fieldChangeSignaturePassword: envValue('EDHR_E2E_FIELD_CHANGE_SIGNATURE_PASSWORD'),
    submittedMode,
    ids: {
      submittedExisting: submittedExistingId
    },
    freshContexts: {
      ...Object.fromEntries(
        FRESH_CONTEXT_DEFINITIONS.map((definition) => [
          definition.key,
          buildFreshContext(definition)
        ])
      ),
      submittedFlow: submittedMode === 'draft-submit' ? submittedFreshContext : undefined
    },
    expectedLoginName: envValue('EDHR_E2E_EXPECTED_LOGIN_NAME'),
    expectedApproverName: envValue('EDHR_E2E_EXPECTED_APPROVER_NAME'),
    expectedRejectReason: envValue('EDHR_E2E_EXPECTED_REJECT_REASON'),
    evidenceFile: resolveEvidenceFile(),
    executablePath: envValue('EDHR_E2E_CHROME_EXECUTABLE') || envValue('PLAYWRIGHT_CHROME_EXECUTABLE'),
    headed: process.env.EDHR_E2E_HEADED === '1'
  }

  const invalid = collectInvalidConfig(config)
  if (invalid.length > 0) {
    return {
      missing: invalid,
      evidenceFile: config.evidenceFile,
      invalidConfig: true,
      submittedMode: config.submittedMode
    }
  }

  return config
}

function collectSubmittedModeIssues(submittedExistingId, submittedFreshContext) {
  const hasFreshWorkOrder = Boolean(submittedFreshContext.workOrderCode)
  const hasFreshTask = Boolean(submittedFreshContext.taskCode)
  const hasAnyFreshContext = hasFreshWorkOrder || hasFreshTask

  if (!submittedExistingId && !hasAnyFreshContext) {
    return [
      {
        key: 'EDHR_E2E_SUBMITTED_EXECUTION_ID | EDHR_E2E_SUBMITTED_FLOW_WORK_ORDER_CODE + EDHR_E2E_SUBMITTED_FLOW_TASK_CODE',
        description:
          'SUBMITTED 负向证明必须且只能选择一种输入：已提交待审批执行记录 ID，或可由脚本通过真实 UI 创建并提交的 fresh DRAFT 工单/任务上下文。'
      }
    ]
  }

  if (submittedExistingId && hasAnyFreshContext) {
    return [
      {
        key: 'EDHR_E2E_SUBMITTED_EXECUTION_ID / EDHR_E2E_SUBMITTED_FLOW_WORK_ORDER_CODE + EDHR_E2E_SUBMITTED_FLOW_TASK_CODE',
        description:
          '两种模式互斥，不能同时提供；existing-submitted 模式不得提交或改变该记录，draft-submit 模式会通过真实 UI 创建并提交 fresh DRAFT。',
        invalidConfig: true
      }
    ]
  }

  if (!submittedExistingId && (!hasFreshWorkOrder || !hasFreshTask)) {
    const missing = []
    if (!hasFreshWorkOrder) {
      missing.push({
        key: SUBMITTED_FRESH_CONTEXT_DEFINITION.workOrderEnv,
        description: 'SUBMITTED draft-submit 模式必须提供真实生产工单编码。'
      })
    }
    if (!hasFreshTask) {
      missing.push({
        key: SUBMITTED_FRESH_CONTEXT_DEFINITION.taskEnv,
        description: 'SUBMITTED draft-submit 模式必须提供真实生产任务编号。'
      })
    }
    return missing
  }

  return []
}

function collectInvalidConfig(config) {
  const invalid = []
  if (isForbiddenLiveTenant(config.tenant)) {
    invalid.push({
      key: 'EDHR_E2E_TENANT',
      description: '当前值命中 live 芋道源码租户保护名单；真实 E2E 只能修改测试租户数据。'
    })
  }

  const contextValues = new Map()
  for (const context of Object.values(config.freshContexts).filter(Boolean)) {
    const contextKey = `${context.workOrderCode}\u0000${context.taskCode}`
    if (contextValues.has(contextKey)) {
      invalid.push({
        key: `${context.label}(${context.workOrderCode}/${context.taskCode})`,
        description: `fresh DRAFT 上下文必须互不复用；当前与 \`${contextValues.get(contextKey)}\` 使用同一工单/任务，open-or-create 将复用历史记录。`
      })
    } else {
      contextValues.set(contextKey, context.label)
    }
  }

  return invalid
}

function isForbiddenLiveTenant(tenant) {
  const value = String(tenant || '').trim()
  const lowerValue = value.toLowerCase()
  return (
    value.includes('芋道源码') ||
    FORBIDDEN_LIVE_TENANTS.has(value) ||
    FORBIDDEN_LIVE_TENANTS.has(lowerValue)
  )
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      'Missing Playwright runtime. Run `pnpm install` in this workspace so the package.json devDependency `playwright` is installed, then re-run `pnpm e2e:edhr:approval-tracking`.'
    )
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
    '# 前端真实 E2E 子 agent 证据：eDHR 审批追踪关闭闭环',
    '',
    `- 生成时间：${new Date().toISOString()}`,
    `- 前端 worktree：${process.cwd()}`,
    '- 前端启动入口：`http://localhost:8081`（本 worktree `.env.local` 的 `VITE_PORT=8081`；启动命令 `pnpm dev`）',
    '- 真实 E2E 复跑命令：`pnpm e2e:edhr:approval-tracking`',
    '- 静态语法检查命令：`pnpm e2e:edhr:approval-tracking:check`',
    '- 产物目录：`test-results/edhr-approval-tracking/`（截图、trace、result.json 均不提交）',
    `- 当前状态：${result.status}`,
    `- SUBMITTED 负向输入模式：${result.submittedMode || 'unresolved'}`,
    ''
  ]

  lines.push('## BDD')
  lines.push('')
  for (const scenario of BDD_SCENARIOS) lines.push(`- ${scenario}`)
  lines.push('')

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED')
    lines.push('')
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:approval-tracking\` -> FAIL, ${result.reason}`)
    if (result.missing?.length) {
      lines.push(result.invalidConfig ? '- 不满足的真实 E2E 前置条件：' : '- 缺失环境变量：')
      for (const item of result.missing) {
        lines.push(`  - \`${item.key}\`：${item.description}`)
      }
    }
    lines.push(
      '- SUBMITTED 负向输入要求：`EDHR_E2E_SUBMITTED_EXECUTION_ID` 与 `EDHR_E2E_SUBMITTED_FLOW_WORK_ORDER_CODE` + `EDHR_E2E_SUBMITTED_FLOW_TASK_CODE` 必须且只能提供一种；前者只读验证已提交记录，后者由脚本通过真实 UI 创建 fresh DRAFT 后提交。'
    )
    lines.push('- fresh DRAFT 要求：DRAFT、approve、reject 以及 draft-submit 模式的 SUBMITTED 上下文必须使用测试租户真实工单/任务；批次入口必须返回真实批次，工序 `task/open` 后必须返回真实 executionId 并进入执行详情。')
    lines.push('- 影响：无法登录真实测试租户、无法创建 fresh DRAFT、无法确认审批列表 BPM 待办、签名授权、字段审计链或业务可见执行编号，也无法生成截图、trace 或 UI 闭环证据；未使用 mock、API 替代或静默跳过。')
    lines.push('')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN')
    lines.push('')
    lines.push('- GREEN: `pnpm e2e:edhr:approval-tracking` -> PASS, 真实 UI 闭环已完成。')
    lines.push(`- SUBMITTED 负向输入模式：${result.submittedMode}`)
    for (const step of result.steps || []) {
      const createdEvidence = step.createdExecution
        ? `, batchExecutionId=${step.createdExecution.batchExecutionId || 'n/a'}, batchExecutionCode=${step.createdExecution.batchExecutionCode || 'n/a'}, taskId=${step.createdExecution.taskId || 'n/a'}, executionId=${step.createdExecution.executionId}, executionCode=${step.createdExecution.executionCode}, workOrderCode=${step.createdExecution.workOrderCode}, taskCode=${step.createdExecution.taskCode}`
        : ''
      const fieldAuditEvidence = step.fieldAudit
        ? `, auditBatchId=${step.fieldAudit.auditBatchId}, fieldAuditRevision=${step.fieldAudit.fieldAuditRevision}, fieldAuditHeadHash=${step.fieldAudit.fieldAuditHeadHash}, cellValuesHash=${step.fieldAudit.cellValuesHash}`
        : ''
      const archiveEvidence = step.archive
        ? `, archive id=${step.archive.id}, sha256=${step.archive.sha256}, signatureHash=${step.archive.signatureHash}, approvalSnapshotId=${step.archive.approvalSnapshotId}, approvalSnapshotHash=${step.archive.approvalSnapshotHash}${step.archive.downloadedSha256 ? `, downloadedSha256=${step.archive.downloadedSha256}` : ''}${step.archive.downloadedFilePath ? `, downloadedFilePath=${step.archive.downloadedFilePath}` : ''}`
        : ''
      lines.push(`- ${step.name} -> PASS${step.screenshot ? `, screenshot: \`${step.screenshot}\`` : ''}${createdEvidence}${fieldAuditEvidence}${archiveEvidence}`)
    }
    lines.push(`- Trace: \`${result.trace}\``)
    lines.push('')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED')
    lines.push('')
    lines.push(`- RED: \`pnpm e2e:edhr:approval-tracking\` -> FAIL, ${result.error?.message || '未知错误'}`)
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

async function findVisibleEnabledButton(pageOrLocator, namePattern) {
  const buttons = pageOrLocator.getByRole('button', { name: namePattern })
  const count = await buttons.count()
  for (let index = 0; index < count; index += 1) {
    const button = buttons.nth(index)
    if ((await button.isVisible()) && (await button.isEnabled())) {
      return button
    }
  }
  return null
}

async function clickOptionalVisibleButton(pageOrLocator, namePattern) {
  const button = await findVisibleEnabledButton(pageOrLocator, namePattern)
  if (!button) return false
  await button.click()
  return true
}

async function clickVisibleButton(pageOrLocator, namePattern, failureMessage) {
  if (await clickOptionalVisibleButton(pageOrLocator, namePattern)) {
    return
  }
  throw new Error(failureMessage)
}

async function fillFirstVisible(locator, value, failureMessage) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(failureMessage)
}

async function waitForText(page, textOrPattern, failureMessage) {
  const locator = page.getByText(textOrPattern).first()
  try {
    await locator.waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    throw new Error(`${failureMessage}: ${error.message}`)
  }
}

function waitForResponseResult(page, matcher, label, timeout = 60000) {
  return page.waitForResponse(matcher, { timeout }).then(
    (response) => ({ response }),
    (error) => ({ error: new Error(`${label} API 未返回：${error.message}`) })
  )
}

function requireResponseResult(result) {
  if (result.error) throw result.error
  return result.response
}

async function assertTextContentContains(locator, expectedText, failureMessage) {
  const text = (await locator.textContent({ timeout: 30000 })) || ''
  assert.ok(
    text.includes(expectedText),
    `${failureMessage}。期望包含：${expectedText}；实际文本片段：${text.slice(0, 1000)}`
  )
}

async function assertLocatorTextMatches(locator, expectedPattern, failureMessage) {
  const text = (await locator.textContent({ timeout: 30000 })) || ''
  assert.match(text, expectedPattern, `${failureMessage}。实际文本片段：${text.slice(0, 1000)}`)
}

async function login(page, config, account, label) {
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
    account.username,
    `${label} 登录页缺少用户名输入框。`
  )
  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入密码"]'),
    account.password,
    `${label} 登录页缺少密码输入框。`
  )

  await clickVisibleButton(loginForm, /^登录$/, `${label} 登录页缺少登录按钮。`)
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })
  const currentUrl = new URL(page.url())
  assert.equal(currentUrl.pathname, '/index', `${label} 登录后未进入首页 /index。`)

  if (config.expectedLoginName) {
    await waitForText(
      page,
      config.expectedLoginName,
      `${label} 登录后页面未展示预期登录标识 ${config.expectedLoginName}`
    )
  }
}

async function gotoPath(page, config, routePath) {
  await page.goto(`${config.baseUrl}${routePath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('body').waitFor({ state: 'visible', timeout: 30000 })
}

async function waitForApiResponse(page, endpoint, expectedParams, label) {
  try {
    const response = await page.waitForResponse(
      (candidate) => {
        if (!candidate.url().includes(endpoint)) return false
        const url = new URL(candidate.url())
        return Object.entries(expectedParams).every(([key, value]) => {
          const actual = url.searchParams.get(key)
          return actual === String(value)
        })
      },
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

async function readApiData(response, label) {
  const payload = await response.json()
  if (!payload || typeof payload !== 'object') {
    throw new Error(`${label} API 响应不是 JSON 对象。`)
  }
  assert.equal(
    payload.code,
    0,
    `${label} API 响应 code 应为 0，实际 ${payload.code}，msg=${payload.msg || payload.message || ''}`
  )
  if (payload.data && typeof payload.data === 'object') {
    return payload.data
  }
  throw new Error(`${label} API 响应缺少 data 对象，无法提取证据。`)
}

async function readApiBoolean(response, label) {
  const payload = await response.json()
  if (!payload || typeof payload !== 'object') {
    throw new Error(`${label} API 响应不是 JSON 对象。`)
  }
  assert.equal(
    payload.code,
    0,
    `${label} API 响应 code 应为 0，实际 ${payload.code}，msg=${payload.msg || payload.message || ''}`
  )
  assert.equal(payload.data, true, `${label} API 响应 data 应为 true，实际 ${payload.data}。`)
  return true
}

function assertNonBlankEvidence(value, label) {
  assert.equal(typeof value, 'string', `${label} 应为字符串。`)
  assert.ok(value.trim().length > 0, `${label} 不能为空。`)
}

function assertArchiveEvidence(archive, label) {
  assert.ok(archive && typeof archive === 'object', `${label} 归档响应缺少对象数据。`)
  assert.ok(Number.isFinite(Number(archive.id)) && Number(archive.id) > 0, `${label} 缺少归档 id。`)
  assertNonBlankEvidence(archive.sha256, `${label} sha256`)
  assertNonBlankEvidence(archive.signatureHash, `${label} signatureHash`)
  assert.ok(
    Number.isFinite(Number(archive.approvalSnapshotId)) && Number(archive.approvalSnapshotId) > 0,
    `${label} approvalSnapshotId 缺失或无效。`
  )
  assertNonBlankEvidence(archive.approvalSnapshotHash, `${label} approvalSnapshotHash`)
  return {
    id: Number(archive.id),
    fileName: archive.fileName,
    artifactType: archive.artifactType,
    contentType: archive.contentType,
    sha256: archive.sha256,
    signatureHash: archive.signatureHash,
    approvalSnapshotId: Number(archive.approvalSnapshotId),
    approvalSnapshotHash: archive.approvalSnapshotHash
  }
}

function assertBatchOpenEvidence(openOrCreateData, context) {
  assert.ok(openOrCreateData && typeof openOrCreateData === 'object', `${context.label} 批次 open-or-create 响应缺少对象数据。`)
  assert.ok(
    Number.isFinite(Number(openOrCreateData.id)) && Number(openOrCreateData.id) > 0,
    `${context.label} 批次 open-or-create 未返回有效 batchExecutionId。`
  )
  assertNonBlankEvidence(openOrCreateData.batchExecutionCode, `${context.label} batchExecutionCode`)
  return {
    label: context.label,
    batchExecutionId: Number(openOrCreateData.id),
    batchExecutionCode: openOrCreateData.batchExecutionCode,
    batchCode: openOrCreateData.batchCode,
    workOrderCode: context.workOrderCode,
    taskCode: context.taskCode
  }
}

function assertTaskOpenEvidence(taskOpenData, context, batchEvidence) {
  assert.ok(taskOpenData && typeof taskOpenData === 'object', `${context.label} 工序 task/open 响应缺少对象数据。`)
  assert.ok(
    Number.isFinite(Number(taskOpenData.executionId)) && Number(taskOpenData.executionId) > 0,
    `${context.label} 工序 task/open 未返回有效 executionId。`
  )
  assert.ok(
    Number.isFinite(Number(taskOpenData.taskId)) && Number(taskOpenData.taskId) > 0,
    `${context.label} 工序 task/open 未返回有效 taskId。`
  )
  return {
    ...batchEvidence,
    taskId: Number(taskOpenData.taskId),
    executionId: Number(taskOpenData.executionId),
    workTaskId: taskOpenData.workTaskId ? Number(taskOpenData.workTaskId) : undefined,
    routeProcessId: taskOpenData.routeProcessId ? Number(taskOpenData.routeProcessId) : undefined,
    status: taskOpenData.status,
    executionPageQuery: taskOpenData.executionPageQuery || {}
  }
}

async function resolveExecutionCodeFromPage(page, label) {
  const bodyText = await page.locator('body').textContent({ timeout: 30000 })
  const match = (bodyText || '').match(/BRE\d+/)
  assert.ok(match, `${label} 执行详情页未展示 executionCode，无法形成后续审批/追踪证据。`)
  return match[0]
}

function assertHashVerificationValid(container, label) {
  const status = container?.hashVerification?.status
  assert.equal(status, 'VALID', `${label} hashVerification.status 应为 VALID，实际 ${status || 'missing'}。`)
}

function assertFieldAuditEvidence(data, label) {
  assert.ok(data && typeof data === 'object', `${label} 响应缺少对象数据。`)
  assertHashVerificationValid(data, label)
  assertNonBlankEvidence(String(data.auditBatchId || ''), `${label} auditBatchId`)
  assert.ok(
    Number.isFinite(Number(data.fieldAuditRevision)) && Number(data.fieldAuditRevision) >= 0,
    `${label} fieldAuditRevision 缺失或无效。`
  )
  assertNonBlankEvidence(data.fieldAuditHeadHash, `${label} fieldAuditHeadHash`)
  assertNonBlankEvidence(data.cellValuesHash, `${label} cellValuesHash`)
  return {
    auditBatchId: String(data.auditBatchId),
    fieldAuditRevision: Number(data.fieldAuditRevision),
    fieldAuditHeadHash: data.fieldAuditHeadHash,
    cellValuesHash: data.cellValuesHash,
    hashVerificationStatus: data.hashVerification.status
  }
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function sanitizeFileName(fileName) {
  return fileName.replace(/[<>:"/\\|?*\x00-\x1F]/g, '_')
}

function watchArchiveGenerateRequests(page) {
  const requests = []
  page.on('request', (request) => {
    if (
      request.method() === 'POST' &&
      request.url().includes('/mes/pro/batch-record-execution-archive/generate')
    ) {
      requests.push({
        url: request.url(),
        postData: request.postData()
      })
    }
  })
  return requests
}

async function assertNoArchiveRequest(requests, startIndex, page, label) {
  await page.waitForTimeout(1000)
  const unexpected = requests.slice(startIndex)
  assert.equal(
    unexpected.length,
    0,
    `${label} 不应发起归档生成接口，实际请求 ${unexpected.length} 次：${JSON.stringify(unexpected)}`
  )
}

async function assertNoVisibleArchiveAction(page, label) {
  const archiveButtons = page.getByRole('button', { name: /生成归档|重新生成归档/ })
  const visible = await visibleCount(archiveButtons)
  assert.equal(visible, 0, `${label} 不应展示可点击归档动作。`)
}

async function selectWorkOrderByCode(page, workOrderCode) {
  const feedbackDialog = page.locator('.el-dialog:visible').filter({ hasText: '生产工单' }).last()
  await feedbackDialog
    .locator('.el-form-item')
    .filter({ hasText: '生产工单' })
    .locator('.el-input')
    .first()
    .click()
  const selectorDialog = page.locator('.el-dialog:visible').filter({ hasText: '生产工单选择' }).last()
  await selectorDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(
    selectorDialog.getByPlaceholder('请输入工单编码'),
    workOrderCode,
    '生产工单选择弹框缺少工单编码输入框。'
  )
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/work-order/page') && response.status() === 200,
    { timeout: 30000 }
  )
  await clickVisibleButton(selectorDialog, /搜索|查询/, '生产工单选择弹框缺少搜索按钮。')
  await responsePromise
  const row = selectorDialog.locator('.el-table__body-wrapper tbody tr').filter({ hasText: workOrderCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.click()
  await clickVisibleButton(selectorDialog, /确\s*定/, '生产工单选择弹框缺少确定按钮。')
  await selectorDialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function selectTaskByCode(page, taskCode) {
  const feedbackDialog = page.locator('.el-dialog:visible').filter({ hasText: '生产任务' }).last()
  await feedbackDialog
    .locator('.el-form-item')
    .filter({ hasText: '生产任务' })
    .locator('.el-input')
    .first()
    .click()
  const selectorDialog = page.locator('.el-dialog:visible').filter({ hasText: '生产任务选择' }).last()
  await selectorDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(
    selectorDialog.getByPlaceholder('请输入任务编号'),
    taskCode,
    '生产任务选择弹框缺少任务编号输入框。'
  )
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/task/page') && response.status() === 200,
    { timeout: 30000 }
  )
  await clickVisibleButton(selectorDialog, /搜索|查询/, '生产任务选择弹框缺少搜索按钮。')
  await responsePromise
  const row = selectorDialog.locator('.el-table__body-wrapper tbody tr').filter({ hasText: taskCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.click()
  await clickVisibleButton(selectorDialog, /确\s*定/, '生产任务选择弹框缺少确定按钮。')
  await selectorDialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function skipOneSpecialNodeViaUi(page, config, label, attemptIndex) {
  const skipButton = await findVisibleEnabledButton(page, /^跳过$/)
  if (!skipButton) return false
  const skipResponsePromise = waitForResponseResult(
    page,
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/mes/pro/edhr-batch-execution/task/special-node/skip'),
    `${label} 特殊节点跳过`
  )
  await skipButton.click()

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '跳过特殊节点' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(
    dialog.locator('textarea'),
    `${label} 真实 E2E 前置特殊节点跳过 ${attemptIndex} ${new Date().toISOString()}`,
    `${label} 特殊节点跳过弹框缺少原因输入框。`
  )
  await fillFirstVisible(
    dialog.locator('input[type="password"]'),
    config.executor.signaturePassword,
    `${label} 特殊节点跳过弹框缺少签名密码输入框。`
  )
  await clickVisibleButton(dialog, /签名并跳过/, `${label} 特殊节点跳过弹框缺少“签名并跳过”按钮。`)
  const skipResponse = requireResponseResult(await skipResponsePromise)
  assert.equal(skipResponse.status(), 200, `${label} 特殊节点跳过 HTTP 状态应为 200，实际 ${skipResponse.status()}`)
  await readApiData(skipResponse, `${label} 特殊节点跳过`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => null)
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => null)
  return true
}

async function ensureBatchHasOpenableRouteTask(page, config, label) {
  for (let attempt = 1; attempt <= 8; attempt += 1) {
    if (await findVisibleEnabledButton(page, /打开填写|打开返工/)) {
      return
    }
    if (!(await skipOneSpecialNodeViaUi(page, config, label, attempt))) {
      const bodyText = ((await page.locator('body').textContent({ timeout: 30000 })) || '').replace(/\s+/g, ' ').trim()
      throw new Error(
        `${label} 批次详情页缺少可打开的工序任务，且没有可跳过特殊节点。页面片段：${bodyText.slice(0, 1000)}`
      )
    }
  }
  throw new Error(`${label} 批次详情页连续处理特殊节点后仍没有可打开的工序任务。`)
}

async function createFreshDraftFromFeedbackUi(page, config, context, stepName, steps) {
  await gotoPath(page, config, '/mes/pro/feedback')
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => null)
  await clickVisibleButton(page, /新增|添加/, '生产报工页面缺少“新增”按钮，无法从真实入口创建 fresh DRAFT。')
  const feedbackDialog = page.locator('.el-dialog:visible').filter({ hasText: '打开 eDHR' }).last()
  await feedbackDialog.waitFor({ state: 'visible', timeout: 30000 })
  await selectWorkOrderByCode(page, context.workOrderCode)
  await selectTaskByCode(page, context.taskCode)

  const batchResponsePromise = waitForResponseResult(
    page,
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/mes/pro/edhr-batch-execution/open-or-create'),
    `${context.label} 批次 open-or-create`
  )
  await clickVisibleButton(feedbackDialog, /打开\s*eDHR/, '生产报工弹框缺少“打开 eDHR”按钮。')
  const batchResponse = requireResponseResult(await batchResponsePromise)
  assert.equal(batchResponse.status(), 200, `${context.label} 批次 open-or-create HTTP 状态应为 200，实际 ${batchResponse.status()}`)
  const batchEvidence = assertBatchOpenEvidence(
    await readApiData(batchResponse, `${context.label} 批次 open-or-create`),
    context
  )

  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-batch-execution/detail'), {
    timeout: 60000
  })
  const batchUrl = new URL(page.url())
  assert.equal(
    Number(batchUrl.searchParams.get('id')),
    batchEvidence.batchExecutionId,
    `${context.label} 批次详情 URL batchExecutionId 与 open-or-create 响应不一致。`
  )
  await waitForText(page, 'eDHR 批次执行详情', `${context.label} 未进入 eDHR 批次执行详情页`)
  await waitForText(page, /批次总控|主流程动作/, `${context.label} 批次详情页未展示主流程总控区`)
  await ensureBatchHasOpenableRouteTask(page, config, context.label)

  const taskOpenResponsePromise = waitForResponseResult(
    page,
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/mes/pro/edhr-batch-execution/task/open'),
    `${context.label} 工序 task/open`
  )
  await clickVisibleButton(page, /打开填写|打开返工/, `${context.label} 批次详情页缺少可打开的工序任务。`)
  const taskOpenResponse = requireResponseResult(await taskOpenResponsePromise)
  assert.equal(
    taskOpenResponse.status(),
    200,
    `${context.label} 工序 task/open HTTP 状态应为 200，实际 ${taskOpenResponse.status()}`
  )
  const createdExecution = assertTaskOpenEvidence(
    await readApiData(taskOpenResponse, `${context.label} 工序 task/open`),
    context,
    batchEvidence
  )

  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-execution/form'), {
    timeout: 60000
  })
  const url = new URL(page.url())
  assert.equal(
    Number(url.searchParams.get('id')),
    createdExecution.executionId,
    `${context.label} 执行详情 URL executionId 与 task/open 响应不一致。`
  )
  await waitForText(page, 'eDHR 执行详情', `${context.label} 未进入 eDHR 执行详情页`)
  await waitForText(page, /草稿（可编辑）|草稿/, `${context.label} fresh DRAFT 未展示草稿状态`)
  createdExecution.executionCode = await resolveExecutionCodeFromPage(page, context.label)
  const shot = await screenshot(page, `${stepName}-fresh-draft-created`, steps)
  steps.push({
    name: `${context.label} 批次入口与工序草稿创建`,
    screenshot: shot,
    createdExecution
  })
  return createdExecution
}

async function openExecutionDetail(page, config, executionId) {
  await gotoPath(page, config, `/mes/pro/feedback/edhr-execution/detail?id=${executionId}`)
  await waitForText(page, 'eDHR 执行详情', '未进入 eDHR 执行详情页')
}

async function scenarioDraftArchiveBlocked(page, config, archiveRequests, draftFlow, steps) {
  const startIndex = archiveRequests.length
  await openExecutionDetail(page, config, draftFlow.executionId)
  await waitForText(page, /草稿（可编辑）|草稿/, 'DRAFT 执行记录未展示草稿状态')
  await assertNoVisibleArchiveAction(page, 'DRAFT 状态')
  await assertNoArchiveRequest(archiveRequests, startIndex, page, 'DRAFT 状态')
  const shot = await screenshot(page, 'draft-archive-blocked', steps)
  steps.push({ name: 'DRAFT 禁止归档 UI 负向', screenshot: shot })
}

async function scenarioSubmittedArchiveBlocked(page, config, archiveRequests, executionId, steps) {
  const startIndex = archiveRequests.length
  await openExecutionDetail(page, config, executionId)
  await waitForText(page, /待审批（审批关闭后才可归档）|审批关闭后才可归档/, 'SUBMITTED 执行记录未展示审批关闭后才可归档提示')
  await assertNoVisibleArchiveAction(page, 'SUBMITTED 状态')
  await assertNoArchiveRequest(archiveRequests, startIndex, page, 'SUBMITTED 状态')
  const shot = await screenshot(page, 'submitted-archive-blocked', steps)
  steps.push({ name: 'SUBMITTED 禁止归档 UI 负向', screenshot: shot })
}

async function submitDraftByUi(page, config, executionId, stepName, steps) {
  await openExecutionDetail(page, config, executionId)
  await waitForText(page, /草稿（可编辑）|草稿/, `${stepName} 执行记录不是草稿状态`)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/mes/pro/batch-record-execution/submit'),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /提交执行/, '草稿详情页缺少“提交执行”按钮。')
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.executor.signaturePassword, '提交弹框缺少签名密码输入框。')
  await fillFirstVisible(dialog.locator('textarea'), `真实 E2E 提交 ${new Date().toISOString()}`, '提交弹框缺少提交备注输入框。')
  await clickVisibleButton(page, /确\s*认\s*提\s*交/, '提交弹框缺少确认提交按钮。')
  const response = await responsePromise
  assert.equal(response.status(), 200, `提交接口 HTTP 状态应为 200，实际 ${response.status()}`)
  await readApiBoolean(response, '提交接口')
  await waitForText(page, /待审批（审批关闭后才可归档）|待审批/, '提交后页面未展示待审批状态')
  const shot = await screenshot(page, stepName, steps)
  steps.push({ name: `${stepName} 提交后进入审批`, screenshot: shot })
  return { executionId }
}

async function chooseEditableField(page) {
  await page.waitForSelector('.edhr-page-shell__form', { timeout: 30000 })
  const controls = page.locator(
    '.edhr-page-shell__form input:not([disabled]):visible, .edhr-page-shell__form textarea:not([disabled]):visible'
  )
  const count = await controls.count()
  assert.ok(count > 0, 'eDHR 执行页没有可编辑字段控件，无法产生 FIELD_CHANGE。')

  for (let index = 0; index < count; index += 1) {
    const control = controls.nth(index)
    const meta = await control.evaluate((element) => ({
      tag: element.tagName,
      type: element.getAttribute('type') || '',
      readOnly: element.readOnly,
      value: element.value || '',
      placeholder: element.getAttribute('placeholder') || '',
      inDate: Boolean(element.closest('.el-date-editor')),
      inSelect: Boolean(element.closest('.el-select')),
      inNumber: Boolean(element.closest('.el-input-number'))
    }))
    if (meta.readOnly || meta.inDate || meta.inSelect) {
      continue
    }

    const newValue = meta.inNumber ? String((Date.now() % 9000) + 1000) : `FA-E2E-${Date.now()}`
    if (newValue === meta.value) {
      continue
    }
    await control.fill(newValue)
    await control.blur()
    return { index, ...meta, newValue }
  }

  throw new Error('eDHR 执行页只有只读、日期或下拉控件，没有可直接填写并审计的字段。')
}

async function saveFieldAuditByUi(page, config, approveFlow, steps) {
  await openExecutionDetail(page, config, approveFlow.executionId)
  await waitForText(page, /草稿（可编辑）|草稿/, 'FIELD_CHANGE 审计前 approve-flow 执行记录不是草稿状态')
  const editedField = await chooseEditableField(page)
  const reasonText = `字段审计E2E真实用户路径验证 ${new Date().toISOString()}`
  const reasonForm = page.locator('.edhr-page-shell__field-audit-reason')
  await reasonForm.waitFor({ state: 'visible', timeout: 30000 })
  await reasonForm.locator('.el-select').click()
  await page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: '纠正录入' })
    .first()
    .click()
  await fillFirstVisible(
    reasonForm.getByPlaceholder('请输入字段变更原因'),
    reasonText,
    '字段审计原因区域缺少原因说明输入框。'
  )

  const pendingRows = await page
    .locator('.edhr-page-shell__field-audit-table .el-table__body-wrapper tbody tr')
    .count()
  assert.ok(pendingRows > 0, '修改字段后没有生成待保存 FIELD_CHANGE 行。')
  const fieldAuditPanel = page.locator('.edhr-page-shell__field-audit').first()
  await clickVisibleButton(fieldAuditPanel, /^保存变更$/, '字段审计区域缺少“保存变更”按钮。')

  const signatureDialog = page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).last()
  await signatureDialog.waitFor({ state: 'visible', timeout: 10000 })
  await waitForText(signatureDialog, 'FIELD_CHANGE', 'FIELD_CHANGE 签名弹框未展示签名动作')
  await fillFirstVisible(
    signatureDialog.locator('input[type="password"]'),
    config.fieldChangeSignaturePassword,
    'FIELD_CHANGE 签名弹框缺少签名密码输入框。'
  )
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/mes/pro/batch-record-execution/field-audit/save-changes'),
    { timeout: 60000 }
  )
  await clickVisibleButton(signatureDialog, /确\s*认\s*保\s*存/, 'FIELD_CHANGE 签名弹框缺少确认保存按钮。')
  const saveResponse = await saveResponsePromise
  assert.equal(saveResponse.status(), 200, `FIELD_CHANGE save-changes HTTP 状态应为 200，实际 ${saveResponse.status()}`)
  const fieldAudit = assertFieldAuditEvidence(
    await readApiData(saveResponse, 'FIELD_CHANGE save-changes'),
    'FIELD_CHANGE save-changes'
  )
  await waitForText(page, '字段变更已写入不可篡改审计链', 'FIELD_CHANGE 保存后页面未展示成功提示')
  const shot = await screenshot(page, 'approve-flow-field-change-saved', steps)
  steps.push({
    name: 'approve-flow FIELD_CHANGE 字段审计保存',
    screenshot: shot,
    fieldAudit,
    editedField
  })
  return fieldAudit
}

async function verifyFieldAuditChainAndDetail(page, config, approveFlow, fieldAudit, steps) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-execution/field-audit/page') &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await gotoPath(
    page,
    config,
    `/mes/pro/feedback/edhr-field-audit?executionId=${approveFlow.executionId}`
  )
  await pageResponsePromise
  const auditTable = page.locator('.edhr-field-audit__table').first()
  await auditTable.waitFor({ state: 'visible', timeout: 30000 })
  await auditTable.getByText('FIELD_CHANGE').first().waitFor({ state: 'visible', timeout: 30000 })
  await auditTable.getByText('校验通过').first().waitFor({ state: 'visible', timeout: 30000 })

  const verifyResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/mes/pro/batch-record-execution/field-audit/verify-chain'),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /校验当前筛选结果/, '字段审计列表缺少“校验当前筛选结果”按钮。')
  const verifyResponse = await verifyResponsePromise
  assert.equal(verifyResponse.status(), 200, `FIELD_CHANGE verify-chain HTTP 状态应为 200，实际 ${verifyResponse.status()}`)
  const verifyData = await readApiData(verifyResponse, 'FIELD_CHANGE verify-chain')
  assertHashVerificationValid(verifyData, 'FIELD_CHANGE verify-chain')
  assertNonBlankEvidence(verifyData.fieldAuditHeadHash, 'FIELD_CHANGE verify-chain fieldAuditHeadHash')
  assertNonBlankEvidence(verifyData.cellValuesHash, 'FIELD_CHANGE verify-chain cellValuesHash')
  await waitForText(page, '字段审计链校验通过', 'FIELD_CHANGE verify-chain 后页面未展示校验通过提示')

  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-execution/field-audit/detail') &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await auditTable.getByRole('button', { name: '详情' }).first().click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-field-audit/detail'), {
    timeout: 30000
  })
  const detailResponse = await detailResponsePromise
  const detailData = await readApiData(detailResponse, 'FIELD_CHANGE detail')
  assertHashVerificationValid(detailData, 'FIELD_CHANGE detail')
  assert.equal(
    String(detailData.auditBatch?.id || ''),
    fieldAudit.auditBatchId,
    'FIELD_CHANGE detail auditBatchId 与 save-changes 响应不一致。'
  )
  await page
    .locator('.edhr-field-audit-detail__title')
    .filter({ hasText: '字段审计详情' })
    .waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText('FIELD_CHANGE').first().waitFor({ state: 'visible', timeout: 30000 })
  const shot = await screenshot(page, 'approve-flow-field-change-verified', steps)
  steps.push({
    name: 'approve-flow FIELD_CHANGE verify-chain 与详情',
    screenshot: shot,
    fieldAudit: {
      ...fieldAudit,
      verifyFieldAuditRevision: verifyData.fieldAuditRevision,
      verifyFieldAuditHeadHash: verifyData.fieldAuditHeadHash,
      verifyCellValuesHash: verifyData.cellValuesHash
    }
  })
}

async function openPendingApprovalList(page, config, executionCode) {
  await gotoPath(
    page,
    config,
    `/mes/pro/feedback/edhr-approval?tab=pending&executionCode=${encodeURIComponent(executionCode)}`
  )
  await waitForText(page, /待我审批/, '未进入 eDHR 待我审批列表')
  await fillFirstVisible(
    page.locator('.edhr-workbench__toolbar .el-form-item').filter({ hasText: '执行编号' }).locator('input'),
    executionCode,
    '审批列表缺少执行编号筛选输入框。'
  )
  await clickVisibleButton(page, /^查询$/, '审批列表缺少查询按钮。')
  const row = page.locator('.edhr-workbench__table .el-table__body-wrapper tbody tr').filter({
    hasText: executionCode
  }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await waitForText(page, executionCode, `审批列表未查询到执行编号 ${executionCode}`)
  await waitForText(page, /待审批/, `审批列表未展示 ${executionCode} 的待审批状态`)
  return row
}

async function assertApprovalDetailFromPendingList(page, config, executionCode, steps) {
  const row = await openPendingApprovalList(page, config, executionCode)
  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes('/mes/pro/batch-record-execution/approval-detail'),
    { timeout: 60000 }
  )
  const executionDetailLink = row.locator('.edhr-workbench__execution-link').filter({ hasText: executionCode }).first()
  await executionDetailLink.waitFor({ state: 'visible', timeout: 30000 })
  await executionDetailLink.click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-approval/detail'), {
    timeout: 30000
  })
  const detailResponse = await detailResponsePromise
  assert.equal(detailResponse.status(), 200, `审批详情接口 HTTP 状态应为 200，实际 ${detailResponse.status()}`)
  const detailData = await readApiData(detailResponse, '审批详情接口')
  assert.equal(
    detailData.executionCode,
    executionCode,
    `审批详情 executionCode 应为 ${executionCode}，实际 ${detailData.executionCode}`
  )
  assert.ok(detailData.bpmTaskId, '审批详情缺少 bpmTaskId。')
  assert.ok(detailData.approvalSnapshotId, '审批详情缺少 approvalSnapshotId。')
  assert.ok(detailData.approvalSnapshotHash, '审批详情缺少 approvalSnapshotHash。')
  await page
    .locator('.edhr-detail__title')
    .filter({ hasText: 'eDHR 审批详情' })
    .waitFor({ state: 'visible', timeout: 30000 })
  await waitForText(page, executionCode, `审批详情页未展示执行编号 ${executionCode}`)
  const shot = await screenshot(page, 'approval-detail-real-api', steps)
  steps.push({ name: '审批详情真实 API 展示', screenshot: shot })
}

async function approveFromPendingList(page, config, executionId, executionCode, steps) {
  const row = await openPendingApprovalList(page, config, executionCode)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/mes/pro/batch-record-execution/approve'),
    { timeout: 60000 }
  )
  await row.getByRole('button', { name: /^通过$/ }).click()
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.approver.signaturePassword, '通过弹框缺少签名密码输入框。')
  await fillFirstVisible(dialog.locator('textarea'), `真实 E2E 审批通过 ${new Date().toISOString()}`, '通过弹框缺少审批意见输入框。')
  await clickVisibleButton(page, /确\s*认$/, '通过弹框缺少确认按钮。')
  const response = await responsePromise
  assert.equal(response.status(), 200, `审批通过接口 HTTP 状态应为 200，实际 ${response.status()}`)
  await readApiData(response, '审批通过接口')
  await openExecutionDetail(page, config, executionId)
  await waitForText(page, /已关闭/, '审批通过后页面未展示已关闭状态')
  const shot = await screenshot(page, 'approve-closes-execution', steps)
  steps.push({ name: '审批通过关闭', screenshot: shot })
  return { executionId, executionCode }
}

async function rejectFromPendingList(page, config, executionId, executionCode, steps) {
  const row = await openPendingApprovalList(page, config, executionCode)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/mes/pro/batch-record-execution/reject'),
    { timeout: 60000 }
  )
  await row.getByRole('button', { name: /^驳回$/ }).click()
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.approver.signaturePassword, '驳回弹框缺少签名密码输入框。')
  const textareas = dialog.locator('textarea')
  await fillFirstVisible(textareas.nth(0), config.expectedRejectReason, '驳回弹框缺少驳回原因输入框。')
  if ((await textareas.count()) > 1) {
    await textareas.nth(1).fill(`真实 E2E 驳回留痕 ${new Date().toISOString()}`)
  }
  await clickVisibleButton(page, /确\s*认$/, '驳回弹框缺少确认按钮。')
  const response = await responsePromise
  assert.equal(response.status(), 200, `审批驳回接口 HTTP 状态应为 200，实际 ${response.status()}`)
  await readApiData(response, '审批驳回接口')
  await openExecutionDetail(page, config, executionId)
  await waitForText(page, /已驳回/, '驳回后页面未展示已驳回状态')
  const shot = await screenshot(page, 'reject-keeps-trace', steps)
  steps.push({ name: '审批驳回留痕', screenshot: shot })
  return { executionId, executionCode }
}

async function assertDoneApprovalList(page, config, flow, expectedStatusPattern, steps) {
  await gotoPath(
    page,
    config,
    `/mes/pro/feedback/edhr-approval?tab=done&executionCode=${encodeURIComponent(flow.executionCode)}`
  )
  await waitForText(page, /我已审批/, '未进入 eDHR 我已审批列表')
  await fillFirstVisible(
    page.locator('.edhr-workbench__toolbar .el-form-item').filter({ hasText: '执行编号' }).locator('input'),
    flow.executionCode,
    '我已审批列表缺少执行编号筛选输入框。'
  )
  const responsePromise = page.waitForResponse(
    (response) => {
      if (response.request().method() !== 'GET') return false
      if (!response.url().includes('/mes/pro/batch-record-execution/approval-done-page')) return false
      const url = new URL(response.url())
      return url.searchParams.get('executionCode') === flow.executionCode
    },
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /^查询$/, '我已审批列表缺少查询按钮。')
  const response = await responsePromise
  assert.equal(response.status(), 200, `我已审批分页接口 HTTP 状态应为 200，实际 ${response.status()}`)
  const pageData = await readApiData(response, '我已审批分页接口')
  const rows = Array.isArray(pageData.list) ? pageData.list : []
  assert.ok(
    rows.some((row) => String(row.executionCode) === String(flow.executionCode)),
    `我已审批分页 rows 未包含执行编号 ${flow.executionCode}。`
  )
  const row = page.locator('.edhr-workbench__table .el-table__body-wrapper tbody tr').filter({
    hasText: flow.executionCode
  }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await assertLocatorTextMatches(row, expectedStatusPattern, `我已审批列表未展示 ${flow.executionCode} 的审批后状态`)
  const shot = await screenshot(page, `done-approval-${flow.executionCode}`, steps)
  steps.push({ name: `我已审批列表 ${flow.executionCode}`, screenshot: shot })
}

async function scenarioArchiveApproved(page, config, approvedFlow, steps) {
  await openExecutionDetail(page, config, approvedFlow.executionId)
  await waitForText(page, /已关闭（可按门槛归档）|已关闭/, '归档场景执行记录不是已关闭状态')
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/mes/pro/batch-record-execution-archive/generate'),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /生成归档|重新生成归档/, '已关闭详情页缺少“生成归档/重新生成归档”按钮。')
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.archiveSignaturePassword, '归档弹框缺少封存密码输入框。')
  await fillFirstVisible(dialog.locator('textarea'), `真实 E2E 归档 ${new Date().toISOString()}`, '归档弹框缺少备注输入框。')
  await clickVisibleButton(page, /确\s*认\s*生\s*成/, '归档弹框缺少确认生成按钮。')
  const response = await responsePromise
  assert.equal(response.status(), 200, `归档生成接口 HTTP 状态应为 200，实际 ${response.status()}`)
  const archiveEvidence = assertArchiveEvidence(await readApiData(response, '归档生成'), '归档生成')
  await waitForText(page, /归档生成成功|已返回现有归档版本|已封存|SEALED/, '归档后页面未展示成功或封存证据')
  const shot = await screenshot(page, 'approved-archive-generated', steps)
  steps.push({ name: '关闭后可归档', screenshot: shot, archive: archiveEvidence })
  return archiveEvidence
}

async function assertArchiveVersionsDialog(page, approvedFlow, archiveEvidence, steps) {
  const responsePromise = page.waitForResponse(
    (response) => {
      if (response.request().method() !== 'GET') return false
      if (!response.url().includes('/mes/pro/batch-record-execution-archive/page')) return false
      const url = new URL(response.url())
      return url.searchParams.get('executionId') === String(approvedFlow.executionId)
    },
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /查看版本/, '执行详情页缺少“查看版本”按钮。')
  const response = await responsePromise
  assert.equal(response.status(), 200, `归档版本分页接口 HTTP 状态应为 200，实际 ${response.status()}`)
  const pageData = await readApiData(response, '归档版本分页接口')
  const rows = Array.isArray(pageData.list) ? pageData.list : []
  assert.ok(
    rows.some((row) => String(row.id) === String(archiveEvidence.id) || String(row.sha256) === String(archiveEvidence.sha256)),
    `归档版本分页 rows 未包含本轮归档 id=${archiveEvidence.id} 或 sha256=${archiveEvidence.sha256}。`
  )
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '归档版本' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await assertLocatorTextMatches(dialog, /归档版本/, '归档版本弹窗未展示版本列')
  await assertTextContentContains(dialog, String(archiveEvidence.sha256), '归档版本弹窗未展示本轮归档 sha256')
  const shot = await screenshot(page, 'archive-versions-dialog', steps)
  steps.push({ name: '归档版本可查看', screenshot: shot })
}

async function scenarioDownloadSealedArchive(page, config, approvedFlow, archiveEvidence, steps) {
  await openExecutionDetail(page, config, approvedFlow.executionId)
  await waitForText(page, /已关闭（可按门槛归档）|已关闭/, '下载场景执行记录不是已关闭状态')
  await waitForText(page, archiveEvidence.sha256, '归档摘要未在详情页展示，无法下载前确认证据。')
  const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes(`/mes/pro/batch-record-execution-archive/download?id=${archiveEvidence.id}`),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /下载归档|归档下载/, '已封存归档缺少“下载归档”按钮。')
  const response = await responsePromise
  assert.equal(response.status(), 200, `归档下载接口 HTTP 状态应为 200，实际 ${response.status()}`)
  const download = await downloadPromise
  const failure = await download.failure()
  assert.equal(failure, null, `归档下载失败：${failure}`)
  const downloadPath = await download.path()
  assert.ok(downloadPath, '归档下载未生成本地下载文件。')
  const stat = fs.statSync(downloadPath)
  assert.ok(stat.size > 0, '归档下载文件为空。')
  const suggestedName = download.suggestedFilename()
  assert.ok(suggestedName && suggestedName.trim().length > 0, '归档下载缺少建议文件名。')
  const savedFileName = `${archiveEvidence.id}-${sanitizeFileName(suggestedName)}`
  const savedFilePath = path.join(RESULT_DIR, savedFileName)
  await download.saveAs(savedFilePath)
  const downloadedSha256 = sha256File(savedFilePath)
  assert.equal(
    downloadedSha256,
    archiveEvidence.sha256,
    '归档下载文件 SHA-256 必须等于归档生成响应 sha256。'
  )
  const shot = await screenshot(page, 'approved-archive-downloaded', steps)
  steps.push({
    name: '受控归档下载',
    screenshot: shot,
    archive: {
      ...archiveEvidence,
      downloadedFileName: suggestedName,
      downloadedFilePath: savedFilePath,
      downloadedBytes: stat.size,
      downloadedSha256
    }
  })
}

async function scenarioTrackingAndSignatures(page, config, approvedFlow, rejectedFlow, steps) {
  const approvedTrackingResponse = waitForApiResponse(
    page,
    '/mes/pro/batch-record-execution/tracking-page',
    { executionCode: approvedFlow.executionCode },
    '通过流程追踪分页'
  )
  await gotoPath(
    page,
    config,
    `/mes/pro/feedback/edhr-tracking?executionCode=${encodeURIComponent(approvedFlow.executionCode)}`
  )
  await approvedTrackingResponse
  await waitForText(page, 'eDHR', '追踪页未加载 eDHR 页面内容')
  const trackingTable = page.locator('.edhr-query__table').first()
  await trackingTable.waitFor({ state: 'visible', timeout: 30000 })
  await assertTextContentContains(trackingTable, approvedFlow.executionCode, '追踪表格未展示通过流程执行编号')
  await assertLocatorTextMatches(
    trackingTable,
    /当前节点|当前处理人|最后事件|最后处理时间/,
    '追踪表格未展示 BPM/人员/时间列'
  )

  const approvedSignatureResponse = waitForApiResponse(
    page,
    '/mes/pro/batch-record-execution/signature-page',
    { executionId: approvedFlow.executionId },
    '通过流程签名分页'
  )
  await gotoPath(page, config, `/mes/pro/feedback/edhr-signatures?executionId=${approvedFlow.executionId}`)
  await approvedSignatureResponse
  const signatureTable = page.locator('.edhr-query__table').first()
  await signatureTable.waitFor({ state: 'visible', timeout: 30000 })
  await assertLocatorTextMatches(signatureTable, /签名编号/, '签名表格未加载签名编号列')
  await assertLocatorTextMatches(signatureTable, /签名含义/, '签名表格未展示签名含义列')
  await assertLocatorTextMatches(signatureTable, /流程任务/, '签名表格未展示 BPM 任务列')
  await assertTextContentContains(signatureTable, config.expectedApproverName, '签名表格未展示预期审批人员')
  await assertLocatorTextMatches(signatureTable, /FIELD_CHANGE/, '签名表格未展示 FIELD_CHANGE 字段变更签名动作')
  await assertLocatorTextMatches(signatureTable, /SUBMIT/, '签名表格未展示提交签名动作')
  await assertLocatorTextMatches(signatureTable, /APPROVE/, '签名表格未展示审批通过签名动作')
  await assertLocatorTextMatches(signatureTable, /ARCHIVE_SEAL/, '签名表格未展示归档封存签名动作')

  const rejectedTrackingResponse = waitForApiResponse(
    page,
    '/mes/pro/batch-record-execution/tracking-page',
    { executionCode: rejectedFlow.executionCode },
    '驳回流程追踪分页'
  )
  await gotoPath(
    page,
    config,
    `/mes/pro/feedback/edhr-tracking?executionCode=${encodeURIComponent(rejectedFlow.executionCode)}`
  )
  await rejectedTrackingResponse
  const rejectedTrackingTable = page.locator('.edhr-query__table').first()
  await rejectedTrackingTable.waitFor({ state: 'visible', timeout: 30000 })
  await assertTextContentContains(rejectedTrackingTable, rejectedFlow.executionCode, '追踪表格未展示驳回流程执行编号')
  await assertLocatorTextMatches(rejectedTrackingTable, /REJECT|已驳回/, '追踪表格未展示驳回流程事件')
  await assertTextContentContains(
    rejectedTrackingTable,
    config.expectedRejectReason,
    '追踪表格 DOM 数据未包含驳回原因'
  )

  const rejectedTimelineResponse = waitForApiResponse(
    page,
    '/mes/pro/batch-record-execution/tracking-timeline',
    { executionId: rejectedFlow.executionId },
    '驳回详情追踪时间线'
  )
  await openExecutionDetail(page, config, rejectedFlow.executionId)
  await rejectedTimelineResponse
  const auditTabs = page.locator('.edhr-page-shell__audit-tabs').first()
  await auditTabs.waitFor({ state: 'visible', timeout: 30000 })
  await page.getByRole('tab', { name: '追踪' }).click()
  await assertLocatorTextMatches(auditTabs, /流程任务/, '详情追踪页签未展示 BPM 任务')
  await assertTextContentContains(auditTabs, config.expectedApproverName, '详情追踪页签未展示处理人')
  await assertTextContentContains(
    auditTabs,
    config.expectedRejectReason,
    '详情追踪页签 DOM 数据未包含驳回原因'
  )
  await page.getByRole('tab', { name: '签名记录' }).click()
  await assertLocatorTextMatches(auditTabs, /密码校验/, '详情签名页签未展示密码校验')
  await assertLocatorTextMatches(auditTabs, /REJECT/, '详情签名页签未展示驳回签名动作')
  await assertTextContentContains(
    auditTabs,
    config.expectedRejectReason,
    '详情签名页签 DOM 数据未包含签名原因'
  )
  const shot = await screenshot(page, 'tracking-signature-evidence', steps)
  steps.push({ name: '追踪与签名查询', screenshot: shot })
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
    timezoneId: 'Asia/Shanghai',
    acceptDownloads: true
  })
  const tracePath = path.join(RESULT_DIR, 'trace.zip')
  const page = await context.newPage()
  const steps = []
  const archiveRequests = watchArchiveGenerateRequests(page)

  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  try {
    await login(page, config, config.executor, '执行人')
    const draftFlow = await createFreshDraftFromFeedbackUi(
      page,
      config,
      config.freshContexts.draft,
      'draft-flow',
      steps
    )
    await scenarioDraftArchiveBlocked(page, config, archiveRequests, draftFlow, steps)

    let submittedFlow
    if (config.submittedMode === 'existing-submitted') {
      submittedFlow = { executionId: config.ids.submittedExisting, mode: config.submittedMode }
    } else {
      const submittedDraftFlow = await createFreshDraftFromFeedbackUi(
        page,
        config,
        config.freshContexts.submittedFlow,
        'submitted-flow',
        steps
      )
      await submitDraftByUi(
        page,
        config,
        submittedDraftFlow.executionId,
        'submitted-flow-draft',
        steps
      )
      submittedFlow = { executionId: submittedDraftFlow.executionId, mode: config.submittedMode }
    }
    await scenarioSubmittedArchiveBlocked(page, config, archiveRequests, submittedFlow.executionId, steps)

    const approveDraftFlow = await createFreshDraftFromFeedbackUi(
      page,
      config,
      config.freshContexts.approveFlow,
      'approve-flow',
      steps
    )
    const approveFieldAudit = await saveFieldAuditByUi(page, config, approveDraftFlow, steps)
    await verifyFieldAuditChainAndDetail(page, config, approveDraftFlow, approveFieldAudit, steps)
    await submitDraftByUi(page, config, approveDraftFlow.executionId, 'approve-flow-draft', steps)

    const rejectDraftFlow = await createFreshDraftFromFeedbackUi(
      page,
      config,
      config.freshContexts.rejectFlow,
      'reject-flow',
      steps
    )
    await submitDraftByUi(page, config, rejectDraftFlow.executionId, 'reject-flow-draft', steps)

    await context.clearCookies()
    await page.evaluate(() => {
      window.localStorage.clear()
      window.sessionStorage.clear()
    })
    await login(page, config, config.approver, '审批人')
    await assertApprovalDetailFromPendingList(page, config, approveDraftFlow.executionCode, steps)
    const approvedFlow = await approveFromPendingList(
      page,
      config,
      approveDraftFlow.executionId,
      approveDraftFlow.executionCode,
      steps
    )
    const rejectedFlow = await rejectFromPendingList(
      page,
      config,
      rejectDraftFlow.executionId,
      rejectDraftFlow.executionCode,
      steps
    )
    await assertDoneApprovalList(page, config, approvedFlow, /已关闭/, steps)
    await assertDoneApprovalList(page, config, rejectedFlow, /已驳回/, steps)
    const approvedArchive = await scenarioArchiveApproved(page, config, approvedFlow, steps)
    await assertArchiveVersionsDialog(page, approvedFlow, approvedArchive, steps)
    await scenarioDownloadSealedArchive(page, config, approvedFlow, approvedArchive, steps)
    await scenarioTrackingAndSignatures(page, config, approvedFlow, rejectedFlow, steps)

    await context.tracing.stop({ path: tracePath })
    await browser.close()
    return {
      status: 'PASS',
      submittedMode: config.submittedMode,
      steps,
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
        ? '真实 E2E 前置条件不满足，不能修改 live 租户、复用 fresh DRAFT 工单/任务上下文，或同时提供互斥的 SUBMITTED 输入。'
        : '缺少真实环境、测试租户、账号、签名密码、未使用过的真实工单/任务上下文或已提交负向记录，不能执行真实 UI E2E。',
      missing: config.missing,
      invalidConfig: config.invalidConfig === true,
      submittedMode: config.submittedMode || 'unresolved',
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR
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
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    console.log(`PASS: eDHR approval tracking real E2E. Trace: ${result.trace}`)
  } catch (error) {
    const result = {
      status: 'FAIL',
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      submittedMode: config.submittedMode,
      trace: error.tracePath,
      steps: error.steps || [],
      error: serializeError(error)
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    console.error(`FAIL: ${error.message}`)
    process.exitCode = 1
  }
}

main()
