package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.service.file.DccPublicUploadPlacementService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({DccPublicUploadPlacementService.class,cn.iocoder.yudao.module.dcc.service.file.DccProjectFolderStorageService.class,
 cn.iocoder.yudao.module.dcc.service.file.DccStorageMappingMutationGuard.class,
 cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionServiceImpl.class})
@Sql(scripts="/sql/g48_storage_mapping.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class G48StorageProjectionTest extends DccProjectFormalCombinationTest {
 @Resource DccPublicUploadPlacementService publicUpload;
 @Resource cn.iocoder.yudao.module.dcc.service.file.DccStorageMappingMutationGuard mutationGuard;
 @Resource cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService directoryAccess;
 @org.junit.jupiter.api.AfterEach void clearOwnStorageFixture(){
  jdbc.update("DELETE FROM dcc_file_directory WHERE id IN (SELECT storage_directory_id FROM dcc_project_folder_storage_mapping WHERE tenant_id=1)");
  jdbc.update("DELETE FROM dcc_project_folder_storage_mapping WHERE tenant_id=1");
  jdbc.update("DELETE FROM dcc_category_directory_binding WHERE tenant_id=1 AND category_id=1");
  jdbc.update("DELETE FROM dcc_file_category WHERE tenant_id=1 AND id=1 AND code='STORAGE-CAT'");
  jdbc.update("DELETE FROM dcc_file_directory WHERE tenant_id=1 AND id IN (101,10200) AND code IN ('STORAGE-101','STORAGE-10200')");
 }
 @org.springframework.test.context.bean.override.mockito.MockitoBean
 cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService taxonomy;
 @org.springframework.test.context.bean.override.mockito.MockitoBean
 cn.iocoder.yudao.module.dcc.service.file.DccControlledFileCategoryPermissionSupport categoryPermissions;
 @BeforeEach void realActorDirectory(){
  when(users.getUser(7L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
   .setId(7L).setTenantId(1L).setStatus(0).setUsername("storage-owner").setNickname("账号7"));
  when(taxonomy.resolveActiveCategoryId(8803L)).thenReturn(1L);
  when(categoryPermissions.hasCategoryPermission(any(),any(),any())).thenReturn(true);
  directory(101L,1L,true);
  jdbc.update("INSERT INTO dcc_file_category(id,tenant_id,code,name,source,lifecycle_stage,active,file_type_taxonomy_id,approval_source_mode) VALUES(1,1,'STORAGE-CAT','类别','LOCAL','CONTROLLED',1,8803,'PUBLIC_MATRIX')");
  jdbc.update("INSERT INTO dcc_category_directory_binding(category_id,directory_id,active,tenant_id) VALUES(1,101,1,1)");
 }
 DccControlledFileSubmitReqVO request(Long project,Long folder){
  var r=new DccControlledFileSubmitReqVO();r.setDccProjectCodeId(project);r.setProjectFolderId(folder);
  r.setCategoryId(1L);r.setFileTypeTaxonomyId(8803L);r.setProcessType("CONTROLLED_FILE");r.setChangeType("NEW");
  r.setProjectFolderChangeReason("正式项目文件夹自动存储");return r;
 }
 @Test void firstNormalUploadNeedsOnlyLogicalFolderAndDerivesStorageBeforeFileCreation(){
  var p=project("STORAGE-PROJECT",7L);var f=folder(p.getId(),"quality");
  g12UploadPolicy();
  var request=new DccControlledFileSubmitReqVO();request.setDccProjectCodeId(p.getId());request.setProjectFolderId(f.getId());
  request.setCategoryId(1L);request.setFileTypeTaxonomyId(8803L);request.setProcessType("CONTROLLED_FILE");request.setChangeType("NEW");
  request.setProjectFolderChangeReason("正式项目文件夹自动存储");
  Long id=tx(()->publicUpload.create(7L,request,()->{
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping",Integer.class));
   Long storage=jdbc.queryForObject("SELECT storage_directory_id FROM dcc_project_folder_storage_mapping",Long.class);
   return file(p.getId(),storage,"new-real-file.pdf").getId();
  }));
  var placement=placements.require(7L,p.getId(),id);
  assertNotEquals(101L,placement.getStorageDirectoryId());
  assertEquals(1,events.selectList().size());assertNull(request.getDirectoryId());
 }
 @Test void sameMappingReusesExactLeafAndLateAuditFailureRollsBackFirstCreation(){
  int originalFiles=files.selectList().size();
  var p=project("REUSE",7L);var f=folder(p.getId(),"quality");g12UploadPolicy();
  var request=request(p.getId(),f.getId());
  Long first=tx(()->publicUpload.create(7L,request,ctx->file(p.getId(),ctx.storageDirectoryId,"one.pdf").getId()));
  Long second=tx(()->publicUpload.create(7L,request,ctx->file(p.getId(),ctx.storageDirectoryId,"two.pdf").getId()));
  assertEquals(files.selectById(first).getDirectoryId(),files.selectById(second).getDirectoryId());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping",Integer.class));
  assertEquals(2,events.selectList().size());
  assertTrue(events.selectList().get(0).getAfterStateJson().contains("mappingCreated"));
  var other=folder(p.getId(),"rollback");var before=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_file_directory",Integer.class);
  policies.deleteById(policies.selectActive(1L,G12_UPLOAD_OPERATION).getId());
  assertThrows(RuntimeException.class,()->tx(()->publicUpload.create(7L,request(p.getId(),other.getId()),
        ctx->file(p.getId(),ctx.storageDirectoryId,"rollback.pdf").getId())));
  assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_file_directory",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping",Integer.class));
  assertEquals(originalFiles+2,files.selectList().size());
 }
 @Test void callerDirectoryAndChangedBaseCannotReplaceTheDerivedStorage(){
  var p=project("EXPLICIT-REJECT",7L);var f=folder(p.getId(),"quality");g12UploadPolicy();
  var r=request(p.getId(),f.getId());r.setDirectoryId(101L);
  assertThrows(RuntimeException.class,()->tx(()->publicUpload.create(7L,r,ctx->{fail("caller NAS must not reach File");return 1L;})));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping",Integer.class));
  var allowed=request(p.getId(),f.getId());
  tx(()->publicUpload.create(7L,allowed,ctx->file(p.getId(),ctx.storageDirectoryId,"kept.pdf").getId()));
  directory(10200L,1L,true);jdbc.update("UPDATE dcc_category_directory_binding SET directory_id=10200 WHERE category_id=1");
  assertThrows(RuntimeException.class,()->tx(()->publicUpload.create(7L,allowed,ctx->{fail("changed base must not reach File");return 1L;})));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping",Integer.class));
 }
 @Test void mappedBaseLeafAndFolderCannotBeDeletedOrReboundAndRightsFollowCurrentBase(){
  var p=project("GUARDED-STORAGE",7L);var f=folder(p.getId(),"protected");g12UploadPolicy();
  Long id=tx(()->publicUpload.create(7L,request(p.getId(),f.getId()),ctx->file(p.getId(),ctx.storageDirectoryId,"guarded.pdf").getId()));
  Long leaf=files.selectById(id).getDirectoryId();
  assertThrows(RuntimeException.class,()->tx(()->{mutationGuard.requireCategoryUnmapped(1L);return null;}));
  assertThrows(RuntimeException.class,()->tx(()->{mutationGuard.requireDirectoriesUnmapped(java.util.List.of(101L));return null;}));
  assertThrows(RuntimeException.class,()->tx(()->{mutationGuard.requireDirectoriesUnmapped(java.util.List.of(leaf));return null;}));
  assertThrows(RuntimeException.class,()->tx(()->{mutationGuard.requireParentNotMappedLeaf(leaf);return null;}));
  assertThrows(RuntimeException.class,()->tx(()->{mutationGuard.requireFolderUnmapped(f.getId());return null;}));
  jdbc.update("INSERT INTO dcc_directory_access_rule(directory_id,subject_type,subject_id,can_query,can_preview,can_download,active,tenant_id) VALUES(101,'USER',7,1,1,1,1,1)");
  assertTrue(directoryAccess.getAuthorizedDirectoryIds(7L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.DOWNLOAD).contains(leaf));
  jdbc.update("UPDATE dcc_file_directory SET access_rule_manually_bound=1 WHERE tenant_id=1 AND id=?",leaf);
  assertFalse(directoryAccess.getAuthorizedDirectoryIds(7L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.DOWNLOAD).contains(leaf));
  jdbc.update("UPDATE dcc_file_directory SET access_rule_manually_bound=0 WHERE tenant_id=1 AND id=?",leaf);
  jdbc.update("UPDATE dcc_directory_access_rule SET active=0 WHERE tenant_id=1 AND directory_id=101 AND subject_id=7");
  assertFalse(directoryAccess.getAuthorizedDirectoryIds(7L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.DOWNLOAD).contains(leaf));
  jdbc.update("DELETE FROM dcc_directory_access_rule WHERE tenant_id=1 AND directory_id=101 AND subject_id=7");
 }
 @Test void lockedCurrentCategoryRejectsDisabledOrChangedTypeDespiteEarlierLookup(){
  var p=project("CURRENT-CATEGORY",7L);var f=folder(p.getId(),"current");
  jdbc.update("UPDATE dcc_file_category SET active=0 WHERE tenant_id=1 AND id=1");
  assertThrows(RuntimeException.class,()->tx(()->publicUpload.create(7L,request(p.getId(),f.getId()),ctx->{fail("disabled category must not create");return 1L;})));
  jdbc.update("UPDATE dcc_file_category SET active=1,file_type_taxonomy_id=9999 WHERE tenant_id=1 AND id=1");
  assertThrows(RuntimeException.class,()->tx(()->publicUpload.create(7L,request(p.getId(),f.getId()),ctx->{fail("changed type must not create");return 1L;})));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping",Integer.class));
 }

 private static final String G12_UPLOAD_OPERATION = "dcc.project-file-placement.upload-bind";

 private int g12PlacementCount(Long projectId) {
  return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement WHERE project_code_id=? AND tenant_id=1",
    Integer.class, projectId);
 }

 private java.util.Map<String, Object> g12DatabaseSnapshot(Long projectId) {
  var result = new java.util.LinkedHashMap<String, Object>();
  result.put("files", jdbc.queryForList("SELECT * FROM dcc_controlled_file WHERE tenant_id=1 AND dcc_project_code_id=? ORDER BY id", projectId));
  result.put("masters", jdbc.queryForList("SELECT * FROM dcc_controlled_file_master WHERE tenant_id=1 AND dcc_project_code_id=? ORDER BY id", projectId));
  result.put("placements", jdbc.queryForList("SELECT * FROM dcc_project_file_placement WHERE tenant_id=1 AND project_code_id=? ORDER BY id", projectId));
  result.put("mappings", jdbc.queryForList("SELECT * FROM dcc_project_folder_storage_mapping WHERE tenant_id=1 AND project_code_id=? ORDER BY id", projectId));
  result.put("directories", jdbc.queryForList("SELECT * FROM dcc_file_directory WHERE tenant_id=1 ORDER BY id"));
  result.put("ledger", jdbc.queryForList("SELECT * FROM gxp_audit_event WHERE tenant_id=1 ORDER BY id"));
  result.put("sequence", jdbc.queryForList("SELECT * FROM gxp_audit_ledger_sequence WHERE tenant_id=1"));
  return result;
 }

 private void g12UploadPolicy() {
  if (policies.selectActive(1L, "dcc.project-file-placement.bind") == null) policy("dcc.project-file-placement.bind");
  policy(G12_UPLOAD_OPERATION);
  var current = policies.selectActive(1L, G12_UPLOAD_OPERATION);
  current.setReasonPolicy("OPTIONAL_TEXT");
  current.setSubjectType("DCC_PROJECT_FILE_PLACEMENT");
  current.setActionType("CREATE");
  current.setStatePolicy("ABSENT_TO_PRESENT");
  policies.updateById(current);
 }

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.NullAndEmptySource
 @org.junit.jupiter.params.provider.ValueSource(strings={" ", " \t\n "})
 void g12BlankUploadReasonArchivesExactFactsAndRealActorWithoutInventingReason(String reason) {
  g12UploadPolicy();
  var project = project("G12-EMPTY-" + java.util.UUID.randomUUID(), 7L);
  var folder = folder(project.getId(), "quality");
  var request = request(project.getId(), folder.getId());
  request.setProjectFolderChangeReason(reason);
  var started = java.time.LocalDateTime.now().minusSeconds(1);
  Long id = tx(() -> publicUpload.create(7L, request,
    storage -> file(project.getId(), storage.storageDirectoryId, "g12-empty.pdf").getId()));
  var placement = placements.require(7L, project.getId(), id);
  assertEquals(folder.getId(), placement.getProjectFolderId());
  assertEquals(files.selectById(id).getDirectoryId(), placement.getStorageDirectoryId());
  assertEquals(1, events.selectList().size());
  var event = events.selectList().get(0);
  assertEquals(G12_UPLOAD_OPERATION, event.getOperationId());
  assertEquals("", event.getReason(), "NOT NULL audit column stores the actual absence as empty text");
  assertEquals(7L, event.getActorId());
  assertEquals("storage-owner", event.getActorUsername());
  assertEquals(1L, event.getTenantId());
  assertEquals("DCC_PROJECT_FILE_PLACEMENT:" + placement.getId(), event.getSubjectId());
  assertEquals("ABSENT", event.getBeforeState());
  assertEquals("{}", event.getBeforeStateJson());
  assertEquals("PRESENT", event.getAfterState());
  var state = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(event.getAfterStateJson(), java.util.Map.class);
  var fact = (java.util.Map<?, ?>) state.get("placement");
  assertEquals(id.toString(), String.valueOf(fact.get("controlledFileId")));
  assertEquals(project.getId().toString(), String.valueOf(fact.get("projectCodeId")));
  assertEquals(folder.getId().toString(), String.valueOf(fact.get("projectFolderId")));
  assertEquals(placement.getStorageDirectoryId().toString(), String.valueOf(state.get("storageDirectoryId")));
  assertEquals(Boolean.TRUE, state.get("mappingCreated"));
  assertNotNull(event.getEventHash());
  assertNotNull(event.getServerOccurredAt());
  assertFalse(event.getServerOccurredAt().isBefore(started));
  assertNull(event.getSignatureRecordId(), "upload placement is not an electronic signature");
 }

 @Test void g12OptionalReasonRetains500CharactersAndRejects501BeforeStorageOrFileCreation() {
  g12UploadPolicy();
  var project = project("G12-LENGTH", 7L);
  var folder = folder(project.getId(), "quality");
  var request = request(project.getId(), folder.getId());
  String actual = "真".repeat(500);
  request.setProjectFolderChangeReason(" " + actual + " ");
  tx(() -> publicUpload.create(7L, request,
    storage -> file(project.getId(), storage.storageDirectoryId, "g12-500.pdf").getId()));
  assertEquals(actual, events.selectList().get(0).getReason());
  int mappings = jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping", Integer.class);
  int fileCount = files.selectList().size();
  var called = new java.util.concurrent.atomic.AtomicBoolean();
  request.setProjectFolderChangeReason("长".repeat(501));
  assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request,
    storage -> { called.set(true); return 1L; })));
  assertFalse(called.get());
  assertEquals(fileCount, files.selectList().size());
  assertEquals(mappings, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping", Integer.class));
  assertEquals(1, events.selectList().size());
 }

 @Test void g12EmptyReasonCannotAuthorizeMissingForeignInactiveOrWrongTenantFolder() {
  g12UploadPolicy();
  var project = project("G12-LOCATION", 7L);
  var other = project("G12-FOREIGN", 7L);
  var foreign = folder(other.getId(), "foreign");
  var inactive = folder(project.getId(), "inactive");
  jdbc.update("UPDATE dcc_project_folder SET active=0 WHERE id=?", inactive.getId());
  var wrongTenant = folder(project.getId(), "wrong-tenant");
  jdbc.update("UPDATE dcc_project_folder SET tenant_id=2 WHERE id=?", wrongTenant.getId());
  var called = new java.util.concurrent.atomic.AtomicBoolean();
  for (Long folderId : new Long[]{null, foreign.getId(), inactive.getId(), wrongTenant.getId()}) {
   var request = request(project.getId(), folderId);
   request.setProjectFolderChangeReason(null);
   var error = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
     () -> tx(() -> publicUpload.create(7L, request, storage -> { called.set(true); return 1L; })));
   assertEquals(cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.FOLDER_INVALID.getCode(), error.getCode());
  }
  assertFalse(called.get());
  assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping", Integer.class));
  assertTrue(events.selectList().isEmpty());
 }

 @Test void g12EmptyReasonCannotBypassRevokedProjectEditorAccess() {
  g12UploadPolicy();
  var project = project("G12-VIEW-ONLY", 7L);
  var folder = folder(project.getId(), "quality");
  jdbc.update("UPDATE dcc_project_access_rule SET access_level='VIEW' WHERE dcc_project_code_id=?", project.getId());
  jdbc.update("UPDATE dcc_project_code SET project_leader_user_id=8 WHERE id=?", project.getId());
  var request = request(project.getId(), folder.getId());
  request.setProjectFolderChangeReason("");
  var called = new java.util.concurrent.atomic.AtomicBoolean();
  assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request,
    storage -> { called.set(true); return 1L; })));
  assertFalse(called.get());
  assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping", Integer.class));
 }

 @Test void g12MissingIndependentAuditPolicyRollsBackFileMappingAndPlacement() {
  policy("dcc.project-file-placement.bind");
  var project = project("G12-NO-UPLOAD-POLICY", 7L);
  var folder = folder(project.getId(), "quality");
  var request = request(project.getId(), folder.getId());
  request.setProjectFolderChangeReason("本次编制人实际填写的归档说明");
  var before = g12DatabaseSnapshot(project.getId());
  var created = new java.util.concurrent.atomic.AtomicLong();
  assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request, storage -> {
   long id = file(project.getId(), storage.storageDirectoryId, "g12-policy-rollback.pdf").getId();
   created.set(id);
   return id;
  })));
  assertTrue(created.get() > 0, "failure must be at the real ledger boundary after file creation");
  assertNull(files.selectById(created.get()));
  assertEquals(before, g12DatabaseSnapshot(project.getId()), "own project facts and exact directory/ledger snapshots must all roll back");
  assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping", Integer.class));
  assertEquals(0, g12PlacementCount(project.getId()));
  assertTrue(events.selectList().isEmpty());
  g12UploadPolicy();
  request.setProjectFolderChangeReason(null);
  Long retried = tx(() -> publicUpload.create(7L, request,
    storage -> file(project.getId(), storage.storageDirectoryId, "g12-policy-rollback.pdf").getId()));
  assertNotNull(placements.require(7L, project.getId(), retried));
  assertEquals(G12_UPLOAD_OPERATION, events.selectList().get(0).getOperationId());
  assertEquals("", events.selectList().get(0).getReason());
 }

 @Test void g12EmptyReasonReplayDoesNotRewriteAuditOrMoveAnExistingPlacement() {
  g12UploadPolicy();
  var project = project("G12-REPLAY", 7L);
  var folder = folder(project.getId(), "original");
  var request = request(project.getId(), folder.getId());
  request.setProjectFolderChangeReason("编制人真实填写说明");
  Long id = tx(() -> publicUpload.create(7L, request,
    storage -> file(project.getId(), storage.storageDirectoryId, "g12-replay.pdf").getId()));
  var original = events.selectList().get(0);
  request.setProjectFolderChangeReason(null);
  assertEquals(id, tx(() -> publicUpload.create(7L, request, storage -> id)));
  assertEquals(1, events.selectList().size());
  assertEquals(original.getEventHash(), events.selectList().get(0).getEventHash());
  assertEquals("编制人真实填写说明", events.selectList().get(0).getReason());
  var other = folder(project.getId(), "other");
  request.setProjectFolderId(other.getId());
  assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request, storage -> id)));
  assertEquals(folder.getId(), placements.require(7L, project.getId(), id).getProjectFolderId());
  assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping", Integer.class));
  assertEquals(1, events.selectList().size());
 }

 @Test void g12LateCallerFailureRollsBackAndEmptyReasonCanRetry() {
  g12UploadPolicy();
  var project = project("G12-LATE-FAILURE", 7L);
  var folder = folder(project.getId(), "quality");
  var request = request(project.getId(), folder.getId());
  request.setProjectFolderChangeReason("本次编制人实际填写的归档说明");
  var before = g12DatabaseSnapshot(project.getId());
  var created = new java.util.concurrent.atomic.AtomicLong();
  assertThrows(IllegalStateException.class, () -> tx(() -> {
   created.set(publicUpload.create(7L, request,
     storage -> file(project.getId(), storage.storageDirectoryId, "g12-retry.pdf").getId()));
   assertEquals(1, events.selectList().size());
   throw new IllegalStateException("later workflow failure");
  }));
  assertNull(files.selectById(created.get()));
  assertEquals(before, g12DatabaseSnapshot(project.getId()), "late caller failure must roll back all actual DB writes");
  assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping", Integer.class));
  assertEquals(0, g12PlacementCount(project.getId()));
  assertTrue(events.selectList().isEmpty());
  request.setProjectFolderChangeReason("");
  Long retried = tx(() -> publicUpload.create(7L, request,
    storage -> file(project.getId(), storage.storageDirectoryId, "g12-retry.pdf").getId()));
  assertNotNull(placements.require(7L, project.getId(), retried));
  assertEquals(1, events.selectList().size());
  assertEquals("", events.selectList().get(0).getReason());
 }

 @Test void g12ManualFolderMaintenanceAndLegacyBindingStillRequireTrueReasons() {
  policy("dcc.project-file-placement.bind");
  policy("dcc.project-folder.create");
  var project = project("G12-MANUAL", 7L);
  var folder = folder(project.getId(), "quality");
  var file = file(project.getId(), 101L, "g12-manual.pdf");
  for (String reason : new String[]{null, "", " \t "}) {
   assertThrows(RuntimeException.class, () -> tx(() -> placements.bind(7L, project.getId(), folder.getId(), file.getId(), 101L, reason)));
   assertThrows(RuntimeException.class, () -> folderMaintenance.save(7L, project.getId(),
     new cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService.Save(null, 0L, "manual", 0, reason)));
  }
  assertTrue(events.selectList().isEmpty());
  assertEquals(1, folders.listByProject(project.getId()).size());
  tx(() -> placements.bind(7L, project.getId(), folder.getId(), file.getId(), 101L, "真实归档理由"));
  assertEquals("dcc.project-file-placement.bind", events.selectList().get(0).getOperationId());
  assertEquals("真实归档理由", events.selectList().get(0).getReason());
 }

 @Test void g12DerivedOptionalEntryCannotBeUsedWithoutServerStorageContext() {
  policy("dcc.project-file-placement.bind");
  g12UploadPolicy();
  var project = project("G12-NO-CONTEXT", 7L);
  var folder = folder(project.getId(), "quality");
  var file = file(project.getId(), 101L, "g12-no-context.pdf");
  assertThrows(RuntimeException.class, () -> tx(() -> placements.bindDerived(7L, project.getId(), folder.getId(),
    file.getId(), 101L, "不得绕过正式派生存储", null)));
  assertEquals(0, g12PlacementCount(project.getId()));
  assertTrue(events.selectList().isEmpty());
 }

 @Test void g12RealLedgerFailureAfterFileCreationRollsBackWithoutBeingMaskedByReasonValidation() {
  var project = project("G12-REAL-LEDGER-ROLLBACK", 7L);
  var folder = folder(project.getId(), "quality");
  var request = request(project.getId(), folder.getId());
  request.setProjectFolderChangeReason("本次编制人实际填写的归档说明");
  var before = g12DatabaseSnapshot(project.getId());
  var created = new java.util.concurrent.atomic.AtomicLong();
  assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request, storage -> {
   long id = file(project.getId(), storage.storageDirectoryId, "g12-real-ledger-rollback.pdf").getId();
   created.set(id);
   return id;
  })));
  assertTrue(created.get() > 0, "the actual file callback must execute before the real ledger refuses missing policy");
  assertNull(files.selectById(created.get()));
  assertEquals(before, g12DatabaseSnapshot(project.getId()), "real ledger refusal must roll back files/masters/placements/mappings/directories/ledger");
  assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping", Integer.class));
  assertEquals(0, g12PlacementCount(project.getId()));
  assertTrue(events.selectList().isEmpty());
  g12UploadPolicy();
  Long retried = tx(() -> publicUpload.create(7L, request,
    storage -> file(project.getId(), storage.storageDirectoryId, "g12-real-ledger-rollback.pdf").getId()));
  assertNotNull(placements.require(7L, project.getId(), retried));
  assertEquals(1, events.selectList().size());
 }
}
