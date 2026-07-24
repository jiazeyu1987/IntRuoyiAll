const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_GANTT_REPLAN_REFRESH_BASE_URL || 'http://localhost:8081').replace(
    /\/+$/,
    ''
  ),
  backendUrl: (
    process.env.MES_GANTT_REPLAN_REFRESH_BACKEND_URL || 'http://127.0.0.1:48081'
  ).replace(/\/+$/, ''),
  tenant: process.env.MES_GANTT_REPLAN_REFRESH_TENANT || '测试租户',
  username: process.env.MES_GANTT_REPLAN_REFRESH_USERNAME || 'aoteman',
  password: process.env.MES_GANTT_REPLAN_REFRESH_PASSWORD || '111111',
  headed: process.env.MES_GANTT_REPLAN_REFRESH_HEADED === '1',
  artifactDir:
    process.env.MES_GANTT_REPLAN_REFRESH_ARTIFACT_DIR ||
    path.resolve('tests/output/mes-pro-task-gantt-replan-refresh-real')
}

function assertLocalTestTenant() {
  const parsed = new URL(config.baseUrl)
  assert.ok(
    ['localhost', '127.0.0.1', '::1', '[::1]'].includes(parsed.hostname),
    `真实重排刷新 E2E 只允许本机入口，当前为 ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', '真实重排刷新 E2E 只能使用测试租户')
}

function todayText(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function settle(page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return item
    }
  }
  throw new Error(`没有找到可见输入框: ${label}`)
}

async function readJsonResponse(page, response, label) {
  try {
    return await response.json()
  } catch (error) {
    if (!String(error?.message || '').includes('No resource with given identifier')) {
      throw error
    }
    const replay = await page.request.get(response.url(), { headers: await authHeaders(page) })
    assert.equal(replay.status(), 200, `${label} replay HTTP ${replay.status()}`)
    return replay.json()
  }
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/task')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(form.locator('input.el-input__inner'), config.tenant, 'tenant')
  }
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):visible'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')

  const loginButton = form.getByRole('button', { name: '登录' }).first()
  await loginButton.waitFor({ state: 'visible', timeout: 60000 })
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginButton.click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `登录 HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginBody.code), `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  assert.ok(loginBody.data?.accessToken, '登录响应必须包含 accessToken')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function readWebStorageValue(page, key) {
  return await page.evaluate((cacheKey) => {
    const raw = window.localStorage.getItem(cacheKey)
    if (!raw) return null
    try {
      const wrapper = JSON.parse(raw)
      if (wrapper && Object.prototype.hasOwnProperty.call(wrapper, 'v')) {
        return JSON.parse(wrapper.v)
      }
      return wrapper
    } catch {
      return raw
    }
  }, key)
}

async function authHeaders(page) {
  const accessToken = await readWebStorageValue(page, 'ACCESS_TOKEN')
  const tenantId = await readWebStorageValue(page, 'tenantId')
  assert.ok(accessToken, '已登录上下文缺少 ACCESS_TOKEN')
  assert.ok(tenantId, '已登录上下文缺少 tenantId')
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
}

async function apiGetJson(page, url, params = {}) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  }
  const response = await page.request.get(
    `${config.backendUrl}/admin-api${url}${search.size ? `?${search.toString()}` : ''}`,
    { headers: await authHeaders(page) }
  )
  const body = await response.json()
  assert.equal(response.status(), 200, `${url} HTTP ${response.status()}`)
  assert.equal(body.code, 0, `${url} 业务错误: ${body.msg || JSON.stringify(body)}`)
  return body.data
}

async function getCandidateRows(page) {
  const data = await apiGetJson(page, '/mes/pro/schedule-order/page', {
    pageNo: 1,
    pageSize: 100,
    completionFilter: 'INCOMPLETE'
  })
  return (data?.list || []).filter(
    (row) =>
      row &&
      row.id &&
      row.workOrderId &&
      !row.frozen &&
      !row.manualFinished &&
      Number(row.uncompletedQuantity ?? row.quantity ?? 0) > 0
  )
}

async function canUseForReplan(page, row, startTime) {
  const request = {
    scopeType: 'SELECTED',
    scheduleOrderIds: [row.id],
    startTime,
    runtimeCapacityBasis: 'PLANNED',
    preserveManualLockedTasks: true
  }
  const preflight = await page.request.post(
    `${config.backendUrl}/admin-api/mes/pro/schedule-order/preflight`,
    {
      headers: await authHeaders(page),
      data: {
        scopeType: request.scopeType,
        scheduleOrderIds: request.scheduleOrderIds,
        includeAdmissionDiff: false,
        startTime: request.startTime,
        capacityMode: request.runtimeCapacityBasis
      }
    }
  )
  const preflightBody = await preflight.json()
  if (preflight.status() !== 200 || preflightBody.code !== 0 || preflightBody.data?.result === 'BLOCKED') {
    return {
      ok: false,
      reason: `preflight:${preflight.status()}:${preflightBody.msg || preflightBody.data?.result || preflightBody.code}`
    }
  }
  const preview = await page.request.post(
    `${config.backendUrl}/admin-api/mes/pro/auto-schedule/replan/preview`,
    { headers: await authHeaders(page), data: request }
  )
  const previewBody = await preview.json()
  const blockingIssues = previewBody.data?.issues?.filter((issue) => issue.severity === 'BLOCKING') || []
  if (
    preview.status() === 200 &&
    previewBody.code === 0 &&
    previewBody.data?.calendarContextToken &&
    (previewBody.data?.summary?.blockingIssueCount ?? 0) === 0 &&
    blockingIssues.length === 0
  ) {
    return { ok: true }
  }
  return {
    ok: false,
    reason: `preview:${preview.status()}:${previewBody.msg || blockingIssues.map((issue) => issue.message || issue.code).join('|') || previewBody.code}`
  }
}

async function chooseSchedulableRow(page, startTime) {
  const candidates = await getCandidateRows(page)
  const checked = []
  for (const row of candidates.slice(0, 25)) {
    const check = await canUseForReplan(page, row, startTime)
    checked.push({ id: row.id, code: row.code, ok: check.ok, reason: check.reason || '' })
    if (check.ok) {
      return { row, checked }
    }
  }
  throw new Error(
    `没有找到可安全复用的测试租户重排样本，checked=${JSON.stringify(checked)}`
  )
}

async function openTaskPage(page, ganttResponses) {
  const ganttResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/task/gantt-list') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/task`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await ganttResponsePromise
  await page.locator('.gantt_container').first().waitFor({ state: 'visible', timeout: 60000 })
  return ganttResponses.length
}

