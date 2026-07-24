package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrEvidencePackagePageReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrDeliveryContractTest {

    @Test
    void deliveryControllerMappings_matchFirstSliceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrDeliveryCockpitController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-delivery-cockpit"}, requestMapping.value());

        Method page = MesProEdhrDeliveryCockpitController.class.getDeclaredMethod("getProjectPage",
                MesProEdhrDeliveryProjectPageReqVO.class);
        assertArrayEquals(new String[]{"/project/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-delivery:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrDeliveryCockpitController.class.getDeclaredMethod("createProject",
                MesProEdhrDeliveryProjectCreateReqVO.class);
        assertArrayEquals(new String[]{"/project/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-delivery:create')",
                create.getAnnotation(PreAuthorize.class).value());

        Method detail = MesProEdhrDeliveryCockpitController.class.getDeclaredMethod("getProjectDetail", Long.class);
        assertArrayEquals(new String[]{"/project/detail"}, detail.getAnnotation(GetMapping.class).value());
        assertEquals("id", detail.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-delivery:query')",
                detail.getAnnotation(PreAuthorize.class).value());

        Method packages = MesProEdhrDeliveryCockpitController.class.getDeclaredMethod("getEvidencePackagePage",
                MesProEdhrEvidencePackagePageReqVO.class);
        assertArrayEquals(new String[]{"/evidence-package/page"}, packages.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-delivery:query')",
                packages.getAnnotation(PreAuthorize.class).value());

        Method gateSummary = MesProEdhrDeliveryCockpitController.class.getDeclaredMethod("getGateSummary", Long.class);
        assertArrayEquals(new String[]{"/gate-summary"}, gateSummary.getAnnotation(GetMapping.class).value());
        assertEquals("projectId", gateSummary.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-delivery:query')",
                gateSummary.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void serviceContract_declaresDeliveryCockpitMethods() throws Exception {
        MesProEdhrDeliveryService.class.getDeclaredMethod("getProjectPage",
                MesProEdhrDeliveryProjectPageReqVO.class);
        MesProEdhrDeliveryService.class.getDeclaredMethod("createProject",
                MesProEdhrDeliveryProjectCreateReqVO.class);
        MesProEdhrDeliveryService.class.getDeclaredMethod("getProjectDetail", Long.class);
        MesProEdhrDeliveryService.class.getDeclaredMethod("getEvidencePackagePage",
                MesProEdhrEvidencePackagePageReqVO.class);
        MesProEdhrDeliveryService.class.getDeclaredMethod("getGateSummary", Long.class);
    }
}
