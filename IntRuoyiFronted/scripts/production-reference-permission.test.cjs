const fs = require('fs')
const assert = require('node:assert/strict')
const vm = require('node:vm')
const vue = require('vue')
const ts = require('typescript')
const compiler = require(require.resolve('@vue/compiler-sfc', { paths: [require.resolve('vue')] }))
const files = ['erp/production/material-list/index.vue', 'mes/pro/edhr/TrackingPage.vue', 'mes/pro/edhr/form-trace/FormTraceAuditTab.vue', 'mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue', 'mes/pro/scheduleorder/index.vue', 'mes/pro/edhr/FormFillLogPage.vue', 'mes/pro/edhr/SignaturePage.vue', 'mes/pro/edhr/DomainTraceDetailPage.vue']
const handlers = ['openWorkOrderList', 'openActiveOrderDocument', 'openWorkOrder', 'openBatch', 'handleOpenProductionMaterialList', 'handleOpenWorkOrder', 'handleOpenGroupWorkOrder', 'openBatchExecutionWorkOrder', 'openBatchExecutionDetail', 'openExecution']
let checks = 0
for (const file of files) {
  const source = fs.readFileSync('src/views/' + file, 'utf8')
  const d = compiler.parse(source).descriptor
  compiler.compileScript(d, { id: file })
  assert.deepEqual(compiler.compileTemplate({ source: d.template.content, filename: file, id: file }).errors, [])
  // Extract actual button branches with their immediate text alternatives.
  const pattern = /<(el-button|el-link)\b[^<>]*?@click="((?:openWorkOrderList|openActiveOrderDocument|openWorkOrder|openBatch|handleOpenProductionMaterialList|handleOpenWorkOrder|handleOpenGroupWorkOrder|openBatchExecutionWorkOrder|openBatchExecutionDetail|openExecution)(?:\([^"]*\))?)"(?:(?!<\/el-button>|<\/el-link>)[\s\S])*?<\/\1>\s*<span[\s\S]*?<\/span>/g
  for (const match of d.template.content.matchAll(pattern)) {
    const compiled = compiler.compileTemplate({ source: match[0], filename: file, id: file, compilerOptions: { mode: 'function', isCustomElement: tag => ['el-button', 'el-link', 'el-tag', 'ProductionBatchCodeLink'].includes(tag) } })
    assert.deepEqual(compiled.errors, [])
    const render = new Function('Vue', compiled.code)(vue)
    for (const allowed of [false, true]) {
      const ctx = { row: { contextStatus: 'COMPLETE', batchExecutionId: 3, executionId: 4, executionCode: 'EX1', workOrderId: 1, batchId: 2, workOrderCode: 'MO1', batchCode: 'B1', productionOrderCount: 1, productionOrderSummary: 'MO1', productionOrderNo: 'MO1', erpWorkOrderCode: 'MO1', productionMaterialListCount: 1, productionMaterialListSummary: 'MAT1' }, link: { no: 'DOC1' }, activeOrderWorkOrderDisplay: { workOrderCode: 'MO1' }, checkPermi: () => allowed, canOpenActiveOrderDocument: () => allowed, getScheduleOrderSourceCodeText: () => 'MO1', handoffReadOnly: false, resolveEvidenceLabel: () => '证据' }
      handlers.forEach(name => { ctx[name] = () => {} })
      const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ comment: text }), setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text }, parentNode: () => null, nextSibling: () => null, insert: (child, parent) => parent.children.push(child), remove: () => {}, patchProp: () => {} })
      const root = { children: [] }
      renderer.createApp({ render() { return render(ctx, []) } }).mount(root)
      const nodes = []; const walk = n => { nodes.push(n); (n.children || []).forEach(walk) }; walk(root)
      assert(nodes.some(n => n.type === (allowed ? match[1] : 'span')), file + ': branch permission')
      if (!allowed) assert(nodes.some(n => /MO1|B1|DOC1|MAT1|EX1|执行表单/.test(n.text || '')), file + ': retains text')
      checks++
    }
  }
  const script = ts.transpile(d.scriptSetup.content, { target: ts.ScriptTarget.ES2022 })
  for (const name of handlers) {
    const start = script.indexOf('const ' + name + ' =')
    if (start < 0) continue
    const end = script.indexOf('\n};', start)
    assert(end > start)
    let calls = 0
    const context = { checkPermi: () => false, canOpenActiveOrderDocument: () => false, router: { push: () => calls++ }, props: { handoffReadOnly: false, resolveEvidenceLabel: () => '证据' } }
    vm.createContext(context)
    vm.runInContext(script.slice(start, end + 3) + '\n' + name + '({});', context)
    assert.equal(calls, 0, file + ': denied handler')
    checks++
  }
  console.log('PASS ' + file)
}
assert(checks >= 25, 'Expected all affected branches and handlers')
console.log('PASS ' + checks + ' permission checks')
