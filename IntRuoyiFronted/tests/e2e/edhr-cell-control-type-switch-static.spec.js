const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const rules = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const editableForm = read('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const api = read('src/api/mes/pro/batchrecordreport/index.ts')

assert.match(
  dialog,
  /\{\s*label:\s*'下拉框 select',\s*value:\s*'select'\s*\}/,
  '单元格规则弹窗必须提供下拉框 select 控件类型。'
)

assert.match(
  dialog,
  /label="下拉选项"[\s\S]*selectedRuleSelectOptionsText/,
  '单元格规则弹窗必须允许在右侧配置下拉选项内容。'
)

assert.match(
  dialog,
  /constraints\.selectionMode\s*=\s*'single'[\s\S]*constraints\.options/,
  '保存下拉框规则前必须把 selectionMode=single 和 options 写入 constraints。'
)

assert.match(
  dialog,
  /validateSelectOptionsBeforeSave/,
  '保存前必须校验下拉框至少两个有效选项，不能用空下拉掩盖配置缺失。'
)

assert.match(
  dialog,
  /valueType\s*===\s*'SIGNATURE'[\s\S]*电子签名/,
  '切换电子签名时必须给出签名配置提示，不能静默保存为普通文本。'
)

assert.match(
  rules,
  /type TemplateSimulationComponentKind =[\s\S]*\|\s*'select'/,
  '模板规则上下文必须包含 select 组件类型，模拟填写才能渲染下拉框。'
)

assert.match(
  editableForm,
  /componentKind === 'select'[\s\S]*<el-select[\s\S]*<el-option/,
  '模板内可编辑组件必须对 select 规则渲染 Element Plus 下拉框。'
)

assert.match(
  api,
  /export interface BatchRecordReportCellRuleConstraints[\s\S]*options\?:/,
  '前端 cell rule constraints 类型必须显式暴露 options。'
)

console.log('PASS: eDHR cell control type switch static contract')
