const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/labelPrint.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue')

assert(fs.existsSync(apiPath), 'eDHR 标签与打印 API 文件必须存在。')
assert(fs.existsSync(pagePath), 'eDHR 标签与打印页面必须存在。')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const endpoint of [
  '/mes/pro/edhr-label-template/page',
  '/mes/pro/edhr-label-template/create',
  '/mes/pro/edhr-label-template/activate',
  '/mes/pro/edhr-label/page',
  '/mes/pro/edhr-label/preview',
  '/mes/pro/edhr-print-task/page',
  '/mes/pro/edhr-print-task/create',
  '/mes/pro/edhr-print-task/mark-failed',
  '/mes/pro/edhr-print-task/confirm'
]) {
  assert.match(api, new RegExp(endpoint.replaceAll('/', '\\/')), `API 必须声明接口 ${endpoint}`)
}

for (const token of [
  'EdhrLabelTemplateRespVO',
  'EdhrLabelInstanceRespVO',
  'EdhrPrintTaskRespVO',
  'EdhrPrintTaskCreateReqVO',
  'EdhrPrintTaskMarkFailedReqVO',
  'EdhrPrintTaskConfirmReqVO',
  "status?: 'WAITING' | 'PRINTING' | 'PENDING_CONFIRM' | 'SUCCESS_CONFIRMED' | 'FAILED' | 'VOID_RESTRICTED'",
  'printConfirmStatus',
  'reprintReason',
  'failureReason',
  'printCountDeducted',
  'previewLabel',
  'createPrintTask',
  'markPrintTaskFailed',
  'confirmPrintTask'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API 类型和方法缺少 ${token}`)
}

for (const label of [
  '标签模板',
  '标签实例',
  '打印任务',
  '模板编码',
  '模板版本',
  '业务对象',
  '字段模型',
  '解析版本',
  '渲染快照',
  '打印状态',
  '确认状态',
  '补打原因',
  '失败原因',
  '作废受限',
  '待确认',
  '标记失败',
  '确认成功'
]) {
  assert.ok(page.includes(label), `页面必须呈现标签和打印业务字段：${label}`)
}

for (const token of [
  'labelTemplateList',
  'labelInstanceList',
  'printTaskList',
  'printTaskDialogVisible',
  'failureDialogVisible',
  'confirmDialogVisible',
  'loadLabelTemplateList',
  'loadLabelInstanceList',
  'loadPrintTaskList',
  'openPrintTaskDialog',
  'submitPrintTask',
  'validateReprintReason',
  'validateOriginalPrintTask',
  '原打印任务ID',
  '请输入原打印任务ID',
  '补打时必须填写补打原因',
  '补打时必须选择原打印任务',
  'openMarkFailedDialog',
  'submitMarkFailed',
  'openConfirmDialog',
  'submitConfirmPrintTask',
  "v-hasPermi=\"['mes:pro-edhr-label-template:create']\"",
  "v-hasPermi=\"['mes:pro-edhr-label:preview']\"",
  "v-hasPermi=\"['mes:pro-edhr-print-task:create']\"",
  "v-hasPermi=\"['mes:pro-edhr-print-task:mark-failed']\"",
  "v-hasPermi=\"['mes:pro-edhr-print-task:confirm']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面交互契约缺少 ${token}`)
}

for (const token of [
  'loadError',
  'labelError',
  'printTaskError',
  'failureError',
  'confirmError',
  'message.error(resolveErrorMessage',
  '<el-alert v-if="loadError"',
  '<el-alert v-if="labelError"',
  '<el-alert v-if="printTaskError"',
  '<el-alert v-if="failureError"',
  '<el-alert v-if="confirmError"'
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面必须暴露后端失败原因：${token}`)
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
  assert.doesNotMatch(api, new RegExp(forbidden, 'i'), `API 不得伪造打印成功或吞错：${forbidden}`)
  assert.doesNotMatch(page, new RegExp(forbidden, 'i'), `页面不得伪造打印成功或吞错：${forbidden}`)
}

console.log('PASS: eDHR label print queue static contract')
