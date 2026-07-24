const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const BASE_URL = process.env.EDHR_FORM_FILL_LOG_BASE_URL || 'http://localhost:8081'
const TEST_TENANT = process.env.EDHR_FORM_FILL_LOG_TEST_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_FORM_FILL_LOG_TEST_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_FORM_FILL_LOG_TEST_PASSWORD || '111111'
const SIGNATURE_PASSWORD =
  process.env.EDHR_FORM_FILL_LOG_SIGNATURE_PASSWORD || process.env.EDHR_FORM_FILL_LOG_TEST_PASSWORD || '111111'
const ADMIN_TENANT = process.env.EDHR_FORM_FILL_LOG_ADMIN_TENANT || '芋道源码'
const ADMIN_USERNAME = process.env.EDHR_FORM_FILL_LOG_ADMIN_USERNAME || 'admin'
const ADMIN_PASSWORD = process.env.EDHR_FORM_FILL_LOG_ADMIN_PASSWORD || 'admin123'
const WORK_ORDER_QUERY = process.env.EDHR_FORM_FILL_LOG_WORK_ORDER_QUERY || '881MO090863'
const EXISTING_EXECUTION_ID = process.env.EDHR_FORM_FILL_LOG_EXECUTION_ID || ''
const EXISTING_WORK_TASK_ID = process.env.EDHR_FORM_FILL_LOG_WORK_TASK_ID || ''
const EXISTING_BATCH_EXECUTION_ID = process.env.EDHR_FORM_FILL_LOG_BATCH_EXECUTION_ID || ''
const EXISTING_BATCH_TASK_ID = process.env.EDHR_FORM_FILL_LOG_BATCH_TASK_ID || ''
const EXISTING_BATCH_CODE = process.env.EDHR_FORM_FILL_LOG_EXISTING_BATCH_CODE || ''
const EXISTING_WORK_ORDER_CODE = process.env.EDHR_FORM_FILL_LOG_EXISTING_WORK_ORDER_CODE || ''
const EXISTING_FORM_KEYWORD = process.env.EDHR_FORM_FILL_LOG_FORM_KEYWORD || ''
const BATCH_ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const LOG_ROUTE = '/mes/pro/feedback/edhr-form-fill-log'
const WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-form-fill-log')
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function timestamp() {
  const pad = (value) => String(value).padStart(2, '0')
  const now = new Date()
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

const RUN_ID = process.env.EDHR_FORM_FILL_LOG_RUN_ID || timestamp()
const BATCH_CODE = process.env.EDHR_FORM_FILL_LOG_BATCH_CODE || `E2E-FILL-LOG-${RUN_ID}`
const FILL_VALUE = process.env.EDHR_FORM_FILL_LOG_VALUE || `E2E-FILL-LOG-VALUE-${RUN_ID}`

function ensurePrerequisites() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实 E2E 必须固定使用本机前端 http://localhost:8081')
  assert.equal(TEST_TENANT, '测试租户', '写入型 E2E 只能使用测试租户')
  assert.notEqual(ADMIN_TENANT, TEST_TENANT, '管理员只读复验必须与测试租户写入阶段分离')
  const requiredExistingValues = [
    EXISTING_EXECUTION_ID,
    EXISTING_BATCH_EXECUTION_ID,
    EXISTING_BATCH_TASK_ID,
    EXISTING_BATCH_CODE,
    EXISTING_WORK_ORDER_CODE,
    EXISTING_FORM_KEYWORD
  ]
  const requiredExistingValueCount = requiredExistingValues.filter(Boolean).length
  assert.ok(
    requiredExistingValueCount === 0 || requiredExistingValueCount === requiredExistingValues.length,
    '复用本机测试租户既有 eDHR 执行时，必须同时提供 executionId、batchExecutionId、batchTaskId、batchCode、workOrderCode 和 formKeyword；workTaskId 若当前任务未返回可留空，由真实打开填写动作生成。'
  )
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome 不存在: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(RESULT_DIR, { recursive: true })
}

function useExistingExecution() {
  return Boolean(EXISTING_EXECUTION_ID)
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('缺少 Playwright runtime，请先在 yudao-ui-admin-vue3 执行 pnpm install。')
  }
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
  throw new Error(`缺少可点击按钮：${label}`)
}

