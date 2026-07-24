const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const messagePopover = read('src/layout/components/Message/src/Message.vue')

assert.doesNotMatch(
  messagePopover,
  /\{\{\s*item\.templateNickname\s*\}\}\s*：\s*\{\{\s*item\.templateContent\s*\}\}/,
  '站内信弹框不能再把发送人和模板内容整段拼接展示。'
)

assert.match(
  messagePopover,
  /const stripEntryInfo\s*=/,
  '站内信弹框必须集中清理入口信息。'
)
assert.match(
  messagePopover,
  /入口\(\?:信息\)\?/,
  '入口信息清理规则必须同时覆盖“入口”和“入口信息”。'
)
assert.match(
  messagePopover,
  /const getMessageDisplay\s*=/,
  '站内信弹框必须通过结构化展示模型渲染消息。'
)

for (const className of [
  'message-card__source',
  'message-card__headline',
  'message-card__body',
  'message-card__date'
]) {
  assert.match(messagePopover, new RegExp(className), `站内信弹框必须渲染结构化区域 ${className}。`)
}

assert.match(
  messagePopover,
  /getMessageDisplay\(item\)\.headline[\s\S]*getMessageDisplay\(item\)\.body/,
  '站内信弹框必须分别渲染标题和正文，不应只显示一段长文本。'
)
assert.doesNotMatch(
  messagePopover,
  /入口[：:]\s*\/|入口信息[：:]\s*\//,
  '站内信弹框模板中不得保留入口路径展示文案。'
)
assert.match(
  messagePopover,
  /name:\s*'Profile'[\s\S]*tab:\s*'notifyMessage'/,
  '站内信弹框“查看全部”入口必须继续跳转到个人中心站内信 Tab。'
)

console.log('PASS: notify popup structured layout static contract')