async function navigateToScheduleOrderWithinSpa(page) {
  const beforeNavigationCount = await page.evaluate(
    () => performance.getEntriesByType('navigation').length
  )
  const scheduleOrderResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.locator('.el-menu-item, .el-sub-menu__title, a').filter({ hasText: '排产工单' }).first().click()
  const body = await readJsonResponse(page, await scheduleOrderResponsePromise, '排产工单列表')
  assert.equal(body.code, 0, `排产工单列表业务错误: ${body.msg || body.code}`)
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 30000 })
  const afterNavigationCount = await page.evaluate(
    () => performance.getEntriesByType('navigation').length
  )
  assert.equal(
    afterNavigationCount,
    beforeNavigationCount,
    '必须通过同一 SPA 实例内的菜单路由进入排产工单，不能整页重载'
  )
}

async function selectRowByCode(page, row) {
  const scheduleCodeInput = await fillFirstVisible(
    page.locator('input[placeholder="请输入排产工单号"]'),
    row.code,
    'schedule order code'
  )
  const workOrderCodeInputs = page.locator('input[placeholder="请输入工单编码"]')
  for (let index = 0; index < (await workOrderCodeInputs.count()); index += 1) {
    const input = workOrderCodeInputs.nth(index)
    if (await input.isVisible()) {
      await input.fill('')
      break
    }
  }
  const [listResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
        response.status() === 200,
      { timeout: 60000 }
    ),
    scheduleCodeInput.press('Enter')
  ])
  const listBody = await readJsonResponse(page, listResponse, '排产工单筛选列表')
  assert.equal(listBody.code, 0, `排产工单筛选接口业务错误: ${listBody.msg || listBody.code}`)
  const filteredRows = listBody.data?.list || []
  const rowIndex = filteredRows.findIndex((item) => Number(item.id) === Number(row.id))
  assert.notEqual(
    rowIndex,
    -1,
    `排产编码 ${row.code} 筛选后未返回目标工单，返回 ${filteredRows.map((item) => item.code).join(',')}`
  )
  await settle(page)

  const tableRow = page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr').nth(rowIndex)
  await tableRow.waitFor({ state: 'visible', timeout: 30000 })
  await tableRow.locator('.el-checkbox').first().click()
}

