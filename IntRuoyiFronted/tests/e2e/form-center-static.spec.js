const fs = require('fs')
const path = require('path')

const root = process.cwd()

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const assertFile = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  if (!fs.existsSync(absolutePath)) {
    throw new Error(`Missing expected file: ${relativePath}`)
  }
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertNoFile = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  if (fs.existsSync(absolutePath)) {
    throw new Error(`Unexpected file should be removed: ${relativePath}`)
  }
}

const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message || `Expected content to include: ${expected}`)
  }
}

const assertNotIncludes = (content, unexpected, message) => {
  if (content.includes(unexpected)) {
    throw new Error(message || `Expected content not to include: ${unexpected}`)
  }
}

const assertNoEmptyCatch = (relativePath) => {
  const content = read(relativePath)
  if (/catch\s*\([^)]*\)\s*\{\s*\}/.test(content) || /catch\s*\{\s*\}/.test(content)) {
    throw new Error(`Empty catch is not allowed in ${relativePath}`)
  }
}

const templateApi = assertFile('src/api/form-center/template.ts')
assertIncludes(templateApi, '/form-center/templates/import-doc')
assertIncludes(templateApi, '/form-center/template-pool')
assertIncludes(templateApi, '/source-file')
assertIncludes(templateApi, '/enable')
assertIncludes(templateApi, 'FormTemplateListItemVO')
assertIncludes(templateApi, 'recognizedFields?: FormRecognizedFieldVO[]')
assertIncludes(templateApi, 'jimuSchemaJson?: string')
assertIncludes(templateApi, 'FormTemplateObsoleteReqVO')
assertIncludes(templateApi, 'FormTemplateObsoletePendingRespVO')
assertIncludes(templateApi, 'submitTemplateObsoleteRequest')
assertIncludes(templateApi, 'findTemplateObsoletePendingRequest')
assertIncludes(templateApi, 'withdrawTemplateObsoleteRequest')
if (/obsoleteTemplateVersion/.test(templateApi)) {
  throw new Error('表单模板前端 API 不应继续暴露直接作废调用，作废必须走 BPM 作废申请')
}

const actionApi = assertFile('src/api/form-center/businessAction.ts')
assertIncludes(actionApi, '/form-center/actions/resolve')
assertIncludes(actionApi, 'BusinessActionContextVO')
assertIncludes(actionApi, 'FormActionResolutionVO')
assertIncludes(actionApi, 'templateVersionRef')

const instanceApi = assertFile('src/api/form-center/instance.ts')
assertIncludes(instanceApi, '/form-center/instances')
assertIncludes(instanceApi, '/submit')
assertIncludes(instanceApi, '/rework-submit')
assertIncludes(instanceApi, '/abandon')
assertIncludes(instanceApi, 'startUserSelectAssignees?: Record<string, number[]>')

const policyApi = assertFile('src/api/form-center/policy.ts')
assertIncludes(policyApi, '/form-center/policies')
assertIncludes(policyApi, '/publish')
assertIncludes(policyApi, 'FormPolicyListItemVO')

