const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const projectRoot = path.resolve(frontendRoot, '..')

const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readProject = (relativePath) =>
  fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')

const bpmApi = readFrontend('src/api/bpm/processInstance/index.ts')
const detail = readFrontend('src/views/bpm/processInstance/detail/index.vue')
const timeline = readFrontend('src/views/bpm/processInstance/detail/ProcessInstanceTimeline.vue')
const approvalDetailVO = readProject(
  'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/task/vo/instance/BpmApprovalDetailRespVO.java'
)
const processInstanceService = readProject(
  'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmProcessInstanceServiceImpl.java'
)

assert.match(
  approvalDetailVO,
  /class ActivityNode[\s\S]*private String assigneeRoleCode;[\s\S]*private String assigneeRoleName;/,
  'BPM 审批详情节点必须返回正式审批角色身份，供流程时间线显示。'
)
assert.match(
  approvalDetailVO,
  /class ActivityNodeTask[\s\S]*private String assigneeRoleCode;[\s\S]*private String assigneeRoleName;/,
  'BPM 审批详情任务必须返回正式审批角色身份，避免只暴露随机 assignee 用户。'
)
assert.match(
  processInstanceService,
  /applyRegistrationCertificateApprovalRole\(approveNodes,\s*processVariables\);[\s\S]*parseUserIds/,
  'BPM 审批详情组装用户信息前必须先把注册证正式审批角色写入节点和任务。'
)
assert.match(
  processInstanceService,
  /roleApi\.getRoleByCode\(REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE\)/,
  '注册证审批详情角色名称必须来自系统正式角色，而不是前端硬编码或随机用户。'
)

assert.match(
  bpmApi,
  /export type ApprovalTaskInfo[\s\S]*assigneeRoleCode\?:\s*string[\s\S]*assigneeRoleName\?:\s*string/,
  '前端 BPM 审批任务类型必须包含正式审批角色字段。'
)
assert.match(
  bpmApi,
  /export type ApprovalNodeInfo[\s\S]*assigneeRoleCode\?:\s*string[\s\S]*assigneeRoleName\?:\s*string/,
  '前端 BPM 审批节点类型必须包含正式审批角色字段。'
)
assert.match(
  timeline,
  /const resolveApprovalRoleLabel = \(roleName\?: string \| null\) =>[\s\S]*`审批角色：\$\{roleName\}`/,
  '审批时间线必须提供统一的“审批角色：角色名”显示规则。'
)
assert.match(
  timeline,
  /const resolveTaskReviewerLabel =[\s\S]*resolveApprovalRoleLabel\(task\.assigneeRoleName \|\| activity\.assigneeRoleName\)[\s\S]*getUserDisplayName\(task\.assigneeUser\)/,
  '审批时间线任务显示必须优先使用正式审批角色，再显示真实个人处理人。'
)
assert.match(
  timeline,
  /v-if="resolveTaskReviewerLabel\(task,\s*activity\)"[\s\S]*\{\{\s*resolveTaskReviewerLabel\(task,\s*activity\)\s*\}\}/,
  '审批时间线可见人员胶囊必须渲染统一审核对象文本。'
)
assert.match(
  timeline,
  /v-if="!hasActivityTasks\(activity\) && resolveActivityReviewerLabel\(activity\)"/,
  '审批时间线未生成任务时也必须显示正式审批角色，不能退回候选用户列表。'
)
assert.match(
  timeline,
  /!resolveActivityReviewerLabel\(activity\)[\s\S]*isEmpty\(activity\.tasks\)[\s\S]*CandidateStrategy\.START_USER_SELECT/,
  '审批时间线有正式审批角色时不得进入发起人自选审批人按钮分支。'
)
assert.match(
  timeline,
  /<template v-if="!resolveActivityReviewerLabel\(activity\)">[\s\S]*v-for="\(\s*user,\s*idx1\s*\) in activity\.candidateUsers"/,
  '审批时间线有正式审批角色时不得继续展示候选用户列表。'
)
assert.match(
  detail,
  /const actorNames = currentApprovalNodes\.value\.flatMap\(\(node\) =>[\s\S]*resolveApprovalNodeReviewerLabel\(node\)/,
  '流程详情顶部“当前处理人”必须复用同一正式审核对象规则。'
)

console.log('PASS: BPM process detail reviewer role display static contract')
