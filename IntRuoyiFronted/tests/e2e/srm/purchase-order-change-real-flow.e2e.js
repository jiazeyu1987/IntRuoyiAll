const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = process.cwd()
const outputDir = path.resolve(frontendRoot, 'output/playwright/srm-phase3')
fs.mkdirSync(outputDir, { recursive: true })

const config = {
  baseUrl: process.env.SRM_PHASE3_BASE_URL || 'http://127.0.0.1:8119',
  tenantName: process.env.SRM_PHASE3_TENANT || '测试租户',
  username: process.env.SRM_PHASE3_USERNAME || 'aoteman',
  password: process.env.SRM_PHASE3_PASSWORD || '111111',
  readonlyTenantName: process.env.SRM_PHASE3_READONLY_TENANT || '芋道源码',
  readonlyUsername: process.env.SRM_PHASE3_READONLY_USERNAME || 'admin',
  readonlyPassword: process.env.SRM_PHASE3_READONLY_PASSWORD || 'admin123',
  supplierId: Number(process.env.SRM_PHASE3_SUPPLIER_ID || '108'),
  sourcePlanId: Number(process.env.SRM_PHASE3_CHANGE_PLAN_ID || process.env.SRM_PHASE3_PLAN_ID || '34'),
  sourcePlanNo: process.env.SRM_PHASE3_CHANGE_PLAN_NO || process.env.SRM_PHASE3_PLAN_NO || 'PP-20260621-0100'
}

const TEST_TENANT = '\u6d4b\u8bd5\u79df\u6237'
const READONLY_TENANT = '\u828b\u9053\u6e90\u7801'

function assertSuccess(payload, action) {
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `${action} failed: ${JSON.stringify(payload)}`)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
}

async function login(page, credentials) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const inputs = form.locator('input')
  const tenantInput = inputs.nth(0)
  const userInput = inputs.nth(1)
  const passwordInput = inputs.nth(2)

  await tenantInput.click()
  await tenantInput.fill('')
  await tenantInput.type(credentials.tenantName, { delay: 50 })
  await page.waitForTimeout(1000)
  const option = page.locator('.el-select-dropdown__item').filter({ hasText: credentials.tenantName }).first()
  if (await option.count()) {
    await option.click()
  } else {
    await tenantInput.press('Enter')
  }

  await userInput.fill('')
  await userInput.type(credentials.username, { delay: 40 })
  await passwordInput.fill('')
  await passwordInput.type(credentials.password, { delay: 40 })

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  )
  await form.locator('button.el-button--primary').first().click()
  const loginPayload = await (await loginResponsePromise).json()
  assertSuccess(loginPayload, `login(${credentials.username})`)
  const permissionResponse = await permissionPromise
  const permissionPayload = await permissionResponse.json()
  assertSuccess(permissionPayload, `permission(${credentials.username})`)
  const headers = permissionResponse.request().headers()
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
  return {
    authorization: headers.authorization || headers.Authorization || '',
    tenantId: headers['tenant-id'] || headers['Tenant-Id'] || '',
    permissionPayload
  }
}

async function fetchJson(page, relativeUrl, authContext, { method = 'GET', body } = {}) {
  const payload = await page.evaluate(
    async ({ url, method, body, authHeader, tenantHeader }) => {
      const headers = { Accept: 'application/json, text/plain, */*' }
      if (authHeader) headers.Authorization = authHeader
      if (tenantHeader) headers['tenant-id'] = tenantHeader
      if (body) headers['Content-Type'] = 'application/json'
      const response = await fetch(url, {
        method,
        credentials: 'include',
        headers,
        body: body ? JSON.stringify(body) : undefined
      })
      return { status: response.status, text: await response.text() }
    },
    {
      url: `${config.baseUrl}${relativeUrl}`,
      method,
      body,
      authHeader: authContext.authorization || '',
      tenantHeader: authContext.tenantId || ''
    }
  )
  assert.equal(payload.status, 200, `${method} ${relativeUrl} should return HTTP 200`)
  return JSON.parse(payload.text)
}

