import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { stripTypeScriptTypes } from 'node:module'
const source = fs.readFileSync(new URL('../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue', import.meta.url), 'utf8')
const start = source.indexOf('const formatActiveOrderSignatureCellText =')
const end = source.indexOf('const toOperationFactSignature =', start)
assert.ok(start >= 0 && end > start)
const js = stripTypeScriptTypes(source.slice(start, end) + '\nthis.format = formatActiveOrderSignatureCellText; this.open = openActiveOrderSignatureRecord;')
const opened = []
const context = { formatDateTime: value => value || '-', signatureEvidenceViewer: { open: id => opened.push(id) } }
vm.runInNewContext(js, context)
const simulated = { role: 'SIMULATION_SESSION', signerName: 'B（模拟）', signedAt: '2026-10-04 09:00:00' }
assert.equal(context.format(simulated), '模拟记录：B（模拟）（2026-10-04 09:00:00）')
assert.equal(context.format({ role: 'PRODUCTION_SUBMIT', signerName: 'B' }), '未签名')
assert.equal(context.format(undefined), '未签名')
assert.equal(context.format({ signatureId: 17, signerName: '冻结B', signedAt: '2026-10-04' }), '冻结B（2026-10-04）')
context.open(simulated)
context.open({ role: 'PRODUCTION_SUBMIT' })
assert.deepEqual(opened, [])
context.open({ signatureId: 17 })
assert.deepEqual(opened, [17])
assert.match(source, /:disabled="!signature\.signatureId"/)
console.log('PASS: 7 display/navigation/disabled contracts; real formatter and navigation functions executed')
