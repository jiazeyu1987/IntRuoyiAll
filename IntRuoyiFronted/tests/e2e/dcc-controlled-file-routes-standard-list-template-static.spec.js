const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/dcc/controlled-file/routes/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
  'DCC 流程路线列表必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  'DCC 流程路线列表必须接入显示字段和列宽持久化 hook。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  'DCC 流程路线列表必须接入标准快速过滤 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.controlledFile\.routes\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, 'DCC 流程路线列表必须用 tableKey dcc.controlledFile.routes.main 接入 UnifiedListTemplate。')
const template = templateMatch[0]

const previewTemplateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.controlledFile\.routes\.preview"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(
  previewTemplateMatch,
  'DCC 流程路线派生四层预览列表必须用 tableKey dcc.controlledFile.routes.preview 接入 UnifiedListTemplate。'
)
const previewTemplate = previewTemplateMatch[0]

assert.match(template, /:query-model="queryParams"/, '标准模板必须绑定原查询参数。')
assert.match(template, /:filter-definitions="routeQuickFilterDefinitions"/, '标准模板必须绑定流程路线快速过滤字段。')
assert.match(template, /:quick-filter-state="routeQuickFilter\.state"/, '标准模板必须绑定快速过滤状态。')
assert.match(template, /@quick-filter-query="routeQuickFilter\.applyQuickFilter"/, '标准模板必须触发原路线查询。')
assert.match(template, /:columns="routeColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(template, /@column-change="saveRouteColumnConfig"/, '显示字段变化必须自动保存。')
assert.match(template, /:show-column-reset="false"/, '红框内重置列入口必须隐藏。')
assert.match(template, /v-model:page="queryParams\.pageNo"/, '标准模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="queryParams\.pageSize"/, '标准模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="handleRoutePagination"/, '标准模板分页必须复用列表分页处理。')

const actionsMatch = template.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, 'DCC 流程路线列表必须保留标准模板 actions 插槽。')
const actions = actionsMatch[1]
assert.match(actions, /handleQuery/, '标准模板工具栏必须保留查询路线操作。')
assert.doesNotMatch(actions, /resetQuery/, '红框内重置按钮必须删除。')
assert.doesNotMatch(actions, /handlePreview/, '红框内刷新预览按钮必须删除。')
assert.doesNotMatch(actions, />\s*重置\s*<\/el-button>/, 'actions 插槽中不应再渲染重置按钮。')
assert.doesNotMatch(actions, />\s*刷新预览\s*<\/el-button>/, 'actions 插槽中不应再渲染刷新预览按钮。')

assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-key="dcc\.controlledFile\.routes\.main"[\s\S]*@header-dragend="handleRouteHeaderDragend"/,
  '表格必须接入列宽拖拽持久化。'
)

for (const field of ['categoryName', 'node1', 'node2', 'node3', 'node4']) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到 DCC 流程路线列配置。`)
  assert.match(
    template,
    new RegExp(`isRouteColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

for (const stageNo of [1, 2, 3, 4]) {
  assert.match(template, new RegExp(`label="节点${stageNo}"`), `流程路线主列表必须显示节点${stageNo}列。`)
  assert.match(
    template,
    new RegExp(`formatRouteNodeAssignees\\(row, ${stageNo}\\)`),
    `节点${stageNo}列必须显示该阶段审批对象。`
  )
}

assert.doesNotMatch(template, /label="路线摘要"/, '主列表不应再显示路线摘要列。')
assert.doesNotMatch(template, /label="备注"/, '主列表不应再显示备注列。')
assert.doesNotMatch(template, /data-testid="dcc-route-summary"/, '主列表不应再保留路线摘要测试标识。')
assert.doesNotMatch(template, /row\.nodeSummary/, '主列表不应再显示合并后的节点摘要。')
assert.doesNotMatch(template, /row\.remark \|\| '-'/, '主列表不应再显示备注内容。')

