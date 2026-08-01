const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.resolve(workspaceRoot, 'IntRuoyiBackend')

const workbenchPath = path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const scopeDoPath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolTeamLeaderScopeDO.java'
)
const scopeServicePath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderScopeServiceImpl.java'
)
const routeDesignerPath = path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const workbenchSource = fs.readFileSync(workbenchPath, 'utf8')
const scopeDoSource = fs.readFileSync(scopeDoPath, 'utf8')
const scopeServiceSource = fs.readFileSync(scopeServicePath, 'utf8')
const routeDesignerSource = fs.readFileSync(routeDesignerPath, 'utf8')

assert.match(
  workbenchSource,
  /data-role-matrix-daily-close|dailyClose|日结/,
  'M5 requires a visible daily-close surface for unresolved items.'
)
for (const token of ['SCOPE_TYPE_WORKSTATION', 'SCOPE_TYPE_PRODUCTION_LINE', 'SCOPE_TYPE_EQUIPMENT', 'SCOPE_TYPE_ORDER']) {
  assert.match(scopeDoSource + scopeServiceSource, new RegExp(token), `M5 scope model must include ${token}.`)
}
assert.match(
  routeDesignerSource,
  /resolveRecordBindingSlotType|batchRecordReports/,
  'Batch-record progress must anchor to explicit formal route process batch-record bindings.'
)
assert.doesNotMatch(
  routeDesignerSource,
  /resolveRecordBindingSlotType\([^)]*\)\s*===\s*'MAIN'[\s\S]{0,120}formBindings/,
  'Batch-record progress must not use formBindings or default MAIN as a replacement source.'
)

console.log('PASS role-matrix daily-close/scope static contract')
