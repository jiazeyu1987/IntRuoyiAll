const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.EDHR_QA_MENU_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
  /\/+$/,
  ''
)
const TARGET_PATH = '/mes/pro/process-pool/qa-regulation'
const RESULT_DIR = path.resolve(WORKSPACE_ROOT, 'output', 'playwright', '20260804-qa-regulation-tab')
const RESULT_PATH = path.join(RESULT_DIR, 'edhr-qa-menu-real-e2e.json')
const SCREENSHOT_PATH = path.join(RESULT_DIR, 'edhr-qa-menu-real-e2e.png')

function parseEnvValue(value) {
  const trimmed = (value || '').trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  const env = {}
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) {
      continue
    }
    const equalsIndex = trimmed.indexOf('=')
    if (equalsIndex <= 0) {
      continue
    }
    env[trimmed.slice(0, equalsIndex).trim()] = parseEnvValue(trimmed.slice(equalsIndex + 1))
  }
  return env
}

function collectLoginConfig() {
  const env = {
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env')),
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env.local')),
    ...process.env
  }
  return {
    tenant: env.EDHR_QA_MENU_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.EDHR_QA_MENU_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.EDHR_QA_MENU_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
}

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

async function login(page, config) {
  assert.ok(config.tenant, 'local default tenant is required')
  assert.ok(config.username, 'local default username is required')
  assert.ok(config.password, 'local default password is required')

  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({
      hasText: config.tenant
    }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form
    .locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])')
    .first()
    .fill(config.username)
  await form.locator('input[type="password"], input[placeholder="请输入密码"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

function visibleMenuItem(page, text) {
  return page
    .locator('.el-menu-item:visible, .el-sub-menu__title:visible, [role="menuitem"]:visible')
    .filter({ hasText: new RegExp(`^\\s*${text}\\s*$`) })
    .first()
}

async function getBox(locator, label) {
  await locator.waitFor({ state: 'visible' })
  const box = await locator.boundingBox()
  assert.ok(box, `${label} must have a visible bounding box`)
  return box
}

async function openEdhrMenu(page) {
  const visibleRoot = visibleMenuItem(page, 'eDHR批记录')
  try {
    await visibleRoot.waitFor({ state: 'visible', timeout: 5000 })
    await visibleRoot.click()
    return
  } catch (error) {
    const rootSubMenu = page
      .locator('.el-sub-menu')
      .filter({ has: page.getByText('eDHR批记录', { exact: true }) })
      .first()
    await rootSubMenu.waitFor({ state: 'attached' })
    const rootTitle = rootSubMenu.locator('.el-sub-menu__title').first()
    await rootTitle.waitFor({ state: 'visible' })
    await rootTitle.click()
  }
}

async function main() {
  const config = collectLoginConfig()
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const consoleErrors = []
  const pageErrors = []
  const writeRequests = []
  let captureWrites = false

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('request', (request) => {
      if (!captureWrites) {
        return
      }
      if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/')) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })

    await login(page, config)
    captureWrites = true

    await page.goto(`${BASE_URL}/index`, { waitUntil: 'domcontentloaded' })
    await openEdhrMenu(page)

    const batchRecordForm = visibleMenuItem(page, '批记录表单')
    const qaMenu = visibleMenuItem(page, 'QA')
    const batchExecution = visibleMenuItem(page, '批次执行')

    const batchRecordFormBox = await getBox(batchRecordForm, '批记录表单 menu')
    const qaMenuBox = await getBox(qaMenu, 'QA menu')
    const batchExecutionBox = await getBox(batchExecution, '批次执行 menu')

    assert.ok(
      batchRecordFormBox.y < qaMenuBox.y && qaMenuBox.y < batchExecutionBox.y,
      'QA menu must be visually located between 批记录表单 and 批次执行'
    )

    await qaMenu.click()
    await page.waitForURL((current) => current.pathname === TARGET_PATH, {
      timeout: 60000,
      waitUntil: 'domcontentloaded'
    })
    await page.locator('[data-qa-regulation-page]').first().waitFor({ state: 'visible' })
    await page.getByText('QA 规程配置', { exact: false }).first().waitFor({ state: 'visible' })
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })

    assert.deepEqual(writeRequests, [], 'QA menu read-only E2E must not send backend write requests after login')
    assert.deepEqual(consoleErrors, [], 'QA menu E2E must not emit console errors')
    assert.deepEqual(pageErrors, [], 'QA menu E2E must not emit page errors')

    const result = {
      ok: true,
      baseUrl: BASE_URL,
      actor: `${config.tenant}/${config.username}`,
      menuOrder: ['批记录表单', 'QA', '批次执行'],
      targetPath: TARGET_PATH,
      writeRequests,
      consoleErrors,
      pageErrors,
      screenshotPath: SCREENSHOT_PATH
    }
    writeResult(result)
    console.log(`PASS eDHR QA menu real E2E ${JSON.stringify(result)}`)
  } catch (error) {
    writeResult({
      ok: false,
      baseUrl: BASE_URL,
      actor: config.tenant && config.username ? `${config.tenant}/${config.username}` : 'missing-local-default-login',
      targetPath: TARGET_PATH,
      error: error.message,
      writeRequests,
      consoleErrors,
      pageErrors
    })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
