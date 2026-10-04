package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccProjectReferenceController;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
class DccReferenceUsagePageDatabaseTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource source;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccProjectCodeMapper projectRows;
    @Resource DccProjectAccessRuleMapper ruleRows;
    @Resource DccProjectCodeAssignmentMapper assignments;
    @Resource org.springframework.transaction.PlatformTransactionManager transactions;
    JdbcTemplate jdbc;DccProjectReferenceService service;DccRelationAccessPolicy access;DccProjectAccessService projects;
    cn.iocoder.yudao.module.system.api.permission.PermissionApi permissionApi;
    DccControlledFileQueryServiceImpl query;
    static final long SOURCE=9007199254740993L;
    @BeforeEach void fixture() throws Exception {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(source);try(var c=source.getConnection()){assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())jdbc.execute(sql);
        jdbc.update("DELETE FROM dcc_project_file_reference");
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status) VALUES(1,1,'源项目','SOURCE','ENABLE'),(2,1,'可读目标项目','P2','ENABLE'),(3,1,'受限目标项目','P3','ENABLE')");
        jdbc.update("INSERT INTO dcc_project_folder(id,tenant_id,project_code_id,parent_id,name,sort_order,active,source_template_id,source_node_key) VALUES(21,1,2,0,'质量目录',0,1,1,'Q'),(22,1,2,0,'工程目录',1,1,1,'E'),(31,1,3,0,'受限目录',0,1,1,'R')");
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,status,latest_controlled_file_id,current_active_controlled_file_id) VALUES(10,1,2,3,'SOP','N-1',1,'ACTIVE_CHAIN',101,?)",SOURCE);
        file(SOURCE,10,"A/1","OBSOLETE");file(101L,10,"A/2","ACTIVE");
        jdbc.update("INSERT INTO dcc_project_file_reference(id,tenant_id,project_id,folder_id,master_id,selected_controlled_file_id,created_by) VALUES(1,1,2,21,10,?,7),(2,1,2,22,10,101,7),(3,1,3,31,10,?,8)",SOURCE,SOURCE);
        var store=new DccRelationStore(jdbc,mock(GxpAuditService.class));var resolver=new DccLatestControlledFileResolverImpl();ReflectionTestUtils.setField(resolver,"fileMapper",files);ReflectionTestUtils.setField(resolver,"masterMapper",masters);ReflectionTestUtils.setField(resolver,"jdbc",jdbc);
        var projectService=new cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessServiceImpl();
        for(var field:projectService.getClass().getDeclaredFields())if(field.getAnnotation(Resource.class)!=null)ReflectionTestUtils.setField(projectService,field.getName(),mock(field.getType()));
        var users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);when(users.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0).setPostIds(Set.of()));
        permissionApi=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permissionApi.getUserRoleIdListByUserId(99L)).thenReturn(Set.of());
        ReflectionTestUtils.setField(projectService,"projectCodeMapper",projectRows);ReflectionTestUtils.setField(projectService,"accessRuleMapper",ruleRows);ReflectionTestUtils.setField(projectService,"assignmentMapper",assignments);ReflectionTestUtils.setField(projectService,"adminUserApi",users);ReflectionTestUtils.setField(projectService,"permissionApi",permissionApi);projects=projectService;
        jdbc.update("INSERT INTO dcc_project_access_rule(tenant_id,dcc_project_code_id,subject_type,subject_id,access_level,active,change_reason) VALUES(1,2,'USER',99,'VIEW',1,'正式测试目的项目读取规则')");
        query=new DccControlledFileQueryServiceImpl();for(var field:query.getClass().getDeclaredFields())if(field.getAnnotation(Resource.class)!=null)ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
        ReflectionTestUtils.setField(query,"controlledFileMapper",files);ReflectionTestUtils.setField(query,"controlledFileMasterMapper",masters);ReflectionTestUtils.setField(query,"projectCodeMapper",projectRows);ReflectionTestUtils.setField(query,"projectAccessService",projects);
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(any(),any())).thenReturn(true);ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        var directory=mock(cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService.class);when(directory.getAuthorizedDirectoryIds(99L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.QUERY)).thenReturn(Set.of(3L));when(directory.getAuthorizedDirectoryIds(99L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of());ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directory);
        var categories=mock(DccControlledFileCategoryPermissionSupport.class);when(categories.hasCategoryPermission(any(),eq(99L),eq(cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.OBSOLETE))).thenReturn(true);ReflectionTestUtils.setField(query,"permissionSupport",categories);
        access=spy(new DccRelationAccessPolicyImpl());ReflectionTestUtils.setField(access,"query",query);
        service=new DccProjectReferenceService(store,mock(DccProjectReferenceAuthority.class),access,resolver);
        if(Arrays.stream(DccProjectReferenceService.class.getDeclaredFields()).anyMatch(f->f.getName().equals("projectAccessService")))ReflectionTestUtils.setField(service,"projectAccessService",projects);
        ReflectionTestUtils.setField(service,"queryService",query);
        var factory=new org.springframework.aop.framework.ProxyFactory(service);factory.setProxyTargetClass(true);factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));service=(DccProjectReferenceService)factory.getProxy();
    }
    @AfterEach void clear(){jdbc.update("DELETE FROM dcc_project_file_reference");TenantContextHolder.clear();}
    void file(long id,long master,String version,String status){files.insert(DccControlledFileDO.builder().id(id).tenantId(1L).masterId(master).categoryId(2L).directoryId(3L).sourceFileId(100L).originalFileId(100L).publishedFileId(200L).fileName("SOP.pdf").title("SOP").fileNumber("N-1").versionNo(version).status(status).dccProjectCodeId(1L).requesterId(88L).submitterId(88L).controlledTime(java.time.LocalDateTime.of(2026,10,1,0,0)).activatedTime("ACTIVE".equals(status)?java.time.LocalDateTime.of(2026,10,2,0,0):null).build());}
    Object page(int page,int size){return ReflectionTestUtils.invokeMethod(service,"getUsagePage",99L,SOURCE,page,size);}
    @Test void sourceNameReadableReverseUsageCountsAuthorizedFoldersAndKeepsStoredVersions() {
        Object first=page(1,1),second=page(2,1);
        assertEquals(2L,(Object)ReflectionTestUtils.invokeMethod(first,"referenceProjectCount"));assertEquals(1L,(Object)ReflectionTestUtils.invokeMethod(first,"visibleReferenceProjectCount"));
        assertEquals(2L,(Object)ReflectionTestUtils.invokeMethod(first,"total"));assertEquals(true,ReflectionTestUtils.invokeMethod(first,"detailsRestricted"));
        List<?> one=ReflectionTestUtils.invokeMethod(first,"list"),two=ReflectionTestUtils.invokeMethod(second,"list");assertEquals(1,one.size());assertEquals(1,two.size());
        assertEquals(SOURCE,(Object)ReflectionTestUtils.invokeMethod(one.get(0),"selectedControlledFileId"));assertEquals("A/1",ReflectionTestUtils.invokeMethod(one.get(0),"versionNo"));assertEquals("OBSOLETE",ReflectionTestUtils.invokeMethod(one.get(0),"status"));
        assertEquals("A/2",ReflectionTestUtils.invokeMethod(two.get(0),"versionNo"));assertEquals("可读目标项目",ReflectionTestUtils.invokeMethod(one.get(0),"projectName"));assertEquals("质量目录",ReflectionTestUtils.invokeMethod(one.get(0),"folderName"));
        assertEquals(0,((List<?>)ReflectionTestUtils.invokeMethod(page(3,1),"list")).size());
        verify(access,atLeastOnce()).assertNameVisible(99L,SOURCE);verify(access,never()).assertContentReadable(any(),any());
    }
    @Test void httpRequiresExactSelectedFileAndReturnsLongIdentityStrings() throws Exception {
        var controller=new DccProjectReferenceController(service);try(var login=mockStatic(SecurityFrameworkUtils.class)){login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var response=MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/project-file-references/usage-page").param("selectedFileId",SOURCE+"").param("pageNo","1").param("pageSize","1")).andReturn();assertEquals(200,response.getResponse().getStatus());
            var body=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),com.fasterxml.jackson.databind.JsonNode.class);assertEquals(0,body.get("code").intValue());
            assertEquals(SOURCE+"",body.get("data").get("sourceControlledFileId").textValue());assertEquals("10",body.get("data").get("masterId").textValue());assertEquals("21",body.get("data").get("list").get(0).get("folderId").textValue());
        }
    }
    @Test void noDestinationScopeIsHonestRestrictedCountAndNeverChangesTheFixedSelection() {
        jdbc.update("DELETE FROM dcc_project_access_rule");Object restricted=page(1,20);
        assertEquals(2L,(Object)ReflectionTestUtils.invokeMethod(restricted,"referenceProjectCount"));assertEquals(0L,(Object)ReflectionTestUtils.invokeMethod(restricted,"visibleReferenceProjectCount"));assertEquals(0L,(Object)ReflectionTestUtils.invokeMethod(restricted,"total"));assertEquals(true,ReflectionTestUtils.invokeMethod(restricted,"detailsRestricted"));
        assertTrue(((List<?>)ReflectionTestUtils.invokeMethod(restricted,"list")).isEmpty());
        jdbc.update("INSERT INTO dcc_project_access_rule(tenant_id,dcc_project_code_id,subject_type,subject_id,access_level,active,change_reason) VALUES(1,2,'USER',99,'VIEW',1,'正式读取恢复'),(1,3,'USER',99,'VIEW',1,'正式第二目的项目读取')");
        Object all=page(1,20);assertEquals(2L,(Object)ReflectionTestUtils.invokeMethod(all,"visibleReferenceProjectCount"));assertEquals(3L,(Object)ReflectionTestUtils.invokeMethod(all,"total"));assertEquals(false,ReflectionTestUtils.invokeMethod(all,"detailsRestricted"));
        List<?> rows=ReflectionTestUtils.invokeMethod(all,"list");assertEquals("A/1",ReflectionTestUtils.invokeMethod(rows.get(0),"versionNo"));
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=NULL WHERE id=10");all=page(1,20);rows=ReflectionTestUtils.invokeMethod(all,"list");assertEquals("A/1",ReflectionTestUtils.invokeMethod(rows.get(0),"versionNo"));
        assertEquals("A/2",ReflectionTestUtils.invokeMethod(rows.get(1),"versionNo"));
    }
    @Test void formalProjectAssignmentHardScopeRestrictsOnlyDestinationDetails() {
        when(permissionApi.hasAnyPermissions(99L,"dcc:project-code-assignment:execute")).thenReturn(true);
        var assignment=mock(DccProjectCodeAssignmentMapper.class);when(assignment.selectActiveProjectCodeIdsByAssigneeUserId(eq(1L),eq(99L),any())).thenReturn(List.of(3L));ReflectionTestUtils.setField(projects,"assignmentMapper",assignment);
        Object none=page(1,20);assertEquals(0L,(Object)ReflectionTestUtils.invokeMethod(none,"total"));assertEquals(2L,(Object)ReflectionTestUtils.invokeMethod(none,"referenceProjectCount"));
        jdbc.update("INSERT INTO dcc_project_access_rule(tenant_id,dcc_project_code_id,subject_type,subject_id,access_level,active,change_reason) VALUES(1,3,'USER',99,'VIEW',1,'正式目的项目授权与分配交集')");
        Object one=page(1,20);assertEquals(1L,(Object)ReflectionTestUtils.invokeMethod(one,"visibleReferenceProjectCount"));assertEquals(1L,(Object)ReflectionTestUtils.invokeMethod(one,"total"));List<?> rows=ReflectionTestUtils.invokeMethod(one,"list");assertEquals(3L,(Object)ReflectionTestUtils.invokeMethod(rows.get(0),"projectId"));
    }
    @Test void bodyPermissionIsIndependentAndNeverRequiresManagementDetail() {
        Object page=page(1,20);List<?> rows=ReflectionTestUtils.invokeMethod(page,"list");assertFalse((Boolean)ReflectionTestUtils.invokeMethod(rows.get(0),"canPreview"));
        var storage=mock(cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper.class);when(storage.selectById(200L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder().id(200L).name("SOP.pdf").type("application/pdf").build());ReflectionTestUtils.setField(query,"fileMapper",storage);
        var directories=(cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService)ReflectionTestUtils.getField(query,"directoryAccessPermissionService");when(directories.getAuthorizedDirectoryIds(99L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(3L));
        page=page(1,20);rows=ReflectionTestUtils.invokeMethod(page,"list");assertTrue((Boolean)ReflectionTestUtils.invokeMethod(rows.get(0),"canPreview"));
        verify(access,never()).assertContentReadable(any(),any());
    }
    @ParameterizedTest @ValueSource(strings={"WRONG_MASTER","FOREIGN_FILE_TENANT","WRONG_FOLDER_PROJECT","MISSING_FOLDER","MISSING_PROJECT","MISSING_FILE"})
    void malformedReferenceIdentityFailsRatherThanDroppingRowsOrReturningEmptySuccess(String invalid) {
        switch(invalid){case "WRONG_MASTER"->jdbc.update("UPDATE dcc_controlled_file SET master_id=999 WHERE id=?",SOURCE);case "FOREIGN_FILE_TENANT"->jdbc.update("UPDATE dcc_controlled_file SET tenant_id=2 WHERE id=101");case "WRONG_FOLDER_PROJECT"->jdbc.update("UPDATE dcc_project_folder SET project_code_id=3 WHERE id=21");case "MISSING_FOLDER"->jdbc.update("DELETE FROM dcc_project_folder WHERE id=21");case "MISSING_PROJECT"->jdbc.update("DELETE FROM dcc_project_code WHERE id=3");case "MISSING_FILE"->jdbc.update("DELETE FROM dcc_controlled_file WHERE id=101");}
        assertThrows(DccRelationFailure.class,()->page(1,20));
    }
    @Test void wrongTenantSourceAndInvalidPageCannotEnterUsageQueries() {
        TenantContextHolder.setTenantId(2L);assertThrows(DccRelationFailure.class,()->page(1,20));TenantContextHolder.setTenantId(1L);
        assertThrows(DccRelationInputFailure.class,()->page(0,20));assertThrows(DccRelationInputFailure.class,()->page(1,201));
        var directory=(cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService)ReflectionTestUtils.getField(query,"directoryAccessPermissionService");when(directory.getAuthorizedDirectoryIds(99L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.QUERY)).thenReturn(Set.of());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->page(1,20));
    }
    @Test void concurrentCancellationCannotMixAuthorizedCountAndPageSnapshots() throws Exception {
        var counted=new CountDownLatch(1);var cancelled=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        var store=new DccRelationStore(jdbc,mock(GxpAuditService.class)){
            @Override public UsageCounts visibleUsageCounts(Long tenant,Long master,List<Long> ids){var result=super.visibleUsageCounts(tenant,master,ids);counted.countDown();try{assertTrue(cancelled.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}return result;}
        };
        var reader=new DccProjectReferenceService(store,mock(DccProjectReferenceAuthority.class),access,(DccLatestControlledFileResolver)ReflectionTestUtils.getField(service,"files"));ReflectionTestUtils.setField(reader,"projectAccessService",projects);ReflectionTestUtils.setField(reader,"queryService",query);
        var factory=new org.springframework.aop.framework.ProxyFactory(reader);factory.setProxyTargetClass(true);factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));var proxy=(DccProjectReferenceService)factory.getProxy();
        try {
            var reading=pool.submit(()->{TenantContextHolder.setTenantId(1L);try{return proxy.getUsagePage(99L,SOURCE,1,20);}finally{TenantContextHolder.clear();}});
            assertTrue(counted.await(10,TimeUnit.SECONDS));service.cancel(99L,2L,21L,10L,1L,true,"并发取消任务自有引用");cancelled.countDown();
            var snapshot=reading.get(15,TimeUnit.SECONDS);assertEquals(2,snapshot.total());assertEquals(2,snapshot.list().size());
            Object next=page(1,20);assertEquals(1L,(Object)ReflectionTestUtils.invokeMethod(next,"total"));assertEquals(1,((List<?>)ReflectionTestUtils.invokeMethod(next,"list")).size());
        } finally {cancelled.countDown();pool.shutdownNow();}
    }
    @Test void permissionInfrastructureFailureIsNotConvertedToEmptyUsage() {
        var directory=(cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService)ReflectionTestUtils.getField(query,"directoryAccessPermissionService");
        var error=new IllegalStateException("name permission source unavailable");when(directory.getAuthorizedDirectoryIds(99L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.QUERY)).thenThrow(error);
        assertSame(error,assertThrows(IllegalStateException.class,()->page(1,20)));assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
    }
    @Test void disabledDestinationProjectCannotKeepVisibleLabelsFromAnOldReaderRule() {
        jdbc.update("UPDATE dcc_project_code SET status='DISABLE' WHERE id=2");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->projects.assertProjectViewerOrAbove(99L,2L));
        Object restricted=page(1,20);assertEquals(0L,(Object)ReflectionTestUtils.invokeMethod(restricted,"total"));assertEquals(0L,(Object)ReflectionTestUtils.invokeMethod(restricted,"visibleReferenceProjectCount"));assertEquals(2L,(Object)ReflectionTestUtils.invokeMethod(restricted,"referenceProjectCount"));assertEquals(true,ReflectionTestUtils.invokeMethod(restricted,"detailsRestricted"));
    }
}
