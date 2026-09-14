const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const page = fs
  .readFileSync(
    path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')
const realE2e = fs
  .readFileSync(
    path.join(frontendRoot, 'tests/e2e/team-leader-loss-maintenance-dialog-real.e2e.js'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

const extractBetween = (source, startMarker, endMarker) => {
  const start = source.indexOf(startMarker)
  assert.notStrictEqual(start, -1, `Missing start marker: ${startMarker}`)
  const end = source.indexOf(endMarker, start)
  assert.notStrictEqual(end, -1, `Missing end marker: ${endMarker}`)
  return source.slice(start, end)
}

const extractDialog = (source, marker) => {
  const markerIndex = source.indexOf(marker)
  assert.notStrictEqual(markerIndex, -1, `Missing dialog marker: ${marker}`)
  const start = source.lastIndexOf('<el-dialog', markerIndex)
  assert.notStrictEqual(start, -1, `Missing dialog start before: ${marker}`)
  const end = source.indexOf('\n    <el-dialog', markerIndex)
  assert.notStrictEqual(end, -1, `Missing next dialog after: ${marker}`)
  return source.slice(start, end)
}

const operationPanel = extractBetween(
  page,
  '<el-table-column label="操作面板"',
  '      </el-table>'
)

assert.match(
  operationPanel,
  /data-team-leader-process-config-manage-loss[\s\S]{0,180}@click="openLossReasonMaintenanceDialog\(row\)"[\s\S]{0,80}>\s*损耗\s*<\/el-button>/,
  'Each process row must expose one formal loss maintenance button.'
)
assert.equal(
  (operationPanel.match(/data-team-leader-process-config-manage-loss/g) || []).length,
  1,
  'The operation template must contain exactly one loss maintenance button.'
)
for (const removedLabel of ['新增损耗', '修改损耗', '删除损耗']) {
  assert.doesNotMatch(
    operationPanel,
    new RegExp(`>\\s*${removedLabel}\\s*<\\/el-button>`),
    `The operation panel must not keep the old ${removedLabel} button.`
  )
}

const dialog = extractDialog(page, 'data-loss-reason-maintenance-dialog')
assert.match(
  dialog,
  /v-model="lossReasonMaintenanceDialogVisible"[\s\S]*width="min\(760px, calc\(100vw - 32px\)\)"/,
  'Loss maintenance must use one responsive dialog.'
)
assert.match(dialog, /:close-on-click-modal="!lossReasonSubmitting"/)
assert.match(dialog, /:close-on-press-escape="!lossReasonSubmitting"/)
assert.match(dialog, /:show-close="!lossReasonSubmitting"/)
assert.match(dialog, /data-loss-reason-maintenance-table/, 'The dialog must expose a stable list marker.')
assert.match(
  dialog,
  /:data="lossReasonMaintenanceReasons"/,
  'The dialog must render the selected route process formal loss list.'
)
assert.match(
  dialog,
  /empty-text="当前工序暂无损耗原因"/,
  'The dialog must expose the current process empty state.'
)
assert.match(
  dialog,
  /isLossReasonEditing\(row\)[\s\S]*v-model="lossReasonForm\.reasonName"[\s\S]*v-model="lossReasonForm\.remark"/,
  'Edit mode must expose name and maintenance remark inline.'
)
assert.match(
  dialog,
  /isLossReasonEditing\(row\)[\s\S]*v-model="lossReasonForm\.enabled"/,
  'Edit mode must expose the enabled state inline.'
)
assert.match(dialog, /<span v-else>\{\{ row\.reasonName }}<\/span>/, 'Readonly rows must show reasonName.')
assert.doesNotMatch(dialog, /row\.reasonCode/, 'The maintenance list must not display internal reasonCode.')
assert.match(
  dialog,
  /data-loss-reason-inline-edit[\s\S]{0,180}@click="startEditLossReason\(row\)"/,
  'Each formal loss row must expose inline edit.'
)
assert.match(
  dialog,
  /data-loss-reason-inline-delete[\s\S]{0,180}@click="handleDeleteLossReason\(row\)"/,
  'Each formal loss row must expose confirmed delete.'
)
assert.match(
  dialog,
  /data-loss-reason-inline-create-row[\s\S]*data-loss-reason-inline-save-create[\s\S]*@click="submitLossReason"/,
  'Create mode must render a new editable row at the end of the list.'
)
assert.match(
  dialog,
  /data-loss-reason-inline-add[\s\S]{0,180}@click="startCreateLossReason"[\s\S]{0,180}>[\s\S]{0,100}新增\s*<\/el-button>/,
  'The dialog must expose one create action below the list.'
)
assert(
  dialog.indexOf('data-loss-reason-maintenance-table') < dialog.indexOf('data-loss-reason-inline-add'),
  'The create action must appear after the current loss list.'
)
const createToolbar = extractBetween(
  dialog,
  'class="team-leader-workbench__loss-maintenance-toolbar"',
  '      <template #footer>'
)
assert.doesNotMatch(
  createToolbar,
  /v-if=/,
  'The bottom create action must remain available for both empty and existing lists.'
)
assert.equal(
  (dialog.match(/:disabled="lossReasonEditorActive \|\| lossReasonSubmitting"/g) || []).length,
  3,
  'Edit, delete, and add must all be disabled while another editor or write is active.'
)
assert.doesNotMatch(
  dialog,
  /data-loss-reason-edit-dialog/,
  'The legacy nested create/edit dialog must be removed.'
)

const createRow = extractBetween(
  dialog,
  'data-loss-reason-inline-create-row',
  '        </template>'
)
assert.match(createRow, /v-model="lossReasonForm\.reasonName"/, 'Create must only collect a reason name.')
assert.doesNotMatch(
  createRow,
  /v-model="lossReasonForm\.(?:reasonCode|enabled|remark)"/,
  'Create must not collect a manual code, enabled state, or maintenance remark.'
)

assert.match(
  page,
  /const\s+lossReasonMaintenanceRow\s*=\s*computed\([\s\S]*row\.routeProcessId\s*===\s*lossReasonMaintenanceRouteProcessId\.value/,
  'The dialog must recompute its row by routeProcessId after formal list reloads.'
)
assert.match(
  page,
  /const\s+lossReasonEditorActive\s*=\s*computed\([\s\S]*lossReasonDialogMode\.value\s*!==\s*'idle'/,
  'Only one create or edit operation may be active.'
)

const cancelLossReasonEditor = extractBetween(
  page,
  'const cancelLossReasonEditor = () => {',
  'const resetLossReasonMaintenance'
)
assert.match(cancelLossReasonEditor, /lossReasonDialogMode\.value\s*=\s*'idle'/)
assert.match(cancelLossReasonEditor, /lossReasonEditingReasonId\.value\s*=\s*undefined/)
assert.match(cancelLossReasonEditor, /resetLossReasonForm\(\)/)

const resetLossReasonMaintenance = extractBetween(
  page,
  'const resetLossReasonMaintenance = () => {',
  'const openLossReasonMaintenanceDialog'
)
assert.match(dialog, /@closed="resetLossReasonMaintenance"/)
assert.match(resetLossReasonMaintenance, /cancelLossReasonEditor\(\)/)
assert.match(resetLossReasonMaintenance, /lossReasonMaintenanceRouteProcessId\.value\s*=\s*undefined/)

const submitLossReason = extractBetween(
  page,
  'const submitLossReason = async () => {',
  'const handleDeleteLossReason = async'
)
assert.match(
  submitLossReason,
  /await createTeamLeaderLossReason\(\{[\s\S]*routeProcessId:\s*row\.routeProcessId,[\s\S]*reasonName[\s\S]*}\)/,
  'Inline create must use the formal routeProcessId + reasonName payload.'
)
assert.match(
  submitLossReason,
  /await updateTeamLeaderLossReason\([\s\S]*reasonName,[\s\S]*enabled:\s*lossReasonForm\.enabled,[\s\S]*remark:/,
  'Inline edit must use the formal update payload.'
)
assert.match(
  submitLossReason,
  /cancelLossReasonEditor\(\)[\s\S]*try\s*{[\s\S]*await loadProcessConfigRows\(\)/,
  'A successful write must clear the editor before reloading formal rows to prevent duplicate create retries.'
)
assert.match(
  submitLossReason,
  /损耗原因已保存，但列表刷新失败/,
  'A failed post-write refresh must be reported separately from a failed write.'
)
assert.doesNotMatch(
  submitLossReason,
  /lossReasonMaintenanceDialogVisible\.value\s*=\s*false/,
  'A successful save must keep the maintenance dialog open.'
)
const submitCatch = submitLossReason.slice(
  submitLossReason.indexOf('} catch (error) {'),
  submitLossReason.indexOf('} finally {')
)
assert.doesNotMatch(
  submitCatch,
  /cancelLossReasonEditor\(\)/,
  'A failed save must preserve the active draft for correction.'
)

const deleteLossReason = extractBetween(
  page,
  'const handleDeleteLossReason = async',
  'const resetProcessConfigParameterForm'
)
assert.match(
  deleteLossReason,
  /await deleteTeamLeaderLossReason\(reason\.id\)[\s\S]*await loadProcessConfigRows\(\)[\s\S]*ElMessage\.success\('损耗原因已删除'\)/,
  'Confirmed delete must use the formal endpoint and reload formal rows.'
)
assert.match(deleteLossReason, /损耗原因已删除，但列表刷新失败/)

const processCreateDialog = extractDialog(page, 'data-team-leader-process-config-create-dialog')
assert.doesNotMatch(
  processCreateDialog,
  /LOSS_REASON|损耗原因/,
  'The top generic create dialog must no longer expose loss creation.'
)
assert.doesNotMatch(
  page,
  /type ProcessConfigCreateType\s*=\s*[^\n]*LOSS_REASON/,
  'The top generic create type must no longer include loss reasons.'
)
assert.match(
  page,
  /type ProcessConfigCreateType\s*=\s*'DEVICE_BINDING' \| 'PARAMETER_RULE'/,
  'The generic create type must contain only device binding and parameter rules.'
)
assert.match(
  page,
  /createType:\s*'DEVICE_BINDING'\s+as ProcessConfigCreateType/,
  'The generic create dialog must default to device binding.'
)
assert.doesNotMatch(
  page,
  /processConfigCreateForm\.createType\s*===\s*'LOSS_REASON'/,
  'The generic create confirmation must not route to loss creation.'
)

assert(
  realE2e.indexOf('const permissionResponse = captureOutcome')
    < realE2e.indexOf("form.getByRole('button', { name: '登录' }).click()"),
  'Real E2E must register the permission response listener before clicking login.'
)
assert.match(realE2e, /function captureOutcome\(promise\)/)
assert.match(realE2e, /async function requireOutcome\(outcomePromise\)/)
assert.doesNotMatch(
  realE2e,
  /const\s+\w+(?:Promise|Response)\s*=\s*waitForBusinessResponse\(/,
  'Every pre-trigger business response waiter must attach a rejection outcome handler immediately.'
)
assert.match(
  realE2e,
  /filter\(\{ hasText: config\.processText }\)[\s\S]*data-route-process-id/,
  'Real E2E must locate the target row by visible business text and formal routeProcessId.'
)
assert.match(
  realE2e,
  /data-loss-reason-inline-enabled[\s\S]*enabledSwitch\.click\(\)[\s\S]*payload\.enabled, false/,
  'Real E2E must modify the enabled state through the visible inline switch.'
)
assert.match(
  realE2e,
  /async function cleanupTaskLossReason[\s\S]*dataState\.created && !dataState\.disabled[\s\S]*cleanupTaskLossReason/,
  'Real E2E failure handling must attempt UI cleanup for enabled task-owned data.'
)

console.log('PASS: team leader loss maintenance uses one row-scoped inline dialog')
