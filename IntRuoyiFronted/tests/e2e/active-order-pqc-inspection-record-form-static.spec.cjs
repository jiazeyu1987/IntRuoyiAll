const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

assert.match(
  panel,
  /data-active-order-pqc-inspection-record-form-button/,
  'PQC提交页签必须提供过程检验记录表单按钮。'
)

assert.match(
  panel,
  /openPqcInspectionRecordForm/,
  'PQC过程检验记录按钮必须打开独立表单弹框。'
)

assert.match(
  panel,
  /data-active-order-pqc-inspection-record-form-dialog/,
  'PQC过程检验记录必须使用独立弹框展示。'
)

assert.match(
  panel,
  /const buildPqcInspectionRecordRows[\s\S]*pqcProcessGroups\.value(?:\.entries\(\))?[\s\S]*for \(const submission of pqcProcess\.submissions\)/,
  'PQC过程检验记录必须汇总当前订单全部PQC提交，而不是按生产工序单独生成。'
)

assert.match(
  panel,
  /const normalizePqcInspectionJudgement[\s\S]*negativeKeywords[\s\S]*不合格[\s\S]*不通过[\s\S]*否[\s\S]*positiveKeywords[\s\S]*合格[\s\S]*通过[\s\S]*是/,
  'PQC枚举类结果必须统一归一为合格/不合格。'
)

assert.match(
  panel,
  /const formatPqcInspectionMeasuredValues[\s\S]*measuredValue[\s\S]*itemResult[\s\S]*join\('，'\)/,
  'PQC数字测量值必须完整展开并用中文逗号分隔。'
)

assert.match(
  panel,
  /<th>检测结果<\/th>[\s\S]*row\.resultText[\s\S]*measuredValuesText/,
  'PQC过程检验记录必须在检测结果中显示全部测量值。'
)

assert.match(
  panel,
  /<th>判定<\/th>[\s\S]*row\.judgementText/,
  'PQC过程检验记录必须显示统一后的合格/不合格判定。'
)

assert.match(
  panel,
  /<span>检验数量<\/span>[\s\S]*pqcInspectionRecordSummary\.inspectionQuantityText/,
  'PQC过程检验记录必须展示汇总检验数量。'
)

assert.match(
  panel,
  /formatPqcInspectionRecordEquipmentText/,
  'PQC过程检验记录必须展示检验设备。'
)

console.log('PASS: active order PQC inspection record form static contract')
