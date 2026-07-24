import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const apiPath = path.join(root, 'src/api/mes/pro/edhr/fieldAudit.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

const extractTypeBody = (name) => {
  const match = apiSource.match(
    new RegExp(`export\\s+type\\s+${name}\\s*=\\s*([\\s\\S]*?)(?=\\r?\\n\\r?\\nexport\\s)`)
  )
  assert.ok(match, `必须声明封闭类型 ${name}`)
  return match[1].trim()
}

const extractUnionMembers = (name) => {
  const body = extractTypeBody(name)
  const members = [...body.matchAll(/'([^']+)'/g)].map((match) => match[1])
  const residue = body.replace(/'[^']+'/g, '').replace(/\|/g, '').replace(/\s/g, '')
  assert.equal(residue, '', `${name} 只能由字符串字面量组成，不得包含 string fallback`)
  return members
}

const extractInterfaceBody = (name) => {
  const match = apiSource.match(
    new RegExp(`export\\s+interface\\s+${name}(?:\\s+extends\\s+[^\\{]+)?\\s*{([\\s\\S]*?)\\n}`)
  )
  assert.ok(match, `必须声明 DTO ${name}`)
  return match[1]
}

const extractInterfaceFields = (name) => {
  const body = extractInterfaceBody(name)
  const fields = [...body.matchAll(/^\s*([A-Za-z_]\w*)(\?)?:\s*([^\n]+)$/gm)].map(
    ([, fieldName, optional, type]) => ({
      name: fieldName,
      optional: optional === '?',
      type: type.trim()
    })
  )
  assert.ok(fields.length > 0, `${name} 必须包含可解析字段`)
  return fields
}

const assertInterface = (name, expectedFields) => {
  assert.deepEqual(extractInterfaceFields(name), expectedFields, `${name} 字段、可选性和类型必须精确`)
}

const extractFunction = (name) => {
  const match = apiSource.match(
    new RegExp(`export\\s+const\\s+${name}\\s*=\\s*async\\s*\\([\\s\\S]*?\\n}`)
  )
  assert.ok(match, `必须导出请求函数 ${name}`)
  return match[0]
}

const assertGetFunction = (name, reqType, respType, url) => {
  const source = extractFunction(name)
  assert.match(source, new RegExp(`params:\\s*${reqType}`), `${name} 必须使用 ${reqType}`)
  assert.match(source, new RegExp(`request\\.get<${respType}>`), `${name} 必须使用 GET ${respType}`)
  assert.match(source, new RegExp(`url:\\s*'${url.replaceAll('/', '\\/')}'`), `${name} URL 必须冻结`)
  assert.match(source, /\bparams\b/, `${name} 必须原样透传 params`)
  assert.doesNotMatch(source, /request\.(post|put|delete|download)/, `${name} 不得使用非 GET 方法`)
  assert.equal(apiSource.split(url).length - 1, 1, `${url} 必须且只能声明一次`)
}

test('BDD: 责任 API 封闭枚举 -> 四个类型与后端 V1 合同精确一致', () => {
  assert.deepEqual(
    extractUnionMembers('EdhrFieldResponsibilityEvidenceStatus'),
    ['COMPLETE', 'EVIDENCE_MISSING', 'BLOCKED']
  )
  assert.deepEqual(
    extractUnionMembers('EdhrFieldResponsibilityValueOrigin'),
    ['HUMAN', 'SYSTEM_BASELINE', 'EMPTY_UNTOUCHED', 'UNKNOWN']
  )
  assert.deepEqual(
    extractUnionMembers('EdhrFieldResponsibilityContextWarning'),
    ['VERSION_CONTEXT_MISSING']
  )
  assert.deepEqual(
    extractUnionMembers('EdhrFieldResponsibilityReasonCode'),
    [
      'EXECUTION_SNAPSHOT_MISSING',
      'FIELD_DEFINITION_MISSING',
      'BASELINE_MISSING',
      'FIELD_AUDIT_MISSING',
      'SIGNATURE_MISSING',
      'SIGNATURE_INVALID',
      'CHAIN_INVALID',
      'CURRENT_VALUE_MISMATCH',
      'FIELD_IDENTITY_AMBIGUOUS',
      'CROSS_TENANT_ASSOCIATION',
      'CROSS_EXECUTION_ASSOCIATION'
    ]
  )
})

test('BDD: 责任汇总合同 -> 请求、响应和字段责任 item 精确对齐后端 VO', () => {
  assertInterface('EdhrFieldResponsibilitySummaryReqVO', [
    { name: 'executionId', optional: false, type: 'number' },
    { name: 'pageNo', optional: false, type: 'number' },
    { name: 'pageSize', optional: false, type: 'number' },
    { name: 'fieldKeyword', optional: true, type: 'string' },
    {
      name: 'evidenceStatus',
      optional: true,
      type: 'EdhrFieldResponsibilityEvidenceStatus'
    },
    { name: 'valueOrigin', optional: true, type: 'EdhrFieldResponsibilityValueOrigin' },
    { name: 'actorId', optional: true, type: 'number' }
  ])
  assertInterface('EdhrFieldResponsibilitySummaryRespVO', [
    { name: 'executionId', optional: false, type: 'number' },
    { name: 'executionCode', optional: false, type: 'string' },
    { name: 'batchRecordDefinitionId', optional: false, type: 'number' },
    { name: 'batchRecordVersionId', optional: true, type: 'number' },
    { name: 'batchRecordReportId', optional: true, type: 'string' },
    { name: 'fieldAuditRevision', optional: false, type: 'number' },
    { name: 'fieldAuditHeadHash', optional: false, type: 'string' },
    { name: 'cellValuesHash', optional: false, type: 'string' },
    {
      name: 'overallEvidenceStatus',
      optional: false,
      type: 'EdhrFieldResponsibilityEvidenceStatus'
    },
    {
      name: 'overallReasonCodes',
      optional: false,
      type: 'EdhrFieldResponsibilityReasonCode[]'
    },
    {
      name: 'contextWarnings',
      optional: false,
      type: 'EdhrFieldResponsibilityContextWarning[]'
    },
    { name: 'total', optional: false, type: 'number' },
    { name: 'list', optional: false, type: 'EdhrFieldResponsibilityItemRespVO[]' }
  ])
  assertInterface('EdhrFieldResponsibilityItemRespVO', [
    { name: 'fieldPath', optional: false, type: 'string' },
    { name: 'fieldKey', optional: false, type: 'string' },
    { name: 'fieldLabel', optional: false, type: 'string' },
    { name: 'rowIndex', optional: false, type: 'number' },
    { name: 'columnIndex', optional: false, type: 'number' },
    { name: 'component', optional: false, type: 'string' },
    { name: 'valueType', optional: false, type: 'string' },
    { name: 'currentValueJson', optional: false, type: 'string' },
    { name: 'currentValueDisplay', optional: false, type: 'string' },
    { name: 'currentValueHash', optional: false, type: 'string' },
    { name: 'valueOrigin', optional: false, type: 'EdhrFieldResponsibilityValueOrigin' },
    { name: 'firstHumanActorId', optional: true, type: 'number' },
    { name: 'firstHumanActorName', optional: true, type: 'string' },
    { name: 'firstHumanChangedAt', optional: true, type: 'string' },
    { name: 'currentValueActorId', optional: true, type: 'number' },
    { name: 'currentValueActorName', optional: true, type: 'string' },
    { name: 'currentValueChangedAt', optional: true, type: 'string' },
    { name: 'evidenceStatus', optional: false, type: 'EdhrFieldResponsibilityEvidenceStatus' },
    { name: 'reasonCodes', optional: false, type: 'EdhrFieldResponsibilityReasonCode[]' },
    { name: 'historyCount', optional: false, type: 'number' },
    { name: 'latestAuditItemId', optional: true, type: 'string' }
  ])
})

test('BDD: 责任历史合同 -> 完整字段身份和双字段复合游标不可降级', () => {
  assertInterface('EdhrFieldResponsibilityHistoryReqVO', [
    { name: 'executionId', optional: false, type: 'number' },
    { name: 'fieldPath', optional: false, type: 'string' },
    { name: 'fieldKey', optional: false, type: 'string' },
    { name: 'rowIndex', optional: false, type: 'number' },
    { name: 'columnIndex', optional: false, type: 'number' },
    { name: 'pageSize', optional: false, type: 'number' },
    { name: 'cursorFieldAuditRevision', optional: true, type: 'number' },
    { name: 'cursorAuditItemId', optional: true, type: 'string' }
  ])
  assertInterface('EdhrFieldResponsibilityHistoryRespVO', [
    { name: 'executionId', optional: false, type: 'number' },
    { name: 'fieldPath', optional: false, type: 'string' },
    { name: 'fieldKey', optional: false, type: 'string' },
    { name: 'rowIndex', optional: false, type: 'number' },
    { name: 'columnIndex', optional: false, type: 'number' },
    { name: 'list', optional: false, type: 'EdhrFieldResponsibilityHistoryItemRespVO[]' },
    { name: 'hasMore', optional: false, type: 'boolean' },
    { name: 'nextCursorFieldAuditRevision', optional: true, type: 'number' },
    { name: 'nextCursorAuditItemId', optional: true, type: 'string' }
  ])
  assertInterface('EdhrFieldResponsibilityHistoryItemRespVO', [
    { name: 'auditItemId', optional: false, type: 'string' },
    { name: 'auditBatchId', optional: false, type: 'string' },
    { name: 'fieldAuditRevision', optional: false, type: 'number' },
    { name: 'oldValueJson', optional: false, type: 'string' },
    { name: 'oldValueDisplay', optional: false, type: 'string' },
    { name: 'oldValueHash', optional: false, type: 'string' },
    { name: 'newValueJson', optional: false, type: 'string' },
    { name: 'newValueDisplay', optional: false, type: 'string' },
    { name: 'newValueHash', optional: false, type: 'string' },
    { name: 'reasonCategory', optional: false, type: 'string' },
    { name: 'reasonText', optional: false, type: 'string' },
    { name: 'actorId', optional: true, type: 'number' },
    { name: 'actorName', optional: true, type: 'string' },
    { name: 'changedAt', optional: false, type: 'string' },
    { name: 'signatureId', optional: true, type: 'number' },
    { name: 'signatureActorUsernameSnapshot', optional: true, type: 'string' },
    { name: 'signatureActorNicknameSnapshot', optional: true, type: 'string' },
    { name: 'signatureDisplayAt', optional: true, type: 'string' },
    { name: 'signatureProjectionHash', optional: true, type: 'string' },
    { name: 'previousHash', optional: false, type: 'string' },
    { name: 'auditHash', optional: false, type: 'string' },
    { name: 'evidenceStatus', optional: false, type: 'EdhrFieldResponsibilityEvidenceStatus' },
    { name: 'reasonCodes', optional: false, type: 'EdhrFieldResponsibilityReasonCode[]' }
  ])
})

test('BDD: 责任导出合同 -> Base64、摘要哈希和生成元数据精确对齐', () => {
  assertInterface('EdhrFieldResponsibilityExportReqVO', [
    { name: 'executionId', optional: false, type: 'number' },
    { name: 'format', optional: true, type: 'EdhrFieldResponsibilityExportFormat' }
  ])
  assertInterface('EdhrFieldResponsibilityExportRespVO', [
    { name: 'fileName', optional: false, type: 'string' },
    { name: 'format', optional: false, type: 'EdhrFieldResponsibilityExportFormat' },
    { name: 'contentType', optional: false, type: 'string' },
    { name: 'contentBase64', optional: false, type: 'string' },
    { name: 'sha256', optional: false, type: 'string' },
    { name: 'recordCount', optional: false, type: 'number' },
    { name: 'fieldAuditRevision', optional: false, type: 'number' },
    { name: 'fieldAuditHeadHash', optional: false, type: 'string' },
    { name: 'cellValuesHash', optional: false, type: 'string' },
    { name: 'evidenceStatus', optional: false, type: 'EdhrFieldResponsibilityEvidenceStatus' },
    { name: 'reasonCodes', optional: false, type: 'EdhrFieldResponsibilityReasonCode[]' },
    {
      name: 'contextWarnings',
      optional: false,
      type: 'EdhrFieldResponsibilityContextWarning[]'
    },
    { name: 'generatedAt', optional: false, type: 'string' }
  ])
  assert.deepEqual(extractUnionMembers('EdhrFieldResponsibilityExportFormat'), ['XLSX'])
})

test('BDD: 三个责任 helper -> 固定 URL、GET 方法和 DTO 泛型', () => {
  assertGetFunction(
    'getEdhrFieldResponsibilitySummary',
    'EdhrFieldResponsibilitySummaryReqVO',
    'EdhrFieldResponsibilitySummaryRespVO',
    '/mes/pro/batch-record-execution/field-audit/responsibility-summary'
  )
  assertGetFunction(
    'getEdhrFieldResponsibilityHistory',
    'EdhrFieldResponsibilityHistoryReqVO',
    'EdhrFieldResponsibilityHistoryRespVO',
    '/mes/pro/batch-record-execution/field-audit/responsibility-history'
  )
  assertGetFunction(
    'exportEdhrFieldResponsibility',
    'EdhrFieldResponsibilityExportReqVO',
    'EdhrFieldResponsibilityExportRespVO',
    '/mes/pro/batch-record-execution/field-audit/responsibility-export'
  )
})

test('BDD: 原字段审计 API 合同 -> 既有 helper、方法和 endpoint 保持不变', () => {
  const existingContracts = [
    ['saveEdhrFieldChanges', 'put', 'EdhrFieldChangeSaveRespVO', '/mes/pro/batch-record-execution/field-audit/save-changes'],
    ['getEdhrFieldAuditPage', 'get', 'EdhrFieldAuditPageRespVO', '/mes/pro/batch-record-execution/field-audit/page'],
    ['getEdhrFieldAuditDetail', 'get', 'EdhrFieldAuditDetailRespVO', '/mes/pro/batch-record-execution/field-audit/detail'],
    ['verifyEdhrFieldAuditChain', 'post', 'EdhrFieldAuditVerifyRespVO', '/mes/pro/batch-record-execution/field-audit/verify-chain'],
    ['exportEdhrFieldAudit', 'get', 'EdhrFieldAuditExportRespVO', '/mes/pro/batch-record-execution/field-audit/export']
  ]
  for (const [name, method, responseType, url] of existingContracts) {
    const source = extractFunction(name)
    assert.match(source, new RegExp(`request\\.${method}<${responseType}>`), `${name} 方法与响应不得变化`)
    assert.match(source, new RegExp(`url:\\s*'${url.replaceAll('/', '\\/')}'`), `${name} URL 不得变化`)
  }
  assert.match(
    extractInterfaceBody('EdhrFieldAuditExportRespVO'),
    /\bcontent:\s*(string\s*\|\s*number\[\]|number\[\]\s*\|\s*string)/,
    '原字段审计导出 JSON content 合同不得变化'
  )
})

test('BDD: 责任合同 fail closed -> 不允许 any、宽泛枚举或兼容字段绕过', () => {
  assert.doesNotMatch(apiSource, /\bas\s+any\b|\bRecord\s*<\s*string\s*,\s*any\s*>|\bany\b/, '不得使用 any 绕过')
  for (const name of [
    'EdhrFieldResponsibilityEvidenceStatus',
    'EdhrFieldResponsibilityValueOrigin',
    'EdhrFieldResponsibilityContextWarning',
    'EdhrFieldResponsibilityReasonCode'
  ]) {
    assert.doesNotMatch(extractTypeBody(name), /\bstring\b|\bunknown\b/, `${name} 不得降级为 string/unknown`)
  }
  assert.doesNotMatch(
    apiSource,
    /\bresponsibilityStatus\b|\boriginType\b|\bwarningCodes\b|\bcursor\b|\bnextCursor\b|\bcontent\b(?=\s*:\s*string\s*$)/m,
    '不得增加责任合同兼容别名或单字段游标'
  )
})
