const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(process.cwd())
const batchApi = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/edhr/batchExecution.ts'),
  'utf8'
)
const detailPage = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)

assert.match(batchApi, /batchRecordSort\??:/, '批次任务类型必须包含 batchRecordSort。')
assert.match(batchApi, /executionMode\??:/, '批次任务类型必须包含 executionMode。')
assert.match(batchApi, /available\??:/, '批次任务类型必须包含 available 门禁字段。')
assert.match(batchApi, /gateMessage\??:/, '批次任务类型必须包含 gateMessage。')

assert.match(detailPage, /groupedTasksByProcess/, '批次详情页必须按工序分组展示多张批记录任务。')
assert.match(detailPage, /batchRecordSort/, '批次详情页必须展示或使用批记录顺序。')
assert.match(detailPage, /executionMode/, '批次详情页必须展示串行/并行模式。')
assert.match(detailPage, /gateMessage/, '批次详情页必须展示后端门禁原因。')
assert.match(
  detailPage,
  /canOpenTask[\s\S]*available\s*!==\s*false/,
  '打开按钮禁用规则必须尊重后端 available=false。'
)

console.log('PASS: eDHR batch multi-record gating static contract')
