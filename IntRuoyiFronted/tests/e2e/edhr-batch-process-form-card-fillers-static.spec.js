const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
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
assert.ok(railStart >= 0 && railEnd > railStart, '右侧当前工序摘要栏必须存在。')
const rail = detail.slice(railStart, railEnd)

const taskCardStart = rail.indexOf('class="edhr-batch-detail__rail-process-form-item"')
const taskCardEnd = rail.indexOf('class="edhr-batch-detail__rail-process-form-actions"', taskCardStart)
assert.ok(taskCardStart >= 0 && taskCardEnd > taskCardStart, '必须能定位右侧单据卡片主体。')
const taskCardBody = rail.slice(taskCardStart, taskCardEnd)

assert.ok(
  taskCardBody.includes('class="edhr-batch-detail__rail-process-form-filler"'),
  '右侧每张单据卡片必须显示填写人元信息。'
)
assert.ok(
  taskCardBody.includes('<span>填写人</span>') &&
    taskCardBody.includes('resolveTaskCardFillersText(task)'),
  '单据卡片填写人必须由当前 task 解析，不能只显示全局底部填写人。'
)

const fillerResolverMatch = detail.match(
  /const resolveTaskCardFillersText = \(row: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*?(?=(?:\r?\n){2}const )/
)
assert.ok(fillerResolverMatch, '必须集中定义单据卡片填写人解析函数。')
const fillerResolver = fillerResolverMatch[0]
assert.match(
  fillerResolver,
  /row\.fillableUsers/,
  '单据卡片填写人必须优先使用后端返回的当前单据 fillableUsers。'
)
assert.match(
  fillerResolver,
  /'未配置'/,
  '单据缺少填写人时必须显式显示“未配置”。'
)
assert.doesNotMatch(
  fillerResolver,
  /userStore|currentUser|当前填写人|createdBy|createUser|updateUser/,
  '单据卡片不得通过当前登录人或创建更新人推断填写人。'
)

console.log('PASS: eDHR right-side process form cards display per-document fillers.')
