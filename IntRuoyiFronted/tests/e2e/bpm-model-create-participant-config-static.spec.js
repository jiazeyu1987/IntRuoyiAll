const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const modelSource = fs.readFileSync(path.join(repoRoot, 'src/views/bpm/model/index.vue'), 'utf8')
const modelApiSource = fs.readFileSync(path.join(repoRoot, 'src/api/bpm/model/index.ts'), 'utf8')

const actionStart = modelSource.indexOf('<template #actions>')
const actionEnd = modelSource.search(/<template\s+#table\b[^>]*>/)
assert.ok(actionStart > -1 && actionEnd > actionStart, '流程模型页必须保留动作工具栏。')
const actionSlot = modelSource.slice(actionStart, actionEnd)

assert.match(
  actionSlot,
  /@click="openCreateApprovalParticipantConfig"/,
  '点击“新建模型”必须打开新建审批模型业务配置弹窗。'
)

assert.doesNotMatch(
  actionSlot,
  /openModelForm\('create'\)/,
  '点击“新建模型”不能继续进入原有新建流程页面。'
)

assert.match(
  modelSource,
  /const\s+participantConfigMode\s*=\s*ref<['"]create['"]\s*\|\s*['"]update['"]>/,
  '审批模型配置弹窗必须区分新建和修改模式。'
)

assert.match(
  modelSource,
  /v-if="participantConfigMode === 'create'"[\s\S]*prop="name"[\s\S]*label="流程名字"[\s\S]*请输入流程名字/,
  '新建审批模型弹窗必须允许用户手工输入流程名字。'
)

assert.match(
  modelSource,
  /name:\s*\[[\s\S]*participantConfigMode\.value\s*===\s*'create'[\s\S]*请输入流程名字/,
  '新建审批模型保存前必须校验流程名字必填。'
)

assert.match(
  modelSource,
  /const\s+openCreateApprovalParticipantConfig\s*=\s*async\s*\(\)\s*=>[\s\S]*participantConfigMode\.value\s*=\s*'create'[\s\S]*resetParticipantConfigForm\(\)[\s\S]*loadParticipantConfigOptions\(\)[\s\S]*participantConfigVisible\.value\s*=\s*true/,
  '新建审批模型必须复用审核人/批准人配置弹窗。'
)

assert.match(
  modelSource,
  /const\s+buildApprovalModelKey\s*=\s*\(name:\s*string\)\s*=>[\s\S]*approval_model_/,
  '新建审批模型必须自动生成合法流程标识，用户只需要输入流程名字。'
)

assert.match(
  modelSource,
  /ModelApi\.createModel\(\{[\s\S]*name:\s*resolveParticipantConfigName\(\)[\s\S]*key:\s*buildApprovalModelKey\(resolveParticipantConfigName\(\)\)[\s\S]*type:\s*BpmModelType\.SIMPLE[\s\S]*formType:\s*BpmModelFormType\.NORMAL[\s\S]*visible:\s*true[\s\S]*managerUserIds:\s*\[userStore\.getUser\.id\][\s\S]*simpleModel:\s*buildApprovalParticipantSimpleModel\(\)[\s\S]*\}\)/,
  '新建保存必须调用正式模型创建接口，并写入 SIMPLE 审批模型。'
)

assert.match(
  modelSource,
  /ModelApi\.updateModel\([\s\S]*simpleModel:\s*buildApprovalParticipantSimpleModel\(/,
  '修改保存仍必须调用正式模型更新接口，并写入 SIMPLE 审批模型。'
)

assert.match(
  modelSource,
  /reviewers:\s*\[[\s\S]*required:\s*true[\s\S]*请至少配置一个审核人/,
  '审核人必须保持必填。'
)

assert.doesNotMatch(
  modelSource,
  /approvers:\s*\[[\s\S]*required:\s*true/,
  '批准人必须保持非必填。'
)

assert.match(
  modelApiSource,
  /export\s+type\s+ModelCreateReqVO\s*=/,
  '模型 API 必须提供新建审批模型请求类型。'
)

assert.match(
  modelApiSource,
  /export\s+const\s+createModel\s*=\s*async\s*\(data:\s*ModelCreateReqVO\)/,
  '模型新建接口不能继续要求完整列表模型类型。'
)

console.log('PASS: BPM model create participant config static contract')
