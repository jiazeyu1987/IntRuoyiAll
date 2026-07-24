const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const listPage = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')

const forbiddenDetailCalls = ['approveEdhrRelease', 'rejectEdhrRelease', 'withdrawEdhrRelease']

for (const call of forbiddenDetailCalls) {
  if (detailPage.includes(call)) {
    throw new Error(`BatchExecutionDetailPage must not call legacy release approval endpoint directly: ${call}`)
  }
}

const forbiddenReleaseApprovalTokens = [
  'submitReleaseThroughFormCenter',
  'releaseTransactionStartUserSelectTasks',
  'releaseTransactionStartUserSelectAssignees',
  'resolveBusinessAction(context)',
  'createFormInstance({',
  'submitFormInstance(draft.id',
  '放行审批人加载失败',
  '请选择${task.name}审批人'
]

for (const token of forbiddenReleaseApprovalTokens) {
  if (detailPage.includes(token)) {
    throw new Error(`BatchExecutionDetailPage still contains release approval submit wiring: ${token}`)
  }
}

const forbiddenListCalls = ['requestVoidBatchExecution', 'withdrawVoidBatchExecution']

for (const call of forbiddenListCalls) {
  if (listPage.includes(call)) {
    throw new Error(`BatchExecutionListPage must route eDHR void through form-center, found direct call ${call}`)
  }
}

const requiredDetailTokens = [
  'findActiveBusinessAction',
  'submitEdhrRelease',
  'submitReleaseByOwnerSignature',
  'releaseTransactionForm.password',
  '负责人电子签名密码不能为空',
  '放行已完成',
  "actionCode: 'RELEASE'",
  "objectType: 'EDHR_BATCH_EXECUTION'"
]

for (const token of requiredDetailTokens) {
  if (!detailPage.includes(token)) {
    throw new Error(`BatchExecutionDetailPage missing owner-signature release token: ${token}`)
  }
}

const requiredListTokens = [
  'findActiveBusinessAction',
  'resolveBusinessAction',
  'createFormInstance',
  'submitFormInstance',
  "actionCode: 'VOID'",
  "objectType: 'EDHR_BATCH_EXECUTION'"
]

for (const token of requiredListTokens) {
  if (!listPage.includes(token)) {
    throw new Error(`BatchExecutionListPage missing form-center void token: ${token}`)
  }
}

console.log('GREEN: eDHR release uses owner signature and void uses form-center platform wiring')
