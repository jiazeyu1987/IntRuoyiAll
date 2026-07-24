const assert = require('node:assert/strict')

function parseWorkOrderCodes() {
  const configuredCodes = process.env.MES_SCHEDULE_ORDER_E2E_WORK_ORDER_CODES
  if (configuredCodes) {
    return configuredCodes
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
  return [process.env.MES_SCHEDULE_ORDER_E2E_WORK_ORDER_CODE || '881MO090863', '881MO090880']
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      "Playwright is required for MES schedule order pool real E2E. Run in a workspace where 'playwright' is installed."
    )
  }
}

const config = {
  baseUrl: (process.env.MES_SCHEDULE_ORDER_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_SCHEDULE_ORDER_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULE_ORDER_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULE_ORDER_E2E_PASSWORD || '111111',
  workOrderCodes: parseWorkOrderCodes(),
  headed: process.env.MES_SCHEDULE_ORDER_E2E_HEADED === '1',
  readonly:
    process.env.MES_SCHEDULE_ORDER_E2E_READONLY === '1' ||
    process.env.MES_SCHEDULE_ORDER_E2E_TENANT === '芋道源码'
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch((error) => {
    if (!String(error?.message || '').includes('Timeout')) {
      throw error
    }
  })
  await page.waitForTimeout(500)
}

async function readJsonResponse(page, response, label) {
  try {
    return await response.json()
  } catch (error) {
    if (!String(error?.message || '').includes('No resource with given identifier')) {
      throw error
    }
    const replay = await page.request.get(response.url())
    assert.equal(
      replay.status(),
      200,
      `${label} 响应体读取失败后重取接口 HTTP 异常: ${replay.status()}`
    )
    return replay.json()
  }
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
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

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
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginBody = await loginResponse.json()
  if (![0, 200].includes(loginBody.code)) {
    throw new Error(`登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  }
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const accessToken = await page.evaluate(() => {
    const raw = localStorage.getItem('ACCESS_TOKEN')
    if (!raw) return ''
    const cached = JSON.parse(raw)
    return typeof cached?.v === 'string' ? JSON.parse(cached.v) : cached?.v || raw
  })
  const permissionResponse = await page.request.get(
    `${config.baseUrl}/admin-api/system/auth/get-permission-info`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': config.tenant === '芋道源码' ? '1' : '122'
      }
    }
  )
  const permissionBody = await permissionResponse.json()
  assert.equal(permissionBody.code, 0, `登录权限接口业务错误: ${JSON.stringify(permissionBody)}`)
  const permissions = permissionBody.data?.permissions || []
  assert.ok(
    config.readonly || permissions.includes('mes:pro-schedule-order:update'),
    `写入型测试租户必须拥有同步进度权限，当前排产工单权限: ${JSON.stringify(permissions.filter((item) => item.includes('pro-schedule-order')))}`
  )
}

async function openScheduleOrderPool(page) {
  const scheduleOrderPagePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const scheduleOrderPageResponse = await scheduleOrderPagePromise
  const body = await readJsonResponse(page, scheduleOrderPageResponse, '排产工单列表')
  assert.equal(body.code, 0, `排产工单列表接口业务错误: ${JSON.stringify(body)}`)

  const pool = page.locator('.schedule-order-pool')
  await pool.waitFor({ state: 'visible', timeout: 30000 })
  const poolText = await pool.innerText()
  assert.ok(poolText.includes('排产工单'), '页面必须显示排产工单区域。')
  assert.equal(poolText.includes('来源生产工单'), false, '排产工单页签不得显示来源生产工单卡片。')
  assert.equal(poolText.includes('差异提示'), false, '排产工单页签不得显示差异提示卡片。')
  assert.equal(poolText.includes('ERP工单编码'), false, '排产工单页签不得显示 ERP工单编码 文案。')

  for (const text of [
    '工单编码',
    '总量',
    '完成',
    '未完',
    '待审批',
    '待检',
    '超报',
    '承诺交期',
    '最晚开工',
    '计划开工',
    '计划完成',
    '进度',
    '差异',
    '风险',
    '当前工序',
    '工艺路线'
  ]) {
    await pool.getByText(text).first().waitFor({ state: 'visible', timeout: 30000 })
  }
}

async function verifyWorkOrderAdmissionDialog(page) {
  const pool = page.locator('.schedule-order-pool')
  const [workOrderResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/admission-diff') &&
        response.status() === 200,
      { timeout: 60000 }
    ),
    pool.getByRole('button', { name: /同步工单/ }).click()
  ])
  const body = await readJsonResponse(page, workOrderResponse, '待同步差异')
  assert.equal(body.code, 0, `待同步差异接口业务错误: ${body.msg || body.code}`)
  assert.ok(Array.isArray(body.data?.list), '待同步差异接口必须返回分页列表。')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '待同步差异' }).last()
  for (const text of [
    '工单编码',
    '产品编号',
    '选中工单加入排产工单池',
    '总数量',
    '入池状态',
    '不可排原因'
  ]) {
    await dialog.getByText(text).first().waitFor({ state: 'visible', timeout: 30000 })
  }
  await dialog
    .locator('input[placeholder="承诺交期"]')
    .waitFor({ state: 'visible', timeout: 30000 })
  await closeVisibleDialog(page)
}

function formatQuantity(value) {
  const numberValue = Number(value)
  assert.ok(Number.isFinite(numberValue), `数量字段必须是有效数字: ${value}`)
  return numberValue.toFixed(2)
}

function formatPercent(value) {
  const numberValue = Number(value ?? 0)
  assert.ok(Number.isFinite(numberValue), `进度字段必须是有效数字: ${value}`)
  return Math.min(100, Math.max(0, numberValue)).toFixed(2)
}

function assertRequiredScheduleOrderFields(scheduleOrder, workOrderCode) {
  for (const field of [
    'totalQuantity',
    'completedQuantity',
    'uncompletedQuantity',
    'progressPercent'
  ]) {
    assert.equal(
      typeof scheduleOrder[field],
      'number',
      `${workOrderCode} 排产工单接口必须返回数字字段 ${field}`
    )
  }
  assert.ok(
    scheduleOrder.totalQuantity >= scheduleOrder.completedQuantity,
    `${workOrderCode} 总数量不得小于已完成数量`
  )
  assert.equal(
    Number((scheduleOrder.totalQuantity - scheduleOrder.completedQuantity).toFixed(6)),
    Number(scheduleOrder.uncompletedQuantity.toFixed(6)),
    `${workOrderCode} 未完成数量必须等于总数量减已完成数量`
  )
  for (const field of ['latestStartTime', 'plannedStartTime', 'plannedEndTime']) {
    assert.ok(
      Object.prototype.hasOwnProperty.call(scheduleOrder, field),
      `${workOrderCode} 排产工单接口必须包含 ${field}`
    )
  }
}

async function closeVisibleDialog(page) {
  const dialog = page.locator('.el-dialog:visible').last()
  if ((await dialog.count()) === 0) {
    return
  }
  await page.keyboard.press('Escape')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  await page.waitForTimeout(300)
}

async function clickRowMoreAction(page, row, actionText) {
  await row.getByText('更多').waitFor({ state: 'visible', timeout: 30000 })
  await row.getByText('更多').click()
  const action = page
    .locator('.el-dropdown-menu:visible')
    .getByText(actionText, { exact: true })
    .first()
  await action.waitFor({ state: 'visible', timeout: 30000 })
  await action.click()
}

async function verifyRouteLink(page, row, scheduleOrder) {
  assert.ok(scheduleOrder.routeId, `${scheduleOrder.erpWorkOrderCode} 缺少可点击工艺路线 ID`)
  const routeLink = row
    .getByRole('button', {
      name: scheduleOrder.routeName || scheduleOrder.routeCode || String(scheduleOrder.routeId)
    })
    .first()
  await routeLink.waitFor({ state: 'visible', timeout: 30000 })
  const [routeResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route/get') &&
        response.url().includes(`id=${scheduleOrder.routeId}`) &&
        response.status() === 200,
      { timeout: 60000 }
    ),
    routeLink.click()
  ])
  const body = await readJsonResponse(page, routeResponse, '工艺路线详情')
  assert.equal(body.code, 0, `工艺路线详情接口业务错误: ${body.msg || body.code}`)
  await page.waitForURL(
    (url) =>
      url.pathname.includes(`/mes/pro/route/edit/${scheduleOrder.routeId}`) &&
      url.searchParams.get('tab') === 'schedule-config',
    { timeout: 60000 }
  )
  if (scheduleOrder.currentRouteProcessId) {
    assert.equal(
      new URL(page.url()).searchParams.get('routeProcessId'),
      String(scheduleOrder.currentRouteProcessId),
      '工艺流程编辑页必须携带当前路线工序定位参数。'
    )
  }
  await page
    .getByText(scheduleOrder.routeName || scheduleOrder.routeCode)
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
}

async function verifyDailyCompare(page, row, scheduleOrder) {
  const [dailyCompareResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/daily-compare') &&
        response.url().includes(`scheduleOrderId=${scheduleOrder.id}`) &&
        response.status() === 200,
      { timeout: 60000 }
    ),
    clickRowMoreAction(page, row, '对比')
  ])
  const body = await readJsonResponse(page, dailyCompareResponse, '报工计划对比')
  assert.equal(body.code, 0, `报工计划对比接口业务错误: ${body.msg || body.code}`)
  assert.ok(Array.isArray(body.data), '报工计划对比接口必须返回数组。')
  const compareDialog = page
    .locator('.el-dialog:visible')
    .filter({ hasText: '报工计划对比' })
    .last()
  await compareDialog.getByText('计划数量').waitFor({ state: 'visible', timeout: 30000 })
  await compareDialog.getByText('实际报工').waitFor({ state: 'visible', timeout: 30000 })
  await compareDialog.getByText('差异').waitFor({ state: 'visible', timeout: 30000 })
  await compareDialog.getByText('状态').waitFor({ state: 'visible', timeout: 30000 })
  await closeVisibleDialog(page)
}

async function verifySyncProgress(page, row, scheduleOrder) {
  if (config.readonly) {
    const disabledMessage = `${config.tenant} 只读最终验证跳过同步进度写入。`
    return { skipped: true, reason: disabledMessage }
  }
  const [syncResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/sync-progress') &&
        response.url().includes(`scheduleOrderId=${scheduleOrder.id}`) &&
        response.status() === 200,
      { timeout: 60000 }
    ),
    clickRowMoreAction(page, row, '同步')
  ])
  const body = await readJsonResponse(page, syncResponse, '同步报工进度')
  assert.equal(body.code, 0, `同步报工进度接口业务错误: ${JSON.stringify(body)}`)
  await page.getByText('报工进度已同步').waitFor({ state: 'visible', timeout: 30000 })
  return { skipped: false }
}

async function searchScheduleOrder(page, workOrderCode) {
  const schedulePanel = page
    .locator('.schedule-order-pool > .el-card, .schedule-order-pool > .content-wrap')
    .first()
  await fillFirstVisible(
    schedulePanel.locator('input[placeholder="请输入工单编码"]'),
    workOrderCode,
    'work order code'
  )
  const [pageResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
        response.status() === 200,
      { timeout: 60000 }
    ),
    schedulePanel.getByRole('button', { name: /搜索/ }).click()
  ])
  const body = await readJsonResponse(page, pageResponse, '排产工单查询')
  assert.equal(body.code, 0, `排产工单查询接口业务错误: ${body.msg || body.code}`)
  const scheduleOrder = body.data?.list?.find?.((row) => row.erpWorkOrderCode === workOrderCode)
  assert.ok(scheduleOrder, `测试租户缺少排产工单 ${workOrderCode}`)
  assertRequiredScheduleOrderFields(scheduleOrder, workOrderCode)
  assert.equal(typeof scheduleOrder.progressPercent, 'number', '排产工单接口必须返回进度百分比。')
  assert.ok(
    scheduleOrder.progressPercent >= 0 && scheduleOrder.progressPercent <= 100,
    '进度百分比必须可被进度条直接渲染。'
  )
  assert.ok(
    Object.prototype.hasOwnProperty.call(scheduleOrder, 'currentProcessId'),
    '排产工单接口必须返回当前工序 ID 字段。'
  )
  assert.ok(
    Object.prototype.hasOwnProperty.call(scheduleOrder, 'currentProcessName'),
    '排产工单接口必须返回当前工序名称字段。'
  )
  assert.ok(
    scheduleOrder.routeVersion && /^ROUTE-/.test(scheduleOrder.routeVersion),
    '排产工单必须固化自动编号的路线版本。'
  )

  const row = schedulePanel.locator('tr.el-table__row').filter({ hasText: workOrderCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  for (const actionText of ['调整', '交期', '更多']) {
    await row.getByText(actionText).waitFor({ state: 'visible', timeout: 30000 })
  }
  const rowText = await row.innerText()
  assert.ok(
    rowText.includes(`${formatPercent(scheduleOrder.progressPercent)}%`),
    '排产工单列表必须显示进度百分比文本。'
  )
  assert.ok(
    rowText.includes(formatQuantity(scheduleOrder.totalQuantity)),
    '排产工单列表必须显示总数量。'
  )
  assert.ok(
    rowText.includes(formatQuantity(scheduleOrder.completedQuantity)),
    '排产工单列表必须显示已完成数量。'
  )
  assert.ok(
    rowText.includes(formatQuantity(scheduleOrder.uncompletedQuantity)),
    '排产工单列表必须显示未完成数量。'
  )
  if (scheduleOrder.currentProcessId) {
    assert.ok(
      rowText.includes(
        scheduleOrder.currentProcessName ||
          scheduleOrder.currentProcessCode ||
          String(scheduleOrder.currentProcessId)
      ),
      '排产工单列表必须直接显示当前工序。'
    )
  }
  if (
    scheduleOrder.plannedEndTime &&
    scheduleOrder.promiseDate &&
    scheduleOrder.plannedEndTime.slice(0, 10) > scheduleOrder.promiseDate
  ) {
    await row
      .locator('.schedule-order-pool__warning-text')
      .first()
      .waitFor({ state: 'visible', timeout: 30000 })
  }
  if (
    scheduleOrder.plannedStartTime &&
    scheduleOrder.latestStartTime &&
    scheduleOrder.plannedStartTime > scheduleOrder.latestStartTime
  ) {
    await row
      .locator('.schedule-order-pool__risk-text')
      .first()
      .waitFor({ state: 'visible', timeout: 30000 })
  }
  await row.locator('.el-progress').first().waitFor({ state: 'visible', timeout: 30000 })
  assert.match(rowText, /ROUTE-/, '排产工单列表必须显示路线版本。')

  const [processResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/process-list') &&
        response.status() === 200,
      { timeout: 60000 }
    ),
    clickRowMoreAction(page, row, '快照')
  ])
  const processBody = await readJsonResponse(page, processResponse, '排产工序快照')
  assert.equal(
    processBody.code,
    0,
    `排产工序快照接口业务错误: ${processBody.msg || processBody.code}`
  )
  assert.ok(
    Array.isArray(processBody.data) && processBody.data.length > 0,
    '排产工单必须存在工序快照。'
  )
  const processDialog = page
    .locator('.el-dialog:visible')
    .filter({ hasText: '排产工序快照' })
    .last()
  await processDialog.getByText('总产能/班次').waitFor({ state: 'visible', timeout: 30000 })
  await processDialog.getByText('已报工').waitFor({ state: 'visible', timeout: 30000 })
  await processDialog.getByText('剩余数量').waitFor({ state: 'visible', timeout: 30000 })
  await processDialog.getByText('工序进度').waitFor({ state: 'visible', timeout: 30000 })
  await closeVisibleDialog(page)

  await verifyDailyCompare(page, row, scheduleOrder)
  const syncResult = await verifySyncProgress(page, row, scheduleOrder)
  await verifyRouteLink(page, row, scheduleOrder)

  return { scheduleOrder, processes: processBody.data, syncResult }
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(page)
    await openScheduleOrderPool(page)
    await verifyWorkOrderAdmissionDialog(page)
    const results = []
    for (let index = 0; index < config.workOrderCodes.length; index += 1) {
      if (index > 0) {
        await openScheduleOrderPool(page)
      }
      const workOrderCode = config.workOrderCodes[index]
      const { scheduleOrder, processes, syncResult } = await searchScheduleOrder(
        page,
        workOrderCode
      )
      results.push({
        workOrderCode,
        scheduleOrderId: scheduleOrder.id,
        progressPercent: scheduleOrder.progressPercent,
        totalQuantity: scheduleOrder.totalQuantity,
        completedQuantity: scheduleOrder.completedQuantity,
        uncompletedQuantity: scheduleOrder.uncompletedQuantity,
        processCount: processes.length,
        syncProgressSkipped: syncResult.skipped === true
      })
    }
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          scheduleOrders: results
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
