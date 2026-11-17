const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const compile = code => ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const declarations = file => {
  const content = parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('source.ts', content, ts.ScriptTarget.Latest, true)
  const result = new Map()
  for (const statement of ast.statements) if (ts.isVariableStatement(statement)) {
    for (const declaration of statement.declarationList.declarations) result.set(declaration.name.getText(ast), `const ${declaration.getText(ast)};`)
  }
  return result
}
const evaluate = (decs, names, context) => {
  vm.createContext(context)
  vm.runInContext(compile(names.map(name => decs.get(name)).join('\n') + `\nthis.result = { ${names.join(', ')} };`), context)
  return context.result
}
const leader = declarations('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
test('historical handoff blocks order mutations including stale dialog submission', async () => {
  const names = ['handoffReadOnly','requireWritableHandoff','openAbnormalDialog','submitAbnormal','openActiveOrderDialog','submitAddActiveOrder','submitActiveOrderReleaseApplication','handleRebuildActiveOrder','handleActiveOrderVersionUpgrade','submitActiveOrderVersionUpgrade','handleRemoveActiveOrder','receivePqcCorrection','submitReview','submitCorrection']
  const context = { route: { query: { handoffReadOnly: '1' } }, computed: fn => ({ get value() { return fn() } }), ElMessage: { warning() {} } }
  const subject = evaluate(leader, names, context)
  for (const name of names.slice(2)) await subject[name]({ id: '48' })
  assert.equal(subject.handoffReadOnly.value, true)
})
test('normal workbench preserves order creation and completion eligibility', () => {
  const context = { route: { query: {} }, computed: fn => ({ get value() { return fn() } }), activeOrderAddDialogVisible: { value: false }, resetActiveOrderForm() {}, activeOrderTestResetSubmitting: { value: false }, isActiveOrderProgressComplete: value => value === 100 }
  const subject = evaluate(leader, ['handoffReadOnly','requireWritableHandoff','openActiveOrderDialog','canApplyActiveOrderRelease'], context)
  subject.openActiveOrderDialog()
  assert.equal(context.activeOrderAddDialogVisible.value, true)
  assert.equal(subject.canApplyActiveOrderRelease({ productionProgressPercent: 100, inspectionProgressPercent: 100 }), true)
})
test('historical dossier remains locked even when a formal upload task exists', () => {
  const context = { props: { handoffReadOnly: true, readOnly: true }, dossierFiles: { value: { uploadTasks: [{}] } }, computed: fn => ({ get value() { return fn() } }) }
  const subject = evaluate(declarations('src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), ['activeOrderDossierMutationLockReason'], context)
  assert.match(subject.activeOrderDossierMutationLockReason.value, /仅可查看/)
})
test('notification attempt and delivery times use Asia/Shanghai independently of local browser timezone', () => {
  const context = { exports: {}, Intl, Date }
  vm.runInNewContext(compile(fs.readFileSync(path.join(root, 'src/utils/notificationTime.ts'), 'utf8')), context)
  const format = context.exports.formatNotificationSentAt
  assert.equal(format(Date.parse('2026-11-15T00:01:02Z')), '2026-11-15 08:01:02')
  assert.equal(format(String(Date.parse('2026-11-15T00:01:02Z'))), '2026-11-15 08:01:02')
  assert.equal(format('2026-11-15T08:01:02+08:00'), '2026-11-15 08:01:02')
  const receipt = { lastAttemptAt: Date.parse('2026-11-15T00:01:02Z'), sentAt: Date.parse('2026-11-15T00:01:03Z') }
  assert.equal(format(receipt.lastAttemptAt), '2026-11-15 08:01:02')
  assert.equal(format(receipt.sentAt), '2026-11-15 08:01:03')
  assert.equal(format(null), '未记录')
  assert.match(format('2026-11-15 08:01:02'), /异常/)
  assert.match(format('invalid'), /通知时间异常/)
  assert.match(format('2026-02-30T00:00:00Z'), /异常/)
  assert.match(format(Infinity), /异常/)
})
test('affected Vue scripts and templates compile', () => {
  for (const file of ['processpool/TeamLeaderWorkbenchPage.vue','processpool/ActiveOrderSubmissionDetailPage.vue','processpool/components/ActiveOrderSubmissionDetailPanel.vue','feedback/FrontlineFixedTemplatePanel.vue','handoff/ActiveOrderHandoffPanel.vue','production-release/components/ReleaseTaskNotificationEntry.vue']) {
    const filename = path.join(root,'src/views/mes/pro',file)
    const { descriptor, errors } = parse(fs.readFileSync(filename,'utf8'), { filename })
    assert.deepEqual(errors, [], file)
    const script = compileScript(descriptor, { id: file })
    const template = compileTemplate({ id: file, filename, source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(template.errors, [], file)
  }
})

test('material totals display independently from the process minimum', () => {
  const context = { deviceState: { selectedProcess: { submittedQuantity: 0, outputSubmittedQuantities: { 9001: 5000, 9002: 0 } } }, isFrontlineProductionProcess: () => true }
  const subject = evaluate(declarations('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), ['formatProductionSubmittedQuantity', 'productionOutputSubmittedQuantity'], context)
  assert.match(subject.productionOutputSubmittedQuantity({ materialId: 9001 }), /5000/)
  assert.match(subject.productionOutputSubmittedQuantity({ materialId: 9002 }), /0 件/)
  assert.match(subject.productionOutputSubmittedQuantity({ materialId: 9003 }), /缺失/)
})
