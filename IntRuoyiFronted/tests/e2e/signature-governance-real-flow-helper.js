const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'signature-governance')
const FORBIDDEN_LIVE_TENANTS = new Set(['芋道源码', 'yudao', 'Yudao', 'YUDAO'])
const SIGNATURE_TAB_PATHS = {
  签名记录: '/signature-governance/signature-records',
  我的签名: '/signature-governance/my-signature',
  用户授权: '/signature-governance/authorizations',
  长期留存: '/signature-governance/retention',
  周期复核: '/signature-governance/periodic-review',
  CSV质量包: '/signature-governance/csv-package',
  统一策略: '/signature-governance/policy'
}
const AUTO_FILL_SOURCE_ENDPOINTS = [
  '/mes/pro/batch-record-execution-archive/page',
  '/mes/pro/edhr-release/page',
  '/mes/pro/edhr-validation-package/page',
  '/dcc/training-executions/page',
  '/mes/pro/edhr-change/page'
]

const COMMON_ENV = [
  ['SIGNATURE_GOVERNANCE_E2E_BASE_URL', 'current frontend URL that serves this worktree code'],
  ['SIGNATURE_GOVERNANCE_E2E_TENANT', 'non-production tenant name'],
  ['SIGNATURE_GOVERNANCE_E2E_USERNAME', 'test user account'],
  ['SIGNATURE_GOVERNANCE_E2E_PASSWORD', 'test user password']
]

const RETENTION_ENV = []
const REVIEW_ENV = []
const CSV_ENV = []

function envValue(name) {
  const rawValue = process.env[name]
  return typeof rawValue === 'string' ? rawValue.trim() : ''
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function writeResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(
    path.join(RESULT_DIR, `${result.scenario}.json`),
    `${JSON.stringify(result, null, 2)}\n`,
    'utf8'
  )
}

function collectConfig(scenario) {
  const scenarioEnv =
    scenario === 'retention'
      ? RETENTION_ENV
      : scenario === 'periodic-review'
        ? REVIEW_ENV
        : scenario === 'csv-package'
          ? CSV_ENV
          : []
  const requiredEnv = [...COMMON_ENV, ...scenarioEnv]
  const missing = requiredEnv.filter(([name]) => !envValue(name)).map(([name, description]) => ({
    name,
    description
  }))
  if (missing.length > 0) {
    const result = {
      scenario,
      status: 'BLOCKED',
      reason: 'missing-prerequisite',
      missing,
      impact: 'Real signature governance E2E cannot run until the current frontend and backend code are deployed with test tenant data.'
    }
    writeResult(result)
    throw new Error(
      `Missing signature governance E2E prerequisites: ${missing.map((item) => item.name).join(', ')}`
    )
  }

  const tenant = envValue('SIGNATURE_GOVERNANCE_E2E_TENANT')
  if (FORBIDDEN_LIVE_TENANTS.has(tenant)) {
    const result = {
      scenario,
      status: 'BLOCKED',
      reason: 'forbidden-live-tenant',
      tenant,
      impact: 'The test would target a protected tenant and must not run.'
    }
    writeResult(result)
    throw new Error('SIGNATURE_GOVERNANCE_E2E_TENANT targets a protected tenant')
  }

  return {
    baseUrl: envValue('SIGNATURE_GOVERNANCE_E2E_BASE_URL').replace(/\/+$/, ''),
    tenant,
    username: envValue('SIGNATURE_GOVERNANCE_E2E_USERNAME'),
    password: envValue('SIGNATURE_GOVERNANCE_E2E_PASSWORD'),
    retention: {},
    review: {},
    csv: {},
    headed: envValue('SIGNATURE_GOVERNANCE_E2E_HEADED') === '1'
  }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for signature governance real E2E verification.')
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
  throw new Error(`No visible input found for ${label}`)
}

async function fillFirstVisibleIfPresent(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return true
    }
  }
  return false
}

