package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileVersionHistoryRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccStorageEffectiveVisibilityDatabaseTest extends cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest {
    private static final long ACTIVE = 9007199254740993L;
    @jakarta.annotation.Resource javax.sql.DataSource dataSource;
    @jakarta.annotation.Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper files;
    @jakarta.annotation.Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper masters;
    org.springframework.jdbc.core.JdbcTemplate jdbc;
    DccControlledFileQueryServiceImpl query;
    DccStorageBrowserProjectionFixture projection;

    @BeforeEach void storageFixture() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        jdbc=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,file_name,file_number,dcc_project_code_id,status) VALUES(10,1,2,'SOP','N-1',5,'ACTIVE_CHAIN')");
        files.insert(DccControlledFileDO.builder().id(ACTIVE).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L).fileName("SOP").title("SOP").fileNumber("N-1").versionNo("A/1").status("ACTIVE").sourceFileId(100L).originalFileId(100L).dccProjectCodeId(5L).requesterId(88L).submitterId(88L).controlledTime(java.time.LocalDateTime.now()).build());
        query=new DccControlledFileQueryServiceImpl();
        for(var field:query.getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&field.getAnnotation(jakarta.annotation.Resource.class)!=null)
            ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
        ReflectionTestUtils.setField(query,"controlledFileMapper",files);ReflectionTestUtils.setField(query,"controlledFileMasterMapper",masters);
        ReflectionTestUtils.setField(query,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        var directories=(cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService)ReflectionTestUtils.getField(query,"directoryAccessPermissionService");
        when(directories.hasDirectoryManagementPermission(99L)).thenReturn(true);
        var scope=(DccControlledFileAssignmentScopeService)ReflectionTestUtils.getField(query,"assignmentScopeService");
        when(scope.resolveActiveAssignedControlledFileIds(99L)).thenReturn(null);
        jdbc.update("UPDATE dcc_controlled_file SET activated_time=CURRENT_TIMESTAMP WHERE id=?", ACTIVE);
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=?,current_active_controlled_file_id=? WHERE id=10", ACTIVE, ACTIVE);
        var access=(DccProjectAccessService)ReflectionTestUtils.getField(query,"projectAccessService");
        when(access.listReadableProjectIds(99L)).thenReturn(List.of(5L));
        projection = new DccStorageBrowserProjectionFixture(query, files);
        projection.rows(ACTIVE);
    }
    @org.junit.jupiter.api.AfterEach void clearTenant(){cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}
    private DccControlledFilePageReqVO request(String status, boolean latest) {
        var request=new DccControlledFilePageReqVO();request.setPageNo(1);request.setPageSize(10);
        request.setStatus(status);request.setLatestVersionOnly(latest);return request;
    }
    private void add(long id, String status, boolean controlled, boolean activated) {
        var row=new DccControlledFileDO();BeanUtils.copyProperties(files.selectById(ACTIVE),row);
        row.setId(id);row.setStatus(status);row.setVersionNo("A/"+id);
        if(!controlled)row.setControlledTime(null);if(!activated)row.setActivatedTime(null);files.insert(row);
    }
    @ParameterizedTest
    @ValueSource(strings={"WORKING","PENDING_SUPERVISOR_REVIEW","PENDING_MATRIX_REVIEW","CONTROLLED_PENDING_EFFECTIVE","REJECTED","WITHDRAWN"})
    void pendingVersionsCannotEnterStorageThroughStatusFilterOrAllVersions(String status) {
        add(300,status,false,false);
        for(boolean latest:List.of(true,false)) {
            projection.rows();
            assertTrue(query.getControlledFileBrowserPage(99L,request(status,latest)).getList().isEmpty());
            projection.rows(ACTIVE);
            var page=query.getControlledFileBrowserPage(99L,request(null,latest));
            assertEquals(1,page.getTotal());assertEquals(ACTIVE,page.getList().get(0).getId());
            assertTrue(page.getList().get(0).getVersionHistory().stream().noneMatch(v->v.getId()==300L));
        }
    }
    @Test void storageRequiresBothControlledAndActivationFacts() {
        for(boolean controlled:List.of(true,false))for(boolean activated:List.of(true,false)) {
            jdbc.update("UPDATE dcc_controlled_file SET controlled_time="+(controlled?"CURRENT_TIMESTAMP":"NULL")+",activated_time="+(activated?"CURRENT_TIMESTAMP":"NULL")+" WHERE id=?",ACTIVE);
            if (controlled && activated) projection.rows(ACTIVE); else projection.rows();
            assertEquals(controlled&&activated?1:0,query.getControlledFileBrowserPage(99L,request("ACTIVE",true)).getTotal());
        }
    }
    @Test void staleWorkingNavigationCannotPutPendingOrDraftVersionsInStorage() {
        add(300,"PENDING_SUPERVISOR_REVIEW",false,false);
        add(301,"WORKING",false,false);
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=300 WHERE id=10");
        var request=request(null,true);
        request.setWorkingFileId(300L);request.setWorkingMasterId(10L);
        var page=query.getControlledFileBrowserPage(99L,request);
        assertEquals(1,page.getTotal());assertEquals(ACTIVE,page.getList().get(0).getId());
        assertTrue(page.getList().get(0).getVersionHistory().stream()
                .noneMatch(v->v.getId()==300L || v.getId()==301L));
    }
    @Test void anUncontrolledFirstVersionHasNoStorageRowButCanBeProcessedInItsWorkView() {
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_REWORK',controlled_time=NULL,activated_time=NULL WHERE id=?",ACTIVE);
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL WHERE id=10");
        var request=request(null,true);request.setWorkingFileId(ACTIVE);request.setWorkingMasterId(10L);
        projection.rows();
        assertEquals(0,query.getControlledFileBrowserPage(99L,request).getTotal());
        projection.rows(ACTIVE);
        assertEquals(1,query.getControlledFileWorkingBrowserPage(99L,request).getTotal());
        assertThrows(IllegalArgumentException.class,()->query.getControlledFileWorkingBrowserPage(99L,request(null,true)));
    }
    @Test void storageVersionOptionsKeepOnlyPreviouslyEffectiveVersions() {
        add(300,"PENDING_MATRIX_REVIEW",false,false);add(301,"CONTROLLED_PENDING_EFFECTIVE",true,false);
        add(302,"OBSOLETE",true,true);add(303,"OBSOLETE",true,false);
        var response=new DccControlledFileRespVO();response.setVersionHistory(List.of(ACTIVE,300L,301L,302L,303L).stream().map(id->{
            var option=new DccControlledFileVersionHistoryRespVO();option.setId(id);return option;
        }).toList());
        ReflectionTestUtils.invokeMethod(query,"restrictDailyBodyProjection",response,files.selectById(ACTIVE),true);
        assertEquals(List.of(ACTIVE,302L),response.getVersionHistory().stream().map(DccControlledFileVersionHistoryRespVO::getId).toList());
    }
    @Test void storageKeepsCheckinSmallDraftsAlongsideEffectiveVersionButHidesUnapprovedFormalVersion() {
        add(300,"PENDING_SUPERVISOR_REVIEW",false,false);
        add(301,"WORKING",false,false);add(302,"WORKING",false,false);
        jdbc.update("UPDATE dcc_controlled_file SET version_no='B/7' WHERE id=?",ACTIVE);
        jdbc.update("UPDATE dcc_controlled_file SET version_no='B/8' WHERE id=300");
        jdbc.update("UPDATE dcc_controlled_file SET version_no='B/7-1',predecessor_controlled_file_id=?,revision_base_active_controlled_file_id=? WHERE id=301",ACTIVE,ACTIVE);
        jdbc.update("UPDATE dcc_controlled_file SET version_no='B/7-2',predecessor_controlled_file_id=301,revision_base_active_controlled_file_id=? WHERE id=302",ACTIVE);
        var response=new DccControlledFileRespVO();response.setVersionHistory(List.of(300L,302L,301L,ACTIVE).stream().map(id->{
            var option=new DccControlledFileVersionHistoryRespVO();option.setId(id);return option;
        }).toList());
        ReflectionTestUtils.invokeMethod(query,"restrictDailyBodyProjection",response,files.selectById(ACTIVE),true);
        assertEquals(List.of(302L,301L,ACTIVE),response.getVersionHistory().stream().map(DccControlledFileVersionHistoryRespVO::getId).toList());
        assertEquals(ACTIVE,query.getControlledFileBrowserPage(99L,request(null,true)).getList().get(0).getId());
    }
}
