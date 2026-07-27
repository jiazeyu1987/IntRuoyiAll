const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')

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
  baseUrl: (process.env.MES_ROUTE_FLOW_TAB_RETURN_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: requiredLoginValue(
    'MES_ROUTE_FLOW_TAB_RETURN_TENANT',
    'VITE_APP_DEFAULT_LOGIN_TENANT'
  ),
  username: requiredLoginValue(
    'MES_ROUTE_FLOW_TAB_RETURN_USERNAME',
    'VITE_APP_DEFAULT_LOGIN_USERNAME'
  ),
  password: requiredLoginValue(
    'MES_ROUTE_FLOW_TAB_RETURN_PASSWORD',
    'VITE_APP_DEFAULT_LOGIN_PASSWORD'
  ),
  routeCode: process.env.MES_ROUTE_FLOW_TAB_RETURN_ROUTE_CODE || ''
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalOnly(baseUrl) {
  const hostname = new URL(baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${baseUrl}`
  )
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(800)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/route')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    if (await tenantOption.count()) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  const accountInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  await accountInput.fill('')
  await accountInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(payload.code),
    `login failed: HTTP ${loginResponse.status()} code=${payload.code}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

function tagsViewItem(page, text) {
  return page.locator('#v-tags-view .v-tags-view__item').filter({ hasText: text }).first()
}

async function navigateAwayInsideApp(page) {
  const candidates = ['生产工单', '工序设置', '工序流转']
  for (const label of candidates) {
    const menuItem = page.locator('.el-menu').getByText(label, { exact: true }).first()
    if (!(await menuItem.count())) continue
    await menuItem.scrollIntoViewIfNeeded()
    await menuItem.click()
    await page.waitForFunction(
      () => !window.location.pathname.includes('/mes/pro/route/edit/'),
      undefined,
      { timeout: 60000 }
    )
    await settle(page)
    return label
  }
  const menuTexts = await page.locator('.el-menu').allInnerTexts()
  throw new Error(`Missing in-app navigation target away from route flow: ${menuTexts.join(' | ')}`)
}

async function openProfileTab(page) {
  await page.goto(`${config.baseUrl}/user/profile`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await tagsViewItem(page, '个人中心').waitFor({ state: 'visible', timeout: 60000 })
}

async function openRouteGraph(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page)

  if (config.routeCode) {
    const routeCodeInput = page
      .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
      .first()
    await routeCodeInput.fill(config.routeCode)
    await page.getByRole('button', { name: /查询|搜索/ }).first().click()
    await settle(page)
  }

  const rows = page.locator('tr.el-table__row')
  const row = config.routeCode
    ? rows.filter({ hasText: config.routeCode }).first()
    : rows.filter({ has: page.locator('.route-list__version-link') }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const selectedRouteText = (await row.innerText()).replace(/\s+/g, ' ').trim()
  const activeVersionLink = row.locator('.route-list__version-link').first()
  await activeVersionLink.waitFor({ state: 'visible', timeout: 60000 })
  await activeVersionLink.click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit/'), {
    timeout: 60000
  })
  await assertRouteGraphVisible(page)
  return selectedRouteText
}

async function assertRouteGraphVisible(page) {
  const url = new URL(page.url())
  assert.ok(
    url.pathname.includes('/mes/pro/route/edit/'),
    `expected route edit page after tag return, got ${url.pathname}`
  )
  assert.equal(url.searchParams.get('tab'), 'flow', `expected tab=flow after tag return, got ${url.search}`)
  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.getByRole('tab', { name: '流转关系图' }).waitFor({
    state: 'visible',
    timeout: 60000
  })
  const nodes = editor.locator('[data-flow-node="route-process"]')
  await nodes.first().waitFor({ state: 'visible', timeout: 60000 })
  assert.ok((await nodes.count()) > 0, 'route flow graph nodes must stay visible after tag return')
}

async function main() {
  assertLocalOnly(config.baseUrl)

  const browser = await chromium.launch({
    headless: true,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1600, height: 900 } })
  const page = await context.newPage()
  const mesWriteRequests = []
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
    ) {
      mesWriteRequests.push(`${request.method()} ${request.url()}`)
    }
  })

  try {
    await login(page)
    await openProfileTab(page)
    const selectedRouteText = await openRouteGraph(page)

    const awayMenu = await navigateAwayInsideApp(page)

    const routeTag = tagsViewItem(page, '工艺流程')
    await routeTag.scrollIntoViewIfNeeded()
    await routeTag.click()
    await assertRouteGraphVisible(page)
    assert.deepEqual(mesWriteRequests, [], `read-only E2E emitted MES writes: ${mesWriteRequests.join(' | ')}`)

    console.log(
      `PASS: route flow top tag return kept flow graph, baseUrl=${config.baseUrl}, tenant=${config.tenant}, username=${config.username}, awayMenu=${awayMenu}, route=${selectedRouteText}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