async function selectReasonCategory(page, reasonArea) {
  const selectRoot = reasonArea.locator('.el-select:visible').first()
  await selectRoot.waitFor({ state: 'visible', timeout: 60000 })
  await selectRoot.evaluate((element) => {
    const wrapper = element.querySelector('.el-select__wrapper')
    if (!(wrapper instanceof HTMLElement)) {
      throw new Error('reason category select wrapper missing')
    }
    wrapper.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true, cancelable: true }))
    wrapper.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
    wrapper.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }))
    wrapper.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    const input = element.querySelector('input[role="combobox"]')
    if (input instanceof HTMLElement) {
      input.focus()
      input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true, cancelable: true }))
    }
  })
  const option = page
    .locator('.el-popper[aria-hidden="false"] .el-select-dropdown__item, .el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: /操作录入|纠正录入/ })
    .first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
}

async function login(page, actor, redirectPath) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 90000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder*="验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(actor.tenant)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: actor.tenant }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), actor.tenant, '租户')
  }

  await fillFirstVisible(
    loginForm.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    actor.username,
    '用户名'
  )
  await fillFirstVisible(loginForm.locator('input[type="password"]'), actor.password, '密码')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 90000 }
  )
  const permissionInfoPromise = page
    .waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/get-permission-info'),
      { timeout: 90000 }
    )
    .catch((error) => error)
  const [loginResponse] = await Promise.all([
    loginResponsePromise,
    clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }), `${actor.username} 登录`)
  ])
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(Number(loginBody.code)), `${actor.username} 登录失败：${loginBody.msg || loginBody.code}`)
  const permissionInfoResponse = await permissionInfoPromise
  if (permissionInfoResponse instanceof Error) {
    throw new Error(`${actor.username} 登录后未加载权限信息：${permissionInfoResponse.message}`)
  }
  const permissionInfo = await permissionInfoResponse.json()
  const permissionData = permissionInfo.data || permissionInfo
  const permissions = JSON.stringify(permissionData)
  assert.match(permissions, /mes:pro-edhr-form-fill-log:query/, `${actor.username} 缺少表单填写日志查询权限`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
  const currentUser = permissionData.user || permissionData.userInfo || {}
  return {
    tenant: actor.tenant,
    username: actor.username,
    userId: Number(currentUser.id || permissionData.userId || permissionData.user?.id || 0),
    displayName: currentUser.nickname || currentUser.name || actor.username
  }
}

async function selectWorkOrderFromOpenDialog(page) {
  const dialog = page.locator('.el-dialog:visible').first()
  const workOrderInput = dialog
    .locator('input[placeholder="输入工单号搜索并选择未冻结工单"], .el-select input[role="combobox"]')
    .first()
  await workOrderInput.waitFor({ state: 'visible', timeout: 60000 })
  await workOrderInput.click()
  await workOrderInput.fill(WORK_ORDER_QUERY)
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: WORK_ORDER_QUERY }).first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
}

