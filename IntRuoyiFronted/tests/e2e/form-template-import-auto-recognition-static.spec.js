const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const importDialog = read('src/views/form-center/template/components/TemplateImportDialog.vue')

assert.match(
  importDialog,
  /导入时会自动进行代码规则识别/,
  '导入弹窗必须明确告知用户：Word 导入时会自动进行代码规则识别'
)

assert.match(
  importDialog,
  /const\s+resolveImportSuccessMessage\s*=\s*\(\s*result:\s*TemplateApi\.FormTemplateImportRespVO\s*\)\s*=>/,
  '导入成功提示必须集中按后端返回状态生成，避免 CREATE/UPGRADE 文案和真实状态不一致'
)

assert.match(
  importDialog,
  /result\.status\s*===\s*'PUBLISHED'[\s\S]*?自动发布[\s\S]*?发布版本测试/,
  '后端返回 PUBLISHED 时必须提示已自动发布，可直接使用发布版本测试'
)

assert.match(
  importDialog,
  /result\.status\s*===\s*'PENDING_APPROVAL'[\s\S]*?提交升版审批[\s\S]*?审批通过后自动发布/,
  '后端返回 PENDING_APPROVAL 时必须提示已提交审批，审批通过后自动发布'
)

assert.match(
  importDialog,
  /result\.status\s*===\s*'DRAFT'[\s\S]*?生成草稿/,
  '后端返回 DRAFT 时必须提示草稿状态，不得冒充已发布'
)

assert.match(
  importDialog,
  /message\.success\(\s*resolveImportSuccessMessage\(result\)\s*\)/,
  'submitForm 必须使用状态感知的导入成功提示'
)

assert.doesNotMatch(
  importDialog,
  /autoDetectTemplateFillRules|fill-rule-auto-detect/,
  'Word 导入弹窗不得调用手工规则识别接口作为导入失败 fallback'
)

console.log('PASS form-template-import-auto-recognition-static')
