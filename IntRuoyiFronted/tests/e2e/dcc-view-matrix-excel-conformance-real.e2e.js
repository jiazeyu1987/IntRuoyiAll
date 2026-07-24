const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_VIEW_MATRIX_E2E_BASE_URL || 'http://127.0.0.1:8088').replace(/\/+$/, '')
const TENANT = process.env.DCC_VIEW_MATRIX_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_VIEW_MATRIX_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_VIEW_MATRIX_E2E_PASSWORD || '111111'
const HEADLESS = process.env.DCC_VIEW_MATRIX_E2E_HEADLESS !== 'false'
const CATEGORY_CODE = process.env.DCC_VIEW_MATRIX_E2E_CATEGORY_CODE || 'DCC_FVM_DHF_001'
const LOOKUP_USER_TEXT = process.env.DCC_VIEW_MATRIX_E2E_LOOKUP_USER_TEXT || 'DCC矩阵-QMS'
const ALLOW_YUDAO_TENANT = process.env.DCC_VIEW_MATRIX_E2E_ALLOW_YUDAO_TENANT === 'true'
const EVIDENCE_PATH =
  process.env.DCC_VIEW_MATRIX_E2E_EVIDENCE_PATH ||
  path.join(__dirname, '..', '..', 'doc', 'tasks', '20260624-dcc-view2-excel-conformance', 'dcc-view-matrix-excel-real-e2e-evidence.json')

const evidence = {
  startedAt: new Date().toISOString(),
  baseUrl: BASE_URL,
  tenant: TENANT,
  username: USERNAME,
  categoryCode: CATEGORY_CODE,
  lookupUserText: LOOKUP_USER_TEXT,
  steps: [],
  finalAssertions: {}
}

function record(status, label, detail = {}) {
  evidence.steps.push({
    status,
    label,
    detail,
    at: new Date().toISOString()
  })
}

async function runStep(label, fn) {
  try {
    const detail = await fn()
    record('PASS', label, detail || {})
    return detail
  } catch (error) {
    record('FAIL', label, { message: error.message })
    throw error
  }
}

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.ok(['127.0.0.1', 'localhost'].includes(url.hostname), `E2E must target local frontend, got ${BASE_URL}`)
  if (ALLOW_YUDAO_TENANT) {
    assert.equal(TENANT, '芋道源码', `DCC view matrix YuDao E2E must use 芋道源码, got ${TENANT}`)
    assert.equal(USERNAME, 'admin', `DCC view matrix YuDao E2E must use admin, got ${USERNAME}`)
    return
  }
  assert.equal(TENANT, '测试租户', `DCC view matrix E2E must use 测试租户 unless DCC_VIEW_MATRIX_E2E_ALLOW_YUDAO_TENANT=true, got ${TENANT}`)
  assert.equal(USERNAME, 'aoteman', `DCC view matrix E2E must use aoteman unless DCC_VIEW_MATRIX_E2E_ALLOW_YUDAO_TENANT=true, got ${USERNAME}`)
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function fillFirstVisible(page, selector, value, label) {
  const locator = page.locator(selector)
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible ${label}: ${selector}`)
}

async function waitForVisibleText(page, scope, text, label) {
  const deadline = Date.now() + 30000
  let lastCount = 0
  while (Date.now() < deadline) {
    const locator = scope.getByText(text, { exact: false })
    const count = await locator.count()
    lastCount = count
    for (let index = 0; index < count; index += 1) {
      const item = locator.nth(index)
      if (await item.isVisible()) {
        return
      }
    }
    await page.waitForTimeout(300)
  }
  throw new Error(`missing visible ${label}: ${text}, matched hidden/total nodes=${lastCount}`)
}

async function assertNoVisibleText(scope, text, label) {
  const locator = scope.getByText(text, { exact: false })
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    assert.equal(await item.isVisible(), false, `${label} must not be visible: ${text}`)
  }
}

async function selectTenant(page, tenantName) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    return false
  }
  await tenantSelect.click()
  await page.locator('.login-form .el-select__input').first().fill(tenantName)
  await page.keyboard.press('Enter')
  return true
}

function assertOkPayload(payload, label) {
  assert.ok(payload && typeof payload === 'object', `${label} payload must be object`)
  if (Object.prototype.hasOwnProperty.call(payload, 'code')) {
    assert.ok(payload.code === 0 || payload.code === 200, `${label} business code must be success, got ${payload.code}`)
  }
}

async function parseJsonResponse(response, label) {
  const text = await response.text()
  let payload
  try {
    payload = JSON.parse(text)
  } catch {
    throw new Error(`${label} returned non-json: ${text.slice(0, 300)}`)
  }
  assert.ok(response.ok(), `${label} HTTP status must be ok, got ${response.status()}: ${text.slice(0, 300)}`)
  assertOkPayload(payload, label)
  return payload.data
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/dcc/controlled-file/categories`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }
  const selected = await selectTenant(page, TENANT)
  if (!selected) {
    await fillFirstVisible(page, 'input[placeholder="请输入租户名称"]', TENANT, 'tenant input')
  }
  await fillFirstVisible(page, 'input[placeholder="请输入用户名"]', USERNAME, 'username input')
  await fillFirstVisible(page, 'input[placeholder="请输入密码"]', PASSWORD, 'password input')
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.locator('.login-form .el-button--primary').first().click()
  const loginResponse = await loginResponsePromise
  await parseJsonResponse(loginResponse, 'login')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

