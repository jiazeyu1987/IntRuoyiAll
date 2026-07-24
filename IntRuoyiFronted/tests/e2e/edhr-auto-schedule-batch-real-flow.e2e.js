const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = (process.env.EDHR_AUTO_SCHEDULE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_AUTO_SCHEDULE_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const TEST_TENANT = process.env.EDHR_AUTO_SCHEDULE_E2E_TEST_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_AUTO_SCHEDULE_E2E_TEST_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_AUTO_SCHEDULE_E2E_TEST_PASSWORD || 'admin123'
const ADMIN_TENANT = process.env.EDHR_AUTO_SCHEDULE_E2E_ADMIN_TENANT || '芋道源码'
const ADMIN_USERNAME = process.env.EDHR_AUTO_SCHEDULE_E2E_ADMIN_USERNAME || 'admin'
const ADMIN_PASSWORD = process.env.EDHR_AUTO_SCHEDULE_E2E_ADMIN_PASSWORD || 'admin123'
const EXPLICIT_WORK_ORDER_CODE = process.env.EDHR_AUTO_SCHEDULE_E2E_WORK_ORDER_CODE || ''
let WORK_ORDER_CODE = EXPLICIT_WORK_ORDER_CODE || '881MO090863'
const MYSQL_CONTAINER = process.env.EDHR_AUTO_SCHEDULE_E2E_MYSQL_CONTAINER || 'int-ruoyi-mysql'
const MYSQL_USER = process.env.EDHR_AUTO_SCHEDULE_E2E_MYSQL_USER || 'root'
const MYSQL_PASSWORD = process.env.EDHR_AUTO_SCHEDULE_E2E_MYSQL_PASSWORD || '123456'
const MYSQL_DATABASE = process.env.EDHR_AUTO_SCHEDULE_E2E_MYSQL_DATABASE || 'ruoyi-vue-pro'
const ROUTES = {
  workbench: '/mes/pro/scheduler-workbench',
  scheduleOrder: '/mes/pro/schedule-order',
  task: '/mes/pro/task',
  batchExecution: '/mes/pro/feedback/edhr-batch-execution'
}
const REQUIRED_NODE_TYPES = [
  'INCOMING_INSPECTION_REPORT',
  'ROUTE_FORM',
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD'
]
const READONLY_WRITE_METHODS = new Set(['POST', 'PUT', 'DELETE', 'PATCH'])

function flattenRows(rows) {
  const result = []
  for (const row of rows || []) {
    result.push(row)
    result.push(...flattenRows(row.children || []))
  }
  return result
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'E2E must use the local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'E2E must use the local backend')
  assert.equal(TEST_TENANT, '测试租户', 'write E2E must use 测试租户')
  assert.equal(TEST_USERNAME, 'aoteman', 'write E2E must use the dedicated test account aoteman')
  assert.equal(ADMIN_TENANT, '芋道源码', 'readonly recheck must use 芋道源码/admin')
  assert.equal(ADMIN_USERNAME, 'admin', 'readonly recheck must use 芋道源码/admin')
  assert.notEqual(TEST_TENANT, ADMIN_TENANT, 'write tenant and readonly tenant must be different')
}

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
  throw new Error(`Missing visible input: ${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click()
      return
    }
  }
  throw new Error(`Missing enabled target: ${label}`)
}

async function forceClickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click({ force: true })
      return
    }
  }
  throw new Error(`Missing enabled target: ${label}`)
}

async function domClickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.evaluate((element) => element.click())
      return
    }
  }
  throw new Error(`Missing enabled target: ${label}`)
}

async function login(page, tenant, username, password, redirectPath) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('BLOCKED: 登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), password, 'password')
  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    clickFirstEnabled(loginForm.locator('.el-button--primary'), 'login button')
  ])
  const loginBody = await loginResponse.json()
  assert.equal(loginBody.code, 0, `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function readJsonResponse(page, response, label) {
  try {
    return await response.json()
  } catch (error) {
    if (!String(error?.message || '').includes('No resource with given identifier')) {
      throw error
    }
    const replay = await page.request.get(response.url())
    assert.equal(replay.status(), 200, `${label} 响应体读取失败后重取接口 HTTP 异常: ${replay.status()}`)
    return replay.json()
  }
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
  const unwrap = (raw) => {
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
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

async function apiGet(page, auth, path, params = {}) {
  assert.ok(auth.token, 'final API verification requires browser access token')
  assert.ok(auth.tenantId, 'final API verification requires browser tenant-id')
  const response = await page.request.get(`${BACKEND_URL}${path}`, {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      'tenant-id': String(auth.tenantId),
      ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
    },
    params
  })
  assert.equal(response.status(), 200, `${path} HTTP status must be 200`)
  const body = await response.json()
  assert.equal(body.code, 0, `${path} business response must succeed: ${body.msg || body.code}`)
  return body.data
}

async function openWorkbench(page) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/scheduler-workbench/summary') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}${ROUTES.workbench}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const body = await readJsonResponse(page, await responsePromise, '排产员工作台')
  assert.equal(body.code, 0, `排产员工作台接口业务错误: ${body.msg || body.code}`)
  await page.getByText('排产员工作台').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function discoverScheduleOrderByCode(page, workOrderCode) {
  await page.goto(`${BASE_URL}${ROUTES.scheduleOrder}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 30000 })
  const panel = page.locator('.schedule-order-pool > .el-card, .schedule-order-pool > .content-wrap').first()
  await fillFirstVisible(panel.locator('input[placeholder="请输入工单编码"]'), workOrderCode, 'schedule order work order code')
  const [pageResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    panel.getByRole('button', { name: /搜索/ }).click()
  ])
  const body = await readJsonResponse(page, pageResponse, '排产工单查询')
  assert.equal(body.code, 0, `排产工单查询接口业务错误: ${body.msg || body.code}`)
  const row = body.data?.list?.find?.((item) => item.erpWorkOrderCode === workOrderCode)
  assert.ok(row, `BLOCKED: 测试租户缺少真实排产工单 ${workOrderCode}`)
  for (const field of ['id', 'workOrderId', 'routeId']) {
    assert.ok(row[field], `BLOCKED: 排产工单 ${workOrderCode} 缺少 ${field}`)
  }
  assert.ok(row.routeCode || row.routeName, `BLOCKED: 排产工单 ${workOrderCode} 缺少路线显示信息`)
  return {
    scheduleOrderId: Number(row.id),
    workOrderId: Number(row.workOrderId),
    routeId: Number(row.routeId),
    routeCode: row.routeCode,
    routeName: row.routeName
  }
}

async function readWorkOrder(page, auth, workOrderId) {
  const workOrder = await apiGet(page, auth, '/admin-api/mes/pro/work-order/get', { id: workOrderId })
  assert.equal(workOrder.code, WORK_ORDER_CODE, `工单编码必须匹配 ${WORK_ORDER_CODE}`)
  assert.ok(workOrder.batchCode, `BLOCKED: 工单 ${WORK_ORDER_CODE} 缺少批次号 batchCode`)
  assert.ok(workOrder.productId, `BLOCKED: 工单 ${WORK_ORDER_CODE} 缺少产品 productId`)
  return workOrder
}

async function discoverEligibleScheduleContext(page, auth) {
  const preferredContext = await discoverScheduleOrderByCode(page, WORK_ORDER_CODE)
  const preferredWorkOrder = await apiGet(page, auth, '/admin-api/mes/pro/work-order/get', {
    id: preferredContext.workOrderId
  })
  if (preferredWorkOrder?.batchCode && (await isWorkOrderInTaskScope(page, auth, WORK_ORDER_CODE))) {
    return { scheduleContext: preferredContext, workOrder: preferredWorkOrder }
  }
  if (EXPLICIT_WORK_ORDER_CODE) {
    assert.ok(preferredWorkOrder?.batchCode, `BLOCKED: 工单 ${WORK_ORDER_CODE} 缺少批次号 batchCode`)
    assert.ok(
      await isWorkOrderInTaskScope(page, auth, WORK_ORDER_CODE),
      `BLOCKED: 工单 ${WORK_ORDER_CODE} 不在生产排产页可发布范围，必须是已确认/自行生产/未冻结的真实待排产工单`
    )
  }

  const pageData = await apiGet(page, auth, '/admin-api/mes/pro/schedule-order/page', {
    pageNo: 1,
    pageSize: 200
  })
  const rows = pageData?.list || []
  for (const row of rows) {
    if (!row?.erpWorkOrderCode || !row?.workOrderId || !row?.routeId) {
      continue
    }
    const workOrder = await apiGet(page, auth, '/admin-api/mes/pro/work-order/get', { id: row.workOrderId })
    if (workOrder?.batchCode && workOrder?.productId && (await isWorkOrderInTaskScope(page, auth, row.erpWorkOrderCode))) {
      WORK_ORDER_CODE = row.erpWorkOrderCode
      const scheduleContext = await discoverScheduleOrderByCode(page, WORK_ORDER_CODE)
      return { scheduleContext, workOrder }
    }
  }
  throw new Error(
    `BLOCKED: 测试租户前 ${rows.length} 条排产工单均缺少 T4 前置数据，至少需要 workOrderId + batchCode + productId + routeId。默认工单 881MO090863 缺少 batchCode。`
  )
}

async function isWorkOrderInTaskScope(page, auth, workOrderCode) {
  const pageData = await apiGet(page, auth, '/admin-api/mes/pro/work-order/page', {
    pageNo: 1,
    pageSize: 10,
    code: workOrderCode,
    status: 1,
    type: 1,
    temporaryFrozen: false
  })
  return flattenRows(pageData?.list || []).some((row) => row.code === workOrderCode)
}

async function runAutoScheduleApplyFromPage(page) {
  await page.goto(`${BASE_URL}${ROUTES.task}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await Promise.race([
    page.getByText('待排产工单').first().waitFor({ state: 'visible', timeout: 60000 }),
    page.locator('input[placeholder="请输入工单编码"]').first().waitFor({ state: 'visible', timeout: 60000 })
  ]).catch(async (error) => {
    const url = page.url()
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(`BLOCKED: 无法打开生产排产页面 ${ROUTES.task}: ${error.message}; url=${url}; body=${bodyText.slice(0, 500)}`)
  })
  const searchForm = page.locator('form.el-form').first()
  await fillFirstVisible(searchForm.locator('input[placeholder="请输入工单编码"]'), WORK_ORDER_CODE, 'task work order code')
  const [workOrderResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/work-order/page') &&
        response.url().includes(encodeURIComponent(WORK_ORDER_CODE)) &&
        response.status() === 200,
      { timeout: 60000 }
    ),
    searchForm.getByRole('button', { name: /搜索/ }).click()
  ])
  const workOrderBody = await readJsonResponse(page, workOrderResponse, '待排产工单查询')
  assert.equal(workOrderBody.code, 0, `待排产工单查询接口业务错误: ${workOrderBody.msg || workOrderBody.code}`)
  assert.ok(
    flattenRows(workOrderBody.data?.list || []).some((row) => row.code === WORK_ORDER_CODE),
    `BLOCKED: /mes/pro/task 当前筛选范围缺少待排产工单 ${WORK_ORDER_CODE}`
  )
  await clickFirstEnabled(page.getByRole('button', { name: /自动排产/ }), '自动排产')
  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '自动排产' }).first()
  await drawer.waitFor({ state: 'visible', timeout: 30000 })

  const [previewResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/auto-schedule/preview') && response.status() === 200,
      { timeout: 60000 }
    ),
    drawer.getByRole('button', { name: '生成预览' }).click()
  ])
  const previewBody = await readJsonResponse(page, previewResponse, '自动排产预览')
  assert.equal(previewBody.code, 0, `自动排产预览接口业务错误: ${previewBody.msg || previewBody.code}`)
  assert.equal(
    previewBody.data?.summary?.blockingIssueCount,
    0,
    `BLOCKED: 自动排产预览存在阻塞项: ${JSON.stringify(previewBody.data?.issues || [])}`
  )
  assert.ok(previewBody.data?.calendarContextToken, 'BLOCKED: 自动排产预览缺少 calendarContextToken')
  assert.ok(Number(previewBody.data?.summary?.generatedTaskCount || 0) >= 1, '自动排产预览必须生成真实任务')

  await drawer.getByRole('button', { name: '确认发布' }).click()
  await page.locator('.el-message-box:visible').first().waitFor({ state: 'visible', timeout: 30000 })
  const [applyResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/auto-schedule/apply'),
      { timeout: 60000 }
    ),
    (async () => {
      const confirmButton = page.locator('.el-message-box:visible').getByRole('button', { name: /确认|确定/ }).first()
      await domClickFirstEnabled(confirmButton, 'confirm auto schedule publish')
    })()
  ])
  assert.equal(applyResponse.status(), 200, `自动排产发布 HTTP 异常: ${applyResponse.status()}`)
  const applyBody = await readJsonResponse(page, applyResponse, '自动排产发布')
  assert.equal(applyBody.code, 0, `BLOCKED: 自动排产发布失败，不能创建 eDHR 批次: ${applyBody.msg || JSON.stringify(applyBody)}`)
  assert.equal(applyBody.data?.applied, true, '自动排产发布必须返回 applied=true')
  await page.getByText('自动排产已发布').first().waitFor({ state: 'visible', timeout: 30000 })
  return applyBody.data
}

