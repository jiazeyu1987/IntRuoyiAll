# DCC Controlled File Protection E2E Environment

This file documents the required inputs for `tests/e2e/dcc-controlled-file-protection.e2e.js`.
The script is intentionally fail-fast. If any release gate or real test input is missing, it prints
`BLOCKED: RG-*` and exits with code `2`. Do not replace these inputs with mock data.

## Run

```powershell
node --check tests\e2e\dcc-controlled-file-protection.e2e.js
node tests\e2e\dcc-controlled-file-protection.e2e.js
```

## Test Server Profile

For the current test server, load the non-secret profile first:

```powershell
. .\tests\e2e\dcc-controlled-file-protection.test-server.profile.ps1
```

Release gates are not set to true by default. Enable a gate with an explicit switch only after the
real prerequisite has been confirmed, for example:

```powershell
. .\tests\e2e\dcc-controlled-file-protection.test-server.profile.ps1 -ConfirmRg02UploadPolicy -ConfirmRg04TestTenant
```

The profile sets source-backed routes, selectors, sample file paths, known test-tenant IDs, and the
test frontend URL. It intentionally does not store passwords, encryption keys, tokens, trace codes,
or product acceptance decisions. Set account passwords in the current shell from the controlled
login-access document immediately before the run, and do not commit them.

## Release Gate Confirmation

Set each value to `true` only after the real prerequisite is confirmed.

```powershell
$env:DCC_E2E_RG01_ENCRYPTION_READY='true'              # RG-01 encryption gateway contract, success artifact, failure states
$env:DCC_E2E_RG02_UPLOAD_POLICY_READY='true'           # RG-02 upload size values and samples
$env:DCC_E2E_RG03_WATERMARK_TRACE_READY='true'         # RG-03 screenshot trace acceptance standard
$env:DCC_E2E_RG04_TEST_TENANT_READY='true'             # RG-04 real test tenant accounts, samples, permissions
$env:DCC_E2E_RG05_ONLYOFFICE_READY='true'              # RG-05 OnlyOffice can pull DCC proxy URLs
$env:DCC_E2E_RG06_AUDIT_FAILURE_BOUNDARY_READY='true'  # RG-06 audit failure transaction policy
$env:DCC_E2E_RG07_DCC_SCOPE_STRATEGY_READY='true'      # RG-07 DCC scope identification strategy
$env:DCC_E2E_CASES='TC-E2E-005'                        # Optional targeted run; unset for the full release gate
```

## Common Login

```powershell
$env:DCC_E2E_BASE_URL='http://localhost:8081'
$env:DCC_E2E_API_BASE_URL='http://localhost:48081'
$env:DCC_E2E_ALLOWED_BASE_URL_PATTERN='^http://localhost:8081$'
$env:DCC_E2E_ALLOWED_API_BASE_URL_PATTERN='^http://localhost:48081$'
$env:DCC_E2E_ENVIRONMENT_NAME='test'
$env:DCC_E2E_CONFIRM_TEST_TENANT_ONLY='true'
$env:DCC_E2E_TENANT_NAME='测试租户'
$env:DCC_E2E_USERNAME='<real-test-user>'
$env:DCC_E2E_PASSWORD='<real-password>'
$env:DCC_E2E_AUDITOR_USERNAME='<real-auditor-user>'
$env:DCC_E2E_AUDITOR_PASSWORD='<real-auditor-password>'
$env:DCC_E2E_ORDINARY_USERNAME='<real-ordinary-user>'
$env:DCC_E2E_ORDINARY_PASSWORD='<real-ordinary-password>'
$env:DCC_E2E_POLICY_USERNAME='<real-policy-boundary-user>'
$env:DCC_E2E_POLICY_PASSWORD='<real-policy-boundary-password>'
$env:DCC_E2E_HEADLESS='true'
$env:DCC_E2E_FORBIDDEN_VISIBLE_TEXTS='originalFileId||sourceFileId||fileUrl||storagePath'
```

The script blocks if `DCC_E2E_TENANT_NAME` is `芋道源码`, if `DCC_E2E_ENVIRONMENT_NAME`
is not a test environment label, or if `DCC_E2E_BASE_URL` does not match
`DCC_E2E_ALLOWED_BASE_URL_PATTERN`.
Final API verification uses `DCC_E2E_API_BASE_URL` directly. Do not point final API checks at the
frontend SPA URL, because a `200 text/html` shell page is not valid backend verification evidence.

