const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const routeListPath = path.join(frontendRoot, 'src/views/mes/pro/route/index.vue')
const routeList = fs.readFileSync(routeListPath, 'utf8')

const getFunctionBody = (source, functionName) => {
  const marker = `const ${functionName} = `
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `必须定义 ${functionName}。`)
  const nextFunction = source.indexOf('\nconst ', start + marker.length)
  assert.notEqual(nextFunction, -1, `必须能截取 ${functionName} 函数体。`)
  return source.slice(start, nextFunction)
}

assert.match(
  routeList,
  /:data="visibleRouteVersions"/,
  '版本工作区表格必须绑定过滤后的 visibleRouteVersions，而不是直接展示所有 routeVersions。'
)

assert.match(
  routeList,
  /const visibleRouteVersions = computed\(\(\) =>\s*routeVersions\.value\.filter\(isVisibleRouteVersionInWorkspace\)\s*\)/,
  '版本列表必须通过 computed 从 routeVersions 过滤得到展示行。'
)

const visiblePredicate = getFunctionBody(routeList, 'isVisibleRouteVersionInWorkspace')
assert.match(
  routeList,
  /const EFFECTIVE_ROUTE_VERSION_STATUS_SET = new Set\(\[\s*'ACTIVE',\s*'SUPERSEDED'\s*\]\)/,
  '版本列表必须显式定义已生效历史版本状态集合。'
)
assert.match(
  visiblePredicate,
  /version\.active\s*\|\|\s*EFFECTIVE_ROUTE_VERSION_STATUS_SET\.has\(String\(version\.lifecycleStatus\)\)/,
  '版本列表只能展示当前 ACTIVE 或已生效历史状态 ACTIVE/SUPERSEDED。'
)
assert.doesNotMatch(
  visiblePredicate,
  /version\.lifecycleStatus\s*!==\s*'CANCELLED'/,
  '版本列表不得只排除 CANCELLED；DRAFT、审核中、待生效和驳回候选版本也不属于已生效历史版本。'
)
assert.doesNotMatch(
  visiblePredicate,
  /DRAFT|PENDING_APPROVAL|READY_TO_PUBLISH|REJECTED|CANCELLED/,
  '版本列表过滤谓词不得把非已生效候选状态纳入可见行。'
)

const canViewRouteVersion = getFunctionBody(routeList, 'canViewRouteVersion')
assert.match(
  canViewRouteVersion,
  /version\.active\s*\|\|\s*version\.lifecycleStatus\s*!==\s*'DRAFT'/,
  '隐藏列表行不能删除深链只读查看能力；非草稿历史上下文仍可查看。'
)

console.log('PASS: mes route version list shows effective historical versions only')