function assertTaskNodeOrder(tasks) {
  assert.ok(Array.isArray(tasks) && tasks.length >= 5, 'eDHR 批次详情必须至少包含四类特殊节点和普通表单')
  const nodeTypes = tasks.map((task) => task.nodeType)
  for (const requiredType of REQUIRED_NODE_TYPES) {
    assert.ok(nodeTypes.includes(requiredType), `eDHR 批次节点缺少 ${requiredType}`)
  }
  const incomingIndex = nodeTypes.indexOf('INCOMING_INSPECTION_REPORT')
  const firstRouteIndex = nodeTypes.indexOf('ROUTE_FORM')
  const sterilizationIndex = nodeTypes.indexOf('STERILIZATION_REPORT')
  const finishedReportIndex = nodeTypes.indexOf('FINISHED_PRODUCT_INSPECTION_REPORT')
  const finishedRecordIndex = nodeTypes.indexOf('FINISHED_PRODUCT_INSPECTION_RECORD')
  assert.ok(incomingIndex < firstRouteIndex, '来料检报告必须在普通表单之前')
  assert.ok(firstRouteIndex < sterilizationIndex, '普通表单必须在灭菌报告之前')
  assert.ok(sterilizationIndex < finishedReportIndex, '灭菌报告必须在成品检报告之前')
  assert.ok(finishedReportIndex < finishedRecordIndex, '成品检报告必须在成品检记录之前')
}

