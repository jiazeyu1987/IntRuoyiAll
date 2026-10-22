package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientAckReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureBindingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.*;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.service.dept.*;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real kernel, HMAC evidence, domain projection, receipt, GxP and Mapper transactions on existing H2 schemas. */
class DccDistributionSignatureTransactionTest extends BaseDbUnitTest {
    @DynamicPropertySource static void existingFixtures(DynamicPropertyRegistry registry) {
        Path backend=existingBackendRoot(Path.of(System.getProperty("user.dir")));
        registry.add("spring.sql.init.schema-locations",()->String.join(",","classpath:/sql/create_tables.sql",
                backend.resolve("yudao-module-system/src/test/resources/sql/create_tables.sql").toUri().toString(),
                backend.resolve("yudao-module-signature/src/test/resources/sql/create_tables.sql").toUri().toString()));
        // Existing schemas use identity columns. This is the test's explicit database mapping, not a generated fake ID.
        registry.add("mybatis-plus.global-config.db-config.id-type",()->"AUTO");
    }
    static Path existingBackendRoot(Path workingDirectory) {
        for(Path current=workingDirectory.toAbsolutePath().normalize();current!=null;current=current.getParent()) {
            Path candidate=current.getFileName()!=null && "IntRuoyiBackend".equals(current.getFileName().toString())
                    ? current : current.resolve("IntRuoyiBackend");
            if(java.nio.file.Files.isRegularFile(candidate.resolve("pom.xml"))
                    && java.nio.file.Files.isRegularFile(candidate.resolve("yudao-module-dcc/pom.xml"))
                    && java.nio.file.Files.isRegularFile(candidate.resolve("yudao-module-signature/src/test/resources/sql/create_tables.sql"))
                    && java.nio.file.Files.isRegularFile(candidate.resolve("yudao-module-system/src/test/resources/sql/create_tables.sql")))
                return candidate;
        }
        throw new IllegalStateException("W07 original DCC/system/signature fixture repository root is missing");
    }
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager tx;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileDistributionMapper distributions;
    @Resource DccControlledFileDistributionRecipientMapper recipients;
    @Resource DccControlledFileSignatureMapper projections;
    @Resource DccControlledFileSignatureBindingMapper bindingMapper;
    @Resource ElectronicSignatureRecordMapper signatures;
    @Resource GxpAuditEventMapper audits;
    @Resource GxpAuditLedgerSequenceMapper sequences;
    @Resource GxpAuditPolicyOperationMapper policies;
    JdbcTemplate jdbc;
    AdminUserApi users;
    DccSignatureVerificationServiceImpl bridge;
    ElectronicSignatureServiceImpl kernel;
    DccDistributionReceiptServiceImpl receipt;
    DccControlledFileSignatureBindingService bindingService;
    FileService storage;
    byte[] publishedPdfBytes;
    final LocalDateTime now=LocalDateTime.of(2026,10,11,9,0);

