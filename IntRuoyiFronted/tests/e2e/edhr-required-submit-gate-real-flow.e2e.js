const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260612-edhr-required-validation-analysis'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-required-submit-gate')
const EVIDENCE_FILE = path.resolve(
  process.cwd(),
  '..',
  'doc',
  'tasks',
  TASK_ID,
  'real-e2e-evidence.md'
)

const REQUIRED_BASE_URL = 'http://localhost:8081'
const DEFAULT_TENANT = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const LIVE_VERIFICATION_TENANT = '芋道源码'
const LIVE_VERIFICATION_USERNAME = 'admin'
const SUBMIT_ENDPOINT_PATTERN = '/mes/pro/batch-record-execution/submit'

const BDD_SCENARIOS = [
  'BDD: 真实页面阻止缺失必填字段提交 -> Given 授权的本机租户存在真实草稿 eDHR 执行记录且至少一个 required 字段未填写 / When 用户登录并打开执行详情页点击“提交执行” / Then 页面显示“eDHR 必填字段未填写”，且不会调用真实提交接口。',
  'BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实草稿 executionId 或 required 缺失数据 / When 执行真实 E2E / Then 脚本写入 BLOCKED 证据并退出非零，不使用 mock、接口造数或测试专用 UI。'
]

function envValue(key) {
  return (process.env[key] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_REQUIRED_E2E_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_REQUIRED_E2E_TENANT') || DEFAULT_TENANT,
    username: envValue('EDHR_REQUIRED_E2E_USERNAME') || DEFAULT_USERNAME,
    password: envValue('EDHR_REQUIRED_E2E_PASSWORD'),
    executionId: envValue('EDHR_REQUIRED_E2E_EXECUTION_ID'),
    executablePath:
      envValue('EDHR_REQUIRED_E2E_CHROME_EXECUTABLE') || envValue('PLAYWRIGHT_CHROME_EXECUTABLE'),
    headed: envValue('EDHR_REQUIRED_E2E_HEADED') === '1'
  }
  return {
    ...config,
    missing: collectInvalidConfig(config)
  }
}

function collectInvalidConfig(config) {
  const invalid = []
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    invalid.push({
      key: 'EDHR_REQUIRED_E2E_BASE_URL',
      description: `真实前端入口必须固定为 ${REQUIRED_BASE_URL}。`
    })
  }
  const isExplicitLiveVerification =
    config.tenant === LIVE_VERIFICATION_TENANT && config.username === LIVE_VERIFICATION_USERNAME
  if ((config.tenant.includes('芋道源码') || config.tenant.toLowerCase() === 'yudao') && !isExplicitLiveVerification) {
    invalid.push({
      key: 'EDHR_REQUIRED_E2E_TENANT',
      description: '当前值命中 live 租户保护名单；除非显式使用 芋道源码/admin 做只读验证，否则真实 E2E 只能使用测试租户。'
    })
  }
  if (!config.password) {
    invalid.push({
      key: 'EDHR_REQUIRED_E2E_PASSWORD',
      description: '测试租户密码必须由当前进程环境或登录基线注入；不得写入脚本默认值。'
    })
  }
  if (!/^\d+$/.test(config.executionId)) {
    invalid.push({
      key: 'EDHR_REQUIRED_E2E_EXECUTION_ID',
      description: '必须提供真实数字型草稿执行 ID，且该执行记录需包含未填写的 required 字段。'
    })
  }
  return invalid
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw blockedError('Missing Playwright runtime. Run `pnpm install`, then re-run `pnpm e2e:edhr:required-submit-gate`.')
  }
}

function blockedError(message) {
  const error = new Error(message)
  error.blocked = true
  return error
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 必填提交门禁真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 生成时间：${new Date().toISOString()}`,
    `- 前端 worktree：${process.cwd()}`,
    `- 固定前端入口：\`${REQUIRED_BASE_URL}\``,
    `- 实际租户：\`${result.tenant || '<missing>'}\``,
    `- 实际账号名：\`${result.username || '<missing>'}\`；密码由 \`EDHR_REQUIRED_E2E_PASSWORD\` 注入，不写入仓库证据。`,
    '- 真实 E2E 复跑命令：`pnpm e2e:edhr:required-submit-gate`',
    '- 静态契约命令：`pnpm e2e:edhr:required-submit-gate:check`',
    '- 临时产物目录：`test-results/edhr-required-submit-gate/`（截图、trace 与 result.json 不提交）',
    `- 当前状态：${result.status}`,
    `- executionId：\`${result.executionId || '<missing>'}\``,
    '',
    '## BDD',
    '',
    ...BDD_SCENARIOS.map((scenario) => `- ${scenario}`),
    ''
  ]

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED', '')
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:required-submit-gate\` -> FAIL, ${result.reason}`)
    if (result.missing?.length) {
      lines.push('- 不满足的真实 E2E 前置条件：')
      for (const item of result.missing) {
        lines.push(`  - \`${item.key}\`：${item.description}`)
      }
    }
    lines.push('- 影响：无法通过真实页面证明缺失 required 字段阻止提交；未使用 mock、接口造数或测试专用 UI。', '')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `pnpm e2e:edhr:required-submit-gate` -> PASS，真实页面显示必填缺失错误，且没有调用提交接口。')
    lines.push(`- 错误提示：\`${result.errorText}\``)
    lines.push(`- 提交接口请求数：\`${result.submitRequestCount}\``)
    lines.push(`- Screenshot: \`${result.screenshot}\``)
    lines.push(`- Trace: \`${result.trace}\``, '')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED', '')
    lines.push(`- RED: \`pnpm e2e:edhr:required-submit-gate\` -> FAIL, ${result.error?.message || '未知错误'}`)
    lines.push('- 影响：真实 UI E2E 未放行；不得提交为通过。', '')
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function writeJsonResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
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
  throw new Error(`Cannot find visible ${label} input`)
}

