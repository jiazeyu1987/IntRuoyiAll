const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const approvalCenterSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/approval-center/index.vue'),
  'utf8'
)

const syncFunctionMatch = approvalCenterSource.match(
  /const\s+syncApprovalQuickFilterStateFromQuery\s*=\s*async\s*\(\)\s*=>\s*\{([\s\S]*?)\n\}/
)
assert.ok(
  syncFunctionMatch,
  'approval center must sync route module/keyword query back into the visible quick-filter controls'
)

const syncFunction = syncFunctionMatch[1]
assert.match(
  syncFunction,
  /queryParams\.moduleCode[\s\S]*fieldKey:\s*'moduleCode'[\s\S]*operator:\s*'eq'[\s\S]*value:\s*queryParams\.moduleCode/,
  'moduleCode route query must be displayed as the module quick-filter value'
)
assert.match(
  syncFunction,
  /queryParams\.keyword\.trim\(\)[\s\S]*fieldKey:\s*'keyword'[\s\S]*operator:\s*'contains'[\s\S]*value:\s*queryParams\.keyword\.trim\(\)/,
  'keyword route query must be displayed as the keyword quick-filter value'
)
assert.match(
  syncFunction,
  /await\s+nextTick\(\)/,
  'quick-filter state sync must wait for field changes before applying operator and value'
)

const refreshAllMatch = approvalCenterSource.match(/const\s+refreshAll\s*=\s*async\s*\(\)\s*=>\s*\{([\s\S]*?)\n\}/)
assert.ok(refreshAllMatch, 'approval center must keep refreshAll')
assert.match(
  refreshAllMatch[1],
  /await\s+loadModules\(\)[\s\S]*await\s+syncApprovalQuickFilterStateFromQuery\(\)[\s\S]*await\s+getList\(\)/,
  'initial load must fetch module descriptors, sync visible route filters, then request the list'
)

const routeLoadMatch = approvalCenterSource.match(
  /const\s+applyRouteQueryAndLoad\s*=\s*async\s*\(\)\s*=>\s*\{([\s\S]*?)\n\}/
)
assert.ok(routeLoadMatch, 'approval center must keep applyRouteQueryAndLoad')
assert.match(
  routeLoadMatch[1],
  /applyRouteQuery\(\)[\s\S]*syncRouteToCanonicalPath\(queryParams\.viewType\)[\s\S]*await\s+syncApprovalQuickFilterStateFromQuery\(\)[\s\S]*await\s+getList\(\)/,
  'route changes must sync visible quick-filter state before reloading the list'
)

assert.match(
  approvalCenterSource,
  /catch\s*\(error\)\s*\{[\s\S]*ElMessage\.error\(message\)[\s\S]*throw\s+error[\s\S]*\}/,
  'module descriptor loading errors must stay visible and must not be overwritten by a follow-up empty list request'
)

console.log('PASS: approval center route filters are visible in quick-filter controls')
