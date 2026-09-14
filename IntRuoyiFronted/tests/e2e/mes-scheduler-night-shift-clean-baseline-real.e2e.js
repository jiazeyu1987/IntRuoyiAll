const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for the scheduler night-shift clean baseline E2E.')
  }
}

function requiredEnv(name) {
  const value = process.env[name]?.trim()
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`)
  }
  return value
}

function timestamp() {
  return new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
}

function nextWorkdayDateTime(additionalWorkdays, hour) {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  while (date.getDay() === 0 || date.getDay() === 6) {
    date.setDate(date.getDate() + 1)
  }
  let remaining = additionalWorkdays
  while (remaining > 0) {
    date.setDate(date.getDate() + 1)
    if (date.getDay() !== 0 && date.getDay() !== 6) {
      remaining -= 1
    }
  }
  date.setHours(hour, 0, 0, 0)
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const artifactDir = path.join(
  workspaceRoot,
  'doc',
  'tasks',
  '20260813-scheduler-seven-issues-closure',
  'artifacts',
  'night-shift-save-validation',
  'clean-baseline'
)
const runStamp = timestamp()
const resumeWorkOrderCode = process.env.MES_SCHEDULER_NIGHT_E2E_RESUME_WORK_ORDER_CODE?.trim()
const config = {
  baseUrl: (process.env.MES_SCHEDULER_NIGHT_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: requiredEnv('MES_SCHEDULER_NIGHT_E2E_TENANT'),
  username: requiredEnv('MES_SCHEDULER_NIGHT_E2E_USERNAME'),
  password: requiredEnv('MES_SCHEDULER_NIGHT_E2E_PASSWORD'),
  routeCode: requiredEnv('MES_SCHEDULER_NIGHT_E2E_ROUTE_CODE'),
  processName: process.env.MES_SCHEDULER_NIGHT_E2E_PROCESS_NAME || '吹球囊成型',
  workOrderCode: resumeWorkOrderCode || `SCHED7-NIGHT-${runStamp}`,
  resumeWorkOrderCode,
  quantity: Number(process.env.MES_SCHEDULER_NIGHT_E2E_QUANTITY || '5'),
  syncWaitMs: Number(process.env.MES_SCHEDULER_NIGHT_E2E_SYNC_WAIT_MS || '180000'),
  headed: process.env.MES_SCHEDULER_NIGHT_E2E_HEADED === '1'
}

assert.equal(config.tenant, '测试租户', '干净基线只能创建在测试租户')
assert.equal(config.username, 'aoteman', '干净基线只能由已授权测试账号 aoteman 创建')
assert.ok(Number.isFinite(config.quantity) && config.quantity > 0, '测试工单数量必须大于 0')

function ensureArtifactDir() {
  fs.mkdirSync(artifactDir, { recursive: true })
  fs.rmSync(path.join(artifactDir, 'e2e-error.txt'), { force: true })
}

function writeJson(name, payload) {
  fs.writeFileSync(path.join(artifactDir, name), JSON.stringify(payload, null, 2), 'utf8')
}

async function parseResponseBody(response) {
  return response.json().catch(async () => ({ raw: await response.text().catch(() => '') }))
}

async function settle(page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 60000 })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.click()
      await item.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
      await item.fill(String(value))
      return item
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function fillInputByLabel(dialog, label, value) {
  const formItem = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await formItem.waitFor({ state: 'visible', timeout: 30000 })
  return fillFirstVisible(formItem.locator('input:not([readonly]), textarea'), value, label)
}

async function fillDateByPlaceholder(root, placeholder, value) {
  const input = await fillFirstVisible(
    root.locator(`input[placeholder="${placeholder}"]`),
    value,
    placeholder
  )
  await input.press('Enter')
}

async function applyAdmissionWorkOrderFilter(page) {
  const filter = page
    .locator('.table-multi-filter[data-table-key="mes.pro.scheduleOrder.admissionDiff"]')
    .first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  if ((await filter.locator('.table-multi-filter__condition-row').count()) === 0) {
    await filter.getByRole('button', { name: '新增筛选条件' }).click()
  }
  let field = filter
    .locator('.table-multi-filter-field[data-filter-key="workOrderCode"]')
    .first()
  if ((await field.count()) === 0 || !(await field.isVisible().catch(() => false))) {
    await filter.locator('.table-multi-filter__field-select').first().click()
    await page.getByRole('option', { name: '工单编码', exact: true }).last().click()
    field = filter
      .locator('.table-multi-filter-field[data-filter-key="workOrderCode"]')
      .first()
  }
  await field.waitFor({ state: 'visible', timeout: 30000 })
  const valueInput = field.locator('.table-multi-filter-field__value input').first()
  await fillFirstVisible(valueInput, config.workOrderCode, 'admission work order code')
  assert.equal(
    await valueInput.inputValue(),
    config.workOrderCode,
    '同步工单筛选值必须写入当前页面输入框'
  )
  await filter
    .locator('.table-multi-filter__tabs .el-tabs__item')
    .filter({ hasText: config.workOrderCode })
    .first()
    .waitFor({ state: 'visible', timeout: 5000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/admission-diff') &&
      response.url().includes(config.workOrderCode) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await filter.getByRole('button', { name: /查询/ }).click()
  return responsePromise
}

async function login(page) {
  const redirect = '/erp/kingdee-sync'
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
  if (!page.url().includes('/login')) return

  if (
    (await page
      .locator(
        '.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible'
      )
      .count()) > 0
  ) {
    throw new Error('登录页验证码已开启，无法无人工输入创建任务专用基线。')
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
    await page.keyboard.type(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    if ((await tenantOption.count()) > 0) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
      await tenantInput.press('Tab')
    }
  } else {
    await fillFirstVisible(
      form.locator('input[placeholder="请输入租户名称"], input.el-input__inner').first(),
      config.tenant,
      'tenant'
    )
  }
  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(
    form.locator('input[placeholder="请输入密码"], input[type="password"]'),
    config.password,
    'password'
  )

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/login') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: /登录/ }).click()
  ])
  const loginBody = await parseResponseBody(loginResponse)
  assert.ok(loginResponse.ok(), `login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginBody.code), `login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, {
    timeout: 60000
  })
  await page.waitForFunction(() => !window.location.href.includes('/login'), null, {
    timeout: 60000
  })
}

async function apiGet(page, apiPath, params = {}) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  }
  const requestPath = `/admin-api${apiPath}${search.size ? `?${search.toString()}` : ''}`
  const result = await page.evaluate(async (pathValue) => {
    const unwrap = (value) => {
      if (!value || typeof value !== 'object') return value
      for (const field of ['accessToken', 'v', 'value', 'data']) {
        if (Object.prototype.hasOwnProperty.call(value, field)) return unwrap(value[field])
      }
      return value
    }
    const readCache = (key) => {
      for (const storage of [localStorage, sessionStorage]) {
        let raw = storage.getItem(key)
        if (!raw) {
          const matched = Object.keys(storage).find((item) => item === key || item.endsWith(key))
          raw = matched ? storage.getItem(matched) : null
        }
        if (!raw) continue
        try {
          const parsed = JSON.parse(raw)
          const unwrapped = unwrap(parsed)
          if (
            typeof unwrapped === 'string' &&
            unwrapped.startsWith('"') &&
            unwrapped.endsWith('"')
          ) {
            return JSON.parse(unwrapped)
          }
          return unwrapped
        } catch (error) {
          return raw.replace(/^"|"$/g, '')
        }
      }
      return undefined
    }
    const headers = { 'Cache-Control': 'no-cache', Pragma: 'no-cache' }
    const accessToken = readCache('ACCESS_TOKEN')
    const tenantId = readCache('tenantId')
    const visitTenantId = readCache('visitTenantId')
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`
    if (tenantId) headers['tenant-id'] = String(tenantId)
    if (visitTenantId && accessToken) headers['visit-tenant-id'] = String(visitTenantId)
    const response = await fetch(pathValue, { method: 'GET', credentials: 'omit', headers })
    return { status: response.status, body: await response.json() }
  }, requestPath)
  assert.equal(result.status, 200, `只读接口 HTTP 异常: ${apiPath}`)
  assert.equal(result.body.code, 0, `只读接口业务错误 ${apiPath}: ${result.body.msg || result.body.code}`)
  return result.body.data
}

