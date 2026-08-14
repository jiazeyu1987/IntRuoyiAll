const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const WORKSPACE_ROOT = path.resolve(__dirname, '..', '..', '..')
const FRONTEND_ROOT = path.join(WORKSPACE_ROOT, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(FRONTEND_ROOT, 'package.json'))
const { chromium } = frontendRequire('playwright')

const TASK_ID = '20260804-bpm-dcc-approval-preview-pane'
const OUTPUT_DIR = path.join(WORKSPACE_ROOT, 'output', 'playwright', TASK_ID)
const RESULT_PATH = path.join(OUTPUT_DIR, 'bpm-dcc-approval-preview-real-result.json')
const SUCCESS_SCREENSHOT = path.join(OUTPUT_DIR, 'bpm-dcc-approval-preview-real.png')
const FAILURE_SCREENSHOT = path.join(OUTPUT_DIR, 'bpm-dcc-approval-preview-real-failed.png')

const VIEW_PATHS = {
  TODO: '/approval-center/todo',
  DONE: '/approval-center/done',
  MY_INITIATED: '/approval-center/my-initiated',
  CC: '/approval-center/cc'
}

function readDotEnv(filePath) {
  if (!fs.existsSync(filePath)) return {}
  const result = {}
  for (const rawLine of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const separatorIndex = line.indexOf('=')
    if (separatorIndex < 0) continue
    const key = line.slice(0, separatorIndex).trim()
    let value = line.slice(separatorIndex + 1).trim()
    value = value.replace(/^['"]|['"]$/g, '').trim()
    result[key] = value
  }
  return result
}

const baseEnv = readDotEnv(path.join(FRONTEND_ROOT, '.env'))
const localEnv = readDotEnv(path.join(FRONTEND_ROOT, '.env.local'))

const config = {
  baseUrl: (
    process.env.BPM_DCC_APPROVAL_PREVIEW_E2E_BASE_URL ||
    `http://127.0.0.1:${localEnv.VITE_PORT || baseEnv.VITE_PORT || '8081'}`
  ).replace(/\/+$/, ''),
  backendUrl: (process.env.BPM_DCC_APPROVAL_PREVIEW_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
  tenant: process.env.BPM_DCC_APPROVAL_PREVIEW_E2E_TENANT || baseEnv.VITE_APP_DEFAULT_LOGIN_TENANT || '',
  username: process.env.BPM_DCC_APPROVAL_PREVIEW_E2E_USERNAME || baseEnv.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
  password: process.env.BPM_DCC_APPROVAL_PREVIEW_E2E_PASSWORD || baseEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
  headed: process.env.BPM_DCC_APPROVAL_PREVIEW_E2E_HEADED === '1',
  timeout: Number(process.env.BPM_DCC_APPROVAL_PREVIEW_E2E_TIMEOUT || 90000),
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    (fs.existsSync('C:/Program Files/Google/Chrome/Application/chrome.exe')
      ? 'C:/Program Files/Google/Chrome/Application/chrome.exe'
      : chromium.executablePath())
}

const evidence = {
  taskId: TASK_ID,
  startedAt: new Date().toISOString(),
  baseUrl: config.baseUrl,
  backendUrl: config.backendUrl,
  tenant: config.tenant,
  username: config.username,
  status: 'FAIL',
  candidateViews: [],
  selectedCandidate: null,
  approvalCenterRowVisible: false,
  processDetail: null,
  dccSummary: null,
  targetResponses: [],
  writeRequests: [],
  targetNetworkFailures: [],
  nonTargetNetworkFailures: [],
  consoleErrors: [],
  pageErrors: [],
  screenshots: {}
}

function ensureOutputDir() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

function writeEvidence() {
  ensureOutputDir()
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

function assertPrerequisites() {
  const front = new URL(config.baseUrl)
  const back = new URL(config.backendUrl)
  assert.match(front.hostname, /^(localhost|127\.0\.0\.1)$/, `frontend must be local: ${config.baseUrl}`)
  assert.match(back.hostname, /^(localhost|127\.0\.0\.1)$/, `backend must be local: ${config.backendUrl}`)
  assert.ok(config.tenant, 'login tenant is missing from frontend .env')
  assert.ok(config.username, 'login username is missing from frontend .env')
  assert.ok(config.password, 'login password is missing from frontend .env')
  assert.ok(fs.existsSync(config.executablePath), `Chrome executable is missing: ${config.executablePath}`)
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined)
  await page.waitForTimeout(700)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible().catch(() => false)) && !(await input.isDisabled().catch(() => false))) {
      await input.fill('')
      await input.fill(value)
      return
    }
  }
  throw new Error(`missing visible login input: ${label}`)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill('')
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: config.timeout })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', VIEW_PATHS.TODO)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form)
  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"], input[placeholder="请输入密码"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  assert.ok(loginPayload && [0, 200].includes(loginPayload.code), `login business code ${loginPayload && loginPayload.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: config.timeout })
}

