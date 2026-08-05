const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260805-production-personnel-management'
const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const FRONTEND_ROOT = path.join(WORKSPACE_ROOT, 'IntRuoyiFronted')
const RESULT_DIR = path.join(FRONTEND_ROOT, 'test-results', 'production-personnel-management-real')
const EVIDENCE_FILE = path.join(WORKSPACE_ROOT, 'doc', 'tasks', TASK_ID, 'e2e-production-personnel-evidence.md')
const TEAM_LEADER_ROUTE = '/mes/pro/process-pool/team-leader'
const REQUIRED_ENV = [
  'PPM_FRONTEND_URL',
  'PPM_BACKEND_URL',
  'PPM_TENANT',
  'PPM_USERNAME',
  'PPM_PASSWORD',
  'PPM_FORMAL_SEARCH_KEYWORD'
]

function envValue(key) {
  return String(process.env[key] || '').trim()
}

function sanitizeUrl(value) {
  return value.replace(/\/+$/, '')
}

function collectConfig() {
  const timestamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
  const config = {
    frontendUrl: sanitizeUrl(envValue('PPM_FRONTEND_URL')),
    backendUrl: sanitizeUrl(envValue('PPM_BACKEND_URL')),
    tenant: envValue('PPM_TENANT'),
    username: envValue('PPM_USERNAME'),
    password: envValue('PPM_PASSWORD'),
    formalSearchKeyword: envValue('PPM_FORMAL_SEARCH_KEYWORD'),
    dataPrefix: envValue('PPM_DATA_PREFIX') || `PPM-${timestamp}`,
    headed: envValue('PPM_HEADED') === '1'
  }
  config.tempDisplayName = `${config.dataPrefix}-临时工`
  config.formalDisplayName = `${config.dataPrefix}-正式工`
  config.missing = collectMissingConfig(config)
  return config
}

function collectMissingConfig(config) {
  const missing = []
  for (const key of REQUIRED_ENV) {
    if (!envValue(key)) {
      missing.push(`${key} is required`)
    }
  }
  if (config.tenant && /芋道源码|prod|production|yudao/i.test(config.tenant)) {
    missing.push('PPM_TENANT must be a writable test tenant, not the admin or production baseline')
  }
  if (config.username && config.username.toLowerCase() === 'admin') {
    missing.push('PPM_USERNAME must not be the admin baseline account')
  }
  if (config.frontendUrl && config.backendUrl && !isAllowedIntMainRuntimePair(config.frontendUrl, config.backendUrl)) {
    missing.push('PPM_FRONTEND_URL and PPM_BACKEND_URL must be a paired int_main runtime: 8081/48081 or slot 1..19')
  }
  return missing
}

function isAllowedIntMainRuntimePair(frontendUrl, backendUrl) {
  const frontendPort = Number(new URL(frontendUrl).port)
  const backendPort = Number(new URL(backendUrl).port)
  if (!Number.isInteger(frontendPort) || !Number.isInteger(backendPort)) {
    return false
  }
  const slot = frontendPort - 8081
  return slot >= 0 && slot <= 19 && backendPort === 48081 + slot
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error('Playwright dependency is missing; run pnpm install in IntRuoyiFronted.')
    blocked.blocked = true
    throw blocked
  }
}

function resolveLaunchOptions(config) {
  const executablePath = envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
  const options = { headless: !config.headed }
  if (!executablePath) {
    return options
  }
  if (!fs.existsSync(executablePath)) {
    throw new Error(`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH does not exist: ${executablePath}`)
  }
  return { ...options, executablePath }
}

function redactedConfig(config) {
  const copy = { ...config }
  delete copy.password
  delete copy.missing
  return copy
}

