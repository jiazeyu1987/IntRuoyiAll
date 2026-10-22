const { test } = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const file = require('node:path').resolve(__dirname, '../../src/views/dcc/controlled-file/detail/native-approval-progress.ts')
const c = { exports: {}, Error }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
const scope = () => ({ fileId: '20', bpmRound: 'R', applicationType: 'REVISION', flowContract: {
  controlledFileId: '20', applicationType: 'REVISION', bpmRound: 'R', attributeRound: 7, status: 'READY', unavailableReason: null,
  processDefinitionId: 'revision:6:exact', deploymentId: 'deployment', definitionDigest: 'a'.repeat(64), executionContract: 'PUBLIC_MATRIX_NATIVE_V2',
  flowRule: 'PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM', needTraining: true } })
const facts = () => ({ fileId: '20', fileBpmRound: 'R', effectiveMode: 'DELAYED', status: 'PENDING_DELAYED_EFFECTIVE_CONFIRM', trainingRecordAvailable: true })
const task = (status = 1) => ({ id: 'T', processInstanceId: 'R', taskDefinitionKey: 'DELAYED_EFFECTIVE_CONFIRM', status })
test('exact V2 shows supervisor document then training and actual compiler confirmation in order', () => {
  const stages = c.exports.buildNativeApprovalProgress(scope(), facts(), [task()])
  assert.deepEqual(Array.from(stages, x => x.stageCode), ['SUPERVISOR_REVIEW', 'MATRIX_REVIEW', 'MATRIX_APPROVAL', 'DOC_CONTROL_REVIEW', 'APPLICANT_TRAINING_RECORD', 'DELAYED_EFFECTIVE_CONFIRM', 'CONTROLLED', 'DISTRIBUTED'])
  const confirm = stages.find(x => x.stageCode === 'DELAYED_EFFECTIVE_CONFIRM')
  assert.equal(confirm.isCurrent, true); assert.equal(confirm.isCompleted, false)
  assert.equal(stages.find(x => x.stageCode === 'APPLICANT_TRAINING_RECORD').isCompleted, true)
  assert.match(c.exports.getNativeApprovalProcessText(scope()), /延迟.*确认/)
})
test('file control facts never replace real same-round confirmation task completion', () => {
  const completed = c.exports.buildNativeApprovalProgress(scope(), { ...facts(), status: 'ACTIVE', controlledTime: 'S', activatedTime: 'E' }, [task(2)])
  assert.equal(completed.find(x => x.stageCode === 'DELAYED_EFFECTIVE_CONFIRM').isCompleted, true)
  assert.throws(() => c.exports.buildNativeApprovalProgress(scope(), facts(), []), /确认.*任务|任务.*确认/)
  assert.throws(() => c.exports.buildNativeApprovalProgress(scope(), facts(), [{ ...task(), processInstanceId: 'other' }]))
  const future = c.exports.buildNativeApprovalProgress(scope(), { ...facts(), status: 'PENDING_DOC_CONTROL_REVIEW', trainingRecordAvailable: false }, [])
  assert.equal(future.find(x => x.stageCode === 'DELAYED_EFFECTIVE_CONFIRM').isCompleted, false)
})
test('immediate mode has no confirmation task and historical current-file facts cannot fill old rounds', () => {
  const immediate = c.exports.buildNativeApprovalProgress(scope(), { ...facts(), effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', status: 'PENDING_DOC_CONTROL_REVIEW' }, [])
  assert.equal(immediate.some(x => x.stageCode === 'DELAYED_EFFECTIVE_CONFIRM'), false)
  const history = c.exports.buildNativeApprovalProgress(scope(), { ...facts(), fileBpmRound: 'different-current', controlledTime: 'S', status: 'ACTIVE' }, [task(2)])
  assert.equal(history.find(x => x.stageCode === 'CONTROLLED').isCompleted, false)
  assert.equal(history.find(x => x.stageCode === 'DELAYED_EFFECTIVE_CONFIRM').isCompleted, true)
  assert.throws(() => c.exports.buildNativeApprovalProgress({ ...scope(), flowContract: { ...scope().flowContract, executionContract: 'PUBLIC_MATRIX_NATIVE_V1' } }, facts(), [task()]))
  assert.equal(c.exports.resolveSignatureDuty({ taskId: 'T', actionType: 'APPROVE' }, [task(2)], scope()), '原编制人延迟生效确认')
})
