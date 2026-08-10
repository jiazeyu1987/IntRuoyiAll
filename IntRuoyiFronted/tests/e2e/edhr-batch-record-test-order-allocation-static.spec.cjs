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
  /<el-tab-pane\s+label="一线生产"\s+name="frontlineProduction"[\s\S]*<el-tab-pane\s+label="订单分配"\s+name="orderAllocation"/,
  '订单分配必须作为一线生产后的第四个内部 Tab。'
)
assert.match(
  page,
  /type\s+BatchRecordTestListKey\s*=\s*\|?\s*'productionLeader'\s*\|\s*'frontlinePqc'\s*\|\s*'frontlineProduction'\s*\|\s*'orderAllocation'/,
  '测试列表联合类型必须包含 orderAllocation。'
)
assert.match(
  page,
  /orderAllocation:\s*\{[\s\S]*casePrefix:\s*'批记录测试-订单分配'[\s\S]*testScopePrefix:\s*'订单分配'[\s\S]*\}/,
  '订单分配必须具有隔离的测试项名称和测试范围前缀。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-order-allocation-list/,
  '订单分配列表必须提供稳定 DOM 锚点。'
)
assert.match(
  page,
  /<UnifiedListTemplate[\s\S]*data-edhr-batch-record-test-order-allocation-list[\s\S]*table-key="mes\.pro\.edhrBatchRecordTest\.orderAllocation"[\s\S]*:query-model="orderAllocationQueryParams"[\s\S]*:filter-definitions="orderAllocationQuickFilterDefinitions"[\s\S]*:columns="orderAllocationColumns"[\s\S]*:show-column-settings="false"[\s\S]*:single-line-toolbar="true"[\s\S]*:total="filteredOrderAllocationRows\.length"[\s\S]*v-model:page="orderAllocationQueryParams\.pageNo"[\s\S]*v-model:limit="orderAllocationQueryParams\.pageSize"[\s\S]*@pagination="handleOrderAllocationPagination"/,
  '订单分配必须沿用标准列表并拥有独立筛选、列、分页和 table-key。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-order-allocation-list[\s\S]*@click="openCreateRowDialog\('orderAllocation'\)"[\s\S]*data-user-table-key="mes\.pro\.edhrBatchRecordTest\.orderAllocation"[\s\S]*:data="pagedOrderAllocationRows"[\s\S]*openDescriptionEditor\('orderAllocation', row\)[\s\S]*handleDeleteRow\('orderAllocation', row\)/,
  '订单分配列表必须接入新增、表格、修改和删除的正式共享能力。'
)
assert.match(
  page,
  /const\s+orderAllocationRows\s*=\s*ref<BatchRecordTestRow\[]>\(\[/,
  '订单分配必须声明独立的固定测试定义列表。'
)
assert.equal(
  (page.match(/caseName:\s*'批记录测试-订单分配-/g) || []).length,
  8,
  '订单分配测试定义必须固定为八行。'
)

const taskTitles = [
  '工序共享报工池与分配范围',
  'FIFO固定顺序与部分分配',
  'FIFO草稿与空白手工分配',
  '未放行调整与已放行锁定',
  '分配订单列与管理历史视图',
  '合格可分配数量关系',
  '调整增减审计',
  '并发重校验与工单状态变化'
]
for (const title of taskTitles) {
  assert.ok(page.includes(title), `订单分配测试任务必须包含：${title}`)
}

const requiredBusinessTexts = [
  '报工按工序进入共享报工池，不存在“当前订单”',
  '现有活跃工单需求不足时不得拒绝报工',
  '活跃工单列表的固定全局顺序',
  '报工不足时允许先部分满足最靠前工单',
  '超出需求的未分配数量继续保留在本次报工池',
  '也可以从空白状态开始手工分配',
  '手工调整不要求继续遵循 FIFO',
  '从 A 工单转移给加急 C 工单',
  '已放行分配必须以绿色显示并锁定',
  '部分放行时只锁定已放行部分',
  '报工管理列表必须有“分配订单”列',
  '未分配数量为零且全部分配均已放行后从管理列表移除',
  '只有合格数量可以进入订单分配',
  '合格可分配数量 = 已放行锁定数量 + 未放行预分配数量 + 尚未分配数量',
  'A 工单 `-100`、C 工单 `+100`',
  '记录操作人、操作时间、原工单、目标工单和调整原因',
  '并发保护下重新校验报工可用数量和工单当前工序剩余需求',
  '工单减量、暂停或取消时，未放行数量退回原报工池',
  '已放行数量不得自动退回'
]
for (const text of requiredBusinessTexts) {
  assert.ok(page.includes(text), `订单分配测试描述必须包含可验证口径：${text}`)
}

assert.match(
  page,
  /const\s+orderAllocationQueryParams\s*=\s*reactive\(\{[\s\S]*pageNo:\s*1[\s\S]*pageSize:\s*10[\s\S]*keyword:\s*''[\s\S]*\}\)/,
  '订单分配必须拥有独立查询和分页状态。'
)
assert.match(
  page,
  /useUserTableColumns\(\s*'mes\.pro\.edhrBatchRecordTest\.orderAllocation'/,
  '订单分配必须拥有独立列配置状态。'
)
assert.match(
  page,
  /useTableQuickFilter\(\s*'mes\.pro\.edhrBatchRecordTest\.orderAllocation'[\s\S]*orderAllocationQuickFilterDefinitions[\s\S]*orderAllocationQueryParams[\s\S]*applyOrderAllocationListFilters/,
  '订单分配必须拥有独立快速筛选状态。'
)
assert.match(
  page,
  /const\s+filteredOrderAllocationRows\s*=\s*computed\([\s\S]*filterBatchRecordTestRows\(orderAllocationRows\.value, keyword\)/,
  '订单分配筛选必须复用正式测试定义筛选函数。'
)
assert.match(
  page,
  /const\s+pagedOrderAllocationRows\s*=\s*computed\([\s\S]*filteredOrderAllocationRows\.value\.slice/,
  '订单分配必须根据独立分页状态输出当前页。'
)
assert.match(
  page,
  /function\s+getBatchRecordTestRowsRef\([\s\S]*listKey\s*===\s*'orderAllocation'\)\s*return\s+orderAllocationRows/,
  '共享 CRUD 分派必须覆盖订单分配列表。'
)
assert.match(
  page,
  /function\s+getBatchRecordTestQueryParams\([\s\S]*listKey\s*===\s*'orderAllocation'\)\s*return\s+orderAllocationQueryParams/,
  '共享新增分页分派必须覆盖订单分配列表。'
)
assert.match(
  page,
  /function\s+captureDefaultBatchRecordTestRows\([\s\S]*orderAllocation:\s*cloneBatchRecordTestRows\(orderAllocationRows\.value\)/,
  '正式默认定义快照必须包含订单分配，保证持久化恢复和租户缓存覆盖第四个 Tab。'
)
assert.match(
  page,
  /function\s+buildCodeReadonlyCasePayload\([\s\S]*testDataText:[\s\S]*definition\.description[\s\S]*analysisMode:\s*'CODE_READONLY'[\s\S]*remark:\s*definition\.description/,
  '每段订单分配文字必须进入 CODE_READONLY 测试数据和结构化检查点。'
)
assert.match(
  page,
  /startCodeReadonlyCodexTestExecution\(\{[\s\S]*targetTenantId:\s*selectedTenantId\.value[\s\S]*caseDefinition:\s*buildCodeReadonlyCasePayload\(row\)/,
  '测试按钮必须继续使用正式 CODE_READONLY 原子执行入口。'
)
assert.doesNotMatch(page, /child_process|spawn\s*\(|execFile|codex(?:\.cmd)?\s+exec/, '浏览器不得裸调 Codex CLI。')
assert.doesNotMatch(page, /catch\s*\{\s*\}/, '页面不得使用空 catch 吞异常。')

console.log('edhr-batch-record-test-order-allocation-static PASS')
