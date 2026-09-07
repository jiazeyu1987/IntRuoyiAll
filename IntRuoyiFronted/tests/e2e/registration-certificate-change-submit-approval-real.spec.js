const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(REPO_ROOT, 'doc', 'tasks', '20260904-registration-change-e2e-sync')
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'registration-certificate-change-submit-approval-result.json')

const config = {
  baseUrl: (process.env.REG_CERT_CHANGE_E2E_BASE_URL || 'http://127.0.0.1:8154').replace(/\/+$/, ''),
  tenant: process.env.REG_CERT_CHANGE_E2E_TENANT || '芋道源码',
  applicantUsername: process.env.REG_CERT_CHANGE_E2E_USERNAME || 'wanglixuan',
  applicantPassword: process.env.REG_CERT_CHANGE_E2E_PASSWORD || '',
  approverUsername: process.env.REG_CERT_CHANGE_E2E_APPROVER_USERNAME || 'chudongchuan',
  approverPassword: process.env.REG_CERT_CHANGE_E2E_APPROVER_PASSWORD || process.env.REG_CERT_CHANGE_E2E_PASSWORD || '',
  certificateNo: process.env.REG_CERT_CHANGE_E2E_CERTIFICATE_NO || '',
  approvalDate: process.env.REG_CERT_CHANGE_E2E_APPROVAL_DATE || '2026-09-04',
  changeFilePath: process.env.REG_CERT_CHANGE_E2E_FILE || path.join(REPO_ROOT, 'e2e_test', 'registration', 'biangeng', 'biangeng.pdf'),
  runKey: process.env.REG_CERT_CHANGE_E2E_RUN_KEY || `E2E-CHANGE-${Date.now()}`
}

function writeResult(result) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function optionTextPattern(text) {
  return new RegExp(`^\\s*${String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`)
}

async function readJsonResponse(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

async function login(page, username, password) {
  expect(password, `password for ${username} must be provided without logging it`).toBeTruthy()
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first().click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginPayload = await readJsonResponse(await loginResponsePromise)
  expect(isBusinessOk(loginPayload), `login code ${loginPayload.code}: ${loginPayload.msg || ''}`).toBe(true)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
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
  const globalOptions = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
  if ((await globalOptions.count()) === 0) {
    await select.locator('.el-select__wrapper, .el-select').first().click({ timeout: 30000, force: true })
  }
  const controls = await combobox.getAttribute('aria-controls')
  let options = controls
    ? page.locator(`[id="${controls}"]:visible .el-select-dropdown__item:not(.is-disabled)`)
    : page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
  try {
    await options.first().waitFor({ state: 'visible', timeout: 2000 })
  } catch (error) {
    options = globalOptions
    await options.first().waitFor({ state: 'visible', timeout: 30000 })
  }
  const optionTexts = []
  let matchedIndex = -1
  const count = await options.count()
  for (let index = 0; index < count; index += 1) {
    const text = (await options.nth(index).innerText()).trim()
    optionTexts.push(text)
    if (text === optionText && matchedIndex < 0) matchedIndex = index
  }
  expect(matchedIndex, `option ${optionText} must exist in ${optionTexts.join('/')}`).toBeGreaterThanOrEqual(0)
  await options.nth(matchedIndex).click({ timeout: 30000, force: true })
}

function isPreferredChangeCandidate(certificate) {
  if (!certificate || certificate.status !== 'CURRENT') {
    return false
  }
  if (certificate.hasPendingChange === true) {
    return false
  }
  if (certificate.hasProjectCode === false || certificate.hasRegistrationFile === false) {
    return false
  }
  return true
}

async function submitChange(page, evidence) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/registration-certificates/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit', timeout: 60000 })
  await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({ timeout: 60000 })
  const pagePayload = await readJsonResponse(await pageResponsePromise)
  expect(isBusinessOk(pagePayload), `list code ${pagePayload.code}: ${pagePayload.msg || ''}`).toBe(true)
  const dataRows = Array.isArray(pagePayload.data?.list) ? pagePayload.data.list : []
  if (config.certificateNo) {
    await applyCertificateNoFilter(page, config.certificateNo)
  }
  const rows = page.locator('.registration-certificate-current-table .el-table__body-wrapper .el-table__row')
  await expect(rows.first(), 'current registration certificate rows must render').toBeVisible({ timeout: 60000 })
  const count = await rows.count()
  let row = null
  let selectedCertificateNo = ''
  for (let index = 0; index < count; index += 1) {
    const candidate = rows.nth(index)
    const candidateCertificateNo = (await candidate.locator('.el-table__cell').first().innerText()).trim()
    if (config.certificateNo && candidateCertificateNo !== config.certificateNo) {
      continue
    }
    const changeButton = candidate.getByRole('button', { name: '变更' })
    if ((await changeButton.count()) === 0) {
      continue
    }
    const certificate = dataRows.find((item) => String(item.certificateNo || '').trim() === candidateCertificateNo)
    if (!config.certificateNo && !isPreferredChangeCandidate(certificate)) {
      continue
    }
    row = candidate
    selectedCertificateNo = candidateCertificateNo
    break
  }
  expect(row, config.certificateNo ? `target certificate ${config.certificateNo} must be changeable` : 'a changeable current certificate must exist').toBeTruthy()
  await row.getByRole('button', { name: '变更' }).click()
  const dialog = page.locator('[data-testid="registration-certificate-change-dialog"]')
  await expect(dialog).toBeVisible({ timeout: 60000 })
  const form = page.locator('[data-testid="registration-certificate-change-form"]')
  await expect(form.locator('.el-loading-mask:visible')).toHaveCount(0, { timeout: 60000 })
  await form.locator('input[placeholder="请选择批准日期"]').fill(config.approvalDate)
  await selectOptionFromSelect(page, form.locator('[data-change-type-values]').first(), '产品名称')
  await page.keyboard.press('Escape')
  const afterValue = `变更后产品名称-${config.runKey}`
  const productNameInput = form.locator('input[placeholder="变更后的产品名称"]').first()
  await expect(productNameInput, 'selecting 产品名称 must render the after-value input').toBeVisible({
    timeout: 30000
  })
  await productNameInput.fill(afterValue)
  await form.locator('input[type="file"]').setInputFiles(config.changeFilePath)
  const changeResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/registration-certificates/') &&
      response.url().includes('/changes') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '确认' }).click()
  const changeResponse = await changeResponsePromise
  const changePayload = await readJsonResponse(changeResponse)
  evidence.changeSubmit = {
    certificateNo: selectedCertificateNo,
    selectedFields: ['产品名称'],
    afterValue,
    requestPath: new URL(changeResponse.url()).pathname,
    httpStatus: changeResponse.status(),
    businessCode: changePayload.code,
    message: changePayload.msg || changePayload.message || '',
    requestId: changePayload.data || null
  }
  expect(changeResponse.ok(), `change HTTP status ${changeResponse.status()}`).toBe(true)
  expect(isBusinessOk(changePayload), `change code ${changePayload.code}: ${changePayload.msg || ''}`).toBe(true)
  await expect(dialog).toBeHidden({ timeout: 30000 })
  return changePayload.data
}

