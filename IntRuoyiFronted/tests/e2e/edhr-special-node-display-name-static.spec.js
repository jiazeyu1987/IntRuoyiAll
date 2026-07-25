const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8')

const attachmentPanelBlock = detail.slice(
  detail.indexOf('class="edhr-batch-detail__special-node-attachments"'),
  detail.indexOf('<section class="edhr-batch-detail__special-node-attachment-section"', detail.indexOf('class="edhr-batch-detail__special-node-attachments"'))
)
assert.match(
  attachmentPanelBlock,
  /resolveTaskDisplayName\(selectedSpecialNodeForEvidence\)/,
  '当前节点附件头部必须复用任务显示名称。'
)

const labelsBlock = detail.match(/const specialNodeLabels: Record<string, string> = \{([\s\S]*?)\}/)
assert(labelsBlock, '必须存在特殊节点中文名称映射。')
assert.match(labelsBlock[1], /EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT\]: '来料检报告'/, '来料检报告必须有固定中文名称。')

const displayNameBlock = detail.match(
  /const resolveTaskDisplayName = \(row: EdhrBatchExecutionTaskRespVO\) =>([\s\S]*?)const resolvePendingTaskTitle/
)
assert(displayNameBlock, '必须存在任务显示名称解析函数。')
const displayNameBody = displayNameBlock[1]
const specialNodeLabelIndex = displayNameBody.indexOf('specialNodeLabels[row.nodeType')
const reportNameIndex = displayNameBody.indexOf('row.batchRecordReportName')
assert.notEqual(specialNodeLabelIndex, -1, '任务显示名称必须接入特殊节点中文名称映射。')
assert.notEqual(reportNameIndex, -1, '任务显示名称仍需保留普通批记录报告名称。')
assert(
  specialNodeLabelIndex < reportNameIndex,
  '特殊节点标题必须优先显示来料检报告等节点业务名称，不能被空报告字段占位符覆盖。'
)

console.log('PASS: eDHR special node display name static contract')
