const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const ts = require('typescript'), vue = require('vue')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/detail/DistributionReadPanel.vue'), 'utf8')
function panel(initial) {
  let metadata = initial, posts = 0
  const hooks = {}, task = { controlledFileId: '1', distributionId: '2', recipientId: '3', versionNo: 'A/1', approvalProcessInstanceId: 'round-1' }
  const sandbox = { exports: {}, Error, defineProps: () => ({ fileId: '1', query: {}, contextKey: 'first' }),
    useRouter: () => ({ push() {} }), require: id => {
      if (id === 'vue') return { ...vue, watch: () => {}, onActivated: fn => { hooks.activated = fn }, onDeactivated: fn => { hooks.deactivated = fn }, onBeforeUnmount: () => {} }
      if (id === '@/utils/dccDistributionNavigation') return { readDccDistributionRoute: () => task }
      if (id === '@/api/dcc/controlledFile/workflow') return {
        getControlledFilePreviewMetadata: async () => metadata,
        acknowledgeElectronicDistribution: async () => { posts++; return true }
      }
      return {}
    } }
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  vm.runInNewContext(ts.transpileModule(script + '\nexport { load, saveReceipt, receiptAcknowledged, authorized, error }', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, sandbox)
  return { ...sandbox.exports, task, hooks, setMetadata: value => { metadata = value }, posts: () => posts }
}
const metadata = signed => ({ receiptAcknowledged: signed, fileName: '文件.docx' })
const command = task => ({ ...task, password: 'test-password' })

test('real preview metadata parser preserves both receipt states and rejects invalid values', () => {
  const api = fs.readFileSync(path.resolve(__dirname, '../../src/api/dcc/controlledFile/workflow.ts'), 'utf8')
  const ast = ts.createSourceFile('api.ts', api, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === 'parseControlledFilePreviewMetadata'))
  const context = { exports: {}, Error, Date, DccControlledFileContractError: Error,
    assertRecordPayload: x => x, assertNoForbiddenDccFileCapabilityFields() {},
    readOptionalString: (x, k) => x[k], assertRequiredString: (x, k) => x[k],
    assertRequiredPreviewKind: (x, k) => x[k], assertRequiredObject: (x, k) => x[k], parseControlledPreviewWatermark: x => x }
  vm.runInNewContext(ts.transpileModule(declaration.getText(ast), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  const parse = context.exports.parseControlledFilePreviewMetadata
  const base = { previewRegisteredAt: '2026-11-25T01:00:00Z' }
  for (const receiptAcknowledged of [false, true]) {
    assert.equal(parse({ ...base, receiptAcknowledged }).receiptAcknowledged, receiptAcknowledged)
  }
  assert.equal(parse(base).receiptAcknowledged, undefined)
  assert.throws(() => parse({ ...base, receiptAcknowledged: 'false' }), /receiptAcknowledged/)
})
test('receipt signed in workbench stays readable and has no signature form in notification', async () => {
  const p = panel(metadata(true)); await p.load()
  assert.equal(p.authorized.value, true); assert.equal(p.receiptAcknowledged.value, true)
  assert.match(source, /<el-alert v-if="receiptAcknowledged"/)
  assert.match(source, /<DistributionReceiptConfirm v-else/)
  await assert.rejects(p.saveReceipt(command(p.task)), /已签收/)
  assert.equal(p.posts(), 0)
})
test('a second entry signing after opening is rechecked before sending a command', async () => {
  const p = panel(metadata(false)); await p.load()
  p.setMetadata(metadata(true))
  await assert.rejects(p.saveReceipt(command(p.task)), /已签收/)
  assert.equal(p.receiptAcknowledged.value, true); assert.equal(p.posts(), 0)
})
test('unsigned receipt still submits exactly once', async () => {
  const p = panel(metadata(false)); await p.load()
  assert.equal(await p.saveReceipt(command(p.task)), true); assert.equal(p.posts(), 1)
})
test('missing receipt state fails visibly without enabling signing', async () => {
  const p = panel({ fileName: '文件.docx' }); await p.load()
  assert.equal(p.authorized.value, false); assert.match(p.error.value, /签收状态读取失败/)
  assert.equal(p.posts(), 0)
})
test('returning to cached notification reloads receipt state', async () => {
  const p = panel(metadata(false)); await p.load(); p.hooks.deactivated()
  assert.equal(p.authorized.value, false)
  p.setMetadata(metadata(true)); p.hooks.activated()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(p.receiptAcknowledged.value, true)
})
