const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const compile = source => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText
const load = (file, dependencies = {}) => {
  const exports = {}
  vm.runInNewContext(compile(read(file)), { exports, require: name => {
    if (!(name in dependencies)) throw new Error(`Unexpected production dependency: ${name}`)
    return dependencies[name]
  } })
  return exports
}
const display = load('src/views/approval-center/approvalReadDisplay.ts')
const page = read('src/views/approval-center/index.vue')
const script = page.split('<script lang="ts" setup>')[1].split('</script>')[0]
const compiled = compile(script)
const declaration = name => {
  const ast = ts.createSourceFile('page.js', compiled, ts.ScriptTarget.ES2020, true)
  const statement = ast.statements.find(node => ts.isVariableStatement(node)
    && node.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement, `Production page declaration ${name}`)
  return statement.getText(ast)
}
const handler = (name, dependencies = {}) => vm.runInNewContext(
  `${declaration(name)}\n${name}`, { approvalReadDisplay_1: display, element_plus_1: { ElMessage: dependencies.ElMessage },
    canOpenView: vm.runInNewContext(`${declaration('canOpenView')}\ncanOpenView`, {
      approvalReadDisplay_1: display,
      resolveDecisionDetailRoute: vm.runInNewContext(`${declaration('resolveDecisionDetailRoute')}\nresolveDecisionDetailRoute`)
    }), ...dependencies })
const facts = extra => ({ moduleCode: 'EDHR', sourceStatus: 'DONE', taskOutcome: 'COMPLETED',
  decisionState: 'UNKNOWN', ...extra })

test('pending EDHR handoff opens its original business handling route, with permission and lifecycle gates', () => {
  const row = { moduleCode: 'EDHR', sourceTaskType: 'ACTIVE_ORDER_HANDOFF', sourceTaskId: '41',
    availableActions: ['PROCESS_IN_MODULE'], decisionDetailRoute: '/mes/pro/feedback/edhr-nonconformance-review',
    decisionDetailQuery: { activeOrderId: '7', reviewId: '71', handoffTaskId: '41', roundId: '71', handoffType: 'QA_REVIEW' } }
  const allowed = handler('canReviewInModule', { queryParams: { viewType: 'TODO' } })
  assert.equal(allowed(row), true)
  assert.equal(allowed({ ...row, availableActions: [] }), false)
  assert.equal(allowed({ ...row, businessDeleted: true }), false)
  assert.equal(allowed({ ...row, decisionDetailRoute: undefined }), false)
  assert.equal(allowed({ ...row, decisionDetailQuery: { ...row.decisionDetailQuery,
    requiredPermissionsMissing: 'mes:pro-edhr-nonconformance-review:query' } }), false)
  assert.equal(handler('canReviewInModule', { queryParams: { viewType: 'DONE' } })(row), false)
  const pushes = []
  handler('openReviewAction', { canReview: () => false, canReviewInModule: allowed,
    openDecisionDetail: handler('openDecisionDetail', { queryParams: { viewType: 'TODO' },
      resolveDecisionDetailRoute: handler('resolveDecisionDetailRoute'),
      resolveDecisionDetailQuery: handler('resolveDecisionDetailQuery'),
      resolveDccApprovalDetailLocation: (_row, path, query) => ({ path, query }),
      router: { push: location => pushes.push(location) }, ElMessage: { error: assert.fail } }),
    ElMessage: { error: assert.fail } })(row)
  assert.deepEqual(JSON.parse(JSON.stringify(pushes)), [{ path: row.decisionDetailRoute, query: row.decisionDetailQuery }])
})

