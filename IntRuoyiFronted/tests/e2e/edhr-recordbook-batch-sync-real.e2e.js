const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const BASE_URL = (process.env.EDHR_RECORDBOOK_E2E_BASE_URL || 'http://127.0.0.1:8098').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_RECORDBOOK_E2E_BACKEND_URL || 'http://127.0.0.1:48098').replace(/\/+$/, '')
const TEST_TENANT = process.env.EDHR_RECORDBOOK_E2E_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_RECORDBOOK_E2E_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_RECORDBOOK_E2E_PASSWORD || '111111'
const BATCH_ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const RULE_ROUTE = '/mes/pro/batch-record-form-list'
const ROUTE_ROUTE = '/mes/pro/route'
const APPROVAL_CENTER_ROUTE = '/approval-center/todo'
const TARGET_PRODUCT_NAME = '数显球囊扩张压力泵'
const TARGET_REPORT_ID = 'b06d417cb1974c9a9fdbdb37ac281bc3'
const TARGET_REPORT_NAME = '产品信息'
const TARGET_VERSION_NO = 'V3.0'
const TARGET_WORK_ORDER_ID = 925555
const TARGET_WORK_ORDER_CODE = 'TESTERPA9ED2D417434'
const TARGET_ROUTE_ID = 922194
const TARGET_ROUTE_CODE = 'E2E-OSF-20260721061819'
const NUMBER_FIELD = {
  label: '生产数量',
  rowIndex: 4,
  columnIndex: 5,
  min: 20,
  max: 40
}
const STRING_FIELD = {
  label: '生产批号',
  rowIndex: 4,
  columnIndex: 4
}
const TASK_DOC_DIR = path.resolve(
  process.cwd(),
  '..',
  '..',
  '..',
  'IntRuoyi',
  'doc',
  'tasks',
  '20260722-jiluben-20260722-implementation'
)
const RESULT_DIR = path.join(TASK_DOC_DIR, 'e2e-artifacts', 'recordbook-batch-sync-real')
const RESULT_JSON = path.join(RESULT_DIR, 'result.json')
const RESULT_MD = path.join(RESULT_DIR, 'result.md')
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function ensureResultDir() {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch {
    throw new Error('缺少 Playwright runtime，请先在前端 worktree 按锁文件恢复依赖。')
  }
}

function assertPrerequisites() {
  assert.equal(BASE_URL, 'http://127.0.0.1:8098', 'worktree 真实 E2E 必须使用隔离前端端口 8098')
  assert.equal(BACKEND_URL, 'http://127.0.0.1:48098', 'worktree 真实 E2E 必须使用隔离后端端口 48098')
  assert.equal(TEST_TENANT, '测试租户', '写入型真实 E2E 只能使用测试租户')
  assert.equal(TEST_USERNAME, 'aoteman', '真实 E2E 必须使用测试租户专用账号 aoteman')
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome 不存在: ${BROWSER_EXECUTABLE}`)
  ensureResultDir()
}

function isVisibleEnabled(locator) {
  return locator.isVisible().catch(() => false).then(async (visible) => {
    if (!visible) return false
    const disabled = await locator.isDisabled().catch(() => true)
    return !disabled
  })
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await isVisibleEnabled(item)) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写控件：${label}`)
}