function writeEvidence(result) {
  ensureDir(RESULT_DIR)
  ensureDir(path.dirname(EVIDENCE_FILE))
  fs.writeFileSync(
    path.join(RESULT_DIR, 'result.json'),
    `${JSON.stringify(result, null, 2)}\n`,
    'utf8'
  )
  const lines = [
    '# 生产人员档案真实 E2E 证据',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- Generated At: \`${new Date().toISOString()}\``,
    `- Status: \`${result.status}\``,
    `- Frontend: \`${result.config?.frontendUrl || '--'}\``,
    `- Backend: \`${result.config?.backendUrl || '--'}\``,
    `- Tenant: \`${result.config?.tenant || '--'}\``,
    `- User: \`${result.config?.username || '--'}\``,
    `- Data Prefix: \`${result.config?.dataPrefix || '--'}\``,
    '',
    '## BDD',
    '',
    '- BDD: 生产人员档案管理真实页面 -> Given 测试生产组长登录真实前端 When 通过生产人员档案 tab 管理正式工和临时工 Then 页面、接口和审计均只作用于当前组长关联员工。',
    '- BDD: 生产填写候选范围 -> Given 临时工已绑定当前组长可切换工序 When 禁用该临时工 Then runtime-config 不再返回该员工候选。',
    ''
  ]
  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `pnpm e2e:production-personnel-management:real` -> PASS')
    for (const step of result.steps || []) {
      lines.push(`- Step: ${step}`)
    }
    lines.push(`- Screenshot: \`${result.screenshot || '--'}\``)
    lines.push('- Password handling: login and signature passwords were injected/generated at runtime and are not written to artifacts.')
  } else if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED', '')
    lines.push(`- E2E: \`pnpm e2e:production-personnel-management:real\` -> BLOCKED, ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- Missing: ${item}`)
    }
  } else {
    lines.push('## RED', '')
    lines.push(`- RED: \`pnpm e2e:production-personnel-management:real\` -> FAIL, ${result.reason}`)
  }
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

async function waitForApiJson(page, fragment, method, trigger) {
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes(fragment) && response.request().method() === method,
  { timeout: 20000 })
  await trigger()
  const response = await responsePromise
  const body = await response.json().catch(() => ({}))
  return { response, body }
}

async function fillFormItem(scope, label, value) {
  const item = scope.locator('.el-form-item', { hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 15000 })
  const input = item.locator('input:visible').first()
  await input.fill(String(value))
}

async function selectFormItem(scope, label, optionText) {
  const item = scope.locator('.el-form-item', { hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 15000 })
  await item.locator('.el-select, .el-select__wrapper').first().click()
  await scope.page().locator('.el-select-dropdown__item:visible', { hasText: optionText }).last().click()
}

async function login(page, config) {
  await page.goto(`${config.frontendUrl}/login?redirect=/index`, { waitUntil: 'networkidle' })
  const form = page.locator('.login-form').first()
  await form.waitFor({ state: 'visible', timeout: 20000 })
  await selectTenant(page, form, config.tenant)
  await fillFirstVisible(form, [
    'input[placeholder="请输入用户名"]',
    'input.el-input__inner:not([type="password"]):not([role="combobox"])'
  ], config.username)
  await fillFirstVisible(form, [
    'input[type="password"]',
    'input[placeholder="请输入密码"]'
  ], config.password)
  const { body } = await waitForApiJson(page, '/system/auth/login', 'POST', async () => {
    await form.getByRole('button', { name: '登录' }).click()
  })
  assert.equal(body.code, 0, `登录失败：${body.msg || body.message || 'unknown'}`)
  await page.waitForResponse((response) =>
    response.url().includes('/system/auth/get-permission-info') && response.status() === 200,
  { timeout: 20000 })
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 20000 })
}

async function selectTenant(page, form, tenantName) {
  const tenantInput = form.locator('input[placeholder="请输入租户名称"], .el-select__input[role="combobox"]').first()
  if (!(await tenantInput.isVisible().catch(() => false))) {
    return
  }
  await tenantInput.click()
  await tenantInput.fill(tenantName)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: tenantName }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function fillFirstVisible(scope, selectors, value) {
  for (const selector of selectors) {
    const input = scope.locator(selector).first()
    if (await input.isVisible().catch(() => false)) {
      await input.fill(String(value))
      return
    }
  }
  throw new Error(`No visible input found for selectors: ${selectors.join(', ')}`)
}

async function readAuthContext(page) {
  const snapshot = await page.evaluate(() => {
    const values = {}
    for (const key of Object.keys(localStorage)) {
      values[key] = localStorage.getItem(key)
    }
    return values
  })
  const tokenEntry = Object.entries(snapshot).find(([key]) => /token/i.test(key))
  const tenantEntry = Object.entries(snapshot).find(([key]) => /tenantId/i.test(key))
  const token = parseStoredValue(tokenEntry?.[1]).replace(/^Bearer\s+/i, '')
  const tenantId = parseStoredValue(tenantEntry?.[1])
  assert.ok(token, '登录后必须能读取访问 token 用于只读核验。')
  return { token, tenantId }
}

function parseStoredValue(rawValue) {
  let value = String(rawValue || '').trim()
  for (let index = 0; index < 3; index += 1) {
    try {
      const parsed = JSON.parse(value)
      if (typeof parsed === 'string') {
        value = parsed
      } else if (parsed && typeof parsed === 'object') {
        value = String(parsed.v || parsed.value || parsed.accessToken || parsed.token || parsed.tenantId || value)
      }
    } catch {
      break
    }
  }
  return value
}

