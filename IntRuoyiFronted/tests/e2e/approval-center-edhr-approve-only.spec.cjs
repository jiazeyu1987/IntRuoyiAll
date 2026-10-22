const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const { ref, reactive } = require('vue')
const { completeApprovalWrite, approvalTaskIdentityKey, createApprovalInvalidationState } =
  require('../../src/store/modules/approvalCenterInvalidationState.ts')

const source = fs.readFileSync('src/views/approval-center/index.vue', 'utf8')
const { bpmMissingPermissionReason } = require('../../src/views/approval-center/approvalReadDisplay.ts')
const body = (name, next) => source.slice(source.indexOf(`const ${name} =`), source.indexOf(`const ${next} =`))
const canReview = vm.runInNewContext(body('canReview', 'canReviewInModule')
  .replace('(row: ApprovalTaskSummaryVO)', '(row)') + '\ncanReview', { queryParams: { viewType: 'TODO' }, bpmMissingPermissionReason })
const row = (moduleCode, availableActions, extra = {}) => ({ moduleCode, availableActions, ...extra })
assert.equal(canReview(row('EDHR', ['APPROVE', 'PROCESS_IN_MODULE'])), true)
assert.equal(canReview(row('EDHR', ['PROCESS_IN_MODULE'])), false)
assert.equal(canReview(row('EDHR', ['APPROVE'], { businessDeleted: true })), false)
assert.equal(canReview(row('DCC', ['APPROVE'])), false)
assert.equal(canReview(row('BPM', ['APPROVE', 'REJECT'])), true)
const submit = body('submitReview', 'normalizeApprovalDisplayText')
assert.ok(submit.indexOf('availableActions?.includes(reviewForm.result)') < submit.indexOf('await reviewApprovalTask('))
assert.match(source, /v-if="reviewTask\?\.availableActions\?\.includes\('REJECT'\)" label="REJECT"/)
assert.match(source, /v-if="reviewTask\?\.availableActions\?\.includes\('APPROVE'\)" label="APPROVE"/)

