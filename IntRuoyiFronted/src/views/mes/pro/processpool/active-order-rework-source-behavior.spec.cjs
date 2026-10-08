const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), ts = require('typescript')
const file = path.join(__dirname, 'components/activeOrderReworkSourceLocation.ts')
const text = fs.readFileSync(file, 'utf8').replace(/^import type .*\r?\n/gm, '').replace(/export /g, '')
const code = ts.transpileModule(text + '\nreturn {buildReworkSourceDetailLocation}', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
const { buildReworkSourceDetailLocation: build } = new Function(code)()
const source = (current=420,previous=419) => ({currentActiveOrderId:current,sourceActiveOrderId:previous,
  sourceBusinessStatus:'REWORKED',reviewId:90,reviewCode:'NCR-90',nonconformanceReason:'返工原因',reviewOpinion:'确认返工',qaUserId:345,
  qaSignature:{signatureId:29026,signerName:'QA',signedAt:1791247258000},
  qaSignatureEvidence:{id:29026,actorId:345,actionCode:'QA_DISPOSITION',contentHash:'a'.repeat(64),evidenceHash:'b'.repeat(64)},
  qaSignatureVerification:{signatureId:29026,verificationStatus:'VALID',storedContentHash:'a'.repeat(64),calculatedContentHash:'a'.repeat(64),
    storedEvidenceHash:'b'.repeat(64),calculatedEvidenceHash:'b'.repeat(64)}})
test('ordinary details have no rework source entry', () => assert.equal(build(420,undefined,true), undefined))
test('formal source419 belongs to successor420 and uses the existing historical detail route', () => {
  assert.deepEqual(build(420, source(),true), {
    name:'MesProEdhrBatchExecutionActiveOrderDetail',query:{activeOrderId:'419',from:'history'}
  })
})
test('changing the current cycle cannot expose stale source lineage', () => {
  assert.throws(() => build(422, source(),true), /不匹配/)
  assert.throws(() => build(420, source(420,420),true), /不匹配/)
})
test('source IDs retain exact Long strings; ambiguous or imprecise identities fail', () => {
  assert.equal(build('9223372036854775806', source('9223372036854775806','9223372036854775805'),true).query.activeOrderId,'9223372036854775805')
  for(const id of [0,-1,'1.5','9223372036854775808',9223372036854775806]) assert.throws(() => build(420,source(420,id),true))
})
test('341 without batch query retains the formal source identifier and cannot generate a forbidden route', () => {
  assert.equal(build(420,source(),false),undefined)
  assert.throws(() => build(422,source(),false),/不匹配/)
})
test('partial source or invalid QA evidence produces a visible error before linking or rendering', () => {
  for(const mutation of [{qaSignature:undefined},{qaSignatureEvidence:undefined},{qaSignatureVerification:undefined},{reviewOpinion:''},
      {qaSignatureVerification:{signatureId:29026,verificationStatus:'MISMATCH'}}]) assert.throws(() => build(420,{...source(),...mutation},true))
})
