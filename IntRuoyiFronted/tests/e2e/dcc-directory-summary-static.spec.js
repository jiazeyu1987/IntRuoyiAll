const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const directoryPage = readSource('src/views/dcc/controlled-file/directories/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const directoryTable = extractBetween(
  directoryPage,
  '<el-table',
  '</el-table>'
)

assert.strictEqual(
  packageJson.scripts['e2e:dcc:directory-summary:static'],
  'node tests/e2e/dcc-directory-summary-static.spec.js',
  'package.json 必须提供 e2e:dcc:directory-summary:static 脚本'
)

assert.ok(
  directoryTable.includes('data-user-table-column-explicit'),
  '受控目录表必须显式排除全局浮动显示字段和重置入口'
)
assert.ok(
  !directoryTable.includes('data-testid="dcc-directory-summary"'),
  '受控目录表不得继续显示目录摘要测试标识'
)
assert.ok(!directoryTable.includes('label="目录摘要"'), '受控目录表不得继续显示目录摘要列')

for (const removedHeader of ['目录摘要', '启用状态', '排序', '创建时间']) {
  assert.ok(
    !directoryTable.includes(`label="${removedHeader}"`),
    `受控目录表不应继续显示独立 ${removedHeader} 表头`
  )
}

for (const behaviorToken of [
  'handleImportFromIntAuth',
  'openAccessRules(row)',
  "openForm('create', row)",
  "openForm('update', row)",
  'handleDeleteParentFolder(row)',
  'TreeExpandActions',
  'deleteDirectorySubtree',
  'getDirectoryActiveNasTransfer',
  'stopDirectoryActiveNasTransfer'
]) {
  assert.ok(directoryPage.includes(behaviorToken), `受控目录原有行为必须保留：${behaviorToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(directoryTable),
  '受控目录摘要不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC directory summary static contract')
