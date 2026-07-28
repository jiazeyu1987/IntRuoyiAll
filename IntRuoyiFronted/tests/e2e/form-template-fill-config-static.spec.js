const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')
const readIfExists = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.ok(fs.existsSync(absolutePath), `Missing expected file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const templatePage = read('src/views/form-center/template/index.vue')
const templateApi = read('src/api/form-center/template.ts')
const fillConfigDialog = readIfExists(
  'src/views/form-center/template/components/FormTemplateFillConfigDialog.vue'
)

const includes = (content, token, message) => assert.ok(content.includes(token), message)
const notIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

const previewActions =
  templatePage.match(/<div v-if="selectedTemplate" class="form-template-preview__actions">[\s\S]*?<\/div>/)?.[0] ||
  ''

assert.ok(previewActions, '表单中心模板预览区必须保留右侧操作栏。')
assert.match(
  previewActions,
  /openSelectedTemplateFill[\s\S]*?>\s*填写\s*<[\s\S]*?openSelectedTemplateFillConfig[\s\S]*?>\s*填写配置\s*<[\s\S]*?downloadSelectedTemplateSource/s,
  '“填写配置”按钮必须位于“填写”和“下载”之间。'
)
assert.match(
  previewActions,
  /openSelectedTemplateFillConfig[\s\S]*?v-hasPermi="\['form:template:update'\]"/s,
  '“填写配置”按钮必须使用 form:template:update 权限控制。'
)
assert.match(
  previewActions,
  /canUseTemplateInteractiveAction\(selectedTemplate\)[\s\S]*openSelectedTemplateFillConfig/s,
  '作废或审批锁定模板不得显示“填写配置”。'
)

includes(templatePage, 'FormTemplateFillConfigDialog', '页面必须挂载模板自身填写配置弹窗。')
includes(templatePage, 'fillConfigDialogVisible', '页面必须维护填写配置弹窗可见状态。')
includes(templatePage, 'openSelectedTemplateFillConfig', '页面必须提供填写配置打开入口。')
includes(templatePage, 'saveSelectedTemplateFillConfig', '页面必须提供模板自身填写配置保存处理。')
includes(templatePage, 'buildTemplateJimuSchemaPayload', '模板规则保存必须合并既有 jimuSchema 字段。')
includes(templatePage, 'assistRows: parsedTemplateJimuSchema.value?.assistRows', '模板编辑保存不得覆盖已有辅助行。')
includes(templatePage, 'fillAssignments: parsedTemplateJimuSchema.value?.fillAssignments', '模板编辑保存不得覆盖已有辅助行填写人。')

includes(fillConfigDialog, 'title="填写配置"', '模板填写配置弹窗标题必须与批记录填写配置一致。')
includes(fillConfigDialog, 'batch-record-cell-rules-editor', '模板填写配置必须复用批记录式视觉编辑器结构。')
includes(fillConfigDialog, '辅助行配置', '模板填写配置必须提供辅助行配置。')
includes(fillConfigDialog, '辅助行填写人', '模板填写配置必须提供辅助行填写人配置。')
includes(fillConfigDialog, 'getSimpleUserList', '模板填写配置必须复用系统用户候选项。')
includes(fillConfigDialog, 'getSimpleRoleList', '模板填写配置必须复用系统角色候选项。')
includes(fillConfigDialog, 'fillAssignments', '模板填写配置必须读写模板自身 fillAssignments。')
includes(fillConfigDialog, 'assistRows', '模板填写配置必须读写模板自身 assistRows。')
includes(fillConfigDialog, '只有草稿版本可以保存填写配置。', '非草稿模板必须明确只读保存约束。')

for (const source of [templatePage, fillConfigDialog, templateApi]) {
  for (const forbidden of [
    'BatchRecordReportApi',
    'EdhrProcessFormPermissionRuleApi',
    '/mes/pro/batch-record-report/cell-rules',
    '/mes/pro/edhr-process-form-permission-rule/save-by-report',
    'batchRecordReportId',
    'batchRecordBindingStatus',
    '当前模板未绑定批记录表单',
    '/mes/pro/batch-record-form-list'
  ]) {
    notIncludes(source, forbidden, `表单中心填写配置不得依赖批记录链路：${forbidden}`)
  }
}

console.log('PASS form-template-fill-config-static')
