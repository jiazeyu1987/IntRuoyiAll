const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/production-release/PqcProductionReleasePage.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
const declaration = ast.statements.find(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.some(d => d.name.getText(ast) === 'confirmedReleaseRejection'))
const context = {}
vm.runInNewContext(ts.transpileModule(declaration.getText(ast) + '\nglobalThis.readRejection = confirmedReleaseRejection',
  { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, context)
const request = { applicationId: '276', pqcReleaseWorkTaskId: '2900', actorId: '9908094669',
  expectedVersion: 1, idempotencyKey: 'request-1', payloadHash: 'a'.repeat(64) }
const error = () => ({ details: { rejectedDecision: { outcome: 'ROLLED_BACK', applicationId: request.applicationId,
  workTaskId: request.pqcReleaseWorkTaskId, actorId: request.actorId, expectedVersion: request.expectedVersion,
  idempotencyKey: request.idempotencyKey, payloadHash: request.payloadHash },
  blockers: [{ blockerType: 'UNSUPPORTED_RELEASE_ACTION', reason: '活跃订单已有不同的UDI编号，不能覆盖' }] } })

test('confirmed UDI rollback permits correction with its real reason', () => {
  assert.match(context.readRejection(error(), request), /UDI.*本次放行已回滚/)
})
test('unbound errors, unknown outcomes and different requests remain uncertain', () => {
  assert.equal(context.readRejection({ code: 400, message: 'UDI conflict' }, request), undefined)
  for (const key of ['outcome', 'applicationId', 'workTaskId', 'actorId', 'expectedVersion', 'idempotencyKey', 'payloadHash']) {
    const value = error(); value.details.rejectedDecision[key] = 'different'
    assert.equal(context.readRejection(value, request), undefined, key)
  }
})
test('missing or ambiguous blockers cannot authorize correction', () => {
  for (const blockers of [[], [{ reason: 'UDI' }], [error().details.blockers[0], error().details.blockers[0]]]) {
    const value = error(); value.details.blockers = blockers
    assert.equal(context.readRejection(value, request), undefined)
  }
})
test('management role rollback preserves its actionable instruction', () => {
  const value = error(); value.details.blockers = [{ blockerType: 'MANAGEMENT_REPRESENTATIVE_ROLE_REQUIRED', reason: '未配置管理者代表' }]
  assert.match(context.readRejection(value, request), /为管理者代表角色分配启用账号/)
})
