const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const templatePage = read('src/views/form-center/template/index.vue')
const templateApi = read('src/api/form-center/template.ts')
const simulatePage = read('src/views/form-center/template/FormTemplateSimulatePage.vue')
const routes = read('src/router/modules/remaining.ts')
const designerWrapper = read('src/views/form-center/template/components/FormTemplateDesignerWrapper.vue')

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
  /data-form-template-action="edit"[\s\S]*?@click="editSelectedTemplate"[\s\S]*?>\s*编辑\s*</,
  '“编辑”必须绑定当前模板的同页编辑入口'
)
assert.match(
  templatePage,
  /const\s+editSelectedTemplate\s*=[\s\S]*?openSelectedTemplateWorkspace\('edit'\)/,
  '“编辑”必须通过统一工作区函数进入当前模板的同页编辑模式'
)
assert.match(
  templatePage,
  /const\s+openDesigner\s*=\s*async\s*\([\s\S]*?templateMode:\s*'preview'\s*\|\s*'edit'\s*=\s*'preview'[\s\S]*?path:\s*route\.path[\s\S]*?templateId:\s*template\.templateId[\s\S]*?versionNo:\s*template\.versionNo[\s\S]*?mode:\s*'designer'[\s\S]*?templateMode/,
  '同页 DesignerWrapper 必须由 openDesigner 保留当前页面路径并使用模板自身模式参数'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateWorkspace\s*=\s*async\s*\(templateMode:\s*'preview'\s*\|\s*'edit'\)\s*=>\s*\{[\s\S]*?openDesigner\(selectedTemplate\.value,\s*templateMode\)/,
  'openSelectedTemplateWorkspace 必须转调 openDesigner，避免重复维护路由细节'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateFill\s*=[\s\S]*?path:\s*'\/mdm\/form-center\/template\/simulate'[\s\S]*?templateId[\s\S]*?versionNo/,
  '“填写”必须跳转表单中心独立模拟填写页'
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
