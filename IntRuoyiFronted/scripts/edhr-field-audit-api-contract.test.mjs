import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const extractInterface = (source, name) => {
  const match = source.match(new RegExp(`export\\s+interface\\s+${name}(?:\\s+extends\\s+[^\\{]+)?\\s*{([\\s\\S]*?)\\n}`))
  assert.ok(match, `必须声明 ${name}`)
  return match[1]
}
const extractInterfaceFields = (source, name) => {
  return [...extractInterface(source, name).matchAll(/^\s*([A-Za-z_]\w*)\??:/gm)].map((match) => match[1])
}

test('BDD: 字段审计 API 合同 -> helper 使用冻结 REST 路径、请求字段和响应字段', () => {
  const apiSource = readText('src/api/mes/pro/edhr/fieldAudit.ts')

  for (const endpoint of [
    '/mes/pro/batch-record-execution/field-audit/save-changes',
    '/mes/pro/batch-record-execution/field-audit/page',
    '/mes/pro/batch-record-execution/field-audit/detail',
    '/mes/pro/batch-record-execution/field-audit/verify-chain',
    '/mes/pro/batch-record-execution/field-audit/export'
  ]) {
    assert.match(apiSource, new RegExp(endpoint.replaceAll('/', '\\/')), `必须使用 ${endpoint}`)
  }

  assert.match(apiSource, /export\s+type\s+EdhrFieldAuditSignatureAction\s*=\s*'FIELD_CHANGE'/, '字段变更签名动作必须固定为 FIELD_CHANGE')
  assert.match(apiSource, /export\s+type\s+EdhrFieldValueType\s*=[\s\S]*'STRING'[\s\S]*'NUMBER'[\s\S]*'BOOLEAN'[\s\S]*'DATE'[\s\S]*'DATETIME'[\s\S]*'JSON'[\s\S]*'NULL'/s, '必须声明 typed JSON valueType 枚举')
  assert.match(apiSource, /export\s+type\s+EdhrFieldChangeReasonCategory\s*=[\s\S]*'CORRECTION'[\s\S]*'PROCESS_OBSERVATION'[\s\S]*'CALCULATION_FIX'[\s\S]*'OPERATOR_ENTRY'[\s\S]*'OTHER'/s, '必须声明字段变更原因枚举')
  assert.match(apiSource, /export\s+type\s+EdhrHashVerificationStatus\s*=[\s\S]*'VALID'[\s\S]*'CHAIN_BROKEN'[\s\S]*'SIGNATURE_MISMATCH'[\s\S]*'SOURCE_MISSING'[\s\S]*'CONCURRENCY_CONFLICT'/s, '必须声明 hash 校验状态')

  for (const field of ['executionId', 'idempotencyKey', 'baseCellValuesHash', 'baseFieldAuditRevision', 'baseFieldAuditHeadHash', 'changes', 'signature', 'reasonCategory', 'reasonText']) {
    assert.match(apiSource, new RegExp(`\\b${field}\\b`), `保存请求必须包含 ${field}`)
  }

  for (const field of ['fieldPath', 'fieldKey', 'rowIndex', 'columnIndex', 'valueType', 'newValueJson', 'newValueDisplay', 'expectedOldValueJson', 'expectedOldValueHash']) {
    assert.match(apiSource, new RegExp(`\\b${field}\\b`), `changes[] 必须包含 ${field}`)
  }

  for (const field of ['fieldAuditRevision', 'fieldAuditHeadHash', 'cellValuesHash', 'auditBatchId', 'signatureId', 'changedAt', 'hashVerification']) {
    assert.match(apiSource, new RegExp(`\\b${field}\\b`), `保存响应必须包含 ${field}`)
  }

  assert.match(apiSource, /saveEdhrFieldChanges[\s\S]*request\.put<EdhrFieldChangeSaveRespVO>/s, '必须暴露 saveEdhrFieldChanges helper')
  assert.match(apiSource, /getEdhrFieldAuditPage[\s\S]*request\.get<EdhrFieldAuditPageRespVO>/s, '必须暴露字段审计分页 helper')
  assert.match(apiSource, /getEdhrFieldAuditDetail[\s\S]*request\.get<EdhrFieldAuditDetailRespVO>/s, '必须暴露字段审计详情 helper')
  assert.match(apiSource, /verifyEdhrFieldAuditChain[\s\S]*request\.post<EdhrFieldAuditVerifyRespVO>/s, '必须暴露 hash 链校验 helper')
  assert.match(apiSource, /exportEdhrFieldAudit[\s\S]*request\.get<EdhrFieldAuditExportRespVO>/s, '服务端导出 helper 必须读取 JSON ExportRespVO')
  assert.doesNotMatch(apiSource, /exportEdhrFieldAudit[\s\S]*request\.download/s, '字段审计导出后端返回 JSON，不得使用 request.download')
  assert.doesNotMatch(apiSource, /save-draft|saveEdhrExecutionDraft|\/field-audit-(page|chain|export)/, '字段审计 helper 不得复用旧保存或旧命名路径')
})

