const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const fillerColumnStart = page.indexOf('label="填写人"')
const fillerColumnEnd = page.indexOf('label="类型"', fillerColumnStart)

assert.notEqual(fillerColumnStart, -1, '批记录表单列表必须存在“填写人”列。')
assert.notEqual(fillerColumnEnd, -1, '“填写人”列后必须保留“类型”列。')

const fillerColumn = page.slice(fillerColumnStart, fillerColumnEnd)

assert.doesNotMatch(
  fillerColumn,
  /['"]查看错误['"]/,
  '填写人规则加载失败时不得在“加载失败”标签旁重复显示“查看错误”文字条目。'
)

assert.match(
  fillerColumn,
  /v-if="!row\.permissionRuleErrorMessage"[\s\S]*class="batch-record-form-filler-cell__text"/,
  '填写人辅助文字必须在错误态隐藏，确保该单元格只保留一个“加载失败”视觉条目。'
)

assert.match(
  fillerColumn,
  /:content="row\.permissionRuleErrorMessage"/,
  '删除额外文字后仍必须通过 tooltip 保留真实错误信息。'
)

assert.match(
  fillerColumn,
  /:title="row\.permissionRuleErrorMessage \|\| undefined"/,
  '删除额外文字后仍必须通过 title 保留真实错误信息。'
)

assert.match(
  fillerColumn,
  /resolveFillRuleStatus\([\s\S]*row\.permissionRuleErrorMessage[\s\S]*\)\.label/,
  '填写人规则错误态必须继续显示“加载失败”状态标签。'
)

console.log('PASS: eDHR batch record filler error state renders one visual entry.')
