const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_PASSWORD || '111111',
  headed: process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_HEADED === '1',
  factor: Number(process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_FACTOR || '3'),
  routeCode: process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_ROUTE_CODE || '',
  workOrderCode: process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_WORK_ORDER_CODE || '',
  erpUnitNumber: process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_ERP_UNIT_NUMBER || '',
  createWhenMissing: process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_CREATE_WHEN_MISSING !== '0',
  erpSyncWaitMs: Number(process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_SYNC_WAIT_MS || '180000'),
  erpSyncPollMs: Number(process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_SYNC_POLL_MS || '5000'),
  artifactDir:
    process.env.MES_SCHEDULE_ROUTE_FACTOR_E2E_ARTIFACT_DIR ||
    path.resolve(process.cwd(), 'tests/output/mes-schedule-route-production-factor-real')
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `MES schedule route production factor E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`
  )
}

if (!Number.isFinite(config.factor) || config.factor <= 0) {
  throw new Error(`生产系数必须大于 0，当前为 ${config.factor}`)
}

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeJsonArtifact(name, payload) {
  ensureArtifactDir()
  fs.writeFileSync(path.join(config.artifactDir, name), JSON.stringify(payload, null, 2), 'utf8')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(300)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(String(value))
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function selectLoginTenant(page, loginForm) {
  const tenantSelectInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantSelectInput.count()) > 0 && (await tenantSelectInput.isVisible())) {
    await tenantSelectInput.click()
    await tenantSelectInput.fill(config.tenant)
    await tenantSelectInput.press('Enter')
    return
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
}

