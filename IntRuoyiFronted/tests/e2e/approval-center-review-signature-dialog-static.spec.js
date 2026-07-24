const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const view = fs.readFileSync(path.join(repoRoot, 'src/views/approval-center/index.vue'), 'utf8')
const api = fs.readFileSync(path.join(repoRoot, 'src/api/approval-center/index.ts'), 'utf8')

const dialogMatch = view.match(/<el-dialog[\s\S]*?title="审核确认"[\s\S]*?<\/el-dialog>/)
assert.ok(dialogMatch, '审核确认弹窗必须存在')
const dialog = dialogMatch[0]

assert.match(dialog, /class="approval-center__review-dialog"/, '审核确认弹窗必须保留专属布局类')
assert.match(dialog, /width="560px"/, '审核确认弹窗应使用更宽的紧凑布局承载签名和条件原因')
assert.match(dialog, /approval-center__review-summary-card/, '审核确认弹窗必须使用摘要卡片优化信息层级')

assert.match(dialog, /<el-form-item\s+v-if="reviewForm\.result === 'REJECT'"\s+label="不通过原因"\s+required>/, '审核通过时不得渲染不通过原因，只有不通过时展示并必填')
assert.match(dialog, /v-model="reviewForm\.signaturePassword"/, '审核确认弹窗必须提供电子签名密码输入')
assert.match(dialog, /label="电子签名"[\s\S]*?type="password"[\s\S]*?show-password/, '电子签名输入必须是密码框并允许显示密码')

assert.match(view, /signaturePassword:\s*string/, 'reviewForm 必须声明 signaturePassword')
assert.match(view, /reviewForm\.signaturePassword = ''/, '打开审核弹窗时必须清空旧签名密码')
assert.match(view, /!reviewForm\.signaturePassword\.trim\(\)[\s\S]*?请输入电子签名密码/, '审核通过和不通过提交前都必须校验电子签名密码')
assert.match(view, /signaturePassword:\s*reviewForm\.signaturePassword\.trim\(\)/, '提交审核时必须把电子签名密码传给统一审核接口')

assert.match(api, /signaturePassword:\s*string/, '统一审核提交请求契约必须包含电子签名密码')

console.log('PASS: approval center review signature dialog static contract')
