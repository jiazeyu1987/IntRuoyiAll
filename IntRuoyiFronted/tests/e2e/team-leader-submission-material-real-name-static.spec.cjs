const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const extractFunction = (name, nextName) => {
  const start = page.indexOf(`const ${name}`)
  const end = page.indexOf(`const ${nextName}`, start)
  assert.ok(start >= 0 && end > start, `${name} block must exist`)
  return page.slice(start, end)
}

const materialRowBlock = extractFunction('toProductionMaterialRow', 'resolveProductionMaterialRows')
const resolveRowsBlock = extractFunction('resolveProductionMaterialRows', 'resolveCorrectionMaterialNames')
const detailRowsBlock = extractFunction(
  'resolveSubmissionMaterialDetailRows',
  'resolveSubmissionMaterialSummaryItems'
)

assert.match(
  page,
  /const resolveSubmittedMaterialName\s*=\s*\(/,
  'team leader submission detail must centralize real material name resolution'
)
assert.doesNotMatch(
  materialRowBlock,
  /`物料 \$\{index \+ 1\}`/,
  'material rows must not create visible placeholder names such as 物料 1'
)
assert.match(
  materialRowBlock,
  /resolveSubmittedMaterialName\(value\)/,
  'material row normalization must read formal material name aliases from the submitted payload'
)
assert.match(
  resolveRowsBlock,
  /rootPayloadMaterialRows[\s\S]*materialId[\s\S]*materialName/,
  'row material details must be enriched from original payload materialDetails when the list row only has placeholders'
)
assert.doesNotMatch(
  detailRowsBlock,
  /materialText:\s*\[item\.materialName,\s*item\.materialCode\]/,
  'expanded material title must not render a placeholder materialName before checking the real payload name'
)
assert.match(
  detailRowsBlock,
  /materialText:\s*resolveSubmissionMaterialTitle\(item\)/,
  'expanded material title must render through the real-name title resolver'
)

console.log('PASS: team leader submission material detail titles use real material names')
