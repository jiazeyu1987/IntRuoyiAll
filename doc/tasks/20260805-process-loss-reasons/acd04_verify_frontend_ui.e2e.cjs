const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260805-process-loss-reasons'
const TASK_DIR = path.resolve(__dirname)
const WORKSPACE_ROOT = path.resolve(TASK_DIR, '../../..')
const FRONTEND_ROOT = path.join(WORKSPACE_ROOT, 'IntRuoyiFronted')
const FIXTURE_FILE = path.join(TASK_DIR, 'fixture-summary.json')
const OUTPUT_FILE = path.join(TASK_DIR, 'frontend-ui-verification.json')
const PASSWORD_ENV = 'ACD04_TEST_PASSWORD'
const TENANT_NAME = process.env.ACD04_TENANT_NAME || '测试租户'
const UNIQUE_CODE = `ZZ-ACD04-UI-${Date.now()}`
const CREATED_NAME = `${UNIQUE_CODE}-leader-a-created`
const UPDATED_NAME = `${UNIQUE_CODE}-leader-b-updated`
const TEAM_LEADER_ROUTE = '/mes/pro/process-pool/production-leader'

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'))
}

function writeJson(file, value) {
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

function requirePassword() {
  const password = process.env[PASSWORD_ENV]
  if (!password) {
    throw new Error(`缺少环境变量 ${PASSWORD_ENV}，无法执行真实登录。`)
  }
  return password
}

function requirePlaywright() {
  try {
    return require(path.join(FRONTEND_ROOT, 'node_modules', 'playwright'))
  } catch (error) {
    throw new Error('缺少 IntRuoyiFronted/node_modules/playwright，无法执行真实 Playwright 页面验证。')
  }
}

function resolveChromiumLaunchOptions() {
  const configured = (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '').trim()
  const candidatePaths = configured
    ? [configured]
    : [
        'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
        'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
        'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
        'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe'
      ]
  for (const executablePath of candidatePaths) {
    if (fs.existsSync(executablePath)) {
      return {
        launchOptions: {
          headless: process.env.ACD04_HEADED !== '1',
          executablePath
        },
        browserExecutablePath: executablePath
      }
    }
  }
  return {
    launchOptions: {
      headless: process.env.ACD04_HEADED !== '1'
    },
    browserExecutablePath: 'playwright-managed'
  }
}

function dataOf(response) {
  return response.data
}

function assertBusinessOk(payload, label) {
  assert.ok(payload, `${label} response is empty`)
  assert.ok([0, 200].includes(payload.code), `${label} business code is ${payload.code}: ${payload.msg || payload.message || ''}`)
}

async function assertHttpOk(url, label) {
  const response = await fetch(url)
  assert.ok(response.ok, `${label} 不可用：${url} -> HTTP ${response.status}`)
  return response
}

async function login(page, config, username, password) {
  await page.goto(`${config.frontendUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle')
  await selectTenant(page, config.tenantName)
  await fillFirst(page, [
    'input[placeholder*="账号"]',
    'input[placeholder*="用户名"]',
    'input[name="username"]'
  ], username)
  await fillFirst(page, [
    'input[placeholder*="密码"]',
    'input[type="password"]',
    'input[name="password"]'
  ], password)
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await clickFirst(page, [
    'button:has-text("登录")',
    '.login-form button[type="submit"]'
  ])
  const response = await responsePromise
  assert.equal(response.ok(), true, `登录接口 HTTP ${response.status()}`)
  const payload = await response.json()
  assertBusinessOk(payload, `登录 ${username}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
  await page.waitForLoadState('networkidle')
}

async function selectTenant(page, tenantName) {
  const tenantInput = page.locator('.el-select input:visible').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 20000 })
  await tenantInput.click()
  await tenantInput.fill(tenantName)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: tenantName }).first()
  await option.waitFor({ state: 'visible', timeout: 20000 })
  await option.click()
}

async function fillFirst(pageOrLocator, selectors, value) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(`${selector}:visible`).first()
    if (await locator.count()) {
      await locator.fill(String(value))
      return
    }
  }
  throw new Error(`找不到可填写控件：${selectors.join(', ')}`)
}

