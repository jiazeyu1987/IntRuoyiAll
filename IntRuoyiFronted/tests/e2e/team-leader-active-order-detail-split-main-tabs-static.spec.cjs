const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const detailPanel = read('src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
const api = read('src/api/mes/pro/processpool/teamLeader.ts')
const backendModel = fs.readFileSync(
  path.resolve(root, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'),
  'utf8'
)
const backendVo = fs.readFileSync(
  path.resolve(root, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'),
  'utf8'
)
const backendService = fs.readFileSync(
  path.resolve(root, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'),
  'utf8'
)
const controller = fs.readFileSync(
  path.resolve(root, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'),
  'utf8'
)

assert.match(
  backendModel,
  /class PqcSubmissionDetail[\s\S]*private Long qaProcessId;[\s\S]*private String qaProcessCode;[\s\S]*private String qaProcessName;/,
  '后端详情模型必须输出 PQC 自己的检验工序身份，不能只借生产工序'
)
assert.match(
  backendVo,
  /class PqcSubmissionDetail[\s\S]*private Long qaProcessId;[\s\S]*private String qaProcessCode;[\s\S]*private String qaProcessName;/,
  '详情 VO 必须输出 PQC 自己的检验工序身份'
)
assert.match(
  backendService,
  /MesQaInspectionRegulationProcessMapper[\s\S]*selectBatchIds[\s\S]*MesPqcInspectionTaskDO::getQaProcessId[\s\S]*setQaProcessName\(qaProcess\.getProcessName\(\)\)/,
  '详情服务必须从正式 PQC 规程工序读取 qaProcessName，不能前端推断'
)
assert.match(
  controller,
  /setQaProcessId\(submission\.getQaProcessId\(\)\)[\s\S]*setQaProcessCode\(submission\.getQaProcessCode\(\)\)[\s\S]*setQaProcessName\(submission\.getQaProcessName\(\)\)/,
  'Controller 必须透传 PQC 自有工序字段'
)
assert.match(
  api,
  /export interface TeamLeaderActiveOrderPqcSubmissionDetailRespVO[\s\S]*qaProcessId\?: number[\s\S]*qaProcessCode\?: string[\s\S]*qaProcessName\?: string/,
  '前端 API 类型必须表达 PQC 自有工序字段'
)

assert.match(
  detailPanel,
  /data-team-leader-active-order-detail-main-tabs/,
  '详情面板第一层必须是主 tab'
)
assert.match(
  detailPanel,
  /label="生产提交"[\s\S]*name="productionSubmissions"[\s\S]*data-team-leader-active-order-detail-production-process-tabs/,
  '生产提交主 tab 下必须按生产工序展示'
)
assert.match(
  detailPanel,
  /label="PQC提交"[\s\S]*name="pqcSubmissions"[\s\S]*data-team-leader-active-order-detail-pqc-process-tabs/,
  'PQC提交主 tab 下必须按 PQC 自有工序展示'
)
assert.match(
  detailPanel,
  /pqcProcessGroups/,
  '前端必须构建独立的 PQC 工序分组'
)
assert.match(
  detailPanel,
  /resolveActiveOrderPqcProcessTabName\(pqcProcess/,
  'PQC 工序 tab key 必须来自 PQC 分组，不得复用生产工序 tab key'
)
assert.doesNotMatch(
  detailPanel,
  /activeOrderDetailInnerTabs/,
  '新结构不应再保留每个生产工序下的生产\/PQC 内层 tab'
)
assert.doesNotMatch(
  detailPanel,
  /process\.pqcSubmissions\?\.length[\s\S]*暂无一线PQC提交/,
  'PQC 空状态不能绑定在生产工序对象下面'
)

console.log('team-leader-active-order-detail-split-main-tabs-static.spec.cjs PASS')
