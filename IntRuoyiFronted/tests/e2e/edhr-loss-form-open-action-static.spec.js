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

const detail = fs.readFileSync(detailPath, 'utf8').replace(/\r\n/g, '\n')
const progress = fs.readFileSync(progressPath, 'utf8').replace(/\r\n/g, '\n')

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
  progress.includes("task.requiredPolicy !== 'OPTIONAL'"),
  '批次必填进度也必须排除后端 OPTIONAL 策略，避免前后端必填口径漂移。'
)

console.log('PASS: eDHR loss form open action never treats required route forms as skippable.')
