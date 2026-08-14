const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const outputDir = path.resolve(
  workspaceRoot,
  'output',
  'playwright',
  '20260803-mes-route-tabs-no-reload-real'
)
fs.mkdirSync(outputDir, { recursive: true })

function readEnvFiles() {
  const result = {}
  for (const fileName of ['.env', '.env.local']) {
    const filePath = path.join(repoRoot, fileName)
    if (!fs.existsSync(filePath)) continue
    const content = fs.readFileSync(filePath, 'utf8')
    for (const line of content.split(/\r?\n/)) {
      const trimmed = line.trim()
      if (!trimmed || trimmed.startsWith('#')) continue
      const separator = trimmed.indexOf('=')
      if (separator <= 0) continue
      const key = trimmed.slice(0, separator).trim()
      const value = trimmed
        .slice(separator + 1)
        .trim()
        .replace(/^['"]|['"]$/g, '')
      result[key] = value
    }
  }
  return result
}

const localEnv = readEnvFiles()
const requiredLoginValue = (envKey, fileKey) => {
  const value = process.env[envKey] || localEnv[fileKey]
  if (!value) {
    throw new Error(`Missing local E2E login precondition: ${envKey} or ${fileKey}`)
  }
  return value
}

const config = {
  baseUrl: (process.env.MES_ROUTE_TABS_NO_RELOAD_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: requiredLoginValue('MES_ROUTE_TABS_NO_RELOAD_TENANT', 'VITE_APP_DEFAULT_LOGIN_TENANT'),
  username: requiredLoginValue(
    'MES_ROUTE_TABS_NO_RELOAD_USERNAME',
    'VITE_APP_DEFAULT_LOGIN_USERNAME'
  ),
  password: requiredLoginValue(
    'MES_ROUTE_TABS_NO_RELOAD_PASSWORD',
    'VITE_APP_DEFAULT_LOGIN_PASSWORD'
  ),
  headed: process.env.MES_ROUTE_TABS_NO_RELOAD_HEADED === '1',
  timeout: Number(process.env.MES_ROUTE_TABS_NO_RELOAD_TIMEOUT || 60000),
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
}

const ROUTE_LIST_PATH = '/mes/pro/route'
const BATCH_RECORD_FORM_LIST_PATH = '/mes/pro/batch-record-form-list'
const ROUTE_LIST_API = '/admin-api/mes/pro/route/page'
const BATCH_RECORD_FORM_LIST_API = '/admin-api/mes/pro/batch-record-report/page'

function assertLocalOnly(baseUrl) {
  const hostname = new URL(baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${baseUrl}`
  )
}

function isRouteListRequest(url) {
  return url.includes(ROUTE_LIST_API)
}

function isBatchRecordFormListRequest(url) {
  return url.includes(BATCH_RECORD_FORM_LIST_API)
}

function isMesWriteRequest(request) {
  return (
    request.url().includes('/admin-api/mes/') &&
    !['GET', 'HEAD', 'OPTIONS'].includes(request.method().toUpperCase())
  )
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => null)
  await page.waitForTimeout(1200)
}

async function selectTenant(page, form) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: config.timeout })
    await tenantOption.click()
    return
  }
  await form.locator('input[placeholder="请输入租户名称"]').first().fill(config.tenant)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form)
  const usernameInput = form
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
  await usernameInput.fill('')
  await usernameInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login payload failed: code=${loginPayload.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
}

async function waitForTargetResponse(page, predicate, label, action) {
  const responsePromise = page.waitForResponse(
    (response) => predicate(response.url()) && response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  try {
    await action()
  } catch (error) {
    responsePromise.catch(() => undefined)
    throw error
  }
  const response = await responsePromise
  assert.ok(response.ok(), `${label} HTTP failed: ${response.status()}`)
  const payload = await response.json().catch(() => undefined)
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    assert.ok([0, 200].includes(payload.code), `${label} payload failed: code=${payload.code}`)
  }
  return response
}

function snapshotCounts(counters) {
  return {
    routeList: counters.routeList,
    batchRecordFormList: counters.batchRecordFormList
  }
}

function assertNoTargetReload(before, after, label) {
  assert.deepEqual(after, before, `${label} must not reload target list APIs`)
}

function tagByText(page, label) {
  return page
    .locator('#v-tags-view .v-tags-view__item')
    .filter({ hasText: label })
    .filter({ visible: true })
    .first()
}

async function assertTagsVisible(page) {
  await tagByText(page, '工艺流程').waitFor({ state: 'visible', timeout: config.timeout })
  await tagByText(page, '批记录表单').waitFor({ state: 'visible', timeout: config.timeout })
}

async function clickVisibleMenuItem(page, label) {
  const menu = page.locator('.el-menu').first()
  await menu.waitFor({ state: 'visible', timeout: config.timeout })
  const item = menu
    .locator('.el-menu-item, .el-sub-menu__title')
    .filter({ hasText: label })
    .filter({ visible: true })
    .first()
  if ((await item.count()) === 0) {
    const visibleMenuText = (await menu.innerText()).replace(/\s+/g, ' ').trim()
    throw new Error(`Missing visible menu item "${label}". Visible menu text: ${visibleMenuText}`)
  }
  await item.scrollIntoViewIfNeeded()
  await item.click()
}

async function clickVisibleMenuPath(page, parentLabel, childLabel) {
  const menu = page.locator('.el-menu').first()
  await menu.waitFor({ state: 'visible', timeout: config.timeout })
  const existingChild = menu
    .locator('.el-menu-item, .el-sub-menu__title')
    .filter({ hasText: childLabel })
    .filter({ visible: true })
    .first()
  if ((await existingChild.count()) === 0) {
    await clickVisibleMenuItem(page, parentLabel)
  }
  await clickVisibleMenuItem(page, childLabel)
}

async function assertRouteListVisible(page) {
  await page.waitForURL((url) => url.pathname === ROUTE_LIST_PATH, { timeout: config.timeout })
  await page.locator('.el-table').first().waitFor({ state: 'visible', timeout: config.timeout })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: config.timeout
  })
}

async function assertBatchRecordFormListVisible(page) {
  await page.waitForURL((url) => url.pathname === BATCH_RECORD_FORM_LIST_PATH, {
    timeout: config.timeout
  })
  await page
    .locator('.batch-record-form-layout')
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })
  await page.getByText('批记录表单', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: config.timeout
  })
}

async function main() {
  assertLocalOnly(config.baseUrl)
  if (config.executablePath) {
    assert.ok(fs.existsSync(config.executablePath), `Chrome not found: ${config.executablePath}`)
  }

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.executablePath || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1600, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(config.timeout)
  page.setDefaultNavigationTimeout(config.timeout)

  const counters = { routeList: 0, batchRecordFormList: 0 }
  const mesWriteRequests = []
  const targetNetworkFailures = []
  const consoleErrors = []
  const pageErrors = []

  page.on('request', (request) => {
    if (isRouteListRequest(request.url())) counters.routeList += 1
    if (isBatchRecordFormListRequest(request.url())) counters.batchRecordFormList += 1
    if (isMesWriteRequest(request)) {
      mesWriteRequests.push(`${request.method()} ${new URL(request.url()).pathname}`)
    }
  })
  page.on('requestfailed', (request) => {
    if (isRouteListRequest(request.url()) || isBatchRecordFormListRequest(request.url())) {
      targetNetworkFailures.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`)
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)

    await waitForTargetResponse(page, isRouteListRequest, 'route list initial load', async () => {
      await page.goto(new URL(ROUTE_LIST_PATH, config.baseUrl).toString(), {
        waitUntil: 'domcontentloaded',
        timeout: config.timeout
      })
    })
    await assertRouteListVisible(page)
    await settle(page)
    const afterRouteInitial = snapshotCounts(counters)

    await waitForTargetResponse(page, isBatchRecordFormListRequest, 'batch record form initial load', async () => {
      await clickVisibleMenuPath(page, 'eDHR批记录', '批记录表单')
    })
    await assertBatchRecordFormListVisible(page)
    await settle(page)
    const afterBatchInitial = snapshotCounts(counters)
    assert.equal(
      afterBatchInitial.routeList,
      afterRouteInitial.routeList,
      'leaving 工艺流程 for 批记录表单 must not reload 工艺流程 list'
    )

    await assertTagsVisible(page)

    const beforeRouteTagReturn = snapshotCounts(counters)
    await tagByText(page, '工艺流程').click()
    await assertRouteListVisible(page)
    await settle(page)
    const afterRouteTagReturn = snapshotCounts(counters)
    assertNoTargetReload(beforeRouteTagReturn, afterRouteTagReturn, 'returning to 工艺流程 tag')

    const beforeBatchTagReturn = snapshotCounts(counters)
    await tagByText(page, '批记录表单').click()
    await assertBatchRecordFormListVisible(page)
    await settle(page)
    const afterBatchTagReturn = snapshotCounts(counters)
    assertNoTargetReload(beforeBatchTagReturn, afterBatchTagReturn, 'returning to 批记录表单 tag')

    assert.deepEqual(mesWriteRequests, [], `read-only E2E emitted MES writes: ${mesWriteRequests.join(' | ')}`)
    assert.deepEqual(targetNetworkFailures, [], `target network failures: ${targetNetworkFailures.join(' | ')}`)
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' | ')}`)

    const screenshot = path.join(outputDir, 'mes-route-tabs-no-reload-pass.png')
    await page.screenshot({ path: screenshot, fullPage: true })

    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routePath: ROUTE_LIST_PATH,
      batchRecordFormPath: BATCH_RECORD_FORM_LIST_PATH,
      counts: {
        afterRouteInitial,
        afterBatchInitial,
        beforeRouteTagReturn,
        afterRouteTagReturn,
        beforeBatchTagReturn,
        afterBatchTagReturn
      },
      mesWriteRequests,
      targetNetworkFailures,
      consoleErrorCount: consoleErrors.length,
      pageErrors,
      screenshot
    }
    const resultPath = path.join(outputDir, 'mes-route-tabs-no-reload-result.json')
    fs.writeFileSync(resultPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(JSON.stringify(result, null, 2))
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
