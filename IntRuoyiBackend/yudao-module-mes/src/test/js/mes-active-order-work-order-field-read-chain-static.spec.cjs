const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (file) => fs.readFileSync(path.join(repoRoot, file), 'utf8')

const mapperXml = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml'
)
const readDo = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesTeamLeaderActiveOrderDetailReadDO.java'
)
const detailModel = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
)
const detailService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'
)
const detailVo = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
)
const controller = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)

assert.match(
  mapperXml,
  /work_order\.demand_bill_no\s+AS\s+demandBillNo/,
  '活跃订单详情 SQL 必须从生产工单读取生产指令 demandBillNo。'
)
assert.match(
  mapperXml,
  /work_order\.material_specification\s+AS\s+productSpecification/,
  '活跃订单详情 SQL 的 productSpecification 必须来自生产工单 material_specification。'
)
assert.doesNotMatch(
  mapperXml,
  /product_item\.specification\s+AS\s+productSpecification/,
  'productSpecification 不得继续从物料主数据 specification 读取。'
)
assert.match(
  mapperXml,
  /work_order\.drawing_number\s+AS\s+drawingNumber/,
  '图号仍必须从生产工单 drawing_number 读取。'
)

for (const [name, source] of [
  ['read DO', readDo],
  ['service detail model', detailModel],
  ['response VO', detailVo]
]) {
  assert.match(source, /private String demandBillNo;/, `${name} 必须暴露生产指令 demandBillNo。`)
  assert.match(
    source,
    /private String productSpecification;/,
    `${name} 必须保留生产工单型号规格 productSpecification。`
  )
}

assert.match(
  detailService,
  /\.setDemandBillNo\(first\.getDemandBillNo\(\)\)/,
  '详情服务必须把读取行中的 demandBillNo 传入领域详情。'
)
assert.match(
  detailService,
  /\.setProductSpecification\(first\.getProductSpecification\(\)\)/,
  '详情服务必须把生产工单型号规格传入领域详情。'
)
assert.match(
  controller,
  /\.setDemandBillNo\(detail\.getDemandBillNo\(\)\)/,
  'Controller 必须把 demandBillNo 映射到响应 VO。'
)
assert.match(
  controller,
  /\.setProductSpecification\(detail\.getProductSpecification\(\)\)/,
  'Controller 必须把生产工单型号规格映射到响应 VO。'
)

console.log('PASS: mes active order work-order field read-chain static contract')
