const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue'),
  'utf8'
)

const listKeys = [
  'productionLeader',
  'frontlinePqc',
  'frontlineProduction',
  'orderAllocation',
  'batchRecordMapping'
]

assert.equal(
  (page.match(/data-edhr-batch-record-test-run-all-button/g) || []).length,
  5,
  '五个 Tab 顶部都必须提供测试全部按钮。'
)
for (const listKey of listKeys) {
  assert.match(
    page,
    new RegExp(`data-edhr-batch-record-test-run-all-button[\\s\\S]*?@click="handleTestTab\\('${listKey}'\\)"`),
    `${listKey} 顶部按钮必须只启动当前 Tab 的全量测试。`
  )
}

assert.equal(
  (page.match(/getTabTestButtonText\('/g) || []).length,
  5,
  '五个按钮必须复用一个进度文案函数。'
)
assert.match(
  page,
  /function\s+getTabTestButtonText\(listKey:\s*BatchRecordTestListKey\)[\s\S]*测试中[\s\S]*completed[\s\S]*total/,
  '运行中的当前 Tab 按钮必须展示已完成数和总数。'
)
assert.match(
  page,
  /const\s+testingTabListKey\s*=\s*ref<BatchRecordTestListKey>\(\)/,
  '批量测试必须保存当前运行的 Tab，供互斥和进度状态使用。'
)
assert.match(
  page,
  /async function\s+handleTestTab\(listKey:\s*BatchRecordTestListKey\)[\s\S]*const\s+rows\s*=\s*\[\.\.\.getBatchRecordTestRowsRef\(listKey\)\.value\]/,
  '批量测试必须读取 Tab 的完整正式行集合，不能只读取分页或筛选结果。'
)
assert.match(
  page,
  /async function\s+handleTestTab\(listKey:[\s\S]*for\s*\(const\s+row\s+of\s+rows\)[\s\S]*await\s+handleTestRow\(row,\s*'tab'\)/,
  '当前 Tab 的所有行必须逐行等待执行，不能并发抢占单一轮询状态。'
)
assert.doesNotMatch(
  page,
  /handleTestTab[\s\S]{0,1800}Promise\.all/,
  '批量测试不得通过 Promise.all 并行启动 Codex CLI。'
)
assert.match(
  page,
  /async function\s+pollCodexTestExecutionResult\(historyKey:\s*string,\s*executionId:\s*number\)[\s\S]*while\s*\(pollToken\s*===\s*resultPollToken\)[\s\S]*terminalExecutionStatuses\.has\(execution\.status\)[\s\S]*return\s+execution/,
  '共享轮询必须等待到终态后返回，批量循环才能严格顺序执行。'
)
assert.match(
  page,
  /while\s*\(pollToken\s*===\s*resultPollToken\)[\s\S]*await\s+waitForResultPollInterval\(pollToken\)/,
  '非终态执行必须等待轮询间隔后继续，不得提前完成当前行。'
)
assert.match(
  page,
  /async function\s+handleTestRow\(row:[\s\S]*testingTabListKey\.value\s*!==\s*undefined[\s\S]*source\s*===\s*'single'[\s\S]*已有批量测试正在执行/,
  '单行入口必须同步阻止批量测试期间的手动执行。'
)
assert.match(
  page,
  /async function\s+handleTestTab\(listKey:[\s\S]*testingTabListKey\.value\s*!==\s*undefined[\s\S]*e2eTestingTabListKey\.value\s*!==\s*undefined[\s\S]*testingRowCaseName\.value\s*!==\s*undefined[\s\S]*已有测试正在执行/,
  '批量入口必须同步阻止重复批量、E2E 或与单行测试重叠。'
)
assert.match(
  page,
  /async function\s+handleTestTab\(listKey:[\s\S]*catch\s*\(error\)[\s\S]*批量测试在[\s\S]*已停止/,
  '启动或读取异常必须明确指出失败行并停止后续任务。'
)
assert.doesNotMatch(
  page,
  /async function\s+handleTestTab\(listKey:[\s\S]*testResult\.visible\s*=\s*true/,
  '批量测试不得自动打开全局结果弹窗，每行回复仍必须从历史入口查看。'
)
assert.match(
  page,
  /@media\s*\(min-width:\s*1181px\)[\s\S]*unified-list-template--single-line-toolbar[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto[\s\S]*unified-list-template__multi-filter[\s\S]*min-width:\s*0/,
  '桌面窄视口必须允许筛选区收缩，不能把测试全部和新增按钮裁切到页面外。'
)

console.log('edhr-batch-record-test-tab-run-all-static PASS')
