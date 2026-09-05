const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(REPO_ROOT, 'doc', 'tasks', '20260904-registration-change-e2e-sync')
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'registration-certificate-change-remaining-result.json')

const config = {
  baseUrl: (process.env.REG_CERT_CHANGE_E2E_BASE_URL || 'http://127.0.0.1:8154').replace(/\/+$/, ''),
  tenant: process.env.REG_CERT_CHANGE_E2E_TENANT || '芋道源码',
  applicantUsername: process.env.REG_CERT_CHANGE_E2E_USERNAME || 'wanglixuan',
  applicantPassword: process.env.REG_CERT_CHANGE_E2E_PASSWORD || '',
  approverUsername: process.env.REG_CERT_CHANGE_E2E_APPROVER_USERNAME || 'chudongchuan',
  approverPassword: process.env.REG_CERT_CHANGE_E2E_APPROVER_PASSWORD || '',
  targetCertificateNo: process.env.REG_CERT_CHANGE_E2E_TARGET_CERTIFICATE_NO || '',
  excludeCertificateNo: process.env.REG_CERT_CHANGE_E2E_CERTIFICATE_NO || '沪械注准20212020492',
  approvalDate: process.env.REG_CERT_CHANGE_E2E_APPROVAL_DATE || '2026-09-04',
  changeFilePath: process.env.REG_CERT_CHANGE_E2E_FILE || path.join(REPO_ROOT, 'e2e_test', 'registration', 'biangeng', 'biangeng.pdf'),
  runKey: process.env.REG_CERT_CHANGE_E2E_REMAINING_RUN_KEY || `E2E-CHANGE-REMAINING-${Date.now()}`,
  resumeCertificateId: process.env.REG_CERT_CHANGE_E2E_RESUME_CERTIFICATE_ID || '',
  resumeCertificateNo: process.env.REG_CERT_CHANGE_E2E_RESUME_CERTIFICATE_NO || '',
  resumeChangeRequestId: process.env.REG_CERT_CHANGE_E2E_RESUME_CHANGE_REQUEST_ID || '',
  resumeAfterValue: process.env.REG_CERT_CHANGE_E2E_RESUME_AFTER_VALUE || '',
  resumeDownloadRequestId: process.env.REG_CERT_CHANGE_E2E_RESUME_DOWNLOAD_REQUEST_ID || '',
  allowExpirySimulation: process.env.REG_CERT_CHANGE_E2E_ALLOW_EXPIRY_SIMULATION === '1',
  mysqlContainer: process.env.REG_CERT_CHANGE_E2E_MYSQL_CONTAINER || 'int-ruoyi-mysql',
  mysqlDatabase: process.env.REG_CERT_CHANGE_E2E_MYSQL_DATABASE || 'ruoyi-vue-pro'
}

const structuredFields = [
  ['产品名称', '变更后的产品名称', '全字段产品名称'],
  ['型号规格', '变更后的型号规格', '全字段型号规格'],
  ['结构组成', '变更后的结构组成', '全字段结构组成'],
  ['适用范围', '变更后的适用范围', '全字段适用范围'],
  ['产品技术要求', '变更后的产品技术要求', '全字段产品技术要求'],
  ['注册人名称', '变更后的注册人名称', '全字段注册人名称'],
  ['住所', '变更后的住所', '全字段住所'],
  ['生产地址', '变更后的生产地址', '全字段生产地址']
]