async function clickVisibleButton(root, text) {
  const button = root.locator(`button:has-text("${text}")`).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  const deadline = Date.now() + 30000
  while ((await button.isDisabled()) && Date.now() < deadline) {
    await button.page().waitForTimeout(250)
  }
  if (await button.isDisabled()) {
    throw new Error(`Button is disabled: ${text}`)
  }
  await button.click()
}

async function selectTenant(page, tenant) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    await fillFirstVisibleIfPresent(page.locator('input[placeholder="请输入租户名称"]'), tenant)
    return
  }
  await tenantSelect.click()
  const input = page.locator('.login-form .el-select__input').first()
  await input.fill(tenant)
  await page.keyboard.press('Enter')
  await page.waitForTimeout(100)
}

async function readSelectedTenant(page) {
  const tenantField = page.locator('.login-form .el-form-item').filter({
    has: page.locator('.el-select')
  }).first()
  if ((await tenantField.count()) === 0 || !(await tenantField.isVisible())) {
    const tenantInput = page.locator('input[placeholder="请输入租户名称"]').first()
    return (await tenantInput.count()) > 0 ? (await tenantInput.inputValue()).trim() : ''
  }
  return (await tenantField.innerText()).trim()
}

async function assertSelectedTenant(page, expectedTenant) {
  const deadline = Date.now() + 3000
  let actualTenant = ''
  do {
    actualTenant = await readSelectedTenant(page)
    if (actualTenant === expectedTenant) {
      return
    }
    await page.waitForTimeout(100)
  } while (Date.now() < deadline)
  throw new Error(`Login tenant mismatch: expected ${expectedTenant}, actual ${actualTenant || '<empty>'}`)
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=/signature-governance/signature-records`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 })

  if (page.url().includes('/login')) {
    if ((await page.locator('.login-form img').count()) > 0) {
      throw new Error('Login captcha is enabled; real E2E cannot run unattended.')
    }
    await selectTenant(page, config.tenant)
    await assertSelectedTenant(page, config.tenant)
    await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
    await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), config.password, 'password')
    await clickVisibleButton(page, '登录')
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  }

  if (!page.url().includes('/signature-governance/signature-records')) {
    await page.goto(`${config.baseUrl}/signature-governance/signature-records`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
  }
  await page
    .locator('[data-user-table-key="signature.governance.records"]')
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
}

async function readCommonResult(response, label) {
  if (!response.ok()) {
    throw new Error(`${label} HTTP ${response.status()} from ${response.url()}`)
  }
  const payload = await response.json()
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    const code = Number(payload.code)
    if (code !== 0 && code !== 200) {
      throw new Error(`${label} failed with code=${payload.code}, msg=${payload.msg || payload.message || ''}`)
    }
    return payload.data
  }
  return payload
}

async function waitForEndpoint(page, endpoint, method, label, action) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes(endpoint) && response.request().method() === method,
    { timeout: 60000 }
  )
  await action()
  return readCommonResult(await responsePromise, label)
}

function assertBlocked(data, blockerCode, label) {
  assert.equal(data.status, 'BLOCKED', `${label} must return BLOCKED`)
  assert.ok(Array.isArray(data.blockers), `${label} must return blocker array`)
  assert.ok(
    data.blockers.some((blocker) => blocker.code === blockerCode),
    `${label} must include ${blockerCode}`
  )
}

function assertReady(data, label) {
  assert.equal(data.status, 'READY', `${label} must return READY`)
  assert.equal(data.ready, true, `${label} must be ready`)
  assert.ok(Array.isArray(data.blockers), `${label} must return blocker array`)
  assert.equal(data.blockers.length, 0, `${label} must not return blockers`)
}

function assertRecorded(data, label) {
  assert.equal(data.status, 'RECORDED', `${label} must return RECORDED`)
  assert.equal(data.recorded, true, `${label} must be recorded`)
  assert.ok(data.receiptId, `${label} must return receiptId`)
}

function assertPassed(data, label) {
  assert.equal(data.status, 'PASSED', `${label} must return PASSED`)
  assert.equal(data.passed, true, `${label} must be passed`)
}

function assertReviewResult(data) {
  assert.ok(['BLOCKED', 'COLLECTED', 'SIGNED', 'CLOSED'].includes(data.status), 'periodic review status must be valid')
  assert.ok(Array.isArray(data.blockers), 'periodic review must return blocker array')
  assert.ok(Array.isArray(data.snapshotItems), 'periodic review must return snapshot item array')
  assert.equal(
    data.blockers.some((blocker) => blocker.code === 'REVIEW_OWNER_MISSING'),
    false,
    'periodic review must not omit the filled review owner'
  )
}

function assertCsvGateResult(data) {
  assert.ok(['GO', 'BLOCKED'].includes(data.status), 'CSV release gate status must be valid')
  assert.ok(Array.isArray(data.blockers), 'CSV release gate must return blocker array')
  assert.equal(data.engineeringVerificationPassed, true, 'engineering verification must be submitted as true')
  assert.equal(
    data.blockers.some((blocker) => blocker.code === 'QA_APPROVAL_MISSING'),
    false,
    'CSV release gate must not omit the filled QA approval'
  )
}

async function activePanel(page, tabText) {
  const expectedPath = SIGNATURE_TAB_PATHS[tabText]
  assert.ok(expectedPath, `unknown signature governance tab: ${tabText}`)
  await page.goto(`${page.url().split('/signature-governance')[0]}${expectedPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.waitForURL((url) => url.pathname === expectedPath, { timeout: 30000 })
  const panel = page.locator('.signature-governance').first()
  await panel.locator(`text=${tabText}`).first().waitFor({ state: 'visible', timeout: 30000 })
  return panel
}

async function fillFormItem(panel, label, value) {
  await fillFirstVisible(panel.locator('.el-form-item').filter({ hasText: label }).locator('input'), value, label)
}

async function loadDccSignatureCandidate(page, panel) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/electronic-signatures/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickVisibleButton(panel, '加载真实样本')
  const data = await readCommonResult(await responsePromise, 'DCC signature candidate page')
  assert.ok(Array.isArray(data.list), 'DCC signature candidate page must return list')
  assert.ok(data.list.length > 0, 'DCC signature candidate page must contain real rows')
  await panel.locator('text=真实文件签名样本').first().waitFor({ state: 'visible', timeout: 30000 })
  await clickVisibleButton(panel, '使用此样本自动回填')
  return data.list[0]
}

