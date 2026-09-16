const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const detailModel = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
)
const detailVo = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
)
const detailService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'
)
const controller = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)

assert.match(
  detailModel,
  /private List<InputMaterialDetail> inputMaterialUsages\s*=\s*List\.of\(\);/,
  '活跃订单详情模型必须提供订单级输入物料用量来源，避免无工序输入配置时生产用料清单实际用量为空。'
)
assert.match(
  detailVo,
  /private List<InputMaterialDetail> inputMaterialUsages;/,
  '活跃订单详情响应 VO 必须暴露订单级输入物料用量来源。'
)
assert.match(
  controller,
  /setInputMaterialUsages\(detail\.getInputMaterialUsages\(\)\.stream\(\)[\s\S]*toActiveOrderInputMaterialDetailRespVO/,
  'Controller 必须把订单级输入物料用量映射到前端响应。'
)
assert.match(
  detailService,
  /resolveInputMaterialUsages\(inputSourceSnapshot\)/,
  '详情服务必须从当前活跃订单的完工回填或绑定领料来源解析订单级输入物料用量。'
)
assert.match(
  detailService,
  /class InputMaterialUsageAccumulator[\s\S]*actualQuantity\.compareTo\(this\.actualQuantity\)\s*>\s*0/,
  '订单级输入物料用量重复物料必须按 actualQuantity 保留最大值。'
)
assert.doesNotMatch(
  detailService,
  /getRequiredQuantity\(\)[\s\S]*setActualQuantity|setActualQuantity[\s\S]*getRequiredQuantity\(\)/,
  '生产用料清单实际用量不得由应发数量冒充。'
)

console.log('PASS mes-active-order-input-material-usage-static')
