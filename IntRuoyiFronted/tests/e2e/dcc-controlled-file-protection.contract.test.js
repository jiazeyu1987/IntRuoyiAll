const assert = require('node:assert')
const fs = require('node:fs')
const { spawnSync } = require('node:child_process')
const path = require('node:path')

const scriptPath = path.resolve(__dirname, 'dcc-controlled-file-protection.e2e.js')
const profilePath = path.resolve(__dirname, 'dcc-controlled-file-protection.test-server.profile.ps1')
const envExamplePath = path.resolve(__dirname, 'dcc-controlled-file-protection.env.example.md')
const workflowApiPath = path.resolve(__dirname, '../../src/api/dcc/controlledFile/workflow.ts')
const viewerPath = path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/index.vue')
const viewerPresentationPath = path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/presentation.ts')

const baseEnv = {
  ...process.env,
  DCC_E2E_BASE_URL: 'http://172.30.30.58:8081',
  DCC_E2E_API_BASE_URL: 'http://172.30.30.58:48081',
  DCC_E2E_ALLOWED_BASE_URL_PATTERN: '^http://172\\.30\\.30\\.58:8081$',
  DCC_E2E_ENVIRONMENT_NAME: 'test',
  DCC_E2E_CONFIRM_TEST_TENANT_ONLY: 'true',
  DCC_E2E_TENANT_NAME: '测试租户',
  DCC_E2E_USERNAME: 'real-test-user-from-login-access',
  DCC_E2E_PASSWORD: 'real-test-password-from-login-access',
  DCC_E2E_RG02_UPLOAD_POLICY_READY: 'true',
  DCC_E2E_RG04_TEST_TENANT_READY: 'true'
}

function runWithEnv(extraEnv) {
  return spawnSync(process.execPath, [scriptPath], {
    cwd: path.resolve(__dirname, '../..'),
    env: { ...baseEnv, ...extraEnv },
    encoding: 'utf8'
  })
}

function outputOf(result) {
  return `${result.stdout || ''}\n${result.stderr || ''}`
}

function assertBlocked(result) {
  assert.equal(result.status, 2, outputOf(result))
  assert.match(outputOf(result), /BLOCKED: DCC controlled file protection E2E prerequisites are missing/)
}

function assertNotContains(text, token) {
  assert.equal(text.includes(token), false, `output must not contain ${token}\n${text}`)
}

function assertContains(text, token) {
  assert.ok(text.includes(token), `output must contain ${token}\n${text}`)
}