async function setSwitchOn(panel, label) {
  const switchControl = panel.locator('.el-form-item').filter({ hasText: label }).locator('.el-switch').first()
  await switchControl.waitFor({ state: 'visible', timeout: 30000 })
  const className = await switchControl.getAttribute('class')
  if (!className || !className.includes('is-checked')) {
    await switchControl.click()
  }
}

async function runRetentionScenario(page, config) {
  const panel = await activePanel(page, '长期留存')
  await loadDccSignatureCandidate(page, panel)
  const data = await waitForEndpoint(
    page,
    '/signature-governance/retention/precheck',
    'POST',
    'retention precheck',
    () => clickVisibleButton(panel, '执行预检')
  )
  assertReady(data, 'retention precheck')
  await page.locator('text=READY').first().waitFor({ state: 'visible', timeout: 30000 })

  const dccReceipt = await waitForEndpoint(
    page,
    '/signature-governance/retention/dcc-evidence-receipts',
    'POST',
    'DCC retention receipt',
    () => clickVisibleButton(panel, '记录DCC回执')
  )
  assertRecorded(dccReceipt, 'DCC retention receipt')

  await clickVisibleButton(panel, '加载eDHR归档样本')
  const edhrReceipt = await waitForEndpoint(
    page,
    '/signature-governance/retention/edhr-archive-receipts',
    'POST',
    'eDHR retention receipt',
    () => clickVisibleButton(panel, '记录eDHR回执')
  )
  assertRecorded(edhrReceipt, 'eDHR retention receipt')

  await panel.locator('.el-form-item').filter({ hasText: '恢复对象Key' }).locator('input:disabled').first()
    .waitFor({ state: 'visible', timeout: 30000 })
}

