const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const detail = read('src/views/system/notify/my/MyNotifyMessageDetail.vue')
const helper = read('src/utils/notifyMessageNavigation.ts')

assert(
  !/<el-descriptions\s+:column="1"\s+border/.test(detail),
  '站内信详情弹窗不应继续使用单列 el-descriptions 作为主布局。'
)
assert.match(
  detail,
  /class="notify-message-detail__header"/,
  '站内信详情弹窗必须包含结构化摘要头。'
)
assert.match(
  detail,
  /class="notify-message-detail__meta-grid"/,
  '站内信详情弹窗必须包含元信息网格。'
)
assert.match(
  detail,
  /class="notify-message-detail__content"/,
  '站内信详情弹窗必须包含独立消息内容区。'
)
assert.match(
  detail,
  /const templateParamEntries = computed/,
  '站内信详情弹窗必须将 templateParams 转为可读结构化参数。'
)
assert.match(
  detail,
  /v-if="templateParamEntries\.length"/,
  '站内信详情弹窗必须在存在业务参数时展示结构化参数区。'
)
assert.match(
  detail,
  /const hasDetailActions = computed/,
  '站内信详情弹窗必须统一判断快捷操作区是否展示。'
)
assert.match(
  detail,
  /v-if="hasDetailActions"[\s\S]*navigateToShowroomProduct[\s\S]*navigateToBpmApproval/,
  '站内信详情弹窗必须在同一快捷操作区保留关联产品与 BPM 审批入口。'
)
assert.match(
  helper,
  /export const BPM_PROCESS_DETAIL_PATH\s*=\s*'\/bpm\/process-instance\/detail'[\s\S]*new URL\([^)]*detailUrl[\s\S]*url\.pathname\s*!==\s*BPM_PROCESS_DETAIL_PATH/,
  'BPM 审批直达入口必须继续使用 URL parser 校验内部详情路径。'
)

console.log('notify-popup-structured-message-layout-static: PASS')
