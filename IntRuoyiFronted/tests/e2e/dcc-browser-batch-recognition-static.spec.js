const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')

const extractConstFunctionBlock = (source, functionName) => {
  const marker = `const ${functionName} = `
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `missing function block: ${functionName}`)
  const nextConst = source.indexOf('\n\nconst ', start + marker.length)
  return nextConst === -1 ? source.slice(start) : source.slice(start, nextConst)
}

const browserRequestParamsBlock = extractConstFunctionBlock(browserPage, 'buildBrowserRequestParams')
const onMountedStart = browserPage.indexOf('onMounted(async () => {')
assert.notEqual(onMountedStart, -1, 'missing browser page onMounted block')
const onBeforeUnmountStart = browserPage.indexOf('onBeforeUnmount', onMountedStart)
assert.notEqual(onBeforeUnmountStart, -1, 'missing browser page onBeforeUnmount block')
const onMountedBlock = browserPage.slice(onMountedStart, onBeforeUnmountStart)

assert.equal(
  packageJson.scripts['e2e:dcc:browser-batch-recognition:static'],
  'node tests/e2e/dcc-browser-batch-recognition-static.spec.js',
  'package.json must expose the DCC browser batch recognition static contract'
)

for (const required of [
  /export interface ControlledFileBatchRecognitionCreateReqVO/,
  /recognitionType: 'BASIC_INFO' \| 'FILE_CATEGORY' \| 'FILE_NUMBER'/,
  /scope: 'CURRENT' \| 'GLOBAL'/,
  /directoryId\?: number/,
  /includeDescendantDirectories\?: boolean/,
  /keyword\?: string/,
  /status\?: string/,
  /categoryId\?: number/,
  /overwriteExisting: boolean/,
  /existingRecordPolicy: string/,
  /syncFileNameTitle: boolean/,
  /workerCount\?: number/,
  /export interface ControlledFileBatchRecognitionTaskRespVO/,
  /workerCount\?: number \| null/,
  /activeWorkerCount\?: number \| null/,
  /recordedFileCount\?: number \| null/,
  /existingRecordPolicy: string/,
  /totalCount: number/,
  /processedCount: number/,
  /successCount: number/,
  /failedCount: number/,
  /skippedExistingCount: number/,
  /remainingCount: number/,
  /lastFailureMessage\?: string \| null/,
  /export const createControlledFileBatchRecognitionTask = async/,
  /url: '\/dcc\/controlled-files\/batch-recognition\/tasks'/,
  /export const getControlledFileBatchRecognitionTask = async/,
  /url: `\/dcc\/controlled-files\/batch-recognition\/tasks\/\$\{taskId\}`/,
  /export const exportControlledFileRecognitionRecordExcel = async/,
  /url: '\/dcc\/controlled-files\/recognition-records\/export-excel'/,
  /export interface ControlledFileRecognitionMigrationImportPreviewRespVO/,
  /export const exportControlledFileRecognitionMigrationExcel = async/,
  /url: '\/dcc\/controlled-files\/recognition-records\/migration-export-excel'/,
  /export const previewControlledFileRecognitionMigrationImport = async/,
  /url: '\/dcc\/controlled-files\/recognition-records\/migration-import-preview'/,
  /export const confirmControlledFileRecognitionMigrationImport = async/,
  /url: '\/dcc\/controlled-files\/recognition-records\/migration-import-confirm'/
]) {
  assert.match(workflowApi, required, `workflow API missing batch recognition contract: ${required}`)
}

