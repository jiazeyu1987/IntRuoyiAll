const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(repoRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const template = panel.slice(0, panel.indexOf('<script'))
const style = panel.slice(panel.indexOf('<style'))

assert.doesNotMatch(
  template,
  /data-pqc-submit-receipt|frontline-pqc-submit-receipt/,
  'PQC 页面不能再渲染截图红框里的正式提交成功回执块。'
)
assert.doesNotMatch(
  style,
  /\.frontline-pqc-submit-receipt\b/,
  '删除回执块后不应保留红框浮层样式。'
)

const submitButton = template.match(
  /<button\s+class="frontline-pqc-submit-button"[\s\S]*?<\/button>/
)?.[0]
assert.ok(submitButton, 'PQC 提交按钮必须保留。')
assert.doesNotMatch(
  submitButton,
  /pqcSubmitReceipt|提交成功/,
  'PQC 明确成功后不能用成功回执永久禁用提交按钮。'
)
assert.match(
  submitButton,
  /:disabled="payloadLoading \|\| pqcSubmitResultUncertain"/,
  'PQC 提交按钮只应在提交中或结果不确定时锁定。'
)

const resetButton = template.match(
  /<button\s+class="frontline-pqc-reset-button"[\s\S]*?<\/button>/
)?.[0]
assert.ok(resetButton, 'PQC 重填按钮必须保留。')
assert.doesNotMatch(
  resetButton,
  /pqcSubmitReceipt/,
  'PQC 明确成功后不能用成功回执永久禁用重填按钮。'
)

const resetHandler = panel.match(
  /const resetPqcSubmissionDraft\s*=\s*\(\)\s*=>\s*\{[\s\S]*?(?=\nconst handleResetPqc)/
)?.[0]
assert.ok(resetHandler, 'PQC 成功提交后必须有独立的单次提交草稿复位函数。')
for (const required of [
  'delete pqcPieceValues[stateKey]',
  'pqcPieceDraftValues.value = []',
  'pqcDraft.scrapQuantity = undefined',
  'pqcDraft.defectDescription = undefined',
  'payloadPreview.value = undefined'
]) {
  assert.ok(resetHandler.includes(required), `PQC 成功后复位必须覆盖：${required}`)
}

const confirmStart = panel.indexOf('const handleConfirmPqcSubmit = async () => {')
const confirmEnd = panel.indexOf('const assertFormalPayloadContext', confirmStart)
const confirmBlock = panel.slice(confirmStart, confirmEnd)
assert.ok(confirmStart >= 0 && confirmEnd > confirmStart, 'PQC 确认提交函数必须存在。')
assert.doesNotMatch(
  confirmBlock.split('\n').slice(0, 8).join('\n'),
  /pqcSubmitReceipt\.value/,
  'PQC 明确成功回执不能阻止用户再次点击提交。'
)
const submitIndex = confirmBlock.indexOf('await ProFeedbackApi.submitFrontlinePqcInspection')
const resetIndex = confirmBlock.indexOf('resetPqcSubmissionDraft()')
assert.ok(submitIndex >= 0, 'PQC 确认提交必须继续调用正式提交接口。')
assert.ok(
  resetIndex > submitIndex,
  'PQC 草稿只能在正式提交接口明确成功后复位。'
)
const finallyBlock = confirmBlock.match(/finally\s*\{[\s\S]*?\}/)?.[0] || ''
assert.doesNotMatch(
  finallyBlock,
  /resetPqcSubmissionDraft|pqcSubmitReceipt/,
  'PQC 失败或响应不确定时不得在 finally 清空草稿或伪造成功状态。'
)

const recoverStart = panel.indexOf('const recoverPqcSubmitReceiptAfterUncertainError')
const recoverEnd = panel.indexOf('const handleConfirmPqcSubmit', recoverStart)
const recoverBlock = panel.slice(recoverStart, recoverEnd)
assert.match(
  recoverBlock,
  /pqcSubmitReceipt\.value = recoveredReceipt/,
  '响应不确定但只读确认已提交时，仍要保留正式回执锁定同一任务。'
)
assert.match(
  recoverBlock,
  /pqcSubmitResultUncertain\.value = true/,
  '只读确认失败时，仍要进入结果不确定锁定态防止重复写入。'
)

console.log('PASS: frontline PQC continuous submit static contract')
