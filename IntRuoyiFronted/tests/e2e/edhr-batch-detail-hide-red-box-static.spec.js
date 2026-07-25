const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8')
const railStart = detail.indexOf('<aside class="edhr-batch-detail__review-rail"')
const railEnd = detail.indexOf('</aside>', railStart)
const rail = detail.slice(railStart, railEnd)
const mainPreview = detail.slice(0, railStart)

test('eDHR batch detail hides right-side red-box fill metadata', () => {
  assert.ok(railStart >= 0 && railEnd > railStart, 'review rail must exist')

  assert.doesNotMatch(rail, /edhr-batch-detail__rail-process-forms-head/)
  assert.doesNotMatch(rail, /selectedProcessTaskGroup\.processCode/)
  assert.doesNotMatch(rail, /selectedProcessTasks\.length\s*\}\}\s*张/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-label">执行编号<\/div>/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-value">未打开<\/div>/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-label">状态<\/div>/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-label">提交时间<\/div>/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-label">完成时间<\/div>/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-label">表单<\/div>/)
  assert.doesNotMatch(rail, /edhr-batch-detail__rail-task-tags/)
  assert.doesNotMatch(rail, /edhr-batch-detail__rail-slot-status-list/)
  assert.doesNotMatch(rail, /edhr-batch-detail__rail-slot-blocker/)
  assert.doesNotMatch(rail, /edhr-batch-detail__rail-tags/)
  assert.doesNotMatch(rail, /edhr-batch-detail__rail-summary/)
  assert.doesNotMatch(rail, /edhr-batch-detail__rail-task-detail/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-label">签核摘要<\/div>/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-label">当前应填写<\/div>/)
  assert.doesNotMatch(rail, /<div class="edhr-batch-detail__rail-label">我的填写状态<\/div>/)

  assert.match(rail, /class="edhr-batch-detail__rail-process-form-item"/)
  assert.match(rail, /class="edhr-batch-detail__rail-execution-code"/)
  assert.match(rail, /detail\?\.batchExecutionCode/)
  assert.match(rail, /class="edhr-batch-detail__rail-process-form-filler"/)
  assert.match(rail, /resolveTaskCardFillersText\(task\)/)
  assert.match(rail, /class="edhr-batch-detail__rail-process-form-action"/)
  assert.match(rail, /handleSelectedPendingTaskAction/)
  assert.doesNotMatch(rail, /class="edhr-batch-detail__primary-fill-meta"/)
  assert.doesNotMatch(rail, /primaryFormFillMetaItems/)
  assert.doesNotMatch(rail, /v-if="showPrimaryFormFillMeta"/)
  assert.doesNotMatch(rail, /primaryFormFillMetaItems\.length/)

  assert.doesNotMatch(mainPreview, /class="edhr-batch-detail__primary-fill-meta"/)
  assert.doesNotMatch(detail, /primaryFormFillMetaItems/)
  assert.doesNotMatch(detail, /const showPrimaryFormFillMeta = computed/)
  assert.doesNotMatch(detail, /type PrimaryFormFillMetaItem/)
  assert.doesNotMatch(detail, /class="edhr-batch-detail__primary-fill-label"/)
  assert.doesNotMatch(detail, /class="edhr-batch-detail__primary-fill-value"/)
})
