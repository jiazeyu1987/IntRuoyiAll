const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const approvalDetailPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/ApprovalDetailPage.vue'
)

const source = fs.readFileSync(approvalDetailPath, 'utf8')

assert(
  !source.includes('<el-descriptions-item label="流程实例">{{ detail.processInstanceId }}</el-descriptions-item>'),
  'Approval detail summary must not expose processInstanceId as a primary decision field.'
)

assert(
  source.includes('审批证据') && source.includes('detail.processInstanceId'),
  'Approval detail must keep process instance evidence in a dedicated evidence section.'
)

assert(
  !source.includes('<el-table-column label="事件" prop="eventType"') &&
    !source.includes('<el-table-column label="流程任务" prop="bpmTaskId"') &&
    !source.includes('<el-table-column label="处理时间" prop="occurredAt"'),
  'Approval tracking table must not expose raw event, BPM task, or occurredAt fields as primary columns.'
)

assert(
  source.includes('formatTrackingEvent(row.eventType)') &&
    source.includes('formatApprovalDetailTime(row.occurredAt)') &&
    source.includes('追踪证据'),
  'Approval tracking table must show readable events/time and move technical evidence into expandable rows.'
)

assert(
  !source.includes('<el-table-column label="动作" prop="actionType"') &&
    !source.includes('<el-table-column label="签名时间" prop="signedAt"') &&
    !source.includes('label="流程任务" prop="bpmTaskId"'),
  'Approval signature table must not expose raw actionType, signedAt, or BPM task as primary columns.'
)

assert(
  source.includes('formatSignatureAction(row.actionType)') &&
    source.includes('formatApprovalDetailTime(') &&
    source.includes('签名确认') &&
    source.includes('签名时间证据') &&
    source.includes('selectedTimeAuditHash'),
  'Approval signature table must show business action/time summary and keep time audit evidence expandable.'
)

assert(
  source.includes('empty-text="暂无追踪事件"') &&
    source.includes('empty-text="暂无签名记录"'),
  'Approval detail tracking and signature tables must expose explicit empty states.'
)

console.log('PASS: EDHR approval detail UI static contract')
