const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-final-archive-work-task', 'complete-batch-59')
const REQUIRED_BASE_URL = 'http://localhost:8081'
const DEFAULT_TENANT = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const WORK_TASK_ROUTE = '/mes/pro/feedback/edhr-work-task'
const BATCH_DETAIL_ROUTE = '/mes/pro/feedback/edhr-batch-execution/detail'
const EXECUTION_DETAIL_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const APPROVAL_ROUTE = '/mes/pro/feedback/edhr-approval'
const DOMAIN_TRACE_DETAIL_ROUTE = '/mes/pro/feedback/edhr-domain-trace/detail'

const ENDPOINTS = {
  batchGet: '/mes/pro/edhr-batch-execution/get',
  batchTaskOpen: '/mes/pro/edhr-batch-execution/task/open',
  batchSync: '/mes/pro/edhr-batch-execution/sync-status',
  batchClose: '/mes/pro/edhr-batch-execution/close',
  workTaskMyPage: '/mes/pro/edhr-work-task/my-page',
  executionDetail: '/mes/pro/batch-record-execution/get',
  fieldAuditSave: '/mes/pro/batch-record-execution/field-audit/save-changes',
  formReviewSign: '/mes/pro/batch-record-execution/cosign',
  executionSubmit: '/mes/pro/batch-record-execution/submit',
  approvalPending: '/mes/pro/batch-record-execution/approval-pending-page',
  approvalApprove: '/mes/pro/batch-record-execution/approve',
  domainTraceVerify: '/mes/pro/batch-record-execution/domain-trace/verify'
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_TENANT') || DEFAULT_TENANT,
    username: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_USERNAME') || DEFAULT_USERNAME,
    password: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_PASSWORD'),
    signaturePassword:
      envValue('EDHR_COMPLETE_ARCHIVE_BATCH_SIGNATURE_PASSWORD') || envValue('EDHR_COMPLETE_ARCHIVE_BATCH_PASSWORD'),
    approver: {
      tenant: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_TENANT') || DEFAULT_TENANT,
      username: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_USERNAME') || 'edhrmatrixapprover',
      password: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_PASSWORD'),
      signaturePassword:
        envValue('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_SIGNATURE_PASSWORD') ||
        envValue('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_PASSWORD')
    },
    archiver: {
      tenant: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_ARCHIVER_TENANT') || DEFAULT_TENANT,
      username: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_ARCHIVER_USERNAME') || 'edhrmatrixarchiver',
      password: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_ARCHIVER_PASSWORD')
    },
    batchId: Number(envValue('EDHR_COMPLETE_ARCHIVE_BATCH_ID') || 59),
    batchCode: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_CODE') || 'CODex-WTASK-MERGE-E2E-1781185736602',
    workOrderCode: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_WORK_ORDER_CODE') || 'CODexERP20260610E',
    routeCode: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_ROUTE_CODE') || 'ROUTE-YXN.069.001.1001',
    requiredTaskCount: Number(envValue('EDHR_COMPLETE_ARCHIVE_BATCH_REQUIRED_COUNT') || 15),
    taskTotal: Number(envValue('EDHR_COMPLETE_ARCHIVE_BATCH_TASK_TOTAL') || 21),
    fillPrefix:
      envValue('EDHR_COMPLETE_ARCHIVE_BATCH_FILL_PREFIX') ||
      `P4-ARCHIVE-TODO-${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`,
    headed: envValue('EDHR_COMPLETE_ARCHIVE_BATCH_HEADED') === '1'
  }
  const missing = []
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    missing.push(`EDHR_COMPLETE_ARCHIVE_BATCH_BASE_URL must be ${REQUIRED_BASE_URL}`)
  }
  if (config.tenant !== DEFAULT_TENANT) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_TENANT must be 测试租户')
  }
  if (config.username !== DEFAULT_USERNAME) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_USERNAME must be aoteman')
  }
  if (!config.password) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_PASSWORD is required')
  }
  if (!config.signaturePassword) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_SIGNATURE_PASSWORD is required')
  }
  if (config.approver.tenant !== DEFAULT_TENANT) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_TENANT must be 测试租户')
  }
  if (!config.approver.username) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_USERNAME is required')
  }
  if (!config.approver.password) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_PASSWORD is required')
  }
  if (!config.approver.signaturePassword) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_APPROVER_SIGNATURE_PASSWORD is required')
  }
  if (config.archiver.tenant !== DEFAULT_TENANT) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_ARCHIVER_TENANT must be 测试租户')
  }
  if (!config.archiver.username) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_ARCHIVER_USERNAME is required')
  }
  if (!config.archiver.password) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_ARCHIVER_PASSWORD is required')
  }
  if (!Number.isFinite(config.batchId) || config.batchId <= 0) {
    missing.push('EDHR_COMPLETE_ARCHIVE_BATCH_ID must be a positive number')
  }
  return { ...config, missing }
}