function isLocalTargetUrl(url) {
  try {
    const parsed = new URL(url)
    return ['127.0.0.1', 'localhost'].includes(parsed.hostname)
  } catch {
    return url.includes('/admin-api/')
  }
}

function isTargetVerificationUrl(url) {
  return (
    (url.includes('/admin-api/approval-center/tasks/page') && url.includes('moduleCode=DCC')) ||
    url.includes('/admin-api/bpm/process-instance/get-approval-detail') ||
    /\/admin-api\/dcc\/controlled-files\/[^/]+(?:[?#]|$)/.test(url) ||
    /\/admin-api\/dcc\/controlled-files\/[^/]+\/preview-metadata/.test(url)
  )
}

function isWriteMethod(method) {
  return !['GET', 'HEAD', 'OPTIONS'].includes(method)
}

function isApprovalCenterPageResponse(response, viewType) {
  if (response.request().method() !== 'GET') return false
  const url = response.url()
  return (
    url.includes('/admin-api/approval-center/tasks/page') &&
    url.includes(`viewType=${viewType}`) &&
    url.includes('moduleCode=DCC')
  )
}

function isApprovalDetailResponse(response, processInstanceId) {
  if (response.request().method() !== 'GET') return false
  const url = response.url()
  return (
    url.includes('/admin-api/bpm/process-instance/get-approval-detail') &&
    url.includes(`processInstanceId=${encodeURIComponent(processInstanceId)}`)
  )
}

function isControlledFileDetailResponse(response, controlledFileId) {
  if (response.request().method() !== 'GET') return false
  const pathname = new URL(response.url()).pathname
  return pathname.endsWith(`/dcc/controlled-files/${controlledFileId}`)
}

async function parseJson(response) {
  return response.json().catch(() => null)
}

async function getVisibleBodyRows(page) {
  return page.evaluate(() =>
    Array.from(document.querySelectorAll('.approval-center__table .el-table__body-wrapper tbody tr'))
      .filter((row) => {
        const rect = row.getBoundingClientRect()
        const style = window.getComputedStyle(row)
        return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
      })
      .map((row) => (row.textContent || '').replace(/\s+/g, ' ').trim())
  )
}

function resolveCandidateFromRows(rows) {
  return rows.find((row) => row && row.moduleCode === 'DCC' && row.processInstanceId && row.businessKey && !row.businessDeleted)
}

async function findApprovalCandidate(page) {
  for (const viewType of ['TODO', 'DONE', 'MY_INITIATED', 'CC']) {
    const targetPath = VIEW_PATHS[viewType]
    const url = new URL(targetPath, config.baseUrl)
    url.searchParams.set('moduleCode', 'DCC')
    const responsePromise = page.waitForResponse(
      (response) => isApprovalCenterPageResponse(response, viewType),
      { timeout: config.timeout }
    )
    await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: config.timeout })
    const response = await responsePromise
    const payload = await parseJson(response)
    assert.equal(response.ok(), true, `approval-center ${viewType} HTTP ${response.status()}`)
    assert.ok(payload && [0, 200].includes(payload.code), `approval-center ${viewType} business code ${payload && payload.code}`)
    const rows = payload.data?.list || []
    const total = payload.data?.total || 0
    const visibleRows = await getVisibleBodyRows(page)
    const candidate = resolveCandidateFromRows(rows)
    evidence.candidateViews.push({
      viewType,
      path: targetPath,
      total,
      rowCount: rows.length,
      visibleRowCount: visibleRows.length,
      candidate: candidate
        ? {
            id: candidate.id,
            businessTitle: candidate.businessTitle,
            businessCode: candidate.businessCode,
            businessKey: candidate.businessKey,
            processInstanceId: candidate.processInstanceId,
            sourceTaskId: candidate.sourceTaskId
          }
        : null
    })
    if (candidate) {
      const expectedText = candidate.businessCode || candidate.businessTitle || candidate.businessKey
      evidence.approvalCenterRowVisible = visibleRows.some((row) => row.includes(String(expectedText)))
      return { viewType, candidate, visibleRows }
    }
  }
  return null
}

