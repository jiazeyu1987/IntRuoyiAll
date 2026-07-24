const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const { chromium } = require('playwright')

const TASK_ID = '20260710-edhr-form-field-responsibility-docs'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-field-responsibility')
const PROJECT_ROOT = path.resolve(process.cwd(), '..')
const DEFAULT_EVIDENCE_FILE = path.resolve(
  PROJECT_ROOT,
  'doc',
  'tasks',
  TASK_ID,
  'field-responsibility-real-e2e-evidence.md'
)

const REQUIRED_BASE_URL = 'http://localhost:8081'
const DEFAULT_TENANT = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const FIELD_AUDIT_ROUTE = '/mes/pro/feedback/edhr-field-audit'
const RESPONSIBILITY_VIEW_QUERY = 'view=responsibility'
const RESPONSIBILITY_SUMMARY_ENDPOINT =
  '/mes/pro/batch-record-execution/field-audit/responsibility-summary'
const RESPONSIBILITY_HISTORY_ENDPOINT =
  '/mes/pro/batch-record-execution/field-audit/responsibility-history'
const RESPONSIBILITY_EXPORT_ENDPOINT =
  '/mes/pro/batch-record-execution/field-audit/responsibility-export'
const ADMIN_READONLY_LABEL = 'admin readonly'

const BDD_SCENARIOS = [
  'BDD: 字段责任汇总可见 -> Given 测试租户存在真实字段审计执行记录 / When 用户登录并打开责任视图 / Then 页面请求真实责任汇总接口并展示字段、当前值、首次填写人与当前操作人。',
  'BDD: 字段责任历史可追溯 -> Given 责任汇总行存在真实审计历史 / When 用户点击查看历史 / Then 页面请求真实责任历史接口并展示变更值、操作人、签名和原因。',
  'BDD: 字段责任证明可导出 -> Given 责任汇总已加载 / When 用户点击责任证明导出 / Then 页面请求真实责任导出接口，返回文件名、contentBase64、sha256 和责任证据状态。',
  'BDD: admin 只读复验 -> Given 芋道源码/admin 登录同一路径 / When 打开责任视图 / Then 不允许产生 MES 写请求，跨租户不可见按只读结果记录。'
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_FIELD_RESPONSIBILITY_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_FIELD_RESPONSIBILITY_TENANT') || DEFAULT_TENANT,
    username: envValue('EDHR_FIELD_RESPONSIBILITY_USERNAME') || DEFAULT_USERNAME,
    password: envValue('EDHR_FIELD_RESPONSIBILITY_PASSWORD') || envValue('EDHR_FIELD_AUDIT_PASSWORD'),
    executionId:
      envValue('EDHR_FIELD_RESPONSIBILITY_EXECUTION_ID') ||
      envValue('EDHR_FIELD_AUDIT_EXECUTION_ID'),
    headed: envValue('EDHR_FIELD_RESPONSIBILITY_HEADED') === '1',
    evidenceFile: envValue('EDHR_FIELD_RESPONSIBILITY_EVIDENCE_FILE') || DEFAULT_EVIDENCE_FILE,
    adminTenant: envValue('EDHR_FIELD_RESPONSIBILITY_ADMIN_TENANT') || ADMIN_TENANT,
    adminUsername: envValue('EDHR_FIELD_RESPONSIBILITY_ADMIN_USERNAME') || ADMIN_USERNAME,
    adminPassword: envValue('EDHR_FIELD_RESPONSIBILITY_ADMIN_PASSWORD'),
    browserExecutable: envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
  }
  return {
    ...config,
    missing: collectInvalidConfig(config)
  }
}

