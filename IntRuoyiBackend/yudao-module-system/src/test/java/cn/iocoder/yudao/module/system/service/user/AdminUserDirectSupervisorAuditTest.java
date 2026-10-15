package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDirectSupervisorSaveReqVO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({AdminUserDirectSupervisorService.class,GxpAuditServiceImpl.class,AdminUserDirectSupervisorServiceTest.Beans.class})
@TestPropertySource(properties="spring.sql.init.encoding=UTF-8")
class AdminUserDirectSupervisorAuditTest extends BaseDbUnitTest {
    @Resource AdminUserDirectSupervisorService service;
    @Resource JdbcTemplate jdbc;
    @Resource AdminUserMapper users;
    @Resource GxpAuditPolicyOperationMapper policies;
    @Resource GxpAuditEventMapper events;
    @Resource GxpAuditLedgerSequenceMapper sequences;
    @MockitoSpyBean GxpAuditServiceImpl ledger;
    @MockitoBean PermissionApi permissions;
    @MockitoBean AdminUserApi accountApi;
    @BeforeEach void setup() {
        for(long id:new long[]{1,7,8,9})jdbc.update("INSERT INTO system_users(id,tenant_id,username,canonical_username,nickname,status) VALUES(?,1,?,?,?,0)",id,"account"+id,"account"+id,"账号"+id);
        when(permissions.hasAnyPermissions(1L,"system:user:update")).thenReturn(true);
        when(accountApi.getUser(1L)).thenReturn(new AdminUserRespDTO().setId(1L).setTenantId(1L).setStatus(0).setUsername("account1").setNickname("账号1"));
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(1L).setTenantId(1L).setUserType(2).setInfo(Map.of("username","account1","nickname","账号1")),new org.springframework.mock.web.MockHttpServletRequest());
        var policy=new GxpAuditPolicyOperationDO();policy.setTenantId(1L);policy.setOperationId("system.user.direct-supervisor.update");policy.setPolicyVersion("IC32-TEST-NOT-DEPLOYMENT-APPROVAL");
        policy.setSourceType("SERVICE_METHOD");policy.setSourceLocator("cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorService#saveRelation");policy.setDomain("SYSTEM");policy.setSubjectType("SYSTEM_USER_DIRECT_SUPERVISOR");policy.setActionType("UPDATE");
        policy.setReasonPolicy("REQUIRED_CATEGORY_AND_TEXT");policy.setSignaturePolicy("NOT_REQUIRED");policy.setStatePolicy("PRESENT_TO_PRESENT");policy.setRetentionClass("GXP_PERMISSION_CONFIG");policy.setTestIds("IC32-W01-B01,IC32-W01-B02,IC32-W01-B06");policy.setOwner("system-user-owner");policy.setApplicability("GXP");policy.setActive(true);policies.insert(policy);
    }
    @AfterEach void clearSecurity() {
        sequences.deleteById(1L); // Existing system clean does not reset the ledger-sequence sidecar.
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    UserDirectSupervisorSaveReqVO request(Long selected,long version) {
        var req=new UserDirectSupervisorSaveReqVO();req.setUserId(7L);req.setSupervisorUserId(selected);req.setOperation(selected==null?"CLEAR":"SET");req.setExpectedRelationVersion(version);req.setChangeReason("人员配置调整：确认实际直属主管");return req;
    }
    @Test void actualLedgerContainsRealBeforeAfterVersionsAndSameValueAddsNoEvent() {
        service.saveRelation(1L,request(8L,0));var first=events.selectList().get(0);
        assertEquals("system.user.direct-supervisor.update",first.getOperationId());assertEquals("SYSTEM_USER_DIRECT_SUPERVISOR",first.getSubjectType());assertEquals("UPDATE",first.getAction());
        assertEquals(1L,first.getActorId());assertEquals("account1",first.getActorUsername());assertEquals("PRESENT",first.getBeforeState());assertEquals("PRESENT",first.getAfterState());
        var before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(first.getBeforeStateJson());
        var after=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(first.getAfterStateJson());
        assertEquals(0L,before.get("relationVersion").asLong());assertEquals(1L,after.get("relationVersion").asLong());assertTrue(before.get("supervisorUserId").isNull());assertEquals(8L,after.get("supervisorUserId").asLong());
        String eventBefore=first.getCanonicalEventJson();service.saveRelation(1L,request(8L,1));assertEquals(1,events.selectList().size());assertEquals(eventBefore,events.selectById(first.getId()).getCanonicalEventJson());
        service.saveRelation(1L,request(null,1));assertEquals(2,events.selectList().size());assertEquals(2L,users.selectById(7L).getDirectSupervisorRevision());assertNull(users.selectById(7L).getDirectSupervisorUserId());
    }
    @Test void missingRealPolicyFailsAfterCasAndRollsBackAllRelationFacts() {
        policies.deleteById(policies.selectList().get(0).getId());
        var error=assertThrows(ServiceException.class,()->service.saveRelation(1L,request(8L,0)));
        assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode(),error.getCode());
        assertEquals(0L,users.selectById(7L).getDirectSupervisorRevision());assertNull(users.selectById(7L).getDirectSupervisorUserId());assertTrue(events.selectList().isEmpty());assertTrue(sequences.selectList().isEmpty());
    }
    @Test void lateActualLedgerAppendFailureRollsBackRelationAndLedgerRowsThenRetryRemainsValid() {
        doAnswer(invocation->{invocation.callRealMethod();throw new IllegalStateException("实际账本insert后故障");}).when(ledger).append(any());
        assertThrows(IllegalStateException.class,()->service.saveRelation(1L,request(8L,0)));
        assertEquals(0L,users.selectById(7L).getDirectSupervisorRevision());assertNull(users.selectById(7L).getDirectSupervisorUserId());assertTrue(events.selectList().isEmpty());assertTrue(sequences.selectList().isEmpty());
        reset(ledger);assertEquals(1L,service.saveRelation(1L,request(8L,0)).getRelationVersion());assertEquals(1,events.selectList().size());
    }
}
