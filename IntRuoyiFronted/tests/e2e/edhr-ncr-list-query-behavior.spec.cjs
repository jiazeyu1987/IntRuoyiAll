const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')

// Execute the page's query builder; this is a focused behavior check, not browser E2E.
const page = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'),
  'utf8'
)
const script = page.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)
assert.ok(script, 'review page must contain its script')
const source = ts.createSourceFile('review.ts', script[1], ts.ScriptTarget.Latest, true)
const declaration = source.statements
  .filter(ts.isVariableStatement)
  .flatMap((statement) => [...statement.declarationList.declarations])
  .find((item) => item.name.getText(source) === 'buildReviewQuery')
assert.ok(declaration?.initializer, 'review page must provide its list query builder')
const executable = ts.transpileModule(
  `(${declaration.initializer.getText(source)})()`,
  { compilerOptions: { target: ts.ScriptTarget.ES2022 } }
).outputText

const batchPage = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)
const batchScript = batchPage.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)
assert.ok(batchScript, 'batch detail must contain its script')
const batchSource = ts.createSourceFile('batch.ts', batchScript[1], ts.ScriptTarget.Latest, true)
const entryDeclaration = batchSource.statements
  .filter(ts.isVariableStatement)
  .flatMap((statement) => [...statement.declarationList.declarations])
  .find((item) => item.name.getText(batchSource) === 'openNonconformanceReviewEntry')
assert.ok(entryDeclaration?.initializer, 'batch detail must provide its review entry')
const entryExecutable = ts.transpileModule(
  `(${entryDeclaration.initializer.getText(batchSource)})()`,
  { compilerOptions: { target: ts.ScriptTarget.ES2022 } }
).outputText

test('batch detail review entry opens creation for the formal active order', () => {
  let destination
  vm.runInNewContext(entryExecutable, {
    ensureViewedReleaseStageWritable: () => true,
    canOpenNonconformanceReview: { value: true },
    detail: { value: { id: 789, activeOrderId: 123 } },
    SOURCE_TYPE_PQC_RELEASE: 'PQC_RELEASE',
    traceRecordReleaseTransactionId: { value: 456 },
    assertBatchExecutionId: () => 789,
    router: { push: (value) => { destination = value } },
    message: { error: (value) => assert.fail(value) }
  })
  assert.equal(destination?.name, 'MesProFeedbackEdhrNonconformanceReview')
  assert.equal(String(destination.query.activeOrderId), '123')
  assert.equal(destination.query.autoCreate, '1')
  for (const key of ['sourceType', 'sourceId', 'batchExecutionId']) {
    assert.equal(Object.hasOwn(destination.query, key), false)
  }
})

for (const activeOrderId of [undefined, 123]) {
  for (const tab of ['all', 'pending']) {
    test(`${tab} list ignores source and batch route context; active order ${activeOrderId}`, () => {
      const queryParams = { pageNo: 2, pageSize: 10 }
      const result = vm.runInNewContext(executable, {
        queryParams,
        activeTab: { value: tab },
        REVIEW_STATUS_PENDING_REVIEW: 'pending_review',
        entryActiveOrderId: { value: activeOrderId },
        entrySourceType: { value: 'PQC_RELEASE' },
        entrySourceId: { value: '456' },
        entryBatchExecutionId: { value: '789' }
      })
      const expected = { pageNo: 2, pageSize: 10 }
      if (tab === 'pending') expected.reviewStatus = 'pending_review'
      assert.deepEqual(JSON.parse(JSON.stringify(result)), expected)
      assert.deepEqual(queryParams, { pageNo: 2, pageSize: 10 })
    })
  }
}
