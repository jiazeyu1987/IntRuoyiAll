const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FIELDS = [
  'incomingInspectionReportRequired',
  'sterilizationReportRequired',
  'finishedProductInspectionReportRequired',
  'finishedProductInspectionRecordRequired'
]

const SWITCH_ITEMS = [
  {
    field: 'incomingInspectionReportRequired',
    label: '来料检报告'
  },
  {
    field: 'sterilizationReportRequired',
    label: '灭菌报告'
  },
  {
    field: 'finishedProductInspectionReportRequired',
    label: '成品检报告'
  },
  {
    field: 'finishedProductInspectionRecordRequired',
    label: '成品检记录限制'
  }
]

const API_PATH = '/mes/pro/edhr-release-setting/dossier-requirements'
const TASK_DIR = path.resolve(
  __dirname,
  '..',
  '..',
  '..',
  'doc',
  'tasks',
  '20260726-edhr-release-dossier-requirement-switches'
)
const RESULT_DIR = path.join(TASK_DIR, 'e2e-artifacts', 'dossier-requirement-setting-real')
const RESULT_JSON = path.join(RESULT_DIR, 'result.json')
const RESULT_MD = path.join(RESULT_DIR, 'result.md')

function parseEnvFile() {
  const envPath = path.resolve(__dirname, '..', '..', '.env')
  const env = {}
  const content = fs.readFileSync(envPath, 'utf8')
  for (const line of content.split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/)
    if (!match) continue
    env[match[1]] = match[2].replace(/^['"]|['"]$/g, '').trim()
  }
  return env
}

const envDefaults = parseEnvFile()
const config = {
  baseUrl: (process.env.EDHR_RELEASE_DOSSIER_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  backendUrl: (process.env.EDHR_RELEASE_DOSSIER_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
  tenant: process.env.EDHR_RELEASE_DOSSIER_E2E_TENANT || envDefaults.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: process.env.EDHR_RELEASE_DOSSIER_E2E_USERNAME || envDefaults.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: process.env.EDHR_RELEASE_DOSSIER_E2E_PASSWORD || envDefaults.VITE_APP_DEFAULT_LOGIN_PASSWORD,
  timeout: Number(process.env.EDHR_RELEASE_DOSSIER_E2E_TIMEOUT || 90000),
  headed: process.env.EDHR_RELEASE_DOSSIER_E2E_HEADED === '1',
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || ''
}

function assertPrerequisites() {
  const explicitBaseUrl = Boolean(process.env.EDHR_RELEASE_DOSSIER_E2E_BASE_URL)
  const explicitBackendUrl = Boolean(process.env.EDHR_RELEASE_DOSSIER_E2E_BACKEND_URL)
  assert.equal(explicitBaseUrl, explicitBackendUrl, '隔离运行态 E2E 必须同时显式传入前端和后端 URL')
  const frontendUrl = new URL(config.baseUrl)
  const backendUrl = new URL(config.backendUrl)
  assert.equal(frontendUrl.hostname, '127.0.0.1', '真实 E2E 只能使用本机前端')
  assert.equal(backendUrl.hostname, '127.0.0.1', '真实 E2E 只能使用本机后端')
  const frontendPort = Number(frontendUrl.port)
  const backendPort = Number(backendUrl.port)
  assert.ok(Number.isInteger(frontendPort) && Number.isInteger(backendPort), '真实 E2E URL 必须包含显式端口')
  assert.equal(backendPort - frontendPort, 40000, '前后端端口必须来自同一 int_main runtime slot')
  if (!explicitBaseUrl) {
    assert.equal(config.baseUrl, 'http://127.0.0.1:8081', '默认真实 E2E 必须使用 int_main 前端 8081')
    assert.equal(config.backendUrl, 'http://127.0.0.1:48081', '默认真实 E2E 必须使用 int_main 后端 48081')
  }
  assert.equal(config.tenant, '芋道源码', '资料限制开关 E2E 必须使用本机默认金手指租户')
  assert.equal(config.username, 'admin', '资料限制开关 E2E 必须使用本机默认金手指账号')
  assert.ok(config.password, '缺少本机默认登录密码来源')
  assert.ok(Number.isFinite(config.timeout) && config.timeout > 0, 'EDHR_RELEASE_DOSSIER_E2E_TIMEOUT 必须为正数')
  if (config.executablePath) {
    assert.ok(fs.existsSync(config.executablePath), `Chrome not found: ${config.executablePath}`)
  }
  fs.mkdirSync(RESULT_DIR, { recursive: true })
}

function pickSetting(source) {
  const picked = {}
  for (const field of FIELDS) {
    picked[field] = source?.[field] === true
  }
  return picked
}

function assertSettingEquals(actual, expected, label) {
  assert.deepEqual(pickSetting(actual), pickSetting(expected), `${label} 配置布尔对象不一致`)
}

function buildExpectedSetting(source, field, value) {
  return {
    ...pickSetting(source),
    [field]: value
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible().catch(() => false)) && !(await input.isDisabled().catch(() => true))) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写登录控件：${label}`)
}

async function selectTenant(page, form) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: config.tenant })
      .first()
    await option.waitFor({ state: 'visible', timeout: config.timeout })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, '租户')
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/user/profile')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })

  if (!page.url().includes('/login')) {
    return
  }

  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页启用了验证码，无法执行无人值守真实 E2E。')
  }

  await selectTenant(page, form)
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    '账号'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, '密码')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `登录 HTTP 失败：${loginResponse.status()}`)
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: config.timeout })
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
}

function unwrapStorageValue(raw) {
  if (!raw) return ''
  let current = raw
  for (let index = 0; index < 8; index += 1) {
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

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      result[key] = result[key] || sessionStorage.getItem(key)
    }
    return result
  })
  const auth = {
    token: unwrapStorageValue(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrapStorageValue(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrapStorageValue(snapshot.visitTenantId)
  }
  assert.ok(auth.token, 'API 复验需要浏览器登录后的 access token')
  assert.ok(auth.tenantId, 'API 复验需要浏览器登录后的 tenant-id')
  return auth
}

async function apiRequest(page, auth, method, apiPath, data) {
  const headers = {
    Authorization: `Bearer ${auth.token}`,
    'tenant-id': String(auth.tenantId),
    ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
  }
  const url = `${config.backendUrl}/admin-api${apiPath}`
  const options = { headers, timeout: config.timeout }
  if (data !== undefined) {
    options.data = data
  }
  const response = await page.request[method](url, options)
  const body = await response.json().catch(() => ({}))
  assert.equal(response.status(), 200, `${method.toUpperCase()} ${apiPath} HTTP 状态必须为 200`)
  assert.equal(Number(body.code), 0, `${method.toUpperCase()} ${apiPath} 业务响应必须成功：${body.msg || body.code}`)
  return body.data
}

async function apiGetSetting(page, auth) {
  return apiRequest(page, auth, 'get', API_PATH)
}

async function apiPutSetting(page, auth, payload) {
  return apiRequest(page, auth, 'put', API_PATH, pickSetting(payload))
}

async function openConfigPane(page) {
  await page.goto(`${config.baseUrl}/user/profile`, { waitUntil: 'domcontentloaded', timeout: config.timeout })
  const configTab = page.locator('.el-tabs__item:visible').filter({ hasText: /^配置$/ }).first()
  await configTab.waitFor({ state: 'visible', timeout: config.timeout })
  await configTab.click()
  const card = page.locator('.edhr-release-dossier-requirement-setting').first()
  await card.waitFor({ state: 'visible', timeout: config.timeout })
  const cardText = (await card.textContent()) || ''
  assert.ok(cardText.includes('eDHR 放行资料限制'), '配置页签必须显示 eDHR 放行资料限制卡片')
  for (const item of SWITCH_ITEMS) {
    assert.ok(cardText.includes(item.label), `配置页签缺少资料限制开关：${item.label}`)
  }
  const alert = card.locator('.el-alert--error:visible').first()
  if (await alert.isVisible().catch(() => false)) {
    throw new Error(`资料限制开关加载失败：${await alert.textContent()}`)
  }
  return card
}

function switchRow(card, label) {
  return card.locator('.edhr-release-dossier-requirement-setting__item').filter({ hasText: label }).first()
}

async function switchChecked(row) {
  return row.locator('.el-switch').first().evaluate((element) => element.classList.contains('is-checked'))
}

async function waitForSwitchEnabled(row, label) {
  const switchLocator = row.locator('.el-switch').first()
  await switchLocator.waitFor({ state: 'visible', timeout: config.timeout })
  const deadline = Date.now() + config.timeout
  while (Date.now() < deadline) {
    const disabled = await switchLocator.evaluate((element) => element.classList.contains('is-disabled')).catch(() => true)
    if (!disabled) return
    await row.page().waitForTimeout(200)
  }
  throw new Error(`开关仍处于禁用状态：${label}`)
}

async function waitForSwitchState(row, expected, label) {
  const deadline = Date.now() + config.timeout
  while (Date.now() < deadline) {
    if ((await switchChecked(row).catch(() => undefined)) === expected) {
      return
    }
    await row.page().waitForTimeout(200)
  }
  throw new Error(`开关状态未变为 ${expected ? '打开' : '关闭'}：${label}`)
}

async function setSwitchViaUi(page, field, nextValue) {
  const item = SWITCH_ITEMS.find((candidate) => candidate.field === field)
  assert.ok(item, `未知资料限制字段：${field}`)
  const card = await openConfigPane(page)
  const row = switchRow(card, item.label)
  await row.waitFor({ state: 'visible', timeout: config.timeout })
  await waitForSwitchEnabled(row, item.label)
  const currentValue = await switchChecked(row)
  if (currentValue === nextValue) {
    return { field, label: item.label, changed: false, value: nextValue }
  }

  await row.locator('.el-switch').first().click()
  const messageBox = page.locator('.el-message-box:visible').first()
  await messageBox.waitFor({ state: 'visible', timeout: config.timeout })
  const messageText = (await messageBox.textContent()) || ''
  assert.ok(messageText.includes(item.label), `确认框必须说明当前开关：${item.label}`)
  assert.ok(messageText.includes(nextValue ? '缺少对应资料的批次将无法放行' : '放行不再强制要求该资料'), '确认框必须说明开关影响')

  const [request, response] = await Promise.all([
    page.waitForRequest(
      (request) =>
        request.url().includes(`/admin-api${API_PATH}`) && request.method() === 'PUT',
      { timeout: config.timeout }
    ),
    page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api${API_PATH}`) && response.request().method() === 'PUT',
      { timeout: config.timeout }
    ),
    messageBox.locator('button, .el-button').filter({ hasText: /确认|确定|OK/ }).last().click()
  ])
  const requestPayload = JSON.parse(request.postData() || '{}')
  for (const requiredField of FIELDS) {
    assert.equal(typeof requestPayload[requiredField], 'boolean', `PUT 必须提交完整布尔字段：${requiredField}`)
  }
  assert.equal(requestPayload[field], nextValue, `PUT payload 中 ${field} 必须为目标值`)
  const responseBody = await response.json()
  assert.equal(Number(responseBody.code), 0, `PUT 保存失败：${responseBody.msg || responseBody.code}`)
  await waitForSwitchState(row, nextValue, item.label)
  return {
    field,
    label: item.label,
    changed: true,
    value: nextValue,
    requestPayload: pickSetting(requestPayload),
    responseHash: responseBody.data?.configHash || ''
  }
}

