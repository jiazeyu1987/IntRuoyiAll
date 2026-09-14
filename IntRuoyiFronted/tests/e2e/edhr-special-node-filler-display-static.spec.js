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

const specialActionStart = detail.indexOf('class="edhr-batch-detail__special-node-action-grid"')
const specialActionEnd = detail.indexOf('</div>', specialActionStart)
assert.ok(specialActionStart >= 0 && specialActionEnd > specialActionStart, '必须能定位右侧特殊节点操作区。')
const specialActionBlock = detail.slice(specialActionStart, specialActionEnd)

assert.ok(
  specialActionBlock.includes('edhr-batch-detail__special-node-filler') &&
    specialActionBlock.includes('edhr-batch-detail__rail-process-form-filler'),
  '右侧特殊节点操作区必须展示填写人，并复用普通单据卡片填写人样式。'
)
assert.ok(
  specialActionBlock.includes('resolveTaskCardFillersText(selectedTaskForEvidence)'),
  '特殊节点填写人必须来自后端当前特殊 task 的 fillableUsers 解析。'
)
assert.doesNotMatch(
  specialActionBlock,
  /currentUser|userStore|createdBy|updateUser/,
  '特殊节点填写人不得由当前登录人、创建人或更新人推断。'
)

console.log('PASS: eDHR special node action rail displays configured fillers.')
