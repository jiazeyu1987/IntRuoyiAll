package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionLifecycleServiceTest {

    @InjectMocks
    private MesProRouteVersionLifecycleServiceImpl lifecycleService;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteVersionPublishProjectionServiceImpl publishProjectionService;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;
    @Spy
    private MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();

    @Test
    void publishCandidate_shouldPersistCanonicalRewrittenSnapshotBeforeStatusSwitch() {
        String activeSnapshot = validSnapshotJson(9601L, "RT-9601-V1", "Route V1");
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(1601L)
                .routeId(9601L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson(activeSnapshot)
                .routeSnapshotSha256(canonicalizer.sha256(activeSnapshot))
                .routeSnapshotFormatVersion(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION)
                .build();
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1602L)
                .routeId(9601L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(validSnapshotJson(9601L, "RT-9601-V2", "Route V2"))
                .build();
        String rewrittenSnapshot = validSnapshotJsonWithRouteProcessId(
                9601L, "RT-9601-V2", "Route V2", 210L);
        String activeHashBefore = active.getRouteSnapshotSha256();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);
        when(routeVersionMapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        when(publishProjectionService.projectCandidate(candidate)).thenReturn(
                new MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult(
                        rewrittenSnapshot, Set.of(210L)));

        lifecycleService.publishCandidate(candidate.getId(), 607L);

        ArgumentCaptor<MesProRouteVersionDO> updateCaptor =
                ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper, times(3)).updateById(updateCaptor.capture());
        MesProRouteVersionDO snapshotUpdate = updateCaptor.getAllValues().get(0);
        assertEquals(candidate.getId(), snapshotUpdate.getId());
        assertEquals(canonicalizer.canonicalize(rewrittenSnapshot), snapshotUpdate.getRouteSnapshotJson());
        assertEquals(canonicalizer.sha256(rewrittenSnapshot), snapshotUpdate.getRouteSnapshotSha256());
        assertEquals(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION,
                snapshotUpdate.getRouteSnapshotFormatVersion());
        assertEquals(activeSnapshot, active.getRouteSnapshotJson());
        assertEquals(activeHashBefore, active.getRouteSnapshotSha256());
    }

    @Test
    void publishCandidate_shouldSupersedeActiveAndActivateCandidateInOneServiceCall() {
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(validSnapshotJson(9001L, "RT-9001-V2", "Route V2"))
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);
        when(routeVersionMapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        when(publishProjectionService.projectCandidate(candidate)).thenReturn(
                projection(candidate.getRouteSnapshotJson(), 10L));

        MesProRouteVersionDO published;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(600L);
            published = lifecycleService.publishCandidate(candidate.getId());
        }

        assertEquals(candidate.getId(), published.getId());
        assertEquals(Boolean.TRUE, published.getActive());
        assertEquals("ACTIVE", published.getLifecycleStatus());
        assertNotNull(published.getPublishedTime());

        ArgumentCaptor<MesProRouteVersionDO> updateCaptor =
                ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper, times(3)).updateById(updateCaptor.capture());
        List<MesProRouteVersionDO> updates = updateCaptor.getAllValues();
        assertEquals(candidate.getId(), updates.get(0).getId());
        assertNotNull(updates.get(0).getRouteSnapshotSha256());
        assertEquals(active.getId(), updates.get(1).getId());
        assertEquals(Boolean.FALSE, updates.get(1).getActive());
        assertEquals("SUPERSEDED", updates.get(1).getLifecycleStatus());
        assertEquals(candidate.getId(), updates.get(2).getId());
        assertEquals(Boolean.TRUE, updates.get(2).getActive());
        assertEquals("ACTIVE", updates.get(2).getLifecycleStatus());
        assertEquals(600L, updates.get(2).getPublishedBy());
        assertNotNull(updates.get(2).getPublishedTime());
        verify(publishProjectionService).projectCandidate(candidate);
    }

    @Test
    void publishCandidate_shouldRecordPublisherAuditFields() {
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(1401L)
                .routeId(9401L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1402L)
                .routeId(9401L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(validSnapshotJson(9401L, "RT-9401-V2", "Route V2"))
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);
        when(routeVersionMapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        when(publishProjectionService.projectCandidate(candidate)).thenReturn(
                projection(candidate.getRouteSnapshotJson(), 10L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(601L);

            MesProRouteVersionDO published = lifecycleService.publishCandidate(candidate.getId());

            ArgumentCaptor<MesProRouteVersionDO> updateCaptor =
                    ArgumentCaptor.forClass(MesProRouteVersionDO.class);
            verify(routeVersionMapper, times(3)).updateById(updateCaptor.capture());
            MesProRouteVersionDO candidateUpdate = updateCaptor.getAllValues().get(2);
            assertEquals(601L, candidateUpdate.getPublishedBy());
            assertNotNull(candidateUpdate.getPublishedTime());
            assertEquals(601L, published.getPublishedBy());
            assertNotNull(published.getPublishedTime());
        }
    }

    @Test
    void publishCandidate_shouldReturnAlreadyActiveCandidateIdempotently() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1502L)
                .routeId(9501L)
                .versionNo("V2")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .sourceRouteVersionId(1501L)
                .routeSnapshotJson(validSnapshotJson(9501L, "RT-9501-V2", "Route V2"))
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);

        MesProRouteVersionDO published = lifecycleService.publishCandidate(candidate.getId(), 601L);

        assertEquals(candidate.getId(), published.getId());
        assertEquals(Boolean.TRUE, published.getActive());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE, published.getLifecycleStatus());
        verify(publishProjectionService, never()).projectCandidate(any(MesProRouteVersionDO.class));
        verify(routeVersionMapper, never()).selectActiveByRouteId(candidate.getRouteId());
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
    }

    @Test
    void publishCandidate_shouldProjectCandidateBeforeActivatingCandidate() {
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(1101L)
                .routeId(9101L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1102L)
                .routeId(9101L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus("READY_TO_PUBLISH")
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(validSnapshotJson(9101L, "RT-9101-V2", "Route V2"))
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);
        when(routeVersionMapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        when(publishProjectionService.projectCandidate(candidate)).thenReturn(
                projection(candidate.getRouteSnapshotJson(), 10L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(602L);
            lifecycleService.publishCandidate(candidate.getId());
        }

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(publishProjectionService, routeVersionMapper);
        inOrder.verify(publishProjectionService).projectCandidate(candidate);
        inOrder.verify(routeVersionMapper).updateById(org.mockito.ArgumentMatchers.<MesProRouteVersionDO>argThat(update ->
                candidate.getId().equals(update.getId())
                        && update.getRouteSnapshotSha256() != null
                        && MesProRouteSnapshotCanonicalizer.FORMAT_VERSION.equals(
                        update.getRouteSnapshotFormatVersion())));
        inOrder.verify(routeVersionMapper).updateById(org.mockito.ArgumentMatchers.<MesProRouteVersionDO>argThat(update ->
                active.getId().equals(update.getId())
                        && Boolean.FALSE.equals(update.getActive())
                        && "SUPERSEDED".equals(update.getLifecycleStatus())));
        inOrder.verify(routeVersionMapper).updateById(org.mockito.ArgumentMatchers.<MesProRouteVersionDO>argThat(update ->
                candidate.getId().equals(update.getId())
                        && Boolean.TRUE.equals(update.getActive())
                        && "ACTIVE".equals(update.getLifecycleStatus())));
    }

    @Test
    void publishCandidate_shouldNotSwitchActiveWhenProjectionFails() {
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(1201L)
                .routeId(9201L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1202L)
                .routeId(9201L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(validSnapshotJson(9201L, "RT-9201-V2", "Route V2"))
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);
        doThrow(new IllegalArgumentException("schedule config process does not exist"))
                .when(publishProjectionService).projectCandidate(candidate);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> lifecycleService.publishCandidate(candidate.getId(), 602L));
        assertEquals("schedule config process does not exist", ex.getMessage());
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
    }

    @Test
    void publishCandidate_shouldRejectDraftUntilSubmittedOrApproved() {
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(1301L)
                .routeId(9301L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1302L)
                .routeId(9301L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(validSnapshotJson(9301L, "RT-9301-V2", "Route V2"))
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> lifecycleService.publishCandidate(candidate.getId(), 603L));
        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
        verify(publishProjectionService, never()).projectCandidate(any(MesProRouteVersionDO.class));
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
    }

    @Test
    void publishCandidate_shouldRejectActiveDrift() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1003L)
                .routeId(9002L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson(validSnapshotJson(9002L, "RT-9002-V2", "Route V2"))
                .build();
        MesProRouteVersionDO driftedActive = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9002L)
                .versionNo("V3")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(driftedActive);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> lifecycleService.publishCandidate(candidate.getId(), 604L));
        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void publishCandidate_shouldRejectNonDraftCandidate() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1004L)
                .routeId(9003L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus("REJECTED")
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson("{\"routeId\":9003}")
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> lifecycleService.publishCandidate(candidate.getId(), 605L));
        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
    }

    @Test
    void publishCandidate_shouldRejectCandidateWithoutConfigSnapshots() {
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(1005L)
                .routeId(9004L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1006L)
                .routeId(9004L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson("{\"routeId\":9004}")
                .build();

        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> lifecycleService.publishCandidate(candidate.getId(), 606L));
        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE.getCode(), ex.getCode());
    }

    private String validSnapshotJson(Long routeId, String routeCode, String routeName) {
        return validSnapshotJsonWithRouteProcessId(routeId, routeCode, routeName, 10L);
    }

    private MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult projection(
            String snapshotJson, Long routeProcessId) {
        return new MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult(
                snapshotJson, Set.of(routeProcessId));
    }

    private String validSnapshotJsonWithRouteProcessId(Long routeId, String routeCode, String routeName,
                                                       Long routeProcessId) {
        return """
                {
                  "routeId": %d,
                  "routeCode": "%s",
                  "routeName": "%s",
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": %d, "processId": 20, "sort": 1}
                      ],
                      "edges": []
                    },
                    "products": [],
                    "scheduleConfigs": [],
                    "batchUseConfigs": [],
                    "scheduleUseConfigs": []
                  }
                }
                """.formatted(routeId, routeCode, routeName, routeProcessId);
    }
}
