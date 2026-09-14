const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(root, '../IntRuoyiBackend')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const packageJson = JSON.parse(read('package.json'))
const externalReview = read('src/views/dcc/controlled-file/external-review/index.vue')
const metadataDialog = read('src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue')
const nasPage = read('src/views/system/nas/index.vue')
const workflowApi = read('src/api/dcc/controlledFile/workflow.ts')
const uploadSubmitter = read('src/views/dcc/controlled-file/upload/submitter.ts')
const dccController = readBackend(
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java'
)

assert.equal(
  packageJson.scripts['e2e:dcc:nas-product-code-unified:static'],
  'node tests/e2e/dcc-nas-product-code-unified-static.spec.js',
  'package.json must expose the DCC/NAS product code unified static contract'
)

for (const [name, source] of [
  ['external review', externalReview],
  ['metadata dialog', metadataDialog],
  ['NAS transfer', nasPage]
]) {
  assert(
    !source.includes('getDccProductOptions') &&
      !source.includes('DCC_PRODUCT_STATUS_ENABLE') &&
      !source.includes('productOptions') &&
      !source.includes('productOptionsLoading') &&
      !source.includes('validateProductMasterSelection') &&
      !source.includes('产品主数据'),
    `${name} must not load or present product master options`
  )
  assert.match(
    source,
    /dccProjectCodeId/,
    `${name} must use DCC project code selection as the product number source`
  )
  assert.match(
    source,
    /productMasterId:\s*null/,
    `${name} write payload must explicitly clear productMasterId for new DCC/NAS writes`
  )
  assert.match(
    source,
    /projectCode/,
    `${name} must derive product number from the selected DCC project code`
  )
}

assert(
  !workflowApi.includes('getDccProductOptions') && !workflowApi.includes('/dcc/controlled-files/product-options'),
  'DCC frontend API wrapper must not expose product master options for DCC/NAS writes'
)
for (const [name, pattern] of [
  [
    'controlled upload submit request',
    /interface ControlledFileSubmitReqVO[\s\S]*?productMasterId\?: null[\s\S]*?dccProjectCodeId/
  ],
  [
    'controlled metadata update request',
    /interface ControlledFileMetadataUpdateReqVO[\s\S]*?productMasterId\?: null[\s\S]*?dccProjectCodeId/
  ],
  [
    'NAS transfer request',
    /interface ControlledFileNasTransferReqVO[\s\S]*?dccProjectCodeId: number[\s\S]*?productMasterId\?: null/
  ]
]) {
  assert.match(workflowApi, pattern, `${name} must only allow clearing productMasterId as null`)
}
assert.match(
  uploadSubmitter,
  /productMasterId: null/,
  'DCC upload draft model must not accept numeric productMasterId input'
)
assert(
  !dccController.includes('@GetMapping("/product-options")') &&
    !dccController.includes('listSimpleProducts'),
  'DCC backend controller must not expose product master options for DCC/NAS writes'
)

console.log('PASS: DCC/NAS product code unified static contract')
