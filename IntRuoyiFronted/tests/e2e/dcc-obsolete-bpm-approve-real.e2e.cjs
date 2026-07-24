const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_OBSOLETE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_OBSOLETE_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_OBSOLETE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_OBSOLETE_E2E_PASSWORD || '111111'
const TASK_DIR = process.env.DCC_OBSOLETE_E2E_TASK_DIR
  ? path.resolve(process.env.DCC_OBSOLETE_E2E_TASK_DIR)
  : path.resolve(__dirname, '../../../doc/tasks/20260720-form-center-controlled-state-machine-implementation/e2e-artifacts')
const SUBMIT_PATH = path.join(TASK_DIR, 'dcc-obsolete-form-center-real-submit.json')
const RESULT_PATH = path.join(TASK_DIR, 'dcc-obsolete-bpm-approve-real.json')

function writeResult(result) {
  fs.mkdirSync(TASK_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, JSON.stringify(result, null, 2) + '\n', 'utf8')
}

function readSubmitArtifact() {
  const artifact = JSON.parse(fs.readFileSync(SUBMIT_PATH, 'utf8'))
  assert.equal(artifact.status, 'PASS_PENDING_LOCK', 'submit artifact must prove pending lock before approval')
  assert.ok(artifact.controlledFileId, 'submit artifact missing controlledFileId')
  assert.ok(artifact.bpmProcessInstanceId, 'submit artifact missing bpmProcessInstanceId')
  return artifact
}

async function login(page) {
  await page.goto(BASE_URL + '/login?redirect=/index', { waitUntil: 'commit', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  await tenantInput.fill(TENANT)
  const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: 60000 })
  await tenantOption.click()
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const loginButton = form.getByRole('button', { name: '登录' }).first()
  await loginButton.waitFor({ state: 'visible', timeout: 60000 })
  await loginButton.click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, 'login HTTP status ' + loginResponse.status())
  assert.ok([0, 200].includes(loginPayload.code), 'login business code ' + loginPayload.code + ': ' + (loginPayload.msg || ''))
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function fetchApprovalCenterPage(page, params) {
  return await page.evaluate(async (query) => {
    const readCache = (key) => {
      const raw = window.localStorage.getItem(key)
      if (!raw) {
        return undefined
      }
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object') {
          if (Object.prototype.hasOwnProperty.call(parsed, 'v')) {
            try {
              return JSON.parse(parsed.v)
            } catch (error) {
              return parsed.v
            }
          }
          if (Object.prototype.hasOwnProperty.call(parsed, 'value')) {
            return parsed.value
          }
          if (Object.prototype.hasOwnProperty.call(parsed, 'data')) {
            return parsed.data
          }
        }
        return parsed
      } catch (error) {
        return raw
      }
    }
    const search = new URLSearchParams()
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        search.set(key, String(value))
      }
    })
    const headers = {}
    const accessToken = readCache('ACCESS_TOKEN')
    const tenantId = readCache('tenantId')
    const visitTenantId = readCache('visitTenantId')
    if (accessToken) {
      headers.Authorization = 'Bearer ' + accessToken
    }
    if (tenantId) {
      headers['tenant-id'] = String(tenantId)
    }
    if (accessToken && visitTenantId) {
      headers['visit-tenant-id'] = String(visitTenantId)
    }
    const response = await fetch('/admin-api/approval-center/tasks/page?' + search.toString(), {
      credentials: 'include',
      headers
    })
    return await response.json()
  }, params)
}

async function findApprovalCenterTodo(page, processInstanceId) {
  const moduleCodes = ['BPM', 'DCC']
  const checked = []
  for (const moduleCode of moduleCodes) {
    for (let pageNo = 1; pageNo <= 5; pageNo++) {
      const payload = await fetchApprovalCenterPage(page, {
        viewType: 'TODO',
        moduleCode,
        pageNo,
        pageSize: 50
      })
      if (![0, 200].includes(payload.code)) {
        checked.push({
          moduleCode,
          pageNo,
          errorCode: payload.code,
          errorMessage: payload.msg || ''
        })
        break
      }
      const data = payload.data
      const rows = data.list || []
      checked.push({
        moduleCode,
        pageNo,
        total: data.total || 0,
        processInstanceIds: rows.map((row) => row.processInstanceId).filter(Boolean)
      })
      const matched = rows.find((row) => row.processInstanceId === processInstanceId)
      if (matched) {
        return { row: matched, checked }
      }
      if (!data.total || pageNo * 50 >= data.total) {
        break
      }
    }
  }
  return { row: null, checked }
}

function unwrap(payload, label) {
  assert.ok([0, 200].includes(payload.code), label + ' business code ' + payload.code + ': ' + (payload.msg || ''))
  return payload.data
}

