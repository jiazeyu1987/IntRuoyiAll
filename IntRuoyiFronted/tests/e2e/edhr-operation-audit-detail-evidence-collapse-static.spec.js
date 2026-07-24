const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const operationAuditPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/OperationAuditPage.vue'
)
const source = fs.readFileSync(operationAuditPath, 'utf8')

assert(
  source.includes('const operationAuditDetailTechnicalEvidenceNames = ref<string[]>([])'),
  '操作审计详情技术证据折叠区必须默认收起。'
)

assert(
  source.includes('class="edhr-operation-audit__technical-evidence"') &&
    source.includes('<el-collapse-item title="技术证据" name="technical-evidence">'),
  '操作审计详情必须把审计 Hash 和 metadata 移入技术证据折叠区。'
)

const technicalEvidenceIndex = source.indexOf('edhr-operation-audit__technical-evidence')
const beforeHashIndex = source.indexOf('detail.beforeSummaryHash')
const metadataIndex = source.indexOf('detail.metadataJson')

assert(
  technicalEvidenceIndex > -1 &&
    beforeHashIndex > technicalEvidenceIndex &&
    metadataIndex > technicalEvidenceIndex,
  '操作审计 Hash 与 metadata 必须渲染在技术证据区内。'
)

assert(
  !/<el-descriptions title="审计证据"[\s\S]*?detail\.beforeSummaryHash/.test(source),
  '审计证据不能继续作为默认展开的顶层详情块。'
)

assert(
  source.includes('title="事件摘要"') &&
    source.includes('label="事件ID"') &&
    source.includes('label="请求ID"') &&
    source.includes('label="失败说明"') &&
    source.includes('empty-text="暂无操作审计记录，请输入对象类型和对象ID后查询"'),
  '操作审计详情必须保留默认可见事件摘要和中文空态。'
)

assert(
  !/mock|降级|静默跳过/.test(source),
  '操作审计详情优化不得引入 mock、降级或静默跳过。'
)

console.log('PASS: EDHR operation audit detail evidence collapse static contract')
