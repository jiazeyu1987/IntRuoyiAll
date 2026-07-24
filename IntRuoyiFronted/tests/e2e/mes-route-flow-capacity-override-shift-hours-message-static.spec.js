const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const graphPath = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const source = fs.readFileSync(graphPath, 'utf8')

if (!source.includes('openCapacityWorkstationRepairDialog')) {
  throw new Error('产能覆盖缺少班次小时必须打开工作站绑定修复弹框。')
}

if (!/if\s*\(shiftHours === undefined \|\| shiftHours <= 0\)\s*{[\s\S]*await openCapacityWorkstationRepairDialog\(\)[\s\S]*return/.test(source)) {
  throw new Error('缺少班次小时分支必须进入修复弹框并停止当前保存流程。')
}

if (source.includes('产能覆盖保存失败：缺少班次小时，无法计算班次产能。')) {
  throw new Error('产能覆盖缺少班次小时提示仍是旧的泛化文案。')
}

if (source.includes('产能覆盖保存失败：当前路线工序未绑定工作站，或工作站未配置班次小时。请先在工艺路线工序中绑定工作站，并在排产员工作台确认该工作站的班次小时后再保存产能覆盖。')) {
  throw new Error('缺少班次小时不能停留在纯错误提示，必须提供绑定修复流程。')
}

console.log('mes-route-flow-capacity-override-shift-hours-repair-static.spec.js passed')
