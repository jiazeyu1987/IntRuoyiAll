package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Runs real Store SQL against task-owned H2; standalone main supports private javac output without Maven target writes. */
class DccExactVersionReferenceUsageTest {
    static final long A1=9007199254740993L,A2=9007199254740995L;
    JdbcTemplate jdbc;DccProjectReferenceService service;DccProjectAccessService projects;
    DccRelationAccessPolicy access;DccControlledFileQueryService query;
    @BeforeEach void fixture() throws Exception {
        TenantContextHolder.setTenantId(1L);
        var source=new DriverManagerDataSource("jdbc:h2:mem:g11_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        try(var c=source.getConnection()){assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        jdbc=new JdbcTemplate(source);
        String migration=Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql"));
        int start=migration.indexOf("CREATE TABLE IF NOT EXISTS dcc_project_file_reference"),end=migration.indexOf(';',start);
        jdbc.execute(migration.substring(start,end));
        jdbc.execute("CREATE TABLE dcc_project_code(id BIGINT,tenant_id BIGINT,project_name VARCHAR(80),status VARCHAR(20),deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_project_folder(id BIGINT,tenant_id BIGINT,project_code_id BIGINT,name VARCHAR(80),deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_controlled_file_master(id BIGINT,tenant_id BIGINT,dcc_project_code_id BIGINT,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_controlled_file(id BIGINT,tenant_id BIGINT,master_id BIGINT,dcc_project_code_id BIGINT,file_name VARCHAR(80),file_number VARCHAR(80),version_no VARCHAR(30),status VARCHAR(50),controlled_time TIMESTAMP,activated_time TIMESTAMP,deleted INT DEFAULT 0)");
        jdbc.update("INSERT INTO dcc_project_code VALUES(1,1,'源项目','ENABLE',0),(2,1,'可读目的','ENABLE',0),(3,1,'受限目的','ENABLE',0),(4,1,'新版目的','ENABLE',0)");
        jdbc.update("INSERT INTO dcc_project_folder VALUES(21,1,2,'旧版目录',0),(22,1,2,'新版目录',0),(23,1,2,'后补目录',0),(31,1,3,'受限目录',0),(41,1,4,'新版专用',0)");
        jdbc.update("INSERT INTO dcc_controlled_file_master VALUES(10,1,1,0)");
        jdbc.update("INSERT INTO dcc_controlled_file VALUES(?,1,10,1,'旧原件.pdf','A','A/1','SUPERSEDED',CURRENT_TIMESTAMP,NULL,0),(?,1,10,1,'新原件.pdf','A','A/2','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",A1,A2);
        jdbc.update("INSERT INTO dcc_project_file_reference(id,tenant_id,project_id,folder_id,master_id,selected_controlled_file_id,created_by) VALUES(1,1,2,21,10,?,7),(2,1,2,22,10,?,7),(3,1,3,31,10,?,8),(4,1,4,41,10,?,8)",A1,A2,A1,A2);
        var store=new DccRelationStore(jdbc,null);
        var resolver=mock(DccLatestControlledFileResolver.class);
        when(resolver.resolveSelected(A1)).thenReturn(new FileVersion(1L,A1,10L,1L,"A","旧原件.pdf","A/1","SUPERSEDED",false,false,false));
        when(resolver.resolveSelected(A2)).thenReturn(new FileVersion(1L,A2,10L,1L,"A","新原件.pdf","A/2","ACTIVE",true,false,true));
        access=mock(DccRelationAccessPolicy.class);projects=mock(DccProjectAccessService.class);query=mock(DccControlledFileQueryService.class);
        when(projects.listReadableProjectIds(99L)).thenReturn(List.of(2L,4L));
        when(query.getRelationPermissions(eq(99L),anyLong())).thenAnswer(invocation->{
            long id=invocation.getArgument(1);var permission=mock(DccFileRelationPermissions.class);
            when(permission.tenantId()).thenReturn(1L);when(permission.controlledFileId()).thenReturn(id);when(permission.masterId()).thenReturn(10L);
            when(permission.versionNo()).thenReturn(id==A1?"A/1":"A/2");when(permission.canPreview()).thenReturn(false);return permission;
        });
        service=new DccProjectReferenceService(store,mock(DccProjectReferenceAuthority.class),access,resolver);
        ReflectionTestUtils.setField(service,"projectAccessService",projects);ReflectionTestUtils.setField(service,"queryService",query);
    }
    @AfterEach void clear(){jdbc.execute("DROP ALL OBJECTS");TenantContextHolder.clear();}
    @Test void selectedVersionHasIndependentGlobalVisibleCountsRowsAndPages(){
        var one=service.getUsagePage(99L,A1,1,1);
        assertEquals(2,one.referenceProjectCount());assertEquals(1,one.visibleReferenceProjectCount());assertEquals(1,one.total());assertTrue(one.detailsRestricted());
        assertEquals(List.of(A1),one.list().stream().map(ReferenceUsageRow::selectedControlledFileId).toList());
        assertTrue(service.getUsagePage(99L,A1,2,1).list().isEmpty());
        var two=service.getUsagePage(99L,A2,1,20);assertEquals(2,two.referenceProjectCount());assertEquals(2,two.visibleReferenceProjectCount());assertEquals(2,two.total());assertFalse(two.detailsRestricted());
        assertTrue(two.list().stream().allMatch(row->row.selectedControlledFileId()==A2));
        assertEquals(2,service.getUsage(99L,A1).referenceProjectCount());assertEquals(2,service.getUsage(99L,A2).referenceProjectCount());
        var summary=service.getUsage(99L,A1);assertEquals(1L,summary.tenantId());assertEquals(A1,summary.sourceControlledFileId());assertEquals(10L,summary.masterId());
        verify(access,never()).assertContentReadable(any(),any());
    }
    @Test void laterSameVersionReferenceAddsFolderWithoutFollowingNewerVersionOrDoubleCountingProject(){
        jdbc.update("INSERT INTO dcc_project_file_reference(id,tenant_id,project_id,folder_id,master_id,selected_controlled_file_id,created_by) VALUES(5,1,2,23,10,?,7)",A1);
        var result=service.getUsagePage(99L,A1,1,20);assertEquals(2,result.referenceProjectCount());assertEquals(1,result.visibleReferenceProjectCount());assertEquals(2,result.total());
        assertEquals(List.of(21L,23L),result.list().stream().map(ReferenceUsageRow::folderId).toList());
        jdbc.update("DELETE FROM dcc_project_file_reference WHERE id=5");assertEquals(1,service.getUsagePage(99L,A1,1,20).total());
    }
    @Test void foreignVersionCorruptionCannotBlockSelectedVersionButSelectedCorruptionFails(){
        jdbc.update("DELETE FROM dcc_controlled_file WHERE id=?",A2);
        assertEquals(1,service.getUsagePage(99L,A1,1,20).total());
        jdbc.update("DELETE FROM dcc_project_folder WHERE id=21");assertThrows(DccRelationFailure.class,()->service.getUsagePage(99L,A1,1,20));
    }
    @Test void zeroVisibleScopeRetainsSelectedVersionGlobalCountAndNeverExposesLabels(){
        when(projects.listReadableProjectIds(99L)).thenReturn(List.of());
        var result=service.getUsagePage(99L,A1,1,20);assertEquals(2,result.referenceProjectCount());assertEquals(0,result.visibleReferenceProjectCount());assertEquals(0,result.total());assertTrue(result.list().isEmpty());assertTrue(result.detailsRestricted());
    }
    @Test void exactSourceNameDeniedPropagatesAndNeverFabricatesZero(){
        var failure=new DccRelationFailure("DCC_NAME_DENIED");doThrow(failure).when(access).assertNameVisible(99L,A1);
        assertSame(failure,assertThrows(DccRelationFailure.class,()->service.getUsage(99L,A1)));
        assertSame(failure,assertThrows(DccRelationFailure.class,()->service.getUsagePage(99L,A1,1,20)));
    }
    @Test void summaryLongIdentityIsSerializedAsExactStrings() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var json=mapper.readTree(mapper.writeValueAsString(service.getUsage(99L,A1)));
        assertEquals("1",json.get("tenantId").textValue());assertEquals(Long.toString(A1),json.get("sourceControlledFileId").textValue());assertEquals("10",json.get("masterId").textValue());
    }
    @Test void summaryReviewRejectsWrongMasterOnTheSelectedReference(){
        jdbc.update("UPDATE dcc_project_file_reference SET master_id=999 WHERE id=1");
        assertEquals(A1,jdbc.queryForObject("SELECT selected_controlled_file_id FROM dcc_project_file_reference WHERE id=1",Long.class));
        var failure=assertThrows(DccRelationFailure.class,()->service.getUsage(99L,A1));
        assertEquals("DCC_REFERENCE_USAGE_IDENTITY_INVALID",failure.getMessage());
    }
    @Test void summaryReviewRejectsMissingSelectedDestinationProjectEvenWithoutLabelAuthority(){
        // Project 3 is outside the readable destination scope; integrity failure must not expose its label.
        jdbc.update("DELETE FROM dcc_project_code WHERE id=3");
        var failure=assertThrows(DccRelationFailure.class,()->service.getUsage(99L,A1));
        assertEquals("DCC_REFERENCE_USAGE_IDENTITY_INVALID",failure.getMessage());
        verifyNoInteractions(projects,query);verify(access,never()).assertContentReadable(any(),any());
    }
    @Test void summaryReviewRejectsMissingSelectedDestinationFolder(){
        jdbc.update("DELETE FROM dcc_project_folder WHERE id=21");
        var failure=assertThrows(DccRelationFailure.class,()->service.getUsage(99L,A1));
        assertEquals("DCC_REFERENCE_USAGE_IDENTITY_INVALID",failure.getMessage());
    }
    @Test void summaryReviewIgnoresOtherVersionCorruptionAndKeepsTenantAndBoundCount(){
        jdbc.update("UPDATE dcc_project_file_reference SET master_id=999 WHERE id=2");
        jdbc.update("DELETE FROM dcc_project_code WHERE id=4");
        jdbc.update("DELETE FROM dcc_project_folder WHERE id=22");
        jdbc.update("DELETE FROM dcc_controlled_file WHERE id=?",A2);
        // A corrupt row belonging to another tenant is outside the selected tenant contract.
        jdbc.update("INSERT INTO dcc_project_file_reference(id,tenant_id,project_id,folder_id,master_id,selected_controlled_file_id,created_by) VALUES(8,2,999,999,999,?,7)",A1);
        var summary=service.getUsage(99L,A1);
        assertEquals(1L,summary.tenantId());assertEquals(A1,summary.sourceControlledFileId());assertEquals(10L,summary.masterId());
        assertEquals(2,summary.referenceProjectCount());assertTrue(summary.referenced());
        verify(access).assertNameVisible(99L,A1);verify(access,never()).assertContentReadable(any(),any());verifyNoInteractions(projects,query);
    }
    public static void main(String[] args) throws Exception {
        int passed=0,failed=0;
        var classes=new ArrayList<Class<?>>();classes.add(DccExactVersionReferenceUsageTest.class);
        if(args.length>0 && args[0].equals("regression"))classes.add(DccProjectReferenceTransactionTest.class);
        for(var cls:classes)for(var method:cls.getDeclaredMethods())if(method.isAnnotationPresent(Test.class)){
            if(args.length>0 && args[0].equals("summary-review") && !method.getName().startsWith("summaryReview"))continue;
            var test=cls.getDeclaredConstructor().newInstance();
            for(var before:cls.getDeclaredMethods())if(before.isAnnotationPresent(BeforeEach.class))before.invoke(test);
            try{method.invoke(test);passed++;System.out.println("PASS "+cls.getSimpleName()+"."+method.getName());}
            catch(java.lang.reflect.InvocationTargetException error){failed++;System.out.println("FAIL "+cls.getSimpleName()+"."+method.getName()+": "+error.getCause());}
            finally{for(var after:cls.getDeclaredMethods())if(after.isAnnotationPresent(AfterEach.class))after.invoke(test);}
        }
        System.out.println("fresh source / isolated H2: passed="+passed+", failed="+failed);if(failed>0)throw new AssertionError("isolated exact-version behavior failed");
    }
}
