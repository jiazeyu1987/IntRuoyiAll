package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.http.converter.json.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Actual C browser controller/query/count/page/Mapper and application Jackson -> D loader contract fixture. */
class DccSelectorBrowserMappingIntegrationTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource dataSource;
    @Resource DccControlledFileSelectorMapper selectors;
    @Resource DccControlledFileMapper files;
    JdbcTemplate jdbc;MockMvc mvc;com.fasterxml.jackson.databind.ObjectMapper json;
    @BeforeEach void fixture() throws Exception {
        jdbc=new JdbcTemplate(dataSource);try(var connection=dataSource.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        TenantContextHolder.setTenantId(1L);var login=new LoginUser();login.setId(99L);login.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
        jdbc.update("DELETE FROM dcc_project_file_placement");
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status) VALUES(5,1,'正式来源项目','P5','ENABLE'),(6,1,'跨项目','P6','ENABLE')");
        jdbc.update("INSERT INTO dcc_file_directory(id,tenant_id,code,name) VALUES(5,1,'NAS5','NAS目录'),(6,1,'NAS6','无权目录')");
        jdbc.update("INSERT INTO dcc_project_folder(id,tenant_id,project_code_id,parent_id,name,sort_order,active,source_template_id,source_node_key,deleted) VALUES(500,1,5,0,'逻辑文件夹',0,TRUE,1,'mapped500',0),(501,1,6,0,'其他逻辑文件夹',0,TRUE,1,'mapped501',0)");
        master(10,5,20L,21L);file(20,10,5,5,"A/1","ACTIVE");file(21,10,5,5,"A/2","CONTROLLED_PENDING_EFFECTIVE");file(24,10,5,5,"A/2-1","WORKING");
        master(11,6,null,22L);file(22,11,6,5,"B/1","CONTROLLED_PENDING_EFFECTIVE");
        master(12,5,9007199254740993L,9007199254740993L);file(9007199254740993L,12,5,5,"A/1","ACTIVE");
        master(13,6,25L,25L);file(25,13,6,6,"A/1","ACTIVE");
        jdbc.update("INSERT INTO dcc_project_file_placement(id,tenant_id,project_code_id,project_folder_id,controlled_file_id,storage_directory_id) VALUES(1,1,5,500,21,5),(2,1,6,501,22,5)");
        var query=new DccControlledFileQueryServiceImpl();
        // Isolate only external policy/storage ports; the Query implementation and SQL are real.
        for(var field:DccControlledFileQueryServiceImpl.class.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers()) && field.getAnnotation(Resource.class)!=null)
            ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
        ReflectionTestUtils.setField(query,"selectorMapper",selectors);ReflectionTestUtils.setField(query,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        var permissions=mock(DccDirectoryAccessPermissionService.class);
        when(permissions.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.QUERY)).thenReturn(Set.of(5L));
        when(permissions.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of());
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",permissions);
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenReturn(true);
        ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        var storage=mock(FileMapper.class);when(storage.selectById(501L)).thenReturn(FileDO.builder().id(501L).name("controlled.pdf").type("application/pdf").build());
        ReflectionTestUtils.setField(query,"fileMapper",storage);
        var settings=mock(DccControlledFileBrowserSettingsService.class);when(settings.getBlacklistedExtensionPatterns()).thenReturn(List.of());
        ReflectionTestUtils.setField(query,"browserSettingsService",settings);
        var controller=new DccControlledFileController();ReflectionTestUtils.setField(controller,"queryService",query);
        var configuration=new YudaoJacksonAutoConfiguration();var builder=new Jackson2ObjectMapperBuilder();
        configuration.ldtEpochMillisCustomizer().customize(builder);builder.modulesToInstall(configuration.timestampSupportModuleBean());json=builder.build();
        mvc=MockMvcBuilders.standaloneSetup(controller).setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
    }
    @AfterEach void clear(){jdbc.update("DELETE FROM dcc_project_file_placement");SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    @Test void formalBrowserResponsesFeedDMappingWithoutClientPaginationOrNasInference() throws Exception {
        var first=page("GLOBAL",null,null,1,2);var second=page("GLOBAL",null,null,2,2);
        var directory=page("PROJECT_FOLDER","5","500",1,2);var wrongNas=page("PROJECT_FOLDER","5","5",1,2);
        assertEquals(3,first.get("total").intValue());assertEquals(2,first.get("list").size());
        assertEquals("21",first.get("list").get(0).get("id").textValue());assertEquals("21",first.get("list").get(0).get("latestControlledFileId").textValue());
        assertTrue(first.get("list").get(0).get("pendingEffect").booleanValue());assertFalse(first.get("list").get(0).get("canPreview").booleanValue());
        assertEquals("9007199254740993",second.get("list").get(0).get("id").textValue());
        assertTrue(second.get("list").get(0).get("projectFolderId").isNull());assertTrue(second.get("list").get(0).get("projectFolderName").isNull());
        assertEquals(1,directory.get("total").intValue());assertEquals("500",directory.get("list").get(0).get("projectFolderId").textValue());
        assertEquals(0,wrongNas.get("total").intValue());
        var artifact=Map.of("globalFirst",first,"globalSecond",second,"projectFolder",directory,"wrongNasFolder",wrongNas);
        Path path=Path.of("../../doc/tasks/20260930-dcc-d-relations/h02-real-selector-responses.json");
        Files.writeString(path,json.writerWithDefaultPrettyPrinter().writeValueAsString(artifact),StandardCharsets.UTF_8);
    }
    private com.fasterxml.jackson.databind.JsonNode page(String scope,String project,String folder,int number,int size) throws Exception {
        var request=get("/dcc/controlled-files/browser-page").param("selectorScope",scope).param("pageNo",String.valueOf(number)).param("pageSize",String.valueOf(size));
        if(project!=null)request.param("dccProjectCodeId",project);if(folder!=null)request.param("projectFolderId",folder);
        var result=mvc.perform(request).andReturn();assertEquals(200,result.getResponse().getStatus());
        var body=json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));assertEquals(0,body.get("code").intValue());return body.get("data");
    }
    private void master(long id,long project,Long active,Long latest){jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,status,current_active_controlled_file_id,latest_controlled_file_id) VALUES(?,1,2,5,?,?,?,'ACTIVE_CHAIN',?,?)",id,"M"+id,"N"+id,project,active,latest);}
    private void file(long id,long master,long project,long directory,String version,String status){files.insert(DccControlledFileDO.builder().id(id).tenantId(1L).masterId(master).categoryId(2L)
            .directoryId(directory).dccProjectCodeId(project).sourceFileId(100L).originalFileId(100L).publishedFileId(501L)
            .title("Title"+id).fileName("Template"+id).sourceOriginalFileName("SOURCE-"+id+".pdf").fileNumber("N"+id).versionNo(version).status(status).requesterId(88L).submitterId(88L)
            .controlledTime("WORKING".equals(status)?null:LocalDateTime.of(2026,10,1,12,0)).activatedTime("ACTIVE".equals(status)?LocalDateTime.of(2026,10,1,12,1):null).build());}
}