async function login(page, redirectPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  if (
    (await loginForm
      .locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]')
      .count()) > 0
  ) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  await selectLoginTenant(page, loginForm)
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
    { timeout: 30000 }
  )
  await loginForm.locator('.el-button--primary').first().click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.equal(payload.code, 0, `login failed: ${payload.msg || JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function apiRequest(page, method, apiPath, body) {
  const result = await page.evaluate(
    async ({ method: requestMethod, apiPath: requestPath, body: requestBody }) => {
      const unwrapCacheValue = (value) => {
        if (!value || typeof value !== 'object') return value
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
          if (raw.startsWith('"') && raw.endsWith('"')) return raw.slice(1, -1)
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
      if (accessToken) headers.Authorization = `Bearer ${accessToken}`
      if (tenantId) headers['tenant-id'] = String(tenantId)
      if (visitTenantId && accessToken) headers['visit-tenant-id'] = String(visitTenantId)
      if (requestBody !== undefined) headers['Content-Type'] = 'application/json'

      const response = await fetch(`/admin-api${requestPath}`, {
        method: requestMethod,
        credentials: 'omit',
        headers,
        body: requestBody === undefined ? undefined : JSON.stringify(requestBody)
      })
      return {
        status: response.status,
        body: await response.json()
      }
    },
    { method, apiPath, body }
  )
  assert.equal(result.status, 200, `接口 HTTP 状态异常: ${apiPath}`)
  assert.equal(result.body.code, 0, `接口业务错误 ${apiPath}: ${result.body.msg || JSON.stringify(result.body)}`)
  return result.body.data
}

function toQueryString(params) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  }
  return search.toString()
}

async function apiGet(page, apiPath, params = {}) {
  const query = toQueryString(params)
  return apiRequest(page, 'GET', `${apiPath}${query ? `?${query}` : ''}`)
}

async function apiPost(page, apiPath, body) {
  return apiRequest(page, 'POST', apiPath, body)
}

function normalizeNumber(value) {
  const numeric = Number(value)
  assert.ok(Number.isFinite(numeric), `数值字段不是有效数字: ${value}`)
  return numeric
}

function nowStamp() {
  return new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
}

function localDateTimeAfterHours(hours) {
  const date = new Date()
  date.setHours(date.getHours() + hours, 0, 0, 0)
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

async function clickButton(root, name, label) {
  const button = root.getByRole('button', { name }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await button.click()
  return button
}

async function fillInputByLabel(dialog, label, value) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(item.locator('input:not([readonly]), textarea').first(), value, label)
}

async function fillDateByPlaceholder(root, placeholder, value) {
  const input = root.locator(`input[placeholder="${placeholder}"]`).first()
  await fillFirstVisible(input, value, placeholder)
  await input.press('Enter')
}

async function findReadyAdmissionWorkOrder(page) {
  if (config.workOrderCode) {
    const data = await apiGet(page, '/mes/pro/schedule-order/admission-diff', {
      pageNo: 1,
      pageSize: 20,
      workOrderCode: config.workOrderCode,
      admissionStatus: 'READY_TO_ADMIT'
    })
    const target = (data.list || []).find((row) => row.workOrderCode === config.workOrderCode)
    assert.ok(target, `测试租户缺少 READY_TO_ADMIT 真实生产工单 ${config.workOrderCode}`)
    assert.equal(target.admissionStatus, 'READY_TO_ADMIT', `工单不是待入池状态: ${JSON.stringify(target)}`)
    assert.equal(Boolean(target.selectable), true, `工单不可勾选入池: ${JSON.stringify(target)}`)
    return target
  }

  const data = await apiGet(page, '/mes/pro/schedule-order/admission-diff', {
    pageNo: 1,
    pageSize: 50,
    admissionStatus: 'READY_TO_ADMIT'
  })
  const target = (data.list || []).find(
    (row) => row.admissionStatus === 'READY_TO_ADMIT' && row.selectable && normalizeNumber(row.quantity) > 0
  )
  return target || null
}

async function findRouteWithScheduleProduct(page) {
  const routePage = await apiGet(page, '/mes/pro/route/page', {
    pageNo: 1,
    pageSize: 50,
    code: config.routeCode || undefined,
    status: 0
  })
  const routes = routePage.list || []
  for (const route of routes) {
    if (!route.id || !route.activeRouteVersionId) continue
    const products = await apiGet(page, '/mes/pro/route-product/list-by-route', { routeId: route.id })
    const scheduleConfigs = await apiGet(page, '/mes/pro/route-schedule-config/list-by-route-version', {
      routeVersionId: route.activeRouteVersionId
    })
    const scheduleUseConfigs = await apiGet(page, '/mes/pro/route/flow-config/process-config-list', {
      routeId: route.id,
      useType: 'SCHEDULE'
    })
    const enabledUseConfigs = (scheduleUseConfigs || []).filter((item) => item.enabled !== false)
    const configuredRouteProcessIds = new Set(
      (scheduleConfigs || [])
        .map((item) => Number(item.routeProcessId))
        .filter((routeProcessId) => Number.isFinite(routeProcessId) && routeProcessId > 0)
    )
    const hasRouteLevelScheduleConfig = enabledUseConfigs.some((item) =>
      configuredRouteProcessIds.has(Number(item.routeProcessId))
    )
    if (!hasRouteLevelScheduleConfig) continue
    for (const product of products || []) {
      if (!product.itemCode || !product.itemId) continue
      if (enabledUseConfigs.length > 0) {
        return {
          route,
          product,
          scheduleConfigs,
          scheduleUseConfigs
        }
      }
    }
  }
  throw new Error('测试租户缺少已启用、已绑定产品、已维护路线级排产配置的工艺流程排产配置，无法创建真实待入池工单。')
}

async function resolveErpUnitNumber(page, routeProduct) {
  if (config.erpUnitNumber) {
    return config.erpUnitNumber
  }
  if (!routeProduct.unitName) {
    throw new Error(`路线产品缺少单位名称，无法确定 ERP 单位编码: ${JSON.stringify(routeProduct)}`)
  }
  const unitPage = await apiGet(page, '/mes/md/unit-measure/page', {
    pageNo: 1,
    pageSize: 200,
    status: 0
  })
  const matchedUnits = (unitPage.list || []).filter(
    (unit) => unit.name === routeProduct.unitName || unit.code === routeProduct.unitName
  )
  assert.equal(
    matchedUnits.length,
    1,
    `路线产品单位 ${routeProduct.unitName} 必须唯一匹配一个 MES 单位编码: ${JSON.stringify(matchedUnits)}`
  )
  return matchedUnits[0].code
}

async function createErpProductionOrderViaUi(page, routeProduct) {
  const billNo = config.workOrderCode || `CODEX-FACTOR-${nowStamp()}`
  const batchNumber = `${billNo}-BATCH`
  const erpUnitNumber = await resolveErpUnitNumber(page, routeProduct)
  await page.goto(`${config.baseUrl}/erp/kingdee-sync`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await clickButton(page, /新增ERP工单/, '新增ERP工单')
  const dialog = page.locator('.el-dialog:visible, .el-overlay-dialog:visible').filter({ hasText: '新增ERP工单' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillInputByLabel(dialog, 'ERP工单号', billNo)
  await fillInputByLabel(dialog, '物料编码', routeProduct.itemCode)
  await fillInputByLabel(dialog, '单位编码', erpUnitNumber)
  await fillInputByLabel(dialog, '来源单号', `${billNo}-SO`)
  await fillInputByLabel(dialog, '批次号', batchNumber)
  await fillFirstVisible(dialog.locator('.el-form-item').filter({ hasText: '生产数量' }).locator('input').first(), '100', '生产数量')
  await fillDateByPlaceholder(dialog, '请选择计划开始时间', localDateTimeAfterHours(12))
  await fillDateByPlaceholder(dialog, '请选择计划完成时间', localDateTimeAfterHours(36))

  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/erp/kingdee-sync/production-order/create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: /创建并提交ERP工单/ }).click()
  const createResponse = await createResponsePromise
  const payload = await createResponse.json()
  assert.equal(createResponse.status(), 200, 'ERP 工单创建 HTTP 状态必须为 200')
  assert.equal(payload.code, 0, `ERP 工单创建失败: ${payload.msg || JSON.stringify(payload)}`)
  assert.equal(String(payload.data.erpBillNo), billNo, `ERP 工单号必须匹配: ${JSON.stringify(payload.data)}`)
  assert.equal(Boolean(payload.data.submitted), true, `ERP 工单必须已提交: ${JSON.stringify(payload.data)}`)
  writeJsonArtifact('created-erp-production-order.json', {
    routeProduct,
    erpUnitNumber,
    billNo,
    batchNumber,
    erpProductionOrder: payload.data
  })
  return {
    workOrderCode: billNo,
    batchNumber,
    erpUnitNumber,
    erpProductionOrder: payload.data
  }
}

async function triggerProductionOrderSyncViaUi(page) {
  await page.goto(`${config.baseUrl}/erp/kingdee-sync`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const row = page
    .locator('.el-table__row')
    .filter({ hasText: '生产工单' })
    .filter({ hasText: 'kingdeeProductionOrderSyncJob' })
    .first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const triggerResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/infra/job/trigger') &&
      ['PUT', 'POST'].includes(response.request().method()),
    { timeout: 30000 }
  )
  await row.getByRole('button', { name: /增量同步|执行一次/ }).click()
  const triggerResponse = await triggerResponsePromise
  const payload = await triggerResponse.json()
  assert.equal(triggerResponse.status(), 200, '生产工单同步任务触发 HTTP 状态必须为 200')
  assert.equal(payload.code, 0, `生产工单同步任务触发失败: ${payload.msg || JSON.stringify(payload)}`)
  writeJsonArtifact('production-order-sync-trigger.json', payload.data || payload)
}

async function waitForMesWorkOrder(page, workOrderCode) {
  const deadline = Date.now() + config.erpSyncWaitMs
  const attempts = []
  while (Date.now() <= deadline) {
    const pageData = await apiGet(page, '/mes/pro/work-order/page', {
      pageNo: 1,
      pageSize: 20,
      code: workOrderCode
    })
    const workOrder = (pageData.list || []).find((row) => String(row.code) === String(workOrderCode))
    attempts.push({
      at: new Date().toISOString(),
      total: pageData.total,
      found: Boolean(workOrder),
      status: workOrder?.status,
      quantity: workOrder?.quantity
    })
    if (workOrder) {
      writeJsonArtifact('synced-mes-work-order.json', { workOrderCode, workOrder, attempts })
      return workOrder
    }
    await page.waitForTimeout(config.erpSyncPollMs)
  }
  writeJsonArtifact('mes-work-order-sync-timeout.json', { workOrderCode, attempts })
  throw new Error(`ERP 工单 ${workOrderCode} 未在 ${config.erpSyncWaitMs}ms 内同步生成 MES 生产工单。`)
}

async function ensureReadyAdmissionWorkOrder(page) {
  const existing = await findReadyAdmissionWorkOrder(page)
  if (existing) {
    return {
      workOrder: existing,
      route: await resolveRouteForWorkOrder(page, existing),
      createdByE2E: false
    }
  }
  if (!config.createWhenMissing) {
    throw new Error('测试租户缺少可写入的 READY_TO_ADMIT 真实生产工单，且已禁用自动创建前置数据。')
  }
  const routeContext = await findRouteWithScheduleProduct(page)
  const created = await createErpProductionOrderViaUi(page, routeContext.product)
  await triggerProductionOrderSyncViaUi(page)
  await waitForMesWorkOrder(page, created.workOrderCode)
  const data = await apiGet(page, '/mes/pro/schedule-order/admission-diff', {
    pageNo: 1,
    pageSize: 20,
    workOrderCode: created.workOrderCode,
    admissionStatus: 'READY_TO_ADMIT'
  })
  const ready = (data.list || []).find((row) => row.workOrderCode === created.workOrderCode)
  assert.ok(ready, `新建工单未出现在 READY_TO_ADMIT 差异列表: ${created.workOrderCode}`)
  assert.equal(Boolean(ready.selectable), true, `新建工单不可入池: ${JSON.stringify(ready)}`)
  return {
    workOrder: ready,
    route: routeContext.route,
    createdByE2E: true,
    routeProduct: routeContext.product,
    erpProductionOrder: created.erpProductionOrder
  }
}

async function resolveRouteForWorkOrder(page, workOrder) {
  const routePage = await apiGet(page, '/mes/pro/route/page', {
    pageNo: 1,
    pageSize: 50,
    code: config.routeCode || undefined,
    status: 0
  })
  const routes = routePage.list || []
  const candidates = []
  for (const route of routes) {
    if (config.routeCode && route.code !== config.routeCode) continue
    const productCodes = String(route.productCodes || '')
    if (productCodes.split(/[,\s，、]+/).filter(Boolean).includes(String(workOrder.productCode))) {
      candidates.push(route)
    }
  }
  assert.ok(
    candidates.length > 0,
    `未找到产品 ${workOrder.productCode} 对应的启用工艺流程排产配置，无法配置生产系数。可通过 MES_SCHEDULE_ROUTE_FACTOR_E2E_ROUTE_CODE 指定路线。`
  )
  const route = candidates[0]
  assert.ok(route.id, `候选路线缺少 id: ${JSON.stringify(route)}`)
  assert.ok(route.activeRouteVersionId, `候选路线缺少当前激活路线版本: ${JSON.stringify(route)}`)
  return route
}

async function openScheduleRouteConfig(page, route) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/route?tab=schedule-config`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await pageResponsePromise.catch(() => {})
  await page.locator('.route-flow-config-panel-page').waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText('工艺流程排产配置', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })

  await fillFirstVisible(page.locator('input[placeholder="请输入工艺路线编码"]'), route.code, 'route code')
  const searchResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: '搜索' }).first().click()
  const searchResponse = await searchResponsePromise
  const searchPayload = await searchResponse.json()
  assert.equal(searchPayload.code, 0, `工艺流程排产配置查询失败: ${searchPayload.msg || JSON.stringify(searchPayload)}`)
  assert.ok(
    (searchPayload.data?.list || []).some((item) => String(item.id) === String(route.id)),
    `页面未查询到目标路线 ${route.code}`
  )

  const row = page
    .locator('.route-flow-config-panel-table .el-table__body-wrapper .el-table__row')
    .filter({ hasText: route.code })
    .first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const openResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/process-config-list') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await row.getByRole('button', { name: route.code, exact: true }).click()
  const openResponse = await openResponsePromise
  const openPayload = await openResponse.json()
  assert.equal(openPayload.code, 0, `生产系数配置加载失败: ${openPayload.msg || JSON.stringify(openPayload)}`)
  assert.ok(Array.isArray(openPayload.data) && openPayload.data.length > 0, `路线 ${route.code} 缺少工序配置`)

  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('.route/flow-config-table .el-table__body-wrapper .el-table__row').first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  await dialog.getByText('生产系数', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  return { dialog, configs: openPayload.data }
}

