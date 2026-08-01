const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApiPath = path.join(root, 'src/api/mes/pro/feedback/index.ts')
const leaderPagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

const read = (file) => fs.readFileSync(file, 'utf8')

const panel = read(panelPath)
const feedbackApi = read(feedbackApiPath)
const leaderPage = read(leaderPagePath)

assert(
  feedbackApi.includes('/mes/pro/feedback/frontline/submit') &&
    feedbackApi.includes('frontlineSubmit') &&
    feedbackApi.includes('/mes/pro/feedback/frontline/device-account/pqc/submit') &&
    feedbackApi.includes('submitFrontlinePqcInspection'),
  'PQC 检验员提交必须存在正式持久化 API wrapper。'
)

const handleValidateStart = panel.indexOf('const handleValidate = async () => {')
const handleValidateEnd = panel.indexOf('const assertFormalPayloadContext', handleValidateStart)
const handleValidateBlock = panel.slice(handleValidateStart, handleValidateEnd)
const validateIndex = handleValidateBlock.indexOf('FrontlineTemplateApi.validatePayload')
const pqcSubmitIndex = handleValidateBlock.indexOf('ProFeedbackApi.submitFrontlinePqcInspection')
const successIndex = handleValidateBlock.indexOf("message.success('已提交')")
assert(
  validateIndex >= 0 && pqcSubmitIndex > validateIndex && successIndex > pqcSubmitIndex,
  'PQC 检验员提交必须先校验模板 payload，再调用正式 PQC 提交接口，最后才提示已提交。'
)

const submitCallPattern =
  /submitFrontlinePqcInspection\s*\(|ProFeedbackApi\.submitFrontlinePqcInspection\s*\(/
assert(
  submitCallPattern.test(panel),
  'PQC 检验员提交按钮必须调用 /mes/pro/feedback/frontline/submit 或正式 PQC 提交接口。'
)

for (const token of ['pqcDraft', 'pqcPieceValues', 'rawPayload']) {
  assert(
    handleValidateBlock.includes(token) || panel.includes(token),
    `PQC 提交持久化 payload 必须保留检验员填写明细字段: ${token}`
  )
}

assert(
  leaderPage.includes('resolvePqcSubmissionContentItems') &&
    leaderPage.includes('pqcDraft') &&
    leaderPage.includes('pqcPieceValues'),
  'PQC 组长列表必须按检验员原始提交明细解析展示，不能只展示汇总。'
)

console.log('mes-frontline-pqc-submit-to-leader-chain-static PASS')
