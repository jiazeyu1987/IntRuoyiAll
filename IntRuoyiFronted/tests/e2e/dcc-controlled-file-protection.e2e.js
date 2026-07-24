const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')

const BLOCKED_EXIT_CODE = 2
const OUTPUT_DIR = path.resolve(__dirname, '../../test-results/dcc-controlled-file-protection')
const DEFAULT_TIMEOUT_MS = Number(process.env.DCC_E2E_TIMEOUT_MS || 30000)
const DOWNLOAD_TIMEOUT_MS = Number(process.env.DCC_E2E_DOWNLOAD_TIMEOUT_MS || 30000)

const forbiddenDccResponseKeys = new Set([
  'originalFileId',
  'sourceFileId',
  'drawingPdfFileId',
  'publishedFileId',
  'stampedFileId',
  'trainingRecordFileId',
  'outputFileId',
  'fileId',
  'fileUrl',
  'configId',
  'path',
  'storagePath',
  'sourcePath',
  'plainFileUrl',
  'tempFileUrl'
])

const releaseGates = {
  'RG-01': {
    env: 'DCC_E2E_RG01_ENCRYPTION_READY',
    description: 'download encryption gateway contract, real success artifact, and real failure states'
  },
  'RG-02': {
    env: 'DCC_E2E_RG02_UPLOAD_POLICY_READY',
    description: 'upload size policy values and real allowed/oversized samples'
  },
  'RG-03': {
    env: 'DCC_E2E_RG03_WATERMARK_TRACE_READY',
    description: 'watermark privacy fields and screenshot traceability acceptance standard'
  },
  'RG-04': {
    env: 'DCC_E2E_RG04_TEST_TENANT_READY',
    description: 'real test tenant accounts, DCC data, workflow paths, samples, and permissions'
  },
  'RG-05': {
    env: 'DCC_E2E_RG05_ONLYOFFICE_READY',
    description: 'OnlyOffice test service can pull tokenized DCC proxy document URLs'
  },
  'RG-06': {
    env: 'DCC_E2E_RG06_AUDIT_FAILURE_BOUNDARY_READY',
    description: 'audit failure transaction boundary and cleanup behavior are confirmed'
  },
  'RG-07': {
    env: 'DCC_E2E_RG07_DCC_SCOPE_STRATEGY_READY',
    description: 'DCC file scope recognition covers all controlled source/artifact kinds'
  }
}

const commonEnv = [
  'DCC_E2E_BASE_URL',
  'DCC_E2E_ALLOWED_BASE_URL_PATTERN',
  'DCC_E2E_ENVIRONMENT_NAME',
  'DCC_E2E_CONFIRM_TEST_TENANT_ONLY',
  'DCC_E2E_TENANT_NAME',
  'DCC_E2E_USERNAME',
  'DCC_E2E_PASSWORD'
]

const finalApiEnvSuffixes = ['FINAL_VERIFY_URL', 'AUDIT_VERIFY_URL', 'TEMP_VERIFY_URL']

function caseDef(id, title, gates, requiredEnv, run) {
  return { id, title, gates, requiredEnv: [...commonEnv, ...requiredEnv], run }
}

const cases = [
  caseDef(
    'TC-E2E-001',
    'real UI response field convergence',
    ['RG-04'],
    [
      'DCC_E2E_TC001_LIST_PATH',
      'DCC_E2E_TC001_LIST_READY_SELECTOR',
      'DCC_E2E_TC001_DETAIL_PATH',
      'DCC_E2E_TC001_DETAIL_READY_SELECTOR',
      'DCC_E2E_TC001_VERSION_PATH',
      'DCC_E2E_TC001_VERSION_READY_SELECTOR',
      'DCC_E2E_TC001_EXTERNAL_REVIEW_PATH',
      'DCC_E2E_TC001_EXTERNAL_REVIEW_READY_SELECTOR',
      'DCC_E2E_TC001_UPLOAD_PATH',
      'DCC_E2E_TC001_UPLOAD_READY_SELECTOR',
      'DCC_E2E_TC001_FINAL_VERIFY_URL',
      'DCC_E2E_TC001_FINAL_EXPECT_JSON_CONTAINS'
    ],
    runResponseFieldConvergence
  ),
  caseDef(
    'TC-E2E-002',
    'real UI direct link blocked and non-DCC regression',
    ['RG-04', 'RG-07'],
    [
      'DCC_E2E_TC002_DCC_DIRECT_URL',
      'DCC_E2E_TC002_DCC_DENIED_TEXT',
      'DCC_E2E_TC002_NON_DCC_DIRECT_URL',
      'DCC_E2E_TC002_AUDIT_VERIFY_URL',
      'DCC_E2E_TC002_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC002_AUDIT_EXPECT_FIELDS'
    ],
    runDirectLinkBoundary
  ),
  caseDef(
    'TC-E2E-003',
    'real UI controlled preview',
    ['RG-04'],
    [
      'DCC_E2E_TC003_DETAIL_PATH',
      'DCC_E2E_TC003_PREVIEW_TRIGGER_SELECTOR',
      'DCC_E2E_TC003_PREVIEW_READY_SELECTOR',
      'DCC_E2E_TC003_WATERMARK_SELECTOR',
      'DCC_E2E_TC003_FAILURE_PATH',
      'DCC_E2E_TC003_FAILURE_TRIGGER_SELECTOR',
      'DCC_E2E_TC003_FAILURE_ERROR_SELECTOR',
      'DCC_E2E_TC003_FAILURE_AUDIT_VERIFY_URL',
      'DCC_E2E_TC003_FAILURE_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC003_FAILURE_AUDIT_EXPECT_FIELDS'
    ],
    runControlledPreview
  ),
  caseDef(
    'TC-E2E-004',
    'screenshot watermark trace',
    ['RG-03', 'RG-04'],
    [
      'DCC_E2E_TC004_PREVIEW_PATH',
      'DCC_E2E_TC004_PREVIEW_READY_SELECTOR',
      'DCC_E2E_TC004_WATERMARK_SELECTOR',
      'DCC_E2E_TC004_WATERMARK_EXPECT_TEXT',
      'DCC_E2E_TC004_AUDIT_PATH',
      'DCC_E2E_TC004_AUDIT_TRACE_INPUT_SELECTOR',
      'DCC_E2E_TC004_AUDIT_SEARCH_SELECTOR',
      'DCC_E2E_TC004_AUDIT_RESULT_SELECTOR',
      'DCC_E2E_TC004_AUDIT_VERIFY_URL',
      'DCC_E2E_TC004_AUDIT_EXPECT_FIELDS'
    ],
    runWatermarkTrace
  ),
  caseDef(
    'TC-E2E-005',
    'real UI OnlyOffice readonly',
    ['RG-04', 'RG-05'],
    [
      'DCC_E2E_TC005_DETAIL_PATH',
      'DCC_E2E_TC005_OFFICE_TRIGGER_SELECTOR',
      'DCC_E2E_TC005_OFFICE_READY_SELECTOR',
      'DCC_E2E_TC005_FORBIDDEN_TOOLBAR_SELECTORS',
      'DCC_E2E_TC005_AUDIT_PATH',
      'DCC_E2E_TC005_AUDIT_EVENT_INPUT_SELECTOR',
      'DCC_E2E_TC005_AUDIT_SEARCH_SELECTOR',
      'DCC_E2E_TC005_AUDIT_RESULT_SELECTOR'
    ],
    runOnlyOfficeReadonly
  ),
  caseDef(
    'TC-E2E-006',
    'real UI upload policy missing or invalid',
    ['RG-02', 'RG-04'],
    [
      'DCC_E2E_TC006_UPLOAD_PATH',
      'DCC_E2E_TC006_CATEGORY_SELECTOR',
      'DCC_E2E_TC006_CATEGORY_OPTION_SELECTOR',
      'DCC_E2E_TC006_FILE_PATH',
      'DCC_E2E_TC006_FILE_INPUT_SELECTOR',
      'DCC_E2E_TC006_SUBMIT_SELECTOR',
      'DCC_E2E_TC006_ERROR_SELECTOR',
      'DCC_E2E_TC006_AUDIT_VERIFY_URL',
      'DCC_E2E_TC006_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC006_TEMP_VERIFY_URL',
      'DCC_E2E_TC006_TEMP_EXPECT_JSON_CONTAINS'
    ],
    runUploadPolicyMissingOrInvalid
  ),
  caseDef(
    'TC-E2E-007',
    'real UI upload size exceeded',
    ['RG-02', 'RG-04'],
    [
      'DCC_E2E_TC007_UPLOAD_PATH',
      'DCC_E2E_TC007_CATEGORY_SELECTOR',
      'DCC_E2E_TC007_CATEGORY_OPTION_SELECTOR',
      'DCC_E2E_TC007_OVERSIZE_FILE_PATH',
      'DCC_E2E_TC007_FILE_INPUT_SELECTOR',
      'DCC_E2E_TC007_SUBMIT_SELECTOR',
      'DCC_E2E_TC007_ERROR_SELECTOR',
      'DCC_E2E_TC007_AUDIT_VERIFY_URL',
      'DCC_E2E_TC007_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC007_TEMP_VERIFY_URL',
      'DCC_E2E_TC007_TEMP_EXPECT_JSON_CONTAINS'
    ],
    runUploadSizeExceeded
  ),
  caseDef(
    'TC-E2E-008',
    'real UI upload ticket success',
    ['RG-02', 'RG-04'],
    [
      'DCC_E2E_TC008_UPLOAD_PATH',
      'DCC_E2E_TC008_CATEGORY_SELECTOR',
      'DCC_E2E_TC008_CATEGORY_OPTION_SELECTOR',
      'DCC_E2E_TC008_FILE_NAME_SELECTOR',
      'DCC_E2E_TC008_FILE_NAME_VALUE',
      'DCC_E2E_TC008_FILE_NUMBER_SELECTOR',
      'DCC_E2E_TC008_FILE_NUMBER_VALUE',
      'DCC_E2E_TC008_PRODUCT_CODE_SELECTOR',
      'DCC_E2E_TC008_PRODUCT_CODE_VALUE',
      'DCC_E2E_TC008_VERSION_SELECTOR',
      'DCC_E2E_TC008_VERSION_VALUE',
      'DCC_E2E_TC008_EFFECTIVE_DATE_SELECTOR',
      'DCC_E2E_TC008_EFFECTIVE_DATE_VALUE',
      'DCC_E2E_TC008_CATEGORY_ID',
      'DCC_E2E_TC008_DIRECTORY_ID',
      'DCC_E2E_TC008_FILE_PATH',
      'DCC_E2E_TC008_FILE_INPUT_SELECTOR',
      'DCC_E2E_TC008_UPLOAD_SUCCESS_SELECTOR',
      'DCC_E2E_TC008_SUBMIT_SELECTOR',
      'DCC_E2E_TC008_SUBMIT_SUCCESS_SELECTOR',
      'DCC_E2E_TC008_FILE_ID_FAILURE_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC008_CROSS_SESSION_FAILURE_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC008_EXPIRED_TICKET_FAILURE_EXPECT_JSON_CONTAINS'
    ],
    runUploadTicketSuccess
  ),
  caseDef(
    'TC-E2E-009',
    'real UI temp file lifecycle',
    ['RG-04', 'RG-06'],
    [
      'DCC_E2E_TC009_FLOW_PATH',
      'DCC_E2E_TC009_CATEGORY_SELECTOR',
      'DCC_E2E_TC009_CATEGORY_OPTION_SELECTOR',
      'DCC_E2E_TC009_FILE_PATH',
      'DCC_E2E_TC009_FILE_INPUT_SELECTOR',
      'DCC_E2E_TC009_UPLOAD_SUCCESS_SELECTOR',
      'DCC_E2E_TC009_REMOVE_SELECTOR',
      'DCC_E2E_TC009_TEMP_VERIFY_URL',
      'DCC_E2E_TC009_TEMP_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC009_AUDIT_VERIFY_URL',
      'DCC_E2E_TC009_AUDIT_EXPECT_JSON_CONTAINS'
    ],
    runTempFileLifecycle
  ),
  caseDef(
    'TC-E2E-010',
    'real UI encrypted download success',
    ['RG-01', 'RG-04'],
    [
      'DCC_E2E_TC010_DOWNLOAD_PATH',
      'DCC_E2E_TC010_DOWNLOAD_TRIGGER_SELECTOR',
      'DCC_E2E_TC010_DOWNLOAD_CONFIRM_SELECTOR',
      'DCC_E2E_TC010_EXPECT_ENCRYPTION_POLICY_VERSION',
      'DCC_E2E_TC010_AUDIT_VERIFY_URL',
      'DCC_E2E_TC010_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC010_AUDIT_EXPECT_FIELDS'
    ],
    runEncryptedDownloadSuccess
  ),
  caseDef(
    'TC-E2E-011',
    'real UI encryption fail closed',
    ['RG-01', 'RG-04'],
    [
      'DCC_E2E_TC011_DOWNLOAD_PATH',
      'DCC_E2E_TC011_DOWNLOAD_TRIGGER_SELECTOR',
      'DCC_E2E_TC011_DOWNLOAD_CONFIRM_SELECTOR',
      'DCC_E2E_TC011_ERROR_SELECTOR',
      'DCC_E2E_TC011_AUDIT_VERIFY_URL',
      'DCC_E2E_TC011_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC011_AUDIT_EXPECT_FIELDS'
    ],
    runEncryptionFailClosed
  ),
  caseDef(
    'TC-E2E-012',
    'real UI download policy not prefix based',
    ['RG-04'],
    [
      'DCC_E2E_POLICY_USERNAME',
      'DCC_E2E_POLICY_PASSWORD',
      'DCC_E2E_TC012_PREFIX_DENIED_PATH',
      'DCC_E2E_TC012_PREFIX_DENIED_ABSENT_SELECTOR',
      'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_VERIFY_URL',
      'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_EXPECT_FIELDS',
      'DCC_E2E_TC012_NO_PREFIX_ALLOWED_PATH',
      'DCC_E2E_TC012_NO_PREFIX_ALLOWED_TRIGGER_SELECTOR',
      'DCC_E2E_TC012_NO_PREFIX_ALLOWED_CONFIRM_SELECTOR',
      'DCC_E2E_TC012_AUDIT_VERIFY_URL',
      'DCC_E2E_TC012_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC012_AUDIT_EXPECT_FIELDS'
    ],
    runDownloadPolicyNotPrefix
  ),
  caseDef(
    'TC-E2E-013',
    'real UI audit query authorization',
    ['RG-04', 'RG-06'],
    [
      'DCC_E2E_AUDITOR_USERNAME',
      'DCC_E2E_AUDITOR_PASSWORD',
      'DCC_E2E_ORDINARY_USERNAME',
      'DCC_E2E_ORDINARY_PASSWORD',
      'DCC_E2E_TC013_AUDIT_PATH',
      'DCC_E2E_TC013_TRACE_INPUT_SELECTOR',
      'DCC_E2E_TC013_TRACE_VALUE',
      'DCC_E2E_TC013_EVENT_INPUT_SELECTOR',
      'DCC_E2E_TC013_EVENT_VALUE',
      'DCC_E2E_TC013_FILE_INPUT_SELECTOR',
      'DCC_E2E_TC013_FILE_VALUE',
      'DCC_E2E_TC013_USER_INPUT_SELECTOR',
      'DCC_E2E_TC013_USER_VALUE',
      'DCC_E2E_TC013_ACTION_SELECTOR',
      'DCC_E2E_TC013_ACTION_OPTION_SELECTOR',
      'DCC_E2E_TC013_RESULT_SELECTOR_FIELD',
      'DCC_E2E_TC013_RESULT_OPTION_SELECTOR',
      'DCC_E2E_TC013_TIME_START_SELECTOR',
      'DCC_E2E_TC013_TIME_START_VALUE',
      'DCC_E2E_TC013_TIME_END_SELECTOR',
      'DCC_E2E_TC013_TIME_END_VALUE',
      'DCC_E2E_TC013_SEARCH_SELECTOR',
      'DCC_E2E_TC013_RESULT_SELECTOR',
      'DCC_E2E_TC013_UNAUTHORIZED_SELECTOR'
    ],
    runAuditQueryAuthorization
  ),
  caseDef(
    'TC-E2E-014',
    'real UI frontend fail closed',
    ['RG-04', 'RG-06'],
    [
      'DCC_E2E_TC014_PREVIEW_FAILURE_PATH',
      'DCC_E2E_TC014_PREVIEW_TRIGGER_SELECTOR',
      'DCC_E2E_TC014_PREVIEW_ERROR_SELECTOR',
      'DCC_E2E_TC014_PREVIEW_AUDIT_VERIFY_URL',
      'DCC_E2E_TC014_PREVIEW_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC014_PREVIEW_AUDIT_EXPECT_FIELDS',
      'DCC_E2E_TC014_UPLOAD_PATH',
      'DCC_E2E_TC014_UPLOAD_CATEGORY_SELECTOR',
      'DCC_E2E_TC014_UPLOAD_CATEGORY_OPTION_SELECTOR',
      'DCC_E2E_TC014_UPLOAD_OVERSIZE_FILE_PATH',
      'DCC_E2E_TC014_UPLOAD_FILE_INPUT_SELECTOR',
      'DCC_E2E_TC014_UPLOAD_FIXED_PURPOSE_VALUE',
      'DCC_E2E_TC014_UPLOAD_SUBMIT_SELECTOR',
      'DCC_E2E_TC014_UPLOAD_ERROR_SELECTOR',
      'DCC_E2E_TC014_UPLOAD_AUDIT_VERIFY_URL',
      'DCC_E2E_TC014_UPLOAD_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC014_DOWNLOAD_FAILURE_PATH',
      'DCC_E2E_TC014_DOWNLOAD_TRIGGER_SELECTOR',
      'DCC_E2E_TC014_DOWNLOAD_CONFIRM_SELECTOR',
      'DCC_E2E_TC014_DOWNLOAD_ERROR_SELECTOR',
      'DCC_E2E_TC014_DOWNLOAD_AUDIT_VERIFY_URL',
      'DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_JSON_CONTAINS',
      'DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_FIELDS'
    ],
    runFrontendFailClosed
  ),
  caseDef(
    'TC-E2E-015',
    'real UI style screenshots',
    ['RG-04'],
    [
      'DCC_E2E_TC015_SCREENSHOT_PATHS',
      'DCC_E2E_TC015_READY_SELECTORS',
      'DCC_E2E_TC015_REQUIRED_VISIBLE_TEXTS'
    ],
    runStyleScreenshots
  ),
  caseDef(
    'TC-E2E-016',
    'real UI non-DCC regression',
    ['RG-04'],
    [
      'DCC_E2E_TC016_NON_DCC_PATH',
      'DCC_E2E_TC016_NON_DCC_SUCCESS_SELECTOR',
      'DCC_E2E_TC016_DCC_BYPASS_PATH',
      'DCC_E2E_TC016_DCC_BYPASS_ERROR_SELECTOR'
    ],
    runNonDccRegression
  ),
  caseDef(
    'TC-E2E-017',
    'system full flow evidence rollup',
    ['RG-01', 'RG-02', 'RG-03', 'RG-04', 'RG-05', 'RG-06', 'RG-07'],
    [],
    runSystemFullFlowRollup
  )
]

