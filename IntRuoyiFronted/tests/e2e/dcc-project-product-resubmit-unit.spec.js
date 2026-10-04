const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vm = require('node:vm')
const root = path.resolve(__dirname, '../..')
const modulePath = path.join(root, 'src/views/dcc/controlled-file/basic-data/components/project-product-resubmit.ts')
assert.ok(fs.existsSync(modulePath), '驳回申请必须有正式恢复/申请人重提逻辑')
const source = fs.readFileSync(modulePath, 'utf8')
const exportsObject = {}
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText, {
  exports: exportsObject, Error,
  require: id => {
    if (id === '../../project-attributes/state') {
      const exports = {}
      const state = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/project-attributes/state.ts'), 'utf8')
      vm.runInNewContext(ts.transpileModule(state, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, { exports, Error })
      return exports
    }
    throw new Error('unexpected module: ' + id)
  }
})
const { canResubmitProjectProductRequest, restoreRejectedProjectProductForm } = exportsObject
const attributes = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'Y', transferTo: '手改目标' }
const rejected = { id: 31, applicantUserId: 9, status: 'REJECTED', projectName: '旧项目', projectCode: 'B',
  projectLeaderUserId: 7, folderTemplateId: 17, productName: '产品', productCode: 'PR', classification: '一类',
  defaultAttributesJson: JSON.stringify(attributes), rejectReason: '字段需修改', remark: '原备注' }
assert.equal(canResubmitProjectProductRequest(rejected, 9), true)
assert.equal(canResubmitProjectProductRequest(rejected, 1), false)
assert.equal(canResubmitProjectProductRequest({ ...rejected, status: 'PENDING_APPROVAL' }, 9), false)
assert.equal(canResubmitProjectProductRequest({ ...rejected, resubmittedRequestId: 32 }, 9), false)
const restored = restoreRejectedProjectProductForm(rejected, 9)
assert.deepEqual(JSON.parse(JSON.stringify(restored.defaultAttributes)), attributes)
assert.equal(restored.projectLeaderUserId, 7); assert.equal(restored.folderTemplateId, 17)
restored.defaultAttributes.targetMarkets.push('FDA')
assert.equal(JSON.parse(rejected.defaultAttributesJson).targetMarkets.length, 1)
assert.throws(() => restoreRejectedProjectProductForm(rejected, 1))
assert.throws(() => restoreRejectedProjectProductForm({ ...rejected, defaultAttributesJson: undefined }, 9))
assert.throws(() => restoreRejectedProjectProductForm({ ...rejected, defaultAttributesJson: '{bad}' }, 9))
const page = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'), 'utf8')
for (const marker of ['修改后重提', 'restoreRejectedProjectProductForm', 'resubmitDccProjectProductRequest', 'projectProductResubmitRequestId', 'rejectReason']) assert.ok(page.includes(marker), marker)
const api = fs.readFileSync(path.join(root, 'src/api/dcc/controlledFile/projectProductRequests.ts'), 'utf8')
assert.ok(api.includes('/resubmit'))
console.log('PASS: 驳回原字段/实际属性恢复、申请人身份、深复制、重复分叉及页面/API重提合同')
