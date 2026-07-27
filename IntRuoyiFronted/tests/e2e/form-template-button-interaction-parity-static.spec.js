const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const templatePage = read('src/views/form-center/template/index.vue')
const templateApi = read('src/api/form-center/template.ts')
const routes = read('src/router/modules/remaining.ts')

assert.match(
  templatePage,
  /const\s+isTemplateWorkspaceMode\s*=\s*computed\(\(\)\s*=>\s*route\.query\.mode\s*===\s*'workspace'\)/,
  '表单模板必须通过当前路由 query 切换同页工作区'
)
assert.match(
  templatePage,
  /const\s+templateWorkspaceMode\s*=\s*computed<'preview'\s*\|\s*'edit'>/,
  '表单模板同页工作区必须区分 preview 和 edit'
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
  /const\s+openSelectedTemplateWorkspace\s*=[\s\S]*?path:\s*route\.path[\s\S]*?mode:\s*'workspace'[\s\S]*?templateMode/,
  '同页工作区必须保留当前页面路径并使用模板自身模式参数'
)
assert.match(
  templatePage,
  /const\s+openSelectedTemplateFill\s*=[\s\S]*?path:\s*'\/approval-center\/manager\/form-center\/template\/simulate'[\s\S]*?templateId[\s\S]*?versionNo/,
  '“填写”必须跳转表单中心独立模拟填写页'
)
assert.match(
  templatePage,
  /v-if="isTemplateWorkspaceMode"/,
  '表单模板页面必须渲染同页工作区'
)
assert.match(
  templatePage,
  /v-else-if="isTemplateSimulationMode"/,
  '表单模板页面必须渲染独立模拟填写工作区'
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
  /path:\s*'template\/simulate'[\s\S]*?@\/views\/form-center\/template\/index\.vue[\s\S]*?activeMenu:\s*'\/mdm\/form-center\/template'/,
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

