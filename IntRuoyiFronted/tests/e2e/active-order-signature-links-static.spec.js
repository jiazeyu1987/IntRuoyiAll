const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../..')
const panelPath = path.join(
  repoRoot,
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const recordsPanePath = path.join(
  repoRoot,
  'IntRuoyiFronted/src/views/signature-governance/components/SignatureGovernanceRecordsPane.vue'
)
const recordsApiPath = path.join(repoRoot, 'IntRuoyiFronted/src/api/signature-governance/records.ts')
const governanceReqPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/governance/vo/SignatureGovernanceRecordPageReqVO.java'
)
const governanceXmlPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-dcc/src/main/resources/mapper/signature/governance/SignatureGovernanceRecordMapper.xml'
)

const panel = fs.readFileSync(panelPath, 'utf8')
const recordsPane = fs.readFileSync(recordsPanePath, 'utf8')
const recordsApi = fs.readFileSync(recordsApiPath, 'utf8')
const governanceReq = fs.readFileSync(governanceReqPath, 'utf8')
const governanceXml = fs.readFileSync(governanceXmlPath, 'utf8')

assert.match(panel, /quickFilterField:\s*['"]signatureId['"]/)
assert.match(panel, /summaryProcessPersonnelRows[\s\S]*signature:/)
assert.match(panel, /data-active-order-summary-operation-qa-signature[\s\S]*fact\.signatureId/)
assert.match(panel, /productionOperatorSignatures/)
assert.match(panel, /inspectorSignatures/)
assert.match(panel, /approverSignatures/)
assert.match(panel, /productionSubmitterSignatures/)
assert.match(panel, /submission\.submitterSignatures/)
assert.match(panel, /submission\.reviewerSignatures/)
assert.doesNotMatch(panel, /new Set\(pqcLossReportRows\.value\.map\(\(row\) => row\.approverDateText/)

assert.match(recordsPane, /key:\s*['"]signatureId['"]/)
assert.match(recordsPane, /quickFilter\.fieldKey === ['"]signatureId['"]/)
assert.match(recordsApi, /signatureId\?:\s*string/)
assert.match(governanceReq, /private String signatureId;/)
assert.match(governanceXml, /merged\.source_code = ['"]BATCH_RECORD['"]/)
assert.match(governanceXml, /merged\.source_record_id = CAST\(#\{reqVO\.signatureId\} AS UNSIGNED\)/)

console.log('active-order-signature-links-static: PASS')
