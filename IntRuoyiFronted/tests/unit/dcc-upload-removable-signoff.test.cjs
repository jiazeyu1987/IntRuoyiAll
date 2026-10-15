const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const exportsObject = {}
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/signoff-departments.ts', 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, { exports: exportsObject, Error })
const h = exportsObject
const original = (id, stageType, candidateSourceIds, patch = {}) => ({ id, stageType, marker: '●', required: true,
  subjectType: stageType === 'SIGNOFF' ? 'DEPT' : 'USER', candidateSourceType: stageType === 'SIGNOFF' ? 'DEPT' : 'USER',
  candidateSourceIds, approveMethod: 'ALL', approveRatio: 100, requireAllApprovals: true,
  subjectName: '正式主体', subjectId: candidateSourceIds.split(',')[0], subjectDepartmentPath: null, ...patch })
const group = (raw, applied) => ({ sourceNodeId: raw.id, stageType: raw.stageType, marker: raw.marker, required: raw.required,
  subjectType: raw.subjectType, candidateSourceType: raw.candidateSourceType, candidateSourceIds: raw.candidateSourceIds.split(','),
  approveMethod: raw.approveMethod, approveRatio: raw.approveRatio, requireAllApprovals: raw.requireAllApprovals, applied })
const response = () => {
  const raw = [original('1', 'SIGNOFF', '51'), original('2', 'SIGNOFF', '52'), original('3', 'APPROVAL', '99'),
    original('4', 'DOC_CONTROL', '100')]
  return { ready: true, executionContract: 'PUBLIC_MATRIX_NATIVE_V1', blockers: [], matrixAfter: { nodes: raw },
    runtimeRuleGroups: raw.map((node, index) => group(node, index !== 0)), runtimeCapabilityLimitations: [],
    nodes: [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ['52'], approveMethod: 'ALL', approveRatio: 100, requireAllApprovals: true },
      { stageCode: 'MATRIX_APPROVAL', candidateSourceType: 'USER', candidateSourceIds: ['99'], resolvedUserIds: ['99'], approveMethod: 'ALL', approveRatio: 100, requireAllApprovals: true }] }
}
const accounts = [{ id: '99', name: '正式批准人' }]

test('G04 removed mandatory A leaves B and does not block original approval projection or mutate public facts', () => {
  const r = response(), before = JSON.stringify(r.matrixAfter)
  assert.deepEqual(Array.from(h.normalizeSignoffDepartments(['52'])), ['52'])
  assert.deepEqual(Array.from(h.defaultSignoffDepartments(r)), ['52'])
  const result = h.resolvedUploadApprovers(r, accounts)
  assert.equal(result.groups.length, 1); assert.equal(result.groups[0].sourceNodeId, '3')
  assert.equal(result.groups[0].approveMethod, 'ALL'); assert.equal(result.groups[0].applied, true)
  assert.equal(JSON.stringify(r.matrixAfter), before)
  assert.equal(r.runtimeRuleGroups[0].required, true); assert.equal(r.runtimeRuleGroups[0].applied, false)
})

test('G04 empty explicit choice has the exact required hint and cannot restore defaults', () => {
  for (const value of [[], null, undefined])
    assert.throws(() => h.normalizeSignoffDepartments(value), { message: '请至少选择一个会签部门' })
  assert.throws(() => h.signoffRequestKey('7', [], 'CONTROLLED_FILE'), { message: '请至少选择一个会签部门' })
  assert.equal(h.normalizeSignoffDepartments(undefined, true), undefined)
})

test('G04 B plus added C remains the exact selected set without changing initial distribution departments', () => {
  const defaults = h.initialUploadDepartmentSelection({ ...response(), nodes: [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ['51', '52'] }] })
  const selected = h.normalizeSignoffDepartments(['52', '53'])
  assert.deepEqual(Array.from(selected), ['52', '53'])
  assert.deepEqual(Array.from(defaults.selectedDistributionDepartmentIds), ['51', '52'])
  assert.deepEqual(Array.from(defaults.selectedSignoffDepartmentIds), ['51', '52'])
})

test('G04 removed signoff permission does not permit skipping mandatory approval or document control groups', () => {
  for (const index of [2, 3]) {
    const r = response(); r.runtimeRuleGroups[index].applied = false
    assert.throws(() => h.resolvedUploadApprovers(r, accounts), /必需参与却未实际参与/)
  }
})

test('G04 a removed source signoff still requires intact original identity and policy evidence', () => {
  for (const change of [r => { r.runtimeRuleGroups[0].required = false }, r => { r.runtimeRuleGroups[0].candidateSourceIds = ['53'] },
    r => { r.runtimeRuleGroups[0].applied = undefined }, r => { r.runtimeRuleGroups[0].sourceNodeId = '999' }]) {
    const r = response(); change(r)
    assert.throws(() => h.resolvedUploadApprovers(r, accounts))
  }
})
