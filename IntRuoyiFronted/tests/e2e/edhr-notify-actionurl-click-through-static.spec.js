const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const helper = read('src/utils/notifyMessageNavigation.ts')
const detail = read('src/views/system/notify/my/MyNotifyMessageDetail.vue')
const popup = read('src/layout/components/Message/src/Message.vue')
const list = read('src/views/system/notify/my/components/MyNotifyMessageList.vue')

assert.match(
  helper,
  /EDHR_WORK_TASK_NOTIFY_PATHS/,
  '站内信统一导航必须声明 eDHR 工作任务 actionUrl 的受控路径白名单。'
)
assert.match(
  helper,
  /templateParams\.actionUrl/,
  'eDHR 工作任务直达入口必须来自结构化 templateParams.actionUrl。'
)
assert.match(
  helper,
  /new URL\([^)]*actionUrl/,
  'eDHR actionUrl 必须用 URL parser 解析，避免字符串拼接。'
)
assert.match(
  helper,
  /EDHR_WORK_TASK_NOTIFY_PATHS\.has\(url\.pathname\)/,
  'eDHR actionUrl 只允许受控内部路径，不得放开任意 URL。'
)
assert.match(
  helper,
  /type:\s*'edhrWorkTask'/,
  '站内信目标类型必须包含 eDHR 工作任务，供弹框、列表和详情复用。'
)
assert.match(
  helper,
  /path:\s*url\.pathname[\s\S]*query:\s*Object\.fromEntries\(url\.searchParams\.entries\(\)\)/,
  'eDHR 工作任务跳转必须保留后端 actionUrl 中的原 query，包括 workTaskId。'
)
assert.match(
  helper,
  /resolveEdhrWorkTaskTarget/,
  'eDHR actionUrl 解析应封装为独立函数，避免污染 BPM 和展厅跳转逻辑。'
)
assert.match(
  helper,
  /resolveShowroomProductTarget\(templateParams\),\s*resolveBpmApprovalTarget\(templateParams\),\s*resolveEdhrWorkTaskTarget\(templateParams\)/,
  '统一目标列表必须纳入 eDHR 工作任务目标。'
)

assert.match(
  detail,
  /edhrWorkTaskNavigation/,
  '站内信详情快捷操作区必须识别 eDHR 工作任务目标。'
)
assert.match(
  detail,
  /v-if="edhrWorkTaskNavigation"[\s\S]*处理批记录任务/,
  '站内信详情必须提供 eDHR 工作任务快捷入口。'
)
assert.match(
  detail,
  /navigateToEdhrWorkTask/,
  '点击 eDHR 快捷入口必须走统一站内信导航。'
)

assert.match(
  popup,
  /hasNotifyMessageTarget\(item\)/,
  '顶部站内信弹框必须继续复用统一目标识别，自动支持 eDHR actionUrl。'
)
assert.match(
  list,
  /hasNotifyMessageTarget\(scope\.row\)/,
  '我的站内信列表必须继续复用统一目标识别，自动支持 eDHR actionUrl。'
)

console.log('PASS: eDHR notify actionUrl click-through static contract')