async function clickVisibleButton(scope, namePattern, failureMessage) {
  const buttons = scope.locator('button:visible')
  const count = await buttons.count()
  for (let index = 0; index < count; index += 1) {
    const button = buttons.nth(index)
    const text = ((await button.textContent()) || '').trim()
    if (namePattern.test(text)) {
      if (await button.isDisabled()) {
        throw new Error(`${failureMessage} 按钮处于禁用状态。`)
      }
      await button.click()
      return
    }
  }
  throw new Error(failureMessage)
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page
    .locator('.login-form')
    .filter({ has: page.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]') })
    .filter({ hasText: '记住我' })
    .first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })

  const captchaInput = loginForm.locator('input[placeholder*="验证码"]').first()
  if ((await captchaInput.count()) > 0 && (await captchaInput.isVisible())) {
    throw blockedError('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
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

  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')
  await clickVisibleButton(loginForm, /^登录$/, '登录页缺少登录按钮。')
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({
    headless: !config.headed,
    ...(config.executablePath ? { executablePath: config.executablePath } : {})
  })
  const context = await browser.newContext({ acceptDownloads: false })
  await context.tracing.start({ screenshots: true, snapshots: true })
  const page = await context.newPage()
  let submitRequestCount = 0

  page.on('request', (request) => {
    if (request.url().includes(SUBMIT_ENDPOINT_PATTERN)) {
      submitRequestCount += 1
    }
  })

  try {
    await login(page, config)
    const detailUrl = `${config.baseUrl}/mes/pro/feedback/edhr-execution/detail?id=${encodeURIComponent(config.executionId)}`
    await page.goto(detailUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForLoadState('networkidle', { timeout: 60000 })

    const pageText = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
    if (/当前快照没有必填字段|无必填项/.test(pageText)) {
      throw blockedError(
        `真实草稿 executionId=${config.executionId} 当前快照没有 required 字段，无法验证“缺失必填字段阻止提交”门禁。`
      )
    }

    await page.locator('button:has-text("提交执行")').first().click()
    const errorLocator = page.locator('text=/eDHR 必填字段未填写/')
    await errorLocator.first().waitFor({ state: 'visible', timeout: 10000 })
    const errorText = (await errorLocator.first().innerText()).trim()
    assert.equal(submitRequestCount, 0, 'required gate must block before submit API request')

    const screenshot = path.join(RESULT_DIR, 'required-submit-gate.png')
    const trace = path.join(RESULT_DIR, 'trace.zip')
    await page.screenshot({ path: screenshot, fullPage: true })
    await context.tracing.stop({ path: trace })
    await browser.close()
    return {
      status: 'PASS',
      executionId: config.executionId,
      tenant: config.tenant,
      username: config.username,
      errorText,
      submitRequestCount,
      screenshot,
      trace
    }
  } catch (error) {
    const trace = path.join(RESULT_DIR, 'trace.zip')
    const screenshot = path.join(RESULT_DIR, 'required-submit-gate-failure.png')
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => undefined)
    await context.tracing.stop({ path: trace }).catch(() => undefined)
    await browser.close().catch(() => undefined)
    return {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.message,
      executionId: config.executionId,
      tenant: config.tenant,
      username: config.username,
      currentUrl: page.url(),
      submitRequestCount,
      screenshot,
      error: {
        name: error.name || 'Error',
        message: error.message || String(error),
        stack: error.stack
      },
      trace
    }
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实 E2E 前置条件缺失。',
      missing: config.missing,
      executionId: config.executionId,
      tenant: config.tenant,
      username: config.username
    }
    writeJsonResult(result)
    writeEvidence(result)
    process.exitCode = 1
    return
  }

  const result = await runRealFlow(config)
  writeJsonResult(result)
  writeEvidence(result)
  if (result.status !== 'PASS') {
    process.exitCode = 1
  }
}

main().catch((error) => {
  const result = {
    status: error.blocked ? 'BLOCKED' : 'FAIL',
    reason: error.message,
    error: {
      name: error.name || 'Error',
      message: error.message || String(error),
      stack: error.stack
    }
  }
  writeJsonResult(result)
  writeEvidence(result)
  process.exitCode = 1
})