async function createPurchaseOrder(page, authContext) {
  const createPayload = await fetchJson(page, '/admin-api/srm/purchase-order/create-from-plan', authContext, {
    method: 'POST',
    body: {
      sourcePlanId: config.sourcePlanId,
      supplierId: config.supplierId,
      orderRemark: `Phase3变更E2E ${config.sourcePlanNo}`
    }
  })
  assertSuccess(createPayload, 'create purchase order from plan')
  return Number(createPayload.data)
}

async function getPurchaseOrder(page, authContext, id, mine = false) {
  const endpoint = mine ? `/admin-api/srm/purchase-order/my/get?id=${id}` : `/admin-api/srm/purchase-order/get?id=${id}`
  const payload = await fetchJson(page, endpoint, authContext)
  assertSuccess(payload, `get purchase order ${id}`)
  return payload.data
}

async function confirmPurchaseOrder(page, authContext, detail, remark) {
  const confirmPayload = await fetchJson(page, '/admin-api/srm/purchase-order/confirm-my', authContext, {
    method: 'PUT',
    body: {
      id: detail.id,
      confirmRemark: remark,
      lines: (detail.lines || []).map((line) => ({
        orderLineId: line.id,
        confirmedQuantity: Number(line.pendingChangedQuantity || line.requestedQuantity),
        confirmedDeliveryDate: line.pendingChangedDeliveryDate || line.requestedDeliveryDate,
        supplierRemark: line.pendingChangedRemark || `确认 ${line.materialCode || line.lineNo}`
      }))
    }
  })
  assertSuccess(confirmPayload, `confirm purchase order ${detail.id}`)
}

async function submitChange(page, authContext, detail, changedQuantity, changedDeliveryDate, changedRemark) {
  const changePayload = await fetchJson(page, '/admin-api/srm/purchase-order/change', authContext, {
    method: 'POST',
    body: {
      orderId: detail.id,
      changeReason: '客户交付窗口调整',
      changeRemark: '真实E2E发起订单变更',
      lines: (detail.lines || []).map((line) => ({
        orderLineId: line.id,
        changedQuantity,
        changedDeliveryDate,
        changedSupplierRemark: changedRemark
      }))
    }
  })
  assertSuccess(changePayload, `submit purchase order change ${detail.id}`)
  return Number(changePayload.data)
}

async function rejectChange(page, authContext, changeId, rejectRemark) {
  const payload = await fetchJson(page, '/admin-api/srm/purchase-order/change/reject-my', authContext, {
    method: 'PUT',
    body: { changeId, rejectRemark }
  })
  assertSuccess(payload, `reject purchase order change ${changeId}`)
}

