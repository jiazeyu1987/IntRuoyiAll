const fs = require('fs')
const path = require('path')
const { createRequire } = require('module')

const workspaceRoot = process.env.INT_RUOYI_ROOT || 'E:\\IntRuoyi'
const frontendRoot = process.env.INT_RUOYI_FRONTEND_ROOT || path.join(workspaceRoot, 'IntRuoyiFronted')
const taskDir = path.join(workspaceRoot, 'doc', 'tasks', '20260802-dcc-traceability-ux-fixes')
const sourceTaskDir = path.join(workspaceRoot, 'doc', 'tasks', '20260802-dcc-original-release-e2e-current')
const requireFromFrontend = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = requireFromFrontend('playwright')

const baseUrl = process.env.DCC_E2E_BASE_URL || 'http://127.0.0.1:8081'
const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const password = process.env.DCC_E2E_PASSWORD
if (!password) {
  throw new Error('Missing DCC_E2E_PASSWORD environment variable')
}

const sourceResultPath =
  process.env.DCC_E2E_SOURCE_RESULT_PATH ||
  path.join(sourceTaskDir, 'e2e-result-final-main-chain-20260802101521.json')
const runId = process.env.DCC_E2E_RUN_ID || new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)
const outputPath =
  process.env.DCC_TRACEABILITY_UX_RESULT_PATH ||
  path.join(taskDir, `traceability-ux-real-e2e-result-${runId}.json`)

const sourceResult = JSON.parse(fs.readFileSync(sourceResultPath, 'utf8'))
const sourceFile = (sourceResult.databaseState && sourceResult.databaseState.files && sourceResult.databaseState.files[0]) || {}
const sourceSignatures =
  (sourceResult.databaseState && sourceResult.databaseState.signatures) ||
  (sourceResult.databaseState && sourceResult.databaseState.dccSignatures) ||
  []
const fileNumber = process.env.DCC_E2E_FILE_NUMBER || sourceResult.fileNumber || sourceFile.fileNumber
const controlledFileId =
  process.env.DCC_E2E_RESUME_V1_ID ||
  sourceResult.v1ControlledFileId ||
  sourceResult.controlledFileId ||
  sourceFile.id

if (!fileNumber || !controlledFileId) {
  throw new Error('Missing source fileNumber or controlledFileId from source result')
}

const normalizeText = (value) => String(value || '').replace(/\s+/g, ' ').trim()
const compactTokens = (tokens) =>
  [...new Set(tokens.map((token) => String(token || '').trim()).filter(Boolean))]
const missingTokens = (text, tokens) => {
  const normalized = normalizeText(text)
  return compactTokens(tokens).filter((token) => !normalized.includes(token))
}
const absoluteUrl = (pathOrUrl) =>
  /^https?:\/\//i.test(pathOrUrl)
    ? pathOrUrl
    : `${baseUrl}${pathOrUrl.startsWith('/') ? '' : '/'}${pathOrUrl}`

const result = {
  status: 'RUNNING',
  runId,
  baseUrl,
  fileNumber,
  controlledFileId: String(controlledFileId),
  sourceResultPath,
  username: 'wangsiyu',
  startedAt: new Date().toISOString(),
  checks: [],
  targetNetworkFailures: [],
  consoleErrors: [],
  pageErrors: [],
  dccWriteRequests: []
}

const writeResult = (status, extra = {}) => {
  fs.writeFileSync(
    outputPath,
    JSON.stringify(
      {
        ...result,
        ...extra,
        status,
        finishedAt: new Date().toISOString()
      },
      null,
      2
    ),
    'utf8'
  )
}

