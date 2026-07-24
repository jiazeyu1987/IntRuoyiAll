const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260615-edhr-tail-four-goals-design'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-permission-matrix')
const EVIDENCE_FILE = path.resolve(process.cwd(), '..', 'doc', 'tasks', TASK_ID, 'permission-matrix-e2e-evidence.md')

const REQUIRED_BASE_URL = 'http://localhost:8081'
const REQUIRED_TENANT = '测试租户'
const REQUIRED_USERNAME = 'aoteman'
const RECORD_TABLE_ID = '12a52dcc1b0a4ee9b6118cfedd825d67'
const ABILITY_LABELS = {
  VIEW: '查看',
  FILL: '填写',
  SIGN: '签名',
  APPROVE: '审批',
  ARCHIVE: '归档',
  AUDIT_VIEW: '审计查看',
  ROUTE_EDIT: '路线编辑',
  PERMISSION_ADMIN: '权限管理'
}

function envValue(name) {
  return (process.env[name] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function blocked(message) {
  const error = new Error(message)
  error.blocked = true
  return error
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch {
    throw blocked('缺少 Playwright runtime，请先在 yudao-ui-admin-vue3 执行 pnpm install。')
  }
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_PERMISSION_MATRIX_E2E_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_PERMISSION_MATRIX_E2E_TENANT') || REQUIRED_TENANT,
    username: envValue('EDHR_PERMISSION_MATRIX_E2E_USERNAME') || REQUIRED_USERNAME,
    password: envValue('EDHR_PERMISSION_MATRIX_E2E_PASSWORD'),
    deniedUserId: Number(envValue('EDHR_PERMISSION_MATRIX_E2E_DENIED_USER_ID') || '912398'),
    deniedUserKeyword: envValue('EDHR_PERMISSION_MATRIX_E2E_DENIED_USER_KEYWORD') || 'admin',
    writerUserId: Number(envValue('EDHR_PERMISSION_MATRIX_E2E_WRITER_USER_ID') || '914520'),
    writerUserKeyword: envValue('EDHR_PERMISSION_MATRIX_E2E_WRITER_USER_KEYWORD') || REQUIRED_USERNAME,
    scopeId: Number(envValue('EDHR_PERMISSION_MATRIX_E2E_SCOPE_ID') || '0'),
    objectType: envValue('EDHR_PERMISSION_MATRIX_E2E_OBJECT_TYPE') || 'RECORD_TABLE',
    objectId: envValue('EDHR_PERMISSION_MATRIX_E2E_OBJECT_ID') || RECORD_TABLE_ID,
    ability: envValue('EDHR_PERMISSION_MATRIX_E2E_ABILITY') || 'VIEW',
    executablePath:
      envValue('EDHR_PERMISSION_MATRIX_E2E_CHROME_EXECUTABLE') || envValue('PLAYWRIGHT_CHROME_EXECUTABLE'),
    headed: envValue('EDHR_PERMISSION_MATRIX_E2E_HEADED') === '1'
  }
  const missing = []
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    missing.push(`base URL must be ${REQUIRED_BASE_URL}, got ${config.baseUrl}`)
  }
  if (config.tenant !== REQUIRED_TENANT) {
    missing.push(`tenant must be ${REQUIRED_TENANT}, got ${config.tenant}`)
  }
  if (config.username !== REQUIRED_USERNAME) {
    missing.push(`username must be ${REQUIRED_USERNAME}, got ${config.username}`)
  }
  if (!config.password) missing.push('EDHR_PERMISSION_MATRIX_E2E_PASSWORD is required')
  if (!Number.isFinite(config.writerUserId) || config.writerUserId <= 0) {
    missing.push('writer user id must be a positive number')
  }
  if (!Number.isFinite(config.deniedUserId) || config.deniedUserId <= 0) {
    missing.push('denied user id must be a positive number')
  }
  if (config.scopeId && (!Number.isFinite(config.scopeId) || config.scopeId <= 0)) {
    missing.push('scope id must be a positive number when provided')
  }
  if (!config.objectType || !config.objectId) missing.push('object type and object id are required')
  if (!ABILITY_LABELS[config.ability]) missing.push(`unsupported ability: ${config.ability}`)
  return { ...config, missing }
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
  assert.equal(response.status(), 200, `${label} HTTP 状态应为 200，实际 ${response.status()}，URL=${response.url()}`)
  return parseBusinessData(await response.json(), label)
}

