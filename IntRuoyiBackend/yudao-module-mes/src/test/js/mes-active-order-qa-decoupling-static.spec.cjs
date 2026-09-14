const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const service = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
)
const migration = read('sql/mysql/20260813_mes_active_order_qa_decoupling.sql')

const evaluateCandidate = service.slice(
  service.indexOf('private CandidateEligibility evaluateCandidateEligibility'),
  service.indexOf('private List<MesProScheduleOrderProcessDO> toRouteProcessSources')
)
const addActiveOrder = service.slice(
  service.indexOf('public Long addActiveOrder'),
  service.indexOf('public void removeActiveOrder')
)

assert.match(evaluateCandidate, /resolveProductionRouteSource/)
assert.doesNotMatch(evaluateCandidate, /Dcc|Qa|Pqc|regulation|validateCandidatePqcPrerequisites/i)
assert.match(addActiveOrder, /requireProductionRouteSourceForAdd/)
assert.match(addActiveOrder, /insertProcessSnapshots/)
assert.doesNotMatch(addActiveOrder, /insertPqcInspectionTasks|qaRegulation|dccProjectCode/i)
assert.match(migration, /MODIFY COLUMN `dcc_project_code_id` bigint DEFAULT NULL/)
assert.match(migration, /MODIFY COLUMN `qa_regulation_id` bigint DEFAULT NULL/)
assert.match(migration, /MODIFY COLUMN `qa_regulation_version_id` bigint DEFAULT NULL/)

console.log('PASS: active-order admission is independent from QA and PQC task generation')
