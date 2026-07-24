const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const componentPath = path.resolve(frontendRoot, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const source = fs.readFileSync(componentPath, 'utf8')

for (const legacyToken of [
  'route-flow-config-panel-pressure-pump-fill-role-button',
  'applyPressurePumpFillRoleDefaults',
  'PRESSURE_PUMP_FILL_ROLE_NAMES',
  '压力泵生产填写员',
  '压力泵设备填写员',
  '压力泵质量填写员',
  'equipmentFillRule',
  'qualityFillRule'
]) {
  assert.ok(!source.includes(legacyToken), `批记录表单填写人改为单列后必须移除旧压力泵三类填写员逻辑：${legacyToken}`)
}

console.log('PASS: MES route flow removed pressure-pump legacy fill-role static contract')
