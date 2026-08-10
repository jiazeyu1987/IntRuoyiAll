const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.QA_REGULATION_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
  /\/+$/,
  ''
)
const TARGET_PATH = '/mes/pro/process-pool/qa-regulation'
const RESULT_DIR = path.resolve(
  WORKSPACE_ROOT,
  'output',
  'playwright',
  '20260810-qa-regulation-configured-sort'
)
const RESULT_PATH = path.join(RESULT_DIR, 'configured-dropdown-real-e2e.json')
const SCREENSHOT_PATH = path.join(RESULT_DIR, 'configured-dropdown-real-e2e.png')
const CHROME_CANDIDATES = [
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH,
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE,
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
].filter(Boolean)

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
    tenant: env.QA_REGULATION_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.QA_REGULATION_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.QA_REGULATION_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD
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
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form
    .locator(
      'input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])'
    )
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

async function readBusinessPayload(response, label) {
  assert.equal(response.ok(), true, `${label} HTTP status ${response.status()}`)
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `${label} business code ${payload.code}`)
  return payload.data
}

async function main() {
  const config = collectLoginConfig()
  const browserExecutable = CHROME_CANDIDATES.find((candidate) => fs.existsSync(candidate))
  const browser = await chromium.launch({
    headless: true,
    executablePath: browserExecutable,
    args: ['--disable-dev-shm-usage']
  })
  const consoleErrors = []
  const pageErrors = []
  const writeRequests = []
  let captureRequests = false

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('console', (message) => {
      if (captureRequests && message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })
    page.on('pageerror', (error) => {
      if (captureRequests) {
        pageErrors.push(error.message)
      }
    })
    page.on('request', (request) => {
      if (
        captureRequests &&
        !['GET', 'HEAD', 'OPTIONS'].includes(request.method()) &&
        request.url().includes('/admin-api/')
      ) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })

    await login(page, config)
    captureRequests = true

    const projectPagePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/dcc/project-codes/page') && response.request().method() === 'GET'
    )
    const projectStatusesPromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/qa/inspection-regulation/project-statuses') &&
        response.request().method() === 'GET'
    )
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    const projectPageData = await readBusinessPayload(await projectPagePromise, 'DCC project page')
    const projectStatusData = await readBusinessPayload(
      await projectStatusesPromise,
      'QA project statuses'
    )
    assert.ok(Array.isArray(projectPageData?.list), 'DCC project page data.list must be an array')
    assert.ok(Array.isArray(projectStatusData), 'QA project statuses data must be an array')

    const qaPage = page.locator('[data-qa-regulation-page]').first()
    await qaPage.waitFor({ state: 'visible' })
    const projectSelect = qaPage.locator('[data-qa-regulation-project-dropdown]').first()
    await projectSelect.waitFor({ state: 'visible' })
    await projectSelect.click()

    const visibleOptions = page.locator(
      '.el-select-dropdown__item.qa-regulation-page__project-option:visible'
    )
    await visibleOptions.first().waitFor({ state: 'visible' })
    const optionRows = await visibleOptions.evaluateAll((options) =>
      options.map((option) => ({
        text: (option.textContent || '').replace(/\s+/g, ' ').trim(),
        configured: option.classList.contains('qa-regulation-page__project-option--configured')
      }))
    )

    assert.ok(optionRows.length > 0, 'QA project dropdown must render project options')
    const firstUnconfiguredIndex = optionRows.findIndex((option) => !option.configured)
    const lastConfiguredIndex = optionRows.findLastIndex((option) => option.configured)
    assert.ok(lastConfiguredIndex >= 0, 'QA project dropdown must contain configured options')
    assert.ok(firstUnconfiguredIndex >= 0, 'QA project dropdown must contain unconfigured options')
    assert.ok(
      lastConfiguredIndex < firstUnconfiguredIndex,
      'Every configured QA project must appear before every unconfigured project'
    )

    const pressurePump = optionRows.find((option) => /^IDI\s*\//.test(option.text))
    const balloonPressurePump = optionRows.find((option) => /^ID\s*\//.test(option.text))
    assert.ok(pressurePump, 'IDI pressure-pump project must be visible in the dropdown')
    assert.ok(balloonPressurePump, 'ID balloon pressure-pump project must be visible in the dropdown')
    assert.equal(pressurePump.configured, true, 'IDI pressure-pump project must use configured styling')
    assert.equal(
      balloonPressurePump.configured,
      true,
      'ID balloon pressure-pump project must use configured styling'
    )
    assert.ok(
      optionRows.indexOf(pressurePump) < firstUnconfiguredIndex &&
        optionRows.indexOf(balloonPressurePump) < firstUnconfiguredIndex,
      'Both pressure-pump projects must be in the configured group'
    )

    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    assert.deepEqual(writeRequests, [], 'Configured dropdown E2E must not send business write requests')
    assert.deepEqual(consoleErrors, [], 'Configured dropdown E2E must not emit console errors')
    assert.deepEqual(pageErrors, [], 'Configured dropdown E2E must not emit page errors')

    const result = {
      ok: true,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: `${config.tenant}/${config.username}`,
      projectCount: projectPageData.list.length,
      backendConfiguredCount: projectStatusData.filter((item) => item.configured === true).length,
      optionRows,
      firstUnconfiguredIndex,
      lastConfiguredIndex,
      writeRequests,
      consoleErrors,
      pageErrors,
      screenshotPath: SCREENSHOT_PATH
    }
    writeResult(result)
    console.log(`PASS QA configured dropdown real E2E ${JSON.stringify(result)}`)
  } catch (error) {
    writeResult({
      ok: false,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: config.tenant && config.username ? `${config.tenant}/${config.username}` : 'missing-login',
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
