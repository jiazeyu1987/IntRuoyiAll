const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const actionPanel = read('src/views/form-center/business-action/ActionFormPanel.vue')
const editableForm = read('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const templateApi = read('src/api/form-center/template.ts')
const instanceApi = read('src/api/form-center/instance.ts')

assert.match(
  templateApi,
  /export\s+const\s+getTemplateVersion[\s\S]*?\/form-center\/templates\/\$\{templateId\}\/versions\/\$\{versionNo\}/,
  '模板管理页面仍可按 templateId + versionNo 精确读取模板版本。'
)
assert.match(
  instanceApi,
  /export\s+const\s+getInstanceSnapshots[\s\S]*?\/form-center\/instances\/\$\{instanceId\}\/snapshots/,
  '动态表单动作面板必须能读取实例快照作为草稿数据来源。'
)
assert.match(
  actionPanel,
  /import\s+EdhrExecutionTemplateEditableForm\s+from\s+'@\/views\/mes\/pro\/edhr\/components\/EdhrExecutionTemplateEditableForm\.vue'/,
  'ActionFormPanel 必须渲染真实模板控件，不能只展示快照 JSON。'
)
assert.doesNotMatch(
  actionPanel,
  /getTemplateVersion/,
  'ActionFormPanel 是运行态业务面板，不得请求模板管理版本接口。'
)
assert.match(
  actionPanel,
  /const\s+actionFormData\s*=\s*ref<Record<string,\s*unknown>>\(\{\}\)/,
  'ActionFormPanel 必须维护本地可编辑表单数据，不能直接把父级元数据当填写数据。'
)
assert.match(
  actionPanel,
  /const\s+editableTemplateFormData\s*=\s*computed<TemplateSimulationValueMap>/,
  'ActionFormPanel 必须通过类型化 computed 把本地数据传给模板控件。'
)
assert.match(
  actionPanel,
  /const\s+actionPanelSheetLayoutJson\s*=\s*ref\(''\)/,
  'ActionFormPanel 必须维护动态表单模板布局。'
)
assert.match(
  actionPanel,
  /const\s+parseTemplateJimuSchema\s*=/,
  'ActionFormPanel 必须解析模板 Jimu schema 中的 sheetLayoutJson。'
)
assert.match(
  actionPanel,
  /const\s+loadTemplateVersionForActionForm\s*=\s*async/,
  'ActionFormPanel 必须在打开动态表单时加载模板版本。'
)
assert.match(
  actionPanel,
  /resolveEmbeddedTemplateVersionForActionForm\(\)/,
  'ActionFormPanel 必须使用 openTask 传入的嵌入模板快照加载运行态布局。'
)
assert.match(
  actionPanel,
  /动态表单运行态缺少 openTask 模板快照，无法渲染/,
  '运行态缺少嵌入模板快照时必须可见失败，不能降级请求模板管理接口。'
)
assert.match(
  actionPanel,
  /const\s+applyLatestDraftSnapshotFormData\s*=/,
  'ActionFormPanel 必须把最新草稿快照合并回本地表单数据。'
)
assert.match(
  actionPanel,
  /actionFormData\.value\s*=\s*\{\s*\.\.\.actionFormData\.value,\s*\.\.\.latestDraftSnapshot\.formData\s*\}/,
  '动态表单打开后必须优先显示已落库的实例草稿字段值。'
)
assert.match(
  actionPanel,
  /const\s+actionPanelFieldIdentityMap\s*=\s*ref<Record<string,\s*string>>\(\{\}\)/,
  'ActionFormPanel 必须维护坐标到 FormCenter 字段码的显式映射。'
)
assert.match(
  actionPanel,
  /buildRecognizedFieldIdentityMap\(template\.recognizedFields\s*\|\|\s*\[\]\)/,
  'ActionFormPanel 必须从模板识别字段生成 fieldCode 映射。'
)
assert.match(
  actionPanel,
  /<EdhrExecutionTemplateEditableForm[\s\S]*?v-model="editableTemplateFormData"[\s\S]*?:sheet-layout-json="actionPanelSheetLayoutJson"[\s\S]*?:cell-rules="actionPanelCellRules"[\s\S]*?:signature-markers="actionPanelSignatureMarkers"[\s\S]*?:field-identity-map="actionPanelFieldIdentityMap"/,
  'ActionFormPanel 必须把实例草稿数据和模板布局一起传给 eDHR 可编辑表单控件。'
)
assert.match(
  actionPanel,
  /const\s+payload:\s*SubmitFormInstanceReqVO\s*=\s*\{\s*formData:\s*actionFormData\.value\s*\}/,
  '提交/重提必须提交用户在动态表单控件里的当前值。'
)
assert.match(
  actionPanel,
  /createFormInstance\(\{[\s\S]*?formData:\s*actionFormData\.value/,
  '新建实例必须使用本地表单数据。'
)
assert.match(
  actionPanel,
  /saveFormDraft\(instanceId\.value!,\s*\{\s*formData:\s*actionFormData\.value\s*\}\)/,
  '保存草稿必须保存动态表单控件当前值。'
)
assert.doesNotMatch(
  actionPanel,
  /props\.formData\.startUserSelectAssignees/,
  '审批人选择也必须从本地表单数据读取，避免父级元数据覆盖实例草稿。'
)
assert.match(
  editableForm,
  /fieldIdentityMap\?:\s*Record<string,\s*string>/,
  'eDHR 模板控件必须支持 FormCenter 字段码作为数据键。'
)
assert.match(
  editableForm,
  /const\s+formDataFieldIdentity\s*=\s*props\.fieldIdentityMap\?\.\[cellIdentity\]\s*\|\|\s*cellIdentity/,
  'eDHR 模板控件必须用 fieldIdentityMap 把单元格坐标映射到实例 formData 字段码。'
)
assert.match(
  editableForm,
  /fieldIdentity:\s*formDataFieldIdentity/,
  'eDHR 模板控件的输入框必须读取映射后的字段码。'
)

console.log('PASS: eDHR dynamic form action panel renders persisted prefill data.')