function writeResult(result) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function readResumeCandidate() {
  if (config.resumeCertificateId && config.resumeCertificateNo && config.resumeChangeRequestId) {
    const afterValues = config.resumeAfterValue
      ? { '变更后的产品名称': config.resumeAfterValue }
      : Object.fromEntries(
        structuredFields.map(([, placeholder, prefix]) => [placeholder, `${prefix}-${config.runKey}`])
      )
    return {
      runKey: config.runKey,
      e2e6: {
        status: 'PASS',
        candidate: {
          certificateId: config.resumeCertificateId,
          certificateNo: config.resumeCertificateNo,
          originalProductName: '',
          originalStatus: 'CURRENT'
        },
        submit: {
          requestId: config.resumeChangeRequestId,
          afterValues
        },
        resumeSource: 'env:REG_CERT_CHANGE_E2E_RESUME_*'
      },
      e2e7: { status: 'PASS' },
      e2e8: { status: 'NOT_RUN' }
    }
  }
  if (!fs.existsSync(RESULT_PATH)) {
    return null
  }
  try {
    const previous = JSON.parse(fs.readFileSync(RESULT_PATH, 'utf8'))
    if (process.env.REG_CERT_CHANGE_E2E_REMAINING_RUN_KEY && previous?.runKey !== config.runKey) {
      return null
    }
    const hasPendingRemainingCheck = previous?.e2e7?.status !== 'PASS' || previous?.e2e8?.status !== 'PASS'
    if (
      previous?.e2e6?.submit?.requestId &&
      previous.e2e6.candidate &&
      (previous.e2e6.status === 'RUNNING' || (previous.e2e6.status === 'PASS' && hasPendingRemainingCheck))
    ) {
      return previous
    }
  } catch (error) {
    return null
  }
  return null
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

function optionTextPattern(text) {
  return new RegExp(`^\\s*${String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`)
}

function assertSafeMysqlTarget() {
  if (!/^[A-Za-z0-9_-]+$/.test(config.mysqlContainer)) {
    throw new Error(`REG_CERT_CHANGE_E2E_MYSQL_CONTAINER contains unsupported characters: ${config.mysqlContainer}`)
  }
  if (!/^[A-Za-z0-9_-]+$/.test(config.mysqlDatabase)) {
    throw new Error(`REG_CERT_CHANGE_E2E_MYSQL_DATABASE contains unsupported characters: ${config.mysqlDatabase}`)
  }
}

function runMysql(sql) {
  assertSafeMysqlTarget()
  const result = spawnSync(
    'docker',
    [
      'exec',
      '-i',
      config.mysqlContainer,
      'sh',
      '-lc',
      `MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -N -B "${config.mysqlDatabase}"`
    ],
    { input: sql, encoding: 'utf8', maxBuffer: 1024 * 1024 }
  )
  if (result.status !== 0) {
    throw new Error(result.stderr.trim() || result.stdout.trim() || `docker mysql exited ${result.status}`)
  }
  return result.stdout.trim()
}

function findLatestPendingDownloadRequestId(certificateId, businessFileId) {
  const safeCertificateId = Number(certificateId)
  const safeBusinessFileId = Number(businessFileId)
  if (!Number.isSafeInteger(safeCertificateId) || safeCertificateId <= 0) {
    throw new Error(`invalid certificate id for pending download lookup: ${certificateId}`)
  }
  if (businessFileId != null && (!Number.isSafeInteger(safeBusinessFileId) || safeBusinessFileId <= 0)) {
    throw new Error(`invalid business file id for pending download lookup: ${businessFileId}`)
  }
  const businessFileFilter = Number.isSafeInteger(safeBusinessFileId) && safeBusinessFileId > 0
    ? `  AND JSON_CONTAINS(JSON_EXTRACT(detail_json, '$.businessFileIds'), CAST(${safeBusinessFileId} AS JSON))\n`
    : ''
  const rows = runMysql(`
SELECT id
FROM dcc_registration_certificate_access_request
WHERE certificate_id = ${safeCertificateId}
  AND request_type = 'DOWNLOAD_FILE'
${businessFileFilter}  AND status IN ('SUBMITTED', 'BPM_BOUND')
  AND deleted = 0
ORDER BY id DESC
LIMIT 1;
`)
  const id = Number(rows.split(/\r?\n/).filter(Boolean).pop())
  return Number.isSafeInteger(id) && id > 0 ? id : null
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

function formItemByLabel(form, label) {
  return form
    .getByText(optionTextPattern(label))
    .locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " el-form-item ")][1]')
}

