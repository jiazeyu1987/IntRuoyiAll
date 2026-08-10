const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue'),
  'utf8'
)

assert.match(
  page,
  /<el-tab-pane\s+label="订单分配"\s+name="orderAllocation"[\s\S]*<el-tab-pane\s+label="批记录映射"\s+name="batchRecordMapping"/,
  '批记录映射必须作为订单分配后的第五个内部 Tab。'
)
assert.match(
  page,
  /type\s+BatchRecordTestListKey\s*=[\s\S]*'orderAllocation'\s*\|\s*'batchRecordMapping'/,
  '测试列表联合类型必须包含 batchRecordMapping。'
)
assert.match(
  page,
  /batchRecordMapping:\s*\{[\s\S]*casePrefix:\s*'批记录测试-批记录映射'[\s\S]*testScopePrefix:\s*'批记录映射'[\s\S]*\}/,
  '批记录映射必须具有隔离的测试项名称和测试范围前缀。'
)
assert.match(
  page,
  /<UnifiedListTemplate[\s\S]*data-edhr-batch-record-test-mapping-list[\s\S]*table-key="mes\.pro\.edhrBatchRecordTest\.batchRecordMapping"[\s\S]*:query-model="batchRecordMappingQueryParams"[\s\S]*:filter-definitions="batchRecordMappingQuickFilterDefinitions"[\s\S]*:columns="batchRecordMappingColumns"[\s\S]*:total="filteredBatchRecordMappingRows\.length"[\s\S]*v-model:page="batchRecordMappingQueryParams\.pageNo"[\s\S]*v-model:limit="batchRecordMappingQueryParams\.pageSize"[\s\S]*@pagination="handleBatchRecordMappingPagination"/,
  '批记录映射必须沿用标准列表并拥有独立筛选、列、分页和 table-key。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-mapping-list[\s\S]*@click="openCreateRowDialog\('batchRecordMapping'\)"[\s\S]*data-user-table-key="mes\.pro\.edhrBatchRecordTest\.batchRecordMapping"[\s\S]*:data="pagedBatchRecordMappingRows"[\s\S]*openDescriptionEditor\('batchRecordMapping', row\)[\s\S]*handleDeleteRow\('batchRecordMapping', row\)/,
  '批记录映射列表必须接入新增、表格、修改和删除的正式共享能力。'
)

