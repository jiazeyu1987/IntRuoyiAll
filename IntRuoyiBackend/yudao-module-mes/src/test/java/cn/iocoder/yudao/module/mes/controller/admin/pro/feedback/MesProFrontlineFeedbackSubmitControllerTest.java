package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackSubmitService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProFrontlineFeedbackSubmitControllerTest {

    @Test
    void shouldExposeOneShotFrontlineSubmitContract() throws Exception {
        Method method = MesProFeedbackController.class.getMethod(
                "frontlineSubmit", MesProFrontlineFeedbackSubmitReqVO.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertArrayEquals(new String[]{"/frontline/submit"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-feedback:create')", preAuthorize.value());

        Parameter body = method.getParameters()[0];
        assertNotNull(body.getAnnotation(RequestBody.class));

        assertEquals(CommonResult.class, method.getReturnType());
        assertTrue(method.getGenericReturnType() instanceof ParameterizedType);

        Field serviceField = MesProFeedbackController.class.getDeclaredField("frontlineFeedbackSubmitService");
        assertEquals(MesProFrontlineFeedbackSubmitService.class, serviceField.getType());
        assertNotNull(serviceField.getAnnotation(Resource.class));
    }

    @Test
    void parameterIdentityAnomaliesMustReachNonBlockingAuditAndResponseContract() throws Exception {
        Field readingDeviceId = MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO.class
                .getDeclaredField("deviceId");
        Field parameterCode = MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO.class
                .getDeclaredField("parameterCode");
        Field selectedDeviceId = MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO.class
                .getDeclaredField("deviceId");
        assertNull(readingDeviceId.getAnnotation(NotNull.class),
                "missing device identity is an UNRESOLVED audit reason, not a Bean Validation blocker");
        assertNull(parameterCode.getAnnotation(NotNull.class),
                "missing parameter code is an UNRESOLVED audit reason, not a Bean Validation blocker");
        assertNull(selectedDeviceId.getAnnotation(NotNull.class),
                "missing selected-device identity is an UNRESOLVED audit reason, not a Bean Validation blocker");

        java.util.Set<String> responseFields = java.util.Arrays.stream(
                        MesProFrontlineFeedbackSubmitRespVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(responseFields.contains("parameterAuditStatus"));
        assertTrue(responseFields.contains("parameterAuditTotalCount"));
        assertTrue(responseFields.contains("parameterAuditResolvedCount"));
        assertTrue(responseFields.contains("parameterAuditUnresolvedCount"));
        assertTrue(responseFields.contains("auditItems"));
    }
}
