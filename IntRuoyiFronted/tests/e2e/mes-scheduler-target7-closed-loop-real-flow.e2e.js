const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES scheduler target 7 closed-loop E2E.")
  }
}

const config = {
  baseUrl: (process.env.MES_SCHEDULER_TARGET7_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_SCHEDULER_TARGET7_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULER_TARGET7_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULER_TARGET7_E2E_PASSWORD || 'admin123',
  workOrderCode: process.env.MES_SCHEDULER_TARGET7_E2E_WORK_ORDER_CODE || 'CODexERP20260610E',
  taskCode: process.env.MES_SCHEDULER_TARGET7_E2E_TASK_CODE || 'TASK-CODEX-20260610-E-B010',
  headed: process.env.MES_SCHEDULER_TARGET7_E2E_HEADED === '1'
}

const quickEntries = [
  ['生产订单', '/mes/pro/work-order'],
  ['排产工单池', '/mes/pro/schedule-order'],
  ['工艺路线与资源', '/mes/pro/route'],
  ['今日资源调整', '/mes/pro/route'],
  ['生成排程日历', '/mes/pro/schedule-calendar'],
  ['生产任务', '/mes/pro/task'],
  ['生产报工', '/mes/pro/feedback']
]

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
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
  throw new Error(`No visible input found for ${label}`)
}

