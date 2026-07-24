import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('batch record report API module exposes fixed-route recognition endpoint', () => {
  const source = readText('src/api/mes/pro/batchrecordreport/index.ts')
  assert.match(source, /recognizeFixedRoute/)
  assert.match(source, /routeKey/)
  assert.match(source, /\/mes\/pro\/batch-record-report\/recognize-fixed/)
  assert.match(source, /recognizeUploadedRoute/)
  assert.match(source, /\/mes\/pro\/batch-record-report\/recognize-uploaded/)
  assert.match(source, /data\.append\('file', file\)/)
  assert.match(source, /data\.append\('routeKey', routeKey\)/)
  assert.match(source, /data\.append\('batchRecordName', batchRecordName\)/)
  assert.match(source, /data\.append\('upgrade', String\(upgrade\)\)/)
  assert.match(source, /existsBatchRecordName/)
  assert.match(source, /\/mes\/pro\/batch-record-report\/exists/)
  assert.match(source, /deleteAllGeneratedReports/)
  assert.match(source, /\/mes\/pro\/batch-record-report\/delete-all/)
  assert.match(source, /params:\s*\{\s*confirm\s*\}/)
  assert.match(source, /skippedBoundReportCount/)
})

test('fixed-route recognition and delete-all APIs return unwrapped backend payloads', () => {
  const source = readText('src/api/mes/pro/batchrecordreport/index.ts')
  const recognizeBlock = source.match(/recognizeFixedRoute:\s*async\s*\(routeKey:\s*string\)\s*=>\s*\{[\s\S]*?\n\s*\},/)?.[0] || ''
  const deleteAllBlock =
    source.match(/deleteAllGeneratedReports:\s*async\s*\(confirm:\s*string\)\s*=>\s*\{[\s\S]*?\n\s*\}/)?.[0] || ''

  assert.match(recognizeBlock, /request\.post<BatchRecordReportImportResultVO>/)
  assert.doesNotMatch(recognizeBlock, /return\s+result\.data/)
  assert.match(deleteAllBlock, /request\.delete<BatchRecordReportDeleteAllRespVO>/)
  assert.doesNotMatch(deleteAllBlock, /return\s+result\.data/)
})

test('report management recognition actions surface backend failures', () => {
  const source = readText('src/views/report/jmreport/index.vue')
  const recognizeBlock = source.match(/const handleRecognize = async[\s\S]*?\nconst openDesigner/)?.[0] || ''
  const deleteAllBlock = source.match(/const handleDeleteAll = async[\s\S]*?\nwatch\(/)?.[0] || ''

  assert.match(recognizeBlock, /catch\s*\(error/)
  assert.match(recognizeBlock, /message\.error\(resolveErrorMessage\(error/)
  assert.match(deleteAllBlock, /catch\s*\(error/)
  assert.match(deleteAllBlock, /message\.error\(resolveErrorMessage\(error/)
})

test('report management delete-all requires PROD prompt before API call', () => {
  const source = readText('src/views/report/jmreport/index.vue')
  const deleteAllBlock = source.match(/const handleDeleteAll = async[\s\S]*?\nwatch\(/)?.[0] || ''

  assert.match(source, /删除全部批记录模板/)
  assert.match(deleteAllBlock, /message\.prompt\(/)
  assert.match(deleteAllBlock, /PROD/)
  assert.match(deleteAllBlock, /confirmation\.trim\(\)\s*!==\s*'PROD'/)
  assert.match(deleteAllBlock, /BatchRecordReportApi\.deleteAllGeneratedReports\('PROD'\)/)
  assert.match(deleteAllBlock, /skippedBoundReportCount/)
  assert.doesNotMatch(deleteAllBlock, /message\.delConfirm/)
})

test('report management jimureport page exposes second tab with six route buttons', () => {
  const source = readText('src/views/report/jmreport/index.vue')
  assert.match(source, /el-tabs/)
  assert.match(source, /六路识别|6 路识别|六种识别/)
  assert.match(source, /routeKey/)
  assert.match(source, /A/)
  assert.match(source, /B/)
  assert.match(source, /C/)
  assert.match(source, /D/)
  assert.match(source, /E/)
  assert.match(source, /F/)
  assert.match(source, /getGeneratedReportPage/)
  assert.match(source, /删除全部批记录模板/)
  assert.match(source, /handleDeleteAll/)
  assert.match(source, /deleteAllGeneratedReports/)
  assert.doesNotMatch(source, /文件导入/)
  assert.doesNotMatch(source, /pendingImportFile/)
  assert.doesNotMatch(source, /pendingBatchRecordName/)
})

test('report management recognition buttons keep fixed-route behavior without file import', () => {
  const source = readText('src/views/report/jmreport/index.vue')
  const recognizeBlock = source.match(/const handleRecognize = async[\s\S]*?\nconst openDesigner/)?.[0] || ''

  assert.doesNotMatch(source, /type="file"/)
  assert.doesNotMatch(source, /accept="\.doc"/)
  assert.doesNotMatch(source, /handleImportFileChange/)
  assert.doesNotMatch(recognizeBlock, /existsBatchRecordName/)
  assert.doesNotMatch(recognizeBlock, /recognizeUploadedRoute/)
  assert.match(recognizeBlock, /recognizeFixedRoute\(routeKey\)/)
  assert.match(recognizeBlock, /message\.error\(resolveErrorMessage\(error/)
})
