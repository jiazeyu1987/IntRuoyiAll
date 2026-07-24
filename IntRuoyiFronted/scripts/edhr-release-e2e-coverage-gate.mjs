#!/usr/bin/env node
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

export const RELEASE_CHECK_SCRIPT = 'e2e:edhr:release:check'
export const RELEASE_REAL_SCRIPT = 'e2e:edhr:release'
export const RELEASE_CHECK_COMMAND = 'node scripts/edhr-release-e2e-coverage-gate.mjs --check'
export const RELEASE_REAL_COMMAND = 'node scripts/edhr-release-e2e-coverage-gate.mjs --run-real'
export const REPORT_SCHEMA_VERSION = 1

export const REQUIRED_FEATURE_IDS = Object.freeze([
  'feedback-entry/open-or-create',
  'execution-detail/save-submit',
  'approval-workbench/detail-approve-reject',
  'archive-generate-download',
  'final-archive-work-task',
  'record-change/void-reopen-supplement',
  'batch-execution',
  'execution-attachment/upload-save',
  'tracking-signature',
  'field-audit',
  'domain-trace',
  'permission-matrix',
  'archive-health/runtime-control',
  'batch-version/phase1-approval'
])

export const RELEASE_E2E_COVERAGE_MATRIX = Object.freeze([
  {
    featureId: 'feedback-entry/open-or-create',
    featureName: 'eDHR feedback entry open-or-create',
    routes: ['/mes/pro/feedback', '/mes/pro/feedback/edhr-execution/form'],
    sourceFiles: [
      'src/views/mes/pro/feedback/FeedbackForm.vue',
      'src/api/mes/pro/feedback/index.ts'
    ],
    apiTokens: [
      '/mes/pro/batch-record-execution/entry-context',
      '/mes/pro/batch-record-execution/open-or-create-by-context'
    ],
    e2eTokens: [
      '/mes/pro/batch-record-execution/open-or-create-by-context',
      'open-or-create 返回 created',
      'fresh DRAFT 创建'
    ],
    e2eFile: 'tests/e2e/edhr-approval-tracking-real-flow.e2e.js',
    packageScript: 'e2e:edhr:approval-tracking',
    checkScript: 'e2e:edhr:approval-tracking:check',
    taskEvidence: 'doc/tasks/20260528-edhr-archive-approval-evidence/real-e2e-evidence.md'
  },
  {
    featureId: 'execution-detail/save-submit',
    featureName: 'eDHR execution form save and submit',
    routes: ['/mes/pro/feedback/edhr-execution/form'],
    sourceFiles: [
      'src/views/mes/pro/edhr/ExecutionPage.vue',
      'src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue',
      'src/views/mes/pro/edhr/ExecutionRenderer.vue',
      'src/api/mes/pro/feedback/index.ts',
      'src/api/mes/pro/edhr/fieldAudit.ts'
    ],
    apiTokens: [
      '/mes/pro/batch-record-execution/get?id=',
      '/mes/pro/batch-record-execution/submit',
      '/mes/pro/batch-record-execution/field-audit/save-changes'
    ],
    e2eTokens: [
      '/mes/pro/batch-record-execution/submit',
      '/mes/pro/batch-record-execution/field-audit/save-changes',
      '保存变更',
      '提交后进入审批'
    ],
    e2eFile: 'tests/e2e/edhr-approval-tracking-real-flow.e2e.js',
    packageScript: 'e2e:edhr:approval-tracking',
    checkScript: 'e2e:edhr:approval-tracking:check',
    taskEvidence: 'doc/tasks/20260528-edhr-archive-approval-evidence/real-e2e-evidence.md'
  },
  {
    featureId: 'approval-workbench/detail-approve-reject',
    featureName: 'eDHR approval workbench detail approve and reject',
    routes: [
      '/mes/pro/feedback/edhr-approval',
      '/mes/pro/feedback/edhr-approval/detail'
    ],
    sourceFiles: [
      'src/router/modules/remaining.ts',
      'src/views/mes/pro/edhr/ApprovalPage.vue',
      'src/views/mes/pro/edhr/ApprovalDetailPage.vue',
      'src/api/mes/pro/edhr/approval.ts'
    ],
    apiTokens: [
      '/mes/pro/batch-record-execution/approval-pending-page',
      '/mes/pro/batch-record-execution/approval-done-page',
      '/mes/pro/batch-record-execution/approval-detail',
      '/mes/pro/batch-record-execution/approve',
      '/mes/pro/batch-record-execution/reject'
    ],
    e2eTokens: [
      '/mes/pro/feedback/edhr-approval/detail',
      '/mes/pro/batch-record-execution/approval-detail',
      '/mes/pro/batch-record-execution/approval-done-page',
      '/mes/pro/batch-record-execution/approve',
      '/mes/pro/batch-record-execution/reject',
      'tab=done',
      '我已审批',
      '审批详情真实 API 展示'
    ],
    e2eFile: 'tests/e2e/edhr-approval-tracking-real-flow.e2e.js',
    packageScript: 'e2e:edhr:approval-tracking',
    checkScript: 'e2e:edhr:approval-tracking:check',
    taskEvidence: 'doc/tasks/20260528-edhr-archive-approval-evidence/real-e2e-evidence.md'
  },
  {
    featureId: 'archive-generate-download',
    featureName: 'eDHR archive generate and download',
    routes: ['/mes/pro/feedback/edhr-execution/form'],
    sourceFiles: [
      'src/views/mes/pro/edhr/ExecutionPage.vue',
      'src/views/mes/pro/edhr/ApprovalDetailPage.vue',
      'src/api/mes/pro/edhr/archive.ts'
    ],
    apiTokens: [
      '/mes/pro/batch-record-execution-archive/generate',
      '/mes/pro/batch-record-execution-archive/page',
      '/mes/pro/batch-record-execution-archive/latest',
      '/mes/pro/batch-record-execution-archive/download'
    ],
    e2eTokens: [
      '/mes/pro/batch-record-execution-archive/generate',
      '/mes/pro/batch-record-execution-archive/page',
      '/mes/pro/batch-record-execution-archive/download',
      '查看版本',
      '归档版本',
      '关闭后可归档',
      '受控归档下载'
    ],
    e2eFile: 'tests/e2e/edhr-approval-tracking-real-flow.e2e.js',
    packageScript: 'e2e:edhr:approval-tracking',
    checkScript: 'e2e:edhr:approval-tracking:check',
    taskEvidence: 'doc/tasks/20260528-edhr-archive-approval-evidence/real-e2e-evidence.md'
  },
  {
    featureId: 'final-archive-work-task',
    featureName: 'eDHR final archive work task',
    routes: [
      '/mes/pro/feedback/edhr-work-task',
      '/mes/pro/feedback/edhr-batch-execution/detail'
    ],
    sourceFiles: [
      'src/api/mes/pro/edhr/workTask.ts',
      'src/api/mes/pro/edhr/batchExecution.ts',
      'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue',
      'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
      'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
    ],
    apiTokens: [
      '/mes/pro/edhr-work-task/my-page',
      '/mes/pro/edhr-work-task/done-page',
      '/mes/pro/edhr-work-task/stats',
      'EDHR_WORK_TASK_TYPE_ARCHIVE',
      'archiveCount',
      'workTaskId: number',
      "BATCH_ARCHIVE_BASE_URL = '/mes/pro/edhr-batch-execution-archive'",
      '/generate',
      '/latest',
      '/download'
    ],
    e2eTokens: [
      '/mes/pro/feedback/edhr-work-task',
      '/mes/pro/feedback/edhr-batch-execution/detail',
      'EDHR_ARCHIVE_TASK_E2E_WORK_TASK_ID',
      'ARCHIVE/TODO',
      'ARCHIVE_GENERATE_ENDPOINT',
      'ARCHIVE_DOWNLOAD_ENDPOINT',
      'workTaskId',
      'SEALED',
      'DONE',
      'final-archive-task',
      'BLOCKED'
    ],
    e2eFile: 'tests/e2e/edhr-final-archive-work-task-real-flow.e2e.js',
    packageScript: 'e2e:edhr:final-archive-task',
    checkScript: 'e2e:edhr:final-archive-task:check',
    taskEvidence: '../doc/tasks/20260612-edhr-final-archive-todo-assessment/real-e2e-evidence.md'
  },
  {
    featureId: 'record-change/void-reopen-supplement',
    featureName: 'eDHR record change void reopen supplement',
    routes: ['/mes/pro/feedback/edhr-change'],
    sourceFiles: [
      'src/api/mes/pro/edhr/change.ts',
      'src/views/mes/pro/edhr/RecordChangePage.vue',
      'src/views/mes/pro/edhr/ExecutionPage.vue',
      'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
      'src/router/modules/remaining.ts'
    ],
    apiTokens: [
      '/mes/pro/edhr-change/void-execution/request',
      '/mes/pro/edhr-change/void-execution/approve',
      '/mes/pro/edhr-change/reopen-batch/request',
      '/mes/pro/edhr-change/reopen-batch/approve',
      '/mes/pro/edhr-change/supplement/request',
      '/mes/pro/edhr-change/supplement/approve',
      '/mes/pro/edhr-change/page',
      '/mes/pro/edhr-change/get',
      'previousArchiveHash',
      'reasonText'
    ],
    e2eTokens: [
      '/mes/pro/feedback/edhr-change',
      '/admin-api/mes/pro/edhr-change/page',
      'EDHR_CHANGE_E2E_PASSWORD',
      'writeRequests.length > 0',
      'BDD:',
      'PASS',
      'FAIL',
      'BLOCKED'
    ],
    e2eFile: 'tests/e2e/edhr-change-record-real-flow.e2e.js',
    packageScript: 'e2e:edhr:record-change',
    checkScript: 'e2e:edhr:record-change:check',
    taskEvidence: '../doc/tasks/20260612-edhr-final-archive-todo-assessment/record-change-release-e2e-evidence.md'
  },
  {
    featureId: 'batch-execution',
    featureName: 'eDHR batch execution replacement entry',
    routes: [
      '/mes/pro/feedback/edhr-batch-execution',
      '/mes/pro/feedback/edhr-batch-execution/detail'
    ],
    sourceFiles: [
      'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
      'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
      'src/api/mes/pro/edhr/batchExecution.ts'
    ],
    apiTokens: [
      '/mes/pro/edhr-batch-execution/page',
      '/mes/pro/edhr-batch-execution/open-or-create',
      '/mes/pro/edhr-batch-execution/task/open'
    ],
    e2eTokens: [
      '/mes/pro/feedback/edhr-batch-execution',
      '/mes/pro/edhr-batch-execution/open-or-create',
      '/mes/pro/edhr-batch-execution/task/open',
      '批次执行编码'
    ],
    e2eFile: 'tests/e2e/edhr-batch-execution-real-flow.e2e.js',
    packageScript: 'e2e:edhr:batch-execution',
    checkScript: 'e2e:edhr:batch-execution:check',
    taskEvidence: 'doc/tasks/20260608-edhr-batch-execution-full-flow/real-e2e-evidence.md'
  },
  {
    featureId: 'execution-attachment/upload-save',
    featureName: 'eDHR execution attachment upload and signed save',
    routes: ['/mes/pro/feedback/edhr-execution/form'],
    sourceFiles: [
      'src/views/mes/pro/edhr/ExecutionPage.vue',
      'src/components/UploadFile/src/UploadFile.vue',
      'src/components/UploadFile/src/UploadImg.vue',
      'src/components/UploadFile/src/UploadImgs.vue',
      'src/api/mes/pro/edhr/attachment.ts',
      'src/api/mes/pro/edhr/fieldAudit.ts',
      'src/api/mes/pro/feedback/index.ts'
    ],
    apiTokens: [
      '/mes/pro/batch-record-execution/attachment/prepare-upload',
      'prepareEdhrAttachmentUpload',
      'attachmentChanges',
      'storageRetentionHash',
      'attachmentSummaries'
    ],
    e2eTokens: [
      '/mes/pro/batch-record-execution/attachment/prepare-upload',
      '/mes/pro/batch-record-execution/field-audit/save-changes',
      'EDHR_ATTACHMENT_E2E_EXECUTION_ID',
      'EDHR_ATTACHMENT_E2E_WORK_TASK_ID',
      'mes_pro_batch_record_execution_attachment',
      'verifyAttachmentLedger',
      '当前附件证据',
      'attachmentChanges',
      'BLOCKED'
    ],
    e2eFile: 'tests/e2e/edhr-attachment-upload-real-flow.e2e.js',
    packageScript: 'e2e:edhr:attachment-upload',
    checkScript: 'e2e:edhr:attachment-upload:check',
    taskEvidence: 'doc/tasks/20260612-edhr-attachment-prepare-upload-api/real-upload-e2e-evidence.md'
  },
  {
    featureId: 'tracking-signature',
    featureName: 'eDHR tracking and signature pages',
    routes: [
      '/mes/pro/feedback/edhr-tracking',
      '/mes/pro/feedback/edhr-signatures',
      '/mes/pro/feedback/edhr-execution/form'
    ],
    sourceFiles: [
      'src/views/mes/pro/edhr/TrackingPage.vue',
      'src/views/mes/pro/edhr/SignaturePage.vue',
      'src/api/mes/pro/edhr/tracking.ts',
      'src/api/mes/pro/edhr/signatures.ts'
    ],
    apiTokens: [
      '/mes/pro/batch-record-execution/tracking-page',
      '/mes/pro/batch-record-execution/tracking-timeline',
      '/mes/pro/batch-record-execution/signature-page'
    ],
    e2eTokens: [
      '/mes/pro/batch-record-execution/tracking-page',
      '/mes/pro/batch-record-execution/tracking-timeline',
      '/mes/pro/batch-record-execution/signature-page',
      'signature-action-filter'
    ],
    e2eFile: 'tests/e2e/edhr-tracking-signature-real-flow.e2e.js',
    packageScript: 'e2e:edhr:tracking-signature',
    checkScript: 'e2e:edhr:tracking-signature:check',
    taskEvidence: 'doc/tasks/20260529-edhr-tracking-signature-real-e2e-gate/real-e2e-evidence.md'
  },
  {
    featureId: 'field-audit',
    featureName: 'eDHR field audit',
    routes: [
      '/mes/pro/feedback/edhr-field-audit',
      '/mes/pro/feedback/edhr-field-audit/detail'
    ],
    sourceFiles: [
      'src/views/mes/pro/edhr/FieldAuditPage.vue',
      'src/views/mes/pro/edhr/FieldAuditDetailPage.vue',
      'src/views/mes/pro/edhr/ExecutionPage.vue',
      'src/api/mes/pro/edhr/fieldAudit.ts'
    ],
    apiTokens: [
      '/mes/pro/batch-record-execution/field-audit/page',
      '/mes/pro/batch-record-execution/field-audit/detail',
      '/mes/pro/batch-record-execution/field-audit/verify-chain',
      '/mes/pro/batch-record-execution/field-audit/export'
    ],
    e2eTokens: [
      '/mes/pro/batch-record-execution/field-audit/page',
      '/mes/pro/batch-record-execution/field-audit/detail',
      '/mes/pro/batch-record-execution/field-audit/verify-chain',
      '/mes/pro/batch-record-execution/field-audit/export',
      '/mes/pro/feedback/edhr-execution/form',
      '定位执行记录',
      '字段审计链可导出'
    ],
    e2eFile: 'tests/e2e/edhr-field-audit-real-flow.e2e.js',
    packageScript: 'e2e:edhr:field-audit',
    checkScript: 'e2e:edhr:field-audit:check',
    taskEvidence: 'doc/tasks/20260528-edhr-field-audit-real-e2e-gate/real-e2e-evidence.md'
  },
  {
    featureId: 'domain-trace',
    featureName: 'eDHR domain trace',
    routes: [
      '/mes/pro/feedback/edhr-domain-trace',
      '/mes/pro/feedback/edhr-domain-trace/detail'
    ],
    sourceFiles: [
      'src/views/mes/pro/edhr/DomainTracePage.vue',
      'src/views/mes/pro/edhr/DomainTraceDetailPage.vue',
      'src/api/mes/pro/edhr/domainTrace.ts'
    ],
    apiTokens: [
      'EDHR_DOMAIN_TRACE_BASE_URL',
      'getEdhrDomainTracePage',
      'getEdhrDomainTraceDetail',
      'verifyEdhrDomainTrace'
    ],
    e2eTokens: [
      '/mes/pro/batch-record-execution/domain-trace/page',
      '/mes/pro/batch-record-execution/domain-trace/detail',
      '/mes/pro/batch-record-execution/domain-trace/verify',
      '/mes/pro/feedback/edhr-execution/form',
      '执行详情',
      'EXPECTED_DOMAIN_TRACE_STATUSES'
    ],
    e2eFile: 'tests/e2e/edhr-domain-trace-real-flow.e2e.js',
    packageScript: 'e2e:edhr:domain-trace',
    checkScript: 'e2e:edhr:domain-trace:check',
    taskEvidence: 'doc/tasks/20260528-edhr-domain-trace-verified-e2e/real-e2e-evidence.md'
  },
  {
    featureId: 'permission-matrix',
    featureName: 'eDHR role and tenant permission matrix',
    routes: [
      '/mes/pro/feedback/edhr-execution/form',
      '/mes/pro/feedback/edhr-approval',
      '/mes/pro/feedback/edhr-tracking',
      '/mes/pro/feedback/edhr-signatures',
      '/mes/pro/feedback/edhr-field-audit',
      '/mes/pro/feedback/edhr-domain-trace/detail'
    ],
    sourceFiles: [
      'src/views/mes/pro/edhr/ExecutionPage.vue',
      'src/views/mes/pro/edhr/ApprovalPage.vue',
      'src/views/mes/pro/edhr/TrackingPage.vue',
      'src/views/mes/pro/edhr/SignaturePage.vue',
      'src/views/mes/pro/edhr/FieldAuditPage.vue',
      'src/views/mes/pro/edhr/DomainTraceDetailPage.vue',
      'src/api/mes/pro/edhr/approval.ts',
      'src/api/mes/pro/edhr/domainTrace.ts'
    ],
    apiTokens: [
      'mes:pro-batch-record-execution:approve',
      'mes:pro-batch-record-execution:field-audit-query',
      'mes:pro-batch-record-execution:domain-trace-query'
    ],
    e2eTokens: [
      'EDHR_WRITE_ENDPOINT_PATTERN',
      'POST|PUT|PATCH|DELETE',
      'no-permission users fail visibly',
      "'clean'"
    ],
    e2eFile: 'tests/e2e/edhr-permission-tenant-matrix.e2e.js',
    packageScript: 'e2e:edhr:permission-matrix',
    checkScript: 'e2e:edhr:permission-matrix:check',
    taskEvidence: 'doc/tasks/20260528-edhr-role-tenant-e2e-gate/real-e2e-evidence.md'
  },
  {
    featureId: 'archive-health/runtime-control',
    featureName: 'eDHR archive health runtime-control',
    routes: ['/infra/monitors/runtime-control'],
    sourceFiles: [
      'src/views/infra/runtime-control/index.vue',
      'src/views/infra/runtime-control/components/OpsProbeStatusPanel.vue',
      'src/views/infra/runtime-control/components/OpsLogDiskRiskPanel.vue',
      'src/api/infra/runtimeControl/index.ts'
    ],
    apiTokens: [
      '/infra/runtime-control/business-health',
      'getRuntimeControlBusinessHealth'
    ],
    e2eTokens: [
      '/admin-api/infra/runtime-control/business-health',
      'edhr-archive-integrity',
      'BUSINESS_HEALTH_API'
    ],
    e2eFile: 'tests/e2e/runtime-control-edhr-archive-health.e2e.js',
    packageScript: 'e2e:edhr:archive-health',
    checkScript: 'e2e:edhr:archive-health:check',
    taskEvidence: 'doc/tasks/20260528-edhr-archive-health-e2e-script-gate/execution-log.md'
  },
  {
    featureId: 'batch-version/phase1-approval',
    featureName: 'eDHR batch record version phase 1 approval',
    routes: ['/mes/pro/batch-record-template'],
    sourceFiles: [
      'src/views/mes/pro/batchrecordtemplate/index.vue',
      'src/api/mes/pro/batchrecordreport/index.ts',
      'src/router/modules/remaining.ts'
    ],
    apiTokens: [
      '/mes/pro/batch-record-report/recognize-uploaded',
      '/mes/pro/batch-record-report/version-approval/submit',
      'versionStatus'
    ],
    e2eTokens: [
      '/admin-api/mes/pro/batch-record-report/recognize-uploaded',
      '/admin-api/mes/pro/batch-record-report/version-approval/submit',
      'PENDING_APPROVAL',
      'approvalInstanceId'
    ],
    e2eFile: 'tests/e2e/edhr-batch-version-phase1-real-flow.e2e.js',
    packageScript: 'e2e:edhr:batch-version-phase1',
    checkScript: 'e2e:edhr:batch-version-phase1:check',
    taskEvidence: 'doc/tasks/20260708-edhr-version-implementation/execution-log.md'
  }
])

