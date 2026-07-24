const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_OBSOLETE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_OBSOLETE_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_OBSOLETE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_OBSOLETE_E2E_PASSWORD || '111111'
const APPROVAL_MODE = process.env.DCC_OBSOLETE_E2E_APPROVAL_MODE || 'BPM_REQUIRED'
const TASK_DIR = process.env.DCC_OBSOLETE_E2E_TASK_DIR
  ? path.resolve(process.env.DCC_OBSOLETE_E2E_TASK_DIR)
  : path.resolve(__dirname, '../../../doc/tasks/20260720-form-center-controlled-state-machine-implementation/e2e-artifacts')
const PROBE_PATH = path.join(TASK_DIR, 'dcc-obsolete-real-sample-probe.json')
const RESULT_PATH = path.join(TASK_DIR, 'dcc-obsolete-form-center-real-submit.json')

function writeResult(result) {
  fs.mkdirSync(TASK_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, JSON.stringify(result, null, 2) + '\n', 'utf8')
}

function readResultStatus() {
  try {
    return JSON.parse(fs.readFileSync(RESULT_PATH, 'utf8')).status
  } catch (error) {
    return undefined
  }
}

function readSelectedSample() {
  const probe = JSON.parse(fs.readFileSync(PROBE_PATH, 'utf8'))
  assert.equal(probe.status, 'PASS', 'sample probe must pass before submit E2E')
  assert.ok(probe.selected?.id, 'sample probe selected id is missing')
  return probe.selected
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
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, 'login HTTP status ' + loginResponse.status())
  assert.ok([0, 200].includes(loginPayload.code), 'login business code ' + loginPayload.code)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

function unwrapResponsePayload(payload, label) {
  assert.ok([0, 200].includes(payload.code), label + ' business code ' + payload.code + ': ' + (payload.msg || ''))
  return payload.data
}

async function readDetailByReload(page, controlledFileId) {
  const detailResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/dcc/controlled-files/' + controlledFileId) && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(BASE_URL + '/dcc/controlled-file/detail/' + controlledFileId, { waitUntil: 'commit' })
  const detailResponse = await detailResponsePromise
  assert.equal(detailResponse.ok(), true, 'detail HTTP status ' + detailResponse.status())
  return unwrapResponsePayload(await detailResponse.json(), 'detail')
}

async function fillStartUserSelectedAssignees(page, dialog) {
  const assigneeItems = dialog.locator('.el-form-item').filter({ hasText: /审批人/ })
  await assigneeItems.first().waitFor({ state: 'visible', timeout: 60000 })
  const itemCount = await assigneeItems.count()
  assert.ok(itemCount > 0, 'DCC obsolete submit must expose start-user-selected approver fields')
  for (let index = 0; index < itemCount; index += 1) {
    const item = assigneeItems.nth(index)
    await item.locator('.el-input:visible, .el-input__wrapper:visible').first().click()

    const userDialog = page.locator('.el-dialog:visible').filter({ hasText: '人员选择' }).last()
    await userDialog.waitFor({ state: 'visible', timeout: 60000 })
    await userDialog.locator('.el-form-item').filter({ hasText: '用户名称' }).locator('input').first().fill(USERNAME)

    const queryResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/system/user/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await userDialog.getByRole('button', { name: /搜索/ }).click()
    await queryResponsePromise

    const row = userDialog.locator('.el-table__body-wrapper tbody tr').filter({ hasText: USERNAME }).first()
    await row.waitFor({ state: 'visible', timeout: 60000 })
    await row.locator('.el-checkbox__input').first().click()
    await userDialog.getByRole('button', { name: /确\s*定/ }).click()
    await userDialog.waitFor({ state: 'hidden', timeout: 60000 })
  }
}

