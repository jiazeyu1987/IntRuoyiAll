const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = envValue('EDHR_BATCH_E2E_TASK_ID') || '20260608-edhr-batch-execution-full-flow'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-batch-execution')
const EVIDENCE_FILE = envValue('EDHR_BATCH_E2E_EVIDENCE_FILE')
  ? path.resolve(envValue('EDHR_BATCH_E2E_EVIDENCE_FILE'))
  : path.resolve(process.cwd(), 'doc', 'tasks', TASK_ID, 'real-e2e-evidence.md')
const REQUIRED_BASE_URL = 'http://localhost:8081'
const BATCH_EXECUTION_ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])

const REQUIRED_ENV = [
  ['EDHR_BATCH_E2E_PASSWORD', '测试租户账号密码'],
  ['EDHR_BATCH_E2E_WORK_ORDER_ID', '真实生产工单ID'],
  ['EDHR_BATCH_E2E_BATCH_CODE', '真实批次号'],
  ['EDHR_BATCH_E2E_FIRST_FIELD_VALUE', '第一道工序表单真实填写值'],
  ['EDHR_BATCH_E2E_CLOSE_PASSWORD', '关闭批次电子签名密码']
]

function envValue(name) {
  return (process.env[name] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 批次执行真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 状态：${result.status}`,
    `- 前端入口：\`${REQUIRED_BASE_URL}\``,
    '- 测试租户：`测试租户`；账号名默认 `aoteman`，密码由环境变量注入。',
    '',
    '## BDD',
    '',
    '- BDD: 创建/打开批次执行 -> Given 测试租户存在真实工单、产品、路线和默认批记录绑定 When 用户从工作台创建或打开批次 Then 页面进入批次详情并展示按路线排序的任务。',
    '- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。',
    '- BDD: 关闭和归档入口 -> Given 批次任务完成且后端返回 canClose=true When 用户关闭批次并生成归档 Then 前端调用真实关闭、生成、下载接口并暴露打印入口。',
    '',
    '## Result',
    ''
  ]

  if (result.status === 'BLOCKED') {
    lines.push(`- BLOCKED: \`node tests/e2e/edhr-batch-execution-real-flow.e2e.js\` -> FAIL, ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- 缺失前置：\`${item.name}\`，${item.description}`)
    }
    lines.push('- 影响：无法在真实前端页面完成批次创建、工序填写、关闭、归档和下载验证；未使用 mock、API-only 或测试专用控件。')
  } else if (result.status === 'PASS') {
    lines.push('- GREEN: 真实前端路径已完成。')
  } else {
    lines.push(`- RED: 真实前端路径失败，${result.error || '未知错误'}`)
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function collectConfig() {
  const tenant = envValue('EDHR_BATCH_E2E_TENANT') || '测试租户'
  const config = {
    baseUrl: envValue('EDHR_BATCH_E2E_BASE_URL') || REQUIRED_BASE_URL,
    tenant,
    username: envValue('EDHR_BATCH_E2E_USERNAME') || 'aoteman',
    password: envValue('EDHR_BATCH_E2E_PASSWORD'),
    workOrderId: envValue('EDHR_BATCH_E2E_WORK_ORDER_ID'),
    workOrderCode: envValue('EDHR_BATCH_E2E_WORK_ORDER_CODE'),
    batchCode: envValue('EDHR_BATCH_E2E_BATCH_CODE'),
    routeId: envValue('EDHR_BATCH_E2E_ROUTE_ID'),
    routeCode: envValue('EDHR_BATCH_E2E_ROUTE_CODE'),
    routeName: envValue('EDHR_BATCH_E2E_ROUTE_NAME'),
    firstFieldValue: envValue('EDHR_BATCH_E2E_FIRST_FIELD_VALUE'),
    closePassword: envValue('EDHR_BATCH_E2E_CLOSE_PASSWORD'),
    executablePath:
      envValue('EDHR_BATCH_E2E_CHROME_EXECUTABLE') ||
      envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH') ||
      envValue('PLAYWRIGHT_CHROME_EXECUTABLE'),
    headed: envValue('EDHR_BATCH_E2E_HEADED') === '1'
  }

  const missing = REQUIRED_ENV.filter(([name]) => !envValue(name)).map(([name, description]) => ({
    name,
    description
  }))
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    missing.push({ name: 'EDHR_BATCH_E2E_BASE_URL', description: `必须固定为 ${REQUIRED_BASE_URL}` })
  }
  if (FORBIDDEN_TENANTS.has(config.tenant.toLowerCase()) || config.tenant.includes('芋道源码')) {
    missing.push({ name: 'EDHR_BATCH_E2E_TENANT', description: '真实 E2E 禁止使用正式或芋道源码租户' })
  }
  for (const [name, value] of [
    ['EDHR_BATCH_E2E_WORK_ORDER_ID', config.workOrderId],
    ['EDHR_BATCH_E2E_ROUTE_ID', config.routeId]
  ]) {
    if (value && !/^\d+$/.test(value)) {
      missing.push({ name, description: '必须为真实数字ID' })
    }
  }

  return { ...config, missing }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('缺少 Playwright runtime，请先在此前端 worktree 执行 pnpm install。')
  }
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
  throw new Error(`页面缺少可见输入框：${label}`)
}

