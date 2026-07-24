const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const taskDir = path.join(repoRoot, 'doc', 'tasks', '20260721-edhr-special-node-attachment-actions')
const artifactDir = path.join(taskDir, 'e2e-artifacts')
const resultFile = path.join(artifactDir, 'yudao-admin-special-node-attachment-e2e.json')
const screenshotFile = path.join(artifactDir, 'yudao-admin-special-node-attachment-e2e.png')
const failureScreenshotFile = path.join(artifactDir, 'yudao-admin-special-node-attachment-e2e-failure.png')

const config = {
  baseUrl: process.env.EDHR_SPECIAL_ATTACHMENT_BASE_URL || 'http://localhost:8081',
  backendUrl: process.env.EDHR_SPECIAL_ATTACHMENT_BACKEND_URL || 'http://127.0.0.1:48081',
  tenant: process.env.EDHR_SPECIAL_ATTACHMENT_TENANT || '芋道源码',
  username: process.env.EDHR_SPECIAL_ATTACHMENT_USERNAME || 'admin',
  password: process.env.EDHR_SPECIAL_ATTACHMENT_PASSWORD || 'admin123',
  headed: process.env.EDHR_SPECIAL_ATTACHMENT_HEADED === '1',
  maxPages: Number(process.env.EDHR_SPECIAL_ATTACHMENT_MAX_PAGES || 30),
  preferredTaskStatus: Number(process.env.EDHR_SPECIAL_ATTACHMENT_PREFERRED_TASK_STATUS || 40),
  requirePreferredTaskStatus: process.env.EDHR_SPECIAL_ATTACHMENT_REQUIRE_PREFERRED_TASK_STATUS === '1',
  batchExecutionCode: process.env.EDHR_SPECIAL_ATTACHMENT_BATCH_EXECUTION_CODE || ''
}

const listPath = '/mes/pro/feedback/edhr-batch-execution'
const detailPath = '/mes/pro/feedback/edhr-batch-execution/detail'
const specialLabels = {
  INCOMING_INSPECTION_REPORT: '来料检报告',
  STERILIZATION_REPORT: '灭菌报告',
  FINISHED_PRODUCT_INSPECTION_REPORT: '成品检报告',
  FINISHED_PRODUCT_INSPECTION_RECORD: '成品检记录'
}
const preferredSpecialOrder = [
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD',
  'INCOMING_INSPECTION_REPORT'
]
const taskStatusBlockedForOperation = new Set([40, 45, 50])
const taskStatusBlockedForUpload = new Set([50])
const batchStatusBlockedForOperation = new Set([30, 40, 50, 60])
const batchStatusBlockedForUpload = new Set([40, 50, 60])

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function writeResult(status, payload) {
  ensureDir(artifactDir)
  fs.writeFileSync(
    resultFile,
    JSON.stringify(
      {
        status,
        generatedAt: new Date().toISOString(),
        ...payload
      },
      null,
      2
    ),
    'utf8'
  )
}

function assertLocalAuthorizedConfig() {
  assert.equal(config.baseUrl, 'http://localhost:8081', 'E2E must use local frontend http://localhost:8081')
  assert.match(config.backendUrl, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'E2E must use local backend 48081')
  assert.equal(config.tenant, '芋道源码', 'this authorized E2E must run in 芋道源码 tenant')
  assert.equal(config.username, 'admin', 'this authorized E2E must run as admin')
  assert.ok(config.password, 'admin password is required')
}

async function firstVisible(locator, message) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) return item
  }
  throw new Error(message)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 60000 })
    await option.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_payload_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      result[key] = result[key] || sessionStorage.getItem(key)
    }
    return result
  })
  const unwrap = (raw) => {
    if (!raw) return ''
    let current = raw
    for (let index = 0; index < 6; index += 1) {
      try {
        current = JSON.parse(current)
      } catch {
        break
      }
      if (current && typeof current === 'object') {
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
      }
      if (typeof current !== 'string') break
    }
    return String(current || '').replace(/^"|"$/g, '')
  }
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

