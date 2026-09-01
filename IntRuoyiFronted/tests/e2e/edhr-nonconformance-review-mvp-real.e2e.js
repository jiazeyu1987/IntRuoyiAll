const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const REPO_ROOT = path.resolve(process.env.NCR_E2E_REPO_ROOT || path.join(process.cwd(), '..'))
const RUNTIME_REGISTRY_PATH =
  process.env.NCR_E2E_RUNTIME_REGISTRY_PATH || 'D:\\IntRuoyiWorktree\\.ports\\worktree-ports.json'

function normalizeWindowsPath(value) {
  return path.resolve(value).replaceAll('/', '\\').replace(/\\+$/, '').toLowerCase()
}

function resolveRegisteredRuntime() {
  const normalizedRepoRoot = normalizeWindowsPath(REPO_ROOT)
  if (normalizedRepoRoot === normalizeWindowsPath('E:\\IntRuoyi')) {
    return { profile: 'int_main', slot: 0, frontendPort: 8081, backendPort: 48081 }
  }
  assert.ok(
    normalizedRepoRoot.startsWith(`${normalizeWindowsPath('D:\\IntRuoyiWorktree')}\\`),
    `E2E repo root must be E:\\IntRuoyi or a registered worktree: ${REPO_ROOT}`
  )
  assert.ok(fs.existsSync(RUNTIME_REGISTRY_PATH), `Runtime registry not found: ${RUNTIME_REGISTRY_PATH}`)
  const document = JSON.parse(fs.readFileSync(RUNTIME_REGISTRY_PATH, 'utf8'))
  const matches = (document.worktrees || []).filter(
    (entry) => entry.active === true && normalizeWindowsPath(entry.path) === normalizedRepoRoot
  )
  assert.equal(matches.length, 1, `Expected one active runtime registration for ${REPO_ROOT}`)
  const runtime = matches[0]
  assert.ok(Number.isInteger(runtime.frontendPort) && runtime.frontendPort > 0, 'Registered frontend port is invalid')
  assert.ok(Number.isInteger(runtime.backendPort) && runtime.backendPort > 0, 'Registered backend port is invalid')
  return runtime
}

const runtime = resolveRegisteredRuntime()
const BASE_URL = (
  process.env.NCR_E2E_BASE_URL || `http://127.0.0.1:${runtime.frontendPort}`
).replace(/\/+$/, '')
const BACKEND_URL = (
  process.env.NCR_E2E_BACKEND_URL || `http://127.0.0.1:${runtime.backendPort}`
).replace(/\/+$/, '')
const TENANT = process.env.NCR_E2E_TENANT || '测试租户'
const USERNAME = process.env.NCR_E2E_USERNAME || 'admin'
const PASSWORD = process.env.NCR_E2E_PASSWORD || ''
const WORK_ORDER_CODE = process.env.NCR_E2E_WORK_ORDER_CODE || '881MO101355'
const SOURCE_ROUTE_CODE = process.env.NCR_E2E_SOURCE_ROUTE_CODE || 'RT000028'
const RESTORE_ROUTE_ONLY = process.env.NCR_E2E_RESTORE_ROUTE_ONLY === '1'
const PQC_ENTRY_ONLY = process.env.NCR_E2E_PQC_ENTRY_ONLY === '1'
const EXISTING_BATCH_ID = process.env.NCR_E2E_EXISTING_BATCH_ID || ''
const EXISTING_BATCH_EXECUTION_CODE = process.env.NCR_E2E_EXISTING_BATCH_EXECUTION_CODE || ''
const EXISTING_EXECUTION_ID = process.env.NCR_E2E_EXISTING_EXECUTION_ID || ''
const RESUME_PENDING_REVIEW_ID = process.env.NCR_E2E_RESUME_PENDING_REVIEW_ID || ''
const RESUME_PENDING_REVIEW_CODE = process.env.NCR_E2E_RESUME_PENDING_REVIEW_CODE || ''
const SKIP_COMPLETED_CONCESSION = process.env.NCR_E2E_SKIP_COMPLETED_CONCESSION === '1'
const SKIP_COMPLETED_REWORK = process.env.NCR_E2E_SKIP_COMPLETED_REWORK === '1'
const RESUME_VOID_REVIEW_ID = process.env.NCR_E2E_RESUME_VOID_REVIEW_ID || ''
const RESUME_VOID_REVIEW_CODE = process.env.NCR_E2E_RESUME_VOID_REVIEW_CODE || ''
const CHROME_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const RUN_ID = process.env.NCR_E2E_RUN_ID || `ncr-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`
const BATCH_CODE = `NCR-E2E-${RUN_ID}`.slice(0, 96)
const RESULT_DIR = path.resolve(process.cwd(), 'output', 'playwright', 'nonconformance-review-mvp', RUN_ID)
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')

const BATCH_LIST_PATH = '/mes/pro/feedback/edhr-batch-execution'
const BATCH_DETAIL_PATH = '/mes/pro/feedback/edhr-batch-execution/detail'
const PQC_LEADER_PATH = '/mes/pro/process-pool/pqc-leader'
const REVIEW_PATH = '/mes/pro/feedback/edhr-nonconformance-review'
const DOMAIN_TRACE_DETAIL_PATH = '/mes/pro/feedback/edhr-domain-trace/detail'

