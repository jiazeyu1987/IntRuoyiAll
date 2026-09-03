const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const read = (relativePath) => fs.readFileSync(path.resolve(workspaceRoot, relativePath), 'utf8')

const page = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = read('IntRuoyiFronted/src/api/mes/pro/processpool/index.ts')

assert.match(
  api,
  /export interface ProcessPoolTimelineMaterialDetailVO[\s\S]*materialId\?: number[\s\S]*materialName\?: string[\s\S]*outputQuantity\?: number \| string[\s\S]*lossQuantity\?: number \| string/,
  '生产组长报工事件类型必须声明多物料明细，避免弹框只能读取单一物料或未类型化 raw payload。'
)

assert.match(
  api,
  /materialDetails\?: ProcessPoolTimelineMaterialDetailVO\[\]/,
  '生产组长报工事件必须携带 materialDetails 类型字段。'
)

assert.match(
  page,
  /interface ProductionReportCorrectionMaterialRow[\s\S]*materialId: number[\s\S]*materialName\?: string[\s\S]*outputQuantity: number[\s\S]*lossQuantity: number/,
  '修改弹框必须维护多物料编辑行。'
)

assert.match(
  page,
  /const resolveProductionMaterialRows = \(row: ProcessPoolTimelineEventVO\)[\s\S]*rootPayload\?\.materialDetails[\s\S]*materialName[\s\S]*outputQuantity[\s\S]*lossQuantity/,
  '多物料上下文必须从正式 materialDetails 或原始 payload materialDetails 解析。'
)

assert.match(
  page,
  /data-production-report-correction-materials[\s\S]*v-for="materialRow in correctionForm\.materialDetails"[\s\S]*materialRow\.materialName[\s\S]*v-model="materialRow\.outputQuantity"[\s\S]*v-model="materialRow\.lossQuantity"/,
  '修改弹框必须逐物料显示并允许调整完成数量/损耗数量。'
)

assert.match(
  page,
  /data-team-leader-allocation-material-context[\s\S]*v-for="materialRow in reviewMaterialRows"[\s\S]*materialRow\.materialName[\s\S]*materialRow\.outputQuantity[\s\S]*materialRow\.lossQuantity/,
  '分配弹框必须显示当前报工的多物料上下文。'
)

assert.match(
  page,
  /data-production-report-correction-devices[\s\S]*v-for="item in correctionDeviceItems"/,
  '修改弹框必须显示多设备上下文。'
)

assert.match(
  page,
  /data-team-leader-allocation-devices[\s\S]*v-for="item in reviewDeviceItems"/,
  '分配弹框必须显示设备上下文。'
)

assert.match(
  page,
  /data-team-leader-allocation-parameters[\s\S]*v-for="item in reviewParameterItems"/,
  '分配弹框必须显示设备参数上下文。'
)

assert.match(
  page,
  /data-production-report-correction-parameters[\s\S]*v-for="parameterRow in correctionForm\.deviceParameterReadings"/,
  '修改弹框必须为真实 E2E 暴露设备参数区域，并逐设备参数展示。'
)

assert.match(
  page,
  /materialDetails: correctionForm\.materialDetails[\s\S]*materialId:[\s\S]*outputQuantity:[\s\S]*lossQuantity:/,
  '修改提交载荷必须保留逐物料完成数量和损耗数量。'
)

assert.match(
  page,
  /deviceId: requirePositiveNumber\(item\.deviceId, '设备参数所属设备不能为空'\)[\s\S]*parameterCode: item\.parameterCode/,
  '设备参数提交必须继续携带设备 ID 与参数编码，不能因展示多设备而丢失身份。'
)

console.log('PASS: team-leader-multi-material-device-dialogs-static')
