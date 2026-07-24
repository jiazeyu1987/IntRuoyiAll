package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.IN_REVIEW;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.READY_TO_PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.WITHDRAWN;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.APPROVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_CONTROLLED_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledContentDomainEventIdempotencyTest {

    @Mock
    private ControlledContentVersionRefMapper versionRefMapper;
    @Mock
    private ControlledContentTransitionAuditMapper transitionAuditMapper;

    private ControlledContentLifecycleCoreService lifecycleCoreService;

    @BeforeEach
    void setUp() {
        lifecycleCoreService = new ControlledContentLifecycleCoreService(versionRefMapper, transitionAuditMapper,
                new ControlledContentStateMachine());
    }

    @Test
    void transitionByDomainEvent_shouldPersistEventKeyOnFirstCurrentEvent() {
        ControlledContentKey key = dccKey();
        ControlledContentVersionRefDO ref = inReviewRef();
        when(versionRefMapper.selectByNativeVersion(122L, "DCC_CONTROLLED_FILE", "1001", 2002L))
                .thenReturn(ref);
        when(transitionAuditMapper.selectByVersionRefIdAndActionAndEventKey(11L, "APPROVE",
                "bpm:process-1:approve-task-9")).thenReturn(null);

        lifecycleCoreService.transitionVersionRefByDomainEvent(key, 2002L, IN_REVIEW, READY_TO_PUBLISH,
                "PENDING_FINALIZATION", APPROVE, 910272L, "dcc approval completed",
                "process-1", "bpm:process-1:approve-task-9");

        verify(versionRefMapper).update(any(), any());
        ArgumentCaptor<ControlledContentTransitionAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ControlledContentTransitionAuditDO.class);
        verify(transitionAuditMapper).insert(auditCaptor.capture());
        assertEquals("bpm:process-1:approve-task-9", auditCaptor.getValue().getEventKey());
    }

    @Test
    void transitionByDomainEvent_shouldReturnCurrentFactWithoutDuplicateSideEffects() {
        ControlledContentKey key = dccKey();
        ControlledContentVersionRefDO ref = inReviewRef();
        when(versionRefMapper.selectByNativeVersion(122L, "DCC_CONTROLLED_FILE", "1001", 2002L))
                .thenReturn(ref);
        when(transitionAuditMapper.selectByVersionRefIdAndActionAndEventKey(11L, "APPROVE",
                "bpm:process-1:approve-task-9"))
                .thenReturn(ControlledContentTransitionAuditDO.builder().id(91L).eventKey("bpm:process-1:approve-task-9").build());

        ControlledContentVersionRefDO result = lifecycleCoreService.transitionVersionRefByDomainEvent(key, 2002L,
                IN_REVIEW, READY_TO_PUBLISH, "PENDING_FINALIZATION", APPROVE, 910272L,
                "duplicate dcc approval completed", "process-1", "bpm:process-1:approve-task-9");

        assertEquals(ref.getId(), result.getId());
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void transitionByDomainEvent_shouldRejectStaleApprovalInstance() {
        ControlledContentKey key = dccKey();
        ControlledContentVersionRefDO withdrawn = inReviewRef();
        withdrawn.setCanonicalStatus(WITHDRAWN.name());
        withdrawn.setApprovalProcessInstanceId(null);
        when(versionRefMapper.selectByNativeVersion(122L, "DCC_CONTROLLED_FILE", "1001", 2002L))
                .thenReturn(withdrawn);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.transitionVersionRefByDomainEvent(key, 2002L, IN_REVIEW,
                        READY_TO_PUBLISH, "PENDING_FINALIZATION", APPROVE, 910272L,
                        "stale dcc approval completed", "process-1", "bpm:process-1:approve-task-9"));

        assertTrue(ex.getMessage().contains("stale controlled content domain event"));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    private ControlledContentKey dccKey() {
        return ControlledContentKey.of(122L, DCC_CONTROLLED_FILE, "1001");
    }

    private ControlledContentVersionRefDO inReviewRef() {
        return ControlledContentVersionRefDO.builder()
                .id(11L)
                .tenantId(122L)
                .contentType("DCC_CONTROLLED_FILE")
                .contentKey("1001")
                .nativeVersionId(2002L)
                .versionNo("V2")
                .canonicalStatus(IN_REVIEW.name())
                .domainStatus("PENDING_DOC_CONTROL_APPROVAL")
                .approvalProcessInstanceId("process-1")
                .openCandidateUniqueFlag(1)
                .build();
    }
}