## TC-E2E-001 Response Field Convergence

```powershell
$env:DCC_E2E_TC001_LIST_PATH='/dcc/controlled-file/browser'
$env:DCC_E2E_TC001_LIST_READY_SELECTOR='<real-list-ready-selector>'
$env:DCC_E2E_TC001_DETAIL_PATH='/dcc/controlled-file/detail/<real-controlled-file-id>'
$env:DCC_E2E_TC001_DETAIL_READY_SELECTOR='<real-detail-ready-selector>'
$env:DCC_E2E_TC001_VERSION_PATH='<real-version-history-route-or-detail-tab-route>'
$env:DCC_E2E_TC001_VERSION_READY_SELECTOR='<real-version-ready-selector>'
$env:DCC_E2E_TC001_EXTERNAL_REVIEW_PATH='/dcc/controlled-file/external-review'
$env:DCC_E2E_TC001_EXTERNAL_REVIEW_READY_SELECTOR='<real-external-review-ready-selector>'
$env:DCC_E2E_TC001_UPLOAD_PATH='<real-upload-route>'
$env:DCC_E2E_TC001_UPLOAD_READY_SELECTOR='<real-upload-ready-selector>'
$env:DCC_E2E_TC001_FORBIDDEN_VISIBLE_TEXTS='originalFileId||sourceFileId||drawingPdfFileId||fileUrl||storagePath'
$env:DCC_E2E_TC001_FINAL_VERIFY_URL='<final-api-url-to-verify-response-fields>'
$env:DCC_E2E_TC001_FINAL_EXPECT_JSON_CONTAINS='<real-business-file-number-or-version-code>'
```

## TC-E2E-002 Direct Link Boundary

```powershell
$env:DCC_E2E_TC002_DCC_DIRECT_URL='<real-browser-url-to-controlled-infra-file>'
$env:DCC_E2E_TC002_DCC_DENIED_TEXT='<real-denied-text-on-page>'
$env:DCC_E2E_TC002_NON_DCC_DIRECT_URL='<real-browser-url-to-non-dcc-file>'
$env:DCC_E2E_TC002_AUDIT_VERIFY_URL='<final-api-url-to-verify-direct-link-denied-audit>'
$env:DCC_E2E_TC002_AUDIT_EXPECT_JSON_CONTAINS='DIRECT_LINK||DENIED||<real-file-number>'
$env:DCC_E2E_TC002_AUDIT_EXPECT_FIELDS='actionType=DIRECT_LINK||result=DENIED||failureCode=DCC_DIRECT_LINK_BLOCKED||sourceIp=*||userAgent=*||requestId=*'
```

## TC-E2E-003 Controlled Preview

```powershell
$env:DCC_E2E_TC003_DETAIL_PATH='/dcc/controlled-file/detail/<real-previewable-file-id>'
$env:DCC_E2E_TC003_PREVIEW_TRIGGER_SELECTOR='<real-preview-button-selector>'
$env:DCC_E2E_TC003_PREVIEW_READY_SELECTOR='<real-preview-rendered-selector>'
$env:DCC_E2E_TC003_WATERMARK_SELECTOR='<real-watermark-selector>'
$env:DCC_E2E_TC003_FAILURE_PATH='<real-broken-preview-file-route>'
$env:DCC_E2E_TC003_FAILURE_TRIGGER_SELECTOR='<real-preview-trigger-selector>'
$env:DCC_E2E_TC003_FAILURE_ERROR_SELECTOR='<real-preview-contract-error-selector>'
$env:DCC_E2E_TC003_FAILURE_AUDIT_VERIFY_URL='<final-api-url-to-verify-preview-failure-audit>'
$env:DCC_E2E_TC003_FAILURE_AUDIT_EXPECT_JSON_CONTAINS='PREVIEW||DENIED||<broken-file-id>'
$env:DCC_E2E_TC003_FAILURE_AUDIT_EXPECT_FIELDS='actionType=PREVIEW||result=DENIED||sourceIp=*||userAgent=*||requestId=*'
```

## TC-E2E-004 Screenshot Watermark Trace

TC004 first reuses `watermarkTraceCode` from preview metadata. Set `DCC_E2E_TC004_TRACE_TEXT`
only when the real preview route cannot expose metadata to the browser capture; if neither source
exists, the E2E fails instead of searching audit with a placeholder.

