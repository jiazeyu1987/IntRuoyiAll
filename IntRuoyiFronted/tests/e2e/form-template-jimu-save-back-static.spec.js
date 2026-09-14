const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const apiSource = read('IntRuoyiFronted/src/api/form-center/template.ts')
const templatePage = read('IntRuoyiFronted/src/views/form-center/template/index.vue')
const backendController = read(
  'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/formcenter/FormCenterController.java'
)
const backendService = read(
  'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java'
)
const backendServiceInterface = read(
  'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeService.java'
)
const backendFilter = read(
  'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormTemplateJimuReportSaveSyncFilter.java'
)

const extractConstFunction = (source, name) => {
  const start = source.indexOf(`const ${name} =`)
  assert.notEqual(start, -1, `${name} must exist`)
  const nextConst = source.indexOf('\nconst ', start + 1)
  const nextLifecycle = source.indexOf('\nonMounted', start + 1)
  const candidates = [nextConst, nextLifecycle].filter((index) => index > start)
  const end = candidates.length > 0 ? Math.min(...candidates) : source.length
  return source.slice(start, end)
}

const openSelectedWorkspaceBody = extractConstFunction(templatePage, 'openSelectedTemplateWorkspace')

assert.match(
  backendController,
  /\/templates\/\{templateId\}\/versions\/\{versionNo\}\/editable-draft[\s\S]*?ensureTemplateEditableDraft/,
  '表单模板编辑正式链路必须提供可写草稿解析接口'
)
assert.match(
  apiSource,
  /ensureTemplateEditableDraft[\s\S]*?\/form-center\/templates\/\$\{templateId\}\/versions\/\$\{versionNo\}\/editable-draft/,
  '前端编辑入口必须调用表单模板自己的可写草稿接口'
)
assert.match(
  openSelectedWorkspaceBody,
  /reportMode\s*===\s*'edit'[\s\S]*?selectedTemplate\.value\.status\s*!==\s*'DRAFT'[\s\S]*?ensureTemplateEditableDraft[\s\S]*?handleDraftVersionReady[\s\S]*?openDesigner\(targetTemplate,\s*reportMode\)/,
  '已发布模板点击编辑时必须先切到草稿版本，再在表单模板页打开 Jimu 编辑器'
)
assert.doesNotMatch(
  openSelectedWorkspaceBody,
  /fill-rule-auto-detect|autoDetectTemplateFillRules/,
  '编辑入口切草稿不得隐式触发填写规则识别'
)
assert.match(
  backendFilter,
  /JMREPORT_SAVE_PATH\s*=\s*"\/jmreport\/save"/,
  '表单模板 Jimu 保存回写必须挂在 Jimu 原生保存端点'
)
assert.match(
  backendFilter,
  /resolveFormTemplateReportId[\s\S]*?FORM_TEMPLATE_REPORT_PREFIX/,
  '保存回写过滤器只能处理 FORMTPL 虚拟报表'
)
assert.match(
  backendFilter,
  /validateTemplateJimuReportSaveWritable[\s\S]*?syncTemplateJimuReportSave/,
  '保存回写必须先做草稿写保护，再在 Jimu 保存成功后同步模板版本'
)
assert.match(
  backendServiceInterface,
  /validateTemplateJimuReportSaveWritable\(String reportId,\s*Long tenantId\)/,
  '服务接口必须暴露 Jimu 保存草稿写保护'
)
assert.match(
  backendServiceInterface,
  /syncTemplateJimuReportSave\(String reportId,\s*Long tenantId\)/,
  '服务接口必须暴露 Jimu 保存后模板版本同步'
)
assert.match(
  backendService,
  /requireDraftTemplateVersionForJimuReport[\s\S]*?FormTemplateStatus\.DRAFT\.name\(\)/,
  '后端必须以模板版本状态 DRAFT 作为唯一写入门禁'
)
assert.match(
  backendService,
  /mergeFormTemplateDesignerJson[\s\S]*?sheetLayoutJson[\s\S]*?JsonUtils\.toJsonString\(designerRoot\)/,
  '后端必须把最新 Jimu 画布写回模板版本 sheetLayoutJson'
)
assert.doesNotMatch(
  backendService,
  /jimuSchemaJson\s*=\s*designerJson/,
  '不能把 Jimu 画布 JSON 直接覆盖表单模板外层 schema'
)

console.log('PASS form-template-jimu-save-back-static')
