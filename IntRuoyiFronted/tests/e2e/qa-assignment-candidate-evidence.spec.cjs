const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const file = path.resolve(__dirname, '../../src/views/mes/pro/handoff/QaHandoffAssignmentConfig.vue')
function config(api) {
  const descriptor = parse(fs.readFileSync(file, 'utf8')).descriptor
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: 'qa-candidates' }).errors, [])
  const ast = ts.createSourceFile('qa.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const source = ast.statements.filter(statement => !ts.isImportDeclaration(statement)).map(statement => statement.getText(ast)).join('\n')
  const code = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
  return Function('ref', 'computed', 'watch', 'onMounted', 'api', 'useMessage', code + ';return {routeId,loadedRouteId,loadedRule,loadRule,error};')(
    vue.ref, vue.computed, () => {}, () => {}, api, () => ({ success() {} }))
}
const rule = (source = 'ROLE_GROUP') => ({ id: 901, candidateSourceType: source, candidateSourceId: 50, candidateLabel: '正式QA角色', enabled: true,
  candidateUsers: [{ id: '345', accountName: 'qa-test', visibleDisplayName: '正式QA' }] })
test('真实配置组件只读保留正式规则返回的账号，不通过角色名或查询结果编造候选', async () => {
  const response = rule(), q = config({ qaAssignment: async () => response })
  q.routeId.value = 98; await q.loadRule()
  assert.deepEqual(q.loadedRule.value.candidateUsers, response.candidateUsers)
  assert.equal(q.loadedRule.value.candidateSourceType, 'ROLE_GROUP')
  assert.equal(q.loadedRouteId.value, 98)
})
test('换路线、读取失败和未配置立即撤销旧正式候选证据', async () => {
  let response = rule()
  const q = config({ qaAssignment: async () => { if (response instanceof Error) throw response; return response } })
  q.routeId.value = 98; await q.loadRule()
  response = new Error('formal resolution denied')
  q.routeId.value = 99; const pending = q.loadRule()
  assert.equal(q.loadedRule.value, null)
  assert.equal(q.loadedRouteId.value, undefined)
  await pending; assert.equal(q.loadedRule.value, null); assert.equal(q.error.value, 'formal resolution denied')
  response = null; await q.loadRule(); assert.equal(q.loadedRule.value, null)
})
test('未部署候选字段不伪造成账号名单，停用规则状态保持真实', async () => {
  const response = { ...rule('USER'), enabled: false, candidateUsers: undefined }
  const q = config({ qaAssignment: async () => response })
  q.routeId.value = 98; await q.loadRule()
  assert.equal(q.loadedRule.value.candidateUsers, undefined)
  assert.equal(q.loadedRule.value.enabled, false)
})
