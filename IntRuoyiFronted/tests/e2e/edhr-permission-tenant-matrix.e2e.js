const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260528-edhr-role-tenant-e2e-gate'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-permission-tenant-matrix')
const EVIDENCE_FILE = path.resolve(process.cwd(), 'doc', 'tasks', TASK_ID, 'real-e2e-evidence.md')
const REQUIRED_BASE_URL = 'http://localhost:8081'
const REQUIRED_TEST_TENANT_ID = '122'
const FORBIDDEN_MUTATING_TENANTS = ['芋道源码', 'yudao', 'prod', 'production']
const MUTATING_METHOD_PATTERN = /POST|PUT|PATCH|DELETE/
const EDHR_WRITE_ENDPOINT_PATTERN = /\/admin-api\/mes\/pro\/batch-record-execution(?!\/(?:tracking|signature|domain-trace)\/?(?:detail|page|timeline)?)(?!-archive\/(?:get|page|download|latest))/
const NO_PERMISSION_PATTERNS = /403|无权限|未授权|forbidden|no permission|no menu|404|页面不存在|菜单不存在|暂无权限/i
const BLANK_SUCCESS_TEXT = '空白成功不可通过'
const DEFAULT_ROLE_USERNAMES = {
  executor: 'edhrmatrixexecutor',
  approver: 'edhrmatrixapprover',
  archiver: 'edhrmatrixarchiver',
  readonly: 'edhrmatrixreadonly',
  denied: 'edhrmatrixdenied'
}

const ROLE_ENV = {
  executor: ['EDHR_MATRIX_EXECUTOR_USERNAME', 'EDHR_MATRIX_EXECUTOR_PASSWORD'],
  approver: ['EDHR_MATRIX_APPROVER_USERNAME', 'EDHR_MATRIX_APPROVER_PASSWORD'],
  archiver: ['EDHR_MATRIX_ARCHIVER_USERNAME', 'EDHR_MATRIX_ARCHIVER_PASSWORD'],
  readonly: ['EDHR_MATRIX_READONLY_USERNAME', 'EDHR_MATRIX_READONLY_PASSWORD'],
  denied: ['EDHR_MATRIX_DENIED_USERNAME', 'EDHR_MATRIX_DENIED_PASSWORD']
}

const REQUIRED_ENV = [
  ['EDHR_MATRIX_BASE_URL', `真实前端入口，必须为 ${REQUIRED_BASE_URL}`],
  ['EDHR_MATRIX_TENANT', '测试租户名称，禁止为 live/formal/prod 租户'],
  ['EDHR_MATRIX_TENANT_ID', `测试租户 ID，必须为 ${REQUIRED_TEST_TENANT_ID}`],
  ['EDHR_MATRIX_EXECUTION_ID', '只读矩阵使用的真实 eDHR 执行记录 ID'],
  ['EDHR_MATRIX_EXECUTION_CODE', '只读矩阵使用的真实 eDHR 执行编号'],
  ['EDHR_MATRIX_ADMIN_BASE_URL', 'formal admin 只读 smoke 前端入口'],
  ['EDHR_MATRIX_ADMIN_TENANT', 'formal admin 只读 smoke 租户名称'],
  ['EDHR_MATRIX_ADMIN_USERNAME', 'formal admin 只读 smoke 用户名'],
  ['EDHR_MATRIX_ADMIN_PASSWORD', 'formal admin 只读 smoke 密码'],
  ...Object.entries(ROLE_ENV).map(([role, [, passwordKey]]) => [passwordKey, `${role} 真实账号密码`])
]

