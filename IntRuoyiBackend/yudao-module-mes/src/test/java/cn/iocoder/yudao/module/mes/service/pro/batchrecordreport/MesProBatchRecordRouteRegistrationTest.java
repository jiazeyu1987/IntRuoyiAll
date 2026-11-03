package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteControlledContentAdapter;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProBatchRecordRouteRegistrationTest {
    private final MesProBatchRecordRouteGenerationServiceImpl service = new MesProBatchRecordRouteGenerationServiceImpl();
    private final MesProRouteVersionMapper versions = mock(MesProRouteVersionMapper.class);
    private final MesProRouteControlledContentAdapter adapter = mock(MesProRouteControlledContentAdapter.class);
    private final MesProRouteDO route = MesProRouteDO.builder().id(42L).code("TEST").name("项目").status(0).build();
    private final MesProRouteVersionDO active = MesProRouteVersionDO.builder()
            .id(100L).routeId(42L).versionNo("V1").active(true).lifecycleStatus("ACTIVE").build();

    MesProBatchRecordRouteRegistrationTest() {
        ReflectionTestUtils.setField(service, "routeVersionMapper", versions);
        ReflectionTestUtils.setField(service, "routeControlledContentAdapter", adapter);
        ReflectionTestUtils.setField(service, "routeProductMapper", mock(MesProRouteProductMapper.class));
        ReflectionTestUtils.setField(service, "processMapper", mock(MesProProcessMapper.class));
        MesProRouteService routes = mock(MesProRouteService.class);
        ReflectionTestUtils.setField(service, "routeService", routes);
        when(routes.buildCurrentRouteSnapshotJson(42L, 100L)).thenReturn("""
                {"configSnapshots":{"flowGraph":{"nodes":[]},"batchUseConfigs":[],
                "routeStartProductionLeaders":[],"batchRecordAttachmentOwners":[]}}
                """);
        when(versions.selectActiveByRouteIdForUpdate(42L)).thenReturn(active);
        when(versions.selectListByRouteId(42L)).thenReturn(List.of(active));
        when(versions.insert(any(MesProRouteVersionDO.class))).thenAnswer(call -> {
            call.<MesProRouteVersionDO>getArgument(0).setId(101L);
            return 1;
        });
    }

    @Test
    void wordInitialVersionRegistersActiveReference() {
        MesProRouteVersionDO created = ReflectionTestUtils.invokeMethod(service,
                "createInitialActiveRouteVersionForUploadedWord", route, null);
        assertNotNull(created);
        assertTrue(created.getActive());
        verify(adapter).recordActiveRegistered(eq(created), isNull(), anyString());
    }

    @Test
    void wordNewCandidateRegistersActiveAndCandidateInOrder() {
        MesProRouteVersionDO candidate = createCandidate(null);
        assertNotNull(candidate);
        assertEquals("DRAFT", candidate.getLifecycleStatus());
        assertEquals(active.getId(), candidate.getSourceRouteVersionId());
        var order = inOrder(adapter);
        order.verify(adapter).recordActiveRegisteredIfMissing(eq(active), isNull(), anyString());
        order.verify(adapter).recordDraftCandidateRegisteredIfMissing(eq(active), eq(candidate), isNull(), anyString());
    }

    @Test
    void wordExistingDraftRegistersMissingReferencesWithoutCreatingAnotherVersion() {
        MesProRouteVersionDO existing = MesProRouteVersionDO.builder().id(101L).routeId(42L)
                .versionNo("V2").active(false).lifecycleStatus("DRAFT").sourceRouteVersionId(100L).build();
        when(versions.selectOpenCandidateByRouteId(42L)).thenReturn(existing);
        assertSame(existing, createCandidate(101L));
        verify(versions, never()).insert(any(MesProRouteVersionDO.class));
        verify(adapter).recordDraftCandidateRegisteredIfMissing(eq(active), eq(existing), isNull(), anyString());
    }

    @Test
    void controlledContentRegistrationFailureIsNotSwallowed() {
        doThrow(new IllegalStateException("registration failed")).when(adapter)
                .recordActiveRegisteredIfMissing(any(), any(), anyString());
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> createCandidate(null));
        assertEquals("registration failed", error.getMessage());
        verify(adapter, never()).recordDraftCandidateRegisteredIfMissing(any(), any(), any(), anyString());
    }

    private MesProRouteVersionDO createCandidate(Long expectedCandidateId) {
        return ReflectionTestUtils.invokeMethod(service, "createOrUpdateCandidateRouteVersion",
                route, active, List.of(), List.of("项目"), false, expectedCandidateId);
    }
}
