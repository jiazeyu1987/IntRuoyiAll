const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const Vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const { renderToString } = require('vue/server-renderer')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const source = fs.readFileSync(filename, 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const ast = ts.createSourceFile('workbench.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['resolveActiveOrderRowClassName', 'canApplyActiveOrderRelease']
const declarations = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
assert.equal(declarations.length, names.length)
const compiled = ts.transpileModule(declarations.map(statement => statement.getText(ast)).join('\n') +
  '\nexports.resolveActiveOrderRowClassName = resolveActiveOrderRowClassName; exports.canApplyActiveOrderRelease = canApplyActiveOrderRelease;',
{ compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const handlers = {}
Function('exports', 'handoffReadOnly', 'isActiveOrderProgressComplete', compiled)(handlers, { value: false }, value => Number(value) === 100)

test('parameter warnings leave list rows unchanged while quantity warnings retain their color', () => {
  assert.equal(handlers.resolveActiveOrderRowClassName({ row: {} }), '')
  assert.equal(handlers.resolveActiveOrderRowClassName({ row: { hasParameterAbnormal: true } }), '')
  for (const quantityFlag of ['hasQuantityConflict', 'quantityConflict']) {
    assert.match(handlers.resolveActiveOrderRowClassName({ row: { hasParameterAbnormal: true, [quantityFlag]: true } }), /quantity-conflict/)
  }
})

test('parameter warning is advisory and leaves formal abnormal and release prerequisites intact', () => {
  const complete = { productionProgressPercent: 100, inspectionProgressPercent: 100, formalProductionReviewComplete: true, hasParameterAbnormal: true }
  assert.equal(handlers.canApplyActiveOrderRelease(complete), true)
  assert.equal(handlers.canApplyActiveOrderRelease({ ...complete, abnormal: true }), false)
  assert.equal(handlers.canApplyActiveOrderRelease({ ...complete, readBlocked: true }), false)
  assert.equal(handlers.canApplyActiveOrderRelease({ ...complete, productionProgressPercent: 50 }), false)
})

test('workbench template compiles after removing the list parameter warning', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'parameter-warning' }).errors, [])
})

test('order identity and quantity warnings render without parameter labels for every warning combination', async () => {
  const marker = descriptor.template.content.indexOf('class="team-leader-workbench__active-order-code-cell"')
  const start = descriptor.template.content.lastIndexOf('<div', marker)
  const end = descriptor.template.content.indexOf('</div>', marker) + '</div>'.length
  const render = Vue.compile(descriptor.template.content.slice(start, end))
  for (const [quantity, parameter] of [[false, false], [true, false], [false, true], [true, true]]) {
    const app = Vue.createSSRApp({
      data: () => ({ row: { workOrderCode: 'WO-627', hasQuantityConflict: quantity, hasParameterAbnormal: parameter } }),
      render
    })
    app.component('ElTag', { setup: (_, { slots }) => () => Vue.h('span', slots.default()) })
    const html = await renderToString(app)
    assert.ok(html.includes('WO-627'))
    assert.equal(html.includes('数量超限（仅提示）'), quantity)
    assert.equal(html.includes('参数异常（仅提示）'), false)
  }
})
