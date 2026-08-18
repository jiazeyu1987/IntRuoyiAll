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
const TARGET_DCC_PROJECT_CODE_ID = Number(process.env.QA_REGULATION_E2E_DCC_PROJECT_CODE_ID || 147)
const RESULT_DIR = path.resolve(
  WORKSPACE_ROOT,
  'output',
  'playwright',
  '20260818-qa-item-inspection-display'
)
const RESULT_PATH = path.join(RESULT_DIR, 'result.json')
const SCREENSHOT_PATH = path.join(RESULT_DIR, 'qa-item-inspection-display.png')
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
    if (equalsIndex > 0) {
      env[trimmed.slice(0, equalsIndex).trim()] = parseEnvValue(trimmed.slice(equalsIndex + 1))
    }
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

function formatDccProjectCodeOption(project) {
  return [project.projectCode, project.projectName, project.docControlNo]
    .filter(Boolean)
    .join(' / ')
    .trim()
}

async function readBusinessData(response, label) {
  assert.equal(response.ok(), true, `${label} HTTP status ${response.status()}`)
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `${label} business code ${payload.code}`)
  return payload.data
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
    const option = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await option.waitFor({ state: 'visible' })
    await option.click()
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

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  await readBusinessData(await responsePromise, 'login')
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

async function selectProject(page, qaPage, project) {
  const currentResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/qa/inspection-regulation/current') &&
      response.url().includes(`dccProjectCodeId=${project.id}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const projectSelect = qaPage.locator('[data-qa-regulation-project-dropdown]').first()
  await projectSelect.click()
  const optionLabel = formatDccProjectCodeOption(project)
  const option = page
    .locator('.el-select-dropdown__item.qa-regulation-page__project-option:visible')
    .filter({ hasText: optionLabel })
    .first()
  await option.waitFor({ state: 'visible' })
  await option.click()
  const configuration = await readBusinessData(await currentResponsePromise, `QA current ${project.id}`)
  await qaPage
    .locator('[data-qa-regulation-configuration-status]')
    .filter({ hasText: /草稿|已发布|未配置/ })
    .waitFor({ state: 'visible' })
  return configuration
}

async function readVisibleItemRows(qaPage) {
  return await qaPage.locator('[data-qa-regulation-items] .el-table__body tbody tr').evaluateAll((rows) =>
    rows.map((row) => {
      const values = Array.from(row.querySelectorAll('input')).map((input) => input.value)
      const readRule = (selector, valueLabel) => {
        const root = row.querySelector(selector)
        const switchInput = root?.querySelector('input[role="switch"], input[type="checkbox"]')
        const valueInput = root?.querySelector(`input[aria-label="${valueLabel}"]`)
        return {
          enabled: Boolean(switchInput?.checked),
          value: valueInput ? Number(valueInput.value) : undefined
        }
      }
      return {
        values,
        first: readRule('[data-qa-regulation-first-inspection]', '首检固定数量'),
        patrol: readRule('[data-qa-regulation-patrol-inspection]', '巡检比例')
      }
    })
  )
}

function findItem(configuration, processName, itemName) {
  const process = configuration.processes.find((candidate) => candidate.processName === processName)
  assert.ok(process, `backend QA process ${processName} is required`)
  const item = process.items.find((candidate) => candidate.itemName === itemName)
  assert.ok(item, `backend QA item ${processName}/${itemName} is required`)
  return item
}

function assertItemRow(rows, processName, itemName, backendItem) {
  const row = rows.find((candidate) =>
    candidate.values.includes(processName) && candidate.values.includes(itemName)
  )
  assert.ok(row, `QA page row ${processName}/${itemName} must be visible`)
  assert.equal(row.first.enabled, backendItem.applicableInspectionTypes.includes('FIRST'))
  assert.equal(row.first.value, Number(backendItem.firstInspectionQuantity))
  assert.equal(row.patrol.enabled, backendItem.applicableInspectionTypes.includes('PATROL'))
  assert.equal(row.patrol.value, Number(backendItem.patrolInspectionRatio))
  return row
}

async function main() {
  const config = collectLoginConfig()
  const browserExecutable = CHROME_CANDIDATES.find((candidate) => fs.existsSync(candidate))
  assert.ok(browserExecutable, 'local Chrome or Edge executable is required for real E2E')
  const browser = await chromium.launch({
    headless: true,
    executablePath: browserExecutable,
    args: ['--disable-dev-shm-usage']
  })
  const consoleErrors = []
  const pageErrors = []
  const writeRequests = []
  const badTargetResponses = []
  const projectPages = []
  let projectStatuses
  let captureRequests = false

  try {
    const context = await browser.newContext({ viewport: { width: 1600, height: 1000 } })
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
        request.url().includes('/admin-api/') &&
        !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
      ) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })
    page.on('response', async (response) => {
      if (!captureRequests || !response.url().includes('/admin-api/')) {
        return
      }
      if (response.status() >= 400 && response.url().includes('/mes/qa/inspection-regulation/')) {
        badTargetResponses.push({ status: response.status(), url: response.url() })
      }
      if (response.url().includes('/dcc/project-codes/page') && response.request().method() === 'GET') {
        const data = await readBusinessData(response, 'DCC project page')
        projectPages.push(...(data?.list || []))
      }
    })

    await login(page, config)
    await page.evaluate(() =>
      localStorage.removeItem('int-ruoyi:qa-regulation:last-dcc-project-code-id')
    )
    captureRequests = true
    const projectStatusesResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/qa/inspection-regulation/project-statuses') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    projectStatuses = await readBusinessData(
      await projectStatusesResponsePromise,
      'QA project statuses'
    )

    const qaPage = page.locator('[data-qa-regulation-page]').first()
    await qaPage.waitFor({ state: 'visible' })
    await qaPage.locator('[data-qa-regulation-project-dropdown]:not(.is-loading)').waitFor({
      state: 'visible'
    })
    assert.ok(Array.isArray(projectStatuses), 'QA project statuses must load from backend')

    for (let attempt = 0; attempt < 100 && projectPages.length === 0; attempt += 1) {
      await page.waitForTimeout(50)
    }

    const targetProject = projectPages.find(
      (project) => Number(project.id) === TARGET_DCC_PROJECT_CODE_ID
    )
    assert.ok(targetProject, `DCC project code ${TARGET_DCC_PROJECT_CODE_ID} must be available`)
    const targetStatus = projectStatuses.find(
      (status) => Number(status.dccProjectCodeId) === TARGET_DCC_PROJECT_CODE_ID
    )
    assert.equal(targetStatus?.configured, true, 'target DCC project must have backend QA configuration')

    const configured = await selectProject(page, qaPage, targetProject)
    assert.ok(configured, 'configured DCC project must return backend QA configuration')
    const appearance = findItem(configured, '组装Ⅰ', '外观')
    const pressureRelease = findItem(configured, '组装Ⅰ', '撤压')
    assert.equal(Number(appearance.firstInspectionQuantity), 13)
    assert.equal(Number(pressureRelease.firstInspectionQuantity), 5)

    await qaPage.getByRole('tab', { name: '检验项目', exact: true }).click()
    await qaPage.locator('[data-qa-regulation-items] .el-table__body tbody tr').first().waitFor({
      state: 'visible'
    })
    const rows = await readVisibleItemRows(qaPage)
    const appearanceRow = assertItemRow(rows, '组装Ⅰ', '外观', appearance)
    const pressureReleaseRow = assertItemRow(rows, '组装Ⅰ', '撤压', pressureRelease)
    assert.equal(
      await qaPage.locator('[data-qa-regulation-final-inspection-switch]').count(),
      1,
      'final inspection applicability must remain one project-level switch'
    )
    await qaPage.screenshot({ path: SCREENSHOT_PATH })

    const unconfiguredStatus = projectStatuses.find((status) => status.configured === false)
    assert.ok(unconfiguredStatus, 'an unconfigured backend DCC project is required for empty-state E2E')
    const unconfiguredProject = projectPages.find(
      (project) => Number(project.id) === Number(unconfiguredStatus.dccProjectCodeId)
    )
    assert.ok(unconfiguredProject, 'unconfigured DCC project must be present in the page options')
    const unconfigured = await selectProject(page, qaPage, unconfiguredProject)
    assert.equal(unconfigured, null, 'unconfigured DCC project current QA response must be null')
    await qaPage
      .locator('[data-qa-regulation-configuration-status]')
      .filter({ hasText: '未配置' })
      .waitFor({ state: 'visible' })
    await qaPage.getByText('当前 DCC 项目未配置 QA 规程', { exact: true }).first().waitFor({
      state: 'visible'
    })
    assert.equal(
      await qaPage.locator('[data-qa-regulation-items] .el-table__body tbody tr').count(),
      0,
      'unconfigured DCC project must not render inspection item rows'
    )
    await qaPage.getByRole('tab', { name: '任务预览', exact: true }).click()
    await qaPage
      .getByText('当前没有可预览的检验任务', { exact: true })
      .first()
      .waitFor({ state: 'visible' })

    assert.deepEqual(writeRequests, [], 'read-only QA display E2E must not send business writes')
    assert.deepEqual(badTargetResponses, [], 'QA target APIs must not return HTTP errors')
    assert.deepEqual(pageErrors, [], 'QA display E2E must not emit page errors')
    assert.deepEqual(consoleErrors, [], 'QA display E2E must not emit console errors')

    const result = {
      ok: true,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: `${config.tenant}/${config.username}`,
      targetDccProjectCodeId: TARGET_DCC_PROJECT_CODE_ID,
      appearance: { backend: appearance, page: appearanceRow },
      pressureRelease: { backend: pressureRelease, page: pressureReleaseRow },
      unconfiguredDccProjectCodeId: unconfiguredProject.id,
      finalInspectionApplicable: configured.finalInspectionApplicable,
      writeRequests,
      badTargetResponses,
      consoleErrors,
      pageErrors,
      screenshotPath: SCREENSHOT_PATH
    }
    writeResult(result)
    console.log(`PASS QA item inspection display real E2E ${JSON.stringify(result)}`)
  } catch (error) {
    writeResult({
      ok: false,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: config.tenant && config.username ? `${config.tenant}/${config.username}` : 'missing-login',
      targetDccProjectCodeId: TARGET_DCC_PROJECT_CODE_ID,
      error: error.message,
      writeRequests,
      badTargetResponses,
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
