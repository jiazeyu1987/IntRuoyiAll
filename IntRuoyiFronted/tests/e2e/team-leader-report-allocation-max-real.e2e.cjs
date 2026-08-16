const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const OUTPUT_DIR = path.resolve(WORKSPACE_ROOT, 'output/playwright/20260812-active-order-allocation-max')
const RESULT_PATH = path.join(OUTPUT_DIR, 'result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'allocation-shortcuts.png')
const FAILURE_SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'allocation-shortcuts-failure.png')
const FRONTEND_URL = 'http://127.0.0.1:8081'
const BACKEND_URL = 'http://127.0.0.1:48081'
const TARGET_PATH = '/mes/pro/process-pool/production-leader'
const SUBMISSION_ENDPOINT = '/admin-api/mes/pro/process-pool/team-leader/submission/page'
const ACTIVE_ORDER_ENDPOINT = '/admin-api/mes/pro/process-pool/team-leader/active-order/list'

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
  const env = { ...readEnvFile(path.join(FRONTEND_ROOT, '.env')), ...process.env }
  return {
    tenant: env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.VITE_APP_DEFAULT_LOGIN_PASSWORD,
    headed: env.ALLOCATION_MAX_E2E_HEADED === '1'
  }
}

function resolveChromiumExecutable() {
  for (const candidate of [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
  ]) {
    if (fs.existsSync(candidate)) return candidate
  }
  return undefined
}

async function fillFirstVisible(locator, value, label) {
  for (let index = 0; index < await locator.count(); index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible().catch(() => false)) {
      await candidate.fill(String(value))
      return
    }
  }
  throw new Error(`${label} input is not visible`)
}

