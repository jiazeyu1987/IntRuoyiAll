const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const descriptor = parse(fs.readFileSync(filename, 'utf8')).descriptor
const ast = ts.createSourceFile('leader.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = new Set(['formatActiveOrderReleaseStatus', 'formatActiveOrderReleaseStatusTag',
  'isActiveOrderNcrClosed', 'openAbnormalDialog', 'submitAbnormal'])
const statements = ast.statements.filter(s => ts.isVariableStatement(s)
  && s.declarationList.declarations.some(d => names.has(d.name.getText(ast))))
assert.equal(statements.length, names.size)
const code = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None }
}).outputText
function harness(row) {
  const calls = [], warnings = [], errors = []
  const values = {
    activeOrderTestResetSubmitting: { value: false }, abnormalDialogVisible: { value: false },
    abnormalSubmitting: { value: false }, abnormalForm: { activeOrderId: 528, abnormalDescription: '实际异常' },
    abnormalFormRef: { value: { validate: async () => true } }, activeOrderOptions: { value: [row] },
    resetAbnormalForm: () => calls.push('reset'), requirePositiveNumber: Number,
    loadActiveOrders: async () => calls.push('refresh'),
    markAndReportWorkOrderAbnormal: async () => calls.push('WRITE'),
    resolveErrorMessage: e => e.message,
    ElMessage: { warning: m => warnings.push(m), error: m => errors.push(m), success: m => calls.push(m) }
  }
  const methods = Function(...Object.keys(values), code + ';return {' + [...names].join(',') + '};')(...Object.values(values))
  return { ...values, ...methods, calls, warnings, errors }
}
const template = compileTemplate({ source: descriptor.template.content, filename, id: 'ncr-terminal' })
const templateAst = descriptor.template.ast
test('真实组长模板可编译', () => assert.deepEqual(template.errors, []))

for (const [status, label, tag] of [
  [undefined, '未申请', 'info'],
  ['MARKET_RELEASE_WITHDRAWN', '上市放行已撤回', 'warning'],
  ['MARKET_RELEASE_REJECTED', '上市放行已驳回', 'danger'],
  ['UNRECOGNIZED', '未知放行状态：UNRECOGNIZED', 'danger']
]) {
  test(`放行申请${status ?? '无申请'}保留真实状态而非误报未申请`, () => {
    const h = harness({})
    assert.equal(h.formatActiveOrderReleaseStatus(status), label)
    assert.equal(h.formatActiveOrderReleaseStatusTag(status), tag)
  })
}

for (const [status, label, tag] of [
  ['NONCONFORMANCE_VOID', 'QA已作废', 'danger'],
  ['NONCONFORMANCE_REWORK', '已结束并转入返工', 'warning']
]) {
  test(`${status}显示准确终态并阻止旧弹窗提交`, async () => {
    const row = { id: 528, releaseApplicationStatus: status }, h = harness(row)
    assert.equal(h.formatActiveOrderReleaseStatus(status), label)
    assert.equal(h.formatActiveOrderReleaseStatusTag(status), tag)
    h.openAbnormalDialog(row)
    assert.equal(h.abnormalDialogVisible.value, false)
    assert.equal(h.warnings.length, 1)
    await h.submitAbnormal()
    assert.deepEqual(h.calls, ['refresh'])
    assert.equal(h.errors.length, 1)
    assert.equal(h.abnormalSubmitting.value, false)
  })
  test(`${status}真实模板异常与更多按钮禁用，详情仍可打开`, () => {
    const h = harness({}), row = { releaseApplicationStatus: status }
    function disabled(marker) {
      const walk = n => {
        if (n.props?.some(p => p.type === 6 && p.name === marker)) return n
        for (const child of n.children || []) { const found = walk(child); if (found) return found }
      }
      const node = walk(templateAst)
      assert.ok(node, marker)
      const expression = node.props.find(p => p.type === 7 && p.name === 'bind' && p.arg?.content === 'disabled').exp.content
      return Function('row', 'isActiveOrderNcrClosed', 'activeOrderTestResetSubmitting',
        'activeOrderSimulationSubmittingId', 'maintenanceSubmitting', 'activeOrderRebuildSubmittingId',
        'return (' + expression + ')')(row, h.isActiveOrderNcrClosed, false, undefined, false, undefined)
    }
    assert.equal(disabled('data-team-leader-report-active-order-abnormal'), true)
    assert.equal(disabled('data-team-leader-active-order-test-menu'), true)
    assert.equal(Boolean(disabled('data-team-leader-active-order-detail')), false)
  })
}
test('尚未申请的新返工周期仍可正常上报异常', async () => {
  const h = harness({ id: 528 })
  h.openAbnormalDialog({ id: 528 })
  assert.equal(h.abnormalDialogVisible.value, true)
  await h.submitAbnormal()
  assert.equal(h.calls.filter(c => c === 'WRITE').length, 1)
  assert.deepEqual(h.errors, [])
})
