const fs = require('node:fs')
const path = require('node:path')

const TASK_ID = '20260612-edhr-final-archive-todo-assessment'
const EVIDENCE_FILE = path.resolve(
  process.cwd(),
  '..',
  'doc',
  'tasks',
  TASK_ID,
  'record-change-release-e2e-evidence.md'
)
const CHANGE_ROUTE = '/mes/pro/feedback/edhr-change'
const CHANGE_PAGE_API = '/admin-api/mes/pro/edhr-change/page'
const CHANGE_API_PREFIX = '/admin-api/mes/pro/edhr-change'

const BDD_SCENARIOS = [
  'BDD: eDHR 变更记录只读入口 -> Given 已授权用户通过真实登录进入 eDHR 变更记录页 / When 页面加载变更记录 / Then 分页接口返回成功，筛选栏和表格可见。',
  'BDD: eDHR 异常变更入口不得静默降级 -> Given 缺少真实登录环境或页面接口失败 / When 执行真实 E2E / Then 脚本必须 FAIL 或 BLOCKED，不使用默认密码、mock 响应或 API-only 成功。',
  'BDD: eDHR 变更记录页只读守卫 -> Given 用户只查看变更记录列表 / When 页面完成加载 / Then 不得向 eDHR 变更接口发送 POST/PUT/PATCH/DELETE 请求。'
]

function envValue(key) {
  return (process.env[key] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_CHANGE_E2E_BASE_URL'),
    tenant: envValue('EDHR_CHANGE_E2E_TENANT'),
    username: envValue('EDHR_CHANGE_E2E_USERNAME'),
    password: envValue('EDHR_CHANGE_E2E_PASSWORD')
  }
  const missing = []
  for (const key of [
    'EDHR_CHANGE_E2E_BASE_URL',
    'EDHR_CHANGE_E2E_TENANT',
    'EDHR_CHANGE_E2E_USERNAME',
    'EDHR_CHANGE_E2E_PASSWORD'
  ]) {
    if (!envValue(key)) missing.push(key)
  }
  return { ...config, baseUrl: config.baseUrl.replace(/\/+$/, ''), missing }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error('Missing Playwright runtime. Run `pnpm install`, then re-run `pnpm e2e:edhr:record-change`.')
    blocked.blocked = true
    throw blocked
  }
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 异常变更记录真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 生成时间：${new Date().toISOString()}`,
    '- 真实 E2E 复跑命令：`pnpm e2e:edhr:record-change`',
    '- 静态语法检查命令：`pnpm e2e:edhr:record-change:check`',
    `- 当前状态：${result.status}`,
    `- 前端入口：\`${result.baseUrl || '<missing>'}\``,
    `- 租户：\`${result.tenant || '<missing>'}\``,
    `- 账号：\`${result.username || '<missing>'}\`；密码由环境变量注入，不写入仓库证据。`,
    '',
    '## BDD',
    '',
    ...BDD_SCENARIOS.map((scenario) => `- ${scenario}`),
    ''
  ]

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED', '')
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:record-change\` -> FAIL, ${result.reason}`)
    if (result.missing?.length) {
      lines.push(`- 缺少环境变量：${result.missing.map((item) => `\`${item}\``).join('、')}`)
    }
    lines.push('- 影响：无法通过真实页面证明 eDHR 异常变更记录入口可用；未使用默认密码、mock 响应或 API-only 成功。', '')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push(
      `- PASS: \`pnpm e2e:edhr:record-change\` -> PASS，真实页面打开变更记录列表，接口返回成功，写请求数 \`${result.writeRequestCount}\`。`
    )
    lines.push('')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED', '')
    lines.push(`- FAIL: \`pnpm e2e:edhr:record-change\` -> FAIL, ${result.error?.message || result.reason}`)
    lines.push('- 影响：eDHR 异常变更记录真实 UI E2E 未放行。', '')
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function serializeError(error) {
  return {
    name: error?.name || 'Error',
    message: error?.message || String(error),
    stack: error?.stack
  }
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
  throw new Error(`Missing visible ${label} input`)
}

async function selectTenant(page, tenant) {
  const form = page.locator('.login-form:visible').first()
  const tenantInput = form.locator('.el-select input[role="combobox"], input[placeholder="请输入租户名称"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    await page.keyboard.press('Enter')
  }
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(CHANGE_ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const form = page.locator('.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if ((await form.locator('.verifybox, .verify-bar-area, .verify-img-panel, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('Captcha is enabled; unattended real E2E cannot continue.')
  }

  await selectTenant(page, config.tenant)
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'), config.username, 'username')
  await fillFirstVisible(form.locator('input[placeholder="请输入密码"]'), config.password, 'password')
  const [response] = await Promise.all([
    page.waitForResponse((item) => item.url().includes('/admin-api/system/auth/login') && item.status() === 200, {
      timeout: 60000
    }),
    form.getByRole('button', { name: /^登录$/ }).first().click()
  ])
  const body = await response.json()
  if (body.code !== 0) {
    throw new Error(`登录接口返回业务错误: tenant=${config.tenant}, username=${config.username}, msg=${body.msg || body.code}`)
  }
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function collectChangeWriteRequests(page) {
  const writeRequests = []
  page.on('request', (request) => {
    const url = request.url()
    if (!url.includes(CHANGE_API_PREFIX)) return
    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())) return
    writeRequests.push(`${request.method()} ${url}`)
  })
  return writeRequests
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    const writeRequests = collectChangeWriteRequests(page)
    await login(page, config)
    const pageResponsePromise = page.waitForResponse(
      (item) => item.url().includes(CHANGE_PAGE_API) && item.status() === 200,
      { timeout: 60000 }
    )
    await page.goto(`${config.baseUrl}${CHANGE_ROUTE}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const pageResponse = await pageResponsePromise
    const pageBody = await pageResponse.json()
    if (pageBody.code !== 0) {
      throw new Error(`变更记录分页接口返回业务错误: ${pageBody.msg || pageBody.code}`)
    }
    await page.locator('.edhr-record-change__toolbar').waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.edhr-record-change__table').waitFor({ state: 'visible', timeout: 60000 })
    if (writeRequests.length > 0) {
      throw new Error(`变更记录只读路径不得发送写请求: ${writeRequests.join(', ')}`)
    }
    return { status: 'PASS', writeRequestCount: writeRequests.length }
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const blocked = {
      status: 'BLOCKED',
      reason: '缺少 eDHR 异常变更记录真实 E2E 登录环境变量。',
      missing: config.missing,
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username
    }
    writeEvidence(blocked)
    console.error(blocked.reason)
    process.exitCode = 1
    return
  }

  try {
    const flow = await runRealFlow(config)
    const result = {
      ...flow,
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username
    }
    writeEvidence(result)
    console.log('PASS: eDHR change record real flow')
  } catch (error) {
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.blocked ? error.message : 'eDHR 异常变更记录真实 E2E 执行失败。',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      error: serializeError(error)
    }
    writeEvidence(result)
    console.error(error)
    process.exitCode = 1
  }
}

main()
