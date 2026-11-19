const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const page = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const script = page.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
const names = ['allocationSaveRequestIdentity', 'getOrCreateAllocationSaveIdempotencyKey', 'createSimulationCopyRunId',
  'getOrCreateActiveOrderReleaseIdempotencyKey', 'submitActiveOrderVersionUpgrade']
const statements = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
assert.equal(statements.length, names.length)
const code = ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n') +
  '\nexports.keys = { getOrCreateAllocationSaveIdempotencyKey, createSimulationCopyRunId, getOrCreateActiveOrderReleaseIdempotencyKey, submitActiveOrderVersionUpgrade };', {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const state = { value: undefined }
  const releaseKeys = new Map(), writes = [], errors = []
  const context = {
    exports: {}, allocationSaveIdempotencyState: state, ...loadUuid(crypto),
    releaseApplicationIdempotencyKeys: releaseKeys,
    requireWritableHandoff: () => true,
    activeOrderTestResetSubmitting: { value: false },
    activeOrderVersionUpgradePreview: { value: { activeOrderId: 321 } },
    activeOrderVersionUpgradeSubmittingId: { value: undefined },
    activeOrderVersionUpgradeReason: { value: ' 升级原因 ' },
    activeOrderVersionUpgradeConfirmRestart: { value: true },
    activeOrderVersionUpgradeDialogVisible: { value: true },
    requirePositiveNumber: value => value,
    submitTeamLeaderActiveOrderVersionUpgrade: async request => {
      writes.push(request)
      return { requestCode: 'UPGRADE-321' }
    },
    ElMessage: { success() {}, error: message => errors.push(message) },
    resolveErrorMessage: (error, message) => error.message || message,
    loadActiveOrders: async () => {}
  }
  vm.runInNewContext(code, context)
  return { ...context.exports.keys, state, context, releaseKeys, writes, errors }
}

test('HTTP allocation saves reuse the same command key and replace it when the command changes', () => {
  const keys = setup()
  const request = { eventId: 1, leaderType: 'PRODUCTION', allocationMode: 'FIFO', allocations: [] }
  const first = keys.getOrCreateAllocationSaveIdempotencyKey(request)
  assert.match(first, uuidPattern)
  assert.equal(keys.getOrCreateAllocationSaveIdempotencyKey({ ...request }), first)
  const next = keys.getOrCreateAllocationSaveIdempotencyKey({ ...request, expectedVersion: 2 })
  assert.match(next, uuidPattern)
  assert.notEqual(next, first)
})

test('HTTP simulation IDs retain the prefix and timestamp with a fresh UUID v4', () => {
  const keys = setup()
  const first = keys.createSimulationCopyRunId()
  const second = keys.createSimulationCopyRunId()
  assert.match(first, /^SIMCOPY-\d+-[0-9a-f-]{36}$/)
  assert.match(first.replace(/^SIMCOPY-\d+-/, ''), uuidPattern)
  assert.notEqual(second, first)
})

test('missing entropy rejects command creation without setting partial idempotency state', () => {
  const keys = setup({})
  assert.throws(() => keys.getOrCreateAllocationSaveIdempotencyKey({ eventId: 1 }), /不支持安全随机数/)
  assert.equal(keys.state.value, undefined)
  assert.throws(() => keys.createSimulationCopyRunId(), /不支持安全随机数/)
})

test('HTTP active order release keys retain the business prefix and reuse their cached UUID', () => {
  const keys = setup()
  const first = keys.getOrCreateActiveOrderReleaseIdempotencyKey({ id: 321 })
  assert.match(first.replace('team-leader-active-order-release-321-', ''), uuidPattern)
  assert.equal(keys.getOrCreateActiveOrderReleaseIdempotencyKey({ id: 321 }), first)
  assert.notEqual(keys.getOrCreateActiveOrderReleaseIdempotencyKey({ id: 322 }), first)
  keys.releaseKeys.delete(321)
  assert.notEqual(keys.getOrCreateActiveOrderReleaseIdempotencyKey({ id: 321 }), first)
})

test('HTTP version upgrade submits the prefixed UUID and preserves per-submit generation', async () => {
  const keys = setup()
  await keys.submitActiveOrderVersionUpgrade()
  assert.equal(keys.writes.length, 1)
  const first = keys.writes[0]
  assert.match(first.idempotencyKey.replace('active-order-version-upgrade-321-', ''), uuidPattern)
  assert.equal(first.activeOrderId, 321)
  assert.equal(first.upgradeReason, '升级原因')
  assert.equal(first.confirmRestartFromBeginning, true)
  assert.equal(keys.context.activeOrderVersionUpgradeSubmittingId.value, undefined)
  await keys.submitActiveOrderVersionUpgrade()
  assert.notEqual(keys.writes[1].idempotencyKey, first.idempotencyKey)
})

test('missing entropy never caches an active order release key or writes a version upgrade', async () => {
  const keys = setup({})
  assert.throws(() => keys.getOrCreateActiveOrderReleaseIdempotencyKey({ id: 321 }), /不支持安全随机数/)
  assert.equal(keys.releaseKeys.size, 0)
  await keys.submitActiveOrderVersionUpgrade()
  assert.equal(keys.writes.length, 0)
  assert.equal(keys.errors.length, 1)
  assert.match(keys.errors[0], /不支持安全随机数/)
  assert.equal(keys.context.activeOrderVersionUpgradeSubmittingId.value, undefined)
  assert.equal(keys.context.activeOrderVersionUpgradeDialogVisible.value, true)
})
