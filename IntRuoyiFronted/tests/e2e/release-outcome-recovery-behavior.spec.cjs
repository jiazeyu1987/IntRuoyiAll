const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/mes/pro/production-release/components/ReleaseOutcomeRecovery.vue')
const descriptor = parse(fs.readFileSync(filename, 'utf8')).descriptor
const ast = ts.createSourceFile('recovery.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = new Set(['labels', 'generation', 'load'])
const statements = ast.statements.filter(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.some(d => names.has(d.name.getText(ast))))
assert.equal(statements.length, 3)
const code = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None }
}).outputText
const own = { id: '453', workTaskId: '2883', workOrderCode: '原订单', batchCode: '原批号', eventType: 'MARKET_APPROVED' }
function harness(response = async () => [own]) {
  let calls = 0
  const values = { props: { active: true }, canQuery: { value: true }, rows: { value: [] },
    error: { value: '' }, loading: { value: false }, listOwnPendingReleaseOutcomes: () => { calls++; return response() } }
  return { ...values, ...Function(...Object.keys(values), code + ';return {load};')(...Object.values(values)), calls: () => calls }
}
test('本人结果恢复真实模板可编译并沿用正式回执组件', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'outcome-recovery' }).errors, [])
  assert.match(descriptor.template.content, /<ReleaseTaskNotificationEntry :work-task-id="row.workTaskId"/)
})
test('只读发现本人原任务结果，不产生补发写入', async () => {
  const h = harness(); await h.load(); assert.deepEqual(h.rows.value, [own]); assert.equal(h.calls(), 1)
})
test('无权限或未展示时不读取结果，清除旧身份记录', async () => {
  for (const key of ['permission', 'active']) {
    const h = harness(); h.rows.value = [own]
    if (key === 'permission') h.canQuery.value = false; else h.props.active = false
    await h.load(); assert.equal(h.calls(), 0); assert.deepEqual(h.rows.value, [])
  }
})
test('离开站内信后迟到响应不得恢复旧身份回执', async () => {
  let finish; const h = harness(() => new Promise(resolve => { finish = resolve }))
  const pending = h.load(); h.props.active = false; await h.load(); finish([own]); await pending
  assert.deepEqual(h.rows.value, []); assert.equal(h.loading.value, false)
})
test('畸形通知、未知事件及查询失败明确显示错误并清除旧结果', async () => {
  for (const response of [async () => [{ ...own, workTaskId: '0' }], async () => [{ ...own, eventType: 'ASSIGNED' }],
    async () => { throw new Error('查询失败') }]) {
    const h = harness(response); h.rows.value = [own]; await h.load()
    assert.deepEqual(h.rows.value, []); assert.ok(h.error.value); assert.equal(h.loading.value, false)
  }
})
