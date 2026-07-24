package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DccControlledFileSignatureEvidenceServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private DccControlledFileSignatureEvidenceServiceImpl service;

    @Test
    void createEvidence_usesCanonicalPayloadAndHmacSha256ForNotApplicableControlledCopy() throws Exception {
        configureEvidenceProperties("unit-test-secret", "dcc-signature-2026-05");
        when(controlledFileMapper.selectById(710088L)).thenReturn(DccControlledFileDO.builder()
                .id(710088L)
                .fileNumber("DCC-SOP-001")
                .versionNo("A.1")
                .sourceFileId(1001L)
                .processInstanceId("bpm-pi-8001")
                .build());
        when(fileService.getFile(1001L)).thenReturn(FileDO.builder()
                .id(1001L)
                .configId(7L)
                .path("dcc/source/sop.pdf")
                .build());
        when(fileService.getFileContent(7L, "dcc/source/sop.pdf"))
                .thenReturn("approved source bytes".getBytes(StandardCharsets.UTF_8));

        LocalDateTime signedAt = LocalDateTime.of(2026, 5, 26, 14, 32, 18, 123_000_000);
        DccControlledFileSignatureEvidence evidence = service.createEvidence(
                DccControlledFileSignatureEvidenceCreateReq.builder()
                        .tenantId(1L)
                        .controlledFileId(710088L)
                        .taskId("bpm-task-9001")
                        .taskActionResult("APPROVED")
                        .meaningCode("REVIEW_APPROVE")
                        .signerUserId(101L)
                        .signerUsername("auditor")
                        .signerNickname("审核员")
                        .signerDeptId(20L)
                        .signerDeptName("质量部")
                        .signerPostNames("QA岗位")
                        .signerRoleNames("质量审核员")
                        .signaturePurpose("REVIEW_APPROVE")
                        .authorizationBasis("DCC电子签名授权启用；系统角色/岗位快照已记录")
                        .authenticationMethod("PASSWORD")
                        .signedAt(signedAt)
                        .reasonText("")
                        .controlledCopyHashStatus("NOT_APPLICABLE")
                        .signatureImageId(501L)
                        .signatureImageVersionNo(3)
                        .signatureImageFileId(1501L)
                        .signatureImageSha256("image-sha256")
                        .signatureImageContentType("image/png")
                        .signatureImageFileSize(2048L)
                        .signatureImageStatusSnapshot("ACTIVE")
                        .signatureImageVerifiedStatus("VALID")
                        .build());

        String sourceHash = sha256Hex("approved source bytes".getBytes(StandardCharsets.UTF_8));
        String signedAtText = OffsetDateTime.of(signedAt, ZoneOffset.ofHours(8)).toString();
        String expectedPayload = "{\"payloadVersion\":\"v3-image\",\"hashAlgorithm\":\"HMAC_SHA256\","
                + "\"keyVersion\":\"dcc-signature-2026-05\",\"tenantId\":1,\"controlledFileId\":710088,"
                + "\"fileNumber\":\"DCC-SOP-001\",\"revisionId\":710088,\"versionNo\":\"A.1\","
                + "\"sourceFileHash\":\"" + sourceHash + "\",\"controlledCopyHashStatus\":\"NOT_APPLICABLE\","
                + "\"controlledCopyHash\":\"\",\"signatureImageId\":501,\"signatureImageVersionNo\":3,"
                + "\"signatureImageFileId\":1501,\"signatureImageSha256\":\"image-sha256\","
                + "\"signatureImageContentType\":\"image/png\",\"signatureImageFileSize\":2048,"
                + "\"signatureImageStatusSnapshot\":\"ACTIVE\",\"signatureImageVerifiedStatus\":\"VALID\","
                + "\"processInstanceId\":\"bpm-pi-8001\","
                + "\"taskId\":\"bpm-task-9001\",\"taskActionResult\":\"APPROVED\","
                + "\"meaningCode\":\"REVIEW_APPROVE\",\"signerUserId\":101,\"signerUsername\":\"auditor\","
                + "\"signerNickname\":\"审核员\",\"signerDeptId\":20,\"signerDeptName\":\"质量部\","
                + "\"signerPostNames\":\"QA岗位\",\"signerRoleNames\":\"质量审核员\","
                + "\"signaturePurpose\":\"REVIEW_APPROVE\","
                + "\"authorizationBasis\":\"DCC电子签名授权启用；系统角色/岗位快照已记录\","
                + "\"authenticationMethod\":\"PASSWORD\","
                + "\"signedAt\":\"" + signedAtText + "\",\"reasonText\":\"\"}";

        assertEquals("v3-image", evidence.getEvidencePayloadVersion());
        assertEquals("HMAC_SHA256", evidence.getEvidenceHashAlgorithm());
        assertEquals("VALID", evidence.getEvidenceStatus());
        assertEquals(710088L, evidence.getRevisionId());
        assertEquals("A.1", evidence.getVersionNo());
        assertEquals("A.1", evidence.getRecordVersionSnapshot());
        assertEquals(1001L, evidence.getSourceFileId());
        assertEquals(sourceHash, evidence.getSourceFileHash());
        assertEquals(sourceHash, evidence.getRecordHashSnapshot());
        assertEquals("SHA-256", evidence.getSourceFileHashAlgorithm());
        assertEquals("BOUND", evidence.getSourceFileHashStatus());
        assertEquals("NOT_APPLICABLE", evidence.getControlledCopyHashStatus());
        assertEquals(501L, evidence.getSignatureImageId());
        assertEquals(3, evidence.getSignatureImageVersionNo());
        assertEquals(1501L, evidence.getSignatureImageFileId());
        assertEquals("image-sha256", evidence.getSignatureImageSha256());
        assertEquals("VALID", evidence.getSignatureImageVerifiedStatus());
        assertEquals(expectedPayload, evidence.getCanonicalPayload());
        assertEquals(hmacSha256Hex("unit-test-secret", expectedPayload), evidence.getEvidenceHash());
        assertTrue(evidence.getEvidenceHash().matches("[0-9a-f]{64}"));
    }

    @Test
    void createEvidence_allowsNullSignerDeptIdAndKeepsCanonicalNull() throws Exception {
        configureEvidenceProperties("unit-test-secret", "dcc-signature-2026-05");
        when(controlledFileMapper.selectById(710088L)).thenReturn(DccControlledFileDO.builder()
                .id(710088L)
                .fileNumber("DCC-SOP-001")
                .versionNo("A.1")
                .sourceFileId(1001L)
                .processInstanceId("bpm-pi-8001")
                .build());
        when(fileService.getFile(1001L)).thenReturn(FileDO.builder()
                .id(1001L)
                .configId(7L)
                .path("dcc/source/sop.pdf")
                .build());
        when(fileService.getFileContent(7L, "dcc/source/sop.pdf"))
                .thenReturn("approved source bytes".getBytes(StandardCharsets.UTF_8));

        LocalDateTime signedAt = LocalDateTime.of(2026, 5, 26, 14, 32, 18, 123_000_000);
        DccControlledFileSignatureEvidence evidence = service.createEvidence(
                DccControlledFileSignatureEvidenceCreateReq.builder()
                        .tenantId(1L)
                        .controlledFileId(710088L)
                        .taskId("bpm-task-9001")
                        .taskActionResult("APPROVED")
                        .meaningCode("REVIEW_APPROVE")
                        .signerUserId(101L)
                        .signerUsername("auditor")
                        .signerNickname("审核员")
                        .signerDeptId(null)
                        .signerDeptName(null)
                        .signerPostNames("QA岗位")
                        .signerRoleNames("质量审核员")
                        .signaturePurpose("REVIEW_APPROVE")
                        .authorizationBasis("DCC电子签名授权启用；系统角色/岗位快照已记录")
                        .authenticationMethod("PASSWORD")
                        .signedAt(signedAt)
                        .reasonText("")
                        .controlledCopyHashStatus("NOT_APPLICABLE")
                        .signatureImageId(501L)
                        .signatureImageVersionNo(3)
                        .signatureImageFileId(1501L)
                        .signatureImageSha256("image-sha256")
                        .signatureImageContentType("image/png")
                        .signatureImageFileSize(2048L)
                        .signatureImageStatusSnapshot("ACTIVE")
                        .signatureImageVerifiedStatus("VALID")
                        .build());

        String sourceHash = sha256Hex("approved source bytes".getBytes(StandardCharsets.UTF_8));
        String signedAtText = OffsetDateTime.of(signedAt, ZoneOffset.ofHours(8)).toString();
        String expectedPayload = "{\"payloadVersion\":\"v3-image\",\"hashAlgorithm\":\"HMAC_SHA256\","
                + "\"keyVersion\":\"dcc-signature-2026-05\",\"tenantId\":1,\"controlledFileId\":710088,"
                + "\"fileNumber\":\"DCC-SOP-001\",\"revisionId\":710088,\"versionNo\":\"A.1\","
                + "\"sourceFileHash\":\"" + sourceHash + "\",\"controlledCopyHashStatus\":\"NOT_APPLICABLE\","
                + "\"controlledCopyHash\":\"\",\"signatureImageId\":501,\"signatureImageVersionNo\":3,"
                + "\"signatureImageFileId\":1501,\"signatureImageSha256\":\"image-sha256\","
                + "\"signatureImageContentType\":\"image/png\",\"signatureImageFileSize\":2048,"
                + "\"signatureImageStatusSnapshot\":\"ACTIVE\",\"signatureImageVerifiedStatus\":\"VALID\","
                + "\"processInstanceId\":\"bpm-pi-8001\","
                + "\"taskId\":\"bpm-task-9001\",\"taskActionResult\":\"APPROVED\","
                + "\"meaningCode\":\"REVIEW_APPROVE\",\"signerUserId\":101,\"signerUsername\":\"auditor\","
                + "\"signerNickname\":\"审核员\",\"signerDeptId\":null,\"signerDeptName\":\"\","
                + "\"signerPostNames\":\"QA岗位\",\"signerRoleNames\":\"质量审核员\","
                + "\"signaturePurpose\":\"REVIEW_APPROVE\","
                + "\"authorizationBasis\":\"DCC电子签名授权启用；系统角色/岗位快照已记录\","
                + "\"authenticationMethod\":\"PASSWORD\","
                + "\"signedAt\":\"" + signedAtText + "\",\"reasonText\":\"\"}";

        assertEquals("VALID", evidence.getEvidenceStatus());
        assertEquals(expectedPayload, evidence.getCanonicalPayload());
        assertEquals(hmacSha256Hex("unit-test-secret", expectedPayload), evidence.getEvidenceHash());
    }

    @Test
    void createEvidence_rejectsBoundControlledCopyWithoutFileId() {
        configureEvidenceProperties("unit-test-secret", "dcc-signature-2026-05");

        assertServiceException(() -> service.createEvidence(DccControlledFileSignatureEvidenceCreateReq.builder()
                .tenantId(1L)
                .controlledFileId(710088L)
                .taskId("bpm-task-9001")
                .taskActionResult("APPROVED")
                .meaningCode("ARCHIVE_SEAL")
                .signerUserId(101L)
                .signerDeptId(20L)
                .signedAt(LocalDateTime.of(2026, 5, 26, 14, 32, 18))
                .controlledCopyHashStatus("BOUND")
                .build()), CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
    }

    @Test
    void createEvidence_rejectsMissingHmacConfig() {
        configureEvidenceProperties("", "");

        assertServiceException(() -> service.createEvidence(DccControlledFileSignatureEvidenceCreateReq.builder()
                .tenantId(1L)
                .controlledFileId(710088L)
                .taskId("bpm-task-9001")
                .taskActionResult("APPROVED")
                .meaningCode("REVIEW_APPROVE")
                .signerUserId(101L)
                .signerDeptId(20L)
                .signedAt(LocalDateTime.of(2026, 5, 26, 14, 32, 18))
                .controlledCopyHashStatus("NOT_APPLICABLE")
                .build()), CONTROLLED_FILE_SIGNATURE_CONFIG_MISSING);
    }

    @Test
    void signatureEvidenceProperties_failsFastWhenMandatoryStartupConfigMissing() {
        DccSignatureEvidenceProperties properties = new DccSignatureEvidenceProperties();
        properties.setHmacSecret("unit-test-secret");

        assertServiceException(properties::validateStartupConfig, CONTROLLED_FILE_SIGNATURE_CONFIG_MISSING);
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private void configureEvidenceProperties(String hmacSecret, String keyVersion) {
        DccSignatureEvidenceProperties properties = new DccSignatureEvidenceProperties();
        properties.setHmacSecret(hmacSecret);
        properties.setKeyVersion(keyVersion);
        ReflectionTestUtils.setField(service, "signatureEvidenceProperties", properties);
    }

    private static String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
