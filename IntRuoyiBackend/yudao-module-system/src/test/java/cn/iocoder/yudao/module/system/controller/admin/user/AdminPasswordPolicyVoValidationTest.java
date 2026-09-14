package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthResetPasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdatePasswordReqVO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPasswordPolicyVoValidationTest {

    private static final String LONG_STRONG_PASSWORD = "YudaoPassword@20260526";
    private static final String LONG_LEGACY_LOGIN_PASSWORD = "legacyPasswordBeyond16";
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void authLogin_allowsLongExistingPasswordForBackendExpiryDecision() {
        AuthLoginReqVO reqVO = AuthLoginReqVO.builder()
                .username("codex2026")
                .password(LONG_LEGACY_LOGIN_PASSWORD)
                .build();

        assertNoPasswordViolation(reqVO, "password");
    }

    @Test
    void authRegister_allowsLongStrongPasswordForServicePolicy() {
        AuthRegisterReqVO reqVO = new AuthRegisterReqVO();
        reqVO.setUsername("codex2026");
        reqVO.setNickname("Codex 用户");
        reqVO.setPassword(LONG_STRONG_PASSWORD);

        assertNoPasswordViolation(reqVO, "password");
    }

    @Test
    void authResetPassword_allowsLongStrongPasswordForServicePolicy() {
        AuthResetPasswordReqVO reqVO = AuthResetPasswordReqVO.builder()
                .mobile("13800138000")
                .code("123456")
                .password(LONG_STRONG_PASSWORD)
                .build();

        assertNoPasswordViolation(reqVO, "password");
    }

    @Test
    void userSave_allowsLongStrongPasswordForServicePolicy() {
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername("codex2026");
        reqVO.setNickname("Codex 用户");
        reqVO.setPassword(LONG_STRONG_PASSWORD);

        assertNoPasswordViolation(reqVO, "password");
    }

    @Test
    void userResetPassword_allowsLongStrongPasswordForServicePolicy() {
        UserUpdatePasswordReqVO reqVO = new UserUpdatePasswordReqVO();
        reqVO.setId(1L);
        reqVO.setPassword(LONG_STRONG_PASSWORD);

        assertNoPasswordViolation(reqVO, "password");
    }

    @Test
    void profileUpdatePassword_allowsLongOldAndNewPasswords() {
        UserProfileUpdatePasswordReqVO reqVO = new UserProfileUpdatePasswordReqVO();
        reqVO.setOldPassword(LONG_LEGACY_LOGIN_PASSWORD);
        reqVO.setNewPassword(LONG_STRONG_PASSWORD);

        assertNoPasswordViolation(reqVO, "oldPassword");
        assertNoPasswordViolation(reqVO, "newPassword");
    }

    private static void assertNoPasswordViolation(Object reqVO, String propertyName) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(reqVO);
        assertTrue(violations.stream()
                        .filter(violation -> propertyName.equals(violation.getPropertyPath().toString()))
                        .noneMatch(violation -> violation.getMessage().contains("4-16")
                                || violation.getMessage().contains("长度")),
                () -> "password policy must not use the old 4-16 length message: " + violations);
    }
}