async function login(page, config) {
  await page.goto(`${FRONTEND_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')
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

function toPositiveInteger(value) {
  const number = Number(value)
  return Number.isInteger(number) && number > 0 ? number : undefined
}

function selectCandidate(submissions, activeOrders) {
  for (const submission of submissions) {
    const processId = toPositiveInteger(submission.processId)
    if (!processId) continue
    const reportRemaining = Number(submission.reportUnallocatedQuantity ?? submission.outputQuantity ?? 0)
    if (!Number.isInteger(reportRemaining) || reportRemaining <= 0) continue
    for (const order of activeOrders) {
      const processMatches = (order.processRemainingQuantities || []).filter(
        (item) => Number(item.processId) === processId
      )
      if (processMatches.length !== 1 || Number(processMatches[0].remainingQuantity) <= 1) continue
      const processRemaining = processMatches[0]
      return { submission, order, processRemaining, reportRemaining }
    }
  }
  return undefined
}

async function readJson(page, pathAndQuery, requestHeaders) {
  assert.ok(requestHeaders?.authorization, 'authenticated API request headers are required')
  return await page.evaluate(async ({ url, headers }) => {
    const response = await fetch(url, { credentials: 'include', headers })
    const body = await response.json()
    return { httpStatus: response.status, body }
  }, { url: pathAndQuery, headers: requestHeaders })
}

async function discoverCandidate(page, result, requestHeaders) {
  const activeOrderResult = await readJson(page, ACTIVE_ORDER_ENDPOINT, requestHeaders)
  assert.equal(activeOrderResult.httpStatus, 200, `active-order HTTP ${activeOrderResult.httpStatus}`)
  assert.equal(activeOrderResult.body.code, 0, `active-order business code ${activeOrderResult.body.code}`)
  const activeOrders = activeOrderResult.body.data || []
  result.activeOrderResponse = {
    count: activeOrders.length,
    allRowsExposeProcessRemainingQuantities: activeOrders.every((order) =>
      Array.isArray(order.processRemainingQuantities)
    )
  }
  assert.equal(
    result.activeOrderResponse.allRowsExposeProcessRemainingQuantities,
    true,
    'active-order API does not expose processRemainingQuantities on every row'
  )

  const submissionResult = await readJson(
    page,
    `${SUBMISSION_ENDPOINT}?pageNo=1&pageSize=50&leaderType=PRODUCTION&allocationView=WORKBENCH`,
    requestHeaders
  )
  assert.equal(submissionResult.httpStatus, 200, `submission HTTP ${submissionResult.httpStatus}`)
  assert.equal(submissionResult.body.code, 0, `submission business code ${submissionResult.body.code}`)
  const submissions = submissionResult.body.data?.list || []
  result.submissionDiscovery = {
    total: Number(submissionResult.body.data?.total || 0),
    inspected: submissions.length
  }
  const candidate = selectCandidate(submissions, activeOrders)
  if (!candidate) {
    result.status = 'BLOCKED'
    result.blocker = {
      reason: '芋道源码/admin 当前没有业务工序唯一、当前工序剩余量大于 1 且报工未分配量大于 0 的可验证样本。',
      activeOrderCount: activeOrders.length,
      submissionTotal: result.submissionDiscovery.total,
      activeOrderProcessContexts: [...new Set(activeOrders.flatMap((order) =>
        (order.processRemainingQuantities || []).map((item) =>
          `${Number(item.routeProcessId)}/${Number(item.processId)}`
        )
      ))].sort(),
      submissionProcessContexts: [...new Set(submissions.map((submission) =>
        `${Number(submission.routeProcessId)}/${Number(submission.processId)}`
      ))].sort(),
      mesWrites: result.mesWriteRequests.length
    }
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    return undefined
  }
  return candidate
}

function inputValue(row) {
  return row.locator('.team-leader-workbench__allocation-quantity-input input').inputValue().then(Number)
}

async function run() {
  const config = collectConfig()
  assert.equal(config.tenant, '芋道源码', 'E2E must use 芋道源码 tenant')
  assert.equal(config.username, 'admin', 'E2E must use admin account')
  assert.ok(config.password, 'default local login password is required')
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const result = {
    status: 'RUNNING',
    tenant: config.tenant,
    username: config.username,
    frontendUrl: FRONTEND_URL,
    backendUrl: BACKEND_URL,
    targetPath: TARGET_PATH,
    activeOrderResponse: null,
    candidate: null,
    observed: null,
    mesWriteRequests: [],
    pageErrors: [],
    consoleErrors: []
  }

  const frontendResponse = await fetch(FRONTEND_URL)
  assert.equal(frontendResponse.ok, true, `frontend HTTP ${frontendResponse.status}`)
  const healthResponse = await fetch(`${BACKEND_URL}/actuator/health`)
  assert.equal(healthResponse.ok, true, `backend health HTTP ${healthResponse.status}`)
  assert.equal((await healthResponse.json()).status, 'UP', 'backend health must be UP')

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: resolveChromiumExecutable()
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  let authenticatedApiHeaders

  page.on('request', (request) => {
    const url = new URL(request.url())
    const headers = request.headers()
    if (url.pathname.startsWith('/admin-api/') && request.method() === 'GET' && headers.authorization) {
      authenticatedApiHeaders = Object.fromEntries(
        ['authorization', 'tenant-id', 'visit-tenant-id']
          .filter((name) => headers[name])
          .map((name) => [name, headers[name]])
      )
    }
    if (url.pathname.startsWith('/admin-api/mes/') && ['POST', 'PUT', 'DELETE'].includes(request.method())) {
      result.mesWriteRequests.push({ method: request.method(), path: url.pathname })
    }
  })
  page.on('pageerror', (error) => result.pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') result.consoleErrors.push(message.text())
  })
  try {
    await login(page, config)
    await page.goto(`${FRONTEND_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    const workbench = page.locator('[data-team-leader-report-workbench]').first()
    await workbench.waitFor({ state: 'visible', timeout: 60000 })
    await page.waitForFunction(() => document.readyState === 'complete', undefined, { timeout: 30000 })
    assert.ok(authenticatedApiHeaders?.authorization, 'page did not issue an authenticated API request')
    const discoveredCandidate = await discoverCandidate(page, result, authenticatedApiHeaders)
    if (!discoveredCandidate) return result

    let candidate
    for (const submission of [discoveredCandidate.submission]) {
      const button = page.locator(`[data-production-report-allocation-event-id="${submission.id}"]`)
      if (!(await button.isVisible().catch(() => false))) continue
      await button.click()
      await page.locator('[data-team-leader-allocation-table]').waitFor({ state: 'visible', timeout: 30000 })
      await page.waitForTimeout(1200)
      candidate = discoveredCandidate
    }
    assert.ok(candidate, `page does not show allocation button for discovered event ${discoveredCandidate.submission.id}`)

    const dialog = page.getByRole('dialog')
    const table = dialog.locator('[data-team-leader-allocation-table]')
    let row = table.locator('tbody tr').filter({ hasText: String(candidate.order.workOrderCode) }).first()
    if (!(await row.isVisible().catch(() => false))) {
      await dialog.getByRole('button', { name: '从空白开始' }).click()
      await dialog.getByRole('button', { name: '新增分配行' }).click()
      row = table.locator('tbody tr').last()
      const select = row.locator('.team-leader-workbench__allocation-order-select')
      await select.click()
      const option = page.locator('.team-leader-workbench__allocation-order-popper .el-select-dropdown__item:visible')
        .filter({ hasText: String(candidate.order.workOrderCode) }).first()
      await option.waitFor({ state: 'visible', timeout: 30000 })
      await option.click()
    }

    const processRemaining = Number(candidate.processRemaining.remainingQuantity)
    const reportRemaining = candidate.reportRemaining
    const expectedMax = Math.min(processRemaining, reportRemaining)
    const expectedHalf = Math.min(Math.floor(processRemaining / 2), reportRemaining)
    assert.ok(expectedMax > 0 && expectedHalf > 0, 'candidate cannot validate both shortcuts')
    await row.locator('[data-team-leader-allocation-clear]').click()
    await row.locator('[data-team-leader-allocation-max]').click()
    const actualMax = await inputValue(row)
    assert.equal(actualMax, expectedMax, 'MAX shortcut does not use current-process remaining quantity')
    await row.locator('[data-team-leader-allocation-clear]').click()
    await row.locator('[data-team-leader-allocation-half]').click()
    const actualHalf = await inputValue(row)
    assert.equal(actualHalf, expectedHalf, 'HALF shortcut does not use current-process remaining quantity')

    result.candidate = {
      eventId: Number(candidate.submission.id),
      processId: Number(candidate.submission.processId),
      routeProcessId: Number(candidate.submission.routeProcessId),
      activeOrderId: Number(candidate.order.id),
      workOrderCode: candidate.order.workOrderCode,
      plannedQuantity: Number(candidate.processRemaining.plannedQuantity),
      allocatedQuantity: Number(candidate.processRemaining.allocatedQuantity),
      processRemainingQuantity: processRemaining,
      reportRemainingQuantity: reportRemaining
    }
    result.observed = { expectedMax, actualMax, expectedHalf, actualHalf }
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    await dialog.getByRole('button', { name: '取消' }).click()
    assert.equal(result.mesWriteRequests.length, 0, 'read-only E2E sent MES write requests')
    assert.equal(result.pageErrors.length, 0, 'page errors occurred during target flow')
    assert.equal(await page.locator('.el-message--error:visible').count(), 0, 'visible error occurred')
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
    if (result.status === 'BLOCKED') {
      console.error(`BLOCKED: ${result.blocker.reason}`)
      process.exitCode = 2
      return
    }
    console.log(
      `PASS: event=${result.candidate.eventId} order=${result.candidate.workOrderCode} ` +
      `remaining=${result.candidate.processRemainingQuantity} max=${result.observed.actualMax} ` +
      `half=${result.observed.actualHalf} mesWrites=0`
    )
  })
  .catch((error) => {
    console.error(`FAIL: team leader report allocation max E2E ${error.message}`)
    process.exitCode = 1
  })