function collectInvalidConfig(config) {
  const invalid = []
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    invalid.push({
      key: 'EDHR_FIELD_RESPONSIBILITY_BASE_URL',
      description: `真实前端入口必须固定为 ${REQUIRED_BASE_URL}。`
    })
  }
  if (config.tenant !== DEFAULT_TENANT || config.username !== DEFAULT_USERNAME) {
    invalid.push({
      key: 'EDHR_FIELD_RESPONSIBILITY_TENANT/USERNAME',
      description: '字段责任写入前置验证只能使用本机测试租户/aoteman。'
    })
  }
  if (!config.password) {
    invalid.push({
      key: 'EDHR_FIELD_RESPONSIBILITY_PASSWORD',
      description: '测试租户密码必须由当前进程环境或登录基线注入；不得写入脚本默认值。'
    })
  }
  if (!/^\d+$/.test(String(config.executionId || ''))) {
    invalid.push({
      key: 'EDHR_FIELD_RESPONSIBILITY_EXECUTION_ID',
      description: '必须提供存在真实字段审计证据的数字型 executionId。'
    })
  }
  if (config.browserExecutable && !fs.existsSync(config.browserExecutable)) {
    invalid.push({
      key: 'PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH',
      description: `指定的 Chromium/Chrome 可执行文件不存在：${config.browserExecutable}`
    })
  }
  return invalid
}

function serializeError(error) {
  if (!error) return undefined
  return {
    name: error.name || 'Error',
    message: error.message || String(error),
    stack: error.stack
  }
}

function writeJsonResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function writeEvidenceMarkdown(result, evidenceFile) {
  ensureDir(path.dirname(evidenceFile))
  const lines = [
    '# eDHR 字段责任真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 生成时间：${new Date().toISOString()}`,
    `- 固定前端入口：\`${REQUIRED_BASE_URL}\``,
    '- 默认测试租户：`测试租户`',
    '- 默认账号名：`aoteman`；密码由环境变量或登录基线注入，不写入仓库证据。',
    '- admin readonly：`芋道源码/admin` 只读复验仅监听并禁止 MES 写请求。',
    '- 真实 E2E 复跑命令：`pnpm e2e:edhr:field-responsibility`',
    '- 静态语法检查命令：`pnpm e2e:edhr:field-responsibility:check`',
    '- 临时产物目录：`test-results/edhr-field-responsibility/`（截图、trace、result.json 与下载文件不提交）',
    `- 当前状态：${result.status}`,
    `- executionId：\`${result.executionId || ''}\``,
    ''
  ]

  lines.push('## BDD')
  lines.push('')
  for (const scenario of BDD_SCENARIOS) lines.push(`- ${scenario}`)
  lines.push('')

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED')
    lines.push('')
    lines.push(`- BLOCKER: \`pnpm e2e:edhr:field-responsibility\` -> FAIL, ${result.reason}`)
    if (result.missing?.length) {
      lines.push('- 不满足的真实 E2E 前置条件：')
      for (const item of result.missing) {
        lines.push(`  - \`${item.key}\`：${item.description}`)
      }
    }
    for (const detail of result.details || []) lines.push(`- ${detail}`)
    lines.push('- 影响：字段责任真实 UI E2E 未放行；不得以接口直调、拦截请求或切换租户替代。')
    lines.push('')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN')
    lines.push('')
    lines.push('- GREEN: `pnpm e2e:edhr:field-responsibility` -> PASS, 责任汇总、历史、导出与 admin 只读复验已完成。')
    for (const step of result.steps || []) {
      lines.push(`- ${step.name} -> PASS${step.screenshot ? `, screenshot: \`${step.screenshot}\`` : ''}`)
    }
    lines.push(`- Trace: \`${result.trace}\``)
    lines.push('')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED')
    lines.push('')
    lines.push(`- RED: \`pnpm e2e:edhr:field-responsibility\` -> FAIL, ${result.error?.message || '未知错误'}`)
    lines.push('- 影响：字段责任真实 UI E2E 未放行。')
    lines.push('')
  }

  fs.writeFileSync(evidenceFile, `${lines.join('\n')}\n`, 'utf8')
}

async function screenshot(page, name, steps) {
  ensureDir(RESULT_DIR)
  const fileName = `${String(steps.length + 1).padStart(2, '0')}-${name}.png`
  const filePath = path.join(RESULT_DIR, fileName)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.fill(value)
      return
    }
  }
  throw blocked(`缺少可填写控件：${label}`)
}

