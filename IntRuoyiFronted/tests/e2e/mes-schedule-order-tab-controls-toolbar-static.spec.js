const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const mainListPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/scheduleorder/components/ScheduleOrderMainList.vue'
)

assert.equal(fs.existsSync(pagePath), true, '排产工单页面必须存在。')
assert.equal(fs.existsSync(mainListPath), true, '排产工单主列表组件必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')
const mainListSource = fs.readFileSync(mainListPath, 'utf8')

assert.equal(
  source.includes('<ContentWrap title="排产工单">'),
  false,
  '排产工单页面顶部标题栏必须删除，不能继续渲染红框标题区。'
)
assert.match(
  source,
  /<ContentWrap[\s\S]*class="schedule-order-pool__content"[\s\S]*:body-style="\{ height: '100%', padding: '10px', display: 'flex', flexDirection: 'column' \}"/,
  '排产工单页面内容卡片必须占满可视高度，避免列表下方留白。'
)

const headerStart = source.indexOf('<template #header>')
const headerEnd = source.indexOf('</template>', headerStart)
const headerSource = headerStart >= 0 && headerEnd > headerStart ? source.slice(headerStart, headerEnd) : ''

for (const forbidden of ['同步工单', '导出', '手动重排', '批量冻结', '批量解冻', '批量删除', 'UserTableColumnSettings']) {
  assert.equal(headerSource.includes(forbidden), false, `页签外标题栏不能继续渲染 ${forbidden}。`)
}

const scheduleTabStart = source.indexOf('<el-tab-pane label="排产工单" name="scheduleOrders">')
const admissionTabStart = source.indexOf('<el-tab-pane label="同步工单" name="workOrderAdmission">')
assert.ok(scheduleTabStart >= 0, '必须存在排产工单页签。')
assert.ok(admissionTabStart > scheduleTabStart, '同步工单页签必须位于排产工单页签之后。')
const scheduleTabSource = source.slice(scheduleTabStart, admissionTabStart)
const admissionTabSource = source.slice(admissionTabStart)
const admissionActionsStart = admissionTabSource.indexOf('<template #actions>')
const admissionActionsEnd = admissionTabSource.indexOf('</template>', admissionActionsStart)
assert.ok(admissionActionsStart >= 0 && admissionActionsEnd > admissionActionsStart, '同步工单页签必须存在 actions 工具栏。')
const admissionActionsSource = admissionTabSource.slice(admissionActionsStart, admissionActionsEnd)

assert.match(
  scheduleTabSource,
  /<template #actions>[\s\S]*schedule-order-pool__tab-actions[\s\S]*导出[\s\S]*手动重排[\s\S]*UserTableColumnSettings/,
  '排产工单页签工具栏必须保留导出、重排和显示字段。'
)
const scheduleActionsStart = scheduleTabSource.indexOf('<template #actions>')
const scheduleActionsEnd = scheduleTabSource.indexOf('</template>', scheduleActionsStart)
const scheduleActionsSource =
  scheduleActionsStart >= 0 && scheduleActionsEnd > scheduleActionsStart
    ? scheduleTabSource.slice(scheduleActionsStart, scheduleActionsEnd)
    : ''