test('BDD: 字段审计响应展示合同 -> page/detail 类型保留 fieldPath、old/new、签名和 hash 链证据', () => {
  const apiSource = readText('src/api/mes/pro/edhr/fieldAudit.ts')
  const pageReqType = extractInterface(apiSource, 'EdhrFieldAuditPageReqVO')
  const pageRespFields = extractInterfaceFields(apiSource, 'EdhrFieldAuditPageRespVO')
  const entryType = extractInterface(apiSource, 'EdhrFieldAuditEntryVO')
  const entryFields = extractInterfaceFields(apiSource, 'EdhrFieldAuditEntryVO')
  const signatureType = extractInterface(apiSource, 'EdhrFieldAuditSignatureVO')
  const batchType = extractInterface(apiSource, 'EdhrFieldAuditBatchVO')
  const detailReqType = extractInterface(apiSource, 'EdhrFieldAuditDetailReqVO')
  const detailRespType = extractInterface(apiSource, 'EdhrFieldAuditDetailRespVO')
  const verifyReqType = extractInterface(apiSource, 'EdhrFieldAuditVerifyReqVO')
  const verifyRespType = extractInterface(apiSource, 'EdhrFieldAuditVerifyRespVO')
  const exportReqType = extractInterface(apiSource, 'EdhrFieldAuditExportReqVO')
  const exportRespType = extractInterface(apiSource, 'EdhrFieldAuditExportRespVO')

  assert.deepEqual(pageRespFields.sort(), ['list', 'total'].sort(), '分页响应顶层必须只有 list/total')

  for (const field of [
    'executionId',
    'auditBatchId',
    'fieldPath',
    'fieldKey',
    'actorId',
    'actorName',
    'reasonCategory',
    'reasonKeyword',
    'changedAtStart',
    'changedAtEnd'
  ]) {
    assert.match(pageReqType, new RegExp(`\\b${field}\\b`), `分页查询请求必须支持 ${field}`)
  }
  assert.doesNotMatch(
    pageReqType,
    /\bexecutionCode\b|\bworkOrderCode\b|\bbatchCode\b|\brouteProcessId\b|\bprocessName\b|\bworkstationName\b|\bsignatureId\b|\bhashVerificationStatus\b/,
    '分页查询请求不得声明后端不支持的筛选字段'
  )

  const expectedEntryFields = [
    'id',
    'auditBatchId',
    'executionId',
    'executionCode',
    'fieldAuditRevision',
    'fieldPath',
    'fieldKey',
    'fieldLabel',
    'rowIndex',
    'columnIndex',
    'component',
    'valueType',
    'oldValueJson',
    'oldValueDisplay',
    'oldValueHash',
    'newValueJson',
    'newValueDisplay',
    'newValueHash',
    'reasonCategory',
    'reasonText',
    'actorId',
    'actorName',
    'signatureId',
    'previousHash',
    'auditHash',
    'changedAt',
    'hashVerification'
  ]
  assert.deepEqual(entryFields.sort(), expectedEntryFields.sort(), '审计 item row 类型必须精确对齐后端 ItemRespVO')
  for (const field of expectedEntryFields) {
    assert.match(apiSource, new RegExp(`\\b${field}\\b`), `审计条目类型必须声明 ${field}`)
  }

  assert.match(entryType, /\bid:\s*string\b/, '分页 Row 必须使用 string 保存审计 item Long ID，避免 JS Number 精度丢失')
  assert.match(entryType, /\bauditBatchId:\s*string\b/, '分页 Row 必须使用 string 保存 auditBatchId Long ID，避免 JS Number 精度丢失')
  assert.doesNotMatch(entryType, /\bauditId\s*:/, '分页 Row 不得声明 auditId')
  assert.doesNotMatch(
    entryType,
    /\bworkOrderId\b|\bworkOrderCode\b|\bbatchCode\b|\brouteProcessId\b|\bprocessName\b|\bworkstationName\b|\bchainSeq\b|\bactionType\b|\bbaseCellValuesHash\b|\bcellValuesHash\b|\bbaseFieldAuditRevision\b|\bbaseFieldAuditHeadHash\b|\bfieldAuditHeadHash\b|\bsignature\s*[?:]:/,
    '审计 item row 不得声明后端不返回的链序号、动作、批次 hash 或 signature 对象'
  )
  assert.match(detailReqType, /\bexecutionId:\s*number\b/, '详情查询必须包含 executionId')
  assert.match(detailReqType, /\bauditBatchId\?:\s*string\b/, '详情查询必须按 string 透传 auditBatchId Long ID')
  assert.match(detailReqType, /\bauditItemId\?:\s*string\b/, '详情查询必须按 string 透传 auditItemId Long ID')
  assert.match(detailRespType, /\bexecutionId:\s*number\b/, '详情响应必须包含 executionId')
  assert.match(detailRespType, /\bexecutionCode:\s*string\b/, '详情响应必须包含 executionCode')
  assert.match(detailRespType, /\bauditBatch:\s*EdhrFieldAuditBatchVO\b/, '详情响应必须包含结构化 auditBatch')
  assert.match(detailRespType, /\bitems:\s*EdhrFieldAuditEntryVO\[\]/, '详情响应必须包含 items[]')
  assert.match(detailRespType, /\bsignature\?:\s*EdhrFieldAuditSignatureVO\b/, '详情响应必须包含 signature')
  assert.match(detailRespType, /\bhashVerification:\s*EdhrHashVerificationVO\b/, '详情响应必须包含 hashVerification')
  assert.match(apiSource, /getEdhrFieldAuditDetail\s*=\s*async\s*\(\s*params:\s*EdhrFieldAuditDetailReqVO\s*\)/, '详情 helper 入参必须是 executionId + auditBatchId/auditItemId 对象')
  assert.match(apiSource, /getEdhrFieldAuditDetail[\s\S]*params\s*\n/s, '详情 helper 必须原样传递查询对象')
  assert.doesNotMatch(apiSource, /EdhrFieldAuditDetailRespVO\s+extends\s+EdhrFieldAuditEntryVO/, '详情响应不得是 flat entry')
  assert.doesNotMatch(apiSource, /getEdhrFieldAuditDetail\s*=\s*async\s*\(\s*auditId|params:\s*{\s*auditId\s*}|\bauditId\?:\s*number\b|\bauditId:\s*number\b/, '不得使用 auditId 作为详情参数或 Row 字段')
  for (const field of [
    'id',
    'beforeFieldAuditRevision',
    'afterFieldAuditRevision',
    'baseFieldAuditHeadHash',
    'previousHeadHash',
    'newHeadHash',
    'baseCellValuesHash',
    'beforeCellValuesHash',
    'afterCellValuesHash',
    'signatureChallengeHash',
    'signatureProjectionHash'
  ]) {
    assert.match(batchType, new RegExp(`\\b${field}\\b`), `审计批次类型必须声明后端字段 ${field}`)
  }
  assert.match(batchType, /\bid:\s*string\b/, '审计批次 id 必须是 string Long ID')
  assert.doesNotMatch(batchType, /\bauditBatchId\s*:|\bfieldAuditRevision\s*:|\bfieldAuditHeadHash\s*:|\bcellValuesHash\s*:/, '审计批次类型不得使用旧 flat hash/revision 字段名')
  for (const field of ['signatureChallengeHash', 'fieldAuditRevision', 'fieldAuditHeadHash', 'cellValuesHash']) {
    assert.match(signatureType, new RegExp(`\\b${field}\\b`), `签名类型必须声明绑定证据 ${field}`)
  }
  assert.doesNotMatch(signatureType, /\bsignaturePayloadHash\b/, '后端签名响应不返回 signaturePayloadHash')
  assert.match(verifyReqType, /\bexecutionId:\s*number\b/, '校验请求必须必填 executionId')
  for (const field of [
    'fromFieldAuditRevision',
    'toFieldAuditRevision',
    'expectedFieldAuditHeadHash',
    'expectedCellValuesHash',
    'includeBrokenItem'
  ]) {
    assert.match(verifyReqType, new RegExp(`\\b${field}\\b`), `校验请求必须支持 ${field}`)
  }
  assert.doesNotMatch(verifyReqType, /\bfilters\b|\bauditBatchId\b|\bauditItemId\b/, '校验请求不得发送 filters 或详情 ID 参数')
  assert.match(verifyRespType, /\bverifiedCount:\s*number\b/, '校验响应必须使用 verifiedCount')
  assert.doesNotMatch(verifyRespType, /\bcheckedCount\b/, '校验响应不得使用旧 checkedCount')
  assert.match(exportReqType, /\bexecutionId:\s*number\b/, '导出请求必须必填 executionId')
  assert.match(exportReqType, /\bformat:\s*EdhrFieldAuditExportFormat\b/, '导出请求必须必填 format')
  assert.doesNotMatch(exportReqType, /\brequireHashVerification\b/, '后端导出请求不支持 requireHashVerification')
  for (const field of [
    'fileName',
    'contentType',
    'fileSize',
    'sha256',
    'executionId',
    'recordCount',
    'fieldAuditRevision',
    'fieldAuditHeadHash',
    'cellValuesHash',
    'hashVerification',
    'generatedAt',
    'content'
  ]) {
    assert.match(exportRespType, new RegExp(`\\b${field}\\b`), `导出响应必须声明 ${field}`)
  }
  assert.match(exportRespType, /\bcontent:\s*(string\s*\|\s*number\[\]|number\[\]\s*\|\s*string)/, '导出响应必须声明 content(byte[]) 的 JSON 表示')
  assert.doesNotMatch(apiSource, /\bcurrentHash\b|\bpayloadHash\b/, '冻结后端合同只允许 previousHash/auditHash，不得依赖 currentHash 或 payloadHash')
  assert.doesNotMatch(apiSource, /\bsignaturePayloadHash\b|\bfilters:\s*EdhrFieldAuditPageReqVO\b|\brequireHashVerification\b/, '不得声明已废弃的签名、校验或导出字段')
  assert.doesNotMatch(signatureType, /\bactionType\b/, '后端签名响应不返回 actionType，UI 需要固定展示 FIELD_CHANGE')
  assert.match(apiSource, /passwordVerified:\s*boolean/, '字段审计签名必须展示密码校验结果')
  assert.match(apiSource, /export\s+type\s+EdhrFieldAuditExportFormat\s*=\s*'XLSX'/, '导出格式必须至少支持 XLSX')
})
