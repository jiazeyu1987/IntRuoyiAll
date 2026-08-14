const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const backendRoot = path.resolve(repoRoot, '..', 'IntRuoyiBackend')

const readSource = (relativePath, root = repoRoot) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertContains = (source, pattern, message) => {
  assert.match(source, pattern, message)
}

const componentSource = readSource('src/components/TableQuickFilter/index.vue')
const hookSource = readSource('src/hooks/web/useTableQuickFilter.ts')

assertContains(componentSource, /快速过滤/, 'component must render quick filter label')
assertContains(componentSource, /showLabel/, 'component must allow page-level hiding of quick filter label')
assertContains(componentSource, /字段/, 'component must render field selector')
assertContains(componentSource, /条件/, 'component must render operator selector')
assertContains(componentSource, /查询/, 'component must render query action')
assertContains(componentSource, /包含/, 'component must render contains operator label')
assertContains(componentSource, /等于/, 'component must render equals operator label')
assertContains(componentSource, /介于/, 'component must render between operator label')
assertContains(
  componentSource,
  /selectedDefinition\?\.type === 'date'[\s\S]*type="date"[\s\S]*value-format="YYYY-MM-DD"/,
  'component must render single-date control'
)
assertContains(componentSource, /el-date-picker[\s\S]*daterange/, 'component must render date range control')
assertContains(componentSource, /table-quick-filter__value--single-date/, 'component must size single-date control')
assertContains(componentSource, /el-autocomplete/, 'component must render autocomplete control')
assertContains(
  componentSource,
  /v-else-if="selectedDefinition\?\.type === 'select'"[\s\S]*@update:model-value="updateValue"/,
  'select quick-filter value changes must only update state in the generic component'
)
assert.doesNotMatch(
  componentSource,
  /updateSelectValue/,
  'generic quick filter must not own select auto-search behavior'
)
assertContains(componentSource, /ElMessage\.error|ElMessage\.warning/, 'component must surface validation errors visibly')
assertContains(componentSource, /update:state/, 'component must emit state updates instead of mutating props')
assert.doesNotMatch(componentSource, /v-model="state\./, 'component must not mutate state prop through v-model')
assert.doesNotMatch(componentSource, /props\.state\.[A-Za-z0-9_]+\s*=/, 'component must not assign to state prop directly')
assert.doesNotMatch(componentSource, /localStorage|sessionStorage/, 'component must not use browser storage fallback')

assertContains(hookSource, /export const useTableQuickFilter/, 'hook must export useTableQuickFilter')
assertContains(hookSource, /TableQuickFilterDefinition/, 'hook must define filter definition type')
assertContains(hookSource, /text/, 'hook must support text fields')
assertContains(hookSource, /select/, 'hook must support select fields')
assertContains(hookSource, /date:\s*\['eq'\]/, 'hook must support single-date fields')
assertContains(hookSource, /dateRange/, 'hook must support date range fields')
assertContains(hookSource, /autocomplete/, 'hook must support autocomplete fields')
assertContains(hookSource, /quickFilter/, 'hook must write quickFilter into query params')
assertContains(hookSource, /contains/, 'hook must support contains operator')
assertContains(hookSource, /eq/, 'hook must support eq operator')
assertContains(hookSource, /between/, 'hook must support between operator')
assertContains(hookSource, /ElMessage/, 'hook must surface validation or backend-facing errors')
assert.doesNotMatch(hookSource, /localStorage|sessionStorage/, 'hook must not persist quick filters locally')
assertContains(
  hookSource,
  /const isQuickFilterInputEmpty = \(\) => \{[\s\S]*definition\.type === 'dateRange'[\s\S]*isEmptyQuickFilterValue\(range\[0\]\)[\s\S]*isEmptyQuickFilterValue\(range\[1\]\)[\s\S]*isEmptyQuickFilterValue\(state\.value\)/,
  'empty quick-filter input must be detected before validation'
)
assertContains(
  hookSource,
  /const clearQuickFilterParams = \(\) => \{[\s\S]*delete queryParamTarget\.quickFilter[\s\S]*definitions\.value\.forEach[\s\S]*delete queryParamTarget\[definition\.queryParamKey\]/,
  'reset must clear both quickFilter and queryParamKey mapped quick filter params'
)
assertContains(
  hookSource,
  /const resetQuickFilter = async \(\) => \{[\s\S]*clearQuickFilterParams\(\)[\s\S]*queryParams\.pageNo = 1[\s\S]*await reload\(\)/,
  'reset command must clear filters, reset page, and reload'
)
assertContains(
  hookSource,
  /const applyQuickFilter = async \(\) => \{[\s\S]*if \(isQuickFilterInputEmpty\(\)\) \{[\s\S]*await resetQuickFilter\(\)[\s\S]*return[\s\S]*\}[\s\S]*if \(!validate\(\)\) return/,
  'empty quick-filter search must execute reset instead of showing the missing value warning'
)

const representativePages = [
  {
    file: 'src/views/mes/pro/workorder/index.vue',
    tableKey: 'mes.pro.workorder.main',
    requiredFields: ['code', 'productName', 'productCode', 'productSpecification', 'requestDate']
  },
  {
    file: 'src/views/mes/pro/scheduleorder/index.vue',
    tableKey: 'mes.pro.scheduleOrder.main',
    requiredFields: ['code', 'erpWorkOrderCode', 'productName', 'productSpecification', 'promiseDate']
  },
  {
    file: 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
    tableKey: 'mes.pro.edhrBatch.execution.main',
    requiredFields: ['batchExecutionCode', 'workOrderCode', 'batchCode', 'product', 'status', 'createTime']
  },
  {
    file: 'src/views/dcc/controlled-file/browser/index.vue',
    tableKey: 'dcc.controlledFile.browser.main',
    requiredFields: ['keyword', 'fileName', 'fileNumber', 'status', 'categoryId']
  },
  {
    file: 'src/views/dcc/controlled-file/signatures/index.vue',
    tableKey: 'dcc.electronicSignature.records',
    requiredFields: ['fileNumber', 'versionNo', 'signerUserId', 'taskActionResult', 'meaningCode']
  }
]

for (const page of representativePages) {
  const source = readSource(page.file)
  assertContains(source, /UnifiedListTemplate/, `${page.file} must render the standard list template`)
  assertContains(source, /:quick-filter-state=/, `${page.file} must bind quick filter state to UnifiedListTemplate`)
  assertContains(source, /@update:quick-filter-state=/, `${page.file} must receive condition Tab state updates from UnifiedListTemplate`)
  assertContains(source, /@quick-filter-query=/, `${page.file} must query through the standard condition Tab bridge`)
  assertContains(source, /useTableQuickFilter/, `${page.file} must use unified quick filter hook`)
  assertContains(source, new RegExp(page.tableKey.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${page.file} must keep stable table key`)
  assertContains(source, /quickFilterDefinitions|QuickFilterDefinition/, `${page.file} must declare filter definitions`)
  for (const field of page.requiredFields) {
    assertContains(source, new RegExp(`key:\\s*'${field}'`), `${page.file} must declare quick filter field ${field}`)
  }
}

const apiFiles = [
  'src/api/mes/pro/workorder/index.ts',
  'src/api/mes/pro/scheduleorder/index.ts',
  'src/api/mes/pro/edhr/batchExecution.ts',
  'src/api/dcc/controlledFile/workflow.ts',
  'src/api/dcc/controlledFile/signatures.ts'
]

for (const apiFile of apiFiles) {
  const source = readSource(apiFile)
  assertContains(source, /quickFilter\??:/, `${apiFile} must expose quickFilter request parameter`)
}

const backendFiles = [
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/workorder/vo/MesProWorkOrderPageReqVO.java',
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/scheduleorder/vo/MesProScheduleOrderPageReqVO.java',
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionPageReqVO.java',
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFilePageReqVO.java',
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/vo/DccElectronicSignaturePageReqVO.java'
]

for (const backendFile of backendFiles) {
  const source = readSource(backendFile, backendRoot)
  assertContains(source, /QuickFilter/, `${backendFile} must include quick filter contract`)
}

const backendQuickFilterSource = readSource(
  'yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/pojo/QuickFilter.java',
  backendRoot
)
assertContains(backendQuickFilterSource, /fieldKey/, 'backend quick filter must include fieldKey')
assertContains(backendQuickFilterSource, /operator/, 'backend quick filter must include operator')
assertContains(backendQuickFilterSource, /valueEnd/, 'backend quick filter must include valueEnd for date ranges')

const backendSupportSource = readSource(
  'yudao-framework/yudao-spring-boot-starter-mybatis/src/main/java/cn/iocoder/yudao/framework/mybatis/core/query/QuickFilterUtils.java',
  backendRoot
)
assertContains(backendSupportSource, /contains/, 'backend support must whitelist contains operator')
assertContains(backendSupportSource, /between/, 'backend support must whitelist between operator')
assertContains(backendSupportSource, /fieldKey/, 'backend support must validate field keys')
assertContains(backendSupportSource, /throw exception|ServiceException|IllegalArgumentException/, 'backend support must fail fast on invalid filters')
assert.doesNotMatch(backendSupportSource, /last\(|apply\(|\\$\\{/, 'backend support must not inject raw SQL field names')

console.log('PASS: table quick filter static contract')
