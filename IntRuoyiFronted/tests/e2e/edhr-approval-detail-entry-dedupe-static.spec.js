const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ApprovalPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const actionColumnMatch = source.match(
  /<el-table-column label="操作"[\s\S]*?<\/el-table-column>\s*<\/el-table>/
)
assert.ok(actionColumnMatch, '审批列表必须保留操作列。')

const actionColumnSource = actionColumnMatch[0]

assert.ok(
  source.includes('class="edhr-workbench__execution-link"') &&
    source.includes('@click="openDetail(row)"'),
  '审批列表必须由执行编号链接承担详情入口。'
)

assert.equal(
  (source.match(/@click="openDetail\(row\)"/g) || []).length,
  1,
  '审批列表详情跳转入口只能保留在执行编号链接上。'
)

assert.doesNotMatch(
  actionColumnSource,
  />\s*查看\s*</,
  '操作列不得重复展示“查看”按钮。'
)

assert.ok(
  source.includes('canOpenDetail(row)') &&
    source.includes('edhr-workbench__execution-link--disabled') &&
    source.includes('缺少执行记录 ID，无法打开审批详情。'),
  '缺少 executionId 时必须展示不可点击执行编号并说明原因，不能跳转到错误详情。'
)

assert.match(
  source,
  /<el-table-column label="操作" width="1[0-9]{2}" fixed="right">/,
  '移除重复查看后，操作列宽度应收敛到 100-199px。'
)

console.log('PASS: EDHR approval detail entry dedupe static contract')