test('direct EDHR mandatory permission matrix disables real view and action handlers with exact reasons', () => {
  const resolveDecisionDetailRoute = handler('resolveDecisionDetailRoute')
  const canOpenView = handler('canOpenView', { resolveDecisionDetailRoute })
  const resolveViewDisabledReason = handler('resolveViewDisabledReason')
  for (const [suffix, label] of [['approve', '批记录审批'], ['track', '批记录跟踪查询'], ['signature-query', '批记录签名查询']]) {
    const row = { moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '8001',
      decisionDetailRoute: '/mes/pro/feedback/edhr-approval/detail',
      decisionDetailQuery: { id: '9001', workTaskId: '8001', bpmTaskId: 'task-9001',
        requiredPermissionsMissing: `mes:pro-batch-record-execution:${suffix}` },
      availableActions: ['APPROVE', 'APPROVE_IN_MODULE'], approvalRemark: '正式意见', capabilities: ['TIMELINE'] }
    for (const viewType of ['TODO', 'DONE']) {
      const queryParams = { viewType }
      assert.equal(canOpenView(row), false)
      assert.equal(handler('canReview', { queryParams })(row), false)
      assert.equal(handler('canReviewInModule', { queryParams })(row), false)
      assert.equal(resolveViewDisabledReason(row), `领域办理缺少权限：${label}`)
      const errors = []
      const dependencies = { queryParams, canOpenView, resolveDecisionDetailRoute, resolveViewDisabledReason,
        router: { push: assert.fail }, ElMessage: { error: value => errors.push(value) } }
      handler('openModuleDetail', dependencies)(row)
      handler('openDecisionDetail', dependencies)(row)
      assert.equal(errors.length, 2)
      assert.equal(row.approvalRemark, '正式意见')
    }
    const permitted = { ...row, decisionDetailQuery: { id: '9001', workTaskId: '8001', bpmTaskId: 'task-9001' } }
    assert.equal(canOpenView(permitted), true)
    assert.equal(handler('canReviewInModule', { queryParams: { viewType: 'TODO' } })(permitted), true)
    const pushes = []
    handler('openDecisionDetail', { queryParams: { viewType: 'TODO' }, canOpenView, resolveDecisionDetailRoute,
      resolveDecisionDetailQuery: handler('resolveDecisionDetailQuery'),
      resolveDccApprovalDetailLocation: (_row, path, query) => ({ path, query }),
      router: { push: value => pushes.push(value) }, ElMessage: { error: assert.fail } })(permitted)
    assert.deepEqual(JSON.parse(JSON.stringify(pushes[0])), { path: permitted.decisionDetailRoute, query: permitted.decisionDetailQuery })
  }
})

test('other EDHR target permissions and unknown navigation contract have explicit labels', () => {
  for (const [code, label] of [['mes:pro-batch-record-execution:query', '批记录查询'],
    ['mes:pro-edhr-batch-execution:query', '批次执行查询'], ['mes:pro-edhr-work-task:query', '工作任务查询'],
    ['mes:pro-production-release:query', '生产放行查询']]) {
    assert.equal(display.bpmMissingPermissionReason({ moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK',
      decisionDetailQuery: { requiredPermissionsMissing: code } }), `领域办理缺少权限：${label}`)
  }
  assert.equal(display.bpmMissingPermissionReason({ moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK',
    decisionDetailQuery: { navigationContractMissing: '目标页面缺少权限合同' } }), '目标页面缺少权限合同')
})