// This executes the component handler and production write coordinator; it is not browser E2E.
const verifySubmission = async (result, options = {}) => {
  const calls = []
  const messages = []
  const reviewDialogVisible = ref(true)
  const reviewSubmitting = ref(false)
  const reviewTask = ref(row('EDHR', ['APPROVE', 'PROCESS_IN_MODULE'], {
    sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '9007199254740993',
    businessKey: '880', processInstanceId: 'process-880', ...options.task
  }))
  const reviewForm = reactive({ result, reason: ' reviewed ', signaturePassword: ' test-only ', ...options.form })
  const session = Object.freeze({ key: 'tenant1:user12:session1', tenantId: '1', loginUserId: '12' })
  const invalidationState = createApprovalInvalidationState()
  invalidationState.setSession(session.key)
  const captures = [], publications = [], refreshes = []
  const approvalInvalidation = {
    syncSession: () => session,
    isCurrent: key => key === session.key,
    captureEdhrSuccess: task => {
      const event = invalidationState.capture(session, task)
      captures.push(event)
      return event
    },
    publishEdhrSuccess: event => {
      publications.push(event)
      return invalidationState.publish(event)
    }
  }
  const actualCanReview = vm.runInNewContext(body('canReview', 'canReviewInModule')
    .replace('(row: ApprovalTaskSummaryVO)', '(row)') + '\ncanReview',
  { queryParams: { viewType: options.viewType || 'TODO' }, bpmMissingPermissionReason })
  const submitReview = vm.runInNewContext(submit + '\nsubmitReview', {
    canReview: actualCanReview,
    reviewTask,
    reviewForm,
    reviewDialogVisible,
    reviewSubmitting,
    approvalInvalidation,
    approvalTaskIdentityKey,
    completeApprovalWrite,
    reviewGeneration: 1,
    buildApprovalCenterRouteStateKey: () => 'TODO:EDHR:page1',
    loadError: ref(''),
    reviewApprovalTask: async command => {
      calls.push(command)
      if (options.rejection) throw options.rejection
      return options.writeResult
    },
    getList: async force => {
      refreshes.push(force)
      if (options.refreshFailure) throw options.refreshFailure
      return true
    },
    resolveErrorMessage: error => error.message,
    ElMessage: {
      error: message => messages.push({ type: 'error', message }),
      success: message => messages.push({ type: 'success', message }),
      warning: message => messages.push({ type: 'warning', message })
    }
  })
  await submitReview()
  assert.equal(reviewSubmitting.value, false)
  if (options.guard || result === 'REJECT') {
    assert.equal(calls.length, 0)
    assert.equal(reviewDialogVisible.value, true)
    assert.match(messages[0].message, options.guard || /不支持所选审核动作/)
    assert.equal(messages[0].type, 'error')
    assert.equal(captures.length, 0)
    assert.equal(publications.length, 0)
    assert.equal(refreshes.length, 0)
  } else {
    assert.equal(calls.length, 1)
    assert.deepEqual(JSON.parse(JSON.stringify(calls[0])), {
      moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '9007199254740993',
      businessKey: '880', processInstanceId: 'process-880', result: 'APPROVE',
      reason: 'reviewed', signaturePassword: 'test-only'
    })
    assert.equal(captures.length, 1)
    const writeFailed = Boolean(options.rejection) || options.writeResult !== true
    assert.equal(reviewDialogVisible.value, writeFailed)
    assert.equal(publications.length, writeFailed ? 0 : 1)
    assert.equal(refreshes.length, writeFailed ? 0 : 1)
    assert.equal(invalidationState.revision('EDHR', 'TODO'), writeFailed ? 0 : 1)
    assert.equal(invalidationState.revision('EDHR', 'DONE'), writeFailed ? 0 : 1)
    if (writeFailed) {
      assert.equal(messages[0].type, 'error')
      assert.equal(messages[0].message, options.rejection?.message || '审批接口未确认办理成功，请核对正式结果')
      assert.equal(reviewForm.signaturePassword, ' test-only ')
    } else {
      assert.equal(messages[0].type, 'success')
      assert.equal(reviewForm.signaturePassword, '')
      assert.equal(refreshes[0], true)
      assert.equal(publications[0], captures[0])
      assert.equal(captures[0].identity.sourceTaskId, reviewTask.value.sourceTaskId)
      if (options.refreshFailure) {
        assert.equal(messages[1].type, 'warning')
        assert.match(messages[1].message, /办理已成功，刷新失败：read failed/)
      }
    }
  }
}

;(async () => {
  await verifySubmission('REJECT')
  await verifySubmission('APPROVE', { writeResult: true })
  await verifySubmission('APPROVE', { rejection: new Error('stale task') })
  await verifySubmission('APPROVE', { task: { sourceTaskId: undefined }, guard: /缺少正式来源任务 ID/ })
  await verifySubmission('APPROVE', { task: { sourceTaskId: ' ' }, guard: /缺少正式来源任务 ID/ })
  await verifySubmission('APPROVE', { task: { availableActions: ['PROCESS_IN_MODULE'] }, guard: /不支持所选审核动作/ })
  await verifySubmission('APPROVE', { task: { businessDeleted: true }, guard: /不支持所选审核动作/ })
  await verifySubmission('APPROVE', { viewType: 'DONE', guard: /不支持所选审核动作/ })
  await verifySubmission('APPROVE', { form: { reason: ' ' }, guard: /必须填写审批意见/ })
  await verifySubmission('APPROVE', { form: { signaturePassword: ' ' }, guard: /电子签名密码/ })
  await verifySubmission('APPROVE', { writeResult: false })
  await verifySubmission('APPROVE', { writeResult: undefined })
  await verifySubmission('APPROVE', { writeResult: true, refreshFailure: new Error('read failed') })
  console.log('PASS: 13 EDHR handler scenarios with formal identity, capability guards and production write/invalidation dependencies')
})()
  .catch(error => { console.error(error); process.exitCode = 1 })
