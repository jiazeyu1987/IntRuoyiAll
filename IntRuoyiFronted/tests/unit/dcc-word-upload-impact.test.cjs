const { test } = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const { parse, compileScript } = require('vue/compiler-sfc')
const vue = name => parse(fs.readFileSync('src/views/dcc/controlled-file/' + name, 'utf8')).descriptor
const ref = value => ({ value }), transpile = code => ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const upload = vue('upload/index.vue'), ast = ts.createSourceFile('upload.ts', upload.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = name => ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === name)).getText(ast)
const identityWatcher = ast.statements.find(s => s.expression?.expression?.getText(ast) === 'watch' && s.getText(ast).includes('() => [formData.fileNumber,'))
function identityHost() {
  const h = { exports: {}, formData: { fileNumber: 'N-1', dccProjectCodeId: '5', fileTypeTaxonomyId: '8', processType: 'CONTROLLED_FILE' },
    userStore: { getUser: { id: '71' } }, getVisitTenantId: () => undefined, getTenantId: () => '1', uploadApplicationActorKey: (user, tenant) => `${tenant}/${user.id}`,
    currentVersionLookupSeq: 0, currentVersionLookupTimer: undefined, currentVersionLookupLoading: ref(false), currentVersionLookupError: ref(''), currentVersionInfo: ref(), selectedHistoryVersion: ref(),
    submitFieldErrors: {}, clearSubmitFieldErrors() {}, clearTimeout() {}, setTimeout: () => 1, isExternalReview: ref(false), message: { error() {} },
    resolveUploadErrorMessage: error => error.message, applyDccProjectCodeProductNumber() {}, canRetryWorkingDraftCreationAfterCurrentVersionConflictMessage: () => false,
    watch: (getter, callback, options) => { h.watchIdentity = getter; h.changeIdentity = callback; assert.equal(options.flush, 'sync') } }
  vm.runInNewContext(transpile([declaration('clearCurrentVersionInfo'), declaration('loadCurrentVersionByFileNumber'), identityWatcher.getText(ast), 'exports.load=loadCurrentVersionByFileNumber'].join('\n')), h)
  return h
}
test('changing any formal upload identity or login invalidates a pending number response immediately', async () => {
  for (const change of [h => h.formData.dccProjectCodeId='6', h => h.formData.fileTypeTaxonomyId='9', h => h.formData.fileNumber='N-2', h => h.userStore.getUser.id='72', h => h.getTenantId=()=>'2']) {
    const h = identityHost(); let complete
    h.getControlledFileCurrentVersion = () => new Promise(resolve => complete = resolve)
    const oldKey = JSON.stringify(h.watchIdentity()), pending = h.exports.load()
    change(h); assert.notEqual(JSON.stringify(h.watchIdentity()), oldKey); h.changeIdentity()
    complete({ matched: true, fileNumber: 'N-1', fileName: 'former tenant' }); await pending
    assert.equal(h.currentVersionInfo.value, undefined); assert.equal(h.currentVersionLookupLoading.value, false)
  }
})
test('draft SAVE retries its own upload session but blocks another session and an occupied ACTIVE identity', async () => {
  const helper = fs.readFileSync('src/views/dcc/controlled-file/upload/submitter.ts', 'utf8')
  const helpers = { exports: {} }
  vm.runInNewContext(transpile(helper.slice(helper.indexOf('const trimText ='), helper.indexOf('const PRODUCT_CODE_PATTERN'))
    + helper.slice(helper.indexOf('export const isFileNumberChainConflictMessage'), helper.indexOf('export const EDITABLE_SOURCE_EXTENSIONS')).replace('export const', 'const')
    + '\nexports.conflict=isFileNumberChainConflictMessage'), helpers)
  const conflict = '文件编号已重复：当前项目、文件类型下已存在该编号'
  assert.equal(helpers.exports.conflict(conflict), true)
  for (const scenario of ['own-session', 'another-session', 'active-identity']) {
    const h = identityHost(); let saved = 0
    Object.assign(h, {
      uploadSessionId: 'page-session', uploadSessionBinding: ref({ clientSessionId: scenario === 'another-session' ? 'foreign-session' : 'page-session', scopedSessionId: 'source-session' }),
      previewUpload: ref({ sessionId: 'source-session', uploadTicket: 'source-ticket' }), isFileNumberChainConflictMessage: helpers.exports.conflict,
      submitLoading: ref(false), uploadSubmitted: ref(false), projectAttributesLoading: ref(false), uploadPreviewLoading: ref(false), hasUnreadyAttachmentUploads: ref(false),
      projectProductLoading: ref(false), projectProductError: ref(''), projectProductResolvedId: ref('5'), isVersionNoFormatValid: ref(true), isProductRequiredForSelectedCategory: ref(false),
      validateDccProjectProductCode: () => ({ valid: true }), resolveReadyAttachmentUploads: () => [], captureUploadSubmissionContext: () => 'same-context',
      refreshRouteReadiness: async () => {}, routeReadinessError: ref(''), approvalMatrixState: ref({ after: {} }), isCustomApprovalSource: ref(false),
      confirmUploadMatrixChange: async () => true, freezeUploadApplicationDraft: () => ({}), uploadSubmitterService: { saveDraft: async () => { saved++; return '20' } },
      referenceIdentity: String, buildSubmittedApplicationRoute: () => ({}), router: { resolve: () => ({ path: '/browser' }), push: async () => {} },
      message: { error() {}, success() {} }, isApprovalMatrixStaleError: () => false
    })
    h.formData.categoryId='2';h.formData.versionNo='A/1';h.formData.changeType='NEW'
    h.getControlledFileCurrentVersion = async () => { if (scenario === 'active-identity') return { matched: true }; throw Error(conflict) }
    h.existingUploadIdentityBlockReason = { get value() { return h.currentVersionInfo.value?.matched ? conflict : '' } }
    vm.runInNewContext(transpile(declaration('canRetryWorkingDraftCreationAfterCurrentVersionConflictMessage') + '\n' + declaration('saveUploadDraft') + '\nexports.save=saveUploadDraft'), h)
    await h.exports.save()
    assert.equal(saved, scenario === 'own-session' ? 1 : 0, scenario)
  }
})
const impact = vue('detail/DetailObsoleteImpact.vue')
function impactHost() {
  const h = { exports: {}, props: { active: true, fileId: '20' }, relations: ref(), usage: ref(), error: ref(''), loading: ref(false), page: ref(1), pageSize: 10, generation: 0,
    referenceIdentity: value => { if (!/^[1-9]\d*$/.test(String(value))) throw Error('invalid identity'); return String(value) },
    listCurrentRelations: async () => ({ sourceControlledFileId: '20', files: [{ masterId: '30', controlledFileId: '31', fileName: 'related.pdf', fileNumber: 'R-1', versionNo: 'A/1', status: 'ACTIVE' }] }),
    getProjectReferenceUsage: async () => ({ sourceControlledFileId: '20', tenantId: '1', masterId: '2', referenceProjectCount: 3 }),
    getProjectReferenceUsagePage: async () => ({ sourceControlledFileId: '20', tenantId: '1', masterId: '2', referenceProjectCount: 3, total: 1, detailsRestricted: true, list: [{ projectName: '实际引用项目' }] }) }
  const script = impact.scriptSetup.content, start = script.indexOf('const load ='), end = script.indexOf('watch(', start)
  vm.runInNewContext(transpile(script.slice(start,end)+'\nexports.load=load;exports.page=loadPage'),h)
  return h
}
test('obsolete impact compiles and displays genuine relations, restricted reference counts and projects', async () => {
  assert.ok(compileScript(impact, { id: 'obsolete-impact', inlineTemplate: true }).content)
  const h=impactHost(); await h.exports.load()
  assert.equal(h.relations.value[0].fileNumber,'R-1'); assert.equal(h.usage.value.referenceProjectCount,3)
  assert.equal(h.usage.value.list[0].projectName,'实际引用项目'); assert.equal(h.error.value,''); assert.equal(h.loading.value,false)
})
test('obsolete impact failures and responses for an old file never become verified empty impact', async () => {
  const h=impactHost();h.getProjectReferenceUsage=async()=>{throw Error('permission denied')};await h.exports.load()
  assert.match(h.error.value,/permission denied/);assert.equal(h.relations.value,undefined);assert.equal(h.usage.value,undefined);assert.equal(h.loading.value,false)
  const c=impactHost();let complete;c.listCurrentRelations=()=>new Promise(resolve=>complete=resolve)
  const pending=c.exports.load();c.generation++;complete({sourceControlledFileId:'20',files:[]});await pending
  assert.equal(c.relations.value,undefined);assert.equal(c.usage.value,undefined);assert.equal(c.error.value,'')
})
