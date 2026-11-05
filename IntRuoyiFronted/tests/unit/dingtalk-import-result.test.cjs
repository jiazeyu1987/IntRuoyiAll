const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/system/user/UserDingTalkImportForm.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const source = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const handler = source.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(source) === 'submitFormSuccess'))
const code = ts.transpileModule(handler.getText(source), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
function harness() {
  const results = [], errors = [], events = [], visible = { value: true }
  const submit = new Function('message', 'showImportResult', 'dialogVisible', 'emits', code + '\nreturn submitFormSuccess')(
    { error: e => errors.push(e) }, r => results.push(r), visible, e => events.push(e))
  return { submit, results, errors, events, visible }
}
const oldResult = () => ({ createUsernames: [], failureUsernames: {}, createdDeptPaths: [], enabledDeptPaths: [], leaderAssignedDeptPaths: {}, leaderSkippedDeptPaths: {} })
test('old backend response reports version mismatch without announcing import success', () => {
  const h = harness()
  assert.throws(() => h.submit({ code: 0, data: oldResult() }), /前后端版本不一致/)
  assert.equal(h.results.length, 0)
  assert.equal(h.events.length, 0)
  assert.equal(h.visible.value, true)
})
test('reimport result displays existing users and authorized users without counting them as new', () => {
  const h = harness()
  h.submit({ code: 0, data: { ...oldResult(), existingUsernames: ['donglina'], authorizedUsernames: ['donglina'] } })
  assert.match(h.results[0].summary, /已创建 0 个用户，匹配 1 个已有用户，1 个用户已获得电子签名授权/)
  assert.deepEqual(h.results[0].sections.find(s => s.title === '电子签名已授权的用户').rows, [['donglina']])
  assert.equal(h.visible.value, false)
  assert.deepEqual(h.events, ['success'])
})
test('backend rejection remains visible without rendering a success result', () => {
  const h = harness()
  h.submit({ code: 500, msg: '授权审计写入失败' })
  assert.deepEqual(h.errors, ['授权审计写入失败'])
  assert.equal(h.results.length, 0)
  assert.equal(h.visible.value, true)
})
