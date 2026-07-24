package cn.iocoder.yudao.module.mes.service.pro.feedback;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportAttributeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportConfirmBatchReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackSurplusAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackSurplusPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackImportRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackSurplusAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackSurplusPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportPayload;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_FEEDBACK_NOT_PREPARE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_PENDING_EXISTS;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_PENDING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_APPROVER_NOT_UNIQUE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_ITEM_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_TARGET_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_QUANTITY_MUST_POSITIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MesProFeedbackImportRecordServiceImplTest {

    @InjectMocks
    private MesProFeedbackImportRecordServiceImpl service;

    @Mock
    private MesProFeedbackImportRecordMapper importRecordMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskMapper taskMapper;
    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock
    private MesProFeedbackService feedbackService;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProScheduleOrderService scheduleOrderService;
    @Mock
    private MesProFeedbackSurplusPoolMapper surplusPoolMapper;
    @Mock
    private MesProFeedbackSurplusAllocationMapper surplusAllocationMapper;

    private MesProFeedbackImportRecordDO importRecord;
    private ThirdPartyFeedbackImportPayload payload;
    private MesProScheduleOrderDO scheduleOrder;
    private MesProScheduleOrderProcessDO scheduleOrderProcess;
    private MesProWorkOrderDO workOrder;
    private MesMdItemDO item;
    private MesProTaskDO task;

    @BeforeEach
    void setUp() {
        payload = new ThirdPartyFeedbackImportPayload();
        payload.setSheetName("sheet-1");
        payload.setRowNo(2);
        payload.setWorkOrderCode("WO-001");
        payload.setTaskCode("TASK-001");
        payload.setItemCode("ITEM-001");
        payload.setItemName("产品A");
        payload.setSpecification("SPEC-A");
        payload.setProcessCode("PROC-001");
        payload.setProcessName("球囊裁剪");
        payload.setFeedbackQuantity(new BigDecimal("12"));
        payload.setFeedbackTime(LocalDateTime.of(2026, 6, 10, 8, 0));
        payload.setFeedbackUserCode("aoteman");
        payload.setFeedbackUserName("芋道1");
        payload.setApproverName("潘金华");
        importRecord = MesProFeedbackImportRecordDO.builder()
                .id(1L)
                .attributionStatus(ATTRIBUTION_STATUS_PENDING)
                .taskCode("TASK-001")
                .workOrderCode("WO-001")
                .itemCode("ITEM-001")
                .processCode("PROC-001")
                .sourcePayloadJson(JsonUtils.toJsonString(payload))
                .build();
        scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SCH-001")
                .workOrderId(100L)
                .erpWorkOrderCode("WO-001")
                .routeId(400L)
                .productId(1000L)
                .build();
        scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("PROC-001")
                .processName("球囊裁剪")
                .plannedQuantity(new BigDecimal("100"))
                .reportedQuantity(new BigDecimal("10"))
                .remainingQuantity(new BigDecimal("90"))
                .enabled(true)
                .build();
        workOrder = MesProWorkOrderDO.builder().id(100L).code("WO-001").productId(1000L).build();
        item = MesMdItemDO.builder().id(1000L).code("ITEM-001").name("产品A").specification("SPEC-A").build();
        task = MesProTaskDO.builder()
                .id(300L)
                .code("PT-0001")
                .workOrderId(100L)
                .routeId(400L)
                .processId(2000L)
                .workstationId(500L)
                .itemId(1000L)
                .quantity(new BigDecimal("100"))
                .build();
        lenient().when(workstationMapper.selectBatchIds(any())).thenAnswer(invocation -> {
            Collection<Long> workstationIds = invocation.getArgument(0);
            return workstationIds.stream()
                    .map(id -> MesMdWorkstationDO.builder().id(id).code("WS-" + id).build())
                    .toList();
        });
        lenient().when(routeProcessService.getProcessIdentityMap(any())).thenAnswer(invocation -> {
            Collection<Long> processIds = invocation.getArgument(0);
            Map<Long, Long> result = new LinkedHashMap<>();
            if (processIds == null) {
                return result;
            }
            processIds.forEach(id -> result.put(id, id));
            return result;
        });
    }

    @Test
    void getAttributionCandidates_shouldReturnOnlySameProcessWithRemainingAndSortExactWorkOrderFirst() {
        MesProScheduleOrderDO nonExactScheduleOrder = MesProScheduleOrderDO.builder()
                .id(11L)
                .code("SCH-002")
                .workOrderId(101L)
                .erpWorkOrderCode("WO-002")
                .productId(1000L)
                .build();
        MesProScheduleOrderDO zeroRemainingScheduleOrder = MesProScheduleOrderDO.builder()
                .id(12L)
                .code("SCH-003")
                .workOrderId(102L)
                .erpWorkOrderCode("WO-003")
                .productId(1000L)
                .build();
        MesProScheduleOrderProcessDO nonExactProcess = MesProScheduleOrderProcessDO.builder()
                .id(21L)
                .scheduleOrderId(11L)
                .processId(2000L)
                .plannedQuantity(new BigDecimal("100"))
                .reportedQuantity(new BigDecimal("95"))
                .remainingQuantity(new BigDecimal("5"))
                .enabled(true)
                .build();
        MesProScheduleOrderProcessDO zeroRemainingProcess = MesProScheduleOrderProcessDO.builder()
                .id(22L)
                .scheduleOrderId(12L)
                .processId(2000L)
                .plannedQuantity(new BigDecimal("100"))
                .reportedQuantity(new BigDecimal("100"))
                .remainingQuantity(BigDecimal.ZERO)
                .enabled(true)
                .build();
        MesProTaskDO nonExactTask = MesProTaskDO.builder()
                .id(301L)
                .code("PT-0002")
                .workOrderId(101L)
                .routeId(400L)
                .processId(2000L)
                .workstationId(501L)
                .itemId(1000L)
                .quantity(new BigDecimal("100"))
                .build();
        MesProTaskDO zeroTask = MesProTaskDO.builder().id(302L).code("PT-0003").build();

        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(processMapper.selectByCode("PROC-001")).thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(new BigDecimal("9"));
        when(scheduleOrderProcessMapper.selectListByProcessIdsOrZeroSnapshots(List.of(2000L))).thenReturn(List.of(
                nonExactProcess, zeroRemainingProcess, scheduleOrderProcess));
        when(scheduleOrderMapper.selectListByIds(List.of(11L, 12L, 10L))).thenReturn(List.of(
                nonExactScheduleOrder, zeroRemainingScheduleOrder, scheduleOrder));
        when(workOrderMapper.selectListByIds(List.of(101L, 102L, 100L))).thenReturn(List.of(
                MesProWorkOrderDO.builder().id(101L).code("WO-002").productId(1000L).build(),
                MesProWorkOrderDO.builder().id(102L).code("WO-003").productId(1000L).build(),
                workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(21L, 22L, 20L)))
                .thenReturn(List.of(
                        MesProTaskScheduleExtDO.builder().taskId(301L).scheduleOrderProcessId(21L).build(),
                        MesProTaskScheduleExtDO.builder().taskId(302L).scheduleOrderProcessId(22L).build(),
                        MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(301L, 302L, 300L))).thenReturn(List.of(nonExactTask, zeroTask, task));

        var candidates = service.getAttributionCandidates(1L);

        assertEquals(3, candidates.size());
        assertEquals(20L, candidates.get(0).getScheduleOrderProcessId());
        assertTrue(Boolean.TRUE.equals(candidates.get(0).getExactWorkOrderMatch()));
        assertEquals(new BigDecimal("9"), candidates.get(0).getSurplusPoolQuantity());
        assertEquals(new BigDecimal("21"), candidates.get(0).getAvailableFeedbackQuantity());
        assertEquals(21L, candidates.get(1).getScheduleOrderProcessId());
        assertFalse(Boolean.TRUE.equals(candidates.get(1).getExactWorkOrderMatch()));
        assertEquals(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_EXTERNAL_OTHER_ORDER, candidates.get(2).getTargetType());
        assertTrue(Boolean.TRUE.equals(candidates.get(2).getExternalOtherOrder()));
        assertEquals(new BigDecimal("9"), candidates.get(2).getSurplusPoolQuantity());
        assertFalse(candidates.stream().anyMatch(candidate -> Long.valueOf(22L).equals(candidate.getScheduleOrderProcessId())));
        verify(importRecordMapper, never()).updateById(any(MesProFeedbackImportRecordDO.class));
    }

    @Test
    void getAttributionCandidates_shouldExcludeProcessWithoutActiveTask() {
        MesProScheduleOrderDO noTaskScheduleOrder = MesProScheduleOrderDO.builder()
                .id(13L)
                .code("SCH-004")
                .workOrderId(103L)
                .erpWorkOrderCode("WO-004")
                .productId(1000L)
                .build();
        MesProScheduleOrderProcessDO noTaskProcess = MesProScheduleOrderProcessDO.builder()
                .id(23L)
                .scheduleOrderId(13L)
                .processId(2000L)
                .processCode("PROC-001")
                .processName("球囊裁剪")
                .plannedQuantity(new BigDecimal("100"))
                .reportedQuantity(new BigDecimal("92"))
                .remainingQuantity(new BigDecimal("8"))
                .enabled(true)
                .build();

        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(processMapper.selectByCode("PROC-001")).thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(scheduleOrderProcessMapper.selectListByProcessIdsOrZeroSnapshots(List.of(2000L))).thenReturn(List.of(scheduleOrderProcess, noTaskProcess));
        when(scheduleOrderMapper.selectListByIds(List.of(10L, 13L))).thenReturn(List.of(scheduleOrder, noTaskScheduleOrder));
        when(workOrderMapper.selectListByIds(List.of(100L, 103L))).thenReturn(List.of(
                workOrder,
                MesProWorkOrderDO.builder().id(103L).code("WO-004").productId(1000L).build()));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L, 23L)))
                .thenReturn(List.of(
                        MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));

        var candidates = service.getAttributionCandidates(1L);

        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().anyMatch(candidate -> Long.valueOf(20L).equals(candidate.getScheduleOrderProcessId())));
        assertFalse(candidates.stream().anyMatch(candidate -> Long.valueOf(23L).equals(candidate.getScheduleOrderProcessId())));
        assertEquals(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_EXTERNAL_OTHER_ORDER,
                candidates.get(1).getTargetType());
    }

    @Test
    void getAttributionCandidates_shouldIncludeDuplicateProcessCodeAndDifferentProductOrders() {
        MesProScheduleOrderDO duplicateProcessScheduleOrder = MesProScheduleOrderDO.builder()
                .id(13L)
                .code("SCH-004")
                .workOrderId(103L)
                .erpWorkOrderCode("WO-004")
                .productId(1001L)
                .build();
        MesProScheduleOrderProcessDO duplicateCodeProcess = MesProScheduleOrderProcessDO.builder()
                .id(23L)
                .scheduleOrderId(13L)
                .processId(2001L)
                .processCode("PROC-001")
                .processName("球囊裁剪")
                .plannedQuantity(new BigDecimal("100"))
                .reportedQuantity(new BigDecimal("93"))
                .remainingQuantity(new BigDecimal("7"))
                .enabled(true)
                .build();
        MesMdItemDO otherItem = MesMdItemDO.builder()
                .id(1001L)
                .code("ITEM-OTHER")
                .name("产品B")
                .specification("SPEC-B")
                .build();
        MesProTaskDO duplicateTask = MesProTaskDO.builder()
                .id(303L)
                .code("PT-0004")
                .workOrderId(103L)
                .routeId(400L)
                .processId(2001L)
                .workstationId(503L)
                .itemId(1001L)
                .quantity(new BigDecimal("100"))
                .build();

        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").name("球囊裁剪").build());
        when(processMapper.selectListByCodes(List.of("PROC-001")))
                .thenReturn(List.of(
                        cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                                .id(2000L).code("PROC-001").name("球囊裁剪").build(),
                        cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                                .id(2001L).code("PROC-001").name("球囊裁剪").build()));
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(scheduleOrderProcessMapper.selectListByProcessIdsOrZeroSnapshots(List.of(2000L, 2001L)))
                .thenReturn(List.of(scheduleOrderProcess, duplicateCodeProcess));
        when(scheduleOrderMapper.selectListByIds(List.of(10L, 13L)))
                .thenReturn(List.of(scheduleOrder, duplicateProcessScheduleOrder));
        when(workOrderMapper.selectListByIds(List.of(100L, 103L)))
                .thenReturn(List.of(workOrder, MesProWorkOrderDO.builder().id(103L).code("WO-004").productId(1001L).build()));
        when(itemMapper.selectListByIds(List.of(1000L, 1001L))).thenReturn(List.of(item, otherItem));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L, 23L)))
                .thenReturn(List.of(
                        MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build(),
                        MesProTaskScheduleExtDO.builder().taskId(303L).scheduleOrderProcessId(23L).build()));
        when(taskMapper.selectListByIds(List.of(300L, 303L))).thenReturn(List.of(task, duplicateTask));

        var candidates = service.getAttributionCandidates(1L);

        assertEquals(3, candidates.size());
        assertTrue(candidates.stream().anyMatch(candidate -> Long.valueOf(23L).equals(candidate.getScheduleOrderProcessId())
                && MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER.equals(candidate.getTargetType())
                && "ITEM-OTHER".equals(candidate.getItemCode())));
        assertEquals(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_EXTERNAL_OTHER_ORDER,
                candidates.get(2).getTargetType());
    }

    @Test
    void getAttributionCandidates_shouldHideFinishedScheduleOrder() {
        scheduleOrder.setStatus(MesProScheduleOrderStatusEnum.FINISHED.getStatus());
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(processMapper.selectByCode("PROC-001")).thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(scheduleOrderProcessMapper.selectListByProcessIdsOrZeroSnapshots(List.of(2000L))).thenReturn(List.of(scheduleOrderProcess));
        when(scheduleOrderMapper.selectListByIds(List.of(10L))).thenReturn(List.of(scheduleOrder));
        when(workOrderMapper.selectListByIds(List.of(100L))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L))).thenReturn(List.of());

        var candidates = service.getAttributionCandidates(1L);

        assertEquals(1, candidates.size());
        assertEquals(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_EXTERNAL_OTHER_ORDER, candidates.get(0).getTargetType());
        assertTrue(Boolean.TRUE.equals(candidates.get(0).getExternalOtherOrder()));
    }

    @Test
    void getImportRecordPage_shouldFilterByRecordIdFeedbackIdAndStatus() {
        var reqVO = new cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setId(18L);
        reqVO.setFeedbackId(118L);
        reqVO.setAttributionStatus("ATTRIBUTED");
        LocalDateTime attributedAt = LocalDateTime.of(2026, 6, 10, 9, 35);
        importRecord.setAttributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED);
        importRecord.setFeedbackId(118L);
        importRecord.setUpdateTime(attributedAt);
        when(importRecordMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(importRecord), 1L));

        var result = service.getImportRecordPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(attributedAt, result.getList().get(0).getAttributionTime());
        verify(importRecordMapper).selectPage(reqVO);
    }

    @Test
    void getImportRecordMapByFeedbackIds_shouldKeepSourceRowForApprovalReview() {
        MesProFeedbackImportRecordDO attributedRecord = MesProFeedbackImportRecordDO.builder()
                .id(19L)
                .feedbackId(719L)
                .attributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED)
                .sourceFileName("A4导入.xlsx")
                .sheetName("Sheet1")
                .rowNo(12)
                .sourcePayloadJson(JsonUtils.toJsonString(payload))
                .build();
        LocalDateTime attributedAt = LocalDateTime.of(2026, 6, 10, 10, 20);
        attributedRecord.setUpdateTime(attributedAt);
        when(importRecordMapper.selectListByFeedbackIds(List.of(719L))).thenReturn(List.of(attributedRecord));

        Map<Long, MesProFeedbackImportRecordDO> result = service.getImportRecordMapByFeedbackIds(List.of(719L));

        assertEquals(1, result.size());
        assertEquals("Sheet1", result.get(719L).getSheetName());
        assertEquals(12, result.get(719L).getRowNo());
        assertEquals(attributedAt, result.get(719L).getUpdateTime());
    }

    @Test
    void attributeImportRecord_shouldResolveCheckFlagFromFrozenScheduleRouteProcess() {
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectListByNicknamesExact(List.of("潘金华")))
                .thenReturn(List.of(AdminUserDO.builder().id(601L).nickname("潘金华").build()));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(true).build());
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        Long feedbackId = service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                .setImportRecordId(1L)
                .setScheduleOrderId(10L)
                .setScheduleOrderProcessId(20L)
                .setFeedbackQuantity(new BigDecimal("8")));

        assertEquals(700L, feedbackId);
        ArgumentCaptor<MesProFeedbackSaveReqVO> feedbackReqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(feedbackReqCaptor.capture());
        assertEquals(new BigDecimal("8"), feedbackReqCaptor.getValue().getFeedbackQuantity());
        assertEquals(BigDecimal.ZERO, feedbackReqCaptor.getValue().getQualifiedQuantity());
        assertEquals(new BigDecimal("8"), feedbackReqCaptor.getValue().getUncheckQuantity());
        assertEquals(10L, feedbackReqCaptor.getValue().getScheduleOrderId());
        assertEquals(20L, feedbackReqCaptor.getValue().getScheduleOrderProcessId());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(600L, 400L, 2000L);
        verify(feedbackService, never()).submitFeedback(700L);
        verify(scheduleOrderProcessMapper).selectById(20L);
        verifyNoMoreInteractions(scheduleOrderProcessMapper);
        ArgumentCaptor<MesProFeedbackDO> feedbackUpdateCaptor = ArgumentCaptor.forClass(MesProFeedbackDO.class);
        verify(feedbackMapper).updateById(feedbackUpdateCaptor.capture());
        assertEquals(700L, feedbackUpdateCaptor.getValue().getId());
        assertEquals(1L, feedbackUpdateCaptor.getValue().getSourceImportRecordId());
        verifyNoInteractions(scheduleOrderService);
        ArgumentCaptor<MesProFeedbackImportRecordDO> recordCaptor = ArgumentCaptor.forClass(MesProFeedbackImportRecordDO.class);
        verify(importRecordMapper).updateById(recordCaptor.capture());
        assertEquals(700L, recordCaptor.getValue().getFeedbackId());
        assertEquals(10L, recordCaptor.getValue().getScheduleOrderId());
        assertEquals(20L, recordCaptor.getValue().getScheduleOrderProcessId());
        verify(surplusPoolMapper, times(1)).sumAvailableQuantityByProcessId(2000L);
        verifyNoInteractions(surplusAllocationMapper);
    }

    @Test
    void attributeImportRecord_shouldCreateFeedbackAndSurplusPoolWhenCurrentOrderQuantityAboveRemaining() {
        payload.setFeedbackQuantity(new BigDecimal("100"));
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectListByNicknamesExact(List.of("潘金华")))
                .thenReturn(List.of(AdminUserDO.builder().id(601L).nickname("潘金华").build()));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);

        Long feedbackId = service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                .setImportRecordId(1L)
                .setScheduleOrderId(10L)
                .setScheduleOrderProcessId(20L)
                .setFeedbackQuantity(new BigDecimal("100")));

        assertEquals(700L, feedbackId);
        ArgumentCaptor<MesProFeedbackSaveReqVO> feedbackReqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(feedbackReqCaptor.capture());
        assertEquals(new BigDecimal("100"), feedbackReqCaptor.getValue().getFeedbackQuantity());
        ArgumentCaptor<MesProFeedbackSurplusPoolDO> poolCaptor = ArgumentCaptor.forClass(MesProFeedbackSurplusPoolDO.class);
        verify(surplusPoolMapper).insert(poolCaptor.capture());
        assertEquals(MesProFeedbackSurplusPoolDO.SOURCE_TYPE_CURRENT_ORDER_OVERPRODUCE, poolCaptor.getValue().getSourceType());
        assertEquals(700L, poolCaptor.getValue().getSourceFeedbackId());
        assertEquals(10L, poolCaptor.getValue().getSourceScheduleOrderId());
        assertEquals(20L, poolCaptor.getValue().getSourceScheduleOrderProcessId());
        assertEquals(2000L, poolCaptor.getValue().getProcessId());
        assertEquals(new BigDecimal("10"), poolCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("10"), poolCaptor.getValue().getAvailableQuantity());
        assertEquals(MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE, poolCaptor.getValue().getStatus());
        verifyNoInteractions(surplusAllocationMapper);
        verify(feedbackService, never()).submitFeedback(700L);
        verifyNoInteractions(scheduleOrderService);
    }

    @Test
    void attributeImportRecord_shouldCreateFeedbackWhenCurrentOrderQuantityEqualsRemaining() {
        payload.setFeedbackQuantity(new BigDecimal("90"));
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectListByNicknamesExact(List.of("潘金华")))
                .thenReturn(List.of(AdminUserDO.builder().id(601L).nickname("潘金华").build()));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);

        Long feedbackId = service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                .setImportRecordId(1L)
                .setScheduleOrderId(10L)
                .setScheduleOrderProcessId(20L)
                .setFeedbackQuantity(new BigDecimal("90")));

        assertEquals(700L, feedbackId);
        ArgumentCaptor<MesProFeedbackSaveReqVO> feedbackReqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(feedbackReqCaptor.capture());
        assertEquals(new BigDecimal("90"), feedbackReqCaptor.getValue().getFeedbackQuantity());
        verify(surplusPoolMapper, times(1)).sumAvailableQuantityByProcessId(2000L);
        verifyNoInteractions(surplusAllocationMapper);
    }

    @Test
    void attributeImportRecord_shouldAssignWholeQuantityToOtherOrderWithoutCreatingFeedback() {
        payload.setFeedbackQuantity(new BigDecimal("250"));
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").name("球囊裁剪").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        doAnswer(invocation -> {
            MesProFeedbackSurplusPoolDO pool = invocation.getArgument(0);
            pool.setId(801L);
            return 1;
        }).when(surplusPoolMapper).insert(any(MesProFeedbackSurplusPoolDO.class));

        Long result = service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_EXTERNAL_OTHER_ORDER)
                .setImportRecordId(1L)
                .setFeedbackQuantity(new BigDecimal("250")));

        assertEquals(0L, result);
        verifyNoInteractions(feedbackService);
        verifyNoInteractions(scheduleOrderService);
        ArgumentCaptor<MesProFeedbackSurplusPoolDO> poolCaptor = ArgumentCaptor.forClass(MesProFeedbackSurplusPoolDO.class);
        verify(surplusPoolMapper).insert(poolCaptor.capture());
        assertEquals(MesProFeedbackSurplusPoolDO.SOURCE_TYPE_EXTERNAL_OTHER_ORDER, poolCaptor.getValue().getSourceType());
        assertEquals(2000L, poolCaptor.getValue().getProcessId());
        assertEquals(new BigDecimal("250"), poolCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("0"), poolCaptor.getValue().getAvailableQuantity());
        ArgumentCaptor<MesProFeedbackSurplusAllocationDO> allocationCaptor =
                ArgumentCaptor.forClass(MesProFeedbackSurplusAllocationDO.class);
        verify(surplusAllocationMapper).insert(allocationCaptor.capture());
        assertEquals(801L, allocationCaptor.getValue().getPoolId());
        assertEquals(MesProFeedbackSurplusAllocationDO.TARGET_TYPE_EXTERNAL_OTHER_ORDER,
                allocationCaptor.getValue().getTargetType());
        assertEquals("其他订单", allocationCaptor.getValue().getTargetOrderLabel());
        assertEquals("其他产品", allocationCaptor.getValue().getTargetProductLabel());
        assertEquals(new BigDecimal("250"), allocationCaptor.getValue().getAllocatedQuantity());
        ArgumentCaptor<MesProFeedbackImportRecordDO> recordCaptor = ArgumentCaptor.forClass(MesProFeedbackImportRecordDO.class);
        verify(importRecordMapper).updateById(recordCaptor.capture());
        assertEquals(ATTRIBUTION_STATUS_ATTRIBUTED, recordCaptor.getValue().getAttributionStatus());
        assertEquals(MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_EXTERNAL_OTHER_ORDER,
                recordCaptor.getValue().getAttributionTargetType());
        assertNull(recordCaptor.getValue().getScheduleOrderId());
        assertNull(recordCaptor.getValue().getScheduleOrderProcessId());
    }

    @Test
    void attributeImportRecord_shouldAttributeMultipleAllocationsInOneRequest() {
        payload.setFeedbackQuantity(new BigDecimal("100"));
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").name("球囊裁剪").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectListByNicknamesExact(List.of("潘金华")))
                .thenReturn(List.of(AdminUserDO.builder().id(601L).nickname("潘金华").build()));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);
        doAnswer(invocation -> {
            MesProFeedbackSurplusPoolDO pool = invocation.getArgument(0);
            pool.setId(802L);
            return 1;
        }).when(surplusPoolMapper).insert(any(MesProFeedbackSurplusPoolDO.class));

        Long feedbackId = service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setImportRecordId(1L)
                .setAllocations(List.of(
                        new MesProFeedbackImportAttributeReqVO.Allocation()
                                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                                .setScheduleOrderId(10L)
                                .setScheduleOrderProcessId(20L)
                                .setFeedbackQuantity(new BigDecimal("90")),
                        new MesProFeedbackImportAttributeReqVO.Allocation()
                                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_EXTERNAL_OTHER_ORDER)
                                .setFeedbackQuantity(new BigDecimal("10"))
                )));

        assertEquals(700L, feedbackId);
        ArgumentCaptor<MesProFeedbackSaveReqVO> feedbackReqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(feedbackReqCaptor.capture());
        assertEquals(new BigDecimal("90"), feedbackReqCaptor.getValue().getFeedbackQuantity());
        ArgumentCaptor<MesProFeedbackSurplusPoolDO> poolCaptor = ArgumentCaptor.forClass(MesProFeedbackSurplusPoolDO.class);
        verify(surplusPoolMapper).insert(poolCaptor.capture());
        assertEquals(MesProFeedbackSurplusPoolDO.SOURCE_TYPE_EXTERNAL_OTHER_ORDER, poolCaptor.getValue().getSourceType());
        assertEquals(new BigDecimal("10"), poolCaptor.getValue().getTotalQuantity());
        ArgumentCaptor<MesProFeedbackSurplusAllocationDO> allocationCaptor =
                ArgumentCaptor.forClass(MesProFeedbackSurplusAllocationDO.class);
        verify(surplusAllocationMapper).insert(allocationCaptor.capture());
        assertEquals(802L, allocationCaptor.getValue().getPoolId());
        assertEquals(new BigDecimal("10"), allocationCaptor.getValue().getAllocatedQuantity());
        ArgumentCaptor<MesProFeedbackImportRecordDO> recordCaptor = ArgumentCaptor.forClass(MesProFeedbackImportRecordDO.class);
        verify(importRecordMapper).updateById(recordCaptor.capture());
        assertEquals(ATTRIBUTION_STATUS_ATTRIBUTED, recordCaptor.getValue().getAttributionStatus());
        assertEquals(700L, recordCaptor.getValue().getFeedbackId());
        assertEquals(MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_CURRENT_ORDER,
                recordCaptor.getValue().getAttributionTargetType());
        assertNull(recordCaptor.getValue().getScheduleOrderId());
        assertNull(recordCaptor.getValue().getScheduleOrderProcessId());
    }

    @Test
    void attributeImportRecord_shouldCreateResidualSurplusPoolWhenAllocationLessThanImportedQuantity() {
        payload.setFeedbackQuantity(new BigDecimal("100"));
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").name("球囊裁剪").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectListByNicknamesExact(List.of("潘金华")))
                .thenReturn(List.of(AdminUserDO.builder().id(601L).nickname("潘金华").build()));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);

        service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setImportRecordId(1L)
                .setAllocations(List.of(
                        new MesProFeedbackImportAttributeReqVO.Allocation()
                                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                                .setScheduleOrderId(10L)
                                .setScheduleOrderProcessId(20L)
                                .setFeedbackQuantity(new BigDecimal("90"))
                )));

        ArgumentCaptor<MesProFeedbackSurplusPoolDO> poolCaptor = ArgumentCaptor.forClass(MesProFeedbackSurplusPoolDO.class);
        verify(surplusPoolMapper).insert(poolCaptor.capture());
        assertEquals(new BigDecimal("10"), poolCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("10"), poolCaptor.getValue().getAvailableQuantity());
        assertEquals(MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE, poolCaptor.getValue().getStatus());
    }

    @Test
    void attributeImportRecord_shouldConsumeExistingSurplusPoolWhenAllocationUsesCache() {
        payload.setFeedbackQuantity(new BigDecimal("5"));
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        MesProFeedbackSurplusPoolDO existingPool = MesProFeedbackSurplusPoolDO.builder()
                .id(901L)
                .processId(2000L)
                .availableQuantity(new BigDecimal("10"))
                .allocatedQuantity(BigDecimal.ZERO)
                .status(MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE)
                .build();
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").name("球囊裁剪").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(new BigDecimal("10"));
        when(surplusPoolMapper.selectAvailableListByProcessId(2000L)).thenReturn(List.of(existingPool));
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectListByNicknamesExact(List.of("潘金华")))
                .thenReturn(List.of(AdminUserDO.builder().id(601L).nickname("潘金华").build()));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);

        service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setImportRecordId(1L)
                .setAllocations(List.of(
                        new MesProFeedbackImportAttributeReqVO.Allocation()
                                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                                .setScheduleOrderId(10L)
                                .setScheduleOrderProcessId(20L)
                                .setFeedbackQuantity(new BigDecimal("12"))
                )));

        ArgumentCaptor<MesProFeedbackSurplusPoolDO> updateCaptor = ArgumentCaptor.forClass(MesProFeedbackSurplusPoolDO.class);
        verify(surplusPoolMapper).updateById(updateCaptor.capture());
        assertEquals(new BigDecimal("3"), updateCaptor.getValue().getAvailableQuantity());
        assertEquals(new BigDecimal("7"), updateCaptor.getValue().getAllocatedQuantity());
        assertEquals(MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE, updateCaptor.getValue().getStatus());
    }

    @Test
    void attributeImportRecord_shouldFailWhenSelectedProcessDoesNotMatchImportProcess() {
        scheduleOrderProcess.setProcessId(9999L);
        scheduleOrderProcess.setProcessCode("PROC-999");
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        lenient().when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.attributeImportRecord(
                new MesProFeedbackImportAttributeReqVO()
                        .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                        .setImportRecordId(1L).setScheduleOrderId(10L)
                        .setScheduleOrderProcessId(20L).setFeedbackQuantity(new BigDecimal("8"))));

        assertEquals(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    void attributeImportRecord_shouldAllowDifferentOrderProductWhenProcessCodeMatches() {
        scheduleOrder.setProductId(1001L);
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        lenient().when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        lenient().when(itemMapper.selectById(1001L))
                .thenReturn(MesMdItemDO.builder().id(1001L).code("ITEM-OTHER").name("产品B").specification("SPEC-B").build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectListByNicknamesExact(List.of("潘金华")))
                .thenReturn(List.of(AdminUserDO.builder().id(601L).nickname("潘金华").build()));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);

        Long feedbackId = service.attributeImportRecord(
                new MesProFeedbackImportAttributeReqVO()
                        .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                        .setImportRecordId(1L).setScheduleOrderId(10L)
                        .setScheduleOrderProcessId(20L).setFeedbackQuantity(new BigDecimal("8")));

        assertEquals(700L, feedbackId);
    }

    @Test
    void attributeImportRecord_shouldResolveApproverByUsernameBeforeNickname() {
        payload.setApproverName("messmokesupervisor");
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectByUsername("messmokesupervisor"))
                .thenReturn(AdminUserDO.builder().id(910260L).username("messmokesupervisor").nickname("eDHR矩阵-审批人").build());
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        Long feedbackId = service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                .setImportRecordId(1L)
                .setScheduleOrderId(10L)
                .setScheduleOrderProcessId(20L)
                .setFeedbackQuantity(new BigDecimal("12")));

        assertEquals(700L, feedbackId);
        verify(adminUserMapper).selectByUsername("messmokesupervisor");
        verify(adminUserMapper, never()).selectListByNicknamesExact(List.of("messmokesupervisor"));
    }

    @Test
    void attributeImportRecord_shouldFailWhenApproverNicknameNotUniqueAndUsernameMissing() {
        payload.setApproverName("eDHR矩阵-审批人");
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectByUsername("eDHR矩阵-审批人")).thenReturn(null);
        when(adminUserMapper.selectListByNicknamesExact(List.of("eDHR矩阵-审批人")))
                .thenReturn(List.of(
                        AdminUserDO.builder().id(910253L).username("mes_smoke_supervisor").nickname("eDHR矩阵-审批人").build(),
                        AdminUserDO.builder().id(910260L).username("messmokesupervisor").nickname("eDHR矩阵-审批人").build()
                ));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.attributeImportRecord(
                new MesProFeedbackImportAttributeReqVO()
                        .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                        .setImportRecordId(1L).setScheduleOrderId(10L)
                        .setScheduleOrderProcessId(20L).setFeedbackQuantity(new BigDecimal("12"))));

        assertEquals(PRO_FEEDBACK_IMPORT_APPROVER_NOT_UNIQUE.getCode(), ex.getCode());
    }

    @Test
    void attributeImportRecord_shouldFailWhenSubmittedQuantityNotPositive() {
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.attributeImportRecord(
                new MesProFeedbackImportAttributeReqVO()
                        .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                        .setImportRecordId(1L).setScheduleOrderId(10L)
                        .setScheduleOrderProcessId(20L).setFeedbackQuantity(BigDecimal.ZERO)));

        assertEquals(PRO_FEEDBACK_QUANTITY_MUST_POSITIVE.getCode(), ex.getCode());
    }

    @Test
    void attributeImportRecord_shouldRejectWhenCurrentOrderHasNoRemainingQuantity() {
        scheduleOrderProcess.setRemainingQuantity(BigDecimal.ZERO);
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.attributeImportRecord(
                new MesProFeedbackImportAttributeReqVO()
                        .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                        .setImportRecordId(1L)
                        .setScheduleOrderId(10L)
                        .setScheduleOrderProcessId(20L)
                        .setFeedbackQuantity(new BigDecimal("6"))));

        assertEquals(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH.getCode(), ex.getCode());
        verifyNoInteractions(feedbackService);
    }

    @Test
    void attributeImportRecord_shouldRejectFinishedTaskAsNoActiveTask() {
        task.setStatus(MesProTaskStatusEnum.FINISHED.getStatus());
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.attributeImportRecord(
                new MesProFeedbackImportAttributeReqVO()
                        .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                        .setImportRecordId(1L)
                        .setScheduleOrderId(10L)
                        .setScheduleOrderProcessId(20L)
                        .setFeedbackQuantity(new BigDecimal("6"))));

        assertEquals(PRO_FEEDBACK_IMPORT_TARGET_TASK_NOT_EXISTS.getCode(), ex.getCode());
        verifyNoInteractions(feedbackService);
    }

    @Test
    void attributeImportRecord_shouldResolveZeroScheduleProcessIdByFrozenRouteProcess() {
        scheduleOrderProcess.setProcessId(0L);
        task.setProcessId(0L);
        payload.setFeedbackQuantity(new BigDecimal("100"));
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        lenient().when(workstationMapper.selectBatchIds(List.of(500L)))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(500L).code("WS-001").build()));
        when(adminUserMapper.selectByUsername("aoteman")).thenReturn(AdminUserDO.builder().id(600L).username("aoteman").build());
        when(adminUserMapper.selectListByNicknamesExact(List.of("潘金华")))
                .thenReturn(List.of(AdminUserDO.builder().id(601L).nickname("潘金华").build()));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 0L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        lenient().when(routeProcessService.getProcessIdentityMap(any()))
                .thenReturn(Map.of(2000L, 2000L));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        when(feedbackService.createFeedbackWithScheduleSnapshot(any())).thenReturn(700L);

        Long feedbackId = service.attributeImportRecord(new MesProFeedbackImportAttributeReqVO()
                .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                .setImportRecordId(1L)
                .setScheduleOrderId(10L)
                .setScheduleOrderProcessId(20L)
                .setFeedbackQuantity(new BigDecimal("100")));

        assertEquals(700L, feedbackId);
        ArgumentCaptor<MesProFeedbackSaveReqVO> feedbackReqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(feedbackReqCaptor.capture());
        assertEquals(2000L, feedbackReqCaptor.getValue().getProcessId());
        ArgumentCaptor<MesProFeedbackSurplusPoolDO> poolCaptor = ArgumentCaptor.forClass(MesProFeedbackSurplusPoolDO.class);
        verify(surplusPoolMapper).insert(poolCaptor.capture());
        assertEquals(2000L, poolCaptor.getValue().getProcessId());
    }

    @Test
    void attributeImportRecord_shouldRejectTaskWhoseWorkstationIsMissing() {
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        lenient().when(workstationMapper.selectBatchIds(List.of(500L))).thenReturn(List.of());
        lenient().when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        lenient().when(routeProcessService.getProcessIdentityMap(any()))
                .thenReturn(Map.of(2000L, 2000L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.attributeImportRecord(
                new MesProFeedbackImportAttributeReqVO()
                        .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                        .setImportRecordId(1L)
                        .setScheduleOrderId(10L)
                        .setScheduleOrderProcessId(20L)
                        .setFeedbackQuantity(new BigDecimal("6"))));

        assertEquals(PRO_FEEDBACK_IMPORT_TARGET_TASK_NOT_EXISTS.getCode(), ex.getCode());
        verifyNoInteractions(feedbackService);
    }

    @Test
    void attributeImportRecord_shouldRejectAmbiguousActiveTasksWhenTaskCodeDoesNotMatch() {
        MesProTaskDO anotherTask = MesProTaskDO.builder()
                .id(301L)
                .code("PT-0002")
                .workOrderId(100L)
                .routeId(400L)
                .processId(2000L)
                .workstationId(501L)
                .itemId(1000L)
                .quantity(new BigDecimal("100"))
                .build();
        when(importRecordMapper.selectById(1L)).thenReturn(importRecord);
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(
                        MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build(),
                        MesProTaskScheduleExtDO.builder().taskId(301L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L, 301L))).thenReturn(List.of(task, anotherTask));
        lenient().when(workstationMapper.selectBatchIds(List.of(500L, 501L)))
                .thenReturn(List.of(
                        MesMdWorkstationDO.builder().id(500L).code("WS-001").build(),
                        MesMdWorkstationDO.builder().id(501L).code("WS-002").build()));
        lenient().when(routeProcessService.resolveFrozenRouteProcess(600L, 400L, 2000L))
                .thenReturn(MesProRouteProcessDO.builder().routeId(400L).processId(2000L).checkFlag(false).build());
        lenient().when(routeProcessService.getProcessIdentityMap(any()))
                .thenReturn(Map.of(2000L, 2000L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.attributeImportRecord(
                new MesProFeedbackImportAttributeReqVO()
                        .setTargetType(MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER)
                        .setImportRecordId(1L)
                        .setScheduleOrderId(10L)
                        .setScheduleOrderProcessId(20L)
                        .setFeedbackQuantity(new BigDecimal("6"))));

        assertEquals(PRO_FEEDBACK_IMPORT_TARGET_TASK_NOT_EXISTS.getCode(), ex.getCode());
        verifyNoInteractions(feedbackService);
    }

    @Test
    void getImportRecordPage_shouldExposeLinkedFeedbackDraftFieldsAndBatchSummary() {
        var reqVO = new cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setImportRecordIds(List.of(1L));
        importRecord.setAttributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED);
        importRecord.setAttributionTargetType(MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_CURRENT_ORDER);
        importRecord.setFeedbackId(700L);
        importRecord.setSourceFileName("第三方报工-批次A.xlsx");
        importRecord.setUpdateTime(LocalDateTime.of(2026, 6, 26, 10, 15));
        MesProFeedbackDO linkedFeedback = MesProFeedbackDO.builder()
                .id(700L)
                .sourceImportRecordId(1L)
                .feedbackUserId(600L)
                .approveUserId(601L)
                .feedbackTime(LocalDateTime.of(2026, 6, 10, 8, 0))
                .remark("草稿备注")
                .status(MesProFeedbackStatusEnum.PREPARE.getStatus())
                .build();
        when(importRecordMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(importRecord), 1L));
        when(importRecordMapper.selectListByIds(List.of(1L))).thenReturn(List.of(importRecord));
        when(feedbackMapper.selectListBySourceImportRecordIds(List.of(1L))).thenReturn(List.of(linkedFeedback));
        when(adminUserMapper.selectById(600L)).thenReturn(AdminUserDO.builder().id(600L).nickname("奥特曼").build());
        when(adminUserMapper.selectById(601L)).thenReturn(AdminUserDO.builder().id(601L).nickname("潘金华").build());
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(2000L).code("PROC-001").name("球囊裁剪").build());
        when(surplusPoolMapper.sumAvailableQuantityByProcessId(2000L)).thenReturn(BigDecimal.ZERO);

        var result = service.getImportRecordPage(reqVO);
        var summary = service.getImportRecordBatchSummary(List.of(1L));

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        var row = result.getList().get(0);
        assertEquals(600L, row.getFeedbackUserId());
        assertEquals("奥特曼", row.getFeedbackUserNickname());
        assertEquals(601L, row.getApproveUserId());
        assertEquals("潘金华", row.getApproveUserNickname());
        assertEquals("草稿备注", row.getRemark());
        assertEquals(MesProFeedbackStatusEnum.PREPARE.getStatus(), row.getLinkedFeedbackStatus());
        assertTrue(Boolean.TRUE.equals(row.getGeneratedFeedbackDraft()));
        assertEquals("第三方报工-批次A.xlsx", summary.getSourceFileName());
        assertEquals(1, summary.getTotalCount());
        assertEquals(0, summary.getPendingCount());
        assertEquals(1, summary.getAttributedCount());
        assertEquals(1, summary.getConfirmableCount());
        assertEquals(0, summary.getSkippedOtherOrderCount());
    }

    @Test
    void confirmImportRecordBatch_shouldFailWhenBatchContainsPendingCurrentOrderRecord() {
        MesProFeedbackImportRecordDO pendingRecord = MesProFeedbackImportRecordDO.builder()
                .id(2L)
                .attributionStatus(ATTRIBUTION_STATUS_PENDING)
                .sourcePayloadJson(JsonUtils.toJsonString(payload))
                .build();
        MesProFeedbackImportRecordDO attributedRecord = MesProFeedbackImportRecordDO.builder()
                .id(1L)
                .attributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED)
                .attributionTargetType(MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_CURRENT_ORDER)
                .feedbackId(700L)
                .sourcePayloadJson(JsonUtils.toJsonString(payload))
                .build();
        when(importRecordMapper.selectListByIds(List.of(1L, 2L))).thenReturn(List.of(attributedRecord, pendingRecord));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.confirmImportRecordBatch(
                new MesProFeedbackImportConfirmBatchReqVO()
                        .setImportRecordIds(List.of(1L, 2L))
                        .setRows(List.of(new MesProFeedbackImportConfirmBatchReqVO.Row()
                                .setImportRecordId(1L)
                                .setFeedbackUserId(600L)
                                .setApproveUserId(601L)
                                .setFeedbackTime(LocalDateTime.of(2026, 6, 26, 9, 0))))));

        assertEquals(PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_PENDING_EXISTS.getCode(), ex.getCode());
        verify(feedbackService, never()).submitFeedback(anyLong(), anyBoolean());
    }

    @Test
    void confirmImportRecordBatch_shouldFailWhenLinkedFeedbackNotPrepare() {
        MesProFeedbackImportRecordDO attributedRecord = MesProFeedbackImportRecordDO.builder()
                .id(1L)
                .attributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED)
                .attributionTargetType(MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_CURRENT_ORDER)
                .feedbackId(700L)
                .sourcePayloadJson(JsonUtils.toJsonString(payload))
                .build();
        MesProFeedbackDO linkedFeedback = MesProFeedbackDO.builder()
                .id(700L)
                .sourceImportRecordId(1L)
                .status(MesProFeedbackStatusEnum.APPROVING.getStatus())
                .build();
        when(importRecordMapper.selectListByIds(List.of(1L))).thenReturn(List.of(attributedRecord));
        when(feedbackMapper.selectListBySourceImportRecordIds(List.of(1L))).thenReturn(List.of(linkedFeedback));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.confirmImportRecordBatch(
                new MesProFeedbackImportConfirmBatchReqVO()
                        .setImportRecordIds(List.of(1L))
                        .setRows(List.of(new MesProFeedbackImportConfirmBatchReqVO.Row()
                                .setImportRecordId(1L)
                                .setFeedbackUserId(600L)
                                .setApproveUserId(601L)
                                .setFeedbackTime(LocalDateTime.of(2026, 6, 26, 9, 0))))));

        assertEquals(PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_FEEDBACK_NOT_PREPARE.getCode(), ex.getCode());
        verify(feedbackService, never()).submitFeedback(anyLong(), anyBoolean());
    }

    @Test
    void confirmImportRecordBatch_shouldSkipExternalOtherOrderAndSubmitCurrentOrderDrafts() {
        MesProFeedbackImportRecordDO currentOrderRecord = MesProFeedbackImportRecordDO.builder()
                .id(1L)
                .attributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED)
                .attributionTargetType(MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_CURRENT_ORDER)
                .feedbackId(700L)
                .remark("原备注")
                .sourcePayloadJson(JsonUtils.toJsonString(payload))
                .build();
        MesProFeedbackImportRecordDO externalOtherOrderRecord = MesProFeedbackImportRecordDO.builder()
                .id(2L)
                .attributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED)
                .attributionTargetType(MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_EXTERNAL_OTHER_ORDER)
                .feedbackId(null)
                .sourcePayloadJson(JsonUtils.toJsonString(payload))
                .build();
        MesProFeedbackDO linkedFeedback = MesProFeedbackDO.builder()
                .id(700L)
                .sourceImportRecordId(1L)
                .status(MesProFeedbackStatusEnum.PREPARE.getStatus())
                .build();
        when(importRecordMapper.selectListByIds(List.of(1L, 2L))).thenReturn(List.of(currentOrderRecord, externalOtherOrderRecord));
        when(feedbackMapper.selectListBySourceImportRecordIds(List.of(1L, 2L))).thenReturn(List.of(linkedFeedback));

        service.confirmImportRecordBatch(new MesProFeedbackImportConfirmBatchReqVO()
                .setImportRecordIds(List.of(1L, 2L))
                .setRows(List.of(new MesProFeedbackImportConfirmBatchReqVO.Row()
                        .setImportRecordId(1L)
                        .setFeedbackUserId(600L)
                        .setApproveUserId(601L)
                        .setFeedbackTime(LocalDateTime.of(2026, 6, 26, 9, 0))
                        .setRemark("确认备注"))));

        ArgumentCaptor<MesProFeedbackDO> feedbackUpdateCaptor = ArgumentCaptor.forClass(MesProFeedbackDO.class);
        verify(feedbackMapper).updateById(feedbackUpdateCaptor.capture());
        assertEquals(700L, feedbackUpdateCaptor.getValue().getId());
        assertEquals(600L, feedbackUpdateCaptor.getValue().getFeedbackUserId());
        assertEquals(601L, feedbackUpdateCaptor.getValue().getApproveUserId());
        assertEquals("确认备注", feedbackUpdateCaptor.getValue().getRemark());
        verify(feedbackService).submitFeedback(700L, true);
        verify(feedbackService, never()).submitFeedback(eq(0L), anyBoolean());
    }
}
