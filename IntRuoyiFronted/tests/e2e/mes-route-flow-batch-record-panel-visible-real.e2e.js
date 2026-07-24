const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_FLOW_BATCH_RECORD_PANEL_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_FLOW_BATCH_RECORD_PANEL_TENANT || '测试租户',
  username: process.env.MES_ROUTE_FLOW_BATCH_RECORD_PANEL_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_FLOW_BATCH_RECORD_PANEL_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_FLOW_BATCH_RECORD_PANEL_ROUTE_CODE || 'RT000017',
  headed: process.env.MES_ROUTE_FLOW_BATCH_RECORD_PANEL_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_FLOW_BATCH_RECORD_PANEL_ARTIFACT_DIR ||
      path.join(__dirname, '..', 'output', '20260722-route-flow-batch-record-panel-visible')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalOnly() {
  const parsed = new URL(config.baseUrl)
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(parsed.hostname),
    `真实页面验证仅允许本机入口，当前为 ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', '真实页面验证必须使用测试租户。')
  assert.equal(config.username, 'aoteman', '真实页面验证必须使用 aoteman。')
}

function adminApiPath(url) {
  const marker = '/admin-api/'
  const index = url.indexOf(marker)
  if (index < 0) return ''
  return url.slice(index + marker.length).split('?')[0]
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(500)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await form.locator('input[placeholder="请输入租户名称"]').first().fill(config.tenant)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/route')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, form)
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
  assert.ok([0, 200].includes(payload.code), `登录失败: ${payload.msg || JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function openRouteFlowGraph(page) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await pageResponsePromise
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  await page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
    .fill(config.routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)
  const row = page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: config.routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).first().click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })
  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.getByRole('tab', { name: '流转关系图' }).click()
  await editor.locator('.route-flow-graph-designer').waitFor({ state: 'visible', timeout: 60000 })
  await editor.locator('[data-flow-node="route-process"]').first().waitFor({ state: 'visible', timeout: 60000 })
  return editor
}

async function main() {
  assertLocalOnly()
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const mesWriteRequests = []
  const pageErrors = []
  const consoleErrors = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('request', (request) => {
      const apiPath = adminApiPath(request.url())
      if (!apiPath.startsWith('mes/')) return
      if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())) {
        mesWriteRequests.push(`${request.method()} ${apiPath}`)
      }
    })
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') consoleErrors.push(message.text())
    })

    await login(page)
    const editor = await openRouteFlowGraph(page)
    await editor.locator('[data-flow-node="route-process"]').first().click()
    const panel = editor.locator('[data-flow-panel="selected-process-detail"]').first()
    const batchRecordField = panel.locator('[data-flow-detail-field="batchRecordFormNames"]').first()
    await batchRecordField.waitFor({ state: 'visible', timeout: 60000 })
    await batchRecordField.getByText('批记录表单', { exact: true }).waitFor({ state: 'visible', timeout: 10000 })
    const fieldText = (await batchRecordField.innerText()).replace(/\s+/g, ' ').trim()
    assert.ok(fieldText.includes('批记录表单'), '左侧面板必须显示批记录表单。')
    assert.deepEqual(mesWriteRequests, [], `只读验证不得产生 MES 写请求: ${mesWriteRequests.join(', ')}`)
    assert.deepEqual(pageErrors, [], `页面错误: ${pageErrors.join('\n')}`)
    assert.deepEqual(consoleErrors, [], `控制台错误: ${consoleErrors.join('\n')}`)

    const evidence = {
      ok: true,
      routeCode: config.routeCode,
      fieldKey: 'batchRecordFormNames',
      fieldText,
      mesWriteRequests,
      pageErrors,
      consoleErrors
    }
    fs.writeFileSync(
      path.join(config.artifactDir, 'route-flow-batch-record-panel-visible-real-result.json'),
      `${JSON.stringify(evidence, null, 2)}\n`,
      'utf8'
    )
    await page.screenshot({
      path: path.join(config.artifactDir, 'route-flow-batch-record-panel-visible-real.png'),
      fullPage: true
    })
    await context.close()
    console.log(`PASS: route flow batch record panel visible real E2E route=${config.routeCode}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
