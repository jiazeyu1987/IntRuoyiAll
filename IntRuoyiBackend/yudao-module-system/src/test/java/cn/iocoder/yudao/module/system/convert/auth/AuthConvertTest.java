package cn.iocoder.yudao.module.system.convert.auth;

import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthSocialLoginReqVO;
import org.junit.jupiter.api.Test;

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

}