function summarizeRows(rows) {
  const fvmRows = rows.filter((row) => String(row.code || '').startsWith('DCC_FVM_'))
  const markerSummary = {}
  const subjectSummary = {}
  let ruleCount = 0
  for (const row of fvmRows) {
    for (const rule of row.rules || []) {
      ruleCount += 1
      markerSummary[rule.marker] = (markerSummary[rule.marker] || 0) + 1
      subjectSummary[rule.subjectLabel] = (subjectSummary[rule.subjectLabel] || 0) + 1
    }
  }
  return {
    categoryCount: fvmRows.length,
    ruleCount,
    markerSummary,
    subjectSummary
  }
}

function assertExcelSummary(summary) {
  assert.equal(summary.categoryCount, 59, 'view matrix must expose 59 Excel FVM categories')
  assert.equal(summary.ruleCount, 243, 'view matrix must expose 243 Excel rules')
  assert.deepEqual(summary.markerSummary, { '●': 195, '▲': 48 })
  assert.equal(summary.subjectSummary.QMS, 59)
  assert.equal(summary.subjectSummary['新品开发部'], 54)
  assert.equal(summary.subjectSummary.QA, 28)
  assert.equal(summary.subjectSummary['市场 / 注册'], 20)
}

async function clickViewMatrixTab(page) {
  await page.goto(`${BASE_URL}/dcc/controlled-file/categories`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await page.getByRole('tab', { name: '查看矩阵' }).click()
  await page.locator('[data-testid="dcc-view-matrix-table"]').waitFor({ state: 'visible', timeout: 30000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/file-categories/view-matrix') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.locator('.view-matrix-toolbar').getByRole('button', { name: '查询' }).click()
  const response = await responsePromise
  const table = page.locator('[data-testid="dcc-view-matrix-table"]')
  await waitForVisibleText(page, table, '可查阅', 'view matrix readable column')
  await assertNoVisibleText(table, 'Excel', 'view matrix table source wording')
  await assertNoVisibleText(table, '待审预览主体', 'view matrix pending preview column')
  await assertNoVisibleText(table, '当前状态/风险', 'view matrix risk column')
  return await parseJsonResponse(response, 'view matrix rows')
}

async function filterCategory(page) {
  const codeInput = page.locator('.view-matrix-toolbar input[placeholder="请输入类别编码"]').first()
  await codeInput.fill(CATEGORY_CODE)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/file-categories/view-matrix') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.locator('.view-matrix-toolbar').getByRole('button', { name: '查询' }).click()
  const response = await responsePromise
  const rows = await parseJsonResponse(response, 'filtered view matrix rows')
  assert.equal(rows.length, 1, `filtered category ${CATEGORY_CODE} must return one row`)
  assert.equal(rows[0].code, CATEGORY_CODE)
  const table = page.locator('[data-testid="dcc-view-matrix-table"]')
  await waitForVisibleText(page, table, CATEGORY_CODE, 'filtered category code')
  return rows[0]
}

async function previewEffectiveUsers(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/file-categories/') &&
      response.url().includes('/view-matrix/effective-preview') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.locator('[data-testid="dcc-view-matrix-effective-preview"]').first().click()
  const response = await responsePromise
  const preview = await parseJsonResponse(response, 'effective preview')
  assert.ok(Array.isArray(preview.viewSubjects), 'preview must return resolved view subjects')
  assert.ok(preview.viewSubjects.length > 0, 'preview must resolve actual users')
  assert.equal(preview.blocking, false, 'preview must not be blocking after Excel seed')
  await page.locator('[data-testid="dcc-view-matrix-effective-users"]').waitFor({ state: 'visible', timeout: 30000 })
  const dialog = page.locator('.el-dialog:visible').last()
  await assertNoVisibleText(dialog, 'Excel', 'effective preview dialog source wording')
  await page.getByText('当前查看矩阵', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.keyboard.press('Escape')
  await page.locator('[data-testid="dcc-view-matrix-effective-users"]').waitFor({ state: 'hidden', timeout: 30000 })
  return {
    resolvedUserCount: preview.viewSubjects.length,
    riskCount: Array.isArray(preview.risks) ? preview.risks.length : 0
  }
}

async function userLookup(page) {
  const simpleUserResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/user/simple-list'),
    { timeout: 60000 }
  ).catch((error) => error)
  await page.locator('[data-testid="dcc-view-matrix-user-lookup"]').click()
  const simpleUserResponse = await simpleUserResponsePromise
  if (simpleUserResponse instanceof Error) {
    throw simpleUserResponse
  }
  const dialog = page.locator('.el-dialog').filter({ hasText: '按人反查查看矩阵' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('.el-select').first().click()
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: LOOKUP_USER_TEXT }).first().click()
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/file-categories/view-matrix/user-lookup') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '查询' }).click()
  const response = await responsePromise
  const rows = await parseJsonResponse(response, 'user lookup')
  assert.ok(Array.isArray(rows), 'user lookup must return rows')
  assert.ok(rows.length >= 59, `QMS lookup should see at least 59 categories, got ${rows.length}`)
  assert.ok(rows.some((row) => row.code === CATEGORY_CODE && row.browseStatus === 'YES'), `${LOOKUP_USER_TEXT} must browse ${CATEGORY_CODE}`)
  await dialog.locator('[data-testid="dcc-view-matrix-user-lookup-table"]').waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText(CATEGORY_CODE, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  return {
    lookupRows: rows.length,
    categoryMatched: rows.some((row) => row.code === CATEGORY_CODE)
  }
}

async function main() {
  assertSafeBoundary()
  fs.mkdirSync(path.dirname(EVIDENCE_PATH), { recursive: true })
  const browser = await chromium.launch({ headless: HEADLESS })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const screenshotDir = path.dirname(EVIDENCE_PATH)
  try {
    await runStep('login tenant', async () => {
      await login(page)
      return { url: page.url() }
    })
    const rows = await runStep('open view matrix tab', async () => {
      const data = await clickViewMatrixTab(page)
      const summary = summarizeRows(data)
      assertExcelSummary(summary)
      evidence.finalAssertions.summary = summary
      return summary
    })
    await runStep('filter category row', async () => {
      const row = await filterCategory(page)
      return {
        code: row.code,
        name: row.name,
        ruleCount: row.rules?.length || 0,
        subjectCount: row.viewSubjects?.length || 0
      }
    })
    await runStep('preview effective users', async () => await previewEffectiveUsers(page))
    await runStep('user reverse lookup', async () => await userLookup(page))
    const screenshotPath = path.join(screenshotDir, 'dcc-view-matrix-excel-real-e2e.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })
    evidence.finalAssertions.screenshotPath = screenshotPath
    evidence.completedAt = new Date().toISOString()
    evidence.status = 'PASS'
  } catch (error) {
    const screenshotPath = path.join(screenshotDir, 'dcc-view-matrix-excel-real-e2e-failure.png')
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => undefined)
    evidence.status = 'FAIL'
    evidence.error = error.message
    evidence.failureScreenshotPath = screenshotPath
    throw error
  } finally {
    fs.writeFileSync(EVIDENCE_PATH, JSON.stringify(evidence, null, 2), 'utf8')
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
