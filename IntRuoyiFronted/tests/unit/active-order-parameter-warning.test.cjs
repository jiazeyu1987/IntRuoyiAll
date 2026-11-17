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

test('parameter warnings color the row and coexist with quantity warnings', () => {
  assert.equal(handlers.resolveActiveOrderRowClassName({ row: {} }), '')
  assert.match(handlers.resolveActiveOrderRowClassName({ row: { hasParameterAbnormal: true } }), /parameter-abnormal/)
  for (const quantityFlag of ['hasQuantityConflict', 'quantityConflict']) {
    assert.match(handlers.resolveActiveOrderRowClassName({ row: { hasParameterAbnormal: true, [quantityFlag]: true } }), /quantity-conflict/)
  }
  assert.match(source, /:deep\(\.team-leader-workbench__active-order-row--parameter-abnormal > td\)/)
})

test('parameter warning is advisory and leaves formal abnormal and release prerequisites intact', () => {
  const complete = { productionProgressPercent: 100, inspectionProgressPercent: 100, hasParameterAbnormal: true }
  assert.equal(handlers.canApplyActiveOrderRelease(complete), true)
  assert.equal(handlers.canApplyActiveOrderRelease({ ...complete, abnormal: true }), false)
  assert.equal(handlers.canApplyActiveOrderRelease({ ...complete, readBlocked: true }), false)
  assert.equal(handlers.canApplyActiveOrderRelease({ ...complete, productionProgressPercent: 50 }), false)
})

test('saved parameter flag renders an explicit red warning and hides it for normal orders', async () => {
  const marker = descriptor.template.content.indexOf('data-team-leader-active-order-parameter-abnormal')
  assert.ok(marker >= 0)
  const start = descriptor.template.content.lastIndexOf('<el-tag', marker)
  const end = descriptor.template.content.indexOf('</el-tag>', marker) + '</el-tag>'.length
  const template = descriptor.template.content.slice(start, end)
  assert.match(template, /type="danger"/)
  const render = Vue.compile(template)
  for (const flag of [true, false]) {
    const app = Vue.createSSRApp({ data: () => ({ row: { hasParameterAbnormal: flag } }), render })
    app.component('ElTag', { setup: (_, { slots }) => () => Vue.h('span', slots.default()) })
    const html = await renderToString(app)
    assert.equal(html.includes('参数异常（仅提示）'), flag)
  }
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'parameter-warning' }).errors, [])
})