const uploadPurposePrerequisites = [
  { caseId: 'TC-E2E-006', prefix: 'DCC_E2E_TC006' },
  { caseId: 'TC-E2E-007', prefix: 'DCC_E2E_TC007' },
  { caseId: 'TC-E2E-008', prefix: 'DCC_E2E_TC008' }
]

function getEnv(name) {
  const value = process.env[name]
  return value && value.trim() ? value.trim() : ''
}

function requireReadyGate(gateId) {
  const gate = releaseGates[gateId]
  return getEnv(gate.env).toLowerCase() === 'true'
}

function splitEnvList(name) {
  return getEnv(name)
    .split('||')
    .map((item) => item.trim())
    .filter(Boolean)
}

function isFileEnv(name) {
  return name.endsWith('_FILE_PATH') || name.endsWith('_OVERSIZE_FILE_PATH')
}

function isFinalApiEnv(name) {
  return finalApiEnvSuffixes.some((suffix) => name.endsWith(suffix))
}

function requiresFinalApiVerification(testCase) {
  return testCase.requiredEnv.some(isFinalApiEnv)
}

function validateUploadPurposePrerequisites(blockers) {
  const selectedCaseIds = activeCaseIds()
  for (const prerequisite of uploadPurposePrerequisites) {
    if (selectedCaseIds && !selectedCaseIds.has(prerequisite.caseId)) {
      continue
    }
    const selectorEnv = `${prerequisite.prefix}_PURPOSE_SELECTOR`
    const valueEnv = `${prerequisite.prefix}_PURPOSE_VALUE`
    const fixedPurposeEnv = `${prerequisite.prefix}_FIXED_PURPOSE_VALUE`
    const selector = getEnv(selectorEnv)
    const value = getEnv(valueEnv)
    const fixedPurpose = getEnv(fixedPurposeEnv)
    if ((selector || value) && fixedPurpose) {
      blockers.push({
        caseId: prerequisite.caseId,
        gateId: 'ENV',
        env: fixedPurposeEnv,
        reason: `choose either visible purpose mode (${selectorEnv}/${valueEnv}) or fixed purpose mode, not both`
      })
      continue
    }
    if (selector || value) {
      if (!selector || !value) {
        blockers.push({
          caseId: prerequisite.caseId,
          gateId: 'ENV',
          env: `${selectorEnv}/${valueEnv}`,
          reason: 'visible upload purpose mode requires both selector and value'
        })
      }
      continue
    }
    if (!fixedPurpose) {
      blockers.push({
        caseId: prerequisite.caseId,
        gateId: 'ENV',
        env: fixedPurposeEnv,
        reason: `required when ${selectorEnv} is not configured because the real upload page has a fixed purpose`
      })
    }
  }
}

