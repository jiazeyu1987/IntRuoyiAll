package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApiImpl;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDirectSupervisorSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.annotation.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorErrors.*;

@Import({AdminUserDirectSupervisorService.class, AdminUserDirectSupervisorServiceTest.Beans.class})
@TestPropertySource(properties="spring.sql.init.encoding=UTF-8")
class AdminUserDirectSupervisorServiceTest extends BaseDbUnitTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class Beans { @Bean JdbcTemplate jdbcTemplate(javax.sql.DataSource source) { return new JdbcTemplate(source); } }
    @Resource AdminUserDirectSupervisorService service;
    @Resource JdbcTemplate jdbc;
    @MockitoSpyBean AdminUserMapper users;
    @MockitoBean PermissionApi permissions;
    @MockitoBean GxpAuditService audit;
    @BeforeEach void fixture() {
        login(1L,1L,2);
        for(long id:new long[]{1,7,8,9}) jdbc.update("INSERT INTO system_users(id,tenant_id,username,canonical_username,nickname,status) VALUES(?,1,?,?,?,0)",id,"account"+id,"account"+id,"账号"+id);
        when(permissions.hasAnyPermissions(1L,"system:user:update")).thenReturn(true);
        when(permissions.hasAnyPermissions(1L,"system:user:query")).thenReturn(true);
    }
    void login(Long id,Long tenant,Integer type) {
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(id).setTenantId(tenant).setUserType(type)
            .setInfo(Map.of("username","account"+id,"nickname","账号"+id)),new org.springframework.mock.web.MockHttpServletRequest());
    }
    @AfterEach void clearLogin() { SecurityContextHolder.clearContext(); }
    UserDirectSupervisorSaveReqVO request(Long supervisor, long version) {
        var req=new UserDirectSupervisorSaveReqVO();req.setUserId(7L);req.setSupervisorUserId(supervisor);
        req.setOperation(supervisor==null?"CLEAR":"SET");req.setExpectedRelationVersion(version);req.setChangeReason("维护正式直属主管");return req;
    }
    void code(int expected, org.junit.jupiter.api.function.Executable action) {
        assertEquals(expected,assertThrows(ServiceException.class,action).getCode());
    }
    void unchanged() {
        var row=users.selectById(7L);assertNull(row.getDirectSupervisorUserId());assertEquals(0L,row.getDirectSupervisorRevision());
    }
    @Test void actualSetChangeAndExplicitClearPersistServerVersionsAndTypedApi() {
        var initial=service.getDirectSupervisorRelation(7L);assertNull(initial.getSupervisorUserId());assertEquals(0L,initial.getRelationVersion());
        var saved=service.saveRelation(1L,request(8L,0));assertEquals(8L,saved.getSupervisorUserId());assertEquals(1L,saved.getRelationVersion());
        var api=new AdminUserApiImpl();ReflectionTestUtils.setField(api,"directSupervisorService",service);
        var dto=api.getDirectSupervisorRelation(7L);assertEquals(1L,dto.getTenantId());assertEquals(7L,dto.getUserId());assertEquals(8L,dto.getSupervisorUserId());assertEquals(1L,dto.getRelationVersion());
        assertEquals(2L,service.saveRelation(1L,request(9L,1)).getRelationVersion());
        var cleared=service.saveRelation(1L,request(null,2));assertNull(cleared.getSupervisorUserId());assertEquals(3L,cleared.getRelationVersion());
        assertNull(users.selectById(7L).getDirectSupervisorUserId());verify(audit,times(3)).append(any());
    }
    @Test void currentSameValueAndAlreadyClearAreNoOpButOldVersionAlwaysConflicts() {
        service.saveRelation(1L,request(8L,0));clearInvocations(audit,users);
        assertEquals(1L,service.saveRelation(1L,request(8L,1)).getRelationVersion());verifyNoInteractions(audit);
        verify(users,never()).updateDirectSupervisorRelation(any(),any(),any(),any(),any(),any());
        code(CONFLICT.getCode(),()->service.saveRelation(1L,request(8L,0)));
        code(CONFLICT.getCode(),()->service.saveRelation(1L,request(9L,0)));
        service.saveRelation(1L,request(null,1));clearInvocations(audit);
        assertEquals(2L,service.saveRelation(1L,request(null,2)).getRelationVersion());verifyNoInteractions(audit);
    }
    @Test void permissionDeniedRejectsQueryAndWriteWithoutMutation() {
        when(permissions.hasAnyPermissions(1L,"system:user:update")).thenReturn(false);
        when(permissions.hasAnyPermissions(1L,"system:user:query")).thenReturn(false);
        code(FORBIDDEN.getCode(),()->service.saveRelation(1L,request(8L,0)));
        code(FORBIDDEN.getCode(),()->service.getRelation(1L,7L));unchanged();verifyNoInteractions(audit);
    }
    @Test void crossTenantOrDisabledTargetActorAndSupervisorCannotWrite() {
        for(long id:new long[]{1,7,8}) {
            jdbc.update("UPDATE system_users SET tenant_id=2 WHERE id=?",id);
            code(INVALID.getCode(),()->service.saveRelation(1L,request(8L,0)));unchanged();
            jdbc.update("UPDATE system_users SET tenant_id=1,status=1 WHERE id=?",id);
            code(INVALID.getCode(),()->service.saveRelation(1L,request(8L,0)));unchanged();
            jdbc.update("UPDATE system_users SET status=0 WHERE id=?",id);
        }
        verifyNoInteractions(audit);
    }
    @Test void foreignReturnedIdentityAndMissingTenantAreNotAllowedEvenIfMapperReturnsAnAccount() {
        var foreign=users.selectById(8L);
        doReturn(foreign).when(users).selectDirectSupervisorUserForUpdate(1L,7L);
        code(INVALID.getCode(),()->service.saveRelation(1L,request(8L,0)));unchanged();
        reset(users);TenantContextHolder.setIgnore(true);
        code(INVALID.getCode(),()->service.saveRelation(1L,request(8L,0)));verifyNoInteractions(audit);
    }
    @Test void selfAndImplicitClearOrMalformedOperationAreRejected() {
        code(INVALID.getCode(),()->service.saveRelation(1L,request(7L,0)));
        var implicit=request(null,0);implicit.setOperation("SET");code(INVALID.getCode(),()->service.saveRelation(1L,implicit));
        var malformed=request(8L,0);malformed.setOperation("CLEAR");code(INVALID.getCode(),()->service.saveRelation(1L,malformed));
        var emptyReason=request(8L,0);emptyReason.setChangeReason(" ");code(INVALID.getCode(),()->service.saveRelation(1L,emptyReason));unchanged();
    }
    @Test void invalidAndOverflowVersionsCannotWriteOrGuessVersionZero() {
        var missing=request(8L,0);missing.setExpectedRelationVersion(null);code(VERSION_INVALID.getCode(),()->service.saveRelation(1L,missing));
        code(VERSION_INVALID.getCode(),()->service.saveRelation(1L,request(8L,-1)));unchanged();
        jdbc.update("UPDATE system_users SET direct_supervisor_revision=? WHERE id=7",Long.MAX_VALUE);
        code(VERSION_INVALID.getCode(),()->service.saveRelation(1L,request(8L,Long.MAX_VALUE)));verifyNoInteractions(audit);
    }
    @Test void auditFailureAfterRealCasRollsBackBothFields() {
        var failure=new IllegalStateException("实际审计失败");doThrow(failure).when(audit).append(any());
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.saveRelation(1L,request(8L,0))));unchanged();
        verify(users).updateDirectSupervisorRelation(1L,7L,8L,0L,1L,1L);verify(audit).append(any());
    }
    @Test void realAuthenticatedActorMustMatchPassedIdTenantAndAdminTypeEvenForNoop() {
        service.saveRelation(1L,request(8L,0));clearInvocations(audit,users);
        for(String invalid:List.of("id","tenant","type","missing")) {
            login(1L,1L,2);
            switch(invalid) {
                case "id" -> login(9L,1L,2);
                case "tenant" -> login(1L,2L,2);
                case "type" -> login(1L,1L,1);
                case "missing" -> SecurityContextHolder.clearContext();
            }
            code(INVALID.getCode(),()->service.saveRelation(1L,request(8L,1)));
            code(INVALID.getCode(),()->service.saveRelation(1L,request(9L,1)));
            code(INVALID.getCode(),()->service.getRelation(1L,7L));
        }
        verifyNoInteractions(audit);verify(users,never()).updateDirectSupervisorRelation(any(),any(),any(),any(),any(),any());
        assertEquals(1L,users.selectById(7L).getDirectSupervisorRevision());
    }
    @Test void ordinaryUpdateByIdCannotResetIndependentRelationFields() {
        service.saveRelation(1L,request(8L,0));
        var change=AdminUserDO.builder().id(7L).nickname("更新后的昵称").directSupervisorUserId(9L).directSupervisorRevision(0L).build();
        users.updateById(change);var saved=users.selectById(7L);assertEquals("更新后的昵称",saved.getNickname());assertEquals(8L,saved.getDirectSupervisorUserId());assertEquals(1L,saved.getDirectSupervisorRevision());
    }
    @Test void sourceRejectsStaleDisabledSupervisorButMaintenanceCanReadAndExplicitlyClearIt() {
        service.saveRelation(1L,request(8L,0));jdbc.update("UPDATE system_users SET status=1 WHERE id=8");
        code(INVALID.getCode(),()->service.getDirectSupervisorRelation(7L));
        var maintenance=service.getRelation(1L,7L);assertEquals(8L,maintenance.getSupervisorUserId());assertEquals(1,maintenance.getSupervisorStatus());
        assertNull(service.saveRelation(1L,request(null,1)).getSupervisorUserId());
    }
    @Test void twoRealConcurrentOldVersionWritesHaveOneWinnerAndOneConflict() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        java.util.function.Function<Long,Future<Object>> submit=supervisor->pool.submit(()->{
            TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);ready.countDown();
            login(1L,1L,2);
            try { if(!go.await(10,TimeUnit.SECONDS))throw new IllegalStateException("并发测试门禁超时");
                try{return service.saveRelation(1L,request(supervisor,0));}catch(ServiceException e){return e;}
            }finally{TenantContextHolder.clear();SecurityContextHolder.clearContext();}
        });
        try { var a=submit.apply(8L);var b=submit.apply(9L);assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();
            var results=List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertEquals(1L,results.stream().filter(ServiceException.class::isInstance).count());
            assertEquals(CONFLICT.getCode(),results.stream().filter(ServiceException.class::isInstance).map(ServiceException.class::cast).findFirst().orElseThrow().getCode());
            assertEquals(1L,users.selectById(7L).getDirectSupervisorRevision());verify(audit).append(any());
        }finally{go.countDown();pool.shutdownNow();}
    }
    @Test void typedSourceRejectsDisabledTargetSelfAndForeignSupervisorWithoutFallback() {
        jdbc.update("UPDATE system_users SET status=1 WHERE id=7");code(INVALID.getCode(),()->service.getDirectSupervisorRelation(7L));
        jdbc.update("UPDATE system_users SET status=0,direct_supervisor_user_id=7 WHERE id=7");code(INVALID.getCode(),()->service.getDirectSupervisorRelation(7L));
        jdbc.update("UPDATE system_users SET direct_supervisor_user_id=8 WHERE id=7");jdbc.update("UPDATE system_users SET tenant_id=2 WHERE id=8");
        code(INVALID.getCode(),()->service.getDirectSupervisorRelation(7L));verifyNoInteractions(audit);
    }
    @Test void zeroAffectedCasDoesNotAppendSuccessOrModifyRelation() {
        doReturn(0).when(users).updateDirectSupervisorRelation(1L,7L,8L,0L,1L,1L);
        code(CONFLICT.getCode(),()->service.saveRelation(1L,request(8L,0)));unchanged();verifyNoInteractions(audit);
    }
    @Test void controllerUsesOnlyIndependentResourceAndJsonLongVersionIsExact() throws Exception {
        var controller=new cn.iocoder.yudao.module.system.controller.admin.user.UserController();
        ReflectionTestUtils.setField(controller,"directSupervisorService",service);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("ic32",mock(cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi.class))).build();
        jdbc.update("UPDATE system_users SET direct_supervisor_revision=? WHERE id=7",9007199254740993L);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/system/user/direct-supervisor").param("userId","7"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.relationVersion").value("9007199254740993"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.userId").value("7"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/system/user/direct-supervisor").contentType("application/json")
            .content("{\"userId\":\"7\",\"supervisorUserId\":\"8\",\"operation\":\"SET\",\"expectedRelationVersion\":\"9007199254740993\",\"changeReason\":\"人员配置调整\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.relationVersion").value("9007199254740994"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/system/user/direct-supervisor").contentType("application/json")
            .content("{\"userId\":7,\"supervisorUserId\":9,\"operation\":\"SET\",\"expectedRelationVersion\":\"9007199254740993\",\"changeReason\":\"旧关系请求\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(CONFLICT.getCode()));
        verify(audit).append(any());
        assertEquals("@ss.hasPermission('system:user:query')",controller.getClass().getMethod("getDirectSupervisor",Long.class).getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('system:user:update')",controller.getClass().getMethod("saveDirectSupervisor",UserDirectSupervisorSaveReqVO.class).getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value());
    }
    @Test void controllerPermissionAndInvalidRequestReturnFailureWithNoRelationWrite() throws Exception {
        var controller=new cn.iocoder.yudao.module.system.controller.admin.user.UserController();ReflectionTestUtils.setField(controller,"directSupervisorService",service);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("ic32",mock(cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi.class))).build();
        when(permissions.hasAnyPermissions(1L,"system:user:update")).thenReturn(false);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/system/user/direct-supervisor").contentType("application/json")
            .content("{\"userId\":7,\"supervisorUserId\":8,\"operation\":\"SET\",\"expectedRelationVersion\":0,\"changeReason\":\"无权限\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(FORBIDDEN.getCode()));
        when(permissions.hasAnyPermissions(1L,"system:user:update")).thenReturn(true);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/system/user/direct-supervisor").contentType("application/json")
            .content("{\"userId\":7,\"supervisorUserId\":null,\"operation\":\"SET\",\"expectedRelationVersion\":0,\"changeReason\":\"非明确清空\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(INVALID.getCode()));
        unchanged();verifyNoInteractions(audit);
    }
}