async function openOrCreateBatch(page) {
  await page.goto(`${BASE_URL}${BATCH_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 90000 })
  await clickFirstEnabled(page.getByRole('button', { name: '打开/创建' }), '打开/创建')
  await selectWorkOrderFromOpenDialog(page)
  await fillFirstVisible(page.locator('.el-dialog input[placeholder="请输入真实批次号"]'), BATCH_CODE, '批次号')

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/open-or-create') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await clickFirstEnabled(page.locator('.el-dialog:visible').getByRole('button', { name: /确\s*认/ }), '确认打开批次')
  const response = await responsePromise
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `打开/创建批次失败：${body.msg || body.code}`)
  const tasks = body.data?.tasks || []
  const task = tasks.find((item) => item.batchRecordReportId && item.requiredFlag !== false) || tasks[0]
  assert.ok(task?.id, '批次必须返回可填写的真实任务')
  await page.waitForURL((url) => url.pathname === `${BATCH_ROUTE}/detail`, { timeout: 90000 })
  return {
    batchExecutionId: body.data.id,
    batchCode: body.data.batchCode || BATCH_CODE,
    workOrderCode: body.data.workOrderCode || WORK_ORDER_QUERY,
    task,
    formKeyword: task.batchRecordReportName || task.batchRecordReportId
  }
}

async function openFirstTaskForFilling(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await clickFirstEnabled(page.getByRole('button', { name: /打开填写|打开返工/ }), '打开填写')
  const response = await responsePromise
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `打开填写失败：${body.msg || body.code}`)
  assert.ok(Number.isFinite(Number(body.data?.executionId)), '打开填写必须返回真实 executionId')
  await page.waitForURL((url) => url.pathname === '/mes/pro/feedback/edhr-execution/form', { timeout: 90000 })
  await page.locator('.edhr-page-shell__content').first().waitFor({ state: 'visible', timeout: 90000 })
  return body.data
}

async function openExistingExecutionForFilling(page) {
  const targetPath = `${BATCH_ROUTE}/detail?id=${encodeURIComponent(EXISTING_BATCH_EXECUTION_ID)}`
  await page.goto(`${BASE_URL}${targetPath}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.locator('.edhr-batch-detail').first().waitFor({ state: 'visible', timeout: 90000 })
  const existingContext = await page.evaluate(
    async ({ batchExecutionId, executionId, batchTaskId }) => {
      const module = await import('/src/api/mes/pro/edhr/batchExecution.ts')
      const detail = await module.getEdhrBatchExecution(Number(batchExecutionId))
      const tasks = Array.isArray(detail?.tasks) ? detail.tasks : []
      const task =
        tasks.find((item) => String(item.id) === String(batchTaskId)) ||
        tasks.find((item) => String(item.executionId) === String(executionId))
      if (!task?.id) {
        throw new Error('既有批次未返回匹配的表单任务，不能复用执行上下文。')
      }
      return {
        batchExecutionId: detail.id,
        batchCode: detail.batchCode,
        workOrderCode: detail.workOrderCode,
        processName: task.processName,
        processCode: task.processCode,
        batchRecordReportId: task.batchRecordReportId,
        batchRecordReportName: task.batchRecordReportName
      }
    },
    {
      batchExecutionId: EXISTING_BATCH_EXECUTION_ID,
      executionId: EXISTING_EXECUTION_ID,
      batchTaskId: EXISTING_BATCH_TASK_ID
    }
  )

  const processLabel = existingContext.processName || existingContext.processCode
  if (processLabel) {
    const processButton = page
      .locator('.edhr-batch-detail__process-task-group-head')
      .filter({ hasText: processLabel })
      .first()
    if ((await processButton.count()) > 0) {
      await processButton.click()
    }
  }
  const formLabel = existingContext.batchRecordReportName || existingContext.batchRecordReportId || EXISTING_FORM_KEYWORD
  const formItem = page.locator('.edhr-batch-detail__rail-process-form-item').filter({ hasText: formLabel }).first()
  await formItem.waitFor({ state: 'visible', timeout: 90000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/open') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await clickFirstEnabled(
    formItem.locator('button.edhr-batch-detail__rail-process-form-action').filter({ hasText: /打开填写|打开返工/ }),
    '既有批次打开填写'
  )
  const response = await responsePromise
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `既有批次打开填写失败：${body.msg || body.code}`)
  assert.ok(Number.isFinite(Number(body.data?.executionId)), '既有批次打开填写必须返回真实 executionId')
  await page.waitForURL((url) => url.pathname === '/mes/pro/feedback/edhr-execution/form', { timeout: 90000 })
  await page.locator('.edhr-page-shell__content').first().waitFor({ state: 'visible', timeout: 90000 })
  return {
    batchContext: {
      batchExecutionId: Number(existingContext.batchExecutionId || EXISTING_BATCH_EXECUTION_ID),
      batchCode: existingContext.batchCode || EXISTING_BATCH_CODE,
      workOrderCode: existingContext.workOrderCode || EXISTING_WORK_ORDER_CODE,
      task: { id: Number(EXISTING_BATCH_TASK_ID) },
      formKeyword: EXISTING_FORM_KEYWORD
    },
    execution: body.data
  }
}

async function fillEditableControls(page) {
  await page
    .locator('.edhr-fill-workspace, .edhr-readonly-form, .edhr-template-editable-form, .edhr-fill-workspace__form')
    .first()
    .waitFor({ state: 'visible', timeout: 90000 })
  const templateForm = page.locator('.edhr-template-editable-form, .edhr-fill-workspace__form').first()
  if ((await templateForm.count()) > 0) {
    await templateForm.waitFor({ state: 'visible', timeout: 90000 })
    const editableCells = templateForm.locator(
      '.edhr-template-editable-form__cell.is-editable, .edhr-fill-workspace__field'
    )
    const editableCellCount = await editableCells.count()
    for (let index = 0; index < editableCellCount; index += 1) {
      const cell = editableCells.nth(index)
      if (!(await cell.isVisible().catch(() => false))) continue

      const input = cell.locator('input:not([type="hidden"]):not([type="password"]):not([type="checkbox"])').first()
      if ((await input.count()) > 0 && (await input.isVisible().catch(() => false)) && (await input.isEnabled().catch(() => false))) {
        const readonly = await input.evaluate((element) => element.hasAttribute('readonly')).catch(() => true)
        if (readonly) continue
        await input.fill(FILL_VALUE)
        await input.press('Tab').catch(() => undefined)
        return { fieldType: 'template-input' }
      }

      const textarea = cell.locator('textarea').first()
      if ((await textarea.count()) > 0 && (await textarea.isVisible().catch(() => false)) && (await textarea.isEnabled().catch(() => false))) {
        await textarea.fill(FILL_VALUE)
        await textarea.press('Tab').catch(() => undefined)
        return { fieldType: 'template-textarea' }
      }
    }
    const stats = await templateForm
      .locator('.edhr-template-editable-form__cell.is-editable, .edhr-fill-workspace__field')
      .evaluateAll((cells) =>
        cells.map((cell, index) => ({
          index,
          text: cell.textContent?.trim().slice(0, 80) || '',
          inputCount: cell.querySelectorAll('input').length,
          enabledInputCount: Array.from(cell.querySelectorAll('input')).filter(
            (input) => !input.disabled && !input.readOnly && input.type !== 'hidden'
          ).length,
          textareaCount: cell.querySelectorAll('textarea').length
        }))
      )
    throw new Error(`当前模板表单没有可填写输入控件，无法产生真实字段审计：${JSON.stringify(stats.slice(0, 20))}`)
  }

  const shellStats = await page.evaluate(() => ({
    editableRootCount: document.querySelectorAll('.edhr-template-editable-form, .edhr-fill-workspace__form').length,
    readonlyRootCount: document.querySelectorAll('.edhr-readonly-form').length,
    templateSheetCount: document.querySelectorAll('.edhr-template-sheet').length,
    fillWorkspaceCount: document.querySelectorAll('.edhr-fill-workspace').length,
    inputCount: document.querySelectorAll('input').length,
    enabledInputCount: Array.from(document.querySelectorAll('input')).filter(
      (input) => !input.disabled && !input.readOnly && input.type !== 'hidden'
    ).length
  }))
  if (shellStats.readonlyRootCount > 0 || shellStats.fillWorkspaceCount > 0) {
    throw new Error(`当前执行页面没有渲染可编辑表单控件：${JSON.stringify(shellStats)}`)
  }

  const form = page.locator('.edhr-page-shell__form').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })
  const formItems = form.locator('.el-form-item')
  const count = await formItems.count()
  for (let index = 0; index < count; index += 1) {
    const item = formItems.nth(index)
    if (!(await item.isVisible().catch(() => false))) continue
    const disabled = await item
      .evaluate((element) => element.closest('.is-disabled') != null || element.querySelector('.is-disabled') != null)
      .catch(() => true)
    if (disabled) continue

    const textarea = item.locator('textarea').first()
    if ((await textarea.count()) > 0 && (await textarea.isVisible().catch(() => false)) && (await textarea.isEnabled().catch(() => false))) {
      await textarea.fill(FILL_VALUE)
      await textarea.press('Tab').catch(() => undefined)
      return { fieldType: 'textarea' }
    }

    const input = item.locator('input:not([type="hidden"]):not([type="password"]):not([type="checkbox"])').first()
    if ((await input.count()) === 0 || !(await input.isVisible().catch(() => false)) || !(await input.isEnabled().catch(() => false))) continue
    const readonly = await input.evaluate((element) => element.hasAttribute('readonly')).catch(() => true)
    if (readonly) continue
    await input.fill(FILL_VALUE)
    await input.press('Tab').catch(() => undefined)
    return { fieldType: 'input' }
  }
  throw new Error('当前 eDHR 表单没有可填写控件，无法产生真实字段审计。')
}

