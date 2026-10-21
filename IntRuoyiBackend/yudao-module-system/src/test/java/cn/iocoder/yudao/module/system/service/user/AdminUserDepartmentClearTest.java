package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import cn.iocoder.yudao.module.system.framework.operatelog.core.DeptParseFunction;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mzt.logapi.beans.MethodExecuteResult;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DefaultFunctionServiceImpl;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.service.impl.ParseFunctionFactory;
import com.mzt.logapi.starter.configuration.LogRecordProperties;
import com.mzt.logapi.starter.diff.DefaultDiffItemsToLogContentService;
import com.mzt.logapi.starter.support.parse.LogFunctionParser;
import com.mzt.logapi.starter.support.parse.LogRecordValueParser;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.module.system.enums.LogRecordConstants.SYSTEM_USER_UPDATE_SUCCESS;

/**
 * Real H2 persistence tests for user department clearing.
 * The test API intentionally exists before the fix, so RED is an assertion failure, not missing code.
 */
@Import(AdminUserServiceImpl.class)
public class AdminUserDepartmentClearTest extends BaseDbUnitTest {

    @Resource private AdminUserServiceImpl userService;
    @Resource private AdminUserMapper userMapper;
    @Resource private UserPostMapper userPostMapper;
    @MockitoBean private DeptService deptService;
    @MockitoBean private PostService postService;
    @MockitoBean private PermissionService permissionService;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private TenantService tenantService;
    @MockitoBean private OAuth2TokenService oauth2TokenService;
    @MockitoBean private ConfigApi configApi;

    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void explicitNullClearsPersistedDepartmentWithoutChangingOtherFields() throws Exception {
        AdminUserDO original = fixture("deptClearActor", 71L);
        AdminUserDO other = fixture("deptOtherActor", 72L);
        UserSaveReqVO request = requestFor(original, "{\"deptId\":null}");

        userService.updateUser(request);

        AdminUserDO saved = userMapper.selectById(original.getId());
        assertNull(saved.getDeptId(), "Explicit deptId:null must clear the stored department");
        assertPojoEquals(original, saved, "deptId", "updateTime", "updater");
        assertEquals(Set.of(11L), userPostMapper.selectListByUserId(original.getId()).stream()
                .map(UserPostDO::getPostId).collect(java.util.stream.Collectors.toSet()));
        assertPojoEquals(other, userMapper.selectById(other.getId()));
    }

    @Test
    void omittedDepartmentPreservesPersistedDepartment() throws Exception {
        AdminUserDO original = fixture("deptOmittedActor", 73L);

        userService.updateUser(requestFor(original, "{}"));

        assertEquals(73L, userMapper.selectById(original.getId()).getDeptId());
        assertPojoEquals(original, userMapper.selectById(original.getId()), "updateTime", "updater");
    }

    @Test
    void clientCannotForgeInternalPresenceToClearAnOmittedDepartment() throws Exception {
        AdminUserDO original = fixture("deptPresenceActor", 74L);

        userService.updateUser(requestFor(original, "{\"deptIdSpecified\":true}"));

        assertEquals(74L, userMapper.selectById(original.getId()).getDeptId(),
                "A client-supplied internal tracking flag must not authorize clearing");
    }

    @Test
    void partialPasswordStatusAndLoginUpdatesPreserveDepartment() {
        AdminUserDO original = fixture("deptPartialActor", 75L);

        // Password writers use this partial-entity Mapper path; do not change the DO's global null strategy.
        userMapper.updateById(new AdminUserDO().setId(original.getId()).setPassword("changed-unit-credential"));
        assertEquals(75L, userMapper.selectById(original.getId()).getDeptId());
        userService.updateUserStatus(original.getId(), 1);
        assertEquals(75L, userMapper.selectById(original.getId()).getDeptId());
        userService.updateUserLogin(original.getId(), "127.0.0.2");
        assertEquals(75L, userMapper.selectById(original.getId()).getDeptId());
    }

    @Test
    void internalPresenceDoesNotLeakThroughSerializationOrBeanCopy() throws Exception {
        AdminUserDO original = fixture("deptSerializedActor", 76L);
        UserSaveReqVO request = requestFor(original, "{\"deptId\":null}");

        var serialized = json.readTree(json.writeValueAsString(request));
        assertTrue(serialized.has("deptId"));
        assertTrue(serialized.get("deptId").isNull());
        assertFalse(serialized.has("deptIdSpecified"));
        AdminUserDO copied = BeanUtils.toBean(request, AdminUserDO.class);
        assertNull(copied.getDeptId());
        assertEquals(original.getId(), copied.getId());
        assertEquals(original.getUsername(), copied.getUsername());
    }

