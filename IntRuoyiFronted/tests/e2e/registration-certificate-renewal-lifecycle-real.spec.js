const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(
  REPO_ROOT,
  'doc',
  'tasks',
  '20260829-registration-certificate-renewal-category-notify'
)
const ARTIFACT_DIR = process.env.REG_CERT_E2E_ARTIFACT_DIR
  ? path.resolve(process.env.REG_CERT_E2E_ARTIFACT_DIR)
  : path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'registration-certificate-renewal-lifecycle-result.json')
const APPROVER_ROLE_CODE = 'dcc_registration_certificate_approver'
const APPROVER_PASSWORD = process.env.REG_CERT_E2E_APPROVER_PASSWORD || 'admin123'
const SIGNATURE_IMAGE_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO5lH3cAAAAASUVORK5CYII='

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
  username:
    process.env.REG_CERT_E2E_USERNAME ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_USERNAME') ||
    'admin',
  password:
    process.env.REG_CERT_E2E_PASSWORD ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD'),
  uploadCompanyName:
    process.env.REG_CERT_E2E_UPLOAD_COMPANY_NAME ||
    '上海瑛泰医疗器械股份有限公司',
  projectCode: process.env.REG_CERT_E2E_PROJECT_CODE || 'T0720260827E2E',
  runKey: process.env.REG_CERT_E2E_RUN_KEY || '',
  businessDate: process.env.REG_CERT_E2E_BUSINESS_DATE || '2026-08-29'
}

const CURRENT_TAB_SELECTOR = '[data-testid="registration-certificate-current-tab"]'
const OLD_TAB_SELECTOR = '[data-testid="registration-certificate-old-index"]'
const INITIAL_EFFECTIVE_DATE = '2026-01-01'
const INITIAL_EXPIRY_DATE = '2026-12-31'
const RENEWAL_EXPIRY_DATE = '2029-08-29'

function initialCertificateNo() {
  return `REGCERT-E2E-${config.runKey}-A`
}

function writeResult(result) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function optionTextPattern(text) {
  return new RegExp(`^\\s*${String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`)
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function responsePathMatches(response, expectedApiPath) {
  return new URL(response.url()).pathname === expectedApiPath
}

function responseSearchParamEquals(response, name, expectedValue) {
  return new URL(response.url()).searchParams.get(name) === String(expectedValue)
}

function summarizePageRows(data) {
  const rows = Array.isArray(data?.list) ? data.list : []
  return {
    total: Number(data?.total || 0),
    certificateNos: rows.slice(0, 5).map((row) => row.certificateNo).filter(Boolean)
  }
}

function toIsoDate(value) {
  if (Array.isArray(value) && value.length >= 3) {
    return [
      String(value[0]).padStart(4, '0'),
      String(value[1]).padStart(2, '0'),
      String(value[2]).padStart(2, '0')
    ].join('-')
  }
  if (typeof value === 'string') {
    return value.slice(0, 10)
  }
  return value
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

function formItem(root, label) {
  return root
    .locator('.el-form-item__label')
    .filter({ hasText: optionTextPattern(label) })
    .first()
    .locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " el-form-item ")][1]')
}

async function selectBooleanOption(page, select, optionText) {
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
  for (let index = 0; index < await options.count(); index += 1) {
    const text = (await options.nth(index).innerText()).trim()
    optionTexts.push(text)
    if (text === optionText && matchedIndex < 0) matchedIndex = index
  }
  expect(matchedIndex, `option ${optionText} must exist in ${optionTexts.join('/')}`).toBeGreaterThanOrEqual(0)
  await options.nth(matchedIndex).click({ timeout: 30000 })
  await expect
    .poll(
      async () => {
        const values = await select
          .locator('.el-select__selected-item:not(.el-select__input-wrapper)')
          .allInnerTexts()
        return values
          .map((value) => value.trim())
          .find((value) => value && !value.startsWith('请选择')) || ''
      },
      { timeout: 10000 }
    )
    .toBe(optionText)
}

async function selectDialogOption(page, dialog, label, optionText) {
  await selectBooleanOption(page, formItem(dialog, label).locator('.el-select').first(), optionText)
}