async function preflight(page) {
  const routePage = await apiGet(page, '/mes/pro/route/page', {
    pageNo: 1,
    pageSize: 50,
    code: config.routeCode,
    status: 0
  })
  const routes = (routePage.list || []).filter((route) => route.code === config.routeCode)
  assert.equal(routes.length, 1, `正式路线编码必须唯一存在: ${config.routeCode}`)
  const route = routes[0]
  assert.ok(route.id && route.activeRouteVersionId, '目标路线必须有当前正式版本')

  const processes = await apiGet(page, '/mes/pro/route-process/list-by-route', {
    routeId: route.id
  })
  const targetProcesses = (processes || []).filter((process) =>
    String(process.processName || '').includes(config.processName)
  )
  assert.equal(targetProcesses.length, 1, `正式路线必须且只能包含一道“${config.processName}”工序`)
  const targetProcess = targetProcesses[0]
  assert.ok(targetProcess.workstationId, '目标工序必须绑定正式工作站')
  assert.ok(Number(targetProcess.processHourlyCapacityTotal) > 0, '目标工序必须有正式小时产能')

  const scheduleConfigs = await apiGet(
    page,
    '/mes/pro/route-schedule-config/list-by-route-version',
    { routeVersionId: route.activeRouteVersionId }
  )
  const targetConfigs = (scheduleConfigs || []).filter(
    (item) => Number(item.routeProcessId) === Number(targetProcess.id)
  )
  assert.equal(targetConfigs.length, 1, '目标路线工序必须有唯一正式排产配置')
  assert.equal(Boolean(targetConfigs[0].nightShiftEnabled), false, '目标路线工序必须原本未开启夜班')

  const nightStatus = await apiGet(
    page,
    '/mes/pro/scheduler-workbench/night-shift-capacity/status'
  )
  assert.equal(Boolean(nightStatus.available), false, '负向样本要求当前没有正式可用夜班产能')
  assert.equal(Number(nightStatus.availableShiftCount), 0, '负向样本要求可用夜班班次数为 0')

  const products = await apiGet(page, '/mes/pro/route-product/list-by-route', {
    routeId: route.id
  })
  const product = (products || []).find(
    (item) => item.itemId && item.itemCode && item.unitName
  )
  assert.ok(product, '目标路线必须绑定带正式单位的产品')

  const unitPage = await apiGet(page, '/mes/md/unit-measure/page', {
    pageNo: 1,
    pageSize: 200,
    status: 0
  })
  const units = (unitPage.list || []).filter(
    (unit) => unit.name === product.unitName || unit.code === product.unitName
  )
  assert.equal(units.length, 1, `产品单位 ${product.unitName} 必须唯一匹配正式单位编码`)

  const admission = await apiGet(page, '/mes/pro/schedule-order/admission-diff', {
    pageNo: 1,
    pageSize: 20,
    workOrderCode: config.workOrderCode
  })
  const existingAdmissionRows = (admission.list || []).filter(
    (row) => row.workOrderCode === config.workOrderCode
  )
  if (config.resumeWorkOrderCode) {
    assert.equal(existingAdmissionRows.length, 1, '恢复模式要求任务工单唯一存在于待同步差异列表')
    assert.equal(existingAdmissionRows[0].admissionStatus, 'READY_TO_ADMIT', '恢复任务工单必须待入池')
    assert.equal(Boolean(existingAdmissionRows[0].selectable), true, '恢复任务工单必须可选入池')
  } else {
    assert.equal(existingAdmissionRows.length, 0, '任务工单编码创建前必须不存在')
  }

  const evidence = {
    workOrderCode: config.workOrderCode,
    route: {
      id: route.id,
      code: route.code,
      name: route.name,
      activeRouteVersionId: route.activeRouteVersionId
    },
    targetProcess: {
      routeProcessId: targetProcess.id,
      processId: targetProcess.processId,
      processCode: targetProcess.processCode,
      processName: targetProcess.processName,
      workstationId: targetProcess.workstationId,
      workstationCode: targetProcess.workstationCode,
      capacitySource: targetProcess.capacitySource,
      processHourlyCapacityTotal: targetProcess.processHourlyCapacityTotal
    },
    targetScheduleConfig: targetConfigs[0],
    nightStatus,
    product: {
      itemId: product.itemId,
      itemCode: product.itemCode,
      itemName: product.itemName,
      unitName: product.unitName,
      unitCode: units[0].code
    },
    resumeMode: Boolean(config.resumeWorkOrderCode)
  }
  writeJson('preflight.json', evidence)
  return { route, targetProcess, product, unitCode: units[0].code, nightStatus }
}