const CHECK_MODE = 'check'
const RUN_REAL_MODE = 'run-real'

function normalizeRelativePath(relativePath) {
  return relativePath.replace(/\\/g, '/')
}

function resolveFromCwd(cwd, relativePath) {
  return path.resolve(cwd, relativePath)
}

function readUtf8(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function pathExists(cwd, relativePath) {
  return fs.existsSync(resolveFromCwd(cwd, relativePath))
}

function uniqueInOrder(values) {
  const seen = new Set()
  const result = []
  for (const value of values) {
    if (seen.has(value)) continue
    seen.add(value)
    result.push(value)
  }
  return result
}

function loadPackageJson(cwd) {
  return JSON.parse(readUtf8(resolveFromCwd(cwd, 'package.json')))
}

function collectFiles(root) {
  if (!fs.existsSync(root)) return []
  const stack = [root]
  const files = []
  while (stack.length > 0) {
    const current = stack.pop()
    const stat = fs.statSync(current)
    if (stat.isDirectory()) {
      for (const child of fs.readdirSync(current)) {
        stack.push(path.join(current, child))
      }
    } else {
      files.push(current)
    }
  }
  return files
}

function collectExpectedEdhrSourceFiles(cwd) {
  const roots = [
    'src/views/mes/pro/edhr',
    'src/api/mes/pro/edhr'
  ]
  const files = roots.flatMap((root) =>
    collectFiles(resolveFromCwd(cwd, root)).map((file) =>
      normalizeRelativePath(path.relative(cwd, file))
    )
  )
  files.push('src/views/mes/pro/feedback/FeedbackForm.vue')
  files.push('src/api/mes/pro/feedback/index.ts')
  return uniqueInOrder(files).sort()
}

function findMissingPackageScripts(packageJson, matrix) {
  const scripts = packageJson.scripts || {}
  const failures = []
  const expectedReleaseScripts = [
    [RELEASE_CHECK_SCRIPT, RELEASE_CHECK_COMMAND],
    [RELEASE_REAL_SCRIPT, RELEASE_REAL_COMMAND]
  ]
  for (const [scriptName, expectedCommand] of expectedReleaseScripts) {
    if (scripts[scriptName] !== expectedCommand) {
      failures.push(
        `package script ${scriptName} must be exactly: ${expectedCommand}`
      )
    }
  }
  for (const item of matrix) {
    for (const scriptName of [item.packageScript, item.checkScript]) {
      if (!scripts[scriptName]) {
        failures.push(`${item.featureId} missing package script: ${scriptName}`)
      }
    }
  }
  return failures
}

function findMatrixShapeFailures(matrix, expectedFeatureIds = REQUIRED_FEATURE_IDS) {
  const failures = []
  const actualIds = matrix.map((item) => item.featureId)
  const missingIds = expectedFeatureIds.filter((id) => !actualIds.includes(id))
  const extraIds = actualIds.filter((id) => !expectedFeatureIds.includes(id))
  if (missingIds.length > 0) {
    failures.push(`matrix missing featureIds: ${missingIds.join(', ')}`)
  }
  if (extraIds.length > 0) {
    failures.push(`matrix has unexpected featureIds: ${extraIds.join(', ')}`)
  }
  for (const item of matrix) {
    for (const field of [
      'featureId',
      'featureName',
      'e2eFile',
      'packageScript',
      'checkScript',
      'taskEvidence'
    ]) {
      if (!item[field] || typeof item[field] !== 'string') {
        failures.push(`${item.featureId || '<unknown>'} missing ${field}`)
      }
    }
    for (const field of ['routes', 'sourceFiles', 'apiTokens', 'e2eTokens']) {
      if (!Array.isArray(item[field]) || item[field].length === 0) {
        failures.push(`${item.featureId || '<unknown>'} missing non-empty ${field}`)
      }
    }
  }
  return failures
}

function hasPlainAdminPassword(source) {
  return source.toLowerCase().includes(['admin', '123'].join(''))
}

function findDangerousBypassPatterns(relativePath, source) {
  const failures = []
  const dangerousPatterns = [
    [/\.skip\s*\(/, 'skip call'],
    [/\b(?:test|it|describe)\.skip\b/, 'test skip'],
    [/\bDEFAULT_PASSWORD\b/, 'default password constant'],
    [/\bprocess\.exitCode\s*=\s*0\b/, 'default success exit code'],
    [/\bprocess\.exit\s*\(\s*0\s*\)/, 'default success exit']
  ]
  for (const [pattern, label] of dangerousPatterns) {
    if (pattern.test(source)) {
      failures.push(`${relativePath} contains forbidden ${label}`)
    }
  }
  if (hasPlainAdminPassword(source)) {
    failures.push(`${relativePath} contains forbidden plaintext default password marker`)
  }
  return failures
}

function containsAllEvidenceMarkers(source) {
  return (
    source.includes('BDD:') &&
    /\b(?:FAIL|RED)\b/.test(source) &&
    /\bPASS\b/.test(source) &&
    /\bBLOCKED\b/.test(source)
  )
}

function stripJavaScriptComments(source) {
  let result = ''
  let index = 0
  let state = 'code'

  while (index < source.length) {
    const current = source[index]
    const next = source[index + 1]

    if (state === 'code') {
      if (current === '/' && next === '/') {
        state = 'line-comment'
        index += 2
        continue
      }
      if (current === '/' && next === '*') {
        state = 'block-comment'
        index += 2
        continue
      }
      if (current === "'") state = 'single-quote'
      if (current === '"') state = 'double-quote'
      if (current === '`') state = 'template'
      result += current
      index += 1
      continue
    }

    if (state === 'line-comment') {
      if (current === '\n' || current === '\r') {
        result += current
        state = 'code'
      }
      index += 1
      continue
    }

    if (state === 'block-comment') {
      if (current === '*' && next === '/') {
        state = 'code'
        index += 2
        continue
      }
      if (current === '\n' || current === '\r') {
        result += current
      }
      index += 1
      continue
    }

    result += current
    if (current === '\\') {
      if (index + 1 < source.length) {
        result += source[index + 1]
        index += 2
        continue
      }
    }
    if (state === 'single-quote' && current === "'") state = 'code'
    if (state === 'double-quote' && current === '"') state = 'code'
    if (state === 'template' && current === '`') state = 'code'
    index += 1
  }

  return result
}

function findBindingFailures(cwd, matrix) {
  const failures = []
  const coveredSourceFiles = new Set()
  for (const item of matrix) {
    const requiredFiles = [...item.sourceFiles, item.e2eFile, item.taskEvidence]
    for (const relativePath of requiredFiles) {
      if (!pathExists(cwd, relativePath)) {
        failures.push(`${item.featureId} missing file: ${relativePath}`)
      }
    }
    for (const relativePath of item.sourceFiles) {
      coveredSourceFiles.add(normalizeRelativePath(relativePath))
    }
    if (failures.some((failure) => failure.startsWith(`${item.featureId} missing file:`))) {
      continue
    }

    const sourceBundle = item.sourceFiles
      .map((relativePath) => readUtf8(resolveFromCwd(cwd, relativePath)))
      .join('\n')
    const e2eSource = readUtf8(resolveFromCwd(cwd, item.e2eFile))
    const taskEvidenceSource = readUtf8(resolveFromCwd(cwd, item.taskEvidence))
    const e2eCoverageSource = stripJavaScriptComments(e2eSource)
    const combinedRouteBinding = `${sourceBundle}\n${e2eCoverageSource}`
    const combinedEvidence = `${e2eSource}\n${taskEvidenceSource}`

    for (const route of item.routes) {
      if (!combinedRouteBinding.includes(route)) {
        failures.push(`${item.featureId} missing route token in source/e2e binding: ${route}`)
      }
    }
    for (const apiToken of item.apiTokens) {
      if (!sourceBundle.includes(apiToken)) {
        failures.push(`${item.featureId} missing source/API contract token in sourceFiles: ${apiToken}`)
      }
    }
    for (const e2eToken of item.e2eTokens) {
      if (!e2eCoverageSource.includes(e2eToken)) {
        failures.push(
          `${item.featureId} missing real E2E token: ${e2eToken}; token must come from E2E script code/string only, not sourceFiles or task evidence`
        )
      }
    }
    if (!containsAllEvidenceMarkers(combinedEvidence)) {
      failures.push(`${item.featureId} missing BDD plus FAIL/PASS/BLOCKED evidence markers`)
    }
    failures.push(...findDangerousBypassPatterns(item.e2eFile, e2eSource))
  }

  const missingCoverage = collectExpectedEdhrSourceFiles(cwd).filter(
    (relativePath) => !coveredSourceFiles.has(relativePath)
  )
  for (const relativePath of missingCoverage) {
    failures.push(`uncovered eDHR source file: ${relativePath}`)
  }
  return failures
}

export function validateCoverageContract({
  cwd = process.cwd(),
  matrix = RELEASE_E2E_COVERAGE_MATRIX,
  packageJson = loadPackageJson(cwd),
  expectedFeatureIds = REQUIRED_FEATURE_IDS
} = {}) {
  const failures = [
    ...findMatrixShapeFailures(matrix, expectedFeatureIds),
    ...findMissingPackageScripts(packageJson, matrix),
    ...findBindingFailures(cwd, matrix)
  ]
  return {
    ok: failures.length === 0,
    failures
  }
}

export function getOrderedPackageScripts(matrix, fieldName) {
  return uniqueInOrder(matrix.map((item) => item[fieldName]))
}

export function getOrderedE2eFiles(matrix = RELEASE_E2E_COVERAGE_MATRIX) {
  return uniqueInOrder(matrix.map((item) => item.e2eFile))
}

function pnpmInvocation(scriptName) {
  if (process.platform !== 'win32') {
    return {
      command: 'pnpm',
      args: [scriptName]
    }
  }

  const searchPath = process.env.PATH || process.env.Path || ''
  for (const entry of searchPath.split(path.delimiter)) {
    const dir = entry.replace(/^"|"$/g, '')
    if (!dir) continue
    const pnpmCjs = path.join(dir, 'node_modules', 'pnpm', 'bin', 'pnpm.cjs')
    if (fs.existsSync(pnpmCjs)) {
      return {
        command: process.execPath,
        args: [pnpmCjs, scriptName]
      }
    }
    const pnpmCmd = path.join(dir, 'pnpm.cmd')
    if (fs.existsSync(pnpmCmd)) {
      return {
        command: pnpmCmd,
        args: [scriptName]
      }
    }
  }

  return {
    command: 'pnpm',
    args: [scriptName]
  }
}

function defaultCommandRunner(commandSpec) {
  const result = spawnSync(commandSpec.command, commandSpec.args, {
    cwd: commandSpec.cwd,
    encoding: 'utf8',
    env: process.env,
    stdio: ['ignore', 'pipe', 'pipe']
  })
  if (result.error) {
    return {
      status: 1,
      error: result.error
    }
  }
  return {
    status: typeof result.status === 'number' ? result.status : 1
  }
}

function commandFailureResult(label, status, error) {
  const suffix = error?.message ? `, ${error.message}` : ''
  return {
    ok: false,
    code: status || 1,
    failures: [`${label} failed with exit code ${status || 1}${suffix}`]
  }
}

function quoteCommandArg(arg) {
  if (/^[A-Za-z0-9_./:=\\-]+$/.test(arg)) return arg
  return JSON.stringify(arg)
}

function buildCommandString(argv, cwd) {
  const scriptPath = normalizeRelativePath(path.relative(cwd, fileURLToPath(import.meta.url)))
  return ['node', scriptPath, ...argv].map(quoteCommandArg).join(' ')
}

function failureMatchesFeature(failure, item) {
  return [item.featureId, item.e2eFile, item.packageScript, item.checkScript].some(
    (token) => token && failure.includes(token)
  )
}

function createFeatureReports(matrix, result) {
  const failures = result.failures || []
  const hasGlobalFailure = failures.some(
    (failure) => !matrix.some((item) => failureMatchesFeature(failure, item))
  )

  return matrix.map((item) => {
    const failed = !result.ok && (
      hasGlobalFailure ||
      failures.some((failure) => failureMatchesFeature(failure, item))
    )
    return {
      featureId: item.featureId,
      featureName: item.featureName,
      e2eFile: item.e2eFile,
      packageScript: item.packageScript,
      checkScript: item.checkScript,
      status: failed ? 'failed' : 'passed'
    }
  })
}

export function buildReleaseCoverageReport({
  mode,
  result,
  matrix = RELEASE_E2E_COVERAGE_MATRIX,
  argv = [],
  cwd = process.cwd(),
  generatedAt = new Date()
} = {}) {
  const features = createFeatureReports(matrix, result)
  const allFeaturesPassed = features.every((feature) => feature.status === 'passed')
  const status = result.ok && allFeaturesPassed ? 'passed' : 'failed'
  const failures = status === 'passed'
    ? []
    : (result.failures?.length ? result.failures : ['release coverage gate failed'])

  return {
    schemaVersion: REPORT_SCHEMA_VERSION,
    generatedAt: generatedAt.toISOString(),
    mode,
    status,
    command: buildCommandString(argv, cwd),
    featureCount: features.length,
    features,
    checkedScripts: mode === CHECK_MODE ? getOrderedPackageScripts(matrix, 'checkScript') : [],
    checkedE2eFiles: mode === CHECK_MODE ? getOrderedE2eFiles(matrix) : [],
    failures,
    realGateClaimed: mode === RUN_REAL_MODE && status === 'passed'
  }
}

function resolveReportPath(cwd, reportPath) {
  return path.isAbsolute(reportPath) ? reportPath : path.resolve(cwd, reportPath)
}

export function writeReleaseCoverageReport(reportPath, report) {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true })
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
}

