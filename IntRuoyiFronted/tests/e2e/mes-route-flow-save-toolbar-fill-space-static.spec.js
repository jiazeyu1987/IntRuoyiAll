const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const editPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const graph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.ok(
  !editPage.includes('<div class="route-edit-page__actions">') ||
    editPage.includes('v-if="activeRouteTab !== \'flow\'"'),
  '流转关系图页签不得继续渲染底部独立保存操作区。'
)

assert.match(
  formContent,
  /<RouteFlowGraphDesigner[\s\S]*?:submitting="formLoading"[\s\S]*?@request-submit="submitForm"[\s\S]*?\/>/,
  '流转关系图工具栏保存按钮必须沿用表单 submitForm 保存链路。'
)

assert.match(
  formContent,
  /const getActiveTab = \(\) => activeTab\.value[\s\S]*defineExpose\(\{[\s\S]*getActiveTab[\s\S]*\}\)/,
  '编辑页必须通过明确方法读取当前页签，避免流转关系图页签继续显示底部操作区。'
)

assert.match(
  graph,
  /data-flow-action="save-route-flow"[\s\S]*?@click="handleRequestSubmit"[\s\S]*?保 存/,
  '保存按钮必须渲染在流转关系图工具栏内并向父级请求提交。'
)

assert.match(
  graph,
  /const handleRequestSubmit = \(\) => \{[\s\S]*?emit\('request-submit'\)[\s\S]*?\}/,
  '保存按钮处理器必须继续向父级请求提交。'
)

assert.ok(
  graph.includes(':disabled="routeFlowWriteControlsDisabled || props.submitting || loading || saving || routeProcessSaving"'),
  '工具栏保存按钮必须跟随父级提交状态禁用，避免重复提交。'
)

assert.match(
  graph,
  /\.route-flow-graph-designer\s*\{[\s\S]*?height:\s*calc\(100vh - 210px\);[\s\S]*?max-height:\s*none;[\s\S]*?\}/,
  '流转关系图根容器必须取消 640px 高度上限并占满底部空白。'
)

assert.ok(
  !/\.route-edit-page__actions\s*\{[\s\S]*?padding:\s*44px 10px 0;[\s\S]*?\}/.test(editPage),
  '编辑页不得保留造成红框空白的 44px 顶部操作区 padding。'
)

console.log('mes-route-flow-save-toolbar-fill-space-static PASS')