function assertBpmRequiredResult(submitted, afterDetail) {
  const projection = afterDetail.actionProjection || {}
  assert.equal(submitted.status, 'IN_APPROVAL', 'BPM_REQUIRED submitted form instance must enter IN_APPROVAL')
  assert.ok(submitted.bpmProcessInstanceId, 'BPM_REQUIRED submitted form instance must have bpmProcessInstanceId')
  assert.equal(projection.actionLocked, true, 'DCC file must be actionLocked while obsolete approval is pending')
  assert.equal(
    projection.actionLockReason,
    'OBSOLETE_APPROVAL_PENDING',
    'DCC lock reason must be OBSOLETE_APPROVAL_PENDING'
  )
  assert.ok(projection.pendingRequestId, 'DCC projection must expose pendingRequestId')
  assert.equal(projection.allowedActions.includes('OBSOLETE'), false, 'OBSOLETE action must be hidden while pending')
  assert.equal(afterDetail.status, 'ACTIVE', 'DCC file must remain ACTIVE until approval effect is applied')
  return {
    status: 'PASS_PENDING_LOCK',
    instanceStatus: submitted.status,
    bpmProcessInstanceId: submitted.bpmProcessInstanceId,
    actionProjection: projection
  }
}

function assertDirectResult(submitted, afterDetail) {
  const projection = afterDetail.actionProjection || {}
  assert.equal(submitted.status, 'EFFECTIVE', 'DIRECT submitted form instance must become EFFECTIVE')
  assert.equal(
    submitted.bpmProcessInstanceId == null,
    true,
    'DIRECT submitted form instance must not have bpmProcessInstanceId'
  )
  assert.equal(afterDetail.status, 'OBSOLETE', 'DIRECT DCC obsolete must apply the domain terminal state')
  assert.notEqual(
    projection.actionLockReason,
    'OBSOLETE_APPROVAL_PENDING',
    'DIRECT obsolete must not leave a pending approval lock reason'
  )
  assert.equal(
    projection.pendingRequestId == null,
    true,
    'DIRECT obsolete must not leave a pending form-center request'
  )
  assert.equal(
    projection.actionLockReason,
    'Controlled file version is terminal',
    'DIRECT obsolete terminal file should only be locked because the file version is terminal'
  )
  assert.equal(
    (projection.allowedActions || []).includes('OBSOLETE'),
    false,
    'DIRECT obsolete terminal file must not still expose OBSOLETE'
  )
  return {
    status: 'PASS_DIRECT_EFFECT',
    instanceStatus: submitted.status,
    bpmProcessInstanceId: submitted.bpmProcessInstanceId || null,
    actionProjection: projection
  }
}

