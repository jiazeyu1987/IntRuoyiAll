const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readFrontend = (file) => fs.readFileSync(path.join(root, file), 'utf8')
const readBackend = (file) =>
  fs.readFileSync(path.resolve(root, '../IntRuoyiBackend', file), 'utf8')

const detailPanel = readFrontend(
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const api = readFrontend('src/api/mes/pro/processpool/teamLeader.ts')
const readDO = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesTeamLeaderActiveOrderDetailReadDO.java'
)
const mapperXml = readBackend(
  'yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml'
)
const model = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
)
const service = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'
)
const vo = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
)
const controller = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)

assert.match(
  mapperXml,
  /CAST\(pool_event\.raw_payload AS CHAR\)\s+AS originalPayloadJson/,
  '活跃订单详情必须读取生产报工事件 raw_payload，作为 selectedDevices 正式快照来源'
)
assert.match(
  mapperXml,
  /LEFT JOIN mes_pro_process_pool_team_device event_device[\s\S]*event_device\.id = pool_event\.device_id/,
  '活跃订单详情必须按 event.device_id 关联正式班组设备'
)
assert.match(
  readDO,
  /private String originalPayloadJson;[\s\S]*private Long eventDeviceId;[\s\S]*private String eventDeviceCode;[\s\S]*private String eventDeviceName;/,
  '详情读模型必须承载事件 raw payload 与正式设备主数据'
)
assert.match(
  model,
  /class SubmissionDeviceDetail[\s\S]*private Long deviceId;[\s\S]*private String deviceCode;[\s\S]*private String deviceName;/,
  '领域详情模型必须用设备集合表达一线生产提交设备信息'
)
assert.match(
  model,
  /class SubmissionDetail[\s\S]*private List<SubmissionDeviceDetail> devices = List\.of\(\);/,
  '生产提交详情必须输出设备集合，不能退回单设备字段'
)
assert.match(
  service,
  /\.setDevices\(resolveSubmissionDevices\(row, activeOrderId\)\)/,
  '详情服务必须从正式事件快照解析并填充生产提交设备集合'
)
assert.match(
  service,
  /selectedDevices[\s\S]*deviceParameterReadings[\s\S]*getEventDeviceId\(\)/,
  '详情服务必须兼容 selectedDevices、多设备参数读数和 event.device_id 三类正式设备来源'
)
assert.match(
  vo,
  /class SubmissionDeviceDetail[\s\S]*private Long deviceId;[\s\S]*private String deviceCode;[\s\S]*private String deviceName;/,
  '详情 VO 必须透出生产提交设备集合'
)
assert.match(
  controller,
  /\.setDevices\(submission\.getDevices\(\)\.stream\(\)[\s\S]*toActiveOrderSubmissionDeviceDetailRespVO/,
  'Controller 必须把生产提交设备集合映射给前端'
)
assert.match(
  api,
  /export interface TeamLeaderActiveOrderSubmissionDeviceDetailRespVO[\s\S]*deviceId\?: number[\s\S]*deviceCode\?: string[\s\S]*deviceName\?: string/,
  '前端 API 类型必须声明生产提交设备明细'
)
assert.match(
  api,
  /export interface TeamLeaderActiveOrderSubmissionDetailRespVO[\s\S]*devices: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO\[\]/,
  '生产提交 API 类型必须携带设备集合'
)
assert.match(
  detailPanel,
  /<el-table-column label="设备"[\s\S]*formatActiveOrderSubmissionDevices\(submission\.devices\)/,
  '生产提交详情表必须显示设备列并格式化设备集合'
)

console.log('team-leader-active-order-production-device-detail-static.spec.cjs PASS')