async function applyCertificateNoFilter(page, certificateNo) {
  const filter = page.locator('[data-testid="registration-certificate-current-tab"] .table-multi-filter').first()
  await filter.waitFor({ state: 'visible', timeout: 60000 })
  if ((await filter.locator('.table-multi-filter__condition-row:visible').count()) === 0) {
    await filter.getByRole('button', { name: '新增筛选条件' }).click()
  }
  await filter.locator('.table-multi-filter__field-select').click()
  await selectVisibleOption(page, '注册证编号')
  await filter.locator('.table-multi-filter-field__value input.el-input__inner').first().fill(certificateNo)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/registration-certificates/page') &&
      response.url().includes(`certificateNo=${encodeURIComponent(certificateNo)}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await filter.getByRole('button', { name: '查询' }).click()
  const payload = await readJsonResponse(await responsePromise)
  expect(isBusinessOk(payload), `certificate filter code ${payload.code}: ${payload.msg || ''}`).toBe(true)
}

async function approveInApprovalCenter(browser, requestId, evidence) {
  const context = await browser.newContext()
  const page = await context.newPage()
  try {
    await login(page, config.approverUsername, config.approverPassword)
    const todoResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/approval-center/tasks/page') &&
        response.url().includes('viewType=TODO') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${config.baseUrl}/approval-center/todo?keyword=${encodeURIComponent(evidence.changeSubmit?.certificateNo || '')}`, {
      waitUntil: 'commit',
      timeout: 60000
    })
    await expect(page.locator('.approval-center')).toBeVisible({ timeout: 60000 })
    const todoPayload = await readJsonResponse(await todoResponsePromise)
    const tasks = Array.isArray(todoPayload.data?.list) ? todoPayload.data.list : []
    evidence.approvalTodo = {
      httpBusinessCode: todoPayload.code,
      total: Number(todoPayload.data?.total || 0),
      titles: tasks.map((task) => task.businessTitle).filter(Boolean).slice(0, 5)
    }
    expect(isBusinessOk(todoPayload), `approval todo code ${todoPayload.code}: ${todoPayload.msg || ''}`).toBe(true)
    const index = tasks.findIndex((task) => String(task.sourceTaskId || '') === String(requestId) || String(task.businessTitle || '').includes('注册证变更审批'))
    expect(index, `change approval task for request ${requestId} must be visible`).toBeGreaterThanOrEqual(0)
    const row = page.locator('.approval-center__table .el-table__row').nth(index)
    await row.getByRole('button', { name: /审核|审批/ }).first().click()
    const dialog = page.locator('.approval-center__review-dialog:visible')
    await expect(dialog).toBeVisible({ timeout: 30000 })
    await dialog.locator('input[type="password"]').fill(config.approverPassword)
    const reviewResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/review') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: '确认审核' }).click()
    const reviewResponse = await reviewResponsePromise
    const reviewPayload = await readJsonResponse(reviewResponse)
    evidence.approvalSubmit = {
      requestPath: new URL(reviewResponse.url()).pathname,
      httpStatus: reviewResponse.status(),
      businessCode: reviewPayload.code,
      message: reviewPayload.msg || reviewPayload.message || '',
      result: reviewPayload.data
    }
    expect(isBusinessOk(reviewPayload), `review code ${reviewPayload.code}: ${reviewPayload.msg || ''}`).toBe(true)
    expect(reviewPayload.data, 'approval result must be true').toBe(true)
  } finally {
    await context.close()
  }
}

test('registration certificate change submit and approval real page path', async ({ page, browser }) => {
  test.setTimeout(300000)
  const evidence = {
    status: 'RUNNING',
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    applicantUsername: config.applicantUsername,
    approverUsername: config.approverUsername,
    certificateNo: config.certificateNo,
    runKey: config.runKey,
    failedResponses: [],
    pageErrors: [],
    consoleErrors: []
  }
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
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
    expect(fs.existsSync(config.changeFilePath), `change file ${config.changeFilePath} must exist`).toBe(true)
    await login(page, config.applicantUsername, config.applicantPassword)
    const requestId = await submitChange(page, evidence)
    await approveInApprovalCenter(browser, requestId, evidence)
    evidence.status = 'PASS'
    writeResult(evidence)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error instanceof Error ? error.message : String(error)
    writeResult(evidence)
    throw error
  }
})