async function main() {
  assert.equal(TENANT, process.env.DCC_OBSOLETE_E2E_EXPECT_TENANT || '测试租户', 'DCC obsolete submit E2E tenant mismatch')
  assert.equal(USERNAME, process.env.DCC_OBSOLETE_E2E_EXPECT_USERNAME || 'aoteman', 'DCC obsolete submit E2E username mismatch')
  assert.ok(['BPM_REQUIRED', 'DIRECT'].includes(APPROVAL_MODE), 'DCC_OBSOLETE_E2E_APPROVAL_MODE must be BPM_REQUIRED or DIRECT')
  const selected = readSelectedSample()
  const controlledFileId = selected.id
  const reason = 'E2E-DCC-OBSOLETE-FORM-CENTER-' + Date.now()
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const pageErrors = []
  const writeRequests = []
  const observedResponses = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('request', (request) => {
      if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/')) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })
    page.on('response', async (response) => {
      const url = response.url()
      if (url.includes('/form-center/') || url.includes('/dcc/controlled-files/')) {
        observedResponses.push({ method: response.request().method(), status: response.status(), url })
      }
    })
    await login(page)

    const beforeDetail = await readDetailByReload(page, controlledFileId)
    assert.equal(beforeDetail.status, 'ACTIVE', 'selected DCC file must still be ACTIVE before submit')
    assert.ok(beforeDetail.actionProjection?.allowedActions?.includes('OBSOLETE'), 'selected DCC file must allow OBSOLETE before submit')

    await page.getByRole('button', { name: /风险操作/ }).click()
    await page.getByRole('menuitem', { name: '作废当前版本' }).click()
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '作废当前版本' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    await dialog.locator('textarea').fill(reason)
    const panel = dialog.locator('[data-testid="dcc-obsolete-form-center-panel"]').first()
    await panel.waitFor({ state: 'visible', timeout: 60000 })

    const resolveResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/form-center/actions/resolve') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await panel.getByRole('button', { name: /解析/ }).click()
    const resolveResponse = await resolveResponsePromise
    const resolvePayload = await resolveResponse.json()
    const resolution = unwrapResponsePayload(resolvePayload, 'resolve business action')
    if (resolution.requiresForm !== true) {
      writeResult({
        status: 'FAIL_RESOLVE_NO_FORM',
        baseUrl: BASE_URL,
        tenant: TENANT,
        username: USERNAME,
        controlledFileId,
        beforeDetail: {
          id: beforeDetail.id,
          fileNumber: beforeDetail.fileNumber,
          versionNo: beforeDetail.versionNo,
          status: beforeDetail.status,
          actionProjection: beforeDetail.actionProjection
        },
        resolvePayload,
        resolution,
        writeRequests,
        observedResponses,
        pageErrors
      })
    }
    assert.equal(resolution.requiresForm, true, 'DCC obsolete action must require a form-center form')
    assert.equal(resolution.approvalMode, APPROVAL_MODE, 'resolved approval mode must match DCC_OBSOLETE_E2E_APPROVAL_MODE')
    assert.equal(
      resolution.requiresBpm,
      APPROVAL_MODE === 'BPM_REQUIRED',
      'resolved requiresBpm must match DCC obsolete approval mode'
    )

    if (APPROVAL_MODE === 'BPM_REQUIRED') {
      await fillStartUserSelectedAssignees(page, dialog)
    }

    const createResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/form-center/instances') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await panel.getByRole('button', { name: /创建/ }).click()
    const createResponse = await createResponsePromise
    const created = unwrapResponsePayload(await createResponse.json(), 'create form instance')
    assert.ok(created.id, 'created form instance id is missing')

    const submitResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/form-center/instances/' + created.id + '/submit') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await panel.getByRole('button', { name: /^提交$/ }).click()
    const submitResponse = await submitResponsePromise
    const submitted = unwrapResponsePayload(await submitResponse.json(), 'submit form instance')

    const afterDetail = await readDetailByReload(page, controlledFileId)
    const modeResult = APPROVAL_MODE === 'DIRECT'
      ? assertDirectResult(submitted, afterDetail)
      : assertBpmRequiredResult(submitted, afterDetail)
    assert.deepEqual(pageErrors, [], 'DCC obsolete submit E2E must not produce page errors')

    const result = {
      status: modeResult.status,
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      approvalMode: APPROVAL_MODE,
      controlledFileId,
      fileNumber: afterDetail.fileNumber,
      versionNo: afterDetail.versionNo,
      reason,
      instanceId: created.id,
      instanceStatus: modeResult.instanceStatus,
      bpmProcessInstanceId: modeResult.bpmProcessInstanceId,
      finalFileStatus: afterDetail.status,
      actionProjection: modeResult.actionProjection,
      writeRequests,
      observedResponses,
      pageErrors
    }
    writeResult(result)
    console.log(
      'GREEN: dcc-obsolete-form-center-real-submit -> ' +
        modeResult.status +
        ', controlledFileId=' +
        controlledFileId +
        ', instanceId=' +
        created.id +
        ', artifact=' +
        RESULT_PATH
    )
  } catch (error) {
    if (readResultStatus() !== 'FAIL_RESOLVE_NO_FORM') {
      writeResult({
        status: 'FAIL',
        baseUrl: BASE_URL,
        tenant: TENANT,
        username: USERNAME,
        controlledFileId,
        error: error.stack || error.message,
        writeRequests,
        observedResponses,
        pageErrors
      })
    }
    console.error(error.stack || error.message)
    process.exit(1)
  } finally {
    await browser.close()
  }
}

main()