for (const required of [
  /v-if="canEditMetadata"/,
  /data-testid="dcc-browser-batch-recognition-trigger"/,
  /data-testid="dcc-browser-file-number-recognition-trigger"/,
  /data-testid="dcc-browser-recognition-record-export"/,
  /data-testid="dcc-browser-recognition-migration-export"/,
  /data-testid="dcc-browser-recognition-migration-import"/,
  /导出记录/,
  /导出迁移/,
  /导入迁移/,
  /title="导入识别迁移包"/,
  /data-testid="dcc-browser-recognition-migration-preview"/,
  /data-testid="dcc-browser-recognition-migration-confirm"/,
  /可应用/,
  /不可应用/,
  /失败原因/,
  /识别当前文件夹及子文件夹/,
  /title="识别当前文件夹及子文件夹"/,
  /title="批量识别进度"/,
  /data-testid="dcc-browser-batch-recognition-worker-count"/,
  /data-testid="dcc-browser-batch-recognition-success-records"/,
  /data-testid="dcc-browser-batch-recognition-failed-records"/,
  /data-testid="dcc-browser-batch-recognition-policy"/,
  /data-testid="dcc-browser-batch-recognition-confirm"/,
  /message\.warning\('请先选择目录'\)/,
  /if \(isBatchRecognitionTaskActive\(batchRecognitionTask\.value\)\) \{[\s\S]*batchRecognitionProgressVisible\.value = true/,
  /const BATCH_RECOGNITION_POLICY_SKIP_ALL_EXISTING = 'SKIP_ALL_EXISTING'/,
  /const BATCH_RECOGNITION_POLICY_RETRY_FAILED = 'RETRY_FAILED'/,
  /const BATCH_RECOGNITION_POLICY_OVERWRITE_ALL = 'OVERWRITE_ALL'/,
  /const FILE_NUMBER_RECOGNITION_TYPE = 'FILE_NUMBER'/,
  /const batchRecognitionForm = reactive\(\{\s*existingRecordPolicy: BATCH_RECOGNITION_POLICY_RETRY_FAILED,\s*workerCount: 5\s*\}\)/,
  /exportControlledFileRecognitionRecordExcel\(buildBrowserRequestParams\(\)\)/,
  /download\.excel\(data, '受控文件识别记录\.xlsx'\)/,
  /exportControlledFileRecognitionMigrationExcel\(buildBrowserRequestParams\(\)\)/,
  /download\.excel\(data, '受控文件识别结果迁移包\.xlsx'\)/,
  /previewControlledFileRecognitionMigrationImport\(recognitionMigrationImportFile\.value\)/,
  /confirmControlledFileRecognitionMigrationImport\(recognitionMigrationImportFile\.value\)/,
  /const buildBatchRecognitionReq = \(\): ControlledFileBatchRecognitionCreateReqVO => \(\{/,
  /recognitionType: 'BASIC_INFO'/,
  /scope: isCurrentDirectorySearch\.value[\s\S]*BATCH_RECOGNITION_SCOPE_CURRENT[\s\S]*BATCH_RECOGNITION_SCOPE_GLOBAL/,
  /directoryId: isCurrentDirectorySearch\.value \? selectedDirectoryId\.value : undefined/,
  /includeDescendantDirectories: isCurrentDirectorySearch\.value/,
  /keyword: normalizeKeyword\(queryParams\.keyword\)/,
  /status: queryParams\.status/,
  /categoryId: queryParams\.categoryId/,
  /recognitionStatus: queryParams\.recognitionStatus/,
  /UNKNOWN_DCC_BASIC_DATA/,
  /UNRECOGNIZED_PROJECT_NAME/,
  /未知基础数据/,
  /未识别项目名称/,
  /batchRecognitionTaskId: queryParams\.batchRecognitionTaskId/,
  /overwriteExisting: batchRecognitionForm\.existingRecordPolicy === BATCH_RECOGNITION_POLICY_OVERWRITE_ALL/,
  /existingRecordPolicy: batchRecognitionForm\.existingRecordPolicy/,
  /syncFileNameTitle: true/,
  /workerCount: batchRecognitionForm\.workerCount/,
  /const buildFileNumberRecognitionReq = \(existingRecordPolicy: string\): ControlledFileBatchRecognitionCreateReqVO => \(\{/,
  /recognitionType: FILE_NUMBER_RECOGNITION_TYPE/,
  /syncFileNameTitle: false/,
  /workerCount: 1/,
  /ElMessageBox\.confirm\(/,
  /包含 DCC 项目代码或项目名称/,
  /confirmButtonText: '覆盖已有'/,
  /cancelButtonText: '跳过已有'/,
  /distinguishCancelAndClose: true/,
  /createControlledFileBatchRecognitionTask\(buildFileNumberRecognitionReq\(existingRecordPolicy\)\)/,
  /createControlledFileBatchRecognitionTask\(buildBatchRecognitionReq\(\)\)/,
  /getControlledFileBatchRecognitionTask\(taskId\)/,
  /startBatchRecognitionPolling\(task\.taskId\)/,
  /await refreshList\(\)/,
  /总数/,
  /配置 Codex/,
  /运行 Codex/,
  /已记录文件/,
  /已处理/,
  /成功/,
  /失败/,
  /成功 \+ 失败 = 总数/,
  /跳过成功，重试失败和未识别/,
  /之前失败、未知基础数据、未识别项目名称、未匹配和没有台账的文件会重新识别/,
  /showBatchRecognitionRecords\('SUCCESS'\)/,
  /showBatchRecognitionRecords\('FAILED'\)/,
  /const clearBatchRecognitionRecordFilters = async \(\) => \{/,
  /queryParams\.recognitionStatus = undefined/,
  /queryParams\.batchRecognitionTaskId = undefined/,
  /await syncRouteFromBrowserState\(\)/,
  /if \(queryParams\.batchRecognitionTaskId && canEditMetadata\.value\) \{[\s\S]*const task = await loadBatchRecognitionTaskSnapshot\(queryParams\.batchRecognitionTaskId\)/,
  /if \(isBatchRecognitionTaskActive\(task\)\) \{[\s\S]*startBatchRecognitionPolling\(task\.taskId\)/,
  /剩余/,
  /当前状态/,
  /最后错误/,
  /识别成功后自动同步 fileName\/title\/productName\/productCode\/dccProjectCodeId/
]) {
  assert.match(browserPage, required, `browser page missing batch recognition UI contract: ${required}`)
}

for (const required of [
  /const buildBrowserRequestParams = \(\): ControlledFilePageReqVO => \{/,
  /categoryId: queryParams\.categoryId/,
  /recognitionStatus: queryParams\.recognitionStatus/,
  /batchRecognitionTaskId: queryParams\.batchRecognitionTaskId/,
  /requestParams\.includeDescendantDirectories = false/,
  /latestVersionOnly: true/
]) {
  assert.match(browserRequestParamsBlock, required, `browser request params missing export/list contract: ${required}`)
}

assert.doesNotMatch(
  browserPage,
  /data-testid="dcc-browser-batch-recognition-stop"|stopBatchRecognitionTask|batchRecognitionStopping|停止识别/,
  'batch recognition progress dialog must not expose manual stop; it should run until the selected folder is finished'
)

assert.ok(
  onMountedBlock.indexOf('queryParams.batchRecognitionTaskId && canEditMetadata.value') <
    onMountedBlock.indexOf('loadBatchRecognitionTaskSnapshot(queryParams.batchRecognitionTaskId)'),
  'read-only users must not call doc_control-only batch recognition task snapshot during file browser startup'
)

assert.doesNotMatch(
  onMountedBlock,
  /if \(queryParams\.batchRecognitionTaskId\) \{[\s\S]*loadBatchRecognitionTaskSnapshot\(queryParams\.batchRecognitionTaskId\)/,
  'file browser startup must guard batch recognition task restore by metadata edit permission'
)

assert.doesNotMatch(
  browserPage,
  /完全匹配 DCC 项目代码页签的项目名称/,
  'file-number recognition dialog must describe contains matching instead of the old exact project-name matching rule'
)

assert.doesNotMatch(
  browserPage,
  /mock|placeholder data|静默成功|fallback|降级/i,
  'batch recognition browser page must not introduce mock data, silent success, fallback, or downgrade logic'
)

console.log('PASS: DCC browser batch recognition static contract')
