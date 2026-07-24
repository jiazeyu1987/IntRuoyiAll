const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const approvalPage = fs.readFileSync(path.join(repoRoot, 'src/views/approval-center/index.vue'), 'utf8')

const unifiedTemplateMatch = approvalPage.match(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/)
assert.ok(unifiedTemplateMatch, 'approval center list must stay inside UnifiedListTemplate')
const unifiedTemplate = unifiedTemplateMatch[0]

assert.doesNotMatch(
  unifiedTemplate,
  /<template\s+#extra-filters>/,
  'approval center must remove the duplicate right-side module and keyword filter block'
)
assert.doesNotMatch(
  unifiedTemplate,
  /<template\s+#actions>/,
  'approval center must remove the duplicate right-side query, reset, and reset column actions'
)
assert.doesNotMatch(
  unifiedTemplate,
  /:show-quick-filter="false"/,
  'approval center must not hide the standard quick-filter controls'
)
assert.doesNotMatch(
  unifiedTemplate,
  /:show-column-reset="true"/,
  'approval center must not opt back into the standard reset-column control by default'
)
assert.doesNotMatch(
  unifiedTemplate,
  /:show-column-settings="false"/,
  'approval center must keep the standard display-field control'
)
assert.match(
  unifiedTemplate,
  /data-user-table-column-explicit-scope/,
  'approval center table must opt out of global floating display-field controls at runtime'
)
assert.match(
  unifiedTemplate,
  /:filter-definitions="approvalQuickFilterDefinitions"[\s\S]*:quick-filter-state="approvalQuickFilterState"[\s\S]*:selected-filter-definition="approvalSelectedFilterDefinition"[\s\S]*:operator-options="approvalOperatorOptions"/,
  'approval center must render the standard quick-filter field, operator, value, and query controls'
)
assert.match(
  unifiedTemplate,
  /@quick-filter-query="applyApprovalQuickFilter"/,
  'approval center must preserve the top quick-filter query action'
)
assert.doesNotMatch(
  approvalPage,
  /<el-button[^>]*@click="refreshAll"[\s\S]*?刷新[\s\S]*?<\/el-button>/,
  'approval center must not show the top-right refresh button'
)
assert.match(approvalPage, /openReviewDialog\(row\)/, 'approval center must preserve row review action')
assert.match(approvalPage, /openModuleDetail\(row\)/, 'approval center must preserve row detail action')
assert.match(approvalPage, /openTimeline\(row\)/, 'approval center must preserve row timeline action')

const businessSummaryColumnMatch = unifiedTemplate.match(
  /<el-table-column[\s\S]*?prop="businessSummary"[\s\S]*?<\/el-table-column>/
)
assert.ok(businessSummaryColumnMatch, 'approval center business summary column must exist')
const businessSummaryColumn = businessSummaryColumnMatch[0]

assert.match(
  businessSummaryColumn,
  /:min-width="getApprovalColumnMinWidthString\('businessSummary', 300\)"/,
  'business summary column must use min-width so it can absorb the removed right-side blank area'
)
assert.doesNotMatch(
  businessSummaryColumn,
  /:width="getApprovalColumnWidthString\('businessSummary'/,
  'business summary column must not stay fixed-width after removing the right-side toolbar area'
)
assert.match(
  approvalPage,
  /\.approval-center__table\s*\{[\s\S]*?width:\s*100%;/,
  'approval center table must explicitly fill the removed red-box area'
)

console.log('PASS: approval center fill list area static contract')
