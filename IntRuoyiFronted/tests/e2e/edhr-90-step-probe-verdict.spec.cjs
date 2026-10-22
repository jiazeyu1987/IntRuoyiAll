const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')

const probePath = path.resolve(__dirname, '../../../doc/tasks/20260915-edhr-90-step-e2e-verification/e2e-90-step-probe.cjs')
const source = fs.readFileSync(probePath, 'utf8')
const classifier = source.slice(source.indexOf('function classifyStep('), source.indexOf('function writeSummary('))
const context = vm.createContext({})
vm.runInContext(classifier, context)
const route = { label: '生产提交', route: '/production', tokenHits: ['提交'], bodySample: '签名 提交', apiResponses: [], pageErrors: [] }
const classify = (data) => context.classifyStep([37, '签名提交', 'production', ['签名', '提交']], new Map([['production', data]]), [{ id: 'frontline-production-interactions', rootVisible: true }])

test('visible submit/signature controls do not prove business submission', () => {
  assert.equal(classify(route).status, 'BLOCKED')
})
test('HTTP 200 with business error is not a successful page', () => {
  assert.equal(classify({ ...route, apiResponses: [{ status: 200, code: 1040760369, msg: '缺少输出物料快照' }] }).status, 'FAIL')
})
test('page errors cannot be ignored by token matching', () => {
  assert.equal(classify({ ...route, pageErrors: ['unsupported module'] }).status, 'FAIL')
})
test('failed click is not discarded and treated as a successful action', () => {
  assert.doesNotMatch(source, /\.click\(\)\.catch\(\(\) => undefined\)/)
})
test('approval probe uses supported EDHR module', () => {
  assert.match(source, /approval-center\/todo\?moduleCode=EDHR&viewType=TODO/)
  assert.doesNotMatch(source, /moduleCode=MES&/)
})
