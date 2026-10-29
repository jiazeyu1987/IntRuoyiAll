const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/DeviationDetail.vue'), 'utf8')
const saveMethod = source.match(/const saveHandling = (async \(\) => \{[\s\S]*?)\nconst canSignSavedHandling/)[1]
const openMethod = source.match(/const openSignDialog = (\(node: string\) => \{[\s\S]*?)\nconst startHandling/)[1]

function harness() {
  const calls = [], messages = []
  const context = {
    props: { id: 901, readonly: false }, loadSequence: 1,
    handlingForm: { expectedContentVersion: 0, revisionReason: '', reviewerAssignments: {}, correctiveOwnerId: undefined },
    signNodeOptions: { value: ['PREPARER', 'VERIFIER', 'DEPARTMENT_OWNER', 'QA', 'QUALITY_OWNER'] },
    handlingSaving: { value: false },
    normalizeOptionalDateTime: value => value || null, normalizeOptionalJson: value => value || null,
    saveDeviationHandling: async (id, body) => { calls.push({ id, body }); return { contentVersion: 1 } },
    applyHandling: record => calls.push({ record }),
    ElMessage: { error: text => messages.push(text), success: () => {} },
    canSignSavedHandling: () => true, mySignNodes: { value: ['VERIFIER'] },
    signForm: { node: 'PREPARER', password: 'old', comment: 'old' }, signVisible: { value: false }
  }
  const compile = expression => ts.transpileModule(`(${expression})`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return { context, calls, messages, save: vm.runInNewContext(compile(saveMethod), context), open: vm.runInNewContext(compile(openMethod), context) }
}

test('saving without explicit responsibility owners does not send a business mutation', async () => {
  const h = harness()
  await h.save()
  assert.equal(h.calls.length, 0)
  assert.match(h.messages[0], /责任人/)
})

test('selected responsibility IDs including Long identities are submitted without guessing or rounding', async () => {
  const h = harness()
  h.context.handlingForm.reviewerAssignments = Object.fromEntries(h.context.signNodeOptions.value.map(node => [node, '9007199254740993']))
  h.context.handlingForm.correctiveOwnerId = '9007199254740995'
  await h.save()
  assert.equal(h.calls[0].body.reviewerAssignments.QA, '9007199254740993')
  assert.equal(h.calls[0].body.correctiveOwnerId, '9007199254740995')
  assert.equal(h.context.handlingSaving.value, false)
})

test('current actor can open only an explicitly assigned signature node', () => {
  const h = harness()
  h.open('QA')
  assert.equal(h.context.signVisible.value, false)
  h.open('VERIFIER')
  assert.equal(h.context.signVisible.value, true)
  assert.equal(h.context.signForm.node, 'VERIFIER')
  assert.equal(h.context.signForm.password, '')
})
