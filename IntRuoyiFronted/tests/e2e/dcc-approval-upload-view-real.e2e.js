const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_ID = '20260804-dcc-approval-upload-view'
const OUTPUT_DIR = path.join(WORKSPACE_ROOT, 'output', 'playwright', TASK_ID)
const EVIDENCE_PATH = path.join(OUTPUT_DIR, 'dcc-approval-upload-view-real-evidence.json')
const SUCCESS_SCREENSHOT = path.join(OUTPUT_DIR, 'dcc-approval-upload-view-real.png')
const FAILURE_SCREENSHOT = path.join(OUTPUT_DIR, 'dcc-approval-upload-view-real-failed.png')
const TARGET_PATH = '/approval-center/todo?moduleCode=DCC&viewType=TODO'

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  const result = {}
  const content = fs.readFileSync(filePath, 'utf8')
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) {
      continue
    }
    const separatorIndex = trimmed.indexOf('=')
    if (separatorIndex <= 0) {
      continue
    }
    const key = trimmed.slice(0, separatorIndex).trim()
    let value = trimmed.slice(separatorIndex + 1).trim()
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1)
    }
    result[key] = value
  }
  return result
}

const baseEnv = readEnvFile(path.join(FRONTEND_ROOT, '.env'))
const localEnv = readEnvFile(path.join(FRONTEND_ROOT, '.env.local'))
const BASE_URL = (
  process.env.DCC_APPROVAL_UPLOAD_VIEW_E2E_BASE_URL ||
  `http://127.0.0.1:${localEnv.VITE_PORT || baseEnv.VITE_PORT || '8081'}`
).replace(/\/+$/, '')
const TENANT =
  process.env.DCC_APPROVAL_UPLOAD_VIEW_E2E_TENANT ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_TENANT ||
  ''
const USERNAME =
  process.env.DCC_APPROVAL_UPLOAD_VIEW_E2E_USERNAME ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_USERNAME ||
  ''
const PASSWORD =
  process.env.DCC_APPROVAL_UPLOAD_VIEW_E2E_PASSWORD ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD ||
  ''

function ensureOutputDir() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

