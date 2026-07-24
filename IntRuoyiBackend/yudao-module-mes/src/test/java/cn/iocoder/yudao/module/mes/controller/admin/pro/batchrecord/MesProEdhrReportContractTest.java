package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportDefinitionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportQueryReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrReportContractTest {

    @Test
    void catalogControllerMappings_matchFirstSliceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrReportCatalogController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-report-catalog"}, requestMapping.value());

        Method page = MesProEdhrReportCatalogController.class.getDeclaredMethod("getPage",
                MesProEdhrReportCatalogPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-report:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method detail = MesProEdhrReportCatalogController.class.getDeclaredMethod("getDetail", Long.class);
        assertArrayEquals(new String[]{"/detail"}, detail.getAnnotation(GetMapping.class).value());
        assertEquals("id", detail.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-report:query')",
                detail.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void definitionControllerMappings_matchFirstSliceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrReportDefinitionController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-report-definition"}, requestMapping.value());

        Method page = MesProEdhrReportDefinitionController.class.getDeclaredMethod("getPage",
                MesProEdhrReportDefinitionPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-report:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method detail = MesProEdhrReportDefinitionController.class.getDeclaredMethod("getDetail", Long.class);
        assertArrayEquals(new String[]{"/detail"}, detail.getAnnotation(GetMapping.class).value());
        assertEquals("id", detail.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-report:query')",
                detail.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void queryControllerMappings_matchFirstSliceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrReportQueryController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-report-query"}, requestMapping.value());

        Method run = MesProEdhrReportQueryController.class.getDeclaredMethod("runReportQuery",
                MesProEdhrReportQueryReqVO.class);
        assertArrayEquals(new String[]{"/run"}, run.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-report:query')",
                run.getAnnotation(PreAuthorize.class).value());

        Method exportAudit = MesProEdhrReportQueryController.class.getDeclaredMethod("recordExportAudit",
                MesProEdhrReportExportAuditReqVO.class);
        assertArrayEquals(new String[]{"/export-audit"}, exportAudit.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-report:export')",
                exportAudit.getAnnotation(PreAuthorize.class).value());

        Method exportAuditPage = MesProEdhrReportQueryController.class.getDeclaredMethod("getExportAuditPage",
                MesProEdhrReportExportAuditPageReqVO.class);
        assertArrayEquals(new String[]{"/export-audit/page"}, exportAuditPage.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-report:query')",
                exportAuditPage.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void serviceContract_declaresReportCatalogQueryAndAuditMethods() throws Exception {
        MesProEdhrReportService.class.getDeclaredMethod("getCatalogPage", MesProEdhrReportCatalogPageReqVO.class);
        MesProEdhrReportService.class.getDeclaredMethod("getCatalogDetail", Long.class);
        MesProEdhrReportService.class.getDeclaredMethod("getDefinitionPage", MesProEdhrReportDefinitionPageReqVO.class);
        MesProEdhrReportService.class.getDeclaredMethod("getDefinitionDetail", Long.class);
        MesProEdhrReportService.class.getDeclaredMethod("runReportQuery", MesProEdhrReportQueryReqVO.class);
        MesProEdhrReportService.class.getDeclaredMethod("recordExportAudit", MesProEdhrReportExportAuditReqVO.class);
        MesProEdhrReportService.class.getDeclaredMethod("getExportAuditPage", MesProEdhrReportExportAuditPageReqVO.class);
    }
}
