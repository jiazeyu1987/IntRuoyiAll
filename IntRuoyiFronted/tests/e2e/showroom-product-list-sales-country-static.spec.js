const { readFileSync } = require('node:fs')
const { join } = require('node:path')

const repoRoot = join(__dirname, '..', '..')
const tablePath = join(repoRoot, 'src', 'views', 'showroom-admin', 'components', 'ProductListTable.vue')
const source = readFileSync(tablePath, 'utf8')

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  !source.includes('<el-table-column label="产品编码"'),
  'product list must no longer render a 产品编码 column'
)
assert(
  source.includes('label="在售国家"') && source.includes('prop="targetMarket"'),
  'product list must render a 中文在售国家 column bound to targetMarket'
)
assert(
  source.includes('targetMarket: string'),
  'normalized row model must expose targetMarket'
)
assert(
  source.includes("resolveOptionalStringField(fields, ['target_market'], 'target_market', index)") ||
    source.includes("resolveOptionalStringField(fields || {}, ['target_market'], 'target_market', index)"),
  'normalized row must read target_market from product fields'
)
assert(
  source.indexOf('label="获证状态"') < source.indexOf('label="在售国家"') &&
    source.indexOf('label="在售国家"') < source.indexOf('label="封面"'),
  '在售国家 column must be rendered between 获证状态 and 封面'
)

console.log('showroom product list sales country static check passed')
