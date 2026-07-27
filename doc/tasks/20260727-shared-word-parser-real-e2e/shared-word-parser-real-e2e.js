const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const playwrightPackage = path.join(frontendRoot, 'node_modules', 'playwright')
assert.ok(fs.existsSync(playwrightPackage), `missing Playwright dependency: ${playwrightPackage}`)

const { chromium } = require(playwrightPackage)

const runId = process.env.SHARED_WORD_E2E_RUN_ID || '20260727-shared-word-parser-real-e2e'
const baseUrl = (process.env.SHARED_WORD_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const backendUrl = (process.env.SHARED_WORD_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const tenant = process.env.SHARED_WORD_E2E_TENANT || '测试租户'
const username = process.env.SHARED_WORD_E2E_USERNAME || 'aoteman'
const password =
  process.env.SHARED_WORD_E2E_PASSWORD ||
  process.env.EDHR_WORD_IMPORT_TEST_PASSWORD ||
  readExistingWordImportTestPassword()
const formCenterDoc = process.env.SHARED_WORD_E2E_FORM_CENTER_DOC || path.join(workspaceRoot, 'resource', '过程检验记录.docx')
const batchRecordDoc = process.env.SHARED_WORD_E2E_BATCH_RECORD_DOC || path.join(workspaceRoot, 'resource', '批记录压力泵.doc')
const productName = process.env.SHARED_WORD_E2E_PRODUCT_NAME || '球囊扩张压力泵'
const taskDir = __dirname
const artifactDir = path.join(taskDir, 'artifacts')
const evidencePath = path.join(taskDir, 'real-e2e-evidence.json')
const screenshots = {
  formCenter: path.join(artifactDir, `form-center-${runId}.png`),
  mesPreflight: path.join(artifactDir, `mes-preflight-${runId}.png`),
  formCenterBlocked: path.join(artifactDir, `form-center-blocked-${runId}.png`),
  failure: path.join(artifactDir, `failure-${runId}.png`)
}

const evidence = {
  runId,
  baseUrl,
  backendUrl,
  tenant,
  username,
  formCenterDoc,
  batchRecordDoc,
  productName,
  startedAt: new Date().toISOString(),
  results: []
}

function readExistingWordImportTestPassword() {
  const scriptPath = path.join(frontendRoot, 'tests', 'e2e', 'edhr-word-template-import-real-flow.e2e.js')
  assert.ok(fs.existsSync(scriptPath), `missing existing Word import E2E script: ${scriptPath}`)
  const source = fs.readFileSync(scriptPath, 'utf8')
  const match = source.match(/const\s+TEST_PASSWORD\s*=\s*process\.env\.EDHR_WORD_IMPORT_TEST_PASSWORD\s*\|\|\s*(['"])(.*?)\1/)
  assert.ok(match?.[2], 'missing EDHR Word import test password source in existing E2E script')
  return match[2]
}

function assertPreconditions() {
  assert.equal(baseUrl, 'http://localhost:8081', `E2E must use local frontend http://localhost:8081, got ${baseUrl}`)
  assert.match(backendUrl, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, `E2E must use local backend 48081, got ${backendUrl}`)
  assert.equal(tenant, '测试租户', `write E2E must use 测试租户, got ${tenant}`)
  assert.equal(username, 'aoteman', `write E2E must use aoteman, got ${username}`)
  assert.ok(password, 'missing login password from env or frontend .env')
  assert.ok(fs.existsSync(formCenterDoc), `missing real form-center Word file: ${formCenterDoc}`)
  assert.ok(fs.existsSync(batchRecordDoc), `missing real batch-record Word file: ${batchRecordDoc}`)
  fs.mkdirSync(artifactDir, { recursive: true })
}

function assertBusinessSuccess(payload, label) {
  assert.ok(payload && typeof payload === 'object', `${label} must return JSON object`)
  assert.ok([0, 200].includes(Number(payload.code)), `${label} business failed: ${JSON.stringify(payload)}`)
  return payload.data
}

function parsePossiblyWrappedCache(raw) {
  if (!raw) return undefined
  let current = raw
  for (let index = 0; index < 8; index += 1) {
    if (typeof current !== 'string') break
    try {
      current = JSON.parse(current)
    } catch {
      break
    }
    if (current && typeof current === 'object' && Object.prototype.hasOwnProperty.call(current, 'v')) {
      current = current.v
    }
  }
  return current
}

async function browserAuth(page) {
  const storage = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      if (!Object.prototype.hasOwnProperty.call(result, key)) result[key] = sessionStorage.getItem(key)
    }
    return result
  })
  const token = parsePossiblyWrappedCache(storage.ACCESS_TOKEN || storage.accessToken || storage.token)
  const tenantId = parsePossiblyWrappedCache(storage.TENANT_ID || storage.tenantId)
  const visitTenantId = parsePossiblyWrappedCache(storage.VISIT_TENANT_ID || storage.visitTenantId)
  return {
    token: typeof token === 'string' ? token : String(token || ''),
    tenantId: typeof tenantId === 'string' ? tenantId : String(tenantId || ''),
    visitTenantId: typeof visitTenantId === 'string' ? visitTenantId : String(visitTenantId || '')
  }
}

async function authenticatedGet(page, endpoint, label) {
  const auth = await browserAuth(page)
  assert.ok(auth.token, `${label} requires browser token`)
  assert.ok(auth.tenantId, `${label} requires tenant-id`)
  const headers = {
    Authorization: `Bearer ${auth.token}`,
    'tenant-id': auth.tenantId
  }
  if (auth.visitTenantId) headers['visit-tenant-id'] = auth.visitTenantId
  const response = await page.request.get(`${backendUrl}${endpoint}`, { headers })
  assert.equal(response.status(), 200, `${label} HTTP must be 200`)
  return assertBusinessSuccess(await response.json(), label)
}

function findMatchingJsonValues(value, matcher, limit = 30) {
  const results = []
  const visit = (item, trail) => {
    if (results.length >= limit || item == null) return
    if (typeof item === 'string' || typeof item === 'number' || typeof item === 'boolean') {
      const text = String(item)
      if (matcher.test(text)) results.push({ path: trail.join('.'), value: text })
      return
    }
    if (Array.isArray(item)) {
      item.forEach((child, index) => visit(child, trail.concat(String(index))))
      return
    }
    if (typeof item === 'object') {
      const compact = {}
      for (const key of ['name', 'path', 'component', 'redirect', 'title', 'permission', 'permissions']) {
        if (Object.prototype.hasOwnProperty.call(item, key)) compact[key] = item[key]
      }
      if (Object.keys(compact).length && matcher.test(JSON.stringify(compact))) {
        results.push({ path: trail.join('.'), value: compact })
      }
      for (const [key, child] of Object.entries(item)) visit(child, trail.concat(key))
    }
  }
  visit(value, [])
  return results
}

async function collectFormCenterAccessSnapshot(page) {
  const snapshot = {
    currentUrl: page.url(),
    pageText: '',
    permissionMatches: [],
    menuMatches: [],
    routerCacheMatches: []
  }
  snapshot.pageText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
  const matcher = /form-center|form:template|表单模板|模板|template/i
  const storage = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  }).catch(() => ({}))
  for (const [key, raw] of Object.entries(storage)) {
    if (/TOKEN|PASSWORD|SECRET|KEY/i.test(key)) continue
    const parsed = parsePossiblyWrappedCache(raw)
    const matches = findMatchingJsonValues(parsed, matcher, 10)
    if (matches.length) snapshot.routerCacheMatches.push({ key, matches })
  }
  try {
    const permissionInfo = await authenticatedGet(page, '/admin-api/system/auth/get-permission-info', 'permission info')
    snapshot.permissionMatches = findMatchingJsonValues(permissionInfo?.permissions || [], matcher, 30)
    snapshot.menuMatches = findMatchingJsonValues(permissionInfo?.menus || [], matcher, 30)
  } catch (error) {
    snapshot.permissionError = String(error?.message || error)
  }
  return snapshot
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page, redirectPath) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('BLOCKER: login captcha is enabled; cannot run unattended real E2E.')
  }

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await form.locator('input[placeholder="请输入租户名称"], input.el-input__inner').first().fill(tenant)
  }

  await form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"]):visible').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).first().click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.status(), 200, 'login HTTP must be 200')
  assertBusinessSuccess(await loginResponse.json(), 'login')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  await settle(page)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click()
      return
    }
  }
  throw new Error(`missing enabled control: ${label}`)
}

