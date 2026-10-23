package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyDecision;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pure service decisions with policy ports; no SQL, Redis, service startup or shared target writes. */
class DccW08VersionBrowserPermissionsTest {
    private DccControlledFileQueryServiceImpl query;
    private DccControlledFileAssignmentScopeService scope;
    private DccDirectoryAccessPermissionService directories;
    private DccControlledFileCategoryPermissionSupport categories;
    private DccControlledFileMasterMapper masters;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        query = new DccControlledFileQueryServiceImpl();
        scope=mock(DccControlledFileAssignmentScopeService.class);
        directories=mock(DccDirectoryAccessPermissionService.class);
        categories=mock(DccControlledFileCategoryPermissionSupport.class);
        masters=mock(DccControlledFileMasterMapper.class);
        ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directories);
        ReflectionTestUtils.setField(query,"permissionSupport",categories);
        ReflectionTestUtils.setField(query,"controlledFileMasterMapper",masters);
        ReflectionTestUtils.setField(query,"downloadPolicyService",new DccDownloadPolicyService());
        var projects=mock(cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService.class);
        when(projects.listReadableProjectIds(99L)).thenReturn(List.of(5L));
        ReflectionTestUtils.setField(query,"projectAccessService",projects);
        when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenReturn(true);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void pendingEffectCannotUseAnyOrdinaryPreviewIdentity() {
        var file=file(2L,"CONTROLLED_PENDING_EFFECTIVE","B/1");
        for(Long actor:List.of(99L,88L)) for(boolean manager:List.of(false,true)) {
            assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query,"canReadBinary",
                    actor,file,DccAccessTypeEnum.PREVIEW,manager,null)));
        }
        verifyNoInteractions(directories,categories,masters);
    }
    @Test void downloadRequiresIndependentCategoryAndDirectoryEvenForManager() {
        var file=file(1L,"ACTIVE","A/1");
        assertFalse(download(file,true).allowed());
        when(categories.hasCategoryPermission(10L,99L,DccFileCategoryPermissionActionEnum.DOWNLOAD)).thenReturn(true);
        assertFalse(download(file,true).allowed());
        when(directories.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.DOWNLOAD)).thenReturn(Set.of(20L));
        assertTrue(download(file,false).allowed());
        when(scope.isWithinAssignedFileScope(99L,1L)).thenReturn(false);
        assertFalse(download(file,true).allowed());
    }
    @Test void currentProjectionUsesExactMasterAndNeverScansEarlierActive() {
        var earlier=file(1L,"ACTIVE","A/1");var current=file(2L,"ACTIVE","B/1");
        var pending=file(3L,"CONTROLLED_PENDING_EFFECTIVE","C/1");
        var master=new DccControlledFileMasterDO();master.setId(100L);master.setTenantId(1L);master.setCurrentActiveControlledFileId(2L);
        when(masters.selectById(100L)).thenReturn(master);
        assertEquals("B/1",current(pending,List.of(earlier,current,pending)));
        assertEquals("B/1",current(earlier,List.of(earlier,current,pending)));
        master.setCurrentActiveControlledFileId(null);
        assertNull(current(pending,List.of(earlier,current,pending)));
        master.setCurrentActiveControlledFileId(3L);
        assertNull(current(pending,List.of(earlier,current,pending)));
    }
    @Test void currentProjectionRequiresControlledAndActivatedFacts() {
        var selected=file(2L,"ACTIVE","B/1");var master=new DccControlledFileMasterDO();
        master.setId(100L);master.setTenantId(1L);master.setCurrentActiveControlledFileId(2L);
        when(masters.selectById(100L)).thenReturn(master);
        selected.setActivatedTime(null);assertNull(current(selected,List.of(selected)));
        selected.setActivatedTime(LocalDateTime.of(2026,10,11,1,0));selected.setControlledTime(null);
        assertNull(current(selected,List.of(selected)));
    }
    @Test void applicationSelectionProjectsPersistedFactsWithoutHistoricalDefaults() {
        var file=file(2L,"ACTIVE","B/1");
        file.setEffectiveMode("AFTER_STANDARDIZATION_SIGNATURE");
        file.setEffectiveTime(LocalDateTime.of(2026,10,11,13,20,30));
        file.setEffectiveSignatureId(9007199254740993L);file.setEffectiveProcessInstanceId("actual-round");
        file.setDistributionDepartmentIdsJson("[\"70\",\"9007199254740993\"]");
        var response=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO();
        ReflectionTestUtils.invokeMethod(query,"fillApplicationSelectionProjection",response,file);
        assertEquals(file.getEffectiveMode(),response.getEffectiveMode());assertEquals(file.getEffectiveTime(),response.getEffectiveTime());
        assertEquals(file.getEffectiveSignatureId(),response.getEffectiveSignatureId());
        assertEquals("actual-round",response.getEffectiveProcessInstanceId());
        assertEquals(List.of("70","9007199254740993"),response.getSelectedDistributionDepartmentIds());
        file.setEffectiveMode(null);file.setEffectiveTime(null);file.setEffectiveSignatureId(null);file.setEffectiveProcessInstanceId(null);file.setDistributionDepartmentIdsJson(null);
        ReflectionTestUtils.invokeMethod(query,"fillApplicationSelectionProjection",response,file);
        assertNull(response.getEffectiveMode());assertNull(response.getEffectiveTime());assertNull(response.getSelectedDistributionDepartmentIds());
    }
    @Test void invalidDistributionSnapshotCannotBorrowSignoffOrDropCorruptedIdentity() {
        for(var json:List.of("null","{}","[\"1\",1]","[1.0]","[null]","[9223372036854775808]")) {
            assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(query,"readDistributionDepartmentIds",json));
        }
        assertEquals(List.of(),ReflectionTestUtils.invokeMethod(query,"readDistributionDepartmentIds","[]"));
        assertEquals(List.of("9007199254740993"),ReflectionTestUtils.invokeMethod(query,"readDistributionDepartmentIds","[9007199254740993]"));
    }
    private DccDownloadPolicyDecision download(DccControlledFileDO file,boolean manager) {
        return ReflectionTestUtils.invokeMethod(query,"decideDownloadBinary",99L,file,manager);
    }
    private String current(DccControlledFileDO file,List<DccControlledFileDO> chain) {
        return ReflectionTestUtils.invokeMethod(query,"resolveCurrentActiveVersionNo",file,chain);
    }
    private DccControlledFileDO file(Long id,String status,String version) {
        var file=DccControlledFileDO.builder().id(id).masterId(100L).tenantId(1L).categoryId(10L).directoryId(20L)
                .requesterId(88L).publishedFileId(500L).dccProjectCodeId(5L).versionNo(version).status(status)
                .controlledTime(LocalDateTime.of(2026,10,10,1,0)).build();
        if("ACTIVE".equals(status))file.setActivatedTime(LocalDateTime.of(2026,10,11,1,0));
        return file;
    }
}
