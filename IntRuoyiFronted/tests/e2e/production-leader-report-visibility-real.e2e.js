const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const OUTPUT_DIR = path.resolve(
  WORKSPACE_ROOT,
  'output/playwright/20260809-frontline-submit-leader-visibility'
)
const RESULT_PATH = path.join(OUTPUT_DIR, 'result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'production-leader-report-visible.png')
const FAILURE_SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'production-leader-report-failure.png')
const TARGET_PATH = '/mes/pro/process-pool/production-leader'
const SUBMISSION_ENDPOINT = '/mes/pro/process-pool/team-leader/submission/page'

function parseEnvValue(value) {
  const trimmed = String(value || '').trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {}
  const env = {}
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const equalsIndex = trimmed.indexOf('=')
    if (equalsIndex <= 0) continue
    env[trimmed.slice(0, equalsIndex).trim()] = parseEnvValue(trimmed.slice(equalsIndex + 1))
  }
  return env
}

function collectConfig() {
  const env = {
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env')),
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env.local')),
    ...process.env
  }
  return {
    frontendUrl: (env.LEADER_REPORT_E2E_FRONTEND_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
    backendUrl: (env.LEADER_REPORT_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
    tenant: env.LEADER_REPORT_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.LEADER_REPORT_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.LEADER_REPORT_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD,
    submitDate: env.LEADER_REPORT_E2E_SUBMIT_DATE || '2026-08-09',
    eventId: Number(env.LEADER_REPORT_E2E_EVENT_ID || 192),
    employeeName: env.LEADER_REPORT_E2E_EMPLOYEE_NAME || '陈丽',
    processName: env.LEADER_REPORT_E2E_PROCESS_NAME || '粗洗工序',
    outputQuantity: Number(env.LEADER_REPORT_E2E_OUTPUT_QUANTITY || 123),
    headed: env.LEADER_REPORT_E2E_HEADED === '1'
  }
}

function resolveChromiumExecutable() {
  const explicit = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  if (explicit) {
    assert.ok(fs.existsSync(explicit), `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH does not exist: ${explicit}`)
    return explicit
  }
  for (const candidate of [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
  ]) {
    if (fs.existsSync(candidate)) return candidate
  }
  return undefined
}

function assertRuntimePair(config) {
  assert.equal(config.frontendUrl, 'http://127.0.0.1:8081', 'int_main frontend must use 8081')
  assert.equal(config.backendUrl, 'http://127.0.0.1:48081', 'int_main backend must use 48081')
  assert.ok(config.tenant, 'default login tenant is required')
  assert.ok(config.username, 'default login username is required')
  assert.ok(config.password, 'default login password is required')
  assert.match(config.submitDate, /^\d{4}-\d{2}-\d{2}$/, 'submit date must use YYYY-MM-DD')
  assert.ok(Number.isInteger(config.eventId) && config.eventId > 0, 'event ID must be positive')
  assert.ok(Number.isFinite(config.outputQuantity) && config.outputQuantity >= 0, 'output quantity must be valid')
}

async function assertRuntimeReady(config) {
  const frontendResponse = await fetch(`${config.frontendUrl}/`)
  assert.equal(frontendResponse.ok, true, `frontend HTTP ${frontendResponse.status}`)
  const backendResponse = await fetch(`${config.backendUrl}/actuator/health`)
  assert.equal(backendResponse.ok, true, `backend health HTTP ${backendResponse.status}`)
  const health = await backendResponse.json()
  assert.equal(health.status, 'UP', 'backend health must be UP')
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible().catch(() => false)) {
      await candidate.fill(String(value))
      return
    }
  }
  throw new Error(`${label} input is not visible`)
}