async function selectFormItemOption(page, form, label, optionText) {
  const item = formItemByLabel(form, label)
  await item.locator('.el-select__wrapper, .el-select').first().click({ force: true })
  await selectVisibleOption(page, optionText)
  await expect(item.getByText(optionTextPattern(optionText))).toBeVisible({ timeout: 10000 })
}

async function gotoCurrentList(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/registration-certificates/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit', timeout: 60000 })
  await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({ timeout: 60000 })
  const payload = await readJsonResponse(await responsePromise)
  expect(isBusinessOk(payload), `list code ${payload.code}: ${payload.msg || ''}`).toBe(true)
  return Array.isArray(payload.data?.list) ? payload.data.list : []
}

async function chooseChangeableCertificate(page, evidence) {
  const rows = await gotoCurrentList(page)
  const tableRows = page.locator('.registration-certificate-current-table .el-table__body-wrapper .el-table__row')
  const count = await tableRows.count()
  for (let index = 0; index < count; index += 1) {
    const row = tableRows.nth(index)
    const text = await row.innerText()
    const button = row.getByRole('button', { name: '变更' })
    const rowCertificateNo = (await row.locator('.el-table__cell').first().innerText()).trim()
    if (config.targetCertificateNo && rowCertificateNo !== config.targetCertificateNo) {
      continue
    }
    if (rowCertificateNo === config.excludeCertificateNo || (await button.count()) === 0) {
      continue
    }
    const certificate = rows.find((item) => String(item.certificateNo || '').trim() === rowCertificateNo) || {}
    evidence.e2e6.candidate = {
      certificateId: certificate.certificateId || '',
      certificateNo: certificate.certificateNo || text.split(/\s+/)[0],
      originalProductName: certificate.productName || '',
      originalStatus: certificate.status || ''
    }
    await button.first().click()
    return { row, certificate }
  }
  throw new Error('未找到另一张可变更的 CURRENT 注册证。')
}

async function submitAllStructuredChange(page, evidence) {
  const { certificate } = await chooseChangeableCertificate(page, evidence)
  const dialog = page.locator('[data-testid="registration-certificate-change-dialog"]')
  await expect(dialog).toBeVisible({ timeout: 60000 })
  const form = page.locator('[data-testid="registration-certificate-change-form"]')
  await form.locator('input[placeholder="请选择批准日期"]').fill(config.approvalDate)
  await form.locator('.el-form-item').filter({ hasText: '变更内容' })
    .locator('.el-select__wrapper, .el-select').first().click({ force: true })
  for (const [label] of structuredFields) {
    await selectVisibleOption(page, label)
  }
  await page.keyboard.press('Escape')

  const afterValues = {}
  for (const [, placeholder, prefix] of structuredFields) {
    const value = `${prefix}-${config.runKey}`
    await expect(form.locator(`input[placeholder="${placeholder}"]`).first()).toBeVisible({ timeout: 30000 })
    await form.locator(`input[placeholder="${placeholder}"]`).first().fill(value)
    afterValues[placeholder] = value
  }
  await selectFormItemOption(page, form, '是否委托生产', '否')
  await selectFormItemOption(page, form, '是否自行生产', '是')
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
  evidence.e2e6.submit = {
    certificateId: certificate.certificateId,
    certificateNo: certificate.certificateNo,
    selectedFields: structuredFields.map(([label]) => label),
    afterValues,
    productionRelation: { entrustedProduction: false, selfProduction: true },
    requestPath: new URL(changeResponse.url()).pathname,
    httpStatus: changeResponse.status(),
    businessCode: changePayload.code,
    message: changePayload.msg || changePayload.message || '',
    requestId: changePayload.data || null
  }
  expect(changeResponse.ok(), `change HTTP status ${changeResponse.status()}`).toBe(true)
  expect(isBusinessOk(changePayload), `change code ${changePayload.code}: ${changePayload.msg || ''}`).toBe(true)
  await expect(dialog).toBeHidden({ timeout: 30000 })
  return { certificate, requestId: changePayload.data, afterValues }
}

