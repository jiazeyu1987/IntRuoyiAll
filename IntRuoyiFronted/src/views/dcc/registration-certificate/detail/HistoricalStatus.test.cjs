const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const root = __dirname + '/../'
test('historical facts label distinguishes recorded state without changing the recorded value', () => {
 const source = fs.readFileSync(root + 'shared/FactsFields.vue', 'utf8')
 const expression = source.match(/:label="([^"]+)"/)[1]
 const facts = { certificateType: 'CE', certificateStatus: 'VALID' }
 for (const historicalRecord of [true, false]) {
  const context = { historicalRecord, field: 'certificateStatus', model: facts, factLabels: { certificateStatus: '证件状态' } }
  assert.equal(vm.runInNewContext(expression, context), historicalRecord ? '证载状态（历史记录）' : '证件状态')
  assert.equal(facts.certificateStatus, 'VALID')
  assert.equal(vm.runInNewContext(expression, { ...context, field: 'expiryDate', factLabels: { expiryDate: '有效期至' } }), historicalRecord ? '原有效期至' : '有效期至')
 }
})
test('old detail shows replacement notice and historical facts; evidence snapshots are marked historical', () => {
 const detail = fs.readFileSync(root + 'detail/index.vue', 'utf8')
 const notice = detail.match(/<el-alert v-if="([^"]+)" title="([^"]+)"[^>]+\/>/g).find(line => line.includes('已被替换'))
 assert.ok(notice)
 const condition = notice.match(/v-if="([^"]+)"/)[1]
 assert.equal(vm.runInNewContext(condition, { detail: { status: 'OLD' } }), true)
 assert.equal(vm.runInNewContext(condition, { detail: { status: 'CURRENT' } }), false)
 assert.ok(detail.includes(':historical-record="detail.status === \'OLD\'"'))
 assert.ok(fs.readFileSync(root + 'history/EvidencePanel.vue', 'utf8').includes(':model-value="parseFacts(snapshot.factsJson)" historical-record readonly'))
})
