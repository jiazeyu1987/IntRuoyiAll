package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccControlledContentAdapterTest {

    @InjectMocks
    private DccControlledContentAdapter adapter;

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
    void recordSubmitted_shouldCreatePlatformCandidateAndMoveItToReview() {
        DccControlledFileDO file = pendingFile(2002L);
        ControlledContentKey key = dccKey();
        when(lifecycleCoreService.getActiveRef(key)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(11L)
                .nativeVersionId(2001L)
                .build());

        adapter.recordSubmitted(file, 501L, "process-1");

        verify(lifecycleCoreService).createCandidateRef(key, 1001L, 2002L, "V2.0",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(), 11L, 2001L,
                501L, "dcc controlled file submitted");
        verify(lifecycleCoreService).transitionVersionRef(key, 2002L, ControlledContentCanonicalStatus.IN_REVIEW,
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(),
                ControlledContentTransitionAction.SUBMIT, 501L, "dcc controlled file submitted",
                "process-1");
    }

    @Test
    void recordWithdrawnAndResubmitted_shouldCloseOldRefAndLinkSuccessor() {
        DccControlledFileDO oldFile = pendingFile(2002L);

        adapter.recordWithdrawn(oldFile, 501L, "applicant withdraw");
        adapter.recordResubmitted(oldFile, 2003L);

        verify(lifecycleCoreService).transitionVersionRef(dccKey(), 2002L, ControlledContentCanonicalStatus.WITHDRAWN,
                DccControlledFileStatusEnum.WITHDRAWN.getStatus(), ControlledContentTransitionAction.WITHDRAW,
                501L, "applicant withdraw", "process-1");
        verify(lifecycleCoreService).linkSuccessorRef(dccKey(), 2002L, 2003L);
    }

    @Test
    void recordFinalization_shouldUseStableEventKeysAndFinalizeCandidate() {
        DccControlledFileDO candidate = finalizingFile(2002L);
        DccControlledFileDO active = activeFile(2001L);

        adapter.recordFinalizationStarted(candidate, 501L, "process-1");
        adapter.recordFinalized(active, candidate, 501L, "process-1");

        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(dccKey(), 2002L,
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                DccControlledFileStatusEnum.FINALIZING.getStatus(), ControlledContentTransitionAction.APPROVE,
                501L, "dcc controlled file approved", "process-1", "process-1:approve");
        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(dccKey(), 2002L,
                ControlledContentCanonicalStatus.READY_TO_PUBLISH, ControlledContentCanonicalStatus.FINALIZING,
                DccControlledFileStatusEnum.FINALIZING.getStatus(), ControlledContentTransitionAction.START_FINALIZATION,
                501L, "dcc controlled file finalization started", "process-1",
                "process-1:start-finalization");
        verify(lifecycleCoreService).finalizeVersionRefs(dccKey(), 2001L, 2002L,
                DccControlledFileStatusEnum.SUPERSEDED.getStatus(), DccControlledFileStatusEnum.ACTIVE.getStatus(),
                501L, "dcc controlled file finalization succeeded", "process-1:finalize-success");
    }

    @Test
    void recordApprovedReadyToPublish_shouldNotStartFinalization() {
        DccControlledFileDO candidate = pendingFile(2002L);

        adapter.recordApprovedReadyToPublish(candidate, 501L, "process-1");

        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(dccKey(), 2002L,
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), ControlledContentTransitionAction.APPROVE,
                501L, "dcc controlled file approved and waiting for publish",
                "process-1", "process-1:approve");
        verifyNoMoreInteractions(lifecycleCoreService);
    }

    @Test
    void recordPublishFinalizationStarted_shouldStartFromReadyToPublish() {
        DccControlledFileDO candidate = finalizingFile(2002L);
        candidate.setStatus(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus());

        adapter.recordPublishFinalizationStarted(candidate, 501L, "publish-effect-1");

        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(dccKey(), 2002L,
                ControlledContentCanonicalStatus.READY_TO_PUBLISH, ControlledContentCanonicalStatus.FINALIZING,
                DccControlledFileStatusEnum.FINALIZING.getStatus(),
                ControlledContentTransitionAction.START_FINALIZATION,
                501L, "dcc controlled file publish finalization started", "process-1",
                "publish-effect-1:start-finalization");
        verifyNoMoreInteractions(lifecycleCoreService);
    }

    @Test
    void recordFinalizationRetried_shouldMoveFailedCandidateBackToFinalizing() {
        DccControlledFileDO candidate = finalizingFile(2002L);
        candidate.setStatus(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus());

        adapter.recordFinalizationRetried(candidate, 501L, "dcc-finalization-retry:2002");

        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(dccKey(), 2002L,
                ControlledContentCanonicalStatus.FINALIZATION_FAILED, ControlledContentCanonicalStatus.FINALIZING,
                DccControlledFileStatusEnum.FINALIZING.getStatus(), ControlledContentTransitionAction.RETRY_FINALIZATION,
                501L, "dcc controlled file finalization retried", "process-1",
                "dcc-finalization-retry:2002:retry-finalization");
    }

    @Test
    void recordApprovedUploadFinalizationStarted_shouldCreateAndAdvanceCandidateFromFormCenterApproval() {
        DccControlledFileDO candidate = finalizingFile(2002L);
        ControlledContentKey key = dccKey();
        when(lifecycleCoreService.getActiveRef(key)).thenReturn(null);

        adapter.recordApprovedUploadFinalizationStarted(candidate, 501L, "form-process-1", "effect-idem-1");

        verify(lifecycleCoreService).createCandidateRef(key, 1001L, 2002L, "V2.0",
                DccControlledFileStatusEnum.FINALIZING.getStatus(), null, null,
                501L, "dcc form-center upload approved");
        verify(lifecycleCoreService).transitionVersionRef(key, 2002L, ControlledContentCanonicalStatus.IN_REVIEW,
                DccControlledFileStatusEnum.FINALIZING.getStatus(), ControlledContentTransitionAction.SUBMIT,
                501L, "dcc form-center upload approved", "form-process-1");
        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(key, 2002L,
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                DccControlledFileStatusEnum.FINALIZING.getStatus(), ControlledContentTransitionAction.APPROVE,
                501L, "dcc form-center upload approved", "form-process-1", "effect-idem-1:approve");
        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(key, 2002L,
                ControlledContentCanonicalStatus.READY_TO_PUBLISH, ControlledContentCanonicalStatus.FINALIZING,
                DccControlledFileStatusEnum.FINALIZING.getStatus(), ControlledContentTransitionAction.START_FINALIZATION,
                501L, "dcc form-center upload finalization started", "form-process-1",
                "effect-idem-1:start-finalization");
    }

    @Test
    void recordApprovedUploadReadyToPublish_shouldCreateCandidateWithoutFinalization() {
        DccControlledFileDO candidate = pendingFile(2002L);
        candidate.setStatus(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus());
        ControlledContentKey key = dccKey();
        when(lifecycleCoreService.getActiveRef(key)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(11L)
                .nativeVersionId(2001L)
                .build());

        adapter.recordApprovedUploadReadyToPublish(candidate, 501L, "form-process-1", "effect-idem-1");

        verify(lifecycleCoreService).createCandidateRef(key, 1001L, 2002L, "V2.0",
                DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), 11L, 2001L,
                501L, "dcc form-center upload approved");
        verify(lifecycleCoreService).transitionVersionRef(key, 2002L, ControlledContentCanonicalStatus.IN_REVIEW,
                DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), ControlledContentTransitionAction.SUBMIT,
                501L, "dcc form-center upload approved", "form-process-1");
        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(key, 2002L,
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), ControlledContentTransitionAction.APPROVE,
                501L, "dcc form-center upload approved and waiting for publish", "form-process-1",
                "effect-idem-1:approve");
        verifyNoMoreInteractions(lifecycleCoreService);
    }

    @Test
    void recordObsoleted_shouldReleasePlatformActiveWithStableEventKey() {
        DccControlledFileDO active = activeFile(2001L);

        adapter.recordObsoleted(active, 501L, "obsolete reason", "dcc-obsolete:2001");

        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(dccKey(), 2001L,
                ControlledContentCanonicalStatus.ACTIVE, ControlledContentCanonicalStatus.OBSOLETE,
                DccControlledFileStatusEnum.OBSOLETE.getStatus(), ControlledContentTransitionAction.OBSOLETE_ACTIVE,
                501L, "obsolete reason", null, "dcc-obsolete:2001");
    }

    @Test
    void recordWorkflowObsoleted_shouldCloseActiveAndCandidateRefsWithStableEventKeys() {
        DccControlledFileDO active = activeFile(2001L);
        DccControlledFileDO obsoleteCandidate = finalizingFile(2002L);

        adapter.recordWorkflowObsoleted(active, obsoleteCandidate, 501L, "obsolete reason", "process-1");

        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(dccKey(), 2001L,
                ControlledContentCanonicalStatus.ACTIVE, ControlledContentCanonicalStatus.OBSOLETE,
                DccControlledFileStatusEnum.OBSOLETE.getStatus(), ControlledContentTransitionAction.OBSOLETE_ACTIVE,
                501L, "obsolete reason", null, "process-1:active-obsolete");
        verify(lifecycleCoreService).transitionVersionRefByDomainEvent(dccKey(), 2002L,
                ControlledContentCanonicalStatus.FINALIZING, ControlledContentCanonicalStatus.OBSOLETE,
                DccControlledFileStatusEnum.OBSOLETE.getStatus(), ControlledContentTransitionAction.FINALIZE_SUCCESS,
                501L, "obsolete reason", "process-1", "process-1:candidate-obsolete");
    }

    private ControlledContentKey dccKey() {
        return ControlledContentKey.of(122L, ControlledContentType.DCC_CONTROLLED_FILE, "1001");
    }

    private DccControlledFileDO pendingFile(Long id) {
        return DccControlledFileDO.builder()
                .id(id)
                .masterId(1001L)
                .versionNo("V2.0")
                .processInstanceId("process-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build();
    }

    private DccControlledFileDO finalizingFile(Long id) {
        return DccControlledFileDO.builder()
                .id(id)
                .masterId(1001L)
                .versionNo("V2.0")
                .processInstanceId("process-1")
                .status(DccControlledFileStatusEnum.FINALIZING.getStatus())
                .build();
    }

    private DccControlledFileDO activeFile(Long id) {
        return DccControlledFileDO.builder()
                .id(id)
                .masterId(1001L)
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
    }

}
