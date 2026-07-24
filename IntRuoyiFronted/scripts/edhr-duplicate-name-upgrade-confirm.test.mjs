import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const pageSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/batchrecordtemplate/index.vue'),
  'utf8'
)

const importBlock =
  pageSource.match(/const runUploadedWordImport = async[\s\S]*?\nconst handleSubmitVersionApproval/)?.[0] || ''

test('duplicate batch record name asks before upgrade and can abandon import', () => {
  assert.match(importBlock, /existsBatchRecordName\(wordImportRouteKey,\s*batchRecordName\)/)
  assert.match(importBlock, /是否升版/)
  assert.match(importBlock, /生成新版本快照[\s\S]*审批通过后才会覆盖后续可用版本/)
  assert.match(importBlock, /选择否将放弃本次导入/)
  assert.match(importBlock, /ElMessageBox\.confirm/)
  assert.match(importBlock, /confirmButtonText:\s*'升版'/)
  assert.match(importBlock, /cancelButtonText:\s*'否，放弃本次导入'/)
  assert.match(importBlock, /catch\s*\{[\s\S]*?clearWordImportState\(\)[\s\S]*?return[\s\S]*?\}/)
  assert.ok(
    importBlock.indexOf('ElMessageBox.confirm') < importBlock.indexOf('BatchRecordReportApi.recognizeUploadedRoute'),
    'duplicate-name prompt must happen before the write import API'
  )
  assert.ok(
    importBlock.indexOf('upgrade = true') < importBlock.indexOf('BatchRecordReportApi.recognizeUploadedRoute'),
    'upgrade flag must only be set before continuing the write import API'
  )
})
