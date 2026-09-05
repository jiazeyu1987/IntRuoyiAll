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
const ACCEPTANCE = path.join(REPO_ROOT, 'e2e_test', 'registration', 'download', 'registration-certificate-download-e2e-acceptance.md')
const CHANGE_FILE = path.join(REPO_ROOT, 'e2e_test', 'registration', 'biangeng', 'biangeng.pdf')
const RENEWAL_FILE = path.join(REPO_ROOT, 'e2e_test', 'registration', 'yanxu', 'yanxu.pdf')
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts', 'old-combo')
const SCREENSHOT_DIR = path.join(ARTIFACT_DIR, 'screenshots')
const DOWNLOAD_DIR = path.join(ARTIFACT_DIR, 'downloads')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'result.json')

function readPassword(label) {
  const doc = fs.readFileSync(ACCEPTANCE, 'utf8')
  const pattern = label === 'manager'
    ? /注册部经理 B：`chudongchuan`。密码\s*([^\r\n]+)/
    : /普通用户 C：`wanglixuan`。\s*密码\s*([^\r\n]+)/
  const match = doc.match(pattern)
  if (!match) throw new Error(`Missing ${label} password in acceptance document`)
  return match[1].trim()
}

if (!fs.existsSync(CHANGE_FILE)) throw new Error(`Missing change approval file: ${CHANGE_FILE}`)
if (!fs.existsSync(RENEWAL_FILE)) throw new Error(`Missing renewal file: ${RENEWAL_FILE}`)
fs.mkdirSync(SCREENSHOT_DIR, { recursive: true })
fs.mkdirSync(DOWNLOAD_DIR, { recursive: true })

const accounts = {
  applicant: { username: 'wanglixuan', password: readPassword('user') },
  manager: { username: 'chudongchuan', password: readPassword('manager') }
}

function localDate(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const runId = `E2E-OLD-COMBO-${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`
const result = {
  status: 'RUNNING',
  baseUrl: BASE_URL,
  tenant: TENANT,
  runId,
  accounts: { applicant: accounts.applicant.username, manager: accounts.manager.username },
  target: null,
  change: { status: 'RUNNING' },
  renewal: { status: 'RUNNING' },
  e2e8: { status: 'RUNNING' },
  e2e9: { status: 'RUNNING' },
  failedResponses: [],
  consoleErrors: [],
  pageErrors: []
}

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
  if (!ok(payload)) throw new Error(`Registration current list failed: ${payload.msg || payload.code}`)
  return Array.isArray(payload.data?.list) ? payload.data.list : []
}

async function nextCurrentPage(page) {
  const nextButton = page.locator('.el-pagination button.btn-next').first()
  if (!(await nextButton.count()) || await nextButton.isDisabled()) return []
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/dcc/registration-certificates/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await nextButton.click()
  const payload = await readJson(await responsePromise)
  if (!ok(payload)) throw new Error(`Registration next page failed: ${payload.msg || payload.code}`)
  return Array.isArray(payload.data?.list) ? payload.data.list : []
}

