const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const respVO = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/MesProRouteRespVO.java')
const service = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteServiceImpl.java')
const edgeMapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/route/MesProRouteProcessFlowEdgeMapper.java')
const routeProcessMapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/route/MesProRouteProcessMapper.java')

assert.match(respVO, /private Boolean flowGraphConfigured;/, 'MesProRouteRespVO 必须返回 flowGraphConfigured。')
assert.match(service, /MesProRouteProcessFlowEdgeMapper/, '路线服务必须注入流转关系边 Mapper。')
assert.match(service, /buildFlowGraphConfiguredByRouteId\(routeIds\)/, '分页展示字段必须批量聚合关系图状态。')
assert.match(service, /route\.setFlowGraphConfigured\(Boolean\.TRUE\.equals\(flowGraphConfiguredByRouteId\.get\(route\.getId\(\)\)\)\)/, '每条路线必须写入明确布尔状态。')
assert.match(edgeMapper, /selectConfiguredRouteIdsByRouteIds/, '关系边 Mapper 必须提供按 routeIds 批量查询已设置路线。')
assert.match(edgeMapper, /SELECT DISTINCT route_id/, '关系边 Mapper 必须用 DISTINCT route_id 聚合已设置状态。')
assert.match(routeProcessMapper, /selectRelationConfiguredRouteIdsByRouteIds/, '组成工序 Mapper 必须按 next_process_id 批量查询已设置路线。')
assert.match(routeProcessMapper, /next_process_id IS NOT NULL/, '组成工序关系状态必须来自 next_process_id。')
assert.match(service, /routeProcessMapper\.selectRelationConfiguredRouteIdsByRouteIds\(routeIds\)/, '关系图状态必须合并组成工序 nextProcessId 来源。')

console.log('mes-route-flow-graph-status-contract-static PASS')
