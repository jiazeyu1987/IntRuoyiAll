const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.EDHR_WORD_UPGRADE_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_WORD_UPGRADE_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const TEST_TENANT = process.env.EDHR_WORD_UPGRADE_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_WORD_UPGRADE_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_WORD_UPGRADE_PASSWORD || '111111'
const ALLOW_ADMIN_WRITE = process.env.EDHR_WORD_UPGRADE_ALLOW_ADMIN_WRITE === '1'
const EXPECTED_TENANT_ID = ALLOW_ADMIN_WRITE ? '1' : '122'
const EXPECTED_UPGRADE_VERSION_NO = process.env.EDHR_WORD_UPGRADE_EXPECTED_VERSION_NO || ''
const SHOULD_COMPLETE_APPROVAL = process.env.EDHR_WORD_UPGRADE_COMPLETE_APPROVAL === '1'
const SIGNATURE_PASSWORD = process.env.EDHR_WORD_UPGRADE_SIGNATURE_PASSWORD || TEST_PASSWORD
const APPROVER_USERNAME = process.env.EDHR_WORD_UPGRADE_APPROVER_USERNAME || TEST_USERNAME
const APPROVER_PASSWORD = process.env.EDHR_WORD_UPGRADE_APPROVER_PASSWORD || TEST_PASSWORD
const APPROVER_SIGNATURE_PASSWORD =
  process.env.EDHR_WORD_UPGRADE_APPROVER_SIGNATURE_PASSWORD ||
  process.env.EDHR_WORD_UPGRADE_SIGNATURE_PASSWORD ||
  APPROVER_PASSWORD
const APPROVAL_MODE = process.env.EDHR_WORD_UPGRADE_APPROVAL_MODE || 'BPM_REQUIRED'
const SINGLE_UPGRADE_ONLY = process.env.EDHR_WORD_UPGRADE_SINGLE_UPGRADE_ONLY === '1'
const RESUME_APPROVAL_INSTANCE_ID = process.env.EDHR_WORD_UPGRADE_RESUME_APPROVAL_INSTANCE_ID || ''
const RESUME_BATCH_RECORD_VERSION_ID = process.env.EDHR_WORD_UPGRADE_RESUME_BATCH_RECORD_VERSION_ID || ''
const RESUME_SOURCE_VERSION_ID = process.env.EDHR_WORD_UPGRADE_RESUME_SOURCE_VERSION_ID || ''
const RESUME_VERSION_NO = process.env.EDHR_WORD_UPGRADE_RESUME_VERSION_NO || ''
const RUN_ID = process.env.EDHR_WORD_UPGRADE_RUN_ID || String(Date.now())
let PROJECT_NAME = process.env.EDHR_WORD_UPGRADE_PROJECT_NAME || '球囊扩张压力泵'
const CREATE_ROUTE_FIXTURE = process.env.EDHR_WORD_UPGRADE_CREATE_ROUTE_FIXTURE === '1'
const FIXTURE_SOURCE_ROUTE_ID = process.env.EDHR_WORD_UPGRADE_FIXTURE_SOURCE_ROUTE_ID || ''
const FIXTURE_ROUTE_CODE = process.env.EDHR_WORD_UPGRADE_FIXTURE_ROUTE_CODE || `E2E-BRV-${RUN_ID}`
const SAMPLE_DOC_PATH =
  process.env.EDHR_WORD_UPGRADE_SAMPLE_DOC ||
  'C:\\Users\\BJB110\\Desktop\\文档\\批记录压力泵.doc'
const FALLBACK_DOC_PATH = path.join(
  WORKSPACE_ROOT,
  'ruoyi-vue-pro',
  'yudao-module-mes',
  'src',
  'test',
  'resources',
  'fixtures',
  'pressure-pump-record.doc'
)
const ROUTE = '/mes/pro/batch-record-form-list'
const ROUTE_LIST_ROUTE = '/mes/pro/route'
const APPROVAL_ROUTE = '/approval-center/todo'
const ARTIFACT_DIR = path.resolve(
  process.env.EDHR_WORD_UPGRADE_ARTIFACT_DIR ||
    path.join(WORKSPACE_ROOT, 'doc/tasks/20260721-batch-record-bpm-toggle-implementation/e2e-artifacts/batch-record-version-upgrade')
)

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'Word 升版 E2E 必须使用本机前端 http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'Word 升版 E2E 必须使用本机后端 48081')
  assert.ok(['BPM_REQUIRED', 'DIRECT'].includes(APPROVAL_MODE), `EDHR_WORD_UPGRADE_APPROVAL_MODE 仅支持 BPM_REQUIRED 或 DIRECT，当前=${APPROVAL_MODE}`)
  if (ALLOW_ADMIN_WRITE) {
    assert.equal(TEST_TENANT, '芋道源码', 'admin 写入验证必须显式使用芋道源码租户')
    assert.equal(TEST_USERNAME, 'admin', 'admin 写入验证必须显式使用 admin 账号')
  } else {
    assert.equal(TEST_TENANT, '测试租户', 'Word 升版写入验证必须使用测试租户')
    assert.equal(TEST_USERNAME, 'aoteman', 'Word 升版写入验证必须使用测试租户 aoteman')
  }
  if (CREATE_ROUTE_FIXTURE) {
    assert.equal(ALLOW_ADMIN_WRITE, false, '路线夹具创建只允许在测试租户执行')
    assert.ok(FIXTURE_SOURCE_ROUTE_ID, 'EDHR_WORD_UPGRADE_CREATE_ROUTE_FIXTURE=1 时必须提供 EDHR_WORD_UPGRADE_FIXTURE_SOURCE_ROUTE_ID')
  }
  assert.ok(fs.existsSync(resolveSampleDoc()), `缺少真实 Word 样本：${SAMPLE_DOC_PATH} / ${FALLBACK_DOC_PATH}`)
}