const BDD_SCENARIOS = [
  'BDD: 两个来源复用统一评审入口 -> Given 受控模拟批次可从批次放行和PQC管理进入不合格审查, When 分别点击不合格审查, Then 两次均进入同一不合格评审页面并创建同一类评审单。',
  'BDD: 冻结后显示三项禁止提示 -> Given 不合格评审单已创建, When 返回批次详情, Then 批次显示冻结中并提示禁止报工、PQC提交、PQC放行。',
  'BDD: 三类处置形成最小状态机 -> Given QA从冻结批次列表选中评审并上传材料、填写意见和签名, When 依次让步放行、返工、作废, Then 前两次恢复冻结前状态且最终作废进入终态。',
  'BDD: 追溯按处置差异展示 -> Given 三类评审均已关闭, When 从批次详情进入主数据追溯, Then 追溯展示评审材料、意见、签名、冻结时间以及让步放行、返工、作废差异信息。'
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function writeResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(RESULT_FILE, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function validateConfig() {
  assert.equal(
    BASE_URL,
    `http://127.0.0.1:${runtime.frontendPort}`,
    'E2E frontend must use the current worktree registered port'
  )
  assert.equal(
    BACKEND_URL,
    `http://127.0.0.1:${runtime.backendPort}`,
    'E2E backend must use the current worktree paired registered port'
  )
  assert.ok(PASSWORD, 'NCR_E2E_PASSWORD is required and must not be committed to the test script')
  assert.ok(fs.existsSync(CHROME_EXECUTABLE), `Chrome executable not found: ${CHROME_EXECUTABLE}`)
  if (PQC_ENTRY_ONLY) {
    assert.ok(EXISTING_BATCH_ID, 'NCR_E2E_EXISTING_BATCH_ID is required for PQC entry-only verification')
    assert.ok(EXISTING_BATCH_EXECUTION_CODE, 'NCR_E2E_EXISTING_BATCH_EXECUTION_CODE is required for PQC entry-only verification')
  }
  if (EXISTING_BATCH_ID) {
    assert.match(EXISTING_BATCH_ID, /^\d+$/, 'NCR_E2E_EXISTING_BATCH_ID must be numeric')
    assert.ok(EXISTING_BATCH_EXECUTION_CODE, 'existing batch execution code is required')
    if (!PQC_ENTRY_ONLY) {
      assert.match(EXISTING_EXECUTION_ID, /^\d+$/, 'NCR_E2E_EXISTING_EXECUTION_ID must be numeric')
    }
  }
  if (RESUME_PENDING_REVIEW_ID) {
    assert.match(RESUME_PENDING_REVIEW_ID, /^\d+$/, 'resume review id must be numeric')
    assert.ok(RESUME_PENDING_REVIEW_CODE, 'resume review code is required')
  }
  if (RESUME_VOID_REVIEW_ID) {
    assert.match(RESUME_VOID_REVIEW_ID, /^\d+$/, 'resume void review id must be numeric')
    assert.ok(RESUME_VOID_REVIEW_CODE, 'resume void review code is required')
  }
}

function loadPlaywright() {
  return require('playwright')
}

async function firstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible()) return candidate
  }
  throw new Error(`Cannot find visible ${label}`)
}

async function clickVisible(locator, label) {
  const target = await firstVisible(locator, label)
  await target.click()
  return target
}

async function waitBusinessResponse(page, endpoint, action, method) {
  const responsePromise = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return url.pathname.endsWith(endpoint) && (!method || response.request().method() === method)
    },
    { timeout: 60000 }
  )
  await action()
  const response = await responsePromise
  assert.equal(response.status(), 200, `${endpoint} must return HTTP 200`)
  const body = await response.json()
  assert.equal(body.code, 0, `${endpoint} business response failed: ${body.msg || body.code}`)
  return { response, body }
}

async function applyTableMultiFilter(page, tableKey, fieldKey, fieldLabel, value, responsePredicate, label) {
  const filter = page.locator(`.table-multi-filter[data-table-key="${tableKey}"]`).first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  if ((await filter.locator('.table-multi-filter__condition-row:visible').count()) === 0) {
    await filter.getByRole('button', { name: '新增筛选条件' }).click()
  }

  let field = filter.locator(`.table-multi-filter-field[data-filter-key="${fieldKey}"]`).first()
  if ((await field.count()) === 0 || !(await field.isVisible().catch(() => false))) {
    await filter.locator('.table-multi-filter__field-select').first().click()
    await page.getByRole('option', { name: fieldLabel, exact: true }).last().click()
    field = filter.locator(`.table-multi-filter-field[data-filter-key="${fieldKey}"]`).first()
  }
  await field.waitFor({ state: 'visible', timeout: 30000 })
  const valueInput = field.locator('.table-multi-filter-field__value input:not([readonly])').first()
  await valueInput.fill(value)
  assert.equal(await valueInput.inputValue(), value, `${label} filter value was not written`)
  await filter
    .locator('.table-multi-filter__tabs .el-tabs__item')
    .filter({ hasText: value })
    .first()
    .waitFor({ state: 'visible', timeout: 5000 })

  const responsePromise = page.waitForResponse(responsePredicate, { timeout: 60000 })
  await filter.getByRole('button', { name: /查询/ }).click()
  return responsePromise
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.login-form:visible').first().waitFor({ state: 'visible', timeout: 60000 })
  const loginForm = await firstVisible(
    page.locator('.login-form').filter({
      has: page.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]')
    }),
    'login form'
  )
  const captchaInput = loginForm.locator('input[placeholder*="验证码"]:visible')
  assert.equal(await captchaInput.count(), 0, 'captcha must be disabled for the local E2E profile')

  const tenantInput = await firstVisible(loginForm.locator('input.el-select__input'), 'tenant input')
  await tenantInput.click()
  await tenantInput.fill(TENANT)
  await page.waitForTimeout(300)
  await clickVisible(page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }), 'tenant option')

  const usernameInput = await firstVisible(
    loginForm.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'),
    'username input'
  )
  const passwordInput = await firstVisible(loginForm.locator('input[placeholder="请输入密码"]'), 'password input')
  await usernameInput.fill(USERNAME)
  await passwordInput.fill(PASSWORD)

  const permissionResponse = page.waitForResponse(
    (response) => new URL(response.url()).pathname.endsWith('/system/auth/get-permission-info'),
    { timeout: 90000 }
  )
  await clickVisible(loginForm.getByRole('button', { name: /^登录$/ }), 'login button')
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 90000 })
  const response = await permissionResponse
  assert.equal(response.status(), 200, 'permission bootstrap must return HTTP 200')
  const body = await response.json()
  assert.equal(body.code, 0, `permission bootstrap failed: ${body.msg || body.code}`)
}

