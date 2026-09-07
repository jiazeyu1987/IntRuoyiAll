import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const repoRoot = resolve(frontRoot, '..')

const component = readFileSync(
  resolve(
    frontRoot,
    'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
  ),
  'utf8'
)
const api = readFileSync(resolve(frontRoot, 'src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
const domain = readFileSync(
  resolve(
    repoRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
  ),
  'utf8'
)
const service = readFileSync(
  resolve(
    repoRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'
  ),
  'utf8'
)
const readDo = readFileSync(
  resolve(
    repoRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesTeamLeaderActiveOrderDetailReadDO.java'
  ),
  'utf8'
)
const mapper = readFileSync(
  resolve(
    repoRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml'
  ),
  'utf8'
)

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  component.includes('data-active-order-production-record-form-button') &&
    component.includes('openProductionRecordForm(process)') &&
    component.includes('生产记录表单'),
  '每个生产工序必须提供“表单”按钮并打开生产记录表单'
)

assert(
  component.includes('data-active-order-production-record-form-dialog') &&
    component.includes('selectedProductionRecordProcess') &&
    component.includes('selectedProductionRecordRows') &&
    component.includes('productionRecordTotals'),
  '生产记录表单必须基于当前工序构建表格和汇总'
)

assert(
  !component.includes(':disabled="!process.submissions?.length"') &&
    component.includes('暂无可生成的生产提交记录'),
  '每个生产工序都必须可打开表单；没有提交时显示明确空态'
)

for (const label of [
  '工序名称',
  '生产批号',
  '产品规格',
  '输出物料编码',
  '输出物料名称',
  '规格型号',
  '设备',
  '设备参数',
  '生产数量',
  '损耗数量',
  '总数量',
  '清场确认',
  '物料确认',
  '清洁确认'
]) {
  assert(component.includes(label), `生产记录表单缺少字段：${label}`)
}

assert(
  component.includes("formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'workplace')") &&
    component.includes("formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'material')") &&
    component.includes("formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'cleaning')") &&
    component.includes('未记录') &&
    component.includes('暂无设备参数'),
  '三项 checkbox 确认必须按清场/物料/清洁独立展示，缺失设备参数必须显式展示暂无设备参数'
)

assert(
  api.includes('batchCode?: string') &&
    api.includes('productSpecification?: string') &&
    api.includes('clearanceConfirmations: TeamLeaderActiveOrderClearanceConfirmationRespVO[]') &&
    api.includes('deviceParameters: TeamLeaderActiveOrderSubmissionDeviceParameterRespVO[]'),
  '前端 API 类型必须包含批号、产品规格、设备参数和清场确认字段'
)

assert(
  domain.includes('private String batchCode;') &&
    domain.includes('private String productSpecification;') &&
    domain.includes('class SubmissionDeviceParameterDetail') &&
    domain.includes('class ClearanceConfirmationDetail'),
  '后端领域详情必须承载批号、产品规格、设备参数和清场确认'
)

assert(
  readDo.includes('private String batchCode;') &&
    readDo.includes('private String productSpecification;') &&
    mapper.includes('work_order.batch_code AS batchCode') &&
    mapper.includes('product_item.specification AS productSpecification'),
  '后端读模型必须从正式工单和产品物料读取批号与产品规格'
)

assert(
  service.includes('resolveSubmissionDeviceParameters') &&
    service.includes('resolveClearanceConfirmations') &&
    service.includes('clearanceConfirmations') &&
    service.includes('deviceParameterReadings'),
  '详情服务必须从正式原始提交 payload 解析设备参数和三项确认'
)

console.log('PASS: active order production record form static contract')