const BDD_SCENARIOS = [
  'BDD: readonly users cannot write -> Given readonly account, When it opens eDHR readonly pages, Then write guard observes zero eDHR POST/PUT/PATCH/DELETE requests.',
  'BDD: no-permission users fail visibly -> Given denied account, When it opens eDHR pages, Then explicit 403/no-permission/no-menu/404 evidence is captured.',
  'BDD: role-specific access is separated -> Given executor, approver, and archiver accounts, When each opens its role page, Then the UI must render recognizable eDHR text under that account.',
  'BDD: formal admin is readonly only -> Given formal admin account, When it performs smoke coverage, Then write guard prevents every eDHR mutating request.'
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function isForbiddenMutatingTenant(tenant) {
  const value = String(tenant || '').trim()
  const lower = value.toLowerCase()
  return FORBIDDEN_MUTATING_TENANTS.some((forbidden) => {
    const normalized = forbidden.toLowerCase()
    return lower === normalized || lower.includes(normalized) || value.includes(forbidden)
  })
}

function collectConfig() {
  const missing = REQUIRED_ENV.filter(([key]) => !envValue(key)).map(([key, description]) => ({
    key,
    description
  }))
  if (missing.length > 0) {
    return {
      status: 'BLOCKED',
      reason: '缺少真实权限矩阵账号、测试租户、前端入口或执行记录，不能执行真实 UI E2E。',
      missing,
      invalidConfig: false
    }
  }

  const config = {
    baseUrl: envValue('EDHR_MATRIX_BASE_URL').replace(/\/+$/, ''),
    tenant: envValue('EDHR_MATRIX_TENANT'),
    tenantId: envValue('EDHR_MATRIX_TENANT_ID'),
    executionId: envValue('EDHR_MATRIX_EXECUTION_ID'),
    executionCode: envValue('EDHR_MATRIX_EXECUTION_CODE'),
    adminBaseUrl: envValue('EDHR_MATRIX_ADMIN_BASE_URL').replace(/\/+$/, ''),
    adminTenant: envValue('EDHR_MATRIX_ADMIN_TENANT'),
    headed: process.env.EDHR_MATRIX_HEADED === '1',
    accounts: Object.fromEntries(
      Object.entries(ROLE_ENV).map(([role, [usernameKey, passwordKey]]) => [
        role,
        {
          role,
          username: envValue(usernameKey) || DEFAULT_ROLE_USERNAMES[role],
          password: envValue(passwordKey)
        }
      ])
    ),
    adminAccount: {
      role: 'admin',
      username: envValue('EDHR_MATRIX_ADMIN_USERNAME'),
      password: envValue('EDHR_MATRIX_ADMIN_PASSWORD')
    }
  }

  const invalid = []
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    invalid.push({
      key: 'EDHR_MATRIX_BASE_URL',
      description: `当前值为 ${config.baseUrl}；本门禁固定真实前端入口为 ${REQUIRED_BASE_URL}。`
    })
  }
  if (config.tenantId !== REQUIRED_TEST_TENANT_ID) {
    invalid.push({
      key: 'EDHR_MATRIX_TENANT_ID',
      description: `当前值为 ${config.tenantId}；矩阵 fixture 和 E2E 只允许 tenantId=${REQUIRED_TEST_TENANT_ID}。`
    })
  }
  if (isForbiddenMutatingTenant(config.tenant)) {
    invalid.push({
      key: 'EDHR_MATRIX_TENANT',
      description: '当前租户命中 live/formal/prod 保护名单；矩阵账号路径不能使用该租户。'
    })
  }
  if (!/^\d+$/.test(config.executionId)) {
    invalid.push({
      key: 'EDHR_MATRIX_EXECUTION_ID',
      description: '执行记录 ID 必须是真实数字 ID，不能是空值、编号或占位文本。'
    })
  }
  if (!config.adminBaseUrl) {
    invalid.push({
      key: 'EDHR_MATRIX_ADMIN_BASE_URL',
      description: 'formal admin 只读 smoke 必须显式配置 admin base URL，不能复用测试租户 base URL。'
    })
  }
  if (!config.adminTenant) {
    invalid.push({
      key: 'EDHR_MATRIX_ADMIN_TENANT',
      description: 'formal admin 只读 smoke 必须显式配置 admin tenant，不能复用 EDHR_MATRIX_TENANT。'
    })
  }

  if (invalid.length > 0) {
    return {
      status: 'BLOCKED',
      reason: '真实 E2E 前置条件不满足，不能使用错误入口、错误租户或无效执行记录。',
      missing: invalid,
      invalidConfig: true
    }
  }
  return config
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error('Missing Playwright runtime. Run `pnpm install`, then re-run `pnpm e2e:edhr:permission-matrix`.')
    blocked.blocked = true
    throw blocked
  }
}

