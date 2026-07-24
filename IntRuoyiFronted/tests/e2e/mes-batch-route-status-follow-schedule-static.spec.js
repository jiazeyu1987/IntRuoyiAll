const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')

assert(fs.existsSync(pagePath), '工艺路线用途配置组件必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  !pageSource.includes('v-if="useType === \'SCHEDULE\'" label="状态"'),
  '工艺流程批记录配置必须与工艺流程排产配置一样显示状态筛选。'
)
assert(
  !pageSource.includes('v-if="useType === \'SCHEDULE\'"\n          label="状态"'),
  '工艺流程批记录配置必须与工艺流程排产配置一样显示状态列。'
)
assert(
  pageSource.includes('status: queryParams.status'),
  '工艺流程批记录配置列表查询必须与工艺流程排产配置一样传递状态筛选。'
)
assert(
  pageSource.includes("row.status === CommonStatusEnum.ENABLE ? '启用' : '停用'"),
  '工艺流程批记录配置状态列必须沿用工艺流程排产配置的启用/停用展示口径。'
)

console.log('PASS: MES batch route status follows schedule route static contract')
