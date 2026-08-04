const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { join } = require('node:path')

const root = process.cwd()
const designer = readFileSync(
  join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)
const flowConfigApi = readFileSync(join(root, 'src/api/mes/pro/route/flowconfig.ts'), 'utf8')
const feedbackApi = readFileSync(join(root, 'src/api/mes/pro/feedback/index.ts'), 'utf8')

assert.match(designer, /data-flow-boundary-field="productionLeader"/, '工序开始必须展示生产组长字段入口。')
assert.match(designer, /handleSelectBoundaryDetailField\('productionLeader'\)/, '生产组长字段必须进入正式字段明细面板。')
assert.match(designer, /data-flow-panel="route-start-production-leader-detail"/, '生产组长必须有独立配置面板。')
assert.match(designer, /生产组长/, '页面必须使用生产组长业务文案。')
assert.match(designer, /data-route-start-production-leader-production-line/, '生产组长配置必须显式绑定当前工艺路线范围。')
assert.match(designer, /当前工艺路线/, '生产组长负责范围必须按当前工艺路线表达。')
assert.doesNotMatch(
  designer,
  /工作站产线|路线工序已绑定工作站产线/,
  '生产组长配置不得要求通过工序工作站反推产线。'
)
assert.match(designer, /data-route-start-production-leader-source-type/, '生产组长配置必须显式选择账号或角色来源。')
assert.match(designer, /data-route-start-production-leader-candidate/, '生产组长配置必须绑定账号或角色。')
assert.match(
  designer,
  /ROUTE_START_PRODUCTION_LEADER_CANDIDATE_SOURCE_OPTIONS[\s\S]*USERS[\s\S]*ROLE/,
  '生产组长来源必须支持账号和权限角色。'
)

assert.match(flowConfigApi, /ProRouteStartProductionLeaderVO/, '前端 API 类型必须声明生产组长配置响应。')
assert.match(flowConfigApi, /getRouteStartProductionLeaders/, '前端 API 必须读取生产组长配置。')
assert.match(flowConfigApi, /saveRouteStartProductionLeaders/, '前端 API 必须保存生产组长配置。')
assert.match(flowConfigApi, /getRouteStartProductionLeaderProductionLines/, '前端 API 必须读取当前路线负责范围。')
assert.doesNotMatch(
  feedbackApi,
  /frontline-pressure-pump:all-processes/,
  '生产填写前端不得依赖压力泵全工序菜单权限控制切换。'
)

console.log('mes-route-start-production-leaders-static: PASS')
