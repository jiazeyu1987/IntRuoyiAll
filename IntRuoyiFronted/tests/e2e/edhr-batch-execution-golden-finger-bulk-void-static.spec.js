const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const listPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
  'utf8'
)
const batchApi = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/batchExecution.ts'), 'utf8')
const changeApi = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/change.ts'), 'utf8')

assert.doesNotMatch(
  listPage,
  /金手指一键作废/,
  '工具栏和弹窗文案不得继续展示“金手指一键作废”。'
)

assert.match(
  listPage,
  /<el-table[\s\S]*row-key="id"[\s\S]*@selection-change="handleBatchExecutionSelectionChange"/,
  '批次执行表格必须配置 row-key 和 selection-change，保证复选与表头全选可用。'
)

assert.match(
  listPage,
  /<el-table-column[\s\S]*v-if="hasGoldenFingerPermission"[\s\S]*type="selection"[\s\S]*:selectable="isGoldenFingerBulkVoidSelectableRow"/,
  '金手指用户必须看到可复选的批次执行选择列，并限制不可作废行被勾选。'
)

assert.match(
  listPage,
  /const isGoldenFingerBulkVoidSelectableRow\s*=\s*\(row:\s*EdhrBatchExecutionRespVO\)\s*=>[\s\S]*hasGoldenFingerPermission\.value[\s\S]*resolveBatchVoidOperationState\(row\)\s*===\s*'normal'/,
  '选择列使用的 isGoldenFingerBulkVoidSelectableRow 必须在 setup 中定义，并只允许正式可作废状态。'
)

assert.match(
  listPage,
  /aria-label="批量作废"[\s\S]*@click="openGoldenFingerBulkVoidDialog"[\s\S]*批量作废/,
  '蓝框工具栏按钮必须显示“批量作废”，并直接打开批量作废弹窗。'
)

assert.doesNotMatch(
  listPage,
  /选择当前页可作废批次|selectCurrentPageGoldenFingerBulkVoidRows/,
  '工具栏不得继续暴露“选择当前页可作废批次”旧入口。'
)

assert.match(
  listPage,
  /当前筛选条件[\s\S]*跨页[\s\S]*可作废批次/,
  '金手指批量作废弹窗必须明确提示按当前筛选条件跨页作废可作废批次。'
)

assert.match(
  listPage,
  /已勾选[\s\S]*selectedGoldenFingerBulkVoidIds\.length[\s\S]*个批次/,
  '金手指批量作废弹窗必须提示当前已勾选批次数。'
)

assert.match(
  listPage,
  /const selectedGoldenFingerBulkVoidIds\s*=\s*computed\(\(\)\s*=>[\s\S]*selectedGoldenFingerBulkVoidRows\.value[\s\S]*\.map\(\(row\)\s*=>\s*Number\(row\.id\)\)/,
  '模板使用的 selectedGoldenFingerBulkVoidIds 必须由已选行派生，避免 setup 缺失导致列表白屏。'
)

assert.match(
  listPage,
  /const selectableGoldenFingerBulkVoidCurrentPageCount\s*=\s*computed\(\(\)\s*=>\s*list\.value\.filter\(isGoldenFingerBulkVoidSelectableRow\)\.length\)/,
  '模板使用的 selectableGoldenFingerBulkVoidCurrentPageCount 必须由当前页可作废行派生。'
)

assert.match(
  listPage,
  /const handleBatchExecutionSelectionChange\s*=\s*\(rows:\s*EdhrBatchExecutionRespVO\[\]\)\s*=>\s*\{[\s\S]*selectedGoldenFingerBulkVoidRows\.value\s*=\s*rows\.filter\(isGoldenFingerBulkVoidSelectableRow\)/,
  '表格 selection-change 必须同步可作废的已选行。'
)

assert.doesNotMatch(
  listPage,
  /batchExecutionTableRef\.value|toggleRowSelection\(row,\s*true\)/,
  '批量作废工具栏入口不得保留旧的当前页预选实现。'
)

assert.match(
  listPage,
  /goldenFingerBulkVoidEdhrBatchExecutions\(\{[\s\S]*filter:\s*buildGoldenFingerBulkVoidFilter\(\)/,
  '批量作废提交必须发送 buildGoldenFingerBulkVoidFilter 结果，保留勾选批次或当前筛选条件。'
)

const submitGoldenFingerBulkVoidStart = listPage.indexOf('const submitGoldenFingerBulkVoid = async () => {')
const submitGoldenFingerBulkVoidEnd = listPage.indexOf(
  'const submitVoidBatchExecution',
  submitGoldenFingerBulkVoidStart
)
assert.notEqual(submitGoldenFingerBulkVoidStart, -1, '必须存在金手指批量作废提交函数。')
assert.notEqual(submitGoldenFingerBulkVoidEnd, -1, '必须能定位金手指批量作废提交函数边界。')
const submitGoldenFingerBulkVoidBody = listPage.slice(
  submitGoldenFingerBulkVoidStart,
  submitGoldenFingerBulkVoidEnd
)

assert.doesNotMatch(
  submitGoldenFingerBulkVoidBody,
  /resolveVoidBatchExecutionApproval/,
  '金手指批量作废不得调用单条作废审批解析接口。'
)

const submitVoidBatchExecutionStart = listPage.indexOf('const submitVoidBatchExecution = async () => {')
const submitVoidBatchExecutionEnd = listPage.indexOf('const handleViewArchive', submitVoidBatchExecutionStart)
assert.notEqual(submitVoidBatchExecutionStart, -1, '必须存在原单条作废提交函数。')
assert.notEqual(submitVoidBatchExecutionEnd, -1, '必须能定位原单条作废提交函数边界。')
const submitVoidBatchExecutionBody = listPage.slice(
  submitVoidBatchExecutionStart,
  submitVoidBatchExecutionEnd
)

assert.match(
  changeApi,
  /requestVoidBatchExecution[\s\S]*\/mes\/pro\/edhr-change\/void-batch-execution\/request/,
  '单条作废申请 API 必须继续指向原 BPM 作废申请接口。'
)

assert.match(
  submitVoidBatchExecutionBody,
  /requestVoidBatchExecution\(\{/,
  '单条作废提交必须继续调用原作废申请 API。'
)

assert.doesNotMatch(
  submitVoidBatchExecutionBody,
  /goldenFingerBulkVoidEdhrBatchExecutions/,
  '单条作废流程不得调用金手指批量直通接口。'
)

assert.match(
  batchApi,
  /export interface EdhrBatchExecutionGoldenFingerBulkVoidReqVO[\s\S]*filter:\s*EdhrBatchExecutionPageReqVO/,
  '批次执行 API 必须声明金手指批量作废请求，并包含当前筛选条件。'
)

assert.match(
  batchApi,
  /export interface EdhrBatchExecutionPageReqVO[\s\S]*batchExecutionIds\?:\s*number\[\]/,
  '批次执行筛选参数必须支持 batchExecutionIds，以便勾选后只作废所选批次。'
)

assert.match(
  batchApi,
  /goldenFingerBulkVoidEdhrBatchExecutions[\s\S]*\/golden-finger\/bulk-void/,
  '批次执行 API 必须调用金手指批量作废专用接口。'
)

assert.doesNotMatch(
  changeApi,
  /goldenFingerBulkVoid/,
  '金手指批量作废不能挂在正式作废审批 change API 中。'
)

console.log('PASS edhr batch execution golden finger bulk void static contract')
