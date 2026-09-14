const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const recordsPanePath = 'src/views/signature-governance/components/SignatureGovernanceRecordsPane.vue'
assert.equal(exists(recordsPanePath), true, `${recordsPanePath} must exist`)

const pageSource = readText('src/views/signature-governance/index.vue')
const routeSource = readText('src/router/modules/remaining.ts')
const apiSource = readText('src/api/signature-governance/records.ts')
const paneSource = readText(recordsPanePath)
const mySignaturePaneSource = readText(
  'src/views/signature-governance/components/SignatureGovernanceMySignaturePane.vue'
)

assert.match(pageSource, /SignatureGovernanceRecordsPane/)
assert.match(pageSource, /activeTab\s*===\s*'signature-records'/)
assert.doesNotMatch(pageSource, /activeTab\s*===\s*'file-signatures'/)
assert.doesNotMatch(pageSource, /activeTab\s*===\s*'batch-signatures'/)
assert.doesNotMatch(pageSource, /DccSignatureRecordsPane/)
assert.doesNotMatch(pageSource, /EdhrBatchSignatureRecordsPane/)

assert.match(routeSource, /redirect:\s*'\/signature-governance\/signature-records'/)
assert.match(routeSource, /path:\s*'signature-records'/)
assert.match(routeSource, /name:\s*'SignatureGovernanceSignatureRecords'/)
assert.match(routeSource, /title:\s*'签名记录'/)
assert.match(routeSource, /redirect:\s*'\/signature-governance\/signature-records'/)
for (const legacyRoute of ['file-signatures', 'batch-signatures']) {
  assert.match(routeSource, new RegExp(`path:\\s*'${legacyRoute}'[\\s\\S]*redirect:\\s*'\\/signature-governance\\/signature-records'`))
  assert.match(routeSource, new RegExp(`path:\\s*'${legacyRoute}'[\\s\\S]*hidden:\\s*true`))
}

assert.match(apiSource, /SignatureGovernanceRecordPageReqVO/)
assert.match(apiSource, /SignatureGovernanceRecordRespVO/)
assert.match(apiSource, /signedAt\?: number/)
assert.doesNotMatch(apiSource, /signedAt\?: string \| number/)
assert.match(apiSource, /sourceCodes/)
assert.match(apiSource, /\/signature-governance\/signature-records\/page/)
assert.match(apiSource, /\/signature-governance\/my-signature-records\/page/)
assert.match(apiSource, /fetchSignatureGovernanceRecordPdfArtifact/)
assert.match(apiSource, /downloadSignatureGovernanceRecordPdf/)
assert.match(apiSource, /\/signature-governance\/signature-records\/\$\{encodeURIComponent\(String\(normalizedGlobalId\)\)\}\/pdf/)
assert.match(apiSource, /responseType:\s*'blob'/)
assert.match(apiSource, /downloadByData\(blob,\s*fileName,\s*'application\/pdf'\)/)

