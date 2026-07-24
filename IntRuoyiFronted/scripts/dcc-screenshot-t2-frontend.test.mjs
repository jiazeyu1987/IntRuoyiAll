import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('dcc upload payload carries screenshot metadata fields', () => {
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')
  const submitterSource = readText('src/views/dcc/controlled-file/upload/submitter.ts')

  for (const field of [
    'sourceFileId',
    'sourceFileName',
    'drawingPdfFileId',
    'productCode',
    'needTraining',
    'processType'
  ]) {
    assert.match(workflowSource, new RegExp(`${field}[?]?:|${field}:`), `workflow contract must expose ${field}`)
    assert.match(submitterSource, new RegExp(`${field}:`), `submit payload must include ${field}`)
  }
})

test('dcc upload page validates product code and drawing pdf before submit', () => {
  const uploadSource = readText('src/views/dcc/controlled-file/upload/index.vue')
  const submitterSource = readText('src/views/dcc/controlled-file/upload/submitter.ts')

  assert.match(uploadSource, /prop="productCode"/)
  assert.match(uploadSource, /v-model="formData\.needTraining"/)
  assert.match(uploadSource, /drawingPdfUpload/)
  assert.match(submitterSource, /validateProductCode/)
  assert.match(submitterSource, /isDrawingSourceFile/)
})

test('dcc upload page restricts source files to editable whitelist and keeps drawing pdf explicit', () => {
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')
  const uploadSource = readText('src/views/dcc/controlled-file/upload/index.vue')
  const submitterSource = readText('src/views/dcc/controlled-file/upload/submitter.ts')

  for (const extension of ['doc', 'docx', 'xls', 'xlsx', 'dwg', 'sldprt', 'sldasm', 'slddrw']) {
    assert.match(submitterSource, new RegExp(`['"]${extension}['"]`), `source whitelist must include ${extension}`)
    assert.match(uploadSource, new RegExp(`\\.${extension}`), `source upload accept must include .${extension}`)
  }

  assert.doesNotMatch(uploadSource, /任意类型/, 'source upload copy must not claim any type is accepted')
  assert.match(submitterSource, /validateControlledFileSelection[\s\S]*可编辑源文件/)
  assert.match(workflowSource, /UploadPreviewPurpose/)
  assert.match(workflowSource, /SOURCE/)
  assert.match(workflowSource, /DRAWING_PDF/)
  assert.match(uploadSource, /uploadPreview\(file\.raw as File,\s*['"]SOURCE['"]\)/)
  assert.match(uploadSource, /uploadPreview\(file\.raw as File,\s*['"]DRAWING_PDF['"]\)/)
})

test('dcc download flow requires user confirmation and backend confirmation parameter', () => {
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')

  assert.match(workflowSource, /nonControlledWarningConfirmed:\s*true/)
  assert.match(workflowSource, /confirmControlledFileDownload/)
  assert.match(workflowSource, /确认下载/)
  assert.match(workflowSource, /下载后的文件为非受控文件/)
})

test('dcc lists and detail show modifying status from backend', () => {
  const mineSource = readText('src/views/dcc/controlled-file/mine/index.vue')
  const browserSource = readText('src/views/dcc/controlled-file/browser/index.vue')
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')

  assert.match(mineSource, /row\.modifying/)
  assert.match(browserSource, /getSelectedVersion\(row\)\.modifying|row\.modifying/)
  assert.match(detailSource, /fileDetail\?\.modifying/)
  assert.match(`${mineSource}\n${browserSource}\n${detailSource}`, /修改中/)
})

test('dcc withdrawn applicant actions expose delete flow and resubmit entries', () => {
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')
  const mineSource = readText('src/views/dcc/controlled-file/mine/index.vue')
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')
  const presentationSource = readText('src/views/dcc/controlled-file/mine/presentation.ts')

  assert.match(workflowSource, /deleteWithdrawnControlledFile/)
  assert.match(workflowSource, /\/dcc\/controlled-files\/\$\{id\}\/withdrawn-flow/)
  assert.match(workflowSource, /resubmitWithdrawnControlledFile/)
  assert.match(workflowSource, /\/dcc\/controlled-files\/\$\{id\}\/resubmit/)
  assert.match(`${mineSource}\n${detailSource}`, /删除流程/)
  assert.match(`${mineSource}\n${detailSource}`, /重新提交/)
  assert.match(presentationSource, /isMineRowWithdrawnActionable/)
  assert.match(presentationSource, /supersededByFileId/)
  assert.match(mineSource, /isMineRowWithdrawnActionable\(row\.status,\s*row\.supersededByFileId\)/)
  assert.match(detailSource, /canHandleWithdrawnFlow/)
  assert.match(detailSource, /!fileDetail\.value\?\.supersededByFileId/)
})
