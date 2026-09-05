const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const TASK_DIR = __dirname
const REPO_ROOT = path.resolve(TASK_DIR, '..', '..', '..')
const FRONTEND_ROOT = path.join(REPO_ROOT, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(FRONTEND_ROOT, 'package.json'))
const { chromium } = frontendRequire('playwright')
const ACCEPTANCE = path.join(REPO_ROOT, 'e2e_test', 'registration', 'download', 'registration-certificate-download-e2e-acceptance.md')
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts', 'change-old')
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

const result = {
  status: 'RUNNING',
  baseUrl: BASE_URL,
  tenant: TENANT,
  accounts: { manager: credentials.manager.username, user: credentials.user.username },
  e2e7: { status: 'RUNNING' },
  e2e8: { status: 'BLOCKED', reason: '' },
  e2e9: { status: 'BLOCKED', reason: '' },
  failedResponses: [],
  consoleErrors: [],
  pageErrors: []
}

function saveResult() {
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
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

function watch(page, label) {
  page.on('pageerror', (error) => result.pageErrors.push({ label, message: error.message }))
  page.on('console', (message) => {
    if (message.type() === 'error') result.consoleErrors.push({ label, message: message.text() })
  })
  page.on('response', (response) => {
    if (response.status() >= 400) {
      result.failedResponses.push({
        label,
        method: response.request().method(),
        path: new URL(response.url()).pathname,
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
  const loginResponse = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const payload = await readJson(await loginResponse)
  if (!isOk(payload)) throw new Error(`Login failed for ${account.username}: ${payload.msg || payload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function openList(page) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/dcc/registration-certificates/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mdm/registration-certificate`, { waitUntil: 'commit', timeout: 60000 })
  await page.locator('[data-testid="registration-certificate-read-page"]').waitFor({ state: 'visible', timeout: 60000 })
  const payload = await readJson(await responsePromise)
  if (!isOk(payload)) throw new Error(`Registration list failed: ${payload.msg || payload.code}`)
  return Array.isArray(payload.data?.list) ? payload.data.list : []
}

async function openDetail(page, certificateId, options = {}) {
  const detailPromise = page.waitForResponse(
    (response) => response.url().includes(`/admin-api/dcc/registration-certificates/${certificateId}`) && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const historyPromise = page.waitForResponse(
    (response) => response.url().includes(`/admin-api/dcc/registration-certificates/${certificateId}/history`) && response.request().method() === 'GET',
    { timeout: 60000 }
  ).catch((error) => ({ optionalHistoryError: error.message }))
  const detailUrl = new URL(`${BASE_URL}/mdm/registration-certificate/detail/${certificateId}`)
  if (options.mode) detailUrl.searchParams.set('mode', options.mode)
  if (options.versionId) detailUrl.searchParams.set('versionId', String(options.versionId))
  await page.goto(detailUrl.toString(), { waitUntil: 'commit', timeout: 60000 })
  await page.locator('[data-testid="registration-certificate-detail-page"]').waitFor({ state: 'visible', timeout: 60000 })
  const detailPayload = await readJson(await detailPromise)
  const historyResponse = await historyPromise
  const historyPayload = historyResponse.optionalHistoryError
    ? { code: 0, data: [], optionalHistoryError: historyResponse.optionalHistoryError }
    : await readJson(historyResponse)
  if (!isOk(detailPayload)) throw new Error(`Detail failed: ${detailPayload.msg || detailPayload.code}`)
  if (!isOk(historyPayload)) throw new Error(`History failed: ${historyPayload.msg || historyPayload.code}`)
  return { detail: detailPayload.data, history: Array.isArray(historyPayload.data) ? historyPayload.data : [] }
}

async function findChangeTarget(browser) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  watch(page, 'find-change')
  try {
    await login(page, credentials.user)
    let rows = await openList(page)
    let scanned = 0
    for (let pageNo = 1; pageNo <= 10; pageNo += 1) {
      for (const row of rows) {
        const certificateId = row.certificateId || row.id
        if (!certificateId) continue
        scanned += 1
        const detailPage = await context.newPage()
        watch(detailPage, `change-detail-${certificateId}`)
        try {
          const { detail, history } = await openDetail(detailPage, certificateId)
          const appliedChangeFile = history.find((item) =>
            item.businessFileId &&
            item.fileKind === 'CHANGE_APPROVAL' &&
            item.fileStatus === 'BOUND' &&
            item.changeStatus === 'APPLIED'
          )
          const changeSection = detailPage.locator('[data-testid="registration-certificate-change-history"]').first()
          if (!(await changeSection.count())) continue
          const requestButton = changeSection.getByTestId('registration-certificate-change-attachment-request-download').first()
          const downloadButton = changeSection.getByTestId('registration-certificate-change-attachment-download').first()
          if (appliedChangeFile && (await requestButton.count()) && await requestButton.isVisible()) {
            const text = await requestButton.innerText()
            if (!text.includes('申请中') && !((await downloadButton.count()) && await downloadButton.isVisible())) {
              return {
                certificateId,
                certificateNo: detail.certificateNo,
                productName: detail.productName,
                changeId: appliedChangeFile.changeId,
                businessFileId: appliedChangeFile.businessFileId,
                requestText: text,
                scanned
              }
            }
          }
        } finally {
          await detailPage.close()
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
      if (!isOk(payload)) throw new Error(`Registration page ${pageNo + 1} failed: ${payload.msg || payload.code}`)
      rows = Array.isArray(payload.data?.list) ? payload.data.list : []
    }
    result.e2e7 = {
      status: 'BLOCKED',
      reason: `普通用户真实前端扫描 ${scanned} 条注册证详情，未找到可新申请下载的变更批件文件。`
    }
    return null
  } finally {
    await context.close()
  }
}

async function getManagerTodoBaseline(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'manager-baseline')
  try {
    await login(page, credentials.manager)
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/approval-center/todo`, { waitUntil: 'commit', timeout: 60000 })
    const payload = await readJson(await responsePromise)
    if (!isOk(payload)) throw new Error(`Todo baseline failed: ${payload.msg || payload.code}`)
    return new Set((payload.data?.list || []).map((task) => task.id))
  } finally {
    await context.close()
  }
}

async function requestChangeDownload(browser, target) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'change-request')
  try {
    await login(page, credentials.user)
    await openDetail(page, target.certificateId)
    const section = page.locator('[data-testid="registration-certificate-change-history"]').first()
    const requestButton = section.getByTestId('registration-certificate-change-attachment-request-download').first()
    await requestButton.waitFor({ state: 'visible', timeout: 60000 })
    result.e2e7.beforeRequestScreenshot = await screenshot(page, 'change-before-request')
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/access-requests') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await requestButton.click()
    const response = await responsePromise
    const payload = await readJson(response)
    if (!isOk(payload)) throw new Error(`Change download request failed: ${payload.msg || payload.code}`)
    await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '已申请下载' }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    await requestButton.waitFor({ state: 'visible', timeout: 30000 })
    const text = await requestButton.innerText()
    if (!text.includes('申请中')) throw new Error(`Change request button did not become pending: ${text}`)
    result.e2e7.request = {
      requestId: payload.data,
      httpStatus: response.status(),
      pendingText: text,
      screenshot: await screenshot(page, 'change-request-pending')
    }
  } finally {
    await context.close()
  }
}

async function approveRequest(browser, baseline, requestId) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'change-approve')
  try {
    await login(page, credentials.manager)
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/approval-center/todo`, { waitUntil: 'commit', timeout: 60000 })
    const payload = await readJson(await responsePromise)
    if (!isOk(payload)) throw new Error(`Approval list failed: ${payload.msg || payload.code}`)
    const tasks = Array.isArray(payload.data?.list) ? payload.data.list : []
    let task = tasks.find((item) => !baseline.has(item.id) && String(item.businessTitle || '').includes('注册证下载审批'))
    if (!task) {
      task = tasks.find((item) => String(item.businessTitle || '').includes('注册证下载审批'))
    }
    if (!task) throw new Error(`No approval task found for request ${requestId}`)
    result.e2e7.approvalCandidate = {
      taskId: task.id,
      processInstanceId: task.processInstanceId || '',
      businessTitle: task.businessTitle || ''
    }
    await screenshot(page, 'change-approval-list')
    const row = page.locator('.approval-center__table .el-table__row').filter({ hasText: task.businessTitle }).first()
    await row.waitFor({ state: 'visible', timeout: 60000 })
    await row.getByRole('button', { name: /审核|审批/ }).first().click()
    const dialog = page.locator('.approval-center__review-dialog:visible')
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    result.e2e7.approvalDialogScreenshot = await screenshot(page, 'change-approval-dialog')
    await dialog.locator('input[type="password"]').fill(credentials.manager.password)
    const reviewResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/review') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: '确认审核' }).click()
    const reviewPayload = await readJson(await reviewResponse)
    if (!isOk(reviewPayload)) throw new Error(`Approval failed: ${reviewPayload.msg || reviewPayload.code}`)
    result.e2e7.approval = {
      requestId,
      taskId: task.id,
      processInstanceId: task.processInstanceId || '',
      screenshot: await screenshot(page, 'change-approval-success')
    }
  } finally {
    await context.close()
  }
}

async function approveOldRequest(browser, baseline, requestId) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'old-approve')
  try {
    await login(page, credentials.manager)
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/approval-center/todo`, { waitUntil: 'commit', timeout: 60000 })
    const payload = await readJson(await responsePromise)
    if (!isOk(payload)) throw new Error(`Old approval list failed: ${payload.msg || payload.code}`)
    const tasks = Array.isArray(payload.data?.list) ? payload.data.list : []
    let task = tasks.find((item) => !baseline.has(item.id) && String(item.businessTitle || '').includes('注册证下载审批'))
    if (!task) task = tasks.find((item) => String(item.businessTitle || '').includes('注册证下载审批'))
    if (!task) throw new Error(`No old certificate approval task found for request ${requestId}`)
    result.e2e8.approvalCandidate = {
      taskId: task.id,
      processInstanceId: task.processInstanceId || '',
      businessTitle: task.businessTitle || ''
    }
    await screenshot(page, 'old-approval-list')
    const row = page.locator('.approval-center__table .el-table__row').filter({ hasText: task.businessTitle }).first()
    await row.waitFor({ state: 'visible', timeout: 60000 })
    await row.getByRole('button', { name: /审核|审批/ }).first().click()
    const dialog = page.locator('.approval-center__review-dialog:visible')
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    result.e2e8.approvalDialogScreenshot = await screenshot(page, 'old-approval-dialog')
    await dialog.locator('input[type="password"]').fill(credentials.manager.password)
    const reviewResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/review') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: '确认审核' }).click()
    const reviewPayload = await readJson(await reviewResponse)
    if (!isOk(reviewPayload)) throw new Error(`Old approval failed: ${reviewPayload.msg || reviewPayload.code}`)
    result.e2e8.approval = {
      requestId,
      taskId: task.id,
      processInstanceId: task.processInstanceId || '',
      screenshot: await screenshot(page, 'old-approval-success')
    }
  } finally {
    await context.close()
  }
}

async function approveOldChangeRequest(browser, baseline, requestId) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'old-change-approve')
  try {
    await login(page, credentials.manager)
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/approval-center/todo`, { waitUntil: 'commit', timeout: 60000 })
    const payload = await readJson(await responsePromise)
    if (!isOk(payload)) throw new Error(`Old change approval list failed: ${payload.msg || payload.code}`)
    const tasks = Array.isArray(payload.data?.list) ? payload.data.list : []
    let task = tasks.find((item) => !baseline.has(item.id) && String(item.businessTitle || '').includes('注册证下载审批'))
    if (!task) task = tasks.find((item) => String(item.businessTitle || '').includes('注册证下载审批'))
    if (!task) throw new Error(`No old change approval task found for request ${requestId}`)
    result.e2e9.approvalCandidate = {
      taskId: task.id,
      processInstanceId: task.processInstanceId || '',
      businessTitle: task.businessTitle || ''
    }
    await screenshot(page, 'old-change-approval-list')
    const row = page.locator('.approval-center__table .el-table__row').filter({ hasText: task.businessTitle }).first()
    await row.waitFor({ state: 'visible', timeout: 60000 })
    await row.getByRole('button', { name: /审核|审批/ }).first().click()
    const dialog = page.locator('.approval-center__review-dialog:visible')
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    result.e2e9.approvalDialogScreenshot = await screenshot(page, 'old-change-approval-dialog')
    await dialog.locator('input[type="password"]').fill(credentials.manager.password)
    const reviewResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/approval-center/tasks/review') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: '确认审核' }).click()
    const reviewPayload = await readJson(await reviewResponse)
    if (!isOk(reviewPayload)) throw new Error(`Old change approval failed: ${reviewPayload.msg || reviewPayload.code}`)
    result.e2e9.approval = {
      requestId,
      taskId: task.id,
      processInstanceId: task.processInstanceId || '',
      screenshot: await screenshot(page, 'old-change-approval-success')
    }
  } finally {
    await context.close()
  }
}

async function downloadApprovedChange(browser, target) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  watch(page, 'change-download')
  try {
    await login(page, credentials.user)
    await openDetail(page, target.certificateId)
    result.e2e7.afterApprovalScreenshot = await screenshot(page, 'change-after-approval')
    const button = page
      .locator('[data-testid="registration-certificate-change-history"]')
      .getByTestId('registration-certificate-change-attachment-download')
      .first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
    await button.click()
    const download = await downloadPromise
    const suggested = download.suggestedFilename()
    const savePath = path.join(DOWNLOAD_DIR, suggested)
    await download.saveAs(savePath)
    const size = fs.statSync(savePath).size
    if (size <= 0) throw new Error('Downloaded change file is empty')
    if (!suggested.includes('变更文件')) throw new Error(`Change filename missing marker: ${suggested}`)
    result.e2e7.download = { suggestedFilename: suggested, savePath, size, hasChangeMarker: true }
    result.e2e7.status = 'PASS'
    result.e2e7.target = target
  } finally {
    await context.close()
  }
}

async function requestOldChangeDownload(browser, target) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'old-change-request')
  try {
    await login(page, credentials.user)
    await openDetail(page, target.certificateId, { mode: 'old-detail', versionId: target.versionId })
    const section = page.locator('[data-testid="registration-certificate-change-history"]').first()
    await section.waitFor({ state: 'visible', timeout: 60000 })
    const requestButton = section.getByTestId('registration-certificate-change-attachment-request-download').first()
    const downloadButton = section.getByTestId('registration-certificate-change-attachment-download').first()
    const hasDownload = (await downloadButton.count()) && await downloadButton.isVisible()
    if (hasDownload) {
      result.e2e9.status = 'BLOCKED'
      result.e2e9.reason = 'OLD + 变更批件候选已存在有效下载授权，真实前端不再显示申请下载入口，无法重复证明申请/审批动作。'
      result.e2e9.beforeRequestScreenshot = await screenshot(page, 'old-change-already-authorized')
      return false
    }
    await requestButton.waitFor({ state: 'visible', timeout: 60000 })
    const buttonText = await requestButton.innerText()
    if (buttonText.includes('申请中')) {
      result.e2e9.status = 'BLOCKED'
      result.e2e9.reason = 'OLD + 变更批件候选已经处于申请中，无法在本轮真实前端重新发起申请。'
      result.e2e9.beforeRequestScreenshot = await screenshot(page, 'old-change-already-pending')
      return false
    }
    result.e2e9.beforeRequestScreenshot = await screenshot(page, 'old-change-before-request')
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/access-requests') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await requestButton.click()
    const response = await responsePromise
    const payload = await readJson(response)
    if (!isOk(payload)) throw new Error(`Old change download request failed: ${payload.msg || payload.code}`)
    await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '已申请下载' }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    const pendingText = await requestButton.innerText()
    if (!pendingText.includes('申请中')) throw new Error(`Old change request button did not become pending: ${pendingText}`)
    result.e2e9.request = {
      requestId: payload.data,
      httpStatus: response.status(),
      pendingText,
      screenshot: await screenshot(page, 'old-change-request-pending')
    }
    return true
  } finally {
    await context.close()
  }
}

