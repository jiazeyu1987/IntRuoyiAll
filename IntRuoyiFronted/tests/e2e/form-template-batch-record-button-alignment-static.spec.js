const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const templatePagePath = path.join(repoRoot, 'src/views/form-center/template/index.vue')
const templateApiPath = path.join(repoRoot, 'src/api/form-center/template.ts')

const templatePage = fs.readFileSync(templatePagePath, 'utf8')
const templateApi = fs.readFileSync(templateApiPath, 'utf8')

for (const field of [
  'batchRecordReportId',
  'batchRecordReportName',
  'batchRecordName',
  'batchRecordVersionNo',
  'batchRecordFormSlotType',
  'batchRecordBindingStatus',
  'batchRecordBindingError'
]) {
  assert.match(templateApi, new RegExp(`${field}\\??:\\s*string`), `FormTemplateListItemVO must expose ${field}`)
}

assert.match(
  templatePage,
  /const\s+openSelectedTemplate\s*=\s*\(\)\s*=>[\s\S]+templateViewDialogRef\.value\?\.open\(selectedTemplate\.value\)/,
  '表单模板“打开”必须进入本页模板查看弹窗，不得依赖批记录绑定'
)
assert.match(
  templatePage,
  /const\s+editSelectedTemplate\s*=\s*async\s*\(\)\s*=>[\s\S]+openSelectedTemplateAction\('edit'\)/,
  '表单模板“编辑”必须进入本页规则编辑流程，不得复用批记录绑定设计器路径'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateFill\s*=\s*\(\)\s*=>[\s\S]+resetTemplateFillValues\(\)[\s\S]+fillDialogVisible\.value\s*=\s*true/,
  '表单模板“填写”必须进入本页模拟填写弹窗，不得依赖批记录绑定'
)

assert.doesNotMatch(templatePage, /path:\s*'\/mes\/pro\/batch-record-form-list'/, '表单模板页不得把打开动作路由到批记录表单页')
assert.doesNotMatch(templatePage, /\/mes\/pro\/feedback\/edhr-batch-execution\/template-simulate/, '表单模板页不得把填写动作路由到批记录模拟填写页')
assert.doesNotMatch(templatePage, /当前模板未绑定批记录表单/, '通用表单模板页不得因缺少批记录绑定拦截打开/填写')
assert.doesNotMatch(templatePage, /resolveSelectedTemplateBatchRecordBinding/, '通用表单模板页不得通过批记录绑定校验驱动打开/填写')

const openSelectedTemplateBody = templatePage.match(/const\s+openSelectedTemplate\s*=[\s\S]*?\n}\n/)?.[0] || ''
const editSelectedTemplateBody = templatePage.match(/const\s+editSelectedTemplate\s*=[\s\S]*?\n}\n/)?.[0] || ''
const fillSelectedTemplateBody = templatePage.match(/const\s+openSelectedTemplateFill\s*=[\s\S]*?\n}\n/)?.[0] || ''

assert.doesNotMatch(openSelectedTemplateBody, /openSelectedTemplateDesigner/, '打开按钮不得进入批记录设计器预览路径')
assert.doesNotMatch(openSelectedTemplateBody, /resolveSelectedTemplateBatchRecordBinding/, '打开按钮不得先校验批记录绑定关系')
assert.doesNotMatch(editSelectedTemplateBody, /openSelectedTemplateDesigner\('edit'\)/, '编辑按钮不得进入批记录设计器编辑路径')
assert.doesNotMatch(editSelectedTemplateBody, /resolveSelectedTemplateBatchRecordBinding/, '编辑按钮不得先校验批记录绑定关系')
assert.doesNotMatch(fillSelectedTemplateBody, /resolveSelectedTemplateBatchRecordBinding/, '填写按钮不得先校验批记录绑定关系')
assert.doesNotMatch(fillSelectedTemplateBody, /router\.push/, '填写按钮不得跳转批记录模拟填写页')

console.log('PASS form-template-batch-record-button-alignment-static')
