const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const progressPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'progress.ts'
)
const actionFormPanelPath = path.join(
  repoRoot,
  'src',
  'views',
  'form-center',
  'business-action',
  'ActionFormPanel.vue'
)

const detail = fs.readFileSync(detailPath, 'utf8').replace(/\r\n/g, '\n')
const progress = fs.readFileSync(progressPath, 'utf8').replace(/\r\n/g, '\n')
const actionFormPanel = fs.readFileSync(actionFormPanelPath, 'utf8').replace(/\r\n/g, '\n')

const extractConstBlock = (source, marker) => {
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `missing block: ${marker}`)
  const next = source.indexOf('\n\nconst ', start + marker.length)
  return next >= 0 ? source.slice(start, next) : source.slice(start)
}

const optionalTaskBlock = extractConstBlock(
  detail,
  'const isOptionalTask = (row: EdhrBatchExecutionTaskRespVO) =>'
)
assert.ok(
  optionalTaskBlock.includes('isOptionalRouteFormTask(row)'),
  '批次详情页可跳过判断必须调用与后端 requiredPolicy=OPTIONAL 对齐的共享 helper。'
)
assert.ok(
  !optionalTaskBlock.includes('!isRequiredBatchRecordTask(row)'),
  '前端不得把 requiredFlag=false 或非必填进度口径直接等同于可跳过路线表单。'
)

assert.ok(
  progress.includes("export const isOptionalRouteFormTask") &&
    progress.includes("task.requiredPolicy === 'OPTIONAL'"),
  '损耗单等路线表单是否可跳过必须与后端 requiredPolicy=OPTIONAL 对齐。'
)

const skipTaskBlock = extractConstBlock(
  detail,
  'const canSkipOptionalTask = (row: EdhrBatchExecutionTaskRespVO) =>'
)
assert.ok(
  skipTaskBlock.includes('isOptionalTask(row)') && skipTaskBlock.includes("hasAllowedTaskAction(row, 'SKIP')"),
  '跳过表单按钮必须同时满足 OPTIONAL 策略和后端 SKIP 动作授权。'
)

const viewTaskBlock = extractConstBlock(
  detail,
  'const canViewRouteFormTask = (row: EdhrBatchExecutionTaskRespVO) =>'
)
assert.ok(
  viewTaskBlock.includes('!isSpecialNode(row)') &&
    viewTaskBlock.includes('!resolveTaskSlotBlocker(row)') &&
    !viewTaskBlock.includes("hasAllowedTaskAction(row, 'OPEN_FORM')"),
  '仅有查看权限的损耗单必须保留只读查看入口，不能被填写权限门禁关闭。'
)

const handleTaskBlock = extractConstBlock(
  detail,
  'const canHandlePendingTask = (row: EdhrBatchExecutionTaskRespVO) =>'
)
assert.ok(
  handleTaskBlock.includes('canViewRouteFormTask(row)'),
  '右侧损耗单卡片主动作必须在无填写权限但有查看权限时仍可点击查看。'
)

assert.ok(
  detail.includes("return '查看表单'"),
  '无填写权限但可查看的损耗单主动作应显示“查看表单”，不能继续显示“打开填写”。'
)

const selectedActionBlock = extractConstBlock(
  detail,
  'const handleSelectedPendingTaskAction = async (row: EdhrBatchExecutionTaskRespVO) =>'
)
assert.ok(
  selectedActionBlock.includes('openPendingTaskByFillCarrier(row, fillCarrier)'),
  '右侧损耗单“打开填写”必须打开当前点击的表单任务。'
)
assert.ok(
  selectedActionBlock.includes('canSkipOptionalTask(row)') &&
    selectedActionBlock.includes('handleSkipOptionalTask(row)'),
  '只有严格可选表单才允许从统一动作入口进入跳过弹窗。'
)
assert.ok(
  selectedActionBlock.includes('openReadonlyRouteFormTask(row)') &&
    selectedActionBlock.indexOf('openReadonlyRouteFormTask(row)') >
      selectedActionBlock.indexOf('handleSkipOptionalTask(row)'),
  '无填写权限的查看动作必须进入只读表单面板，且不得抢占可选表单跳过分支。'
)

const readonlyOpenBlock = extractConstBlock(
  detail,
  'const openReadonlyRouteFormTask = (row: EdhrBatchExecutionTaskRespVO) =>'
)
assert.ok(
  readonlyOpenBlock.includes('routeFormReadonly.value = true') &&
    readonlyOpenBlock.includes('routeFormDrawerVisible.value = true') &&
    !readonlyOpenBlock.includes('openEdhrBatchTask') &&
    !readonlyOpenBlock.includes('skipEdhrBatchSpecialNode'),
  '只读查看动态表单不得调用填写打开接口或跳过接口。'
)

assert.ok(
  detail.includes(':disabled="routeFormReadonly"') &&
    detail.includes('当前账号仅有查看权限'),
  '只读动态表单面板必须禁用保存/提交并展示只读原因。'
)
assert.ok(
  actionFormPanel.includes('<el-button :disabled="disabled"') &&
    actionFormPanel.includes('@click="resolveAction"'),
  '表单中心动作面板只读时连“解析”动作也必须禁用，不能发起写入相关动作链。'
)

assert.ok(
  progress.includes("task.requiredPolicy !== 'OPTIONAL'"),
  '批次必填进度也必须排除后端 OPTIONAL 策略，避免前后端必填口径漂移。'
)

console.log('PASS: eDHR loss form open action never treats required route forms as skippable.')
