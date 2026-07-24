const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/feedback/index.vue'), 'utf8')

assert.match(source, /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/)
assert.match(source, /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.feedback\.main"/)
assert.match(source, /:quick-filter-state="feedbackQuickFilter\.state"/)
assert.match(source, /@quick-filter-query="feedbackQuickFilter\.applyQuickFilter"/)
assert.match(source, /:columns="feedbackColumns"/)
assert.match(source, /@column-change="saveFeedbackColumnConfig"/)
assert.match(source, /data-user-table-key="mes\.pro\.feedback\.main"[\s\S]*@header-dragend="handleFeedbackHeaderDragend"/)
assert.match(source, /const buildFeedbackPageParams/)

for (const field of ['excelProductCode', 'excelProductName', 'excelProcessCode', 'excelProcessName', 'excelDepartment', 'excelEmployeeNo', 'excelEmployeeName', 'excelSectionLeader', 'feedbackQuantity', 'excelFeedbackTime']) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`))
  assert.match(source, new RegExp(`isFeedbackColumnVisible\\('${field}'\\)`))
}

for (const filterField of ['id', 'code', 'type', 'status', 'feedbackTime']) {
  assert.match(source, new RegExp(`key:\\s*'${filterField}'`))
}

assert.doesNotMatch(source, /localStorage|sessionStorage/)
console.log('PASS: mes pro feedback unified list template static contract')
