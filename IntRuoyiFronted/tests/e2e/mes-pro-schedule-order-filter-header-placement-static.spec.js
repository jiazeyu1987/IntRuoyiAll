const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const templatePath = path.join(root, 'src/components/UnifiedListTemplate/index.vue')

const source = fs.readFileSync(pagePath, 'utf8')
const templateSource = fs.readFileSync(templatePath, 'utf8')

assert.ok(source.includes("import TableQuickFilter from '@/components/TableQuickFilter/index.vue'"), '排产工单页必须显式接管快速筛选控件。')

assert.ok(
  /<ContentWrap title="排产工单">[\s\S]*?<template #header>[\s\S]*schedule-order-pool__header-filter[\s\S]*<TableQuickFilter[\s\S]*:filter-definitions="scheduleOrderQuickFilterDefinitions"[\s\S]*@query="scheduleOrderQuickFilter\.applyQuickFilter"[\s\S]*schedule-order-pool__header-actions/.test(
    source
  ),
  '排产工单快速筛选必须放入 ContentWrap 标题栏右侧，并位于页面按钮组左侧。'
)

assert.ok(
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.scheduleOrder\.main"[\s\S]*:show-query-form="false"/.test(
    source
  ),
  '排产工单主列表必须关闭统一列表模板内置筛选行，避免表格上方重复出现筛选条。'
)

assert.ok(
  /showQueryForm\?:\s*boolean/.test(templateSource) &&
    /v-if="showQueryForm !== false"[\s\S]*class="unified-list-template__query-form"/.test(
      templateSource
    ),
  '统一列表模板必须支持按页面关闭内置查询表单。'
)

assert.ok(
  /\.schedule-order-pool__header-filter\s*\{[\s\S]*width:\s*100%;[\s\S]*min-width:\s*0;[\s\S]*max-width:\s*780px;/.test(
    source
  ),
  '标题栏筛选区域必须占用标题右侧空白区域，并限制宽度避免挤压右侧按钮。'
)

assert.ok(
  /\.schedule-order-pool__header-main\s*\{[\s\S]*display:\s*grid;[\s\S]*width:\s*100%;[\s\S]*min-width:\s*0;[\s\S]*grid-template-columns:\s*minmax\(620px,\s*1fr\) auto;/.test(
    source
  ),
  '排产工单标题栏必须用两段式网格承载筛选和操作按钮。'
)

assert.ok(
  /\.schedule-order-pool\s*:deep\(\.el-card__header > \.flex\)\s*\{[\s\S]*width:\s*100%;[\s\S]*min-width:\s*0;/.test(
    source
  ) &&
    /\.schedule-order-pool\s*:deep\(\.el-card__header > \.flex > \.flex-grow\)\s*\{[\s\S]*min-width:\s*0;/.test(
      source
    ),
  'ContentWrap 标题槽父级必须允许筛选栏占位和收缩，避免红框筛选项被挤没。'
)

assert.ok(
  /\.schedule-order-pool__header-filter\s*:deep\(\.table-quick-filter\)\s*\{[\s\S]*display:\s*grid;[\s\S]*grid-template-columns:\s*150px 96px minmax\(180px,\s*1fr\) auto;/.test(
    source
  ),
  '标题栏快速筛选必须显式为字段、条件、输入框和查询按钮保留网格列。'
)

assert.ok(
  /<UserTableColumnSettings[\s\S]*class="schedule-order-pool__header-column-settings"/.test(source) &&
    /同步工单[\s\S]*导出[\s\S]*手动重排[\s\S]*UserTableColumnSettings/.test(source),
  '页面标题栏右侧仍必须保留同步工单、导出、手动重排和显示字段。'
)

console.log('PASS: MES schedule order filter header placement static contract')
