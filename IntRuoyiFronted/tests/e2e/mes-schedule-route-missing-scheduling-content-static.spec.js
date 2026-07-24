const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const schedulePagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')

assert(fs.existsSync(pagePath), '工艺路线用途共享页必须存在。')
assert(fs.existsSync(schedulePagePath), '工艺流程排产配置页必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const schedulePageSource = fs.readFileSync(schedulePagePath, 'utf8')

assert(
  schedulePageSource.includes('use-type="SCHEDULE"'),
  '当前静态契约只允许工艺流程排产配置承接排产相关缺项。'
)

assert(
  pageSource.includes('prop="status"'),
  '工艺流程排产配置列表必须承接源工艺路线状态筛选或展示能力。'
)
assert(
  pageSource.includes('label="状态"'),
  '工艺流程排产配置列表必须显示状态字段，便于排产员识别启用状态。'
)
assert(
  pageSource.includes('copyDialogVisible'),
  '工艺流程排产配置页面必须承接复制路线入口，不应要求用户回基础工艺路线补做排产链路复制。'
)
assert(
  pageSource.includes('ProRouteApi.copyRoute'),
  '工艺流程排产配置页面复制入口必须调用既有复制路线 API。'
)
assert(
  pageSource.includes('复制路线'),
  '工艺流程排产配置页面必须提供明确的复制路线弹窗标题。'
)

console.log('PASS: MES schedule route missing scheduling content static contract')