async function readUniqueBatchFromApi(page, auth, context) {
  const pageData = await apiGet(page, auth, '/admin-api/mes/pro/edhr-batch-execution/page', {
    pageNo: 1,
    pageSize: 50,
    workOrderCode: WORK_ORDER_CODE,
    batchCode: context.batchCode
  })
  const candidates = pageData?.list || []
  const details = []
  for (const row of candidates) {
    const detail = await apiGet(page, auth, '/admin-api/mes/pro/edhr-batch-execution/get', { id: row.id })
    if (
      Number(detail.workOrderId) === Number(context.workOrderId) &&
      detail.batchCode === context.batchCode &&
      Number(detail.routeId) === Number(context.routeId)
    ) {
      details.push(detail)
    }
  }
  assert.equal(
    details.length,
    1,
    `同一 workOrderId + batchCode + routeId 必须唯一，实际 ${details.length}: ${JSON.stringify(details.map((item) => item.id))}`
  )
  assertTaskNodeOrder(details[0].tasks || [])
  return details[0]
}

function sqlString(value) {
  return String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")
}

function mysql(sql) {
  try {
    return execFileSync(
      'docker',
      [
        'exec',
        '-i',
        MYSQL_CONTAINER,
        'mysql',
        `-u${MYSQL_USER}`,
        `-p${MYSQL_PASSWORD}`,
        '--default-character-set=utf8mb4',
        '--batch',
        '--raw',
        '--skip-column-names',
        MYSQL_DATABASE,
        '-e',
        sql
      ],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }
    ).trim()
  } catch (error) {
    throw new Error(`BLOCKED: 无法执行只读 DB 核对，缺少 Docker/MySQL 前置或容器不可用: ${error.stderr || error.message}`)
  }
}

