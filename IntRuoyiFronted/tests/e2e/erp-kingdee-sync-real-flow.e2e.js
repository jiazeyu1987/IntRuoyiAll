const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = process.env.ERP_KINGDEE_SYNC_E2E_BASE_URL || 'http://localhost:8081'
const API_BASE = process.env.ERP_KINGDEE_SYNC_E2E_API_BASE || 'http://127.0.0.1:48081/admin-api'
const TENANT = process.env.ERP_KINGDEE_SYNC_E2E_TENANT || '测试租户'
const USERNAME = process.env.ERP_KINGDEE_SYNC_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.ERP_KINGDEE_SYNC_E2E_PASSWORD || 'admin123'
const HANDLER_SYNC_TYPE = process.env.ERP_KINGDEE_SYNC_E2E_SYNC_TYPE || 'PRODUCTION_ORDER'
const ROW_LABEL = process.env.ERP_KINGDEE_SYNC_E2E_ROW_LABEL || '生产工单'

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(800)
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
  throw new Error(`missing visible ${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }

  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible())) {
    await tenantSelect.click()
    await page.locator('.login-form .el-select__input').first().fill(TENANT)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), USERNAME, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), PASSWORD, 'password')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await page.locator('.login-form .el-button--primary').first().click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.status(), 200, 'login http status')
  const body = await loginResponse.json()
  assert.equal(body.code, 0, `login code: ${JSON.stringify(body)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  await settle(page)
}

function unwrapWsCacheValue(raw) {
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

async function buildHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const token = unwrapWsCacheValue(snapshot.ACCESS_TOKEN)
  const tenantId = unwrapWsCacheValue(snapshot.tenantId)
  assert.ok(token, 'ACCESS_TOKEN missing')
  return {
    Authorization: `Bearer ${token}`,
    'tenant-id': tenantId || '122',
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
}

async function fetchJson(page, requestUrl, requestHeaders) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, { headers: requestHeaders })
      const text = await response.text()
      let payload
      try {
        payload = JSON.parse(text)
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    { requestUrl, requestHeaders }
  )
}

function collectMenuRoute(menus, targetComponent, base = '') {
  for (const menu of menus || []) {
    const path = menu.path || ''
    const full = path.startsWith('/') ? path : `${base}/${path}`.replace(/\/+/g, '/')
    if (menu.component === targetComponent || menu.componentName === 'ErpKingdeeSync') {
      return full
    }
    const found = collectMenuRoute(menu.children || [], targetComponent, full)
    if (found) return found
  }
  return ''
}

async function latestRun(page, authHeaders, syncType) {
  const response = await fetchJson(
    page,
    `${API_BASE}/erp/kingdee-sync/run/page?syncType=${encodeURIComponent(syncType)}&pageNo=1&pageSize=1`,
    authHeaders
  )
  assert.equal(response.status, 200, 'run page http status')
  assert.equal(response.payload.code, 0, `run page code: ${JSON.stringify(response.payload)}`)
  return response.payload.data?.list?.[0] || null
}

async function run() {
  const browser = await chromium.launch({ headless: process.env.ERP_KINGDEE_SYNC_E2E_HEADED !== '1' })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const observedRequests = []
  page.on('response', async (response) => {
    const url = response.url()
    if (url.includes('/admin-api/erp/kingdee-sync') || url.includes('/admin-api/infra/job/')) {
      let payload = ''
      try {
        payload = (await response.text()).slice(0, 1000)
      } catch {
        payload = '<unreadable>'
      }
      observedRequests.push({ url, method: response.request().method(), status: response.status(), payload })
    }
  })

  try {
    await login(page)
    const authHeaders = await buildHeaders(page)
    const permission = await fetchJson(page, `${API_BASE}/system/auth/get-permission-info`, authHeaders)
    assert.equal(permission.status, 200, 'permission http status')
    assert.equal(permission.payload.code, 0, `permission code: ${JSON.stringify(permission.payload)}`)
    const route = collectMenuRoute(permission.payload.data.menus, 'erp/sync/index')
    assert.ok(route, 'ErpKingdeeSync route missing from permission menus')

    const watermark = await fetchJson(page, `${API_BASE}/erp/kingdee-sync/watermark/list`, authHeaders)
    assert.equal(watermark.status, 200, 'watermark http status')
    assert.equal(watermark.payload.code, 0, `watermark code: ${JSON.stringify(watermark.payload)}`)
    assert.ok(Array.isArray(watermark.payload.data), 'watermark data must be array')

    const runPage = await fetchJson(page, `${API_BASE}/erp/kingdee-sync/run/page?pageNo=1&pageSize=10`, authHeaders)
    assert.equal(runPage.status, 200, 'run page http status')
    assert.equal(runPage.payload.code, 0, `run page code: ${JSON.stringify(runPage.payload)}`)

    await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page)
    await page.getByText('金蝶同步运行', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    const pageText = await page.locator('body').innerText({ timeout: 30000 })
    if (!pageText.includes(ROW_LABEL)) {
      throw new Error(`sync page did not render row label ${ROW_LABEL}. url=${page.url()} text=${pageText.slice(0, 1200)}`)
    }

    const before = await latestRun(page, authHeaders, HANDLER_SYNC_TYPE)
    const beforeId = before?.id || 0
    const targetRow = page.locator('.el-table__row').filter({ hasText: ROW_LABEL }).first()
    await targetRow.waitFor({ state: 'visible', timeout: 30000 })
    await targetRow.getByRole('button', { name: /执行一次/ }).click()
    await page.getByText(/成功|触发/, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    await page.waitForTimeout(1200)
    const triggerRequest = observedRequests.find((request) => request.url.includes('/admin-api/infra/job/trigger'))
    assert.ok(
      triggerRequest,
      `manual trigger did not call /infra/job/trigger. observed=${JSON.stringify(observedRequests, null, 2)}`
    )
    assert.equal(triggerRequest.status, 200, `trigger http status: ${JSON.stringify(triggerRequest)}`)

    let latest = null
    for (let attempt = 0; attempt < 12; attempt += 1) {
      await page.waitForTimeout(1500)
      latest = await latestRun(page, authHeaders, HANDLER_SYNC_TYPE)
      if (latest && latest.id && Number(latest.id) !== Number(beforeId)) break
    }
    assert.ok(
      latest && latest.id && Number(latest.id) !== Number(beforeId),
      `manual trigger did not create a new run record. observed=${JSON.stringify(observedRequests, null, 2)} latest=${JSON.stringify(latest)}`
    )
    assert.ok([10, 20, 30, 'RUNNING', 'SUCCESS', 'FAILED'].includes(latest.status), `unexpected run status ${latest.status}`)
    if (latest.status === 30 || latest.status === 'FAILED') {
      assert.ok(latest.failureMessage || latest.errorMessage, 'failed sync run must expose failure message')
    }

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          route,
          syncType: HANDLER_SYNC_TYPE,
          beforeId,
          latest,
          observedRequests
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error.stack || error.message)
  process.exit(1)
})