async function clickVisibleButton(scope, namePattern, failureMessage) {
  const button = scope.getByRole('button', { name: namePattern }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await button.click()
}

async function waitForText(page, textOrPattern, failureMessage) {
  const locator = page.getByText(textOrPattern).first()
  await locator.waitFor({ state: 'visible', timeout: 30000 }).catch((error) => {
    throw new Error(`${failureMessage}: ${error.message}`)
  })
}

async function login(page, config, redirectPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page
    .locator('form.login-form')
    .filter({ has: page.getByPlaceholder('请输入用户名') })
    .first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = loginForm.locator('input.el-select__input:visible').first()
  await tenantInput.click()
  await page.keyboard.press('Control+A')
  await page.keyboard.type(config.tenant)
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: config.tenant }).first()
  if ((await option.count()) > 0) {
    await option.click()
  } else {
    await page.keyboard.press('Enter')
  }

  await fillFirstVisible(loginForm.locator('input.el-input__inner:not([role="combobox"])').first(), config.username, '用户名')
  await fillFirstVisible(loginForm.locator('input[type="password"]'), config.password, '密码')
  const captchaInput = loginForm.locator('input[placeholder*="验证码"]').first()
  if ((await captchaInput.count()) > 0 && (await captchaInput.isVisible())) {
    await captchaInput.fill('1')
  }

  await clickVisibleButton(loginForm, /^登录$/, '登录页缺少登录按钮。')
  await page.waitForURL((url) => url.pathname !== '/login', { timeout: 60000 })
}

function buildResponsibilityUrl(config) {
  return `${config.baseUrl}${FIELD_AUDIT_ROUTE}?executionId=${config.executionId}&${RESPONSIBILITY_VIEW_QUERY}`
}

function parseBusinessData(payload, label, config) {
  const code = payload?.code
  if (code !== 0 && code !== 200) {
    const msg = payload?.msg || payload?.message || JSON.stringify(payload)
    if (String(msg).includes('不存在') || String(msg).includes('缺少') || String(msg).includes('无权限')) {
      throw blocked(`本机测试租户缺少可用于字段责任真实 E2E 的 active executionId=${config.executionId}：${label} 返回 ${code} ${msg}`)
    }
    throw new Error(`${label} 业务状态码应为 0 或 200，实际 ${code}：${msg}`)
  }
  return payload.data
}

async function parseJsonResponse(response, label, config) {
  assert.equal(response.status(), 200, `${label} HTTP 状态应为 200，实际 ${response.status()}，URL: ${response.url()}`)
  return parseBusinessData(await response.json(), label, config)
}

function chooseResponsibilityRow(summary) {
  assert.ok(summary && typeof summary === 'object', '字段责任汇总响应必须是对象。')
  assert.ok(Array.isArray(summary.list), '字段责任汇总响应必须包含 list。')
  assert.ok(summary.list.length > 0, '字段责任汇总必须返回真实责任行。')
  const visibleValueRow = summary.list.find((row) =>
    row.evidenceStatus === 'COMPLETE' &&
    row.historyCount > 0 &&
    String(row.currentValueDisplay || '').trim()
  )
  const completeRow = summary.list.find((row) => row.evidenceStatus === 'COMPLETE' && row.historyCount > 0)
  const row = visibleValueRow || completeRow || summary.list.find((item) => item.historyCount > 0) || summary.list[0]
  for (const field of ['fieldPath', 'fieldKey', 'currentValueHash', 'valueOrigin', 'evidenceStatus']) {
    assert.ok(row[field] !== undefined && row[field] !== null && String(row[field]).trim(), `责任行缺少 ${field}`)
  }
  if (row.evidenceStatus === 'COMPLETE') {
    assert.ok(row.firstHumanActorName || row.currentValueActorName, '完整责任行必须展示实际操作人。')
  }
  return row
}