function dbVerifyUniqueBatch(context, apiBatchId) {
  const output = mysql(`
SELECT JSON_OBJECT(
  'batchCount', COUNT(*),
  'batchIds', JSON_ARRAYAGG(id),
  'taskOrderCsv', (
    SELECT GROUP_CONCAT(t.node_type ORDER BY t.route_process_sort, t.id SEPARATOR ',')
    FROM mes_pro_edhr_batch_execution_task t
    WHERE t.tenant_id = 122
      AND t.deleted = 0
      AND t.batch_execution_id = MIN(b.id)
  )
)
FROM mes_pro_edhr_batch_execution b
WHERE b.tenant_id = 122
  AND b.deleted = 0
  AND b.work_order_id = ${Number(context.workOrderId)}
  AND b.batch_code = '${sqlString(context.batchCode)}'
  AND b.route_id = ${Number(context.routeId)};
`)
  assert.ok(output, 'DB 核对必须返回 JSON')
  const row = JSON.parse(output)
  assert.equal(Number(row.batchCount), 1, `DB 核对发现重复 eDHR 批次: ${JSON.stringify(row)}`)
  assert.ok(row.batchIds.includes(Number(apiBatchId)), `DB 批次 ID 必须包含 API 批次 ${apiBatchId}: ${JSON.stringify(row)}`)
  assertTaskNodeOrder(String(row.taskOrderCsv || '').split(',').filter(Boolean).map((nodeType) => ({ nodeType })))
  return row
}

