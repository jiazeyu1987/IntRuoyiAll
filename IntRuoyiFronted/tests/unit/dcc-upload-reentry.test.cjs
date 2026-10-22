const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const source = parse(fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8'))
  .descriptor.scriptSetup.content
const ast = ts.createSourceFile('upload.ts', source, ts.ScriptTarget.Latest, true)
const nodeFor = (name) =>
  ast.statements.find(
    (s) =>
      ts.isVariableStatement(s) && s.declarationList.declarations.some((d) => d.name.text === name)
  )
function setup() {
  const node = nodeFor('resetCompletedUploadDraft')
  assert.ok(node, 'successful cached page must have a real reentry lifecycle')
  const c = {
    exports: {},
    Object,
    JSON,
    Map,
    Set,
    ref: (value) => ({ value }),
    formData: {
      fileNumber: 'OLD-1',
      fileName: 'old.pdf',
      categoryId: 9,
      dccProjectCodeId: 8,
      projectFolderId: 2,
      projectFolderChangeReason: 'old',
      relatedControlledFileIds: ['4'],
      selectedSignoffDepartmentIds: ['7'],
      projectAttributes: { targetMarkets: ['CE'] },
      needTraining: true,
      effectiveDate: '2026-10-06'
    },
    uploadSessionId: 'OLD-SESSION',
    createControlledFileUploadSessionId: () => 'NEW-SESSION',
    resolveProcessTypeByRoute: () => 'CONTROLLED_FILE',
    isExternalReview: { value: false },
    DEFAULT_MANUAL_VERSION_NO: 'V1.0',
    DEFAULT_CONTROLLED_INITIAL_VERSION_NO: 'A/1',
    clearTimeout: () => {},
    nextAttachmentUploadGeneration: () => {},
    syncAttachmentUploadLoading: () => {},
    clearSubmitFieldErrors: () => {},
    directoryReads: [],
    formRef: { value: { clearValidate: () => {} } },
    uploadRef: { value: { clearFiles: () => {} } },
    drawingPdfUploadRef: { value: { clearFiles: () => {} } },
    attachmentUploadRef: { value: { clearFiles: () => {} } }
  }
  for (const m of source.matchAll(/(?:const|let) (\w+) = (?:ref[^\n]*|reactive[^\n]*)/g))
    if (!(m[1] in c)) c[m[1]] = { value: [] }
  for (const m of source.matchAll(/let (\w+(?:Sequence|Seq)) = 0/g)) c[m[1]] = 1
  c.currentVersionLookupTimer = undefined
  c.uploadSubmitted = { value: true }
  c.submitLoading = { value: false }
  c.previewUpload = { value: { uploadTicket: 'BOUND-TICKET' } }
  c.editableUpload = { value: { uploadTicket: 'EDITABLE-BOUND-TICKET' } }
  c.editableUploadLoading = { value: true }
  c.attachmentUploadAttempts = { value: [{ uid: 'a', status: 'READY' }] }
  c.loadApprovalUsers = () => { c.directoryReads.push('users') }
  c.loadSignoffDepartments = () => { c.directoryReads.push('departments') }
  const functions = [
    'createInitialUploadDraft',
    'resetUploadNameLinkage',
    'clearCurrentVersionInfo',
    'resetUploadNameContext',
    'resetUploadDirectoryContext',
    'resetSelectedPreview',
    'resetEditableUpload',
    'resetDrawingPdfUpload',
    'resetAttachmentUploads',
    'resetCategorySelectionForFileTypeTaxonomyChange',
    'resetProjectFileTemplateSelection',
    'resetCompletedUploadDraft'
  ]
  const code =
    functions
      .map((n) => {
        const node = nodeFor(n)
        assert.ok(node, n)
        return node.getText(ast)
      })
      .join('\n') + '\nexports.reset=resetCompletedUploadDraft'
  vm.runInNewContext(
    ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText,
    c
  )
  return c
}
test('successful cached upload starts a fresh independent session without reusing bound tickets', () => {
  const c = setup()
  c.exports.reset()
  assert.equal(c.uploadSubmitted.value, false)
  assert.equal(c.uploadSessionId, 'NEW-SESSION')
  assert.equal(c.formData.fileNumber, '')
  assert.equal(c.formData.fileName, '')
  assert.equal(c.formData.dccProjectCodeId, null)
  assert.equal(c.formData.needTraining, false)
  assert.equal(c.formData.projectAttributes, undefined)
  assert.equal(c.formData.selectedSignoffDepartmentIds, undefined)
  assert.equal(c.formData.selectedDistributionDepartmentIds, undefined)
  assert.equal(c.formData.effectiveMode, null)
  assert.equal(c.formData.effectiveDate, null)
  assert.equal(c.formData.versionNo, 'A/1')
  assert.equal(c.previewUpload.value, undefined)
  assert.equal(c.editableUpload.value, undefined)
  assert.equal(c.editableUploadLoading.value, false)
  assert.ok(c.editableUploadRequestSeq > 1)
  assert.equal(c.uploadSessionBinding.value, undefined)
  assert.equal(c.attachmentUploads.value.length, 0)
  assert.ok(
    source.includes('onActivated(resetCompletedUploadDraft)'),
    'real page activation must bind the reset'
  )
  assert.deepEqual(c.directoryReads, ['users', 'departments'])
})
test('unsent draft and busy submission retain their current session and duplicate guard', () => {
  for (const state of ['UNSENT', 'BUSY']) {
    const c = setup()
    if (state === 'UNSENT') c.uploadSubmitted.value = false
    else c.submitLoading.value = true
    c.exports.reset()
    assert.equal(c.uploadSessionId, 'OLD-SESSION')
    assert.equal(c.formData.fileNumber, 'OLD-1')
    assert.equal(c.previewUpload.value.uploadTicket, 'BOUND-TICKET')
    assert.deepEqual(c.directoryReads, [])
  }
})
