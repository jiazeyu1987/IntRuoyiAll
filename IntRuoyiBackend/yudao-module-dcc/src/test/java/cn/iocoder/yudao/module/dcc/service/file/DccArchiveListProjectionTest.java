package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccArchiveListProjectionTest extends BaseMockitoUnitTest {
    @Mock DccArchiveAccessService archiveAccess;
    @Mock DccControlledFileMapper files;
    @Mock DccFileDirectoryMapper directories;
    @Mock DccProjectCodeMapper projects;
    @Mock DccProjectFilePlacementMapper placements;
    @Mock DccProjectFolderMapper folders;
    @Mock DccProjectFolderStorageMappingMapper mappings;
    @Mock DccControlledFileCategoryPermissionSupport permissions;
    @Mock DccControlledFileRouteSnapshotMapper routes;
    @Mock DccApplicationExecutionContractService executionContracts;
    @Mock DccOfflineTrainingRecordService offlineTraining;
    DccControlledFileQueryServiceImpl query;
    @BeforeEach void setup(){
        TenantContextHolder.setTenantId(1L);query=new DccControlledFileQueryServiceImpl();
        wire("archiveAccess",archiveAccess);wire("controlledFileMapper",files);wire("directoryMapper",directories);
        wire("projectCodeMapper",projects);wire("projectFilePlacementMapper",placements);wire("projectFolderMapper",folders);
        wire("projectStorageMappingMapper",mappings);wire("permissionSupport",permissions);wire("routeSnapshotMapper",routes);
        wire("executionContracts",executionContracts);wire("offlineTraining",offlineTraining);
    }
    void wire(String field,Object value){ReflectionTestUtils.setField(query,field,value);}
    @AfterEach void clear(){TenantContextHolder.clear();}
    DccControlledFileDO archived(){return DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(10L)
            .dccProjectCodeId(5L).directoryId(3L).categoryId(2L).fileName("SOP.pdf").fileNumber("ARC-1")
            .versionNo("A/1").status("OBSOLETE").obsoleteReason("新版生效自动作废")
            .obsoletedTime(LocalDateTime.of(2026,11,5,12,0)).supersededByFileId(21L).build();}
    void page(DccControlledFilePageReqVO request,DccControlledFileDO file){
        when(archiveAccess.canQuery(99L)).thenReturn(true);
        when(files.selectPage(eq(request),any())).thenReturn(new PageResult<>(List.of(file),1L));
        when(directories.selectEnabledList()).thenReturn(List.of());
    }
    @Test void archiveListKeepsSavedIdentityLocationAndReasonWithoutWorkflowProjection(){
        var request=new DccControlledFilePageReqVO();page(request,archived());
        var directory=new DccFileDirectoryDO();directory.setId(3L);directory.setName("原目录");directory.setTenantId(1L);
        when(directories.selectEnabledList()).thenReturn(List.of(directory));
        var project=new DccProjectCodeDO();project.setId(5L);project.setTenantId(1L);project.setProjectName("原项目");project.setProjectCode("P-1");
        when(projects.selectById(5L)).thenReturn(project);
        var placement=new DccProjectFilePlacementDO();placement.setId(8L);placement.setTenantId(1L);placement.setProjectCodeId(5L);
        placement.setControlledFileId(20L);placement.setStorageDirectoryId(3L);placement.setProjectFolderId(7L);
        when(placements.findFile(20L)).thenReturn(placement);
        var folder=new DccProjectFolderDO();folder.setId(7L);folder.setTenantId(1L);folder.setProjectCodeId(5L);folder.setActive(true);folder.setName("原文件夹");
        when(folders.selectById(7L)).thenReturn(folder);
        var result=query.getArchivePage(99L,request);var row=result.getList().get(0);
        assertEquals(1L,result.getTotal());assertEquals(20L,row.getId());assertEquals("SOP.pdf",row.getFileName());
        assertEquals("ARC-1",row.getFileNumber());assertEquals("A/1",row.getVersionNo());assertEquals("OBSOLETE",row.getStatus());
        assertEquals("原目录",row.getDirectoryPath());assertEquals("原项目",row.getProjectName());assertEquals("P-1",row.getProjectCode());
        assertEquals("原文件夹",row.getProjectFolderName());assertEquals(7L,row.getProjectFolderId());
        assertEquals("新版生效自动作废",row.getObsoleteReason());assertEquals(21L,row.getSupersededByFileId());assertEquals(archived().getObsoletedTime(),row.getObsoletedTime());
        assertFalse(row.getCanPreview());assertFalse(row.getCanDownload());assertFalse(row.getCanDownloadReadOnly());assertFalse(row.getCanDownloadEditable());
        assertFalse(row.getCanEditFile());assertFalse(row.getCanObsolete());assertFalse(row.getCanPublish());assertFalse(row.getCanManualRelease());assertFalse(row.getCanPrint());
        assertFalse(row.getCanExportApprovalHistory());assertFalse(row.getCanPrintApprovalHistory());assertNull(row.getActionProjection());assertNull(row.getRouteSnapshots());
        verify(archiveAccess,times(1)).canQuery(99L);verifyNoMoreInteractions(archiveAccess);
        verifyNoInteractions(permissions,routes,executionContracts,offlineTraining);
    }
    @Test void deniedArchiveActorCannotQuerySavedFiles(){
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getArchivePage(99L,new DccControlledFilePageReqVO()));
        verifyNoInteractions(files,directories,projects,placements,folders,mappings);
    }
    @Test void foreignTenantOrNonArchivedRowIsRejectedBeforeLocationProjection(){
        var request=new DccControlledFilePageReqVO();var file=archived();page(request,file);
        file.setTenantId(2L);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getArchivePage(99L,request));
        file.setTenantId(1L);file.setStatus("ACTIVE");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getArchivePage(99L,request));
        verifyNoInteractions(projects,placements,folders,mappings,permissions,routes,executionContracts,offlineTraining);
    }
}
