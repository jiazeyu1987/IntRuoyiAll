package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineRuntimeConfigRespVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDefectReasonOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceParameterOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceAccountContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRuntimeConfig;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRuntimeConfigService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamEmployeeOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineRuntimeConfigControllerTest {

    @Mock
    private MesFrontlineDeviceAccountContextService contextService;
    @Mock
    private MesFrontlineEmployeeSwitchService employeeSwitchService;
    @Mock
    private MesFrontlineRuntimeConfigService runtimeConfigService;

    @InjectMocks
    private MesFrontlineDeviceAccountController controller;

    @Test
    void getRuntimeConfig_usesLoginUserAndReturnsTeamLeaderConfiguredOptions() {
        MesFrontlineRuntimeConfig runtimeConfig = new MesFrontlineRuntimeConfig(
                101L,
                1001L,
                201L,
                List.of(new MesFrontlineTeamEmployeeOption(8801L, null, "TMP-001", "临时工甲", "临时工甲", "TEMPORARY")),
                List.of(new MesFrontlineTeamDeviceOption(7001L, "D-001", "压力泵", "ENABLED", List.of(
                        new MesFrontlineDeviceParameterOption("pressure", "压力", "MPa",
                                new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("15"), "DECIMAL")))),
                List.of(new MesFrontlineDefectReasonOption(8301L, "LOSS", "LOSS-001", "正常损耗"))
        );
        when(runtimeConfigService.getRuntimeConfig(9001L, 101L, 1001L, 201L)).thenReturn(runtimeConfig);

        CommonResult<MesFrontlineRuntimeConfigRespVO> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            response = controller.getRuntimeConfig(101L, 1001L, 201L);
        }

        verify(runtimeConfigService).getRuntimeConfig(9001L, 101L, 1001L, 201L);
        MesFrontlineRuntimeConfigRespVO data = response.getData();
        assertEquals(101L, data.getRouteId());
        assertEquals(1001L, data.getRouteProcessId());
        assertEquals(201L, data.getProcessId());
        assertEquals("临时工甲", data.getEmployees().get(0).getEmployeeName());
        assertEquals("临时工甲", data.getEmployees().get(0).getDisplayName());
        assertEquals("TEMPORARY", data.getEmployees().get(0).getEmployeeType());
        assertEquals("压力泵", data.getDevices().get(0).getDeviceName());
        assertEquals("ENABLED", data.getDevices().get(0).getDeviceStatus());
        assertEquals("MPa", data.getDevices().get(0).getParameters().get(0).getUnit());
        assertEquals(new BigDecimal("15"), data.getDevices().get(0).getParameters().get(0).getDefaultValue());
        assertEquals("正常损耗", data.getDefectReasons().get(0).getReasonName());
    }

    @Test
    void runtimeConfigEndpoint_isReadOnlyAndDoesNotAcceptClientLeaderUserId() throws Exception {
        Method method = MesFrontlineDeviceAccountController.class.getDeclaredMethod(
                "getRuntimeConfig", Long.class, Long.class, Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertArrayEquals(new String[]{"/runtime-config"}, getMapping.value());
        assertEquals("@ss.hasPermission('mes:pro-feedback:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