async function setFirstProcessFactorViaUi(page, dialog, routeConfigs) {
  const enabledConfig =
    routeConfigs.find((item) => item.enabled !== false && Number(item.productionQuantityFactor || 1) !== config.factor) ||
    routeConfigs.find((item) => item.enabled !== false) ||
    routeConfigs[0]
  assert.ok(enabledConfig?.routeProcessId, `缺少可配置的路线工序: ${JSON.stringify(enabledConfig)}`)

  const rows = dialog.locator('.route/flow-config-table .el-table__body-wrapper .el-table__row')
  const count = await rows.count()
  let targetRow
  for (let index = 0; index < count; index += 1) {
    const row = rows.nth(index)
    const codeText = (await row.locator('td').nth(1).innerText()).trim()
    const nameText = (await row.locator('td').nth(2).innerText()).trim()
    if (codeText === String(enabledConfig.processCode || '').trim() || nameText === String(enabledConfig.processName || '').trim()) {
      targetRow = row
      break
    }
  }
  assert.ok(targetRow, `页面未找到目标工序行: ${JSON.stringify(enabledConfig)}`)

  const input = targetRow.locator('td').filter({ hasText: '生产系数' }).locator('input').first()
  const factorInput = (await input.count()) > 0 ? input : targetRow.locator('.el-input-number input').first()
  await factorInput.waitFor({ state: 'visible', timeout: 30000 })
  const originalFactor = normalizeNumber(await factorInput.inputValue())
  await factorInput.fill(String(config.factor))
  await factorInput.press('Enter')

  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/save') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '保存用途配置' }).click()
  const saveResponse = await saveResponsePromise
  const savePayload = await saveResponse.json()
  assert.equal(saveResponse.status(), 200, '生产系数保存 HTTP 状态必须为 200')
  assert.equal(savePayload.code, 0, `生产系数保存失败: ${savePayload.msg || JSON.stringify(savePayload)}`)
  await page.getByText('用途配置保存成功').first().waitFor({ state: 'visible', timeout: 10000 })

  return {
    targetProcess: enabledConfig,
    originalFactor,
    targetFactor: config.factor
  }
}

