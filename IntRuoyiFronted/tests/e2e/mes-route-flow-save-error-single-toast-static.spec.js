const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeApi = read('src/api/mes/pro/route/index.ts')
const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const designer = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.match(
  routeApi,
  /validateRouteProcessFlowGraph:\s*async\s*\(\s*data:\s*RouteFlowGraphSaveReqVO,\s*options:\s*Record<string,\s*unknown>\s*=\s*\{\}\s*\)[\s\S]*request\.post<RouteFlowValidationVO>\(\{[\s\S]*\/mes\/pro\/route-process-flow\/validate[\s\S]*data,\s*\.\.\.options/s,
  '关系图校验 API 必须支持 ignoreErrorMessage 等请求选项，避免 axios 和页面重复 toast。'
)

assert.match(
  routeApi,
  /saveRouteProcessFlowGraph:\s*async\s*\(\s*data:\s*RouteFlowGraphSaveReqVO,\s*options:\s*Record<string,\s*unknown>\s*=\s*\{\}\s*\)[\s\S]*request\.post<RouteFlowValidationVO>\(\{[\s\S]*\/mes\/pro\/route-process-flow\/save[\s\S]*data,\s*\.\.\.options/s,
  '关系图保存 API 必须支持 ignoreErrorMessage 等请求选项，避免 axios 和页面重复 toast。'
)

assert.match(
  routeApi,
  /saveScheduleConfig:\s*async\s*\(\s*data:\s*ProRouteScheduleConfigVO,\s*options:\s*Record<string,\s*unknown>\s*=\s*\{\}\s*\)[\s\S]*request\.post\(\{[\s\S]*\/mes\/pro\/route-schedule-config\/save[\s\S]*data,\s*\.\.\.options/s,
  '路线排产配置保存 API 必须支持静默 axios 自动错误提示，由保存入口统一提示。'
)

assert.match(
  flowConfigApi,
  /saveScheduleConfig:\s*async\s*\(\s*data:\s*ProRouteFlowConfigSaveVO,\s*options:\s*Record<string,\s*unknown>\s*=\s*\{\}\s*\)[\s\S]*request\.post\(\{[\s\S]*\/mes\/pro\/route\/flow-config\/schedule\/save[\s\S]*data,\s*\.\.\.options/s,
  '工序排产配置保存 API 必须支持静默 axios 自动错误提示。'
)

assert.match(
  flowConfigApi,
  /saveBatchRecordConfig:\s*async\s*\(\s*data:\s*ProRouteFlowConfigSaveVO,\s*options:\s*Record<string,\s*unknown>\s*=\s*\{\}\s*\)[\s\S]*request\.post\(\{[\s\S]*\/mes\/pro\/route\/flow-config\/batch-record\/save[\s\S]*data,\s*\.\.\.options/s,
  '批记录配置保存 API 必须支持静默 axios 自动错误提示。'
)

assert.match(
  designer,
  /validateRouteProcessFlowGraph\(buildPayload\(\),\s*\{\s*ignoreErrorMessage:\s*true\s*\}\)/,
  '保存前关系图校验必须关闭 axios 自动错误 toast。'
)

assert.match(
  designer,
  /saveRouteProcessFlowGraph\(buildPayload\(\),\s*\{\s*ignoreErrorMessage:\s*true\s*\}\)/,
  '关系图保存必须关闭 axios 自动错误 toast。'
)

assert.match(
  designer,
  /ProRouteFlowConfigApi\.saveBatchRecordConfig\(\{[\s\S]*processConfigs:[\s\S]*\},\s*\{\s*ignoreErrorMessage:\s*true\s*\}\s*\)/,
  '批记录草稿配置保存必须关闭 axios 自动错误 toast，由 RouteFormContent 统一提示。'
)

assert.match(
  designer,
  /ProRouteFlowConfigApi\.saveScheduleConfig\(\{[\s\S]*processConfigs:[\s\S]*\},\s*\{\s*ignoreErrorMessage:\s*true\s*\}\s*\)/,
  '排产草稿配置保存必须关闭 axios 自动错误 toast，由 RouteFormContent 统一提示。'
)

assert.doesNotMatch(
  designer,
  /message\.error\(resolveErrorMessage\(error,\s*'流转关系图校验失败'\)\)/,
  'validateBeforeSubmit 不得在 rethrow 前自行 toast，否则父组件会重复提示。'
)

assert.doesNotMatch(
  designer,
  /message\.error\(resolveErrorMessage\(error,\s*'保存流转关系图失败'\)\)/,
  'saveFromParent 不得在 rethrow 前自行 toast，否则父组件会重复提示。'
)

assert.doesNotMatch(
  designer,
  /showSaveValidationToast\(result\)/,
  '校验未通过应抛出可读错误并由保存入口统一 toast，不能子组件和父组件重复提示。'
)

console.log('PASS: route flow save errors surface once from the outer save handler')