test('handoff target permission matrix disables actual handlers while preserving historical facts', () => {
  for (const [permission, label] of [['mes:pro-feedback:query', '一线生产与PQC查询'],
    ['mes:pro-feedback:create', '一线生产与PQC填写'], ['mes:pro-process-pool-team-leader:query', '组长工作台查询'],
    ['mes:pro-edhr-nonconformance-review:query', '不合格评审查询']]) {
    const row = { moduleCode: 'EDHR', sourceTaskType: 'ACTIVE_ORDER_HANDOFF', sourceTaskId: '41',
      decisionDetailRoute: '/mes/pro/process-pool/production-leader',
      decisionDetailQuery: { activeOrderId: '7', eventId: '71', handoffTaskId: '41', roundId: '71',
        handoffType: 'PRODUCTION_REVIEW', handoffReadOnly: '1', requiredPermissionsMissing: permission },
      availableActions: ['PROCESS_IN_MODULE'], capabilities: ['TIMELINE'], taskOutcome: 'COMPLETED' }
    const reason = `领域办理缺少权限：${label}`
    assert.equal(handler('canOpenView')(row), false)
    assert.equal(handler('resolveViewDisabledReason')(row), reason)
    const errors = []
    for (const name of ['openModuleDetail', 'openDecisionDetail']) handler(name, {
      resolveDecisionDetailRoute: handler('resolveDecisionDetailRoute'), resolveViewDisabledReason: handler('resolveViewDisabledReason'),
      router: { push: assert.fail }, ElMessage: { error: value => errors.push(value) }
    })(row)
    assert.deepEqual(errors, [reason, reason])
    assert.equal(display.edhrReadDisplay(row).taskLabel, '任务已完成')
    assert.deepEqual(row.capabilities, ['TIMELINE'])
    const allowed = { ...row, decisionDetailQuery: { ...row.decisionDetailQuery } }
    delete allowed.decisionDetailQuery.requiredPermissionsMissing
    const pushes = []
    handler('openModuleDetail', { resolveDecisionDetailRoute: handler('resolveDecisionDetailRoute'),
      resolveDecisionDetailQuery: handler('resolveDecisionDetailQuery'),
      resolveDccApprovalDetailLocation: (_row, path, query) => ({ path, query }),
      router: { push: value => pushes.push(value) }, ElMessage: { error: assert.fail } })(allowed)
    assert.deepEqual(JSON.parse(JSON.stringify(pushes[0])), { path: allowed.decisionDetailRoute, query: allowed.decisionDetailQuery })
  }
})

