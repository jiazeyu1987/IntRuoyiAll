package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyTemplateMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import jakarta.annotation.Resource;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@Import(MesProEdhrRehearsalReadinessServiceImpl.class)
class MesProEdhrRehearsalReadinessServiceTest extends BaseDbUnitTest {

    private static final Long ROUTE_ID = 922045L;
    private static final Long EXECUTOR_ID = 611L;
    private static final Long APPROVER_ID = 916L;
    private static final Long ARCHIVER_ID = 1161L;
    private static final Long ROUTE_ARCHIVER_ID = 1162L;
    private static final String BPM_PROCESS_KEY = "mes-edhr-approval-v1";
    private static final List<String> EXECUTOR_REQUIRED_PERMISSIONS = List.of(
            "mes:pro-work-order:query",
            "mes:pro-edhr-batch-execution:query",
            "mes:pro-edhr-batch-execution:create",
            "mes:pro-edhr-batch-execution:update",
            "mes:pro-edhr-batch-execution:close",
            "mes:pro-edhr-work-task:query",
            "mes:pro-batch-record-execution:track",
            "mes:pro-batch-record-execution:domain-trace-query");
    private static final List<String> APPROVER_REQUIRED_PERMISSIONS = List.of(
            "mes:pro-edhr-batch-execution:query",
            "mes:pro-edhr-work-task:query",
            "mes:pro-batch-record-execution:approve",
            "mes:pro-batch-record-execution:track",
            "mes:pro-batch-record-execution:domain-trace-query");
    private static final List<String> ARCHIVER_REQUIRED_PERMISSIONS = List.of(
            "mes:pro-edhr-batch-execution:query",
            "mes:pro-edhr-work-task:query",
            "mes:pro-edhr-batch-execution-archive:create",
            "mes:pro-edhr-batch-execution-archive:query",
            "mes:pro-edhr-batch-execution-archive:download",
            "mes:pro-batch-record-execution:track",
            "mes:pro-batch-record-execution:domain-trace-query");

    @Resource
    private MesProEdhrRehearsalReadinessService readinessService;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProEdhrPermissionScopeMapper permissionScopeMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;
    @Resource
    private MesProBatchRecordVersionMapper batchRecordVersionMapper;
    @Resource
    private NotifyTemplateMapper notifyTemplateMapper;

    @MockitoBean
    private DccElectronicSignatureAuthorizationService signatureAuthorizationService;
    @MockitoBean
    private BpmProcessDefinitionService bpmProcessDefinitionService;
    @MockitoBean
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @MockitoBean
    private MesProEdhrPermissionScopeService permissionScopeService;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;

