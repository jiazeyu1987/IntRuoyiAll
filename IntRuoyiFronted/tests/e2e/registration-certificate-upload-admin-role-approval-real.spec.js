const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

test.setTimeout(300000)

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(
  REPO_ROOT,
  'doc',
  'tasks',
  '20260830-registration-certificate-upload-flow-verification'
)
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(
  ARTIFACT_DIR,
  'registration-certificate-upload-admin-role-approval-real-result.json'
)
const APPROVER_ROLE_CODE = 'dcc_registration_certificate_approver'
const APPROVER_PERMISSION = 'dcc:registration-certificate:upload:approve'

function readDotEnvValue(name) {
  for (const fileName of ['.env.local', '.env']) {
    const filePath = path.join(FRONTEND_ROOT, fileName)
    if (!fs.existsSync(filePath)) continue
    const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/)
    for (const line of lines) {
      const match = line.match(/^\s*([A-Za-z0-9_]+)\s*=\s*(.*?)\s*$/)
      if (match && match[1] === name) {
        return match[2].replace(/^['"]|['"]$/g, '')
      }
    }
  }
  return ''
}

const config = {
  baseUrl: (
    process.env.REG_CERT_E2E_BASE_URL ||
    process.env.E2E_BASE_URL ||
    `http://127.0.0.1:${readDotEnvValue('VITE_PORT') || '8081'}`
  ).replace(/\/+$/, ''),
  tenant:
    process.env.REG_CERT_E2E_TENANT ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_TENANT') ||
    '芋道源码',
  username: process.env.REG_CERT_E2E_USERNAME || 'admin',
  password:
    process.env.REG_CERT_E2E_PASSWORD ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD') ||
    '111111',
  signaturePassword:
    process.env.REG_CERT_E2E_SIGNATURE_PASSWORD ||
    process.env.REG_CERT_E2E_PASSWORD ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD') ||
    '111111',
  uploadCompanyName:
    process.env.REG_CERT_E2E_UPLOAD_COMPANY_NAME ||
    '珠海德瑞医疗器械有限公司'
}

function optionTextPattern(text) {
  return new RegExp(`^\\s*${String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`)
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function formItem(root, label) {
  return root
    .locator('.el-form-item__label')
    .filter({ hasText: optionTextPattern(label) })
    .first()
    .locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " el-form-item ")][1]')
}

