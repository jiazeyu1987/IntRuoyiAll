package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackImportRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.FeedbackScheduleLinkageGuard;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_SIMULATE_SOURCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_SIMULATE_SOURCE_NOT_ENOUGH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyFeedbackImportServiceImplTest {

    @InjectMocks
    private ThirdPartyFeedbackImportServiceImpl service;

    @Spy
    private ThirdPartyFeedbackExcelParser parser = new ThirdPartyFeedbackExcelParser();
    @Mock
    private MesProFeedbackImportRecordMapper importRecordMapper;
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
    private MesProProcessMapper processMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskMapper taskMapper;
    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;

    @BeforeEach
    void stubCurrentRouteProcessIdentity() {
        lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        lenient().when(routeProcessService.resolveFrozenRouteProcess(
                        anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        lenient().when(routeProcessService.getProcessIdentityMap(any()))
                .thenAnswer(invocation -> {
                    java.util.Collection<Long> processIds = invocation.getArgument(0);
                    return processIds.stream()
                            .collect(java.util.stream.Collectors.toMap(id -> id, id -> id));
                });
    }
    @Mock
    private MesProFeedbackService feedbackService;
    @Mock
    private MesProFeedbackMapper feedbackMapper;

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: direct work report Excel now matches enabled schedule processes directly")
    void isEnabledScheduleRouteProcess_shouldUseFrozenScheduleProcessConfigWithoutCurrentRemap() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .routeId(20L)
                .build();
        MesProScheduleOrderProcessDO scheduleProcess = MesProScheduleOrderProcessDO.builder()
                .id(30L)
                .scheduleOrderId(10L)
                .routeProcessId(99L)
                .processId(1000L)
                .build();
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(99L)
                .routeId(20L)
                .processId(1000L)
                .build();
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .id(800L)
                .routeId(20L)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO historicalConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(801L)
                .routeFlowConfigId(800L)
                .routeId(20L)
                .routeProcessId(99L)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                20L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(flowConfig);
        when(routeProcessService.resolveFrozenRouteProcess(99L, 20L, 1000L))
                .thenReturn(frozenRouteProcess);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                99L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(historicalConfig);

        Boolean result = ReflectionTestUtils.invokeMethod(service,
                "isEnabledScheduleRouteProcess", scheduleOrder, scheduleProcess);

        assertEquals(Boolean.TRUE, result);
        verify(routeProcessService, never()).resolveCurrentRouteProcess(99L, 20L, 1000L);
        verify(routeFlowProcessConfigMapper, never()).selectListByRouteIdAndUseType(
                20L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
    }

    @Spy
    private FeedbackScheduleLinkageGuard feedbackScheduleLinkageGuard = new FeedbackScheduleLinkageGuard();
    @Test
    void simulateImportWorkbook_shouldGeneratePendingImportRecordFromRealSource() {
        AdminUserDO currentUser = AdminUserDO.builder().id(1L).username("aoteman").nickname("芋道1").build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .processId(2000L)
                .remainingQuantity(new BigDecimal("8"))
                .enabled(true)
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .erpWorkOrderCode("WO-001")
                .workOrderId(100L)
                .productId(1000L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder().id(100L).productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("ITEM-001").name("产品A").specification("SPEC-A").build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("PROC-001").name("球囊裁剪").build();
        MesProTaskDO task = MesProTaskDO.builder().id(300L).code("TASK-001").build();

        when(adminUserMapper.selectById(1L)).thenReturn(currentUser);
        when(scheduleOrderProcessMapper.selectList(any())).thenReturn(List.of(scheduleOrderProcess));
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workOrderMapper.selectById(100L)).thenReturn(workOrder);
        when(itemMapper.selectById(1000L)).thenReturn(item);
        when(processMapper.selectById(2000L)).thenReturn(process);
        when(importRecordMapper.selectBySourceFingerprint(any(), any(), any())).thenReturn(null);
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);

            ThirdPartyFeedbackImportResult result = service.simulateImportWorkbook(1);

            assertEquals(1, result.getSheetCount());
            assertEquals(1, result.getImportedCount());
            assertEquals(1, result.getPendingCount());
            assertEquals(List.of(700L), result.getImportRecordIds());
        }

        ArgumentCaptor<MesProFeedbackImportRecordDO> recordCaptor = ArgumentCaptor.forClass(MesProFeedbackImportRecordDO.class);
        org.mockito.Mockito.verify(importRecordMapper).insert(recordCaptor.capture());
        MesProFeedbackImportRecordDO record = recordCaptor.getValue();
        assertTrue(record.getSourceFileName().startsWith("simulated-third-party-feedback-"));
        ThirdPartyFeedbackImportPayload payload = JsonUtils.parseObject(record.getSourcePayloadJson(), ThirdPartyFeedbackImportPayload.class);
        assertEquals("aoteman", payload.getFeedbackUserCode());
        assertEquals("aoteman", payload.getApproverName());
        assertTrue(payload.getFeedbackQuantity().compareTo(new BigDecimal("100")) >= 0);
        assertTrue(payload.getFeedbackQuantity().compareTo(new BigDecimal("10000")) <= 0);
        assertTrue(payload.getFeedbackQuantity().stripTrailingZeros().scale() <= 0);
        assertEquals("WO-001", payload.getWorkOrderCode());
        assertEquals("TASK-001", payload.getTaskCode());
    }

    @Test
    void simulateImportWorkbook_shouldGenerateRequestedPendingImportRecordsFromDistinctSources() {
        AdminUserDO currentUser = AdminUserDO.builder().id(1L).username("aoteman").nickname("芋道1").build();
        MesProScheduleOrderProcessDO processA = MesProScheduleOrderProcessDO.builder()
                .id(20L).scheduleOrderId(10L).processId(2000L)
                .remainingQuantity(new BigDecimal("8")).enabled(true).build();
        MesProScheduleOrderProcessDO processB = MesProScheduleOrderProcessDO.builder()
                .id(21L).scheduleOrderId(11L).processId(2001L)
                .remainingQuantity(new BigDecimal("3")).enabled(true).build();
        MesProScheduleOrderDO orderA = MesProScheduleOrderDO.builder()
                .id(10L).erpWorkOrderCode("WO-001").workOrderId(100L).productId(1000L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO orderB = MesProScheduleOrderDO.builder()
                .id(11L).erpWorkOrderCode("WO-002").workOrderId(101L).productId(1001L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProWorkOrderDO workOrderA = MesProWorkOrderDO.builder().id(100L).productId(1000L).build();
        MesProWorkOrderDO workOrderB = MesProWorkOrderDO.builder().id(101L).productId(1001L).build();
        MesMdItemDO itemA = MesMdItemDO.builder().id(1000L).code("ITEM-001").name("产品A").specification("SPEC-A").build();
        MesMdItemDO itemB = MesMdItemDO.builder().id(1001L).code("ITEM-002").name("产品B").specification("SPEC-B").build();
        MesProProcessDO proProcessA = MesProProcessDO.builder().id(2000L).code("PROC-001").name("球囊裁剪").build();
        MesProProcessDO proProcessB = MesProProcessDO.builder().id(2001L).code("PROC-002").name("焊接").build();
        MesProTaskDO taskA = MesProTaskDO.builder().id(300L).code("TASK-001").build();
        MesProTaskDO taskB = MesProTaskDO.builder().id(301L).code("TASK-002").build();

        when(adminUserMapper.selectById(1L)).thenReturn(currentUser);
        when(scheduleOrderProcessMapper.selectList(any())).thenReturn(List.of(processA, processB));
        when(scheduleOrderMapper.selectById(10L)).thenReturn(orderA);
        when(scheduleOrderMapper.selectById(11L)).thenReturn(orderB);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(21L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(301L).scheduleOrderProcessId(21L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(taskA));
        when(taskMapper.selectListByIds(List.of(301L))).thenReturn(List.of(taskB));
        when(workOrderMapper.selectById(100L)).thenReturn(workOrderA);
        when(workOrderMapper.selectById(101L)).thenReturn(workOrderB);
        when(itemMapper.selectById(1000L)).thenReturn(itemA);
        when(itemMapper.selectById(1001L)).thenReturn(itemB);
        when(processMapper.selectById(2000L)).thenReturn(proProcessA);
        when(processMapper.selectById(2001L)).thenReturn(proProcessB);
        when(importRecordMapper.selectBySourceFingerprint(any(), any(), any())).thenReturn(null);
        AtomicLong idSequence = new AtomicLong(700L);
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(idSequence.getAndIncrement());
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));

        ThirdPartyFeedbackImportResult result;
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);

            result = service.simulateImportWorkbook(2);
        }

        assertEquals(1, result.getSheetCount());
        assertEquals(2, result.getImportedCount());
        assertEquals(2, result.getPendingCount());
        assertEquals(List.of(700L, 701L), result.getImportRecordIds());
        ArgumentCaptor<MesProFeedbackImportRecordDO> recordCaptor = ArgumentCaptor.forClass(MesProFeedbackImportRecordDO.class);
        org.mockito.Mockito.verify(importRecordMapper, times(2)).insert(recordCaptor.capture());
        Set<String> taskCodes = recordCaptor.getAllValues().stream()
                .map(record -> JsonUtils.parseObject(record.getSourcePayloadJson(), ThirdPartyFeedbackImportPayload.class))
                .map(ThirdPartyFeedbackImportPayload::getTaskCode)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("TASK-001", "TASK-002"), taskCodes);
        for (MesProFeedbackImportRecordDO record : recordCaptor.getAllValues()) {
            BigDecimal simulatedQuantity = JsonUtils.parseObject(record.getSourcePayloadJson(),
                    ThirdPartyFeedbackImportPayload.class).getFeedbackQuantity();
            assertTrue(simulatedQuantity.compareTo(new BigDecimal("100")) >= 0);
            assertTrue(simulatedQuantity.compareTo(new BigDecimal("10000")) <= 0);
            assertTrue(simulatedQuantity.stripTrailingZeros().scale() <= 0);
        }
    }

    @Test
    void importDirectWorkReportWorkbook_shouldSkipWithoutDirectProgressWhenFeedbackUserMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .quantity(new BigDecimal("1000"))
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(BigDecimal.ZERO.setScale(6))
                .remainingQuantity(new BigDecimal("1000.000000"))
                .progressPercent(BigDecimal.ZERO.setScale(6))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(adminUserMapper.selectByUsername("A2020002")).thenReturn(null);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(0, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(0, result.getSubmittedCount());
        assertEquals(List.of(), result.getFeedbackCodes());
        assertEquals(List.of(), result.getImportRecordIds());
        assertEquals(List.of(), result.getDirectWorkReportDetails());
        assertEquals(2, result.getSkippedRows());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning =
                result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("FEEDBACK_USER_NOT_FOUND", warning.getReasonCode());
        assertEquals("A2020002", warning.getFeedbackUserCode());
        assertEquals("881MO093613", warning.getWorkOrderCode());
        assertEquals("Z2570", warning.getProcessCode());
        verify(importRecordMapper, never()).insert(any(MesProFeedbackImportRecordDO.class));
        verify(scheduleOrderProcessMapper, never()).updateProgress(anyLong(), any(), any(), any());
        verify(feedbackService, never()).createFeedbackWithScheduleSnapshot(any());
        verify(feedbackService, never()).submitFeedback(anyLong(), eq(true));
    }

    @Test
    void importDirectWorkReportWorkbook_shouldCreateSubmittedFeedbackAndLinkImportRecordForMatchedRow() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .quantity(new BigDecimal("1000"))
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(BigDecimal.ZERO.setScale(6))
                .remainingQuantity(new BigDecimal("1000.000000"))
                .progressPercent(BigDecimal.ZERO.setScale(6))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        lenient().when(adminUserMapper.selectByUsername("A2020002")).thenReturn(feedbackUser);
        lenient().when(adminUserMapper.selectByUsername("李萍")).thenReturn(null);
        lenient().when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        lenient().when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(10L)).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        lenient().when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        lenient().when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        lenient().when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode()))
                .thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        lenient().when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        lenient().when(importRecordMapper.selectAppliedDirectProgressListByScheduleOrderId(10L)).thenReturn(List.of());
        lenient().when(feedbackMapper.selectProgressListByScheduleOrderId(10L)).thenReturn(List.of(
                MesProFeedbackDO.builder()
                        .id(900L)
                        .scheduleOrderId(10L)
                        .scheduleOrderProcessId(20L)
                        .feedbackQuantity(new BigDecimal("213"))
                        .status(MesProFeedbackStatusEnum.APPROVING.getStatus())
                        .build()));

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getImportedCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(List.of("FB-001"), result.getFeedbackCodes());
        assertEquals(List.of(700L), result.getImportRecordIds());
        ThirdPartyFeedbackImportResult.DirectWorkReportDetail detail = result.getDirectWorkReportDetails().get(0);
        assertEquals("FB-001", detail.getFeedbackCode());
        assertEquals(new BigDecimal("213.000000"), detail.getAfterReportedQuantity());
        ArgumentCaptor<MesProFeedbackSaveReqVO> reqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(reqCaptor.capture());
        MesProFeedbackSaveReqVO req = reqCaptor.getValue();
        assertEquals("FB-001", req.getCode());
        assertEquals(100L, req.getWorkOrderId());
        assertEquals(300L, req.getTaskId());
        assertEquals(10L, req.getScheduleOrderId());
        assertEquals(20L, req.getScheduleOrderProcessId());
        assertEquals(1L, req.getFeedbackUserId());
        assertEquals(2L, req.getApproveUserId());
        assertEquals(0, new BigDecimal("213").compareTo(req.getFeedbackQuantity()));
        verify(feedbackMapper).updateById(org.mockito.ArgumentMatchers.argThat((MesProFeedbackDO feedback) ->
                Long.valueOf(900L).equals(feedback.getId()) && Long.valueOf(700L).equals(feedback.getSourceImportRecordId())));
        verify(feedbackService).submitFeedback(900L, true);
        verify(importRecordMapper).updateById(org.mockito.ArgumentMatchers.argThat((MesProFeedbackImportRecordDO record) ->
                Long.valueOf(700L).equals(record.getId()) && Long.valueOf(900L).equals(record.getFeedbackId())
                        && record.getProgressSourceType() == null && record.getProgressQuantity() == null));
    }

    @Test
    void importDirectWorkReportWorkbook_shouldUseUniqueProcessWorkstationWhenTaskWorkstationMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("PT-50072")
                .workOrderId(100L)
                .workstationId(null)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("1000"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .quantity(new BigDecimal("1000"))
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(BigDecimal.ZERO.setScale(6))
                .remainingQuantity(new BigDecimal("1000.000000"))
                .progressPercent(BigDecimal.ZERO.setScale(6))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(400L).code("WS-400").name("外鞘管组件包装工作站").processId(2000L).build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        lenient().when(adminUserMapper.selectByUsername("A2020002")).thenReturn(feedbackUser);
        lenient().when(adminUserMapper.selectByUsername("李萍")).thenReturn(null);
        lenient().when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        lenient().when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        lenient().when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workstationMapper.selectListByProcessIds(List.of(2000L))).thenReturn(List.of(workstation));
        lenient().when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode()))
                .thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        lenient().when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        lenient().when(feedbackMapper.selectProgressListByScheduleOrderId(10L)).thenReturn(List.of(
                MesProFeedbackDO.builder()
                        .id(900L)
                        .scheduleOrderId(10L)
                        .scheduleOrderProcessId(20L)
                        .feedbackQuantity(new BigDecimal("213"))
                        .status(MesProFeedbackStatusEnum.APPROVING.getStatus())
                        .build()));

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getImportedCount());
        assertEquals(1, result.getSubmittedCount());
        ArgumentCaptor<MesProFeedbackSaveReqVO> reqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(reqCaptor.capture());
        assertEquals(300L, reqCaptor.getValue().getTaskId());
        assertEquals(400L, reqCaptor.getValue().getWorkstationId());
        assertEquals(20L, reqCaptor.getValue().getScheduleOrderProcessId());
        verify(feedbackService).submitFeedback(900L, true);
    }

    @Test
    void importDirectWorkReportWorkbook_whenSameSourceRowImportedTwice_shouldAccumulateFormalFeedbackOnly() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .quantity(new BigDecimal("1000"))
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(BigDecimal.ZERO.setScale(6))
                .remainingQuantity(new BigDecimal("1000.000000"))
                .progressPercent(BigDecimal.ZERO.setScale(6))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();
        List<MesProFeedbackDO> formalFeedbacks = new java.util.ArrayList<>();
        AtomicLong importRecordIdSequence = new AtomicLong(700L);
        AtomicLong feedbackIdSequence = new AtomicLong(900L);

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectByUsername("A2020002")).thenReturn(feedbackUser);
        when(adminUserMapper.selectByUsername("李萍")).thenReturn(null);
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(10L)).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode()))
                .thenReturn("FB-001", "FB-002");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(importRecordIdSequence.getAndIncrement());
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class)))
                .thenAnswer(invocation -> {
                    MesProFeedbackSaveReqVO req = invocation.getArgument(0);
                    Long feedbackId = feedbackIdSequence.getAndIncrement();
                    formalFeedbacks.add(MesProFeedbackDO.builder()
                            .id(feedbackId)
                            .scheduleOrderId(req.getScheduleOrderId())
                            .scheduleOrderProcessId(req.getScheduleOrderProcessId())
                            .feedbackQuantity(req.getFeedbackQuantity())
                            .status(MesProFeedbackStatusEnum.APPROVING.getStatus())
                            .build());
                    return feedbackId;
                });
        lenient().when(importRecordMapper.selectAppliedDirectProgressListByScheduleOrderId(10L)).thenReturn(List.of(
                MesProFeedbackImportRecordDO.builder()
                        .id(800L)
                        .scheduleOrderId(10L)
                        .scheduleOrderProcessId(20L)
                        .progressSourceType(MesProFeedbackImportRecordDO.PROGRESS_SOURCE_TYPE_DIRECT_WORK_REPORT)
                        .progressQuantity(new BigDecimal("120.000000"))
                        .build()));
        when(feedbackMapper.selectProgressListByScheduleOrderId(10L))
                .thenAnswer(invocation -> List.copyOf(formalFeedbacks));

        ThirdPartyFeedbackImportResult first = service.importDirectWorkReportWorkbook(file);
        ThirdPartyFeedbackImportResult second = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, first.getImportedCount());
        assertEquals(1, second.getImportedCount());
        assertEquals(1, first.getSubmittedCount());
        assertEquals(1, second.getSubmittedCount());
        assertEquals(List.of("FB-001"), first.getFeedbackCodes());
        assertEquals(List.of("FB-002"), second.getFeedbackCodes());
        assertEquals(new BigDecimal("213.000000"),
                first.getDirectWorkReportDetails().get(0).getAfterReportedQuantity());
        assertEquals(new BigDecimal("426.000000"),
                second.getDirectWorkReportDetails().get(0).getAfterReportedQuantity());
        verify(importRecordMapper, never()).selectAppliedDirectProgressListByScheduleOrderId(10L);
        verify(feedbackService, times(2)).createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class));
        verify(feedbackService).submitFeedback(900L, true);
        verify(feedbackService).submitFeedback(901L, true);
    }

    @Test
    void importDirectWorkReportWorkbook_shouldSkipOverRemainingWithoutDirectProgress() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildLiPingWorkbookWithOutputQuantity(200));
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .quantity(new BigDecimal("100"))
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("100"))
                .reportedQuantity(BigDecimal.ZERO.setScale(6))
                .remainingQuantity(new BigDecimal("100.000000"))
                .progressPercent(BigDecimal.ZERO.setScale(6))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(0, result.getImportedCount());
        assertEquals(0, result.getSubmittedCount());
        assertEquals(List.of(), result.getFeedbackCodes());
        assertEquals(List.of(), result.getImportRecordIds());
        assertEquals(List.of(), result.getDirectWorkReportDetails());
        assertEquals(2, result.getSkippedRows());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning =
                result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("REMAINING_NOT_ENOUGH", warning.getReasonCode());
        assertTrue(warning.getReason().contains("超过当前工序剩余数量"));
        assertEquals(new BigDecimal("200"), warning.getFeedbackQuantity());
        verify(importRecordMapper, never()).insert(any(MesProFeedbackImportRecordDO.class));
        verify(scheduleOrderProcessMapper, never()).updateProgress(anyLong(), any(), any(), any());
        verify(feedbackService, never()).createFeedbackWithScheduleSnapshot(any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: direct work report Excel now updates schedule progress only")
    void importDirectWorkReportWorkbook_shouldCreateFeedbackAndSubmitApprovalSkippingMiscRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300"))
                .progressPercent(BigDecimal.ZERO)
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getSheetCount());
        assertEquals(1, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(1, result.getSkippedRows());
        assertEquals(List.of("FB-001"), result.getFeedbackCodes());
        assertEquals(List.of(700L), result.getImportRecordIds());
        ArgumentCaptor<MesProFeedbackSaveReqVO> reqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(reqCaptor.capture());
        MesProFeedbackSaveReqVO req = reqCaptor.getValue();
        assertEquals("FB-001", req.getCode());
        assertEquals(100L, req.getWorkOrderId());
        assertEquals(300L, req.getTaskId());
        assertEquals(20L, req.getScheduleOrderProcessId());
        assertEquals(1L, req.getFeedbackUserId());
        assertEquals(2L, req.getApproveUserId());
        assertEquals(0, new BigDecimal("213").compareTo(req.getFeedbackQuantity()));
        assertEquals(0, new BigDecimal("213").compareTo(req.getQualifiedQuantity()));
        verify(feedbackMapper).updateById(org.mockito.ArgumentMatchers.argThat((MesProFeedbackDO feedback) ->
                Long.valueOf(900L).equals(feedback.getId()) && Long.valueOf(700L).equals(feedback.getSourceImportRecordId())));
        verify(feedbackService).submitFeedback(900L, true);
        verify(importRecordMapper).updateById(org.mockito.ArgumentMatchers.argThat((MesProFeedbackImportRecordDO record) ->
                Long.valueOf(700L).equals(record.getId()) && Long.valueOf(900L).equals(record.getFeedbackId())
                        && MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED.equals(record.getAttributionStatus())));
        verify(taskMapper, never()).selectListByCodes(List.of("881MO093613-1-11"));
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: duplicate direct work report imports now accumulate progress instead of creating feedback")
    void importDirectWorkReportWorkbook_whenSameSourceRowAlreadyImported_shouldCreateAnotherFeedback() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(new BigDecimal("300"))
                .remainingQuantity(new BigDecimal("300"))
                .progressPercent(new BigDecimal("30.000000"))
                .build();
        MesProScheduleOrderProcessDO refreshedScheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(new BigDecimal("513"))
                .remainingQuantity(new BigDecimal("487"))
                .progressPercent(new BigDecimal("51.300000"))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();
        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-REPEAT");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(701L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(901L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(refreshedScheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getImportedCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(1, result.getSkippedRows());
        assertEquals(List.of("FB-REPEAT"), result.getFeedbackCodes());
        assertEquals(List.of(701L), result.getImportRecordIds());
        assertEquals(1, result.getDirectWorkReportDetails().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportDetail detail = result.getDirectWorkReportDetails().get(0);
        assertEquals("881MO093613", detail.getWorkOrderCode());
        assertEquals("SO-001", detail.getScheduleOrderCode());
        assertEquals("3020110069", detail.getProductCode());
        assertEquals("外鞘管组件", detail.getProductName());
        assertEquals("WS-400", detail.getWorkstationCode());
        assertEquals("棘突球囊扩张导管", detail.getWorkstationName());
        assertEquals("Z2570", detail.getProcessCode());
        assertEquals("外鞘管组件包装", detail.getProcessName());
        assertEquals(new BigDecimal("213"), detail.getFeedbackQuantity());
        assertEquals(new BigDecimal("300"), detail.getBeforeReportedQuantity());
        assertEquals(new BigDecimal("513"), detail.getAfterReportedQuantity());
        assertEquals(new BigDecimal("213"), detail.getReportedQuantityDelta());
        assertEquals(new BigDecimal("30.000000"), detail.getBeforeProgressPercent());
        assertEquals(new BigDecimal("51.300000"), detail.getAfterProgressPercent());
        assertEquals(new BigDecimal("21.300000"), detail.getProgressDeltaPercent());
        assertEquals("FB-REPEAT", detail.getFeedbackCode());
        assertEquals(701L, detail.getImportRecordId());
        verify(feedbackService).createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class));
        verify(feedbackService).submitFeedback(901L, true);
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: over-remaining direct work report rows now update progress with warning")
    void importDirectWorkReportWorkbook_shouldSkipOverRemainingRowWhenReportedQuantityExceedsPlan() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildLiPingWorkbookWithOutputQuantity(200));
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("1000"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(new BigDecimal("900"))
                .remainingQuantity(new BigDecimal("100"))
                .progressPercent(new BigDecimal("90.000000"))
                .build();
        MesProScheduleOrderProcessDO refreshedScheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(new BigDecimal("1100"))
                .remainingQuantity(BigDecimal.ZERO.setScale(6))
                .progressPercent(new BigDecimal("100.000000"))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();
        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(0, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(0, result.getSubmittedCount());
        assertEquals(2, result.getSkippedRows());
        assertEquals(List.of(), result.getFeedbackCodes());
        assertEquals(List.of(), result.getImportRecordIds());
        assertEquals(List.of(), result.getDirectWorkReportDetails());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("REMAINING_NOT_ENOUGH", warning.getReasonCode());
        assertEquals("881MO093613", warning.getWorkOrderCode());
        assertEquals("SO-001", warning.getScheduleOrderCode());
        assertEquals("3020110069", warning.getProductCode());
        assertEquals("Z2570", warning.getProcessCode());
        assertEquals(new BigDecimal("200"), warning.getFeedbackQuantity());
        assertEquals(new BigDecimal("900"), warning.getReportedQuantity());
        assertEquals(new BigDecimal("100"), warning.getRemainingQuantity());
        assertEquals(new BigDecimal("90.000000"), warning.getProgressPercent());
        assertTrue(warning.getReason().contains("超过剩余数量"));
        verify(importRecordMapper, never()).insert(any(MesProFeedbackImportRecordDO.class));
        verify(feedbackService, never()).createFeedbackWithScheduleSnapshot(any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: fully reported direct work report rows now update progress with warning")
    void importDirectWorkReportWorkbook_shouldSkipAlreadyFullyReportedRow() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildLiPingWorkbookWithOutputQuantity(100));
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("1000"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(new BigDecimal("1100"))
                .remainingQuantity(BigDecimal.ZERO.setScale(6))
                .progressPercent(new BigDecimal("110.000000"))
                .build();
        MesProScheduleOrderProcessDO refreshedScheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .plannedQuantity(new BigDecimal("1000"))
                .reportedQuantity(new BigDecimal("1200"))
                .remainingQuantity(BigDecimal.ZERO.setScale(6))
                .progressPercent(new BigDecimal("100.000000"))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();
        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(0, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(0, result.getSubmittedCount());
        assertEquals(2, result.getSkippedRows());
        assertEquals(List.of(), result.getFeedbackCodes());
        assertEquals(List.of(), result.getImportRecordIds());
        assertEquals(List.of(), result.getDirectWorkReportDetails());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("FULLY_REPORTED", warning.getReasonCode());
        assertEquals("881MO093613", warning.getWorkOrderCode());
        assertEquals("Z2570", warning.getProcessCode());
        assertEquals(new BigDecimal("100"), warning.getFeedbackQuantity());
        assertEquals(new BigDecimal("1100"), warning.getReportedQuantity());
        assertEquals(BigDecimal.ZERO.setScale(6), warning.getRemainingQuantity());
        assertEquals(new BigDecimal("100.000000"), warning.getProgressPercent());
        assertTrue(warning.getReason().contains("已报满"));
        verify(importRecordMapper, never()).insert(any(MesProFeedbackImportRecordDO.class));
        verify(feedbackService, never()).createFeedbackWithScheduleSnapshot(any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: direct LiPing import now updates progress instead of creating feedback")
    void importWorkbook_shouldRouteLiPingHeaderToDirectWorkReportImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300"))
                .progressPercent(BigDecimal.ZERO)
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importWorkbook(file);

        assertEquals(1, result.getSheetCount());
        assertEquals(1, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(1, result.getSkippedRows());
        assertEquals(List.of("FB-001"), result.getFeedbackCodes());
        verify(feedbackService).createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class));
        verify(feedbackService).submitFeedback(900L, true);
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: feedback users are no longer matched for direct work report progress import")
    void importDirectWorkReportWorkbook_shouldSkipRowsWhenFeedbackUserMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbookWithMissingFeedbackUser());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300"))
                .progressPercent(BigDecimal.ZERO)
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002", "A2020113"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getSheetCount());
        assertEquals(1, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(2, result.getSkippedRows());
        assertEquals(List.of("FB-001"), result.getFeedbackCodes());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("FEEDBACK_USER_NOT_FOUND", warning.getReasonCode());
        assertEquals("A2020113", warning.getFeedbackUserCode());
        assertEquals("881MO093613", warning.getWorkOrderCode());
        assertEquals("Z2570", warning.getProcessCode());
        verify(feedbackService, times(1)).createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class));
        verify(feedbackService).submitFeedback(900L, true);
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: covered by direct progress non-intersection warning tests")
    void importDirectWorkReportWorkbook_shouldWarnWhenWorkOrderCodeMatchesMultipleSystemOrders() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO duplicatedWorkOrderA = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesProWorkOrderDO duplicatedWorkOrderB = MesProWorkOrderDO.builder()
                .id(101L).code("881MO093613").productId(1000L).build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();

        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(workOrderMapper.selectListByCodes(List.of("881MO093613")))
                .thenReturn(List.of(duplicatedWorkOrderA, duplicatedWorkOrderB));

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(0, result.getImportedCount());
        assertEquals(0, result.getSubmittedCount());
        assertEquals(2, result.getSkippedRows());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("WORK_ORDER_NOT_UNIQUE", warning.getReasonCode());
        assertEquals("881MO093613", warning.getWorkOrderCode());
        assertEquals("Z2570", warning.getProcessCode());
        assertTrue(warning.getReason().contains("多个系统工单"));
        verify(importRecordMapper, never()).insert(any(MesProFeedbackImportRecordDO.class));
        verify(feedbackService, never()).createFeedbackWithScheduleSnapshot(any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: covered by direct progress non-intersection warning tests")
    void importDirectWorkReportWorkbook_shouldSkipRowsWhenEffectiveScheduleOrderMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbookWithMissingScheduleOrder());
        MesProWorkOrderDO validWorkOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesProWorkOrderDO missingScheduleWorkOrder = MesProWorkOrderDO.builder()
                .id(101L).code("MO000093759").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300"))
                .progressPercent(BigDecimal.ZERO)
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613", "MO000093759")))
                .thenReturn(List.of(validWorkOrder, missingScheduleWorkOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L, 101L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getSheetCount());
        assertEquals(1, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(2, result.getSkippedRows());
        assertEquals(List.of("FB-001"), result.getFeedbackCodes());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("SCHEDULE_ORDER_NOT_FOUND", warning.getReasonCode());
        assertEquals("MO000093759", warning.getWorkOrderCode());
        assertEquals("Z2570", warning.getProcessCode());
        verify(feedbackService, times(1)).createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class));
        verify(feedbackService).submitFeedback(900L, true);
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: covered by direct progress non-intersection warning tests")
    void importDirectWorkReportWorkbook_shouldWarnWhenWorkOrderHasMultipleEffectiveScheduleOrders() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbookWithMissingScheduleOrder());
        MesProWorkOrderDO validWorkOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesProWorkOrderDO duplicatedScheduleWorkOrder = MesProWorkOrderDO.builder()
                .id(101L).code("MO000093759").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L)
                .code("881MO093613-1-11")
                .workOrderId(100L)
                .workstationId(400L)
                .routeId(500L)
                .processId(2000L)
                .itemId(1000L)
                .quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SO-001")
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO duplicateScheduleOrderA = MesProScheduleOrderDO.builder()
                .id(11L)
                .code("SO-DUP-A")
                .workOrderId(101L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderDO duplicateScheduleOrderB = MesProScheduleOrderDO.builder()
                .id(12L)
                .code("SO-DUP-B")
                .workOrderId(101L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .processName("外鞘管组件包装")
                .enabled(true)
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300"))
                .progressPercent(BigDecimal.ZERO)
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613", "MO000093759")))
                .thenReturn(List.of(validWorkOrder, duplicatedScheduleWorkOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L, 101L)))
                .thenReturn(List.of(scheduleOrder, duplicateScheduleOrderA, duplicateScheduleOrderB));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getImportedCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(2, result.getSkippedRows());
        assertEquals(List.of("FB-001"), result.getFeedbackCodes());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("SCHEDULE_ORDER_NOT_UNIQUE", warning.getReasonCode());
        assertEquals("MO000093759", warning.getWorkOrderCode());
        assertEquals("Z2570", warning.getProcessCode());
        assertTrue(warning.getReason().contains("多个有效排产工单"));
        verify(feedbackService, times(1)).createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class));
        verify(feedbackService).submitFeedback(900L, true);
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: route-flow disabled rows are non-intersection progress warnings")
    void importDirectWorkReportWorkbook_shouldSkipRowsWhenScheduleRouteFlowDisabled() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .workOrderId(100L)
                .routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .routeProcessId(600L)
                .processId(2000L)
                .processCode("Z2570")
                .enabled(true)
                .remainingQuantity(new BigDecimal("300"))
                .build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(false).build());
        org.mockito.Mockito.lenient().when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                        600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getSheetCount());
        assertEquals(0, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(0, result.getSubmittedCount());
        assertEquals(2, result.getSkippedRows());
        assertEquals(List.of(), result.getFeedbackCodes());
        assertEquals(List.of(), result.getImportRecordIds());
        assertEquals(List.of(), result.getDirectWorkReportDetails());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("PROCESS_NOT_ENABLED", warning.getReasonCode());
        assertEquals("881MO093613", warning.getWorkOrderCode());
        assertEquals("Z2570", warning.getProcessCode());
        verify(importRecordMapper, never()).insert(any(MesProFeedbackImportRecordDO.class));
        verify(feedbackService, never()).createFeedbackWithScheduleSnapshot(any());
        verify(routeFlowProcessConfigMapper, never())
                .selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: task selection no longer participates in direct work report progress import")
    void importDirectWorkReportWorkbook_shouldUseHistoricalTaskSelectionWhenExternalTaskCodeDoesNotMatchMesTaskCode() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO taskA = MesProTaskDO.builder()
                .id(300L).code("TASK-A").workOrderId(100L).workstationId(400L)
                .routeId(500L).processId(2000L).itemId(1000L).quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProTaskDO taskB = MesProTaskDO.builder()
                .id(301L).code("TASK-B").workOrderId(100L).workstationId(401L)
                .routeId(500L).processId(2000L).itemId(1000L).quantity(new BigDecimal("200"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L).workOrderId(100L).routeId(500L).status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L).scheduleOrderId(10L).routeProcessId(600L).processId(2000L).processCode("Z2570")
                .processName("外鞘管组件包装").enabled(true).reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300")).progressPercent(BigDecimal.ZERO).build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(
                        MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build(),
                        MesProTaskScheduleExtDO.builder().taskId(301L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L, 301L))).thenReturn(List.of(taskA, taskB));
        when(workstationMapper.selectBatchIds(List.of(400L, 401L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getSheetCount());
        assertEquals(1, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(1, result.getSkippedRows());
        assertEquals(List.of("FB-001"), result.getFeedbackCodes());
        assertEquals(List.of(700L), result.getImportRecordIds());
        assertEquals(1, result.getDirectWorkReportDetails().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportDetail detail = result.getDirectWorkReportDetails().get(0);
        assertEquals(MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED, detail.getAttributionStatus());
        assertEquals("FB-001", detail.getFeedbackCode());
        assertEquals("A2020002", detail.getFeedbackUserCode());
        assertEquals("李萍", detail.getApproverName());
        ArgumentCaptor<MesProFeedbackSaveReqVO> reqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(reqCaptor.capture());
        assertEquals(300L, reqCaptor.getValue().getTaskId());
        assertEquals(400L, reqCaptor.getValue().getWorkstationId());
        verify(feedbackService).submitFeedback(900L, true);
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: task selection no longer participates in direct work report progress import")
    void importDirectWorkReportWorkbook_shouldMatchTaskCodeWhenScheduleProcessLinksMultipleTasks() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO earlierTask = MesProTaskDO.builder()
                .id(300L).code("881MO093613-1-11").workOrderId(100L).workstationId(400L)
                .routeId(500L).processId(2000L).itemId(1000L).quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProTaskDO laterTask = MesProTaskDO.builder()
                .id(301L).code("881MO093613-1-12").workOrderId(100L).workstationId(401L)
                .routeId(500L).processId(2000L).itemId(1000L).quantity(new BigDecimal("200"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L).workOrderId(100L).routeId(500L).status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L).scheduleOrderId(10L).routeProcessId(600L).processId(2000L).processCode("Z2570")
                .processName("外鞘管组件包装").enabled(true).reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300")).progressPercent(BigDecimal.ZERO).build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(
                        MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build(),
                        MesProTaskScheduleExtDO.builder().taskId(301L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L, 301L))).thenReturn(List.of(earlierTask, laterTask));
        when(workstationMapper.selectBatchIds(List.of(400L, 401L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getSheetCount());
        assertEquals(1, result.getImportedCount());
        assertEquals(0, result.getPendingCount());
        assertEquals(1, result.getSubmittedCount());
        assertEquals(1, result.getSkippedRows());
        assertEquals(List.of("FB-001"), result.getFeedbackCodes());
        assertEquals(1, result.getDirectWorkReportDetails().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportDetail detail = result.getDirectWorkReportDetails().get(0);
        assertEquals(MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED, detail.getAttributionStatus());
        assertEquals("FB-001", detail.getFeedbackCode());
        assertEquals("A2020002", detail.getFeedbackUserCode());
        assertEquals("李萍", detail.getApproverName());
        ArgumentCaptor<MesProFeedbackSaveReqVO> reqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(reqCaptor.capture());
        assertEquals(300L, reqCaptor.getValue().getTaskId());
        assertEquals(400L, reqCaptor.getValue().getWorkstationId());
        verify(feedbackService).submitFeedback(900L, true);
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: task process identity no longer participates in direct work report progress import")
    void importDirectWorkReportWorkbook_shouldMatchHistoricalTaskProcessIdentity() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO historicalTask = MesProTaskDO.builder()
                .id(300L).code("881MO093613-1-11").workOrderId(100L).workstationId(400L)
                .routeId(500L).processId(1999L).itemId(1000L).quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L).workOrderId(100L).routeId(500L).status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L).scheduleOrderId(10L).routeProcessId(600L).processId(2000L).processCode("Z2570")
                .processName("外鞘管组件包装").enabled(true).reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300")).progressPercent(BigDecimal.ZERO).build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeProcessService.getProcessIdentityMap(List.of(2000L))).thenReturn(java.util.Map.of(1999L, 2000L, 2000L, 2000L));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(historicalTask));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getImportedCount());
        ArgumentCaptor<MesProFeedbackSaveReqVO> reqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(reqCaptor.capture());
        assertEquals(300L, reqCaptor.getValue().getTaskId());
        assertEquals(2000L, reqCaptor.getValue().getProcessId());
        assertEquals(20L, reqCaptor.getValue().getScheduleOrderProcessId());
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: zero task process id no longer participates in direct work report progress import")
    void importDirectWorkReportWorkbook_shouldResolveZeroScheduleProcessIdByRouteProcessIdentity() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L).code("TASK-ZERO-SNAPSHOT").workOrderId(100L).workstationId(400L)
                .routeId(500L).processId(2000L).itemId(1000L).quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L).workOrderId(100L).routeId(500L).status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L).scheduleOrderId(10L).routeProcessId(600L).processId(0L).processCode("Z2570")
                .processName("外鞘管组件包装").enabled(true).reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300")).progressPercent(BigDecimal.ZERO).build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("棘突球囊扩张导管").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(any())).thenReturn(List.of(process));
        when(routeProcessService.resolveFrozenRouteProcess(600L, 500L, 0L))
                .thenReturn(MesProRouteProcessDO.builder().id(600L).routeId(500L).processId(2000L).build());
        org.mockito.Mockito.doReturn(java.util.Map.of(2000L, 2000L))
                .when(routeProcessService).getProcessIdentityMap(any());
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(workstation));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode())).thenReturn("FB-001");
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(700L);
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class))).thenReturn(900L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(scheduleOrderProcess);

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(1, result.getImportedCount());
        ArgumentCaptor<MesProFeedbackSaveReqVO> reqCaptor = ArgumentCaptor.forClass(MesProFeedbackSaveReqVO.class);
        verify(feedbackService).createFeedbackWithScheduleSnapshot(reqCaptor.capture());
        assertEquals(300L, reqCaptor.getValue().getTaskId());
        assertEquals(2000L, reqCaptor.getValue().getProcessId());
        assertEquals(20L, reqCaptor.getValue().getScheduleOrderProcessId());
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: deleted workstation no longer blocks direct work report progress import")
    void importDirectWorkReportWorkbook_shouldWarnWhenOnlyTaskWorkstationWasDeleted() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buildLiPingWorkbook());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L).code("881MO093613").productId(1000L).build();
        MesMdItemDO item = MesMdItemDO.builder().id(1000L).code("3020110069").name("外鞘管组件").build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(300L).code("TASK-DELETED-WORKSTATION").workOrderId(100L).workstationId(400L)
                .routeId(500L).processId(2000L).itemId(1000L).quantity(new BigDecimal("300"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L).code("SCH-001").workOrderId(100L).routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L).scheduleOrderId(10L).routeProcessId(600L).processId(2000L).processCode("Z2570")
                .processName("外鞘管组件包装").enabled(true).reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("300")).progressPercent(BigDecimal.ZERO).build();
        MesProProcessDO process = MesProProcessDO.builder().id(2000L).code("Z2570").name("外鞘管组件包装").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(List.of("881MO093613"))).thenReturn(List.of(workOrder));
        when(itemMapper.selectListByIds(List.of(1000L))).thenReturn(List.of(item));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(100L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L))).thenReturn(List.of(scheduleOrderProcess));
        when(processMapper.selectListByIds(List.of(2000L))).thenReturn(List.of(process));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(600L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                        .id(801L).routeFlowConfigId(800L).routeId(500L).routeProcessId(600L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(task));
        when(workstationMapper.selectBatchIds(List.of(400L))).thenReturn(List.of());

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(0, result.getImportedCount());
        assertEquals(0, result.getSubmittedCount());
        assertEquals(2, result.getSkippedRows());
        assertEquals(1, result.getDirectWorkReportSkipWarnings().size());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = result.getDirectWorkReportSkipWarnings().get(0);
        assertEquals("ACTIVE_TASK_NOT_FOUND", warning.getReasonCode());
        assertEquals(3, warning.getRowNo());
        assertEquals("881MO093613", warning.getWorkOrderCode());
        assertEquals("Z2570", warning.getProcessCode());
        verify(importRecordMapper, never()).insert(any(MesProFeedbackImportRecordDO.class));
        verify(feedbackService, never()).createFeedbackWithScheduleSnapshot(any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("obsolete: replaced by progress-only located work-order result contract")
    void importDirectWorkReportWorkbook_shouldReturnLocatedWorkOrdersForCreatedAndSkippedRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildLiPingWorkbookWithFourWorkOrders());
        MesMdItemDO itemA = MesMdItemDO.builder().id(1000L).code("YXN.069.001.1002").name("冠状动脉棘突球囊").build();
        MesMdItemDO itemB = MesMdItemDO.builder().id(1001L).code("YXN.069.001.1008").name("冠状动脉棘突球囊").build();
        MesProWorkOrderDO workOrderA = MesProWorkOrderDO.builder().id(100L).code("881MO093613").productId(1000L).build();
        MesProWorkOrderDO workOrderB = MesProWorkOrderDO.builder().id(101L).code("881MO093616").productId(1000L).build();
        MesProWorkOrderDO workOrderC = MesProWorkOrderDO.builder().id(102L).code("881MO093615").productId(1000L).build();
        MesProWorkOrderDO workOrderD = MesProWorkOrderDO.builder().id(103L).code("881MO098538").productId(1001L).build();
        MesProScheduleOrderDO scheduleOrderA = MesProScheduleOrderDO.builder()
                .id(10L).code("SCH-881MO093613").workOrderId(100L).routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO scheduleOrderB = MesProScheduleOrderDO.builder()
                .id(11L).code("SCH-881MO093616").workOrderId(101L).routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO scheduleOrderC = MesProScheduleOrderDO.builder()
                .id(12L).code("SCH-881MO093615").workOrderId(102L).routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderDO scheduleOrderD = MesProScheduleOrderDO.builder()
                .id(13L).code("SCH-881MO098538").workOrderId(103L).routeId(500L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus()).build();
        MesProScheduleOrderProcessDO processA = buildScheduleOrderProcess(20L, 10L, 600L,
                2000L, "Z2570", "棘突近端粘接", "1000", "0", "1000", "0.000000");
        MesProScheduleOrderProcessDO processB = buildScheduleOrderProcess(21L, 11L, 601L,
                2001L, "Z2976", "棘突远端塑型", "1000", "900", "100", "90.000000");
        MesProScheduleOrderProcessDO processC = buildScheduleOrderProcess(22L, 12L, 602L,
                2002L, "Z2630", "棘突远端焊接", "1000", "0", "1000", "0.000000");
        MesProScheduleOrderProcessDO processD = buildScheduleOrderProcess(23L, 13L, 603L,
                2003L, "Z2631", "棘突丝切割", "1000", "0", "1000", "0.000000");
        MesProTaskDO taskA = buildTask(300L, "881MO093613-1-11", 100L, 400L, 2000L, 1000L);
        MesProTaskDO taskB = buildTask(301L, "881MO093616-1-11", 101L, 401L, 2001L, 1000L);
        MesProTaskDO taskC = buildTask(302L, "881MO093615-1-11", 102L, 402L, 2002L, 1000L);
        MesProTaskDO taskD = buildTask(303L, "881MO098538-1-11", 103L, 403L, 2003L, 1001L);
        MesMdWorkstationDO workstationA = MesMdWorkstationDO.builder().id(400L).code("WS-400").name("工位A").build();
        MesMdWorkstationDO workstationB = MesMdWorkstationDO.builder().id(401L).code("WS-401").name("工位B").build();
        MesMdWorkstationDO workstationC = MesMdWorkstationDO.builder().id(402L).code("WS-402").name("工位C").build();
        AdminUserDO feedbackUser = AdminUserDO.builder().id(1L).username("A2020002").nickname("李萍").build();
        AdminUserDO approveUser = AdminUserDO.builder().id(2L).username("approval_liping").nickname("李萍").build();

        when(workOrderMapper.selectListByCodes(any())).thenReturn(List.of(workOrderA, workOrderB, workOrderC, workOrderD));
        when(itemMapper.selectListByIds(any())).thenReturn(List.of(itemA, itemB));
        when(adminUserMapper.selectListByUsernames(List.of("A2020002"))).thenReturn(List.of(feedbackUser));
        when(adminUserMapper.selectListByNicknamesExact(List.of("李萍"))).thenReturn(List.of(approveUser));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(any()))
                .thenReturn(List.of(scheduleOrderA, scheduleOrderB, scheduleOrderC, scheduleOrderD));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(any()))
                .thenReturn(List.of(processA, processB, processC, processD));
        when(processMapper.selectListByIds(any())).thenReturn(List.of(
                MesProProcessDO.builder().id(2000L).code("Z2570").name("棘突近端粘接").build(),
                MesProProcessDO.builder().id(2001L).code("Z2976").name("棘突远端塑型").build(),
                MesProProcessDO.builder().id(2002L).code("Z2630").name("棘突远端焊接").build(),
                MesProProcessDO.builder().id(2003L).code("Z2631").name("棘突丝切割").build()));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(500L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(true).build());
        for (long routeProcessId = 600L; routeProcessId <= 603L; routeProcessId++) {
            when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                    routeProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                    .thenReturn(MesProRouteFlowProcessConfigDO.builder()
                            .id(800L + routeProcessId).routeFlowConfigId(800L).routeId(500L)
                            .routeProcessId(routeProcessId).useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                            .enabled(true).build());
        }
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(any())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build(),
                MesProTaskScheduleExtDO.builder().taskId(301L).scheduleOrderProcessId(21L).build(),
                MesProTaskScheduleExtDO.builder().taskId(302L).scheduleOrderProcessId(22L).build(),
                MesProTaskScheduleExtDO.builder().taskId(303L).scheduleOrderProcessId(23L).build()));
        when(taskMapper.selectListByIds(any())).thenReturn(List.of(taskA, taskB, taskC, taskD));
        when(workstationMapper.selectBatchIds(any())).thenReturn(List.of(workstationA, workstationB, workstationC));
        when(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode()))
                .thenReturn("FB-001", "FB-002");
        AtomicLong idSequence = new AtomicLong(700L);
        doAnswer(invocation -> {
            MesProFeedbackImportRecordDO record = invocation.getArgument(0);
            record.setId(idSequence.getAndIncrement());
            return 1;
        }).when(importRecordMapper).insert(any(MesProFeedbackImportRecordDO.class));
        when(feedbackService.createFeedbackWithScheduleSnapshot(any(MesProFeedbackSaveReqVO.class)))
                .thenReturn(900L, 901L);
        when(scheduleOrderProcessMapper.selectById(20L)).thenReturn(buildScheduleOrderProcess(20L, 10L, 600L,
                2000L, "Z2570", "棘突近端粘接", "1000", "100", "900", "10.000000"));
        when(scheduleOrderProcessMapper.selectById(22L)).thenReturn(buildScheduleOrderProcess(22L, 12L, 602L,
                2002L, "Z2630", "棘突远端焊接", "1000", "100", "900", "10.000000"));

        ThirdPartyFeedbackImportResult result = service.importDirectWorkReportWorkbook(file);

        assertEquals(2, result.getImportedCount());
        assertEquals(2, result.getSubmittedCount());
        assertEquals(3, result.getSkippedRows());
        assertEquals(2, result.getDirectWorkReportDetails().size());
        assertEquals(2, result.getDirectWorkReportSkipWarnings().size());
        java.util.LinkedHashSet<String> locatedWorkOrders = new java.util.LinkedHashSet<>();
        result.getDirectWorkReportDetails().forEach(detail -> locatedWorkOrders.add(detail.getWorkOrderCode()));
        result.getDirectWorkReportSkipWarnings().forEach(warning -> locatedWorkOrders.add(warning.getWorkOrderCode()));
        assertEquals(Set.of("881MO093613", "881MO093616", "881MO093615", "881MO098538"), locatedWorkOrders);
        assertEquals(Set.of("REMAINING_NOT_ENOUGH", "ACTIVE_TASK_NOT_FOUND"),
                result.getDirectWorkReportSkipWarnings().stream()
                        .map(ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning::getReasonCode)
                        .collect(java.util.stream.Collectors.toSet()));
        verify(feedbackService, times(2)).createFeedbackWithScheduleSnapshot(any());
        verify(feedbackService).submitFeedback(900L, true);
        verify(feedbackService).submitFeedback(901L, true);
    }

    @Test
    void simulateImportWorkbook_shouldFailWhenNoEligibleSourceExists() {
        when(scheduleOrderProcessMapper.selectList(any())).thenReturn(List.of());

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(adminUserMapper.selectById(1L)).thenReturn(AdminUserDO.builder().id(1L).username("aoteman").build());

            ServiceException ex = assertThrows(ServiceException.class, () -> service.simulateImportWorkbook(2));
            assertEquals(PRO_FEEDBACK_SIMULATE_SOURCE_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    void simulateImportWorkbook_shouldFailWhenEligibleSourceLessThanRequestedCount() {
        AdminUserDO currentUser = AdminUserDO.builder().id(1L).username("aoteman").nickname("芋道1").build();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(20L)
                .scheduleOrderId(10L)
                .processId(2000L)
                .remainingQuantity(new BigDecimal("8"))
                .enabled(true)
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .erpWorkOrderCode("WO-001")
                .workOrderId(100L)
                .productId(1000L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();

        when(adminUserMapper.selectById(1L)).thenReturn(currentUser);
        when(scheduleOrderProcessMapper.selectList(any())).thenReturn(List.of(scheduleOrderProcess));
        when(scheduleOrderMapper.selectById(10L)).thenReturn(scheduleOrder);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(20L)))
                .thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(300L).scheduleOrderProcessId(20L).build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(MesProTaskDO.builder().id(300L).code("TASK-001").build()));
        when(workOrderMapper.selectById(100L)).thenReturn(MesProWorkOrderDO.builder().id(100L).productId(1000L).build());
        when(itemMapper.selectById(1000L)).thenReturn(MesMdItemDO.builder().id(1000L).code("ITEM-001").build());
        when(processMapper.selectById(2000L)).thenReturn(MesProProcessDO.builder().id(2000L).code("PROC-001").name("球囊裁剪").build());

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);

            ServiceException ex = assertThrows(ServiceException.class, () -> service.simulateImportWorkbook(2));
            assertEquals(PRO_FEEDBACK_SIMULATE_SOURCE_NOT_ENOUGH.getCode(), ex.getCode());
        }
    }

    private byte[] buildLiPingWorkbook() throws Exception {
        return buildLiPingWorkbookWithOutputQuantity(213);
    }

    private byte[] buildLiPingWorkbookWithOutputQuantity(int outputQuantity) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            var header = sheet.createRow(0);
            List.of(
                    "任务单", "生产订单", "产品代码", "产品名称", "工序编码", "工序名称", "部门", "人员工号",
                    "人员名称", "工段长", "日期", "工序单价", "总产出", "总金额"
            ).forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));
            fillLiPingRow(sheet.createRow(1), "杂务计时", "", "3020110069", "外鞘管组件",
                    "ZW001", "杂务", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:20:00", 0);
            fillLiPingRow(sheet.createRow(2), "881MO093613-1-11", "881MO093613-1", "3020110069", "外鞘管组件",
                    "Z2570", "外鞘管组件包装", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:27:17", outputQuantity);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildLiPingWorkbookWithMissingFeedbackUser() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            var header = sheet.createRow(0);
            List.of(
                    "任务单", "生产订单", "产品代码", "产品名称", "工序编码", "工序名称", "部门", "人员工号",
                    "人员名称", "工段长", "日期", "工序单价", "总产出", "总金额"
            ).forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));
            fillLiPingRow(sheet.createRow(1), "杂务计时", "", "3020110069", "外鞘管组件",
                    "ZW001", "杂务", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:20:00", 0);
            fillLiPingRow(sheet.createRow(2), "881MO093613-1-11", "881MO093613-1", "3020110069", "外鞘管组件",
                    "Z2570", "外鞘管组件包装", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:27:17", 213);
            fillLiPingRow(sheet.createRow(3), "881MO093613-1-12", "881MO093613-1", "3020110069", "外鞘管组件",
                    "Z2570", "外鞘管组件包装", "组装", "A2020113", "朱欣妮", "李萍", "2026/4/9 15:30:17", 100);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildLiPingWorkbookWithMissingScheduleOrder() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            var header = sheet.createRow(0);
            List.of(
                    "任务单", "生产订单", "产品代码", "产品名称", "工序编码", "工序名称", "部门", "人员工号",
                    "人员名称", "工段长", "日期", "工序单价", "总产出", "总金额"
            ).forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));
            fillLiPingRow(sheet.createRow(1), "杂务计时", "", "3020110069", "外鞘管组件",
                    "ZW001", "杂务", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:20:00", 0);
            fillLiPingRow(sheet.createRow(2), "881MO093613-1-11", "881MO093613-1", "3020110069", "外鞘管组件",
                    "Z2570", "外鞘管组件包装", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:27:17", 213);
            fillLiPingRow(sheet.createRow(3), "881MO093613-1-12", "MO000093759", "3020110069", "外鞘管组件",
                    "Z2570", "外鞘管组件包装", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:30:17", 100);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildLiPingWorkbookWithFourWorkOrders() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            var header = sheet.createRow(0);
            List.of(
                    "任务单", "生产订单", "产品代码", "产品名称", "工序编码", "工序名称", "部门", "人员工号",
                    "人员名称", "工段长", "日期", "工序单价", "总产出", "总金额"
            ).forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));
            fillLiPingRow(sheet.createRow(1), "杂务计时", "", "YXN.069.001.1002", "冠状动脉棘突球囊",
                    "ZW001", "杂务", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:20:00", 0);
            fillLiPingRow(sheet.createRow(2), "881MO093613-1-11", "881MO093613-1", "YXN.069.001.1002", "冠状动脉棘突球囊",
                    "Z2570", "棘突近端粘接", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:27:17", 100);
            fillLiPingRow(sheet.createRow(3), "881MO093616-1-11", "881MO093616-1", "YXN.069.001.1002", "冠状动脉棘突球囊",
                    "Z2976", "棘突远端塑型", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:28:17", 200);
            fillLiPingRow(sheet.createRow(4), "881MO093615-1-11", "881MO093615-1", "YXN.069.001.1002", "冠状动脉棘突球囊",
                    "Z2630", "棘突远端焊接", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:29:17", 100);
            fillLiPingRow(sheet.createRow(5), "881MO098538-1-11", "881MO098538-1", "YXN.069.001.1008", "冠状动脉棘突球囊",
                    "Z2631", "棘突丝切割", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:30:17", 100);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private MesProScheduleOrderProcessDO buildScheduleOrderProcess(Long id, Long scheduleOrderId, Long routeProcessId,
                                                                   Long processId, String processCode, String processName,
                                                                   String plannedQuantity, String reportedQuantity,
                                                                   String remainingQuantity, String progressPercent) {
        return MesProScheduleOrderProcessDO.builder()
                .id(id)
                .scheduleOrderId(scheduleOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .processCode(processCode)
                .processName(processName)
                .enabled(true)
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .reportedQuantity(new BigDecimal(reportedQuantity))
                .remainingQuantity(new BigDecimal(remainingQuantity))
                .progressPercent(new BigDecimal(progressPercent))
                .build();
    }

    private MesProTaskDO buildTask(Long id, String code, Long workOrderId, Long workstationId,
                                   Long processId, Long itemId) {
        return MesProTaskDO.builder()
                .id(id)
                .code(code)
                .workOrderId(workOrderId)
                .workstationId(workstationId)
                .routeId(500L)
                .processId(processId)
                .itemId(itemId)
                .quantity(new BigDecimal("1000"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
    }

    private void fillLiPingRow(org.apache.poi.ss.usermodel.Row row, String taskCode, String workOrderCode,
                               String itemCode, String itemName, String processCode, String processName,
                               String department, String feedbackUserCode, String feedbackUserName,
                               String approverName, String feedbackTime, int outputQuantity) {
        row.createCell(0).setCellValue(taskCode);
        row.createCell(1).setCellValue(workOrderCode);
        row.createCell(2).setCellValue(itemCode);
        row.createCell(3).setCellValue(itemName);
        row.createCell(4).setCellValue(processCode);
        row.createCell(5).setCellValue(processName);
        row.createCell(6).setCellValue(department);
        row.createCell(7).setCellValue(feedbackUserCode);
        row.createCell(8).setCellValue(feedbackUserName);
        row.createCell(9).setCellValue(approverName);
        row.createCell(10).setCellValue(feedbackTime);
        row.createCell(11).setCellValue(1.23);
        row.createCell(12).setCellValue(outputQuantity);
        row.createCell(13).setCellValue(outputQuantity * 1.23);
    }
}