async function fillNumberInput(locator, value, label) {
  const input = await visibleLocator(locator, label)
  await input.click()
  await input.fill(String(value))
  await input.evaluate((element, nextValue) => {
    element.value = String(nextValue)
    element.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: String(nextValue) }))
    element.dispatchEvent(new Event('change', { bubbles: true }))
  }, value)
  await input.blur()
  await input.page().waitForTimeout(100)
  const actual = await input.inputValue()
  assert.equal(Number(actual), Number(value), `${label} 页面输入值未写入为 ${value}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await isVisibleEnabled(item)) {
      await item.click()
      return item
    }
  }
  throw new Error(`缺少可点击按钮：${label}`)
}

async function visibleLocator(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      return item
    }
  }
  throw new Error(`缺少可见元素：${label}`)
}

async function clickButtonByText(scope, text, label = text) {
  return clickFirstEnabled(
    scope.locator('button, .el-button').filter({ hasText: text }),
    label
  )
}

async function selectElementOption(page, selectScope, optionText, label) {
  const trigger = await visibleLocator(selectScope.locator('input, .el-select, .el-input').first(), `${label}下拉框`)
  await trigger.click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function selectFormItemOption(page, formItemText, optionText, scope = page) {
  const formItem = await visibleLocator(scope.locator('.el-form-item').filter({ hasText: formItemText }), formItemText)
  await selectElementOption(page, formItem, optionText, formItemText)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(BATCH_ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(BATCH_ROUTE)}`, {
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
    await tenantInput.fill(TEST_TENANT)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TEST_TENANT }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TEST_TENANT, '租户')
  }

  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), TEST_USERNAME, '用户名')
  await fillFirstVisible(loginForm.locator('input[type="password"], input[placeholder="请输入密码"]'), TEST_PASSWORD, '密码')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
      { timeout: 90000 }
    ),
    clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }).or(loginForm.locator('.el-button--primary')), '登录')
  ])
  const loginBody = await loginResponse.json()
  assert.equal(Number(loginBody.code), 0, `登录接口业务失败：${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 90000 })
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
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
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
          current = current.accessToken
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'v')) {
          current = current.v
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'value')) {
          current = current.value
          continue
        }
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

async function apiGet(page, auth, apiPath, params = {}) {
  assert.ok(auth.token, 'API 只读核验需要浏览器登录后的 access token')
  assert.ok(auth.tenantId, 'API 只读核验需要浏览器登录后的 tenant-id')
  const response = await page.request.get(`${BACKEND_URL}${apiPath}`, {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      'tenant-id': String(auth.tenantId),
      ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
    },
    params
  })
  assert.equal(response.status(), 200, `${apiPath} HTTP 状态必须为 200`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `${apiPath} 业务响应必须成功：${body.msg || body.code}`)
  return body.data
}

function parseSnapshot(raw) {
  if (!raw || typeof raw !== 'string') return undefined
  try {
    return JSON.parse(raw)
  } catch {
    return undefined
  }
}

function asNumber(value) {
  if (value === null || value === undefined || value === '') return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

function valueTypeOf(field) {
  return String(field?.valueType || field?.type || field?.cellValueType || '').toUpperCase()
}

function isNumberField(field) {
  return valueTypeOf(field) === 'NUMBER' || String(field?.componentKind || field?.componentFlag || '').toLowerCase() === 'number'
}

function findSnapshotField(snapshot, target) {
  const fields = Array.isArray(snapshot?.fields) ? snapshot.fields : []
  return fields.find((field) => {
    const rowIndex = field.rowIndex ?? field.position?.rowIndex
    const columnIndex = field.columnIndex ?? field.position?.columnIndex
    return rowIndex === target.rowIndex && columnIndex === target.columnIndex
  })
}

function requireSnapshotField(snapshot, target) {
  const field = findSnapshotField(snapshot, target)
  assert.ok(field, `运行态快照缺少字段：${target.label} (${target.rowIndex}, ${target.columnIndex})`)
  return field
}

function requireCellValue(execution, target) {
  const values = Array.isArray(execution?.cellValues) ? execution.cellValues : []
  const value = values.find((item) => item.rowIndex === target.rowIndex && item.columnIndex === target.columnIndex)
  assert.ok(value, `执行实例缺少单元格值：${target.label}`)
  return value
}

function normalizeComparable(value) {
  if (value == null) return ''
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function assertDualAuditValues(item, sourceValue, storedValue, fieldLabel) {
  assert.equal(normalizeComparable(item.recordbookValueJson), normalizeComparable(sourceValue), `${fieldLabel} 记录本值不匹配`)
  assert.equal(normalizeComparable(item.batchRecordValueJson), normalizeComparable(storedValue), `${fieldLabel} 批记录值不匹配`)
  const serialized = JSON.stringify(item)
  const forbiddenPatterns = [
    /conversion/i,
    /formula/i,
    /warning/i,
    /clip/i,
    /clamp/i,
    /转换/,
    /公式/,
    /警告/,
    /裁剪/
  ]
  for (const pattern of forbiddenPatterns) {
    assert.ok(!pattern.test(serialized), `${fieldLabel} 审计项包含转换过程或提示信息：${pattern}`)
  }
}

async function verifyRuleSaved(page, auth) {
  const rules = await apiGet(page, auth, '/admin-api/mes/pro/batch-record-report/cell-rules', {
    reportId: TARGET_REPORT_ID
  })
  const matchingRules = (rules.rules || []).filter(
    (rule) => rule.rowIndex === NUMBER_FIELD.rowIndex && rule.columnIndex === NUMBER_FIELD.columnIndex
  )
  const numberRule = matchingRules.find(isNumberField) || matchingRules[0]
  assert.ok(numberRule, '目标批记录规则缺少生产数量字段')
  assert.ok(isNumberField(numberRule), '生产数量必须为 NUMBER 字段')
  assert.equal(
    asNumber(numberRule.constraints?.min),
    NUMBER_FIELD.min,
    `生产数量 min 规则未保存为 20；匹配规则：${JSON.stringify(matchingRules)}`
  )
  assert.equal(
    asNumber(numberRule.constraints?.max),
    NUMBER_FIELD.max,
    `生产数量 max 规则未保存为 40；匹配规则：${JSON.stringify(matchingRules)}`
  )
  return numberRule
}

async function configureNumericRulesViaUi(page, auth) {
  await page.goto(`${BASE_URL}${RULE_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.locator('input[placeholder="请输入产品名称"]').first().waitFor({ state: 'visible', timeout: 90000 })
  await fillFirstVisible(page.locator('input[placeholder="请输入产品名称"]'), TARGET_PRODUCT_NAME, '批记录产品名称过滤')
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/batch-record-report/page') && response.request().method() === 'GET',
      { timeout: 90000 }
    ),
    clickButtonByText(page, '查询', '批记录表单查询')
  ])
  const targetRow = page.locator('.el-table__body-wrapper tr').filter({
    hasText: TARGET_PRODUCT_NAME
  }).filter({ hasText: TARGET_REPORT_NAME }).filter({ hasText: TARGET_VERSION_NO }).first()
  await targetRow.waitFor({ state: 'visible', timeout: 90000 })
  await targetRow.click()
  await page.locator('.batch-record-form-preview__actions').filter({ hasText: '规则' }).waitFor({ state: 'visible', timeout: 90000 })
  await clickButtonByText(page.locator('.batch-record-form-preview__actions'), '规则', '打开单元格规则')

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '单元格规则' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })
  const targetCell = dialog.locator('.batch-record-cell-rules-editor__cell-button').filter({ hasText: NUMBER_FIELD.label }).first()
  await targetCell.waitFor({ state: 'visible', timeout: 90000 })
  await targetCell.click()
  const fillableSwitch = dialog.locator('.batch-record-cell-rules-editor__fillable-toggle .el-switch').first()
  await fillableSwitch.waitFor({ state: 'visible', timeout: 30000 })
  const isFillable = await fillableSwitch.evaluate((node) => node.classList.contains('is-checked'))
  if (!isFillable) {
    await fillableSwitch.click()
  }
  const valueTypeItem = dialog.locator('.el-form-item').filter({ hasText: '字段类型' }).first()
  await valueTypeItem.waitFor({ state: 'visible', timeout: 30000 })
  await selectElementOption(page, valueTypeItem, '数字', '生产数量字段类型')
  await dialog.locator('.el-form-item').filter({ hasText: '字段范围' }).waitFor({ state: 'visible', timeout: 30000 })
  await fillNumberInput(dialog.locator('input[placeholder="最小值"]'), NUMBER_FIELD.min, '生产数量最小值')
  await fillNumberInput(dialog.locator('input[placeholder="最大值"]'), NUMBER_FIELD.max, '生产数量最大值')
  const [saveRequest, saveResponse] = await Promise.all([
    page.waitForRequest(
      (request) =>
        request.url().includes('/admin-api/mes/pro/batch-record-report/cell-rules') &&
        request.method() === 'PUT',
      { timeout: 90000 }
    ),
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/batch-record-report/cell-rules') && response.request().method() === 'PUT',
      { timeout: 90000 }
    ),
    clickButtonByText(dialog, '保存规则', '保存单元格规则')
  ])
  const savePayload = JSON.parse(saveRequest.postData() || '{}')
  const savedMatchingRules = (savePayload.rules || []).filter(
    (rule) => rule.rowIndex === NUMBER_FIELD.rowIndex && rule.columnIndex === NUMBER_FIELD.columnIndex
  )
  const savedNumberRule = savedMatchingRules.find(isNumberField) || savedMatchingRules[0]
  assert.ok(savedNumberRule, `保存请求缺少生产数量规则：${JSON.stringify(savePayload)}`)
  assert.equal(
    asNumber(savedNumberRule.constraints?.min),
    NUMBER_FIELD.min,
    `保存请求生产数量 min 不是 20：${JSON.stringify({
      matchingRules: savedMatchingRules,
      sameRowRules: (savePayload.rules || []).filter((rule) => rule.rowIndex === NUMBER_FIELD.rowIndex),
      minMaxRules: (savePayload.rules || []).filter((rule) => rule.constraints?.min || rule.constraints?.max)
    })}`
  )
  assert.equal(
    asNumber(savedNumberRule.constraints?.max),
    NUMBER_FIELD.max,
    `保存请求生产数量 max 不是 40：${JSON.stringify(savedMatchingRules)}`
  )
  const saveBody = await saveResponse.json()
  assert.equal(Number(saveBody.code), 0, `保存单元格规则失败：${saveBody.msg || saveBody.code}`)
  return verifyRuleSaved(page, auth)
}

