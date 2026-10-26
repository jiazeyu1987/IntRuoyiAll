const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const read = p => fs.readFileSync(path.resolve(__dirname, '../..', p), 'utf8')
const records = read('src/views/Profile/components/ProductionTaskRecords.vue')
for (const label of ['我已处理', '候选待办', '逾期任务', '责任来源', '审核来源', '到期时间', '完成时间']) assert.ok(records.includes(label), label)
assert.match(read('src/views/Profile/components/ProfileWorkbench.vue'), /ProductionTaskRecords/)
const rules = read('src/views/mes/pro/route/RouteTaskRulesDialog.vue')
for (const kind of ['Close', 'Archive', 'ReleaseApproval']) {
  assert.ok(rules.includes(`getEdhrRoute${kind}Rule`))
  assert.ok(rules.includes(`saveEdhrRoute${kind}Rule`))
}
console.log('PASS: task records and route rules replace retired board')
