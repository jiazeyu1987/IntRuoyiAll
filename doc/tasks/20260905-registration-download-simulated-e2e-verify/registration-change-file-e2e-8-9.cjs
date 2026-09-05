const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const TASK_DIR = __dirname
const REPO_ROOT = path.resolve(TASK_DIR, '..', '..', '..')
const FRONTEND_ROOT = path.join(REPO_ROOT, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(FRONTEND_ROOT, 'package.json'))
const { chromium } = frontendRequire('playwright')

const BASE_URL = (process.env.REG_CERT_E2E_BASE_URL || 'http://127.0.0.1:8158').replace(/\/+$/, '')
const TENANT = process.env.REG_CERT_E2E_TENANT || '芋道源码'
const PREPARE_ONLY = process.env.REG_CERT_E2E_PREPARE_ONLY === '1'
const ACCEPTANCE = path.join(REPO_ROOT, 'e2e_test', 'registration', 'download', 'registration-certificate-download-e2e-acceptance.md')

function readPassword(label) {
  const doc = fs.readFileSync(ACCEPTANCE, 'utf8')
  const pattern = label === 'manager'
    ? /注册部经理 B：`chudongchuan`。密码\s*([^\r\n]+)/
    : /普通用户 C：`wanglixuan`。\s*密码\s*([^\r\n]+)/
  const match = doc.match(pattern)
  if (!match) throw new Error(`Missing ${label} password in acceptance document`)
  return match[1].trim()
}

const USER_PASSWORD = process.env.REG_CERT_E2E_USER_PASSWORD || readPassword('user')
const MANAGER_PASSWORD = process.env.REG_CERT_E2E_MANAGER_PASSWORD || readPassword('manager')

const CHANGE_FILE = path.join(REPO_ROOT, 'e2e_test', 'registration', 'biangeng', 'biangeng.pdf')
if (!fs.existsSync(CHANGE_FILE)) throw new Error(`Missing change approval file: ${CHANGE_FILE}`)

const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts', 'change-file-e2e-8-9')
const SCREENSHOT_DIR = path.join(ARTIFACT_DIR, 'screenshots')
const DOWNLOAD_DIR = path.join(ARTIFACT_DIR, 'downloads')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'result.json')
fs.mkdirSync(SCREENSHOT_DIR, { recursive: true })
fs.mkdirSync(DOWNLOAD_DIR, { recursive: true })

const runId = `E2E-CHANGE-${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`
const today = new Date().toISOString().slice(0, 10)
const accounts = {
  applicant: { username: 'wanglixuan', password: USER_PASSWORD },
  user: { username: 'wanglixuan', password: USER_PASSWORD },
  manager: { username: 'chudongchuan', password: MANAGER_PASSWORD }
}

const result = {
  status: 'RUNNING',
  baseUrl: BASE_URL,
  tenant: TENANT,
  runId,
  accounts: { applicant: 'wanglixuan', user: 'wanglixuan', manager: 'chudongchuan' },
  preparedChange: null,
  e2e8: { status: 'RUNNING' },
  e2e9: { status: 'BLOCKED', reason: '' },
  failedResponses: [],
  consoleErrors: [],
  pageErrors: []
}

const skipCertificateIds = new Set(
  (process.env.REG_CERT_E2E_SKIP_CERTIFICATE_IDS || '')
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean)
)