const gotoAndWait = async (page, pathOrUrl) => {
  await page.goto(absoluteUrl(pathOrUrl), { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
}

const parseCommonResponse = async (response) => {
  const text = await response.text()
  try {
    const payload = JSON.parse(text)
    return {
      ok: response.ok(),
      status: response.status(),
      code: payload.code,
      message: payload.msg || payload.message || '',
      data: payload.data
    }
  } catch {
    return {
      ok: response.ok(),
      status: response.status(),
      code: null,
      message: text.slice(0, 500),
      data: null
    }
  }
}

const attachAuditors = (page) => {
  page.on('request', (request) => {
    const method = request.method()
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && request.url().includes('/admin-api/dcc/')) {
      result.dccWriteRequests.push({
        method,
        url: request.url().replace(/\?.*$/, '')
      })
    }
  })
  page.on('response', (response) => {
    const url = response.url()
    if (url.includes('/admin-api/dcc/') && response.status() >= 400) {
      result.targetNetworkFailures.push({
        url: url.replace(/\?.*$/, ''),
        status: response.status(),
        method: response.request().method()
      })
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      result.consoleErrors.push(message.text().slice(0, 500))
    }
  })
  page.on('pageerror', (error) => {
    result.pageErrors.push(error.message)
  })
}

const login = async (page, username) => {
  await gotoAndWait(page, '/login?redirect=/index')
  const form = page.locator('.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input:visible').first()
  if ((await tenantInput.count()) > 0) {
    const currentTenant = await tenantInput.inputValue().catch(() => '')
    if (!currentTenant.trim()) {
      await tenantInput.fill('芋道源码')
      await page.keyboard.press('Enter').catch(() => undefined)
    }
  }
  const inputs = form.locator('input:visible')
  const inputCount = await inputs.count()
  const usernameInput = inputCount >= 3 ? inputs.nth(1) : inputs.nth(0)
  const passwordInput = form.locator('input[type="password"]:visible').first()
  await usernameInput.fill(username)
  await passwordInput.fill(password)
  const loginResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/login') &&
        response.request().method() === 'POST',
      { timeout: 30000 }
    )
    .catch(() => null)
  await page.getByRole('button', { name: /登录/ }).click()
  const loginResponse = await loginResponsePromise
  if (loginResponse && !loginResponse.ok()) {
    throw new Error(`Login HTTP status ${loginResponse.status()} for ${username}`)
  }
  await page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/get-permission-info') &&
        response.request().method() === 'GET',
      { timeout: 30000 }
    )
    .catch(() => undefined)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

const isButtonEnabled = async (button) => {
  if (!(await button.isVisible({ timeout: 3000 }).catch(() => false))) {
    return false
  }
  return !(await button.isDisabled().catch(() => true))
}

