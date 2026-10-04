const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const context = { exports: {}, Error, require: name => {
  if (name.endsWith('project-attributes/state')) {
    const moduleContext = { exports: {}, Error }
    vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/project-attributes/state.ts', 'utf8'),
      { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, moduleContext)
    return moduleContext.exports
  }
  throw new Error('Unexpected dependency: ' + name)
}}
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/submitter.ts', 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, context)
const actual = { targetMarkets: ['CE'], licenseHolder: 'N', actualManufacturer: 'Y', documentTransfer: 'N' }
const draft = { categoryId: 2, directoryId: 3, fileName: 'SOP', fileNumber: 'N-1', productMasterId: null,
  productCode: '12345678901234', dccProjectCodeId: 5, fileTypeTaxonomyId: 6, relatedControlledFileIds: [],
  needTraining: false, processType: 'CONTROLLED_FILE', changeType: 'NEW', versionNo: 'A/1', effectiveDate: '2099-12-20',
  projectAttributes: actual, selectedSignoffDepartmentIds: [7], projectFolderId: '9007199254740993', projectFolderChangeReason: '文件归入项目质量目录' }
const uploaded = { sessionId: 'session-1', uploadTicket: 'ticket-1', fileName: 'SOP.docx' }
test('ordinary upload includes a frozen copy of the actual project attributes and selected departments', () => {
  const request = JSON.parse(JSON.stringify(draft))
  const payload = context.exports.buildSubmitPayload(request, uploaded)
  request.projectAttributes.targetMarkets.push('FDA'); request.selectedSignoffDepartmentIds.push(9)
  assert.deepEqual(JSON.parse(JSON.stringify(payload.projectAttributes)), actual)
  assert.deepEqual(JSON.parse(JSON.stringify(payload.selectedSignoffDepartmentIds)), [7])
  assert.equal(payload.defaultSource, undefined)
  assert.equal(payload.projectFolderId, '9007199254740993')
  assert.equal(payload.projectFolderChangeReason, '文件归入项目质量目录')
})
test('ordinary upload rejects missing logical folder and empty placement reason', () => {
  assert.throws(() => context.exports.buildSubmitPayload({ ...draft, projectFolderId: undefined }, uploaded))
  assert.throws(() => context.exports.buildSubmitPayload({ ...draft, projectFolderChangeReason: ' ' }, uploaded))
})
test('ordinary upload rejects missing or invalid actual properties before the API request', () => {
  for (const attributes of [undefined, { ...actual, targetMarkets: [] }]) {
    assert.throws(() => context.exports.buildSubmitPayload({ ...draft, projectAttributes: attributes }, uploaded))
  }
})
test('external review is not required to fabricate DCC project attributes', () => {
  const result = context.exports.buildSubmitPayload({ ...draft, processType: 'EXTERNAL_REVIEW', projectAttributes: undefined }, uploaded)
  assert.equal(result.projectAttributes, undefined)
})
