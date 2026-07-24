const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const REQUIRED_TENANT = '测试租户'
const REQUIRED_USERNAME = 'aoteman'
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])
const DEFAULT_ROUTE = '/mes/pro/feedback/edhr-report'

const BDD_SCENARIOS = [
  'BDD: 报表目录真实可见 -> Given 测试租户用户通过真实登录页进入系统 When 打开 eDHR 报表目录 Then 页面展示统一追溯报表和 12 类标准报表目录。',
  'BDD: 标准报表只读查询 -> Given 页面已选择已发布标准报表 When 用户点击只读查询 Then 前端通过真实页面请求 `/edhr-report-query/run` 并展示口径版本、筛选快照、权限摘要和结果行。',
  'BDD: 导出审计真实记录 -> Given 只读查询已经返回结果 When 用户点击导出审计 Then 前端通过真实页面请求 `/edhr-report-query/export-audit`，审计列表展示 RECORDED 记录。',
  'BDD: 缺前置即阻塞 -> Given 缺少真实前端、测试租户账号或页面路由 When 脚本执行 Then 直接失败并写入证据，不切换租户、不使用 mock 或接口绕过。'
]

function envValue(name) {
  return String(process.env[name] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function collectConfig() {
  const missing = []
  const config = {
    baseUrl: envValue('EDHR_REPORT_E2E_BASE_URL').replace(/\/+$/, ''),
    tenant: envValue('EDHR_REPORT_E2E_TENANT'),
    username: envValue('EDHR_REPORT_E2E_USERNAME'),
    password: envValue('EDHR_REPORT_E2E_PASSWORD'),
    route: envValue('EDHR_REPORT_E2E_ROUTE') || DEFAULT_ROUTE,
    evidenceFile:
      envValue('EDHR_REPORT_E2E_EVIDENCE_FILE') ||
      path.resolve(process.cwd(), 'test-results', 'edhr-report', 'real-e2e-evidence.md'),
    screenshotFile:
      envValue('EDHR_REPORT_E2E_SCREENSHOT_FILE') ||
      path.resolve(process.cwd(), 'test-results', 'edhr-report', 'real-e2e.png'),
    headed: envValue('EDHR_REPORT_E2E_HEADED') === '1'
  }

  for (const [name, description] of [
    ['EDHR_REPORT_E2E_BASE_URL', '本机真实前端入口，例如 http://127.0.0.1:8096'],
    ['EDHR_REPORT_E2E_TENANT', `必须为 ${REQUIRED_TENANT}`],
    ['EDHR_REPORT_E2E_USERNAME', `必须为 ${REQUIRED_USERNAME}`],
    ['EDHR_REPORT_E2E_PASSWORD', '测试租户账号真实登录密码']
  ]) {
    if (!envValue(name)) missing.push({ name, description })
  }

  if (config.tenant && config.tenant !== REQUIRED_TENANT) {
    missing.push({
      name: 'EDHR_REPORT_E2E_TENANT',
      description: `当前值为 ${config.tenant}；本脚本只允许 ${REQUIRED_TENANT}。`
    })
  }
  if (config.tenant && FORBIDDEN_TENANTS.has(config.tenant.toLowerCase())) {
    missing.push({
      name: 'EDHR_REPORT_E2E_TENANT',
      description: '当前租户命中 live/prod 保护名单。'
    })
  }
  if (config.username && config.username !== REQUIRED_USERNAME) {
    missing.push({
      name: 'EDHR_REPORT_E2E_USERNAME',
      description: `当前值为 ${config.username}；本脚本只允许 ${REQUIRED_USERNAME}。`
    })
  }
  if (config.route && !config.route.startsWith('/')) {
    missing.push({ name: 'EDHR_REPORT_E2E_ROUTE', description: '目标路由必须以 / 开头。' })
  }

  return { ...config, missing }
}

function writeEvidence(config, status, details) {
  ensureDir(path.dirname(config.evidenceFile))
  const lines = [
    '# eDHR 报表目录真实 E2E 证据',
    '',
    `- 状态：${status}`,
    `- 前端入口：${config.baseUrl || '(missing)'}`,
    `- 租户：${config.tenant || '(missing)'}`,
    `- 账号：${config.username || '(missing)'}`,
    `- 路由：${config.route || '(missing)'}`,
    `- 截图：${details.screenshotFile || '(未生成)'}`,
    '',
    '## BDD',
    '',
    ...BDD_SCENARIOS.map((scenario) => `- \`${scenario}\``),
    '',
    '## 结果',
    '',
    ...Object.entries(details)
      .filter(([key]) => key !== 'error')
      .map(([key, value]) => `- ${key}: ${typeof value === 'string' ? value : JSON.stringify(value)}`),
    '',
    details.error ? '## Error' : '',
    details.error ? '' : '',
    details.error ? `\`\`\`\n${details.error.stack || details.error.message || String(details.error)}\n\`\`\`` : ''
  ].filter((line) => line !== undefined)
  fs.writeFileSync(config.evidenceFile, `${lines.join('\n')}\n`, 'utf8')
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error('缺少 Playwright runtime，请先在 yudao-ui-admin-vue3 执行 pnpm install。')
    blocked.cause = error
    throw blocked
  }
}

function parseBusinessData(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 响应必须是对象。`)
  if (Object.prototype.hasOwnProperty.call(body, 'code')) {
    assert.ok(
      body.code === 0 || body.code === 200,
      `${label} 业务状态码应为 0 或 200，实际 ${body.code}: ${body.msg || body.message || ''}`
    )
    return body.data
  }
  return body
}

async function parseJsonResponse(response, label) {
  assert.equal(response.status(), 200, `${label} HTTP 状态应为 200，实际 ${response.status()}`)
  return parseBusinessData(await response.json(), label)
}

async function fillVisible(locator, value, message) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(message)
}

async function login(page, config) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', config.route)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill('')
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillVisible(form.locator('input.el-input__inner').first(), config.tenant, '缺少租户输入框。')
  }

  await fillVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    '缺少账号输入框。'
  )
  await fillVisible(form.locator('input[type="password"]'), config.password, '缺少密码输入框。')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  await parseJsonResponse(loginResponse, '登录')
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function clickEnabledButton(page, name, message) {
  const button = page.getByRole('button', { name }).first()
  await button.waitFor({ state: 'visible', timeout: 60000 })
  const deadline = Date.now() + 60000
  while (Date.now() < deadline) {
    if (!(await button.isDisabled())) {
      await button.click()
      return
    }
    await page.waitForTimeout(500)
  }
  throw new Error(message)
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const error = new Error(`缺少或非法 E2E 前置：${JSON.stringify(config.missing, null, 2)}`)
    writeEvidence(config, 'BLOCKED', { missing: config.missing, error })
    throw error
  }

  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 980 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page, config)

    const catalogPromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/edhr-report-catalog/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    const definitionPromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/edhr-report-definition/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    ).then((response) => ({ response })).catch((error) => ({ error }))
    await page.goto(new URL(config.route, config.baseUrl).toString(), { waitUntil: 'domcontentloaded' })
    await page.getByText('统一追溯报表', { exact: false }).first().waitFor({ state: 'visible' })

    const catalogData = await parseJsonResponse(await catalogPromise, '报表目录')
    assert.equal(catalogData.total, 12, `报表目录 total 应为 12，实际 ${catalogData.total}`)
    assert.ok(Array.isArray(catalogData.list) && catalogData.list.length > 0, '报表目录 list 不能为空。')
    const definitionResult = await definitionPromise
    if (definitionResult.error) {
      throw definitionResult.error
    }
    const definitionData = await parseJsonResponse(definitionResult.response, '报表定义')
    assert.ok(
      Array.isArray(definitionData.list) && definitionData.list.some((item) => item.status === 'PUBLISHED'),
      '首个标准报表必须有已发布定义。'
    )

    const runPromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/edhr-report-query/run') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await clickEnabledButton(page, /只读查询/, '只读查询按钮未变为可用。')
    const runData = await parseJsonResponse(await runPromise, '只读查询')
    assert.ok(runData.caliberVersion, '只读查询必须返回 caliberVersion。')
    assert.ok(runData.filterSnapshotJson, '只读查询必须返回 filterSnapshotJson。')
    assert.ok(runData.permissionSummaryJson, '只读查询必须返回 permissionSummaryJson。')
    assert.ok(Array.isArray(runData.rows) && runData.rows.length > 0, '只读查询必须返回结果行。')

    const auditCreatePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-report-query/export-audit') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    const auditPagePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-report-query/export-audit/page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await clickEnabledButton(page, /导出审计/, '导出审计按钮未变为可用。')
    const auditCreateData = await parseJsonResponse(await auditCreatePromise, '导出审计创建')
    assert.equal(auditCreateData.resultStatus, 'RECORDED', '导出审计创建结果必须为 RECORDED。')
    const auditPageData = await parseJsonResponse(await auditPagePromise, '导出审计列表')
    assert.ok(
      Array.isArray(auditPageData.list) &&
        auditPageData.list.some((item) => item.id === auditCreateData.id && item.resultStatus === 'RECORDED'),
      '导出审计列表必须包含本次 RECORDED 记录。'
    )

    ensureDir(path.dirname(config.screenshotFile))
    await page.screenshot({ path: config.screenshotFile, fullPage: true })
    writeEvidence(config, 'PASS', {
      catalogTotal: catalogData.total,
      selectedReportCode: runData.reportCode,
      caliberVersion: runData.caliberVersion,
      queryRowCount: runData.rows.length,
      auditId: auditCreateData.id,
      auditStatus: auditCreateData.resultStatus,
      screenshotFile: config.screenshotFile
    })
  } catch (error) {
    writeEvidence(config, 'FAIL', { screenshotFile: config.screenshotFile, error })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