async function firstVisible(locator, failureMessage) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(failureMessage)
}

async function fillFirstVisible(locator, value, failureMessage) {
  const item = await firstVisible(locator, failureMessage)
  await item.click()
  await item.fill('')
  await item.fill(String(value))
}

async function clickVisibleButton(scope, namePattern, failureMessage) {
  const button = await firstVisible(scope.getByRole('button', { name: namePattern }), failureMessage)
  if (await button.isDisabled()) throw new Error(`${failureMessage} 按钮处于禁用状态。`)
  await button.click()
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page
    .locator('form.login-form')
    .filter({ has: page.getByPlaceholder('请输入用户名') })
    .filter({ hasText: '记住我' })
    .first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  const captchaInput = loginForm.locator('input[placeholder*="验证码"]').first()
  if ((await captchaInput.count()) > 0 && (await captchaInput.isVisible())) {
    throw blocked('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }
  const tenantInput = loginForm.locator('input.el-select__input:visible').first()
  await tenantInput.click()
  await page.keyboard.press('Control+A')
  await page.keyboard.type(config.tenant)
  await page.keyboard.press('Enter')
  await page.waitForTimeout(400)
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, '登录页缺少用户名输入框。')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, '登录页缺少密码输入框。')
  await clickVisibleButton(loginForm, /^登录$/, '登录页缺少登录按钮。')
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })
}

function formItem(page, surfaceSelector, label) {
  return page.locator(`${surfaceSelector} .el-form-item`).filter({ hasText: label }).first()
}

async function setSelectByVisibleText(page, selectRoot, optionText) {
  await selectRoot.click()
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await dropdown.waitFor({ state: 'visible', timeout: 30000 })
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click({ force: true })
}

