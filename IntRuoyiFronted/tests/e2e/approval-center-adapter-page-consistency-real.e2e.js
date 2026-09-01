const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const REPO_ROOT = path.resolve(process.env.APPROVAL_CENTER_E2E_REPO_ROOT || path.join(process.cwd(), '..'))
const REGISTRY_PATH = 'D:\\IntRuoyiWorktree\\.ports\\worktree-ports.json'
const TENANT = process.env.APPROVAL_CENTER_E2E_TENANT || '测试租户'
const USERNAME = process.env.APPROVAL_CENTER_E2E_USERNAME || 'admin'
const PASSWORD = process.env.APPROVAL_CENTER_E2E_PASSWORD || ''
const CHROME_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const RUN_ID =
  process.env.APPROVAL_CENTER_E2E_RUN_ID ||
  `approval-center-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`
const RESULT_DIR = path.resolve(process.cwd(), 'output', 'playwright',
  'approval-center-adapter-page-consistency', RUN_ID)
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')

function normalizeWindowsPath(value) {
  return path.resolve(value).replaceAll('/', '\\').replace(/\\+$/, '').toLowerCase()
}

function resolveRuntime() {
  const repoRoot = normalizeWindowsPath(REPO_ROOT)
  if (repoRoot === normalizeWindowsPath('E:\\IntRuoyi')) {
    return { profile: 'int_main', slot: 0, frontendPort: 8081, backendPort: 48081 }
  }
  assert.ok(fs.existsSync(REGISTRY_PATH), `Runtime registry not found: ${REGISTRY_PATH}`)
  const document = JSON.parse(fs.readFileSync(REGISTRY_PATH, 'utf8'))
  const matches = (document.worktrees || []).filter(
    (entry) => entry.active === true && normalizeWindowsPath(entry.path) === repoRoot
  )
  assert.equal(matches.length, 1, `Expected one active runtime registration for ${REPO_ROOT}`)
  return matches[0]
}

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_FILE, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

async function firstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible()) return candidate
  }
  throw new Error(`Cannot find visible ${label}`)
}

async function waitApprovalPageResponse(page) {
  const response = await page.waitForResponse((candidate) => {
    const url = new URL(candidate.url())
    return url.pathname.endsWith('/approval-center/tasks/page') &&
      url.searchParams.get('viewType') === 'TODO'
  }, { timeout: 90000 })
  assert.equal(response.status(), 200, 'approval task page must return HTTP 200')
  const body = await response.json()
  assert.equal(body.code, 0, `approval task page failed: ${body.msg || body.code}`)
  assert.ok(body.data && Array.isArray(body.data.list), 'approval task page must return a list')
  assert.ok(Number.isFinite(Number(body.data.total)), 'approval task page must return total')
  return body.data
}

async function run() {
  assert.ok(PASSWORD, 'APPROVAL_CENTER_E2E_PASSWORD is required')
  assert.ok(fs.existsSync(CHROME_EXECUTABLE), `Chrome executable not found: ${CHROME_EXECUTABLE}`)
  const runtime = resolveRuntime()
  const baseUrl = `http://127.0.0.1:${runtime.frontendPort}`
  const { chromium } = require('playwright')
  const browser = await chromium.launch({ headless: true, executablePath: CHROME_EXECUTABLE })
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const page = await context.newPage()
  const targetConsoleErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error' && /审批待办数量加载失败|APPROVAL_ADAPTER_PAGE_INCONSISTENT/.test(message.text())) {
      targetConsoleErrors.push(message.text())
    }
  })

  try {
    await page.goto(`${baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.locator('.login-form:visible').first().waitFor({ state: 'visible', timeout: 60000 })
    const loginForm = await firstVisible(page.locator('.login-form'), 'login form')
    const tenantInput = await firstVisible(loginForm.locator('input.el-select__input'), 'tenant input')
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    await page.waitForTimeout(300)
    await firstVisible(
      page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }),
      'tenant option'
    ).then((option) => option.click())
    await firstVisible(
      loginForm.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'),
      'username input'
    ).then((input) => input.fill(USERNAME))
    await firstVisible(loginForm.locator('input[placeholder="请输入密码"]'), 'password input')
      .then((input) => input.fill(PASSWORD))

    const badgeResponsePromise = waitApprovalPageResponse(page)
    await firstVisible(loginForm.getByRole('button', { name: /^登录$/ }), 'login button')
      .then((button) => button.click())
    await page.waitForURL((url) => url.pathname === '/index', { timeout: 90000 })
    const badgePage = await badgeResponsePromise

    const workbenchResponsePromise = waitApprovalPageResponse(page)
    await page.goto(`${baseUrl}/approval-center/todo`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    const workbenchPage = await workbenchResponsePromise
    await page.locator('.approval-center').waitFor({ state: 'visible', timeout: 60000 })
    const pageText = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
    assert.doesNotMatch(pageText, /审批待办数量加载失败|APPROVAL_ADAPTER_PAGE_INCONSISTENT/)
    assert.equal(targetConsoleErrors.length, 0, `target console errors: ${targetConsoleErrors.join(' | ')}`)

    const result = {
      status: 'PASS',
      runId: RUN_ID,
      tenant: TENANT,
      username: USERNAME,
      runtimeProfile: runtime.profile,
      runtimeSlot: runtime.slot,
      badgeTotal: badgePage.total,
      badgeRowCount: badgePage.list.length,
      workbenchTotal: workbenchPage.total,
      workbenchRowCount: workbenchPage.list.length,
      targetConsoleErrorCount: targetConsoleErrors.length
    }
    writeResult(result)
    console.log(JSON.stringify(result, null, 2))
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  writeResult({
    status: 'FAIL',
    runId: RUN_ID,
    tenant: TENANT,
    username: USERNAME,
    error: { name: error.name, message: error.message, stack: error.stack }
  })
  console.error(error)
  process.exitCode = 1
})
