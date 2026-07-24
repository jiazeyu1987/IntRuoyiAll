const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertContains = (source, pattern, message) => {
  assert.match(source, pattern, message)
}

const apiSource = readSource('src/api/system/userTableColumnConfig/index.ts')
const hookSource = readSource('src/hooks/web/useUserTableColumns.ts')
const settingsSource = readSource('src/components/UserTableColumnSettings/index.vue')
const globalEnhancerSource = readSource('src/components/UserTableColumnGlobalEnhancer/index.vue')
const appSource = readSource('src/App.vue')

assertContains(
  apiSource,
  /\/system\/user-table-column-config\/get/,
  'frontend API must call backend get endpoint'
)
assertContains(
  apiSource,
  /\/system\/user-table-column-config\/save/,
  'frontend API must call backend save endpoint'
)
assertContains(
  apiSource,
  /\/system\/user-table-column-config\/reset/,
  'frontend API must call backend reset endpoint'
)

assertContains(
  hookSource,
  /export const useUserTableColumns/,
  'hook must export useUserTableColumns'
)
assertContains(
  hookSource,
  /header-dragend/,
  'hook must document or expose Element Plus header-dragend handling'
)
assertContains(
  hookSource,
  /ElMessage\.error/,
  'hook must surface load/save/reset failures visibly'
)
assert.doesNotMatch(
  hookSource,
  /localStorage|sessionStorage/,
  'hook must not use browser storage fallback'
)

assertContains(settingsSource, /显示字段/, 'settings component must expose visible-field UI')
assertContains(settingsSource, /至少保留 1 个业务字段/, 'settings component must enforce one visible business column')
assert.doesNotMatch(settingsSource, /\$emit\('save'\)|@click="\$emit\('save'\)"|>\s*保存\s*</, 'settings component must not render manual save action')
assertContains(settingsSource, /emit\('change', props\.columns\)/, 'settings component must emit change for autosave')
assertContains(settingsSource, /重置列/, 'settings component must render explicit column reset action')
assertContains(settingsSource, /showReset/, 'settings component must allow page-level hiding of reset action')
assertContains(hookSource, /autoSaveConfig/, 'hook must expose autosave helper')
assertContains(hookSource, /await autoSaveConfig\(\)/, 'header dragend must autosave after column width changes')