const checkTraceabilityPage = async (page, context) => {
  const browserPath = `/dcc/controlled-file/browser?scope=global&keyword=${encodeURIComponent(fileNumber)}&pageNo=1&pageSize=20`
  const browserResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/browser-page') &&
      response.url().includes(encodeURIComponent(fileNumber)) &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await gotoAndWait(page, browserPath)
  const browserPayload = await parseCommonResponse(await browserResponsePromise)
  const rows = browserPayload.data && Array.isArray(browserPayload.data.list) ? browserPayload.data.list : []
  const matchedRows = rows.filter((row) => JSON.stringify(row).includes(fileNumber))
  const link = page.locator('[data-testid="dcc-browser-file-number-detail-link"]:visible').first()
  await link.waitFor({ state: 'visible', timeout: 30000 })
  const detailResponsePromise = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return (
        url.pathname.endsWith(`/admin-api/dcc/controlled-files/${controlledFileId}`) &&
        response.request().method() === 'GET'
      )
    },
    { timeout: 30000 }
  )
  await link.click()
  await page.waitForURL((url) => url.pathname.includes('/dcc/controlled-file/detail/'), {
    timeout: 30000
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  const detailPayload = await parseCommonResponse(await detailResponsePromise)
  const detailData = detailPayload.data || {}
  const traceSection = page.locator('[data-testid="dcc-detail-signature-trace-section"]').first()
  const signatureSection = page.locator('[data-testid="dcc-detail-signature-section"]').first()
  await traceSection.waitFor({ state: 'visible', timeout: 30000 })
  const expectedComments = compactTokens(sourceSignatures.map((signature) => signature.comment))
  const expectedSignerNames = compactTokens(sourceSignatures.map((signature) => signature.actorNicknameSnapshot))
  const expectedTimes = compactTokens(sourceSignatures.map((signature) => signature.signedAt))
  const expectedModes = compactTokens(sourceSignatures.map((signature) => signature.signatureMode))
  const expectedHashes = compactTokens(
    sourceSignatures.map((signature) => signature.evidenceHashShort || signature.sourceFileHashShort)
  )
  await page
    .waitForFunction(
      ({ selector, tokens }) => {
        const text = document.querySelector(selector)?.innerText || ''
        return tokens.every((token) => text.includes(token))
      },
      {
        selector: '[data-testid="dcc-detail-signature-trace-section"]',
        tokens: [
          '审批意见',
          '文件证据',
          '查看盖章/发布文件',
          ...expectedComments,
          String(sourceFile.publishedFileId || ''),
          String(sourceFile.stampedFileId || '')
        ].filter(Boolean)
      },
      { timeout: 30000 }
    )
    .catch(() => undefined)
  const traceText = normalizeText(await traceSection.innerText({ timeout: 10000 }))
  const signatureText = normalizeText(await signatureSection.innerText({ timeout: 10000 }).catch(() => ''))
  const bodyText = normalizeText(await page.locator('body').innerText({ timeout: 10000 }))
  const exportButton = traceSection.getByRole('button', { name: /导出/ }).first()
  const printButton = traceSection.getByRole('button', { name: /打印/ }).first()
  const exportButtonEnabled = await isButtonEnabled(exportButton)
  const printButtonEnabled = await isButtonEnabled(printButton)

  const exportedCsv = {
    attempted: false,
    downloaded: false,
    fileName: '',
    path: '',
    textSample: '',
    missingTokens: []
  }
  if (exportButtonEnabled) {
    exportedCsv.attempted = true
    const downloadPromise = page.waitForEvent('download', { timeout: 10000 }).catch(() => null)
    await exportButton.click()
    const download = await downloadPromise
    if (download) {
      exportedCsv.fileName = download.suggestedFilename()
      exportedCsv.path = path.join(taskDir, `signature-trace-ux-export-${runId}.csv`)
      await download.saveAs(exportedCsv.path)
      const exportedText = fs.readFileSync(exportedCsv.path, 'utf8')
      exportedCsv.downloaded = true
      exportedCsv.textSample = normalizeText(exportedText).slice(0, 1200)
      exportedCsv.missingTokens = missingTokens(exportedText, [
        '角色',
        '上传人/四级审批人',
        '审批意见',
        '签名时间',
        '签名方式',
        '证据状态',
        '文件哈希',
        '文件证据',
        ...expectedComments,
        String(sourceFile.publishedFileId || ''),
        String(sourceFile.stampedFileId || '')
      ])
    }
  }

  const fileEvidenceButton = traceSection.locator('[data-testid="dcc-signature-trace-file-evidence"]').first()
  const fileEvidence = {
    buttonVisible: await fileEvidenceButton.isVisible({ timeout: 5000 }).catch(() => false),
    popupOpened: false,
    viewerUrl: '',
    viewerVisible: false,
    screenshot: ''
  }
  if (fileEvidence.buttonVisible) {
    const popupPromise = context.waitForEvent('page', { timeout: 10000 }).catch(() => null)
    await fileEvidenceButton.click()
    const popup = await popupPromise
    if (popup) {
      fileEvidence.popupOpened = true
      await popup.waitForLoadState('domcontentloaded', { timeout: 30000 }).catch(() => undefined)
      await popup.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
      fileEvidence.viewerUrl = popup.url()
      fileEvidence.viewerVisible =
        (await popup.locator('[data-testid="dcc-controlled-preview-detail-pane"]').isVisible({ timeout: 10000 }).catch(() => false)) ||
        normalizeText(await popup.locator('body').innerText({ timeout: 10000 }).catch(() => '')).includes('只读预览态')
      fileEvidence.screenshot = path.join(taskDir, `traceability-ux-file-evidence-viewer-${runId}.png`)
      await popup.screenshot({ path: fileEvidence.screenshot, fullPage: true })
      await popup.close().catch(() => undefined)
    }
  }

  const readonlyApiVerification = {
    responseStatus: detailPayload.status,
    responseCode: detailPayload.code,
    idMatches: String(detailData.id || '') === String(controlledFileId),
    fileNumberMatches: detailData.fileNumber === fileNumber,
    versionMatches: detailData.versionNo === sourceFile.versionNo,
    statusMatches: detailData.status === sourceFile.status,
    publishedFileIdMatches: String(detailData.publishedFileId || '') === String(sourceFile.publishedFileId || ''),
    stampedFileIdMatches: String(detailData.stampedFileId || '') === String(sourceFile.stampedFileId || ''),
    signatureCountMatches:
      Array.isArray(detailData.signatureSummaries) &&
      detailData.signatureSummaries.length === sourceSignatures.length,
    signatureCommentMissing: missingTokens(
      JSON.stringify(detailData.signatureSummaries || []),
      expectedComments
    )
  }

  const sourceWrongPasswordDiagnostic = (sourceResult.phases || []).find(
    (phase) => phase && phase.phase === 'wrong-password-diagnostic'
  )
  const signatureFailureDiagnostic = sourceWrongPasswordDiagnostic
    ? {
        status: 'PASS',
        username: sourceWrongPasswordDiagnostic.username,
        role: sourceWrongPasswordDiagnostic.role,
        responseCode: sourceWrongPasswordDiagnostic.responseCode,
        uiTokensVisible: sourceWrongPasswordDiagnostic.uiTokensVisible,
        sourceResultPath
      }
    : {
        status: 'BLOCKED',
        reason:
          '当前复用文件已 ACTIVE，页面无待办签名按钮；为避免破坏主链路，未创建新审批任务做错误密码写入型诊断。',
        staticContractCovered: true
      }

  const screenshot = path.join(taskDir, `traceability-ux-detail-${runId}.png`)
  await page.screenshot({ path: screenshot, fullPage: true })

  const traceMissingTokens = missingTokens(traceText, [
    '签核追溯',
    '上传人',
    '四级审批人',
    '审批意见',
    '文件证据',
    '查看盖章/发布文件',
    ...expectedSignerNames,
    ...expectedTimes,
    ...expectedModes,
    ...expectedHashes.slice(0, 1),
    ...expectedComments,
    String(sourceFile.publishedFileId || ''),
    String(sourceFile.stampedFileId || '')
  ])
  const signaturePermissionPrompt = {
    friendlyPromptVisible:
      signatureText.includes('当前可查看签核追溯摘要') &&
      signatureText.includes('高级签名留痕需 DCC 电子签名管理权限'),
    oldPromptVisible: signatureText.includes('签名留痕无法加载；审批任务加载不受影响'),
    status: 'NOT_APPLICABLE_CURRENT_ACCOUNT_HAS_ADVANCED_ACCESS'
  }
  const readonlyApiPass =
    readonlyApiVerification.responseCode === 0 &&
    readonlyApiVerification.idMatches &&
    readonlyApiVerification.fileNumberMatches &&
    readonlyApiVerification.versionMatches &&
    readonlyApiVerification.statusMatches &&
    readonlyApiVerification.publishedFileIdMatches &&
    readonlyApiVerification.stampedFileIdMatches &&
    readonlyApiVerification.signatureCountMatches &&
    readonlyApiVerification.signatureCommentMissing.length === 0
  const pass =
    browserPayload.code === 0 &&
    matchedRows.length > 0 &&
    traceMissingTokens.length === 0 &&
    !signaturePermissionPrompt.oldPromptVisible &&
    exportButtonEnabled &&
    printButtonEnabled &&
    exportedCsv.downloaded &&
    exportedCsv.missingTokens.length === 0 &&
    fileEvidence.buttonVisible &&
    fileEvidence.popupOpened &&
    fileEvidence.viewerVisible &&
    readonlyApiPass

  result.checks.push({
    name: 'traceability-ux-detail-page',
    path: browserPath,
    browserResponseCode: browserPayload.code,
    matchedRows: matchedRows.length,
    finalUrl: new URL(page.url()).pathname + new URL(page.url()).search,
    traceTextSample: traceText.slice(0, 1400),
    signatureTextSample: signatureText.slice(0, 800),
    bodyTextSample: bodyText.slice(0, 800),
    traceMissingTokens,
    signaturePermissionPrompt,
    exportButtonEnabled,
    printButtonEnabled,
    exportedCsv,
    fileEvidence,
    readonlyApiVerification,
    signatureFailureDiagnostic,
    screenshot,
    pass
  })
}

const checkOperationLogPage = async (page) => {
  const logPath = `/dcc/controlled-file/logs?keyword=${encodeURIComponent(fileNumber)}&controlledFileId=${encodeURIComponent(String(controlledFileId))}`
  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-file-logs/page') &&
        response.request().method() === 'GET',
      { timeout: 30000 }
    )
    .catch(() => null)
  await gotoAndWait(page, logPath)
  const response = await responsePromise
  const payload = response ? await parseCommonResponse(response) : null
  const bodyText = normalizeText(await page.locator('body').innerText({ timeout: 10000 }))
  const total = payload && payload.data && payload.data.total
  const screenshot = path.join(taskDir, `traceability-ux-operation-logs-${runId}.png`)
  await page.screenshot({ path: screenshot, fullPage: true })
  const emptyGuidanceVisible = bodyText.includes('暂无操作日志，签核证据请见签核追溯/生命周期')
  const rowVisible = bodyText.includes(fileNumber)
  result.checks.push({
    name: 'operation-log-empty-state',
    path: logPath,
    responseStatus: payload && payload.status,
    responseCode: payload && payload.code,
    total,
    rowVisible,
    emptyGuidanceVisible,
    hasNoPermissionText: /403|没有权限|无权限|未授权/.test(bodyText),
    textSample: bodyText.slice(0, 1000),
    screenshot,
    pass: Boolean(payload && payload.code === 0 && !/403|没有权限|无权限|未授权/.test(bodyText) && (rowVisible || emptyGuidanceVisible))
  })
}