async function login(page, config) {
  const loginUrl = new URL('/login', config.frontendUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({
      hasText: config.tenant
    }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(
      form.locator('input[placeholder="请输入租户名称"]'),
      config.tenant,
      'tenant'
    )
  }

  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(
    form.locator('input[type="password"], input[placeholder="请输入密码"]'),
    config.password,
    'password'
  )

  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(response.ok(), true, `login HTTP ${response.status()}`)
  assert.ok([0, 200].includes(body.code), `login business code ${body.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

function isTargetSubmissionResponse(response, config, reviewStatus) {
  if (response.request().method() !== 'GET') return false
  const url = new URL(response.url())
  if (url.pathname !== `/admin-api${SUBMISSION_ENDPOINT}`) return false
  if (url.searchParams.get('leaderType') !== 'PRODUCTION') return false
  if (url.searchParams.get('submitDate') !== config.submitDate) return false
  const actualReviewStatus = url.searchParams.get('submissionReviewStatus') || ''
  return reviewStatus ? actualReviewStatus === reviewStatus : actualReviewStatus === ''
}

function findTargetRow(body, config) {
  const rows = Array.isArray(body.data?.list) ? body.data.list : []
  return rows.find((row) => Number(row.id) === config.eventId)
}

function visibleTab(page, label) {
  return page.locator('.el-tabs__item:visible').filter({
    hasText: new RegExp(`^\\s*${label}\\s*$`)
  }).first()
}

async function run() {
  const config = collectConfig()
  assertRuntimePair(config)
  await assertRuntimeReady(config)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })

  const result = {
    status: 'RUNNING',
    frontendUrl: config.frontendUrl,
    backendUrl: config.backendUrl,
    targetPath: TARGET_PATH,
    tenant: config.tenant,
    username: config.username,
    submitDate: config.submitDate,
    eventId: config.eventId,
    employeeName: config.employeeName,
    processName: config.processName,
    outputQuantity: config.outputQuantity,
    managementResponse: null,
    historyResponse: null,
    visibleManagementRow: '',
    observedSubmissionRequests: [],
    mesWriteRequests: [],
    targetRequestFailures: [],
    nonTargetLocalFailures: [],
    pageErrors: [],
    consoleErrors: []
  }

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: resolveChromiumExecutable()
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()

  page.on('request', (request) => {
    const url = new URL(request.url())
    if (
      url.pathname.startsWith('/admin-api/mes/') &&
      ['POST', 'PUT', 'DELETE'].includes(request.method())
    ) {
      result.mesWriteRequests.push({ method: request.method(), path: url.pathname })
    }
  })
  page.on('response', (response) => {
    const url = new URL(response.url())
    if (response.request().method() === 'GET' && url.pathname === `/admin-api${SUBMISSION_ENDPOINT}`) {
      result.observedSubmissionRequests.push({
        method: 'GET',
        path: url.pathname,
        query: Object.fromEntries(url.searchParams.entries()),
        httpStatus: response.status()
      })
    }
    const isLocalApi =
      url.pathname.startsWith('/admin-api/') &&
      [new URL(config.frontendUrl).host, new URL(config.backendUrl).host].includes(url.host)
    if (!isLocalApi) return
    if (response.status() >= 400) {
      result.nonTargetLocalFailures.push({
        method: response.request().method(),
        path: url.pathname,
        httpStatus: response.status()
      })
      return
    }
    response.json().then((body) => {
      if (body && ![0, 200].includes(body.code)) {
        result.nonTargetLocalFailures.push({
          method: response.request().method(),
          path: url.pathname,
          httpStatus: response.status(),
          businessCode: body.code,
          message: body.msg || body.message || ''
        })
      }
    }).catch(() => undefined)
  })
  page.on('requestfailed', (request) => {
    const url = new URL(request.url())
    if (url.pathname.startsWith('/admin-api/mes/pro/process-pool/team-leader/')) {
      result.targetRequestFailures.push({
        method: request.method(),
        path: url.pathname,
        errorText: request.failure()?.errorText || 'unknown'
      })
    }
  })
  page.on('pageerror', (error) => result.pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') result.consoleErrors.push(message.text())
  })

  try {
    await login(page, config)
    await page.goto(`${config.frontendUrl}${TARGET_PATH}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.locator('[data-production-leader-workbench-page]').waitFor({
      state: 'visible',
      timeout: 60000
    })

    const [, managementResponse] = await Promise.all([
      visibleTab(page, '报工管理').click(),
      page.waitForResponse(
        (response) => isTargetSubmissionResponse(response, config),
        { timeout: 30000 }
      )
    ])
    const managementBody = await managementResponse.json()
    assert.equal(managementResponse.ok(), true, `management HTTP ${managementResponse.status()}`)
    assert.equal(managementBody.code, 0, `management business code ${managementBody.code}`)
    const targetRow = findTargetRow(managementBody, config)
    assert.ok(targetRow, `management response does not contain event ${config.eventId}`)
    assert.equal(targetRow.actualEmployeeUserName, config.employeeName, 'management employee mismatch')
    assert.equal(targetRow.processName, config.processName, 'management process mismatch')
    assert.equal(Number(targetRow.outputQuantity), config.outputQuantity, 'management output quantity mismatch')
    result.managementResponse = {
      httpStatus: managementResponse.status(),
      businessCode: managementBody.code,
      total: Number(managementBody.data?.total || 0),
      eventId: Number(targetRow.id),
      sourceFeedbackId: Number(targetRow.sourceFeedbackId || 0)
    }

    const managementSection = page.locator('[data-team-leader-report-workbench]:visible').first()
    await managementSection.waitFor({ state: 'visible', timeout: 30000 })
    const visibleRow = managementSection
      .locator('.el-table__body-wrapper tbody tr:visible')
      .filter({ hasText: config.employeeName })
      .filter({ hasText: config.processName })
      .first()
    await visibleRow.waitFor({ state: 'visible', timeout: 30000 })
    result.visibleManagementRow = (await visibleRow.innerText()).replace(/\s+/g, ' ').trim()
    assert.match(
      result.visibleManagementRow,
      new RegExp(`${config.outputQuantity}(?:\\.0+)?\\s*件`),
      'visible management row does not show the expected completion quantity'
    )
    await managementSection.locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 30000 })
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })

    const [, historyResponse] = await Promise.all([
      visibleTab(page, '报工历史').click(),
      page.waitForResponse(
        (response) => isTargetSubmissionResponse(response, config, 'APPROVED'),
        { timeout: 30000 }
      )
    ])
    const historyBody = await historyResponse.json()
    assert.equal(historyResponse.ok(), true, `history HTTP ${historyResponse.status()}`)
    assert.equal(historyBody.code, 0, `history business code ${historyBody.code}`)
    assert.equal(findTargetRow(historyBody, config), undefined, 'unreviewed event appeared in report history')
    result.historyResponse = {
      httpStatus: historyResponse.status(),
      businessCode: historyBody.code,
      total: Number(historyBody.data?.total || 0),
      targetEventPresent: false
    }

    const historyTargetRows = managementSection
      .locator('.el-table__body-wrapper tbody tr:visible')
      .filter({ hasText: config.employeeName })
      .filter({ hasText: config.processName })
    assert.equal(await historyTargetRows.count(), 0, 'unreviewed target row is visible in report history')
    await page.waitForTimeout(1000)

    assert.equal(result.mesWriteRequests.length, 0, 'read-only E2E sent MES write requests')
    assert.equal(result.targetRequestFailures.length, 0, 'target team-leader request failed')
    const targetLocalFailures = result.nonTargetLocalFailures.filter((item) =>
      item.path.startsWith('/admin-api/mes/pro/process-pool/team-leader/')
    )
    assert.equal(targetLocalFailures.length, 0, 'target team-leader response failed')
    if (result.pageErrors.length > 0) {
      assert.ok(
        result.nonTargetLocalFailures.length > 0,
        'pageerror occurred without a captured non-target local API failure'
      )
      assert.ok(
        result.pageErrors.every((message) => message === 'AxiosError'),
        'unexpected pageerror occurred during target flow'
      )
      assert.ok(
        result.consoleErrors.every((message) =>
          message.includes('审批待办数量加载失败') ||
          message.includes('个人工作台待处理数量加载失败')
        ),
        'console error belongs to the target flow or has no explicit classification'
      )
      result.nonTargetErrorClassification = {
        pageErrorsExplained: true,
        reason: 'Approval and personal-workbench badge requests failed outside the MES team-leader report flow.'
      }
    }
    assert.equal(
      await page.locator('.el-message--error:visible').count(),
      0,
      'visible error message occurred during target flow'
    )

    result.status = 'PASS'
    result.screenshot = SCREENSHOT_PATH
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    return result
  } catch (error) {
    result.status = 'FAIL'
    result.error = { name: error.name || 'Error', message: error.message || String(error) }
    try {
      await page.screenshot({ path: FAILURE_SCREENSHOT_PATH, fullPage: true })
      result.screenshot = FAILURE_SCREENSHOT_PATH
    } catch {
      result.screenshot = null
    }
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    throw error
  } finally {
    await browser.close()
  }
}

run()
  .then((result) => {
    console.log(
      `PASS: production leader report event=${result.eventId} employee=${result.employeeName} ` +
      `process=${result.processName} quantity=${result.outputQuantity} historyPresent=false mesWrites=0`
    )
  })
  .catch((error) => {
    console.error(`FAIL: production leader report visibility ${error.message}`)
    process.exitCode = 1
  })