async function restoreFactorByApi(page, route, targetProcess, originalFactor) {
  const currentConfigs = await apiGet(page, '/mes/pro/route/flow-config/process-config-list', {
    routeId: route.id,
    useType: 'SCHEDULE'
  })
  const processConfigs = currentConfigs.map((item) => ({
    routeProcessId: item.routeProcessId,
    processId: item.processId,
    processCode: item.processCode,
    processName: item.processName,
    sort: item.sort,
    enabled: item.enabled,
    useType: 'SCHEDULE',
    productionQuantityFactor:
      Number(item.routeProcessId) === Number(targetProcess.routeProcessId)
        ? originalFactor
        : item.productionQuantityFactor,
    remark: item.remark
  }))
  await apiPost(page, '/mes/pro/route/flow-config/save', {
    routeId: route.id,
    useType: 'SCHEDULE',
    processConfigs
  })
}

async function admitWorkOrderViaUi(page, workOrder) {
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 30000 })
  await page.getByRole('button', { name: /同步工单/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '待同步差异' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[placeholder="请输入工单编码"]'), workOrder.workOrderCode, 'admission work order code')
  await dialog.getByRole('button', { name: /搜索/ }).first().click()

  const row = dialog.locator('.el-table__row').filter({ hasText: workOrder.workOrderCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.locator('.el-checkbox').first().click()

  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/create-from-work-order') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: /选中工单加入排产工单池/ }).click()
  const createResponse = await createResponsePromise
  const createPayload = await createResponse.json()
  assert.equal(createResponse.status(), 200, '创建排产工单 HTTP 状态必须为 200')
  assert.equal(createPayload.code, 0, `创建排产工单失败: ${createPayload.msg || JSON.stringify(createPayload)}`)
  await settle(page)
  return createPayload.data
}

async function assertCreatedProcessQuantities(page, workOrder, targetProcess, factor) {
  const pageData = await apiGet(page, '/mes/pro/schedule-order/page', {
    pageNo: 1,
    pageSize: 20,
    erpWorkOrderCode: workOrder.workOrderCode,
    completionFilter: 'ALL'
  })
  const scheduleOrder = (pageData.list || []).find((row) => String(row.workOrderId) === String(workOrder.workOrderId))
  assert.ok(scheduleOrder, `未找到刚创建的排产工单: ${workOrder.workOrderCode}`)
  assert.equal(
    normalizeNumber(scheduleOrder.quantity).toFixed(6),
    normalizeNumber(workOrder.quantity).toFixed(6),
    '排产工单主数量必须等于生产工单数量'
  )

  const processes = await apiGet(page, '/mes/pro/schedule-order/process-list', {
    scheduleOrderId: scheduleOrder.id
  })
  assert.ok(Array.isArray(processes) && processes.length > 0, '排产工单必须生成工序快照')
  const target = processes.find((item) => Number(item.routeProcessId) === Number(targetProcess.routeProcessId))
  assert.ok(target, `工序快照缺少目标工序: ${JSON.stringify(targetProcess)}`)

  const expected = Number((normalizeNumber(workOrder.quantity) * factor).toFixed(6))
  assert.equal(normalizeNumber(target.plannedQuantity).toFixed(6), expected.toFixed(6), '目标工序 plannedQuantity 必须等于工单数量 × 生产系数')
  assert.equal(normalizeNumber(target.remainingQuantity).toFixed(6), expected.toFixed(6), '目标工序 remainingQuantity 必须等于工单数量 × 生产系数')
  assert.equal(normalizeNumber(target.productionQuantityFactor).toFixed(6), factor.toFixed(6), '工序快照必须固化生产系数')
  assert.ok(String(target.resourceSnapshotJson || '').includes('productionQuantityFactor'), '工序资源快照必须包含生产系数')

  return {
    scheduleOrder,
    targetProcessSnapshot: target,
    processCount: processes.length,
    expectedQuantity: expected,
    processes
  }
}

async function main() {
  const executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  const browser = await chromium.launch({ headless: !config.headed, executablePath })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  let restoreContext

  try {
    await login(page, '/mes/pro/route?tab=schedule-config')
    const readyContext = await ensureReadyAdmissionWorkOrder(page)
    const workOrder = readyContext.workOrder
    const route = readyContext.route
    const opened = await openScheduleRouteConfig(page, route)
    const factorChange = await setFirstProcessFactorViaUi(page, opened.dialog, opened.configs)
    restoreContext = { route, ...factorChange }
    await opened.dialog.getByRole('button', { name: '关闭' }).click()
    await opened.dialog.waitFor({ state: 'hidden', timeout: 30000 })

    await admitWorkOrderViaUi(page, workOrder)
    const assertion = await assertCreatedProcessQuantities(
      page,
      workOrder,
      factorChange.targetProcess,
      factorChange.targetFactor
    )

    await restoreFactorByApi(page, route, factorChange.targetProcess, factorChange.originalFactor)
    restoreContext = undefined

    writeJsonArtifact('result.json', {
      tenant: config.tenant,
      username: config.username,
      workOrder,
      route,
      readyContext,
      factorChange,
      assertion
    })

    console.log(
      `PASS: MES schedule route production factor real E2E. workOrder=${workOrder.workOrderCode}, route=${route.code}, process=${factorChange.targetProcess.processCode || factorChange.targetProcess.processName}, quantity=${workOrder.quantity}, factor=${factorChange.targetFactor}, expected=${assertion.expectedQuantity}, scheduleOrder=${assertion.scheduleOrder.code || assertion.scheduleOrder.id}`
    )
  } finally {
    if (restoreContext) {
      await restoreFactorByApi(
        page,
        restoreContext.route,
        restoreContext.targetProcess,
        restoreContext.originalFactor
      ).catch((error) => {
        console.error(`生产系数恢复失败，需要人工恢复 route=${restoreContext.route.code}: ${error.message}`)
      })
    }
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