async function tryOpenBpmDetail(page, candidateContext) {
  const { viewType, candidate } = candidateContext
  const detailUrl = new URL('/bpm/process-instance/detail', config.baseUrl)
  detailUrl.searchParams.set('id', candidate.processInstanceId)
  if (candidate.sourceTaskId) {
    detailUrl.searchParams.set('taskId', candidate.sourceTaskId)
  }

  const approvalDetailResponsePromise = page.waitForResponse(
    (response) => isApprovalDetailResponse(response, candidate.processInstanceId),
    { timeout: config.timeout }
  )
  const dccDetailResponsePromise = page.waitForResponse(
    (response) => {
      if (response.request().method() !== 'GET') return false
      const pathname = new URL(response.url()).pathname
      return /\/admin-api\/dcc\/controlled-files\/\d+$/.test(pathname)
    },
    { timeout: config.timeout }
  )
  await page.goto(detailUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  const approvalDetailResponse = await approvalDetailResponsePromise
  const approvalDetailPayload = await parseJson(approvalDetailResponse)
  assert.equal(approvalDetailResponse.ok(), true, `approval detail HTTP ${approvalDetailResponse.status()}`)
  assert.ok(
    approvalDetailPayload && [0, 200].includes(approvalDetailPayload.code),
    `approval detail business code ${approvalDetailPayload && approvalDetailPayload.code}`
  )
  const detail = approvalDetailPayload.data || {}
  const processDefinition = detail.processDefinition || {}
  const processInstance = detail.processInstance || {}
  const customPath = String(processDefinition.formCustomViewPath || '')
  evidence.processDetail = {
    sourceViewType: viewType,
    url: page.url(),
    processInstanceId: candidate.processInstanceId,
    taskId: candidate.sourceTaskId || '',
    businessKey: processInstance.businessKey || candidate.businessKey,
    processName: processInstance.name,
    status: processInstance.status,
    formType: processDefinition.formType,
    formCustomViewPath: customPath,
    activityNodeCount: (detail.activityNodes || []).length,
    todoTaskPresent: Boolean(detail.todoTask)
  }
  if (!customPath.includes('dcc/controlled-file/detail')) {
    return false
  }

  const controlledFileId = String(processInstance.businessKey || candidate.businessKey || '').trim()
  assert.match(controlledFileId, /^\d+$/, `DCC process businessKey must be numeric, got ${controlledFileId}`)
  const compactSummary = page.locator('[data-testid="bpm-dcc-approval-compact-summary"]').first()
  await compactSummary.waitFor({ state: 'visible', timeout: config.timeout })
  const dccDetailResponse = await dccDetailResponsePromise
  assert.equal(
    isControlledFileDetailResponse(dccDetailResponse, controlledFileId),
    true,
    `DCC controlled file detail response must match businessKey ${controlledFileId}`
  )
  const dccDetailPayload = await parseJson(dccDetailResponse)
  assert.equal(dccDetailResponse.ok(), true, `DCC controlled file detail HTTP ${dccDetailResponse.status()}`)
  assert.ok(
    dccDetailPayload && [0, 200].includes(dccDetailPayload.code),
    `DCC controlled file detail business code ${dccDetailPayload && dccDetailPayload.code}`
  )

  const previewPane = page.locator('[data-testid="bpm-dcc-approval-file-preview"]').first()
  await previewPane.waitFor({ state: 'visible', timeout: config.timeout })
  await previewPane.locator('.protected-viewer-shell').first().waitFor({ state: 'visible', timeout: config.timeout })
  await previewPane.locator('.protected-viewer-title').first().waitFor({ state: 'visible', timeout: config.timeout })
  await settle(page)

  const domState = await page.evaluate((expectedProcessInstanceId) => {
    const isVisible = (element) => {
      if (!(element instanceof HTMLElement)) return false
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    }
    const preview = document.querySelector('[data-testid="bpm-dcc-approval-file-preview"]')
    const summary = document.querySelector('[data-testid="bpm-dcc-approval-compact-summary"]')
    const bodyText = document.body.innerText || ''
    return {
      bodyTextSample: bodyText.slice(0, 1200),
      oldTechnicalHeaderVisible: bodyText.includes(`编号：${expectedProcessInstanceId}`),
      oldJumpPromptVisible: bodyText.includes('进入文控审批处理页'),
      compactSummaryVisible: Boolean(summary && isVisible(summary)),
      previewPaneVisible: Boolean(preview && isVisible(preview)),
      protectedViewerVisible: Boolean(preview?.querySelector('.protected-viewer-shell')),
      protectedViewerTitle:
        preview?.querySelector('.protected-viewer-title')?.textContent?.replace(/\s+/g, ' ').trim() || '',
      previewErrorText:
        preview?.querySelector('.el-alert--error')?.textContent?.replace(/\s+/g, ' ').trim() || '',
      timelineVisible: Array.from(document.querySelectorAll('.el-timeline')).some(isVisible),
      operationButtonTexts: Array.from(document.querySelectorAll('.processInstance-wrap-main button'))
        .filter(isVisible)
        .map((button) => (button.textContent || '').replace(/\s+/g, ' ').trim())
        .filter(Boolean)
    }
  }, candidate.processInstanceId)

  assert.equal(domState.compactSummaryVisible, true, 'DCC compact approval summary must be visible')
  assert.equal(domState.previewPaneVisible, true, 'DCC approval red-box preview pane must be visible')
  assert.equal(domState.protectedViewerVisible, true, 'ProtectedPdfViewer shell must be embedded in the red-box pane')
  assert.equal(domState.oldTechnicalHeaderVisible, false, 'old yellow-box technical process id header must not be visible')
  assert.equal(domState.oldJumpPromptVisible, false, 'old DCC jump prompt must not be visible')
  assert.equal(domState.timelineVisible, true, 'right-side process timeline must remain visible')

  evidence.dccSummary = {
    controlledFileId,
    fileNumber: dccDetailPayload.data?.fileNumber,
    title: dccDetailPayload.data?.title,
    versionNo: dccDetailPayload.data?.versionNo,
    domState
  }
  evidence.screenshots.success = SUCCESS_SCREENSHOT
  await page.screenshot({ path: SUCCESS_SCREENSHOT, fullPage: true })
  return true
}

async function main() {
  assertPrerequisites()
  ensureOutputDir()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1680, height: 1050 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(config.timeout)
  page.setDefaultNavigationTimeout(config.timeout)

  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    const item = { method: request.method(), url: request.url(), failure: request.failure()?.errorText || '' }
    if (!isLocalTargetUrl(item.url)) return
    if (isTargetVerificationUrl(item.url)) {
      evidence.targetNetworkFailures.push(item)
    } else {
      evidence.nonTargetNetworkFailures.push(item)
    }
  })
  page.on('request', (request) => {
    const item = { method: request.method(), url: request.url() }
    if (isLocalTargetUrl(item.url) && isWriteMethod(item.method) && item.url.includes('/admin-api/')) {
      if (!item.url.includes('/system/auth/login')) evidence.writeRequests.push(item)
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (
      url.includes('/admin-api/approval-center/tasks/page') ||
      url.includes('/admin-api/bpm/process-instance/get-approval-detail') ||
      /\/admin-api\/dcc\/controlled-files\/[^/]+(?:[?#]|$)/.test(url) ||
      /\/admin-api\/dcc\/controlled-files\/[^/]+\/preview-metadata/.test(url)
    ) {
      const payload = await parseJson(response)
      evidence.targetResponses.push({
        method: response.request().method(),
        status: response.status(),
        url,
        businessCode: payload && payload.code
      })
    }
  })

  try {
    await login(page)
    const candidateContext = await findApprovalCandidate(page)
    if (!candidateContext) {
      throw new Error('E2E_BLOCKED: no DCC approval center rows with processInstanceId/businessKey in TODO/DONE/MY_INITIATED/CC')
    }
    evidence.selectedCandidate = {
      viewType: candidateContext.viewType,
      id: candidateContext.candidate.id,
      businessTitle: candidateContext.candidate.businessTitle,
      businessCode: candidateContext.candidate.businessCode,
      businessKey: candidateContext.candidate.businessKey,
      processInstanceId: candidateContext.candidate.processInstanceId,
      sourceTaskId: candidateContext.candidate.sourceTaskId
    }
    assert.equal(evidence.approvalCenterRowVisible, true, 'selected DCC approval row must be visible in approval center table')
    const opened = await tryOpenBpmDetail(page, candidateContext)
    assert.equal(opened, true, 'selected process instance must use the DCC controlled-file custom BPM form')
    assert.deepEqual(evidence.pageErrors, [], 'page must not throw runtime errors')
    assert.deepEqual(evidence.targetNetworkFailures, [], 'target local requests must not fail')
    assert.deepEqual(evidence.writeRequests, [], 'read-only BPM DCC preview E2E must not send target write requests')
    evidence.status = 'PASS'
    evidence.finishedAt = new Date().toISOString()
    writeEvidence()
    console.log(`PASS: BPM DCC approval preview real E2E -> ${RESULT_PATH}`)
  } catch (error) {
    evidence.status = String(error.message || '').startsWith('E2E_BLOCKED') ? 'BLOCKED' : 'FAIL'
    evidence.error = error.stack || error.message
    evidence.finishedAt = new Date().toISOString()
    evidence.screenshots.failure = FAILURE_SCREENSHOT
    await page.screenshot({ path: FAILURE_SCREENSHOT, fullPage: true }).catch(() => undefined)
    writeEvidence()
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(`${evidence.status}: BPM DCC approval preview real E2E -> ${error.stack || error.message}`)
  process.exit(1)
})
