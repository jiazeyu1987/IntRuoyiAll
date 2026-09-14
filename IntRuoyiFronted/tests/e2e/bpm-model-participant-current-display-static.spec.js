const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const modelSource = fs.readFileSync(path.join(repoRoot, 'src/views/bpm/model/index.vue'), 'utf8')

assert.match(
  modelSource,
  /const\s+resolveBusinessParticipantConfig\s*=/,
  '修改弹窗必须能从业务正式审批来源生成当前审批配置。'
)

assert.match(
  modelSource,
  /resolveBusinessParticipantConfig\(model\)/,
  '回显当前审批情况时必须优先读取业务正式审批来源，不能只读取 simpleModel。'
)

assert.match(
  modelSource,
  /REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE[\s\S]*reviewers:\s*\{[\s\S]*objectType:\s*'role'[\s\S]*objectIds:\s*\[Number\(role\.id\)\]/,
  '注册证审批当前注册部经理角色必须回显到审核人区域。'
)

assert.match(
  modelSource,
  /const\s+parseBpmnParticipantConfig\s*=/,
  '没有 simpleModel 的旧 BPMN 模型必须解析 BPMN 用户任务候选人用于回显。'
)

assert.match(
  modelSource,
  /parseBpmnParticipantConfig\(model\.bpmnXml\)/,
  '回显当前审批情况时必须读取 BPMN 当前用户任务，不能显示空白默认配置。'
)

assert.match(
  modelSource,
  /const\s+resolveCurrentParticipantConfig\s*=/,
  '必须有统一的当前审批配置解析入口，避免弹窗只显示默认空表单。'
)

assert.match(
  modelSource,
  /hydrateParticipantConfig\(resolveCurrentParticipantConfig\(participantConfigModel\.value\)\)/,
  '打开修改弹窗时必须用当前审批配置回填表单。'
)

assert.doesNotMatch(
  modelSource,
  /hydrateParticipantConfig\(simpleModel\)/,
  '打开修改弹窗不能只用 simpleModel 回填，否则 BPMN 或业务角色流程会显示空白。'
)

console.log('PASS: BPM model participant config displays current approval state')
