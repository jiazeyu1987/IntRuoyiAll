const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES scheduler target 7 closed-loop E2E.")
  }
}

function requiredEnv(name) {
  const value = process.env[name]?.trim()
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`)
  }
  return value
}

const config = {
  baseUrl: (process.env.MES_SCHEDULER_TARGET7_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: requiredEnv('MES_SCHEDULER_TARGET7_E2E_TENANT'),
  username: requiredEnv('MES_SCHEDULER_TARGET7_E2E_USERNAME'),
  password: requiredEnv('MES_SCHEDULER_TARGET7_E2E_PASSWORD'),
  workOrderCode: process.env.MES_SCHEDULER_TARGET7_E2E_WORK_ORDER_CODE || 'CODexERP20260610E',
  taskCode: process.env.MES_SCHEDULER_TARGET7_E2E_TASK_CODE || 'TASK-CODEX-20260610-E-B010',
  headed: process.env.MES_SCHEDULER_TARGET7_E2E_HEADED === '1'
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    const editable = await item
      .evaluate((element) => {
        if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement) {
          return !element.readOnly && !element.disabled
        }
        return true
      })
      .catch(() => true)
    if ((await item.isVisible()) && editable) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

function summarizePageResponse(response, body, rowCodeField = 'code') {
  const list = Array.isArray(body?.data?.list) ? body.data.list : []
  return {
    url: response.url(),
    businessCode: body?.code,
    total: body?.data?.total,
    codes: list.slice(0, 10).map((row) => row?.[rowCodeField] || row?.code || row?.erpWorkOrderCode)
  }
}

function formatResponseEvidence(summary) {
  return JSON.stringify(summary)
}

async function applyTableMultiFilter(page, tableKey, fieldKey, fieldLabel, value, responseUrlPart, label) {
  const filter = page.locator(`.table-multi-filter[data-table-key="${tableKey}"]`).first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  if ((await filter.locator('.table-multi-filter__condition-row').count()) === 0) {
    await filter.getByRole('button', { name: '新增筛选条件' }).click()
  }

  let field = filter.locator(`.table-multi-filter-field[data-filter-key="${fieldKey}"]`).first()
  if ((await field.count()) === 0 || !(await field.isVisible().catch(() => false))) {
    await filter.locator('.table-multi-filter__field-select').first().click()
    await page.getByRole('option', { name: fieldLabel, exact: true }).last().click()
    field = filter.locator(`.table-multi-filter-field[data-filter-key="${fieldKey}"]`).first()
  }
  await field.waitFor({ state: 'visible', timeout: 30000 })
  const valueInput = field.locator('.table-multi-filter-field__value input')
  await fillFirstVisible(valueInput, value, label)
  const actualValue = await valueInput.first().inputValue()
  assert.equal(actualValue, value, `${label} 筛选值未写入当前页面输入框`)
  await filter
    .locator('.table-multi-filter__tabs .el-tabs__item')
    .filter({ hasText: value })
    .first()
    .waitFor({ state: 'visible', timeout: 5000 })

  const [searchResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes(responseUrlPart) && response.status() === 200,
      { timeout: 60000 }
    ),
    filter.getByRole('button', { name: /查询/ }).click()
  ])
  return { filter, searchResponse }
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
  await page.waitForFunction(() => !window.location.href.includes('/login'), null, { timeout: 60000 })
}

async function openWorkbench(page) {
  const summaryPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/scheduler-workbench/summary') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/scheduler-workbench?target7E2e=${Date.now()}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const summaryResponse = await summaryPromise
  const summaryBody = await summaryResponse.json()
  assert.equal(summaryBody.code, 0, `工作台 summary 接口业务错误: ${summaryBody.msg || summaryBody.code}`)
  await page.locator('.scheduler-workbench').waitFor({ state: 'visible', timeout: 30000 })
  for (const text of ['工序列表', '排产设置']) {
    await page.getByText(text).first().waitFor({ state: 'visible', timeout: 30000 })
  }
  return summaryBody.data
}

async function verifySchedulerRuntimeStatusUi(page) {
  await openWorkbench(page)
  await page.getByRole('button', { name: /排产设置/ }).first().click()
  for (const text of ['默认允许使用夜班', '可用夜班班次与产能', '自动排产任务', '处理器 mesProNightlyReplanJob']) {
    await page.getByText(text).first().waitFor({ state: 'visible', timeout: 30000 })
  }
  await page.keyboard.press('Escape')
  await settle(page)
}

async function searchWorkOrderInUi(page) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/work-order/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/work-order`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await responsePromise.catch(() => {})
  const { searchResponse } = await applyTableMultiFilter(
    page,
    'mes.pro.workorder.main',
    'code',
    '工单编号',
    config.workOrderCode,
    '/admin-api/mes/pro/work-order/page',
    'work order code'
  )
  const body = await searchResponse.json()
  assert.equal(body.code, 0, `生产工单查询接口业务错误: ${body.msg || body.code}`)
  const workOrder = body.data?.list?.find?.((row) => row.code === config.workOrderCode)
  assert.ok(
    workOrder,
    `测试租户缺少 ERP 同步生产工单 ${config.workOrderCode}; ${formatResponseEvidence(
      summarizePageResponse(searchResponse, body, 'code')
    )}`
  )
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
  const { searchResponse: pageResponse } = await applyTableMultiFilter(
    page,
    'mes.pro.scheduleOrder.main',
    'erpWorkOrderCode',
    '来源生产工单号',
    config.workOrderCode,
    '/admin-api/mes/pro/schedule-order/page',
    'source work order code'
  )
  const body = await pageResponse.json()
  assert.equal(body.code, 0, `排产工单查询接口业务错误: ${body.msg || body.code}`)
  const scheduleOrder = body.data?.list?.find?.((row) => row.erpWorkOrderCode === config.workOrderCode)
  assert.ok(
    scheduleOrder,
    `测试租户缺少排产工单 ${config.workOrderCode}; ${formatResponseEvidence(
      summarizePageResponse(pageResponse, body, 'erpWorkOrderCode')
    )}`
  )
  assert.equal(String(scheduleOrder.quantity), String(workOrder.quantity), '排产数量必须等于 ERP 生产工单数量')
  assert.equal(typeof scheduleOrder.progressPercent, 'number', '排产工单必须返回进度百分比')
  assert.ok(scheduleOrder.promiseDate || scheduleOrder.promisedDeliveryDate, '排产工单必须有承诺交期')
  assert.ok(Number(scheduleOrder.routeId) > 0, '排产工单必须固化工艺路线编号')
  assert.ok(
    typeof scheduleOrder.routeVersion === 'string' && scheduleOrder.routeVersion.trim().length > 0,
    `排产工单必须固化路线版本; ${formatResponseEvidence({
      id: scheduleOrder.id,
      code: scheduleOrder.code,
      erpWorkOrderCode: scheduleOrder.erpWorkOrderCode,
      routeId: scheduleOrder.routeId,
      routeCode: scheduleOrder.routeCode,
      routeName: scheduleOrder.routeName,
      routeVersion: scheduleOrder.routeVersion,
      productCode: scheduleOrder.productCode,
      quantity: scheduleOrder.quantity
    })}`
  )

  const row = page
    .locator('[data-user-table-key="mes.pro.scheduleOrder.main"] tr.el-table__row')
    .filter({ hasText: config.workOrderCode })
    .first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const [processResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/schedule-order/process-list') && response.status() === 200,
      { timeout: 60000 }
    ),
    row.getByRole('button', { name: '查看' }).click()
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
  await page
    .locator('[data-user-table-key="mes.pro.scheduleOrder.processRoute"]')
    .getByText('班次产能')
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
  return { scheduleOrder, processes: processBody.data }
}

async function openFeedbackAttributionUi(page) {
  const pagePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/feedback/import-record/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/feedback?tab=import-record&target7E2e=${Date.now()}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await pagePromise.catch(() => {})
  await page.getByText('第三方导入').waitFor({ state: 'visible', timeout: 30000 })
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
    `/mes/pro/feedback/import-record/page?pageNo=1&pageSize=200&attributionStatus=ATTRIBUTED`
  )
  const attributed = imported.list?.find?.(
    (row) => row.workOrderCode === config.workOrderCode && row.taskCode === config.taskCode
  )
  assert.ok(
    attributed,
    `测试租户缺少已归属报工导入记录 ${config.taskCode}; ${formatResponseEvidence({
      total: imported.total,
      sample: (imported.list || []).slice(0, 20).map((row) => ({
        id: row.id,
        taskCode: row.taskCode,
        workOrderCode: row.workOrderCode,
        attributionStatus: row.attributionStatus,
        scheduleOrderId: row.scheduleOrderId,
        scheduleOrderProcessId: row.scheduleOrderProcessId
      }))
    })}`
  )
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
    await verifySchedulerRuntimeStatusUi(page)
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
