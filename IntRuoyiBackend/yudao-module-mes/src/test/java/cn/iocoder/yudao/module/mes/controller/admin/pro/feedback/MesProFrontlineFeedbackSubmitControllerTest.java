package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesProFrontlineFeedbackSubmitControllerTest {

    @Test
    void frontlineSubmitContract_exposesSingleFeedbackEntry() throws Exception {
        RequestMapping requestMapping = MesProFeedbackController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/feedback"}, requestMapping.value());

        Method method = MesProFeedbackController.class.getDeclaredMethod("frontlineSubmit",
                MesProFrontlineFeedbackSubmitReqVO.class);
        assertArrayEquals(new String[]{"/frontline/submit"}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-feedback:create')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals(CommonResult.class, method.getReturnType());
        assertNotNull(method.getParameters()[0].getAnnotation(RequestBody.class));
    }

    @Test
    void frontlineSubmitRequest_hasFeedbackRecordbookPoolEmployeeAndSignatureContract() throws Exception {
        requireField(MesProFrontlineFeedbackSubmitReqVO.class, "feedbackPayload");
        requireField(MesProFrontlineFeedbackSubmitReqVO.class, "recordbookPayload");
        requireField(MesProFrontlineFeedbackSubmitReqVO.class, "processPoolContext");
        requireField(MesProFrontlineFeedbackSubmitReqVO.class, "actualEmployeeId");
        requireField(MesProFrontlineFeedbackSubmitReqVO.class, "signatureId");
        requireField(MesProFrontlineFeedbackSubmitReqVO.class, "signatureEmployeeId");
        requireField(MesProFrontlineFeedbackSubmitReqVO.class, "rawPayload");

        requireField(MesProFrontlineFeedbackSubmitRespVO.class, "feedbackId");
        requireField(MesProFrontlineFeedbackSubmitRespVO.class, "recordbookEntryId");
        requireField(MesProFrontlineFeedbackSubmitRespVO.class, "recordbookEventId");
        requireField(MesProFrontlineFeedbackSubmitRespVO.class, "processPoolEventId");
    }

    private static Field requireField(Class<?> type, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        type.getDeclaredMethod("get" + suffix);
        return field;
    }
}
