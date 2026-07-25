const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_PROCESS_FLOW_ADMIN_BASE_URL || 'http://127.0.0.1:8041').replace(/\/+$/, ''),
  tenant: process.env.MES_PROCESS_FLOW_ADMIN_TENANT || '芋道源码',
  username: process.env.MES_PROCESS_FLOW_ADMIN_USERNAME || 'admin',
  password: process.env.MES_PROCESS_FLOW_ADMIN_PASSWORD || '',
  headed: process.env.MES_PROCESS_FLOW_ADMIN_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_PROCESS_FLOW_ADMIN_ARTIFACT_DIR ||
      path.join(__dirname, '..', '..', '..', 'output', 'playwright', '20260725-process-flow-tab-e2e-fix')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalAuthorizedTarget() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `真实 E2E 仅允许本机入口，当前为 ${config.baseUrl}`
  )
  assert.equal(config.tenant, '芋道源码', `本用例只覆盖用户授权租户，当前为 ${config.tenant}`)
  assert.equal(config.username, 'admin', `本用例只覆盖用户授权账号，当前为 ${config.username}`)
}

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function isLocalOrAdminApiRequest(url) {
  try {
    const parsed = new URL(url)
    return ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(parsed.hostname) || url.includes('/admin-api/')
  } catch {
    return url.includes('/admin-api/')
  }
}

function adminApiPath(url) {
  const marker = '/admin-api/'
  const index = url.indexOf(marker)
  if (index < 0) return ''
  return url.slice(index + marker.length).split('?')[0]
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(600)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return item
    }
  }
  throw new Error(`未找到可见输入框: ${label}`)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
}

async function login(page) {
  const targetPath = '/mes/pro/route'
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form)
  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"]):visible'), config.username, 'username')

  const passwordInput = form.locator('input[type="password"]').first()
  await passwordInput.waitFor({ state: 'visible', timeout: 30000 })
  if (config.password) {
    await passwordInput.fill(config.password)
  }
  const hasPassword = await passwordInput.evaluate((element) => Boolean(element.value))
  assert.ok(hasPassword, '登录页未提供本机默认密码，且未设置 MES_PROCESS_FLOW_ADMIN_PASSWORD')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(payload.code),
    `登录失败: HTTP ${loginResponse.status()} ${payload.msg || JSON.stringify(payload)}`
  )
  try {
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
  } catch (error) {
    const bodyText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
    fs.writeFileSync(
      path.join(config.artifactDir, 'login-timeout-state.json'),
      `${JSON.stringify(
        {
          url: page.url(),
          bodyText: bodyText.slice(0, 4000)
        },
        null,
        2
      )}\n`,
      'utf8'
    )
    await page.screenshot({
      path: path.join(config.artifactDir, 'login-timeout.png'),
      fullPage: true
    })
    throw error
  }
}

async function openProcessFlowList(page) {
  const routePageResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route/page') &&
        response.request().method() === 'GET' &&
        response.status() === 200,
      { timeout: 60000 }
    )
    .catch((error) => ({ error }))
  await page.goto(`${config.baseUrl}/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  const routePageResponse = await routePageResponsePromise
  assert.ok(
    !routePageResponse.error,
    `工艺流程列表接口未成功返回: ${routePageResponse.error?.message || routePageResponse.error}`
  )
  const row = page.locator('.el-table__body-wrapper .el-table__row').first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const rowText = (await row.innerText()).trim()
  assert.ok(rowText, '真实数据中缺少可进入的工艺路线记录')
  return { rowText }
}
async function openFlowTab(page, route) {
  const routeEditRequests = []
  const forbiddenWrites = []
  let captureEditRequests = false

  page.on('request', (request) => {
    const apiPath = adminApiPath(request.url())
    if (captureEditRequests && apiPath) {
      routeEditRequests.push(`${request.method()} ${apiPath}`)
    }
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) && /^mes\/pro\/route/.test(apiPath)) {
      forbiddenWrites.push(`${request.method()} ${apiPath}`)
    }
  })

  const flowGraphResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process-flow/get') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  const routeProcessResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-process/list-by-route') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )

  captureEditRequests = true
  const row = page.locator('.el-table__body-wrapper .el-table__row').first()
  await row.getByRole('button', { name: '编辑' }).first().click()
  await page.locator('.route-edit-page').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.route-flow-graph-designer').first().waitFor({ state: 'visible', timeout: 60000 })

  const [flowGraphResponse, routeProcessResponse] = await Promise.all([
    flowGraphResponsePromise,
    routeProcessResponsePromise
  ])
  const flowGraphPayload = await flowGraphResponse.json()
  const routeProcessPayload = await routeProcessResponse.json()
  assert.ok(
    [0, 200].includes(flowGraphPayload.code),
    `工艺流程图加载失败: ${flowGraphPayload.msg || JSON.stringify(flowGraphPayload)}`
  )
  assert.ok(
    [0, 200].includes(routeProcessPayload.code),
    `工艺流程工序列表加载失败: ${routeProcessPayload.msg || JSON.stringify(routeProcessPayload)}`
  )
  assert.ok(
    Array.isArray(routeProcessPayload.data) && routeProcessPayload.data.length > 0,
    '真实数据路线必须至少包含一个工序'
  )
  captureEditRequests = false
  assert.deepEqual(forbiddenWrites, [], `只读访问工艺流程不得产生写请求: ${forbiddenWrites.join(', ')}`)
  return { flowGraphPayload, routeProcessPayload, routeEditRequests }
}

async function main() {
  assertLocalAuthorizedTarget()
  ensureArtifactDir()

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const pageErrors = []
  const consoleErrors = []
  const requestFailures = []

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })
    page.on('requestfailed', (request) => {
      requestFailures.push({
        method: request.method(),
        url: request.url(),
        resourceType: request.resourceType(),
        errorText: request.failure()?.errorText || ''
      })
    })

    await login(page)
    await settle(page)
    const route = await openProcessFlowList(page)
    const result = await openFlowTab(page, route)
    await settle(page)
    await page.screenshot({
      path: path.join(config.artifactDir, 'process-flow-admin-tab.png'),
      fullPage: true
    })

    const evidence = {
      ok: true,
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routeRowText: route.rowText,
      nodeCount: result.flowGraphPayload.data?.nodes?.length || 0,
      routeProcessCount: result.routeProcessPayload.data.length,
      routeEditRequests: result.routeEditRequests,
      pageErrors,
      consoleErrors,
      requestFailures,
      localRequestFailures: requestFailures.filter((request) => isLocalOrAdminApiRequest(request.url)),
      externalRequestFailures: requestFailures.filter((request) => !isLocalOrAdminApiRequest(request.url))
    }
    fs.writeFileSync(
      path.join(config.artifactDir, 'process-flow-admin-tab-result.json'),
      `${JSON.stringify(evidence, null, 2)}\n`,
      'utf8'
    )
    assert.deepEqual(pageErrors, [], `页面异常: ${pageErrors.join('\n')}`)
    assert.deepEqual(consoleErrors, [], `控制台错误: ${consoleErrors.join('\n')}`)
    assert.deepEqual(evidence.localRequestFailures, [], `本机/API 请求失败: ${JSON.stringify(evidence.localRequestFailures, null, 2)}`)
    console.log(
      `PASS: 芋道源码/admin 工艺流程页签可访问，routeProcessCount=${evidence.routeProcessCount}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
