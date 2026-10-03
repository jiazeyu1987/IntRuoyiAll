const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const source = fs.readFileSync(path.join(__dirname, 'TeamLeaderWorkbenchPage.vue'), 'utf8')
  .match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('workbench.ts', source, ts.ScriptTarget.Latest, true)
const handler = ast.statements.find(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.some(d => d.name.getText(ast) === 'openCorrection'))
const code = ts.transpileModule(handler.getText(ast) + '\nreturn openCorrection',
  { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
function harness(detail, failure) {
  const opened = [], errors = [], reads = []
  const dependencies = {
    requirePositiveNumber: value => value,
    isPqcSubmissionRow: () => true,
    canCorrectSubmission: row => !row.released,
    getTeamLeaderSubmissionDetail: async (...args) => {
      reads.push(args)
      if (failure) throw failure
      return detail
    },
    resolveCurrentLeaderType: () => 'PQC',
    openPqcCorrection: (...args) => opened.push(args),
    ElMessage: { error: message => errors.push(message) },
    resolveErrorMessage: error => error.message
  }
  return { opened, errors, reads,
    open: new Function(...Object.keys(dependencies), code)(...Object.values(dependencies)) }
}
test('group correction uses the exact formal event payload, not merged member items', async () => {
  const detail = { id: 101, originalPayloadJson: '{"items":["A"]}' }
  const h = harness(detail)
  await h.open({ id: 101, groupedEventIds: [101, 102], originalPayloadJson: '{"items":["A","B"]}' })
  assert.deepEqual(h.reads, [[101, 'PQC']])
  assert.deepEqual(h.opened, [[detail, 101]])
})
test('a detail read failure is visible and does not open stale grouped data', async () => {
  const h = harness(undefined, new Error('Formal detail unavailable'))
  await h.open({ id: 101, groupedEventIds: [101, 102] })
  assert.deepEqual(h.opened, [])
  assert.deepEqual(h.errors, ['Formal detail unavailable'])
})
test('a newly released or mismatched formal event cannot be corrected', async () => {
  for (const detail of [{ id: 101, released: true }, { id: 999 }]) {
    const h = harness(detail)
    await h.open({ id: 101, groupedEventIds: [101, 102] })
    assert.deepEqual(h.opened, [])
    assert.equal(h.errors.length, 1)
  }
})
