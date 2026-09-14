const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const columnMatch = page.match(
  /<el-table-column label="允许超量比例[^"]*"[\s\S]*?<\/el-table-column>\s*<el-table-column label="损耗原因"/u
)
assert(columnMatch, 'The process config overage column must remain a bounded table column block.')
const column = columnMatch[0]

assert.match(
  column,
  /<el-table-column label="允许超量比例\(%\)"/u,
  'The overage column title must include the percentage unit.'
)
assert.doesNotMatch(
  column,
  /<span>\s*%\s*<\/span>/u,
  'The percentage unit must not be repeated beside every row input.'
)
assert.match(
  column,
  /<el-input-number[\s\S]*v-model="row\.overagePercent"/u,
  'The overage column must keep the editable overage value input.'
)

const detailMatch = page.match(
  /<el-descriptions-item label="允许超量比例">([\s\S]*?)<\/el-descriptions-item>/u
)
assert(detailMatch, 'The detail dialog must keep an overage percentage field.')
assert.match(
  detailMatch[1],
  /resolveProcessConfigOveragePercent\(processConfigDetailRow\)[\s\S]*\}\}\s*%/u,
  'The detail dialog must continue displaying the percentage value with its unit.'
)

console.log('team-leader-overage-percent-header-static PASS')
