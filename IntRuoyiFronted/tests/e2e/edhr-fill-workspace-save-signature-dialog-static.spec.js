const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(!source.includes('edhr-fill-workspace__save-signature-dialog'), '保存草稿点击后不得再弹保存前确认或签名弹框。')
assert.ok(!source.includes('fieldAuditSignatureDialogVisible'), '保存草稿不得保留保存前签名弹框状态。')
assert.ok(!source.includes('fieldAuditSignatureForm'), '保存草稿不得要求输入签名密码。')
assert.ok(!source.includes('确认保存'), '保存草稿不得出现二次确认按钮。')
assert.ok(!source.includes('openFieldAuditSignatureDialog'), '保存草稿按钮不得先打开确认弹框。')

assert.ok(
  /@click="handleSaveFieldAuditChanges"[\s\S]{0,500}保存草稿/.test(source),
  '左侧“保存草稿”按钮必须点击即调用真实保存函数。'
)

assert.ok(
  /@click="handleSaveFieldAuditChanges"[\s\S]{0,500}保存变更/.test(source),
  '待保存变更区“保存变更”按钮必须点击即调用真实保存函数。'
)

const saveCallStart = source.indexOf('const handleSaveFieldAuditChanges = async () =>')
const saveCallEnd = source.indexOf('const submitReviewAssigneeOptions', saveCallStart)
assert.ok(saveCallStart > -1 && saveCallEnd > saveCallStart, '保存草稿真实保存函数必须存在。')
const saveCallSource = source.slice(saveCallStart, saveCallEnd)

assert.ok(saveCallSource.includes('saveEdhrFieldChanges({'), '保存草稿必须调用真实保存接口。')
assert.ok(!saveCallSource.includes('signature:'), '保存草稿接口请求不得携带电子签名密码对象。')
assert.ok(saveCallSource.includes("showFillActionResultDialog('save-success')"), '保存成功后仍必须显示“已保存”大弹框。')
assert.ok(
  source.includes('const fieldAuditSaveGateError = computed(() => fieldAuditOpenGateError.value)'),
  '保存草稿按钮只能受结构性门禁控制，不得因为原因未填写而禁用。'
)
assert.ok(
  source.includes("const DEFAULT_FIELD_AUDIT_DRAFT_REASON_CATEGORY: EdhrFieldChangeReasonCategory = 'OPERATOR_ENTRY'"),
  '保存草稿必须内置最小审计原因分类，确保点击即可保存。'
)
assert.ok(
  source.includes("const DEFAULT_FIELD_AUDIT_DRAFT_REASON_TEXT = '保存草稿'"),
  '保存草稿必须内置最小审计原因说明，确保点击即可保存。'
)
assert.ok(
  saveCallSource.includes('reasonCategory: resolveFieldAuditDraftReasonCategory()') &&
    saveCallSource.includes('reasonText: resolveFieldAuditDraftReasonText()'),
  '保存草稿接口必须使用自动审计原因，不得要求用户先填写原因。'
)

console.log('PASS: EDHR fill workspace draft save direct static contract')