function redactConfig(config) {
  return {
    ...config,
    password: '<redacted>',
    signaturePassword: '<redacted>',
    approver: {
      ...config.approver,
      password: '<redacted>',
      signaturePassword: '<redacted>'
    },
    archiver: {
      ...config.archiver,
      password: '<redacted>'
    }
  }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Missing Playwright runtime. Run pnpm install in yudao-ui-admin-vue3 first.')
  }
}

function unwrapCommonResult(json, label) {
  if (json && typeof json === 'object' && Object.prototype.hasOwnProperty.call(json, 'code')) {
    assert.equal(json.code, 0, `${label} business response failed: ${json.msg || json.message || json.code}`)
    return json.data
  }
  return json
}

async function parseApiResponse(response, label) {
  assert.ok(response.ok(), `${label} HTTP ${response.status()} ${response.url()}`)
  return unwrapCommonResult(await response.json(), label)
}

function responseMatches(response, endpoint, method) {
  return (
    response.url().includes(endpoint) &&
    (!method || response.request().method().toUpperCase() === method.toUpperCase())
  )
}

async function waitForApiResponse(page, endpoint, label, method, predicate) {
  const response = await page.waitForResponse(
    async (candidate) => {
      if (!responseMatches(candidate, endpoint, method)) return false
      if (!predicate) return true
      try {
        return await predicate(candidate)
      } catch {
        return false
      }
    },
    { timeout: 90000 }
  )
  return await parseApiResponse(response, label)
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
  throw new Error(`Cannot find visible input: ${label}`)
}

async function clickVisibleButton(root, name, label) {
  const deadline = Date.now() + 30000
  let sawVisibleDisabled = false
  while (Date.now() < deadline) {
    const candidates = [root.locator('button').filter({ hasText: name }), root.getByRole('button', { name })]
    for (const buttons of candidates) {
      const count = await buttons.count()
      for (let index = 0; index < count; index += 1) {
        const button = buttons.nth(index)
        if (!(await button.isVisible().catch(() => false))) continue
        await button.scrollIntoViewIfNeeded()
        if (await button.isDisabled().catch(() => true)) {
          sawVisibleDisabled = true
          continue
        }
        await button.click()
        return
      }
    }
    await pageDelay(250)
  }
  if (sawVisibleDisabled) {
    throw new Error(`${label || name} button is disabled`)
  }
  throw new Error(`Cannot find visible button: ${label || name}`)
}

function pageDelay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitForText(page, text, label) {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 90000 }).catch((error) => {
    throw new Error(`${label}: ${error.message}`)
  })
}

