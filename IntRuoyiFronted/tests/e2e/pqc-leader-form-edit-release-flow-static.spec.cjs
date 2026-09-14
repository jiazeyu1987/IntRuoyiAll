const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const readUtf8 = (absolutePath) => fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')

const page = readUtf8(path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
))
const teamLeaderApi = readUtf8(path.join(
  frontendRoot,
  'src/api/mes/pro/processpool/teamLeader.ts'
))
const timelineApi = readUtf8(path.join(
  frontendRoot,
  'src/api/mes/pro/processpool/index.ts'
))

assert.match(
  teamLeaderApi,
  /pqcFormView\?:\s*'CURRENT'\s*\|\s*'HISTORY'/,
  'PQC submission page query must carry an explicit current/history form view.'
)
assert.match(
  timelineApi,
  /activeOrderId\?:\s*number[\s\S]*released\?:\s*boolean/,
  'PQC timeline row must expose activeOrderId and released state from the backend read model.'
)
assert.match(
  page,
  /pqcFormView:\s*isPqcFormHistoryTab\.value\s*\?\s*'HISTORY'\s*:\s*activeLeaderTab\.value === 'PQC'\s*\?\s*'CURRENT'\s*:\s*undefined/,
  'PQC management/history tabs must send explicit pqcFormView instead of deriving behavior from review status.'
)
assert.match(
  page,
  /submissionReviewStatus:\s*isPqcFormHistoryTab\.value\s*\?\s*'APPROVED'\s*:\s*queryParams\.submissionReviewStatus \|\| undefined/,
  'PQC history keeps the approved review filter while current PQC forms keep all pre-release review states.'
)
assert.match(
  page,
  /const canReviewSubmission = \(row: ProcessPoolTimelineEventVO\) =>\s*!\(isProductionReportHistoryTab\.value \|\| isPqcFormHistoryTab\.value\)[\s\S]*!isProductionLeader\.value[\s\S]*!row\.released[\s\S]*Boolean\(row\.id\)/,
  'PQC current rows must keep the review button resident before release, regardless of latest review status.'
)
assert.doesNotMatch(
  page,
  /const canReviewSubmission = \(row: ProcessPoolTimelineEventVO\) =>[\s\S]{0,240}submissionReviewStatus === 'PENDING'/,
  'PQC review button must no longer be hidden by APPROVED or REJECTED review status.'
)
assert.match(
  page,
  /const canCorrectSubmission = \(row: ProcessPoolTimelineEventVO\) =>\s*!\(isProductionReportHistoryTab\.value \|\| isPqcFormHistoryTab\.value\)[\s\S]*\(isProductionLeader\.value \|\| !row\.released\)[\s\S]*Boolean\(row\.id\)/,
  'PQC current rows must keep the correction button resident before release.'
)
assert.doesNotMatch(
  page,
  /const canCorrectSubmission = \(row: ProcessPoolTimelineEventVO\) =>[\s\S]{0,240}submissionReviewStatus === 'REJECTED'/,
  'PQC correction button must no longer require a rejected review.'
)
assert.match(
  page,
  /ElMessage\.error\('已放行的PQC表单只能在历史中查看'\)/,
  'The correction guard must explain that released PQC forms are historical and not editable.'
)
assert.match(
  page,
  /correctionForm\.correctionMode === 'PQC'[\s\S]*correctProcessPoolPqcInspection\(buildPqcCorrectionRequest\(\)\)/,
  'PQC correction must submit the formal PQC correction contract instead of the production report contract.'
)

console.log('PASS: PQC leader form edit/review/release frontend static contract')
