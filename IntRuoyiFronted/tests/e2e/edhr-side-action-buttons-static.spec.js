const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')
const readItemBlock = (key) => {
  const match = detail.match(new RegExp(`key:\\s*'${key}'[\\s\\S]*?disabled:\\s*executionRequired`))
  assert(match, `缺少 ${key} 红框按钮配置块。`)
  return match[0]
}

const signatureBlock = readItemBlock('signature')
const approvalBlock = readItemBlock('approval')
const archiveBlock = readItemBlock('single-archive')

assert(
  signatureBlock.includes("path: '/signature-governance/batch-signatures'") &&
    signatureBlock.includes('executionId'),
  '签名记录按钮必须打开独立签名记录页并携带当前执行记录。'
)

assert(
  archiveBlock.includes("path: '/mes/pro/feedback/edhr-execution/form'") &&
    archiveBlock.includes("viewMode: 'tracking'") &&
    archiveBlock.includes('executionId'),
  'single archive action must open the retained execution form tracking view.'
)

assert(
  approvalBlock.includes("path: '/mes/pro/feedback/edhr-approval/detail'") &&
    /buildSelectedProcessEvidenceQuery\(\{[\s\S]*?id:\s*executionId[\s\S]*?focus:\s*'approval'/.test(approvalBlock),
  '审批记录按钮必须打开当前执行记录的审批详情，并携带当前工序上下文。'
)

assert(
  !signatureBlock.includes("path: '/mes/pro/feedback/edhr-execution/detail'"),
  '右侧签名记录按钮不得继续引用已删除的执行详情页。'
)

assert(
  !archiveBlock.includes("path: '/mes/pro/feedback/edhr-execution/detail'"),
  '右侧单表归档按钮不得继续引用已删除的执行详情页。'
)

assert(
  !detail.includes("path: '/mes/pro/feedback/edhr-execution/detail'"),
  '批次证据入口不得保留已删除的执行详情路由。'
)

console.log('edhr side action buttons static contract passed')