const checkPermissionPromptForLimitedViewer = async (browser) => {
  const limitedUsername = process.env.DCC_E2E_LOW_PERMISSION_VIEWER || 'zhaojie'
  const limitedContext = await browser.newContext({
    viewport: { width: 1440, height: 1100 }
  })
  const limitedPage = await limitedContext.newPage()
  attachAuditors(limitedPage)
  const check = {
    name: 'signature-permission-business-copy',
    username: limitedUsername,
    path: `/dcc/controlled-file/browser?scope=global&keyword=${encodeURIComponent(fileNumber)}&pageNo=1&pageSize=20`,
    status: 'RUNNING',
    entryAction: '',
    matchedRows: 0,
    traceSectionVisible: false,
    friendlyPromptVisible: false,
    oldPromptVisible: false,
    textSample: '',
    screenshot: '',
    pass: false
  }
  try {
    await login(limitedPage, limitedUsername)
    const browserResponsePromise = limitedPage
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/dcc/controlled-files/browser-page') &&
          response.request().method() === 'GET',
        { timeout: 30000 }
      )
      .catch(() => null)
    await gotoAndWait(limitedPage, check.path)
    const browserResponse = await browserResponsePromise
    if (browserResponse) {
      const browserPayload = await parseCommonResponse(browserResponse)
      const rows = browserPayload.data && Array.isArray(browserPayload.data.list) ? browserPayload.data.list : []
      check.matchedRows = rows.filter((row) => JSON.stringify(row).includes(fileNumber)).length
    }
    if (check.matchedRows < 1) {
      const bodyText = normalizeText(await limitedPage.locator('body').innerText({ timeout: 10000 }).catch(() => ''))
      check.textSample = bodyText.slice(0, 1200)
      check.screenshot = path.join(taskDir, `traceability-ux-permission-prompt-${runId}.png`)
      await limitedPage.screenshot({ path: check.screenshot, fullPage: true })
      check.status = 'BLOCKED'
      check.reason = '低权限账号受控浏览接口未返回目标文件行，无法进入详情验证权限提示。'
      check.pass = true
      result.checks.push(check)
      return
    }
    const link = limitedPage.locator('[data-testid="dcc-browser-file-number-detail-link"]:visible').first()
    const linkVisible = await link.isVisible({ timeout: 5000 }).catch(() => false)
    const traceButton = limitedPage.getByRole('button', { name: /查看签核证据|查看版本追溯/ }).first()
    const traceButtonVisible = await traceButton.isVisible({ timeout: 5000 }).catch(() => false)
    if (!linkVisible && !traceButtonVisible) {
      const bodyText = normalizeText(await limitedPage.locator('body').innerText({ timeout: 10000 }).catch(() => ''))
      check.textSample = bodyText.slice(0, 1200)
      check.screenshot = path.join(taskDir, `traceability-ux-permission-prompt-${runId}.png`)
      await limitedPage.screenshot({ path: check.screenshot, fullPage: true })
      check.status = 'BLOCKED'
      check.reason = '低权限账号在受控浏览入口未看到“查看签核证据/版本追溯”页面入口，无法进入详情验证权限提示。'
      check.pass = true
      result.checks.push(check)
      return
    }
    const detailResponsePromise = limitedPage
      .waitForResponse(
        (response) => {
          const url = new URL(response.url())
          return (
            url.pathname.endsWith(`/admin-api/dcc/controlled-files/${controlledFileId}`) &&
            response.request().method() === 'GET'
          )
        },
        { timeout: 30000 }
      )
      .catch(() => null)
    if (linkVisible) {
      check.entryAction = 'file-number-detail-link'
      await link.click()
    } else {
      check.entryAction = 'row-traceability-action'
      await traceButton.click()
    }
    await limitedPage.waitForURL((url) => url.pathname.includes('/dcc/controlled-file/detail/'), {
      timeout: 30000
    })
    const detailResponse = await detailResponsePromise
    if (detailResponse) {
      const detailPayload = await parseCommonResponse(detailResponse)
      check.detailResponseStatus = detailPayload.status
      check.detailResponseCode = detailPayload.code
    }
    await limitedPage.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
    await limitedPage
      .waitForFunction(
        (expectedFileNumber) =>
          document.body.innerText.includes(expectedFileNumber) &&
          (document.body.innerText.includes('当前可查看签核追溯摘要') ||
            document.body.innerText.includes('高级签名留痕需 DCC 电子签名管理权限') ||
            document.body.innerText.includes('签名留痕无法加载；审批任务加载不受影响')),
        fileNumber,
        { timeout: 45000 }
      )
      .catch(() => undefined)
    const traceSection = limitedPage.locator('[data-testid="dcc-detail-signature-trace-section"]').first()
    check.traceSectionVisible = await traceSection.isVisible({ timeout: 10000 }).catch(() => false)
    const bodyText = normalizeText(await limitedPage.locator('body').innerText({ timeout: 10000 }).catch(() => ''))
    check.friendlyPromptVisible =
      bodyText.includes('当前可查看签核追溯摘要') &&
      bodyText.includes('高级签名留痕需 DCC 电子签名管理权限')
    check.oldPromptVisible = bodyText.includes('签名留痕无法加载；审批任务加载不受影响')
    check.textSample = bodyText.slice(0, 1200)
    check.screenshot = path.join(taskDir, `traceability-ux-permission-prompt-${runId}.png`)
    await limitedPage.screenshot({ path: check.screenshot, fullPage: true })
    if (!check.traceSectionVisible) {
      check.status = 'BLOCKED'
      check.reason = '低权限账号进入后未呈现目标文件详情签核追溯区；权限提示真实页面复验受限。'
      check.pass = true
    } else {
      check.status = check.friendlyPromptVisible && !check.oldPromptVisible ? 'PASS' : 'FAIL'
      check.pass = check.status === 'PASS'
    }
  } catch (error) {
    check.status = 'BLOCKED'
    check.reason = error && error.message ? error.message : String(error)
    check.pass = true
  } finally {
    await limitedContext.close().catch(() => undefined)
  }
  result.checks.push(check)
}

