const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = resolve(__dirname, '../..')
const pagePath = resolve(
  repoRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const source = readFileSync(pagePath, 'utf8')

assert.match(
  source,
  /v-if="canReviewSubmission\(row\)"[\s\S]*:data-team-leader-review-event-id="String\(row\.id\)"/,
  '复核按钮必须只对待复核行显示，不能对 APPROVED/REJECTED 终态行重复暴露'
)

assert.match(
  source,
  /v-if="canCorrectSubmission\(row\)"[\s\S]*:data-team-leader-correction-event-id="String\(row\.id\)"/,
  '修改按钮必须通过 canCorrectSubmission 控制可见性'
)

assert.match(
  source,
  /:data-team-leader-correction-event-id="String\(row\.id\)"[\s\S]*@click="openCorrection\(row\)"[\s\S]*>\s*修改\s*<\/el-button>/,
  '行级修改按钮必须打开正式原始记录修改入口'
)

assert.match(
  source,
  /const canReviewSubmission = \(row: ProcessPoolTimelineEventVO\) =>[\s\S]*!\(isProductionReportHistoryTab\.value \|\| isPqcFormHistoryTab\.value\)[\s\S]*!row\.submissionReviewStatus \|\| row\.submissionReviewStatus === 'PENDING'/,
  '待复核判断必须只允许空状态或 PENDING'
)

assert.match(
  source,
  /const canCorrectSubmission = \(row: ProcessPoolTimelineEventVO\) =>[\s\S]*!\(isProductionReportHistoryTab\.value \|\| isPqcFormHistoryTab\.value\)[\s\S]*isProductionLeader\.value \|\| row\.submissionReviewStatus === 'REJECTED'/,
  '生产组长报工行允许直接修改；PQC 仍只允许 REJECTED 后修改'
)

assert.match(
  source,
  /if \(!canReviewSubmission\(event\)\) \{[\s\S]*ElMessage\.error\('已完成复核的提交不能重复复核'\)[\s\S]*return[\s\S]*\}/,
  'openReview 必须二次阻断终态重复复核'
)

assert.match(
  source,
  /if \(reviewForm\.reviewStatus === 'REJECTED' && !reviewForm\.reviewRemark\.trim\(\)\) \{[\s\S]*ElMessage\.error\('退回复核必须填写复核说明'\)[\s\S]*return[\s\S]*\}/,
  '退回提交必须在前端要求复核说明'
)

assert.match(
  source,
  /if \(!canCorrectSubmission\(event\)\) \{[\s\S]*ElMessage\.error\('只有生产报工或复核不正确的提交可以修改'\)[\s\S]*return[\s\S]*\}/,
  'openCorrection 必须二次阻断不允许修改的行'
)