```powershell
$env:DCC_E2E_TC004_PREVIEW_PATH='/dcc/controlled-file/detail/<real-previewable-file-id>'
$env:DCC_E2E_TC004_PREVIEW_READY_SELECTOR='<real-preview-rendered-selector>'
$env:DCC_E2E_TC004_WATERMARK_SELECTOR='<real-watermark-selector>'
$env:DCC_E2E_TC004_WATERMARK_EXPECT_TEXT='<real-user-account-or-display-name-visible-in-screenshot>'
$env:DCC_E2E_TC004_TRACE_TEXT='<optional-real-watermark-trace-code-visible-in-screenshot>'
$env:DCC_E2E_TC004_AUDIT_PATH='/dcc/controlled-file/logs'
$env:DCC_E2E_TC004_AUDIT_TRACE_INPUT_SELECTOR='<real-trace-input-selector>'
$env:DCC_E2E_TC004_AUDIT_SEARCH_SELECTOR='<real-search-button-selector>'
$env:DCC_E2E_TC004_AUDIT_RESULT_SELECTOR='<real-result-row-selector>'
$env:DCC_E2E_TC004_AUDIT_VERIFY_URL='/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&watermarkTraceCode={traceText}&controlledFileId=<real-controlled-file-id>&userId=<real-user-id>&actionType=PREVIEW&result=SUCCESS'
$env:DCC_E2E_TC004_AUDIT_EXPECT_JSON_CONTAINS='PREVIEW||SUCCESS||CONTROLLED_PREVIEW||{traceText}||<real-controlled-file-id>||<real-user-id>'
$env:DCC_E2E_TC004_AUDIT_EXPECT_FIELDS='watermarkTraceCode={traceText}||controlledFileId=<real-controlled-file-id>||userId=<real-user-id>||tenantName=<real-tenant-id-or-name>||actionType=PREVIEW||purpose=CONTROLLED_PREVIEW||result=SUCCESS||accessEventCode=*||requestId=*||sourceIp=*||userAgent=*'
```

## TC-E2E-005 OnlyOffice Readonly

Separate multiple forbidden toolbar selectors with `||`. The audit event value is normally reused
from preview metadata `accessEventCode`; set `DCC_E2E_TC005_AUDIT_EVENT_VALUE` only for a product-
approved fixed event value. The bad-token URL is normally derived from preview metadata
`onlyofficeDocumentUrl`; set `DCC_E2E_TC005_BAD_TOKEN_DENIED_TEXT` only when the product requires a
specific denial message in addition to HTTP failure. The test-server profile stores routes and
selectors only, not secrets, tokens, or an OnlyOffice acceptance conclusion.

```powershell
$env:DCC_E2E_TC005_DETAIL_PATH='/dcc/controlled-file/detail/<real-office-file-id>'
$env:DCC_E2E_TC005_OFFICE_TRIGGER_SELECTOR='<real-office-preview-button-selector>'
$env:DCC_E2E_TC005_OFFICE_READY_SELECTOR='<real-onlyoffice-ready-selector>'
$env:DCC_E2E_TC005_FORBIDDEN_TOOLBAR_SELECTORS='<download-selector>||<print-selector>||<edit-selector>'
$env:DCC_E2E_TC005_AUDIT_PATH='/dcc/controlled-file/logs'
$env:DCC_E2E_TC005_AUDIT_EVENT_INPUT_SELECTOR='<real-event-input-selector>'
$env:DCC_E2E_TC005_AUDIT_EVENT_VALUE='<optional-real-office-read-access-event-code>'
$env:DCC_E2E_TC005_AUDIT_SEARCH_SELECTOR='<real-search-button-selector>'
$env:DCC_E2E_TC005_AUDIT_RESULT_SELECTOR='<real-office-read-result-row-selector>'
$env:DCC_E2E_TC005_BAD_TOKEN_DENIED_TEXT='<optional-real-token-denied-text>'
```

## TC-E2E-006 / 007 / 008 Upload

The current upload page uses a fixed `SOURCE` purpose for the main controlled source file. If a future
page adds a real purpose control, configure `*_PURPOSE_SELECTOR` and `*_PURPOSE_VALUE` instead of
`*_FIXED_PURPOSE_VALUE`; never configure both modes.

