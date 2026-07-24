const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const auditPane = read('src/views/mes/pro/edhr/components/OperationAuditListPane.vue')

const traceDrawerMatch = batchDetail.match(
  /<el-drawer v-model="traceRecordDrawerVisible" title="追溯记录"[\s\S]*?<\/el-drawer>/
)
assert.ok(traceDrawerMatch, '批次详情必须保留追溯记录抽屉。')

const traceDrawer = traceDrawerMatch[0]
const auditTabMatch = traceDrawer.match(
  /<el-tab-pane label="操作审计" name="audit"[\s\S]*?<\/el-tab-pane>/
)
assert.ok(auditTabMatch, '追溯记录抽屉必须保留操作审计页签。')

const auditTab = auditTabMatch[0]
assert.match(
  auditTab,
  /<OperationAuditListPane[\s\S]*object-type="BATCH_EXECUTION"[\s\S]*:object-id="traceRecordBatchExecutionId"/,
  '操作审计页签必须在弹窗红框内直接挂载批次操作审计列表。'
)
assert.doesNotMatch(
  auditTab,
  /<el-button[\s\S]*openBatchOperationAudit[\s\S]*查看审计[\s\S]*<\/el-button>/,
  '操作审计页签不得只保留“查看审计”二级跳转按钮。'
)
assert.match(
  batchDetail,
  /import OperationAuditListPane from '@\/views\/mes\/pro\/edhr\/components\/OperationAuditListPane\.vue'/,
  '批次详情必须导入可复用操作审计列表组件。'
)

assert.match(auditPane, /<UnifiedListTemplate/)
assert.match(auditPane, /table-key="mes\.pro\.edhr\.operationAudit"/)
assert.match(auditPane, /EdhrOperationAuditApi\.getPage/)
assert.match(auditPane, /defineExpose\(\{\s*reload:\s*getList\s*\}\)/)
assert.doesNotMatch(auditPane, /router\.push\(\{[\s\S]*edhr-operation-audit/)

console.log('PASS: eDHR trace drawer inline operation audit standard list contract')