function apiHeaders(auth) {
  assert.ok(auth.token, 'browser auth token is required for read-only discovery')
  assert.ok(auth.tenantId, 'tenant-id is required for read-only discovery')
  return {
    Authorization: `Bearer ${auth.token}`,
    'tenant-id': String(auth.tenantId),
    ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
  }
}

async function apiGet(page, auth, apiPath, params = {}) {
  const response = await page.request.get(`${config.backendUrl}${apiPath}`, {
    headers: apiHeaders(auth),
    params
  })
  assert.equal(response.status(), 200, `${apiPath} HTTP status must be 200`)
  const body = await response.json()
  assert.ok(body.code === 0 || body.code === 200, `${apiPath} business code failed: ${JSON.stringify(body)}`)
  return body.data
}

function isOperableSpecialTask(detail, task) {
  return (
    task?.nodeType &&
    task.nodeType !== 'ROUTE_FORM' &&
    Array.isArray(task.allowedActions) &&
    task.allowedActions.includes('CLOSE') &&
    !taskStatusBlockedForOperation.has(Number(task.status)) &&
    !batchStatusBlockedForOperation.has(Number(detail.status)) &&
    task.available !== false
  )
}

function isUploadableSpecialTaskBeforeRelease(detail, task) {
  return (
    task?.nodeType &&
    task.nodeType !== 'ROUTE_FORM' &&
    !taskStatusBlockedForUpload.has(Number(task.status)) &&
    !batchStatusBlockedForUpload.has(Number(detail.status)) &&
    detail?.releaseSummary?.releaseStatus !== 'RELEASED'
  )
}

async function findCandidate(page, auth) {
  const scanned = []
  for (let pageNo = 1; pageNo <= config.maxPages; pageNo += 1) {
    const pageData = await apiGet(page, auth, '/admin-api/mes/pro/edhr-batch-execution/page', {
      pageNo,
      pageSize: 20,
      ...(config.batchExecutionCode ? { batchExecutionCode: config.batchExecutionCode } : {})
    })
    const rows = Array.isArray(pageData?.list)
      ? pageData.list
      : Array.isArray(pageData?.records)
        ? pageData.records
        : Array.isArray(pageData)
          ? pageData
          : []
    scanned.push({ pageNo, rowCount: rows.length })
    if (!rows.length) break
    for (const row of rows) {
      if (config.batchExecutionCode && row.batchExecutionCode !== config.batchExecutionCode) {
        continue
      }
      const detail = await apiGet(page, auth, '/admin-api/mes/pro/edhr-batch-execution/get', { id: row.id })
      const tasks = Array.isArray(detail?.tasks) ? detail.tasks : []
      const uploadableCandidates = preferredSpecialOrder
        .map((nodeType) =>
          tasks.find(
            (task) =>
              task.nodeType === nodeType &&
              Number(task.status) === config.preferredTaskStatus &&
              isUploadableSpecialTaskBeforeRelease(detail, task)
          )
        )
        .filter(Boolean)
      const operableCandidates = preferredSpecialOrder
        .map((nodeType) => tasks.find((task) => task.nodeType === nodeType && isOperableSpecialTask(detail, task)))
        .filter(Boolean)
      const candidates = uploadableCandidates.length || config.requirePreferredTaskStatus
        ? uploadableCandidates
        : operableCandidates
      if (candidates.length) {
        const task = candidates[0]
        return {
          scanned,
          batchExecution: {
            id: detail.id,
            batchExecutionCode: detail.batchExecutionCode,
            workOrderCode: detail.workOrderCode,
            batchCode: detail.batchCode,
            status: detail.status
          },
          taskBefore: {
            id: task.id,
            nodeType: task.nodeType,
            label: specialLabels[task.nodeType] || task.processName || task.processCode,
            status: task.status,
            preferredStatusMatched: Number(task.status) === config.preferredTaskStatus,
            uploadableBeforeRelease: isUploadableSpecialTaskBeforeRelease(detail, task),
            submittableByCloseAction: isOperableSpecialTask(detail, task),
            specialPayloadJson: task.specialPayloadJson || ''
          }
        }
      }
    }
  }
  throw new Error(`未找到可安全验证的芋道源码特殊节点；已扫描 ${JSON.stringify(scanned)}`)
}