async function clickFirst(pageOrLocator, selectors) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(`${selector}:visible`).first()
    if (await locator.count()) {
      await locator.click()
      return
    }
  }
  throw new Error(`找不到可点击控件：${selectors.join(', ')}`)
}

async function navigateToLossReasonPage(page, config) {
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/admin-api/mes/pro/process-pool/team-leader/loss-reasons/page'),
    { timeout: 30000 }
  )
  await page.goto(`${config.frontendUrl}${TEAM_LEADER_ROUTE}`, { waitUntil: 'domcontentloaded' })
  await page.locator('[data-production-leader-workbench-page]').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-team-leader-loss-reason-tab]').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-loss-reason-standard-list]').waitFor({ state: 'visible', timeout: 30000 })
  const response = await responsePromise
  assert.equal(response.ok(), true, `损耗原因标准列表 HTTP ${response.status()}`)
  const payload = await response.json()
  assertBusinessOk(payload, '损耗原因标准列表')
  return dataOf(payload)
}

async function visibleRowTexts(page) {
  const rows = page.locator('[data-loss-reason-standard-list] .el-table__body-wrapper tbody tr:visible')
  await rows.first().waitFor({ state: 'visible', timeout: 30000 })
  return rows.evaluateAll((items) => items.map((item) => item.innerText.replace(/\s+/g, ' ').trim()))
}

function assertAuthorizedRows(rowTexts, fixture) {
  const routeProcesses = fixture.routeProcesses
  const expectedA = String(routeProcesses.routeProcessAId)
  const expectedB = String(routeProcesses.routeProcessBId)
  const unauthorized = String(routeProcesses.unauthorizedRouteProcessId)
  const joined = rowTexts.join('\n')
  assert.ok(joined.includes('ACD04 Authorized Route'), '生产组长页面未显示授权工艺路线')
  assert.ok(joined.includes('ACD04 Route Process One'), '生产组长页面未显示授权工序 A')
  assert.ok(joined.includes('ACD04 Route Process Two'), '生产组长页面未显示授权工序 B')
  assert.equal(joined.includes('ACD04 Unauthorized Route'), false, '生产组长页面错误显示未授权路线')
  assert.equal(joined.includes('ACD04 Unauthorized Process'), false, '生产组长页面错误显示未授权工序')
  return { expectedRouteProcessIds: [Number(expectedA), Number(expectedB)], unauthorizedRouteProcessId: Number(unauthorized) }
}

async function routeProcessRowIndex(page, processText) {
  const rowTexts = await visibleRowTexts(page)
  const index = rowTexts.findIndex((text) => text.includes(processText))
  assert.notEqual(index, -1, `找不到工序行：${processText}`)
  return index
}

function operationPanelAt(page, rowIndex) {
  return page.locator('[data-loss-reason-operation-panel]:visible').nth(rowIndex)
}

async function createReason(page, rowIndex, code, name) {
  const panel = operationPanelAt(page, rowIndex)
  await panel.getByRole('button', { name: '新增损耗原因' }).click()
  await page.locator('[data-loss-reason-edit-dialog]').waitFor({ state: 'visible', timeout: 20000 })
  await fillFirst(page.locator('[data-loss-reason-edit-dialog]'), ['input[placeholder="请输入损耗原因编码"]'], code)
  await fillFirst(page.locator('[data-loss-reason-edit-dialog]'), ['input[placeholder="请输入损耗原因名称"]'], name)
  await fillFirst(page.locator('[data-loss-reason-edit-dialog]'), ['textarea[placeholder="记录新增、修改或删除原因"]'], 'AC-D04 UI shared create verification')
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/admin-api/mes/pro/process-pool/team-leader/loss-reasons')
      && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: '保存损耗原因' }).click()
  const response = await responsePromise
  assert.equal(response.ok(), true, `新增损耗原因 HTTP ${response.status()}`)
  const payload = await response.json()
  assertBusinessOk(payload, '新增损耗原因')
  await page.locator('[data-loss-reason-edit-dialog]').waitFor({ state: 'hidden', timeout: 20000 })
  await page.getByText(code).waitFor({ state: 'visible', timeout: 30000 })
  return Number(dataOf(payload))
}

