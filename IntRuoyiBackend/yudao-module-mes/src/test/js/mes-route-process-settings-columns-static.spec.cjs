const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeProcessList = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteProcessList.vue')

for (const label of ['生产系数', '班次产能', '批记录表单', '损耗单', '过程检验单', '参数记录表']) {
  assert.match(routeProcessList, new RegExp(`label="${label}"`), `工序设置表必须展示 ${label} 列。`)
}

for (const field of [
  'productionQuantityFactor',
  'shiftCapacity',
  'batchRecordFormNames',
  'lossReportFormNames',
  'processInspectionFormNames',
  'parameterRecordFormNames'
]) {
  assert.match(
    routeProcessList,
    new RegExp(`data-route-process-setting-field="${field}"`),
    `工序设置表必须暴露 ${field} 字段。`
  )
}

assert.match(routeProcessList, /ProRouteFlowConfigApi\.getProcessConfigList\(props\.routeId,/, '工序设置表必须读取 flow-config。')
assert.match(routeProcessList, /ProRouteApi\.getScheduleConfigListByRouteVersion/, '工序设置表必须读取路线班次产能配置。')
assert.match(routeProcessList, /routeProcessId:\s*draft\.routeProcessId/, '工序设置表保存必须按 routeProcessId。')
assert.match(routeProcessList, /data-route-process-action="save-process-settings"/, '工序设置表必须提供行级保存入口。')
assert.match(routeProcessList, /BatchRecordReportApi\.getGeneratedReportPage/, '工序设置表记录表单列必须使用正式表单选项接口。')
assert.match(routeProcessList, /formSlotType:\s*binding\.formSlotType/, '工序设置表批记录保存必须携带记录类型槽位。')
assert.doesNotMatch(routeProcessList, /<RouteFlowConfigPanel/, '不得恢复旧独立配置面板。')

console.log('mes-route-process-settings-columns-static PASS')
