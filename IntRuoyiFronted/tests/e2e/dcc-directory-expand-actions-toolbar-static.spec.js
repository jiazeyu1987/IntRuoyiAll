const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const directoryPage = readSource('src/views/dcc/controlled-file/directories/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const toolbar = extractBetween(directoryPage, '<el-form', '</el-form>')
const tableShellBeforeTable = extractBetween(
  directoryPage,
  '<ContentWrap>\n    <el-alert',
  '<el-table'
)

assert.ok(
  toolbar.includes('<TreeExpandActions @expand="expandAll" @collapse="collapseAll" />'),
  '目录管理页的全部展开/全部折叠必须并入查询工具栏'
)

assert.ok(
  toolbar.indexOf('刷新目录树') < toolbar.indexOf('TreeExpandActions'),
  '展开折叠操作应位于刷新目录树之后，形成目录级命令组'
)

assert.ok(
  !tableShellBeforeTable.includes('TreeExpandActions'),
  '目录表格上方不应继续保留孤立的展开折叠控制行'
)

assert.ok(
  !tableShellBeforeTable.includes('mb-12px flex justify-end'),
  '目录表格前不应继续使用单独右对齐控制行占用首屏空间'
)

for (const behaviorToken of [
  'handleQuery',
  'resetQuery',
  'getList',
  'handleImportFromIntAuth',
  'openAccessRules(row)',
  "openForm('create', row)",
  "openForm('update', row)",
  'handleDeleteParentFolder(row)',
  'expandAll',
  'collapseAll',
  'useTreeTableExpand(true)'
]) {
  assert.ok(directoryPage.includes(behaviorToken), `目录管理原有行为必须保留：${behaviorToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(toolbar + tableShellBeforeTable),
  '目录展开操作工具栏收敛不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC directory expand actions toolbar static contract')
