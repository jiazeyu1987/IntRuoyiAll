package cn.iocoder.yudao.module.system.convert.auth;

import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthSocialLoginReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuthConvertTest {

    @Test
    public void testConvertSocialLogin_mapsSocialType() {
        AuthSocialLoginReqVO reqVO = AuthSocialLoginReqVO.builder()
                .type(10)
                .code("code")
                .state("state")
                .build();

        SocialUserBindReqDTO result = AuthConvert.INSTANCE.convert(11L, 22, reqVO);

        assertEquals(11L, result.getUserId());
        assertEquals(22, result.getUserType());
        assertEquals(10, result.getSocialType());
        assertEquals("code", result.getCode());
        assertEquals("state", result.getState());
    }

    @Test
    public void testConvertSmsSend_leavesCreateIpForServiceLayer() {
        AuthSmsSendReqVO reqVO = AuthSmsSendReqVO.builder()
                .mobile("13800138000")
                .scene(1)
                .build();

        SmsCodeSendReqDTO result = AuthConvert.INSTANCE.convert(reqVO);

        assertEquals("13800138000", result.getMobile());
        assertEquals(1, result.getScene());
        assertNull(result.getCreateIp());
    }

    @Test
    public void testConvertPermissionInfo_addsWildcardForSuperAdminRole() {
        AdminUserDO user = new AdminUserDO().setId(1L).setUsername("admin");
        RoleDO role = new RoleDO().setId(1L).setCode(RoleCodeEnum.SUPER_ADMIN.getCode());
        MenuDO menu = new MenuDO()
                .setId(10L)
                .setParentId(0L)
                .setName("系统管理")
                .setPath("/system")
                .setType(MenuTypeEnum.DIR.getType());

        AuthPermissionInfoRespVO result = AuthConvert.INSTANCE.convert(user, List.of(role), List.of(menu));

        assertTrue(result.getPermissions().contains("*:*:*"));
    }

}
