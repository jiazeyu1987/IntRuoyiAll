const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (
  process.env.MES_PARTIAL_REPLAN_E2E_BASE_URL || 'http://127.0.0.1:8081'
).replace(/\/+$/, '')
const BACKEND_URL = (
  process.env.MES_PARTIAL_REPLAN_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
).replace(/\/+$/, '')

const TENANT = process.env.MES_PARTIAL_REPLAN_E2E_TENANT || '芋道源码'
const USERNAME = process.env.MES_PARTIAL_REPLAN_E2E_USERNAME || 'admin'
const PASSWORD = process.env.MES_PARTIAL_REPLAN_E2E_PASSWORD || process.env.MES_REPLAN_E2E_PASSWORD || ''
const EXPECTED_TENANT_ID = '1'
const TASK_MARKER = `E2E_PARTIAL_REPLAN_BLOCKER_${new Date()
  .toISOString()
  .replace(/[-:.TZ]/g, '')
  .slice(0, 14)}`
const ISSUE_MESSAGE = `${TASK_MARKER} 自动重排局部阻断红行验证`
const RESOLUTION_REASON = `${TASK_MARKER} cleanup`

const READ_EQUIVALENT_MES_POST_PATHS = new Set(['/admin-api/mes/pro/schedule-order/page'])
const EXPECTED_MES_MUTATION_KEYS = [
  'POST /admin-api/mes/pro/auto-schedule/issues',
  'PUT /admin-api/mes/pro/auto-schedule/issues/resolve'
]

function assertLocalOnly() {
  assert.match(BASE_URL, /^http:\/\/(127\.0\.0\.1|localhost):8081$/, 'E2E must use int_main local frontend 8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'E2E must use int_main local backend 48081')
  assert.equal(TENANT, '芋道源码', 'fixture E2E must use the user-authorized 芋道源码 tenant')
  assert.equal(USERNAME, 'admin', 'fixture E2E must use the user-authorized admin account')
  assert.ok(
    PASSWORD,
    'missing explicit test credential env MES_PARTIAL_REPLAN_E2E_PASSWORD or MES_REPLAN_E2E_PASSWORD'
  )
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible ${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/mes/pro/schedule-calendar`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  const captchaCount = await loginForm
    .locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]')
    .count()
  if (captchaCount > 0) {
    throw new Error('BLOCKED: 登录页验证码已开启，无法无人工输入执行真实 E2E。')
  }

  const tenantSelect = loginForm.locator('.el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible())) {
    const currentTenantText = (await tenantSelect.innerText()).replace(/\s+/g, ' ').trim()
    if (!currentTenantText.includes(TENANT)) {
      await tenantSelect.click()
      const selectInput = loginForm.locator('.el-select__input').first()
      if ((await selectInput.count()) > 0) {
        await selectInput.fill(TENANT)
      }
      const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
      await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
      await tenantOption.click()
    }
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }

  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), USERNAME, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), PASSWORD, 'password')
  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    loginForm.locator('.el-button--primary').first().click()
  ])
  const loginBody = await loginResponse.json()
  assert.equal(loginBody.code, 0, `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, {
    timeout: 60000
  })
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function unwrapStorageValue(raw) {
  if (!raw) return ''
  let current = raw
  for (let index = 0; index < 6; index += 1) {
    try {
      current = JSON.parse(current)
    } catch {
      break
    }
    if (current && typeof current === 'object') {
      if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
        current = current.accessToken
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'v')) {
        current = current.v
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'value')) {
        current = current.value
        continue
      }
    }
    if (typeof current !== 'string') break
  }
  return String(current || '').replace(/^"|"$/g, '')
}

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      result[key] = result[key] || sessionStorage.getItem(key)
    }
    return result
  })
  return {
    token: unwrapStorageValue(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrapStorageValue(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrapStorageValue(snapshot.visitTenantId)
  }
}

async function apiGet(page, auth, pathName, params = {}) {
  assert.ok(auth.token, 'API support requires browser access token')
  assert.ok(auth.tenantId, 'API support requires browser tenant-id')
  const response = await page.request.get(`${BACKEND_URL}${pathName}`, {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      'tenant-id': String(auth.tenantId),
      ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
    },
    params
  })
  assert.equal(response.status(), 200, `${pathName} HTTP status must be 200`)
  const body = await response.json()
  assert.equal(body.code, 0, `${pathName} business response must succeed: ${body.msg || body.code}`)
  return body.data
}