export function runCheckMode({
  cwd = process.cwd(),
  matrix = RELEASE_E2E_COVERAGE_MATRIX,
  commandRunner = defaultCommandRunner
} = {}) {
  const validation = validateCoverageContract({ cwd, matrix })
  if (!validation.ok) {
    return { ok: false, code: 1, failures: validation.failures }
  }

  for (const scriptName of getOrderedPackageScripts(matrix, 'checkScript')) {
    const invocation = pnpmInvocation(scriptName)
    const result = commandRunner({
      kind: 'package-check',
      scriptName,
      command: invocation.command,
      args: invocation.args,
      cwd
    })
    if (result.status !== 0) {
      return commandFailureResult(`check script ${scriptName}`, result.status, result.error)
    }
  }

  for (const e2eFile of getOrderedE2eFiles(matrix)) {
    const result = commandRunner({
      kind: 'node-check',
      file: e2eFile,
      command: process.execPath,
      args: ['--check', e2eFile],
      cwd
    })
    if (result.status !== 0) {
      return commandFailureResult(`node --check ${e2eFile}`, result.status, result.error)
    }
  }

  return {
    ok: true,
    code: 0,
    failures: [],
    checkedFeatureCount: matrix.length,
    checkedScripts: getOrderedPackageScripts(matrix, 'checkScript'),
    checkedE2eFiles: getOrderedE2eFiles(matrix)
  }
}

