const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const workflowApiPath = path.join(root, 'src/api/dcc/controlledFile/workflow.ts')
const directoriesApiPath = path.join(root, 'src/api/dcc/controlledFile/directories.ts')
const nasPagePath = path.join(root, 'src/views/system/nas/index.vue')
const restorePanelPath = path.join(
  root,
  'src/views/system/nas/components/NasPermissionRestorePanel.vue'
)
const accessSubjectOptionsPath = path.join(root, 'src/views/dcc/controlled-file/shared/options.ts')
const realDataE2ePath = path.join(root, 'tests/e2e/dcc-nas-permission-real-data.e2e.js')

const workflowApiSource = fs.readFileSync(workflowApiPath, 'utf8')
const directoriesApiSource = fs.readFileSync(directoriesApiPath, 'utf8')
const nasPageSource = fs.readFileSync(nasPagePath, 'utf8')
const accessSubjectOptionsSource = fs.readFileSync(accessSubjectOptionsPath, 'utf8')
const restorePanelSource = fs.existsSync(restorePanelPath)
  ? fs.readFileSync(restorePanelPath, 'utf8')
  : ''
const realDataE2eSource = fs.existsSync(realDataE2ePath)
  ? fs.readFileSync(realDataE2ePath, 'utf8')
  : ''
const nasUiSource = `${nasPageSource}\n${restorePanelSource}`

const requiredApiFragments = [
  'NasPermissionSnapshotSummaryVO',
  'NasPermissionSnapshotItemVO',
  'NasUnmappedPrincipalVO',
  'NasPrincipalMappingSaveReqVO',
  'NasPermissionRestorePreviewVO',
  'NasPermissionRestoreApplyReqVO',
  'NasPermissionRestoreStatusVO',
  'getNasPermissionSnapshotSummary',
  'getNasPermissionSnapshotItems',
  'getNasUnmappedPrincipals',
  'saveNasPrincipalMapping',
  'previewNasPermissionRestore',
  'applyNasPermissionRestore',
  'getNasPermissionRestoreStatus',
  '/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-snapshot',
  '/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-snapshot/items',
  '/dcc/nas-permission/principals/unmapped',
  '/dcc/nas-permission/principal-mappings',
  '/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore/preview',
  '/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore',
  '/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore/${restoreId}'
]

for (const fragment of requiredApiFragments) {
  if (!workflowApiSource.includes(fragment)) {
    throw new Error(`workflow.ts must expose NAS permission restore contract fragment: ${fragment}`)
  }
}

const requiredPageFragments = [
  '权限快照',
  '身份映射',
  '恢复预览',
  '应用恢复',
  '快照状态',
  '未映射主体',
  'restoreSupported',
  'runtimeEnforcementReady',
  'handleOpenPermissionRestore',
  'loadPermissionSnapshotSummary',
  'loadPermissionSnapshotItems',
  'loadUnmappedPrincipals',
  'handleSavePrincipalMapping',
  'handlePreviewPermissionRestore',
  'handleApplyPermissionRestore',
  'pollPermissionRestoreStatus',
  'assertRestoreBlockerContract',
  'assertRestoreApplyContract',
  'assertRestoreStatusContract'
]

for (const fragment of requiredPageFragments) {
  if (!nasUiSource.includes(fragment)) {
    throw new Error(`NAS management page must expose permission restore UI fragment: ${fragment}`)
  }
}

if (!nasPageSource.includes('NasPermissionRestorePanel')) {
  throw new Error('NAS management page must mount NasPermissionRestorePanel in the transfer result area')
}

if (!nasUiSource.includes('targetSubjectType') || !nasUiSource.includes('targetSubjectId')) {
  throw new Error('NAS principal mapping UI must require explicit DCC subject type and subject id')
}

if (!nasUiSource.includes('planHash') || !nasUiSource.includes('idempotencyKey')) {
  throw new Error('NAS restore apply UI must submit backend planHash and idempotencyKey')
}

