const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_SPECIAL_NODE_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_SPECIAL_NODE_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = process.env.EDHR_SPECIAL_NODE_TEST_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_SPECIAL_NODE_TEST_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_SPECIAL_NODE_TEST_PASSWORD || 'admin123'
const ADMIN_TENANT = process.env.EDHR_SPECIAL_NODE_ADMIN_TENANT || '芋道源码'
const ADMIN_USERNAME = process.env.EDHR_SPECIAL_NODE_ADMIN_USERNAME || 'admin'
const ADMIN_PASSWORD = process.env.EDHR_SPECIAL_NODE_ADMIN_PASSWORD || 'admin123'
const ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const PREFERRED_WORK_ORDER_CODE = process.env.EDHR_SPECIAL_NODE_WORK_ORDER_CODE || '881MO090863'
const PREFERRED_ROUTE_ID = process.env.EDHR_SPECIAL_NODE_ROUTE_ID || '922045'
const BATCH_CODE = `E2E-SPECIAL-${Date.now()}`
const STERILIZATION_BATCH_NO = `STER-${Date.now()}`

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'E2E must use the local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'E2E must use the local backend')
  assert.equal(TEST_TENANT, '测试租户', 'write E2E must use 测试租户')
  assert.equal(TEST_USERNAME, 'aoteman', 'write E2E must use the dedicated test account aoteman')
  assert.notEqual(TEST_TENANT, ADMIN_TENANT, 'write tenant and readonly admin tenant must be different')
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

async function login(page, tenant, username, password) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('Login captcha is enabled; unattended real E2E cannot continue.')
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
  await clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }), 'login button')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openCreateDialog(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(page.getByRole('button', { name: '打开/创建' }), '打开/创建')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  return dialog
}

async function selectWorkOrderOption(page, dialog, optionIndex) {
  const workOrderSelect = dialog.locator('.el-select input[role="combobox"]').first()
  await workOrderSelect.click()
  const options = page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
  await options.first().waitFor({ state: 'visible', timeout: 60000 })
  const optionCount = await options.count()
  if (optionIndex >= optionCount) {
    throw new Error(`测试租户可见未冻结工单不足，无法选择第 ${optionIndex + 1} 条工单。`)
  }
  await options.nth(optionIndex).click()
}

async function selectWorkOrderByKeyword(page, dialog, keyword) {
  const workOrderSelect = dialog.locator('.el-select input[role="combobox"]').first()
  await workOrderSelect.click()
  await workOrderSelect.fill(keyword)
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: keyword }).first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
}

async function fillRouteId(dialog, routeId) {
  const routeInput = dialog.locator('.el-form-item').filter({ hasText: '路线ID' }).locator('input').first()
  await routeInput.fill(routeId)
}

async function submitCreateDialog(page, dialog, batchCode) {
  await fillFirstVisible(dialog.locator('.el-form-item').filter({ hasText: '批次号' }).locator('input'), batchCode, 'batch code')
  const openResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/open-or-create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: '确 认' }), 'confirm create')
  const response = await openResponse
  assert.equal(response.status(), 200, 'open-or-create HTTP status must be 200')
  return await response.json()
}

async function createBatchFromAvailableWorkOrder(page) {
  const dialog = await openCreateDialog(page)
  const preferredBatchCode = `${BATCH_CODE}-PREFERRED`
  try {
    await selectWorkOrderByKeyword(page, dialog, PREFERRED_WORK_ORDER_CODE)
    await fillRouteId(dialog, PREFERRED_ROUTE_ID)
    const preferredBody = await submitCreateDialog(page, dialog, preferredBatchCode)
    if (preferredBody.code === 0) {
      assert.ok(Number.isFinite(Number(preferredBody.data?.id)), 'open-or-create must return a real batchExecution id')
      await page.waitForURL((url) => url.pathname === `${ROUTE}/detail`, { timeout: 60000 })
      await page.getByText(preferredBatchCode).first().waitFor({ state: 'visible', timeout: 60000 })
      return { batchExecutionId: Number(preferredBody.data.id), batchCode: preferredBatchCode }
    }
    if (!String(preferredBody.message || preferredBody.msg || '').includes('工艺路线不存在')) {
      throw new Error(`preferred open-or-create business response failed: ${JSON.stringify(preferredBody)}`)
    }
  } catch (error) {
    if (!String(error instanceof Error ? error.message : error).includes('工艺路线不存在')) {
      throw error
    }
  }

  let lastFailure = ''
  for (let optionIndex = 0; optionIndex < 10; optionIndex += 1) {
    await selectWorkOrderOption(page, dialog, optionIndex)
    const batchCode = `${BATCH_CODE}-${optionIndex + 1}`
    const body = await submitCreateDialog(page, dialog, batchCode)
    if (body.code === 0) {
      assert.ok(Number.isFinite(Number(body.data?.id)), 'open-or-create must return a real batchExecution id')
      await page.waitForURL((url) => url.pathname === `${ROUTE}/detail`, { timeout: 60000 })
      await page.getByText(batchCode).first().waitFor({ state: 'visible', timeout: 60000 })
      return { batchExecutionId: Number(body.data.id), batchCode }
    }
    lastFailure = JSON.stringify(body)
    if (!String(body.message || body.msg || '').includes('工艺路线不存在')) {
      throw new Error(`open-or-create business response failed: ${lastFailure}`)
    }
  }
  throw new Error(`测试租户前 10 条未冻结工单均无法解析 eDHR 工艺路线，最后一次响应：${lastFailure}`)
}

