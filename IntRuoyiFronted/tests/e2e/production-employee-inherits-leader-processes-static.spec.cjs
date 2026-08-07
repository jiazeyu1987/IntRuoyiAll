const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend', 'yudao-module-mes', 'src', 'main', 'java')

const readUtf8 = (filePath) => fs.readFileSync(filePath, 'utf8')

const page = readUtf8(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
)
const api = readUtf8(path.join(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts'))
const controller = readUtf8(
  path.join(
    backendRoot,
    'cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
  )
)
const runtimeConfigService = readUtf8(
  path.join(
    backendRoot,
    'cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java'
  )
)
const processContextService = readUtf8(
  path.join(
    backendRoot,
    'cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineDeviceAccountContextServiceImpl.java'
  )
)

for (const removedUiContract of [
  '生产人员工序绑定',
  '绑定工序员工',
  'processEmployeeBindingForm',
  'submitProcessEmployeeBinding',
  'data-team-leader-employee-config'
]) {
  assert.ok(!page.includes(removedUiContract), `班组配置不得保留员工工序绑定 UI: ${removedUiContract}`)
}

for (const removedApiContract of [
  'TeamEmployeeBindingSaveReqVO',
  'TeamEmployeeBindingDisableReqVO',
  'TeamProcessEmployeeBindingSaveReqVO',
  'addTeamEmployeeBinding',
  'disableTeamEmployeeBinding',
  'saveTeamProcessEmployeeBinding',
  '/employee-binding/add',
  '/employee-binding/disable',
  '/process-employee-binding/save'
]) {
  assert.ok(!api.includes(removedApiContract), `前端不得暴露员工工序绑定合同: ${removedApiContract}`)
  assert.ok(!controller.includes(removedApiContract), `后端不得暴露员工工序绑定合同: ${removedApiContract}`)
}

assert.ok(
  !runtimeConfigService.includes('MesProcessPoolTeamEmployeeBindingMapper') &&
    !runtimeConfigService.includes('listEmployeeBindings'),
  '一线运行配置不得查询历史员工工序绑定'
)
assert.match(
  processContextService,
  /MesProcessPoolTeamEmployeeProfileMapper[\s\S]*resolveResponsibleLeaderUserId/,
  '一线工序范围必须通过生产人员档案解析负责生产组长'
)
assert.match(
  processContextService,
  /resolveResponsibleLeaderContext\(loginUserId\)[\s\S]*listRouteStartProductionLeaderSwitchableProcesses\(responsibleLeader\.leaderUserId\(\)\)/,
  '生产员工必须继承其生产组长在正式工序开始配置中负责的全部工序'
)

console.log('production employee inherits leader processes static contract PASS')
