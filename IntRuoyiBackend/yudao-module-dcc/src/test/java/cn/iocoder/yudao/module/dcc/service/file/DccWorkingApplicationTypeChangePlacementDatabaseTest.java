package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real folder/category mapping, placement, GXP ledger and action transaction; no placement-success substitute. */
@Import({DccPublicUploadPlacementService.class,DccProjectFolderStorageService.class,DccProjectFilePlacementService.class,
        DccFolderTemplateService.class,DccProjectConfigurationAuditService.class,GxpAuditServiceImpl.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccWorkingApplicationTypeChangePlacementDatabaseTest extends DccWorkingApplicationTypeChangeActionDatabaseTest {
    @Resource DccPublicUploadPlacementService locations;
    @Resource DccProjectFolderMapper logicalFolders;
    @Resource DccProjectFolderStorageMappingMapper storageMappings;
    @Resource GxpAuditEventMapper auditEvents;
    @MockitoBean AdminUserApi auditAccounts;
    @MockitoBean PermissionApi folderPermissions;
    @MockitoBean DccFileTypeTaxonomyAdminService storageTaxonomy;

    void mappedActionFixture() throws Exception {
        actionFixture();
        when(storageTaxonomy.resolveActiveCategoryId(6L)).thenReturn(2L);when(storageTaxonomy.resolveActiveCategoryId(8L)).thenReturn(3L);
        when(auditAccounts.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                .setId(99L).setTenantId(1L).setStatus(0).setUsername("type-change-placement").setNickname("Task applicant"));
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(99L).setTenantId(1L).setUserType(2)
                .setInfo(Map.of("username","type-change-placement",LoginUser.INFO_KEY_NICKNAME,"Task applicant")),new org.springframework.mock.web.MockHttpServletRequest());
        var folder=new DccProjectFolderDO();folder.setId(36L);folder.setTenantId(1L);folder.setProjectCodeId(5L);folder.setParentId(0L);
        folder.setName("Exact original logical folder");folder.setActive(true);folder.setSortOrder(0);folder.setSourceTemplateId(1L);folder.setSourceNodeKey("C10");logicalFolders.insert(folder);
        directoryRows.insert(DccFileDirectoryDO.builder().id(31L).tenantId(1L).parentId(3L).code("PFS-SOURCE-C10").name("Original derived storage")
                .active(true).sort(0).accessRuleManuallyBound(false).build());
        var mapping=new DccProjectFolderStorageMappingDO();mapping.setId(18L);mapping.setTenantId(1L);mapping.setProjectCodeId(5L);
        mapping.setProjectFolderId(36L);mapping.setCategoryId(2L);mapping.setBaseDirectoryId(3L);mapping.setStorageDirectoryId(31L);storageMappings.insert(mapping);
        jdbc.update("UPDATE dcc_controlled_file SET directory_id=31 WHERE id IN (20,21,22)");
        jdbc.update("UPDATE dcc_controlled_file_master SET directory_id=31 WHERE id=10");
        for(long file:List.of(20L,21L,22L)){
            var placement=new DccProjectFilePlacementDO();placement.setTenantId(1L);placement.setProjectCodeId(5L);placement.setProjectFolderId(36L);
            placement.setControlledFileId(file);placement.setStorageDirectoryId(31L);placementRows.insert(placement);
        }
        wire(action,"placements",locations);
    }

    DccProjectFolderStorageMappingDO requireTargetLocation(){
        var target=storageMappings.lockMapping(1L,36L,3L);
        assertNotNull(target,"Confirm must resolve a target-category derived mapping instead of keeping the source category storage");
        assertEquals(5L,target.getProjectCodeId());assertEquals(36L,target.getProjectFolderId());assertEquals(3L,target.getCategoryId());
        assertNotEquals(31L,target.getStorageDirectoryId());
        assertEquals(target.getStorageDirectoryId(),files.selectById(22L).getDirectoryId());
        assertEquals(target.getStorageDirectoryId(),placementRows.findFile(22L).getStorageDirectoryId());
        assertEquals(36L,placementRows.findFile(22L).getProjectFolderId());
        return target;
    }

    @ParameterizedTest @ValueSource(longs={3L,4L})
    void confirmRebindsCurrentWorkingToTargetDerivedStorageOnly(long targetBase) throws Exception {
        mappedActionFixture();
        if(targetBase==4L){
            directoryRows.insert(DccFileDirectoryDO.builder().id(4L).tenantId(1L).parentId(0L).code("C10-TARGET-BASE").name("Target type base").active(true).build());
            jdbc.update("UPDATE dcc_category_directory_binding SET directory_id=4 WHERE tenant_id=1 AND category_id=3");
        }
        var request=command();var old=original(20L);var intermediate=original(21L);
        var oldMapping=storageMappings.selectById(18L);var oldPlacement=placementRows.findFile(20L);var intermediatePlacement=placementRows.findFile(21L);
        tx().executeWithoutResult(s->{action.confirm(99L,22L,request);var target=requireTargetLocation();
            assertEquals(targetBase,target.getBaseDirectoryId());assertEquals(targetBase,directoryRows.selectById(target.getStorageDirectoryId()).getParentId());
            assertEquals(target.getStorageDirectoryId(),masters.selectById(10L).getDirectoryId());
            var proof=typeChanges.selectListBySource(SOURCE).get(0);
            var before=JsonUtils.parseObject(proof.getBeforeSnapshotJson(),cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.TypeChangeState.class);
            var after=JsonUtils.parseObject(proof.getAfterSnapshotJson(),cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.TypeChangeState.class);
            assertEquals(31L,before.directoryId());assertEquals(target.getStorageDirectoryId(),after.directoryId());
            assertEquals(36L,before.projectFolderId());assertEquals(36L,after.projectFolderId());
            assertEquals(targetMatrix.canonical(),contracts.readConfirmedTypeChangeBaseline(99L,files.selectById(22L)));
        });
        assertEquals(old,original(20L));assertEquals(intermediate,original(21L));assertEquals(oldMapping,storageMappings.selectById(18L));
        assertEquals(oldPlacement,placementRows.findFile(20L));assertEquals(intermediatePlacement,placementRows.findFile(21L));
        assertEquals(10L,files.selectById(22L).getMasterId());assertEquals(100L,files.selectById(22L).getSourceFileId());
        assertEquals(3,files.selectListByMasterId(10L).size());assertEquals(1,typeChanges.selectListBySource(SOURCE).size());
        assertEquals(0,auditEvents.selectList().size(),"Existing placement update is audited by exact TYPE_CHANGE, not a fabricated CREATE event");
    }

    @Test void lateFailureRollsBackNewDerivedMappingPlacementAndProofTogether() throws Exception {
        mappedActionFixture();var request=command();var current=original(22L);var old=original(20L);var mapping=storageMappings.selectById(18L);
        var placement=placementRows.findFile(22L);int directoryCount=directoryRows.selectList().size();
        var failure=assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{
            action.confirm(99L,22L,request);requireTargetLocation();throw new IllegalStateException("TEST_LATE_DERIVED_FAILURE");
        }));
        assertEquals("TEST_LATE_DERIVED_FAILURE",failure.getMessage());assertEquals(current,original(22L));assertEquals(old,original(20L));
        assertEquals(mapping,storageMappings.selectById(18L));assertEquals(placement,placementRows.findFile(22L));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder_storage_mapping WHERE tenant_id=1 AND project_folder_id=36 AND category_id=3",Integer.class));
        assertEquals(directoryCount,directoryRows.selectList().size());assertEquals(6L,masters.selectById(10L).getFileTypeTaxonomyLeafId());
        assertEquals(6L,claims.selectById(600L).getFileTypeTaxonomyLeafId());assertEquals(0,typeChanges.selectListBySource(SOURCE).size());assertEquals(0,auditEvents.selectList().size());
    }
}