async function saveFieldAudit(page) {
  const pendingRows = page.locator('.edhr-page-shell__field-audit-table .el-table__row')
  const railSummary = page.locator('.edhr-fill-workspace__change-summary').filter({ hasText: /待保存变更/ }).first()
  if ((await pendingRows.count()) > 0) {
    await pendingRows.first().waitFor({ state: 'visible', timeout: 90000 })
  } else {
    await railSummary.waitFor({ state: 'visible', timeout: 90000 })
  }

  const reasonArea = page.locator('.edhr-fill-workspace__field-audit-reason, .edhr-page-shell__field-audit-reason').first()
  await reasonArea.waitFor({ state: 'visible', timeout: 90000 })
  await selectReasonCategory(page, reasonArea)
  const reasonInput = reasonArea
    .locator('input[placeholder="请输入字段变更原因"], input:not([type="hidden"]):not([role="combobox"])')
    .first()
  if ((await reasonInput.count()) > 0 && (await reasonInput.isVisible().catch(() => false))) {
    await reasonInput.fill(`E2E-FILL-LOG-REASON-${RUN_ID}`)
  }
  await clickFirstEnabled(
    page.locator('.edhr-fill-workspace__rail-actions, .edhr-page-shell__field-audit').getByRole('button', { name: /保存变更|^保存$/ }),
    '保存字段审计'
  )
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })
  await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder="请输入当前账号密码"]'), SIGNATURE_PASSWORD, '字段审计签名密码')
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/field-audit/save-changes') &&
      response.request().method() === 'PUT',
    { timeout: 90000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: /确\s*认\s*保\s*存/ }), '确认保存字段审计')
  const response = await responsePromise
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `字段审计保存失败：${body.msg || body.code}`)
  assert.equal(body.data?.hashVerification?.status, 'VALID', '字段审计保存后 hashVerification 必须为 VALID')
  return body.data
}

