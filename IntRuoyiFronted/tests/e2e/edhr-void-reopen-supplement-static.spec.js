const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const changeApi = readSource('src/api/mes/pro/edhr/change.ts')
const batchDetailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const recordChangePage = readSource('src/views/mes/pro/edhr/RecordChangePage.vue')
const remainingRouter = readSource('src/router/modules/remaining.ts')

for (const symbol of [
  'EDHR_CHANGE_TYPE_VOID',
  'EDHR_CHANGE_TYPE_REOPEN',
  'EDHR_CHANGE_TYPE_SUPPLEMENT',
  'requestVoidExecution',
  'approveVoidExecution',
  'requestReopenBatch',
  'approveReopenBatch',
  'requestReopenExecution',
  'approveReopenExecution',
  'requestSupplement',
  'saveSupplementDraft',
  'submitSupplement',
  'approveSupplement',
  'getEdhrRecordChangePage',
  'getEdhrRecordChange'
]) {
  assert.match(changeApi, new RegExp(symbol), `EDHR 变更 API 必须声明 ${symbol}。`)
}

for (const endpoint of [
  '/mes/pro/edhr-change/void-execution/request',
  '/mes/pro/edhr-change/void-execution/approve',
  '/mes/pro/edhr-change/reopen-batch/request',
  '/mes/pro/edhr-change/reopen-batch/approve',
  '/mes/pro/edhr-change/reopen-execution/request',
  '/mes/pro/edhr-change/reopen-execution/approve',
  '/mes/pro/edhr-change/supplement/request',
  '/mes/pro/edhr-change/supplement/save-draft',
  '/mes/pro/edhr-change/supplement/submit',
  '/mes/pro/edhr-change/supplement/approve'
]) {
  assert.match(changeApi, new RegExp(endpoint.replace(/[/-]/g, (match) => `\\${match}`)), `缺少接口 ${endpoint}`)
}

assert.match(changeApi, /reasonCategory\??:\s*string/, '变更申请必须包含原因分类。')
assert.match(changeApi, /reasonText\??:\s*string/, '变更申请必须包含原因说明。')
assert.match(changeApi, /password\??:\s*string/, '变更申请/审批必须包含签名密码。')
assert.match(changeApi, /previousArchiveHash\??:\s*string/, '变更响应必须暴露原归档 hash。')

assert.match(batchDetailPage, /申请重开|requestReopenBatch/, '批次详情页必须提供重开入口或调用。')
assert.doesNotMatch(batchDetailPage, /需按开发计划接入电子签名弹窗后提交/, '批次详情页不得保留重开占位提示。')
assert.match(recordChangePage, /eDHR 变更详情|变更记录/, '变更记录页必须存在。')
assert.match(remainingRouter, /edhr-change/, '前端路由必须注册 eDHR 变更记录页。')

for (const requiredSnippet of [
  'reopenDialogVisible',
  'reopenForm.reasonCategory',
  'reopenForm.reasonText',
  'reopenForm.password',
  'submitReopenBatch',
  'requestReopenBatch'
]) {
  assert.match(batchDetailPage, new RegExp(requiredSnippet.replace(/[.]/g, '\\.')), `批次详情页必须包含 ${requiredSnippet}。`)
}

for (const requiredSnippet of [
  'getEdhrRecordChangePage',
  'getEdhrRecordChange',
  'changeTypeOptions',
  'changeStatusOptions',
  'detailDialogVisible',
  'selectedChange'
]) {
  assert.match(recordChangePage, new RegExp(requiredSnippet.replace(/[.]/g, '\\.')), `变更记录页必须包含 ${requiredSnippet}。`)
}

assert.doesNotMatch(changeApi, /mock|fallback|defaultSuccess|admin123|route\.fulfill/i, '变更 API 不得包含 mock、fallback 或默认密码路径。')

console.log('PASS: eDHR void/reopen/supplement static contract')