```powershell
$env:DCC_E2E_TC006_UPLOAD_PATH='<real-upload-route-with-missing-or-invalid-policy>'
$env:DCC_E2E_TC006_CATEGORY_SELECTOR='<real-category-select-selector>'
$env:DCC_E2E_TC006_CATEGORY_OPTION_SELECTOR='<real-category-option-without-source-policy>'
$env:DCC_E2E_TC006_FILE_PATH='D:\path\to\allowed-size-real-sample.pdf'
$env:DCC_E2E_TC006_FILE_INPUT_SELECTOR='<real-file-input-selector>'
$env:DCC_E2E_TC006_FIXED_PURPOSE_VALUE='SOURCE'
$env:DCC_E2E_TC006_SUBMIT_SELECTOR='<real-submit-selector>'
$env:DCC_E2E_TC006_ERROR_SELECTOR='<real-policy-missing-error-selector>'
$env:DCC_E2E_TC006_AUDIT_VERIFY_URL='/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&requestId={requestId}'
$env:DCC_E2E_TC006_AUDIT_EXPECT_JSON_CONTAINS='UPLOAD||DENIED||SOURCE||DCC_UPLOAD_SIZE_POLICY_MISSING||{requestId}'
$env:DCC_E2E_TC006_TEMP_VERIFY_URL='/admin-api/dcc/controlled-files/upload-temporary/status?requestId={requestId}'
$env:DCC_E2E_TC006_TEMP_EXPECT_JSON_CONTAINS='requestId||{requestId}||temporaryFileCount||0||bindable||false'

$env:DCC_E2E_TC007_UPLOAD_PATH='<real-upload-route>'
$env:DCC_E2E_TC007_CATEGORY_SELECTOR='<real-category-select-selector>'
$env:DCC_E2E_TC007_CATEGORY_OPTION_SELECTOR='<real-category-option-with-source-size-policy>'
$env:DCC_E2E_TC007_OVERSIZE_FILE_PATH='D:\path\to\oversized-real-sample.bin'
$env:DCC_E2E_TC007_FILE_INPUT_SELECTOR='<real-file-input-selector>'
$env:DCC_E2E_TC007_FIXED_PURPOSE_VALUE='SOURCE'
$env:DCC_E2E_TC007_SUBMIT_SELECTOR='<real-submit-selector>'
$env:DCC_E2E_TC007_ERROR_SELECTOR='<real-size-exceeded-error-selector>'
$env:DCC_E2E_TC007_AUDIT_VERIFY_URL='/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&requestId={requestId}'
$env:DCC_E2E_TC007_AUDIT_EXPECT_JSON_CONTAINS='UPLOAD||DENIED||SOURCE||DCC_UPLOAD_SIZE_EXCEEDED||{requestId}'
$env:DCC_E2E_TC007_TEMP_VERIFY_URL='/admin-api/dcc/controlled-files/upload-temporary/status?requestId={requestId}'
$env:DCC_E2E_TC007_TEMP_EXPECT_JSON_CONTAINS='requestId||{requestId}||temporaryFileCount||0||bindable||false'

$env:DCC_E2E_TC008_UPLOAD_PATH='<real-upload-route>'
$env:DCC_E2E_TC008_CATEGORY_SELECTOR='<real-category-select-selector>'
$env:DCC_E2E_TC008_CATEGORY_OPTION_SELECTOR='<real-category-option-with-source-size-policy>'
$env:DCC_E2E_TC008_CATEGORY_ID='<real-category-id>'
$env:DCC_E2E_TC008_DIRECTORY_ID='<real-leaf-directory-id>'
$env:DCC_E2E_TC008_FILE_NAME_SELECTOR='<real-file-name-input-selector>'
$env:DCC_E2E_TC008_FILE_NAME_VALUE='<unique-real-file-name>'
$env:DCC_E2E_TC008_FILE_NUMBER_SELECTOR='<real-file-number-input-selector>'
$env:DCC_E2E_TC008_FILE_NUMBER_VALUE='<unique-real-file-number>'
$env:DCC_E2E_TC008_PRODUCT_CODE_SELECTOR='<real-product-code-input-selector>'
$env:DCC_E2E_TC008_PRODUCT_CODE_VALUE='<14-char-real-product-code>'
$env:DCC_E2E_TC008_VERSION_SELECTOR='<real-version-input-selector>'
$env:DCC_E2E_TC008_VERSION_VALUE='<unique-real-version>'
$env:DCC_E2E_TC008_EFFECTIVE_DATE_SELECTOR='<real-effective-date-input-selector>'
$env:DCC_E2E_TC008_EFFECTIVE_DATE_VALUE='<YYYY-MM-DD>'
$env:DCC_E2E_TC008_FILE_PATH='D:\path\to\allowed-real-sample.pdf'
$env:DCC_E2E_TC008_FILE_INPUT_SELECTOR='<real-file-input-selector>'
$env:DCC_E2E_TC008_FIXED_PURPOSE_VALUE='SOURCE'
$env:DCC_E2E_TC008_UPLOAD_SUCCESS_SELECTOR='<real-upload-success-selector>'
$env:DCC_E2E_TC008_SUBMIT_SELECTOR='<real-business-submit-selector>'
$env:DCC_E2E_TC008_SUBMIT_SUCCESS_SELECTOR='<real-submit-success-selector>'
$env:DCC_E2E_TC008_FILE_ID_FAILURE_EXPECT_JSON_CONTAINS='<real-file-id-failure-token>'
$env:DCC_E2E_TC008_CROSS_SESSION_FAILURE_EXPECT_JSON_CONTAINS='<real-cross-session-failure-token>'
$env:DCC_E2E_TC008_EXPIRED_TICKET_FAILURE_EXPECT_JSON_CONTAINS='<real-expired-or-invalid-ticket-failure-token>'
```

