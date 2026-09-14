const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const simulatePage = read('src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue')
const editableForm = read('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const simulateTemplate = simulatePage.split('<script setup')[0]

const assertIncludes = (content, token, message) => {
  assert.ok(content.includes(token), message)
}

const assertNotIncludes = (content, token, message) => {
  assert.ok(!content.includes(token), message)
}

assertNotIncludes(
  simulateTemplate,
  'edhr-batch-template-simulate__title',
  '截图红框中的工序标题不应渲染'
)
assertNotIncludes(
  simulateTemplate,
  'edhr-batch-template-simulate__subtitle',
  '截图红框中的模板名称不应渲染'
)
assertNotIncludes(
  simulateTemplate,
  '<el-descriptions',
  '截图红框中的模板摘要信息不应渲染'
)
assertNotIncludes(simulateTemplate, '模板内填写', '截图红框中的左侧标题不应渲染')
assertNotIncludes(
  simulateTemplate,
  '左侧直接在原模板格内模拟填写。',
  '截图红框中的左侧说明不应渲染'
)

assertIncludes(
  editableForm,
  'showRuleLegend?: boolean',
  '共享可编辑模板组件必须提供显式规则图例展示开关'
)
assertIncludes(
  editableForm,
  'showRuleLegend: true',
  '共享可编辑模板组件必须默认保留规则图例'
)
assertIncludes(
  editableForm,
  'v-if="props.showRuleLegend"',
  '规则图例必须受显式展示开关控制'
)
assertIncludes(
  simulateTemplate,
  ':show-rule-legend="false"',
  '模拟填写页必须仅对当前左侧模板关闭规则图例'
)

assertIncludes(
  simulateTemplate,
  '<el-button link type="primary" @click="handleBack">',
  '返回按钮必须保留'
)
assertIncludes(simulateTemplate, '表单显示', '右侧表单显示标题必须保留')
assertIncludes(
  simulateTemplate,
  '<EdhrExecutionTemplateEditableForm',
  '左侧可编辑模板必须保留'
)
assertIncludes(simulateTemplate, '<EdhrExecutionReadonlyForm', '右侧只读模板必须保留')

console.log('PASS: eDHR batch template simulate red-box sections hidden static contract')