function saveResult() {
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function ok(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function readJson(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

async function screenshot(page, name) {
  const file = path.join(SCREENSHOT_DIR, `${name}.png`)
  await page.screenshot({ path: file, fullPage: true })
  return file
}

function watch(page, label) {
  page.on('pageerror', (error) => result.pageErrors.push({ label, message: error.message }))
  page.on('console', (message) => {
    if (message.type() === 'error') result.consoleErrors.push({ label, message: message.text() })
  })
  page.on('response', (response) => {
    if (response.status() >= 400) {
      const url = new URL(response.url())
      result.failedResponses.push({
        label,
        method: response.request().method(),
        path: url.pathname,
        status: response.status()
      })
    }
  })
}

async function login(page, account) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    if (await option.count()) await option.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(account.username)
  await form.locator('input[type="password"]').first().fill(account.password)
  const loginResponse = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const payload = await readJson(await loginResponse)
  if (!ok(payload)) throw new Error(`Login failed for ${account.username}: ${payload.msg || payload.message || payload.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function openCurrentList(page) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/dcc/registration-certificates/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mdm/registration-certificate`, { waitUntil: 'commit', timeout: 60000 })
  await page.locator('[data-testid="registration-certificate-read-page"]').waitFor({ state: 'visible', timeout: 60000 })
  const payload = await readJson(await responsePromise)
  if (!ok(payload)) throw new Error(`Registration page failed: ${payload.msg || payload.code}`)
  return Array.isArray(payload.data?.list) ? payload.data.list : []
}

async function openDetail(page, certificateId) {
  const detailResponse = page.waitForResponse(
    (response) => response.url().includes(`/admin-api/dcc/registration-certificates/${certificateId}`) && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const historyResponse = page.waitForResponse(
    (response) => response.url().includes(`/admin-api/dcc/registration-certificates/${certificateId}/history`) && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mdm/registration-certificate/detail/${certificateId}`, { waitUntil: 'commit', timeout: 60000 })
  await page.locator('[data-testid="registration-certificate-detail-page"]').waitFor({ state: 'visible', timeout: 60000 })
  const detailPayload = await readJson(await detailResponse)
  const historyPayload = await readJson(await historyResponse)
  if (!ok(detailPayload)) throw new Error(`Detail failed: ${detailPayload.msg || detailPayload.code}`)
  if (!ok(historyPayload)) throw new Error(`History failed: ${historyPayload.msg || historyPayload.code}`)
  return { detail: detailPayload.data, history: Array.isArray(historyPayload.data) ? historyPayload.data : [] }
}

async function findChangeableTarget(page) {
  let rows = await openCurrentList(page)
  for (let pageNo = 1; pageNo <= 8; pageNo += 1) {
    const candidates = rows.filter((row) =>
      row.status === 'CURRENT' &&
      row.hasPendingChange === false &&
      row.hasPendingRenewal === false &&
      row.certificateId &&
      !skipCertificateIds.has(String(row.certificateId)) &&
      !String(row.certificateNo || '').includes('E2E-REM') &&
      !String(row.productName || '').includes('注册证提醒E2E'))
    for (const row of candidates) {
      const tableRow = page.locator('.el-table__row').filter({ hasText: String(row.certificateNo || '') }).first()
      const changeButton = tableRow.getByRole('button', { name: /^变更$/ })
      if ((await changeButton.count()) > 0 && await changeButton.isVisible()) {
        return row
      }
    }
    const nextButton = page.locator('.el-pagination button.btn-next').first()
    if (!(await nextButton.count()) || await nextButton.isDisabled()) break
    const nextResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await nextButton.click()
    const payload = await readJson(await nextResponse)
    if (!ok(payload)) throw new Error(`Registration page failed: ${payload.msg || payload.code}`)
    rows = Array.isArray(payload.data?.list) ? payload.data.list : []
  }
  throw new Error('No visible CURRENT registration certificate change entry found after scanning 8 pages')
}

async function submitChange(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'submit-change')
  try {
    await login(page, accounts.applicant)
    const target = await findChangeableTarget(page)
    const tableRow = page.locator('.el-table__row').filter({ hasText: String(target.certificateNo) }).first()
    await tableRow.getByRole('button', { name: /^变更$/ }).click()
    const dialog = page.locator('[data-testid="registration-certificate-change-dialog"]:visible')
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    await dialog.locator('input[placeholder="请选择批准日期"]').fill(today)
    await dialog.locator('input[placeholder="请选择批准日期"]').press('Enter')
    await dialog.locator('.el-form-item').filter({ hasText: '变更内容' }).locator('.el-select').click()
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '产品名称' }).first().click()
    const changedProductName = `${String(target.productName || '注册证产品')}-${runId}`
    await dialog.locator('input[placeholder="变更后的产品名称"]').fill(changedProductName)
    await dialog.getByTestId('registration-certificate-change-approval-file').locator('input[type="file"]').setInputFiles(CHANGE_FILE)
    result.preparedChange = {
      certificateId: target.certificateId,
      certificateNo: target.certificateNo,
      originalProductName: target.productName || '',
      changedProductName,
      approvalDate: today
    }
    result.preparedChange.beforeSubmitScreenshot = await screenshot(page, 'change-submit-dialog')
    const submitResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/') &&
        response.url().includes('/change') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: /^确认$/ }).click()
    const payload = await readJson(await submitResponse)
    if (!ok(payload)) throw new Error(`Change submit failed: ${payload.msg || payload.message || payload.code}`)
    await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '变更已提交审核' }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    result.preparedChange.submit = { httpStatus: 200, payloadCode: payload.code, screenshot: await screenshot(page, 'change-submit-success') }
  } finally {
    await context.close()
  }
}

async function todoIds(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'todo-baseline')
  try {
    await login(page, accounts.manager)
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/approval-center/todo`, { waitUntil: 'commit', timeout: 60000 })
    const payload = await readJson(await responsePromise)
    if (!ok(payload)) throw new Error(`Todo list failed: ${payload.msg || payload.code}`)
    return new Set((payload.data?.list || []).map((task) => task.id))
  } finally {
    await context.close()
  }
}

async function approveLatest(browser, baseline, titleText, password, screenshotPrefix) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, screenshotPrefix)
  try {
    await login(page, accounts.manager)
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/approval-center/todo`, { waitUntil: 'commit', timeout: 60000 })
    const payload = await readJson(await responsePromise)
    if (!ok(payload)) throw new Error(`Approval list failed: ${payload.msg || payload.code}`)
    const tasks = Array.isArray(payload.data?.list) ? payload.data.list : []
    let task = tasks.find((item) => !baseline.has(item.id) && String(item.businessTitle || '').includes(titleText))
    if (!task) task = tasks.find((item) => String(item.businessTitle || '').includes(titleText))
    if (!task) throw new Error(`No approval task found for ${titleText}`)
    await screenshot(page, `${screenshotPrefix}-list`)
    const row = page.locator('.approval-center__table .el-table__row').filter({ hasText: task.businessTitle }).first()
    await row.waitFor({ state: 'visible', timeout: 60000 })
    await row.getByRole('button', { name: /审核|审批/ }).first().click()
    const dialog = page.locator('.approval-center__review-dialog:visible')
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    await screenshot(page, `${screenshotPrefix}-dialog`)
    await dialog.locator('input[type="password"]').fill(password)
    const reviewResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/review') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: '确认审核' }).click()
    const reviewPayload = await readJson(await reviewResponse)
    if (!ok(reviewPayload)) throw new Error(`${titleText} approval failed: ${reviewPayload.msg || reviewPayload.code}`)
    return {
      taskId: task.id,
      processInstanceId: task.processInstanceId || '',
      businessTitle: task.businessTitle || '',
      screenshot: await screenshot(page, `${screenshotPrefix}-success`)
    }
  } finally {
    await context.close()
  }
}

async function locateChangeFile(browser, certificateId) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'locate-change-file')
  try {
    await login(page, accounts.user)
    const opened = await openDetail(page, certificateId)
    const applied = opened.history.find((item) =>
      item.businessFileId &&
      item.fileKind === 'CHANGE_APPROVAL' &&
      item.fileStatus === 'BOUND' &&
      item.changeStatus === 'APPLIED'
    )
    if (!applied) throw new Error('No APPLIED change approval file after manager approval')
    const section = page.getByTestId('registration-certificate-change-history').first()
    const requestButton = section.getByTestId('registration-certificate-change-attachment-request-download').first()
    await requestButton.waitFor({ state: 'visible', timeout: 60000 })
    const beforeText = await requestButton.innerText()
    result.e2e8.beforeRequest = {
      businessFileId: applied.businessFileId,
      fileName: applied.originalFileName || '',
      buttonText: beforeText,
      screenshot: await screenshot(page, 'e2e8-before-request')
    }
    return { businessFileId: applied.businessFileId, fileName: applied.originalFileName || '' }
  } finally {
    await context.close()
  }
}

async function requestChangeFileDownload(browser, certificateId) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'e2e8-request')
  try {
    await login(page, accounts.user)
    await openDetail(page, certificateId)
    const section = page.getByTestId('registration-certificate-change-history').first()
    const button = section.getByTestId('registration-certificate-change-attachment-request-download').first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    const requestResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/access-requests') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await button.click()
    const payload = await readJson(await requestResponse)
    if (!ok(payload)) throw new Error(`Change file download request failed: ${payload.msg || payload.code}`)
    await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '已申请下载' }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    result.e2e8.request = {
      requestId: payload.data,
      screenshot: await screenshot(page, 'e2e8-request-pending')
    }
  } finally {
    await context.close()
  }
}

async function downloadChangeFile(browser, certificateId) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  watch(page, 'e2e8-download')
  try {
    await login(page, accounts.user)
    await openDetail(page, certificateId)
    const section = page.getByTestId('registration-certificate-change-history').first()
    const button = section.getByTestId('registration-certificate-change-attachment-download').first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    await screenshot(page, 'e2e8-after-approval')
    const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
    await button.click()
    const download = await downloadPromise
    const suggestedFilename = download.suggestedFilename()
    const savePath = path.join(DOWNLOAD_DIR, suggestedFilename)
    await download.saveAs(savePath)
    const size = fs.statSync(savePath).size
    if (size <= 0) throw new Error('Downloaded change approval file is empty')
    if (!suggestedFilename.includes('变更文件')) throw new Error(`Downloaded filename missing 变更文件 marker: ${suggestedFilename}`)
    result.e2e8.download = { suggestedFilename, savePath, size, hasChangeMarker: true }
    result.e2e8.status = 'PASS'
  } finally {
    await context.close()
  }
}

async function checkExpiredPathAvailability(browser, certificateId) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'e2e9-check')
  try {
    await login(page, accounts.user)
    await openDetail(page, certificateId)
    const section = page.getByTestId('registration-certificate-change-history').first()
    const downloadButtonCount = await section.getByTestId('registration-certificate-change-attachment-download').count()
    const requestButtonCount = await section.getByTestId('registration-certificate-change-attachment-request-download').count()
    result.e2e9.observedImmediatelyAfterApproval = {
      downloadButtonCount,
      requestButtonCount,
      screenshot: await screenshot(page, 'e2e9-immediate-state')
    }
    result.e2e9.reason =
      '授权过期需要超过 24 小时后的同一授权状态。当前真实前端没有可用的授权时间推进或授权过期模拟入口；项目规则禁止用 API/SQL 回填或回拨授权时间，因此本轮不能把 E2E-9 伪造成 PASS。'
  } finally {
    await context.close()
  }
}

async function main() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  try {
    const changeBaseline = await todoIds(browser)
    await submitChange(browser)
    result.preparedChange.approval = await approveLatest(browser, changeBaseline, '注册证变更审批', accounts.manager.password, 'change-approval')
    const file = await locateChangeFile(browser, result.preparedChange.certificateId)
    result.e2e8.target = file
    if (PREPARE_ONLY) {
      result.e2e8.status = 'NOT_RUN'
      result.e2e9.status = 'NOT_RUN'
      result.status = 'PREPARED_ONLY'
      return
    }
    const requestBaseline = await todoIds(browser)
    await requestChangeFileDownload(browser, result.preparedChange.certificateId)
    result.e2e8.approval = await approveLatest(browser, requestBaseline, '注册证下载审批', accounts.manager.password, 'e2e8-download-approval')
    result.e2e8.authorizedAt = new Date().toISOString()
    result.e2e8.expiresAtExpected = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString()
    await downloadChangeFile(browser, result.preparedChange.certificateId)
    await checkExpiredPathAvailability(browser, result.preparedChange.certificateId)
    result.status = result.e2e8.status === 'PASS' ? 'PARTIAL_PASS' : 'FAIL'
  } catch (error) {
    result.status = 'FAIL'
    result.error = error.stack || error.message
    throw error
  } finally {
    await browser.close()
    saveResult()
  }
}

main()