test('all official task outcomes remain separate from an unknown decision', () => {
  for (const [taskOutcome, label] of Object.entries({ WAIT: '待处理', RUNNING: '处理中',
    COMPLETED: '任务已完成', CANCELLED: '已取消', OVERDUE: '已逾期' })) {
    const value = display.edhrReadDisplay(facts({ taskOutcome }))
    assert.equal(value.taskLabel, label)
    assert.equal(value.decisionLabel, '决定未知')
    assert.equal(value.decisionType, 'info')
  }
})
test('only matching formal APPROVE and REJECT produce decision colors', () => {
  for (const [result, label, color] of [['APPROVE', '审批通过', 'success'], ['REJECT', '审批驳回', 'danger']]) {
    const value = display.edhrReadDisplay(facts({ decisionState: result, approvalResult: result }))
    assert.equal(value.decisionLabel, label)
    assert.equal(value.decisionType, color)
  }
  assert.equal(display.edhrReadDisplay(facts({ approvalResult: 'APPROVE' })).decisionType, 'info')
  assert.equal(display.edhrReadDisplay(facts({ decisionState: 'APPROVE', approvalResult: 'REJECT' })).decisionLabel, '决定未知')
})
test('unsupported and missing statuses retain source and business codes', () => {
  const value = display.edhrReadDisplay(facts({ taskOutcome: 'UNSUPPORTED', sourceStatus: 'RAW_TASK_7',
    decisionState: 'UNSUPPORTED', decisionSourceStatus: 'RAW_DECISION_9', businessStatus: 'RAW_BUSINESS_3' }))
  assert.match(value.taskLabel, /RAW_TASK_7/)
  assert.match(value.decisionLabel, /RAW_DECISION_9/)
  assert.equal(value.businessStatus, 'RAW_BUSINESS_3')
  assert.equal(display.edhrReadDisplay(facts()).businessStatus, '未记录')
})
test('both formal decisions preserve exact multiline opinion; missing never uses source or comment', () => {
  const original = '  多行意见\n"引号"和\\反斜线  '
  for (const result of ['APPROVE', 'REJECT']) {
    assert.equal(display.edhrReadDisplay(facts({ decisionState: result, approvalResult: result,
      approvalRemark: original })).opinion, original)
  }
  for (const approvalRemark of [undefined, '', ' \n']) {
    const value = display.edhrReadDisplay(facts({ approvalRemark, sourceRemark: 'SYSTEM 创建说明', comment: '旧文本' }))
    assert.equal(value.opinion, '未记录')
    assert.equal(value.sourceRemark, 'SYSTEM 创建说明')
    assert.equal(value.terminationReason, undefined)
  }
  assert.equal(display.edhrReadDisplay(facts({ terminationReason: '正式终止原因' })).terminationReason, '正式终止原因')
})
test('formal decision actor never comes from a current name, assignee or historical actor fallback', () => {
  const value = display.edhrReadDisplay(facts({ assigneeUserName: '当前姓名', assigneeUserId: 88, actorUserId: 99 }))
  assert.equal(value.actorName, '姓名未记录')
  assert.equal(value.actorIdentity, '未记录')
  assert.equal(display.edhrReadDisplay(facts({ decisionActorUserId: 17 })).actorIdentity, '系统用户 ID：17')
  const reviewer = handler('resolveReviewerLabel', { queryParams: { viewType: 'DONE' } })
  assert.equal(reviewer(facts({ assigneeUserName: '当前姓名', assigneeUserId: 88 })), '姓名未记录；未记录')
  assert.equal(handler('resolveReviewerLabel', { queryParams: { viewType: 'TODO' } })(facts({ assigneeRoleName: '复核员' })), '审批角色：复核员')
  assert.equal(reviewer({ moduleCode: 'DCC', assigneeUserName: '原审核人' }), '原审核人')
  const applicant = handler('resolveApplicantLabel', { queryParams: { viewType: 'DONE' } })
  assert.equal(applicant(facts({ initiatorUserName: '当前目录申请人' })), '姓名未记录')
  assert.equal(applicant({ moduleCode: 'DCC', initiatorUserName: '原申请人' }), '原申请人')
})
test('task times, formal decision time, evidence and domain reference are independent facts', () => {
  const value = display.edhrReadDisplay(facts({ taskCreatedAt: 'create', taskCompletedAt: 'complete',
    decisionAt: 'decide', actedAt: 'legacy', evidenceId: 'signature-1', domainReferenceId: 'task-2' }))
  assert.equal(value.taskCreatedAt, 'create')
  assert.equal(value.taskCompletedAt, 'complete')
  assert.equal(value.decisionAt, 'decide')
  assert.equal(value.evidenceId, 'signature-1')
  assert.equal(value.domainReferenceId, 'task-2')
  const missing = display.edhrReadDisplay(facts({ taskCompletedAt: 'complete', actedAt: 'legacy' }))
  assert.equal(missing.decisionAt, undefined)
  assert.equal(missing.evidenceId, '未记录')
})
test('handoff provider creation and completion shapes keep task actors separate from formal decisions', () => {
  const created = display.edhrReadDisplay({ moduleCode: 'EDHR', action: 'CREATED', status: 'TODO',
    taskOutcome: 'CREATED', sourceStatus: 'TODO', decisionState: 'UNKNOWN', actorUserId: 17,
    taskCreatedAt: 'create', actedAt: 'create' })
  assert.equal(created.taskLabel, '交接已创建')
  assert.equal(created.creationActorIdentity, '系统用户 ID：17')
  assert.equal(created.creationAt, 'create')
  assert.equal(created.taskCompletedAt, undefined)
  assert.equal(created.completionActorIdentity, '未记录')
  assert.equal(created.actorIdentity, '未记录')
  const done = display.edhrReadDisplay(facts({ taskCompletedByUserId: 18, actorUserId: 18,
    taskCompletedAt: 'complete', actedAt: 'complete' }))
  assert.equal(done.completionActorIdentity, '系统用户 ID：18')
  assert.equal(done.actorIdentity, '未记录')
  assert.equal(done.decisionAt, undefined)
  assert.equal(done.creationActorIdentity, '未记录')
  assert.equal(done.decisionLabel, '决定未知')
  assert.ok(page.includes('edhrReadDisplay(row).completionActorIdentity'))
  assert.ok(page.includes('edhrReadDisplay(entry).completionActorIdentity'))
  assert.ok(page.includes('edhrReadDisplay(entry).creationActorIdentity'))
})
test('production disabled reason names every missing mandatory module permission without granting navigation', () => {
  const reason = handler('resolveViewDisabledReason')
  for (const [suffix, label] of [['approve', '批记录审批'], ['track', '批记录跟踪查询'], ['signature-query', '批记录签名查询']]) {
    const row = { moduleCode: 'BPM', availableActions: ['OPEN_DETAIL'],
      decisionDetailQuery: { requiredPermissionsMissing: `mes:pro-batch-record-execution:${suffix}` },
      businessContextTags: ['领域授权不足'] }
    assert.equal(reason(row), `领域办理缺少权限：${label}`)
    assert.equal(display.canProcessBpmInModule(row, 'TODO'), false)
  }
  const value = reason({ moduleCode: 'BPM', decisionDetailQuery: { requiredPermissionsMissing:
    'mes:pro-batch-record-execution:track,mes:pro-batch-record-execution:signature-query' } })
  assert.equal(value, '领域办理缺少权限：批记录跟踪查询、批记录签名查询')
})
const bpm = { moduleCode: 'BPM', availableActions: ['OPEN_DETAIL', 'PROCESS_IN_MODULE'],
  decisionDetailRoute: '/mes/pro/feedback/edhr-approval/detail',
  decisionDetailQuery: { id: '31', workTaskId: '42', bpmTaskId: 'engine-53' },
  detailRoute: '/bpm/process-instance/detail', detailQuery: { id: 'process-64' } }