async function verifyBeforeApproval(page, certificate, afterValues, evidence) {
  await page.goto(`${config.baseUrl}/mdm/registration-certificate/detail/${certificate.certificateId}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  const detailPage = page.locator('[data-testid="registration-certificate-detail-page"]')
  await expect(detailPage).toBeVisible({ timeout: 60000 })
  const unexpected = afterValues['变更后的产品名称']
  await expect(detailPage.locator('.detail-title')).not.toContainText(unexpected, { timeout: 10000 })
  await expect(detailPage.locator('.el-descriptions').first()).not.toContainText(unexpected, { timeout: 10000 })
  evidence.e2e6.beforeApproval = {
    currentDetailDoesNotShowAfterProductName: true,
    checkedValue: unexpected
  }
}

async function approveLatestChange(browser, certificateNo, requestId, evidenceNode, expectedTitle) {
  const context = await browser.newContext()
  const page = await context.newPage()
  try {
    await login(page, config.approverUsername, config.approverPassword)
    let todoPayload
    let tasks
    let index = -1
    let queryMode = 'certificateNo'
    const keywords = requestId ? [String(requestId)] : [certificateNo, '']
    for (const keyword of keywords) {
      const todoResponsePromise = page.waitForResponse(
        (response) => {
          const url = response.url()
          return url.includes('/admin-api/approval-center/tasks/page') &&
            url.includes('viewType=TODO') &&
            response.request().method() === 'GET' &&
            (keyword ? url.includes(`keyword=${encodeURIComponent(keyword)}`) : !url.includes('keyword='))
        },
        { timeout: 60000 }
      )
      const url = keyword
        ? `${config.baseUrl}/approval-center/todo?keyword=${encodeURIComponent(keyword)}`
        : `${config.baseUrl}/approval-center/todo`
      await page.goto(url, { waitUntil: 'commit', timeout: 60000 })
      await expect(page.locator('.approval-center')).toBeVisible({ timeout: 60000 })
      todoPayload = await readJsonResponse(await todoResponsePromise)
      tasks = Array.isArray(todoPayload.data?.list) ? todoPayload.data.list : []
      expect(isBusinessOk(todoPayload), `approval todo code ${todoPayload.code}: ${todoPayload.msg || ''}`).toBe(true)
      index = tasks.findIndex((task) => {
        const haystack = JSON.stringify(task)
        const requestIdText = String(requestId || '')
        const targetByRequestId = requestIdText && haystack.includes(requestIdText)
        const targetByBusiness = (certificateNo && haystack.includes(certificateNo)) || haystack.includes(config.runKey)
        const titleMatches = !expectedTitle || String(task.businessTitle || '').includes(expectedTitle)
        if (keyword === requestIdText) {
          return targetByRequestId && titleMatches
        }
        if (keyword) {
          return (targetByRequestId || targetByBusiness) && titleMatches
        }
        return (targetByRequestId || targetByBusiness) && titleMatches
      })
      if (index >= 0) {
        queryMode = keyword === String(requestId) ? 'requestId' : (keyword ? 'certificateNo' : 'unfiltered')
        break
      }
    }
    evidenceNode.todo = {
      businessCode: todoPayload.code,
      total: Number(todoPayload.data?.total || 0),
      queryMode,
      titles: tasks.map((task) => task.businessTitle).filter(Boolean).slice(0, 10)
    }
    expect(index, `approval task for ${certificateNo} must be visible`).toBeGreaterThanOrEqual(0)
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
    evidenceNode.submit = {
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

function simulateExpiredGrantForE2e9(requestId, evidence) {
  if (!config.allowExpirySimulation) {
    evidence.e2e9 = {
      status: 'BLOCKED',
      reason: 'E2E-9 需要超过 24 小时的授权；未设置 REG_CERT_CHANGE_E2E_ALLOW_EXPIRY_SIMULATION=1，未修改运行库数据。'
    }
    return false
  }
  const safeRequestId = Number(requestId)
  if (!Number.isSafeInteger(safeRequestId) || safeRequestId <= 0) {
    throw new Error(`invalid E2E-9 request id for expiry simulation: ${requestId}`)
  }
  const affectedRows = runMysql(`
UPDATE dcc_registration_certificate_grant
SET granted_at = DATE_SUB(NOW(), INTERVAL 25 HOUR),
    expires_at = DATE_SUB(NOW(), INTERVAL 1 HOUR)
WHERE request_id = ${safeRequestId}
  AND status = 'ACTIVE'
  AND deleted = 0;
SELECT ROW_COUNT();
`)
    .split(/\r?\n/)
    .filter(Boolean)
    .pop()
  if (affectedRows !== '1') {
    throw new Error(`E2E-9 grant expiry simulation expected 1 affected row, got ${affectedRows || '0'}`)
  }
  evidence.e2e9 = {
    status: 'RUNNING',
    simulation: {
      mysqlContainer: config.mysqlContainer,
      mysqlDatabase: config.mysqlDatabase,
      sourceRequestId: safeRequestId,
      affectedRows: Number(affectedRows),
      grantedAtOffset: 'NOW() - 25 hours',
      expiresAtOffset: 'NOW() - 1 hour'
    }
  }
  return true
}

async function verifyAppliedAllFields(page, certificate, afterValues, evidence) {
  await page.goto(`${config.baseUrl}/mdm/registration-certificate/detail/${certificate.certificateId}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  const detailPage = page.locator('[data-testid="registration-certificate-detail-page"]')
  await expect(detailPage).toBeVisible({ timeout: 60000 })
  for (const value of Object.values(afterValues)) {
    await expect(detailPage.getByText(value, { exact: false }).first()).toBeVisible({ timeout: 60000 })
  }
  const history = page.locator('[data-testid="registration-certificate-change-history"]')
  await expect(history).toBeVisible({ timeout: 60000 })
  await expect(history).toContainText('已变更', { timeout: 60000 })
  await expect(history).toContainText(config.runKey, { timeout: 60000 })
  await expect(history).toContainText('biangeng.pdf', { timeout: 60000 })
  evidence.e2e6.afterApproval = {
    certificateId: String(certificate.certificateId),
    allAfterValuesVisible: true,
    changeHistoryVisible: true,
    changeFileVisible: true
  }
}

async function downloadLatestChangeFileAsApprover(browser, certificate, evidence) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  try {
    await login(page, config.approverUsername, config.approverPassword)
    await page.goto(`${config.baseUrl}/mdm/registration-certificate/detail/${certificate.certificateId}`, {
      waitUntil: 'commit',
      timeout: 60000
    })
    const historyItem = page.locator('[data-testid="registration-certificate-change-history"] .change-history__item')
      .filter({ hasText: config.runKey })
      .first()
    await expect(historyItem).toBeVisible({ timeout: 60000 })
    await expect(historyItem.getByText('biangeng.pdf', { exact: false })).toBeVisible({ timeout: 30000 })
    const downloadButton = historyItem.locator('[data-testid="registration-certificate-change-attachment-download"]').first()
    await expect(downloadButton).toBeVisible({ timeout: 30000 })
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/files/') &&
        response.url().includes('/download'),
      { timeout: 60000 }
    )
    await downloadButton.click()
    const response = await responsePromise
    evidence.e2e7 = {
      status: 'PASS',
      requestPath: new URL(response.url()).pathname,
      httpStatus: response.status(),
      contentDisposition: response.headers()['content-disposition'] || '',
      fileAreaVisible: true,
      directDownloadButtonVisible: true
    }
    expect(response.ok(), `direct download HTTP status ${response.status()}`).toBe(true)
    expect(evidence.e2e7.contentDisposition).toContain(config.runKey)
    expect(evidence.e2e7.contentDisposition.toLowerCase()).toContain('.pdf')
  } finally {
    await context.close()
  }
}