async function clickMenuText(page, text) {
  await page.locator('.el-menu:visible').first().waitFor({ state: 'visible', timeout: 90000 })
  const candidates = page.locator('.el-menu').getByText(text, { exact: true })
  const targetText = await firstVisible(candidates, `menu ${text}`)
  const menuTarget = targetText.locator(
    'xpath=ancestor-or-self::*[contains(@class, "el-menu-item") or contains(@class, "el-sub-menu__title")][1]'
  )
  assert.equal(await menuTarget.count(), 1, `menu ${text} must resolve to one clickable menu target`)
  const targetClass = (await menuTarget.getAttribute('class')) || ''
  if (targetClass.includes('el-sub-menu__title')) {
    const subMenu = menuTarget.locator('xpath=..')
    const subMenuClass = (await subMenu.getAttribute('class')) || ''
    if (subMenuClass.includes('is-opened')) return
  }
  await menuTarget.click()
  await page.waitForTimeout(200)
}

async function openBatchListFromMenu(page) {
  await clickMenuText(page, 'MES 系统')
  await clickMenuText(page, 'eDHR批记录')
  const responsePromise = page.waitForResponse(
    (response) => new URL(response.url()).pathname.endsWith('/mes/pro/edhr-batch-execution/page'),
    { timeout: 60000 }
  )
  await clickMenuText(page, '批次执行')
  await page.waitForURL((url) => url.pathname === BATCH_LIST_PATH, { timeout: 60000 })
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `batch list failed: ${body.msg || body.code}`)
}

async function openQaListFromMenu(page) {
  if (new URL(page.url()).pathname === REVIEW_PATH) {
    const refreshButton = page.getByRole('button', { name: '刷新' }).first()
    await refreshButton.waitFor({ state: 'visible', timeout: 60000 })
    const responsePromise = page.waitForResponse(
      (response) => new URL(response.url()).pathname.endsWith('/mes/pro/edhr-nonconformance-review/pending-page'),
      { timeout: 60000 }
    )
    await refreshButton.click()
    const response = await responsePromise
    const body = await response.json()
    assert.equal(body.code, 0, `QA pending list refresh failed: ${body.msg || body.code}`)
    return
  }
  await clickMenuText(page, 'MES 系统')
  await clickMenuText(page, '生产管理')
  await clickMenuText(page, 'eDHR不合格评审')
  await page.waitForURL((url) => url.pathname === REVIEW_PATH, { timeout: 60000 })
  const refreshButton = page.getByRole('button', { name: '刷新' }).first()
  await refreshButton.waitFor({ state: 'visible', timeout: 60000 })
  const responsePromise = page.waitForResponse(
    (response) => new URL(response.url()).pathname.endsWith('/mes/pro/edhr-nonconformance-review/pending-page'),
    { timeout: 60000 }
  )
  await refreshButton.click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `QA pending list failed: ${body.msg || body.code}`)
}

async function openPqcLeaderManagementFromMenu(page) {
  const responsePromise = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return (
        url.pathname.endsWith('/mes/pro/process-pool/team-leader/submission/page') &&
        url.searchParams.get('leaderType') === 'PQC'
      )
    },
    { timeout: 60000 }
  )
  await clickMenuText(page, 'MES 系统')
  await clickMenuText(page, '生产管理')
  await clickMenuText(page, 'PQC组长')
  await page.waitForURL((url) => url.pathname === PQC_LEADER_PATH, { timeout: 60000 })
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `PQC management list failed: ${body.msg || body.code}`)
}

async function openRouteListFromMenu(page) {
  await clickMenuText(page, 'MES 系统')
  await clickMenuText(page, '生产管理')
  const responsePromise = page.waitForResponse(
    (response) => new URL(response.url()).pathname.endsWith('/mes/pro/route/page'),
    { timeout: 60000 }
  )
  await clickMenuText(page, '工艺流程')
  await page.waitForURL((url) => url.pathname === '/mes/pro/route', { timeout: 60000 })
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `route list failed: ${body.msg || body.code}`)
}

