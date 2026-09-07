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
const controller = readFileSync(
  resolve(
    repoRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
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

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  component.includes('data-active-order-production-record-material-device-group') &&
    component.includes('data-active-order-production-record-device-table') &&
    component.includes('data-active-order-production-record-device-parameter-table') &&
    component.includes('selectedProductionRecordMaterialDeviceGroups') &&
    component.includes('buildProductionRecordMaterialDeviceGroups'),
  '生产记录表单必须按输出物料分组，物料下再展示设备表和对应设备参数表'
)

for (const label of ['设备名称', '设备编号', '计量状态', '参数数量', '参数名称', '参数范围', '提交值']) {
  assert(component.includes(`label="${label}"`), `物料设备参数表缺少列：${label}`)
}

assert(
  /v-for="materialDeviceGroup in selectedProductionRecordMaterialDeviceGroups"[\s\S]*输出物料[\s\S]*v-for="deviceGroup in materialDeviceGroup\.devices"/.test(
    component
  ),
  '设备参数必须嵌套在对应输出物料下面，不能跨物料汇总成一张扁平表'
)

assert(
  component.includes('formatProductionRecordDeviceMeteringValidityText') &&
    component.includes('计量有效') &&
    component.includes('计量超期') &&
    component.includes('计量状态未记录'),
  '物料设备参数表必须展示设备计量状态，并区分有效、超期和未记录'
)

assert(
  component.includes('formatProductionRecordParameterRange') &&
    component.includes('lowerLimit') &&
    component.includes('upperLimit') &&
    component.includes('≥') &&
    component.includes('≤'),
  '物料设备参数表必须按上下限展示参数范围'
)

assert(
  component.includes('isProductionRecordParameterOutOfRange') &&
    component.includes('team-leader-workbench__production-record-warning') &&
    component.includes('data-active-order-production-record-parameter-value') &&
    component.includes(':data-parameter-status="parameter.parameterStatus"'),
  '物料设备参数表必须对超范围提交值标红，并暴露参数状态'
)

assert(
  !/<el-table-column label="设备参数"[\s\S]*formatProductionRecordDeviceParameters\(row\.deviceParameters\)/.test(
    component
  ),
  '生产记录物料主表不能继续保留横向设备参数列'
)

assert(
  /interface TeamLeaderActiveOrderSubmissionDeviceDetailRespVO\s*\{[\s\S]*inMeteringValidityPeriod\?: boolean/.test(
    api
  ),
  '前端设备详情类型必须承载计量状态字段'
)

assert(
  /class SubmissionDeviceDetail[\s\S]*private Boolean inMeteringValidityPeriod;/.test(domain) &&
    controller.includes('.setInMeteringValidityPeriod(device.getInMeteringValidityPeriod())'),
  '后端详情领域模型和响应转换必须透传设备计量状态'
)

assert(
  service.includes('deviceMeteringValidity') &&
    service.includes('addDeviceMeteringValidityFromValue') &&
    service.includes('optionalBooleanValue') &&
    service.includes('.setInMeteringValidityPeriod(inMeteringValidityPeriod)'),
  '后端详情服务必须从正式原始提交 payload 解析 deviceMeteringValidity 到设备行'
)

console.log('PASS: active order production record device parameter table static contract')
