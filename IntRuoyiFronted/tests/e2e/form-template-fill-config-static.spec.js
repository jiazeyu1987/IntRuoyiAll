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
const loadVueCompilerSfc = () => {
  const pluginVuePath = require.resolve('@vitejs/plugin-vue', { paths: [root] })
  const compilerSfcPath = require.resolve('@vue/compiler-sfc', {
    paths: [path.dirname(pluginVuePath)]
  })
  return require(compilerSfcPath)
}
const formatVueCompilerErrors = (errors) =>
  errors
    .map((error) => {
      const location =
        error.loc?.start?.line != null ? `:${error.loc.start.line}:${error.loc.start.column}` : ''
      return `${error.message || String(error)}${location}`
    })
    .join('\n')
const assertVueTemplateCompiles = (relativePath, content) => {
  const { compileTemplate, parse } = loadVueCompilerSfc()
  const parsed = parse(content, { filename: relativePath })
  assert.strictEqual(
    parsed.errors.length,
    0,
    `${relativePath} must be parseable by Vue SFC compiler:\n${formatVueCompilerErrors(parsed.errors)}`
  )
  assert.ok(parsed.descriptor.template, `${relativePath} must keep a template block.`)
  const compiled = compileTemplate({
    source: parsed.descriptor.template.content,
    filename: relativePath,
    id: 'data-v-form-template-fill-config-static'
  })
  assert.strictEqual(
    compiled.errors.length,
    0,
    `${relativePath} template must compile without missing end tags:\n${formatVueCompilerErrors(compiled.errors)}`
  )
}

const previewActions =
  templatePage.match(/<div v-if="selectedTemplate" class="form-template-preview__actions">[\s\S]*?<\/div>/)?.[0] ||
  ''

assert.ok(previewActions, '表单中心模板预览区必须保留右侧操作栏。')
assertVueTemplateCompiles(
  'src/views/form-center/template/components/FormTemplateFillConfigDialog.vue',
  fillConfigDialog
)
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
includes(templatePage, 'fillConfigOpening', '正式版本进入填写配置时必须有明确加载状态，不能表现为点击无反应。')
includes(templatePage, 'saveSelectedTemplateFillConfig', '页面必须提供模板自身填写配置保存处理。')
includes(templatePage, 'buildTemplateJimuSchemaPayload', '模板规则保存必须合并既有 jimuSchema 字段。')
includes(templatePage, 'assistRows: parsedTemplateJimuSchema.value?.assistRows', '模板编辑保存不得覆盖已有辅助行。')
includes(templatePage, 'fillAssignments: parsedTemplateJimuSchema.value?.fillAssignments', '模板编辑保存不得覆盖已有辅助行填写人。')

