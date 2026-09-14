const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/production-release/PqcProductionReleasePage.vue'
)
const page = fs.readFileSync(pagePath, 'utf8')

assert.match(page, /let\s+listRequestSequence\s*=\s*0/)
assert.match(page, /const\s+requestId\s*=\s*\+\+listRequestSequence/)
assert.match(page, /if\s*\(requestId\s*!==\s*listRequestSequence\)/)
assert.match(page, /row\.underReview\s*\|\|\s*row\.approvalReady\s*===\s*false/)
assert.doesNotMatch(page, /row\.underReview\s*\|\|\s*!row\.approvalReady/)

console.log('PQC production release frontend performance contract: PASS')