async function queryFillLog(page, context, expectedActor) {
  await page.goto(`${BASE_URL}${LOG_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.locator('.edhr-form-fill-log-page').waitFor({ state: 'visible', timeout: 90000 })
  const valueInput = page.locator('.table-quick-filter__value input').first()
  await valueInput.waitFor({ state: 'visible', timeout: 60000 })
  await valueInput.fill(context.formKeyword)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/form-fill-log/page') &&
      response.request().method() === 'GET' &&
      decodeURIComponent(response.url()).includes('formKeyword='),
    { timeout: 90000 }
  )
  await clickFirstEnabled(page.locator('.table-quick-filter').getByRole('button', { name: /查询/ }), '填写日志快速过滤查询')
  const response = await responsePromise
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `填写日志分页失败：${body.msg || body.code}`)
  assert.ok(body.data?.list?.length > 0, '填写日志必须返回真实审计记录')
  const rowData = body.data.list.find((item) => String(item.batchCode) === String(context.batchCode))
  assert.ok(rowData, `填写日志必须包含本次批号 ${context.batchCode}`)
  assert.equal(Number(rowData.actorId), expectedActor.userId, '填写人必须来自真实登录用户 ID')
  assert.ok(String(rowData.actorName || '').trim(), '填写人名称必须来自真实登录用户快照')
  assert.match(String(rowData.cellSummary || ''), new RegExp(FILL_VALUE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), '写入摘要必须包含本次真实写入值')

  const row = page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: context.batchCode }).first()
  await row.waitFor({ state: 'visible', timeout: 90000 })
  await clickFirstEnabled(row.getByRole('button', { name: '明细' }), '填写日志明细')
  const detailResponse = await page.waitForResponse(
    (item) =>
      item.url().includes('/admin-api/mes/pro/batch-record-execution/form-fill-log/detail') &&
      item.request().method() === 'GET',
    { timeout: 90000 }
  )
  const detailBody = await detailResponse.json()
  assert.ok([0, 200].includes(Number(detailBody.code)), `填写日志明细失败：${detailBody.msg || detailBody.code}`)
  assert.ok(
    (detailBody.data?.items || []).some((item) => String(item.newValueDisplay).includes(FILL_VALUE)),
    '填写日志明细必须展示本次写入的新值'
  )
  await page.locator('.el-drawer:visible').getByRole('button', { name: /close|关闭/i }).click().catch(() => page.keyboard.press('Escape'))

  await clickFirstEnabled(row.getByRole('button', { name: context.batchCode }), '批号跳转')
  await page.waitForURL((url) => url.pathname === `${BATCH_ROUTE}/detail` && url.searchParams.get('id') === String(context.batchExecutionId), {
    timeout: 90000
  })
  await page.goto(`${BASE_URL}${LOG_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.locator('.edhr-form-fill-log-page').waitFor({ state: 'visible', timeout: 90000 })
  await valueInput.fill(context.formKeyword).catch(async () => {
    await page.locator('.table-quick-filter__value input').first().fill(context.formKeyword)
  })
  await clickFirstEnabled(page.locator('.table-quick-filter').getByRole('button', { name: /查询/ }), '重新查询填写日志')
  const refreshedRow = page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: context.batchCode }).first()
  await refreshedRow.waitFor({ state: 'visible', timeout: 90000 })
  await clickFirstEnabled(refreshedRow.getByRole('button', { name: context.workOrderCode }), '生产工单号跳转')
  await page.waitForURL(
    (url) =>
      url.pathname === `${BATCH_ROUTE}/detail` &&
      url.searchParams.get('id') === String(context.batchExecutionId) &&
      url.searchParams.get('focus') === 'work-order',
    { timeout: 90000 }
  )
  return rowData
}