async function fetchApi(config, auth, apiPath) {
  const headers = { Authorization: `Bearer ${auth.token}` }
  if (auth.tenantId) {
    headers['tenant-id'] = String(auth.tenantId)
  }
  const response = await fetch(`${config.backendUrl}${apiPath}`, { headers })
  const body = await response.json().catch(() => ({}))
  assert.equal(response.ok, true, `${apiPath} HTTP ${response.status}`)
  assert.equal(body.code, 0, `${apiPath} 业务失败：${body.msg || body.message || 'unknown'}`)
  return body.data
}

async function openPersonnelPage(page, config) {
  await page.goto(`${config.frontendUrl}${TEAM_LEADER_ROUTE}`, { waitUntil: 'networkidle' })
  await page.locator('[data-team-leader-production-personnel-tab]').waitFor({ state: 'visible', timeout: 25000 })
  await page.locator('[data-team-leader-production-personnel-list]').waitFor({ state: 'visible', timeout: 25000 })
}

async function linkFormalEmployeeViaPage(page, config, steps) {
  const select = page.locator('[data-team-leader-formal-employee-select]').first()
  await select.click()
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/employee-profile/formal-candidates') && response.request().method() === 'GET',
  { timeout: 20000 })
  const input = select.locator('input.el-select__input, input:visible').first()
  await input.fill(config.formalSearchKeyword)
  const response = await responsePromise
  const body = await response.json().catch(() => ({}))
  assert.equal(body.code, 0, `正式工候选搜索失败：${body.msg || body.message || 'unknown'}`)
  const candidates = Array.isArray(body.data) ? body.data : []
  if (!candidates.length) {
    const blocked = new Error('正式工受限搜索没有返回候选，无法执行选择正式工新增真实路径。')
    blocked.blocked = true
    throw blocked
  }
  const candidateLabel = String(candidates[0].displayName)
  await page.locator('.el-select-dropdown__item:visible', { hasText: candidateLabel }).first().click()
  const formalCard = page.locator('[data-team-leader-production-personnel-tab] .el-card', { hasText: '搜索选择正式工' }).first()
  await fillFormItem(formalCard, '显示名', config.formalDisplayName)
  const { body: linkBody } = await waitForApiJson(page, '/employee-profile/formal/link', 'POST', async () => {
    await formalCard.getByRole('button', { name: '关联正式工' }).click()
  })
  assert.equal(linkBody.code, 0, `关联正式工失败：${linkBody.msg || linkBody.message || 'unknown'}`)
  await findPersonnelRow(page, config.formalDisplayName).waitFor({ state: 'visible', timeout: 15000 })
  steps.push('正式工通过远程姓名下拉真实选择并关联当前生产组长')
  return Number(linkBody.data)
}

async function createTemporaryEmployeeViaPage(page, config, steps) {
  const form = page.locator('[data-team-leader-temporary-employee-form]').first()
  await fillFormItem(form, '显示名', config.tempDisplayName)
  await fillFormItem(form, '签名密码', `Pwd#${Date.now()}A`)
  const { body } = await waitForApiJson(page, '/employee-profile/temporary/create', 'POST', async () => {
    await form.getByRole('button', { name: '新增临时工' }).click()
  })
  assert.equal(body.code, 0, `新增临时工失败：${body.msg || body.message || 'unknown'}`)
  await findPersonnelRow(page, config.tempDisplayName).waitFor({ state: 'visible', timeout: 15000 })
  steps.push('临时工通过真实页面录入显示名和电子签名密码')
  return Number(body.data)
}

async function assertDuplicateTemporaryWorkerRejected(page, config, steps) {
  const form = page.locator('[data-team-leader-temporary-employee-form]').first()
  await fillFormItem(form, '显示名', config.tempDisplayName)
  await fillFormItem(form, '签名密码', `Pwd#${Date.now()}B`)
  const { body } = await waitForApiJson(page, '/employee-profile/temporary/create', 'POST', async () => {
    await form.getByRole('button', { name: '新增临时工' }).click()
  })
  assert.notEqual(body.code, 0, '重复临时工显示名必须被后端拒绝。')
  assert.match(String(body.msg || body.message || ''), /重名|重复|后缀|已存在|显示名/,
    '重复显示名错误必须提示用户加后缀或修改显示名。')
  steps.push('同一生产组长重复显示名被拒绝并返回可理解提示')
}

