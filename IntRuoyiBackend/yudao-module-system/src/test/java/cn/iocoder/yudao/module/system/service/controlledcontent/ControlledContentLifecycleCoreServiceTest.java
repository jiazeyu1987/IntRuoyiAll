package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_CONTROLLED_FILE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.MES_ROUTE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.IN_REVIEW;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.READY_TO_PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REGISTER_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.SUBMIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControlledContentLifecycleCoreServiceTest {

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
    void registerActiveRef_whenNoActiveRef_insertsActiveRefAndAudit() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectActive(122L, "MES_ROUTE", "9001")).thenReturn(null);

        lifecycleCoreService.registerActiveRef(key, 9001L, 9201L, "V1", "ACTIVE",
                910272L, "route active version registered");

        ArgumentCaptor<ControlledContentVersionRefDO> refCaptor =
                ArgumentCaptor.forClass(ControlledContentVersionRefDO.class);
        verify(versionRefMapper).insert(refCaptor.capture());
        ControlledContentVersionRefDO ref = refCaptor.getValue();
        assertEquals(122L, ref.getTenantId());
        assertEquals("MES_ROUTE", ref.getContentType());
        assertEquals("9001", ref.getContentKey());
        assertEquals(9001L, ref.getNativeMasterId());
        assertEquals(9201L, ref.getNativeVersionId());
        assertEquals("V1", ref.getVersionNo());
        assertEquals("ACTIVE", ref.getCanonicalStatus());
        assertEquals("ACTIVE", ref.getDomainStatus());
        assertEquals(1, ref.getActiveUniqueFlag());

        ArgumentCaptor<ControlledContentTransitionAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ControlledContentTransitionAuditDO.class);
        verify(transitionAuditMapper).insert(auditCaptor.capture());
        ControlledContentTransitionAuditDO audit = auditCaptor.getValue();
        assertEquals(122L, audit.getTenantId());
        assertEquals("MES_ROUTE", audit.getContentType());
        assertEquals("9001", audit.getContentKey());
        assertEquals("ACTIVE", audit.getToStatus());
        assertEquals("REGISTER_ACTIVE", audit.getAction());
        assertEquals(910272L, audit.getActorId());
    }

    @Test
    void registerActiveRef_whenActiveRefExists_failsFastWithoutInsert() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectActive(122L, "MES_ROUTE", "9001"))
                .thenReturn(ControlledContentVersionRefDO.builder()
                        .id(7001L)
                        .tenantId(122L)
                        .contentType("MES_ROUTE")
                        .contentKey("9001")
                        .versionNo("V1")
                        .canonicalStatus(ACTIVE.name())
                        .activeUniqueFlag(1)
                        .build());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.registerActiveRef(key, 9001L, 9201L, "V1", "ACTIVE",
                        910272L, "duplicate active"));

        assertEquals("controlled content already has an active ref: V1/ACTIVE", exception.getMessage());
        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void createCandidateRef_whenNoOpenCandidate_insertsDraftRefAndAudit() {
        ControlledContentKey key = ControlledContentKey.of(122L, DCC_CONTROLLED_FILE, "1001");
        when(versionRefMapper.selectOpenCandidate(122L, "DCC_CONTROLLED_FILE", "1001")).thenReturn(null);
        when(versionRefMapper.selectById(8L)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(8L)
                .tenantId(122L)
                .contentType("DCC_CONTROLLED_FILE")
                .contentKey("1001")
                .nativeVersionId(1999L)
                .canonicalStatus(ACTIVE.name())
                .activeUniqueFlag(1)
                .build());

        lifecycleCoreService.createCandidateRef(key, 1001L, 2002L, "V2", "DRAFT",
                8L, 1999L, 910272L, "create candidate");

        ArgumentCaptor<ControlledContentVersionRefDO> refCaptor =
                ArgumentCaptor.forClass(ControlledContentVersionRefDO.class);
        verify(versionRefMapper).insert(refCaptor.capture());
        ControlledContentVersionRefDO ref = refCaptor.getValue();
        assertEquals(122L, ref.getTenantId());
        assertEquals("DCC_CONTROLLED_FILE", ref.getContentType());
        assertEquals("1001", ref.getContentKey());
        assertEquals(1001L, ref.getNativeMasterId());
        assertEquals(2002L, ref.getNativeVersionId());
        assertEquals("V2", ref.getVersionNo());
        assertEquals("DRAFT", ref.getCanonicalStatus());
        assertEquals("DRAFT", ref.getDomainStatus());
        assertEquals(8L, ref.getSourceVersionRefId());
        assertEquals(1999L, ref.getSourceNativeVersionId());
        assertEquals(1, ref.getOpenCandidateUniqueFlag());

        ArgumentCaptor<ControlledContentTransitionAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ControlledContentTransitionAuditDO.class);
        verify(transitionAuditMapper).insert(auditCaptor.capture());
        ControlledContentTransitionAuditDO audit = auditCaptor.getValue();
        assertEquals(122L, audit.getTenantId());
        assertEquals("DCC_CONTROLLED_FILE", audit.getContentType());
        assertEquals("1001", audit.getContentKey());
        assertEquals("DRAFT", audit.getToStatus());
        assertEquals("CREATE_CANDIDATE", audit.getAction());
        assertEquals(910272L, audit.getActorId());
    }

    @Test
    void createCandidateRef_whenOpenCandidateExists_failsFastWithoutInsert() {
        ControlledContentKey key = ControlledContentKey.of(122L, DCC_CONTROLLED_FILE, "1001");
        when(versionRefMapper.selectOpenCandidate(122L, "DCC_CONTROLLED_FILE", "1001"))
                .thenReturn(ControlledContentVersionRefDO.builder()
                        .id(10L)
                        .tenantId(122L)
                        .contentType("DCC_CONTROLLED_FILE")
                        .contentKey("1001")
                        .versionNo("V2")
                        .canonicalStatus("DRAFT")
                        .openCandidateUniqueFlag(1)
                        .build());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.createCandidateRef(key, 1001L, 2003L, "V3", "DRAFT",
                        8L, 1999L, 910272L, "create duplicate candidate"));

        assertEquals("controlled content already has an open candidate: V2/DRAFT", exception.getMessage());
        verify(versionRefMapper, never()).insert(org.mockito.ArgumentMatchers.any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(
                org.mockito.ArgumentMatchers.any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void createCandidateRef_whenSourceRefNativeVersionDrifted_failsFastWithoutInsert() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectOpenCandidate(122L, "MES_ROUTE", "9001")).thenReturn(null);
        when(versionRefMapper.selectById(8L)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(8L)
                .tenantId(122L)
                .contentType("MES_ROUTE")
                .contentKey("9001")
                .nativeVersionId(1998L)
                .canonicalStatus(ACTIVE.name())
                .activeUniqueFlag(1)
                .build());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.createCandidateRef(key, 9001L, 2002L, "V2", "DRAFT",
                        8L, 1999L, 910272L, "create candidate"));

        assertEquals("controlled content source ref native version drifted", exception.getMessage());
        verify(versionRefMapper, never()).insert(org.mockito.ArgumentMatchers.any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(
                org.mockito.ArgumentMatchers.any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void createCandidateRef_whenSourceRefBelongsToAnotherContent_failsFastWithoutInsert() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectOpenCandidate(122L, "MES_ROUTE", "9001")).thenReturn(null);
        when(versionRefMapper.selectById(8L)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(8L)
                .tenantId(122L)
                .contentType("MES_ROUTE")
                .contentKey("9000")
                .nativeVersionId(1999L)
                .canonicalStatus(ACTIVE.name())
                .activeUniqueFlag(1)
                .build());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.createCandidateRef(key, 9001L, 2002L, "V2", "DRAFT",
                        8L, 1999L, 910272L, "create candidate"));

        assertEquals("controlled content source ref belongs to another content", exception.getMessage());
        verify(versionRefMapper, never()).insert(org.mockito.ArgumentMatchers.any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(
                org.mockito.ArgumentMatchers.any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void createCandidateRef_whenSourceRefIsNotActive_failsFastWithoutInsert() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectOpenCandidate(122L, "MES_ROUTE", "9001")).thenReturn(null);
        when(versionRefMapper.selectById(8L)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(8L)
                .tenantId(122L)
                .contentType("MES_ROUTE")
                .contentKey("9001")
                .nativeVersionId(1999L)
                .canonicalStatus("DRAFT")
                .openCandidateUniqueFlag(1)
                .build());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.createCandidateRef(key, 9001L, 2002L, "V2", "DRAFT",
                        8L, 1999L, 910272L, "create candidate"));

        assertEquals("controlled content source ref is not active", exception.getMessage());
        verify(versionRefMapper, never()).insert(org.mockito.ArgumentMatchers.any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(
                org.mockito.ArgumentMatchers.any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void transitionVersionRef_shouldValidateTransitionUpdateFlagsAndInsertAudit() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectByNativeVersion(122L, "MES_ROUTE", "9001", 1002L))
                .thenReturn(ControlledContentVersionRefDO.builder()
                        .id(11L)
                        .tenantId(122L)
                        .contentType("MES_ROUTE")
                        .contentKey("9001")
                        .nativeVersionId(1002L)
                        .versionNo("V2")
                        .canonicalStatus("DRAFT")
                        .domainStatus("DRAFT")
                        .openCandidateUniqueFlag(1)
                        .build());

        lifecycleCoreService.transitionVersionRef(key, 1002L, IN_REVIEW, "PENDING_APPROVAL",
                SUBMIT, 502L, "route version submitted", "route-approval-502");

        verify(versionRefMapper).update(any(), any());
        ArgumentCaptor<ControlledContentTransitionAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ControlledContentTransitionAuditDO.class);
        verify(transitionAuditMapper).insert(auditCaptor.capture());
        ControlledContentTransitionAuditDO audit = auditCaptor.getValue();
        assertEquals("DRAFT", audit.getFromStatus());
        assertEquals("IN_REVIEW", audit.getToStatus());
        assertEquals("DRAFT", audit.getDomainFromStatus());
        assertEquals("PENDING_APPROVAL", audit.getDomainToStatus());
        assertEquals("SUBMIT", audit.getAction());
    }

    @Test
    void transitionVersionRef_whenProfileDoesNotSupportAction_failsFastWithoutWrite() {
        ControlledContentKey key = ControlledContentKey.of(122L, DCC_CONTROLLED_FILE, "1001");
        when(versionRefMapper.selectByNativeVersion(122L, "DCC_CONTROLLED_FILE", "1001", 2002L))
                .thenReturn(ControlledContentVersionRefDO.builder()
                        .id(31L)
                        .tenantId(122L)
                        .contentType("DCC_CONTROLLED_FILE")
                        .contentKey("1001")
                        .nativeVersionId(2002L)
                        .versionNo("V2")
                        .canonicalStatus(READY_TO_PUBLISH.name())
                        .domainStatus("FINALIZING")
                        .openCandidateUniqueFlag(1)
                        .build());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.transitionVersionRef(key, 2002L, ACTIVE,
                        "ACTIVE", PUBLISH, 502L, "manual publish is not supported for DCC",
                        null));

        assertTrue(exception.getMessage().contains("unsupported controlled content action"));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void publishVersionRefs_shouldSupersedeActiveAndActivateCandidateWithTwoAudits() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectByNativeVersion(122L, "MES_ROUTE", "9001", 1001L))
                .thenReturn(ControlledContentVersionRefDO.builder()
                        .id(21L)
                        .tenantId(122L)
                        .contentType("MES_ROUTE")
                        .contentKey("9001")
                        .nativeVersionId(1001L)
                        .versionNo("V1")
                        .canonicalStatus(ACTIVE.name())
                        .domainStatus("ACTIVE")
                        .activeUniqueFlag(1)
                        .build());
        when(versionRefMapper.selectByNativeVersion(122L, "MES_ROUTE", "9001", 1002L))
                .thenReturn(ControlledContentVersionRefDO.builder()
                        .id(22L)
                        .tenantId(122L)
                        .contentType("MES_ROUTE")
                        .contentKey("9001")
                        .nativeVersionId(1002L)
                        .versionNo("V2")
                        .canonicalStatus(READY_TO_PUBLISH.name())
                        .domainStatus("READY_TO_PUBLISH")
                        .openCandidateUniqueFlag(1)
                        .build());

        lifecycleCoreService.publishVersionRefs(key, 1001L, 1002L, "SUPERSEDED", "ACTIVE",
                504L, "route version published");

        verify(versionRefMapper, times(2)).update(any(), any());
        ArgumentCaptor<ControlledContentTransitionAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ControlledContentTransitionAuditDO.class);
        verify(transitionAuditMapper, times(2)).insert(auditCaptor.capture());
        assertEquals("SUPERSEDED", auditCaptor.getAllValues().get(0).getToStatus());
        assertEquals("SUPERSEDE_ACTIVE", auditCaptor.getAllValues().get(0).getAction());
        assertEquals("ACTIVE", auditCaptor.getAllValues().get(1).getToStatus());
        assertEquals("PUBLISH", auditCaptor.getAllValues().get(1).getAction());
    }

    @Test
    void writeMethods_shouldBeTransactionalToKeepRefAndAuditAtomic() throws NoSuchMethodException {
        assertTransactional("registerActiveRef", ControlledContentKey.class, Long.class, Long.class, String.class,
                String.class, Long.class, String.class);
        assertTransactional("createCandidateRef", ControlledContentKey.class, Long.class, Long.class, String.class,
                String.class, Long.class, Long.class, Long.class, String.class);
        assertTransactional("transitionVersionRef", ControlledContentKey.class, Long.class,
                cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.class,
                String.class,
                cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.class,
                Long.class, String.class, String.class);
        assertTransactional("transitionVersionRefByDomainEvent", ControlledContentKey.class, Long.class,
                cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.class,
                cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.class,
                String.class,
                cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.class,
                Long.class, String.class, String.class, String.class);
        assertTransactional("publishVersionRefs", ControlledContentKey.class, Long.class, Long.class, String.class,
                String.class, Long.class, String.class);
        assertTransactional("finalizeVersionRefs", ControlledContentKey.class, Long.class, Long.class, String.class,
                String.class, Long.class, String.class, String.class);
        assertTransactional("linkSuccessorRef", ControlledContentKey.class, Long.class, Long.class);
    }

    private void assertTransactional(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = ControlledContentLifecycleCoreService.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional != null, methodName + " should be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }

}