function writeEvidence(evidence) {
  ensureOutputDir()
  fs.writeFileSync(EVIDENCE_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

function assertPrerequisites() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/, 'E2E must target local frontend only')
  assert.ok(TENANT, 'default login tenant is missing from env')
  assert.ok(USERNAME, 'default login username is missing from env')
  assert.ok(PASSWORD, 'default login password is missing from env')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(1000)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload?.code), `login business code ${loginPayload?.code}: ${loginPayload?.msg || ''}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function parseJsonResponse(response) {
  try {
    return await response.json()
  } catch {
    return null
  }
}

async function collectVisibleState(page) {
  return page.evaluate(() => {
    const isVisible = (element) => {
      if (!(element instanceof HTMLElement)) {
        return false
      }
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
    }
    const byTestId = (testId) => document.querySelector(`[data-testid="${testId}"]`)
    const bodyRows = Array.from(document.querySelectorAll('.approval-center__table .el-table__body-wrapper tbody tr'))
      .filter(isVisible)
      .map((row) => (row.textContent || '').replace(/\s+/g, ' ').trim())
    const markerVisibility = {}
    for (const marker of [
      'dcc-detail-lifecycle-timeline',
      'dcc-detail-route-snapshot-section',
      'dcc-detail-version-history',
      'dcc-detail-distribution-section',
      'dcc-controlled-print-records',
      'dcc-detail-training-section',
      'dcc-detail-signature-trace-section',
      'dcc-detail-signature-section',
      'dcc-detail-controlled-browser-linkage',
      'dcc-detail-publish-completion-summary'
    ]) {
      const element = byTestId(marker)
      markerVisibility[marker] = Boolean(element && isVisible(element))
    }
    const actionPanel = byTestId('dcc-approval-upload-action-panel')
    const actionButtons = Array.from(actionPanel?.querySelectorAll('button') || [])
      .filter(isVisible)
      .map((button) => (button.textContent || '').replace(/\s+/g, ' ').trim())
      .filter(Boolean)
    return {
      url: window.location.href,
      bodyRows,
      uploadViewVisible: Boolean(byTestId('dcc-approval-upload-view') && isVisible(byTestId('dcc-approval-upload-view'))),
      submissionSummaryText: byTestId('dcc-approval-upload-submission-summary')?.textContent?.replace(/\s+/g, ' ').trim() || '',
      filePreviewText: byTestId('dcc-approval-upload-file-preview')?.textContent?.replace(/\s+/g, ' ').trim() || '',
      actionPanelText: actionPanel?.textContent?.replace(/\s+/g, ' ').trim() || '',
      actionButtons,
      previewShellVisible: Boolean(document.querySelector('[data-testid="dcc-approval-upload-file-preview"] .protected-viewer-shell')),
      previewErrorText:
        document.querySelector('[data-testid="dcc-approval-upload-file-preview"] .el-alert--error')?.textContent?.replace(/\s+/g, ' ').trim() || '',
      markerVisibility
    }
  })
}

async function main() {
  assertPrerequisites()
  ensureOutputDir()

  const consoleErrors = []
  const pageErrors = []
  const failedRequests = []
  const targetResponses = []
  let approvalCenterPayload = null
  let previewMetadataPayload = null

  const browser = await chromium.launch({
    headless: process.env.DCC_APPROVAL_UPLOAD_VIEW_E2E_HEADED !== '1'
  })
  const context = await browser.newContext({ viewport: { width: 1680, height: 1000 }, locale: 'zh-CN' })
  const page = await context.newPage()

  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    failedRequests.push({ method: request.method(), url: request.url(), failure: request.failure()?.errorText || '' })
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (url.includes('/admin-api/approval-center/tasks/page')) {
      const payload = await parseJsonResponse(response)
      targetResponses.push({ label: 'approval-center-tasks-page', status: response.status(), url, code: payload?.code })
      if (url.includes('moduleCode=DCC')) {
        approvalCenterPayload = payload
      }
    }
    if (url.includes('/admin-api/dcc/controlled-files/') && url.includes('/preview-metadata')) {
      const payload = await parseJsonResponse(response)
      previewMetadataPayload = payload
      targetResponses.push({ label: 'dcc-preview-metadata', status: response.status(), url, code: payload?.code })
    }
  })

  try {
    await login(page)
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)

    if (!approvalCenterPayload) {
      const response = await page.waitForResponse(
        (item) => item.url().includes('/admin-api/approval-center/tasks/page') && item.url().includes('moduleCode=DCC'),
        { timeout: 30000 }
      )
      approvalCenterPayload = await parseJsonResponse(response)
    }
    assert.ok([0, 200].includes(approvalCenterPayload?.code), `approval-center DCC TODO code ${approvalCenterPayload?.code}: ${approvalCenterPayload?.msg || ''}`)
    const approvalRows = approvalCenterPayload?.data?.list || []
    const approvalTotal = approvalCenterPayload?.data?.total || 0
    if (!approvalRows.length || approvalTotal <= 0) {
      throw new Error(`E2E_BLOCKED: no DCC TODO rows for ${TENANT}/${USERNAME}`)
    }

    const openButton = page.getByRole('button', { name: /^打开$/ }).first()
    if (!(await openButton.count())) {
      throw new Error(`E2E_BLOCKED: DCC TODO rows exist but no visible 打开 button`)
    }

    await Promise.all([
      page.waitForURL((current) => current.pathname.includes('/dcc/controlled-file/detail/'), { timeout: 60000 }),
      openButton.click()
    ])
    await page.locator('[data-testid="dcc-approval-upload-view"]').waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('[data-testid="dcc-approval-upload-file-preview"] .protected-viewer-shell').waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('[data-testid="dcc-approval-upload-action-panel"]').waitFor({ state: 'visible', timeout: 60000 })
    await page.waitForResponse(
      (response) => response.url().includes('/admin-api/dcc/controlled-files/') && response.url().includes('/preview-metadata'),
      { timeout: 60000 }
    ).catch(() => null)
    await settle(page)

    const currentUrl = new URL(page.url())
    assert.equal(currentUrl.searchParams.get('handling'), 'approval', `detail URL must include handling=approval: ${currentUrl.href}`)
    assert.equal(currentUrl.searchParams.get('from'), 'approval-center', `detail URL must include from=approval-center: ${currentUrl.href}`)
    assert.notEqual(currentUrl.searchParams.get('viewer'), '1', `approval upload view must not be viewer=1: ${currentUrl.href}`)
    assert.notEqual(currentUrl.searchParams.get('traceability'), '1', `approval upload view must not be traceability=1: ${currentUrl.href}`)

    const state = await collectVisibleState(page)
    assert.equal(state.uploadViewVisible, true, `upload view must be visible: ${JSON.stringify(state)}`)
    assert.equal(state.previewShellVisible, true, `file preview shell must be visible: ${JSON.stringify(state)}`)
    assert.equal(state.previewErrorText, '', `file preview must not show an error: ${state.previewErrorText}`)
    assert.ok(state.submissionSummaryText.includes('提交范围'), `submission summary must include upload scope: ${state.submissionSummaryText}`)
    assert.ok(state.filePreviewText.includes('附件预览'), `file preview block must include attachment preview title: ${state.filePreviewText}`)
    for (const emptyTaskPhrase of ['暂无待处理审批任务', '当前没有待处理审批任务']) {
      assert.ok(!state.actionPanelText.includes(emptyTaskPhrase), `action panel must keep current task actions: ${state.actionPanelText}`)
    }
    assert.ok(state.actionButtons.length > 0, `action panel must expose visible task buttons: ${JSON.stringify(state)}`)
    assert.ok(
      state.actionButtons.some((buttonText) => /通过|处理|审批|签名|回退|转办|加签|驳回/.test(buttonText)),
      `action panel must expose approval handling buttons: ${JSON.stringify(state.actionButtons)}`
    )
    for (const [marker, visible] of Object.entries(state.markerVisibility)) {
      assert.equal(visible, false, `full traceability marker must be hidden in upload approval view: ${marker}`)
    }
    assert.ok([0, 200].includes(previewMetadataPayload?.code), `preview metadata must load through formal chain: ${JSON.stringify(previewMetadataPayload)}`)
    assert.equal(pageErrors.length, 0, `page errors must be empty: ${JSON.stringify(pageErrors)}`)

    await page.screenshot({ path: SUCCESS_SCREENSHOT, fullPage: true })
    const evidence = {
      status: 'PASS',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      approvalTotal,
      selectedBusinessKey: approvalRows[0]?.businessKey || approvalRows[0]?.businessId || null,
      finalUrl: page.url(),
      targetResponses,
      failedRequests,
      consoleErrors,
      pageErrors,
      screenshot: SUCCESS_SCREENSHOT,
      state
    }
    writeEvidence(evidence)
    process.stdout.write(`PASS: DCC approval upload view real E2E\n${JSON.stringify(evidence, null, 2)}\n`)
  } catch (error) {
    await page.screenshot({ path: FAILURE_SCREENSHOT, fullPage: true }).catch(() => null)
    const evidence = {
      status: String(error.message || '').startsWith('E2E_BLOCKED:') ? 'BLOCKED' : 'FAIL',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      error: error.message,
      stack: error.stack,
      targetResponses,
      failedRequests,
      consoleErrors,
      pageErrors,
      screenshot: FAILURE_SCREENSHOT,
      state: await collectVisibleState(page).catch(() => null)
    }
    writeEvidence(evidence)
    throw error
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(String(error.message || '').startsWith('E2E_BLOCKED:') ? 2 : 1)
})