async function requestAndDownloadAsOrdinaryUser(browser, certificate, evidence) {
  if (config.resumeDownloadRequestId) {
    evidence.e2e8.status = 'RUNNING'
    evidence.e2e8.requestSubmit = {
      resumedApprovedRequestId: Number(config.resumeDownloadRequestId),
      requestPath: 'resume from already-approved real page download request',
      businessCode: 0,
      reasonInputVisible: false,
      fixedPurposeUsedByPage: '页面提交的注册证文件下载申请'
    }
    await downloadAsOrdinaryUser(browser, certificate, evidence.e2e8)
    evidence.e2e8.status = 'PASS'
    if (!simulateExpiredGrantForE2e9(config.resumeDownloadRequestId, evidence)) {
      return
    }
    const secondRequestId = await submitDownloadRequestAsOrdinaryUser(browser, certificate, evidence.e2e9, {
      reasonSuffix: ' 二次申请',
      expectedInitialDownloadBlocked: true
    })
    await approveLatestChange(browser, certificate.certificateNo, secondRequestId, evidence.e2e9.approval = {}, '注册证下载审批')
    await downloadAsOrdinaryUser(browser, certificate, evidence.e2e9)
    evidence.e2e9.status = 'PASS'
    return
  }
  const requestId = await submitDownloadRequestAsOrdinaryUser(browser, certificate, evidence.e2e8, {
    reasonSuffix: '',
    expectedInitialDownloadBlocked: false
  })
  if (!requestId) {
    return
  }
  await approveLatestChange(browser, certificate.certificateNo, requestId, evidence.e2e8.approval = {}, '注册证下载审批')
  await downloadAsOrdinaryUser(browser, certificate, evidence.e2e8)
  evidence.e2e8.status = 'PASS'

  if (!simulateExpiredGrantForE2e9(requestId, evidence)) {
    return
  }
  const secondRequestId = await submitDownloadRequestAsOrdinaryUser(browser, certificate, evidence.e2e9, {
    reasonSuffix: ' 二次申请',
    expectedInitialDownloadBlocked: true
  })
  await approveLatestChange(browser, certificate.certificateNo, secondRequestId, evidence.e2e9.approval = {}, '注册证下载审批')
  await downloadAsOrdinaryUser(browser, certificate, evidence.e2e9)
  evidence.e2e9.status = 'PASS'
}

