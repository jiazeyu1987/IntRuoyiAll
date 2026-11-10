const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const qs = require('qs')
const source = ts.createSourceFile('approval-center.ts', require('vue/compiler-sfc').parse(fs.readFileSync('src/views/approval-center/index.vue', 'utf8')).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
let request
function find(node) {
  if (ts.isCallExpression(node) && node.expression.getText(source) === 'getApprovalTaskTimeline') request = node.arguments[0].getText(source)
  ts.forEachChild(node, find)
}
find(source)
assert.ok(request, 'actual timeline request must exist')
const build = new Function('row', ts.transpileModule(`return (${request})`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)
const row = { moduleCode: 'DCC', sourceTaskType: 'DCC_PROJECT_PRODUCT_REVIEW', sourceTaskId: 'DCC_PROJECT_PRODUCT:1:10:DCC_PROJECT_PRODUCT_REVIEW', businessKey: '10' }
test('native project timeline omits null and blank Flowable identity in actual qs transport', () => {
  assert.match(qs.stringify({ processInstanceId: null }, { allowDots: true }), /processInstanceId=$/, 'reproduces original null serialization')
  for (const value of [null, undefined, '', '   ']) {
    const params = build({ ...row, processInstanceId: value })
    const query = new URLSearchParams(qs.stringify(params, { allowDots: true }))
    assert.equal(query.has('processInstanceId'), false)
    assert.equal(query.get('sourceTaskId'), row.sourceTaskId)
    assert.equal(query.get('businessKey'), row.businessKey)
  }
})
test('real BPM timeline retains its exact process identity', () => {
  const params = build({ ...row, processInstanceId: 'process-10' })
  assert.equal(new URLSearchParams(qs.stringify(params, { allowDots: true })).get('processInstanceId'), 'process-10')
})
