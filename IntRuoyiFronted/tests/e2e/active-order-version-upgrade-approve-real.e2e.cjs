const assert = require('assert')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const TASK_ID = '20260902-active-order-latest-version-upgrade-restart-code'
const FRONTEND_URL = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_FRONTEND_URL || 'http://127.0.0.1:8093'
const TENANT_NAME = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TENANT || '芋道源码'
const USERNAME = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_USERNAME || 'admin'
const PASSWORD = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_PASSWORD || ''
const WORK_ORDER_CODE =
  process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_WORK_ORDER_CODE || 'CODX-PQC-20260807-SP-WO-05'
const SOURCE_ACTIVE_ORDER_ID = Number(process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_ACTIVE_ORDER_ID || '45')
const PROCESS_INSTANCE_ID = process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_PROCESS_INSTANCE_ID || ''
const EXPECTED_TARGET_ROUTE_VERSION_ID = Number(
  process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TARGET_ROUTE_VERSION_ID || '742'
)
const EXPECTED_TARGET_ROUTE_VERSION_NO =
  process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TARGET_ROUTE_VERSION_NO || 'V12'
const EXPECTED_TARGET_QA_VERSION_ID = Number(
  process.env.ACTIVE_ORDER_VERSION_UPGRADE_E2E_TARGET_QA_VERSION_ID || '65'
)
const OUTPUT_DIR = path.resolve(__dirname, '../../../doc/tasks', TASK_ID, 'e2e-artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'active-order-version-upgrade-approve-real-result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'active-order-version-upgrade-approve-real.png')

if (!PASSWORD) {
  throw new Error('ACTIVE_ORDER_VERSION_UPGRADE_E2E_PASSWORD is required; do not store passwords in source or logs.')
}
if (!PROCESS_INSTANCE_ID) {
  throw new Error('ACTIVE_ORDER_VERSION_UPGRADE_E2E_PROCESS_INSTANCE_ID is required for the approval UI path.')
}

function sanitizeUrl(url) {
  return String(url || '').replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')
}

async function fillFirst(pageOrLocator, selectors, value) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(selector).first()
    if ((await locator.count()) === 0) continue
    try {
      await locator.waitFor({ state: 'visible', timeout: 8000 })
      await locator.fill(value)
      return selector
    } catch (_) {
      // Try next stable selector.
    }
  }
  throw new Error(`No fillable locator found for selectors: ${selectors.join(', ')}`)
}

async function clickFirst(pageOrLocator, selectors, label) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(selector).first()
    if ((await locator.count()) === 0) continue
    try {
      await locator.waitFor({ state: 'visible', timeout: 10000 })
      await locator.click()
      return selector
    } catch (_) {
      // Try next stable selector.
    }
  }
  throw new Error(`No clickable locator found for ${label}: ${selectors.join(', ')}`)
}

async function selectLoginTenant(page, tenantName) {
  const tenantInput = page.locator('.login-form .el-select input:visible').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 15000 })
  await tenantInput.click()
  await tenantInput.fill(tenantName)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: tenantName }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await selectLoginTenant(page, TENANT_NAME)
  await fillFirst(
    page,
    [
      '.login-form input[placeholder="请输入用户名"]',
      '.login-form input[placeholder*="账号"]',
      '.login-form input[name="username"]'
    ],
    USERNAME
  )
  await fillFirst(
    page,
    [
      '.login-form input[type="password"]',
      '.login-form input[placeholder="请输入密码"]',
      '.login-form input[name="password"]'
    ],
    PASSWORD
  )
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') && response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await clickFirst(page, ['.login-form button[type="submit"]', 'button:has-text("登录")'], '登录')
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  const body = await loginResponse.json()
  assert.equal(Number(body.code), 0, `登录接口业务失败：${body.msg || body.message || 'unknown'}`)
  await permissionResponsePromise
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