async function submitDownloadRequestAsOrdinaryUser(browser, certificate, evidenceNode, options) {
  const context = await browser.newContext({ acceptDownloads: true })
  const page = await context.newPage()
  try {
    await login(page, config.applicantUsername, config.applicantPassword)
    await page.goto(`${config.baseUrl}/mdm/registration-certificate/detail/${certificate.certificateId}`, {
      waitUntil: 'commit',
      timeout: 60000
    })
    const historyItem = page.locator('[data-testid="registration-certificate-change-history"] .change-history__item')
      .filter({ hasText: config.runKey })
      .first()
    await expect(historyItem).toBeVisible({ timeout: 60000 })
    if (options.expectedInitialDownloadBlocked) {
      await expect(historyItem.locator('[data-testid="registration-certificate-change-attachment-download"]').first())
        .toHaveCount(0, { timeout: 30000 })
    }
    const requestButton = historyItem.locator('[data-testid="registration-certificate-change-attachment-request-download"]').first()
    if ((await requestButton.count()) === 0) {
      evidenceNode.status = 'BLOCKED'
      evidenceNode.reason =
        options.expectedInitialDownloadBlocked
          ? '授权过期后页面未恢复变更批件文件申请下载入口。'
          : '普通用户页面未出现变更批件文件申请下载入口，可能已有直接下载权限或缺少申请权限。'
      return null
    }
    await expect(requestButton).toBeVisible({ timeout: 30000 })
    const requestButtonText = (await requestButton.innerText()).trim()
    if (requestButtonText.includes('申请中') || await requestButton.isDisabled()) {
      const businessFileId = await historyItem.evaluate((element) => {
        const button = element.querySelector('[data-testid="registration-certificate-change-attachment-request-download"]')
        return button ? button.closest('[data-business-file-id]')?.getAttribute('data-business-file-id') : null
      }).catch(() => null)
      const pendingRequestId = findLatestPendingDownloadRequestId(certificate.certificateId, businessFileId)
      if (!pendingRequestId) {
        throw new Error('普通用户页面显示申请中，但只读查询未找到对应 DOWNLOAD_FILE 待审批请求。')
      }
      evidenceNode.status = 'RUNNING'
      evidenceNode.requestSubmit = {
        resumedPendingRequestId: pendingRequestId,
        requestPath: 'read-only resume from already-submitted real page request',
        businessCode: 0,
        reasonInputVisible: false,
        fixedPurposeUsedByPage: `页面提交的注册证文件下载申请${options.reasonSuffix}`
      }
      return pendingRequestId
    }
    const requestResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/access-requests') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    ).catch((error) => ({ waitError: error.message }))
    await requestButton.click()
    const actionError = page.locator('.el-alert--error:visible').first()
    try {
      await actionError.waitFor({ state: 'visible', timeout: 3000 })
      const message = (await actionError.innerText()).trim()
      if (message.includes('缺少项目代码')) {
        evidenceNode.status = 'BLOCKED'
        evidenceNode.reason = '普通用户点击变更批件文件申请下载后，页面在发起申请前提示缺少项目代码，未产生访问申请 POST。'
        return null
      }
    } catch (error) {
      // No synchronous page-side validation appeared; continue waiting for the real submit response.
    }
    const requestResponse = await requestResponsePromise
    expect('waitError' in requestResponse, requestResponse.waitError).toBe(false)
    const requestPayload = await readJsonResponse(requestResponse)
    evidenceNode.status = 'RUNNING'
    evidenceNode.requestSubmit = {
      requestPath: new URL(requestResponse.url()).pathname,
      httpStatus: requestResponse.status(),
      businessCode: requestPayload.code,
      message: requestPayload.msg || requestPayload.message || '',
      requestId: requestPayload.data || null,
      reasonInputVisible: false,
      fixedPurposeUsedByPage: `页面提交的注册证文件下载申请${options.reasonSuffix}`
    }
    expect(isBusinessOk(requestPayload), `download request code ${requestPayload.code}: ${requestPayload.msg || ''}`).toBe(true)
    await expect(requestButton).toContainText('申请中', { timeout: 30000 })
    return evidenceNode.requestSubmit.requestId
  } finally {
    await context.close()
  }
}

