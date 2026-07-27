const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const templatePage = fs.readFileSync(
  path.join(repoRoot, 'src/views/form-center/template/index.vue'),
  'utf8'
)
const templateApi = fs.readFileSync(
  path.join(repoRoot, 'src/api/form-center/template.ts'),
  'utf8'
)

const openSelectedTemplateBody =
  templatePage.match(/const\s+openSelectedTemplate\s*=[\s\S]*?\n}\n/)?.[0] || ''
const editSelectedTemplateBody =
  templatePage.match(/const\s+editSelectedTemplate\s*=[\s\S]*?\n}\n/)?.[0] || ''
const fillSelectedTemplateBody =
  templatePage.match(/const\s+openSelectedTemplateFill\s*=[\s\S]*?\n}\n/)?.[0] || ''

assert.match(
  openSelectedTemplateBody,
  /templateViewDialogRef\.value\?\.open\(selectedTemplate\.value\)/,
  '表单模板“打开”必须查看当前模板自身内容'
)
assert.match(
  editSelectedTemplateBody,
  /openSelectedTemplateAction\('edit'\)/,
  '表单模板“编辑”必须进入当前模板自身规则编辑工作区'
)
assert.match(
  fillSelectedTemplateBody,
  /resetTemplateFillValues\(\)[\s\S]*fillDialogVisible\.value\s*=\s*true/,
  '表单模板“填写”必须打开当前模板自身模拟填写工作区'
)

for (const forbidden of [
  'resolveSelectedTemplateBatchRecordBinding',
  'openSelectedTemplateDesigner',
  '当前模板未绑定批记录表单',
  "/mes/pro/batch-record-form-list",
  "/mes/pro/feedback/edhr-batch-execution/template-simulate"
]) {
  assert.doesNotMatch(templatePage, new RegExp(forbidden), `表单模板三按钮不得依赖批记录链路：${forbidden}`)
}

for (const field of [
  'batchRecordReportId',
  'batchRecordReportName',
  'batchRecordName',
  'batchRecordVersionNo',
  'batchRecordFormSlotType',
  'batchRecordBindingStatus',
  'batchRecordBindingError'
]) {
  assert.doesNotMatch(templateApi, new RegExp(`\\b${field}\\??:`), `表单模板 API 类型不得包含 ${field}`)
}

assert.match(templatePage, /<TemplateViewDialog\s+ref="templateViewDialogRef"\s*\/>/)
assert.match(templatePage, /v-model="rulesDialogVisible"/)
assert.match(templatePage, /v-model="fillDialogVisible"/)

console.log('PASS form-template-independent-button-actions-static')
