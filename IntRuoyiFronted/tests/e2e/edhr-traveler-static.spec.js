const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/traveler.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-traveler/TravelerPage.vue')

assert(fs.existsSync(apiPath), 'eDHR流转单 API 文件必须存在。')
assert(fs.existsSync(pagePath), 'eDHR流转单页面必须存在。')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const endpoint of [
  '/mes/pro/edhr-traveler-template/page',
  '/mes/pro/edhr-traveler-template/create',
  '/mes/pro/edhr-traveler-template/activate',
  '/mes/pro/edhr-traveler/page',
  '/mes/pro/edhr-traveler/get',
  '/mes/pro/edhr-traveler/generate',
  '/mes/pro/edhr-traveler/event/page'
]) {
  assert.match(api, new RegExp(endpoint.replaceAll('/', '\\/')), `API 必须声明接口 ${endpoint}`)
}

for (const token of [
  'EdhrTravelerTemplateRespVO',
  'EdhrTravelerGenerateReqVO',
  'EdhrTravelerRespVO',
  'EdhrTravelerEventRespVO',
  "scopeType?: 'BATCH_LEVEL' | 'SN_LEVEL'",
  "printStatus?: 'NOT_PRINTED' | 'QUEUED'",
  'createTemplate',
  'activateTemplate',
  'generateTraveler',
  'getTravelerEventPage'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API 类型和方法缺少 ${token}`)
}

for (const label of [
  '模板编码',
  '模板名称',
  '模板版本',
  '模板状态',
  '流转单编码',
  '批次执行',
  '工单',
  '批次',
  'SN',
  '工序',
  '打印状态',
  '生成流转单',
  '创建模板',
  '启用',
  '事件'
]) {
  assert.ok(page.includes(label), `页面必须呈现业务字段和操作：${label}`)
}

for (const token of [
  'templateList',
  'travelerList',
  'generateDialogVisible',
  'templateDialogVisible',
  'eventDrawerVisible',
  'loadTemplateList',
  'loadTravelerList',
  'openGenerateDialog',
  'submitGenerate',
  'openEventDrawer',
  'getEdhrTravelerTemplatePage',
  'createEdhrTravelerTemplate',
  'activateEdhrTravelerTemplate',
  'getEdhrTravelerPage',
  'generateEdhrTraveler',
  'getEdhrTravelerEventPage',
  "v-hasPermi=\"['mes:pro-edhr-traveler-template:create']\"",
  "v-hasPermi=\"['mes:pro-edhr-traveler-template:activate']\"",
  "v-hasPermi=\"['mes:pro-edhr-traveler:generate']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面交互契约缺少 ${token}`)
}

for (const token of [
  'loadError',
  'generateError',
  'templateError',
  'message.error(resolveErrorMessage',
  '<el-alert v-if="loadError"',
  '<el-alert v-if="generateError"',
  '<el-alert v-if="templateError"'
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面必须暴露后端失败原因：${token}`)
}

for (const forbidden of [
  '/print',
  '/reprint',
  'PRINT_SUCCESS',
  'printSuccess',
  '扣减',
  '补打',
  '打印成功'
]) {
  assert.doesNotMatch(api, new RegExp(forbidden, 'i'), `首切片 API 不得提前实现打印执行：${forbidden}`)
  assert.doesNotMatch(page, new RegExp(forbidden, 'i'), `首切片页面不得提前开放打印执行：${forbidden}`)
}

assert.doesNotMatch(page, /mock|fixture|demo/i, '流转单页面不得使用 mock、fixture 或 demo 数据。')

console.log('PASS: eDHR traveler static contract')
