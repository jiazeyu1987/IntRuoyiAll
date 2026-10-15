package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserDirectSupervisorRespDTO;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Offline MVC serialization of actual source failures; workflow/security runtime is outside this scope. */
class DccSupervisorSourceHttpBoundaryTest {
    AdminUserApi users;
    DccControlledFileWorkflowService workflow;
    DccUserManagementDirectSupervisorSource source;
    MockMvc mvc;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(99L).setTenantId(1L).setUserType(2),new MockHttpServletRequest());
        users=mock(AdminUserApi.class);workflow=mock(DccControlledFileWorkflowService.class);
        source=new DccUserManagementDirectSupervisorSource();ReflectionTestUtils.setField(source,"adminUserApi",users);
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        when(users.getDirectSupervisorRelation(99L)).thenReturn(new AdminUserDirectSupervisorRespDTO(1L,99L,null,0L));
        when(workflow.submitWorkingIteration(eq(99L),eq(20L),any())).thenAnswer(ignored->{
            source.require(new DccDirectSupervisorSource.Context(1L,99L,20L,5L,"REVISION"));
            throw new AssertionError("This boundary fixture must reject before workflow start");
        });
        var controller=new DccControlledFileController();ReflectionTestUtils.setField(controller,"workflowService",workflow);
        mvc=MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler("ic32-offline",mock(ApiErrorLogCommonApi.class))).build();
    }
    @AfterEach void clear(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @ParameterizedTest @ValueSource(strings={"UNCONFIGURED","DISABLED","RELATION_INVALID"})
    void sourceBusinessFailureReachesTheExistingHttpCallerWithItsExactReason(String scenario)throws Exception {
        if(scenario.equals("DISABLED")) {
            when(users.getDirectSupervisorRelation(99L)).thenReturn(new AdminUserDirectSupervisorRespDTO(1L,99L,88L,1L));
            when(users.getUser(88L)).thenReturn(new AdminUserRespDTO().setId(88L).setTenantId(1L).setStatus(1));
        }
        if(scenario.equals("RELATION_INVALID"))when(users.getDirectSupervisorRelation(99L)).thenReturn(new AdminUserDirectSupervisorRespDTO(2L,99L,88L,1L));
        var result=mvc.perform(post("/dcc/controlled-files/20/submit").contentType("application/json")
                .content("{\"idempotencyKey\":\"ic32-http\",\"effectiveMode\":\"AFTER_STANDARDIZATION_SIGNATURE\"}")).andReturn();
        var json=new ObjectMapper().readTree(result.getResponse().getContentAsString());
        verify(workflow).submitWorkingIteration(eq(99L),eq(20L),any());
        assertNotEquals(500,json.path("code").asInt(),"Domain configuration failure must not become generic 500");
        assertNotEquals(0,json.path("code").asInt());
        String expected=scenario.equals("UNCONFIGURED")?"DIRECT_SUPERVISOR_NOT_CONFIGURED":scenario.equals("DISABLED")?"DIRECT_SUPERVISOR_ACCOUNT_INVALID":"DIRECT_SUPERVISOR_RELATION_INVALID";
        assertTrue(json.path("msg").asText().contains(expected),json.toString());
        if(scenario.equals("UNCONFIGURED"))assertTrue(json.path("msg").asText().contains("用户管理"));
    }
    @Test void technicalDirectoryFailureRemainsTheOriginalTechnicalFailure()throws Exception {
        var failure=new IllegalStateException("ic32-directory-unavailable");
        when(users.getDirectSupervisorRelation(99L)).thenThrow(failure);
        var result=mvc.perform(post("/dcc/controlled-files/20/submit").contentType("application/json")
                .content("{\"idempotencyKey\":\"ic32-http\",\"effectiveMode\":\"AFTER_STANDARDIZATION_SIGNATURE\"}")).andReturn();
        assertSame(failure,result.getResolvedException());
        assertEquals(500,new ObjectMapper().readTree(result.getResponse().getContentAsString()).path("code").asInt());
    }
}
