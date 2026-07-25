const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')
const railStart = detail.indexOf('<aside class="edhr-batch-detail__review-rail"')
const railEnd = detail.indexOf('</aside>', railStart)
assert.ok(railStart >= 0 && railEnd > railStart, '右侧操作栏模板必须存在')
const rail = detail.slice(railStart, railEnd)
const mainPreview = detail.slice(0, railStart)

const assertIncludes = (token, message) => {
  assert.ok(detail.includes(token), message)
}

const assertExcludes = (token, message) => {
  assert.ok(!detail.includes(token), message)
}

assertIncludes(
  'grid-template-columns: 240px minmax(0, 1fr) 260px;',
  '复盘主区域必须使用工序列表、表单、操作栏三列布局'
)
assertIncludes(
  '<aside class="edhr-batch-detail__review-rail" aria-label="当前工序摘要">',
  '右侧蓝框区域必须保留当前工序表单操作入口'
)
assertExcludes(
  '<div class="edhr-batch-detail__rail-actions">',
  '右侧摘要栏顶部不得显示基础/详情入口'
)
assertExcludes(
  '<div class="edhr-batch-detail__rail-summary">',
  '右侧红框摘要不再显示，签核/填写状态旧摘要不得恢复'
)
assertExcludes(
  '<div class="edhr-batch-detail__rail-label">执行编号</div>',
  '右侧摘要栏不得展示执行编号卡片'
)
assertExcludes(
  '<div class="edhr-batch-detail__rail-value">未打开</div>',
  '右侧摘要栏不得展示未打开执行编号占位卡片'
)
assertExcludes(
  '<div class="edhr-batch-detail__rail-label">签核摘要</div>',
  '右侧红框不得继续展示签核摘要'
)
assertExcludes(
  '<div class="edhr-batch-detail__rail-label">当前应填写</div>',
  '右侧红框不得继续展示填写人'
)
assertExcludes(
  '<div class="edhr-batch-detail__rail-label">我的填写状态</div>',
  '右侧红框不得继续展示当前登录人的填写状态'
)
assertExcludes(
  '<div class="edhr-batch-detail__rail-label">完成时间</div>',
  '右侧摘要栏不得展示完成时间'
)
assertExcludes(
  '<div v-if="selectedExecution" class="edhr-batch-detail__rail-tags">',
  '右侧摘要栏不得展示执行摘要标签'
)
assertExcludes(
  '<div class="edhr-batch-detail__section-header">\n          <div class="edhr-batch-detail__section-actions">',
  '基础/详情入口不应继续占用复盘区顶部红框位置'
)
assertExcludes(
  '<el-descriptions v-if="selectedExecution" :column="4" border>',
  '执行摘要不应继续以四列表格占用表单顶部'
)
assertExcludes(
  '<div v-if="selectedExecution" class="edhr-batch-detail__execution-summary">',
  '执行摘要标签不应继续占用表单顶部'
)
assert.ok(
  /\.edhr-batch-detail__review-rail\s*\{[\s\S]*?position:\s*sticky;/.test(detail),
  '右侧操作栏应在宽屏保持粘性，方便查看长表单'
)
assert.ok(
  rail.includes('class="edhr-batch-detail__rail-execution-code"') &&
    rail.includes('detail?.batchExecutionCode'),
  '右侧当前表单卡片顶部必须显示批次执行编号'
)
assert.ok(
  /\.edhr-batch-detail__rail-execution-code\s*\{[\s\S]*?overflow-wrap:\s*anywhere;[\s\S]*?white-space:\s*normal;/.test(detail),
  '批次执行编号过长时必须允许自动换行，不能撑破右侧栏'
)
assertExcludes(
  'class="edhr-batch-detail__primary-fill-meta"',
  'Right red-box fill metadata must stay hidden'
)
assert.ok(
  !rail.includes('class="edhr-batch-detail__primary-fill-meta"') &&
    !rail.includes('primaryFormFillMetaItems'),
  'Right rail must not render the red-box fill metadata block'
)
assert.ok(
  !rail.includes('v-if="showPrimaryFormFillMeta"') &&
    !detail.includes('const showPrimaryFormFillMeta = computed'),
  'Right red-box fill metadata control logic must not remain'
)
assert.ok(
  !mainPreview.includes('class="edhr-batch-detail__primary-fill-meta"'),
  '顶部表单预览红框位置不得继续显示填写人和提交时间'
)
assertExcludes(
  '.edhr-batch-detail__primary-fill-value',
  'Right red-box fill metadata styles must not remain'
)
console.log('PASS: EDHR review summary right rail static contract')
