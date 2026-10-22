package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.iocoder.yudao.module.system.service.user.ProfileUpdateHttpContractSupport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserProfileControllerContractTest extends ProfileUpdateHttpContractSupport {
    @Override protected Object controller() { return new UserProfileController(); }
    @Override protected String endpoint() { return "/system/user/profile/update"; }

    @Test
    void avatarOnlyUpdatesProvidedField() throws Exception {
        request("{\"avatar\":\"https://example.test/new.png\"}");
        var actual = userMapper.selectById(original.getId());
        assertEquals("https://example.test/new.png", actual.getAvatar());
        assertEquals(original.getNickname(), actual.getNickname());
        assertEquals(original.getEmail(), actual.getEmail());
        assertEquals(original.getMobile(), actual.getMobile());
    }
}
