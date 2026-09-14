package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateEntrustedEnterprise;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProductionRelation;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftState;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateResolvedDraft;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.formalization.DccRegistrationCertificateFormalizationResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.formalization.DccRegistrationCertificateFormalizationService;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentProjectionSnapshot;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_NOT_STAGED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccRegistrationCertificateFormalizationTest {

    @Mock
    private DccRegistrationCertificateMapper certificateMapper;
    @Mock
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Mock
    private DccRegistrationCertificateFileMapper fileMapper;
    @Mock
    private ControlledContentRegistrationProjectionService projectionService;

    private DccRegistrationCertificateFormalizationService service;

    @BeforeEach
    void setUp() {
        initTableInfo(DccRegistrationCertificateVersionDO.class);
        initTableInfo(DccRegistrationCertificateDO.class);
        initTableInfo(DccRegistrationCertificateFileDO.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new DccRegistrationCertificateFormalizationService(
                certificateMapper, versionMapper, fileMapper, projectionService,
                new DccRegistrationCertificateBusinessClock(clock));
    }

    @Test
    void formalize_currentEffectiveDateBindsTheOwnedFileAndRegistersActive() {
        DccRegistrationCertificateDraftState state = draftState(LocalDate.of(2026, 8, 17));
        when(fileMapper.selectById(5001L)).thenReturn(stagedFile());
        allowFormalWrites();

        DccRegistrationCertificateFormalizationResult result = service.formalize(
                state, matchingResolvedDraft(), 1L, 99L, 1, 5001L);

        assertEquals(1001L, result.certificateId());
        assertEquals(2001L, result.versionId());
        assertEquals(3001L, result.snapshotId());
        assertEquals(5001L, result.businessFileId());
        ArgumentCaptor<ControlledContentProjectionSnapshot> after =
                ArgumentCaptor.forClass(ControlledContentProjectionSnapshot.class);
        verify(projectionService).registerActive(any(), any(), after.capture(),
                eq(1001L), eq(2001L), eq("1"), eq("CURRENT"), eq(99L), anyString());
        assertEquals(2001L, after.getValue().activeNativeVersionId());
        assertNull(after.getValue().openCandidateNativeVersionId());
        verify(projectionService, never()).registerReadyCandidate(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void formalize_futureEffectiveDateRegistersReadyCandidateWithoutEarlyActivation() {
        DccRegistrationCertificateDraftState state = draftState(LocalDate.of(2026, 8, 18));
        when(fileMapper.selectById(5001L)).thenReturn(stagedFile());
        allowFormalWrites();

        service.formalize(state, matchingResolvedDraft(), 1L, 99L, 1, 5001L);

        ArgumentCaptor<ControlledContentProjectionSnapshot> after =
                ArgumentCaptor.forClass(ControlledContentProjectionSnapshot.class);
        verify(projectionService).registerReadyCandidate(any(), any(), after.capture(),
                eq(1001L), eq(2001L), eq("1"), eq("PENDING_EFFECTIVE"), eq(99L), anyString());
        assertNull(after.getValue().activeNativeVersionId());
        assertEquals(2001L, after.getValue().openCandidateNativeVersionId());
        verify(projectionService, never()).registerActive(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void formalizeAutomaticallyUsesTheOnlyStagedRegistrationFileWhenIdIsOmitted() {
        DccRegistrationCertificateDraftState state = draftState(LocalDate.of(2026, 8, 17));
        when(fileMapper.selectList(any())).thenReturn(List.of(stagedFile()));
        allowFormalWrites();

        DccRegistrationCertificateFormalizationResult result = service.formalize(
                state, matchingResolvedDraft(), 1L, 99L, 1, null);

        assertEquals(5001L, result.businessFileId());
        verify(fileMapper).selectList(any());
    }

    @Test
    void formalize_rejectsAFileOwnedByAnotherVersionBeforeAnyFormalWrite() {
        DccRegistrationCertificateFileDO file = stagedFile();
        file.setOwnerId(9999L);
        when(fileMapper.selectById(5001L)).thenReturn(file);

        ServiceException error = assertThrows(ServiceException.class, () -> service.formalize(
                draftState(LocalDate.of(2026, 8, 17)), matchingResolvedDraft(),
                1L, 99L, 1, 5001L));

        assertEquals(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT.getCode(), error.getCode());
        verify(versionMapper, never()).update(any(), any());
        verify(certificateMapper, never()).update(any(), any());
        verify(projectionService, never()).registerActive(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void formalize_rejectsAnAlreadyBoundFileBeforeAnyFormalWrite() {
        DccRegistrationCertificateFileDO file = stagedFile();
        file.setStatus("BOUND");
        when(fileMapper.selectById(5001L)).thenReturn(file);

        ServiceException error = assertThrows(ServiceException.class, () -> service.formalize(
                draftState(LocalDate.of(2026, 8, 17)), matchingResolvedDraft(),
                1L, 99L, 1, 5001L));

        assertEquals(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED.getCode(), error.getCode());
        verify(versionMapper, never()).update(any(), any());
        verify(fileMapper, never()).update(any(), any());
    }

    @Test
    void formalize_rejectsProjectionDriftBeforeLookingUpTheFile() {
        DccRegistrationCertificateResolvedDraft drifted = new DccRegistrationCertificateResolvedDraft(
                "other product", matchingResolvedDraft().productionRelation());

        ServiceException error = assertThrows(ServiceException.class, () -> service.formalize(
                draftState(LocalDate.of(2026, 8, 17)), drifted, 1L, 99L, 1, 5001L));

        assertEquals(REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH.getCode(), error.getCode());
        verify(fileMapper, never()).selectById(any());
        verify(versionMapper, never()).update(any(), any());
    }

    @Test
    void formalize_surfacesProjectionFailureAsStableConflictForTransactionRollback() {
        DccRegistrationCertificateDraftState state = draftState(LocalDate.of(2026, 8, 17));
        when(fileMapper.selectById(5001L)).thenReturn(stagedFile());
        allowFormalWrites();
        IllegalStateException drift = new IllegalStateException("platform projection drift");
        when(projectionService.registerActive(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(drift);

        ServiceException error = assertThrows(ServiceException.class, () -> service.formalize(
                state, matchingResolvedDraft(), 1L, 99L, 1, 5001L));

        assertEquals(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT.getCode(), error.getCode());
        assertNotNull(error.getCause());
        assertSame(drift, error.getCause());
    }

    private void allowFormalWrites() {
        when(versionMapper.update(any(), any())).thenReturn(1);
        when(certificateMapper.update(any(), any())).thenReturn(1);
        when(fileMapper.update(any(), any())).thenReturn(1);
    }

    private static DccRegistrationCertificateDraftState draftState(LocalDate effectiveDate) {
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .id(1001L).ownerCompanyId(10L).productMasterId(20L).status("DRAFT").rowVersion(1).build();
        certificate.setTenantId(1L);
        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .id(2001L).certificateId(1001L).versionNo(1).versionType("INITIAL")
                .effectiveDate(effectiveDate).status("DRAFT").build();
        version.setTenantId(1L);
        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .id(3001L).versionId(2001L).revisionNo(1).productName("Product A")
                .entrustedProduction(true).selfProduction(false).build();
        snapshot.setTenantId(1L);
        DccRegistrationCertificateSnapshotEntrustedDO entrusted =
                DccRegistrationCertificateSnapshotEntrustedDO.builder()
                        .id(4001L).snapshotId(3001L).enterpriseId(30L)
                        .enterpriseNameSnapshot("Factory A").sortOrder(0).build();
        entrusted.setTenantId(1L);
        return new DccRegistrationCertificateDraftState(certificate, version, snapshot, List.of(entrusted));
    }

    private static DccRegistrationCertificateResolvedDraft matchingResolvedDraft() {
        return new DccRegistrationCertificateResolvedDraft("Product A",
                new DccRegistrationCertificateProductionRelation(true, false,
                        List.of(new DccRegistrationCertificateEntrustedEnterprise(30L, "Factory A"))));
    }

    private static DccRegistrationCertificateFileDO stagedFile() {
        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .id(5001L).ownerType("VERSION").ownerId(2001L)
                .fileKind("REGISTRATION_CERTIFICATE").status("STAGED").build();
        file.setTenantId(1L);
        return file;
    }

    private static void initTableInfo(Class<?> entityType) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityType);
    }
}
