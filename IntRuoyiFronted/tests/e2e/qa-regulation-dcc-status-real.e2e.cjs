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
  '20260804-qa-regulation-tab'
)
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

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
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
  await form
    .locator('input[type="password"], input[placeholder="请输入密码"]')
    .first()
    .fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
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
      candidate.url().includes('/dcc/project-codes/page') && candidate.request().method() === 'GET',
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

async function waitForQaReadonlyResponse(page, endpointName) {
  const response = await page.waitForResponse(
    (candidate) =>
      candidate.url().includes(`/mes/qa/inspection-regulation/${endpointName}`) &&
      candidate.request().method() === 'GET',
    { timeout: 60000 }
  )
  assert.equal(response.ok(), true, `${endpointName} HTTP status ${response.status()}`)
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `${endpointName} business code ${payload.code}`)
  return {
    url: response.url(),
    dataType: Array.isArray(payload.data) ? 'array' : typeof payload.data,
    dataCount: Array.isArray(payload.data) ? payload.data.length : undefined,
    versionNo: payload.data?.versionNo,
    lifecycleStatus: payload.data?.lifecycleStatus
  }
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
      if (
        !['GET', 'HEAD', 'OPTIONS'].includes(request.method()) &&
        request.url().includes('/admin-api/')
      ) {
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
    await qaPage.getByText('QA检验规程', { exact: true }).first().waitFor({ state: 'visible' })
    await qaPage.getByRole('tab', { name: '通用检验规程' }).waitFor({ state: 'visible' })
    await qaPage.locator('[data-qa-regulation-qa-empty]').waitFor({ state: 'visible' })

    const idiProject = firstDccProjectPage.sample.find((project) => project.projectCode === 'IDI')
    if (!idiProject?.productMasterId) {
      throw new Error(
        `E2E_BLOCKED_QA_DCC_PRODUCT_BINDING: IDI DCC project code id ${
          idiProject?.id || 'unknown'
        } returned productMasterId null; cannot verify the formal DCC-to-product QA regulation path.`
      )
    }

    const projectStatusResponsePromise = waitForProjectStatusResponse(page)
    const publishedVersionResponsePromise = waitForQaReadonlyResponse(page, 'published-version')
    const currentResponsePromise = waitForQaReadonlyResponse(page, 'current')
    const versionsResponsePromise = waitForQaReadonlyResponse(page, 'versions')
    await page.goto(`${BASE_URL}${TARGET_PATH}?dccProjectCodeId=${idiProject.id}`, {
      waitUntil: 'domcontentloaded'
    })
    const [projectStatusResponse, publishedVersionResponse, currentResponse, versionsResponse] =
      await Promise.all([
        projectStatusResponsePromise,
        publishedVersionResponsePromise,
        currentResponsePromise,
        versionsResponsePromise
      ])
    await qaPage.waitFor({ state: 'visible' })
    await qaPage.getByText('IDI', { exact: false }).first().waitFor({ state: 'visible' })
    assert.equal(publishedVersionResponse.versionNo, currentResponse.versionNo)
    assert.equal(publishedVersionResponse.lifecycleStatus, 'PUBLISHED')
    assert.ok(
      versionsResponse.dataCount > 0,
      'QA regulation version list must contain at least one option'
    )
    await qaPage
      .locator('[data-qa-regulation-current-published-version]')
      .getByText(currentResponse.versionNo)
      .waitFor({
        state: 'visible'
      })
    const selectedVersionStatus = await qaPage
      .locator('[data-qa-regulation-selected-version-status]')
      .first()
      .innerText()
    assert.notEqual(
      selectedVersionStatus.trim(),
      '加载中',
      'selected QA regulation version status must leave loading state after readonly APIs resolve'
    )
    assert.match(
      selectedVersionStatus,
      /已发布|草稿|已退役|未配置/,
      'selected QA regulation version status must render a business lifecycle state'
    )
    const commonBindingControl = qaPage
      .locator('[data-qa-regulation-common-binding-control]')
      .first()
    await commonBindingControl.waitFor({ state: 'visible' })
    await commonBindingControl.getByText('关联通用检验规程', { exact: true }).waitFor({
      state: 'visible'
    })
    await commonBindingControl
      .locator('[data-qa-regulation-common-binding-scope]')
      .getByText('IDI')
      .waitFor({ state: 'visible' })
    const commonBindingStatus = await commonBindingControl
      .locator('[data-qa-regulation-common-binding-status]')
      .innerText()
    assert.match(
      commonBindingStatus,
      /已关联|未关联/,
      'binding status must use the formal API state'
    )

    await qaPage.getByRole('tab', { name: '通用检验规程' }).click()
    const commonHeader = qaPage.locator('[data-qa-common-layout-header]').first()
    await commonHeader.waitFor({ state: 'visible' })
    await commonHeader.getByText('通用检验规程', { exact: true }).waitFor({ state: 'visible' })
    await commonHeader.locator('[data-qa-common-set-switch]').waitFor({ state: 'visible' })
    const commonWorkspace = qaPage.locator('[data-qa-regulation-common-workspace]').first()
    await commonWorkspace.waitFor({ state: 'visible' })
    const commonPanel = commonWorkspace.locator('[data-qa-regulation-common-panel]').first()
    await commonPanel.waitFor({ state: 'visible' })
    await commonPanel.getByText('规程信息', { exact: true }).waitFor({
      state: 'visible'
    })
    await commonWorkspace
      .locator('[data-qa-common-set-standard-list]')
      .waitFor({ state: 'visible' })
    const commonSetHeaderStatus = await commonHeader
      .locator('[data-qa-common-selected-version-status]')
      .innerText()
    assert.match(
      commonSetHeaderStatus,
      /已发布|草稿|已退役|未选择版本/,
      'common regulation workspace must show the selected version status'
    )
    await qaPage.screenshot({ path: SCREENSHOT_PATH })

    assert.deepEqual(
      writeRequests,
      [],
      'QA DCC status real E2E must not send backend write requests'
    )
    assert.deepEqual(pageErrors, [], 'QA DCC status real E2E must not emit page errors')
    assert.deepEqual(
      badResponses.filter((item) => item.url.includes('/mes/qa/inspection-regulation/')),
      [],
      'QA regulation readonly requests must not return HTTP errors'
    )
    const qaConsoleErrors = consoleErrors.filter(
      (message) => message.includes('QA') || message.includes('qa')
    )
    assert.deepEqual(qaConsoleErrors, [], 'QA DCC status real E2E must not emit QA console errors')

    const result = {
      ok: true,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: `${config.tenant}/${config.username}`,
      browserExecutable: browserExecutable || 'playwright-default',
      firstDccProjectPage,
      projectStatusResponse,
      publishedVersionResponse,
      currentResponse,
      versionsResponse,
      selectedVersionStatus,
      commonSetHeaderStatus,
      commonBindingStatus,
      writeRequests,
      badResponses,
      consoleErrors,
      qaConsoleErrors,
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
      actor:
        config.tenant && config.username
          ? `${config.tenant}/${config.username}`
          : 'missing-local-default-login',
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
