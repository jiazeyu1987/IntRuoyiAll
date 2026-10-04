package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.sql.DataSource;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real Spring transaction, name mapper and official GxP kernel; storage/directory/environment are isolated ports. */
@Import({DccLegacySourceNameRegistrationService.class,GxpAuditServiceImpl.class})
class DccLegacySourceNameRegistrationTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource DccLegacySourceNameRegistrationService registration;
    @Resource org.springframework.transaction.PlatformTransactionManager transactions;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper claimMapper;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper masterMapper;
    @Resource(name="dccControlledFileMapper") cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper fileMapper;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper reservationMapper;
    @MockBean PermissionApi permissions;
    @MockBean AdminUserApi users;
    @MockBean FileService storage;
    @MockBean DccLegacyRegistrationSourceEnvironment sourceEnvironment;
    @TempDir Path temp;
    JdbcTemplate jdbc;AdminUserRespDTO account;LoginUser login;
    final byte[] body="real-test-body".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    final String uuid="92ca05d0-aec8-11f1-a944-02b4e226a5ef";
    @BeforeEach void setup() throws Exception {
        try(var c=dataSource.getConnection()){assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("sql/dcc_b_gxp_audit_tables.sql")).execute(dataSource);
        jdbc.update("DELETE FROM gxp_audit_event");jdbc.update("DELETE FROM gxp_audit_ledger_sequence");jdbc.update("DELETE FROM gxp_audit_policy_operation");
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(10,1,2,3,'historical','N-1','ACTIVE_CHAIN',5,6,'N-1')");
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(id,tenant_id,master_id,normalized_name) VALUES(7,1,10,'historical')");
        jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,requester_id,submitter_id,source_sha256) VALUES(20,1,10,2,3,100,100,'historical','historical','N-1','A/1','ACTIVE',99,99,?)",DigestUtil.sha256Hex(body));
        jdbc.update("INSERT INTO infra_file(id,config_id,name,path,size) VALUES(100,28,'SOP.pdf','original/100',?)",body.length);
        jdbc.update("INSERT INTO infra_file_config(id,storage,config) VALUES(28,20,?)","{\"endpoint\":\"http://127.0.0.1:9000\",\"bucket\":\"originals\",\"region\":\"us-east-1\",\"enablePathStyleAccess\":true}");
        jdbc.update("INSERT INTO dcc_controlled_file_source_ownership(tenant_id,controlled_file_id,source_file_id,origin_source_file_id,source_sha256,ownership_type,claimed_time) VALUES(1,20,100,100,?,'EXCLUSIVE',CURRENT_TIMESTAMP)",DigestUtil.sha256Hex(body));
        when(sourceEnvironment.identity()).thenReturn(Map.of("database_name","ruoyi-vue-pro","server_uuid",uuid));
        when(storage.getFileContent(28L,"original/100")).thenReturn(body);
        account=new AdminUserRespDTO();account.setId(99L);account.setTenantId(1L);account.setUsername("real-unit-admin");account.setNickname("Unit Document Control");account.setStatus(0);
        when(users.getUser(99L)).thenReturn(account);
        when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(true);when(permissions.hasAnyPermissions(99L,"dcc:controlled-file:update")).thenReturn(true);
        login=new LoginUser();login.setId(99L);login.setTenantId(1L);login.setUserType(UserTypeEnum.ADMIN.getValue());login.setInfo(Map.of("username",account.getUsername(),"nickname",account.getNickname()));
        SecurityFrameworkUtils.setLoginUser(login,new org.springframework.mock.web.MockHttpServletRequest());
    }
    void policy(){jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability) VALUES(1,'isolated-unit-policy',?,'SERVICE_METHOD',?,'DCC','DCC_LEGACY_SOURCE_NAME_SCOPE','ACTIVATE','REQUIRED_CATEGORY_AND_TEXT','NOT_REQUIRED','ABSENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','LEGACY-ACT-01','dcc-owner','GXP')",DccLegacySourceNameRegistrationService.OPERATION,DccLegacySourceNameRegistrationService.class.getName()+"#activateVerifiedScope");}
    Map<String,Object> row(String table,long id){return jdbc.queryForMap("SELECT * FROM "+table+" WHERE id=?",id);}
    DccLegacyNameVerifiedScope seal() throws Exception {
        var e=new DccLegacyNameVerifiedScope.Evidence(7L,10L,20L,100L,28L,"A/1","SOP.pdf","original/100",DigestUtil.sha256Hex(body),body.length,20,"http://127.0.0.1:9000","originals","us-east-1",true,"historical",null,null,null,5L,6L,"N-1",null,
            DccLegacyNameVerifiedScope.rowHash(row("dcc_controlled_file",20)),DccLegacyNameVerifiedScope.rowHash(row("dcc_controlled_file_master",10)),DccLegacyNameVerifiedScope.rowHash(row("dcc_controlled_file_name_claim",7)),DccLegacyNameVerifiedScope.rowHash(row("infra_file",100)));
        Path facts=temp.resolve("facts.jsonl"),results=temp.resolve("results.jsonl"),receipt=temp.resolve("receipt.json"),decision=temp.resolve("decision.json"),manifest=temp.resolve("manifest.json");
        var rows=new ArrayList<Map<String,Object>>();
        rows.add(Map.of("kind","version","id","20","tenantId","1","masterId","10","sourceFileId","100","sourceSha256",e.sourceSha256(),"versionNo","A/1"));
        rows.add(Map.of("kind","claim","id","7","tenantId","1","masterId","10","normalizedName","historical"));
        rows.add(Map.of("kind","master","id","10","tenantId","1","projectId","5","leafId","6","normalizedNumber","N-1"));
        rows.add(Map.of("kind","storage","id","100","configId","28","name","SOP.pdf","nameHex",HexFormat.of().withUpperCase().formatHex("SOP.pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"size",String.valueOf(body.length)));
        rows.add(Map.of("kind","ownership","controlledFileId","20","tenantId","1","sourceFileId","100","sourceSha256",e.sourceSha256()));
        Files.writeString(facts,rows.stream().map(JsonUtils::toJsonString).collect(java.util.stream.Collectors.joining("\n"))+"\n");
        var result=new LinkedHashMap<String,Object>();result.put("id","100");result.put("status","MATCH");result.put("actualSha256",e.sourceSha256());result.put("actualLength",body.length);result.put("httpStatus",200);result.put("errorCode",null);
        Files.writeString(results,new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result)+"\n");
        var finished=OffsetDateTime.now(ZoneOffset.UTC).withNano(0);var proof=new LinkedHashMap<String,Object>();proof.put("status","SOURCE_BYTES_VERIFIED");proof.put("sourceBytesVerified",true);proof.put("source",Map.of("database","ruoyi-vue-pro","serverUuid",uuid));proof.put("readerExitCode",0);proof.put("objectsRead",1);proof.put("matches",1);proof.put("resultsSha256",DigestUtil.sha256Hex(Files.readAllBytes(results)));proof.put("sourceFactsSha256",DigestUtil.sha256Hex(Files.readAllBytes(facts)));proof.put("finishedAtUtc",finished.toString());
        Files.writeString(receipt,JsonUtils.toJsonString(proof));
        Files.writeString(decision,JsonUtils.toJsonString(Map.of("historicalNamesDecision",Map.of("preserveHistoricalFilesVersionsNamesAndSignatures",true,"verifiedHistoricalOriginalNamesRemainOccupied",true,"rejectFutureNewExactDuplicateNames",true,"autoRenameMergeOrDeleteHistoricalRecords",false))));
        var root=new DccLegacyNameVerifiedScope.RootManifest(1,"ruoyi-vue-pro",uuid,1L,"unit-sealed",DigestUtil.sha256Hex(Files.readAllBytes(facts)),DigestUtil.sha256Hex(Files.readAllBytes(receipt)),DigestUtil.sha256Hex(Files.readAllBytes(decision)),DccLegacyNameVerifiedScope.identityHash(List.of(e)),finished.atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime(),"verified original history","unit-root-review",List.of(e));
        var rawManifest=JsonUtils.parseObject(JsonUtils.toJsonString(root),Map.class);
        rawManifest.put("verifiedAt",root.verifiedAt().toString());
        Files.writeString(manifest,JsonUtils.toJsonString(rawManifest));
        return DccLegacyNameVerifiedScope.fromProtectedArtifact(manifest,DigestUtil.sha256Hex(Files.readAllBytes(manifest)),facts,receipt,results,decision);
    }
    void zero(){assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_legacy_source_name_scope",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_legacy_source_name_evidence",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_source_name_reservation",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));}
    @Test void absentAuthenticationCannotFallBackToSystemActor() throws Exception {var s=seal();SecurityContextHolder.clearContext();assertThrows(IllegalArgumentException.class,()->registration.activateVerifiedScope(s));zero();verifyNoInteractions(storage);}
    @Test void actualMissingAuditPolicyRollsBackAllNewSidecarRows() throws Exception {var s=seal();assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->registration.activateVerifiedScope(s));zero();assertNull(row("dcc_controlled_file_name_claim",7).get("source_original_file_name"));}
    @Test void actualKernelAppendAndSidecarsCommitTogetherWithRealActorAndReplayZeroWrites() throws Exception {
        policy();var s=seal();var before=List.of(row("dcc_controlled_file_name_claim",7),row("dcc_controlled_file_master",10),row("dcc_controlled_file",20));
        var receipt=registration.activateVerifiedScope(s);assertFalse(receipt.replay());assertNotNull(receipt.eventHash());assertEquals(before,List.of(row("dcc_controlled_file_name_claim",7),row("dcc_controlled_file_master",10),row("dcc_controlled_file",20)));
        assertEquals("real-unit-admin",jdbc.queryForObject("SELECT actor_username FROM gxp_audit_event",String.class));
        var again=registration.activateVerifiedScope(s);assertTrue(again.replay());assertEquals(receipt.eventId(),again.eventId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_source_name_reservation",Integer.class));
    }
    @Test void currentSource404CannotCreatePreparedScopeOrAnyPartialOccupancy() throws Exception {policy();var s=seal();when(storage.getFileContent(28L,"original/100")).thenThrow(new java.io.IOException("test 404"));assertThrows(IllegalStateException.class,()->registration.activateVerifiedScope(s));zero();}
    @Test void currentSourceMismatchCannotTrustSealedMatchOrBoolean() throws Exception {policy();var s=seal();when(storage.getFileContent(28L,"original/100")).thenReturn("changed".getBytes());assertThrows(IllegalArgumentException.class,()->registration.activateVerifiedScope(s));zero();}
    @Test void freshOriginalRowDriftFailsBeforeWrites() throws Exception {policy();var s=seal();jdbc.update("UPDATE dcc_controlled_file SET title='changed' WHERE id=20");assertThrows(IllegalArgumentException.class,()->registration.activateVerifiedScope(s));zero();}
    @Test void sourceMetadataDeletionAndForeignTenantOwnerAreRejected() throws Exception {policy();var s=seal();jdbc.update("UPDATE dcc_controlled_file_source_ownership SET tenant_id=2 WHERE controlled_file_id=20");assertThrows(IllegalArgumentException.class,()->registration.activateVerifiedScope(s));zero();}
    @Test void inactiveActualAccountAndMissingRealRoleNeverReachStorageOrWrites() throws Exception {var s=seal();account.setStatus(1);assertThrows(IllegalArgumentException.class,()->registration.activateVerifiedScope(s));zero();account.setStatus(0);when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(false);assertThrows(IllegalArgumentException.class,()->registration.activateVerifiedScope(s));zero();verifyNoInteractions(storage);}
    @Test void foreignDatabaseUuidCannotActivateThisScope() throws Exception {policy();var s=seal();when(sourceEnvironment.identity()).thenReturn(Map.of("database_name","ruoyi-vue-pro","server_uuid","wrong"));assertThrows(IllegalArgumentException.class,()->registration.activateVerifiedScope(s));zero();}
    @Test void persistedEvidenceCannotReplayWithTamperedPayloadAndAnUnchangedProofHash() throws Exception {policy();var s=seal();registration.activateVerifiedScope(s);jdbc.update("UPDATE dcc_legacy_source_name_evidence SET source_path='tampered' WHERE controlled_file_id=20");assertThrows(IllegalStateException.class,()->registration.activateVerifiedScope(s));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));}

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"tenant","visitTenant","permission","role","username","nickname"})
    void realAuthenticationAndAuthorizationMustRemainExactBeforeAnyWrites(String change) throws Exception {
        var sealed=seal();switch(change){case "tenant"->login.setTenantId(2L);case "visitTenant"->login.setVisitTenantId(2L);case "permission"->when(permissions.hasAnyPermissions(99L,"dcc:controlled-file:update")).thenReturn(false);case "role"->when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(false);case "username"->login.setInfo(Map.of("username","SYSTEM_ACTOR","nickname",account.getNickname()));case "nickname"->login.setInfo(Map.of("username",account.getUsername()));}
        assertThrows(IllegalArgumentException.class,()->registration.activateVerifiedScope(sealed));zero();verifyNoInteractions(storage);
    }
    @Test void loaderRejectsActualIncomplete35Of39ReceiptEvenWhenManifestTriesToClaimMatch() throws Exception {
        seal();var path=temp.resolve("receipt.json");var proof=JsonUtils.parseObject(Files.readString(path),Map.class);proof.put("status","SOURCE_BYTES_NOT_VERIFIED");proof.put("sourceBytesVerified",false);proof.put("readerExitCode",1);proof.put("objectsRead",39);proof.put("matches",35);Files.writeString(path,JsonUtils.toJsonString(proof));
        var manifest=JsonUtils.parseObject(Files.readString(temp.resolve("manifest.json")),Map.class);manifest.put("bytesReceiptSha256",DigestUtil.sha256Hex(Files.readAllBytes(path)));Files.writeString(temp.resolve("manifest.json"),JsonUtils.toJsonString(manifest));
        assertThrows(IllegalArgumentException.class,()->reload());zero();verifyNoInteractions(storage);
    }
    @Test void loaderRejectsDuplicateActualResult() throws Exception {
        seal();var results=temp.resolve("results.jsonl");Files.writeString(results,Files.readString(results)+Files.readString(results));resealReceipt();
        assertThrows(IllegalArgumentException.class,()->reload());zero();
    }
    @Test void loaderRejectsMissingFrozenVersionRatherThanTrustingDeclaredCounts() throws Exception {
        seal();var facts=temp.resolve("facts.jsonl");Files.writeString(facts,Files.readAllLines(facts).stream().filter(line->!line.contains("\"version\"")).collect(java.util.stream.Collectors.joining("\n"))+"\n");
        var proof=JsonUtils.parseObject(Files.readString(temp.resolve("receipt.json")),Map.class);proof.put("sourceFactsSha256",DigestUtil.sha256Hex(Files.readAllBytes(facts)));Files.writeString(temp.resolve("receipt.json"),JsonUtils.toJsonString(proof));
        var manifest=JsonUtils.parseObject(Files.readString(temp.resolve("manifest.json")),Map.class);manifest.put("factsSha256",DigestUtil.sha256Hex(Files.readAllBytes(facts)));manifest.put("bytesReceiptSha256",DigestUtil.sha256Hex(Files.readAllBytes(temp.resolve("receipt.json"))));Files.writeString(temp.resolve("manifest.json"),JsonUtils.toJsonString(manifest));
        assertThrows(IllegalArgumentException.class,()->reload());zero();
    }
    @Test void loaderRejectsAUserDecisionThatNeverAuthorizedHistoricalOccupation() throws Exception {
        seal();var decision=temp.resolve("decision.json");Files.writeString(decision,JsonUtils.toJsonString(Map.of("historicalNamesDecision",Map.of("preserveHistoricalFilesVersionsNamesAndSignatures",false,"verifiedHistoricalOriginalNamesRemainOccupied",false,"rejectFutureNewExactDuplicateNames",true,"autoRenameMergeOrDeleteHistoricalRecords",true))));
        var manifest=JsonUtils.parseObject(Files.readString(temp.resolve("manifest.json")),Map.class);manifest.put("userDecisionSha256",DigestUtil.sha256Hex(Files.readAllBytes(decision)));Files.writeString(temp.resolve("manifest.json"),JsonUtils.toJsonString(manifest));assertThrows(IllegalArgumentException.class,()->reload());zero();
    }
    @Test void officialKernelLateAppendFailureRollsBackAlreadyPreparedRegistryAndVerifiedScope() throws Exception {
        policy();var sealed=seal();jdbc.execute("ALTER TABLE gxp_audit_event ADD CONSTRAINT reject_test_activation CHECK (actor_id<>99)");
        try {assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->registration.activateVerifiedScope(sealed));zero();assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_ledger_sequence",Integer.class));}
        finally {jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT reject_test_activation");}
    }
    @Test void firstRegistrationAndConcurrentNewUseTheActualUniqueRegistryAndFailClosedUnresolvedGate() throws Exception {
        policy();var sealed=seal();jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(11,1,2,4,'new','N-2','ACTIVE_CHAIN',7,6,'N-2')");
        var nameService=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(nameService,"claimMapper",claimMapper);org.springframework.test.util.ReflectionTestUtils.setField(nameService,"masterMapper",masterMapper);org.springframework.test.util.ReflectionTestUtils.setField(nameService,"fileMapper",fileMapper);org.springframework.test.util.ReflectionTestUtils.setField(nameService,"reservationMapper",reservationMapper);
        var start=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var activation=pool.submit(()->{TenantContextHolder.setTenantId(1L);SecurityFrameworkUtils.setLoginUser(login,new org.springframework.mock.web.MockHttpServletRequest());try{start.await();return registration.activateVerifiedScope(sealed);}finally{SecurityContextHolder.clearContext();TenantContextHolder.clear();}});
            var creation=pool.submit(()->{TenantContextHolder.setTenantId(1L);try{start.await();new org.springframework.transaction.support.TransactionTemplate(transactions).execute(s->{nameService.claimIdentity(1L,"SOP.pdf",7L,6L,"N-2",11L);return null;});return true;}catch(cn.iocoder.yudao.framework.common.exception.ServiceException rejected){assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NAME_EXISTS.getCode(),rejected.getCode());return false;}catch(IllegalStateException unresolved){assertEquals("historical name claims require verified original source names",unresolved.getMessage());return false;}finally{TenantContextHolder.clear();}});
            start.countDown();assertFalse(activation.get(30,java.util.concurrent.TimeUnit.SECONDS).replay());assertFalse(creation.get(30,java.util.concurrent.TimeUnit.SECONDS));
        } finally {pool.shutdownNow();}
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_source_name_reservation",Integer.class));assertEquals("LEGACY_GROUP",jdbc.queryForObject("SELECT reservation_kind FROM dcc_source_name_reservation",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE source_original_file_name IS NOT NULL",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
    }
    DccLegacyNameVerifiedScope reload() throws Exception {var manifest=temp.resolve("manifest.json");return DccLegacyNameVerifiedScope.fromProtectedArtifact(manifest,DigestUtil.sha256Hex(Files.readAllBytes(manifest)),temp.resolve("facts.jsonl"),temp.resolve("receipt.json"),temp.resolve("results.jsonl"),temp.resolve("decision.json"));}
    void resealReceipt() throws Exception {var path=temp.resolve("receipt.json");var proof=JsonUtils.parseObject(Files.readString(path),Map.class);proof.put("resultsSha256",DigestUtil.sha256Hex(Files.readAllBytes(temp.resolve("results.jsonl"))));Files.writeString(path,JsonUtils.toJsonString(proof));var manifest=JsonUtils.parseObject(Files.readString(temp.resolve("manifest.json")),Map.class);manifest.put("bytesReceiptSha256",DigestUtil.sha256Hex(Files.readAllBytes(path)));Files.writeString(temp.resolve("manifest.json"),JsonUtils.toJsonString(manifest));}
    @AfterEach void cleanup(){jdbc.update("DELETE FROM dcc_controlled_file_source_ownership WHERE controlled_file_id=20");jdbc.update("DELETE FROM infra_file WHERE id=100");jdbc.update("DELETE FROM infra_file_config WHERE id=28");jdbc.update("DELETE FROM gxp_audit_event");jdbc.update("DELETE FROM gxp_audit_ledger_sequence");jdbc.update("DELETE FROM gxp_audit_policy_operation");SecurityContextHolder.clearContext();TenantContextHolder.clear();}
}