async function main() {
  assert.equal(TENANT, process.env.DCC_OBSOLETE_E2E_EXPECT_TENANT || '测试租户')
  assert.equal(USERNAME, process.env.DCC_OBSOLETE_E2E_EXPECT_USERNAME || 'aoteman')
  const submitArtifact = readSubmitArtifact()
  const controlledFileId = submitArtifact.controlledFileId
  const processInstanceId = submitArtifact.bpmProcessInstanceId
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const pageErrors = []
  const writeRequests = []
  const observedResponses = []
  let taskId = null
  let activePage = null
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    activePage = page
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('request', (request) => {
      if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/')) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })
    page.on('response', (response) => {
      const url = response.url()
      if (url.includes('/bpm/') || url.includes('/form-center/') || url.includes('/dcc/controlled-files/')) {
        observedResponses.push({ method: response.request().method(), status: response.status(), url })
      }
    })
    await login(page)

    const approvalCenterResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/approval-center/tasks/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(BASE_URL + '/approval-center/todo?moduleCode=DCC', { waitUntil: 'commit' })
    await approvalCenterResponsePromise
    const { row: todo, checked: checkedApprovalCenterPages } = await findApprovalCenterTodo(page, processInstanceId)
    assert.ok(todo, 'approval center todo row for DCC obsolete process is missing: ' + processInstanceId
      + ', checked=' + JSON.stringify(checkedApprovalCenterPages))
    taskId = todo.sourceTaskId || todo.taskId || todo.id
    assert.ok(taskId, 'approval center todo row missing sourceTaskId/taskId/id: ' + JSON.stringify(todo))

    const approvalCenterBpmResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/approval-center/tasks/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(BASE_URL + '/approval-center/todo?moduleCode=' + encodeURIComponent(todo.moduleCode || 'BPM'), { waitUntil: 'commit' })
    await approvalCenterBpmResponsePromise
    const targetApprovalRow = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: processInstanceId }).first()
    await targetApprovalRow.waitFor({ state: 'visible', timeout: 60000 })
    await targetApprovalRow.getByRole('button', { name: '审核' }).click()
    const approvalCenterDialog = page.locator('.el-dialog:visible').filter({ hasText: '审核确认' }).last()
    await approvalCenterDialog.waitFor({ state: 'visible', timeout: 60000 })
    await approvalCenterDialog.locator('input[type="password"]').first().fill(PASSWORD)
    const approvalTextarea = approvalCenterDialog.locator('textarea').first()
    if (await approvalTextarea.count()) {
      await approvalTextarea.fill('E2E DCC obsolete approval pass')
    }
    const approveResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/approval-center/tasks/review') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await approvalCenterDialog.getByRole('button', { name: '确认审核' }).click()
    const approvePayload = await (await approveResponsePromise).json()
    unwrap(approvePayload, 'approval center review task')

    const detailAfterResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/dcc/controlled-files/' + controlledFileId) && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(BASE_URL + '/dcc/controlled-file/detail/' + controlledFileId, { waitUntil: 'commit' })
    const detailAfterPayload = await (await detailAfterResponsePromise).json()
    const detailAfter = unwrap(detailAfterPayload, 'dcc detail after approve')
    assert.equal(detailAfter.status, 'OBSOLETE', 'DCC controlled file must be OBSOLETE after BPM approval effect')
    const projection = detailAfter.actionProjection || {}
    assert.equal(projection.actionLocked, true, 'DCC terminal obsolete file must remain readonly locked')
    assert.equal(projection.actionLockReason, 'Controlled file version is terminal',
      'DCC obsolete terminal lock reason must not remain approval-pending')
    assert.equal(projection.canWithdraw, false, 'DCC obsolete terminal file must not expose withdraw')
    assert.equal(projection.pendingRequestId ?? null, null, 'DCC obsolete terminal file must not expose pendingRequestId')
    assert.equal(projection.pendingVersionNo ?? null, null, 'DCC obsolete terminal file must not expose pendingVersionNo')
    assert.equal((projection.allowedActions || []).includes('OBSOLETE'), false, 'obsolete terminal file must not expose OBSOLETE action')
    assert.deepEqual(pageErrors, [], 'BPM approval E2E page errors must be empty')

    writeResult({
      status: 'PASS',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      controlledFileId,
      taskId,
      approvalCenterTodo: todo,
      checkedApprovalCenterPages,
      processInstanceId,
      finalStatus: detailAfter.status,
      actionProjection: projection,
      writeRequests,
      observedResponses,
      pageErrors
    })
    console.log('GREEN: dcc-obsolete-bpm-approve-real -> PASS, controlledFileId=' + controlledFileId + ', taskId=' + taskId + ', artifact=' + RESULT_PATH)
  } catch (error) {
    try {
      fs.mkdirSync(TASK_DIR, { recursive: true })
      if (activePage) {
        await activePage.screenshot({ path: path.join(TASK_DIR, 'dcc-obsolete-bpm-approve-real-failure.png'), fullPage: true })
      }
    } catch (screenshotError) {
      // do not mask the original failure
    }
    writeResult({
      status: 'FAIL',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      controlledFileId,
      taskId,
      checkedApprovalCenterPages: typeof checkedApprovalCenterPages === 'undefined' ? [] : checkedApprovalCenterPages,
      processInstanceId,
      error: error.stack || error.message,
      writeRequests,
      observedResponses,
      pageErrors
    })
    console.error(error.stack || error.message)
    process.exit(1)
  } finally {
    await browser.close()
  }
}

main()