async function openPageAndScreenshot(page, relativePath, screenshotName, markerLocator) {
  await page.goto(`${config.baseUrl}${relativePath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page, 30000)
  if (markerLocator) {
    await markerLocator(page).waitFor({ state: 'visible', timeout: 30000 })
  }
  const screenshotPath = path.join(outputDir, screenshotName)
  await page.screenshot({ path: screenshotPath, fullPage: true })
  return screenshotPath
}

async function main() {
  assert.equal(config.tenantName, TEST_TENANT, `write E2E must use 测试租户, got ${config.tenantName}`)
  assert.equal(config.username, 'aoteman', `write E2E must use aoteman, got ${config.username}`)
  assert.equal(config.readonlyTenantName, READONLY_TENANT, `readonly tenant must be 芋道源码, got ${config.readonlyTenantName}`)

  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  try {
    const writerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const writerPage = await writerContext.newPage()
    const writerAuth = await login(writerPage, {
      tenantName: config.tenantName,
      username: config.username,
      password: config.password
    })
    assert.equal(writerAuth.tenantId, '122', 'writer tenant-id must be 122')

    const orderId = await createPurchaseOrder(writerPage, writerAuth)
    const createdOrder = await getPurchaseOrder(writerPage, writerAuth, orderId)
    const firstConfirmShot = await openPageAndScreenshot(
      writerPage,
      '/srm/purchase-order/my',
      'purchase-order-change-before-first-confirm.png',
      (page) => page.getByRole('button', { name: '确认' }).first()
    )
    const supplierCreatedOrder = await getPurchaseOrder(writerPage, writerAuth, orderId, true)
    await confirmPurchaseOrder(writerPage, writerAuth, supplierCreatedOrder, 'Phase3变更E2E首次确认')

    const confirmedOrder = await getPurchaseOrder(writerPage, writerAuth, orderId)
    assert.equal(confirmedOrder.orderStatus, 'CONFIRMED')
    const originalLine = confirmedOrder.lines[0]
    const changedQuantity = Number(originalLine.confirmedQuantity) - 1
    const changedDeliveryDate = originalLine.confirmedDeliveryDate

    const changeId = await submitChange(
      writerPage,
      writerAuth,
      confirmedOrder,
      changedQuantity,
      changedDeliveryDate,
      '供应商请确认减量'
    )
    const changePendingOrder = await getPurchaseOrder(writerPage, writerAuth, orderId)
    assert.equal(changePendingOrder.orderStatus, 'CHANGE_PENDING')
    assert.equal(Number(changePendingOrder.lines[0].confirmedQuantity), Number(originalLine.confirmedQuantity))
    assert.equal(Number(changePendingOrder.lines[0].pendingChangedQuantity), changedQuantity)

    const adminChangeShot = await openPageAndScreenshot(
      writerPage,
      '/srm/purchase-order',
      'purchase-order-change-admin-pending.png',
      (page) => page.getByText(changePendingOrder.orderNo, { exact: false }).first()
    )

    const supplierPendingShot = await openPageAndScreenshot(
      writerPage,
      '/srm/purchase-order/my',
      'purchase-order-change-supplier-pending.png',
      (page) => page.getByRole('button', { name: '拒绝变更' }).first()
    )

    await rejectChange(writerPage, writerAuth, changeId, '当前产能不足，无法接受减量计划')
    const rejectedOrder = await getPurchaseOrder(writerPage, writerAuth, orderId)
    assert.equal(rejectedOrder.orderStatus, 'CONFIRMED')
    assert.equal(rejectedOrder.latestChange.changeStatus, 'REJECTED')
    assert.equal(rejectedOrder.latestChange.rejectRemark, '当前产能不足，无法接受减量计划')
    assert.equal(rejectedOrder.lines[0].pendingChangedQuantity, null)
    assert.equal(Number(rejectedOrder.lines[0].confirmedQuantity), Number(originalLine.confirmedQuantity))

    const supplierRejectedShot = await openPageAndScreenshot(
      writerPage,
      '/srm/purchase-order/my',
      'purchase-order-change-supplier-rejected.png',
      (page) => page.getByText(rejectedOrder.orderNo, { exact: false }).first()
    )

    const readonlyContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const readonlyPage = await readonlyContext.newPage()
    const readonlyAuth = await login(readonlyPage, {
      tenantName: config.readonlyTenantName,
      username: config.readonlyUsername,
      password: config.readonlyPassword
    })
    assert.equal(readonlyAuth.tenantId, '1', 'readonly verification must use tenant-id=1')
    const readonlyOrders = await fetchJson(
      readonlyPage,
      `/admin-api/srm/purchase-order/page?pageNo=1&pageSize=20&orderNo=${encodeURIComponent(rejectedOrder.orderNo)}`,
      readonlyAuth
    )
    assertSuccess(readonlyOrders, 'readonly purchase order page')
    const readonlyVisible = (readonlyOrders.data?.list || []).some((item) => item.orderNo === rejectedOrder.orderNo)
    assert.equal(readonlyVisible, false, 'readonly admin tenant should not see test tenant purchase orders')

    console.log(JSON.stringify({
      ok: true,
      baseUrl: config.baseUrl,
      tenantId: writerAuth.tenantId,
      readonlyTenantId: readonlyAuth.tenantId,
      sourcePlanId: config.sourcePlanId,
      sourcePlanNo: config.sourcePlanNo,
      supplierId: config.supplierId,
      orderId,
      orderNo: rejectedOrder.orderNo,
      changeId,
      orderStatusAfterReject: rejectedOrder.orderStatus,
      latestChangeStatus: rejectedOrder.latestChange.changeStatus,
      latestChangeRejectRemark: rejectedOrder.latestChange.rejectRemark,
      screenshots: {
        supplierBeforeFirstConfirm: firstConfirmShot,
        adminChangePending: adminChangeShot,
        supplierChangePending: supplierPendingShot,
        supplierRejected: supplierRejectedShot
      }
    }, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
