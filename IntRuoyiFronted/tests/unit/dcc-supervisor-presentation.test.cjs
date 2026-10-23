const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const cache = new Map()
const base = 'src/views/dcc/controlled-file/'
function load(file) {
  if (cache.has(file)) return cache.get(file)
  const context = { exports: {}, require: name => {
    if (name === '../shared/lifecycle' || name === './lifecycle') return load(base + 'shared/lifecycle.ts')
    if (name === './stage-name') return load(base + 'shared/stage-name.ts')
    if (name === '@/api/dcc/controlledFile/workflow') return {}
    if (name === '@/api/form-center/actionProjection') return {}
    throw new Error('Unexpected module: ' + name)
  } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  cache.set(file, context.exports)
  return context.exports
}
test('actual status and handling summary name the direct supervisor without adding public matrix stages', () => {
  const lifecycle = load(base + 'shared/lifecycle.ts')
  const handling = load(base + 'shared/handlingSummary.ts')
  assert.equal(lifecycle.getDccControlledFileStatusLabel('PENDING_SUPERVISOR_REVIEW'), '待直属主管审核')
  const summary = handling.getControlledFileHandlingSummary({ status: 'PENDING_SUPERVISOR_REVIEW' })
  assert.equal(summary.nextStep, '等待直属主管审核')
  assert.equal(summary.responsibilityHint, '责任：直属主管')
  assert.equal(lifecycle.getDccControlledFileStatusLabel('PENDING_MATRIX_REVIEW'), '待会签审核')
  assert.deepEqual(Array.from(lifecycle.DCC_CONTROLLED_FILE_STAGE_OPTIONS, row => row.value),
    ['DOC_CONTROL_REVIEW', 'MATRIX_REVIEW', 'MATRIX_APPROVAL', 'DOC_CONTROL_APPROVAL'])
})
test('actual supervisor signature meanings and same-round task duty remain separate from other signatures', () => {
  const evidence = load(base + 'shared/signature-evidence.ts')
  const meaning = evidence.DCC_SIGNATURE_MEANING_OPTIONS
  assert.equal(meaning.find(row => row.value === 'SUPERVISOR_REVIEW_APPROVE')?.label, '直属主管审核通过')
  assert.equal(meaning.find(row => row.value === 'SUPERVISOR_REVIEW_REJECT')?.label, '直属主管审核驳回')
  const native = load(base + 'detail/native-approval-progress.ts')
  const scope = { fileId: '10', bpmRound: 'round', applicationType: 'UPLOAD' }
  const tasks = [{ id: 'task', processInstanceId: 'round', taskDefinitionKey: 'SUPERVISOR_REVIEW', status: 2 }]
  for (const actionType of ['APPROVE', 'REJECT'])
    assert.equal(native.resolveSignatureDuty({ taskId: 'task', actionType }, tasks, scope), '直属主管')
  assert.equal(native.resolveSignatureDuty({ taskId: 'task', actionType: 'ASSIGN' }, tasks, scope), '签名职责未记录')
  assert.equal(native.resolveSignatureDuty({ taskId: 'task', actionType: 'APPROVE' },
    [{ ...tasks[0], processInstanceId: 'older' }], scope), '签名职责未记录')
})
test('actual stage labels retain recorded names and use the supervisor label only for missing names', () => {
  const stages = load(base + 'shared/stage-name.ts')
  assert.equal(stages.resolveDccStageDisplayName('SUPERVISOR_REVIEW', null), '直属主管审核')
  assert.equal(stages.resolveDccTimelineActivityName('SUPERVISOR_REVIEW', '？'), '直属主管审核')
  assert.equal(stages.resolveDccStageDisplayName('SUPERVISOR_REVIEW', '研发主管签审'), '研发主管签审')
  assert.equal(stages.resolveDccStageDisplayName('MATRIX_REVIEW', null), '会签审核')
})

test('actual supervisor runtime lookup provides the signed stage without changing matrix or withdrawal choices', () => {
  const lifecycle = load(base + 'shared/lifecycle.ts')
  for (const stage of [
    lifecycle.getDccControlledFileStageByKey('SUPERVISOR_REVIEW'),
    lifecycle.getDccControlledFileStageByStatus('PENDING_SUPERVISOR_REVIEW')
  ]) {
    assert.equal(stage?.key, 'SUPERVISOR_REVIEW')
    assert.equal(stage.label, '直属主管审核')
    assert.equal(stage.requiredPermission, 'REVIEW')
    assert.equal(stage.order, 0)
  }
  assert.equal(lifecycle.isDccControlledFileWithdrawableStatus('PENDING_SUPERVISOR_REVIEW'), false)
  assert.deepEqual(Array.from(lifecycle.DCC_CONTROLLED_FILE_STAGE_KEYS),
    ['DOC_CONTROL_REVIEW', 'MATRIX_REVIEW', 'MATRIX_APPROVAL', 'DOC_CONTROL_APPROVAL'])
  assert.equal(lifecycle.getDccControlledFileStageByKey('UNKNOWN_REVIEW'), undefined)
})

test('actual frozen supervisor snapshot and running task are retained with their stage zero and counts', () => {
  const approval = load(base + 'shared/approval.ts')
  const snapshots = [
    { stageCode: 'SUPERVISOR_REVIEW', stageOrder: 0, resolvedUserIds: [7] },
    { stageCode: 'MATRIX_REVIEW', stageOrder: 1, resolvedUserIds: [8] }
  ]
  const tasks = [{ id: 'supervisor', taskDefinitionKey: 'SUPERVISOR_REVIEW', status: 1 }]
  const stages = approval.buildDccTaskStageProgress({
    routeSnapshots: snapshots, taskList: tasks, fileStatus: 'PENDING_SUPERVISOR_REVIEW'
  })
  assert.deepEqual(Array.from(stages, row => row.stageCode), ['SUPERVISOR_REVIEW', 'MATRIX_REVIEW'])
  assert.equal(stages[0].stageName, '直属主管审核')
  assert.equal(stages[0].stageOrder, 0)
  assert.equal(stages[0].runningCount, 1)
  assert.equal(stages[0].approvedCount, 0)
  assert.equal(stages[0].completionText, '0/1')
  assert.equal(stages[0].isCurrent, true)
  assert.equal(stages[1].isPending, true)
  const completed = approval.buildDccTaskStageProgress({
    routeSnapshots: snapshots, taskList: [{ ...tasks[0], status: 2 }], fileStatus: 'PENDING_MATRIX_REVIEW'
  })
  assert.equal(completed[0].approvedCount, 1)
  assert.equal(completed[0].isCompleted, true)
})

test('actual approval signature dialog explains the supervisor duty before signing', () => {
  const actions = load(base + 'detail/approval-actions.ts')
  assert.equal(actions.getDccApprovalActionLabels('SUPERVISOR_REVIEW').dialogTitle, '直属主管审核签名')
  assert.equal(actions.getDccApprovalSignatureMeaningPreview('SUPERVISOR_REVIEW', 'approve'), '直属主管审核通过')
  assert.equal(actions.getDccApprovalSignatureMeaningPreview('SUPERVISOR_REVIEW', 'reject'), '直属主管审核驳回')
  assert.equal(actions.getDccApprovalActionLabels('MATRIX_APPROVAL').approveText, '批准通过')
  assert.equal(actions.getDccApprovalActionLabels('DOC_CONTROL_REVIEW').dialogTitle, '文控审核签名')
})

test('explicit unknown runtime codes cannot borrow a legacy numeric node identity', () => {
  const approval = load(base + 'shared/approval.ts')
  assert.throws(() => approval.buildDccTaskStageProgress({
    routeSnapshots: [
      { stageCode: 'UNKNOWN_REVIEW', stageNo: 1, resolvedUserIds: [7] },
      { stageNo: 2, resolvedUserIds: [8] }
    ],
    taskList: [], fileStatus: 'PENDING_MATRIX_REVIEW'
  }), /审批节点代码未支持.*UNKNOWN_REVIEW/)
  const stages = approval.buildDccTaskStageProgress({
    routeSnapshots: [{ stageNo: 2, resolvedUserIds: [8] }],
    taskList: [], fileStatus: 'PENDING_MATRIX_REVIEW'
  })
  assert.deepEqual(Array.from(stages, row => row.stageCode), ['MATRIX_REVIEW'])
  assert.equal(stages[0].isCurrent, true)
})
