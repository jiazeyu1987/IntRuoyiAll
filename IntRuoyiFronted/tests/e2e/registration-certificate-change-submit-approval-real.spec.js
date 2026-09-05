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
  approverUsername: process.env.REG_CERT_CHANGE_E2E_APPROVER_USERNAME || 'chudongqian',
  approverPassword: process.env.REG_CERT_CHANGE_E2E_APPROVER_PASSWORD || process.env.REG_CERT_CHANGE_E2E_PASSWORD || '',
  certificateNo: process.env.REG_CERT_CHANGE_E2E_CERTIFICATE_NO || '沪械注准20212020492',
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

async function submitChange(page, evidence) {
  await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit', timeout: 60000 })
  await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({ timeout: 60000 })
  const row = page.locator('.el-table:visible .el-table__row').filter({ hasText: config.certificateNo }).first()
  await expect(row, `target certificate ${config.certificateNo} must be visible`).toBeVisible({ timeout: 60000 })
  await expect(row.getByRole('button', { name: '变更' })).toBeVisible({ timeout: 60000 })
  await row.getByRole('button', { name: '变更' }).click()
  const dialog = page.locator('[data-testid="registration-certificate-change-dialog"]')
  await expect(dialog).toBeVisible({ timeout: 60000 })
  const form = page.locator('[data-testid="registration-certificate-change-form"]')
  await form.locator('input[placeholder="请选择批准日期"]').fill(config.approvalDate)
  await form.locator('.el-form-item').filter({ hasText: '变更内容' }).locator('.el-select').first().click()
  await selectVisibleOption(page, '产品名称')
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
    certificateNo: config.certificateNo,
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
    await page.goto(`${config.baseUrl}/approval-center/todo?keyword=${encodeURIComponent(config.certificateNo)}`, {
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
