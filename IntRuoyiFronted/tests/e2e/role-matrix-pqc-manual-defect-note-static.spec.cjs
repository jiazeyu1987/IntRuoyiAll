const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const panelPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const apiPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/api/mes/pro/feedback/index.ts'
)

const panelSource = fs.readFileSync(panelPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.doesNotMatch(
  panelSource,
  /data-pqc-defect-description|frontline-pqc-defect-description|defectDescription|nonconformanceDescription/,
  'PQC 填写页必须移除手动不良说明控件、草稿字段和提交字段。'
)
assert.match(
  apiSource,
  /interface FrontlinePqcInspectionSubmitReqVO/,
  '前端 PQC 提交类型必须保留正式提交接口。'
)
assert.doesNotMatch(
  apiSource,
  /nonconformanceDescription|defectDescription/,
  '前端 PQC 提交类型不得声明已移除的不良说明字段。'
)

console.log('PASS role-matrix PQC manual defect note static contract')
