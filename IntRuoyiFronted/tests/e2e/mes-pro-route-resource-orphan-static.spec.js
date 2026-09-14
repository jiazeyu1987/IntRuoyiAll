const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const servicePath = path.join(
  root,
  '..',
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'service',
  'pro',
  'route',
  'MesProRouteResourceServiceImpl.java'
)
const service = fs.readFileSync(servicePath, 'utf8').replace(/\r\n/g, '\n')

const routeMapIndex = service.indexOf('Map<Long, MesProRouteDO> routeMap = routeService.getRouteMap(routeIds);')
const itemMapIndex = service.indexOf('Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(itemIds);')
const processMapIndex = service.indexOf('Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);')
const loopIndex = service.indexOf('for (MesProRouteProductDO routeProduct : routeProducts)')

assert.ok(routeMapIndex >= 0, 'route resource service must load routeMap')
assert.ok(itemMapIndex > routeMapIndex, 'route resource service must load itemMap after routeMap')
assert.ok(processMapIndex > itemMapIndex, 'route resource service must load processMap after itemMap')
assert.ok(loopIndex > processMapIndex, 'route resource service must build rows after loading dependent maps')

const preLoopSource = service.slice(itemMapIndex, loopIndex)
assert.match(
  preLoopSource,
  /routeProducts\s*=\s*filterResolvedRouteProducts\(routeProducts,\s*routeMap,\s*itemMap\);/,
  'route resource page must remove route_product rows whose route or item cannot be resolved before building rows'
)
assert.match(
  service,
  /private List<MesProRouteProductDO> filterResolvedRouteProducts\([\s\S]*routeMap\.containsKey\(routeProduct\.getRouteId\(\)\)[\s\S]*itemMap\.containsKey\(routeProduct\.getItemId\(\)\)/,
  'route product validity filter must require both an existing route and an existing item'
)
assert.ok(
  preLoopSource.indexOf('filterResolvedRouteProducts') < preLoopSource.indexOf('List<MesProRouteProcessDO> routeProcesses'),
  'unresolved route_product rows must be filtered before route process and row assembly'
)

console.log('PASS: route resource read model filters orphan route products before row assembly')