async function adminReadonlyCheck(browser) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const writes = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && WRITE_METHODS.has(request.method())) {
      writes.push({ method: request.method(), url: request.url() })
    }
  })
  try {
    await login(page, { tenant: ADMIN_TENANT, username: ADMIN_USERNAME, password: ADMIN_PASSWORD }, LOG_ROUTE)
    await page.goto(`${BASE_URL}${LOG_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
    await page.locator('.edhr-form-fill-log-page').waitFor({ state: 'visible', timeout: 90000 })
    await clickFirstEnabled(page.locator('.table-quick-filter').getByRole('button', { name: /查询/ }), '管理员只读查询')
    await page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/batch-record-execution/form-fill-log/page') &&
        response.request().method() === 'GET',
      { timeout: 90000 }
    )
    assert.deepEqual(writes, [], `芋道源码/admin 只读复验不得产生 MES/eDHR 写请求：${JSON.stringify(writes)}`)
  } finally {
    await context.close()
  }
}

async function run() {
  ensurePrerequisites()
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({
    headless: process.env.EDHR_FORM_FILL_LOG_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    const testActor = await login(page, { tenant: TEST_TENANT, username: TEST_USERNAME, password: TEST_PASSWORD }, BATCH_ROUTE)
    assert.ok(Number.isFinite(testActor.userId) && testActor.userId > 0, '测试租户登录必须返回真实用户 ID')
    const prepared = useExistingExecution()
      ? await openExistingExecutionForFilling(page)
      : {
          batchContext: await openOrCreateBatch(page),
          execution: undefined
        }
    const batchContext = prepared.batchContext
    const execution = prepared.execution || (await openFirstTaskForFilling(page))
    await fillEditableControls(page)
    const audit = await saveFieldAudit(page)
    const logRow = await queryFillLog(page, batchContext, testActor)
    await context.close()
    await adminReadonlyCheck(browser)

    const result = {
      runId: RUN_ID,
      tenant: TEST_TENANT,
      username: TEST_USERNAME,
      userId: testActor.userId,
      batchExecutionId: batchContext.batchExecutionId,
      batchCode: batchContext.batchCode,
      workOrderCode: batchContext.workOrderCode,
      executionId: execution.executionId,
      auditBatchId: audit.auditBatchId,
      formKeyword: batchContext.formKeyword,
      fillValue: FILL_VALUE,
      logRowAuditBatchId: logRow.auditBatchId
    }
    fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(`PASS: eDHR form fill log real flow batch=${batchContext.batchCode} auditBatch=${audit.auditBatchId}`)
  } catch (error) {
    const failure = {
      url: page.url(),
      message: error instanceof Error ? error.message : String(error),
      bodyText: await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
    }
    fs.writeFileSync(path.join(RESULT_DIR, 'failure.json'), `${JSON.stringify(failure, null, 2)}\n`, 'utf8')
    await page.screenshot({ path: path.join(RESULT_DIR, 'failure.png'), fullPage: true }).catch(() => undefined)
    throw error
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