async function uploadWithNativeInput(page, dialog, filePath) {
  const fileName = path.basename(filePath)
  const fileInput = dialog.locator('input[type="file"]').last()
  await fileInput.waitFor({ state: 'attached', timeout: 30000 })
  await fileInput.setInputFiles(filePath)
  const visibleFile = dialog
    .locator('.el-upload-list__item, .el-upload-list__item-name, .el-upload-list__item-file-name')
    .filter({ hasText: fileName })
    .first()
  const fileVisible = await visibleFile.waitFor({ state: 'visible', timeout: 15000 })
    .then(() => true)
    .catch(() => false)
  if (!fileVisible) {
    const uploadState = await dialog.evaluate((root) => ({
      text: root.textContent?.replace(/\s+/g, ' ').trim().slice(0, 500) || '',
      fileInputCount: root.querySelectorAll('input[type="file"]').length,
      fileInputsWithFiles: Array.from(root.querySelectorAll('input[type="file"]'))
        .filter((input) => input.files && input.files.length > 0)
        .length
    }))
    throw new Error(`BLOCKER: Word file was not visible in upload list after setInputFiles: ${JSON.stringify(uploadState)}`)
  }
}

async function runFormCenterImport(page) {
  const candidateRoutes = ['/approval-center/manager/form-center/template', '/mdm/form-center/template']
  const templateName = `E2E共享Word解析-${runId}-${Date.now()}`
  const routeAttempts = []
  let route = ''
  for (const candidateRoute of candidateRoutes) {
    await page.goto(`${baseUrl}${candidateRoute}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    const bodyText = await page.locator('body').innerText({ timeout: 10000 }).catch(() => '')
    const visible = await page.getByText('模板名称').first().waitFor({ state: 'visible', timeout: 15000 })
      .then(() => true)
      .catch(() => false)
    routeAttempts.push({
      route: candidateRoute,
      currentUrl: page.url(),
      visible,
      is404: /404|页面不存在/.test(bodyText),
      bodyText: bodyText.slice(0, 300)
    })
    if (visible) {
      route = candidateRoute
      break
    }
  }
  if (!route) {
    const snapshot = await collectFormCenterAccessSnapshot(page)
    evidence.results.push({
      scope: 'form-center',
      status: 'BLOCKED',
      file: formCenterDoc,
      candidateRoutes: routeAttempts,
      accessSnapshot: snapshot,
      screenshot: screenshots.formCenterBlocked,
      blocker: '测试租户/aoteman 无法通过真实前端路由打开表单模板页；页面返回 404/不可达。'
    })
    await page.screenshot({ path: screenshots.formCenterBlocked, fullPage: true }).catch(() => null)
    return
  }
  await clickFirstEnabled(page.getByRole('button', { name: /^导入$/ }), 'form-center import button')

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入表单模板' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.locator('input[placeholder*="模板名称"]').first().fill(templateName)
  await dialog.locator('textarea[placeholder*="备注"]').first().fill(`真实 Word 解析 E2E ${runId}`)
  await uploadWithNativeInput(page, dialog, formCenterDoc)

  const requestPromise = page.waitForRequest(
    (request) =>
      request.url().includes('/admin-api/form-center/templates/import-doc') &&
      request.method() === 'POST',
    { timeout: 20000 }
  ).catch(() => null)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/form-center/templates/import-doc') &&
      response.request().method() === 'POST',
    { timeout: 300000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /^导入$/ }), 'form-center dialog import button')
  const request = await requestPromise
  if (!request) {
    const snapshot = await collectFormCenterAccessSnapshot(page)
    const dialogText = await dialog.innerText().catch(() => '')
    evidence.results.push({
      scope: 'form-center',
      status: 'BLOCKED',
      route,
      endpoint: '/admin-api/form-center/templates/import-doc',
      file: formCenterDoc,
      templateName,
      dialogText: dialogText.slice(0, 1000),
      accessSnapshot: snapshot,
      screenshot: screenshots.formCenterBlocked,
      blocker: '表单中心真实导入弹窗未发出 import-doc 请求；未使用 API-only 代替页面提交。'
    })
    await page.screenshot({ path: screenshots.formCenterBlocked, fullPage: true }).catch(() => null)
    return
  }
  const response = await responsePromise
  assert.equal(response.status(), 200, 'form-center import HTTP must be 200')
  const data = assertBusinessSuccess(await response.json(), 'form-center import-doc')
  assert.ok(data.templateId, `form-center import must return templateId: ${JSON.stringify(data)}`)
  assert.ok(data.versionNo, `form-center import must return versionNo: ${JSON.stringify(data)}`)
  assert.ok(Array.isArray(data.recognizedFields), `form-center import must return recognizedFields array: ${JSON.stringify(data)}`)
  await page.getByText(/导入成功|已生成/).first().waitFor({ state: 'visible', timeout: 60000 }).catch(() => null)
  await page.screenshot({ path: screenshots.formCenter, fullPage: true }).catch(() => null)
  evidence.results.push({
    scope: 'form-center',
    status: 'PASS',
    route,
    endpoint: '/admin-api/form-center/templates/import-doc',
    file: formCenterDoc,
    templateName,
    templateId: data.templateId,
    versionNo: data.versionNo,
    importAction: data.importAction,
    recognizedFieldCount: data.recognizedFields.length,
    warningCount: Array.isArray(data.warnings) ? data.warnings.length : 0,
    screenshot: screenshots.formCenter
  })
}

async function selectProduct(page, dialog) {
  const select = dialog.locator('.el-form-item').filter({ hasText: '产品名称' }).locator('.el-select').first()
  await select.click({ force: true })
  const input = select.locator('input:visible').first()
  if ((await input.count()) > 0) {
    await input.click({ force: true })
    await input.fill('')
    await input.pressSequentially(productName, { delay: 20 })
  }
  const option = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: productName })
    .first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
}

async function runMesPreflight(page) {
  const route = '/mes/pro/batch-record-form-list'
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText(/批记录名称|表单名称/).first().waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(page.getByRole('button', { name: /^导入$/ }), 'MES Word import button')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入 Word' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await selectProduct(page, dialog)

  const preflightPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-report/recognize-uploaded/preflight') &&
      response.request().method() === 'GET',
    { timeout: 120000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /选择文件/ }), 'MES choose Word file')
  await page.locator('input.batch-record-form-word-import-input[type="file"]').setInputFiles(batchRecordDoc)
  await dialog.getByText(path.basename(batchRecordDoc)).first().waitFor({ state: 'visible', timeout: 60000 })
  const preflightResponse = await preflightPromise
  assert.equal(preflightResponse.status(), 200, 'MES preflight HTTP must be 200')
  const preflight = assertBusinessSuccess(await preflightResponse.json(), 'MES recognize-uploaded preflight')
  await dialog.locator('.batch-record-word-import-form__preflight .el-loading-mask').waitFor({ state: 'hidden', timeout: 60000 }).catch(() => null)
  const allowedActions = Array.isArray(preflight.allowedActions) ? preflight.allowedActions : []
  const confirmDisabled = await dialog.getByRole('button', { name: /^确定$/ }).first().isDisabled()
  await page.screenshot({ path: screenshots.mesPreflight, fullPage: true }).catch(() => null)

  const mesResult = {
    scope: 'mes-batch-record',
    status: allowedActions.length > 0 && !confirmDisabled ? 'READY_TO_IMPORT' : 'BLOCKED',
    route,
    endpoint: '/admin-api/mes/pro/batch-record-report/recognize-uploaded/preflight',
    file: batchRecordDoc,
    productName,
    confirmDisabled,
    allowedActions,
    recommendedAction: preflight.recommendedAction || null,
    latestBatchRecordVersionNo: preflight.latestBatchRecordVersionNo || null,
    latestBatchRecordVersionStatus: preflight.latestBatchRecordVersionStatus || null,
    currentBatchRecordVersionNo: preflight.currentBatchRecordVersionNo || null,
    currentBatchRecordHasMainReports: Boolean(preflight.currentBatchRecordHasMainReports),
    routeGovernanceStatus: preflight.routeGovernanceStatus || null,
    currentRouteName: preflight.currentRouteName || null,
    currentRouteVersionNo: preflight.currentRouteVersionNo || null,
    nextVersionNo: preflight.nextVersionNo || null,
    routeUpgradeRequired: Boolean(preflight.routeUpgradeRequired),
    duplicateRouteCount: Array.isArray(preflight.duplicateRoutes) ? preflight.duplicateRoutes.length : 0,
    screenshot: screenshots.mesPreflight
  }
  evidence.results.push(mesResult)
  if (mesResult.status === 'BLOCKED') {
    throw new Error(`BLOCKER: MES Word import preflight returned no enabled import action: ${JSON.stringify(mesResult)}`)
  }
}

async function main() {
  assertPreconditions()
  const browser = await chromium.launch({ headless: process.env.SHARED_WORD_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page, '/approval-center/manager/form-center/template')
    await runFormCenterImport(page).catch(async (error) => {
      if (!evidence.results.some((item) => item.scope === 'form-center')) {
        evidence.results.push({
          scope: 'form-center',
          status: String(error?.message || '').startsWith('BLOCKER:') ? 'BLOCKED' : 'FAIL',
          file: formCenterDoc,
          currentUrl: page.url(),
          error: error?.stack || String(error),
          screenshot: screenshots.formCenterBlocked
        })
        await page.screenshot({ path: screenshots.formCenterBlocked, fullPage: true }).catch(() => null)
      }
    })
    await runMesPreflight(page).catch((error) => {
      if (!evidence.results.some((item) => item.scope === 'mes-batch-record')) {
        evidence.results.push({
          scope: 'mes-batch-record',
          status: String(error?.message || '').startsWith('BLOCKER:') ? 'BLOCKED' : 'FAIL',
          file: batchRecordDoc,
          productName,
          currentUrl: page.url(),
          error: error?.stack || String(error),
          screenshot: screenshots.mesPreflight
        })
      }
    })
    evidence.completedAt = new Date().toISOString()
    evidence.status = evidence.results.every((item) => item.status === 'PASS') ? 'PASS' : 'BLOCKED'
  } catch (error) {
    evidence.completedAt = new Date().toISOString()
    evidence.status = String(error?.message || '').startsWith('BLOCKER:') ? 'BLOCKED' : 'FAIL'
    evidence.error = error?.stack || String(error)
    if (!page.isClosed()) {
      await page.screenshot({ path: screenshots.failure, fullPage: true }).catch(() => null)
      evidence.failureScreenshot = screenshots.failure
    }
    throw error
  } finally {
    fs.writeFileSync(evidencePath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
