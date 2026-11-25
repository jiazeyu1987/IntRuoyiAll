package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteStartProductionLeaderItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteStartProductionLeaderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionGateService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_START_PRODUCTION_LEADER_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProRouteProductionLeaderNodeTypeTest {
    @InjectMocks private MesProRouteFlowConfigServiceImpl service;
    @Mock private MesProRouteMapper routeMapper;
    @Mock private MesProRouteVersionMapper routeVersionMapper;
    @Mock private MesProRouteProcessMapper routeProcessMapper;
    @Mock private MesProProcessMapper processMapper;
    @Mock private MesProRouteCandidateConfigService routeCandidateConfigService;
    @Mock private MesProEdhrPermissionGateService permissionGateService;
    @Mock private AdminUserApi adminUserApi;

    @Test
    void groupReadsHaveNoExecutionScopeAndDoNotParseTemplateSnapshots() {
        givenRoute("GROUP");
        when(routeVersionMapper.selectById(99L)).thenReturn(MesProRouteVersionDO.builder()
                .id(99L).routeId(10L).active(true).lifecycleStatus("ACTIVE")
                .routeSnapshotJson("damaged-template-snapshot").build());

        assertTrue(service.getRouteStartProductionLeaders(10L, null).isEmpty());
        assertTrue(service.getRouteStartProductionLeaders(10L, 99L).isEmpty());
        assertTrue(service.getRouteStartProductionLeaderProductionLines(10L, 99L).isEmpty());

        verify(routeVersionMapper, times(2)).selectById(99L);
        verifyNoInteractions(routeProcessMapper, processMapper, routeCandidateConfigService);
    }

    @Test
    void groupReadsStillRejectExplicitVersionBelongingToAnotherRoute() {
        givenRoute("GROUP");
        when(routeVersionMapper.selectById(99L)).thenReturn(MesProRouteVersionDO.builder()
                .id(99L).routeId(20L).active(true).lifecycleStatus("ACTIVE")
                .routeSnapshotJson("damaged-template-snapshot").build());

        assertThrows(ServiceException.class, () -> service.getRouteStartProductionLeaders(10L, 99L));
        assertThrows(ServiceException.class,
                () -> service.getRouteStartProductionLeaderProductionLines(10L, 99L));
        verifyNoInteractions(routeProcessMapper, processMapper, routeCandidateConfigService);
    }

    @Test
    void groupReadsStillRejectMissingExplicitVersion() {
        givenRoute("GROUP");

        assertThrows(ServiceException.class, () -> service.getRouteStartProductionLeaders(10L, 99L));
        assertThrows(ServiceException.class,
                () -> service.getRouteStartProductionLeaderProductionLines(10L, 99L));
        verifyNoInteractions(routeProcessMapper, processMapper, routeCandidateConfigService);
    }

    @Test
    void groupCannotSaveProductionLeaderEvenWithAnOtherwiseValidRequest() {
        givenRoute("GROUP");

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.saveRouteStartProductionLeaders(request(10L)));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_START_PRODUCTION_LEADER_INVALID.getCode(), error.getCode());
        assertTrue(error.getMessage().contains("目录模板不参与生产组长执行授权"));
        verifyNoInteractions(permissionGateService, routeVersionMapper, routeCandidateConfigService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN"})
    void unknownNodeTypeNeverGrantsExecutionScope(String nodeType) {
        givenRoute(nodeType);
        assertThrows(ServiceException.class, () -> service.getRouteStartProductionLeaders(10L, null));
        assertThrows(ServiceException.class, () -> service.getRouteStartProductionLeaderProductionLines(10L, null));
        assertThrows(ServiceException.class, () -> service.saveRouteStartProductionLeaders(request(10L)));
        verifyNoInteractions(routeVersionMapper, routeCandidateConfigService);
    }

    @Test
    void flowStillRejectsCopiedLeaderScopeFromAnotherRoute() {
        givenRoute("FLOW");
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(MesProRouteVersionDO.builder()
                .id(99L).routeId(10L).routeSnapshotJson("""
                        {"configSnapshots":{"routeStartProductionLeaders":[
                          {"productionLineId":20,"candidateSourceType":"USERS","candidateSourceIds":[7],
                           "candidateSourceNames":["生产组长"]}]}}
                        """).build());
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of());
        doReturn(List.of()).when(processMapper).selectBatchIds(anyCollection());

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.getRouteStartProductionLeaders(10L, null));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_START_PRODUCTION_LEADER_INVALID.getCode(), error.getCode());
    }

    @Test
    void flowStillRejectsDamagedLeaderSnapshot() {
        givenRoute("FLOW");
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(MesProRouteVersionDO.builder()
                .id(99L).routeId(10L).routeSnapshotJson("""
                        {"configSnapshots":{"routeStartProductionLeaders":{"bad":"snapshot"}}}
                        """).build());

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.getRouteStartProductionLeaders(10L, null));
        assertEquals(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE.getCode(), error.getCode());
    }

    @Test
    void flowSavesOwnScopeAndValidatesLeaderWithoutAffectingTemplateRules() {
        givenRoute("FLOW");
        when(routeVersionMapper.selectById(99L)).thenReturn(MesProRouteVersionDO.builder()
                .id(99L).routeId(10L).active(false).lifecycleStatus("DRAFT")
                .routeSnapshotJson("{\"configSnapshots\":{\"flowGraph\":{\"nodes\":[]}}}").build());
        doReturn(List.of()).when(processMapper).selectBatchIds(anyCollection());

        service.saveRouteStartProductionLeaders(request(10L));

        verify(permissionGateService).requireAbility(any());
        verify(adminUserApi).validateUserList(List.of(7L));
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(99L),
                eq(MesProRouteFlowConfigServiceImpl.ROUTE_START_PRODUCTION_LEADERS_KEY),
                argThat(snapshot -> snapshot.toString().contains("productionLineId=10")));
    }

    @Test
    void flowSaveStillRejectsAnotherRouteScopeWithoutWriting() {
        givenRoute("FLOW");
        when(routeVersionMapper.selectById(99L)).thenReturn(MesProRouteVersionDO.builder()
                .id(99L).routeId(10L).active(false).lifecycleStatus("DRAFT")
                .routeSnapshotJson("{\"configSnapshots\":{\"flowGraph\":{\"nodes\":[]}}}").build());
        doReturn(List.of()).when(processMapper).selectBatchIds(anyCollection());

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.saveRouteStartProductionLeaders(request(20L)));
        assertEquals(PRO_ROUTE_FLOW_CONFIG_START_PRODUCTION_LEADER_INVALID.getCode(), error.getCode());
        verifyNoInteractions(routeCandidateConfigService);
    }

    private void givenRoute(String nodeType) {
        when(routeMapper.selectById(10L)).thenReturn(MesProRouteDO.builder()
                .id(10L).nodeType(nodeType).code("RT-10").name("压力泵实际流程").build());
    }

    private MesProRouteStartProductionLeaderSaveReqVO request(Long scopeId) {
        return new MesProRouteStartProductionLeaderSaveReqVO().setRouteId(10L).setRouteVersionId(99L)
                .setItems(List.of(new MesProRouteStartProductionLeaderItemSaveReqVO()
                        .setProductionLineId(scopeId).setCandidateSourceType("USERS")
                        .setCandidateSourceIds(List.of(7L)).setCandidateSourceNames(List.of("生产组长"))));
    }
}
