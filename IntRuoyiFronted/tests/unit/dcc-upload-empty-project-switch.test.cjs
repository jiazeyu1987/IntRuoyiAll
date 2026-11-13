const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/upload/index.vue'), 'utf8')
const block = (start, end) => source.slice(source.indexOf(start), source.indexOf(end, source.indexOf(start)))
const ref = value => ({ value })

function setup({ library = false, cancel = false } = {}) {
  const calls = []
  const context = {
    exports: {}, ref, reactive: value => value, isExternalReview: ref(false), isLibraryUpload: ref(library),
    DEFAULT_MANUAL_VERSION_NO: 'A/1', DEFAULT_CONTROLLED_INITIAL_VERSION_NO: 'A/1',
    resolveProcessTypeByRoute: () => 'CONTROLLED_FILE', libraryScope: ref('GENERAL'), acceptedLibraryScope: library ? 'GENERAL' : 'PROJECT',
    acceptedProjectCodeId: ref('5'), acceptedProjectCodeOption: ref(), projectCodeOptions: ref([{ id: '6' }]),
    projectAttributesSelectionSequence: 0, projectAttributesLoading: ref(false),
    projectAttributesRestoreSnapshot: ref(), projectAttributesPanelKey: ref(0),
    projectAttributesPanel: ref({ selectProject: async () => true }),
    fileList: ref([]), attachmentFileList: ref([]),
    hasTemporaryUploadState: () => false,
    cleanupCurrentUploadSession: async () => { calls.push('cleanup'); return true },
    message: {
      confirm: async () => { calls.push('confirm'); if (cancel) throw 'cancel' },
      error: value => { throw new Error(value) }
    },
    projectFolderRequestSequence: 0, projectFoldersLoading: ref(false), projectFoldersProjectId: ref(),
    projectFolders: ref([]), projectFoldersError: ref(''), selectedUploadRelations: ref([]),
    uploadRelationsVisible: ref(false), uploadRelationSource: ref(), approvalMatrixState: ref(),
    approvalMatrixScopeKey: '', routeReadinessRequestSeq: 0, routeReadiness: ref(),
    routeReadinessSelectionKey: ref(''), signoffSelectionCategoryKey: '',
    resetProjectFileTemplateSelection: () => calls.push('reset'), applyDccProjectCodeProductNumber: () => {},
    loadUploadProjectFolders: async () => {}, loadProjectFileTemplate: async () => {}
  }
  const code = block('const createInitialUploadDraft =', 'const fileTypeTaxonomyPathMap =')
    + block('const uploadProjectAttributesEdited =', 'const projectFolderTree =')
    + '\nexports.formData = formData; exports.capture = captureProjectAttributes; exports.run = handleProjectCodeChange'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  context.exports.formData.dccProjectCodeId = '6'
  return { ...context.exports, context, calls }
}

test('empty project form switches or clears project without confirmation', async () => {
  for (const requested of ['6', null]) {
    const state = setup()
    state.formData.dccProjectCodeId = requested
    await state.run()
    assert.deepEqual(state.calls, ['cleanup', 'reset'])
    assert.equal(state.context.acceptedProjectCodeId.value, requested)
  }
})

test('empty QMS/general form can clear its library project during a scope switch', async () => {
  const state = setup({ library: true })
  state.formData.dccProjectCodeId = null
  state.formData.projectAttributes = { libraryScope: 'QMS', targetMarkets: null }
  await state.run()
  assert.deepEqual(state.calls, ['cleanup', 'reset'])
})

test('automatically loaded project attributes alone do not prompt', async () => {
  const state = setup()
  const actual = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
  state.capture({ applicationType: 'UPLOAD', projectId: '6', actual, defaultSource: actual })
  await state.run()
  assert.deepEqual(state.calls, ['cleanup', 'reset'])
})

test('cancelling a project clear preserves entered fields, relationships and selected files', async () => {
  for (const input of [
    { projectFolderId: '500' }, { fileName: '说明书' }, { fileNumber: 'SOP-01' },
    { relatedControlledFileIds: ['8'] }, { selectedSignoffDepartmentIds: ['9'] },
    { selectedDistributionDepartmentIds: ['10'] }, { mainFile: true }, { attachment: true }
  ]) {
    const state = setup({ cancel: true })
    state.formData.dccProjectCodeId = null
    Object.assign(state.formData, input)
    if (input.mainFile) state.context.fileList.value = [{ uid: 1 }]
    if (input.attachment) state.context.attachmentFileList.value = [{ uid: 2 }]
    await state.run()
    assert.deepEqual(state.calls, ['confirm'])
    assert.equal(state.formData.dccProjectCodeId, '5')
    for (const [key, value] of Object.entries(input)) assert.deepEqual(state.formData[key], value)
    assert.equal(state.context.fileList.value.length, input.mainFile ? 1 : 0)
    assert.equal(state.context.attachmentFileList.value.length, input.attachment ? 1 : 0)
  }
})

test('user edits to project attributes still prompt on project switch', async () => {
  const state = setup({ cancel: true })
  const defaults = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
  state.capture({ applicationType: 'UPLOAD', projectId: '6', actual: { ...defaults, licenseHolder: 'N' }, defaultSource: defaults })
  await state.run()
  assert.deepEqual(state.calls, ['confirm'])
  assert.equal(state.formData.dccProjectCodeId, '5')
})

test('confirming a populated project clear continues cleanup and resets project choices', async () => {
  const state = setup()
  state.formData.dccProjectCodeId = null
  state.formData.projectFolderId = '500'
  await state.run()
  assert.deepEqual(state.calls, ['confirm', 'cleanup', 'reset'])
  assert.equal(state.formData.projectFolderId, null)
  assert.equal(state.context.acceptedProjectCodeId.value, null)
})
