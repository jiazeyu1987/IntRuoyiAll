package cn.iocoder.yudao.module.system.controller.admin.oauth2;

import cn.iocoder.yudao.module.system.controller.admin.oauth2.vo.user.OAuth2UserUpdateReqVO;
import cn.iocoder.yudao.module.system.service.user.ProfileUpdateHttpContractSupport;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;

public class OAuth2UserProfileUpdateContractTest extends ProfileUpdateHttpContractSupport {
    @Override protected Object controller() { return new OAuth2UserController(); }
    @Override protected String endpoint() { return "/system/oauth2/user/update"; }

    @Test
    void originalUserWriteScopeDeclarationIsUnchanged() throws Exception {
        var method = OAuth2UserController.class.getMethod("updateUserInfo", OAuth2UserUpdateReqVO.class);
        assertEquals("@ss.hasScope('user.write')", method.getAnnotation(PreAuthorize.class).value());
        // Annotation inspection is not evidence that standalone MockMvc enforces method security.
    }
}