async function adminReadonlyRecheck(page) {
  const writeRequests = []
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      READONLY_WRITE_METHODS.has(request.method()) &&
      !request.url().includes('/system/auth/login')
    ) {
      writeRequests.push(`${request.method()} ${request.url()}`)
    }
  })
  await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD, ROUTES.batchExecution)
  await page.goto(`${BASE_URL}${ROUTES.workbench}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('排产员工作台').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.goto(`${BASE_URL}${ROUTES.batchExecution}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 30000 })
  assert.deepEqual(writeRequests, [], `admin 只读复验不得调用 MES 写接口: ${writeRequests.join(', ')}`)
}

async function main() {
  assertLocalOnly()
  const browser = await chromium.launch({ headless: process.env.EDHR_AUTO_SCHEDULE_E2E_HEADED !== '1' })
  const testContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const testPage = await testContext.newPage()
  try {
    await login(testPage, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD, ROUTES.workbench)
    const auth = await browserAuth(testPage)
    assert.equal(String(auth.tenantId), '122', `写入 E2E 必须在测试租户 tenant_id=122，实际 ${auth.tenantId}`)
    await openWorkbench(testPage)
    const { scheduleContext, workOrder } = await discoverEligibleScheduleContext(testPage, auth)
    await readWorkOrder(testPage, auth, scheduleContext.workOrderId)
    const context = { ...scheduleContext, batchCode: workOrder.batchCode }

    await runAutoScheduleApplyFromPage(testPage)
    const firstBatch = await readUniqueBatchFromApi(testPage, auth, context)
    const firstDb = dbVerifyUniqueBatch(context, firstBatch.id)

    await runAutoScheduleApplyFromPage(testPage)
    const secondBatch = await readUniqueBatchFromApi(testPage, auth, context)
    assert.equal(secondBatch.id, firstBatch.id, '重复触发自动排产必须复用同一个 eDHR 批次')
    const secondDb = dbVerifyUniqueBatch(context, firstBatch.id)
    assert.equal(Number(secondDb.batchCount), Number(firstDb.batchCount), '重复触发后 DB 唯一批次数不得变化')

    const adminContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const adminPage = await adminContext.newPage()
    await adminReadonlyRecheck(adminPage)
    await adminContext.close()

    console.log(
      `PASS: T4 auto schedule creates unique eDHR batch workOrder=${WORK_ORDER_CODE} batchCode=${context.batchCode} routeId=${context.routeId} batchExecutionId=${firstBatch.id}`
    )
  } finally {
    await testContext.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
