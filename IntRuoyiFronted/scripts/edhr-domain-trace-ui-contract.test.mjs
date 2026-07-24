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

const countRoutePathDefinitions = (source, routePath) =>
  Array.from(source.matchAll(new RegExp(`path:\\s*'${routePath.replaceAll('/', '\\/')}'`, 'g'))).length
const extractRouteBlock = (source, routePath) => {
  const marker = `path: '${routePath}'`
  const start = source.indexOf(marker)
  assert.ok(start >= 0, `必须存在路由 ${routePath}`)
  const rest = source.slice(start + marker.length)
  const nextRouteMatch = rest.match(/\r?\n\s*\{\r?\n\s*path:/)
  const end = nextRouteMatch ? start + marker.length + nextRouteMatch.index : source.length
  return source.slice(start, end)
}
const extractDetailItemsTable = (source) => {
  const marker = '<div class="edhr-domain-trace-detail__section-title">追溯明细</div>'
  const start = source.indexOf(marker)
  assert.ok(start >= 0, '必须存在追溯明细表格')
  const rest = source.slice(start)
  const tableEndMatch = rest.match(/\r?\n\s*<\/el-table>/)
  const end = tableEndMatch ? start + tableEndMatch.index + tableEndMatch[0].length : source.length
  return source.slice(start, end)
}

test('BDD: 主数据追溯真实路由 -> 列表和详情页使用真实权限与 Vue 页面', () => {
  const routerSource = readText('src/router/modules/remaining.ts')

  assert.equal(countRoutePathDefinitions(routerSource, 'pro/feedback/edhr-domain-trace'), 1, '必须注册主数据追溯列表路由')
  assert.equal(countRoutePathDefinitions(routerSource, 'pro/feedback/edhr-domain-trace/detail'), 1, '必须注册主数据追溯详情路由')
  assert.match(routerSource, /DomainTracePage\.vue/, '列表路由必须指向 DomainTracePage.vue')
  assert.match(routerSource, /DomainTraceDetailPage\.vue/, '详情路由必须指向 DomainTraceDetailPage.vue')
  assert.match(routerSource, /MesProFeedbackEdhrDomainTrace['"]?/, '必须声明主数据追溯列表路由 name')
  assert.match(routerSource, /MesProFeedbackEdhrDomainTraceDetail['"]?/, '必须声明主数据追溯详情路由 name')
  assert.match(routerSource, /title:\s*'主数据追溯'/, '列表路由标题必须为主数据追溯')
  assert.match(routerSource, /title:\s*'主数据追溯详情'/, '详情路由标题必须为主数据追溯详情')
  assert.match(routerSource, /mes:pro-batch-record-execution:domain-trace-query/, '路由必须使用主数据追溯查询权限')
  assert.doesNotMatch(
    `${extractRouteBlock(routerSource, 'pro/feedback/edhr-domain-trace')}\n${extractRouteBlock(routerSource, 'pro/feedback/edhr-domain-trace/detail')}`,
    /field-audit|tracking|archive|dcc|operate-log/i,
    '主数据追溯路由不得降级到其他页面'
  )
})

test('BDD: 主数据追溯列表页 -> 查询真实分页、展示状态/hash/blockers/items，并跳转详情', () => {
  const page = readText('src/views/mes/pro/edhr/DomainTracePage.vue')

  for (const token of [
    'getEdhrDomainTracePage',
    'EDHR_DOMAIN_TRACE_QUERY_PERMISSION',
    'hasPermission',
    'executionCode',
    'workOrderCode',
    'batchCode',
    'status',
    'VERIFIED',
    'BLOCKED',
    'domainTraceHash',
    'domainTraceSnapshotId',
    'verifiedAt',
    'blockerCount',
    'blockers',
    'items',
    'itemType',
    'itemKey',
    'itemName',
    'sourceId',
    'sourceCode',
    'sourceVersion',
    'snapshotJson',
    'snapshotHash',
    'blockerReason',
    'openDetail',
    "/mes/pro/feedback/edhr-domain-trace/detail"
  ]) {
    assert.match(page, new RegExp(token.replaceAll('/', '\\/')), `列表页必须包含 ${token}`)
  }
  assert.match(page, /当前账号没有主数据追溯查询权限/, '缺查询权限必须显式展示')
  assert.match(page, /resolveTraceStatusType[\s\S]*BLOCKED[\s\S]*danger/s, 'BLOCKED 必须以错误状态展示')
  assert.match(page, /getEdhrDomainTracePage\s*\(\s*buildQuery\(\)\s*\)/, '列表页必须调用真实分页 helper')
  assert.doesNotMatch(page, /mock|fallback|operate-log|dcc|controlled-file|tracking-page|field-audit/i, '列表页不得 mock 或降级复用其他模块')
  assert.doesNotMatch(page, /\btraceType\b|\bsourceTable\b|\bsourceHash\b|\bsnapshotId\b|\bsnapshotVersion\b|\bsourceName\b|\brequiredFlag\b/, '列表页不得依赖后端不返回的旧字段')
})

test('BDD: 主数据追溯详情页 -> 缺 executionId 失败，BLOCKED 展示阻塞项，校验动作走真实 API', () => {
  const detail = readText('src/views/mes/pro/edhr/DomainTraceDetailPage.vue')

  for (const token of [
    'getEdhrDomainTraceDetail',
    'verifyEdhrDomainTrace',
    'EDHR_DOMAIN_TRACE_QUERY_PERMISSION',
    'EDHR_DOMAIN_TRACE_VERIFY_PERMISSION',
    'executionId',
    'status',
    'VERIFIED',
    'BLOCKED',
    'domainTraceHash',
    'expectedDomainTraceHash',
    'verifiedAt',
    'blockers',
    'items',
    'blockerCode',
    'blockerMessage',
    'itemType',
    'itemKey',
    'itemName',
    'sourceId',
    'sourceCode',
    'sourceVersion',
    'snapshotJson',
    'snapshotHash',
    'blockerReason',
    'domainTraceSnapshotId'
  ]) {
    assert.match(detail, new RegExp(token), `详情页必须包含 ${token}`)
  }
  const detailItemsTable = extractDetailItemsTable(detail)
  for (const token of [
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
  ]) {
    assert.match(detailItemsTable, new RegExp(token), `详情 item 表格必须展示 ${token}`)
  }
  assert.doesNotMatch(
    detailItemsTable,
    /\bid\b|domainTraceSnapshotId|verifiedAt|traceType|sourceTable|sourceHash|snapshotId|snapshotVersion|sourceName|requiredFlag/,
    '详情 item 表格不得展示 item 级 id/domainTraceSnapshotId/verifiedAt 空列或旧字段'
  )
  assert.match(detail, /缺少 executionId|缺少执行ID/, '缺 executionId 必须 fail fast')
  assert.match(detail, /主数据追溯校验未通过|阻塞原因|阻塞项/, 'BLOCKED 时必须清晰展示阻塞原因')
  assert.match(detail, /verifyEdhrDomainTrace\s*\(\s*{[\s\S]*executionId:[\s\S]*expectedDomainTraceHash:[\s\S]*domainTraceHash/s, '校验动作必须携带 executionId 与 expectedDomainTraceHash')
  assert.match(detail, /result\.status\s*===\s*'BLOCKED'|result\.status\s*!==\s*'VERIFIED'/, '校验后不得把 BLOCKED 当成功')
  assert.doesNotMatch(detail, /mock|fallback|operate-log|dcc|controlled-file|tracking-page|field-audit/i, '详情页不得 mock 或降级复用其他模块')
  assert.doesNotMatch(detail, /\btraceType\b|\bsourceTable\b|\bsourceHash\b|\bsnapshotId\b|\bsnapshotVersion\b|\bsourceName\b|\brequiredFlag\b/, '详情页不得依赖后端不返回的旧字段')
})

test('BDD: 执行详情真实入口 -> ExecutionPage 提供主数据追溯按钮并跳转详情页', () => {
  const executionPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')

  assert.match(executionPage, /主数据追溯/, '执行详情必须提供主数据追溯入口文案')
  assert.match(executionPage, /EDHR_DOMAIN_TRACE_QUERY_PERMISSION/, '执行详情入口必须复用主数据追溯查询权限')
  assert.match(executionPage, /mes:pro-batch-record-execution:domain-trace-query/, '执行详情入口必须受真实权限约束')
  assert.match(executionPage, /openDomainTracePage/, '执行详情必须声明主数据追溯跳转函数')
  assert.match(executionPage, /path:\s*'\/mes\/pro\/feedback\/edhr-domain-trace\/detail'/, '执行详情入口必须跳转主数据追溯详情真实路由')
  assert.match(executionPage, /executionId:\s*execution\.value\?\.id\s*\?\s*String\(execution\.value\.id\)/, '跳转必须传递真实 executionId')
  assert.doesNotMatch(executionPage, /test-only|测试入口|mock/i, '执行详情不得新增测试专用入口或 mock')
})
