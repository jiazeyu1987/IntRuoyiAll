param(
  [switch]$ConfirmRg01EncryptedDownload,
  [switch]$ConfirmRg02UploadPolicy,
  [switch]$ConfirmRg03WatermarkTrace,
  [switch]$ConfirmRg04TestTenant,
  [switch]$ConfirmRg05OnlyOffice,
  [switch]$ConfirmRg06AuditBoundary,
  [switch]$ConfirmRg07ScopeStrategy
)

$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

function U {
  param([Parameter(Mandatory = $true)][string]$Hex)
  return -join (($Hex -split ' ') | ForEach-Object { [char]([Convert]::ToInt32($_, 16)) })
}

function Set-DccE2EEnv {
  param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][string]$Value
  )
  [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
}

function Set-DccE2EFileEnv {
  param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][string]$Path
  )
  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    throw "DCC E2E sample file is missing: $Name -> $Path"
  }
  Set-DccE2EEnv $Name $Path
}

function Set-DccE2EGate {
  param(
    [Parameter(Mandatory = $true)][bool]$Confirmed,
    [Parameter(Mandatory = $true)][string]$Name
  )
  if ($Confirmed) {
    Set-DccE2EEnv $Name 'true'
  } else {
    [Environment]::SetEnvironmentVariable($Name, $null, 'Process')
  }
}

