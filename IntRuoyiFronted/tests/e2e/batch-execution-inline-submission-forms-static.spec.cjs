const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const backendRoot = path.join(repoRoot, 'IntRuoyiBackend')

const read = (file) => fs.readFileSync(file, 'utf8')

const batchDetail = read(
  path.join(frontendRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
)
const activeOrderDetailPage = read(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue')
)
const activeOrderPanel = read(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
)
const frontendApi = read(path.join(frontendRoot, 'src/api/mes/pro/edhr/batchExecution.ts'))
const teamLeaderApi = read(path.join(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts'))
const backendResp = read(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionRespVO.java'
  )
)
const backendActiveOrderDetailResp = read(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
  )
)
const backendTeamLeaderController = read(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
  )
)
const backendService = read(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
  )
)
const backendPqcTaskMapper = read(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcInspectionTaskMapper.java'
  )
)
const backendActiveOrderService = read(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
  )
)
const backendStage1SimulationService = read(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/MesStage1ActiveOrderCompleteSimulationServiceImpl.java'
  )
)
const backendStage2_5SimulationService = read(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage2_5/MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java'
  )
)

assert(
  backendResp.includes('private Long activeOrderId;'),
  '批次执行详情响应必须返回正式来源 activeOrderId'
)
assert(
  backendService.includes('MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper'),
  '批次执行服务必须通过来源关系表解析 activeOrderId'
)
assert(
  backendService.includes('selectSuccessfulListByBatchExecutionIdAndOperation(batchExecutionId, "OPEN")') &&
    backendService.includes('activeOrderIdFromProvisionAudit'),
  '来源关系尚未捕获完成时，批次执行详情只能从同批次成功 OPEN 审计读取正式 activeOrderId'
)
assert(
  backendStage2_5SimulationService.includes('selectExistingBatchBeforeCompletion') &&
    backendStage2_5SimulationService.includes('batchExecutionMapper.selectByContext(') &&
    backendStage2_5SimulationService.includes('existingBatchResult(validated, existingBatch, cleanedRunId)'),
  'Stage2.5 打开批次表单前必须先按当前活跃订单正式上下文复用既有批次，不能二次完工导致幂等冲突'
)
assert(
  /private EdhrBatchExecutionRespVO toResp\(MesProEdhrBatchExecutionDO batch\)[\s\S]*?return new EdhrBatchExecutionRespVO\(\)[\s\S]*?\.setActiveOrderId\(resolveBatchActiveOrderId\(latest\.getId\(\)\)\)[\s\S]*?\.setBatchCode\(latest\.getBatchCode\(\)\)/.test(
    backendService
  ),
  '普通批次执行详情响应必须从正式来源关系设置 activeOrderId'
)
assert(
  frontendApi.includes('activeOrderId?: number'),
  '前端批次执行类型必须包含 activeOrderId'
)
assert(
  batchDetail.includes('ActiveOrderSubmissionDetailPanel'),
  '批次执行详情页必须复用活跃订单提交详情面板'
)
assert(
  batchDetail.includes('getTeamLeaderActiveOrderDetail'),
  '批次执行详情页必须按 activeOrderId 加载活跃订单提交详情'
)
assert(
  batchDetail.includes("task.formSlotType === 'MAIN'") && batchDetail.includes("return 'production'"),
  'MAIN 槽位必须进入生产表单内嵌模式'
)
assert(
  batchDetail.includes("task.formSlotType === 'PROCESS_INSPECTION'") &&
    batchDetail.includes("return 'pqc'"),
  'PROCESS_INSPECTION 槽位必须进入 PQC 表单内嵌模式'
)
assert(
  batchDetail.includes('批次执行缺少正式活跃订单来源，无法展示一线提交表单'),
  '缺少 activeOrderId 时必须显式报错，不能按工单号推断'
)
assert(
  !batchDetail.includes('getTeamLeaderActiveOrderList'),
  '批次执行详情页不得通过活跃订单列表按工单号猜测来源'
)
assert(
  activeOrderPanel.includes("displayMode?: 'full' | 'production' | 'pqc'"),
  '活跃订单提交面板必须支持按生产/PQC 表单单独嵌入'
)
assert(
  activeOrderPanel.includes('visibleProductionProcesses'),
  '生产表单嵌入必须能按当前批次工序过滤'
)
assert(
  backendActiveOrderDetailResp.includes('private Integer version;') &&
    backendTeamLeaderController.includes('.setVersion(detail.getVersion())'),
  '活跃订单详情必须透传 version，供 Stage2.5 生成批次执行时做 expectedVersion 校验'
)
assert(
  teamLeaderApi.includes('version: number') && teamLeaderApi.includes('simulateStage2_5BackfillBatchExecution'),
  '前端活跃订单详情类型必须包含 version，并复用 Stage2.5 生成或打开真实批次执行'
)
assert(
  activeOrderPanel.includes('open-batch-execution-production-form') &&
    activeOrderPanel.includes('open-batch-execution-pqc-form'),
  '活跃订单提交详情必须暴露打开批次执行生产表单和过程检验记录的事件'
)
assert(
  activeOrderDetailPage.includes('simulateStage2_5BackfillBatchExecution') &&
    activeOrderDetailPage.includes('handleOpenBatchExecutionSubmissionForm'),
  '活跃订单提交详情页必须点击按钮后先生成或打开真实批次执行'
)
assert(
  activeOrderDetailPage.includes('batchExecutionOpenResultCache') &&
    activeOrderDetailPage.includes('resolveBatchExecutionOpenResult') &&
    activeOrderDetailPage.includes('batchExecutionOpenResultCache.set(currentDetail.activeOrderId, result)'),
  '同一活跃订单先打开生产表单后再打开 PQC 表单时，必须复用已生成批次执行结果，不能重复创建 Stage2.5'
)
assert(
  activeOrderDetailPage.includes("formSlotType: mode === 'production' ? 'MAIN' : 'PROCESS_INSPECTION'") &&
    activeOrderDetailPage.includes('routeProcessId'),
  '跳转批次执行详情必须携带正式槽位类型和 routeProcessId，不按工序名称定位'
)
assert(
  batchDetail.includes('route.query.formSlotType') &&
    batchDetail.includes('route.query.routeProcessId'),
  '批次执行详情必须支持通过 formSlotType 和 routeProcessId 选中对应表单槽位'
)
assert(
  backendPqcTaskMapper.includes('String inspectionType,') &&
    backendPqcTaskMapper.includes('String shiftCode,') &&
    backendPqcTaskMapper.includes('Integer roundNo') &&
    backendPqcTaskMapper.includes('MesPqcInspectionTaskDO::getInspectionType') &&
    backendPqcTaskMapper.includes('MesPqcInspectionTaskDO::getShiftCode') &&
    backendPqcTaskMapper.includes('MesPqcInspectionTaskDO::getRoundNo'),
  'PQC 任务查重身份必须包含检验类型、班次和轮次，上午/下午巡检不能互相误判重复'
)
assert(
  backendActiveOrderService.includes('task.getInspectionType(), task.getBusinessDate(), task.getShiftCode(), task.getRoundNo()'),
  '生成 PQC 任务时必须按完整身份查重，不能只按 qaItemCode/ruleKey/businessDate 查重'
)
assert(
  teamLeaderApi.includes('STAGE1_SIMULATION_REQUEST_TIMEOUT') &&
    teamLeaderApi.includes('timeout: STAGE1_SIMULATION_REQUEST_TIMEOUT'),
  'Stage1 模拟会批量生成生产/PQC/签名数据，前端请求必须使用专用长超时，不能沿用默认 30 秒'
)
assert(
  backendStage1SimulationService.includes('activeOrderMapper.updateSimulationMetadata(') &&
    backendStage1SimulationService.includes('templateActiveOrder.setSimulationStage(STAGE)'),
  'Stage1 模拟完成并持久化 100% 后，必须把当前活跃订单自身标记为 STAGE1，供 Stage2.5 继续生成批次执行'
)

console.log('batch-execution-inline-submission-forms static contract passed')
