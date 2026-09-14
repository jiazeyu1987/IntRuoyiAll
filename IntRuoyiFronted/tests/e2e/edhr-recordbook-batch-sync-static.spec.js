const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(process.cwd())
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const routeDesigner = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const fieldAuditApi = read('src/api/mes/pro/edhr/fieldAudit.ts')
const formFillLogApi = read('src/api/mes/pro/edhr/formFillLog.ts')
const fieldAuditPage = read('src/views/mes/pro/edhr/FieldAuditPage.vue')
const fieldAuditDetailPage = read('src/views/mes/pro/edhr/FieldAuditDetailPage.vue')
const formFillLogPage = read('src/views/mes/pro/edhr/FormFillLogPage.vue')
const operationAuditPane = read('src/views/mes/pro/edhr/components/OperationAuditListPane.vue')
const operationAuditPage = read('src/views/mes/pro/edhr/OperationAuditPage.vue')
const cellRulesDialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const templateRules = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const realE2E = read('tests/e2e/edhr-recordbook-batch-sync-real.e2e.js')

assert.match(flowConfigApi, /recordbookEnabled\??:/, '路线批记录绑定类型必须包含 recordbookEnabled。')
assert.doesNotMatch(routeDesigner, /data-route-process-setting-field="recordbook-enabled"/, '路线配置不得再显示记录本启用开关。')
assert.doesNotMatch(
  routeDesigner,
  /recordbookEnabled:\s*(report|binding)\.recordbookEnabled\s*!==\s*false/,
  '保存路线配置时不得继续保留历史关闭状态。'
)
assert.ok(
  (routeDesigner.match(/recordbookEnabled:\s*true/g) || []).length >= 3,
  '读取、草稿快照和保存 payload 都必须默认启用记录本。'
)
assert.match(
  routeDesigner,
  /hasSelectedScheduleCapacityDraftChanges/,
  '只修改记录本开关时不得触发产能覆盖保存或产能覆盖必须大于 0 校验。'
)
assert.match(
  routeDesigner,
  /getChangedSelectedProcessAttributeDrafts\(\)\.filter\(hasSelectedScheduleCapacityDraftChanges\)/,
  '保存工序属性草稿时必须只对产能字段变更保存产能配置。'
)

assert.match(batchDetail, /isRecordbookEnabledForCurrentTask/, '批次详情必须按任务冻结配置判断记录本是否启用。')
assert.match(batchDetail, /currentProcessFillCarrier[\s\S]*RECORDBOOK/, '记录本启用时批次详情默认填写载体必须是记录本。')
assert.match(batchDetail, /v-if="isRecordbookEnabledForCurrentTask"/, '记录本禁用时 UI 不得显示记录本按钮。')
assert.match(batchDetail, /recordbookEnabled/, '批次详情任务模型必须使用 recordbookEnabled 冻结值。')
assert.match(
  batchDetail,
  /isRecordbookEnabledForCurrentTask\.value[\s\S]*记录本填写/,
  '记录本禁用时批次详情不得显示“记录本填写”证据入口。'
)
assert.match(
  batchDetail,
  /hideRecordbookMode:\s*!isRecordbookEnabledForCurrentTask\.value/,
  '记录本禁用时从批次详情跳转操作审计必须隐藏记录本模式。'
)
assert.match(
  batchDetail,
  /const\s+openPendingTaskByFillCarrier[\s\S]*fillCarrier\s*===\s*'RECORDBOOK'[\s\S]*handleOpenTask\(row,\s*'RECORDBOOK'\)[\s\S]*handleOpenTask\(row,\s*'FORM'\)/,
  '批次详情行级待办打开必须尊重用户选择的批记录模式，不能按默认记录本重算。'
)

