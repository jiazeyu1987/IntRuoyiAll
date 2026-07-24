const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/infra/runtime-control/index.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/infra/runtimeControl/index.ts'),
  'utf8'
)

function assertIncludes(source, fragment, label) {
  assert(source.includes(fragment), `missing ${label}: ${fragment}`)
}

assertIncludes(pageSource, "action: 'apply-test-db-sql'", 'quick apply action')
assertIncludes(pageSource, "label: '测试服数据库快应用'", 'quick apply label')
assertIncludes(pageSource, 'icon: \'ep:coin\'', 'database icon')
assertIncludes(pageSource, 'operationDialog.sqlPath', 'dialog SQL path state')
assertIncludes(pageSource, 'v-model="operationDialog.sqlPath"', 'SQL path input binding')
assertIncludes(pageSource, 'placeholder="输入本机 SQL 文件绝对路径"', 'SQL path placeholder')
assertIncludes(pageSource, "operationDialog.action === 'apply-test-db-sql'", 'quick apply branching')
assertIncludes(pageSource, '请填写 SQL 文件路径', 'SQL path validation')
assertIncludes(pageSource, 'sqlPath:', 'submit payload SQL path')
assertIncludes(pageSource, '只应用明确 SQL，不同步数据库整库、MinIO 或发布包状态。', 'expected result copy')

assertIncludes(apiSource, 'sqlPath?: string', 'runtime-control action sqlPath type')

console.log('PASS: runtime-control test DB quick apply static wiring is present')
