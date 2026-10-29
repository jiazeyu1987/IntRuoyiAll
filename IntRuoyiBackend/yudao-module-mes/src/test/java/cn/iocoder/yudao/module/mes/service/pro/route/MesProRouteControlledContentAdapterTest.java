package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteControlledContentAdapterTest {

    @InjectMocks
    private MesProRouteControlledContentAdapter adapter;

    @Mock
    private ControlledContentLifecycleCoreService lifecycleCoreService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(122L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void reviewedArchive_shouldAllowSourceFreeDraftAndApprovedFirstPublishWithoutReactivatingSummary() {
        MesProRouteVersionDO legacy = MesProRouteVersionDO.builder().id(5L).tenantId(122L).routeId(9001L)
                .active(false).lifecycleStatus(MesRouteLegacySummaryIdentity.STATUS)
                .routeSnapshotFormatVersion(MesRouteLegacySummaryIdentity.FORMAT_VERSION).build();
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder().id(1002L).routeId(9001L).versionNo("V2")
                .active(false).lifecycleStatus("DRAFT").build();
        var key = routeKey(9001L);
        when(lifecycleCoreService.getVersionRef(key, 5L)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(7001L).nativeVersionId(5L).nativeMasterId(9001L).tenantId(122L).canonicalStatus("LEGACY_INCOMPLETE")
                .domainStatus("LEGACY_INCOMPLETE").build());
        try (var identity = org.mockito.Mockito.mockStatic(MesRouteLegacySummaryIdentity.class,
                org.mockito.Mockito.CALLS_REAL_METHODS)) {
            identity.when(() -> MesRouteLegacySummaryIdentity.isKnownArchivedIdentity(legacy)).thenReturn(true);
            adapter.recordFirstCandidateCreated(legacy, candidate, 501L, "normal authoring");
            verify(lifecycleCoreService).createCandidateRef(key, 9001L, 1002L, "V2", "DRAFT", null, null, 501L, "normal authoring");
            candidate.setLifecycleStatus("READY_TO_PUBLISH");
            adapter.recordFirstPublished(legacy, candidate, 501L);
            verify(lifecycleCoreService).transitionVersionRef(key, 1002L, ControlledContentCanonicalStatus.ACTIVE,
                    "ACTIVE", ControlledContentTransitionAction.PUBLISH, 501L, "历史摘要归档后首次完整路线版本发布", null);
            verify(lifecycleCoreService, never()).registerActiveRef(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void ordinaryLifecycleRecord_shouldRejectLegacyFormatEvenWithDraftStatus() {
        MesProRouteVersionDO legacy = MesProRouteVersionDO.builder().id(5L).routeId(9001L).active(false)
                .lifecycleStatus("DRAFT").routeSnapshotFormatVersion(MesRouteLegacySummaryIdentity.FORMAT_VERSION).build();
        assertThrows(IllegalStateException.class, () -> adapter.recordSubmitted(legacy, 501L, null));
        org.mockito.Mockito.verifyNoInteractions(lifecycleCoreService);
    }

    @Test
    void recordActiveRegistered_shouldUseRouteAsPlatformContentKey() {
        MesProRouteVersionDO active = activeVersion();

        adapter.recordActiveRegistered(active, 501L, "route copied");

        verify(lifecycleCoreService).registerActiveRef(routeKey(active.getRouteId()), active.getRouteId(),
                active.getId(), active.getVersionNo(), MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE,
                501L, "route copied");
    }

    @Test
    void recordActiveRegisteredIfMissing_shouldRegisterImportedActiveAndKeepMatchingExistingRef() {
        MesProRouteVersionDO active = activeVersion();
        ControlledContentKey key = routeKey(active.getRouteId());
        when(lifecycleCoreService.getActiveRef(key))
                .thenReturn(null)
                .thenReturn(ControlledContentVersionRefDO.builder()
                        .id(7001L)
                        .nativeVersionId(active.getId())
                        .build());

        adapter.recordActiveRegisteredIfMissing(active, 501L, "Word import active registered");
        adapter.recordActiveRegisteredIfMissing(active, 501L, "Word import active registered");

        verify(lifecycleCoreService).registerActiveRef(key, active.getRouteId(), active.getId(),
                active.getVersionNo(), MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE,
                501L, "Word import active registered");
    }

    @Test
    void recordActiveRegisteredIfMissing_shouldRejectActiveIdentityDrift() {
        MesProRouteVersionDO active = activeVersion();
        ControlledContentKey key = routeKey(active.getRouteId());
        when(lifecycleCoreService.getActiveRef(key)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(7001L)
                .nativeVersionId(9999L)
                .build());

        assertThrows(IllegalStateException.class,
                () -> adapter.recordActiveRegisteredIfMissing(active, 501L, "Word import active registered"));

        verify(lifecycleCoreService, never()).registerActiveRef(key, active.getRouteId(), active.getId(),
                active.getVersionNo(), MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE,
                501L, "Word import active registered");
    }

    @Test
    void recordCandidateCreated_shouldUseRouteAsPlatformContentKeyAndLinkActiveRef() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        ControlledContentKey key = routeKey(active.getRouteId());
        when(lifecycleCoreService.getActiveRef(key)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(7001L)
                .build());

        adapter.recordCandidateCreated(active, candidate, 501L, "平台状态机接入");

        verify(lifecycleCoreService).createCandidateRef(key, active.getRouteId(), candidate.getId(),
                candidate.getVersionNo(), MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                7001L, active.getId(), 501L, "平台状态机接入");
    }

    @Test
    void recordDraftCandidateRegisteredIfMissing_shouldRegisterImportedCandidateOnlyOnce() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        ControlledContentKey key = routeKey(active.getRouteId());
        when(lifecycleCoreService.getVersionRef(key, candidate.getId()))
                .thenReturn(null)
                .thenReturn(ControlledContentVersionRefDO.builder().id(7002L).build());
        when(lifecycleCoreService.getActiveRef(key)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(7001L)
                .nativeVersionId(active.getId())
                .build());

        adapter.recordDraftCandidateRegisteredIfMissing(active, candidate, 501L,
                "Word import candidate registered");
        adapter.recordDraftCandidateRegisteredIfMissing(active, candidate, 501L,
                "Word import candidate registered");

        verify(lifecycleCoreService).createCandidateRef(key, active.getRouteId(), candidate.getId(),
                candidate.getVersionNo(), MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                7001L, active.getId(), 501L, "Word import candidate registered");
    }

    @Test
    void recordSubmittedAndWithdrawn_shouldMirrorReviewStateAndReturnToDraft() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        ControlledContentKey key = routeKey(candidate.getRouteId());
        when(lifecycleCoreService.getVersionRef(key, candidate.getId()))
                .thenReturn(ControlledContentVersionRefDO.builder().id(7002L).build());

        adapter.recordSubmitted(candidate, 502L, "route-approval-502");
        adapter.recordWithdrawn(candidate, 503L);

        verify(lifecycleCoreService).transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW,
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
                ControlledContentTransitionAction.SUBMIT, 502L, "route version submitted",
                "route-approval-502");
        verify(lifecycleCoreService).transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.DRAFT, MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                ControlledContentTransitionAction.WITHDRAW, 503L, "route version approval withdrawn",
                null);
    }

    @Test
    void recordSubmitted_shouldRegisterHistoricalDraftCandidateRefBeforeSubmit() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        ControlledContentKey key = routeKey(candidate.getRouteId());
        when(lifecycleCoreService.getVersionRef(key, candidate.getId())).thenReturn(null);
        when(lifecycleCoreService.getActiveRef(key)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(7001L)
                .nativeVersionId(active.getId())
                .build());

        adapter.recordSubmitted(candidate, 502L, null);

        verify(lifecycleCoreService).createCandidateRef(key, candidate.getRouteId(), candidate.getId(),
                candidate.getVersionNo(), MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                7001L, active.getId(), 502L, "route version historical draft registered before submission");
        verify(lifecycleCoreService).transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW,
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
                ControlledContentTransitionAction.SUBMIT, 502L, "route version submitted",
                null);
    }

    @Test
    void recordRejectedAndCancelled_shouldCloseOpenCandidateInPlatform() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED);
        candidate.setApprovalProcessInstanceId("route-approval-505");
        ControlledContentKey key = routeKey(candidate.getRouteId());
        when(lifecycleCoreService.getVersionRef(key, candidate.getId()))
                .thenReturn(ControlledContentVersionRefDO.builder().id(7002L).build());

        adapter.recordRejected(candidate, 505L, "资料不完整", "approval-event-505");
        adapter.recordCancelled(candidate, 506L);

        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.REJECTED,
                MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED,
                ControlledContentTransitionAction.REJECT, 505L, "资料不完整",
                "route-approval-505", "approval-event-505");
        verify(lifecycleCoreService).transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.CANCELLED,
                MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED,
                ControlledContentTransitionAction.CANCEL, 506L, "route version candidate cancelled",
                null);
    }

    @Test
    void recordCancelled_shouldRegisterHistoricalDraftCandidateRefBeforeCancel() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        ControlledContentKey key = routeKey(candidate.getRouteId());
        when(lifecycleCoreService.getVersionRef(key, candidate.getId())).thenReturn(null);
        when(lifecycleCoreService.getActiveRef(key)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(7001L)
                .nativeVersionId(active.getId())
                .build());

        adapter.recordCancelled(candidate, 506L);

        verify(lifecycleCoreService).createCandidateRef(key, candidate.getRouteId(), candidate.getId(),
                candidate.getVersionNo(), MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                7001L, active.getId(), 506L, "route version historical draft registered before cancellation");
        verify(lifecycleCoreService).transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.CANCELLED,
                MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED,
                ControlledContentTransitionAction.CANCEL, 506L, "route version candidate cancelled",
                null);
    }

    @Test
    void recordApprovedAndPublished_shouldMirrorReadyThenAtomicActiveSwitch() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        candidate.setApprovalProcessInstanceId("route-approval-504");

        adapter.recordApproved(candidate, 504L, "approval-event-504");
        adapter.recordPublished(active, candidate, 504L);

        ControlledContentKey key = routeKey(candidate.getRouteId());
        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW,
                ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                ControlledContentTransitionAction.APPROVE, 504L, "route version approved",
                "route-approval-504", "approval-event-504");
        verify(lifecycleCoreService).publishVersionRefs(key, active.getId(), candidate.getId(),
                MesProRouteVersionLifecycleServiceImpl.STATUS_SUPERSEDED,
                MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE, 504L, "route version published");
    }

    private ControlledContentKey routeKey(Long routeId) {
        return ControlledContentKey.of(122L, ControlledContentType.MES_ROUTE, String.valueOf(routeId));
    }

    private MesProRouteVersionDO activeVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
    }

    private MesProRouteVersionDO draftCandidate(MesProRouteVersionDO active) {
        return MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(active.getRouteId())
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(active.getId())
                .build();
    }
}
