const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const file = path.resolve(__dirname, '../../src/views/signature-governance/components/signatureRecordEvidence.ts')
const js = ts.transpileModule(fs.readFileSync(file, 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
const out = {}; Function('exports', js)(out)
const { createSignatureRecordEvidenceLoader: loader, signatureRecordVerificationDisplay: display, signatureRecordSignedAt: signedAt } = out
const evidence = id => ({ globalId: `UNIFIED-${id}`, evidence: { id, actionCode: 'TEAM_LEADER_REVIEW' }, verification: { signatureId: id, verificationStatus: 'VALID', auditBindingStatus: 'DEVELOPMENT_DISABLED', timeTrustStatus: 'NOT_BOUND', verificationScope: 'LOCAL_DIGEST_TIME_AND_CREATION_AUDIT', reasonCodes: ['GXP_AUDIT_DEVELOPMENT_DISABLED'] } })
const state = () => ({ loading: false, error: '', selected: undefined })

test('review signature details load without any business detail path', async () => {
  for (const id of [64344, 64351]) {
    const s = state(); const l = loader(async key => { assert.equal(key, `UNIFIED-${id}`); return evidence(id) }, s)
    await l.open(`UNIFIED-${id}`); assert.equal(s.selected.evidence.id, id); assert.equal(s.error, '')
  }
})
test('stale success or failure and close cannot repopulate the dialog', async () => {
  const pending = []; const s = state(); const l = loader(() => new Promise((resolve, reject) => pending.push({ resolve, reject })), s)
  const first = l.open('UNIFIED-64344'); const second = l.open('UNIFIED-64351')
  pending[1].resolve(evidence(64351)); await second
  pending[0].reject(new Error('old failure')); await first
  assert.equal(s.selected.evidence.id, 64351); assert.equal(s.error, '')
  const third = l.open('UNIFIED-64344'); l.clear(); pending[2].resolve(evidence(64344)); await third
  assert.equal(s.selected, undefined); assert.equal(s.loading, false)
})
test('mismatching global, evidence and verification identities fail visibly', async () => {
  for (const result of [evidence(64351), { ...evidence(64344), verification: { signatureId: 64351 } }]) {
    const s = state(); await loader(async () => result, s).open('UNIFIED-64344')
    assert.match(s.error, /身份不一致/); assert.equal(s.selected, undefined)
  }
  const s = state(); await loader(async () => { throw new Error('没有查询权限') }, s).open('UNIFIED-64344')
  assert.equal(s.error, '没有查询权限')
})
test('disabled audit, incomplete, mismatch and unknown diagnostics remain explicit', () => {
  const v = evidence(64344).verification
  assert.match(display(v).status, /审计已停用/); assert.match(display(v).scope, /创建审计已停用/)
  assert.match(display(v).reasons, /本地开发环境已停用创建审计/)
  assert.equal(display({ ...v, verificationStatus: 'INCOMPLETE' }).status, '证据未完整')
  assert.equal(display({ ...v, verificationStatus: 'MISMATCH' }).status, '不一致')
  assert.equal(display({ ...v, verificationStatus: 'UNRECOGNIZED', reasonCodes: ['NEW_REASON'] }).status, 'UNRECOGNIZED')
  assert.equal(display({ ...v, reasonCodes: ['NEW_REASON'] }).reasons, 'NEW_REASON')
})
test('signature time uses Shanghai business zone for epoch and local datetime', () => {
  assert.equal(signedAt(Date.UTC(2026, 10, 16, 0, 0, 0)), '2026-11-16 08:00:00（Asia/Shanghai）')
  assert.equal(signedAt('2026-11-16T08:00:00'), '2026-11-16 08:00:00（Asia/Shanghai）')
})

test('normal details open exact unified evidence without a business path; business navigation remains separate', () => {
  const pane = fs.readFileSync(path.resolve(__dirname, '../../src/views/signature-governance/components/SignatureGovernanceRecordsPane.vue'), 'utf8')
  const ast = ts.createSourceFile('pane.ts', pane.match(/<script lang="ts" setup>([\s\S]*?)<\/script>/)[1], ts.ScriptTarget.Latest, true)
  const handlers = ast.statements.filter(ts.isVariableStatement).flatMap(statement => [...statement.declarationList.declarations])
    .filter(d => ['openRecordDetail', 'openBusinessDetail'].includes(d.name.getText(ast))).map(d => `const ${d.getText(ast)}`).join('\n')
  const compiled = ts.transpileModule(handlers, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const opened = [], pushed = [], errors = []
  const { openRecordDetail, openBusinessDetail } = Function('recordEvidenceDialog', 'router', 'message', compiled + '\nreturn {openRecordDetail,openBusinessDetail}')(
    { value: { open: id => opened.push(id) } }, { push: path => pushed.push(path) }, { error: text => errors.push(text) })
  openRecordDetail({ sourceTable: 'system_electronic_signature', globalId: 'UNIFIED-64344' })
  assert.deepEqual(opened, ['UNIFIED-64344']); assert.deepEqual(pushed, []); assert.deepEqual(errors, [])
  openBusinessDetail({ detailPath: '/mes/review?reviewId=123' })
  openRecordDetail({ sourceTable: 'dcc_controlled_file_signature', detailPath: '/dcc/file?id=77' })
  assert.deepEqual(pushed, ['/mes/review?reviewId=123', '/dcc/file?id=77'])
  openRecordDetail({ sourceTable: 'dcc_controlled_file_signature' })
  assert.deepEqual(errors, ['当前签名记录缺少详情路径'])
})
