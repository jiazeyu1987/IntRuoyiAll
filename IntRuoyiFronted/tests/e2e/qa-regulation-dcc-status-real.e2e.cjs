const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.QA_REGULATION_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, '')
const TARGET_PATH = '/mes/pro/process-pool/qa-regulation'
const RESULT_DIR = path.resolve(WORKSPACE_ROOT, 'output', 'playwright', '20260804-qa-regulation-tab')
const RESULT_PATH = path.join(RESULT_DIR, 'qa-regulation-dcc-status-real-e2e.json')
const SCREENSHOT_PATH = path.join(RESULT_DIR, 'qa-regulation-dcc-status-real-e2e.png')
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

function resolveBrowserExecutable() {
  return CHROME_CANDIDATES.find((candidate) => fs.existsSync(candidate))
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

async function waitForProjectStatusResponse(page) {
  const response = await page.waitForResponse(
    (candidate) =>
      candidate.url().includes('/mes/qa/inspection-regulation/project-statuses') &&
      candidate.request().method() === 'GET',
    { timeout: 60000 }
  )
  assert.equal(response.ok(), true, `project-statuses HTTP status ${response.status()}`)
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `project-statuses business code ${payload.code}`)
  assert.ok(Array.isArray(payload.data), 'project-statuses response data must be an array')
  return {
    url: response.url(),
    count: payload.data.length,
    configuredCount: payload.data.filter((item) => item.configured === true).length,
    unconfiguredCount: payload.data.filter((item) => item.configured === false).length
  }
}

async function waitForDccProjectCodePageResponse(page) {
  const response = await page.waitForResponse(
    (candidate) =>
      candidate.url().includes('/dcc/project-codes/page') &&
      candidate.request().method() === 'GET',
    { timeout: 60000 }
  )
  assert.equal(response.ok(), true, `dcc project-codes page HTTP status ${response.status()}`)
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `dcc project-codes page business code ${payload.code}`)
  const list = payload.data?.list
  assert.ok(Array.isArray(list), 'dcc project-codes page response data.list must be an array')
  return {
    url: response.url(),
    count: list.length,
    productBoundCount: list.filter((item) => item.productMasterId).length,
    sample: list.slice(0, 10).map((item) => ({
      id: item.id,
      projectCode: item.projectCode,
      productMasterId: item.productMasterId ?? null
    }))
  }
}

async function selectIdiProject(page, qaPage) {
  const dccCard = qaPage.locator('[data-qa-regulation-dcc-project]').first()
  const select = dccCard.locator('.el-select').first()
  await select.waitFor({ state: 'visible' })
  await select.click()
  const input = select.locator('input[role="combobox"], input.el-select__input').first()

  const idiProjectPagePromise = waitForDccProjectCodePageResponse(page)
  await input.fill('IDI')
  const idiProjectPage = await idiProjectPagePromise
  const idiProject = idiProjectPage.sample.find((project) => project.projectCode === 'IDI')
  if (!idiProject?.productMasterId) {
    throw new Error(
      `E2E_BLOCKED_QA_DCC_PRODUCT_BINDING: IDI DCC project code id ${
        idiProject?.id || 'unknown'
      } returned productMasterId null; cannot verify backend QA project-statuses split without the formal DCC-to-product binding.`
    )
  }

  const statusResponsePromise = waitForProjectStatusResponse(page)
  const idiOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: /IDI/ }).first()
  await idiOption.waitFor({ state: 'visible' })
  await idiOption.click()
  const statusResponse = await statusResponsePromise

  await dccCard.getByText('IDI', { exact: false }).first().waitFor({ state: 'visible' })
  await qaPage.getByText('PQC-IDI-001', { exact: false }).first().waitFor({ state: 'visible' })
  const regulationNameInput = qaPage
    .locator('.el-form-item')
    .filter({ hasText: '规程名称' })
    .locator('input')
    .first()
  await regulationNameInput.waitFor({ state: 'visible' })
  assert.equal(
    await regulationNameInput.inputValue(),
    '按压式球囊扩充压力泵组装过程检验规程',
    'IDI draft regulation name must be populated from the pressure-pump template'
  )
  await qaPage.getByText('原文依据摘录', { exact: false }).first().waitFor({ state: 'visible' })
  return statusResponse
}

