const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260701-edhr-form-permission-signature-verification'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-approval-history-readonly')
const EVIDENCE_FILE = path.resolve(
  process.cwd(),
  'doc',
  'tasks',
  TASK_ID,
  'approval-history-readonly-evidence.md'
)

const DEFAULT_BASE_URL = 'http://localhost:8081'
const DEFAULT_TENANT = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const DEFAULT_APPROVED_EXECUTION_ID = '761'
const DEFAULT_REJECTED_EXECUTION_ID = '760'
const FORBIDDEN_LIVE_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])

const BDD_SCENARIOS = [
  'BDD: 历史审批通过详情只读验证 -> Given 测试租户存在真实已关闭 eDHR 执行记录 / When 用户通过真实前端打开审批详情页 / Then 页面加载审批详情、追踪时间线、签名记录且展示 APPROVE 证据。',
  'BDD: 历史审批驳回详情只读验证 -> Given 测试租户存在真实已驳回 eDHR 执行记录 / When 用户通过真实前端打开审批详情页 / Then 页面加载审批详情、追踪时间线、签名记录且展示 REJECT 和驳回原因证据。',
  'BDD: 缺少真实前置即阻塞 -> Given 缺少登录密码、真实历史记录、菜单权限或前端入口 / When 执行 E2E / Then 写入 BLOCKED/FAIL 证据，不使用 mock、不改数据、不接口绕过。'
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error(
      'Missing Playwright runtime. Run `pnpm install` in the frontend workspace before running this real E2E.'
    )
    blocked.blocked = true
    throw blocked
  }
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_APPROVAL_HISTORY_BASE_URL') || DEFAULT_BASE_URL,
    tenant: envValue('EDHR_APPROVAL_HISTORY_TENANT') || DEFAULT_TENANT,
    username: envValue('EDHR_APPROVAL_HISTORY_USERNAME') || DEFAULT_USERNAME,
    password: envValue('EDHR_APPROVAL_HISTORY_PASSWORD'),
    approvedExecutionId:
      envValue('EDHR_APPROVAL_HISTORY_APPROVED_EXECUTION_ID') || DEFAULT_APPROVED_EXECUTION_ID,
    rejectedExecutionId:
      envValue('EDHR_APPROVAL_HISTORY_REJECTED_EXECUTION_ID') || DEFAULT_REJECTED_EXECUTION_ID,
    executablePath:
      envValue('EDHR_APPROVAL_HISTORY_CHROME_EXECUTABLE') ||
      envValue('PLAYWRIGHT_CHROME_EXECUTABLE'),
    headed: envValue('EDHR_APPROVAL_HISTORY_HEADED') === '1'
  }

  const missing = []
  if (!config.password) {
    missing.push({
      key: 'EDHR_APPROVAL_HISTORY_PASSWORD',
      description: '测试租户登录密码必须通过环境变量注入，不能写入仓库。'
    })
  }
  if (config.baseUrl !== DEFAULT_BASE_URL) {
    missing.push({
      key: 'EDHR_APPROVAL_HISTORY_BASE_URL',
      description: `真实前端入口必须固定为 ${DEFAULT_BASE_URL}。`
    })
  }
  if (config.tenant !== DEFAULT_TENANT || isForbiddenLiveTenant(config.tenant)) {
    missing.push({
      key: 'EDHR_APPROVAL_HISTORY_TENANT',
      description: '只允许使用测试租户执行只读真实 E2E。'
    })
  }
  if (!/^\d+$/.test(config.approvedExecutionId)) {
    missing.push({
      key: 'EDHR_APPROVAL_HISTORY_APPROVED_EXECUTION_ID',
      description: '必须提供真实已关闭 eDHR executionId。'
    })
  }
  if (!/^\d+$/.test(config.rejectedExecutionId)) {
    missing.push({
      key: 'EDHR_APPROVAL_HISTORY_REJECTED_EXECUTION_ID',
      description: '必须提供真实已驳回 eDHR executionId。'
    })
  }
  if (String(config.approvedExecutionId) === String(config.rejectedExecutionId)) {
    missing.push({
      key: 'EDHR_APPROVAL_HISTORY_APPROVED_EXECUTION_ID / EDHR_APPROVAL_HISTORY_REJECTED_EXECUTION_ID',
      description: '已关闭与已驳回历史记录不能使用同一个 executionId。'
    })
  }

  if (missing.length > 0) return { ...config, missing }
  return { ...config, missing: [] }
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
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function writeEvidenceMarkdown(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 审批历史只读真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 生成时间：${new Date().toISOString()}`,
    `- 前端工作目录：${process.cwd()}`,
    `- 固定前端入口：\`${DEFAULT_BASE_URL}\``,
    '- 固定测试租户：`测试租户`',
    '- 默认账号名：`aoteman`；密码由 `EDHR_APPROVAL_HISTORY_PASSWORD` 注入，不写入仓库证据。',
    '- 测试性质：只读真实 UI E2E，仅验证既有历史通过/驳回记录展示链路，不创建、不提交、不审批、不归档。',
    `- 当前状态：${result.status}`,
    ''
  ]

  lines.push('## BDD')
  lines.push('')
  for (const scenario of BDD_SCENARIOS) lines.push(`- ${scenario}`)
  lines.push('')

  if (result.reason) {
    lines.push('## 阻塞原因')
    lines.push('')
    lines.push(`- ${result.reason}`)
    if (Array.isArray(result.missing)) {
      for (const item of result.missing) lines.push(`- ${item.key}: ${item.description}`)
    }
    lines.push('')
  }

  if (Array.isArray(result.steps)) {
    lines.push('## Steps')
    lines.push('')
    for (const step of result.steps) {
      lines.push(`- ${step.name}: ${step.status || 'PASS'}`)
      if (step.executionId) lines.push(`  - executionId: \`${step.executionId}\``)
      if (step.executionCode) lines.push(`  - executionCode: \`${step.executionCode}\``)
      if (step.statusLabel) lines.push(`  - statusLabel: ${step.statusLabel}`)
      if (step.requiredAction) lines.push(`  - requiredAction: ${step.requiredAction}`)
      if (step.rejectReason) lines.push(`  - rejectReason: ${step.rejectReason}`)
      if (step.screenshot) lines.push(`  - screenshot: ${step.screenshot}`)
    }
    lines.push('')
  }

  if (result.error) {
    lines.push('## Error')
    lines.push('')
    lines.push('```text')
    lines.push(result.error.message || String(result.error))
    lines.push('```')
    lines.push('')
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
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
  await item.fill(value)
}

