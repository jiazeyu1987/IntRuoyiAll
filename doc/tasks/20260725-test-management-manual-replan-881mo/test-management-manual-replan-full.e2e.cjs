const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { createRequire } = require('node:module')

const taskDir = __dirname
const workspaceRoot = path.resolve(taskDir, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')
const artifactsDir = path.join(taskDir, 'artifacts')
const summaryPath = path.join(artifactsDir, 'test-management-manual-replan-summary.json')
const generatedManualScript = path.join(taskDir, 'manual-replan-current.generated.cjs')
const priorManualScript = path.join(
  workspaceRoot,
  'doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs'
)

const caseName = '排产工单手动重排 881MO093613/881MO093615'
const targetSources = ['881MO093613', '881MO093615']
const methodItems = [
  '在排产工单页签中筛选并选择来源生产工单号为 881MO093613、881MO093615 的两个排产工单。',
  '点击手动重排，在弹框中选择开始重排，确认应用重排。'
]
const checkpoints = [
  {
    name: '重排成功',
    expectedText: '点击确认应用重排后，页面提示应用重排成功，后端 apply 请求业务码为 0。'
  },
  {
    name: '仅目标工单产品编号变橙色',
    expectedText:
      '只有来源生产工单号为 881MO093613、881MO093615 的两个排产工单产品编号具有橙色已排产样式。'
  },
  {
    name: '最近一次成功排产时间更新',
    expectedText: '最近一次成功排产时间更新为本次手动重排执行时间，operationType 为 REPLAN_APPLY。'
  },
  {
    name: '生产排产甘特图仅包含目标工单',
    expectedText: '生产排产页签的甘特图接口和折叠后 UI 有且仅有 881MO093613、881MO093615 两个工单。'
  }
]

function parseDotEnv(filePath) {
  const values = {}
  const source = fs.readFileSync(filePath, 'utf8')
  for (const line of source.split(/\r?\n/)) {
    const match = line.match(/^\s*([^#=\s][^=]*?)\s*=\s*(.*)\s*$/)
    if (!match) continue
    const key = match[1].trim()
    let value = match[2].trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    values[key] = value
  }
  return values
}

const env = parseDotEnv(path.join(frontendRoot, '.env'))
const config = {
  mode: process.env.TEST_MANAGEMENT_REPLAN_MODE || 'full',
  baseUrl: (process.env.TEST_MANAGEMENT_REPLAN_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  backendUrl: (process.env.TEST_MANAGEMENT_REPLAN_BACKEND_URL || 'http://127.0.0.1:48081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.TEST_MANAGEMENT_REPLAN_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: process.env.TEST_MANAGEMENT_REPLAN_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: process.env.TEST_MANAGEMENT_REPLAN_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD,
  headed: process.env.TEST_MANAGEMENT_REPLAN_HEADED === '1'
}

for (const [key, value] of Object.entries({
  baseUrl: config.baseUrl,
  backendUrl: config.backendUrl,
  tenant: config.tenant,
  username: config.username,
  password: config.password
})) {
  assert.ok(value, `missing config ${key}`)
}

function unwrap(payload) {
  return payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')
    ? payload.data
    : payload
}

function menuContains(menus, predicate) {
  const queue = Array.isArray(menus) ? [...menus] : []
  while (queue.length > 0) {
    const menu = queue.shift()
    if (predicate(menu)) return true
    if (Array.isArray(menu.children)) queue.push(...menu.children)
  }
  return false
}

async function settle(page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function firstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`No visible element found for ${label}`)
}

async function fillFirstVisible(locator, value, label) {
  const item = await firstVisible(locator, label)
  await item.fill(value)
  return item
}

async function selectTenant(page, form) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) === 0 || !(await tenantInput.isVisible())) {
    return
  }
  await tenantInput.click()
  await tenantInput.fill(config.tenant)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  if (!page.url().includes('/login')) return { permissions: [], menus: [] }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form)
  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"], input[placeholder="请输入密码"]'), config.password, 'password')

    await form.getByRole('button', { name: /^登录$/ }).click()
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  return apiGetJson(page, '/system/auth/get-permission-info')
}

async function readWebStorageValue(page, key) {
  return page.evaluate((cacheKey) => {
    const raw = window.localStorage.getItem(cacheKey)
    if (!raw) return null
    try {
      const wrapper = JSON.parse(raw)
      if (wrapper && Object.prototype.hasOwnProperty.call(wrapper, 'v')) {
        return JSON.parse(wrapper.v)
      }
      return wrapper
    } catch {
      return raw
    }
  }, key)
}

async function authHeaders(page) {
  const accessToken = await readWebStorageValue(page, 'ACCESS_TOKEN')
  const tenantId = await readWebStorageValue(page, 'tenantId')
  assert.ok(accessToken, 'logged-in context is missing ACCESS_TOKEN')
  assert.ok(tenantId, 'logged-in context is missing tenantId')
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
}

async function apiGetJson(page, url, params = {}) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  }
  const response = await page.request.get(
    `${config.backendUrl}/admin-api${url}${search.size ? `?${search.toString()}` : ''}`,
    { headers: await authHeaders(page) }
  )
  const body = await response.json()
  assert.equal(response.status(), 200, `${url} HTTP ${response.status()}`)
  assert.equal(body.code, 0, `${url} business error: ${body.msg || JSON.stringify(body)}`)
  return body.data
}

