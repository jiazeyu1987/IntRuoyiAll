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

assert(
  /v-if="fileDetail\?\.needTraining && fileDetail\?\.trainingRecordAvailable"/.test(
    trainingSection
  ),
  '三流程培训证据提示必须同时受 needTraining 和 trainingRecordAvailable 控制'
)
assert(
  /data-testid="dcc-detail-training-record-evidence"/.test(trainingSection),
  '三流程培训证据提示必须提供稳定测试标识'
)
assert(
  /title="三流程培训记录已上传"/.test(trainingSection),
  '培训证据提示必须使用正式上传文案'
)
assert(
  /:description="`证据文件：\$\{fileDetail\.trainingRecordFileName \|\| '已绑定文件'\}`"/.test(
    trainingSection
  ),
  '培训证据提示必须显示后端投影的 trainingRecordFileName，不得写死文件名'
)
assert(
  /:empty-text="fileDetail\?\.needTraining && fileDetail\?\.trainingRecordAvailable \? '三流程培训记录已上传，见上方证据' : '当前版本暂无培训记录'"/.test(
    trainingSection
  ),
  '培训空状态必须区分已上传证据和普通无培训记录'
)
assert(
  !/trainingRecordFileName\s*=\s*['"][^'"]+['"]/.test(trainingSection),
  '培训证据文件名不得在模板中硬编码'
)
assert(
  !/DCC-E2E|fake|mock|placeholder|伪造|虚构/i.test(trainingSection),
  '培训证据区域不得包含测试编号、fake、mock、placeholder 或伪造证据'
)

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