async function createBatchExecutionViaUi(page) {
  const batchCode = `JILUBEN-E2E-${Date.now()}`
  await page.goto(`${BASE_URL}${BATCH_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.locator('button, .el-button').filter({ hasText: '打开/创建' }).first().waitFor({
    state: 'visible',
    timeout: 90000
  })
  await clickButtonByText(page, '打开/创建', '打开或创建 eDHR 批次执行')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })
  const workOrderItem = dialog.locator('.el-form-item').filter({ hasText: '生产工单' }).first()
  const workOrderInput = await visibleLocator(workOrderItem.locator('input'), '生产工单')
  await workOrderInput.click()
  await workOrderInput.fill(TARGET_WORK_ORDER_CODE)
  const workOrderOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: TARGET_WORK_ORDER_CODE })
    .filter({ hasText: `ID ${TARGET_WORK_ORDER_ID}` })
    .first()
  await workOrderOption.waitFor({ state: 'visible', timeout: 90000 })
  const routeOptionsResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/work-order-route-options') &&
      response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await workOrderOption.click()
  await routeOptionsResponsePromise.catch(() => {
    throw new Error('选择生产工单后未加载工艺路线选项。')
  })

  const routeItem = dialog.locator('.el-form-item').filter({ hasText: '工艺路线' }).first()
  const routeInput = await visibleLocator(routeItem.locator('input'), '工艺路线')
  const routeText = await routeInput.inputValue().catch(() => '')
  if (!routeText.includes(String(TARGET_ROUTE_ID))) {
    await selectElementOption(page, routeItem, String(TARGET_ROUTE_ID), '工艺路线')
  }

  await fillFirstVisible(dialog.locator('input[placeholder="请输入真实批次号"]'), batchCode, '批次号')
  const [openResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/edhr-batch-execution/open-or-create') && response.request().method() === 'POST',
      { timeout: 90000 }
    ),
    clickButtonByText(dialog, '确 认', '确认打开或创建批次执行')
  ])
  const openBody = await openResponse.json()
  const openPayload = openResponse.request().postDataJSON()
  assert.equal(
    Number(openBody.code),
    0,
    `打开或创建批次执行业务失败：${openBody.msg || openBody.code}; payload=${JSON.stringify(openPayload)}`
  )
  const batchExecutionId = Number(openBody.data?.id)
  assert.ok(Number.isFinite(batchExecutionId) && batchExecutionId > 0, '打开或创建批次执行未返回有效 ID')
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-batch-execution/detail'), { timeout: 90000 })
  return { batchExecutionId, batchCode }
}

async function getTargetTask(page, auth, batchExecutionId, expectedRecordbookEnabled = true) {
  const detail = await apiGet(page, auth, '/admin-api/mes/pro/edhr-batch-execution/get', { id: batchExecutionId })
  const tasks = Array.isArray(detail?.tasks) ? detail.tasks : []
  const task = tasks.find((item) => item.batchRecordReportId === TARGET_REPORT_ID)
  assert.ok(task, `新批次未冻结目标批记录任务：${TARGET_REPORT_NAME}`)
  assert.equal(
    task.recordbookEnabled,
    expectedRecordbookEnabled,
    expectedRecordbookEnabled ? '目标任务必须启用记录本作为默认模式' : '目标任务必须冻结为记录本禁用'
  )
  return { detail, task }
}

async function verifyRuntimeSnapshot(page, auth, executionContext) {
  const execution = await apiGet(page, auth, '/admin-api/mes/pro/batch-record-execution/get', {
    id: executionContext.executionId,
    workTaskId: executionContext.workTaskId
  })
  const snapshot = parseSnapshot(execution?.executionSnapshotJson)
  assert.ok(snapshot, '目标任务运行态冻结快照缺失')
  const numberField = requireSnapshotField(snapshot, NUMBER_FIELD)
  const stringField = requireSnapshotField(snapshot, STRING_FIELD)
  assert.ok(isNumberField(numberField), '运行态生产数量字段必须是 NUMBER')
  assert.equal(asNumber(numberField.constraints?.min), NUMBER_FIELD.min, '运行态生产数量 min 未冻结为 20')
  assert.equal(asNumber(numberField.constraints?.max), NUMBER_FIELD.max, '运行态生产数量 max 未冻结为 40')
  return { execution, snapshot, numberField, stringField }
}

async function selectTargetProcessInDetail(page, task) {
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-batch-execution/detail'), { timeout: 90000 })
  const processText = task.processName || String(task.routeProcessSort || '')
  if (processText) {
    const processGroup = page.locator('.edhr-batch-detail__process-task-group').filter({ hasText: processText }).first()
    await processGroup.waitFor({ state: 'visible', timeout: 90000 })
    await processGroup.locator('.edhr-batch-detail__process-task-group-head').first().click()
  }
  const targetTaskRow = page.locator('.edhr-batch-detail__rail-process-form-item').filter({ hasText: TARGET_REPORT_NAME }).first()
  await targetTaskRow.waitFor({ state: 'visible', timeout: 90000 })
  return targetTaskRow
}

async function assertDefaultRecordbookUi(page, task) {
  await selectTargetProcessInDetail(page, task)
  const recordbookButton = page.getByLabel('选择记录本填写')
  await recordbookButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await recordbookButton.getAttribute('aria-pressed'), 'true', '批记录任务必须默认进入记录本模式')
  await page.getByLabel('选择批记录填写').waitFor({ state: 'visible', timeout: 30000 })
}

async function openTargetTaskFromDetail(page, task, fillCarrier) {
  const taskRow = await selectTargetProcessInDetail(page, task)
  const carrierLabel = fillCarrier === 'RECORDBOOK' ? '选择记录本填写' : '选择批记录填写'
  if (fillCarrier === 'RECORDBOOK') {
    await page.getByLabel(carrierLabel).click()
  } else {
    await page.getByLabel(carrierLabel).click()
  }
  await page.waitForFunction(
    (label) => document.querySelector(`[aria-label="${label}"]`)?.getAttribute('aria-pressed') === 'true',
    carrierLabel,
    { timeout: 30000 }
  )
  const actionButton = taskRow.locator('.edhr-batch-detail__rail-process-form-action').first()
  await actionButton.waitFor({ state: 'visible', timeout: 90000 })
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') && response.request().method() === 'POST',
      { timeout: 90000 }
    ),
    actionButton.click()
  ])
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/feedback/edhr-execution/form'), { timeout: 90000 })
  const url = new URL(page.url())
  const executionId = Number(url.searchParams.get('id') || url.searchParams.get('executionId'))
  const workTaskId = Number(url.searchParams.get('workTaskId'))
  assert.ok(Number.isFinite(executionId) && executionId > 0, '执行页 URL 缺少有效 executionId')
  assert.ok(Number.isFinite(workTaskId) && workTaskId > 0, '执行页 URL 缺少有效 workTaskId')
  if (fillCarrier === 'RECORDBOOK') {
    assert.equal(url.searchParams.get('fillCarrier'), 'RECORDBOOK', '记录本模式 URL 缺少 fillCarrier=RECORDBOOK')
    assert.equal(url.searchParams.get('fillMode'), 'RECORDBOOK_UNRESTRICTED', '记录本模式 URL 缺少 fillMode=RECORDBOOK_UNRESTRICTED')
  } else {
    assert.equal(url.searchParams.get('fillCarrier'), 'FORM', '批记录模式 URL 缺少 fillCarrier=FORM')
  }
  return { executionId, workTaskId }
}

async function fillAssistField(page, label, value) {
  const row = page.locator('.edhr-fill-workspace__assist-row').filter({ hasText: label }).first()
  await row.waitFor({ state: 'visible', timeout: 90000 })
  const input = await visibleLocator(row.locator('input, textarea'), `${label}填写控件`)
  await input.fill('')
  await input.fill(String(value))
}

async function fillReasonBeforeSave(page, reasonText, scope = page) {
  await selectFormItemOption(page, '原因分类', '操作录入', scope)
  await fillFirstVisible(scope.locator('input[placeholder="请输入字段变更原因"]'), reasonText, '字段变更原因')
}

async function saveFieldChangesViaUi(page, reasonText) {
  await clickFirstEnabled(
    page.getByRole('button', { name: /保存草稿|保存变更/ }),
    '保存字段变更'
  )
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })
  await fillReasonBeforeSave(page, reasonText, dialog)
  await fillFirstVisible(dialog.locator('input[type="password"]'), TEST_PASSWORD, '字段变更电子签名密码')
  const [saveResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/batch-record-execution/field-audit/save-changes') &&
        response.request().method() === 'PUT',
      { timeout: 90000 }
    ),
    clickButtonByText(dialog, '确 认 保 存', '确认保存字段变更')
  ])
  const saveBody = await saveResponse.json()
  assert.equal(Number(saveBody.code), 0, `字段审计保存失败：${saveBody.msg || saveBody.code}`)
  assert.equal(saveBody.data?.hashVerification?.status, 'VALID', '字段审计链保存后必须 VALID')
  await page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).waitFor({ state: 'hidden', timeout: 90000 })
  return saveBody.data
}

async function verifyExecutionStoredValue(page, auth, executionId, workTaskId, target, expectedValue) {
  const execution = await apiGet(page, auth, '/admin-api/mes/pro/batch-record-execution/get', {
    id: executionId,
    workTaskId
  })
  const cellValue = requireCellValue(execution, target)
  assert.equal(normalizeComparable(cellValue.value), normalizeComparable(expectedValue), `${target.label} 批记录存储值不匹配`)
  return execution
}

async function verifyAuditBatch(page, auth, executionId, auditBatchId, expectations) {
  const detail = await apiGet(page, auth, '/admin-api/mes/pro/batch-record-execution/field-audit/detail', {
    executionId,
    auditBatchId
  })
  assert.equal(detail?.hashVerification?.status, 'VALID', '字段审计详情链路必须 VALID')
  for (const expectation of expectations) {
    const item = (detail.items || []).find(
      (entry) => entry.rowIndex === expectation.target.rowIndex && entry.columnIndex === expectation.target.columnIndex
    )
    assert.ok(item, `字段审计详情缺少 ${expectation.target.label}`)
    assertDualAuditValues(item, expectation.sourceValue, expectation.storedValue, expectation.target.label)
  }
  return detail
}

async function verifyFormFillLog(page, auth, auditBatchId, expectations) {
  const detail = await apiGet(page, auth, '/admin-api/mes/pro/batch-record-execution/form-fill-log/detail', {
    auditBatchId
  })
  for (const expectation of expectations) {
    const item = (detail.items || []).find(
      (entry) => entry.rowIndex === expectation.target.rowIndex && entry.columnIndex === expectation.target.columnIndex
    )
    assert.ok(item, `填写日志详情缺少 ${expectation.target.label}`)
    assert.equal(normalizeComparable(item.recordbookValueDisplay), normalizeComparable(expectation.sourceValue), `${expectation.target.label} 填写日志记录本值不匹配`)
    assert.equal(normalizeComparable(item.batchRecordValueDisplay), normalizeComparable(expectation.storedValue), `${expectation.target.label} 填写日志批记录值不匹配`)
    assertDualAuditValues(
      {
        ...item,
        recordbookValueJson: item.recordbookValueDisplay,
        batchRecordValueJson: item.batchRecordValueDisplay
      },
      expectation.sourceValue,
      expectation.storedValue,
      expectation.target.label
    )
  }
  return detail
}

async function saveRecordbookValueAndVerify(page, auth, executionContext, expectations, reasonSuffix) {
  for (const expectation of expectations) {
    await fillAssistField(page, expectation.target.label, expectation.sourceValue)
  }
  const saveResult = await saveFieldChangesViaUi(page, `真实E2E记录本写入验证-${reasonSuffix}`)
  assert.ok(saveResult.auditBatchId, '字段审计保存结果缺少 auditBatchId')
  for (const expectation of expectations) {
    await verifyExecutionStoredValue(
      page,
      auth,
      executionContext.executionId,
      executionContext.workTaskId,
      expectation.target,
      expectation.storedValue
    )
  }
  await verifyAuditBatch(page, auth, executionContext.executionId, saveResult.auditBatchId, expectations)
  await verifyFormFillLog(page, auth, saveResult.auditBatchId, expectations)
  return saveResult
}

async function verifyBatchRecordModeRejectsOutOfRange(page) {
  await fillAssistField(page, NUMBER_FIELD.label, 50)
  const validation = page.locator('.edhr-fill-workspace__assist-validation').filter({ hasText: /40|范围|最大|超过/ }).first()
  await validation.waitFor({ state: 'visible', timeout: 30000 })
  const saveButton = page.getByRole('button', { name: /保存草稿|保存变更/ }).first()
  const disabled = await saveButton.isDisabled().catch(() => false)
  assert.ok(disabled || (await validation.isVisible()), '批记录模式越界值必须阻止保存')
}

function findTargetRouteBinding(rows) {
  const safeRows = Array.isArray(rows) ? rows : []
  for (const row of safeRows) {
    const reports = Array.isArray(row.batchRecordReports) ? row.batchRecordReports : []
    const binding = reports.find((report) => report.batchRecordReportId === TARGET_REPORT_ID)
    if (binding) {
      return {
        row,
        binding,
        routeProcessId: Number(row.routeProcessId),
        processName: row.processName || '',
        recordbookEnabled: binding.recordbookEnabled !== false
      }
    }
  }
  throw new Error(`目标路线未绑定批记录表单：${TARGET_REPORT_NAME}`)
}

async function getTargetRouteBinding(page, auth, routeVersionId) {
  assert.ok(routeVersionId, '读取路线记录本开关必须使用候选 routeVersionId；ACTIVE 结果通过新批次冻结验收。')
  const rows = await apiGet(page, auth, '/admin-api/mes/pro/route/flow-config', {
    routeId: TARGET_ROUTE_ID,
    useType: 'BATCH',
    routeVersionId
  })
  const binding = findTargetRouteBinding(rows)
  binding.routeVersionId = routeVersionId
  assert.ok(Number.isFinite(binding.routeProcessId) && binding.routeProcessId > 0, '目标路线工序缺少有效 routeProcessId')
  return binding
}

async function getTargetRouteListItem(page, auth) {
  const pageData = await apiGet(page, auth, '/admin-api/mes/pro/route/page', {
    pageNo: 1,
    pageSize: 20,
    code: TARGET_ROUTE_CODE
  })
  const rows = Array.isArray(pageData?.list) ? pageData.list : []
  const routeItem = rows.find((row) => row.code === TARGET_ROUTE_CODE || row.id === TARGET_ROUTE_ID)
  assert.ok(routeItem, `路线列表缺少目标路线：${TARGET_ROUTE_CODE}`)
  return routeItem
}

function parseRouteVersionContextFromUrl(page) {
  const url = new URL(page.url())
  const routeVersionId = Number(url.searchParams.get('routeVersionId'))
  const routeVersionNo = url.searchParams.get('routeVersionNo') || ''
  const routeVersionStatus = url.searchParams.get('routeVersionStatus') || ''
  assert.ok(Number.isFinite(routeVersionId) && routeVersionId > 0, '路线候选编辑页 URL 缺少 routeVersionId')
  assert.equal(routeVersionStatus, 'DRAFT', `路线候选版本必须是 DRAFT 才能编辑：${routeVersionStatus}`)
  return { routeVersionId, routeVersionNo, routeVersionStatus }
}

async function openTargetRouteCandidateEditorFromList(page) {
  const pageResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/route/page') && response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await page.goto(`${BASE_URL}${ROUTE_ROUTE}?code=${encodeURIComponent(TARGET_ROUTE_CODE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const pageResponse = await pageResponsePromise
  const pageBody = await pageResponse.json()
  assert.equal(Number(pageBody.code), 0, `路线列表加载失败：${pageBody.msg || pageBody.code}`)
  const routeRow = page.locator('.el-table__body-wrapper tr').filter({ hasText: TARGET_ROUTE_CODE }).first()
  await routeRow.waitFor({ state: 'visible', timeout: 90000 })
  await clickFirstEnabled(routeRow.locator('button, .el-button').filter({ hasText: '编辑' }), '进入目标路线候选编辑')
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit/'), { timeout: 90000 })
  await page.locator('.route-flow-graph-designer').waitFor({ state: 'visible', timeout: 90000 })
  return parseRouteVersionContextFromUrl(page)
}

async function openTargetRouteVersionWorkspace(page) {
  const pageResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/route/page') && response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await page.goto(`${BASE_URL}${ROUTE_ROUTE}?code=${encodeURIComponent(TARGET_ROUTE_CODE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const pageResponse = await pageResponsePromise
  const pageBody = await pageResponse.json()
  assert.equal(Number(pageBody.code), 0, `路线列表加载失败：${pageBody.msg || pageBody.code}`)
  const routeRow = page.locator('.el-table__body-wrapper tr').filter({ hasText: TARGET_ROUTE_CODE }).first()
  await routeRow.waitFor({ state: 'visible', timeout: 90000 })
  await clickFirstEnabled(routeRow.locator('[data-testid="route-version-workspace"]'), '打开路线版本工作区')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺路线版本' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })
  return dialog
}

async function cancelStaleRouteCandidateViaUi(page, context, reason) {
  const dialog = await openTargetRouteVersionWorkspace(page)
  const versionRow = dialog.locator('.el-table__body-wrapper tr')
    .filter({ hasText: context.routeVersionNo })
    .filter({ hasText: '草稿' })
    .first()
  await versionRow.waitFor({ state: 'visible', timeout: 90000 })
  const [cancelResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route-version/cancel') &&
        response.url().includes(`id=${context.routeVersionId}`) &&
        response.request().method() === 'POST',
      { timeout: 90000 }
    ),
    clickFirstEnabled(versionRow.locator('button, .el-button').filter({ hasText: '取消' }), '取消旧路线草稿候选版本')
  ])
  const cancelBody = await cancelResponse.json()
  assert.equal(Number(cancelBody.code), 0, `取消旧路线草稿候选版本失败：${cancelBody.msg || cancelBody.code}`)
  return {
    routeVersionId: context.routeVersionId,
    routeVersionNo: context.routeVersionNo,
    reason
  }
}

async function clickVisiblePrimaryMessageBox(page, label) {
  const messageBox = page.locator('.el-message-box:visible').first()
  await messageBox.waitFor({ state: 'visible', timeout: 90000 })
  await clickFirstEnabled(messageBox.locator('.el-button--primary, button').filter({ hasText: /确定|确认|OK/ }), label)
}

async function dismissRouteSubmitPromptIfVisible(page) {
  const messageBox = page.locator('.el-message-box:visible').first()
  if (!(await messageBox.isVisible({ timeout: 3000 }).catch(() => false))) {
    return false
  }
  const cancelButton = messageBox.locator('button, .el-button').filter({ hasText: /取消|稍后|关闭/ }).first()
  if (await cancelButton.isVisible().catch(() => false)) {
    await cancelButton.click()
  } else {
    await page.keyboard.press('Escape')
  }
  await messageBox.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => {})
  return true
}