async function findSourceRouteRow(page) {
  await openRouteListFromMenu(page)
  const response = await applyTableMultiFilter(
    page,
    'mes.pro.route.main.admin-layout-v1',
    'code',
    '路线编码',
    SOURCE_ROUTE_CODE,
    (response) => {
      const url = new URL(response.url())
      return url.pathname.endsWith('/mes/pro/route/page') && url.searchParams.get('code') === SOURCE_ROUTE_CODE
    },
    'source route code'
  )
  const body = await response.json()
  assert.equal(body.code, 0, `route filter failed: ${body.msg || body.code}`)
  assert.equal(body.data?.list?.length, 1, `route filter must return exactly one ${SOURCE_ROUTE_CODE}`)
  assert.equal(body.data.list[0].code, SOURCE_ROUTE_CODE)
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: SOURCE_ROUTE_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function setSourceRouteEnabled(page, enabled) {
  const row = await findSourceRouteRow(page)
  const routeSwitch = row.locator('.el-switch').first()
  const currentEnabled = await routeSwitch.evaluate((element) => element.classList.contains('is-checked'))
  if (currentEnabled === enabled) return false

  const responsePromise = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return url.pathname.endsWith('/mes/pro/route/update-status') && response.request().method() === 'PUT'
    },
    { timeout: 60000 }
  )
  await routeSwitch.click()
  const confirm = page.locator('.el-message-box:visible').first()
  await confirm.waitFor({ state: 'visible', timeout: 30000 })
  await confirm.getByRole('button', { name: /确定/ }).click()
  const response = await responsePromise
  assert.equal(response.status(), 200, 'route status update must return HTTP 200')
  const body = await response.json()
  assert.equal(body.code, 0, `route status update failed: ${body.msg || body.code}`)
  await page.waitForTimeout(500)
  const updatedRow = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: SOURCE_ROUTE_CODE }).first()
  await updatedRow.waitFor({ state: 'visible', timeout: 30000 })
  const updatedEnabled = await updatedRow
    .locator('.el-switch')
    .first()
    .evaluate((element) => element.classList.contains('is-checked'))
  assert.equal(updatedEnabled, enabled, `source route enabled state must be ${enabled}`)
  return true
}

async function closeVisibleBusinessDialogs(page) {
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const dialog = page.locator('.el-dialog:visible').first()
    if ((await dialog.count()) === 0 || !(await dialog.isVisible())) return true
    const closeButton = dialog.locator('.el-dialog__headerbtn').first()
    if ((await closeButton.count()) > 0 && (await closeButton.isVisible())) {
      await closeButton.click()
    } else {
      await page.keyboard.press('Escape')
    }
    await page.waitForTimeout(300)
  }
  return (await page.locator('.el-dialog:visible').count()) === 0
}

async function createBatch(page) {
  await openBatchListFromMenu(page)
  await clickVisible(page.getByRole('button', { name: '打开/创建' }), 'open/create batch button')
  const dialogLocator = page.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' })
  await dialogLocator.first().waitFor({ state: 'visible', timeout: 30000 })
  const dialog = await firstVisible(dialogLocator, 'batch create dialog')

  const workOrderItem = dialog.locator('.el-form-item').filter({ hasText: '生产工单' })
  const workOrderInput = await firstVisible(workOrderItem.locator('input'), 'work order select input')
  await workOrderInput.click()
  await workOrderInput.fill(WORK_ORDER_CODE)
  const workOrderPage = page.waitForResponse(
    (response) => new URL(response.url()).pathname.endsWith('/mes/pro/work-order/page'),
    { timeout: 60000 }
  )
  await page.waitForTimeout(500)
  await workOrderPage
  await clickVisible(
    page.locator('.edhr-batch-page__work-order-select-popper:visible .el-select-dropdown__item').filter({ hasText: WORK_ORDER_CODE }),
    `work order ${WORK_ORDER_CODE}`
  )

  const routeItem = dialog.locator('.el-form-item').filter({ hasText: '工艺路线' })
  const routeInput = await firstVisible(routeItem.locator('input'), 'route select input')
  await page.waitForTimeout(800)
  if (!new RegExp(SOURCE_ROUTE_CODE).test((await routeItem.innerText()).replace(/\s+/g, ' '))) {
    await routeInput.click()
    await clickVisible(
      page.locator('.edhr-batch-page__work-order-select-popper:visible .el-select-dropdown__item'),
      'route option'
    )
  }

  await dialog.getByPlaceholder('请输入真实批次号').fill(BATCH_CODE)
  await dialog.locator('textarea').fill(`nonconformance review MVP E2E ${RUN_ID}`)
  const { body } = await waitBusinessResponse(
    page,
    '/mes/pro/edhr-batch-execution/open-or-create-manual',
    () => clickVisible(dialog.getByRole('button', { name: /确\s*认/ }), 'confirm batch create'),
    'POST'
  )
  const batch = body.data
  assert.ok(batch?.id, 'open-or-create-manual must return batch execution id')
  assert.equal(batch.batchCode, BATCH_CODE, 'created batch code must match task-owned fixture')
  await page.waitForURL((url) => url.pathname === BATCH_DETAIL_PATH && url.searchParams.get('id') === String(batch.id), {
    timeout: 60000
  })
  return batch
}

