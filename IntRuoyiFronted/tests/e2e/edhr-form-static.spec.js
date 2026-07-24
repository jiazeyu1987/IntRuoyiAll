const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/form.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-form/FormPage.vue')

assert(fs.existsSync(apiPath), 'eDHR独立表单 API 文件必须存在。')
assert(fs.existsSync(pagePath), 'eDHR独立表单页面必须存在。')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const endpoint of [
  '/mes/pro/edhr-form-template/page',
  '/mes/pro/edhr-form-template/create',
  '/mes/pro/edhr-form-template/activate',
  '/mes/pro/edhr-form-instance/page',
  '/mes/pro/edhr-form-instance/get',
  '/mes/pro/edhr-form-instance/create',
  '/mes/pro/edhr-form-instance/save-draft',
  '/mes/pro/edhr-form-instance/submit',
  '/mes/pro/edhr-form-instance/event/page'
]) {
  assert.match(api, new RegExp(endpoint.replaceAll('/', '\\/')), `API 必须声明接口 ${endpoint}`)
}

for (const token of [
  'EdhrFormTemplateRespVO',
  'EdhrFormCreateInstanceReqVO',
  'EdhrFormInstanceRespVO',
  'EdhrFormEventRespVO',
  "status?: 'DRAFT' | 'ACTIVE'",
  "status?: 'DRAFT' | 'SUBMITTED'",
  'createTemplate',
  'activateTemplate',
  'createFormInstance',
  'saveFormInstanceDraft',
  'submitFormInstance',
  'getFormEventPage'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API 类型和方法缺少 ${token}`)
}

for (const label of [
  '独立表单实例',
  '表单模板',
  '模板编码',
  '模板名称',
  '模板版本',
  '字段定义',
  '实例编码',
  '表单状态',
  '保存草稿',
  '提交表单',
  '创建模板',
  '启用',
  '创建实例',
  '事件'
]) {
  assert.ok(page.includes(label), `页面必须呈现业务字段和操作：${label}`)
}

for (const token of [
  'templateList',
  'instanceList',
  'templateDialogVisible',
  'instanceDialogVisible',
  'detailDrawerVisible',
  'eventDrawerVisible',
  'loadTemplateList',
  'loadInstanceList',
  'openTemplateDialog',
  'openInstanceDialog',
  'openDetailDrawer',
  'submitTemplate',
  'submitActivateTemplate',
  'submitCreateInstance',
  'submitSaveDraft',
  'submitForm',
  'getEdhrFormTemplatePage',
  'createEdhrFormTemplate',
  'activateEdhrFormTemplate',
  'getEdhrFormInstancePage',
  'createEdhrFormInstance',
  'saveEdhrFormInstanceDraft',
  'submitEdhrFormInstance',
  'getEdhrFormEventPage',
  "v-hasPermi=\"['mes:pro-edhr-form-template:create']\"",
  "v-hasPermi=\"['mes:pro-edhr-form-template:activate']\"",
  "v-hasPermi=\"['mes:pro-edhr-form-instance:create']\"",
  "v-hasPermi=\"['mes:pro-edhr-form-instance:save']\"",
  "v-hasPermi=\"['mes:pro-edhr-form-instance:submit']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面交互契约缺少 ${token}`)
}

for (const token of [
  'loadError',
  'templateError',
  'instanceError',
  'detailError',
  'message.error(resolveErrorMessage',
  '<el-alert v-if="loadError"',
  '<el-alert v-if="templateError"',
  '<el-alert v-if="instanceError"',
  '<el-alert v-if="detailError"'
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面必须暴露后端失败原因：${token}`)
}

for (const forbidden of [
  '/recordbook',
  '/tag',
  '/approve',
  '/signature',
  '/attachment',
  '/print',
  '电子签名',
  '记录本',
  '受控标签',
  '打印成功'
]) {
  assert.doesNotMatch(api, new RegExp(forbidden, 'i'), `首切片 API 不得提前实现非目标能力：${forbidden}`)
  assert.doesNotMatch(page, new RegExp(forbidden, 'i'), `首切片页面不得提前开放非目标能力：${forbidden}`)
}

assert.doesNotMatch(page, /mock|fixture|demo/i, '独立表单页面不得使用 mock、fixture 或 demo 数据。')

console.log('PASS: eDHR independent form static contract')
