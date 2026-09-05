const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(
  repoRoot,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8')

assert.match(
  source,
  /:data="buildActiveOrderProductionSubmissionRows\(process\)"/,
  'Production submission table must render flattened output-material rows, not raw submission events.'
)

assert.match(
  source,
  /row-key="key"/,
  'Production submission table must use the flattened row key so multiple output materials render as distinct rows.'
)

assert.match(
  source,
  /interface ActiveOrderProductionSubmissionRow[\s\S]*materialName\?: string[\s\S]*outputQuantity\?: number \| string[\s\S]*lossQuantity\?: number \| string/,
  'Flattened production rows must carry each output material name, output quantity, and loss quantity.'
)

assert.match(
  source,
  /for \(const \[materialIndex, material\] of \(submission\.materials \?\? \[\]\)\.entries\(\)\)[\s\S]*rows\.push\(toActiveOrderProductionMaterialSubmissionRow\(submission, material, materialIndex\)\)[\s\S]*key: `\$\{submission\.eventId\}-\$\{materialIndex\}-\$\{material\.materialId\}-\$\{material\.materialCode\}`[\s\S]*materialId: material\.materialId[\s\S]*materialName: material\.materialName[\s\S]*outputQuantity: material\.outputQuantity[\s\S]*lossQuantity: material\.lossQuantity[\s\S]*devices: material\.devices\?\.length \? material\.devices : submission\.devices/,
  'Each output material must be pushed as its own visible row and inherit submission devices when material devices are empty.'
)

assert.match(
  source,
  /<el-table-column label="输出物料"[\s\S]*row\.materialName \|\| '-'/,
  'Production table must expose output material as a first-class standard list column.'
)

assert.match(
  source,
  /<el-table-column label="完成数量"[\s\S]*formatTraceQuantity\(row\.outputQuantity \?\? row\.submittedQuantity\)/,
  'Production table completion quantity must prefer output-material quantity and only use event quantity when no material rows exist.'
)

assert.match(
  source,
  /<el-table-column label="损耗数量"[\s\S]*formatTraceQuantity\(row\.lossQuantity\)/,
  'Production table must show loss quantity per output material row.'
)

assert.doesNotMatch(
  source,
  /:data="process\.submissions"/,
  'Production table must not bind directly to process.submissions because that collapses multiple output materials into one event row.'
)

console.log('PASS: team leader active order output-material rows static contract')
