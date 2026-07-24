package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchRecordFormPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleSaveReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesProEdhrProcessFormPermissionRuleControllerContractTest {

    @Test
    void processFormPermissionRuleEndpoints_areAvailableForRouteBatchRecordTab() throws Exception {
        Method getRule = MesProEdhrProcessFormPermissionRuleController.class.getDeclaredMethod(
                "getRule", Long.class, String.class);
        assertArrayEquals(new String[]{"/get"}, getRule.getAnnotation(GetMapping.class).value());
        assertEquals("routeProcessId", getRule.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("batchRecordReportId", getRule.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-process-form-permission-rule:query')",
                getRule.getAnnotation(PreAuthorize.class).value());

        Method saveRule = MesProEdhrProcessFormPermissionRuleController.class.getDeclaredMethod(
                "saveRule", MesProEdhrProcessFormPermissionRuleSaveReqVO.class);
        assertArrayEquals(new String[]{"/save"}, saveRule.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-process-form-permission-rule:update')",
                saveRule.getAnnotation(PreAuthorize.class).value());

        Method getRuleByReport = MesProEdhrProcessFormPermissionRuleController.class.getDeclaredMethod(
                "getRuleByReport", String.class);
        assertArrayEquals(new String[]{"/get-by-report"}, getRuleByReport.getAnnotation(GetMapping.class).value());
        assertEquals("batchRecordReportId", getRuleByReport.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-process-form-permission-rule:query')",
                getRuleByReport.getAnnotation(PreAuthorize.class).value());

        Method saveRuleByReport = MesProEdhrProcessFormPermissionRuleController.class.getDeclaredMethod(
                "saveRuleByReport", MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.class);
        assertArrayEquals(new String[]{"/save-by-report"}, saveRuleByReport.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-process-form-permission-rule:update')",
                saveRuleByReport.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void processFormPermissionRuleContract_supportsFillAndSignatureCandidateRules() throws Exception {
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getRouteProcessId"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getBatchRecordReportId"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getFillRule"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getEquipmentFillRule"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getQualityFillRule"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getSignatureRules"));
        assertNotNull(MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getBatchRecordReportId"));
        assertNotNull(MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getFillRule"));

        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule.class.getDeclaredMethod("getCandidateSourceType"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule.class.getDeclaredMethod("getCandidateSourceIds"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule.class.getDeclaredMethod("getCompletionPolicy"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule.class.getDeclaredMethod("getDueMinutes"));
        Field dueMinutes = MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule.class
                .getDeclaredField("dueMinutes");
        assertNull(dueMinutes.getAnnotation(NotNull.class),
                "工序表单填写设置已不展示处理时限，保存契约不能继续要求 dueMinutes 必填");

        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule.class.getDeclaredMethod("getSignatureCellKey"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule.class.getDeclaredMethod("getSignatureRole"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule.class.getDeclaredMethod("getRule"));

        assertNotNull(MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getFillRuleStatus"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getEquipmentFillRuleStatus"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getQualityFillRuleStatus"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getSignatureRuleStatus"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getEquipmentFillRule"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getQualityFillRule"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleRespVO.CandidateRule.class.getDeclaredMethod("getCandidateUsers"));
        assertNotNull(MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getAffectedRouteBindingCount"));
    }
}