function formatShanghaiDate(timestamp) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(timestamp))
}

function normalizeDateValue(value) {
  if (value === undefined || value === null || value === '') {
    return ''
  }
  if (typeof value === 'number') {
    return formatShanghaiDate(value)
  }
  const text = String(value)
  const directDate = text.match(/\d{4}-\d{2}-\d{2}/)
  if (directDate) {
    return directDate[0]
  }
  if (/^\d{13}$/.test(text)) {
    return formatShanghaiDate(Number(text))
  }
  return ''
}

function shiftMonth(monthText, offset) {
  const [year, month] = monthText.split('-').map(Number)
  const shifted = new Date(Date.UTC(year, month - 1 + offset, 1))
  return `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth() + 1).padStart(2, '0')}`
}

function flattenCalendarTasks(dayDetail) {
  return (dayDetail?.workshops || []).flatMap((workshop) =>
    (workshop.lines || []).flatMap((line) => line.tasks || [])
  )
}

async function discoverCandidateScheduleOrder(page, auth) {
  const rules = await apiGet(page, auth, '/admin-api/mes/pro/schedule-calendar/rules')
  const simulationDate = normalizeDateValue(rules?.simulationCurrentDate)
  const currentMonth = formatShanghaiDate(Date.now()).slice(0, 7)
  const baseMonths = [simulationDate.slice(0, 7), currentMonth].filter(Boolean)
  const months = [
    ...new Set(
      baseMonths.flatMap((month) => [month, shiftMonth(month, -1), shiftMonth(month, 1)])
    )
  ]
  const inspectedDays = []
  const inspectedWorkOrderIds = new Set()

  for (const month of months) {
    const monthData = await apiGet(page, auth, '/admin-api/mes/pro/schedule-calendar/month', {
      month
    })
    const taskDays = (monthData?.days || [])
      .filter((day) => Number(day.totalTaskCount || 0) > 0)
      .sort((left, right) => String(right.date).localeCompare(String(left.date)))

    for (const day of taskDays) {
      const date = normalizeDateValue(day.date)
      if (!date) {
        continue
      }
      inspectedDays.push(date)
      const dayDetail = await apiGet(page, auth, '/admin-api/mes/pro/schedule-calendar/day-detail', {
        date
      })
      const workOrderIds = [
        ...new Set(
          flattenCalendarTasks(dayDetail)
            .map((task) => Number(task.workOrderId || 0))
            .filter((workOrderId) => workOrderId > 0)
        )
      ]

      for (const workOrderId of workOrderIds) {
        if (inspectedWorkOrderIds.has(workOrderId)) {
          continue
        }
        inspectedWorkOrderIds.add(workOrderId)
        const pageData = await apiGet(page, auth, '/admin-api/mes/pro/schedule-order/page', {
          pageNo: 1,
          pageSize: 20,
          workOrderId,
          completionFilter: 'INCOMPLETE'
        })
        const row = (pageData?.list || []).find(
          (item) =>
            item.id &&
            item.code &&
            item.erpWorkOrderCode &&
            Number(item.workOrderId) === workOrderId &&
            Number(item.blockingIssueCount || 0) === 0
        )
        if (row) {
          return {
            id: row.id,
            code: row.code,
            erpWorkOrderCode: row.erpWorkOrderCode,
            workOrderId,
            date
          }
        }
      }
    }
  }

  throw new Error(
    `BLOCKED: ${TENANT}/${USERNAME} 扫描月份 ${months.join(',')} 的 ${inspectedDays.length} 个有任务日期、` +
      `${inspectedWorkOrderIds.size} 个工单后，没有找到可用于任务自有阻断 fixture 的未阻断排产工单。`
  )
}

