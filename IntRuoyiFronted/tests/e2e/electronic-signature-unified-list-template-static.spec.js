const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const unifiedTemplate = read('src/components/UnifiedListTemplate/index.vue')
const dccSignaturePage = read('src/views/dcc/controlled-file/signatures/index.vue')
const edhrSignaturePage = read('src/views/mes/pro/edhr/SignaturePage.vue')
const governancePage = read('src/views/signature-governance/index.vue')
const governanceBlockerList = read(
  'src/views/signature-governance/components/SignatureGovernanceBlockerList.vue'
)
const governancePolicyModuleList = read(
  'src/views/signature-governance/components/SignatureGovernancePolicyModuleList.vue'
)

const expectUnifiedList = (source, tableKey, label) => {
  assert.match(
    source,
    new RegExp(`<UnifiedListTemplate[\\s\\S]*table-key="${tableKey.replace(/\./g, '\\.')}"`),
    `${label} must use UnifiedListTemplate with stable table key`
  )
  assert.match(
    source,
    new RegExp(`data-user-table-key="${tableKey.replace(/\./g, '\\.')}"`),
    `${label} table must expose data-user-table-key`
  )
}

assert.match(
  unifiedTemplate,
  /showQuickFilter\?: boolean/,
  'UnifiedListTemplate must support explicit quick-filter visibility when a paged API has no filter contract'
)
assert.match(
  unifiedTemplate,
  /v-if="showQuickFilter !== false" class="unified-list-template__quick-filter"/,
  'UnifiedListTemplate must hide quick filter without hiding column settings'
)

for (const requiredImport of [
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  /useUserTableColumns, type UserTableColumnDefinition/,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition,[\s\S]*type TableQuickFilterValue/
]) {
  assert.match(dccSignaturePage, requiredImport, 'DCC signature page must import standard list dependencies')
  assert.match(edhrSignaturePage, requiredImport, 'eDHR signature page must import standard list dependencies')
}

for (const tableKey of [
  'dcc.electronicSignature.records',
  'dcc.electronicSignature.authorizations',
  'dcc.electronicSignature.authorizationAudit'
]) {
  expectUnifiedList(dccSignaturePage, tableKey, tableKey)
  assert.match(
    dccSignaturePage,
    new RegExp(`useUserTableColumns\\(\\s*'${tableKey.replace(/\./g, '\\.')}'`),
    `${tableKey} must persist visible columns and column widths`
  )
}

