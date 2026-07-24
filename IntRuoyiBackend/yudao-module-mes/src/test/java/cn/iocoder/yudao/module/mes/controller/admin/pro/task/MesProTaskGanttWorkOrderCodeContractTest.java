package cn.iocoder.yudao.module.mes.controller.admin.pro.task;

import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttDataRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProTaskGanttWorkOrderCodeContractTest {

    private static final Path PROJECT_DIR = Path.of("").toAbsolutePath();

    @Test
    void ganttRespVo_shouldExposeWorkOrderCodeForFrontendIdentity() throws Exception {
        Field field = GanttDataRespVO.class.getDeclaredField("workOrderCode");
        Schema schema = field.getAnnotation(Schema.class);

        assertEquals("生产工单编码", schema.description());
        assertEquals("MO-GANTT-001", new GanttDataRespVO().setWorkOrderCode("MO-GANTT-001").getWorkOrderCode());
    }

    @Test
    void ganttRespVo_shouldExposeScheduleOrderProcessIdForScheduledOnlyContract() throws Exception {
        Field field = GanttDataRespVO.class.getDeclaredField("scheduleOrderProcessId");
        Schema schema = field.getAnnotation(Schema.class);

        assertEquals("排产工序 ID", schema.description());
        assertEquals(9001L, new GanttDataRespVO().setScheduleOrderProcessId(9001L).getScheduleOrderProcessId());
    }

    @Test
    void ganttRespVo_shouldExposeScheduleOrderIdForLatestReplanScopeContract() throws Exception {
        Field field = GanttDataRespVO.class.getDeclaredField("scheduleOrderId");
        Schema schema = field.getAnnotation(Schema.class);

        assertEquals("排产工单 ID", schema.description());
        assertEquals(8001L, new GanttDataRespVO().setScheduleOrderId(8001L).getScheduleOrderId());
    }

    @Test
    void currentAndPreviewGanttPaths_shouldPopulateWorkOrderCodeFromWorkOrder() throws Exception {
        String controller = readSource("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskController.java");
        String currentGanttMethod = between(
                controller,
                "public CommonResult<List<GanttDataRespVO>> listGanttTaskList",
                "private List<MesProTaskRespVO> buildTaskRespVOList");

        assertEquals(2, count(currentGanttMethod, ".setWorkOrderCode(workOrder.getCode())"),
                "当前排产甘特图的工单项目行和任务行都必须使用正式工单编码");

        String autoScheduleService = readSource(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java");
        String previewBuildMethod = between(
                autoScheduleService,
                "private List<GanttDataRespVO> buildPreviewTasks",
                "private List<GanttLinkRespVO> buildPreviewLinks");

        assertTrue(previewBuildMethod.contains(".setWorkOrderCode(workOrder.getCode())"),
                "自动排产预览的工单项目行必须使用正式工单编码");
        assertTrue(previewBuildMethod.contains("workOrder.getCode()))"),
                "自动排产预览任务行必须从对应工单传递正式工单编码");
        assertTrue(autoScheduleService.contains("String workOrderCode"),
                "自动排产预览任务行转换方法必须接收对应工单编码");
        assertTrue(autoScheduleService.contains(".setWorkOrderCode(workOrderCode)"),
                "自动排产预览任务行 VO 必须继承对应工单编码");
    }

    @Test
    void currentGanttPath_shouldResolveProcessFromScheduleSnapshotAndSkipLegacyNonProcessRows() throws Exception {
        String controller = readSource("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskController.java");
        String currentGanttMethod = between(
                controller,
                "public CommonResult<List<GanttDataRespVO>> listGanttTaskList",
                "private List<MesProTaskRespVO> buildTaskRespVOList");

        assertTrue(currentGanttMethod.contains("scheduleOrderProcessMapper.selectListByIds("),
                "当前排产甘特图必须按任务排产扩展批量加载冻结的排产工序快照");
        assertTrue(currentGanttMethod.contains("MesProTaskScheduleExtDO::getScheduleOrderProcessId"),
                "当前排产甘特图必须从任务排产扩展提取 scheduleOrderProcessId");
        assertTrue(currentGanttMethod.contains("GanttTaskRange workOrderRange = resolveWorkOrderGanttRange(woTasks);"),
                "工单 project 行必须只聚合参与排产任务时间");
        assertTrue(currentGanttMethod.contains(".setStartDate(workOrderRange.startDate()).setEndDate(workOrderRange.endDate())"),
                "工单 project 行必须输出聚合后的起止时间");
        assertTrue(currentGanttMethod.contains("if (!isScheduledGanttProcessTask(taskExt, activeScheduleOrderMap))"),
                "未参与排产的任务不能作为当前排产工序 task 行输出");
        assertTrue(currentGanttMethod.contains(".setProcess(resolveGanttTaskProcessName(task, taskExt, scheduleOrderProcessMap))"),
                "工序 task 行必须通过统一解析器输出真实工序名称");
        assertTrue(controller.contains("Map<Long, MesProScheduleOrderDO> activeScheduleOrderMap)"),
                "参与排产任务过滤必须集中在命名清晰的 helper 中");
        assertTrue(controller.contains("private String resolveGanttTaskProcessName("),
                "工序名解析必须集中在命名清晰的 helper 中");
        assertTrue(controller.contains("生产排产甘特图任务引用不存在的排产工序快照"),
                "缺少排产工序快照时必须 fail fast，不能返回空工序给前端");
        assertTrue(controller.contains("生产排产甘特图任务缺少工序名称"),
                "缺少工序名称时必须 fail fast，不能返回空工序给前端");

        String mapper = readSource(
                "src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/scheduleorder/MesProScheduleOrderProcessMapper.java");
        assertTrue(mapper.contains("default List<MesProScheduleOrderProcessDO> selectListByIds(Collection<Long> ids)"),
                "排产工序 mapper 必须提供按快照 ID 批量读取方法");
        assertTrue(mapper.contains(".in(MesProScheduleOrderProcessDO::getId, ids)"),
                "排产工序快照批量读取必须按 ID 精确查询");
    }

    @Test
    void currentGanttPath_shouldOnlyReturnScheduledTasksAndTheirWorkOrders() throws Exception {
        String controller = readSource("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskController.java");
        String currentGanttMethod = between(
                controller,
                "public CommonResult<List<GanttDataRespVO>> listGanttTaskList",
                "private List<MesProTaskRespVO> buildTaskRespVOList");

        assertTrue(currentGanttMethod.contains("List<MesProTaskDO> scheduledTasks = allTasks.stream()"),
                "当前排产甘特图必须先收口为参与排产的任务集合");
        assertTrue(currentGanttMethod.contains("isScheduledGanttProcessTask(taskExtMap.get(task.getId()), activeScheduleOrderMap)"),
                "参与排产口径必须来自任务排产扩展快照，不能来自普通工序 ID");
        assertTrue(currentGanttMethod.contains("Map<Long, List<MesProTaskDO>> scheduledTaskMap"),
                "工单 project 行必须基于参与排产任务分组生成");
        assertTrue(currentGanttMethod.contains("List<MesProWorkOrderDO> scheduledWorkOrders = workOrders.stream()"),
                "未参与排产的工单必须在组装甘特图前过滤");
        assertTrue(currentGanttMethod.contains("if (CollUtil.isEmpty(woTasks))"),
                "没有参与排产任务的工单必须跳过，不得输出空 project 行");
        assertTrue(currentGanttMethod.contains(".setScheduleOrderProcessId(taskExt.getScheduleOrderProcessId())"),
                "任务行必须暴露排产工序 ID 供前端和 E2E 验证参与排产口径");
        assertTrue(controller.contains("Map<Long, MesProScheduleOrderDO> activeScheduleOrderMap)"),
                "参与排产判断必须集中在命名清晰的 helper 中");
        assertTrue(controller.contains("taskExt.getScheduleOrderId() != null")
                        && controller.contains("activeScheduleOrderMap.containsKey(taskExt.getScheduleOrderId())"),
                "参与排产任务必须同时有排产扩展、排产工序 ID，并归属当前有效排产工单");
        assertTrue(!controller.contains("|| task.getProcessId() != null && task.getProcessId() > 0"),
                "当前排产甘特图不得把仅有普通 processId 的历史手工任务当作参与排产");
        assertTrue(!currentGanttMethod.contains("taskExt != null ? taskExt.getScheduleSource() : \"MANUAL\""),
                "当前排产甘特图不得用默认 MANUAL 兜底伪装未参与排产任务");
    }

    @Test
    void currentGanttPath_shouldOnlyReturnCurrentScheduleOrderWorkOrders() throws Exception {
        String controller = readSource("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskController.java");
        String currentGanttMethod = between(
                controller,
                "public CommonResult<List<GanttDataRespVO>> listGanttTaskList",
                "private List<MesProTaskRespVO> buildTaskRespVOList");

        assertTrue(controller.contains("import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;"),
                "当前排产甘特图必须读取排产工单主表来确认当前排产工单集合");
        assertTrue(controller.contains("import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;"),
                "当前排产甘特图必须注入排产工单 mapper");
        assertTrue(controller.contains("import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;"),
                "当前排产甘特图必须复用排产工单状态枚举，而不是硬编码状态");
        assertTrue(controller.contains("private MesProScheduleOrderMapper scheduleOrderMapper;"),
                "当前排产甘特图必须具备排产工单主表查询依赖");
        assertTrue(currentGanttMethod.contains("Map<Long, MesProScheduleOrderDO> baseActiveScheduleOrderMap = convertMap("),
                "当前排产甘特图必须先构造当前有效排产工单集合");
        assertTrue(currentGanttMethod.contains("scheduleOrderMapper.selectListByIds("),
                "当前排产甘特图必须按任务扩展中的 scheduleOrderId 批量读取排产工单");
        assertTrue(currentGanttMethod.contains("MesProTaskScheduleExtDO::getScheduleOrderId"),
                "当前排产甘特图必须从任务排产扩展提取 scheduleOrderId");
        assertTrue(currentGanttMethod.contains(".filter(this::isCurrentGanttScheduleOrder)"),
                "当前排产甘特图必须复用当前有效排产工单判断");
        assertTrue(currentGanttMethod.contains("isScheduledGanttProcessTask(taskExtMap.get(task.getId()), activeScheduleOrderMap)"),
                "任务参与排产判断必须校验当前有效排产工单集合");
        assertTrue(controller.contains("private boolean isCurrentGanttScheduleOrder(MesProScheduleOrderDO scheduleOrder)"),
                "当前有效排产工单判断必须集中在命名清晰的 helper 中");
        assertTrue(controller.contains("MesProScheduleOrderStatusEnum.FINISHED.getStatus()")
                        && controller.contains("MesProScheduleOrderStatusEnum.CANCELED.getStatus()"),
                "当前排产甘特图必须排除已完成和已取消的历史排产工单");
        assertTrue(controller.contains("scheduleOrderProcess.getScheduleOrderId(), taskExt.getScheduleOrderId()"),
                "排产工序快照必须与任务归属的当前排产工单一致");
        assertTrue(controller.contains("生产排产甘特图任务排产工单和工序快照不一致"),
                "排产工单/工序快照不一致时必须 fail fast，不能把历史工序混入当前甘特图");
    }

    @Test
    void currentGanttPath_shouldUseLatestAppliedReplanScope() throws Exception {
        String controller = readSource("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskController.java");
        String currentGanttMethod = between(
                controller,
                "public CommonResult<List<GanttDataRespVO>> listGanttTaskList",
                "private List<MesProTaskRespVO> buildTaskRespVOList");

        assertTrue(controller.contains("import cn.iocoder.yudao.framework.common.util.json.JsonUtils;"),
                "当前甘特图必须能解析最近一次成功重排说明快照");
        assertTrue(controller.contains("import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProReplanExplanationRespVO;"),
                "当前甘特图必须复用重排说明 VO 中的排产工单范围");
        assertTrue(controller.contains("private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;"),
                "当前甘特图必须注入最近一次成功重排说明快照 mapper");
        assertTrue(currentGanttMethod.contains("Set<Long> latestAppliedReplanScheduleOrderIds = resolveLatestAppliedReplanScheduleOrderIds();"),
                "当前甘特图必须先解析最新一次成功重排的排产工单范围");
        assertTrue(currentGanttMethod.contains("Map<Long, MesProScheduleOrderDO> activeScheduleOrderMap = CollUtil.isEmpty(latestAppliedReplanScheduleOrderIds)"),
                "当前甘特图必须构造最终不可重新赋值的当前排产工单作用域");
        assertTrue(currentGanttMethod.contains(".filter(entry -> latestAppliedReplanScheduleOrderIds.contains(entry.getKey()))"),
                "当前有效排产工单集合必须被最新重排范围收窄");
        assertTrue(currentGanttMethod.contains(".setScheduleOrderId(resolveWorkOrderScheduleOrderId(woTasks, taskExtMap))"),
                "project 行必须暴露其来自的排产工单 ID");
        assertTrue(currentGanttMethod.contains(".setScheduleOrderId(taskExt.getScheduleOrderId())"),
                "task 行必须暴露其来自的排产工单 ID");
        assertTrue(controller.contains("replanExplanationSnapshotMapper.selectLatest()"),
                "最新重排范围必须来自持久化的成功重排快照，刷新页面后仍然生效");
        assertTrue(controller.contains("MesProReplanExplanationRespVO.OrderItem::getScheduleOrderId"),
                "最新重排范围必须取重排说明中的排产工单 ID，而不是生产工单或产品信息");
    }

    private static String readSource(String relativePath) throws Exception {
        return Files.readString(PROJECT_DIR.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        assertTrue(start >= 0, "缺少源码片段起点: " + startToken);
        assertTrue(end > start, "缺少源码片段终点: " + endToken);
        return source.substring(start, end);
    }

    private static int count(String source, String needle) {
        int count = 0;
        int index = source.indexOf(needle);
        while (index >= 0) {
            count++;
            index = source.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
