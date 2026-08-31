package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotRule;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineRuntimeConfigServiceTest {

    private static final Long LOGIN_USER_ID = 9001L;
    private static final Long ROUTE_ID = 101L;
    private static final Long ROUTE_PROCESS_ID = 1001L;
    private static final Long PROCESS_ID = 201L;

    @Mock
    private MesFrontlineDeviceAccountContextService contextService;
    @Mock
    private MesFrontlineTemplateResolver templateResolver;
    @Mock
    private MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    @Mock
    private MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    @Mock
    private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesProcessPoolDefectReasonMapper defectReasonMapper;
    @Mock
    private MesFrontlineSessionSnapshotService sessionSnapshotService;
    @Mock
    private MesFrontlineActiveOrderProcessService activeOrderProcessService;
    @Mock
    private MesFrontlineProcessMaterialService processMaterialService;

    private MesFrontlineRuntimeConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlineRuntimeConfigServiceImpl(contextService, templateResolver, employeeProfileMapper,
                processDeviceMapper, deviceMapper, parameterRuleMapper, processSnapshotMapper,
                defectReasonMapper, sessionSnapshotService, activeOrderProcessService, processMaterialService);
        org.mockito.Mockito.lenient().when(sessionSnapshotService.issue(any()))
                .thenReturn(new MesFrontlineSessionSnapshotReference("snapshot-001", "hash-001"));
        org.mockito.Mockito.lenient().when(contextService.resolveResponsibleLeaderUserId(LOGIN_USER_ID))
                .thenReturn(LOGIN_USER_ID);
        org.mockito.Mockito.lenient().when(templateResolver.resolve(any(MesFrontlineTemplateRequest.class)))
                .thenAnswer(invocation -> {
                    MesFrontlineTemplateRequest request = invocation.getArgument(0);
                    return new MesFrontlineTemplateDescriptor("FRONTLINE-PROD", "PRODUCTION",
                            request.routeProcessId(), request.processId(), request.actualEmployeeId());
                });
    }

    @Test
    void getRuntimeConfig_returnsTeamLeaderConfiguredRuntimeOptions() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        7001L, "D-001", "压力泵", 301L, "WS-301", "精洗工位"));
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, 9001L, null, "TMP-001", "临时工甲", "临时工甲-A", "TEMPORARY", true),
                employeeProfile(8802L, 9002L, 10002L, "E-002", "其它班组员工", "其它班组员工", "FORMAL", true)));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(
                processDevice(9001L, 7001L),
                processDevice(9001L, 7002L),
                processDevice(9001L, 7003L)));
        when(deviceMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                teamDevice(7001L, 9001L, "D-001", "压力泵", "ENABLED", true),
                teamDevice(7002L, 9001L, "D-002", "报修泵", "REPAIRING", true),
                teamDevice(7003L, 9001L, "D-003", "禁用泵", "DISABLED", false)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(7001L, ROUTE_PROCESS_ID, "pressure", "压力", "MPa",
                        "10", "20", "15", "DECIMAL"),
                MesProcessPoolDeviceParameterRuleDO.builder()
                        .leaderUserId(9001L)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .deviceId(7001L)
                        .parameterCode("y-cleaning-power")
                        .parameterName("清洗功率")
                        .unit("%")
                        .lowerLimit(new BigDecimal("20"))
                        .upperLimit(new BigDecimal("30"))
                        .standardText("20-30%")
                        .valueType("DECIMAL")
                        .enabled(Boolean.TRUE)
                        .build(),
                MesProcessPoolDeviceParameterRuleDO.builder()
                        .leaderUserId(9001L)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .deviceId(7001L)
                        .parameterCode("y-single-bound")
                        .parameterName("单边范围")
                        .unit("%")
                        .lowerLimit(new BigDecimal("20"))
                        .standardText(">=20%")
                        .valueType("DECIMAL")
                        .enabled(Boolean.TRUE)
                        .build(),
                parameterRule(7001L, null, "legacy-pressure", "历史空路线压力", "MPa",
                        "10", "20", "15", "DECIMAL"),
                parameterRule(7001L, 2002L, "temperature", "温度", "℃",
                        "30", "60", "45", "DECIMAL"),
                MesProcessPoolDeviceParameterRuleDO.builder()
                        .leaderUserId(9001L)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .deviceId(7001L)
                        .parameterCode("z-cleaning-medium")
                        .parameterName("清洗介质")
                        .standardText("纯化水")
                        .valueType("TEXT_STANDARD")
                        .enabled(Boolean.TRUE)
                        .build()));
        when(defectReasonMapper.selectList(any())).thenReturn(List.of(
                defectReason(8301L, ROUTE_PROCESS_ID, "LOSS", "LOSS-001", "正常损耗"),
                defectReason(8302L, 2002L, "LOSS", "LOSS-002", "其它工序损耗")));
        when(templateResolver.resolve(new MesFrontlineTemplateRequest(
                LOGIN_USER_ID, 8801L, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID)))
                .thenReturn(new MesFrontlineTemplateDescriptor(
                        "FRONTLINE-PROD", "PRODUCTION", ROUTE_PROCESS_ID, PROCESS_ID, 8801L));

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(ROUTE_ID, config.routeId());
        assertEquals(ROUTE_PROCESS_ID, config.routeProcessId());
        assertEquals(PROCESS_ID, config.processId());
        assertEquals(1, config.employees().size());
        assertEquals("临时工甲-A", config.employees().get(0).employeeName());
        assertNull(config.employees().get(0).systemUserId());
        assertEquals(1, config.devices().size());
        assertEquals("压力泵", config.devices().get(0).deviceName());
        assertEquals("ENABLED", config.devices().get(0).deviceStatus());
        assertEquals(4, config.devices().get(0).parameters().size());
        assertEquals("pressure", config.devices().get(0).parameters().get(0).parameterCode());
        assertEquals(new BigDecimal("15"), config.devices().get(0).parameters().get(0).defaultValue());
        assertEquals("10-20MPa，目标15MPa", config.devices().get(0).parameters().get(0).standardText());
        assertEquals(new BigDecimal("25"), config.devices().get(0).parameters().get(1).defaultValue());
        assertNull(config.devices().get(0).parameters().get(2).defaultValue());
        assertEquals("纯化水", config.devices().get(0).parameters().get(3).standardText());
        assertEquals("TEXT_STANDARD", config.devices().get(0).parameters().get(3).valueType());
        assertNull(config.devices().get(0).parameters().get(3).lowerLimit());
        assertNull(config.devices().get(0).parameters().get(3).defaultValue());
        assertEquals(1, config.defectReasons().size());
        assertEquals("正常损耗", config.defectReasons().get(0).reasonName());
        assertEquals(ROUTE_ID, config.productionSubmitContext().routeId());
        assertEquals(ROUTE_PROCESS_ID, config.productionSubmitContext().routeProcessId());
        assertEquals(PROCESS_ID, config.productionSubmitContext().processId());
        assertEquals(301L, config.productionSubmitContext().workstationId());
        assertEquals(LOGIN_USER_ID, config.productionSubmitContext().approveUserId());
        assertNull(config.productionSubmitContext().workOrderId());
        assertNull(config.productionSubmitContext().workOrderCode());
        assertNull(config.productionSubmitContext().taskId());
        assertNull(config.productionSubmitContext().itemId());
        assertNull(config.productionSubmitContext().recordbookId());
        assertEquals(1, config.employeeSwitchSnapshots().size());
        assertEquals(8801L, config.employeeSwitchSnapshots().get(0).actualEmployeeId());
        assertEquals(ROUTE_PROCESS_ID, config.employeeSwitchSnapshots().get(0).routeProcessId());
        assertEquals("FRONTLINE-PROD", config.employeeSwitchSnapshots().get(0).template().templateNo());
        assertEquals(8801L, config.employeeSwitchSnapshots().get(0).template().actualEmployeeId());
    }

    @Test
    void getRuntimeConfig_returnsEnabledLeaderPersonnelProfilesInsteadOfOnlyProcessBindings() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        7001L, "D-001", "压力泵", 301L, "WS-301", "精洗工位"));
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, 9001L, null, "TMP-001", "临时工甲", "临时工甲-A", "TEMPORARY", true),
                employeeProfile(8803L, 9001L, 10003L, "USER-10003", "正式工乙", "正式工乙", "FORMAL", true),
                employeeProfile(8804L, 9001L, 10004L, "USER-10004", "禁用员工", "禁用员工", "FORMAL", false),
                employeeProfile(8805L, 9002L, 10005L, "USER-10005", "其它组员工", "其它组员工", "FORMAL", true)));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(processDevice(9001L, 7001L)));
        when(deviceMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                teamDevice(7001L, 9001L, "D-001", "压力泵", "ENABLED", true)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of());
        when(defectReasonMapper.selectList(any())).thenReturn(List.of());

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(2, config.employees().size());
        assertEquals(8801L, config.employees().get(0).employeeProfileId());
        assertEquals(8803L, config.employees().get(1).employeeProfileId());
        assertEquals(10003L, config.employees().get(1).systemUserId());
        assertEquals(2, config.employeeSwitchSnapshots().size());
        assertEquals(8801L, config.employeeSwitchSnapshots().get(0).actualEmployeeId());
        assertEquals(10003L, config.employeeSwitchSnapshots().get(1).actualEmployeeId());
        assertEquals(10003L, config.employeeSwitchSnapshots().get(1).template().actualEmployeeId());
    }

    @Test
    void getRuntimeConfig_usesFrozenActiveOrderParameterStandardAfterCurrentRuleChanges() {
        when(activeOrderProcessService.requireProcess(LOGIN_USER_ID, 8101L, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineActiveOrderProcess(8101L, ROUTE_ID, 627L, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        301L, "WS-301", "精洗工位",
                        new BigDecimal("1.000000"), new BigDecimal("100.000000"), Boolean.FALSE));
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, LOGIN_USER_ID, 10001L, "LOGIN-001",
                        "当前组长人员", "当前组长人员", "FORMAL", true)));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(processDevice(LOGIN_USER_ID, 7001L)));
        when(deviceMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                teamDevice(7001L, LOGIN_USER_ID, "D-001", "压力泵", "ENABLED", true)));
        lenient().when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(7001L, ROUTE_PROCESS_ID, "pressure", "压力", "MPa",
                        "0", "12", "6", "DECIMAL")));
        when(defectReasonMapper.selectList(any())).thenReturn(List.of());
        String frozenJson = JsonUtils.toJsonString(List.of(MesDeviceParameterSnapshotRule.builder()
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .deviceId(7001L)
                .parameterCode("pressure")
                .parameterName("压力")
                .unit("MPa")
                .lowerLimit(BigDecimal.ZERO)
                .upperLimit(new BigDecimal("10"))
                .defaultValue(new BigDecimal("5"))
                .valueType("DECIMAL")
                .standardText("0-10MPa，目标5MPa")
                .build()));
        when(processSnapshotMapper.selectByActiveOrderAndProcess(8101L, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesProcessPoolActiveOrderProcessSnapshotDO()
                        .setId(5101L)
                        .setActiveOrderId(8101L)
                        .setRouteId(ROUTE_ID)
                        .setRouteProcessId(ROUTE_PROCESS_ID)
                        .setProcessId(PROCESS_ID)
                        .setParameterSnapshotJson(frozenJson)
                        .setParameterSnapshotSha256(MesDeviceParameterSnapshotCodec.sha256(frozenJson))
                        .setParameterSnapshotState(MesDeviceParameterSnapshotCodec.STATE_FROZEN));

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, 8101L, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(new BigDecimal("10"), config.devices().get(0).parameters().get(0).upperLimit());
        assertEquals(5101L, config.productionSubmitContext().activeOrderProcessSnapshotId());
        assertEquals(MesDeviceParameterSnapshotCodec.STATE_FROZEN,
                config.productionSubmitContext().parameterSnapshotState());
        verify(parameterRuleMapper, never()).selectList(any());
    }

    @Test
    void getRuntimeConfig_usesFrozenActiveOrderProcessInsteadOfCurrentRouteAuthorization() {
        Long activeOrderId = 8101L;
        Long frozenRouteProcessId = 980645L;
        when(activeOrderProcessService.requireProcess(LOGIN_USER_ID, activeOrderId, ROUTE_ID,
                frozenRouteProcessId, PROCESS_ID)).thenReturn(new MesFrontlineActiveOrderProcess(activeOrderId,
                ROUTE_ID, 627L, "RT000028", "球囊扩张压力泵", frozenRouteProcessId, PROCESS_ID,
                "ER0C9BD936FFAE", "粗洗工序", 1, 980010L, "WS-CX", "粗洗工位",
                new BigDecimal("1.000000"), new BigDecimal("100.000000"), Boolean.FALSE));
        when(templateResolver.resolve(new MesFrontlineTemplateRequest(LOGIN_USER_ID, 10001L, ROUTE_ID,
                frozenRouteProcessId, PROCESS_ID, Boolean.FALSE))).thenReturn(new MesFrontlineTemplateDescriptor(
                "PRODUCTION_SIMPLIFIED", "PRODUCTION", frozenRouteProcessId, PROCESS_ID, 10001L));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of());
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, LOGIN_USER_ID, 10001L, "LOGIN-001",
                        "当前组长人员", "当前组长人员", "FORMAL", true)));
        when(defectReasonMapper.selectList(any())).thenReturn(List.of());
        when(processSnapshotMapper.selectByActiveOrderAndProcess(activeOrderId, frozenRouteProcessId, PROCESS_ID))
                .thenReturn(new MesProcessPoolActiveOrderProcessSnapshotDO()
                        .setId(5101L)
                        .setActiveOrderId(activeOrderId)
                        .setRouteId(ROUTE_ID)
                        .setRouteVersionId(627L)
                        .setRouteProcessId(frozenRouteProcessId)
                        .setProcessId(PROCESS_ID));
        when(processMaterialService.listFrozenMaterials(activeOrderId, ROUTE_ID, frozenRouteProcessId, PROCESS_ID))
                .thenReturn(List.of(
                        new MesFrontlineProcessMaterial(501L, "A001", "弹簧", null, BigDecimal.ONE),
                        new MesFrontlineProcessMaterial(502L, "A002", "杠杆", null, BigDecimal.ONE)));

        MesFrontlineRuntimeConfig config = assertDoesNotThrow(() -> service.getRuntimeConfig(
                LOGIN_USER_ID, activeOrderId, ROUTE_ID, frozenRouteProcessId, PROCESS_ID));

        assertEquals(frozenRouteProcessId, config.routeProcessId());
        assertEquals(List.of(501L, 502L), config.materials().stream()
                .map(MesFrontlineProcessMaterial::materialId).toList());
        assertEquals("PRODUCTION_SIMPLIFIED", config.employeeSwitchSnapshots().get(0).template().templateNo());
        assertEquals(5101L, config.productionSubmitContext().activeOrderProcessSnapshotId());
        ArgumentCaptor<MesFrontlineSessionSnapshotContent> snapshotCaptor =
                ArgumentCaptor.forClass(MesFrontlineSessionSnapshotContent.class);
        verify(sessionSnapshotService).issue(snapshotCaptor.capture());
        assertEquals(config.materials(), snapshotCaptor.getValue().materials());
        verify(contextService, never()).requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID,
                frozenRouteProcessId, PROCESS_ID);
    }

    @Test
    void getRuntimeConfig_doesNotRequireActiveOrderWhenFrontlineProductionHasNoWorkOrder() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        null, null, null, 301L, "WS-301", "精洗工位"));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of());
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, LOGIN_USER_ID, 10001L, "LOGIN-001",
                        "当前组长人员", "当前组长人员", "FORMAL", true)));
        when(defectReasonMapper.selectList(any())).thenReturn(List.of());

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(ROUTE_ID, config.productionSubmitContext().routeId());
        assertEquals(ROUTE_PROCESS_ID, config.productionSubmitContext().routeProcessId());
        assertEquals(PROCESS_ID, config.productionSubmitContext().processId());
        assertEquals(301L, config.productionSubmitContext().workstationId());
        assertEquals(LOGIN_USER_ID, config.productionSubmitContext().approveUserId());
        assertNull(config.productionSubmitContext().workOrderId());
        assertNull(config.productionSubmitContext().workOrderCode());
        assertNull(config.productionSubmitContext().taskId());
        assertNull(config.productionSubmitContext().itemId());
        assertNull(config.productionSubmitContext().recordbookId());
    }

    @Test
    void getRuntimeConfig_usesCurrentLoginLeaderPersonnelWhenDeviceScopeBelongsToAnotherLeader() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        7001L, "D-001", "压力泵", 301L, "WS-301", "精洗工位"));
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, LOGIN_USER_ID, 10001L, "LOGIN-001",
                        "当前组长人员", "当前组长人员", "FORMAL", true),
                employeeProfile(8802L, 9002L, 10002L, "DEVICE-001",
                        "设备scope人员", "设备scope人员", "FORMAL", true)));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(processDevice(9002L, 7001L)));
        when(deviceMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                teamDevice(7001L, 9002L, "D-001", "压力泵", "ENABLED", true)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of());
        when(defectReasonMapper.selectList(any())).thenReturn(List.of());

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(1, config.employees().size());
        assertEquals(8801L, config.employees().get(0).employeeProfileId());
        assertEquals("当前组长人员", config.employees().get(0).employeeName());
        assertEquals(1, config.devices().size());
        assertEquals("压力泵", config.devices().get(0).deviceName());
    }

    @Test
    void getRuntimeConfig_keepsLeaderScopeWhenRouteStartCandidateDeviceHasNoTeamBinding() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        41L, "DV-041", "正式工位设备", 301L, "WS-301", "精洗工位",
                        MesFrontlineRouteProcessCandidate.CONTEXT_SOURCE_ROUTE_START_PRODUCTION_LEADER));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of());
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, LOGIN_USER_ID, 10001L, "LOGIN-001",
                        "当前组长人员", "当前组长人员", "FORMAL", true)));
        when(defectReasonMapper.selectList(any())).thenReturn(List.of());

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(1, config.employees().size());
        assertEquals(8801L, config.employees().get(0).employeeProfileId());
        assertEquals("当前组长人员", config.employees().get(0).employeeName());
        assertEquals(0, config.devices().size());
    }

    @Test
    void getRuntimeConfig_returnsEnabledLossReasonsByRouteProcessWithoutLeaderOwnership() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        null, null, null, 301L, "WS-301", "精洗工位"));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of());
        when(defectReasonMapper.selectList(any())).thenReturn(List.of(
                defectReason(8301L, ROUTE_PROCESS_ID, "LOSS", "LOSS-001", "正常损耗").setLeaderUserId(9999L),
                defectReason(8302L, 2002L, "LOSS", "LOSS-002", "其它工序损耗").setLeaderUserId(9999L),
                defectReason(8303L, ROUTE_PROCESS_ID, "LOSS", "LOSS-003", "停用损耗")
                        .setLeaderUserId(9999L).setEnabled(Boolean.FALSE)));

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(1, config.defectReasons().size());
        assertEquals(8301L, config.defectReasons().get(0).reasonId());
        assertEquals("LOSS-001", config.defectReasons().get(0).reasonCode());
    }

    private static MesProcessPoolTeamEmployeeProfileDO employeeProfile(Long id, Long leaderUserId, Long systemUserId,
                                                                       String employeeCode, String employeeName,
                                                                       String displayName, String employeeType,
                                                                       boolean enabled) {
        return MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(id)
                .leaderUserId(leaderUserId)
                .systemUserId(systemUserId)
                .employeeCode(employeeCode)
                .employeeName(employeeName)
                .displayName(displayName)
                .employeeType(employeeType)
                .enabled(enabled)
                .build();
    }

    private static MesProcessPoolTeamProcessDeviceDO processDevice(Long leaderUserId, Long deviceId) {
        return MesProcessPoolTeamProcessDeviceDO.builder()
                .leaderUserId(leaderUserId)
                .processId(PROCESS_ID)
                .deviceId(deviceId)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProcessPoolTeamDeviceDO teamDevice(Long id, Long leaderUserId, String deviceCode,
                                                         String deviceName, String deviceStatus, boolean enabled) {
        return MesProcessPoolTeamDeviceDO.builder()
                .id(id)
                .leaderUserId(leaderUserId)
                .deviceCode(deviceCode)
                .deviceName(deviceName)
                .deviceStatus(deviceStatus)
                .enabled(enabled)
                .build();
    }

    private static MesProcessPoolDeviceParameterRuleDO parameterRule(Long deviceId, Long routeProcessId,
                                                                     String parameterCode, String parameterName,
                                                                     String unit, String lowerLimit,
                                                                     String upperLimit, String defaultValue,
                                                                     String valueType) {
        return MesProcessPoolDeviceParameterRuleDO.builder()
                .leaderUserId(9001L)
                .routeProcessId(routeProcessId)
                .processId(PROCESS_ID)
                .deviceId(deviceId)
                .parameterCode(parameterCode)
                .parameterName(parameterName)
                .unit(unit)
                .lowerLimit(new BigDecimal(lowerLimit))
                .upperLimit(new BigDecimal(upperLimit))
                .defaultValue(new BigDecimal(defaultValue))
                .standardText(lowerLimit + "-" + upperLimit + unit + "，目标" + defaultValue + unit)
                .valueType(valueType)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProcessPoolDefectReasonDO defectReason(Long id, Long routeProcessId, String reasonType,
                                                             String reasonCode, String reasonName) {
        return MesProcessPoolDefectReasonDO.builder()
                .id(id)
                .leaderUserId(9001L)
                .routeProcessId(routeProcessId)
                .processId(PROCESS_ID)
                .reasonType(reasonType)
                .reasonCode(reasonCode)
                .reasonName(reasonName)
                .enabled(Boolean.TRUE)
                .build();
    }

}
