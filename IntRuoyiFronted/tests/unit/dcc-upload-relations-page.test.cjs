const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const page = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
const source = () => {
  const begin = page.indexOf('const buildUploadRelationSource =')
  const end = page.indexOf('const handleFileTypeTaxonomyChange =', begin)
  assert.notEqual(begin, -1, 'public upload must build a real unsubmitted source')
  assert.notEqual(end, -1)
  return page.slice(begin, end)
}
const candidate = { tenantId: '1', controlledFileId: '9007199254740993', masterId: '9007199254740995',
  projectId: '6', projectName: '另一项目', fileName: '受控.pdf', fileNumber: 'N-2', versionNo: 'B/1',
  folderName: '质量', controlled: true, pendingEffect: true, executable: false, canPreview: false }
const state = () => ({
  formData: { dccProjectCodeId: '5', projectFolderId: '500', fileNumber: 'N-1', versionNo: 'A/1', relatedControlledFileIds: [] },
  selectedProjectCode: { value: { id: '5', projectName: '编制项目' } },
  selectedProjectFolder: { value: { id: '500', name: '文件夹' } },
  previewUpload: { value: { sessionId: 'upload-session', fileName: '真实原文件.docx' } },
  projectFoldersLoading: { value: false }, projectFoldersError: { value: '' },
  selectedUploadRelations: { value: [] }, uploadRelationSource: { value: undefined }, uploadRelationsVisible: { value: false },
  getTenantId: () => 1, getVisitTenantId: () => undefined,
  referenceIdentity: value => String(value), message: { warning() {}, error() {} }
})
const handlers = state => {
  const context = { ...state, exports: {}, Error, JSON, Promise }
  const mapping = { exports: {}, Error, JSON, BigInt, Set, Number }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/relations/project-reference-contract.ts', 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, mapping)
  context.mapReferenceDirectoryNodes = mapping.exports.mapReferenceDirectoryNodes
  context.projectFolderTree = { value: [{ id: '500', projectCodeId: '5', parentId: '0', name: '文件夹', active: true, sortOrder: 0, children: [] }] }
  context.uploadRelationDirectories = { value: [] }
  vm.runInNewContext(ts.transpileModule(source() + '\nexports.api={buildUploadRelationSource,openUploadRelations,persistUploadRelations}',
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports.api
}
test('public upload uses the formal relation dialog and independent logical folder field', () => {
  assert.match(page, /<DccFileSelector[\s\S]*?purpose="relations"/)
  assert.match(page, /v-model="formData.projectFolderId"/)
  assert.doesNotMatch(page, /status: 'ACTIVE'[\s\S]{0,200}page\.list\.filter/)
})
test('opening the dialog retains actual file and source identity without inventing File or Master IDs', () => {
  const current = state(); const api = handlers(current); api.openUploadRelations()
  const initial = current.uploadRelationSource.value
  assert.equal(initial.fileName, '真实原文件.docx'); assert.equal(initial.folderId, '500')
  assert.equal(initial.controlledFileId, undefined); assert.equal(initial.masterId, undefined)
  assert.equal(initial.unsubmitted, true); assert.equal(current.uploadRelationsVisible.value, true)
  assert.deepEqual(current.formData.relatedControlledFileIds, [])
})
test('confirmed cross-project future controlled selection preserves the complete Long string', async () => {
  const current = state(); const api = handlers(current); api.openUploadRelations()
  await api.persistUploadRelations([candidate])
  assert.deepEqual(JSON.parse(JSON.stringify(current.formData.relatedControlledFileIds)), ['9007199254740993'])
  assert.equal(current.selectedUploadRelations.value[0].projectId, '6')
})
test('source changes and foreign tenant reject the selection without replacing parent input', async () => {
  const current = state(); const api = handlers(current); api.openUploadRelations()
  current.formData.projectFolderId = '501'
  await assert.rejects(api.persistUploadRelations([candidate]), /变化/)
  assert.deepEqual(current.formData.relatedControlledFileIds, [])
  current.formData.projectFolderId = '500'
  await assert.rejects(api.persistUploadRelations([{ ...candidate, tenantId: '2' }]), /租户/)
  assert.deepEqual(current.formData.relatedControlledFileIds, [])
})
