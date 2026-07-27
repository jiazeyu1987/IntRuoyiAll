const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const templatePage = read('src/views/form-center/template/index.vue')
const templateApi = read('src/api/form-center/template.ts')
const routes = read('src/router/modules/remaining.ts')
const designerWrapper = read(
  'src/views/form-center/template/components/FormTemplateDesignerWrapper.vue'
)
const simulatePage = read('src/views/form-center/template/FormTemplateSimulatePage.vue')

assert.match(
  templatePage,
  /<FormTemplateDesignerWrapper[\s\S]*?v-if="isDesignerMode\s*&&\s*templateDesignerMode\s*===\s*'preview'"/,
  '表单模板必须像批记录管理一样通过独立 DesignerWrapper 切换同页设计工作区'
)
assert.match(
  templatePage,
  /const\s+isDesignerMode\s*=\s*computed\(\(\)\s*=>\s*route\.query\.mode\s*===\s*'designer'\)/,
  '表单模板必须复用批记录管理的 designer 页面模式'
)
assert.match(
  templatePage,
  /const\s+templateDesignerMode\s*=\s*computed<'preview'\s*\|\s*'edit'>/,
  '表单模板 DesignerWrapper 必须区分 preview 和 edit'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplate\s*=[\s\S]*?openSelectedTemplateWorkspace\('preview'\)/,
  '“打开”必须进入当前模板的同页只读工作区'
)
assert.match(
  templatePage,
  /const\s+editSelectedTemplate\s*=[\s\S]*?openSelectedTemplateWorkspace\('edit'\)/,
  '“编辑”必须进入当前模板的同页编辑工作区'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateWorkspace\s*=[\s\S]*?path:\s*route\.path[\s\S]*?mode:\s*'designer'[\s\S]*?templateMode/,
  '同页 DesignerWrapper 必须保留当前页面路径并使用模板自身模式参数'
)
assert.match(
  templatePage,
  /const\s+getList\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*?if\s*\(isDesignerMode\.value\s*\|\|\s*isTemplateSimulationMode\.value\)\s*\{[\s\S]*?syncTemplateRouteContext\(\)[\s\S]*?return[\s\S]*?TemplateApi\.getTemplatePool/,
  'DesignerWrapper 和独立模拟填写页必须直接读取精确模板版本，不得先执行模板池列表查询'
)
assert.match(
  templatePage,
  /const\s+syncTemplateRouteContext\s*=\s*async[\s\S]*?requiresExactTemplateVersion\s*=\s*isDesignerMode\.value\s*\|\|\s*isTemplateSimulationMode\.value[\s\S]*?requiresExactTemplateVersion\s*\?\s*await\s+TemplateApi\.getTemplateVersion\(templateId,\s*versionNo\)/,
  '每次进入 DesignerWrapper 或独立模拟填写页都必须重新读取当前精确模板版本'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateFill\s*=[\s\S]*?path:\s*'\/mdm\/form-center\/template\/simulate'[\s\S]*?templateId[\s\S]*?versionNo/,
  '“填写”必须跳转表单中心独立模拟填写页'
)
assert.match(
  templatePage,
  /v-if="!isDesignerMode\s*&&\s*!isTemplateSimulationMode"/,
  '表单模板列表必须在 DesignerWrapper 和独立模拟填写页之外渲染'
)
assert.match(
  routes,
  /path:\s*'form-center\/template\/simulate'[\s\S]*?@\/views\/form-center\/template\/FormTemplateSimulatePage\.vue/,
  '表单中心模拟填写路由必须使用独立页面组件'
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
  designerWrapper,
  /defineOptions\(\{\s*name:\s*'FormCenterTemplateDesignerWrapper'\s*\}\)/,
  '表单模板必须提供独立 DesignerWrapper 组件'
)
assert.doesNotMatch(
  templatePage,
  /templateViewDialogRef|fillDialogVisible|rulesDialogVisible/,
  '三个按钮不得继续依赖查看、填写或编辑弹窗'
)

assert.match(
  templateApi,
  /getTemplateVersion[\s\S]*?\/form-center\/templates\/\$\{templateId\}\/versions\/\$\{versionNo\}/,
  '独立工作区必须按 templateId + versionNo 精确读取模板'
)
assert.match(
  routes,
  /path:\s*'form-center\/template\/simulate'[\s\S]*?@\/views\/form-center\/template\/FormTemplateSimulatePage\.vue[\s\S]*?activeMenu:\s*'\/mdm\/form-center\/template'/,
  '表单中心必须注册隐藏的独立模拟填写路由'
)

for (const source of [templatePage, templateApi]) {
  for (const forbidden of [
    'batchRecordReportId',
    'batchRecordBindingStatus',
    'reportId',
    '/mes/pro/feedback/edhr-batch-execution/template-simulate'
  ]) {
    assert.doesNotMatch(source, new RegExp(forbidden), `表单模板交互不得依赖批记录字段或路由：${forbidden}`)
  }
}

console.log('PASS form-template-button-interaction-parity-static')
