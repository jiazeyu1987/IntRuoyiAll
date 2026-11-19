const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { ref } = require('vue')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/index.vue'), 'utf8')
const ast = ts.createSourceFile('page.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const uuid = '[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}'
function loadFunction(name, context) {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  const js = ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${js}; return ${name}`)(...Object.values(context))
}
function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const writes = []
  const context = {
    ...loadUuid(crypto), createError: ref(''), createForm: {}, createFormRef: ref({ clearValidate() {}, validate: async () => true }),
    orderOptions: ref([]), createVisible: ref(false), createLoading: ref(false), loadOrderOptions: async () => {},
    createDeviation: async data => { writes.push(data); throw new Error('write failed') },
    message: { success() {}, error() {} }, load: async () => {},
    batchNcrLoading: ref(false), batchNcrVisible: ref(true),
    batchNcrForm: { qaReviewerUserId: 1, nonconformanceReason: 'reason', signaturePassword: 'unit-test', remark: '' },
    selectedCritical: ref([{ id: 7, batchExecutionId: 31, activeOrderId: 41 }]), isCriticalOpen: () => true,
    batchNcrGuard: { capture: () => () => true }, approvalInvalidation: { syncSession: () => ({ key: 'session' }), isCurrent: () => true },
    createCriticalDeviationReview: async data => { writes.push(data); throw new Error('write failed') }
  }
  return { context, writes, open: loadFunction('openCreate', context), submit: loadFunction('submitCreate', context), batch: loadFunction('submitBatchNcr', context) }
}
test('HTTP creates UUID audit keys, preserves create retry key and existing per-submit NCR key lifecycle', async () => {
  const p = setup()
  await p.open()
  const first = p.context.createForm.idempotencyKey
  assert.match(first, new RegExp(`^EDHR_DEVIATION_${uuid}$`))
  await p.submit(); await p.submit()
  assert.equal(p.writes[0].idempotencyKey, first)
  assert.equal(p.writes[1].idempotencyKey, first)
  await p.open()
  assert.notEqual(p.context.createForm.idempotencyKey, first)
  await p.batch(); await p.batch()
  assert.match(p.writes[2].idempotencyKey, new RegExp(`^EDHR_DEVIATION_NCR_SOURCE_31_${uuid}$`))
  assert.notEqual(p.writes[2].idempotencyKey, p.writes[3].idempotencyKey)
  p.context.selectedCritical.value = [{ id: 8, activeOrderId: 42 }]
  await p.batch()
  assert.match(p.writes[4].idempotencyKey, new RegExp(`^EDHR_DEVIATION_NCR_SOURCE_42_${uuid}$`))
})
test('missing or failed secure entropy stops create opening and NCR write without busy state', async () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy unavailable') } }]) {
    const p = setup(crypto)
    await assert.rejects(p.open(), /安全随机数|entropy unavailable/)
    assert.equal(p.context.createVisible.value, false)
    await assert.rejects(p.batch(), /安全随机数|entropy unavailable/)
    assert.equal(p.context.batchNcrLoading.value, false)
    assert.equal(p.writes.length, 0)
  }
})