$tenantName = U '6D4B 8BD5 79DF 6237'
$workspaceRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..\..')).Path
$allowedSample = Join-Path $workspaceRoot ('resource\' + (U '5DE5 827A 8DEF 7EBF 4E0E 8BBE 5907 53F0 8D26') + '.xlsx')
$oversizeSample = Join-Path $workspaceRoot ('resource\' + (U '4EA7 54C1 8D44 6599 4FEE 6539 7248') + '.xlsx')
$runStamp = Get-Date -Format 'yyyyMMddHHmmss'
$effectiveDate = Get-Date -Format 'yyyy-MM-dd'
$systemCategoryName = U '4F53 7CFB 6587 4EF6'
$dhfCategoryName = U '6280 672F 6587 4EF6 002D 0044 0048 0046'
$categorySelector = '.el-form-item:has(.el-form-item__label:has-text("' + (U '6587 4EF6 7C7B 522B') + '")) .el-select'
$systemCategoryOptionSelector = '.el-select-dropdown:visible .el-select-dropdown__item:has-text("' + $systemCategoryName + '")'
$dhfCategoryOptionSelector = '.el-select-dropdown:visible .el-select-dropdown__item:has-text("' + $dhfCategoryName + '")'
$sourceFileLabel = U '53D7 63A7 6587 4EF6'

Set-DccE2EEnv 'DCC_E2E_BASE_URL' 'http://172.30.30.58:8081'
Set-DccE2EEnv 'DCC_E2E_API_BASE_URL' 'http://172.30.30.58:48081'
Set-DccE2EEnv 'DCC_E2E_ALLOWED_BASE_URL_PATTERN' '^http://172\.30\.30\.58:8081$'
Set-DccE2EEnv 'DCC_E2E_ALLOWED_API_BASE_URL_PATTERN' '^http://172\.30\.30\.58:48081$'
Set-DccE2EEnv 'DCC_E2E_ENVIRONMENT_NAME' 'test'
Set-DccE2EEnv 'DCC_E2E_CONFIRM_TEST_TENANT_ONLY' 'true'
Set-DccE2EEnv 'DCC_E2E_TENANT_NAME' $tenantName
Set-DccE2EEnv 'DCC_E2E_USERNAME' 'aoteman'
Set-DccE2EEnv 'DCC_E2E_HEADLESS' 'true'
Set-DccE2EEnv 'DCC_E2E_FORBIDDEN_VISIBLE_TEXTS' 'originalFileId||sourceFileId||drawingPdfFileId||fileUrl||storagePath||plainFileUrl||tempFileUrl'
Set-DccE2EEnv 'DCC_E2E_AUDITOR_USERNAME' 'aoteman'
Set-DccE2EEnv 'DCC_E2E_ORDINARY_USERNAME' 'codexe2ereset'
Set-DccE2EEnv 'DCC_E2E_POLICY_USERNAME' 'showroomviewer'

Set-DccE2EGate $ConfirmRg01EncryptedDownload.IsPresent 'DCC_E2E_RG01_ENCRYPTION_READY'
Set-DccE2EGate $ConfirmRg02UploadPolicy.IsPresent 'DCC_E2E_RG02_UPLOAD_POLICY_READY'
Set-DccE2EGate $ConfirmRg03WatermarkTrace.IsPresent 'DCC_E2E_RG03_WATERMARK_TRACE_READY'
Set-DccE2EGate $ConfirmRg04TestTenant.IsPresent 'DCC_E2E_RG04_TEST_TENANT_READY'
Set-DccE2EGate $ConfirmRg05OnlyOffice.IsPresent 'DCC_E2E_RG05_ONLYOFFICE_READY'
Set-DccE2EGate $ConfirmRg06AuditBoundary.IsPresent 'DCC_E2E_RG06_AUDIT_FAILURE_BOUNDARY_READY'
Set-DccE2EGate $ConfirmRg07ScopeStrategy.IsPresent 'DCC_E2E_RG07_DCC_SCOPE_STRATEGY_READY'

Set-DccE2EEnv 'DCC_E2E_TC001_LIST_PATH' '/dcc/controlled-file/browser'
Set-DccE2EEnv 'DCC_E2E_TC001_LIST_READY_SELECTOR' ('.el-form-item:has-text("' + (U '5904 7406 72B6 6001') + '")')
Set-DccE2EEnv 'DCC_E2E_TC001_DETAIL_PATH' '/dcc/controlled-file/detail/2054545668044046252'
Set-DccE2EEnv 'DCC_E2E_TC001_DETAIL_READY_SELECTOR' ('text=' + (U '5BA1 6279 9636 6BB5 8FDB 5EA6'))
Set-DccE2EEnv 'DCC_E2E_TC001_VERSION_PATH' '/dcc/controlled-file/detail/2054545668044046252'
Set-DccE2EEnv 'DCC_E2E_TC001_VERSION_READY_SELECTOR' ('text=' + (U '7248 672C 5386 53F2'))
Set-DccE2EEnv 'DCC_E2E_TC001_EXTERNAL_REVIEW_PATH' '/dcc/controlled-file/external-review'
Set-DccE2EEnv 'DCC_E2E_TC001_EXTERNAL_REVIEW_READY_SELECTOR' ('text=' + (U '5916 6765 6587 4EF6 8BC4 5BA1'))
Set-DccE2EEnv 'DCC_E2E_TC001_UPLOAD_PATH' '/dcc/controlled-file/upload'
Set-DccE2EEnv 'DCC_E2E_TC001_UPLOAD_READY_SELECTOR' ('text=' + (U '53D7 63A7 6587 4EF6 63D0 4EA4'))
Set-DccE2EEnv 'DCC_E2E_TC001_FORBIDDEN_VISIBLE_TEXTS' 'originalFileId||sourceFileId||drawingPdfFileId||fileUrl||storagePath||plainFileUrl||tempFileUrl'
Set-DccE2EEnv 'DCC_E2E_TC001_FINAL_VERIFY_URL' '/admin-api/dcc/controlled-files/2054545668044046252'
Set-DccE2EEnv 'DCC_E2E_TC001_FINAL_EXPECT_JSON_CONTAINS' 'CODEX-E2E-FOURTH-5662414||V1.0'

Set-DccE2EEnv 'DCC_E2E_TC002_DCC_DIRECT_URL' 'http://172.30.30.58:48081/admin-api/infra/file/28/get/dcc/original/20260527/codex-e2e-stamped.pdf'
Set-DccE2EEnv 'DCC_E2E_TC002_DCC_DENIED_TEXT' ('DCC ' + (U '53D7 63A7 6587 4EF6 7981 6B62 901A 8FC7 901A 7528 6587 4EF6 76F4 94FE 8BBF 95EE'))
Set-DccE2EEnv 'DCC_E2E_TC002_NON_DCC_DIRECT_URL' 'http://172.30.30.58:48081/admin-api/infra/file/28/get/20260527/codex-r12-approval-template.docx'
Set-DccE2EEnv 'DCC_E2E_TC002_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&controlledFileId=2054545668044046252&actionType=DIRECT_LINK&result=DENIED&failureCode=DCC_DIRECT_LINK_BLOCKED'
Set-DccE2EEnv 'DCC_E2E_TC002_AUDIT_EXPECT_JSON_CONTAINS' 'DIRECT_LINK||DENIED||DCC_DIRECT_LINK_BLOCKED||INFRA_DIRECT_LINK||2054545668044046252'
Set-DccE2EEnv 'DCC_E2E_TC002_AUDIT_EXPECT_FIELDS' 'actionType=DIRECT_LINK||result=DENIED||failureCode=DCC_DIRECT_LINK_BLOCKED||purpose=INFRA_DIRECT_LINK||userId=0||sourceIp=*||userAgent=*||requestId=*'

Set-DccE2EEnv 'DCC_E2E_TC003_DETAIL_PATH' '/dcc/controlled-file/detail/2054545668044046252?viewer=1&from=detail'
Set-DccE2EEnv 'DCC_E2E_TC003_PREVIEW_TRIGGER_SELECTOR' '.protected-viewer-shell'
Set-DccE2EEnv 'DCC_E2E_TC003_PREVIEW_READY_SELECTOR' '.protected-viewer-canvas, .protected-viewer-image, .protected-viewer-text, .protected-viewer-video, .protected-viewer-audio-card'
Set-DccE2EEnv 'DCC_E2E_TC003_WATERMARK_SELECTOR' '[data-testid="protected-preview-badge"], [data-testid="protected-preview-watermark-overlay"], [data-testid="protected-preview-corner-watermark"]'
Set-DccE2EEnv 'DCC_E2E_TC003_FAILURE_PATH' '/dcc/controlled-file/detail/2054545668044046236?viewer=1&from=detail'
Set-DccE2EEnv 'DCC_E2E_TC003_FAILURE_TRIGGER_SELECTOR' '.protected-viewer-shell'
Set-DccE2EEnv 'DCC_E2E_TC003_FAILURE_ERROR_SELECTOR' '.el-alert--error'
Set-DccE2EEnv 'DCC_E2E_TC003_FAILURE_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&controlledFileId=2054545668044046236&userId=113&actionType=PREVIEW&result=DENIED'
Set-DccE2EEnv 'DCC_E2E_TC003_FAILURE_AUDIT_EXPECT_JSON_CONTAINS' 'PREVIEW||DENIED||CONTROLLED_PREVIEW||2054545668044046236||113'
Set-DccE2EEnv 'DCC_E2E_TC003_FAILURE_AUDIT_EXPECT_FIELDS' 'actionType=PREVIEW||result=DENIED||controlledFileId=2054545668044046236||userId=113||sourceIp=*||userAgent=*||requestId=*'

Set-DccE2EEnv 'DCC_E2E_TC004_PREVIEW_PATH' '/dcc/controlled-file/detail/2054545668044046252?viewer=1&from=detail'
Set-DccE2EEnv 'DCC_E2E_TC004_PREVIEW_READY_SELECTOR' '.protected-viewer-shell'
Set-DccE2EEnv 'DCC_E2E_TC004_WATERMARK_SELECTOR' '[data-testid="protected-preview-corner-watermark"]'
Set-DccE2EEnv 'DCC_E2E_TC004_WATERMARK_EXPECT_TEXT' 'aoteman'
Set-DccE2EEnv 'DCC_E2E_TC004_AUDIT_PATH' '/dcc/controlled-file/logs'
Set-DccE2EEnv 'DCC_E2E_TC004_AUDIT_TRACE_INPUT_SELECTOR' ('.el-form-item:has-text("' + (U '5173 952E 5B57') + '") input')
Set-DccE2EEnv 'DCC_E2E_TC004_AUDIT_SEARCH_SELECTOR' ('button:has-text("' + (U '67E5 8BE2') + '")')
Set-DccE2EEnv 'DCC_E2E_TC004_AUDIT_RESULT_SELECTOR' '.el-table__body-wrapper tbody tr'
Set-DccE2EEnv 'DCC_E2E_TC004_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&watermarkTraceCode={traceText}&controlledFileId=2054545668044046252&userId=113&actionType=PREVIEW&result=SUCCESS'
Set-DccE2EEnv 'DCC_E2E_TC004_AUDIT_EXPECT_JSON_CONTAINS' 'PREVIEW||SUCCESS||CONTROLLED_PREVIEW||{traceText}||2054545668044046252||113'
Set-DccE2EEnv 'DCC_E2E_TC004_AUDIT_EXPECT_FIELDS' 'watermarkTraceCode={traceText}||controlledFileId=2054545668044046252||userId=113||userIdentifier=113||tenantName=122||actionType=PREVIEW||purpose=CONTROLLED_PREVIEW||result=SUCCESS||accessEventCode=*||requestId=*||sourceIp=*||userAgent=*'

Set-DccE2EEnv 'DCC_E2E_TC005_DETAIL_PATH' '/dcc/controlled-file/detail/2054545668044046251?viewer=1&from=detail'
Set-DccE2EEnv 'DCC_E2E_TC005_OFFICE_TRIGGER_SELECTOR' '.protected-viewer-shell'
Set-DccE2EEnv 'DCC_E2E_TC005_OFFICE_READY_SELECTOR' '.onlyoffice-viewer-frame iframe'
Set-DccE2EEnv 'DCC_E2E_TC005_FORBIDDEN_TOOLBAR_SELECTORS' (('.onlyoffice-viewer-frame button:has-text("' + (U '4E0B 8F7D') + '")') + '||' + ('.onlyoffice-viewer-frame button:has-text("' + (U '6253 5370') + '")') + '||.onlyoffice-viewer-frame [contenteditable="true"]')
Set-DccE2EEnv 'DCC_E2E_TC005_AUDIT_PATH' '/dcc/controlled-file/logs'
Set-DccE2EEnv 'DCC_E2E_TC005_AUDIT_EVENT_INPUT_SELECTOR' ('.el-form-item:has-text("' + (U '5173 952E 5B57') + '") input')
Set-DccE2EEnv 'DCC_E2E_TC005_AUDIT_SEARCH_SELECTOR' ('button:has-text("' + (U '67E5 8BE2') + '")')
Set-DccE2EEnv 'DCC_E2E_TC005_AUDIT_RESULT_SELECTOR' '.el-table__body-wrapper tbody tr'

Set-DccE2EEnv 'DCC_E2E_TC006_UPLOAD_PATH' '/dcc/controlled-file/upload'
Set-DccE2EEnv 'DCC_E2E_TC006_CATEGORY_SELECTOR' $categorySelector
Set-DccE2EEnv 'DCC_E2E_TC006_CATEGORY_OPTION_SELECTOR' $dhfCategoryOptionSelector
Set-DccE2EFileEnv 'DCC_E2E_TC006_FILE_PATH' $allowedSample
Set-DccE2EEnv 'DCC_E2E_TC006_FILE_INPUT_SELECTOR' 'input[type="file"]'
Set-DccE2EEnv 'DCC_E2E_TC006_FIXED_PURPOSE_VALUE' 'SOURCE'
Set-DccE2EEnv 'DCC_E2E_TC006_SUBMIT_SELECTOR' ('button:has-text("' + (U '63D0 4EA4 5BA1 6279') + '")')
Set-DccE2EEnv 'DCC_E2E_TC006_ERROR_SELECTOR' '.el-message--error'
Set-DccE2EEnv 'DCC_E2E_TC006_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&requestId={requestId}&actionType=UPLOAD&result=DENIED&failureCode=DCC_UPLOAD_SIZE_POLICY_MISSING'
Set-DccE2EEnv 'DCC_E2E_TC006_AUDIT_EXPECT_JSON_CONTAINS' 'UPLOAD||DENIED||SOURCE||DCC_UPLOAD_SIZE_POLICY_MISSING||{requestId}'
Set-DccE2EEnv 'DCC_E2E_TC006_TEMP_VERIFY_URL' '/admin-api/dcc/controlled-files/upload-temporary/status?requestId={requestId}'
Set-DccE2EEnv 'DCC_E2E_TC006_TEMP_EXPECT_JSON_CONTAINS' 'requestId||{requestId}||temporaryFileCount||0||bindable||false'

Set-DccE2EEnv 'DCC_E2E_TC007_UPLOAD_PATH' '/dcc/controlled-file/upload'
Set-DccE2EEnv 'DCC_E2E_TC007_CATEGORY_SELECTOR' $categorySelector
Set-DccE2EEnv 'DCC_E2E_TC007_CATEGORY_OPTION_SELECTOR' $systemCategoryOptionSelector
Set-DccE2EFileEnv 'DCC_E2E_TC007_OVERSIZE_FILE_PATH' $oversizeSample
Set-DccE2EEnv 'DCC_E2E_TC007_FILE_INPUT_SELECTOR' 'input[type="file"]'
Set-DccE2EEnv 'DCC_E2E_TC007_FIXED_PURPOSE_VALUE' 'SOURCE'
Set-DccE2EEnv 'DCC_E2E_TC007_SUBMIT_SELECTOR' ('button:has-text("' + (U '63D0 4EA4 5BA1 6279') + '")')
Set-DccE2EEnv 'DCC_E2E_TC007_ERROR_SELECTOR' '.el-message--error'
Set-DccE2EEnv 'DCC_E2E_TC007_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&requestId={requestId}&actionType=UPLOAD&result=DENIED&failureCode=DCC_UPLOAD_SIZE_EXCEEDED'
Set-DccE2EEnv 'DCC_E2E_TC007_AUDIT_EXPECT_JSON_CONTAINS' 'UPLOAD||DENIED||SOURCE||DCC_UPLOAD_SIZE_EXCEEDED||{requestId}'
Set-DccE2EEnv 'DCC_E2E_TC007_TEMP_VERIFY_URL' '/admin-api/dcc/controlled-files/upload-temporary/status?requestId={requestId}'
Set-DccE2EEnv 'DCC_E2E_TC007_TEMP_EXPECT_JSON_CONTAINS' 'requestId||{requestId}||temporaryFileCount||0||bindable||false'

Set-DccE2EEnv 'DCC_E2E_TC008_UPLOAD_PATH' '/dcc/controlled-file/upload'
Set-DccE2EEnv 'DCC_E2E_TC008_CATEGORY_SELECTOR' $categorySelector
Set-DccE2EEnv 'DCC_E2E_TC008_CATEGORY_OPTION_SELECTOR' $systemCategoryOptionSelector
Set-DccE2EEnv 'DCC_E2E_TC008_CATEGORY_ID' '906101'
Set-DccE2EEnv 'DCC_E2E_TC008_DIRECTORY_ID' '906200'
Set-DccE2EEnv 'DCC_E2E_TC008_FILE_NAME_SELECTOR' ('.el-form-item:has(.el-form-item__label:has-text("' + (U '6587 4EF6 540D 79F0') + '")) input')
Set-DccE2EEnv 'DCC_E2E_TC008_FILE_NAME_VALUE' ('Codex-E2E-Upload-' + $runStamp)
Set-DccE2EEnv 'DCC_E2E_TC008_FILE_NUMBER_SELECTOR' ('.el-form-item:has(.el-form-item__label:has-text("' + (U '6587 4EF6 7F16 53F7') + '")) input')
Set-DccE2EEnv 'DCC_E2E_TC008_FILE_NUMBER_VALUE' ('DCC-E2E-UP-' + $runStamp)
Set-DccE2EEnv 'DCC_E2E_TC008_PRODUCT_CODE_SELECTOR' ('.el-form-item:has(.el-form-item__label:has-text("' + (U '4EA7 54C1 7F16 53F7') + '")) input')
Set-DccE2EEnv 'DCC_E2E_TC008_PRODUCT_CODE_VALUE' $runStamp
Set-DccE2EEnv 'DCC_E2E_TC008_VERSION_SELECTOR' ('.el-form-item:has(.el-form-item__label:has-text("' + (U '7248 672C 53F7') + '")) input')
Set-DccE2EEnv 'DCC_E2E_TC008_VERSION_VALUE' 'V1.0'
Set-DccE2EEnv 'DCC_E2E_TC008_EFFECTIVE_DATE_SELECTOR' ('.el-form-item:has(.el-form-item__label:has-text("' + (U '751F 6548 65E5 671F') + '")) input')
Set-DccE2EEnv 'DCC_E2E_TC008_EFFECTIVE_DATE_VALUE' $effectiveDate
Set-DccE2EFileEnv 'DCC_E2E_TC008_FILE_PATH' $allowedSample
Set-DccE2EEnv 'DCC_E2E_TC008_FILE_INPUT_SELECTOR' 'input[type="file"]'
Set-DccE2EEnv 'DCC_E2E_TC008_FIXED_PURPOSE_VALUE' 'SOURCE'
Set-DccE2EEnv 'DCC_E2E_TC008_UPLOAD_SUCCESS_SELECTOR' ('text=' + (U '9884 89C8 6587 4EF6 FF1A'))
Set-DccE2EEnv 'DCC_E2E_TC008_SUBMIT_SELECTOR' ('button:has-text("' + (U '63D0 4EA4 5BA1 6279') + '")')
Set-DccE2EEnv 'DCC_E2E_TC008_SUBMIT_SUCCESS_SELECTOR' ('.el-message--success:has-text("' + (U '53D7 63A7 6587 4EF6 5DF2 63D0 4EA4 5BA1 6279') + '")')
Set-DccE2EEnv 'DCC_E2E_TC008_FILE_ID_FAILURE_EXPECT_JSON_CONTAINS' 'DCC upload ticket is invalid'
Set-DccE2EEnv 'DCC_E2E_TC008_CROSS_SESSION_FAILURE_EXPECT_JSON_CONTAINS' 'DCC upload ticket is invalid'
Set-DccE2EEnv 'DCC_E2E_TC008_EXPIRED_TICKET_FAILURE_EXPECT_JSON_CONTAINS' 'DCC upload ticket is invalid'

Set-DccE2EEnv 'DCC_E2E_TC009_FLOW_PATH' '/dcc/controlled-file/upload'
Set-DccE2EEnv 'DCC_E2E_TC009_CATEGORY_SELECTOR' $categorySelector
Set-DccE2EEnv 'DCC_E2E_TC009_CATEGORY_OPTION_SELECTOR' $systemCategoryOptionSelector
Set-DccE2EFileEnv 'DCC_E2E_TC009_FILE_PATH' $allowedSample
Set-DccE2EEnv 'DCC_E2E_TC009_FILE_INPUT_SELECTOR' 'input[type="file"]'
Set-DccE2EEnv 'DCC_E2E_TC009_UPLOAD_SUCCESS_SELECTOR' ('text=' + (U '9884 89C8 6587 4EF6 FF1A'))
Set-DccE2EEnv 'DCC_E2E_TC009_REMOVE_SELECTOR' ('.el-form-item:has(.el-form-item__label:has-text("' + $sourceFileLabel + '")) .el-upload-list__item .el-icon--close')
Set-DccE2EEnv 'DCC_E2E_TC009_CLEANUP_SUCCESS_SELECTOR' ('.el-message--success:has-text("' + (U '5DF2 6E05 7406 672C 6B21 4E0A 4F20 4E34 65F6 6587 4EF6') + '")')
Set-DccE2EEnv 'DCC_E2E_TC009_TEMP_VERIFY_URL' '/admin-api/dcc/controlled-files/upload-temporary/status?requestId={requestId}'
Set-DccE2EEnv 'DCC_E2E_TC009_TEMP_EXPECT_JSON_CONTAINS' 'requestId||{requestId}||bindable||false||CLEANED||USER_DISCARDED'
Set-DccE2EEnv 'DCC_E2E_TC009_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&requestId={requestId}&actionType=TEMP_FILE&result=CLEANED'
Set-DccE2EEnv 'DCC_E2E_TC009_AUDIT_EXPECT_JSON_CONTAINS' 'TEMP_FILE||CLEANED||UPLOAD_TEMPORARY_FILE||USER_DISCARDED||{requestId}'

Set-DccE2EEnv 'DCC_E2E_TC010_DOWNLOAD_PATH' '/dcc/controlled-file/detail/2054545668044046252'
Set-DccE2EEnv 'DCC_E2E_TC010_DOWNLOAD_TRIGGER_SELECTOR' ('button:has-text("' + (U '4E0B 8F7D 53D7 63A7 6587 4EF6') + '")')
Set-DccE2EEnv 'DCC_E2E_TC010_DOWNLOAD_CONFIRM_SELECTOR' ('button:has-text("' + (U '786E 8BA4 4E0B 8F7D') + '")')
Set-DccE2EEnv 'DCC_E2E_TC010_EXPECT_ENCRYPTION_POLICY_VERSION' 'test-20260530-dcc-p7'
Set-DccE2EEnv 'DCC_E2E_TC010_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&controlledFileId=2054545668044046252&userId=113&actionType=DOWNLOAD&result=ALLOWED'
Set-DccE2EEnv 'DCC_E2E_TC010_AUDIT_EXPECT_JSON_CONTAINS' 'DOWNLOAD||ALLOWED||CONTROLLED_DOWNLOAD||2054545668044046252||113'
Set-DccE2EEnv 'DCC_E2E_TC010_AUDIT_EXPECT_FIELDS' 'actionType=DOWNLOAD||result=ALLOWED||sourceIp=*||userAgent=*||requestId=*'

Set-DccE2EEnv 'DCC_E2E_TC011_DOWNLOAD_PATH' '/dcc/controlled-file/detail/2054545668044046236'
Set-DccE2EEnv 'DCC_E2E_TC011_DOWNLOAD_TRIGGER_SELECTOR' ('button:has-text("' + (U '4E0B 8F7D 53D7 63A7 6587 4EF6') + '")')
Set-DccE2EEnv 'DCC_E2E_TC011_DOWNLOAD_CONFIRM_SELECTOR' ('button:has-text("' + (U '786E 8BA4 4E0B 8F7D') + '")')
Set-DccE2EEnv 'DCC_E2E_TC011_ERROR_SELECTOR' '.el-message--error'
Set-DccE2EEnv 'DCC_E2E_TC011_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&controlledFileId=2054545668044046236&userId=113&actionType=DOWNLOAD&result=DENIED&failureCode=ENCRYPTION_CONTRACT_FAILED'
Set-DccE2EEnv 'DCC_E2E_TC011_AUDIT_EXPECT_JSON_CONTAINS' 'DOWNLOAD||DENIED||CONTROLLED_DOWNLOAD||ENCRYPTION_CONTRACT_FAILED||2054545668044046236||113'
Set-DccE2EEnv 'DCC_E2E_TC011_AUDIT_EXPECT_FIELDS' 'actionType=DOWNLOAD||result=DENIED||failureCode=ENCRYPTION_CONTRACT_FAILED||reason=ENCRYPTION_CONTRACT_FAILED||sourceIp=*||userAgent=*||requestId=*'

Set-DccE2EEnv 'DCC_E2E_TC012_PREFIX_DENIED_PATH' '/dcc/controlled-file/detail/2054545668044046234'
Set-DccE2EEnv 'DCC_E2E_TC012_PREFIX_DENIED_ABSENT_SELECTOR' ('button:has-text("' + (U '4E0B 8F7D 53D7 63A7 6587 4EF6') + '")')
Set-DccE2EEnv 'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_VERIFY_URL' '/admin-api/dcc/controlled-files/2054545668044046234'
Set-DccE2EEnv 'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_EXPECT_FIELDS' 'id=2054545668044046234||categoryId=906102||canPreview=true||canDownload=false'
Set-DccE2EEnv 'DCC_E2E_TC012_NO_PREFIX_ALLOWED_PATH' '/dcc/controlled-file/detail/2054545668044046252'
Set-DccE2EEnv 'DCC_E2E_TC012_NO_PREFIX_ALLOWED_TRIGGER_SELECTOR' ('button:has-text("' + (U '4E0B 8F7D 53D7 63A7 6587 4EF6') + '")')
Set-DccE2EEnv 'DCC_E2E_TC012_NO_PREFIX_ALLOWED_CONFIRM_SELECTOR' ('button:has-text("' + (U '786E 8BA4 4E0B 8F7D') + '")')
Set-DccE2EEnv 'DCC_E2E_TC012_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&controlledFileId=2054545668044046252&userId=910204&actionType=DOWNLOAD&result=ALLOWED'
Set-DccE2EEnv 'DCC_E2E_TC012_AUDIT_EXPECT_JSON_CONTAINS' 'DOWNLOAD||ALLOWED||CONTROLLED_DOWNLOAD||2054545668044046252||910204'
Set-DccE2EEnv 'DCC_E2E_TC012_AUDIT_EXPECT_FIELDS' 'actionType=DOWNLOAD||result=ALLOWED||controlledFileId=2054545668044046252||userId=910204||sourceIp=*||userAgent=*||requestId=*'

Set-DccE2EEnv 'DCC_E2E_TC013_AUDIT_PATH' '/dcc/controlled-file/logs'
Set-DccE2EEnv 'DCC_E2E_TC013_TRACE_INPUT_SELECTOR' ('.el-form-item:has-text("' + (U '8FFD 8E2A 7801') + '") input')
Set-DccE2EEnv 'DCC_E2E_TC013_EVENT_INPUT_SELECTOR' ('.el-form-item:has-text("' + (U '4E8B 4EF6 7801') + '") input')
Set-DccE2EEnv 'DCC_E2E_TC013_EVENT_VALUE' 'AE-20260529-6AAD4B0DD668'
Set-DccE2EEnv 'DCC_E2E_TC013_FILE_INPUT_SELECTOR' ('.el-form-item:has-text("' + (U '6587 4EF6 0049 0044') + '") input')
Set-DccE2EEnv 'DCC_E2E_TC013_FILE_VALUE' '2054545668044046252'
Set-DccE2EEnv 'DCC_E2E_TC013_USER_INPUT_SELECTOR' ('.el-form-item:has-text("' + (U '7528 6237 0049 0044') + '") input')
Set-DccE2EEnv 'DCC_E2E_TC013_USER_VALUE' '113'
Set-DccE2EEnv 'DCC_E2E_TC013_TRACE_VALUE' 'WM-20260529-CBFC5421A6DF'
Set-DccE2EEnv 'DCC_E2E_TC013_ACTION_SELECTOR' ('.el-form-item:has-text("' + (U '52A8 4F5C') + '") .el-select')
Set-DccE2EEnv 'DCC_E2E_TC013_ACTION_OPTION_SELECTOR' ('.el-select-dropdown:visible .el-select-dropdown__item:has-text("' + (U '9884 89C8') + '")')
Set-DccE2EEnv 'DCC_E2E_TC013_RESULT_SELECTOR_FIELD' ('.el-form-item:has-text("' + (U '7ED3 679C') + '") .el-select')
Set-DccE2EEnv 'DCC_E2E_TC013_RESULT_OPTION_SELECTOR' ('.el-select-dropdown:visible .el-select-dropdown__item:has-text("' + (U '6210 529F') + '")')
Set-DccE2EEnv 'DCC_E2E_TC013_TIME_START_SELECTOR' ('.el-form-item:has-text("' + (U '53D1 751F 65F6 95F4') + '") input[placeholder="' + (U '5F00 59CB 65F6 95F4') + '"]')
Set-DccE2EEnv 'DCC_E2E_TC013_TIME_START_VALUE' '2026-05-29 00:00:00'
Set-DccE2EEnv 'DCC_E2E_TC013_TIME_END_SELECTOR' ('.el-form-item:has-text("' + (U '53D1 751F 65F6 95F4') + '") input[placeholder="' + (U '7ED3 675F 65F6 95F4') + '"]')
Set-DccE2EEnv 'DCC_E2E_TC013_TIME_END_VALUE' '2026-05-31 23:59:59'
Set-DccE2EEnv 'DCC_E2E_TC013_SEARCH_SELECTOR' ('button:has-text("' + (U '67E5 8BE2') + '")')
Set-DccE2EEnv 'DCC_E2E_TC013_RESULT_SELECTOR' '.el-table__body-wrapper tbody tr'
Set-DccE2EEnv 'DCC_E2E_TC013_UNAUTHORIZED_SELECTOR' 'text=Access Denied'

Set-DccE2EEnv 'DCC_E2E_TC014_PREVIEW_FAILURE_PATH' '/dcc/controlled-file/detail/2054545668044046236?viewer=1&from=detail'
Set-DccE2EEnv 'DCC_E2E_TC014_PREVIEW_TRIGGER_SELECTOR' '.protected-viewer-shell'
Set-DccE2EEnv 'DCC_E2E_TC014_PREVIEW_ERROR_SELECTOR' '.el-alert--error'
Set-DccE2EEnv 'DCC_E2E_TC014_PREVIEW_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&controlledFileId=2054545668044046236&userId=113&actionType=PREVIEW&result=DENIED'
Set-DccE2EEnv 'DCC_E2E_TC014_PREVIEW_AUDIT_EXPECT_JSON_CONTAINS' 'PREVIEW||DENIED||CONTROLLED_PREVIEW||2054545668044046236||113'
Set-DccE2EEnv 'DCC_E2E_TC014_PREVIEW_AUDIT_EXPECT_FIELDS' 'actionType=PREVIEW||result=DENIED||controlledFileId=2054545668044046236||userId=113||sourceIp=*||userAgent=*||requestId=*'
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_PATH' '/dcc/controlled-file/upload'
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_CATEGORY_SELECTOR' $categorySelector
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_CATEGORY_OPTION_SELECTOR' $systemCategoryOptionSelector
Set-DccE2EFileEnv 'DCC_E2E_TC014_UPLOAD_OVERSIZE_FILE_PATH' $oversizeSample
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_FILE_INPUT_SELECTOR' 'input[type="file"]'
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_FIXED_PURPOSE_VALUE' 'SOURCE'
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_SUBMIT_SELECTOR' ('button:has-text("' + (U '63D0 4EA4 5BA1 6279') + '")')
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_ERROR_SELECTOR' '.el-message--error'
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&requestId={requestId}&actionType=UPLOAD&result=DENIED&failureCode=DCC_UPLOAD_SIZE_EXCEEDED'
Set-DccE2EEnv 'DCC_E2E_TC014_UPLOAD_AUDIT_EXPECT_JSON_CONTAINS' 'UPLOAD||DENIED||SOURCE||DCC_UPLOAD_SIZE_EXCEEDED||{requestId}'
Set-DccE2EEnv 'DCC_E2E_TC014_DOWNLOAD_FAILURE_PATH' '/dcc/controlled-file/detail/2054545668044046236'
Set-DccE2EEnv 'DCC_E2E_TC014_DOWNLOAD_TRIGGER_SELECTOR' ('button:has-text("' + (U '4E0B 8F7D 53D7 63A7 6587 4EF6') + '")')
Set-DccE2EEnv 'DCC_E2E_TC014_DOWNLOAD_CONFIRM_SELECTOR' ('button:has-text("' + (U '786E 8BA4 4E0B 8F7D') + '")')
Set-DccE2EEnv 'DCC_E2E_TC014_DOWNLOAD_ERROR_SELECTOR' '.el-message--error'
Set-DccE2EEnv 'DCC_E2E_TC014_DOWNLOAD_AUDIT_VERIFY_URL' '/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&controlledFileId=2054545668044046236&userId=113&actionType=DOWNLOAD&result=DENIED&failureCode=ENCRYPTION_CONTRACT_FAILED'
Set-DccE2EEnv 'DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_JSON_CONTAINS' 'DOWNLOAD||DENIED||CONTROLLED_DOWNLOAD||ENCRYPTION_CONTRACT_FAILED||2054545668044046236||113'
Set-DccE2EEnv 'DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_FIELDS' 'actionType=DOWNLOAD||result=DENIED||failureCode=ENCRYPTION_CONTRACT_FAILED||reason=ENCRYPTION_CONTRACT_FAILED||sourceIp=*||userAgent=*||requestId=*'

Set-DccE2EEnv 'DCC_E2E_TC015_SCREENSHOT_PATHS' '/dcc/controlled-file/logs||/dcc/controlled-file/upload||/dcc/controlled-file/detail/2054545668044046252?viewer=1&from=detail'
Set-DccE2EEnv 'DCC_E2E_TC015_READY_SELECTORS' (('text=' + (U '6587 63A7 65E5 5FD7')) + '||' + ('text=' + (U '53D7 63A7 6587 4EF6 63D0 4EA4')) + '||.protected-viewer-shell')
Set-DccE2EEnv 'DCC_E2E_TC015_REQUIRED_VISIBLE_TEXTS' ((U '6587 63A7 65E5 5FD7') + '||' + (U '53D7 63A7 6587 4EF6 63D0 4EA4') + '||WM-')

Set-DccE2EEnv 'DCC_E2E_TC016_NON_DCC_PATH' '/infra/file'
Set-DccE2EEnv 'DCC_E2E_TC016_NON_DCC_SUCCESS_SELECTOR' ('span.el-breadcrumb__inner:has-text("' + (U '6587 4EF6 7BA1 7406') + '")')
Set-DccE2EEnv 'DCC_E2E_TC016_DCC_BYPASS_PATH' 'http://172.30.30.58:48081/admin-api/infra/file/28/get/dcc/original/20260527/codex-e2e-stamped.pdf'
Set-DccE2EEnv 'DCC_E2E_TC016_DCC_BYPASS_ERROR_SELECTOR' ('text=' + 'DCC ' + (U '53D7 63A7 6587 4EF6 7981 6B62 901A 8FC7 901A 7528 6587 4EF6 76F4 94FE 8BBF 95EE'))

if (-not [Environment]::GetEnvironmentVariable('DCC_E2E_PASSWORD', 'Process')) {
  Write-Warning 'DCC_E2E_PASSWORD is intentionally not stored in this profile. Set it in the current shell from docs/login-access.md before running the real E2E.'
}

Write-Host 'DCC test-server E2E profile loaded. Release gates are opt-in switches; unresolved product/environment inputs still block with EXIT:2.'
