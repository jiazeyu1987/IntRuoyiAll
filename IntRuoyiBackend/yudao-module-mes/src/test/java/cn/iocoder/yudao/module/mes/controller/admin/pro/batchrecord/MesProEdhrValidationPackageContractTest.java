package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackageCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceLinkCreateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrValidationPackageContractTest {

    @Test
    void validationPackageControllerMappings_matchTraceabilityContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrValidationPackageController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-validation-package"}, requestMapping.value());

        Method page = MesProEdhrValidationPackageController.class.getDeclaredMethod("getPackagePage",
                MesProEdhrValidationPackagePageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-validation:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrValidationPackageController.class.getDeclaredMethod("createPackage",
                MesProEdhrValidationPackageCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-validation:create')",
                create.getAnnotation(PreAuthorize.class).value());

        Method detail = MesProEdhrValidationPackageController.class.getDeclaredMethod("getPackageDetail", Long.class);
        assertArrayEquals(new String[]{"/detail"}, detail.getAnnotation(GetMapping.class).value());
        assertEquals("id", detail.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-validation:query')",
                detail.getAnnotation(PreAuthorize.class).value());

        Method evaluateTrace = MesProEdhrValidationPackageController.class.getDeclaredMethod("evaluateTrace", Long.class);
        assertArrayEquals(new String[]{"/evaluate-trace"}, evaluateTrace.getAnnotation(PostMapping.class).value());
        assertEquals("packageId", evaluateTrace.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-validation:evaluate-trace')",
                evaluateTrace.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void validationItemAndTraceControllerMappings_matchTraceabilityContract() throws Exception {
        RequestMapping itemMapping = MesProEdhrValidationRequirementItemController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-validation-requirement-item"}, itemMapping.value());

        Method itemPage = MesProEdhrValidationRequirementItemController.class.getDeclaredMethod("getRequirementItemPage",
                MesProEdhrValidationRequirementItemPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, itemPage.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-validation:query')",
                itemPage.getAnnotation(PreAuthorize.class).value());

        Method itemCreate = MesProEdhrValidationRequirementItemController.class.getDeclaredMethod("createRequirementItem",
                MesProEdhrValidationRequirementItemCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, itemCreate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-validation:create')",
                itemCreate.getAnnotation(PreAuthorize.class).value());

        RequestMapping traceMapping = MesProEdhrValidationTraceLinkController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-validation-trace-link"}, traceMapping.value());

        Method traceCreate = MesProEdhrValidationTraceLinkController.class.getDeclaredMethod("createTraceLink",
                MesProEdhrValidationTraceLinkCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, traceCreate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-validation:create')",
                traceCreate.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void serviceContract_declaresValidationPackageTraceMethods() throws Exception {
        MesProEdhrValidationService.class.getDeclaredMethod("getPackagePage",
                MesProEdhrValidationPackagePageReqVO.class);
        MesProEdhrValidationService.class.getDeclaredMethod("createPackage",
                MesProEdhrValidationPackageCreateReqVO.class);
        MesProEdhrValidationService.class.getDeclaredMethod("getPackageDetail", Long.class);
        MesProEdhrValidationService.class.getDeclaredMethod("getRequirementItemPage",
                MesProEdhrValidationRequirementItemPageReqVO.class);
        MesProEdhrValidationService.class.getDeclaredMethod("createRequirementItem",
                MesProEdhrValidationRequirementItemCreateReqVO.class);
        MesProEdhrValidationService.class.getDeclaredMethod("createTraceLink",
                MesProEdhrValidationTraceLinkCreateReqVO.class);
        MesProEdhrValidationService.class.getDeclaredMethod("evaluateTrace", Long.class);
    }
}
