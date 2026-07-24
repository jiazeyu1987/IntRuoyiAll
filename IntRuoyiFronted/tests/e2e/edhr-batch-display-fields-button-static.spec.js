const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const actionsMatch = source.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, 'eDHR batch execution page must keep a toolbar actions slot.')
const actionsSource = actionsMatch[1]

assert.doesNotMatch(
  actionsSource,
  /<el-button\s+type="primary"\s+@click="handleQuery">\s*查询\s*<\/el-button>/,
  'eDHR batch execution red-box toolbar must remove the extra query button.'
)

assert.match(
  actionsSource,
  /<UserTableColumnSettings[\s\S]*:columns="edhrBatchExecutionColumns"[\s\S]*:saving="edhrBatchExecutionColumnSaving"[\s\S]*:show-reset="false"[\s\S]*@change="saveEdhrBatchExecutionColumnConfig"[\s\S]*@reset="resetEdhrBatchExecutionColumnConfig"/,
  'eDHR batch execution red-box toolbar must render the same visible-field settings behavior as schedule order.'
)

assert.match(
  source,
  /import UserTableColumnSettings from '@\/components\/UserTableColumnSettings\/index\.vue'/,
  'eDHR batch execution page must directly import UserTableColumnSettings for the red-box toolbar entry.'
)

assert.match(
  source,
  /<UnifiedListTemplate[\s\S]*:show-column-settings="false"[\s\S]*:show-column-reset="false"/,
  'eDHR batch execution page must hide the default list-template visible-field entry to avoid duplicate controls.'
)

assert.match(
  source,
  /@quick-filter-query="edhrBatchQuickFilter\.applyQuickFilter"/,
  'eDHR batch execution quick filter query behavior must remain wired through UnifiedListTemplate.'
)

assert.doesNotMatch(
  actionsSource,
  /@click="resetQuery"[\s\S]*重置/,
  'eDHR batch execution toolbar must remove the reset action from the highlighted toolbar.'
)

assert.match(
  actionsSource,
  /openCreateDialog[\s\S]*打开\/创建/,
  'eDHR batch execution toolbar must keep the create action.'
)

assert.doesNotMatch(
  actionsSource,
  /openReadinessDialog[\s\S]*演练预检/,
  'eDHR batch execution toolbar must remove the rehearsal precheck action from the highlighted toolbar.'
)

console.log('PASS: edhr batch display fields button static contract')
