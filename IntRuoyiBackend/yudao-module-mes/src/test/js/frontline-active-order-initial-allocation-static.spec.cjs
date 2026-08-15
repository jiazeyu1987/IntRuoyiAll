const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const moduleRoot = path.resolve(__dirname, '../../..')
const readSource = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const contextVo = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/' +
    'MesProFrontlineProcessPoolContextReqVO.java'
)
const submitService = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/' +
    'MesProFrontlineFeedbackSubmitServiceImpl.java'
)
const allocationService = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/' +
    'MesReportAllocationCommandService.java'
)
const completionService = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/' +
    'MesTeamLeaderOrderProcessCompletionService.java'
)
const snapshotLine = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/' +
    'MesReportAllocationSnapshotLine.java'
)
const timelineAllocationRead = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/' +
    'ProcessPoolTimelineReportAllocationReadDO.java'
)
const timelineEventVo = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/' +
    'ProcessPoolTimelineEventRespVO.java'
)
const timelineService = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/' +
    'ProcessPoolTimelineServiceImpl.java'
)
const timelineMapper = readSource(
  'src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
)
const snapshotRespVo = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/' +
    'MesTeamLeaderReportAllocationSnapshotRespVO.java'
)
const previewRespVo = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/' +
    'MesTeamLeaderReportAllocationPreviewRespVO.java'
)
const teamLeaderController = readSource(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/' +
    'MesProcessPoolTeamLeaderController.java'
)
const initialAllocationMigration = readSource(
  '../sql/mysql/20260814_mes_frontline_selected_initial_allocation.sql'
)

test('P1-AC1 backend submit contract requires activeOrderId', () => {
  assert.match(
    contextVo,
    /@NotNull\(message\s*=\s*"请选择活跃订单后再提交"\)[\s\S]{0,240}private\s+Long\s+activeOrderId\s*;/,
    '一线提交后端上下文必须把 activeOrderId 作为必填字段，不能只接收 workOrderId。'
  )
})

