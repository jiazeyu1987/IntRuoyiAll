const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const loaded = { exports: {} }
const code = ts.transpileModule(
  fs.readFileSync('src/views/mes/pro/feedback/frontlinePqcProgress.ts', 'utf8'),
  {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }
).outputText
new Function('module', 'exports', code)(loaded, loaded.exports)
const { getPqcProcessProgress } = loaded.exports
const task = (inspectionRuleKey, taskStatus, roundNo = 1) => ({
  inspectionRuleKey,
  taskStatus,
  roundNo
})
const process = (...pqcTaskOptions) => ({
  pqcTaskOptions,
  inspectionTypeRules: ['FIRST', 'PATROL', 'FINAL'].map((key) => ({ key, required: true })),
  inspectionItems: [{ applicableInspectionTypes: ['FIRST', 'PATROL', 'FINAL'] }]
})

test('eight processes with three formally submitted display 3/8; item tasks count once', () => {
  const processes = Array.from({ length: 8 }, (_, i) =>
    process(
      task('FIRST', i < 3 ? 'SUBMITTED' : 'PENDING'),
      task('FIRST', i < 3 ? 'CONFIRMED' : 'PENDING')
    )
  )
  assert.deepEqual(getPqcProcessProgress(processes, 'FIRST'), { submittedCount: 3, totalCount: 8 })
  processes[3].pqcTaskOptions[0].taskStatus = 'SUBMITTED'
  assert.equal(getPqcProcessProgress(processes, 'FIRST').submittedCount, 3)
  processes[3].pqcTaskOptions[1].taskStatus = 'SUBMITTED'
  assert.equal(getPqcProcessProgress(processes, 'FIRST').submittedCount, 4)
})

test('patrol progress is isolated by round, latest active round used before selection', () => {
  const processes = [
    process(
      task('PATROL', 'CONFIRMED'),
      task('PATROL', 'SUBMITTED', 2),
      task('PATROL', 'CANCELLED', 3)
    ),
    process(
      task('PATROL', 'CONFIRMED'),
      task('PATROL', 'PENDING', 2),
      task('PATROL', 'CANCELLED', 3)
    )
  ]
  assert.deepEqual(getPqcProcessProgress(processes, 'PATROL', 1), {
    submittedCount: 2,
    totalCount: 2
  })
  assert.deepEqual(getPqcProcessProgress(processes, 'PATROL', 2), {
    submittedCount: 1,
    totalCount: 2
  })
  assert.deepEqual(getPqcProcessProgress(processes, 'PATROL'), { submittedCount: 1, totalCount: 2 })
  assert.deepEqual(getPqcProcessProgress(processes, 'PATROL', 3), {
    submittedCount: 0,
    totalCount: 2
  })
})

test('uncreated inspection counts applicable processes, excluding nonapplicable ones', () => {
  const excluded = { pqcTaskOptions: [], inspectionTypeRules: [], inspectionItems: [] }
  assert.deepEqual(getPqcProcessProgress([process(), process(), excluded], 'FINAL'), {
    submittedCount: 0,
    totalCount: 2
  })
  assert.deepEqual(getPqcProcessProgress([process(), excluded], 'PATROL'), {
    submittedCount: 0,
    totalCount: 1
  })
  assert.deepEqual(getPqcProcessProgress([process(), excluded], 'FIRST'), {
    submittedCount: 0,
    totalCount: 0
  })
  assert.deepEqual(getPqcProcessProgress([], 'FIRST'), { submittedCount: 0, totalCount: 0 })
})