async function openExistingBatchFromList(page) {
  await openBatchListFromMenu(page)
  const filter = page.locator('.table-multi-filter[data-table-key="mes.pro.edhrBatch.execution.main"]').first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  if ((await filter.locator('.table-multi-filter__condition-row').count()) === 0) {
    await filter.getByRole('button', { name: '新增筛选条件' }).click()
  }
  const conditionRow = filter.locator('.table-multi-filter__condition-row').first()
  await conditionRow.waitFor({ state: 'visible', timeout: 30000 })
  const fieldControl = conditionRow.locator('.table-multi-filter__field-select')
  if (!/批次执行编码/.test((await fieldControl.innerText()).trim())) {
    await fieldControl.click()
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: '批次执行编码' })
    await option.first().waitFor({ state: 'visible', timeout: 30000 })
    await clickVisible(option, 'batch execution code filter option')
  }
  await conditionRow.getByRole('textbox', { name: '请输入批次执行编码' }).fill(
    EXISTING_BATCH_EXECUTION_CODE
  )
  const pageResponse = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return url.pathname.endsWith('/mes/pro/edhr-batch-execution/page')
    },
    { timeout: 60000 }
  )
  await conditionRow.getByRole('button', { name: /查询/ }).click()
  const response = await pageResponse
  const body = await response.json()
  assert.equal(body.code, 0, `existing batch filter failed: ${body.msg || body.code}`)
  assert.equal(body.data?.list?.length, 1, 'existing batch filter must return exactly one row')
  assert.equal(String(body.data.list[0].id), EXISTING_BATCH_ID)
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: EXISTING_BATCH_EXECUTION_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const detailResponse = page.waitForResponse(
    (detail) => {
      const url = new URL(detail.url())
      return url.pathname.endsWith('/mes/pro/edhr-batch-execution/get') && url.searchParams.get('id') === EXISTING_BATCH_ID
    },
    { timeout: 60000 }
  )
  await row.getByRole('button', { name: EXISTING_BATCH_EXECUTION_CODE, exact: true }).click()
  await page.waitForURL((url) => url.pathname === BATCH_DETAIL_PATH && url.searchParams.get('id') === EXISTING_BATCH_ID, {
    timeout: 60000
  })
  const detail = await detailResponse
  const detailBody = await detail.json()
  assert.equal(detailBody.code, 0, `existing batch detail failed: ${detailBody.msg || detailBody.code}`)
  return detailBody.data
}

async function openTraceExecution(page, batch) {
  const processHeads = page.locator('.edhr-batch-detail__process-task-group-head')
  const count = await processHeads.count()
  for (let index = 0; index < count; index += 1) {
    const head = processHeads.nth(index)
    if (!(await head.isVisible())) continue
    await head.click()
    const openButton = page.getByRole('button', { name: /打开填写|打开返工/ }).first()
    if ((await openButton.count()) === 0 || !(await openButton.isVisible()) || (await openButton.isDisabled())) continue
    const { body } = await waitBusinessResponse(
      page,
      '/mes/pro/edhr-batch-execution/task/open',
      () => openButton.click(),
      'POST'
    )
    const executionId = body.data?.executionId
    assert.ok(executionId, 'task/open must return executionId for trace verification')
    await page.waitForURL((url) => /\/mes\/pro\/feedback\/edhr-execution\/(detail|form)$/.test(url.pathname), {
      timeout: 60000
    })
    await page.goBack({ waitUntil: 'domcontentloaded' })
    await page.waitForURL((url) => url.pathname === BATCH_DETAIL_PATH && url.searchParams.get('id') === String(batch.id), {
      timeout: 60000
    })
    return { executionId, processIndex: index }
  }
  throw new Error('No enabled real process task is available to create trace execution')
}

async function openReleaseReviewEntry(page, batch) {
  const releaseProcess = page.locator('.edhr-batch-detail__release-process-item').first()
  await releaseProcess.waitFor({ state: 'visible', timeout: 90000 })
  await releaseProcess.click()
  const button = await firstVisible(page.getByRole('button', { name: '不合格审查' }), 'release nonconformance review button')
  assert.equal(await button.isDisabled(), false, 'release nonconformance review button must be enabled')
  await button.click()
  await page.waitForURL((url) => url.pathname === REVIEW_PATH, { timeout: 60000 })
  const url = new URL(page.url())
  assert.equal(url.searchParams.get('sourceType'), 'PQC_RELEASE')
  assert.equal(url.searchParams.get('batchExecutionId'), String(batch.id))
}

async function openPqcReviewEntry(page, batch) {
  await openPqcLeaderManagementFromMenu(page)
  const filter = page.locator('.table-multi-filter[data-table-key="mes.processPool.teamLeader.submissions"]').first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  if ((await filter.locator('.table-multi-filter__condition-row').count()) === 0) {
    await filter.getByRole('button', { name: '新增筛选条件' }).click()
  }
  const conditionRow = filter.locator('.table-multi-filter__condition-row').first()
  await conditionRow.waitFor({ state: 'visible', timeout: 30000 })
  const fieldControl = conditionRow.locator('.table-multi-filter__field-select')
  if (!/生产工单/.test((await fieldControl.innerText()).trim())) {
    await fieldControl.click()
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: '生产工单' })
    await option.first().waitFor({ state: 'visible', timeout: 30000 })
    await clickVisible(option, 'PQC work order filter option')
  }
  await conditionRow.getByRole('textbox', { name: '请输入生产工单' }).fill(batch.workOrderCode)
  const pageResponse = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return (
        url.pathname.endsWith('/mes/pro/process-pool/team-leader/submission/page') &&
        url.searchParams.get('leaderType') === 'PQC' &&
        url.searchParams.get('workOrderCode') === batch.workOrderCode
      )
    },
    { timeout: 60000 }
  )
  await conditionRow.getByRole('button', { name: /查询/ }).click()
  const response = await pageResponse
  const body = await response.json()
  assert.equal(body.code, 0, `PQC management filter failed: ${body.msg || body.code}`)
  const matchingRows = (body.data?.list || []).filter(
    (row) => String(row.batchExecutionId) === String(batch.id)
  )
  assert.equal(matchingRows.length, 1, 'PQC management must expose exactly one submitted row for this batch')
  const eventId = String(matchingRows[0].id)
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: batch.workOrderCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const button = row.locator(`[data-pqc-submission-nonconformance-review-event-id="${eventId}"]`)
  await button.waitFor({ state: 'visible', timeout: 90000 })
  assert.equal(await button.isDisabled(), false, 'PQC management nonconformance review button must be enabled')
  await button.click()
  await page.waitForURL((url) => url.pathname === REVIEW_PATH, { timeout: 60000 })
  const url = new URL(page.url())
  assert.equal(url.searchParams.get('sourceType'), 'PQC_SUBMISSION')
  assert.equal(url.searchParams.get('sourceId'), eventId)
  assert.equal(url.searchParams.get('batchExecutionId'), String(batch.id))
  return { eventId, reviewUrl: page.url() }
}

