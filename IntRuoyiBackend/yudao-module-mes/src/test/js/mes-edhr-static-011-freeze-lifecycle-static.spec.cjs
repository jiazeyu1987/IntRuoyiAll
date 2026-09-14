const assert = require('assert/strict')
const fs = require('fs')
const path = require('path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const readModule = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const service = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java'
)
const mapper = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrNonconformanceReviewMapper.java'
)

assert.match(service, /captureWorkOrderExternalFreezeAtReviewStart/)
assert.match(service, /recomputeWorkOrderTemporaryFreeze/)
assert.match(service, /selectFreezeLifecycleByWorkOrderId/)
assert.doesNotMatch(
  service,
  /requireWorkOrderUpdate\([^;]+review\.getPreviousWorkOrderTemporaryFrozen\(\)[^;]*\);/s,
  'dispose must not directly restore one review historical freeze boolean'
)
assert.doesNotMatch(
  mapper,
  /selectOriginalExternalFreezeSnapshotCountByWorkOrderId|NOT\s+EXISTS[\s\S]+earlier/i,
  'freeze lifecycle must not select the work-order historical first review as the external freeze source'
)
assert.match(mapper, /selectFreezeLifecycleByWorkOrderId/)
assert.match(mapper, /orderByAsc\(MesProEdhrNonconformanceReviewDO::getFrozenAt\)/)
assert.match(mapper, /orderByAsc\(MesProEdhrNonconformanceReviewDO::getId\)/)

console.log('mes-edhr-static-011-freeze-lifecycle-static: PASS')
