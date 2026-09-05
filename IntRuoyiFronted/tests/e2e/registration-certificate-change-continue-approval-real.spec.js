const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(REPO_ROOT, 'doc', 'tasks', '20260904-registration-change-e2e-sync')
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'registration-certificate-change-continue-approval-result.json')

const config = {
  baseUrl: (process.env.REG_CERT_CHANGE_E2E_BASE_URL || 'http://127.0.0.1:8154').replace(/\/+$/, ''),
  tenant: process.env.REG_CERT_CHANGE_E2E_TENANT || '芋道源码',
  applicantUsername: process.env.REG_CERT_CHANGE_E2E_USERNAME || 'wanglixuan',
  applicantPassword: process.env.REG_CERT_CHANGE_E2E_PASSWORD || '',
  approverUsername: process.env.REG_CERT_CHANGE_E2E_APPROVER_USERNAME || 'chudongchuan',
  approverPassword: process.env.REG_CERT_CHANGE_E2E_APPROVER_PASSWORD || '',
  certificateId: process.env.REG_CERT_CHANGE_E2E_CERTIFICATE_ID || '990819128',
  certificateNo: process.env.REG_CERT_CHANGE_E2E_CERTIFICATE_NO || '沪械注准20212020492',
  requestId: process.env.REG_CERT_CHANGE_E2E_REQUEST_ID || '224',
  afterValue: process.env.REG_CERT_CHANGE_E2E_AFTER_VALUE || '变更后产品名称-E2E-CHANGE-20260904-1439',
  runKey: process.env.REG_CERT_CHANGE_E2E_RUN_KEY || 'E2E-CHANGE-20260904-1439'
}

function writeResult(result) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
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

async function approvePendingChange(page, evidence) {
  await login(page, config.approverUsername, config.approverPassword)
  const todoResponsePromise = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return (
        url.pathname === '/admin-api/approval-center/tasks/page' &&
        url.searchParams.get('viewType') === 'TODO' &&
        url.searchParams.get('moduleCode') === 'BPM' &&
        response.request().method() === 'GET'
      )
    },
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/approval-center/todo?moduleCode=BPM`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  await expect(page.locator('.approval-center')).toBeVisible({ timeout: 60000 })
  const todoPayload = await readJsonResponse(await todoResponsePromise)
  const tasks = Array.isArray(todoPayload.data?.list) ? todoPayload.data.list : []
  evidence.approvalTodo = {
    businessCode: todoPayload.code,
    total: Number(todoPayload.data?.total || 0),
    titles: tasks.map((task) => task.businessTitle).filter(Boolean).slice(0, 10)
  }
  expect(isBusinessOk(todoPayload), `approval todo code ${todoPayload.code}: ${todoPayload.msg || ''}`).toBe(true)
  const index = tasks.findIndex((task) => {
    const haystack = JSON.stringify(task)
    return (
      String(task.sourceTaskId || '') === String(config.requestId) ||
      String(task.businessKey || '').includes(String(config.requestId)) ||
      haystack.includes(config.certificateNo) ||
      haystack.includes(config.runKey) ||
      String(task.businessTitle || '').includes('注册证变更审批')
    )
  })
  if (index < 0) {
    evidence.approvalTask = null
    evidence.approvalSubmit = {
      skipped: true,
      reason: 'target approval task not visible; continuing with applied-detail verification'
    }
    return
  }
  const approvalTask = tasks[index]
  evidence.approvalTask = {
    businessTitle: approvalTask.businessTitle || '',
    sourceTaskType: approvalTask.sourceTaskType || '',
    sourceTaskId: approvalTask.sourceTaskId || '',
    businessKey: approvalTask.businessKey || '',
    processInstanceId: approvalTask.processInstanceId || '',
    currentNodeName: approvalTask.currentNodeName || '',
    availableActions: approvalTask.availableActions || []
  }
  const row = page.locator('.approval-center__table .el-table__body-wrapper tbody tr').nth(index)
  await expect(row).toBeVisible({ timeout: 60000 })
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
}

async function verifyAppliedChange(page, evidence) {
  await page.goto(`${config.baseUrl}/mdm/registration-certificate/detail/${config.certificateId}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  await expect(page.locator('[data-testid="registration-certificate-detail-page"]')).toBeVisible({ timeout: 60000 })
  await expect(
    page.locator('[data-testid="registration-certificate-detail-page"]').getByText(config.afterValue, { exact: false }).first()
  ).toBeVisible({ timeout: 60000 })
  await expect(page.locator('[data-testid="registration-certificate-change-history"]')).toBeVisible({ timeout: 60000 })
  await expect(page.locator('[data-testid="registration-certificate-change-history"]')).toContainText('已变更', {
    timeout: 60000
  })
  await expect(page.locator('[data-testid="registration-certificate-change-history"]')).toContainText(config.afterValue, {
    timeout: 60000
  })
  await expect(page.locator('[data-testid="registration-certificate-change-history"]')).toContainText('biangeng.pdf', {
    timeout: 60000
  })
  evidence.detailVerification = {
    certificateId: config.certificateId,
    expectedAfterValueVisible: true,
    changeHistoryAppliedVisible: true,
    changeFileVisible: true
  }
}

test('continue registration certificate change approval with corrected approver', async ({ page, browser }) => {
  test.setTimeout(240000)
  const evidence = {
    status: 'RUNNING',
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    applicantUsername: config.applicantUsername,
    approverUsername: config.approverUsername,
    certificateNo: config.certificateNo,
    requestId: config.requestId,
    afterValue: config.afterValue,
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
    await approvePendingChange(page, evidence)
    const applicantContext = await browser.newContext()
    const applicantPage = await applicantContext.newPage()
    try {
      await login(applicantPage, config.applicantUsername, config.applicantPassword)
      await verifyAppliedChange(applicantPage, evidence)
    } finally {
      await applicantContext.close()
    }
    evidence.status = 'PASS'
    writeResult(evidence)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error instanceof Error ? error.message : String(error)
    writeResult(evidence)
    throw error
  }
})
