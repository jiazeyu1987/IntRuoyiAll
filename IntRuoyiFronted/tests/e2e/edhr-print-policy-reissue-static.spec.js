const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/labelPrint.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue')

assert(fs.existsSync(apiPath), 'eDHR label/print API file must exist.')
assert(fs.existsSync(pagePath), 'eDHR label/print page must exist.')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const endpoint of [
  '/mes/pro/edhr-print-policy/page',
  '/mes/pro/edhr-print-policy/create',
  '/mes/pro/edhr-print-policy/activate',
  '/mes/pro/edhr-print-task/reprint/apply',
  '/mes/pro/edhr-print-task/history-copy',
  '/mes/pro/edhr-print-task/export-history'
]) {
  assert.match(api, new RegExp(endpoint.replaceAll('/', '\\/')), `API must declare endpoint ${endpoint}`)
}

for (const token of [
  'EdhrPrintPolicyRespVO',
  'EdhrPrintPolicyCreateReqVO',
  'EdhrReprintApplyReqVO',
  'EdhrReprintRequestRespVO',
  'EdhrPrintHistoryCopyReqVO',
  'EdhrPrintHistoryExportReqVO',
  'EdhrPrintExportAuditRespVO',
  "status?: 'DRAFT' | 'ACTIVE' | 'DISABLED'",
  'firstPrintLimit',
  'reprintLimit',
  'reasonDictJson',
  'voidCopyWatermark',
  'reprintReasonCode',
  'usedReprintCount',
  'filterSnapshotJson',
  'evidenceHash',
  'getEdhrPrintPolicyPage',
  'createEdhrPrintPolicy',
  'activateEdhrPrintPolicy',
  'applyReprint',
  'createVoidHistoryCopy',
  'exportPrintHistory'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API contract is missing ${token}`)
}

for (const label of [
  '打印策略',
  '策略编码',
  '首次次数',
  '补打上限',
  '原因字典',
  '水印模板',
  '作废水印',
  '补打申请',
  '原因编码',
  '已用次数',
  '作废历史副本',
  '仅历史追溯',
  '不可用于生产流转',
  '导出历史',
  '筛选快照',
  '证据Hash'
]) {
  assert.ok(page.includes(label), `page must render policy/reprint/void/export label: ${label}`)
}

for (const token of [
  'printPolicyList',
  'printPolicyDialogVisible',
  'reprintDialogVisible',
  'historyCopyDialogVisible',
  'exportDialogVisible',
  'loadPrintPolicyList',
  'openPrintPolicyDialog',
  'submitPrintPolicy',
  'activatePrintPolicy',
  'openReprintDialog',
  'submitReprint',
  'validateReprintPolicy',
  'openHistoryCopyDialog',
  'submitHistoryCopy',
  'openExportDialog',
  'submitExportHistory',
  "v-hasPermi=\"['mes:pro-edhr-print-policy:create']\"",
  "v-hasPermi=\"['mes:pro-edhr-print-policy:activate']\"",
  "v-hasPermi=\"['mes:pro-edhr-print-task:reprint']\"",
  "v-hasPermi=\"['mes:pro-edhr-print-task:history-copy']\"",
  "v-hasPermi=\"['mes:pro-edhr-print-task:export']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `page interaction contract is missing ${token}`)
}

for (const token of [
  'printPolicyError',
  'reprintError',
  'historyCopyError',
  'exportError',
  'message.error(resolveErrorMessage',
  '<el-alert v-if="printPolicyError"',
  '<el-alert v-if="reprintError"',
  '<el-alert v-if="historyCopyError"',
  '<el-alert v-if="exportError"'
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `page must expose backend failure reason: ${token}`)
}

for (const forbidden of [
  'window.print',
  'mock',
  'fixture',
  'demo',
  'DEFAULT_SUCCESS',
  'silent',
  'catch {}',
  'catch{}'
]) {
  assert.doesNotMatch(api, new RegExp(forbidden, 'i'), `API must not fake print success or swallow errors: ${forbidden}`)
  assert.doesNotMatch(page, new RegExp(forbidden, 'i'), `page must not fake print success or swallow errors: ${forbidden}`)
}

console.log('PASS: eDHR print policy reissue/void static contract')
