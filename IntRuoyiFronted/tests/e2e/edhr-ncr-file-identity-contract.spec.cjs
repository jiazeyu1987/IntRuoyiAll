const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const apiSource = fs.readFileSync(
  path.resolve(__dirname, '../../src/api/mes/pro/edhr/nonconformanceReview.ts'),
  'utf8'
)
const pageSource = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'),
  'utf8'
)

assert.match(apiSource, /uploadNonconformanceReviewMaterial/)
assert.match(apiSource, /fileId: EdhrRouteId/)
assert.match(pageSource, /fileId/)
assert.match(pageSource, /handleMaterialUpload/)
assert.doesNotMatch(pageSource, /<UploadFile/)

console.log('edhr-ncr-file-identity-contract: PASS')
