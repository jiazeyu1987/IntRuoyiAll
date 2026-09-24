const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

const pqcTabStart = panel.indexOf('label="过程检表单"')
const processRecordTabStart = panel.indexOf('label="过程检验记录"', pqcTabStart)
const nextTabStart = panel.indexOf('</el-tab-pane>', processRecordTabStart)
assert.ok(pqcTabStart > 0, 'PQC提交页签必须存在。')
assert.ok(processRecordTabStart > pqcTabStart, 'PQC提交必须包含过程检验记录内层页签。')
assert.ok(nextTabStart > pqcTabStart, '必须能定位 PQC提交页签边界。')
const pqcTabBlock = panel.slice(pqcTabStart, nextTabStart)

assert.ok(
  pqcTabBlock.includes('name="pqcSubmissions"') &&
    pqcTabBlock.includes('class="team-leader-workbench__pqc-inspection-record-form"'),
  'PQC提交页签必须直接内嵌过程检验记录表单。'
)

assert.ok(
  !pqcTabBlock.includes('data-active-order-pqc-inspection-record-form-button'),
  'PQC提交页签不得再显示过程检验记录表单按钮。'
)

assert.match(
  panel,
  /data-pqc-inspection-record-form-table/,
  'PQC过程检验记录必须使用正式表单表格展示。'
)

assert.match(
  panel,
  /const buildPqcInspectionRecordRows[\s\S]*pqcProcessGroups\.value(?:\.entries\(\))?[\s\S]*for \(const submission of pqcProcess\.submissions\)/,
  'PQC过程检验记录必须汇总当前订单全部PQC提交，而不是按生产工序单独生成。'
)

assert.match(
  panel,
  /const normalizePqcPassFailText[\s\S]*negativeKeywords[\s\S]*不合格[\s\S]*不通过[\s\S]*否[\s\S]*positiveKeywords[\s\S]*合格[\s\S]*通过[\s\S]*是/,
  'PQC枚举类结果必须统一归一为通过/不通过。'
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
  'PQC过程检验记录必须显示统一后的通过/不通过判定。'
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