    @Test
    void omittedDepartmentDoesNotProduceClearAuditDiff() throws Exception {
        AdminUserDO original = fixture("deptAuditActor", 77L);
        UserSaveReqVO request = requestFor(original, "{}");
        when(deptService.getDept(77L)).thenReturn(new DeptDO().setId(77L).setName("Existing audit department"));
        DeptParseFunction departmentParser = new DeptParseFunction();
        ReflectionTestUtils.setField(departmentParser, "deptService", deptService);
        var functions = new DefaultFunctionServiceImpl(new ParseFunctionFactory(List.of(departmentParser)));
        var content = new DefaultDiffItemsToLogContentService(new LogRecordProperties());
        content.setFunctionService(functions);
        var diff = new DiffParseFunction();
        diff.setDiffItemsToLogContentService(content);
        var parser = new LogRecordValueParser();
        parser.setBeanFactory(new DefaultListableBeanFactory());
        parser.setLogFunctionParser(new LogFunctionParser(functions));
        parser.setDiffParseFunction(diff);
        ReflectionTestUtils.setField(parser, "diffLog", true);

        LogRecordContext.putEmptySpan();
        try {
            userService.updateUser(request);
            assertEquals(77L, userMapper.selectById(original.getId()).getDeptId());
            var method = AdminUserServiceImpl.class.getMethod("updateUser", UserSaveReqVO.class);
            var execution = new MethodExecuteResult(method, new Object[]{request}, AdminUserServiceImpl.class);
            execution.setSuccess(true);
            String audit = parser.singleProcessTemplate(execution, SYSTEM_USER_UPDATE_SUCCESS, Map.of());

            // Positive calibration: the real diff pipeline must still detect an actual clear.
            UserSaveReqVO explicitClear = BeanUtils.toBean(request, UserSaveReqVO.class);
            explicitClear.setDeptId(null);
            assertTrue(diff.diff(LogRecordContext.getMethodOrGlobal(DiffParseFunction.OLD_OBJECT),
                    explicitClear).contains("部门"), "Real diff must detect explicit department clearing");
            assertTrue(audit.contains(original.getNickname()), "Production audit template must resolve the actor");
            assertFalse(audit.contains("_DIFF"), "Production audit template must actually execute the diff");
            assertFalse(audit.contains("【部门】"),
                    "Omitted deptId must not be rendered as a department clear: " + audit);
        } finally {
            LogRecordContext.clear();
        }
    }

    private AdminUserDO fixture(String username, Long department) {
        AdminUserDO user = new AdminUserDO()
                .setUsername(username)
                .setCanonicalUsername(username.toLowerCase(java.util.Locale.ROOT))
                .setNickname(username)
                .setPassword("preserved-unit-credential")
                .setPasswordCredentialStatus("ACTIVE")
                .setPasswordUpdateTime(LocalDateTime.of(2026, 10, 15, 8, 0))
                .setDeptId(department)
                .setDirectSupervisorUserId(424242L)
                .setDirectSupervisorRevision(7L)
                .setPostIds(Set.of(11L))
                .setEmail(username + "@example.test")
                .setMobile("")
                .setSex(0)
                .setStatus(0)
                .setRemark("department-clear-only");
        user.setTenantId(1L);
        userMapper.insert(user);
        userPostMapper.insert(new UserPostDO().setUserId(user.getId()).setPostId(11L));
        return userMapper.selectById(user.getId());
    }

    private UserSaveReqVO requestFor(AdminUserDO original, String departmentJson) throws Exception {
        UserSaveReqVO request = json.readValue(departmentJson, UserSaveReqVO.class);
        request.setId(original.getId());
        request.setUsername(original.getUsername());
        request.setNickname(original.getNickname());
        request.setEmail(original.getEmail());
        request.setMobile(original.getMobile());
        request.setSex(original.getSex());
        request.setRemark(original.getRemark());
        request.setPostIds(original.getPostIds());
        // Deliberately do not call setDeptId here: its JSON presence is the contract being tested.
        return request;
    }
}
