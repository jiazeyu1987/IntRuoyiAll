const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const componentPath = path.resolve(frontendRoot, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const source = fs.readFileSync(componentPath, 'utf8')

const configDialogMatch = source.match(/<Dialog[\s\S]*?class="route/flow-config-dialog"[\s\S]*?<RouteForm ref="routeFormRef" \/>/)
assert.ok(configDialogMatch, '工艺流程排产配置弹窗结构必须存在。')
const dialogBlock = configDialogMatch[0]

assert.doesNotMatch(
  dialogBlock,
  /route/flow-config__title|route/flow-config__subtitle/,
  '配置弹窗顶部不得显示路线编码、名称、负责人或关键工序摘要。'
)

const summaryMatch = dialogBlock.match(/<div class="route/flow-config__summary">[\s\S]*?<\/div>\s*<el-alert/)
assert.ok(summaryMatch, '配置弹窗必须保留顶部产品选择摘要区域。')
assert.doesNotMatch(
  summaryMatch[0],
  /应用工作台默认值/,
  '应用工作台默认值按钮不得继续出现在顶部摘要区域。'
)

const footerMatch = dialogBlock.match(/<template #footer>[\s\S]*?<\/template>/)
assert.ok(footerMatch, '配置弹窗必须有底部 footer。')
const footerBlock = footerMatch[0]
assert.match(
  footerBlock,
  /class="route/flow-config-footer"/,
  '配置弹窗 footer 必须使用左右分区布局。'
)
assert.match(
  footerBlock,
  /class="route/flow-config-footer__left"/,
  '应用工作台默认值按钮必须放在 footer 左侧区域。'
)
assert.match(
  footerBlock,
  /应用工作台默认值/,
  '应用工作台默认值按钮必须显示在 footer 中。'
)
assert.match(
  footerBlock,
  /@click="applyWorkbenchScheduleDefaults"/,
  '应用工作台默认值按钮必须保持可点击并触发原方法。'
)
assert.doesNotMatch(
  footerBlock,
  /disabled|:disabled/,
  '应用工作台默认值按钮不得禁用，必须灰色但可点击。'
)
assert.match(
  footerBlock,
  /class="route-flow-config-panel-workbench-default-button"/,
  '应用工作台默认值按钮必须使用灰色普通按钮样式类。'
)
assert.match(
  source,
  /\.route-flow-config-panel-workbench-default-button[\s\S]*?color:\s*#4b5563/,
  '应用工作台默认值按钮必须是灰色视觉。'
)
