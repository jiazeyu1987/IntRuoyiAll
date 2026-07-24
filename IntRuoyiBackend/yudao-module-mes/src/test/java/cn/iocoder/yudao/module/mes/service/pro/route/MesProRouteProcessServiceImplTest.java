package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORKSTATION_PROCESS_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProRouteProcessServiceImplTest {

    @InjectMocks
    private MesProRouteProcessServiceImpl routeProcessService;

    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesProRouteProductService routeProductService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Mock
    private MesMdWorkstationService workstationService;

    @Test
    void createRouteProcess_shouldPersistBoundWorkstationWhenProcessMatches() {
        MesProRouteProcessSaveReqVO reqVO = buildRouteProcessSaveReqVO();
        reqVO.setWorkstationId(3001L);
        when(routeService.getRoute(1001L)).thenReturn(MesProRouteDO.builder().id(1001L).build());
        when(processService.getProcess(2001L)).thenReturn(MesProProcessDO.builder().id(2001L).build());
        when(workstationService.validateWorkstationExistsAndEnable(3001L)).thenReturn(MesMdWorkstationDO.builder()
                .id(3001L)
                .processId(2001L)
                .build());

        routeProcessService.createRouteProcess(reqVO);

        ArgumentCaptor<MesProRouteProcessDO> captor = ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper).insert(captor.capture());
        assertEquals(3001L, captor.getValue().getWorkstationId());
    }

    @Test
    void createRouteProcess_shouldRejectBoundWorkstationWhenProcessDiffers() {
        MesProRouteProcessSaveReqVO reqVO = buildRouteProcessSaveReqVO();
        reqVO.setWorkstationId(3001L);
        when(routeService.getRoute(1001L)).thenReturn(MesProRouteDO.builder().id(1001L).build());
        when(processService.getProcess(2001L)).thenReturn(MesProProcessDO.builder().id(2001L).build());
        when(workstationService.validateWorkstationExistsAndEnable(3001L)).thenReturn(MesMdWorkstationDO.builder()
                .id(3001L)
                .processId(9999L)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> routeProcessService.createRouteProcess(reqVO));

        assertEquals(PRO_WORKSTATION_PROCESS_MISMATCH.getCode(), ex.getCode());
        verify(routeProcessMapper, never()).insert(any(MesProRouteProcessDO.class));
    }

    private MesProRouteProcessSaveReqVO buildRouteProcessSaveReqVO() {
        MesProRouteProcessSaveReqVO reqVO = new MesProRouteProcessSaveReqVO();
        reqVO.setRouteId(1001L);
        reqVO.setProcessId(2001L);
        reqVO.setSort(1);
        reqVO.setKeyFlag(false);
        reqVO.setCheckFlag(false);
        return reqVO;
    }

    @Test
    void deleteRouteProcess_shouldCleanupFlowEdgesAndLayoutForDeletedProcess() {
        Long routeId = 9101L;
        Long routeProcessId = 9102L;
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId)
                .processId(9103L)
                .sort(1)
                .build();
        when(routeProcessMapper.selectById(routeProcessId)).thenReturn(routeProcess);

        routeProcessService.deleteRouteProcess(routeProcessId);

        verify(routeProcessMapper).deleteById(routeProcessId);
        verify(routeProcessFlowService).deleteByRouteProcessId(routeId, routeProcessId);
        verify(routeService).maintainRouteVersionAfterProcessChange(routeId);
    }

    @Test
    void resolveCurrentRouteProcess_shouldReturnDirectCurrentRelation() {
        MesProRouteProcessDO current = MesProRouteProcessDO.builder()
                .id(101L).routeId(11L).processId(21L).build();
        when(routeProcessMapper.selectByRouteIdAndProcessId(11L, 21L)).thenReturn(current);

        MesProRouteProcessDO result = routeProcessService.resolveCurrentRouteProcess(null, 11L, 21L);

        assertSame(current, result);
        verifyNoInteractions(processMapper);
    }

    @Test
    void resolveCurrentRouteProcess_shouldUseHistoricalRouteProcessIdentity() {
        MesProRouteProcessDO historical = MesProRouteProcessDO.builder()
                .id(102L).routeId(11L).processId(22L).build();
        MesProProcessDO legacyProcess = MesProProcessDO.builder()
                .id(22L).code("OP-10").name("旧工序").build();
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(23L).code("OP-10").name("当前工序").build();
        MesProRouteProcessDO current = MesProRouteProcessDO.builder()
                .id(103L).routeId(11L).processId(23L).build();
        when(routeProcessMapper.selectById(102L)).thenReturn(null);
        when(routeProcessMapper.selectByIdIgnoreDeleted(102L)).thenReturn(historical);
        when(routeProcessMapper.selectByRouteIdAndProcessId(11L, 22L)).thenReturn(null);
        when(processMapper.selectByIdIgnoreDeleted(22L)).thenReturn(legacyProcess);
        when(processMapper.selectListByCode("OP-10")).thenReturn(List.of(currentProcess));
        when(routeProcessMapper.selectListByRouteIdAndProcessIds(11L, List.of(23L))).thenReturn(List.of(current));

        MesProRouteProcessDO result = routeProcessService.resolveCurrentRouteProcess(102L, 11L, 999L);

        assertSame(current, result);
    }

    @Test
    void resolveFrozenRouteProcess_shouldReturnHistoricalSnapshotWithoutCurrentRemap() {
        MesProRouteProcessDO historical = MesProRouteProcessDO.builder()
                .id(102L).routeId(11L).processId(22L).build();
        when(routeProcessMapper.selectById(102L)).thenReturn(null);
        when(routeProcessMapper.selectByIdIgnoreDeleted(102L)).thenReturn(historical);

        MesProRouteProcessDO result = routeProcessService.resolveFrozenRouteProcess(102L, 11L, 22L);

        assertSame(historical, result);
        verify(routeProcessMapper, never()).selectByRouteIdAndProcessId(any(), any());
        verifyNoInteractions(processMapper);
    }

    @Test
    void resolveFrozenRouteProcess_shouldIgnoreZeroSourceProcessWhenRouteProcessMatches() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(922483L).routeId(900026L).processId(922894L).build();
        when(routeProcessMapper.selectById(922483L)).thenReturn(routeProcess);

        MesProRouteProcessDO result = routeProcessService.resolveFrozenRouteProcess(922483L, 900026L, 0L);

        assertSame(routeProcess, result);
        verifyNoInteractions(processMapper);
    }

    @Test
    void resolveFrozenRouteProcess_shouldAcceptLegacySourceProcessWhenRouteProcessCodeMatches() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(922483L).routeId(900026L).processId(922894L).build();
        MesProProcessDO legacyProcess = MesProProcessDO.builder()
                .id(900394L).code("Z2630").name("旧吹球囊成型").build();
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(922894L).code("Z2630").name("吹球囊成型").build();
        when(routeProcessMapper.selectById(922483L)).thenReturn(routeProcess);
        when(processMapper.selectByIdIgnoreDeleted(900394L)).thenReturn(legacyProcess);
        when(processMapper.selectByIdIgnoreDeleted(922894L)).thenReturn(currentProcess);

        MesProRouteProcessDO result = routeProcessService.resolveFrozenRouteProcess(922483L, 900026L, 900394L);

        assertSame(routeProcess, result);
    }

    @Test
    void resolveFrozenRouteProcess_shouldAcceptMigratedSourceProcessWhenNameMatchesRouteProcess() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(922339L).routeId(922046L).processId(922789L).build();
        MesProProcessDO sourceProcess = MesProProcessDO.builder()
                .id(922744L).code("B010").name("吹球囊成型").build();
        MesProProcessDO routeProcessDefinition = MesProProcessDO.builder()
                .id(922789L).code("Z2630").name("吹球囊成型").build();
        when(routeProcessMapper.selectById(922339L)).thenReturn(routeProcess);
        when(processMapper.selectByIdIgnoreDeleted(922744L)).thenReturn(sourceProcess);
        when(processMapper.selectByIdIgnoreDeleted(922789L)).thenReturn(routeProcessDefinition);

        MesProRouteProcessDO result = routeProcessService.resolveFrozenRouteProcess(922339L, 922046L, 922744L);

        assertSame(routeProcess, result);
    }

    @Test
    void resolveFrozenRouteProcess_shouldRejectMigratedSourceProcessWhenNameDoesNotMatchRouteProcess() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(922339L).routeId(922046L).processId(922789L).build();
        MesProProcessDO sourceProcess = MesProProcessDO.builder()
                .id(922744L).code("B010").name("吹球囊成型").build();
        MesProProcessDO routeProcessDefinition = MesProProcessDO.builder()
                .id(922789L).code("Z2630").name("其他工序").build();
        when(routeProcessMapper.selectById(922339L)).thenReturn(routeProcess);
        when(processMapper.selectByIdIgnoreDeleted(922744L)).thenReturn(sourceProcess);
        when(processMapper.selectByIdIgnoreDeleted(922789L)).thenReturn(routeProcessDefinition);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> routeProcessService.resolveFrozenRouteProcess(922339L, 922046L, 922744L));

        assertEquals(PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void resolveCurrentRouteProcess_shouldResolveCurrentRelationWhenRouteIdMissing() {
        MesProRouteProcessDO current = MesProRouteProcessDO.builder()
                .id(101L).routeId(11L).processId(21L).build();
        when(routeProcessMapper.selectById(101L)).thenReturn(current);

        MesProRouteProcessDO result = routeProcessService.resolveCurrentRouteProcess(101L, null, null);

        assertSame(current, result);
        verifyNoInteractions(processMapper);
    }

    @Test
    void resolveCurrentRouteProcess_shouldUseHistoricalRouteFromSnapshotWhenRouteIdMissing() {
        MesProRouteProcessDO historical = MesProRouteProcessDO.builder()
                .id(102L).routeId(11L).processId(22L).build();
        MesProProcessDO legacyProcess = MesProProcessDO.builder()
                .id(22L).code("OP-10").name("旧工序").build();
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(23L).code("OP-10").name("当前工序").build();
        MesProRouteProcessDO current = MesProRouteProcessDO.builder()
                .id(103L).routeId(11L).processId(23L).build();
        when(routeProcessMapper.selectById(102L)).thenReturn(null);
        when(routeProcessMapper.selectByIdIgnoreDeleted(102L)).thenReturn(historical);
        when(routeProcessMapper.selectByRouteIdAndProcessId(11L, 22L)).thenReturn(null);
        when(processMapper.selectByIdIgnoreDeleted(22L)).thenReturn(legacyProcess);
        when(processMapper.selectListByCode("OP-10")).thenReturn(List.of(currentProcess));
        when(routeProcessMapper.selectListByRouteIdAndProcessIds(11L, List.of(23L))).thenReturn(List.of(current));

        MesProRouteProcessDO result = routeProcessService.resolveCurrentRouteProcess(102L, null, null);

        assertSame(current, result);
    }

    @Test
    void resolveCurrentRouteProcess_shouldMapLegacyProcessCodeWithoutRouteProcessSnapshot() {
        MesProProcessDO legacyProcess = MesProProcessDO.builder()
                .id(22L).code("OP-10").name("旧工序").build();
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(23L).code("OP-10").name("当前工序").build();
        MesProRouteProcessDO current = MesProRouteProcessDO.builder()
                .id(103L).routeId(11L).processId(23L).build();
        when(routeProcessMapper.selectByRouteIdAndProcessId(11L, 22L)).thenReturn(null);
        when(processMapper.selectByIdIgnoreDeleted(22L)).thenReturn(legacyProcess);
        when(processMapper.selectListByCode("OP-10")).thenReturn(List.of(currentProcess));
        when(routeProcessMapper.selectListByRouteIdAndProcessIds(11L, List.of(23L))).thenReturn(List.of(current));

        MesProRouteProcessDO result = routeProcessService.resolveCurrentRouteProcess(null, 11L, 22L);

        assertSame(current, result);
    }

    @Test
    void resolveCurrentRouteProcess_shouldFailWhenIdentityIsMissing() {
        when(routeProcessMapper.selectByRouteIdAndProcessId(11L, 22L)).thenReturn(null);
        when(processMapper.selectByIdIgnoreDeleted(22L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> routeProcessService.resolveCurrentRouteProcess(null, 11L, 22L));

        assertEquals(PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void resolveCurrentRouteProcess_shouldFailWhenIdentityIsAmbiguous() {
        MesProProcessDO legacyProcess = MesProProcessDO.builder()
                .id(22L).code("OP-10").name("旧工序").build();
        MesProProcessDO currentProcess1 = MesProProcessDO.builder()
                .id(23L).code("OP-10").name("当前工序1").build();
        MesProProcessDO currentProcess2 = MesProProcessDO.builder()
                .id(24L).code("OP-10").name("当前工序2").build();
        MesProRouteProcessDO candidate1 = MesProRouteProcessDO.builder()
                .id(103L).routeId(11L).processId(23L).build();
        MesProRouteProcessDO candidate2 = MesProRouteProcessDO.builder()
                .id(104L).routeId(11L).processId(24L).build();
        when(routeProcessMapper.selectByRouteIdAndProcessId(11L, 22L)).thenReturn(null);
        when(processMapper.selectByIdIgnoreDeleted(22L)).thenReturn(legacyProcess);
        when(processMapper.selectListByCode("OP-10")).thenReturn(List.of(currentProcess1, currentProcess2));
        when(routeProcessMapper.selectListByRouteIdAndProcessIds(11L, List.of(23L, 24L)))
                .thenReturn(List.of(candidate1, candidate2));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> routeProcessService.resolveCurrentRouteProcess(null, 11L, 22L));

        assertEquals(PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS.getCode(), ex.getCode());
    }

    @Test
    void getProcessIdentityMap_shouldMapHistoricalAliasesToRequestedProcess() {
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(23L).code("OP-10").name("当前工序").build();
        MesProProcessDO legacyProcess = MesProProcessDO.builder()
                .id(22L).code("OP-10").name("旧工序").build();
        when(processMapper.selectListByIdsIgnoreDeleted(List.of(23L))).thenReturn(List.of(currentProcess));
        when(processMapper.selectListByCodesIgnoreDeleted(
                argThat(codes -> codes.size() == 1 && codes.contains("OP-10"))))
                .thenReturn(List.of(legacyProcess, currentProcess));

        Map<Long, Long> identityMap = routeProcessService.getProcessIdentityMap(List.of(23L));

        assertEquals(Map.of(22L, 23L, 23L, 23L), identityMap);
    }

    @Test
    void getProcessIdentityMap_shouldIgnoreZeroProcessIdSentinel() {
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(922894L).code("Z2630").name("吹球囊成型").build();
        when(processMapper.selectListByIdsIgnoreDeleted(List.of(922894L))).thenReturn(List.of(currentProcess));
        when(processMapper.selectListByCodesIgnoreDeleted(
                argThat(codes -> codes.size() == 1 && codes.contains("Z2630"))))
                .thenReturn(List.of(currentProcess));

        Map<Long, Long> identityMap = routeProcessService.getProcessIdentityMap(List.of(0L, 922894L));

        assertEquals(Map.of(922894L, 922894L), identityMap);
    }

    @Test
    void getProcessIdentityMap_shouldPreserveExplicitTargetsWhenTargetCodesDuplicate() {
        MesProProcessDO historicalTarget = MesProProcessDO.builder()
                .id(900394L).code("Z2630").name("历史吹球囊成型").build();
        MesProProcessDO currentTarget = MesProProcessDO.builder()
                .id(922894L).code("Z2630").name("当前吹球囊成型").build();
        when(processMapper.selectListByIdsIgnoreDeleted(List.of(900394L, 922894L)))
                .thenReturn(List.of(historicalTarget, currentTarget));
        when(processMapper.selectListByCodesIgnoreDeleted(
                argThat(codes -> codes.size() == 1 && codes.contains("Z2630"))))
                .thenReturn(List.of(historicalTarget, currentTarget));

        Map<Long, Long> identityMap =
                routeProcessService.getProcessIdentityMap(List.of(900394L, 922894L));

        assertEquals(Map.of(900394L, 900394L, 922894L, 922894L), identityMap);
    }

    @Test
    void getProcessIdentityMap_shouldMapExternalAliasToOnlyActiveTargetWhenDuplicateCodeContainsDeletedSnapshot() {
        MesProProcessDO deletedSnapshotTarget = MesProProcessDO.builder()
                .id(922864L).code("Z3710").name("球囊裁剪-快照工序").build();
        deletedSnapshotTarget.setDeleted(true);
        MesProProcessDO activeTarget = MesProProcessDO.builder()
                .id(922895L).code("Z3710").name("球囊裁剪-当前工序").build();
        activeTarget.setDeleted(false);
        MesProProcessDO deletedExternalAlias = MesProProcessDO.builder()
                .id(900400L).code("Z3710").name("球囊裁剪-旧工序").build();
        deletedExternalAlias.setDeleted(true);
        when(processMapper.selectListByIdsIgnoreDeleted(List.of(922864L, 922895L)))
                .thenReturn(List.of(deletedSnapshotTarget, activeTarget));
        when(processMapper.selectListByCodesIgnoreDeleted(
                argThat(codes -> codes.size() == 1 && codes.contains("Z3710"))))
                .thenReturn(List.of(deletedExternalAlias, deletedSnapshotTarget, activeTarget));

        Map<Long, Long> identityMap =
                routeProcessService.getProcessIdentityMap(List.of(922864L, 922895L));

        assertEquals(Map.of(900400L, 922895L, 922864L, 922864L, 922895L, 922895L), identityMap);
    }

    @Test
    void getProcessIdentityMap_shouldNotMapActiveSiblingProcessToSingleRequestedTarget() {
        MesProProcessDO requestedTarget = MesProProcessDO.builder()
                .id(922919L).code("Z2775").name("外管拉伸2-棘突路线").build();
        requestedTarget.setDeleted(false);
        MesProProcessDO deletedSnapshot = MesProProcessDO.builder()
                .id(922851L).code("Z2775").name("外管拉伸2-历史快照").build();
        deletedSnapshot.setDeleted(true);
        MesProProcessDO activeSibling = MesProProcessDO.builder()
                .id(922896L).code("Z2775").name("外管拉伸2-球囊路线").build();
        activeSibling.setDeleted(false);
        when(processMapper.selectListByIdsIgnoreDeleted(List.of(922919L)))
                .thenReturn(List.of(requestedTarget));
        when(processMapper.selectListByCodesIgnoreDeleted(
                argThat(codes -> codes.size() == 1 && codes.contains("Z2775"))))
                .thenReturn(List.of(deletedSnapshot, activeSibling, requestedTarget));

        Map<Long, Long> identityMap = routeProcessService.getProcessIdentityMap(List.of(922919L));

        assertEquals(Map.of(922851L, 922919L, 922919L, 922919L), identityMap);
    }

    @Test
    void getProcessIdentityMap_shouldIgnoreDeletedExternalAliasWhenActiveTargetsDuplicate() {
        MesProProcessDO firstActiveTarget = MesProProcessDO.builder()
                .id(922912L).code("Z2490").name("球囊压握").build();
        firstActiveTarget.setDeleted(false);
        MesProProcessDO secondActiveTarget = MesProProcessDO.builder()
                .id(922935L).code("Z2490").name("球囊压握").build();
        secondActiveTarget.setDeleted(false);
        MesProProcessDO deletedExternalAlias = MesProProcessDO.builder()
                .id(922861L).code("Z2490").name("球囊压握").build();
        deletedExternalAlias.setDeleted(true);
        when(processMapper.selectListByIdsIgnoreDeleted(List.of(922912L, 922935L)))
                .thenReturn(List.of(firstActiveTarget, secondActiveTarget));
        when(processMapper.selectListByCodesIgnoreDeleted(
                argThat(codes -> codes.size() == 1 && codes.contains("Z2490"))))
                .thenReturn(List.of(deletedExternalAlias, firstActiveTarget, secondActiveTarget));

        Map<Long, Long> identityMap =
                routeProcessService.getProcessIdentityMap(List.of(922912L, 922935L));

        assertEquals(Map.of(922912L, 922912L, 922935L, 922935L), identityMap);
    }

    @Test
    void getProcessIdentityMap_shouldFailAliasMappingWhenTargetCodesDuplicate() {
        MesProProcessDO historicalTarget = MesProProcessDO.builder()
                .id(900394L).code("Z2630").name("历史吹球囊成型").build();
        MesProProcessDO currentTarget = MesProProcessDO.builder()
                .id(922894L).code("Z2630").name("当前吹球囊成型").build();
        MesProProcessDO externalAlias = MesProProcessDO.builder()
                .id(800001L).code("Z2630").name("外部绑定旧工序").build();
        when(processMapper.selectListByIdsIgnoreDeleted(List.of(900394L, 922894L)))
                .thenReturn(List.of(historicalTarget, currentTarget));
        when(processMapper.selectListByCodesIgnoreDeleted(
                argThat(codes -> codes.size() == 1 && codes.contains("Z2630"))))
                .thenReturn(List.of(externalAlias, historicalTarget, currentTarget));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> routeProcessService.getProcessIdentityMap(List.of(900394L, 922894L)));

        assertEquals(PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS.getCode(), ex.getCode());
    }
}
