const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const read = (relativePath) => fs.readFileSync(path.resolve(workspaceRoot, relativePath), 'utf8')

const page = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = read('IntRuoyiFronted/src/api/mes/pro/processpool/index.ts')
const itemApi = read('IntRuoyiFronted/src/api/mes/md/item/index.ts')

assert.match(
  page,
  /import \{ MdItemApi \} from '@\/api\/mes\/md\/item'/,
  '修改报工弹框必须使用物料主数据接口解析真实物料名称。'
)

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
  /<el-tabs[\s\S]*data-production-report-correction-material-tabs[\s\S]*v-for="materialRow in correctionForm\.materialDetails"[\s\S]*:label="formatCorrectionMaterialTabLabel\(materialRow\)"[\s\S]*v-model="materialRow\.outputQuantity"/,
  '修改弹框必须按提交物料生成物料 tab，并在物料 tab 内允许调整完成数量。'
)

assert.match(
  page,
  /data-team-leader-allocation-material-context[\s\S]*v-for="materialRow in reviewMaterialRows"[\s\S]*materialRow\.materialName[\s\S]*materialRow\.outputQuantity[\s\S]*materialRow\.lossQuantity/,
  '分配弹框必须显示当前报工的多物料上下文。'
)

assert.match(
  page,
  /data-production-report-correction-devices[\s\S]*<el-tabs[\s\S]*v-for="deviceRow in resolveCorrectionMaterialDevices\(materialRow\)"[\s\S]*:label="formatCorrectionDeviceTabLabel\(deviceRow\)"/,
  '修改弹框必须在物料 tab 内按设备编号生成设备 tab。'
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
  /data-production-report-correction-parameters[\s\S]*v-for="parameterRow in resolveCorrectionDeviceParameters\(materialRow, deviceRow\)"/,
  '修改弹框必须在设备 tab 下只展示该设备对应的参数。'
)

assert.match(
  page,
  /team-leader-workbench__correction-field-row[\s\S]*team-leader-workbench__correction-field-label[\s\S]*team-leader-workbench__correction-field-control/,
  '修改弹框的 label 和控件必须使用统一两列行布局。'
)

assert.match(
  page,
  /<el-select[\s\S]*v-if="isCorrectionSelectParameter\(parameterRow\)"[\s\S]*v-model="parameterRow\.value"[\s\S]*v-for="option in resolveCorrectionParameterOptions\(parameterRow\)"/,
  '选择类设备参数必须使用下拉框，并读取参数快照里的正式选项。'
)

assert.match(
  page,
  /v-else[\s\S]*<el-input-number[\s\S]*v-model="parameterRow\.value"/,
  '非选择类设备参数必须继续使用数值输入框。'
)

assert.match(
  page,
  /isCorrectionSelectParameter\(parameterRow\)[\s\S]*return value \? \{ textValue: value \} : undefined[\s\S]*return value === undefined \? undefined : \{ value \}/,
  '修改弹框提交时选择类参数必须提交 textValue，数值类参数必须提交 value。'
)

assert.doesNotMatch(
  page,
  />校验信息</,
  '修改报工内容弹框不应再显示校验信息区。'
)

assert.doesNotMatch(
  page,
  />变更预览</,
  '修改报工内容弹框不应再显示变更预览区。'
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

assert.match(
  page,
  /\.team-leader-workbench__correction-field-row\s*\{[^}]*grid-template-columns:\s*128px minmax\(180px,\s*220px\)/,
  '物料 tab 内必须为 label 和控件保留稳定列宽。'
)

assert.match(
  page,
  /resolveCorrectionMaterialNames[\s\S]*MdItemApi\.getItem\(item\.materialId\)[\s\S]*material\?\.name/,
  '修改报工弹框必须按物料编号读取真实物料名称，不能展示“物料 1”等占位名称。'
)

assert.match(
  page,
  /\.team-leader-workbench__correction-material-tabs,[\s\S]*\.team-leader-workbench__correction-device-tabs\s*\{[^}]*min-width:\s*0/,
  '物料明细和设备明细必须使用可收缩的 tab 容器，避免长物料名或设备号挤破弹框。'
)

assert.match(
  page,
  /resolveCorrectionMaterialLossTotal\(materialRow\)[\s\S]*resolveCorrectionMaterialTotalQuantity\(materialRow\)[\s\S]*resolveCorrectionMaterialDevices\(materialRow\)[\s\S]*resolveCorrectionDeviceParameters\(materialRow, deviceRow\)/,
  '每个物料 tab 必须展示损耗合计、总数量、自己的设备和设备参数。'
)

assert.match(
  page,
  /lossDetails: item\.lossDetails[\s\S]*selectedDevices: item\.selectedDevices[\s\S]*deviceParameterReadings: item\.deviceParameterReadings/,
  '修改提交载荷必须按物料携带损耗原因、多设备和设备参数。'
)

assert.match(
  page,
  /const materialLossDetails = hasMaterialLossDetails[\s\S]*correctionForm\.materialDetails\.flatMap\(\(item\) => item\.lossDetails\)/,
  '修改提交必须从物料级损耗明细生成总损耗原因字段。'
)

assert.match(
  page,
  /const materialParameterReadings = hasMaterialParameters[\s\S]*correctionForm\.materialDetails\.flatMap\(\(item\) => item\.deviceParameterReadings\)/,
  '修改提交必须从物料级设备参数生成总设备参数字段。'
)

assert.match(itemApi, /getItem: async \(id: number\)/)

console.log('PASS: team-leader-multi-material-device-dialogs-static')