function splitCaseSelection(value) {
  return value
    .split(/[,\s|]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function activeCaseIds() {
  const selection = getEnv('DCC_E2E_CASES')
  if (!selection) {
    return null
  }
  return new Set(splitCaseSelection(selection))
}

function selectedCases() {
  const selectedIds = activeCaseIds()
  if (!selectedIds) {
    return cases
  }
  const known = new Set(cases.map((testCase) => testCase.id))
  const unknown = [...selectedIds].filter((caseId) => !known.has(caseId))
  if (unknown.length > 0) {
    throw new Error(`Unknown DCC_E2E_CASES value: ${unknown.join(', ')}`)
  }
  return cases.filter((testCase) => selectedIds.has(testCase.id))
}

function validatePrerequisites(activeCases = cases) {
  const blockers = []
  for (const testCase of activeCases) {
    for (const gateId of testCase.gates) {
      const gate = releaseGates[gateId]
      if (!requireReadyGate(gateId)) {
        blockers.push({
          caseId: testCase.id,
          gateId,
          env: gate.env,
          reason: gate.description
        })
      }
    }
    for (const envName of testCase.requiredEnv) {
      const value = getEnv(envName)
      if (!value) {
        blockers.push({
          caseId: testCase.id,
          gateId: 'ENV',
          env: envName,
          reason: 'required real E2E input is missing'
        })
      } else if (isFileEnv(envName) && !fs.existsSync(path.resolve(value))) {
        blockers.push({
          caseId: testCase.id,
          gateId: 'ENV',
          env: envName,
          reason: `file does not exist: ${value}`
        })
      }
    }
  }
  validateUploadPurposePrerequisites(blockers)
  const baseUrl = getEnv('DCC_E2E_BASE_URL')
  const allowedPattern = getEnv('DCC_E2E_ALLOWED_BASE_URL_PATTERN')
  if (baseUrl && allowedPattern) {
    try {
      const allowedRegex = new RegExp(allowedPattern)
      if (!allowedRegex.test(baseUrl)) {
        blockers.push({
          caseId: 'TC-E2E-017',
          gateId: 'RG-04',
          env: 'DCC_E2E_ALLOWED_BASE_URL_PATTERN',
          reason: `base URL is outside the confirmed test environment: ${baseUrl}`
        })
      }
    } catch (error) {
      blockers.push({
        caseId: 'TC-E2E-017',
        gateId: 'ENV',
        env: 'DCC_E2E_ALLOWED_BASE_URL_PATTERN',
        reason: `invalid regular expression: ${error.message}`
      })
    }
  }
  const requiresApiBase = activeCases.some(requiresFinalApiVerification)
  const apiBaseUrl = getEnv('DCC_E2E_API_BASE_URL')
  const allowedApiPattern = getEnv('DCC_E2E_ALLOWED_API_BASE_URL_PATTERN')
  if (requiresApiBase && !apiBaseUrl) {
    blockers.push({
      caseId: 'TC-E2E-017',
      gateId: 'ENV',
      env: 'DCC_E2E_API_BASE_URL',
      reason: 'final API verification must use the backend API base URL, not the frontend SPA URL'
    })
  }
  if (requiresApiBase && !allowedApiPattern) {
    blockers.push({
      caseId: 'TC-E2E-017',
      gateId: 'ENV',
      env: 'DCC_E2E_ALLOWED_API_BASE_URL_PATTERN',
      reason: 'final API backend URL must be constrained to the confirmed test environment'
    })
  }
  if (apiBaseUrl && allowedApiPattern) {
    try {
      const allowedApiRegex = new RegExp(allowedApiPattern)
      if (!allowedApiRegex.test(apiBaseUrl)) {
        blockers.push({
          caseId: 'TC-E2E-017',
          gateId: 'RG-04',
          env: 'DCC_E2E_ALLOWED_API_BASE_URL_PATTERN',
          reason: `API base URL is outside the confirmed test environment: ${apiBaseUrl}`
        })
      }
    } catch (error) {
      blockers.push({
        caseId: 'TC-E2E-017',
        gateId: 'ENV',
        env: 'DCC_E2E_ALLOWED_API_BASE_URL_PATTERN',
        reason: `invalid regular expression: ${error.message}`
      })
    }
  }
  const environmentName = getEnv('DCC_E2E_ENVIRONMENT_NAME').toLowerCase()
  if (environmentName && !['test', 'testing', 'test-tenant'].includes(environmentName)) {
    blockers.push({
      caseId: 'TC-E2E-017',
      gateId: 'RG-04',
      env: 'DCC_E2E_ENVIRONMENT_NAME',
      reason: 'E2E write paths may run only against the confirmed test environment'
    })
  }
  if (getEnv('DCC_E2E_CONFIRM_TEST_TENANT_ONLY').toLowerCase() !== 'true') {
    blockers.push({
      caseId: 'TC-E2E-017',
      gateId: 'RG-04',
      env: 'DCC_E2E_CONFIRM_TEST_TENANT_ONLY',
      reason: 'explicit confirmation is required before mutating DCC test tenant data'
    })
  }
  if (getEnv('DCC_E2E_TENANT_NAME') === '芋道源码') {
    blockers.push({
      caseId: 'TC-E2E-017',
      gateId: 'RG-04',
      env: 'DCC_E2E_TENANT_NAME',
      reason: 'write-path E2E must not use the 芋道源码 tenant'
    })
  }
  return blockers
}

function printBlocked(blockers, activeCases = cases) {
  const unique = new Map()
  for (const blocker of blockers) {
    const key = `${blocker.gateId}:${blocker.env}:${blocker.reason}`
    if (!unique.has(key)) {
      unique.set(key, blocker)
    }
  }
  console.error('BLOCKED: DCC controlled file protection E2E prerequisites are missing.')
  for (const blocker of unique.values()) {
    console.error(`- BLOCKED: ${blocker.gateId} ${blocker.env} -> ${blocker.reason}`)
  }
  console.error('Blocked cases:')
  for (const testCase of activeCases) {
    const caseBlockers = blockers.filter((blocker) => blocker.caseId === testCase.id)
    if (caseBlockers.length > 0) {
      const labels = [...new Set(caseBlockers.map((blocker) => blocker.gateId))].join(',')
      console.error(`- ${testCase.id}: ${testCase.title} -> BLOCKED: ${labels}`)
    }
  }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for DCC E2E. Install dependencies so require('playwright') resolves.")
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function activeLoginForm(page) {
  const forms = page.locator('form.login-form')
  const count = await forms.count()
  for (let index = 0; index < count; index += 1) {
    const form = forms.nth(index)
    if ((await form.isVisible()) && (await form.locator('input[type="password"]').first().isVisible())) {
      return form
    }
  }
  throw new Error('No visible account login form found')
}

async function selectTenant(page, form, tenantName) {
  const tenantSelect = form.locator('.el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), tenantName, 'tenant')
    return
  }
  await tenantSelect.click()
  await fillFirstVisible(form.locator('.el-select__input'), tenantName, 'tenant')
  await page.keyboard.press('Enter')
}

async function login(page, role = 'default') {
  const baseUrl = getEnv('DCC_E2E_BASE_URL').replace(/\/+$/, '')
  const tenantName = getEnv('DCC_E2E_TENANT_NAME')
  const username =
    role === 'auditor'
      ? getEnv('DCC_E2E_AUDITOR_USERNAME')
      : role === 'ordinary'
        ? getEnv('DCC_E2E_ORDINARY_USERNAME')
        : role === 'policy'
          ? getEnv('DCC_E2E_POLICY_USERNAME')
          : getEnv('DCC_E2E_USERNAME')
  const password =
    role === 'auditor'
      ? getEnv('DCC_E2E_AUDITOR_PASSWORD')
      : role === 'ordinary'
        ? getEnv('DCC_E2E_ORDINARY_PASSWORD')
        : role === 'policy'
          ? getEnv('DCC_E2E_POLICY_PASSWORD')
          : getEnv('DCC_E2E_PASSWORD')

  await page.goto(`${baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle', { timeout: DEFAULT_TIMEOUT_MS }).catch(() => undefined)
  if (page.url().includes('/login')) {
    const loginForm = await activeLoginForm(page)
    await selectTenant(page, loginForm, tenantName)
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, 'username')
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), password, 'password')
    await loginForm.locator('button:has-text("登录")').first().click()
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: DEFAULT_TIMEOUT_MS })
  }
}

async function newLoggedInPage(browser, role = 'default') {
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 }, acceptDownloads: true })
  const page = await context.newPage()
  await login(page, role)
  return { context, page }
}

function makeUrl(pathOrUrl) {
  if (/^https?:\/\//i.test(pathOrUrl)) {
    return pathOrUrl
  }
  const baseUrl = getEnv('DCC_E2E_BASE_URL').replace(/\/+$/, '')
  const normalized = pathOrUrl.startsWith('/') ? pathOrUrl : `/${pathOrUrl}`
  return `${baseUrl}${normalized}`
}

function makeApiUrl(pathOrUrl) {
  if (/^https?:\/\//i.test(pathOrUrl)) {
    return pathOrUrl
  }
  if (!pathOrUrl.startsWith('/admin-api/')) {
    throw new Error(`Final API verification requires an /admin-api path or absolute backend URL: ${pathOrUrl}`)
  }
  const apiBaseUrl = getEnv('DCC_E2E_API_BASE_URL')
  if (!apiBaseUrl) {
    throw new Error('DCC_E2E_API_BASE_URL is required for final API verification')
  }
  return `${apiBaseUrl.replace(/\/+$/, '')}${pathOrUrl}`
}

function applyTemplate(value, templateValues = {}) {
  return Object.entries(templateValues).reduce(
    (result, [key, replacement]) => result.replaceAll(`{${key}}`, String(replacement)),
    value
  )
}

async function gotoAndWait(page, pathOrUrl, readySelector) {
  await page.goto(makeUrl(pathOrUrl), { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle', { timeout: DEFAULT_TIMEOUT_MS }).catch(() => undefined)
  if (readySelector) {
    await page.locator(readySelector).first().waitFor({ state: 'visible', timeout: DEFAULT_TIMEOUT_MS })
  }
}

async function clickRequired(page, selector) {
  await page.locator(selector).first().waitFor({ state: 'visible', timeout: DEFAULT_TIMEOUT_MS })
  await page.locator(selector).first().click()
}

async function fillRequired(page, selector, value) {
  await page.locator(selector).first().waitFor({ state: 'visible', timeout: DEFAULT_TIMEOUT_MS })
  await page.locator(selector).first().fill(value)
}

async function setFileRequired(page, selector, filePath) {
  const resolved = path.resolve(filePath)
  await page.locator(selector).first().setInputFiles(resolved)
}

async function selectRequired(page, triggerSelector, optionSelector, label) {
  await page.locator(triggerSelector).first().waitFor({ state: 'visible', timeout: DEFAULT_TIMEOUT_MS })
  await page.locator(triggerSelector).first().click()
  await page.locator(optionSelector).first().waitFor({ state: 'visible', timeout: DEFAULT_TIMEOUT_MS })
  await page.locator(optionSelector).first().click()
  await page.waitForLoadState('networkidle', { timeout: DEFAULT_TIMEOUT_MS }).catch(() => undefined)
  assert.ok(label, 'selectRequired label is required')
}

async function selectUploadCategory(page, opts) {
  const selector = getEnv(opts.categorySelectorEnv)
  const optionSelector = getEnv(opts.categoryOptionSelectorEnv)
  if (!selector || !optionSelector) {
    throw new Error(`${opts.label} requires category selector and option selector`)
  }
  await selectRequired(page, selector, optionSelector, `${opts.label} category`)
}

async function fillUploadBusinessFields(page, prefix) {
  const fields = [
    ['FILE_NAME_SELECTOR', 'FILE_NAME_VALUE'],
    ['FILE_NUMBER_SELECTOR', 'FILE_NUMBER_VALUE'],
    ['PRODUCT_CODE_SELECTOR', 'PRODUCT_CODE_VALUE'],
    ['VERSION_SELECTOR', 'VERSION_VALUE'],
    ['EFFECTIVE_DATE_SELECTOR', 'EFFECTIVE_DATE_VALUE']
  ]
  for (const [selectorSuffix, valueSuffix] of fields) {
    const selectorEnv = `${prefix}_${selectorSuffix}`
    const valueEnv = `${prefix}_${valueSuffix}`
    const selector = getEnv(selectorEnv)
    const value = getEnv(valueEnv)
    if (!selector || !value) {
      throw new Error(`${prefix} requires ${selectorEnv} and ${valueEnv}`)
    }
    await fillRequired(page, selector, value)
    if (selectorSuffix === 'EFFECTIVE_DATE_SELECTOR') {
      await page.keyboard.press('Enter')
      await page.keyboard.press('Tab')
    }
  }
}

async function applyUploadPurpose(page, opts) {
  const selector = getEnv(opts.purposeSelectorEnv)
  const value = getEnv(opts.purposeValueEnv)
  const fixedPurpose = getEnv(opts.fixedPurposeValueEnv)
  if ((selector || value) && fixedPurpose) {
    throw new Error(`${opts.label} must configure either visible purpose mode or fixed purpose mode, not both`)
  }
  if (selector || value) {
    if (!selector || !value) {
      throw new Error(`${opts.label} visible purpose mode requires both selector and value`)
    }
    await fillRequired(page, selector, value)
    return value
  }
  if (fixedPurpose) {
    return fixedPurpose
  }
  throw new Error(`${opts.label} requires ${opts.fixedPurposeValueEnv} when no visible purpose selector is configured`)
}

function watchDccResponses(page) {
  const responses = []
  const infraFileRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/infra/file')) {
      infraFileRequests.push(request.url())
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/dcc')) {
      return
    }
    const headers = response.headers()
    let body = null
    try {
      if ((headers['content-type'] || '').includes('application/json')) {
        body = await response.json()
      }
    } catch (error) {
      body = null
    }
    responses.push({ url, status: response.status(), headers, requestHeaders: response.request().headers(), body })
  })
  return { responses, infraFileRequests }
}

function walkObject(value, visitor, pathParts = []) {
  if (Array.isArray(value)) {
    value.forEach((item, index) => walkObject(item, visitor, [...pathParts, String(index)]))
    return
  }
  if (value && typeof value === 'object') {
    for (const [key, nested] of Object.entries(value)) {
      visitor(key, nested, [...pathParts, key])
      walkObject(nested, visitor, [...pathParts, key])
    }
  }
}

function assertNoForbiddenDccResponseFields(responses, label) {
  const violations = []
  for (const response of responses) {
    walkObject(response.body, (key, value, pathParts) => {
      if (forbiddenDccResponseKeys.has(key) && value !== null && value !== undefined && value !== '') {
        violations.push(`${response.url} -> ${pathParts.join('.')}`)
      }
    })
  }
  assert.deepEqual(violations, [], `${label} exposed forbidden DCC capability fields`)
}

function findLatestDccResponse(responses, urlPart, startIndex = 0) {
  const floor = Math.max(0, startIndex)
  for (let index = responses.length - 1; index >= floor; index -= 1) {
    const response = responses[index]
    if (response.url.includes(urlPart)) {
      return response
    }
  }
  return null
}

function findLatestDccResponseByBodyToken(responses, token, startIndex = 0) {
  const floor = Math.max(0, startIndex)
  for (let index = responses.length - 1; index >= floor; index -= 1) {
    const response = responses[index]
    if (JSON.stringify(response.body || '').includes(token)) {
      return response
    }
  }
  return null
}

function readDccResponseBodyString(response, fieldName, label) {
  const visited = []
  const visit = (value) => {
    if (Array.isArray(value)) {
      for (const item of value) {
        const nested = visit(item)
        if (nested) {
          return nested
        }
      }
      return null
    }
    if (value && typeof value === 'object') {
      if (typeof value[fieldName] === 'string' && value[fieldName].trim()) {
        return value[fieldName].trim()
      }
      visited.push(value)
      for (const nestedValue of Object.values(value)) {
        const nested = visit(nestedValue)
        if (nested) {
          return nested
        }
      }
    }
    return null
  }
  const resolved = visit(response && response.body)
  if (resolved) {
    return resolved
  }
  throw new Error(`${label} missing required ${fieldName}; configure the real input explicitly or fix preview metadata`)
}

function resolveUploadRequestId(captures, startIndex, label) {
  const uploadResponse = findLatestDccResponse(captures.responses, '/upload-preview', startIndex)
  assert.ok(uploadResponse, `${label} must observe the upload-preview response`)
  try {
    return readDccResponseBodyString(uploadResponse, 'requestId', label)
  } catch (error) {
    const requestId = uploadResponse.requestHeaders?.['x-dcc-request-id']
    assert.ok(requestId, `${label} must send X-DCC-Request-Id on upload-preview`)
    return requestId
  }
}

function resolveWatermarkTraceText(captures, startIndex = 0) {
  const explicitTraceText = getEnv('DCC_E2E_TC004_TRACE_TEXT')
  if (explicitTraceText) {
    return explicitTraceText
  }
  const metadata = findLatestDccResponse(captures.responses, '/preview-metadata', startIndex)
  return readDccResponseBodyString(metadata, 'watermarkTraceCode', 'TC-E2E-004 preview metadata')
}

function resolveOnlyOfficeAuditEventValue(captures, startIndex = 0) {
  const explicitEventValue = getEnv('DCC_E2E_TC005_AUDIT_EVENT_VALUE')
  if (explicitEventValue) {
    return explicitEventValue
  }
  const metadata = findLatestDccResponse(captures.responses, '/preview-metadata', startIndex)
  return readDccResponseBodyString(metadata, 'accessEventCode', 'TC-E2E-005 preview metadata')
}

function resolveOnlyOfficeBadTokenUrl(captures, startIndex = 0) {
  const explicitUrl = getEnv('DCC_E2E_TC005_BAD_TOKEN_URL')
  if (explicitUrl) {
    return explicitUrl
  }
  const metadata = findLatestDccResponse(captures.responses, '/preview-metadata', startIndex)
  const documentUrl = readDccResponseBodyString(metadata, 'onlyofficeDocumentUrl', 'TC-E2E-005 preview metadata')
  const url = new URL(documentUrl, getEnv('DCC_E2E_BASE_URL'))
  url.searchParams.set('token', 'invalid-e2e-token')
  return url.toString()
}

async function assertOnlyOfficeBadTokenDenied(page, badTokenResponse) {
  const badTokenStatus = badTokenResponse ? badTokenResponse.status() : 0
  if (badTokenStatus >= 400) {
    return
  }
  const deniedTextValue = getEnv('DCC_E2E_TC005_BAD_TOKEN_DENIED_TEXT')
  if (deniedTextValue && await page.locator(`text=${deniedTextValue}`).count() > 0) {
    return
  }
  assert.ok(badTokenResponse, 'bad-token denial response must be present')
  const bodyText = await badTokenResponse.text()
  let parsed
  assert.doesNotThrow(() => {
    parsed = JSON.parse(bodyText)
  }, 'bad-token denial response must be a JSON failure envelope when HTTP status is success')
  assert.notEqual(Number(parsed.code), 0, 'OnlyOffice bad token read must return a non-success failure envelope')
  assert.match(
    JSON.stringify(parsed),
    /OnlyOffice|token|令牌|预览/i,
    'OnlyOffice bad token failure envelope must identify the tokenized preview read boundary'
  )
}

function assertBodyContainsTokens(bodyText, envName, label, templateValues = {}) {
  for (const token of splitEnvList(envName)) {
    const expectedToken = applyTemplate(token, templateValues)
    assert.ok(
      bodyText.includes(expectedToken),
      `${label} final API response must include token: ${expectedToken}; body=${bodyText.slice(0, 600)}`
    )
  }
}

function assertBodyExcludesTokens(bodyText, envName, label, templateValues = {}) {
  for (const token of splitEnvList(envName)) {
    const forbiddenToken = applyTemplate(token, templateValues)
    assert.ok(!bodyText.includes(forbiddenToken), `${label} final API response must not include token: ${forbiddenToken}`)
  }
}

function hasFieldValue(value, fieldName, expected) {
  if (Array.isArray(value)) {
    return value.some((item) => hasFieldValue(item, fieldName, expected))
  }
  if (value && typeof value === 'object') {
    return Object.entries(value).some(([key, nested]) => {
      if (key === fieldName && String(nested) === expected) {
        return true
      }
      return hasFieldValue(nested, fieldName, expected)
    })
  }
  return false
}

function hasNonEmptyFieldValue(value, fieldName) {
  if (Array.isArray(value)) {
    return value.some((item) => hasNonEmptyFieldValue(item, fieldName))
  }
  if (value && typeof value === 'object') {
    return Object.entries(value).some(([key, nested]) => {
      if (key === fieldName && nested !== null && nested !== undefined && String(nested).trim() !== '') {
        return true
      }
      return hasNonEmptyFieldValue(nested, fieldName)
    })
  }
  return false
}

function assertJsonFieldExpectations(body, envName, label, templateValues = {}) {
  for (const pair of splitEnvList(envName)) {
    const separatorIndex = pair.indexOf('=')
    assert.ok(separatorIndex > 0, `${envName} item must use field=value syntax: ${pair}`)
    const fieldName = pair.slice(0, separatorIndex)
    const expected = applyTemplate(pair.slice(separatorIndex + 1), templateValues)
    if (expected === '*') {
      assert.ok(
        hasNonEmptyFieldValue(body, fieldName),
        `${label} final API response must include non-empty ${fieldName}`
      )
      continue
    }
    assert.ok(hasFieldValue(body, fieldName, expected), `${label} final API response must include ${fieldName}=${expected}`)
  }
}

async function authenticatedPageGet(page, url) {
  return page.evaluate(async ({ targetUrl }) => {
    const readCacheValue = (key) => {
      const raw = window.localStorage.getItem(key)
      if (!raw) {
        return ''
      }
      try {
        const item = JSON.parse(raw)
        if (item && typeof item === 'object' && Object.prototype.hasOwnProperty.call(item, 'v')) {
          try {
            return JSON.parse(item.v)
          } catch (error) {
            return item.v
          }
        }
        return item
      } catch (error) {
        return raw
      }
    }
    const accessToken = readCacheValue('ACCESS_TOKEN')
    const tenantId = readCacheValue('tenantId')
    const visitTenantId = readCacheValue('visitTenantId')
    const headers = {
      'Cache-Control': 'no-cache',
      Pragma: 'no-cache'
    }
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`
    }
    if (tenantId) {
      headers['tenant-id'] = String(tenantId)
    }
    if (visitTenantId && accessToken) {
      headers['visit-tenant-id'] = String(visitTenantId)
    }
    const response = await window.fetch(targetUrl, { method: 'GET', headers })
    const responseHeaders = {}
    response.headers.forEach((value, key) => {
      responseHeaders[key.toLowerCase()] = value
    })
    return {
      ok: response.ok,
      status: response.status,
      headers: responseHeaders,
      bodyText: await response.text(),
      auth: {
        hasAccessToken: Boolean(accessToken),
        hasTenantId: Boolean(tenantId)
      }
    }
  }, { targetUrl: url })
}

async function authenticatedPagePostJson(page, url, payload) {
  return page.evaluate(async ({ targetUrl, requestPayload }) => {
    const readCacheValue = (key) => {
      const raw = window.localStorage.getItem(key)
      if (!raw) {
        return ''
      }
      try {
        const item = JSON.parse(raw)
        if (item && typeof item === 'object' && Object.prototype.hasOwnProperty.call(item, 'v')) {
          try {
            return JSON.parse(item.v)
          } catch (error) {
            return item.v
          }
        }
        return item
      } catch (error) {
        return raw
      }
    }
    const accessToken = readCacheValue('ACCESS_TOKEN')
    const tenantId = readCacheValue('tenantId')
    const visitTenantId = readCacheValue('visitTenantId')
    const headers = {
      'Cache-Control': 'no-cache',
      Pragma: 'no-cache',
      'Content-Type': 'application/json'
    }
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`
    }
    if (tenantId) {
      headers['tenant-id'] = String(tenantId)
    }
    if (visitTenantId && accessToken) {
      headers['visit-tenant-id'] = String(visitTenantId)
    }
    const response = await window.fetch(targetUrl, {
      method: 'POST',
      headers,
      body: JSON.stringify(requestPayload)
    })
    const responseHeaders = {}
    response.headers.forEach((value, key) => {
      responseHeaders[key.toLowerCase()] = value
    })
    return {
      ok: response.ok,
      status: response.status,
      headers: responseHeaders,
      bodyText: await response.text(),
      auth: {
        hasAccessToken: Boolean(accessToken),
        hasTenantId: Boolean(tenantId)
      }
    }
  }, { targetUrl: url, requestPayload: payload })
}

async function verifyFinalApi(page, envName, label, expectations = {}) {
  const templateValues = expectations.templateValues || {}
  const url = applyTemplate(getEnv(envName), templateValues)
  if (!url) {
    return
  }
  const response = await authenticatedPageGet(page, makeApiUrl(url))
  assert.ok(response.auth.hasAccessToken, `${label} final API verification requires ACCESS_TOKEN from browser storage`)
  assert.ok(response.auth.hasTenantId, `${label} final API verification requires tenant-id from browser storage`)
  assert.ok(response.ok, `${label} final API verification failed with HTTP ${response.status}`)
  const bodyText = response.bodyText
  const contentType = response.headers['content-type'] || ''
  if (contentType.includes('application/json')) {
    const body = JSON.parse(bodyText)
    assertNoForbiddenDccResponseFields([{ url, body }], `${label} final API`)
    if (expectations.expectedFieldsEnv) {
      assertJsonFieldExpectations(body, expectations.expectedFieldsEnv, label, templateValues)
    }
  }
  if (expectations.expectedContainsEnv) {
    assertBodyContainsTokens(bodyText, expectations.expectedContainsEnv, label, templateValues)
  }
  if (expectations.forbiddenContainsEnv) {
    assertBodyExcludesTokens(bodyText, expectations.forbiddenContainsEnv, label, templateValues)
  }
}

async function postSubmitExpectFailure(page, payload, expectedContainsEnv, label) {
  const response = await authenticatedPagePostJson(
    page,
    makeApiUrl('/admin-api/dcc/controlled-files/submit'),
    payload
  )
  assert.ok(response.auth.hasAccessToken, `${label} POST verification requires ACCESS_TOKEN from browser storage`)
  assert.ok(response.auth.hasTenantId, `${label} POST verification requires tenant-id from browser storage`)
  assert.ok(response.ok, `${label} POST verification failed with HTTP ${response.status}`)
  const parsed = JSON.parse(response.bodyText)
  assert.notEqual(Number(parsed.code), 0, `${label} must fail closed instead of submitting`)
  assertNoForbiddenDccResponseFields([{ url: '/admin-api/dcc/controlled-files/submit', body: parsed }], label)
  assertBodyContainsTokens(response.bodyText, expectedContainsEnv, label)
}

async function screenshot(page, name, selector) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const fileName = `${name.replace(/[^a-zA-Z0-9_-]/g, '-')}.png`
  const filePath = path.join(OUTPUT_DIR, fileName)
  if (selector) {
    await page.locator(selector).first().screenshot({ path: filePath })
  } else {
    await page.screenshot({ path: filePath, fullPage: true })
  }
  return filePath
}

async function assertNoVisible(page, selector, message) {
  const locator = page.locator(selector)
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    if (await locator.nth(index).isVisible()) {
      throw new Error(message)
    }
  }
}

async function assertPageDoesNotExposeForbiddenCapabilityText(page, label) {
  const bodyText = await page.locator('body').innerText({ timeout: DEFAULT_TIMEOUT_MS })
  const visibleForbiddenTokens = [
    ...forbiddenDccResponseKeys,
    ...splitEnvList('DCC_E2E_FORBIDDEN_VISIBLE_TEXTS'),
    ...splitEnvList('DCC_E2E_TC001_FORBIDDEN_VISIBLE_TEXTS')
  ]
  const violations = visibleForbiddenTokens.filter((token) => token && bodyText.includes(token))
  assert.deepEqual(violations, [], `${label} page exposes forbidden DCC capability text`)
}

function splitRequiredTextGroups(name) {
  return getEnv(name)
    .split('||')
    .map((group) => group.split('&&').map((text) => text.trim()).filter(Boolean))
}

async function assertDccOperationalStyle(page, label, requiredTexts) {
  const bodyText = await page.locator('body').innerText({ timeout: DEFAULT_TIMEOUT_MS })
  for (const text of requiredTexts) {
    assert.ok(bodyText.includes(text), `${label} must include required operational text: ${text}`)
  }
  const violations = await page.evaluate(() => {
    const selectors = [
      '.hero',
      '[class*="hero"]',
      '[class*="orb"]',
      '[class*="bokeh"]',
      '[class*="gradient"]',
      '.el-card .el-card'
    ]
    const matches = selectors.filter((selector) => document.querySelector(selector))
    const headingTooLarge = Array.from(document.querySelectorAll('h1,h2')).some((node) => {
      const fontSize = Number.parseFloat(window.getComputedStyle(node).fontSize || '0')
      return fontSize > 32
    })
    const forbiddenText = ['mock', 'Mock', '测试入口', '仅用于测试', '占位'].filter((token) =>
      document.body.innerText.includes(token)
    )
    return { matches, headingTooLarge, forbiddenText }
  })
  assert.deepEqual(violations.matches, [], `${label} must not use hero/orb/gradient/nested-card styling`)
  assert.equal(violations.headingTooLarge, false, `${label} must use compact operational heading scale`)
  assert.deepEqual(violations.forbiddenText, [], `${label} must not expose test or placeholder copy`)
}

function assertNewResponsesDoNotContainToken(captures, startIndex, token, label) {
  const offenders = captures.responses
    .slice(startIndex)
    .filter((response) => JSON.stringify(response.body || '').includes(token))
    .map((response) => response.url)
  assert.deepEqual(offenders, [], `${label} must not expose ${token}`)
}

async function runFailureInteraction(page, opts, label) {
  await gotoAndWait(page, getEnv(opts.pathEnv), null)
  const downloadPromise = opts.expectNoDownload
    ? page.waitForEvent('download', { timeout: 3000 }).then(() => 'downloaded').catch(() => 'no-download')
    : Promise.resolve('no-download')
  await clickRequired(page, getEnv(opts.triggerSelectorEnv))
  await page.locator(getEnv(opts.errorSelectorEnv)).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  assert.equal(await downloadPromise, 'no-download', `${label} must not create a browser download`)
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function sha256Buffer(buffer) {
  return crypto.createHash('sha256').update(buffer).digest('hex')
}

async function runUploadFlow(page, opts) {
  await gotoAndWait(page, getEnv(opts.pathEnv), null)
  await selectUploadCategory(page, opts)
  if (opts.businessPrefix) {
    await fillUploadBusinessFields(page, opts.businessPrefix)
  }
  await page.locator(getEnv(opts.fileInputEnv)).first().waitFor({ state: 'attached', timeout: DEFAULT_TIMEOUT_MS })
  await setFileRequired(page, getEnv(opts.fileInputEnv), getEnv(opts.filePathEnv))
  await applyUploadPurpose(page, opts)
  await clickRequired(page, getEnv(opts.submitSelectorEnv))
}

async function auditSearch(page, opts) {
  await gotoAndWait(page, getEnv(opts.pathEnv), null)
  const value = opts.value !== undefined ? opts.value : getEnv(opts.valueEnv)
  if (!value) {
    throw new Error(`${opts.valueEnv || 'audit value'} is required for audit search`)
  }
  await fillRequired(page, getEnv(opts.inputSelectorEnv), value)
  await clickRequired(page, getEnv(opts.searchSelectorEnv))
  const resultRow = page.locator(getEnv(opts.resultSelectorEnv)).first()
  await resultRow.waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  return resultRow.innerText()
}

async function runResponseFieldConvergence(session, evidence) {
  const { page, captures, context } = session
  await gotoAndWait(page, getEnv('DCC_E2E_TC001_LIST_PATH'), getEnv('DCC_E2E_TC001_LIST_READY_SELECTOR'))
  await assertPageDoesNotExposeForbiddenCapabilityText(page, 'TC-E2E-001 list')
  await gotoAndWait(page, getEnv('DCC_E2E_TC001_DETAIL_PATH'), getEnv('DCC_E2E_TC001_DETAIL_READY_SELECTOR'))
  await assertPageDoesNotExposeForbiddenCapabilityText(page, 'TC-E2E-001 detail')
  await gotoAndWait(page, getEnv('DCC_E2E_TC001_VERSION_PATH'), getEnv('DCC_E2E_TC001_VERSION_READY_SELECTOR'))
  await assertPageDoesNotExposeForbiddenCapabilityText(page, 'TC-E2E-001 version')
  await gotoAndWait(
    page,
    getEnv('DCC_E2E_TC001_EXTERNAL_REVIEW_PATH'),
    getEnv('DCC_E2E_TC001_EXTERNAL_REVIEW_READY_SELECTOR')
  )
  await assertPageDoesNotExposeForbiddenCapabilityText(page, 'TC-E2E-001 external review')
  await gotoAndWait(page, getEnv('DCC_E2E_TC001_UPLOAD_PATH'), getEnv('DCC_E2E_TC001_UPLOAD_READY_SELECTOR'))
  await assertPageDoesNotExposeForbiddenCapabilityText(page, 'TC-E2E-001 upload')
  assertNoForbiddenDccResponseFields(captures.responses, 'TC-E2E-001')
  await verifyFinalApi(page, 'DCC_E2E_TC001_FINAL_VERIFY_URL', 'TC-E2E-001', {
    expectedContainsEnv: 'DCC_E2E_TC001_FINAL_EXPECT_JSON_CONTAINS'
  })
  evidence.push('TC-E2E-001')
}

async function runDirectLinkBoundary(session, evidence) {
  const { page, context } = session
  const directPage = await context.newPage()
  try {
    const dccResponse = await directPage.goto(getEnv('DCC_E2E_TC002_DCC_DIRECT_URL'), { waitUntil: 'domcontentloaded' })
    const dccStatus = dccResponse ? dccResponse.status() : 0
    const deniedText = await directPage.locator(`text=${getEnv('DCC_E2E_TC002_DCC_DENIED_TEXT')}`).count()
    assert.ok(dccStatus >= 400 || deniedText > 0, 'DCC direct link must be denied in browser context')

    const nonDccDownloadPromise = directPage
      .waitForEvent('download', { timeout: DEFAULT_TIMEOUT_MS })
      .catch(() => null)
    let nonDccResponse = null
    let nonDccError = null
    try {
      nonDccResponse = await directPage.goto(getEnv('DCC_E2E_TC002_NON_DCC_DIRECT_URL'), {
        waitUntil: 'domcontentloaded'
      })
    } catch (error) {
      nonDccError = error
    }
    const nonDccDownload = await nonDccDownloadPromise
    assert.ok(
      nonDccDownload || (nonDccResponse && nonDccResponse.ok()),
      `Non-DCC direct link must either start a download or return the original file stream: ${nonDccError?.message || ''}`
    )
    if (nonDccResponse) {
      assert.ok(
        !((nonDccResponse.headers()['content-type'] || '').toLowerCase().includes('application/json')),
        'Non-DCC direct link must return the original file stream, not a JSON failure envelope'
      )
    }
    await verifyFinalApi(page, 'DCC_E2E_TC002_AUDIT_VERIFY_URL', 'TC-E2E-002', {
      expectedContainsEnv: 'DCC_E2E_TC002_AUDIT_EXPECT_JSON_CONTAINS',
      expectedFieldsEnv: 'DCC_E2E_TC002_AUDIT_EXPECT_FIELDS'
    })
  } finally {
    await directPage.close()
  }
  evidence.push('TC-E2E-002')
}

async function runControlledPreview(session, evidence) {
  const { page, captures } = session
  const startResponseCount = captures.responses.length
  await gotoAndWait(page, getEnv('DCC_E2E_TC003_DETAIL_PATH'), null)
  await clickRequired(page, getEnv('DCC_E2E_TC003_PREVIEW_TRIGGER_SELECTOR'))
  await page.locator(getEnv('DCC_E2E_TC003_PREVIEW_READY_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  await page.locator(getEnv('DCC_E2E_TC003_WATERMARK_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  const metadata = findLatestDccResponse(captures.responses, '/preview-metadata', startResponseCount)
  assert.ok(metadata, 'Preview metadata response must be observed')
  for (const key of ['viewerToken', 'viewerTokenId', 'viewerTokenNonce', 'accessEventCode', 'watermarkTraceCode', 'watermark']) {
    assert.ok(JSON.stringify(metadata.body).includes(key), `Preview metadata must include ${key}`)
  }
  assertNoForbiddenDccResponseFields([metadata], 'TC-E2E-003 metadata')
  await screenshot(page, 'TC-E2E-003-preview', getEnv('DCC_E2E_TC003_PREVIEW_READY_SELECTOR'))
  const startInfraCount = captures.infraFileRequests.length
  await runFailureInteraction(
    page,
    {
      pathEnv: 'DCC_E2E_TC003_FAILURE_PATH',
      triggerSelectorEnv: 'DCC_E2E_TC003_FAILURE_TRIGGER_SELECTOR',
      errorSelectorEnv: 'DCC_E2E_TC003_FAILURE_ERROR_SELECTOR'
    },
    'TC-E2E-003 preview failure'
  )
  await verifyFinalApi(page, 'DCC_E2E_TC003_FAILURE_AUDIT_VERIFY_URL', 'TC-E2E-003 preview failure audit', {
    expectedContainsEnv: 'DCC_E2E_TC003_FAILURE_AUDIT_EXPECT_JSON_CONTAINS',
    expectedFieldsEnv: 'DCC_E2E_TC003_FAILURE_AUDIT_EXPECT_FIELDS'
  })
  assert.equal(
    captures.infraFileRequests.length,
    startInfraCount,
    'TC-E2E-003 failure path must not call old infra file endpoints'
  )
  evidence.push('TC-E2E-003')
}

async function runWatermarkTrace(session, evidence) {
  const { page, captures } = session
  const startResponseCount = captures.responses.length
  await gotoAndWait(page, getEnv('DCC_E2E_TC004_PREVIEW_PATH'), getEnv('DCC_E2E_TC004_PREVIEW_READY_SELECTOR'))
  const watermark = page.locator(getEnv('DCC_E2E_TC004_WATERMARK_SELECTOR')).first()
  await watermark.waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  const traceText = resolveWatermarkTraceText(captures, startResponseCount)
  const watermarkText = await watermark.innerText()
  assert.ok(
    watermarkText.includes(traceText),
    `TC-E2E-004 screenshot watermark must include trace code ${traceText}; text=${watermarkText}`
  )
  assert.ok(
    watermarkText.includes(getEnv('DCC_E2E_TC004_WATERMARK_EXPECT_TEXT')),
    `TC-E2E-004 screenshot watermark must include expected actor text; text=${watermarkText}`
  )
  await page.locator(`text=${traceText}`).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  await screenshot(page, 'TC-E2E-004-watermark', getEnv('DCC_E2E_TC004_WATERMARK_SELECTOR'))
  const auditRowText = await auditSearch(page, {
    pathEnv: 'DCC_E2E_TC004_AUDIT_PATH',
    inputSelectorEnv: 'DCC_E2E_TC004_AUDIT_TRACE_INPUT_SELECTOR',
    value: traceText,
    searchSelectorEnv: 'DCC_E2E_TC004_AUDIT_SEARCH_SELECTOR',
    resultSelectorEnv: 'DCC_E2E_TC004_AUDIT_RESULT_SELECTOR'
  })
  assert.ok(
    auditRowText.includes(traceText),
    `TC-E2E-004 audit result row must include the screenshot trace code ${traceText}; row=${auditRowText}`
  )
  await verifyFinalApi(page, 'DCC_E2E_TC004_AUDIT_VERIFY_URL', 'TC-E2E-004 audit API', {
    expectedContainsEnv: 'DCC_E2E_TC004_AUDIT_EXPECT_JSON_CONTAINS',
    expectedFieldsEnv: 'DCC_E2E_TC004_AUDIT_EXPECT_FIELDS',
    templateValues: { traceText }
  })
  await screenshot(page, 'TC-E2E-004-audit', getEnv('DCC_E2E_TC004_AUDIT_RESULT_SELECTOR'))
  evidence.push('TC-E2E-004')
}

async function runOnlyOfficeReadonly(session, evidence) {
  const { page, captures } = session
  const startResponseCount = captures.responses.length
  await gotoAndWait(page, getEnv('DCC_E2E_TC005_DETAIL_PATH'), null)
  await clickRequired(page, getEnv('DCC_E2E_TC005_OFFICE_TRIGGER_SELECTOR'))
  await page.locator(getEnv('DCC_E2E_TC005_OFFICE_READY_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  for (const selector of splitEnvList('DCC_E2E_TC005_FORBIDDEN_TOOLBAR_SELECTORS')) {
    await assertNoVisible(page, selector, `OnlyOffice forbidden control is visible: ${selector}`)
  }
  await screenshot(page, 'TC-E2E-005-onlyoffice', getEnv('DCC_E2E_TC005_OFFICE_READY_SELECTOR'))
  const auditEventValue = resolveOnlyOfficeAuditEventValue(captures, startResponseCount)
  await auditSearch(page, {
    pathEnv: 'DCC_E2E_TC005_AUDIT_PATH',
    inputSelectorEnv: 'DCC_E2E_TC005_AUDIT_EVENT_INPUT_SELECTOR',
    value: auditEventValue,
    searchSelectorEnv: 'DCC_E2E_TC005_AUDIT_SEARCH_SELECTOR',
    resultSelectorEnv: 'DCC_E2E_TC005_AUDIT_RESULT_SELECTOR'
  })
  const badTokenResponse = await page.goto(resolveOnlyOfficeBadTokenUrl(captures, startResponseCount), {
    waitUntil: 'domcontentloaded'
  })
  await assertOnlyOfficeBadTokenDenied(page, badTokenResponse)
  evidence.push('TC-E2E-005')
}

async function runUploadPolicyMissingOrInvalid(session, evidence) {
  const { page, captures, context } = session
  const startResponseCount = captures.responses.length
  await runUploadFlow(page, {
    pathEnv: 'DCC_E2E_TC006_UPLOAD_PATH',
    categorySelectorEnv: 'DCC_E2E_TC006_CATEGORY_SELECTOR',
    categoryOptionSelectorEnv: 'DCC_E2E_TC006_CATEGORY_OPTION_SELECTOR',
    fileInputEnv: 'DCC_E2E_TC006_FILE_INPUT_SELECTOR',
    filePathEnv: 'DCC_E2E_TC006_FILE_PATH',
    purposeSelectorEnv: 'DCC_E2E_TC006_PURPOSE_SELECTOR',
    purposeValueEnv: 'DCC_E2E_TC006_PURPOSE_VALUE',
    fixedPurposeValueEnv: 'DCC_E2E_TC006_FIXED_PURPOSE_VALUE',
    submitSelectorEnv: 'DCC_E2E_TC006_SUBMIT_SELECTOR',
    label: 'TC-E2E-006 upload purpose'
  })
  await page.locator(getEnv('DCC_E2E_TC006_ERROR_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  const requestId = resolveUploadRequestId(captures, startResponseCount, 'TC-E2E-006 upload')
  assertNewResponsesDoNotContainToken(captures, startResponseCount, 'uploadTicket', 'TC-E2E-006')
  await verifyFinalApi(page, 'DCC_E2E_TC006_AUDIT_VERIFY_URL', 'TC-E2E-006 audit', {
    expectedContainsEnv: 'DCC_E2E_TC006_AUDIT_EXPECT_JSON_CONTAINS',
    templateValues: { requestId }
  })
  await verifyFinalApi(page, 'DCC_E2E_TC006_TEMP_VERIFY_URL', 'TC-E2E-006 temp file', {
    expectedContainsEnv: 'DCC_E2E_TC006_TEMP_EXPECT_JSON_CONTAINS',
    templateValues: { requestId }
  })
  evidence.push('TC-E2E-006')
}

async function runUploadSizeExceeded(session, evidence) {
  const { page, captures, context } = session
  const startResponseCount = captures.responses.length
  await runUploadFlow(page, {
    pathEnv: 'DCC_E2E_TC007_UPLOAD_PATH',
    categorySelectorEnv: 'DCC_E2E_TC007_CATEGORY_SELECTOR',
    categoryOptionSelectorEnv: 'DCC_E2E_TC007_CATEGORY_OPTION_SELECTOR',
    fileInputEnv: 'DCC_E2E_TC007_FILE_INPUT_SELECTOR',
    filePathEnv: 'DCC_E2E_TC007_OVERSIZE_FILE_PATH',
    purposeSelectorEnv: 'DCC_E2E_TC007_PURPOSE_SELECTOR',
    purposeValueEnv: 'DCC_E2E_TC007_PURPOSE_VALUE',
    fixedPurposeValueEnv: 'DCC_E2E_TC007_FIXED_PURPOSE_VALUE',
    submitSelectorEnv: 'DCC_E2E_TC007_SUBMIT_SELECTOR',
    label: 'TC-E2E-007 upload purpose'
  })
  await page.locator(getEnv('DCC_E2E_TC007_ERROR_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  const requestId = resolveUploadRequestId(captures, startResponseCount, 'TC-E2E-007 upload')
  assertNewResponsesDoNotContainToken(captures, startResponseCount, 'uploadTicket', 'TC-E2E-007')
  await verifyFinalApi(page, 'DCC_E2E_TC007_AUDIT_VERIFY_URL', 'TC-E2E-007 audit', {
    expectedContainsEnv: 'DCC_E2E_TC007_AUDIT_EXPECT_JSON_CONTAINS',
    templateValues: { requestId }
  })
  await verifyFinalApi(page, 'DCC_E2E_TC007_TEMP_VERIFY_URL', 'TC-E2E-007 temp file', {
    expectedContainsEnv: 'DCC_E2E_TC007_TEMP_EXPECT_JSON_CONTAINS',
    templateValues: { requestId }
  })
  evidence.push('TC-E2E-007')
}

async function runUploadTicketSuccess(session, evidence) {
  const { page, captures } = session
  const startResponseCount = captures.responses.length
  await gotoAndWait(page, getEnv('DCC_E2E_TC008_UPLOAD_PATH'), null)
  await selectUploadCategory(page, {
    categorySelectorEnv: 'DCC_E2E_TC008_CATEGORY_SELECTOR',
    categoryOptionSelectorEnv: 'DCC_E2E_TC008_CATEGORY_OPTION_SELECTOR',
    label: 'TC-E2E-008 upload'
  })
  await fillUploadBusinessFields(page, 'DCC_E2E_TC008')
  await setFileRequired(page, getEnv('DCC_E2E_TC008_FILE_INPUT_SELECTOR'), getEnv('DCC_E2E_TC008_FILE_PATH'))
  await applyUploadPurpose(page, {
    purposeSelectorEnv: 'DCC_E2E_TC008_PURPOSE_SELECTOR',
    purposeValueEnv: 'DCC_E2E_TC008_PURPOSE_VALUE',
    fixedPurposeValueEnv: 'DCC_E2E_TC008_FIXED_PURPOSE_VALUE',
    label: 'TC-E2E-008 upload purpose'
  })
  await page.locator(getEnv('DCC_E2E_TC008_UPLOAD_SUCCESS_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  const uploadResponse = findLatestDccResponseByBodyToken(captures.responses, 'uploadTicket', startResponseCount)
  assert.ok(uploadResponse, 'Upload response must expose uploadTicket')
  assert.ok(readDccResponseBodyString(uploadResponse, 'requestId', 'TC-E2E-008 upload response'))
  const uploadTicket = readDccResponseBodyString(uploadResponse, 'uploadTicket', 'TC-E2E-008 upload response')
  const sessionId = readDccResponseBodyString(uploadResponse, 'sessionId', 'TC-E2E-008 upload response')
  const sourceFileName = readDccResponseBodyString(uploadResponse, 'fileName', 'TC-E2E-008 upload response')
  assertNoForbiddenDccResponseFields([uploadResponse], 'TC-E2E-008 upload response')
  const failureSuffix = String(Date.now())
  const failurePayload = {
    categoryId: Number(getEnv('DCC_E2E_TC008_CATEGORY_ID')),
    directoryId: Number(getEnv('DCC_E2E_TC008_DIRECTORY_ID')),
    fileName: `${getEnv('DCC_E2E_TC008_FILE_NAME_VALUE')}-${failureSuffix}`,
    fileNumber: `${getEnv('DCC_E2E_TC008_FILE_NUMBER_VALUE')}-${failureSuffix}`,
    productCode: getEnv('DCC_E2E_TC008_PRODUCT_CODE_VALUE'),
    needTraining: false,
    selectedSignoffUserIds: [],
    processType: 'CONTROLLED_FILE',
    versionNo: `${getEnv('DCC_E2E_TC008_VERSION_VALUE')}.${failureSuffix.slice(-6)}`,
    effectiveDate: getEnv('DCC_E2E_TC008_EFFECTIVE_DATE_VALUE'),
    remark: 'TC-E2E-008 failure boundary'
  }
  await postSubmitExpectFailure(
    page,
    {
      ...failurePayload,
      sessionId,
      originalFileId: 1,
      sourceFileId: 1
    },
    'DCC_E2E_TC008_FILE_ID_FAILURE_EXPECT_JSON_CONTAINS',
    'TC-E2E-008 fileId injection'
  )
  await postSubmitExpectFailure(
    page,
    {
      ...failurePayload,
      sessionId: `${sessionId}-cross-session`,
      originalUploadTicket: uploadTicket,
      sourceUploadTicket: uploadTicket,
      sourceFileName
    },
    'DCC_E2E_TC008_CROSS_SESSION_FAILURE_EXPECT_JSON_CONTAINS',
    'TC-E2E-008 cross-session ticket'
  )
  await postSubmitExpectFailure(
    page,
    {
      ...failurePayload,
      sessionId,
      originalUploadTicket: 'UT-EXPIRED-E2E-NOT-BINDABLE',
      sourceUploadTicket: 'UT-EXPIRED-E2E-NOT-BINDABLE',
      sourceFileName
    },
    'DCC_E2E_TC008_EXPIRED_TICKET_FAILURE_EXPECT_JSON_CONTAINS',
    'TC-E2E-008 expired or invalid ticket'
  )
  await clickRequired(page, getEnv('DCC_E2E_TC008_SUBMIT_SELECTOR'))
  await page.locator(getEnv('DCC_E2E_TC008_SUBMIT_SUCCESS_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  evidence.push('TC-E2E-008')
}

async function runTempFileLifecycle(session, evidence) {
  const { page, captures } = session
  const startResponseCount = captures.responses.length
  await gotoAndWait(page, getEnv('DCC_E2E_TC009_FLOW_PATH'), null)
  await selectUploadCategory(page, {
    categorySelectorEnv: 'DCC_E2E_TC009_CATEGORY_SELECTOR',
    categoryOptionSelectorEnv: 'DCC_E2E_TC009_CATEGORY_OPTION_SELECTOR',
    label: 'TC-E2E-009 upload'
  })
  await page.locator(getEnv('DCC_E2E_TC009_FILE_INPUT_SELECTOR')).first().waitFor({
    state: 'attached',
    timeout: DEFAULT_TIMEOUT_MS
  })
  await setFileRequired(page, getEnv('DCC_E2E_TC009_FILE_INPUT_SELECTOR'), getEnv('DCC_E2E_TC009_FILE_PATH'))
  await page.locator(getEnv('DCC_E2E_TC009_UPLOAD_SUCCESS_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  const requestId = resolveUploadRequestId(captures, startResponseCount, 'TC-E2E-009 upload')
  const removeLocator = page.locator(getEnv('DCC_E2E_TC009_REMOVE_SELECTOR')).first()
  await removeLocator.waitFor({
    state: 'attached',
    timeout: DEFAULT_TIMEOUT_MS
  })
  const cleanupResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/controlled-files/upload-temporary/session-cleanup') &&
      response.request().method() === 'POST',
    { timeout: DEFAULT_TIMEOUT_MS }
  )
  await removeLocator.locator('xpath=ancestor::*[contains(@class, "el-upload-list__item")]').first().hover()
  await removeLocator.waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  await removeLocator.click()
  const cleanupResponse = await cleanupResponsePromise
  assert.ok(cleanupResponse.ok(), `TC-E2E-009 cleanup request failed with HTTP ${cleanupResponse.status()}`)
  await verifyFinalApi(page, 'DCC_E2E_TC009_TEMP_VERIFY_URL', 'TC-E2E-009 temp file', {
    expectedContainsEnv: 'DCC_E2E_TC009_TEMP_EXPECT_JSON_CONTAINS',
    templateValues: { requestId }
  })
  await verifyFinalApi(page, 'DCC_E2E_TC009_AUDIT_VERIFY_URL', 'TC-E2E-009', {
    expectedContainsEnv: 'DCC_E2E_TC009_AUDIT_EXPECT_JSON_CONTAINS',
    templateValues: { requestId }
  })
  evidence.push('TC-E2E-009')
}

async function runEncryptedDownloadSuccess(session, evidence) {
  const { page } = session
  await gotoAndWait(page, getEnv('DCC_E2E_TC010_DOWNLOAD_PATH'), null)
  const downloadResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/admin-api/dcc') && response.url().includes('/download'), {
      timeout: DOWNLOAD_TIMEOUT_MS
    })
  await clickRequired(page, getEnv('DCC_E2E_TC010_DOWNLOAD_TRIGGER_SELECTOR'))
  await clickRequired(page, getEnv('DCC_E2E_TC010_DOWNLOAD_CONFIRM_SELECTOR'))
  const downloadResponse = await downloadResponsePromise
  assert.equal(downloadResponse.status(), 200, 'Encrypted download response must be HTTP 200')
  const responseBody = await downloadResponse.body()
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const downloadPath = path.join(OUTPUT_DIR, 'TC-E2E-010-encrypted-download.bin')
  fs.writeFileSync(downloadPath, responseBody)
  const cipherSha256 = sha256Buffer(responseBody)
  const response = { headers: downloadResponse.headers() }
  for (const header of [
    'x-dcc-access-event-code',
    'x-dcc-download-request-id',
    'x-dcc-encryption-policy-version',
    'x-dcc-artifact-id',
    'x-dcc-plain-sha256',
    'x-dcc-cipher-sha256'
  ]) {
    assert.ok(response.headers[header], `Encrypted download response must include ${header}`)
  }
  assert.equal(
    cipherSha256,
    response.headers['x-dcc-cipher-sha256'],
    'Downloaded file hash must match encrypted artifact evidence'
  )
  assert.notEqual(
    cipherSha256,
    response.headers['x-dcc-plain-sha256'],
    'Downloaded file must not match plaintext evidence'
  )
  assert.equal(
    response.headers['x-dcc-encryption-policy-version'],
    getEnv('DCC_E2E_TC010_EXPECT_ENCRYPTION_POLICY_VERSION'),
    'Encrypted download policy version must match expected real contract version'
  )
  await verifyFinalApi(page, 'DCC_E2E_TC010_AUDIT_VERIFY_URL', 'TC-E2E-010', {
    expectedContainsEnv: 'DCC_E2E_TC010_AUDIT_EXPECT_JSON_CONTAINS',
    expectedFieldsEnv: 'DCC_E2E_TC010_AUDIT_EXPECT_FIELDS'
  })
  evidence.push('TC-E2E-010')
}

async function runEncryptionFailClosed(session, evidence) {
  const { page, context } = session
  await gotoAndWait(page, getEnv('DCC_E2E_TC011_DOWNLOAD_PATH'), null)
  const downloadPromise = page.waitForEvent('download', { timeout: 3000 }).then(() => 'downloaded').catch(() => 'no-download')
  await clickRequired(page, getEnv('DCC_E2E_TC011_DOWNLOAD_TRIGGER_SELECTOR'))
  await clickRequired(page, getEnv('DCC_E2E_TC011_DOWNLOAD_CONFIRM_SELECTOR'))
  await page.locator(getEnv('DCC_E2E_TC011_ERROR_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  assert.equal(await downloadPromise, 'no-download', 'Encryption failure must not create a browser download')
  await verifyFinalApi(page, 'DCC_E2E_TC011_AUDIT_VERIFY_URL', 'TC-E2E-011', {
    expectedContainsEnv: 'DCC_E2E_TC011_AUDIT_EXPECT_JSON_CONTAINS',
    expectedFieldsEnv: 'DCC_E2E_TC011_AUDIT_EXPECT_FIELDS'
  })
  evidence.push('TC-E2E-011')
}

async function runDownloadPolicyNotPrefix(session, evidence) {
  const policy = await newLoggedInPage(session.browser, 'policy')
  try {
    const { page } = policy
    await gotoAndWait(page, getEnv('DCC_E2E_TC012_PREFIX_DENIED_PATH'), null)
    await assertNoVisible(
      page,
      getEnv('DCC_E2E_TC012_PREFIX_DENIED_ABSENT_SELECTOR'),
      'TC-E2E-012 denied file must not expose a download control'
    )
    await verifyFinalApi(page, 'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_VERIFY_URL', 'TC-E2E-012 denied detail', {
      expectedFieldsEnv: 'DCC_E2E_TC012_PREFIX_DENIED_DETAIL_EXPECT_FIELDS'
    })
    await gotoAndWait(page, getEnv('DCC_E2E_TC012_NO_PREFIX_ALLOWED_PATH'), null)
    const downloadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/admin-api/dcc') && response.url().includes('/download'), {
        timeout: DOWNLOAD_TIMEOUT_MS
      })
    await clickRequired(page, getEnv('DCC_E2E_TC012_NO_PREFIX_ALLOWED_TRIGGER_SELECTOR'))
    await clickRequired(page, getEnv('DCC_E2E_TC012_NO_PREFIX_ALLOWED_CONFIRM_SELECTOR'))
    const downloadResponse = await downloadResponsePromise
    assert.equal(downloadResponse.status(), 200, 'TC-E2E-012 allowed download response must be HTTP 200')
    const responseHeaders = downloadResponse.headers()
    assert.ok(responseHeaders['x-dcc-download-request-id'], 'TC-E2E-012 allowed download response must include request id')
    await verifyFinalApi(page, 'DCC_E2E_TC012_AUDIT_VERIFY_URL', 'TC-E2E-012', {
      expectedContainsEnv: 'DCC_E2E_TC012_AUDIT_EXPECT_JSON_CONTAINS',
      expectedFieldsEnv: 'DCC_E2E_TC012_AUDIT_EXPECT_FIELDS'
    })
  } finally {
    await policy.context.close()
  }
  evidence.push('TC-E2E-012')
}

async function runAuditQueryAuthorization(session, evidence) {
  const auditor = await newLoggedInPage(session.browser, 'auditor')
  try {
    await gotoAndWait(auditor.page, getEnv('DCC_E2E_TC013_AUDIT_PATH'), null)
    for (const [selectorEnv, valueEnv] of [
      ['DCC_E2E_TC013_TRACE_INPUT_SELECTOR', 'DCC_E2E_TC013_TRACE_VALUE'],
      ['DCC_E2E_TC013_EVENT_INPUT_SELECTOR', 'DCC_E2E_TC013_EVENT_VALUE'],
      ['DCC_E2E_TC013_FILE_INPUT_SELECTOR', 'DCC_E2E_TC013_FILE_VALUE'],
      ['DCC_E2E_TC013_USER_INPUT_SELECTOR', 'DCC_E2E_TC013_USER_VALUE'],
      ['DCC_E2E_TC013_TIME_START_SELECTOR', 'DCC_E2E_TC013_TIME_START_VALUE'],
      ['DCC_E2E_TC013_TIME_END_SELECTOR', 'DCC_E2E_TC013_TIME_END_VALUE']
    ]) {
      await fillRequired(auditor.page, getEnv(selectorEnv), getEnv(valueEnv))
    }
    await selectRequired(
      auditor.page,
      getEnv('DCC_E2E_TC013_ACTION_SELECTOR'),
      getEnv('DCC_E2E_TC013_ACTION_OPTION_SELECTOR'),
      'TC-E2E-013 action'
    )
    await selectRequired(
      auditor.page,
      getEnv('DCC_E2E_TC013_RESULT_SELECTOR_FIELD'),
      getEnv('DCC_E2E_TC013_RESULT_OPTION_SELECTOR'),
      'TC-E2E-013 result'
    )
    await clickRequired(auditor.page, getEnv('DCC_E2E_TC013_SEARCH_SELECTOR'))
    await auditor.page.locator(getEnv('DCC_E2E_TC013_RESULT_SELECTOR')).first().waitFor({
      state: 'visible',
      timeout: DEFAULT_TIMEOUT_MS
    })
    await screenshot(auditor.page, 'TC-E2E-013-auditor', null)
  } finally {
    await auditor.context.close()
  }

  const ordinary = await newLoggedInPage(session.browser, 'ordinary')
  try {
    await gotoAndWait(ordinary.page, getEnv('DCC_E2E_TC013_AUDIT_PATH'), null)
    await ordinary.page.locator(getEnv('DCC_E2E_TC013_UNAUTHORIZED_SELECTOR')).first().waitFor({
      state: 'visible',
      timeout: DEFAULT_TIMEOUT_MS
    })
  } finally {
    await ordinary.context.close()
  }
  evidence.push('TC-E2E-013')
}

async function runFrontendFailClosed(session, evidence) {
  const { page, captures } = session
  const startInfraCount = captures.infraFileRequests.length
  await runFailureInteraction(
    page,
    {
      pathEnv: 'DCC_E2E_TC014_PREVIEW_FAILURE_PATH',
      triggerSelectorEnv: 'DCC_E2E_TC014_PREVIEW_TRIGGER_SELECTOR',
      errorSelectorEnv: 'DCC_E2E_TC014_PREVIEW_ERROR_SELECTOR'
    },
    'TC-E2E-014 preview failure'
  )
  await verifyFinalApi(page, 'DCC_E2E_TC014_PREVIEW_AUDIT_VERIFY_URL', 'TC-E2E-014 preview failure audit', {
    expectedContainsEnv: 'DCC_E2E_TC014_PREVIEW_AUDIT_EXPECT_JSON_CONTAINS',
    expectedFieldsEnv: 'DCC_E2E_TC014_PREVIEW_AUDIT_EXPECT_FIELDS'
  })

  const uploadStartResponseCount = captures.responses.length
  await runUploadFlow(page, {
    pathEnv: 'DCC_E2E_TC014_UPLOAD_PATH',
    categorySelectorEnv: 'DCC_E2E_TC014_UPLOAD_CATEGORY_SELECTOR',
    categoryOptionSelectorEnv: 'DCC_E2E_TC014_UPLOAD_CATEGORY_OPTION_SELECTOR',
    fileInputEnv: 'DCC_E2E_TC014_UPLOAD_FILE_INPUT_SELECTOR',
    filePathEnv: 'DCC_E2E_TC014_UPLOAD_OVERSIZE_FILE_PATH',
    fixedPurposeValueEnv: 'DCC_E2E_TC014_UPLOAD_FIXED_PURPOSE_VALUE',
    submitSelectorEnv: 'DCC_E2E_TC014_UPLOAD_SUBMIT_SELECTOR',
    label: 'TC-E2E-014 upload fail closed'
  })
  await page.locator(getEnv('DCC_E2E_TC014_UPLOAD_ERROR_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  const requestId = resolveUploadRequestId(captures, uploadStartResponseCount, 'TC-E2E-014 upload fail closed')
  assertNewResponsesDoNotContainToken(captures, uploadStartResponseCount, 'uploadTicket', 'TC-E2E-014 upload fail closed')
  await verifyFinalApi(page, 'DCC_E2E_TC014_UPLOAD_AUDIT_VERIFY_URL', 'TC-E2E-014 upload fail closed audit', {
    expectedContainsEnv: 'DCC_E2E_TC014_UPLOAD_AUDIT_EXPECT_JSON_CONTAINS',
    templateValues: { requestId }
  })

  await gotoAndWait(page, getEnv('DCC_E2E_TC014_DOWNLOAD_FAILURE_PATH'), null)
  const downloadPromise = page.waitForEvent('download', { timeout: 3000 }).then(() => 'downloaded').catch(() => 'no-download')
  await clickRequired(page, getEnv('DCC_E2E_TC014_DOWNLOAD_TRIGGER_SELECTOR'))
  await clickRequired(page, getEnv('DCC_E2E_TC014_DOWNLOAD_CONFIRM_SELECTOR'))
  await page.locator(getEnv('DCC_E2E_TC014_DOWNLOAD_ERROR_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  assert.equal(await downloadPromise, 'no-download', 'TC-E2E-014 download fail closed must not create a file')
  await verifyFinalApi(page, 'DCC_E2E_TC014_DOWNLOAD_AUDIT_VERIFY_URL', 'TC-E2E-014 download fail closed audit', {
    expectedContainsEnv: 'DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_JSON_CONTAINS',
    expectedFieldsEnv: 'DCC_E2E_TC014_DOWNLOAD_AUDIT_EXPECT_FIELDS'
  })
  assert.equal(
    captures.infraFileRequests.length,
    startInfraCount,
    'Frontend fail-closed paths must not call old infra file endpoints'
  )
  evidence.push('TC-E2E-014')
}

async function runStyleScreenshots(session, evidence) {
  const { page } = session
  const paths = splitEnvList('DCC_E2E_TC015_SCREENSHOT_PATHS')
  const selectors = splitEnvList('DCC_E2E_TC015_READY_SELECTORS')
  const requiredTextGroups = splitRequiredTextGroups('DCC_E2E_TC015_REQUIRED_VISIBLE_TEXTS')
  assert.equal(paths.length, selectors.length, 'TC015 path and selector counts must match')
  assert.equal(paths.length, requiredTextGroups.length, 'TC015 path and required text group counts must match')
  for (let index = 0; index < paths.length; index += 1) {
    await gotoAndWait(page, paths[index], selectors[index])
    await assertDccOperationalStyle(page, `TC-E2E-015 page ${index + 1}`, requiredTextGroups[index])
    await screenshot(page, `TC-E2E-015-${index + 1}`, null)
  }
  evidence.push('TC-E2E-015')
}

async function runNonDccRegression(session, evidence) {
  const { page } = session
  await gotoAndWait(page, getEnv('DCC_E2E_TC016_NON_DCC_PATH'), getEnv('DCC_E2E_TC016_NON_DCC_SUCCESS_SELECTOR'))
  await gotoAndWait(page, getEnv('DCC_E2E_TC016_DCC_BYPASS_PATH'), null)
  await page.locator(getEnv('DCC_E2E_TC016_DCC_BYPASS_ERROR_SELECTOR')).first().waitFor({
    state: 'visible',
    timeout: DEFAULT_TIMEOUT_MS
  })
  evidence.push('TC-E2E-016')
}

async function runSystemFullFlowRollup(session, evidence) {
  const expected = cases.filter((testCase) => testCase.id !== 'TC-E2E-017').map((testCase) => testCase.id)
  assert.deepEqual(evidence, expected, 'TC-E2E-017 requires all previous real user path cases to pass in order')
  evidence.push('TC-E2E-017')
}

async function main() {
  const activeCases = selectedCases()
  const blockers = validatePrerequisites(activeCases)
  if (blockers.length > 0) {
    printBlocked(blockers, activeCases)
    process.exitCode = BLOCKED_EXIT_CODE
    return
  }

  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: getEnv('DCC_E2E_HEADLESS') !== 'false' })
  const evidence = []
  const primary = await newLoggedInPage(browser, 'default')
  const captures = watchDccResponses(primary.page)
  const session = {
    browser,
    context: primary.context,
    page: primary.page,
    captures
  }

  try {
    for (const testCase of activeCases) {
      await testCase.run(session, evidence)
      console.log(`PASS: ${testCase.id} ${testCase.title}`)
    }
    fs.writeFileSync(
      path.join(OUTPUT_DIR, 'summary.json'),
      JSON.stringify({ passedCases: evidence, generatedAt: new Date().toISOString() }, null, 2),
      'utf8'
    )
  } finally {
    await primary.context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(`FAIL: ${error.message}`)
  if (error.stack) {
    console.error(error.stack)
  }
  process.exitCode = 1
})
