const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const source = fs.readFileSync(path.join(repoRoot, 'src/views/bpm/model/index.vue'), 'utf8')

const viewDialogMatch = source.match(
  /<Dialog\s+:title="modelApprovalRouteDialogTitle"\s+v-model="viewDetailVisible"[\s\S]*?<\/Dialog>\s*<Dialog\s+title="表单详情"/
)

assert.ok(viewDialogMatch, 'BPM model page must keep the read-only model view dialog')

const viewDialog = viewDialogMatch[0]

assert.match(
  viewDialog,
  /data-bpm-model-view="approval-route"/,
  'view dialog must render the approval route map surface'
)

assert.match(
  viewDialog,
  /v-for="\(\s*step,\s*index\s*\)\s+in\s+modelApprovalRouteSteps"/,
  'view dialog must render approval participants as vertical route steps'
)

assert.doesNotMatch(
  viewDialog,
  /<el-descriptions[\s\S]*<\/el-descriptions>/,
  'view dialog must not use a descriptions table for participant-only viewing'
)

for (const role of ['starter', 'reviewer', 'approver']) {
  assert.match(
    viewDialog,
    new RegExp(`data-approval-role="\\$\\{step\\.key\\}"|:data-approval-role="step\\.key"`),
    `view dialog must expose approval route role markers including ${role}`
  )
}

for (const label of ['发起权限', '审核环节', '批准环节']) {
  assert.match(source, new RegExp(`label:\\s*'${label}'`), `approval route steps must include ${label}`)
}

for (const oldLabel of ['谁发起', '谁审核', '谁审批', '查看流程模型']) {
  assert.doesNotMatch(
    viewDialog,
    new RegExp(oldLabel),
    `approval route dialog must use formal wording instead of ${oldLabel}`
  )
}

assert.match(
  source,
  /if\s*\(startUsers\.length\s*===\s*0\s*&&\s*startDepts\.length\s*===\s*0\)\s*return\s*'全部人员可发起'/,
  'starter empty-scope text must use formal wording'
)

assert.match(
  source,
  /'34':\s*'由当前审批人指定'/,
  'approver-selected candidate strategy must use formal wording'
)

assert.match(
  source,
  /'35':\s*'由发起人指定'/,
  'starter-selected candidate strategy must use formal wording'
)

assert.match(
  source,
  /'36':\s*'发起人本人'/,
  'requester candidate strategy must use business-readable wording'
)

assert.match(
  source,
  /'37':\s*'发起人所在部门负责人'/,
  'starter department leader strategy must use business-readable wording'
)

