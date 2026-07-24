import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.ok(fs.existsSync(absolutePath), `必须存在 ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractInterface = (source, name) => {
  const match = source.match(
    new RegExp(`export\\s+interface\\s+${name}(?:\\s+extends\\s+[^\\{]+)?\\s*{([\\s\\S]*?)\\n}`)
  )
  assert.ok(match, `必须声明 ${name}`)
  return match[1]
}

test('BDD: 主数据追溯 API 合同 -> helper 使用冻结路径、方法、权限和状态枚举', () => {
  const apiSource = readText('src/api/mes/pro/edhr/domainTrace.ts')

  assert.match(apiSource, /const\s+EDHR_DOMAIN_TRACE_BASE_URL\s*=\s*'\/mes\/pro\/batch-record-execution\/domain-trace'/, '必须声明冻结 base path')
  assert.match(apiSource, /export\s+const\s+EDHR_DOMAIN_TRACE_QUERY_PERMISSION\s*=\s*'mes:pro-batch-record-execution:domain-trace-query'/, '必须导出查询权限')
  assert.match(apiSource, /export\s+const\s+EDHR_DOMAIN_TRACE_VERIFY_PERMISSION\s*=\s*'mes:pro-batch-record-execution:domain-trace-verify'/, '必须导出校验权限')
  assert.match(apiSource, /export\s+type\s+EdhrDomainTraceStatus\s*=[\s\S]*'VERIFIED'[\s\S]*'BLOCKED'/s, '状态至少必须包含 VERIFIED/BLOCKED')
  assert.doesNotMatch(apiSource, /mock|fallback|operate-log|dcc|controlled-file|tracking-page|field-audit|archive/i, '主数据追溯 API 不得降级复用其他模块路径或 mock/fallback')

  assert.match(apiSource, /getEdhrDomainTraceDetail\s*=\s*async\s*\(\s*params:\s*EdhrDomainTraceDetailReqVO\s*\)/, '必须暴露详情 helper')
  assert.match(apiSource, /getEdhrDomainTraceDetail[\s\S]*request\.get<EdhrDomainTraceDetailRespVO>[\s\S]*url:\s*`\$\{EDHR_DOMAIN_TRACE_BASE_URL\}\/detail`[\s\S]*params/s, '详情必须 GET /detail 并透传 params')
  assert.match(apiSource, /getEdhrDomainTracePage\s*=\s*async\s*\(\s*params:\s*EdhrDomainTracePageReqVO\s*\)/, '必须暴露分页 helper')
  assert.match(apiSource, /getEdhrDomainTracePage[\s\S]*request\.get<PageResult<EdhrDomainTracePageRowVO\[\]>>[\s\S]*url:\s*`\$\{EDHR_DOMAIN_TRACE_BASE_URL\}\/page`[\s\S]*params/s, '分页必须 GET /page')
  assert.match(apiSource, /verifyEdhrDomainTrace\s*=\s*async\s*\(\s*data:\s*EdhrDomainTraceVerifyReqVO\s*\)/, '必须暴露校验 helper')
  assert.match(apiSource, /verifyEdhrDomainTrace[\s\S]*request\.post<EdhrDomainTraceVerifyRespVO>[\s\S]*url:\s*`\$\{EDHR_DOMAIN_TRACE_BASE_URL\}\/verify`[\s\S]*data/s, '校验必须 POST /verify')
})

test('BDD: 主数据追溯请求响应类型 -> 后端最终字段可追溯且不依赖旧字段', () => {
  const apiSource = readText('src/api/mes/pro/edhr/domainTrace.ts')
  const detailReq = extractInterface(apiSource, 'EdhrDomainTraceDetailReqVO')
  const pageReq = extractInterface(apiSource, 'EdhrDomainTracePageReqVO')
  const row = extractInterface(apiSource, 'EdhrDomainTracePageRowVO')
  const detailResp = extractInterface(apiSource, 'EdhrDomainTraceDetailRespVO')
  const verifyReq = extractInterface(apiSource, 'EdhrDomainTraceVerifyReqVO')
  const verifyResp = extractInterface(apiSource, 'EdhrDomainTraceVerifyRespVO')
  const blocker = extractInterface(apiSource, 'EdhrDomainTraceBlockerVO')
  const item = extractInterface(apiSource, 'EdhrDomainTraceItemVO')

  assert.match(detailReq, /\bexecutionId:\s*number\b/, '详情查询必须必填 executionId')
  for (const field of ['executionCode', 'workOrderCode', 'batchCode', 'status', 'verifiedAtStart', 'verifiedAtEnd']) {
    assert.match(pageReq, new RegExp(`\\b${field}\\??:`), `分页请求必须支持 ${field}`)
  }
  assert.doesNotMatch(pageReq, /\bfilters\b|\bignoreBlockers\b|\bfallback\b/, '分页请求不得发送 filters、忽略阻塞或 fallback 字段')

  for (const field of [
    'executionId',
    'executionCode',
    'workOrderCode',
    'batchCode',
    'status',
    'domainTraceHash',
    'domainTraceSnapshotId',
    'verifiedAt',
    'blockerCount'
  ]) {
    assert.match(row, new RegExp(`\\b${field}\\??:`), `分页行必须声明 ${field}`)
  }
  assert.match(apiSource, /interface\s+EdhrDomainTraceDetailRespVO\s+extends\s+EdhrDomainTracePageRowVO/, '详情响应必须继承分页行的 executionId、executionCode、status、domainTraceHash 与 verifiedAt')
  for (const field of ['blockers', 'items']) {
    assert.match(detailResp, new RegExp(`\\b${field}\\??:`), `详情响应必须声明 ${field}`)
  }
  assert.match(detailResp, /\bblockers:\s*EdhrDomainTraceBlockerVO\[\]/, '详情响应必须包含 blockers[]')
  assert.match(detailResp, /\bitems:\s*EdhrDomainTraceItemVO\[\]/, '详情响应必须包含 items[]')

  for (const field of ['itemType', 'itemKey', 'blockerCode', 'blockerMessage']) {
    assert.match(blocker, new RegExp(`\\b${field}\\??:`), `阻塞项必须声明 ${field}`)
  }
  assert.doesNotMatch(blocker, /\btraceType\b|\bsourceTable\b|\bsourceHash\b|\bsourceVersion\b|\bsnapshotId\b|\bsnapshotVersion\b|\bsourceName\b|\brequiredFlag\b/, '阻塞项不得声明后端不返回的旧字段')
  const allowedItemFields = [
    'itemType',
    'itemKey',
    'itemName',
    'sourceId',
    'sourceCode',
    'sourceVersion',
    'snapshotJson',
    'snapshotHash',
    'status',
    'blockerReason'
  ]
  for (const field of allowedItemFields) {
    assert.match(item, new RegExp(`\\b${field}\\??:`), `追溯明细必须声明 ${field}`)
  }
  assert.doesNotMatch(
    item,
    /\bid\b|\bdomainTraceSnapshotId\b|\bverifiedAt\b|\btraceType\b|\bsourceTable\b|\bsourceHash\b|\bsnapshotId\b|\bsnapshotVersion\b|\bsourceName\b|\brequiredFlag\b|\bblockerCode\b|\bblockerMessage\b/,
    '追溯明细只能声明后端真实 item 字段，不得声明 id/domainTraceSnapshotId/verifiedAt/旧字段或把 blocker 字段混入 item'
  )

  assert.match(verifyReq, /\bexecutionId:\s*number\b/, '校验请求必须必填 executionId')
  assert.match(verifyReq, /\bexpectedDomainTraceHash\?:\s*string\b/, '校验请求必须支持 expectedDomainTraceHash')
  assert.doesNotMatch(verifyReq, /\bforce\b|\bignore\b|\bmock\b|\bfallback\b/, '校验请求不得声明绕过字段')
  assert.match(apiSource, /interface\s+EdhrDomainTraceVerifyRespVO\s+extends\s+EdhrDomainTraceDetailRespVO/, '校验响应必须继承详情响应的 executionId、status、domainTraceHash、verifiedAt、blockers 与 items')
  assert.ok(verifyResp != null, '必须声明校验响应类型')
  assert.doesNotMatch(apiSource, /\btraceType\b|\bsourceTable\b|\bsourceHash\b|\bsnapshotId\b|\bsnapshotVersion\b|\bsourceName\b|\brequiredFlag\b/, 'API helper 不得依赖后端不返回的旧字段')
})