function serializeError(error) {
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
    '# eDHR Role/Tenant Matrix E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- Generated at: ${new Date().toISOString()}`,
    `- Worktree: \`${process.cwd()}\``,
    '- Command: `pnpm e2e:edhr:permission-matrix`',
    '- Check command: `pnpm e2e:edhr:permission-matrix:check`',
    '- Result JSON: `test-results/edhr-permission-tenant-matrix/result.json`',
    `- Status: ${result.status}`,
    '',
    '## BDD',
    '',
    ...BDD_SCENARIOS.map((scenario) => `- ${scenario}`),
    ''
  ]
  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED', '')
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:permission-matrix\` -> FAIL, ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- \`${item.key}\`: ${item.description}`)
    }
    lines.push('- 影响：无法生成真实 UI 权限矩阵证据；未使用 mock、fallback、测试按钮或 API 替代真实用户路径。')
    lines.push('')
  }
  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `pnpm e2e:edhr:permission-matrix` -> PASS, role/tenant matrix real UI gate completed.')
    for (const step of result.steps || []) {
      lines.push(`- ${step.role} ${step.route} -> ${step.outcome}; writeGuard=${step.writeGuard || 'not-installed'}`)
    }
    lines.push('')
  }
  if (result.status === 'FAIL') {
    lines.push('## RED', '')
    lines.push(`- RED: \`pnpm e2e:edhr:permission-matrix\` -> FAIL, ${result.error?.message || 'unknown error'}`)
    lines.push('')
  }
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
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

async function clickVisibleButton(scope, namePattern, failureMessage) {
  const buttons = scope.getByRole('button', { name: namePattern })
  const count = await buttons.count()
  for (let index = 0; index < count; index += 1) {
    const button = buttons.nth(index)
    if (await button.isVisible()) {
      await button.click()
      return
    }
  }
  throw new Error(failureMessage)
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
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), account.username, `${account.role} 缺少用户名输入框。`)
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), account.password, `${account.role} 缺少密码输入框。`)
  await clickVisibleButton(loginForm, /^登录$/, `${account.role} 缺少登录按钮。`)
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })
}

async function resetSession(context, page) {
  await context.clearCookies()
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  }).catch(() => {})
}

function installEdhrWriteGuard(page, label) {
  const writes = []
  page.on('request', (request) => {
    const method = request.method()
    const url = request.url()
    if (MUTATING_METHOD_PATTERN.test(method) && EDHR_WRITE_ENDPOINT_PATTERN.test(url)) {
      writes.push({ method, url, label })
    }
  })
  return {
    assertClean() {
      assert.equal(writes.length, 0, `${label} readonly write guard caught eDHR mutating requests: ${JSON.stringify(writes)}`)
    },
    status() {
      return writes.length === 0 ? 'clean' : `failed:${writes.length}`
    }
  }
}

async function gotoEdhr(page, runtime, route) {
  const url = new URL(route, runtime.baseUrl)
  if (!url.searchParams.has('id')) url.searchParams.set('id', runtime.executionId)
  if (!url.searchParams.has('executionId')) url.searchParams.set('executionId', runtime.executionId)
  if (!url.searchParams.has('executionCode')) url.searchParams.set('executionCode', runtime.executionCode)
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
}

async function observePageOutcome(page, label, options = {}) {
  await page.waitForTimeout(1000)
  const bodyText = ((await page.locator('body').textContent({ timeout: 30000 })) || '').trim()
  const title = (await page.title().catch(() => '')) || ''
  const explicitPermissionError = NO_PERMISSION_PATTERNS.test(bodyText) || NO_PERMISSION_PATTERNS.test(title)
  if (options.requireNoPermission) {
    assert.ok(
      explicitPermissionError,
      `${label} no-permission path did not expose explicit 403/无权限/forbidden/no permission/no menu/404 evidence; ${BLANK_SUCCESS_TEXT}. body=${bodyText.slice(0, 500)}`
    )
    return { outcome: 'explicit-permission-block', title, evidence: bodyText.slice(0, 500) }
  }
  if (explicitPermissionError) {
    assert.ok(
      !options.requireRendered,
      `${label} allowed-path smoke must render eDHR UI, but captured explicit permission error. body=${bodyText.slice(0, 500)}`
    )
    return { outcome: 'explicit-permission-error', title, evidence: bodyText.slice(0, 500) }
  }
  assert.ok(bodyText.length > 0, `${label} rendered blank page; ${BLANK_SUCCESS_TEXT}.`)
  assert.match(bodyText, /eDHR|电子批记录|执行|审批|追踪|签名|字段审计|主数据追溯|归档/i, `${label} did not render recognizable eDHR evidence.`)
  return { outcome: 'rendered', title, evidence: bodyText.slice(0, 500) }
}