async function downloadApprovedOldChange(browser, target) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  watch(page, 'old-change-download')
  try {
    await login(page, credentials.user)
    await openDetail(page, target.certificateId, { mode: 'old-detail', versionId: target.versionId })
    result.e2e9.afterApprovalScreenshot = await screenshot(page, 'old-change-after-approval')
    const button = page
      .locator('[data-testid="registration-certificate-change-history"]')
      .getByTestId('registration-certificate-change-attachment-download')
      .first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
    await button.click()
    const download = await downloadPromise
    const suggested = download.suggestedFilename()
    const savePath = path.join(DOWNLOAD_DIR, suggested)
    await download.saveAs(savePath)
    const size = fs.statSync(savePath).size
    if (size <= 0) throw new Error('Downloaded old change file is empty')
    if (!suggested.includes('变更文件')) throw new Error(`Old change filename missing change marker: ${suggested}`)
    if (!suggested.includes('已失效')) throw new Error(`Old change filename missing expired marker: ${suggested}`)
    const extensionIndex = suggested.lastIndexOf('.')
    const baseName = extensionIndex > 0 ? suggested.slice(0, extensionIndex) : suggested
    if (!baseName.endsWith('已失效')) throw new Error(`Expired marker is not before extension: ${suggested}`)
    result.e2e9.download = {
      suggestedFilename: suggested,
      savePath,
      size,
      hasChangeMarker: true,
      hasExpiredMarker: true,
      expiredMarkerBeforeExtension: true
    }
    result.e2e9.target = target
    result.e2e9.status = 'PASS'
    result.e2e9.reason = ''
  } finally {
    await context.close()
  }
}