async function createReview(page, sourceType, reason, batch) {
  await page.getByPlaceholder('请输入不合格原因').fill(reason)
  const { body } = await waitBusinessResponse(
    page,
    '/mes/pro/edhr-nonconformance-review/create',
    () => clickVisible(page.getByRole('button', { name: '提交不合格评审' }), 'submit review'),
    'POST'
  )
  const review = body.data
  assert.ok(review?.id, 'review create must return id')
  assert.equal(review.sourceType, sourceType)
  assert.equal(String(review.batchExecutionId), String(batch.id))
  assert.equal(review.reviewStatus, 'pending_review')
  assert.equal(review.disposition == null, true)

  await page.goBack({ waitUntil: 'domcontentloaded' })
  if (sourceType === 'PQC_RELEASE') {
    await page.waitForURL((url) => url.pathname === BATCH_DETAIL_PATH, { timeout: 60000 })
    const detailResponse = page.waitForResponse(
      (response) => {
        const url = new URL(response.url())
        return url.pathname.endsWith('/mes/pro/edhr-batch-execution/get') && url.searchParams.get('id') === String(batch.id)
      },
      { timeout: 60000 }
    )
    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    const detail = await detailResponse
    const detailBody = await detail.json()
    assert.equal(detailBody.code, 0, `frozen batch detail failed: ${detailBody.msg || detailBody.code}`)
    assert.equal(detailBody.data?.status, 15, 'fresh batch detail must preserve frozen status')
    const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
    assert.match(bodyText, /冻结中/)
    assert.match(bodyText, /冻结后禁止报工、PQC提交、PQC放行/)
  }
  await openQaListFromMenu(page)
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: review.reviewCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '处理' }).click()
  return review
}

async function selectPendingReviewFromQa(page, review) {
  await openQaListFromMenu(page)
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: review.reviewCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '处理' }).click()
}

async function disposeReview(page, review, disposition, opinion, signature) {
  const fileInput = page.locator('.edhr-ncr__dispose-form input[type="file"]').first()
  await fileInput.waitFor({ state: 'attached', timeout: 30000 })
  const uploadResponse = page.waitForResponse(
    (response) => new URL(response.url()).pathname.endsWith('/infra/file/upload'),
    { timeout: 60000 }
  )
  await fileInput.setInputFiles({
    name: `${RUN_ID}-${disposition}.txt`,
    mimeType: 'text/plain',
    buffer: Buffer.from(`NCR E2E review material ${RUN_ID} ${disposition}\n`, 'utf8')
  })
  const upload = await uploadResponse
  assert.equal(upload.status(), 200, 'review material upload must return HTTP 200')
  const uploadBody = await upload.json()
  assert.equal(uploadBody.code, 0, `review material upload failed: ${uploadBody.msg || uploadBody.code}`)

  await page.getByPlaceholder('请输入评审意见').fill(opinion)
  await page.getByPlaceholder('请输入QA签名').fill(signature)
  const label = {
    concession_release: '让步放行',
    rework: '返工',
    void: '作废'
  }[disposition]
  const { body } = await waitBusinessResponse(
    page,
    '/mes/pro/edhr-nonconformance-review/dispose',
    () => clickVisible(page.getByRole('button', { name: label }), `${label} button`),
    'POST'
  )
  const closed = body.data
  assert.equal(String(closed.id), String(review.id))
  assert.equal(closed.reviewStatus, 'closed')
  assert.equal(closed.disposition, disposition)
  assert.ok(closed.reviewMaterialUrl)
  assert.equal(closed.reviewOpinion, opinion)
  assert.equal(closed.qaSignature, signature)
  assert.ok(closed.frozenAt)
  assert.ok(closed.closedAt)
  if (disposition === 'void') assert.ok(closed.voidedAt)
  else assert.ok(closed.unfrozenAt)

  await page.getByText(label, { exact: true }).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.screenshot({ path: path.join(RESULT_DIR, `${disposition}.png`), fullPage: true })
  return closed
}