function readUtf8(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function testFixedUploadPurposeDoesNotRequireMissingUiSelector() {
  const result = runWithEnv({
    DCC_E2E_TC006_FIXED_PURPOSE_VALUE: 'SOURCE',
    DCC_E2E_TC007_FIXED_PURPOSE_VALUE: 'SOURCE',
    DCC_E2E_TC008_FIXED_PURPOSE_VALUE: 'SOURCE'
  })
  assertBlocked(result)
  const output = outputOf(result)
  for (const prefix of ['DCC_E2E_TC006', 'DCC_E2E_TC007', 'DCC_E2E_TC008']) {
    assertNotContains(output, `${prefix}_PURPOSE_SELECTOR`)
    assertNotContains(output, `${prefix}_PURPOSE_VALUE`)
  }
}

function testUploadPurposeStillBlocksWithoutExplicitMode() {
  const result = runWithEnv({})
  assertBlocked(result)
  const output = outputOf(result)
  for (const prefix of ['DCC_E2E_TC006', 'DCC_E2E_TC007', 'DCC_E2E_TC008']) {
    assertContains(output, `${prefix}_FIXED_PURPOSE_VALUE`)
  }
}

function testTestServerProfileIsSourceBackedAndSecretFree() {
  const profile = readUtf8(profilePath)
  const envExample = readUtf8(envExamplePath)
  const e2eScript = readUtf8(scriptPath)
  const requiredFragments = [
    'DCC_E2E_BASE_URL',
    'http://172.30.30.58:8081',
    'DCC_E2E_TC001_DETAIL_PATH',
    '/dcc/controlled-file/detail/2054545668044046252',
    'DCC_E2E_TC003_DETAIL_PATH',
    '?viewer=1&from=detail',
    'DCC_E2E_TC006_FIXED_PURPOSE_VALUE',
    'DCC_E2E_TC006_CATEGORY_OPTION_SELECTOR',
    'DCC_UPLOAD_SIZE_POLICY_MISSING',
    'DCC_E2E_TC009_TEMP_VERIFY_URL',
    'SOURCE',
    'DCC_E2E_TC010_DOWNLOAD_TRIGGER_SELECTOR',
    "U '4E0B 8F7D 53D7 63A7 6587 4EF6'",
    'DCC_E2E_TC010_DOWNLOAD_CONFIRM_SELECTOR',
    "U '786E 8BA4 4E0B 8F7D'"
  ]
  for (const fragment of requiredFragments) {
    assertContains(profile, fragment)
  }
  for (const secretPattern of [/DCC_E2E_PASSWORD\s*=/, /BASE64_KEY/i, /hmac-secret/i]) {
    assert.equal(secretPattern.test(profile), false, `profile must not contain secret pattern ${secretPattern}`)
  }
  assertContains(envExample, 'test-server.profile.ps1')
  assertContains(envExample, 'Release gates are not set to true by default')
  assertContains(e2eScript, 'ACCESS_TOKEN')
  assertContains(e2eScript, 'tenant-id')
  assertContains(e2eScript, 'page.evaluate')
  assertContains(e2eScript, 'activeLoginForm')
  assertContains(e2eScript, 'input[type="password"]')
  assertNotContains(e2eScript, 'context.request.get(makeUrl(url))')
}

function testOnlyOfficeProfileAndWatermarkTraceContract() {
  const profile = readUtf8(profilePath)
  const envExample = readUtf8(envExamplePath)
  const e2eScript = readUtf8(scriptPath)
  const onlyOfficeFragments = [
    'DCC_E2E_TC005_DETAIL_PATH',
    '/dcc/controlled-file/detail/2054545668044046251?viewer=1&from=detail',
    'DCC_E2E_TC005_OFFICE_TRIGGER_SELECTOR',
    '.protected-viewer-shell',
    'DCC_E2E_TC005_OFFICE_READY_SELECTOR',
    '.onlyoffice-viewer-frame iframe',
    'DCC_E2E_TC005_FORBIDDEN_TOOLBAR_SELECTORS',
    'DCC_E2E_TC005_AUDIT_EVENT_INPUT_SELECTOR'
  ]
  for (const fragment of onlyOfficeFragments) {
    assertContains(profile, fragment)
  }
  assertContains(envExample, 'TC004 first reuses `watermarkTraceCode` from preview metadata')
  assertContains(e2eScript, 'resolveWatermarkTraceText')
  assertContains(e2eScript, 'resolveOnlyOfficeAuditEventValue')
  assertContains(e2eScript, 'resolveOnlyOfficeBadTokenUrl')
  assertContains(e2eScript, 'DCC_E2E_CASES')
  assertContains(profile, '[data-testid="protected-preview-corner-watermark"]')
  assertContains(profile, 'DCC_E2E_TC004_WATERMARK_EXPECT_TEXT')
  assertContains(profile, 'DCC_E2E_TC004_AUDIT_VERIFY_URL')
  assertContains(profile, 'watermarkTraceCode={traceText}')
  assertContains(profile, 'tenantName=122')
  assertContains(e2eScript, "verifyFinalApi(page, 'DCC_E2E_TC004_AUDIT_VERIFY_URL'")
  assertContains(e2eScript, "templateValues: { traceText }")
  assertContains(e2eScript, 'TC-E2E-004 audit result row must include the screenshot trace code')
  assertContains(e2eScript, 'onlyofficeDocumentUrl')
  assertContains(e2eScript, 'function findLatestDccResponse')
  assertContains(e2eScript, "findLatestDccResponse(captures.responses, '/preview-metadata', startIndex)")
  assertContains(e2eScript, 'const startResponseCount = captures.responses.length')
  assertContains(e2eScript, 'resolveWatermarkTraceText(captures, startResponseCount)')
  assertContains(e2eScript, 'resolveOnlyOfficeAuditEventValue(captures, startResponseCount)')
  assertContains(e2eScript, 'resolveOnlyOfficeBadTokenUrl(captures, startResponseCount)')
  assertNotContains(e2eScript, "findDccResponse(captures.responses, '/preview-metadata')")
  assertNotContains(e2eScript, "'DCC_E2E_TC004_TRACE_TEXT',")
  assertNotContains(e2eScript, "'DCC_E2E_TC005_AUDIT_EVENT_VALUE',")
  assertNotContains(e2eScript, "'DCC_E2E_TC005_BAD_TOKEN_URL',")
  assertNotContains(e2eScript, "'DCC_E2E_TC005_BAD_TOKEN_DENIED_TEXT',")
  assertNotContains(profile, 'token=')
  assertNotContains(profile, 'invalid-e2e-token')
}

function testFinalApiVerificationUsesBackendBaseUrl() {
  const profile = readUtf8(profilePath)
  const envExample = readUtf8(envExamplePath)
  const e2eScript = readUtf8(scriptPath)
  assertContains(profile, "DCC_E2E_API_BASE_URL' 'http://172.30.30.58:48081'")
  assertContains(envExample, 'DCC_E2E_API_BASE_URL')
  assertContains(e2eScript, 'DCC_E2E_API_BASE_URL')
  assertContains(e2eScript, 'function makeApiUrl')
  assertContains(e2eScript, 'authenticatedPageGet(page, makeApiUrl(url))')
  assertContains(e2eScript, 'authenticatedPagePostJson')
  assertNotContains(e2eScript, 'authenticatedPageGet(page, makeUrl(url))')
}

function testUploadLifecycleUsesRequestIdAndRealCategorySelectors() {
  const profile = readUtf8(profilePath)
  const e2eScript = readUtf8(scriptPath)
  const workflowApi = readUtf8(workflowApiPath)
  assertContains(workflowApi, "export const DCC_REQUEST_ID_HEADER = 'X-DCC-Request-Id'")
  assertContains(workflowApi, 'headers: { [DCC_REQUEST_ID_HEADER]: requestId }')
  assertContains(workflowApi, 'headers: requestId ? { [DCC_REQUEST_ID_HEADER]: requestId } : undefined')
  assertContains(workflowApi, 'DCC upload response request id mismatch')
  assertContains(e2eScript, 'resolveUploadRequestId')
  assertContains(e2eScript, 'templateValues: { requestId }')
  assertContains(e2eScript, 'DCC_E2E_TC009_REMOVE_SELECTOR')
  assertContains(e2eScript, 'postSubmitExpectFailure')
  assertContains(profile, 'DCC_E2E_TC008_CATEGORY_ID')
  assertContains(profile, 'DCC_E2E_TC008_DIRECTORY_ID')
  assertContains(profile, 'DCC_E2E_TC009_AUDIT_VERIFY_URL')
  assertContains(profile, '{requestId}')
  assertContains(profile, 'USER_DISCARDED')
}

function testPreviewWatermarkTraceCodeIsRendered() {
  const workflowApi = readUtf8(workflowApiPath)
  const viewer = readUtf8(viewerPath)
  const presentation = readUtf8(viewerPresentationPath)
  assertContains(workflowApi, 'traceCode?: string | null')
  assertContains(viewer, 'withTraceableWatermark')
  assertContains(viewer, 'metadata.watermarkTraceCode')
  assertContains(viewer, 'preview.watermark')
  assertContains(presentation, 'resolvePreviewWatermarkTraceCode')
  assertContains(presentation, 'buildPreviewWatermarkText')
  assertContains(presentation, 'traceCode')
}

function testPreviewUsesSingleTraceableMetadataEnvelope() {
  const workflowApi = readUtf8(workflowApiPath)
  const viewer = readUtf8(viewerPath)
  assertContains(workflowApi, 'metadata?: ControlledFilePreviewMetadataVO')
  assertContains(workflowApi, 'const resolvedMetadata = metadata ||')
  assertContains(viewer, 'resolvedPreviewMetadata')
  assertContains(viewer, 'previewControlledFileWithWatermark')
  assertContains(viewer, 'resolvedPreviewMetadata.value || undefined')
}

function testPreviewTransformControlsAreBoundedAndTypeScoped() {
  const viewer = readUtf8(viewerPath)
  assertContains(viewer, 'protected-viewer-transform-controls')
  assertContains(viewer, 'protected-viewer-transform-controls--sticky')
  assertContains(viewer, 'protected-viewer-transform-controls__grid')
  assertContains(viewer, 'protected-viewer-transform-controls__status')
  assertContains(viewer, 'protected-viewer-frame--transformable')
  assertContains(viewer, "['PDF', 'IMAGE'].includes(resolvedPreviewKind.value)")
  assertContains(viewer, 'MIN_VIEWER_ZOOM_PERCENT = 50')
  assertContains(viewer, 'MAX_VIEWER_ZOOM_PERCENT = 250')
  assertContains(viewer, 'VIEWER_ZOOM_STEP_PERCENT = 25')
  assertContains(viewer, 'handleZoomIn')
  assertContains(viewer, 'handleZoomOut')
  assertContains(viewer, 'handleRotateRight')
  assertContains(viewer, 'handleResetViewerTransform')
  assertContains(viewer, 'pdfPreviewTransformStyle')
  assertContains(viewer, 'getPdfPageViewportStyle')
  assertContains(viewer, 'protected-viewer-page__canvas-viewport')
  assertContains(viewer, 'protected-viewer-transform-stage')
  assertContains(viewer, 'renderPdfPages(previewPayload.bytes, currentRenderVersion, scale)')
  assertContains(viewer, 'currentPdfBytes')
  assertContains(viewer, 'clonePdfBytesForWorker')
  assertContains(viewer, 'data: clonePdfBytesForWorker(pdfBytes)')
  assertContains(viewer, 'viewerZoomPercent.value <= MIN_VIEWER_ZOOM_PERCENT')
  assertContains(viewer, 'viewerZoomPercent.value >= MAX_VIEWER_ZOOM_PERCENT')
  assertContains(viewer, 'viewerRotationDegrees.value + 90')
  assertContains(viewer, '放大')
  assertContains(viewer, '缩小')
  assertContains(viewer, '旋转')
  assertContains(viewer, '复原')
  assertContains(viewer, 'position: sticky')
  assertContains(viewer, 'align-self: flex-end')
  assertContains(viewer, 'max-height: calc(100vh - 180px)')
  assertContains(viewer, 'overscroll-behavior: contain')
  assert.ok(!viewer.includes('左旋转90°'), '控制栏不应再显示左旋转按钮')
  assert.ok(!viewer.includes('右旋转90°'), '控制栏不应再显示右旋转90°长按钮')
  assert.ok(!viewer.includes('handleRotateLeft'), '控制栏不应保留左旋处理函数')
  assert.ok(!viewer.includes('applyPdfZoomChange'), 'PDF 缩放不应每次点击后重新渲染整份 PDF')
  assert.ok(!viewer.includes('rerenderCurrentPdf'), 'PDF 缩放不应依赖重新加载 pdf.js 文档')
  assert.ok(!viewer.includes('baseScale * viewerScale.value'), 'PDF 初始渲染不应绑定缩放按钮状态')
}

function testPreviewBinaryRequestUsesHeaderBoundViewerContext() {
  const workflowApi = readUtf8(workflowApiPath)
  assertContains(workflowApi, "export const DCC_WATERMARK_TRACE_CODE_HEADER = 'X-DCC-Watermark-Trace-Code'")
  assertContains(workflowApi, '[DCC_VIEWER_TOKEN_HEADER]: resolvedMetadata.viewerToken')
  assertContains(workflowApi, '[DCC_ACCESS_EVENT_CODE_HEADER]: resolvedMetadata.accessEventCode')
  assertContains(workflowApi, '[DCC_WATERMARK_TRACE_CODE_HEADER]: resolvedMetadata.watermarkTraceCode')
  assertContains(workflowApi, '[DCC_VIEWER_TOKEN_ID_HEADER]: resolvedMetadata.viewerTokenId')
  assertContains(workflowApi, '[DCC_VIEWER_TOKEN_NONCE_HEADER]: resolvedMetadata.viewerTokenNonce')
  assertNotContains(workflowApi, 'viewerToken: resolvedMetadata.viewerToken')
}

function testScreenshotSmokeProfileUsesValidPreviewSample() {
  const profile = readUtf8(profilePath)
  const e2eScript = readUtf8(scriptPath)
  assertContains(
    profile,
    "DCC_E2E_TC015_SCREENSHOT_PATHS' '/dcc/controlled-file/logs||/dcc/controlled-file/upload||/dcc/controlled-file/detail/2054545668044046252?viewer=1&from=detail'"
  )
  assertContains(profile, "||WM-'")
  assertContains(e2eScript, 'function splitRequiredTextGroups')
  assertContains(e2eScript, 'requiredTextGroups[index]')
  assertContains(e2eScript, 'TC015 path and required text group counts must match')
  assertNotContains(profile, '2054545668044046245?viewer=1&from=detail')
}

function testOnlyOfficeBadTokenAcceptsFailFastCommonResult() {
  const e2eScript = readUtf8(scriptPath)
  assertContains(e2eScript, 'async function assertOnlyOfficeBadTokenDenied')
  assertContains(e2eScript, 'bad-token denial response')
  assertContains(e2eScript, 'JSON.parse')
  assertContains(e2eScript, 'parsed.code')
  assertContains(e2eScript, 'OnlyOffice bad token read must return a non-success failure envelope')
  assertNotContains(e2eScript, 'badTokenStatus >= 400 || deniedText > 0')
}

function testOnlyOfficeReadonlyPreviewDoesNotPromptForAnonymousCoeditName() {
  const viewer = readUtf8(path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue'))
  assertContains(viewer, "name: '受控预览'")
  assertContains(viewer, "id: 'dcc-readonly-viewer'")
  assertContains(viewer, 'user: onlyOfficeReadOnlyUser')
  assertContains(viewer, 'anonymous: {')
  assertContains(viewer, 'request: false')
}

function testEncryptedDownloadUsesResponseEvidenceHeadersInsteadOfManualPlainHash() {
  const profile = readUtf8(profilePath)
  const e2eScript = readUtf8(scriptPath)
  const workflowApi = readUtf8(workflowApiPath)
  assertContains(e2eScript, "'DCC_E2E_TC010_DOWNLOAD_CONFIRM_SELECTOR'")
  assertContains(e2eScript, "clickRequired(page, getEnv('DCC_E2E_TC010_DOWNLOAD_CONFIRM_SELECTOR'))")
  assertContains(e2eScript, 'page.waitForResponse')
  assertContains(e2eScript, 'TC-E2E-010-encrypted-download.bin')
  assertContains(e2eScript, 'function sha256Buffer')
  assertContains(workflowApi, 'crypto.getRandomValues')
  assertNotContains(workflowApi, 'crypto?.randomUUID')
  assertNotContains(workflowApi, 'Math.random')
  assertContains(profile, 'DCC_E2E_TC010_DOWNLOAD_CONFIRM_SELECTOR')
  assertNotContains(e2eScript, "'DCC_E2E_TC010_EXPECT_PLAIN_SHA256',")
  assertContains(e2eScript, "response.headers['x-dcc-plain-sha256']")
  assertContains(e2eScript, "response.headers['x-dcc-cipher-sha256']")
  assertContains(e2eScript, 'Downloaded file hash must match encrypted artifact evidence')
  assertContains(e2eScript, 'Downloaded file must not match plaintext evidence')
}

function testEncryptionFailClosedUsesRealDownloadConfirmation() {
  const profile = readUtf8(profilePath)
  const envExample = readUtf8(envExamplePath)
  const e2eScript = readUtf8(scriptPath)
  assertContains(e2eScript, "'DCC_E2E_TC011_DOWNLOAD_CONFIRM_SELECTOR'")
  assertContains(e2eScript, "clickRequired(page, getEnv('DCC_E2E_TC011_DOWNLOAD_CONFIRM_SELECTOR'))")
  assertContains(profile, 'DCC_E2E_TC011_DOWNLOAD_PATH')
  assertContains(profile, 'DCC_E2E_TC011_DOWNLOAD_CONFIRM_SELECTOR')
  assertContains(profile, 'ENCRYPTION')
  assertContains(envExample, 'DCC_E2E_TC011_DOWNLOAD_CONFIRM_SELECTOR')
}

function testAuditAuthorizationUsesRealSelectFilters() {
  const profile = readUtf8(profilePath)
  const envExample = readUtf8(envExamplePath)
  const e2eScript = readUtf8(scriptPath)
  assertContains(e2eScript, "'DCC_E2E_TC013_ACTION_OPTION_SELECTOR'")
  assertContains(e2eScript, "'DCC_E2E_TC013_RESULT_OPTION_SELECTOR'")
  assertContains(e2eScript, 'selectRequired(')
  assertContains(e2eScript, "getEnv('DCC_E2E_TC013_ACTION_SELECTOR')")
  assertContains(e2eScript, "getEnv('DCC_E2E_TC013_RESULT_SELECTOR_FIELD')")
  assertContains(profile, 'DCC_E2E_AUDITOR_USERNAME')
  assertContains(profile, 'DCC_E2E_ORDINARY_USERNAME')
  assertContains(profile, 'DCC_E2E_TC013_ACTION_OPTION_SELECTOR')
  assertContains(profile, 'DCC_E2E_TC013_RESULT_OPTION_SELECTOR')
  assertContains(envExample, 'DCC_E2E_TC013_ACTION_OPTION_SELECTOR')
  assertContains(envExample, 'DCC_E2E_TC013_RESULT_OPTION_SELECTOR')
}

function testDownloadPolicyUsesRealCapabilityBoundary() {
  const profile = readUtf8(profilePath)
  const envExample = readUtf8(envExamplePath)
  const e2eScript = readUtf8(scriptPath)
  assertContains(e2eScript, "role === 'policy'")
  assertContains(e2eScript, "'DCC_E2E_POLICY_USERNAME'")
  assertContains(e2eScript, "'DCC_E2E_POLICY_PASSWORD'")
  assertContains(e2eScript, "newLoggedInPage(session.browser, 'policy')")
  assertContains(e2eScript, "'DCC_E2E_TC012_PREFIX_DENIED_ABSENT_SELECTOR'")
  assertContains(e2eScript, 'assertNoVisible(')
  assertContains(e2eScript, "getEnv('DCC_E2E_TC012_PREFIX_DENIED_ABSENT_SELECTOR')")
  assertContains(e2eScript, "'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_VERIFY_URL'")
  assertContains(e2eScript, "'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_EXPECT_FIELDS'")
  assertContains(e2eScript, "'DCC_E2E_TC012_NO_PREFIX_ALLOWED_CONFIRM_SELECTOR'")
  assertContains(e2eScript, 'TC-E2E-012 allowed download response must be HTTP 200')
  assertContains(e2eScript, 'TC-E2E-012 allowed download response must include request id')
  assertNotContains(e2eScript, "'DCC_E2E_TC012_PREFIX_DENIED_ERROR_SELECTOR'")
  assertContains(profile, 'DCC_E2E_POLICY_USERNAME')
  assertContains(profile, 'showroomviewer')
  assertContains(profile, 'DCC_E2E_TC012_PREFIX_DENIED_PATH')
  assertContains(profile, '2054545668044046234')
  assertContains(profile, 'DCC_E2E_TC012_NO_PREFIX_ALLOWED_PATH')
  assertContains(profile, '2054545668044046252')
  assertContains(profile, 'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_EXPECT_FIELDS')
  assertContains(profile, 'canDownload=false')
  assertContains(profile, 'DCC_E2E_TC012_NO_PREFIX_ALLOWED_CONFIRM_SELECTOR')
  assertContains(profile, 'userId=910204')
  assertContains(envExample, 'DCC_E2E_POLICY_PASSWORD')
  assertContains(envExample, 'DCC_E2E_TC012_PREFIX_DENIED_ABSENT_SELECTOR')
  assertContains(envExample, 'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_VERIFY_URL')
  assertContains(envExample, 'DCC_E2E_TC012_NO_PREFIX_ALLOWED_CONFIRM_SELECTOR')
}

function testFrontendFailClosedUsesRealBrokenArtifactAndUploadBoundary() {
  const profile = readUtf8(profilePath)
  const envExample = readUtf8(envExamplePath)
  const e2eScript = readUtf8(scriptPath)
  assertContains(profile, 'DCC_E2E_TC003_FAILURE_PATH')
  assertContains(profile, '2054545668044046236?viewer=1&from=detail')
  assertContains(profile, 'DCC_E2E_TC003_FAILURE_AUDIT_VERIFY_URL')
  assertContains(e2eScript, "'DCC_E2E_TC003_FAILURE_AUDIT_VERIFY_URL'")
  assertContains(e2eScript, "'DCC_E2E_TC014_PREVIEW_AUDIT_VERIFY_URL'")
  assertContains(e2eScript, "'DCC_E2E_TC014_UPLOAD_AUDIT_VERIFY_URL'")
  assertContains(e2eScript, "'DCC_E2E_TC014_DOWNLOAD_CONFIRM_SELECTOR'")
  assertContains(e2eScript, "'DCC_E2E_TC014_DOWNLOAD_AUDIT_VERIFY_URL'")
  assertContains(e2eScript, 'TC-E2E-014 preview failure')
  assertContains(e2eScript, 'TC-E2E-014 upload fail closed')
  assertContains(e2eScript, 'TC-E2E-014 download fail closed')
  assertContains(e2eScript, "expectedFieldsEnv: 'DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_FIELDS'")
  assertNotContains(envExample, 'missing-token')
  assertNotContains(envExample, 'missing-header')
  assertNotContains(envExample, 'missing-ticket')
  assertContains(envExample, 'real-broken-preview-file-route')
  assertContains(envExample, 'real-upload-oversize-or-policy-route')
  assertContains(profile, 'DCC_E2E_TC014_DOWNLOAD_CONFIRM_SELECTOR')
  assertContains(profile, 'ENCRYPTION_CONTRACT_FAILED')
}

function testDirectLinkBoundaryPreservesLoggedInPageForAuditVerification() {
  const profile = readUtf8(profilePath)
  const e2eScript = readUtf8(scriptPath)
  assertContains(profile, 'DCC_E2E_TC002_DCC_DIRECT_URL')
  assertContains(profile, 'DCC_E2E_TC002_NON_DCC_DIRECT_URL')
  assertContains(profile, 'failureCode=DCC_DIRECT_LINK_BLOCKED')
  assertContains(profile, 'sourceIp=*')
  assertContains(profile, 'userAgent=*')
  assertContains(profile, 'requestId=*')
  assertContains(e2eScript, 'const directPage = await context.newPage()')
  assertContains(e2eScript, "directPage.goto(getEnv('DCC_E2E_TC002_DCC_DIRECT_URL')")
  assertContains(e2eScript, ".waitForEvent('download'")
  assertContains(e2eScript, 'Non-DCC direct link must either start a download or return the original file stream')
  assertContains(e2eScript, "verifyFinalApi(page, 'DCC_E2E_TC002_AUDIT_VERIFY_URL'")
  assertContains(e2eScript, "expected === '*'")
  assertContains(e2eScript, 'final API response must include non-empty')
  assertNotContains(e2eScript, "page.goto(getEnv('DCC_E2E_TC002_DCC_DIRECT_URL')")
}

testFixedUploadPurposeDoesNotRequireMissingUiSelector()
testUploadPurposeStillBlocksWithoutExplicitMode()
testTestServerProfileIsSourceBackedAndSecretFree()
testOnlyOfficeProfileAndWatermarkTraceContract()
testFinalApiVerificationUsesBackendBaseUrl()
testUploadLifecycleUsesRequestIdAndRealCategorySelectors()
testPreviewWatermarkTraceCodeIsRendered()
testPreviewUsesSingleTraceableMetadataEnvelope()
testPreviewTransformControlsAreBoundedAndTypeScoped()
testPreviewBinaryRequestUsesHeaderBoundViewerContext()
testScreenshotSmokeProfileUsesValidPreviewSample()
testOnlyOfficeBadTokenAcceptsFailFastCommonResult()
testEncryptedDownloadUsesResponseEvidenceHeadersInsteadOfManualPlainHash()
testOnlyOfficeReadonlyPreviewDoesNotPromptForAnonymousCoeditName()
testEncryptionFailClosedUsesRealDownloadConfirmation()
testAuditAuthorizationUsesRealSelectFilters()
testDownloadPolicyUsesRealCapabilityBoundary()
testFrontendFailClosedUsesRealBrokenArtifactAndUploadBoundary()
testDirectLinkBoundaryPreservesLoggedInPageForAuditVerification()
console.log('PASS: DCC E2E upload purpose contract')
