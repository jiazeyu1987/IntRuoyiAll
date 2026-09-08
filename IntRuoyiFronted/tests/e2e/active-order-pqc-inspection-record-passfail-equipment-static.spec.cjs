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
  /const normalizePqcPassFailText[\s\S]*negativeKeywords[\s\S]*不合格[\s\S]*不通过[\s\S]*NG[\s\S]*FAIL[\s\S]*return '不通过'[\s\S]*positiveKeywords[\s\S]*合格[\s\S]*通过[\s\S]*OK[\s\S]*SUCCESS[\s\S]*return '通过'/,
  'PQC 检测结果和判定必须把 OK/SUCCESS 等英文成功状态显示为“通过”，把 NG/FAIL 等失败状态显示为“不通过”。'
)

assert.match(
  panel,
  /const formatPqcInspectionMeasuredValues[\s\S]*normalizePqcPassFailText\(item\.measuredValue \|\| item\.itemResult\)[\s\S]*join\('，'\)/,
  'PQC 检测结果列必须通过 normalizePqcPassFailText 显示为通过/不通过，并保留多样本中文逗号分隔。'
)

assert.match(
  panel,
  /const normalizePqcInspectionJudgement[\s\S]*normalizePqcPassFailText\(item\.judgement \|\| item\.itemResult \|\| item\.measuredValue\)[\s\S]*\|\| '未记录'/,
  'PQC 判定列必须通过 normalizePqcPassFailText 显示为通过/不通过。'
)

assert.match(
  panel,
  /const summarizePqcInspectionJudgements[\s\S]*judgements\.includes\('不通过'\)[\s\S]*return '不通过'[\s\S]*judgements\[0\] === '通过'[\s\S]*return '通过'/,
  'PQC 汇总判定必须输出通过/不通过，而不是合格/不合格或 SUCCESS。'
)

assert.match(
  panel,
  /const formatActiveOrderPqcEquipmentSummary[\s\S]*selectedEquipmentName[\s\S]*selectedEquipmentNumber[\s\S]*`\$\{equipmentName\}\(\$\{equipmentNumber\}\)`/,
  'PQC 检验设备必须显示为设备名称(设备编号)。'
)

assert.doesNotMatch(
  panel,
  /selectedEquipmentNumber \|\| item\.selectedEquipmentName/,
  'PQC 检验设备不得再优先只显示设备编号。'
)

console.log('PASS: active order PQC inspection pass/fail and equipment display static contract')