    @BeforeEach void fixture() throws Exception {
        try(var connection=dataSource.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        // The shared base fixture clears DCC business rows, while bindings also retain old approval fixtures.
        // This class already requires its isolated in-memory H2; only its own file identity is cleaned.
        jdbc.update("DELETE FROM dcc_controlled_file_signature_binding WHERE controlled_file_id=42");
        var login=new LoginUser().setId(99L).setTenantId(1L).setUserType(2).setInfo(Map.of("username","receiver",LoginUser.INFO_KEY_NICKNAME,"接收人"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null));
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,
                  file_name,title,file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted,
                  published_file_id,stamped_file_id,effective_date,process_instance_id,process_definition_key,
                  controlled_time,activated_time,distributed_time)
                VALUES(42,10,10,20,100,100,'SOP.pdf','SOP','SOP-42','B/1','ACTIVE',99,99,1,0,
                  200,200,'2026-10-11','round-42','dcc-controlled-file-upload',?,?,?)
                """,now,now,now);
        jdbc.update("INSERT INTO dcc_controlled_file_distribution(id,controlled_file_id,department_id,distribution_medium,status,tenant_id,deleted) VALUES(51,42,61,'PUBLIC_FOLDER','SENT',1,0)");
        jdbc.update("INSERT INTO dcc_controlled_file_distribution_recipient(id,distribution_id,user_id,tenant_id,deleted) VALUES(71,51,99,1,0)");
        jdbc.update("INSERT INTO dcc_controlled_file_message_job(id,business_type,business_id,template_code,recipient_user_id,status,sent_at,tenant_id,deleted) VALUES(81,'DISTRIBUTION',51,'dcc_distribution',99,'SENT',?,1,0)",now);
        jdbc.update("UPDATE dcc_controlled_file_distribution_recipient SET message_job_id=81 WHERE id=71");
        users=mock(AdminUserApi.class);
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setUsername("receiver").setNickname("接收人").setStatus(0));
        doAnswer(call->{if(!"fixture-valid-password".equals(call.getArgument(1)))throw new IllegalArgumentException("fixture authentication rejected");return null;})
                .when(users).reauthenticateForSignature(eq(99L),anyString());
        var audit=new GxpAuditServiceImpl();
        wire(audit,"auditEventMapper",audits,"ledgerSequenceMapper",sequences,"policyOperationMapper",policies,"adminUserApi",users);
        kernel=new ElectronicSignatureServiceImpl();
        wire(kernel,"adminUserApi",users,"signatureRecordMapper",signatures,"gxpAuditService",audit,"subjectAdapters",List.of(new DccControlledFileSignatureSubjectAdapter()));
        var evidence=new DccControlledFileSignatureEvidenceServiceImpl();
        var properties=new DccSignatureEvidenceProperties();properties.setHmacSecret("w07-fixture-hmac-key");properties.setKeyVersion("w07-fixture-key-v1");
        storage=mock(FileService.class);var sourceFile=new FileDO();sourceFile.setId(100L);sourceFile.setConfigId(1L);sourceFile.setPath("w07-fixture-source.pdf");
        when(storage.getFile(100L)).thenReturn(sourceFile);when(storage.getFileContent(1L,"w07-fixture-source.pdf")).thenReturn("W07 exact source content".getBytes(StandardCharsets.UTF_8));
        try(var pdf=new org.apache.pdfbox.pdmodel.PDDocument();var bytes=new java.io.ByteArrayOutputStream()) {
            var page=new org.apache.pdfbox.pdmodel.PDPage();pdf.addPage(page);
            try(var content=new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf,page)){content.beginText();content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA,12);content.newLineAtOffset(50,700);content.showText("W07 immutable published controlled copy");content.endText();}
            pdf.save(bytes);publishedPdfBytes=bytes.toByteArray();
        }
        when(storage.getFile(200L)).thenReturn(FileDO.builder().id(200L).configId(1L).path("w07-published-controlled.pdf").type("application/pdf").size((long)publishedPdfBytes.length).build());
        when(storage.getFileContent(1L,"w07-published-controlled.pdf")).thenReturn(publishedPdfBytes);
        wire(evidence,"controlledFileMapper",files,"fileService",storage,"signatureEvidenceProperties",properties);
        var accounts=mock(AdminUserService.class);when(accounts.getUser(99L)).thenReturn(AdminUserDO.builder().id(99L).username("receiver").nickname("接收人").deptId(61L).postIds(Set.of(30L)).build());
        var dept=mock(DeptService.class);var deptRow=new DeptDO();deptRow.setId(61L);deptRow.setName("质量部");when(dept.getDept(61L)).thenReturn(deptRow);
        var posts=mock(PostService.class);var postRow=new PostDO();postRow.setId(30L);postRow.setName("受控文件使用岗位");when(posts.getPostList(Set.of(30L))).thenReturn(List.of(postRow));
        var permissions=mock(PermissionService.class);when(permissions.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(40L));
        var roles=mock(RoleService.class);var roleRow=new RoleDO();roleRow.setId(40L);roleRow.setName("文件接收人");when(roles.getRoleList(Set.of(40L))).thenReturn(List.of(roleRow));
        var image=mock(DccElectronicSignatureImageService.class);when(image.requireActiveSnapshot(99L)).thenReturn(DccElectronicSignatureImageSnapshot.builder()
                .imageId(501L).versionNo(1).fileId(1501L).fileName("signature.png").contentType("image/png").fileSize(10L)
                .sha256("fixture-image-hash").imageStatus("ACTIVE").verifiedStatus("VALID").build());
        bridge=new DccSignatureVerificationServiceImpl();
        wire(bridge,"adminUserService",accounts,"deptService",dept,"postService",posts,"permissionService",permissions,"roleService",roles,
                "electronicSignatureAuthorizationService",mock(DccElectronicSignatureAuthorizationService.class),"signatureImageService",image,
                "signatureEvidenceService",evidence,"signatureMapper",projections,"electronicSignatureService",kernel);
        receipt=new DccDistributionReceiptServiceImpl();
        bindingService=new DccControlledFileSignatureBindingService();
        wire(bindingService,"bindingMapper",bindingMapper,"signatureMapper",projections,"fileService",storage,"controlledFileMapper",files,
                "distributionMapper",distributions,"distributionRecipientMapper",recipients,"messageJobMapper",jobMapper,"gxpAuditService",audit);
        wire(receipt,"controlledFileMapper",files,"distributionMapper",distributions,"distributionRecipientMapper",recipients,"signatureVerificationService",bridge,
                "adminUserApi",users,"messageJobMapper",jobMapper,"gxpAuditService",audit,"signatureBindingService",bindingService);
        insertExplicitFixturePolicy("signature.record.create","REQUIRED_CATEGORY_AND_TEXT");
        insertExplicitFixturePolicy("signature.dcc.distribution-receipt.create","OPTIONAL_TEXT");
        insertExplicitFixturePolicy("dcc.distribution.recipient.signature-bind","REQUIRED_CATEGORY_AND_TEXT");
    }
    void insertExplicitFixturePolicy(String operationId,String reasonPolicy){
        var policy=new GxpAuditPolicyOperationDO();policy.setTenantId(1L);policy.setPolicyVersion("w07-ic06-test-policy");
        policy.setOperationId(operationId);policy.setSourceType("SERVICE_METHOD");policy.setSourceLocator("cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl#sign");
        policy.setDomain("SIGNATURE");policy.setSubjectType("SIGNATURE_RECORD");policy.setActionType("CREATE");policy.setReasonPolicy(reasonPolicy);
        policy.setSignaturePolicy(operationId.equals("dcc.distribution.recipient.body-opened")?"NOT_REQUIRED":"REQUIRED");policy.setStatePolicy("PRESENT_TO_PRESENT");policy.setRetentionClass("GXP_ELECTRONIC_SIGNATURE");
        policy.setTestIds("W07-IC06");policy.setOwner("w07-fixture");policy.setApplicability("GXP");policy.setActive(true);policies.insert(policy);
    }
    @AfterEach void clear(){
        SecurityContextHolder.clearContext();
        if(jdbc!=null)jdbc.update("DELETE FROM dcc_controlled_file_signature_binding WHERE controlled_file_id=42");
        if(jdbc!=null)for(String table:List.of("system_electronic_signature","gxp_audit_event","gxp_audit_ledger_sequence","gxp_audit_policy_operation"))jdbc.update("DELETE FROM "+table);
        TenantContextHolder.clear();
    }
    static void wire(Object bean,Object... fields){for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(bean,(String)fields[i],fields[i+1]);}
    void acknowledge(String comment,String password){
        var request=new DccControlledFileDistributionRecipientAckReqVO();request.setPassword(password);request.setComment(comment);
        new TransactionTemplate(tx).executeWithoutResult(ignored->receipt.acknowledgeElectronicDistribution(99L,42L,51L,71L,request));
    }
    @Test void emptyCommentSignsThroughActualKernelProjectionReceiptAndAuditInOneTransaction(){
        acknowledge(null,"fixture-valid-password");
        assertEquals(1L,signatures.selectCount());assertEquals(1L,projections.selectCount());assertEquals(2L,audits.selectCount());assertEquals(1L,bindingMapper.selectCount());
        var signed=projections.selectActionSignature(42L,"DISTRIBUTION:51:71",99L,"DISTRIBUTION_ACK");
        assertNotNull(signed.getId());assertEquals("",signed.getComment());assertEquals("DISTRIBUTION_ACK",signed.getMeaningCode());
        assertEquals(signed.getSignedAt(),recipients.selectById(71L).getAcknowledgedAt());assertNull(recipients.selectById(71L).getReadAt());
        assertEquals("",signatures.selectList().get(0).getReason());assertEquals("HMAC_SHA256",signed.getEvidenceHashAlgorithm());
        assertTrue(bindingService.verifyPublishedCopyBinding(signed,files.selectById(42L)).valid());
        var event=audits.selectList().stream().filter(row->row.getOperationId().equals("signature.dcc.distribution-receipt.create")).findFirst().orElseThrow();assertTrue(event.getSubjectId().startsWith("DCC_CONTROLLED_FILE:SHA256:"));
        assertEquals("signature.dcc.distribution-receipt.create",event.getOperationId());
        assertTrue(event.getSubjectId().length()<256);assertTrue(event.getAfterStateJson().contains(signatures.selectList().get(0).getSubjectId()));
        assertTrue(event.getIdempotencyKey().startsWith("DCC_RECEIPT_SIGNATURE:"));assertTrue(event.getIdempotencyKey().length()<128);
        var query=new ElectronicSignatureQueryServiceImpl();wire(query,"signatureRecordMapper",signatures);
        assertEquals("VALID",query.verifyEvidence(signatures.selectList().get(0).getId()).verificationStatus());
        acknowledge("","fixture-valid-password");assertEquals(1L,signatures.selectCount());assertEquals(2L,audits.selectCount());assertEquals(1L,bindingMapper.selectCount());
    }
    @Test void optionalEmptyStringAlsoPersistsNoInventedOpinion(){
        acknowledge("","fixture-valid-password");assertEquals("",signatures.selectList().get(0).getReason());assertEquals("",projections.selectList().get(0).getComment());
    }
    @Test void optionalWhitespaceAlsoPersistsNoInventedOpinion(){
        acknowledge("  ","fixture-valid-password");assertEquals("",signatures.selectList().get(0).getReason());assertEquals("",projections.selectList().get(0).getComment());
    }
    @Test void optionalReasonNormalizationReplaysSameKernelRecordAndChangedOpinionConflicts(){
        acknowledge(null,"fixture-valid-password");var stored=signatures.selectList().get(0);
        var emptyReplay=new ElectronicSignatureCommand(stored.getModuleCode(),stored.getActionCode(),stored.getSubjectType(),stored.getSubjectId(),
                stored.getSubjectVersion(),"fixture-valid-password","  ",stored.getIdempotencyKey(),null,null);
        var replay=new TransactionTemplate(tx).execute(ignored->kernel.sign(emptyReplay));
        assertEquals(stored.getId(),replay.signatureId());assertEquals(1L,signatures.selectCount());assertEquals(2L,audits.selectCount());
        var changed=new ElectronicSignatureCommand(stored.getModuleCode(),stored.getActionCode(),stored.getSubjectType(),stored.getSubjectId(),
                stored.getSubjectVersion(),"fixture-valid-password","实际补充意见",stored.getIdempotencyKey(),null,null);
        assertThrows(ServiceException.class,()->new TransactionTemplate(tx).execute(ignored->kernel.sign(changed)));
        assertEquals(1L,signatures.selectCount());assertEquals("",signatures.selectById(stored.getId()).getReason());
    }
    @Test void requiredCurrentAuditPolicyIsNotBypassedAndRollsBackKernelSignature(){
        jdbc.update("UPDATE gxp_audit_policy_operation SET reason_policy='REQUIRED_CATEGORY_AND_TEXT' WHERE operation_id='signature.dcc.distribution-receipt.create'");
        assertThrows(ServiceException.class,()->acknowledge(null,"fixture-valid-password"));
        assertEquals(0L,signatures.selectCount());assertEquals(0L,projections.selectCount());assertEquals(0L,audits.selectCount());assertNull(recipients.selectById(71L).getAcknowledgedAt());
    }
    @Test void wrongPasswordHasNoSignatureProjectionAuditOrReceipt(){
        assertThrows(IllegalArgumentException.class,()->acknowledge(null,"wrong-password"));
        assertEquals(0L,signatures.selectCount());assertEquals(0L,projections.selectCount());assertEquals(0L,audits.selectCount());assertNull(recipients.selectById(71L).getAcknowledgedAt());
    }
    @Test void projectionFailureRollsBackRealKernelAndAudit(){
        var failing=mock(DccControlledFileSignatureMapper.class);when(failing.insert(any(DccControlledFileSignatureDO.class))).thenReturn(0);
        ReflectionTestUtils.setField(bridge,"signatureMapper",failing);
        assertThrows(ServiceException.class,()->acknowledge(null,"fixture-valid-password"));
        assertEquals(0L,signatures.selectCount());assertEquals(0L,audits.selectCount());assertNull(recipients.selectById(71L).getAcknowledgedAt());
    }
    @Test void approvalBlankReasonAndUnknownKernelActionStayRejectedWithRealMapper(){
        new TransactionTemplate(tx).executeWithoutResult(ignored->{
            for(String action:List.of("APPROVE","REJECT","ASSIGN"))
                for(String comment:new String[]{null,"","  "})
                    assertThrows(ServiceException.class,()->bridge.verifyPasswordAndCreateSignature(99L,42L,"required-"+action,"MATRIX_REVIEW",action,"fixture-valid-password",comment));
            assertThrows(ServiceException.class,()->kernel.sign(new ElectronicSignatureCommand("DCC","UNKNOWN","DCC_CONTROLLED_FILE","subject","version","fixture-valid-password","reason","unknown-idem",null,null)));
        });
        assertEquals(0L,signatures.selectCount());assertEquals(0L,audits.selectCount());
    }
    @Test void missingReceiptPolicyDoesNotFallbackToOriginalOperation(){
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='signature.dcc.distribution-receipt.create'");
        var failure=assertThrows(ServiceException.class,()->acknowledge(null,"fixture-valid-password"));
        assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode(),failure.getCode());
        assertEquals(0L,signatures.selectCount());assertEquals(0L,projections.selectCount());assertEquals(0L,audits.selectCount());assertNull(recipients.selectById(71L).getAcknowledgedAt());
    }
    @Test void inactiveReceiptPolicyCannotBeUsedOrFallbackToOriginalOperation(){
        jdbc.update("UPDATE gxp_audit_policy_operation SET active=FALSE WHERE operation_id='signature.dcc.distribution-receipt.create'");
        var failure=assertThrows(ServiceException.class,()->acknowledge(null,"fixture-valid-password"));
        assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode(),failure.getCode());
        assertEquals(0L,signatures.selectCount());assertEquals(0L,audits.selectCount());assertNull(recipients.selectById(71L).getAcknowledgedAt());
    }
    private void bodyFixture(){
        wire(receipt,"messageJobMapper",jobMapper);
    }
    @Resource(name="dccControlledFileMessageJobMapper") DccControlledFileMessageJobMapper jobMapper;
    @Test void actualBodySuccessPersistsHumanReadTimeAndReplayWritesNothing(){
        bodyFixture();insertExplicitFixturePolicy("dcc.distribution.recipient.body-opened","REQUIRED_CATEGORY_AND_TEXT");
        new TransactionTemplate(tx).executeWithoutResult(ignored->receipt.recordDistributionBodyOpened(99L,42L,51L,71L));
        var opened=recipients.selectById(71L);assertNotNull(opened.getReadAt());assertNull(opened.getAcknowledgedAt());
        assertEquals(1L,audits.selectCount());assertEquals(99L,audits.selectList().get(0).getActorId());assertEquals(0L,signatures.selectCount());
        var first=opened.getReadAt();new TransactionTemplate(tx).executeWithoutResult(ignored->receipt.recordDistributionBodyOpened(99L,42L,51L,71L));
        assertEquals(first,recipients.selectById(71L).getReadAt());assertEquals(1L,audits.selectCount());assertEquals(0L,projections.selectCount());
    }
    @Test void lateBodyAuditMissingPolicyRollsBackReadAtWithoutSignatureOrNotificationFact(){
        bodyFixture();
        assertThrows(ServiceException.class,()->new TransactionTemplate(tx).executeWithoutResult(ignored->receipt.recordDistributionBodyOpened(99L,42L,51L,71L)));
        assertNull(recipients.selectById(71L).getReadAt());assertNull(recipients.selectById(71L).getAcknowledgedAt());assertEquals(0L,audits.selectCount());
        assertEquals("SENT",jobMapper.selectById(81L).getStatus());assertEquals(now,jobMapper.selectById(81L).getSentAt());
    }
    @Test void bodySuccessRequiresCurrentEnabledRecipientAndActuallySentMessage(){
        bodyFixture();insertExplicitFixturePolicy("dcc.distribution.recipient.body-opened","REQUIRED_CATEGORY_AND_TEXT");
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(1));
        assertThrows(ServiceException.class,()->new TransactionTemplate(tx).executeWithoutResult(ignored->receipt.recordDistributionBodyOpened(99L,42L,51L,71L)));
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        jdbc.update("UPDATE dcc_controlled_file_message_job SET status='FAILED',sent_at=NULL WHERE id=81");
        assertThrows(ServiceException.class,()->new TransactionTemplate(tx).executeWithoutResult(ignored->receipt.recordDistributionBodyOpened(99L,42L,51L,71L)));
        assertNull(recipients.selectById(71L).getReadAt());assertEquals(0L,audits.selectCount());
    }
    private String oldBindingRows(){return cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
            jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature_binding WHERE signature_id BETWEEN 501 AND 505 ORDER BY signature_id")));}
    private void savedFiveApprovalBindingFixture(){
        // Existing persisted approval evidence fixtures are not re-signed or re-bound by the ACK consumer.
        for(long index=1;index<=5;index++){
            var old=DccControlledFileSignatureDO.builder().id(500L+index).controlledFileId(42L).revisionId(42L).versionNo("B/1")
                    .taskId("EXISTING_APPROVAL_"+index).actorId(98L).actionType("APPROVE").meaningCode("MATRIX_REVIEW_APPROVE").signatureMode("PASSWORD")
                    .evidenceHash(Long.toHexString(index).repeat(64)).signedAt(now).build();
            assertEquals(1,projections.insert(old));
            var binding=bindingService.createBindingEvent(old,files.selectById(42L),200L,"w07-published-controlled.pdf",publishedPdfBytes,98L,"original-approval-binding-"+index);
            assertEquals(1,bindingMapper.insert(binding));assertNotNull(binding.getId());
        }
    }
    private void unchangedPublishedFact(String originalHash,String oldRows){
        assertEquals(oldRows,oldBindingRows());assertEquals(originalHash,cn.hutool.crypto.digest.DigestUtil.sha256Hex(publishedPdfBytes));
        var file=files.selectById(42L);assertEquals(200L,file.getPublishedFileId());assertEquals(200L,file.getStampedFileId());
        assertEquals(now,file.getActivatedTime());assertEquals("ACTIVE",file.getStatus());
    }
    @Test void oneNewAckBindsItsExactCopyAndReplayPreservesAllFiveOriginalBindings(){
        savedFiveApprovalBindingFixture();String before=oldBindingRows(),pdf=cn.hutool.crypto.digest.DigestUtil.sha256Hex(publishedPdfBytes);
        acknowledge(null,"fixture-valid-password");var ack=projections.selectActionSignature(42L,"DISTRIBUTION:51:71",99L,"DISTRIBUTION_ACK");
        assertNotNull(ack);assertTrue(bindingService.verifyPublishedCopyBinding(ack,files.selectById(42L)).valid());
        assertEquals(6L,bindingMapper.selectCount());assertEquals(6L,projections.selectCount());assertEquals(1L,signatures.selectCount());
        unchangedPublishedFact(pdf,before);assertNull(recipients.selectById(71L).getReadAt());
        acknowledge(null,"fixture-valid-password");assertEquals(6L,bindingMapper.selectCount());assertEquals(6L,projections.selectCount());assertEquals(1L,signatures.selectCount());
        unchangedPublishedFact(pdf,before);assertEquals(1L,jobMapper.selectCount());
    }
    @Test void publishedCopyUnreadableRollsBackNewSignatureProjectionAndReceiptWithoutTouchingFive()throws Exception{
        savedFiveApprovalBindingFixture();String before=oldBindingRows(),pdf=cn.hutool.crypto.digest.DigestUtil.sha256Hex(publishedPdfBytes);
        when(storage.getFileContent(1L,"w07-published-controlled.pdf")).thenThrow(new IllegalStateException("published immutable PDF unreadable"));
        assertThrows(RuntimeException.class,()->acknowledge(null,"fixture-valid-password"));
        assertEquals(0L,signatures.selectCount());assertEquals(5L,projections.selectCount());assertEquals(5L,bindingMapper.selectCount());
        assertNull(recipients.selectById(71L).getAcknowledgedAt());assertEquals("SENT",distributions.selectById(51L).getStatus());assertEquals(0L,audits.selectCount());unchangedPublishedFact(pdf,before);
    }
    @Test void lateReceiptBindingAuditFailureRollsBackAllNewFactsAndPreservesOriginalBindings(){
        savedFiveApprovalBindingFixture();String before=oldBindingRows(),pdf=cn.hutool.crypto.digest.DigestUtil.sha256Hex(publishedPdfBytes);
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.recipient.signature-bind'");
        assertThrows(RuntimeException.class,()->acknowledge(null,"fixture-valid-password"));
        assertEquals(0L,signatures.selectCount());assertEquals(5L,projections.selectCount());assertEquals(5L,bindingMapper.selectCount());
        assertNull(recipients.selectById(71L).getAcknowledgedAt());assertEquals(0L,audits.selectCount());unchangedPublishedFact(pdf,before);
    }
    @Test void zeroInsertedBindingNeverCommitsNewAckAndLeavesOriginalFiveUntouched(){
        savedFiveApprovalBindingFixture();String before=oldBindingRows(),pdf=cn.hutool.crypto.digest.DigestUtil.sha256Hex(publishedPdfBytes);
        var failing=mock(DccControlledFileSignatureBindingMapper.class);when(failing.insert(any(DccControlledFileSignatureBindingDO.class))).thenReturn(0);
        wire(bindingService,"bindingMapper",failing);assertThrows(RuntimeException.class,()->acknowledge(null,"fixture-valid-password"));
        assertEquals(0L,signatures.selectCount());assertEquals(5L,projections.selectCount());assertEquals(5L,bindingMapper.selectCount());
        assertNull(recipients.selectById(71L).getAcknowledgedAt());assertEquals(0L,audits.selectCount());unchangedPublishedFact(pdf,before);
    }
}
