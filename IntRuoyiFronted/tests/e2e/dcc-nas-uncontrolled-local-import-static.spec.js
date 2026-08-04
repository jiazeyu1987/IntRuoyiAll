const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const nasPage = read('src/views/system/nas/index.vue')
const nasApi = read('src/api/system/nas/index.ts')
const workflowApi = read('src/api/dcc/controlledFile/workflow.ts')
const realE2E = read('tests/e2e/dcc-nas-uncontrolled-local-import-real.e2e.js')
const packageJson = JSON.parse(read('package.json'))

assert.equal(
  packageJson.scripts['e2e:dcc:nas-uncontrolled-local-import:static'],
  'node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js',
  'package.json must expose the NAS uncontrolled local import static contract.'
)
assert.equal(
  packageJson.scripts['e2e:dcc:nas-uncontrolled-local-import:real:check'],
  'node tests/e2e/dcc-nas-uncontrolled-local-import-real.e2e.js --check',
  'package.json must expose the NAS uncontrolled local import real E2E prerequisite gate.'
)
assert.equal(
  packageJson.scripts['e2e:dcc:nas-uncontrolled-local-import:real'],
  'node tests/e2e/dcc-nas-uncontrolled-local-import-real.e2e.js',
  'package.json must expose the NAS uncontrolled local import full real E2E command.'
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
assert.match(
  nasPage,
  /\['MATCHED',\s*'UNCLASSIFIED_PENDING',\s*'AMBIGUOUS'\]\.includes\([\s\S]*classificationStatus/,
  'page must allow unresolved files with a backend pending local path to be selected for local download.'
)
assert.doesNotMatch(
  nasPage,
  /请先选择已唯一匹配的未受控文件/,
  'local download selection prompt must not exclude 未分类/待处理 rows that have a pending local path.'
)
assert.match(
  nasPage,
  /selectionScope:\s*'EXPLICIT_SELECTED_FILES'/,
  'frontend must send the backend import-selected selection scope contract.'
)

for (const realGateToken of [
  'dcc_nas_control_audit_file',
  'dcc_controlled_file_nas_transfer_task',
  'dcc_controlled_file_nas_transfer_task_item',
  'dcc_project_code_id',
  'selected_import_task_id',
  'selected_import_task_item_id',
  'archive_category_id_snapshot',
  'archive_effective_date_snapshot',
  'DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID',
  'MYSQL_ROOT_PASSWORD',
  'PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH',
  'showDirectoryPicker',
  'FileSystemDirectoryHandle',
  'FileSystemFileHandle',
  'MATCHED',
  'UNCLASSIFIED_PENDING',
  'AMBIGUOUS',
  'ARCHIVE_METADATA_REQUIRED',
  '_未分类待处理',
  'PENDING_PATH',
  'DCC_NAS_UNCONTROLLED_IMPORT_LOCAL_DIR',
  'verifySharedNasSourceFiles',
  'NAS_EXISTING_FILE_READY',
  'syncTaskOwnedFixtureToExistingNasFiles',
  'installDirectoryPickerHarness',
  'runFullPageFlow',
  'DCC_NAS_UNCONTROLLED_LOCAL_IMPORT_REAL_E2E_PASS'
]) {
  assert.match(realE2E, new RegExp(realGateToken), `real E2E prerequisite gate missing token: ${realGateToken}`)
}

assert.match(
  realE2E,
  /const requiredTaskColumns = \[[\s\S]*'dcc_project_code_id'[\s\S]*\]/,
  'real E2E schema gate must include transfer task dcc_project_code_id because the backend mapper selects it.'
)

assert.match(
  realE2E,
  /process\.exit\(1\)[\s\S]*DCC_NAS_UNCONTROLLED_LOCAL_IMPORT_REAL_CHECK_PASS/,
  'real E2E gate must fail fast on blockers and only print PASS after all preconditions pass.'
)
assert.doesNotMatch(
  realE2E,
  /full real page flow requires authorized shared NAS source files/,
  'full real E2E mode must execute the authorized source-file path instead of staying on the old authorization blocker.'
)
assert.doesNotMatch(
  realE2E,
  /DCC_NAS_UNCONTROLLED_IMPORT_ALLOW_NAS_WRITE|WriteAllBytes|New-Item\s+-ItemType\s+Directory/i,
  'full real E2E must verify existing shared NAS source files without creating or overwriting NAS files.'
)
assert.match(
  realE2E,
  /System\.Security\.Cryptography\.SHA256[\s\S]*local file sha256 changed/,
  'full real E2E must prove local files match existing NAS source bytes by SHA-256 instead of extension assumptions.'
)
assert.doesNotMatch(
  realE2E,
  /subarray\(0,\s*4\)[\s\S]*%PDF/,
  'full real E2E must not assume existing NAS files with .pdf names start with the %PDF magic header.'
)
assert.match(
  realE2E,
  /showDirectoryPicker[\s\S]*getDirectoryHandle[\s\S]*getFileHandle[\s\S]*createWritable[\s\S]*write[\s\S]*close/,
  'full real E2E must drive the browser local-directory write contract through the page harness.'
)
assert.doesNotMatch(
  realE2E,
  /localAbsolutePath|absoluteLocalPath|zip|default download/i,
  'real E2E gate must not introduce local absolute path persistence or browser download fallback.'
)
