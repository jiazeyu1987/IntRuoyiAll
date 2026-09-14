const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const { chromium } = require(require.resolve('playwright', { paths: [frontendRoot] }))

const config = {
  baseUrl: (process.env.FORM_CENTER_ROUTE_E2E_BASE_URL || 'http://127.0.0.1:8094').replace(/\/+$/, ''),
  backendUrl: (process.env.FORM_CENTER_ROUTE_E2E_BACKEND_URL || 'http://127.0.0.1:48094').replace(/\/+$/, ''),
  tenant: process.env.FORM_CENTER_ROUTE_E2E_TENANT || '芋道源码',
  username: process.env.FORM_CENTER_ROUTE_E2E_USERNAME || 'admin',
  password: process.env.FORM_CENTER_ROUTE_E2E_PASSWORD || readDefaultLoginPassword(),
  batchExecutionId: Number(process.env.FORM_CENTER_ROUTE_E2E_BATCH_ID || 900000000910),
  batchTaskId: Number(process.env.FORM_CENTER_ROUTE_E2E_TASK_ID || 7234),
  workTaskId: Number(process.env.FORM_CENTER_ROUTE_E2E_WORK_TASK_ID || 2301),
  templateId: Number(process.env.FORM_CENTER_ROUTE_E2E_TEMPLATE_ID || 28),
  templateVersionId: Number(process.env.FORM_CENTER_ROUTE_E2E_TEMPLATE_VERSION_ID || 32),
  templateVersionNo: process.env.FORM_CENTER_ROUTE_E2E_TEMPLATE_VERSION_NO || 'V3.0',
  browserExecutable:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  headed: process.env.FORM_CENTER_ROUTE_E2E_HEADED === '1',
  outputDir:
    process.env.FORM_CENTER_ROUTE_E2E_OUTPUT_DIR ||
    path.join(repoRoot, 'doc', 'tasks', '20260803-form-center-route-missing-e2e', 'real-e2e-output')
}

