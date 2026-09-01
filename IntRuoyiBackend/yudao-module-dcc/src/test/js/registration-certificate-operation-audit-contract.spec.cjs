const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const detailModel = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateDetail.java'
)
const historyItem = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/history/DccRegistrationCertificateHistoryItem.java'
)
const operationAuditService = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/audit/DccRegistrationCertificateOperationAuditService.java'
)
const queryService = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateQueryServiceImpl.java'
)
const historyService = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/history/DccRegistrationCertificateHistoryServiceImpl.java'
)

for (const field of [
  'String uploadOperatorName',
  'LocalDateTime uploadedAt',
  'String uploadApproverName',
  'LocalDateTime uploadApprovedAt'
]) {
  assert.match(
    detailModel,
    new RegExp(`private\\s+${field.replace(' ', '\\s+')}\\s*;`),
    `detail response must expose ${field}`
  )
}

for (const field of [
  'String renewalOperatorName',
  'LocalDateTime renewalOperatedAt',
  'String renewalApproverName',
  'LocalDateTime renewalApprovedAt'
]) {
  assert.match(
    historyItem,
    new RegExp(field.replace(' ', '\\s+')),
    `renewal history response must expose ${field}`
  )
}

for (const formalColumn of [
  'requester_user_id',
  'requested_at',
  'formalized_by',
  'formalized_at',
  'INITIAL_CERTIFICATE',
  'RENEWAL_CERTIFICATE'
]) {
  assert.match(
    operationAuditService,
    new RegExp(formalColumn),
    `operation audit query must read formal ${formalColumn} facts`
  )
}
assert.match(
  operationAuditService,
  /AdminUserApi[\s\S]*getUserList/,
  'operation audit service must resolve formal user names through the system user API'
)
assert.doesNotMatch(
  operationAuditService,
  /String\.valueOf\(\s*(userId|operatorId|approverId)\s*\)|未配置人员|未知人员/,
  'operation audit service must not replace a missing formal user name with an id or placeholder'
)
assert.match(
  queryService,
  /operationAuditService\.getInitialAudit\(tenantId, certificateId\)/,
  'detail query must load the certificate-wide initial upload operation audit'
)
for (const mapping of [
  'uploadOperatorName\\(initialAudit\\.operatorName\\(\\)\\)',
  'uploadedAt\\(initialAudit\\.operatedAt\\(\\)\\)',
  'uploadApproverName\\(initialAudit\\.approverName\\(\\)\\)',
  'uploadApprovedAt\\(initialAudit\\.approvedAt\\(\\)\\)'
]) {
  assert.match(queryService, new RegExp(mapping), `detail mapping must include ${mapping}`)
}
assert.match(
  historyService,
  /operationAuditService\.getRenewalAudits\(tenantId, certificateId\)/,
  'history query must load renewal audits for the same certificate'
)
for (const mapping of [
  'renewalAudit\\.operatorName\\(\\)',
  'renewalAudit\\.operatedAt\\(\\)',
  'renewalAudit\\.approverName\\(\\)',
  'renewalAudit\\.approvedAt\\(\\)'
]) {
  assert.match(historyService, new RegExp(mapping), `renewal history mapping must include ${mapping}`)
}

console.log('registration certificate operation audit backend contract: PASS')
