const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const workbench = readUtf8('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const teamLeaderApi = readUtf8('IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts')
const controllerSaveReq = readUtf8(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderLossReasonSaveReqVO.java'
)
const controller = readUtf8(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)
const saveReqBo = readUtf8(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderLossReasonSaveReqBO.java'
)
const lossReasonService = readUtf8(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderLossReasonServiceImpl.java'
)

const extractAfterMarker = (source, marker, nextMarker) => {
  const markerIndex = source.indexOf(marker)
  assert.notStrictEqual(markerIndex, -1, `Missing marker: ${marker}`)
  const start = source.lastIndexOf('<el-dialog', markerIndex)
  assert.notStrictEqual(start, -1, `Missing dialog start before: ${marker}`)
  const end = source.indexOf(nextMarker, markerIndex)
  assert.notStrictEqual(end, -1, `Missing next marker after: ${marker}`)
  return source.slice(start, end)
}

const extractCreatePayload = (source) => {
  const start = source.indexOf('await createTeamLeaderLossReason({')
  assert.notStrictEqual(start, -1, 'Create loss reason call must exist.')
  const end = source.indexOf('})', start)
  assert.notStrictEqual(end, -1, 'Create loss reason call must close.')
  return source.slice(start, end)
}

const lossReasonDialog = extractAfterMarker(
  workbench,
  'data-loss-reason-edit-dialog',
  '\n    <el-dialog'
)

assert.match(
  lossReasonDialog,
  /<el-form-item\s+v-if="lossReasonDialogMode === 'edit'"\s+label="原因编码"/,
  '原因编码只能在修改已有损耗原因时展示，新增时由系统自动生成。'
)
assert.match(
  lossReasonDialog,
  /<el-form-item\s+v-if="lossReasonDialogMode === 'edit'"\s+label="启用状态"/,
  '启用状态只能在修改已有损耗原因时维护，新增默认启用。'
)
assert.match(
  lossReasonDialog,
  /<el-form-item\s+v-if="lossReasonDialogMode === 'edit'"\s+label="维护说明"/,
  '维护说明只能在修改已有损耗原因时展示，新增弹窗删除截图红框说明区。'
)
assert.doesNotMatch(
  lossReasonDialog,
  /<el-form-item\s+label="原因编码"\s+required>/,
  '新增损耗原因弹窗不得要求手工原因编码。'
)
assert.doesNotMatch(
  workbench,
  /lossReasonDialogMode\.value === 'create'\s*&&\s*!reasonCode/,
  '新增损耗原因提交前不得再校验手工 reasonCode。'
)
assert.doesNotMatch(
  workbench,
  /损耗原因编码和名称不能为空/,
  '新增损耗原因错误提示不得继续要求编码。'
)

const createPayload = extractCreatePayload(workbench)
assert.match(createPayload, /routeProcessId:\s*row\.routeProcessId/, '新增 payload 必须绑定当前路线工序。')
assert.match(createPayload, /reasonName\b/, '新增 payload 必须提交原因名称。')
assert.doesNotMatch(createPayload, /reasonCode/, '新增 payload 不得提交手工 reasonCode。')
assert.doesNotMatch(createPayload, /enabled:/, '新增 payload 不得提交启用状态，后端默认启用。')
assert.doesNotMatch(createPayload, /remark:/, '新增 payload 不得提交维护说明。')

assert.match(
  teamLeaderApi,
  /interface TeamLeaderLossReasonSaveReqVO[\s\S]*routeProcessId:\s*number[\s\S]*reasonName:\s*string/,
  '前端新增损耗原因类型必须只要求路线工序和原因名称。'
)
assert.doesNotMatch(
  teamLeaderApi,
  /interface TeamLeaderLossReasonSaveReqVO[\s\S]*reasonCode:\s*string/,
  '前端新增损耗原因类型不得继续声明必填 reasonCode。'
)
assert.doesNotMatch(controllerSaveReq, /NotBlank\(message = "原因编码不能为空"\)/, '后端新增 VO 不得要求原因编码。')
assert.doesNotMatch(saveReqBo, /private String reasonCode;/, '后端新增 BO 不得继续接收手工原因编码。')
assert.doesNotMatch(controller, /\.reasonCode\(reqVO\.getReasonCode\(\)\)/, 'Controller 不得从请求体透传手工原因编码。')
assert.doesNotMatch(
  lossReasonService,
  /StrUtil\.trim\(reqBO\.getReasonCode\(\)\)/,
  '新增服务不得从请求 BO 读取手工原因编码。'
)
assert.match(
  lossReasonService,
  /private\s+String\s+generateLossReasonCode\s*\(\s*Long\s+routeProcessId\s*\)/,
  '后端服务必须有正式自动生成损耗原因编码的方法。'
)
assert.match(
  lossReasonService,
  /\.reasonCode\(generateLossReasonCode\(routeProcess\.getId\(\)\)\)/,
  '新增损耗原因必须使用当前路线工序生成唯一编码。'
)
assert.doesNotMatch(
  lossReasonService,
  /StrUtil\.isBlank\(reqBO\.getReasonCode\(\)\)/,
  '新增保存校验不得再要求 reasonCode。'
)

console.log('PASS: team leader loss reason create dialog hides manual fields and backend generates code')
