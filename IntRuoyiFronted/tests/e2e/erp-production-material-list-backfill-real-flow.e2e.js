const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = process.env.ERP_PML_BACKFILL_E2E_BASE_URL || 'http://localhost:8081'
const API_BASE = process.env.ERP_PML_BACKFILL_E2E_API_BASE || 'http://127.0.0.1:48081/admin-api'
const TENANT = process.env.ERP_PML_BACKFILL_E2E_TENANT || '测试租户'
const USERNAME = process.env.ERP_PML_BACKFILL_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.ERP_PML_BACKFILL_E2E_PASSWORD || '111111'
const ROW_LABEL = '生产用料清单'

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(1000)
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
  await page.goto(`${BASE_URL}/login?redirect=/erp/sync`, { waitUntil: 'domcontentloaded', timeout: 60000 })
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
  assert.ok([0, 200].includes(body.code), `login code: ${JSON.stringify(body)}`)
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

async function fetchJson(page, requestUrl, requestHeaders, options = {}) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders, options }) => {
      const response = await fetch(requestUrl, {
        method: options.method || 'GET',
        headers: requestHeaders,
        body: options.body || undefined
      })
      const text = await response.text()
      let payload
      try {
        payload = JSON.parse(text)
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    { requestUrl, requestHeaders, options }
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

async function latestRun(page, headers) {
  const response = await fetchJson(
    page,
    `${API_BASE}/erp/kingdee-sync/run/page?syncType=PRODUCTION_MATERIAL_LIST&pageNo=1&pageSize=1`,
    headers
  )
  assert.equal(response.status, 200, 'run page http status')
  assert.equal(response.payload.code, 0, `run page code: ${JSON.stringify(response.payload)}`)
  return response.payload.data?.list?.[0] || null
}

async function run() {
  const browser = await chromium.launch({ headless: process.env.ERP_PML_BACKFILL_E2E_HEADED !== '1' })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const observedRequests = []
  page.on('response', async (response) => {
    const url = response.url()
    if (url.includes('/admin-api/erp/production-material-list/sync-kingdee') || url.includes('/admin-api/erp/kingdee-sync/run/page')) {
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

    const before = await latestRun(page, authHeaders)
    const beforeId = before?.id || 0

    await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page)
    await page.getByText('金蝶同步运行', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    const targetRow = page.locator('.el-table__row').filter({ hasText: ROW_LABEL }).first()
    await targetRow.waitFor({ state: 'visible', timeout: 30000 })
    await targetRow.getByRole('button', { name: /执行一次/ }).click()
    await page.getByText(/成功|触发|回补/, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })

    let latest = null
    for (let attempt = 0; attempt < 20; attempt += 1) {
      await page.waitForTimeout(1500)
      latest = await latestRun(page, authHeaders)
      if (latest && latest.id && Number(latest.id) !== Number(beforeId)) break
    }

    const manualResponse = observedRequests.find((request) =>
      request.url.includes('/admin-api/erp/production-material-list/sync-kingdee')
    )

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          route,
          beforeId,
          latest,
          manualResponse,
          observedRequests
        },
        null,
        2
      )
    )

    assert.ok(manualResponse, `manual sync endpoint was not called. observed=${JSON.stringify(observedRequests, null, 2)}`)
    assert.equal(manualResponse.status, 200, `manual sync http status: ${JSON.stringify(manualResponse)}`)
    assert.ok(
      latest && latest.id && Number(latest.id) !== Number(beforeId),
      `manual sync did not create a new run record. latest=${JSON.stringify(latest)} observed=${JSON.stringify(observedRequests, null, 2)}`
    )
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error.stack || error.message)
  process.exit(1)
})