const templatePage = assertFile('src/views/form-center/template/index.vue')
assertIncludes(templatePage, 'UnifiedListTemplate')
assertIncludes(templatePage, 'form-template-workbench')
assertIncludes(templatePage, 'form-template-workbench__list')
assertIncludes(templatePage, 'form-template-preview')
assertIncludes(templatePage, 'form-template-preview__actions')
assertIncludes(templatePage, '模板名称')
assertIncludes(templatePage, '当前生效版本')
assertIncludes(templatePage, '待发布版本')
assertIncludes(templatePage, '状态')
assertIncludes(templatePage, '修改时间')
assertIncludes(templatePage, '备注')
assertIncludes(templatePage, 'TemplateImportDialog')
assertIncludes(templatePage, '表单预览')
assertIncludes(templatePage, '识别字段')
assertIncludes(templatePage, 'highlight-current-row')
assertIncludes(templatePage, '@row-click="selectTemplate"')
assertIncludes(templatePage, 'selectedTemplate')
assertIncludes(templatePage, 'selectTemplate')
assertIncludes(templatePage, 'isSelectedTemplateRow')
assertIncludes(templatePage, 'recognizedFields')
assertIncludes(templatePage, 'EdhrExecutionReadonlyForm')
assertIncludes(templatePage, 'form-template-visual-preview')
assertIncludes(templatePage, 'visualPreviewFormViewModel')
assertIncludes(templatePage, 'downloadSelectedTemplateSource')
assertIncludes(templatePage, 'publishSelectedTemplate')
assertIncludes(templatePage, 'openSelectedTemplate')
assertIncludes(templatePage, 'editSelectedTemplate')
assertIncludes(templatePage, 'openSelectedTemplateFill')
assertIncludes(templatePage, 'obsoleteSelectedTemplate')
assertIncludes(templatePage, 'obsoleteRequestDialogVisible')
assertIncludes(templatePage, 'obsoleteRequestForm')
assertIncludes(templatePage, 'submitTemplateObsoleteRequest')
assertIncludes(templatePage, 'findTemplateObsoletePendingRequest')
assertIncludes(templatePage, 'withdrawTemplateObsoleteRequest')
assertIncludes(templatePage, "type FormTemplateObsoleteOperationState")
assertIncludes(templatePage, "'pending-withdrawable'")
assertIncludes(templatePage, "'pending-readonly'")
assertIncludes(templatePage, "'voided'")
assertIncludes(templatePage, 'resolveTemplateObsoleteOperationState')
assertIncludes(templatePage, '撤回作废申请')
assertIncludes(templatePage, '作废申请中')
assertIncludes(templatePage, 'form-template-route-workspace')
assertIncludes(templatePage, 'form-template-fill-workspace')
assertIncludes(templatePage, 'form-template-signature-dialog')
assertIncludes(templatePage, 'openSelectedTemplateAction')
assertIncludes(templatePage, 'openTemplateActionDialog')
assertIncludes(templatePage, 'openSelectedTemplateWorkspace')
assertIncludes(templatePage, 'openSelectedTemplateCellLinks')
assertIncludes(templatePage, "path: '/mes/pro/batch-record-cell-link'")
assertIncludes(templatePage, 'templateId: row.templateId')
assertIncludes(templatePage, 'versionNo: row.versionNo')
assertIncludes(templatePage, "returnLabel: '返回'")
if (/obsoleteTemplateVersion/.test(templatePage)) {
  throw new Error('表单模板页面不能继续调用直接作废 API，必须提交 BPM 作废申请')
}
assertIncludes(templatePage, 'EdhrExecutionTemplateEditableForm')
assertIncludes(templatePage, 'simulatedPreviewFormViewModel')
assertIncludes(templatePage, 'templatePreviewSignatureMarkers')
assertIncludes(templatePage, 'formatTemplateUpdatedTime')
assertIncludes(templatePage, 'form:template-source:download')
assertIncludes(templatePage, 'form:template:publish')
assertIncludes(templatePage, 'publishTemplateVersion')
assertIncludes(templatePage, 'enableTemplateVersion')
assertIncludes(templatePage, 'saveTemplateJimuSchema')
const previewActionsMatch = templatePage.match(/<div v-if="selectedTemplate" class="form-template-preview__actions">[\s\S]*?<\/div>/)
if (!previewActionsMatch) {
  throw new Error('表单模板右侧预览必须保留独立操作区')
}
const previewActions = previewActionsMatch[0]
for (const label of ['最大化', '打开', '编辑', '填写', '链接', '作废']) {
  if (!new RegExp(`>\\s*${label}\\s*<`).test(previewActions)) {
    throw new Error(`表单模板右侧预览操作区缺少“${label}”按钮`)
  }
}
if (/>\s*删除\s*</.test(previewActions)) {
  throw new Error('表单模板右侧预览操作区不应展示“删除”按钮，应保留语义准确的“作废”入口')
}
if (/>\s*签名\s*</.test(previewActions)) {
  throw new Error('表单模板右侧预览操作区不应展示“签名”按钮')
}
if (/>\s*规则\s*</.test(previewActions)) {
  throw new Error('表单模板右侧预览操作区不应保留独立“规则”按钮，应合并到“编辑”')
}
const cellLinkButtonMatch = previewActions.match(/<el-button[\s\S]*?openSelectedTemplateCellLinks[\s\S]*?<\/el-button>/)
if (!cellLinkButtonMatch || !/canUseTemplateInteractiveAction\(selectedTemplate\)/.test(cellLinkButtonMatch[0])) {
  throw new Error('表单模板“链接”按钮必须受终态只读动作投影控制，并进入单元格链接工作台')
}
if (/>\s*重命名\s*</.test(previewActions)) {
  throw new Error('表单模板右侧预览操作区不应新增“重命名”按钮')
}
const disableButtonMatch = previewActions.match(/<el-button[\s\S]*?disableSelectedTemplate[\s\S]*?<\/el-button>/)
if (!disableButtonMatch || !/canDisableTemplate\(selectedTemplate\)/.test(disableButtonMatch[0])) {
  throw new Error('表单模板“停用”按钮必须只在可停用状态显示，不能覆盖已停用状态')
}
const editButtonMatch = previewActions.match(/<el-button[\s\S]*?editSelectedTemplate[\s\S]*?<\/el-button>/)
if (!editButtonMatch || !/canUseTemplateInteractiveAction\(selectedTemplate\)/.test(editButtonMatch[0])) {
  throw new Error('已作废表单模板不应显示“编辑”按钮，编辑入口必须受终态只读动作投影控制')
}
const fillButtonMatch = previewActions.match(/<el-button[\s\S]*?openSelectedTemplateFill[\s\S]*?<\/el-button>/)
if (!fillButtonMatch || !/canUseTemplateInteractiveAction\(selectedTemplate\)/.test(fillButtonMatch[0])) {
  throw new Error('已作废表单模板不应显示“填写”按钮，填写入口必须受终态只读动作投影控制')
}
const enableButtonMatch = previewActions.match(/<el-button[\s\S]*?enableSelectedTemplate[\s\S]*?<\/el-button>/)
if (!enableButtonMatch || !/canEnableTemplate\(selectedTemplate\)/.test(enableButtonMatch[0])) {
  throw new Error('表单模板已停用后必须显示“启用”按钮')
}
if (!/>\s*启用\s*</.test(enableButtonMatch[0])) {
  throw new Error('表单模板启用动作必须显示为“启用”')
}
if (!/form:template:disable/.test(enableButtonMatch[0])) {
  throw new Error('表单模板“启用”按钮应复用现有模板停用权限，避免新增未配置权限导致按钮隐藏')
}
if (/canDisableOrObsoleteTemplate/.test(templatePage)) {
  throw new Error('表单模板停用和作废不能共用状态判断，已停用状态需要独立启用动作')
}
assertIncludes(templatePage, 'const canDisableTemplate = (row: FormTemplateListItemVO) =>')
assertIncludes(templatePage, "!['PENDING_APPROVAL', 'DISABLED', 'OBSOLETE'].includes(row.status)")
assertIncludes(templatePage, 'const canEnableTemplate = (row: FormTemplateListItemVO) =>')
assertIncludes(templatePage, "row.status === 'DISABLED'")
assertIncludes(templatePage, 'const canObsoleteTemplate = (row: FormTemplateListItemVO) =>')
assertIncludes(templatePage, 'const canUseTemplateInteractiveAction = (row: FormTemplateListItemVO) =>')
assertIncludes(templatePage, "row.status !== 'OBSOLETE'")
if (/label=["']版本["']/.test(templatePage)) {
  throw new Error('表单模板列表不得继续使用单一“版本”列，应展示当前生效版本和待发布版本')
}
if (/prop=["']actions["']/.test(templatePage)) {
  throw new Error('表单模板列表不应继续把全部操作塞进行内操作列，应使用右侧选中表单操作区')
}
if (/form-template-preview__field-grid/.test(templatePage)) {
  throw new Error('表单模板右侧红框主内容不得继续使用字段卡片网格，应渲染与批记录表单页签一致的视觉表格预览')
}
const editFunction = templatePage.match(/const editSelectedTemplate = (?:async )?\(\) => \{[\s\S]*?\n\}/)
if (!editFunction || !/openSelectedTemplateWorkspace\('edit'\)/.test(editFunction[0])) {
  throw new Error('表单模板“编辑”按钮应进入模板编辑工作区，不得打开导入/升版弹窗')
}
if (editFunction && /importDialogRef/.test(editFunction[0])) {
  throw new Error('表单模板“编辑”按钮不得复用导入/升版弹窗')
}
if (/openSelectedTemplateAction\('cellRules'\)/.test(templatePage)) {
  throw new Error('表单模板“规则”能力应合并到“编辑”，不应保留独立 cellRules 入口')
}
if (/deleteSelectedTemplate/.test(templatePage)) {
  throw new Error('表单模板不应保留 deleteSelectedTemplate 包装函数，作废动作统一走 obsoleteSelectedTemplate')
}
const obsoleteFunction = templatePage.match(/const obsoleteSelectedTemplate = async \(\) => \{[\s\S]*?\n\}/)
if (!obsoleteFunction || !/obsoleteRequestDialogVisible\.value\s*=\s*true/.test(obsoleteFunction[0])) {
  throw new Error('表单模板“作废”按钮必须打开作废申请弹窗，不得直接置为已作废')
}
if (obsoleteFunction && /TemplateApi\./.test(obsoleteFunction[0])) {
  throw new Error('表单模板“作废”按钮点击后不能直接调用后端作废 API')
}
if (/rulesDialogVisible\.value\s*=\s*true/.test(templatePage) && !/editableTemplateCellRules/.test(templatePage)) {
  throw new Error('表单模板“编辑”按钮不得只打开静态表格，应进入可编辑规则工作区')
}
if (!/isDesignerMode\s*&&\s*templateDesignerMode\s*===\s*'edit'/.test(templatePage)) {
  throw new Error('表单模板“编辑”按钮必须进入路由驱动的 DesignerWrapper 规则确认工作区')
}
if (/v-model=["']rulesDialogVisible["']/.test(templatePage)) {
  throw new Error('表单模板“编辑”按钮不得继续依赖规则确认弹窗')
}
for (const expected of [
  'batch-record-cell-rules-editor__sheet',
  'aria-label="选择单元格规则"',
  '是否可填写',
  '字段说明',
  'selectedRuleKey',
  'isSelectedCellFillable',
  'enableSelectedCellRule',
  'disableSelectedCellRule',
  'buildManualRuleFromCell',
  'selectRuleCell',
  'editableTemplateSheetLayoutJson'
]) {
  assertIncludes(templatePage, expected, `表单模板“编辑”按钮缺少批记录式规则确认契约：${expected}`)
}
if (/editableTemplateCellRules\.value\.length > 0/.test(templatePage)) {
  throw new Error('form template rules must allow saving an empty rule set after all cells are switched to non-fillable')
}
const fillWorkspaceMatch = templatePage.match(/v-if="isTemplateSimulationMode"[\s\S]*?form-template-fill-workspace[\s\S]*?<\/ContentWrap>/)
if (!fillWorkspaceMatch || !fillWorkspaceMatch[0].includes('EdhrExecutionTemplateEditableForm') || !fillWorkspaceMatch[0].includes('EdhrExecutionReadonlyForm')) {
  throw new Error('表单模板“填写”按钮应像批记录模拟填写一样同时展示模板内填写和表单显示预览')
}

const importDialog = assertFile('src/views/form-center/template/components/TemplateImportDialog.vue')
assertIncludes(importDialog, 'accept=".doc,.docx"')
assertIncludes(importDialog, 'templateName')
assertIncludes(importDialog, 'remark')
assertIncludes(importDialog, 'importTemplateDoc')
assertIncludes(importDialog, 'el-autocomplete')
assertIncludes(importDialog, 'loadTemplateOptions')
assertIncludes(importDialog, 'selectedTemplateId')
assertIncludes(importDialog, '版本号由系统自动生成')
if (/payload\.append\(\s*['"]versionNo['"]/.test(importDialog)) {
  throw new Error('导入弹窗不得向后端提交手工版本号，版本号必须由后端自动生成')
}
if (/prop=["']versionNo["']/.test(importDialog)) {
  throw new Error('导入弹窗不得保留手工版本号输入项')
}

assertNoFile('src/views/form-center/template/components/TemplateViewDialog.vue')
const templateDesignerWrapper = assertFile(
  'src/views/form-center/template/components/FormTemplateDesignerWrapper.vue'
)
assertIncludes(templateDesignerWrapper, "name: 'FormCenterTemplateDesignerWrapper'")
const templateSimulatePage = assertFile(
  'src/views/form-center/template/FormTemplateSimulatePage.vue'
)
assertIncludes(templateSimulatePage, "name: 'FormCenterTemplateSimulatePage'")
assertIncludes(templateSimulatePage, "import FormTemplateIndex from './index.vue'")

const actionPanel = assertFile('src/views/form-center/business-action/ActionFormPanel.vue')
assertIncludes(actionPanel, 'resolveBusinessAction')
assertIncludes(actionPanel, 'createFormInstance')
assertIncludes(actionPanel, 'submitFormInstance')
assertIncludes(actionPanel, 'reworkSubmitFormInstance')
assertIncludes(actionPanel, 'abandonFormInstance')
assertIncludes(actionPanel, 'buildSubmitPayload')
assertIncludes(actionPanel, 'actionFormData.value.startUserSelectAssignees')
assertIncludes(actionPanel, 'EdhrExecutionTemplateEditableForm')
assertIncludes(actionPanel, 'resolveEmbeddedTemplateVersionForActionForm')
assertIncludes(actionPanel, '动态表单运行态缺少 openTask 模板快照，无法渲染')
assertNotIncludes(
  actionPanel,
  'getTemplateVersion',
  'ActionFormPanel 运行态不得调用模板管理版本接口'
)
assertIncludes(actionPanel, 'applyLatestDraftSnapshotFormData')
assertIncludes(actionPanel, 'startUserSelectAssignees 必须是对象')
assertIncludes(actionPanel, 'FORM_POLICY_NOT_FOUND')
assertIncludes(actionPanel, 'BPM_BINDING_MISSING')
assertIncludes(actionPanel, 'templateVersionRef.templateName')
assertIncludes(actionPanel, 'templateVersionRef.versionNo')
assertIncludes(actionPanel, 'instanceStatus')
assertIncludes(actionPanel, 'instanceCode')

assertNoFile('src/views/form-center/business-action/index.vue')

const policyPage = assertFile('src/views/form-center/policy/index.vue')
assertIncludes(policyPage, 'getPolicyPage')
assertIncludes(policyPage, 'savePolicy')
assertIncludes(policyPage, 'publishPolicy')
assertIncludes(policyPage, 'form:policy:create')
assertIncludes(policyPage, 'form:policy:publish')
assertIncludes(policyPage, 'loadPublishedTemplates')

const remainingRoutes = assertFile('src/router/modules/remaining.ts')
assertIncludes(remainingRoutes, 'ApprovalCenterFormCenter')
assertIncludes(remainingRoutes, '/approval-center/manager/form-center/template')
assertIncludes(remainingRoutes, "path: 'form-center/template/simulate'")
assertIncludes(remainingRoutes, "MdmFormCenterTemplateSimulate")
assertIncludes(remainingRoutes, "@/views/form-center/template/FormTemplateSimulatePage.vue")
assertNotIncludes(remainingRoutes, "path: 'business-action'")
assertNotIncludes(remainingRoutes, "ApprovalCenterFormCenterBusinessAction")
assertNotIncludes(remainingRoutes, "import('@/views/form-center/business-action/index.vue')")
assertIncludes(remainingRoutes, "path: 'policy'")
assertIncludes(remainingRoutes, "path: 'effect'")
assertIncludes(remainingRoutes, "activeMenu: '/mdm/form-center/template'")
assertNotIncludes(remainingRoutes, "activeMenu: '/mdm/form-center/business-action'")
assertIncludes(remainingRoutes, "activeMenu: '/mdm/form-center/policy'")
assertIncludes(remainingRoutes, "activeMenu: '/mdm/form-center/effect'")
assertIncludes(remainingRoutes, "permission: ['form:template:query']")
assertIncludes(remainingRoutes, "permission: ['form:policy:query']")

const formCenterSeed = read('../IntRuoyiBackend/sql/mysql/20260717_bpm_form_center.sql')
const formCenterMoveSeed = read('../IntRuoyiBackend/sql/mysql/20260721_form_center_menu_under_basic_data.sql')
const formCenterRetireSeed = assertFile('../IntRuoyiBackend/sql/mysql/20260722_form_center_business_action_page_retire.sql')
for (const sqlSource of [formCenterSeed, formCenterMoveSeed, formCenterRetireSeed]) {
  assertNotIncludes(sqlSource, "'业务动作表单'")
  assertNotIncludes(sqlSource, "'business-action'")
  assertNotIncludes(sqlSource, "'form-center/business-action/index'")
  assertNotIncludes(sqlSource, "'FormCenterBusinessAction'")
}
assertIncludes(formCenterSeed, "(605071210, '实例创建', 'form:instance:create', 3, 20, 605071200")
assertIncludes(formCenterSeed, "(605071219, '实例快照查询', 'form:instance:snapshot:query', 3, 25, 605071200")
assertIncludes(formCenterRetireSeed, 'form_center_business_action_page_retire')
assertIncludes(formCenterRetireSeed, '605071209')

for (const file of [
  'src/api/form-center/template.ts',
  'src/api/form-center/businessAction.ts',
  'src/api/form-center/instance.ts',
  'src/api/form-center/policy.ts',
  'src/views/form-center/template/index.vue',
  'src/views/form-center/template/FormTemplateSimulatePage.vue',
  'src/views/form-center/template/components/FormTemplateDesignerWrapper.vue',
  'src/views/form-center/template/components/TemplateImportDialog.vue',
  'src/views/form-center/business-action/ActionFormPanel.vue',
  'src/views/form-center/policy/index.vue'
]) {
  assertNoEmptyCatch(file)
}

console.log('form-center static contract passed')