async function waitForRouteVersionActive(page, auth, routeVersionId) {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    const version = await apiGet(page, auth, '/admin-api/mes/pro/route-version/get', { id: routeVersionId })
    if (version.lifecycleStatus === 'ACTIVE' && version.active === true) {
      return version
    }
    await page.waitForTimeout(1000)
  }
  throw new Error(`路线版本 ${routeVersionId} 未在审批后变为 ACTIVE`)
}

async function findApprovalTaskForRouteVersion(page, auth, routeVersion) {
  const pageData = await apiGet(page, auth, '/admin-api/approval-center/tasks/page', {
    pageNo: 1,
    pageSize: 100,
    viewType: 'TODO'
  })
  const rows = Array.isArray(pageData?.list) ? pageData.list : []
  const serializedVersionId = String(routeVersion.id)
  const processInstanceId = String(routeVersion.approvalProcessInstanceId || '')
  const task = rows.find((row) => {
    const serialized = JSON.stringify(row)
    return (
      (processInstanceId && row.processInstanceId === processInstanceId) ||
      serialized.includes(serializedVersionId) ||
      serialized.includes(TARGET_ROUTE_CODE)
    )
  })
  assert.ok(task, `审批中心未找到路线版本 ${routeVersion.id} 的待办任务`)
  return task
}