async function bindTemporaryWorkerToSwitchableProcessViaPage(page, processId, employeeProfileId, steps) {
  const form = page.locator('[data-team-leader-employee-config]').first()
  await fillFormItem(form, '工序ID', processId)
  await fillFormItem(form, '员工档案ID', employeeProfileId)
  const { body } = await waitForApiJson(page, '/process-employee-binding/save', 'POST', async () => {
    await form.getByRole('button', { name: '绑定工序员工' }).click()
  })
  assert.equal(body.code, 0, `绑定工序员工失败：${body.msg || body.message || 'unknown'}`)
  steps.push('临时工档案通过真实页面绑定到当前组长可切换工序')
}

async function ensureDeviceScopeViaPage(page, process, steps) {
  const deviceId = Number(process.deviceId)
  if (!Number.isFinite(deviceId) || deviceId <= 0) {
    const blocked = new Error('可切换工序缺少真实设备 ID，无法补齐生产填写 runtime-config 设备负责范围。')
    blocked.blocked = true
    throw blocked
  }

  const deviceCard = page.locator('[data-team-leader-device-config]').first()
  const statusForm = deviceCard.locator('form').nth(1)
  await fillFormItem(statusForm, '设备ID', deviceId)
  await selectFormItem(statusForm, '状态', '启用')
  const { body: statusBody } = await waitForApiJson(page, '/team-device/status/update', 'PUT', async () => {
    await statusForm.getByRole('button', { name: '更新状态' }).click()
  })
  assert.equal(statusBody.code, 0, `更新设备状态失败：${statusBody.msg || statusBody.message || 'unknown'}`)

  const relationCard = page.locator('[data-team-leader-process-relation-config]').first()
  const relationForm = relationCard.locator('form').first()
  await fillFormItem(relationForm, '工序ID', process.processId)
  await fillFormItem(relationForm, '设备ID', deviceId)
  const { body: bindingBody } = await waitForApiJson(page, '/process-device-binding/save', 'POST', async () => {
    await relationForm.getByRole('button', { name: '绑定工序设备' }).click()
  })
  assert.equal(bindingBody.code, 0, `绑定工序设备失败：${bindingBody.msg || bindingBody.message || 'unknown'}`)
  steps.push('可切换工序设备通过真实页面启用并绑定到当前组长负责范围')
}

async function resetTemporarySignaturePasswordViaPage(page, displayName, steps) {
  const row = findPersonnelRow(page, displayName)
  await row.getByRole('button', { name: '重置签名密码' }).click()
  const box = page.locator('.el-message-box:visible').last()
  await box.locator('input[type="password"], input:visible').first().fill(`Pwd#${Date.now()}C`)
  const { body } = await waitForApiJson(page, '/temp-signature-password/reset', 'PUT', async () => {
    await box.getByRole('button', { name: '重置' }).click()
  })
  assert.equal(body.code, 0, `重置临时工签名密码失败：${body.msg || body.message || 'unknown'}`)
  steps.push('临时工签名密码通过真实页面重置并复用统一密码入口')
}

async function disableTemporaryWorkerViaPage(page, displayName, steps) {
  const row = findPersonnelRow(page, displayName)
  await row.getByRole('button', { name: '禁用' }).click()
  const box = page.locator('.el-message-box:visible').last()
  const { body } = await waitForApiJson(page, '/employee-profile/status/update', 'PUT', async () => {
    await box.getByRole('button', { name: '禁用' }).click()
  })
  assert.equal(body.code, 0, `禁用员工失败：${body.msg || body.message || 'unknown'}`)
  await row.waitFor({ state: 'hidden', timeout: 15000 }).catch(async () => {
    const text = await row.innerText().catch(() => '')
    assert.ok(!text.includes('可选择'), '禁用后员工不得继续处于可选择状态。')
  })
  steps.push('员工禁用后从未禁用人员列表中移除')
}

async function disableFormalWorkerIfVisible(page, displayName) {
  const row = findPersonnelRow(page, displayName)
  if (!(await row.isVisible().catch(() => false))) {
    return
  }
  await row.getByRole('button', { name: '禁用' }).click()
  const box = page.locator('.el-message-box:visible').last()
  const { body } = await waitForApiJson(page, '/employee-profile/status/update', 'PUT', async () => {
    await box.getByRole('button', { name: '禁用' }).click()
  })
  assert.equal(body.code, 0, `禁用正式工清理失败：${body.msg || body.message || 'unknown'}`)
}

function findPersonnelRow(page, displayName) {
  return page.locator('[data-team-leader-production-personnel-list] .el-table__body-wrapper tbody tr', {
    hasText: displayName
  }).first()
}