async function clickVisibleButton(scope, namePattern, failureMessage) {
  const button = await firstVisible(scope.getByRole('button', { name: namePattern }), failureMessage)
  await button.click()
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

async function readApiData(response, label) {
  assert.equal(response.status(), 200, `${label} HTTP 状态应为 200，实际 ${response.status()}`)
  return parseBusinessData(await response.json(), label)
}

function responseMatches(response, endpoint, expectedParams) {
  if (response.request().method() !== 'GET') return false
  if (!response.url().includes(endpoint)) return false
  const url = new URL(response.url())
  return Object.entries(expectedParams).every(([key, value]) => url.searchParams.get(key) === String(value))
}

async function waitForApiResponse(page, endpoint, expectedParams, label) {
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
}

async function screenshot(page, name) {
  ensureDir(RESULT_DIR)
  const filePath = path.join(RESULT_DIR, `${name}.png`)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

function extractRows(data, label) {
  assert.ok(data && typeof data === 'object', `${label} data 必须是对象。`)
  assert.ok(Array.isArray(data.list), `${label} PageResult 缺少 list rows。`)
  return data.list
}

function assertSignatureAction(rows, actionType, label) {
  const found = rows.find((row) => row && row.actionType === actionType)
  assert.ok(found, `${label} 签名分页未包含 ${actionType} 动作。`)
  assert.equal(found.passwordVerified, true, `${label} ${actionType} 签名必须为密码校验通过。`)
  assert.ok(found.actorName || found.actorNickname, `${label} ${actionType} 签名缺少签名人。`)
  return found
}

function assertTrackingEvent(rows, eventType, label) {
  const found = rows.find((row) => row && row.eventType === eventType)
  assert.ok(found, `${label} 追踪时间线未包含 ${eventType} 事件。`)
  assert.ok(found.actorName, `${label} ${eventType} 追踪事件缺少处理人。`)
  assert.ok(found.occurredAt, `${label} ${eventType} 追踪事件缺少处理时间。`)
  return found
}

async function verifyReadonlyApprovalDetail(page, config, scenario) {
  const detailPromise = waitForApiResponse(
    page,
    '/mes/pro/batch-record-execution/approval-detail',
    { id: scenario.executionId },
    `${scenario.label} 审批详情`
  )
  const trackingPromise = waitForApiResponse(
    page,
    '/mes/pro/batch-record-execution/tracking-timeline',
    { executionId: scenario.executionId },
    `${scenario.label} 追踪时间线`
  )
  const signaturePromise = waitForApiResponse(
    page,
    '/mes/pro/batch-record-execution/signature-page',
    { executionId: scenario.executionId },
    `${scenario.label} 签名分页`
  )

  await page.goto(`${config.baseUrl}/mes/pro/feedback/edhr-approval/detail?id=${scenario.executionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const detail = await readApiData(await detailPromise, `${scenario.label} 审批详情`)
  const trackingRows = await readApiData(await trackingPromise, `${scenario.label} 追踪时间线`)
  const signatureRows = extractRows(await readApiData(await signaturePromise, `${scenario.label} 签名分页`), `${scenario.label} 签名分页`)

  assert.equal(String(detail.executionId || detail.id), String(scenario.executionId), `${scenario.label} executionId 不匹配。`)
  assert.equal(Number(detail.status), scenario.expectedStatus, `${scenario.label} 状态不符合预期。`)
  assert.ok(detail.executionSnapshotJson, `${scenario.label} 缺少 executionSnapshotJson。`)
  assert.ok(detail.approvalSnapshotId, `${scenario.label} 缺少 approvalSnapshotId。`)
  assert.ok(detail.approvalSnapshotHash, `${scenario.label} 缺少 approvalSnapshotHash。`)
  assert.equal(
    detail.approvalSnapshotStatus,
    scenario.expectedSnapshotStatus,
    `${scenario.label} approvalSnapshotStatus 不符合预期。`
  )

  const trackingEvent = assertTrackingEvent(trackingRows, scenario.requiredAction, scenario.label)
  const signature = assertSignatureAction(signatureRows, scenario.requiredAction, scenario.label)
  if (scenario.requiredAction === 'REJECT') {
    const reason = detail.rejectReason || trackingEvent.rejectReason || trackingEvent.comment || signature.reason || signature.comment
    assert.ok(reason && String(reason).trim().length > 0, '驳回历史记录必须展示驳回原因。')
    scenario.rejectReason = reason
  }

  await page.getByText('eDHR 审批详情').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText(String(detail.executionCode)).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText(scenario.expectedStatusLabel).first().waitFor({ state: 'visible', timeout: 30000 })
  const tabs = page.locator('.edhr-detail__tabs').first()
  await page.getByRole('tab', { name: '追踪' }).click()
  await firstVisible(
    tabs.getByText(trackingEvent.actorName),
    `${scenario.label} 追踪页签未可见展示处理人 ${trackingEvent.actorName}。`
  )
  await page.getByRole('tab', { name: '签名记录' }).click()
  const signatureActor = signature.actorName || signature.actorNickname
  await firstVisible(
    tabs.getByText(signatureActor),
    `${scenario.label} 签名页签未可见展示签名人 ${signatureActor}。`
  )
  const shot = await screenshot(page, `${scenario.name}-${scenario.executionId}`)

  return {
    name: scenario.label,
    status: 'PASS',
    executionId: scenario.executionId,
    executionCode: detail.executionCode,
    statusLabel: scenario.expectedStatusLabel,
    requiredAction: scenario.requiredAction,
    rejectReason: scenario.rejectReason,
    screenshot: shot
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
  const page = await context.newPage()
  const tracePath = path.join(RESULT_DIR, 'trace.zip')
  const steps = []
  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  try {
    await login(page, config)
    steps.push(
      await verifyReadonlyApprovalDetail(page, config, {
        name: 'approved-history',
        label: '历史审批通过详情只读验证',
        executionId: config.approvedExecutionId,
        expectedStatus: 3,
        expectedSnapshotStatus: 'APPROVED',
        expectedStatusLabel: '已关闭',
        requiredAction: 'APPROVE'
      })
    )
    steps.push(
      await verifyReadonlyApprovalDetail(page, config, {
        name: 'rejected-history',
        label: '历史审批驳回详情只读验证',
        executionId: config.rejectedExecutionId,
        expectedStatus: 2,
        expectedSnapshotStatus: 'REJECTED',
        expectedStatusLabel: '已驳回',
        requiredAction: 'REJECT'
      })
    )
    await context.tracing.stop({ path: tracePath })
    await browser.close()
    return {
      status: 'PASS',
      generatedAt: new Date().toISOString(),
      approvedExecutionId: config.approvedExecutionId,
      rejectedExecutionId: config.rejectedExecutionId,
      steps,
      trace: tracePath,
      resultDir: RESULT_DIR
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
      reason: '真实审批历史只读 E2E 前置条件不满足。',
      missing: config.missing,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result)
    console.error(`BLOCKED: ${result.reason}`)
    for (const item of config.missing) console.error(`- ${item.key}: ${item.description}`)
    process.exitCode = 1
    return
  }

  try {
    const result = await runRealFlow(config)
    writeJsonResult(result)
    writeEvidenceMarkdown(result)
    console.log(`PASS: eDHR approval history readonly real E2E. Trace: ${result.trace}`)
  } catch (error) {
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      trace: error.tracePath,
      steps: error.steps || [],
      error: serializeError(error)
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result)
    console.error(`${result.status}: ${error.message}`)
    process.exitCode = 1
  }
}

main()
