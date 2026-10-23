package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DccProjectReferenceApiPersistenceTest {
    JdbcTemplate jdbc;MockMvc mvc;DccProjectReferenceAuthority authority;DccRelationAccessPolicy access;
    DccLatestControlledFileResolver files;GxpAuditService audit;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MYSQL;DB_CLOSE_DELAY=-1");jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE dcc_controlled_file_master (id BIGINT,tenant_id BIGINT,deleted INT)");jdbc.update("INSERT INTO dcc_controlled_file_master VALUES (10,1,0),(20,1,0)");
        jdbc.execute("CREATE TABLE dcc_project_code(id BIGINT,tenant_id BIGINT,deleted INT)");
        jdbc.execute("CREATE TABLE dcc_project_folder(id BIGINT,project_code_id BIGINT,tenant_id BIGINT,deleted INT)");
        jdbc.update("INSERT INTO dcc_project_code VALUES(2,1,0)");jdbc.update("INSERT INTO dcc_project_folder VALUES(21,2,1,0)");
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())jdbc.execute(sql);
        files=mock(DccLatestControlledFileResolver.class);authority=mock(DccProjectReferenceAuthority.class);access=mock(DccRelationAccessPolicy.class);audit=mock(GxpAuditService.class);
        when(files.resolveSelectedForUpdate(any())).thenAnswer(call->files.resolveSelected((Long)call.getArgument(0)));
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N1","一.pdf","A/1","ACTIVE",true,false,true));
        when(files.resolveSelected(200L)).thenReturn(new FileVersion(1L,200L,20L,9L,"N2","二.pdf","A/1","ACTIVE",true,false,true));
        var target=new DccProjectReferenceService(new DccRelationStore(jdbc,audit),authority,access,files);
        var proxy=new ProxyFactory(target);proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds),new AnnotationTransactionAttributeSource()));
        var controller=new DccProjectReferenceController((DccProjectReferenceService)proxy.getProxy());
        mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new DccRelationsExceptionHandler()).build();
        TenantContextHolder.setTenantId(1L);login(7L);
    }
    void login(long id){var user=new LoginUser();user.setId(id);user.setTenantId(1L);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));}
    @AfterEach void clear(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @Test void actualApiBatchCreatesAndExactCancelPreservesOtherVersionAndUsage() throws Exception {
        mvc.perform(post("/dcc/project-file-references/batch").contentType("application/json").content("{\"projectId\":2,\"folderId\":21,\"selectedFileIds\":[100,200],\"reason\":\"确认引用\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data[0].reference.masterId").value("10"));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        long ref=jdbc.queryForObject("SELECT id FROM dcc_project_file_reference WHERE master_id=10",Long.class);
        mvc.perform(post("/dcc/project-file-references/cancel").contentType("application/json").content("{\"projectId\":2,\"folderId\":21,\"masterId\":10,\"referenceId\":"+ref+",\"confirmed\":true,\"reason\":\"确认取消\"}"))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(0));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        mvc.perform(get("/dcc/project-file-references/usage").param("selectedFileId","100")).andExpect(jsonPath("$.data.referenced").value(false));
        verify(authority,atLeastOnce()).assertProjectLeader(7L,2L);verify(access,never()).assertContentReadable(any(),any());
    }
    @Test void actualApiFailureRollsBackBatchAndCannotSubstituteClientActorForNonLeaderAdmin() throws Exception {
        doThrow(new DccRelationFailure("DCC_NAME_DENIED")).when(access).assertNameVisible(7L,200L);
        mvc.perform(post("/dcc/project-file-references/batch").contentType("application/json").content("{\"projectId\":2,\"folderId\":21,\"selectedFileIds\":[100,200],\"reason\":\"确认引用\"}"))
                .andExpect(jsonPath("$.code").value(DccRelationErrorCodes.BUSINESS_FAILURE));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        login(1L);doThrow(new DccRelationFailure("DCC_PROJECT_LEADER_DENIED")).when(authority).assertProjectLeader(1L,2L);
        mvc.perform(post("/dcc/project-file-references").contentType("application/json").content("{\"projectId\":2,\"folderId\":21,\"selectedFileId\":100,\"actorId\":7,\"reason\":\"伪造操作者\"}"))
                .andExpect(jsonPath("$.code").value(DccRelationErrorCodes.BUSINESS_FAILURE));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));verify(authority).assertProjectLeader(1L,2L);
    }
}
