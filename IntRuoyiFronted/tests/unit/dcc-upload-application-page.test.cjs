const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const page = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
const body = (start, next) => {
  const from = page.indexOf(start)
  assert.notEqual(from, -1, 'missing actual page handler ' + start)
  const to = page.indexOf(next, from)
  assert.notEqual(to, -1, 'missing handler boundary ' + next)
  return page.slice(from, to)
}
const attributes = { targetMarkets: ['CE'], licenseHolder: 'N', actualManufacturer: 'Y', documentTransfer: 'N' }
const snapshot = id => ({ projectId: String(id), applicationType: 'UPLOAD', defaultSource: attributes, actual: structuredClone(attributes) })
const ref = value => ({ value })
const signoffModel = { exports: {}, Error }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/signoff-departments.ts', 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, signoffModel)
const submitterModel = { exports: {}, Error, require: () => ({ validateAttributes: value => value }) }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/submitter.ts', 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, submitterModel)
const setup = overrides => {
  const calls = { submitted: [], success: [], errors: [], confirmations: [], reset: 0, navigated: 0, confirmed: 0 }
  const approvalRoute = { ready: true, nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9223372036854775709'], approveMethod: 'ALL' }], blockers: [] }
  const approvalDirectory = [{ id: '9223372036854775709', name: '批准甲', deptName: '法规部' }]
  const state = {
    ...signoffModel.exports,
    buildUploadConfirmationSummary: submitterModel.exports.buildUploadConfirmationSummary,
    h: require('vue').h,
    formData: { dccProjectCodeId: 5, processType: 'CONTROLLED_FILE', changeType: 'NEW', categoryId: 2,
      directoryId: 3, relatedControlledFileIds: [8], projectAttributes: structuredClone(attributes), effectiveDate: '2099-12-20',
      fileNumber: 'SOP-001', versionNo: 'A/1', needTraining: true,
      projectFolderId: '500', projectFolderChangeReason: '归入正式项目目录', selectedSignoffDepartmentIds: ['7'] },
    signoffDepartments: ref([{ id: '7', name: '质量' }]), signoffDepartmentsLoading: ref(false), signoffDepartmentsError: ref(''),
    routeReadinessSelectionKey: ref(signoffModel.exports.signoffRequestKey(2, ['7'], 'CONTROLLED_FILE')),
    isExternalReview: ref(false), isNormalNewUpload: ref(true), projectAttributesPanel: ref({ getSnapshot: () => snapshot(5), selectProject: async () => true }),
    projectAttributesPanelKey: ref(0), acceptedProjectCodeId: ref(5), projectAttributesLoading: ref(false), projectAttributesSelectionSequence: 0,
    projectFoldersLoading: ref(false), projectFoldersError: ref(''), projectFolders: ref([]), projectFoldersProjectId: ref('5'),
    selectedProjectFolder: ref({ id: '500', name: '逻辑文件夹', projectCodeId: '5', active: true }), projectFolderRequestSequence: 0,
    selectedProjectCode: ref({ id: '5', projectName: '甲项目' }), selectedCategory: ref({ name: '质量文件' }),
    selectedFileTypeTaxonomyLeafName: ref('作业指导书'), selectedProjectTemplateType: ref({ name: '作业指导书' }),
    selectedUploadRelations: ref([{ fileName: '关联文件.pdf', versionNo: 'B/1', controlledFileId: '8', pendingEffect: true }]), uploadRelationsVisible: ref(false), uploadRelationSource: ref(undefined), loadUploadProjectFolders: async () => {},
    relatedFileRequestSequence: 0, relatedFileKeyword: ref(''), relatedFilePageNo: ref(1), relatedFileTotal: ref(1),
    relatedFileOptions: ref([8]), relatedFileOptionsError: ref(''), resetProjectFileTemplateSelection: () => calls.reset++,
    applyDccProjectCodeProductNumber() {}, loadRelatedFileOptions: async () => {}, loadProjectFileTemplate: async () => {},
    projectProductLoading: ref(false), projectProductError: ref(''), projectProductResolvedId: ref(5),
    projectProduct: ref({ projectCodeId: '5', source: 'DCC_CATALOG', productMasterId: null,
      productCatalogId: '51', productRelationId: '52', productCreateRequestId: '53', productCode: 'PRODUCT-5', productName: '正式产品' }), submitFieldErrors: {},
    clearSubmitFieldErrors() {}, formRef: ref({ validate: async () => true }), isVersionNoFormatValid: ref(true),
    versionFormatPreflightMessage: ref(''), resolveReadyAttachmentUploads: () => [], hasUnreadyAttachmentUploads: ref(false),
    currentVersionLookupTimer: undefined, loadCurrentVersionByFileNumber: async () => {}, currentVersionLookupError: ref(''),
    existingUploadIdentityBlockReason: ref(''), currentVersionProjectionBlockReason: ref(''), currentVersionInfo: ref({}),
    isRequestedVersionDuplicate: ref(false), uploadDirectoryTree: ref({}), previewUpload: ref({ sessionId: 'session-1', uploadTicket: 'ticket-1', fileName: 'SOP.PDF' }),
    drawingPdfUpload: ref(undefined), validateDccProjectProductCode: () => ({ valid: true }), isProductRequiredForSelectedCategory: ref(false),
    validateDrawingPdfUpload: () => ({ valid: true }), refreshRouteReadiness: async () => {}, routeReadiness: ref(approvalRoute), routeReadinessLoading: ref(false), routeReadinessError: ref(''),
    approvalUsers: ref(approvalDirectory), approvalUsersLoading: ref(false), approvalUsersError: ref(''),
    uploadApproverError: ref(''), uploadApprovers: ref(signoffModel.exports.resolvedUploadApprovers(approvalRoute, approvalDirectory)),
    submitLoading: ref(false), uploadSubmitted: ref(false), attachmentUploadBlockMessage: ref(''),
    uploadSubmitterService: { submit: async (...args) => calls.submitted.push(args) },
    router: { push: async () => calls.navigated++ },
    message: { confirm: async content => { calls.confirmed++; calls.confirmations.push(content) }, warning: text => calls.errors.push(text), error: text => calls.errors.push(text), success: text => calls.success.push(text) },
    buildSubmitFailureFeedback: error => ({ message: error.message }), applySubmitFailureFeedback() {}, ...overrides
  }
  state.ElMessageBox = { confirm: (...args) => state.message.confirm(...args) }
  return { state, calls }
}
const evaluate = (state, source, exported) => {
  const context = { ...state, exports: {}, Error, JSON, Promise }
  vm.runInNewContext(ts.transpileModule(source + '\nexports.handler = ' + exported,
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context
}
const submit = state => evaluate(state,
  body('const freezeUploadApplicationDraft =', 'const submitForm =') + body('const submitForm =', '\nwatch('), 'submitForm').exports.handler()
test('public upload mounts the formal editable attributes component and handles both edits and explicit restore', () => {
  assert.match(page, /<ProjectApplicationAttributes[\s\S]*?ref="projectAttributesPanel"[\s\S]*?action="UPLOAD"/)
  assert.match(page, /@change="captureProjectAttributes"/)
  assert.match(page, /@restore-defaults="captureProjectAttributes"/)
})
test('cancelled project switch retains the original project, actual properties and related selection', async () => {
  const { state, calls } = setup({ projectAttributesPanel: ref({ selectProject: async () => false }) })
  state.formData.dccProjectCodeId = 6
  await evaluate(state, body('const handleProjectCodeChange =', 'const projectFolderTree ='), 'handleProjectCodeChange').exports.handler()
  assert.equal(state.formData.dccProjectCodeId, 5); assert.deepEqual(state.formData.relatedControlledFileIds, [8]); assert.equal(calls.reset, 0)
})
test('confirmation cancel preserves edited fields and does not submit or navigate', async () => {
  const { state, calls } = setup(); state.message.confirm = async () => { calls.confirmed++; throw 'cancel' }
  await submit(state)
  assert.equal(calls.confirmed, 1); assert.equal(calls.submitted.length, 0); assert.equal(calls.navigated, 0)
  assert.equal(state.formData.projectAttributes.targetMarkets[0], 'CE'); assert.equal(state.submitLoading.value, false)
})
test('confirmed upload sends a detached actual snapshot from the same selected project', async () => {
  const { state, calls } = setup(); await submit(state)
  assert.equal(calls.confirmed, 1); assert.equal(calls.submitted.length, 1)
  state.formData.projectAttributes.targetMarkets.push('FDA')
  assert.deepEqual(JSON.parse(JSON.stringify(calls.submitted[0][0].projectAttributes)), attributes)
  assert.equal(calls.navigated, 1)
})
test('formally unbound product cannot submit a product-required category', async () => {
  const { state, calls } = setup({
    projectProduct: ref({ projectCodeId: '5', source: 'UNBOUND', productMasterId: null,
      productCatalogId: null, productRelationId: null, productCreateRequestId: null, productCode: null, productName: null }),
    isProductRequiredForSelectedCategory: ref(true),
    validateDccProjectProductCode: submitterModel.exports.validateDccProjectProductCode
  })
  state.formData.productCode = ''
  await submit(state)
  assert.equal(calls.confirmed, 0)
  assert.equal(calls.submitted.length, 0)
  assert.ok(calls.errors.length > 0)
})
test('formal product provenance changes during confirmation reject the exact pending upload', async () => {
  const { state, calls } = setup()
  state.message.confirm = async () => {
    calls.confirmed++
    state.projectProduct.value.productRelationId = '54'
  }
  await submit(state)
  assert.equal(calls.confirmed, 1)
  assert.equal(calls.submitted.length, 0)
  assert.match(calls.errors.join(' '), /变化/)
})
test('normal NEW confirms and submits with only its real project folder and no NAS tree', async () => {
  const { state, calls } = setup({ uploadDirectoryTree: ref(undefined) })
  state.formData.directoryId = null
  await submit(state)
  assert.equal(calls.confirmed, 1)
  assert.equal(calls.submitted.length, 1)
  assert.equal(calls.submitted[0][0].projectFolderId, '500')
  assert.equal(calls.submitted[0][0].directoryId, null)
  // The real submitter payload independently omits this draft-only field for NEW.
})
test('external review still blocks when its required real NAS directory is absent', async () => {
  const { state, calls } = setup({ isExternalReview: ref(true), isNormalNewUpload: ref(false), uploadDirectoryTree: ref(undefined) })
  state.formData.processType = 'EXTERNAL_REVIEW'
  state.formData.directoryId = null
  await submit(state)
  assert.equal(calls.confirmed, 0)
  assert.equal(calls.submitted.length, 0)
  assert.match(calls.errors.join(' '), /目录/)
})
test('project or file change during confirmation rejects the obsolete request visibly', async () => {
  const { state, calls } = setup(); state.message.confirm = async () => { state.previewUpload.value.uploadTicket = 'another-file' }
  await submit(state)
  assert.equal(calls.submitted.length, 0); assert.equal(calls.navigated, 0); assert.match(calls.errors.join(' '), /变化/)
})
test('attributes for another project reject before confirmation and network submission', async () => {
  const { state, calls } = setup({ projectAttributesPanel: ref({ getSnapshot: () => snapshot(6) }) })
  await submit(state)
  assert.equal(calls.confirmed, 0); assert.equal(calls.submitted.length, 0); assert.match(calls.errors.join(' '), /项目/)
})
test('two clicks awaiting preflight create only one confirmation and one approval request', async () => {
  const { state, calls } = setup(); let finish
  state.message.confirm = async () => { calls.confirmed++; await new Promise(resolve => { finish = resolve }) }
  const pending = submit(state)
  const second = submit(state)
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(calls.confirmed, 1); finish(); await Promise.all([pending, second])
  assert.equal(calls.submitted.length, 1)
})
test('late cancelled project selection does not restore an older project over a newer choice', async () => {
  const { state } = setup(); let cancel
  state.projectAttributesPanel.value.selectProject = async id => id === 6 ? new Promise(resolve => { cancel = resolve }) : true
  const context = evaluate(state, body('const handleProjectCodeChange =', 'const projectFolderTree ='), 'handleProjectCodeChange')
  state.formData.dccProjectCodeId = 6; const previous = context.exports.handler()
  state.formData.dccProjectCodeId = 7; await context.exports.handler()
  cancel(false); await previous
  assert.equal(state.formData.dccProjectCodeId, 7); assert.equal(state.acceptedProjectCodeId.value, 7)
})
test('attribute validation and backend failures stay visible without success or navigation', async () => {
  const { state, calls } = setup(); state.projectAttributesPanel.value.getSnapshot = () => { throw new Error('请选择文件转移是或否') }
  await submit(state); assert.equal(calls.submitted.length, 0); assert.match(calls.errors.join(' '), /文件转移/)
  state.projectAttributesPanel.value.getSnapshot = () => snapshot(5)
  state.uploadSubmitterService.submit = async () => { throw new Error('当前文件名已被占用') }
  await submit(state); assert.match(calls.errors.join(' '), /已被占用/); assert.equal(calls.success.length, 0); assert.equal(calls.navigated, 0)
})

test('actual clicked confirmation renders required formal facts and independent date notice', async () => {
  const { state, calls } = setup(); await submit(state)
  assert.equal(calls.confirmed, 1)
  const read = node => typeof node === 'string' ? node : Array.isArray(node?.children) ? node.children.map(read).join(' ') : String(node?.children || '')
  const text = read(calls.confirmations[0])
  for (const expected of ['甲项目', '逻辑文件夹', '作业指导书', 'SOP.PDF', 'SOP-001', 'A/1', '2099-12-20', 'CE', '注册人：否', '生产方：是', '文件转移：否', '关联文件.pdf', 'B/1', '质量', '批准甲', '9223372036854775709', '需要培训', '受控日期由系统记录，生效日期可晚于受控日期']) assert.ok(text.includes(expected), expected)
})

test('confirmation rejects actual attribute panel changes even if formData is not updated', async () => {
  const { state, calls } = setup()
  state.message.confirm = async () => {
    calls.confirmed++
    state.projectAttributesPanel.value.getSnapshot = () => ({ ...snapshot(5), actual: { ...attributes, targetMarkets: ['FDA'] } })
  }
  await submit(state)
  assert.equal(calls.submitted.length, 0); assert.match(calls.errors.join(' '), /变化/)
})

test('confirmation rejects changed matrix approvers and missing official enabled accounts', async () => {
  const { state, calls } = setup()
  state.message.confirm = async () => { state.routeReadiness.value.nodes[0].resolvedUserIds = ['7'] }
  await submit(state)
  assert.equal(calls.submitted.length, 0); assert.match(calls.errors.join(' '), /变化/)
  const missing = setup({ approvalUsers: ref([]) })
  await submit(missing.state)
  assert.equal(missing.calls.confirmed, 0); assert.equal(missing.calls.submitted.length, 0); assert.match(missing.calls.errors.join(' '), /启用账号/)
})

test('context changes while preflight is pending reject the clicked request before confirmation', async () => {
  const { state, calls } = setup()
  state.loadCurrentVersionByFileNumber = async () => { state.formData.needTraining = false }
  await submit(state)
  assert.equal(calls.confirmed, 0); assert.equal(calls.submitted.length, 0); assert.match(calls.errors.join(' '), /变化/)
})

const approvalDirectoryLoader = state => evaluate(state,
  body('const loadApprovalUsers =', 'const loadSignoffDepartments ='), 'loadApprovalUsers').exports.handler
test('actual enabled-directory handler preserves full Long accounts and exposes a failed read', async () => {
  const { state } = setup()
  Object.assign(state, { approvalUsersRequestSeq: 0, getSimpleUserList: async () => [{ id: '9223372036854775709', nickname: '批准甲', deptName: '法规部' }] })
  await approvalDirectoryLoader(state)()
  assert.equal(state.approvalUsers.value[0].id, '9223372036854775709')
  state.getSimpleUserList = async () => { throw new Error('真实目录权限错误') }
  await approvalDirectoryLoader(state)()
  assert.equal(state.approvalUsers.value.length, 0); assert.equal(state.approvalUsersError.value, '真实目录权限错误')
  assert.equal(state.approvalUsersLoading.value, false)
})

test('a late enabled-directory read cannot replace newer account facts', async () => {
  const { state } = setup(); let finish, request = 0
  Object.assign(state, { approvalUsersRequestSeq: 0, getSimpleUserList: () => ++request === 1
    ? new Promise(resolve => { finish = resolve }) : Promise.resolve([{ id: '9', nickname: '新批准人' }]) })
  const loader = approvalDirectoryLoader(state)
  const old = loader(); await loader()
  finish([{ id: '8', nickname: '旧批准人' }]); await old
  assert.equal(state.approvalUsers.value[0].id, '9'); assert.equal(state.approvalUsersLoading.value, false)
})

test('cancel and close both leave every upload fact and selected preview unchanged', async () => {
  const { state, calls } = setup()
  const before = JSON.stringify({ form: state.formData, preview: state.previewUpload.value, related: state.selectedUploadRelations.value })
  state.message.confirm = async () => { throw 'close' }
  await submit(state)
  assert.equal(calls.submitted.length, 0); assert.equal(calls.errors.length, 0)
  assert.equal(JSON.stringify({ form: state.formData, preview: state.previewUpload.value, related: state.selectedUploadRelations.value }), before)
})

test('public approval preflight does not describe a missing account-directory projection as ready', () => {
  const { state } = setup({ approvalUsersError: ref('真实账号目录读取失败') })
  const context = evaluate({ ...state, computed: calculate => ({ get value() { return calculate() } }) },
    body('const approvalChainPreflightText =', 'const uploadPreflightChecks ='), 'approvalChainPreflightText')
  assert.equal(context.exports.handler.value, '真实账号目录读取失败')
})
