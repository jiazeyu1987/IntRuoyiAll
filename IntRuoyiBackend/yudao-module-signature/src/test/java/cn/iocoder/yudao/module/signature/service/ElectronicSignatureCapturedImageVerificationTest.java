package cn.iocoder.yudao.module.signature.service;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ElectronicSignatureCapturedImageVerificationTest {
    /** Exercise the real V2 writer and reader, including image bytes in both digests. */
    @ParameterizedTest
    @ValueSource(strings = {"VALID", "VERIFIED", "INVALID"})
    void onlyTheImageProvidersValidStatusPasses(String imageStatus) {
        TenantContextHolder.setTenantId(1L);
        try {
            var records = mock(ElectronicSignatureRecordMapper.class);
            var audit = mock(GxpAuditService.class);
            var authentication = mock(SignatureAuthenticationApi.class);
            var signer = new AuthenticatedSigner(1L, "SYSTEM_USER", 2L, 2L, "ce75d9a0-9194-4bcb-a26e-ef302b9bc956", 1L, "request");
            when(authentication.identityDomain()).thenReturn("SYSTEM_USER");
            when(authentication.lockAndRevalidate(eq(signer), any())).thenReturn(signer);
            when(records.insert(any(ElectronicSignatureRecordDO.class))).thenAnswer(call -> {
                ElectronicSignatureRecordDO row = call.getArgument(0);
                row.setId(10L);
                when(records.selectById(10L)).thenReturn(row);
                return 1;
            });
            var writer = new ElectronicSignatureTransactionExecutor(records, audit, List.of(authentication),
                    Clock.fixed(Instant.parse("2026-10-24T07:00:00Z"), ZoneOffset.UTC));
            var command = new ElectronicSignatureCommand("BPM", "APPROVE", "BPM_APPROVAL_TASK",
                    "EDHR_RELEASE:271", "1", null, "test approval", "test-image", null, null);
            var image = Map.of("state", "CAPTURED", "snapshot", Map.of(
                    "imageStatus", "ACTIVE", "verifiedStatus", imageStatus, "imageId", "7",
                    "versionNo", "1", "fileId", "8", "fileUrl", "/test.png", "sha256", "a".repeat(64),
                    "contentType", "image/png", "fileSize", "123"));
            writer.executeAuthenticated(new AuthenticatedSignatureRequest(command,
                    new SignatureSubjectSnapshot(command.subjectType(), command.subjectId(), "1", "{}",
                            null, null, null, null, "2830", null, null),
                    new SignatureActionDefinition("BPM", "APPROVE", command.subjectType(), "APPROVE", "approve", "p1"),
                    JsonUtils.parseTree("{}"), "test account", JsonUtils.parseTree(JsonUtils.toJsonString(image))), signer);
            var creation = mock(SignatureCreationVerificationService.class);
            when(creation.verifySignatureCreation(any())).thenReturn(new SignatureCreationVerification("CONSISTENT", List.of(), 20L));
            var verifier = new ElectronicSignatureVerifiedFactService(records, creation);
            if ("VALID".equals(imageStatus)) {
                assertEquals("VALID", verifier.requireVerifiedFact(10L).verification().verificationStatus());
            } else {
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                        () -> verifier.requireVerifiedFact(10L));
            }
        } finally {
            TenantContextHolder.clear();
        }
    }
}
