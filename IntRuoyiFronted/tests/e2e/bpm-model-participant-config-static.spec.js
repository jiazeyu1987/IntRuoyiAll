const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const modelSource = fs.readFileSync(path.join(repoRoot, 'src/views/bpm/model/index.vue'), 'utf8')
const modelApiSource = fs.readFileSync(path.join(repoRoot, 'src/api/bpm/model/index.ts'), 'utf8')

const actionColumnMatch = modelSource.match(
  /<el-table-column[\s\S]*?label="操作"[\s\S]*?<template #default="\{ row \}">([\s\S]*?)<\/template>[\s\S]*?<\/el-table-column>/
)
assert.ok(actionColumnMatch, '流程模型列表必须保留操作列。')
const actionColumn = actionColumnMatch[1]

const updateButtonMatch = actionColumn.match(
  /<el-button[\s\S]*?@click="openApprovalParticipantConfig\(row\)"[\s\S]*?>\s*修改\s*<\/el-button>/
)
assert.ok(updateButtonMatch, '点击“修改”必须进入审核人/批准人配置，不再进入旧模型编辑页。')

assert.doesNotMatch(
  updateButtonMatch[0],
  /openModelForm\('update'/,
  '“修改”按钮不能继续调用旧模型编辑逻辑。'
)

assert.match(
  modelSource,
  /<Dialog\s+:title="participantConfigDialogTitle"\s+v-model="participantConfigVisible"[\s\S]*data-bpm-model-view="participant-config"/,
  '必须提供审核人/批准人业务配置弹窗。'
)

assert.match(
  modelSource,
  /<el-form-item[^>]*prop="reviewers"[^>]*label="审核人"[\s\S]*reviewers:\s*\[\s*\{[\s\S]*?required:\s*true/s,
  '审核人必须是必填项。'
)

assert.match(
  modelSource,
  /prop="approvers"[\s\S]*?批准人/s,
  '批准人必须可以配置。'
)

assert.doesNotMatch(
  modelSource,
  /approvers:\s*\[[\s\S]*?required:\s*true/s,
  '批准人必须是非必填项。'
)

for (const label of ['用户', '权限角色', '部门', '发起对象直属主管']) {
  assert.match(
    modelSource,
    new RegExp(`label:\\s*'${label}'|<el-option[^>]*label="${label}"`),
    `审批对象类型必须支持${label}。`
  )
}

for (const relation of ['or', 'and']) {
  assert.match(
    modelSource,
    new RegExp(`value="${relation}"|value:\\s*'${relation}'`),
    `审核人和批准人必须支持${relation === 'or' ? '或' : '和'}关系。`
  )
}

assert.match(
  modelSource,
  /const\s+buildApprovalParticipantSimpleModel\s*=/,
  '保存时必须构造正式 simpleModel。'
)

assert.match(
  modelSource,
  /ModelApi\.updateModel\([\s\S]*simpleModel:\s*buildApprovalParticipantSimpleModel\(/,
  '保存时必须调用正式模型更新接口并写入 simpleModel。'
)

assert.match(
  modelSource,
  /type:\s*BpmModelType\.SIMPLE/,
  '保存后的审批模型必须转为可部署的 SIMPLE 模型。'
)

assert.match(
  modelSource,
  /CandidateStrategy\.MIXED/,
  '或关系混选对象必须使用正式的混合审批对象策略。'
)

assert.match(
  modelSource,
  /NodeType\.PARALLEL_BRANCH_NODE/,
  '和关系多对象必须生成正式并行分支，保证每个对象都要通过。'
)

assert.match(
  modelSource,
  /ApproveMethodType\.ANY_APPROVE/,
  '角色或部门对象内部必须按任意成员代表通过处理。'
)

assert.match(
  modelApiSource,
  /simpleModel\??:\s*unknown|simpleModel\??:\s*any/,
  '模型 API 类型必须允许更新 simpleModel。'
)

console.log('PASS: BPM model participant config static contract')
