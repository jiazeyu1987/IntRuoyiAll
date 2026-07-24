import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const readText = (path) => readFileSync(path, 'utf8')

test('workflow api exposes applicant training-record upload and fourth-node recipient plan contracts', () => {
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')
  const approvalActionSource = readText('src/views/dcc/controlled-file/detail/approval-actions.ts')
  const approveContract = workflowSource.match(/export interface ControlledFileApproveTaskReqVO \{[\s\S]*?\n\}/)?.[0] ?? ''
  const approvalActionForm = approvalActionSource.match(/export interface DccApprovalActionForm \{[\s\S]*?\n\}/)?.[0] ?? ''

  assert.match(workflowSource, /export interface ControlledFileTrainingRecordReqVO/)
  assert.match(workflowSource, /trainingRecordFileId:\s*number/)
  assert.match(workflowSource, /export const uploadControlledFileTrainingRecord/)
  assert.match(workflowSource, /\/dcc\/controlled-files\/\$\{id\}\/training-record/)
  assert.match(workflowSource, /electronicDistributionRecipientUserIds\?:\s*number\[\]/)
  assert.match(approvalActionSource, /electronicDistributionRecipientUserIds\?:\s*number\[\]/)
  assert.doesNotMatch(
    approveContract,
    /trainingRecordFileId/,
    'fourth-node approval API contract must not accept trainingRecordFileId'
  )
  assert.doesNotMatch(
    approvalActionForm,
    /trainingRecordFileId/,
    'fourth-node approval form must not carry trainingRecordFileId'
  )
})

test('detail page exposes applicant training-record step before fourth-node approval', () => {
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')
  const lifecycleSource = readText('src/views/dcc/controlled-file/shared/lifecycle.ts')

  assert.match(detailSource, /canUploadApplicantTrainingRecord/)
  assert.match(detailSource, /uploadControlledFileTrainingRecord/)
  assert.match(detailSource, /上传培训记录/)
  assert.match(detailSource, /fileStatus\.value === 'PENDING_APPLICANT_TRAINING_RECORD'/)
  assert.match(detailSource, /fileDetail\.value\?\.requesterId === currentUserId\.value/)
  assert.match(lifecycleSource, /待申请人上传培训记录/)
  assert.doesNotMatch(
    detailSource,
    /shouldCollectTrainingRecord[\s\S]*Boolean\(fileDetail\.value\?\.needTraining\)/,
    'fourth-node dialog must not collect the applicant training record'
  )
  assert.doesNotMatch(detailSource, /fourthNodeUpload\.trainingRecord/)
  assert.doesNotMatch(detailSource, /submitDccApprovalAction\(\{[\s\S]*trainingRecordFileId:/)
})

test('detail page lets document-control select electronic distribution recipients at the fourth node', () => {
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')

  assert.match(detailSource, /electronicDistributionRecipientUserIds/)
  assert.match(detailSource, /电子发放接收人/)
  assert.match(detailSource, /v-model="fourthNodeUpload\.electronicDistributionRecipientUserIds"/)
  assert.match(detailSource, /submitDccApprovalAction\(\{[\s\S]*electronicDistributionRecipientUserIds:/)
})
