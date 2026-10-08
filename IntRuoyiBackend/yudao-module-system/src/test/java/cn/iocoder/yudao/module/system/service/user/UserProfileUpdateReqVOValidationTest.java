package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.oauth2.vo.user.OAuth2UserUpdateReqVO;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserProfileUpdateReqVOValidationTest {
    @Test
    void bothVoRetainOptionalAndOriginalBlankContracts() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String mobile : new String[]{null, "13800138000", "abcdefghijk", "           "}) {
                for (String email : new String[]{null, "", "valid@example.test"}) {
                    var profile = new UserProfileUpdateReqVO();
                    profile.setMobile(mobile);
                    profile.setEmail(email);
                    var oauth = new OAuth2UserUpdateReqVO(null, email, mobile, null);
                    assertTrue(validator.validate(profile).isEmpty(), "profile original accepted boundary");
                    assertTrue(validator.validate(oauth).isEmpty(), "oauth original accepted boundary");
                }
            }
            for (String mobile : new String[]{"", "1380013800", "138001380000"}) {
                var profile = new UserProfileUpdateReqVO();
                profile.setMobile(mobile);
                assertFalse(validator.validate(profile).isEmpty());
                assertFalse(validator.validate(new OAuth2UserUpdateReqVO(null, null, mobile, null)).isEmpty());
            }
            for (String email : new String[]{" ", "bad", "a@", "a".repeat(40) + "@example.test"}) {
                var profile = new UserProfileUpdateReqVO();
                profile.setEmail(email);
                assertFalse(validator.validate(profile).isEmpty());
                assertFalse(validator.validate(new OAuth2UserUpdateReqVO(null, email, null, null)).isEmpty());
            }
        }
    }
}
