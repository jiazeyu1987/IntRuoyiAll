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
  /const\s+openSelectedTemplate\s*=\s*async\s*\(\)\s*=>[\s\S]+openSelectedTemplateDesigner\('preview'\)/,
  '表单模板“打开”必须复用批记录设计器预览路径'
)
assert.match(
  templatePage,
  /const\s+editSelectedTemplate\s*=\s*async\s*\(\)\s*=>[\s\S]+openSelectedTemplateDesigner\('edit'\)/,
  '表单模板“编辑”必须复用批记录设计器编辑路径'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateFill\s*=\s*async\s*\(\)\s*=>[\s\S]+\/mes\/pro\/feedback\/edhr-batch-execution\/template-simulate/,
  '表单模板“填写”必须跳转批记录模板模拟填写页'
)

assert.match(templatePage, /path:\s*'\/mes\/pro\/batch-record-form-list'/, '打开/编辑必须进入批记录表单页')
assert.match(templatePage, /reportMode:\s*mode/, '打开/编辑必须传递批记录 reportMode')
assert.match(templatePage, /reportId:\s*binding\.batchRecordReportId/, '三按钮必须使用后端返回的稳定 reportId')
assert.match(templatePage, /returnLabel:\s*'返回表单模板'/, '填写页返回标签必须指向表单模板')
assert.match(templatePage, /当前模板未绑定批记录表单/, '缺少 reportId 时必须 fail fast 提示绑定缺失')

const openSelectedTemplateBody = templatePage.match(/const\s+openSelectedTemplate\s*=[\s\S]*?\n}\n/)?.[0] || ''
const editSelectedTemplateBody = templatePage.match(/const\s+editSelectedTemplate\s*=[\s\S]*?\n}\n/)?.[0] || ''
const fillSelectedTemplateBody = templatePage.match(/const\s+openSelectedTemplateFill\s*=[\s\S]*?\n}\n/)?.[0] || ''

assert.doesNotMatch(openSelectedTemplateBody, /templateViewDialogRef\.value\?\.open/, '打开按钮不得再进入 TemplateViewDialog')
assert.doesNotMatch(editSelectedTemplateBody, /openSelectedTemplateAction\('edit'\)/, '编辑按钮不得再进入本页规则弹窗')
assert.doesNotMatch(fillSelectedTemplateBody, /fillDialogVisible\.value\s*=\s*true/, '填写按钮不得再进入本页模拟填写弹窗')

console.log('PASS form-template-batch-record-button-alignment-static')