function writeArtifact(payload) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  const artifactPath = path.join(ARTIFACT_DIR, `edhr-word-import-upgrade-${RUN_ID}-${APPROVAL_MODE}.json`)
  fs.writeFileSync(artifactPath, JSON.stringify(payload, null, 2) + '\n', 'utf8')
  return artifactPath
}

function resolveSampleDoc() {
  return fs.existsSync(SAMPLE_DOC_PATH) ? SAMPLE_DOC_PATH : FALLBACK_DOC_PATH
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
    if (await item.isVisible().catch(() => false)) {
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

async function maybeBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    console.log(`WARN: ${label} 未捕获到响应：${result.__error.message}`)
    return undefined
  }
  return result
}

async function unwrapBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    throw new Error(`${label} 等待失败：${result.__error.message}`)
  }
  return result
}

async function loginWithCredentials(page, username, password, targetRoute = ROUTE) {
  await page.context().clearCookies().catch(() => undefined)
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  }).catch(() => undefined)
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  if (!page.url().includes('/login')) {
    await page.goto(`${BASE_URL}${targetRoute}`, { waitUntil: 'commit', timeout: 60000 })
    return
  }
  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }

  const tenantSelect = loginForm.locator('.el-select').first()
  const selectedTenantText = await tenantSelect.innerText().catch(() => '')
  if (!selectedTenantText.includes(TEST_TENANT)) {
    const tenantInput = loginForm.locator('.el-select input[role="combobox"]:visible, input.el-select__input:visible').first()
    if (await tenantInput.count()) {
      await tenantInput.click()
      await tenantInput.fill(TEST_TENANT)
      await page.keyboard.press('Enter')
      await page.waitForTimeout(300)
      const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TEST_TENANT }).first()
      if (await tenantOption.isVisible().catch(() => false)) {
        await tenantOption.click()
      }
    } else {
      const textboxes = loginForm.locator('input.el-input__inner')
      await textboxes.nth(0).fill(TEST_TENANT)
    }
  }

  const usernameInput = loginForm.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  await usernameInput.fill(username)
  await loginForm.locator('input[type="password"]').first().fill(password)
  const loginResponse = page
    .waitForResponse((response) =>
      response.url().includes('/system/auth/login') &&
      response.request().method() === 'POST', {
      timeout: 60000
    })
    .catch((error) => ({ __error: error }))
  await loginForm.getByRole('button', { name: '登录' }).click()
  const response = await unwrapBusinessWait(loginResponse, `${TEST_TENANT}/${username} 登录`)
  const body = await response.json()
  assert.ok(response.ok() && [0, 200].includes(Number(body.code)),
    `${TEST_TENANT}/${username} 登录失败：HTTP ${response.status()} ${body.msg || JSON.stringify(body)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
  await page.goto(`${BASE_URL}${targetRoute}`, { waitUntil: 'commit', timeout: 60000 })
}

async function login(page) {
  await loginWithCredentials(page, TEST_USERNAME, TEST_PASSWORD, ROUTE)
}

async function browserAuth(page) {
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
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
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

async function queryRoutesByProjectName(page, projectName) {
  const routePage = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route/page',
    { pageNo: 1, pageSize: 20, name: projectName },
    '工艺路线夹具查询'
  )
  return (routePage?.list || [])
    .filter((item) => String(item.name || '').trim() === String(projectName || '').trim())
}

function buildRouteListUrl(query) {
  const url = new URL(ROUTE_LIST_ROUTE, BASE_URL)
  for (const [key, value] of Object.entries(query || {})) {
    if (value !== undefined && value !== null && String(value) !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

async function openRouteListPage(page, query) {
  await page.goto(buildRouteListUrl(query), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.el-table').first().waitFor({ state: 'visible', timeout: 60000 })
}

async function copyRouteFixtureThroughUi(page, sourceRoute, projectName) {
  const targetCode = FIXTURE_ROUTE_CODE
  await openRouteListPage(page, { code: sourceRoute.code })
  const sourceRow = page
    .locator('.el-table__body-wrapper .el-table__row')
    .filter({ hasText: String(sourceRoute.code || '') })
    .first()
  await sourceRow.waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(sourceRow.getByRole('button', { name: /^复制$/ }), '复制')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '复制工艺路线' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.locator('.el-form-item').filter({ hasText: '副本编码' }).locator('input').fill(targetCode)
  await dialog.locator('.el-form-item').filter({ hasText: '副本名称' }).locator('input').fill(projectName)
  const copyResponsePromise = waitForBusinessResponse(
    page,
    '/admin-api/mes/pro/route/copy',
    '工艺路线夹具复制',
    'POST',
    120000
  ).catch((error) => ({ __error: error }))
  await clickFirstEnabled(dialog.getByRole('button', { name: /^确认复制$/ }), '确认复制')
  const copiedRouteId = await unwrapBusinessWait(copyResponsePromise, '工艺路线夹具复制')
  await dialog.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
  await openRouteListPage(page, { code: targetCode })
  const targetRow = page
    .locator('.el-table__body-wrapper .el-table__row')
    .filter({ hasText: targetCode })
    .filter({ hasText: projectName })
    .first()
  await targetRow.waitFor({ state: 'visible', timeout: 60000 })
  const routes = await queryRoutesByProjectName(page, projectName)
  const matched = routes.find((item) => Number(item.id) === Number(copiedRouteId)) || routes[0]
  assert.ok(matched, `路线夹具复制后必须能查询到项目路线：${projectName}`)
  assert.ok(matched.activeRouteVersionId, `路线夹具必须带当前激活版本：${JSON.stringify(matched)}`)
  return {
    action: 'created',
    routeId: matched.id,
    routeCode: matched.code,
    routeName: matched.name,
    activeRouteVersionId: matched.activeRouteVersionId,
    activeRouteVersionNo: matched.activeRouteVersionNo,
    sourceRouteId: sourceRoute.id
  }
}

async function ensureRouteFixtureForProject(page) {
  if (!CREATE_ROUTE_FIXTURE) {
    return { action: 'not-requested' }
  }
  const existingRoutes = await queryRoutesByProjectName(page, PROJECT_NAME)
  if (existingRoutes.length > 1) {
    throw new Error(`路线夹具前置失败：项目 ${PROJECT_NAME} 已存在 ${existingRoutes.length} 条路线，不能继续制造重复路线`)
  }
  if (existingRoutes.length === 1) {
    const existingRoute = existingRoutes[0]
    assert.ok(existingRoute.activeRouteVersionId, `已有项目路线缺少当前激活版本：${JSON.stringify(existingRoute)}`)
    return {
      action: 'reused',
      routeId: existingRoute.id,
      routeCode: existingRoute.code,
      routeName: existingRoute.name,
      activeRouteVersionId: existingRoute.activeRouteVersionId,
      activeRouteVersionNo: existingRoute.activeRouteVersionNo
    }
  }
  const sourceRoute = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route/get',
    { id: FIXTURE_SOURCE_ROUTE_ID },
    '工艺路线夹具源路线查询'
  )
  assert.ok(sourceRoute?.id, `源路线不存在：${FIXTURE_SOURCE_ROUTE_ID}`)
  assert.ok(sourceRoute.activeRouteVersionId, `源路线必须带当前激活版本：${JSON.stringify(sourceRoute)}`)
  return copyRouteFixtureThroughUi(page, sourceRoute, PROJECT_NAME)
}

async function resolveProjectNameByApi(page) {
  const projectPage = await authenticatedGet(
    page,
    '/admin-api/dcc/project-codes/page',
    { pageNo: 1, pageSize: 100, keyword: PROJECT_NAME, status: 'ENABLE' },
    'DCC 项目代码候选查询'
  )
  const projectNames = (projectPage?.list || [])
    .map((item) => String(item.projectName || '').trim())
    .filter(Boolean)
  assert.ok(projectNames.length > 0, `测试租户必须存在 DCC 项目名称候选：${PROJECT_NAME}`)

  const preferredProjectName = String(PROJECT_NAME || '').trim()
  return projectNames.includes(preferredProjectName) ? preferredProjectName : projectNames[0]
}

async function uncheckAllRouteProductOptions(dialog) {
  const routeOptions = dialog.locator('.batch-record-word-import-form__route-list .el-checkbox.is-checked')
  for (let index = await routeOptions.count() - 1; index >= 0; index -= 1) {
    const option = routeOptions.nth(index)
    if (await option.isVisible().catch(() => false)) {
      await option.click({ force: true })
    }
  }
  assert.equal(
    await dialog.locator('.batch-record-word-import-form__route-list .el-checkbox.is-checked').count(),
    0,
    '本 E2E 聚焦批记录升版，必须取消所有产线重建项'
  )
}

async function openFormListPage(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('产品名称').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('button', { name: /^导入$/ }).first().waitFor({ state: 'visible', timeout: 60000 })
}

async function selectRemoteOption(page, selectRoot, value, label) {
  await selectRoot.click({ force: true })
  const input = selectRoot.locator('input:visible').first()
  const readonly = await input.evaluate((element) => element.hasAttribute('readonly')).catch(() => false)
  if (!readonly) {
    await input.click({ force: true })
    await input.fill(value)
    await page.waitForTimeout(600)
  }
  const optionItems = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
  const optionCount = await optionItems.count()
  let option = optionItems.filter({ hasText: value }).first()
  for (let index = 0; index < optionCount; index += 1) {
    const candidate = optionItems.nth(index)
    const text = await candidate.innerText().then((item) => item.replace(/\s+/g, '').trim()).catch(() => '')
    if (text === String(value).replace(/\s+/g, '').trim()) {
      option = candidate
      break
    }
  }
  await option.waitFor({ state: 'visible', timeout: 60000 })
  const selectedOptionText = await option.innerText()
    .then((text) => text.split(/\r?\n/).map((item) => item.trim()).find(Boolean) || value)
    .catch(() => value)
  await option.click({ force: true })
  await page.waitForFunction(
    (expected) =>
      Array.from(document.querySelectorAll('.el-dialog input'))
        .some((item) => String(item.value || '').includes(expected)),
    value,
    { timeout: 10000 }
  ).catch(() => undefined)
  const selectedInputValue = await input.inputValue().catch(() => '')
  assert.ok(selectedOptionText, `${label} 必须选择有效选项`)
  return String(selectedInputValue || selectedOptionText).trim()
}

async function startImportDialog(page) {
  await openFormListPage(page)
  console.log('STEP: open import dialog')
  await clickFirstEnabled(page.getByRole('button', { name: /^导入$/ }), '导入')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入 Word' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await selectRemoteOption(
    page,
    dialog.locator('.el-form-item').filter({ hasText: '表单类型' }).locator('.el-select').first(),
    '批记录',
    '表单类型'
  )
  const selectedProjectLabel = await selectRemoteOption(
    page,
    dialog.locator('.el-form-item').filter({ hasText: '产品名称' }).locator('.el-select').first(),
    PROJECT_NAME,
    '产品名称'
  )
  assert.ok(
    selectedProjectLabel.includes(PROJECT_NAME) || PROJECT_NAME.includes(selectedProjectLabel),
    `产品名称下拉选中项必须匹配已解析产品名：selected=${selectedProjectLabel}, project=${PROJECT_NAME}`
  )
  console.log(`STEP: set word file ${resolveSampleDoc()}`)
  await clickFirstEnabled(dialog.getByRole('button', { name: /选择文件/ }), '选择文件')
  await page.locator('input.batch-record-form-word-import-input[type="file"]').setInputFiles(resolveSampleDoc())
  console.log('STEP: wait preflight')
  await dialog.getByText('最新批记录版本').waitFor({ state: 'visible', timeout: 60000 })
  await dialog.locator('.batch-record-word-import-form__preflight .el-loading-mask').waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
  await dialog.getByText(/重建 V1\.0|升版导入/).first().waitFor({ state: 'visible', timeout: 60000 })
  return dialog
}

async function confirmVisibleMessageBox(page, titleText, confirmText) {
  const box = page.locator('.el-message-box:visible').filter({ hasText: titleText }).first()
  await box.waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(box.getByRole('button', { name: confirmText }), confirmText)
  await box.waitFor({ state: 'hidden', timeout: 60000 })
}

async function performImport(page, expectedAction = 'AUTO') {
  const dialog = await startImportDialog(page)
  const dialogText = await dialog.innerText()
  assert.match(dialogText, /最新批记录版本/, '导入弹窗必须展示最新批记录版本')
  assert.match(dialogText, /当前工艺流程版本/, '导入弹窗必须展示当前工艺流程版本')
  if (expectedAction === 'UPGRADE') {
    assert.match(dialogText, /升版导入/, '再次导入必须显示升版导入选项')
    const upgradeButton = dialog.getByText(/升版导入/).first()
    await upgradeButton.click()
  }
  const activeActionText = await dialog
    .locator('.batch-record-word-import-form__action-row .el-radio-button.is-active')
    .first()
    .innerText()
    .catch(() => '')
  const actualAction = expectedAction === 'UPGRADE' || activeActionText.includes('升版导入')
    ? 'UPGRADE'
    : 'REBUILD_V1'
  await uncheckAllRouteProductOptions(dialog)
  const requestInfo = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/pro/batch-record-report/recognize-uploaded') && request.method() === 'POST') {
      requestInfo.push({
        url: request.url(),
        postData: request.postData() || ''
      })
    }
  })
  const uploadResponsePromise = waitForBusinessResponse(
    page,
    '/admin-api/mes/pro/batch-record-report/recognize-uploaded',
    'Word 导入识别保存',
    'POST',
    600000
  ).catch((error) => ({ __error: error }))
  await clickFirstEnabled(dialog.getByRole('button', { name: /^确定$/ }), '确定')
  if (actualAction === 'UPGRADE') {
    await confirmVisibleMessageBox(page, '确认批记录升版', '升版')
  }
  const result = await unwrapBusinessWait(uploadResponsePromise, 'Word 导入识别保存')
  assert.ok(result.batchRecordVersionId, `导入必须返回版本 ID：${JSON.stringify(result)}`)
  assert.ok(result.versionNo, `导入必须返回版本号：${JSON.stringify(result)}`)
  assert.ok(result.importedCount > 0, `导入必须生成表单：${JSON.stringify(result)}`)
  await page.getByText(/解析完成/).first().waitFor({ state: 'visible', timeout: 5000 }).catch(() => undefined)
  if (actualAction === 'UPGRADE') {
    assert.ok(result.sourceBatchRecordVersionId, `升版导入必须返回来源版本：${JSON.stringify(result)}`)
    assert.notEqual(result.versionNo, 'V1.0', `升版导入不能仍是 V1.0：${JSON.stringify(result)}`)
    assert.ok(requestInfo.length > 0, `升版导入必须调用导入写接口：${JSON.stringify(requestInfo)}`)
  }
  return result
}

async function verifyReports(page, importResult) {
  const pageData = await authenticatedGet(
    page,
    '/admin-api/mes/pro/batch-record-report/page',
    { pageNo: 1, pageSize: 200 },
    '导入后批记录表单分页查询'
  )
  const list = (pageData?.list || []).filter(
    (item) => Number(item.batchRecordVersionId) === Number(importResult.batchRecordVersionId)
  )
  assert.ok(
    list.length > 0,
    `导入后必须能按 versionId=${importResult.batchRecordVersionId}/${importResult.versionNo} 查询到表单`
  )
  assert.ok(
    list.every((item) => item.versionNo === importResult.versionNo),
    `表单必须带版本号：${JSON.stringify(list)}`
  )
}

async function queryApprovalTodoByProcess(page, importResult) {
  assert.ok(importResult.approvalInstanceId, `审批中心待办查询必须有流程实例 ID：${JSON.stringify(importResult)}`)
  const pageData = await authenticatedGet(
    page,
    '/admin-api/approval-center/tasks/page',
    {
      pageNo: 1,
      pageSize: 200,
      viewType: 'TODO',
      moduleCode: 'BPM'
    },
    '审批中心 BPM 待办流程实例查询'
  )
  const list = Array.isArray(pageData?.list) ? pageData.list : []
  const matchedIndex = list.findIndex((item) =>
    item?.moduleCode === 'BPM' &&
    item?.sourceTaskType === 'BPM_TASK_TODO' &&
    String(item?.processInstanceId) === String(importResult.approvalInstanceId) &&
    item?.businessStatus === 'TODO' &&
    Array.isArray(item?.availableActions) &&
    item.availableActions.includes('APPROVE') &&
    item.availableActions.includes('REJECT')
  )
  const matched = matchedIndex >= 0 ? list[matchedIndex] : undefined
  assert.ok(matched, `审批中心待办必须按流程实例出现：versionId=${importResult.batchRecordVersionId}, processInstanceId=${importResult.approvalInstanceId}, list=${JSON.stringify(list.slice(0, 10))}`)
  return { ...matched, matchedIndex }
}

async function verifyUpgradeApprovalTodo(page, importResult) {
  assert.equal(importResult.versionStatus, 'PENDING_APPROVAL', `升版导入必须提交待审批版本：${JSON.stringify(importResult)}`)
  assert.ok(importResult.sourceBatchRecordVersionId, `升版导入必须携带来源版本：${JSON.stringify(importResult)}`)
  assert.ok(importResult.approvalInstanceId, `升版导入必须形成审批实例：${JSON.stringify(importResult)}`)
  if (EXPECTED_UPGRADE_VERSION_NO) {
    assert.equal(
      importResult.versionNo,
      EXPECTED_UPGRADE_VERSION_NO,
      `升版导入必须生成指定目标版本：${JSON.stringify(importResult)}`
    )
  }
  await queryApprovalTodoByProcess(page, importResult)
}

async function verifyUpgradeDirectPublished(page, importResult) {
  assert.equal(importResult.versionStatus, 'APPROVED', `DIRECT 升版导入必须直接生效：${JSON.stringify(importResult)}`)
  assert.ok(importResult.sourceBatchRecordVersionId, `DIRECT 升版导入必须携带来源版本：${JSON.stringify(importResult)}`)
  assert.equal(importResult.approvalInstanceId == null, true, `DIRECT 升版导入不得生成审批实例：${JSON.stringify(importResult)}`)
  if (EXPECTED_UPGRADE_VERSION_NO) {
    assert.equal(
      importResult.versionNo,
      EXPECTED_UPGRADE_VERSION_NO,
      `DIRECT 升版导入必须生成指定目标版本：${JSON.stringify(importResult)}`
    )
  }
  const reportPage = await authenticatedGet(
    page,
    '/admin-api/mes/pro/batch-record-report/page',
    {
      pageNo: 1,
      pageSize: 200,
      batchRecordName: PROJECT_NAME,
      versionNo: importResult.versionNo
    },
    'DIRECT 升版后批记录表单分页查询'
  )
  const reports = (reportPage?.list || []).filter(
    (item) => String(item.batchRecordVersionId) === String(importResult.batchRecordVersionId)
  )
  assert.ok(reports.length > 0, `DIRECT 升版后必须能查询到新版本表单：versionId=${importResult.batchRecordVersionId}`)
  assert.ok(
    reports.every((item) => item.versionStatus === 'APPROVED'),
    `DIRECT 升版后版本状态必须为 APPROVED：${JSON.stringify(reports)}`
  )
  return {
    versionStatus: 'APPROVED',
    approvedReportCount: reports.length,
    approvalInstanceId: null
  }
}

async function completeUpgradeApprovalFromTodo(page, importResult) {
  assert.ok(SIGNATURE_PASSWORD, '完整审批 E2E 需要电子签名密码')
  let approvalTask = await queryApprovalTodoByProcess(page, importResult)
  let activeSignaturePassword = SIGNATURE_PASSWORD
  if (APPROVER_USERNAME !== TEST_USERNAME) {
    assert.ok(APPROVER_PASSWORD, '实际审批人登录密码不能为空')
    assert.ok(APPROVER_SIGNATURE_PASSWORD, '实际审批人电子签名密码不能为空')
    await loginWithCredentials(page, APPROVER_USERNAME, APPROVER_PASSWORD, APPROVAL_ROUTE)
    approvalTask = await queryApprovalTodoByProcess(page, importResult)
    activeSignaturePassword = APPROVER_SIGNATURE_PASSWORD
  }
  await page.goto(
    `${BASE_URL}${APPROVAL_ROUTE}?moduleCode=BPM`,
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  await page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  ).catch(() => undefined)
  await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
  const approvalRow = await findApprovalRowByTask(page, approvalTask, importResult)
  const reviewResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/review') &&
      response.request().method() === 'POST',
    { timeout: 120000 }
  ).then(async (response) => {
    let requestPayload
    try {
      requestPayload = response.request().postDataJSON()
    } catch {
      requestPayload = undefined
    }
    assert.equal(response.status(), 200, '审批中心升版审核 HTTP 必须为 200')
    return {
      data: assertBusinessSuccess(await response.json(), '审批中心升版审核'),
      requestPayload
    }
  }).catch((error) => ({ __error: error }))
  await clickFirstEnabled(approvalRow.getByRole('button', { name: /^审核$/ }), '审批中心审核')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '审核确认' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText('审核通过', { exact: true }).click()
  await fillFirstVisible(dialog.locator('input[type="password"]'), activeSignaturePassword, '电子签名密码')
  await clickFirstEnabled(dialog.getByRole('button', { name: /确认审核/ }), '确认审核')
  const reviewEnvelope = await unwrapBusinessWait(reviewResponse, '审批中心升版审核')
  assert.equal(
    String(reviewEnvelope.requestPayload?.processInstanceId),
    String(importResult.approvalInstanceId),
    `审批中心审核请求必须锁定本次升版流程：${JSON.stringify(reviewEnvelope.requestPayload)}`
  )
  const reviewResult = reviewEnvelope.data
  assert.equal(reviewResult, true, `统一审批中心审核接口必须返回 true：${JSON.stringify(reviewResult)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => undefined)
  return reviewResult
}

