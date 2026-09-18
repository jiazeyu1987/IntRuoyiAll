const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const repoRoot = path.resolve(moduleRoot, '..', '..')
const readBackend = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')
const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, 'IntRuoyiFronted', relativePath), 'utf8')

function methodBody(source, signature) {
  const start = source.indexOf(signature)
  assert.notEqual(start, -1, `Missing method ${signature}`)
  const bodyStart = source.indexOf('{', start)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    if (source[i] === '{') depth += 1
    if (source[i] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(bodyStart + 1, i)
    }
  }
  throw new Error(`Unterminated method ${signature}`)
}

const queryService = readBackend(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java'
)
const workflowService = readBackend(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java'
)
const detailPage = readFrontend('src/views/dcc/controlled-file/detail/index.vue')

const editableStateGuard = methodBody(queryService, 'private boolean isEditableWorkingVersion')
assert.match(
  editableStateGuard,
  /DccControlledFileStatusEnum\.PENDING_APPLICANT_REWORK\.getStatus\(\)\.equals\(status\)/,
  'PENDING_APPLICANT_REWORK must be accepted by the same controlled checkout/checkin edit gate as rejected versions'
)

const checkinCopy = methodBody(queryService, 'private DccControlledFileDO copyForCheckin')
assert.match(
  checkinCopy,
  /\.predecessorControlledFileId\(file\.getId\(\)\)[\s\S]*\.sourceSha256\(preparedSource\.sourceSha256\(\)\)[\s\S]*\.previousSourceSha256\(previousHash\)[\s\S]*\.status\("WORKING"\)/,
  'returned applicant content changes must create an auditable successor WORKING iteration with source hash lineage'
)

const submitWorkingIteration = methodBody(workflowService, 'public Long submitWorkingIteration')
assert.match(
  submitWorkingIteration,
  /DccControlledFileDO applicantReworkPredecessor = validateWorkingIterationSubmission\(userId,\s*tenantId,\s*file,\s*master\);/,
  'working iteration submission must carry the replaceable returned-applicant predecessor through the submit flow'
)
assert.match(
  submitWorkingIteration,
  /String processInstanceId = createApprovalProcess\(userId,\s*file,\s*resolvedRoute,\s*BPM_PROCESS_DEFINITION_KEY\);[\s\S]*file\.setProcessInstanceId\(processInstanceId\);[\s\S]*closeApplicantReworkPredecessorForResubmission\(userId,\s*applicantReworkPredecessor,\s*file,\s*processInstanceId\);[\s\S]*platformAdapter\.recordSubmitted\(file,\s*userId,\s*processInstanceId\);[\s\S]*platformAdapter\.recordResubmitted\(applicantReworkPredecessor,\s*file\.getId\(\)\);/,
  'after corrected A/2 starts its new approval, the old returned A/1 DCC/BPM/platform candidate must be closed and linked before completion'
)

const validateWorkingIterationSubmission = methodBody(workflowService, 'private DccControlledFileDO validateWorkingIterationSubmission')
assert.match(
  validateWorkingIterationSubmission,
  /resolveApplicantReworkPredecessorForResubmission\(file,\s*masterVersions\)/,
  'submission validation must explicitly identify the unique returned-applicant predecessor'
)
assert.match(
  validateWorkingIterationSubmission,
  /\.filter\(item -> applicantReworkPredecessor == null\s*\|\|\s*!Objects\.equals\(item\.getId\(\),\s*applicantReworkPredecessor\.getId\(\)\)\)[\s\S]*isUnfinishedWorkflowVersion\(item\)/,
  'unfinished-workflow guard may ignore only the predecessor being superseded by the corrected working iteration'
)