async function openDetail(page, certificateId, versionId) {
  const detailPromise = page.waitForResponse(
    (response) => response.url().includes(`/admin-api/dcc/registration-certificates/${certificateId}`) && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const historyPromise = page.waitForResponse(
    (response) => response.url().includes(`/admin-api/dcc/registration-certificates/${certificateId}/history`) && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const url = new URL(`${BASE_URL}/mdm/registration-certificate/detail/${certificateId}`)
  if (versionId) {
    url.searchParams.set('mode', 'old-detail')
    url.searchParams.set('versionId', String(versionId))
  }
  await page.goto(url.toString(), { waitUntil: 'commit', timeout: 60000 })
  await page.locator('[data-testid="registration-certificate-detail-page"]').waitFor({ state: 'visible', timeout: 60000 })
  const detailPayload = await readJson(await detailPromise)
  const historyPayload = await readJson(await historyPromise)
  if (!ok(detailPayload)) throw new Error(`Detail failed: ${detailPayload.msg || detailPayload.code}`)
  if (!ok(historyPayload)) throw new Error(`History failed: ${historyPayload.msg || historyPayload.code}`)
  return { detail: detailPayload.data, history: Array.isArray(historyPayload.data) ? historyPayload.data : [] }
}

async function findBaseTarget(page) {
  let rows = await openCurrentList(page)
  let scanned = 0
  for (let pageNo = 1; pageNo <= 10; pageNo += 1) {
    for (const row of rows) {
      scanned += 1
      if (
        row.status !== 'CURRENT' ||
        row.hasPendingChange ||
        row.hasPendingRenewal ||
        !row.certificateId ||
        String(row.productName || '').length > 90 ||
        String(row.certificateNo || '').length > 90
      ) {
        continue
      }
      const tableRow = page.locator('.el-table__row').filter({ hasText: String(row.certificateNo || '') }).first()
      const changeButton = tableRow.getByRole('button', { name: /^变更$/ })
      const renewalButton = tableRow.getByRole('button', { name: /^延续$/ })
      if (
        (await changeButton.count()) > 0 &&
        (await renewalButton.count()) > 0 &&
        await changeButton.isVisible() &&
        await renewalButton.isVisible()
      ) {
        const detailPage = await page.context().newPage()
        watch(detailPage, `select-detail-${row.certificateId}`)
        try {
          const opened = await openDetail(detailPage, row.certificateId)
          const requestButton = detailPage.getByTestId('registration-certificate-detail-attachment-request-download').first()
          const downloadButton = detailPage.getByTestId('registration-certificate-detail-attachment-download').first()
          const canRequestMain = (await requestButton.count()) > 0 && await requestButton.isVisible()
          const canDownloadMain = (await downloadButton.count()) > 0 && await downloadButton.isVisible()
          const requestText = canRequestMain ? await requestButton.innerText() : ''
          if (!opened.detail?.registrationFileId || !canRequestMain || canDownloadMain || requestText.includes('申请中')) {
            continue
          }
        } finally {
          await detailPage.close()
        }
        result.target = {
          certificateId: row.certificateId,
          versionId: row.versionId,
          rowVersion: row.rowVersion,
          certificateNo: row.certificateNo,
          productName: row.productName,
          classification: row.classification,
          scanned
        }
        return row
      }
    }
    rows = await nextCurrentPage(page)
    if (rows.length === 0) break
  }
  throw new Error(`Scanned ${scanned} current records but found no clean CURRENT row with both change and renewal entries`)
}

async function todoIds(browser, label) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, label)
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

async function approveLatest(browser, baseline, titleText, screenshotPrefix) {
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
    await dialog.locator('input[type="password"]').fill(accounts.manager.password)
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

async function submitChange(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'submit-change')
  try {
    await login(page, accounts.applicant)
    const target = await findBaseTarget(page)
    const row = page.locator('.el-table__row').filter({ hasText: String(target.certificateNo) }).first()
    await row.getByRole('button', { name: /^变更$/ }).click()
    const dialog = page.locator('[data-testid="registration-certificate-change-dialog"]:visible')
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    await dialog.locator('input[placeholder="请选择批准日期"]').fill(localDate(0))
    await dialog.locator('input[placeholder="请选择批准日期"]').press('Enter')
    await dialog.locator('.el-form-item').filter({ hasText: '变更内容' }).locator('.el-select').click()
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '产品名称' }).first().click()
    const changedProductName = `${String(target.productName || '注册证产品')}-${runId}`
    await dialog.locator('input[placeholder="变更后的产品名称"]').fill(changedProductName)
    await dialog.getByTestId('registration-certificate-change-approval-file').locator('input[type="file"]').setInputFiles(CHANGE_FILE)
    result.change.beforeSubmitScreenshot = await screenshot(page, 'change-submit-dialog')
    const submitResponse = page.waitForResponse(
      (response) => response.url().includes(`/admin-api/dcc/registration-certificates/${target.certificateId}/changes`) && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: /^确认$/ }).click()
    const payload = await readJson(await submitResponse)
    if (!ok(payload)) throw new Error(`Change submit failed: ${payload.msg || payload.message || payload.code}`)
    await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '变更已提交审核' }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    result.change.submit = { httpStatus: 200, code: payload.code, screenshot: await screenshot(page, 'change-submit-success') }
  } finally {
    await context.close()
  }
}

async function findCurrentRowByCertificateNo(page, certificateNo) {
  let rows = await openCurrentList(page)
  for (let pageNo = 1; pageNo <= 10; pageNo += 1) {
    const row = rows.find((item) => String(item.certificateNo || '') === String(certificateNo) && item.status === 'CURRENT')
    if (row) return row
    rows = await nextCurrentPage(page)
    if (rows.length === 0) break
  }
  throw new Error(`Current row not found after change approval: ${certificateNo}`)
}

