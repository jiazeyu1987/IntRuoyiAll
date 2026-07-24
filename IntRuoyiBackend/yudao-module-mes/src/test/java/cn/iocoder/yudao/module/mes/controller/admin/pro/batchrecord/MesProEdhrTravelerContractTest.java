package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplatePageReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrTravelerContractTest {

    @Test
    void templateControllerMappings_matchFirstSliceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrTravelerTemplateController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-traveler-template"}, requestMapping.value());

        Method page = MesProEdhrTravelerTemplateController.class.getDeclaredMethod("getPage",
                MesProEdhrTravelerTemplatePageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-traveler-template:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrTravelerTemplateController.class.getDeclaredMethod("create",
                MesProEdhrTravelerTemplateCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-traveler-template:create')",
                create.getAnnotation(PreAuthorize.class).value());

        Method activate = MesProEdhrTravelerTemplateController.class.getDeclaredMethod("activate",
                MesProEdhrTravelerActivateReqVO.class);
        assertArrayEquals(new String[]{"/activate"}, activate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-traveler-template:activate')",
                activate.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void travelerControllerMappings_matchFirstSliceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrTravelerController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-traveler"}, requestMapping.value());

        Method page = MesProEdhrTravelerController.class.getDeclaredMethod("getPage",
                MesProEdhrTravelerPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-traveler:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method get = MesProEdhrTravelerController.class.getDeclaredMethod("get", Long.class);
        assertArrayEquals(new String[]{"/get"}, get.getAnnotation(GetMapping.class).value());
        assertEquals("id", get.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-traveler:query')",
                get.getAnnotation(PreAuthorize.class).value());

        Method generate = MesProEdhrTravelerController.class.getDeclaredMethod("generate",
                MesProEdhrTravelerGenerateReqVO.class);
        assertArrayEquals(new String[]{"/generate"}, generate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-traveler:generate')",
                generate.getAnnotation(PreAuthorize.class).value());

        Method eventPage = MesProEdhrTravelerController.class.getDeclaredMethod("getEventPage",
                MesProEdhrTravelerEventPageReqVO.class);
        assertArrayEquals(new String[]{"/event/page"}, eventPage.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-traveler:query')",
                eventPage.getAnnotation(PreAuthorize.class).value());
    }
}
