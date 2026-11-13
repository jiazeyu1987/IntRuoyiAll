package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccProjectBrowserMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Execute the actual SQL authorization against isolated SELECT fixtures; no file scan in Java. */
class DccBrowserSqlAuthorizationTest {
    DccBrowserSqlAuthorization authorization;
    DccControlledFileCategoryPermissionSupport categories;
    DccControlledFileViewMatrixAccessService matrix;
    DccProjectAccessService projects;
    DccDirectoryAccessPermissionService directories;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        authorization=new DccBrowserSqlAuthorization();
        categories=mock(DccControlledFileCategoryPermissionSupport.class);
        matrix=mock(DccControlledFileViewMatrixAccessService.class);
        projects=mock(DccProjectAccessService.class);
        directories=mock(DccDirectoryAccessPermissionService.class);
        var projectMapper=mock(DccProjectCodeMapper.class);
        var mapper=mock(DccProjectBrowserMapper.class);
        ReflectionTestUtils.setField(authorization,"categories",categories);
        ReflectionTestUtils.setField(authorization,"matrix",matrix);
        ReflectionTestUtils.setField(authorization,"projects",projects);
        ReflectionTestUtils.setField(authorization,"projectMapper",projectMapper);
        ReflectionTestUtils.setField(authorization,"directories",directories);
        ReflectionTestUtils.setField(authorization,"mapper",mapper);
        ReflectionTestUtils.setField(authorization,"archiveAccess",mock(DccArchiveAccessService.class));
        ReflectionTestUtils.setField(authorization,"guard",mock(DccControlledFileDetailAuthorizationGuard.class));
        when(projects.listReadableProjectIds(99L)).thenReturn(List.of(300L));
        when(directories.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.QUERY)).thenReturn(Set.of(20L));
        when(directories.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(21L));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void directoryGrantAndMatrixAreIndependentAndEachRequestObservesRevocation() throws Exception {
        var rows=List.of(file(1,1,300,20,88,"ACTIVE","one.pdf"),file(2,1,300,21,88,"ACTIVE","two.pdf"),file(3,1,300,22,88,"ACTIVE","three.pdf"),file(4,2,300,20,88,"ACTIVE","foreign.pdf"));
        assertEquals(List.of(1L,2L),selected(authorization.scope(99L,false,null,false,true,List.of()),rows));
        when(directories.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.QUERY)).thenReturn(Set.of());
        when(directories.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of());
        assertEquals(List.of(),selected(authorization.scope(99L,false,null,false,true,List.of()),rows));
        when(matrix.accessibleCategoryIds(99L)).thenReturn(Set.of(10L));
        assertEquals(List.of(1L,2L,3L),selected(authorization.scope(99L,false,null,false,true,List.of()),rows));
    }
    @Test void assignmentDoesNotGrantViewAndBoundsManagerWhileEmptyRetainsOnlyIndependentGrants() throws Exception {
        var rows=List.of(file(1,1,300,22,88,"ACTIVE","one.pdf"),file(2,1,300,22,88,"ACTIVE","two.pdf"));
        assertTrue(selected(authorization.scope(99L,false,Set.of(1L),false,true,List.of()),rows).isEmpty());
        assertEquals(List.of(1L),selected(authorization.scope(99L,true,Set.of(1L),false,true,List.of()),rows));
        assertTrue(selected(authorization.scope(99L,true,Set.of(),false,true,List.of()),rows).isEmpty());
        when(matrix.accessibleCategoryIds(99L)).thenReturn(Set.of(10L));
        assertEquals(List.of(1L,2L),selected(authorization.scope(99L,true,Set.of(),false,true,List.of()),rows));
        assertTrue(selected(authorization.historyScope(99L,true,Set.of(),List.of()),rows).isEmpty());
    }
    @Test void blacklistAppliesToManagersAndUsesLastExtensionFromExactSource() throws Exception {
        var rows=List.of(file(1,1,300,20,88,"ACTIVE","one.PDF"),file(2,1,300,20,88,"ACTIVE","two.DB"),file(3,1,300,20,88,"ACTIVE","three.db.pdf"),file(4,1,300,20,88,"SUPERSEDED","old.pdf"),file(5,1,300,20,88,"OBSOLETE","obsolete.pdf"));
        assertEquals(List.of(1L,3L),selected(authorization.scope(99L,true,null,false,true,List.of("*.db")),rows));
    }
    @Test void pendingCsvKeepsJavaLongWhitespacePlusAndLeadingZeroMeaning() throws Exception {
        var rows=List.of(file(3,1,300,22,88,"PENDING_MATRIX_REVIEW","pending.pdf"),file(4,1,300,22,88,"CONTROLLED_PENDING_EFFECTIVE","future.pdf"));
        assertEquals(List.of(3L),selected(authorization.scope(99L,false,null,false,false,List.of()),rows));
    }
    @Test void requesterStillRequiresReadableProjectForProjectBrowser() throws Exception {
        var rows=List.of(file(1,1,300,22,99,"WORKING","own.pdf"),file(2,1,301,22,99,"WORKING","other.pdf"));
        assertEquals(List.of(1L),selected(authorization.scope(99L,false,null,true,true,List.of()),rows));
    }
    @Test void archiveRequiresExplicitArchiveQueryEvenForManagerAndDirectoryGrant() throws Exception {
        var rows=List.of(file(1,1,300,20,88,"SUPERSEDED","old.pdf"),file(2,1,300,20,88,"OBSOLETE","obsolete.pdf"));
        assertEquals(List.of(),selected(authorization.scope(99L,true,null,true,true,List.of()),rows));
        var archive=mock(DccArchiveAccessService.class);when(archive.canQuery(99L)).thenReturn(true);
        ReflectionTestUtils.setField(authorization,"archiveAccess",archive);
        assertEquals(List.of(1L,2L),selected(authorization.scope(99L,false,null,true,true,List.of()),rows));
        assertEquals(List.of(),selected(authorization.scope(99L,true,null,false,false,List.of()),rows));
    }
    private String file(long id,long tenant,long project,long directory,long requester,String status,String name) {
        return "SELECT "+id+" id,"+tenant+" tenant_id,0 deleted,'"+status+"' status,NULL controlled_time,'' change_type,'' revision_change_type,"+project+" dcc_project_code_id,"+directory+" directory_id,"+requester+" requester_id,10 category_id,'p"+id+"' process_instance_id,'"+name+"' source_original_file_name,'"+name+"' file_name,'"+name+"' title";
    }
    private List<Long> selected(QueryWrapper<Object> permission,List<String> rows) throws Exception {
        String fixtures="WITH dcc_project_code AS (SELECT 300 id,1 tenant_id,0 deleted,'PROJECT' library_scope),"+
                "dcc_controlled_file_route_snapshot AS (SELECT 3 controlled_file_id,1 tenant_id,0 deleted,'MATRIX_REVIEW' stage_code,CHAR(9)||'+00099'||CHAR(10) resolved_user_ids),"+
                "ACT_RU_TASK AS (SELECT '' TENANT_ID_,'' PROC_INST_ID_,'' TASK_DEF_KEY_,'' ASSIGNEE_),"+
                "dcc_controlled_file_distribution_recipient AS (SELECT 1 distribution_id,1 tenant_id,0 deleted,99 user_id),"+
                "dcc_controlled_file_distribution AS (SELECT 1 id,4 controlled_file_id,1 tenant_id,0 deleted,'PUBLIC_FOLDER' distribution_medium,'SENT' status),"+
                "dcc_controlled_file AS ("+String.join(" UNION ALL ",rows)+") SELECT f.id FROM dcc_controlled_file f WHERE (${permission.sqlSegment}) ORDER BY f.id";
        Map<String,Object> params=Map.of("permission",permission,"historyPermission",permission);
        var config=new Configuration();
        var bound=new XMLLanguageDriver().createSqlSource(config,"<script>"+fixtures+"</script>",Map.class).getBoundSql(params);
        net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(bound.getSql());
        try(var connection=DriverManager.getConnection("jdbc:h2:mem:dcc_scope_"+UUID.randomUUID()+";MODE=MySQL","sa","");var statement=connection.prepareStatement(bound.getSql())){
            for(int i=0;i<bound.getParameterMappings().size();i++)statement.setObject(i+1,config.newMetaObject(params).getValue(bound.getParameterMappings().get(i).getProperty()));
            try(var result=statement.executeQuery()){var ids=new ArrayList<Long>();while(result.next())ids.add(result.getLong(1));return ids;}
        }
    }
}
