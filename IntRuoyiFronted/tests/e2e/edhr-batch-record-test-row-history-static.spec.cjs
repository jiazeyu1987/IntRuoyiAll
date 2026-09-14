const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue'),
  'utf8'
)

assert.equal(
  (page.match(/data-edhr-batch-record-test-history-button/g) || []).length,
  5,
  '五张批记录测试列表都必须在每行操作中提供历史按钮。'
)
assert.equal(
  (page.match(/@click="openRowTestHistory\(row\)"/g) || []).length,
  5,
  '五张列表的历史按钮都必须按当前行打开历史。'
)
assert.equal(
  (page.match(/:disabled="!isRowTestHistoryReady\(row\)"/g) || []).length,
  5,
  '没有当前行终态回复时历史按钮必须置灰并禁止打开。'
)
assert.equal(
  (page.match(/:type="getRowTestHistoryButtonType\(row\)"/g) || []).length,
  5,
  '五张列表的历史按钮必须按当前行正式测试结果决定颜色。'
)
assert.match(
  page,
  /function\s+getRowTestHistoryButtonType\(row:\s*BatchRecordTestRow\)[\s\S]*rowTestHistories\[getRowTestHistoryKey\(row\)\][\s\S]*if\s*\(!history\?\.ready\s*\|\|\s*!history\.data\)\s*return\s*'info'[\s\S]*return\s+getExecutionStatusTagType\(history\.data\.status\)/,
  '历史按钮必须使用当前行正式执行状态；未完成保持灰色。'
)
assert.match(
  page,
  /function\s+getExecutionStatusTagType[\s\S]*status\s*===\s*'PASS'\)\s*return\s*'success'[\s\S]*status\s*===\s*'FAIL'[\s\S]*return\s*'danger'/,
  '正式状态映射必须保证成功为绿色、失败为红色。'
)
assert.match(
  page,
  /type\s+BatchRecordTestRowHistory\s*=\s*\{[\s\S]*historyKey:\s*string[\s\S]*executionId\?:\s*number[\s\S]*ready:\s*boolean[\s\S]*data\?:\s*CodexTestApi\.CodexTestExecutionVO/,
  '逐行历史必须显式保存稳定 key、executionId、ready 和正式执行结果。'
)
assert.match(
  page,
  /const\s+rowTestHistories\s*=\s*reactive<Record<string, BatchRecordTestRowHistory>>\(\{\}\)/,
  '历史必须按稳定字符串 key 分行保存，不能继续只使用单一全局结果。'
)
assert.match(
  page,
  /function\s+getRowTestHistoryKey\(row:\s*BatchRecordTestRow\)\s*\{[\s\S]*return\s+row\.caseName/,
  '行历史 key 必须使用唯一 caseName，不能使用跨列表可能重复或变化的展示位置。'
)
assert.match(
  page,
  /async function\s+handleTestRow\(row:[\s\S]*if\s*\(testingRowCaseName\.value\s*!==\s*undefined\)\s*\{[\s\S]*已有测试正在执行[\s\S]*return[\s\S]*const\s+historyKey/,
  '处理函数必须同步阻止快速重复点击创建多个执行批次。'
)
assert.match(
  page,
  /async function\s+handleTestRow\(row:[\s\S]*const\s+historyKey\s*=\s*getRowTestHistoryKey\(row\)[\s\S]*clearRowTestHistory\(historyKey,\s*row\.title\)[\s\S]*await\s+CodexTestApi\.startCodeReadonlyCodexTestExecution/,
  '点击测试必须在启动请求前只清空当前行历史。'
)
assert.doesNotMatch(
  page,
  /async function\s+handleTestRow\(row:[\s\S]*testResult\.visible\s*=\s*true[\s\S]*startCodeReadonlyCodexTestExecution/,
  '点击测试不得继续自动打开全局结果弹窗，回复必须从当前行历史入口查看。'
)
assert.match(
  page,
  /async function\s+handleTestRow\(row:[\s\S]*history\.executionId\s*=\s*executionId[\s\S]*pollCodexTestExecutionResult\(historyKey,\s*executionId\)/,
  '启动成功后必须把 executionId 写入当前行并携带 historyKey 轮询。'
)
assert.match(
  page,
  /async function\s+pollCodexTestExecutionResult\(historyKey:\s*string,\s*executionId:\s*number\)[\s\S]*pollToken\s*!==\s*resultPollToken[\s\S]*history\?\.executionId\s*!==\s*executionId[\s\S]*history\.historyKey\s*!==\s*historyKey[\s\S]*return/,
  '轮询写入必须同时校验 poll token、executionId 和 historyKey。'
)
assert.match(
  page,
  /terminalExecutionStatuses\.has\(execution\.status\)[\s\S]*history\.data\s*=\s*execution[\s\S]*history\.ready\s*=\s*true/,
  '只有正式终态执行结果可以写入历史并启用对应结果颜色。'
)
assert.match(
  page,
  /function\s+openRowTestHistory\(row:\s*BatchRecordTestRow\)[\s\S]*isRowTestHistoryReady\(row\)[\s\S]*testResult\.historyKey\s*=\s*history\.historyKey[\s\S]*testResult\.executionId\s*=\s*history\.executionId[\s\S]*testResult\.data\s*=\s*history\.data[\s\S]*testResult\.visible\s*=\s*true/,
  '点击历史必须把当前行已完成快照复制到结果弹窗，不能展示最后一次全局回复。'
)
assert.match(
  page,
  /function\s+clearRowTestHistory\(historyKey:\s*string,\s*rowTitle:\s*string\)[\s\S]*rowTestHistories\[historyKey\]\s*=\s*\{[\s\S]*ready:\s*false[\s\S]*data:\s*undefined/,
  '重新测试必须移除当前行旧回复并恢复灰色状态。'
)
assert.doesNotMatch(page, /catch\s*\{\s*\}/, '逐行历史不得通过空 catch 吞掉执行或读取错误。')

console.log('edhr-batch-record-test-row-history-static PASS')