async function login(page, redirect = '/mes/pro/scheduler-workbench') {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirect)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirect)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  if ((await page.locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = page.locator('.el-select input[role="combobox"]:visible').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
    await page.keyboard.type(config.tenant)
    await tenantInput.press('Enter')
    await tenantInput.press('Tab')
  } else if ((await page.locator('input[placeholder="请输入租户名称"]').count()) > 0) {
    await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.locator('.el-button--primary:visible').first().click()
  ])
  const loginBody = await loginResponse.json()
  if (loginBody.code !== 0) {
    throw new Error(`登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  }
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openWorkbench(page) {
  const summaryPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/scheduler-workbench/summary') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/scheduler-workbench`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const summaryResponse = await summaryPromise
  const summaryBody = await summaryResponse.json()
  assert.equal(summaryBody.code, 0, `工作台 summary 接口业务错误: ${summaryBody.msg || summaryBody.code}`)
  await page.locator('.scheduler-workbench').waitFor({ state: 'visible', timeout: 30000 })
  for (const text of ['快捷入口', '夜间自动重排', '瓶颈建议', '报工偏差', '今日可用产能']) {
    await page.getByText(text).first().waitFor({ state: 'visible', timeout: 30000 })
  }
  return summaryBody.data
}

async function verifyQuickEntryNavigation(page) {
  for (const [label, targetPath] of quickEntries) {
    await openWorkbench(page)
    const button = page.locator('.scheduler-workbench__quick-links button').filter({ hasText: label }).first()
    await button.waitFor({ state: 'visible', timeout: 30000 })
    await button.click()
    await page.waitForURL((url) => url.pathname.includes(targetPath), { timeout: 30000 })
    await settle(page)
  }
}

async function searchWorkOrderInUi(page) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/work-order/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/work-order`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await responsePromise.catch(() => {})
  await page.getByText('工单编码').first().waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(page.locator('input[placeholder="请输入工单编码"]'), config.workOrderCode, 'work order code')
  const [searchResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/work-order/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: /搜索/ }).first().click()
  ])
  const body = await searchResponse.json()
  assert.equal(body.code, 0, `生产工单查询接口业务错误: ${body.msg || body.code}`)
  const workOrder = body.data?.list?.find?.((row) => row.code === config.workOrderCode)
  assert.ok(workOrder, `测试租户缺少 ERP 同步生产工单 ${config.workOrderCode}`)
  await page.locator('tr.el-table__row').filter({ hasText: config.workOrderCode }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  assert.ok(Number(workOrder.quantity) > 0, 'ERP 生产工单数量必须大于 0')
  return workOrder
}

async function searchScheduleOrderAndSnapshotInUi(page, workOrder) {
  const firstPagePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await firstPagePromise.catch(() => {})
  const schedulePanel = page.locator('.schedule-order-pool > .el-card, .schedule-order-pool > .content-wrap').first()
  await fillFirstVisible(schedulePanel.locator('input[placeholder="请输入工单编码"]'), config.workOrderCode, 'work order code')
  const [pageResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    schedulePanel.getByRole('button', { name: /搜索/ }).click()
  ])
  const body = await pageResponse.json()
  assert.equal(body.code, 0, `排产工单查询接口业务错误: ${body.msg || body.code}`)
  const scheduleOrder = body.data?.list?.find?.((row) => row.erpWorkOrderCode === config.workOrderCode)
  assert.ok(scheduleOrder, `测试租户缺少排产工单 ${config.workOrderCode}`)
  assert.equal(String(scheduleOrder.quantity), String(workOrder.quantity), '排产数量必须等于 ERP 生产工单数量')
  assert.equal(typeof scheduleOrder.progressPercent, 'number', '排产工单必须返回进度百分比')
  assert.ok(scheduleOrder.promiseDate || scheduleOrder.promisedDeliveryDate, '排产工单必须有承诺交期')
  assert.ok(scheduleOrder.routeVersion && /^ROUTE-/.test(scheduleOrder.routeVersion), '排产工单必须固化自动编号路线版本')

  const row = schedulePanel.locator('tr.el-table__row').filter({ hasText: config.workOrderCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const [processResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/schedule-order/process-list') && response.status() === 200,
      { timeout: 60000 }
    ),
    row.getByText('工序快照').click()
  ])
  const processBody = await processResponse.json()
  assert.equal(processBody.code, 0, `工序快照接口业务错误: ${processBody.msg || processBody.code}`)
  assert.ok(Array.isArray(processBody.data) && processBody.data.length > 0, '排产工单必须有工序快照')
  assert.ok(
    processBody.data.some((item) => ['MACHINE', 'WORKER', 'UNCONFIGURED'].includes(item.capacitySource)),
    '工序快照必须声明设备/人工/未配置产能来源'
  )
  assert.ok(processBody.data.some((item) => item.shiftHours !== undefined), '工序快照必须包含班次小时')
  assert.ok(processBody.data.some((item) => item.shiftCapacityTotal !== undefined), '工序快照必须包含班次产能')
  await page.getByText('总产能/班次').first().waitFor({ state: 'visible', timeout: 30000 })
  return { scheduleOrder, processes: processBody.data }
}

async function openFeedbackAttributionUi(page) {
  const pagePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/feedback/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/feedback`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await pagePromise.catch(() => {})
  await page.getByText('第三方导入').waitFor({ state: 'visible', timeout: 30000 })
  await page.getByRole('tab', { name: '待归属' }).click()
  await page.getByText('归属状态').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function apiGet(page, path) {
  const result = await page.evaluate(async (requestPath) => {
    const unwrapCacheValue = (value) => {
      if (!value || typeof value !== 'object') {
        return value
      }
      for (const field of ['accessToken', 'v', 'value', 'data']) {
        if (Object.prototype.hasOwnProperty.call(value, field)) {
          return unwrapCacheValue(value[field])
        }
      }
      return value
    }
    const readCache = (key) => {
      const storages = [localStorage, sessionStorage]
      let raw
      for (const storage of storages) {
        raw = storage.getItem(key)
        if (raw) break
        const matchedKey = Object.keys(storage).find((item) => item === key || item.endsWith(key))
        if (matchedKey) {
          raw = storage.getItem(matchedKey)
          break
        }
      }
      if (!raw) return undefined
      try {
        const parsed = JSON.parse(raw)
        const unwrapped = unwrapCacheValue(parsed)
        if (typeof unwrapped === 'string' && unwrapped.startsWith('"') && unwrapped.endsWith('"')) {
          return unwrapped.slice(1, -1)
        }
        return unwrapped
      } catch (error) {
        if (raw.startsWith('"') && raw.endsWith('"')) {
          return raw.slice(1, -1)
        }
        return raw
      }
    }
    const accessToken = readCache('ACCESS_TOKEN')
    const tenantId = readCache('tenantId')
    const visitTenantId = readCache('visitTenantId')
    const headers = {
      'Cache-Control': 'no-cache',
      Pragma: 'no-cache'
    }
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`
    }
    if (tenantId) {
      headers['tenant-id'] = String(tenantId)
    }
    if (visitTenantId && accessToken) {
      headers['visit-tenant-id'] = String(visitTenantId)
    }
    const response = await fetch(requestPath, { credentials: 'omit', headers })
    return {
      status: response.status,
      body: await response.json()
    }
  }, `/admin-api${path}`)
  assert.equal(result.status, 200, `接口 HTTP 状态异常: ${path}`)
  assert.equal(result.body.code, 0, `接口业务错误 ${path}: ${result.body.msg || result.body.code}`)
  return result.body.data
}

async function verifyClosedLoopData(page, workOrder, scheduleOrder, processes, summary) {
  const imported = await apiGet(
    page,
    `/mes/pro/feedback/import-record/page?pageNo=1&pageSize=50&attributionStatus=ATTRIBUTED`
  )
  const attributed = imported.list?.find?.(
    (row) => row.workOrderCode === config.workOrderCode && row.taskCode === config.taskCode
  )
  assert.ok(attributed, `测试租户缺少已归属报工导入记录 ${config.taskCode}`)
  assert.equal(String(attributed.scheduleOrderId), String(scheduleOrder.id), '报工归属必须指向当前排产工单')
  assert.ok(attributed.scheduleOrderProcessId, '报工归属必须指向排产工单工序')

  const refreshedSummary = await apiGet(page, `/mes/pro/scheduler-workbench/summary?date=${new Date().toISOString().slice(0, 10)}`)
  assert.ok('todayAvailableCapacity' in refreshedSummary, '工作台必须返回今日可用产能')
  assert.ok('nightlyReplanText' in refreshedSummary, '工作台必须返回夜间重排说明')
  assert.ok('reportedDeviationText' in refreshedSummary, '工作台必须返回报工偏差说明')
  assert.ok(Array.isArray(refreshedSummary.bottlenecks), '工作台必须返回瓶颈工序列表')

  assert.equal(summary.nightlyReplanText, refreshedSummary.nightlyReplanText, '工作台夜间重排说明刷新前后应一致')

  return {
    workOrderId: workOrder.id,
    scheduleOrderId: scheduleOrder.id,
    routeVersion: scheduleOrder.routeVersion,
    processCount: processes.length,
    attributedImportRecordId: attributed.id,
    reportedTaskCode: attributed.taskCode,
    todayAvailableCapacity: refreshedSummary.todayAvailableCapacity,
    bottleneckCount: refreshedSummary.bottlenecks.length
  }
}

async function main() {
  assert.notEqual(config.tenant, '芋道源码', '目标 7 真实回归禁止使用芋道源码/admin 租户')
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(page)
    const summary = await openWorkbench(page)
    await verifyQuickEntryNavigation(page)
    const workOrder = await searchWorkOrderInUi(page)
    const { scheduleOrder, processes } = await searchScheduleOrderAndSnapshotInUi(page, workOrder)
    await openFeedbackAttributionUi(page)
    const evidence = await verifyClosedLoopData(page, workOrder, scheduleOrder, processes, summary)
    console.log(JSON.stringify({
      status: 'PASS',
      tenant: config.tenant,
      baseUrl: config.baseUrl,
      workOrderCode: config.workOrderCode,
      ...evidence
    }, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
