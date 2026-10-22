const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const fixtures = require('./dcc-upload-test-loader.cjs')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const evaluate = (code, state = {}) => {
  const context = { exports: {}, Error, JSON, Date, ...state }
  vm.runInNewContext(ts.transpileModule(code, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports
}
const attributes = evaluate(read('src/views/dcc/controlled-file/project-attributes/state.ts'))
const departments = fixtures.loadUploadDepartments()
const helpers = evaluate(read('src/views/dcc/controlled-file/upload/submitter.ts'), {
  require: id => {
    if (id.endsWith('approval-matrix')) return fixtures.loadUploadMatrix()
    if (id.endsWith('signoff-departments')) return departments
    assert.equal(id, '../project-attributes/state')
    return attributes
  }
})
const actual = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const draft = overrides => ({
  categoryId: 2, directoryId: null, fileName: '操作规程.pdf', fileNumber: 'G12-001',
  productMasterId: null, productCode: '', dccProjectCodeId: '5', projectFolderId: '9007199254740993',
  fileTypeTaxonomyId: '6', relatedControlledFileIds: [], needTraining: false,
  processType: 'CONTROLLED_FILE', changeType: 'NEW', versionNo: 'A/1',
  effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: null,
  projectAttributes: actual, selectedSignoffDepartmentIds: ['7'], selectedDistributionDepartmentIds: ['8'],
  approvalMatrixSelection: fixtures.matrixSelection(), ...overrides
})
const uploaded = { sessionId: 'g12-source-session', uploadTicket: 'g12-source-ticket', fileName: '操作规程.pdf' }

for (const [label, reason] of [['omitted', undefined], ['empty', ''], ['whitespace', ' \t\n ']]) {
  test(`G12 ${label} reason allows actual submit and working payloads without an invented explanation`, () => {
    for (const build of [helpers.buildSubmitPayload, helpers.buildWorkingDraftPayload]) {
      const payload = build(draft({ projectFolderChangeReason: reason }), uploaded)
      assert.equal(payload.projectFolderChangeReason, undefined)
      assert.equal(Object.hasOwn(JSON.parse(JSON.stringify(payload)), 'projectFolderChangeReason'), false)
      assert.equal(payload.projectFolderId, '9007199254740993')
      assert.equal(payload.dccProjectCodeId, '5')
      assert.equal(payload.originalUploadTicket, uploaded.uploadTicket)
      assert.equal(payload.idempotencyKey, uploaded.sessionId)
      assert.deepEqual(JSON.parse(JSON.stringify(payload.projectAttributes)), actual)
      assert.equal(payload.needTraining, false)
    }
  })
}

test('G12 meaningful reason and 500-character boundary preserve the actual trimmed user explanation', () => {
  for (const reason of [' 存放于本项目质量目录 ', '原'.repeat(500)]) {
    for (const build of [helpers.buildSubmitPayload, helpers.buildWorkingDraftPayload]) {
      assert.equal(build(draft({ projectFolderChangeReason: reason }), uploaded).projectFolderChangeReason, reason.trim())
    }
  }
})

test('G12 empty reason reaches injected submit/save transport while overlong reason stops before transport', async () => {
  const calls = []
  const service = helpers.createUploadSubmitterService({
    submit: async payload => { calls.push(['SUBMIT', payload]); return 71 },
    createWorking: async payload => { calls.push(['SAVE', payload]); return 72 }
  })
  assert.equal(await service.submit(draft(), uploaded), 71)
  assert.equal(await service.saveDraft(draft({ projectFolderChangeReason: '' }), uploaded), 72)
  assert.deepEqual(calls.map(call => call[0]), ['SUBMIT', 'SAVE'])
  for (const [, payload] of calls) assert.equal(payload.projectFolderChangeReason, undefined)
  for (const method of ['submit', 'saveDraft']) {
    await assert.rejects(service[method](draft({ projectFolderChangeReason: '长'.repeat(501) }), uploaded), /500/)
  }
  assert.equal(calls.length, 2)
})

test('G12 optional reason cannot remove logical folder identity, attribute or ticket validation', () => {
  for (const projectFolderId of [undefined, null, '', '0', '-1', 'not-an-id']) {
    assert.throws(() => helpers.buildSubmitPayload(draft({ projectFolderId }), uploaded), /有效的项目文件夹/)
  }
  assert.throws(() => helpers.buildSubmitPayload(draft({ projectAttributes: { ...actual, targetMarkets: [] } }), uploaded))
  assert.throws(() => helpers.buildSubmitPayload(draft(), { ...uploaded, uploadTicket: '' }), /上传票据/)
})

test('G12 external review does not acquire an unrelated folder explanation requirement', () => {
  const payload = helpers.buildSubmitPayload(draft({ processType: 'EXTERNAL_REVIEW', effectiveDate: '2099-01-01',
    projectFolderId: undefined, projectAttributes: undefined, projectFolderChangeReason: '长'.repeat(501) }), uploaded)
  assert.equal(payload.projectFolderChangeReason, undefined)
})

const page = parse(read('src/views/dcc/controlled-file/upload/index.vue')).descriptor
const ast = ts.createSourceFile('upload.ts', page.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = name => {
  const statement = ast.statements.find(node => ts.isVariableStatement(node)
    && node.declarationList.declarations.some(item => item.name.text === name))
  assert.ok(statement, `actual page declaration ${name}`)
  return statement.getText(ast)
}
const ref = value => ({ value })
const pageFreeze = (formData, overrides = {}) => {
  const calls = []
  const state = {
    formData, isExternalReview: ref(false), isCustomApprovalSource: ref(false), selectedFileTypeTaxonomyPath: ref({approvalSourceMode:'PUBLIC_MATRIX'}), buildUploadEffectiveChoice: helpers.buildUploadEffectiveChoice,
    resolveTodayDate: () => '2026-10-14', projectFoldersLoading: ref(false), projectFoldersError: ref(''),
    selectedProjectFolder: ref({ id: '9007199254740993', projectCodeId: '5', active: true }),
    projectAttributesLoading: ref(false), projectAttributesPanel: ref({ getSnapshot: () => ({
      applicationType: 'UPLOAD', projectId: '5', actual
    }) }), signoffDepartmentsLoading: ref(false), signoffDepartmentsError: ref(''),
    normalizeSignoffDepartments: departments.normalizeSignoffDepartments,
    normalizeDistributionDepartments: departments.normalizeDistributionDepartments,
    signoffDepartments: ref([{ id: '7' }, { id: '8' }]), routeReadinessSelectionKey: ref('g12-scope'),
    signoffRequestKey: () => 'g12-scope', approvalUsersLoading: ref(false), approvalUsersError: ref(''),
    routeReadiness: ref({ ready: true }), approvalUsers: ref([]), userStore: { getUser: { id: '71' } },
    resolvedUploadApprovers: () => calls.push('approvers'),
    resolvedUploadApplicationPeople: () => calls.push('people'), ...overrides
  }
  const exported = evaluate(declaration('freezeUploadApplicationDraft') + '\nexports.freeze = freezeUploadApplicationDraft', state)
  return { freeze: exported.freeze, calls }
}

test('G12 actual page freeze accepts empty/missing/whitespace explanation in save and submission paths', () => {
  for (const reason of [undefined, '', ' \t ']) {
    for (const saving of [false, true]) {
      const original = draft({ projectFolderChangeReason: reason })
      const { freeze, calls } = pageFreeze(original)
      const frozen = freeze(saving)
      assert.equal(frozen.projectFolderChangeReason, reason)
      assert.equal(frozen.projectFolderId, original.projectFolderId)
      assert.deepEqual(calls, saving ? [] : ['approvers', 'people'])
      frozen.fileName = 'changed.pdf'
      assert.equal(original.fileName, '操作规程.pdf')
    }
  }
})

test('G12 page freeze accepts 500 characters and rejects 501 before further submission work', () => {
  for (const saving of [false, true]) {
    assert.equal(pageFreeze(draft({ projectFolderChangeReason: '字'.repeat(500) })).freeze(saving).projectFolderChangeReason.length, 500)
    const { freeze, calls } = pageFreeze(draft({ projectFolderChangeReason: '字'.repeat(501) }))
    assert.throws(() => freeze(saving), /500/)
    assert.deepEqual(calls, [])
  }
})

test('G12 actual freeze rejects wrong-project, missing, stale and loading folder contexts even with empty explanation', () => {
  for (const overrides of [
    { selectedProjectFolder: ref({ id: '9007199254740993', projectCodeId: '6' }) },
    { selectedProjectFolder: ref(undefined) },
    { projectFoldersLoading: ref(true) },
    { projectFoldersError: ref('项目文件夹已失效') }
  ]) {
    const { freeze, calls } = pageFreeze(draft(), overrides)
    assert.throws(() => freeze(), /文件夹/)
    assert.deepEqual(calls, [])
  }
})

test('G12 page freeze still rejects mismatched project-attribute snapshot and unavailable accounts', () => {
  assert.throws(() => pageFreeze(draft(), { projectAttributesPanel: ref({ getSnapshot: () => ({
    applicationType: 'UPLOAD', projectId: '6', actual
  }) }) }).freeze(), /本次申请属性与所选项目不一致/)
  assert.throws(() => pageFreeze(draft(), { approvalUsersError: ref('正式账号读取失败') }).freeze(), /正式账号读取失败/)
})
