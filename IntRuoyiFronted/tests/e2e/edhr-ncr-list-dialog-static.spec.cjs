const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const api = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/edhr/nonconformanceReview.ts'),
  'utf8'
)
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'),
  'utf8'
)

assert.match(api, /getNonconformanceReviewPage[\s\S]*url:\s*`\$\{EDHR_NONCONFORMANCE_REVIEW_BASE_URL\}\/page`/)
assert.match(page, /getNonconformanceReviewPage\(/, '列表必须使用全量分页接口。')
assert.doesNotMatch(page, /getPendingNonconformanceReviewPage\(/, '列表不得继续调用仅返回进行中的旧接口。')
assert.match(page, /<el-tabs[\s\S]*data-edhr-ncr-review-tabs/, '评审列表必须使用内部页签。')
assert.match(page, /label="全部"\s+name="all"/, '必须提供“全部”页签。')
assert.match(page, /label="进行中"\s+name="pending"/, '必须提供“进行中”页签。')
assert.doesNotMatch(page, /label="创建不合格评审"/, '创建入口必须是按钮，不得继续占用页签。')
assert.match(page, /data-edhr-ncr-open-create/, '必须提供新建按钮。')
assert.match(page, /data-edhr-ncr-create-dialog/, '新建必须通过弹框显示。')
assert.match(page, /data-edhr-ncr-active-order/, '新建弹框必须只选择活跃订单。')
assert.match(page, /data-edhr-ncr-create-reason/, '新建弹框必须填写不合格原因。')
assert.match(page, /data-edhr-ncr-review-dialog/, '评审详情必须通过弹框显示。')
assert.match(page, /data-edhr-ncr-review-process/, '待处理记录必须提供处理按钮。')
assert.match(page, /openReviewDialog\(row\)/, '处理按钮必须打开当前行评审详情。')
assert.match(page, /activeTab\.value === 'pending'/, '进行中页签必须按 pending_review 查询。')
assert.doesNotMatch(page, /class="edhr-ncr__section edhr-ncr__detail"/, '页面不得常驻显示评审详情面板。')

console.log('PASS edhr-ncr-list-dialog-static')
