const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const helper = read('src/utils/notifyMessageNavigation.ts')
const edhrHelper = read('src/utils/edhrWorkTaskNavigation.ts')
const popup = read('src/layout/components/Message/src/Message.vue')
const list = read('src/views/system/notify/my/components/MyNotifyMessageList.vue')
const detail = read('src/views/system/notify/my/MyNotifyMessageDetail.vue')

assert.match(
  helper,
  /export const BPM_PROCESS_DETAIL_PATH\s*=\s*'\/bpm\/process-instance\/detail'/,
  '站内信点击直达必须集中声明 BPM 安全详情路径。'
)
assert.match(
  helper,
  /export const getNotifyMessageTarget\s*=/,
  '站内信点击直达必须集中解析消息业务目标。'
)
assert.match(
  helper,
  /new URL\([^)]*detailUrl/,
  'BPM detailUrl 必须继续用 URL parser 解析。'
)
assert.match(
  helper,
  /url\.pathname\s*!==\s*BPM_PROCESS_DETAIL_PATH/,
  'BPM detailUrl 必须校验只允许内部流程详情路径。'
)
assert.match(
  helper,
  /export const navigateToNotifyMessageTarget\s*=/,
  '站内信点击直达必须集中执行目标跳转，避免各组件散落拼 URL。'
)

assert.match(
  edhrHelper,
  /export const EDHR_WORK_TASK_NOTIFY_PATHS\s*=\s*new Set/,
  'eDHR 工作任务站内信 actionUrl 必须使用内部路径白名单。'
)
assert.match(
  helper,
  /templateParams\.actionUrl[\s\S]*new URL\([^)]*actionUrl[\s\S]*EDHR_WORK_TASK_NOTIFY_PATHS\.has\(url\.pathname\)/,
  'eDHR 工作任务站内信 actionUrl 必须用 URL parser 解析并校验白名单路径。'
)

assert.match(
  popup,
  /hasNotifyMessageTarget\(item\)/,
  '顶部站内信弹框必须根据业务目标决定消息卡片是否可点击。'
)
assert.match(
  popup,
  /@click="handleMessageCardClick\(item\)"/,
  '顶部站内信弹框点击消息卡片必须跳转到具体业务内容。'
)
assert.match(
  popup,
  /message-card--clickable/,
  '顶部站内信弹框存在业务目标时必须显示可点击样式。'
)

assert.match(
  list,
  /<template #default="scope">[\s\S]*hasNotifyMessageTarget\(scope\.row\)[\s\S]*@click\.stop="handleNotifyContentClick\(scope\.row\)"/,
  '我的站内信列表“消息内容”列必须在存在业务目标时可点击直达。'
)
assert.match(
  list,
  /my-notify-message-list__content-link/,
  '我的站内信列表可点击内容必须使用稳定链接样式。'
)
assert.match(
  list,
  /class="my-notify-message-list__content-link"[\s\S]*role="link"[\s\S]*:title="scope\.row\.templateContent \|\| '-'"[\s\S]*@click\.stop="handleNotifyContentClick\(scope\.row\)"/,
  '我的站内信列表“消息内容”可点击内容必须具备链接语义、完整标题和稳定点击处理。'
)
assert.doesNotMatch(
  list,
  /label="消息内容"[\s\S]{0,260}show-overflow-tooltip/,
  '我的站内信列表“消息内容”列不能依赖表格溢出 tooltip 包裹点击内容，避免点击区域被 tooltip 容器截断。'
)
assert.match(
  list,
  /\.my-notify-message-list__content-link\s*\{[\s\S]*display:\s*block;[\s\S]*width:\s*100%;/,
  '我的站内信列表“消息内容”链接必须铺满内容列宽度，不能只让文字局部可点。'
)

assert.match(
  detail,
  /hasDetailTarget/,
  '站内信详情弹窗必须根据当前消息业务目标决定正文是否可点击。'
)
assert.match(
  detail,
  /@click="handleContentClick"/,
  '站内信详情弹窗消息正文点击必须跳转到具体业务内容。'
)
assert.match(
  detail,
  /notify-message-detail__content-button/,
  '站内信详情弹窗可点击正文必须使用稳定链接样式。'
)
assert.match(
  detail,
  /edhrWorkTaskNavigation[\s\S]*navigateToEdhrWorkTask/,
  '站内信详情弹窗必须保留 eDHR 工作任务直达入口。'
)

console.log('PASS: notify content click through static contract')
