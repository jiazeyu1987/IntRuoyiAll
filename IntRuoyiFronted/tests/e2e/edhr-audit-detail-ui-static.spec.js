const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const fieldAuditDetailPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/FieldAuditDetailPage.vue'
)
const operationAuditPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/OperationAuditPage.vue'
)

const fieldAuditDetailSource = fs.readFileSync(fieldAuditDetailPath, 'utf8')
const operationAuditSource = fs.readFileSync(operationAuditPath, 'utf8')

assert(
  !fieldAuditDetailSource.includes('oldValueJson=') &&
    !fieldAuditDetailSource.includes('newValueJson=') &&
    !fieldAuditDetailSource.includes('oldValueHash=') &&
    !fieldAuditDetailSource.includes('newValueHash='),
  'Field audit detail main change table must not expose JSON/hash internals inline.'
)

assert(
  !fieldAuditDetailSource.includes('label="previousHash"') &&
    !fieldAuditDetailSource.includes('label="当前审计哈希 auditHash"'),
  'Field audit detail main table must move previousHash/auditHash into expandable evidence.'
)

assert(
  fieldAuditDetailSource.includes('label="变更值"') &&
    fieldAuditDetailSource.includes('type="expand"') &&
    fieldAuditDetailSource.includes('字段审计链校验通过'),
  'Field audit detail must show business change summary, expandable evidence, and readable verify copy.'
)

assert(
  !operationAuditSource.includes('label="审计Hash"') &&
    !operationAuditSource.includes('label="metadataJson"') &&
    !operationAuditSource.includes('label="previousAuditHash"') &&
    !operationAuditSource.includes('label="auditHash"'),
  'Operation audit primary table/detail must not expose audit hash or metadata as default fields.'
)

assert(
  operationAuditSource.includes('title="事件摘要"') &&
    operationAuditSource.includes('title="技术证据"') &&
    operationAuditSource.includes('审计证据') &&
    operationAuditSource.includes('empty-text="暂无操作审计记录，请输入对象类型和对象ID后查询"'),
  'Operation audit must provide readable event summary, collapsed technical evidence, and Chinese empty state.'
)

console.log('PASS: EDHR audit detail UI static contract')
