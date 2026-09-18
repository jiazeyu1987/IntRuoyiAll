const fs = require('fs')
const path = require('path')
const assert = require('assert')

const pagePath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/production-release/PqcProductionReleasePage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')

const labelFunctionMatch = source.match(
  /const resolveStatusLabel = \(row: MesPqcProductionReleasePageItemRespVO\) => \{[\s\S]*?\n\}/
)

assert(labelFunctionMatch, 'PQC production release page must define resolveStatusLabel.')

const labelFunctionSource = labelFunctionMatch[0]

assert(
  /\[PQC_RELEASE_VIEW_RELEASED\]:\s*'已生产放行'/.test(labelFunctionSource),
  'Released PQC production release rows must display status label "已生产放行".'
)

console.log('PASS: PQC production release status label static contract')
