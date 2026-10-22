package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccProjectReferenceController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.*;
import org.springframework.test.context.TestPropertySource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Real B VIEW directory/leader and D reference/query guards, state rows and GxP ledger on task-owned H2. */
@Import({DccProjectReferenceController.class,DccProjectReferenceService.class,DccProjectReferenceAuthorityImpl.class,
        DccLatestControlledFileResolverImpl.class,DccRelationStore.class})
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_d_reference_view;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=value")
class DccProjectReferenceViewDirectoryCombinationTest extends DccProjectFormalCombinationTest {
    @Resource DccProjectReferenceService referenceService;
    @Resource DccProjectReferenceController referenceController;
    MockMvc mvc;com.fasterxml.jackson.databind.ObjectMapper json;DccControlledFileQueryServiceImpl query;
    @BeforeEach void referenceFixture() throws Exception {
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))
            if(!sql.isBlank())jdbc.execute(sql);
        jdbc.update("DELETE FROM dcc_project_file_reference");
        jdbc.update("DELETE FROM dcc_current_file_relation");
        jdbc.update("DELETE FROM dcc_current_file_relation_set");
        jdbc.update("DELETE FROM dcc_controlled_file_related_file");
        jdbc.execute("ALTER TABLE dcc_project_file_reference ALTER COLUMN id RESTART WITH 9007199254742001");
        var configuration=new YudaoJacksonAutoConfiguration();var builder=new Jackson2ObjectMapperBuilder();
        configuration.ldtEpochMillisCustomizer().customize(builder);builder.modulesToInstall(configuration.timestampSupportModuleBean());json=builder.build();
        mvc=MockMvcBuilders.standaloneSetup(projectController,referenceController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("dcc-d-reference-view",apiErrorLogs)).build();
        query=new DccControlledFileQueryServiceImpl();
        for(var field:DccControlledFileQueryServiceImpl.class.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())&&field.getAnnotation(Resource.class)!=null)
            ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
        ReflectionTestUtils.setField(query,"controlledFileMapper",files);
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(anyLong(),anyLong())).thenReturn(true);
        ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        var directory=mock(DccDirectoryAccessPermissionService.class);
        when(directory.getAuthorizedDirectoryIds(anyLong(),eq(DccAccessTypeEnum.QUERY))).thenReturn(Set.of(101L));
        when(directory.getAuthorizedDirectoryIds(anyLong(),eq(DccAccessTypeEnum.PREVIEW))).thenReturn(Set.of());
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directory);
        // Execute actual C guards on source rows; only upstream policy APIs are isolated.
        doAnswer(call->{query.assertRelationNameVisible(call.getArgument(0),call.getArgument(1));return null;}).when(sourceAccess).assertNameVisible(any(),any());
        doAnswer(call->{query.assertRelationContentReadable(call.getArgument(0),call.getArgument(1));return null;}).when(sourceAccess).assertContentReadable(any(),any());
        policy("dcc.project-reference.create");policy("dcc.project-reference.cancel");
        when(permissions.hasAnyPermissions(anyLong(),eq("dcc:project-code:query"),eq("dcc:controlled-file:query"))).thenReturn(true);
    }
    @AfterEach void clearReferenceFacts(){
        for(String table:List.of("dcc_project_file_reference","dcc_current_file_relation","dcc_current_file_relation_set","dcc_controlled_file_related_file"))jdbc.update("DELETE FROM "+table);
    }
    void grant(Long project,Long actor,String level){
        var rule=new DccProjectAccessRuleDO();rule.setTenantId(1L);rule.setDccProjectCodeId(project);rule.setSubjectType("USER");
        rule.setSubjectId(actor);rule.setAccessLevel(level);rule.setActive(true);rule.setChangeReason("D引用目录正式范围");rules.insert(rule);
    }
    com.fasterxml.jackson.databind.JsonNode http(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        var response=mvc.perform(request).andReturn().getResponse();assertEquals(200,response.getStatus());
        return json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }
    String batch(Long project,Long folder,Long file){return JsonUtils.toJsonString(Map.of("projectId",String.valueOf(project),"folderId",String.valueOf(folder),
            "selectedFileIds",List.of(String.valueOf(file)),"reason","实际批量引用"));}
    String cancel(Long project,Long folder,Long master,Long reference,boolean confirmed){return JsonUtils.toJsonString(Map.of("projectId",String.valueOf(project),
            "folderId",String.valueOf(folder),"masterId",String.valueOf(master),"referenceId",String.valueOf(reference),"confirmed",confirmed,"reason","取消最后入口"));}
    @ParameterizedTest @ValueSource(strings={"VIEW","OWNER","ADMIN","OTHER_PROJECT_LEADER","DISABLED_TARGET_LEADER"})
    void referenceViewReaderScopeNeverSubstitutesTheEnabledUniqueTargetLeader(String identity) throws Exception {
        var target=project("REFERENCE-VIEW-TARGET",7L);var origin=project("REFERENCE-VIEW-SOURCE",9L);
        var folder=folder(target.getId(),"empty-authorized");var source=file(origin.getId(),101L,"名称可见正文不可读.pdf");
        var existing=referenceService.create(7L,target.getId(),folder.getId(),source.getId(),"负责人建立入口");
        long actor="ADMIN".equals(identity)?1:"OTHER_PROJECT_LEADER".equals(identity)?9:"DISABLED_TARGET_LEADER".equals(identity)?7:8;
        if(actor!=7)grant(target.getId(),actor,"OWNER".equals(identity)?"OWNER":"VIEW");login(actor);
        if("DISABLED_TARGET_LEADER".equals(identity))when(users.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L).setStatus(1));
        var directory=http(get("/dcc/project-codes/{project}/folders",target.getId()));
        assertEquals("DISABLED_TARGET_LEADER".equals(identity)?1080000341:0,directory.get("code").intValue());
        var created=http(post("/dcc/project-file-references/batch").contentType("application/json").content(batch(target.getId(),folder.getId(),source.getId())));
        assertEquals("DISABLED_TARGET_LEADER".equals(identity)?1080090003:1080090004,created.get("code").intValue());
        var removed=http(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(target.getId(),folder.getId(),source.getMasterId(),existing.reference().id(),true)));
        assertEquals(created.get("code").intValue(),removed.get("code").intValue());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));assertEquals(1,events.selectList().size());
        assertThrows(ServiceException.class,()->query.assertRelationContentReadable(actor,source.getId()));
    }
    @Test void referenceViewFormalBatchPinnedVersionCountExactCancelAndHistoryPreservation() throws Exception {
        final long leader=9223372036854775707L;when(users.getUser(leader)).thenReturn(new AdminUserRespDTO().setId(leader).setStatus(0));
        when(permissions.getUserRoleIdListByUserId(leader)).thenReturn(Set.of());
        var target=project("REFERENCE-LONG-TARGET",leader);var origin=project("REFERENCE-LONG-SOURCE",9L);
        var folder=folder(target.getId(),"first");var second=folder(target.getId(),"second");var source=file(origin.getId(),101L,"Pinned source.pdf");
        jdbc.update("INSERT INTO dcc_current_file_relation_set(tenant_id,source_master_id,controlled_file_id) VALUES(1,?,?)",source.getMasterId(),source.getId());
        jdbc.update("INSERT INTO dcc_current_file_relation(tenant_id,source_master_id,related_master_id) VALUES(1,?,?)",source.getMasterId(),Long.MAX_VALUE-5);
        jdbc.update("INSERT INTO dcc_controlled_file_related_file(controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,relation_source,tenant_id) VALUES(?,?,?,?, 'UPLOAD',1)",source.getId(),Long.MAX_VALUE-7,origin.getId(),Long.MAX_VALUE-5);
        String original=JsonUtils.toJsonString(files.selectById(source.getId()));login(leader);
        var directory=http(get("/dcc/project-codes/{project}/folders",target.getId()));assertEquals(0,directory.get("code").intValue());
        var created=http(post("/dcc/project-file-references/batch").contentType("application/json").content(batch(target.getId(),folder.getId(),source.getId())));
        assertEquals(0,created.get("code").intValue());var ref=created.get("data").get(0);
        assertTrue(ref.get("reference").get("id").isTextual());assertEquals(String.valueOf(leader),ref.get("reference").get("createdBy").textValue());
        long reference=Long.parseLong(ref.get("reference").get("id").textValue());assertEquals(1,ref.get("referenceProjectCount").intValue());
        var inSecond=referenceService.create(leader,target.getId(),second.getId(),source.getId(),"同项目第二目录");assertEquals(1,inSecond.referenceProjectCount());
        var future=files.selectById(source.getId());future.setId(null);future.setVersionNo("B/1");future.setStatus("CONTROLLED_PENDING_EFFECTIVE");
        future.setActivatedTime(null);future.setProcessInstanceId("formal-next-round");future.setControlledTime(LocalDateTime.now().withNano(0));files.insert(future);
        var master=masters.selectById(source.getMasterId());master.setLatestControlledFileId(future.getId());masters.updateById(master);
        var listed=http(get("/dcc/project-file-references").param("projectId",String.valueOf(target.getId())).param("folderId",String.valueOf(folder.getId())));
        assertEquals(String.valueOf(source.getId()),listed.get("data").get(0).get("selectedVersion").get("controlledFileId").textValue());
        assertEquals("A/1",listed.get("data").get(0).get("selectedVersion").get("versionNo").textValue());
        var unconfirmed=http(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(target.getId(),folder.getId(),source.getMasterId(),reference,false)));
        assertEquals(DccRelationErrorCodes.BUSINESS_FAILURE,unconfirmed.get("code").intValue());
        var firstCancel=http(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(target.getId(),folder.getId(),source.getMasterId(),reference,true)));
        assertEquals(1,firstCancel.get("data").intValue());
        var lastCancel=http(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(target.getId(),second.getId(),source.getMasterId(),inSecond.reference().id(),true)));
        assertEquals(0,lastCancel.get("data").intValue());assertEquals(original,JsonUtils.toJsonString(files.selectById(source.getId())));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation WHERE source_master_id=?",Integer.class,source.getMasterId()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_related_file WHERE controlled_file_id=?",Integer.class,source.getId()));
        assertEquals(4,events.selectList().size());assertThrows(ServiceException.class,()->query.assertRelationContentReadable(leader,source.getId()));
        var evidence=new LinkedHashMap<String,Object>();evidence.put("projectId",String.valueOf(target.getId()));evidence.put("projectName",target.getProjectName());
        evidence.put("projectLeaderUserId",String.valueOf(leader));evidence.put("leaderAccount",Map.of("id",String.valueOf(leader),"status",0));
        evidence.put("folders",directory.get("data"));evidence.put("created",created.get("data"));evidence.put("listed",listed.get("data"));
        evidence.put("firstCancel",firstCancel.get("data"));evidence.put("lastCancel",lastCancel.get("data"));evidence.put("sourceProjectId",String.valueOf(origin.getId()));evidence.put("sourceProjectName",origin.getProjectName());
        Files.writeString(Path.of("../../doc/tasks/20260930-dcc-d-relations/reference-view-real-responses.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(evidence),StandardCharsets.UTF_8);
    }
}