test('P1-AC2 selected order is validated by exact activeOrderId', () => {
  assert.match(
    submitService,
    /authorizeActiveOrder\(loginUserId,\s*context\.getActiveOrderId\(\),\s*context\.getWorkOrderId\(\),/,
    '活跃订单授权必须以精确 activeOrderId 为主身份，并同时核对工单上下文。'
  )
})

test('P1-AC3 transactional submit persists full initial allocation before success', () => {
  assert.match(
    submitService,
    /@Transactional\(rollbackFor\s*=\s*Exception\.class\)[\s\S]{0,200}submit\(/,
    '一线正式提交必须保留事务边界。'
  )
  const eventCreation = submitService.indexOf('createSubmitEvent(eventPayload)')
  const initialAllocation = submitService.indexOf('createInitialAllocation(', eventCreation)
  const successResponse = submitService.indexOf('return new MesProFrontlineFeedbackSubmitRespVO()', eventCreation)
  assert.ok(
    eventCreation >= 0 && initialAllocation > eventCreation && initialAllocation < successResponse,
    '报工事件创建后、返回成功前，必须调用正式初始分配能力。'
  )
  const allocationCall = submitService.slice(initialAllocation, initialAllocation + 600)
  assert.match(allocationCall, /context\.getActiveOrderId\(\)/, '初始分配必须使用一线所选 activeOrderId。')
  assert.match(allocationCall, /getOutputQuantity\(\)/, '初始分配必须保存完整 outputQuantity。')
})

test('P1-AC4 allocation path does not silently cap an over-capacity request', () => {
  assert.doesNotMatch(
    allocationService,
    /entry\.getValue\(\)\.min\(remaining\)\.min\(remainingPool\)/,
    '超过订单工序剩余量时不得用 min 静默截断用户提交数量。'
  )
})

test('P1-AC4 adjustable overage is preserved while formal schedule progress stays capped', () => {
  assert.match(
    completionService,
    /reconcileAffectedAllocations\(event,\s*affectedAllocations,\s*false,\s*true\)/,
    '一线初始分配和组长调整后的对账必须进入可调整超量路径。'
  )
  assert.match(
    completionService,
    /!allowAdjustableOverage\s*&&\s*confirmedQuantity\.compareTo\(target\.plannedQuantity\(\)\)\s*>\s*0/,
    '只有正式确认路径可以按订单目标阻塞，待调整分配不得因超量回滚提交。'
  )
  assert.match(
    completionService,
    /scheduleProgressQuantity\s*=\s*allowAdjustableOverage[\s\S]{0,160}confirmedQuantity\.min\(target\.plannedQuantity\(\)\)/,
    '待调整超量必须完整保存在分配事实中，同时排产进度最多推进到正式目标。'
  )
})

test('P1-AC4 initial allocation schema permits completion state before leader review', () => {
  assert.match(
    initialAllocationMigration,
    /MODIFY COLUMN `review_id` bigint DEFAULT NULL/,
    '一线初始分配记录在组长复核前必须允许 review_id 为空。'
  )
  assert.match(
    initialAllocationMigration,
    /MODIFY COLUMN `last_review_id` bigint DEFAULT NULL/,
    '一线初始分配形成订单工序完成状态时，last_review_id 必须允许为空。'
  )
  assert.match(
    initialAllocationMigration,
    /table_name = 'mes_pro_process_pool_order_process_completion'[\s\S]{0,280}column_name = 'last_review_id'[\s\S]{0,160}is_nullable = 'YES'/,
    '迁移必须对完成状态表的 last_review_id 可空性执行正式后置校验。'
  )
})

test('P1-AC5 current allocation exposes order-level overage state', () => {
  assert.match(snapshotLine, /private\s+BigDecimal\s+overageQuantity\s*;/,
    '当前分配行必须暴露订单级 overageQuantity。')
  assert.match(snapshotLine, /private\s+Boolean\s+needsAdjustment\s*;/,
    '当前分配行必须暴露 needsAdjustment，供组长列表红色标识。')
  for (const [source, label] of [
    [snapshotRespVo, '当前分配接口'],
    [previewRespVo, 'FIFO 预览接口']
  ]) {
    assert.match(source, /class\s+Line[\s\S]{0,900}BigDecimal\s+overageQuantity\s*;/,
      `${label}必须把正式 overageQuantity 返回前端。`)
    assert.match(source, /class\s+Line[\s\S]{0,1000}Boolean\s+needsAdjustment\s*;/,
      `${label}必须把正式 needsAdjustment 返回前端。`)
  }
  assert.match(
    teamLeaderController,
    /setOverageQuantity\(line\.getOverageQuantity\(\)\)[\s\S]{0,120}setNeedsAdjustment\(line\.getNeedsAdjustment\(\)\)/,
    '组长当前分配和 FIFO 预览的控制器映射必须保留正式超量状态。'
  )
})

test('P2-AC7 report-management list projects persisted order-level overage state', () => {
  assert.match(timelineAllocationRead, /private\s+BigDecimal\s+overageQuantity\s*;/,
    '报工管理列表读模型必须携带订单级 overageQuantity。')
  assert.match(timelineAllocationRead, /private\s+Boolean\s+needsAdjustment\s*;/,
    '报工管理列表读模型必须携带 needsAdjustment。')
  assert.match(timelineEventVo, /class\s+ReportAllocationRespVO[\s\S]{0,800}BigDecimal\s+overageQuantity/,
    '报工管理列表响应的分配行必须返回 overageQuantity。')
  assert.match(timelineService,
    /setOverageQuantity\((?:line\.getOverageQuantity\(\)|requireAllocationOverage\(line\))\)/,
    '报工管理列表服务必须映射正式超量值。')
  assert.match(timelineMapper, /planned_quantity_snapshot/,
    '报工管理列表 SQL 必须以逐工序正式计划数量快照计算超量。')
  assert.match(timelineMapper, /AS\s+overageQuantity/i,
    '报工管理列表 SQL 必须投影订单级超量数量。')
})
