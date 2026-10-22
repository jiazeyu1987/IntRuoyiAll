package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.bpm.approval.service.provider.*;
import cn.iocoder.yudao.module.bpm.approval.service.signature.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewPrepareDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalReviewReceiptMapper;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.signature.service.*;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual center executor and receipt MyBatis statements in isolated H2 physical transactions.
 * Signature, identity, image and domain systems are explicit boundary doubles, not crypto evidence. */
class ApprovalCenterReviewExecutorTest {
    @Test void succeedsWithSignatureDomainImageAuditThenActualReceiptInsert() throws Exception {
        var f=new Fixture(null);
        try {
            TenantContextHolder.setTenantId(1L);
            var result=f.tx.execute(s -> f.executor.execute(4L,f.command,f.signer,"c".repeat(64)));
            assertNotNull(result);
            var row=f.mapper.selectExact(1L,f.command.getIdempotencyKey());
            assertNotNull(row);assertEquals(result.receiptId(),row.getId().toString());
            assertEquals(700L,row.getReceiptAuditId());
            assertEquals(ApprovalCenterReceiptService.recordHash(row),row.getReceiptRecordHash());
            assertEquals(List.of("SIGNATURE","DOMAIN","IMAGE","AUDIT"),f.jdbc.queryForList("SELECT stage FROM committed_effect ORDER BY ordinal",String.class));
            // Winning retry returns the original result without new domain/image/signature effects.
            var retry=f.tx.execute(s -> f.executor.execute(4L,f.command,f.signer,"c".repeat(64)));
            assertEquals(result,retry);
            assertEquals(4,f.jdbc.queryForObject("SELECT COUNT(*) FROM committed_effect",Integer.class));
            assertThrows(IllegalArgumentException.class,()->f.tx.execute(s -> f.executor.execute(4L,f.command,f.signer,"d".repeat(64))));
        } finally {TenantContextHolder.clear();}
    }

    @Test void domainImageAndAuditFailuresRollbackAllPriorTransactionalEffectsAndNoReceipt() throws Exception {
        for(String stage:List.of("DOMAIN","IMAGE","AUDIT")) {
            var f=new Fixture(stage);
            try {
                TenantContextHolder.setTenantId(1L);
                assertThrows(IllegalStateException.class,()->f.tx.execute(s -> f.executor.execute(4L,f.command,f.signer,"c".repeat(64))),stage);
                assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM committed_effect",Integer.class),stage);
                assertNull(f.mapper.selectExact(1L,f.command.getIdempotencyKey()),stage);
            } finally {TenantContextHolder.clear();}
        }
    }

    private static final class Fixture {
        final JdbcTemplate jdbc;
        final TransactionTemplate tx;
        final BpmApprovalReviewReceiptMapper mapper;
        final ApprovalCenterReviewExecutor executor;
        final String failure;
        final ApprovalTaskReviewCommand command;
        final AuthenticatedSigner signer;
        int ordinal;

