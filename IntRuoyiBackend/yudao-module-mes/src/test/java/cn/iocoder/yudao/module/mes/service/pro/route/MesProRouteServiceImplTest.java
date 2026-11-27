package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowNodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchPolicySettingsRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench.MesProSchedulerWorkbenchService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteServiceImplTest {
    @Mock private org.springframework.beans.factory.ObjectProvider<MesProRouteTreeService> routeTrees;
    @Mock private MesProRouteTreeService routeTreeService;
    @org.mockito.Mock private MesProRouteDeviceIdentitySnapshotService deviceIdentitySnapshots;


    @Mock
    private cn.iocoder.yudao.module.mes.service.audit.MesVersionOperateLogService versionOperateLogService;

    @Mock
    private MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private MesProRouteProcessFlowBoundaryEdgeMapper routeProcessFlowBoundaryEdgeMapper;

    @Test
    void pageShowsDistinctFormalProductCountAndBoundProjectCode() {
        var request = new MesProRoutePageReqVO();
        when(routeMapper.selectPage(request)).thenReturn(new PageResult<>(
                List.of(MesProRouteDO.builder().id(11L).build(), MesProRouteDO.builder().id(12L).build(),
                        MesProRouteDO.builder().id(13L).build()), 3L));
        when(routeProductMapper.selectListByRouteIds(Set.of(11L, 12L, 13L))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(1L).routeId(11L).itemId(101L).build(),
                MesProRouteProductDO.builder().id(2L).routeId(11L).itemId(102L).build(),
                MesProRouteProductDO.builder().id(3L).routeId(11L).itemId(101L).build()));
        when(routeDccProjectBindingMapper.selectCurrentListByRouteIds(Set.of(11L, 12L, 13L)))
                .thenReturn(List.of(MesRouteDccProjectBindingDO
                        .builder().routeId(12L).dccProjectCodeId(201L).build()));
        var project = new DccProjectCodeDO();
        project.setId(201L);
        project.setProjectCode("INT-201");
        when(dccProjectCodeMapper.selectByIds(Set.of(201L))).thenReturn(List.of(project));

        var page = routeService.getRoutePageRespVO(request);
        assertEquals(3L, page.getTotal());
        assertEquals(2, page.getList().get(0).getProductCount());
        assertEquals(0, page.getList().get(1).getProductCount());
        assertEquals("INT-201", page.getList().get(1).getDccProjectCode());
        assertEquals(0, page.getList().get(2).getProductCount());
        assertNull(page.getList().get(2).getDccProjectCode());
        verify(routeDccProjectBindingMapper).selectCurrentListByRouteIds(Set.of(11L, 12L, 13L));
        verify(dccProjectCodeMapper).selectByIds(Set.of(201L));
    }

    @Test
    void deletionReferenceQueryDistinguishesTerminalDeletedAndOtherRoutes() throws Exception {
        String sql = String.join(" ", MesProRouteMapper.class
                .getMethod("existsUnfinishedBusinessByRouteId", Long.class)
                .getAnnotation(org.apache.ibatis.annotations.Select.class).value())
                .replace("#{routeId}", "12");
        try (var connection = java.sql.DriverManager.getConnection("jdbc:h2:mem:route_delete_refs;MODE=MySQL");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE mes_pro_process_pool_active_order(route_id BIGINT, deleted INT, active_status VARCHAR)");
            statement.execute("CREATE TABLE mes_pro_schedule_order(route_id BIGINT, deleted INT, status INT)");
            statement.execute("CREATE TABLE mes_pro_edhr_batch_execution(route_id BIGINT, deleted INT, status INT)");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(12,0,'CLOSED'),(12,1,'ACTIVE'),(99,0,'ACTIVE')");
            statement.execute("INSERT INTO mes_pro_schedule_order VALUES(12,0,3),(12,0,4),(12,1,0),(99,0,0)");
            statement.execute("INSERT INTO mes_pro_edhr_batch_execution VALUES(12,0,30),(12,0,40),(12,0,50),(12,0,60),(12,1,10),(99,0,10)");
            try (var rows = statement.executeQuery(sql)) { rows.next(); assertFalse(rows.getBoolean(1)); }
            for (String table : List.of("mes_pro_process_pool_active_order", "mes_pro_schedule_order", "mes_pro_edhr_batch_execution")) {
                String status = table.endsWith("active_order") ? "'ACTIVE'" : "0";
                statement.execute("INSERT INTO " + table + " VALUES(12,0," + status + ")");
                try (var rows = statement.executeQuery(sql)) { rows.next(); assertTrue(rows.getBoolean(1), table); }
                statement.execute("DELETE FROM " + table);
            }
        }
    }

    @Test
    void deletedRouteDetailMustNotReadHistoricalRow() {
        AssertUtils.assertServiceException(() -> routeService.getRouteRespVO(12L),
                ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS);
        verify(routeMapper, never()).selectByIdIgnoreDeleted(anyLong());
    }

    @Test
    void unfinishedBusinessPreventsAllDeletionSideEffects() {
        when(routeTrees.getObject()).thenReturn(routeTreeService);
        when(routeMapper.selectByIdForUpdate(12L)).thenReturn(MesProRouteDO.builder()
                .id(12L).status(CommonStatusEnum.DISABLE.getStatus()).build());
        when(routeMapper.existsUnfinishedBusinessByRouteId(12L)).thenReturn(true);
        var error = org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> routeService.deleteRoute(12L));
        assertTrue(error.getMessage().contains("仍被活跃订单"));
        verify(routeMapper, never()).deleteById(anyLong());
        org.mockito.Mockito.verifyNoInteractions(routeVersionMapper, routeProcessFlowService,
                routeProcessService, routeProductService, routeProductBomService);
    }

    @Test
    void unusedDisabledRouteCanBeDeleted() {
        when(routeTrees.getObject()).thenReturn(routeTreeService);
        when(routeMapper.selectByIdForUpdate(12L)).thenReturn(MesProRouteDO.builder()
                .id(12L).status(CommonStatusEnum.DISABLE.getStatus()).build());
        routeService.deleteRoute(12L);
        verify(routeProcessFlowService).deleteByRouteId(12L);
        verify(routeProcessService).deleteRouteProcessByRouteId(12L);
        verify(routeProductService).deleteRouteProductByRouteId(12L);
        verify(routeProductBomService).deleteRouteProductBomByRouteId(12L);
        verify(routeMapper).deleteById(12L);
    }

    @Test
    void deletingUnassignedDirectoryReleasesItsUniqueCodeForNextImport() {
        when(routeTrees.getObject()).thenReturn(routeTreeService);
        when(routeMapper.selectByIdForUpdate(12L)).thenReturn(MesProRouteDO.builder().id(12L)
                .nodeType("GROUP").code(MesProRouteTreeService.UNASSIGNED_CODE).status(1).build());
        when(routeMapper.updateById(any(MesProRouteDO.class))).thenReturn(1);
        routeService.deleteRoute(12L);
        var update = ArgumentCaptor.forClass(MesProRouteDO.class);
        verify(routeMapper).updateById(update.capture());
        assertEquals("GROUP-UNASSIGNED-DELETED-12", update.getValue().getCode());
        verify(routeMapper).deleteById(12L);
    }

    @Mock
    private cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProjectGuard projectGuard;

    @InjectMocks
    private MesProRouteServiceImpl routeService;

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private cn.iocoder.yudao.module.system.api.user.AdminUserApi adminUserApi;
    @Mock
    private cn.iocoder.yudao.module.system.api.permission.RoleApi roleApi;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Mock
    private MesProRouteProductService routeProductService;
    @Mock
    private MesProRouteProductBomService routeProductBomService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesProSchedulerWorkbenchService schedulerWorkbenchService;
    @Mock
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;
    @Mock
    private MesProEdhrPermissionScopeService permissionScopeService;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock
    private MesProRouteWorkstationService routeWorkstationService;

    private MesProRouteCreateReqVO creationRequest(List<Long> processIds) {
        MesProRouteCreateReqVO request = new MesProRouteCreateReqVO();
        request.setName("  新建流程  ");
        request.setProcessIds(processIds);
        request.setWorkshopId(70L);
        return request;
    }

    @Test
    void createWithProcesses_rejectsTrimmedDuplicateBeforeGeneratingCode() {
        when(routeMapper.selectByName("新建流程")).thenReturn(MesProRouteDO.builder().id(20L).build());
        AssertUtils.assertServiceException(() -> routeService.createRouteWithProcesses(creationRequest(List.of(8L))),
                ErrorCodeConstants.PRO_ROUTE_NAME_DUPLICATE);
        org.mockito.Mockito.verifyNoInteractions(autoCodeRecordService);
        verify(routeMapper, never()).insert(any(MesProRouteDO.class));
    }

    @Test
    void createWithProcesses_rejectsEmptyRepeatedAndMissingProcessesWithoutWriting() {
        AssertUtils.assertServiceException(() -> routeService.createRouteWithProcesses(creationRequest(List.of())),
                ErrorCodeConstants.PRO_ROUTE_ENABLE_NO_PROCESS);
        AssertUtils.assertServiceException(() -> routeService.createRouteWithProcesses(creationRequest(List.of(8L, 8L))),
                ErrorCodeConstants.PRO_ROUTE_PROCESS_DUPLICATE);
        AssertUtils.assertServiceException(() -> routeService.createRouteWithProcesses(creationRequest(List.of(999L))),
                ErrorCodeConstants.PRO_ROUTE_PROCESS_NOT_EXISTS);
        org.mockito.Mockito.verifyNoInteractions(autoCodeRecordService);
        verify(routeMapper, never()).insert(any(MesProRouteDO.class));
    }

    @Test
    void createWithProcesses_concurrentNameConflictReturnsBusinessError() {
        when(processMapper.selectById(8L)).thenReturn(new MesProProcessDO());
        when(autoCodeRecordService.generateProjectCode(org.mockito.ArgumentMatchers.eq("PRO_ROUTE_CODE"), org.mockito.ArgumentMatchers.any())).thenReturn("GYLC-0001");
        when(routeMapper.insert(any(MesProRouteDO.class))).thenThrow(
                new org.springframework.dao.DuplicateKeyException("Duplicate uk_mes_pro_route_name_live"));
        AssertUtils.assertServiceException(() -> routeService.createRouteWithProcesses(creationRequest(List.of(8L))),
                ErrorCodeConstants.PRO_ROUTE_NAME_DUPLICATE);
        verify(routeProcessMapper, never()).insert(any(MesProRouteProcessDO.class));
        org.mockito.Mockito.verifyNoInteractions(platformAdapter);
    }

    @Test
    void createWithProcesses_buildsOrderedGraphAndCompleteV1() {
        assertInitialCreation(List.of(8L, 3L, 12L));
    }

    @Test
    void createWithProcesses_singleProcessHasBothBoundariesAndNoMiddleEdge() {
        assertInitialCreation(List.of(8L));
    }

    private void assertInitialCreation(List<Long> processIds) {
        when(routeWorkstationService.create(eq(500L), org.mockito.ArgumentMatchers.anyInt(), any(), eq(70L)))
                .thenAnswer(call -> 900L + (int) call.getArgument(1));
        java.util.List<MesProRouteProcessDO> insertedProcesses = new java.util.ArrayList<>();
        java.util.List<MesProRouteScheduleConfigDO> scheduleConfigs = new java.util.ArrayList<>();
        java.util.List<MesProRouteFlowProcessConfigDO> useConfigs = new java.util.ArrayList<>();
        MesProRouteVersionDO[] storedVersion = new MesProRouteVersionDO[1];
        for (Long id : processIds) {
            when(processMapper.selectById(id)).thenReturn(new MesProProcessDO());
        }
        when(autoCodeRecordService.generateProjectCode(org.mockito.ArgumentMatchers.eq("PRO_ROUTE_CODE"), org.mockito.ArgumentMatchers.any())).thenReturn("GYLC-10000");
        org.mockito.Mockito.doAnswer(invocation -> {
            ((MesProRouteDO) invocation.getArgument(0)).setId(500L);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteProcessDO process = invocation.getArgument(0);
            process.setId(600L + insertedProcesses.size());
            insertedProcesses.add(process);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            storedVersion[0] = invocation.getArgument(0);
            storedVersion[0].setId(700L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));
        when(routeVersionMapper.selectActiveByRouteId(500L)).thenAnswer(invocation -> storedVersion[0]);
        when(routeVersionMapper.selectById(700L)).thenAnswer(invocation -> storedVersion[0]);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO update = invocation.getArgument(0);
            storedVersion[0].setRouteSnapshotJson(update.getRouteSnapshotJson());
            storedVersion[0].setRouteSnapshotSha256(update.getRouteSnapshotSha256());
            return 1;
        }).when(routeVersionMapper).updateById(any(MesProRouteVersionDO.class));
        MesProSchedulerWorkbenchPolicySettingsRespVO defaults = new MesProSchedulerWorkbenchPolicySettingsRespVO();
        defaults.setDefaultNightShiftEnabled(false);
        defaults.setDefaultScheduleUseEnabled(false);
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaults);
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder().id(800L).routeId(500L).useType("SCHEDULE").build();
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(500L, "SCHEDULE")).thenReturn(flowConfig);
        org.mockito.Mockito.doAnswer(invocation -> {
            scheduleConfigs.add(invocation.getArgument(0));
            return 1;
        }).when(routeScheduleConfigMapper).insert(any(MesProRouteScheduleConfigDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            useConfigs.add(invocation.getArgument(0));
            return 1;
        }).when(routeFlowProcessConfigMapper).insert(any(MesProRouteFlowProcessConfigDO.class));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(scheduleConfigs);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(500L, "SCHEDULE")).thenReturn(useConfigs);
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        when(routeProcessFlowService.getGraph(500L)).thenReturn(graph);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteProcessFlowSaveReqVO request = invocation.getArgument(0);
            graph.setEdges(JSON.parseArray(JSON.toJSONString(request.getEdges()),
                    cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowEdgeRespVO.class));
            graph.setBoundaryEdges(JSON.parseArray(JSON.toJSONString(request.getBoundaryEdges()),
                    cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowBoundaryEdgeRespVO.class));
            for (MesProRouteProcessDO process : insertedProcesses) {
                MesProRouteProcessFlowNodeRespVO node = new MesProRouteProcessFlowNodeRespVO();
                node.setRouteProcessId(process.getId());
                node.setProcessId(process.getProcessId());
                node.setRouteProcessWorkstationId(process.getWorkstationId());
                graph.getNodes().add(node);
            }
            return null;
        }).when(routeProcessFlowService).saveGraph(any(MesProRouteProcessFlowSaveReqVO.class));

        assertEquals(500L, routeService.createRouteWithProcesses(creationRequest(processIds)));
        ArgumentCaptor<MesProRouteDO> route = ArgumentCaptor.forClass(MesProRouteDO.class);
        verify(routeMapper).insert(route.capture());
        assertEquals("新建流程", route.getValue().getName());
        assertEquals("GYLC-10000", route.getValue().getCode());
        assertNull(route.getValue().getRemark());
        assertEquals(processIds, insertedProcesses.stream().map(MesProRouteProcessDO::getProcessId).toList());
        for (int i = 0; i < processIds.size(); i++) assertEquals(i + 1, insertedProcesses.get(i).getSort());
        assertEquals(processIds.size() - 1, graph.getEdges().size());
        for (int i = 0; i < graph.getEdges().size(); i++) {
            assertEquals(insertedProcesses.get(i).getId(), graph.getEdges().get(i).getSourceRouteProcessId());
            assertEquals(insertedProcesses.get(i + 1).getId(), graph.getEdges().get(i).getTargetRouteProcessId());
        }
        assertEquals("START", graph.getBoundaryEdges().get(0).getBoundaryType());
        assertEquals(insertedProcesses.get(0).getId(), graph.getBoundaryEdges().get(0).getRouteProcessId());
        assertEquals("END", graph.getBoundaryEdges().get(1).getBoundaryType());
        assertEquals(insertedProcesses.get(processIds.size() - 1).getId(), graph.getBoundaryEdges().get(1).getRouteProcessId());
        JSONObject snapshot = JSON.parseObject(storedVersion[0].getRouteSnapshotJson());
        JSONObject configs = snapshot.getJSONObject("configSnapshots");
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(storedVersion[0].getRouteSnapshotJson()));
        assertEquals(processIds.size(), configs.getJSONArray("productionProcessConfigs").size());
        assertEquals(processIds, configs.getJSONArray("productionProcessConfigs").stream()
                .map(value -> ((JSONObject) value).getLong("processId")).toList());
        assertEquals(processIds.size(), configs.getJSONObject("flowGraph").getJSONArray("nodes").size());
        for (int i = 0; i < processIds.size(); i++) {
            assertEquals(901L + i, insertedProcesses.get(i).getWorkstationId());
            assertEquals(901L + i, configs.getJSONObject("flowGraph").getJSONArray("nodes")
                    .getJSONObject(i).getLong("routeProcessWorkstationId"));
            verify(routeWorkstationService).create(eq(500L), eq(i + 1), any(), eq(70L));
        }
        assertEquals(processIds.size(), configs.getJSONArray("scheduleConfigs").size());
        assertEquals(processIds.size(), configs.getJSONArray("scheduleUseConfigs").size());
        assertEquals("V1", storedVersion[0].getVersionNo());
        verify(routeVersionMapper).insert(any(MesProRouteVersionDO.class));
        verify(platformAdapter).recordActiveRegistered(eq(storedVersion[0]), eq(null), any());
    }

    @Test
    void createWithProcesses_graphFailureRollsBackRouteAndProcesses() {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:route_creation_rollback;DB_CLOSE_DELAY=-1", "sa", "");
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE route_create_test(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE route_process_create_test(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE workstation_create_test(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE workstation_machine_create_test(id BIGINT PRIMARY KEY)");
        when(routeWorkstationService.create(eq(500L), eq(1), any(), eq(70L))).thenAnswer(call -> {
            jdbc.update("INSERT INTO workstation_create_test VALUES(900)");
            jdbc.update("INSERT INTO workstation_machine_create_test VALUES(901)");
            return 900L;
        });
        when(processMapper.selectById(8L)).thenReturn(new MesProProcessDO());
        when(autoCodeRecordService.generateProjectCode(org.mockito.ArgumentMatchers.eq("PRO_ROUTE_CODE"), org.mockito.ArgumentMatchers.any())).thenReturn("GYLC-0001");
        org.mockito.Mockito.doAnswer(invocation -> {
            ((MesProRouteDO) invocation.getArgument(0)).setId(500L);
            return jdbc.update("INSERT INTO route_create_test VALUES(500)");
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            ((MesProRouteProcessDO) invocation.getArgument(0)).setId(600L);
            return jdbc.update("INSERT INTO route_process_create_test VALUES(600)");
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        org.mockito.Mockito.doThrow(new IllegalStateException("graph initialization failed"))
                .when(routeProcessFlowService).saveGraph(any(MesProRouteProcessFlowSaveReqVO.class));
        org.springframework.aop.framework.ProxyFactory factory = new org.springframework.aop.framework.ProxyFactory(routeService);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource),
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        MesProRouteService transactionalService = (MesProRouteService) factory.getProxy();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> transactionalService.createRouteWithProcesses(creationRequest(List.of(8L))));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM route_create_test", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM route_process_create_test", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM workstation_create_test", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM workstation_machine_create_test", Integer.class));
        org.mockito.Mockito.verifyNoInteractions(platformAdapter);
    }

    @BeforeEach
    void stubCurrentRouteProcessIdentity() {
        ReflectionTestUtils.setField(routeService, "routeOwnerPermissionService",
                new MesProRouteOwnerPermissionServiceImpl(permissionScopeService));
        lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(anyLong(), any()))
                .thenReturn(emptyList());
    }

    @Test
    void createRoute_shouldRejectDuplicateNameAndSkipCreate() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setCode("ROUTE-DUP-NAME");
        reqVO.setName("重复工艺路线");

        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(null);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(MesProRouteDO.builder()
                .id(9001L)
                .code("ROUTE-EXISTING")
                .name(reqVO.getName())
                .build());

        AssertUtils.assertServiceException(
                () -> routeService.createRoute(reqVO),
                ErrorCodeConstants.PRO_ROUTE_NAME_DUPLICATE
        );

        verify(routeMapper, never()).insert(any(MesProRouteDO.class));
        verify(routeVersionMapper, never()).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.class));
    }

    @Test
    void createRoute_shouldRegisterInitialActiveVersionRef() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setCode("ROUTE-NEW-ACTIVE-REF");
        reqVO.setName("新建路线");
        reqVO.setDescription("新建路线说明");

        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(null);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO route = invocation.getArgument(0);
            route.setId(9008L);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO activeVersion = invocation.getArgument(0);
            activeVersion.setId(9208L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        Long routeId = routeService.createRoute(reqVO);

        assertEquals(9008L, routeId);
        ArgumentCaptor<MesProRouteVersionDO> activeCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(platformAdapter).recordActiveRegistered(activeCaptor.capture(), eq(null),
                eq("route active version registered"));
        MesProRouteVersionDO activeVersion = activeCaptor.getValue();
        assertEquals(9208L, activeVersion.getId());
        assertEquals(9008L, activeVersion.getRouteId());
        assertEquals("V1", activeVersion.getVersionNo());
        assertEquals(Boolean.TRUE, activeVersion.getActive());
        assertEquals("ACTIVE", activeVersion.getLifecycleStatus());
    }

    @Test
    void createRoute_shouldGrantRouteEditPermissionToCreator() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setCode("ROUTE-NEW-PERM");
        reqVO.setName("新建权限路线");
        Long routeId = 922119L;
        Long creatorUserId = 1L;

        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(null);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO route = invocation.getArgument(0);
            route.setId(routeId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO activeVersion = invocation.getArgument(0);
            activeVersion.setId(922120L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(creatorUserId);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("admin");

            routeService.createRoute(reqVO);
        }

        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> scopeCaptor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService).saveRules(scopeCaptor.capture());
        MesProEdhrPermissionScopeSaveCommand command = scopeCaptor.getValue();
        assertEquals("route-" + routeId, command.getScopeName());
        assertEquals("ROUTE", command.getObjectType());
        assertEquals(String.valueOf(routeId), command.getObjectId());
        assertEquals(creatorUserId, command.getActorUserId());
        assertEquals("admin", command.getActorUsername());
        assertEquals(List.of("VIEW", "ROUTE_EDIT", "PERMISSION_ADMIN"),
                command.getRules().stream().map(MesProEdhrPermissionRuleCommand::getAbility).toList());
        command.getRules().forEach(rule -> {
            assertEquals("USER", rule.getSubjectType());
            assertEquals(creatorUserId, rule.getSubjectId());
            assertEquals("ALLOW", rule.getDecision());
            assertEquals("ENABLED", rule.getStatus());
        });
    }

    @Test
    void updateRoute_shouldRejectDuplicateNameAndSkipUpdate() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setId(9002L);
        reqVO.setCode("ROUTE-UPDATE");
        reqVO.setName("重复工艺路线");

        when(routeMapper.selectById(reqVO.getId())).thenReturn(MesProRouteDO.builder()
                .id(reqVO.getId())
                .code(reqVO.getCode())
                .name("原工艺路线")
                .build());
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(MesProRouteDO.builder()
                .id(9003L)
                .code("ROUTE-EXISTING")
                .name(reqVO.getName())
                .build());

        AssertUtils.assertServiceException(
                () -> routeService.updateRoute(reqVO),
                ErrorCodeConstants.PRO_ROUTE_NAME_DUPLICATE
        );

        verify(routeMapper, never()).updateById(any(MesProRouteDO.class));
        verify(routeVersionMapper, never()).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.class));
    }

    @Test
    void updateRoute_shouldCreateDraftCandidateVersionWithoutInactivatingActiveVersion() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setId(9101L);
        reqVO.setCode("ROUTE-CANDIDATE");
        reqVO.setName("候选路线");
        reqVO.setDescription("候选变更");

        MesProRouteDO oldRoute = MesProRouteDO.builder()
                .id(reqVO.getId())
                .code(reqVO.getCode())
                .name("原路线")
                .description("原说明")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO activeVersion = MesProRouteVersionDO.builder()
                .id(9201L)
                .routeId(reqVO.getId())
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();

        when(routeMapper.selectById(reqVO.getId())).thenReturn(oldRoute);
        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(oldRoute);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(null);
        when(routeVersionMapper.selectActiveByRouteId(reqVO.getId())).thenReturn(activeVersion);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(reqVO.getId())).thenReturn("V1");
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(reqVO.getId());
        MesProRouteProcessFlowNodeRespVO node = new MesProRouteProcessFlowNodeRespVO();
        node.setRouteProcessId(3001L);
        node.setProcessId(4001L);
        node.setProcessName("工序");
        node.setSort(1);
        graph.setNodes(List.of(node));
        when(routeProcessFlowService.getGraph(reqVO.getId())).thenReturn(graph);

        routeService.updateRoute(reqVO);

        verify(routeMapper, never()).updateById(any(MesProRouteDO.class));
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        ArgumentCaptor<MesProRouteVersionDO> versionCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(versionCaptor.capture());
        MesProRouteVersionDO candidate = versionCaptor.getValue();
        assertEquals(reqVO.getId(), candidate.getRouteId());
        assertEquals("V2", candidate.getVersionNo());
        assertEquals(Boolean.FALSE, candidate.getActive());
        assertEquals("DRAFT", candidate.getLifecycleStatus());
        assertEquals(activeVersion.getId(), candidate.getSourceRouteVersionId());
        JSONObject snapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
        assertEquals(reqVO.getId(), snapshot.getLong("routeId"));
        assertEquals(reqVO.getCode(), snapshot.getString("routeCode"));
        assertEquals(reqVO.getName(), snapshot.getString("routeName"));
        verify(platformAdapter).recordCandidateCreated(activeVersion, candidate, null,
                "route version candidate created");
    }

    @Test
    void updateRoute_shouldCarryActiveConfigSnapshotIntoDraftCandidate() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setId(9102L);
        reqVO.setCode("ROUTE-CANDIDATE-FULL");
        reqVO.setName("候选路线改名");
        reqVO.setDescription("候选变更");

        MesProRouteDO oldRoute = MesProRouteDO.builder()
                .id(reqVO.getId())
                .code(reqVO.getCode())
                .name("原路线")
                .description("原说明")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO activeVersion = MesProRouteVersionDO.builder()
                .id(9202L)
                .routeId(reqVO.getId())
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson(validCompleteRouteSnapshotJson(reqVO.getId(), reqVO.getCode(), "原路线"))
                .build();

        when(routeMapper.selectById(reqVO.getId())).thenReturn(oldRoute);
        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(oldRoute);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(null);
        when(routeVersionMapper.selectActiveByRouteId(reqVO.getId())).thenReturn(activeVersion);
        when(routeVersionMapper.selectById(activeVersion.getId())).thenReturn(activeVersion);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(reqVO.getId())).thenReturn("V1");
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(reqVO.getId());
        MesProRouteProcessFlowNodeRespVO node = new MesProRouteProcessFlowNodeRespVO();
        node.setRouteProcessId(3001L);
        node.setProcessId(4001L);
        node.setProcessName("工序");
        node.setSort(1);
        graph.setNodes(List.of(node));
        when(routeProcessFlowService.getGraph(reqVO.getId())).thenReturn(graph);

        routeService.updateRoute(reqVO);

        ArgumentCaptor<MesProRouteVersionDO> versionCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(versionCaptor.capture());
        MesProRouteVersionDO candidate = versionCaptor.getValue();
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(candidate.getRouteSnapshotJson()));
        JSONObject snapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
        assertEquals(reqVO.getName(), snapshot.getString("routeName"));
        assertTrue(snapshot.getJSONObject("configSnapshots").containsKey("flowGraph"));
        assertTrue(snapshot.getJSONObject("configSnapshots").containsKey("scheduleUseConfigs"));
    }

    @Test
    void copyRoute_shouldAppendSuffixWhenTargetNameExists() {
        Long sourceRouteId = 9004L;
        Long targetRouteId = 9005L;
        Long targetVersionId = 9205L;
        String targetCode = "ROUTE-COPY";
        String targetName = "源工艺路线-副本";
        String resolvedTargetName = "源工艺路线-副本2";

        when(routeMapper.selectById(sourceRouteId)).thenReturn(MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("ROUTE-SOURCE")
                .name("源工艺路线")
                .build());
        when(routeMapper.selectByCode(targetCode)).thenReturn(null);
        when(routeMapper.selectByName(targetName)).thenReturn(MesProRouteDO.builder()
                .id(9006L)
                .code("ROUTE-EXISTING")
                .name(targetName)
                .build());
        when(routeMapper.selectByName(resolvedTargetName)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO targetRoute = invocation.getArgument(0);
            targetRoute.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(emptyList());
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO targetVersion = invocation.getArgument(0);
            targetVersion.setId(targetVersionId);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        Long copiedRouteId = routeService.copyRoute(sourceRouteId, targetCode, targetName, 10L);

        assertEquals(targetRouteId, copiedRouteId);
        ArgumentCaptor<MesProRouteDO> routeCaptor = ArgumentCaptor.forClass(MesProRouteDO.class);
        verify(routeMapper).insert(routeCaptor.capture());
        assertEquals(resolvedTargetName, routeCaptor.getValue().getName());
    }

    @Test
    void copyRoute_shouldRefreshActiveVersionWithCompleteConfigSnapshotAfterChildConfigsCopied() {
        Long sourceRouteId = 9006L;
        Long targetRouteId = 9007L;
        Long sourceVersionId = 9206L;
        Long targetVersionId = 9207L;
        String targetCode = "ROUTE-COPY-FULL";
        String targetName = "复制完整快照路线";
        MesProRouteDO sourceRoute = MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("ROUTE-SOURCE-FULL")
                .name("源完整路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteProcessDO sourceProcess = MesProRouteProcessDO.builder()
                .id(9306L)
                .routeId(sourceRouteId)
                .processId(9406L)
                .sort(1)
                .build();
        MesProRouteScheduleConfigDO sourceScheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(9506L)
                .routeVersionId(sourceVersionId)
                .routeProcessId(sourceProcess.getId())
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .hourlyCapacity(new BigDecimal("12"))
                .build();
        MesProRouteScheduleConfigDO targetScheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(9507L)
                .routeVersionId(targetVersionId)
                .routeProcessId(9307L)
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .hourlyCapacity(new BigDecimal("12"))
                .build();
        MesProRouteFlowProcessConfigDO targetScheduleUseConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(9607L)
                .routeId(targetRouteId)
                .routeProcessId(9307L)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO targetBatchUseConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(9608L)
                .routeId(targetRouteId)
                .routeProcessId(9307L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();

        when(routeMapper.selectById(sourceRouteId)).thenReturn(sourceRoute);
        when(routeMapper.selectByCode(targetCode)).thenReturn(null);
        when(routeMapper.selectByName(targetName)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO targetRoute = invocation.getArgument(0);
            targetRoute.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        when(routeProductMapper.selectListByRouteId(targetRouteId)).thenReturn(List.of());
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(sourceProcess));
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteProcessDO targetProcess = invocation.getArgument(0);
            targetProcess.setId(9307L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(MesProRouteVersionDO.builder()
                .id(sourceVersionId)
                .routeId(sourceRouteId)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson("""
                        {"routeId":9006,"configSnapshots":{"batchUseConfigs":[{"routeProcessId":9306,"inputMaterialIds":[7701],"outputMaterialIds":[8801]}],"productionProcessConfigSchemaVersion":1,"productionProcessConfigs":[{"routeProcessId":9306,"processId":9406,"overagePercent":0,"lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}],"batchRecordAttachmentOwners":[{"attachmentCode":"INCOMING_INSPECTION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912398],"candidateSourceNames":["张三"]},{"attachmentCode":"STERILIZATION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912398],"candidateSourceNames":["张三"]},{"attachmentCode":"FINISHED_PRODUCT_INSPECTION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912399],"candidateSourceNames":["李四"]},{"attachmentCode":"FINISHED_PRODUCT_INSPECTION_RECORD","candidateSourceType":"USERS","candidateSourceIds":[912399],"candidateSourceNames":["李四"]}]}}
                        """)
                .build());
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO targetVersion = invocation.getArgument(0);
            targetVersion.setId(targetVersionId);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(sourceVersionId))
                .thenReturn(List.of(sourceScheduleConfig));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(targetVersionId))
                .thenReturn(List.of(targetScheduleConfig));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                targetRouteId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(targetScheduleUseConfig));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                targetRouteId, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(targetBatchUseConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                targetRouteId, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of());
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(targetRouteId);
        MesProRouteProcessFlowNodeRespVO node = new MesProRouteProcessFlowNodeRespVO();
        node.setRouteProcessId(9307L);
        node.setProcessId(9406L);
        node.setProcessName("工序");
        node.setSort(1);
        graph.setNodes(List.of(node));
        when(routeProcessFlowService.getGraph(targetRouteId)).thenReturn(graph);

        Long copiedRouteId = routeService.copyRoute(sourceRouteId, targetCode, targetName, 10L);

        assertEquals(targetRouteId, copiedRouteId);
        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        MesProRouteVersionDO update = updateCaptor.getValue();
        assertEquals(targetVersionId, update.getId());
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(update.getRouteSnapshotJson()));
        JSONObject snapshot = JSON.parseObject(update.getRouteSnapshotJson());
        assertEquals(targetRouteId, snapshot.getLong("routeId"));
        assertEquals(targetCode, snapshot.getString("routeCode"));
        assertEquals(targetVersionId, snapshot.getLong("routeVersionId"));
        JSONObject copiedProductionConfig = snapshot.getJSONObject("configSnapshots")
                .getJSONArray("productionProcessConfigs").getJSONObject(0);
        assertEquals(9307L, copiedProductionConfig.getLong("routeProcessId"));
        assertEquals(9406L, copiedProductionConfig.getLong("processId"));
        assertEquals(0, snapshot.getJSONObject("configSnapshots").getJSONArray("products").size());
        assertEquals(1, snapshot.getJSONObject("configSnapshots").getJSONArray("scheduleConfigs").size());
        assertEquals(1, snapshot.getJSONObject("configSnapshots").getJSONArray("scheduleUseConfigs").size());
        assertEquals(1, snapshot.getJSONObject("configSnapshots").getJSONArray("batchUseConfigs").size());
        JSONArray owners = snapshot.getJSONObject("configSnapshots").getJSONArray("batchRecordAttachmentOwners");
        assertEquals(4, owners.size());
        assertEquals("INCOMING_INSPECTION_REPORT", owners.getJSONObject(0).getString("attachmentCode"));
        assertEquals(List.of(912398), owners.getJSONObject(0)
                .getJSONArray("candidateSourceIds").toJavaList(Integer.class));
        ArgumentCaptor<MesProRouteVersionDO> activeCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(platformAdapter).recordActiveRegistered(activeCaptor.capture(), eq(null),
                eq("route active version registered"));
        assertEquals(targetVersionId, activeCaptor.getValue().getId());
        assertEquals(targetRouteId, activeCaptor.getValue().getRouteId());
        assertEquals("V1", activeCaptor.getValue().getVersionNo());
    }

    @Test
    void copyRoute_preservesMaterialDirectionsEmptyArraysAndHistoricalProcessIdentity() {
        MesProRouteVersionDO source = prepareMaterialCopy("""
                [{"routeProcessId":91,"inputMaterialIds":[7702,7701],"outputMaterialIds":[],
                  "id":999,"routeId":10,"formBindings":[{"routeBindingId":888}]},
                 {"routeProcessId":102,"inputMaterialIds":[],"outputMaterialIds":[8801,8802]},
                 {"routeProcessId":103,"inputMaterialIds":[],"outputMaterialIds":[]}]
                """);
        String original = source.getRouteSnapshotJson();
        when(routeProcessService.resolveCurrentRouteProcess(91L, 10L, null))
                .thenReturn(MesProRouteProcessDO.builder().id(101L).routeId(10L).build());

        assertEquals(20L, routeService.copyRoute(10L, "COPY", "副本", 30L));

        var updates = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updates.capture());
        var update = updates.getValue();
        JSONObject snapshot = JSON.parseObject(update.getRouteSnapshotJson());
        JSONArray configs = snapshot.getJSONObject("configSnapshots").getJSONArray("batchUseConfigs");
        assertEquals(3, configs.size());
        for (int i = 0; i < configs.size(); i++) {
            assertEquals(201L + i, configs.getJSONObject(i).getLong("routeProcessId"));
            assertEquals(501L + i, configs.getJSONObject(i).getLong("id"));
            assertEquals(20L, configs.getJSONObject(i).getLong("routeId"));
            assertTrue(configs.getJSONObject(i).getJSONArray("formBindings").isEmpty());
            assertTrue(configs.getJSONObject(i).getJSONArray("batchRecordReports").isEmpty());
        }
        assertEquals(List.of(7702L, 7701L), configs.getJSONObject(0)
                .getJSONArray("inputMaterialIds").toJavaList(Long.class));
        assertEquals(List.of(8801L, 8802L), configs.getJSONObject(1)
                .getJSONArray("outputMaterialIds").toJavaList(Long.class));
        assertEquals("[]", configs.getJSONObject(0).getJSONArray("outputMaterialIds").toJSONString());
        assertEquals("[]", configs.getJSONObject(1).getJSONArray("inputMaterialIds").toJSONString());
        assertEquals("[]", configs.getJSONObject(2).getJSONArray("inputMaterialIds").toJSONString());
        assertEquals("[]", configs.getJSONObject(2).getJSONArray("outputMaterialIds").toJSONString());
        assertEquals(original, source.getRouteSnapshotJson());
        assertEquals(new MesProRouteSnapshotCanonicalizer().sha256(update.getRouteSnapshotJson()),
                update.getRouteSnapshotSha256());
        var created = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(created.capture());
        assertEquals(40L, created.getValue().getSourceRouteVersionId());
        verify(routeVersionMapper, never()).selectOpenCandidateByRouteId(10L);
        verify(routeProductBomMapper, never()).selectList(eq(10L), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"inputMaterialIds\":[]}", "{\"outputMaterialIds\":[]}",
            "{\"inputMaterialIds\":null,\"outputMaterialIds\":[]}",
            "{\"inputMaterialIds\":\"[]\",\"outputMaterialIds\":[]}",
            "{\"inputMaterialIds\":[],\"outputMaterialIds\":{}}",
            "{\"inputMaterialIds\":[null],\"outputMaterialIds\":[]}",
            "{\"inputMaterialIds\":[0],\"outputMaterialIds\":[]}",
            "{\"inputMaterialIds\":[-1],\"outputMaterialIds\":[]}",
            "{\"inputMaterialIds\":[1.5],\"outputMaterialIds\":[]}",
            "{\"inputMaterialIds\":[\"bad\"],\"outputMaterialIds\":[]}",
            "{\"inputMaterialIds\":[],\"outputMaterialIds\":[8801,8801]}"
    })
    void copyRoute_rejectsIncompleteOrInvalidSourceMaterials(String raw) {
        JSONObject invalid = JSON.parseObject(raw);
        invalid.put("routeProcessId", 101L);
        prepareMaterialCopy("[" + invalid.toJSONString() + "]");
        AssertUtils.assertServiceException(() -> routeService.copyRoute(10L, "COPY", "副本", 30L),
                ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, 40L);
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "[]",
            "[{\"routeProcessId\":101,\"inputMaterialIds\":[],\"outputMaterialIds\":[]},"
                    + "{\"routeProcessId\":101,\"inputMaterialIds\":[],\"outputMaterialIds\":[]}]"})
    void copyRoute_rejectsMissingMaterialCoverageOrDuplicateProcess(String raw) {
        prepareMaterialCopy(raw);
        AssertUtils.assertServiceException(() -> routeService.copyRoute(10L, "COPY", "副本", 30L),
                ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, 40L);
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
    }

    @Test
    void copyRoute_rejectsTwoHistoricalEntriesResolvingToSameCurrentProcess() {
        prepareMaterialCopy("""
                [{"routeProcessId":91,"inputMaterialIds":[],"outputMaterialIds":[]},
                 {"routeProcessId":101,"inputMaterialIds":[],"outputMaterialIds":[]}]
                """);
        when(routeProcessService.resolveCurrentRouteProcess(91L, 10L, null))
                .thenReturn(MesProRouteProcessDO.builder().id(101L).routeId(10L).build());
        AssertUtils.assertServiceException(() -> routeService.copyRoute(10L, "COPY", "副本", 30L),
                ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, 40L);
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
    }

    @Test
    void copyRoute_rejectsMaterialProcessOutsideCopiedRoute() {
        prepareMaterialCopy("""
                [{"routeProcessId":91,"inputMaterialIds":[],"outputMaterialIds":[]}]
                """);
        when(routeProcessService.resolveCurrentRouteProcess(91L, 10L, null))
                .thenReturn(MesProRouteProcessDO.builder().id(999L).routeId(10L).build());
        AssertUtils.assertServiceException(() -> routeService.copyRoute(10L, "COPY", "副本", 30L),
                ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
    }

    @Test
    void copyRoute_rejectsLostTargetBatchConfigBeforeRegisteringActiveVersion() {
        prepareMaterialCopy("""
                [{"routeProcessId":101,"inputMaterialIds":[],"outputMaterialIds":[]},
                 {"routeProcessId":102,"inputMaterialIds":[],"outputMaterialIds":[]},
                 {"routeProcessId":103,"inputMaterialIds":[],"outputMaterialIds":[]}]
                """);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                20L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of());
        AssertUtils.assertServiceException(() -> routeService.copyRoute(10L, "COPY", "副本", 30L),
                ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, 50L);
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
    }

    @Test
    void copyRoute_materialFailureRollsBackTheAdvisedCopyTransaction() {
        prepareMaterialCopy("[{\"routeProcessId\":101,\"inputMaterialIds\":[]}]");
        var transactions = org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        var status = new org.springframework.transaction.support.SimpleTransactionStatus();
        when(transactions.getTransaction(any())).thenReturn(status);
        var proxy = new org.springframework.aop.framework.ProxyFactory(routeService);
        proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        MesProRouteService advised = (MesProRouteService) proxy.getProxy();

        AssertUtils.assertServiceException(() -> advised.copyRoute(10L, "COPY", "副本", 30L),
                ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, 40L);

        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
    }

    @Test
    void copyRoute_nonemptyRouteRequiresAFormalSourceVersionForMaterials() {
        prepareMaterialCopy("[]");
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(null);
        AssertUtils.assertServiceException(() -> routeService.copyRoute(10L, "COPY", "副本", 30L),
                ErrorCodeConstants.PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS, 10L);
        verify(routeVersionMapper, never()).insert(any(MesProRouteVersionDO.class));
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
    }

    private MesProRouteVersionDO prepareMaterialCopy(String batchConfigs) {
        // Lenient setup is shared by successful and deliberately early-failing copies.
        lenient().when(routeMapper.selectById(10L)).thenReturn(MesProRouteDO.builder()
                .id(10L).code("SOURCE").name("源路线").build());
        org.mockito.Mockito.doAnswer(call -> {
            ((MesProRouteDO) call.getArgument(0)).setId(20L);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        var processes = List.of(101L, 102L, 103L).stream().map(id -> MesProRouteProcessDO.builder()
                .id(id).routeId(10L).processId(900L).build()).toList();
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(processes);
        org.mockito.Mockito.doAnswer(call -> {
            MesProRouteProcessDO process = call.getArgument(0);
            process.setId(200L + process.getSort());
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        for (int i = 0; i < processes.size(); i++) processes.get(i).setSort(i + 1);
        JSONObject configs = new JSONObject();
        if (!"null".equals(batchConfigs)) configs.put("batchUseConfigs", JSON.parse(batchConfigs));
        JSONObject snapshot = new JSONObject();
        snapshot.put("configSnapshots", configs);
        MesProRouteVersionDO source = MesProRouteVersionDO.builder().id(40L).routeId(10L)
                .active(true).routeSnapshotJson(snapshot.toJSONString()).build();
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(source);
        lenient().doAnswer(call -> {
            ((MesProRouteVersionDO) call.getArgument(0)).setId(50L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));
        var schedules = processes.stream().map(p -> MesProRouteScheduleConfigDO.builder()
                .id(p.getId() + 1000L).routeVersionId(40L).routeProcessId(p.getId()).build()).toList();
        lenient().when(routeScheduleConfigMapper.selectListByRouteVersionId(40L)).thenReturn(schedules);
        for (var process : processes) {
            lenient().when(routeProcessService.resolveCurrentRouteProcess(process.getId(), 10L, null))
                    .thenReturn(process);
        }
        var targetConfigs = processes.stream().map(p -> MesProRouteFlowProcessConfigDO.builder()
                .id(p.getId() + 400L).routeId(20L).routeProcessId(p.getId() + 100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType()).build()).toList();
        lenient().when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                20L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(targetConfigs);
        return source;
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "USERS", "ROLE"})
    void copyRoute_preservesLeaderSourcesAndMapsOnlyTheResponsibleRoute(String sourceType) {
        var assignments = JSON.parseArray("""
                [{"productionLineId":10,"productionLineCode":"SOURCE","productionLineName":"源路线",
                  "candidateSourceType":"%s","candidateSourceIds":[901,902],
                  "candidateSourceNames":["甲","乙"],"sort":1,"remark":"正式分配"}]
                """.formatted(sourceType));
        MesProRouteVersionDO source = prepareLeaderCopy(assignments);
        String before = source.getRouteSnapshotJson();

        assertEquals(20L, routeService.copyRoute(10L, "COPY", "副本", 30L));

        var updates = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updates.capture());
        var copied = updates.getValue();
        JSONObject snapshot = JSON.parseObject(copied.getRouteSnapshotJson());
        JSONArray leaders = snapshot.getJSONObject("configSnapshots").getJSONArray("routeStartProductionLeaders");
        JSONObject expected = JSON.parseObject(assignments.getJSONObject(0).toJSONString());
        expected.put("productionLineId", 20L);
        expected.put("productionLineCode", "COPY");
        expected.put("productionLineName", "副本");
        assertEquals(JSON.parseObject(expected.toJSONString()), leaders.getJSONObject(0));
        assertEquals(before, source.getRouteSnapshotJson());
        assertEquals(10L, assignments.getJSONObject(0).getLong("productionLineId"));
        assertEquals(new MesProRouteSnapshotCanonicalizer().sha256(copied.getRouteSnapshotJson()),
                copied.getRouteSnapshotSha256());
        assertDoesNotThrow(() -> MesProRouteVersionSnapshotValidator
                .requireProductionLeaderAssignments(copied.getRouteSnapshotJson()));
        if ("ROLE".equals(sourceType)) {
            verify(roleApi).validRoleList(List.of(901L, 902L));
            org.mockito.Mockito.verifyNoInteractions(adminUserApi);
        } else {
            verify(adminUserApi).validateUserList(List.of(901L, 902L));
            org.mockito.Mockito.verifyNoInteractions(roleApi);
        }
        // Subsequent candidate snapshot generation reads the target's saved assignment, not the source route.
        copied.setRouteId(20L);
        when(routeVersionMapper.selectById(50L)).thenReturn(copied);
        when(routeMapper.selectById(20L)).thenReturn(MesProRouteDO.builder()
                .id(20L).code("COPY").name("副本").nodeType("FLOW").build());
        JSONObject regenerated = JSON.parseObject(routeService.buildCurrentRouteSnapshotJson(20L, 50L));
        assertEquals(leaders, regenerated.getJSONObject("configSnapshots").getJSONArray("routeStartProductionLeaders"));
        assertDoesNotThrow(() -> MesProRouteVersionSnapshotValidator
                .requireProductionLeaderAssignments(regenerated.toJSONString()));
        leaders.getJSONObject(0).getJSONArray("candidateSourceIds").add(999L);
        assertEquals(before, source.getRouteSnapshotJson());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "EMPTY"})
    void copyRoute_preservesAbsentOrExplicitEmptyLeadersWithoutInventingAssignments(String state) {
        prepareLeaderCopy("EMPTY".equals(state) ? new JSONArray() : null);
        routeService.copyRoute(10L, "COPY", "副本", 30L);
        var updates = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updates.capture());
        var copied = updates.getValue();
        JSONObject configs = JSON.parseObject(copied.getRouteSnapshotJson()).getJSONObject("configSnapshots");
        assertEquals("EMPTY".equals(state), configs.containsKey("routeStartProductionLeaders"));
        if ("EMPTY".equals(state)) assertTrue(configs.getJSONArray("routeStartProductionLeaders").isEmpty());
        org.mockito.Mockito.verifyNoInteractions(adminUserApi, roleApi);
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> MesProRouteVersionSnapshotValidator.requireProductionLeaderAssignments(copied.getRouteSnapshotJson()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "[null]", "[1]",
            "[{\"productionLineId\":99,\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[901]}]",
            "[{\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[901]}]",
            "[{\"productionLineId\":10,\"candidateSourceType\":\"DEPT\",\"candidateSourceIds\":[901]}]",
            "[{\"productionLineId\":10,\"candidateSourceType\":\"USERS\"}]",
            "[{\"productionLineId\":10,\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[]}]",
            "[{\"productionLineId\":10,\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[null]}]",
            "[{\"productionLineId\":10,\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[0]}]",
            "[{\"productionLineId\":10,\"candidateSourceType\":\"ROLE\",\"candidateSourceIds\":[\"invalid\"]}]"})
    void copyRoute_rejectsInvalidLeaderSnapshotsBeforeRegisteringSuccess(String raw) {
        // JSON null is present and invalid; a genuinely absent key has its own successful-copy test.
        MesProRouteVersionDO source = prepareLeaderCopy(JSON.parse(raw));
        if ("null".equals(raw)) {
            JSONObject snapshot = JSON.parseObject(source.getRouteSnapshotJson());
            snapshot.getJSONObject("configSnapshots").put("routeStartProductionLeaders", null);
            source.setRouteSnapshotJson(JSON.toJSONString(snapshot,
                    com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue));
        }
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> routeService.copyRoute(10L, "COPY", "副本", 30L));
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
        org.mockito.Mockito.verifyNoInteractions(adminUserApi, roleApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USERS", "ROLE"})
    void copyRoute_invalidOrDisabledLeaderIdentityRollsBackAndPreservesTheFailure(String sourceType) {
        prepareLeaderCopy(JSON.parseArray("""
                [{"productionLineId":10,"candidateSourceType":"%s","candidateSourceIds":[901]}]
                """.formatted(sourceType)));
        var identityFailure = new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "正式人员或角色已失效");
        if ("ROLE".equals(sourceType)) {
            org.mockito.Mockito.doThrow(identityFailure).when(roleApi).validRoleList(List.of(901L));
        } else {
            org.mockito.Mockito.doThrow(identityFailure).when(adminUserApi).validateUserList(List.of(901L));
        }
        var transactions = org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        var status = new org.springframework.transaction.support.SimpleTransactionStatus();
        when(transactions.getTransaction(any())).thenReturn(status);
        var proxy = new org.springframework.aop.framework.ProxyFactory(routeService);
        proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        MesProRouteService advised = (MesProRouteService) proxy.getProxy();
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> advised.copyRoute(10L, "COPY", "副本", 30L));
        org.junit.jupiter.api.Assertions.assertSame(identityFailure, thrown);
        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        verify(platformAdapter, never()).recordActiveRegistered(any(), any(), any());
    }

    private MesProRouteVersionDO prepareLeaderCopy(Object assignments) {
        MesProRouteVersionDO source = prepareMaterialCopy("""
                [{"routeProcessId":101,"inputMaterialIds":[7701],"outputMaterialIds":[8801]},
                 {"routeProcessId":102,"inputMaterialIds":[],"outputMaterialIds":[]},
                 {"routeProcessId":103,"inputMaterialIds":[],"outputMaterialIds":[]}]
                """);
        JSONObject snapshot = JSON.parseObject(source.getRouteSnapshotJson());
        snapshot.put("routeId", 10L);
        if (assignments != null) snapshot.getJSONObject("configSnapshots").put("routeStartProductionLeaders", assignments);
        source.setRouteSnapshotJson(snapshot.toJSONString());
        return source;
    }

    @Test
    void buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings() {
        Long routeId = 10L;
        Long routeVersionId = 100L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-1").name("路线").build();
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(routeId)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(BigDecimal.ONE)
                .build();
        MesProRouteFlowProcessBatchRecordDO formBinding = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9901L)
                .routeFlowProcessConfigId(901L)
                .routeId(routeId)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .formBindingKey("FB-LIVE")
                .formTemplateId(2002L)
                .formTemplateNameSnapshot("工序设置当前生产记录")
                .lastPublishedTemplateVersionId(3002L)
                .lastPublishedTemplateVersionNo("V2")
                .instanceScope("PROCESS")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .candidateSourceType("USERS")
                .candidateSourceIds("9002")
                .candidateSourceNames("[\"李四\"]")
                .reportSort(1)
                .build();
        MesProRouteFlowProcessBatchRecordDO legacyReport = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9902L)
                .routeFlowProcessConfigId(901L)
                .routeId(routeId)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .batchRecordReportId("REPORT-LIVE")
                .formSlotType("MAIN")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .reportSort(2)
                .build();
        MesProRouteFlowProcessBatchRecordDO migratedTemplateReport = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9903L)
                .routeFlowProcessConfigId(901L)
                .routeId(routeId)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .batchRecordReportId("FORMTPL:32")
                .batchRecordDefinitionId(7001L)
                .batchRecordVersionId(7002L)
                .formSlotType("PROCESS_INSPECTION")
                .formBindingKey("FB-MIGRATED")
                .formTemplateId(2003L)
                .formTemplateNameSnapshot("迁移旧模板")
                .lastPublishedTemplateVersionId(32L)
                .lastPublishedTemplateVersionNo("V3.0")
                .instanceScope("BATCH_SHARED")
                .sharedFormKey("shared-process-inspection")
                .fillableScopeJson("{\"tables\":[1]}")
                .recordCategory("INTERNAL_RECORD")
                .validationProfile("INTERNAL_TRACE")
                .ownerRoleKey("QUALITY")
                .archiveVisibility("FINAL_DHR")
                .reportSort(3)
                .build();
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(routeId);
        graph.setNodes(emptyList());
        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeVersionMapper.selectById(routeVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeId(routeId)
                .routeSnapshotJson("""
                        {
                          "configSnapshots": {
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "inputMaterialIds": [7701, 7702],
                                "outputMaterialIds": [8801, 8802]
                              }
                            ]
                          }
                        }
                        """)
                .build());
        when(routeProcessFlowService.getGraph(routeId)).thenReturn(graph);
        when(routeProductMapper.selectListByRouteId(routeId)).thenReturn(emptyList());
        when(routeProductBomMapper.selectList(routeId, null, null)).thenReturn(emptyList());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId)).thenReturn(emptyList());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(processConfig));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(emptyList());
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(formBinding, legacyReport, migratedTemplateReport));

        JSONObject snapshot = JSON.parseObject(routeService.buildCurrentRouteSnapshotJson(routeId, routeVersionId));

        JSONObject batchUseConfig = snapshot.getJSONObject("configSnapshots")
                .getJSONArray("batchUseConfigs").getJSONObject(0);
        assertEquals(List.of(7701L, 7702L), batchUseConfig.getJSONArray("inputMaterialIds")
                .toJavaList(Long.class));
        assertEquals(List.of(8801L, 8802L), batchUseConfig.getJSONArray("outputMaterialIds")
                .toJavaList(Long.class));
        assertFalse(batchUseConfig.containsKey("frontlineReportMaterialIds"));
        JSONArray formBindings = batchUseConfig.getJSONArray("formBindings");
        assertEquals(1, formBindings.size());
        assertEquals("FB-LIVE", formBindings.getJSONObject(0).getString("formBindingKey"));
        assertEquals(2002L, formBindings.getJSONObject(0).getLong("formTemplateId"));
        assertEquals("工序设置当前生产记录", formBindings.getJSONObject(0).getString("formTemplateName"));
        assertEquals("USERS", formBindings.getJSONObject(0).getString("candidateSourceType"));
        assertEquals(List.of(9002), formBindings.getJSONObject(0)
                .getJSONArray("candidateSourceIds").toJavaList(Integer.class));
        assertEquals(List.of("李四"), formBindings.getJSONObject(0)
                .getJSONArray("candidateSourceNames").toJavaList(String.class));
        JSONArray batchRecordReports = batchUseConfig.getJSONArray("batchRecordReports");
        assertEquals(2, batchRecordReports.size());
        assertEquals("REPORT-LIVE", batchRecordReports.getJSONObject(0).getString("batchRecordReportId"));
        assertEquals("MAIN", batchRecordReports.getJSONObject(0).getString("formSlotType"));
        assertEquals("FORMTPL:32", batchRecordReports.getJSONObject(1).getString("batchRecordReportId"));
        assertEquals(7001L, batchRecordReports.getJSONObject(1).getLong("batchRecordDefinitionId"));
        assertEquals(7002L, batchRecordReports.getJSONObject(1).getLong("batchRecordVersionId"));
        assertEquals("PROCESS_INSPECTION", batchRecordReports.getJSONObject(1).getString("formSlotType"));
    }

    @Test
    void generatedAndSavedSnapshot_shouldPreserveSopAndRejectMissingOrMalformedBindings() {
        Long routeId = 701L;
        Long versionId = 702L;
        var route = MesProRouteDO.builder().id(routeId).code("SOP-REGRESSION").name("SOP route").build();
        var graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(routeId);
        graph.setEdges(emptyList());
        var node = new MesProRouteProcessFlowNodeRespVO();
        node.setRouteProcessId(11L);
        node.setProcessId(21L);
        node.setProcessName("粗洗工序");
        node.setSort(1);
        node.setX(120);
        node.setKeyFlag(true);
        graph.setNodes(List.of(node));
        var version = MesProRouteVersionDO.builder().id(versionId).routeId(routeId)
                .routeSnapshotJson("""
                        {"configSnapshots":{"productionProcessConfigs":[{"routeProcessId":11,"processId":21,
                        "overagePercent":0,"lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}],
                        "routeStartProductionLeaders":[{"productionLineId":701,
                        "candidateSourceType":"USER","candidateSourceIds":[607]}]}}
                        """).build();
        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeVersionMapper.selectById(versionId)).thenReturn(version);
        when(routeProcessFlowService.getGraph(routeId)).thenReturn(graph);
        when(routeProductMapper.selectListByRouteId(routeId)).thenReturn(emptyList());
        when(routeProductBomMapper.selectList(routeId, null, null)).thenReturn(emptyList());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(versionId)).thenReturn(emptyList());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId,
                MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(emptyList());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId,
                MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(emptyList());

        var canonicalizer = new MesProRouteSnapshotCanonicalizer();
        for (String sop : new String[]{null, "/files/粗洗 SOP.pdf?version=31"}) {
            node.setSopFileUrl(sop);
            String generated = routeService.buildCurrentRouteSnapshotJson(routeId, versionId);
            assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(generated));
            assertDoesNotThrow(() -> MesProRouteVersionSnapshotValidator.requireProductionLeaderAssignments(generated));
            assertTrue(canonicalizer.validate(routeId, generated).ready());
            JSONObject snapshot = JSON.parseObject(generated);
            JSONObject frozen = snapshot.getJSONObject("configSnapshots").getJSONObject("flowGraph")
                    .getJSONArray("nodes").getJSONObject(0);
            assertTrue(frozen.containsKey("sopFileUrl"));
            assertEquals(sop, frozen.get("sopFileUrl"));
            // All other node fields retain the previous conversion's values and null omission.
            JSONObject expected = (JSONObject) JSON.toJSON(node);
            expected.remove("sopFileUrl");
            JSONObject otherFields = new JSONObject(frozen);
            otherFields.remove("sopFileUrl");
            assertEquals(JSON.parseObject(expected.toJSONString()), otherFields);

            ReflectionTestUtils.invokeMethod(routeService, "refreshRouteVersionSnapshot", route, versionId);
            frozen.remove("sopFileUrl");
            assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(
                    MesProRouteVersionSnapshotIdentityWriter.toSnapshotJson(snapshot)));
            for (Object invalid : List.of(123, true, new JSONArray(), new JSONObject())) {
                frozen.put("sopFileUrl", invalid);
                assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(
                        MesProRouteVersionSnapshotIdentityWriter.toSnapshotJson(snapshot)));
            }
        }
        var updates = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper, org.mockito.Mockito.times(2)).updateById(updates.capture());
        for (var saved : updates.getAllValues()) {
            assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(saved.getRouteSnapshotJson()));
            assertEquals(canonicalizer.sha256(saved.getRouteSnapshotJson()), saved.getRouteSnapshotSha256());
            assertEquals(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION, saved.getRouteSnapshotFormatVersion());
            assertGeneratedSnapshotPublishes(saved, routeId);
        }
    }

    private void assertGeneratedSnapshotPublishes(MesProRouteVersionDO saved, Long routeId) {
        var lifecycle = new MesProRouteVersionLifecycleServiceImpl();
        var mapper = org.mockito.Mockito.mock(MesProRouteVersionMapper.class);
        var projection = org.mockito.Mockito.mock(MesProRouteVersionPublishProjectionServiceImpl.class);
        var craft = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftService.class);
        ReflectionTestUtils.setField(lifecycle, "routeVersionMapper", mapper);
        ReflectionTestUtils.setField(lifecycle, "publishProjectionService", projection);
        ReflectionTestUtils.setField(lifecycle, "craftService", craft);
        ReflectionTestUtils.setField(lifecycle, "routeSnapshotCanonicalizer", new MesProRouteSnapshotCanonicalizer());
        ReflectionTestUtils.setField(lifecycle, "deviceIdentitySnapshots", deviceIdentitySnapshots);
        ReflectionTestUtils.setField(lifecycle, "platformAdapter", platformAdapter);
        ReflectionTestUtils.setField(lifecycle, "versionOperateLogService", versionOperateLogService);
        ReflectionTestUtils.setField(lifecycle, "routeTrees", routeTrees);
        when(routeTrees.getObject()).thenReturn(routeTreeService);
        var candidate = MesProRouteVersionDO.builder().id(saved.getId()).routeId(routeId).active(false)
                .sourceRouteVersionId(700L).lifecycleStatus("READY_TO_PUBLISH")
                .routeSnapshotJson(saved.getRouteSnapshotJson()).build();
        when(mapper.selectById(candidate.getId())).thenReturn(candidate);
        when(mapper.selectActiveByRouteId(routeId)).thenReturn(
                MesProRouteVersionDO.builder().id(700L).routeId(routeId).active(true).build());
        when(mapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        // Projection and persistence are mocked; all lifecycle snapshot gates execute normally.
        when(projection.projectCandidate(candidate)).thenReturn(
                new MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult(
                        saved.getRouteSnapshotJson(), Set.of(11L)));
        String before = new MesProRouteSnapshotCanonicalizer().canonicalize(saved.getRouteSnapshotJson());
        assertEquals("ACTIVE", lifecycle.publishCandidate(candidate.getId(), 607L).getLifecycleStatus());
        assertEquals(before, candidate.getRouteSnapshotJson());
        var writes = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(mapper, org.mockito.Mockito.times(3)).updateById(writes.capture());
        assertEquals(before, writes.getAllValues().get(0).getRouteSnapshotJson());
    }

    @Test
    void buildCurrentRouteSnapshotJson_shouldPreserveExistingStartConfigurations() {
        Long routeId = 922119L;
        Long routeVersionId = 34126020001L;
        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .code("RT000028")
                .name("球囊扩张压力泵")
                .build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeId(routeId)
                .routeSnapshotJson("""
                        {"routeId":922119,"configSnapshots":{"lossCoefficient":1.25,"routeStartProductionLeaders":[{"productionLineId":71,"candidateSourceType":"USERS","candidateSourceIds":[912397],"candidateSourceNames":["王五"],"sort":1}],"batchRecordAttachmentOwners":[{"attachmentCode":"INCOMING_INSPECTION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912398],"candidateSourceNames":["张三"]},{"attachmentCode":"STERILIZATION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912398],"candidateSourceNames":["张三"]},{"attachmentCode":"FINISHED_PRODUCT_INSPECTION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912399],"candidateSourceNames":["李四"]},{"attachmentCode":"FINISHED_PRODUCT_INSPECTION_RECORD","candidateSourceType":"USERS","candidateSourceIds":[912399],"candidateSourceNames":["李四"]}]}}
                        """)
                .build();
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(routeId);
        MesProRouteProcessFlowNodeRespVO withSop = new MesProRouteProcessFlowNodeRespVO();
        withSop.setRouteProcessId(21L);
        withSop.setProcessId(201L);
        withSop.setSopFileUrl("/files/frozen-sop.pdf");
        MesProRouteProcessFlowNodeRespVO unboundSop = new MesProRouteProcessFlowNodeRespVO();
        unboundSop.setRouteProcessId(22L);
        unboundSop.setProcessId(202L);
        graph.setNodes(List.of(withSop, unboundSop));
        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeVersionMapper.selectById(routeVersionId)).thenReturn(routeVersion);
        JSONObject savedSnapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        JSONObject sharedSources = JSON.parseObject("{\"DEVICE:7\":{\"id\":8,\"code\":\"B\",\"name\":\"已确认设备\"}}");
        savedSnapshot.getJSONObject("configSnapshots").put("sharedSourceSnapshots", sharedSources);
        routeVersion.setRouteSnapshotJson(savedSnapshot.toJSONString());
        when(routeProcessFlowService.getGraph(routeId)).thenReturn(graph);
        when(routeProductMapper.selectListByRouteId(routeId)).thenReturn(emptyList());
        when(routeProductBomMapper.selectList(routeId, null, null)).thenReturn(emptyList());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId)).thenReturn(emptyList());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(emptyList());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(emptyList());

        JSONObject snapshot = JSON.parseObject(routeService.buildCurrentRouteSnapshotJson(routeId, routeVersionId));

        JSONArray frozenNodes = snapshot.getJSONObject("configSnapshots").getJSONObject("flowGraph").getJSONArray("nodes");
        assertEquals("/files/frozen-sop.pdf", frozenNodes.getJSONObject(0).getString("sopFileUrl"));
        assertTrue(frozenNodes.getJSONObject(1).containsKey("sopFileUrl"));
        org.junit.jupiter.api.Assertions.assertNull(frozenNodes.getJSONObject(1).get("sopFileUrl"));
        assertEquals(sharedSources, snapshot.getJSONObject("configSnapshots").getJSONObject("sharedSourceSnapshots"));
        assertEquals(new BigDecimal("1.25"), snapshot.getJSONObject("configSnapshots").getBigDecimal("lossCoefficient"));
        JSONArray owners = snapshot.getJSONObject("configSnapshots").getJSONArray("batchRecordAttachmentOwners");
        assertEquals(4, owners.size());
        assertEquals("INCOMING_INSPECTION_REPORT", owners.getJSONObject(0).getString("attachmentCode"));
        assertEquals(List.of(912398), owners.getJSONObject(0)
                .getJSONArray("candidateSourceIds").toJavaList(Integer.class));
        JSONArray productionLeaders = snapshot.getJSONObject("configSnapshots")
                .getJSONArray("routeStartProductionLeaders");
        assertEquals(1, productionLeaders.size());
        assertEquals(71L, productionLeaders.getJSONObject(0).getLong("productionLineId"));
        assertEquals(List.of(912397), productionLeaders.getJSONObject(0)
                .getJSONArray("candidateSourceIds").toJavaList(Integer.class));
    }

    @Test
    void copyRoute_shouldGrantRouteEditPermissionToCopier() {
        Long sourceRouteId = 9016L;
        Long targetRouteId = 9017L;
        Long targetVersionId = 9217L;
        Long copierUserId = 501L;
        String targetCode = "ROUTE-COPY-PERM";
        String targetName = "复制权限路线";

        when(routeMapper.selectById(sourceRouteId)).thenReturn(MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("ROUTE-SOURCE-PERM")
                .name("源权限路线")
                .build());
        when(routeMapper.selectByCode(targetCode)).thenReturn(null);
        when(routeMapper.selectByName(targetName)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO targetRoute = invocation.getArgument(0);
            targetRoute.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        when(routeProductMapper.selectListByRouteId(anyLong())).thenReturn(emptyList());
        when(routeProductBomMapper.selectList(anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(emptyList());
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(emptyList());
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO targetVersion = invocation.getArgument(0);
            targetVersion.setId(targetVersionId);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(copierUserId);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");

            routeService.copyRoute(sourceRouteId, targetCode, targetName, 10L);
        }

        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> scopeCaptor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService).saveRules(scopeCaptor.capture());
        MesProEdhrPermissionScopeSaveCommand command = scopeCaptor.getValue();
        assertEquals("route-" + targetRouteId, command.getScopeName());
        assertEquals("ROUTE", command.getObjectType());
        assertEquals(String.valueOf(targetRouteId), command.getObjectId());
        assertEquals(copierUserId, command.getActorUserId());
        assertEquals("aoteman", command.getActorUsername());
        assertEquals(List.of("VIEW", "ROUTE_EDIT", "PERMISSION_ADMIN"),
                command.getRules().stream().map(MesProEdhrPermissionRuleCommand::getAbility).toList());
        command.getRules().forEach(rule -> {
            assertEquals("USER", rule.getSubjectType());
            assertEquals(copierUserId, rule.getSubjectId());
            assertEquals("ALLOW", rule.getDecision());
            assertEquals("ENABLED", rule.getStatus());
        });
    }

    @Test
    void writeMethodsThatCreateRouteVersions_shouldBeTransactionalWithPlatformRefs() throws NoSuchMethodException {
        assertTransactional("createRoute", MesProRouteSaveReqVO.class);
        assertTransactional("updateRoute", MesProRouteSaveReqVO.class);
        assertTransactional("copyRoute", Long.class, String.class, String.class, Long.class);
        assertTransactional("maintainRouteVersionAfterProcessChange", Long.class);
    }

    @Test
    void updateRouteStatus_shouldEnableRouteEvenWithoutRouteProductBomConsumption() {
        Long routeId = 1001L;
        Long productId = 2001L;

        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        MesProRouteProcessDO process = MesProRouteProcessDO.builder()
                .id(3001L)
                .routeId(routeId)
                .keyFlag(true)
                .build();
        MesProRouteProductDO product = MesProRouteProductDO.builder()
                .id(4001L)
                .routeId(routeId)
                .itemId(productId)
                .build();
        MesMdItemDO item = MesMdItemDO.builder()
                .id(productId)
                .name("冠状动脉棘突球囊扩张导管")
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.getRouteProcessListByRouteId(routeId)).thenReturn(List.of(process));
        lenient().when(routeProductService.getRouteProductListByRouteId(routeId)).thenReturn(List.of(product));
        lenient().when(routeProductBomService.getRouteProductBomList(routeId, null, productId)).thenReturn(emptyList());
        lenient().when(itemService.validateItemExists(productId)).thenReturn(item);

        assertDoesNotThrow(() -> routeService.updateRouteStatus(routeId, CommonStatusEnum.ENABLE.getStatus()));

        verify(routeProductService, never()).getRouteProductListByRouteId(eq(routeId));
        verify(routeProductBomService, never()).getRouteProductBomList(any(), any(), any());
        verify(itemService, never()).validateItemExists(any());
        verify(routeMapper).updateById(any(MesProRouteDO.class));
    }

    @Test
    void updateRouteStatus_shouldEnableRouteWithoutKeyProcess() {
        Long routeId = 1002L;

        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        MesProRouteProcessDO process = MesProRouteProcessDO.builder()
                .id(3002L)
                .routeId(routeId)
                .keyFlag(false)
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.getRouteProcessListByRouteId(routeId)).thenReturn(List.of(process));

        assertDoesNotThrow(() -> routeService.updateRouteStatus(routeId, CommonStatusEnum.ENABLE.getStatus()));

        verify(routeMapper).updateById(any(MesProRouteDO.class));
        verify(routeProductService, never()).getRouteProductListByRouteId(eq(routeId));
        verify(routeProductBomService, never()).getRouteProductBomList(any(), any(), any());
    }

    @Test
    void updateRouteStatus_shouldIgnoreNullKeyFlagWhenAnotherProcessIsKey() {
        Long routeId = 1003L;

        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        MesProRouteProcessDO normalProcess = MesProRouteProcessDO.builder()
                .id(3003L)
                .routeId(routeId)
                .keyFlag(null)
                .build();
        MesProRouteProcessDO keyProcess = MesProRouteProcessDO.builder()
                .id(3004L)
                .routeId(routeId)
                .keyFlag(true)
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.getRouteProcessListByRouteId(routeId)).thenReturn(List.of(normalProcess, keyProcess));

        assertDoesNotThrow(() -> routeService.updateRouteStatus(routeId, CommonStatusEnum.ENABLE.getStatus()));

        verify(routeMapper).updateById(any(MesProRouteDO.class));
    }

    @Test
    void ensureDefaultScheduleArtifacts_shouldCreateDefaultUseConfigAndScheduleConfig() {
        Long routeId = 2001L;
        Long routeProcessId = 3001L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-AUTO").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId).routeId(routeId).processId(4001L).sort(1).build();
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO routeVersion =
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(5001L).routeId(routeId).active(Boolean.TRUE).versionNo("V1").build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null)).thenReturn(routeProcess);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(null);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(routeProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(null);
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(routeVersion.getId(), routeProcessId))
                .thenReturn(null);
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaultPolicySettings());
        when(scheduleCalendarRuleMapper.selectByTenantId(1L)).thenReturn(MesProScheduleCalendarRuleDO.builder()
                .id(7001L)
                .build());
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteFlowConfigDO data = invocation.getArgument(0);
            data.setId(6001L);
            return 1;
        }).when(routeFlowConfigMapper).insert(any(MesProRouteFlowConfigDO.class));

        TenantContextHolder.setTenantId(1L);
        try {
            routeService.ensureDefaultScheduleArtifacts(routeId, routeProcessId);
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<MesProRouteFlowConfigDO> useConfigCaptor = ArgumentCaptor.forClass(MesProRouteFlowConfigDO.class);
        verify(routeFlowConfigMapper).insert(useConfigCaptor.capture());
        MesProRouteFlowConfigDO useConfig = useConfigCaptor.getValue();
        assertEquals(routeId, useConfig.getRouteId());
        assertEquals(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType(), useConfig.getUseType());
        assertEquals("AUTO-SCHEDULE", useConfig.getConfigVersion());
        assertEquals("[AUTO_DEFAULT_SCHEDULE_USE]", useConfig.getRemark());

        ArgumentCaptor<MesProRouteFlowProcessConfigDO> processConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessConfigDO.class);
        verify(routeFlowProcessConfigMapper).insert(processConfigCaptor.capture());
        MesProRouteFlowProcessConfigDO processConfig = processConfigCaptor.getValue();
        assertEquals(6001L, processConfig.getRouteFlowConfigId());
        assertEquals(routeId, processConfig.getRouteId());
        assertEquals(routeProcessId, processConfig.getRouteProcessId());
        assertEquals(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType(), processConfig.getUseType());
        assertEquals(Boolean.TRUE, processConfig.getEnabled());
        assertEquals("SEQUENTIAL", processConfig.getExecutionMode());
        assertEquals("[AUTO_DEFAULT_SCHEDULE_USE]", processConfig.getRemark());

        ArgumentCaptor<MesProRouteScheduleConfigDO> scheduleConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).insert(scheduleConfigCaptor.capture());
        MesProRouteScheduleConfigDO scheduleConfig = scheduleConfigCaptor.getValue();
        assertEquals(routeVersion.getId(), scheduleConfig.getRouteVersionId());
        assertEquals(routeProcessId, scheduleConfig.getRouteProcessId());
        assertEquals(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode(), scheduleConfig.getCapacityMode());
        assertNull(scheduleConfig.getHourlyCapacity());
        assertNull(scheduleConfig.getInfiniteDurationQuantityFactor());
        assertNull(scheduleConfig.getInfiniteDurationBaseMinutes());
        assertEquals(Boolean.TRUE, scheduleConfig.getNightShiftEnabled());
        assertEquals(7001L, scheduleConfig.getCalendarRuleId());
        assertEquals(MesProRouteServiceImpl.DEFAULT_SCHEDULE_CONFIG_VERSION, scheduleConfig.getConfigVersion());
        assertEquals("[AUTO_DEFAULT_SCHEDULE_CONFIG]", scheduleConfig.getRemark());
    }

    @Test
    void ensureDefaultScheduleArtifacts_shouldRebindProcessConfigOwnedByDifferentFlow() {
        Long routeId = 2002L;
        Long routeProcessId = 3002L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-REBIND").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId).routeId(routeId).processId(4002L).sort(1).build();
        MesProRouteFlowConfigDO currentFlow = MesProRouteFlowConfigDO.builder()
                .id(6002L)
                .routeId(routeId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO staleProcessConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(6102L)
                .routeFlowConfigId(9999L)
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO routeVersion =
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(5002L).routeId(routeId).active(Boolean.TRUE).versionNo("V1").build();
        MesProRouteScheduleConfigDO scheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(7002L).routeVersionId(routeVersion.getId()).routeProcessId(routeProcessId).build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null)).thenReturn(routeProcess);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(currentFlow);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                routeProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(staleProcessConfig);
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(
                routeVersion.getId(), routeProcessId)).thenReturn(scheduleConfig);
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaultPolicySettings());

        routeService.ensureDefaultScheduleArtifacts(routeId, routeProcessId);

        verify(routeFlowProcessConfigMapper).updateById(
                org.mockito.ArgumentMatchers.<MesProRouteFlowProcessConfigDO>argThat(config ->
                Long.valueOf(6102L).equals(config.getId())
                        && Long.valueOf(6002L).equals(config.getRouteFlowConfigId())
                        && routeId.equals(config.getRouteId())
                        && routeProcessId.equals(config.getRouteProcessId())
                        && MesProRouteFlowConfigTypeEnum.SCHEDULE.getType().equals(config.getUseType())));
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void ensureDefaultScheduleArtifacts_shouldNormalizeHistoricalProcessConfigs() {
        Long routeId = 2003L;
        Long routeProcessId = 3003L;
        Long historicalRouteProcessId = 2999L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-HISTORICAL").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId).routeId(routeId).processId(4003L).sort(1).build();
        MesProRouteFlowConfigDO currentFlow = MesProRouteFlowConfigDO.builder()
                .id(6003L)
                .routeId(routeId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO historicalProcessConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(6103L)
                .routeFlowConfigId(currentFlow.getId())
                .routeId(routeId)
                .routeProcessId(historicalRouteProcessId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO routeVersion =
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(5003L).routeId(routeId).active(Boolean.TRUE).versionNo("V1").build();
        MesProRouteScheduleConfigDO historicalScheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(7003L)
                .routeVersionId(routeVersion.getId())
                .routeProcessId(historicalRouteProcessId)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacity(new BigDecimal("88"))
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null)).thenReturn(routeProcess);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(currentFlow);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                routeProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(null);
        lenient().when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(historicalProcessConfig));
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(
                routeVersion.getId(), routeProcessId)).thenReturn(null);
        lenient().when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersion.getId()))
                .thenReturn(List.of(historicalScheduleConfig));
        lenient().when(routeProcessService.resolveCurrentRouteProcess(
                historicalRouteProcessId, routeId, null)).thenReturn(routeProcess);
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaultPolicySettings());
        lenient().when(scheduleCalendarRuleMapper.selectByTenantId(1L))
                .thenReturn(MesProScheduleCalendarRuleDO.builder().id(7004L).build());

        TenantContextHolder.setTenantId(1L);
        try {
            routeService.ensureDefaultScheduleArtifacts(routeId, routeProcessId);
        } finally {
            TenantContextHolder.clear();
        }

        verify(routeFlowProcessConfigMapper).updateById(
                org.mockito.ArgumentMatchers.<MesProRouteFlowProcessConfigDO>argThat(config ->
                        Long.valueOf(6103L).equals(config.getId())
                                && routeProcessId.equals(config.getRouteProcessId())));
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeScheduleConfigMapper).updateById(
                org.mockito.ArgumentMatchers.<MesProRouteScheduleConfigDO>argThat(config ->
                        Long.valueOf(7003L).equals(config.getId())
                                && routeProcessId.equals(config.getRouteProcessId())));
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void ensureDefaultScheduleArtifacts_shouldNormalizeHistoricalRequestedRouteProcess() {
        Long routeId = 2004L;
        Long historicalRouteProcessId = 2998L;
        Long currentRouteProcessId = 3004L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-HISTORICAL-REQ").build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(currentRouteProcessId).routeId(routeId).processId(4004L).sort(1).build();
        MesProRouteFlowConfigDO currentFlow = MesProRouteFlowConfigDO.builder()
                .id(6004L)
                .routeId(routeId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO routeVersion =
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(5004L).routeId(routeId).active(Boolean.TRUE).versionNo("V1").build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.resolveCurrentRouteProcess(historicalRouteProcessId, routeId, null))
                .thenReturn(currentRouteProcess);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(currentFlow);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                currentRouteProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(null);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(List.of());
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(
                routeVersion.getId(), currentRouteProcessId)).thenReturn(null);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersion.getId())).thenReturn(List.of());
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaultPolicySettings());
        when(scheduleCalendarRuleMapper.selectByTenantId(1L))
                .thenReturn(MesProScheduleCalendarRuleDO.builder().id(7005L).build());

        TenantContextHolder.setTenantId(1L);
        try {
            routeService.ensureDefaultScheduleArtifacts(routeId, historicalRouteProcessId);
        } finally {
            TenantContextHolder.clear();
        }

        verify(routeFlowProcessConfigMapper).insert(
                org.mockito.ArgumentMatchers.<MesProRouteFlowProcessConfigDO>argThat(config ->
                        currentRouteProcessId.equals(config.getRouteProcessId())));
        verify(routeScheduleConfigMapper).insert(
                org.mockito.ArgumentMatchers.<MesProRouteScheduleConfigDO>argThat(config ->
                        currentRouteProcessId.equals(config.getRouteProcessId())));
    }

    @Test
    void resolveCopiedRouteProcessId_shouldMapHistoricalSourceIdentity() {
        Long routeId = 2100L;
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(3100L)
                .routeId(routeId)
                .processId(4100L)
                .build();
        when(routeProcessService.resolveCurrentRouteProcess(3099L, routeId, null))
                .thenReturn(currentRouteProcess);

        Long result = ReflectionTestUtils.invokeMethod(
                routeService, "resolveCopiedRouteProcessId", routeId, 3099L, Map.of(3100L, 3200L));

        assertEquals(3200L, result);
    }

    private MesProSchedulerWorkbenchPolicySettingsRespVO defaultPolicySettings() {
        MesProSchedulerWorkbenchPolicySettingsRespVO respVO = new MesProSchedulerWorkbenchPolicySettingsRespVO();
        respVO.setErpWorkOrderSyncTime("02:00");
        respVO.setNightlyReplanTime("03:00");
        respVO.setPriorityRule("PROMISE_DATE");
        respVO.setProtectReportedTasks(true);
        respVO.setProtectCompletedTasks(true);
        respVO.setProtectLockedTasks(true);
        respVO.setDefaultScheduleUseEnabled(true);
        respVO.setDefaultScheduleCapacityMode("FINITE_HOURLY");
        respVO.setDefaultFiniteHourlyCapacity(new BigDecimal("88"));
        respVO.setDefaultNightShiftEnabled(true);
        respVO.setDefaultWorkerQuantity(5);
        respVO.setDefaultWorkerSingleHourlyCapacity(new BigDecimal("30"));
        return respVO;
    }

    private String validCompleteRouteSnapshotJson(Long routeId, String routeCode, String routeName) {
        return """
                {
                  "routeId": %d,
                  "routeCode": "%s",
                  "routeName": "%s",
                  "configSnapshots": {
                    "flowGraph": {
                      "routeId": %d,
                      "nodes": [
                        { "routeProcessId": 3001, "processId": 4001, "processName": "工序", "sort": 1 }
                      ],
                      "edges": [],
                      "boundaryEdges": []
                    },
                    "products": [
                      { "id": 5001, "routeId": %d, "itemId": 6001, "quantity": 1 }
                    ],
                    "scheduleConfigs": [
                      { "id": 7001, "routeVersionId": 8001, "routeProcessId": 3001 }
                    ],
                    "batchUseConfigs": [
                      { "id": 9001, "routeId": %d, "routeProcessId": 3001, "useType": "BATCH" }
                    ],
                    "scheduleUseConfigs": [
                      { "id": 9002, "routeId": %d, "routeProcessId": 3001, "useType": "SCHEDULE" }
                    ],
                    "productionProcessConfigSchemaVersion": 1,
                    "productionProcessConfigs": [
                      { "routeProcessId": 3001, "processId": 4001, "overagePercent": 0,
                        "lossReasons": [], "deviceSelectionGroups": [], "parameterRules": [] }
                    ]
                  }
                }
                """.formatted(routeId, routeCode, routeName, routeId, routeId, routeId, routeId);
    }

    private void assertTransactional(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = MesProRouteServiceImpl.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional != null, methodName + " should be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }
}
