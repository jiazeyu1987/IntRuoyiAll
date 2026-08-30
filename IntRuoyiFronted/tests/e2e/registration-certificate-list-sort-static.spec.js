const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')

const readUtf8 = (...segments) => fs.readFileSync(path.resolve(...segments), 'utf8')

const pageSource = readUtf8(
  frontendRoot,
  'src/views/dcc/registration-certificate/index/index.vue'
)
const apiSource = readUtf8(frontendRoot, 'src/api/dcc/registrationCertificate/index.ts')
const reqVoSource = readUtf8(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/query/vo/DccRegistrationCertificatePageReqVO.java'
)
const querySource = readUtf8(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificatePageQuery.java'
)
const serviceSource = readUtf8(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateQueryServiceImpl.java'
)
const mapperSource = readUtf8(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/dal/mysql/DccRegistrationCertificateQueryMapper.java'
)

function requireIncludes(source, token, message) {
  assert.ok(source.includes(token), message)
}

const currentSortFields = [
  'certificateNo',
  'ownerCompanyName',
  'productName',
  'classification',
  'projectCode',
  'versionNo',
  'status',
  'hasProjectCode',
  'hasRegistrationFile',
  'approvalDate',
  'effectiveDate',
  'expiryDate',
  'reminder',
  'remark'
]

const oldSortFields = [
  'certificateNo',
  'ownerCompanyName',
  'productName',
  'classification',
  'versionNo',
  'status',
  'expiryDate'
]

const tableBlocks = Array.from(
  pageSource.matchAll(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/g),
  (match) => match[0]
)
assert.ok(tableBlocks.length >= 2, 'registration certificate page must render current and old list tables')
const currentTableBlock = tableBlocks[0]
const oldTableBlock = tableBlocks[1]

requireIncludes(currentTableBlock, 'v-model:sort-state="currentSortState"', 'current list must bind sort state')
requireIncludes(currentTableBlock, '@sort-change="handleCurrentSortChange"', 'current list must handle sort changes')
requireIncludes(oldTableBlock, 'v-model:sort-state="oldSortState"', 'old index must bind sort state')
requireIncludes(oldTableBlock, '@sort-change="handleOldSortChange"', 'old index must handle sort changes')
requireIncludes(
  currentTableBlock,
  '<template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">',
  'current table must receive UnifiedListTemplate sort helpers'
)
requireIncludes(
  oldTableBlock,
  '<template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">',
  'old table must receive UnifiedListTemplate sort helpers'
)
requireIncludes(
  currentTableBlock,
  '@sort-change="handleTemplateSortChange"',
  'current Element Plus table must delegate sort-change to UnifiedListTemplate'
)
requireIncludes(
  oldTableBlock,
  '@sort-change="handleTemplateSortChange"',
  'old Element Plus table must delegate sort-change to UnifiedListTemplate'
)

for (const field of currentSortFields) {
  requireIncludes(
    currentTableBlock,
    `v-bind="sortColumnAttrs('${field}')"`,
    `current list column ${field} must be registered as server-sortable`
  )
}

for (const field of oldSortFields) {
  requireIncludes(
    oldTableBlock,
    `v-bind="sortColumnAttrs('${field}')"`,
    `old index column ${field} must be registered as server-sortable`
  )
}

assert.match(
  currentTableBlock,
  /label="提醒状态"[\s\S]*prop="reminder"[\s\S]*v-bind="sortColumnAttrs\('reminder'\)"[\s\S]*?<template #default/,
  'reminder visual state must be registered as a formal current-list server sort field'
)
assert.ok(
  !currentTableBlock.includes('prop="visualState"'),
  'reminder visual state must not register an Element Plus sort prop'
)
assert.ok(
  !/label="提醒状态"[\s\S]*?:sortable="false"[\s\S]*?<template #default/.test(currentTableBlock),
  'reminder visual state must no longer be blocked from header sorting'
)
requireIncludes(pageSource, 'const CURRENT_SERVER_SORT_FIELDS', 'current sort field whitelist is required')
requireIncludes(pageSource, 'const OLD_SERVER_SORT_FIELDS', 'old sort field whitelist is required')
requireIncludes(pageSource, 'query.sortField = sortField', 'sort field must enter page query params')
requireIncludes(pageSource, 'query.sortOrder = order === \'ascending\' ? \'asc\' : \'desc\'', 'sort order must map to backend values')
requireIncludes(pageSource, "'reminder'", 'current list sort whitelist must include reminder')
requireIncludes(pageSource, "key: 'reminderState'", 'current quick filter must expose reminder state')
requireIncludes(pageSource, "queryParamKey: 'reminderState'", 'current quick filter must send reminderState')
requireIncludes(
  pageSource,
  'REGISTRATION_CERTIFICATE_REMINDER_FILTER_OPTIONS',
  'current quick filter must use explicit reminder-state options'
)

