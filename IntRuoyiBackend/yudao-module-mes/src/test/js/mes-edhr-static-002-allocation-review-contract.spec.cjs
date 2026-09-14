const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repo = path.resolve(__dirname, '../../../../')

function read(relativePath) {
  return fs.readFileSync(path.join(repo, relativePath), 'utf8')
}

function sliceMethod(source, signature) {
  const startAt = source.indexOf(signature)
  assert.notEqual(startAt, -1, `missing method anchor: ${signature}`)
  const bodyStart = source.indexOf('{', startAt)
  assert.notEqual(bodyStart, -1, `missing method body: ${signature}`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    const char = source[i]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(startAt, i + 1)
      }
    }
  }
  assert.fail(`unterminated method body: ${signature}`)
}

const service = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java')
const mapper = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolReportAllocationMapper.java')

const save = sliceMethod(service, 'public MesReportAllocationSnapshot save(MesReportAllocationSaveCommand command)')
const unchangedBranchStart = save.indexOf('if (before.equals(desired))')
assert.notEqual(unchangedBranchStart, -1, 'EDHR-STATIC-002: save must keep an explicit unchanged-allocation branch')
const unchangedBranch = save.slice(unchangedBranchStart, save.indexOf('int newVersion', unchangedBranchStart))
assert(unchangedBranch.includes('requiresFormalReview(current)'),
  'EDHR-STATIC-002: unchanged allocations must inspect current rows for missing reviewId before returning')
assert(unchangedBranch.includes('MesProcessPoolSubmissionReviewDO review = requireReview(event, command)')
  && unchangedBranch.includes('Long reviewId = review.getId()'),
  'EDHR-STATIC-002: unchanged first confirmation must create or reuse a formal review')
assert(unchangedBranch.includes('attachReviewToCurrentRowsByEventId'),
  'EDHR-STATIC-002: unchanged first confirmation must attach the formal review to existing current rows')
assert(unchangedBranch.indexOf('attachReviewToCurrentRowsByEventId')
  < unchangedBranch.indexOf('return buildSnapshot'),
  'EDHR-STATIC-002: no-change return must happen only after missing review rows are attached')

const requiresFormalReview = sliceMethod(service, 'private boolean requiresFormalReview(List<MesProcessPoolReportAllocationDO> current)')
assert(requiresFormalReview.includes('allocation.getReviewId() == null'),
  'EDHR-STATIC-002: formal review requirement must be driven by null allocation reviewId')

const attachReview = sliceMethod(mapper, 'default int attachReviewToCurrentRowsByEventId')
assert(attachReview.includes('.isNull(MesProcessPoolReportAllocationDO::getReviewId)'),
  'EDHR-STATIC-002: mapper attachment must only update rows still missing reviewId')
assert(attachReview.includes('.eq(MesProcessPoolReportAllocationDO::getLifecycleStatus')
  && attachReview.includes('MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT'),
  'EDHR-STATIC-002: mapper attachment must be scoped to current allocation rows')

console.log('PASS: EDHR-STATIC-002 allocation formal review static contract')
