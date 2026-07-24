const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/dcc/controlled-file/browser/index.vue'),
  'utf8'
)

assert.match(
  source,
  /const DCC_BROWSER_DEFAULT_PAGE_SIZE = 20/,
  '文件查阅默认分页条数应为 20，用更多列表行填满可视区域。'
)
assert.match(
  source,
  /const DCC_BROWSER_MIN_PAGE_SIZE = 20/,
  '文件查阅最小分页条数应为 20，避免回到 10 条后再次出现空白。'
)
assert.match(
  source,
  /pageSize:\s*DCC_BROWSER_DEFAULT_PAGE_SIZE/,
  '文件查阅查询参数初始 pageSize 必须使用 20 条默认值。'
)
assert.match(
  source,
  /const resolveBrowserPageSize = \(value: unknown\) => \{[\s\S]*?Math\.max\(parsedPageSize,\s*DCC_BROWSER_MIN_PAGE_SIZE\)/,
  '文件查阅必须统一规范 pageSize，低于 20 条时提升到 20 条。'
)

assert.doesNotMatch(
  source,
  /pageSize:\s*parsePositiveNumber\(route\.query\.pageSize\)\s*\|\|\s*10/,
  '文件查阅路由 pageSize 不得继续回退到 10 条。'
)
assert.doesNotMatch(
  source,
  /queryParams\.pageSize\s*=\s*parsePositiveNumber\(route\.query\.pageSize\)\s*\|\|\s*10/,
  '同步路由时不得把文件查阅 pageSize 回退到 10 条。'
)
assert.doesNotMatch(
  source,
  /queryParams\.pageSize\s*=\s*10/,
  '重置查询时不得把文件查阅 pageSize 重置为 10 条。'
)
assert.doesNotMatch(
  source,
  /pageSize:\s*String\(state\.pageSize\s*\|\|\s*10\)/,
  '写入文件查阅路由时不得继续使用 10 条默认值。'
)

const handlePaginationMatch = source.match(/const handlePagination = async \(\) => \{[\s\S]*?\n\}/)
assert.ok(handlePaginationMatch, '文件查阅必须保留分页处理逻辑。')
assert.match(
  handlePaginationMatch[0],
  /resolveBrowserPageSize\(queryParams\.pageSize\)/,
  '分页变更时必须规范 pageSize，选择或缓存 10 条时自动提升到 20 条。'
)

console.log('PASS: dcc browser fill blank with rows static contract')
