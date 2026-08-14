const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const columnSettingsPath = path.join(root, 'src/components/UserTableColumnSettings/index.vue')
const templatePath = path.join(root, 'src/components/UnifiedListTemplate/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')
const columnSettingsSource = fs.readFileSync(columnSettingsPath, 'utf8')
const templateSource = fs.readFileSync(templatePath, 'utf8')

assert.ok(
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.scheduleOrder\.main"/.test(
    source
  ),
  '排产工单顶部筛选和动作区必须接入统一列表模板。'
)

assert.ok(
  /<ContentWrap title="排产工单">[\s\S]*?<template #header>[\s\S]*schedule-order-pool__header-actions[\s\S]*?同步工单[\s\S]*?导出[\s\S]*?手动重排[\s\S]*?<UserTableColumnSettings/.test(
    source
  ),
  '同步工单、导出、手动重排和显示字段必须放在排产工单标题栏右侧。'
)

assert.ok(
  !/<template #actions>[\s\S]*?同步工单[\s\S]*?手动重排[\s\S]*?<\/template>/.test(source),
  '排产工单主操作不能继续放在统一列表模板 actions 插槽内，避免占用筛选行右侧。'
)

assert.ok(
  !source.includes('schedule-order-pool__toolbar-group--query') &&
    !/<el-button[^>]*@click="handleScheduleOrderQuery"[\s\S]*?搜索[\s\S]*?<\/el-button>/.test(source) &&
    !/<el-button[^>]*@click="resetScheduleOrderQuery"[\s\S]*?重置[\s\S]*?<\/el-button>/.test(source),
  '排产工单页必须删除与快速过滤“查询”重复的页面级搜索/重置按钮组。'
)

assert.ok(
  /schedule-order-pool__toolbar-group schedule-order-pool__toolbar-group--primary[\s\S]*?同步工单[\s\S]*?手动重排/.test(
    source
  ),
  '同步工单和手动重排必须归入页面级主操作组。'
)

assert.ok(
  /schedule-order-pool__toolbar-group schedule-order-pool__toolbar-group--batch[\s\S]*?批量冻结[\s\S]*?批量解冻[\s\S]*?批量删除/.test(
    source
  ),
  '批量冻结、批量解冻和批量删除必须归入单独批量操作组。'
)

assert.ok(
  /\.unified-list-template__query-form\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-wrap:\s*nowrap;[\s\S]*align-items:\s*center;[\s\S]*gap:\s*12px;/.test(
    templateSource
  ),
  '统一列表模板查询表单必须使用不换行 flex 布局，让顶部 item 默认排成一行。'
)

assert.ok(
  /key:\s*'completionFilter'[\s\S]*label:\s*'完成状态'[\s\S]*queryParamKey:\s*'completionFilter'/.test(source) &&
    !/#extra-filters[\s\S]*label="完成状态"/.test(source),
  '完成状态必须融合进统一快速筛选列表，且继续写回 completionFilter 查询参数。'
)

assert.ok(
  /\.unified-list-template__toolbar\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-wrap:\s*nowrap;[\s\S]*justify-content:\s*flex-end;[\s\S]*gap:\s*10px;/.test(
    templateSource
  ),
  '统一列表模板工具栏容器必须默认不换行，并将右侧按钮组靠右排布。'
)

assert.ok(
  /\.schedule-order-pool__toolbar-group\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-wrap:\s*nowrap;[\s\S]*gap:\s*10px;/.test(
    source
  ),
  '按钮组内部必须默认不换行，避免按钮散落到第二行。'
)

assert.ok(
  /\.schedule-order-pool__header-main\s*\{[\s\S]*display:\s*grid;[\s\S]*width:\s*100%;[\s\S]*grid-template-columns:\s*minmax\(620px,\s*1fr\) auto;[\s\S]*align-items:\s*center;[\s\S]*gap:\s*12px;/.test(
    source
  ) &&
    /\.schedule-order-pool__header-actions\s*\{[\s\S]*display:\s*flex;[\s\S]*min-width:\s*0;[\s\S]*flex-wrap:\s*nowrap;[\s\S]*justify-content:\s*flex-end;/.test(
      source
    ),
  '标题栏外层容器必须占满右侧空间，动作按钮组必须继续靠右排列。'
)

assert.ok(
  /\.unified-list-template__toolbar-actions\s*\{[\s\S]*flex:\s*1 1 auto;[\s\S]*min-width:\s*0;/.test(templateSource),
  '统一列表模板动作区 form-item 必须占用剩余宽度并允许内容自适应。'
)

assert.ok(
  /<div class="unified-list-template__toolbar">[\s\S]*<slot name="actions"><\/slot>[\s\S]*<UserTableColumnSettings[\s\S]*class="unified-list-template__column-settings"/.test(
    templateSource
  ),
  '统一列表模板仍需保留默认显示字段能力，供未自定义标题栏动作的列表复用。'
)

assert.ok(
  /@media \(max-width:\s*1360px\)\s*\{[\s\S]*\.unified-list-template__query-form,[\s\S]*\.unified-list-template__toolbar[\s\S]*\{[\s\S]*flex-wrap:\s*wrap;/.test(
    templateSource
  ),
  '统一列表模板窄屏必须恢复换行，避免工具栏溢出。'
)

assert.ok(
  source.includes(':show-column-reset="false"') && columnSettingsSource.includes('v-if="showReset"'),
  '排产工单页必须隐藏显示字段配置的重置列按钮，避免与筛选动作混淆。'
)

assert.ok(
  source.includes(':show-column-settings="false"') &&
    /<UserTableColumnSettings[\s\S]*class="schedule-order-pool__header-column-settings"/.test(source) &&
    /v-if="showColumnSettings !== false"/.test(templateSource),
  '排产工单页必须隐藏筛选行默认显示字段入口，仅保留标题栏右侧显示字段。'
)

console.log('PASS: MES schedule order toolbar layout static contract')
