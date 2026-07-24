const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')

const text = {
  tenant: '\u6d4b\u8bd5\u79df\u6237',
  username: 'aoteman',
  password: '111111',
  login: '\u767b\u5f55',
  workOrder: '\u751f\u4ea7\u5de5\u5355',
  scheduleOrder: '\u6392\u4ea7\u5de5\u5355',
  calendarBack: '\u8fd4\u56de\u6392\u4ea7',
  total: '\u603b\u91cf',
  done: '\u5b8c\u6210',
  remaining: '\u672a\u5b8c',
  pendingApproval: '\u5f85\u5ba1\u6279',
  pendingInspection: '\u5f85\u68c0',
  overReported: '\u8d85\u62a5',
  currentProcess: '\u5f53\u524d\u5de5\u5e8f',
  route: '\u5de5\u827a\u8def\u7ebf',
  frozen: '\u51bb\u7ed3\u72b6\u6001',
  compare: '\u5bf9\u6bd4',
  snapshot: '\u5feb\u7167',
  nightShift: '\u591c\u73ed',
  shortage: '\u77ed\u7f3a',
  locked: '\u9501\u5b9a'
}

const config = {
  baseUrl: (process.env.MES_TARGET_ALIGNMENT_BASE_URL || 'http://127.0.0.1:8137').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_TARGET_ALIGNMENT_TENANT || text.tenant,
  username: process.env.MES_TARGET_ALIGNMENT_USERNAME || text.username,
  password: process.env.MES_TARGET_ALIGNMENT_PASSWORD || text.password,
  readonlyFinal: process.env.MES_TARGET_ALIGNMENT_READONLY_FINAL === '1',
  headed: process.env.MES_TARGET_ALIGNMENT_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_TARGET_ALIGNMENT_ARTIFACT_DIR ||
      path.join(workspaceRoot, 'output', 'smart-scheduling-target-alignment-readonly')
  )
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(800)
}

async function login(page, targetPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    await page.goto(`${config.baseUrl}${targetPath}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: text.login }).click()
  ])
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${config.baseUrl}${targetPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
}

async function verifyWorkOrderPage(page) {
  await page.goto(`${config.baseUrl}/mes/pro/work-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page
    .getByText(text.workOrder, { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.sync-status-bar').waitFor({ state: 'visible', timeout: 60000 })
  const bodyText = await page.locator('body').innerText()
  for (const fragment of ['ERP Sync', 'Auto job', 'Last run', 'Created', 'Updated', 'Skipped']) {
    assert.ok(bodyText.includes(fragment), `work-order sync status must include ${fragment}`)
  }
  await page.screenshot({
    path: path.join(config.artifactDir, 'work-order-sync-status.png'),
    fullPage: true
  })
  return { syncStatusVisible: true }
}

async function verifyScheduleOrderPage(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `schedule-order page api failed: ${body.msg || body.code}`)
  assert.ok(Array.isArray(body.data?.list), 'schedule-order page api must return list')
  const first = body.data.list[0]
  assert.ok(first, 'schedule-order page must have at least one real row')
  for (const field of [
    'pendingApprovalQuantity',
    'pendingInspectionQuantity',
    'overReportedQuantity',
    'currentProcessId',
    'currentRouteProcessId',
    'freezeReason'
  ]) {
    assert.ok(
      Object.prototype.hasOwnProperty.call(first, field),
      `schedule-order row must expose ${field}`
    )
  }
  const pool = page.locator('.schedule-order-pool')
  await pool.waitFor({ state: 'visible', timeout: 60000 })
  for (const fragment of [
    text.total,
    text.done,
    text.remaining,
    text.pendingApproval,
    text.pendingInspection,
    text.overReported,
    text.currentProcess,
    text.route,
    text.frozen
  ]) {
    await pool
      .getByText(fragment, { exact: false })
      .first()
      .waitFor({ state: 'visible', timeout: 60000 })
  }
  await page.screenshot({
    path: path.join(config.artifactDir, 'schedule-order-layered-progress.png'),
    fullPage: true
  })
  return {
    rowId: first.id,
    progressPercent: first.progressPercent,
    currentRouteProcessId: first.currentRouteProcessId || null
  }
}

async function verifyScheduleCalendarPage(page) {
  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-calendar/month') &&
        response.status() === 200,
      { timeout: 60000 }
    )
    .catch(() => null)
  await page.goto(`${config.baseUrl}/mes/pro/schedule-calendar`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.schedule-calendar-page').waitFor({ state: 'visible', timeout: 60000 })
  await page
    .getByText(text.calendarBack, { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
  const response = await responsePromise
  const monthBody = response ? await response.json() : null
  if (monthBody) {
    assert.equal(
      monthBody.code,
      0,
      `schedule-calendar month api failed: ${monthBody.msg || monthBody.code}`
    )
  }
  const calendarText = await page.locator('body').innerText()
  for (const fragment of [text.nightShift, text.shortage]) {
    assert.ok(calendarText.includes(fragment), `calendar page must expose ${fragment}`)
  }
  assert.ok(
    calendarText.includes(text.locked) ||
      calendarText.includes(text.frozen) ||
      calendarText.includes(text.nightShift),
    'calendar page must expose scheduling protection or shift state text'
  )
  await page.screenshot({
    path: path.join(config.artifactDir, 'schedule-calendar-night-freeze.png'),
    fullPage: true
  })
  return { calendarVisible: true }
}

async function main() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  if (!config.readonlyFinal) {
    assert.equal(
      config.tenant,
      text.tenant,
      'target alignment E2E writes/debugs must use test tenant'
    )
  }
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const mesWriteRequests = []
  page.on('request', (request) => {
    const method = request.method()
    if (
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) &&
      request.url().includes('/admin-api/mes/')
    ) {
      mesWriteRequests.push(`${method} ${request.url()}`)
    }
  })
  try {
    await login(page, '/mes/pro/schedule-order')
    const evidence = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      workOrder: await verifyWorkOrderPage(page),
      scheduleOrder: await verifyScheduleOrderPage(page),
      scheduleCalendar: await verifyScheduleCalendarPage(page)
    }
    if (config.readonlyFinal) {
      assert.deepEqual(
        mesWriteRequests,
        [],
        `readonly final verification must not send MES write requests: ${mesWriteRequests.join(', ')}`
      )
      evidence.mesWriteRequests = mesWriteRequests
    }
    fs.writeFileSync(
      path.join(config.artifactDir, 'target-alignment-readonly-report.json'),
      `${JSON.stringify(evidence, null, 2)}\n`,
      'utf8'
    )
    console.log(JSON.stringify(evidence, null, 2))
  } finally {
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