const reworkPredecessorResolver = methodBody(workflowService, 'private DccControlledFileDO resolveApplicantReworkPredecessorForResubmission')
assert.match(
  reworkPredecessorResolver,
  /Map<Long,\s*DccControlledFileDO>[\s\S]*new LinkedHashSet<>\(\)[\s\S]*while \(candidate != null\)[\s\S]*candidate = versionsById\.get\(candidate\.getPredecessorControlledFileId\(\)\)/,
  'returned-applicant predecessor resolution must walk the legal predecessor chain, not only the direct previous iteration'
)
assert.match(
  reworkPredecessorResolver,
  /DccControlledFileStatusEnum\.WORKING\.getStatus\(\)\.equals\(candidate\.getStatus\(\)\)[\s\S]*isSameWorkingCorrectionInReworkChain\(file,\s*candidate\)[\s\S]*return null/,
  'each repeated check-in hop must be verified as same master/requester/revision and forward version lineage'
)
const reworkPredecessorMatcher = methodBody(workflowService, 'private boolean isApplicantReworkPredecessorForResubmission')
assert.match(
  reworkPredecessorMatcher,
  /DccControlledFileStatusEnum\.PENDING_APPLICANT_REWORK\.getStatus\(\)\.equals\(predecessor\.getStatus\(\)\)[\s\S]*StrUtil\.isBlank\(predecessor\.getProcessInstanceId\(\)\)[\s\S]*Objects\.equals\(file\.getRequesterId\(\),\s*predecessor\.getRequesterId\(\)\)/,
  'replaceable predecessor must be the same requester, same master, pending applicant rework version with a live process identity'
)
assert.match(
  reworkPredecessorMatcher,
  /Objects\.equals\(fileVersion\.revisionCode\(\),\s*predecessorVersion\.revisionCode\(\)\)[\s\S]*fileVersion\.iterationNo\(\) > predecessorVersion\.iterationNo\(\)/,
  'corrected resubmission must advance the same revision iteration instead of bypassing another open candidate'
)

const closeReworkPredecessor = methodBody(workflowService, 'private void closeApplicantReworkPredecessorForResubmission')
assert.match(
  closeReworkPredecessor,
  /bpmProcessInstanceService\.cancelProcessInstanceByStartUser\(userId,[\s\S]*new BpmProcessInstanceCancelReqVO\(\)\.setId\(predecessor\.getProcessInstanceId\(\)\)\.setReason\(reason\)\)/,
  'old returned-applicant BPM process must be explicitly cancelled when corrected content is resubmitted'
)
assert.match(
  closeReworkPredecessor,
  /\.status\(DccControlledFileStatusEnum\.WITHDRAWN\.getStatus\(\)\)[\s\S]*\.rejectReason\(reason\)/,
  'old returned-applicant file row must move to a terminal status with the resubmission reason recorded'
)
assert.match(
  closeReworkPredecessor,
  /platformAdapter\.recordWithdrawn\(predecessor,\s*userId,\s*reason\)/,
  'old returned-applicant unified controlled-content candidate must be withdrawn before the corrected candidate is submitted'
)
assert.match(
  closeReworkPredecessor,
  /predecessor\.setStatus\(DccControlledFileStatusEnum\.WITHDRAWN\.getStatus\(\)\)[\s\S]*platformAdapter\.recordWithdrawn\(predecessor,\s*userId,\s*reason\)/,
  'old returned-applicant platform candidate ref must be withdrawn before the corrected version creates a new platform candidate'
)

assert.doesNotMatch(
  detailPage,
  /处理后将继续提交原流程/,
  'returned applicant guidance must not claim a signature-only action continues the original bad-content flow'
)
assert.doesNotMatch(
  detailPage,
  /<el-button type="primary" @click="openActionDialog\('approve'\)">/,
  'returned applicant task panels must not expose an unconditional signature-only approve button'
)
assert.match(
  detailPage,
  /data-testid="dcc-returned-applicant-rework-guide"/,
  'detail page must show an explicit applicant rework guide'
)
assert.doesNotMatch(
  detailPage,
  /isReturnedApplicantTask[\s\S]{0,400}rejectReason/,
  'returned applicant task detection must be driven by formal task/status/requester identity, not reject-reason text'
)
assert.match(
  detailPage,
  /openReturnedApplicantReworkInBrowser/,
  'detail page must provide a navigation action to the controlled browser checkout/checkin path'
)
assert.match(
  detailPage,
  /检出[\s\S]{0,120}检入[\s\S]{0,120}新工作版本/,
  'returned applicant copy must direct the requester to checkout/checkin a new working version before resubmission'
)

console.log('DCC-STATIC-010 applicant rework content-change contract PASS')
