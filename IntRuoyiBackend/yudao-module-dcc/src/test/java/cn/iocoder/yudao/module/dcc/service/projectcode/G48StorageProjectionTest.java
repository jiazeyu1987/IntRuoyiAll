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
  jdbc.update("INSERT INTO dcc_file_category(id,tenant_id,code,name,source,lifecycle_stage,active,file_type_taxonomy_id) VALUES(1,1,'STORAGE-CAT','类别','LOCAL','CONTROLLED',1,8803)");
  jdbc.update("INSERT INTO dcc_category_directory_binding(category_id,directory_id,active,tenant_id) VALUES(1,101,1,1)");
 }
 DccControlledFileSubmitReqVO request(Long project,Long folder){
  var r=new DccControlledFileSubmitReqVO();r.setDccProjectCodeId(project);r.setProjectFolderId(folder);
  r.setCategoryId(1L);r.setFileTypeTaxonomyId(8803L);r.setProcessType("CONTROLLED_FILE");r.setChangeType("NEW");
  r.setProjectFolderChangeReason("正式项目文件夹自动存储");return r;
 }
 @Test void firstNormalUploadNeedsOnlyLogicalFolderAndDerivesStorageBeforeFileCreation(){
  var p=project("STORAGE-PROJECT",7L);var f=folder(p.getId(),"quality");
  policy("dcc.project-file-placement.bind");
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
  var p=project("REUSE",7L);var f=folder(p.getId(),"quality");policy("dcc.project-file-placement.bind");
  var request=request(p.getId(),f.getId());
  Long first=tx(()->publicUpload.create(7L,request,ctx->file(p.getId(),ctx.storageDirectoryId,"one.pdf").getId()));
  Long second=tx(()->publicUpload.create(7L,request,ctx->file(p.getId(),ctx.storageDirectoryId,"two.pdf").getId()));
  assertEquals(files.selectById(first).getDirectoryId(),files.selectById(second).getDirectoryId());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping",Integer.class));
  assertEquals(2,events.selectList().size());
  assertTrue(events.selectList().get(0).getAfterStateJson().contains("mappingCreated"));
  var other=folder(p.getId(),"rollback");var before=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_file_directory",Integer.class);
  policies.deleteById(policies.selectList().get(0).getId());
  assertThrows(RuntimeException.class,()->tx(()->publicUpload.create(7L,request(p.getId(),other.getId()),
        ctx->file(p.getId(),ctx.storageDirectoryId,"rollback.pdf").getId())));
  assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_file_directory",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping",Integer.class));
  assertEquals(originalFiles+2,files.selectList().size());
 }
 @Test void callerDirectoryAndChangedBaseCannotReplaceTheDerivedStorage(){
  var p=project("EXPLICIT-REJECT",7L);var f=folder(p.getId(),"quality");policy("dcc.project-file-placement.bind");
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
  var p=project("GUARDED-STORAGE",7L);var f=folder(p.getId(),"protected");policy("dcc.project-file-placement.bind");
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
}
