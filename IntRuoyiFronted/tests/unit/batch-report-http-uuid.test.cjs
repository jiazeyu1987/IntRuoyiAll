const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('report.ts', script, ts.ScriptTarget.Latest, true)
const names = ['createProductionReleaseReportIdempotencyKey', 'submitSpecialNodeComplete']
const statements = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
const code = ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n') +
  '\nexports.createKey = createProductionReleaseReportIdempotencyKey; exports.submit = submitSpecialNodeComplete;', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const httpCrypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }
const uuidPattern = '[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}'

function setup(crypto = httpCrypto) {
  const exports = {}, writes = [], errors = [], keys = {}
  const context = {
    exports, ...loadUuid(crypto),
    currentSpecialNode: { value: { id: '9007199254740993', nodeType: 'STERILIZATION_REPORT' } },
    specialNodeCompleteError: { value: '' }, specialNodeCompleteLoading: { value: false },
    specialNodeActionLoading: {}, specialNodeCompleteDialogVisible: { value: true },
    specialNodeCompleteForm: { sterilizationBatchNo: 'BATCH-1' },
    productionReleaseReportStageReceipt: { value: null },
    productionReleaseReportCompleteIdempotencyKeys: keys,
    isSterilizationNode: () => false, isProductionReleaseReportNode: () => true,
    buildSpecialNodeSubmitAttachments: () => [{ fileId: '9007199254740997' }],
    requireProductionReleaseReportWorkTask: () => ({
      id: '9007199254740995', batchTaskId: '9007199254740993', version: 7
    }),
    requireProductionReleaseReportStringId: value => value,
    requireProductionReleaseReportVersion: value => value,
    completeEdhrProductionReleaseReportNode: async request => {
      writes.push(request)
      throw new Error('request timed out')
    },
    resolveErrorMessage: error => error.message,
    message: { error: text => errors.push(text), success() {} },
    assertProductionReleaseReportCompleteResult() {},
    resolveProductionReleaseReportStageReceiptText: () => 'completed',
    loadDetail: async () => {}, clearSpecialNodePendingAttachments() {}
  }
  vm.runInNewContext(code, context)
  return { ...exports, context, writes, errors, keys }
}

test('report actions generate prefixed UUID v4 with HTTP random source and preserve complete retry key', async () => {
  const p = setup()
  const attachmentKey = p.createKey('attachment', '9007199254740995')
  assert.match(attachmentKey, new RegExp(`^report-attachment-9007199254740995-${uuidPattern}$`))
  await p.submit()
  await p.submit()
  assert.equal(p.writes.length, 2)
  assert.match(p.writes[0].idempotencyKey, new RegExp(`^report-complete-9007199254740995-${uuidPattern}$`))
  assert.equal(p.writes[0].idempotencyKey, p.writes[1].idempotencyKey)
  assert.notEqual(p.writes[0].idempotencyKey, attachmentKey)
  assert.equal(p.writes[0].taskId, '9007199254740993')
  assert.equal(p.writes[0].expectedVersion, 7)
  assert.equal(p.context.specialNodeCompleteLoading.value, false)
  assert.equal(p.context.specialNodeCompleteDialogVisible.value, true)
})

test('missing or failing random source prevents report write and restores busy state', async () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy unavailable') } }]) {
    const p = setup(crypto)
    await p.submit()
    assert.equal(p.writes.length, 0)
    assert.deepEqual(p.keys, {})
    assert.equal(p.errors.length, 1)
    assert.match(p.errors[0], /不支持安全随机数|entropy unavailable/)
    assert.equal(p.context.specialNodeCompleteLoading.value, false)
    assert.equal(p.context.specialNodeActionLoading['9007199254740993'], undefined)
    assert.equal(p.context.specialNodeCompleteDialogVisible.value, true)
  }
})
