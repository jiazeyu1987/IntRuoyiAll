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

assert(source.includes('<el-table-column label="BU"'), 'product list must render a BU column')
assert(!source.includes('<el-table-column label="英文名称"'), 'product list must no longer render English name column')
assert(source.includes('pipelineLayout: string'), 'normalized row model must expose pipelineLayout')
assert(
  source.includes('pipelineLayout: fields.pipeline_layout ||') ||
    source.includes('pipelineLayout: resolveOptionalStringField(fields'),
  'normalized row must read pipeline_layout from product fields'
)
assert(source.includes("{{ row.pipelineLayout || '未填写' }}"), 'BU column must display Chinese BU with an empty fallback label')

console.log('showroom product list BU column static check passed')
