const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const nasPage = read('src/views/system/nas/index.vue')
const nasApi = read('src/api/system/nas/index.ts')
const workflowApi = read('src/api/dcc/controlledFile/workflow.ts')
const packageJson = JSON.parse(read('package.json'))

assert.equal(
  packageJson.scripts['e2e:dcc:nas-uncontrolled-local-import:static'],
  'node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js',
  'package.json must expose the NAS uncontrolled local import static contract.'
)

for (const required of [
  'DccNasControlAuditFileRespVO',
  'auditFileId',
  'sourceSignature',
  'expectedLocalRelativePath',
  'downloadStatus',
  'archiveErrorCode'
]) {
  assert.match(nasApi, new RegExp(required), `NAS API wrapper missing audit file field: ${required}`)
}

for (const requiredEndpoint of [
  /url: `\/dcc\/controlled-files\/nas-control-audit\/\$\{taskId\}\/files`/,
  /url: `\/dcc\/controlled-files\/nas-control-audit\/\$\{taskId\}\/files\/recognize`/,
  /url: `\/dcc\/controlled-files\/nas-control-audit\/\$\{taskId\}\/import-selected`/,
  /url: `\/dcc\/controlled-files\/nas-uncontrolled-import\/tasks\/\$\{importTaskId\}\/files\/\$\{auditFileId\}\/content`/,
  /url: `\/dcc\/controlled-files\/nas-uncontrolled-import\/tasks\/\$\{importTaskId\}\/files\/\$\{auditFileId\}\/local-write-result`/
]) {
  assert.match(nasApi, requiredEndpoint, `NAS API wrapper missing endpoint: ${requiredEndpoint}`)
}

assert.match(
  nasApi,
  /downloadNasUncontrolledImportContent[\s\S]*request\.download<Blob>[\s\S]*sourceSignature[\s\S]*localRelativePath/,
  'content download wrapper must request binary bytes with sourceSignature/localRelativePath snapshot params.'
)
assert.match(
  nasApi,
  /recordNasUncontrolledImportLocalWriteResult[\s\S]*request\.post[\s\S]*localWriteStatus/,
  'local-write-result wrapper must POST the terminal local write status.'
)
assert.match(
  workflowApi,
  /ControlledFileNasTransferSourceType\s*=\s*'NAS'\s*\|\s*'LOCAL_FOLDER'\s*\|\s*'NAS_UNCONTROLLED_IMPORT'/,
  'existing transfer response type must include NAS_UNCONTROLLED_IMPORT instead of narrowing it away.'
)

assert.match(nasPage, /showDirectoryPicker/, 'page must request a user-authorized local directory.')
assert.match(
  nasPage,
  /showDirectoryPicker[\s\S]*validateNasUncontrolledLocalRelativePath[\s\S]*importSelectedNasUncontrolledFiles/,
  'page must not create backend import-selected task before directory authorization and local relative path validation.'
)
assert.match(
  nasPage,
  /importSelectedNasUncontrolledFiles[\s\S]*downloadNasUncontrolledImportContent[\s\S]*recordNasUncontrolledImportLocalWriteResult/,
  'page must create import task, download each selected file, then report local write result in order.'
)
assert.match(nasPage, /getDirectoryHandle[\s\S]*getFileHandle[\s\S]*createWritable/, 'page must write into nested local directories.')
assert.match(nasPage, /await writable\.write\(blob\)[\s\S]*await writable\.close\(\)/, 'page must close the local writable stream after writing bytes.')
assert.match(
  nasPage,
  /await writable\.close\(\)[\s\S]*localWriteStatus:\s*'LOCAL_WRITTEN'/,
  'page must only report LOCAL_WRITTEN after writable.close() succeeds.'
)
assert.match(
  nasPage,
  /catch[\s\S]*localWriteStatus:\s*'LOCAL_WRITE_FAILED'[\s\S]*localWriteErrorCode:\s*'LOCAL_WRITE_FAILED'/,
  'page must report LOCAL_WRITE_FAILED when local write fails.'
)

for (const pathGuard of [
  /relativePath\.includes\('\\\\'\)/,
  /relativePath\.startsWith\('\/'\)/,
  /\^\[A-Za-z\]:/,
  /segment === '\.\.'/,
  /segment === '\.'/
]) {
  assert.match(nasPage, pathGuard, `local relative path guard missing: ${pathGuard}`)
}

assert.doesNotMatch(
  nasPage,
  /localAbsolutePath|absoluteLocalPath/,
  'frontend must not store or send the user local absolute directory path.'
)
assert.match(
  nasPage,
  /ARCHIVE_METADATA_REQUIRED[\s\S]*归档元数据/,
  'page must surface ARCHIVE_METADATA_REQUIRED as an explicit pending metadata state.'
)
assert.match(
  nasPage,
  /未分类\/待处理|UNCLASSIFIED_PENDING|AMBIGUOUS/,
  'page must keep unrecognized files visibly in 未分类/待处理 instead of silently treating them as matched.'
)