async function createErpProductionOrder(page, context) {
  const batchNumber = `${config.workOrderCode}-BATCH`
  await page.goto(`${config.baseUrl}/erp/kingdee-sync`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.getByRole('button', { name: /新增ERP工单/ }).click()
  const dialog = page
    .locator('.el-dialog:visible, .el-overlay-dialog:visible')
    .filter({ hasText: '新增ERP工单' })
    .last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillInputByLabel(dialog, 'ERP工单号', config.workOrderCode)
  await fillInputByLabel(dialog, '物料编码', context.product.itemCode)
  await fillInputByLabel(dialog, '单位编码', context.unitCode)
  await fillInputByLabel(dialog, '来源单号', `${config.workOrderCode}-SO`)
  await fillInputByLabel(dialog, '批次号', batchNumber)
  await fillInputByLabel(dialog, '生产数量', String(config.quantity))
  await fillDateByPlaceholder(dialog, '请选择计划开始时间', nextWorkdayDateTime(0, 8))
  await fillDateByPlaceholder(dialog, '请选择计划完成时间', nextWorkdayDateTime(1, 17))
  await dialog.screenshot({ path: path.join(artifactDir, 'erp-order-before-create.png') })

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/erp/kingdee-sync/production-order/create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: /创建并提交ERP工单/ }).click()
  const response = await responsePromise
  const body = await parseResponseBody(response)
  writeJson('erp-order-create-response.json', { status: response.status(), body })
  assert.ok(response.ok(), `ERP 工单创建 HTTP ${response.status()}`)
  assert.equal(body.code, 0, `ERP 工单创建失败: ${body.msg || body.code}`)
  assert.equal(String(body.data?.erpBillNo), config.workOrderCode, 'ERP 工单号必须匹配任务标识')
  assert.equal(Boolean(body.data?.submitted), true, 'ERP 工单必须已提交')
  return { batchNumber, erp: body.data }
}

