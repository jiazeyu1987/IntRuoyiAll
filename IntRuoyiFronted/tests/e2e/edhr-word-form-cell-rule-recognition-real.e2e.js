const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.EDHR_WORD_RULE_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_WORD_RULE_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const TENANT = process.env.EDHR_WORD_RULE_TENANT || '测试租户'
const USERNAME = process.env.EDHR_WORD_RULE_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_WORD_RULE_PASSWORD || '111111'
const EXPECTED_TENANT_ID = '122'
const PROJECT_NAME_KEYWORD = process.env.EDHR_WORD_RULE_PROJECT_NAME || '球囊扩张压力泵'
const ROUTE = '/mes/pro/batch-record-form-list'
const RUN_ID = process.env.EDHR_WORD_RULE_RUN_ID || String(Date.now())
const RESULT_DIR = path.join(
  WORKSPACE_ROOT,
  'doc',
  'tasks',
  '20260717-word-form-format-rule-recognition',
  'e2e-artifacts'
)
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

const SCENARIOS = [
  {
    label: '损耗单',
    formSlotType: 'LOSS_REPORT',
    filePath: process.env.EDHR_WORD_RULE_LOSS_DOC || 'C:\\Users\\BJB110\\Desktop\\文档\\损耗单.doc'
  },
  {
    label: '过程检验单',
    formSlotType: 'PROCESS_INSPECTION',
    filePath: process.env.EDHR_WORD_RULE_INSPECTION_DOC || 'C:\\Users\\BJB110\\Desktop\\文档\\过程检验记录.docx'
  }
]

let runtimeAuth = {}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实 E2E 必须使用本机前端 http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, '真实 E2E 必须使用本机后端 48081')
  assert.equal(TENANT, '测试租户', '写入型 Word 规则识别 E2E 只能使用测试租户')
  assert.equal(USERNAME, 'aoteman', '写入型 Word 规则识别 E2E 只能使用 aoteman')
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome 不存在：${BROWSER_EXECUTABLE}`)
  for (const scenario of SCENARIOS) {
    assert.ok(fs.existsSync(scenario.filePath), `${scenario.label} 真实 Word 样本不存在：${scenario.filePath}`)
  }
  fs.mkdirSync(RESULT_DIR, { recursive: true })
}

function assertBusinessSuccess(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 必须返回 JSON 对象`)
  const code = Number(body.code)
  assert.ok([0, 200].includes(code), `${label} 业务响应失败：${body.msg || body.message || body.code}`)
  return body.data
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && (await item.isEnabled().catch(() => false))) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写控件：${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && !(await item.isDisabled().catch(() => true))) {
      await item.click()
      return
    }
  }
  throw new Error(`缺少可点击控件：${label}`)
}

async function waitForBusinessResponse(page, endpoint, label, method, timeout = 180000) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(endpoint) && item.request().method() === method,
    { timeout }
  )
  await response.finished().catch(() => undefined)
  assert.equal(response.status(), 200, `${label} HTTP 必须为 200`)
  return assertBusinessSuccess(await response.json(), label)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const loginForm = page.locator('form.login-form:visible, .login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 90000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder*="验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TENANT, '租户')
  }

  await fillFirstVisible(
    loginForm.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    USERNAME,
    '用户名'
  )
  await fillFirstVisible(loginForm.locator('input[type="password"]'), PASSWORD, '密码')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await Promise.all([
    loginResponsePromise,
    clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }), '登录')
  ])
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.status(), 200, '登录接口 HTTP 必须为 200')
  const loginData = assertBusinessSuccess(await loginResponse.json(), '测试租户登录')
  runtimeAuth = {
    token: loginData?.accessToken,
    tenantId: EXPECTED_TENANT_ID
  }
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
}

async function browserAuth(page) {
  if (runtimeAuth.token) return runtimeAuth
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
  const unwrap = (raw) => {
    if (!raw) return ''
    let current = raw
    for (let index = 0; index < 6; index += 1) {
      try {
        current = JSON.parse(current)
      } catch {
        break
      }
      if (current && typeof current === 'object') {
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) current = current.accessToken
        else if (Object.prototype.hasOwnProperty.call(current, 'v')) current = current.v
        else if (Object.prototype.hasOwnProperty.call(current, 'value')) current = current.value
      }
      if (typeof current !== 'string') break
    }
    return String(current || '').replace(/^"|"$/g, '')
  }
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID) || EXPECTED_TENANT_ID,
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

