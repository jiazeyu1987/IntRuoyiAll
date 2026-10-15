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
const originalMatrixHelpers = require('./dcc-upload-test-loader.cjs')
const matrixHelpers = { ...originalMatrixHelpers,
  matrixReadiness: (ready, scope = {}, before = 1, after = 1) => originalMatrixHelpers.matrixReadiness(ready, { ...scope, approvalIds: ['9223372036854775709'] }, before, after),
  matrixRejection: (scope = {}, reason, before, after) => originalMatrixHelpers.matrixRejection({ ...scope, approvalIds: ['9223372036854775709'] }, reason, before, after) }
const matrixModule = matrixHelpers.loadUploadMatrix()
const submitterModel = { exports: {}, Error, require: id => id.endsWith('approval-matrix') ? matrixModule : id.endsWith('signoff-departments') ? signoffModel.exports : ({ validateAttributes: value => value }) }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/submitter.ts', 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, submitterModel)
const setup = overrides => {
  const calls = { submitted: [], success: [], errors: [], confirmations: [], reset: 0, navigated: 0, confirmed: 0 }
  const approvalRoute = { ready: true, supervisorSourceDigest:'c'.repeat(64),supervisorSourceToken:'fixture-server-supervisor-source',nodes: [{stageCode:'SUPERVISOR_REVIEW',resolvedUserIds:['72']}, { stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9223372036854775709'], approveMethod: 'ALL' }], blockers: [] }
  const approvalDirectory = [{id:'71',name:'真实编制甲',deptId:'7',deptName:'质量'}, {id:'72',name:'正式主管乙',deptId:'10',deptName:'研发'}, { id: '9223372036854775709', name: '批准甲', deptName: '法规部' }]
  const matrixScope = { tenantId: '1', projectId: '5', categoryId: '2', taxonomyId: '4', actionType: 'NEW', applicationContextKey: 'scope' }
  const matrix = new matrixModule.UploadApprovalMatrixState(matrixScope)
  matrix.accept(matrix.begin(), matrixHelpers.matrixReadiness(approvalRoute))
  const state = {
    userStore:{getUser:{id:'71',deptId:'7',nickname:'缓存名字不得作为确认事实'}},
    ...matrixModule, approvalMatrixState: ref(matrix), approvalMatrixScopeKey: JSON.stringify(['1', '5', '2', '4', 'NEW','PUBLIC_MATRIX',signoffModel.exports.uploadApplicationActorKey({id:'71',deptId:'7'},'1')]),
    getTenantId: () => '1', getVisitTenantId: () => undefined, referenceIdentity: String, uploadFileTypeIdentity: String, createControlledFileUploadSessionId: () => 'scope',
    ...signoffModel.exports,
    buildUploadConfirmationSummary: submitterModel.exports.buildUploadConfirmationSummary,
    validateUploadEffectiveDate: submitterModel.exports.validateUploadEffectiveDate,
    buildUploadEffectiveChoice: submitterModel.exports.buildUploadEffectiveChoice,
    validateUploadTickets: submitterModel.exports.validateUploadTickets,
    resolveTodayDate: () => '2026-10-11',
    hasUploadApplicationInput: () => true, hasTemporaryUploadState: () => false,
    cleanupCurrentUploadSession: async () => true, projectAttributesRestoreSnapshot: ref(undefined),
    h: require('vue').h,
    formData: { dccProjectCodeId: 5, fileTypeTaxonomyId: '4', processType: 'CONTROLLED_FILE', changeType: 'NEW', categoryId: 2,
      directoryId: 3, relatedControlledFileIds: [8], projectAttributes: structuredClone(attributes), effectiveMode: 'PRESET_DATE', effectiveDate: '2099-12-20',
      fileNumber: 'SOP-001', versionNo: 'A/1', needTraining: true,
      projectFolderId: '500', projectFolderChangeReason: '归入正式项目目录', selectedSignoffDepartmentIds: ['7'], selectedDistributionDepartmentIds: ['7'] },
    signoffDepartments: ref([{ id: '7', name: '质量' }]), signoffDepartmentsLoading: ref(false), signoffDepartmentsError: ref(''),
    routeReadinessSelectionKey: ref(signoffModel.exports.signoffRequestKey(2, ['7'], 'CONTROLLED_FILE')),
    routeReadinessRequestSeq: 0, signoffSelectionCategoryKey: signoffModel.exports.signoffCategoryKey(2,'CONTROLLED_FILE'),
    selectedFileTypeTaxonomyPath: ref({ approvalSourceMode: 'PUBLIC_MATRIX' }), isCustomApprovalSource: ref(false),
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
    validateDrawingPdfUpload: () => ({ valid: true }), refreshRouteReadiness: async () => true, routeReadiness: ref(approvalRoute), routeReadinessLoading: ref(false), routeReadinessError: ref(''),
    approvalUsers: ref(approvalDirectory), approvalUsersLoading: ref(false), approvalUsersError: ref(''),
    uploadApproverError: ref(''), uploadApprovers: ref(signoffModel.exports.resolvedUploadApprovers(approvalRoute, approvalDirectory)),
    submitLoading: ref(false), uploadSubmitted: ref(false), uploadPreviewLoading: ref(false), uploadDrawingPdfLoading: ref(false), attachmentUploadBlockMessage: ref(''),
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
  body('const ensureUploadApprovalMatrix =', 'const refreshRouteReadiness =') + body('const refreshRejectedUploadMatrix =', 'const handleFileExceed:') + body('const freezeUploadApplicationDraft =', 'const submitForm =') + body('const submitForm =', '\nwatch('), 'submitForm').exports.handler()
const saveDraft = state => evaluate(state, body('const ensureUploadApprovalMatrix =', 'const refreshRouteReadiness =') + body('const refreshRejectedUploadMatrix =', 'const handleFileExceed:') + body('const freezeUploadApplicationDraft =', 'const submitForm ='), 'saveUploadDraft').exports.handler()
const setupCustom = (ready=true) => {
  const fixture=require('./dcc-upload-a32-fixture.cjs'),{state,calls}=setup(),config=structuredClone(fixture.config)
  const matrix=new matrixModule.UploadApprovalMatrixState(fixture.scope);matrix.setApplicationCustomMatrix(config)
  const route=fixture.response(fixture.scope,config,['7'],ready);matrix.accept(matrix.begin(['7']),route)
  state.approvalMatrixState.value=matrix;state.approvalMatrixScopeKey=JSON.stringify(['1','5','2','4','NEW','APPLICATION_CUSTOM',state.uploadApplicationActorKey(state.userStore.getUser,'1')])
  state.isCustomApprovalSource.value=true;state.selectedFileTypeTaxonomyPath.value={approvalSourceMode:'APPLICATION_CUSTOM'}
  Object.assign(state.formData,{applicationCustomMatrix:config,effectiveMode:'DELAYED',effectiveDate:null})
  state.routeReadiness.value=route;state.approvalUsers.value.push({id:'66',name:'文控丁',deptId:'11',deptName:'文控部'})
  state.resolveUploadErrorMessage=submitterModel.exports.resolveUploadErrorMessage
  state.buildSubmittedApplicationRoute=id=>({path:'/detail/'+id});state.router.resolve=()=>({path:'/browser'})
  return {state,calls,matrix,fixture}
}

test('A32 actual CUSTOM draft save bypasses public after, keeps partial configuration and creates no approval',async()=>{
  const {state,calls,matrix,fixture}=setupCustom(false),saved=[]
  state.formData.applicationCustomMatrix={approverUserId:null,documentControlUserId:'66'}
  matrix.setApplicationCustomMatrix(state.formData.applicationCustomMatrix)
  matrix.accept(matrix.begin(['7']),fixture.response(fixture.scope,state.formData.applicationCustomMatrix,['7'],false))
  state.uploadSubmitterService.saveDraft=async(...args)=>{saved.push(submitterModel.exports.buildWorkingDraftPayload(...args));return '9223372036854775701'}
  await saveDraft(state)
  assert.equal(saved.length,1,calls.errors.join(' '));assert.equal(calls.submitted.length,0)
  assert.equal(saved[0].applicationCustomMatrix.approverUserId,null);assert.equal(saved[0].applicationCustomMatrixPhase,'PREVIEW');assert.equal(saved[0].expectedApprovalRouteId,undefined)
})
test('A32 actual CUSTOM submit confirmation includes document control and cancellation preserves all input with zero writes',async()=>{
  const {state,calls}=setupCustom();let summary=''
  const text=node=>typeof node==='string'?node:Array.isArray(node?.children)?node.children.map(text).join(' '):String(node?.children||'')
  state.message.confirm=async(content)=>{calls.confirmed++;summary=text(content);throw 'cancel'}
  const before=JSON.stringify(state.formData);await submit(state)
  assert.equal(calls.confirmed,1,calls.errors.join(' '));assert.ok(summary.includes('文控丁'),summary);assert.ok(summary.includes('本次自定义'),summary)
  assert.equal(calls.submitted.length,0);assert.deepEqual(state.formData.applicationCustomMatrix,JSON.parse(before).applicationCustomMatrix)
  assert.equal(state.formData.fileNumber,'SOP-001');assert.equal(state.submitLoading.value,false)
})
test('A32 actual CUSTOM submit requires every attachment ready and rejects custom account change during confirmation',async()=>{
  const first=setupCustom();first.state.hasUnreadyAttachmentUploads.value=true;await submit(first.state)
  assert.equal(first.calls.confirmed,0);assert.equal(first.calls.submitted.length,0)
  const second=setupCustom();second.state.message.confirm=async()=>{second.calls.confirmed++;second.state.formData.applicationCustomMatrix.documentControlUserId='67'}
  await submit(second.state);assert.equal(second.calls.confirmed,1,second.calls.errors.join(' '));assert.equal(second.calls.submitted.length,0);assert.match(second.calls.errors.join(' '),/变化/)
})
test('A32 accepted project switch clears CUSTOM actors and proof while cancellation preserves configuration',async()=>{
  const {state}=setupCustom();const old=JSON.stringify(state.formData.applicationCustomMatrix)
  state.formData.dccProjectCodeId=6;state.message.confirm=async()=>{throw 'cancel'}
  await evaluate(state,body('const handleProjectCodeChange =','const projectFolderTree ='),'handleProjectCodeChange').exports.handler()
  assert.equal(JSON.stringify(state.formData.applicationCustomMatrix),old);assert.equal(state.formData.dccProjectCodeId,5)
  state.formData.dccProjectCodeId=6;state.message.confirm=async()=>{}
  await evaluate(state,body('const handleProjectCodeChange =','const projectFolderTree ='),'handleProjectCodeChange').exports.handler()
  assert.equal(state.formData.applicationCustomMatrix,null);assert.equal(state.formData.approvalMatrixSelection,undefined)
  assert.deepEqual(Array.from(state.formData.relatedControlledFileIds),[])
})
test('A32 confirmed CUSTOM submit sends both accounts and every attachment without public baseline or task precreation',async()=>{
 const {state,calls}=setupCustom(),attachments=[{sessionId:'one',uploadTicket:'ticket-one',fileName:'附件一.pdf'},{sessionId:'two',uploadTicket:'ticket-two',fileName:'附件二.docx'}]
 state.resolveReadyAttachmentUploads=()=>attachments
 state.uploadSubmitterService.submit=async(...args)=>calls.submitted.push(submitterModel.exports.buildSubmitPayload(...args))
 await submit(state);assert.equal(calls.submitted.length,1,calls.errors.join(' '));const payload=calls.submitted[0]
 assert.equal(payload.approvalSourceMode,'APPLICATION_CUSTOM');assert.equal(payload.applicationCustomMatrix.documentControlUserId,'66');assert.equal(payload.expectedApprovalRouteId,undefined)
 assert.equal(payload.attachmentUploadTickets.length,2);assert.equal(payload.effectiveMode,'DELAYED');assert.equal(payload.effectiveDate,null)
 const text=node=>typeof node==='string'?node:Array.isArray(node?.children)?node.children.map(text).join(' '):String(node?.children||'')
 assert.match(text(calls.confirmations[0]),/附件一.pdf/);assert.match(text(calls.confirmations[0]),/附件二.docx/)
})
test('A32 draft SAVE rejects changed custom input while a real preflight is pending',async()=>{
 const {state,calls}=setupCustom(false),saved=[]
 state.refreshRouteReadiness=async()=>{state.formData.applicationCustomMatrix.documentControlUserId='67';return false}
 state.uploadSubmitterService.saveDraft=async(...args)=>{saved.push(args);return '99'}
 await saveDraft(state);assert.equal(saved.length,0);assert.match(calls.errors.join(' '),/变化|不一致|预检/)
})
test('A32 only custom account edits make a type dirty, cancellation keeps the original mode and no reset',async()=>{
 const {state,calls}=setupCustom();let cleaned=0
 Object.assign(state,{fileTypeChangeRequestSequence:0,acceptedFileTypeTaxonomyId:ref('4'),fileTypeTaxonomyPathMap:ref(new Map([['6',{approvalSourceMode:'PUBLIC_MATRIX'}]])),
  fileTypeCategoryRequestSequence:0,cleanupCurrentUploadSession:async()=>{cleaned++;return true},resetUploadNameContext:()=>{},
  resetCategorySelectionForFileTypeTaxonomyChange:()=>{throw new Error('cancel must not reset')},syncAutoCategoryFromSelectedFileTypeTaxonomy:async()=>{}})
 state.formData.fileTypeTaxonomyId='6';state.message.confirm=async()=>{calls.confirmed++;throw 'cancel'}
 await evaluate(state,body('const handleFileTypeTaxonomyChange =','const loadUploadDirectoryTree ='),'handleFileTypeTaxonomyChange').exports.handler()
 assert.equal(calls.confirmed,1);assert.equal(cleaned,0);assert.equal(state.formData.fileTypeTaxonomyId,'4');assert.equal(state.formData.applicationCustomMatrix.documentControlUserId,'66')
})
test('A32 confirmed type and source switch clears custom and old relation choices before loading new formal category',async()=>{
 const {state,calls}=setupCustom()
 Object.assign(state,{fileTypeChangeRequestSequence:0,acceptedFileTypeTaxonomyId:ref('4'),fileTypeTaxonomyPathMap:ref(new Map([['6',{approvalSourceMode:'PUBLIC_MATRIX'}]])),
  fileTypeCategoryRequestSequence:0,cleanupCurrentUploadSession:async()=>true,resetUploadNameContext:()=>{},resetUploadDirectoryContext:()=>{},
  resetSelectedPreview:()=>{},resetDrawingPdfUpload:()=>{},resetAttachmentUploads:()=>{},syncAutoCategoryFromSelectedFileTypeTaxonomy:async()=>{}})
 state.formData.fileTypeTaxonomyId='6'
 await evaluate(state,body('const resetCategorySelectionForFileTypeTaxonomyChange =','const syncAutoCategoryFromSelectedFileTypeTaxonomy =')
   +body('const handleFileTypeTaxonomyChange =','const loadUploadDirectoryTree ='),'handleFileTypeTaxonomyChange').exports.handler()
 assert.equal(calls.confirmed,1);assert.equal(state.formData.applicationCustomMatrix,null);assert.equal(state.formData.approvalMatrixSelection,undefined)
 assert.deepEqual(Array.from(state.formData.relatedControlledFileIds),[]);assert.deepEqual(Array.from(state.selectedUploadRelations.value),[])
})
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

test('IC05 signature mode and empty distribution confirm and submit the exact explicit choice', async () => {
  const { state, calls } = setup()
  state.formData.effectiveMode = 'AFTER_STANDARDIZATION_SIGNATURE'; state.formData.effectiveDate = null
  state.formData.selectedDistributionDepartmentIds = []
  await submit(state)
  assert.equal(calls.submitted.length, 1)
  const frozen = calls.submitted[0][0]
  assert.equal(frozen.effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE'); assert.equal(frozen.effectiveDate, null)
  assert.deepEqual(Array.from(frozen.selectedDistributionDepartmentIds), [])
})

test('IC05 missing mode and unavailable distribution reject before any confirmation or write', async () => {
  for (const value of ['MODE', 'MISSING_DEPARTMENTS', 'DISABLED_DEPARTMENT']) {
    const { state, calls } = setup()
    if (value === 'MODE') state.formData.effectiveMode = null
    else state.formData.selectedDistributionDepartmentIds = value === 'MISSING_DEPARTMENTS' ? undefined : ['99']
    await submit(state)
    assert.equal(calls.confirmed, 0); assert.equal(calls.submitted.length, 0); assert.ok(calls.errors.length)
  }
})

test('IC05 changing mode or independent distribution during confirmation rejects the obsolete submission', async () => {
  for (const field of ['MODE', 'DEPARTMENTS']) {
    const { state, calls } = setup()
    state.message.confirm = async () => {
      if (field === 'MODE') { state.formData.effectiveMode = 'AFTER_STANDARDIZATION_SIGNATURE'; state.formData.effectiveDate = null }
      else state.formData.selectedDistributionDepartmentIds = []
    }
    await submit(state)
    assert.equal(calls.submitted.length, 0); assert.match(calls.errors.join(' '), /变化/)
  }
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
  const { state } = setup({ hasUploadApplicationInput: () => false }); let cancel
  state.projectAttributesPanel.value.selectProject = async id => id === 6 ? new Promise(resolve => { cancel = resolve }) : true
  const context = evaluate(state, body('const handleProjectCodeChange =', 'const projectFolderTree ='), 'handleProjectCodeChange')
  state.formData.dccProjectCodeId = 6; const previous = context.exports.handler()
  state.formData.dccProjectCodeId = 7; await context.exports.handler()
  cancel(false); await previous
  assert.equal(state.formData.dccProjectCodeId, 7); assert.equal(state.acceptedProjectCodeId.value, 7)
})

test('manual project context changes require confirmation even when actual attributes are not dirty', async () => {
  let selected = 0
  const { state, calls } = setup({ projectAttributesPanel: ref({ selectProject: async () => { selected++; return true } }) })
  state.formData.dccProjectCodeId = 6
  state.message.confirm = async () => { throw 'cancel' }
  await evaluate(state, body('const handleProjectCodeChange =', 'const projectFolderTree ='), 'handleProjectCodeChange').exports.handler()
  assert.equal(selected, 0)
  assert.equal(state.formData.dccProjectCodeId, 5)
  assert.equal(state.formData.projectFolderId, '500')
  assert.equal(calls.reset, 0)
})

test('successful submission remains locked and visible when navigation fails', async () => {
  const { state, calls } = setup()
  state.router.push = async () => { throw new Error('路由不可用') }
  state.resolveUploadErrorMessage = submitterModel.exports.resolveUploadErrorMessage
  await submit(state)
  assert.equal(calls.submitted.length, 1)
  assert.equal(state.uploadSubmitted.value, true)
  assert.match(calls.errors.join(' '), /已提交成功.*路由不可用/)
  await submit(state)
  assert.equal(calls.submitted.length, 1)
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
  for (const expected of ['甲项目', '逻辑文件夹', '作业指导书', 'SOP.PDF', 'SOP-001', 'A/1', '2099-12-20', 'CE', '注册人：否', '生产方：是', '文件转移：否', '关联文件.pdf', 'B/1', '质量', '批准甲', '9223372036854775709', '需要培训', '生效与受控分别记录', '全部人员']) assert.ok(text.includes(expected), expected)
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
  const businessSnapshot = () => JSON.stringify({ form: Object.fromEntries(Object.entries(state.formData).filter(([key]) => key !== 'approvalMatrixSelection')), preview: state.previewUpload.value, related: state.selectedUploadRelations.value })
  const before = businessSnapshot()
  state.message.confirm = async () => { throw 'close' }
  await submit(state)
  assert.equal(calls.submitted.length, 0); assert.equal(calls.errors.length, 0)
  assert.equal(businessSnapshot(), before)
  assert.equal(state.formData.approvalMatrixSelection.matrixChangeConfirmed, false)
})

test('public approval preflight does not describe a missing account-directory projection as ready', () => {
  const { state } = setup({ approvalUsersError: ref('真实账号目录读取失败') })
  const context = evaluate({ ...state, computed: calculate => ({ get value() { return calculate() } }) },
    body('const approvalChainPreflightText =', 'const uploadPreflightChecks ='), 'approvalChainPreflightText')
  assert.equal(context.exports.handler.value, '真实账号目录读取失败')
})

test('explicit durable draft save uses only working creation and navigates to the exact formal resource', async () => {
  const { state, calls } = setup()
  const saved = [], routes = []
  state.formData.effectiveMode = null; state.formData.effectiveDate = null
  state.formData.fileTypeTaxonomyId = '4'
  state.referenceIdentity = value => String(value)
  state.resolveUploadErrorMessage = submitterModel.exports.resolveUploadErrorMessage
  state.buildSubmittedApplicationRoute = (id, query) => ({ path: '/dcc/controlled-file/detail/' + id, query })
  state.router.resolve = () => ({ path: '/dcc/controlled-file/browser' })
  state.router.push = async route => routes.push(route)
  state.uploadSubmitterService.saveDraft = async (...args) => { saved.push(args); return 11 }
  await saveDraft(state)
  assert.equal(saved.length, 1, calls.errors.join(' ')); assert.equal(calls.submitted.length, 0); assert.equal(calls.confirmed, 0)
  assert.equal(saved[0][0].effectiveMode, null); assert.equal(saved[0][0].effectiveDate, null)
  assert.deepEqual(Array.from(saved[0][0].selectedDistributionDepartmentIds), ['7'])
  assert.equal(routes[0].path, '/dcc/controlled-file/detail/11'); assert.equal(state.uploadSubmitted.value, true)
  await saveDraft(state); assert.equal(saved.length, 1)
})

test('IC13 cancelling real before/after matrix review sends zero submission and preserves business input', async () => {
  const { state, calls } = setup()
  state.approvalMatrixState.value.accept(state.approvalMatrixState.value.begin(), matrixHelpers.matrixReadiness({ ready: true, blockers: [], nodes: [] }, {}, 1, 2))
  state.message.confirm = async () => { calls.confirmed++; throw 'cancel' }
  const before = JSON.stringify({ actual: state.formData.projectAttributes, departments: state.formData.selectedDistributionDepartmentIds, preview: state.previewUpload.value })
  await submit(state)
  assert.equal(calls.confirmed, 1); assert.equal(calls.submitted.length, 0)
  assert.equal(JSON.stringify({ actual: state.formData.projectAttributes, departments: state.formData.selectedDistributionDepartmentIds, preview: state.previewUpload.value }), before)
  assert.equal(state.formData.approvalMatrixSelection, undefined)
})

test('IC13 upload confirmation submits the exact server after token and the same business choice', async () => {
  const { state, calls } = setup()
  state.approvalMatrixState.value.accept(state.approvalMatrixState.value.begin(), matrixHelpers.matrixReadiness({ ready: true, blockers: [], nodes: [] }, {}, 1, 2))
  await submit(state)
  assert.equal(calls.confirmed, 2); assert.equal(calls.submitted.length, 1)
  const selected = calls.submitted[0][0].approvalMatrixSelection
  assert.equal(selected.expectedApprovalRouteVersionNo, 2); assert.equal(selected.approvalMatrixConfirmationToken, 'fixture-confirm-2'); assert.equal(selected.matrixChangeConfirmed, true)
  assert.deepEqual(Array.from(calls.submitted[0][0].selectedDistributionDepartmentIds), ['7'])
  assert.equal(selected.executionContract, 'PUBLIC_MATRIX_NATIVE_V1')
  assert.equal(selected.expectedProcessDefinitionId, 'dcc-controlled-file-upload:5:fixture-exact')
  assert.equal(selected.expectedDefinitionDigest, 'a'.repeat(64))
})

test('IC16 new upload without actual prepared definition keeps the backend blocker and never submits or saves', async () => {
  for (const action of ['submit', 'save']) {
    const { state, calls } = setup()
    let saves = 0
    state.uploadSubmitterService.saveDraft = async () => { saves++; return 11 }
    state.resolveUploadErrorMessage = submitterModel.exports.resolveUploadErrorMessage
    const response = matrixHelpers.matrixReadiness({ ready: false, nodes: [], blockers: [{ reasonCode: 'EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY', message: 'EXECUTION_CONTRACT_NEW_DEFINITION_NOT_DEPLOYED' }] })
    response.preparedProcessDefinitionId = null; response.preparedDefinitionDigest = null
    state.approvalMatrixState.value.accept(state.approvalMatrixState.value.begin(), response)
    if (action === 'submit') await submit(state)
    else await saveDraft(state)
    assert.equal(calls.submitted.length, 0); assert.equal(saves, 0); assert.equal(calls.success.length, 0)
    assert.match(calls.errors.join(' '), /EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY.*NOT_DEPLOYED/)
  }
})

test('IC13 a backend stale rejection refreshes latest comparison without reporting upload success', async () => {
  const { state, calls } = setup(); let refreshes = 0
  state.uploadSubmitterService.submit = async () => { throw matrixHelpers.matrixRejection() }
  state.refreshRouteReadiness = async () => {
    refreshes++
    if (refreshes > 1) state.approvalMatrixState.value.accept(state.approvalMatrixState.value.begin(), matrixHelpers.matrixReadiness({ ready: true, blockers: [], nodes: [] }, {}, 1, 3))
    return true
  }
  state.resolveUploadErrorMessage = submitterModel.exports.resolveUploadErrorMessage
  await submit(state)
  assert.equal(refreshes, 2); assert.equal(calls.success.length, 0); assert.equal(state.uploadSubmitted.value, false)
  assert.equal(state.formData.approvalMatrixSelection, undefined); assert.throws(() => state.approvalMatrixState.value.selection(), /确认/)
  assert.match(calls.errors.join(' '), /再次变化/)
})

test('IC13 formal confirmation-required rejection preserves its exact reason and fresh readiness runtime blocker', async () => {
  const { state, calls } = setup(); let reads = 0
  state.uploadSubmitterService.submit = async () => { throw matrixHelpers.matrixRejection({}, 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED') }
  state.refreshRouteReadiness = async () => {
    reads++
    if (reads > 1) {
      state.approvalMatrixState.value.accept(state.approvalMatrixState.value.begin(), matrixHelpers.matrixReadiness({ ready: false, blockers: [{ reasonCode: 'SIGNATURE_MISSING', stageName: '批准', userName: '张甲', message: '该账号签名图片未配置' }], nodes: [] }, {}, 1, 3))
      state.routeReadiness.value = { ready: false, blockers: [{ reasonCode: 'SIGNATURE_MISSING', stageName: '批准', userName: '张甲', message: '该账号签名图片未配置' }], nodes: [] }
      return false
    }
    return true
  }
  await submit(state)
  assert.equal(reads, 2); assert.equal(calls.success.length, 0); assert.equal(state.uploadSubmitted.value, false)
  const text = calls.errors.join(' ')
  assert.match(text, /APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED/); assert.match(text, /SIGNATURE_MISSING.*该账号签名图片未配置/)
})

test('IC13 unrelated date error never refreshes matrix or loses the formal business reason', async () => {
  const { state, calls } = setup(); let reads = 0
  state.refreshRouteReadiness = async () => { reads++; return true }
  state.uploadSubmitterService.submit = async () => { throw Object.assign(new Error('预设日期不得早于正式业务当日'), { code: 400 }) }
  await submit(state)
  assert.equal(reads, 1); assert.equal(calls.success.length, 0)
  assert.match(calls.errors.join(' '), /预设日期不得早于正式业务当日/)
})

test('PUBLIC confirmed upload displays the exact original approval group candidate and policy', async () => {
  const { state, calls } = setup()
  state.routeReadiness.value = matrixHelpers.matrixReadiness(state.routeReadiness.value)
  await submit(state)
  assert.equal(calls.submitted.length, 1)
  const read = node => typeof node === 'string' ? node : Array.isArray(node?.children) ? node.children.map(read).join(' ') : String(node?.children || '')
  const text = read(calls.confirmations[0])
  assert.match(text, /原批准组 41/); assert.match(text, /正式批准主体/)
  assert.match(text, /组内全部批准/); assert.match(text, /实际参与：参与/)
  assert.match(text, /批准甲.*9223372036854775709/)
})

test('PUBLIC malformed original group refuses confirmation and final group mutation refuses the already opened confirmation', async () => {
  for (const variant of ['BEFORE', 'DURING']) {
    const { state, calls } = setup()
    state.routeReadiness.value = matrixHelpers.matrixReadiness(state.routeReadiness.value)
    if (variant === 'BEFORE') state.routeReadiness.value.runtimeRuleGroups[0].sourceNodeId = '999'
    else state.message.confirm = async () => { calls.confirmed++; state.routeReadiness.value.runtimeRuleGroups[0].applied = false }
    await submit(state)
    assert.equal(calls.submitted.length, 0); assert.equal(calls.success.length, 0)
    assert.match(calls.errors.join(' '), variant === 'BEFORE' ? /原组.*不一致/ : /变化/)
  }
})

test('IC31 actual upload confirmation passes the real compiler and source supervisor names and departments without client assignee fields',async()=>{
  const {state,calls}=setup();await submit(state)
  assert.equal(calls.submitted.length,1,calls.errors.join(' '))
  const read=node=>typeof node==='string'?node:Array.isArray(node?.children)?node.children.map(read).join(' '):String(node?.children||'')
  const text=read(calls.confirmations[0])
  assert.match(text,/编制人.*真实编制甲（质量）/);assert.match(text,/直属主管.*正式主管乙（研发）/)
  assert.doesNotMatch(text,/缓存名字不得作为确认事实/)
  assert.equal(calls.submitted[0][0].supervisorUserId,undefined);assert.equal(calls.submitted[0][0].compilerUserId,undefined)
})
test('IC31 actual upload refuses missing supervisor or compiler directory evidence before business confirmation and submission',async()=>{
  for(const change of [s=>s.routeReadiness.value.nodes=s.routeReadiness.value.nodes.filter(n=>n.stageCode!=='SUPERVISOR_REVIEW'),
    s=>s.routeReadiness.value.nodes.push({...s.routeReadiness.value.nodes[0]}),
    s=>s.approvalUsers.value=s.approvalUsers.value.filter(a=>a.id!=='71'),
    s=>s.approvalUsers.value.find(a=>a.id==='72').deptName=undefined]){
    const {state,calls}=setup();change(state);await submit(state)
    assert.equal(calls.confirmed,0);assert.equal(calls.submitted.length,0);assert.match(calls.errors.join(' '),/主管|编制|部门/)
  }
})
test('IC31 login account changes during route preparation or business confirmation invalidate the clicked upload',async()=>{
  for(const when of ['PREVIEW','CONFIRM']){
    const {state,calls}=setup()
    if(when==='PREVIEW')state.refreshRouteReadiness=async()=>{state.userStore.getUser.id='72';state.userStore.getUser.deptId='10';return true}
    else state.message.confirm=async()=>{calls.confirmed++;state.userStore.getUser.id='72';state.userStore.getUser.deptId='10'}
    await submit(state);assert.equal(calls.submitted.length,0);assert.match(calls.errors.join(' '),/变化/)
    assert.equal(calls.confirmed,when==='PREVIEW'?0:1)
  }
})
test('IC31 a changed formal supervisor route or tenant while final confirmation is open sends no old submission',async()=>{
  for(const scope of ['SUPERVISOR','TENANT']){
    let tenant='1';const {state,calls}=setup();state.getTenantId=()=>tenant
    state.message.confirm=async()=>{calls.confirmed++;if(scope==='SUPERVISOR')state.routeReadiness.value.nodes[0].resolvedUserIds=['71'];else tenant='2'}
    await submit(state);assert.equal(calls.confirmed,1);assert.equal(calls.submitted.length,0);assert.match(calls.errors.join(' '),/变化/)
  }
})
test('IC31 enabled-account read arriving after an actor or tenant switch stays unavailable rather than publishing former compiler facts',async()=>{
  for(const scope of ['ACTOR','TENANT']){
    let release,tenant='1';const {state}=setup();state.getTenantId=()=>tenant
    Object.assign(state,{approvalUsersRequestSeq:0,getSimpleUserList:()=>new Promise(r=>release=r)})
    const pending=approvalDirectoryLoader(state)()
    if(scope==='ACTOR')state.userStore.getUser.id='72';else tenant='2'
    release([{id:'71',nickname:'旧编制甲',deptId:'7',deptName:'质量'}]);await pending
    assert.equal(state.approvalUsers.value.length,0);assert.equal(state.approvalUsersLoading.value,false)
    assert.match(state.approvalUsersError.value,/账号或租户已变化/)
  }
})
test('IC31 native login has no directory fallback and confirmation notices directory department or name changes',async()=>{
  for(const change of [s=>s.userStore.getUser.id=9007199254740992,s=>s.userStore.getUser.deptId='8']){
    const {state,calls}=setup();change(state);await submit(state)
    assert.equal(calls.confirmed,0);assert.equal(calls.submitted.length,0);assert.match(calls.errors.join(' '),/编制/)
  }
  for(const field of ['name','deptName']){
    const {state,calls}=setup();state.message.confirm=async()=>{calls.confirmed++;state.approvalUsers.value.find(row=>row.id==='72')[field]='其它事实'}
    await submit(state);assert.equal(calls.confirmed,1);assert.equal(calls.submitted.length,0);assert.match(calls.errors.join(' '),/变化/)
  }
})
test('IC31 actual external-review confirmation does not require native people or add client supervisor selection',async()=>{
  const {state,calls}=setup();state.isExternalReview.value=true;state.isNormalNewUpload.value=false
  state.formData.processType='EXTERNAL_REVIEW';state.userStore.getUser={id:0,deptId:0}
  state.routeReadiness.value.nodes=state.routeReadiness.value.nodes.filter(node=>node.stageCode!=='SUPERVISOR_REVIEW')
  state.approvalUsers.value=state.approvalUsers.value.filter(row=>row.id==='9223372036854775709')
  await submit(state);assert.equal(calls.submitted.length,1,calls.errors.join(' '))
  const read=node=>typeof node==='string'?node:Array.isArray(node?.children)?node.children.map(read).join(' '):String(node?.children||'')
  assert.doesNotMatch(read(calls.confirmations[0]),/编制人|直属主管/)
})

test('IC31 actor or supervisor changes during the original matrix confirmation prevent the later business confirmation and submission',async()=>{
  for(const scope of ['ACTOR','SUPERVISOR']){
    const {state,calls}=setup()
    state.approvalMatrixState.value.accept(state.approvalMatrixState.value.begin(),matrixHelpers.matrixReadiness({ready:true,blockers:[],nodes:[]},{},1,2))
    state.message.confirm=async()=>{
      calls.confirmed++
      if(scope==='ACTOR'){state.userStore.getUser.id='72';state.userStore.getUser.deptId='10'}
      else state.routeReadiness.value.nodes[0].resolvedUserIds=['71']
    }
    await submit(state)
    assert.equal(calls.confirmed,1);assert.equal(calls.submitted.length,0);assert.match(calls.errors.join(' '),/变化/)
  }
})
test('IC31 actual plain draft SAVE remains available with genuine supervisor not-ready matrix facts and no source pair',async()=>{
  const {state,calls}=setup();const saved=[]
  state.resolveUploadErrorMessage=submitterModel.exports.resolveUploadErrorMessage
  state.buildSubmittedApplicationRoute=id=>({path:'/dcc/controlled-file/detail/'+id});state.router.resolve=()=>({path:'/dcc/controlled-file/browser'})
  state.uploadSubmitterService.saveDraft=async(...args)=>{saved.push(args);return 11}
  const unready=matrixHelpers.matrixReadiness({ready:false,nodes:[],blockers:[{reasonCode:'DIRECT_SUPERVISOR_SOURCE_NOT_CONFIGURED',message:'主管来源真实未配置'}]})
  state.approvalMatrixState.value.accept(state.approvalMatrixState.value.begin(),unready);state.routeReadiness.value=unready
  await saveDraft(state)
  assert.equal(saved.length,1,calls.errors.join(' '));assert.equal(calls.submitted.length,0)
  assert.equal(saved[0][0].approvalMatrixSelection.expectedSupervisorSourceDigest,undefined)
  assert.equal(saved[0][0].approvalMatrixSelection.supervisorSourceToken,undefined)
})
test('IC31 actual formal payload carries its frozen supervisor pair and never exposes technical source values in business confirmation',async()=>{
  const {state,calls}=setup();await submit(state)
  assert.equal(calls.submitted.length,1,calls.errors.join(' '))
  const selection=calls.submitted[0][0].approvalMatrixSelection
  assert.equal(selection.expectedSupervisorSourceDigest,'c'.repeat(64));assert.equal(selection.supervisorSourceToken,'fixture-server-supervisor-source')
  const read=node=>typeof node==='string'?node:Array.isArray(node?.children)?node.children.map(read).join(' '):String(node?.children||'')
  assert.doesNotMatch(read(calls.confirmations[0]),/fixture-server-supervisor-source|cccccccccccccccc/)
})
