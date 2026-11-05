const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')

const source = fs.readFileSync('src/views/mes/pro/feedback/frontlinePqcCompletion.ts', 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText
const loaded = { exports: {} }
new Function('module', 'exports', compiled)(loaded, loaded.exports)
const { getCompletedPqcInspections } = loaded.exports
const task = (inspectionType, roundNo, taskStatus) => ({ inspectionType, roundNo, taskStatus })

test('only formally submitted or confirmed inspections count as completed', () => {
  assert.deepEqual(getCompletedPqcInspections([]), { first: false, patrolRounds: [], final: false })
  assert.deepEqual(getCompletedPqcInspections([
    task('FIRST', 1, 'SUBMITTED'),
    task('FINAL', 1, 'CONFIRMED'),
    task('PATROL', 3, 'CANCELLED'),
    task('PATROL', 4, 'PENDING')
  ]), { first: true, patrolRounds: [], final: true })
})

test('completed patrol rounds keep their original numbers in numeric order', () => {
  assert.deepEqual(getCompletedPqcInspections([
    task('PATROL', 12, 'CONFIRMED'),
    task('PATROL', 5, 'SUBMITTED'),
    task('PATROL', 1, 'SUBMITTED'),
    task('PATROL', 5, 'CONFIRMED'),
    task('PATROL', 3, 'CANCELLED'),
    task('PATROL', 2, 'SUBMITTED'),
    task('PATROL', 2, 'PENDING'),
    task('FIRST', 1, 'PENDING'),
    task('FINAL', 1, 'PENDING')
  ]), { first: false, patrolRounds: [1, 5, 12], final: false })
})

test('an incompletely submitted inspection stage does not receive a completion badge', () => {
  assert.deepEqual(getCompletedPqcInspections([
    task('FIRST', 1, 'SUBMITTED'),
    task('FIRST', 1, 'PENDING'),
    task('FINAL', 1, 'CONFIRMED'),
    task('FINAL', 1, 'CANCELLED')
  ]), { first: false, patrolRounds: [], final: false })
})
