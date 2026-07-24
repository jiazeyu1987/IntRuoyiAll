const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/process/index.vue'), 'utf8')

assert.match(source, /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/)
assert.match(source, /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.process\.main"/)
assert.match(source, /:quick-filter-state="processQuickFilter\.state"/)
assert.match(source, /@quick-filter-query="processQuickFilter\.applyQuickFilter"/)
assert.match(source, /:columns="processColumns"/)
assert.match(source, /@column-change="saveProcessColumnConfig"/)
assert.match(source, /data-user-table-key="mes\.pro\.process\.main"[\s\S]*@header-dragend="handleProcessHeaderDragend"/)
assert.match(source, /const buildProcessPageParams/)

for (const field of ['code', 'name', 'routeList', 'workstationNames', 'status', 'remark', 'createTime', 'operation']) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`))
}
assert.doesNotMatch(source, /key:\s*'machineryQuantityTotal'/)
assert.doesNotMatch(source, /key:\s*'availableShiftCapacityTotal'/)

for (const filterField of ['code', 'name', 'routeId', 'status']) {
  assert.match(source, new RegExp(`key:\\s*'${filterField}'`))
}

assert.doesNotMatch(source, /localStorage|sessionStorage/)
console.log('PASS: mes pro process unified list template static contract')