async function triggerProductionOrderSync(page) {
  await page.goto(`${config.baseUrl}/erp/kingdee-sync`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  const row = page
    .locator('.el-table__row')
    .filter({ hasText: '生产工单' })
    .filter({ hasText: 'kingdeeProductionOrderSyncJob' })
    .first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/infra/job/trigger') &&
      ['PUT', 'POST'].includes(response.request().method()),
    { timeout: 60000 }
  )
  await row.getByRole('button', { name: /增量同步|执行一次/ }).click()
  const response = await responsePromise
  const body = await parseResponseBody(response)
  writeJson('production-order-sync-response.json', { status: response.status(), body })
  assert.ok(response.ok(), `生产工单同步触发 HTTP ${response.status()}`)
  assert.equal(body.code, 0, `生产工单同步触发失败: ${body.msg || body.code}`)
}

async function waitForMesWorkOrder(page) {
  const deadline = Date.now() + config.syncWaitMs
  const attempts = []
  while (Date.now() <= deadline) {
    const data = await apiGet(page, '/mes/pro/work-order/page', {
      pageNo: 1,
      pageSize: 20,
      code: config.workOrderCode
    })
    const workOrder = (data.list || []).find((row) => row.code === config.workOrderCode)
    attempts.push({ at: new Date().toISOString(), total: data.total, found: Boolean(workOrder) })
    if (workOrder) {
      writeJson('synced-mes-work-order.json', { workOrder, attempts })
      return workOrder
    }
    await page.waitForTimeout(5000)
  }
  writeJson('mes-work-order-sync-timeout.json', { attempts })
  throw new Error(`ERP 工单 ${config.workOrderCode} 未在限定时间内同步为 MES 生产工单`)
}

