const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for MES schedule order manual finish real E2E.')
  }
}

function isFalseBit(value) {
  return value === false || value === 0 || value === '0' || value === null || value === undefined
}

const config = {
  baseUrl: (process.env.MES_SCHEDULE_ORDER_MANUAL_FINISH_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_SCHEDULE_ORDER_MANUAL_FINISH_E2E_TENANT || '测试租户',
  plannerUsername: process.env.MES_SCHEDULE_ORDER_MANUAL_FINISH_E2E_PLANNER_USERNAME || 'smokeplan1',
  plannerPassword: process.env.MES_SCHEDULE_ORDER_MANUAL_FINISH_E2E_PLANNER_PASSWORD || '111111',
  adminUsername: process.env.MES_SCHEDULE_ORDER_MANUAL_FINISH_E2E_ADMIN_USERNAME || 'smokeappr1',
  adminPassword: process.env.MES_SCHEDULE_ORDER_MANUAL_FINISH_E2E_ADMIN_PASSWORD || '111111',
  headed: process.env.MES_SCHEDULE_ORDER_MANUAL_FINISH_E2E_HEADED === '1'
}

const WRITE_TENANT_ID = '122'
const FILTER_INCOMPLETE = 'INCOMPLETE'
const FILTER_COMPLETED = 'COMPLETED'
const STATUS_FINISHED = 3

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch((error) => {
    if (!String(error?.message || '').includes('Timeout')) {
      throw error
    }
  })
  await page.waitForTimeout(500)
}

async function readJsonResponse(response, label) {
  const body = await response.json()
  assert.equal(body.code, 0, `${label} 接口业务错误: ${body.msg || body.code}`)
  return body
}

async function login(page, username, password) {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  if (
    (await page
      .locator(
        '.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible'
      )
      .count()) > 0
  ) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  }

  const textInputs = form.locator('input.el-input__inner:not([role="combobox"])')
  await textInputs.first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `登录失败: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function getAuthHeaders(page) {
  const accessToken = await page.evaluate(() => {
    const raw = localStorage.getItem('ACCESS_TOKEN')
    if (!raw) return ''
    const cached = JSON.parse(raw)
    return typeof cached?.v === 'string' ? JSON.parse(cached.v) : cached?.v || raw
  })
  assert.ok(accessToken, '登录后必须存在 ACCESS_TOKEN。')
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': WRITE_TENANT_ID
  }
}

async function openScheduleOrderPage(page) {
  const listResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const response = await listResponsePromise
  await readJsonResponse(response, '排产工单列表')
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 30000 })
}

async function queryScheduleOrderByApi(page, completionFilter, workOrderCode) {
  const headers = await getAuthHeaders(page)
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '20',
    completionFilter,
    erpWorkOrderCode: workOrderCode
  })
  const response = await page.request.get(
    `${config.baseUrl}/admin-api/mes/pro/schedule-order/page?${params.toString()}`,
    { headers }
  )
  assert.equal(response.status(), 200, `排产工单分页 HTTP 异常: ${response.status()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `排产工单分页接口业务错误: ${body.msg || body.code}`)
  return body.data?.list || []
}