async function openBatchTrace(page, batch, traceExecution, expectedLabels) {
  await page.goto(`${BASE_URL}${BATCH_DETAIL_PATH}?id=${encodeURIComponent(batch.id)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const detailResponse = page.waitForResponse(
    (response) => new URL(response.url()).pathname.endsWith('/mes/pro/batch-record-execution/domain-trace/detail'),
    { timeout: 60000 }
  )
  let traceButton
  const processHeads = page.locator('.edhr-batch-detail__process-task-group-head')
  if ((await processHeads.count()) > traceExecution.processIndex) {
    const processHead = processHeads.nth(traceExecution.processIndex)
    if (await processHead.isVisible()) {
      await processHead.click()
      const traceButtons = page.getByRole('button', { name: '主数据追溯' })
      const traceButtonCount = await traceButtons.count()
      for (let index = 0; index < traceButtonCount; index += 1) {
        const candidate = traceButtons.nth(index)
        if (await candidate.isVisible()) {
          traceButton = candidate
          break
        }
      }
    }
  }
  if (traceButton) {
    assert.equal(await traceButton.isDisabled(), false, 'domain trace must be enabled after opening a real execution')
    await traceButton.click()
  } else {
    await page.goto(
      `${BASE_URL}${DOMAIN_TRACE_DETAIL_PATH}?executionId=${encodeURIComponent(traceExecution.executionId)}`,
      { waitUntil: 'domcontentloaded', timeout: 60000 }
    )
  }
  await page.waitForURL((url) => url.pathname === DOMAIN_TRACE_DETAIL_PATH, { timeout: 60000 })
  const response = await detailResponse
  const body = await response.json()
  assert.equal(body.code, 0, `domain trace detail failed: ${body.msg || body.code}`)
  const pageText = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
  assert.match(pageText, /不合格评审/)
  assert.match(pageText, /评审材料/)
  assert.match(pageText, /评审意见/)
  assert.match(pageText, /QA签名/)
  assert.match(pageText, /冻结(?:时间)?[：:]/)
  for (const label of expectedLabels) assert.match(pageText, new RegExp(label))
  await page.screenshot({ path: path.join(RESULT_DIR, `trace-${expectedLabels.length}.png`), fullPage: true })
  await page.goBack({ waitUntil: 'domcontentloaded' })
  await page.waitForURL((url) => url.pathname === BATCH_DETAIL_PATH, { timeout: 60000 })
}

async function run() {
  validateConfig()
  ensureDir(RESULT_DIR)
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: true, executablePath: CHROME_EXECUTABLE })
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  await context.tracing.start({ screenshots: true, snapshots: true })
  const page = await context.newPage()
  const pageErrors = []
  const consoleErrors = []
  const targetReviewWriteRequests = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('request', (request) => {
    const pathname = new URL(request.url()).pathname
    if (
      request.method() !== 'GET' &&
      [
        '/mes/pro/edhr-nonconformance-review/create',
        '/mes/pro/edhr-nonconformance-review/dispose'
      ].some((endpoint) => pathname.endsWith(endpoint))
    ) {
      targetReviewWriteRequests.push({ method: request.method(), pathname })
    }
  })

  let batch
  const reviews = []
  let sourceRouteNeedsRestore = false
  let sourceRouteRestoreError
  try {
    await login(page)
    if (RESTORE_ROUTE_ONLY) {
      await closeVisibleBusinessDialogs(page)
      await setSourceRouteEnabled(page, false)
      const result = {
        status: 'PASS',
        mode: 'RESTORE_ROUTE_ONLY',
        runId: RUN_ID,
        tenant: TENANT,
        username: USERNAME,
        sourceRouteCode: SOURCE_ROUTE_CODE
      }
      await context.tracing.stop()
      writeResult(result)
      await browser.close()
      console.log(JSON.stringify(result, null, 2))
      return
    }
    if (PQC_ENTRY_ONLY) {
      batch = await openExistingBatchFromList(page)
      const entry = await openPqcReviewEntry(page, batch)
      assert.equal(targetReviewWriteRequests.length, 0, 'PQC entry-only verification must not submit review writes')
      const targetConsoleErrors = consoleErrors.filter((message) =>
        /edhr-nonconformance-review|系统异常|Uncaught|TypeError|ReferenceError/.test(message)
      )
      assert.equal(targetConsoleErrors.length, 0, `target console errors: ${targetConsoleErrors.join(' | ')}`)
      const result = {
        status: 'PASS',
        mode: 'PQC_ENTRY_ONLY',
        runId: RUN_ID,
        tenant: TENANT,
        username: USERNAME,
        runtimeProfile: runtime.profile,
        runtimeSlot: runtime.slot,
        baseUrl: BASE_URL,
        backendUrl: BACKEND_URL,
        batchExecutionId: batch.id,
        batchExecutionCode: batch.batchExecutionCode,
        workOrderCode: batch.workOrderCode,
        pqcSubmissionEventId: entry.eventId,
        reviewUrl: entry.reviewUrl,
        targetReviewWriteRequestCount: targetReviewWriteRequests.length,
        pageErrors,
        consoleErrors,
        targetConsoleErrors,
        resultDir: RESULT_DIR
      }
      await context.tracing.stop()
      writeResult(result)
      await browser.close()
      console.log(JSON.stringify(result, null, 2))
      return
    }
    await openQaListFromMenu(page)
    let traceExecution
    if (EXISTING_BATCH_ID) {
      batch = await openExistingBatchFromList(page)
      traceExecution = { executionId: Number(EXISTING_EXECUTION_ID), processIndex: 0 }
    } else {
      sourceRouteNeedsRestore = await setSourceRouteEnabled(page, true)
      batch = await createBatch(page)
      if (sourceRouteNeedsRestore) {
        await setSourceRouteEnabled(page, false)
        sourceRouteNeedsRestore = false
        await page.goto(`${BASE_URL}${BATCH_DETAIL_PATH}?id=${encodeURIComponent(batch.id)}`, {
          waitUntil: 'domcontentloaded',
          timeout: 60000
        })
      }
      traceExecution = await openTraceExecution(page, batch)
    }

    if (SKIP_COMPLETED_CONCESSION) {
      reviews.push({ id: Number(RESUME_PENDING_REVIEW_ID || 1), disposition: 'concession_release' })
    } else {
      let concessionReview
      if (RESUME_PENDING_REVIEW_ID) {
        concessionReview = {
          id: Number(RESUME_PENDING_REVIEW_ID),
          reviewCode: RESUME_PENDING_REVIEW_CODE
        }
        await selectPendingReviewFromQa(page, concessionReview)
      } else {
        await openReleaseReviewEntry(page, batch)
        concessionReview = await createReview(page, 'PQC_RELEASE', `${RUN_ID} 让步放行原因`, batch)
      }
      reviews.push(await disposeReview(page, concessionReview, 'concession_release', `${RUN_ID} 让步放行评审意见`, 'QA-E2E'))
    }
    await openBatchTrace(page, batch, traceExecution, ['让步放行'])

    if (SKIP_COMPLETED_REWORK) {
      reviews.push({ id: 2, disposition: 'rework' })
    } else {
      await openPqcReviewEntry(page, batch)
      const reworkReview = await createReview(page, 'PQC_SUBMISSION', `${RUN_ID} 返工原因`, batch)
      reviews.push(await disposeReview(page, reworkReview, 'rework', `${RUN_ID} 返工评审意见`, 'QA-E2E'))
    }
    await openBatchTrace(page, batch, traceExecution, ['让步放行', '返工'])

    let voidReview
    if (RESUME_VOID_REVIEW_ID) {
      voidReview = { id: Number(RESUME_VOID_REVIEW_ID), reviewCode: RESUME_VOID_REVIEW_CODE }
      await selectPendingReviewFromQa(page, voidReview)
    } else {
      await page.goto(`${BASE_URL}${BATCH_DETAIL_PATH}?id=${encodeURIComponent(batch.id)}`, {
        waitUntil: 'domcontentloaded',
        timeout: 60000
      })
      await openReleaseReviewEntry(page, batch)
      voidReview = await createReview(page, 'PQC_RELEASE', `${RUN_ID} 作废原因`, batch)
    }
    reviews.push(await disposeReview(page, voidReview, 'void', `${RUN_ID} 作废评审意见`, 'QA-E2E'))
    await openBatchTrace(page, batch, traceExecution, ['让步放行', '返工', '作废'])

    const nonTargetPageErrors = pageErrors.filter((message) => /设备账号 .* 未绑定启用工艺路线/.test(message))
    const targetPageErrors = pageErrors.filter((message) => !nonTargetPageErrors.includes(message))
    assert.equal(targetPageErrors.length, 0, `target page errors: ${targetPageErrors.join(' | ')}`)
    const nonTargetConsoleErrors = consoleErrors.filter((message) =>
      /审批待办数量加载失败/.test(message)
    )
    const targetConsoleErrors = consoleErrors.filter(
      (message) =>
        !nonTargetConsoleErrors.includes(message) &&
        /edhr-nonconformance-review|系统异常|Uncaught|TypeError|ReferenceError/.test(message)
    )
    assert.equal(targetConsoleErrors.length, 0, `target console errors: ${targetConsoleErrors.join(' | ')}`)

    const result = {
      status: 'PASS',
      runId: RUN_ID,
      tenant: TENANT,
      username: USERNAME,
      runtimeProfile: runtime.profile,
      runtimeSlot: runtime.slot,
      baseUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      batchExecutionId: batch.id,
      batchExecutionCode: batch.batchExecutionCode,
      batchCode: BATCH_CODE,
      reviewIds: reviews.map((review) => review.id),
      dispositions: reviews.map((review) => review.disposition),
      bdd: BDD_SCENARIOS,
      pageErrors,
      nonTargetPageErrors,
      nonTargetConsoleErrors,
      targetConsoleErrors,
      resultDir: RESULT_DIR
    }
    await context.tracing.stop({ path: path.join(RESULT_DIR, 'trace.zip') })
    writeResult(result)
    await browser.close()
    console.log(JSON.stringify(result, null, 2))
  } catch (error) {
    if (sourceRouteNeedsRestore) {
      try {
        const dialogsClosed = await closeVisibleBusinessDialogs(page)
        if (!dialogsClosed) {
          await page.goto(`${BASE_URL}/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
        }
        await setSourceRouteEnabled(page, false)
        sourceRouteNeedsRestore = false
      } catch (restoreError) {
        sourceRouteRestoreError = restoreError.message
      }
    }
    await page.screenshot({ path: path.join(RESULT_DIR, 'failure.png'), fullPage: true }).catch(() => undefined)
    await context.tracing.stop({ path: path.join(RESULT_DIR, 'failure-trace.zip') }).catch(() => undefined)
    const result = {
      status: 'FAIL',
      runId: RUN_ID,
      tenant: TENANT,
      username: USERNAME,
      runtimeProfile: runtime.profile,
      runtimeSlot: runtime.slot,
      baseUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      batchExecutionId: batch?.id,
      batchExecutionCode: batch?.batchExecutionCode,
      batchCode: BATCH_CODE,
      completedDispositions: reviews.map((review) => review.disposition),
      bdd: BDD_SCENARIOS,
      pageErrors,
      consoleErrors,
      sourceRouteRestoreError,
      error: {
        name: error.name,
        message: error.message,
        stack: error.stack
      },
      resultDir: RESULT_DIR
    }
    writeResult(result)
    await browser.close().catch(() => undefined)
    console.error(JSON.stringify(result, null, 2))
    process.exitCode = 1
  }
}

run()
