const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const TASK_DIR = __dirname
const REPO_ROOT = path.resolve(TASK_DIR, '..', '..', '..')
const FRONTEND_ROOT = path.join(REPO_ROOT, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(FRONTEND_ROOT, 'package.json'))
const { chromium } = frontendRequire('playwright')
const ACCEPTANCE = path.join(REPO_ROOT, 'e2e_test', 'registration', 'download', 'registration-certificate-download-e2e-acceptance.md')
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts')
const SCREENSHOT_DIR = path.join(ARTIFACT_DIR, 'screenshots')
const DOWNLOAD_DIR = path.join(ARTIFACT_DIR, 'downloads')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'result.json')
const BASE_URL = (process.env.REG_CERT_E2E_BASE_URL || 'http://127.0.0.1:8158').replace(/\/+$/, '')
const TENANT = process.env.REG_CERT_E2E_TENANT || '芋道源码'

function readPassword(label) {
  const doc = fs.readFileSync(ACCEPTANCE, 'utf8')
  const pattern = label === 'manager'
    ? /注册部经理 B：`chudongchuan`。密码\s*([^\r\n]+)/
    : /普通用户 C：`wanglixuan`。\s*密码\s*([^\r\n]+)/
  const match = doc.match(pattern)
  if (!match) throw new Error(`Missing ${label} password in acceptance document`)
  return match[1].trim()
}

const credentials = {
  manager: { username: 'chudongchuan', password: readPassword('manager') },
  user: { username: 'wanglixuan', password: readPassword('user') }
}

fs.mkdirSync(SCREENSHOT_DIR, { recursive: true })
fs.mkdirSync(DOWNLOAD_DIR, { recursive: true })

const evidence = {
  status: 'RUNNING',
  baseUrl: BASE_URL,
  tenant: TENANT,
  accounts: {
    manager: credentials.manager.username,
    user: credentials.user.username
  },
  selected: null,
  managerDownload: null,
  userBeforeRequest: null,
  accessRequest: null,
  approvalBaselineTaskIds: [],
  approval: null,
  userAfterApprovalDownload: null,
  blocked: [],
  failedResponses: [],
  consoleErrors: [],
  pageErrors: []
}

