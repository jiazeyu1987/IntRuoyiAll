const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const packageJson = JSON.parse(fs.readFileSync(path.join(repoRoot, 'package.json'), 'utf8'))
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'),
  'utf8'
)
const workflowApiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/dcc/controlledFile/workflow.ts'),
  'utf8'
)

const extractConstBlock = (source, name) => {
  const marker = `const ${name} = `
  const start = source.indexOf(marker)
  assert.ok(start >= 0, `missing const block: ${name}`)
  const next = source.indexOf('\n\nconst ', start + marker.length)
  return next === -1 ? source.slice(start) : source.slice(start, next)
}

assert.strictEqual(
  packageJson.scripts['e2e:dcc:project-code-batch-ai-category:static'],
  'node tests/e2e/dcc-project-code-batch-ai-category-static.spec.js',
  'package.json must expose the DCC project-code batch AI category static test'
)

const exportButtonIndex = pageSource.indexOf('@click="handleExport"')
const batchButtonIndex = pageSource.indexOf('data-testid="dcc-project-code-batch-ai-category"')
assert.ok(exportButtonIndex >= 0, 'project-code toolbar must keep export button')
assert.ok(batchButtonIndex > exportButtonIndex, 'batch AI category button must render after export button')

for (const token of [
  '批量AI分类',
  'data-testid="dcc-project-code-batch-ai-category"',
  'v-if="canRunAiCategory"',
  ':loading="batchAiCategoryRunning"',
  '@click="handleBatchAiCategoryProjectCodes"',
  '<el-progress',
  'v-if="batchAiCategoryProgressVisible"',
  'data-testid="dcc-project-code-batch-ai-category-progress"',
  'data-testid="dcc-project-code-batch-ai-category-progress-close"',
  'aria-label="关闭批量AI分类进度"',
  '@click="handleCloseBatchAiCategoryProgress"',
  '已处理 {{ batchAiCategoryProcessed }}/{{ batchAiCategoryTotal }}',
  '状态：{{ batchAiCategoryStatusText }}',
  '失败文件 {{ batchAiCategoryFailedFileCount }}',
  'data-testid="dcc-project-code-batch-ai-category-view-failures"',
  '@click="handleViewBatchAiCategoryFailures"',
  '查看失败文件',
  'data-testid="dcc-project-code-batch-ai-category-export-failures"',
  '@click="handleExportBatchAiCategoryFailures"',
  '导出失败明细',
  'batchAiCategoryConsistencyMessage',
  '统计异常：',
  '结果合计',
  '与已处理',
  'data-testid="dcc-project-code-batch-ai-category-failure-summary"',
  '主要失败原因',
  'batchAiCategoryFailureSummaries',
  '失败阶段',
  'summary.stage',
  'summary.code',
  'summary.reason',
  'summary.count',
  '歧义文件',
  '{{ batchAiCategoryAmbiguousFileCount }} 个',
  '并发跳过',
  '{{ batchAiCategoryConflictFileCount }} 个',
  '已有记录',
  '{{ batchAiCategorySkippedFileCount }} 个',
  'v-if="batchAiCategoryInterruptionMessage"',
  '最近失败：{{ batchAiCategoryInterruptionMessage }}'
]) {
  assert.ok(pageSource.includes(token), `batch AI category UI must expose token: ${token}`)
}

assert.ok(
  pageSource.includes('batchAiCategoryDismissedTaskId') &&
    pageSource.includes("task.status !== 'COMPLETED'") &&
    pageSource.includes('batchAiCategoryDismissedTaskId.value !== task.taskId'),
  'completed batch AI category progress must stay hidden and user-dismissed tasks must remain hidden'
)

for (const token of [
  'failureSummaries?: ControlledFileBatchRecognitionFailureSummaryVO[]',
  'export interface ControlledFileBatchRecognitionFailureSummaryVO',
  'stage: string',
  'code: string',
  'reason: string',
  'count: number'
]) {
  assert.ok(workflowApiSource.includes(token), `batch task API must expose failure summary token: ${token}`)
}

assert.ok(
  pageSource.includes("checkPermi(['dcc:project-code:update'])") &&
    pageSource.includes("checkPermi(['dcc:controlled-file:update'])"),
  'batch AI category button must require both backend permissions'
)

const canRunBatchAiCategoryBlock = extractConstBlock(pageSource, 'canRunBatchAiCategory')
assert.ok(
  canRunBatchAiCategoryBlock.includes('canRunAiCategory.value') &&
    canRunBatchAiCategoryBlock.includes("checkRole(['doc_control'])"),
  'batch AI category startup restore must require both update permissions and doc_control role'
)

const restoreLatestBatchAiCategoryTaskBlock = extractConstBlock(
  pageSource,
  'restoreLatestBatchAiCategoryTask'
)
assert.ok(
  /if \(!canRunBatchAiCategory\.value\) \{[\s\S]*return[\s\S]*\}/.test(
    restoreLatestBatchAiCategoryTaskBlock
  ),
  'project-code tab must not call latest batch AI category task API for assigned-only correctors'
)
assert.ok(
  restoreLatestBatchAiCategoryTaskBlock.indexOf('if (!canRunBatchAiCategory.value)') <
    restoreLatestBatchAiCategoryTaskBlock.indexOf('getLatestControlledFileBatchRecognitionTask'),
  'permission guard must run before getLatestControlledFileBatchRecognitionTask'
)

for (const token of [
  'createControlledFileBatchRecognitionTask',
  'exportControlledFileRecognitionRecordExcel',
  'getControlledFileBatchRecognitionTask',
  'getLatestControlledFileBatchRecognitionTask',
  "recognitionType: 'FILE_CATEGORY'",
  "scope: 'GLOBAL'",
  'const startBatchAiCategoryPolling',
  'const restoreLatestBatchAiCategoryTask',
  'batchAiCategoryTask.value = task',
  'task.unclassifiedCount',
  'task.ambiguousCount',
  'task.conflictCount',
  "path: '/dcc/controlled-file/browser'",
  "recognitionStatus: 'FAILED'",
  'batchRecognitionTaskId: String(taskId)',
  "download.excel(data, 'DCC批量AI分类失败明细.xlsx')",
  'onBeforeUnmount'
]) {
  assert.ok(pageSource.includes(token), `batch AI category must use persistent task token: ${token}`)
}

const apiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/dcc/controlledFile/projectCodes.ts'),
  'utf8'
)
assert.ok(
  apiSource.includes("classificationStatus?: 'MATCHED' | 'UNCLASSIFIED' | 'AMBIGUOUS'") &&
    apiSource.includes('classificationMessage?: string | null'),
  'AI category response must expose structured classification status and message'
)

assert.ok(
  !pageSource.includes('for (const projectCode of projectCodes)') &&
    !pageSource.includes('classifyProjectCodeAssociatedFileByAi(projectCode.id, candidate.fileId)'),
  'batch AI category must not execute browser-side nested classification loops'
)

assert.ok(
  pageSource.includes(':disabled="batchAiCategoryRunning"') &&
    pageSource.includes('associatedFilesLoading || batchAiCategoryRunning'),
  'batch AI category must disable conflicting toolbar and detail actions while running'
)

console.log('PASS: DCC project-code batch AI category static contract')
