package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/** Real HTTP JSON/@Valid -> controller method -> service -> H2 mapper.
 * Standalone MockMvc does not install OAuth2 method-security proxies or production filters.
 */
@Import(AdminUserServiceImpl.class)
// Each HTTP contract class owns its context. Do not leave this factory cached for
// another test after a different context has rebound MyBatis Plus entity metadata.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class ProfileUpdateHttpContractSupport extends BaseDbUnitTest {
    @Resource protected AdminUserServiceImpl userService;
    @Resource protected AdminUserMapper userMapper;
    @MockitoBean protected DeptService deptService;
    @MockitoBean protected PostService postService;
    @MockitoBean protected PermissionService permissionService;
    @MockitoBean protected PasswordEncoder passwordEncoder;
    @MockitoBean protected TenantService tenantService;
    @MockitoBean protected FileApi fileApi;
    @MockitoBean protected ConfigApi configApi;
    @MockitoBean protected OAuth2TokenService oauth2TokenService;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    protected AdminUserDO original;
    protected abstract Object controller();
    protected abstract String endpoint();

    @BeforeEach
    void prepareHttp() {
        original = seed("profile-contract", "old@example.test", "13800138000");
        LoginUser login = new LoginUser();
        login.setId(original.getId());
        login.setTenantId(1L);
        login.setUserType(UserTypeEnum.ADMIN.getValue());
        login.setScopes(List.of("user.write"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null));
        Object controller = controller();
        ReflectionTestUtils.setField(controller, "userService", userService);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
    }
    @AfterEach
    void releaseHttp() {
        SecurityContextHolder.clearContext();
        if (validator != null) validator.close();
    }
    protected AdminUserDO seed(String name, String email, String mobile) {
        AdminUserDO user = new AdminUserDO();
        user.setUsername(name);
        user.setCanonicalUsername(name);
        user.setNickname("original");
        user.setPassword("unchanged-synthetic-hash");
        user.setPasswordCredentialStatus("ACTIVE");
        user.setEmail(email);
        user.setMobile(mobile);
        user.setSex(1);
        user.setAvatar("https://example.test/old.png");
        user.setStatus(0);
        user.setDeptId(99L);
        user.setPostIds(Set.of(77L));
        user.setTenantId(1L);
        user.setUpdateTime(LocalDateTime.of(2000, 1, 1, 0, 0));
        user.setUpdater("previous-operator");
        userMapper.insert(user);
        return userMapper.selectById(user.getId());
    }
    protected void request(String json) throws Exception {
        mvc.perform(put(endpoint()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
    }
    private AdminUserDO actual() { return userMapper.selectById(original.getId()); }
    private void assertProtectedFields() {
        AdminUserDO actual = actual();
        assertEquals(original.getUsername(), actual.getUsername());
        assertEquals(original.getCanonicalUsername(), actual.getCanonicalUsername());
        assertEquals(original.getPassword(), actual.getPassword());
        assertEquals(original.getPasswordCredentialStatus(), actual.getPasswordCredentialStatus());
        assertEquals(original.getDeptId(), actual.getDeptId());
        assertEquals(original.getPostIds(), actual.getPostIds());
        assertEquals(original.getTenantId(), actual.getTenantId());
        assertEquals(original.getStatus(), actual.getStatus());
        assertEquals(original.getCreateTime(), actual.getCreateTime());
    }
    private void assertUpdatedMetadata() {
        assertTrue(actual().getUpdateTime().isAfter(original.getUpdateTime()));
        assertEquals(original.getId().toString(), actual().getUpdater());
        assertProtectedFields();
    }
    @Test
    void missingContactBaselinesPermitNicknameOnlyAndRemainUnchanged() throws Exception {
        for (String mobile : new String[]{null, "", "           ", "13800138000"}) {
            for (String email : new String[]{null, "", " ", "old@example.test"}) {
                userMapper.update(new AdminUserDO(), Wrappers.<AdminUserDO>lambdaUpdate()
                        .eq(AdminUserDO::getId, original.getId())
                        .set(AdminUserDO::getMobile, mobile).set(AdminUserDO::getEmail, email));
                request("{\"nickname\":\"nickname-only\"}");
                assertEquals("nickname-only", actual().getNickname());
                assertEquals(mobile, actual().getMobile());
                assertEquals(email, actual().getEmail());
                assertProtectedFields();
            }
        }
    }
    @Test
    void missingAndExplicitNullContactsPreservePersistedValues() throws Exception {
        request("{\"nickname\":\"first\"}");
        assertEquals("first", actual().getNickname());
        assertEquals(original.getEmail(), actual().getEmail());
        assertEquals(original.getMobile(), actual().getMobile());
        request("{\"nickname\":\"second\",\"email\":null,\"mobile\":null,\"sex\":null,\"avatar\":null}");
        assertEquals("second", actual().getNickname());
        assertEquals(original.getEmail(), actual().getEmail());
        assertEquals(original.getMobile(), actual().getMobile());
        assertEquals(original.getSex(), actual().getSex());
        assertEquals(original.getAvatar(), actual().getAvatar());
        assertUpdatedMetadata();
    }
    @Test
    void absentOrNullAllFieldsDoesNotGenerateInvalidSql() throws Exception {
        request("{}");
        request("{\"nickname\":null,\"email\":null,\"mobile\":null,\"sex\":null,\"avatar\":null}");
        assertEquals(original.getNickname(), actual().getNickname());
        assertEquals(original.getEmail(), actual().getEmail());
        assertEquals(original.getMobile(), actual().getMobile());
        assertUpdatedMetadata();
    }
    @Test
    void validContactsAndOriginalBlankBoundariesWriteExactlyAsProvided() throws Exception {
        request("{\"email\":\"new@example.test\",\"mobile\":\"13900139000\",\"sex\":2}");
        assertEquals("new@example.test", actual().getEmail());
        assertEquals("13900139000", actual().getMobile());
        assertEquals(2, actual().getSex());
        request("{\"email\":\"\",\"mobile\":\"abcdefghijk\"}");
        assertEquals("", actual().getEmail());
        assertEquals("abcdefghijk", actual().getMobile());
        // Another blank mobile proves existing blank unique bypass, rather than a new regex/NotBlank policy.
        seed("blank-contact-other", "other@example.test", "           ");
        request("{\"mobile\":\"           \"}");
        assertEquals("           ", actual().getMobile());
        assertUpdatedMetadata();
    }
    @Test
    void realHttpValidationRejectsInvalidLengthsAndEmailsWithoutWrites() throws Exception {
        for (String json : List.of(
                "{\"mobile\":\"\"}", "{\"mobile\":\"1380013800\"}", "{\"mobile\":\"138001380000\"}",
                "{\"email\":\"bad\"}", "{\"email\":\" \"}", "{\"email\":\"a@\"}",
                "{\"email\":\"" + "a".repeat(40) + "@example.test\"}")) {
            mvc.perform(put(endpoint()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
            assertEquals(original.getEmail(), actual().getEmail());
            assertEquals(original.getMobile(), actual().getMobile());
            assertEquals(original.getUpdateTime(), actual().getUpdateTime());
        }
    }
    @Test
    void uniqueConflictsRejectWithoutContactOrNicknameWrites() {
        seed("profile-conflict", "used@example.test", "13900139000");
        for (String json : List.of("{\"nickname\":\"must-not-save\",\"email\":\"used@example.test\"}",
                "{\"nickname\":\"must-not-save\",\"mobile\":\"13900139000\"}")) {
            Exception exception = assertThrows(Exception.class, () -> request(json));
            Throwable root = exception;
            while (root.getCause() != null) root = root.getCause();
            assertInstanceOf(ServiceException.class, root);
            assertEquals(json.contains("email") ? USER_EMAIL_EXISTS.getCode() : USER_MOBILE_EXISTS.getCode(), ((ServiceException) root).getCode());
            assertEquals(original.getNickname(), actual().getNickname());
            assertEquals(original.getEmail(), actual().getEmail());
            assertEquals(original.getMobile(), actual().getMobile());
            assertEquals(original.getUpdateTime(), actual().getUpdateTime());
        }
    }
}
