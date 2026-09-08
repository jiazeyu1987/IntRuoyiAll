const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

const productionTabStart = panel.indexOf('<el-tab-pane label="生产提交" name="productionSubmissions">')
assert.ok(productionTabStart > 0, '必须存在生产提交主页签。')
const pqcTabStart = panel.indexOf('<el-tab-pane label="PQC提交" name="pqcSubmissions">')
assert.ok(pqcTabStart > productionTabStart, '必须存在 PQC提交主页签。')
const materialTabStart = panel.indexOf('data-team-leader-active-order-detail-material-tab', pqcTabStart)
assert.ok(materialTabStart > pqcTabStart, '必须能定位 PQC提交页签边界。')

const productionTab = panel.slice(productionTabStart, pqcTabStart)
const pqcTab = panel.slice(pqcTabStart, materialTabStart)

assert.doesNotMatch(
  productionTab,
  /data-active-order-production-record-form-button/,
  '生产提交页签内不得再显示“表单”按钮；正式生产记录表单应直接内嵌。'
)
assert.doesNotMatch(
  productionTab,
  /buildActiveOrderProductionSubmissionRows\(process\)/,
  '生产提交页签不得再使用简化生产提交表格数据。'
)
assert.match(
  productionTab,
  /class="team-leader-workbench__production-record-form"[\s\S]*process\.processName[\s\S]*buildProductionRecordRows\(process\)/,
  '每个生产工序页签必须直接渲染正式生产记录表单，并使用当前工序生成表单行。'
)
assert.match(
  productionTab,
  /data-active-order-production-record-input-materials[\s\S]*process\.inputMaterials/,
  '生产记录表单内嵌内容必须展示当前工序的输入物料批次号。'
)
assert.match(
  productionTab,
  /v-if="process\.inputMaterials\?\.length"[\s\S]*data-active-order-production-record-input-materials/,
  '当前工序没有输入物料时，生产记录表单内嵌内容不得显示“输入物料批次号”整块区域。'
)
assert.doesNotMatch(
  productionTab,
  /暂无输入物料批次号/,
  '当前工序没有输入物料时，不应显示输入物料空状态占位。'
)
assert.match(
  productionTab,
  /buildProductionRecordMaterialDeviceGroups\(buildProductionRecordRows\(process\)\)/,
  '生产记录表单内嵌内容必须展示当前工序的物料使用设备/设备参数。'
)

assert.doesNotMatch(
  pqcTab,
  /data-active-order-pqc-inspection-record-form-button/,
  'PQC提交页签内不得再显示“表单”按钮；过程检验记录表单应直接内嵌。'
)
assert.doesNotMatch(
  pqcTab,
  /team-leader-workbench__active-order-pqc-card/,
  'PQC提交页签不得再显示简化 PQC 提交卡片。'
)
assert.match(
  pqcTab,
  /class="team-leader-workbench__pqc-inspection-record-form"[\s\S]*data-pqc-inspection-record-form-table[\s\S]*pqcInspectionRecordRows/,
  'PQC提交页签必须直接渲染正式过程检验记录表单。'
)
assert.match(
  pqcTab,
  /<span>批次数量<\/span>[\s\S]*pqcInspectionRecordSummary\.batchQuantityText/,
  '内嵌 PQC 过程检验记录表单必须保留批次数量汇总。'
)

console.log('PASS: active order inline form detail static contract')
