const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const templatePage = read('src/views/form-center/template/index.vue')
const templateApi = read('src/api/form-center/template.ts')
const simulatePage = read('src/views/form-center/template/FormTemplateSimulatePage.vue')
const routes = read('src/router/modules/remaining.ts')
const designerWrapper = read('src/views/mes/pro/batchrecord-shared/DesignerWrapper.vue')
const formTemplateDesignerWrapper = read(
  'src/views/form-center/template/components/FormTemplateDesignerWrapper.vue'
)
const formTemplateFillConfigDialog = read(
  'src/views/form-center/template/components/FormTemplateFillConfigDialog.vue'
)

assert.match(
  templatePage,
  /<FormTemplateDesignerWrapper\s+v-(?:else-)?if="isDesignerMode(?:\s*&&\s*!templateRouteLoadError)?"[\s\S]*designer-title="表单模板 Jimu 编辑器"[\s\S]*preview-title="表单模板预览"[\s\S]*\/>/,
  '表单模板必须保留当前表单模板页内的 Jimu 预览壳'
)
assert.doesNotMatch(
  templatePage,
  /isTemplateDesignerEditMode|templateMode/,
  '表单模板“编辑”不得再回退到 templateMode=edit 的规则面板'
)
assert.match(
  templatePage,
  /const\s+isDesignerMode\s*=\s*computed\(\(\)\s*=>\s*route\.query\.mode\s*===\s*'designer'\)/,
  '表单模板必须复用批记录管理的 designer 页面模式'
)
assert.match(
  templatePage,
  /import\s+FormTemplateDesignerWrapper\s+from\s+'\.\/components\/FormTemplateDesignerWrapper\.vue'/,
  '表单模板必须通过表单中心自己的组件进入 Jimu 设计器'
)
assert.doesNotMatch(
  templatePage,
  /import\s+DesignerWrapper\s+from\s+'@\/views\/mes\/pro\/batchrecord-shared\/DesignerWrapper\.vue'/,
  '表单模板不得从批记录表单页面导入 DesignerWrapper 壳'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplate\s*=[\s\S]*?openSelectedTemplateWorkspace\('preview'\)/,
  '“打开”必须进入当前模板的同页只读工作区'
)
assert.match(
  templatePage,
  /data-form-template-action="edit"[\s\S]*?@click="editSelectedTemplate"[\s\S]*?>\s*编辑\s*</,
  '“编辑”必须绑定当前模板的同页编辑入口'
)
assert.match(
  templatePage,
  /const\s+openDesigner\s*=\s*async\s*\([\s\S]*?reportMode:\s*'preview'\s*\|\s*'edit'\s*=\s*'preview'[\s\S]*?const\s+reportId\s*=\s*normalizeRouteQueryText\(template\.designerReportId\)[\s\S]*?path:\s*route\.path[\s\S]*?templateId:\s*template\.templateId[\s\S]*?versionNo:\s*template\.versionNo[\s\S]*?mode:\s*'designer'[\s\S]*?reportId[\s\S]*?reportMode/,
  'openDesigner 必须保留当前表单模板页面路径，并用当前模板 reportId + reportMode 进入 Jimu'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateWorkspace\s*=\s*async\s*\(reportMode:\s*'preview'\s*\|\s*'edit'\)\s*=>\s*\{[\s\S]*?openDesigner\(selectedTemplate\.value,\s*reportMode\)/,
  'openSelectedTemplateWorkspace 必须转调 openDesigner，避免重复维护路由细节'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateFill\s*=[\s\S]*?path:\s*'\/mdm\/form-center\/template\/simulate'[\s\S]*?templateId[\s\S]*?versionNo/,
  '“填写”必须跳转表单中心独立模拟填写页'
)
assert.match(
  designerWrapper,
  /defineOptions\(\{\s*name:\s*'MesProBatchRecordReportDesignerWrapper'\s*\}\)/,
  '批记录表单 Jimu DesignerWrapper 必须继续存在，作为交互对齐基准'
)
assert.match(
  designerWrapper,
  /reportMode\s*=\s*computed<'preview'\s*\|\s*'edit'>/,
  '共享 Jimu DesignerWrapper 必须支持 edit/preview 模式切换'
)
assert.match(
  formTemplateDesignerWrapper,
  /reportMode\s*=\s*computed<'preview'\s*\|\s*'edit'>/,
  '表单模板 Jimu DesignerWrapper 必须支持 edit/preview 模式切换'
)
assert.match(
  formTemplateDesignerWrapper,
  /BatchRecordReportApi\.getEditPath\(reportId\)/,
  '表单模板 Jimu 壳必须像批记录表单一样通过 edit path 进入 Jimu 编辑器'
)
assert.doesNotMatch(
  templatePage,
  /if\s*\(reportMode\s*===\s*'edit'\)[\s\S]{0,600}?return/,
  '表单模板“编辑”不得再分流到非 Jimu 的本地规则面板'
)
assert.match(
  formTemplateFillConfigDialog,
  /embedded\?:\s*boolean[\s\S]*?close:\s*\[\]/,
  '表单模板填写配置编辑器必须支持页面内嵌和关闭返回'
)
assert.doesNotMatch(
  templatePage,
  /\/mes\/pro\/batch-record-form-list/,
  '表单模板“编辑”不得跳转到批记录表单列表页面'
)
assert.doesNotMatch(
  templatePage,
  /templateViewDialogRef|fillDialogVisible|rulesDialogVisible/,
  '三个按钮不得继续依赖查看、填写或编辑弹窗'
)
assert.match(
  routes,
  /path:\s*'form-center\/template\/simulate'[\s\S]*?@\/views\/form-center\/template\/FormTemplateSimulatePage\.vue[\s\S]*?activeMenu:\s*'\/mdm\/form-center\/template'/,
  '表单中心必须注册隐藏的独立模拟填写路由'
)
assert.match(
  simulatePage,
  /<FormTemplateIndex\s+simulation-only\s*\/>[\s\S]*?import\s+FormTemplateIndex\s+from\s+'\.\/index\.vue'/,
  '独立模拟填写页面必须复用表单模板自身运行态，不得复制批记录数据链路'
)
assert.match(
  templatePage,
  /defineProps<\{\s*simulationOnly\?:\s*boolean\s*\}>[\s\S]*?isTemplateSimulationMode\s*=\s*computed\(\(\)\s*=>\s*props\.simulationOnly\)/,
  '独立模拟填写页面必须通过显式组件属性隔离，避免列表页实例重复加载模板版本'
)

assert.match(
  templateApi,
  /getTemplateVersion[\s\S]*?\/form-center\/templates\/\$\{templateId\}\/versions\/\$\{versionNo\}/,
  '独立工作区必须按 templateId + versionNo 精确读取模板'
)

for (const source of [templatePage, templateApi]) {
  for (const forbidden of [
    'batchRecordReportId',
    '/mes/pro/feedback/edhr-batch-execution/template-simulate'
  ]) {
    assert.doesNotMatch(source, new RegExp(forbidden), `表单模板交互不得依赖批记录字段或路由：${forbidden}`)
  }
}

console.log('PASS form-template-button-interaction-parity-static')
