const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const signaturePagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/SignaturePage.vue'
)

const source = fs.readFileSync(signaturePagePath, 'utf8')

assert(
  !source.includes('label="FIELD_CHANGE"') &&
    !source.includes('label="FORM_REVIEW"') &&
    !source.includes('label="SUBMIT"'),
  'Signature action filter must not expose raw action enum labels.'
)

assert(
  source.includes('label="字段变更"') &&
    source.includes('label="表单复核"') &&
    source.includes('label="提交审批"'),
  'Signature action filter must use readable Chinese business labels.'
)

assert(
  !source.includes('label="时间审计Hash"'),
  'Signature list must not keep selectedTimeAuditHash as a primary table column.'
)

assert(
  source.includes('type="expand"') &&
    source.includes('签名时间证据') &&
    source.includes('selectedTimeAuditHash'),
  'Signature time audit evidence must remain available in an expandable evidence section.'
)

assert(
  source.includes('label="签名动作"') &&
    source.includes('formatSignatureAction(row.actionType)') &&
    source.includes('formatSignatureSignedAt'),
  'Signature list must show business action labels and formatted signature time.'
)

assert(
  source.includes('empty-text="暂无签名记录"') &&
    source.includes('loadError') &&
    source.includes('v-loading="loading"'),
  'Signature list must keep explicit empty, error, and loading states.'
)

console.log('PASS: EDHR signature page UI static contract')