for (const forbidden of ['同步工单', '批量冻结', '批量解冻', '批量删除']) {
  assert.equal(scheduleActionsSource.includes(forbidden), false, `排产工单页签工具栏不能继续渲染 ${forbidden}。`)
}
assert.match(
  scheduleTabSource,
  /<ScheduleOrderMainList[\s\S]*<template #actions>/,
  '排产工单控制按钮必须通过排产工单主列表组件的 actions 插槽渲染。'
)
assert.match(
  mainListSource,
  /<UnifiedListTemplate[\s\S]*class="schedule-order-pool__schedule-template"[\s\S]*<template #actions>/,
  '排产工单主列表组件必须把 actions 插槽放在自己的统一列表工具栏内。'
)
assert.match(
  scheduleTabSource,
  /:columns="scheduleOrderColumns"[\s\S]*:saving="scheduleOrderColumnSaving"/,
  '排产工单页签显示字段必须绑定排产工单列配置。'
)
assert.match(
  admissionActionsSource,
  /<template #actions>[\s\S]*schedule-order-pool__admission-actions[\s\S]*重置[\s\S]*选中工单加入排产工单池[\s\S]*UserTableColumnSettings/,
  '同步工单页签工具栏必须包含重置、入池和显示字段。'
)
for (const forbidden of ['schedule-order-pool__admission-summary', '可入池', '警告', '阻断']) {
  assert.equal(admissionActionsSource.includes(forbidden), false, `同步工单页签工具栏不能继续渲染 ${forbidden}。`)
}
assert.match(
  admissionActionsSource,
  /schedule-order-pool__admission-show-admitted[\s\S]*显示已入池订单/,
  '同步工单页签工具栏允许渲染新的显示已入池订单开关。'
)
assert.match(
  source,
  /\.schedule-order-pool__admission-template\s+:deep\(\.unified-list-template__query-form\)\s*\{[\s\S]*flex-wrap:\s*nowrap;[\s\S]*align-items:\s*center;/,
  '同步工单页签筛选和按钮必须在同一行，按钮位于右侧黄色区域。'
)
assert.equal(
  /\.schedule-order-pool__admission-template\s+:deep\(\.unified-list-template__toolbar-actions\)\s*\{[\s\S]*flex-basis:\s*100%;/.test(source),
  false,
  '同步工单页签按钮区不能再被强制挤到筛选行下方。'
)
assert.match(
  admissionTabSource,
  /:columns="workOrderAdmissionColumns"[\s\S]*:saving="workOrderAdmissionColumnSaving"/,
  '同步工单页签显示字段必须绑定同步工单列配置。'
)
assert.equal(headerSource.includes('schedule-order-pool__header-actions'), false, 'ContentWrap 标题栏不能继续保留全局按钮组。')
assert.match(
  source,
  /\.schedule-order-pool__schedule-template\s+:deep\(\.unified-list-template__query-form\)\s*\{[\s\S]*flex-wrap:\s*nowrap;[\s\S]*align-items:\s*center;/,
  '排产工单页签筛选和按钮必须在同一行，按钮位于右侧紫框区域。'
)
assert.equal(
  /\.schedule-order-pool__schedule-template\s+:deep\(\.unified-list-template__toolbar-actions\)\s*\{[\s\S]*flex-basis:\s*100%;/.test(source),
  false,
  '排产工单页签按钮区不能再被强制挤到筛选行下方。'
)
assert.match(
  source,
  /const scheduleOrderTableHeight = '100%'/,
  '排产工单和同步工单表格高度必须由页签固定列表容器控制。'
)
assert.match(
  scheduleTabSource,
  /:height="scheduleOrderTableHeight"/,
  '排产工单表格必须使用固定列表容器高度。'
)
assert.match(
  admissionTabSource,
  /:height="scheduleOrderTableHeight"/,
  '同步工单表格必须使用固定列表容器高度。'
)
assert.equal(admissionTabSource.includes(':height="520"'), false, '同步工单表格不能继续使用固定 520 高度。')
assert.equal(
  admissionTabSource.includes(`:width="getWorkOrderAdmissionColumnWidthString('message')"`),
  false,
  '同步工单不可排原因列必须释放固定宽度，用列表内容填满右侧空白区域。'
)
assert.match(
  source,
  /\.schedule-order-pool\s*\{[\s\S]*height:\s*calc\(100vh - var\(--top-tool-height\) - var\(--tags-view-height\) - var\(--app-footer-height\) - 32px\);[\s\S]*min-height:\s*0;/,
  '排产工单页面必须限定在可视高度内，避免页脚上方出现空白。'
)
assert.match(
  source,
  /\.schedule-order-pool__tabs\s+:deep\(\.el-tabs__content\)\s*\{[\s\S]*flex:\s*1 1 auto;[\s\S]*min-height:\s*0;[\s\S]*overflow:\s*hidden;/,
  '排产工单和同步工单页签内容区必须固定，不能让整页滚动。'
)
assert.match(
  source,
  /\.schedule-order-pool__schedule-template\s+:deep\(\.unified-list-template__table-shell\),[\s\S]*\.schedule-order-pool__admission-template\s+:deep\(\.unified-list-template__table-shell\)\s*\{[\s\S]*flex:\s*1 1 auto;[\s\S]*min-height:\s*0;[\s\S]*overflow:\s*hidden;/,
  '两个页签表格外壳必须撑满剩余区域，表头和分页固定，仅表体滚动。'
)
assert.match(
  source,
  /\.schedule-order-pool__admission-table-shell\s*\{[\s\S]*height:\s*100%;[\s\S]*overflow-x:\s*auto;/,
  '同步工单内部表格外壳必须填满统一列表表格区域。'
)

console.log('PASS: MES schedule order tab controls toolbar static contract')
