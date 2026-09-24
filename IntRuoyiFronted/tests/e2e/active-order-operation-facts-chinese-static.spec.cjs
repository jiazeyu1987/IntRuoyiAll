const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')
const read = (relativePath) => fs.readFileSync(path.resolve(repoRoot, relativePath), 'utf8')

const panel = read(
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)

assert.match(
  panel,
  /<td>\{\{ formatActiveOrderOperationType\(fact\) \}\}<\/td>/,
  '操作事实表的操作列必须通过中文格式化函数展示。'
)
assert.match(
  panel,
  /<td>\{\{ formatActiveOrderOperationResultStatus\(fact\.resultStatus\) \}\}<\/td>/,
  '操作事实表的结果列必须通过中文格式化函数展示。'
)
assert.match(
  panel,
  /CLOSE_ACTIVE_ORDER_BY_RELEASE:\s*'按上市放行关闭活跃订单'/,
  'CLOSE_ACTIVE_ORDER_BY_RELEASE 必须映射为中文操作文案。'
)
assert.match(panel, /SUCCESS:\s*'成功'/, 'SUCCESS 必须映射为中文结果。')
assert.match(
  panel,
  /ACTIVE_ORDER_OPERATION_LABEL_UNMAPPED/,
  '未映射操作必须显式报错，不得显示未知操作兜底文案。'
)
assert.doesNotMatch(panel, /return '未知操作'/, '操作事实不得显示“未知操作”。')
assert.match(
  panel,
  /return '未知结果'/,
  '未来新增未知结果枚举必须显示中文默认文案，不直接暴露英文码。'
)
assert.doesNotMatch(panel, /<th>来源类型<\/th>/, '操作事实表不得显示来源类型列。')
assert.doesNotMatch(panel, /<th>来源编号<\/th>/, '操作事实表不得显示来源编号列。')
assert.doesNotMatch(
  panel,
  /formatActiveOrderOperationSourceType\(fact\.sourceType\)/,
  '操作事实表不得渲染来源类型。'
)
assert.doesNotMatch(panel, /<td>\{\{ fact\.sourceId \}\}<\/td>/, '操作事实表不得渲染来源编号。')
assert.doesNotMatch(
  panel,
  /data-active-order-operation-source-id/,
  '操作事实表行不得把来源编号继续暴露到 DOM 属性。'
)
assert.doesNotMatch(
  panel,
  /<td>\{\{ fact\.(operationName|operationType|sourceType|resultStatus)/,
  '操作事实表不得直接显示后端英文枚举字段。'
)

console.log('PASS: active-order operation facts Chinese copy static contract')
