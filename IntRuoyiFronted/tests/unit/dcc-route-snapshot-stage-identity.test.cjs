const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
const expression = source.match(/const getPreviewApprovalProgress = \(snapshot: ControlledFileRouteSnapshotVO\) =>\s*([\s\S]*?)\r?\n\r?\ntype ControlledFileDepartmentObligationDetail/)
assert.ok(expression, 'read the actual function shared by both route tables')
const resolve = new Function('stageProgressList', 'snapshot', 'return ' + expression[1])
const stages = [
  { stageCode: 'SUPERVISOR_REVIEW', stageOrder: 1, stageName: '直属主管审核', completionText: '1/1', isCompleted: true, isCurrent: false },
  { stageCode: 'MATRIX_REVIEW', stageOrder: 2, stageName: '会签', completionText: '1/2', isCompleted: false, isCurrent: true },
  { stageCode: 'MATRIX_APPROVAL', stageOrder: 3, stageName: '批准', completionText: '0/0', isCompleted: false, isCurrent: false },
  { stageCode: 'DOC_CONTROL_REVIEW', stageOrder: 4, stageName: '文控标准化审核', completionText: '0/0', isCompleted: false, isCurrent: false }
]

for (const [index, stage] of stages.entries()) {
  test(`${stage.stageCode} retains its own name, completion and state when snapshot numbers differ`, () => {
    const actual = resolve({ value: stages }, { stageCode: stage.stageCode, stageNo: index })
    assert.strictEqual(actual, stage)
  })
}

test('an unknown stage cannot borrow a completed stage with the same order', () => {
  assert.equal(resolve({ value: stages }, { stageCode: 'UNKNOWN', stageOrder: 1 }), undefined)
})

test('a snapshot without stage identity cannot borrow another stage by number', () => {
  assert.equal(resolve({ value: stages }, { stageNo: 1 }), undefined)
})

test('public snapshots with matching stage numbers still use their own stage', () => {
  for (const stage of stages) {
    assert.strictEqual(resolve({ value: stages }, {
      stageCode: stage.stageCode, stageOrder: stage.stageOrder
    }), stage)
  }
})