async function selectRemoteProjectCode(page, dialog, projectCode) {
  const field = formItem(dialog, 'DCC项目代码')
  await field.locator('.el-select').click({ timeout: 30000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/project-codes/page') &&
      response.url().includes(`keyword=${encodeURIComponent(projectCode)}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await field.locator('input[role="combobox"], input.el-select__input').first().fill(projectCode)
  const payload = await readJsonResponse(await responsePromise)
  expect(
    isBusinessOk(payload),
    `project code search code ${payload.code}: ${payload.msg || ''}`
  ).toBe(true)
  expect(
    (payload.data?.list || []).some((item) => item.projectCode === projectCode),
    `project code ${projectCode} must exist in remote search result`
  ).toBe(true)
  await page
    .locator('.el-select-dropdown__item:visible:not(.is-disabled)')
    .filter({ hasText: projectCode })
    .first()
    .click({ timeout: 30000, force: true })
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
  expect(
    isBusinessOk(payload),
    `owner company search code ${payload.code}: ${payload.msg || ''}`
  ).toBe(true)
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

async function fillFormInput(root, label, value) {
  await formItem(root, label).locator('input.el-input__inner').first().fill(value)
}

function normalizeString(raw) {
  if (raw === undefined || raw === null) return ''
  let current = String(raw).trim()
  for (let index = 0; index < 6; index += 1) {
    try {
      const parsed = JSON.parse(current)
      if (typeof parsed !== 'string' || parsed === current) return current.replace(/^"(.*)"$/, '$1')
      current = parsed
    } catch {
      return current.replace(/^"(.*)"$/, '$1')
    }
  }
  return String(current).trim()
}

function readWsCacheValue(snapshot, cacheKey) {
  const raw = snapshot[cacheKey] ?? snapshot[`vueuse-color-scheme-${cacheKey}`]
  if (!raw) return ''
  const unwrap = (value) => {
    let current = value
    for (let index = 0; index < 6; index += 1) {
      if (!current || typeof current !== 'object') {
        return typeof current === 'string' ? normalizeString(current) : current || ''
      }
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

async function requestJsonWithMethod(page, headers, method, pathname, body) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders, requestMethod, requestBody }) => {
      const response = await fetch(requestUrl, {
        method: requestMethod,
        headers:
          requestBody && requestMethod !== 'GET'
            ? { ...requestHeaders, 'Content-Type': 'application/json' }
            : requestHeaders,
        body: requestBody
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
    {
      requestUrl: apiUrl(pathname),
      requestHeaders: headers,
      requestMethod: method,
      requestBody: body
    }
  )
}

async function updateUserPassword(page, headers, userId, password) {
  const response = await requestJsonWithMethod(
    page,
    headers,
    'PUT',
    '/admin-api/system/user/update-password',
    JSON.stringify({ id: userId, password })
  )
  expect(response.status, `/system/user/update-password HTTP status for ${userId}`).toBe(200)
  expect(
    isBusinessOk(response.payload),
    `/system/user/update-password business code ${response.payload?.code}: ${response.payload?.msg || ''}`
  ).toBe(true)
}

async function getApproverCandidates(page, headers) {
  const rolePage = await getBusinessData(page, headers, '/admin-api/system/role/page', {
    pageNo: 1,
    pageSize: 100,
    code: APPROVER_ROLE_CODE
  })
  const role = Array.isArray(rolePage.list)
    ? rolePage.list.find((item) => item.code === APPROVER_ROLE_CODE)
    : null
  expect(role, `role ${APPROVER_ROLE_CODE} must exist`).toBeTruthy()
  const userPage = await getBusinessData(page, headers, '/admin-api/system/user/page', {
    pageNo: 1,
    pageSize: 200,
    roleId: role.id
  })
  const users = Array.isArray(userPage.list) ? userPage.list : []
  expect(users.length, `role ${APPROVER_ROLE_CODE} must have candidate users`).toBeGreaterThan(0)
  return users.map((user) => ({
    id: user.id,
    username: user.username,
    nickname: user.nickname
  }))
}

async function uploadSignatureImage(page, headers, fileName, reason) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders, fileNameValue, reasonValue, base64Image }) => {
      const bytes = Uint8Array.from(atob(base64Image), (char) => char.charCodeAt(0))
      const file = new File([bytes], fileNameValue, { type: 'image/png' })
      const formData = new FormData()
      formData.append('file', file)
      formData.append('reason', reasonValue)
      const safeHeaders = { ...requestHeaders }
      delete safeHeaders['Content-Type']
      delete safeHeaders['content-type']
      const response = await fetch(requestUrl, {
        method: 'POST',
        headers: safeHeaders,
        body: formData
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
    {
      requestUrl: apiUrl('/admin-api/dcc/electronic-signature-authorizations/my-image/upload'),
      requestHeaders: headers,
      fileNameValue: fileName,
      reasonValue: reason,
      base64Image: SIGNATURE_IMAGE_BASE64
    }
  )
}

async function enableSignatureImage(page, headers, imageId, reason) {
  return await requestJsonWithMethod(
    page,
    headers,
    'POST',
    `/admin-api/dcc/electronic-signature-authorizations/my-image/${imageId}/enable?reason=${encodeURIComponent(reason)}`
  )
}

async function ensureActiveSignatureImage(page, headers, username) {
  const currentImage = await getBusinessData(page, headers, '/admin-api/dcc/electronic-signature-authorizations/my-image')
  if (currentImage && currentImage.active) {
    return currentImage
  }
  const reason = `REGCERT-E2E-${config.runKey}-SIGNATURE-${username}`
  const uploadResponse = await uploadSignatureImage(
    page,
    headers,
    `${username}-signature.png`,
    reason
  )
  expect(uploadResponse.status, `/my-image/upload HTTP status for ${username}`).toBe(200)
  expect(
    isBusinessOk(uploadResponse.payload),
    `/my-image/upload business code ${uploadResponse.payload?.code}: ${uploadResponse.payload?.msg || ''}`
  ).toBe(true)
  const uploadedImage = uploadResponse.payload.data
  expect(uploadedImage?.id, `signature image upload for ${username} must return image id`).toBeTruthy()
  const enableResponse = await enableSignatureImage(page, headers, uploadedImage.id, reason)
  expect(enableResponse.status, `/my-image/{id}/enable HTTP status for ${username}`).toBe(200)
  expect(
    isBusinessOk(enableResponse.payload),
    `/my-image/{id}/enable business code ${enableResponse.payload?.code}: ${enableResponse.payload?.msg || ''}`
  ).toBe(true)
  const enabledImage = await getBusinessData(
    page,
    headers,
    '/admin-api/dcc/electronic-signature-authorizations/my-image'
  )
  expect(enabledImage?.active, `signature image for ${username} must be active after enable`).toBe(true)
  return enabledImage
}

async function login(page, credentials) {
  expect(credentials.password, 'login password must be available without logging it').toBeTruthy()
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    await selectVisibleOption(page, config.tenant)
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form
    .locator('input.el-input__inner:not([role="combobox"]):visible')
    .first()
    .fill(credentials.username)
  await form.locator('input[type="password"]').first().fill(credentials.password)

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
  )
  await form.getByRole('button', { name: '登录' }).click()
  const tenantPayload = await readJsonResponse(await tenantResponsePromise)
  const loginPayload = await readJsonResponse(await loginResponsePromise)
  expect(isBusinessOk(tenantPayload), `tenant login code ${tenantPayload.code}`).toBe(true)
  expect(isBusinessOk(loginPayload), `login code ${loginPayload.code}`).toBe(true)
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

async function waitForCertificateRow(page, headers, pathname, certificateNo, predicate) {
  let lastRows = []
  for (let attempt = 0; attempt < 40; attempt += 1) {
    const data = await getBusinessData(page, headers, pathname, {
      pageNo: 1,
      pageSize: 10,
      certificateNo
    })
    const rows = Array.isArray(data?.list) ? data.list : []
    lastRows = rows
    const row = rows.find((item) => String(item.certificateNo) === String(certificateNo))
    if (row && (!predicate || predicate(row))) return row
    await page.waitForTimeout(1500)
  }
  throw new Error(`certificate row ${certificateNo} not found at ${pathname}; last=${JSON.stringify(lastRows)}`)
}

async function findCertificateRow(page, headers, pathname, certificateNo, predicate) {
  const data = await getBusinessData(page, headers, pathname, {
    pageNo: 1,
    pageSize: 10,
    certificateNo
  })
  const rows = Array.isArray(data?.list) ? data.list : []
  const row = rows.find((item) => String(item.certificateNo) === String(certificateNo))
  if (row && (!predicate || predicate(row))) return row
  return null
}

async function addAndApplyTextFilter(
  page,
  tableScopeSelector,
  fieldLabel,
  value,
  expectedApiPath,
  evidence,
  evidenceKey
) {
  const scope = page.locator(tableScopeSelector).first()
  await scope.waitFor({ state: 'visible', timeout: 60000 })
  await waitForTableIdle(scope)
  const filter = scope.locator('.table-multi-filter').first()
  await filter.waitFor({ state: 'visible', timeout: 60000 })
  if ((await filter.locator('.table-multi-filter__condition-row:visible').count()) === 0) {
    await filter.getByRole('button', { name: '新增筛选条件' }).click()
  }
  await filter.locator('.table-multi-filter__field-select').click()
  await selectVisibleOption(page, fieldLabel)
  const valueInput = filter.locator('.table-multi-filter-field__value input.el-input__inner').first()
  await valueInput.fill(value)
  await expect(valueInput, `${fieldLabel} filter input must contain target value before query`).toHaveValue(
    value,
    { timeout: 30000 }
  )
  await expect(filter.locator('.table-multi-filter__pending-status')).toBeVisible({ timeout: 30000 })
  const responsePromise = page.waitForResponse(
    (response) =>
      responsePathMatches(response, expectedApiPath) &&
      response.request().method() === 'GET' &&
      responseSearchParamEquals(response, 'certificateNo', value),
    { timeout: 60000 }
  )
  await filter.getByRole('button', { name: '查询' }).click()
  const payload = await readJsonResponse(await responsePromise)
  expect(
    isBusinessOk(payload),
    `filter ${expectedApiPath} code ${payload.code}: ${payload.msg || ''}`
  ).toBe(true)
  const pageSummary = summarizePageRows(payload.data)
  if (evidence && evidenceKey) {
    evidence[evidenceKey] = {
      fieldLabel,
      value,
      requestPath: expectedApiPath,
      total: pageSummary.total,
      certificateNos: pageSummary.certificateNos
    }
  }
  expect(
    pageSummary.certificateNos.includes(value),
    `filtered ${expectedApiPath} must return ${value}; total=${pageSummary.total}; first=${pageSummary.certificateNos.join(', ')}`
  ).toBe(true)
  return payload.data
}

async function waitForTableIdle(tableScope) {
  await expect(tableScope.locator('.el-loading-mask:visible')).toHaveCount(0, { timeout: 60000 })
}

async function submitInitialUpload(page, evidence, testInfo) {
  const certificateNo = initialCertificateNo()
  const uploadFilePath = testInfo.outputPath(`${certificateNo}.pdf`)
  fs.writeFileSync(uploadFilePath, Buffer.from('%PDF-1.4\n% Codex registration certificate E2E\n', 'utf8'))

  const projectCodeResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/project-codes/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
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
  await projectCodeResponsePromise
  await selectRemoteProjectCode(page, dialog, config.projectCode)
  await expect(formItem(dialog, '产品名称').locator('input.el-input__inner')).not.toHaveValue('', {
    timeout: 30000
  })
  await selectRemoteOwnerCompany(page, dialog, config.uploadCompanyName)
  await fillFormInput(dialog, '注册证号', certificateNo)
  await fillFormInput(dialog, '类别', 'II类')
  const dateInputs = dialog.locator('input[placeholder="请选择日期"]')
  await dateInputs.nth(0).fill('2025-12-31')
  await dateInputs.nth(1).fill(INITIAL_EFFECTIVE_DATE)
  await dateInputs.nth(2).fill(INITIAL_EXPIRY_DATE)
  await selectBooleanOption(
    page,
    dialog.locator('[data-testid="registration-certificate-upload-entrusted-production"]'),
    '否'
  )
  await selectBooleanOption(
    page,
    dialog.locator('[data-testid="registration-certificate-upload-self-production"]'),
    '是'
  )
  await dialog.locator('textarea[placeholder="请输入备注"]').fill(`REGCERT-E2E-${config.runKey}`)
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
  expect(
    isBusinessOk(uploadPayload),
    `upload code ${uploadPayload.code}: ${uploadPayload.msg || ''}`
  ).toBe(true)
  expect(uploadPayload.data, 'upload request id must be returned').toBeTruthy()
  evidence.initialUpload = {
    certificateNo,
    requestId: uploadPayload.data,
    idempotencyKey: uploadResponse.request().headers()['idempotency-key']
  }
  return { certificateNo, requestId: uploadPayload.data }
}

async function approveRequestInApprovalCenter(browser, request, label, candidates, evidence) {
  expect(Array.isArray(candidates) && candidates.length > 0, `${label} approver candidates are required`).toBe(true)
  const approvalKeyword = encodeURIComponent(String(request.bpmProcessInstanceId))
  const triedUsers = []
  for (const reviewer of candidates) {
    const reviewerContext = await browser.newContext()
    const reviewerPage = await reviewerContext.newPage()
    try {
      await login(reviewerPage, {
        username: reviewer.username,
        password: APPROVER_PASSWORD
      })
      const taskPageResponsePromise = reviewerPage.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/approval-center/tasks/page') &&
          response.request().method() === 'GET' &&
          response.url().includes('viewType=TODO'),
        { timeout: 60000 }
      )
      await reviewerPage.goto(`${config.baseUrl}/approval-center/todo?keyword=${approvalKeyword}`, {
        waitUntil: 'commit',
        timeout: 60000
      })
      await expect(reviewerPage.locator('.approval-center')).toBeVisible({ timeout: 60000 })
      const taskPayload = await readJsonResponse(await taskPageResponsePromise)
      expect(
        isBusinessOk(taskPayload),
        `approval task page code ${taskPayload.code}: ${taskPayload.msg || ''}`
      ).toBe(true)
      const tasks = Array.isArray(taskPayload.data?.list) ? taskPayload.data.list : []
      const approvalTaskIndex = tasks.findIndex(
        (taskItem) =>
          String(taskItem.processInstanceId || '') === String(request.bpmProcessInstanceId) ||
          String(taskItem.businessKey || '') === String(request.bpmProcessInstanceId) ||
          String(taskItem.sourceTaskId || '') === String(request.requestId)
      )
      const approvalTask = approvalTaskIndex >= 0 ? tasks[approvalTaskIndex] : null
      if (!approvalTask) {
        triedUsers.push(reviewer.username)
        continue
      }
      const reviewerHeaders = await buildAuthHeaders(reviewerPage)
      await ensureActiveSignatureImage(reviewerPage, reviewerHeaders, reviewer.username)
      expect(approvalTask.availableActions || [], `${label} approval must allow APPROVE`).toContain('APPROVE')
      const taskRow = reviewerPage.locator('.approval-center__table .el-table__row').nth(approvalTaskIndex)
      await expect(taskRow).toBeVisible({ timeout: 60000 })
      await taskRow.getByRole('button', { name: /审核|审批/ }).first().click()
      const reviewDialog = reviewerPage.locator('.approval-center__review-dialog:visible')
      await expect(reviewDialog).toBeVisible({ timeout: 30000 })
      await reviewDialog.locator('input[type="password"]').fill(APPROVER_PASSWORD)
      const reviewResponsePromise = reviewerPage.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/approval-center/tasks/review') &&
          response.request().method() === 'POST',
        { timeout: 60000 }
      )
      await reviewDialog.getByRole('button', { name: '确认审核' }).click()
      const reviewPayload = await readJsonResponse(await reviewResponsePromise)
      expect(
        isBusinessOk(reviewPayload),
        `${label} review code ${reviewPayload.code}: ${reviewPayload.msg || ''}`
      ).toBe(true)
      expect(reviewPayload.data, `${label} review result must be true`).toBe(true)
      evidence.approvals.push({
        label,
        requestId: request.requestId,
        processInstanceId: request.bpmProcessInstanceId,
        reviewerUsername: reviewer.username,
        result: 'APPROVE'
      })
      return reviewer.username
    } finally {
      await reviewerContext.close()
    }
  }
  throw new Error(`${label} approval task must be visible to one approver candidate; tried=${triedUsers.join(',')}`)
}

async function submitRenewal(page, currentRow, evidence, testInfo) {
  const renewalCertificateNo = `${currentRow.certificateNo}-R`
  const renewalFilePath = testInfo.outputPath(`${renewalCertificateNo}.pdf`)
  fs.writeFileSync(renewalFilePath, Buffer.from('%PDF-1.4\n% Codex renewal certificate E2E\n', 'utf8'))

  await addAndApplyTextFilter(
    page,
    CURRENT_TAB_SELECTOR,
    '注册证编号',
    currentRow.certificateNo,
    '/admin-api/dcc/registration-certificates/page',
    evidence,
    'currentFilter'
  )
  const currentTab = page.locator(CURRENT_TAB_SELECTOR)
  await waitForTableIdle(currentTab)
  const certificateRow = currentTab
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: currentRow.certificateNo })
    .first()
  await expect(certificateRow).toBeVisible({ timeout: 60000 })
  await certificateRow.getByRole('button', { name: '延续' }).first().click()

  const dialog = page.locator('[data-testid="registration-certificate-renewal-dialog"]')
  await expect(dialog).toBeVisible({ timeout: 60000 })
  const dateInputs = dialog.locator('input[placeholder="请选择日期"]')
  await dateInputs.nth(0).fill(config.businessDate)
  await dateInputs.nth(1).fill(config.businessDate)
  await dateInputs.nth(2).fill(RENEWAL_EXPIRY_DATE)
  await selectDialogOption(page, dialog, '类别否变更', '是')
  await dialog.locator('input[placeholder="请输入变更后的注册证号"]').fill(renewalCertificateNo)
  await dialog.locator('input[placeholder="请输入变更后的类别"]').fill('III类')
  await dialog.locator('input[type="file"]').setInputFiles(renewalFilePath)

  const renewalResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(
        `/admin-api/dcc/registration-certificates/${currentRow.certificateId}/renewals`
      ) && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '提交审批' }).click()
  const renewalResponse = await renewalResponsePromise
  const renewalPayload = await readJsonResponse(renewalResponse)
  expect(renewalResponse.ok(), `renewal HTTP status ${renewalResponse.status()}`).toBe(true)
  expect(
    isBusinessOk(renewalPayload),
    `renewal code ${renewalPayload.code}: ${renewalPayload.msg || ''}`
  ).toBe(true)
  expect(renewalPayload.data, 'renewal request id must be returned').toBeTruthy()
  await expect(dialog).toBeHidden({ timeout: 30000 })
  evidence.renewalUpload = {
    certificateNo: renewalCertificateNo,
    requestId: renewalPayload.data,
    idempotencyKey: renewalResponse.request().headers()['idempotency-key'],
    approvalDate: config.businessDate,
    effectiveDate: config.businessDate,
    expiryDate: RENEWAL_EXPIRY_DATE,
    categoryChanged: true,
    classification: 'III类'
  }
  return { certificateNo: renewalCertificateNo, requestId: renewalPayload.data }
}

async function runActivationJobThroughApi(page, evidence) {
  const headers = await buildAuthHeaders(page)
  const jobsPage = await getBusinessData(page, headers, '/admin-api/infra/job/page', {
    pageNo: 1,
    pageSize: 100,
    handlerName: 'registrationCertificateReminderDailyJob'
  })
  const jobs = Array.isArray(jobsPage.list) ? jobsPage.list : []
  const job = jobs.find((item) => item.handlerName === 'registrationCertificateReminderDailyJob')
  expect(job, 'registration certificate daily job must be listed').toBeTruthy()
  const triggerPayloadResponse = await requestJsonWithMethod(
    page,
    headers,
    'PUT',
    `/admin-api/infra/job/trigger?id=${job.id}`
  )
  expect(
    triggerPayloadResponse.status,
    `job trigger HTTP status for ${job.handlerName}`
  ).toBe(200)
  expect(
    isBusinessOk(triggerPayloadResponse.payload),
    `job trigger code ${triggerPayloadResponse.payload?.code}: ${triggerPayloadResponse.payload?.msg || ''}`
  ).toBe(true)
  evidence.activationJob = {
    jobId: job.id,
    jobName: job.name,
    handlerName: job.handlerName,
    triggered: true,
    triggerMode: 'api'
  }
}

async function submitAndApproveOldViewAccess(
  page,
  browser,
  headers,
  oldCertificateRow,
  oldRow,
  approverCandidates,
  evidence
) {
  await oldCertificateRow.getByRole('button', { name: '申请查看' }).first().click()
  await expect(page.locator('[data-testid="registration-certificate-detail-page"]')).toBeVisible({
    timeout: 60000
  })
  await expect(page.locator('[data-testid="registration-certificate-access-request-action"]')).toBeVisible({
    timeout: 60000
  })
  const accessPanel = page.locator('[data-testid="registration-certificate-access-request-action"]').first()
  await expect(accessPanel.getByText('查看旧证')).toBeVisible({ timeout: 30000 })
  const accessResponsePromise = page.waitForResponse(
    (response) =>
      responsePathMatches(response, '/admin-api/dcc/registration-certificates/access-requests') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await accessPanel.getByRole('button', { name: '提交访问申请' }).click()
  const accessResponse = await accessResponsePromise
  const accessPayload = await readJsonResponse(accessResponse)
  expect(accessResponse.ok(), `old view access HTTP status ${accessResponse.status()}`).toBe(true)
  expect(
    isBusinessOk(accessPayload),
    `old view access code ${accessPayload.code}: ${accessPayload.msg || ''}`
  ).toBe(true)
  expect(accessPayload.data, 'old view access request id must be returned').toBeTruthy()
  const requestId = accessPayload.data
  const accessBoundStatus = await waitForStatus(page, headers, requestId, 'BPM_BOUND')
  expect(accessBoundStatus.bpmProcessInstanceId, 'old view access BPM process id').toBeTruthy()
  await approveRequestInApprovalCenter(
    browser,
    {
      requestId,
      bpmProcessInstanceId: accessBoundStatus.bpmProcessInstanceId
    },
    'old view access',
    approverCandidates,
    evidence
  )
  const approvedStatus = await waitForStatus(page, headers, requestId, 'APPROVED')
  const grants = Array.isArray(approvedStatus.grants) ? approvedStatus.grants : []
  const oldViewGrant = grants.find(
    (grant) => grant.status === 'ACTIVE' && grant.grantType === 'VIEW_OLD_CERTIFICATE'
  )
  expect(oldViewGrant, 'approved old view access must create an active old certificate view grant').toBeTruthy()
  evidence.oldViewAccess = {
    requestId,
    idempotencyKey: accessResponse.request().headers()['idempotency-key'],
    processInstanceId: accessBoundStatus.bpmProcessInstanceId,
    grantId: oldViewGrant.grantId,
    certificateId: oldRow.certificateId,
    versionId: oldRow.versionId
  }
}

test.describe('registration certificate renewal lifecycle real path', () => {
  test('initial upload and renewal are approved, activated, moved to old index and opened by old version', async ({
    page,
    browser
  }, testInfo) => {
    test.setTimeout(420000)
    expect(config.runKey, 'REG_CERT_E2E_RUN_KEY must be explicit for task-owned data').toMatch(
      /^[A-Za-z0-9][A-Za-z0-9._-]{2,80}$/
    )

    const evidence = {
      status: 'RUNNING',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      runKey: config.runKey,
      approverCandidates: [],
      approvals: [],
      failedResponses: [],
      pageErrors: []
    }
    page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
    page.on('response', (response) => {
      if (response.status() >= 400) {
        evidence.failedResponses.push({
          method: response.request().method(),
          path: new URL(response.url()).pathname,
          status: response.status()
        })
      }
    })

    try {
      await login(page, { username: config.username, password: config.password })
      const headers = await buildAuthHeaders(page)
      const permission = await getBusinessData(page, headers, '/admin-api/system/auth/get-permission-info')
      const permissionText = JSON.stringify(permission || {})
      expect(permissionText).toContain('dcc:registration-certificate:upload:create')
      expect(permissionText).toContain('dcc:registration-certificate:renewal:upload')
      expect(permissionText).toContain('infra:job:trigger')
      const approverCandidates = await getApproverCandidates(page, headers)
      evidence.approverCandidates = approverCandidates.map((candidate) => candidate.username)
      for (const candidate of approverCandidates) {
        await updateUserPassword(page, headers, candidate.id, APPROVER_PASSWORD)
      }

      let initialUpload = {
        certificateNo: initialCertificateNo(),
        requestId: null,
        reusedExistingCurrent: false
      }
      let currentRow = await findCertificateRow(
        page,
        headers,
        '/admin-api/dcc/registration-certificates/page',
        initialUpload.certificateNo,
        (row) => row.status === 'CURRENT' && row.versionNo === 1
      )
      if (currentRow) {
        initialUpload.reusedExistingCurrent = true
        evidence.initialUpload = {
          certificateNo: initialUpload.certificateNo,
          requestId: null,
          reusedExistingCurrent: true
        }
      } else {
        initialUpload = await submitInitialUpload(page, evidence, testInfo)
        const uploadBoundStatus = await waitForStatus(page, headers, initialUpload.requestId, 'BPM_BOUND')
        expect(uploadBoundStatus.bpmProcessInstanceId, 'initial upload BPM process id').toBeTruthy()
        await approveRequestInApprovalCenter(
          browser,
          {
            requestId: initialUpload.requestId,
            bpmProcessInstanceId: uploadBoundStatus.bpmProcessInstanceId
          },
          'initial upload',
          approverCandidates,
          evidence
        )
        await waitForStatus(page, headers, initialUpload.requestId, 'APPROVED')
        currentRow = await waitForCertificateRow(
          page,
          headers,
          '/admin-api/dcc/registration-certificates/page',
          initialUpload.certificateNo,
          (row) => row.status === 'CURRENT' && row.versionNo === 1
        )
      }
      evidence.currentAfterInitialApproval = {
        certificateId: currentRow.certificateId,
        versionId: currentRow.versionId,
        certificateNo: currentRow.certificateNo,
        status: currentRow.status,
        versionNo: currentRow.versionNo,
        reusedExistingCurrent: initialUpload.reusedExistingCurrent
      }

      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, {
        waitUntil: 'commit',
        timeout: 60000
      })
      await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({
        timeout: 60000
      })
      const renewalUpload = await submitRenewal(page, currentRow, evidence, testInfo)
      const renewalBoundStatus = await waitForStatus(page, headers, renewalUpload.requestId, 'BPM_BOUND')
      expect(renewalBoundStatus.bpmProcessInstanceId, 'renewal BPM process id').toBeTruthy()
      await approveRequestInApprovalCenter(
        browser,
        {
          requestId: renewalUpload.requestId,
          bpmProcessInstanceId: renewalBoundStatus.bpmProcessInstanceId
        },
        'renewal upload',
        approverCandidates,
        evidence
      )
      await waitForStatus(page, headers, renewalUpload.requestId, 'APPROVED')

      await runActivationJobThroughApi(page, evidence)
      const renewalCurrentRow = await waitForCertificateRow(
        page,
        headers,
        '/admin-api/dcc/registration-certificates/page',
        renewalUpload.certificateNo,
        (row) =>
          row.status === 'CURRENT' &&
          row.versionNo === 2 &&
          toIsoDate(row.effectiveDate) === config.businessDate &&
          toIsoDate(row.expiryDate) === RENEWAL_EXPIRY_DATE
      )
      const oldRow = await waitForCertificateRow(
        page,
        headers,
        '/admin-api/dcc/registration-certificates/old-index/page',
        initialUpload.certificateNo,
        (row) => row.status === 'OLD' && toIsoDate(row.expiryDate) === INITIAL_EXPIRY_DATE
      )
      const renewalDetail = await getBusinessData(
        page,
        headers,
        `/admin-api/dcc/registration-certificates/${renewalCurrentRow.certificateId}`,
        { versionId: renewalCurrentRow.versionId }
      )
      evidence.afterActivation = {
        current: {
          certificateId: renewalCurrentRow.certificateId,
          versionId: renewalCurrentRow.versionId,
          certificateNo: renewalCurrentRow.certificateNo,
          status: renewalCurrentRow.status,
          versionNo: renewalCurrentRow.versionNo,
          effectiveDate: toIsoDate(renewalCurrentRow.effectiveDate),
          expiryDate: toIsoDate(renewalCurrentRow.expiryDate),
          classification: renewalDetail.classification
        },
        old: {
          certificateId: oldRow.certificateId,
          versionId: oldRow.versionId,
          certificateNo: oldRow.certificateNo,
          status: oldRow.status,
          versionNo: oldRow.versionNo,
          expiryDate: toIsoDate(oldRow.expiryDate)
        }
      }
      expect(renewalDetail.certificateNo).toBe(renewalUpload.certificateNo)
      expect(renewalDetail.versionId).toBe(renewalCurrentRow.versionId)
      expect(renewalDetail.classification).toBe('III类')

      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, {
        waitUntil: 'commit',
        timeout: 60000
      })
      await page.getByRole('tab', { name: '老证' }).click()
      await expect(page.locator('[data-testid="registration-certificate-old-index"]')).toBeVisible({
        timeout: 60000
      })
      await addAndApplyTextFilter(
        page,
        OLD_TAB_SELECTOR,
        '注册证编号',
        initialUpload.certificateNo,
        '/admin-api/dcc/registration-certificates/old-index/page',
        evidence,
        'oldFilter'
      )
      const oldTab = page.locator(OLD_TAB_SELECTOR)
      await waitForTableIdle(oldTab)
      const oldCertificateRow = oldTab
        .locator('.el-table__body-wrapper tbody tr')
        .filter({ hasText: initialUpload.certificateNo })
        .first()
      await expect(oldCertificateRow).toBeVisible({ timeout: 60000 })
      await expect(oldCertificateRow.getByRole('button', { name: '申请查看' }).first()).toBeVisible()
      await submitAndApproveOldViewAccess(
        page,
        browser,
        headers,
        oldCertificateRow,
        oldRow,
        approverCandidates,
        evidence
      )

      const detailResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes(`/admin-api/dcc/registration-certificates/${oldRow.certificateId}`) &&
          response.url().includes(`versionId=${oldRow.versionId}`) &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, {
        waitUntil: 'commit',
        timeout: 60000
      })
      await page.getByRole('tab', { name: '老证' }).click()
      await expect(page.locator('[data-testid="registration-certificate-old-index"]')).toBeVisible({
        timeout: 60000
      })
      await addAndApplyTextFilter(
        page,
        OLD_TAB_SELECTOR,
        '注册证编号',
        initialUpload.certificateNo,
        '/admin-api/dcc/registration-certificates/old-index/page',
        evidence,
        'oldFilterAfterGrant'
      )
      const grantedOldCertificateRow = page
        .locator(OLD_TAB_SELECTOR)
        .locator('.el-table__body-wrapper tbody tr')
        .filter({ hasText: initialUpload.certificateNo })
        .first()
      await expect(grantedOldCertificateRow).toBeVisible({ timeout: 60000 })
      await grantedOldCertificateRow.getByRole('button', { name: '详情' }).first().click()
      await expect(page.locator('[data-testid="registration-certificate-detail-page"]')).toBeVisible({
        timeout: 60000
      })
      const oldDetailPayload = await readJsonResponse(await detailResponsePromise)
      expect(
        isBusinessOk(oldDetailPayload),
        `old detail code ${oldDetailPayload.code}: ${oldDetailPayload.msg || ''}`
      ).toBe(true)
      expect(oldDetailPayload.data?.certificateNo).toBe(initialUpload.certificateNo)
      expect(oldDetailPayload.data?.versionId).toBe(oldRow.versionId)
      expect(oldDetailPayload.data?.status).toBe('OLD')
      await expect(page.locator('[data-testid="registration-certificate-detail-page"]')).toContainText(
        `已失效，失效日期 ${INITIAL_EXPIRY_DATE}`
      )
      expect(new URL(page.url()).searchParams.get('versionId')).toBe(String(oldRow.versionId))
      expect(evidence.pageErrors).toEqual([])
      const unexpectedFailedResponses = evidence.failedResponses.filter(
        (failure) => !(failure.method === 'GET' && failure.status === 502 && /^\/user\/avatar\//.test(failure.path))
      )
      expect(unexpectedFailedResponses).toEqual([])

      evidence.status = 'PASS'
      writeResult(evidence)
    } catch (error) {
      evidence.status = 'FAIL'
      evidence.error = error.stack || error.message
      fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
      await page.screenshot({
        path: path.join(ARTIFACT_DIR, 'registration-certificate-renewal-lifecycle-failed.png'),
        fullPage: true
      }).catch(() => undefined)
      writeResult(evidence)
      throw error
    }
  })
})