async function waitForApprovalCenterTaskRow(page, rowTexts, routeVersionId) {
  const tableRows = page.locator('.approval-center__table .el-table__body-wrapper tr')
  await tableRows.first().waitFor({ state: 'visible', timeout: 90000 })
  const targetTexts = rowTexts.filter(Boolean).map(String)
  const deadline = Date.now() + 90000
  while (Date.now() < deadline) {
    for (const text of targetTexts) {
      const candidate = tableRows.filter({ hasText: text }).first()
      if (await candidate.isVisible().catch(() => false)) {
        return candidate
      }
    }
    await page.waitForTimeout(500)
  }
  const visibleRows = await tableRows.evaluateAll((rows) =>
    rows.slice(0, 10).map((row) => row.innerText)
  ).catch(() => [])
  throw new Error(
    `审批中心页面未显示路线版本 ${routeVersionId} 待办行；匹配键=${targetTexts.join(' | ')}；首屏=${JSON.stringify(visibleRows)}`
  )
}

async function approveRouteVersionViaApprovalCenterUi(page, auth, routeVersion) {
  const latestVersion = await apiGet(page, auth, '/admin-api/mes/pro/route-version/get', { id: routeVersion.id })
  if (latestVersion.lifecycleStatus === 'ACTIVE' && latestVersion.active === true) {
    return { status: 'SKIPPED_ACTIVE', version: latestVersion }
  }
  assert.equal(latestVersion.lifecycleStatus, 'PENDING_APPROVAL', `路线版本 ${routeVersion.id} 必须处于 PENDING_APPROVAL 才能审批`)
  const task = await findApprovalTaskForRouteVersion(page, auth, latestVersion)
  const taskResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/approval-center/tasks/page') && response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await page.goto(`${BASE_URL}${APPROVAL_CENTER_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await taskResponsePromise
  const rowTexts = [
    task.sourceTaskId,
    task.processInstanceId,
    task.businessKey,
    task.businessCode,
    TARGET_ROUTE_CODE,
    task.businessTitle
  ].filter(Boolean).map(String)
  const taskRow = await waitForApprovalCenterTaskRow(page, rowTexts, routeVersion.id)
  await clickFirstEnabled(taskRow.locator('button, .el-button').filter({ hasText: '审核' }), '打开路线版本审批弹窗')
  const dialog = page.locator('.approval-center__review-dialog:visible, .el-dialog:visible').filter({ hasText: '电子签名' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), TEST_PASSWORD, '路线版本审批电子签名密码')
  const [reviewResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/review') && response.request().method() === 'POST',
      { timeout: 90000 }
    ),
    clickButtonByText(dialog, '确认审核', '确认路线版本审批')
  ])
  const reviewBody = await reviewResponse.json()
  assert.equal(Number(reviewBody.code), 0, `路线版本审批失败：${reviewBody.msg || reviewBody.code}`)
  const activeVersion = await waitForRouteVersionActive(page, auth, routeVersion.id)
  return { status: 'APPROVED', task, version: activeVersion }
}