test('BPM exact formal module navigation uses the production page handler', () => {
  const pushed = []
  const open = handler('openDecisionDetail', { queryParams: { viewType: 'TODO' },
    router: { push: location => pushed.push(location) }, ElMessage: { error: assert.fail } })
  open(bpm)
  assert.equal(pushed[0].path, bpm.decisionDetailRoute)
  assert.deepEqual(JSON.parse(JSON.stringify(pushed[0].query)), bpm.decisionDetailQuery)
  assert.notEqual(pushed[0].query, bpm.decisionDetailQuery)
})
test('BPM action eligibility rejects missing route, read-only views, deleted rows and guessed identity', () => {
  for (const row of [{ ...bpm, decisionDetailRoute: undefined }, { ...bpm, decisionDetailRoute: ' ' },
    { ...bpm, availableActions: ['OPEN_DETAIL'] }, { ...bpm, businessDeleted: true },
    { moduleCode: 'BPM', businessKey: 'edhr:31', businessTitle: 'eDHR', detailRoute: bpm.decisionDetailRoute }]) {
    assert.equal(display.canProcessBpmInModule(row, 'TODO'), false)
    assert.equal(display.bpmModuleDecisionLocation(row, 'TODO'), undefined)
  }
  for (const view of ['DONE', 'CC', 'MY_INITIATED']) assert.equal(display.canProcessBpmInModule(bpm, view), false)
  const errors = []
  const open = handler('openDecisionDetail', { queryParams: { viewType: 'TODO' }, router: { push: assert.fail },
    ElMessage: { error: value => errors.push(value) }, resolveViewDisabledReason: handler('resolveViewDisabledReason') })
  open({ ...bpm, decisionDetailRoute: undefined, businessContextTags: ['缺少正式任务关系'] })
  assert.equal(errors[0], '缺少正式任务关系')
})
test('existing EDHR module tokens and DCC detail handling still use their production branches', () => {
  const canReviewInModule = handler('canReviewInModule', { queryParams: { viewType: 'TODO' } })
  for (const token of ['REVIEW_IN_MODULE', 'APPROVE_IN_MODULE']) assert.equal(canReviewInModule({ moduleCode: 'EDHR', availableActions: [token] }), true)
  assert.equal(canReviewInModule(bpm), true)
  const queryParams = { viewType: 'TODO' }
  const isDccModuleHandlingAction = handler('isDccModuleHandlingAction', { queryParams })
  const resolveDccApprovalDetailLocation = handler('resolveDccApprovalDetailLocation', {
    isDccModuleHandlingAction, DCC_CONTROLLED_FILE_DETAIL_ROUTE_PREFIX: '/dcc/controlled-file/detail/', DCC_APPROVAL_HANDLING_MODE: 'approval' })
  const result = resolveDccApprovalDetailLocation({ moduleCode: 'DCC', availableActions: ['PROCESS_IN_MODULE'],
    sourceTaskId: 'dcc-task', processInstanceId: 'dcc-process' }, '/dcc/controlled-file/detail/7', { id: '7' })
  assert.equal(result.query.handling, 'approval')
  assert.equal(result.query.taskId, 'dcc-task')
  assert.equal(result.query.processInstanceId, 'dcc-process')
  const pushes = []
  const open = handler('openDecisionDetail', { queryParams, router: { push: value => pushes.push(value) },
    resolveDecisionDetailRoute: handler('resolveDecisionDetailRoute'), resolveDecisionDetailQuery: handler('resolveDecisionDetailQuery'),
    resolveDccApprovalDetailLocation, ElMessage: { error: assert.fail } })
  open({ moduleCode: 'EDHR', detailRoute: '/original/edhr', detailQuery: { workTaskId: '8' } })
  assert.equal(pushes[0].path, '/original/edhr')
  assert.equal(pushes[0].query.workTaskId, '8')
})
test('capabilities keep signature and timeline, at most two others, complete overflow without mutating facts', () => {
  const row = { requiresSignature: true, capabilities: ['NOTIFICATION', 'REMINDER', 'AUDIT', 'TIMELINE', 'SIGNATURE_AUTHORIZATION', 'EVIDENCE_LEDGER'] }
  const value = display.compactApprovalCapabilities(row)
  assert.deepEqual(Array.from(value.visible), ['TIMELINE', 'NOTIFICATION', 'REMINDER'])
  assert.deepEqual(Array.from(value.overflow), ['AUDIT', 'SIGNATURE_AUTHORIZATION', 'EVIDENCE_LEDGER'])
  assert.equal(row.capabilities.length, 6)
  assert.match(page, /trigger="click"[\s\S]*aria-label="查看全部审批能力"/)
  assert.match(page, /v-for="capability in row\.capabilities \|\| \[\]"/)
  assert.equal(display.APPROVAL_CAPABILITIES_DEFAULT_WIDTH, 180)
})
test('actual column hook retains saved widths and visibility under the unchanged table preference identity', async () => {
  let resets = 0
  const requests = []
  const columns = load('src/hooks/web/useUserTableColumns.ts', {
    vue: { ref: value => ({ value }), computed: getter => ({ get value() { return getter() } }), onMounted: () => {} },
    'element-plus': { ElMessage: { error: assert.fail }, ElMessageBox: { confirm: assert.fail } },
    '@/api/system/userTableColumnConfig': {
      getUserTableColumnConfig: async key => { requests.push(key); return { columns: [{ key: 'capabilities', width: 312, visible: false }] } },
      resetUserTableColumnConfig: () => resets++, saveUserTableColumnConfig: assert.fail
    }
  })
  const defaults = vm.runInNewContext(`${declaration('approvalDefaultColumns')}\napprovalDefaultColumns`, { approvalReadDisplay_1: display })
  const control = columns.useUserTableColumns('approval.center.done.applicant.v1', defaults)
  assert.equal(control.getColumnWidth('capabilities'), 180)
  await control.loadConfig()
  assert.equal(control.getColumnWidth('capabilities'), 312)
  assert.equal(control.isColumnVisible('capabilities'), false)
  assert.equal(resets, 0)
  assert.deepEqual(requests, ['approval.center.done.applicant.v1'])
  for (const key of ['todo', 'done', 'myInitiated', 'cc']) assert.ok(page.includes(`approval.center.${key}.applicant.v1`))
  assert.match(page, /@column-change="saveApprovalColumnConfig"/)
  assert.match(page, /v-bind="sortColumnAttrs\('capabilities'\)"/)
})
test('template wires list and timeline through the same formal read facts', () => {
  for (const target of ['row', 'entry']) {
    for (const field of ['opinion', 'decisionLabel', 'decisionSourceStatus', 'evidenceId']) assert.ok(page.includes(`edhrReadDisplay(${target}).${field}`))
    for (const field of ['taskCompletedAt', 'decisionAt']) assert.ok(page.includes(`formatEdhrTime(${target}.${field})`))
  }
  assert.match(page, /entry\.moduleCode === 'EDHR' \? edhrReadDisplay\(entry\)\.decisionType/)
  assert.match(page, /white-space: pre-wrap/)
  assert.match(page, /label="审批来源"/)
  assert.match(page, /BPM: '流程审批'/)
  assert.match(page, /canReview\(row\) \? '审核' : '前往业务办理'/)
})
test('submission fails before identity capture or write when the formal source task ID is missing', async () => {
  for (const sourceTaskId of [undefined, '', ' \n', 12]) {
    const errors = []
    const submit = handler('submitReview', {
      reviewSubmitting: { value: false },
      reviewTask: { value: { moduleCode: 'EDHR', sourceTaskId, availableActions: ['APPROVE'] } },
      reviewForm: { result: 'APPROVE', reason: '用户意见', signaturePassword: 'password' },
      canReview: () => true,
      ElMessage: { error: value => errors.push(value) },
      approvalInvalidation: { syncSession: assert.fail, captureEdhrSuccess: assert.fail },
      reviewApprovalTask: assert.fail
    })
    await submit()
    assert.equal(errors.length, 1)
    assert.match(errors[0], /正式来源任务 ID/)
  }
  assert.match(page, /const task = Object\.freeze\(\{ \.\.\.reviewTask\.value, sourceTaskId \}\)/)
  assert.match(page, /reviewTask\.value\.sourceTaskId === task\.sourceTaskId && approvalTaskIdentityKey/)
  assert.doesNotMatch(page, /approvalTaskIdentityKey\(reviewTask\.value!\)/)
})
test('actual submission freezes the lossless source identity before capturing success and writing', async () => {
  const sourceTaskId = '9223372036854775806'
  const taskRef = { value: { moduleCode: 'EDHR', sourceTaskId, sourceTaskType: 'FORMAL_TASK',
    processInstanceId: 'formal-process', availableActions: ['APPROVE'] } }
  let captured
  let command
  let published = 0
  const submit = handler('submitReview', {
    reviewSubmitting: { value: false }, reviewTask: taskRef,
    reviewPreview: { value: { prepareId: 'prepare-lossless', content: { allowedActions: ['APPROVE'], subjectVersion: 'EDHR1:lossless-version', contentHash: 'a'.repeat(64), contentPolicyVersion: 'EDHR_CONTENT_V1' } } }, reviewIdempotencyKey: { value: 'review-lossless' }, reviewPreviewError: { value: '' },
    reviewForm: { result: 'APPROVE', reason: '用户原意见', signaturePassword: 'test-only' },
    canReview: () => true, reviewGeneration: 1,
    approvalInvalidation: {
      syncSession: () => ({ key: 'session' }), isCurrent: () => true,
      captureEdhrSuccess: task => { captured = task; return { sourceTaskId: task.sourceTaskId } },
      publishEdhrSuccess: () => published++
    },
    buildApprovalCenterRouteStateKey: () => 'route',
    approvalCenterInvalidationState_1: {
      approvalTaskIdentityKey: task => JSON.stringify([task.moduleCode, task.sourceTaskType, task.sourceTaskId, task.processInstanceId]),
      completeApprovalWrite: async options => {
        await options.write()
        options.publish()
        options.success()
      }
    },
    approval_center_1: { reviewApprovalTask: async value => {
      command = value
      taskRef.value = { ...taskRef.value, sourceTaskId: 'different-source' }
      return { receiptId: 'receipt-lossless', signatureId: 'signature-lossless', prepareId: value.prepareId, idempotencyKey: value.idempotencyKey, evidenceHash: 'b'.repeat(64), resultCode: 'APPROVE', resultingStatus: 'RELEASED', committedAt: '2026-10-10T00:00:00Z' }
    } },
    ElMessage: { error: assert.fail, success: assert.fail }
  })
  await submit()
  assert.equal(Object.isFrozen(captured), true)
  assert.equal(captured.sourceTaskId, sourceTaskId)
  assert.equal(command.sourceTaskId, sourceTaskId)
  assert.equal(command.processInstanceId, 'formal-process')
  assert.equal(command.prepareId, 'prepare-lossless')
  assert.equal(command.idempotencyKey, 'review-lossless')
  assert.equal(command.expectedSubjectVersion, 'EDHR1:lossless-version')
  assert.equal(command.expectedContentHash, 'a'.repeat(64))
  assert.equal(published, 1)
})
