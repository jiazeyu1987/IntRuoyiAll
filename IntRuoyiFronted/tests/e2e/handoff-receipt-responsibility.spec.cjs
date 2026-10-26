const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/utils/handoffReceiptResponsibility.ts'), 'utf8')
const code = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
const exportsObject = {}
Function('exports', code)(exportsObject)
const read = exportsObject.readHandoffReceiptCandidates
const snapshot = { identityDomain: 'SYSTEM_USER', sourceType: 'NONCONFORMANCE_REVIEW', activeOrderId: 101, sourceId: 303, roundId: 303, userIds: [345, 346] }
const task = (changes = {}) => ({ activeOrderId: '101', sourceType: snapshot.sourceType, sourceId: '303', roundId: '303', responsibilitySnapshotJson: JSON.stringify({ ...snapshot, ...changes }) })
test('reads original task candidates regardless of subsequent role membership', () => {
  assert.deepEqual(read(task()), ['345', '346'])
  assert.deepEqual(read(task({ userIds: ['9223372036854775807'] })), ['9223372036854775807'])
})
for (const change of [{ activeOrderId: 102 }, { sourceId: 304 }, { roundId: 304 }, { sourceType: 'OTHER' }, { identityDomain: 'MES_EMPLOYEE_PROFILE' }, { userIds: [] }, { userIds: [345, 345] }, { userIds: [9007199254740992] }, { userIds: ['9223372036854775808'] }])
  test('rejects unrelated or invalid responsibility evidence ' + JSON.stringify(change), () => assert.throws(() => read(task(change))))
test('missing or malformed original evidence is not replaced with current members', () => {
  assert.throws(() => read({ ...task(), responsibilitySnapshotJson: undefined }))
  assert.throws(() => read({ ...task(), responsibilitySnapshotJson: 'broken' }))
})
