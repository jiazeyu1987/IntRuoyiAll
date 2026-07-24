const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../../../..')
const source = fs.readFileSync(
  path.join(
    repoRoot,
    'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java'
  ),
  'utf8'
)

assert.match(
  source,
  /buildExecutionActionUrl\(MesProEdhrWorkTaskDO task,\s*Long executionId\)/,
  '填写/返工 actionUrl 必须使用完整工作任务上下文构造。'
)
assert.match(
  source,
  /\/mes\/pro\/feedback\/edhr-execution\/form/,
  '填写/返工 actionUrl 必须直达 eDHR 填写工作区。'
)
assert.match(source, /fillCarrier=FORM/, '填写/返工 actionUrl 必须携带批记录填写载体。')
assert.match(source, /recordCategory=BATCH_RECORD/, '填写/返工 actionUrl 必须携带批记录记录分类。')
assert.match(source, /batchExecutionId=/, '填写/返工 actionUrl 必须携带批次执行 ID。')
assert.match(source, /batchTaskId=/, '填写/返工 actionUrl 必须携带批次任务 ID。')
assert.doesNotMatch(
  source,
  /buildExecutionActionUrl\(Long executionId,\s*Long workTaskId\)/,
  '填写/返工 actionUrl 不得只用 executionId/workTaskId 丢失批次上下文。'
)

console.log('PASS: MES eDHR work task actionUrl opens fill workspace')
