const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const descriptor = parse(fs.readFileSync(path.join(root, 'src/views/system/notify/my/components/MyNotifyMessageList.vue'), 'utf8')).descriptor
const ast = ts.createSourceFile('notify.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === 'canReadActiveOrderHandoffs'))
assert.ok(declaration)
const find = node => node.tag === 'ActiveOrderHandoffPanel' ? node : (node.children || []).map(find).find(Boolean)
const element = find(descriptor.template.ast)
assert.ok(element, '正式通知列表及个人中心共用的组件必须提供交接入口')
const template = compileTemplate({ source: element.loc.source, filename: 'handoff-entry.vue', id: 'handoff-entry', compilerOptions: { mode: 'function', prefixIdentifiers: false, cacheHandlers: false } })
assert.deepEqual(template.errors, [])
const render = Function('Vue', template.code)({ ...vue, resolveComponent: () => 'handoff-panel' })
function entry(permissions) {
  const code = ts.transpileModule(declaration.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const enabled = Function('computed', 'hasPermission', code + '; return canReadActiveOrderHandoffs;')(vue.computed, values => values.some(value => permissions.includes(value)))
  return render({ canReadActiveOrderHandoffs: enabled.value }, [])
}
test('ordinary production, leaders, QA and release query roles reach the actual handoff panel', () => {
  for (const permission of ['mes:pro-feedback:query', 'mes:pro-process-pool-team-leader:query',
    'mes:pro-edhr-nonconformance-review:query', 'mes:pro-edhr-work-task:query']) {
    assert.equal(entry([permission]).type, 'handoff-panel')
  }
})
test('unrelated notification users and mutation-only permissions cannot mount the handoff panel', () => {
  for (const permissions of [[], ['system:notify-message:query'], ['mes:pro-feedback:create'], ['mes:pro-edhr-work-task:update']]) {
    assert.equal(entry(permissions).type, vue.Comment)
  }
})
