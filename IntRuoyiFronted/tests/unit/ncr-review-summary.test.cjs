const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { renderToString } = require('vue/server-renderer')
const compiler = require('vue/compiler-sfc')
const root = 'src/views/mes/pro/edhr-nonconformance/'
const descriptor = compiler.parse(fs.readFileSync(root + 'NonconformanceReviewSummary.vue', 'utf8')).descriptor
const output = {}
vm.runInNewContext(ts.transpileModule(compiler.compileScript(descriptor, { id: 'summary', inlineTemplate: true }).content,
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText,
{ exports: output, require: id => { assert.equal(id, 'vue'); return vue } })
async function render(summary) {
  return renderToString(vue.createSSRApp(output.default, { summary, statusLabel: '已关闭', dispositionLabel: '返工' }))
}
test('read-only summary retains exact task ID, assigned QA and actual actor separately', async () => {
  const html = await render({ review: { reviewCode: 'NCR-1', nonconformanceReason: '<script>原因</script>', reviewOpinion: '返工意见' },
    qaReviewerName: '指定QA甲', qaUserName: '处置QA乙', qaTaskId: '2124588436506107904', qaTaskStatus: 'DONE' })
  for (const text of ['NCR-1', '指定QA甲', '处置QA乙', '2124588436506107904', '已完成', '返工意见', '&lt;script&gt;']) assert(html.includes(text))
  assert(!/<button|<input|<form|<script>/.test(html))
})
test('unassigned and canceled tasks are shown explicitly', async () => {
  const empty = await render({ review: {} })
  for (const label of ['未指派', '尚未处置', '未生成']) assert(empty.includes(label))
  const canceled = await render({ review: {}, qaTaskId: '123', qaTaskStatus: 'CANCELED' })
  assert(canceled.includes('已取消（原周期已关闭）'))
})

const page = compiler.parse(fs.readFileSync(root + 'NonconformanceReviewPage.vue', 'utf8')).descriptor
compiler.compileScript(page, { id: 'page' })
assert.deepEqual(compiler.compileTemplate({ source: page.template.content, filename: 'page.vue', id: 'page' }).errors, [])
const script = ts.transpile(page.scriptSetup.content, { target: ts.ScriptTarget.ES2022 })
function handlerContext() {
  const ctx = { orderDetailSequence: 0, approvalInvalidation: { syncSession: () => ({ key: 'session' }) },
    resolveErrorMessage: e => e.message }
  for (const field of ['orderDetailVisible', 'orderDetailLoading', 'orderDetailError', 'orderDetail', 'orderDetailReview', 'orderDetailSummary']) ctx[field] = { value: undefined }
  vm.createContext(ctx)
  for (const name of ['closeOrderDetail', 'openActiveOrderDetail']) {
    const start = script.indexOf('const ' + name + ' ='), end = script.indexOf('\n};', start)
    assert(start > 0 && end > start)
    vm.runInContext(script.slice(start, end + 3) + '\nglobalThis.' + name + ' = ' + name, ctx)
  }
  return ctx
}
test('switching detail keeps late responses from mixing reviews', async () => {
  const ctx = handlerContext(), pending = {}
  ctx.getNonconformanceReviewActiveOrderDetail = id => new Promise(resolve => { pending[id] = resolve })
  ctx.getNonconformanceReviewSummary = async id => ({ review: { id, activeOrderId: id + 100 } })
  const first = ctx.openActiveOrderDetail({ id: 1 })
  const second = ctx.openActiveOrderDetail({ id: 2 })
  pending[2]({ activeOrderId: 102 }); await second
  pending[1]({ activeOrderId: 101 }); await first
  assert.equal(ctx.orderDetailSummary.value.review.id, 2)
  assert.equal(ctx.orderDetail.value.activeOrderId, 102)
})
test('missing summary and mismatched original cycle show errors without partial success', async () => {
  const ctx = handlerContext()
  ctx.getNonconformanceReviewActiveOrderDetail = async () => ({ activeOrderId: 101 })
  ctx.getNonconformanceReviewSummary = async () => { throw new Error('正式QA交接任务缺失') }
  await ctx.openActiveOrderDetail({ id: 1 })
  assert.equal(ctx.orderDetailError.value, '正式QA交接任务缺失')
  assert.equal(ctx.orderDetailSummary.value, undefined)
  ctx.getNonconformanceReviewSummary = async () => ({ review: { id: 1, activeOrderId: 999 } })
  await ctx.openActiveOrderDetail({ id: 1 })
  assert.match(ctx.orderDetailError.value, /不一致/)
  assert.equal(ctx.orderDetail.value, undefined)
})
