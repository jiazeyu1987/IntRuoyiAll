package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerRoleService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectReviewerRoleServiceTest {
    static final AtomicInteger IDS=new AtomicInteger();
    DccProjectReviewerRoleService service;JdbcTemplate jdbc;PermissionApi permissions;TransactionTemplate tx;
    @BeforeEach void setup(){
        TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);login(7L);
        var source=new JdbcDataSource();source.setURL("jdbc:h2:mem:project_role_"+IDS.incrementAndGet()+";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE system_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR(64),name VARCHAR(64),status INT,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(64),nickname VARCHAR(64),status INT,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE system_user_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,user_id BIGINT,role_id BIGINT,deleted INT DEFAULT 0)");
        jdbc.update("INSERT INTO system_role VALUES(10,1,'dcc_project_reviewer','项目审核员',0,0)");
        jdbc.update("INSERT INTO system_users VALUES(7,1,'a','审核甲',0,0),(8,1,'b','审核乙',0,0),(9,1,'applicant','申请人',0,0),(1,1,'admin','管理员',0,0)");
        jdbc.update("INSERT INTO system_user_role VALUES(70,1,7,10,0),(80,1,8,10,0)");
        permissions=mock(PermissionApi.class);for(long id:new long[]{1,7,8,9})when(permissions.hasAnyPermissions(id,"dcc:project-code:query")).thenReturn(true);
        for(long id:new long[]{7,8})when(permissions.hasAnyPermissions(id,"dcc:project-code:review")).thenReturn(true);
        service=new DccProjectReviewerRoleService();
        ReflectionTestUtils.setField(service,"dataSource",source);
        ReflectionTestUtils.setField(service,"permissions",permissions);
    }
    @AfterEach void clear(){TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    void login(Long id){SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(id).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());}
    DccProjectProductCreateRequestDO request(){var row=new DccProjectProductCreateRequestDO();row.setTenantId(1L);row.setReviewContract(DccProjectReviewerRoleService.CONTRACT);row.setReviewerRoleId(10L);row.setReviewerRoleCode("dcc_project_reviewer");return row;}
    @Test void allCurrentMembersAreCandidatesAndEitherMayReviewWithoutUpdatePermission(){
        var view=service.freezeRole();assertEquals(List.of(7L,8L),view.candidates().stream().map(DccProjectReviewerRoleService.Member::userId).toList());
        for(long actor:new long[]{7,8}){login(actor);assertDoesNotThrow(()->tx.execute(s->{service.assertReviewer(actor,request());return null;}));}
        verify(permissions,never()).hasAnyPermissions(anyLong(),eq("dcc:project-code:update"));
    }
    @Test void newApplicantNeedsOnlyQueryReadinessNotReviewerIdentity(){login(9L);var view=service.readiness(9L);assertFalse(view.canReview());assertEquals(2,view.candidates().size());}
    @Test void adminEvenWithAllMenusCannotBypassActualRoleMembership(){when(permissions.hasAnyPermissions(1L,"dcc:project-code:review")).thenReturn(true);login(1L);assertFalse(service.canReview(1L,request()));assertThrows(RuntimeException.class,()->tx.execute(s->{service.assertReviewer(1L,request());return null;}));}
    @Test void membershipRevocationAndNewAssignmentUseLiveRowsNotSubmitTimeFirstMember(){
        assertTrue(service.canReview(7L,request()));jdbc.update("DELETE FROM system_user_role WHERE user_id=7");assertFalse(service.canReview(7L,request()));
        jdbc.update("INSERT INTO system_user_role VALUES(90,1,9,10,0)");when(permissions.hasAnyPermissions(9L,"dcc:project-code:review")).thenReturn(true);login(9L);assertTrue(service.canReview(9L,request()));
    }
    @Test void disabledOrForeignTenantRoleUserAndMembershipNeverAuthorize(){
        jdbc.update("UPDATE system_role SET status=1 WHERE id=10");assertThrows(RuntimeException.class,()->service.freezeRole());assertFalse(service.canReview(7L,request()));
        jdbc.update("UPDATE system_role SET status=0,tenant_id=2 WHERE id=10");assertThrows(RuntimeException.class,()->service.freezeRole());
        jdbc.update("UPDATE system_role SET tenant_id=1 WHERE id=10");jdbc.update("UPDATE system_users SET tenant_id=2 WHERE id=7");assertFalse(service.canReview(7L,request()));
        jdbc.update("UPDATE system_users SET tenant_id=1,status=1 WHERE id=7");assertFalse(service.canReview(7L,request()));
        jdbc.update("UPDATE system_users SET status=0 WHERE id=7");jdbc.update("UPDATE system_user_role SET tenant_id=2 WHERE user_id=7");assertFalse(service.canReview(7L,request()));
    }
    @Test void missingDuplicateOrEmptyRoleClearlyRejectsInsteadOfOldConfiguredUserFallback(){
        jdbc.update("DELETE FROM system_user_role");assertThrows(RuntimeException.class,()->service.freezeRole());
        jdbc.update("DELETE FROM system_role");assertThrows(RuntimeException.class,()->service.freezeRole());
        jdbc.update("INSERT INTO system_role VALUES(10,1,'dcc_project_reviewer','项目审核员',0,0),(11,1,'dcc_project_reviewer','重复',0,0)");assertThrows(RuntimeException.class,()->service.freezeRole());
    }
    @Test void frozenRoleIdentityAndUnknownContractAreNotGuessed(){var row=request();row.setReviewerRoleId(11L);assertFalse(service.canReview(7L,row));row.setReviewContract("UNKNOWN");assertThrows(RuntimeException.class,()->service.isRoleContract(row));row.setReviewContract(null);row.setReviewerRoleId(null);row.setReviewerRoleCode(null);assertFalse(service.isRoleContract(row));}
    @Test void partialNewContractCannotFallIntoLegacyOrMissingRoleIdentity(){
        var noMode=request();noMode.setReviewContract(null);assertThrows(RuntimeException.class,()->service.isRoleContract(noMode));
        var noId=request();noId.setReviewerRoleId(null);assertThrows(RuntimeException.class,()->service.isRoleContract(noId));
        var zeroId=request();zeroId.setReviewerRoleId(0L);assertThrows(RuntimeException.class,()->service.isRoleContract(zeroId));
        var wrongCode=request();wrongCode.setReviewerRoleCode("other");assertThrows(RuntimeException.class,()->service.isRoleContract(wrongCode));
        var noCode=request();noCode.setReviewerRoleCode(null);assertThrows(RuntimeException.class,()->service.isRoleContract(noCode));
    }
    @Test void reviewMenuRevocationAndWrongAuthenticatedActorRejectImmediately(){when(permissions.hasAnyPermissions(7L,"dcc:project-code:review")).thenReturn(false);assertFalse(service.canReview(7L,request()));login(8L);assertThrows(RuntimeException.class,()->tx.execute(s->{service.assertReviewer(7L,request());return null;}));}
}