async function submitRenewal(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'submit-renewal')
  try {
    await login(page, accounts.applicant)
    const currentRow = await findCurrentRowByCertificateNo(page, result.target.certificateNo)
    result.renewal.beforeVersionId = currentRow.versionId
    result.renewal.beforeRowVersion = currentRow.rowVersion
    result.renewal.beforeProductName = currentRow.productName
    const row = page.locator('.el-table__row').filter({ hasText: String(currentRow.certificateNo) }).first()
    await row.getByRole('button', { name: /^延续$/ }).click()
    const dialog = page.locator('[data-testid="registration-certificate-renewal-dialog"]:visible')
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    const dateInputs = dialog.locator('input[placeholder="请选择日期"]')
    await dateInputs.nth(0).fill(localDate(0))
    await dateInputs.nth(0).press('Enter')
    await dateInputs.nth(1).fill(localDate(0))
    await dateInputs.nth(1).press('Enter')
    await dateInputs.nth(2).fill(localDate(365))
    await dateInputs.nth(2).press('Enter')
    await dialog.locator('[data-testid="registration-certificate-renewal-file"]').locator('input[type="file"]').setInputFiles(RENEWAL_FILE)
    result.renewal.beforeSubmitScreenshot = await screenshot(page, 'renewal-submit-dialog')
    const submitResponse = page.waitForResponse(
      (response) => response.url().includes(`/admin-api/dcc/registration-certificates/${currentRow.certificateId}/renewals`) && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: /^提交审批$/ }).click()
    const payload = await readJson(await submitResponse)
    if (!ok(payload)) throw new Error(`Renewal submit failed: ${payload.msg || payload.message || payload.code}`)
    await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '延续注册证已提交注册部经理审批' }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    result.renewal.submit = {
      requestId: payload.data,
      approvalDate: localDate(0),
      effectiveDate: localDate(0),
      expiryDate: localDate(365),
      screenshot: await screenshot(page, 'renewal-submit-success')
    }
  } finally {
    await context.close()
  }
}

async function verifyOldIndexAndOpen(page) {
  const oldResponse = page.waitForResponse(
    (response) => response.url().includes('/admin-api/dcc/registration-certificates/old-index/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mdm/registration-certificate`, { waitUntil: 'commit', timeout: 60000 })
  await page.locator('[data-testid="registration-certificate-read-page"]').waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('tab', { name: '老证' }).click()
  const payload = await readJson(await oldResponse)
  if (!ok(payload)) throw new Error(`Old index failed: ${payload.msg || payload.code}`)
  const rows = Array.isArray(payload.data?.list) ? payload.data.list : []
  const row = rows.find((item) =>
    String(item.certificateId) === String(result.target.certificateId) &&
    String(item.versionId) === String(result.renewal.beforeVersionId)
  )
  result.renewal.oldIndex = {
    total: Number(payload.data?.total || rows.length || 0),
    foundTargetOnFirstPage: Boolean(row),
    targetVersionId: result.renewal.beforeVersionId,
    screenshot: await screenshot(page, 'old-index-after-renewal')
  }
  await openDetail(page, result.target.certificateId, result.renewal.beforeVersionId)
  await page.getByTestId('registration-certificate-detail-attachment').first().waitFor({ state: 'visible', timeout: 60000 })
  await screenshot(page, 'old-detail-opened')
}

async function requestFileDownload(page, locator, resultBucket, screenshotPrefix) {
  await locator.waitFor({ state: 'visible', timeout: 60000 })
  const beforeText = await locator.innerText()
  if (beforeText.includes('申请中')) throw new Error(`${screenshotPrefix} is already pending`)
  resultBucket.beforeRequestScreenshot = await screenshot(page, `${screenshotPrefix}-before-request`)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/dcc/registration-certificates/access-requests') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await locator.click()
  const response = await responsePromise
  const payload = await readJson(response)
  if (!ok(payload)) throw new Error(`${screenshotPrefix} request failed: ${payload.msg || payload.code}`)
  await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '已申请下载' }).first()
    .waitFor({ state: 'visible', timeout: 30000 })
  await locator.waitFor({ state: 'visible', timeout: 30000 })
  const pendingText = await locator.innerText()
  if (!pendingText.includes('申请中')) throw new Error(`${screenshotPrefix} request button did not become pending: ${pendingText}`)
  resultBucket.request = {
    requestId: payload.data,
    httpStatus: response.status(),
    pendingText,
    screenshot: await screenshot(page, `${screenshotPrefix}-pending`)
  }
}

async function requestOldRegistrationDownload(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'old-registration-request')
  try {
    await login(page, accounts.applicant)
    await verifyOldIndexAndOpen(page)
    const requestButton = page.getByTestId('registration-certificate-detail-attachment-request-download').first()
    await requestFileDownload(page, requestButton, result.e2e8, 'old-registration')
  } finally {
    await context.close()
  }
}

