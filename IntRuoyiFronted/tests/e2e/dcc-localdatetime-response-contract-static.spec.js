const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')

const readSource = (absoluteOrRelativePath, root = frontendRoot) => {
  const absolutePath = path.isAbsolute(absoluteOrRelativePath)
    ? absoluteOrRelativePath
    : path.join(root, absoluteOrRelativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${absolutePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const backendSerializerConfig = readSource(
  'IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-web/src/main/java/cn/iocoder/yudao/framework/jackson/config/YudaoJacksonAutoConfiguration.java',
  workspaceRoot
)

assert.match(
  backendSerializerConfig,
  /serializerByType\(LocalDateTime\.class,\s*TimestampLocalDateTimeSerializer\.INSTANCE\)/,
  'backend HTTP serializer must continue emitting LocalDateTime as epoch-millisecond timestamps'
)

const sources = new Map()

const readCachedSource = (relativePath, root = frontendRoot) => {
  const key = `${root}:${relativePath}`
  if (!sources.has(key)) {
    sources.set(key, readSource(relativePath, root))
  }
  return sources.get(key)
}

const getInterfaceBody = (source, interfaceName) => {
  const pattern = new RegExp(`export interface ${interfaceName}(?:\\s+extends\\s+[^\\{]+)?\\s*\\{([\\s\\S]*?)\\n\\}`)
  const match = source.match(pattern)
  assert.ok(match, `missing frontend interface ${interfaceName}`)
  return match[1]
}

const assertBackendLocalDateTimeField = (backendPath, field) => {
  const backendSource = readCachedSource(backendPath, workspaceRoot)
  assert.match(
    backendSource,
    new RegExp(`private\\s+(?:java\\.time\\.)?LocalDateTime\\s+${field};`),
    `backend ${backendPath} must expose ${field} as LocalDateTime`
  )
}

const assertFrontendTimestampField = (frontendPath, interfaceName, field) => {
  const frontendSource = readCachedSource(frontendPath)
  const interfaceBody = getInterfaceBody(frontendSource, interfaceName)
  const fieldMatch = interfaceBody.match(new RegExp(`(?:^|\\n)\\s*${field}\\??:\\s*([^\\n]+)`))
  assert.ok(fieldMatch, `missing frontend field ${interfaceName}.${field}`)
  const declaredType = fieldMatch[1].replace(/\/\/.*$/, '').trim()
  assert.match(
    declaredType,
    /^number(?:\s*\|\s*null)?$/,
    `frontend ${interfaceName}.${field} must be a numeric timestamp, got ${declaredType}`
  )
}

const contracts = [
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileUploadTemporaryStatusRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileUploadTemporaryStatusRespVO',
    fields: ['expireTime', 'cleanupTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileUploadRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileUploadRespVO',
    fields: ['expireTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFilePrintRecordRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFilePrintRecordVO',
    fields: ['printTime', 'approvalTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileVersionHistoryRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileVersionHistoryVO',
    fields: ['publishedTime', 'obsoletedTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileDistributionStatusRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileDistributionStatusVO',
    fields: ['acknowledgedAt', 'recoveredAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileDistributionRecipientStatusRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileDistributionRecipientStatusVO',
    fields: ['readAt', 'acknowledgedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccPaperDistributionRecordRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFilePaperDistributionRecordVO',
    fields: ['issuedAt', 'recoveredAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileTrainingAssignmentRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileTrainingAssignmentVO',
    fields: ['acknowledgedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSignatureSummaryRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileSignatureSummaryVO',
    fields: ['signedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccSignatureActionRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'DccSignatureActionRespVO',
    fields: ['signedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileVO',
    fields: [
      'projectCodeRecognizedTime',
      'submittedTime',
      'approvedTime',
      'publishedTime',
      'rejectedTime',
      'stampedTime',
      'obsoletedTime'
    ]
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccExternalFileReviewRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ExternalFileReviewVO',
    fields: ['closedTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileBatchRecognitionTaskRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'ControlledFileBatchRecognitionTaskRespVO',
    fields: ['startedAt', 'completedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccNasPermissionSnapshotSummaryRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'NasPermissionSnapshotSummaryVO',
    fields: ['capturedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccNasPermissionRestoreStatusRespVO.java',
    frontend: 'src/api/dcc/controlledFile/workflow.ts',
    interfaceName: 'NasPermissionRestoreStatusVO',
    fields: ['startedAt', 'completedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/assignment/DccProjectCodeAssignmentRespVO.java',
    frontend: 'src/api/dcc/controlledFile/projectCodeAssignments.ts',
    interfaceName: 'DccProjectCodeAssignmentRespVO',
    fields: ['assignedTime', 'expireTime', 'revokedTime', 'createTime', 'updateTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/assignment/DccProjectCodeAssignmentFileRespVO.java',
    frontend: 'src/api/dcc/controlledFile/projectCodeAssignments.ts',
    interfaceName: 'DccProjectCodeAssignmentFileRespVO',
    fields: ['lastChangedTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/assignment/DccProjectCodeAssignmentAuditRespVO.java',
    frontend: 'src/api/dcc/controlledFile/projectCodeAssignments.ts',
    interfaceName: 'DccProjectCodeAssignmentAuditRespVO',
    fields: ['changedTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/DccProjectCodeRespVO.java',
    frontend: 'src/api/dcc/controlledFile/projectCodes.ts',
    interfaceName: 'DccProjectCodeRespVO',
    fields: ['createTime', 'updateTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/onboarding/DccProductOnboardingRespVO.java',
    frontend: 'src/api/dcc/controlledFile/projectCodes.ts',
    interfaceName: 'DccProductOnboardingRespVO',
    fields: ['approvedTime', 'createTime', 'updateTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileCategoryRespVO.java',
    frontend: 'src/api/dcc/controlledFile/fileCategories.ts',
    interfaceName: 'ControlledFileCategoryVO',
    fields: ['createTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccCategoryApprovalMatrixRespVO.java',
    frontend: 'src/api/dcc/controlledFile/fileCategories.ts',
    interfaceName: 'ControlledFileCategoryApprovalMatrixVO',
    fields: ['effectiveTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccCategoryReviewMatrixRowRespVO.java',
    frontend: 'src/api/dcc/controlledFile/fileCategories.ts',
    interfaceName: 'ControlledFileCategoryReviewMatrixRowVO',
    fields: ['effectiveTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileTypeTaxonomyRespVO.java',
    frontend: 'src/api/dcc/controlledFile/fileTypeTaxonomies.ts',
    interfaceName: 'DccFileTypeTaxonomyVO',
    fields: ['createTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/route/vo/DccApprovalRouteRespVO.java',
    frontend: 'src/api/dcc/controlledFile/approvalRoutes.ts',
    interfaceName: 'ControlledFileApprovalRouteVO',
    fields: ['effectiveTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/position/vo/DccApprovalPositionRespVO.java',
    frontend: 'src/api/dcc/controlledFile/approvalPositions.ts',
    interfaceName: 'ControlledFileApprovalPositionVO',
    fields: ['createTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/directory/vo/DccDirectoryRespVO.java',
    frontend: 'src/api/dcc/controlledFile/directories.ts',
    interfaceName: 'ControlledFileDirectoryVO',
    fields: ['createTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccApprovalPrintTemplateRespVO.java',
    frontend: 'src/api/dcc/controlledFile/approvalPrintTemplate.ts',
    interfaceName: 'ApprovalPrintTemplateVO',
    fields: ['updateTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/log/vo/DccControlledFileLogRespVO.java',
    frontend: 'src/api/dcc/controlledFile/logs.ts',
    interfaceName: 'DccControlledFileLogRespVO',
    fields: ['occurredAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/audit/vo/DccControlledFileAuditRespVO.java',
    frontend: 'src/api/dcc/controlledFile/audits.ts',
    interfaceName: 'DccControlledFileAuditRespVO',
    fields: ['occurredAt', 'issuedAt', 'expiresAt', 'createTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/distribution/vo/DccDistributionTaskRespVO.java',
    frontend: 'src/api/dcc/controlledFile/distribution.ts',
    interfaceName: 'DistributionTaskVO',
    fields: ['readAt', 'acknowledgedAt', 'publishedTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/training/vo/DccTrainingTaskRespVO.java',
    frontend: 'src/api/dcc/controlledFile/training.ts',
    interfaceName: 'TrainingTaskProgressVO',
    fields: ['firstViewedAt', 'lastViewedAt', 'acknowledgedAt', 'publishedTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/training/vo/DccTrainingExecutionRespVO.java',
    frontend: 'src/api/dcc/controlledFile/training.ts',
    interfaceName: 'TrainingExecutionRowVO',
    fields: ['firstViewedAt', 'lastViewedAt', 'acknowledgedAt', 'publishedTime']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/vo/DccElectronicSignatureRespVO.java',
    frontend: 'src/api/dcc/controlledFile/signatures.ts',
    interfaceName: 'DccElectronicSignatureVO',
    fields: ['signedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/vo/DccSignatureEvidenceRespVO.java',
    frontend: 'src/api/dcc/controlledFile/signatures.ts',
    interfaceName: 'DccSignatureEvidenceRespVO',
    fields: ['verifiedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/vo/DccSignatureVerifyRespVO.java',
    frontend: 'src/api/dcc/controlledFile/signatures.ts',
    interfaceName: 'DccSignatureVerifyRespVO',
    fields: ['verifiedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/vo/DccElectronicSignatureImageRespVO.java',
    frontend: 'src/api/dcc/controlledFile/signatures.ts',
    interfaceName: 'DccElectronicSignatureImageVO',
    fields: ['uploadedAt', 'enabledAt', 'disabledAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/vo/DccSignatureAuthorizationRespVO.java',
    frontend: 'src/api/dcc/controlledFile/signatures.ts',
    interfaceName: 'DccElectronicSignatureAuthorizationVO',
    fields: ['loginDate', 'lockedUntil', 'latestAuditAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/vo/DccElectronicSignatureAuthorizationAuditRespVO.java',
    frontend: 'src/api/dcc/controlledFile/signatures.ts',
    interfaceName: 'DccElectronicSignatureAuthorizationAuditVO',
    fields: ['operatedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/governance/vo/SignatureGovernanceRecordRespVO.java',
    frontend: 'src/api/signature-governance/records.ts',
    interfaceName: 'SignatureGovernanceRecordRespVO',
    fields: ['signedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccNasControlAuditFileRespVO.java',
    frontend: 'src/api/system/nas/index.ts',
    interfaceName: 'DccNasControlAuditFileRespVO',
    fields: ['modifiedAt']
  },
  {
    backend: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/protection/vo/DccUploadSizePolicyRespVO.java',
    frontend: 'src/api/dcc/controlledFile/uploadSizePolicies.ts',
    interfaceName: 'DccUploadSizePolicyVO',
    fields: ['effectiveFrom', 'effectiveTo', 'createTime']
  }
]

for (const contract of contracts) {
  for (const field of contract.fields) {
    assertBackendLocalDateTimeField(contract.backend, field)
    assertFrontendTimestampField(contract.frontend, contract.interfaceName, field)
  }
}

console.log(`PASS: DCC LocalDateTime response contract (${contracts.length} interfaces)`)
