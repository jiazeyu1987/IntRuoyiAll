const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../..')
const servicePath = path.join(root, 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java')
const testPath = path.join(root, 'IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceTest.java')
const backendRulesPath = path.join(root, 'docs/backend-development.md')
const experienceIndexPath = path.join(root, 'docs/experience-index.md')

const service = fs.readFileSync(servicePath, 'utf8')
const tests = fs.readFileSync(testPath, 'utf8')
const backendRules = fs.readFileSync(backendRulesPath, 'utf8')
const experienceIndex = fs.readFileSync(experienceIndexPath, 'utf8')

assert.ok(service.includes('byRouteProcessReferenceId'), 'projection mapping used by graph projection must be named as route-process references, not generic original IDs')
assert.ok(service.includes('byFrozenOfficialRouteProcessId'), 'production leader inheritance must expose a frozen official routeProcessId mapping')
assert.ok(!service.includes('byOriginalId'), 'generic byOriginalId naming is ambiguous between official routeProcessId and clientRouteProcessId')
assert.ok(service.includes('QA 规程不是损耗原因或设备参数标准的数据源'), 'service must document that QA regulations are not the source for production leader loss/parameter configs')
assert.ok(service.includes('clientRouteProcessId 只用于流程图投影引用'), 'service must document clientRouteProcessId is not an inheritance source')
assert.ok(service.includes('运行态不得 fallback 回读旧 routeProcessId'), 'service must document runtime must not fallback to old routeProcessId')

assert.ok(tests.includes('projectCandidate_shouldNotInheritTeamLeaderConfigsByProcessIdOrSortWithoutFrozenRouteProcessId'), 'regression must cover same processId/sort without frozen official routeProcessId')
assert.ok(tests.includes('processId 和 sort 相同也不得继承'), 'test must make processId/sort non-identity explicit')

assert.ok(backendRules.includes('QA 规程不是生产组长损耗原因或设备参数标准的数据源'), 'backend gate must separate QA regulation from production leader config source')
assert.ok(backendRules.includes('clientRouteProcessId 只用于流程图投影引用'), 'backend gate must forbid clientRouteProcessId as inheritance source')
assert.ok(backendRules.includes('不得按 processId、工序名称、sort 或运行态 fallback 回读旧 routeProcessId'), 'backend gate must forbid processId/name/sort/fallback inheritance')
assert.ok(experienceIndex.includes('QA规程不是损耗设备参数来源'), 'experience index must route future agents to the clarity gate')

console.log('PASS: route publish chain clarity static contract')
