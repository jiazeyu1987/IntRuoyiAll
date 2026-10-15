package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual canonical creation and management reconstruction/HMAC. File-byte loading is an explicit port. */
class DccDelayedEffectiveHmacTest {
    private DccControlledFileSignatureEvidenceServiceImpl creator;
    private DccControlledFileSignatureEvidenceCreateReq request;
    private DccControlledFileDO file;
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);creator=new DccControlledFileSignatureEvidenceServiceImpl();
        var properties=new DccSignatureEvidenceProperties();properties.setHmacSecret("task-owned-isolated-hmac-key");properties.setKeyVersion("G01-unit");
        ReflectionTestUtils.setField(creator,"signatureEvidenceProperties",properties);
        var files=mock(DccControlledFileMapper.class);file=DccControlledFileDO.builder().id(20L).sourceFileId(110L).fileNumber("SOP-01").versionNo("B/1").processInstanceId("R").build();
        when(files.selectById(20L)).thenReturn(file);ReflectionTestUtils.setField(creator,"controlledFileMapper",files);
        var bytes=mock(FileService.class);when(bytes.getFile(110L)).thenReturn(FileDO.builder().id(110L).configId(7L).path("g01-source.pdf").build());
        when(bytes.getFileContent(7L,"g01-source.pdf")).thenReturn(new byte[]{1,2,3});ReflectionTestUtils.setField(creator,"fileService",bytes);
        request=DccControlledFileSignatureEvidenceCreateReq.builder().tenantId(1L).controlledFileId(20L).taskId("T").processInstanceId("R")
                .taskActionResult("APPROVED").meaningCode("DELAYED_EFFECTIVE_CONFIRM_APPROVE").signerUserId(99L).signerUsername("compiler").signerNickname("编制人")
                .signerDeptId(7L).signerDeptName("研发").signerPostNames("编制岗位").signerRoleNames("普通用户").signaturePurpose("延迟生效确认")
                .authorizationBasis("原编制人正式电子签名授权").authenticationMethod("PASSWORD").signedAt(LocalDateTime.of(2026,10,15,12,0,0,123456789))
                .reasonText("真实衔接确认").selectedEffectiveTime(LocalDateTime.of(2026,10,16,12,0)).attributeRound(2).controlledCopyHashStatus("NOT_APPLICABLE")
                .signatureImageId(501L).signatureImageVersionNo(1).signatureImageFileId(1501L).signatureImageSha256("image-sha")
                .signatureImageContentType("image/png").signatureImageFileSize(50L).signatureImageStatusSnapshot("ACTIVE").signatureImageVerifiedStatus("VALID").build();
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void selectedTimeAndRoundAreActualHmacInputs(){
        var original=creator.createEvidence(request);assertEquals("v5-delayed-effective",original.getEvidencePayloadVersion());
        assertEquals(request.getSelectedEffectiveTime(),original.getSelectedEffectiveTime());assertEquals(2,original.getAttributeRound());
        request.setSelectedEffectiveTime(request.getSelectedEffectiveTime().plusSeconds(1));assertNotEquals(original.getEvidenceHash(),creator.createEvidence(request).getEvidenceHash());
        request.setSelectedEffectiveTime(original.getSelectedEffectiveTime());request.setAttributeRound(3);assertNotEquals(original.getEvidenceHash(),creator.createEvidence(request).getEvidenceHash());
    }
    @Test void missingRoundOrFractionalSelectedTimeIsRejected(){request.setAttributeRound(null);assertThrows(RuntimeException.class,()->creator.createEvidence(request));request.setAttributeRound(2);request.setSelectedEffectiveTime(request.getSelectedEffectiveTime().withNano(1));assertThrows(RuntimeException.class,()->creator.createEvidence(request));}
    @Test void ordinaryV4CanonicalRemainsFreeOfNewFields(){request.setMeaningCode("DOC_CONTROL_REVIEW_APPROVE");request.setSelectedEffectiveTime(null);request.setAttributeRound(null);var evidence=creator.createEvidence(request);assertEquals("v4-workflow",evidence.getEvidencePayloadVersion());assertFalse(evidence.getCanonicalPayload().contains("selectedEffectiveTime"));assertFalse(evidence.getCanonicalPayload().contains("attributeRound"));}
    @Test void actualSubjectEncoderDecoderAndLoadPreserveNewRoundAndHashChoiceWithoutChangingFourApprovalOrder(){
        request.setSignedAt(null);file.setRequesterId(99L);file.setTenantId(1L);var evidence=creator.createDelayedSigningIntent(request);String subject=DccControlledFileSignatureSubjectAdapter.encodeSubjectId(20L,"T","DELAYED_EFFECTIVE_CONFIRM","APPROVE",request.getMeaningCode(),evidence);
        var adapter=new DccControlledFileSignatureSubjectAdapter();ReflectionTestUtils.setField(adapter,"signatureEvidenceService",creator);cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot snapshot;
        try(var scope=DccControlledFileSignatureSubjectAdapter.openDelayedIntent(evidence)){snapshot=adapter.loadAndAuthorize(new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(99L,"DCC","APPROVE","DCC_CONTROLLED_FILE",subject,DccControlledFileSignatureSubjectAdapter.subjectVersion(subject),request.getReasonText()));}
        assertEquals("R",snapshot.processInstanceId());assertEquals("T",snapshot.taskId());assertEquals(35,snapshot.nodeOrder());assertEquals("DELAYED_EFFECTIVE_CONFIRM",snapshot.nodeCode());
        request.setSelectedEffectiveTime(request.getSelectedEffectiveTime().plusSeconds(1));var changed=creator.createDelayedSigningIntent(request);
        assertNotEquals(subject,DccControlledFileSignatureSubjectAdapter.encodeSubjectId(20L,"T","DELAYED_EFFECTIVE_CONFIRM","APPROVE",request.getMeaningCode(),changed));
        assertThrows(IllegalArgumentException.class,()->adapter.loadAndAuthorize(new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(99L,"DCC","APPROVE","DCC_CONTROLLED_FILE",subject,"forged",request.getReasonText())));
    }
    @Test void actualSubjectFitsExisting2048SchemaWithoutTruncation() throws Exception {
        request.setSignedAt(null);file.setRequesterId(99L);file.setTenantId(1L);request.setReasonText("衔接依据".repeat(120));request.setSignerRoleNames("正式普通上传账号角色".repeat(20));
        var evidence=creator.createDelayedSigningIntent(request);String encoded=DccControlledFileSignatureSubjectAdapter.encodeSubjectId(20L,"T","DELAYED_EFFECTIVE_CONFIRM","APPROVE",request.getMeaningCode(),evidence);
        System.out.println("G01_SUBJECT_CAPACITY characters="+encoded.length()+" utf8Bytes="+encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:g01_capacity_"+java.util.UUID.randomUUID()+";MODE=MYSQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE system_electronic_signature(subject_id VARCHAR(2048) NOT NULL,reason VARCHAR(500) NOT NULL)");
        assertDoesNotThrow(()->jdbc.update("INSERT INTO system_electronic_signature(subject_id,reason) VALUES (?,?)",encoded,request.getReasonText()));
        assertEquals(encoded,jdbc.queryForObject("SELECT subject_id FROM system_electronic_signature",String.class));
    }
    @Test void appendedIntentTamperingCannotChangeChoiceWhileKeepingAuthorizedClaims(){
        request.setSignedAt(null);file.setRequesterId(99L);file.setTenantId(1L);
        var intent=creator.createDelayedSigningIntent(request);String subject=DccControlledFileSignatureSubjectAdapter.encodeSubjectId(20L,"T","DELAYED_EFFECTIVE_CONFIRM","APPROVE",request.getMeaningCode(),intent);
        var adapter=new DccControlledFileSignatureSubjectAdapter();ReflectionTestUtils.setField(adapter,"signatureEvidenceService",creator);
        String wire=new String(java.util.Base64.getUrlDecoder().decode(subject),java.nio.charset.StandardCharsets.UTF_8);String[] lines=wire.split("\n",-1);
        assertThrows(RuntimeException.class,()->adapter.loadAndAuthorize(new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(99L,"DCC","APPROVE","DCC_CONTROLLED_FILE",subject,DccControlledFileSignatureSubjectAdapter.subjectVersion(subject),request.getReasonText())));
        for(String changed:java.util.List.of("selectedEffectiveTime","attributeRound")){
            var original=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(intent.getCanonicalPayload(),java.util.Map.class);
            original.put(changed,changed.equals("attributeRound")?3:((Number)original.get(changed)).longValue()+1000);
            String fake=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString((wire+"\n"+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(original).getBytes(java.nio.charset.StandardCharsets.UTF_8))).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try(var scope=DccControlledFileSignatureSubjectAdapter.openDelayedIntent(intent)){assertThrows(RuntimeException.class,()->adapter.loadAndAuthorize(new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(99L,"DCC","APPROVE","DCC_CONTROLLED_FILE",fake,DccControlledFileSignatureSubjectAdapter.subjectVersion(fake),request.getReasonText())));}
        }
    }
    @Test void actualManagementReconstructionVerifiesImmutableTimeAndRejectsTampering() throws Exception {
        var evidence=creator.createEvidence(request);var s=DccControlledFileSignatureDO.builder().id(700L).controlledFileId(20L).revisionId(20L)
                .versionNo(file.getVersionNo()).fileNumberSnapshot(file.getFileNumber()).processInstanceId("R").taskId("T").actionType("APPROVE")
                .meaningCode(request.getMeaningCode()).actorId(99L).actorUsernameSnapshot(request.getSignerUsername()).actorNicknameSnapshot(request.getSignerNickname())
                .actorDeptIdSnapshot(request.getSignerDeptId()).actorDeptNameSnapshot(request.getSignerDeptName()).actorPostNamesSnapshot(request.getSignerPostNames())
                .actorRoleNamesSnapshot(request.getSignerRoleNames()).signaturePurpose(request.getSignaturePurpose()).authorizationBasis(request.getAuthorizationBasis())
                .authenticationMethod("PASSWORD").signedAt(request.getSignedAt()).comment(request.getReasonText()).sourceFileId(110L).sourceFileHash(evidence.getSourceFileHash())
                .sourceFileHashAlgorithm("SHA-256").sourceFileHashStatus("BOUND").controlledCopyHashStatus("NOT_APPLICABLE")
                .selectedEffectiveTime(request.getSelectedEffectiveTime()).attributeRound(2).signatureImageId(501L).signatureImageVersionNo(1).signatureImageFileId(1501L)
                .signatureImageSha256("image-sha").signatureImageContentType("image/png").signatureImageFileSize(50L).signatureImageStatusSnapshot("ACTIVE").signatureImageVerifiedStatus("VALID")
                .evidencePayloadVersion("v5-delayed-effective").evidenceHashAlgorithm("HMAC_SHA256").evidenceKeyVersion("G01-unit").evidenceStatus("VALID").evidenceHash(evidence.getEvidenceHash()).build();
        var verifier=new DccElectronicSignatureManagementServiceImpl();ReflectionTestUtils.setField(verifier,"signatureEvidenceProperties",ReflectionTestUtils.getField(creator,"signatureEvidenceProperties"));
        ReflectionTestUtils.setField(verifier,"fileService",ReflectionTestUtils.getField(creator,"fileService"));
        var images=mock(DccElectronicSignatureImageService.class);when(images.verifySignatureSnapshot(s)).thenReturn(DccElectronicSignatureImageSnapshot.builder().content(new byte[]{7}).build());
        ReflectionTestUtils.setField(verifier,"signatureImageService",images);
        String rebuilt=ReflectionTestUtils.invokeMethod(verifier,"buildCanonicalPayload",s,file);assertEquals(evidence.getCanonicalPayload(),rebuilt);
        Object valid=ReflectionTestUtils.invokeMethod(verifier,"verifySignature",s,file,false);assertEquals("VALID",ReflectionTestUtils.invokeMethod(valid,"verificationStatus"));
        s.setSelectedEffectiveTime(s.getSelectedEffectiveTime().plusSeconds(1));Object changed=ReflectionTestUtils.invokeMethod(verifier,"verifySignature",s,file,false);assertEquals("INVALID",ReflectionTestUtils.invokeMethod(changed,"verificationStatus"));
        s.setSelectedEffectiveTime(request.getSelectedEffectiveTime());s.setAttributeRound(3);changed=ReflectionTestUtils.invokeMethod(verifier,"verifySignature",s,file,false);assertEquals("INVALID",ReflectionTestUtils.invokeMethod(changed,"verificationStatus"));
    }
}