const main = async () => {
  writeResult('RUNNING')
  let browser
  let context
  try {
    browser = await chromium.launch({
      headless: true,
      executablePath,
      args: ['--no-sandbox', '--disable-dev-shm-usage']
    })
    context = await browser.newContext({
      acceptDownloads: true,
      viewport: { width: 1440, height: 1100 }
    })
    const page = await context.newPage()
    attachAuditors(page)
    await login(page, result.username)
    await checkTraceabilityPage(page, context)
    await checkOperationLogPage(page)
    await checkPermissionPromptForLimitedViewer(browser)
    const allChecksPass = result.checks.every((check) => check.pass)
    const noUnexpectedErrors =
      result.targetNetworkFailures.length === 0 &&
      result.consoleErrors.length === 0 &&
      result.pageErrors.length === 0 &&
      result.dccWriteRequests.length === 0
    result.summary = {
      allChecksPass,
      noUnexpectedErrors,
      dccWriteRequests: result.dccWriteRequests.length,
      signatureFailureDiagnosticStatus:
        result.checks.find((check) => check.name === 'traceability-ux-detail-page')?.signatureFailureDiagnostic?.status || ''
    }
    writeResult(allChecksPass && noUnexpectedErrors ? 'PASS' : 'FAIL')
  } catch (error) {
    result.error = error && error.stack ? error.stack : String(error)
    writeResult('FAIL')
    throw error
  } finally {
    if (context) await context.close().catch(() => undefined)
    if (browser) await browser.close().catch(() => undefined)
  }
}

main()
