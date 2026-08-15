const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const pageFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordcelllink/index.vue')
const apiFile = resolve(process.cwd(), 'src/api/mes/pro/batchrecordcelllink/index.ts')
const page = readFileSync(pageFile, 'utf-8')
const api = readFileSync(apiFile, 'utf-8')

for (const token of [
  "const LINK_MODE_REPEAT_ROW_GROUP = 'REPEAT_ROW_GROUP'",
  'v-model="linkMode"',
  'label="重复行组"',
  'data-batch-record-repeat-row-group-mode',
  'data-repeat-row-group-candidate-list',
  'saveRepeatRowGroup',
  'BatchRecordRepeatRowGroupSaveReqVO',
  'BatchRecordRepeatRowGroupVO',
  'templateStartRowIndex',
  'repeatAreaStartRowIndex',
  'recordSequence',
  'projectionTargetCellKey',
  'routeProcessId: targetForm.value?.routeProcessId'
]) {
  assert.ok(page.includes(token) || api.includes(token), 'repeat-row group contract missing token: ' + token)
}

assert.ok(!page.includes('Array.from({ length: 4 })'), 'repeat row count must not be hard-coded as four rows')
assert.ok(!page.includes('一线提交时写入批记录'), 'configuration page must not generate records during frontline submit')
console.log('batch-record-cell-link repeat-row-group static contract passed')