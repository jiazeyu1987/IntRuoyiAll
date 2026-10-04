package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper;
import org.springframework.test.context.jdbc.Sql;
@org.springframework.context.annotation.Import(DccControlledFileNameClaimService.class)
public class DccLegacySourceNameOccupancyTest extends BaseDbUnitTest {
 @Test void obsoleteAuditSnapshotMustIncludeTheActualSidecarTwentyYearDeadline() {
  legacyFixture();var obsolete=java.time.LocalDateTime.of(2026,10,3,12,0);names.retainObsoleteIdentity(1L,10L,obsolete,obsolete.plusYears(20));
  var audit=new DccWorkflowFileStateAudit();org.springframework.test.util.ReflectionTestUtils.setField(audit,"jdbcTemplate",jdbc);
  var master=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.builder().id(10L).tenantId(1L).build();
  var snapshot=transaction().execute(s->audit.capture(files.selectById(20L),master));
  assertEquals(obsolete.plusYears(20).toString(),cn.hutool.json.JSONUtil.parseObj(snapshot.json()).getStr("retainUntil"));
 }
 public static String utf8Hex(String value){return value==null?null:java.util.HexFormat.of().withUpperCase().formatHex(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 @Resource DataSource source;
 @Resource PlatformTransactionManager tx;
 @Resource DccControlledFileNameClaimMapper claims;
 @Resource DccControlledFileNameClaimService names;
 @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper files;
 @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper reservations;
 @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSelectorMapper selector;
 JdbcTemplate jdbc;
 @BeforeEach void setup() throws Exception {TenantContextHolder.setTenantId(1L);try(var c=source.getConnection()){assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}jdbc=new JdbcTemplate(source);}
 @AfterEach void clear(){TenantContextHolder.clear();}
 @Test void verifiedSidecarMustExistAndMustNeverTreatRawNullClaimAsResolved() throws Exception {
  var type=Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccLegacySourceNameOccupancyService");
  var bean=type.getConstructor(JdbcTemplate.class).newInstance(jdbc);
  assertEquals(Long.valueOf(0L),ReflectionTestUtils.invokeMethod(bean,"countUnresolved",1L));
 }
 void legacyFixture() {
  jdbc.update("INSERT INTO infra_file_config(id,storage,config) VALUES(28,20,?)","{\"endpoint\":\"http://127.0.0.1:9000\",\"bucket\":\"originals\",\"region\":\"us-east-1\",\"enablePathStyleAccess\":true}");
  jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status) VALUES(10,1,2,3,'old-template','OLD-1','ACTIVE_CHAIN')");
  jdbc.update("INSERT INTO dcc_controlled_file_name_claim(id,tenant_id,master_id,normalized_name) VALUES(7,1,10,'old-template')");
  jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,requester_id,submitter_id,change_type,source_sha256) VALUES(20,1,10,2,3,100,100,'old-template','old-template','OLD-1','A/1','ACTIVE',99,99,'NEW',?)","a".repeat(64));
  jdbc.execute("CREATE TABLE IF NOT EXISTS infra_file(id BIGINT PRIMARY KEY,config_id BIGINT,name VARCHAR(256),size BIGINT,deleted TINYINT DEFAULT 0)");
  jdbc.update("INSERT INTO infra_file(id,config_id,name,size,path) VALUES(100,28,'SOP.pdf',4,'sealed/object')");
  jdbc.update("INSERT INTO dcc_legacy_source_name_scope(id,tenant_id,scope_id,manifest_sha256,facts_sha256,bytes_receipt_sha256,user_decision_sha256,scope_identity_sha256,status,claim_count,version_count,source_count,edge_count,name_count,verified_at,activated_at,actor_id,reason,request_id) VALUES(1,1,'test',?,?,?,?,?,'VERIFIED',1,1,1,1,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,99,'verified originals','test')","b".repeat(64),"c".repeat(64),"d".repeat(64),"e".repeat(64),"f".repeat(64));
  jdbc.update("INSERT INTO dcc_legacy_source_name_evidence(tenant_id,verification_scope_id,legacy_claim_id,legacy_master_id,controlled_file_id,source_file_id,config_id,source_path,storage_type,storage_endpoint,storage_bucket,storage_region,storage_path_style,version_no,source_original_file_name,expected_sha256,actual_sha256,expected_size,actual_size,bytes_status,expected_version_count,claim_normalized_name,metadata_identity_sha256,preimage_sha256,proof_row_sha256) VALUES(1,1,7,10,20,100,28,'sealed/object',20,'http://127.0.0.1:9000','originals','us-east-1',1,'A/1','SOP.pdf',?,?,4,4,'MATCH',1,'old-template',?,?,?)","a".repeat(64),"a".repeat(64),"b".repeat(64),"c".repeat(64),"d".repeat(64));
  jdbc.update("INSERT INTO dcc_source_name_reservation(tenant_id,source_original_file_name,reservation_kind,verification_scope_id,generation,active) VALUES(1,'SOP.pdf','LEGACY_GROUP',1,1,1)");
 }
 @Test void completeVerifiedScopeMustResolveTheRealMapperGuardWithoutWritingAnyLegacyName() {
  legacyFixture();
  assertEquals(0,claims.countUnresolvedNames(1L));
  assertNull(jdbc.queryForObject("SELECT source_original_file_name FROM dcc_controlled_file_name_claim WHERE id=7",String.class));
 }
 void selectorFixture(){legacyFixture();formalLegacyIdentity();jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status) VALUES(5,1,'Formal project','P5','ENABLE')");jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20 WHERE id=10");jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP WHERE id=20");}
 @Test void legacySourceNameKeywordMustUseVerifiedExactVersionInAllCandidateCountAndPageQueries() {
  selectorFixture();var request=new DccControlledFileSelectorQuery(null,null,null,"SOP.pdf",1,10);
  assertEquals(1,selector.selectCandidates(1L,request).size());
  assertEquals(1,selector.countAuthorized(1L,request,java.util.List.of(20L)));
  assertEquals(20L,selector.selectAuthorizedPage(1L,request,java.util.List.of(20L),10,0).get(0).getId());
 }
 @Test void publicSelectorProjectsVerifiedLegacyNameOnlyAfterFormalNamePermissionAndDoesNotGrantBody() {
  selectorFixture();var query=selectorQuery();var request=new DccControlledFileSelectorQuery(null,null,null,"SOP.pdf",1,10);
  var result=query.getControlledFileSelectorPage(99L,request);assertEquals(1,result.getTotal());
  assertEquals("SOP.pdf",result.getList().get(0).fileName());assertFalse(result.getList().get(0).canPreview());
  assertNull(files.selectById(20L).getSourceOriginalFileName());
 }
 @Test void selectorUnknownOrPreparedNameCannotSearchOrProjectANewIdentity() {
  selectorFixture();jdbc.update("UPDATE dcc_legacy_source_name_scope SET status='PREPARED'");
  assertEquals(0,selector.selectCandidates(1L,new DccControlledFileSelectorQuery(null,null,null,"SOP.pdf",1,10)).size());
  assertThrows(IllegalStateException.class,()->selectorQuery().getControlledFileSelectorPage(99L,new DccControlledFileSelectorQuery(null,null,null,null,1,10)));
 }
 @Test void selectorForeignTenantAndHardAssignmentCannotReachVerifiedNameProjection() {
  selectorFixture();var query=selectorQuery();var scope=org.mockito.Mockito.mock(DccControlledFileAssignmentScopeService.class);org.springframework.test.util.ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
  assertEquals(0,query.getControlledFileSelectorPage(99L,new DccControlledFileSelectorQuery(null,null,null,null,1,10)).getTotal());
  TenantContextHolder.setTenantId(2L);assertEquals(0,selector.selectCandidates(2L,new DccControlledFileSelectorQuery(null,null,null,"SOP.pdf",1,10)).size());
 }
 @Test void selectorVerifiedNameDoesNotRepairMissingFormalLegacyLeafOrNumber() {
  selectorFixture();jdbc.update("UPDATE dcc_controlled_file_master SET file_type_taxonomy_leaf_id=NULL WHERE id=10");jdbc.update("UPDATE dcc_legacy_source_name_evidence SET master_leaf_id=NULL");
  assertThrows(IllegalStateException.class,()->selectorQuery().getControlledFileSelectorPage(99L,new DccControlledFileSelectorQuery(null,null,null,null,1,10)));
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.CsvSource({"SOP.DB,template.pdf,0","SOP.PDF,template.db,1"})
 void selectorBlacklistUsesVerifiedRawSourceExtensionAfterNamePermission(String source,String template,long total) {
  selectorFixture();jdbc.update("UPDATE infra_file SET name=? WHERE id=100",source);jdbc.update("UPDATE dcc_legacy_source_name_evidence SET source_original_file_name=?",source);jdbc.update("UPDATE dcc_source_name_reservation SET source_original_file_name=?",source);jdbc.update("UPDATE dcc_controlled_file SET file_name=?,title=? WHERE id=20",template,template);
  var query=selectorQuery();var settings=org.mockito.Mockito.mock(DccControlledFileBrowserSettingsService.class);org.mockito.Mockito.when(settings.getBlacklistedExtensionPatterns()).thenReturn(java.util.List.of("*.db"));org.springframework.test.util.ReflectionTestUtils.setField(query,"browserSettingsService",settings);
  var result=query.getControlledFileSelectorPage(99L,new DccControlledFileSelectorQuery(null,null,null,null,1,10));assertEquals(total,result.getTotal());
  if(total==1){assertEquals(source,result.getList().get(0).fileName());assertFalse(result.getList().get(0).canPreview());}
 }
 DccControlledFileQueryServiceImpl selectorQuery(){
  var query=new DccControlledFileQueryServiceImpl();for(var field:query.getClass().getDeclaredFields())if(field.getAnnotation(Resource.class)!=null && !java.lang.reflect.Modifier.isStatic(field.getModifiers()))org.springframework.test.util.ReflectionTestUtils.setField(query,field.getName(),org.mockito.Mockito.mock(field.getType()));
  org.springframework.test.util.ReflectionTestUtils.setField(query,"selectorMapper",selector);org.springframework.test.util.ReflectionTestUtils.setField(query,"controlledFileMapper",files);org.springframework.test.util.ReflectionTestUtils.setField(query,"nameClaimService",names);
  var master=org.springframework.test.util.ReflectionTestUtils.getField(names,"masterMapper");org.springframework.test.util.ReflectionTestUtils.setField(query,"controlledFileMasterMapper",master);
  var directory=org.mockito.Mockito.mock(cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService.class);org.mockito.Mockito.when(directory.hasDirectoryManagementPermission(99L)).thenReturn(true);org.springframework.test.util.ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directory);
  var assigned=org.mockito.Mockito.mock(DccControlledFileAssignmentScopeService.class);org.mockito.Mockito.when(assigned.isWithinAssignedFileScope(org.mockito.ArgumentMatchers.eq(99L),org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);org.springframework.test.util.ReflectionTestUtils.setField(query,"assignmentScopeService",assigned);
  return query;
 }

 void formalLegacyIdentity() {
  jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=5,file_type_taxonomy_leaf_id=6,normalized_file_number='N-1' WHERE id=10");
  jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=5,file_type_taxonomy_id=6,file_number='N-1' WHERE id=20");
  jdbc.update("UPDATE dcc_controlled_file_name_claim SET dcc_project_code_id=5,file_type_taxonomy_leaf_id=6,normalized_file_number='N-1' WHERE id=7");
  jdbc.update("UPDATE dcc_legacy_source_name_evidence SET claim_project_id=5,claim_leaf_id=6,claim_number='N-1',master_project_id=5,master_leaf_id=6,master_number='N-1'");
 }
 void newMaster(long id,long project,String number) {jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status) VALUES(?,1,2,3,?,?,?,6,?,'ACTIVE_CHAIN')",id,"new-template-"+id,number,project,number);}
 org.springframework.transaction.support.TransactionTemplate transaction(){return new org.springframework.transaction.support.TransactionTemplate(tx);}
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={
  "UPDATE dcc_legacy_source_name_scope SET status='PREPARED'",
  "DELETE FROM dcc_legacy_source_name_evidence",
  "UPDATE dcc_legacy_source_name_scope SET version_count=2",
  "UPDATE dcc_legacy_source_name_evidence SET expected_version_count=2",
  "UPDATE dcc_legacy_source_name_evidence SET bytes_status='MISSING'",
  "UPDATE dcc_legacy_source_name_evidence SET actual_sha256=REPEAT('b',64)",
  "UPDATE dcc_legacy_source_name_evidence SET actual_size=5",
  "UPDATE dcc_legacy_source_name_evidence SET expected_sha256=REPEAT('z',64),actual_sha256=REPEAT('z',64)",
  "UPDATE infra_file SET config_id=NULL WHERE id=100",
  "UPDATE infra_file SET name=NULL WHERE id=100",
  "UPDATE infra_file SET path='changed/object' WHERE id=100",
  "UPDATE infra_file SET size=NULL WHERE id=100",
  "UPDATE infra_file SET name='sop.pdf' WHERE id=100",
  "UPDATE dcc_controlled_file SET source_sha256=NULL WHERE id=20",
  "UPDATE dcc_controlled_file SET source_file_id=101 WHERE id=20",
  "UPDATE dcc_controlled_file SET tenant_id=2 WHERE id=20",
  "UPDATE dcc_source_name_reservation SET verification_scope_id=NULL",
  "UPDATE dcc_source_name_reservation SET active=0",
  "UPDATE dcc_controlled_file_master SET normalized_file_number='inferred-number' WHERE id=10"
 })
 void anyMissingProofOrActualIdentityDriftKeepsRealUploadGuardClosed(String mutation) {
  legacyFixture();assertEquals(0,claims.countUnresolvedNames(1L));jdbc.update(mutation);
  assertEquals(1,claims.countUnresolvedNames(1L));
  assertThrows(IllegalStateException.class,()->names.preflightNewSourceName(99L,2L,5L,6L,"SAFE.pdf","scope","a".repeat(64)));
 }
 @Test void oldExactNameBlocksNewEvenSameMasterAndMissingFormalNumberStillRejects() {
  legacyFixture();formalLegacyIdentity();
  assertThrows(RuntimeException.class,()->names.claimIdentity(1L,"SOP.pdf",5L,6L,"N-1",10L));
  assertThrows(RuntimeException.class,()->names.preflightNewSourceName(99L,2L,5L,6L,"SOP.pdf","scope","a".repeat(64)));
  newMaster(11,7,"N-2");
  names.claimIdentity(1L,"sop.pdf",7L,6L,"N-2",11L);
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE source_original_file_name='sop.pdf'",Integer.class));
 }
 @Test void exactOwnHistoricalSourceProjectionNeverWritesOldRowAndForeignSelectedRejects() {
  legacyFixture();formalLegacyIdentity();var selected=files.selectById(20L);
  assertEquals("SOP.pdf",names.requireSourceName(selected));assertNull(selected.getSourceOriginalFileName());
  names.claimExistingVersion(1L,"SOP.pdf",5L,6L,"N-1",10L,20L);
  assertNull(files.selectById(20L).getSourceOriginalFileName());
  assertNull(jdbc.queryForObject("SELECT source_original_file_name FROM dcc_controlled_file_name_claim WHERE id=7",String.class));
  newMaster(11,7,"N-2");assertThrows(RuntimeException.class,()->names.claimExistingVersion(1L,"SOP.pdf",7L,6L,"N-2",11L,20L));
  selected.setSourceFileId(999L);assertThrows(IllegalArgumentException.class,()->names.requireSourceName(selected));
 }
 @Test void legacyVerificationDoesNotInventMissingFormalIdentity() {
  legacyFixture();assertThrows(IllegalArgumentException.class,()->names.claimExistingVersion(1L,"SOP.pdf",null,null,null,10L,20L));
 }
 @Test void unknownRawClaimOutsideSealedScopeStillBlocksNew() {
  legacyFixture();jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,master_id,normalized_name) VALUES(1,99,'unverified')");
  assertEquals(1,claims.countUnresolvedNames(1L));
 }
 @Test void preparedOtherScopeCannotContaminateVerifiedVersionOrAuthorizeAnUnverifiedName() {
  legacyFixture();formalLegacyIdentity();
  jdbc.update("INSERT INTO dcc_legacy_source_name_scope(id,tenant_id,scope_id,manifest_sha256,facts_sha256,bytes_receipt_sha256,user_decision_sha256,scope_identity_sha256,status,claim_count,version_count,source_count,edge_count,name_count,verified_at,actor_id,reason,request_id) SELECT 2,tenant_id,'prepared',?,facts_sha256,bytes_receipt_sha256,user_decision_sha256,scope_identity_sha256,'PREPARED',claim_count,version_count,source_count,edge_count,name_count,verified_at,actor_id,reason,'prepared' FROM dcc_legacy_source_name_scope WHERE id=1","9".repeat(64));
  jdbc.update("INSERT INTO dcc_legacy_source_name_evidence(tenant_id,verification_scope_id,legacy_claim_id,legacy_master_id,controlled_file_id,source_file_id,config_id,source_path,storage_type,storage_endpoint,storage_bucket,storage_region,storage_path_style,version_no,source_original_file_name,expected_sha256,actual_sha256,expected_size,actual_size,bytes_status,expected_version_count,claim_normalized_name,claim_project_id,claim_leaf_id,claim_number,master_project_id,master_leaf_id,master_number,metadata_identity_sha256,preimage_sha256,proof_row_sha256) SELECT tenant_id,2,legacy_claim_id,legacy_master_id,controlled_file_id,source_file_id,config_id,source_path,storage_type,storage_endpoint,storage_bucket,storage_region,storage_path_style,version_no,'UNVERIFIED.pdf',expected_sha256,actual_sha256,expected_size,actual_size,bytes_status,expected_version_count,claim_normalized_name,claim_project_id,claim_leaf_id,claim_number,master_project_id,master_leaf_id,master_number,metadata_identity_sha256,preimage_sha256,proof_row_sha256 FROM dcc_legacy_source_name_evidence WHERE verification_scope_id=1");
  assertEquals(0,claims.countUnresolvedNames(1L));
  assertEquals(java.util.List.of("SOP.pdf"),reservations.verifiedNames(1L,10L,20L));
  assertEquals(0,reservations.verifiedOwnerName(1L,10L,"UNVERIFIED.pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
 }
 @Test void registryAndModernClaimRollbackTogetherOnLaterBusinessFailure() {
  legacyFixture();newMaster(11,7,"N-2");
  assertThrows(IllegalStateException.class,()->transaction().execute(s->{names.claimIdentity(1L,"SAFE.pdf",7L,6L,"N-2",11L);throw new IllegalStateException("later signature failed");}));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_source_name_reservation",Integer.class));
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"endpoint","bucket","region","enablePathStyleAccess","storage","deleted"})
 void sourceStorageLocatorDriftCannotReuseOldObjectProof(String field) {
  legacyFixture();
  if("storage".equals(field))jdbc.update("UPDATE infra_file_config SET storage=10 WHERE id=28");
  else if("deleted".equals(field))jdbc.update("UPDATE infra_file_config SET deleted=1 WHERE id=28");
  else {var config=cn.hutool.json.JSONUtil.parseObj(jdbc.queryForObject("SELECT config FROM infra_file_config WHERE id=28",String.class));config.set(field,"enablePathStyleAccess".equals(field)?false:"changed");jdbc.update("UPDATE infra_file_config SET config=? WHERE id=28",config.toString());}
  assertEquals(1,claims.countUnresolvedNames(1L));
  assertThrows(IllegalStateException.class,()->names.requireSourceName(files.selectById(20L)));
 }
 @Test void storageCredentialRotationDoesNotChangeVerifiedObjectLocation() {
  legacyFixture();var config=cn.hutool.json.JSONUtil.parseObj(jdbc.queryForObject("SELECT config FROM infra_file_config WHERE id=28",String.class));
  config.set("accessKey","new-test-credential");config.set("accessSecret","new-test-secret");
  jdbc.update("UPDATE infra_file_config SET config=? WHERE id=28",config.toString());
  assertEquals(0,claims.countUnresolvedNames(1L));assertEquals("SOP.pdf",names.requireSourceName(files.selectById(20L)));
 }
 @Test void mixedModernAndLegacyIdentityRetentionCoversBothWithoutChangingOldClaim() {
  legacyFixture();
  jdbc.update("INSERT INTO dcc_controlled_file_name_claim(id,tenant_id,master_id,normalized_name,source_original_file_name,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(8,1,10,'MODERN.pdf','MODERN.pdf',5,6,'N-1')");
  jdbc.update("INSERT INTO dcc_source_name_reservation(tenant_id,source_original_file_name,reservation_kind,modern_claim_id,modern_master_id,generation,active) VALUES(1,'MODERN.pdf','MODERN',8,10,1,1)");
  var obsolete=java.time.LocalDateTime.of(2026,10,3,12,0);var deadline=obsolete.plusYears(20);
  names.retainObsoleteIdentity(1L,10L,obsolete,deadline);
  assertEquals(deadline,jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE id=8",java.sql.Timestamp.class).toLocalDateTime());
  assertNull(jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE id=7",java.sql.Timestamp.class));
  jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20");
  jdbc.update("UPDATE dcc_controlled_file_name_claim SET retain_until=? WHERE id=8",deadline.plusDays(1));
  assertFalse(names.releaseExpiredIdentity(1L,10L,deadline));
  assertNull(jdbc.queryForObject("SELECT released_time FROM dcc_legacy_source_name_evidence",java.sql.Timestamp.class));
  assertTrue(names.releaseExpiredIdentity(1L,10L,deadline.plusDays(1)));
  assertEquals(1,jdbc.queryForObject("SELECT deleted FROM dcc_controlled_file_name_claim WHERE id=8",Integer.class));
  assertEquals(0,jdbc.queryForObject("SELECT deleted FROM dcc_controlled_file_name_claim WHERE id=7",Integer.class));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_source_name_reservation WHERE active=1",Integer.class));
 }
 @Test void concurrentNewClaimsHaveOneWinnerInActualRegistryAndModernConstraint() throws Exception {
  legacyFixture();newMaster(11,7,"N-2");newMaster(12,8,"N-3");
  var latch=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
  try {var a=pool.submit(()->attempt(11,7,"N-2",latch));var b=pool.submit(()->attempt(12,8,"N-3",latch));latch.countDown();assertEquals(1,(a.get(20,java.util.concurrent.TimeUnit.SECONDS)?1:0)+(b.get(20,java.util.concurrent.TimeUnit.SECONDS)?1:0));}
  finally {pool.shutdownNow();}
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_source_name_reservation WHERE source_original_file_name='SAFE.pdf'",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE source_original_file_name='SAFE.pdf'",Integer.class));
 }
 boolean attempt(long id,long project,String number,java.util.concurrent.CountDownLatch start) throws Exception {TenantContextHolder.setTenantId(1L);try {start.await();names.claimIdentity(1L,"SAFE.pdf",project,6L,number,id);return true;}catch(cn.iocoder.yudao.framework.common.exception.ServiceException rejected){assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NAME_EXISTS.getCode(),rejected.getCode());return false;}finally{TenantContextHolder.clear();}}

 @Test void twentyCalendarYearsAndAllLegacyOwnersMustExpireBeforeNameAndOriginalNumberReuse() {
  legacyFixture();formalLegacyIdentity();
  // Exact real migration shape: old claim formal fields remain NULL, formal number belongs to old Master.
  jdbc.update("UPDATE dcc_controlled_file_name_claim SET dcc_project_code_id=NULL,file_type_taxonomy_leaf_id=NULL,normalized_file_number=NULL WHERE id=7");
  jdbc.update("UPDATE dcc_legacy_source_name_evidence SET claim_project_id=NULL,claim_leaf_id=NULL,claim_number=NULL");
  newMaster(12,5,"N-1");
  assertThrows(RuntimeException.class,()->names.claimIdentity(1L,"other.pdf",5L,6L,"N-1",12L));
  jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status) VALUES(11,1,2,4,'other-legacy-template','OLD-2','ACTIVE_CHAIN')");
  jdbc.update("INSERT INTO dcc_controlled_file_name_claim(id,tenant_id,master_id,normalized_name) VALUES(8,1,11,'other-legacy-template')");
  jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,requester_id,submitter_id,change_type,source_sha256) VALUES(21,1,11,2,4,100,100,'other-legacy-template','other','OLD-2','A/1','OBSOLETE',99,99,'NEW',?)","a".repeat(64));
  jdbc.update("INSERT INTO dcc_legacy_source_name_evidence(tenant_id,verification_scope_id,legacy_claim_id,legacy_master_id,controlled_file_id,source_file_id,config_id,source_path,storage_type,storage_endpoint,storage_bucket,storage_region,storage_path_style,version_no,source_original_file_name,expected_sha256,actual_sha256,expected_size,actual_size,bytes_status,expected_version_count,claim_normalized_name,metadata_identity_sha256,preimage_sha256,proof_row_sha256) VALUES(1,1,8,11,21,100,28,'sealed/object',20,'http://127.0.0.1:9000','originals','us-east-1',1,'A/1','SOP.pdf',?,?,4,4,'MATCH',1,'other-legacy-template',?,?,?)","a".repeat(64),"a".repeat(64),"b".repeat(64),"c".repeat(64),"d".repeat(64));
  jdbc.update("UPDATE dcc_legacy_source_name_scope SET claim_count=2,version_count=2,edge_count=2");
  jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20");
  var obsolete=java.time.LocalDateTime.of(2024,2,29,12,0);var end=obsolete.plusYears(20);
  names.retainObsoleteIdentity(1L,10L,obsolete,end);
  assertFalse(names.releaseExpiredIdentity(1L,10L,end.minusSeconds(1)));
  assertTrue(names.releaseExpiredIdentity(1L,10L,end));
  assertEquals(1,reservations.readName(1L,"SOP.pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8)).getActive());
  assertFalse(names.releaseExpiredIdentity(1L,11L,end)); // Unknown real obsolescence remains occupied.
  names.retainObsoleteIdentity(1L,11L,obsolete,end);
  assertTrue(names.releaseExpiredIdentity(1L,11L,end));
  assertEquals(0,claims.countUnresolvedNames(1L));
  names.claimIdentity(1L,"SOP.pdf",5L,6L,"N-1",12L);
  assertEquals(0,jdbc.queryForObject("SELECT deleted FROM dcc_controlled_file_name_claim WHERE id=7",Integer.class));
  assertNull(jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE id=7",java.sql.Timestamp.class));
  assertEquals("MODERN",reservations.readName(1L,"SOP.pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8)).getReservationKind());
 }
 @Test void activeLegacyReservationAndNewShareTheRealExactRegistryLock() throws Exception {
  legacyFixture();newMaster(11,7,"N-2");
  var locked=new java.util.concurrent.CountDownLatch(1);var started=new java.util.concurrent.CountDownLatch(1);var unlock=new java.util.concurrent.CountDownLatch(1);
  var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
  try {
   var activation=pool.submit(()->{TenantContextHolder.setTenantId(1L);try{return transaction().execute(s->{assertNotNull(reservations.lockName(1L,"SOP.pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8)));locked.countDown();try{assertTrue(started.await(10,java.util.concurrent.TimeUnit.SECONDS));assertTrue(unlock.await(10,java.util.concurrent.TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}return true;});}finally{TenantContextHolder.clear();}});
   assertTrue(locked.await(10,java.util.concurrent.TimeUnit.SECONDS));
   var creation=pool.submit(()->{TenantContextHolder.setTenantId(1L);try{started.countDown();names.claimIdentity(1L,"SOP.pdf",7L,6L,"N-2",11L);return true;}catch(RuntimeException rejected){return false;}finally{TenantContextHolder.clear();}});
   assertTrue(started.await(10,java.util.concurrent.TimeUnit.SECONDS));unlock.countDown();assertTrue(activation.get(20,java.util.concurrent.TimeUnit.SECONDS));assertFalse(creation.get(20,java.util.concurrent.TimeUnit.SECONDS));
  } finally {unlock.countDown();pool.shutdownNow();}
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_source_name_reservation",Integer.class));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE source_original_file_name IS NOT NULL",Integer.class));
 }
 public static String jsonExtract(String value,String path){if(value==null)return null;var n=cn.hutool.json.JSONUtil.parseObj(value).get(path.substring(2));return n==null?null:cn.hutool.json.JSONUtil.toJsonStr(n);}
 public static String jsonUnquote(String value){if(value==null)return null;if(value.startsWith("\""))return cn.hutool.json.JSONUtil.parseArray("["+value+"]").getStr(0);return value;}
 @AfterEach void cleanupOwnSidecars(){jdbc.update("DELETE FROM dcc_legacy_source_name_evidence");jdbc.update("DELETE FROM dcc_source_name_reservation");jdbc.update("DELETE FROM dcc_legacy_source_name_scope");jdbc.update("DELETE FROM infra_file WHERE id=100");jdbc.update("DELETE FROM infra_file_config WHERE id=28");}
}