assert.match(
  source,
  /Dept Leader Approval['"]:\s*'部门负责人审批'/,
  'approval route must translate English node names before display'
)

assert.match(
  source,
  /Start user dept leader['"]:\s*'发起人所在部门负责人'/,
  'approval route must translate English starter-department-leader text before display'
)

assert.match(
  source,
  /Requester['"]?:\s*'发起人本人'/,
  'approval route must translate requester text before display'
)

assert.match(
  source,
  /const\s+APPROVAL_ROUTE_PARAMETERLESS_STRATEGIES[\s\S]*'36'[\s\S]*'37'/,
  'approval route must hide meaningless raw params for starter-person and starter-department-leader strategies'
)

assert.match(
  source,
  /const\s+APPROVAL_ROUTE_TEMPLATE_FIELD_LABELS[\s\S]*batchRecordName:\s*'批记录名称'[\s\S]*versionNo:\s*'版本号'[\s\S]*batchExecutionCode:\s*'批次执行编码'[\s\S]*workOrderCode:\s*'工单编码'/,
  'approval route must translate BPMN template variables into readable business field names'
)

assert.match(
  source,
  /Object\.entries\(APPROVAL_ROUTE_TEMPLATE_FIELD_LABELS\)\.forEach\(\(\[fieldKey,\s*readableLabel\]\)[\s\S]*escapeApprovalRoutePattern\(fieldKey\)[\s\S]*readableText\s*=\s*readableText\.replace\(fieldPattern,\s*readableLabel\)/,
  'approval route must translate standalone field keys such as batchExecutionCode and workOrderCode before display'
)

assert.match(
  source,
  /const\s+formatApprovalRouteTemplateText\s*=/,
  'approval route must normalize BPMN node names before display'
)

assert.match(
  source,
  /节点：\$\{name\}/,
  'approval route participant entries must label node names for human readability'
)

assert.match(
  source,
  /审批对象：\$\{text\}/,
  'approval route participant entries must label approval objects for human readability'
)

assert.match(
  source,
  /const\s+selectParticipantSource\s*=/,
  'approval route must avoid stacking simple-model and BPMN participant text together'
)

assert.match(
  source,
  /'10':\s*'审批角色'/,
  'role candidate strategy must display as approval role for readability'
)

assert.match(
  source,
  /import\s+\*\s+as\s+RoleApi\s+from\s+'@\/api\/system\/role'/,
  'approval route must import the system role API to resolve readable role names'
)

assert.match(
  source,
  /RoleApi\.getSimpleRoleList\(\)/,
  'view dialog must load the system role list before rendering role candidate names'
)

assert.match(
  source,
  /const\s+resolveApprovalRoleName\s*=/,
  'approval route must have a dedicated role-name resolver'
)

assert.match(
  source,
  /const\s+REGISTRATION_CERTIFICATE_APPROVAL_PROCESS_KEY\s*=\s*'dcc-registration-certificate-access'/,
  'registration certificate approval route display must be keyed by the stable process key'
)

assert.match(
  source,
  /const\s+REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE\s*=\s*'dcc_registration_certificate_approver'/,
  'registration certificate approval route display must use the formal registration manager role code'
)

assert.match(
  source,
  /const\s+resolveApprovalRoleNameByCode\s*=/,
  'approval route must resolve business-owned approver role names by formal role code'
)

assert.match(
  source,
  /未识别角色（编码：/,
  'unmatched business role codes must be explicit instead of hiding the missing role'
)

assert.match(
  source,
  /const\s+resolveBusinessApprovalRouteParticipants\s*=/,
  'approval route must expose business-owned participant summaries when a flow has business-side assignee resolution'
)

assert.match(
  source,
  /审批对象：\$\{roleNames\}/,
  'registration certificate route must display the resolved registration manager as the approval object'
)

assert.match(
  source,
  /未识别角色（ID：/,
  'unmatched role ids must be explicit instead of exposing an unreadable raw id'
)

assert.match(
  source,
  /formatCandidateRule\([^)]*approvalRoleList\.value/s,
  'candidate rule formatting must use the loaded role list to render readable approval role names'
)

assert.match(
  source,
  /white-space:\s*pre-line;/,
  'approval route values must preserve readable line breaks'
)

for (const hiddenLabel of [
  '流程名',
  '流程标识',
  '流程分类',
  '可见范围',
  '流程类型',
  '表单信息',
  '最后发布'
]) {
  assert.doesNotMatch(
    viewDialog,
    new RegExp(`label="${hiddenLabel}"`),
    `view dialog must hide ${hiddenLabel} because only participant roles matter`
  )
}

assert.match(
  source,
  /const\s+openModelView\s*=\s*async\s*\(row:\s*ModelInfo\)\s*=>[\s\S]*ModelApi\.getModel\(String\(row\.id\)\)/,
  'view dialog must load model detail before resolving audit and approval participants'
)

assert.match(
  source,
  /const\s+modelViewParticipants\s*=\s*computed\(/,
  'view dialog must derive a stable participant summary'
)

assert.match(
  source,
  /const\s+businessParticipants\s*=\s*resolveBusinessApprovalRouteParticipants\(model\)/,
  'participant summary must apply business-owned approval route participants before falling back to raw BPMN strategy text'
)

assert.match(
  source,
  /const\s+modelApprovalRouteSteps\s*=\s*computed\(/,
  'view dialog must derive vertical approval route steps from participant summary'
)

assert.match(
  source,
  /isRegistrationCertificateApprovalModel\(selectedModel\.value\)[\s\S]*?filter\(\(step\)\s*=>\s*step\.key\s*===\s*'starter'\s*\|\|\s*!isUnconfiguredApprovalRouteStep\(step\)\)/,
  'single-node registration certificate route must hide empty generic audit or approval buckets'
)

assert.match(
  source,
  /parseBpmnUserTaskParticipants\(/,
  'BPMN model detail must be parsed for audit and approval task participants'
)

assert.match(
  source,
  /collectSimpleModelParticipants\(/,
  'Simple model detail must be traversed for audit and approval task participants'
)

console.log('PASS: BPM model view participant-only static contract')
