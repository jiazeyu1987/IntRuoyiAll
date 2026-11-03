package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.*;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesProRouteFullFlowReadinessServiceTest {
    private final MesProRouteVersionMapper versions = mock(MesProRouteVersionMapper.class);
    private final MesProRouteVersionSnapshotResolver snapshots = mock(MesProRouteVersionSnapshotResolver.class);
    private final MesProRouteProcessFlowService graphs = mock(MesProRouteProcessFlowService.class);
    private final MesProRouteCandidateConfigServiceImpl production = mock(MesProRouteCandidateConfigServiceImpl.class);
    private final MesProRouteFlowConfigService configs = mock(MesProRouteFlowConfigService.class);
    private final MesTeamLeaderActiveOrderService orders = mock(MesTeamLeaderActiveOrderService.class);
    private final MesFixedTestOrderModeGuard mode = mock(MesFixedTestOrderModeGuard.class);
    private final MesProEdhrCandidateResolver candidates = mock(MesProEdhrCandidateResolver.class);
    private final MesProductionReleaseRequiredCandidateResolver release = mock(MesProductionReleaseRequiredCandidateResolver.class);
    private final MesProBatchRecordReportMapper reports = mock(MesProBatchRecordReportMapper.class);
    private final MesProBatchRecordJimuReportGateway reportGateway = mock(MesProBatchRecordJimuReportGateway.class);
    private final MesProEdhrWorkTaskService tasks = mock(MesProEdhrWorkTaskService.class);
    private final FormTemplateVersionMapper templates = mock(FormTemplateVersionMapper.class);
    private final MesProRouteFullFlowReadinessService service = new MesProRouteFullFlowReadinessService(
            versions, snapshots, graphs, production, configs, orders, mode, candidates, release, reports, reportGateway, tasks, templates);
    private MesProRouteVersionDO version;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(9L);
        version = new MesProRouteVersionDO();
        version.setId(20L); version.setRouteId(10L); version.setActive(true);
        version.setLifecycleStatus("ACTIVE"); version.setVersionNo("V3");
        when(versions.selectById(20L)).thenReturn(version);
        when(versions.selectActiveByRouteId(10L)).thenReturn(version);
        when(release.resolveRequiredCandidates(eq(9L), anyString())).thenAnswer(call ->
                new MesProductionReleaseRoleCandidates(5L, call.getArgument(1), List.of(101L), "hash"));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    private void validConfiguration() {
        JSONObject root = JSON.parseObject("""
            {"routeId":10,"routeCode":"R","routeName":"路线","configSnapshots":{
              "products":[],"scheduleConfigs":[],"scheduleUseConfigs":[],
              "flowGraph":{"nodes":[{"routeProcessId":1,"processId":2}]},
              "productionProcessConfigSchemaVersion":1,
              "productionProcessConfigs":[{"routeProcessId":1,"processId":2,"overagePercent":0,
                "lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}],
              "batchUseConfigs":[{"routeProcessId":1,"enabled":true,"formBindings":[],
                "batchRecordReports":[{"routeBindingId":3,"batchRecordReportId":"r1",
                "batchRecordDefinitionId":4,"batchRecordVersionId":5}]}]}}
            """);
        when(snapshots.resolveVersion(20L)).thenReturn(new MesProRouteVersionSnapshotResolver.ResolvedRouteVersionSnapshot(10L,20L,root.toJSONString(),"hash"));
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setValid(true); graph.setValidationStatus("VALID");
        when(graphs.getGraph(10L,20L)).thenReturn(graph);
        when(configs.getRouteStartProductionLeaders(10L,20L)).thenReturn(List.of(
                new MesProRouteStartProductionLeaderRespVO().setProductionLineId(10L).setCandidateSourceType("USERS").setCandidateSourceIds(List.of(101L))));
        when(configs.getBatchRecordAttachmentOwners(10L,20L)).thenReturn(java.util.stream.IntStream.range(0,4).mapToObj(i ->
                new MesProRouteBatchRecordAttachmentOwnerRespVO().setAttachmentCode("A"+i).setAttachmentName("附件"+i)
                        .setCandidateSourceType("USERS").setCandidateSourceIds(List.of(101L))).toList());
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setBatchRecordDefinitionId(4L); report.setBatchRecordVersionId(5L);
        when(reports.selectByReportId("r1")).thenReturn(report);
        when(reportGateway.getReportJsonReadOnly("r1")).thenReturn("{\"rows\":{}}");
    }

    @Test void passingConfigurationNeverClaimsFutureProductionOrReleasePassed() {
        validConfiguration();
        var result = service.check(10L,20L,101L);
        assertEquals("PENDING",result.overallStatus());
        assertTrue(result.items().stream().noneMatch(i -> i.status().equals("BLOCKED")), result.items().toString());
        for (String stage : List.of("重置","双100%完工","PQC生产放行","上市放行"))
            assertTrue(result.items().stream().anyMatch(i -> stage.equals(i.stage()) && "PENDING".equals(i.status())));
        verify(orders).checkFixedTestOrderProductionSource(10L,20L);
        verify(orders, never()).resetFixedSimulationActiveOrder(anyLong());
        verify(tasks).checkRouteFillReadiness(10L,20L,1L,null,3L,"r1",5L,null,null);
    }
    @Test void draftCannotBorrowAnotherActiveVersion() {
        version.setActive(false); version.setLifecycleStatus("DRAFT");
        var result = service.check(10L,20L,101L);
        assertEquals("BLOCKED",result.overallStatus());
        verifyNoInteractions(snapshots, orders, production, tasks);
        verify(versions,never()).selectActiveByRouteId(anyLong());
    }
    @Test void otherRouteVersionIsRejected() {
        version.setRouteId(99L);
        assertEquals("BLOCKED",service.check(10L,20L,101L).overallStatus());
        verifyNoInteractions(snapshots,orders,production);
    }
    @Test void damagedSnapshotBlocksDependentChecksButStillChecksReleaseRoles() {
        when(snapshots.resolveVersion(20L)).thenThrow(new IllegalStateException("hash mismatch"));
        var result=service.check(10L,20L,101L);
        assertEquals("BLOCKED",result.overallStatus());
        assertTrue(result.items().stream().anyMatch(i -> "hash mismatch".equals(i.detail())));
        verifyNoInteractions(production, configs, tasks);
        verify(release).resolveRequiredCandidates(9L,MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
    }
    @Test void missingCandidateIsVisibleAndDoesNotSuppressOtherStages() {
        validConfiguration();
        when(release.resolveRequiredCandidates(9L,MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER))
                .thenThrow(new MesProductionReleaseRoleResolutionException(9L,MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER,
                        MesProductionReleaseRoleResolutionReason.CANDIDATE_EMPTY));
        var result=service.check(10L,20L,101L);
        assertEquals("BLOCKED",result.overallStatus());
        assertTrue(result.items().stream().anyMatch(i -> "PQC生产放行".equals(i.stage()) && "BLOCKED".equals(i.status())));
        verify(release).resolveRequiredCandidates(9L,MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
    }
    @Test void unavailableDatabaseFailsRequestRatherThanReturningSuccess() {
        when(versions.selectById(20L)).thenThrow(new DataAccessResourceFailureException("database unavailable"));
        assertThrows(DataAccessResourceFailureException.class, () -> service.check(10L,20L,101L));
    }
    @Test void missingBatchVersionIsBlockedRatherThanFilledFromSlots() {
        validConfiguration();
        when(reports.selectByReportId("r1")).thenReturn(null);
        var result=service.check(10L,20L,101L);
        assertEquals("BLOCKED",result.overallStatus());
        assertTrue(result.items().stream().anyMatch(i -> i.stage().equals("批记录") && i.status().equals("BLOCKED")));
        verifyNoInteractions(templates);
    }
    @Test void formSlotsAreInspectedIndependentlyOfMissingBatchReport() {
        validConfiguration();
        var resolved=snapshots.resolveVersion(20L);
        JSONObject root=JSON.parseObject(resolved.routeSnapshotJson());
        root.getJSONObject("configSnapshots").getJSONArray("batchUseConfigs").getJSONObject(0)
                .put("formBindings", JSON.parseArray("[{\"routeBindingId\":8,\"formBindingKey\":\"SLOT-1\",\"formTemplateId\":30,\"lastPublishedTemplateVersionId\":31}]"));
        when(snapshots.resolveVersion(20L)).thenReturn(new MesProRouteVersionSnapshotResolver.ResolvedRouteVersionSnapshot(10L,20L,root.toJSONString(),"hash"));
        var template=new cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO();
        template.setId(31L); template.setTemplateId(30L); template.setStatus("PUBLISHED");
        when(templates.selectById(31L)).thenReturn(template);
        when(reports.selectByReportId("r1")).thenReturn(null);
        var result=service.check(10L,20L,101L);
        assertTrue(result.items().stream().anyMatch(i -> i.stage().equals("表单槽位") && i.status().equals("PASS")));
        assertTrue(result.items().stream().anyMatch(i -> i.stage().equals("批记录") && i.status().equals("BLOCKED")));
        verify(tasks).checkRouteFillReadiness(10L,20L,1L,null,8L,null,null,"SLOT-1",31L);
    }
    @Test void invalidGraphBlocksCompletionWithoutHidingOtherConfigurationFindings() {
        validConfiguration();
        MesProRouteProcessFlowGraphRespVO graph=new MesProRouteProcessFlowGraphRespVO();
        graph.setValid(false); graph.setValidationStatus("INVALID");
        when(graphs.getGraph(10L,20L)).thenReturn(graph);
        var result=service.check(10L,20L,101L);
        assertEquals("BLOCKED",result.overallStatus());
        assertTrue(result.items().stream().anyMatch(i -> i.name().equals("工序连接完整性") && i.status().equals("BLOCKED")));
        verify(orders).checkFixedTestOrderProductionSource(10L,20L);
        verify(release).resolveRequiredCandidates(9L,MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
    }
    @Test void endpointUsesExistingRoutePermissionAndTransactionIsReadOnly() throws Exception {
        assertTrue(MesProRouteFullFlowReadinessService.class.getMethod("check",Long.class,Long.class,Long.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class).readOnly());
        var method=cn.iocoder.yudao.module.mes.controller.admin.pro.route.MesProRouteProcessFlowController.class
                .getMethod("checkFullFlowReadiness",Long.class,Long.class);
        assertEquals("@ss.hasPermission('mes:pro-route:query')",method
                .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value());
    }

}