async function openTestManagement(page) {
  const permissionInfo = await login(page)
  const permissions = Array.isArray(permissionInfo.permissions) ? permissionInfo.permissions : []
  const menus = Array.isArray(permissionInfo.menus) ? permissionInfo.menus : []
  assert.ok(
    permissions.includes('system:codex-test:query') || permissions.includes('*:*:*'),
    'permission response must include system:codex-test:query'
  )
  assert.ok(
    menuContains(
      menus,
      (menu) =>
        menu?.name === '测试管理' ||
        menu?.path === 'codex-test-management' ||
        menu?.component === 'system/codex-test-management/index'
    ),
    'dynamic menu response must include 测试管理 menu'
  )

  await page.getByText('系统管理', { exact: true }).first().click()
  const testMenu = page.getByText('测试管理', { exact: true }).first()
  await testMenu.waitFor({ state: 'visible', timeout: 30000 })
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/codex-test-case/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await testMenu.click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `case page business error: ${body.msg || body.code}`)
  await page.locator('text=自然语言测试方法').waitFor({ state: 'visible', timeout: 30000 })
  return body.data
}

async function searchCase(page) {
  const nameInput = page.locator('input[placeholder="输入测试项名称"]').first()
  await nameInput.waitFor({ state: 'visible', timeout: 30000 })
  await nameInput.fill(caseName)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/codex-test-case/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: /查询/ }).first().click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `case search business error: ${body.msg || body.code}`)
  const rows = body.data?.list || []
  return rows.find((row) => row.name === caseName) || null
}

async function ensureSwitch(dialog, index, expectedChecked) {
  const item = dialog.locator('.el-form-item').filter({ hasText: '执行控制' }).first()
  const toggle = item.locator('.el-switch').nth(index)
  await toggle.waitFor({ state: 'visible', timeout: 30000 })
  const checked = await toggle.evaluate((node) => node.classList.contains('is-checked'))
  if (checked !== expectedChecked) await toggle.click()
}

async function normalizeCheckpoints(dialog) {
  let rows = dialog.locator('.codex-test-checkpoint')
  let count = await rows.count()
  while (count < checkpoints.length) {
    await dialog.getByRole('button', { name: /新增检查点/ }).click()
    rows = dialog.locator('.codex-test-checkpoint')
    count = await rows.count()
  }
  while (count > checkpoints.length) {
    await rows.nth(count - 1).getByRole('button', { name: /删除/ }).click()
    rows = dialog.locator('.codex-test-checkpoint')
    count = await rows.count()
  }
}

async function fillCaseDialog(page, existingCase) {
  if (existingCase) {
    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: caseName }).first()
    await row.waitFor({ state: 'visible', timeout: 30000 })
    await row.getByRole('button', { name: /修改/ }).click()
  } else {
    await page.getByRole('button', { name: /新增测试项|新增/ }).first().click()
  }

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: existingCase ? '修改测试项' : '新增测试项' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[placeholder="例如：排产手动重排工单校验"]').fill(caseName)
  await dialog
    .locator('textarea[placeholder="按行录入测试方法，例如：a. 打开排产工单页"]')
    .fill(methodItems.join('\n'))
  await dialog
    .locator('textarea[placeholder="用户手写数据，例如：来源生产工单号=881MO093613,881MO093615"]')
    .fill('来源生产工单号=881MO093613,881MO093615')

  await dialog.locator('.el-radio-button').filter({ hasText: '顺序执行' }).first().click()
  await ensureSwitch(dialog, 0, false)
  await ensureSwitch(dialog, 1, true)
  await normalizeCheckpoints(dialog)

  const rows = dialog.locator('.codex-test-checkpoint')
  for (let index = 0; index < checkpoints.length; index += 1) {
    const row = rows.nth(index)
    const checkpoint = checkpoints[index]
    await row.locator('input').nth(1).fill(checkpoint.name)
    await row.locator('textarea').first().fill(checkpoint.expectedText)
  }

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(
        existingCase ? '/admin-api/system/codex-test-case/update' : '/admin-api/system/codex-test-case/create'
      ) && ['POST', 'PUT'].includes(response.request().method()),
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: /^保存$/ }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(response.status(), 200, `save case HTTP ${response.status()}`)
  assert.equal(body.code, 0, `save case business error: ${body.msg || JSON.stringify(body)}`)
  await page.locator('.el-message--success').filter({ hasText: '保存成功' }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
}

