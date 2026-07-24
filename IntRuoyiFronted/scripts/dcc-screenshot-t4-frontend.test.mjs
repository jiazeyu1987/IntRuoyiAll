import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('dcc workflow api exposes task action and fourth-node contracts', () => {
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')

  for (const field of ['selectedSignoffUserIds', 'stampedPdfFileId', 'trainingRecordFileId']) {
    assert.match(workflowSource, new RegExp(`${field}[?]?:|${field}:`), `workflow contract must expose ${field}`)
  }

  for (const apiName of [
    'returnControlledFileTask',
    'transferControlledFileTask',
    'createControlledFileSignTask',
    'createDistributionRecipientSignTask'
  ]) {
    assert.match(workflowSource, new RegExp(`export const ${apiName}`), `${apiName} must be exported`)
  }

  for (const endpoint of ['return-task', 'transfer-task', 'sign-task']) {
    assert.match(workflowSource, new RegExp(`/dcc/controlled-files/\\$\\{id\\}/${endpoint}`))
  }
  assert.match(workflowSource, /recipients\/\$\{recipientId\}\/sign/)
})

test('dcc upload page lets applicant choose matrix signoff users', () => {
  const uploadSource = readText('src/views/dcc/controlled-file/upload/index.vue')
  const submitterSource = readText('src/views/dcc/controlled-file/upload/submitter.ts')

  assert.match(uploadSource, /UserSelectV2/)
  assert.match(uploadSource, /v-model="formData\.selectedSignoffUserIds"/)
  assert.match(uploadSource, /:multiple="true"|multiple/)
  assert.match(submitterSource, /selectedSignoffUserIds/)
})