assertContains(
  appSource,
  /UserTableColumnGlobalEnhancer/,
  'root app must mount global table column enhancer'
)
assertContains(
  globalEnhancerSource,
  /TARGET_ROUTE_PREFIXES/,
  'global enhancer must scope itself by route prefixes'
)
for (const requiredPrefix of [
  '/mes/pro/workorder',
  '/mes/pro/work-order',
  '/mes/pro/scheduleorder',
  '/mes/pro/schedule-order',
  '/mes/pro/scheduler-workbench',
  '/mes/pro/task',
  '/mes/pro/edhr',
  '/mes/pro/feedback/edhr',
  '/mes/pro/edhr-batch',
  '/mes/pro/edhr-work-task',
  '/mes/pro/edhr-release',
  '/mes/pro/edhr-traveler',
  '/mes/pro/edhr-validation',
  '/dcc/controlled-file',
  '/approval-center'
]) {
  assertContains(
    globalEnhancerSource,
    new RegExp(requiredPrefix.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `global enhancer must include target route prefix ${requiredPrefix}`
  )
}
assertContains(
  globalEnhancerSource,
  /data-user-table-column-global-enhancer/,
  'global enhancer must render a detectable settings surface'
)
assert.doesNotMatch(
  globalEnhancerSource,
  />\s*保存\s*<\/button>/,
  'global enhancer must not render a manual save button next to visible-field control'
)
assertContains(
  globalEnhancerSource,
  /await savePanel\(tableKey/,
  'global enhancer must autosave when visible columns change'
)
assertContains(
  globalEnhancerSource,
  /dragWidthSnapshot/,
  'global enhancer must record a pre-drag width snapshot for managed tables'
)
assertContains(
  globalEnhancerSource,
  /handleManagedHeaderPointerDown/,
  'global enhancer must detect user-initiated header resize gestures'
)
assertContains(
  globalEnhancerSource,
  /finalizeManagedColumnResize/,
  'global enhancer must finalize managed table column width persistence after drag end'
)
assertContains(
  globalEnhancerSource,
  /window\.addEventListener\('mouseup', finalizeManagedColumnResize/,
  'global enhancer must persist width after mouse-based header drag release'
)
assertContains(
  globalEnhancerSource,
  /window\.addEventListener\('pointerup', finalizeManagedColumnResize/,
  'global enhancer must persist width after pointer-based header drag release'
)
assertContains(
  globalEnhancerSource,
  /Math\.abs\(nextWidth - previousWidth\) > 1/,
  'global enhancer must save only when column width actually changes'
)
assertContains(
  globalEnhancerSource,
  /saveUserTableColumnConfig/,
  'global enhancer must save through backend persistence API'
)
assertContains(
  globalEnhancerSource,
  /resetUserTableColumnConfig/,
  'global enhancer must reset through backend persistence API'
)
assertContains(
  globalEnhancerSource,
  /getUserTableColumnConfig/,
  'global enhancer must load through backend persistence API'
)
assertContains(
  globalEnhancerSource,
  /ElMessage\.error/,
  'global enhancer must surface backend failures visibly'
)
assert.doesNotMatch(
  globalEnhancerSource,
  /localStorage|sessionStorage/,
  'global enhancer must not use browser storage fallback'
)

const representativePages = [
  {
    file: 'src/views/mes/pro/workorder/index.vue',
    tableKey: 'mes.pro.workorder.main',
    requiredColumns: ['code', 'productCode', 'productName', 'productSpecification']
  },
  {
    file: 'src/views/mes/pro/scheduleorder/index.vue',
    tableKey: 'mes.pro.scheduleOrder.main',
    requiredColumns: ['erpWorkOrderCode', 'productCode', 'productName', 'productSpecification']
  },
  {
    file: 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
    tableKey: 'mes.pro.edhrBatch.execution.main',
    requiredColumns: ['batchExecutionCode', 'workOrderCode', 'currentProcess', 'product']
  },
  {
    file: 'src/views/dcc/controlled-file/browser/index.vue',
    tableKey: 'dcc.controlledFile.browser.adminStyle',
    requiredColumns: ['fileName', 'fileNumber', 'directory', 'productName']
  },
  {
    file: 'src/views/dcc/controlled-file/detail/index.vue',
    tableKey: 'dcc.controlledFile.detail.signatureEvidence',
    requiredColumns: ['versionNo', 'signer', 'departmentPost', 'role']
  }
]

for (const page of representativePages) {
  const source = readSource(page.file)
  assertContains(source, new RegExp(page.tableKey.replaceAll('.', '\\.')), `${page.file} must declare ${page.tableKey}`)
  if (
    [
      'mes.pro.scheduleOrder.main',
      'mes.pro.workorder.main',
      'mes.pro.edhrBatch.execution.main',
      'dcc.controlledFile.browser.adminStyle'
    ].includes(page.tableKey)
  ) {
    assertContains(source, /UnifiedListTemplate/, `${page.file} must render UserTableColumnSettings through UnifiedListTemplate`)
    assertContains(source, /@column-change="save/, `${page.file} must autosave column visibility changes through UnifiedListTemplate`)
  } else {
    assertContains(source, /UserTableColumnSettings/, `${page.file} must render UserTableColumnSettings`)
    assertContains(source, /@change="save/, `${page.file} must autosave column visibility changes`)
  }
  assert.doesNotMatch(source, /@save="save.*ColumnConfig"/, `${page.file} must not rely on manual save for column settings`)
  assertContains(source, /useUserTableColumns/, `${page.file} must use unified column hook`)
  assertContains(source, /@header-dragend/, `${page.file} must listen to header-dragend`)
  assertContains(source, /data-user-table-column-explicit/, `${page.file} must mark explicitly configured table to prevent duplicate enhancement`)
  assertContains(source, /data-user-table-key/, `${page.file} must declare table key on explicitly configured table`)
  for (const columnKey of page.requiredColumns) {
    assertContains(source, new RegExp(`key:\\s*'${columnKey}'`), `${page.file} must declare stable column key ${columnKey}`)
  }
}

const targetTableRoots = [
  'src/views/mes/pro/workorder',
  'src/views/mes/pro/scheduleorder',
  'src/views/mes/pro/scheduler-workbench',
  'src/views/mes/pro/task',
  'src/views/mes/pro/edhr-batch',
  'src/views/mes/pro/edhr',
  'src/views/mes/pro/edhr-work-task',
  'src/views/mes/pro/edhr-release',
  'src/views/mes/pro/edhr-traveler',
  'src/views/mes/pro/edhr-validation',
  'src/views/dcc/controlled-file',
  'src/views/approval-center'
]

const countActualElTables = (source) => (source.match(/<el-table(?:\s|>)/g) || []).length
const walkVueFiles = (absoluteDir) => {
  const files = []
  if (!fs.existsSync(absoluteDir)) return files
  for (const entry of fs.readdirSync(absoluteDir, { withFileTypes: true })) {
    const absolutePath = path.join(absoluteDir, entry.name)
    if (entry.isDirectory()) {
      files.push(...walkVueFiles(absolutePath))
    } else if (entry.isFile() && entry.name.endsWith('.vue')) {
      files.push(absolutePath)
    }
  }
  return files
}

const allTargetTableFiles = []
for (const root of targetTableRoots) {
  const absoluteRoot = path.join(repoRoot, root)
  for (const file of walkVueFiles(absoluteRoot)) {
    const relativeFile = path.relative(repoRoot, file).replaceAll(path.sep, '/')
    const source = fs.readFileSync(file, 'utf8')
    const tableCount = countActualElTables(source)
    if (tableCount > 0) {
      allTargetTableFiles.push({ file: relativeFile, source, tableCount })
    }
  }
}

assert.ok(allTargetTableFiles.length > 0, 'target modules must contain table files to cover')
const explicitTableCount = allTargetTableFiles.reduce(
  (sum, item) => sum + (item.source.match(/data-user-table-column-explicit/g) || []).length,
  0
)
const totalTargetTables = allTargetTableFiles.reduce((sum, item) => sum + item.tableCount, 0)
assert.ok(totalTargetTables >= 100, `target module table inventory unexpectedly low: ${totalTargetTables}`)
assert.ok(explicitTableCount >= representativePages.length, 'representative explicit tables must stay marked')

for (const item of allTargetTableFiles) {
  const usesExplicitHook = /useUserTableColumns/.test(item.source)
  const hasExplicitMarker = /data-user-table-column-explicit/.test(item.source)
  assert.ok(
    usesExplicitHook ? hasExplicitMarker : true,
    `${item.file} uses explicit hook but is missing duplicate-prevention marker`
  )
  if (usesExplicitHook) {
    assert.ok(
      /@header-dragend/.test(item.source),
      `${item.file} uses explicit hook but is missing column width drag persistence`
    )
  }
}

console.log('PASS: user table column config static contract')
