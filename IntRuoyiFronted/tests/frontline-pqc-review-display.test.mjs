import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { parse } from 'vue/compiler-sfc'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const module = { exports: {} }
const source = readFileSync(new URL('../src/views/mes/pro/feedback/frontlinePqcReviewDisplay.ts', import.meta.url), 'utf8')
new Function('exports', ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(module.exports)
const { getPqcReviewLabel, getPqcReviewCounts, formatPqcReviewStatus } = module.exports

test('submission and review approval have distinct display states', () => {
  assert.equal(getPqcReviewLabel({ taskStatus: 'PENDING' }), '待检验提交')
  assert.equal(getPqcReviewLabel({ taskStatus: 'SUBMITTED', currentReviewStatus: 'PENDING' }), '待复核')
  assert.equal(getPqcReviewLabel({ taskStatus: 'CONFIRMED', currentReviewStatus: 'APPROVED' }), '复核通过')
  assert.equal(getPqcReviewLabel({ taskStatus: 'PENDING', returnedSubmission: { row: { rejectedReviewId: 8 } } }), '退回待修改')
  assert.equal(getPqcReviewLabel({ taskStatus: 'CANCELLED' }), '已撤销')
  assert.throws(() => getPqcReviewLabel({ taskStatus: 'UNKNOWN' }), /未知PQC任务状态/)
})

test('order review counts keep submitted, approved, returned and draft tasks separate', () => {
  assert.deepEqual(getPqcReviewCounts([
    { taskStatus: 'SUBMITTED', currentReviewStatus: 'PENDING' }, { taskStatus: 'CONFIRMED', currentReviewStatus: 'APPROVED' }, { taskStatus: 'CONFIRMED', currentReviewStatus: 'APPROVED' },
    { taskStatus: 'PENDING', returnedSubmission: { row: { rejectedReviewId: 8 } } },
    { taskStatus: 'PENDING' }, { taskStatus: 'CANCELLED' }
  ]), { pending: 1, confirmed: 2, returned: 1, unavailable: 0 })
})

test('historical decisions are named as reviews and unknown backend states fail visibly', () => {
  assert.equal(formatPqcReviewStatus('APPROVED'), '复核通过')
  assert.equal(formatPqcReviewStatus('REJECTED'), '退回修改')
  assert.throws(() => formatPqcReviewStatus('PENDING'), /未知PQC复核状态/)
})


test('other operators rejected records and resubmitted revisions use the current review fact', () => {
  const task = { taskStatus: 'SUBMITTED', currentReviewStatus: 'REJECTED',
    reviewHistory: [{ reviewId: 5, reviewStatus: 'REJECTED' }] }
  assert.equal(getPqcReviewLabel(task), '退回待修改')
  assert.deepEqual(getPqcReviewCounts([task]), { pending: 0, confirmed: 0, returned: 1, unavailable: 0 })
  task.currentReviewStatus = 'PENDING'
  assert.equal(getPqcReviewLabel(task), '待复核')
  assert.deepEqual(getPqcReviewCounts([task]), { pending: 1, confirmed: 0, returned: 0, unavailable: 0 })
  assert.equal(task.reviewHistory[0].reviewStatus, 'REJECTED')
})

test('missing current review facts are reported without guessing from task status or old reviews', () => {
  const task = { taskStatus: 'CONFIRMED', reviewHistory: [{ reviewStatus: 'APPROVED' }] }
  assert.equal(getPqcReviewLabel(task), '复核状态缺失，请刷新订单')
  assert.deepEqual(getPqcReviewCounts([task]), { pending: 0, confirmed: 0, returned: 0, unavailable: 1 })
})

test('successful resubmission receipt changes current review to pending while preserving historical rejection', () => {
  const panel = readFileSync(new URL('../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue', import.meta.url), 'utf8')
  const ast = ts.createSourceFile('panel.ts', parse(panel).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = ['resolvePqcTaskSummaryState', 'updatePqcSubmittedTasksInProcess']
  const declarations = ast.statements.filter(ts.isVariableStatement).filter(statement =>
    statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
  assert.equal(declarations.length, names.length)
  const body = ts.transpileModule(declarations.map(statement => statement.getText(ast)).join('\n') +
    '\nreturn updatePqcSubmittedTasksInProcess', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const update = new Function(body)()
  const history = [{ reviewId: 51, reviewStatus: 'REJECTED' }]
  const task = { pqcTaskId: 31, taskStatus: 'PENDING', currentReviewStatus: 'REJECTED',
    reviewHistory: history, returnedSubmission: { row: { rejectedReviewId: 51 } } }
  const result = update({ pqcTaskOptions: [task], taskSummary: {} }, new Set([31]))
  assert.equal(getPqcReviewLabel(result.pqcTaskOptions[0]), '待复核')
  assert.equal(result.pqcTaskOptions[0].returnedSubmission, undefined)
  assert.equal(result.pqcTaskOptions[0].reviewHistory, history)
  assert.equal(task.currentReviewStatus, 'REJECTED')
})