async function pickManualFinishCandidate(page) {
  const headers = await getAuthHeaders(page)
  const response = await page.request.get(
    `${config.baseUrl}/admin-api/mes/pro/schedule-order/page?pageNo=1&pageSize=100&completionFilter=${FILTER_INCOMPLETE}`,
    { headers }
  )
  assert.equal(response.status(), 200, `未完成排产工单分页 HTTP 异常: ${response.status()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `未完成排产工单分页接口业务错误: ${body.msg || body.code}`)
  const candidate = (body.data?.list || []).find((row) => {
    const totalQuantity = Number(row.totalQuantity || 0)
    const completedQuantity = Number(row.completedQuantity || 0)
    const uncompletedQuantity = Number(row.uncompletedQuantity || 0)
    return (
      row.id &&
      row.erpWorkOrderCode &&
      !row.manualFinished &&
      isFalseBit(row.frozen) &&
      row.status !== STATUS_FINISHED &&
      Number.isFinite(totalQuantity) &&
      totalQuantity > 0 &&
      Number.isFinite(completedQuantity) &&
      Number.isFinite(uncompletedQuantity) &&
      uncompletedQuantity > 0
    )
  })
  assert.ok(candidate, 'BLOCKED: 测试租户未找到可用于完成验收的真实未完成排产工单。')
  return candidate
}

async function searchScheduleOrder(page, target) {
  const searchInput = page.locator('input[placeholder="请输入工单编码"]:visible').first()
  await searchInput.fill(target.erpWorkOrderCode)
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) =>
        item.url().includes('/admin-api/mes/pro/schedule-order/page') && item.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: /^搜索$/ }).click()
  ])
  const body = await readJsonResponse(response, '排产工单搜索')
  const rowData =
    (body.data?.list || []).find((row) => String(row.id) === String(target.id)) ||
    (body.data?.list || []).find((row) => row.code === target.code) ||
    (body.data?.list || []).find((row) => row.erpWorkOrderCode === target.erpWorkOrderCode)
  const row = page
    .locator('.schedule-order-pool .el-table__body-wrapper tbody tr')
    .filter({ hasText: target.code || target.erpWorkOrderCode })
    .first()
  return { row, rowData }
}

async function setCompletionFilter(page, value) {
  const select = page.locator('.schedule-order-pool .el-form-item').filter({ hasText: '完成状态' }).first()
  const label =
    value === FILTER_COMPLETED ? '已完成' : value === FILTER_INCOMPLETE ? '未完成' : '全部'
  await select.locator('.el-select').first().click()
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: label })
      .first()
      .click()
  ])
  await settle(page)
}

async function submitManualFinish(page, row, reason) {
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) =>
        item.url().includes('/admin-api/mes/pro/schedule-order/manual-finish') &&
        item.status() === 200,
      { timeout: 60000 }
    ),
    (async () => {
      await row.getByRole('button', { name: /^完成$/ }).click()
      const dialog = page.locator('.el-dialog:visible').filter({ hasText: '排产工单完成' }).first()
      await dialog.waitFor({ state: 'visible', timeout: 30000 })
      await dialog.locator('textarea').fill(reason)
      await dialog.getByRole('button', { name: /^完成$/ }).click()
      await page.locator('.el-message-box:visible').waitFor({ state: 'visible', timeout: 30000 })
      await page.locator('.el-message-box:visible').getByRole('button', { name: /^确定$/ }).click()
    })()
  ])
  await readJsonResponse(response, '完成')
  await page.getByText('排产工单已完成').waitFor({ state: 'visible', timeout: 30000 })
}

async function submitRevoke(page, row, reason) {
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) =>
        item.url().includes('/admin-api/mes/pro/schedule-order/revoke-manual-finish') &&
        item.status() === 200,
      { timeout: 60000 }
    ),
    (async () => {
      await row.getByRole('button', { name: /^撤销完成$/ }).click()
      const dialog = page.locator('.el-dialog:visible').filter({ hasText: '撤销排产工单完成' }).first()
      await dialog.waitFor({ state: 'visible', timeout: 30000 })
      await dialog.locator('textarea').fill(reason)
      await dialog.getByRole('button', { name: /^撤销完成$/ }).click()
      await page.locator('.el-message-box:visible').waitFor({ state: 'visible', timeout: 30000 })
      await page.locator('.el-message-box:visible').getByRole('button', { name: /^确定$/ }).click()
    })()
  ])
  await readJsonResponse(response, '撤销完成')
  await page.getByText('排产工单已撤销完成').waitFor({ state: 'visible', timeout: 30000 })
}

async function openTraceAndAssert(page, row, expectedOperationType, expectedReason) {
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) =>
        item.url().includes('/admin-api/mes/pro/schedule-order/operation-log') &&
        item.status() === 200,
      { timeout: 60000 }
    ),
    row.getByRole('button', { name: /^追溯$/ }).click()
  ])
  const body = await readJsonResponse(response, '排产工单追溯')
  assert.ok(
    Array.isArray(body.data) &&
      body.data.some(
        (item) => item.operationType === expectedOperationType && item.reason === expectedReason
      ),
    `追溯记录必须包含 ${expectedOperationType}/${expectedReason}`
  )
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '排产工单追溯' }).first()
  await dialog.getByText(expectedReason).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.keyboard.press('Escape')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function openProcessDialogAndAssertManualHint(page, row) {
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) =>
        item.url().includes('/admin-api/mes/pro/schedule-order/process-list') &&
        item.status() === 200,
      { timeout: 60000 }
    ),
    row.getByRole('button', { name: /^查看$/ }).click()
  ])
  await readJsonResponse(response, '工艺流程排产配置')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺流程排产配置' }).first()
  await dialog
    .getByText('该工单已由有权限人员完成；汇总按 100% 展示，以下工序仍保留真实进度，可撤销完成。')
    .waitFor({ state: 'visible', timeout: 30000 })
  await page.keyboard.press('Escape')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const plannerPage = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  const adminPage = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(plannerPage, config.plannerUsername, config.plannerPassword)
    await openScheduleOrderPage(plannerPage)

    const candidate = await pickManualFinishCandidate(plannerPage)
    const finishReason = `E2E完成-${Date.now()}`
    const revokeReason = `E2E撤销完成-${Date.now()}`

    let searchResult = await searchScheduleOrder(plannerPage, candidate)
    await searchResult.row.waitFor({ state: 'visible', timeout: 30000 })
    assert.ok(searchResult.rowData, `列表未找到目标工单 ${candidate.erpWorkOrderCode}`)
    await submitManualFinish(plannerPage, searchResult.row, finishReason)

    const incompleteRowsAfterFinish = await queryScheduleOrderByApi(
      plannerPage,
      FILTER_INCOMPLETE,
      candidate.erpWorkOrderCode
    )
    assert.equal(
      incompleteRowsAfterFinish.some((row) => row.erpWorkOrderCode === candidate.erpWorkOrderCode),
      false,
      '完成后工单必须从未完成状态筛选中消失。'
    )

    await setCompletionFilter(plannerPage, FILTER_COMPLETED)
    searchResult = await searchScheduleOrder(plannerPage, candidate)
    await searchResult.row.waitFor({ state: 'visible', timeout: 30000 })
    assert.ok(searchResult.rowData?.manualFinished, '完成后列表行必须标记 manualFinished=true。')
    assert.equal(searchResult.rowData?.status, STATUS_FINISHED, '完成后状态必须为已完成。')
    assert.equal(Number(searchResult.rowData?.progressPercent), 100, '完成后汇总进度必须为 100。')
    assert.equal(
      Number(searchResult.rowData?.completedQuantity),
      Number(searchResult.rowData?.totalQuantity),
      '完成后汇总完成数量必须等于总量。'
    )
    assert.equal(Number(searchResult.rowData?.uncompletedQuantity), 0, '完成后汇总未完成数量必须为 0。')
    const rowText = await searchResult.row.innerText()
    assert.ok(rowText.includes('已完成'), '完成后列表状态必须显示已完成。')
    await searchResult.row
      .getByRole('button', { name: /^撤销完成$/ })
      .waitFor({ state: 'visible', timeout: 30000 })
    await searchResult.row
      .locator('.schedule-order-pool__work-order-code--finished')
      .first()
      .waitFor({ state: 'visible', timeout: 30000 })
    await openProcessDialogAndAssertManualHint(plannerPage, searchResult.row)
    await openTraceAndAssert(plannerPage, searchResult.row, 'MANUAL_FINISH', finishReason)

    await login(adminPage, config.adminUsername, config.adminPassword)
    await openScheduleOrderPage(adminPage)
    await setCompletionFilter(adminPage, FILTER_COMPLETED)
    let adminSearchResult = await searchScheduleOrder(adminPage, candidate)
    await adminSearchResult.row.waitFor({ state: 'visible', timeout: 30000 })
    await submitRevoke(adminPage, adminSearchResult.row, revokeReason)

    await setCompletionFilter(adminPage, FILTER_INCOMPLETE)
    adminSearchResult = await searchScheduleOrder(adminPage, candidate)
    await adminSearchResult.row.waitFor({ state: 'visible', timeout: 30000 })
    assert.ok(
      !adminSearchResult.rowData?.manualFinished,
      '撤销完成后列表行必须清除 manualFinished 标记。'
    )
    assert.notEqual(
      Number(adminSearchResult.rowData?.progressPercent),
      100,
      '撤销完成后进度必须恢复真实报工口径，不能仍是 100。'
    )
    assert.ok(
      Number(adminSearchResult.rowData?.uncompletedQuantity) > 0,
      '撤销完成后未完成数量必须恢复为真实值。'
    )
    const revertedRowText = await adminSearchResult.row.innerText()
    assert.equal(revertedRowText.includes('撤销完成'), false, '撤销后列表不应继续显示撤销完成动作。')
    await adminSearchResult.row
      .getByRole('button', { name: /^完成$/ })
      .waitFor({ state: 'visible', timeout: 30000 })
    await openTraceAndAssert(adminPage, adminSearchResult.row, 'REVOKE_MANUAL_FINISH', revokeReason)

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          scheduleOrderId: candidate.id,
          workOrderCode: candidate.erpWorkOrderCode,
          finishReason,
          revokeReason,
          plannerUsername: config.plannerUsername,
          adminUsername: config.adminUsername
        },
        null,
        2
      )
    )
  } finally {
    await plannerPage.close()
    await adminPage.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
