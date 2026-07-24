const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const approvalPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ApprovalPage.vue')
const source = fs.readFileSync(approvalPagePath, 'utf8')

assert(
  !source.includes('<el-table-column label="提交人" prop="submittedBy"') &&
    !source.includes('<el-table-column label="提交时间" prop="submittedAt"'),
  'Approval list must not expose submittedBy and submittedAt raw fields as primary table columns.'
)

assert(
  source.includes('label="提交信息"') &&
    source.includes('formatSubmittedAt(row.submittedAt)') &&
    source.includes('formatSubmittedBy(row.submittedBy)'),
  'Approval list must group submitter and formatted submitted time into a readable submit info column.'
)

assert(
  source.includes('label="生产上下文"') &&
    !source.includes('<el-table-column label="工单号" prop="workOrderCode"') &&
    !source.includes('<el-table-column label="批次号" prop="batchCode"') &&
    !source.includes('<el-table-column label="工序" prop="processName"') &&
    !source.includes('<el-table-column label="工作站" prop="workstationName"'),
  'Approval list must group work order, batch, process, and workstation into one production context column.'
)

assert(
  source.includes(`v-if="activeTab === 'pending'"`) &&
    source.includes('待审批操作') &&
    source.includes('只读操作'),
  'Approval list must show approve/reject actions only on pending records and keep done records read-only.'
)

assert(
  source.includes('empty-text="暂无审批记录"') &&
    source.includes('edhr-workbench__submit-time') &&
    source.includes('edhr-workbench__context-line'),
  'Approval list must expose explicit empty state and compact secondary text styles.'
)

console.log('PASS: EDHR approval list UI static contract')
