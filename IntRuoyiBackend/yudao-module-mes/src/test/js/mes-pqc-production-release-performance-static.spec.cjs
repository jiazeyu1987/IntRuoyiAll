const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '..', '..', '..')
const servicePath = path.join(
  root,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java'
)
const mapperPath = path.join(
  root,
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderReleaseApplicationMapper.java'
)
const reviewMapperPath = path.join(
  root,
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrNonconformanceReviewMapper.java'
)

const service = fs.readFileSync(servicePath, 'utf8')
const mapper = fs.readFileSync(mapperPath, 'utf8')
const reviewMapper = fs.readFileSync(reviewMapperPath, 'utf8')

assert.match(service, /selectPqcReleasePage\(/)
assert.doesNotMatch(service, /selectListForPqcReleasePage\(/)
assert.doesNotMatch(service, /applyApprovalReadiness\(/)
assert.doesNotMatch(service, /subList\(/)
assert.match(mapper, /selectPqcReleasePage/)
assert.match(mapper, /PQC_RELEASE_PENDING/)
assert.match(mapper, /candidate_user_snapshot/)
assert.match(mapper, /REPLACE\(COALESCE\(t\.candidate_user_snapshot/)
assert.match(mapper, /tenant_id = #\{tenantId\}/)
for (const status of ['RELEASED', 'VOIDED', 'REWORKED', 'CONCESSION_RELEASED']) {
  assert.match(mapper, new RegExp(status))
}
assert.match(reviewMapper, /selectLatestBySourceIdsInternal/)
assert.match(reviewMapper, /tenantId/)

console.log('PQC production release performance contract: PASS')
