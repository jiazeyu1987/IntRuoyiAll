const fs = require('fs')
const path = require('path')
const assert = require('assert')

const projectRoot = path.resolve(__dirname, '..', '..')
const componentPath = path.join(
  projectRoot,
  'src',
  'views',
  'mes',
  'pro',
  'processpool',
  'components',
  'ActiveOrderSubmissionDetailPanel.vue'
)

const source = fs.readFileSync(componentPath, 'utf8')

const dialogStart = source.indexOf('data-active-order-pqc-inspection-record-form-dialog')
assert(dialogStart > 0, 'PQC 过程检验记录弹框不存在')
const dialogBlock = source.slice(source.lastIndexOf('<el-dialog', dialogStart), source.indexOf('>', dialogStart))
assert(
  /width="(?:95|96|97|98)vw"/.test(dialogBlock),
  'PQC 过程检验记录弹框必须使用接近全屏的视口宽度，避免纸质表单右侧列被截断'
)

const tableStart = source.indexOf('data-pqc-inspection-record-form-table')
assert(tableStart > 0, 'PQC 过程检验记录表不存在')
const tableBlock = source.slice(source.lastIndexOf('<table', tableStart), source.indexOf('</table>', tableStart))

assert(
  tableBlock.includes('<thead>') && tableBlock.includes('<tbody>'),
  'PQC 过程检验记录必须使用纸质表单表格，包含业务表头和数据区'
)

for (const forbiddenTag of ['<el-table', '<el-table-column']) {
  assert(!tableBlock.includes(forbiddenTag), `PQC 过程检验记录不应继续使用列表表头：${forbiddenTag}`)
}

assert(!tableBlock.includes('检验项目信息'), '不应再把多个业务字段合成“检验项目信息”一列')
assert(/<th[^>]*>序号<\/th>/.test(tableBlock), '纸质表头必须包含序号')
assert(/<th[^>]*>检验日期<\/th>/.test(tableBlock), '纸质表头必须包含检验日期')
assert(/<th[^>]*colspan="4"[^>]*>检验项目<\/th>/.test(tableBlock), '检验项目必须跨 4 个业务层级列')
assert(/<th[^>]*>检验类型<\/th>/.test(tableBlock), '纸质表头必须从检验类型开始拆分结果行')
assert(/<th[^>]*>检测数量 pcs<\/th>/.test(tableBlock), '纸质表头必须包含检测数量 pcs')
assert(/<th[^>]*>检测结果<\/th>/.test(tableBlock), '纸质表头必须包含检测结果')
assert(/<th[^>]*>检验设备<\/th>/.test(tableBlock), '纸质表头必须包含检验设备')
assert(/<th[^>]*>判定<\/th>/.test(tableBlock), '纸质表头必须包含判定')
assert(/<th[^>]*>检验人\/日期<\/th>/.test(tableBlock), '纸质表头必须包含检验人/日期')
assert(/<th[^>]*>复核人\/日期<\/th>/.test(tableBlock), '纸质表头必须包含复核人/日期')
assert(
  /data-pqc-inspection-record-common-cell/.test(tableBlock),
  '检验项目层级单元格必须有稳定 DOM 标识'
)
assert(
  /row\.qaProcessName/.test(tableBlock) &&
    /row\.itemNameText/.test(tableBlock) &&
    /row\.inspectionMethodText/.test(tableBlock) &&
    /row\.standardText/.test(tableBlock),
  '公共信息单元格必须同时显示 PQC工序、检验项、检验方法、标准'
)
const commonCellStart = tableBlock.indexOf('data-pqc-inspection-record-common-cell')
assert(commonCellStart > 0, '必须存在公共信息单元格')
const commonCellBlock = tableBlock.slice(
  tableBlock.lastIndexOf('<div', commonCellStart),
  tableBlock.indexOf('</div>', commonCellStart) + '</div>'.length
)
for (const internalHeader of ['PQC工序', '检验项', '检验方法', '标准']) {
  assert(
    !commonCellBlock.includes(`<span>${internalHeader}</span>`),
    `公共信息单元格内部不应再拆出小表头：${internalHeader}`
  )
}
assert(
  /data-pqc-inspection-record-common-process/.test(tableBlock) &&
    /data-pqc-inspection-record-common-item/.test(tableBlock) &&
    /data-pqc-inspection-record-common-method/.test(tableBlock) &&
    /data-pqc-inspection-record-common-standard/.test(tableBlock),
  '公共信息单元格内部必须以表格式值单元呈现工序、检验项、检验方法、标准'
)

