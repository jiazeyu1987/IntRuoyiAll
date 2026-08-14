package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSubmitRespVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlinePqcSubmitReceiptControllerTest {

    @Mock
    private MesFrontlinePqcContextService pqcContextService;

    @InjectMocks
    private MesFrontlineDeviceAccountController controller;

    @Test
    void submitReceiptEndpoint_isReadOnlyGetRouteWithQueryPermission() throws Exception {
        Method method = MesFrontlineDeviceAccountController.class.getDeclaredMethod(
                "getPqcSubmitReceipt", Long.class);

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertArrayEquals(new String[]{"/pqc/submit-receipt"}, getMapping.value());
        assertEquals("@ss.hasPermission('mes:pro-feedback:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void getPqcSubmitReceipt_usesLoginUserAndMapsSubmittedReceipt() {
        LocalDateTime serverSubmitTime = LocalDateTime.of(2026, 8, 8, 9, 30);
        when(pqcContextService.getSubmittedPqcInspection(9001L, 6101L))
                .thenReturn(Optional.of(new MesFrontlinePqcSubmitResult(
                        6101L, 7101L, 8101L, 9101L, "QUALIFIED", serverSubmitTime)));

        CommonResult<MesFrontlinePqcSubmitRespVO> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            response = controller.getPqcSubmitReceipt(6101L);
        }

        verify(pqcContextService).getSubmittedPqcInspection(9001L, 6101L);
        MesFrontlinePqcSubmitRespVO data = response.getData();
        assertEquals(6101L, data.getPqcTaskId());
        assertEquals(7101L, data.getPqcEventId());
        assertEquals(8101L, data.getPqcRecordId());
        assertEquals(9101L, data.getSignatureId());
        assertEquals("QUALIFIED", data.getInspectionResult());
        assertEquals(serverSubmitTime, data.getServerSubmitTime());
    }

    @Test
    void getPqcSubmitReceipt_returnsNullWhenTaskHasNoSubmittedReceipt() {
        when(pqcContextService.getSubmittedPqcInspection(9001L, 6102L)).thenReturn(Optional.empty());

        CommonResult<MesFrontlinePqcSubmitRespVO> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            response = controller.getPqcSubmitReceipt(6102L);
        }

        verify(pqcContextService).getSubmittedPqcInspection(9001L, 6102L);
        assertNull(response.getData());
    }
}
