const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const approvalActions = readSource('src/views/dcc/controlled-file/detail/approval-actions.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')

assert.match(
  approvalActions,
  /if \(!form\.reason\?\.trim\(\)\) \{\s*errors\.reason = mode === 'reject' \? '请输入驳回原因' : '请输入审批意见'/,
  'DCC 审批通过和驳回都必须在前端校验处理意见，避免空原因进入电子签名。'
)

assert.match(
  approvalActions,
  /reason:\s*form\.reason\.trim\(\)/,
  'DCC 审批通过请求必须发送非空且已 trim 的审批意见。'
)

assert.doesNotMatch(
  detailPage,
  /请输入审批意见（选填）/,
  'DCC 审批通过弹窗不得继续提示审批意见选填。'
)

assert.match(
  detailPage,
  /:placeholder="actionDialog\.mode === 'reject' \? '请输入驳回原因' : '请输入审批意见'"/,
  'DCC 审批通过弹窗必须提示审批意见必填。'
)

console.log('PASS: DCC approval reason required static contract')