async function loadResumableMesWorkOrder(page) {
  assert.ok(config.resumeWorkOrderCode, '恢复模式必须显式提供任务生产工单编码')
  const data = await apiGet(page, '/mes/pro/work-order/page', {
    pageNo: 1,
    pageSize: 20,
    code: config.resumeWorkOrderCode
  })
  const workOrders = (data.list || []).filter(
    (row) => row.code === config.resumeWorkOrderCode
  )
  assert.equal(workOrders.length, 1, '恢复模式必须唯一命中已同步 MES 生产工单')
  assert.equal(Number(workOrders[0].quantityScheduled || 0), 0, '恢复任务工单不得已经加入排产池')
  writeJson('resumed-mes-work-order.json', { workOrder: workOrders[0] })
  return workOrders[0]
}

async function admitWorkOrder(page, workOrder) {
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  const pool = page.locator('.schedule-order-pool').first()
  await pool.waitFor({ state: 'visible', timeout: 30000 })
  const initialResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/admission-diff') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.getByRole('tab', { name: '同步工单', exact: true }).click()
  await initialResponsePromise
  const searchResponse = await applyAdmissionWorkOrderFilter(page)
  const searchBody = await parseResponseBody(searchResponse)
  assert.equal(searchBody.code, 0, `待同步差异查询失败: ${searchBody.msg || searchBody.code}`)
  const candidates = (searchBody.data?.list || []).filter(
    (row) => row.workOrderCode === config.workOrderCode
  )
  assert.equal(candidates.length, 1, '任务生产工单必须唯一出现在待同步差异列表')
  assert.equal(candidates[0].admissionStatus, 'READY_TO_ADMIT', '任务生产工单必须处于待入池状态')
  assert.equal(Boolean(candidates[0].selectable), true, '任务生产工单必须可选入池')

  const admissionTable = page
    .locator('[data-user-table-key="mes.pro.scheduleOrder.admissionDiff"]')
    .first()
  const row = admissionTable
    .locator('.el-table__row')
    .filter({ hasText: config.workOrderCode })
    .first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.locator('.el-checkbox').first().click()
  await page.screenshot({ path: path.join(artifactDir, 'admission-before-create.png'), fullPage: true })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/create-from-work-order') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: /选中工单加入排产工单池/ }).click()
  const response = await responsePromise
  const body = await parseResponseBody(response)
  writeJson('schedule-order-create-response.json', { status: response.status(), body })
  assert.ok(response.ok(), `排产工单创建 HTTP ${response.status()}`)
  assert.equal(body.code, 0, `排产工单创建失败: ${body.msg || body.code}`)

  const pageData = await apiGet(page, '/mes/pro/schedule-order/page', {
    pageNo: 1,
    pageSize: 20,
    erpWorkOrderCode: config.workOrderCode,
    completionFilter: 'ALL'
  })
  const scheduleOrders = (pageData.list || []).filter(
    (item) => Number(item.workOrderId) === Number(workOrder.id)
  )
  assert.equal(scheduleOrders.length, 1, '任务生产工单必须生成唯一排产工单')
  return scheduleOrders[0]
}