    @BeforeEach
    void stubCurrentRouteProcessIdentity() {
        lenient().when(routeProcessService.resolveFrozenRouteProcess(anyLong(), anyLong(), isNull()))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .build());
    }

    @Test
    void resolveFrozenRouteProcessId_shouldKeepHistoricalIdentity() {
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(1001L)
                .routeId(ROUTE_ID)
                .processId(2002L)
                .build();
        when(routeProcessService.resolveFrozenRouteProcess(1001L, ROUTE_ID, null))
                .thenReturn(frozenRouteProcess);
        MesProEdhrRehearsalReadinessServiceImpl target =
                AopTestUtils.getTargetObject((MesProEdhrRehearsalReadinessServiceImpl) readinessService);

        Long result = ReflectionTestUtils.invokeMethod(
                target, "resolveFrozenRouteProcessId", ROUTE_ID, 1001L);

        assertEquals(1001L, result);
        verify(routeProcessService, never()).resolveCurrentRouteProcess(1001L, ROUTE_ID, null);
    }

    @Test
    void preflight_passesWhenAllRehearsalPrerequisitesArePresent() {
        seedRequiredMenus(EXECUTOR_ID, 7001L, 901000L, "执行人", EXECUTOR_REQUIRED_PERMISSIONS);
        seedRequiredMenus(APPROVER_ID, 7002L, 902000L, "审批人", APPROVER_REQUIRED_PERMISSIONS);
        seedRequiredMenus(ARCHIVER_ID, 7003L, 903000L, "归档人", ARCHIVER_REQUIRED_PERMISSIONS);
        insertArchiveAssignmentRule(ARCHIVER_ID);
        insertRouteRecord(8001L, 1001L);
        insertPermissionScope(1001L);
        insertReport("RPT-1");
        insertProcessFormFillRule(9902L, 9001L, String.valueOf(EXECUTOR_ID));
        seedBpmNotifyTemplates();
        when(jimuReportGateway.getReportJson("RPT-1")).thenReturn(reviewedReportJson());
        when(signatureAuthorizationService.getAuthorizationMap(List.of(EXECUTOR_ID, APPROVER_ID, ARCHIVER_ID)))
                .thenReturn(Map.of(EXECUTOR_ID, true, APPROVER_ID, true, ARCHIVER_ID, true));
        ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("mes-edhr-approval-v1:1:definition");
        when(bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY)).thenReturn(definition);
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(EXECUTOR_ID));
        when(bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of("mes-edhr-approval-v1:1:definition")))
                .thenReturn(List.of(definitionInfo));
        when(bpmProcessDefinitionService.getProcessDefinitionInfo("mes-edhr-approval-v1:1:definition"))
                .thenReturn(definitionInfo);
        when(bpmProcessDefinitionService.canUserStartProcessDefinition(definitionInfo, EXECUTOR_ID)).thenReturn(true);

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_PASS, result.getOverallStatus(),
                () -> "unexpected blockers: " + result.getItems());
        assertTrue(result.getItems().stream().noneMatch(item ->
                MesProEdhrRehearsalReadinessResult.ITEM_STATUS_BLOCKER.equals(item.getStatus())));
    }

    @Test
    void preflight_acceptsMultipleAssistRowFillRulesForSameReportVersion() {
        seedHappyPathMenusAndSignatures();
        insertArchiveAssignmentRule(ARCHIVER_ID);
        insertRouteRecord(8011L, 1011L);
        insertPermissionScope(1011L);
        insertReport("RPT-1");
        MesProBatchRecordReportDO report = reportMapper.selectByReportId("RPT-1");
        insertProcessFormFillRule(9911L, 0L, "RPT-1",
                report.getBatchRecordDefinitionId(), report.getBatchRecordVersionId(),
                "AR_001", String.valueOf(EXECUTOR_ID));
        insertProcessFormFillRule(9912L, 0L, "RPT-1",
                report.getBatchRecordDefinitionId(), report.getBatchRecordVersionId(),
                "AR_002", String.valueOf(EXECUTOR_ID));
        when(jimuReportGateway.getReportJson("RPT-1")).thenReturn(reviewedReportJson());
        seedBpmStartAllowed();

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_PASS, result.getOverallStatus(),
                () -> "unexpected blockers: " + result.getItems());
    }

    @Test
    void preflight_reportsArchiverThatDoesNotMatchRouteArchiveRuleBeforeRealClose() {
        seedHappyPathMenusSignaturesAndTemplate();
        seedRequiredMenus(ROUTE_ARCHIVER_ID, 7004L, 904000L, "路线归档人", ARCHIVER_REQUIRED_PERMISSIONS);
        insertArchiveAssignmentRule(ROUTE_ARCHIVER_ID);
        when(signatureAuthorizationService.getAuthorizationMap(List.of(EXECUTOR_ID, APPROVER_ID, ARCHIVER_ID)))
                .thenReturn(Map.of(EXECUTOR_ID, true, APPROVER_ID, true, ARCHIVER_ID, true));
        seedBpmStartAllowed();

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "ARCHIVE_RULE_ASSIGNEE_MISMATCH", "archiver");
    }

    @Test
    void preflight_reportsBlockersWithoutRepairingMissingPrerequisites() {
        seedMenu(EXECUTOR_ID, 7001L, 900230L, "eDHR工作任务", "mes:pro-edhr-work-task:query");
        insertRouteRecord(8001L, null);
        when(signatureAuthorizationService.getAuthorizationMap(List.of(EXECUTOR_ID, APPROVER_ID, ARCHIVER_ID)))
                .thenReturn(Map.of(EXECUTOR_ID, true, APPROVER_ID, false, ARCHIVER_ID, false));
        ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("mes-edhr-approval-v1:1:definition");
        when(bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY)).thenReturn(definition);
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(999L));
        when(bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of("mes-edhr-approval-v1:1:definition")))
                .thenReturn(List.of(definitionInfo));
        when(bpmProcessDefinitionService.getProcessDefinitionInfo("mes-edhr-approval-v1:1:definition"))
                .thenReturn(definitionInfo);
        when(bpmProcessDefinitionService.canUserStartProcessDefinition(definitionInfo, EXECUTOR_ID)).thenReturn(false);

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "MENU_PARENT_MISSING", "executor");
        assertHasBlocker(result, "MENU_MISSING", "approver");
        assertHasBlocker(result, "MENU_MISSING", "archiver");
        assertHasBlocker(result, "SIGNATURE_AUTH_MISSING", "approver");
        assertHasBlocker(result, "SIGNATURE_AUTH_MISSING", "archiver");
        assertHasBlocker(result, "BPM_START_USER_DENIED", "executor");
        assertHasBlocker(result, "BPM_NOTIFY_TEMPLATE_MISSING", "bpm");
        assertEquals(0L, permissionScopeMapper.selectCount());
        verify(signatureAuthorizationService, never()).updateAuthorization(APPROVER_ID, true);
    }

    @Test
    void preflight_reportsMissingRecordScopeSignAbilityBeforeRealExecution() {
        seedHappyPathMenusSignaturesAndTemplate();
        when(permissionScopeService.evaluate(argThat(command ->
                command != null && EXECUTOR_ID.equals(command.getActorUserId()))))
                .thenReturn(permissionResult(1001L, Map.of(
                        "VIEW", "ALLOW",
                        "FILL", "ALLOW",
                        "SIGN", "DENY")));
        ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("mes-edhr-approval-v1:1:definition");
        when(bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY)).thenReturn(definition);
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(EXECUTOR_ID));
        when(bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of("mes-edhr-approval-v1:1:definition")))
                .thenReturn(List.of(definitionInfo));
        when(bpmProcessDefinitionService.canUserStartProcessDefinition(definitionInfo, EXECUTOR_ID)).thenReturn(true);

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "PERMISSION_RULE_MISSING", "executor");
    }

    @Test
    void preflight_reportsDuplicateBpmDefinitionInfoBeforeStartEligibilityAssumption() {
        seedHappyPathMenusSignaturesAndTemplate();
        ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("mes-edhr-approval-v1:1:definition");
        when(bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY)).thenReturn(definition);
        BpmProcessDefinitionInfoDO left = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(EXECUTOR_ID));
        BpmProcessDefinitionInfoDO right = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(EXECUTOR_ID));
        when(bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of("mes-edhr-approval-v1:1:definition")))
                .thenReturn(List.of(left, right));

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "BPM_DEFINITION_INFO_MISMATCH", "executor");
        verify(bpmProcessDefinitionService, never()).canUserStartProcessDefinition(left, EXECUTOR_ID);
    }

    @Test
    void preflight_reportsAllMissingPermissionsForRoleMenuReadiness() {
        seedMenu(EXECUTOR_ID, 7001L, 900220L, "eDHR批记录", null);
        seedMenu(EXECUTOR_ID, 7001L, 901000L, "执行人工单查询", "mes:pro-work-order:query");
        seedRequiredMenus(APPROVER_ID, 7002L, 902000L, "审批人", APPROVER_REQUIRED_PERMISSIONS);
        seedRequiredMenus(ARCHIVER_ID, 7003L, 903000L, "归档人", ARCHIVER_REQUIRED_PERMISSIONS);
        insertRouteRecord(8001L, 1001L);
        insertPermissionScope(1001L);
        insertReport("RPT-1");
        seedBpmNotifyTemplates();
        when(jimuReportGateway.getReportJson("RPT-1")).thenReturn(reviewedReportJson());
        when(signatureAuthorizationService.getAuthorizationMap(List.of(EXECUTOR_ID, APPROVER_ID, ARCHIVER_ID)))
                .thenReturn(Map.of(EXECUTOR_ID, true, APPROVER_ID, true, ARCHIVER_ID, true));
        ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("mes-edhr-approval-v1:1:definition");
        when(bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY)).thenReturn(definition);
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(EXECUTOR_ID));
        when(bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of("mes-edhr-approval-v1:1:definition")))
                .thenReturn(List.of(definitionInfo));
        when(bpmProcessDefinitionService.canUserStartProcessDefinition(definitionInfo, EXECUTOR_ID)).thenReturn(true);

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        MesProEdhrRehearsalReadinessResult.Item menuBlocker = result.getItems().stream()
                .filter(item -> "MENU_MISSING".equals(item.getCode()) && "executor".equals(item.getRoleKey()))
                .findFirst()
                .orElseThrow();
        assertTrue(menuBlocker.getMessage().contains("mes:pro-edhr-batch-execution:create"));
        assertTrue(menuBlocker.getMessage().contains("mes:pro-batch-record-execution:domain-trace-query"));
    }

    @Test
    void preflight_reportsUnreviewedTemplateCellRulesWithoutWritingReportJson() {
        seedHappyPathMenusAndSignatures();
        insertRouteRecord(8001L, 1001L);
        insertPermissionScope(1001L);
        insertReport("RPT-1");
        when(jimuReportGateway.getReportJson("RPT-1")).thenReturn(unreviewedReportJson());
        ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("mes-edhr-approval-v1:1:definition");
        when(bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY)).thenReturn(definition);
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(EXECUTOR_ID));
        when(bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of("mes-edhr-approval-v1:1:definition")))
                .thenReturn(List.of(definitionInfo));
        when(bpmProcessDefinitionService.canUserStartProcessDefinition(definitionInfo, EXECUTOR_ID)).thenReturn(true);

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "TEMPLATE_CELL_RULE_UNREVIEWED", "template");
        verify(jimuReportGateway, never()).updateReportJson(anyString(), anyString());
    }

    @Test
    void preflight_reportsMissingProcessFormFillRuleBeforeCreatingFillTask() {
        seedHappyPathMenusAndSignatures();
        insertRouteRecord(8001L, 1001L);
        insertPermissionScope(1001L);
        insertReport("RPT-1");
        when(jimuReportGateway.getReportJson("RPT-1")).thenReturn(reviewedReportJson());
        insertArchiveAssignmentRule(ARCHIVER_ID);
        seedBpmStartAllowed();

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "PROCESS_FORM_FILL_RULE_MISSING", "executor");
    }

    @Test
    void preflight_resolvesLatestApprovedReportVersionWhenRouteBindingStillPointsToOldVersion() {
        seedHappyPathMenusAndSignatures();
        MesProBatchRecordVersionDO oldVersion = insertBatchRecordVersion(76001L, "V1.0", "APPROVED");
        MesProBatchRecordVersionDO latestVersion = insertBatchRecordVersion(76001L, "V2.0", "APPROVED");
        insertRouteRecord(8002L, 1002L, "RPT-OLD", 76001L, oldVersion.getId(), "MAIN");
        insertPermissionScope(1002L);
        insertVersionedReport(9002L, "RPT-OLD", 76001L, oldVersion.getId(), 1, "MAIN");
        insertVersionedReport(9003L, "RPT-LATEST", 76001L, latestVersion.getId(), 1, "MAIN");
        insertProcessFormFillRule(9903L, 9001L, "RPT-LATEST", 76001L, latestVersion.getId(),
                String.valueOf(EXECUTOR_ID));
        when(jimuReportGateway.getReportJson("RPT-LATEST")).thenReturn(reviewedReportJson());
        insertArchiveAssignmentRule(ARCHIVER_ID);
        seedBpmStartAllowed();

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_PASS, result.getOverallStatus(),
                () -> "unexpected blockers: " + result.getItems());
    }

    @Test
    void preflight_reportsBlockerWhenRouteBindingAndReportLackStableDefinitionIdentity() {
        seedHappyPathMenusAndSignatures();
        insertRouteRecord(8003L, 1003L, "RPT-LEGACY-NO-STABLE", null, null, "MAIN");
        insertPermissionScope(1003L);
        insertLegacyReportWithoutStableIdentity("RPT-LEGACY-NO-STABLE");
        insertProcessFormFillRule(9904L, 9001L, "RPT-LEGACY-NO-STABLE", null, null,
                String.valueOf(EXECUTOR_ID));
        when(jimuReportGateway.getReportJson("RPT-LEGACY-NO-STABLE")).thenReturn(reviewedReportJson());
        insertArchiveAssignmentRule(ARCHIVER_ID);
        seedBpmStartAllowed();

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "TEMPLATE_STABLE_IDENTITY_MISSING", "template");
    }

    @Test
    void preflight_reportsDisabledBpmNotifyTemplateBeforeRehearsal() {
        seedHappyPathMenusSignaturesAndTemplate();
        notifyTemplateMapper.updateById(new NotifyTemplateDO()
                .setId(91002L)
                .setStatus(CommonStatusEnum.DISABLE.getStatus()));
        ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("mes-edhr-approval-v1:1:definition");
        when(bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY)).thenReturn(definition);
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(EXECUTOR_ID));
        when(bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of("mes-edhr-approval-v1:1:definition")))
                .thenReturn(List.of(definitionInfo));
        when(bpmProcessDefinitionService.canUserStartProcessDefinition(definitionInfo, EXECUTOR_ID)).thenReturn(true);

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "BPM_NOTIFY_TEMPLATE_DISABLED", "bpm");
    }

    @Test
    void preflight_ignoresLegacyDisabledBatchFlowWhenRouteBatchRecordsExist() {
        seedHappyPathMenusSignaturesAndTemplate();
        insertArchiveAssignmentRule(ARCHIVER_ID);
        MesProRouteFlowConfigDO flowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(ROUTE_ID, "BATCH");
        flowConfig.setEnabled(Boolean.FALSE);
        routeFlowConfigMapper.updateById(flowConfig);
        seedBpmStartAllowed();

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_PASS, result.getOverallStatus(),
                () -> "unexpected blockers: " + result.getItems());
    }

    @Test
    void preflight_allowsBatchRecordWithoutPermissionScopeWhenBindingAndFillRuleExist() {
        seedHappyPathMenusAndSignatures();
        insertArchiveAssignmentRule(ARCHIVER_ID);
        insertRouteRecord(8001L, null);
        insertReport("RPT-1");
        insertProcessFormFillRule(9901L, 9001L, String.valueOf(EXECUTOR_ID));
        when(jimuReportGateway.getReportJson("RPT-1")).thenReturn(reviewedReportJson());
        seedBpmStartAllowed();

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_PASS, result.getOverallStatus(),
                () -> "unexpected blockers: " + result.getItems());
        verify(permissionScopeService, never()).evaluate(any(MesProEdhrPermissionEvaluateCommand.class));
    }

    @Test
    void preflight_reportsMissingBatchRecordWhenBindingBelongsToDisabledProcessConfig() {
        seedHappyPathMenusSignaturesAndTemplate();
        MesProRouteFlowProcessConfigDO enabledConfig =
                routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(ROUTE_ID, "BATCH").stream()
                        .findFirst()
                        .orElseThrow();
        MesProRouteFlowProcessConfigDO disabledConfig = new MesProRouteFlowProcessConfigDO()
                .setRouteFlowConfigId(enabledConfig.getRouteFlowConfigId())
                .setRouteId(enabledConfig.getRouteId())
                .setRouteProcessId(9002L)
                .setUseType(enabledConfig.getUseType())
                .setEnabled(Boolean.FALSE)
                .setExecutionMode(enabledConfig.getExecutionMode());
        routeFlowProcessConfigMapper.insert(disabledConfig);
        MesProRouteFlowProcessBatchRecordDO binding =
                routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(ROUTE_ID, "BATCH").stream()
                        .findFirst()
                        .orElseThrow();
        binding.setRouteFlowProcessConfigId(disabledConfig.getId());
        routeFlowProcessBatchRecordMapper.updateById(binding);
        seedBpmStartAllowed();

        MesProEdhrRehearsalReadinessResult result = readinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(ROUTE_ID)
                .setExecutorUserId(EXECUTOR_ID)
                .setApproverUserId(APPROVER_ID)
                .setArchiverUserId(ARCHIVER_ID));

        assertEquals(MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED, result.getOverallStatus());
        assertHasBlocker(result, "ROUTE_BATCH_RECORD_MISSING", "route");
    }

    private void assertHasBlocker(MesProEdhrRehearsalReadinessResult result, String code, String roleKey) {
        assertTrue(result.getItems().stream().anyMatch(item ->
                        code.equals(item.getCode())
                                && roleKey.equals(item.getRoleKey())
                                && MesProEdhrRehearsalReadinessResult.ITEM_STATUS_BLOCKER.equals(item.getStatus())),
                "Missing blocker " + code + " for " + roleKey);
    }

    private void seedMenu(Long userId, Long roleId, Long menuId, String menuName, String permission) {
        if (userRoleMapper.selectListByUserId(userId).stream().noneMatch(row -> roleId.equals(row.getRoleId()))) {
            userRoleMapper.insert(new UserRoleDO().setUserId(userId).setRoleId(roleId));
        }
        if (menuMapper.selectById(menuId) == null) {
            menuMapper.insert(new MenuDO()
                    .setId(menuId)
                    .setName(menuName)
                    .setParentId(0L)
                    .setPermission(permission)
                    .setStatus(0));
        }
        roleMenuMapper.insert(new RoleMenuDO().setRoleId(roleId).setMenuId(menuId));
    }

    private void seedHappyPathMenusSignaturesAndTemplate() {
        seedHappyPathMenusAndSignatures();
        insertRouteRecord(8001L, 1001L);
        insertPermissionScope(1001L);
        insertReport("RPT-1");
        insertProcessFormFillRule(9901L, 9001L, String.valueOf(EXECUTOR_ID));
        when(jimuReportGateway.getReportJson("RPT-1")).thenReturn(reviewedReportJson());
    }

    private void seedHappyPathMenusAndSignatures() {
        seedRequiredMenus(EXECUTOR_ID, 7001L, 901000L, "执行人", EXECUTOR_REQUIRED_PERMISSIONS);
        seedRequiredMenus(APPROVER_ID, 7002L, 902000L, "审批人", APPROVER_REQUIRED_PERMISSIONS);
        seedRequiredMenus(ARCHIVER_ID, 7003L, 903000L, "归档人", ARCHIVER_REQUIRED_PERMISSIONS);
        when(signatureAuthorizationService.getAuthorizationMap(List.of(EXECUTOR_ID, APPROVER_ID, ARCHIVER_ID)))
                .thenReturn(Map.of(EXECUTOR_ID, true, APPROVER_ID, true, ARCHIVER_ID, true));
        seedBpmNotifyTemplates();
    }

    private void seedBpmStartAllowed() {
        ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("mes-edhr-approval-v1:1:definition");
        when(bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY)).thenReturn(definition);
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId("mes-edhr-approval-v1:1:definition")
                .setStartUserIds(List.of(EXECUTOR_ID));
        when(bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of("mes-edhr-approval-v1:1:definition")))
                .thenReturn(List.of(definitionInfo));
        when(bpmProcessDefinitionService.getProcessDefinitionInfo("mes-edhr-approval-v1:1:definition"))
                .thenReturn(definitionInfo);
        when(bpmProcessDefinitionService.canUserStartProcessDefinition(definitionInfo, EXECUTOR_ID)).thenReturn(true);
    }

    private void seedRequiredMenus(Long userId,
                                   Long roleId,
                                   long firstPermissionMenuId,
                                   String roleName,
                                   List<String> permissions) {
        seedMenu(userId, roleId, 900220L, "eDHR批记录", null);
        for (int index = 0; index < permissions.size(); index++) {
            seedMenu(userId, roleId, firstPermissionMenuId + index, roleName + "权限" + index, permissions.get(index));
        }
    }

    private void seedBpmNotifyTemplates() {
        insertNotifyTemplate(91001L, "MES_EDHR_BPM_TASK_ASSIGNED", CommonStatusEnum.ENABLE.getStatus());
        insertNotifyTemplate(91002L, "MES_EDHR_BPM_APPROVED", CommonStatusEnum.ENABLE.getStatus());
        insertNotifyTemplate(91003L, "MES_EDHR_BPM_REJECTED", CommonStatusEnum.ENABLE.getStatus());
        insertNotifyTemplate(91004L, "MES_EDHR_BPM_TASK_TIMEOUT", CommonStatusEnum.ENABLE.getStatus());
    }

    private void insertNotifyTemplate(Long id, String code, Integer status) {
        notifyTemplateMapper.insert(new NotifyTemplateDO()
                .setId(id)
                .setName(code)
                .setCode(code)
                .setType(1)
                .setNickname("system")
                .setContent(code + " content")
                .setParams(List.of("taskName"))
                .setStatus(status));
    }

    private void insertRouteRecord(Long id, Long permissionScopeId) {
        insertRouteRecord(id, permissionScopeId, "RPT-1", null, null, null);
    }

    private void insertRouteRecord(Long id, Long permissionScopeId, String reportId,
                                   Long batchRecordDefinitionId, Long batchRecordVersionId,
                                   String formSlotType) {
        MesProRouteFlowConfigDO flowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(ROUTE_ID, "BATCH");
        if (flowConfig == null) {
            flowConfig = new MesProRouteFlowConfigDO()
                    .setRouteId(ROUTE_ID)
                    .setUseType("BATCH")
                    .setEnabled(Boolean.TRUE)
                    .setConfigVersion("TEST-BATCH");
            routeFlowConfigMapper.insert(flowConfig);
        }
        MesProRouteFlowProcessConfigDO processConfig = new MesProRouteFlowProcessConfigDO()
                .setRouteFlowConfigId(flowConfig.getId())
                .setRouteId(ROUTE_ID)
                .setRouteProcessId(9001L)
                .setUseType("BATCH")
                .setEnabled(Boolean.TRUE)
                .setExecutionMode("SEQUENTIAL");
        routeFlowProcessConfigMapper.insert(processConfig);
        routeFlowProcessBatchRecordMapper.insert(new MesProRouteFlowProcessBatchRecordDO()
                .setId(id)
                .setRouteFlowProcessConfigId(processConfig.getId())
                .setRouteId(ROUTE_ID)
                .setRouteProcessId(9001L)
                .setUseType("BATCH")
                .setBatchRecordReportId(reportId)
                .setBatchRecordDefinitionId(batchRecordDefinitionId)
                .setBatchRecordVersionId(batchRecordVersionId)
                .setFormSlotType(formSlotType)
                .setReportSort(1)
                .setPermissionScopeId(permissionScopeId));
    }

    private void insertPermissionScope(Long id) {
        permissionScopeMapper.insert(new MesProEdhrPermissionScopeDO()
                .setId(id)
                .setScopeName("record-table-RPT-1")
                .setObjectType("RECORD_TABLE")
                .setObjectId("RPT-1")
                .setStatus("ENABLED")
                .setVersion(1));
        mockRecordScopeAbilitiesAllowed();
    }

    private void insertArchiveAssignmentRule(Long archiverUserId) {
        MesProEdhrWorkTaskAssignmentRuleDO rule = assignmentRuleMapper.selectByScopeAndType(
                "ROUTE", ROUTE_ID, MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE);
        if (rule == null) {
            assignmentRuleMapper.insert(new MesProEdhrWorkTaskAssignmentRuleDO()
                    .setId(980000L + archiverUserId)
                    .setScopeType("ROUTE")
                    .setScopeId(ROUTE_ID)
                    .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE)
                    .setAssigneeUserId(archiverUserId)
                    .setCandidateSourceType("USER")
                    .setCandidateSourceId(archiverUserId)
                    .setDueMinutes(1440)
                    .setEnabled(true)
                    .setRemark("演练归档责任规则"));
            return;
        }
        assignmentRuleMapper.updateById(new MesProEdhrWorkTaskAssignmentRuleDO()
                .setId(rule.getId())
                .setAssigneeUserId(archiverUserId)
                .setCandidateSourceType("USER")
                .setCandidateSourceId(archiverUserId)
                .setDueMinutes(1440)
                .setEnabled(true)
                .setRemark("演练归档责任规则"));
    }

    private void insertProcessFormFillRule(Long id, Long routeProcessId, String candidateSourceIds) {
        MesProBatchRecordReportDO report = reportMapper.selectByReportId("RPT-1");
        insertProcessFormFillRule(id, routeProcessId, "RPT-1",
                report == null ? null : report.getBatchRecordDefinitionId(),
                report == null ? null : report.getBatchRecordVersionId(),
                candidateSourceIds);
    }

    private void insertProcessFormFillRule(Long id, Long routeProcessId, String reportId,
                                           Long batchRecordDefinitionId, Long batchRecordVersionId,
                                           String candidateSourceIds) {
        insertProcessFormFillRule(id, routeProcessId, reportId, batchRecordDefinitionId, batchRecordVersionId,
                "ALL", candidateSourceIds);
    }

    private void insertProcessFormFillRule(Long id, Long routeProcessId, String reportId,
                                           Long batchRecordDefinitionId, Long batchRecordVersionId,
                                           String scopeKey, String candidateSourceIds) {
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setId(id)
                .setRouteProcessId(routeProcessId)
                .setBatchRecordReportId(reportId)
                .setBatchRecordDefinitionId(batchRecordDefinitionId)
                .setBatchRecordVersionId(batchRecordVersionId)
                .setRuleType("FILL")
                .setScopeKey(scopeKey)
                .setCandidateSourceType("USER")
                .setCandidateSourceIds(candidateSourceIds)
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(480)
                .setEnabled(true)
                .setRemark("演练填写权限规则"));
    }

    private void mockRecordScopeAbilitiesAllowed() {
        when(permissionScopeService.evaluate(any(MesProEdhrPermissionEvaluateCommand.class))).thenAnswer(invocation -> {
            MesProEdhrPermissionEvaluateCommand command = invocation.getArgument(0);
            Map<String, String> decisions = new LinkedHashMap<>();
            for (String ability : command.getAbilities()) {
                decisions.put(ability, "ALLOW");
            }
            return permissionResult(command.getScopeId(), decisions)
                    .setObjectType(command.getObjectType())
                    .setObjectId(command.getObjectId());
        });
    }

    private MesProEdhrPermissionEvaluateResult permissionResult(Long scopeId, Map<String, String> decisions) {
        return new MesProEdhrPermissionEvaluateResult()
                .setScopeId(scopeId)
                .setObjectType("RECORD_TABLE")
                .setObjectId("RPT-1")
                .setDecisions(decisions);
    }

    private void insertReport(String reportId) {
        Long definitionId = 76000L + Math.abs((long) reportId.hashCode());
        MesProBatchRecordVersionDO version = insertBatchRecordVersion(definitionId, "V1.0", "APPROVED");
        reportMapper.insert(new MesProBatchRecordReportDO()
                .setId(9001L)
                .setSampleKey("SAMPLE-1")
                .setBatchRecordName("棘突球囊")
                .setFormSlotType("MAIN")
                .setRouteKey("A")
                .setBatchRecordDefinitionId(definitionId)
                .setBatchRecordVersionId(version.getId())
                .setSourceFileName("route-a.doc")
                .setSourceFileSha256("sha256")
                .setSourceTableIndex(1)
                .setReportCategoryId("category-edhr-readiness")
                .setLastImportTime(LocalDateTime.now())
                .setReportId(reportId)
                .setReportCode(reportId)
                .setReportName("Route A Report"));
    }

    private void insertLegacyReportWithoutStableIdentity(String reportId) {
        reportMapper.insert(new MesProBatchRecordReportDO()
                .setId(9004L)
                .setSampleKey("SAMPLE-LEGACY")
                .setBatchRecordName("棘突球囊")
                .setFormSlotType("MAIN")
                .setRouteKey("A")
                .setSourceFileName("legacy-route-a.doc")
                .setSourceFileSha256("legacy-sha256")
                .setSourceTableIndex(1)
                .setReportCategoryId("category-edhr-readiness")
                .setLastImportTime(LocalDateTime.now())
                .setReportId(reportId)
                .setReportCode(reportId)
                .setReportName("Legacy Route A Report"));
    }

    private MesProBatchRecordVersionDO insertBatchRecordVersion(Long definitionId, String versionNo, String status) {
        MesProBatchRecordVersionDO version = MesProBatchRecordVersionDO.builder()
                .definitionId(definitionId)
                .versionNo(versionNo)
                .status(status)
                .sourceFileName(versionNo + ".doc")
                .sourceFileSha256("sha-" + definitionId + "-" + versionNo)
                .approvedAt("APPROVED".equals(status) ? LocalDateTime.now() : null)
                .build();
        batchRecordVersionMapper.insert(version);
        return version;
    }

    private void insertVersionedReport(Long id, String reportId, Long definitionId, Long versionId,
                                       Integer sourceTableIndex, String formSlotType) {
        reportMapper.insert(new MesProBatchRecordReportDO()
                .setId(id)
                .setSampleKey("SAMPLE-" + reportId)
                .setBatchRecordName("棘突球囊")
                .setProductName("棘突球囊")
                .setFormSlotType(formSlotType)
                .setRouteKey("A")
                .setBatchRecordDefinitionId(definitionId)
                .setBatchRecordVersionId(versionId)
                .setSourceFileName(reportId + ".doc")
                .setSourceFileSha256("sha256-" + reportId)
                .setSourceTableIndex(sourceTableIndex)
                .setReportCategoryId("category-edhr-readiness")
                .setLastImportTime(LocalDateTime.now())
                .setReportId(reportId)
                .setReportCode(reportId)
                .setReportName("Route A Report"));
    }

    private String reviewedReportJson() {
        return """
                {
                  "rows": {
                    "0": {
                      "cells": {
                        "0": {
                          "text": "",
                          "fillForm": {"field": "ebr_r0_c0"},
                          "edhrCellRule": {
                            "rowIndex": 0,
                            "columnIndex": 0,
                            "valueType": "STRING",
                            "componentFlag": "input-text",
                            "required": true,
                            "reviewed": true
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private String unreviewedReportJson() {
        return """
                {
                  "rows": {
                    "0": {
                      "cells": {
                        "0": {
                          "text": "",
                          "fillForm": {"field": "ebr_r0_c0"}
                        }
                      }
                    }
                  }
                }
                """;
    }
}