async function gotoCalendarDate(page, date) {
  const detailPromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-calendar/day-detail') &&
        response.url().includes(`date=${date}`) &&
        response.status() === 200,
      { timeout: 60000 }
    )
    .catch(() => null)
  await page.goto(`${BASE_URL}/mes/pro/schedule-calendar?date=${encodeURIComponent(date)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.schedule-calendar-page').waitFor({ state: 'visible', timeout: 60000 })
  await detailPromise
  await settle(page)
  await page.locator(`.calendar-cell[data-date="${date}"]`).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
}

async function fillFormItemInput(dialog, label, value) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  const input = item.locator('input, textarea').first()
  await input.click({ clickCount: 3 })
  await input.fill(String(value))
}

async function createIssueViaUi(page, candidate) {
  await gotoCalendarDate(page, candidate.date)
  await page.getByRole('button', { name: '异常登记' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '异常登记' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormItemInput(dialog, '异常类型', 'MANUAL_EXCEPTION')
  await fillFormItemInput(dialog, '工单ID', candidate.workOrderId)
  await fillFormItemInput(dialog, '说明', ISSUE_MESSAGE)

  const createResponsePromise = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return (
        response.request().method() === 'POST' &&
        url.pathname === '/admin-api/mes/pro/auto-schedule/issues'
      )
    },
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '登记异常' }).click()
  const response = await createResponsePromise
  assert.equal(response.status(), 200, 'create issue HTTP status must be 200')
  const body = await response.json()
  assert.equal(body.code, 0, `create issue business response must succeed: ${body.msg || body.code}`)
  const issueId = Number(body.data)
  assert.ok(issueId > 0, `created issue id must be positive, actual=${body.data}`)
  await settle(page)
  return issueId
}

function issueIdOf(issue) {
  return Number(issue?.id || issue?.issueId || 0)
}

async function findIssue(page, auth, issueId, workOrderId) {
  const issues = await apiGet(page, auth, '/admin-api/mes/pro/auto-schedule/issues', {
    workOrderId,
    severity: 'BLOCKING'
  })
  return (issues || []).find((issue) => issueIdOf(issue) === Number(issueId))
}

async function assertCreatedIssueOpen(page, auth, issueId, candidate) {
  const issue = await findIssue(page, auth, issueId, candidate.workOrderId)
  assert.ok(issue, `created issue ${issueId} must be readable by workOrderId=${candidate.workOrderId}`)
  assert.equal(issue.resolved, false, `created issue ${issueId} must be unresolved`)
  assert.equal(issue.severity, 'BLOCKING', `created issue ${issueId} must be BLOCKING`)
  assert.equal(issue.workOrderId, candidate.workOrderId, `created issue ${issueId} must stay bound to target workOrderId`)
  assert.equal(issue.message, ISSUE_MESSAGE, `created issue ${issueId} must keep task marker message`)
  return issue
}

async function ensureScheduleOrderCodeFilter(page, multiFilter) {
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const searchInput = multiFilter.locator('input[placeholder="请输入排产工单号"]').first()
    if ((await searchInput.count()) > 0 && (await searchInput.isVisible())) {
      return searchInput
    }

    const codeTab = multiFilter.locator('.el-tabs__item').filter({ hasText: '排产工单号' }).first()
    if ((await codeTab.count()) > 0 && (await codeTab.isVisible())) {
      await codeTab.click()
      await page.waitForTimeout(200)
      continue
    }

    const addButton = multiFilter.getByRole('button', { name: '新增筛选条件' }).first()
    await addButton.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(await addButton.isEnabled(), true, 'schedule order multi-filter add button must be enabled')
    await addButton.click()
    await page.waitForTimeout(300)
  }
  const tabLabels = await multiFilter
    .locator('.el-tabs__item')
    .evaluateAll((tabs) => tabs.map((tab) => tab.textContent?.trim()).filter(Boolean))
  throw new Error(`missing visible schedule order code filter after adding conditions; tabs=${tabLabels.join('|')}`)
}

async function searchScheduleOrder(page, code) {
  const initialPageResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
      { timeout: 60000 }
    )
    .catch(() => null)
  await page.goto(`${BASE_URL}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 60000 })
  await initialPageResponsePromise
  await settle(page)
  const existingRow = page
    .locator('.schedule-order-pool .el-table__body-wrapper:visible tbody tr')
    .filter({ hasText: code })
    .first()
  if ((await existingRow.count()) > 0 && (await existingRow.isVisible())) {
    return
  }

  const multiFilter = page.locator(
    '.table-multi-filter[data-table-key="mes.pro.scheduleOrder.main"]'
  ).first()
  await multiFilter.waitFor({ state: 'visible', timeout: 30000 })
  const searchInput = await ensureScheduleOrderCodeFilter(page, multiFilter)
  await fillFirstVisible(searchInput, code, 'schedule order search input')
  await page.waitForTimeout(250)
  const pageResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
      { timeout: 60000 }
    )
  await multiFilter.getByRole('button', { name: '查询' }).click()
  const pageResponse = await pageResponsePromise
  const pageBody = await pageResponse.json()
  assert.equal(
    pageBody.code,
    0,
    `schedule order search business response must succeed: ${pageBody.msg || pageBody.code}`
  )
  const returnedCodes = (pageBody.data?.list || []).map((row) => row.code).filter(Boolean)
  assert.ok(
    returnedCodes.includes(code),
    `schedule order search response must contain ${code}, actual=${returnedCodes.join(',')}`
  )
  await settle(page)
}