requireIncludes(apiSource, 'export type RegistrationCertificateSortField', 'frontend API must expose sort field contract')
requireIncludes(apiSource, "| 'reminder'", 'frontend API sort field contract must include reminder')
requireIncludes(
  apiSource,
  'export type DccRegistrationCertificateReminderFilterState',
  'frontend API must expose reminder filter-state contract'
)
requireIncludes(
  apiSource,
  'reminderState?: DccRegistrationCertificateReminderFilterState',
  'frontend page request must include reminderState'
)
requireIncludes(apiSource, 'sortField?: RegistrationCertificateSortField', 'frontend page request must include sortField')
requireIncludes(apiSource, "sortOrder?: 'asc' | 'desc'", 'frontend page request must include sortOrder')

requireIncludes(reqVoSource, 'private String reminderState;', 'backend request VO must accept reminderState')
requireIncludes(reqVoSource, '.reminderState(reminderState)', 'backend request VO must map reminderState into query')
requireIncludes(reqVoSource, 'private String sortField;', 'backend request VO must accept sortField')
requireIncludes(reqVoSource, 'private String sortOrder;', 'backend request VO must accept sortOrder')
requireIncludes(reqVoSource, '.sortField(sortField)', 'backend request VO must map sortField into query')
requireIncludes(reqVoSource, '.sortOrder(sortOrder)', 'backend request VO must map sortOrder into query')
requireIncludes(querySource, 'private String reminderState;', 'backend page query must carry reminderState')
requireIncludes(querySource, 'private String sortField;', 'backend page query must carry sortField')
requireIncludes(querySource, 'private String sortOrder;', 'backend page query must carry sortOrder')

requireIncludes(serviceSource, 'CURRENT_SORT_FIELDS', 'service must define current list sort whitelist')
requireIncludes(serviceSource, 'OLD_INDEX_SORT_FIELDS', 'service must define old index sort whitelist')
requireIncludes(serviceSource, 'validateSort(normalized, CURRENT_SORT_FIELDS)', 'current page must validate requested sort')
requireIncludes(serviceSource, 'validateSort(normalized, OLD_INDEX_SORT_FIELDS)', 'old index must validate requested sort')
requireIncludes(serviceSource, 'validateCurrentReminderState(normalized)', 'current page must validate reminderState')
requireIncludes(
  serviceSource,
  'validateOldIndexReminderState(normalized)',
  'old index must reject unsupported reminderState instead of ignoring it'
)
requireIncludes(
  serviceSource,
  'REGISTRATION_CERTIFICATE_REMINDER_STATE_INVALID',
  'invalid reminderState must fail fast'
)
requireIncludes(serviceSource, 'REGISTRATION_CERTIFICATE_SORT_INVALID', 'invalid sort must fail fast')

requireIncludes(mapperSource, 'currentOrderBy()', 'current page must use dynamic whitelisted order by')
requireIncludes(mapperSource, 'oldIndexOrderBy()', 'old index must use dynamic whitelisted order by')
requireIncludes(mapperSource, 'SORT_FIELD_CERTIFICATE_NO', 'mapper sort fields must be fixed constants')
requireIncludes(mapperSource, 'SORT_FIELD_REMINDER', 'mapper sort fields must include fixed reminder constant')
requireIncludes(mapperSource, 'REMINDER_STATE_EXPRESSION', 'mapper must use a fixed reminder-state expression')
requireIncludes(
  mapperSource,
  'query.reminderState == \'NORMAL\'',
  'mapper must support visible normal-state filtering as a formal grouped predicate'
)
assert.ok(!mapperSource.includes('${sortField}'), 'mapper must not splice sortField into SQL')
assert.ok(!mapperSource.includes('${sortOrder}'), 'mapper must not splice sortOrder into SQL')
assert.ok(!mapperSource.includes('${reminderState}'), 'mapper must not splice reminderState into SQL')
