const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const frontendApi = read('src/api/mes/pro/processpool/teamLeader.ts')
const detailPanel = read('src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
const backendDomain = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
)
const backendVo = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
)
const backendService = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'
)
const controller = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)

assert.match(
  backendDomain,
  /class\s+SubmissionDetail[\s\S]*List<SubmissionMaterialDetail>\s+materials\s*=\s*List\.of\(\)/,
  '后端领域详情的每条生产提交必须包含输出物料明细 materials。'
)
assert.match(
  backendDomain,
  /class\s+SubmissionMaterialDetail[\s\S]*materialCode[\s\S]*materialName[\s\S]*outputQuantity[\s\S]*lossQuantity[\s\S]*List<SubmissionDeviceDetail>\s+devices\s*=\s*List\.of\(\)/,
  '输出物料明细必须包含物料、完成数量、损耗数量和设备列表。'
)
assert.match(
  backendVo,
  /class\s+SubmissionDetail[\s\S]*List<SubmissionMaterialDetail>\s+materials/,
  '后端响应 VO 的每条生产提交必须暴露输出物料明细 materials。'
)
assert.match(
  backendService,
  /setMaterials\(resolveSubmissionMaterials\(row,\s*activeOrderId\)\)/,
  '详情服务必须从正式提交 payload 或物料明细解析输出物料并写入 SubmissionDetail。'
)
assert.match(
  controller,
  /setMaterials\(toSubmissionMaterialDetailRespVOs\(submission\.getMaterials\(\)\)\)/,
  'Controller 必须把输出物料明细映射到前端响应。'
)
assert.match(
  frontendApi,
  /export interface TeamLeaderActiveOrderSubmissionMaterialDetailRespVO[\s\S]*materialCode[\s\S]*materialName[\s\S]*outputQuantity[\s\S]*lossQuantity[\s\S]*devices/,
  '前端 API 类型必须声明生产提交输出物料明细。'
)
assert.match(
  frontendApi,
  /export interface TeamLeaderActiveOrderSubmissionDetailRespVO[\s\S]*materials:\s*TeamLeaderActiveOrderSubmissionMaterialDetailRespVO\[\]/,
  '前端每条生产提交类型必须包含 materials 数组。'
)
assert.match(
  detailPanel,
  /submission\.materials\?\.length[\s\S]*输出物料明细[\s\S]*material\.materialName[\s\S]*material\.materialCode[\s\S]*material\.outputQuantity[\s\S]*material\.lossQuantity[\s\S]*formatActiveOrderSubmissionDevices\(material\.devices\)/,
  '详情页必须在每条生产提交下展示输出物料明细，而不是只显示提交事件行。'
)

console.log('PASS: active-order output material detail display static contract')
