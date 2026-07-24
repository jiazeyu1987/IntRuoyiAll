const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const detail = read('src/views/system/notify/my/MyNotifyMessageDetail.vue')
const helper = read('src/utils/notifyMessageNavigation.ts')

assert.match(
  helper,
  /export const BPM_PROCESS_DETAIL_PATH\s*=\s*'\/bpm\/process-instance\/detail'/,
  '我的站内信详情必须声明 BPM 审批详情安全路径。'
)
assert.match(
  detail,
  /const bpmApprovalNavigation = computed/,
  '我的站内信详情必须根据 templateParams.detailUrl 识别 BPM 审批直达入口。'
)
assert.match(
  helper,
  /templateParams\.detailUrl/,
  'BPM 审批直达入口必须来自站内信模板参数 detailUrl。'
)
assert.match(
  helper,
  /new URL\([^)]*detailUrl/,
  'BPM 审批 detailUrl 必须用 URL parser 解析，避免字符串拼接。'
)
assert.match(
  helper,
  /url\.pathname\s*!==\s*BPM_PROCESS_DETAIL_PATH/,
  'BPM 审批直达入口只能允许站内 BPM 流程详情路径。'
)
assert.match(
  detail,
  /v-if="bpmApprovalNavigation"[\s\S]*去审批/,
  '我的站内信详情必须在 BPM 通知中显示“去审批”快捷操作。'
)
assert.match(
  detail,
  /navigateToNotifyMessageTarget\(router,\s*navigation/,
  '点击“去审批”必须通过统一跳转工具进入 BPM 流程详情并携带原 query。'
)
