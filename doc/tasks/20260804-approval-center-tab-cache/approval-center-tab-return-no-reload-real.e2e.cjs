const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require(path.resolve(
  __dirname,
  '../../../IntRuoyiFronted/node_modules/playwright'
))

const repoRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const envFile = path.join(frontendRoot, '.env')
const taskDir = __dirname
const artifacts = {
  result: path.join(taskDir, 'approval-center-tab-return-no-reload-result.json'),
  screenshot: path.join(taskDir, 'approval-center-tab-return-no-reload.png'),
  failure: path.join(taskDir, 'approval-center-tab-return-no-reload-failed.png')
}

const parseEnvFile = (filePath) => {
  const values = {}
  if (!fs.existsSync(filePath)) return values
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/)
    if (match) values[match[1]] = match[2]
  }
  return values
}

const envDefaults = parseEnvFile(envFile)
const config = {
  baseUrl: (process.env.APPROVAL_CENTER_TAB_CACHE_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_CENTER_TAB_CACHE_TENANT || envDefaults.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: process.env.APPROVAL_CENTER_TAB_CACHE_USERNAME || envDefaults.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: process.env.APPROVAL_CENTER_TAB_CACHE_PASSWORD || envDefaults.VITE_APP_DEFAULT_LOGIN_PASSWORD,
  targetPath: process.env.APPROVAL_CENTER_TAB_CACHE_TARGET_PATH || '/approval-center/todo',
  alternatePath: process.env.APPROVAL_CENTER_TAB_CACHE_ALTERNATE_PATH || '/user/profile'
}

assert.ok(config.tenant, 'missing login tenant')
assert.ok(config.username, 'missing login username')
assert.ok(config.password, 'missing login password')

const chromeCandidates = [
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
].filter(Boolean)

const executablePath = chromeCandidates.find((candidate) => fs.existsSync(candidate))

const isSuccessPayload = (payload) => payload && (payload.code === 0 || payload.code === 200)

const settle = async (page) => {
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => null)
  await page.waitForTimeout(1000)
}

async function selectTenant(page, loginForm) {
  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]')
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  const tenantOption = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: config.tenant })
    .first()
  await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
  await tenantOption.click()
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('form.login-form:visible, .login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, loginForm)

  const textboxes = loginForm.getByRole('textbox')
  if ((await textboxes.count()) >= 2) {
    await textboxes.nth(1).fill(config.username)
  } else {
    await loginForm
      .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
      .first()
      .fill(config.username)
  }
  await loginForm.locator('input[type="password"], input[placeholder="请输入密码"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: '登录' }).first().click()
  const loginPayload = await (await loginResponsePromise).json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `login failed with business code ${loginPayload?.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function isApprovalTaskPageResponse(response) {
  if (response.request().method() !== 'GET') return false
  const url = new URL(response.url())
  return url.pathname === '/admin-api/approval-center/tasks/page'
}

async function main() {
  fs.mkdirSync(taskDir, { recursive: true })
  const browser = await chromium.launch({
    headless: process.env.APPROVAL_CENTER_TAB_CACHE_HEADED !== '1',
    executablePath
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  await context.clearCookies()
  const page = await context.newPage()
  const pageErrors = []
  const approvalResponses = []
  let phase = 'login'

  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('response', (response) => {
    if (!isApprovalTaskPageResponse(response)) return
    const url = new URL(response.url())
    approvalResponses.push({
      phase,
      status: response.status(),
      path: url.pathname,
      pageNo: url.searchParams.get('pageNo'),
      pageSize: url.searchParams.get('pageSize'),
      viewType: url.searchParams.get('viewType') || '',
      moduleCode: url.searchParams.get('moduleCode') || ''
    })
  })

  try {
    phase = 'initial'
    const initialResponsePromise = page.waitForResponse(isApprovalTaskPageResponse, { timeout: 60000 })
    await login(page)

    if (new URL(page.url()).pathname !== config.targetPath) {
      await page.goto(`${config.baseUrl}${config.targetPath}`, {
        waitUntil: 'domcontentloaded',
        timeout: 60000
      })
    }
    const initialResponse = await initialResponsePromise
    const initialPayload = await initialResponse.json().catch(() => null)
    assert.ok(isSuccessPayload(initialPayload), `initial approval list failed with code ${initialPayload?.code}`)
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)

    const initialResponseCount = approvalResponses.filter((item) => item.phase === 'initial').length
    assert.ok(initialResponseCount >= 1, 'initial approval center load must request the task page')

    phase = 'alternate'
    const alternateNavigation = page.waitForURL(
      (url) => url.pathname === config.alternatePath,
      { timeout: 60000 }
    )
    await page
      .locator('#v-menu')
      .getByRole('menuitem', { name: /^个人中心/ })
      .first()
      .click()
    await alternateNavigation
    await settle(page)

    phase = 'return'
    const approvalTag = page
      .locator('#v-tags-view .v-tags-view__item')
      .filter({ hasText: /审批中心/ })
      .first()
    await approvalTag.waitFor({ state: 'visible', timeout: 30000 })
    await approvalTag.click()
    await page.waitForURL((url) => url.pathname === '/approval-center/todo', { timeout: 60000 })
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)

    const returnResponses = approvalResponses.filter((item) => item.phase === 'return')
    assert.deepEqual(returnResponses, [], `tab return must not request approval task page: ${JSON.stringify(returnResponses)}`)
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join('\n')}`)

    await page.screenshot({ path: artifacts.screenshot, fullPage: true })
    const result = {
      baseUrl: config.baseUrl,
      targetPath: config.targetPath,
      alternatePath: config.alternatePath,
      user: config.username,
      tenant: config.tenant,
      initialResponseCount,
      returnResponseCount: returnResponses.length,
      approvalResponses,
      pageErrors,
      screenshot: artifacts.screenshot
    }
    fs.writeFileSync(artifacts.result, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`approval center tab return no-reload real e2e passed\n${JSON.stringify({
      targetPath: result.targetPath,
      alternatePath: result.alternatePath,
      user: result.user,
      tenant: result.tenant,
      initialResponseCount: result.initialResponseCount,
      returnResponseCount: result.returnResponseCount,
      screenshot: result.screenshot
    }, null, 2)}\n`)
  } catch (error) {
    await page.screenshot({ path: artifacts.failure, fullPage: true }).catch(() => null)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
