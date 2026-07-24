const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

const traceDrawerMatch = batchDetail.match(
  /<el-drawer v-model="traceRecordDrawerVisible" title="追溯记录"[\s\S]*?<\/el-drawer>/
)
assert.ok(traceDrawerMatch, '批次详情必须保留追溯记录弹框。')

const traceDrawer = traceDrawerMatch[0]
const expectedTabs = [
  {
    label: '放行事件',
    name: 'release',
    component: 'ReleaseEventListPane',
    importPath: '@/views/mes/pro/edhr/components/ReleaseEventListPane.vue',
    tableKey: 'mes.pro.edhr.traceDrawer.releaseEvents'
  },
  {
    label: '变更记录',
    name: 'change',
    component: 'FormTraceChangeTab',
    importPath: '@/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue',
    tableKey: 'mes.pro.edhr.formTrace.change'
  },
  {
    label: '操作审计',
    name: 'audit',
    component: 'OperationAuditListPane',
    importPath: '@/views/mes/pro/edhr/components/OperationAuditListPane.vue',
    tableKey: 'mes.pro.edhr.operationAudit'
  },
  {
    label: '域追溯',
    name: 'domain',
    component: 'DomainTraceListPane',
    importPath: '@/views/mes/pro/edhr/components/DomainTraceListPane.vue',
    tableKey: 'mes.pro.edhr.traceDrawer.domainTrace'
  }
]

for (const tab of expectedTabs) {
  const tabMatch = traceDrawer.match(
    new RegExp(`<el-tab-pane label="${tab.label}" name="${tab.name}"[\\s\\S]*?<\\/el-tab-pane>`)
  )
  assert.ok(tabMatch, `追溯记录弹框必须保留 ${tab.label} 页签。`)
  const tabBlock = tabMatch[0]
  assert.match(tabBlock, new RegExp(`<${tab.component}[\\s\\S]*?\\/?>`), `${tab.label} 页签必须挂载 ${tab.component}。`)
  assert.doesNotMatch(tabBlock, /<el-button[\s\S]*查看(变更|审计|追溯)[\s\S]*<\/el-button>/, `${tab.label} 页签不得只保留二级菜单跳转按钮。`)
  assert.doesNotMatch(tabBlock, /<el-table\b/, `${tab.label} 页签不得继续直接写裸 el-table，必须走标准列表组件。`)
  assert.match(
    batchDetail,
    new RegExp(`import ${tab.component} from '${tab.importPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}'`),
    `批次详情必须导入 ${tab.component}。`
  )
}

const componentFiles = [
  'src/views/mes/pro/edhr/components/ReleaseEventListPane.vue',
  'src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue',
  'src/views/mes/pro/edhr/components/OperationAuditListPane.vue',
  'src/views/mes/pro/edhr/components/DomainTraceListPane.vue'
]

for (const file of componentFiles) {
  const source = read(file)
  assert.match(source, /<UnifiedListTemplate/, `${file} 必须使用标准列表模板。`)
}

for (const tab of expectedTabs) {
  const file =
    tab.component === 'FormTraceChangeTab'
      ? 'src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue'
      : `src/views/mes/pro/edhr/components/${tab.component}.vue`
  const source = read(file)
  assert.ok(source.includes(`table-key="${tab.tableKey}"`), `${tab.component} 必须使用稳定 table-key ${tab.tableKey}。`)
  assert.ok(source.includes(`data-user-table-key="${tab.tableKey}"`), `${tab.component} 内部表格必须声明 data-user-table-key。`)
}

console.log('PASS: eDHR trace drawer four tabs standard list contract')
