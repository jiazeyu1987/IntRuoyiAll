const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const distributionSection = extractBetween(
  detailPage,
  '<div class="text-15px font-600">分发状态</div>',
  '<ContentWrap data-testid="dcc-detail-training-section"',
  'detail distribution section'
)

assert.equal(
  packageJson.scripts['e2e:dcc:detail-distribution-summary:static'],
  'node tests/e2e/dcc-detail-distribution-summary-static.spec.js',
  'package.json 必须提供详情分发摘要静态契约脚本'
)

assert.match(
  distributionSection,
  /data-testid="dcc-detail-distribution-section"/,
  '详情页分发状态区域必须提供稳定测试标识'
)

const retainedColumns = ['部门', '接收人', '分发摘要', '回收摘要', '操作']
for (const label of retainedColumns) {
  assert.match(distributionSection, new RegExp(`label="${label}"`), `分发状态主表必须展示：${label}`)
}

const removedColumns = [
  '文件编号',
  '版本',
  '名称',
  '发放方式',
  '状态',
  '发放人',
  '发放日期',
  '回收人',
  '回收日期'
]
for (const label of removedColumns) {
  assert.doesNotMatch(
    distributionSection,
    new RegExp(`label="${label}"`),
    `分发状态主表不应继续横向拆散或重复展示：${label}`
  )
}

assert.match(
  distributionSection,
  /data-testid="dcc-detail-distribution-summary"/,
  '分发摘要列必须提供稳定测试标识'
)
assert.match(
  distributionSection,
  /getDistributionMediumLabel\(row\.distributionMedium\)/,
  '分发摘要必须继续展示发放方式'
)
assert.match(
  distributionSection,
  /getDistributionStatusLabel\(row\.status\)/,
  '分发摘要必须继续展示分发状态'
)
assert.match(
  distributionSection,
  /getDistributionAckUserSummary\(row,\s*userNameMap\)/,
  '分发摘要必须继续展示发放人'
)
assert.match(
  distributionSection,
  /formatControlledFileDateTime\(row\.acknowledgedAt\)/,
  '分发摘要必须继续展示发放日期'
)

assert.match(
  distributionSection,
  /data-testid="dcc-detail-recovery-summary"/,
  '回收摘要列必须提供稳定测试标识'
)
assert.match(
  distributionSection,
  /getDistributionRecoverUserSummary\(row\)/,
  '回收摘要必须继续展示回收人'
)
assert.match(
  distributionSection,
  /formatControlledFileDateTime\(row\.recoveredAt\)/,
  '回收摘要必须继续展示回收日期'
)
assert.match(
  distributionSection,
  /getDistributionRecipientDisplay\(row\)/,
  '分发状态主表必须继续展示接收人'
)

for (const actionText of ['导出回执', '打印回执', '确认签收', '接收人加签', '确认纸质发放', '确认回收']) {
  assert.match(distributionSection, new RegExp(actionText), `分发状态主表必须保留操作：${actionText}`)
}
assert.match(
  distributionSection,
  /hasDistributionRowAction\(row\)/,
  '分发行级空操作判断必须保留'
)

assert.doesNotMatch(
  distributionSection,
  /mock|placeholder|fallback|降级|吞异常/i,
  '详情分发摘要优化不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC detail distribution summary static contract')