function locateScheduleOrderRow(page, candidate) {
  assert.ok(
    candidate.erpWorkOrderCode,
    `schedule order ${candidate.code} must expose a visible source work order code`
  )
  return page
    .locator('.schedule-order-pool .el-table__body-wrapper:visible tbody tr')
    .filter({ hasText: candidate.erpWorkOrderCode })
    .first()
}

async function assertBlockedRowVisible(page, candidate) {
  const targetRow = locateScheduleOrderRow(page, candidate)
  await targetRow.waitFor({ state: 'visible', timeout: 60000 })

  const className = await targetRow.evaluate((row) => row.className)
  assert.match(
    String(className),
    /schedule-order-pool__row--blocked/,
    `blocked schedule order ${candidate.code} must render blocked row class`
  )

  const reason = targetRow.locator('.schedule-order-pool__blocking-reason').first()
  await reason.waitFor({ state: 'visible', timeout: 30000 })
  const reasonText = (await reason.innerText()).replace(/\s+/g, ' ').trim()
  assert.ok(reasonText.includes('阻断：'), `blocked row ${candidate.code} must show 阻断 reason text`)
  assert.ok(reasonText.includes(ISSUE_MESSAGE), `blocked row ${candidate.code} must show created reason`)

  await reason.hover()
  const tooltip = page.locator('.el-popper:visible').filter({ hasText: ISSUE_MESSAGE }).first()
  await tooltip.waitFor({ state: 'visible', timeout: 10000 })

  const firstCellBackground = await targetRow
    .locator('td.el-table__cell')
    .first()
    .evaluate((cell) => window.getComputedStyle(cell).backgroundColor)
  assert.notEqual(
    firstCellBackground,
    'rgba(0, 0, 0, 0)',
    `blocked row ${candidate.code} should have a visible red-tinted background`
  )

  return {
    className: String(className),
    reasonText,
    firstCellBackground
  }
}

