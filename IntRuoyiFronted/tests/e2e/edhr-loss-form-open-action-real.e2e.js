const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const taskDir = path.join(repoRoot, 'doc', 'tasks', '20260725-edhr-loss-form-open-action')

const config = {
  baseUrl: (process.env.EDHR_LOSS_FORM_OPEN_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.EDHR_LOSS_FORM_OPEN_E2E_TENANT || '芋道源码',
  username: process.env.EDHR_LOSS_FORM_OPEN_E2E_USERNAME || 'admin',
  password: process.env.EDHR_LOSS_FORM_OPEN_E2E_PASSWORD || readDefaultLoginPassword(),
  browserExecutable:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  explicitBatchExecutionId: Number(process.env.EDHR_LOSS_FORM_OPEN_E2E_BATCH_ID || 0),
  explicitTaskId: Number(process.env.EDHR_LOSS_FORM_OPEN_E2E_TASK_ID || 0),
  outputDir:
    process.env.EDHR_LOSS_FORM_OPEN_E2E_OUTPUT_DIR ||
    path.join(taskDir, 'real-e2e-output')
}

function readDefaultLoginPassword() {
  const envPath = path.join(frontendRoot, '.env')
  if (!fs.existsSync(envPath)) return ''
  const envText = fs.readFileSync(envPath, 'utf8')
  const match = envText.match(/^\s*VITE_APP_DEFAULT_LOGIN_PASSWORD\s*=\s*(.+?)\s*$/m)
  return match ? match[1].trim() : ''
}

function ensurePrerequisites() {
  assert.match(config.baseUrl, /^http:\/\/(localhost|127\.0\.0\.1):8081$/, '真实 E2E 只允许本机 8081 前端')
  assert.equal(config.tenant, '芋道源码', '管理员只读验证必须使用芋道源码租户')
  assert.equal(config.username, 'admin', '管理员只读验证必须使用 admin')
  assert.ok(config.password, '缺少管理员登录密码来源')
  assert.ok(fs.existsSync(config.browserExecutable), `Chrome 不存在: ${config.browserExecutable}`)
  assert.equal(
    config.explicitBatchExecutionId > 0,
    config.explicitTaskId > 0,
    '显式指定目标时必须同时提供 EDHR_LOSS_FORM_OPEN_E2E_BATCH_ID 和 EDHR_LOSS_FORM_OPEN_E2E_TASK_ID'
  )
  fs.mkdirSync(config.outputDir, { recursive: true })
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

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/feedback/edhr-batch-execution')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if (
    (await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0
  ) {
    throw new Error('登录页启用了验证码，无法执行无人值守真实 E2E')
  }

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
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, '租户')
  }

  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible'),
    config.username,
    '账号'
  )
  await fillFirstVisible(form.locator('input[type="password"]:visible'), config.password, '密码')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `登录 HTTP 失败：${loginResponse.status()}`)
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function apiGet(page, apiPath) {
  const result = await page.evaluate(async (apiPath) => {
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
    const response = await fetch(`/admin-api${apiPath}`, { headers })
    return { status: response.status, body: await response.json().catch(() => null) }
  }, apiPath)

  assert.equal(result.status, 200, `GET ${apiPath} HTTP ${result.status}`)
  assert.ok(
    result.body && (result.body.code === 0 || result.body.code === 200),
    `GET ${apiPath} 业务失败：${JSON.stringify(result.body)}`
  )
  return result.body.data
}

function canUseAsReadonlyLossForm(task) {
  const actions = Array.isArray(task.allowedActions) ? task.allowedActions : []
  return (
    task.nodeType === 'ROUTE_FORM' &&
    task.formSlotType === 'LOSS_REPORT' &&
    task.requiredPolicy === 'REQUIRED' &&
    !actions.includes('OPEN_FORM') &&
    Number(task.formCenterInstanceId || 0) > 0 &&
    Number(task.formTemplateId || 0) > 0 &&
    Boolean(task.formBindingKey) &&
    Boolean(task.formTemplateVersionId) &&
    Boolean(task.formTemplateVersionNo)
  )
}

function summarizeTask(task) {
  return {
    taskId: task.id,
    taskName: task.formTemplateName || task.batchRecordReportName || task.processName,
    processName: task.processName,
    status: task.status,
    requiredPolicy: task.requiredPolicy,
    activeWorkTaskId: task.activeWorkTaskId,
    allowedActions: task.allowedActions || [],
    formCenterInstanceId: task.formCenterInstanceId,
    formTemplateId: task.formTemplateId,
    disabledReason: task.disabledReason,
    gateMessage: task.gateMessage
  }
}

async function loadDetail(page, batchExecutionId) {
  const detail = await apiGet(page, `/mes/pro/edhr-batch-execution/get?id=${batchExecutionId}`)
  assert.equal(Number(detail.id), Number(batchExecutionId), `批次详情 ID 必须匹配 ${batchExecutionId}`)
  return detail
}