async function restoreOriginalViaUi(page, auth, originalSetting) {
  const steps = []
  let current = await apiGetSetting(page, auth)
  for (const field of FIELDS) {
    const desired = originalSetting[field] === true
    if ((current[field] === true) === desired) continue
    const restoreStep = await setSwitchViaUi(page, field, desired)
    steps.push(restoreStep)
    current = await apiGetSetting(page, auth)
  }
  const restored = await apiGetSetting(page, auth)
  assertSettingEquals(restored, originalSetting, '恢复后')
  return {
    method: steps.length > 0 ? 'UI' : 'NOOP',
    steps,
    restored: pickSetting(restored),
    restoredHash: restored.configHash || ''
  }
}

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_JSON, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
  const lines = [
    '# eDHR 放行资料限制开关真实 E2E',
    '',
    `- Status: ${result.status}`,
    `- Base URL: ${config.baseUrl}`,
    `- Backend URL: ${config.backendUrl}`,
    '- Tenant/User: 芋道源码/admin',
    `- Original: ${JSON.stringify(result.originalSetting || {})}`,
    `- Changed: ${JSON.stringify(result.changedSetting || {})}`,
    `- Restore: ${JSON.stringify(result.restore || {})}`
  ]
  if (result.status === 'PASS') {
    lines.push('- GREEN: real-profile-config-dossier-switch -> PASS，真实页面配置页签展示 4 个资料限制开关，UI 确认切换成功，API 复核变更成功，最后通过 UI 恢复原始状态并复验。')
  } else if (result.status === 'BLOCKED') {
    lines.push(`- BLOCKER: real-profile-config-dossier-switch -> ${result.reason || result.restoreError || 'unknown blocker'}`)
  } else {
    lines.push(`- RED: real-profile-config-dossier-switch -> FAIL，${result.reason || result.restoreError || 'unknown error'}`)
  }
  fs.writeFileSync(RESULT_MD, `${lines.join('\n')}\n`, 'utf8')
}