assert.match(fieldAuditApi, /fillCarrier\??:/, '字段审计保存请求必须携带填写载体。')
assert.match(fieldAuditApi, /fillMode\??:/, '字段审计保存请求必须携带填写模式。')
assert.match(fieldAuditApi, /recordbookValueJson\??:/, '字段审计项必须暴露记录本填写值。')
assert.match(fieldAuditApi, /batchRecordValueJson\??:/, '字段审计项必须暴露批记录存储值。')
assert.match(executionPage, /fillCarrier:\s*'RECORDBOOK'/, '记录本模式保存时必须向后端声明 RECORDBOOK。')
assert.match(executionPage, /fillMode:\s*RECORDBOOK_UNRESTRICTED_FILL_MODE/, '记录本模式保存时必须向后端声明不受控填写模式。')
assert.match(executionPage, /execution\.value\?\.recordbookEnabled\s*===\s*true/, '执行页必须用冻结配置阻止禁用任务进入记录本模式。')
assert.match(formFillLogApi, /recordbookValueDisplay\??:/, '填写日志接口必须暴露记录本填写值。')
assert.match(formFillLogApi, /batchRecordValueDisplay\??:/, '填写日志接口必须暴露批记录存储值。')
for (const [name, source] of [
  ['字段审计列表', fieldAuditPage],
  ['字段审计详情', fieldAuditDetailPage],
  ['填写日志详情', formFillLogPage]
]) {
  assert.match(source, /记录本填写值/, `${name}必须展示记录本填写值。`)
  assert.match(source, /批记录存储值/, `${name}必须展示批记录存储值。`)
}

assert.match(operationAuditPane, /hideRecordbookMode/, '内嵌操作审计必须支持禁用记录本后隐藏记录本筛选。')
assert.match(operationAuditPage, /hideRecordbookMode/, '操作审计页面必须支持禁用记录本后隐藏记录本筛选。')

assert.match(cellRulesDialog, /字段范围/, '批记录单元格规则弹窗必须复用现有侧栏展示数字字段范围配置。')
assert.match(cellRulesDialog, /最小值/, '批记录单元格规则弹窗必须允许通过真实页面填写数字最小值。')
assert.match(cellRulesDialog, /最大值/, '批记录单元格规则弹窗必须允许通过真实页面填写数字最大值。')
assert.match(cellRulesDialog, /v-model="selectedNumericMin"/, '数字最小值必须通过标准 v-model 绑定到 computed setter。')
assert.match(cellRulesDialog, /v-model="selectedNumericMax"/, '数字最大值必须通过标准 v-model 绑定到 computed setter。')
assert.match(
  cellRulesDialog,
  /set:\s*\(value:\s*number \| null \| undefined\)\s*=>\s*setSelectedNumericConstraint\('min', value\)/,
  '数字最小值 computed setter 必须写回所选规则 constraints.min。'
)
assert.match(
  cellRulesDialog,
  /set:\s*\(value:\s*number \| null \| undefined\)\s*=>\s*setSelectedNumericConstraint\('max', value\)/,
  '数字最大值 computed setter 必须写回所选规则 constraints.max。'
)
assert.match(
  cellRulesDialog,
  /@change="setSelectedNumericConstraint\('min', \$event\)"/,
  '数字最小值必须通过 Element Plus change 事件写回，确保真实保存 payload 带 constraints.min。'
)
assert.match(
  cellRulesDialog,
  /@change="setSelectedNumericConstraint\('max', \$event\)"/,
  '数字最大值必须通过 Element Plus change 事件写回，确保真实保存 payload 带 constraints.max。'
)
assert.match(templateRules, /'min', 'max', 'scale', 'precision'/, '数字规则清洗必须保留 min/max 以冻结到运行态快照。')

assert.doesNotMatch(
  realE2E,
  /ensureDisabledRecordbookSampleViaUi|restoreRecordbookEnabledViaUi|setTargetRouteRecordbookSwitch|data-route-process-setting-field="recordbook-enabled"/,
  '真实 E2E 不得继续尝试通过已隐藏的记录本开关创建禁用样本。'
)
assert.match(realE2E, /recordbook-default-entry/, '真实 E2E 必须保留记录本默认入口验证。')
assert.doesNotMatch(realE2E, /routeSnapshotJson/, '真实 E2E 不得依赖路线版本接口未暴露的 routeSnapshotJson 字段。')
assert.match(realE2E, /async function waitForApprovalCenterTaskRow/, '真实 E2E 审批中心必须显式等待目标待办行渲染完成。')
assert.match(realE2E, /waitForApprovalCenterTaskRow\(page,\s*rowTexts,\s*routeVersion\.id\)/, '路线版本审批必须通过等待后的待办行定位，不能在表格渲染前即时判空。')
assert.doesNotMatch(realE2E, /withdrawPendingRouteCandidateViaUi/, '真实 E2E 不得依赖当前返回 500 的撤回路径作为恢复前置。')

console.log('PASS: eDHR recordbook batch sync static contract')
