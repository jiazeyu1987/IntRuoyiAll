const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(process.cwd())

const flowConfigApi = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/route/flowconfig.ts'),
  'utf8'
)
const routeFlowConfigPanel = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue'),
  'utf8'
)
const routeProcessPage = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteProcessList.vue'),
  'utf8'
)

assert.match(flowConfigApi, /batchRecordReports\??:/, '工艺流程批记录配置 API 类型必须使用 batchRecordReports 数组。')
assert.match(flowConfigApi, /executionMode\??:/, '工艺流程批记录配置 API 类型必须声明 executionMode。')
assert.doesNotMatch(
  flowConfigApi,
  /ProRouteFlowProcessConfigSaveVO[\s\S]*batchRecordReportId\??:/,
  '保存请求不得继续使用单个 batchRecordReportId。'
)

assert.match(routeFlowConfigPanel, /batchRecordReports/, '工艺流程批记录配置页面必须渲染多批记录列表。')
assert.match(routeFlowConfigPanel, /executionMode/, '工艺流程批记录配置页面必须提供串行/并行执行模式。')
assert.match(routeFlowConfigPanel, /SEQUENTIAL/, '页面必须支持串行模式。')
assert.match(routeFlowConfigPanel, /PARALLEL/, '页面必须支持并行模式。')
assert.doesNotMatch(
  routeFlowConfigPanel,
  /<el-select[\s\S]*v-model="scope\.row\.batchRecordReportId"/,
  '工艺流程批记录配置页面不得继续使用单个默认批记录下拉。'
)

assert.doesNotMatch(routeProcessPage, /默认批记录/, '原始工艺路线组成工序页不得展示默认批记录绑定。')
assert.doesNotMatch(routeProcessPage, /batchRecordReportId/, '原始工艺路线组成工序页不得读写 batchRecordReportId。')

console.log('PASS: eDHR multi batch route static contract')