async function resolveIssueViaUi(page, candidate, issueId) {
  await gotoCalendarDate(page, candidate.date)
  await page.waitForFunction(
    () =>
      [...document.querySelectorAll('button')].some(
        (button) => button.innerText.trim() === '关闭异常' && !button.disabled
      ),
    null,
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '关闭异常' }).first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '关闭异常' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFormItemInput(dialog, '异常ID', issueId)
  await fillFormItemInput(dialog, '关闭原因', RESOLUTION_REASON)

  const resolveResponsePromise = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return (
        response.request().method() === 'PUT' &&
        url.pathname === '/admin-api/mes/pro/auto-schedule/issues/resolve'
      )
    },
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '关闭异常' }).click()
  const response = await resolveResponsePromise
  assert.equal(response.status(), 200, 'resolve issue HTTP status must be 200')
  const body = await response.json()
  assert.equal(body.code, 0, `resolve issue business response must succeed: ${body.msg || body.code}`)
  assert.equal(body.data, true, 'resolve issue response data must be true')
  await settle(page)
}

async function assertIssueResolved(page, auth, issueId, candidate) {
  const issue = await findIssue(page, auth, issueId, candidate.workOrderId)
  assert.ok(issue, `resolved issue ${issueId} must stay queryable for cleanup evidence`)
  assert.equal(issue.resolved, true, `issue ${issueId} must be resolved after UI cleanup`)
  assert.equal(issue.resolutionReason, RESOLUTION_REASON, `issue ${issueId} must keep cleanup reason`)
}

async function assertScheduleOrderCleared(page, auth, candidate) {
  const pageData = await apiGet(page, auth, '/admin-api/mes/pro/schedule-order/page', {
    pageNo: 1,
    pageSize: 10,
    workOrderId: candidate.workOrderId
  })
  const rows = pageData?.list || []
  const row = rows.find((item) => item.code === candidate.code || Number(item.id) === Number(candidate.id))
  assert.ok(row, `target schedule order ${candidate.code} must remain queryable after cleanup`)
  assert.equal(Number(row.blockingIssueCount || 0), 0, `target schedule order ${candidate.code} blocker count must clear`)

  await searchScheduleOrder(page, candidate.code)
  const targetRow = locateScheduleOrderRow(page, candidate)
  await targetRow.waitFor({ state: 'visible', timeout: 60000 })
  const className = await targetRow.evaluate((element) => element.className)
  assert.doesNotMatch(
    String(className),
    /schedule-order-pool__row--blocked/,
    `target schedule order ${candidate.code} should not stay red after cleanup`
  )
  assert.equal(
    await targetRow.locator('.schedule-order-pool__blocking-reason').filter({ hasText: TASK_MARKER }).count(),
    0,
    `target schedule order ${candidate.code} should not show task marker after cleanup`
  )
}

function trackMesMutations(page, expectedMutations, unexpectedMutations) {
  page.on('request', (request) => {
    const url = new URL(request.url())
    if (!url.pathname.startsWith('/admin-api/mes/')) {
      return
    }
    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())) {
      return
    }
    if (request.method() === 'POST' && READ_EQUIVALENT_MES_POST_PATHS.has(url.pathname)) {
      return
    }
    const key = `${request.method()} ${url.pathname}`
    if (EXPECTED_MES_MUTATION_KEYS.includes(key)) {
      expectedMutations.push(key)
    } else {
      unexpectedMutations.push(key)
    }
  })
}

