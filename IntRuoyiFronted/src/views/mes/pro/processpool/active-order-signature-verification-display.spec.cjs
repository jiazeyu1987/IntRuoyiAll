const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const helper = path.join(__dirname, 'components/activeOrderSignatureVerificationDisplay.ts')
const panel = path.join(__dirname, 'components/ActiveOrderSubmissionDetailPanel.vue')
const api = path.resolve(__dirname, '../../../../api/mes/pro/edhr/activeOrderSignature.ts')
const source = fs.readFileSync(helper, 'utf8').replace(/^import .*\n/gm, '').replace(/export /g, '')
const code = ts.transpileModule(source + '\nreturn signatureVerificationDisplay', {
  compilerOptions: { target: ts.ScriptTarget.ES2020 }
}).outputText
const display = new Function(code)()
const fact = (status, reasonCodes, time = 'CONSISTENT', audit = 'CONSISTENT') => ({
  verificationStatus: status, digestStatus: 'CONSISTENT', timeConsistencyStatus: time,
  auditBindingStatus: audit, verificationScope: 'LOCAL_DIGEST_TIME_AND_CREATION_AUDIT',
  timeSource: 'SERVER_CLOCK', timeTrustStatus: 'NOT_BOUND', reasonCodes
})

test('VALID states the local scope and never declares externally trusted time', () => {
  const result = display(fact('VALID', []))
  assert.equal(result.label, '本地核验通过')
  assert.equal(result.type, 'success')
  assert.equal(result.scope, '本地摘要、签署时间值与创建审计')
  assert.equal(result.timeSource, '服务器时钟')
  assert.equal(result.timeTrust, '未绑定外部可信时间')
  assert.equal(result.digest, '一致')
  assert.equal(result.reasons, '无异常原因')
})

test('missing creation audit is incomplete with its actual reason and dimension', () => {
  const result = display(fact('INCOMPLETE', ['AUDIT_LINK_MISSING'], 'CONSISTENT', 'MISSING'))
  assert.equal(result.label, '证据未完整')
  assert.equal(result.type, 'warning')
  assert.equal(result.audit, '未记录')
  assert.equal(result.time, '一致')
  assert.equal(result.reasons, '创建审计未记录（AUDIT_LINK_MISSING）')
  assert.equal(result.timeTrust, '未绑定外部可信时间')
})

test('changed time is mismatch and retains the time failure reason', () => {
  const result = display(fact('MISMATCH', ['TIME_VALUE_MISMATCH'], 'MISMATCH'))
  assert.equal(result.label, '不一致')
  assert.equal(result.type, 'danger')
  assert.equal(result.time, '不一致')
  assert.equal(result.audit, '一致')
  assert.equal(result.reasons, '签署时间值不一致（TIME_VALUE_MISMATCH）')
})

test('unknown reason and scope stay visible as supplied facts', () => {
  const result = display({ ...fact('INCOMPLETE', ['FUTURE_REASON']), verificationScope: 'FUTURE_SCOPE' })
  assert.equal(result.reasons, 'FUTURE_REASON')
  assert.equal(result.scope, 'FUTURE_SCOPE')
})

test('dialog renders every projected dimension, reason, scope and time trust', () => {
  const vue = fs.readFileSync(panel, 'utf8')
  const dialog = vue.slice(vue.indexOf('label="原记录完整性状态"'), vue.indexOf('label="内容摘要"'))
  for (const property of ['type', 'label', 'digest', 'time', 'audit', 'scope', 'timeSource', 'timeTrust', 'reasons']) {
    assert.ok(dialog.includes(`signatureVerificationDisplay(signatureEvidenceViewer.state.selected.verification).${property}`), property)
  }
  assert.ok(vue.includes("import { signatureVerificationDisplay } from './activeOrderSignatureVerificationDisplay'"))
  const type = fs.readFileSync(api, 'utf8')
  assert.ok(type.includes("'VALID' | 'INCOMPLETE' | 'MISMATCH'"))
  assert.ok(type.includes("timeTrustStatus: 'NOT_BOUND'"))
  assert.ok(type.includes('reasonCodes: string[]'))
})