async function verifyCleanWorkbenchRow(page, context, scheduleOrder) {
  const rows = await apiGet(page, '/mes/pro/schedule-order/process-wip-statistics')
  const candidates = (rows || []).filter(
    (row) =>
      row.routeCode === context.route.code &&
      row.processCode === context.targetProcess.processCode &&
      String(row.processName || '').includes(config.processName) &&
      Array.isArray(row.scheduleOrderIds) &&
      row.scheduleOrderIds.length === 1 &&
      Number(row.scheduleOrderIds[0]) === Number(scheduleOrder.id)
  )
  assert.equal(candidates.length, 1, '任务排产工单必须在工作台形成唯一、未混合的目标工序行')
  assert.equal(Boolean(candidates[0].nightShiftMixed), false, '任务目标行不得混合多个夜班设置')
  assert.equal(Boolean(candidates[0].nightShiftEnabled), false, '任务目标行夜班必须原本关闭')

  await page.goto(`${config.baseUrl}/mes/pro/scheduler-workbench?cleanBaseline=${Date.now()}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  const table = page.locator('.scheduler-workbench__process-wip-table').first()
  await table.waitFor({ state: 'visible', timeout: 60000 })
  const targetRow = table
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: context.route.code })
    .filter({ hasText: context.targetProcess.processCode })
    .first()
  await targetRow.waitFor({ state: 'visible', timeout: 60000 })
  await targetRow.scrollIntoViewIfNeeded()
  await targetRow.screenshot({ path: path.join(artifactDir, 'clean-workbench-target-row.png') })
  return candidates[0]
}

async function main() {
  ensureArtifactDir()
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    const preflightContext = await preflight(page)
    let erpResult
    let workOrder
    if (config.resumeWorkOrderCode) {
      workOrder = await loadResumableMesWorkOrder(page)
    } else {
      erpResult = await createErpProductionOrder(page, preflightContext)
      await triggerProductionOrderSync(page)
      workOrder = await waitForMesWorkOrder(page)
    }
    const scheduleOrder = await admitWorkOrder(page, workOrder)
    const workbenchRow = await verifyCleanWorkbenchRow(page, preflightContext, scheduleOrder)
    const result = {
      result: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      workOrderCode: config.workOrderCode,
      erpBillId: erpResult?.erp?.id,
      resumedFromExistingWorkOrder: Boolean(config.resumeWorkOrderCode),
      workOrderId: workOrder.id,
      scheduleOrderId: scheduleOrder.id,
      routeId: preflightContext.route.id,
      routeCode: preflightContext.route.code,
      routeVersionId: preflightContext.route.activeRouteVersionId,
      routeProcessId: preflightContext.targetProcess.id,
      processId: preflightContext.targetProcess.processId,
      processCode: preflightContext.targetProcess.processCode,
      processName: preflightContext.targetProcess.processName,
      scheduleOrderIds: workbenchRow.scheduleOrderIds,
      nightShiftEnabled: Boolean(workbenchRow.nightShiftEnabled),
      nightShiftAvailable: Boolean(preflightContext.nightStatus.available),
      artifacts: [
        'preflight.json',
        'erp-order-before-create.png',
        'erp-order-create-response.json',
        'production-order-sync-response.json',
        'synced-mes-work-order.json',
        'admission-before-create.png',
        'schedule-order-create-response.json',
        'clean-workbench-target-row.png'
      ]
    }
    writeJson('baseline-result.json', result)
    console.log(JSON.stringify(result, null, 2))
  } catch (error) {
    fs.writeFileSync(path.join(artifactDir, 'e2e-error.txt'), `${error.stack || error.message}\n`, 'utf8')
    console.error(`BLOCKER: scheduler night-shift clean baseline -> ${error.stack || error.message}`)
    process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main()