## TC-E2E-009 Temporary File Lifecycle

```powershell
$env:DCC_E2E_TC009_FLOW_PATH='<real-upload-route>'
$env:DCC_E2E_TC009_CATEGORY_SELECTOR='<real-category-select-selector>'
$env:DCC_E2E_TC009_CATEGORY_OPTION_SELECTOR='<real-category-option-with-source-size-policy>'
$env:DCC_E2E_TC009_FILE_PATH='D:\path\to\allowed-real-sample.pdf'
$env:DCC_E2E_TC009_FILE_INPUT_SELECTOR='<real-file-input-selector>'
$env:DCC_E2E_TC009_UPLOAD_SUCCESS_SELECTOR='<real-upload-success-selector>'
$env:DCC_E2E_TC009_REMOVE_SELECTOR='<real-source-upload-remove-icon-selector>'
$env:DCC_E2E_TC009_TEMP_VERIFY_URL='/admin-api/dcc/controlled-files/upload-temporary/status?requestId={requestId}'
$env:DCC_E2E_TC009_TEMP_EXPECT_JSON_CONTAINS='requestId||{requestId}||bindable||false||CLEANED||USER_DISCARDED'
$env:DCC_E2E_TC009_AUDIT_VERIFY_URL='/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&requestId={requestId}'
$env:DCC_E2E_TC009_AUDIT_EXPECT_JSON_CONTAINS='TEMP_FILE||CLEANED||UPLOAD_TEMPORARY_FILE||USER_DISCARDED||{requestId}'
```

## TC-E2E-010 / 011 / 012 Download