async function discoverSwitchableProcess(config, auth) {
  const processes = await fetchApi(config, auth, '/admin-api/mes/pro/feedback/frontline/device-account/processes')
  if (!Array.isArray(processes) || processes.length === 0) {
    const blocked = new Error('当前测试账号没有可切换生产填写工序，无法核验员工卡片候选范围。')
    blocked.blocked = true
    throw blocked
  }
  return processes[0]
}

async function assertRuntimeConfigCandidateScope(config, auth, process, displayName, expectedVisible, steps) {
  const query = new URLSearchParams({
    routeId: String(process.routeId),
    routeProcessId: String(process.routeProcessId),
    processId: String(process.processId)
  })
  const data = await fetchApi(config, auth, `/admin-api/mes/pro/feedback/frontline/device-account/runtime-config?${query.toString()}`)
  const employees = Array.isArray(data.employees) ? data.employees : []
  const found = employees.some((employee) => employee.displayName === displayName || employee.employeeName === displayName)
  assert.equal(found, expectedVisible, expectedVisible
    ? '绑定且未禁用的临时工必须进入生产填写候选。'
    : '禁用后的临时工不得进入生产填写候选。')
  steps.push(expectedVisible
    ? '生产填写 runtime-config 返回关联当前组长且未禁用的临时工候选'
    : '禁用后生产填写 runtime-config 不再返回该临时工候选')
}

async function assertAuditTrailVisible(page, displayName, steps) {
  const table = page.locator('[data-team-leader-personnel-audit-list]').first()
  await table.waitFor({ state: 'visible', timeout: 15000 })
  const text = await table.innerText()
  for (const action of ['CREATE_TEMPORARY_EMPLOYEE', 'RESET_TEMP_SIGNATURE_PASSWORD', 'DISABLE_EMPLOYEE']) {
    assert.ok(text.includes(action), `操作追溯必须包含 ${action}`)
  }
  assert.ok(text.includes(displayName), '操作追溯必须包含任务自有临时工显示名。')
  steps.push('新增、重置密码、禁用操作均在追溯表中可见')
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    writeEvidence({
      status: 'BLOCKED',
      reason: '缺少真实写入型 E2E 前置条件。',
      missing: config.missing,
      config: redactedConfig(config),
      steps: []
    })
    process.exitCode = 2
    return
  }

  const steps = []
  let browser
  try {
    const { chromium } = loadPlaywright()
    browser = await chromium.launch(resolveLaunchOptions(config))
    const context = await browser.newContext({ viewport: { width: 1440, height: 980 } })
    const page = await context.newPage()
    await login(page, config)
    steps.push('测试生产组长通过真实登录页进入系统')
    const auth = await readAuthContext(page)
    const process = await discoverSwitchableProcess(config, auth)
    steps.push(`只读发现可切换工序 routeProcessId=${process.routeProcessId} processId=${process.processId}`)
    await openPersonnelPage(page, config)
    await linkFormalEmployeeViaPage(page, config, steps)
    const tempProfileId = await createTemporaryEmployeeViaPage(page, config, steps)
    await assertDuplicateTemporaryWorkerRejected(page, config, steps)
    await bindTemporaryWorkerToSwitchableProcessViaPage(page, process.processId, tempProfileId, steps)
    await ensureDeviceScopeViaPage(page, process, steps)
    await assertRuntimeConfigCandidateScope(config, auth, process, config.tempDisplayName, true, steps)
    await resetTemporarySignaturePasswordViaPage(page, config.tempDisplayName, steps)
    await disableTemporaryWorkerViaPage(page, config.tempDisplayName, steps)
    await assertRuntimeConfigCandidateScope(config, auth, process, config.tempDisplayName, false, steps)
    await disableFormalWorkerIfVisible(page, config.formalDisplayName)
    await assertAuditTrailVisible(page, config.tempDisplayName, steps)
    ensureDir(RESULT_DIR)
    const screenshot = path.join(RESULT_DIR, 'production-personnel-management-pass.png')
    await page.screenshot({ path: screenshot, fullPage: true })
    writeEvidence({
      status: 'PASS',
      reason: '生产人员档案真实页面验收通过。',
      config: redactedConfig(config),
      steps,
      screenshot
    })
  } catch (error) {
    writeEvidence({
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.message || String(error),
      missing: error.blocked ? [error.message || String(error)] : [],
      config: redactedConfig(config),
      steps,
      error: {
        name: error.name || 'Error',
        message: error.message || String(error),
        stack: error.stack
      }
    })
    process.exitCode = error.blocked ? 2 : 1
  } finally {
    if (browser) {
      await browser.close()
    }
  }
}

void main()
