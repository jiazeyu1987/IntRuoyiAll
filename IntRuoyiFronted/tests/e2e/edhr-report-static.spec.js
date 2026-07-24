const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()

const read = (relativePath) => {
  const absolutePath = path.resolve(root, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertIncludes = (source, fragment, message) => {
  assert(source.includes(fragment), message)
}

const assertNotIncludes = (source, fragment, message) => {
  assert(!source.includes(fragment), message)
}

const apiSource = read('src/api/mes/pro/edhr/report.ts')
const pageSource = read('src/views/mes/pro/edhr-report/ReportPage.vue')
const combinedSource = `${apiSource}\n${pageSource}`

for (const endpoint of [
  '/mes/pro/edhr-report-catalog/page',
  '/mes/pro/edhr-report-catalog/detail',
  '/mes/pro/edhr-report-definition/page',
  '/mes/pro/edhr-report-definition/detail',
  '/mes/pro/edhr-report-query/run',
  '/mes/pro/edhr-report-query/export-audit',
  '/mes/pro/edhr-report-query/export-audit/page'
]) {
  assertIncludes(apiSource, endpoint, `report API must keep endpoint ${endpoint}`)
}

for (const permission of ['mes:pro-edhr-report:query', 'mes:pro-edhr-report:export']) {
  assertIncludes(apiSource, permission, `report API must export permission ${permission}`)
  assertIncludes(pageSource, permission, `report page must gate action with ${permission}`)
}

for (const contract of [
  'EdhrReportCatalogRespVO',
  'EdhrReportDefinitionRespVO',
  'EdhrReportQueryReqVO',
  'EdhrReportQueryRespVO',
  'EdhrReportExportAuditReqVO',
  'getEdhrReportCatalogPage',
  'getEdhrReportCatalogDetail',
  'getEdhrReportDefinitionPage',
  'getEdhrReportDefinitionDetail',
  'runEdhrReportQuery',
  'recordEdhrReportExportAudit',
  'getEdhrReportExportAuditPage'
]) {
  assertIncludes(apiSource, contract, `report API must expose ${contract}`)
}

for (const copy of [
  "defineOptions({ name: 'MesProEdhrReport' })",
  '统一追溯报表',
  '标准报表目录',
  '报表定义',
  '字段口径',
  '只读查询',
  '筛选快照',
  '权限摘要',
  '数据来源',
  '口径版本',
  '导出审计',
  '12 类标准报表'
]) {
  assertIncludes(pageSource, copy, `report page must expose ${copy}`)
}

for (const behavior of [
  'resolveErrorMessage',
  'loadError',
  '<el-alert',
  'handleRunQuery',
  'handleRecordExportAudit',
  'fieldCaliberJson',
  'filterSnapshotJson',
  'permissionSummaryJson',
  'formatDateTime(queryResult.dataUpdatedAt)',
  "import { formatDate } from '@/utils/formatTime'"
]) {
  assertIncludes(pageSource, behavior, `report page must implement visible behavior ${behavior}`)
}

assertIncludes(pageSource, 'empty-text="请选择已发布报表后执行查询"', 'query result table must not show silent success before query')
assertIncludes(pageSource, 'empty-text="暂无导出审计记录"', 'audit table must show Chinese empty state')

for (const forbidden of [
  'edhr-report-dashboard',
  'custom-report',
  'project-package',
  'downloadExport',
  'exportFile',
  'printCount',
  'csvPackage',
  'oqPackage',
  'pqPackage'
]) {
  assertNotIncludes(combinedSource, forbidden, `first report slice must not implement non-goal ${forbidden}`)
}

assert(
  !/catch\s*\(\s*[^)]*\s*\)\s*\{\s*\}/.test(pageSource),
  'report page must not silently swallow frontend errors'
)

console.log('PASS: eDHR report catalog and dialect static contract')
