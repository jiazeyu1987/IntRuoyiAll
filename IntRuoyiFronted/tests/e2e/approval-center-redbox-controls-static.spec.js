const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const approvalPage = fs.readFileSync(path.join(repoRoot, 'src/views/approval-center/index.vue'), 'utf8')
const unifiedTemplateComponent = fs.readFileSync(
  path.join(repoRoot, 'src/components/UnifiedListTemplate/index.vue'),
  'utf8'
)

const unifiedTemplateMatch = approvalPage.match(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/)
assert.ok(unifiedTemplateMatch, 'approval center list must stay inside UnifiedListTemplate')
const unifiedTemplate = unifiedTemplateMatch[0]

assert.doesNotMatch(
  unifiedTemplate,
  /<template\s+#extra-filters>/,
  'approval center must not render the red-box module and keyword filter block'
)
assert.doesNotMatch(
  unifiedTemplate,
  /<template\s+#actions>/,
  'approval center must not render the red-box query and reset button block'
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
  /:filter-definitions="approvalQuickFilterDefinitions"[\s\S]*:quick-filter-state="approvalQuickFilterState"[\s\S]*:selected-filter-definition="approvalSelectedFilterDefinition"[\s\S]*:operator-options="approvalOperatorOptions"/,
  'approval center must render the standard quick-filter field, operator, value, and query controls'
)
assert.match(
  unifiedTemplate,
  /@quick-filter-query="applyApprovalQuickFilter"/,
  'approval center must preserve quick filter query behavior'
)
assert.match(
  unifiedTemplateComponent,
  /withDefaults\(defineProps[\s\S]*showQuickFilter:\s*true/,
  'UnifiedListTemplate must default quick filter visibility to true when pages omit showQuickFilter'
)
assert.doesNotMatch(
  unifiedTemplateComponent,
  /ep:refresh|刷新|refreshAll/,
  'UnifiedListTemplate must not provide the approval-center refresh button'
)
assert.doesNotMatch(
  approvalPage,
  /<el-button[^>]*@click="refreshAll"[\s\S]*?刷新[\s\S]*?<\/el-button>/,
  'approval center tabs must not show the top-right refresh button'
)
assert.match(approvalPage, /openReviewDialog\(row\)/, 'approval center must preserve row review action')
assert.match(approvalPage, /openModuleDetail\(row\)/, 'approval center must preserve row detail action')
assert.match(approvalPage, /openTimeline\(row\)/, 'approval center must preserve row timeline action')

console.log('PASS: approval center red-box controls static contract')
