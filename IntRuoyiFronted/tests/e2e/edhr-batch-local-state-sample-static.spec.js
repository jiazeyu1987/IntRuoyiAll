const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const listPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
  'utf8'
)

assert.doesNotMatch(
  listPage,
  /临时状态样本/,
  '批次执行列表工具栏不得展示临时状态样本入口。'
)

assert.doesNotMatch(
  listPage,
  /showLocalStateSampleActions|localStateSampleOptions|localStateSampleLoading|handleCreateLocalStateSample/,
  '批次执行列表页不得保留临时状态样本入口的本地状态和处理函数。'
)

assert.doesNotMatch(
  listPage,
  /createEdhrLocalStateSample|EdhrLocalStateSampleState|LOCAL_STATE_SAMPLE/,
  '批次执行列表页不得继续导入或触发本地状态样本创建能力。'
)

console.log('PASS edhr batch local state sample toolbar removal static contract')
