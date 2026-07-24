const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')

function readSource(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function assertContains(source, marker, message) {
  assert.match(source, new RegExp(marker), message)
}

const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const signaturesApi = readSource('src/api/dcc/controlledFile/signatures.ts')
const lifecycle = readSource('src/views/dcc/controlled-file/shared/lifecycle.ts')
const detailPresentation = readSource('src/views/dcc/controlled-file/detail/presentation.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const browserPresentation = readSource('src/views/dcc/controlled-file/browser/presentation.ts')
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const workbenchPresentation = readSource('src/views/dcc/controlled-file/workbench/presentation.ts')
const workbenchPage = readSource('src/views/dcc/controlled-file/workbench/index.vue')
const backendActionProjectionVo = fs.readFileSync(
  path.resolve(
    repoRoot,
    '..',
    'ruoyi-vue-pro/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileActionProjectionRespVO.java'
  ),
  'utf8'
)

assertContains(
  backendActionProjectionVo,
  'private\\s+Boolean\\s+actionLocked;[\\s\\S]*private\\s+String\\s+actionLockReason;[\\s\\S]*private\\s+List<String>\\s+allowedActions;[\\s\\S]*private\\s+Boolean\\s+canWithdraw;[\\s\\S]*private\\s+Long\\s+pendingRequestId;[\\s\\S]*private\\s+String\\s+pendingVersionNo;',
  'Backend DCC actionProjection VO must keep the minimal platform contract fields.'
)
assert.doesNotMatch(
  backendActionProjectionVo,
  /pendingRequestType|withdrawActionCode/,
  'Backend DCC actionProjection VO must not reintroduce non-platform projection fields.'
)

assertContains(
  workflowApi,
  "DCC_CONTROLLED_FILE_ACTIONS[\\s\\S]*'UPLOAD_TRAINING_RECORD'",
  'DCC workflow API action union must include applicant training-record upload.'
)
assertContains(
  workflowApi,
  'interface\\s+DccControlledFileActionProjectionVO[\\s\\S]*actionLocked:\\s*boolean[\\s\\S]*actionLockReason\\?:\\s*string \\| null[\\s\\S]*allowedActions:\\s*DccControlledFileAction\\[\\][\\s\\S]*canWithdraw:\\s*boolean[\\s\\S]*pendingRequestId\\?:\\s*number \\| null[\\s\\S]*pendingVersionNo\\?:\\s*string \\| null',
  'DCC workflow API must type the backend actionProjection contract.'
)
assert.doesNotMatch(
  workflowApi,
  /pendingRequestType|withdrawActionCode/,
  'DCC frontend actionProjection contract must not invent fields that are not required by the backend projection.'
)
assertContains(
  workflowApi,
  'interface\\s+ControlledFileVO[\\s\\S]*actionProjection\\?:\\s*DccControlledFileActionProjectionVO \\| null',
  'ControlledFileVO must expose actionProjection for detail, workbench and browser responses.'
)
assertContains(
  workflowApi,
  'interface\\s+ControlledFileCurrentVersionRespVO[\\s\\S]*actionProjection\\?:\\s*DccControlledFileActionProjectionVO \\| null',
  'Current-version lookup must type actionProjection so upload/revision can fail visible when projection is absent.'
)
assertContains(
  signaturesApi,
  'controlledFileId\\?:\\s*number \\| string',
  'DCC signature evidence queries must accept string Snowflake IDs without browser Number precision loss.'
)

assertContains(
  lifecycle,
  'DCC_ACTION_PROJECTION_MISSING_REASON',
  'lifecycle.ts must centralize the visible missing-projection reason.'
)
assertContains(
  lifecycle,
  'hasDccControlledFileActionProjection',
  'lifecycle.ts must expose a projection presence helper.'
)
assertContains(
  lifecycle,
  'isDccControlledFileActionAllowed[\\s\\S]*allowedActions',
  'lifecycle.ts must check actions from backend allowedActions.'
)
const allowedHelperBody = lifecycle.match(
  /export const isDccControlledFileActionAllowed = \([\s\S]*?\) => \{([\s\S]*?)\n\}/
)
assert.ok(allowedHelperBody, 'lifecycle.ts must declare isDccControlledFileActionAllowed as a helper body.')
assert.doesNotMatch(
  allowedHelperBody[1],
  /status\s*===|PENDING_|FINALIZATION_FAILED|WITHDRAWN|ACTIVE/,
  'lifecycle projection helpers must not re-create a local status-to-action state machine.'
)

assertContains(
  detailPresentation,
  'getDetailActionState[\\s\\S]*isDccControlledFileActionAllowed[\\s\\S]*PREVIEW[\\s\\S]*DOWNLOAD[\\s\\S]*OBSOLETE[\\s\\S]*MANUAL_RELEASE[\\s\\S]*ACKNOWLEDGE_TRAINING[\\s\\S]*RETRY_FINALIZATION',
  'Detail action state must consume backend allowedActions for all ordinary DCC actions.'
)
assert.doesNotMatch(
  detailPresentation,
  /canRetryFinalization:\s*status\s*===\s*['"`]FINALIZATION_FAILED['"`]/,
  'Detail failure retry must not be locally inferred from status text.'
)

assertContains(
  detailPage,
  "actionProjection\\?\\.canWithdraw[\\s\\S]*isDccControlledFileActionAllowed\\(fileDetail\\.value,\\s*'WITHDRAW'\\)",
  'Detail withdraw button must require backend canWithdraw and allowedActions.'
)
assertContains(
  detailPage,
  "canUploadApplicantTrainingRecord[\\s\\S]*isDccControlledFileActionAllowed\\(fileDetail\\.value,\\s*'UPLOAD_TRAINING_RECORD'\\)",
  'Detail applicant training-record upload must require backend allowedActions.'
)
assertContains(
  detailPage,
  'canOpenMetadataDialog[\\s\\S]*hasDccControlledFileActionProjection',
  'Detail metadata entry must become read-only when actionProjection is missing.'
)
assertContains(
  detailPage,
  'resolveDccActionProjectionReadonlyReason',
  'Detail state strip must show backend lock or missing-projection reason.'
)
assertContains(
  detailPage,
  'v-if="dccSignatureEvidenceError"[\\s\\S]*dccSignatureEvidenceError[\\s\\S]*loadDccSignatureEvidenceList[\\s\\S]*resolveReadSideErrorMessage',
  'Detail signature evidence loading failures must be visible without blocking approval task loading.'
)
assertContains(
  detailPage,
  'ensureDetailActionAllowed[\\s\\S]*isDccControlledFileActionAllowed',
  'Detail command handlers must re-check backend projection before invoking ordinary actions.'
)
assertContains(
  detailPage,
  'findCurrentUserTodoTask[\\s\\S]*task\\.assigneeUserId[\\s\\S]*task\\.assignee',
  'Detail approval task matching must support BPM task assignee fields even when assigneeUser is not hydrated.'
)
assert.doesNotMatch(
  detailPage,
  /Number\(controlledFileId\.value\)/,
  'Detail page must not convert Snowflake controlledFileId route params to Number.'
)
assertContains(
  detailPage,
  'watch\\([\\s\\S]*currentUserId[\\s\\S]*approvalTaskList\\.value[\\s\\S]*findCurrentUserTodoTask',
  'Detail approval task matching must recompute when userStore current user arrives after task list loading.'
)

assertContains(
  browserPresentation,
  'getBrowserRowActionState[\\s\\S]*isDccControlledFileActionAllowed[\\s\\S]*PREVIEW[\\s\\S]*DOWNLOAD',
  'Browser row actions must consume actionProjection for preview/download.'
)
assertContains(
  browserPage,
  'hasBrowserMoreActions[\\s\\S]*hasDccControlledFileActionProjection[\\s\\S]*isDccControlledFileActionUnlocked',
  'Browser metadata action must require backend projection instead of only local role/status checks.'
)
assertContains(
  browserPage,
  'getBrowserRowActionBlockReason',
  'Browser operation column must show a visible readonly/block reason when actions are unavailable.'
)

assertContains(
  uploadPage,
  'currentVersionProjectionBlockReason',
  'Upload/revision page must expose a visible actionProjection blocker for current-version lookup.'
)
assertContains(
  uploadPage,
  'hasDccControlledFileActionProjection',
  'Upload/revision page must fail visible when a matched current version lacks actionProjection.'
)

assertContains(
  workbenchPresentation,
  'resolveWorkbenchFilePrimaryAction[\\s\\S]*isDccControlledFileActionAllowed[\\s\\S]*MANUAL_RELEASE[\\s\\S]*RETRY_FINALIZATION[\\s\\S]*toWorkbenchFileRow[\\s\\S]*primaryActionText',
  'Workbench file rows must derive operational labels from backend actionProjection.'
)
assertContains(
  workbenchPage,
  'row\\.primaryActionText',
  'Workbench rows must render action text derived from projection-aware presentation state.'
)

for (const [name, source] of [
  ['detail page', detailPage],
  ['browser page', browserPage],
  ['upload page', uploadPage],
  ['workbench page', workbenchPage]
]) {
  assert.doesNotMatch(source, /\bcatch\s*\{\s*\}/, `${name} must not contain empty catch blocks.`)
  assert.doesNotMatch(source, /\bcatch\s*\{\s*message\./, `${name} must not swallow errors with unbound catch blocks.`)
}

console.log('PASS: dcc controlled file state machine frontend projection contract')