async function assertSavedCase(page) {
  const saved = await searchCase(page)
  assert.ok(saved, `saved test case must be searchable by exact name: ${caseName}`)
  assert.equal(saved.checkpointCount, checkpoints.length, 'saved test case must expose four checkpoints')
  const detail = await apiGetJson(page, '/system/codex-test-case/get', { id: saved.id })
  assert.equal(detail.name, caseName)
  assert.equal(detail.defaultExecutionMode, 'SEQUENTIAL')
  assert.equal(detail.parallelSafe, false)
  assert.equal(detail.status, 'ENABLE')
  assert.equal(detail.methodText, methodItems.join('\n'))
  assert.ok(!detail.methodText.includes('完成后核验'), 'method text must only contain操作步骤')
  assert.equal(detail.testDataText, '来源生产工单号=881MO093613,881MO093615')
  assert.deepEqual(
    (detail.checkpoints || []).map((item) => item.name),
    checkpoints.map((item) => item.name)
  )
  return detail
}

function prepareManualReplanScript() {
  assert.ok(fs.existsSync(priorManualScript), `missing prior manual replan E2E source: ${priorManualScript}`)
  const source = fs.readFileSync(priorManualScript, 'utf8')
  const targetOutputLiteral =
    "'doc/tasks/20260725-test-management-manual-replan-881mo/artifacts/manual-replan'"
    let patched = source.replace(
    "'output/playwright/verify-manual-reschedule-881mo-20260724-repair'",
    targetOutputLiteral
  )
  patched = patched.replace(
    "const { chromium } = require('playwright')",
    "const { createRequire } = require('node:module')"
  )
  patched = patched.replace(
    "const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')",
    "const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')\nconst frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))\nconst { chromium } = frontendRequire('playwright')"
  )
  assert.notEqual(patched, source, 'manual replan E2E source must contain the known output path literal')
  fs.writeFileSync(generatedManualScript, patched, 'utf8')
}

function runManualReplan() {
  prepareManualReplanScript()
  const result = spawnSync(process.execPath, [generatedManualScript], {
    cwd: workspaceRoot,
    env: {
      ...process.env,
      MANUAL_REPLAN_BASE_URL: config.baseUrl,
      MANUAL_REPLAN_BACKEND_URL: config.backendUrl,
      MANUAL_REPLAN_TENANT: config.tenant,
      MANUAL_REPLAN_USERNAME: config.username,
      MANUAL_REPLAN_PASSWORD: config.password
    },
    encoding: 'utf8',
    timeout: 240000
  })
  fs.writeFileSync(path.join(artifactsDir, 'manual-replan-stdout.log'), result.stdout || '', 'utf8')
  fs.writeFileSync(path.join(artifactsDir, 'manual-replan-stderr.log'), result.stderr || '', 'utf8')
  if (result.status !== 0) {
    throw new Error(`manual replan E2E failed with status ${result.status}: ${result.stderr || result.stdout}`)
  }
  const reportPath = path.join(artifactsDir, 'manual-replan', 'repair-verification-report.json')
  assert.ok(fs.existsSync(reportPath), `manual replan report missing: ${reportPath}`)
  return JSON.parse(fs.readFileSync(reportPath, 'utf8'))
}

async function run() {
  fs.mkdirSync(artifactsDir, { recursive: true })
  const parsedBaseUrl = new URL(config.baseUrl)
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(parsedBaseUrl.hostname),
    `E2E must use local frontend, got ${config.baseUrl}`
  )

  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage', '--no-sandbox'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const summary = {
    status: 'RUNNING',
    mode: config.mode,
    caseName,
    targetSources,
    tenant: config.tenant,
    username: config.username,
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    checkedAt: new Date().toISOString()
  }

  try {
    await openTestManagement(page)
    const beforeCase = await searchCase(page)
    summary.caseExistedBefore = Boolean(beforeCase)
    summary.beforeCaseId = beforeCase?.id || null

    if (config.mode === 'assert-existing') {
      assert.ok(beforeCase, `RED expected: test case is not present in Test Management: ${caseName}`)
      summary.status = 'PASS'
      summary.caseDetail = beforeCase
      fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
      return
    }

    await fillCaseDialog(page, beforeCase)
    const savedDetail = await assertSavedCase(page)
    await page.screenshot({ path: path.join(artifactsDir, 'test-management-case-saved.png'), fullPage: true })
    summary.savedCase = {
      id: savedDetail.id,
      name: savedDetail.name,
      checkpointCount: savedDetail.checkpoints?.length || 0,
      defaultExecutionMode: savedDetail.defaultExecutionMode,
      parallelSafe: savedDetail.parallelSafe,
      status: savedDetail.status
    }

    await context.close()
    await browser.close()

    const manualReport = runManualReplan()
    summary.status = 'PASS'
    summary.manualReplan = {
      status: manualReport.status,
      targetScheduleOrders: manualReport.targetScheduleOrders,
      applyRequest: manualReport.applyRequest,
      goals: manualReport.goals,
      latestSuccess: manualReport.latestSuccess,
      ganttScope: manualReport.ganttScope
    }
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    console.log(`PASS: ${caseName}`)
  } catch (error) {
    summary.status = 'FAIL'
    summary.error = error.stack || error.message || String(error)
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    await page.screenshot({ path: path.join(artifactsDir, 'failure.png'), fullPage: true }).catch(() => undefined)
    throw error
  } finally {
    if (browser.isConnected()) await browser.close().catch(() => undefined)
  }
}

run().catch((error) => {
  console.error(error.stack || error.message || error)
  process.exit(1)
})