async function updateNewestReasonInRow(page, rowIndex, newName) {
  const panel = operationPanelAt(page, rowIndex)
  await panel.getByRole('button', { name: '修改损耗原因' }).last().click()
  await page.locator('[data-loss-reason-edit-dialog]').waitFor({ state: 'visible', timeout: 20000 })
  const dialog = page.locator('[data-loss-reason-edit-dialog]')
  await fillFirst(dialog, ['input[placeholder="请输入损耗原因名称"]'], newName)
  await fillFirst(dialog, ['textarea[placeholder="记录新增、修改或删除原因"]'], 'AC-D04 UI shared update verification')
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/admin-api/mes/pro/process-pool/team-leader/loss-reasons/')
      && response.request().method() === 'PUT',
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: '保存损耗原因' }).click()
  const response = await responsePromise
  assert.equal(response.ok(), true, `修改损耗原因 HTTP ${response.status()}`)
  const payload = await response.json()
  assertBusinessOk(payload, '修改损耗原因')
  await page.locator('[data-loss-reason-edit-dialog]').waitFor({ state: 'hidden', timeout: 20000 })
  await page.getByText(newName).waitFor({ state: 'visible', timeout: 30000 })
}

async function deleteNewestReasonInRow(page, rowIndex, code, updatedName) {
  const panel = operationPanelAt(page, rowIndex)
  await panel.getByRole('button', { name: '删除损耗原因' }).last().click()
  await page.getByText(`确认删除损耗原因「${code} / ${updatedName}」？`, { exact: false })
    .waitFor({ state: 'visible', timeout: 20000 })
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/admin-api/mes/pro/process-pool/team-leader/loss-reasons/')
      && response.request().method() === 'DELETE',
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: /确定|确认/ }).click()
  const response = await responsePromise
  assert.equal(response.ok(), true, `删除损耗原因 HTTP ${response.status()}`)
  const payload = await response.json()
  assertBusinessOk(payload, '删除损耗原因')
  await page.getByText(`${code} / ${updatedName}（停用）`).waitFor({ state: 'visible', timeout: 30000 })
}

async function loginAndOpen(browser, config, username, password, targetErrors, pageErrors) {
  const context = await browser.newContext({ ignoreHTTPSErrors: true })
  const page = await context.newPage()
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('response', (response) => {
    const url = response.url()
    if (url.includes('/admin-api/mes/pro/process-pool/team-leader/loss-reasons') && !response.ok()) {
      targetErrors.push(`${response.request().method()} ${url} -> HTTP ${response.status()}`)
    }
  })
  await login(page, config, username, password)
  const rows = await navigateToLossReasonPage(page, config)
  return { context, page, rows }
}

