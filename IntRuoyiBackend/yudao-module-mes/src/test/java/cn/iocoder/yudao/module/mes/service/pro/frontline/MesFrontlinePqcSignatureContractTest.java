package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.MesFrontlineDeviceAccountController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSubmitReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcItemEquipmentConfigService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceSelectionSnapshotCodec;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Controller, context, signature writer/query, event writer and H2 mappers; scope/configuration APIs are doubles. */
@Sql(scripts = "/sql/pqc-signature-contract.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/pqc-signature-contract-clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@org.springframework.test.context.jdbc.SqlMergeMode(org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE)
class MesFrontlinePqcSignatureContractTest extends BaseDbUnitTest {
    @Resource private MesProProcessPoolEventMapper persistedEvents;
    @Resource private MesProProcessPoolPqcRecordMapper persistedPqc;
    @Resource private MesPqcInspectionPieceDetailMapper persistedPieces;
    @Resource private MesProProcessPoolMapper persistedPools;
    @Resource private MesProProcessPoolQuantityFragmentMapper persistedFragments;
    @Resource private ElectronicSignatureRecordMapper signatureRecords;
    private ElectronicSignatureQueryServiceImpl actualQuery;
    private static final long WORK_ORDER_ID = 1001L;
    private static final long ROUTE_ID = 2001L;
    private static final long ROUTE_VERSION_ID = 3001L;
    private static final long ACTIVE_ORDER_ID = 5001L;
    private static final long DCC_PROJECT_ID = 6001L;
    private static final long REGULATION_ID = 7001L;
    private static final long REGULATION_VERSION_ID = 8001L;
    private static final long QA_PROCESS_ID = 9001L;

    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    private MesProWorkOrderMapper workOrderMapper;
    private MesProRouteMapper routeMapper;
    private MesProRouteVersionMapper routeVersionMapper;
    private DccProjectCodeMapper dccProjectCodeMapper;
    private MesQaInspectionRegulationMapper regulationMapper;
    private MesQaInspectionRegulationVersionMapper versionMapper;
    private MesQaInspectionRegulationProcessMapper regulationProcessMapper;
    private MesQaInspectionRegulationItemMapper regulationItemMapper;
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    private MesProProcessPoolEventMapper processPoolEventMapper;
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private MesPqcInspectionPieceDetailMapper pieceDetailMapper;
    private MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private MesProcessPoolTeamDeviceMapper teamDeviceMapper;
    private AdminUserApi adminUserApi;
    private MesProcessPoolEventService eventService;
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private MesProBatchRecordExecutionSignatureService signatureService;
    private MesQaInspectionRegulationService regulationService;
    private MesPqcItemEquipmentConfigService pqcItemEquipmentConfigService;
    private MesMdItemService itemService;
    private MesProEdhrNonconformanceReviewService nonconformanceReviewService;
    private GxpAuditService gxpAuditService;
    private MesFrontlinePqcContextService service;

    @BeforeEach
    void setUp() {
        activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
        processPoolEventMapper = persistedEvents;
        processSnapshotMapper = mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        workOrderMapper = mock(MesProWorkOrderMapper.class);
        routeMapper = mock(MesProRouteMapper.class);
        routeVersionMapper = mock(MesProRouteVersionMapper.class);
        dccProjectCodeMapper = mock(DccProjectCodeMapper.class);
        regulationMapper = mock(MesQaInspectionRegulationMapper.class);
        versionMapper = mock(MesQaInspectionRegulationVersionMapper.class);
        regulationProcessMapper = mock(MesQaInspectionRegulationProcessMapper.class);
        regulationItemMapper = mock(MesQaInspectionRegulationItemMapper.class);
        regulationService = mock(MesQaInspectionRegulationService.class);
        pqcItemEquipmentConfigService = mock(MesPqcItemEquipmentConfigService.class);
        pqcTaskMapper = mock(MesPqcInspectionTaskMapper.class);
        pieceDetailMapper = persistedPieces;
        itemService = mock(MesMdItemService.class);
        scopeMapper = mock(MesProcessPoolTeamLeaderScopeMapper.class);
        teamDeviceMapper = mock(MesProcessPoolTeamDeviceMapper.class);
        when(teamDeviceMapper.selectBatchIds(any())).thenReturn(List.of(
                MesProcessPoolTeamDeviceDO.builder().id(2101L).deviceCode("FM")
                        .deviceName("压力表").enabled(true).deviceStatus("ENABLED").build()));
        adminUserApi = mock(AdminUserApi.class);
        pqcRecordMapper = persistedPqc;
        nonconformanceReviewService = mock(MesProEdhrNonconformanceReviewService.class);
        gxpAuditService = mock(GxpAuditService.class);
        eventService = actualEventService();
        signatureService = actualSignatureService();
        service = new MesFrontlinePqcContextServiceImpl(activeOrderMapper, processPoolEventMapper,
                processSnapshotMapper,
                teamDeviceMapper,
                workOrderMapper, routeMapper, routeVersionMapper, dccProjectCodeMapper,
                regulationMapper, versionMapper, regulationProcessMapper, regulationItemMapper,
                regulationService, pqcItemEquipmentConfigService, pqcTaskMapper,
                pieceDetailMapper, itemService, scopeMapper,
                adminUserApi, eventService, pqcRecordMapper, signatureService, nonconformanceReviewService,
                gxpAuditService);
    }


    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {3001L, 3002L})
    void controllerRequestWithoutOperatorWritesAndVerifiesNewSignature(long signer) {
        long loginUserId = 3001L;
        long actualEmployeeId = signer;
        long pqcTaskId = 9101L;
        LocalDateTime submitTime = LocalDateTime.of(2026, 8, 19, 15, 30);
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(ACTIVE_ORDER_ID, WORK_ORDER_ID, submitTime);
        when(pqcTaskMapper.selectByIdForUpdate(pqcTaskId)).thenReturn(
                pendingTask(pqcTaskId, "FIRST", "FIRST", "FIRST"));
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder);
        when(processSnapshotMapper.selectByActiveOrderAndProcess(ACTIVE_ORDER_ID, 30001L, 40001L))
                .thenReturn(processSnapshot(30001L, 40001L));
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot(30001L, 40001L),
                processSnapshot(30002L, 40002L)));
        when(scopeMapper.selectActiveScopesByLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC))
                .thenReturn(List.of(pqcEmployeeScope(loginUserId, actualEmployeeId)));
        when(adminUserApi.getUserList(any())).thenReturn(List.of(enabledUser(loginUserId), enabledUser(actualEmployeeId)));
        when(regulationProcessMapper.selectById(QA_PROCESS_ID)).thenReturn(MesQaInspectionRegulationProcessDO.builder()
                .id(QA_PROCESS_ID).regulationVersionId(REGULATION_VERSION_ID).build());
        when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(MesQaInspectionRegulationVersionDO.builder()
                .id(REGULATION_VERSION_ID).regulationId(REGULATION_ID).lifecycleStatus("PUBLISHED").build());
        when(regulationMapper.selectById(REGULATION_ID)).thenReturn(MesQaInspectionRegulationDO.builder()
                .id(REGULATION_ID).dccProjectCodeId(DCC_PROJECT_ID).build());
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(DccProjectCodeDO.builder()
                .id(DCC_PROJECT_ID).build());
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID))
                .thenReturn(List.of(publishedItem("FIRST")));
        when(pqcTaskMapper.updateSubmittedIfPending(anyLong(), any(), anyString(), anyString(), anyString()))
                .thenReturn(1);

        when(pqcTaskMapper.updateSubmittedEventId(eq(pqcTaskId), anyLong())).thenReturn(1);
        var controller = new MesFrontlineDeviceAccountController();
        ReflectionTestUtils.setField(controller, "pqcContextService", service);
        var req = new MesFrontlinePqcSubmitReqVO().setActiveOrderId(ACTIVE_ORDER_ID).setPqcTaskId(pqcTaskId)
                .setRegulationVersionId(REGULATION_VERSION_ID).setQaProcessId(QA_PROCESS_ID)
                .setActualEmployeeId(signer).setActualInspectionQuantity(2).setScrapQuantity(0)
                .setSignaturePassword("sign-123").setRawPayload(Map.of()).setClientSubmitTime(submitTime);
        req.setItemResults(List.of(new MesFrontlinePqcSubmitReqVO.ItemResult()
                .setItemCode("ID-001").setSampleValues(List.of("合格", "合格"))));
        var login = new LoginUser().setId(loginUserId).setTenantId(1L).setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        try {
            var resp = controller.submitPqcInspection(req).getData();
            var event = persistedEvents.selectById(resp.getPqcEventId());
            assertEquals(loginUserId, event.getDeviceAccountId());
            assertNull(event.getDeviceId());
            assertNull(event.getWorkstationId());
            assertEquals(signer, event.getSignatureUserId());
            var record = signatureRecords.selectById(event.getSignatureId());
            assertEquals(signer, record.getActorId());
            var identity = com.alibaba.fastjson.JSON.parseObject(record.getCanonicalContentJson()).getJSONObject("signatureIdentity");
            assertEquals(loginUserId, identity.getLong("operatorId"));
            assertEquals(signer, identity.getLong("signerId"));
            assertEquals("SYSTEM_USER", identity.getString("domain"));
            assertEquals("VALID", actualQuery.verifyEvidence(record.getId()).verificationStatus());
            var detailService = mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService.class);
            var reader = new MesActiveOrderSignatureEvidenceService(null, null, detailService, actualQuery, persistedEvents, null, null, null);
            var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(ACTIVE_ORDER_ID).setProcesses(List.of(
                    new MesTeamLeaderActiveOrderDetail.ProcessDetail().setPqcSubmissions(List.of(
                            new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail().setPqcTaskIds(List.of(pqcTaskId))
                                    .setSubmittedEventIds(List.of(event.getId())).setSubmitterSignatures(List.of(
                                            new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(record.getId()).setSignerName("PQC")))))));
            when(detailService.getDetail(800L)).thenReturn(detail);
            assertEquals("VALID", reader.getBatch(800L, null, record.getId()).verification().verificationStatus());
            persistedEvents.updateById(new MesProProcessPoolEventDO().setId(event.getId()).setDeviceAccountId(3999L));
            var mismatch = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> reader.getBatch(800L, null, record.getId()));
            assertEquals(1_040_760_452, mismatch.getCode());
            verify(adminUserApi, atLeastOnce()).reauthenticateForSignature(signer, "sign-123");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private MesProcessPoolEventService actualEventService() {
        var result = new MesProcessPoolEventServiceImpl();
        ReflectionTestUtils.setField(result, "processPoolMapper", persistedPools);
        ReflectionTestUtils.setField(result, "processPoolEventMapper", persistedEvents);
        ReflectionTestUtils.setField(result, "quantityFragmentMapper", persistedFragments);
        ReflectionTestUtils.setField(result, "pqcRecordMapper", persistedPqc);
        ReflectionTestUtils.setField(result, "nonconformanceReviewService", nonconformanceReviewService);
        ReflectionTestUtils.setField(result, "reportManagementSummaryService", mock(MesProductionReportManagementSummaryService.class));
        return result;
    }

    private MesProBatchRecordExecutionSignatureService actualSignatureService() {
        var adapter = new MesBatchRecordSignatureSubjectAdapter();
        var writer = new ElectronicSignatureServiceImpl();
        actualQuery = new ElectronicSignatureQueryServiceImpl();
        for (Object target : List.of(writer, actualQuery)) {
            ReflectionTestUtils.setField(target, "signatureRecordMapper", signatureRecords);
            ReflectionTestUtils.setField(target, "adminUserApi", adminUserApi);
            ReflectionTestUtils.setField(target, "subjectAdapters", List.of(adapter));
        }
        ReflectionTestUtils.setField(writer, "gxpAuditService", gxpAuditService);
        var directory = mock(AdminUserService.class);
        when(directory.getUser(anyLong())).thenAnswer(inv -> new AdminUserDO().setId(inv.getArgument(0))
                .setUsername("user-"+inv.getArgument(0)).setNickname("员工"+inv.getArgument(0)).setStatus(0));
        var authorization = mock(DccElectronicSignatureAuthorizationService.class);
        when(authorization.isElectronicSignatureEnabled(anyLong())).thenReturn(true);
        var result = new MesProBatchRecordExecutionSignatureService();
        ReflectionTestUtils.setField(result, "adminUserService", directory);
        ReflectionTestUtils.setField(result, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(result, "authorizationService", authorization);
        ReflectionTestUtils.setField(result, "electronicSignatureService", writer);
        ReflectionTestUtils.setField(result, "permissionService", mock(PermissionService.class));
        return result;
    }
    private static MesProcessPoolActiveOrderDO activeOrder(long id, long workOrderId, LocalDateTime joinedAt) {
        return MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(workOrderId).routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID).activeStatus("ACTIVE").businessStatus("ACTIVE")
                .dccProjectCodeId(DCC_PROJECT_ID).qaRegulationId(REGULATION_ID)
                .qaRegulationVersionId(REGULATION_VERSION_ID)
                .joinedAt(joinedAt).build();
    }

    private static MesPqcInspectionTaskDO pendingTask(long taskId,
                                                       String inspectionType,
                                                       String inspectionRuleKey,
                                                       String shiftCode) {
        return MesPqcInspectionTaskDO.builder()
                .id(taskId)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(30001L)
                .processId(40001L)
                .qaProcessId(QA_PROCESS_ID)
                .qaItemCode("ID-001")
                .regulationVersionId(REGULATION_VERSION_ID)
                .inspectionType(inspectionType)
                .inspectionRuleKey(inspectionRuleKey)
                .businessDate(LocalDate.of(2026, 8, 19))
                .shiftCode(shiftCode)
                .roundNo(1)
                .plannedInspectionQuantity(1)
                .taskStatus("PENDING")
                .build();
    }

    private static MesQaInspectionRegulationItemDO publishedItem(String inspectionType) {
        return MesQaInspectionRegulationItemDO.builder()
                .id(8101L)
                .regulationVersionId(REGULATION_VERSION_ID)
                .qaProcessId(QA_PROCESS_ID)
                .inspectionType(inspectionType)
                .itemSort(1)
                .itemCode("ID-001")
                .itemName("外观")
                .inspectionMethod("目测")
                .inspectionTool("目测")
                .standardText("应合格")
                .samplingPlanText("全检")
                .resultType("BOOLEAN")
                .firstInspectionQuantity(1)
                .build();
    }

    private static MesProcessPoolTeamLeaderScopeDO pqcEmployeeScope(long loginUserId, long actualEmployeeId) {
        return MesProcessPoolTeamLeaderScopeDO.builder()
                .leaderUserId(loginUserId)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .employeeUserId(actualEmployeeId)
                .enabled(true)
                .build();
    }

    private static AdminUserRespDTO enabledUser(long userId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setUsername("user-" + userId);
        user.setNickname("员工" + userId);
        user.setStatus(0);
        return user;
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot(long routeProcessId, long processId) {
        String parameterSnapshotJson = "[]";
        String deviceSelectionSnapshotJson = "[{\"deviceGroupKey\":\"DEFAULT\",\"selectionMode\":\"SINGLE\",\"deviceIds\":[2101]}]";
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID).routeProcessId(routeProcessId).processId(processId)
                .parameterSnapshotState(MesDeviceParameterSnapshotCodec.STATE_FROZEN)
                .parameterSnapshotJson(parameterSnapshotJson)
                .parameterSnapshotSha256(MesDeviceParameterSnapshotCodec.sha256(parameterSnapshotJson))
                .deviceSelectionSnapshotJson(deviceSelectionSnapshotJson)
                .deviceSelectionSnapshotSha256(MesDeviceSelectionSnapshotCodec.sha256(deviceSelectionSnapshotJson))
                .build();
    }

}