async function openResponsibilitySummary(page, config) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes(RESPONSIBILITY_SUMMARY_ENDPOINT) &&
      new URL(response.url()).searchParams.get('executionId') === String(config.executionId),
    { timeout: 60000 }
  )
  await page.goto(buildResponsibilityUrl(config), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await waitForText(page, /当前责任汇总|责任视图仅展示实际证据/, '未进入字段责任汇总视图')
  const response = await responsePromise
  const summary = await parseJsonResponse(response, '字段责任汇总', config)
  const row = chooseResponsibilityRow(summary)
  const bodyText = (await page.locator('body').textContent({ timeout: 30000 })) || ''
  assert.ok(bodyText.includes(row.fieldLabel || row.fieldKey), '责任汇总页面未展示字段身份。')
  const currentValueDisplay = String(row.currentValueDisplay || '').trim()
  if (currentValueDisplay) {
    assert.ok(bodyText.includes(currentValueDisplay), '责任汇总页面未展示当前值。')
  } else {
    const rowLocator = page
      .locator('.edhr-field-audit__responsibility-summary .el-table__body .el-table__row')
      .filter({ hasText: row.fieldLabel || row.fieldKey })
      .first()
    await rowLocator.waitFor({ state: 'visible', timeout: 30000 })
    await rowLocator.locator('.el-table__expand-icon, .el-table__expand-column .cell').first().click()
    const expandedText = (await page.locator('body').textContent({ timeout: 30000 })) || ''
    assert.ok(expandedText.includes(row.currentValueHash), '责任汇总页面未展示当前值哈希。')
  }
  return { response, summary, row }
}

async function openResponsibilityHistory(page, config, row) {
  assert.ok(Number(row.historyCount) > 0, '责任历史 E2E 需要 historyCount > 0 的真实责任行。')
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes(RESPONSIBILITY_HISTORY_ENDPOINT) &&
      new URL(response.url()).searchParams.get('executionId') === String(config.executionId),
    { timeout: 60000 }
  )
  const rowLocator = page
    .locator('.edhr-field-audit__responsibility-summary .el-table__body .el-table__row')
    .filter({ hasText: row.fieldLabel || row.fieldKey })
    .first()
  await rowLocator.waitFor({ state: 'visible', timeout: 30000 })
  await clickVisibleButton(rowLocator, /查看历史/, '责任汇总目标行缺少“查看历史”按钮。')
  const response = await responsePromise
  const history = await parseJsonResponse(response, '字段责任历史', config)
  assert.ok(Array.isArray(history.list), '字段责任历史必须包含 list。')
  assert.ok(history.list.length > 0, '字段责任历史必须返回真实变更记录。')
  await page.waitForFunction(
    () => Array.from(document.querySelectorAll('.edhr-field-audit__history-dialog, .el-dialog, .el-overlay'))
      .some((element) => {
        const rect = element.getBoundingClientRect()
        const style = window.getComputedStyle(element)
        const text = element.textContent || ''
        return rect.width > 0 &&
          rect.height > 0 &&
          style.display !== 'none' &&
          style.visibility !== 'hidden' &&
          text.includes('字段责任历史') &&
          text.includes('审计序号')
      }),
    null,
    { timeout: 30000 }
  ).catch((error) => {
    throw new Error(`字段责任历史弹窗未展示。: ${error.message}`)
  })
  return { response, history }
}

function decodeResponsibilityExport(payload) {
  assert.ok(payload && typeof payload === 'object', '字段责任导出响应必须是对象。')
  for (const field of ['fileName', 'contentType', 'contentBase64', 'sha256', 'recordCount', 'evidenceStatus']) {
    assert.ok(payload[field] !== undefined && payload[field] !== null && String(payload[field]).trim(), `字段责任导出缺少 ${field}`)
  }
  const bytes = Buffer.from(payload.contentBase64, 'base64')
  assert.ok(bytes.length > 0, '字段责任导出 contentBase64 解码后不能为空。')
  const sha256 = crypto.createHash('sha256').update(bytes).digest('hex')
  assert.equal(sha256, payload.sha256, '字段责任导出 sha256 必须与 contentBase64 计算结果一致。')
  return bytes
}

async function exportResponsibility(page, config) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes(RESPONSIBILITY_EXPORT_ENDPOINT) &&
      new URL(response.url()).searchParams.get('executionId') === String(config.executionId),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /责任证明导出/, '字段责任汇总页缺少“责任证明导出”按钮。')
  const response = await responsePromise
  const exportData = await parseJsonResponse(response, '字段责任导出', config)
  const bytes = decodeResponsibilityExport(exportData)
  const savedFilePath = path.join(RESULT_DIR, exportData.fileName)
  ensureDir(RESULT_DIR)
  fs.writeFileSync(savedFilePath, bytes)
  return { response, exportData, savedFilePath }
}