export function runRealMode({
  cwd = process.cwd(),
  matrix = RELEASE_E2E_COVERAGE_MATRIX,
  commandRunner = defaultCommandRunner
} = {}) {
  const validation = validateCoverageContract({ cwd, matrix })
  if (!validation.ok) {
    return { ok: false, code: 1, failures: validation.failures }
  }

  for (const scriptName of getOrderedPackageScripts(matrix, 'packageScript')) {
    const invocation = pnpmInvocation(scriptName)
    const result = commandRunner({
      kind: 'package-real',
      scriptName,
      command: invocation.command,
      args: invocation.args,
      cwd
    })
    if (result.status !== 0) {
      return commandFailureResult(`real E2E script ${scriptName}`, result.status, result.error)
    }
  }

  return {
    ok: true,
    code: 0,
    failures: [],
    ranScripts: getOrderedPackageScripts(matrix, 'packageScript')
  }
}

function setParsedMode(parsed, mode) {
  if (parsed.mode && parsed.mode !== mode) {
    throw new Error('Choose exactly one release coverage gate mode: --check or --run-real')
  }
  parsed.mode = mode
}

function setParsedReportPath(parsed, reportPath) {
  if (typeof reportPath !== 'string' || reportPath.trim().length === 0 || reportPath.startsWith('-')) {
    throw new Error('--report requires a non-empty path that does not start with "-"')
  }
  if (parsed.reportPath) {
    throw new Error('Duplicate --report argument')
  }
  parsed.reportPath = reportPath
}