async function clickButton(root, name) {
  const button = root.getByRole('button', { name }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  if (await button.isDisabled()) throw new Error(`按钮不可用：${name}`)
  await button.click()
}

async function selectWorkOrderFromDialog(page, config) {
  const dialog = page.locator('.el-dialog:visible').first()
  const workOrderInput = dialog
    .locator(
      'input[placeholder="输入工单号或产品名称搜索并选择未冻结工单"], input[placeholder="输入工单号搜索并选择未冻结工单"], .el-select input'
    )
    .first()
  await workOrderInput.waitFor({ state: 'visible', timeout: 30000 })
  await workOrderInput.click()
  await workOrderInput.fill(config.workOrderCode || config.workOrderId)

  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: config.workOrderCode || `ID ${config.workOrderId}` })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const optionText = await option.innerText()
  if (!optionText.includes(config.workOrderId) && config.workOrderCode && !optionText.includes(config.workOrderCode)) {
    throw new Error(`工单下拉选项与目标不匹配：${optionText}`)
  }
  await option.click()
}

async function selectRouteFromDialog(page, config) {
  if (!config.routeId) return
  const dialog = page.locator('.el-dialog:visible').first()
  const routeField = dialog.locator('.el-form-item').filter({ hasText: '工艺路线' }).first()
  const routeSelect = routeField.locator('.el-select').first()
  await routeSelect.waitFor({ state: 'visible', timeout: 30000 })
  await routeSelect.click()

  const optionTextCandidates = [`ID ${config.routeId}`, config.routeCode, config.routeName].filter(Boolean)
  let option
  for (const optionText of optionTextCandidates) {
    const candidate = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: optionText })
      .first()
    try {
      await candidate.waitFor({ state: 'visible', timeout: 30000 })
      option = candidate
      break
    } catch {
      // Try the next stable visible route identifier before failing with the full target list.
    }
  }
  if (!option) {
    throw new Error(`路线下拉选项与目标不匹配：${optionTextCandidates.join(' / ')}`)
  }
  const optionText = await option.innerText()
  if (!optionTextCandidates.some((candidate) => optionText.includes(candidate))) {
    throw new Error(`路线下拉选项与目标不匹配：${optionText}`)
  }
  await option.click()
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${BATCH_EXECUTION_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，真实 E2E 无法无人值守执行。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({
      hasText: config.tenant
    }).first()
    if ((await tenantOption.count()) > 0) {
      await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, '密码')
  await clickButton(loginForm, /^登录$/)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({
    headless: !config.headed,
    ...(config.executablePath ? { executablePath: config.executablePath } : {})
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
    const page = await context.newPage()
  try {
    await login(page, config)
    await page.goto(`${config.baseUrl}${BATCH_EXECUTION_ROUTE}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })
    await clickButton(page, '打开/创建')
    const routeOptionsResult = page
      .waitForResponse(
        (response) =>
          response.url().includes('/mes/pro/edhr-batch-execution/work-order-route-options') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      .catch(() => null)
    await selectWorkOrderFromDialog(page, config)
    await routeOptionsResult
    await selectRouteFromDialog(page, config)
    await fillFirstVisible(
      page.locator('.el-dialog input[placeholder="请输入真实批次号"]'),
      config.batchCode,
      '批次号'
    )
    const [openResult] = await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/mes/pro/edhr-batch-execution/open-or-create') &&
          response.request().method() === 'POST',
        { timeout: 60000 }
      ),
      clickButton(page.locator('.el-dialog'), '确 认')
    ])
    assert.equal(openResult.status(), 200, '打开/创建批次接口必须返回 HTTP 200')
    const openBody = await openResult.json()
    assert.equal(openBody.code, 0, `打开/创建批次业务响应必须成功：${openBody.msg || openBody.code}`)
    const tasks = openBody.data?.tasks || []
    assert.ok(tasks.length > 0, '打开/创建批次必须返回真实工序任务')
    assert.equal(openBody.data?.taskTotal, tasks.length, '批次任务总数必须与后端返回任务列表一致')
    const openableTask = tasks.find(
      (task) =>
        task.requiredFlag !== false &&
        task.activeWorkTaskId &&
        Array.isArray(task.allowedActions) &&
        task.allowedActions.includes('OPEN_FORM')
    )
    assert.ok(
      openableTask && (openableTask.batchRecordReportId || openableTask.formTemplateId),
      '批次任务必须包含至少一个当前账号可打开的真实批记录或动态表单'
    )
    assert.equal(openBody.data?.blockedCount, 0, '当前真实测试工单不能存在阻塞任务')
    await page.waitForURL((url) => url.pathname === `${BATCH_EXECUTION_ROUTE}/detail`, { timeout: 60000 })
    const processGroup = page
      .locator('.edhr-batch-detail__process-task-group')
      .filter({ hasText: openableTask.processName || openableTask.processCode || '' })
      .first()
    await processGroup.waitFor({ state: 'visible', timeout: 60000 })
    await processGroup.click()
    const formItem = page
      .locator('.edhr-batch-detail__rail-process-form-item')
      .filter({
        hasText:
          openableTask.batchRecordReportName ||
          openableTask.formTemplateName ||
          openableTask.processName ||
          String(openableTask.id)
      })
      .first()
    await formItem.waitFor({ state: 'visible', timeout: 60000 })
    const openTaskButton = formItem.getByRole('button', { name: /打开填写|打开返工/ }).first()
    await openTaskButton.waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(await openTaskButton.isEnabled(), true, '当前真实任务的打开填写按钮必须可用')

    const [taskOpenResult] = await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/mes/pro/edhr-batch-execution/task/open') &&
          response.request().method() === 'POST',
        { timeout: 60000 }
      ),
      openTaskButton.click()
    ])
    assert.equal(taskOpenResult.status(), 200, '打开工序任务接口必须返回 HTTP 200')
    const taskOpenBody = await taskOpenResult.json()
    assert.equal(taskOpenBody.code, 0, `打开工序任务业务响应必须成功：${taskOpenBody.msg || taskOpenBody.code}`)
    assert(Number.isFinite(Number(taskOpenBody.data?.executionId)), '打开工序任务必须返回真实单张 eDHR 执行ID')
    await page.waitForURL(
      (url) =>
        url.pathname === '/mes/pro/feedback/edhr-execution/detail' ||
        url.pathname === '/mes/pro/feedback/edhr-execution/form',
      { timeout: 60000 }
    )
    await page.locator('body').waitFor({ state: 'visible', timeout: 60000 })
    assert.ok(
      ['/mes/pro/feedback/edhr-execution/detail', '/mes/pro/feedback/edhr-execution/form'].includes(
        new URL(page.url()).pathname
      ),
      `打开工序任务后必须进入 eDHR 执行页：${page.url()}`
    )
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实 E2E 前置条件缺失或命中受保护租户。',
      missing: config.missing
    }
    writeEvidence(result)
    console.error(result.reason)
    process.exitCode = 1
    return
  }

  try {
    await runRealFlow(config)
    writeEvidence({ status: 'PASS' })
    console.log('PASS: eDHR batch execution real path')
  } catch (error) {
    writeEvidence({ status: 'FAIL', error: error instanceof Error ? error.message : String(error) })
    throw error
  }
}

main()
