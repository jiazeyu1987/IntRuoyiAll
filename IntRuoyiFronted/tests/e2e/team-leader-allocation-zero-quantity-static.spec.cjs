const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

assert.match(
  page,
  /v-model="row\.allocatedQuantity"[\s\S]*?:min="0"[\s\S]*?:precision="0"/
)

const normalizer = page.match(
  /const\s+normalizeAllocationSubmitQuantity[\s\S]*?\n\}/
)?.[0] || ''
assert.match(
  normalizer,
  /value\s*===\s*undefined\s*\|\|\s*value\s*===\s*null\s*\|\|\s*String\(value\)\.trim\(\)\s*===\s*''[\s\S]*?return\s+0/
)
assert.match(normalizer, /parsed\s*<\s*0/)
assert.doesNotMatch(normalizer, /parsed\s*<=\s*0/)

const submitBuilder = page.match(/const\s+buildAllocationSubmitLines[\s\S]*?\n\}/)?.[0] || ''
assert.match(
  submitBuilder,
  /normalizeAllocationSubmitQuantity\(line\.allocatedQuantity,\s*'分配数量必须为0或正整数'\)/
)
assert.match(submitBuilder, /allocatedQuantity\s*===\s*0[\s\S]*?return\s*\[\]/)

console.log('team-leader allocation zero quantity static assertions passed')
