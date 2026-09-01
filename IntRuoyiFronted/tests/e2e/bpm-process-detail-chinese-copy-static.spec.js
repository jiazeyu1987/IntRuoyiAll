const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const displayNames = fs.readFileSync(
  path.join(root, 'src/views/bpm/processInstance/detail/display-name.ts'),
  'utf8'
)
const detail = fs.readFileSync(
  path.join(root, 'src/views/bpm/processInstance/detail/index.vue'),
  'utf8'
)
const timeline = fs.readFileSync(
  path.join(root, 'src/views/bpm/processInstance/detail/ProcessInstanceTimeline.vue'),
  'utf8'
)
const operation = fs.readFileSync(
  path.join(root, 'src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue'),
  'utf8'
)
const printDialog = fs.readFileSync(
  path.join(root, 'src/views/bpm/processInstance/detail/PrintDialog.vue'),
  'utf8'
)

assert.match(
  displayNames,
  /Registration certificate access request\s+\(\\d\+\)[\s\S]*注册证访问申请/,
  '流程实例标题必须把注册证访问申请英文模板确定性转换为中文并保留申请编号。'
)
assert.match(
  displayNames,
  /REG_CERT_ACCESS_APPROVAL[\s\S]*注册证访问审批/,
  '注册证访问审批节点必须按正式任务定义键显示中文。'
)
assert.match(
  detail,
  /resolveProcessInstanceDisplayName\(processInstance\.value\?\.name\)/,
  '流程详情主标题必须使用共享中文显示规则。'
)
assert.match(
  timeline,
  /resolveProcessNodeDisplayName\(activity\.id, activity\.name\)/,
  '审批时间线和下一节点必须使用共享中文节点名称。'
)
assert.match(
  operation,
  /:label="resolveProcessNodeDisplayName\(item\.taskDefinitionKey, item\.name\)"/,
  '退回节点必须使用共享中文节点名称。'
)
assert.match(
  operation,
  /resolveProcessFormDisplayName\(runningTask\?\.formName\)/,
  '节点表单标题必须使用共享中文显示规则。'
)
assert.match(
  printDialog,
  /resolveProcessInstanceDisplayName\(printData\.processInstance\.name\)/,
  '打印标题必须使用共享中文流程实例名称。'
)
assert.match(
  printDialog,
  /resolveProcessNodeDisplayName\(undefined, item\.name\)/,
  '打印流程节点必须使用共享中文节点名称。'
)
assert.equal(
  (printDialog.match(/resolveProcessNodeDisplayName\(undefined, item\.name\)/g) || []).length,
  2,
  '自定义打印模板和默认打印表格都必须使用共享中文节点名称。'
)
assert.match(
  printDialog,
  /resolveProcessFormDisplayName\(item\.name\)/,
  '打印表单字段名称必须使用共享中文表单名称规则。'
)
assert.match(
  printDialog,
  /resolveProcessDetailDescription\(item\.description\)/,
  '打印流程描述中拼接的英文节点名称必须同步中文化。'
)
assert.match(
  displayNames,
  /const segments = normalized\.split\(' \/ '\)[\s\S]*segments\[1\] = resolveProcessNodeDisplayName/,
  '打印描述只允许转换系统生成的节点名称段，不得改写审批意见等用户输入。'
)
assert.doesNotMatch(
  printDialog,
  /<h2[^>]*>\{\{\s*printData\.processInstance\.name\s*\}\}<\/h2>/,
  '打印预览不得继续直接显示英文流程实例名称。'
)

console.log('BPM process detail Chinese copy static contract passed')
