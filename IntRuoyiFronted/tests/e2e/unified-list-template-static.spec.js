const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const componentPath = path.join(root, 'src/components/UnifiedListTemplate/index.vue')
const scheduleOrderPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const scheduleOrderMainListPath = path.join(
  root,
  'src/views/mes/pro/scheduleorder/components/ScheduleOrderMainList.vue'
)

assert.equal(fs.existsSync(componentPath), true, '统一列表模板组件必须存在。')
assert.equal(fs.existsSync(scheduleOrderMainListPath), true, '排产工单主列表包装组件必须存在。')

const componentSource = fs.readFileSync(componentPath, 'utf8')
const scheduleOrderSource = fs.readFileSync(scheduleOrderPath, 'utf8')
const scheduleOrderMainListSource = fs.readFileSync(scheduleOrderMainListPath, 'utf8')

assert.match(componentSource, /TableQuickFilter/, '模板必须内置快速过滤组件。')
assert.match(componentSource, /UserTableColumnSettings/, '模板必须内置显示字段组件。')
assert.match(componentSource, /Pagination/, '模板必须内置分页组件。')
assert.match(componentSource, /name:\s*'UnifiedListTemplate'/, '模板必须声明稳定组件名称。')
assert.match(componentSource, /tableKey:\s*string/, '模板必须接收稳定 tableKey。')
assert.match(componentSource, /filterDefinitions/, '模板必须接收快速过滤字段定义。')
assert.match(componentSource, /quickFilterState/, '模板必须接收快速过滤状态。')
assert.match(componentSource, /selectedFilterDefinition/, '模板必须接收当前过滤字段定义。')
assert.match(componentSource, /operatorOptions/, '模板必须接收合法操作符。')
assert.match(componentSource, /columns/, '模板必须接收显示字段列状态。')
assert.match(componentSource, /columnSaving/, '模板必须接收列配置保存状态。')
assert.match(componentSource, /showColumnSettings/, '模板必须允许页面隐藏默认显示字段入口。')
assert.match(componentSource, /<slot\s+name="extra-filters"/, '模板必须提供额外筛选插槽。')
assert.match(componentSource, /<slot\s+name="actions"/, '模板必须提供业务动作插槽。')
assert.match(componentSource, /<slot\s+name="table"/, '模板必须提供表格插槽。')
assert.match(componentSource, /update:quickFilterState/, '模板必须透传快速过滤状态更新。')
assert.match(componentSource, /quick-filter-query/, '模板必须透传快速过滤查询事件。')
assert.match(componentSource, /column-change/, '模板必须透传显示字段自动保存事件。')
assert.match(componentSource, /column-reset/, '模板必须透传列配置重置事件。')
assert.match(componentSource, /update:page/, '模板必须透传分页页码更新。')
assert.match(componentSource, /update:limit/, '模板必须透传分页条数更新。')
assert.match(componentSource, /@pagination="\$emit\('pagination', \$event\)"/, '模板必须透传分页刷新事件。')
assert.match(componentSource, /showColumnReset/, '模板必须允许页面隐藏重置列按钮。')
assert.doesNotMatch(componentSource, />\s*保存\s*</, '模板不能重新引入显示字段手动保存按钮。')
assert.doesNotMatch(componentSource, /localStorage|sessionStorage/, '模板不能使用浏览器本地存储兜底。')
assert.match(componentSource, /\.unified-list-template__toolbar/, '模板必须提供统一工具栏样式。')
assert.match(componentSource, /@media \(max-width:\s*1360px\)/, '模板必须提供窄屏换行样式。')

assert.match(scheduleOrderSource, /import BaseScheduleOrderMainList from '\.\/components\/ScheduleOrderMainList\.vue'/, '排产工单必须通过主列表包装组件接入统一列表模板。')
assert.match(scheduleOrderMainListSource, /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/, '排产工单主列表包装组件必须导入统一列表模板。')
assert.match(scheduleOrderMainListSource, /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.scheduleOrder\.main"/, '排产工单必须用稳定 tableKey 接入模板。')
assert.match(scheduleOrderSource, /key:\s*'completionFilter'[\s\S]*label:\s*'完成筛选'[\s\S]*queryParamKey:\s*'completionFilter'/, '完成筛选必须融合进统一快速筛选列表。')
assert.doesNotMatch(scheduleOrderSource, /#extra-filters[\s\S]*label="完成筛选"/, '完成筛选不能再作为额外筛选插槽单独渲染。')
assert.match(
  scheduleOrderSource,
  /<ScheduleOrderMainList[\s\S]*?<template #actions>[\s\S]*导出[\s\S]*手动重排[\s\S]*<UserTableColumnSettings/,
  '排产工单主操作和显示字段必须通过主列表动作插槽保留。'
)
assert.match(
  scheduleOrderSource,
  /<el-tabs[\s\S]*<el-tab-pane label="排产工单" name="scheduleOrders"[\s\S]*<el-tab-pane label="同步工单" name="workOrderAdmission"/,
  '排产工单和同步工单必须保留同屏页签入口。'
)
assert.doesNotMatch(
  scheduleOrderMainListSource,
  />\s*同步工单\s*</,
  '排产工单主列表包装组件不能混入同步工单业务入口。'
)
assert.match(scheduleOrderSource, /#table[\s\S]*<el-table[\s\S]*data-user-table-key="mes\.pro\.scheduleOrder\.main"[\s\S]*border[\s\S]*@header-dragend="handleScheduleOrderHeaderDragend"/, '排产工单主表必须在表格插槽中保留列宽拖拽持久化。')
assert.doesNotMatch(scheduleOrderSource, /import TableQuickFilter from '@\/components\/TableQuickFilter\/index.vue'/, '排产工单不应再直接导入快速过滤组件。')
assert.match(scheduleOrderSource, /import UserTableColumnSettings from '@\/components\/UserTableColumnSettings\/index.vue'/, '排产工单标题栏右侧显示字段需要直接导入显示字段组件。')
assert.match(scheduleOrderMainListSource, /:show-column-settings="false"/, '排产工单必须隐藏筛选行默认显示字段入口，避免与标题栏显示字段重复。')

console.log('PASS: unified list template static contract')
