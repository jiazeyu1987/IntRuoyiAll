const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(__dirname, '../../src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const historyBlockMatch = source.match(
  /<div class="schedule-order-pool__feedback-history">[\s\S]*?<\/UnifiedListTemplate>\s*<\/div>/
)

assert(
  historyBlockMatch,
  '历史报工明细必须使用 UnifiedListTemplate 标准模板包裹表格。'
)

const historyBlock = historyBlockMatch[0]

assert(
  historyBlock.includes('table-key="mes.pro.scheduleOrder.feedbackHistory"'),
  '历史报工明细标准模板必须使用稳定 table-key 以保存列宽配置。'
)

assert(
  historyBlock.includes(':show-query-form="false"'),
  '历史报工明细只显示表格，不展示标准模板筛选栏、字段工具栏或操作区。'
)

assert(
  historyBlock.includes(':total="0"'),
  '历史报工明细标准模板必须隐藏分页，仅保留表格。'
)

assert(
  /<el-table[\s\S]*data-user-table-column-explicit[\s\S]*data-user-table-key="mes\.pro\.scheduleOrder\.feedbackHistory"[\s\S]*@header-dragend="handleFeedbackHistoryHeaderDragend"/.test(
    historyBlock
  ),
  '历史报工明细表格必须声明可拖拽列宽并绑定列宽保存处理。'
)

assert(
  historyBlock.includes("getFeedbackHistoryColumnWidthString('code', 160)") &&
    historyBlock.includes("getFeedbackHistoryColumnWidthString('feedbackTime', 170)") &&
    historyBlock.includes("getFeedbackHistoryColumnWidthString('feedbackQuantity', 110)") &&
    historyBlock.includes("getFeedbackHistoryColumnWidthString('qualifiedQuantity', 100)") &&
    historyBlock.includes("getFeedbackHistoryColumnWidthString('feedbackUserNickname', 120)"),
  '历史报工明细所有业务列必须从反馈历史列宽配置读取宽度。'
)

assert(
  !/<el-table[\s\S]*?row\.feedbackHistoryList[\s\S]*?<\/el-table>\s*<\/div>\s*<\/template>/.test(
    historyBlock.replace(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/, '')
  ),
  '历史报工明细不得保留标准模板外的旧 el-table 实现。'
)

console.log('PASS: schedule order feedback history standard list static contract')