```powershell
$env:DCC_E2E_TC010_DOWNLOAD_PATH='/dcc/controlled-file/detail/<real-downloadable-file-id>'
$env:DCC_E2E_TC010_DOWNLOAD_TRIGGER_SELECTOR='<real-download-button-selector>'
$env:DCC_E2E_TC010_DOWNLOAD_CONFIRM_SELECTOR='<real-download-confirm-button-selector>'
$env:DCC_E2E_TC010_EXPECT_ENCRYPTION_POLICY_VERSION='<real-encryption-policy-version>'
$env:DCC_E2E_TC010_AUDIT_VERIFY_URL='<final-api-url-to-verify-download-success-audit>'
$env:DCC_E2E_TC010_AUDIT_EXPECT_JSON_CONTAINS='DOWNLOAD||ALLOWED||<real-artifact-id>||<real-cipher-sha256>'
$env:DCC_E2E_TC010_AUDIT_EXPECT_FIELDS='actionType=DOWNLOAD||result=ALLOWED||sourceIp=*||userAgent=*||requestId=*'

$env:DCC_E2E_TC011_DOWNLOAD_PATH='/dcc/controlled-file/detail/<real-encryption-failure-file-id>'
$env:DCC_E2E_TC011_DOWNLOAD_TRIGGER_SELECTOR='<real-download-button-selector>'
$env:DCC_E2E_TC011_DOWNLOAD_CONFIRM_SELECTOR='<real-download-confirm-button-selector>'
$env:DCC_E2E_TC011_ERROR_SELECTOR='<real-encryption-error-selector>'
$env:DCC_E2E_TC011_AUDIT_VERIFY_URL='<final-api-url-to-verify-download-failure-audit>'
$env:DCC_E2E_TC011_AUDIT_EXPECT_JSON_CONTAINS='DOWNLOAD||DENIED||ENCRYPTION'
$env:DCC_E2E_TC011_AUDIT_EXPECT_FIELDS='actionType=DOWNLOAD||result=DENIED'

$env:DCC_E2E_TC012_PREFIX_DENIED_PATH='/dcc/controlled-file/detail/<real-prefix-but-denied-file-id>'
$env:DCC_E2E_TC012_PREFIX_DENIED_ABSENT_SELECTOR='<real-download-button-selector-that-must-not-be-visible>'
$env:DCC_E2E_TC012_PREFIX_DENIED_DETAIL_VERIFY_URL='<final-api-url-to-verify-denied-file-canDownload-false>'
$env:DCC_E2E_TC012_PREFIX_DENIED_DETAIL_EXPECT_FIELDS='id=<denied-file-id>||canPreview=true||canDownload=false'
$env:DCC_E2E_TC012_NO_PREFIX_ALLOWED_PATH='/dcc/controlled-file/detail/<real-no-prefix-but-allowed-file-id>'
$env:DCC_E2E_TC012_NO_PREFIX_ALLOWED_TRIGGER_SELECTOR='<real-download-button-selector>'
$env:DCC_E2E_TC012_NO_PREFIX_ALLOWED_CONFIRM_SELECTOR='<real-download-confirm-button-selector>'
$env:DCC_E2E_TC012_AUDIT_VERIFY_URL='<final-api-url-to-verify-policy-audit>'
$env:DCC_E2E_TC012_AUDIT_EXPECT_JSON_CONTAINS='DOWNLOAD||ALLOWED||<allowed-file-id>||<policy-user-id>'
$env:DCC_E2E_TC012_AUDIT_EXPECT_FIELDS='actionType=DOWNLOAD||result=ALLOWED||sourceIp=*||userAgent=*||requestId=*'
```

## TC-E2E-013 Audit Authorization

```powershell
$env:DCC_E2E_TC013_AUDIT_PATH='/dcc/controlled-file/logs'
$env:DCC_E2E_TC013_TRACE_INPUT_SELECTOR='<real-trace-input-selector>'
$env:DCC_E2E_TC013_TRACE_VALUE='<real-trace-code>'
$env:DCC_E2E_TC013_EVENT_INPUT_SELECTOR='<real-event-code-input-selector>'
$env:DCC_E2E_TC013_EVENT_VALUE='<real-access-event-code>'
$env:DCC_E2E_TC013_FILE_INPUT_SELECTOR='<real-file-filter-selector>'
$env:DCC_E2E_TC013_FILE_VALUE='<real-controlled-file-id-or-number>'
$env:DCC_E2E_TC013_USER_INPUT_SELECTOR='<real-user-filter-selector>'
$env:DCC_E2E_TC013_USER_VALUE='<real-user-id-or-name>'
$env:DCC_E2E_TC013_ACTION_SELECTOR='<real-action-type-filter-selector>'
$env:DCC_E2E_TC013_ACTION_OPTION_SELECTOR='<real-action-type-option-selector>'
$env:DCC_E2E_TC013_RESULT_SELECTOR_FIELD='<real-result-filter-selector>'
$env:DCC_E2E_TC013_RESULT_OPTION_SELECTOR='<real-result-option-selector>'
$env:DCC_E2E_TC013_TIME_START_SELECTOR='<real-start-time-selector>'
$env:DCC_E2E_TC013_TIME_START_VALUE='<real-start-time>'
$env:DCC_E2E_TC013_TIME_END_SELECTOR='<real-end-time-selector>'
$env:DCC_E2E_TC013_TIME_END_VALUE='<real-end-time>'
$env:DCC_E2E_TC013_SEARCH_SELECTOR='<real-search-button-selector>'
$env:DCC_E2E_TC013_RESULT_SELECTOR='<real-result-row-selector>'
$env:DCC_E2E_TC013_UNAUTHORIZED_SELECTOR='<real-ordinary-user-denied-selector>'
```

## TC-E2E-014 Frontend Fail Closed

