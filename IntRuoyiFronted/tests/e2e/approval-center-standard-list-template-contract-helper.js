const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const approvalPage = readSource('src/views/approval-center/index.vue')
const routerModule = readSource('src/router/modules/remaining.ts')

const extractUnifiedTemplate = () => {
  const unifiedTemplateMatch = approvalPage.match(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/)
  assert.ok(unifiedTemplateMatch, 'approval center list must be rendered through UnifiedListTemplate')
  return unifiedTemplateMatch[0]
}

const assertApprovalCenterStandardListContract = ({
  routePath,
  routeName,
  routeTitle,
  viewType,
  tableKey,
  expectsReviewAction = false
}) => {
  const routeBlockMatch = routerModule.match(
    new RegExp(`path:\\s*'${escapeRegExp(routePath)}'[\\s\\S]*?permission:\\s*\\['bpm:task:query'\\]`)
  )
  assert.ok(routeBlockMatch, `approval center route ${routePath} must exist`)
  const routeBlock = routeBlockMatch[0]

  assert.match(routeBlock, new RegExp(`name:\\s*'${escapeRegExp(routeName)}'`), `${routeName} route name must be preserved`)
  assert.match(routeBlock, new RegExp(`title:\\s*'${escapeRegExp(routeTitle)}'`), `${routeName} route title must be preserved`)
  assert.match(
    routeBlock,
    /component:\s*\(\)\s*=>\s*import\('@\/views\/approval-center\/index\.vue'\)/,
    `${routeName} must keep using the approval center component`
  )

  const unifiedTemplate = extractUnifiedTemplate()

  for (const [pattern, description] of [
    [/import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/, 'UnifiedListTemplate import'],
    [/useUserTableColumns/, 'user column settings hook'],
    [/useTableQuickFilter/, 'quick filter hook'],
    [new RegExp(`${viewType}:\\s*'${escapeRegExp(tableKey)}'`), `${viewType} stable table key`],
    [/const approvalDefaultColumns: UserTableColumnDefinition\[\]/, 'approval default columns'],
    [/const approvalQuickFilterDefinitions = computed<TableQuickFilterDefinition\[\]>/, 'approval quick filter definitions'],
    [/queryParamKey:\s*'moduleCode'/, 'module quick filter query param'],
    [/queryParamKey:\s*'keyword'/, 'keyword quick filter query param']
  ]) {
    assert.match(approvalPage, pattern, `${description} must exist`)
  }

  for (const [pattern, description] of [
    [/:table-key="approvalCenterTableKey"/, 'stable table key binding'],
    [/:query-model="queryParams"/, 'query model binding'],
    [/:filter-definitions="approvalQuickFilterDefinitions"/, 'quick filter definitions binding'],
    [/:quick-filter-state="approvalQuickFilterState"/, 'quick filter state binding'],
    [/:selected-filter-definition="approvalSelectedFilterDefinition"/, 'selected quick filter binding'],
    [/:operator-options="approvalOperatorOptions"/, 'quick filter operators binding'],
    [/:columns="approvalColumns"/, 'user column settings binding'],
    [/:column-saving="approvalColumnSaving"/, 'column saving binding'],
    [/v-model:page="queryParams\.pageNo"/, 'page number pagination binding'],
    [/v-model:limit="queryParams\.pageSize"/, 'page size pagination binding'],
    [/@update:quick-filter-state="updateApprovalQuickFilterState"/, 'quick filter state update handler'],
    [/@quick-filter-query="applyApprovalQuickFilter"/, 'quick filter query handler'],
    [/@column-change="saveApprovalColumnConfig"/, 'column config save handler'],
    [/@column-reset="resetApprovalColumnConfig"/, 'column config reset handler'],
    [/@pagination="handlePagination"/, 'pagination reload handler']
  ]) {
    assert.match(unifiedTemplate, pattern, `${description} must be wired through UnifiedListTemplate`)
  }

  assert.doesNotMatch(
    unifiedTemplate,
    /:show-column-settings="false"/,
    'approval center must enable standard list column filtering'
  )
  assert.doesNotMatch(
    unifiedTemplate,
    /:show-column-reset="true"/,
    'approval center must keep the standard reset-column control hidden by default'
  )
  assert.match(
    unifiedTemplate,
    /query-form-test-id="approval-center-filter-form"/,
    'approval center must keep the search filter form visible through UnifiedListTemplate'
  )

  const tableSlotMatch = unifiedTemplate.match(/<template\s+#table\b[^>]*>[\s\S]*?<\/template>/)
  assert.ok(tableSlotMatch, 'approval center must provide its table through the standard list table slot')
  const tableSlot = tableSlotMatch[0]

  for (const [pattern, description] of [
    [/data-user-table-column-explicit/, 'explicit user columns marker'],
    [/:data-user-table-key="approvalCenterTableKey"/, 'table key marker'],
    [/@header-dragend="handleApprovalHeaderDragend"/, 'header drag width persistence'],
    [/:show-overflow-tooltip="true"/, 'dense overflow tooltip'],
    [/openModuleDetail\(row\)/, 'open detail action'],
    [/openTimeline\(row\)/, 'timeline action']
  ]) {
    assert.match(unifiedTemplate, pattern, `${description} must be preserved`)
  }

  if (expectsReviewAction) {
    assert.match(unifiedTemplate, /openReviewDialog\(row\)/, 'TODO list must preserve direct review action')
  }

  assert.match(
    approvalPage,
    /queryParams\.viewType === 'TODO'[\s\S]*actions\.includes\('APPROVE'\)[\s\S]*actions\.includes\('REJECT'\)/,
    'direct review must remain limited to TODO tasks with approve and reject capabilities'
  )

  for (const key of ['source', 'businessSummary', 'applicant', 'node', 'reviewer', 'capabilities', 'time', 'actions']) {
    assert.match(
      approvalPage,
      new RegExp(`isApprovalColumnVisible\\('${key}'\\)`),
      `approval column ${key} must be controlled by standard list column settings`
    )
  }

  assert.doesNotMatch(approvalPage, /<Pagination\b/, 'approval center page must use UnifiedListTemplate pagination only')
  assert.doesNotMatch(
    unifiedTemplate,
    /mock|fallback|降级|吞异常/i,
    'approval center standard list conversion must not introduce mock, fallback, downgrade, or swallowed-error behavior'
  )
}

module.exports = {
  assertApprovalCenterStandardListContract
}