function saveResult() {
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

function isOk(payload) {
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
  const filePath = path.join(SCREENSHOT_DIR, `${name}.png`)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

function attachWatchers(page, label) {
  page.on('pageerror', (error) => evidence.pageErrors.push({ label, message: error.message }))
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push({ label, message: message.text() })
  })
  page.on('response', (response) => {
    if (response.status() >= 400) {
      const url = new URL(response.url())
      evidence.failedResponses.push({
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
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first().click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(account.username)
  await form.locator('input[type="password"]').first().fill(account.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginPayload = await readJson(await loginResponsePromise)
  if (!isOk(loginPayload)) {
    throw new Error(`Login failed for ${account.username}: ${loginPayload.msg || loginPayload.message || loginPayload.code}`)
  }
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

async function openRegistrationList(page) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/registration-certificates/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mdm/registration-certificate`, { waitUntil: 'commit' })
  await page.locator('[data-testid="registration-certificate-read-page"]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  const payload = await readJson(await pageResponsePromise)
  if (!isOk(payload)) throw new Error(`Registration page failed: ${payload.msg || payload.code}`)
  return Array.isArray(payload.data?.list) ? payload.data.list : []
}

async function openDetailByCertificateId(page, certificateId) {
  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/dcc/registration-certificates/${certificateId}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const historyResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/dcc/registration-certificates/${certificateId}/history`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mdm/registration-certificate/detail/${certificateId}`, { waitUntil: 'commit' })
  await page.locator('[data-testid="registration-certificate-detail-page"]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  const detailPayload = await readJson(await detailResponsePromise)
  const historyPayload = await readJson(await historyResponsePromise)
  if (!isOk(detailPayload)) throw new Error(`Detail failed: ${detailPayload.msg || detailPayload.code}`)
  if (!isOk(historyPayload)) throw new Error(`History failed: ${historyPayload.msg || historyPayload.code}`)
  return { detail: detailPayload.data, history: historyPayload.data || [] }
}

async function selectTargetAsUser(browser) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  attachWatchers(page, 'user-select-list')
  try {
    await login(page, credentials.user)
    let rows = await openRegistrationList(page)
    if (rows.length === 0) throw new Error('No registration certificates in current list')
    for (let pageNo = 1; pageNo <= 8; pageNo += 1) {
      for (const row of rows) {
        const certificateId = row.certificateId || row.id
        if (!certificateId) continue
        const detailPage = await context.newPage()
        attachWatchers(detailPage, `user-select-detail-${certificateId}`)
        try {
          const { detail, history } = await openDetailByCertificateId(detailPage, certificateId)
          const attachment = detailPage.locator('[data-testid="registration-certificate-detail-attachment"]').first()
          if (!(await attachment.count())) continue
          const requestButton = attachment.getByTestId('registration-certificate-detail-attachment-request-download')
          const downloadButton = attachment.getByTestId('registration-certificate-detail-attachment-download')
          const canRequest = (await requestButton.count()) > 0 && (await requestButton.first().isVisible())
          const canDownload = (await downloadButton.count()) > 0 && (await downloadButton.first().isVisible())
          const pending = canRequest && (await requestButton.first().innerText()).includes('申请中')
          if (detail?.registrationFileId && canRequest && !canDownload && !pending) {
            evidence.selected = {
              certificateId: detail.certificateId,
              registrationFileId: detail.registrationFileId,
              certificateNo: detail.certificateNo,
              projectCode: detail.projectCode || '',
              productName: detail.productName || '',
              approvalDate: detail.approvalDate || '',
              firstObtainedDate: detail.firstObtainedDate || '',
              status: detail.status,
              changeFileCount: history.filter((item) => item.fileKind === 'CHANGE_APPROVAL' && item.businessFileId).length,
              oldStatus: detail.status === 'OLD'
            }
            evidence.userBeforeRequest = {
              screenshot: await screenshot(detailPage, 'user-before-request'),
              downloadButtonCount: await downloadButton.count(),
              requestButtonText: await requestButton.first().innerText()
            }
            return { target: evidence.selected }
          }
        } finally {
          await detailPage.close()
        }
      }
      const nextButton = page.locator('.el-pagination button.btn-next').first()
      if (!(await nextButton.count()) || await nextButton.isDisabled()) break
      const nextPageResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/dcc/registration-certificates/page') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await nextButton.click()
      const payload = await readJson(await nextPageResponsePromise)
      if (!isOk(payload)) throw new Error(`Registration page failed: ${payload.msg || payload.code}`)
      rows = Array.isArray(payload.data?.list) ? payload.data.list : []
      if (rows.length === 0) break
    }
    throw new Error('No target certificate where ordinary user can apply download and cannot directly download')
  } finally {
    await context.close()
  }
}

async function managerDirectDownload(browser, target) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  attachWatchers(page, 'manager-direct-download')
  try {
    await login(page, credentials.manager)
    await openDetailByCertificateId(page, target.certificateId)
    await screenshot(page, 'manager-detail-download')
    const button = page.getByTestId('registration-certificate-detail-attachment-download').first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
    await button.click()
    const download = await downloadPromise
    const suggested = download.suggestedFilename()
    const savePath = path.join(DOWNLOAD_DIR, `manager-${suggested}`)
    await download.saveAs(savePath)
    const size = fs.statSync(savePath).size
    if (size <= 0) throw new Error('Manager downloaded file is empty')
    evidence.managerDownload = { suggestedFilename: suggested, savePath, size }
  } finally {
    await context.close()
  }
}

async function userSubmitRequest(browser, target) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  attachWatchers(page, 'user-submit-request')
  try {
    await login(page, credentials.user)
    await openDetailByCertificateId(page, target.certificateId)
    const button = page.getByTestId('registration-certificate-detail-attachment-request-download').first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    const requestResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/registration-certificates/access-requests') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await button.click()
    const response = await requestResponsePromise
    const payload = await readJson(response)
    if (!isOk(payload)) throw new Error(`Access request failed: ${payload.msg || payload.code}`)
    await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '已申请下载' }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    await button.waitFor({ state: 'visible', timeout: 30000 })
    const text = await button.innerText()
    if (!text.includes('申请中')) throw new Error(`Request button did not become pending: ${text}`)
    evidence.accessRequest = {
      requestId: payload.data,
      status: response.status(),
      screenshot: await screenshot(page, 'user-request-pending')
    }
  } finally {
    await context.close()
  }
}

function isTodoTaskPageResponse(response) {
  if (response.request().method() !== 'GET') return false
  const url = new URL(response.url())
  if (!url.pathname.includes('/admin-api/approval-center/tasks/page')) return false
  return (url.searchParams.get('viewType') || 'TODO') === 'TODO'
}

function isUnfilteredTodoTaskPageResponse(response) {
  if (!isTodoTaskPageResponse(response)) return false
  const url = new URL(response.url())
  const keyword = url.searchParams.get('keyword') || ''
  const moduleCode = url.searchParams.get('moduleCode') || ''
  return keyword.trim() === '' && moduleCode.trim() === ''
}

async function loadManagerTodoTasks(browser, label) {
  const context = await browser.newContext()
  const page = await context.newPage()
  attachWatchers(page, label)
  try {
    await login(page, credentials.manager)
    const taskPageResponsePromise = page.waitForResponse(
      isUnfilteredTodoTaskPageResponse,
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/approval-center/todo`, { waitUntil: 'commit' })
    const payload = await readJson(await taskPageResponsePromise)
    if (!isOk(payload)) throw new Error(`Approval list failed: ${payload.msg || payload.code}`)
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    return { context, page, tasks: Array.isArray(payload.data?.list) ? payload.data.list : [] }
  } catch (error) {
    await context.close()
    throw error
  }
}

async function captureManagerApprovalBaseline(browser) {
  const { context, tasks } = await loadManagerTodoTasks(browser, 'manager-approval-baseline')
  try {
    evidence.approvalBaselineTaskIds = tasks
      .filter((task) => String(task.businessTitle || '').includes('注册证下载审批'))
      .map((task) => task.id)
  } finally {
    await context.close()
  }
}

async function managerApprove(browser, requestId) {
  const findApprovalTaskIndex = (tasks) => {
    const requestToken = String(requestId)
    const downloadTasks = tasks.filter((task) =>
      String(task.businessTitle || '').includes('注册证下载审批')
    )
    const requestMatched = downloadTasks.findIndex((task) =>
      String(task.businessTitle || '').includes(requestToken) ||
      String(task.businessIdentifier || '') === requestToken
    )
    if (requestMatched >= 0) return tasks.indexOf(downloadTasks[requestMatched])
    const baselineIds = new Set(evidence.approvalBaselineTaskIds || [])
    const newDownloadTaskIndex = tasks.findIndex((task) =>
      String(task.businessTitle || '').includes('注册证下载审批') && !baselineIds.has(task.id)
    )
    if (newDownloadTaskIndex >= 0) return newDownloadTaskIndex
    return tasks.findIndex((task) => String(task.businessTitle || '').includes('注册证下载审批'))
  }
  const { context, page, tasks } = await loadManagerTodoTasks(browser, 'manager-approve')
  try {
    let index = findApprovalTaskIndex(tasks)
    evidence.approvalTaskCandidates = tasks.slice(0, 20).map((task) => ({
      id: task.id,
      sourceTaskId: task.sourceTaskId,
      businessKey: task.businessKey,
      processInstanceId: task.processInstanceId,
      businessTitle: task.businessTitle,
      businessIdentifier: task.businessIdentifier,
      applicant: task.applicant,
      sourceTaskType: task.sourceTaskType
    }))
    if (index < 0) {
      evidence.blocked.push({ step: 'manager-approve', reason: '审批中心待办中未找到注册证下载审批任务' })
      throw new Error(`Registration certificate download approval task not found for request ${requestId}`)
    }
    await screenshot(page, 'manager-approval-list')
    const selectedTask = tasks[index]
    const row = page
      .locator('.approval-center__table .el-table__row')
      .filter({ hasText: String(selectedTask.businessTitle || '注册证下载审批') })
      .first()
    await row.waitFor({ state: 'visible', timeout: 60000 })
    await row.getByRole('button', { name: '审核' }).click()
    const dialog = page.locator('.approval-center__review-dialog:visible')
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    await screenshot(page, 'manager-approval-dialog')
    await dialog.locator('input[type="password"]').fill(credentials.manager.password)
    const reviewResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/approval-center/tasks/review') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: '确认审核' }).click()
    const reviewPayload = await readJson(await reviewResponsePromise)
    if (!isOk(reviewPayload)) throw new Error(`Approval failed: ${reviewPayload.msg || reviewPayload.code}`)
    evidence.approval = {
      requestId,
      taskId: selectedTask.id,
      processInstanceId: selectedTask.processInstanceId || '',
      screenshot: await screenshot(page, 'manager-approval-success')
    }
  } finally {
    await context.close()
  }
}

async function userDownloadAfterApproval(browser, target) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  attachWatchers(page, 'user-download-after-approval')
  try {
    await login(page, credentials.user)
    await openDetailByCertificateId(page, target.certificateId)
    await screenshot(page, 'user-after-approval-detail')
    const button = page.getByTestId('registration-certificate-detail-attachment-download').first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
    await button.click()
    const download = await downloadPromise
    const suggested = download.suggestedFilename()
    const savePath = path.join(DOWNLOAD_DIR, `user-${suggested}`)
    await download.saveAs(savePath)
    const size = fs.statSync(savePath).size
    if (size <= 0) throw new Error('User downloaded file is empty')
    evidence.userAfterApprovalDownload = { suggestedFilename: suggested, savePath, size }
  } finally {
    await context.close()
  }
}

async function main() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  try {
    const { target } = await selectTargetAsUser(browser)
    await managerDirectDownload(browser, target)
    await captureManagerApprovalBaseline(browser)
    await userSubmitRequest(browser, target)
    await managerApprove(browser, evidence.accessRequest.requestId)
    await userDownloadAfterApproval(browser, target)
    evidence.status = 'PASS'
  } catch (error) {
    evidence.status = evidence.blocked.length ? 'BLOCKED' : 'FAIL'
    evidence.error = error.stack || error.message
    throw error
  } finally {
    await browser.close()
    saveResult()
  }
}

main()