async function main() {
  const fixtureSummary = readJson(FIXTURE_FILE)
  const fixture = fixtureSummary.fixture
  const password = requirePassword()
  const { chromium } = requirePlaywright()
  const { launchOptions, browserExecutablePath } = resolveChromiumLaunchOptions()
  const config = {
    frontendUrl: fixture.frontendUrl,
    backendUrl: fixture.backendUrl,
    tenantId: fixture.tenantId,
    tenantName: TENANT_NAME
  }
  const targetErrors = []
  const pageErrors = []
  const evidence = {
    taskId: TASK_ID,
    generatedAt: new Date().toISOString(),
    status: 'FAIL',
    frontendUrl: config.frontendUrl,
    backendUrl: config.backendUrl,
    tenantId: config.tenantId,
    tenantName: config.tenantName,
    browserExecutablePath,
    checks: []
  }

  let browser
  try {
    await assertHttpOk(`${config.backendUrl}/actuator/health`, '后端 health')
    await assertHttpOk(`${config.frontendUrl}/`, '前端入口')
    evidence.checks.push({ name: 'runtime frontend/backend are reachable', status: 'PASS' })

    browser = await chromium.launch(launchOptions)

    const leaderA = await loginAndOpen(browser, config, fixture.users.leaderA.username, password, targetErrors, pageErrors)
    const rowScope = assertAuthorizedRows(await visibleRowTexts(leaderA.page), fixture)
    evidence.checks.push({
      name: 'leader A sees only route-start authorized process rows',
      status: 'PASS',
      ...rowScope
    })

    const rowIndexA = await routeProcessRowIndex(leaderA.page, 'ACD04 Route Process One')
    const createdReasonId = await createReason(leaderA.page, rowIndexA, UNIQUE_CODE, CREATED_NAME)
    evidence.checks.push({
      name: 'leader A creates a loss reason through the operation panel',
      status: 'PASS',
      routeProcessId: fixture.routeProcesses.routeProcessAId,
      reasonId: createdReasonId,
      reasonCode: UNIQUE_CODE
    })
    await leaderA.context.close()

    const leaderB = await loginAndOpen(browser, config, fixture.users.leaderB.username, password, targetErrors, pageErrors)
    await leaderB.page.getByText(UNIQUE_CODE).waitFor({ state: 'visible', timeout: 30000 })
    await leaderB.page.getByText(CREATED_NAME).waitFor({ state: 'visible', timeout: 30000 })
    const rowIndexB = await routeProcessRowIndex(leaderB.page, 'ACD04 Route Process One')
    await updateNewestReasonInRow(leaderB.page, rowIndexB, UPDATED_NAME)
    evidence.checks.push({
      name: 'leader B sees and updates leader A shared loss reason',
      status: 'PASS',
      reasonId: createdReasonId,
      updatedName: UPDATED_NAME
    })
    await leaderB.context.close()

    const leaderAAfterUpdate = await loginAndOpen(browser, config, fixture.users.leaderA.username, password, targetErrors, pageErrors)
    await leaderAAfterUpdate.page.getByText(UPDATED_NAME).waitFor({ state: 'visible', timeout: 30000 })
    const rowIndexAAfterUpdate = await routeProcessRowIndex(leaderAAfterUpdate.page, 'ACD04 Route Process One')
    await deleteNewestReasonInRow(leaderAAfterUpdate.page, rowIndexAAfterUpdate, UNIQUE_CODE, UPDATED_NAME)
    evidence.checks.push({
      name: 'leader A sees leader B update and deletes the shared reason',
      status: 'PASS',
      reasonId: createdReasonId
    })
    await leaderAAfterUpdate.context.close()

    const leaderBAfterDelete = await loginAndOpen(browser, config, fixture.users.leaderB.username, password, targetErrors, pageErrors)
    await leaderBAfterDelete.page.getByText(`${UNIQUE_CODE} / ${UPDATED_NAME}（停用）`).waitFor({
      state: 'visible',
      timeout: 30000
    })
    evidence.checks.push({
      name: 'leader B sees deleted reason as disabled after leader A delete',
      status: 'PASS',
      reasonId: createdReasonId
    })
    await leaderBAfterDelete.context.close()

    assert.deepEqual(targetErrors, [], `目标损耗原因接口存在 HTTP 错误：${targetErrors.join('; ')}`)
    assert.deepEqual(pageErrors, [], `页面存在未处理异常：${pageErrors.join('; ')}`)
    evidence.checks.push({ name: 'target loss-reason page has no HTTP/page errors', status: 'PASS' })
    evidence.status = 'PASS'
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    throw error
  } finally {
    evidence.targetErrors = targetErrors
    evidence.pageErrors = pageErrors
    if (browser) {
      await browser.close()
    }
    writeJson(OUTPUT_FILE, evidence)
  }

  console.log(JSON.stringify({
    status: evidence.status,
    output: OUTPUT_FILE,
    checks: evidence.checks.map((check) => check.name)
  }, null, 2))
}

main().catch((error) => {
  console.error(JSON.stringify({
    status: 'FAIL',
    output: OUTPUT_FILE,
    error: error.message
  }, null, 2))
  process.exitCode = 1
})
