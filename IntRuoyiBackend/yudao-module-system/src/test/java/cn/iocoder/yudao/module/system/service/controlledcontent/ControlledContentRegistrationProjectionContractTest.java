package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.stream.Stream;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.READY_TO_PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.SUPERSEDED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_CONTROLLED_FILE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControlledContentRegistrationProjectionContractTest {

    private static final ControlledContentKey KEY = ControlledContentKey.of(
            122L, DCC_REGISTRATION_CERTIFICATE, "REG-9001");

    @Mock
    private ControlledContentVersionRefMapper versionRefMapper;
    @Mock
    private ControlledContentTransitionAuditMapper transitionAuditMapper;

    private ControlledContentRegistrationProjectionService service;

    @BeforeEach
    void setUp() {
        service = new ControlledContentRegistrationProjectionService(versionRefMapper, transitionAuditMapper,
                new ControlledContentStateMachine());
        lenient().when(versionRefMapper.update(any(), any())).thenReturn(1);
        lenient().when(transitionAuditMapper.insert(any(ControlledContentTransitionAuditDO.class))).thenReturn(1);
    }

    @Test
    void publish_whenCountsMatchButActiveIdDrifts_failsBeforeFirstWrite() {
        ControlledContentVersionRefDO driftedActive = ref(11L, 101L, ACTIVE.name(), 1, null);
        ControlledContentVersionRefDO candidate = ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1);
        when(versionRefMapper.selectList(any())).thenReturn(List.of(driftedActive, candidate));
        stubLatestAudits(driftedActive, candidate);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.publish(
                KEY, snapshot(100L, 200L), snapshot(200L, null),
                "SUPERSEDED", "ACTIVE", 501L, "effective-date switch"));

        assertTrue(exception.getMessage().contains("active native version ID expected 100 but was 101"));
        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void publish_whenExistingRefHasNoAudit_failsBeforeFirstWrite() {
        ControlledContentVersionRefDO active = ref(11L, 100L, ACTIVE.name(), 1, null);
        ControlledContentVersionRefDO candidate = ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1);
        when(versionRefMapper.selectList(any())).thenReturn(List.of(active, candidate));
        when(transitionAuditMapper.selectOne(any())).thenReturn(audit(active),
                (ControlledContentTransitionAuditDO) null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.publish(
                KEY, snapshot(100L, 200L), snapshot(200L, null),
                "SUPERSEDED", "ACTIVE", 501L, "effective-date switch"));

        assertTrue(exception.getMessage().contains("version ref 12 has no transition audit"));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void publish_whenLatestAuditStatusDrifts_failsBeforeFirstWrite() {
        ControlledContentVersionRefDO active = ref(11L, 100L, ACTIVE.name(), 1, null);
        ControlledContentVersionRefDO candidate = ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1);
        when(versionRefMapper.selectList(any())).thenReturn(List.of(active, candidate));
        when(transitionAuditMapper.selectOne(any())).thenReturn(audit(active),
                ControlledContentTransitionAuditDO.builder()
                        .id(120L)
                        .tenantId(KEY.getTenantId())
                        .versionRefId(candidate.getId())
                        .contentType(KEY.getContentType().name())
                        .contentKey(KEY.getContentKey())
                        .toStatus(ACTIVE.name())
                        .build());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.publish(
                KEY, snapshot(100L, 200L), snapshot(200L, null),
                "SUPERSEDED", "ACTIVE", 501L, "effective-date switch"));

        assertTrue(exception.getMessage().contains(
                "version ref 12 latest audit status ACTIVE does not match READY_TO_PUBLISH"));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void publish_whenTwoActiveRefsExist_failsBeforeFirstWrite() {
        when(versionRefMapper.selectList(any())).thenReturn(List.of(
                ref(11L, 100L, ACTIVE.name(), 1, null),
                ref(12L, 101L, ACTIVE.name(), 1, null),
                ref(13L, 200L, READY_TO_PUBLISH.name(), null, 1)));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.publish(
                KEY, snapshot(100L, 200L), snapshot(200L, null),
                "SUPERSEDED", "ACTIVE", 501L, "effective-date switch"));

        assertTrue(exception.getMessage().contains("active ref count expected at most 1 but was 2"));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void publish_whenTwoOpenCandidateRefsExist_failsBeforeFirstWrite() {
        when(versionRefMapper.selectList(any())).thenReturn(List.of(
                ref(11L, 100L, ACTIVE.name(), 1, null),
                ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1),
                ref(13L, 201L, READY_TO_PUBLISH.name(), null, 1)));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.publish(
                KEY, snapshot(100L, 200L), snapshot(200L, null),
                "SUPERSEDED", "ACTIVE", 501L, "effective-date switch"));

        assertTrue(exception.getMessage().contains("open candidate ref count expected at most 1 but was 2"));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("foreignPlatformSnapshots")
    void registerActive_whenPlatformSnapshotOwnerMismatches_failsBeforeFirstWrite(
            String scenario, ControlledContentProjectionSnapshot platformBefore, String mismatchedField) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.registerActive(KEY, platformBefore, snapshot(300L, null),
                        9001L, 300L, "V1", "ACTIVE", 501L, "initial registration"));

        assertTrue(exception.getMessage().contains("platformBefore " + mismatchedField + " mismatch"));
        verify(versionRefMapper, never()).selectList(any());
        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void registerActive_whenDomainAfterOwnerMismatches_failsBeforeFirstWrite() {
        ControlledContentProjectionSnapshot foreignAfter = new ControlledContentProjectionSnapshot(
                KEY.getTenantId(), KEY.getContentType(), "REG-OTHER", 300L, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.registerActive(KEY, snapshot(null, null), foreignAfter,
                        9001L, 300L, "V1", "ACTIVE", 501L, "initial registration"));

        assertTrue(exception.getMessage().contains("domainAfter contentKey mismatch"));
        verify(versionRefMapper, never()).selectList(any());
        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void registrationActions_whenReadyCandidateOrPublishDeltaIsIllegal_failBeforeFirstWrite() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> service.registerReadyCandidate(KEY, snapshot(100L, null), snapshot(100L, 201L),
                        9001L, 200L, "V2", "READY_TO_PUBLISH", 501L, "renewal"))
                .getMessage().contains("REGISTER_READY_CANDIDATE delta"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> service.publish(KEY, snapshot(100L, 200L), snapshot(100L, null),
                        "SUPERSEDED", "ACTIVE", 501L, "effective-date switch"))
                .getMessage().contains("PUBLISH delta"));
        verify(versionRefMapper, never()).selectList(any());
        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void publish_whenRefBelongsToAnotherProjection_failsBeforeFirstWrite() {
        ControlledContentVersionRefDO foreignActive = ref(11L, 100L, ACTIVE.name(), 1, null);
        foreignActive.setTenantId(999L);
        when(versionRefMapper.selectList(any())).thenReturn(List.of(foreignActive));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.publish(
                KEY, snapshot(100L, 200L), snapshot(200L, null),
                "SUPERSEDED", "ACTIVE", 501L, "effective-date switch"));

        assertTrue(exception.getMessage().contains("version ref 11 belongs to another controlled content"));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void registerActive_whenSnapshotsAreMissingOrDeltaIsContradictory_failsBeforeWrite() {
        assertEquals("platformBefore must not be null", assertThrows(IllegalArgumentException.class,
                () -> service.registerActive(KEY, null, snapshot(300L, null),
                        9001L, 300L, "V1", "ACTIVE", 501L, "initial registration")).getMessage());
        assertEquals("domainAfter must not be null", assertThrows(IllegalArgumentException.class,
                () -> service.registerActive(KEY, snapshot(null, null), null,
                        9001L, 300L, "V1", "ACTIVE", 501L, "initial registration")).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> service.registerActive(KEY, snapshot(null, null), snapshot(300L, 301L),
                        9001L, 300L, "V1", "ACTIVE", 501L, "initial registration"))
                .getMessage().contains("REGISTER_ACTIVE delta"));
        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void registerActive_whenProjectionIsGenuinelyEmpty_createsExactActiveProjection() {
        ControlledContentVersionRefDO persisted = ref(91L, 300L, ACTIVE.name(), 1, null);
        when(versionRefMapper.selectList(any())).thenReturn(List.of(), List.of(persisted));
        when(transitionAuditMapper.selectCount(any())).thenReturn(0L, 1L);
        stubLatestAudits(persisted);
        doAnswer(invocation -> {
            ControlledContentVersionRefDO inserted = invocation.getArgument(0);
            inserted.setId(91L);
            return 1;
        }).when(versionRefMapper).insert(any(ControlledContentVersionRefDO.class));

        ControlledContentVersionRefDO result = assertDoesNotThrow(() -> service.registerActive(
                KEY, snapshot(null, null), snapshot(300L, null),
                9001L, 300L, "V1", "ACTIVE", 501L, "initial registration"));

        assertEquals(300L, result.getNativeVersionId());
        assertEquals(ACTIVE.name(), result.getCanonicalStatus());
        ArgumentCaptor<ControlledContentTransitionAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ControlledContentTransitionAuditDO.class);
        verify(transitionAuditMapper).insert(auditCaptor.capture());
        assertEquals("REGISTER_ACTIVE", auditCaptor.getValue().getAction());
    }

    @Test
    void registerReadyCandidate_whenDeltaMatches_createsReadyCandidateWithoutApproval() {
        ControlledContentVersionRefDO active = ref(11L, 100L, ACTIVE.name(), 1, null);
        ControlledContentVersionRefDO candidate = ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1);
        when(versionRefMapper.selectList(any())).thenReturn(List.of(active), List.of(active, candidate));
        stubLatestAudits(active, active, candidate);
        doAnswer(invocation -> {
            ControlledContentVersionRefDO inserted = invocation.getArgument(0);
            inserted.setId(12L);
            return 1;
        }).when(versionRefMapper).insert(any(ControlledContentVersionRefDO.class));

        ControlledContentVersionRefDO result = assertDoesNotThrow(() -> service.registerReadyCandidate(
                KEY, snapshot(100L, null), snapshot(100L, 200L),
                9001L, 200L, "V2", "READY_TO_PUBLISH", 501L, "formal renewal registered"));

        assertEquals(READY_TO_PUBLISH.name(), result.getCanonicalStatus());
        assertEquals(11L, result.getSourceVersionRefId());
        ArgumentCaptor<ControlledContentTransitionAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ControlledContentTransitionAuditDO.class);
        verify(transitionAuditMapper).insert(auditCaptor.capture());
        assertEquals("REGISTER_READY_CANDIDATE", auditCaptor.getValue().getAction());
    }

    @Test
    void publish_whenDeltaMatches_supersedesActiveAndActivatesCandidate() {
        ControlledContentVersionRefDO active = ref(11L, 100L, ACTIVE.name(), 1, null);
        ControlledContentVersionRefDO candidate = ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1);
        ControlledContentVersionRefDO oldAfter = ref(11L, 100L, SUPERSEDED.name(), null, null);
        ControlledContentVersionRefDO newAfter = ref(12L, 200L, ACTIVE.name(), 1, null);
        when(versionRefMapper.selectList(any())).thenReturn(
                List.of(active, candidate), List.of(oldAfter, newAfter));
        stubLatestAudits(active, candidate, oldAfter, newAfter);

        assertDoesNotThrow(() -> service.publish(KEY, snapshot(100L, 200L), snapshot(200L, null),
                "SUPERSEDED", "ACTIVE", 501L, "effective-date switch"));

        verify(versionRefMapper, times(2)).update(any(), any());
        ArgumentCaptor<ControlledContentTransitionAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ControlledContentTransitionAuditDO.class);
        verify(transitionAuditMapper, times(2)).insert(auditCaptor.capture());
        assertEquals(List.of("SUPERSEDE_ACTIVE", "PUBLISH"), auditCaptor.getAllValues().stream()
                .map(ControlledContentTransitionAuditDO::getAction).toList());
    }

    @Test
    void genericLifecycleMutation_whenRegistrationTypeUsed_rejectsMissingProjectionContract() {
        ControlledContentLifecycleCoreService genericService = new ControlledContentLifecycleCoreService(
                versionRefMapper, transitionAuditMapper, new ControlledContentStateMachine());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> genericService.registerActiveRef(KEY, 9001L, 300L, "V1", "ACTIVE",
                        501L, "must use projection contract"));

        assertTrue(exception.getMessage().contains("require projection snapshots"));
        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    private ControlledContentProjectionSnapshot snapshot(Long activeNativeVersionId, Long candidateNativeVersionId) {
        return ControlledContentProjectionSnapshot.of(KEY, activeNativeVersionId, candidateNativeVersionId);
    }

    private static Stream<Arguments> foreignPlatformSnapshots() {
        return Stream.of(
                Arguments.of("tenant mismatch", new ControlledContentProjectionSnapshot(
                        999L, KEY.getContentType(), KEY.getContentKey(), null, null), "tenantId"),
                Arguments.of("type mismatch", new ControlledContentProjectionSnapshot(
                        KEY.getTenantId(), DCC_CONTROLLED_FILE, KEY.getContentKey(), null, null), "contentType"),
                Arguments.of("key mismatch", new ControlledContentProjectionSnapshot(
                        KEY.getTenantId(), KEY.getContentType(), "REG-OTHER", null, null), "contentKey"));
    }

    private ControlledContentVersionRefDO ref(Long id, Long nativeVersionId, String status,
                                              Integer activeFlag, Integer candidateFlag) {
        return ControlledContentVersionRefDO.builder()
                .id(id)
                .tenantId(KEY.getTenantId())
                .contentType(KEY.getContentType().name())
                .contentKey(KEY.getContentKey())
                .nativeMasterId(9001L)
                .nativeVersionId(nativeVersionId)
                .versionNo("V" + nativeVersionId)
                .canonicalStatus(status)
                .domainStatus(status)
                .activeUniqueFlag(activeFlag)
                .openCandidateUniqueFlag(candidateFlag)
                .build();
    }

    private void stubLatestAudits(ControlledContentVersionRefDO first, ControlledContentVersionRefDO... remaining) {
        ControlledContentTransitionAuditDO[] remainingAudits = java.util.Arrays.stream(remaining)
                .map(this::audit)
                .toArray(ControlledContentTransitionAuditDO[]::new);
        when(transitionAuditMapper.selectOne(any())).thenReturn(audit(first), remainingAudits);
    }

    private ControlledContentTransitionAuditDO audit(ControlledContentVersionRefDO ref) {
        return ControlledContentTransitionAuditDO.builder()
                .id(ref.getId() * 10)
                .tenantId(KEY.getTenantId())
                .versionRefId(ref.getId())
                .contentType(KEY.getContentType().name())
                .contentKey(KEY.getContentKey())
                .toStatus(ref.getCanonicalStatus())
                .build();
    }

}
