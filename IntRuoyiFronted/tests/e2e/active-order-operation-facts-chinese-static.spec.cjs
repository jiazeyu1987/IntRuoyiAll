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
  /<td>\{\{ formatActiveOrderOperationSourceType\(fact\.sourceType\) \}\}<\/td>/,
  '操作事实表的来源类型列必须通过中文格式化函数展示。'
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
assert.match(
  panel,
  /ACTIVE_ORDER_DOSSIER_FILE:\s*'活跃订单资料文件'/,
  'ACTIVE_ORDER_DOSSIER_FILE 必须映射为中文来源类型。'
)
assert.match(panel, /SUCCESS:\s*'成功'/, 'SUCCESS 必须映射为中文结果。')
assert.match(
  panel,
  /return '未知操作'/,
  '未来新增未知操作枚举必须显示中文默认文案，不直接暴露英文码。'
)
assert.match(
  panel,
  /return '未知来源'/,
  '未来新增未知来源类型必须显示中文默认文案，不直接暴露英文码。'
)
assert.match(
  panel,
  /return '未知结果'/,
  '未来新增未知结果枚举必须显示中文默认文案，不直接暴露英文码。'
)
assert.doesNotMatch(
  panel,
  /<td>\{\{ fact\.(operationName|operationType|sourceType|resultStatus)/,
  '操作事实表不得直接显示后端英文枚举字段。'
)

console.log('PASS: active-order operation facts Chinese copy static contract')