function hasTemplatePlaceholders(value) {
  return /\$\{[^}]+}/.test(String(value || ''))
}

async function findApprovalRowByTask(page, approvalTask, importResult) {
  const rows = page.locator('.el-table__body-wrapper .el-table__row')
  await rows.first().waitFor({ state: 'visible', timeout: 60000 })
  const rowTexts = await rows.evaluateAll((items) => items.map((item) => item.innerText || ''))
  const title = String(approvalTask?.businessTitle || '').trim()
  const businessCode = String(approvalTask?.businessCode || '').trim()
  const businessKey = String(approvalTask?.businessKey || '').trim()
  const sourceTaskId = String(approvalTask?.sourceTaskId || '').trim()
  const versionNo = String(importResult?.versionNo || '').trim()
  const versionId = String(importResult?.batchRecordVersionId || '').trim()
  const candidateRules = []
  if (title && !hasTemplatePlaceholders(title)) {
    candidateRules.push({
      label: `businessTitle=${title}`,
      match: (text) => text.includes(title)
    })
  }
  if (businessCode) {
    candidateRules.push({ label: `businessCode=${businessCode}`, match: (text) => text.includes(businessCode) })
  }
  if (businessKey) {
    candidateRules.push({ label: `businessKey=${businessKey}`, match: (text) => text.includes(businessKey) })
  }
  if (sourceTaskId) {
    candidateRules.push({ label: `sourceTaskId=${sourceTaskId}`, match: (text) => text.includes(sourceTaskId) })
  }
  if (versionNo && PROJECT_NAME) {
    candidateRules.push({
      label: `project+version=${PROJECT_NAME}/${versionNo}`,
      match: (text) => text.includes(PROJECT_NAME) && text.includes(versionNo)
    })
  }
  if (versionNo) {
    candidateRules.push({
      label: `type+version=批记录升版/${versionNo}`,
      match: (text) => text.includes('批记录升版') && text.includes(versionNo)
    })
  }
  if (versionId) {
    candidateRules.push({
      label: `versionId=${versionId}`,
      match: (text) => text.includes(versionId)
    })
  }
  for (const rule of candidateRules) {
    const matchedIndexes = rowTexts
      .map((text, index) => ({ text, index }))
      .filter(({ text }) => rule.match(text))
      .map(({ index }) => index)
    if (matchedIndexes.length === 1) {
      return rows.nth(matchedIndexes[0])
    }
  }
  if (
    Number.isInteger(approvalTask?.matchedIndex) &&
    approvalTask.matchedIndex >= 0 &&
    approvalTask.matchedIndex < rowTexts.length
  ) {
    return rows.nth(approvalTask.matchedIndex)
  }
  throw new Error(
    `无法在审批中心页面唯一定位升版待办：processInstanceId=${importResult.approvalInstanceId}, ` +
      `title=${title}, rowTexts=${JSON.stringify(rowTexts)}`
  )
}