async function discoverTarget(page) {
  if (config.explicitBatchExecutionId > 0 && config.explicitTaskId > 0) {
    const detail = await loadDetail(page, config.explicitBatchExecutionId)
    const task = (detail.tasks || []).find((item) => Number(item.id) === config.explicitTaskId)
    assert.ok(task, `显式任务不存在：${config.explicitTaskId}`)
    assert.ok(canUseAsReadonlyLossForm(task), `显式任务不满足只读损耗单条件：${JSON.stringify(summarizeTask(task))}`)
    return { detail, task, scanned: [], source: 'env' }
  }

  const pageData = await apiGet(page, '/mes/pro/edhr-batch-execution/page?pageNo=1&pageSize=50')
  const records = Array.isArray(pageData?.list) ? pageData.list : []
  const scanned = []
  for (const record of records) {
    const detail = await loadDetail(page, record.id)
    const lossTasks = (detail.tasks || []).filter(
      (task) => task.nodeType === 'ROUTE_FORM' && task.formSlotType === 'LOSS_REPORT'
    )
    scanned.push({
      batchExecutionId: detail.id,
      batchCode: detail.batchCode,
      lossTasks: lossTasks.map(summarizeTask)
    })
    const task = lossTasks.find(canUseAsReadonlyLossForm)
    if (task) return { detail, task, scanned, source: 'page-scan' }
  }
  throw new Error(`未找到可验证的只读损耗单真实任务：${JSON.stringify(scanned.slice(0, 10))}`)
}

async function verifyReadonlyCardAction(page, detail, task) {
  const targetUrl =
    `${config.baseUrl}/mes/pro/feedback/edhr-batch-execution/detail?id=${detail.id}` +
    `&batchTaskId=${task.id}`

  await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('eDHR批次详情', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })

  const formPanel = page.locator('.edhr-batch-detail__rail-process-forms').first()
  await formPanel.waitFor({ state: 'visible', timeout: 60000 })
  const taskCard = formPanel
    .locator('.edhr-batch-detail__rail-process-form-item')
    .filter({ hasText: '损耗单' })
    .first()
  await taskCard.waitFor({ state: 'visible', timeout: 60000 })

  const actionButton = taskCard.locator('.edhr-batch-detail__rail-process-form-action').first()
  await actionButton.waitFor({ state: 'visible', timeout: 30000 })
  const actionLabel = (await actionButton.innerText()).trim()
  assert.equal(actionLabel, '查看表单', `无填写权限损耗单主动作必须显示“查看表单”，实际为：${actionLabel}`)
  assert.equal(await actionButton.isEnabled(), true, '只读查看按钮必须可点击')

  const blockedWrites = []
  page.on('request', (request) => {
    const url = request.url()
    const method = request.method().toUpperCase()
    if (url.includes('/admin-api/mes/pro/edhr-batch-execution/task/open')) {
      blockedWrites.push(`${method} ${url}`)
    }
    if (url.includes('/admin-api/mes/pro/edhr-batch-execution/task/special-node/skip')) {
      blockedWrites.push(`${method} ${url}`)
    }
    if (
      !['GET', 'HEAD', 'OPTIONS'].includes(method) &&
      (url.includes('/admin-api/mes/') || url.includes('/admin-api/form-center/'))
    ) {
      blockedWrites.push(`${method} ${url}`)
    }
  })

  await actionButton.click()
  const drawer = page.locator('.el-drawer:visible').filter({ hasText: /查看表单：/ }).last()
  await drawer.waitFor({ state: 'visible', timeout: 60000 })
  await drawer.getByText('查看表单：损耗单', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await drawer
    .getByText('当前账号仅有查看权限，表单保存、提交、重提和放弃操作已禁用。', { exact: true })
    .waitFor({ state: 'visible', timeout: 30000 })
  await drawer.locator('.form-action-panel').first().waitFor({ state: 'visible', timeout: 60000 })

  const actionStates = await drawer.locator('.form-action-panel__actions button').evaluateAll((buttons) =>
    buttons.map((button) => ({
      label: (button.textContent || '').replace(/\s+/g, ''),
      disabled: button.disabled
    }))
  )
  const expectedLabels = ['解析', '创建', '保存草稿', '提交', '重提', '放弃']
  for (const label of expectedLabels) {
    const item = actionStates.find((state) => state.label.includes(label))
    assert.ok(item, `只读表单动作区缺少按钮：${label}`)
    assert.equal(item.disabled, true, `只读表单动作按钮必须禁用：${label}`)
  }

  await page.waitForTimeout(800)
  assert.deepEqual(blockedWrites, [], `只读查看不得触发打开填写、跳过或写入接口：${JSON.stringify(blockedWrites)}`)

  const screenshotPath = path.join(config.outputDir, 'readonly-loss-form-card.png')
  await page.screenshot({ path: screenshotPath, fullPage: true })

  return {
    targetUrl,
    actionLabel,
    actionStates,
    screenshotPath,
    blockedWrites
  }
}

async function main() {
  ensurePrerequisites()
  const browser = await chromium.launch({
    headless: process.env.EDHR_LOSS_FORM_OPEN_E2E_HEADED === '1' ? false : true,
    executablePath: config.browserExecutable
  })
  const context = await browser.newContext({ viewport: { width: 1680, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const consoleErrors = []
  const pageErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))

  try {
    await login(page)
    const { detail, task, scanned, source } = await discoverTarget(page)
    const uiResult = await verifyReadonlyCardAction(page, detail, task)
    assert.deepEqual(pageErrors, [], `页面运行时错误：${JSON.stringify(pageErrors)}`)

    const result = {
      tenant: config.tenant,
      username: config.username,
      source,
      batchExecutionId: detail.id,
      batchCode: detail.batchCode,
      task: summarizeTask(task),
      uiResult,
      consoleErrorCount: consoleErrors.length,
      pageErrors,
      scannedCount: scanned.length
    }
    const evidencePath = path.join(config.outputDir, 'readonly-loss-form-card-result.json')
    fs.writeFileSync(evidencePath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(
      `PASS: readonly loss form card opens view drawer batch=${detail.id} task=${task.id} evidence=${evidencePath}`
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
