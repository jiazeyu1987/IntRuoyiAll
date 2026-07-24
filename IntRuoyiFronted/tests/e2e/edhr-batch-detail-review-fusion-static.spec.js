const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const reviewPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionReviewPage.vue')
const routerPath = path.join(repoRoot, 'src', 'router', 'modules', 'remaining.ts')

const detail = fs.readFileSync(detailPath, 'utf8')
const review = fs.readFileSync(reviewPath, 'utf8')
const router = fs.readFileSync(routerPath, 'utf8')

assert(
  detail.includes("getEdhrBatchReviewTimeline"),
  '融合详情页必须加载 review-timeline 作为工序复盘数据源'
)
assert(
  detail.includes("EdhrExecutionReadonlyForm"),
  '融合详情页必须直接承载已填写批记录只读表单'
)
assert(
  detail.includes("工序复盘") && detail.includes("批次级信息"),
  '融合详情页必须同时展示工序复盘主线和批次级无工序信息'
)
assert(
  !detail.includes("工序任务索引") &&
    detail.includes("已填写表单") &&
    detail.includes('aria-label="已填写批记录"') &&
    !detail.includes('class="edhr-batch-detail__table"') &&
    !detail.includes("edhr-batch-detail__process-group"),
  '工序复盘必须聚焦已填写批记录和证据链，不能保留旧工序任务索引或割裂详情表格'
)
assert(
  detail.includes("selectedExecution") && detail.includes("executionReviews"),
  '融合详情页必须支持按工序选择已填写执行记录'
)
assert(
  detail.includes("RELEASE_VIRTUAL_PROCESS") &&
    detail.includes("label: '放行'") &&
    detail.includes("selectReleaseProcess") &&
    detail.includes("放行参数") &&
    detail.includes("handleReleasePrecheck") &&
    detail.includes("handleGenerateArchive") &&
    !detail.includes('<section class="edhr-batch-detail__closing"'),
  '放行、关闭和最终归档必须归属到左侧最后一个放行虚拟工序'
)
assert(
  !detail.includes("管理后台工作区"),
  '融合后不应在详情页保留割裂的管理后台工作区'
)
assert(
  router.includes("BatchExecutionReviewPage.vue"),
  '复盘路由必须使用具备独立 keep-alive 名称的融合壳组件'
)
assert(
  review.includes("BatchExecutionDetailPage.vue") &&
    review.includes("defineOptions({ name: 'MesProEdhrBatchExecutionReview' })"),
  '复盘兼容壳必须继续复用详情实现，并提供独立 keep-alive 名称'
)

console.log('edhr batch detail/review fusion static contract passed')
