const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const modelSource = fs.readFileSync(path.join(repoRoot, 'src/views/bpm/model/index.vue'), 'utf8')

assert.match(
  modelSource,
  /<Dialog\s+:title="modelApprovalRouteDialogTitle"\s+v-model="viewDetailVisible"/,
  '流程审批路线弹窗标题必须绑定当前审批路线名称，不能只显示通用标题。'
)

assert.match(
  modelSource,
  /const\s+modelApprovalRouteDialogTitle\s*=\s*computed\(\(\)\s*=>\s*\{[\s\S]*?const\s+approvalRouteName\s*=\s*resolveModelDisplayName\(selectedModel\.value\)[\s\S]*?return\s+approvalRouteName\s*\?\s*`审批路线：\$\{approvalRouteName\}`\s*:\s*'流程审批路线'/,
  '流程审批路线弹窗标题必须来自当前流程模型正式显示名。'
)

assert.match(
  modelSource,
  /const\s+formatApprovalRouteParticipant\s*=/,
  '流程审批路线弹窗必须把批准环节格式化为审批路线名称展示。'
)

assert.match(
  modelSource,
  /审批路线：\$\{approvalRouteName\}/,
  '批准环节必须显示“审批路线：流程名”。'
)

assert.match(
  modelSource,
  /审批角色：\$\{roleNames\}/,
  '业务层解析出的角色类审批对象必须显示为“审批角色：角色名”。'
)

assert.match(
  modelSource,
  /const\s+approvalRouteName\s*=\s*resolveModelDisplayName\(model\)/,
  '审批路线名称必须来自流程模型的正式显示名。'
)

assert.match(
  modelSource,
  /selectBusinessParticipantSource\(\s*businessParticipants\.approvers,\s*simpleParticipants\.approvers,\s*bpmnParticipants\.approvers\s*\)[\s\S]*?\.map\(\s*\(item\)\s*=>\s*formatApprovalRouteParticipant\(item,\s*approvalRouteName\)\s*\)/,
  '批准环节必须在业务审批人映射后把候选审批节点转换为审批路线名称展示。'
)

console.log('PASS: BPM model approval route view displays the route name')
