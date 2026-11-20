const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const source = fs.readFileSync(filename, 'utf8')
const { descriptor, errors } = parse(source, { filename })
assert.deepEqual(errors, [])
const compiled = compileScript(descriptor, { id: 'completion-prerequisites' })
const template = compileTemplate({
  source: descriptor.template.content, filename, id: 'completion-prerequisites',
  compilerOptions: { bindingMetadata: compiled.bindings }
})
assert.deepEqual(template.errors, [])

const script = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content,
  ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const required = new Set([
  'isActiveOrderProgressComplete', 'canApplyActiveOrderRelease',
  'resolveActiveOrderReleaseApplyDisabledReason'
])
const selected = script.statements.filter(statement => ts.isVariableStatement(statement)
  && statement.declarationList.declarations.some(declaration => required.has(declaration.name.getText(script))))
assert.equal(selected.length, required.size)
const executable = ts.transpileModule(selected.map(statement => statement.getText(script)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const context = vm.createContext({
  handoffReadOnly: { value: false }, releaseApplicationLocks: new Map(),
  formatActiveOrderReleaseStatus: value => value
})
vm.runInContext(executable + '\nthis.canApply = canApplyActiveOrderRelease; this.reason = resolveActiveOrderReleaseApplyDisabledReason;', context)

const ready = { id: 1, productionProgressPercent: 100, inspectionProgressPercent: 100,
  formalProductionReviewComplete: true }
assert.equal(context.canApply(ready), true)
const noReview = { ...ready, formalProductionReviewComplete: false }
assert.equal(context.canApply(noReview), false)
assert.match(context.reason(noReview), /生产复核未完成/)
const noBatch = { ...ready, releaseApplyBlockReason: '缺少正式生产批号，请由上游岗位维护后再申请完工' }
assert.equal(context.canApply(noBatch), false)
assert.match(context.reason(noBatch), /缺少正式生产批号/)
assert.equal(context.canApply({ ...ready, productionProgressPercent: 99 }), false)
assert.equal(context.canApply({ ...ready, inspectionProgressPercent: 99 }), false)
assert.equal(context.canApply({ ...ready, releaseApplicationStatus: 'PENDING' }), false)
assert.equal(context.canApply({ ...ready, readBlocked: true }), false)
assert.equal(context.canApply({ ...ready, abnormal: true }), false)
console.log('PASS: Vue script/template compile; completion requires double 100%, formal review and batch; existing blockers remain enforced')