async function gotoPath(page, config, route) {
  await page.goto(`${config.baseUrl}${route}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
}

async function screenshot(page, name) {
  ensureDir(RESULT_DIR)
  const filePath = path.join(RESULT_DIR, `${name}.png`)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function login(page, config, account = config, redirectPath = `${BATCH_DETAIL_ROUTE}?id=${config.batchId}`) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 90000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('Captcha is enabled; unattended real E2E cannot continue.')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(account.tenant || config.tenant)
    await page.keyboard.press('Enter')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), account.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), account.password, 'password')
  await clickVisibleButton(loginForm, /^登录$/, 'login')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 90000 })
}

async function loadBatchDetailByUi(page, config, label = 'batch detail') {
  const detailPromise = waitForApiResponse(
    page,
    ENDPOINTS.batchGet,
    label,
    'GET',
    (response) => response.url().includes(`id=${config.batchId}`)
  )
  await gotoPath(page, config, `${BATCH_DETAIL_ROUTE}?id=${config.batchId}`)
  const detail = await detailPromise
  await waitForText(page, config.batchCode, `${label} did not display batch code`)
  assert.equal(detail.batchCode, config.batchCode, `${label} batchCode mismatch`)
  assert.equal(detail.workOrderCode, config.workOrderCode, `${label} workOrderCode mismatch`)
  assert.equal(detail.routeCode, config.routeCode, `${label} routeCode mismatch`)
  assert.equal(detail.taskTotal, config.taskTotal, `${label} taskTotal mismatch`)
  assert.equal(detail.blockedCount, 0, `${label} blockedCount mismatch`)
  return detail
}

function getRequiredTasks(detail) {
  return (detail.tasks || [])
    .filter((task) => task.requiredFlag !== false && task.batchRecordReportId)
    .sort((left, right) => (left.routeProcessSort || 0) - (right.routeProcessSort || 0))
}

async function syncBatchByUi(page, config) {
  const syncPromise = waitForApiResponse(
    page,
    ENDPOINTS.batchSync,
    'sync batch status',
    'POST',
    (response) => response.url().includes(`id=${config.batchId}`)
  )
  await clickVisibleButton(page, '同步状态', 'sync status')
  return await syncPromise
}

async function openTaskByUi(page, task) {
  const processToken = task.processCode || task.processName || task.batchRecordReportName
  const row = page.locator('.el-table__row').filter({ hasText: processToken }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.scrollIntoViewIfNeeded()
  const openResponsePromise = waitForApiResponse(page, ENDPOINTS.batchTaskOpen, `open task ${processToken}`, 'POST')
  await clickVisibleButton(row, '打开填写', `open task ${processToken}`)
  const opened = await openResponsePromise
  assert.ok(opened?.executionId, `Task ${processToken} did not return executionId`)
  await page.waitForURL((url) => url.pathname === EXECUTION_DETAIL_ROUTE, { timeout: 90000 })
  return opened
}

async function selectWorkTaskType(page, toolbar, label) {
  await toolbar.locator('.el-select').first().click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: label }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function fillToolbarInput(toolbar, label, value) {
  const formItem = toolbar.locator('.el-form-item').filter({ hasText: label }).first()
  await fillFirstVisible(formItem.locator('input'), value, label)
}

async function queryFillTaskOnBoard(page, config, task, tabLabel) {
  await gotoPath(page, config, WORK_TASK_ROUTE)
  const board = page.locator('.edhr-work-task-page').first()
  await board.waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('tab', { name: tabLabel }).click()
  const toolbar = page.locator('.edhr-work-task-page__toolbar').first()
  await toolbar.waitFor({ state: 'visible', timeout: 30000 })
  await selectWorkTaskType(page, toolbar, '填写')
  await fillToolbarInput(toolbar, '批次', config.batchCode)
  await fillToolbarInput(toolbar, '工序', task.processName || task.processCode || '')
  const listPromise = waitForApiResponse(page, ENDPOINTS.workTaskMyPage, `work task board ${tabLabel}`, 'GET')
  await clickVisibleButton(toolbar, '查询', `query fill task ${tabLabel}`)
  const pageData = await listPromise
  const rows = pageData.list || []
  const target = rows.find(
    (row) =>
      row.taskType === 'FILL' &&
      row.batchCode === config.batchCode &&
      row.processName === task.processName &&
      (row.status === 'TODO' || row.status === 'OVERDUE')
  )
  if (!target) return null
  const targetRow = page.locator('.el-table__body-wrapper tr').filter({ hasText: config.batchCode }).filter({ hasText: task.processName }).first()
  await targetRow.waitFor({ state: 'visible', timeout: 30000 })
  await clickVisibleButton(targetRow, '处理', `process fill task ${target.id}`)
  await page.waitForURL(
    (url) =>
      (url.pathname === BATCH_DETAIL_ROUTE || url.pathname === EXECUTION_DETAIL_ROUTE) &&
      url.searchParams.get('workTaskId') === String(target.id),
    { timeout: 90000 }
  )
  return target
}

async function openCurrentFillWorkTaskFromBoard(page, config, task) {
  let target = await queryFillTaskOnBoard(page, config, task, '我的待办')
  if (!target) {
    target = await queryFillTaskOnBoard(page, config, task, '逾期任务')
  }
  if (!target) {
    throw new Error(`Cannot find FILL TODO/OVERDUE work task for ${task.processName}`)
  }
  const url = new URL(page.url())
  if (url.pathname === BATCH_DETAIL_ROUTE) {
    await waitForText(page, config.batchCode, `batch detail for work task ${target.id}`)
    return await openTaskByUi(page, task)
  }
  const executionId = Number(url.searchParams.get('id') || url.searchParams.get('executionId'))
  assert.ok(Number.isFinite(executionId) && executionId > 0, `Execution URL for work task ${target.id} has no id`)
  return {
    taskId: task.id,
    executionId,
    status: task.status,
    workTaskId: target.id
  }
}

async function readExecutionCodeFromVisibleDetail(page, executionId) {
  const summary = page.locator('.edhr-page-shell__summary').first()
  await summary.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForFunction(
    (id) => {
      const text = document.querySelector('.edhr-page-shell__summary')?.innerText || ''
      if (!text.includes('执行编码')) return false
      const normalized = text.replace(/\s+/g, ' ').trim()
      if (normalized.includes('执行编码 --')) return false
      return normalized.includes(String(id)) || /执行编码\s+[\w.-]+/.test(normalized)
    },
    executionId,
    { timeout: 30000 }
  )
  const summaryText = (await summary.innerText()).replace(/\s+/g, ' ').trim()
  const match = summaryText.match(/执行编码\s+([A-Za-z0-9._-]+)/)
  assert.ok(match?.[1] && match[1] !== '--', `Execution detail did not show a valid code: ${summaryText}`)
  return match[1]
}

async function fillEditableControls(page, taskIndex) {
  const form = page.locator('.edhr-page-shell__form').first()
  if ((await form.count()) === 0 || !(await form.isVisible())) {
    return { filled: 0, selected: 0 }
  }
  let filled = 0
  let selected = 0
  const formItems = form.locator('.el-form-item')
  const count = await formItems.count()
  for (let index = 0; index < count; index += 1) {
    const item = formItems.nth(index)
    if (!(await item.isVisible().catch(() => false))) continue
    const itemDisabled = await item
      .evaluate((element) => element.closest('.is-disabled') != null || element.querySelector('.is-disabled') != null)
      .catch(() => true)
    if (itemDisabled) continue

    const select = item.locator('.el-select input[role="combobox"]').first()
    if ((await select.count()) > 0 && (await select.isVisible().catch(() => false)) && (await select.isEnabled().catch(() => false))) {
      await select.click()
      await page.keyboard.press('ArrowDown')
      await page.keyboard.press('Enter')
      selected += 1
      continue
    }

    const checkbox = item.locator('.el-checkbox:not(.is-disabled)').first()
    if ((await checkbox.count()) > 0 && (await checkbox.isVisible().catch(() => false))) {
      await checkbox.click()
      selected += 1
      continue
    }

    const numberInput = item.locator('.el-input-number input').first()
    if ((await numberInput.count()) > 0 && (await numberInput.isVisible().catch(() => false)) && (await numberInput.isEnabled().catch(() => false))) {
      await numberInput.fill(String(10 + taskIndex))
      await numberInput.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const dateInput = item.locator('.el-date-editor input').first()
    if ((await dateInput.count()) > 0 && (await dateInput.isVisible().catch(() => false)) && (await dateInput.isEnabled().catch(() => false))) {
      const isDateTime = await dateInput
        .evaluate((element) => element.closest('.el-date-editor')?.className.includes('datetime') === true)
        .catch(() => false)
      await dateInput.fill(isDateTime ? '2026-06-10 10:20:30' : '2026-06-10')
      await dateInput.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const textarea = item.locator('textarea').first()
    if ((await textarea.count()) > 0 && (await textarea.isVisible().catch(() => false)) && (await textarea.isEnabled().catch(() => false))) {
      await textarea.fill(`P4-T${taskIndex}-${filled + 1}`)
      await textarea.press('Tab').catch(() => undefined)
      filled += 1
      continue
    }

    const input = item.locator('input:not([type="hidden"]):not([type="password"]):not([type="checkbox"])').first()
    if ((await input.count()) === 0 || !(await input.isVisible().catch(() => false)) || !(await input.isEnabled().catch(() => false))) continue
    const readonly = await input.evaluate((element) => element.hasAttribute('readonly')).catch(() => true)
    if (readonly) continue
    await input.fill(`P4-T${taskIndex}-${filled + 1}`)
    await input.press('Tab').catch(() => undefined)
    filled += 1
  }
  return { filled, selected }
}

async function isActuallyDisabled(locator) {
  if ((await locator.count()) === 0) return true
  return await locator
    .evaluate((element) => (
      element.disabled === true ||
      element.classList.contains('is-disabled') ||
      element.getAttribute('aria-disabled') === 'true' ||
      element.closest('.is-disabled') != null
    ))
    .catch(() => true)
}

async function selectFieldAuditReasonCategory(page, reasonArea) {
  const wrapper = reasonArea.locator('.el-select__wrapper').first()
  const input = reasonArea.locator('.el-select input[role="combobox"]').first()
  for (let attempt = 0; attempt < 3; attempt += 1) {
    if ((await input.count()) > 0) {
      await input.scrollIntoViewIfNeeded()
      await input.click({ force: true })
    } else {
      await wrapper.scrollIntoViewIfNeeded()
      await wrapper.click({ force: true })
    }
    const correctionOption = page.locator('.el-select-dropdown__item').filter({ hasText: '纠正录入' }).last()
    if (await correctionOption.isVisible().catch(() => false)) {
      await correctionOption.click()
    } else {
      await page.keyboard.press('ArrowDown')
      await page.keyboard.press('Enter')
    }
    await page.waitForTimeout(300)
    const reasonText = await reasonArea.innerText().catch(() => '')
    if (reasonText.includes('CORRECTION') || reasonText.includes('纠正录入')) return
  }
  throw new Error('Field audit reason category was not selected')
}

async function saveFieldAuditIfNeeded(page, config, taskIndex) {
  const pendingRows = page.locator('.edhr-page-shell__field-audit-table .el-table__row')
  const pendingCount = await pendingRows.count()
  const legacySaveButton = page.locator('.edhr-page-shell__field-audit').getByRole('button', { name: /保存变更/ }).first()
  if (pendingCount === 0 || (await legacySaveButton.count()) === 0) {
    return { saved: false, pendingCount }
  }

  const reasonArea = page.locator('.edhr-page-shell__field-audit-reason').first()
  await selectFieldAuditReasonCategory(page, reasonArea)
  await fillFirstVisible(
    reasonArea.locator('.el-form-item').last().locator('input:not([type="hidden"])'),
    `${config.fillPrefix}-FIELD_CHANGE_REASON-${taskIndex}`,
    'field audit reason'
  )
  await page.keyboard.press('Tab')
  await page.waitForTimeout(300)

  const sectionButtons = page.locator('.edhr-page-shell__field-audit .edhr-page-shell__section-actions .el-button')
  const namedSaveButton = sectionButtons.filter({ hasText: /保存变更/ }).first()
  const saveButton = (await namedSaveButton.count()) > 0 ? namedSaveButton : sectionButtons.first()
  await saveButton.waitFor({ state: 'visible', timeout: 30000 })
  await saveButton.scrollIntoViewIfNeeded()
  if (await isActuallyDisabled(saveButton)) {
    throw new Error('Field audit save button is disabled')
  }
  await saveButton.click()
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, 'field audit signature password')
  const saveResponsePromise = waitForApiResponse(page, ENDPOINTS.fieldAuditSave, 'field audit save', 'PUT')
  await dialog.locator('.el-dialog__footer .el-button').last().click({ force: true })
  const result = await saveResponsePromise
  assert.equal(result.hashVerification?.status, 'VALID', 'Field audit hash verification must be VALID')
  await waitForText(page, '字段变更已写入', 'field audit success message missing')
  return { saved: true, pendingCount, result }
}

async function formReviewSign(page, config, taskIndex) {
  await clickVisibleButton(page, '复核签名', `form review signature T${taskIndex}`)
  const dialog = page.locator('.el-dialog').filter({ hasText: '表单复核签名' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, 'review signature password')
  await fillFirstVisible(dialog.locator('textarea'), `${config.fillPrefix}-FORM_REVIEW-${taskIndex}`, 'review comment')
  const signResponsePromise = waitForApiResponse(page, ENDPOINTS.formReviewSign, `form review signature T${taskIndex}`, 'PUT')
  await clickVisibleButton(dialog, /确 认 签 名/, 'confirm form review signature')
  return await signResponsePromise
}

async function verifyDomainTrace(page, config, executionId) {
  await clickVisibleButton(page, '主数据追溯', 'domain trace')
  await page.waitForURL((url) => url.pathname === DOMAIN_TRACE_DETAIL_ROUTE, { timeout: 90000 })
  await waitForText(page, '主数据追溯', 'domain trace detail missing')
  const verifyResponsePromise = waitForApiResponse(
    page,
    ENDPOINTS.domainTraceVerify,
    `domain trace verify executionId=${executionId}`,
    'POST'
  )
  await clickVisibleButton(page, /校验|验证|Verify/i, 'domain trace verify')
  const result = await verifyResponsePromise
  if (result?.status) {
    assert.notEqual(result.status, 'BLOCKED', `Domain trace blocked: ${JSON.stringify(result.blockers || [])}`)
  }
  await gotoPath(page, config, `${EXECUTION_DETAIL_ROUTE}?id=${executionId}`)
  await waitForText(page, 'eDHR 执行详情', 'execution detail missing after domain trace')
  return result
}

async function readExecutionDetailByLoggedInApi(page, executionId) {
  return await page.evaluate(
    async ({ executionId }) => {
      function readCacheValue(key) {
        const raw = window.localStorage.getItem(key) || window.sessionStorage.getItem(key)
        if (!raw) return ''
        try {
          const parsed = JSON.parse(raw)
          if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
            try {
              return JSON.parse(parsed.v)
            } catch {
              return parsed.v
            }
          }
          return parsed
        } catch {
          return raw
        }
      }
      const accessToken = readCacheValue('ACCESS_TOKEN')
      const tenantId = readCacheValue('tenantId')
      const headers = { Accept: 'application/json' }
      if (accessToken) headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
      if (tenantId) headers['tenant-id'] = String(tenantId)
      const response = await fetch(`/admin-api/mes/pro/batch-record-execution/get?id=${executionId}`, {
        credentials: 'include',
        headers
      })
      if (!response.ok) throw new Error(`execution get HTTP ${response.status}`)
      const json = await response.json()
      if (json.code !== 0) throw new Error(json.msg || json.message || `execution get business ${json.code}`)
      return json.data
    },
    { executionId }
  )
}

async function readDomainTraceDetailByLoggedInApi(page, executionId) {
  return await page.evaluate(
    async ({ executionId }) => {
      function readCacheValue(key) {
        const raw = window.localStorage.getItem(key) || window.sessionStorage.getItem(key)
        if (!raw) return ''
        try {
          const parsed = JSON.parse(raw)
          if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
            try {
              return JSON.parse(parsed.v)
            } catch {
              return parsed.v
            }
          }
          return parsed
        } catch {
          return raw
        }
      }
      const accessToken = readCacheValue('ACCESS_TOKEN')
      const tenantId = readCacheValue('tenantId')
      const headers = { Accept: 'application/json' }
      if (accessToken) headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
      if (tenantId) headers['tenant-id'] = String(tenantId)
      const response = await fetch(`/admin-api/mes/pro/batch-record-execution/domain-trace/detail?executionId=${executionId}`, {
        credentials: 'include',
        headers
      })
      if (!response.ok) throw new Error(`domain trace detail HTTP ${response.status}`)
      const json = await response.json()
      if (json.code !== 0) throw new Error(json.msg || json.message || `domain trace detail business ${json.code}`)
      return json.data
    },
    { executionId }
  )
}

async function submitExecution(page, config, taskIndex) {
  await clickVisibleButton(page, '提交执行', `submit execution T${taskIndex}`)
  const dialog = page.locator('.el-dialog').filter({ hasText: '提交 eDHR 执行' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, 'submit password')
  await fillFirstVisible(dialog.locator('textarea'), `${config.fillPrefix}-SUBMIT-${taskIndex}`, 'submit comment')
  const submitResponsePromise = waitForApiResponse(page, ENDPOINTS.executionSubmit, `submit execution T${taskIndex}`, 'PUT')
  await clickVisibleButton(dialog, /确 认 提 交/, 'confirm submit execution')
  await submitResponsePromise
}

async function approveExecution(page, config, executionCode, taskIndex) {
  await gotoPath(page, config, `${APPROVAL_ROUTE}?tab=pending`)
  await waitForText(page, '待我审批', 'approval page missing')
  const toolbar = page.locator('.edhr-workbench__toolbar').first()
  await fillFirstVisible(toolbar.locator('input').first(), executionCode, 'approval execution code')
  const pendingResponsePromise = waitForApiResponse(page, ENDPOINTS.approvalPending, `approval pending ${executionCode}`, 'GET')
  await clickVisibleButton(toolbar, '查询', 'approval query')
  const pageData = await pendingResponsePromise
  const rows = pageData.list || []
  assert.ok(rows.some((row) => row.executionCode === executionCode), `Approval pending list did not contain ${executionCode}`)
  const row = page.locator('.el-table__row').filter({ hasText: executionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await clickVisibleButton(row, '通过', `approve ${executionCode}`)
  const dialog = page.locator('.el-dialog').filter({ hasText: '通过 eDHR 审批' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, 'approval password')
  await fillFirstVisible(dialog.locator('textarea').last(), `${config.fillPrefix}-APPROVE-${taskIndex}`, 'approval comment')
  const approveResponsePromise = waitForApiResponse(page, ENDPOINTS.approvalApprove, `approve ${executionCode}`, 'PUT')
  await clickVisibleButton(dialog, /^确 认$/, 'confirm approval')
  const result = await approveResponsePromise
  assert.equal(result.status, 3, `Execution ${executionCode} was not approved/closed`)
  return result
}

async function processTask(page, approverPage, config, task, taskIndex) {
  const opened = await openCurrentFillWorkTaskFromBoard(page, config, task)
  const executionId = Number(opened.executionId)
  let detailData = await waitForApiResponse(
    page,
    ENDPOINTS.executionDetail,
    `execution detail ${executionId}`,
    'GET',
    (response) => response.url().includes(`id=${executionId}`)
  ).catch(() => undefined)
  if (!detailData) {
    detailData = await readExecutionDetailByLoggedInApi(page, executionId).catch(() => undefined)
  }
  const executionCode = detailData?.executionCode || opened.executionCode || await readExecutionCodeFromVisibleDetail(page, executionId)
  await waitForText(page, executionCode, `execution detail did not display ${executionCode}`)

  const fill = await fillEditableControls(page, taskIndex)
  const fieldAudit = await saveFieldAuditIfNeeded(page, config, taskIndex)
  const reviewSign = await formReviewSign(page, config, taskIndex)
  const latestTraceDetail = await readDomainTraceDetailByLoggedInApi(page, executionId).catch(() => undefined)
  const domainTrace =
    latestTraceDetail?.status === 'VERIFIED'
      ? { status: 'VERIFIED', skipped: true }
      : await verifyDomainTrace(page, config, executionId)
  await submitExecution(page, config, taskIndex)
  const approval = await approveExecution(
    approverPage,
    { ...config, signaturePassword: config.approver.signaturePassword },
    executionCode,
    taskIndex
  )

  return {
    taskId: task.id,
    routeProcessSort: task.routeProcessSort,
    processCode: task.processCode,
    processName: task.processName,
    executionId,
    executionCode,
    filledFields: fill.filled,
    selectedFields: fill.selected,
    fieldAuditSaved: fieldAudit.saved,
    fieldAuditPendingCount: fieldAudit.pendingCount,
    formReviewSignatureId: reviewSign?.signatureId,
    domainTraceStatus: domainTrace?.status,
    approvalSignatureId: approval?.signatureId
  }
}

async function closeBatch(page, config) {
  const detail = await syncBatchByUi(page, config)
  assert.equal(detail.taskApprovedCount, config.requiredTaskCount, 'Approved count before close mismatch')
  assert.equal(detail.taskTotal, config.taskTotal, 'Task total before close mismatch')
  assert.equal(detail.blockedCount, 0, 'Blocked count before close mismatch')
  assert.equal(detail.canClose, true, 'Backend must return canClose=true before close')

  await clickVisibleButton(page, '关闭批次', 'close batch')
  const dialog = page.locator('.el-dialog').filter({ hasText: '关闭 eDHR 批次' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('textarea'), `${config.fillPrefix}-BATCH_CLOSE`, 'close comment')
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.signaturePassword, 'close password')
  const closeResponsePromise = waitForApiResponse(page, ENDPOINTS.batchClose, 'close batch', 'POST')
  await clickVisibleButton(dialog, /^确 认$/, 'confirm close batch')
  const closed = await closeResponsePromise
  assert.ok(closed.closedAt, 'Closed batch response did not return closedAt')
  return closed
}

async function verifyArchiveTodoByLoggedInApi(page, config) {
  return await page.evaluate(
    async ({ batchId }) => {
      function readCacheValue(key) {
        const raw = window.localStorage.getItem(key) || window.sessionStorage.getItem(key)
        if (!raw) return ''
        try {
          const parsed = JSON.parse(raw)
          if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
            try {
              return JSON.parse(parsed.v)
            } catch {
              return parsed.v
            }
          }
          return parsed
        } catch {
          return raw
        }
      }
      const accessToken = readCacheValue('ACCESS_TOKEN')
      const tenantId = readCacheValue('tenantId')
      const headers = { Accept: 'application/json' }
      if (accessToken) headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
      if (tenantId) headers['tenant-id'] = String(tenantId)
      const params = new URLSearchParams({
        pageNo: '1',
        pageSize: '20',
        taskType: 'ARCHIVE',
        status: 'TODO'
      })
      const response = await fetch(`/admin-api/mes/pro/edhr-work-task/my-page?${params}`, {
        credentials: 'include',
        headers
      })
      if (!response.ok) throw new Error(`work task my-page HTTP ${response.status}`)
      const json = await response.json()
      if (json.code !== 0) throw new Error(json.msg || json.message || `work task my-page business ${json.code}`)
      const list = json.data?.list || []
      return list.find((item) => Number(item.businessScopeId) === Number(batchId) || Number(item.batchExecutionId) === Number(batchId)) || null
    },
    { batchId: config.batchId }
  )
}

function writeResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

async function run(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const approverContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const archiverContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const page = await context.newPage()
  const approverPage = await approverContext.newPage()
  const archiverPage = await archiverContext.newPage()
  const processedTasks = []
  try {
    await login(page, config)
    await login(approverPage, config, config.approver, `${APPROVAL_ROUTE}?tab=pending`)
    await login(archiverPage, config, config.archiver, WORK_TASK_ROUTE)
    let detail = await loadBatchDetailByUi(page, config, 'initial batch detail')
    let requiredTasks = getRequiredTasks(detail)
    assert.equal(requiredTasks.length, config.requiredTaskCount, 'Required task count mismatch')
    await screenshot(page, '01-initial-batch')

    let taskIndex = 0
    for (const task of requiredTasks) {
      taskIndex += 1
      if (task.status === 40) continue
      processedTasks.push(await processTask(page, approverPage, config, task, taskIndex))
      writeResult({ status: 'RUNNING', config: redactConfig(config), processedTasks })
    }

    detail = await loadBatchDetailByUi(page, config, 'after task approvals')
    requiredTasks = getRequiredTasks(detail)
    assert.ok(requiredTasks.every((task) => task.status === 40), 'Not all required tasks are approved')
    const closed = await closeBatch(page, config)
    await screenshot(page, '02-batch-closed')
    const archiveTodo = await verifyArchiveTodoByLoggedInApi(archiverPage, config)
    assert.ok(archiveTodo?.id, 'Close did not create a visible ARCHIVE/TODO work task for archiver user')
    const result = {
      status: 'PASS',
      batchId: config.batchId,
      batchCode: config.batchCode,
      fillPrefix: config.fillPrefix,
      processedTasks,
      closed,
      archiveTodo
    }
    writeResult(result)
    return result
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      missing: config.missing,
      config: redactConfig(config)
    }
    writeResult(result)
    throw new Error(`Missing prerequisites:\n${config.missing.map((item) => `- ${item}`).join('\n')}`)
  }
  const result = await run(config)
  console.log(`PASS: batch ${result.batchId} closed and ARCHIVE/TODO task ${result.archiveTodo.id} is visible`)
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
