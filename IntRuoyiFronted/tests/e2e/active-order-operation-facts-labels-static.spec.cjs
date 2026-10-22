const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const panelPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const panel = fs.readFileSync(panelPath, 'utf8')

const operationTypes = [
  'OPEN',
  'VIEW',
  'SYNC',
  'CLOSE',
  'QUALITY_REJECT',
  'REEXECUTE',
  'SKIP',
  'COMPLETE',
  'COMPLETE_RELEASE_REPORT',
  'COMPLETE_PRE_RELEASE_DOSSIER',
  'ARCHIVE',
  'DOWNLOAD',
  'VERIFIED_BACKFILL',
  'CANDIDATE_SIGNATURE_COMPLETE',
  'FILL_TASK_REASSIGN',
  'REWORK_TASK_CREATED',
  'PRECHECK',
  'SUBMIT',
  'APPROVE',
  'REJECT',
  'WITHDRAW',
  'DOSSIER_UPLOAD',
  'DOSSIER_DELETE',
  'NONCONFORMANCE_REVIEW_CREATE',
  'NONCONFORMANCE_REVIEW_DISPOSE',
  'FIELD_CHANGE',
  'GOLDEN_FINGER_SUBMIT',
  'PERMISSION_EVALUATE',
  'PERMISSION_RULE_SAVE'
]

for (const operationType of operationTypes) {
  assert.match(
    panel,
    new RegExp(`\\b${operationType}:\\s*'[^']*[\\u4e00-\\u9fa5][^']*'`),
    `${operationType} 必须有中文操作名称。`
  )
}

assert.match(panel, /'创建并打开 eDHR 批次':\s*'[^']*电子批记录[^']*'/)
assert.match(panel, /'打开 eDHR 工序任务':\s*'[^']*电子批记录[^']*'/)
assert.match(panel, /'P3推送PQC生产放行':\s*'[^']*'/)
assert.doesNotMatch(panel, /return '未知操作'/, '操作事实不得显示“未知操作”。')
assert.doesNotMatch(
  panel,
  /&&\s*!\/\[A-Za-z\]\//,
  '中文操作名称包含技术缩写时仍应视为可读名称。'
)

console.log('PASS: active-order operation fact labels static contract')
