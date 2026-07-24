const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const pagePath = path.resolve(frontendRoot, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const routeApiPath = path.resolve(frontendRoot, 'src/api/mes/pro/route/index.ts')
const packageJsonPath = path.resolve(frontendRoot, 'package.json')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const routeApiSource = fs.readFileSync(routeApiPath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

assert.equal(
  packageJson.scripts?.['e2e:mes:schedule-route-shared-config:static'],
  'node tests/e2e/mes-schedule-route-shared-config-static.spec.js',
  'package.json must expose the MES schedule route shared config static contract'
)

assert.doesNotMatch(
  routeApiSource,
  /interface ProRouteScheduleConfigVO[\s\S]*item(Id|Code|Name|Specification)\?:/,
  'schedule config API type must not expose product dimension fields'
)
assert.doesNotMatch(
  pageSource,
  /selectedProductId/,
  '工艺流程排产配置页面不得继续按产品选择排产配置。'
)
assert.doesNotMatch(
  pageSource,
  /\.filter\(\(config\) => Number\(config\.itemId\)/,
  '工艺流程排产配置加载不得按 itemId 过滤配置。'
)
assert.doesNotMatch(
  pageSource,
  /itemId:\s*selectedProductId/,
  '工艺流程排产配置保存 payload 不得提交产品维度 itemId。'
)
assert.match(
  pageSource,
  /所有关联产品共用当前路线排产配置/,
  '工艺流程排产配置页面必须明确提示关联产品共用路线级排产配置。'
)
assert.match(
  pageSource,
  /new Map<number, ProRouteScheduleConfigVO>\(\s*configs\.map\(\(config\) => \[config\.routeProcessId, config\]\)/,
  '工艺流程排产配置必须按 routeProcessId 建立路线级配置映射。'
)

console.log('PASS: MES schedule route shared config static contract')
