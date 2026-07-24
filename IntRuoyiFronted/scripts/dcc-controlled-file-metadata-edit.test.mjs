import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')
const browserSource = readText('src/views/dcc/controlled-file/browser/index.vue')
const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')
const dialogSource = fs.existsSync(
  path.join(root, 'src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue')
)
  ? readText('src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue')
  : ''

const extractInterfaceBody = (source, interfaceName) => {
  const match = source.match(new RegExp(`export interface ${interfaceName} \\{([\\s\\S]*?)\\n\\}`))
  assert.ok(match, `${interfaceName} must be exported`)
  return match[1]
}

test('BDD: metadata API contract -> Given doc_control edits controlled file basics, When frontend calls backend, Then it uses PUT metadata endpoint with productName and required fields', () => {
  const updateBody = extractInterfaceBody(workflowSource, 'ControlledFileMetadataUpdateReqVO')
  const fileBody = extractInterfaceBody(workflowSource, 'ControlledFileVO')

  assert.match(updateBody, /dccProjectCodeId\?:\s*number\s*\|\s*null/)
  assert.match(updateBody, /needTraining:\s*boolean/)
  for (const field of ['fileTypeLevel1', 'fileTypeLevel2', 'fileTypeLevel3', 'fileTypeLevel4', 'fileTypeLevel5']) {
    assert.match(updateBody, new RegExp(`${field}\\?:\\s*string\\s*\\|\\s*null`))
    assert.match(fileBody, new RegExp(`${field}\\?:\\s*string\\s*\\|\\s*null`))
  }
  assert.match(updateBody, /productName\?:\s*string/)
  assert.match(updateBody, /fileName:\s*string/)
  assert.match(updateBody, /productCode\?:\s*string/)
  assert.match(updateBody, /fileNumber:\s*string/)
  assert.match(updateBody, /categoryId:\s*number/)
  assert.match(updateBody, /directoryId:\s*number/)
  assert.match(fileBody, /productName\?:\s*string/)
  assert.match(workflowSource, /export const updateControlledFileMetadata = async/)
  assert.match(workflowSource, /request\.put\(\{\s*url:\s*`\/dcc\/controlled-files\/\$\{id\}\/metadata`,\s*data\s*\}\)/)
})

test('BDD: doc_control-only entry -> Given current roles contain or miss doc_control, When browser and detail render, Then no generic role helper or super_admin shortcut exposes the metadata editor', () => {
  for (const [name, source] of [
    ['browser', browserSource],
    ['detail', detailSource]
  ]) {
    assert.match(source, /DOC_CONTROL_ROLE_CODE\s*=\s*'doc_control'/, `${name} must use doc_control role code`)
    assert.match(source, /roles\.includes\(DOC_CONTROL_ROLE_CODE\)/, `${name} must check roles.includes directly`)
    assert.match(source, /修改基础信息/, `${name} must expose the edit entry for doc_control`)
    assert.doesNotMatch(source, /super_admin/, `${name} must not treat super_admin as doc_control`)
    assert.doesNotMatch(source, /checkRole|hasRole/, `${name} must not use generic role helpers for metadata edit`)
  }
})

test('BDD: metadata dialog behavior -> Given save fails, When backend rejects metadata update, Then the dialog keeps the error visible and does not fake success', () => {
  assert.match(dialogSource, /ControlledFileMetadataDialog/)
  assert.match(dialogSource, /DCC基础条目/)
  assert.match(dialogSource, /getProjectCodePage/)
  assert.match(dialogSource, /metadataForm\.dccProjectCodeId/)
  assert.match(dialogSource, /培训要求/)
  assert.match(dialogSource, /metadataForm\.needTraining/)
  for (const label of ['文件类别 I', '文件类别 II', '文件类别 III', '文件类别 IV', '文件类别 V']) {
    assert.match(dialogSource, new RegExp(label))
  }
  for (const field of ['fileTypeLevel1', 'fileTypeLevel2', 'fileTypeLevel3', 'fileTypeLevel4', 'fileTypeLevel5']) {
    assert.match(dialogSource, new RegExp(`metadataForm\\.${field}`))
  }
  assert.match(dialogSource, /产品名称/)
  assert.match(dialogSource, /文件名称/)
  assert.match(dialogSource, /产品编号/)
  assert.match(dialogSource, /文件编号/)
  assert.match(dialogSource, /文件类别/)
  assert.match(dialogSource, /受控目录/)
  assert.match(dialogSource, /updateControlledFileMetadata/)
  assert.match(dialogSource, /metadataDialog\.inlineError/)
  assert.match(dialogSource, /catch\s*\(error\)\s*\{[\s\S]*metadataDialog\.inlineError\s*=/)
  assert.doesNotMatch(dialogSource, /catch\s*\(error\)\s*\{[\s\S]*closeMetadataDialog\(\)[\s\S]*\}/)
  assert.doesNotMatch(dialogSource, /el-form-item[^>]*label="流程实例"/)
  assert.doesNotMatch(dialogSource, /el-form-item[^>]*label="提交人"/)
})

test('BDD: category directory scope -> Given a target category is selected, When choosing storage path, Then frontend limits options to the category binding subtree and refreshes data after save', () => {
  assert.match(dialogSource, /category\.directoryId/)
  assert.match(dialogSource, /collectDirectoryOptions/)
  assert.match(dialogSource, /directoryOptions/)
  assert.match(browserSource, /@saved="handleMetadataSaved"/)
  assert.match(detailSource, /@saved="handleMetadataSaved"/)
  assert.match(browserSource, /await getList\(\)/)
  assert.match(detailSource, /await reloadAll\(\)/)
})

test('BDD: metadata display -> Given file type levels exist, When detail panel renders, Then it displays all five levels as read-only business metadata', () => {
  const basicInfoSource = readText('src/views/dcc/controlled-file/shared/ControlledFileBasicInfoPanel.vue')
  for (const label of ['文件类别 I', '文件类别 II', '文件类别 III', '文件类别 IV', '文件类别 V']) {
    assert.match(basicInfoSource, new RegExp(label))
  }
  for (const field of ['fileTypeLevel1', 'fileTypeLevel2', 'fileTypeLevel3', 'fileTypeLevel4', 'fileTypeLevel5']) {
    assert.match(basicInfoSource, new RegExp(`file\\?\\.${field}\\s*\\|\\|\\s*'-'`))
  }
})
