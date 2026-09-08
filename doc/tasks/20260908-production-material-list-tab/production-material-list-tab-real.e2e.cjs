const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..')
const frontendRequire = createRequire(path.join(PROJECT_ROOT, 'IntRuoyiFronted', 'package.json'))
const { chromium } = frontendRequire('playwright')

const FRONTEND_URL = process.env.PML_E2E_FRONTEND_URL || 'http://127.0.0.1:8207'
const TENANT_NAME = process.env.PML_E2E_TENANT || '芋道源码'
const USERNAME = process.env.PML_E2E_USERNAME || 'admin'
const PASSWORD = process.env.PML_E2E_PASSWORD || ''
const WORK_ORDER_CODE =
  process.env.PML_E2E_WORK_ORDER_CODE ||
  'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
const TASK_DIR = path.resolve(__dirname)
const RESULT_PATH = path.join(TASK_DIR, 'production-material-list-tab-real-result.json')
const SCREENSHOT_PATH = path.join(TASK_DIR, 'production-material-list-tab-real.png')

if (!PASSWORD) {
  throw new Error('PML_E2E_PASSWORD is required.')
}

const sanitize = (value) => String(value || '').replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')

async function selectLoginTenant(page) {
  const tenantInput = page.locator('.login-form .el-select input:visible').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  await tenantInput.click()
  await tenantInput.fill(TENANT_NAME)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: TENANT_NAME }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  if (!page.url().includes('/login')) {
    return
  }
  await selectLoginTenant(page)
  await page.locator('.login-form input[placeholder="请输入用户名"]:visible').first().fill(USERNAME)
  await page.locator('.login-form input[type="password"][placeholder="请输入密码"]:visible').first().fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await page.locator('.login-form button[type="submit"]:visible, .login-form button:has-text("登录"):visible').first().click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录失败：HTTP ${loginResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

async function openTargetDetail(page) {
  await page.goto(`${FRONTEND_URL}/mes/pro/process-pool/production-leader`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.el-tabs__item:has-text("活跃订单池")').first().click()
  await page.locator('table', { hasText: '生产订单号' }).first().waitFor({ state: 'visible', timeout: 30000 })
  const targetCell = page.getByText(WORK_ORDER_CODE, { exact: false }).first()
  await targetCell.waitFor({ state: 'visible', timeout: 60000 })
  const row = targetCell.locator('xpath=ancestor::tr[1]')
  const detailPagePromise = page.waitForURL((url) => url.pathname.includes('/submission-detail'), {
    timeout: 30000
  })
  await row.locator('[data-team-leader-active-order-detail], button:has-text("详情")').first().click()
  await detailPagePromise
  await page.locator('[data-team-leader-active-order-detail-page]', { hasText: WORK_ORDER_CODE }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
}

async function verifyProductionMaterialListTab(page, evidence) {
  await page.getByRole('tab', { name: '生产用料清单' }).click()
  await page.locator('[data-team-leader-active-order-detail-production-material-list-tab]').waitFor({
    state: 'attached',
    timeout: 10000
  })
  const responses = await Promise.allSettled(evidence.productionMaterialListResponsePromises)
  const matchingResponses = responses
    .filter((result) => result.status === 'fulfilled')
    .map((result) => result.value)
    .filter((result) => result.url.includes(encodeURIComponent(WORK_ORDER_CODE)))
  const materialListResponse = matchingResponses[matchingResponses.length - 1]
  assert.ok(materialListResponse, `页面未触发当前生产订单 ${WORK_ORDER_CODE} 的生产用料清单查询`)
  evidence.productionMaterialListRequest = sanitize(materialListResponse.url)
  evidence.productionMaterialListStatus = materialListResponse.status
  assert.equal(
    materialListResponse.ok,
    true,
    `生产用料清单接口失败：HTTP ${materialListResponse.status}`
  )
  const list = materialListResponse.body?.data?.list ?? []
  evidence.productionMaterialListRowCount = list.length
  evidence.productionMaterialListBillNos = [...new Set(list.map((row) => row.sourceBillNo).filter(Boolean))]
  assert.ok(list.length > 0, `生产订单 ${WORK_ORDER_CODE} 没有返回生产用料清单明细`)
  await page.locator('[data-active-order-production-material-list-document]').first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  const firstDocument = page.locator('[data-active-order-production-material-list-document]').first()
  await firstDocument.locator('h3', { hasText: '生产用料清单' }).waitFor({
    state: 'visible',
    timeout: 10000
  })
  await firstDocument.getByText('生产订单号：', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 10000
  })
  await firstDocument.getByText('单据编号：', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 10000
  })
  for (const row of list.slice(0, 3)) {
    if (row.childMaterialCode) {
      await firstDocument.getByText(row.childMaterialCode, { exact: false }).first().waitFor({
        state: 'visible',
        timeout: 10000
      })
    }
  }
}

async function run() {
  const evidence = {
    frontendUrl: FRONTEND_URL,
    tenant: TENANT_NAME,
    username: USERNAME,
    workOrderCode: WORK_ORDER_CODE,
    consoleErrors: [],
    pageErrors: [],
    productionMaterialListResponses: []
  }
  const browser = await chromium.launch({ headless: process.env.PML_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1680, height: 980 } })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  evidence.productionMaterialListResponsePromises = []
  page.on('response', (response) => {
    if (
      response.url().includes('/admin-api/erp/production-material-list/page') &&
      response.request().method() === 'GET'
    ) {
      const responsePromise = (async () => {
        const result = {
          url: response.url(),
          status: response.status(),
          ok: response.ok(),
          body: undefined
        }
        if (response.ok()) {
          result.body = await response.json().catch(() => undefined)
        }
        evidence.productionMaterialListResponses.push({
          url: sanitize(result.url),
          status: result.status,
          ok: result.ok
        })
        return result
      })()
      evidence.productionMaterialListResponsePromises.push(responsePromise)
    }
  })
  try {
    await login(page)
    await openTargetDetail(page)
    await verifyProductionMaterialListTab(page, evidence)
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    evidence.screenshot = SCREENSHOT_PATH
    evidence.status = 'PASS'
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error?.stack || String(error)
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true }).catch(() => {})
    evidence.screenshot = SCREENSHOT_PATH
    throw error
  } finally {
    delete evidence.productionMaterialListResponsePromises
    await browser.close().catch(() => {})
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
  }
}

run().catch((error) => {
  console.error(error?.stack || String(error))
  process.exit(1)
})