assert.match(paneSource, /<UnifiedListTemplate/)
assert.match(paneSource, /table-key="signature\.governance\.records"/)
assert.match(paneSource, /label="来源"/)
assert.match(paneSource, /label="业务记录"/)
assert.match(paneSource, /label="签名人"/)
assert.match(paneSource, /label="部门\/岗位"/)
assert.match(paneSource, /label="角色"/)
assert.match(paneSource, /label="签名摘要"/)
assert.match(paneSource, /label="证据摘要"/)
assert.match(paneSource, /label="签名时间"/)
assert.match(paneSource, /label="操作"/)
assert.match(paneSource, /sourceOptions/)
assert.match(paneSource, /文件/)
assert.match(paneSource, /批记录/)
assert.match(paneSource, /展厅/)
assert.match(paneSource, /\{\s*label:\s*'审批',\s*value:\s*'BPM'\s*\}/)
assert.match(paneSource, /报工审批/)
assert.match(paneSource, /SCHEDULING/)
assert.match(paneSource, /DOCUMENT_CONTROL/)
assert.match(paneSource, /记录范围/)
assert.match(paneSource, /我的签名/)
assert.match(paneSource, /getMySignatureGovernanceRecordPage/)
assert.match(paneSource, /MY_SIGNATURES/)
assert.match(paneSource, /pageLoader\(buildPageParams\(\)\)/)
assert.match(paneSource, /formatRecordSignedAt/)
assert.match(paneSource, /formatDate\(new Date\(signedAt\)\)/)
assert.doesNotMatch(paneSource, /Number\(text\)|text\.replace\(' ', 'T'\)|new Date\(normalizedText\)/)
assert.match(paneSource, /formatSourceTableLabel/)
assert.match(
  paneSource,
  /{{\s*formatSourceLabel\(row\)\s*}}/,
  '来源列必须使用统一中文来源标签'
)
assert.match(paneSource, /bpm_approval_signature_record[\s\S]*审批签名记录/)
assert.match(paneSource, /mes_pro_batch_record_execution_signature[\s\S]*批记录执行签名记录/)
assert.match(paneSource, /formatSignatureActionText/)
assert.match(paneSource, /APPROVE[\s\S]*审批通过/)
assert.match(paneSource, /formatSignatureMeaningText/)
assert.match(paneSource, /PQC_SUBMIT[\s\S]*PQC 检验提交/)
assert.match(paneSource, /formatEvidenceStatusText/)
assert.match(paneSource, /PASSWORD_VERIFIED[\s\S]*签名密码已验证/)
assert.match(paneSource, /CAPTURED[\s\S]*已采集/)
assert.match(paneSource, /formatBusinessRecordName/)
assert.match(paneSource, /BPM审批[\s\S]*审批/)
assert.match(
  paneSource,
  /来源表：\{\{\s*formatSourceTableLabel\(row\.sourceTable\)\s*\}\}/,
  '来源表必须显示中文来源说明'
)
assert.match(
  paneSource,
  /{{\s*formatSignatureMeaningText\(row\)\s*}}/,
  '签名摘要标签必须显示中文含义'
)
assert.match(
  paneSource,
  /{{\s*formatSignatureSummaryText\(row\)\s*}}/,
  '签名摘要正文必须显示中文动作或正式意见'
)
assert.match(
  paneSource,
  /{{\s*formatEvidenceStatusText\(row\.evidenceStatus\)\s*}}/,
  '证据状态必须显示中文状态'
)
assert.doesNotMatch(
  paneSource,
  /{{\s*row\.evidenceStatus\s*\|\|\s*'未记录'\s*}}/,
  '证据状态不得直接暴露英文状态码'
)
assert.doesNotMatch(
  paneSource,
  /来源表：\{\{\s*row\.sourceTable\s*\}\}/,
  '来源表不得直接暴露数据库表名'
)
assert.doesNotMatch(
  paneSource,
  /row\.sourceLabel\s*\|\|\s*sourceLabel\(row\.sourceCode\)/,
  '来源列不得直接使用后端旧英文模块标签'
)
assert.doesNotMatch(
  paneSource,
  /row\.comment\s*\|\|\s*row\.meaningCode\s*\|\|\s*'-'/,
  '签名摘要正文不得把含义编码作为可见默认文本'
)
assert.match(paneSource, /fetchSignatureGovernanceRecordPdfArtifact/)
assert.match(paneSource, /downloadSignatureGovernanceRecordPdf/)
assert.match(paneSource, /openRecordPdfPreview/)
assert.match(paneSource, /handleRecordPdfDownload/)
assert.match(paneSource, />\s*预览\s*</)
assert.match(paneSource, />\s*PDF\s*</)
assert.match(paneSource, /<iframe[\s\S]*:src="signatureRecordPdfPreviewDialog\.objectUrl"/)
assert.doesNotMatch(paneSource, /v-if="row\.sourceCode === 'FILE'"/)
assert.doesNotMatch(paneSource, />\s*预览PDF\s*</)
assert.doesNotMatch(paneSource, /mock|fallback|TODO/i)

assert.match(mySignaturePaneSource, /data-testid="dcc-my-signature-image-actions"/)
assert.doesNotMatch(mySignaturePaneSource, /我的签名记录/)
assert.doesNotMatch(mySignaturePaneSource, /getMySignatureGovernanceRecordPage/)
assert.doesNotMatch(mySignaturePaneSource, /mySignatureRecord/)
assert.doesNotMatch(mySignaturePaneSource, /fetchDccSignatureEvidencePdfArtifact/)

console.log('signature governance unified records static contract passed')