async function applyReplanThroughUi(page, row, startDate, expectedStartTime) {
  await selectRowByCode(page, row)
  await page.getByRole('button', { name: /手动重排/ }).first().click()
  const drawer = page.locator('.el-drawer').filter({ hasText: '排产前检查 / 手动重排' }).first()
  await drawer.waitFor({ state: 'visible', timeout: 30000 })
  await drawer.getByRole('button', { name: /开始重排/ }).first().click()

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '开始重排日期' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const startDateInput = dialog.locator('input[placeholder="请选择开始日期"]').first()
  await startDateInput.fill(startDate)
  await startDateInput.press('Enter')
  await page.keyboard.press('Tab')
  await page
    .locator('.el-picker__popper:visible, .el-popper:visible')
    .waitFor({ state: 'hidden', timeout: 5000 })
    .catch(() => {})
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  const [applyResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/auto-schedule/replan/apply') &&
        response.request().method() === 'POST',
      { timeout: 180000 }
    ),
    dialog.getByRole('button', { name: /确认开始重排|确认应用重排|确定/ }).last().click()
  ])
  const applyBody = await applyResponse.json()
  assert.equal(applyResponse.status(), 200, `${startDate} 应用重排 HTTP ${applyResponse.status()}`)
  assert.equal(applyBody.code, 0, `${startDate} 应用重排业务错误: ${applyBody.msg || JSON.stringify(applyBody)}`)
  const applyRequest = applyResponse.request().postDataJSON()
  assert.equal(applyRequest.startTime, expectedStartTime, '应用重排必须使用二次确认中的整日起排时间')
  assert.ok(applyRequest.calendarContextToken, '应用重排请求必须带本次预览的 calendarContextToken')
  return applyBody.data
}

async function run() {
  assertLocalTestTenant()
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage', '--no-sandbox']
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  const pageErrors = []
  const consoleErrors = []
  const ganttResponses = []
  const scheduleOrderResponses = []

  page.on('pageerror', (error) => {
    pageErrors.push(String(error.stack || error.message || error))
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('response', (response) => {
    const url = response.url()
    if (
      url.includes('/admin-api/mes/pro/task/gantt-list') &&
      response.request().method() === 'GET'
    ) {
      ganttResponses.push({ url, status: response.status(), at: Date.now() })
    }
    if (
      url.includes('/admin-api/mes/pro/schedule-order/page') &&
      response.request().method() === 'GET'
    ) {
      scheduleOrderResponses.push({ url, status: response.status(), at: Date.now() })
    }
  })

  try {
    await login(page)
    const initialGanttCount = await openTaskPage(page, ganttResponses)
    const startDate = todayText(1)
    const expectedStartTime = `${startDate} 00:00:00`
    const { row, checked } = await chooseSchedulableRow(page, expectedStartTime)
    await navigateToScheduleOrderWithinSpa(page)
    const applyStartedAt = Date.now()
    const ganttRefreshResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/task/gantt-list') &&
        response.request().method() === 'GET',
      { timeout: 90000 }
    ).then(
      (response) => ({ response }),
      (error) => ({ error })
    )
    const applyResult = await applyReplanThroughUi(page, row, startDate, expectedStartTime)
    const ganttRefreshResult = await ganttRefreshResponsePromise
    if (ganttRefreshResult.error) {
      throw new Error(
        `应用重排成功后未观察到已打开甘特图页自动刷新: ${ganttRefreshResult.error.message}`
      )
    }
    const ganttRefreshResponse = ganttRefreshResult.response
    const ganttRefreshBody = await readJsonResponse(page, ganttRefreshResponse, '重排后甘特图刷新')
    assert.equal(ganttRefreshBody.code, 0, `重排后甘特图刷新业务错误: ${ganttRefreshBody.msg || ganttRefreshBody.code}`)
    await settle(page)

    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      selectedScheduleOrder: {
        id: row.id,
        code: row.code,
        workOrderId: row.workOrderId,
        erpWorkOrderCode: row.erpWorkOrderCode
      },
      checkedCandidateCount: checked.length,
      applyResult,
      ganttRefresh: {
        status: ganttRefreshResponse.status(),
        rowCount: Array.isArray(ganttRefreshBody.data) ? ganttRefreshBody.data.length : null
      },
      initialGanttCount,
      finalGanttCount: ganttResponses.length,
      ganttResponsesAfterApply: ganttResponses.filter((item) => item.at >= applyStartedAt),
      scheduleOrderResponsesAfterApply: scheduleOrderResponses.filter((item) => item.at >= applyStartedAt),
      pageErrors,
      consoleErrors
    }
    fs.writeFileSync(
      path.join(config.artifactDir, 'report.json'),
      `${JSON.stringify(result, null, 2)}\n`,
      'utf8'
    )
    console.log(JSON.stringify(result, null, 2))
    assert.ok(
      result.ganttResponsesAfterApply.length > 0,
      '重排成功后已打开的当前排产甘特图必须自动请求 gantt-list'
    )
    assert.equal(pageErrors.length, 0, `页面运行错误必须为 0: ${JSON.stringify(pageErrors, null, 2)}`)
    assert.equal(
      consoleErrors.some((item) => item.includes('[MES] 应用重排失败')),
      false,
      `页面不得出现应用重排失败日志: ${JSON.stringify(consoleErrors, null, 2)}`
    )
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error.stack || error.message || error)
  process.exit(1)
})
