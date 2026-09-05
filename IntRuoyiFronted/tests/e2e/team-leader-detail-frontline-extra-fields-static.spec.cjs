const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const api = fs.readFileSync(path.join(repoRoot, 'src/api/mes/pro/processpool/index.ts'), 'utf8')

assert.match(
  api,
  /export interface ProcessPoolTimelineSelectedDeviceVO[\s\S]*inMeteringValidityPeriod\?: boolean/,
  '时间线选用设备类型必须承载设备是否在计量有效期内。'
)

assert.match(
  page,
  /interface SubmissionMaterialDeviceRow[\s\S]*meteringValidityText: string/,
  '生产组长详情设备行必须暴露计量有效期展示文本。'
)

assert.match(
  page,
  /<el-table-column label="计量状态" prop="meteringValidityText"/,
  '生产组长详情设备表必须在设备行显示计量状态。'
)

assert.match(
  page,
  /const resolveSubmissionDeviceMeteringValidityMap = \([\s\S]*rootPayload\?\.deviceMeteringValidity[\s\S]*deviceId/,
  '生产组长详情必须从一线提交 raw payload 的 deviceMeteringValidity 解析设备有效期。'
)

assert.match(
  page,
  /resolveSubmissionMaterialDeviceRows[\s\S]*meteringValidityMap[\s\S]*inMeteringValidityPeriod/,
  '物料设备行必须按设备 ID 合并计量有效期。'
)

assert.match(
  page,
  /const formatSubmissionDeviceMeteringValidityText = \([\s\S]*计量有效[\s\S]*计量超期[\s\S]*计量状态未记录/,
  '计量有效期展示必须区分有效、超期和未记录。'
)

assert.match(
  page,
  /const formatSubmissionParameterValueText = \([\s\S]*item\.textValue[\s\S]*item\.value/,
  '设备参数显示值必须优先使用 textValue，再回退到 value。'
)

assert.match(
  page,
  /valueText: `\$\{formatSubmissionParameterValueText\(item\)\}\$\{unit\}`/,
  '设备参数列表必须使用选择类参数文本值生成展示。'
)

console.log('PASS: team-leader detail displays frontline extra fields')
