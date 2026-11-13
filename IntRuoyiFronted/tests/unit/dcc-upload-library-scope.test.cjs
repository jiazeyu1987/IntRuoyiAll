const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const ctx = { exports: {}, Error }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/project-attributes/state.ts','utf8'), {compilerOptions:{ module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022 }}).outputText,ctx)
test('library application round preserves explicit scope without project attribute values', () => {
  const marker = { libraryScope:'QMS',targetMarkets:null,otherMarket:null,licenseHolder:null,actualManufacturer:null,documentTransfer:null,transferTo:null }
  assert.equal(ctx.exports.validateAttributes(marker), marker)
  assert.throws(() => ctx.exports.validateAttributes({...marker,targetMarkets:['NA']}), /独立文件库/)
  assert.throws(() => ctx.exports.validateAttributes({...marker,libraryScope:'PROJECT'}), /独立文件库/)
  const saved = ctx.exports.createAttributeState('UPLOAD',{projectId:'91',applicationType:'UPLOAD',defaultSource:marker,actual:marker})
  assert.equal(ctx.exports.buildSnapshot(saved).actual.libraryScope,'QMS')
})

test('library submission does not read the retained project attribute panel', () => {
  const source = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
  const capture = source.slice(source.indexOf('const captureUploadSubmissionContext ='), source.indexOf('const confirmUploadApplication ='))
  let reads = 0
  const snapshot = { projectId: '1111', actual: { targetMarkets: ['CE'] } }
  const runtime = { JSON, Object, uploadApplicationActorKey: () => 'actor', userStore: { getUser: {} }, getVisitTenantId: () => null, getTenantId: () => 1,
    formData: {}, previewUpload: {}, resolveReadyAttachmentUploads: () => [], selectedUploadRelations: {},
    isExternalReview: { value: false }, isLibraryUpload: { value: true },
    projectAttributesPanel: { value: { getSnapshot: () => { reads++; if (runtime.isLibraryUpload.value) throw Error('请先读取项目默认属性'); return snapshot } } },
    selectedProjectCode: {}, projectProduct: {}, selectedProjectFolder: {}, selectedFileTypeTaxonomyLeafName: {}, selectedCategory: {}, approvalUsers: {}, signoffDepartments: {} }
  vm.createContext(runtime)
  vm.runInContext(capture + '\nthis.capture = captureUploadSubmissionContext', runtime)
  for (const scope of ['QMS', 'GENERAL']) {
    runtime.formData.libraryScope = scope
    assert.equal(JSON.parse(runtime.capture()).attributes, undefined)
  }
  assert.equal(reads, 0)
  runtime.isLibraryUpload.value = false
  assert.deepEqual(JSON.parse(runtime.capture()).attributes, snapshot)
  assert.equal(reads, 1)
})