async function verifyApprovedVersion(page, importResult, reviewResult) {
  assert.equal(reviewResult, true, '完整审批 E2E 必须先完成审批中心审核提交')
  const reportPage = await authenticatedGet(
    page,
    '/admin-api/mes/pro/batch-record-report/page',
    {
      pageNo: 1,
      pageSize: 200,
      batchRecordName: PROJECT_NAME,
      versionNo: importResult.versionNo
    },
    '升版审核后批记录表单分页查询'
  )
  const approvedReports = (reportPage?.list || []).filter(
    (item) => String(item.batchRecordVersionId) === String(importResult.batchRecordVersionId)
  )
  assert.ok(
    approvedReports.length > 0,
    `审核通过后必须能查询到新版本表单：versionId=${importResult.batchRecordVersionId}`
  )
  assert.ok(
    approvedReports.every((item) => item.versionStatus === 'APPROVED'),
    `审核通过后版本状态必须为 APPROVED：${JSON.stringify(approvedReports)}`
  )

  const todoPage = await authenticatedGet(
    page,
    '/admin-api/approval-center/tasks/page',
    {
      pageNo: 1,
      pageSize: 200,
      viewType: 'TODO',
      moduleCode: 'BPM'
    },
    '升版审核后审批中心 BPM 待办查询'
  )
  const todoList = Array.isArray(todoPage?.list) ? todoPage.list : []
  const todoMatch = todoList.find(
    (item) =>
      item?.sourceTaskType === 'BPM_TASK_TODO' &&
      String(item?.processInstanceId) === String(importResult.approvalInstanceId)
  )
  assert.equal(todoMatch, undefined, `审核通过后该升版任务不应继续出现在待办：${JSON.stringify(todoMatch)}`)
  return {
    versionStatus: 'APPROVED',
    approvedReportCount: approvedReports.length
  }
}