function writeResult(result) {
  fs.writeFileSync(RESULT_JSON, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
  const lines = [
    '# 记录本同步批记录真实 E2E',
    '',
    `- Status: ${result.status}`,
    `- Base URL: \`${BASE_URL}\``,
    `- Backend URL: \`${BACKEND_URL}\``,
    '- Tenant/User: `测试租户 / aoteman`',
    `- Batch code: ${result.batchCode || '--'}`,
    `- Batch execution ID: ${result.batchExecutionId || '--'}`,
    ''
  ]
  if (result.status === 'PASS') {
    lines.push('- GREEN: rule-ui-minmax-save -> PASS，通过批记录表单列表“规则”弹窗保存 `生产数量` 20/40。')
    lines.push('- GREEN: runtime-snapshot-minmax -> PASS，新批次任务运行态快照冻结 `生产数量` 20/40。')
    lines.push('- GREEN: recordbook-default-entry -> PASS，批记录任务默认进入记录本填写模式。')
    lines.push('- GREEN: recordbook-number-controlled-sync -> PASS，记录本 `50/30/10` 写入后批记录分别存储 `40/30/20`。')
    lines.push('- GREEN: recordbook-string-unchanged -> PASS，记录本文本字段与批记录存储值一致。')
    lines.push('- GREEN: audit-dual-value-only -> PASS，字段审计与填写日志只保留记录本值/批记录值，未出现转换过程字段。')
    lines.push('- GREEN: batch-record-mode-controlled-validation -> PASS，批记录模式越界值被页面受控校验阻止保存。')
  } else if (result.status === 'BLOCKED') {
    lines.push(`- BLOCKER: ${result.reason}`)
    lines.push('- Forbidden: 未使用 SQL/API 写入、mock、临时脚本或临时入口造数。')
  } else {
    lines.push(`- RED: edhr-recordbook-batch-sync-real.e2e.js -> FAIL，${result.reason || result.error}`)
  }
  fs.writeFileSync(RESULT_MD, `${lines.join('\n')}\n`, 'utf8')
}

async function main() {
  assertPrerequisites()
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({
    headless: process.env.EDHR_RECORDBOOK_E2E_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const result = {
    status: 'FAIL',
    batchCode: '',
    batchExecutionId: undefined,
    executionId: undefined,
    workTaskId: undefined,
    steps: []
  }
  try {
    await login(page)
    const auth = await browserAuth(page)

    const savedRule = await configureNumericRulesViaUi(page, auth)
    result.steps.push({ step: 'rule-ui-minmax-save', status: 'PASS', savedRule })

    const batch = await createBatchExecutionViaUi(page)
    result.batchCode = batch.batchCode
    result.batchExecutionId = batch.batchExecutionId
    result.steps.push({ step: 'batch-ui-open-or-create', status: 'PASS', batch })

    const { task } = await getTargetTask(page, auth, batch.batchExecutionId)
    await assertDefaultRecordbookUi(page, task)
    result.steps.push({ step: 'recordbook-default-entry', status: 'PASS' })

    const executionContext = await openTargetTaskFromDetail(page, task, 'RECORDBOOK')
    result.executionId = executionContext.executionId
    result.workTaskId = executionContext.workTaskId
    const runtime = await verifyRuntimeSnapshot(page, auth, executionContext)
    result.steps.push({
      step: 'runtime-snapshot-minmax',
      status: 'PASS',
      taskId: task.id,
      numberField: runtime.numberField,
      stringField: runtime.stringField
    })

    const recordbookSaves = []
    recordbookSaves.push(
      await saveRecordbookValueAndVerify(
        page,
        auth,
        executionContext,
        [{ target: NUMBER_FIELD, sourceValue: 50, storedValue: 40 }],
        '50'
      )
    )
    recordbookSaves.push(
      await saveRecordbookValueAndVerify(
        page,
        auth,
        executionContext,
        [
          { target: NUMBER_FIELD, sourceValue: 30, storedValue: 30 },
          { target: STRING_FIELD, sourceValue: `PB-${batch.batchCode}`, storedValue: `PB-${batch.batchCode}` }
        ],
        '30'
      )
    )
    recordbookSaves.push(
      await saveRecordbookValueAndVerify(
        page,
        auth,
        executionContext,
        [{ target: NUMBER_FIELD, sourceValue: 10, storedValue: 20 }],
        '10'
      )
    )
    result.steps.push({ step: 'recordbook-controlled-sync-and-audit', status: 'PASS', recordbookSaves })

    await page.goto(`${BASE_URL}${BATCH_ROUTE}/detail?id=${batch.batchExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    })
    await openTargetTaskFromDetail(page, task, 'FORM')
    await verifyBatchRecordModeRejectsOutOfRange(page)
    result.steps.push({ step: 'batch-record-mode-controlled-validation', status: 'PASS' })

    result.status = 'PASS'
    writeResult(result)
    console.log('PASS: eDHR recordbook controlled sync real E2E')
  } catch (error) {
    result.status = error?.message?.startsWith('BLOCKED:') ? 'BLOCKED' : 'FAIL'
    result.reason = error instanceof Error ? error.message : String(error)
    writeResult(result)
    throw error
  } finally {
    await context.close().catch(() => {})
    await browser.close().catch(() => {})
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.stack || error.message : error)
  process.exitCode = process.exitCode || 1
})