async function scanOldTab(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'old-scan')
  try {
    await login(page, credentials.user)
    await page.goto(`${BASE_URL}/mdm/registration-certificate`, { waitUntil: 'commit', timeout: 60000 })
    await page.locator('[data-testid="registration-certificate-read-page"]').waitFor({ state: 'visible', timeout: 60000 })
    const oldResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/old-index/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.getByRole('tab', { name: '老证' }).click()
    const payload = await readJson(await oldResponse)
    if (!isOk(payload)) throw new Error(`Old index failed: ${payload.msg || payload.code}`)
    const rows = Array.isArray(payload.data?.list) ? payload.data.list : []
    const total = Number(payload.data?.total || rows.length || 0)
    result.e2e8.reason = total > 0
      ? `普通用户老证页可见 ${total} 条记录，但本任务未发现可直接打开详情并继续完成下载申请的稳定入口；按前端-only 门禁未用 API/SQL 补造 OLD 下载样本。`
      : '普通用户真实前端老证页无可操作 OLD 记录，无法执行失效证件下载申请。'
    result.e2e8.oldIndex = { total, firstRows: rows.slice(0, 5).map((row) => ({ certificateId: row.certificateId, certificateNo: row.certificateNo, status: row.status })) }
    result.e2e8.screenshot = await screenshot(page, 'old-index-scan')
    result.e2e9.reason = '组合场景依赖“OLD 证件 + 变更批件文件”可操作样本；本轮未通过真实前端取得该组合样本。'
  } finally {
    await context.close()
  }
}

