const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (
  process.env.MES_PARTIAL_REPLAN_E2E_BASE_URL || 'http://127.0.0.1:8081'
).replace(/\/+$/, '')
const BACKEND_URL = (
  process.env.MES_PARTIAL_REPLAN_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
).replace(/\/+$/, '')
const READONLY_ALLOWED_MES_POST_PATHS = new Set([
  '/admin-api/mes/pro/schedule-order/page'
])

const TENANT = process.env.MES_PARTIAL_REPLAN_E2E_TENANT || '测试租户'
const USERNAME = process.env.MES_PARTIAL_REPLAN_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.MES_PARTIAL_REPLAN_E2E_PASSWORD || process.env.MES_REPLAN_E2E_PASSWORD || ''
const AUTHORIZED_READONLY_IDENTITIES = [
  { tenant: '测试租户', username: 'aoteman', tenantId: '122' },
  { tenant: '芋道源码', username: 'admin', tenantId: '1' }
]

function expectedIdentity() {
  return AUTHORIZED_READONLY_IDENTITIES.find(
    (identity) => identity.tenant === TENANT && identity.username === USERNAME
  )
}

function assertLocalOnly() {
  const identity = expectedIdentity()
  assert.match(BASE_URL, /^http:\/\/(127\.0\.0\.1|localhost):8081$/, 'E2E must use int_main local frontend 8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'E2E must use int_main local backend 48081')
  assert.ok(
    identity,
    'readonly E2E identity must be 测试租户/aoteman or explicitly authorized 芋道源码/admin'
  )
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
  await page.goto(`${BASE_URL}/login?redirect=/mes/pro/schedule-order`, {
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
    await tenantSelect.click()
    const selectInput = loginForm.locator('.el-select__input').first()
    if ((await selectInput.count()) > 0) {
      await selectInput.fill(TENANT)
    }
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
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
  assert.ok(auth.token, 'readonly API support requires browser access token')
  assert.ok(auth.tenantId, 'readonly API support requires browser tenant-id')
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

async function findBlockedScheduleOrder(page, auth) {
  const pageData = await apiGet(page, auth, '/admin-api/mes/pro/schedule-order/page', {
    pageNo: 1,
    pageSize: 200
  })
  const rows = pageData?.list || []
  const blocked = rows.find((row) => Number(row.blockingIssueCount || 0) > 0)
  if (blocked) {
    assert.ok(blocked.code, 'blocked schedule order must expose code')
    assert.ok(
      blocked.latestBlockingIssueMessage,
      `blocked schedule order ${blocked.code} must expose latestBlockingIssueMessage`
    )
    return blocked
  }

  const issueData = await apiGet(page, auth, '/admin-api/mes/pro/auto-schedule/issues', {
    severity: 'BLOCKING'
  })
  const blockingIssues = issueData || []
  const unresolvedBlockingIssues = blockingIssues.filter((issue) => !issue.resolved)
  const openBlockingIssues = unresolvedBlockingIssues.filter((issue) => issue.workOrderId)
  const mappedSamples = []
  for (const issue of openBlockingIssues.slice(0, 20)) {
    const byWorkOrder = await apiGet(page, auth, '/admin-api/mes/pro/schedule-order/page', {
      pageNo: 1,
      pageSize: 10,
      workOrderId: issue.workOrderId
    })
    const candidates = byWorkOrder?.list || []
    const mappedBlocked = candidates.find((row) => Number(row.blockingIssueCount || 0) > 0)
    mappedSamples.push({
      workOrderId: issue.workOrderId,
      issueId: issue.id,
      scheduleOrderCount: candidates.length,
      mappedCodes: candidates.map((row) => row.code).filter(Boolean).slice(0, 3)
    })
    if (mappedBlocked) {
      assert.ok(mappedBlocked.code, 'blocked schedule order must expose code')
      assert.ok(
        mappedBlocked.latestBlockingIssueMessage,
        `blocked schedule order ${mappedBlocked.code} must expose latestBlockingIssueMessage`
      )
      return mappedBlocked
    }
  }

  throw new Error(
    `BLOCKED: ${TENANT}排产工单列表 ${rows.length} 条没有未解决阻断展示行；` +
      `只读 issues 接口 BLOCKING 总数=${blockingIssues.length}，` +
      `未解决=${unresolvedBlockingIssues.length}，` +
      `未解决且 workOrderId 非空=${openBlockingIssues.length}，` +
      `前 ${mappedSamples.length} 条映射结果=${JSON.stringify(mappedSamples)}。`
  )
}

async function searchScheduleOrder(page, code) {
  await page.goto(`${BASE_URL}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 60000 })
  const quickFilter = page.locator('.table-quick-filter[data-table-key="mes.pro.scheduleOrder.main"]').first()
  const searchInput = page.locator(
    'input[placeholder="请输入排产工单号"], input[placeholder="请输入工单编码"], input[placeholder="请输入排产工单编号"]'
  )
  await fillFirstVisible(searchInput, code, 'schedule order search input')
  const pageResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
        response.status() === 200,
      { timeout: 60000 }
    )
    .catch(() => null)
  const queryButton = quickFilter.getByRole('button', { name: /查询|搜索/ }).first()
  if ((await queryButton.count()) > 0) {
    await queryButton.click()
  } else {
    await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  }
  await pageResponsePromise
  await settle(page)
}

async function assertBlockedRowVisible(page, blocked) {
  const targetRow = page
    .locator('.schedule-order-pool .el-table__body-wrapper:visible tbody tr')
    .filter({ hasText: blocked.code })
    .first()
  await targetRow.waitFor({ state: 'visible', timeout: 60000 })

  const className = await targetRow.evaluate((row) => row.className)
  assert.match(
    String(className),
    /schedule-order-pool__row--blocked/,
    `blocked schedule order ${blocked.code} must render blocked row class`
  )

  const reason = targetRow.locator('.schedule-order-pool__blocking-reason').first()
  await reason.waitFor({ state: 'visible', timeout: 30000 })
  const reasonText = (await reason.innerText()).replace(/\s+/g, ' ').trim()
  assert.ok(reasonText.includes('阻断：'), `blocked row ${blocked.code} must show 阻断 reason text`)
  assert.ok(
    reasonText.includes(blocked.latestBlockingIssueMessage),
    `blocked row ${blocked.code} must show latest reason: ${blocked.latestBlockingIssueMessage}`
  )

  await reason.hover()
  const tooltip = page.locator('.el-popper:visible').filter({ hasText: blocked.latestBlockingIssueMessage }).first()
  await tooltip.waitFor({ state: 'visible', timeout: 10000 })

  const firstCellBackground = await targetRow
    .locator('td.el-table__cell')
    .first()
    .evaluate((cell) => window.getComputedStyle(cell).backgroundColor)
  assert.notEqual(
    firstCellBackground,
    'rgba(0, 0, 0, 0)',
    `blocked row ${blocked.code} should have a visible red-tinted background`
  )

  return {
    className: String(className),
    reasonText,
    firstCellBackground
  }
}

async function main() {
  assertLocalOnly()
  const writeRequests = []
  const pageErrors = []
  const consoleErrors = []
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
  page.on('request', (request) => {
    const url = new URL(request.url())
    if (
      url.pathname.startsWith('/admin-api/mes/') &&
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) &&
      !READONLY_ALLOWED_MES_POST_PATHS.has(url.pathname)
    ) {
      writeRequests.push(`${request.method()} ${url.pathname}`)
    }
  })

  try {
    await login(page)
    const auth = await browserAuth(page)
    const identity = expectedIdentity()
    assert.ok(identity, 'readonly E2E identity must stay authorized after login')
    assert.equal(
      String(auth.tenantId),
      identity.tenantId,
      `readonly E2E must stay in ${identity.tenant} tenant_id=${identity.tenantId}, actual ${auth.tenantId}`
    )
    const blocked = await findBlockedScheduleOrder(page, auth)
    await searchScheduleOrder(page, blocked.code)
    const ui = await assertBlockedRowVisible(page, blocked)

    assert.deepEqual(writeRequests, [], `readonly E2E must not call MES write APIs: ${writeRequests.join(', ')}`)
    assert.deepEqual(pageErrors, [], `page errors must stay empty: ${pageErrors.join('\n')}`)

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          mode: 'readonly-real-page',
          baseUrl: BASE_URL,
          backendUrl: BACKEND_URL,
          tenant: TENANT,
          username: USERNAME,
          scheduleOrderCode: blocked.code,
          blockingIssueCount: blocked.blockingIssueCount,
          latestBlockingIssueMessage: blocked.latestBlockingIssueMessage,
          className: ui.className,
          reasonText: ui.reasonText,
          firstCellBackground: ui.firstCellBackground,
          mesWriteRequestCount: writeRequests.length,
          pageErrorCount: pageErrors.length,
          consoleErrorCount: consoleErrors.length
        },
        null,
        2
      )
    )
  } catch (error) {
    const message = error?.message || String(error)
    console.log(
      JSON.stringify(
        {
          status: message.includes('BLOCKED:') ? 'BLOCKED' : 'FAIL',
          mode: 'readonly-real-page',
          baseUrl: BASE_URL,
          backendUrl: BACKEND_URL,
          tenant: TENANT,
          username: USERNAME,
          reason: message,
          mesWriteRequestCount: writeRequests.length,
          pageErrorCount: pageErrors.length,
          consoleErrorCount: consoleErrors.length
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