async function main() {
  assertLocalOnly()
  const expectedMutations = []
  const unexpectedMutations = []
  const pageErrors = []
  const consoleErrors = []
  let candidate
  let issueId = 0
  let uiEvidence
  let auth
  let issueResolved = false
  let cleanupStatus = 'not-created'

  const browser = await chromium.launch({
    headless: process.env.MES_PARTIAL_REPLAN_E2E_HEADED !== '1',
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.on('pageerror', (error) => pageErrors.push(String(error)))
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  trackMesMutations(page, expectedMutations, unexpectedMutations)

  try {
    await login(page)
    auth = await browserAuth(page)
    assert.equal(String(auth.tenantId), EXPECTED_TENANT_ID, `must stay in 芋道源码 tenant_id=1, actual ${auth.tenantId}`)

    candidate = await discoverCandidateScheduleOrder(page, auth)
    issueId = await createIssueViaUi(page, candidate)
    cleanupStatus = 'open'
    await assertCreatedIssueOpen(page, auth, issueId, candidate)
    await searchScheduleOrder(page, candidate.code)
    uiEvidence = await assertBlockedRowVisible(page, candidate)
    await resolveIssueViaUi(page, candidate, issueId)
    await assertIssueResolved(page, auth, issueId, candidate)
    issueResolved = true
    cleanupStatus = 'resolved-via-ui'
    await assertScheduleOrderCleared(page, auth, candidate)

    assert.deepEqual(unexpectedMutations, [], `unexpected MES mutation APIs: ${unexpectedMutations.join(', ')}`)
    assert.deepEqual(
      expectedMutations,
      EXPECTED_MES_MUTATION_KEYS,
      `fixture E2E must only create and resolve one task-owned issue: ${expectedMutations.join(', ')}`
    )
    assert.deepEqual(pageErrors, [], `page errors must stay empty: ${pageErrors.join('\n')}`)

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          mode: 'real-page-task-owned-fixture',
          baseUrl: BASE_URL,
          backendUrl: BACKEND_URL,
          tenant: TENANT,
          username: USERNAME,
          taskMarker: TASK_MARKER,
          scheduleOrderCode: candidate.code,
          scheduleOrderId: candidate.id,
          sourceWorkOrderCode: candidate.erpWorkOrderCode,
          workOrderId: candidate.workOrderId,
          issueDate: candidate.date,
          issueId,
          reasonText: uiEvidence.reasonText,
          firstCellBackground: uiEvidence.firstCellBackground,
          expectedMesMutationRequests: expectedMutations,
          unexpectedMesMutationCount: unexpectedMutations.length,
          pageErrorCount: pageErrors.length,
          consoleErrorCount: consoleErrors.length,
          cleanup: `${cleanupStatus}-and-row-cleared`
        },
        null,
        2
      )
    )
  } catch (error) {
    let message = error?.message || String(error)
    const currentUrl = page.url()
    const bodySnippet = await page
      .locator('body')
      .innerText({ timeout: 5000 })
      .then((text) => text.replace(/\s+/g, ' ').trim().slice(0, 1000))
      .catch((bodyError) => `BODY_READ_FAILED: ${bodyError.message || bodyError}`)
    if (issueId > 0 && candidate && auth && !issueResolved) {
      try {
        const currentIssue = await findIssue(page, auth, issueId, candidate.workOrderId)
        if (currentIssue?.resolved === true) {
          issueResolved = true
          cleanupStatus = 'already-resolved-after-failure'
        } else {
          await resolveIssueViaUi(page, candidate, issueId)
          await assertIssueResolved(page, auth, issueId, candidate)
          issueResolved = true
          cleanupStatus = 'resolved-via-ui-after-failure'
        }
      } catch (cleanupError) {
        cleanupStatus = `FAILED: ${cleanupError?.message || String(cleanupError)}`
        message = `${message}; task-owned cleanup failed: ${cleanupStatus}`
      }
    }
    console.log(
      JSON.stringify(
        {
          status: message.includes('BLOCKED:') ? 'BLOCKED' : 'FAIL',
          mode: 'real-page-task-owned-fixture',
          baseUrl: BASE_URL,
          backendUrl: BACKEND_URL,
          tenant: TENANT,
          username: USERNAME,
          taskMarker: TASK_MARKER,
          scheduleOrderCode: candidate?.code,
          sourceWorkOrderCode: candidate?.erpWorkOrderCode,
          workOrderId: candidate?.workOrderId,
          issueDate: candidate?.date,
          issueId: issueId || undefined,
          reason: message,
          expectedMesMutationRequests: expectedMutations,
          unexpectedMesMutationRequests: unexpectedMutations,
          pageErrorCount: pageErrors.length,
          consoleErrorCount: consoleErrors.length,
          currentUrl,
          bodySnippet,
          pageErrors: pageErrors.slice(0, 5),
          consoleErrors: consoleErrors.slice(0, 5),
          cleanup: cleanupStatus
        },
        null,
        2
      )
    )
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error.stack || error.message)
  process.exit(1)
})
