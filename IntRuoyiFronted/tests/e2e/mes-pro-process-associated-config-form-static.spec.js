const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const formSource = readText('src/views/mes/pro/process/ProProcessForm.vue')
const listSource = readText('src/views/mes/pro/process/index.vue')
const routeProcessApiSource = readText('src/api/mes/pro/route/process/index.ts')
const routeEditPageSource = readText('src/views/mes/pro/route/RouteEditPage.vue')
const routeFlowGraphSource = readText('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const assertIncludes = (content, expected, message) => {
  assert(content.includes(expected), `${message}: missing ${expected}`)
}

const assertNotIncludes = (content, expected, message) => {
  assert(!content.includes(expected), `${message}: unexpected ${expected}`)
}

assertIncludes(
  routeProcessApiSource,
  'getRouteProcessByRouteAndProcess',
  '路线工序 API 必须提供 routeId + processId 解析当前 routeProcessId 的能力'
)

assertIncludes(
  listSource,
  "@click=\"openForm('detail', scope.row.id, scope.row)\"",
  '工序编码详情入口必须把当前行传入弹窗，保留 routeList 上下文'
)
assertIncludes(
  listSource,
  "@click=\"openForm('update', scope.row.id, scope.row)\"",
  '工序编辑入口必须把当前行传入弹窗，保留 routeList 上下文'
)
assert.match(
  listSource,
  /formRef\.value\.open\(type,\s*id,\s*\{[\s\S]*row[\s\S]*routeId:\s*queryParams\.routeId[\s\S]*\}\)/,
  '工序列表打开弹窗时必须传入当前行和当前路线筛选上下文。'
)

assertNotIncludes(
  formSource,
  'RouteFlowConfigPanel',
  '工序弹窗不得继续引用已下线的旧配置面板'
)
assertIncludes(formSource, 'ProRouteProcessApi', '工序弹窗必须解析当前路线工序编号')
assertIncludes(formSource, '批记录与填写配置', '工序弹窗必须展示关联配置区域标题')
assertIncludes(formSource, 'label="所属工艺路线"', '工序弹窗必须允许选择配置所属工艺路线')
assertIncludes(formSource, 'v-model="selectedRouteId"', '所属工艺路线选择必须绑定 selectedRouteId')
assertIncludes(
  formSource,
  'getRouteProcessByRouteAndProcess',
  '工序弹窗必须通过 routeId + processId 查询 routeProcessId'
)
assertIncludes(formSource, 'openAssociatedRouteConfig', '工序弹窗必须提供打开正式工序设置入口')
assertIncludes(formSource, "name: 'MesProRouteEdit'", '工序弹窗必须跳转到工艺路线编辑页')
assertIncludes(formSource, "tab: 'flow'", '工序弹窗必须跳转到流转关系图页签')
assertIncludes(
  formSource,
  'routeProcessId: String(selectedRouteProcessId.value)',
  '工序弹窗必须用解析出的路线工序编号深链定位当前工序'
)
assert.match(
  formSource,
  /routeOptions\.value\.length\s*===\s*1[\s\S]*selectedRouteId\.value\s*=\s*routeOptions\.value\[0\]\.id/,
  '只有单所属路线时才能自动选择路线，多路线不能静默默认第一条。'
)

assertIncludes(routeEditPageSource, 'route.query.routeProcessId', '工艺路线编辑页必须接收目标路线工序参数')
assert.match(
  routeFlowGraphSource,
  /props\.targetRouteProcessId[\s\S]*selectedRouteProcessId\.value = restoredRouteProcessId/,
  '流转关系图必须用目标路线工序参数恢复当前工序'
)
assertNotIncludes(routeFlowGraphSource, 'data-flow-action="save-selected-process-settings"', '流转关系图不得保留专门保存工序设置入口')

console.log('PASS: MES pro process associated config form contract is satisfied')