        Fixture(String failure) throws Exception {
            this.failure=failure;
            var source=new DriverManagerDataSource("jdbc:h2:mem:center_executor_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
            jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));
            jdbc.execute("CREATE TABLE committed_effect (ordinal INT PRIMARY KEY,stage VARCHAR(32) NOT NULL)");
            jdbc.execute("CREATE TABLE bpm_approval_review_receipt (id BIGINT PRIMARY KEY,tenant_id BIGINT,actor_id BIGINT,operator_id BIGINT,identity_domain VARCHAR(32),idempotency_key VARCHAR(36) UNIQUE,request_hash VARCHAR(64),request_hash_version VARCHAR(32),prepare_id VARCHAR(36),identity_json CLOB,content_policy_version VARCHAR(64),subject_version VARCHAR(128),preview_content_hash VARCHAR(64),canonical_business_content_json CLOB,action_code VARCHAR(32),reason VARCHAR(4000),center_signature_id BIGINT,center_evidence_hash VARCHAR(64),signature_creation_audit_id BIGINT,result_code VARCHAR(32),resulting_status VARCHAR(64),domain_links_json CLOB,response_json CLOB,receipt_audit_id BIGINT UNIQUE,receipt_record_hash VARCHAR(64),committed_at TIMESTAMP)");
            var config=new Configuration();config.setMapUnderscoreToCamelCase(true);config.addMapper(BpmApprovalReviewReceiptMapper.class);
            var factory=new SqlSessionFactoryBean();factory.setDataSource(source);factory.setConfiguration(config);
            mapper=new SqlSessionTemplate(factory.getObject()).getMapper(BpmApprovalReviewReceiptMapper.class);
            String version="EDHR1:"+"a".repeat(64),hash=ApprovalContentJson.hash("{}");
            var identity=new ApprovalIdentity(1L,"EDHR","EDHR_WORK_TASK","2",null,"EDHR_RELEASE","3");
            var content=new PreparedApprovalContent(identity,"approval-content-v1",version,hash,"EDHR_RELEASE","scope","{}",List.of(),List.of("APPROVE"));
            command=new ApprovalTaskReviewCommand().setModuleCode(ApprovalModuleCode.EDHR).setSourceTaskType("EDHR_WORK_TASK").setSourceTaskId("2").setResult(ApprovalTaskReviewResult.APPROVE).setReason("reviewed").setPrepareId(UUID.randomUUID().toString()).setIdempotencyKey(UUID.randomUUID().toString()).setExpectedSubjectVersion(version).setExpectedContentHash(hash).setContentPolicyVersion("approval-content-v1");
            signer=new AuthenticatedSigner(1L,"SYSTEM_USER",4L,4L,UUID.randomUUID().toString(),1L,command.getIdempotencyKey());
            var audit=mock(GxpAuditService.class);
            doAnswer(i -> {assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());return null;}).when(audit).acquireLedgerLock();
            when(audit.reserveEventId(anyString(),anyString(),anyString(),anyString())).thenAnswer(i -> {
                assertEquals(ApprovalCenterReceiptService.sourceLocator(Long.valueOf(i.getArgument(1))),i.getArgument(2));return 700L;
            });
            when(audit.append(any())).thenAnswer(i -> {GxpAuditCommand c=i.getArgument(0);assertEquals(700L,c.getReservedEventId());effect("AUDIT");return new GxpAuditAppendResult(700L,1L,"a".repeat(64),false);});
            var auth=mock(SignatureAuthenticationApi.class);when(auth.identityDomain()).thenReturn("SYSTEM_USER");when(auth.lockAndRevalidate(any(),any())).thenReturn(signer);
            var authority=mock(ApprovalAuthorityApi.class);
            var resolver=mock(ApprovalContentResolver.class);when(resolver.lockAndAuthorize(4L,identity)).thenReturn(content);
            var contents=mock(ApprovalContentResolverRegistry.class);when(contents.require(ApprovalModuleCode.EDHR)).thenReturn(resolver);
            var provider=mock(ApprovalTaskProvider.class,withSettings().extraInterfaces(ApprovalTrustedDecisionProvider.class));when(provider.isVisibleTo(4L)).thenReturn(true);
            when(((ApprovalTrustedDecisionProvider)provider).decide(anyLong(),anyLong(),any(),any(),any())).thenAnswer(i -> {effect("DOMAIN");return new ApprovalDomainDecision("APPROVED","COMPLETED","{}");});
            var providers=mock(ApprovalTaskProviderRegistry.class);when(providers.requireProvider(ApprovalModuleCode.EDHR)).thenReturn(provider);
            var prepare=new BpmApprovalReviewPrepareDO();prepare.setTenantId(1L);prepare.setOwnerCode("EDHR");prepare.setSourceTaskType("EDHR_WORK_TASK");prepare.setSourceTaskId("2");prepare.setSubjectType("EDHR_RELEASE");prepare.setSubjectId("3");prepare.setSubjectVersion(version);prepare.setPreviewContentHash(hash);prepare.setContentPolicyVersion("approval-content-v1");prepare.setCanonicalBusinessContentJson("{}");
            var prepares=mock(ApprovalReviewPrepareService.class);when(prepares.require(4L,command.getPrepareId(),true)).thenReturn(prepare);
            var receipts=mock(ApprovalCenterReceiptService.class);
            when(receipts.findVerifiedCurrent(4L,command.getIdempotencyKey())).thenAnswer(i -> mapper.selectExactCurrent(1L,command.getIdempotencyKey()));
            when(receipts.findVerified(4L,command.getIdempotencyKey())).thenAnswer(i -> {var row=mapper.selectExact(1L,command.getIdempotencyKey());assertNotNull(row);assertEquals(ApprovalCenterReceiptService.recordHash(row),row.getReceiptRecordHash());return row;});
            when(receipts.response(any())).thenAnswer(i -> cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(((cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewReceiptDO)i.getArgument(0)).getResponseJson(),ApprovalDecisionReceipt.class));
            var signatures=mock(ElectronicSignatureTransactionExecutor.class);when(signatures.executeAuthenticated(any(),any())).thenAnswer(i -> {effect("SIGNATURE");return new ElectronicSignatureResult(500L,"VERIFIED",LocalDateTime.of(2026,10,10,9,0),"clock",version,hash,"b".repeat(64),"SHA-256","digest-format-v2");});
            var fact=mock(ElectronicSignatureVerifiedFactService.VerifiedSignatureFact.class);when(fact.auditEventId()).thenReturn(600L);
            var verifier=mock(ElectronicSignatureVerifiedFactService.class);when(verifier.requireVerifiedFact(500L)).thenReturn(fact);
            var images=mock(ApprovalSignatureImageSnapshotProvider.class);when(images.requireActiveSnapshotLocked(4L)).thenReturn(ApprovalSignatureImageSnapshot.builder().imageId(10L).versionNo(1).fileId(11L).fileUrl("/signature.png").sha256("a".repeat(64)).contentType("image/png").fileSize(12L).imageStatus("ACTIVE").verifiedStatus("VERIFIED").build());doAnswer(i -> {effect("IMAGE");return null;}).when(images).markReferencedLocked(4L,10L);
            var users=mock(AdminUserApi.class);when(users.getUser(4L)).thenReturn(new AdminUserRespDTO().setId(4L).setNickname("Reviewer"));
            executor=new ApprovalCenterReviewExecutor(audit,authority,List.of(auth),contents,providers,prepares,receipts,mapper,signatures,verifier,images,users,Clock.fixed(Instant.parse("2026-10-10T01:00:00Z"),ZoneOffset.UTC));
            ReflectionTestUtils.setField(executor,"signatureAuthorization",mock(ApprovalSignatureAuthorizationService.class));
        }
        void effect(String stage) {jdbc.update("INSERT INTO committed_effect VALUES (?,?)",++ordinal,stage);if(stage.equals(failure))throw new IllegalStateException(stage+" failed");}
    }
}
