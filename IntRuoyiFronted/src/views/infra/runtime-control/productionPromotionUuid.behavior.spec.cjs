const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../../../../tests/helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.join(__dirname, 'index.vue'), 'utf8')
const script = source.match(/<script lang="ts" setup>([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('runtime-control.ts', script, ts.ScriptTarget.Latest, true)
const names = new Set(['clearProductionPreview', 'previewProductionPromotion', 'confirmProductionPromotion'])
const selected = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast))))
const code = ts.transpileModule(selected.map(statement => statement.getText(ast)).join('\n') +
  '\nexports.actions = { clearProductionPreview, previewProductionPromotion, confirmProductionPromotion };', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(crypto) {
  const errors = [], submissions = []
  const context = {
    exports: {}, ...loadUuid(crypto),
    workflowReason: { value: 'approved test release' },
    workflowBusy: { value: false }, canPromoteProd: { value: true },
    productionPreview: { value: undefined },
    productionDialog: { visible: false, confirmText: '', workflowId: '', idempotencyKey: '' },
    reportActionError: error => errors.push(error), loadReleaseWorkflows: async () => {},
    RuntimeControlApi: {
      previewRuntimeControlReleaseWorkflowProduction: async () => ({
        eligible: true, previewId: 'preview-1', expectedStateVersion: 5
      }),
      authorizeRuntimeControlReleaseWorkflowProduction: async () => ({ grantId: 'grant-1' }),
      promoteRuntimeControlReleaseWorkflowProduction: async (id, command) => {
        submissions.push({ id, ...command })
        throw new Error('transport failure')
      }
    }
  }
  vm.runInNewContext(code, context)
  return { ...context, ...context.exports.actions, errors, submissions }
}

const workflow = { workflowId: 'workflow-1', state: 'TESTED', stateVersion: 5 }

test('HTTP preview creates UUID v4 and a failed promotion retries with the same key', async () => {
  const runtime = setup({ getRandomValues: bytes => webcrypto.getRandomValues(bytes) })
  await runtime.previewProductionPromotion(workflow)
  const key = runtime.productionDialog.idempotencyKey
  assert.match(key, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  assert.equal(runtime.productionDialog.visible, true)
  assert.equal(runtime.errors.length, 0)
  runtime.productionDialog.confirmText = 'PROD'
  await runtime.confirmProductionPromotion()
  await runtime.confirmProductionPromotion()
  assert.equal(runtime.submissions.length, 2)
  assert.ok(runtime.submissions.every(command => command.idempotencyKey === key))
  assert.equal(runtime.workflowBusy.value, false)
  runtime.clearProductionPreview()
  assert.equal(runtime.productionDialog.idempotencyKey, '')
})

test('missing secure random source reports failure without opening the promotion dialog', async () => {
  const runtime = setup({})
  await runtime.previewProductionPromotion(workflow)
  assert.equal(runtime.errors.length, 1)
  assert.match(runtime.errors[0].message, /安全随机数/)
  assert.equal(runtime.productionDialog.visible, false)
  assert.equal(runtime.productionDialog.idempotencyKey, '')
  assert.equal(runtime.submissions.length, 0)
  assert.equal(runtime.workflowBusy.value, false)
})