async function main() {
  const config = collectLoginConfig()
  const browserExecutable = resolveBrowserExecutable()
  const browser = await chromium.launch({
    headless: true,
    executablePath: browserExecutable,
    args: ['--disable-dev-shm-usage']
  })
  const consoleErrors = []
  const pageErrors = []
  const writeRequests = []
  const badResponses = []
  let captureRequests = false

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
      if (!captureRequests) {
        return
      }
      if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/')) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })
    page.on('response', (response) => {
      if (!captureRequests || !response.url().includes('/admin-api/')) {
        return
      }
      if (response.status() >= 400) {
        badResponses.push({ status: response.status(), url: response.url() })
      }
    })

    await login(page, config)
    captureRequests = true

    const firstDccProjectPagePromise = waitForDccProjectCodePageResponse(page)
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    const firstDccProjectPage = await firstDccProjectPagePromise

    const qaPage = page.locator('[data-qa-regulation-page]').first()
    await qaPage.waitFor({ state: 'visible' })
    await qaPage.getByText('QA 规程配置', { exact: false }).first().waitFor({ state: 'visible' })

    const statusSummary = qaPage.locator('[data-qa-regulation-config-status]').first()
    await statusSummary.waitFor({ state: 'visible' })
    await statusSummary.getByText('已配置 QA 规程', { exact: false }).first().waitFor({ state: 'visible' })
    await statusSummary.getByText('待配置 QA 规程', { exact: false }).first().waitFor({ state: 'visible' })
    await statusSummary.getByText('配置状态来自后台 QA 规程记录', { exact: false }).first().waitFor({
      state: 'visible'
    })
    assert.equal(
      await qaPage.locator('[data-qa-regulation-status-load-error]').count(),
      0,
      'QA status load error must not be visible during successful E2E'
    )

    const configuredRows = await qaPage
      .locator('[data-qa-regulation-configured-projects] .qa-regulation-page__project-status-row')
      .count()
    const unconfiguredRows = await qaPage
      .locator('[data-qa-regulation-unconfigured-projects] .qa-regulation-page__project-status-row')
      .count()
    assert.ok(
      configuredRows + unconfiguredRows > 0,
      'QA status split must render at least one loaded DCC project row'
    )

    const idiStatusResponse = await selectIdiProject(page, qaPage)
    await qaPage.screenshot({ path: SCREENSHOT_PATH })

    assert.deepEqual(writeRequests, [], 'QA DCC status real E2E must not send backend write requests')
    assert.deepEqual(pageErrors, [], 'QA DCC status real E2E must not emit page errors')
    assert.deepEqual(
      badResponses.filter((item) => item.url.includes('/mes/qa/inspection-regulation/project-statuses')),
      [],
      'project-statuses request must not return HTTP errors'
    )
    assert.deepEqual(consoleErrors, [], 'QA DCC status real E2E must not emit console errors')

    const result = {
      ok: true,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: `${config.tenant}/${config.username}`,
      browserExecutable: browserExecutable || 'playwright-default',
      firstDccProjectPage,
      idiStatusResponse,
      configuredRows,
      unconfiguredRows,
      writeRequests,
      badResponses,
      consoleErrors,
      pageErrors,
      screenshotPath: SCREENSHOT_PATH
    }
    writeResult(result)
    console.log(`PASS QA regulation DCC status real E2E ${JSON.stringify(result)}`)
  } catch (error) {
    writeResult({
      ok: false,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: config.tenant && config.username ? `${config.tenant}/${config.username}` : 'missing-local-default-login',
      browserExecutable: browserExecutable || 'playwright-default',
      error: error.message,
      writeRequests,
      badResponses,
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