async function selectSubjectById(page, row, userId, keyword, rowIndex) {
  const subjectSelect = row.locator('.el-select').nth(1)
  await subjectSelect.click()
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await dropdown.waitFor({ state: 'visible', timeout: 30000 })
  await page.keyboard.type(keyword || String(userId))
  await page.waitForTimeout(500)
  const exactIdPattern = new RegExp(`#${userId}(\\D|$)`)
  const options = dropdown.locator('.el-select-dropdown__item')
  let option = options.filter({ hasText: exactIdPattern }).first()
  if ((await option.count()) === 0 && keyword) {
    option = options.filter({ hasText: keyword }).first()
  }
  if ((await option.count()) === 0) option = options.filter({ hasText: String(userId) }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.scrollIntoViewIfNeeded()
  await option.click({ force: true })
  const summary = row.locator('.edhr-permission-matrix__subject-summary')
  await summary.waitFor({ state: 'visible', timeout: 30000 })
  const summaryText = await summary.innerText()
  assert.ok(
    summaryText.includes(`#${userId}`) || summaryText.includes(String(userId)),
    `第 ${rowIndex + 1} 条规则主体选择后应回显用户 ${userId}，实际：${summaryText}`
  )
}

async function fillScopeForm(page, config, scopeName) {
  if (config.scopeId) {
    await fillFirstVisible(formItem(page, '.edhr-permission-matrix__toolbar', '权限范围ID').locator('input'), config.scopeId, '缺少权限范围ID输入框。')
  }
  await fillFirstVisible(formItem(page, '.edhr-permission-matrix__toolbar', '范围名称').locator('input'), scopeName, '缺少范围名称输入框。')
  await fillFirstVisible(formItem(page, '.edhr-permission-matrix__toolbar', '对象类型').locator('input'), config.objectType, '缺少对象类型输入框。')
  await fillFirstVisible(formItem(page, '.edhr-permission-matrix__toolbar', '对象ID').locator('input'), config.objectId, '缺少对象ID输入框。')
}

async function fillRuleRow(page, rowIndex, userId, userKeyword, ability, decisionText) {
  const rows = page.locator('.edhr-permission-matrix__rule-table .el-table__body-wrapper .el-table__row')
  const row = rows.nth(rowIndex)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await selectSubjectById(page, row, userId, userKeyword, rowIndex)
  await setSelectByVisibleText(page, row.locator('.el-select').nth(2), ABILITY_LABELS[ability])
  if (decisionText) {
    await setSelectByVisibleText(page, row.locator('.el-select').nth(3), decisionText)
  }
}

async function selectOnlyAbility(page, ability) {
  const group = page.locator('.edhr-permission-matrix__ability-group')
  for (const [value, label] of Object.entries(ABILITY_LABELS)) {
    const button = group.locator('.el-checkbox-button').filter({ hasText: label }).first()
    await button.waitFor({ state: 'visible', timeout: 30000 })
    const className = (await button.getAttribute('class')) || ''
    if (value === ability && !className.includes('is-checked')) {
      await button.click()
    }
    if (value !== ability && className.includes('is-checked')) {
      await button.click()
    }
  }
}

async function saveLoadAndEvaluate(page, config) {
  const scopeName = `P8-对象权限-${Date.now()}`
  await page.goto(`${config.baseUrl}/mes/pro/feedback/edhr-permission-matrix`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('保存规则').first().waitFor({ state: 'visible', timeout: 60000 })
  let writerRuleRow = 0
  let deniedRuleRow = 1
  if (config.scopeId) {
    await fillFirstVisible(formItem(page, '.edhr-permission-matrix__toolbar', '权限范围ID').locator('input'), config.scopeId, '缺少权限范围ID输入框。')
    const loadResponse = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/edhr-permission-scopes/get') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await clickVisibleButton(page, /^读取规则$/, '权限矩阵页缺少读取规则按钮。')
    await parseJsonResponse(await loadResponse, '对象权限读取')
    const existingRowCount = await page.locator('.edhr-permission-matrix__rule-table .el-table__body-wrapper .el-table__row').count()
    writerRuleRow = existingRowCount
    deniedRuleRow = existingRowCount + 1
    await clickVisibleButton(page, /添加规则/, '权限矩阵页缺少添加规则按钮。')
    await clickVisibleButton(page, /添加规则/, '权限矩阵页缺少添加规则按钮。')
  } else {
    await fillScopeForm(page, config, scopeName)
    await clickVisibleButton(page, /添加规则/, '权限矩阵页缺少添加规则按钮。')
  }
  await fillRuleRow(page, writerRuleRow, config.writerUserId, config.writerUserKeyword, config.ability, '允许')
  await fillRuleRow(page, deniedRuleRow, config.deniedUserId, config.deniedUserKeyword, config.ability, '拒绝')

  const saveResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/edhr-permission-scopes/save') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /^保存规则$/, '权限矩阵页缺少保存规则按钮。')
  const saveData = await parseJsonResponse(await saveResponse, '对象权限保存')
  assert.ok(Number(saveData?.scopeId), '对象权限保存必须返回真实 scopeId。')
  assert.equal(saveData.objectType, config.objectType, '对象权限保存必须回显对象类型。')
  assert.equal(saveData.objectId, config.objectId, '对象权限保存必须回显对象ID。')
  assert.ok((saveData.rules || []).length >= 2, '对象权限保存必须返回两条真实规则。')

  const getResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/edhr-permission-scopes/get') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /^读取规则$/, '权限矩阵页缺少读取规则按钮。')
  const getData = await parseJsonResponse(await getResponse, '对象权限读取')
  assert.equal(Number(getData.scopeId), Number(saveData.scopeId), '对象权限读取必须返回刚保存的 scopeId。')

  await selectOnlyAbility(page, config.ability)
  const evaluateResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/edhr-permission-scopes/evaluate') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /^评估$/, '权限矩阵页缺少评估按钮。')
  const evaluateData = await parseJsonResponse(await evaluateResponse, '对象权限评估')
  assert.equal(evaluateData.decisions?.[config.ability], 'ALLOW', `当前写入账号 ${config.ability} 决策必须为 ALLOW。`)
  assert.ok(evaluateData.operationAuditEventId, '对象权限评估必须返回操作审计事件 ID。')
  const evidenceCollapse = page.locator('.edhr-permission-matrix__evidence-collapse').first()
  await evidenceCollapse.getByText('评估证据').first().click()
  await evidenceCollapse
    .locator('.edhr-permission-matrix__evidence-item')
    .filter({ hasText: '审计事件ID' })
    .filter({ hasText: String(evaluateData.operationAuditEventId) })
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })

  return {
    scopeId: saveData.scopeId,
    ruleCount: saveData.rules.length,
    operationAuditEventId: evaluateData.operationAuditEventId,
    scopeName
  }
}

function writeResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 对象级权限矩阵真实 UI 验证',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- Generated at: ${new Date().toISOString()}`,
    `- Command: \`node tests/e2e/edhr-permission-matrix-real-flow.e2e.js\``,
    '- Tenant/User: `测试租户/aoteman`；登录密码仅从环境变量读取，不写入证据。',
    `- Status: ${result.status}`,
    '',
    '## BDD',
    '',
    '- BDD: 对象权限维护 -> Given 测试租户真实 eDHR 权限矩阵页 When aoteman 在页面创建记录表对象权限规则并保存 Then 后端返回真实 scopeId、规则明细，并保留保存审计。',
    '- BDD: 对象权限评估 -> Given 已保存对象权限规则 When aoteman 在同一页面读取并评估对象权限 Then 页面展示后端决策和 operationAuditEventId。',
    ''
  ]
  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `node tests/e2e/edhr-permission-matrix-real-flow.e2e.js` -> PASS, 权限矩阵页保存、读取、评估真实 UI 路径通过。')
    lines.push(`- scopeId=${result.scopeId}; ruleCount=${result.ruleCount}; operationAuditEventId=${result.operationAuditEventId}`)
    lines.push('')
  } else {
    lines.push('## RED', '')
    lines.push(`- RED: \`node tests/e2e/edhr-permission-matrix-real-flow.e2e.js\` -> FAIL, ${result.error?.message || result.reason || 'unknown error'}`)
    lines.push('- Impact: 第 58 条对象级权限真实 UI 验收未放行。')
    lines.push('')
  }
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实 E2E 前置条件缺失或命中保护范围。',
      missing: config.missing,
      generatedAt: new Date().toISOString()
    }
    writeResult(result)
    writeEvidence(result)
    console.error(`${result.reason} ${config.missing.join('; ')}`)
    process.exitCode = 1
    return
  }
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({
    headless: !config.headed,
    ...(config.executablePath ? { executablePath: config.executablePath } : {})
  })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai'
  })
  const page = await context.newPage()
  try {
    await login(page, config)
    const result = {
      status: 'PASS',
      generatedAt: new Date().toISOString(),
      ...(await saveLoadAndEvaluate(page, config))
    }
    await browser.close()
    writeResult(result)
    writeEvidence(result)
    console.log('PASS: eDHR permission matrix real UI flow')
  } catch (error) {
    await browser.close()
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.blocked ? error.message : undefined,
      error: {
        name: error?.name || 'Error',
        message: error?.message || String(error),
        stack: error?.stack
      },
      generatedAt: new Date().toISOString()
    }
    writeResult(result)
    writeEvidence(result)
    if (error.blocked) {
      console.error(error.message)
      process.exitCode = 1
      return
    }
    throw error
  }
}

main()