```powershell
$env:DCC_E2E_TC014_PREVIEW_FAILURE_PATH='<real-broken-preview-file-route>'
$env:DCC_E2E_TC014_PREVIEW_TRIGGER_SELECTOR='<real-preview-trigger-selector>'
$env:DCC_E2E_TC014_PREVIEW_ERROR_SELECTOR='<real-preview-error-selector>'
$env:DCC_E2E_TC014_PREVIEW_AUDIT_VERIFY_URL='<final-api-url-to-verify-preview-failure-audit>'
$env:DCC_E2E_TC014_PREVIEW_AUDIT_EXPECT_JSON_CONTAINS='PREVIEW||DENIED||<broken-file-id>'
$env:DCC_E2E_TC014_PREVIEW_AUDIT_EXPECT_FIELDS='actionType=PREVIEW||result=DENIED||sourceIp=*||userAgent=*||requestId=*'
$env:DCC_E2E_TC014_UPLOAD_PATH='<real-upload-oversize-or-policy-route>'
$env:DCC_E2E_TC014_UPLOAD_CATEGORY_SELECTOR='<real-upload-category-selector>'
$env:DCC_E2E_TC014_UPLOAD_CATEGORY_OPTION_SELECTOR='<real-upload-category-option-selector>'
$env:DCC_E2E_TC014_UPLOAD_OVERSIZE_FILE_PATH='<real-oversize-file-path>'
$env:DCC_E2E_TC014_UPLOAD_FILE_INPUT_SELECTOR='<real-file-input-selector>'
$env:DCC_E2E_TC014_UPLOAD_FIXED_PURPOSE_VALUE='<real-fixed-purpose-value>'
$env:DCC_E2E_TC014_UPLOAD_SUBMIT_SELECTOR='<real-upload-submit-selector>'
$env:DCC_E2E_TC014_UPLOAD_ERROR_SELECTOR='<real-upload-error-selector>'
$env:DCC_E2E_TC014_UPLOAD_AUDIT_VERIFY_URL='<final-api-url-to-verify-upload-failure-audit>'
$env:DCC_E2E_TC014_UPLOAD_AUDIT_EXPECT_JSON_CONTAINS='UPLOAD||DENIED||{requestId}'
$env:DCC_E2E_TC014_DOWNLOAD_FAILURE_PATH='<real-broken-download-file-route>'
$env:DCC_E2E_TC014_DOWNLOAD_TRIGGER_SELECTOR='<real-download-trigger-selector>'
$env:DCC_E2E_TC014_DOWNLOAD_CONFIRM_SELECTOR='<real-download-confirm-selector>'
$env:DCC_E2E_TC014_DOWNLOAD_ERROR_SELECTOR='<real-download-error-selector>'
$env:DCC_E2E_TC014_DOWNLOAD_AUDIT_VERIFY_URL='<final-api-url-to-verify-download-failure-audit>'
$env:DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_JSON_CONTAINS='DOWNLOAD||DENIED||<broken-file-id>'
$env:DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_FIELDS='actionType=DOWNLOAD||result=DENIED||sourceIp=*||userAgent=*||requestId=*'
```

## TC-E2E-015 Style Screenshots

Separate parallel route and selector lists with `||`. The counts must match.

```powershell
$env:DCC_E2E_TC015_SCREENSHOT_PATHS='/dcc/controlled-file/logs||<upload-policy-route>||<preview-error-route>'
$env:DCC_E2E_TC015_READY_SELECTORS='<audit-ready-selector>||<upload-ready-selector>||<preview-error-ready-selector>'
$env:DCC_E2E_TC015_REQUIRED_VISIBLE_TEXTS='文控日志||<real-upload-page-title>||<real-preview-error-text>'
```

## TC-E2E-016 Non-DCC Regression

```powershell
$env:DCC_E2E_TC016_NON_DCC_PATH='<real-non-dcc-file-business-route>'
$env:DCC_E2E_TC016_NON_DCC_SUCCESS_SELECTOR='<real-non-dcc-success-selector>'
$env:DCC_E2E_TC016_DCC_BYPASS_PATH='<real-dcc-file-through-non-dcc-entry-route>'
$env:DCC_E2E_TC016_DCC_BYPASS_ERROR_SELECTOR='<real-dcc-bypass-denied-selector>'
```

## Evidence

Generated screenshots and downloaded evidence are written under:

```text
yudao-ui-admin-vue3/test-results/dcc-controlled-file-protection/
```

Do not commit `test-results/**`.