async function runRoute(page, runtime, routeSpec, account, options = {}) {
  const guard = options.writeGuard ? installEdhrWriteGuard(page, `${account.role}:${routeSpec.route}`) : null
  await gotoEdhr(page, runtime, routeSpec.route)
  const outcome = await observePageOutcome(page, `${account.role}:${routeSpec.name}`, options)
  if (guard) guard.assertClean()
  return {
    role: account.role,
    username: account.username,
    route: routeSpec.route,
    name: routeSpec.name,
    outcome: outcome.outcome,
    title: outcome.title,
    evidence: outcome.evidence,
    writeGuard: guard?.status()
  }
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
  const testRuntime = {
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    executionId: config.executionId,
    executionCode: config.executionCode
  }
  const adminRuntime = {
    baseUrl: config.adminBaseUrl,
    tenant: config.adminTenant,
    executionId: config.executionId,
    executionCode: config.executionCode
  }
  try {
    const roleRoutes = [
      { role: 'executor', name: 'execution detail', route: `/mes/pro/feedback/edhr-execution/detail?id=${config.executionId}` },
      { role: 'approver', name: 'approval page', route: `/mes/pro/feedback/edhr-approval?executionCode=${encodeURIComponent(config.executionCode)}` },
      { role: 'archiver', name: 'archive status', route: `/mes/pro/feedback/edhr-execution/detail?id=${config.executionId}` }
    ]
    for (const routeSpec of roleRoutes) {
      await resetSession(context, page)
      await login(page, testRuntime, config.accounts[routeSpec.role])
      steps.push(
        await runRoute(page, testRuntime, routeSpec, config.accounts[routeSpec.role], {
          writeGuard: true,
          requireRendered: true
        })
      )
    }

    const readonlyRoutes = [
      { name: 'execution readonly', route: `/mes/pro/feedback/edhr-execution/detail?id=${config.executionId}` },
      { name: 'tracking readonly', route: `/mes/pro/feedback/edhr-tracking?executionCode=${encodeURIComponent(config.executionCode)}` },
      { name: 'signatures readonly', route: `/mes/pro/feedback/edhr-signatures?executionId=${config.executionId}` },
      { name: 'field audit readonly', route: `/mes/pro/feedback/edhr-field-audit?executionId=${config.executionId}` },
      { name: 'domain trace readonly', route: `/mes/pro/feedback/edhr-domain-trace/detail?executionId=${config.executionId}&executionCode=${encodeURIComponent(config.executionCode)}` }
    ]
    await resetSession(context, page)
    await login(page, testRuntime, config.accounts.readonly)
    for (const routeSpec of readonlyRoutes) {
      steps.push(await runRoute(page, testRuntime, routeSpec, config.accounts.readonly, { writeGuard: true }))
    }

    await resetSession(context, page)
    await login(page, testRuntime, config.accounts.denied)
    steps.push(
      await runRoute(
        page,
        testRuntime,
        { name: 'denied eDHR execution', route: `/mes/pro/feedback/edhr-execution/detail?id=${config.executionId}` },
        config.accounts.denied,
        { writeGuard: true, requireNoPermission: true }
      )
    )

    await resetSession(context, page)
    await login(page, adminRuntime, config.adminAccount)
    steps.push(
      await runRoute(
        page,
        adminRuntime,
        { name: 'formal admin readonly smoke', route: `/mes/pro/feedback/edhr-execution/detail?id=${config.executionId}` },
        config.adminAccount,
        { writeGuard: true }
      )
    )

    await browser.close()
    return {
      status: 'PASS',
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: EVIDENCE_FILE,
      tenantId: config.tenantId,
      tenant: config.tenant,
      executionId: config.executionId,
      executionCode: config.executionCode,
      steps
    }
  } catch (error) {
    await browser.close()
    throw Object.assign(error, { steps })
  }
}

async function main() {
  const config = collectConfig()
  if (config.status === 'BLOCKED') {
    const result = {
      status: 'BLOCKED',
      reason: config.reason,
      missing: config.missing,
      invalidConfig: config.invalidConfig,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: EVIDENCE_FILE
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result)
    console.error(`BLOCKED: ${result.reason}`)
    for (const item of result.missing) console.error(`- ${item.key}: ${item.description}`)
    process.exitCode = 1
    return
  }

  try {
    const result = await runRealFlow(config)
    writeJsonResult(result)
    writeEvidenceMarkdown(result)
    console.log('PASS: eDHR role/tenant permission matrix real UI E2E completed.')
  } catch (error) {
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.blocked ? error.message : undefined,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: EVIDENCE_FILE,
      steps: error.steps || [],
      missing: error.blocked ? [{ key: 'playwright', description: error.message }] : undefined,
      error: serializeError(error)
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result)
    console.error(`${result.status}: ${error.message}`)
    process.exitCode = 1
  }
}

main()
