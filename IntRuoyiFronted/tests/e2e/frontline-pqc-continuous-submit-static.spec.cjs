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
assert.match(
  submitButton,
  /:disabled="isPqcSubmitBlocked"/,
  'PQC 提交按钮只应在提交中或结果不确定时锁定，成功/失败后必须恢复连续提交。'
)

const resetButton = template.match(
  /<button\s+class="frontline-pqc-reset-button"[\s\S]*?<\/button>/
)?.[0]
assert.ok(resetButton, 'PQC 重填按钮必须保留。')
assert.match(
  resetButton,
  /:disabled="payloadLoading \|\| pqcSubmitResultUncertain"/,
  'PQC 重填按钮应与提交按钮使用同一连续提交锁定边界。'
)
assert.match(
  panel,
  /const isPqcSubmitBlocked = computed\(\(\) =>[\s\S]*payloadLoading\.value[\s\S]*pqcSubmitResultUncertain\.value/,
  'PQC 提交按钮计算锁定边界必须包含提交中和结果不确定。'
)

assert.doesNotMatch(
  panel,
  /pqcSubmitReceipt/,
  'PQC 页面不能保留回执状态变量，否则成功回执仍可能锁死下一次提交。'
)

const resetHandler = panel.match(
  /const resetPqcSubmissionDrafts\s*=\s*\(submittedPqcTaskIds: number\[\] = \[\]\)\s*=>\s*\{[\s\S]*?(?=\nconst resetPqcSubmissionDraft)/
)?.[0]
assert.ok(resetHandler, 'PQC 成功提交后必须有独立的批量提交草稿复位函数。')
for (const required of [
  'clearPqcPieceValues()',
  'pqcDraft.scrapQuantity = undefined',
  'pqcDraft.defectDescription = undefined',
  'payloadPreview.value = undefined'
]) {
  assert.ok(resetHandler.includes(required), `PQC 成功后复位必须覆盖：${required}`)
}

const taskRotationHandler = panel.match(
  /const markPqcTasksSubmittedAndSelectNext\s*=\s*\(submittedPqcTaskIds: number\[\]\)\s*=>\s*\{[\s\S]*?(?=\nconst markPqcTaskSubmittedAndSelectNext)/
)?.[0]
assert.ok(taskRotationHandler, 'PQC 成功后必须更新当前任务状态并选择下一条待执行任务。')
const taskStatusUpdateHelper = panel.match(
  /const updatePqcSubmittedTasksInProcess\s*=\s*\([\s\S]*?(?=\nconst syncPqcSubmittedTasksInProcessOptions)/
)?.[0]
assert.ok(taskStatusUpdateHelper, 'PQC 成功后必须通过正式 helper 更新已提交任务状态。')
assert.match(
  taskStatusUpdateHelper,
  /taskStatus: 'SUBMITTED' as FrontlinePqcTaskStatus/,
  '已提交任务必须从待执行选项中移除。'
)
assert.match(
  taskRotationHandler,
  /updatePqcSubmittedTasksInProcess\(process, submittedTaskIds\)/,
  '任务轮换必须先更新当前任务状态，再选择下一条待执行任务。'
)
assert.match(taskRotationHandler, /getDefaultPqcTaskOption\(updatedProcess\)/, '成功后必须尝试选择下一条待执行任务。')

const confirmStart = panel.indexOf('const handleConfirmPqcSubmit = async () => {')
const confirmEnd = panel.indexOf('const assertFormalPayloadContext', confirmStart)
const confirmBlock = panel.slice(confirmStart, confirmEnd)
assert.ok(confirmStart >= 0 && confirmEnd > confirmStart, 'PQC 确认提交函数必须存在。')
assert.doesNotMatch(
  confirmBlock,
  /pqcSubmitReceipt\.value = await ProFeedbackApi\.submitFrontlinePqcInspection/,
  'PQC 明确成功响应不能写入恢复回执锁。'
)
const submitIndex = confirmBlock.indexOf('await ProFeedbackApi.submitFrontlinePqcInspection')
const resetIndex = confirmBlock.indexOf('resetPqcSubmissionDrafts(submitPayloads.map((payload) => payload.pqcTaskId))')
assert.ok(submitIndex >= 0, 'PQC 确认提交必须继续调用正式提交接口。')
assert.ok(
  resetIndex > submitIndex,
  'PQC 草稿只能在正式提交接口明确成功后复位。'
)
assert.match(
  confirmBlock,
  /submitPayloads = buildPqcInspectionSubmitPayloads\(\)[\s\S]*for \(const submitPayload of submitPayloads\)/,
  'PQC 确认提交必须为当前工序全部检验方法构建并提交 payload。'
)
const finallyBlock = confirmBlock.match(/finally\s*\{[\s\S]*?\}/)?.[0] || ''
assert.doesNotMatch(
  finallyBlock,
  /resetPqcSubmissionDraft/,
  'PQC 失败或响应不确定时不得在 finally 清空草稿或伪造成功状态。'
)

const recoverStart = panel.indexOf('const recoverPqcSubmitReceiptAfterUncertainError')
const recoverEnd = panel.indexOf('const handleConfirmPqcSubmit', recoverStart)
const recoverBlock = panel.slice(recoverStart, recoverEnd)
assert.match(
  recoverBlock,
  /resetPqcSubmissionDraft\(recoveredReceipt\.pqcTaskId\)/,
  '响应不确定但只读确认已提交时，应按成功提交处理并进入下一次独立提交。'
)
assert.match(
  recoverBlock,
  /pqcSubmitResultUncertain\.value = true/,
  '只读确认失败时，仍要进入结果不确定锁定态防止重复写入。'
)

console.log('PASS: frontline PQC continuous submit static contract')