const mappingPane = page.match(
  /<el-tab-pane\s+label="批记录映射"\s+name="batchRecordMapping">([\s\S]*?)<\/el-tab-pane>/
)?.[1]
assert.ok(mappingPane, '必须能隔离读取批记录映射页签模板。')
const mappingTitleColumn = mappingPane.match(
  /<el-table-column\b(?=[^>]*\bprop="title")[^>]*\/>/
)?.[0]
assert.ok(mappingTitleColumn, '批记录映射必须存在映射项列。')
assert.match(
  mappingTitleColumn,
  /class-name="edhr-batch-record-test-page__mapping-title-column"[\s\S]*ColumnMinWidthString\('title', 180\)[\s\S]*:show-overflow-tooltip="false"/,
  '映射项列必须在较窄桌面以 180px 最小宽度正常换行，不能显示省略号。'
)
const mappingActionColumn = mappingPane.match(
  /<el-table-column\b(?=[^>]*\bprop="actions")[^>]*>/
)?.[0]
assert.ok(mappingActionColumn, '批记录映射必须存在操作列。')
assert.match(
  mappingActionColumn,
  /getBatchRecordMappingColumnWidthString\('actions', 220\)/,
  '批记录映射操作列必须为四个行操作预留 220px。'
)
assert.doesNotMatch(
  mappingActionColumn,
  /fixed="right"/,
  '较窄桌面下固定操作列会覆盖描述内容，批记录映射操作列不得固定。'
)
assert.match(
  page,
  /const batchRecordMappingDefaultColumns:[\s\S]*?\{ key: 'title', label: '业务环节', minWidth: 180 \}[\s\S]*?\{ key: 'description', label: '业务说明', minWidth: 280,[\s\S]*?\{ key: 'actions', label: '操作', width: 220,/,
  '批记录映射必须使用业务化列名，并保持窄桌面列宽边界。'
)
assert.match(
  page,
  /const\s+batchRecordMappingRows\s*=\s*ref<BatchRecordTestRow\[]>\(\[/,
  '批记录映射必须声明独立的固定测试定义列表。'
)
assert.equal(
  (page.match(/caseName:\s*'批记录测试-批记录映射-/g) || []).length,
  15,
  '批记录映射测试定义必须固定为十五行。'
)

const mappingTitles = [
  '放行申请条件',
  '生产组长发起申请',
  '申请依据复核',
  '批次资料统一归档',
  '工序批记录对应关系',
  '生产批记录归集',
  '过程检验记录归集',
  '生产损耗记录归集',
  '填写审核与签名追溯',
  '放行资料形成顺序',
  '放行资料完整性检查',
  '生产负责人审批',
  '重复申请处理',
  '缺失资料处理',
  '全流程业务验证'
]
for (const title of mappingTitles) {
  assert.ok(page.includes(title), `批记录映射需求必须包含：${title}`)
}

const requiredBusinessTexts = [
  '生产和检验均已完成并经过组长确认后，才允许申请放行',
  '生产组长在活跃订单中发起放行申请，并可填写申请说明',
  '重新核对订单、工单、产品、工艺路线、各道工序以及生产和检验记录',
  '同一活跃订单的放行资料统一归入一份批次执行档案',
  '不得用补充表单槽位或工序开始配置替代正式批记录表单',
  '生产数量、设备、工艺参数、填写人、审核人和签名时间',
  '检验项目、检验方法、质量标准、实测结果、判定、填写人、审核人和签名时间',
  '损耗数量、损耗原因、所属工序、产品、批号、填写人、审核人和签名时间',
  '签名人员和签名时间必须来自实际填写、复核和确认记录',
  '先建立批次档案，再依次形成生产批记录、过程检验记录和生产损耗记录',
  '任何资料缺失时均不得进入负责人审批',
  '生产负责人在正式审批任务中完成放行或驳回',
  '同一申请重复提交时沿用原处理结果',
  '明确告知缺少的资料、不能继续的原因和处理建议',
  '生产组长发起申请、生产负责人审批以及最终资料核验均通过实际业务页面完成'
]
for (const text of requiredBusinessTexts) {
  assert.ok(page.includes(text), `批记录映射描述必须包含可验证口径：${text}`)
}

const mappingRowsBlock = page.match(
  /const\s+batchRecordMappingRows\s*=\s*ref<BatchRecordTestRow\[]>\(\[([\s\S]*?)\n\]\)\n\nconst defaultBatchRecordTestRows/
)?.[1]
assert.ok(mappingRowsBlock, '必须能隔离读取批记录映射固定数据块。')
assert.doesNotMatch(
  mappingRowsBlock,
  /activeOrderId|idempotencyKey|applyRemark|sourceSnapshotHash|applicationId|batchExecutionId|releaseTransactionId|releaseApprovalWorkTaskId|signatureEvidenceCount|blockerType|PENDING_RELEASE_APPROVAL|RELEASE_APPROVE|Writer|writer|Fixture|E2E|API|service|raw payload|submitForApproval|release precheck|formBindings|默认MAIN/,
  '批记录映射的标题、说明和测试项业务名称不得出现字段名、状态码、程序组件或测试工具术语。'
)

assert.match(
  page,
  /const\s+batchRecordMappingQueryParams\s*=\s*reactive\(\{[\s\S]*pageNo:\s*1[\s\S]*pageSize:\s*10[\s\S]*keyword:\s*''[\s\S]*\}\)/,
  '批记录映射必须拥有独立查询和分页状态。'
)
assert.match(
  page,
  /useUserTableColumns\(\s*'mes\.pro\.edhrBatchRecordTest\.batchRecordMapping'/,
  '批记录映射必须拥有独立列配置状态。'
)
assert.match(
  page,
  /useTableQuickFilter\(\s*'mes\.pro\.edhrBatchRecordTest\.batchRecordMapping'[\s\S]*batchRecordMappingQuickFilterDefinitions[\s\S]*batchRecordMappingQueryParams[\s\S]*applyBatchRecordMappingListFilters/,
  '批记录映射必须拥有独立快速筛选状态。'
)
assert.match(
  page,
  /const\s+filteredBatchRecordMappingRows\s*=\s*computed\([\s\S]*filterBatchRecordTestRows\(batchRecordMappingRows\.value, keyword\)/,
  '批记录映射筛选必须复用正式测试定义筛选函数。'
)
assert.match(
  page,
  /const\s+pagedBatchRecordMappingRows\s*=\s*computed\([\s\S]*filteredBatchRecordMappingRows\.value\.slice/,
  '批记录映射必须根据独立分页状态输出当前页。'
)
assert.match(
  page,
  /function\s+getBatchRecordTestRowsRef\([\s\S]*listKey\s*===\s*'batchRecordMapping'\)\s*return\s+batchRecordMappingRows/,
  '共享 CRUD 分派必须覆盖批记录映射列表。'
)
assert.match(
  page,
  /function\s+getBatchRecordTestQueryParams\([\s\S]*listKey\s*===\s*'batchRecordMapping'\)\s*return\s+batchRecordMappingQueryParams/,
  '共享新增分页分派必须覆盖批记录映射列表。'
)
assert.match(
  page,
  /function\s+captureDefaultBatchRecordTestRows\([\s\S]*batchRecordMapping:\s*cloneBatchRecordTestRows\(batchRecordMappingRows\.value\)/,
  '正式默认定义快照必须包含批记录映射，保证持久化恢复和租户缓存覆盖第五个 Tab。'
)
assert.match(
  page,
  /startCodeReadonlyCodexTestExecution\(\{[\s\S]*targetTenantId:\s*selectedTenantId\.value[\s\S]*caseDefinition:\s*buildCodeReadonlyCasePayload\(row\)/,
  '测试按钮必须继续使用正式 CODE_READONLY 原子执行入口。'
)
assert.doesNotMatch(
  page,
  /child_process|spawn\s*\(|execFile|codex(?:\.cmd)?\s+exec/,
  '浏览器不得裸调 Codex CLI。'
)
assert.doesNotMatch(page, /catch\s*\{\s*\}/, '页面不得使用空 catch 吞异常。')

console.log('edhr-batch-record-test-mapping-static PASS')
