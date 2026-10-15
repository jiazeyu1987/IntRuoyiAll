const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')

function load(response, failure) {
  const calls = []
  const context = { exports: {}, require: name => {
    if (name === '@/config/axios') return { default: { get: async request => {
      calls.push(request)
      if (failure) throw failure
      return response
    } } }
    if (name.endsWith('project-attributes/state')) return { validateAttributes: value => value }
    throw new Error('Unexpected dependency: ' + name)
  } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/api/dcc/controlledFile/applicationRead.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return { api: context.exports, calls }
}
const row = { controlledFileId: '20', applicationType: 'UPLOAD', bpmRound: 'round-1', attributeRound: 1 }
const flow = {
  tenantId: '1', controlledFileId: '20', applicationType: 'UPLOAD', bpmRound: 'round-1', attributeRound: 1,
  status: 'READY', unavailableReason: null, processDefinitionKey: 'dcc-controlled-file-upload',
  processDefinitionId: 'upload:5:opaque', deploymentId: 'deployment-A', definitionDigest: 'a'.repeat(64),
  executionContract: 'PUBLIC_MATRIX_NATIVE_V1', flowRule: 'PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL',
  needTraining: false, formInstanceId: null, formInstanceCode: null
}
test('existing round API preserves each exact old and new server contract without changing request parameters', async () => {
  const oldRow = { ...row, bpmRound: 'older', attributeRound: 2, flowContract: {
    ...flow, bpmRound: 'older', attributeRound: 2, processDefinitionId: 'upload:4:old',
    executionContract: 'LEGACY_V4_ACTION', flowRule: 'LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL', needTraining: true
  } }
  const state = load([{ ...row, flowContract: flow }, oldRow])
  const result = await state.api.getControlledFileApplicationRounds('20')
  assert.equal(result[0].flowContract.processDefinitionId, flow.processDefinitionId)
  assert.equal(result[1].flowContract.flowRule, 'LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL')
  assert.equal(result[1].flowContract.needTraining, true)
  assert.equal(state.calls.length, 1)
  assert.equal(state.calls[0].url, '/dcc/controlled-files/20/application-rounds')
  assert.equal(state.calls[0].params, undefined)
})
test('flow response rejects foreign or incomplete formal identities rather than filling them from the mapping', async () => {
  for (const change of [
    { controlledFileId: '21' }, { applicationType: 'REVISION' }, { bpmRound: 'different' },
    { attributeRound: 2 }, { tenantId: 9223372036854775000 }, { tenantId: '' },
    { processDefinitionId: '' }, { deploymentId: ' deployment-A' }, { definitionDigest: 'bad' },
    { processDefinitionKey: 'dcc-controlled-file-revision' }
  ]) {
    await assert.rejects(load([{ ...row, flowContract: { ...flow, ...change } }])
      .api.getControlledFileApplicationRounds('20'), /流程合同/)
  }
})
test('READY requires an exact supported action rule, execution contract and this-round training choice', async () => {
  for (const change of [
    { status: 'OTHER' }, { executionContract: 'LEGACY_V4_ACTION' },
    { flowRule: 'PUBLIC_V5_OBSOLETE_APPROVAL_END' }, { needTraining: null },
    { needTraining: 'false' }, { unavailableReason: 'HISTORICAL_CONTRACT_NOT_RECORDED' },
    { formInstanceId: '50', formInstanceCode: 'FORM-50' }
  ]) {
    await assert.rejects(load([{ ...row, flowContract: { ...flow, ...change } }])
      .api.getControlledFileApplicationRounds('20'), /流程合同/)
  }
})
test('verified unavailable history retains mapping facts without supplying an executable flow', async () => {
  for (const [status, unavailableReason] of [
    ['NOT_RECORDED', 'HISTORICAL_CONTRACT_NOT_RECORDED'],
    ['UNSUPPORTED_DEFINITION', 'UNSUPPORTED_HISTORICAL_GRAPH']
  ]) {
    const unavailable = { ...flow, status, unavailableReason, executionContract: null, flowRule: null }
    const result = await load([{ ...row, flowContract: unavailable }]).api.getControlledFileApplicationRounds('20')
    assert.equal(result[0].bpmRound, row.bpmRound)
    assert.equal(result[0].flowContract.unavailableReason, unavailableReason)
    assert.equal(result[0].flowContract.flowRule, null)
  }
  for (const change of [
    { status: 'NOT_RECORDED', unavailableReason: null, executionContract: null, flowRule: null },
    { status: 'UNSUPPORTED_DEFINITION', unavailableReason: 'UNSUPPORTED_HISTORICAL_GRAPH' }
  ]) {
    await assert.rejects(load([{ ...row, flowContract: { ...flow, ...change } }])
      .api.getControlledFileApplicationRounds('20'), /流程合同/)
  }
})
test('obsolete metadata uses its independent form identity and never creates a training or native-upload contract', async () => {
  const obsolete = { ...row, applicationType: 'OBSOLETE' }
  const obsoleteFlow = { ...flow, applicationType: 'OBSOLETE', processDefinitionKey: 'dcc-controlled-file-obsolete',
    flowRule: 'PUBLIC_V5_OBSOLETE_APPROVAL_END', needTraining: null, formInstanceId: '50', formInstanceCode: 'FORM-50' }
  const result = await load([{ ...obsolete, flowContract: obsoleteFlow }]).api.getControlledFileApplicationRounds('20')
  assert.equal(result[0].flowContract.formInstanceCode, 'FORM-50')
  for (const change of [{ formInstanceId: null }, { formInstanceCode: null }, { needTraining: false },
    { flowRule: 'PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL' }]) {
    await assert.rejects(load([{ ...obsolete, flowContract: { ...obsoleteFlow, ...change } }])
      .api.getControlledFileApplicationRounds('20'), /流程合同/)
  }
})
test('training-only progress rejects another-round contract while retaining exact endpoint and server errors', async () => {
  const progress = { ...row, flowContract: flow,
    tasks: [{ id: 'review', processInstanceId: row.bpmRound, taskDefinitionKey: 'MATRIX_REVIEW', status: 2 }] }
  const state = load(progress)
  assert.equal((await state.api.getControlledFileApplicationProgress('20', row.bpmRound)).flowContract.needTraining, false)
  assert.equal(state.calls[0].params.bpmRound, row.bpmRound)
  await assert.rejects(load({ ...progress, flowContract: { ...flow, bpmRound: 'other' } })
    .api.getControlledFileApplicationProgress('20', row.bpmRound), /流程合同/)
  const failure = new Error('actual source unavailable')
  await assert.rejects(load(progress, failure).api.getControlledFileApplicationProgress('20', row.bpmRound),
    error => error === failure)
})
test('old mapping responses do not acquire a guessed workflow contract and malformed supplied objects fail', async () => {
  for (const supplied of [undefined, null]) {
    const result = await load([{ ...row, flowContract: supplied }]).api.getControlledFileApplicationRounds('20')
    assert.equal(result[0].flowContract, supplied)
    assert.equal(result[0].bpmRound, row.bpmRound)
  }
  for (const supplied of ['PUBLIC_MATRIX_NATIVE_V1', [], {}]) {
    await assert.rejects(load([{ ...row, flowContract: supplied }]).api.getControlledFileApplicationRounds('20'), /流程合同/)
  }
})