async function verifyAdminReadonly(browser, config) {
  if (!config.adminPassword) {
    throw blocked('缺少 EDHR_FIELD_RESPONSIBILITY_ADMIN_PASSWORD，无法执行芋道源码/admin 只读复验。')
  }
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai'
  })
  const page = await context.newPage()
  const writeRequests = []
  page.on('request', (request) => {
    const method = request.method()
    const url = request.url()
    if (url.includes('/admin-api/mes/pro/') && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      writeRequests.push(`${method} ${url}`)
    }
  })
  try {
    await login(
      page,
      {
        ...config,
        tenant: config.adminTenant,
        username: config.adminUsername,
        password: config.adminPassword
      },
      `${FIELD_AUDIT_ROUTE}?executionId=${config.executionId}&${RESPONSIBILITY_VIEW_QUERY}`
    )
    const responsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes(RESPONSIBILITY_SUMMARY_ENDPOINT),
      { timeout: 60000 }
    )
    await page.goto(buildResponsibilityUrl(config), { waitUntil: 'domcontentloaded', timeout: 60000 })
    const response = await responsePromise
    assert.equal(response.status(), 200, `${ADMIN_READONLY_LABEL} HTTP 状态应为 200。`)
    await response.json()
    assert.deepEqual(writeRequests, [], `${ADMIN_READONLY_LABEL} 不得发送 MES 写请求：${writeRequests.join('; ')}`)
  } finally {
    await context.close()
  }
}

async function runRealFlow(config) {
  ensureDir(RESULT_DIR)
  const launchOptions = { headless: !config.headed }
  if (config.browserExecutable) {
    launchOptions.executablePath = config.browserExecutable
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
    acceptDownloads: true
  })
  const tracePath = path.join(RESULT_DIR, 'trace.zip')
  const page = await context.newPage()
  const steps = []
  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  try {
    await login(page, config, `${FIELD_AUDIT_ROUTE}?executionId=${config.executionId}&${RESPONSIBILITY_VIEW_QUERY}`)
    const summaryResult = await openResponsibilitySummary(page, config)
    steps.push({
      name: '字段责任汇总可见',
      screenshot: await screenshot(page, 'responsibility-summary', steps),
      summary: summaryResult.summary
    })

    const historyResult = await openResponsibilityHistory(page, config, summaryResult.row)
    steps.push({
      name: '字段责任历史可追溯',
      screenshot: await screenshot(page, 'responsibility-history', steps),
      history: historyResult.history
    })

    await page.keyboard.press('Escape')
    const exportResult = await exportResponsibility(page, config)
    steps.push({
      name: '字段责任证明可导出',
      screenshot: await screenshot(page, 'responsibility-export', steps),
      export: exportResult.exportData
    })

    await verifyAdminReadonly(browser, config)
    steps.push({
      name: 'admin readonly 无写请求'
    })

    await context.tracing.stop({ path: tracePath })
    await context.close()
    await browser.close()
    return {
      status: 'PASS',
      steps,
      trace: tracePath,
      executionId: String(config.executionId)
    }
  } catch (error) {
    try {
      await context.tracing.stop({ path: tracePath })
    } catch (traceError) {
      error.message = `${error.message}; trace 写入失败: ${traceError instanceof Error ? traceError.message : String(traceError)}`
    }
    await context.close()
    await browser.close()
    throw Object.assign(error, { tracePath, steps, executionId: String(config.executionId) })
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实字段责任 E2E 前置条件不满足。',
      missing: config.missing,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: config.evidenceFile,
      executionId: String(config.executionId || '')
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    process.exitCode = 1
    return
  }

  try {
    const result = await runRealFlow(config)
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
  } catch (error) {
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.message || String(error),
      details: error.details || [],
      error: serializeError(error),
      trace: error.tracePath,
      steps: error.steps || [],
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: config.evidenceFile,
      executionId: String(error.executionId || config.executionId)
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    process.exitCode = 1
  }
}

main()
