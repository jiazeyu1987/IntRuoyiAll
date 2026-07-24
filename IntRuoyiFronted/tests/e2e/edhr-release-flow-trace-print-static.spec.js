const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const formTraceReleaseTab = read('src/views/mes/pro/edhr/form-trace/FormTraceReleaseTab.vue')

const releaseFlowStepsModelStart = detailPage.indexOf('const releaseFlowStepsViewModel = computed')
const releaseFlowStepsModelEnd = detailPage.indexOf(
  'const terminalReleaseActionItems',
  releaseFlowStepsModelStart
)
assert.ok(
  releaseFlowStepsModelStart > 0 && releaseFlowStepsModelEnd > releaseFlowStepsModelStart,
  '必须能定位放行流程步骤 ViewModel。'
)
const releaseFlowStepsModel = detailPage.slice(
  releaseFlowStepsModelStart,
  releaseFlowStepsModelEnd
)

for (const retainedStep of ["key: 'close'", "key: 'precheck'", "key: 'release-approval'"]) {
  assert.match(
    releaseFlowStepsModel,
    new RegExp(retainedStep),
    `放行流程图必须保留步骤：${retainedStep}`
  )
}
assert.doesNotMatch(
  releaseFlowStepsModel,
  /key:\s*'archive'[\s\S]*title:\s*'归档打印'/,
  '放行流程图不得继续渲染“归档打印”卡片。'
)
assert.doesNotMatch(
  releaseFlowStepsModel,
  /key:\s*'archived'[\s\S]*title:\s*'已归档'/,
  '放行流程图不得继续渲染“已归档”卡片。'
)

assert.match(
  formTraceReleaseTab,
  /getLatestEdhrBatchArchive[\s\S]*printEdhrBatchArchive/,
  '表单追溯放行 Tab 必须复用现有最终归档查询和打印 API。'
)
assert.match(
  formTraceReleaseTab,
  /v-hasPermi="\['mes:pro-edhr-batch-execution-archive:download'\]"[\s\S]*:disabled="row\.releaseStatus !== 'RELEASED' \|\| printLoadingBatchExecutionId === row\.batchExecutionId"[\s\S]*@click="handlePrintArchive\(row\)"[\s\S]*打印/,
  '表单追溯放行记录必须在已放行后提供受权限控制的打印入口。'
)
assert.match(
  formTraceReleaseTab,
  /const handlePrintArchive = async \(row: EdhrReleaseRowVO\) => \{[\s\S]*row\.releaseStatus !== 'RELEASED'[\s\S]*getLatestEdhrBatchArchive\(row\.batchExecutionId\)[\s\S]*printEdhrBatchArchive\(archive\.id, archive\.fileName\)[\s\S]*finally/,
  '打印处理必须先校验正式放行状态，再查询最新归档并打开打印窗口，同时保证 loading 收尾。'
)
assert.match(
  formTraceReleaseTab,
  /当前批次没有可打印的打印版 PDF/,
  '最新归档不存在时必须显示明确错误，不能默认成功。'
)

console.log('PASS: eDHR release flow archive cards removed and form trace print contract')