async function findOldTarget(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'old-find')
  try {
    await login(page, credentials.user)
    await page.goto(`${BASE_URL}/mdm/registration-certificate`, { waitUntil: 'commit', timeout: 60000 })
    await page.locator('[data-testid="registration-certificate-read-page"]').waitFor({ state: 'visible', timeout: 60000 })
    const oldResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/old-index/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.getByRole('tab', { name: '老证' }).click()
    const payload = await readJson(await oldResponse)
    if (!isOk(payload)) throw new Error(`Old index failed: ${payload.msg || payload.code}`)
    const rows = Array.isArray(payload.data?.list) ? payload.data.list : []
    for (const row of rows) {
      const detailPage = await context.newPage()
      watch(detailPage, `old-detail-${row.certificateId}`)
      try {
        const { detail, history } = await openDetail(detailPage, row.certificateId, {
          mode: 'old-detail',
          versionId: row.versionId
        })
        const requestButton = detailPage.getByTestId('registration-certificate-detail-attachment-request-download').first()
        const downloadButton = detailPage.getByTestId('registration-certificate-detail-attachment-download').first()
        const canRequest = (await requestButton.count()) && await requestButton.isVisible()
        const canDownload = (await downloadButton.count()) && await downloadButton.isVisible()
        const appliedOldChange = history.find((item) =>
          item.businessFileId &&
          item.fileKind === 'CHANGE_APPROVAL' &&
          item.fileStatus === 'BOUND' &&
          item.changeStatus === 'APPLIED' &&
          String(item.targetVersionId) === String(detail.versionId)
        )
        if (appliedOldChange) {
          result.e2e9.oldChangeCandidate = {
            certificateId: row.certificateId,
            versionId: row.versionId,
            certificateNo: detail.certificateNo,
            productName: detail.productName,
            businessFileId: appliedOldChange.businessFileId
          }
        }
        if (detail.registrationFileId && canRequest && !canDownload) {
          return {
            certificateId: row.certificateId,
            versionId: row.versionId,
            certificateNo: detail.certificateNo,
            productName: detail.productName,
            businessFileId: detail.registrationFileId
          }
        }
      } finally {
        await detailPage.close()
      }
    }
    result.e2e8.reason = `普通用户真实前端老证页可见 ${rows.length} 条记录，但没有找到未授权且可申请下载的失效注册证文件。`
    result.e2e8.oldIndex = {
      total: Number(payload.data?.total || rows.length || 0),
      firstRows: rows.slice(0, 5).map((row) => ({ certificateId: row.certificateId, versionId: row.versionId, certificateNo: row.certificateNo, status: row.status }))
    }
    result.e2e9.reason = result.e2e9.oldChangeCandidate
      ? '已找到 OLD + 变更批件候选，但本脚本未自动复用同一候选完成组合下载申请。'
      : '组合场景依赖“OLD 证件 + 变更批件文件”可操作样本；本轮未通过真实前端取得该组合样本。'
    return null
  } finally {
    await context.close()
  }
}

