import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('electronic batch record page imports a Word file with resolved route', () => {
  const source = readText('src/views/mes/pro/batchrecordtemplate/index.vue')
  const importBlock = source.match(/const handleImportFile[\s\S]*?\nconst openDesigner/)?.[0] || ''

  assert.match(source, /文件导入/)
  assert.match(source, /type="file"/)
  assert.match(source, /accept="\.doc,\.docx"/)
  assert.match(source, /ref="wordImportFileInputRef"/)
  assert.match(source, /@change="handleImportFileChange"/)
  assert.match(source, /batchRecordName|批记录名称/)
  assert.match(source, /批记录名称/)
  assert.match(source, /batch-record-record-list/)
  assert.doesNotMatch(source, /batch-record-toolbar-shell/)
  assert.match(source, /class="batch-record-word-import-input"/)
  assert.match(source, /const getWordImportDefaultBatchRecordName = \(fileName: string\)/)
  assert.match(source, /replace\(\/\\\.doc\$\/i,\s*''\)/)
  assert.match(source, /const resolveWordImportRouteKey = \(fileName: string\)/)
  assert.match(source, /wordImportDialog\.batchRecordName\s*=\s*getWordImportDefaultBatchRecordName\(file\.name\)/)
  assert.match(source, /v-model="wordImportDialog\.batchRecordName"/)
  assert.match(source, /confirmWordImportDialog/)
  assert.match(importBlock, /existsBatchRecordName\(wordImportRouteKey,\s*batchRecordName\)/)
  assert.match(importBlock, /是否升版/)
  assert.match(importBlock, /ElMessageBox\.confirm/)
  assert.match(importBlock, /confirmButtonText:\s*'升版'/)
  assert.match(importBlock, /cancelButtonText:\s*'否，放弃本次导入'/)
  assert.match(importBlock, /catch\s*\{[\s\S]*?clearWordImportState\(\)[\s\S]*?return[\s\S]*?\}/)
  assert.ok(
    importBlock.indexOf('ElMessageBox.confirm') < importBlock.indexOf('BatchRecordReportApi.recognizeUploadedRoute'),
    '同名升版确认必须发生在导入写接口调用之前'
  )
  assert.match(importBlock, /recognizeUploadedRoute\([\s\S]*file[\s\S]*wordImportRouteKey[\s\S]*batchRecordName[\s\S]*upgrade[\s\S]*productNames/)
  assert.match(importBlock, /clearWordImportState\(\)/)
  assert.match(importBlock, /await refreshBatchRecordNames\(batchRecordName\)\s*queryParams\.pageNo = 1[\s\S]*await getList\(\)/)
  assert.match(importBlock, /getList\(\)/)
})

test('electronic batch record page filters by batch record name dropdown', () => {
  const source = readText('src/views/mes/pro/batchrecordtemplate/index.vue')
  const listBlock = source.match(/const getAllReportsForSelectedBatchRecord = async[\s\S]*?\nconst getList/)?.[0] || ''
  const selectionBlock = source.match(/const selectBatchRecordName = async[\s\S]*?\nconst syncBatchRecordNameSelection/)?.[0] || ''

  assert.match(source, /selectedBatchRecordName/)
  assert.match(source, /batch-record-record-list/)
  assert.match(source, /batchRecordNameOptions/)
  assert.match(source, /getBatchRecordNameOptions/)
  assert.match(source, /selectBatchRecordName/)
  assert.match(selectionBlock, /queryParams\.batchRecordName = batchRecordName/)
  assert.match(
    listBlock,
    /batchRecordName:\s*targetReportId\.value\s*\?\s*undefined\s*:\s*selectedBatchRecordName\.value\s*\|\|\s*undefined/
  )
  assert.match(listBlock, /pageSize:\s*BATCH_RECORD_REPORT_LIST_PAGE_SIZE/)
  assert.match(source, /onMounted[\s\S]*handleRefresh\(\)/)
  assert.match(source, /const syncBatchRecordNameSelection = \(\)/)
  assert.match(source, /!batchRecordNameOptions\.value\.includes\(currentBatchRecordName\)/)
  assert.match(source, /queryParams\.batchRecordName = ''/)
  assert.match(source, /await refreshBatchRecordNames\(batchRecordName\)/)
})

test('electronic batch record API supports uploaded route upgrade contract', () => {
  const source = readText('src/api/mes/pro/batchrecordreport/index.ts')
  assert.match(source, /recognizeUploadedRoute:\s*async\s*\(/)
  assert.match(source, /batchRecordName:\s*string/)
  assert.match(source, /upgrade:\s*boolean/)
  assert.match(source, /data\.append\('routeKey', routeKey\)/)
  assert.match(source, /data\.append\('batchRecordName', batchRecordName\)/)
  assert.match(source, /data\.append\('upgrade', String\(upgrade\)\)/)
  assert.match(source, /WORD_IMPORT_REQUEST_TIMEOUT\s*=\s*10\s*\*\s*60\s*\*\s*1000/)
  assert.match(source, /timeout:\s*WORD_IMPORT_REQUEST_TIMEOUT/)
  assert.match(source, /existsBatchRecordName/)
  assert.match(source, /\/mes\/pro\/batch-record-report\/exists/)
  assert.match(source, /getBatchRecordNameOptions/)
  assert.match(source, /\/mes\/pro\/batch-record-report\/batch-record-names/)
})