async function authenticatedGet(page, endpoint, params, label) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, `${label} 需要浏览器登录 token`)
  assert.equal(String(tenantId), EXPECTED_TENANT_ID, `${label} 租户 ID 不匹配，实际 tenant-id=${tenantId}`)
  const response = await page.request.get(`${BACKEND_URL}${endpoint}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    params
  })
  assert.equal(response.status(), 200, `${label} HTTP 必须为 200`)
  return assertBusinessSuccess(await response.json(), label)
}

async function resolveProjectNameByApi(page) {
  const pageData = await authenticatedGet(
    page,
    '/admin-api/dcc/project-codes/page',
    { pageNo: 1, pageSize: 100, projectName: PROJECT_NAME_KEYWORD },
    'DCC 项目代码候选查询'
  )
  const projectNames = (pageData?.list || [])
    .map((item) => String(item.projectName || '').trim())
    .filter(Boolean)
  assert.ok(projectNames.length > 0, `测试租户必须存在 DCC 项目名称候选：${PROJECT_NAME_KEYWORD}`)
  return projectNames.includes(PROJECT_NAME_KEYWORD) ? PROJECT_NAME_KEYWORD : projectNames[0]
}

async function openFormListPage(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.getByText('产品名称').first().waitFor({ state: 'visible', timeout: 90000 })
  await page.getByRole('button', { name: /^导入$/ }).first().waitFor({ state: 'visible', timeout: 90000 })
}

async function selectRemoteOption(page, selectRoot, value, label) {
  await selectRoot.click({ force: true })
  const input = selectRoot.locator('input:visible').first()
  const readonly = await input.evaluate((element) => element.hasAttribute('readonly')).catch(() => false)
  if (!readonly) {
    const remoteResponsePromise = label === '产品名称'
      ? page.waitForResponse(
        (response) => response.url().includes('/admin-api/dcc/project-codes/page') && response.request().method() === 'GET',
        { timeout: 60000 }
      ).catch(() => undefined)
      : Promise.resolve()
    await input.click({ force: true })
    await input.fill('')
    await input.pressSequentially(value, { delay: 20 })
    await remoteResponsePromise
  }
  const listboxId = await input.getAttribute('aria-controls').catch(() => '')
  const normalizedValue = String(value).replace(/\s+/g, '').trim()
  await page.waitForFunction(({ expected, listboxId }) => {
    const normalize = (text) => String(text || '').replace(/\s+/g, '').trim()
    const isUsableDropdown = (dropdown) => {
      if (!dropdown) return false
      const popper = dropdown.closest('.el-select__popper')
      const style = window.getComputedStyle(dropdown)
      const rect = dropdown.getBoundingClientRect()
      return dropdown.id === listboxId ||
        popper?.getAttribute('aria-hidden') === 'false' ||
        (style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0)
    }
    const dropdowns = [
      ...(listboxId ? [document.getElementById(listboxId)].filter(Boolean) : []),
      ...Array.from(document.querySelectorAll('.el-select-dropdown, .el-select-dropdown__list'))
    ].filter(isUsableDropdown)
    for (const dropdown of dropdowns) {
      const options = Array.from(dropdown.querySelectorAll('.el-select-dropdown__item:not(.is-disabled)'))
      for (const option of options) {
        const text = normalize(option.textContent)
        if (text && (text === expected || text.includes(expected) || expected.includes(text))) {
          return true
        }
      }
    }
    return false
  }, { expected: normalizedValue, listboxId }, { timeout: label === '产品名称' ? 60000 : 15000 })
  const selectedText = await page.evaluate(({ expected, listboxId }) => {
    const normalize = (text) => String(text || '').replace(/\s+/g, '').trim()
    const isUsableDropdown = (dropdown) => {
      if (!dropdown) return false
      const popper = dropdown.closest('.el-select__popper')
      const style = window.getComputedStyle(dropdown)
      const rect = dropdown.getBoundingClientRect()
      return dropdown.id === listboxId ||
        popper?.getAttribute('aria-hidden') === 'false' ||
        (style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0)
    }
    const dropdowns = [
      ...(listboxId ? [document.getElementById(listboxId)].filter(Boolean) : []),
      ...Array.from(document.querySelectorAll('.el-select-dropdown, .el-select-dropdown__list'))
    ].filter(isUsableDropdown)
    for (const dropdown of dropdowns) {
      const options = Array.from(dropdown.querySelectorAll('.el-select-dropdown__item:not(.is-disabled)'))
      for (const option of options) {
        const text = normalize(option.textContent)
        if (!text) continue
        if (text === expected || text.includes(expected) || expected.includes(text)) {
          option.scrollIntoView({ block: 'nearest' })
          option.click()
          return option.textContent || ''
        }
      }
    }
    return ''
  }, { expected: normalizedValue, listboxId })
  assert.ok(selectedText, `${label} 下拉必须包含选项：${value}`)
  await page.locator('.el-select-dropdown:visible').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined)
  return selectedText.split(/\r?\n/).map((item) => item.trim()).find(Boolean) || value
}

async function confirmExtraSlotUpgradeIfNeeded(page, scenarioLabel) {
  const box = page
    .locator('.el-message-box:visible')
    .filter({ hasText: `确认${scenarioLabel}升版` })
    .first()
  const visible = await box.waitFor({ state: 'visible', timeout: 5000 }).then(() => true).catch(() => false)
  if (!visible) return false
  await clickFirstEnabled(box.getByRole('button', { name: /升版导入/ }), `${scenarioLabel}升版导入`)
  await box.waitFor({ state: 'hidden', timeout: 60000 })
  return true
}

async function importExtraSlotByUi(page, projectName, scenario) {
  await openFormListPage(page)
  await clickFirstEnabled(page.getByRole('button', { name: /^导入$/ }), '导入')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入 Word' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await selectRemoteOption(
    page,
    dialog.locator('.el-form-item').filter({ hasText: '表单类型' }).locator('.el-select').first(),
    scenario.label,
    '表单类型'
  )
  const selectedProjectLabel = await selectRemoteOption(
    page,
    dialog.locator('.el-form-item').filter({ hasText: '产品名称' }).locator('.el-select').first(),
    projectName,
    '产品名称'
  )
  assert.ok(
    selectedProjectLabel.includes(projectName) || projectName.includes(selectedProjectLabel),
    `产品名称下拉选中项必须匹配：selected=${selectedProjectLabel}, expected=${projectName}`
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /选择文件/ }), '选择文件')
  await page.locator('input.batch-record-form-word-import-input[type="file"]').setInputFiles(scenario.filePath)
  await dialog.getByText(/已选择 Word 文件/).waitFor({ state: 'visible', timeout: 60000 })

  const uploadResponsePromise = waitForBusinessResponse(
    page,
    '/admin-api/mes/pro/batch-record-report/upload-extra-slot',
    `${scenario.label} Word 解析保存`,
    'POST',
    600000
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /^确定$/ }), '确定')
  const upgraded = await confirmExtraSlotUpgradeIfNeeded(page, scenario.label)
  const result = await uploadResponsePromise
  assert.ok(result.importedCount > 0, `${scenario.label} 导入必须生成至少一份表单：${JSON.stringify(result)}`)
  assert.ok(Array.isArray(result.reports) && result.reports.length > 0, `${scenario.label} 导入响应必须返回报表清单`)
  await page.getByText(new RegExp(`${scenario.label}解析完成`)).first().waitFor({ state: 'visible', timeout: 600000 })
  return { result, upgraded }
}

function collectPersistedAutoRules(sheetLayoutJson) {
  const sheetLayout = JSON.parse(sheetLayoutJson || '{}')
  const rows = sheetLayout.rows || {}
  const rules = []
  for (const row of Object.values(rows)) {
    const cells = row?.cells || {}
    for (const cell of Object.values(cells)) {
      const rule = cell?.edhrCellRule
      if (rule?.source === 'AUTO') rules.push(rule)
    }
  }
  return rules
}

async function verifyAutomaticCellRulesByApi(page, scenario, importResult) {
  const reports = (importResult.reports || []).filter((item) => item.reportId)
  assert.ok(reports.length > 0, `${scenario.label} 自动规则核验需要 reportId`)
  const summary = {
    scenario: scenario.label,
    reportCount: reports.length,
    totalSuggestions: 0,
    persistedAutoRules: 0,
    valueTypes: new Set(),
    typedConstraintHits: 0
  }
  for (const report of reports) {
    const cellRules = await authenticatedGet(
      page,
      '/admin-api/mes/pro/batch-record-report/cell-rules',
      { reportId: report.reportId },
      `${scenario.label} 单元格规则查询 ${report.reportName || report.reportId}`
    )
    const suggestions = cellRules?.suggestions || []
    assert.ok(suggestions.length > 0, `${scenario.label} 必须返回自动规则候选：${report.reportName}`)
    summary.totalSuggestions += suggestions.length
    for (const suggestion of suggestions) {
      assert.equal(suggestion.source, 'AUTO', `${scenario.label} 候选 source 必须为 AUTO：${JSON.stringify(suggestion)}`)
      assert.equal(suggestion.reviewed, false, `${scenario.label} 候选不能标记为人工确认：${JSON.stringify(suggestion)}`)
      if (suggestion.valueType) summary.valueTypes.add(suggestion.valueType)
      const constraints = suggestion.constraints || {}
      if (
        (suggestion.valueType === 'NUMBER' && Object.prototype.hasOwnProperty.call(constraints, 'min')) ||
        (suggestion.valueType === 'DATE' && constraints.format === 'yyyy-MM-dd') ||
        (suggestion.valueType === 'DATETIME' && constraints.format === 'yyyy-MM-dd HH:mm:ss') ||
        (suggestion.valueType === 'STRING' && Object.prototype.hasOwnProperty.call(constraints, 'maxLength'))
      ) {
        summary.typedConstraintHits += 1
      }
    }
    const persistedRules = collectPersistedAutoRules(cellRules.sheetLayoutJson)
    assert.ok(persistedRules.length > 0, `${scenario.label} 报表 JSON 必须持久化 AUTO edhrCellRule：${report.reportName}`)
    summary.persistedAutoRules += persistedRules.length
    for (const rule of persistedRules) {
      assert.equal(rule.reviewed, false, `${scenario.label} 持久化自动规则不能标记 reviewed=true：${JSON.stringify(rule)}`)
      assert.ok(rule.componentFlag, `${scenario.label} 持久化自动规则必须有 componentFlag：${JSON.stringify(rule)}`)
      if (rule.valueType) summary.valueTypes.add(rule.valueType)
    }
  }
  assert.ok(
    summary.valueTypes.has('NUMBER') || summary.valueTypes.has('BOOLEAN') || summary.valueTypes.has('DATE'),
    `${scenario.label} 至少要识别出一种强类型：${JSON.stringify([...summary.valueTypes])}`
  )
  assert.ok(summary.typedConstraintHits > 0, `${scenario.label} 至少要生成一条可追溯格式/范围约束`)
  return {
    ...summary,
    valueTypes: [...summary.valueTypes].sort()
  }
}

async function main() {
  assertLocalOnly()
  const browser = await chromium.launch({
    headless: process.env.EDHR_WORD_RULE_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const summaries = []
  try {
    await login(page)
    const projectName = await resolveProjectNameByApi(page)
    for (const scenario of SCENARIOS) {
      const { result, upgraded } = await importExtraSlotByUi(page, projectName, scenario)
      const summary = await verifyAutomaticCellRulesByApi(page, scenario, result)
      summaries.push({ ...summary, upgraded, versionNo: result.versionNo, versionStatus: result.versionStatus })
    }
    await page.screenshot({ path: path.join(RESULT_DIR, `word-form-cell-rules-${RUN_ID}.png`), fullPage: true })
    const evidence = {
      runId: RUN_ID,
      tenant: TENANT,
      username: USERNAME,
      projectName,
      scenarios: summaries
    }
    fs.writeFileSync(
      path.join(RESULT_DIR, `word-form-cell-rules-${RUN_ID}.json`),
      JSON.stringify(evidence, null, 2),
      'utf8'
    )
    console.log(
      `PASS: Word form cell rule recognition E2E project=${projectName} scenarios=${summaries.map((item) => `${item.scenario}:reports=${item.reportCount},suggestions=${item.totalSuggestions},persisted=${item.persistedAutoRules},types=${item.valueTypes.join('|')},upgraded=${item.upgraded}`).join('; ')}`
    )
  } catch (error) {
    await page.screenshot({ path: path.join(RESULT_DIR, `word-form-cell-rules-failure-${RUN_ID}.png`), fullPage: true }).catch(() => undefined)
    const html = await page.content().catch(() => '')
    if (html) {
      fs.writeFileSync(path.join(RESULT_DIR, `word-form-cell-rules-failure-${RUN_ID}.html`), html, 'utf8')
    }
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