includes(fillConfigDialog, "title: props.title || '填写配置'", '模板填写配置弹窗标题必须默认与批记录填写配置一致。')
includes(fillConfigDialog, 'fullscreen: true', '模板填写配置弹窗右上角必须显示最大化/恢复按钮。')
includes(fillConfigDialog, 'defaultFullscreen: true', '模板填写配置弹窗必须默认最大化打开。')
includes(fillConfigDialog, 'batch-record-cell-rules-editor__main-panel', '模板填写配置必须把表格预览放入左侧黄框主区域。')
includes(fillConfigDialog, 'data-fill-config-panel="template-config-sidebar"', '模板填写配置必须把字段、辅助行和操作按钮集中到右侧蓝框侧栏。')
includes(fillConfigDialog, 'batch-record-cell-rules-editor__side-scroll', '右侧蓝框侧栏必须独立滚动，避免挤压左侧表格。')
includes(fillConfigDialog, 'batch-record-cell-rules-editor__side-actions', '关闭、重新读取、保存填写配置按钮必须位于右侧蓝框底部。')
notIncludes(fillConfigDialog, '<template #footer>', '模板填写配置不能再使用全宽弹窗 footer，按钮必须收进右侧蓝框。')
includes(fillConfigDialog, 'batch-record-cell-rules-editor', '模板填写配置必须复用批记录式视觉编辑器结构。')
includes(fillConfigDialog, '辅助行配置', '模板填写配置必须提供辅助行配置。')
includes(fillConfigDialog, '辅助行填写人', '模板填写配置必须提供辅助行填写人配置。')
includes(fillConfigDialog, 'getSimpleUserList', '模板填写配置必须复用系统用户候选项。')
includes(fillConfigDialog, 'getSimpleRoleList', '模板填写配置必须复用系统角色候选项。')
includes(fillConfigDialog, 'fillAssignments', '模板填写配置必须读写模板自身 fillAssignments。')
includes(fillConfigDialog, 'assistRows', '模板填写配置必须读写模板自身 assistRows。')
includes(fillConfigDialog, '只有草稿版本可以保存填写配置。', '非草稿模板必须明确只读保存约束。')
includes(fillConfigDialog, '复选框组 radio-group', '右侧控件类型必须支持复选框组。')
includes(fillConfigDialog, '单选组 option-group', '右侧控件类型必须支持单选组。')
includes(fillConfigDialog, '下拉选择 select', '右侧控件类型必须支持下拉选择。')
includes(fillConfigDialog, '电子签名 signature', '右侧控件类型必须支持电子签名。')
includes(fillConfigDialog, '多行文本 textarea', '右侧控件类型必须支持多行文本。')
includes(fillConfigDialog, '文件上传 upload-file', '右侧控件类型必须支持文件上传。')
includes(fillConfigDialog, '图片上传 upload-image', '右侧控件类型必须支持图片上传。')
includes(fillConfigDialog, '多图片上传 upload-images', '右侧控件类型必须支持多图片上传。')
includes(templateApi, "'SIGNATURE'", '代码规则识别候选字段类型必须允许电子签名。')
includes(templateApi, "'radio-group'", '代码规则识别候选控件类型必须允许单选组。')
includes(templateApi, "'signature'", '代码规则识别候选控件类型必须允许电子签名。')
includes(templateApi, "'upload-images'", '代码规则识别候选控件类型必须允许多图片上传。')
notIncludes(fillConfigDialog, 'AI 自动识别', '填写配置页面不应再以 AI 命名规则识别入口。')
notIncludes(fillConfigDialog, 'AI 填写规则识别', '填写配置页面不应再以 AI 命名规则识别区域。')
notIncludes(fillConfigDialog, "source: 'AI'", '应用识别结果保存来源不应继续标记为 AI。')
notIncludes(templateApi, 'Codex CLI analysis', '表单模板前端 API 不应再描述 Codex CLI 识别。')
includes(fillConfigDialog, 'if (rows.length === 0) {', '未配置辅助行时必须允许保存单元规则。')
includes(fillConfigDialog, 'return []', '未配置辅助行时保存空辅助行集合。')
notIncludes(fillConfigDialog, 'At least one assist row is required for fillable cells.', '单元类型配置不得被辅助行必填校验拦截。')
notIncludes(fillConfigDialog, 'is not assigned to an assist row.', '普通单元规则保存不得要求全部归属辅助行。')

const openFillConfigBlock =
  templatePage.match(/const openSelectedTemplateFillConfig = async \(\) => \{[\s\S]*?\n\}/)?.[0] || ''
assert.ok(openFillConfigBlock, '填写配置打开入口必须是异步正式入口。')
assert.match(
  openFillConfigBlock,
  /row\.status !== 'DRAFT'[\s\S]*TemplateApi\.autoDetectTemplateFillRules\(row\.templateId,\s*row\.versionNo\)[\s\S]*handleDraftVersionReady\(response\)[\s\S]*fillConfigDialogVisible\.value = true/s,
  '正式版本点击填写配置必须先通过规则识别接口生成或复用草稿、切换草稿，再打开可编辑面板。'
)
assert.match(
  openFillConfigBlock,
  /fillConfigOpening\.value = true[\s\S]*finally[\s\S]*fillConfigOpening\.value = false/s,
  '正式版本生成草稿期间必须维护加载状态，并在结束后恢复。'
)

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