if (restorePanelSource.includes('crypto.randomUUID()')) {
  throw new Error('NAS restore apply UI must use project UUID utility instead of direct crypto.randomUUID')
}

if (!restorePanelSource.includes('generateUUID')) {
  throw new Error('NAS restore apply UI must generate restore idempotency keys with generateUUID')
}

if (!restorePanelSource.includes("status === 'NOT_COLLECTED'")) {
  throw new Error('NAS permission restore UI must render NOT_COLLECTED snapshot status without showing an API error')
}

if (!restorePanelSource.includes('isSnapshotReadyForDetail')) {
  throw new Error('NAS permission restore UI must gate detail/restore calls until a real snapshot exists')
}

if (
  restorePanelSource.includes(`await Promise.all([
    loadPermissionSnapshotSummary(),`)
) {
  throw new Error('NAS permission restore drawer must load snapshot summary before detail APIs')
}

if (!restorePanelSource.includes('const summary = await loadPermissionSnapshotSummary()')) {
  throw new Error('NAS permission restore refresh must use the loaded summary as the detail API gate')
}

if (!directoriesApiSource.includes('subjectType: string')) {
  throw new Error('DCC directory access-rule API must use string subjectType values')
}

const accessSubjectOptionsBlock = accessSubjectOptionsSource.match(
  /export const ACCESS_SUBJECT_TYPE_OPTIONS = \[[\s\S]*?\n\]/
)?.[0]
if (!accessSubjectOptionsBlock) {
  throw new Error('DCC access-rule subject type selector options must be declared')
}

for (const requiredSubjectType of ["value: 'USER'", "value: 'DEPT'", "value: 'ROLE'", "value: 'POSITION'"]) {
  if (!accessSubjectOptionsBlock.includes(requiredSubjectType)) {
    throw new Error(`DCC access-rule subject type selector must expose ${requiredSubjectType}`)
  }
}

for (const forbiddenSubjectType of ['value: 1', 'value: 2', 'value: 3', 'value: 4']) {
  if (accessSubjectOptionsBlock.includes(forbiddenSubjectType)) {
    throw new Error(`DCC access-rule subject type selector must not use numeric option ${forbiddenSubjectType}`)
  }
}

if (nasUiSource.includes('mock') || nasUiSource.includes('Mock')) {
  throw new Error('NAS permission restore UI must not contain mock success branches')
}

for (const forbiddenFallback of ['page.list || []', 'page.total || 0', 'result.list || []', 'sampleRules || []']) {
  if (nasUiSource.includes(forbiddenFallback)) {
    throw new Error(`NAS permission restore UI must fail fast instead of using fallback: ${forbiddenFallback}`)
  }
}

const requiredRealDataE2eFragments = [
  "'test-mapping'",
  "'test-blocker'",
  'NAS_PERMISSION_E2E_ALLOW_DB_FIXTURE',
  'NAS_PERMISSION_E2E_FIXTURE_MYSQL_PASSWORD',
  'applyMappingFixture',
  'applyBlockerFixture',
  'INSERT INTO dcc_nas_acl_descriptor',
  'UPDATE dcc_nas_acl_directory_snapshot ds',
  'assertApplyRestoreDisabled',
  'savedMappings=',
  'blockers='
]

for (const fragment of requiredRealDataE2eFragments) {
  if (!realDataE2eSource.includes(fragment)) {
    throw new Error(`NAS real-data E2E must cover mapping/blocker branch fragment: ${fragment}`)
  }
}

if (realDataE2eSource.includes('UPDATE dcc_nas_acl_ace a')) {
  throw new Error('NAS blocker E2E fixture must not mutate shared ACE rows directly')
}

if (realDataE2eSource.includes('UPDATE dcc_nas_acl_identity_mapping m')) {
  throw new Error('NAS mapping E2E fixture must not mutate shared identity mapping rows directly')
}

console.log('PASS: NAS permission restore frontend source wiring is present')