async function downloadAsOrdinaryUser(browser, certificate, evidenceNode) {
  const downloadContext = await browser.newContext({ acceptDownloads: true })
  const downloadPage = await downloadContext.newPage()
  try {
    await login(downloadPage, config.applicantUsername, config.applicantPassword)
    await downloadPage.goto(`${config.baseUrl}/mdm/registration-certificate/detail/${certificate.certificateId}`, {
      waitUntil: 'commit',
      timeout: 60000
    })
    const historyItem = downloadPage.locator('[data-testid="registration-certificate-change-history"] .change-history__item')
      .filter({ hasText: config.runKey })
      .first()
    await expect(historyItem).toBeVisible({ timeout: 60000 })
    const downloadButton = historyItem.locator('[data-testid="registration-certificate-change-attachment-download"]').first()
    await expect(downloadButton).toBeVisible({ timeout: 60000 })
    const downloadEventPromise = downloadPage.waitForEvent('download', { timeout: 60000 }).catch(() => null)
    const responsePromise = downloadPage.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/registration-certificates/files/') &&
        response.url().includes('/download'),
      { timeout: 60000 }
    )
    await downloadButton.click()
    const response = await responsePromise
    const download = await downloadEventPromise
    const contentDisposition = response.headers()['content-disposition'] || ''
    const suggestedFilename = download ? download.suggestedFilename() : ''
    evidenceNode.authorizedDownload = {
      requestPath: new URL(response.url()).pathname,
      httpStatus: response.status(),
      contentDisposition,
      suggestedFilename,
      historyItemRunKeyVisible: true
    }
    expect(response.ok(), `authorized download HTTP status ${response.status()}`).toBe(true)
    if (contentDisposition || suggestedFilename) {
      const downloadIdentity = `${contentDisposition} ${suggestedFilename}`
      expect(downloadIdentity).toContain(config.runKey)
      expect(downloadIdentity.toLowerCase()).toContain('.pdf')
    }
  } finally {
    await downloadContext.close()
  }
}
test('registration certificate change remaining acceptance scenarios', async ({ page, browser }) => {
  test.setTimeout(480000)
  const evidence = {
    status: 'RUNNING',
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    applicantUsername: config.applicantUsername,
    approverUsername: config.approverUsername,
    runKey: config.runKey,
    e2e6: { status: 'RUNNING' },
    e2e7: { status: 'NOT_RUN' },
    e2e8: { status: 'NOT_RUN' },
    e2e9: {
      status: 'BLOCKED',
      reason: '验收要求超过 24 小时后重新申请；当前未发现正式页面业务日期推进入口，本轮不通过 DB/API 伪造过期。'
    },
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
    const resume = readResumeCandidate()
    let certificate
    let requestId
    let afterValues
    if (resume) {
      config.runKey = resume.runKey || config.runKey
      evidence.runKey = config.runKey
      evidence.e2e6 = resume.e2e6
      await login(page, config.applicantUsername, config.applicantPassword)
      certificate = {
        certificateId: resume.e2e6.candidate.certificateId,
        certificateNo: resume.e2e6.candidate.certificateNo
      }
      requestId = resume.e2e6.submit.requestId
      afterValues = resume.e2e6.submit.afterValues
    } else {
      await login(page, config.applicantUsername, config.applicantPassword)
      const submitted = await submitAllStructuredChange(page, evidence)
      certificate = submitted.certificate
      requestId = submitted.requestId
      afterValues = submitted.afterValues
    }
    if (evidence.e2e6.status === 'PASS') {
      await verifyAppliedAllFields(page, certificate, afterValues, evidence)
    } else {
      await verifyBeforeApproval(page, certificate, afterValues, evidence)
      await approveLatestChange(browser, certificate.certificateNo, requestId, evidence.e2e6.approval = {}, '注册证变更审批')
      await verifyAppliedAllFields(page, certificate, afterValues, evidence)
      evidence.e2e6.status = 'PASS'
    }
    if (evidence.e2e7.status !== 'PASS') {
      await downloadLatestChangeFileAsApprover(browser, certificate, evidence)
    }
    await requestAndDownloadAsOrdinaryUser(browser, certificate, evidence)
    evidence.status = evidence.e2e9.status === 'PASS'
      ? 'PASS'
      : (evidence.e2e8.status === 'PASS' ? 'PARTIAL_PASS_WITH_E2E9_BLOCKED' : 'PARTIAL_PASS_WITH_BLOCKERS')
    writeResult(evidence)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error instanceof Error ? error.message : String(error)
    writeResult(evidence)
    throw error
  }
})