async function readJsonResponse(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

async function selectVisibleOption(page, optionText) {
  const option = page
    .locator('.el-select-dropdown__item:visible:not(.is-disabled)')
    .filter({ hasText: optionTextPattern(optionText) })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click({ timeout: 30000, force: true })
}

async function selectOptionFromSelect(page, select, optionText) {
  const combobox = select.locator('input[role="combobox"], input.el-select__input').first()
  await select.locator('.el-select__wrapper, .el-select').first().click({ timeout: 30000 })
  const controls = await combobox.getAttribute('aria-controls')
  const optionRoot = controls
    ? page.locator(`[id="${controls}"]:visible`).first()
    : page.locator('.el-select-dropdown:visible').last()
  const options = optionRoot.locator('.el-select-dropdown__item:not(.is-disabled)')
  await options.first().waitFor({ state: 'visible', timeout: 30000 })
  const optionTexts = []
  let matchedIndex = -1
  const count = await options.count()
  for (let index = 0; index < count; index += 1) {
    const text = (await options.nth(index).innerText()).trim()
    optionTexts.push(text)
    if (text === optionText && matchedIndex < 0) matchedIndex = index
  }
  expect(matchedIndex, `option ${optionText} must exist in ${optionTexts.join('/')}`).toBeGreaterThanOrEqual(0)
  await options.nth(matchedIndex).click({ timeout: 30000 })
}

async function selectDialogOption(page, dialog, label, optionText) {
  await selectOptionFromSelect(page, formItem(dialog, label).locator('.el-select').first(), optionText)
}

async function selectRemoteOwnerCompany(page, dialog, companyName) {
  const field = formItem(dialog, '公司名称')
  await field.locator('.el-select').click({ timeout: 30000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/registration-certificates/uploads/owner-companies') &&
      response.url().includes(`keyword=${encodeURIComponent(companyName)}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await field.locator('input[role="combobox"], input.el-select__input').first().fill(companyName)
  const payload = await readJsonResponse(await responsePromise)
  expect(isBusinessOk(payload), `owner company search code ${payload.code}: ${payload.msg || ''}`).toBe(true)
  const companies = Array.isArray(payload.data) ? payload.data : []
  expect(
    companies.some((item) => item.name === companyName),
    `owner company ${companyName} must exist in remote search result`
  ).toBe(true)
  await page
    .locator('.el-select-dropdown__item:visible:not(.is-disabled)')
    .filter({ hasText: companyName })
    .first()
    .click({ timeout: 30000, force: true })
}

function readWsCacheValue(snapshot, name) {
  const entries = Object.entries(snapshot || {})
  const candidates = entries.filter(([key]) => key === name || key.endsWith(`-${name}`))
  const raw = candidates.length > 0 ? candidates[candidates.length - 1][1] : ''
  if (!raw) return ''
  const normalizeString = (value) => String(value || '').replace(/^['"]|['"]$/g, '').trim()
  const unwrap = (value) => {
    let current = value
    for (let depth = 0; depth < 6; depth += 1) {
      if (current == null) return ''
      if (typeof current === 'string') return normalizeString(current)
      if (typeof current !== 'object') return normalizeString(current)
      if (Object.prototype.hasOwnProperty.call(current, 'value')) {
        current = current.value
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'v')) {
        current = current.v
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
        current = current.accessToken
        continue
      }
      return current
    }
    return current || ''
  }
  try {
    return unwrap(JSON.parse(raw))
  } catch {
    return normalizeString(raw)
  }
}

async function buildAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const accessToken = readWsCacheValue(snapshot, 'ACCESS_TOKEN')
  const tenantId = readWsCacheValue(snapshot, 'tenantId')
  const visitTenantId = readWsCacheValue(snapshot, 'visitTenantId')
  expect(accessToken, 'ACCESS_TOKEN must exist after login').toBeTruthy()
  expect(tenantId, 'tenantId must exist after login').toBeTruthy()
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

function apiUrl(pathname, params = {}) {
  const url = new URL(pathname, config.baseUrl)
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

async function requestJson(page, headers, pathname, params = {}) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, {
        method: 'GET',
        headers: requestHeaders
      })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch (error) {
        payload = { parseError: error.message, text }
      }
      return { status: response.status, payload }
    },
    { requestUrl: apiUrl(pathname, params), requestHeaders: headers }
  )
}

async function getBusinessData(page, headers, pathname, params = {}) {
  const response = await requestJson(page, headers, pathname, params)
  expect(response.status, `${pathname} HTTP status`).toBe(200)
  expect(
    isBusinessOk(response.payload),
    `${pathname} business code ${response.payload?.code}: ${response.payload?.msg || ''}`
  ).toBe(true)
  return response.payload.data
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if (await tenantInput.count()) {
    const currentTenant = (await tenantInput.inputValue()).trim()
    if (currentTenant !== config.tenant) {
      await tenantInput.fill(config.tenant)
      await selectVisibleOption(page, config.tenant)
    }
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form
    .locator('input.el-input__inner:not([role="combobox"]):visible')
    .first()
    .fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const tenantResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/tenant/get-id-by-name') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  ).catch((error) => ({ error }))
  const tenantAlert = page.locator('.login-error-alert:visible')
  const tenantAlertPromise = tenantAlert.textContent({ timeout: 60000 }).catch(() => '')
  const loginClickPromise = form.getByRole('button', { name: '登录' }).click()
  await loginClickPromise
  const tenantOutcome = await Promise.race([
    tenantResponsePromise.then(async (response) => ({
      response,
      payload: await readJsonResponse(response)
    })),
    tenantAlertPromise.then((message) => ({ alert: message }))
  ])
  if (tenantOutcome.alert) {
    throw new Error(`tenant resolve failed before login POST: ${tenantOutcome.alert.trim()}`)
  }
  const tenantPayload = tenantOutcome.payload
  expect(isBusinessOk(tenantPayload), `tenant login code ${tenantPayload.code}: ${tenantPayload.msg || ''}`).toBe(true)
  const loginResponse = await loginResponsePromise
  if (loginResponse.error) {
    throw new Error(`login POST was not sent or did not return: ${loginResponse.error.message}`)
  }
  const loginPayload = await readJsonResponse(loginResponse)
  expect(isBusinessOk(loginPayload), `login code ${loginPayload.code}: ${loginPayload.msg || ''}`).toBe(true)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

async function waitForStatus(page, headers, requestId, expectedStatus) {
  let lastStatus = null
  for (let attempt = 0; attempt < 30; attempt += 1) {
    const status = await getBusinessData(
      page,
      headers,
      `/admin-api/dcc/registration-certificates/access-requests/${requestId}`
    )
    lastStatus = status
    if (status?.requestStatus === expectedStatus) return status
    await page.waitForTimeout(1000)
  }
  throw new Error(
    `request ${requestId} did not reach ${expectedStatus}; last=${JSON.stringify(lastStatus)}`
  )
}

async function waitForCertificateRow(page, headers, certificateNo) {
  let lastRows = []
  for (let attempt = 0; attempt < 40; attempt += 1) {
    const data = await getBusinessData(page, headers, '/admin-api/dcc/registration-certificates/page', {
      pageNo: 1,
      pageSize: 10,
      certificateNo
    })
    const rows = Array.isArray(data?.list) ? data.list : []
    lastRows = rows
    const row = rows.find((item) => String(item.certificateNo) === String(certificateNo))
    if (row) return row
    await page.waitForTimeout(1500)
  }
  throw new Error(`certificate row ${certificateNo} not found; last=${JSON.stringify(lastRows)}`)
}

async function submitUpload(page, certificateNo, productName, uploadFilePath) {
  await page.goto(`${config.baseUrl}/mdm/registration-certificate`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({
    timeout: 60000
  })
  await page.getByRole('button', { name: '上传注册证' }).click()
  const dialog = page.locator('[data-testid="registration-certificate-upload-dialog"]')
  await expect(dialog).toBeVisible({ timeout: 60000 })
  await selectRemoteOwnerCompany(page, dialog, config.uploadCompanyName)
  await dialog.locator('input[placeholder="请输入产品名称"]').fill(productName)
  await dialog.locator('input[placeholder="请输入注册证号"]').fill(certificateNo)
  await selectDialogOption(page, dialog, '类别', '三类')
  const dateInputs = dialog.locator('input[placeholder="请选择日期"]')
  await dateInputs.nth(0).fill('2026-01-01')
  await dateInputs.nth(1).fill('2026-08-01')
  await dateInputs.nth(2).fill('2027-08-01')
  await selectDialogOption(page, dialog, '是否委托生产', '否')
  await selectDialogOption(page, dialog, '是否自行生产', '是')
  await dialog.locator('textarea[placeholder="请输入备注"]').fill('admin role approval e2e')
  await dialog.locator('input[type="file"]').setInputFiles(uploadFilePath)

  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/registration-certificates/uploads') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '保存' }).click()
  const uploadResponse = await uploadResponsePromise
  const uploadPayload = await readJsonResponse(uploadResponse)
  expect(uploadResponse.ok(), `upload HTTP status ${uploadResponse.status()}`).toBe(true)
  expect(isBusinessOk(uploadPayload), `upload code ${uploadPayload.code}: ${uploadPayload.msg || ''}`).toBe(true)
  expect(uploadPayload.data, 'upload request id must be returned').toBeTruthy()
  return uploadPayload.data
}

async function approveUploadInApprovalCenter(page, requestId, processInstanceId) {
  const taskPageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/page') &&
      response.request().method() === 'GET' &&
      response.url().includes('viewType=TODO'),
    { timeout: 60000 }
  )
  await page.goto(
    `${config.baseUrl}/approval-center/todo?keyword=${encodeURIComponent(String(processInstanceId))}`,
    {
      waitUntil: 'commit',
      timeout: 60000
    }
  )
  await expect(page.locator('.approval-center')).toBeVisible({ timeout: 60000 })
  const taskPayload = await readJsonResponse(await taskPageResponsePromise)
  expect(isBusinessOk(taskPayload), `approval task page code ${taskPayload.code}: ${taskPayload.msg || ''}`).toBe(true)
  const tasks = Array.isArray(taskPayload.data?.list) ? taskPayload.data.list : []
  const taskIndex = tasks.findIndex(
    (taskItem) =>
      String(taskItem.processInstanceId || '') === String(processInstanceId) ||
      String(taskItem.sourceTaskId || '') === String(requestId)
  )
  expect(taskIndex, `upload approval task ${requestId} must be visible to admin`).toBeGreaterThanOrEqual(0)
  const task = tasks[taskIndex]
  expect(task.moduleCode, 'registration upload approval uses the BPM native approval provider').toBe('BPM')
  expect(task.sourceTaskType, 'approval source task type must be BPM todo task').toBe('BPM_TASK_TODO')
  expect(task.businessTitle || '', 'approval title must identify registration certificate upload').toContain(
    '注册证上传审批'
  )
  expect(task.availableActions || [], 'upload approval must allow APPROVE').toContain('APPROVE')
  expect(
    String(task.assigneeUserName || task.assigneeUserId || ''),
    'this regression must verify admin can approve even when current BPM assignee is another user'
  ).not.toContain(config.username)

  const taskRow = page.locator('.approval-center__table .el-table__row').nth(taskIndex)
  await expect(taskRow).toBeVisible({ timeout: 60000 })
  await taskRow.getByRole('button', { name: /审核|审批/ }).first().click()
  const reviewDialog = page.locator('.approval-center__review-dialog:visible')
  await expect(reviewDialog).toBeVisible({ timeout: 30000 })
  await reviewDialog.locator('input[type="password"]').fill(config.signaturePassword)
  const reviewResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/review') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await reviewDialog.getByRole('button', { name: '确认审核' }).click()
  const reviewPayload = await readJsonResponse(await reviewResponsePromise)
  expect(
    isBusinessOk(reviewPayload),
    `approval review code ${reviewPayload.code}: ${reviewPayload.msg || ''}`
  ).toBe(true)
  expect(String(reviewPayload.msg || '')).not.toContain('审批人不是你')
  expect(String(reviewPayload.msg || '')).not.toContain('assignee')
  expect(reviewPayload.data, 'approval review result must be true').toBe(true)
  return task
}

test('admin with registration manager role can approve upload task assigned to another user', async ({
  page
}, testInfo) => {
  await login(page)
  const headers = await buildAuthHeaders(page)
  const permissionData = await getBusinessData(page, headers, '/admin-api/system/auth/get-permission-info')
  const permissionPayload = { code: 0, data: permissionData }
  const permissionText = JSON.stringify(permissionPayload.data || {})
  expect(permissionText).toContain(APPROVER_PERMISSION)
  expect(permissionText).toContain(APPROVER_ROLE_CODE)
  const signatureImage = await getBusinessData(
    page,
    headers,
    '/admin-api/dcc/electronic-signature-authorizations/my-image'
  )
  expect(signatureImage?.active, 'admin must already have an active signature image').toBe(true)

  const runKey = `${Date.now()}-${testInfo.workerIndex}`
  const certificateNo = `REGCERT-ADMIN-APPROVE-${runKey}`
  const productName = `注册证上传审核回归产品-${runKey}`
  const uploadFilePath = testInfo.outputPath(`${certificateNo}.pdf`)
  fs.writeFileSync(uploadFilePath, Buffer.from('%PDF-1.4\n% Codex admin approval E2E\n', 'utf8'))

  const requestId = await submitUpload(page, certificateNo, productName, uploadFilePath)
  const boundStatus = await waitForStatus(page, headers, requestId, 'BPM_BOUND')
  expect(boundStatus.bpmProcessInstanceId, 'upload request must expose BPM process instance id').toBeTruthy()

  const approvalTask = await approveUploadInApprovalCenter(
    page,
    requestId,
    boundStatus.bpmProcessInstanceId
  )
  const approvedStatus = await waitForStatus(page, headers, requestId, 'APPROVED')
  const certificateRow = await waitForCertificateRow(page, headers, certificateNo)

  const result = {
    requestId,
    certificateNo,
    uploadCompanyName: config.uploadCompanyName,
    processInstanceId: boundStatus.bpmProcessInstanceId,
    originalAssigneeUserId: approvalTask.assigneeUserId || null,
    originalAssigneeUserName: approvalTask.assigneeUserName || null,
    reviewedBy: config.username,
    finalRequestStatus: approvedStatus.requestStatus,
    certificateStatus: certificateRow.status,
    certificateVersionNo: certificateRow.versionNo
  }
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
})