function parseArguments(argv) {
  const parsed = {
    mode: undefined,
    reportPath: undefined
  }

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--check') {
      setParsedMode(parsed, CHECK_MODE)
      continue
    }
    if (arg === '--run-real') {
      setParsedMode(parsed, RUN_REAL_MODE)
      continue
    }
    if (arg === '--report') {
      index += 1
      setParsedReportPath(parsed, argv[index])
      continue
    }
    if (arg.startsWith('--report=')) {
      setParsedReportPath(parsed, arg.slice('--report='.length))
      continue
    }
    throw new Error(`Unknown argument: ${arg}`)
  }

  return {
    mode: parsed.mode || CHECK_MODE,
    reportPath: parsed.reportPath
  }
}

function printResult(result, mode) {
  if (result.ok) {
    if (mode === CHECK_MODE) {
      console.log(
        `PASS: eDHR release E2E coverage check completed; features=${result.checkedFeatureCount}, checkScripts=${result.checkedScripts.length}, syntaxFiles=${result.checkedE2eFiles.length}`
      )
    } else {
      console.log(
        `PASS: eDHR release real E2E gate completed; scripts=${result.ranScripts.join(', ')}`
      )
    }
    return
  }
  console.error(`FAIL: eDHR release E2E coverage gate failed in ${mode} mode`)
  for (const failure of result.failures) {
    console.error(`- ${failure}`)
  }
}

export function main(argv = process.argv.slice(2), cwd = process.cwd(), options = {}) {
  const { mode, reportPath } = parseArguments(argv)
  const matrix = options.matrix || RELEASE_E2E_COVERAGE_MATRIX
  const commandRunner = options.commandRunner || defaultCommandRunner
  const result = mode === CHECK_MODE
    ? runCheckMode({ cwd, matrix, commandRunner })
    : runRealMode({ cwd, matrix, commandRunner })
  if (reportPath) {
    const report = buildReleaseCoverageReport({
      mode,
      result,
      matrix,
      argv,
      cwd,
      generatedAt: options.generatedAt || new Date()
    })
    writeReleaseCoverageReport(resolveReportPath(cwd, reportPath), report)
  }
  printResult(result, mode)
  return result.code
}

const executedFile = process.argv[1] ? path.resolve(process.argv[1]) : ''
if (executedFile && executedFile === fileURLToPath(import.meta.url)) {
  try {
    process.exitCode = main()
  } catch (error) {
    console.error(`FAIL: ${error.message}`)
    process.exitCode = 1
  }
}