test('dcc external review route uses independent external-review workflow page', () => {
  const routeSource = readText('src/router/modules/remaining.ts')
  const pageSource = readText('src/views/dcc/controlled-file/external-review/index.vue')
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')

  assert.match(routeSource, /controlled-file\/external-review/)
  assert.match(routeSource, /external-file-review/)
  assert.match(routeSource, /@\/views\/dcc\/controlled-file\/external-review\/index\.vue/)
  assert.match(routeSource, /path:\s*'manager\/model'[\s\S]*@\/views\/bpm\/model\/index\.vue/)
  for (const label of ['外来来源', '外来归属', '评审原因', '参与人']) {
    assert.match(pageSource, new RegExp(label), `external review page must render ${label}`)
  }
  assert.match(pageSource, /submitExternalFileReview/)
  assert.match(pageSource, /participantUserIds/)
  assert.match(workflowSource, /\/dcc\/external-file-reviews\/submit/)
  assert.match(workflowSource, /EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY/)
  assert.doesNotMatch(pageSource, /submitControlledFile\(/)
})

test('dcc detail page uses dcc task actions and collects fourth-node files', () => {
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')
  const approvalActionSource = readText('src/views/dcc/controlled-file/detail/approval-actions.ts')
  const approvalActionForm = approvalActionSource.match(/export interface DccApprovalActionForm \{[\s\S]*?\n\}/)?.[0] ?? ''

  for (const apiName of [
    'returnControlledFileTask',
    'transferControlledFileTask',
    'createControlledFileSignTask',
    'createDistributionRecipientSignTask'
  ]) {
    assert.match(detailSource, new RegExp(apiName), `detail page must use ${apiName}`)
  }

  assert.match(detailSource, /uploadControlledFilePreview/)
  assert.match(detailSource, /stampedPdfFileId/)
  assert.match(detailSource, /trainingRecordFileId/)
  assert.match(detailSource, /PENDING_DOC_CONTROL_APPROVAL/)
  assert.match(detailSource, /accept="\.pdf,application\/pdf"/)
  assert.match(detailSource, /盖章 PDF/)
  assert.match(detailSource, /培训记录/)
  assert.match(detailSource, /PENDING_APPLICANT_TRAINING_RECORD/)
  assert.match(detailSource, /const openTaskActionDialog\s*=\s*\(mode: DccTaskActionMode\)/)
  assert.match(detailSource, /const submitTaskActionDialog\s*=\s*async\s*\(\)/)
  assert.match(detailSource, /findCurrentUserTodoTask/)
  assert.match(detailSource, /处理回退/)
  assert.match(detailSource, /有流程回退，需处理/)
  assert.match(detailSource, /接收人加签/)
  assert.match(detailSource, /distributionSignDialog/)
  assert.match(detailSource, /流程打印/)
  assert.match(detailSource, /流程导出 Word/)
  assert.match(detailSource, /getProcessInstancePrintData/)
  assert.match(detailSource, /application\/msword/)
  assert.match(detailSource, /v-if="fileDetail"[\s\S]*流程打印/)
  assert.match(detailSource, /if \(!processInstanceId\)[\s\S]*return null/)
  assert.match(detailSource, /const submitReturnTask[\s\S]*returnControlledFileTask[\s\S]*await submitReturnTask\(/)
  assert.match(detailSource, /const submitTransferTask[\s\S]*transferControlledFileTask[\s\S]*await submitTransferTask\(/)
  assert.match(detailSource, /const submitSignTask[\s\S]*createControlledFileSignTask[\s\S]*await submitSignTask\(/)
  assert.match(detailSource, /await createDistributionRecipientSignTask\(/)
  assert.match(detailSource, /submitDccApprovalAction\(\{[\s\S]*stampedPdfFileId:/)
  assert.doesNotMatch(detailSource, /submitDccApprovalAction\(\{[\s\S]*trainingRecordFileId:/)
  assert.match(approvalActionSource, /stampedPdfFileId/)
  assert.doesNotMatch(approvalActionForm, /trainingRecordFileId/)

  assert.doesNotMatch(detailSource, /TaskApi\.(returnTask|transferTask|signCreateTask)/)
})

test('bpm model page surfaces Word print template capability for process export checks', () => {
  const bpmModelSource = readText('src/views/bpm/model/index.vue')
  const extraSettingsSource = readText('src/views/bpm/model/form/ExtraSettings.vue')

  assert.match(bpmModelSource, /Word 打印模板/)
  assert.match(extraSettingsSource, /自定义打印模板/)
})

test('bpm generic operation buttons cannot bypass dcc controlled-file task endpoints', () => {
  const bpmSource = readText('src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue')

  assert.match(bpmSource, /CONTROLLED_FILE_PROCESS_DEFINITION_KEY/)
  assert.match(bpmSource, /DCC受控文件审批请返回文控中心/)

  for (const operation of ['COPY', 'TRANSFER', 'DELEGATE', 'ADD_SIGN', 'RETURN']) {
    const marker = `OperationButtonType.${operation})`
    const index = bpmSource.indexOf(marker)
    assert.notEqual(index, -1, `missing ${marker}`)
    const nearbyTemplate = bpmSource.slice(Math.max(0, index - 260), index + 160)
    assert.match(
      nearbyTemplate,
      /!isDccControlledFileProcess/,
      `${operation} button must be hidden for DCC controlled-file processes`
    )
  }

  assert.match(bpmSource, /!isDccControlledFileProcess && runningTask\?\.children\.length > 0/)
  assert.match(bpmSource, /!isDccControlledFileProcess[\s\S]*processDefinition\?\.formType === 10/)
})

test('user profile route keeps real reset-password page when backend menu has duplicate shell child', () => {
  const remainingSource = readText('src/router/modules/remaining.ts')
  const permissionSource = readText('src/store/modules/permission.ts')
  const profileSource = readText('src/views/Profile/Index.vue')

  assert.match(
    remainingSource,
    /path:\s*'\/user'[\s\S]*path:\s*'profile'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/Profile\/Index\.vue'\)/,
    '/user/profile must point at the real local Profile page'
  )
  assert.match(profileSource, /<el-tab-pane[\s\S]*:label="t\('profile\.info\.resetPwd'\)"[\s\S]*name="resetPwd"/)
  assert.match(profileSource, /<ResetPwd\s*\/>/)

  const mergeFunctionStart = permissionSource.indexOf('const mergeHiddenStaticShellRoute =')
  const mergeFunctionEnd = permissionSource.indexOf(
    'const mergeStaticRoutesWithDynamicRoutes',
    mergeFunctionStart
  )
  assert.notEqual(mergeFunctionStart, -1, 'hidden shell route merge function must exist')
  assert.notEqual(mergeFunctionEnd, -1, 'hidden shell route merge function must be inspectable')

  const mergeFunctionSource = permissionSource.slice(mergeFunctionStart, mergeFunctionEnd)
  assert.match(
    mergeFunctionSource,
    /mergeHiddenStaticChildWithDynamicChild/,
    'duplicate backend children must be merged with the static hidden child'
  )
  assert.match(
    permissionSource,
    /component:\s*staticChild\.component/,
    'static hidden child component must win so /user/profile renders Profile/Index.vue'
  )
  assert.doesNotMatch(
    mergeFunctionSource,
    /hiddenStaticChildren[\s\S]*\.filter\(\(child\)[\s\S]*!dynamicChildNames\.has[\s\S]*!dynamicChildPaths\.has/,
    'static profile child must not be dropped merely because the backend menu has the same path or name'
  )
})
