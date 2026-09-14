const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const moduleRoot = path.resolve(__dirname, '..', '..')
const serviceSource = fs.readFileSync(
  path.join(moduleRoot, 'main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionServiceImpl.java'),
  'utf8'
)
const policyTestSource = fs.readFileSync(
  path.join(moduleRoot, 'test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportRevisionPolicyTest.java'),
  'utf8'
)

test('approved production reports remain correctable through the formal revision path', () => {
  assert.doesNotMatch(
    serviceSource,
    /PRO_PROCESS_POOL_REVISION_PRODUCTION_REPORT_APPROVED_LOCKED/,
    'production report correction must not fail solely because the latest review is APPROVED'
  )
  assert.doesNotMatch(
    serviceSource,
    /STATUS_APPROVED\.equals\(latestReview\.getReviewStatus\(\)\)/,
    'APPROVED review status is not an absolute correction lock'
  )
  assert.match(
    policyTestSource,
    /void productionLeaderCanCorrectAnApprovedProductionReport\(\)[\s\S]*STATUS_APPROVED[\s\S]*assertEquals\(7011L/,
    'policy test must assert approved production reports can still be corrected'
  )
})
