package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesFrontlinePqcEmployeeSwitchServiceTest {

    private static final long LOGIN_USER_ID = 100L;
    private static final long ACTUAL_EMPLOYEE_ID = 101L;
    private static final long ACTIVE_ORDER_ID = 5001L;
    private static final long WORK_ORDER_ID = 1001L;
    private static final long ROUTE_ID = 2001L;
    private static final long ROUTE_VERSION_ID = 3001L;
    private static final long DCC_PROJECT_ID = 6001L;
    private static final long REGULATION_ID = 7001L;
    private static final long REGULATION_VERSION_ID = 8001L;
    private static final long QA_PROCESS_ID = 9001L;
    private static final long PQC_TASK_ID = 9101L;

    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    private MesPqcInspectionTaskMapper taskMapper;
    private MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private AdminUserApi adminUserApi;
    private MesFrontlinePqcContextService service;

    @BeforeEach
    void setUp() {
        activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
        taskMapper = mock(MesPqcInspectionTaskMapper.class);
        scopeMapper = mock(MesProcessPoolTeamLeaderScopeMapper.class);
        adminUserApi = mock(AdminUserApi.class);

        MesProWorkOrderMapper workOrderMapper = mock(MesProWorkOrderMapper.class);
        MesProRouteMapper routeMapper = mock(MesProRouteMapper.class);
        MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper =
                mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        MesQaInspectionRegulationService regulationService = mock(MesQaInspectionRegulationService.class);
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(
                MesProWorkOrderDO.builder().id(WORK_ORDER_ID).productId(4001L).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(
                MesProRouteDO.builder().id(ROUTE_ID).code("RT-01").name("正式路线").build());
        when(regulationService.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, REGULATION_VERSION_ID))
                .thenReturn(lockedVersion());
        when(taskMapper.selectById(PQC_TASK_ID)).thenReturn(task());
        when(taskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(task()));
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                MesProcessPoolActiveOrderProcessSnapshotDO.builder().activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .routeProcessId(30001L).processId(40001L).build()));
        when(scopeMapper.selectActiveScopesByLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC))
                .thenReturn(List.of(employeeScope(ACTUAL_EMPLOYEE_ID)));
        when(adminUserApi.getUserList(any())).thenReturn(List.of(user(LOGIN_USER_ID), user(ACTUAL_EMPLOYEE_ID)));

        service = new MesFrontlinePqcContextServiceImpl(activeOrderMapper,
                mock(MesProProcessPoolEventMapper.class),
                processSnapshotMapper, workOrderMapper, routeMapper,
                mock(MesProRouteVersionMapper.class), mock(DccProjectCodeMapper.class),
                mock(MesQaInspectionRegulationMapper.class), mock(MesQaInspectionRegulationVersionMapper.class),
                mock(MesQaInspectionRegulationProcessMapper.class),
                mock(MesQaInspectionRegulationItemMapper.class),
                mock(MesQaInspectionRegulationItemEquipmentMapper.class), regulationService, taskMapper,
                mock(MesPqcInspectionPieceDetailMapper.class), mock(MesMdItemService.class), scopeMapper,
                adminUserApi, mock(MesProcessPoolEventService.class),
                mock(MesProProcessPoolPqcRecordMapper.class),
                mock(MesProBatchRecordExecutionSignatureService.class));
    }

    @Test
    void switchesOnlyWithExplicitActiveOrderVersionProcessTaskEmployeeIdentityAndAuthorization() {
        MesFrontlinePqcEmployeeSwitchResult result = service.switchPqcActualEmployee(LOGIN_USER_ID,
                ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, PQC_TASK_ID, ACTUAL_EMPLOYEE_ID);

        assertEquals(LOGIN_USER_ID, result.loginUserId());
        assertEquals(ACTUAL_EMPLOYEE_ID, result.actualEmployeeId());
        assertEquals(REGULATION_VERSION_ID, result.regulationVersionId());
        assertEquals(QA_PROCESS_ID, result.qaProcessId());
        assertEquals(ACTUAL_EMPLOYEE_ID, result.template().actualEmployeeId());
        assertEquals(QA_PROCESS_ID, result.template().qaProcessId());
        verify(taskMapper).selectById(PQC_TASK_ID);
        verify(taskMapper).selectListByActiveOrderId(ACTIVE_ORDER_ID);
    }

    @Test
    void rejectsEveryMismatchedSwitchIdentityDimension() {
        assertIdentityMismatch(ACTIVE_ORDER_ID + 1, REGULATION_VERSION_ID, QA_PROCESS_ID, PQC_TASK_ID);
        assertIdentityMismatch(ACTIVE_ORDER_ID, REGULATION_VERSION_ID + 1, QA_PROCESS_ID, PQC_TASK_ID);
        assertIdentityMismatch(ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID + 1, PQC_TASK_ID);
        assertIdentityMismatch(ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, PQC_TASK_ID + 1);
    }

    @Test
    void rejectsEmployeeOutsideCurrentPqcAuthorizationBeforeReadingTask() {
        long unauthorizedEmployeeId = 999L;

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.switchPqcActualEmployee(LOGIN_USER_ID, ACTIVE_ORDER_ID,
                        REGULATION_VERSION_ID, QA_PROCESS_ID, PQC_TASK_ID, unauthorizedEmployeeId));

        assertEquals(PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND.getCode(), error.getCode());
        verify(taskMapper, never()).selectById(PQC_TASK_ID);
    }

    private void assertIdentityMismatch(long activeOrderId, long regulationVersionId,
                                        long qaProcessId, long pqcTaskId) {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.switchPqcActualEmployee(LOGIN_USER_ID, activeOrderId,
                        regulationVersionId, qaProcessId, pqcTaskId, ACTUAL_EMPLOYEE_ID));
        assertEquals(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH.getCode(), error.getCode());
    }

    private static MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder().id(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID).activeStatus("ACTIVE")
                .dccProjectCodeId(DCC_PROJECT_ID).qaRegulationId(REGULATION_ID)
                .qaRegulationVersionId(REGULATION_VERSION_ID).build();
    }

    private static MesPqcInspectionTaskDO task() {
        return MesPqcInspectionTaskDO.builder().id(PQC_TASK_ID).activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(30001L).processId(40001L)
                .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                .qaItemCode("QA-001")
                .inspectionRuleKey("FIRST").inspectionType("FIRST")
                .businessDate(LocalDate.of(2026, 8, 14)).shiftCode("FIRST").roundNo(1)
                .plannedInspectionQuantity(1).taskStatus("PENDING").build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO lockedVersion() {
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem item =
                MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem.builder()
                        .itemSort(1).itemCode("QA-001").itemName("外观").inspectionMethod("目测")
                        .inspectionTool("目测").samplingPlanText("全检").standardText("应合格")
                        .equipmentRequired(false).resultType("BOOLEAN")
                        .equipmentOptions(List.of()).applicableInspectionTypes(List.of("FIRST"))
                        .firstInspectionQuantity(1).build();
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess process =
                MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess.builder()
                        .qaProcessId(QA_PROCESS_ID).processCode("QA-P01").processName("清洗").sort(1)
                        .items(List.of(item)).build();
        return MesQaInspectionRegulationPublishedVersionRespVO.builder()
                .dccProjectCodeId(DCC_PROJECT_ID).regulationId(REGULATION_ID)
                .publishedVersionId(REGULATION_VERSION_ID).versionNo("G/0").lifecycleStatus("PUBLISHED")
                .inspectionTypeRules(List.of(
                        rule("FIRST", "FIRST", "FIRST", 1, 1, 10),
                        rule("PATROL_AM", "PATROL", "AM", 1, null, 20),
                        rule("PATROL_PM", "PATROL", "PM", 1, null, 30),
                        rule("FINAL", "FINAL", "FINAL", 1, 1, 40)))
                .processes(List.of(process)).build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule rule(
            String key, String inspectionType, String label, int round, Integer fixedQuantity, int sort) {
        return MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule.builder()
                .key(key).inspectionType(inspectionType).label(label).roundLabel(label + round)
                .required("FIRST".equals(key)).fixedQuantity(fixedQuantity)
                .taskRule(sort == 10 ? "ONCE" : "BY_SHIFT").releaseGate("REQUIRED").build();
    }

    private static MesProcessPoolTeamLeaderScopeDO employeeScope(long employeeId) {
        return MesProcessPoolTeamLeaderScopeDO.builder().id(1L).leaderUserId(LOGIN_USER_ID)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .employeeUserId(employeeId).enabled(true).build();
    }

    private static AdminUserRespDTO user(long userId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setUsername("user-" + userId);
        user.setNickname("PQC-" + userId);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }
}