async function clickSpecialRowAction(page, nodeName, actionName) {
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: nodeName }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(row.getByRole('button', { name: actionName }), `${nodeName} ${actionName}`)
}

async function exerciseSpecialNodes(page) {
  for (const nodeName of ['来料检报告', '灭菌报告', '成品检报告', '成品检记录']) {
    await page.getByText(nodeName).first().waitFor({ state: 'visible', timeout: 60000 })
  }
  assert.equal(await page.getByText('无模板节点').count(), 4, 'detail page must render four no-template special node tags')
  assert.equal(await page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: '来料检报告' }).getByRole('button', { name: '打开填写' }).count(), 0, 'special nodes must not expose ordinary form opening')

  const skipResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/special-node/skip') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickSpecialRowAction(page, '来料检报告', '跳过')
  const skipDialog = page.locator('.el-dialog:visible').filter({ hasText: '跳过特殊节点' }).first()
  await skipDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(skipDialog.locator('textarea').first(), '真实演练：来料检报告由现场记录确认后跳过', 'special node skip reason')
  await fillFirstVisible(skipDialog.locator('input[type="password"]'), TEST_PASSWORD, 'special node skip signature password')
  await clickFirstEnabled(skipDialog.getByRole('button', { name: /签名并跳过/ }), 'sign and skip special node')
  const skipResult = await skipResponse
  assert.equal(skipResult.status(), 200, 'special node skip HTTP status must be 200')
  const skipBody = await skipResult.json()
  assert.equal(skipBody.code, 0, `special node skip business response must succeed: ${JSON.stringify(skipBody)}`)
  await page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: '来料检报告' }).getByText('已跳过').waitFor({ state: 'visible', timeout: 60000 })

  const completeResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/special-node/complete') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickSpecialRowAction(page, '灭菌报告', '完成')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '完成特殊节点' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(dialog.locator('input[placeholder="请输入灭菌批次"]'), STERILIZATION_BATCH_NO, 'sterilization batch no')
  await clickFirstEnabled(dialog.getByRole('button', { name: '确 认' }), 'complete special node')
  const completeResult = await completeResponse
  assert.equal(completeResult.status(), 200, 'special node complete HTTP status must be 200')
  const completeBody = await completeResult.json()
  assert.equal(completeBody.code, 0, `special node complete business response must succeed: ${JSON.stringify(completeBody)}`)
  await page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: '灭菌报告' }).getByText('已批准').waitFor({ state: 'visible', timeout: 60000 })
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

async function finalApiVerify(page, batchExecutionId) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, 'final API verification requires browser access token')
  assert.ok(tenantId, 'final API verification requires browser tenant-id')
  const response = await page.request.get(`${BACKEND_URL}/admin-api/mes/pro/edhr-batch-execution/get`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    params: { id: batchExecutionId }
  })
  assert.equal(response.status(), 200, 'batch detail API HTTP status must be 200')
  const body = await response.json()
  assert.equal(body.code, 0, `batch detail business response must succeed: ${body.msg || body.code}`)
  const tasks = body.data?.tasks || []
  assert.equal(tasks.filter((task) => task.nodeType && task.nodeType !== 'ROUTE_FORM').length, 4, 'final API detail must contain four special nodes')
  const skippedIncoming = tasks.find((task) => task.nodeType === 'INCOMING_INSPECTION_REPORT' && task.status === 45)
  assert.ok(skippedIncoming, 'incoming inspection node must be skipped')
  assert.match(skippedIncoming.specialPayloadJson || '', /skipReason/, 'incoming inspection skip payload must keep skip reason')
  assert.match(skippedIncoming.specialPayloadJson || '', /skipSignatureId/, 'incoming inspection skip payload must keep signature id')
  const sterilizationTask = tasks.find((task) => task.nodeType === 'STERILIZATION_REPORT')
  assert.ok(sterilizationTask, 'sterilization special node must exist')
  assert.equal(sterilizationTask.status, 40, 'sterilization special node must be completed as approved')
  assert.match(sterilizationTask.specialPayloadJson || '', new RegExp(STERILIZATION_BATCH_NO), 'sterilization payload must keep the real batch no')
}

async function verifyAdminReadonly(page) {
  await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD)
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })
  assert.equal(await page.getByRole('button', { name: '打开/创建' }).count(), 1, 'admin readonly verification must reach the real batch execution page')
}

async function main() {
  assertLocalOnly()
  const browser = await chromium.launch({ headless: process.env.EDHR_SPECIAL_NODE_HEADED !== '1' })
  const testContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const testPage = await testContext.newPage()
  let createdBatchCode = ''
  try {
    await login(testPage, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD)
    const { batchExecutionId, batchCode } = await createBatchFromAvailableWorkOrder(testPage)
    createdBatchCode = batchCode
    await exerciseSpecialNodes(testPage)
    await finalApiVerify(testPage, batchExecutionId)

    const adminContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const adminPage = await adminContext.newPage()
    await verifyAdminReadonly(adminPage)
    await adminContext.close()
  } finally {
    await testContext.close()
    await browser.close()
  }
  console.log(`PASS: eDHR special no-template nodes real E2E batch=${createdBatchCode} sterilization=${STERILIZATION_BATCH_NO}`)
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
