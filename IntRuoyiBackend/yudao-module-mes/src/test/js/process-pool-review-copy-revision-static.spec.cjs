const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(repoRoot, relativePath))

const files = {
  reviewCopyController: 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProcessPoolReviewCopyController.java',
  reviewCopyVo: 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolReviewCopyGenerateSubmitReqVO.java',
  reviewCopyApi: 'IntRuoyiFronted/src/api/mes/pro/processpool/reviewCopy.ts',
  eventRevisionController: 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProProcessPoolEventRevisionController.java',
  eventRevisionVo: 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolEventRevisionUpdateReqVO.java',
  eventRevisionApi: 'IntRuoyiFronted/src/api/mes/pro/processpool/eventRevision.ts',
  timelineApi: 'IntRuoyiFronted/src/api/mes/pro/processpool/index.ts',
  timelinePage: 'IntRuoyiFronted/src/views/mes/pro/processpool/TimelinePage.vue',
  timelineMapper: 'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
}

for (const [name, relativePath] of Object.entries(files)) {
  assert(exists(relativePath), `${name} 必须存在。`)
}

const reviewCopyController = read(files.reviewCopyController)
const reviewCopyVo = read(files.reviewCopyVo)
const reviewCopyApi = read(files.reviewCopyApi)
const eventRevisionController = read(files.eventRevisionController)
const eventRevisionVo = read(files.eventRevisionVo)
const eventRevisionApi = read(files.eventRevisionApi)
const timelineApi = read(files.timelineApi)
const timelinePage = read(files.timelinePage)
const timelineMapper = read(files.timelineMapper)

assert(reviewCopyController.includes('/mes/pro/process-pool/review-copy'), 'F5 必须声明审核副本控制器路径。')
assert(reviewCopyController.includes('/generate-submit'), 'F5 必须提供审核副本生成提交正式接口。')
assert(reviewCopyController.includes('mes:pro-process-pool-review-copy:generate-submit'), 'F5 必须使用审核副本写权限。')
assert(!reviewCopyController.includes('mes:pro-process-pool:query'), 'F5 写接口不得复用时间轴查询权限。')
assert(reviewCopyVo.includes('fieldMappings'), 'F5 审核副本提交 VO 必须包含字段映射。')
assert(reviewCopyVo.includes('reviewerSignatureSnapshot'), 'F5 审核副本提交 VO 必须包含审核电子签名快照。')
assert(reviewCopyApi.includes("url: '/mes/pro/process-pool/review-copy/generate-submit'"), 'F5 前端 wrapper 必须调用正式审核副本接口。')
assert(/request\.post\s*<\s*number\s*>\s*\(/.test(reviewCopyApi), 'F5 前端 wrapper 必须使用 POST 并返回记录 ID。')
assert(!reviewCopyApi.includes('/timeline/'), 'F5 写 wrapper 不得复用时间轴接口。')

assert(eventRevisionController.includes('/mes/pro/process-pool/event-revision'), 'F6 必须声明原始记录修改控制器路径。')
assert(eventRevisionController.includes('/update-original'), 'F6 必须提供原始记录修改正式接口。')
assert(eventRevisionController.includes('mes:pro-process-pool:event-revision:update'), 'F6 必须使用原始记录修改写权限。')
assert(!eventRevisionController.includes('mes:pro-process-pool:query'), 'F6 写接口不得复用时间轴查询权限。')
assert(eventRevisionVo.includes('changedFields'), 'F6 原始记录修改 VO 必须包含字段级变更列表。')
assert(eventRevisionVo.includes('revisionSignatureSnapshot'), 'F6 原始记录修改 VO 必须包含重新电子签名快照。')
assert(eventRevisionApi.includes("url: '/mes/pro/process-pool/event-revision/update-original'"), 'F6 前端 wrapper 必须调用正式原始记录修改接口。')
assert(/request\.post\s*<\s*number\s*>\s*\(/.test(eventRevisionApi), 'F6 前端 wrapper 必须使用 POST 并返回 revision ID。')
assert(!eventRevisionApi.includes('/timeline/'), 'F6 写 wrapper 不得复用时间轴接口。')

assert(!/request\.(post|put|delete|upload|download)\(/.test(timelineApi), '时间轴 API 必须保持只读。')
assert(!/@click="[^"]*(create|update|submit|allocate|generate|fifo|auditCopy|revision)/i.test(timelinePage),
  '时间轴页面不得提供审核副本、原始记录修改或 FIFO 写入口。')
for (const label of ['审核副本状态', '修改历史摘要', '原始 payload', 'FIFO 分配状态']) {
  assert(timelinePage.includes(label), `时间轴详情必须展示 ${label}。`)
}

for (const token of [
  'mes_pro_process_pool_review_copy',
  'mes_pro_process_pool_event_revision',
  'auditCopyStatus',
  'auditCopySummary',
  'modificationHistorySummary',
  '原始记录已修改',
  '原始记录暂无修改'
]) {
  assert(timelineMapper.includes(token), `时间轴 mapper 必须包含 ${token}。`)
}
assert(!/createReviewCopy\s*\(|updateOriginalRecord\s*\(/.test(timelineMapper), '时间轴 mapper 不得包含写操作。')
assert(!/LEFT JOIN\s+mes_pro_process_pool_review_copy\s+review_copy/i.test(timelineMapper),
  '时间轴不得直接 JOIN 审核副本明细表，必须先按事件聚合，避免一条提交事件被多份审核副本重复展开。')
assert(/GROUP BY\s+review_copy\.tenant_id,\s*review_copy\.event_id/i.test(timelineMapper),
  '时间轴审核副本摘要必须按租户和提交事件聚合。')

console.log('PASS process-pool-review-copy-revision-static')
