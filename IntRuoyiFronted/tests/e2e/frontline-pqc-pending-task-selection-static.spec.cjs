const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelSource = fs
  .readFileSync(
    path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

const start = panelSource.indexOf('const hasPqcTaskOptionSnapshot =')
const end = panelSource.indexOf('const getPqcTaskOptions =', start)
assert.ok(start >= 0 && end > start, 'PQC selectable-task predicate must exist.')

const predicateSource = panelSource.slice(start, end)
assert.match(
  predicateSource,
  /option\.taskStatus\s*===\s*'PENDING'/,
  'Only PENDING PQC tasks may enter the employee-switch and formal-submit context.'
)

console.log('frontline-pqc-pending-task-selection-static: PASS')
