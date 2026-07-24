import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..', '..')
const controllerPath = resolve(
  repoRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordExecutionAttachmentController.java'
)
const reqVoPath = resolve(
  repoRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionAttachmentPrepareUploadReqVO.java'
)
const respVoPath = resolve(
  repoRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.java'
)
const junitPath = resolve(
  repoRoot,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordExecutionAttachmentControllerTest.java'
)

for (const sourcePath of [controllerPath, reqVoPath, respVoPath, junitPath]) {
  assert.ok(existsSync(sourcePath), `missing source file: ${sourcePath}`)
}

const controller = readFileSync(controllerPath, 'utf8')
const reqVo = readFileSync(reqVoPath, 'utf8')
const respVo = readFileSync(respVoPath, 'utf8')
const junit = readFileSync(junitPath, 'utf8')

assert.match(
  controller,
  /@RequestMapping\("\/mes\/pro\/batch-record-execution\/attachment"\)/,
  'controller must keep the eDHR attachment base route'
)
assert.match(controller, /@PostMapping\("\/prepare-upload"\)/, 'prepareUpload must remain a POST endpoint')
assert.match(
  controller,
  /@PreAuthorize\("@ss\.hasPermission\('mes:pro-batch-record-execution:field-audit-update'\)"\)/,
  'prepareUpload must keep field-audit update permission'
)
assert.match(
  controller,
  /@RequestPart\("file"\)\s+MultipartFile\s+file/,
  'prepareUpload must accept the uploaded file as multipart part named file'
)

for (const token of [
  'getLoginUserId()',
  'file.getOriginalFilename()',
  'file.getContentType()',
  'file.getBytes()',
  '.setExecutionId(reqVO.getExecutionId())',
  '.setWorkTaskId(reqVO.getWorkTaskId())',
  '.setOperatorId(getLoginUserId())',
  '.setFileName(file.getOriginalFilename())',
  '.setContentType(file.getContentType())',
  '.setContent(file.getBytes())',
]) {
  assert.ok(controller.includes(token), `controller must preserve command mapping token: ${token}`)
}

for (const field of ['executionId', 'workTaskId']) {
  assert.match(reqVo, new RegExp(`@NotNull[\\s\\S]+private Long ${field};`), `request field ${field} must be required`)
}

for (const setter of [
  'setUploadToken',
  'setFileId',
  'setFileUrl',
  'setStorageConfigId',
  'setStoragePath',
  'setFileName',
  'setContentType',
  'setFileSize',
  'setSha256',
  'setStorageRetentionJson',
  'setStorageRetentionHash',
]) {
  assert.ok(controller.includes(`.${setter}(result.get`), `response mapper must include ${setter}`)
}

for (const field of [
  'uploadToken',
  'fileId',
  'fileUrl',
  'storageConfigId',
  'storagePath',
  'fileName',
  'contentType',
  'fileSize',
  'sha256',
  'storageRetentionJson',
  'storageRetentionHash',
]) {
  assert.match(respVo, new RegExp(`private [^;]+ ${field};`), `response VO must expose ${field}`)
}

for (const testToken of [
  'prepareUpload_delegatesMultipartFileAndReturnsStructuredMetadata',
  'mappingsAndPermissions_matchFrozenAttachmentPrepareUploadContract',
  'MockMultipartFile',
  'ArgumentCaptor<MesProBatchRecordExecutionAttachmentPrepareUploadCommand>',
  'assertArrayEquals(content, captor.getValue().getContent())',
  'getStorageRetentionHash',
]) {
  assert.ok(junit.includes(testToken), `JUnit contract must keep ${testToken}`)
}

console.log('edhr attachment prepare-upload backend contract passed')
