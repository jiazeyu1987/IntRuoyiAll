package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineRuntimeConfigRespVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDefectReasonOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceParameterOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceAccountContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineEmployeeSwitchResult;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProductionSubmitContext;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRuntimeConfig;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRuntimeConfigService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamEmployeeOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTemplateDescriptor;
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
import java.time.LocalDateTime;
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
                List.of(
                        new MesFrontlineTeamEmployeeOption(8801L, null, "TMP-001", "临时工甲", "临时工甲", "TEMPORARY"),
                        new MesFrontlineTeamEmployeeOption(8802L, 10002L, "EMP-002", "正式工乙", "正式工乙", "FORMAL")),
                List.of(new MesFrontlineTeamDeviceOption(7001L, "D-001", "压力泵", "ENABLED", List.of(
                        new MesFrontlineDeviceParameterOption("pressure", "压力", "MPa",
                                 new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("15"), "DECIMAL",
                                 "10-20MPa，目标15MPa", List.of("10", "15", "20"), "15", 1)))),
                List.of(new MesFrontlineDefectReasonOption(8301L, "LOSS", "LOSS-001", "正常损耗")),
                List.of(new MesFrontlineProcessMaterial(501L, "A001", "弹簧", null, BigDecimal.ONE)),
                new MesFrontlineProductionSubmitContext(41L, "WO-F2-001", "F2正式工单",
                        51L, 101L, 1001L, 201L, 301L, 61L, 9001L, 901L,
                        new BigDecimal("300.000"), LocalDateTime.of(2026, 8, 30, 0, 0)),
                List.of(
                        new MesFrontlineEmployeeSwitchResult(9001L, 8801L, 101L, 1001L, 201L, false,
                                new MesFrontlineTemplateDescriptor("FRONTLINE-PROD", "PRODUCTION", 1001L, 201L, 8801L)),
                        new MesFrontlineEmployeeSwitchResult(9001L, 10002L, 101L, 1001L, 201L, false,
                                new MesFrontlineTemplateDescriptor("FRONTLINE-PROD", "PRODUCTION", 1001L, 201L, 10002L)))
                , "snapshot-001", "hash-001"
        );
        when(runtimeConfigService.getRuntimeConfig(9001L, 8101L, 101L, 1001L, 201L)).thenReturn(runtimeConfig);

        CommonResult<MesFrontlineRuntimeConfigRespVO> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            response = controller.getRuntimeConfig(8101L, 101L, 1001L, 201L);
        }

        verify(runtimeConfigService).getRuntimeConfig(9001L, 8101L, 101L, 1001L, 201L);
        MesFrontlineRuntimeConfigRespVO data = response.getData();
        assertEquals(101L, data.getRouteId());
        assertEquals(1001L, data.getRouteProcessId());
        assertEquals(201L, data.getProcessId());
        assertEquals("临时工甲", data.getEmployees().get(0).getEmployeeName());
        assertEquals("临时工甲", data.getEmployees().get(0).getDisplayName());
        assertEquals("TEMPORARY", data.getEmployees().get(0).getEmployeeType());
        assertEquals("压力泵", data.getDevices().get(0).getDeviceName());
        assertEquals("10-20MPa，目标15MPa",
                data.getDevices().get(0).getParameters().get(0).getStandardText());
        assertEquals("ENABLED", data.getDevices().get(0).getDeviceStatus());
        assertEquals("MPa", data.getDevices().get(0).getParameters().get(0).getUnit());
        assertEquals(new BigDecimal("15"), data.getDevices().get(0).getParameters().get(0).getDefaultValue());
        assertEquals(List.of("10", "15", "20"),
                data.getDevices().get(0).getParameters().get(0).getOptionValues());
        assertEquals("15", data.getDevices().get(0).getParameters().get(0).getDefaultText());
        assertEquals(1, data.getDevices().get(0).getParameters().get(0).getDecimalScale());
        assertEquals("正常损耗", data.getDefectReasons().get(0).getReasonName());
        assertEquals(501L, data.getMaterials().get(0).getMaterialId());
        assertEquals("弹簧", data.getMaterials().get(0).getMaterialName());
        assertEquals(BigDecimal.ONE, data.getMaterials().get(0).getBomQuantity());
        assertEquals(List.of(), data.getMaterials().get(0).getBatchCodes());
        assertEquals(41L, data.getProductionSubmitContext().getWorkOrderId());
        assertEquals("WO-F2-001", data.getProductionSubmitContext().getWorkOrderCode());
        assertEquals(51L, data.getProductionSubmitContext().getTaskId());
        assertEquals(61L, data.getProductionSubmitContext().getItemId());
        assertEquals(9001L, data.getProductionSubmitContext().getApproveUserId());
        assertEquals(901L, data.getProductionSubmitContext().getRecordbookId());
        assertEquals(2, data.getEmployeeSwitchSnapshots().size());
        assertEquals(8801L, data.getEmployeeSwitchSnapshots().get(0).getActualEmployeeId());
        assertEquals(1001L, data.getEmployeeSwitchSnapshots().get(0).getRouteProcessId());
        assertEquals("FRONTLINE-PROD", data.getEmployeeSwitchSnapshots().get(0).getTemplate().getTemplateNo());
        assertEquals(10002L, data.getEmployeeSwitchSnapshots().get(1).getActualEmployeeId());
        assertEquals(10002L, data.getEmployeeSwitchSnapshots().get(1).getTemplate().getActualEmployeeId());
        assertEquals("snapshot-001", data.getFrontlineSessionSnapshotId());
        assertEquals("hash-001", data.getFrontlineSessionSnapshotHash());
    }

    @Test
    void runtimeConfigEndpoint_isReadOnlyAndDoesNotAcceptClientLeaderUserId() throws Exception {
        Method method = MesFrontlineDeviceAccountController.class.getDeclaredMethod(
                "getRuntimeConfig", Long.class, Long.class, Long.class, Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertArrayEquals(new String[]{"/runtime-config"}, getMapping.value());
        assertEquals("@ss.hasPermission('mes:pro-feedback:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
