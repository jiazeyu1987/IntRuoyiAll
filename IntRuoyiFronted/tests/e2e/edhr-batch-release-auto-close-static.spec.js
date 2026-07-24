const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const detailPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)

const runActionMatch = detailPage.match(
  /const runReleaseTransactionAction = async \(action: \(\) => Promise<unknown>, successText: string\) => \{([\s\S]*?)\n\}/
)

assert.ok(runActionMatch, '批次详情页必须保留统一的放行动作成功/失败处理函数。')

const runActionBody = runActionMatch[1]
const tryMatch = runActionBody.match(/try \{([\s\S]*?)\} catch/)
const catchMatch = runActionBody.match(/catch \(error\) \{([\s\S]*?)\} finally/)

assert.ok(tryMatch, '放行动作处理函数必须有明确成功路径。')
assert.ok(catchMatch, '放行动作处理函数必须有明确失败路径。')

const successBody = tryMatch[1]
const failureBody = catchMatch[1]

assert.match(
  successBody,
  /await action\(\)[\s\S]*releaseTransactionDialogVisible\.value\s*=\s*false/,
  '放行接口成功后必须先关闭二次确认弹框。'
)
assert.match(
  successBody,
  /releaseApprovalDrawerVisible\.value\s*=\s*false/,
  '放行接口成功后必须自动关闭外层“放行审批”抽屉。'
)
assert.match(successBody, /await loadDetail\(\)/, '放行接口成功后必须刷新批次详情。')
assert.doesNotMatch(
  failureBody,
  /releaseApprovalDrawerVisible\.value\s*=\s*false/,
  '放行接口失败时不得自动关闭外层“放行审批”抽屉，避免隐藏错误。'
)

console.log('PASS edhr batch release auto close static contract')