async function openApprovalTodo(page, evidence) {
  const approvalPagePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/page') &&
      response.url().includes(`keyword=${encodeURIComponent(PROCESS_INSTANCE_ID)}`) &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await page.goto(
    `${FRONTEND_URL}/approval-center/todo?moduleCode=BPM&keyword=${encodeURIComponent(PROCESS_INSTANCE_ID)}`,
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  await page.locator('.approval-center__table').first().waitFor({ state: 'visible', timeout: 30000 })
  const approvalPageResponse = await approvalPagePromise
  const body = await approvalPageResponse.json()
  assert.equal(Number(body.code), 0, `审批待办列表业务失败：${body.msg || body.message || 'unknown'}`)
  assert.equal(Number(body.data.total), 1, `审批待办必须只命中当前流程实例，实际 total=${body.data.total}`)
  const task = body.data.list[0]
  assert.equal(task.processInstanceId, PROCESS_INSTANCE_ID, '审批待办流程实例必须匹配目标实例')
  assert.equal(task.sourceTaskType, 'BPM_TASK_TODO', '审批动作必须来自真实 BPM 待办')
  assert.ok((task.availableActions || []).includes('APPROVE'), '审批待办必须开放 APPROVE 动作')
  evidence.approvalTodo = {
    sourceTaskId: task.sourceTaskId,
    processInstanceId: task.processInstanceId,
    businessTitle: task.businessTitle,
    currentNodeName: task.currentNodeName
  }
  const row = page.locator('.approval-center__table .el-table__body-wrapper tbody tr', {
    hasText: task.businessTitle
  }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function approveFromUi(page, row, evidence) {
  const reviewButton = row.locator('[data-approval-action="review"]').first()
  await reviewButton.waitFor({ state: 'visible', timeout: 15000 })
  await reviewButton.click()
  const dialog = page.locator('.el-dialog:visible', { hasText: '审核确认' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText('审核通过需完成电子签名确认。', { exact: false }).waitFor({
    state: 'visible',
    timeout: 15000
  })
  await fillFirst(dialog, ['input[type="password"]', 'input[placeholder*="电子签名"]'], PASSWORD)
  const reviewResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/review') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.locator('button', { hasText: '确认审核' }).first().click()
  const reviewResponse = await reviewResponsePromise
  assert.equal(reviewResponse.ok(), true, `审批提交 HTTP 失败：${reviewResponse.status()}`)
  const reviewBody = await reviewResponse.json()
  assert.equal(Number(reviewBody.code), 0, `审批提交业务失败：${reviewBody.msg || reviewBody.message || 'unknown'}`)
  evidence.review = {
    businessCode: reviewBody.code,
    result: reviewBody.data
  }
  await page.locator('.el-message', { hasText: '审核已通过' }).first().waitFor({
    state: 'visible',
    timeout: 15000
  })
}

async function openActiveOrderPoolAndReadList(page) {
  const listResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/list') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await page.goto(`${FRONTEND_URL}/mes/pro/process-pool/production-leader`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('[data-team-leader-report-workbench]').first().waitFor({ state: 'visible', timeout: 30000 })
  const activeOrderTab = page
    .locator('.el-tabs__item:visible, [role="tab"]:visible', { hasText: '活跃订单池' })
    .first()
  await activeOrderTab.waitFor({ state: 'visible', timeout: 15000 })
  await activeOrderTab.click()
  const listResponse = await listResponsePromise
  const listBody = await listResponse.json()
  assert.equal(Number(listBody.code), 0, `活跃订单列表业务失败：${listBody.msg || listBody.message || 'unknown'}`)
  await page.locator('[data-team-leader-active-order-list]').first().waitFor({ state: 'visible', timeout: 30000 })
  return listBody.data || []
}

async function findActiveOrderRowAcrossPages(page, workOrderCode) {
  const rowSelector = '[data-team-leader-active-order-list] .el-table__body-wrapper tbody tr'
  for (let pageIndex = 1; pageIndex <= 30; pageIndex++) {
    const row = page.locator(rowSelector, { hasText: workOrderCode }).first()
    if ((await row.count()) > 0) {
      try {
        await row.waitFor({ state: 'visible', timeout: 2000 })
        return row
      } catch (_) {
        // Continue to the next rendered page.
      }
    }
    const nextButton = page.locator('[data-team-leader-active-order-config] .el-pagination button.btn-next').first()
    if ((await nextButton.count()) === 0 || (await nextButton.isDisabled())) break
    await nextButton.click()
    await page.waitForTimeout(500)
  }
  throw new Error(`审批通过后未在活跃订单池分页中找到新订单：${workOrderCode}`)
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const evidence = {
    taskId: TASK_ID,
    frontendUrl: FRONTEND_URL,
    tenant: TENANT_NAME,
    username: USERNAME,
    sourceActiveOrderId: SOURCE_ACTIVE_ORDER_ID,
    processInstanceId: PROCESS_INSTANCE_ID,
    workOrderCode: WORK_ORDER_CODE,
    expectedTargetRouteVersionId: EXPECTED_TARGET_ROUTE_VERSION_ID,
    expectedTargetRouteVersionNo: EXPECTED_TARGET_ROUTE_VERSION_NO,
    expectedTargetQaVersionId: EXPECTED_TARGET_QA_VERSION_ID,
    requests: [],
    consoleErrors: [],
    pageErrors: []
  }
  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/approval-center/tasks/review') && !url.includes('/active-order/list')) return
    let body = null
    try {
      body = await response.json()
    } catch (_) {
      body = null
    }
    evidence.requests.push({
      url: sanitizeUrl(url),
      method: response.request().method(),
      status: response.status(),
      businessCode: body && body.code,
      businessMessage: body && (body.msg || body.message)
    })
  })
  try {
    await login(page)
    evidence.login = 'PASS'
    const row = await openApprovalTodo(page, evidence)
    await approveFromUi(page, row, evidence)
    const activeOrders = await openActiveOrderPoolAndReadList(page)
    const replacement = activeOrders.find(
      (item) => item.workOrderCode === WORK_ORDER_CODE && Number(item.id) !== SOURCE_ACTIVE_ORDER_ID
    )
    assert.ok(replacement, `审批通过后必须出现同工单的新活跃订单：${WORK_ORDER_CODE}`)
    assert.equal(Number(replacement.routeVersionId), EXPECTED_TARGET_ROUTE_VERSION_ID, '新活跃订单必须使用冻结目标工艺路线版本')
    assert.equal(replacement.routeVersionNo, EXPECTED_TARGET_ROUTE_VERSION_NO, '新活跃订单必须显示冻结目标工艺路线版本号')
    assert.equal(replacement.activeStatus, 'ACTIVE', '新活跃订单必须进入 ACTIVE 状态')
    assert.equal(replacement.businessStatus, 'ACTIVE', '新活跃订单业务状态必须为 ACTIVE')
    await findActiveOrderRowAcrossPages(page, WORK_ORDER_CODE)
    evidence.replacementActiveOrder = {
      id: replacement.id,
      workOrderCode: replacement.workOrderCode,
      routeVersionId: replacement.routeVersionId,
      routeVersionNo: replacement.routeVersionNo,
      activeStatus: replacement.activeStatus,
      businessStatus: replacement.businessStatus
    }
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    evidence.screenshot = SCREENSHOT_PATH
    evidence.status = 'PASS'
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    try {
      await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
      evidence.screenshot = SCREENSHOT_PATH
    } catch (_) {
      // Preserve the original failure.
    }
    throw error
  } finally {
    fs.writeFileSync(RESULT_PATH, JSON.stringify(evidence, null, 2), 'utf8')
    await browser.close()
  }
  console.log(`PASS: active-order version upgrade approve real E2E -> ${RESULT_PATH}`)
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error))
  process.exit(1)
})
