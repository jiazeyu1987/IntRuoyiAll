package cn.iocoder.yudao.module.mes.service.pro.feedback;

import cn.hutool.core.collection.ListUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmQualityStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.wm.itemconsume.MesWmItemConsumeService;
import cn.iocoder.yudao.module.mes.service.wm.productproduce.MesWmProductProduceLineService;
import cn.iocoder.yudao.module.mes.service.wm.productproduce.MesWmProductProduceService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesProFeedbackServiceImpl} 的单元测试
 *
 * @author 瑛泰源码
 */
@Import({MesProFeedbackServiceImpl.class, FeedbackScheduleLinkageGuard.class})
public class MesProFeedbackServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesProFeedbackServiceImpl feedbackService;

    @Resource
    private MesProFeedbackMapper feedbackMapper;
    @MockitoBean
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @MockitoBean
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;

    @MockitoBean
    private MesProWorkOrderService workOrderService;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;
    @MockitoBean
    private MesMdWorkstationService workstationService;
    @MockitoBean
    private MesProTaskService taskService;
    @MockitoBean
    private MesProScheduleOrderService scheduleOrderService;
    @MockitoBean
    private MesWmItemConsumeService itemConsumeService;
    @MockitoBean
    private MesWmProductProduceService productProduceService;
    @MockitoBean
    private MesWmProductProduceLineService produceLineService;

    @Test
    public void testCreateFeedback_autoLinksScheduleOrderAndProcessSnapshot() {
        Long workOrderId = randomLongId();
        Long taskId = randomLongId();
        Long routeId = randomLongId();
        Long processId = randomLongId();
        Long itemId = randomLongId();
        Long workstationId = randomLongId();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(randomLongId())
                .code("SCH-FB-001")
                .workOrderId(workOrderId)
                .productId(itemId)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(randomLongId())
                .scheduleOrderId(scheduleOrder.getId())
                .processId(processId)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100.000000"))
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("100.000000"))
                .build();
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(workOrderId)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrder.getId()))
                .thenReturn(ListUtil.of(scheduleOrderProcess));

        MesProFeedbackSaveReqVO reqVO = new MesProFeedbackSaveReqVO();
        reqVO.setCode("FB-AUTO-LINK-001");
        reqVO.setType(1);
        reqVO.setWorkstationId(workstationId);
        reqVO.setRouteId(routeId);
        reqVO.setProcessId(processId);
        reqVO.setWorkOrderId(workOrderId);
        reqVO.setTaskId(taskId);
        reqVO.setItemId(itemId);
        reqVO.setFeedbackQuantity(new BigDecimal("10.000000"));
        reqVO.setQualifiedQuantity(new BigDecimal("10.000000"));
        reqVO.setUnqualifiedQuantity(BigDecimal.ZERO);
        reqVO.setFeedbackUserId(randomLongId());
        reqVO.setFeedbackTime(LocalDateTime.of(2026, 6, 14, 12, 0));
        reqVO.setApproveUserId(randomLongId());

        when(workstationService.validateWorkstationExists(workstationId))
                .thenReturn(MesMdWorkstationDO.builder().id(workstationId).processId(processId).build());
        when(routeProcessService.resolveCurrentRouteProcess(null, routeId, processId))
                .thenReturn(MesProRouteProcessDO.builder().routeId(routeId).processId(processId)
                        .checkFlag(Boolean.FALSE).build());
        when(workOrderService.validateWorkOrderConfirmed(workOrderId))
                .thenReturn(MesProWorkOrderDO.builder().id(workOrderId).productId(itemId).build());
        when(taskService.validateTaskNotFinished(taskId)).thenReturn(MesProTaskDO.builder()
                .id(taskId)
                .workOrderId(workOrderId)
                .workstationId(workstationId)
                .routeId(routeId)
                .processId(processId)
                .itemId(itemId)
                .build());

        Long feedbackId = feedbackService.createFeedback(reqVO);

        MesProFeedbackDO feedback = feedbackMapper.selectById(feedbackId);
        assertEquals(scheduleOrder.getId(), feedback.getScheduleOrderId());
        assertEquals(scheduleOrderProcess.getId(), feedback.getScheduleOrderProcessId());
    }

    @Test
    public void testCreateFeedbackWithScheduleSnapshot_shouldKeepFrozenRouteProcessForScheduleSnapshot() {
        Long workOrderId = randomLongId();
        Long taskId = randomLongId();
        Long routeId = randomLongId();
        Long frozenProcessId = randomLongId();
        Long routeProcessId = randomLongId();
        Long itemId = randomLongId();
        Long workstationId = randomLongId();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(randomLongId())
                .workOrderId(workOrderId)
                .routeId(routeId)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(randomLongId())
                .scheduleOrderId(scheduleOrder.getId())
                .routeProcessId(routeProcessId)
                .processId(frozenProcessId)
                .enabled(Boolean.TRUE)
                .remainingQuantity(new BigDecimal("100.000000"))
                .build();

        MesProFeedbackSaveReqVO reqVO = new MesProFeedbackSaveReqVO();
        reqVO.setCode("FB-SNAPSHOT-CANONICAL-001");
        reqVO.setType(1);
        reqVO.setWorkstationId(workstationId);
        reqVO.setRouteId(routeId);
        reqVO.setProcessId(frozenProcessId);
        reqVO.setWorkOrderId(workOrderId);
        reqVO.setTaskId(taskId);
        reqVO.setScheduleOrderId(scheduleOrder.getId());
        reqVO.setScheduleOrderProcessId(scheduleOrderProcess.getId());
        reqVO.setItemId(itemId);
        reqVO.setFeedbackQuantity(new BigDecimal("10.000000"));
        reqVO.setQualifiedQuantity(new BigDecimal("10.000000"));
        reqVO.setUnqualifiedQuantity(BigDecimal.ZERO);
        reqVO.setFeedbackUserId(randomLongId());
        reqVO.setFeedbackTime(LocalDateTime.of(2026, 7, 5, 15, 24));
        reqVO.setApproveUserId(randomLongId());

        when(scheduleOrderMapper.selectById(scheduleOrder.getId())).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(scheduleOrderProcess.getId())).thenReturn(scheduleOrderProcess);
        when(routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, frozenProcessId))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(routeProcessId)
                        .routeId(routeId)
                        .processId(frozenProcessId)
                        .checkFlag(Boolean.FALSE)
                        .build());
        when(workstationService.validateWorkstationExists(workstationId))
                .thenReturn(MesMdWorkstationDO.builder().id(workstationId).processId(frozenProcessId).build());
        when(workOrderService.validateWorkOrderConfirmed(workOrderId))
                .thenReturn(MesProWorkOrderDO.builder().id(workOrderId).productId(itemId).build());
        when(taskService.validateTaskNotFinished(taskId)).thenReturn(MesProTaskDO.builder()
                .id(taskId)
                .workOrderId(workOrderId)
                .workstationId(workstationId)
                .routeId(routeId)
                .processId(frozenProcessId)
                .itemId(itemId)
                .build());

        Long feedbackId = feedbackService.createFeedbackWithScheduleSnapshot(reqVO);

        MesProFeedbackDO feedback = feedbackMapper.selectById(feedbackId);
        assertEquals(routeId, feedback.getRouteId());
        assertEquals(frozenProcessId, feedback.getProcessId());
        assertEquals(scheduleOrder.getId(), feedback.getScheduleOrderId());
        assertEquals(scheduleOrderProcess.getId(), feedback.getScheduleOrderProcessId());
        verify(routeProcessService).resolveFrozenRouteProcess(routeProcessId, routeId, frozenProcessId);
        verify(routeProcessService, never()).resolveCurrentRouteProcess(routeProcessId, routeId, frozenProcessId);
    }

    @Test
    public void testCreateFeedbackWithScheduleSnapshot_shouldResolveZeroScheduleProcessIdByRouteProcess() {
        Long workOrderId = randomLongId();
        Long taskId = randomLongId();
        Long routeId = randomLongId();
        Long routeProcessId = randomLongId();
        Long routeProcessProcessId = randomLongId();
        Long itemId = randomLongId();
        Long workstationId = randomLongId();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(randomLongId())
                .workOrderId(workOrderId)
                .routeId(routeId)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(randomLongId())
                .scheduleOrderId(scheduleOrder.getId())
                .routeProcessId(routeProcessId)
                .processId(0L)
                .enabled(Boolean.TRUE)
                .remainingQuantity(new BigDecimal("100.000000"))
                .build();

        MesProFeedbackSaveReqVO reqVO = new MesProFeedbackSaveReqVO();
        reqVO.setCode("FB-SNAPSHOT-ZERO-PROCESS-001");
        reqVO.setType(1);
        reqVO.setWorkstationId(workstationId);
        reqVO.setRouteId(routeId);
        reqVO.setProcessId(routeProcessProcessId);
        reqVO.setWorkOrderId(workOrderId);
        reqVO.setTaskId(taskId);
        reqVO.setScheduleOrderId(scheduleOrder.getId());
        reqVO.setScheduleOrderProcessId(scheduleOrderProcess.getId());
        reqVO.setItemId(itemId);
        reqVO.setFeedbackQuantity(new BigDecimal("10.000000"));
        reqVO.setQualifiedQuantity(new BigDecimal("10.000000"));
        reqVO.setUnqualifiedQuantity(BigDecimal.ZERO);
        reqVO.setFeedbackUserId(randomLongId());
        reqVO.setFeedbackTime(LocalDateTime.of(2026, 7, 5, 15, 24));
        reqVO.setApproveUserId(randomLongId());

        when(scheduleOrderMapper.selectById(scheduleOrder.getId())).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(scheduleOrderProcess.getId())).thenReturn(scheduleOrderProcess);
        when(routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, 0L))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(routeProcessId)
                        .routeId(routeId)
                        .processId(routeProcessProcessId)
                        .checkFlag(Boolean.FALSE)
                        .build());
        when(workstationService.validateWorkstationExists(workstationId))
                .thenReturn(MesMdWorkstationDO.builder().id(workstationId).processId(routeProcessProcessId).build());
        when(workOrderService.validateWorkOrderConfirmed(workOrderId))
                .thenReturn(MesProWorkOrderDO.builder().id(workOrderId).productId(itemId).build());
        when(taskService.validateTaskNotFinished(taskId)).thenReturn(MesProTaskDO.builder()
                .id(taskId)
                .workOrderId(workOrderId)
                .workstationId(workstationId)
                .routeId(routeId)
                .processId(routeProcessProcessId)
                .itemId(itemId)
                .build());

        Long feedbackId = feedbackService.createFeedbackWithScheduleSnapshot(reqVO);

        MesProFeedbackDO feedback = feedbackMapper.selectById(feedbackId);
        assertEquals(routeId, feedback.getRouteId());
        assertEquals(routeProcessProcessId, feedback.getProcessId());
        assertEquals(scheduleOrder.getId(), feedback.getScheduleOrderId());
        assertEquals(scheduleOrderProcess.getId(), feedback.getScheduleOrderProcessId());
        verify(routeProcessService).resolveFrozenRouteProcess(routeProcessId, routeId, 0L);
    }

    @Test
    public void testCreateFeedback_shouldAllowRouteProcessTaskWithoutBoundWorkstation() {
        Long workOrderId = randomLongId();
        Long taskId = randomLongId();
        Long routeId = randomLongId();
        Long processId = randomLongId();
        Long itemId = randomLongId();
        Long workstationId = randomLongId();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(randomLongId())
                .code("SCH-FB-ROUTE-001")
                .workOrderId(workOrderId)
                .productId(itemId)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(randomLongId())
                .scheduleOrderId(scheduleOrder.getId())
                .processId(processId)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100.000000"))
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("100.000000"))
                .build();
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(workOrderId)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrder.getId()))
                .thenReturn(ListUtil.of(scheduleOrderProcess));

        MesProFeedbackSaveReqVO reqVO = new MesProFeedbackSaveReqVO();
        reqVO.setCode("FB-ROUTE-TASK-001");
        reqVO.setType(1);
        reqVO.setWorkstationId(workstationId);
        reqVO.setRouteId(routeId);
        reqVO.setProcessId(processId);
        reqVO.setWorkOrderId(workOrderId);
        reqVO.setTaskId(taskId);
        reqVO.setItemId(itemId);
        reqVO.setFeedbackQuantity(new BigDecimal("10.000000"));
        reqVO.setQualifiedQuantity(new BigDecimal("10.000000"));
        reqVO.setUnqualifiedQuantity(BigDecimal.ZERO);
        reqVO.setFeedbackUserId(randomLongId());
        reqVO.setFeedbackTime(LocalDateTime.of(2026, 6, 14, 12, 0));
        reqVO.setApproveUserId(randomLongId());

        when(workstationService.validateWorkstationExists(workstationId))
                .thenReturn(MesMdWorkstationDO.builder().id(workstationId).processId(processId).build());
        when(routeProcessService.resolveCurrentRouteProcess(null, routeId, processId))
                .thenReturn(MesProRouteProcessDO.builder().routeId(routeId).processId(processId)
                        .checkFlag(Boolean.FALSE).build());
        when(workOrderService.validateWorkOrderConfirmed(workOrderId))
                .thenReturn(MesProWorkOrderDO.builder().id(workOrderId).productId(itemId).build());
        when(taskService.validateTaskNotFinished(taskId)).thenReturn(MesProTaskDO.builder()
                .id(taskId)
                .workOrderId(workOrderId)
                .workstationId(null)
                .routeId(routeId)
                .processId(processId)
                .itemId(itemId)
                .build());

        Long feedbackId = feedbackService.createFeedback(reqVO);

        MesProFeedbackDO feedback = feedbackMapper.selectById(feedbackId);
        assertEquals(taskId, feedback.getTaskId());
        assertEquals(workstationId, feedback.getWorkstationId());
        assertEquals(scheduleOrder.getId(), feedback.getScheduleOrderId());
        assertEquals(scheduleOrderProcess.getId(), feedback.getScheduleOrderProcessId());
    }

    @Test
    public void testCreateFeedback_shouldRejectWhenExceedScheduleOrderProcessRemainingQuantity() {
        Long workOrderId = randomLongId();
        Long taskId = randomLongId();
        Long routeId = randomLongId();
        Long processId = randomLongId();
        Long itemId = randomLongId();
        Long workstationId = randomLongId();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(randomLongId())
                .code("SCH-FB-OVER-001")
                .workOrderId(workOrderId)
                .productId(itemId)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(randomLongId())
                .scheduleOrderId(scheduleOrder.getId())
                .processId(processId)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100.000000"))
                .reportedQuantity(new BigDecimal("95.000000"))
                .remainingQuantity(new BigDecimal("5.000000"))
                .build();
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(workOrderId)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrder.getId()))
                .thenReturn(ListUtil.of(scheduleOrderProcess));

        MesProFeedbackSaveReqVO reqVO = new MesProFeedbackSaveReqVO();
        reqVO.setCode("FB-OVER-001");
        reqVO.setType(1);
        reqVO.setWorkstationId(workstationId);
        reqVO.setRouteId(routeId);
        reqVO.setProcessId(processId);
        reqVO.setWorkOrderId(workOrderId);
        reqVO.setTaskId(taskId);
        reqVO.setItemId(itemId);
        reqVO.setFeedbackQuantity(new BigDecimal("6.000000"));
        reqVO.setQualifiedQuantity(new BigDecimal("6.000000"));
        reqVO.setUnqualifiedQuantity(BigDecimal.ZERO);
        reqVO.setFeedbackUserId(randomLongId());
        reqVO.setFeedbackTime(LocalDateTime.of(2026, 6, 24, 10, 0));
        reqVO.setApproveUserId(randomLongId());

        when(workstationService.validateWorkstationExists(workstationId))
                .thenReturn(MesMdWorkstationDO.builder().id(workstationId).processId(processId).build());
        when(routeProcessService.resolveCurrentRouteProcess(null, routeId, processId))
                .thenReturn(MesProRouteProcessDO.builder().routeId(routeId).processId(processId)
                        .checkFlag(Boolean.FALSE).build());
        when(workOrderService.validateWorkOrderConfirmed(workOrderId))
                .thenReturn(MesProWorkOrderDO.builder().id(workOrderId).productId(itemId).build());
        when(taskService.validateTaskNotFinished(taskId)).thenReturn(MesProTaskDO.builder()
                .id(taskId)
                .workOrderId(workOrderId)
                .workstationId(workstationId)
                .routeId(routeId)
                .processId(processId)
                .itemId(itemId)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> feedbackService.createFeedback(reqVO));

        assertEquals(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    public void testCreateFeedbackWithScheduleSnapshot_shouldRejectWhenExceedScheduleOrderProcessRemainingQuantity() {
        Long workOrderId = randomLongId();
        Long taskId = randomLongId();
        Long routeId = randomLongId();
        Long processId = randomLongId();
        Long routeProcessId = randomLongId();
        Long itemId = randomLongId();
        Long workstationId = randomLongId();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(randomLongId())
                .workOrderId(workOrderId)
                .routeId(routeId)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(randomLongId())
                .scheduleOrderId(scheduleOrder.getId())
                .routeProcessId(routeProcessId)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .remainingQuantity(new BigDecimal("5.000000"))
                .build();

        MesProFeedbackSaveReqVO reqVO = new MesProFeedbackSaveReqVO();
        reqVO.setCode("FB-SNAPSHOT-OVER-001");
        reqVO.setType(1);
        reqVO.setWorkstationId(workstationId);
        reqVO.setRouteId(routeId);
        reqVO.setProcessId(processId);
        reqVO.setWorkOrderId(workOrderId);
        reqVO.setTaskId(taskId);
        reqVO.setScheduleOrderId(scheduleOrder.getId());
        reqVO.setScheduleOrderProcessId(scheduleOrderProcess.getId());
        reqVO.setItemId(itemId);
        reqVO.setFeedbackQuantity(new BigDecimal("6.000000"));
        reqVO.setQualifiedQuantity(new BigDecimal("6.000000"));
        reqVO.setUnqualifiedQuantity(BigDecimal.ZERO);
        reqVO.setFeedbackUserId(randomLongId());
        reqVO.setFeedbackTime(LocalDateTime.of(2026, 7, 11, 10, 0));
        reqVO.setApproveUserId(randomLongId());

        when(scheduleOrderMapper.selectById(scheduleOrder.getId())).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(scheduleOrderProcess.getId())).thenReturn(scheduleOrderProcess);
        when(routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, processId))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(routeProcessId)
                        .routeId(routeId)
                        .processId(processId)
                        .checkFlag(Boolean.FALSE)
                        .build());
        when(workstationService.validateWorkstationExists(workstationId))
                .thenReturn(MesMdWorkstationDO.builder().id(workstationId).processId(processId).build());
        when(workOrderService.validateWorkOrderConfirmed(workOrderId))
                .thenReturn(MesProWorkOrderDO.builder().id(workOrderId).productId(itemId).build());
        when(taskService.validateTaskNotFinished(taskId)).thenReturn(MesProTaskDO.builder()
                .id(taskId)
                .workOrderId(workOrderId)
                .workstationId(workstationId)
                .routeId(routeId)
                .processId(processId)
                .itemId(itemId)
                .build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> feedbackService.createFeedbackWithScheduleSnapshot(reqVO));

        assertEquals(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateProFeedbackWhenIpqcFinish_success_withUnqualified() {
        // 准备数据：插入一条待检验状态的报工单
        Long taskId = randomLongId();
        Long workOrderId = randomLongId();
        Long sourceLineId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.UNCHECK.getStatus());
            o.setTaskId(taskId);
            o.setWorkOrderId(workOrderId);
            o.setFeedbackQuantity(BigDecimal.valueOf(100));
        });
        feedbackMapper.insert(feedback);

        // mock: 产出行（用于 updateTaskAndWorkOrderByFeedback 内的数量聚合）
        MesWmProductProduceLineDO qualifiedLine = MesWmProductProduceLineDO.builder()
                .quantity(BigDecimal.valueOf(80))
                .qualityStatus(MesWmQualityStatusEnum.PASS.getStatus())
                .build();
        MesWmProductProduceLineDO unqualifiedLine = MesWmProductProduceLineDO.builder()
                .quantity(BigDecimal.valueOf(20))
                .qualityStatus(MesWmQualityStatusEnum.FAIL.getStatus())
                .build();
        when(produceLineService.getProductProduceLineListByFeedbackId(feedback.getId()))
                .thenReturn(ListUtil.of(qualifiedLine, unqualifiedLine));

        // 调用
        BigDecimal qualifiedQty = BigDecimal.valueOf(80);
        BigDecimal unqualifiedQty = BigDecimal.valueOf(20);
        BigDecimal laborScrapQty = BigDecimal.valueOf(5);
        BigDecimal materialScrapQty = BigDecimal.valueOf(10);
        BigDecimal otherScrapQty = BigDecimal.valueOf(5);
        feedbackService.updateProFeedbackWhenIpqcFinish(feedback.getId(), sourceLineId,
                qualifiedQty, unqualifiedQty, laborScrapQty, materialScrapQty, otherScrapQty);

        // 断言 1：调用了 splitPendingAndFinishProduce
        verify(productProduceService).splitPendingAndFinishProduce(
                eq(feedback.getId()), eq(sourceLineId), eq(qualifiedQty), eq(unqualifiedQty));

        // 断言 2：报工单状态更新为已完成
        MesProFeedbackDO updatedFeedback = feedbackMapper.selectById(feedback.getId());
        assertEquals(MesProFeedbackStatusEnum.FINISHED.getStatus(), updatedFeedback.getStatus());

        // 断言 3：数量回写正确
        assertEquals(0, qualifiedQty.compareTo(updatedFeedback.getQualifiedQuantity()));
        assertEquals(0, unqualifiedQty.compareTo(updatedFeedback.getUnqualifiedQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedFeedback.getUncheckQuantity()));
        assertEquals(0, laborScrapQty.compareTo(updatedFeedback.getLaborScrapQuantity()));
        assertEquals(0, materialScrapQty.compareTo(updatedFeedback.getMaterialScrapQuantity()));
        assertEquals(0, otherScrapQty.compareTo(updatedFeedback.getOtherScrapQuantity()));

        // 断言 4：更新了任务和工单的已生产数量
        // 注意：数量经过 DB 存取后 scale 可能变化，用 any() 匹配
        verify(taskService).updateProducedQuantity(eq(taskId),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
        verify(workOrderService).updateProducedQuantity(eq(workOrderId),
                any(BigDecimal.class));
    }

    @Test
    public void testUpdateProFeedbackWhenIpqcFinish_success_allQualified() {
        // 准备数据：全部合格
        Long taskId = randomLongId();
        Long workOrderId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.UNCHECK.getStatus());
            o.setTaskId(taskId);
            o.setWorkOrderId(workOrderId);
            o.setFeedbackQuantity(BigDecimal.valueOf(50));
        });
        feedbackMapper.insert(feedback);

        // mock: 产出行（全部合格）
        MesWmProductProduceLineDO qualifiedLine = MesWmProductProduceLineDO.builder()
                .quantity(BigDecimal.valueOf(50))
                .qualityStatus(MesWmQualityStatusEnum.PASS.getStatus())
                .build();
        when(produceLineService.getProductProduceLineListByFeedbackId(feedback.getId()))
                .thenReturn(ListUtil.of(qualifiedLine));

        // 调用
        feedbackService.updateProFeedbackWhenIpqcFinish(feedback.getId(), randomLongId(),
                BigDecimal.valueOf(50), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        // 断言 1：报工单状态为已完成
        MesProFeedbackDO updatedFeedback = feedbackMapper.selectById(feedback.getId());
        assertEquals(MesProFeedbackStatusEnum.FINISHED.getStatus(), updatedFeedback.getStatus());

        // 断言 2：合格品数量正确，不合格品和废品为 0
        assertEquals(0, BigDecimal.valueOf(50).compareTo(updatedFeedback.getQualifiedQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedFeedback.getUnqualifiedQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedFeedback.getLaborScrapQuantity()));
    }

    @Test
    public void testUpdateProFeedbackWhenIpqcFinish_feedbackNotExists() {
        // 调用不存在的 feedbackId，应该抛异常
        Long feedbackId = randomLongId();
        assertThrows(Exception.class, () ->
                feedbackService.updateProFeedbackWhenIpqcFinish(feedbackId, randomLongId(),
                        BigDecimal.TEN, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    public void testUpdateProFeedbackWhenIpqcFinish_feedbackNotUncheck() {
        // 准备数据：草稿状态（不是 UNCHECK），应该报错
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.PREPARE.getStatus());
        });
        feedbackMapper.insert(feedback);

        // 调用，应该抛异常
        assertThrows(Exception.class, () ->
                feedbackService.updateProFeedbackWhenIpqcFinish(feedback.getId(), randomLongId(),
                        BigDecimal.TEN, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        // 断言：不应该调用任何产出单方法
        verify(productProduceService, never()).splitPendingAndFinishProduce(anyLong(), anyLong(), any(), any());
    }

    @Test
    public void testUpdateProFeedbackWhenIpqcFinish_feedbackAlreadyFinished() {
        // 准备数据：已完成状态，不能再次完成
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.FINISHED.getStatus());
        });
        feedbackMapper.insert(feedback);

        // 调用，应该抛异常
        assertThrows(Exception.class, () ->
                feedbackService.updateProFeedbackWhenIpqcFinish(feedback.getId(), randomLongId(),
                        BigDecimal.TEN, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    // ==================== approveFeedback 测试 ====================

    @Test
    public void testRejectFeedback_shouldReturnToDraftAndPersistRejectReason() {
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus());
            o.setRemark(null);
        });
        feedbackMapper.insert(feedback);

        feedbackService.rejectFeedback(feedback.getId(), "数量不一致");

        MesProFeedbackDO updated = feedbackMapper.selectById(feedback.getId());
        assertEquals(MesProFeedbackStatusEnum.PREPARE.getStatus(), updated.getStatus());
        assertEquals("数量不一致", updated.getRemark());
    }

    @Test
    public void testApproveFeedback_shouldUseFrozenRouteProcessForScheduleSnapshot() {
        Long routeId = randomLongId();
        Long frozenProcessId = randomLongId();
        Long scheduleOrderProcessId = randomLongId();
        Long routeProcessId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus());
            o.setFeedbackQuantity(BigDecimal.TEN);
            o.setUncheckQuantity(BigDecimal.ZERO);
            o.setRouteId(routeId);
            o.setProcessId(frozenProcessId);
            o.setScheduleOrderProcessId(scheduleOrderProcessId);
        });
        feedbackMapper.insert(feedback);
        when(scheduleOrderProcessMapper.selectById(scheduleOrderProcessId))
                .thenReturn(MesProScheduleOrderProcessDO.builder()
                        .id(scheduleOrderProcessId)
                        .routeProcessId(routeProcessId)
                        .processId(frozenProcessId)
                        .build());
        when(routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, frozenProcessId))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(randomLongId())
                        .routeId(routeId)
                        .processId(frozenProcessId)
                        .keyFlag(false)
                        .checkFlag(false)
                        .build());

        assertTrue(feedbackService.approveFeedback(feedback.getId()));

        verify(itemConsumeService).generateItemConsume(argThat(currentFeedback ->
                frozenProcessId.equals(currentFeedback.getProcessId())
                        && routeId.equals(currentFeedback.getRouteId())));
        verify(routeProcessService, never()).resolveCurrentRouteProcess(routeProcessId, routeId, frozenProcessId);
    }

    @Test
    public void approveFeedback_shouldRejectScheduleLinkedFeedbackWithoutFrozenProcessSnapshot() {
        Long routeId = randomLongId();
        Long processId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus());
            o.setFeedbackQuantity(BigDecimal.TEN);
            o.setUncheckQuantity(BigDecimal.ZERO);
            o.setRouteId(routeId);
            o.setProcessId(processId);
            o.setScheduleOrderId(randomLongId());
            o.setScheduleOrderProcessId(null);
        });
        feedbackMapper.insert(feedback);

        assertThrows(ServiceException.class, () -> feedbackService.approveFeedback(feedback.getId()));

        verify(routeProcessService, never()).resolveCurrentRouteProcess(null, routeId, processId);
        verify(itemConsumeService, never()).generateItemConsume(any());
    }

    @Test
    public void updateFeedback_shouldPreserveFrozenScheduleSnapshotInsteadOfRefillingCurrentEffectiveSchedule() {
        Long feedbackId = randomLongId();
        Long workOrderId = randomLongId();
        Long taskId = randomLongId();
        Long routeId = randomLongId();
        Long processId = randomLongId();
        Long itemId = randomLongId();
        Long workstationId = randomLongId();
        Long frozenScheduleOrderId = randomLongId();
        Long frozenScheduleOrderProcessId = randomLongId();
        Long routeProcessId = randomLongId();
        Long currentScheduleOrderId = randomLongId();
        Long currentScheduleOrderProcessId = randomLongId();
        MesProFeedbackDO existing = randomPojo(MesProFeedbackDO.class, o -> {
            o.setId(feedbackId);
            o.setStatus(MesProFeedbackStatusEnum.PREPARE.getStatus());
            o.setRouteId(routeId);
            o.setProcessId(processId);
            o.setWorkOrderId(workOrderId);
            o.setTaskId(taskId);
            o.setWorkstationId(workstationId);
            o.setScheduleOrderId(frozenScheduleOrderId);
            o.setScheduleOrderProcessId(frozenScheduleOrderProcessId);
        });
        feedbackMapper.insert(existing);
        MesProFeedbackSaveReqVO updateReqVO = new MesProFeedbackSaveReqVO();
        updateReqVO.setId(feedbackId);
        updateReqVO.setCode("FB-PRESERVE-SNAPSHOT");
        updateReqVO.setType(1);
        updateReqVO.setWorkstationId(workstationId);
        updateReqVO.setRouteId(routeId);
        updateReqVO.setProcessId(processId);
        updateReqVO.setWorkOrderId(workOrderId);
        updateReqVO.setTaskId(taskId);
        updateReqVO.setItemId(itemId);
        updateReqVO.setFeedbackQuantity(new BigDecimal("5.000000"));
        updateReqVO.setQualifiedQuantity(new BigDecimal("5.000000"));
        updateReqVO.setUnqualifiedQuantity(BigDecimal.ZERO);
        updateReqVO.setFeedbackUserId(randomLongId());
        updateReqVO.setFeedbackTime(LocalDateTime.of(2026, 7, 16, 2, 50));
        updateReqVO.setApproveUserId(randomLongId());
        MesProScheduleOrderDO frozenScheduleOrder = MesProScheduleOrderDO.builder()
                .id(frozenScheduleOrderId)
                .workOrderId(workOrderId)
                .routeId(routeId)
                .build();
        MesProScheduleOrderProcessDO frozenProcess = MesProScheduleOrderProcessDO.builder()
                .id(frozenScheduleOrderProcessId)
                .scheduleOrderId(frozenScheduleOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .remainingQuantity(new BigDecimal("10.000000"))
                .build();
        MesProScheduleOrderDO currentScheduleOrder = MesProScheduleOrderDO.builder()
                .id(currentScheduleOrderId)
                .workOrderId(workOrderId)
                .routeId(routeId)
                .build();
        MesProScheduleOrderProcessDO currentProcess = MesProScheduleOrderProcessDO.builder()
                .id(currentScheduleOrderProcessId)
                .scheduleOrderId(currentScheduleOrderId)
                .routeProcessId(randomLongId())
                .processId(processId)
                .enabled(Boolean.TRUE)
                .remainingQuantity(new BigDecimal("10.000000"))
                .build();
        when(scheduleOrderMapper.selectById(frozenScheduleOrderId)).thenReturn(frozenScheduleOrder);
        when(scheduleOrderProcessMapper.selectById(frozenScheduleOrderProcessId)).thenReturn(frozenProcess);
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(workOrderId)).thenReturn(currentScheduleOrder);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(currentScheduleOrderId))
                .thenReturn(ListUtil.of(currentProcess));
        when(routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, processId))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(routeProcessId)
                        .routeId(routeId)
                        .processId(processId)
                        .checkFlag(Boolean.FALSE)
                        .build());
        when(workstationService.validateWorkstationExists(workstationId))
                .thenReturn(MesMdWorkstationDO.builder().id(workstationId).processId(processId).build());
        when(workOrderService.validateWorkOrderConfirmed(workOrderId))
                .thenReturn(MesProWorkOrderDO.builder().id(workOrderId).productId(itemId).build());
        when(taskService.validateTaskNotFinished(taskId)).thenReturn(MesProTaskDO.builder()
                .id(taskId)
                .workOrderId(workOrderId)
                .workstationId(workstationId)
                .routeId(routeId)
                .processId(processId)
                .itemId(itemId)
                .build());

        feedbackService.updateFeedback(updateReqVO);

        MesProFeedbackDO updated = feedbackMapper.selectById(feedbackId);
        assertEquals(frozenScheduleOrderId, updated.getScheduleOrderId());
        assertEquals(frozenScheduleOrderProcessId, updated.getScheduleOrderProcessId());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(null, routeId, processId);
    }

    @Test
    public void testApproveFeedback_keyNonCheck_success() {
        // 准备数据：关键非质检工序，常规审批
        Long routeId = randomLongId();
        Long processId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus());
            o.setFeedbackQuantity(BigDecimal.valueOf(100));
            o.setQualifiedQuantity(BigDecimal.valueOf(80));
            o.setUnqualifiedQuantity(BigDecimal.valueOf(20));
            o.setUncheckQuantity(BigDecimal.ZERO);
            o.setRouteId(routeId);
            o.setProcessId(processId);
            o.setScheduleOrderId(null);
            o.setScheduleOrderProcessId(null);
        });
        feedbackMapper.insert(feedback);

        // mock: 工序配置 key=true, check=false
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(routeId).processId(processId)
                .keyFlag(true).checkFlag(false).build();
        when(routeProcessService.resolveCurrentRouteProcess(null, routeId, processId))
                .thenReturn(routeProcess);

        // mock: 产品产出单
        MesWmProductProduceDO produce = randomPojo(MesWmProductProduceDO.class);
        when(productProduceService.generateProductProduce(any(), eq(false))).thenReturn(produce);

        // mock: 产出行（用于 updateTaskAndWorkOrderByFeedback）
        MesWmProductProduceLineDO qualifiedLine = MesWmProductProduceLineDO.builder()
                .quantity(BigDecimal.valueOf(80))
                .qualityStatus(MesWmQualityStatusEnum.PASS.getStatus()).build();
        when(produceLineService.getProductProduceLineListByFeedbackId(feedback.getId()))
                .thenReturn(ListUtil.of(qualifiedLine));

        // 调用
        boolean result = feedbackService.approveFeedback(feedback.getId());

        // 断言 1：返回 true（已完成）
        assertTrue(result);

        // 断言 2：状态为已完成，uncheckQuantity 清零
        MesProFeedbackDO updated = feedbackMapper.selectById(feedback.getId());
        assertEquals(MesProFeedbackStatusEnum.FINISHED.getStatus(), updated.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(updated.getUncheckQuantity()));

        // 断言 3：调用了产出单生成 + 入库 + 任务/工单更新
        verify(productProduceService).generateProductProduce(any(), eq(false));
        verify(productProduceService).finishProductProduce(produce.getId());
        verify(taskService).updateProducedQuantity(eq(feedback.getTaskId()),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
        verify(workOrderService).updateProducedQuantity(eq(feedback.getWorkOrderId()),
                any(BigDecimal.class));
    }

    @Test
    public void testApproveFeedback_keyCheck_enterUncheck() {
        // 准备数据：关键质检工序，应进入待检验
        Long routeId = randomLongId();
        Long processId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus());
            o.setFeedbackQuantity(BigDecimal.valueOf(50));
            o.setUncheckQuantity(BigDecimal.valueOf(50)); // 质检工序 uncheckQuantity > 0
            o.setRouteId(routeId);
            o.setProcessId(processId);
            o.setScheduleOrderId(null);
            o.setScheduleOrderProcessId(null);
        });
        feedbackMapper.insert(feedback);

        // mock: key=true, check=true
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(routeId).processId(processId)
                .keyFlag(true).checkFlag(true).build();
        when(routeProcessService.resolveCurrentRouteProcess(null, routeId, processId))
                .thenReturn(routeProcess);

        // 调用
        boolean result = feedbackService.approveFeedback(feedback.getId());

        // 断言 1：返回 false（待检验）
        assertFalse(result);

        // 断言 2：状态为待检验
        MesProFeedbackDO updated = feedbackMapper.selectById(feedback.getId());
        assertEquals(MesProFeedbackStatusEnum.UNCHECK.getStatus(), updated.getStatus());

        // 断言 3：生成了待检产出单，但没有 finishProductProduce
        verify(productProduceService).generateProductProduce(any(), eq(true));
        verify(productProduceService, never()).finishProductProduce(anyLong());

        // 断言 4：没有更新任务/工单数量（等 IPQC 回调）
        verify(taskService, never()).updateProducedQuantity(anyLong(), any(), any(), any());
        verify(workOrderService, never()).updateProducedQuantity(anyLong(), any());
    }

    @Test
    public void testApproveFeedback_nonKey_directFinish() {
        // 准备数据：非关键工序，直接完结
        Long routeId = randomLongId();
        Long processId = randomLongId();
        Long scheduleOrderProcessId = randomLongId();
        Long routeProcessId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus());
            o.setFeedbackQuantity(BigDecimal.valueOf(30));
            o.setUncheckQuantity(BigDecimal.ZERO);
            o.setRouteId(routeId);
            o.setProcessId(processId);
            o.setScheduleOrderId(9001L);
            o.setScheduleOrderProcessId(scheduleOrderProcessId);
        });
        feedbackMapper.insert(feedback);

        // mock: key=false, check=false
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId).processId(processId)
                .keyFlag(false).checkFlag(false).build();
        when(scheduleOrderProcessMapper.selectById(scheduleOrderProcessId))
                .thenReturn(MesProScheduleOrderProcessDO.builder()
                        .id(scheduleOrderProcessId)
                        .routeProcessId(routeProcessId)
                        .processId(processId)
                        .build());
        when(routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, processId))
                .thenReturn(routeProcess);

        // 调用
        boolean result = feedbackService.approveFeedback(feedback.getId());

        // 断言 1：返回 true（已完成）
        assertTrue(result);

        // 断言 2：状态为已完成
        MesProFeedbackDO updated = feedbackMapper.selectById(feedback.getId());
        assertEquals(MesProFeedbackStatusEnum.FINISHED.getStatus(), updated.getStatus());

        // 断言 3：不生成产出单，不更新任务/工单
        verify(productProduceService, never()).generateProductProduce(any(), anyBoolean());
        verify(taskService, never()).updateProducedQuantity(anyLong(), any(), any(), any());
        verify(scheduleOrderService).syncFeedbackProgress(9001L);
    }

    @Test
    public void testApproveFeedback_nonCheck_uncheckQuantityReject() {
        // 准备数据：非质检工序，但 uncheckQuantity > 0（异常数据），应被拦截
        Long routeId = randomLongId();
        Long processId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus());
            o.setFeedbackQuantity(BigDecimal.valueOf(10));
            o.setUncheckQuantity(BigDecimal.valueOf(10)); // 非质检工序不应有待检数量
            o.setRouteId(routeId);
            o.setProcessId(processId);
            o.setScheduleOrderProcessId(null);
        });
        feedbackMapper.insert(feedback);

        // mock: key=true, check=false（非质检）
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(routeId).processId(processId)
                .keyFlag(true).checkFlag(false).build();
        when(routeProcessService.resolveCurrentRouteProcess(null, routeId, processId))
                .thenReturn(routeProcess);

        // 调用，应该抛异常
        assertThrows(Exception.class, () ->
                feedbackService.approveFeedback(feedback.getId()));

        // 断言：不应该执行任何后续操作
        verify(productProduceService, never()).generateProductProduce(any(), anyBoolean());
        verify(itemConsumeService, never()).generateItemConsume(any());
    }

    @Test
    public void testApproveFeedback_nonKeyCheck_directFinishAndCleanUncheck() {
        // 准备数据：非关键 + 质检工序（!key+check），uncheckQuantity > 0
        // 应放行（checkFlag=true 不拦截 uncheckQuantity），直接完结并清零 uncheckQuantity
        Long routeId = randomLongId();
        Long processId = randomLongId();
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus());
            o.setFeedbackQuantity(BigDecimal.valueOf(20));
            o.setUncheckQuantity(BigDecimal.valueOf(20));
            o.setRouteId(routeId);
            o.setProcessId(processId);
            o.setScheduleOrderId(null);
            o.setScheduleOrderProcessId(null);
        });
        feedbackMapper.insert(feedback);

        // mock: key=false, check=true
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(routeId).processId(processId)
                .keyFlag(false).checkFlag(true).build();
        when(routeProcessService.resolveCurrentRouteProcess(null, routeId, processId))
                .thenReturn(routeProcess);

        // 调用
        boolean result = feedbackService.approveFeedback(feedback.getId());

        // 断言 1：返回 true（直接完成，不走 UNCHECK）
        assertTrue(result);

        // 断言 2：状态为已完成，uncheckQuantity 被清零
        MesProFeedbackDO updated = feedbackMapper.selectById(feedback.getId());
        assertEquals(MesProFeedbackStatusEnum.FINISHED.getStatus(), updated.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(updated.getUncheckQuantity()));

        // 断言 3：非关键工序不生成产出单
        verify(productProduceService, never()).generateProductProduce(any(), anyBoolean());
    }

    @Test
    public void testSubmitFeedback_shouldSyncScheduleProgressWhenLinked() {
        MesProFeedbackDO feedback = randomPojo(MesProFeedbackDO.class, o -> {
            o.setStatus(MesProFeedbackStatusEnum.PREPARE.getStatus());
            o.setScheduleOrderId(9001L);
            o.setSourceImportRecordId(null);
        });
        feedbackMapper.insert(feedback);

        feedbackService.submitFeedback(feedback.getId());

        MesProFeedbackDO updated = feedbackMapper.selectById(feedback.getId());
        assertEquals(MesProFeedbackStatusEnum.APPROVING.getStatus(), updated.getStatus());
        verify(scheduleOrderService).syncFeedbackProgress(9001L);
    }

}