async function uploadFileThroughUi(page, fileName, bodyText) {
  const prepareResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/special-node/attachment/prepare-upload') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const chooserPromise = page.waitForEvent('filechooser', { timeout: 30000 })
  await page
    .locator('.edhr-batch-detail__special-node-action-grid .edhr-batch-detail__rail-task-action')
    .filter({ hasText: '上传文件' })
    .first()
    .click()
  const chooser = await chooserPromise
  await chooser.setFiles({
    name: fileName,
    mimeType: 'text/plain',
    buffer: Buffer.from(bodyText, 'utf8')
  })
  const response = await prepareResponsePromise
  assert.equal(response.status(), 200, 'prepare-upload HTTP status must be 200')
  const body = await response.json()
  assert.ok(body.code === 0 || body.code === 200, `prepare-upload business response failed: ${JSON.stringify(body)}`)
  return body.data?.data || body.data
}

async function assertPendingFileCount(page, expected, fileName) {
  const pendingSection = page.locator('section[aria-label="待提交附件"]').first()
  await pendingSection.waitFor({ state: 'visible', timeout: 30000 })
  const rows = pendingSection.locator('.edhr-batch-detail__special-node-file-row')
  await page.waitForFunction(
    ({ selector, count }) => document.querySelectorAll(selector).length === count,
    { selector: 'section[aria-label="待提交附件"] .edhr-batch-detail__special-node-file-row', count: expected },
    { timeout: 30000 }
  )
  assert.equal(await rows.count(), expected, `pending file row count must be ${expected}`)
  if (expected > 0) {
    await pendingSection.getByText(fileName, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    await pendingSection.getByText('待提交', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  }
}

async function exerciseUi(page, candidate) {
  const detailUrl = new URL(detailPath, config.baseUrl)
  detailUrl.searchParams.set('id', String(candidate.batchExecution.id))
  await page.goto(detailUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText(candidate.batchExecution.batchExecutionCode || candidate.batchExecution.batchCode || String(candidate.batchExecution.id), { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })

  const specialButton = page
    .locator('.edhr-batch-detail__special-process-task-group .edhr-batch-detail__process-task-group-head')
    .filter({ hasText: candidate.taskBefore.label })
    .first()
  await specialButton.waitFor({ state: 'visible', timeout: 60000 })
  await specialButton.click()

  await page.locator('.edhr-batch-detail__special-node-attachments').waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText('当前节点附件', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText('待提交附件', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText('已入账附件', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })

  const actionButtons = page.locator('.edhr-batch-detail__special-node-action-grid .edhr-batch-detail__rail-task-action')
  const uploadButton = actionButtons.filter({ hasText: '上传文件' }).first()
  const skipButton = actionButtons.filter({ hasText: '跳过节点' }).first()
  const completeButton = actionButtons.filter({ hasText: '完成节点' }).first()
  await uploadButton.waitFor({ state: 'visible', timeout: 30000 })
  await skipButton.waitFor({ state: 'visible', timeout: 30000 })
  await completeButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await uploadButton.isDisabled(), false, 'upload button must be enabled before release even when the special node is already completed')

  const fileName = `codex-edhr-special-node-attachment-${candidate.taskBefore.id}.txt`
  const firstAttachment = await uploadFileThroughUi(page, fileName, `first upload ${Date.now()}`)
  await assertPendingFileCount(page, 1, fileName)
  const secondAttachment = await uploadFileThroughUi(page, fileName, `replacement upload ${Date.now()}`)
  await assertPendingFileCount(page, 1, fileName)
  assert.notEqual(firstAttachment.uploadToken, secondAttachment.uploadToken, 'same-name replacement should keep latest upload token')

  const pendingSection = page.locator('section[aria-label="待提交附件"]').first()
  const previewPagePromise = page.context().waitForEvent('page', { timeout: 30000 })
  await pendingSection.getByRole('button', { name: '预览' }).first().click()
  const previewPage = await previewPagePromise
  await previewPage.waitForLoadState('domcontentloaded', { timeout: 30000 }).catch(() => undefined)
  assert.ok(previewPage.url() && previewPage.url() !== 'about:blank', 'preview must open a real URL')
  await previewPage.close().catch(() => undefined)

  await page.screenshot({ path: screenshotFile, fullPage: true })
  await pendingSection.getByRole('button', { name: '删除' }).first().click()
  await assertPendingFileCount(page, 0, fileName)

  const skipDisabled = await skipButton.isDisabled()
  const completeDisabled = await completeButton.isDisabled()
  if (candidate.taskBefore.submittableByCloseAction) {
    await skipButton.click()
    const skipDialog = page.locator('.el-dialog:visible').filter({ hasText: '跳过特殊节点' }).first()
    await skipDialog.waitFor({ state: 'visible', timeout: 30000 })
    await skipDialog.getByRole('button', { name: '取 消' }).first().click()
    await skipDialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => undefined)

    await completeButton.click()
    const completeDialog = page.locator('.el-dialog:visible').filter({ hasText: '完成特殊节点' }).first()
    await completeDialog.waitFor({ state: 'visible', timeout: 30000 })
    await completeDialog.getByRole('button', { name: '取 消' }).first().click()
    await completeDialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => undefined)
  }

  return {
    fileName,
    firstUploadToken: firstAttachment.uploadToken,
    secondUploadToken: secondAttachment.uploadToken,
    previewUrl: previewPage.url(),
    skipDisabled,
    completeDisabled,
    screenshotFile
  }
}

async function main() {
  ensureDir(artifactDir)
  assertLocalAuthorizedConfig()
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const mesWriteRequests = []
  page.on('request', (request) => {
    const url = request.url()
    const method = request.method().toUpperCase()
    if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS' && url.includes('/admin-api/mes/')) {
      mesWriteRequests.push({ method, url: url.replace(/([?&](?:password|token|secret|key)=)[^&]+/gi, '$1[REDACTED]') })
    }
  })

  try {
    await login(page)
    await page.goto(`${config.baseUrl}${listPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('批次执行', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    const auth = await browserAuth(page)
    const candidate = await findCandidate(page, auth)
    const uiEvidence = await exerciseUi(page, candidate)
    const detailAfter = await apiGet(page, auth, '/admin-api/mes/pro/edhr-batch-execution/get', { id: candidate.batchExecution.id })
    const taskAfter = (detailAfter.tasks || []).find((task) => task.id === candidate.taskBefore.id)
    assert.ok(taskAfter, 'verified task must still exist after UI exercise')
    assert.equal(Number(taskAfter.status), Number(candidate.taskBefore.status), 'E2E must not submit skip/complete and must not change task status')
    assert.equal(taskAfter.specialPayloadJson || '', candidate.taskBefore.specialPayloadJson || '', 'E2E must not persist pending attachment into audit payload without skip/complete')
    const forbiddenStatusWrites = mesWriteRequests.filter((request) =>
      request.url.includes('/task/special-node/skip') || request.url.includes('/task/special-node/complete')
    )
    assert.equal(forbiddenStatusWrites.length, 0, 'E2E must not submit skip or complete endpoints')

    const evidence = {
      tenant: config.tenant,
      username: config.username,
      candidate,
      uiEvidence,
      mesWriteRequests,
      statusAfter: taskAfter.status,
      specialPayloadUnchanged: true
    }
    writeResult('PASS', evidence)
    await context.close()
    await browser.close()
    console.log(`PASS: yudao admin special node attachment E2E batch=${candidate.batchExecution.id} task=${candidate.taskBefore.id}`)
  } catch (error) {
    await page.screenshot({ path: failureScreenshotFile, fullPage: true }).catch(() => undefined)
    writeResult('FAIL', {
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined,
      mesWriteRequests,
      failureScreenshotFile: fs.existsSync(failureScreenshotFile) ? failureScreenshotFile : undefined
    })
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
    throw error
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