assert.match(
  dccSignaturePage,
  /useTableQuickFilter\(\s*'dcc\.electronicSignature\.records'[\s\S]*recordQuickFilterDefinitions[\s\S]*recordQueryParams[\s\S]*loadRecordPage/,
  'DCC signature records must connect quick filter to the original page loader'
)
assert.match(
  dccSignaturePage,
  /useTableQuickFilter\(\s*'dcc\.electronicSignature\.authorizations'[\s\S]*authorizationQuickFilterDefinitions[\s\S]*authorizationQueryParams[\s\S]*loadAuthorizationPage/,
  'DCC signature authorizations must connect quick filter to the original page loader'
)
assert.doesNotMatch(
  dccSignaturePage,
  /<template #actions>|<template #extra-filters>|handleRecordQuery|resetRecordQuery|handleAuthorizationQuery|resetAuthorizationQuery/,
  'DCC signature page must remove independent query/reset controls outside the user red box'
)
assert.doesNotMatch(
  edhrSignaturePage,
  /<template #actions>|<template #extra-filters>|handleQuery|resetQuery/,
  'eDHR signature history must remove independent query/reset controls outside the user red box'
)
assert.doesNotMatch(
  dccSignaturePage,
  /@keyup\.enter="handleRecordQuery"|@keyup\.enter="handleAuthorizationQuery"/,
  'DCC signature page must not keep Enter handlers for removed independent filters'
)
assert.doesNotMatch(
  edhrSignaturePage,
  /@keyup\.enter="handleQuery"/,
  'eDHR signature history must not keep Enter handlers for removed independent filters'
)
assert.match(
  dccSignaturePage,
  /@quick-filter-query="recordQuickFilter\.applyQuickFilter"[\s\S]*@quick-filter-query="authorizationQuickFilter\.applyQuickFilter"/,
  'DCC signature lists must keep quick-filter query events wired'
)
assert.match(
  edhrSignaturePage,
  /@quick-filter-query="signatureQuickFilter\.applyQuickFilter"/,
  'eDHR signature history must keep quick-filter query events wired'
)
assert.match(
  unifiedTemplate,
  /@query="\$emit\('quick-filter-query'\)"/,
  'UnifiedListTemplate must relay quick-filter button and Enter queries'
)
assert.match(
  unifiedTemplate,
  /emit\('quick-filter-query'\)/,
  'UnifiedListTemplate must still trigger quick-filter auto queries after select changes'
)
assert.match(
  dccSignaturePage,
  /table-key="dcc\.electronicSignature\.authorizationAudit"[\s\S]*:show-quick-filter="false"/,
  'Authorization audit must not expose fake client-side quick filtering for a server-paged audit API'
)
assert.match(
  dccSignaturePage,
  /emptyQuickFilterDefinitions/,
  'Authorization audit must pass an explicit empty quick-filter contract'
)
assert.doesNotMatch(
  dccSignaturePage,
  /authorizationAuditDisplayList|authorizationAuditTemplateTotal|normalizeAuthorizationAuditFilterText/,
  'Authorization audit must not filter only the current server page'
)
assert.doesNotMatch(
  dccSignaturePage,
  /<Pagination[\s\S]*loadAuthorizationAuditPage/,
  'Authorization audit pagination must be owned by UnifiedListTemplate'
)
assert.match(
  dccSignaturePage,
  /recordQuickFilterDefinitions = computed<TableQuickFilterDefinition\[\]>\(\(\) => \[[\s\S]*\.\.\.\(isAdvancedSignatureView\.value[\s\S]*evidenceHashShort/,
  'DCC evidence hash quick filter must stay gated by advanced view'
)
assert.match(
  dccSignaturePage,
  /evidenceHashShort: isAdvancedSignatureView\.value[\s\S]*recordQueryParams\.evidenceHashShort\?\.trim\(\)/,
  'DCC evidence hash query parameter must stay gated by advanced view'
)

expectUnifiedList(edhrSignaturePage, 'mes.pro.edhr.signature.history', 'eDHR signature history')
assert.match(
  edhrSignaturePage,
  /useTableQuickFilter\(\s*'mes\.pro\.edhr\.signature\.history'[\s\S]*signatureQuickFilterDefinitions[\s\S]*queryParams[\s\S]*getList/,
  'eDHR signature history must connect quick filter to the original page loader'
)
for (const optionalTrim of [
  'executionCode',
  'actorName',
  'processInstanceId',
  'bpmTaskId'
]) {
  assert.match(
    edhrSignaturePage,
    new RegExp(`${optionalTrim}: queryParams\\.${optionalTrim}\\?\\.trim\\(\\) \\|\\| undefined`),
    `${optionalTrim} must tolerate quick-filter reset deleting the query field`
  )
}

assert.doesNotMatch(
  governancePage,
  /<el-table/,
  'Signature governance page must delegate tab lists to standard-list components'
)
for (const tableKey of [
  'signature.governance.retention.blockers',
  'signature.governance.periodicReview.blockers',
  'signature.governance.csvPackage.blockers',
  'signature.governance.policy.modules',
  'signature.governance.policy.blockers'
]) {
  assert.match(
    governancePage,
    new RegExp(`table-key="${tableKey.replace(/\./g, '\\.')}"`),
    `${tableKey} must be represented on the governance page`
  )
}

for (const [componentSource, componentName] of [
  [governanceBlockerList, 'SignatureGovernanceBlockerList'],
  [governancePolicyModuleList, 'SignatureGovernancePolicyModuleList']
]) {
  assert.match(componentSource, /<UnifiedListTemplate/, `${componentName} must wrap its table`)
  assert.match(componentSource, /useUserTableColumns\(props\.tableKey/, `${componentName} must persist columns`)
  assert.match(componentSource, /useTableQuickFilter\(\s*props\.tableKey/, `${componentName} must use quick filter`)
  assert.match(componentSource, /@header-dragend="/, `${componentName} must persist dragged widths`)
  assert.match(componentSource, /v-model:page="queryParams\.pageNo"/, `${componentName} must use template pagination`)
}

console.log('PASS: electronic signature unified list template static contract')
