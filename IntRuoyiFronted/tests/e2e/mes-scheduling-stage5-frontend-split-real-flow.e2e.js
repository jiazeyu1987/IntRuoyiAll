const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const artifactDir = path.resolve(
  process.env.MES_SCHEDULING_STAGE5_E2E_ARTIFACT_DIR ||
    path.join(__dirname, '../output/20260711-scheduling-stage5-frontend-split-real')
)

const config = {
  baseUrl: (process.env.MES_SCHEDULING_STAGE5_E2E_BASE_URL || 'http://127.0.0.1:18081').replace(/\/+$/, ''),
  tenant: process.env.MES_SCHEDULING_STAGE5_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULING_STAGE5_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULING_STAGE5_E2E_PASSWORD,
  headed: process.env.MES_SCHEDULING_STAGE5_E2E_HEADED === '1'
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function trackRuntimeIssues(page, issues) {
  page.on('pageerror', (error) => {
    issues.push(`PAGEERROR ${error.message}`)
  })
  page.on('console', (message) => {
    const text = message.text()
    if (
      message.type() === 'error' &&
      !text.includes('hm.baidu.com') &&
      (text.includes('/src/views/mes/') || text.includes('/admin-api/mes/'))
    ) {
      issues.push(`CONSOLE ${message.type()} ${text}`)
    }
  })
  page.on('requestfailed', (request) => {
    const url = request.url()
    if (url.includes('hm.baidu.com')) return
    if (request.failure()?.errorText === 'net::ERR_ABORTED') return
    if (url.includes('/src/views/mes/') || url.includes('/admin-api/mes/')) {
      issues.push(`REQFAIL ${request.method()} ${url} ${request.failure()?.errorText || ''}`)
    }
  })
  page.on('response', (response) => {
    const url = response.url()
    if ((url.includes('/src/views/mes/') || url.includes('/admin-api/mes/')) && response.status() >= 500) {
      issues.push(`HTTP_${response.status()} ${url}`)
    }
  })
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
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/schedule-order')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const captchaCount = await page
    .locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible')
    .count()
  if (captchaCount > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(form.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginBody = await loginResponse.json()
  assert.equal(loginBody.code, 0, `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
}

async function readJsonResponse(page, response, label) {
  try {
    return await response.json()
  } catch (error) {
    if (!String(error?.message || '').includes('No resource with given identifier')) {
      throw error
    }
    const replay = await page.request.get(response.url())
    assert.equal(replay.status(), 200, `${label} 响应体重取 HTTP 异常: ${replay.status()}`)
    return replay.json()
  }
}

async function verifyScheduleOrderPage(page) {
  const pagePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const pageResponse = await pagePromise
  const pageBody = await readJsonResponse(page, pageResponse, '排产工单列表')
  assert.equal(pageBody.code, 0, `排产工单列表接口业务错误: ${pageBody.msg || pageBody.code}`)
  assert.ok(Array.isArray(pageBody.data?.list), '排产工单列表必须返回真实 list。')
  assert.ok(pageBody.data.list.length > 0, '测试租户缺少排产工单真实数据，无法验证阶段5页面拆分。')

  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('[data-user-table-key="mes.pro.scheduleOrder.main"]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  await page.locator('.schedule-order-pool .unified-list-template').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  for (const text of ['排产工单', '生产工单号', '产品编号', '手动重排']) {
    await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  }

  const row = page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr:visible').first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const processPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/process-list') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await row.locator('button:visible').filter({ hasText: '查看' }).first().click()
  const processResponse = await processPromise
  const processBody = await readJsonResponse(page, processResponse, '排产工单工序列表')
  assert.equal(processBody.code, 0, `排产工单工序列表业务错误: ${processBody.msg || processBody.code}`)
  assert.ok(Array.isArray(processBody.data), '排产工单工序列表必须返回数组。')
  assert.ok(processBody.data.length > 0, '测试租户排产工单缺少工序快照真实数据。')
  const processDialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺流程' }).last()
  await processDialog.waitFor({ state: 'visible', timeout: 30000 })
  await processDialog.locator('[data-user-table-key="mes.pro.scheduleOrder.processRoute"]').waitFor({
    state: 'visible',
    timeout: 30000
  })
  await page.screenshot({ path: path.join(artifactDir, 'schedule-order-process-detail.png'), fullPage: true })
  await page.keyboard.press('Escape')
  await processDialog.waitFor({ state: 'hidden', timeout: 30000 })

  const selectableCheckbox = page
    .locator('.schedule-order-pool .el-table__body-wrapper tbody tr:visible .el-checkbox:not(.is-disabled)')
    .first()
  await selectableCheckbox.waitFor({ state: 'visible', timeout: 30000 })
  await selectableCheckbox.click({ force: true })
  await page.waitForFunction(() =>
    Array.from(document.querySelectorAll('button')).some((button) => {
      const text = button.textContent || ''
      return /手动重排/.test(text) && !button.disabled && !button.classList.contains('is-disabled')
    })
  )
  await page.getByRole('button', { name: /手动重排/ }).click()
  const replanDrawer = page.locator('.el-drawer:visible').filter({ hasText: '排产前检查 / 手动重排' })
  await replanDrawer.waitFor({
    state: 'visible',
    timeout: 30000
  })
  const preflightPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/preflight') &&
      response.request().method() === 'POST' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await replanDrawer.getByRole('button', { name: /重新检查/ }).click()
  const preflightResponse = await preflightPromise
  const preflightBody = await readJsonResponse(page, preflightResponse, '排产前检查')
  assert.equal(preflightBody.code, 0, `排产前检查业务错误: ${preflightBody.msg || preflightBody.code}`)
  await page.screenshot({ path: path.join(artifactDir, 'schedule-order-replan-drawer.png'), fullPage: true })

  return {
    scheduleOrderCount: pageBody.data.list.length,
    processCount: processBody.data.length,
    preflightIssueCount: preflightBody.data?.issues?.length || 0
  }
}

async function verifyWorkbenchPage(page) {
  const summaryPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/scheduler-workbench/summary') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  const wipPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/process-wip-statistics') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/scheduler-workbench`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const [summaryResponse, wipResponse] = await Promise.all([summaryPromise, wipPromise])
  const summaryBody = await readJsonResponse(page, summaryResponse, '排产工作台概览')
  const wipBody = await readJsonResponse(page, wipResponse, '工序在制统计')
  assert.equal(summaryBody.code, 0, `排产工作台概览业务错误: ${summaryBody.msg || summaryBody.code}`)
  assert.equal(wipBody.code, 0, `工序在制统计业务错误: ${wipBody.msg || wipBody.code}`)
  assert.ok(Array.isArray(wipBody.data), '工序在制统计必须返回数组。')
  assert.ok(wipBody.data.length > 0, '测试租户缺少工序在制真实数据，无法验证阶段5工作台拆分。')

  await page.locator('.scheduler-workbench').waitFor({ state: 'visible', timeout: 60000 })
  const processWipTable = page.locator('.scheduler-workbench__process-wip-table')
  await processWipTable.waitFor({
    state: 'visible',
    timeout: 60000
  })
  await page.locator('[data-user-table-key="mes.pro.schedulerWorkbench.processWip"]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  for (const text of ['工艺路线', '工序', '在制', '班次产能', '今日报工']) {
    await processWipTable.getByText(text, { exact: false }).first().waitFor({
      state: 'visible',
      timeout: 30000
    })
  }
  await page.screenshot({ path: path.join(artifactDir, 'scheduler-workbench-process-wip.png'), fullPage: true })

  return {
    wipCount: wipBody.data.length,
    hasTodayAvailableCapacity: 'todayAvailableCapacity' in (summaryBody.data || {})
  }
}

async function main() {
  assert.equal(config.tenant, '测试租户', '阶段5真实 E2E 必须使用测试租户。')
  assert.equal(config.username, 'aoteman', '阶段5真实 E2E 必须使用 aoteman。')
  assert.ok(config.password, '阶段5真实 E2E 必须通过环境变量提供密码，禁止在脚本中硬编码。')
  ensureDir(artifactDir)
  const runtimeIssues = []
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  trackRuntimeIssues(page, runtimeIssues)

  try {
    await login(page)
    const scheduleOrder = await verifyScheduleOrderPage(page)
    const workbench = await verifyWorkbenchPage(page)
    assert.deepEqual(runtimeIssues, [], `阶段5页面存在运行时错误: ${JSON.stringify(runtimeIssues, null, 2)}`)
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      scheduleOrder,
      workbench,
      screenshots: [
        'schedule-order-process-detail.png',
        'schedule-order-replan-drawer.png',
        'scheduler-workbench-process-wip.png'
      ]
    }
    fs.writeFileSync(path.join(artifactDir, 'result.json'), JSON.stringify(result, null, 2), 'utf8')
    console.log(JSON.stringify(result, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  ensureDir(artifactDir)
  fs.writeFileSync(path.join(artifactDir, 'error.txt'), error.stack || String(error), 'utf8')
  console.error(error)
  process.exit(1)
})