function isRuntimeBlocker(error) {
  const message = error instanceof Error ? error.stack || error.message : String(error)
  return message.includes(API_PATH) && (message.includes('请求地址不存在') || message.includes('404'))
}

async function main() {
  assertPrerequisites()
  const result = {
    status: 'FAIL',
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    identity: '芋道源码/admin',
    steps: []
  }
  let pendingError
  let browser
  let context
  let page
  let auth
  let originalSetting

  try {
    browser = await chromium.launch({
      headless: !config.headed,
      executablePath: config.executablePath || undefined,
      args: ['--disable-dev-shm-usage']
    })
    context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    page = await context.newPage()
    page.setDefaultTimeout(config.timeout)

    await login(page)
    auth = await browserAuth(page)
    originalSetting = await apiGetSetting(page, auth)
    result.originalSetting = pickSetting(originalSetting)
    result.originalHash = originalSetting.configHash || ''

    const card = await openConfigPane(page)
    if (originalSetting.configHash) {
      const cardText = (await card.textContent()) || ''
      assert.ok(!cardText.includes(originalSetting.configHash), '页面不应展示当前配置 hash')
    }
    result.steps.push({ step: 'profile-config-visible', status: 'PASS' })

    const targetField = 'incomingInspectionReportRequired'
    const targetValue = originalSetting[targetField] !== true
    const toggleStep = await setSwitchViaUi(page, targetField, targetValue)
    result.steps.push({ step: 'toggle-incoming-inspection-report', status: 'PASS', toggleStep })

    const expectedChanged = buildExpectedSetting(originalSetting, targetField, targetValue)
    const changedSetting = await apiGetSetting(page, auth)
    assertSettingEquals(changedSetting, expectedChanged, '切换后')
    result.changedSetting = pickSetting(changedSetting)
    result.changedHash = changedSetting.configHash || ''
    result.steps.push({ step: 'api-verify-changed-state', status: 'PASS' })
  } catch (error) {
    pendingError = error
    result.blocked = isRuntimeBlocker(error)
    result.reason = error instanceof Error ? error.stack || error.message : String(error)
    if (page) {
      const screenshotPath = path.join(RESULT_DIR, 'failure.png')
      fs.mkdirSync(RESULT_DIR, { recursive: true })
      await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {})
      result.failureScreenshot = screenshotPath
    }
  } finally {
    if (page && auth && originalSetting) {
      try {
        result.restore = await restoreOriginalViaUi(page, auth, pickSetting(originalSetting))
      } catch (restoreError) {
        result.restoreError = restoreError instanceof Error ? restoreError.stack || restoreError.message : String(restoreError)
        try {
          const restoredByApi = await apiPutSetting(page, auth, originalSetting)
          const restored = await apiGetSetting(page, auth)
          assertSettingEquals(restored, originalSetting, 'API cleanup 后')
          result.restore = {
            method: 'API_CLEANUP_AFTER_UI_RESTORE_FAILURE',
            restored: pickSetting(restored),
            restoredHash: restored.configHash || restoredByApi.configHash || ''
          }
        } catch (cleanupError) {
          result.cleanupError = cleanupError instanceof Error ? cleanupError.stack || cleanupError.message : String(cleanupError)
        }
        pendingError =
          pendingError ||
          new Error(`UI 恢复原始配置失败，已尝试受控 API cleanup：${result.restoreError}`)
      }
    }

    result.status = pendingError ? (result.blocked ? 'BLOCKED' : 'FAIL') : 'PASS'
    writeResult(result)
    await context?.close().catch(() => {})
    await browser?.close().catch(() => {})
  }

  if (pendingError) {
    throw pendingError
  }
  console.log('PASS: eDHR release dossier requirement setting real E2E')
}

main().catch((error) => {
  console.error(error instanceof Error ? error.stack || error.message : error)
  process.exitCode = 1
})
