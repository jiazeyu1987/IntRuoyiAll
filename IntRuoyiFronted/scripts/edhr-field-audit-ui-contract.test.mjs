import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('BDD: 执行详情字段变更留痕 -> pending diff、原因、签名和 hash 基准齐备后才保存', () => {
  const executionPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')

  assert.match(executionPage, /saveEdhrFieldChanges/, '执行详情字段保存必须调用字段审计 save-changes helper')
  assert.doesNotMatch(executionPage, /saveEdhrExecutionDraft\s*\(/, '执行详情不得再用旧 save-draft 保存字段变更')
  assert.doesNotMatch(executionPage, /saveDraftToServer/, '提交前不得通过旧草稿保存静默覆盖 cellValues')

  for (const token of [
    'pendingFieldChanges',
    'fieldPath',
    'component',
    'oldValueJson',
    'newValueJson',
    'oldValueDisplay',
    'newValueDisplay',
    'expectedOldValueJson',
    'reasonCategory',
    'reasonText',
    'baseCellValuesHash',
    'baseFieldAuditRevision',
    'baseFieldAuditHeadHash',
    'hashVerification.status',
    'VALID'
  ]) {
    assert.match(executionPage, new RegExp(token.replace('.', '\\.')), `执行详情必须包含 ${token}`)
  }

  assert.match(executionPage, /signature:\s*{[\s\S]*password:/, '执行详情保存请求必须提交 signature.password')
  assert.match(executionPage, /FIELD_CHANGE/, '签名弹窗必须展示 FIELD_CHANGE 动作')
  assert.match(executionPage, /缺少变更原因|原因不能为空/, '缺原因必须显式阻断保存')
  assert.match(executionPage, /当前状态不允许编辑字段|非草稿/, '非草稿状态必须阻断字段保存')
  assert.match(executionPage, /字段审计链校验未通过|hash 校验/, 'hash 失败必须显式阻断成功态')
  assert.match(executionPage, /buildFieldIdentity/, '执行详情必须使用 fieldPath + fieldKey + rowIndex + columnIndex 生成稳定字段 identity')
  assert.match(executionPage, /fieldIdentity/, 'draft/baseline 状态必须使用稳定字段 identity，而不是只用 fieldKey')
  assert.match(executionPage, /draftFieldValues\[field\.fieldIdentity\]/, '字段控件 v-model 必须按 fieldIdentity 读写')
  assert.doesNotMatch(executionPage, /draftFieldValues\[field\.fieldKey\]|baselineFieldValues\.value\[field\.fieldKey\]|baselineFieldValueHashes\.value\[field\.fieldKey\]/, '重复 fieldKey 会串值，状态索引不得只用 fieldKey')
  assert.match(executionPage, /缺少 fieldKey|fieldKey 缺失/, 'fieldKey 缺失必须 fail fast')
  assert.match(executionPage, /缺少 fieldPath|fieldPath 缺失/, 'fieldPath 缺失必须 fail fast')
  assert.doesNotMatch(executionPage, /field_\$\{rowIndex\}_\$\{columnIndex\}_\$\{index\}|`field_\$\{/, '不得生成 field_... fallback key 继续保存')
  assert.doesNotMatch(executionPage, /Number\.isFinite\(numericValue\)\s*\?\s*numericValue\s*:\s*0/, 'NUMBER 默认值非数字时不得静默变成 0')
})

test('BDD: 字段审计查询详情 -> UI 展示 old/new、原因、人员、签名、hash 状态并可校验导出', () => {
  const fieldAuditPage = readText('src/views/mes/pro/edhr/FieldAuditPage.vue')
  const fieldAuditDetailPage = readText('src/views/mes/pro/edhr/FieldAuditDetailPage.vue')
  const routerSource = readText('src/router/modules/remaining.ts')
  const source = `${fieldAuditPage}\n${fieldAuditDetailPage}`

  assert.match(routerSource, /edhr-field-audit/, '必须提供字段审计链查询路由')
  assert.match(routerSource, /edhr-field-audit\/detail/, '必须提供字段审计详情路由')

  for (const permission of [
    'mes:pro-batch-record-execution:field-audit-query',
    'mes:pro-batch-record-execution:field-audit-verify',
    'mes:pro-batch-record-execution:field-audit-export'
  ]) {
    assert.match(source, new RegExp(permission.replaceAll(':', '\\:')), `字段审计 UI 必须使用权限 ${permission}`)
  }

  for (const token of [
    'getEdhrFieldAuditPage',
    'getEdhrFieldAuditDetail',
    'verifyEdhrFieldAuditChain',
    'exportEdhrFieldAudit',
    'fieldPath',
    'oldValueJson',
    'newValueJson',
    'oldValueDisplay',
    'newValueDisplay',
    'oldValueHash',
    'newValueHash',
    'reasonCategory',
    'reasonKeyword',
    'reasonText',
    'actorName',
    'changedAt',
    'FIELD_CHANGE',
    'signatureId',
    'previousHash',
    'auditHash',
    'hashVerification',
    'calculatedHeadHash',
    'storedHeadHash',
    'checkedBatchCount',
    'checkedItemCount',
    'brokenBatchId',
    'brokenItemId',
    'failedReason',
    'afterFieldAuditRevision',
    'newHeadHash',
    'afterCellValuesHash',
    'signatureChallengeHash',
    'signatureProjectionHash',
    'beforeFieldAuditRevision',
    'previousHeadHash',
    'beforeCellValuesHash',
    'fieldAuditHeadHash',
    'cellValuesHash'
  ]) {
    assert.match(source, new RegExp(token), `字段审计 UI 必须展示或消费 ${token}`)
  }

  assert.match(source, /EDHR_HASH_STATUS_LABEL_MAP|CHAIN_BROKEN|SIGNATURE_MISMATCH|SOURCE_MISSING|CONCURRENCY_CONFLICT/, '必须展示失败 hash 状态')
  assert.doesNotMatch(
    fieldAuditPage,
    /\bqueryParams\.(executionCode|workOrderCode|batchCode|routeProcessId|processName|workstationName|signatureId|hashVerificationStatus)\b|\bhashVerificationStatus\b/,
    '列表页不得暴露或发送后端不支持的筛选字段'
  )
  assert.doesNotMatch(source, /\bchainSeq\b|\brow\.actionType\b|\brow\.signature(?!Id)\b/, '字段审计 UI 不得读取后端 item row 不返回的 chainSeq/actionType/signature')
  assert.doesNotMatch(fieldAuditPage, /\bdata\.hashVerification\b/, '分页响应顶层不返回 hashVerification，列表页不得读取 data.hashVerification')
  assert.match(source, /fieldAuditRevision/, '审计序号必须使用后端返回的 fieldAuditRevision')
  assert.match(source, /verifiedCount/, '校验结果必须展示 verifiedCount')
  assert.doesNotMatch(source, /checkedCount/, '校验结果不得使用旧 checkedCount')
  assert.match(fieldAuditPage, /缺少执行ID|请先输入执行ID|executionId.*校验/s, '列表页校验必须在缺少 executionId 时 fail fast')
  assert.match(fieldAuditPage, /verifyEdhrFieldAuditChain\s*\(\s*{\s*executionId:\s*verifyQuery\.executionId[\s\S]*includeBrokenItem:\s*true/s, '列表页校验必须按后端 VerifyReqVO 发送 executionId/includeBrokenItem')
  assert.doesNotMatch(fieldAuditPage, /verifyEdhrFieldAuditChain\s*\(\s*{\s*filters\b|\bfilters:\s*buildQuery\(\)/, '列表页校验不得发送 filters')
  assert.match(fieldAuditPage, /缺少执行ID[\s\S]*导出|无法导出/s, '列表页导出必须在缺少 executionId 时 fail fast')
  assert.match(fieldAuditPage, /exportEdhrFieldAudit\s*\(\s*{[\s\S]*executionId:\s*exportQuery\.executionId[\s\S]*format:\s*'XLSX'/s, '导出必须发送 executionId 与 format')
  assert.match(fieldAuditPage, /exportPayload\.content/, '导出必须校验服务端返回的 content')
  assert.match(fieldAuditPage, /exportPayload\.fileName/, '导出必须使用服务端返回的 fileName')
  assert.match(fieldAuditPage, /exportPayload\.contentType/, '导出必须使用服务端返回的 contentType')
  assert.match(fieldAuditPage, /new Blob\(\[[\s\S]*exportPayload\.contentType/, '导出必须将服务端 content 转 Blob')
  assert.match(fieldAuditPage, /format:\s*'XLSX'/, '导出必须按后端 ExportReqVO 发送 format')
  assert.doesNotMatch(fieldAuditPage, /\brequireHashVerification\b/, '导出不得发送 requireHashVerification')
  assert.doesNotMatch(fieldAuditPage, /download\.excel\(data|download\.excel\(\s*exportPayload/, '导出 JSON 响应不得当作 Blob 直接下载')
  assert.match(fieldAuditPage, /executionId:\s*String\(row\.executionId\)/, '列表打开详情必须传 executionId')
  assert.match(fieldAuditPage, /auditItemId:\s*String\(row\.id\)/, '列表打开详情必须传 auditItemId: row.id')
  assert.doesNotMatch(fieldAuditPage, /v-model\.number="queryParams\.auditBatchId"|Number\.isFinite\(queryParams\.auditBatchId\)|Number\(route\.query\.auditBatchId\)/, '审计批次 Long ID 筛选不得转为 JS Number')
  assert.match(fieldAuditDetailPage, /route\.query\.executionId/, '详情页必须解析 executionId')
  assert.match(fieldAuditDetailPage, /route\.query\.auditBatchId/, '详情页必须解析 auditBatchId')
  assert.match(fieldAuditDetailPage, /route\.query\.auditItemId/, '详情页必须解析 auditItemId')
  assert.match(fieldAuditDetailPage, /parsePositiveQueryLongId/, '详情页必须按字符串解析审计批次和明细 Long ID')
  assert.doesNotMatch(fieldAuditDetailPage, /const auditBatchId = parsePositiveQueryNumber|const auditItemId = parsePositiveQueryNumber/, '详情页不得把 auditBatchId/auditItemId 转成 JS Number')
  assert.match(fieldAuditDetailPage, /getEdhrFieldAuditDetail\s*\(\s*detailQuery\s*\)/, '详情页必须用结构化查询对象调用 detail helper')
  assert.match(fieldAuditDetailPage, /expectedFieldAuditHeadHash:\s*detail\.value\.auditBatch\?\.newHeadHash/, '详情页校验链必须绑定 expectedFieldAuditHeadHash')
  assert.match(fieldAuditDetailPage, /expectedCellValuesHash:\s*detail\.value\.auditBatch\?\.afterCellValuesHash/, '详情页校验链必须绑定 expectedCellValuesHash')
  assert.match(fieldAuditDetailPage, /detail\.auditBatch/, '详情页必须展示 auditBatch 摘要')
  assert.match(fieldAuditDetailPage, /detail\.items/, '详情页必须展示 items 表格')
  assert.match(fieldAuditDetailPage, /detail\.signature/, '详情页必须展示结构化 signature')
  assert.doesNotMatch(source, /\bauditId\b/, '字段审计详情不得使用 auditId 参数或 row.auditId')
  assert.doesNotMatch(source, /\bcurrentHash\b|\bpayloadHash\b/, '字段审计 UI 必须使用 auditHash/previousHash，不得依赖 currentHash 或 payloadHash')
  assert.doesNotMatch(source, /\bsignaturePayloadHash\b|\bfilters:\s*buildQuery\(\)|\brequireHashVerification\b/, '字段审计 UI 不得使用旧签名、校验或导出字段')
  assert.doesNotMatch(source, /operate-log|tracking-page|dcc|controlled-file/i, '字段审计 UI 不得降级到普通日志、追踪页或 DCC 页面')
})
