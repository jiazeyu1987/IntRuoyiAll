const fs = require('fs')
const path = require('path')
const assert = require('assert')

const workspaceRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

function sliceBetween(source, startNeedle, endNeedle, label) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

const batchExecutionService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
)
const executionService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java'
)

const buildOpenRequest = sliceBetween(
  batchExecutionService,
  'private MesProBatchRecordExecutionOpenOrCreateByContextReqVO buildOpenOrCreateExecutionReq(',
  'private Long resolveTaskPermissionScopeId',
  'buildOpenOrCreateExecutionReq'
)

assert.match(
  buildOpenRequest,
  /\.setTaskId\(task\.getId\(\)\)/,
  'eDHR 打开传统批记录时必须把当前批次任务 ID 传给执行记录创建/打开链路。'
)
assert.doesNotMatch(
  buildOpenRequest,
  /\.setTaskId\(null\)/,
  'eDHR 打开传统批记录不得传空 taskId，否则会按粗粒度上下文复用旧执行记录。'
)

const openOrCreateByContext = sliceBetween(
  executionService,
  'public MesProBatchRecordExecutionOpenOrCreateByContextRespVO openOrCreateByContext(',
  'public void submitBatchRecordExecution(',
  'openOrCreateByContext'
)

assert.match(
  openOrCreateByContext,
  /selectActiveByContext\([\s\S]*reqVO\.getBatchExecutionId\(\),\s*reqVO\.getTaskId\(\)/,
  '执行记录 active 查询必须按 batchExecutionId + taskId 隔离当前批次任务。'
)
assert.match(
  openOrCreateByContext,
  /buildActiveContextKey\(workOrder\.getId\(\),\s*reqVO\.getTaskId\(\)/,
  '执行记录 activeContextKey 必须包含当前批次任务 ID。'
)
assert.match(
  openOrCreateByContext,
  /\.taskId\(reqVO\.getTaskId\(\)\)/,
  '新建执行记录必须持久化当前批次任务 ID。'
)
assert.doesNotMatch(
  openOrCreateByContext,
  /selectActiveByContext\([\s\S]*reqVO\.getBatchExecutionId\(\),\s*null/,
  '执行记录 active 查询不得把 taskId 写成 null。'
)

console.log('mes-edhr-cell-link-task-id-context-static PASS')
