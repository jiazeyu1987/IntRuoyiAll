const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(repoRoot, relativePath))

const routeList = read('yudao-ui-admin-vue3/src/views/mes/pro/route/index.vue')
const routeEditPage = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteEditPage.vue')
const routeFormContent = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteFormContent.vue')
const flowGraphDesigner = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.doesNotMatch(routeList, /label="排产配置"/, '路线列表不能再展示独立排产配置列。')
assert.doesNotMatch(routeList, /label="批记录配置"/, '路线列表不能再展示独立批记录配置列。')
assert.doesNotMatch(routeList, /scheduleRouteEnabled/, '路线列表列配置不能再包含 scheduleRouteEnabled。')
assert.doesNotMatch(routeList, /batchRouteEnabled/, '路线列表列配置不能再包含 batchRouteEnabled。')
assert.doesNotMatch(routeList, /['"]schedule-config['"]/, '路线列表不能再跳转到旧排产配置页签。')
assert.doesNotMatch(routeList, /['"]batch-record-config['"]/, '路线列表不能再跳转到旧批记录配置页签。')
assert.strictEqual(
  exists('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteFlowConfigPanel.vue'),
  false,
  '独立排产/批记录配置面板组件必须从前端删除。'
)

assert.doesNotMatch(routeEditPage, /schedule-config|batch-record-config/, '编辑页旧 tab 参数必须统一落到 flow。')
assert.doesNotMatch(routeFormContent, /label="排产配置"|label="批记录配置"/, '编辑页不能再注册独立配置页签。')
assert.doesNotMatch(routeFormContent, /RouteFlowConfigPanel/, '独立配置面板不能再由编辑页页签挂载。')

assert.doesNotMatch(flowGraphDesigner, /<RouteFlowConfigPanel/, '选中工序侧栏不能再挂载独立排产/批记录配置面板。')
assert.doesNotMatch(flowGraphDesigner, /data-flow-node-config="schedule"/, '选中工序侧栏不能再出现独立排产属性块。')
assert.doesNotMatch(flowGraphDesigner, /data-flow-node-config="batch-record"/, '选中工序侧栏不能再出现独立批记录属性块。')
assert.doesNotMatch(flowGraphDesigner, /route-flow-graph-designer__node-config/, '关系图节点属性不能再用独立黄框式配置块包裹。')
assert.match(routeFormContent, /:target-route-process-id="targetRouteProcessId"/, '编辑页必须把目标路线工序传给流转关系图。')
assert.match(flowGraphDesigner, /targetRouteProcessId\?: number/, '关系图必须接收目标路线工序。')

assert.match(flowGraphDesigner, /\|\s*'productionQuantityFactor'/, '生产系数必须增加到选中工序设置字段列。')
assert.match(flowGraphDesigner, /\|\s*'shiftCapacity'/, '班次产能必须增加到选中工序设置字段列。')
assert.match(flowGraphDesigner, /data-flow-field-editor="productionQuantityFactor"/, '生产系数必须在选中工序设置字段中编辑。')
assert.match(flowGraphDesigner, /data-flow-field-editor="shiftCapacity"/, '班次产能必须在选中工序设置字段中编辑。')
assert.match(flowGraphDesigner, /data-flow-field-editor="record-form"/, '记录类型表单必须在选中工序设置字段中编辑。')
assert.match(flowGraphDesigner, /data-flow-action="save-selected-process-settings"/, '选中工序设置必须通过字段列表统一保存。')
assert.match(flowGraphDesigner, /保存工序设置/, '选中工序保存按钮必须明确指向工序设置。')
assert.doesNotMatch(flowGraphDesigner, /保存属性/, '选中工序保存按钮不能再使用泛化属性文案。')
assert.match(flowGraphDesigner, /ProRouteFlowConfigApi\.saveScheduleConfig/, '生产系数必须保存到流程排产配置接口。')
assert.match(flowGraphDesigner, /ProRouteApi\.saveScheduleConfig/, '班次产能必须保存到路线排产配置接口。')
assert.match(flowGraphDesigner, /ProRouteFlowConfigApi\.saveBatchRecordConfig/, '记录类型表单必须保存到流程批记录配置接口。')
assert.match(flowGraphDesigner, /label: '生产系数'/, '排产节点属性必须提供生产系数字段。')
assert.match(flowGraphDesigner, /label: '班次产能'/, '排产节点属性必须以班次产能作为唯一产能输入。')
assert.match(
  flowGraphDesigner,
  /const REQUIRED_PROCESS_ATTRIBUTE_FIELD_KEYS: ProcessDetailFieldKey\[\] = \[[\s\S]*'productionQuantityFactor'[\s\S]*'shiftCapacity'[\s\S]*'batchRecordFormNames'[\s\S]*'lossReportFormNames'[\s\S]*'processInspectionFormNames'[\s\S]*'parameterRecordFormNames'[\s\S]*\]/,
  '排产与批记录迁移字段必须作为选中工序设置的必备列。'
)
assert.match(
  flowGraphDesigner,
  /selectedProcessDetailFieldKeys\.value\s*=\s*mergeRequiredProcessAttributeFieldKeys\(\s*resolveSavedProcessDetailFieldKeys\(config\?\.columns\)\s*\)/,
  '加载用户列配置时必须把缺失的排产与批记录字段补入工序设置列。'
)
assert.doesNotMatch(flowGraphDesigner, /label="有限产能\(h\)"/, '前端不能再把有限产能展示成小时产能输入。')
assert.doesNotMatch(flowGraphDesigner, /label="班次小时"/, '白班夜班和班次小时不属于路线节点配置入口。')
assert.doesNotMatch(flowGraphDesigner, /SchedulerWorkbenchApi|getPolicySettings|defaultScheduleCapacityMode/, '排产规则和工作台默认值不应残留在路线节点配置前端。')
assert.doesNotMatch(flowGraphDesigner, /formulaTime|FORMULA_SAMPLE_QUANTITY|1000产品制作时间/, '无限产能公式配置不应残留在前端节点属性。')
assert.doesNotMatch(flowGraphDesigner, /<el-option label="无限" value="INFINITE_FORMULA"/, '前端暂不展示无限产能模式。')
assert.doesNotMatch(flowGraphDesigner, /label="执行模式"|SEQUENTIAL|PARALLEL/, '前端不再展示旧批记录执行模式。')
assert.doesNotMatch(flowGraphDesigner, /placeholder="槽位"/, '前端不再展示批记录槽位。')
assert.doesNotMatch(flowGraphDesigner, /placeholder="校验策略"/, '前端不再展示校验策略。')
for (const recordType of ['MAIN', 'LOSS_REPORT', 'PROCESS_INSPECTION', 'PARAMETER_RECORD']) {
  assert.match(
    flowGraphDesigner,
    new RegExp(`formSlotType:\\s*['"]${recordType}['"]`),
    `节点属性必须提供 ${recordType} 记录类型绑定入口。`
  )
}
assert.match(flowGraphDesigner, /:data-flow-record-type="field\.recordBindingType"/, '记录类型行必须向页面暴露稳定测试属性。')
assert.doesNotMatch(
  flowGraphDesigner,
  /batchRecordReportId:\s*report\.batchRecordReportId,[\s\S]{0,260}recordCategory:/,
  '前端保存批记录节点属性不能再提交隐藏记录类型。'
)
assert.doesNotMatch(
  flowGraphDesigner,
  /batchRecordReportId:\s*report\.batchRecordReportId,[\s\S]{0,260}validationProfile:/,
  '前端保存批记录节点属性不能再提交隐藏校验策略。'
)
assert.match(
  flowGraphDesigner,
  /formSlotType:\s*binding\.formSlotType/,
  '前端应按记录类型绑定表单，内部仅把记录类型映射到后端兼容字段。'
)

console.log('mes-route-unified-node-config-static PASS')
