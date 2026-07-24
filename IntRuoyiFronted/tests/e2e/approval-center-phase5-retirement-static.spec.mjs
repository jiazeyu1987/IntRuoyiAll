import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(process.cwd())
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')
const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const approvalCenter = read('src/views/approval-center/index.vue')
const profileWorkbench = read('src/views/Profile/components/ProfileWorkbench.vue')
const dccWorkbenchPresentation = read('src/views/dcc/controlled-file/workbench/presentation.ts')
const dccWorkbench = read('src/views/dcc/controlled-file/workbench/index.vue')
const showroomRoutes = read('src/router/modules/showroom.ts')
const remainingRoutes = read('src/router/modules/remaining.ts')
const dccApprovalTasks = read('src/views/dcc/controlled-file/approval-tasks/index.vue')
const edhrApprovalPage = read('src/views/mes/pro/edhr/ApprovalPage.vue')

assert.match(approvalCenter, /useRoute\(\)/, 'approval center must read route query filters')
assert.match(approvalCenter, /route\.query/, 'approval center must apply query filters from links')
assert.match(approvalCenter, /viewType/, 'approval center query support must include viewType')
assert.match(approvalCenter, /moduleCode/, 'approval center query support must include moduleCode')
assert.match(approvalCenter, /buildApprovalCenterPath/, 'approval center must normalize legacy query links to child paths')
assert.match(approvalCenter, /\/approval-center\/todo/, 'approval center must canonicalize TODO route to child path')

for (const legacyPath of [
  '/bpm/task/todo',
  '/bpm/task/done',
  '/bpm/process-instance/my',
  '/dcc/controlled-file/approval-tasks'
]) {
  assert.doesNotMatch(
    profileWorkbench,
    new RegExp(escapeRegExp(legacyPath)),
    `ProfileWorkbench must not link to legacy approval entry ${legacyPath}`
  )
}
for (const profileApprovalToken of [
  'openApprovalCenter',
  'getTaskTodoPage',
  'getTaskDonePage',
  'getProcessInstanceMyPage',
  'BPM 审批中心',
  '审批任务',
  '待办审批',
  '已办审批',
  '我的申请'
]) {
  assert.doesNotMatch(
    profileWorkbench,
    new RegExp(escapeRegExp(profileApprovalToken)),
    `ProfileWorkbench must not expose approval-center aggregation in profile: ${profileApprovalToken}`
  )
}
assert.doesNotMatch(
  profileWorkbench,
  /\/approval-center|moduleCode|viewType/,
  'ProfileWorkbench must not navigate to approval center; approvals have their own center.'
)

assert.doesNotMatch(
  `${dccWorkbenchPresentation}\n${dccWorkbench}`,
  /\/dcc\/controlled-file\/approval-tasks/,
  'DCC workbench must not expose the legacy approval-tasks center as a formal entry'
)
assert.match(
  `${dccWorkbenchPresentation}\n${dccWorkbench}`,
  /\/approval-center\?moduleCode=DCC&viewType=TODO|\/approval-center\/todo\?moduleCode=DCC/,
  'DCC workbench approval todo entry must point at unified approval center'
)

assert.doesNotMatch(
  remainingRoutes,
  /path:\s*'process-instance\/my'[\s\S]{0,260}component:\s*\(\)\s*=>\s*import\('@\/views\/bpm\/processInstance\/index\.vue'\)/,
  'BPM my process legacy list route must not mount the old list component'
)
assert.doesNotMatch(
  remainingRoutes,
  /path:\s*'task\/todo'[\s\S]{0,260}component:\s*\(\)\s*=>\s*import\('@\/views\/bpm\/task\/todo\/index\.vue'\)/,
  'BPM todo legacy list route must not mount the old list component'
)
assert.match(
  remainingRoutes,
  /path:\s*'process-instance\/my'[\s\S]{0,260}moduleCode:\s*'BPM'[\s\S]{0,120}viewType:\s*'MY_INITIATED'/,
  'BPM my process legacy route must redirect to unified MY_INITIATED tasks'
)
assert.match(
  remainingRoutes,
  /path:\s*'task\/todo'[\s\S]{0,260}moduleCode:\s*'BPM'[\s\S]{0,120}viewType:\s*'TODO'/,
  'BPM todo legacy route must redirect to unified TODO tasks'
)
assert.match(
  remainingRoutes,
  /path:\s*'task\/done'[\s\S]{0,260}moduleCode:\s*'BPM'[\s\S]{0,120}viewType:\s*'DONE'/,
  'BPM done legacy route must redirect to unified DONE tasks'
)
assert.match(
  dccApprovalTasks,
  /router\.replace[\s\S]{0,180}path:\s*'\/approval-center'[\s\S]{0,180}moduleCode:\s*'DCC'[\s\S]{0,120}viewType:\s*'TODO'/,
  'DCC legacy approval task component must redirect to unified DCC TODO tasks'
)
assert.match(
  edhrApprovalPage,
  /router\.replace[\s\S]{0,220}path:\s*'\/approval-center'[\s\S]{0,220}moduleCode:\s*'EDHR'/,
  'eDHR legacy approval list component must redirect to unified EDHR tasks'
)

assert.match(showroomRoutes, /path:\s*'approval'/, 'Showroom approval formal route must remain available')
assert.match(
  showroomRoutes,
  /name:\s*'ShowroomAdminApproval'[\s\S]*hidden:\s*true/,
  'Showroom approval route must be hidden from the module menu'
)
assert.doesNotMatch(
  showroomRoutes,
  /name:\s*'ShowroomAdminApproval'[\s\S]{0,180}title:\s*'审批中心'/,
  'Showroom formal page must not remain a visible private approval center'
)

process.stdout.write('approval-center phase5 retirement static contract passed\n')
