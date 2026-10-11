const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve('src/views/mes/pro/edhr-batch/completionSourcePreview.ts'), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const context = { exports: {} }
vm.runInNewContext(compiled, context)
const resolve = context.exports.resolveCompletionSourceDraftPreview
const preview = {
  executionId: '300', executionCreated: true,
  formViewModel: { cellValuesJson: '[{"rowIndex":1,"columnIndex":0,"value":10}]', remark: 'draft', signatureCellMarkers: ['draft'] },
  completionSourceMaterialization: {
    status: 'APPLIED', executionStatus: 0, receiptId: '400', executionId: '300', auditBatchId: '1000',
    sourceSnapshotHash: 'a'.repeat(64), mappingHash: 'b'.repeat(64), fieldAuditHeadHash: 'c'.repeat(64), appliedFieldCount: 1
  }
}
assert.equal(resolve(preview).cellValuesJson, preview.formViewModel.cellValuesJson)
assert.equal(resolve(preview).remark, undefined)
assert.equal(resolve(preview).signatureCellMarkers.length, 0)
assert.equal(resolve({ ...preview, completionSourceMaterialization: undefined }), undefined)
for (const status of ['PENDING', 'CONFIGURATION_MISSING']) {
  assert.equal(resolve({ ...preview, completionSourceMaterialization: { ...preview.completionSourceMaterialization, status } }), undefined)
}
for (const change of [{ executionId: '301' }, { auditBatchId: undefined }, { sourceSnapshotHash: 'bad' }, { mappingHash: '' }, { appliedFieldCount: 0 }, { executionStatus: undefined }, { executionStatus: 1 }, { executionStatus: 2 }, { executionStatus: 3 }, { executionStatus: 4 }]) {
  assert.equal(resolve({ ...preview, completionSourceMaterialization: { ...preview.completionSourceMaterialization, ...change } }), undefined)
}
assert.equal(resolve({ ...preview, executionCreated: false }), undefined)
const page = fs.readFileSync(path.resolve('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'), 'utf8')
assert(page.includes('resolveCompletionSourceDraftPreview(selectedTaskPreview.value)'))
assert(page.includes('cellValuesJson: EMPTY_FORM_CELL_VALUES_JSON'))
assert(page.includes('生产事实已回填，表单尚未提交'))
assert(page.includes("status === 'CONFIGURATION_MISSING'"))
console.log('PASS completion source preview: verified projection visible; manual draft, missing mapping and mismatched evidence hidden')