assert.match(source, /const routeQuickFilterDefinitions = computed<TableQuickFilterDefinition\[\]>\(\(\) => \[/, '必须定义 DCC 流程路线快速过滤字段。')
assert.match(source, /key: 'categoryId'[\s\S]*label: '文件类别'[\s\S]*type: 'select'[\s\S]*queryParamKey: 'categoryId'/, '快速过滤必须支持文件类别。')
assert.match(source, /const routeQuickFilter = useTableQuickFilter\([\s\S]*'dcc\.controlledFile\.routes\.main'[\s\S]*routeQuickFilterDefinitions[\s\S]*queryParams[\s\S]*handleQuery/, '必须用标准 hook 连接快速过滤和查询。')
assert.match(source, /useUserTableColumns\('dcc\.controlledFile\.routes\.main', routeDefaultColumns\)/, '必须用稳定 tableKey 保存显示字段。')
assert.match(source, /const formatRouteNodeAssignees = \(row: ControlledFileApprovalRouteVO, stageNo: number\) => \{/, '必须提供主列表节点审批对象聚合函数。')
assert.match(source, /const formatRouteNodeSubject = \(node: ControlledFileApprovalRouteNodeVO\) => \{/, '必须提供单节点审批对象解析函数。')
assert.match(source, /await loadRouteSubjectLookups\(\)[\s\S]*await handleQuery\(\)/, '主列表加载前必须准备审批对象名称映射。')

assert.doesNotMatch(source, /class="-mb-15px"/, '旧搜索栏样式必须移除。')
assert.doesNotMatch(source, /<ContentWrap>[\s\S]*<el-form[\s\S]*查询路线[\s\S]*<\/ContentWrap>/, '旧查询表单必须移除。')
assert.doesNotMatch(
  source,
  /<ContentWrap v-if="queryParams\.categoryId">[\s\S]*<el-table :data="previewRows"/,
  '派生四层预览列表不得继续使用裸 el-table，必须由标准列表模板承载。'
)
assert.doesNotMatch(source, /preview-title-row/, '红框内派生四层预览标题行必须删除。')
assert.doesNotMatch(source, />\s*派生四层预览\s*</, '红框内派生四层预览标题必须删除。')
assert.doesNotMatch(source, /固定为文控审核、审核会签、批准、文控批准/, '红框内派生四层预览说明必须删除。')
assert.doesNotMatch(source, /const resetQuery = /, '删除重置按钮后不应保留废弃 resetQuery。')

assert.match(previewTemplate, /:show-query-form="false"/, '派生四层预览列表无需重复查询栏，但必须保留标准模板表格壳和分页。')
assert.match(previewTemplate, /:query-model="previewQueryParams"/, '派生四层预览模板必须绑定独立分页状态。')
assert.match(previewTemplate, /:columns="routePreviewColumns"/, '派生四层预览模板必须绑定显示字段配置。')
assert.match(previewTemplate, /:total="routePreviewTotal"/, '派生四层预览模板必须绑定总数。')
assert.match(previewTemplate, /v-model:page="previewQueryParams\.pageNo"/, '派生四层预览模板分页必须绑定 pageNo。')
assert.match(previewTemplate, /v-model:limit="previewQueryParams\.pageSize"/, '派生四层预览模板分页必须绑定 pageSize。')
assert.match(previewTemplate, /@pagination="handleRoutePreviewPagination"/, '派生四层预览模板必须保留分页事件。')
assert.match(
  previewTemplate,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-key="dcc\.controlledFile\.routes\.preview"[\s\S]*:data="paginatedPreviewRows"[\s\S]*@header-dragend="handleRoutePreviewHeaderDragend"/,
  '派生四层预览表格必须接入标准模板、分页数据和列宽拖拽持久化。'
)

for (const field of ['stageNo', 'stageName', 'approvalMode', 'candidateSourceIds', 'resolvedUserIds']) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到 DCC 流程路线预览列配置。`)
  assert.match(
    previewTemplate,
    new RegExp(`isRoutePreviewColumnVisible\\('${field}'\\)`),
    `${field} 预览列必须受显示字段配置控制。`
  )
}
assert.match(
  source,
  /useUserTableColumns\('dcc\.controlledFile\.routes\.preview', routePreviewDefaultColumns\)/,
  '派生四层预览必须用稳定 tableKey 保存显示字段。'
)

console.log('PASS: dcc controlled file routes standard list template static contract')