async function main() {
  assertLocalOnly()
  const launchOptions = { headless: process.env.EDHR_WORD_UPGRADE_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page)
    PROJECT_NAME = await resolveProjectNameByApi(page)
    const routeFixtureSummary = RESUME_APPROVAL_INSTANCE_ID
      ? { action: 'not-requested-resume-approval' }
      : await ensureRouteFixtureForProject(page)
    if (RESUME_APPROVAL_INSTANCE_ID) {
      assert.ok(RESUME_BATCH_RECORD_VERSION_ID, '续跑审批必须提供 EDHR_WORD_UPGRADE_RESUME_BATCH_RECORD_VERSION_ID')
      assert.ok(RESUME_VERSION_NO, '续跑审批必须提供 EDHR_WORD_UPGRADE_RESUME_VERSION_NO')
      const resumeResult = {
        batchRecordVersionId: Number(RESUME_BATCH_RECORD_VERSION_ID),
        sourceBatchRecordVersionId: RESUME_SOURCE_VERSION_ID ? Number(RESUME_SOURCE_VERSION_ID) : undefined,
        versionNo: RESUME_VERSION_NO,
        versionStatus: 'PENDING_APPROVAL',
        approvalInstanceId: RESUME_APPROVAL_INSTANCE_ID
      }
      await verifyUpgradeApprovalTodo(page, resumeResult)
      let approvalSummary
      if (SHOULD_COMPLETE_APPROVAL && APPROVAL_MODE === 'BPM_REQUIRED') {
        const reviewResult = await completeUpgradeApprovalFromTodo(page, resumeResult)
        approvalSummary = await verifyApprovedVersion(page, resumeResult, reviewResult)
      }
      const artifactPath = writeArtifact({
        scenario: 'edhr-word-import-upgrade-action-real-flow',
        tenant: TEST_TENANT,
        username: TEST_USERNAME,
        projectName: PROJECT_NAME,
        approvalMode: APPROVAL_MODE,
        resumeApprovalOnly: true,
        routeFixtureSummary,
        upgradeImportResult: resumeResult,
        approvalSummary
      })
      console.log(
        `PASS: Word import upgrade approval resume E2E project=${PROJECT_NAME} mode=${APPROVAL_MODE} version=${resumeResult.versionNo} source=${resumeResult.sourceBatchRecordVersionId} approval=${resumeResult.approvalInstanceId}${approvalSummary ? ` approvedStatus=${approvalSummary.versionStatus} approvedReports=${approvalSummary.approvedReportCount}` : ''} artifact=${artifactPath}`
      )
      return
    }
    if (ALLOW_ADMIN_WRITE) {
      const result = await performImport(page, 'UPGRADE')
      await verifyReports(page, result)
      let directSummary
      if (APPROVAL_MODE === 'DIRECT') {
        directSummary = await verifyUpgradeDirectPublished(page, result)
      } else {
        await verifyUpgradeApprovalTodo(page, result)
      }
      let approvalSummary
      if (SHOULD_COMPLETE_APPROVAL && APPROVAL_MODE === 'BPM_REQUIRED') {
        const reviewResult = await completeUpgradeApprovalFromTodo(page, result)
        approvalSummary = await verifyApprovedVersion(page, result, reviewResult)
      }
      const artifactPath = writeArtifact({
        scenario: 'edhr-word-import-upgrade-action-real-flow',
        tenant: TEST_TENANT,
        username: TEST_USERNAME,
        projectName: PROJECT_NAME,
        approvalMode: APPROVAL_MODE,
        routeFixtureSummary,
        importResult: {
          batchRecordVersionId: result.batchRecordVersionId,
          sourceBatchRecordVersionId: result.sourceBatchRecordVersionId,
          versionNo: result.versionNo,
          versionStatus: result.versionStatus,
          approvalInstanceId: result.approvalInstanceId
        },
        directSummary,
        approvalSummary
      })
      console.log(
        `PASS: admin Word import upgrade approval formation tenant=${TEST_TENANT} username=${TEST_USERNAME} project=${PROJECT_NAME} mode=${APPROVAL_MODE} version=${result.versionNo} source=${result.sourceBatchRecordVersionId} approval=${result.approvalInstanceId}${directSummary ? ` directStatus=${directSummary.versionStatus} directReports=${directSummary.approvedReportCount}` : ''}${approvalSummary ? ` approvedStatus=${approvalSummary.versionStatus} approvedReports=${approvalSummary.approvedReportCount}` : ''} artifact=${artifactPath}`
      )
    } else if (SINGLE_UPGRADE_ONLY) {
      const upgradeResult = await performImport(page, 'UPGRADE')
      await verifyReports(page, upgradeResult)
      let directSummary
      if (APPROVAL_MODE === 'DIRECT') {
        directSummary = await verifyUpgradeDirectPublished(page, upgradeResult)
      } else {
        await verifyUpgradeApprovalTodo(page, upgradeResult)
      }
      let approvalSummary
      if (SHOULD_COMPLETE_APPROVAL && APPROVAL_MODE === 'BPM_REQUIRED') {
        const reviewResult = await completeUpgradeApprovalFromTodo(page, upgradeResult)
        approvalSummary = await verifyApprovedVersion(page, upgradeResult, reviewResult)
      }
      const artifactPath = writeArtifact({
        scenario: 'edhr-word-import-upgrade-action-real-flow',
        tenant: TEST_TENANT,
        username: TEST_USERNAME,
        projectName: PROJECT_NAME,
        approvalMode: APPROVAL_MODE,
        singleUpgradeOnly: true,
        routeFixtureSummary,
        upgradeImportResult: {
          batchRecordVersionId: upgradeResult.batchRecordVersionId,
          sourceBatchRecordVersionId: upgradeResult.sourceBatchRecordVersionId,
          versionNo: upgradeResult.versionNo,
          versionStatus: upgradeResult.versionStatus,
          approvalInstanceId: upgradeResult.approvalInstanceId
        },
        directSummary,
        approvalSummary
      })
      console.log(
        `PASS: Word import single upgrade action E2E project=${PROJECT_NAME} mode=${APPROVAL_MODE} version=${upgradeResult.versionNo} source=${upgradeResult.sourceBatchRecordVersionId} approval=${upgradeResult.approvalInstanceId}${directSummary ? ` directStatus=${directSummary.versionStatus} directReports=${directSummary.approvedReportCount}` : ''}${approvalSummary ? ` approvedStatus=${approvalSummary.versionStatus} approvedReports=${approvalSummary.approvedReportCount}` : ''} artifact=${artifactPath}`
      )
    } else {
      const firstResult = await performImport(page)
      await verifyReports(page, firstResult)
      const secondResult = await performImport(page, 'UPGRADE')
      await verifyReports(page, secondResult)
      let directSummary
      if (APPROVAL_MODE === 'DIRECT') {
        directSummary = await verifyUpgradeDirectPublished(page, secondResult)
      } else {
        await verifyUpgradeApprovalTodo(page, secondResult)
      }
      let approvalSummary
      if (SHOULD_COMPLETE_APPROVAL && APPROVAL_MODE === 'BPM_REQUIRED') {
        const reviewResult = await completeUpgradeApprovalFromTodo(page, secondResult)
        approvalSummary = await verifyApprovedVersion(page, secondResult, reviewResult)
      }
      const artifactPath = writeArtifact({
        scenario: 'edhr-word-import-upgrade-action-real-flow',
        tenant: TEST_TENANT,
        username: TEST_USERNAME,
        projectName: PROJECT_NAME,
        approvalMode: APPROVAL_MODE,
        routeFixtureSummary,
        firstImportResult: {
          batchRecordVersionId: firstResult.batchRecordVersionId,
          versionNo: firstResult.versionNo,
          versionStatus: firstResult.versionStatus
        },
        upgradeImportResult: {
          batchRecordVersionId: secondResult.batchRecordVersionId,
          sourceBatchRecordVersionId: secondResult.sourceBatchRecordVersionId,
          versionNo: secondResult.versionNo,
          versionStatus: secondResult.versionStatus,
          approvalInstanceId: secondResult.approvalInstanceId
        },
        directSummary,
        approvalSummary
      })
      console.log(
        `PASS: Word import upgrade action E2E project=${PROJECT_NAME} mode=${APPROVAL_MODE} first=${firstResult.versionNo} second=${secondResult.versionNo} source=${secondResult.sourceBatchRecordVersionId} approval=${secondResult.approvalInstanceId}${directSummary ? ` directStatus=${directSummary.versionStatus} directReports=${directSummary.approvedReportCount}` : ''}${approvalSummary ? ` approvedStatus=${approvalSummary.versionStatus} approvedReports=${approvalSummary.approvedReportCount}` : ''} artifact=${artifactPath}`
      )
    }
  } catch (error) {
    const outputDir = path.join(__dirname, 'output', 'edhr-word-import-upgrade-action')
    fs.mkdirSync(outputDir, { recursive: true })
    if (!page.isClosed()) {
      await page.screenshot({ path: path.join(outputDir, `failure-${RUN_ID}.png`), fullPage: true }).catch(() => undefined)
      const html = await page.content().catch(() => '')
      if (html) fs.writeFileSync(path.join(outputDir, `failure-${RUN_ID}.html`), html, 'utf8')
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
