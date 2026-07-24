const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const fieldAuditPath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/FieldAuditPage.vue')

const fieldAuditSource = fs.readFileSync(fieldAuditPath, 'utf8')

assert(
  fieldAuditSource.includes('label="变更值"') && fieldAuditSource.includes('type="expand"'),
  'Field audit list must keep the main change summary and an expandable evidence panel.'
)

assert(
  !fieldAuditSource.includes('oldValueJson=') &&
    !fieldAuditSource.includes('newValueJson=') &&
    !fieldAuditSource.includes('oldValueHash=') &&
    !fieldAuditSource.includes('newValueHash=') &&
    !fieldAuditSource.includes('previousHash='),
  'Field audit main table must not expose JSON or hash internals inline.'
)

assert(
  fieldAuditSource.includes('字段审计链校验通过') && fieldAuditSource.includes('共校验'),
  'Field audit verify alert must become user-friendly and count-based.'
)

console.log('PASS: EDHR UI optimization static contract')