async function downloadOldRegistration(browser) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  watch(page, 'old-registration-download')
  try {
    await login(page, accounts.applicant)
    await openDetail(page, result.target.certificateId, result.renewal.beforeVersionId)
    const button = page.getByTestId('registration-certificate-detail-attachment-download').first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    result.e2e8.afterApprovalScreenshot = await screenshot(page, 'old-registration-after-approval')
    const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
    await button.click()
    const download = await downloadPromise
    const suggestedFilename = download.suggestedFilename()
    const savePath = path.join(DOWNLOAD_DIR, suggestedFilename)
    await download.saveAs(savePath)
    const size = fs.statSync(savePath).size
    if (size <= 0) throw new Error('Downloaded old registration file is empty')
    if (!suggestedFilename.includes('已失效')) throw new Error(`Old registration filename missing 已失效: ${suggestedFilename}`)
    result.e2e8.download = { suggestedFilename, savePath, size, hasExpiredMarker: true }
    result.e2e8.status = 'PASS'
  } finally {
    await context.close()
  }
}

async function requestOldChangeDownload(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'old-change-request')
  try {
    await login(page, accounts.applicant)
    const opened = await openDetail(page, result.target.certificateId, result.renewal.beforeVersionId)
    const sameVersionChange = opened.history.find((item) =>
      item.businessFileId &&
      item.fileKind === 'CHANGE_APPROVAL' &&
      item.fileStatus === 'BOUND' &&
      item.changeStatus === 'APPLIED' &&
      String(item.targetVersionId) === String(result.renewal.beforeVersionId)
    )
    if (!sameVersionChange) throw new Error(`No same-version OLD change approval file for version ${result.renewal.beforeVersionId}`)
    result.e2e9.target = {
      certificateId: result.target.certificateId,
      versionId: result.renewal.beforeVersionId,
      businessFileId: sameVersionChange.businessFileId,
      originalFileName: sameVersionChange.originalFileName || ''
    }
    const section = page.getByTestId('registration-certificate-change-history').first()
    const requestButton = section.getByTestId('registration-certificate-change-attachment-request-download').first()
    await requestFileDownload(page, requestButton, result.e2e9, 'old-change')
  } finally {
    await context.close()
  }
}

async function downloadOldChange(browser) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  watch(page, 'old-change-download')
  try {
    await login(page, accounts.applicant)
    await openDetail(page, result.target.certificateId, result.renewal.beforeVersionId)
    const button = page
      .getByTestId('registration-certificate-change-history')
      .getByTestId('registration-certificate-change-attachment-download')
      .first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    result.e2e9.afterApprovalScreenshot = await screenshot(page, 'old-change-after-approval')
    const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
    await button.click()
    const download = await downloadPromise
    const suggestedFilename = download.suggestedFilename()
    const savePath = path.join(DOWNLOAD_DIR, suggestedFilename)
    await download.saveAs(savePath)
    const size = fs.statSync(savePath).size
    if (size <= 0) throw new Error('Downloaded old change file is empty')
    if (!suggestedFilename.includes('变更文件')) throw new Error(`Old change filename missing 变更文件: ${suggestedFilename}`)
    if (!suggestedFilename.includes('已失效')) throw new Error(`Old change filename missing 已失效: ${suggestedFilename}`)
    const extensionIndex = suggestedFilename.lastIndexOf('.')
    const baseName = extensionIndex > 0 ? suggestedFilename.slice(0, extensionIndex) : suggestedFilename
    if (!baseName.endsWith('已失效')) throw new Error(`已失效 marker is not before extension: ${suggestedFilename}`)
    result.e2e9.download = {
      suggestedFilename,
      savePath,
      size,
      hasChangeMarker: true,
      hasExpiredMarker: true,
      expiredMarkerBeforeExtension: true
    }
    result.e2e9.status = 'PASS'
  } finally {
    await context.close()
  }
}

async function main() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  try {
    const changeBaseline = await todoIds(browser, 'change-baseline')
    await submitChange(browser)
    result.change.approval = await approveLatest(browser, changeBaseline, '注册证变更审批', 'change-approval')
    result.change.status = 'PASS'

    const renewalBaseline = await todoIds(browser, 'renewal-baseline')
    await submitRenewal(browser)
    result.renewal.approval = await approveLatest(browser, renewalBaseline, '注册证延续审批', 'renewal-approval')
    result.renewal.status = 'PASS'

    const oldRegistrationBaseline = await todoIds(browser, 'old-registration-baseline')
    await requestOldRegistrationDownload(browser)
    result.e2e8.approval = await approveLatest(browser, oldRegistrationBaseline, '注册证下载审批', 'old-registration-approval')
    await downloadOldRegistration(browser)

    const oldChangeBaseline = await todoIds(browser, 'old-change-baseline')
    await requestOldChangeDownload(browser)
    result.e2e9.approval = await approveLatest(browser, oldChangeBaseline, '注册证下载审批', 'old-change-approval')
    await downloadOldChange(browser)

    result.status = result.e2e8.status === 'PASS' && result.e2e9.status === 'PASS' ? 'PASS' : 'PARTIAL_PASS'
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