const scriptBlock = source.slice(source.indexOf('<script setup lang="ts">'), source.indexOf('</script>'))
assert(
  /sequenceRowSpan:\s*number/.test(scriptBlock) &&
    /inspectionDateRowSpan:\s*number/.test(scriptBlock) &&
    /qaProcessRowSpan:\s*number/.test(scriptBlock) &&
    /itemRowSpan:\s*number/.test(scriptBlock) &&
    /methodRowSpan:\s*number/.test(scriptBlock) &&
    /standardRowSpan:\s*number/.test(scriptBlock),
  'PQC 汇总行必须携带序号、日期、工序、检验项、方法、标准的逐级 rowspan'
)
assert(
  /:rowspan="row\.qaProcessRowSpan"/.test(tableBlock) &&
    /:rowspan="row\.itemRowSpan"/.test(tableBlock) &&
    /:rowspan="row\.methodRowSpan"/.test(tableBlock) &&
    /:rowspan="row\.standardRowSpan"/.test(tableBlock),
  '检验项目下的工序、检验项、检验方法、标准必须分别按自身层级 rowspan 合并'
)
assert(
  /const formatActiveOrderPqcItemUniqueSummary/.test(scriptBlock),
  '检验方法和标准必须使用去重摘要，不能按样本数追加 ×N，否则相同内容无法合并'
)
assert(
  /const inspectionMethodText = formatActiveOrderPqcItemUniqueSummary/.test(scriptBlock) &&
    /const standardText = formatActiveOrderPqcItemUniqueSummary/.test(scriptBlock),
  '检验方法和标准列必须用去重后的文本参与显示与 rowspan 合并'
)
assert(
  /itemIdentityKey:\s*string/.test(scriptBlock),
  'PQC 过程检验记录行必须携带正式项目身份，不能只用显示名称合并复合项目'
)
assert(
  /const resolveActiveOrderPqcItemIdentityKey/.test(scriptBlock) &&
    /itemCode/.test(scriptBlock) &&
    /aggregateDetailId/.test(scriptBlock),
  'PQC 复合项目必须按 itemCode/aggregateDetailId 等正式身份分组'
)
const itemGroupingBlock = scriptBlock.slice(
  scriptBlock.indexOf('const buildActiveOrderPqcItemRows'),
  scriptBlock.indexOf('const normalizePqcInspectionJudgement')
)
assert(
  /const key = resolveActiveOrderPqcItemIdentityKey\(item\)/.test(itemGroupingBlock),
  'PQC 检验项聚合 key 必须来自正式项目身份'
)
assert(
  !/const key = itemName \|\| itemCode/.test(itemGroupingBlock),
  'PQC 检验项聚合禁止优先按 itemName 合并，否则复合件多个“外观”会被误合并'
)
assert(
  /for \(const groupedItem of groupedItems\)[\s\S]*for \(const submission of pqcProcess\.submissions\)/.test(
    scriptBlock
  ),
  'PQC 纸质表格必须先按正式检验项目排列，再在项目下连续展示各检验类型'
)
assert(
  /row\.itemIdentityKey/.test(scriptBlock),
  'PQC rowspan key 必须包含正式项目身份，不能仅包含 itemNameText'
)

console.log('PASS stage1-pqc-inspection-record-rowspan-layout-static')