async function runReviewScenario(page, config) {
  const panel = await activePanel(page, '周期复核')
  await loadDccSignatureCandidate(page, panel)
  const data = await waitForEndpoint(
    page,
    '/signature-governance/periodic-review/batches',
    'POST',
    'periodic review',
    () => clickVisibleButton(panel, '创建批次')
  )
  assertReviewResult(data)
  await page.locator('text=审阅批次').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function runCsvScenario(page, config) {
  const panel = await activePanel(page, 'CSV质量包')
  await loadDccSignatureCandidate(page, panel)
  await clickVisibleButton(panel, '加载CSV来源样本')
  await panel.locator('text=材料类型').first().waitFor({ state: 'visible', timeout: 30000 })
  await panel.locator('text=追溯关系').first().waitFor({ state: 'visible', timeout: 30000 })
  await panel.locator('text=培训记录').first().waitFor({ state: 'visible', timeout: 30000 })
  await panel.locator('text=变更控制').first().waitFor({ state: 'visible', timeout: 30000 })
  await panel.locator('text=QA批准').first().waitFor({ state: 'visible', timeout: 30000 })
  const data = await waitForEndpoint(
    page,
    '/signature-governance/csv/packages/',
    'POST',
    'CSV release gate',
    () => clickVisibleButton(panel, '评估门禁')
  )
  assertCsvGateResult(data)
  await page.locator('text=发布门禁').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function runPolicyScenario(page) {
  const policyResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/signature-governance/policies/current') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${page.url().split('/signature-governance')[0]}${SIGNATURE_TAB_PATHS.统一策略}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const policyData = await readCommonResult(await policyResponsePromise, 'current policy')
  assertReady(policyData, 'current policy')
  assert.ok(Array.isArray(policyData.moduleStatuses), 'current policy must return module statuses')
  assert.equal(policyData.moduleStatuses.length, 4, 'current policy must cover DCC, eDHR, Showroom, and IntAuth')
  assert.equal(
    policyData.moduleStatuses.every((moduleStatus) => moduleStatus.policySourcePresent === true),
    true,
    'current policy must expose a configured policy source for every module'
  )
  assert.equal(
    policyData.moduleStatuses.every((moduleStatus) => moduleStatus.authorityConfirmed === true),
    true,
    'current policy authority must be confirmed for every module'
  )
  await activePanel(page, '统一策略')
  await page.locator('text=READY').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function runSignatureGovernanceScenario(scenario) {
  const config = collectConfig(scenario)
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } })
  const page = await context.newPage()
  try {
    await login(page, config)
    if (scenario === 'retention') {
      await runRetentionScenario(page, config)
    } else if (scenario === 'periodic-review') {
      await runReviewScenario(page, config)
    } else if (scenario === 'csv-package') {
      await runCsvScenario(page, config)
    } else if (scenario === 'policy') {
      await runPolicyScenario(page)
    } else {
      throw new Error(`Unknown signature governance scenario: ${scenario}`)
    }
    const result = {
      scenario,
      status: 'PASS',
      verifiedAt: new Date().toISOString()
    }
    writeResult(result)
    console.log(`PASS: signature governance ${scenario} E2E`)
  } catch (error) {
    const result = {
      scenario,
      status: 'FAIL',
      error: error instanceof Error ? error.message : String(error),
      verifiedAt: new Date().toISOString()
    }
    writeResult(result)
    throw error
  } finally {
    await browser.close()
  }
}

module.exports = {
  runSignatureGovernanceScenario
}