async function requestOldDownload(browser, target) {
  const context = await browser.newContext()
  const page = await context.newPage()
  watch(page, 'old-request')
  try {
    await login(page, credentials.user)
    await openDetail(page, target.certificateId, { mode: 'old-detail', versionId: target.versionId })
    const requestButton = page.getByTestId('registration-certificate-detail-attachment-request-download').first()
    await requestButton.waitFor({ state: 'visible', timeout: 60000 })
    result.e2e8.beforeRequestScreenshot = await screenshot(page, 'old-before-request')
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/access-requests') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await requestButton.click()
    const response = await responsePromise
    const payload = await readJson(response)
    if (!isOk(payload)) throw new Error(`Old certificate download request failed: ${payload.msg || payload.code}`)
    await page.locator('.el-message:visible, .el-message--success:visible').filter({ hasText: '已申请下载' }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    await requestButton.waitFor({ state: 'visible', timeout: 30000 })
    const text = await requestButton.innerText()
    if (!text.includes('申请中')) throw new Error(`Old request button did not become pending: ${text}`)
    result.e2e8.request = {
      requestId: payload.data,
      httpStatus: response.status(),
      pendingText: text,
      screenshot: await screenshot(page, 'old-request-pending')
    }
  } finally {
    await context.close()
  }
}

async function downloadApprovedOld(browser, target) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  watch(page, 'old-download')
  try {
    await login(page, credentials.user)
    await openDetail(page, target.certificateId, { mode: 'old-detail', versionId: target.versionId })
    result.e2e8.afterApprovalScreenshot = await screenshot(page, 'old-after-approval')
    const button = page.getByTestId('registration-certificate-detail-attachment-download').first()
    await button.waitFor({ state: 'visible', timeout: 60000 })
    const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
    await button.click()
    const download = await downloadPromise
    const suggested = download.suggestedFilename()
    const savePath = path.join(DOWNLOAD_DIR, suggested)
    await download.saveAs(savePath)
    const size = fs.statSync(savePath).size
    if (size <= 0) throw new Error('Downloaded old certificate file is empty')
    if (!suggested.includes('已失效')) throw new Error(`Old certificate filename missing expired marker: ${suggested}`)
    result.e2e8.download = { suggestedFilename: suggested, savePath, size, hasExpiredMarker: true }
    result.e2e8.target = target
    result.e2e8.status = 'PASS'
    result.e2e8.reason = ''
  } finally {
    await context.close()
  }
}

