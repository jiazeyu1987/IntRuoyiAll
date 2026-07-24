const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/detail/index.vue')
const presentationPath = path.join(repoRoot, 'src/views/dcc/controlled-file/detail/presentation.ts')
const packageJsonPath = path.join(repoRoot, 'package.json')

const detailPage = fs.readFileSync(detailPagePath, 'utf8')
const presentation = fs.readFileSync(presentationPath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const extractBetween = (source, startToken, endToken) => {
  const startIndex = source.indexOf(startToken)
  const endIndex = source.indexOf(endToken, startIndex + startToken.length)
  assert(startIndex >= 0 && endIndex > startIndex, `无法提取 ${startToken} 到 ${endToken} 内容`)
  return source.slice(startIndex, endIndex)
}

const trainingSection = extractBetween(
  detailPage,
  'data-testid="dcc-detail-training-section"',
  '</ContentWrap>'
)
const trainingSummaryHelper = extractBetween(
  presentation,
  'export const getDetailTrainingAssignmentSummary',
  'export const isVersionHistoryVisibleToReader'
)

assert(
  packageJson.scripts['e2e:dcc:detail-training-summary:static'] ===
    'node tests/e2e/dcc-detail-training-summary-static.spec.js',
  'package.json 必须提供 e2e:dcc:detail-training-summary:static 脚本'
)

assert(
  presentation.includes('export const getDetailTrainingAssignmentSummary'),
  '详情页必须通过 getDetailTrainingAssignmentSummary 基于现有字段生成培训摘要'
)

assert(trainingSection.includes('label="部门"'), '培训表必须保留部门列')
assert(trainingSection.includes('label="受训人"'), '培训表必须保留受训人列')
assert(trainingSection.includes('label="培训摘要"'), '培训表必须新增培训摘要列')

const removedColumns = ['培训状态', '累计时长', '可确认', '部门状态', '确认时间']
for (const label of removedColumns) {
  assert(!trainingSection.includes(`label="${label}"`), `培训表不应继续拆散显示列：${label}`)
}

const summaryTokens = [
  'progressText',
  'eligibilityLabel',
  'departmentStatusLabel',
  'acknowledgedAtText'
]
for (const token of summaryTokens) {
  assert(trainingSection.includes(token), `培训摘要模板必须显示 ${token}`)
  assert(presentation.includes(token), `培训摘要 helper 必须生成 ${token}`)
}

const existingFieldTokens = [
  'row.status',
  'row.accumulatedViewSeconds',
  'row.requiredViewSeconds',
  'row.eligibleToAcknowledge',
  'row.trainingStatus',
  'row.acknowledgedAt'
]
for (const token of existingFieldTokens) {
  assert(
    presentation.includes(token) || detailPage.includes(token),
    `培训摘要必须继续使用现有真实字段：${token}`
  )
}

const behaviorHooks = [
  'flattenedTrainingAssignments',
  'getPendingTrainingAssignments',
  'handleAcknowledgeTraining',
  'openApplicantTrainingRecordDialog'
]
for (const hook of behaviorHooks) {
  assert(detailPage.includes(hook), `详情页培训行为必须保留：${hook}`)
}

const forbiddenTerms = ['mock', 'placeholder data', 'fallback', '降级', '吞异常']
for (const term of forbiddenTerms) {
  assert(!detailPage.toLowerCase().includes(term.toLowerCase()), `详情页培训摘要不得引入 ${term}`)
  assert(!trainingSummaryHelper.toLowerCase().includes(term.toLowerCase()), `培训摘要 helper 不得引入 ${term}`)
}

console.log('DCC detail training summary static contract passed.')
