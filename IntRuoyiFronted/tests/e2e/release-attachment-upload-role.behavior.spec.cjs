const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const roles = [
  'BATCH_ATTACHMENT_INCOMING_INSPECTION_REPORT_UPLOAD_1',
  'BATCH_ATTACHMENT_STERILIZATION_REPORT_UPLOAD_1',
  'BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_REPORT_UPLOAD_1',
  'BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_RECORD_UPLOAD_1'
]
const pages = ['src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue']

function evaluateVariable(file, name, context) {
  const source = parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(statement => ts.isVariableStatement(statement) &&
    statement.declarationList.declarations.some(item => item.name.getText(ast) === name))
  assert.ok(declaration, `${file} must expose ${name}`)
  const code = ts.transpileModule(declaration.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${code};return ${name};`)(...Object.values(context))
}

test('four upload roles each select only upload actions on list and dossier panel', () => {
  for (const page of pages) {
    for (const role of roles) {
      const userStore = { roles: [role], permissions: new Set(['mes:pro-edhr-batch-execution:query', 'mes:pro-edhr-batch-execution:upload']) }
      const value = evaluateVariable(page, 'uploadOnly', { userStore, computed: callback => ({ get value() { return callback() } }) })
      assert.equal(value.value, true, `${page}: ${role}`)
      userStore.roles = ['SOME_OTHER_UPLOAD_ROLE']
      assert.equal(value.value, false, 'ordinary upload permissions keep their existing behavior')
      userStore.roles = [role]
      userStore.permissions.add('mes:pro-edhr-release:approve')
      assert.equal(value.value, false, 'additional independent approval role retains its permissions')
    }
  }
})

test('upload task categories open the matching active-order dossier tab', () => {
  for (const key of ['INCOMING_INSPECTION_FILE', 'STERILIZATION_FILE', 'FINISHED_PRODUCT_FILE']) {
    const value = evaluateVariable('src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue', 'initialActiveTab', {
      route: { query: { tab: key } }, computed: callback => ({ get value() { return callback() } })
    })
    assert.equal(value.value, `dossierFile:${key}`)
  }
})

test('personal workbench navigation preserves exact upload task, batch and category', async () => {
  const module = { exports: {} }
  const code = ts.transpileModule(fs.readFileSync(path.join(root, 'src/utils/edhrWorkTaskNavigation.ts'), 'utf8'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText
  const dependency = name => {
    if (name.endsWith('/exactJavaLong')) return { requireExactJavaLong: value => String(value) }
    if (name.endsWith('/batchExecution')) return { openEdhrBatchTask: () => assert.fail('upload task must directly open active order') }
    throw new Error(`Unexpected import ${name}`)
  }
  Function('require', 'module', 'exports', code)(dependency, module, module.exports)
  const calls = []
  const id = '9007199254740993'
  const item = { id, taskType: 'DOSSIER_COMPLETE', actionUrl: `/mes/pro/feedback/edhr-batch-execution/active-order-detail?batchExecutionId=8001&from=execution&tab=FINISHED_PRODUCT_FILE&workTaskId=${id}` }
  assert.equal(module.exports.EDHR_WORK_TASK_NOTIFY_PATHS.has('/mes/pro/feedback/edhr-batch-execution/active-order-detail'), true)
  await module.exports.navigateToEdhrWorkTask({ push: target => calls.push(target) }, item, 'https://mes.example')
  assert.equal(calls.length, 1)
  assert.equal(calls[0].path, '/mes/pro/feedback/edhr-batch-execution/active-order-detail')
  assert.equal(calls[0].query.workTaskId, id)
  assert.equal(calls[0].query.tab, 'FINISHED_PRODUCT_FILE')
  assert.equal(calls[0].query.batchExecutionId, '8001')
})