const targetVersionEndpointPattern = /\/admin-api\/form-center\/templates\/\d+\/versions\/[^/?#]+(?:[?#].*)?$/

function readDefaultLoginPassword() {
  const envPath = path.join(frontendRoot, '.env')
  if (!fs.existsSync(envPath)) return ''
  const envText = fs.readFileSync(envPath, 'utf8')
  const match = envText.match(/^\s*VITE_APP_DEFAULT_LOGIN_PASSWORD\s*=\s*(.+?)\s*$/m)
  return match ? match[1].trim() : ''
}

function assertLocalRuntime() {
  assert.equal(config.baseUrl, 'http://127.0.0.1:8094', `unexpected frontend URL: ${config.baseUrl}`)
  assert.equal(config.backendUrl, 'http://127.0.0.1:48094', `unexpected backend URL: ${config.backendUrl}`)
  assert.equal(config.tenant, '芋道源码', `unexpected tenant: ${config.tenant}`)
  assert.equal(config.username, 'admin', `unexpected username: ${config.username}`)
  assert.ok(config.password, 'missing local login password source')
  assert.ok(fs.existsSync(config.browserExecutable), `missing Chrome executable: ${config.browserExecutable}`)
  fs.mkdirSync(config.outputDir, { recursive: true })
}

async function assertRuntimeUp() {
  const frontend = await fetch(`${config.baseUrl}/`)
  assert.equal(frontend.status, 200, `frontend must return HTTP 200, got ${frontend.status}`)
  const healthResponse = await fetch(`${config.backendUrl}/actuator/health`)
  assert.equal(healthResponse.status, 200, `backend health HTTP must be 200, got ${healthResponse.status}`)
  const health = await healthResponse.json()
  assert.equal(health.status, 'UP', `backend health must be UP: ${JSON.stringify(health)}`)
}

function unwrapBusinessResponse(payload, label) {
  assert.ok(payload && typeof payload === 'object', `${label} must return JSON payload`)
  assert.ok(payload.code === 0 || payload.code === 200, `${label} business failure: ${payload.msg || payload.code}`)
  return payload.data
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
  throw new Error(`missing visible login input: ${label}`)
}

async function login(page) {
  const targetPath = '/index'
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('login captcha is enabled; unattended E2E cannot continue')
  }

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }

  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[placeholder="请输入密码"], input[type="password"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.status(), 200, `login HTTP status: ${loginResponse.status()}`)
  unwrapBusinessResponse(await loginResponse.json(), 'login')
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function apiGet(page, apiPath) {
  const result = await page.evaluate(async (requestPath) => {
    const unwrap = (value) => {
      if (!value || typeof value !== 'object') return value
      for (const key of ['accessToken', 'value', 'v', 'data']) {
        if (Object.prototype.hasOwnProperty.call(value, key)) return unwrap(value[key])
      }
      return value
    }
    const normalize = (value) => {
      if (typeof value !== 'string') return value
      const trimmed = value.trim()
      if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
        try {
          return JSON.parse(trimmed)
        } catch {
          return trimmed.slice(1, -1)
        }
      }
      return trimmed
    }
    const readStorageValue = (suffix) => {
      for (const storage of [localStorage, sessionStorage]) {
        const matchedKey = Object.keys(storage).find((key) => key === suffix || key.endsWith(suffix))
        if (!matchedKey) continue
        const raw = storage.getItem(matchedKey)
        if (!raw) continue
        try {
          return normalize(unwrap(JSON.parse(raw)))
        } catch {
          return normalize(raw)
        }
      }
      return undefined
    }

    const headers = { 'Cache-Control': 'no-cache', Pragma: 'no-cache' }
    const accessToken = readStorageValue('ACCESS_TOKEN')
    const tenantId = readStorageValue('tenantId')
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`
    if (tenantId) headers['tenant-id'] = String(tenantId)
    const response = await fetch(`/admin-api${requestPath}`, { headers })
    return { status: response.status, body: await response.json().catch(() => null) }
  }, apiPath)
  assert.equal(result.status, 200, `GET ${apiPath} HTTP ${result.status}`)
  return unwrapBusinessResponse(result.body, `GET ${apiPath}`)
}

function summarizeTask(task) {
  return {
    id: task.id,
    processName: task.processName,
    formSlotType: task.formSlotType,
    formTemplateId: task.formTemplateId,
    formTemplateName: task.formTemplateName,
    formTemplateVersionId: task.formTemplateVersionId,
    formTemplateVersionNo: task.formTemplateVersionNo,
    formCenterInstanceId: task.formCenterInstanceId,
    activeWorkTaskId: task.activeWorkTaskId,
    allowedActions: task.allowedActions || [],
    status: task.status
  }
}

async function assertTargetTask(page) {
  const detail = await apiGet(page, `/mes/pro/edhr-batch-execution/get?id=${config.batchExecutionId}`)
  const task = (detail.tasks || []).find((item) => Number(item.id) === config.batchTaskId)
  assert.ok(task, `target batch task ${config.batchTaskId} not found`)
  assert.equal(Number(task.formTemplateId), config.templateId, `target task templateId mismatch: ${JSON.stringify(summarizeTask(task))}`)
  assert.equal(Number(task.formTemplateVersionId), config.templateVersionId, `target task versionId mismatch: ${JSON.stringify(summarizeTask(task))}`)
  assert.equal(String(task.formTemplateVersionNo), config.templateVersionNo, `target task versionNo mismatch: ${JSON.stringify(summarizeTask(task))}`)
  assert.ok(Number(task.formCenterInstanceId) > 0, `target task must have FormCenter instance: ${JSON.stringify(summarizeTask(task))}`)
  assert.ok(Number(task.activeWorkTaskId) === config.workTaskId, `target task must expose active work task ${config.workTaskId}: ${JSON.stringify(summarizeTask(task))}`)
  return { detail, task }
}

async function verifyTargetPath(page, targetTask) {
  const exactForbiddenPath = `/admin-api/form-center/templates/${config.templateId}/versions/${config.templateVersionNo}`
  const network = {
    forbiddenTemplateVersionRequests: [],
    exactForbiddenRequests: [],
    routeMissingResponses: [],
    targetFormCenterResponses: [],
    failedLocalResponses: []
  }
  const consoleErrors = []
  const pageErrors = []

  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))
  page.on('request', (request) => {
    const url = request.url()
    const parsed = new URL(url)
    const pathAndSearch = `${parsed.pathname}${parsed.search}`
    if (targetVersionEndpointPattern.test(pathAndSearch)) {
      network.forbiddenTemplateVersionRequests.push({ method: request.method(), url: pathAndSearch })
    }
    if (pathAndSearch === exactForbiddenPath) {
      network.exactForbiddenRequests.push({ method: request.method(), url: pathAndSearch })
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/')) return
    const parsed = new URL(url)
    const pathAndSearch = `${parsed.pathname}${parsed.search}`
    if (pathAndSearch.includes('/admin-api/form-center/')) {
      network.targetFormCenterResponses.push({
        method: response.request().method(),
        status: response.status(),
        url: pathAndSearch
      })
    }
    if (response.status() >= 400 && url.startsWith(config.baseUrl)) {
      network.failedLocalResponses.push({
        method: response.request().method(),
        status: response.status(),
        url: pathAndSearch
      })
    }
    if (response.status() >= 400 || pathAndSearch.includes('/admin-api/form-center/')) {
      const text = await response.text().catch(() => '')
      if (text.includes('请求地址不存在')) {
        network.routeMissingResponses.push({
          method: response.request().method(),
          status: response.status(),
          url: pathAndSearch
        })
      }
    }
  })

  const targetUrl =
    `${config.baseUrl}/mes/pro/feedback/edhr-batch-execution/detail?id=${config.batchExecutionId}` +
    `&batchTaskId=${config.batchTaskId}&workTaskId=${config.workTaskId}`

  await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('eDHR批次详情', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.edhr-batch-detail__rail-process-forms').first().waitFor({ state: 'visible', timeout: 60000 })
  const activeAction = page
    .locator('.edhr-batch-detail__rail-process-form-item.is-active .edhr-batch-detail__rail-process-form-action')
    .first()
  await activeAction.waitFor({ state: 'visible', timeout: 60000 })
  const actionText = (await activeAction.innerText()).replace(/\s+/g, '')
  assert.equal(actionText, '查看表单', `admin readonly path must expose 查看表单, got ${actionText}`)
  await activeAction.click()

  const drawer = page.locator('.el-drawer:visible').filter({ hasText: /填写表单|查看表单/ }).last()
  await drawer.waitFor({ state: 'visible', timeout: 60000 })
  await drawer.locator('.form-action-panel').waitFor({ state: 'visible', timeout: 60000 })
  const expectedRuntimeBlocker = '动态表单运行态缺少 openTask 模板快照，无法渲染。'
  await drawer.getByText(expectedRuntimeBlocker, { exact: true }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await page.waitForTimeout(1500)

  assert.deepEqual(network.exactForbiddenRequests, [], `exact forbidden request observed: ${JSON.stringify(network.exactForbiddenRequests)}`)
  assert.deepEqual(
    network.forbiddenTemplateVersionRequests,
    [],
    `runtime template management version request observed: ${JSON.stringify(network.forbiddenTemplateVersionRequests)}`
  )
  assert.deepEqual(network.routeMissingResponses, [], `route-missing response observed: ${JSON.stringify(network.routeMissingResponses)}`)
  assert.deepEqual(pageErrors, [], `page errors observed: ${JSON.stringify(pageErrors)}`)

  const screenshotPath = path.join(config.outputDir, 'form-center-route-missing-real-e2e.png')
  await page.screenshot({ path: screenshotPath, fullPage: true })
  return {
    targetUrl,
    mode: 'admin-readonly-drawer',
    actionText,
    expectedRuntimeBlocker,
    targetTask: summarizeTask(targetTask),
    network,
    consoleErrorCount: consoleErrors.length,
    pageErrors,
    screenshotPath
  }
}

async function main() {
  assertLocalRuntime()
  await assertRuntimeUp()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.browserExecutable,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1600, height: 980 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  try {
    await login(page)
    const target = await assertTargetTask(page)
    const ui = await verifyTargetPath(page, target.task)
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      backendUrl: config.backendUrl,
      tenant: config.tenant,
      username: config.username,
      batchExecutionId: config.batchExecutionId,
      batchExecutionCode: target.detail.batchExecutionCode,
      batchCode: target.detail.batchCode,
      task: summarizeTask(target.task),
      ui
    }
    const resultPath = path.join(config.outputDir, 'form-center-route-missing-real-e2e-result.json')
    fs.writeFileSync(resultPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(`PASS: no FormCenter template version request observed; evidence=${resultPath}`)
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