async function main() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  try {
    const target = await findChangeTarget(browser)
    if (target) {
      const baseline = await getManagerTodoBaseline(browser)
      await requestChangeDownload(browser, target)
      await approveRequest(browser, baseline, result.e2e7.request.requestId)
      await downloadApprovedChange(browser, target)
    }
    const oldTarget = await findOldTarget(browser)
    if (oldTarget) {
      const oldBaseline = await getManagerTodoBaseline(browser)
      await requestOldDownload(browser, oldTarget)
      await approveOldRequest(browser, oldBaseline, result.e2e8.request.requestId)
      await downloadApprovedOld(browser, oldTarget)
    } else {
      await scanOldTab(browser)
    }
    if (result.e2e9.oldChangeCandidate) {
      const oldChangeBaseline = await getManagerTodoBaseline(browser)
      const requested = await requestOldChangeDownload(browser, result.e2e9.oldChangeCandidate)
      if (requested) {
        await approveOldChangeRequest(browser, oldChangeBaseline, result.e2e9.request.requestId)
        await downloadApprovedOldChange(browser, result.e2e9.oldChangeCandidate)
      }
    }
    result.status = [result.e2e7.status, result.e2e8.status, result.e2e9.status].some((status) => status === 'PASS')
      ? 'PARTIAL_PASS'
      : 'BLOCKED'
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
